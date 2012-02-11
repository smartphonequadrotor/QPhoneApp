package com.ventus.smartphonequadrotor.qphoneapp.util.json;

public class Responses {
	private TriAxisSensorResponse[] Gyro;
	private TriAxisSensorResponse[] Accel;
	private CameraResponse[] Camera;
	private BatteryResponse[] Battery;
	private GpsResponse[] GPS;
	
	public Responses(	TriAxisSensorResponse[] gyro,
						TriAxisSensorResponse[] accel, 
						CameraResponse[] camera,
						BatteryResponse[] battery, 
						GpsResponse[] gps	) {
		Gyro = gyro;
		Accel = accel;
		Camera = camera;
		Battery = battery;
		GPS = gps;
	}
	
	public TriAxisSensorResponse[] getGyro() {
		return Gyro;
	}
	public TriAxisSensorResponse[] getAccel() {
		return Accel;
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

	@Override
	public String toString() {
		return String.format(
			"Gyro: %1$s\nAccel: %2$s\nCamera: %3$s\nBattery: %4$s\nGps: %5$s",
			Gyro.toString(),
			Accel.toString(),
			Camera.toString(),
			Battery.toString(),
			GPS.toString()
		);
	}
}
