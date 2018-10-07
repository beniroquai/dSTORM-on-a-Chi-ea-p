package de.nanoimaging.stormcontroler;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import de.nanoimaging.stormcontroler.R;


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
    final String pub_topic_lens_xl = "/home/lights";

    // Seekbars
    private SeekBar seekbar_x_left;
    private SeekBar seekbar_y_left;
    private SeekBar seekbar_z_left;
    private SeekBar seekbar_x_right;
    private SeekBar seekbar_y_right;
    private SeekBar seekbar_z_right;

    TextView textViewXRight;
    TextView textViewYRight;
    TextView textViewZRight;
    TextView textViewXLeft;
    TextView textViewYLeft;
    TextView textViewZLeft;

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

    Button button_lensy_bwd;
    Button button_lensx_bwd;
    Button button_lensy_fwd;
    Button button_lensx_fwd;



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
        button_lensx_fwd = findViewById(R.id.button_lensx_fwd);
        button_lensx_bwd = findViewById(R.id.button_lensx_bwd);
        button_lensy_fwd = findViewById(R.id.button_lensy_fwd);
        button_lensy_bwd = findViewById(R.id.button_lensy_bwd);

        // set seekbar and coresponding texts for GUI
        seekbar_x_left = (SeekBar) findViewById(R.id.seekbar_x_left);
        seekbar_y_left = (SeekBar) findViewById(R.id.seekbar_y_left);
        seekbar_z_left = (SeekBar) findViewById(R.id.seekbar_z_left);
        seekbar_x_right = (SeekBar) findViewById(R.id.seekbar_x_right);
        seekbar_y_right = (SeekBar) findViewById(R.id.seekbar_y_right);
        seekbar_z_right = (SeekBar) findViewById(R.id.seekbar_z_right);

        textViewXRight = findViewById(R.id.textViewXRight);
        textViewYRight = findViewById(R.id.textViewYRight);
        textViewZRight = findViewById(R.id.textViewZRight);
        textViewXLeft = findViewById(R.id.textViewXLeft);
        textViewYLeft = findViewById(R.id.textViewYLeft);
        textViewZLeft = findViewById(R.id.textViewZLeft);

        //set change listener
        seekbar_x_left.setOnSeekBarChangeListener(this);
        seekbar_y_left.setOnSeekBarChangeListener(this);
        seekbar_z_left.setOnSeekBarChangeListener(this);
        seekbar_x_right.setOnSeekBarChangeListener(this);
        seekbar_y_right.setOnSeekBarChangeListener(this);
        seekbar_z_right.setOnSeekBarChangeListener(this);


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


        //******************* STEPPER for coarse Lens movement in XY ********************************************//
        // this goes wherever you setup your button listener:
        button_lensx_fwd.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage("stepper/lens/x/fwd", "10");
                }
                return true;
            }
        });
        button_lensx_bwd.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage("stepper/lens/x/bwd", "1");
                }
                return true;
            }
        });
        button_lensy_fwd.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage("stepper/lens/y/fwd", "10");
                }
                return true;
            }
        });
        button_lensy_bwd.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    publishMessage("stepper/lens/y/bwd", "1");
                }
                return true;
            }
        });


    }

    @Override
    public void onProgressChanged(SeekBar bar, int progress, boolean fromUser)
    {
        if (bar.equals(seekbar_x_left)) {
            // For left Lens in X
            textViewXLeft.setText("Lens X (left): "+String.valueOf(progress));
            publishMessage("lens/left/x", String.valueOf(progress));
        }
        else if (bar.equals(seekbar_y_left))
        {
            // For left Lens in Y
            textViewYLeft.setText("Lens Y (left): "+String.valueOf(progress));
            publishMessage("lens/left/y", String.valueOf(progress));
        }
        else if (bar.equals(seekbar_z_left))
        {
            // For left Lens in Z
            textViewZLeft.setText("Lens Z (left): "+String.valueOf(progress));
            publishMessage("lens/left/z", String.valueOf(progress));
        }
        else if (bar.equals(seekbar_x_right))
        {
            // For right Lens in X
            textViewXRight.setText("Lens X (right): "+String.valueOf(progress));
            publishMessage("lens/right/x", String.valueOf(progress));
        }
        else if (bar.equals(seekbar_y_right))
        {
            // For right Lens in Y
            textViewYRight.setText("Lens Y (right): "+String.valueOf(progress));
            publishMessage("lens/right/y", String.valueOf(progress));
        }
        else if (bar.equals(seekbar_z_right))
        {
            // For right Lens in Z
            textViewZRight.setText("Lens Z (right): "+String.valueOf(progress));
            publishMessage("lens/right/z", String.valueOf(progress));
        }
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






}