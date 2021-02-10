/* Here ESP32 will keep 2 roles:
  1/ read data from DHT11/DHT22 sensor
  2/ control led on-off
  So it willpublish temperature topic and scribe topic bulb on/off
*/
#include <Stepper.h>
#include <Adafruit_NeoPixel.h>
#include <Ps3Controller.h>

#ifdef __AVR__
#include <avr/power.h> // Required for 16 MHz Adafruit Trinket
#endif

// define stepper motor control pins
#define IN1  21
#define IN2  17
#define IN3  4
#define IN4  16


/* Motor GPOP pin */
// Define number of steps per rotation:
const int stepsPerRevolution = 2048;

// Create stepper object called 'myStepper', note the pin order:
Stepper stepper_z = Stepper(stepsPerRevolution, IN1, IN2, IN3, IN4);
unsigned int motor_speed = 8;

// PS3 stuff
int player = 0;
int battery = 0;

int stick_value_ry = 0;
int stick_value_shoulder_1 = 0;
int stick_value_shoulder_2 = 0;
int val_analog_stick_x  = 0;
int val_threshold = 3;
int scalingfactor = 2;

int signum(int input) {
  if (input > 0) return 1;
  else if (input ==0) return 0;
  else return -1;
}

/*LED GPIO pin*/
int LED_PIN = 2;
int LED_ARRAY_PIN = 18;
int LED_ARRAY_COUNT = 16;

/*LASER GPIO pin*/
//int LASER_PIN_PLUS = 21;
//int LASER_PIN_MINUS = 18;
int LASER_PIN_PLUS = 23;// 22;
int LASER_PIN_MINUS = 34;//23;
int LASER2_PIN_PLUS = 19;// 22;
int LASER2_PIN_MINUS = 33;//23;

/*Lens GPIO pins*/
int LENS_X_PIN = 26;
int LENS_Z_PIN = 25;

String my_status = "idle";

///* topics - DON'T FORGET TO REGISTER THEM! */
int LED_val = 0;
int LENS_X_val = 0;
int LENS_Z_val = 0;
int LENS_VIBRATE = 0;
int LASER_val = 0;
int LASER_val_2 = 0;
int STEPPER_Z_FWD = 0;
int STEPPER_Z_BWD = 0;
int LED_ARRAY_val = 0;
int LENS_X_SOFI = 0;
int LENS_Z_SOFI = 0;
int STATE = 0;
int STEPS = 200;
int SPEED = 0;

int lensval_x = 0;
int lensval_z = 0;
int laser_diff = 0;
// global switch for vibrating the lenses
int sofi_periode = 100;  // ms
int sofi_amplitude_x = 0;   // how many steps +/- ?
int sofi_amplitude_z = 0;   // how many steps +/- ?

// default values for x/z lens' positions
int lens_x_int = 0;
int lens_z_int = 0;
int laser_int = 0;
int laser2_int = 0;
int lens_x_offset = 0;
int lens_z_offset = 0;//1000;

boolean is_sofi_x = false;
boolean is_sofi_z = false;

// PWM Stuff
int pwm_resolution = 15;
int pwm_frequency = 800000;//19000; //12000
int pwm_max = (int)pow(2,pwm_resolution);
// lens x-channel
int PWM_CHANNEL_X = 0;

// lens z-channel
int PWM_CHANNEL_Z = 1;

// laser-channel
int PWM_CHANNEL_LASER = 2;

// laser-channel
int PWM_CHANNEL_LASER_2 = 3;



void setup() {
  /* set led and laser as output to control led on-off */
  pinMode(LED_PIN, OUTPUT);

  // switch of the laser directly
  pinMode(LASER_PIN_MINUS, OUTPUT);
  pinMode(LASER_PIN_MINUS, OUTPUT);
  digitalWrite(LASER_PIN_PLUS, LOW);
  digitalWrite(LASER_PIN_MINUS, LOW);
  digitalWrite(LASER2_PIN_PLUS, LOW);
  digitalWrite(LASER2_PIN_MINUS, LOW);

  // Visualize, that ESP is on!
  digitalWrite(LED_PIN, HIGH);
  delay(1000);
  digitalWrite(LED_PIN, LOW);


  // Set the speed of the motor:
  stepper_z.setSpeed(motor_speed);
  // Set the speed to 5 rpm:
  stepper_z.step(50);
  stepper_z.step(-50);

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

  ledcSetup(PWM_CHANNEL_LASER_2, pwm_frequency, pwm_resolution);
  ledcAttachPin(LASER2_PIN_PLUS, PWM_CHANNEL_LASER_2);
  ledcWrite(PWM_CHANNEL_LASER_2, 0);

  Serial.begin(115200);
  // We start by connecting to a WiFi network

  // test lenses
  ledcWrite(PWM_CHANNEL_Z, 5000);
  ledcWrite(PWM_CHANNEL_X, 5000);
  delay(500);

  //Set the lenses to their offset level
  ledcWrite(PWM_CHANNEL_Z, lens_z_offset);
  ledcWrite(PWM_CHANNEL_X, lens_x_offset);

  if (!Ps3.begin("01:02:03:04:05:06")) {
    Serial.println("Initialization failed.");
    return;
  }

  Serial.println("Initialization finished.");

  Ps3.attach(onEvent);
  Ps3.attachOnConnect(onConnection);

}

void loop() {
  // if true, we want to vibrate both lenses along x
  if (is_sofi_x) {
    // move lens in x-direction
    ledcWrite(PWM_CHANNEL_X, LENS_X_val + lens_x_offset - random(-sofi_amplitude_x, sofi_amplitude_x));
    // Visualize, that ESP is on!
    digitalWrite(LED_PIN, HIGH);
    delay(sofi_periode);
    digitalWrite(LED_PIN, LOW);
  }

  // if true, we want to vibrate both lenses along z
  if (is_sofi_z) {
    // move lens in z-direction
    ledcWrite(PWM_CHANNEL_Z, LENS_Z_val + lens_z_offset - random(-sofi_amplitude_z, sofi_amplitude_z));
    // Visualize, that ESP is on!
    digitalWrite(LED_PIN, HIGH);
    delay(sofi_periode);
    digitalWrite(LED_PIN, LOW);
  }
// Move motor
  if (abs(SPEED) > 0) {
    Serial.println("Running Motor with steps:");
    stepper_z.step(signum(SPEED));
  }

// Move X lens
  if (((LENS_X_val + lensval_x + lens_x_offset) >= 0) and 
  ((LENS_X_val + lensval_x + lens_x_offset) < pwm_max)and 
  (abs(lensval_x)>0)) {
    // if the limit has not been reached, add the new value

// for fine movement only
    if (lensval_x == -5000) {
      // dirty hack around since the event detector takes many loops
      LENS_X_val -= 1;
      lensval_x = 0;
    }
    if (lensval_x == 5000) {
      // dirty hack around since the event detector takes many loops
      LENS_X_val += 1;
      lensval_x = 0;
    }
    LENS_X_val += lensval_x;
    // apply a nonlinear look-up table
    ledcWrite(PWM_CHANNEL_X, (int)(pow(((float)(LENS_X_val + lens_x_offset)/(float)pwm_max),2)*(float)pwm_max));
    Serial.print("Lens (right) X is set to: ");
    Serial.print((int)(pow(((float)(LENS_X_val + lens_x_offset)/(float)pwm_max),2)*(float)pwm_max), DEC);
    Serial.println();
  }

// Move Z lens
  if (((LENS_Z_val + lensval_z + lens_z_offset) >= 0) and 
  ((LENS_Z_val + lensval_z + lens_z_offset) < pwm_max) and
  (abs(lensval_z)>0)) {
   // if the limit has not been reached, add the new value
    LENS_Z_val += lensval_z;
    ledcWrite(PWM_CHANNEL_X, LENS_Z_val + lens_z_offset);
    Serial.print("Lens (right) Z is set to: ");
    Serial.print(LENS_Z_val + lens_z_offset, DEC);
    Serial.println();
  }
}


void onEvent()
{
  /*
    int LED_val = 0;
    int LENS_X_val = 0;
    int LENS_Z_val = 0;
    int LENS_VIBRATE = 0;
    int LASER_val = 0;
    int LASER_val_2 = 0;
    int STEPPER_Z_FWD = 0;
    int STEPPER_Z_BWD = 0;
    int LED_ARRAY_val = 0;
    int LENS_X_SOFI = 0;
    int LENS_Z_SOFI = 0;
    int STATE = 0;
    int STEPS = 200;
    int SPEED = 0;

    }*/
  //--- Digital cross/square/triangle/circle button events ---
  if ( Ps3.event.button_down.cross ){
    Serial.println("Sofi Z on");
    is_sofi_z = true;
  }
  //Sofi Z on
  if ( Ps3.event.button_down.square ){
    Serial.println("Sofi Z off");
    is_sofi_z = false;
  }
    
  //Sofi Z off
  if ( Ps3.event.button_down.triangle ) {
    Serial.println("Sofi x on");
    is_sofi_x = true;
  }
    //Sofi X on
  if ( Ps3.event.button_down.circle ){
    Serial.println("Sofi x off");
    is_sofi_x = false;
  //Sofi X off
  }
    
  //--------------- Digital D-pad button events --------------
  if ( Ps3.event.button_down.up ) {
    laser_diff = -2;
    Serial.println("Laser on");
  }
  else if ( Ps3.event.button_down.down ) {
    laser_diff = -1;
    Serial.println("Laser off");
  }
    if ( Ps3.event.button_up.up ) {
    laser_diff = 0;
    }
  else if ( Ps3.event.button_down.up ) {
    laser_diff = 0;
    
  }
  else if ( Ps3.event.button_down.right ) {
    Serial.print ("Laser ++ ");
    laser_diff = 50;
  }
  else if ( Ps3.event.button_up.right) {
    Serial.print ("Laser 0 ");
    laser_diff = 0;
  }
  else if ( Ps3.event.button_down.left ) {
    Serial.print ("Laser -- ");
    laser_diff = -50;
  }
  else if ( Ps3.event.button_up.left) {
    Serial.print ("Laser 0 ");
    laser_diff = 0;
  }

  if (laser_diff==-1) {
    LASER_val = 0;
    ledcWrite(PWM_CHANNEL_LASER, LASER_val);
    Serial.print("Laser val: ");
    Serial.println(LASER_val);
  }
  else if (laser_diff==-2) {
    LASER_val = pwm_max - 10;
    ledcWrite(PWM_CHANNEL_LASER, LASER_val);
  }  
  else if (((LASER_val+laser_diff) <= (pwm_max - 10)) and 
  ((LASER_val+laser_diff) >= 0) and 
  (abs(laser_diff)!=0)){
    LASER_val += laser_diff;
    ledcWrite(PWM_CHANNEL_LASER, LASER_val);
    Serial.print("Laser val: ");
    Serial.println(LASER_val);
  }
  

  //---------------- LENS x ---------------
  if ( abs(Ps3.event.analog_changed.stick.lx) + abs(Ps3.event.analog_changed.stick.ly) > 2 ) {
    SPEED = Ps3.data.analog.stick.ly;

    // if the joystic is in the middle ===> stop the motor
    if ( abs(SPEED) < 10)
    {
      Serial.println("Stopping the motor");
      digitalWrite(IN1, LOW);
      digitalWrite(IN2, LOW);
      digitalWrite(IN3, LOW);
      digitalWrite(IN4, LOW);
      SPEED = 0;
    }
    else
    {
      // set motor speed
      stepper_z.setSpeed((int)((float)abs(SPEED)/10.));
      Serial.println(SPEED, DEC);
    }
  }

  if ( abs(Ps3.event.analog_changed.stick.rx) + abs(Ps3.event.analog_changed.stick.ry) > 2 ) {
    stick_value_ry = -Ps3.data.analog.stick.ry;
  }

  if  (abs(stick_value_ry) < 20) {
    // sometimes the controller does not go back to zero properly
    lensval_x = 0;
  }
  else if (abs(stick_value_ry) < 50) {
    lensval_x = signum(stick_value_ry);
    // here you want to have fine scannning.. either +/- 1
  }
  else  {
    // just take the value
    lensval_x = (int)((float)stick_value_ry/2.);
  }

// fine changes using shoulders
  if ( abs(Ps3.event.button_down.r1) ) {
    lensval_x = 5000;
  }
  if ( abs(Ps3.event.button_up.r1) ) {
    lensval_x = 0;
  }
  if ( abs(Ps3.event.button_down.l1) ) {
    lensval_x = -5000;
  }
  if ( abs(Ps3.event.button_up.l1) ) {
    lensval_x = 0;
  }  



  //---------- LENS Z using the shoulder  ----------
  if ( abs(Ps3.event.analog_changed.button.r2) ) {
    stick_value_shoulder_1 = Ps3.data.analog.button.r2;
  }
  if ( abs(Ps3.event.analog_changed.button.l2) ) {
    stick_value_shoulder_2 = -Ps3.data.analog.button.l2;
  }

// check for backlash
  if  ((abs(stick_value_shoulder_1) < 3) and (abs(stick_value_shoulder_2) < 3))  {
    // sometimes the controller does not go back to zero properly
    lensval_z = 0;
  }
  // add only small values
  else if ((abs(stick_value_shoulder_1) < 10) or (abs(stick_value_shoulder_2) <10)) {
    // here you want to have fine scannning.. either +/- 1
    if(abs(stick_value_shoulder_1)) lensval_z = signum(stick_value_shoulder_1);
    if(abs(stick_value_shoulder_2)) lensval_z = signum(stick_value_shoulder_2); 
  }
  else  {
    // just take the value
    lensval_z =  (int)((float)stick_value_shoulder_1/2. + (float)stick_value_shoulder_2/2.);
  }
}



void onConnection() {

  if (Ps3.isConnected()) {
    Serial.println("Controller connected.");
  }
}
