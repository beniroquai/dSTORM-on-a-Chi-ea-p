package de.nanoimaging.stormcontroler;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


/*
 +====================== Android MQTT Controller =======================+
 |                                                                      |
 |           Simple remote control app for IoT purposes                 |
 |         This app comunicates with a remote cloud MQTT broker         |
 |            Participate, find help, info, and more at:                |
 |                                                                      |
 |   -------> https://github.com/ismenc/esp8266-mqtt-control <-------   |
 |   -----> https://github.com/ismenc/android-mqtt-controller <------   |
 |                                                                      |
 +======================================================================+
 */
public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    /*
     * Todo Replace togglebutton with button
     * Todo Method that checks the lights status over MQTT
	 * Todo On button click, check lights status and then switch status
     * Todo Minor validations and use cases
	 * Todo Move methods to MqttUtil for better modularization
     */

    MqttAndroidClient mqttAndroidClient;

    // Server uri follows the format tcp://ipaddress:port
    String serverUri = "tcp://192.168.43.88";

    final String mqttUser = "username";
    final String mqttPass = "pi";

    final String clientId = "Mobile";

    boolean is_vibration = false;
    // TAG
    String TAG = "dSTORM-on-a-chieap";

    // MQTT Topics
    public static final String topic_x_left = "lens/left/x";
    public static final String topic_x_right = "lens/right/x";
    public static final String topic_z_left = "lens/left/z";
    public static final String topic_z_right = "lens/right/z";
    public static final String topic_both_vibrate = "lens/both/vibrate";
    public static final String topic_laser_left = "laser/left/state";
    public static final String topic_laser_right = "laser/right/state";
    public static final String topic_stepper_y_fwd = "stepper/y/fwd";
    public static final String topic_stepper_y_bwd = "stepper/y/bwd";
    public static final String topic_stepper_x_fwd = "stepper/x/fwd";
    public static final String topic_stepper_x_bwd = "stepper/x/bwd";

    // PWM settings
    int PWM_resolution = 32768 - 1; // bitrate of the PWM signal
    int myperiode = 20; // time to pause between toggling
    int myamplitude = 20; // amplitude of the lens in each periode
    int coarse_increment = 20; // steps for ++/--


    // Handle long-press events - want to increment by long-press
    private boolean mAutoIncrement = false;
    private boolean mAutoDecrement = false;
    private Handler repeatUpdateHandler = new Handler();
    static int REP_DELAY = 150;

    String mSwitchCaseLens = "";

    // Seekbars
    private SeekBar seekbar_z_left;
    private SeekBar seekbar_x_left;
    private SeekBar seekbar_x_right;
    private SeekBar seekbar_z_right;

    TextView textViewXRight;
    TextView textViewZRight;
    TextView textViewZLeft;
    TextView textViewXLeft;

    // Buttons
    Button lightsButtonLeft;
    Button lightsButtonRight;

    Button button_x_fwd_coarse;
    Button button_x_fwd_fine;
    Button button_x_bwd_coarse;
    Button button_x_bwd_fine;

    Button button_y_fwd_coarse;
    Button button_y_fwd_fine;
    Button button_y_bwd_coarse;
    Button button_y_bwd_fine;

    ToggleButton button_sofi;
    ToggleButton button_laser_left;
    ToggleButton button_laser_right;

    Button button_z_left_plus;
    Button button_z_left_minus;
    Button button_x_left_plus;
    Button button_x_left_minus;
    Button button_x_right_plus;
    Button button_x_right_minus;
    Button button_z_right_plus;
    Button button_z_right_minus;
    Button button_z_left_plus2;
    Button button_z_left_minus2;
    Button button_x_left_plus2;
    Button button_x_left_minus2;
    Button button_x_right_plus2;
    Button button_x_right_minus2;
    Button button_z_right_plus2;
    Button button_z_right_minus2;
    Button button_ip_address_go;

    EditText EditTextIPAddress;

    // Save the state of the progress bar
    int val_lens_x_right = 0;
    int val_lens_z_right = 0;
    int val_lens_z_left = 0;
    int val_lens_x_left = 0;

    boolean state_sofi = false;
    boolean state_laser_left = false;
    boolean isState_laser_right = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_view);

        class RptUpdater implements Runnable {
            public void run() {
                if (mAutoIncrement) {
                    increment();
                    repeatUpdateHandler.postDelayed(new RptUpdater(), REP_DELAY);
                } else if (mAutoDecrement) {
                    decrement();
                    repeatUpdateHandler.postDelayed(new RptUpdater(), REP_DELAY);
                }
            }
        }


        // Register GUI components
        lightsButtonLeft = findViewById(R.id.button_lights_left);
        //lightsButtonRight = findViewById(R.id.button_lights_right);

        EditTextIPAddress = (EditText) findViewById(R.id.editText_ip_address);
        button_x_fwd_coarse = findViewById(R.id.button_x_fwd_coarse);
        button_x_fwd_fine = findViewById(R.id.button_x_fwd_fine);
        button_x_bwd_coarse = findViewById(R.id.button_x_bwd_coarse);
        button_x_bwd_fine = findViewById(R.id.button_x_bwd_fine);
        button_y_fwd_coarse = findViewById(R.id.button_y_fwd_coarse);
        button_y_fwd_fine = findViewById(R.id.button_y_fwd_fine);
        button_y_bwd_coarse = findViewById(R.id.button_y_bwd_coarse);
        button_y_bwd_fine = findViewById(R.id.button_y_bwd_fine);
        button_ip_address_go = findViewById(R.id.button_ip_address_go);

        // toggle buttons
        button_sofi = findViewById(R.id.button_vibrate);
        button_sofi.setText("SOFI: 0");
        button_sofi.setTextOn("SOFI: 1");
        button_sofi.setTextOff("SOFI: 0");

        button_laser_left = findViewById(R.id.button_laser_left);
        button_laser_left.setTextOn("L(l):1");
        button_laser_left.setTextOff("L(l):1");
        button_laser_left.setText("L(l):1");

        button_laser_right = findViewById(R.id.button_laser_right);
        button_laser_right.setTextOn("L(r):1");
        button_laser_right.setTextOff("L(r):1");
        button_laser_right.setText("L(r):1");

        // simple buttons
        button_z_left_plus = findViewById(R.id.button_z_left_plus);
        button_z_left_minus = findViewById(R.id.button_z_left_minus);
        button_x_left_plus = findViewById(R.id.button_x_left_plus);
        button_x_left_minus = findViewById(R.id.button_x_left_minus);
        button_x_right_plus = findViewById(R.id.button_x_right_plus);
        button_x_right_minus = findViewById(R.id.button_x_right_minus);
        button_z_right_plus = findViewById(R.id.button_z_right_plus);
        button_z_right_minus = findViewById(R.id.button_z_right_minus);

        button_z_left_plus2 = findViewById(R.id.button_z_left_plus2);
        button_z_left_minus2 = findViewById(R.id.button_z_left_minus2);
        button_x_left_plus2 = findViewById(R.id.button_x_left_plus2);
        button_x_left_minus2 = findViewById(R.id.button_x_left_minus2);
        button_x_right_plus2 = findViewById(R.id.button_x_right_plus2);
        button_x_right_minus2 = findViewById(R.id.button_x_right_minus2);
        button_z_right_plus2 = findViewById(R.id.button_z_right_plus2);
        button_z_right_minus2 = findViewById(R.id.button_z_right_minus2);

        // set seekbar and coresponding texts for GUI
        seekbar_x_left = findViewById(R.id.seekbar_x_left);
        seekbar_z_left = findViewById(R.id.seekbar_z_left);
        seekbar_x_right = findViewById(R.id.seekbar_x_right);
        seekbar_z_right = findViewById(R.id.seekbar_z_right);

        seekbar_x_right.setMax(PWM_resolution);
        seekbar_z_right.setMax(PWM_resolution);
        seekbar_x_left.setMax(PWM_resolution);
        seekbar_z_left.setMax(PWM_resolution);

        textViewXRight = findViewById(R.id.textViewXRight);
        textViewZRight = findViewById(R.id.textViewZRight);
        textViewXLeft = findViewById(R.id.textViewXLeft);
        textViewZLeft = findViewById(R.id.textViewZLeft);

        //set change listener
        seekbar_x_right.setOnSeekBarChangeListener(this);
        seekbar_z_right.setOnSeekBarChangeListener(this);
        seekbar_x_left.setOnSeekBarChangeListener(this);
        seekbar_z_left.setOnSeekBarChangeListener(this);


        if (isNetworkAvailable()) {
            initialConfig();
        } else
            Toast.makeText(this, R.string.no_internets, Toast.LENGTH_SHORT).show();

        //getCallingActivity().publish(connection, topic, message, selectedQos, retainValue);



        button_ip_address_go.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    serverUri = "tcp://" + EditTextIPAddress.getText().toString(); //tcp://192.168.43.88";
                    Toast.makeText(MainActivity.this, "IP-Address set to: " + serverUri, Toast.LENGTH_SHORT).show();
                    stopConnection();
                    initialConfig();
                }
                return true;
            }
        });

        // this goes wherever you setup your button listener:
        lightsButtonLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage("lens/left/led", "1");
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    publishMessage("lens/left/led", "0");
                }

                return true;
            }
        });





        //******************* Coarse Lens movements (LEFT ONLY) ********************************//
        button_z_left_plus2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    val_lens_z_left = val_lens_z_left + coarse_increment;
                    publishMessage(topic_z_left, String.valueOf(val_lens_z_left));
                    updateGUI();
                }
                return true;
            }
        });
        button_x_left_plus2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    val_lens_x_left = val_lens_x_left + coarse_increment;
                    publishMessage(topic_x_left, String.valueOf(val_lens_x_left));
                    updateGUI();
                }
                return true;
            }
        });
        button_z_left_minus2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    val_lens_z_left = val_lens_z_left - coarse_increment;
                    publishMessage(topic_z_left, String.valueOf(val_lens_z_left));
                    updateGUI();
                }
                return true;
            }
        });
        button_x_left_minus2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    val_lens_x_left = val_lens_x_left - coarse_increment;
                    publishMessage(topic_x_left, String.valueOf(val_lens_x_left));
                    updateGUI();
                }
                return true;
            }
        });


        //******************* Coarse Lens movements (LEFT ONLY) ********************************//
        button_z_right_plus2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    val_lens_z_right = val_lens_z_right + coarse_increment;
                    publishMessage(topic_z_right, String.valueOf(val_lens_z_right));
                    updateGUI();
                }
                return true;
            }
        });
        button_x_right_plus2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    val_lens_x_right = val_lens_x_right + coarse_increment;
                    publishMessage(topic_x_right, String.valueOf(val_lens_x_right));
                    updateGUI();
                }
                return true;
            }
        });
        button_z_right_minus2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    val_lens_z_right = val_lens_z_right - coarse_increment;
                    publishMessage(topic_z_right, String.valueOf(val_lens_z_right));
                    updateGUI();
                }
                return true;
            }
        });
        button_x_right_minus2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    val_lens_x_right = val_lens_x_right - coarse_increment;
                    publishMessage(topic_x_right, String.valueOf(val_lens_x_right));
                    updateGUI();
                }
                return true;
            }
        });




        //******************* STEPPER in Y-Direction ********************************************//
        // this goes wherever you setup your button listener:
        button_y_fwd_coarse.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage(topic_stepper_y_fwd, "10");
                }
                return true;
            }
        });
        button_y_fwd_fine.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage(topic_stepper_y_fwd, "1");
                }
                return true;
            }
        });
        button_y_bwd_coarse.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage(topic_stepper_y_bwd, "10");
                }
                return true;
            }
        });
        button_y_bwd_fine.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage(topic_stepper_y_bwd, "1");
                }
                return true;
            }
        });

        //******************* STEPPER in X-Direction ********************************************//
        // this goes wherever you setup your button listener:
        button_x_fwd_coarse.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage(topic_stepper_x_fwd, "10");
                }
                return true;
            }
        });
        button_x_fwd_fine.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage(topic_stepper_x_fwd, "1");
                }
                return true;
            }
        });
        button_x_bwd_coarse.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage(topic_stepper_x_bwd, "10");
                }
                return true;
            }
        });
        button_x_bwd_fine.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage(topic_stepper_x_bwd, "1");
                }
                return true;
            }
        });


        //******************* SOFI-Mode  ********************************************//
        // This is to let the lens vibrate by a certain amount
        button_sofi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.i(TAG, "Checked");
                    is_vibration = true;
                    publishMessage(topic_both_vibrate, "1");
                    //new vibration_process().execute("");
                } else {
                    is_vibration = false;
                    publishMessage(topic_both_vibrate, "0");
                    Log.i(TAG, "Not Checked");
                }
            }

        });
        //******************* Laser left ********************************************//
        // This is to turn on the left laser
        button_laser_left.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.i(TAG, "Checked");
                    publishMessage(topic_laser_left, "1");
                    //new vibration_process().execute("");
                } else {
                    is_vibration = false;
                    publishMessage(topic_laser_left, "0");
                    Log.i(TAG, "Not Checked");
                }
            }

        });
        //******************* Laser right ********************************************//
        // This is to turn on the left laser
        button_laser_right.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.i(TAG, "Checked");
                    publishMessage(topic_laser_right, "1");
                    //new vibration_process().execute("");
                } else {
                    is_vibration = false;
                    publishMessage(topic_laser_right, "0");
                    Log.i(TAG, "Not Checked");
                }
            }

        });

        //******************* Lens (left) ********************************************//
        /*--------------------------
        LENS Z LEFT ++
        // Increment Lens Left in Z
        -------------------------- */
        button_z_left_plus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSwitchCaseLens = topic_z_left;
                increment();
            }
        });

        button_z_left_plus.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View arg0) {
                mSwitchCaseLens = topic_z_left;
                mAutoIncrement = true;
                repeatUpdateHandler.post(new RptUpdater());
                return false;
            }
        });

        button_z_left_plus.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mSwitchCaseLens = topic_z_left;
                mAutoIncrement = false;
                return false;
            }
        });


        /*--------------------------
        LENS Z LEFT --
        // Decrement Lens Left in Z
        -------------------------- */
        button_z_left_minus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSwitchCaseLens = topic_z_left;
                decrement();
            }
        });

        button_z_left_minus.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View arg0) {
                mSwitchCaseLens = topic_z_left;
                mAutoDecrement = true;
                repeatUpdateHandler.post(new RptUpdater());
                return false;
            }
        });

        button_z_left_minus.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mSwitchCaseLens = topic_z_left;
                mAutoDecrement = false;
                return false;
            }
        });



        /*--------------------------
        LENS X LEFT ++
        // Increment Lens Left in X
        -------------------------- */
        button_x_left_plus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSwitchCaseLens = topic_x_left;
                increment();
            }
        });

        button_x_left_plus.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View arg0) {
                mSwitchCaseLens = topic_x_left;
                mAutoIncrement = true;
                repeatUpdateHandler.post(new RptUpdater());
                return false;
            }
        });

        button_x_left_plus.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mSwitchCaseLens = topic_x_left;
                mAutoIncrement = false;
                return false;
            }
        });


        /*--------------------------
        LENS X LEFT --
        // Decrement Lens Left in X
        -------------------------- */
        button_x_left_minus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSwitchCaseLens = topic_x_left;
                decrement();
            }
        });

        button_x_left_minus.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View arg0) {
                mSwitchCaseLens = topic_x_left;
                mAutoDecrement = true;
                repeatUpdateHandler.post(new RptUpdater());
                return false;
            }
        });

        button_x_left_minus.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mSwitchCaseLens = topic_x_left;
                mAutoDecrement = false;
                return false;
            }
        });


        //******************* Lens (right) ********************************************//
        /*--------------------------
        LENS Z right ++
        // Increment Lens right in Z
        -------------------------- */
        button_z_right_plus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSwitchCaseLens = topic_z_right;
                increment();
            }
        });

        button_z_right_plus.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View arg0) {
                mSwitchCaseLens = topic_z_right;
                mAutoIncrement = true;
                repeatUpdateHandler.post(new RptUpdater());
                return false;
            }
        });

        button_z_right_plus.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mSwitchCaseLens = topic_z_right;
                mAutoIncrement = false;
                return false;
            }
        });


        /*--------------------------
        LENS Z right --
        // Decrement Lens right in Z
        -------------------------- */
        button_z_right_minus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSwitchCaseLens = topic_z_right;
                decrement();
            }
        });

        button_z_right_minus.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View arg0) {
                mSwitchCaseLens = topic_z_right;
                mAutoDecrement = true;
                repeatUpdateHandler.post(new RptUpdater());
                return false;
            }
        });

        button_z_right_minus.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mSwitchCaseLens = topic_z_right;
                mAutoDecrement = false;
                return false;
            }
        });



        /*--------------------------
        LENS X right ++
        // Increment Lens right in X
        -------------------------- */
        button_x_right_plus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSwitchCaseLens = topic_x_right;
                increment();
            }
        });

        button_x_right_plus.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View arg0) {
                mSwitchCaseLens = topic_x_right;
                mAutoIncrement = true;
                repeatUpdateHandler.post(new RptUpdater());
                return false;
            }
        });

        button_x_right_plus.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mSwitchCaseLens = topic_x_right;
                mAutoIncrement = false;
                return false;
            }
        });


        /*--------------------------
        LENS X right --
        // Decrement Lens right in X
        -------------------------- */
        button_x_right_minus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mSwitchCaseLens = topic_x_right;
                decrement();
            }
        });

        button_x_right_minus.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View arg0) {
                mSwitchCaseLens = topic_x_right;
                mAutoDecrement = true;
                repeatUpdateHandler.post(new RptUpdater());
                return false;
            }
        });

        button_x_right_minus.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mSwitchCaseLens = topic_x_right;
                mAutoDecrement = false;
                return false;
            }
        });


        // set gui
        updateGUI();
    }







    public void decrement() {
        switch (mSwitchCaseLens) {
            case topic_z_left:
                if (val_lens_z_left>0) {
                    val_lens_z_left--;
                    publishMessage(topic_z_left, String.valueOf(val_lens_z_left));
                    break;
                }
        }
        switch (mSwitchCaseLens) {
            case topic_x_left:
                if (val_lens_x_left>0) {
                    val_lens_x_left--;
                    publishMessage(topic_x_left, String.valueOf(val_lens_x_left));
                    break;
                }
        }
        switch (mSwitchCaseLens) {
            case topic_z_right:
                if (val_lens_z_right>0) {
                    val_lens_z_right--;
                    publishMessage(topic_z_right, String.valueOf(val_lens_z_right));
                    break;
                }
        }
        switch (mSwitchCaseLens) {
            case topic_x_right:
                if (val_lens_x_right>0) {
                    val_lens_x_right--;
                    publishMessage(topic_x_right, String.valueOf(val_lens_x_right));
                    break;
                }
        }
        updateGUI();
    }

    
    public void increment() {
        switch (mSwitchCaseLens) {
            case topic_z_left:
                if (val_lens_z_left<PWM_resolution) {
                    val_lens_z_left++;
                    publishMessage(topic_z_left, String.valueOf(val_lens_z_left));
                    break;
                }
        }
        switch (mSwitchCaseLens) {
            case topic_x_left:
                if (val_lens_x_left<PWM_resolution) {
                val_lens_x_left++;
                publishMessage(topic_x_left, String.valueOf(val_lens_x_left));
                break;
                }
        }
        switch (mSwitchCaseLens) {
            case topic_z_right:
                if (val_lens_z_right<PWM_resolution) {
                    val_lens_z_right++;
                    publishMessage(topic_z_right, String.valueOf(val_lens_z_right));
                    break;
                }
        }
        switch (mSwitchCaseLens) {
            case topic_x_right:
                if (val_lens_x_right<PWM_resolution) {
                    val_lens_x_right++;
                    publishMessage(topic_x_right, String.valueOf(val_lens_x_right));
                    break;
                }
        }
        updateGUI();
    }


    @Override
    public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
        /*
        if (bar.equals(seekbar_x_left)) {
            // For left Lens in X
            val_lens_x_left = progress;
            updateGUI();
            publishMessage("lens/left/x", String.valueOf(progress));
        }
        */
        if (bar.equals(seekbar_z_left)) {
            // For left Lens in Y
            val_lens_z_left = progress;
            updateGUI();
            publishMessage(topic_z_left, String.valueOf(progress));
        } else if (bar.equals(seekbar_x_left)) {
            // For left Lens in Z
            val_lens_x_left = progress;
            updateGUI();
            publishMessage(topic_x_left, String.valueOf(progress));
        } else if (bar.equals(seekbar_x_right)) {
            // For right Lens in X
            val_lens_x_right = progress;
            updateGUI();
            publishMessage(topic_x_right, String.valueOf(progress));
        } else if (bar.equals(seekbar_z_right)) {
            // For right Lens in Y
            val_lens_z_right = progress;
            updateGUI();
            publishMessage(topic_z_right, String.valueOf(progress));
        }

    }

    public void updateGUI() {
        // Update all slides if value has been changed
        textViewXRight.setText("LX (r): " + String.format("%05d", val_lens_x_right));
        seekbar_x_right.setProgress(val_lens_x_right);

        textViewZLeft.setText("LZ (l): " + String.format("%05d", val_lens_z_left));
        seekbar_z_left.setProgress(val_lens_z_left);

        textViewZRight.setText("LZ (r): " + String.format("%05d", val_lens_z_right));
        seekbar_z_right.setProgress(val_lens_z_right);

        textViewXLeft.setText("LX (l): " + String.format("%05d", val_lens_x_left));
        seekbar_x_left.setProgress(val_lens_x_left);

    }


    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void initialConfig() {
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    //addToHistory("Reconnected to : " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                    // subscribeToTopic();
                } else {
                    //addToHistory("Connected to: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                //addToHistory("The Connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                //addToHistory("Incoming message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setUserName(mqttUser);
        mqttConnectOptions.setPassword(mqttPass.toCharArray());
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);

        try {
            //addToHistory("Connecting to " + serverUri);
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);

                    // Todo Obtener estado de luces If de si las luces están encendidas
                    publishMessage("A phone has connected.", "");
                    // subscribeToTopic();
                    lightsButtonLeft.setEnabled(true);
                    Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    //addToHistory("Failed to connect to: " + serverUri);
                    Toast.makeText(MainActivity.this, "Connection attemp failed", Toast.LENGTH_SHORT).show();
                }
            });


        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    public void publishMessage(String pub_topic, String publishMessage) {

        Log.d(TAG, pub_topic + " " + publishMessage);
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(publishMessage.getBytes());
            mqttAndroidClient.publish(pub_topic, message);
            //addToHistory("Message Published");
            if (!mqttAndroidClient.isConnected()) {
                //addToHistory(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            Toast.makeText(this, "Error while sending data", Toast.LENGTH_SHORT).show();
            //System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void stopConnection() {
        mqttAndroidClient.close();
        Toast.makeText(MainActivity.this, "Connection closed - on purpose?", Toast.LENGTH_SHORT).show();
    }
}




