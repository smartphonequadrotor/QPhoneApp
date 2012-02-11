package com.ventus.smartphonequadrotor.qphoneapp.util.json;

public class GpsResponse extends ResponseAbstract {
	private float Bearing;
	private float Accuracy;
	private double Altitude;
	private float Speed;
	private double Latitude;
	private double Longitude;
	public GpsResponse(	long Timestamp, 
						float bearing, 
						float accuracy,
						double altitude, 
						float speed, 
						double latitude, 
						double longitude	) {
		super(Timestamp);
		Bearing = bearing;
		Accuracy = accuracy;
		Altitude = altitude;
		Speed = speed;
		Latitude = latitude;
		Longitude = longitude;
	}
	
	public float getBearing() {
		return Bearing;
	}
	public float getAccuracy() {
		return Accuracy;
	}
	public double getAltitude() {
		return Altitude;
	}
	public float getSpeed() {
		return Speed;
	}
	public double getLatitude() {
		return Latitude;
	}
	public double getLongitude() {
		return Longitude;
	}

	public String toString() {
		return String.format(
			"%1$sBearing: %2$f\nAccuracy: %3$f\nAltitude: %4$f\nSpeed: %5$f\nLatitude: %6$fLongitude: %7$f",
			super.toString(),
			Bearing,
			Accuracy,
			Altitude,
			Speed,
			Latitude,
			Longitude
		);
	}
}
