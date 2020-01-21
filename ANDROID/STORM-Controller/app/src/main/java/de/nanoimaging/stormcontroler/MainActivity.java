package de.nanoimaging.stormcontroler;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

import org.apache.log4j.BasicConfigurator;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import de.nanoimaging.stormcontroler.broker.MQTTService;
import de.nanoimaging.stormcontroler.util.Utils;
import io.moquette.BrokerConstants;


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

    // MQTT-Broker related STUFF
    private MQTTService mService;
    private boolean mBound = false;
    Context context;
    File confFile, passwordFile;
    boolean is_servicerunning = false;


    MqttAndroidClient mqttAndroidClient;

    // Server uri follows the format tcp://ipaddress:port
    String serverUri = "192.168.43.88";

    final String mqttUser = "username";
    final String mqttPass = "pi";

    final String clientId = "Mobile";


    int seekbar_val_lens_x_left = 0;
    int seekbar_val_lens_z_left =0;
    int seekbar_val_laser_red =0;

    boolean is_SOFI_z = false;
    boolean is_SOFI_x = false;

    int tap_counter_ipadress_button = 0;
    // TAG
    String TAG = "dSTORM-on-a-chieap";

    // Save settings for later
    private final String PREFERENCE_FILE_KEY = "myAppPreference";

    // MQTT Topics
    public static final String topic_laser_red = "laser/red";
    public static final String topic_z_left = "lens/right/z";
    public static final String topic_x_left = "lens/right/x";
    public static final String topic_stepper_y_fwd = "stepper/y/fwd";
    public static final String topic_stepper_y_bwd = "stepper/y/bwd";
    public static final String topic_stepper_x_fwd = "stepper/x/fwd";
    public static final String topic_stepper_x_bwd = "stepper/x/bwd";

    // PWM settings
    int PWM_resolution = 32768 - 1; // bitrate of the PWM signal
    int myperiode = 10; // time to pause between toggling
    int myamplitude_z = 20; // amplitude of the lens in each periode
    int myamplitude_x = 20; // amplitude of the lens in each periode
    int coarse_increment = 20; // steps for ++/--



    // Seekbars
    private SeekBar seekbar_z_left;
    private SeekBar seekbar_x_left;
    private SeekBar seekbar_laser_red;

    TextView textViewLaserRed;
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

    ToggleButton button_sofi_z;
    ToggleButton button_sofi_x;
    ToggleButton button_laser_left;
    ToggleButton button_laser_right;

    Button button_z_left_plus;
    Button button_z_left_minus;
    Button button_x_left_plus;
    Button button_x_left_minus;
    Button button_laser_red_plus;
    Button button_laser_red_minus;
    Button button_z_left_plus2;
    Button button_z_left_minus2;
    Button button_x_left_plus2;
    Button button_x_left_minus2;
    Button button_laser_red_plus2;
    Button button_laser_red_minus2;
    Button button_ip_address_go;
    Button button_load_localip;
    Button button_load_defaultip;
    Button button_stop_mqtt_service;
    Button button_start_mqtt_service;


    EditText EditTextIPAddress;
    EditText EditTextSOFIAmplitude_x;
    EditText EditTextSOFIAmplitude_z;

    // Save the state of the progress bar
    int val_laser_red = 0;
    int val_lens_z_left = 0;
    int val_lens_x_left = 0;


    String[] permissions = new String[]{
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };


    @Override
    protected void onStart() {
        super.onStart();
        this.bindService(new Intent(this, MQTTService.class), mConnection, BIND_IMPORTANT);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            MainActivity.this.mService = ((MQTTService.LocalBinder) service).getService();
            MainActivity.this.mBound = ((MQTTService.LocalBinder) service).getServerStatus();
            // MainActivity.this.updateStartedStatus();
        }

        public void onServiceDisconnected(ComponentName arg0) {
            MainActivity.this.mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_view);
        context = this;

        // check for permission
        checkPermissions();


        // Take care of previously saved settings
        SharedPreferences sharedPref = this.getSharedPreferences(
                PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();


        // Register GUI components
        lightsButtonLeft = findViewById(R.id.button_lights_left);
        //lightsButtonRight = findViewById(R.id.button_lights_right);
        EditTextSOFIAmplitude_z = (EditText) findViewById(R.id.editText_SOFI_z);
        EditTextSOFIAmplitude_x = (EditText) findViewById(R.id.editText_SOFI_x);

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
        button_load_localip = findViewById(R.id.button_load_localip);
        button_load_defaultip = findViewById(R.id.button_load_defaultip);


        // toggle buttons
        button_sofi_z = findViewById(R.id.button_SOFI_z);
        button_sofi_z.setText("SOFI (z): 0");
        button_sofi_z.setTextOn("SOFI (z): 1");
        button_sofi_z.setTextOff("SOFI (z): 0");

        button_sofi_x = findViewById(R.id.button_SOFI_x);
        button_sofi_x.setText("SOFI (x): 0");
        button_sofi_x.setTextOn("SOFI (x): 1");
        button_sofi_x.setTextOff("SOFI (x): 0");


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
        button_laser_red_plus = findViewById(R.id.button_laser_red_plus);
        button_laser_red_minus = findViewById(R.id.button_laser_red_minus);

        button_z_left_plus2 = findViewById(R.id.button_z_left_plus2);
        button_z_left_minus2 = findViewById(R.id.button_z_left_minus2);
        button_x_left_plus2 = findViewById(R.id.button_x_left_plus2);
        button_x_left_minus2 = findViewById(R.id.button_x_left_minus2);
        button_laser_red_plus2 = findViewById(R.id.button_laser_red_plus2);
        button_laser_red_minus2 = findViewById(R.id.button_laser_red_minus2);
        button_start_mqtt_service = findViewById(R.id.button_start_mqtt_service);
        button_stop_mqtt_service = findViewById(R.id.button_stop_mqtt_service);

        // set seekbar and coresponding texts for GUI
        seekbar_x_left = findViewById(R.id.seekbar_x_left);
        seekbar_z_left = findViewById(R.id.seekbar_z_left);
        seekbar_laser_red = findViewById(R.id.seekbar_laser_red);

        seekbar_laser_red.setMax(PWM_resolution);
        seekbar_x_left.setMax(PWM_resolution);
        seekbar_z_left.setMax(PWM_resolution);


        textViewLaserRed = findViewById(R.id.textViewLaserRed);
        textViewXLeft = findViewById(R.id.textViewXLeft);
        textViewZLeft = findViewById(R.id.textViewZLeft);

        //set change listener
        seekbar_laser_red.setOnSeekBarChangeListener(this);
        seekbar_x_left.setOnSeekBarChangeListener(this);
        seekbar_z_left.setOnSeekBarChangeListener(this);


        // Read old IP ADress if available and set it to the GUI
        serverUri = sharedPref.getString("IP_ADDRESS", serverUri);
        EditTextIPAddress.setText(serverUri);


        if (isNetworkAvailable()) {
            initialConfig();
        } else
            Toast.makeText(this, R.string.no_internets, Toast.LENGTH_SHORT).show();

        //getCallingActivity().publish(connection, topic, message, selectedQos, retainValue);

        // MOQUETTE related stuff
        BasicConfigurator.configure();

        button_start_mqtt_service.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(!is_servicerunning) startService();
                is_servicerunning = true;
                return true;
            }
        });

        button_stop_mqtt_service.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                is_servicerunning = false;
                stopService();
                return true;
            }
        });

        confFile = new File(getApplicationContext().getDir("media", 0).getAbsolutePath() + Utils.BROKER_CONFIG_FILE);
        passwordFile = new File(getApplicationContext().getDir("media", 0).getAbsolutePath() + Utils.PASSWORD_FILE);
        Log.i("MAIN", confFile.getAbsolutePath());
        loadConfig();


        button_ip_address_go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    serverUri = EditTextIPAddress.getText().toString(); //tcp://192.168.43.88";
                    Toast.makeText(MainActivity.this, "IP-Address set to: " + serverUri, Toast.LENGTH_SHORT).show();
                    stopConnection();
                    initialConfig();

                    // Save the IP address for next start
                    editor.putString("IP_ADDRESS", serverUri);
                    editor.commit();


            }
        });

        button_load_defaultip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tap_counter_ipadress_button = 0;
                serverUri = "192.168.43.88";
                EditTextIPAddress.setText(serverUri);
                Toast.makeText(MainActivity.this, "IP-Address set to default: " + serverUri, Toast.LENGTH_SHORT).show();
                stopConnection();
                initialConfig();

                // Save the IP address for next start
                editor.putString("IP_ADDRESS", serverUri);
                editor.commit();

            }
        });

        button_load_localip.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                    serverUri = String.valueOf(wifiIpAddress(MainActivity.this));
                    EditTextIPAddress.setText(serverUri);
                    Toast.makeText(MainActivity.this, "IP-Address set to: " + serverUri, Toast.LENGTH_SHORT).show();
                    stopConnection();
                    initialConfig();

                    // Save the IP address for next start
                    editor.putString("IP_ADDRESS", serverUri);
                    //editor.putString("IP_ADDRESS", serverUri);
                    editor.commit();

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
        button_z_left_plus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    val_lens_z_left  ++;
                    publishMessage(topic_z_left, String.valueOf(val_lens_z_left));
                    updateGUI();
                }
                return true;
            }
        });
        button_x_left_plus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    val_lens_x_left  ++;
                    publishMessage(topic_x_left, String.valueOf(val_lens_x_left));
                    updateGUI();
                }
                return true;
            }
        });
        button_z_left_minus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    val_lens_z_left  --;
                    publishMessage(topic_z_left, String.valueOf(val_lens_z_left));
                    updateGUI();
                }
                return true;
            }
        });
        button_x_left_minus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    val_lens_x_left -- ;
                    publishMessage(topic_x_left, String.valueOf(val_lens_x_left));
                    updateGUI();
                }
                return true;
            }
        });


        button_laser_red_plus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    val_laser_red ++;
                    publishMessage(topic_laser_red, String.valueOf(val_laser_red));
                    updateGUI();
                }
                return true;
            }
        });
        button_laser_red_minus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    val_laser_red ++;
                    publishMessage(topic_laser_red, String.valueOf(val_laser_red));
                    updateGUI();
                }
                return true;
            }
        });

        button_laser_red_plus2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    val_laser_red = val_laser_red + coarse_increment;
                    publishMessage(topic_laser_red, String.valueOf(val_laser_red));
                    updateGUI();
                }
                return true;
            }
        });
        button_laser_red_minus2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    val_laser_red = val_laser_red - coarse_increment;
                    publishMessage(topic_laser_red, String.valueOf(val_laser_red));
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
        button_sofi_z.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.i(TAG, "Checked");
                    myamplitude_z = Integer.parseInt(EditTextSOFIAmplitude_z.getText().toString()); //tcp://192.168.43.88";
                    Log.i(TAG, "Set the amplitude to: " + String.valueOf(myamplitude_z));
                    is_SOFI_z = true;
                    //publishMessage(topic_both_vibrate, "1");
                    new vibration_process_z().execute("");
                } else {
                    is_SOFI_z = false;
                    //publishMessage(topic_both_vibrate, "0");
                    Log.i(TAG, "Not Checked");
                }
            }

        });

        // This is to let the lens vibrate by a certain amount
        button_sofi_x.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.i(TAG, "Checked x");
                    myamplitude_x = Integer.parseInt(EditTextSOFIAmplitude_x.getText().toString()); //tcp://192.168.43.88";
                    Log.i(TAG, "Set the amplitude to: " + String.valueOf(myamplitude_z));
                    is_SOFI_x = true;
                    //publishMessage(topic_both_vibrate, "1");
                    new vibration_process_x().execute("");
                } else {
                    is_SOFI_x = false;
                    //publishMessage(topic_both_vibrate, "0");
                    Log.i(TAG, "Not Checked");

                }
            }

        });
    }


    @Override
    public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
        double normalizedseebar = (double)progress/(double)PWM_resolution;
        double quadraticseekbar = Math.pow(normalizedseebar,2);
        int seekbarvalue = (int)(quadraticseekbar*(double)PWM_resolution);
        Log.i(TAG, "My value: "+String.valueOf(seekbarvalue));

        if (bar.equals(seekbar_z_left)) {
            // For left Lens in Y
            val_lens_z_left = seekbarvalue;
            seekbar_val_lens_z_left = progress;
            publishMessage(topic_z_left, String.valueOf(val_lens_z_left));
        } else if (bar.equals(seekbar_x_left)) {
            // For left Lens in Z
            val_lens_x_left = seekbarvalue;
            seekbar_val_lens_x_left = progress;
            publishMessage(topic_x_left, String.valueOf(val_lens_x_left));
        } else if (bar.equals(seekbar_laser_red)) {
            // For right Lens in Y
            val_laser_red = seekbarvalue;
            seekbar_val_laser_red = progress;
            publishMessage(topic_laser_red, String.valueOf(val_laser_red));
        }

        updateGUI();
    }

    public void updateGUI() {
        // Update all slides if value has been changed
        textViewLaserRed.setText("Laser: " + String.format("%05d", val_laser_red));
        seekbar_laser_red.setProgress(seekbar_val_laser_red);

        textViewZLeft.setText("LZ (l): " + String.format("%05d", val_lens_z_left));
        seekbar_z_left.setProgress(seekbar_val_lens_z_left);

        textViewXLeft.setText("LX (l): " + String.format("%05d", val_lens_x_left));
        seekbar_x_left.setProgress(seekbar_val_lens_x_left);

    }


    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void initialConfig() {
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), "tcp://"+serverUri, clientId);
        Log.i(TAG, "My ip is: tcp://"+serverUri);
        Log.i(TAG, "My client ID is: "+clientId);
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
        mqttConnectOptions.setCleanSession(true);

        try {
            //addToHistory("Connecting to " + serverUri);
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(true);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);

                    // subscribeToTopic();
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

    public void publishMessage(String pub_topic, String publishMessage) {
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(publishMessage.getBytes());
            //message.setRetained(true);
            mqttAndroidClient.publish(pub_topic, message);
            Log.i(TAG, pub_topic + " " + publishMessage);
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
        try {
            mqttAndroidClient.disconnectForcibly();
            mqttAndroidClient.close();
            mqttAndroidClient.unregisterResources();

            Toast.makeText(MainActivity.this, "Connection closed - on purpose?", Toast.LENGTH_SHORT).show();
        }
        catch(Throwable e){
            Toast.makeText(MainActivity.this, "Something went wrong - propbably no connection established?", Toast.LENGTH_SHORT).show();
            Log.e(TAG, String.valueOf(e));
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }





    protected String wifiIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e("WIFIIP", "Unable to get host address.");
            ipAddressString = null;
        }

        return ipAddressString;
    }


    private class vibration_process_z extends AsyncTask<String, Void, String> {

        int lens_left_z_tmp =  val_lens_z_left;


        void mysleep(int sleeptime){
            try {
                Thread.sleep(sleeptime);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        }
        @Override
        protected String doInBackground(String... params) {
            while(is_SOFI_z){

                lens_left_z_tmp = Math.abs(val_lens_z_left + ((int)(Math.random() * (double)myamplitude_z - (double)myamplitude_x/2)));
                //lens_left_z_tmp = (int)(Math.pow(lens_left_z_tmp,2)*(double)PWM_resolution);
                publishMessage(topic_z_left, String.valueOf(lens_left_z_tmp));
                mysleep(myperiode*3);

            }

            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            publishMessage(topic_z_left, String.valueOf(lens_left_z_tmp));

            Toast.makeText(MainActivity.this, "Vibration-mode is stopped", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(MainActivity.this, "Vibration-mode is started", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onProgressUpdate(Void... values) {}
    }


    private class vibration_process_x extends AsyncTask<String, Void, String> {


        int lens_left_x_tmp =  val_lens_x_left;

        void mysleep(int sleeptime){
            try {
                Thread.sleep(sleeptime);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        }
        @Override
        protected String doInBackground(String... params) {
            while(is_SOFI_x){
                lens_left_x_tmp = Math.abs(val_lens_x_left + ((int)(Math.random() * (double)myamplitude_x - (double)myamplitude_x/2)));
                //lens_left_x_tmp = (int)(Math.pow(lens_left_x_tmp,2)*(double)PWM_resolution);
                publishMessage(topic_x_left, String.valueOf(lens_left_x_tmp));
                mysleep(myperiode*3);
            }

            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            publishMessage(topic_x_left, String.valueOf(lens_left_x_tmp));
            Toast.makeText(MainActivity.this, "Vibration-mode is stopped", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(MainActivity.this, "Vibration-mode is started", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // do something
            }
            return;
        }
    }

    // MOQUETTE related stuff
    private Properties defaultConfig() {
        Properties props = new Properties();
        props.setProperty(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME, this.getExternalFilesDir(null).getAbsolutePath() + File.separator +  "UC2_moquette_store.mapdb");//BrokerConstants.DEFAULT_MOQUETTE_STORE_MAP_DB_FILENAME );
        props.setProperty(BrokerConstants.PORT_PROPERTY_NAME, "1883");
        props.setProperty(BrokerConstants.NEED_CLIENT_AUTH, "false");
        props.setProperty(BrokerConstants.HOST_PROPERTY_NAME, Utils.getBrokerURL(this));
        props.setProperty(BrokerConstants.WEB_SOCKET_PORT_PROPERTY_NAME, String.valueOf(BrokerConstants.WEBSOCKET_PORT));
        props.setProperty(BrokerConstants.ALLOW_ANONYMOUS_PROPERTY_NAME, "true");

        return props;
    }

    private Properties loadConfig() {

        try (InputStream input = new FileInputStream(confFile)) {
            Properties props = new Properties();
            props.load(input);
            updateUI(props);
            return props;
        } catch (FileNotFoundException e) {
            Log.e("MAIN", "Config file not found. Using default config");
        } catch (IOException ex) {
            Log.e("MAIN", "IOException. Using default config");
        }
        Properties props = defaultConfig();
        updateUI(props);
        return props;
    }

    private void updateUI(Properties props) {
    }

    public void startService(View v) {
        startService();
    }

    public void startService() {
        Log.i(TAG, "we start the service");
        if (mBound == true && mService != null) {
            Log.i("MainActivity", "Service already running");
            return;
        }
        Intent serviceIntent = new Intent(this, MQTTService.class);

        Bundle bundle = new Bundle();
        bundle.putSerializable("config", defaultConfig());
        serviceIntent.putExtras(bundle);

        startService(serviceIntent);
        this.bindService(new Intent(this, MQTTService.class), mConnection, BIND_IMPORTANT);
    }

    public void stopService(View v) {
        stopService();
    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, MQTTService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            this.unbindService(mConnection);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}




