package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.MoistureLevel
import com.example.network.ESP32Client
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `launch MainActivity`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        org.junit.Assert.assertNotNull(activity)
    }

    @Test
    fun `verify moisture level thresholds`() {
        assertEquals(MoistureLevel.VERY_DRY, MoistureLevel.fromPercentage(3))
        assertEquals(MoistureLevel.DRY, MoistureLevel.fromPercentage(15))
        assertEquals(MoistureLevel.NORMAL, MoistureLevel.fromPercentage(50))
        assertEquals(MoistureLevel.WET, MoistureLevel.fromPercentage(85))
    }

    @Test
    fun `verify ip sanitization and address validation`() {
        val client = ESP32Client()
        assertEquals("172.25.89.50", client.sanitizeIp("http://172.25.89.50/"))
        assertEquals("172.25.89.50", client.sanitizeIp("https://172.25.89.50"))
        assertTrue(client.isValidAddress("172.25.89.50"))
        assertTrue(client.isValidAddress("moistur-esp32.local"))
    }
}
