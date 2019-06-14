/* Here ESP32 will keep 2 roles:
  1/ read data from DHT11/DHT22 sensor
  2/ control led on-off
  So it willpublish temperature topic and scribe topic bulb on/off
*/
#include <WiFi.h>
#include <PubSubClient.h>
#include <Stepper.h>

/* change it with your ssid-password */
const char* ssid = "Blynk";
const char* password = "12345678";
/* this is the IP of PC/raspberry where you installed MQTT Server
  on Wins use "ipconfig"
  on Linux use "ifconfig" to get its IP address */
const char* mqtt_server = "192.168.43.88";


/* create an instance of PubSubClient client */
WiFiClient espClient;
PubSubClient client(espClient);

/*LED GPIO pin*/
const char led = 2;

// change this to the number of steps on your motor
#define STEPS 200

// create an instance of the stepper class, specifying
// the number of steps of the motor and the pins it's
// attached to
//Stepper STP_X(STEPS,23,22,21,19);
//Stepper STP_Y(STEPS,26,25,32,33);
Stepper STP_X(STEPS,13,12,14,27);
Stepper STP_Y(STEPS,26,25,23,32);

///* topics */
#define LED_TOPIC     "lens/left/led"
#define STEPPER_X_FWD     "stepper/x/fwd"
#define STEPPER_X_BWD     "stepper/x/bwd"
#define STEPPER_Y_FWD     "stepper/y/fwd"
#define STEPPER_Y_BWD     "stepper/y/bwd"

#define CLIENT_ID "ESP32Client_motor2";

long lastMsg = 0;
char msg[20];


void receivedCallback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message received: ");
  Serial.println(topic);

  // Convert pointer to int
  int payload_int = 0;
  for (int i = 0; i < length; i++) {
    char c = payload[i];
    if (c >= '0' && c <= '9')
      payload_int = payload_int * 10 + c - '0'; //einzelne Ziffern zu einem Integer zusammenfügen
    else {
      Serial.print ((int)c);
      Serial.println(" war so nicht erwartet");
    }
  }

  Serial.print("Value is : [");
  Serial.print(payload_int);
  Serial.print("]");

  if (String(topic) == LED_TOPIC)  {
    /* we got '1' -> on */
    if (payload_int== 1) {
      digitalWrite(led, HIGH);
    } else {
      /* we got '0' -> on */
      digitalWrite(led, LOW);
    }
  }

  // Catch the value for stepment of lens in X-direction
  if (String(topic) == STEPPER_X_FWD) {
    STP_X.step((int)payload_int*10);
    Serial.print("Motor is running in x for: ");
    Serial.print((int)payload_int);
    Serial.println();
  }

  // Catch the value for stepment of lens in Y-direction
  if (String(topic) == STEPPER_X_BWD) {
    STP_X.step(-(int)payload_int*10);
    Serial.print("Motor is running in x for: ");
    Serial.print((int)payload_int);
    Serial.println();
  }

    // Catch the value for stepment of lens in Y-direction
  if (String(topic) == STEPPER_Y_FWD) {
    STP_Y.step((int)payload_int*10);
    Serial.print("Motor is running in y for: ");
    Serial.print((int)payload_int);
    Serial.println();
  }

  // Catch the value for stepment of lens in Y-direction
  if (String(topic) == STEPPER_Y_BWD) {
    STP_Y.step(-(int)payload_int*10);
    Serial.print("Motor is running in y for: ");
    Serial.print((int)payload_int);
    Serial.println();
  }


}

void mqttconnect() {
  /* Loop until reconnected */
  while (!client.connected()) {
    Serial.print("MQTT connecting ...");
    /* client ID */
    String clientId = CLIENT_ID;
    /* connect now */
    if (client.connect(clientId.c_str(), "username", "pi")) {
      Serial.println("connected");
      /* subscribe topic with default QoS 0*/
      client.subscribe(LED_TOPIC);
      client.subscribe(STEPPER_X_FWD);
      client.subscribe(STEPPER_X_BWD);
      client.subscribe(STEPPER_Y_FWD);
      client.subscribe(STEPPER_Y_BWD);
    } else {
      Serial.print("failed, status code =");
      Serial.print(client.state());
      Serial.println("try again in 5 seconds");
      /* Wait 5 seconds before retrying */
      delay(5000);
    }
  }
}

void setup() {
  Serial.begin(115200);
  // We start by connecting to a WiFi network
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);

  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  /* set led as output to control led on-off */
  pinMode(led, OUTPUT);

  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());

  /* configure the MQTT server with IPaddress and port */
  client.setServer(mqtt_server, 1883);
  /* this receivedCallback function will be invoked
    when client received subscribed topic */
  client.setCallback(receivedCallback);

  // Set the speed for stepper Motors
  STP_X.setSpeed(20);
  STP_Y.setSpeed(20);
}
void loop() {
  /* if client was disconnected then try to reconnect again */
  if (!client.connected()) {
    mqttconnect();
  }
  /* this function will listen for incomming
    subscribed topic-process-invoke receivedCallback */
  client.loop();
  /* we count until 3 secs reached to avoid blocking program if using delay()*/
  long now = millis();
  if (now - lastMsg > 3000) {
    lastMsg = now;

  }
}
