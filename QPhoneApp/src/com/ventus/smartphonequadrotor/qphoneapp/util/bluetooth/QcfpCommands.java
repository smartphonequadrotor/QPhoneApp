package com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth;

public class QcfpCommands {
	public static byte QCFP_ASYNC_DATA = 0x10;

	public static byte QCFP_CALIBRATE_QUADROTOR = 0x40;
	public static byte QCFP_CALIBRATE_QUADROTOR_STOP = 0x00;
	public static byte QCFP_CALIBRATE_QUADROTOR_START = 0x01;
	
	public static byte QCFP_SET_THROTTLE = 0x24;
	public static byte QCFP_SET_DESIRED_ANGLES = 0x25;
	public static byte QCFP_SET_DESIRED_HEIGHT = 0x26;
	public static byte QCFP_ALTITUDE_HOLD_EN = 0x27;
	
	public static byte QCFP_CALIBRATE_QUADROTOR_UNCALIBRATED = 0x00;
	public static byte QCFP_CALIBRATE_QUADROTOR_CALIBRATED = 0x01;
	public static byte QCFP_CALIBRATE_QUADROTOR_CALIBRATING = 0x02;
	public static byte QCFP_CALIBRATE_QUADROTOR_UNABLE_TO_CALIBRATE = 0x03;

	public static byte QCFP_FLIGHT_MODE = 0x41;
	public static byte QCFP_FLIGHT_MODE_DISABLE = 0x00;
	public static byte QCFP_FLIGHT_MODE_ENABLE = 0x01;
	public static byte QCFP_FLIGHT_MODE_PENDING = 0x02;

	public static byte QCFP_RAW_MOTOR_CONTROL = (byte) 0xF0; 	// Verify that this
																// cast doesn't
																// change the
																// bit pattern
																// or anything
}
