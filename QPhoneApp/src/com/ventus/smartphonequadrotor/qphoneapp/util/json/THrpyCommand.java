package com.ventus.smartphonequadrotor.qphoneapp.util.json;

public class THrpyCommand {
	private int Height;
	private float Roll;
	private float Pitch;
	private float Yaw;
	private int Throttle;
	
	public THrpyCommand(int throttle, int height, float roll, float pitch, float yaw) {
		Throttle = throttle;
		Height = height;
		Roll = roll;
		Pitch = pitch;
		Yaw = yaw;
	}

	public int getHeight() {
		return Height;
	}

	public float getRoll() {
		return Roll;
	}

	public float getPitch() {
		return Pitch;
	}

	public float getYaw() {
		return Yaw;
	}

	@Override
	public String toString() {
		return String.format(
				"Throttle: %1$f Height: %2$f Roll: %3$f Pitch: %4$f Yaw: %5$d", 
				Throttle,
				Height,
				Roll,
				Pitch,
				Yaw
			);
	}

	public int getThrottle() {
		return Throttle;
	}
}
