package com.ventus.smartphonequadrotor.qphoneapp.util.json;

public class HrpyCommand {
	private int Height;
	private float Roll;
	private float Pitch;
	private float Yaw;
	
	public HrpyCommand(int height, float roll, float pitch, float yaw) {
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
				"Height: %1$f Roll: %2$f Pitch: %3$f Yaw: %4$d", 
				Height,
				Roll,
				Pitch,
				Yaw
			);
	}
}
