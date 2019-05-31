package de.nanoimaging.stormcontroler;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
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
    final String serverUri = "tcp://192.168.43.88";
    final String mqttUser = "username";
    final String mqttPass = "pi";

    final String clientId = "Mobile";

    boolean is_vibration = false;
    // TAG
    String TAG = "dSTORM-on-a-chieap";

    // MQTT Topics
    String topic_x_left = "lens/left/x";
    String topic_x_right = "lens/right/x";
    String topic_z_left = "lens/left/z";
    String topic_z_right = "lens/right/z";
    String topic_both_vibrate = "lens/both/vibrate";

    // PWM settings
    int seekbar_max = 1024-1; // bitrate of the PWM signal
    int myperiode = 20; // time to pause between toggling
    int myamplitude = 20; // amplitude of the lens in each periode

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

    ToggleButton button_vibrate;


    /*
    Button button_x_left_plus;
    Button button_x_left_minus;
    Button button_z_right_plus;
    Button button_z_right_minus;

    */
    Button button_y_left_plus;
    Button button_y_left_minus;
    Button button_x_left_plus;
    Button button_x_left_minus;
    Button button_x_right_plus;
    Button button_x_right_minus;
    Button button_y_right_plus;
    Button button_y_right_minus;

    // Safe the state of the progress bar
    int lens_right_x = 0;
    int lens_right_y = 0;
    int lens_left_y = 0;
    int lens_left_x = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_view);

        // Register GUI components
        lightsButtonLeft = findViewById(R.id.button_lights_left);
        lightsButtonRight = findViewById(R.id.button_lights_right);

        button_x_fwd_coarse = findViewById(R.id.button_x_fwd_coarse);
        button_x_fwd_fine = findViewById(R.id.button_x_fwd_fine);
        button_x_bwd_coarse = findViewById(R.id.button_x_bwd_coarse);
        button_x_bwd_fine = findViewById(R.id.button_x_bwd_fine);
        button_y_fwd_coarse = findViewById(R.id.button_y_fwd_coarse);
        button_y_fwd_fine = findViewById(R.id.button_y_fwd_fine);
        button_y_bwd_coarse = findViewById(R.id.button_y_bwd_coarse);
        button_y_bwd_fine = findViewById(R.id.button_y_bwd_fine);
        button_vibrate = findViewById(R.id.button_vibrate);
        button_vibrate.setTextOn("Vibration: ON");
        button_vibrate.setTextOff("Vibration: OFF");

        button_y_left_plus = findViewById(R.id.button_z_left_plus);
        button_y_left_minus = findViewById(R.id.button_z_left_minus);
        button_x_left_plus = findViewById(R.id.button_x_left_plus);
        button_x_left_minus = findViewById(R.id.button_x_left_minus);
        button_x_right_plus = findViewById(R.id.button_x_right_plus);
        button_x_right_minus = findViewById(R.id.button_x_right_minus);
        button_y_right_plus = findViewById(R.id.button_z_right_plus);
        button_y_right_minus = findViewById(R.id.button_z_right_minus);

        // set seekbar and coresponding texts for GUI
        seekbar_x_left = (SeekBar) findViewById(R.id.seekbar_x_left);
        seekbar_z_left = (SeekBar) findViewById(R.id.seekbar_z_left);
        seekbar_x_right = (SeekBar) findViewById(R.id.seekbar_x_right);
        seekbar_z_right = (SeekBar) findViewById(R.id.seekbar_z_right);

        seekbar_x_right.setMax(seekbar_max);
        seekbar_z_right.setMax(seekbar_max);
        seekbar_x_left.setMax(seekbar_max);
        seekbar_z_left.setMax(seekbar_max);

        textViewXRight = findViewById(R.id.textViewXRight);
        textViewZRight = findViewById(R.id.textViewZRight);
        textViewXLeft = findViewById(R.id.textViewXLeft);
        textViewZLeft = findViewById(R.id.textViewZLeft);

        //set change listener
        seekbar_x_right.setOnSeekBarChangeListener(this);
        seekbar_z_right.setOnSeekBarChangeListener(this);
        seekbar_x_left.setOnSeekBarChangeListener(this);
        seekbar_z_left.setOnSeekBarChangeListener(this);



        if(isNetworkAvailable()) {
            initialConfig();
        }else
            Toast.makeText(this, R.string.no_internets, Toast.LENGTH_SHORT).show();

        //getCallingActivity().publish(connection, topic, message, selectedQos, retainValue);

        // this goes wherever you setup your button listener:
        lightsButtonLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage("lens/left/led", "1");
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    publishMessage("lens/left/led", "0");
                }

                return true;
            }
        });
        // this goes wherever you setup your button listener:
        lightsButtonRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage("lens/right/led", "1");
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    publishMessage("lens/right/led", "0");
                }

                return true;
            }
        });

        //******************* STEPPER in X-Direction ********************************************//
        // this goes wherever you setup your button listener:
        button_x_fwd_coarse.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage("stepper/x/fwd", "10");
                }
                return true;
            }
        });
        button_x_fwd_fine.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage("stepper/x/fwd", "1");
                }
                return true;
            }
        });
        button_x_bwd_coarse.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage("stepper/x/bwd", "10");
                }
                return true;
            }
        });
        button_x_bwd_fine.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage("stepper/x/bwd", "1");
                }
                return true;
            }
        });


        //******************* STEPPER in Y-Direction ********************************************//
        // this goes wherever you setup your button listener:
        button_y_fwd_coarse.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage("stepper/y/fwd", "10");
                }
                return true;
            }
        });
        button_y_fwd_fine.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage("stepper/y/fwd", "1");
                }
                return true;
            }
        });
        button_y_bwd_coarse.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage("stepper/y/bwd", "10");
                }
                return true;
            }
        });
        button_y_bwd_fine.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage("stepper/y/bwd", "1");
                }
                return true;
            }
        });



        button_vibrate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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



        button_y_left_plus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    lens_left_y++;
                    updateGUI();
                    publishMessage(topic_z_left, String.valueOf(lens_left_y));
                }
                return true;
            }
        });

        button_y_left_minus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    lens_left_y--;
                    updateGUI();
                    publishMessage(topic_z_left, String.valueOf(lens_left_y));
                }
                return true;
            }
        });

        button_x_left_plus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    lens_left_x++;
                    updateGUI();
                    publishMessage(topic_x_left, String.valueOf(lens_left_x));
                }
                return true;
            }
        });

        button_x_left_minus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    lens_left_x--;
                    updateGUI();
                    publishMessage(topic_x_left , String.valueOf(lens_left_x));
                }
                return true;
            }
        });

        // incremental updates on the lenses positions by +/- buttons
        button_x_right_plus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    lens_right_x++;
                    updateGUI();
                    publishMessage(topic_x_right, String.valueOf(lens_right_x));
                }
                return true;
            }
        });

        button_x_right_minus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    lens_right_x--;
                    updateGUI();
                    publishMessage(topic_x_right, String.valueOf(lens_right_x));
                }
                return true;
            }
        });

        button_y_right_plus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    lens_right_y++;
                    updateGUI();
                    publishMessage(topic_z_left, String.valueOf(lens_right_y));
                }
                return true;
            }
        });

        button_y_right_minus.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    lens_right_y--;
                    updateGUI();
                    publishMessage(topic_z_right, String.valueOf(lens_right_y));
                }
                return true;
            }
        });


    }

    @Override
    public void onProgressChanged(SeekBar bar, int progress, boolean fromUser)
    {
        /*
        if (bar.equals(seekbar_x_left)) {
            // For left Lens in X
            lens_left_x = progress;
            updateGUI();
            publishMessage("lens/left/x", String.valueOf(progress));
        }
        */
        if (bar.equals(seekbar_z_left))
        {
            // For left Lens in Y
            lens_left_y = progress;
            updateGUI();
            publishMessage(topic_z_left, String.valueOf(progress));
        }
        else if (bar.equals(seekbar_x_left))
        {
            // For left Lens in Z
            lens_left_x = progress;
            updateGUI();
            publishMessage(topic_x_left, String.valueOf(progress));
        }
        else if (bar.equals(seekbar_x_right))
        {
            // For right Lens in X
            lens_right_x = progress;
            updateGUI();
            publishMessage(topic_x_right, String.valueOf(progress));
        }
        else if (bar.equals(seekbar_z_right))
        {
            // For right Lens in Y
            lens_right_y = progress;
            updateGUI();
            publishMessage(topic_z_right, String.valueOf(progress));
        }

    }

    public void updateGUI(){

        textViewXRight.setText("LX (right): "+String.valueOf(lens_right_x));
        seekbar_x_right.setProgress(lens_right_x);

        textViewZLeft.setText("Ly (left): "+String.valueOf(lens_left_y));
        seekbar_z_left.setProgress(lens_left_y);

        textViewZRight.setText("LY (right): "+String.valueOf(lens_right_y));
        seekbar_z_right.setProgress(lens_right_y);

        textViewXLeft.setText("LY (right): "+String.valueOf(lens_left_x));
        seekbar_x_left.setProgress(lens_left_x);

    }



    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void initialConfig(){
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


        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }



    public void publishMessage(String pub_topic, String publishMessage){

        Log.d(TAG, pub_topic + " " + publishMessage);
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(publishMessage.getBytes());
            mqttAndroidClient.publish(pub_topic, message);
            //addToHistory("Message Published");
            if(!mqttAndroidClient.isConnected()){
                //addToHistory(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            Toast.makeText(this, "Error while sending data", Toast.LENGTH_SHORT).show();
            //System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private class vibration_process extends AsyncTask<String, Void, String> {

        int lens_left_x_tmp =  lens_left_x;
        int lens_right_x_tmp =  lens_right_x;

        void mysleep(int sleeptime){
            try {
                Thread.sleep(sleeptime);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        }
        @Override
        protected String doInBackground(String... params) {
            while(is_vibration){

                lens_left_x_tmp = lens_left_x_tmp+myamplitude;
                lens_right_x_tmp = lens_right_x_tmp+myamplitude;

                publishMessage("lens/left/x", String.valueOf(lens_left_x_tmp));
                publishMessage("lens/right/x", String.valueOf(lens_right_x_tmp));
                mysleep(myperiode);

                lens_left_x_tmp = lens_left_x_tmp-myamplitude;
                lens_right_x_tmp = lens_right_x_tmp-myamplitude;

                publishMessage("lens/left/x", String.valueOf(lens_left_x_tmp));
                publishMessage("lens/right/x", String.valueOf(lens_right_x_tmp));
                mysleep(myperiode);
            }

            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            publishMessage("lens/left/x", String.valueOf(lens_left_x));
            publishMessage("lens/right/x", String.valueOf(lens_right_x));
            Toast.makeText(MainActivity.this, "Vibration-mode is stopped", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(MainActivity.this, "Vibration-mode is started", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onProgressUpdate(Void... values) {}
    }




}
