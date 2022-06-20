#include <Ps3Controller.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>


//#define home_wifi
#ifdef home_wifi
const char *ssid = "BenMur";
const char *password = "MurBen3128";
#else
const char* ssid = "Blynk";
const char* password = "12345678";
#endif


//Your Domain name with URL path or IP address with path
const char* SERVER_URL = "http://192.168.43.138"; // IP: 192.168.43.138 host: ESPLENS


int maxpwm = 65536;

// DEFINING VALUE PAIRS
String PAYLOAD_LENS = "lens_value";
String PAYLOAD_LED_red = "red";
String PAYLOAD_LED_blue = "blue";
String PAYLOAD_LED_green = "green";
String PAYLOAD_MOVE_SPEED = "speed";
String PAYLOAD_MOVE_STEPS = "steps";
String PAYLOAD_LASER = "value";

// Defining Endpoints
String POST_MOVE_Z = "/move_z";
String POST_LENS_X = "/lens_x";
String POST_LENS_Z = "/lens_z";
String POST_LASER = "/laser";
String POST_LASER_RED = "/laser_red";
String POST_LASER_BLUE = "/laser_blue";
String POST_LASER_GREEN = "/laser_green";

int offset_val = 30;
int lens_z = 0;
int lens_x = 0;
boolean is_laser_red = false;
boolean is_sofi = false;
int stick_ly = 0;
int laser_power = 0;

WiFiClient client;
HTTPClient http;


static inline int8_t sgn(int val) {
  if (val < 0) return -1;
  if (val == 0) return 0;
  return 1;
}


void onConnect() {
  Serial.println("Connected.");
}


void post_laser(int laserval, String which_laser) {
  // Your Domain name with URL path or IP address with path

  http.begin(client, SERVER_URL + which_laser);

  // Specify content-type header
  http.addHeader("Content-Type", "application/json");

  // add payload
  StaticJsonDocument<250> payload;
  char buffer[250];
  payload.clear();
  payload[PAYLOAD_LASER] = laserval;
  serializeJson(payload, buffer);

  int httpResponseCode = http.POST(buffer);

  // Free resources
  http.end();
}


void post_lens(int lensval, String which_lens) {
  // Your Domain name with URL path or IP address with path


  http.begin(client, SERVER_URL + which_lens);

  // Specify content-type header
  http.addHeader("Content-Type", "application/json");

  // add payload
  StaticJsonDocument<250> payload;
  char buffer[250];
  payload.clear();
  payload[PAYLOAD_LENS] = lensval;
  serializeJson(payload, buffer);

  int httpResponseCode = http.POST(buffer);

  // Free resources
  http.end();
}


void post_motor(int steps, int speed) {
  // Your Domain name with URL path or IP address with path

  http.begin(client, SERVER_URL + POST_MOVE_Z);

  // Specify content-type header
  http.addHeader("Content-Type", "application/json");

  // add payload
  StaticJsonDocument<250> payload;
  char buffer[250];
  payload.clear();
  payload[PAYLOAD_MOVE_SPEED] = speed;
  payload[PAYLOAD_MOVE_STEPS] = steps;
  serializeJson(payload, buffer);

  // DEBUGGING
  Serial.print("Endpoint: ");
  Serial.println(POST_MOVE_Z);
  Serial.print("payload: ");
  Serial.println(buffer);

  int httpResponseCode = http.POST(buffer);

  Serial.print("HTTP Response code: ");
  Serial.println(httpResponseCode);

  // Free resources
  http.end();
}



void setup() {
  Serial.begin(115200);         // Start the Serial communication to send messages to the computer
  Serial.print("Connecting to:...");
  Serial.println(ssid);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED)
  {
    delay(100);
    Serial.print(".");
  }
  delay(100);
  Serial.print("Connected to ");
  Serial.println(WiFi.SSID());              // Tell us what network we're connected to
  Serial.print("IP address:\t");
  Serial.println(WiFi.localIP());           // Send the IP address of the ESP8266 to the computer

  //Ps3.attach(notify);
  Ps3.attachOnConnect(onConnect);
  Ps3.begin("01:02:03:04:05:06");

  Serial.println("Ready.");
}


void loop() {
  if (Ps3.isConnected()) {
    
    /*
     * ANALOG LEFT
     */
    if ( abs(Ps3.data.analog.stick.lx) > offset_val) {
      // unused
      int stick_lx = Ps3.data.analog.stick.lx;
      stick_lx = stick_lx - sgn(stick_lx) * offset_val;
      Serial.println(stick_lx);
    }

    if ( abs(Ps3.data.analog.stick.ly) > offset_val) {
      // move_z
      stick_ly = Ps3.data.analog.stick.ly;
      stick_ly = stick_ly - sgn(stick_ly) * offset_val;
      post_motor(0, stick_ly * 20);
      //post_motor((int)pow((float)stick_ly, 1.5), (int)pow((float)stick_ly, 1.5));
      Serial.println(stick_ly);
    }
    else {
      if (abs(stick_ly) > 0) {
        stick_ly = 0;
        post_motor(0, 0); // switch motor off;
      }
    }
  }


  /*
   * Keypad left
   */
  if ( Ps3.data.button.left) {
    // fine lens -
    lens_x -= 1;
    post_lens(lens_x, POST_LENS_X);
  }
  if ( Ps3.data.button.right) {
    // fine lens +
    lens_x += 1;
    post_lens(lens_x, POST_LENS_X);
  }
  if ( Ps3.data.button.down) {
    // fine focus +
    post_motor(10, 10);
    //post_motor(0,0);
  }
  if ( Ps3.data.button.up) {
    // fine focus -
    post_motor(-10, -10);
    //post_motor(0,0);
  }
  if ( Ps3.data.button.start) {
    // reset
    lens_z = 0;
    lens_x = 0;
    post_lens(lens_z, POST_LENS_Z);
    post_lens(lens_x, POST_LENS_X);
    is_laser_red = false;
    laser_power = 0;
    post_laser(0, POST_LASER);
  }

  int offset_val_shoulder = 5;
  if ( abs(Ps3.data.analog.button.r2) > offset_val_shoulder) {
    // lens_x++ coarse 
    /*
    int stick_rx = Ps3.data.analog.button.r2;
    stick_rx = stick_rx - sgn(stick_rx) * offset_val_shoulder;
    int stick_rx_norm = sgn(stick_rx) * (int)(pow(((float)(stick_rx) / (float)(128 - offset_val_shoulder)), 2) * 1000);
    if (((lens_x + stick_rx_norm) <= maxpwm) and  ((lens_x + stick_rx) >= 0)) {
      lens_x += abs(stick_rx_norm);
    }
    */
    lens_x +=1000;
    post_lens(lens_x, POST_LENS_X);
    Serial.println(lens_x);
  }
  
  if ( abs(Ps3.data.analog.button.l2) > offset_val_shoulder) {
    // lens_x-- coarse
    /*
    int stick_rx = Ps3.data.analog.button.l2;
    stick_rx = stick_rx - sgn(stick_rx) * offset_val_shoulder;
    int stick_rx_norm =  (int)(pow(((float)(stick_rx) / (float)(128 - offset_val_shoulder)), 2) * 1000);
    if (((lens_x - stick_rx_norm) <= maxpwm) and  ((lens_x - stick_rx) >= 0)) {
      lens_x -= abs(stick_rx_norm);
    }
    */
    lens_x -=1000;
    post_lens(lens_x, POST_LENS_X);
    Serial.println(lens_x);
  }

  
  if ( abs(Ps3.data.analog.button.r1) > offset_val_shoulder) {
    // lens_x + semi coarse
    lens_x += 100;
    post_lens(lens_x, POST_LENS_X);
  }
  if ( abs(Ps3.data.analog.button.l1) > offset_val_shoulder) {
    // lens_x - semi coarse
    lens_x -= 100;
    post_lens(lens_x, POST_LENS_X);
  }


/*

  if ( abs(Ps3.data.analog.button.r2) > offset_val) {
    // lens_x++
    int stick_rx = Ps3.data.analog.button.r2;
    stick_rx = stick_rx - sgn(stick_rx) * offset_val;
    int stick_rx_norm = sgn(stick_rx) * (int)(pow(((float)(stick_rx) / (float)(128 - offset_val)), 2) * 1000);
    if (((lens_x + stick_rx_norm) <= maxpwm) and  ((lens_x + stick_rx) >= 0)) {
      lens_x += abs(stick_rx_norm);
    }
    post_lens(lens_x, POST_LENS_X);
    Serial.println(stick_rx_norm);
  }
  
  if ( abs(Ps3.data.analog.button.l2) > offset_val) {
    // lens_x--
    int stick_rx = Ps3.data.analog.button.l2;
    stick_rx = stick_rx - sgn(stick_rx) * offset_val;
    int stick_rx_norm =  (int)(pow(((float)(stick_rx) / (float)(128 - offset_val)), 2) * 1000);
    if (((lens_x - stick_rx_norm) <= maxpwm) and  ((lens_x - stick_rx) >= 0)) {
      lens_x -= abs(stick_rx_norm);
    }
    post_lens(lens_x, POST_LENS_X);
    Serial.println(stick_rx_norm);
  }

  
  if ( abs(Ps3.data.analog.button.r1) > offset_val) {
    // lens_z ++
    int stick_ry = Ps3.data.analog.button.r1;
    stick_ry = stick_ry - sgn(stick_ry) * offset_val;
    int stick_ry_norm = (int)(pow(((float)(stick_ry) / (float)(128 - offset_val)), 2) * 1000);
    if (((lens_z + stick_ry_norm) <= maxpwm) and  ((lens_z + stick_ry) >= 0)) {
      lens_z += abs(stick_ry_norm);
    }
    post_lens(lens_z, POST_LENS_Z);
    Serial.print("Lens Z");
    Serial.println(stick_ry_norm);
  }
  if ( abs(Ps3.data.analog.button.l1) > offset_val) {
    // lens_z --
    int stick_ry =  Ps3.data.analog.button.l1;
    stick_ry = stick_ry - sgn(stick_ry) * offset_val;
    int stick_ry_norm = (int)(pow(((float)(stick_ry) / (float)(128 - offset_val)), 2) * 1000);
    if (((lens_z - stick_ry_norm) <= maxpwm) and  ((lens_z - stick_ry) >= 0)) {
      lens_z -= abs(stick_ry_norm);
    }
    post_lens(lens_z, POST_LENS_Z);
    Serial.print("Lens Z");
    Serial.println(stick_ry_norm);
  }

*/

  if ( Ps3.data.button.circle ) {
    //if(not is_laser_red){
    Serial.println("Laser on");
    is_laser_red = true;
    laser_power += 200;
    delay(100);
    post_laser(laser_power, POST_LASER);
    //}

  }

  if ( Ps3.data.button.cross ) {
    if (is_laser_red) {
      Serial.println("Laser off");
      is_laser_red = false;
      post_laser(0, POST_LASER);
    }

  }

  if ( Ps3.data.button.triangle) {
    if (not is_sofi) {
      Serial.println("SOFI on");
      is_sofi = true;
    }
  }

  if ( Ps3.data.button.square ) {
    if (is_sofi) {
      is_sofi = false;
      Serial.println("SOFI off");
    }

  }

}
