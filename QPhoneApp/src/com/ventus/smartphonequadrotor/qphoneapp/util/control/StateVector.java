package com.ventus.smartphonequadrotor.qphoneapp.util.control;

/**
 * This class is used to encapsulate any state of the quadrotor.
 * This class, will be a part of the CmacInput class that will be used
 * to lookup values in the various Cmac layers.
 * @author abhin
 *
 */
public class StateVector {
	private float height;
	private float rollError;
	private float pitchError;
	private float yaw;
	private float heightDerivative;
	private float rollErrorDerivative;
	private float pitchErrorDerivative;
	private float yawDerivative;
	
	public StateVector(float height, float rollError, float pitchError,
			float yaw, float heightDerivative, float rollErrorDerivative,
			float pitchErrorDerivative, float yawDerivative) {
		this.height = height;
		this.rollError = rollError;
		this.pitchError = pitchError;
		this.yaw = yaw;
		this.heightDerivative = heightDerivative;
		this.rollErrorDerivative = rollErrorDerivative;
		this.pitchErrorDerivative = pitchErrorDerivative;
		this.yawDerivative = yawDerivative;
	}

	public float getHeight() {
		return height;
	}

	public void setHeight(float height) {
		this.height = height;
	}

	public float getRollError() {
		return rollError;
	}

	public void setRollError(float rollError) {
		this.rollError = rollError;
	}

	public float getPitchError() {
		return pitchError;
	}

	public void setPitchError(float pitchError) {
		this.pitchError = pitchError;
	}

	public float getYaw() {
		return yaw;
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
	}

	public float getHeightDerivative() {
		return heightDerivative;
	}

	public void setHeightDerivative(float heightDerivative) {
		this.heightDerivative = heightDerivative;
	}

	public float getRollErrorDerivative() {
		return rollErrorDerivative;
	}

	public void setRollErrorDerivative(float rollErrorDerivative) {
		this.rollErrorDerivative = rollErrorDerivative;
	}

	public float getPitchErrorDerivative() {
		return pitchErrorDerivative;
	}

	public void setPitchErrorDerivative(float pitchErrorDerivative) {
		this.pitchErrorDerivative = pitchErrorDerivative;
	}

	public float getYawDerivative() {
		return yawDerivative;
	}

	public void setYawDerivative(float yawDerivative) {
		this.yawDerivative = yawDerivative;
	}
}
