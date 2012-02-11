package com.ventus.smartphonequadrotor.qphoneapp.util.json;

/**
 * This holds data from 3 axis sensors like the gyroscope, acceleromter, magnemometer etc.
 * @author Abhin
 *
 */
public class TriAxisSensorData {
	private float X;
	private float Y;
	private float Z;
	
	public TriAxisSensorData(float x, float y, float z) {
		this.X = x;
		this.Y = y;
		this.Z = z;
	}
	
	public float getX() {
		return X;
	}
	public float getY() {
		return Y;
	}
	public float getZ() {
		return Z;
	}
}
