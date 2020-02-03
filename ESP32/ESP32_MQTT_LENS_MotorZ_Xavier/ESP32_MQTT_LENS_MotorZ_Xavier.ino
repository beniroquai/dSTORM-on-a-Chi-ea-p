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
int LED_PIN = 2;

/*LASER GPIO pin*/
int LASER_PIN_PLUS = 22;
int LASER_PIN_MINUS = 23;

/*Lens GPIO pins*/
int LENS_X_PIN = 25;
int LENS_Z_PIN = 26;

///* topics - DON'T FORGET TO REGISTER THEM! */
#define LED_TOPIC         "lens/left/led"
#define LENS_X_TOPIC      "lens/right/x"
#define LENS_Z_TOPIC      "lens/right/z"
#define LENS_VIBRATE      "lens/both/vibrate"
#define LASER_TOPIC       "laser/red"
#define STEPPER_X_FWD     "stepper/y/fwd"
#define STEPPER_X_BWD     "stepper/y/bwd"

#define STEPS 200

#define CLIENT_ID "ESP32Client_bene";

// global switch for vibrating the lenses
int sofi_periode = 10;  // ms
int sofi_state = false;   // is sofi turned on?
int sofi_amplitude = 1;   // how many steps +/- ?

// default values for x/z lens' positions
int lens_x_int = 0;
int lens_z_int = 0;
int laser_int = 0;

// create an instance of the stepper class, specifying
// the number of steps of the motor and the pins it's
// attached to
//Stepper STP_X(STEPS,5,21,18,19);
//int motorPin_X[] = {5,21,18,19};
int motorPin_X[] = {1,1,1,1};
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

  // MOTOR
  pinMode(motorPin_X[0], OUTPUT);
  pinMode(motorPin_X[1], OUTPUT);
  pinMode(motorPin_X[2], OUTPUT);
  pinMode(motorPin_X[3], OUTPUT);

  // switch of the laser directly
  pinMode(LASER_PIN_MINUS, OUTPUT);
  pinMode(LASER_PIN_MINUS, OUTPUT);
  digitalWrite(LASER_PIN_PLUS, LOW);
  digitalWrite(LASER_PIN_MINUS, LOW);


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

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  
  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());

  /* configure the MQTT server with IPaddress and port */
  client.setServer(mqtt_server, 1883);
  /* this receivedCallback function will be invoked
    when client received subscribed topic */
  client.setCallback(receivedCallback);

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
  if (sofi_state) {
    // if value is too small, lens toggles in wrong direction => add some value!
    if(lens_x_int - sofi_amplitude / 2) lens_x_int += sofi_amplitude / 2;

    // move lens in plus-direction
    ledcWrite(PWM_CHANNEL_X, lens_x_int + sofi_amplitude / 2);

    // DEBUGGING: SHOW LED flasshing
    digitalWrite(LED_PIN, HIGH);
    delay(sofi_periode);
    
    // move lens in plus-direction
    ledcWrite(PWM_CHANNEL_X, lens_x_int - sofi_amplitude / 2);

    // DEBUGGING: SHOW LED flasshing
    digitalWrite(LED_PIN, LOW);
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

  // Catch the value for movement of lens in X-direction (right)
  if (String(topic) == LASER_TOPIC) {
    //dacWrite(25, (int)payload_int);
    laser_int = abs((int)payload_int);
    ledcWrite(PWM_CHANNEL_LASER, laser_int);
    Serial.print("Laser Intensity is set to: ");
    Serial.print(laser_int);
    Serial.println();
  }
  

   // Catch the value for stepment of lens in X-direction
  if (String(topic) == STEPPER_X_FWD) {
    // Drive motor X in positive direction
    drive_left(highSpeed, motorPin_X,(int)payload_int*10);
    stop(motorPin_X);
    //STP_X.step((int)payload_int*10);
    Serial.print("Motor is running in x for: ");
    Serial.print((int)payload_int);
    Serial.println();
  }

  // Catch the value for stepment of lens in Y-direction
  if (String(topic) == STEPPER_X_BWD) {
      // Drive motor X in positive direction
    drive_right(highSpeed, motorPin_X,(int)payload_int*10);
    stop(motorPin_X);
    //    STP_X.step(-(int)payload_int*10);
    Serial.print("Motor is running in x for: ");
    Serial.print((int)payload_int);
    Serial.println();
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
      client.subscribe(LENS_X_TOPIC);
      client.subscribe(LENS_Z_TOPIC);
      client.subscribe(LENS_VIBRATE);
      client.subscribe(LASER_TOPIC);
      client.subscribe(STEPPER_X_FWD);
      client.subscribe(STEPPER_X_BWD);

    } else {
      Serial.print("faiLED_PIN, status code =");
      Serial.print(client.state());
      Serial.println("try again in 5 seconds");
      /* Wait 5 seconds before retrying */
      delay(5000);
    }
  }
}








void drive_right(unsigned int motorSpeed, int motorPin[], int steps)
{ // 1

  int motorPin1 = motorPin[0];
  int motorPin2 = motorPin[1];
  int motorPin3 = motorPin[2];
  int motorPin4 = motorPin[3];

  for (int i = 0; i < steps; i++)
  {
    digitalWrite(motorPin4, HIGH);
    digitalWrite(motorPin3, LOW);
    digitalWrite(motorPin2, LOW);
    digitalWrite(motorPin1, LOW);
    delayMicroseconds(motorSpeed);

    // 2
    digitalWrite(motorPin4, HIGH);
    digitalWrite(motorPin3, HIGH);
    digitalWrite(motorPin2, LOW);
    digitalWrite(motorPin1, LOW);
    delayMicroseconds(motorSpeed);

    // 3
    digitalWrite(motorPin4, LOW);
    digitalWrite(motorPin3, HIGH);
    digitalWrite(motorPin2, LOW);
    digitalWrite(motorPin1, LOW);
    delayMicroseconds(motorSpeed);

    // 4
    digitalWrite(motorPin4, LOW);
    digitalWrite(motorPin3, HIGH);
    digitalWrite(motorPin2, HIGH);
    digitalWrite(motorPin1, LOW);
    delayMicroseconds(motorSpeed);

    // 5
    digitalWrite(motorPin4, LOW);
    digitalWrite(motorPin3, LOW);
    digitalWrite(motorPin2, HIGH);
    digitalWrite(motorPin1, LOW);
    delayMicroseconds(motorSpeed);

    // 6
    digitalWrite(motorPin4, LOW);
    digitalWrite(motorPin3, LOW);
    digitalWrite(motorPin2, HIGH);
    digitalWrite(motorPin1, HIGH);
    delayMicroseconds(motorSpeed);

    // 7
    digitalWrite(motorPin4, LOW);
    digitalWrite(motorPin3, LOW);
    digitalWrite(motorPin2, LOW);
    digitalWrite(motorPin1, HIGH);
    delayMicroseconds(motorSpeed);

    // 8
    digitalWrite(motorPin4, HIGH);
    digitalWrite(motorPin3, LOW);
    digitalWrite(motorPin2, LOW);
    digitalWrite(motorPin1, HIGH);
    delayMicroseconds(motorSpeed);
  }
}

void drive_left(unsigned int motorSpeed, int motorPin[], int steps)
{ // 1

  int motorPin1 = motorPin[0];
  int motorPin2 = motorPin[1];
  int motorPin3 = motorPin[2];
  int motorPin4 = motorPin[3];

  for (int i = 0; i < steps; i++)
  {
    // 1
    digitalWrite(motorPin1, HIGH);
    digitalWrite(motorPin2, LOW);
    digitalWrite(motorPin3, LOW);
    digitalWrite(motorPin4, LOW);
    delayMicroseconds(motorSpeed);

    // 2
    digitalWrite(motorPin1, HIGH);
    digitalWrite(motorPin2, HIGH);
    digitalWrite(motorPin3, LOW);
    digitalWrite(motorPin4, LOW);
    delayMicroseconds(motorSpeed);

    // 3
    digitalWrite(motorPin1, LOW);
    digitalWrite(motorPin2, HIGH);
    digitalWrite(motorPin3, LOW);
    digitalWrite(motorPin4, LOW);
    delayMicroseconds(motorSpeed);

    // 4
    digitalWrite(motorPin1, LOW);
    digitalWrite(motorPin2, HIGH);
    digitalWrite(motorPin3, HIGH);
    digitalWrite(motorPin4, LOW);
    delayMicroseconds(motorSpeed);

    // 5
    digitalWrite(motorPin1, LOW);
    digitalWrite(motorPin2, LOW);
    digitalWrite(motorPin3, HIGH);
    digitalWrite(motorPin4, LOW);
    delayMicroseconds(motorSpeed);

    // 6
    digitalWrite(motorPin1, LOW);
    digitalWrite(motorPin2, LOW);
    digitalWrite(motorPin3, HIGH);
    digitalWrite(motorPin4, HIGH);
    delayMicroseconds(motorSpeed);

    // 7
    digitalWrite(motorPin1, LOW);
    digitalWrite(motorPin2, LOW);
    digitalWrite(motorPin3, LOW);
    digitalWrite(motorPin4, HIGH);
    delayMicroseconds(motorSpeed);

    // 8
    digitalWrite(motorPin1, HIGH);
    digitalWrite(motorPin2, LOW);
    digitalWrite(motorPin3, LOW);
    digitalWrite(motorPin4, HIGH);
    delayMicroseconds(motorSpeed);
  }
}


void stop(int motorPin[])
{
  int motorPin1 = motorPin[0];
  int motorPin2 = motorPin[1];
  int motorPin3 = motorPin[2];
  int motorPin4 = motorPin[3];

  digitalWrite(motorPin4, LOW);
  digitalWrite(motorPin3, LOW);
  digitalWrite(motorPin2, LOW);
  digitalWrite(motorPin1, LOW);
}
