package com.ventus.smartphonequadrotor.qphoneapp.util.json;

public class Responses {
	private TriAxisSensorResponse[] Orientation;
	private TriAxisSensorResponse[] Gyro;
	private TriAxisSensorResponse[] Accel;
	private TriAxisSensorResponse[] Mag;
	private HeightSensorResponse[] Height;
	private CameraResponse[] Camera;
	private BatteryResponse[] Battery;
	private GpsResponse[] GPS;
	private String SystemState;
	private String Debug;
	
	public Responses(	TriAxisSensorResponse[] orientation,
						TriAxisSensorResponse[] gyro,
						TriAxisSensorResponse[] accel, 
						TriAxisSensorResponse[] mag, 
						HeightSensorResponse[] height,
						CameraResponse[] camera,
						BatteryResponse[] battery, 
						GpsResponse[] gps,
						String systemState,
						String debug) {
		Orientation = orientation;
		Gyro = gyro;
		Accel = accel;
		Mag = mag;
		Height = height;
		Camera = camera;
		Battery = battery;
		GPS = gps;
		SystemState = systemState;
		Debug = debug;
	}
	
	public TriAxisSensorResponse[] getOrientation() {
		return Orientation;
	}
	public TriAxisSensorResponse[] getGyro() {
		return Gyro;
	}
	public TriAxisSensorResponse[] getAccel() {
		return Accel;
	}
	public TriAxisSensorResponse[] getMag() {
		return Mag;
	}
	public HeightSensorResponse[] getHeight() {
		return Height;
	}
	public CameraResponse[] getCamera() {
		return Camera;
	}
	public BatteryResponse[] getBattery() {
		return Battery;
	}
	public GpsResponse[] getGps() {
		return GPS;
	}
	public String getSystemState() {
		return SystemState;
	}
	public String getDebug() {
		return Debug;
	}

	@Override
	public String toString() {
		return String.format(
			"Orientation: %8$s\nGyro: %1$s\nAccel: %2$s\nMagnetometer: %9$s\nHeight: %10$s" +
			"Camera: %3$s\nBattery: %4$s\nGps: %5$s\nSystemState: %6$s\nDebug: %7$s\n",
			Gyro.toString(),
			Accel.toString(),
			Camera.toString(),
			Battery.toString(),
			GPS.toString(),
			SystemState,
			Debug,
			Orientation.toString(),
			Mag.toString(),
			Height
		);
	}
}
