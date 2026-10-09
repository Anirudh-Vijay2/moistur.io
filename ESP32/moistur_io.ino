#include <WiFi.h>
#include <WebServer.h>

#define SOIL_PIN 34
#define RELAY_PIN 23

#define PUMP_ON LOW
#define PUMP_OFF HIGH

const char* WIFI_SSID = "YOUR_WIFI_SSID";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";

const int DRY_VALUE = 3000;
const int WET_VALUE = 1200;

const int AUTO_WATER_THRESHOLD = 5;
const unsigned long PUMP_DURATION = 4000;

WebServer server(80);

bool pumpState = false;
bool automaticWateringActive = false;
bool automaticCycleCompleted = false;

unsigned long pumpStartTime = 0;

// ---------- Pump ----------
void pumpOn() {
  digitalWrite(RELAY_PIN, PUMP_ON);
  pumpState = true;
}

void pumpOff() {
  digitalWrite(RELAY_PIN, PUMP_OFF);
  pumpState = false;
}

// ---------- Moisture ----------
int readMoisture() {
  long total = 0;

  for (int i = 0; i < 10; i++) {
    total += analogRead(SOIL_PIN);
    delay(10);
  }

  int raw = total / 10;

  int moisture = map(raw, DRY_VALUE, WET_VALUE, 0, 100);
  moisture = constrain(moisture, 0, 100);

  return moisture;
}

int readRaw() {
  long total = 0;

  for (int i = 0; i < 10; i++) {
    total += analogRead(SOIL_PIN);
    delay(10);
  }

  return total / 10;
}

// ---------- API ----------
void handleData() {
  int raw = readRaw();

  int moisture = map(raw, DRY_VALUE, WET_VALUE, 0, 100);
  moisture = constrain(moisture, 0, 100);

  String json = "{";
  json += "\"moisture\":" + String(moisture) + ",";
  json += "\"raw\":" + String(raw) + ",";
  json += "\"pump\":" + String(pumpState ? "true" : "false");
  json += "}";

  server.send(200, "application/json", json);
}

void handlePumpOn() {
  automaticWateringActive = false;
  pumpOn();

  server.send(
    200,
    "application/json",
    "{\"success\":true,\"pump\":true}"
  );
}

void handlePumpOff() {
  automaticWateringActive = false;
  pumpOff();

  server.send(
    200,
    "application/json",
    "{\"success\":true,\"pump\":false}"
  );
}

// ---------- Web Dashboard ----------
void handleRoot() {
  String html = R"rawliteral(
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>moistur.io</title>

<style>
body {
  font-family: Arial;
  text-align: center;
  background: #111;
  color: white;
  margin-top: 50px;
}

.card {
  max-width: 400px;
  margin: auto;
  padding: 25px;
  background: #222;
  border-radius: 20px;
}

h1 {
  margin-bottom: 30px;
}

.moisture {
  font-size: 60px;
  font-weight: bold;
}

button {
  padding: 15px 25px;
  margin: 10px;
  font-size: 18px;
  border: none;
  border-radius: 10px;
  cursor: pointer;
}
</style>
</head>

<body>

<div class="card">

<h1>🌱 moistur.io</h1>

<div class="moisture">
<span id="moisture">--</span>%
</div>

<p>Raw: <span id="raw">--</span></p>

<p>
Pump:
<span id="pump">--</span>
</p>

<button onclick="pumpOn()">Turn ON</button>
<button onclick="pumpOff()">Turn OFF</button>

</div>

<script>

function updateData() {
  fetch('/data')
    .then(response => response.json())
    .then(data => {
      document.getElementById('moisture').innerText = data.moisture;
      document.getElementById('raw').innerText = data.raw;
      document.getElementById('pump').innerText =
        data.pump ? 'ON' : 'OFF';
    });
}

function pumpOn() {
  fetch('/pump/on');
}

function pumpOff() {
  fetch('/pump/off');
}

setInterval(updateData, 1000);
updateData();

</script>

</body>
</html>
)rawliteral";

  server.send(200, "text/html", html);
}

// ---------- Automatic Watering ----------
void automaticWatering() {
  int moisture = readMoisture();

  Serial.print("Moisture: ");
  Serial.print(moisture);
  Serial.print("% | Pump: ");
  Serial.println(pumpState ? "ON" : "OFF");

  if (moisture < AUTO_WATER_THRESHOLD &&
      !pumpState &&
      !automaticCycleCompleted) {

    pumpOn();

    automaticWateringActive = true;
    automaticCycleCompleted = true;
    pumpStartTime = millis();

    Serial.println("Automatic watering started");
  }

  if (automaticWateringActive &&
      pumpState &&
      millis() - pumpStartTime >= PUMP_DURATION) {

    pumpOff();
    automaticWateringActive = false;

    Serial.println("Automatic watering completed");
  }

  if (moisture >= AUTO_WATER_THRESHOLD) {
    automaticCycleCompleted = false;
  }
}

// ---------- Setup ----------
void setup() {
  Serial.begin(115200);

  pinMode(RELAY_PIN, OUTPUT);
  digitalWrite(RELAY_PIN, PUMP_OFF);

  analogSetPinAttenuation(SOIL_PIN, ADC_11db);

  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  Serial.print("Connecting to Wi-Fi");

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println();
  Serial.println("Wi-Fi CONNECTED!");
  Serial.print("SSID: ");
  Serial.println(WIFI_SSID);

  Serial.print("ESP32 IP: ");
  Serial.println(WiFi.localIP());

  Serial.print("Gateway: ");
  Serial.println(WiFi.gatewayIP());

  server.on("/", handleRoot);
  server.on("/data", handleData);
  server.on("/pump/on", handlePumpOn);
  server.on("/pump/off", handlePumpOff);

  server.begin();

  Serial.println("HTTP server started!");
  Serial.println("Ready.");
}

// ---------- Loop ----------
void loop() {
  server.handleClient();

  automaticWatering();

  delay(500);
}