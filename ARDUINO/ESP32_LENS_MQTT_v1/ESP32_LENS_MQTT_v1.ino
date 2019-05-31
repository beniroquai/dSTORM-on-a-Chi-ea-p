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
#define LENS_ZR_TOPIC     "lens/right/z"
#define LENS_XL_TOPIC     "lens/left/x"
#define LENS_ZL_TOPIC     "lens/left/z"
#define LENS_VIBRATE      "lens/both/vibrate"

#define CLIENT_ID "ESP32Client";

// global switch for vibrating the lenses
int sofi_periode = 1;  // ms
int sofi_state = false;   // is sofi turned on?
int sofi_amplitude = 20;   // how many steps +/- ?

// default values for x/z lens' positions
int lens_xl_int = 0;
int lens_zl_int = 0;
int lens_xr_int = 0;
int lens_zr_int = 0;

// PWM Stuff
int pwm_resolution = 10;
int pwm_frequency = 800000;//19000; //12000

// right x-channel
int pwm_channel_xr = 0;
int pwmpin_xr = 27;

// right z-channel
int pwm_channel_zr = 1;
int pwmpin_zr = 26;

// left x-channel
int pwm_channel_xl = 2;
int pwmpin_xl = 25;

// left z-channel
int pwm_channel_zl = 3;
int pwmpin_zl = 33;

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

  /* setup the PWM ports and reset them to 0*/
  ledcSetup(1, pwm_frequency, pwm_resolution);
  ledcAttachPin(pwmpin_xr, pwm_channel_xr);
  ledcWrite(pwm_channel_xr, 0);

  ledcSetup(2, pwm_frequency , pwm_resolution);
  ledcAttachPin(pwmpin_zr, pwm_channel_zr);
  ledcWrite(pwm_channel_zr, 0);

  ledcSetup(3, pwm_frequency , pwm_resolution);
  ledcAttachPin(pwmpin_xl, pwm_channel_xl);
  ledcWrite(pwm_channel_xl, 0);

  ledcSetup(4, pwm_frequency , pwm_resolution);
  ledcAttachPin(pwmpin_zl, pwm_channel_zl);
  ledcWrite(pwm_channel_zl, 0);
}

void loop() {
  /* if client was disconnected then try to reconnect again */
  if (!client.connected()) {
    mqttconnect();
  }
  /* this function will listen for incomming
    subscribed topic-process-invoke receivedCallback */
  client.loop();

  if (sofi_state) {
    // if true, we want to vibrate both lenses along x
    ledcWrite(pwm_channel_xr, lens_xr_int + sofi_amplitude / 2);
    ledcWrite(pwm_channel_xl, lens_xl_int + sofi_periode / 2);
    delay(sofi_periode);
    ledcWrite(pwm_channel_xr, lens_xr_int - sofi_amplitude / 2);
    ledcWrite(pwm_channel_xl, lens_xl_int - sofi_periode / 2);
    delay(sofi_periode);
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
    if (payload_int == 1) {
      digitalWrite(led, HIGH);
    } else {
      /* we got '0' -> on */
      digitalWrite(led, LOW);
    }
  }

  // Catch the value for movement of lens in X-direction (right)
  if (String(topic) == LENS_XR_TOPIC) {
    //dacWrite(25, (int)payload_int);
    lens_xr_int = abs((int)payload_int);
    ledcWrite(pwm_channel_xr, lens_xr_int);
    Serial.print("Lens (right) X is set to: ");
    Serial.print(lens_xr_int);
    Serial.println();
  }

  // Catch the value for movement of lens in Z-direction (right)
  if (String(topic) == LENS_ZR_TOPIC) {
    //dacWrite(26, (int)payload_int);
    lens_zr_int = abs((int)payload_int);
    ledcWrite(pwm_channel_zr, lens_zr_int);
    Serial.print("Lens (right) Y is set to: ");
    Serial.print(lens_zr_int);
    Serial.println();
  }

  // Catch the value for movement of lens in X-direction (left)
  if (String(topic) == LENS_XL_TOPIC) {
    lens_xl_int = abs((int)payload_int);
    ledcWrite(pwm_channel_xl, lens_xl_int);
    Serial.print("Lens (left) X is set to: ");
    Serial.print(lens_xl_int );
    Serial.println();
  }

  // Catch the value for movement of lens in Z-direction (left)
  if (String(topic) == LENS_ZL_TOPIC) {
    //dacWrite(26, (int)payload_int);
    lens_zl_int = abs((int)payload_int);
    ledcWrite(pwm_channel_zl, lens_zl_int);
    Serial.print("Lens (left) Y is set to: ");
    Serial.print(lens_zl_int);
    Serial.println();
  }

  // Catch the value for starting the vibration mode
  if (String(topic) == LENS_VIBRATE) {
    /* we got '1' -> on */
    if (payload_int == 1) {
      sofi_state = true;
      Serial.print("LENS_VIBRATE is set to: ");
      Serial.print(sofi_state);
      Serial.println();
    } else {
      /* we got '0' -> off */
      sofi_state = false;
      Serial.print("LENS_VIBRATE is set to: ");
      Serial.print(sofi_state);
      Serial.println();
    }
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
      client.subscribe(LENS_ZR_TOPIC);
      client.subscribe(LENS_XL_TOPIC);
      client.subscribe(LENS_ZL_TOPIC);

    } else {
      Serial.print("failed, status code =");
      Serial.print(client.state());
      Serial.println("try again in 5 seconds");
      /* Wait 5 seconds before retrying */
      delay(5000);
    }
  }
}


