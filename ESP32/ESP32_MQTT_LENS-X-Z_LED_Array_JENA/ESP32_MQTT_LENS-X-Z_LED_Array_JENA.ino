/* Here ESP32 will keep 2 roles:
  1/ read data from DHT11/DHT22 sensor
  2/ control led on-off
  So it willpublish temperature topic and scribe topic bulb on/off
*/

#include <WiFi.h>
#include <PubSubClient.h>
#include <Stepper.h>
#include <Adafruit_NeoPixel.h>
#ifdef __AVR__
#include <avr/power.h> // Required for 16 MHz Adafruit Trinket
#endif

/* change it with your ssid-password */
const char* ssid = "Blynk";
const char* password = "12345678";
/* this is the IP of PC/raspberry where you installed MQTT Server
  on Wins use "ipconfig"
  on Linux use "ifconfig" to get its IP address */
#define BUFLEN 16
String localIP;
String gatewayIP;
//char MQTT_SERVER[BUFLEN];
const char* MQTT_SERVER = "192.168.43.88";


/* create an instance of PubSubClient client */
WiFiClient espClient;
PubSubClient client(espClient);

/*LED GPIO pin*/
int LED_PIN = 2;
int LED_ARRAY_PIN = 18;
int LED_ARRAY_COUNT = 16;

/*LASER GPIO pin*/
//int LASER_PIN_PLUS = 21;
//int LASER_PIN_MINUS = 18;
int LASER_PIN_PLUS = 22;
int LASER_PIN_MINUS = 23;

/*Lens GPIO pins*/
int LENS_X_PIN = 26;
int LENS_Z_PIN = 25;

///* topics - DON'T FORGET TO REGISTER THEM! */
#define LED_TOPIC         "led/onboard"
#define LENS_X_TOPIC      "lens/right/x"
#define LENS_Z_TOPIC      "lens/right/z"
#define LENS_VIBRATE      "lens/both/vibrate"
#define LASER_TOPIC       "laser/red"
#define STEPPER_X_FWD     "stepper/y/fwd"
#define STEPPER_X_BWD     "stepper/y/bwd"
#define LED_ARRAY_TOPIC   "led/array"
#define LENS_X_SOFI       "lens/right/sofi/x"
#define LENS_Z_SOFI       "lens/right/sofi/z"

#define STEPS 200

#define CLIENT_ID "dSTORM_TROMSO";

// global switch for vibrating the lenses
int sofi_periode = 10;  // ms
int sofi_amplitude_x = 0;   // how many steps +/- ?
int sofi_amplitude_z = 0;   // how many steps +/- ?

// default values for x/z lens' positions
int lens_x_int = 0;
int lens_z_int = 0;
int laser_int = 0;

boolean is_sofi_x = false;
boolean is_sofi_z = false;

// Declare our NeoPixel strip object:
Adafruit_NeoPixel strip(LED_ARRAY_COUNT, LED_ARRAY_PIN, NEO_GRB + NEO_KHZ800);

unsigned int highSpeed = 5000;
// PWM Stuff
int pwm_resolution = 15;
int pwm_frequency = 800000;//19000; //12000

// lens x-channel
int PWM_CHANNEL_X = 0;

// lens z-channel
int PWM_CHANNEL_Z = 1;

// laser-channel
int PWM_CHANNEL_LASER = 2;

// MQTT Stuff
long lastMsg = 0;
char msg[20];

void setup() {


  /* set led and laser as output to control led on-off */
  pinMode(LED_PIN, OUTPUT);

  // Visualize, that ESP is on!
  digitalWrite(LED_PIN, HIGH);
  delay(1000);
  digitalWrite(LED_PIN, LOW);

  // switch of the laser directly
  pinMode(LASER_PIN_MINUS, OUTPUT);
  pinMode(LASER_PIN_MINUS, OUTPUT);
  digitalWrite(LASER_PIN_PLUS, LOW);
  digitalWrite(LASER_PIN_MINUS, LOW);

  strip.begin();           // INITIALIZE NeoPixel strip object (REQUIRED)
  strip.show();            // Turn OFF all pixels ASAP
  strip.setBrightness(50); // Set BRIGHTNESS to about 1/5 (max = 255)

  /* setup the PWM ports and reset them to 0*/
  ledcSetup(PWM_CHANNEL_X, pwm_frequency, pwm_resolution);
  ledcAttachPin(LENS_X_PIN, PWM_CHANNEL_X);
  ledcWrite(PWM_CHANNEL_X, 0);

  ledcSetup(PWM_CHANNEL_Z, pwm_frequency, pwm_resolution);
  ledcAttachPin(LENS_Z_PIN, PWM_CHANNEL_Z);
  ledcWrite(PWM_CHANNEL_Z, 0);

  ledcSetup(PWM_CHANNEL_LASER, pwm_frequency, pwm_resolution);
  ledcAttachPin(LASER_PIN_PLUS, PWM_CHANNEL_LASER);
  ledcWrite(PWM_CHANNEL_LASER, 0);


  Serial.begin(115200);
  // We start by connecting to a WiFi network
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);

  WiFi.begin(ssid, password);


  // Connect to Wifi
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }


  // Connect wifi and assign Server IP
  localIP = WiFi.localIP().toString();
  gatewayIP = WiFi.gatewayIP().toString();
  //gatewayIP.toCharArray(MQTT_SERVER, BUFLEN);

  Serial.println("");
  Serial.print("WiFi connected with IP:");
  Serial.println(localIP);
  Serial.print("Default Gateway (MQTT-SERVER):\t");
  Serial.println(MQTT_SERVER);


  /* configure the MQTT server with IPaddress and port */
  client.setServer(MQTT_SERVER, 1883);
  /* this receivedCallback function will be invoked
    when client received subscribed topic */
  client.setCallback(receivedCallback);


  // Start the LED Illuminator
  strip.begin();           // INITIALIZE NeoPixel strip object (REQUIRED)
  strip.show();            // Turn OFF all pixels ASAP
  strip.setBrightness(50); // Set BRIGHTNESS to about 1/5 (max = 255)


  ledson(strip.Color(255,255,255));
  delay(1000);
  ledson(strip.Color(0,0,0));

}

void loop() {
  /* if client was disconnected then try to reconnect again */
  if (!client.connected()) {
    mqttconnect();
  }
  /* this function will listen for incomming
    subscribed topic-process-invoke receivedCallback */
  client.loop();

  // if true, we want to vibrate both lenses along x
  if (is_sofi_x) {
    // move lens in x-direction
    ledcWrite(PWM_CHANNEL_X, lens_x_int - random(-1, 1)*sofi_amplitude_x);
    // Visualize, that ESP is on!
    digitalWrite(LED_PIN, HIGH);
    delay(sofi_periode);
    digitalWrite(LED_PIN, LOW);

  }

  // if true, we want to vibrate both lenses along z
  if (is_sofi_z) {
    // move lens in z-direction
    ledcWrite(PWM_CHANNEL_Z, lens_z_int - random(-1, 1)*sofi_amplitude_z);
    digitalWrite(LED_PIN, HIGH);
    delay(sofi_periode);
    digitalWrite(LED_PIN, LOW);

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



  // Just for debugging
  if (String(topic) == LED_TOPIC)  {
    /* we got '1' -> on */
    if (payload_int == 1) {
      digitalWrite(LED_PIN, HIGH);
    } else {
      /* we got '0' -> on */
      digitalWrite(LED_PIN, LOW);
    }
  }

  if (String(topic) == LENS_X_SOFI)  {
    /* we got '1' -> on */
    if (payload_int == 0) {
      is_sofi_x = false;
      sofi_amplitude_x = 0;
    } else {
      is_sofi_x = true;
      sofi_amplitude_x = abs((int)payload_int);
      Serial.print("Sofi AMplitude X set to: ");
      Serial.print(sofi_amplitude_x);
      Serial.println();
    }
  }

  if (String(topic) == LENS_Z_SOFI)  {
    /* we got '1' -> on */
    if (payload_int == 0) {
      is_sofi_z = false;
      sofi_amplitude_z = 0;
    } else {
      is_sofi_z = true;
      sofi_amplitude_z = abs((int)payload_int);
      Serial.print("Sofi AMplitude X set to: ");
      Serial.print(sofi_amplitude_z);
      Serial.println();
    }
  }

  // Just for debugging
  if (String(topic) == LED_ARRAY_TOPIC)  {
    /* we got '1' -> on */
    if (payload_int == 1) {
      ledson(strip.Color(255,   255,   255));
    } else {
      /* we got '0' -> on */
      ledson(strip.Color(0,   0,   0));
    }
  }

  // Catch the value for movement of lens in X-direction (right)
  if (String(topic) == LASER_TOPIC) {
    //dacWrite(25, (int)payload_int);
    laser_int = abs((int)payload_int);
    ledcWrite(PWM_CHANNEL_LASER, laser_int);
    Serial.print("Laser Intensity is set to: ");
    Serial.print(laser_int);
    Serial.println();

    int led_int = laser_int/128;
    ledson(strip.Color(led_int,led_int,led_int));
    

  
  }


  // Catch the value for movement of lens in X-direction (right)
  if (String(topic) == LENS_X_TOPIC) {
    //dacWrite(25, (int)payload_int);
    lens_x_int = abs((int)payload_int);
    ledcWrite(PWM_CHANNEL_X, lens_x_int);
    Serial.print("Lens (right) X is set to: ");
    Serial.print(lens_x_int);
    Serial.println();
  }

  // Catch the value for movement of lens in Z-direction (right)
  if (String(topic) == LENS_Z_TOPIC) {
    //dacWrite(26, (int)payload_int);
    lens_z_int = abs((int)payload_int);
    ledcWrite(PWM_CHANNEL_Z, lens_z_int);
    Serial.print("Lens (right) Y is set to: ");
    Serial.print(lens_z_int);
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
      client.subscribe(LED_ARRAY_TOPIC);
      client.subscribe(LENS_X_TOPIC);
      client.subscribe(LENS_Z_TOPIC);
      client.subscribe(LENS_VIBRATE);
      client.subscribe(LASER_TOPIC);
      client.subscribe(STEPPER_X_FWD);
      client.subscribe(LENS_X_SOFI);
      client.subscribe(LENS_Z_SOFI);
    } else {
      Serial.print("FAILED, status code =");
      Serial.print(client.state());
      Serial.println("try again in 5 seconds");
      /* Wait 5 seconds before retrying */
      delay(5000);
    }
  }
}







// Fill strip pixels one after another with a color. Strip is NOT cleared
// first; anything there will be covered pixel by pixel. Pass in color
// (as a single 'packed' 32-bit value, which you can get by calling
// strip.Color(red, green, blue) as shown in the loop() function above),
// and a delay time (in milliseconds) between pixels.
void ledson(uint32_t color) {
  for (int i = 0; i < strip.numPixels(); i++) { // For each pixel in strip...
    strip.setPixelColor(i, color);         //  Set pixel's color (in RAM)
    strip.show();                          //  Update strip to match
  }
}


