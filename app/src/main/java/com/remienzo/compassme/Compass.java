package com.remienzo.compassme;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
public class Compass implements SensorEventListener {


    private CompassListener listener;

    private SensorManager sensorManager;
    private Sensor gravitySensor;
    private Sensor magneticSensor;

    private float[] gravityVector = new float[3];
    private float[] magneticVector = new float[3];
    private float[] rotationMatrix = new float[9];

    private float azimuth; // angle (rad) entre l'axe y du telephone et le nord
    // bearing : angle (deg) entre la direction nord et le vecteur entre notre position et celle de la target
    private float bearing = 0;
    public Compass(Context context) {
        sensorManager = (SensorManager) context
                .getSystemService(Context.SENSOR_SERVICE);
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void start() {
        sensorManager.registerListener(this, gravitySensor,
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magneticSensor,
                SensorManager.SENSOR_DELAY_GAME);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    public void setBearing(float fix) {
        this.bearing = fix;
    }


    public void setListener(CompassListener l) {
        listener = l;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        synchronized (this) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                gravityVector = event.values;
            }
            else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                magneticVector = event.values;
            }
            boolean success = SensorManager.getRotationMatrix(rotationMatrix, null, gravityVector, magneticVector);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientation);
                azimuth = (float) Math.toDegrees(orientation[0]);
                azimuth = (azimuth + bearing) % 360;
                if (listener != null) {
                    listener.onNewBearing(azimuth);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
