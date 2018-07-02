package com.remienzo.compassme;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.location.Address;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;

public class CompassActivity extends AppCompatActivity implements SensorEventListener, StepListener {
    private Location currentLocation;
    private Compass compass;
    private ImageView arrowImageView;
    private EditText addressSearchZone;
    private Button compassMeButton;
    private float currentAzimuth;
    private Location targetLocation = new Location("");
    private final static int TARGET_REFRESH_DELAY = 3000;
    private TextView textView;
    private StepSensor simpleStepDetector;
    private SensorManager sensorManager;
    private Sensor accel;
    private static final String SUFFIX = " STEPS";
    private int numSteps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.targetNorth();
        if (CompassActivity.this.checkSelfPermission(Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            CompassActivity.this.requestPermissions(new String[]{Manifest.permission.INTERNET},
                    1);
        }
        if (CompassActivity.this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            CompassActivity.this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }
        setContentView(R.layout.activity_compass);
        arrowImageView = (ImageView) findViewById(R.id.main_image_hands);
        setupCompass();
        addressSearchZone = (EditText) findViewById(R.id.editText);
        compassMeButton = (Button) findViewById(R.id.button2);
        compassMeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(addressSearchZone.getText().toString().length() == 0){
                    CompassActivity.this.targetNorth();
                }
                else {
                    Geocoder geocoder = new Geocoder(CompassActivity.this);
                    List<Address> addresses = null;
                    try {
                        addresses = geocoder.getFromLocationName(addressSearchZone.getText().toString(), 1);
                    } catch (Exception e) {
                    }
                    if (addresses != null && addresses.size() > 0) {
                        CompassActivity.this.targetLocation.setLatitude(addresses.get(0).getLatitude());
                        CompassActivity.this.targetLocation.setLongitude(addresses.get(0).getLongitude());
                    }
                }
            }
        });
        CompassActivity.this.update();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable(){
            public void run(){
                CompassActivity.this.update();
                handler.postDelayed(this, CompassActivity.TARGET_REFRESH_DELAY);
            }
        }, TARGET_REFRESH_DELAY);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new StepSensor();
        simpleStepDetector.setListener(this);

        textView = findViewById(R.id.tv_steps);
        textView.setText("0 STEP");
        Button BtnStart = findViewById(R.id.btn_start);
        Button BtnStop = findViewById(R.id.btn_stop);



        BtnStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {

                numSteps = 0;
                sensorManager.registerListener(CompassActivity.this, accel, SensorManager.SENSOR_DELAY_FASTEST);

            }
        });


        BtnStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {

                sensorManager.unregisterListener(CompassActivity.this);

            }
        });
    }

    protected void update(){
        LocationManager locationManager = (LocationManager)CompassActivity.this.getSystemService(LOCATION_SERVICE);
        try {
            Location location=null;
            try {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            catch(SecurityException e){}
            if (location != null) {
                currentLocation = location;
            }
            CompassActivity.this.compass.setBearing(-currentLocation.bearingTo(CompassActivity.this.targetLocation));
        }
        catch(Exception e){}

    }

    protected void targetNorth(){
        targetLocation.setLatitude(90);
        targetLocation.setLongitude(0.0f);
    }
    @Override
    protected void onStart() {
        super.onStart();
        compass.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        compass.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        compass.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        compass.stop();
    }

    private void setupCompass() {
        compass = new Compass(this);
        CompassListener cl = new CompassListener() {

            @Override
            public void onNewBearing(float bearing) {
                adjustArrow(bearing);
            }
        };
        compass.setListener(cl);
    }

    private void adjustArrow(float azimuth) {
        //Log.d(TAG, "will set rotation from " + currentAzimuth + " to " + azimuth);

        Animation an = new RotateAnimation(-currentAzimuth, -azimuth,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        currentAzimuth = azimuth;

        an.setDuration(500);
        an.setRepeatCount(0);
        an.setFillAfter(true);

        arrowImageView.startAnimation(an);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.update(event.values[0], event.values[1], event.values[2]);
        }
    }

    public void stepOccurred() {
        numSteps++;
        textView.setText(numSteps + SUFFIX);
    }
}
