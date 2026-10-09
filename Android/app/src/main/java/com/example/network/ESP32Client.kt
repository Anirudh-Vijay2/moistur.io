package com.example.network

import com.example.model.ESP32Data
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class ESP32Client(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .writeTimeout(2500, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)
        .build()
) {

    fun sanitizeIp(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("http://", ignoreCase = true)) {
            cleaned = cleaned.substring(7)
        } else if (cleaned.startsWith("https://", ignoreCase = true)) {
            cleaned = cleaned.substring(8)
        }
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length - 1)
        }
        return cleaned.trim()
    }

    fun isValidAddress(ip: String): Boolean {
        val sanitized = sanitizeIp(ip)
        if (sanitized.isBlank()) return false
        // Allow standard IPv4 or hostnames (e.g. moistur-esp32.local or 192.168.1.50 or with port)
        val regex = Regex("^([a-zA-Z0-9.-]+)(:[0-9]{1,5})?$")
        return regex.matches(sanitized)
    }

    suspend fun fetchData(ipAddress: String): Result<ESP32Data> = withContext(Dispatchers.IO) {
        val host = sanitizeIp(ipAddress)
        if (!isValidAddress(host)) {
            return@withContext Result.failure(IllegalArgumentException("Invalid ESP32 address"))
        }

        val url = "http://$host/data"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("ESP32 returned error HTTP ${response.code}")
                    )
                }

                val body = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response from ESP32"))

                try {
                    val json = JSONObject(body)
                    if (!json.has("moisture")) {
                        return@withContext Result.failure(IOException("Missing moisture reading in ESP32 data"))
                    }
                    val rawMoisture = json.getInt("moisture")
                    val moisture = rawMoisture.coerceIn(0, 100)
                    val pump = if (json.has("pump")) json.getBoolean("pump") else false

                    Result.success(ESP32Data(moisture = moisture, pump = pump))
                } catch (e: Exception) {
                    Result.failure(IOException("Invalid response format from ESP32"))
                }
            }
        } catch (e: UnknownHostException) {
            Result.failure(IOException("Unable to resolve ESP32 address"))
        } catch (e: SocketTimeoutException) {
            Result.failure(IOException("ESP32 connection timed out"))
        } catch (e: ConnectException) {
            Result.failure(IOException("Unable to connect to ESP32"))
        } catch (e: IOException) {
            Result.failure(IOException("Connection failed: ${e.message ?: "Network error"}"))
        } catch (e: Exception) {
            Result.failure(IOException("Unable to communicate with ESP32"))
        }
    }

    suspend fun setPump(ipAddress: String, turnOn: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        val host = sanitizeIp(ipAddress)
        if (!isValidAddress(host)) {
            return@withContext Result.failure(IllegalArgumentException("Invalid ESP32 address"))
        }

        val endpoint = if (turnOn) "/pump/on" else "/pump/off"
        val url = "http://$host$endpoint"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(turnOn)
                } else {
                    Result.failure(IOException("Pump command rejected: HTTP ${response.code}"))
                }
            }
        } catch (e: UnknownHostException) {
            Result.failure(IOException("Unable to resolve ESP32 address"))
        } catch (e: SocketTimeoutException) {
            Result.failure(IOException("ESP32 pump request timed out"))
        } catch (e: ConnectException) {
            Result.failure(IOException("Unable to connect to ESP32"))
        } catch (e: IOException) {
            Result.failure(IOException("Pump command failed: ${e.message ?: "Network error"}"))
        } catch (e: Exception) {
            Result.failure(IOException("Unable to send pump command to ESP32"))
        }
    }
}
