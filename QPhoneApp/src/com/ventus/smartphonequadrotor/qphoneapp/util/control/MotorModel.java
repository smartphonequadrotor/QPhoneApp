package com.ventus.smartphonequadrotor.qphoneapp.util.control;

import com.ventus.smartphonequadrotor.qphoneapp.util.SimpleMatrix;

/**
 * This encapsulates the behaviour of the motors (including its non linearities)
 * along with the definitions of motor speed used by the QCFP.
 * @author abhin
 *
 */
public class MotorModel {
	
	public static final int NUMBER_OF_MOTORS = 4;
	public static final byte MIN_QCFP_MOTOR_SPEED = 0x0;
	public static final byte MAX_QCFP_MOTOR_SPEED = 0x50;
	
	/**
	 * Converts the rotations-per-second value to the duty cycle.
	 * This function is specific to motors 1, 2 and 3.
	 * @param rps motor speeds in rotations per second
	 * @return duty cycle
	 */
	public static double motor123RpsToDutyCycle(double rps) {
		double temp;
		temp = 0.0;
		
		// coefficients
		double a = 6.0591653718273717E+00;
		double b = -4.0488409071747195E-02;
		double c = 1.5810285607433920E-03;
		double d = -1.5588605135663991E-05;
		double f = 5.7274865787604323E-08;
		
		temp += a + b * rps + c * Math.pow(rps, 2.0) + d * Math.pow(rps, 3.0) + f * Math.pow(rps, 4.0);
		return temp;
	}
	
	/**
	 * Converts the rotations-per-second value to the duty cycle.
	 * This function is specific to motor 4.
	 * @param rps motor speeds in rotations per second
	 * @return duty cycle
	 */
	public static double motor4RpsToDutyCycle(double rps) {
		double temp;
		temp = 0.0;
		
		// coefficients
		double a = 6.0890556405092111E+00;
		double b = -4.1825141232435560E-02;
		double c = 1.5307073977065305E-03;
		double d = -1.4529077018987592E-05;
		double f = 5.1597815214909915E-08;

		temp += a + b * rps + c * Math.pow(rps, 2.0) + d * Math.pow(rps, 3.0) + f * Math.pow(rps, 4.0);
		return temp;
	}
	
	/**
	 * This converts the duty cycle to values recognized by the qcfp.
	 * 5.75 corresponds to 0
	 * 6.0 corresponds to 1 and so on
	 * This linear progression goes on till 11.
	 * @param dutyCycle
	 * @return
	 */
	private static byte getQcfpByteFromDutyCycle(double dutyCycle) {
		final double initialDutyCycle = 5.75;
		final int qcfpScaleFactor = 20;
		int qcfpVal = (int) Math.floor((dutyCycle - initialDutyCycle) * qcfpScaleFactor);
		return (byte) (qcfpVal & 0x000000ff);
	}
	
	/**
	 * This converts the rps values of motor speeds into values understood by the QCFP protocol.
	 * @param rps
	 * @return
	 */
	public static byte[] motorRpsToQcfpValues(SimpleMatrix rps) {
		if (rps.getNumElements() != NUMBER_OF_MOTORS)
			throw new IllegalArgumentException("The number of motors is not correct");
		
		byte[] qcfpValues = new byte[NUMBER_OF_MOTORS];
		
		for (int i = 0; i < NUMBER_OF_MOTORS; i++) {
			if (i < 3) {
				qcfpValues[i] = getQcfpByteFromDutyCycle(motor123RpsToDutyCycle(rps.get(i)));
			} else if (i == 4) {
				qcfpValues[i] = getQcfpByteFromDutyCycle(motor4RpsToDutyCycle(rps.get(i)));
			}
			
			//now cap the values to the allowed range
			if (qcfpValues[i] < MIN_QCFP_MOTOR_SPEED)
				qcfpValues[i] = MIN_QCFP_MOTOR_SPEED;
			if (qcfpValues[i] > MAX_QCFP_MOTOR_SPEED)
				qcfpValues[i] = MAX_QCFP_MOTOR_SPEED;
		}
		
		return qcfpValues;
	}
}
