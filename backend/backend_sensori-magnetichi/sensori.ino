#include <WiFi.h>
#include <WiFiUdp.h>
#include <WebServer.h>

#define DEVICE_ID "RTNode6" // nome del opera

// GPIO dai sensori
#define SA0  0 
#define SA1  4
#define SA2 26
#define SA3 25

#define MAX_REPEAT 3

const char * ssid = "SCRIVI_SSID_NAME"; //node di wifi
const char * pass = "SCRIVI_PASS"; //password per wifi

const int localPort=1702; // la porta per inviare i dati
const int receiverPort=8085; // la porta per UDP server recivetore

IPAddress dataLoggerAddress(192, 168, 43, 46); // IP di UDP server
WiFiUDP udp;
WebServer server(80);  // la porta 80 per web server

int nLoop;
boolean debug=true;

int sensorPin[4] = { SA0, SA1, SA2, SA3 };
int sensorStatus[4] = { HIGH, HIGH, HIGH, HIGH };
int sensorVal[4];

int sendMsg=1,repeatMsg=0;
byte sendBuffer[32];
char messageBuffer[32];
long lastMessageTS=0;

// ----------- HTML-pagina -----------
String makeHTML() {
  String page = "<!DOCTYPE html><html><head>";
  page += "<meta charset='UTF-8'><meta http-equiv='refresh' content='2'>"; // autoreload 2 sec
  page += "<title>ESP32 Sensors</title>";
  page += "<style>body{font-family:Arial;text-align:center;} .ok{color:green;} .fail{color:red;}</style>";
  page += "</head><body>";
  page += "<h1>ESP32 Web Interface</h1>";
  page += "<h3>Device: " + String(DEVICE_ID) + "</h3>";
  page += "<table border='1' cellpadding='10' style='margin:auto;'><tr><th>Sensor</th><th>Status</th></tr>";
  for (int i=0; i<4; i++) {
    page += "<tr><td>Sensor " + String(i) + "</td>";
    page += (sensorStatus[i] == HIGH) ? "<td class='ok'>HIGH</td>" : "<td class='fail'>LOW</td>";
    page += "</tr>";
  }
  page += "</table>";
  page += "<p>IP: " + WiFi.localIP().toString() + "</p>";
  page += "</body></html>";
  return page;
}

void setup() {
  for(int i=0; i < 4; i++) pinMode(sensorPin[i], INPUT_PULLUP);

  Serial.begin(115200);
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, pass);

  Serial.print("Connecting to ");
  Serial.println(ssid);

  nLoop=0;
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
    nLoop++;
    if (nLoop > 20) ESP.restart();
  }

  Serial.println();
  Serial.print("Connected! IP address: ");
  Serial.println(WiFi.localIP());
  Serial.print("Web server available at: http://");
  Serial.println(WiFi.localIP());
  
  udp.begin(localPort);

  // запускаем веб-сервер
  server.on("/", []() {
    server.send(200, "text/html", makeHTML());
  });
  server.begin();

  delay(1000);
}

void loop() {
  // loop per controllare i sensori
  for(int i=0; i < 4; i++) {
    sensorVal[i] = digitalRead(sensorPin[i]);
    if (sensorVal[i]!=sensorStatus[i]) {
      sendMsg=1;
      repeatMsg=0;
      sensorStatus[i]=sensorVal[i];
    }
  }

  if (millis()-lastMessageTS > 1000) {
    sendMsg=1;
    repeatMsg=0;
  } 
  
  if (sendMsg) {
    memset(messageBuffer, 0, sizeof(messageBuffer));
    sprintf(messageBuffer,"#%s=%d%d%d%d",DEVICE_ID,sensorStatus[0],sensorStatus[1],sensorStatus[2],sensorStatus[3]);
    Serial.println(messageBuffer);
    int len = strlen(messageBuffer);
    for(int i=0; i < len; i++) sendBuffer[i] = messageBuffer[i];
    sendUDPMessage(sendBuffer, len);
    lastMessageTS=millis();
    repeatMsg++;
    if (repeatMsg>=MAX_REPEAT) sendMsg=0;
  }

  server.handleClient();
  delay(100);
}

void sendUDPMessage(byte buffer[], int msgLen) {
  udp.beginPacket(dataLoggerAddress, receiverPort);
  udp.write(buffer, msgLen);
  int result = udp.endPacket();
  Serial.print("Send result: ");
  Serial.println(result);
}
