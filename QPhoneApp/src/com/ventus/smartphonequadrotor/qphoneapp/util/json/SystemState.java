package com.ventus.smartphonequadrotor.qphoneapp.util.json;

/**
 * These are the commands that the controller can send to the phone over the
 * network. These are a part of the commands and the Responses packet groups.
 * @author abhin
 *
 */
public enum SystemState {
	/**
	 * This indicates that the quadrotor is armed and its motors are spinning atleast at the 
	 * minimum speed.
	 */
	ARMED,
	
	/**
	 * This indicates that the quadrotor is disarmed and its motors are halted.
	 */
	DISARMED,
	
	/**
	 * This indicates that the quadrotor is currently undergoing calibration.
	 */
	CALIBRATING,
	
	/**
	 * This indicates that the quadrotor is done calibration.
	 */
	CALIBRATED,
	
	/**
	 * This indicates that the quadrotor could not calibrate.
	 */
	UNABLE_TO_CALIBRATE;
}
