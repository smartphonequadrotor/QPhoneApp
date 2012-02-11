package com.ventus.smartphonequadrotor.qphoneapp.util.json;

/**
 * This holds data from 3 axis sensors like the gyroscope, acceleromter, magnemometer etc.
 * @author Abhin
 *
 */
public class TriAxisSensorResponse extends ResponseAbstract {
	private float X;
	private float Y;
	private float Z;
	
	public TriAxisSensorResponse(long Timestamp, float X, float Y, float Z) {
		super(Timestamp);
		this.X = X;
		this.Y = Y;
		this.Z = Z;
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

	@Override
	public String toString() {
		return String.format("%1$sX: %2$f\nY: %3$f\nZ: %4$f\n", super.toString(), X, Y, Z);
	}
}
