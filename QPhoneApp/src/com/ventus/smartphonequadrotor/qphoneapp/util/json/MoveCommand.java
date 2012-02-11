package com.ventus.smartphonequadrotor.qphoneapp.util.json;

public class MoveCommand {
	private float XVector;
	private float YVector;
	private float ZVector;
	private int Speed;
	private int Duration;
	public MoveCommand(float xVector, float yVector, float zVector, int speed, int duration) {
		XVector = xVector;
		YVector = yVector;
		ZVector = zVector;
		Speed = speed;
		Duration = duration;
	}
	public float getXVector() {
		return XVector;
	}
	public float getYVector() {
		return YVector;
	}
	public float getZVector() {
		return ZVector;
	}
	public int getSpeed() {
		return Speed;
	}
	public int getDuration() {
		return Duration;
	}
	@Override
	public String toString() {
		return String.format(
			"Vector: {%1$f, %2$f, %3$f}\nSpeed: %4$d\nDuration: %5$d", 
			XVector, 
			YVector, 
			ZVector, 
			Speed, 
			Duration
		);
	}	
}
