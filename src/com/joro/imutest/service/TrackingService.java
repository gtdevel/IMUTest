package com.joro.imutest.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

public class TrackingService extends Service implements SensorEventListener {
	static String SERVICE_NAME = "MOTION_SERVICE";
	final int SIZE_AXIS_ARRAYS = 1000;
	private SensorManager mSensorManager;
	private Sensor mSensor;
	private float[] timestamp;
	private int count;
	private float[] axisX;
	private float[] axisY;
	private float[] axisZ;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		Log.i(SERVICE_NAME, "Motion Service Created");
		count = 0;
		axisX = new float[SIZE_AXIS_ARRAYS];
		axisY = new float[SIZE_AXIS_ARRAYS];
		axisZ = new float[SIZE_AXIS_ARRAYS];
		timestamp = new float[SIZE_AXIS_ARRAYS];
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		// mMotionData=new MotionData(this);

		// TODO Auto-generated method stub
		super.onCreate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(SERVICE_NAME, "Motion Service Started");
		mSensorManager.registerListener(this, mSensor,
				SensorManager.SENSOR_DELAY_UI);
		// mMotionData.open();
		// TODO Auto-generated method stub
		return START_STICKY;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		mSensorManager.unregisterListener(this);
		Log.i(SERVICE_NAME, "Motion Service Stopped");
		storeData(count - 1);
		// Cursor mCursor = mMotionData.getAll();
		// mMotionData.close();
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.hardware.SensorEventListener#onSensorChanged(android.hardware
	 * .SensorEvent)
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		
		timestamp[count] = event.timestamp;
		axisX[count] = event.values[0];
		axisY[count] = event.values[1];
		axisZ[count] = event.values[2];
		Log.i(SERVICE_NAME,
				"Count=" + String.valueOf(count) + ", Timestamp="
						+ String.valueOf(timestamp[count]) + ", axisX="
						+ String.valueOf(axisX[count]) + ", axisX="
						+ String.valueOf(axisY[count]) + ", axisX="
						+ String.valueOf(axisZ[count]));

		if (count == SIZE_AXIS_ARRAYS - 1) {
			storeData(count);
		} else {
			count++;
		}

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	private void storeData(int size) {
		File folder = new File(Environment.getExternalStorageDirectory()
				+ "/Download");
		if (!folder.exists()) {
			folder.mkdir();
		}
		Log.i(SERVICE_NAME, "Saving Data");
		final String filename = folder.toString() + "/Data.csv";
		try {
			FileWriter fw = new FileWriter(filename, true);
			for (int i = 0; i < size; i++) {
				fw.append(String.valueOf(timestamp[i]) + ','
						+ String.valueOf(axisX[i]) + ','
						+ String.valueOf(axisY[i]) + ','
						+ String.valueOf(axisZ[i]) + '\n');
			}
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		count = 0;
	}
}
