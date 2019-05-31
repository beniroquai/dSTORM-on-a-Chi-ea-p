/* Here ESP32 will keep 2 roles:
  1/ read data from DHT11/DHT22 sensor
  2/ control led on-off
  So it willpublish temperature topic and scribe topic bulb on/off
*/

#include <WiFi.h>
#include <PubSubClient.h>

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

///* topics */
#define LED_TOPIC     "lens/left/led"
#define LENS_XR_TOPIC     "lens/right/x"
#define LENS_YR_TOPIC     "lens/right/y"
#define LENS_XL_TOPIC     "lens/left/x"
#define LENS_YL_TOPIC     "lens/left/y"

#define CLIENT_ID "ESP32Client";

// PWM Stuff
int pwm_resolution = 10;
int pwm_frequency = 19000; //12000
int pwm_channel_xr = 0;
int pwmpin_xr = 27;

int pwm_channel_yr = 1;
int pwmpin_yr = 26;

int pwm_channel_yl = 2;
int pwmpin_yl = 25;

int pwm_channel_xl = 3;
int pwmpin_xl = 33;

// MQTT Stuff
long lastMsg = 0;
char msg[20];

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

  // setup the PWM port
  ledcSetup(1, pwm_frequency, pwm_resolution);
  ledcAttachPin(pwmpin_xr, 1);

  ledcSetup(2, pwm_frequency , pwm_resolution);
  ledcAttachPin(pwmpin_yr, 2);

  ledcSetup(3, pwm_frequency , pwm_resolution);
  ledcAttachPin(pwmpin_xl, 3);
  
  ledcSetup(4, pwm_frequency , pwm_resolution);
  ledcAttachPin(pwmpin_yl, 4);
 
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

  // Catch the value for movement of lens in X-direction
  if (String(topic) == LENS_XR_TOPIC) {
    //dacWrite(25, (int)payload_int);
    ledcWrite(1, (int)payload_int);
    Serial.print("Lens (right) X is set to: ");
    Serial.print((int)payload_int);
    Serial.println();
  }

  // Catch the value for movement of lens in Y-direction
  if (String(topic) == LENS_YR_TOPIC) {
    //dacWrite(26, (int)payload_int);
    ledcWrite(2, (int)payload_int);
    Serial.print("Lens (right) Y is set to: ");
    Serial.print((int)payload_int);
    Serial.println();
  }

  // Catch the value for movement of lens in X-direction
  if (String(topic) == LENS_XL_TOPIC) {
    ledcWrite(3, (int)payload_int);
    Serial.print("Lens (left) X is set to: ");
    Serial.print((int)payload_int);
    Serial.println();
  }

  // Catch the value for movement of lens in Y-direction
  if (String(topic) == LENS_YL_TOPIC) {
    //dacWrite(26, (int)payload_int);
    ledcWrite(4, (int)payload_int);
    Serial.print("Lens (left) Y is set to: ");
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
      client.subscribe(LENS_XR_TOPIC);
      client.subscribe(LENS_YR_TOPIC);
      client.subscribe(LENS_XL_TOPIC);
      client.subscribe(LENS_YL_TOPIC);
      
    } else {
      Serial.print("failed, status code =");
      Serial.print(client.state());
      Serial.println("try again in 5 seconds");
      /* Wait 5 seconds before retrying */
      delay(5000);
    }
  }
}


