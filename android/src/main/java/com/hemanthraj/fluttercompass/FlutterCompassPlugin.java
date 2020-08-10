package com.hemanthraj.fluttercompass;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;


import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.EventSink;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH;

public final class FlutterCompassPlugin implements StreamHandler {
    // A static variable which will retain the value across Isolates.
    private static double currentAzimuth = 0;
    private static int currentAccuracy = SENSOR_STATUS_ACCURACY_HIGH;

    private double newAzimuth;
    private double filter;
    private SensorEventListener sensorEventListener;

    private final SensorManager sensorManager;
    private final Sensor sensor;
    private final float[] orientation;
    private final float[] rMat;

    public static void registerWith(Registrar registrar) {
        EventChannel channel = new EventChannel(registrar.messenger(), "hemanthraj/flutter_compass");
        channel.setStreamHandler(new FlutterCompassPlugin(registrar.context(), Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR));
    }


    public void onListen(Object arguments, EventSink events) {
        // Added check for the sensor, if null then the device does not have the TYPE_ROTATION_VECTOR sensor
        if (sensor != null) {
            sensorEventListener = createSensorEventListener(events);
            sensorManager.registerListener(sensorEventListener, this.sensor, SensorManager.SENSOR_DELAY_UI);
            events.success(new double[]{currentAzimuth, currentAccuracy});
        } else {
            // Send null to Flutter side
            events.success(null);
        }
    }

    public void onCancel(Object arguments) {
        this.sensorManager.unregisterListener(this.sensorEventListener);
    }

    private SensorEventListener createSensorEventListener(final EventSink events) {
        return new SensorEventListener() {
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                if (currentAccuracy != accuracy) {
                    currentAccuracy = accuracy;
                    events.success(new double[]{currentAzimuth, (double) accuracy});
                }
            }

            public void onSensorChanged(SensorEvent event) {
                SensorManager.getRotationMatrixFromVector(rMat, event.values);
                newAzimuth = ((Math.toDegrees((double) SensorManager.getOrientation(rMat, orientation)[0]) + (double) 360) % (double) 360 - Math.toDegrees((double) SensorManager.getOrientation(rMat, orientation)[2]) + (double) 360) % (double) 360;
                if (Math.abs(currentAzimuth - newAzimuth) >= filter) {
                    currentAzimuth = newAzimuth;
                    events.success(new double[]{newAzimuth, (double) currentAccuracy});
                }
            }
        };
    }

    private FlutterCompassPlugin(Context context, int sensorType, int fallbackSensorType) {
        filter = 1.0F;

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        orientation = new float[3];
        rMat = new float[9];
        Sensor defaultSensor = this.sensorManager.getDefaultSensor(sensorType);
        if (defaultSensor != null) {
            sensor = defaultSensor;
        } else {
            sensor = this.sensorManager.getDefaultSensor(fallbackSensorType);
        }
    }

}
