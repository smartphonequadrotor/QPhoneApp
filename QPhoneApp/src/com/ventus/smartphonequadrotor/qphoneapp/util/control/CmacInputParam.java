package com.ventus.smartphonequadrotor.qphoneapp.util.control;


/**
 * Various parameters related to the CmacInput vector like the various default
 * values and the indexes.
 * @author abhin
 *
 */
public enum CmacInputParam {
	/**
	 * current height - desired height
	 * The bounds are defined in meters.
	 */
	HEIGHT_ERROR(0, 0, 50),
	
	/**
	 * current roll - desired roll
	 * The bounds are defined in radians.
	 */
	ROLL_ERROR(1, -Math.PI, Math.PI),
	
	/**
	 * current Pitch - desired Pitch
	 * The bounds are defined in radians.
	 */
	PITCH_ERROR(2, -Math.PI, Math.PI),
	
	/**
	 * current yaw - desired yaw
	 * The bounds are defined in radians.
	 */
	YAW_ERROR(3, -Math.PI, Math.PI),
	
	/**
	 * first derivative of "height"
	 * The bounds are defined in meters/second.
	 */
	HEIGHT_ERROR_DERIVATIVE(4, -40, 12),
	
	/**
	 * first derivative of "rollError"
	 * The bounds are defined in radians/sec.
	 */
	ROLL_ERROR_DERIVATIVE(5, -Math.PI/2, Math.PI/2),
	
	/**
	 * first derivative of "PitchError"
	 * The bounds are defined in radians/sec.
	 */
	PITCH_ERROR_DERIVATIVE(6, -Math.PI/2, Math.PI/2),
	
	/**
	 * first derivative of "yaw"
	 * The bounds are defined in radians/sec.
	 */
	YAW_ERROR_DERIVATIVE(7, -Math.PI/2, Math.PI/2),
	
	/**
	 * first derivative of the desired roll
	 * The bounds are defined in radians/sec.
	 */
	DESIRED_ROLL_DERIVATIVE(8, -Math.PI/2, Math.PI/2),
	
	/**
	 * first derivative of the desired Pitch
	 * The bounds are defined in radians/sec.
	 */
	DESIRED_PITCH_DERIVATIVE(9, -Math.PI/2, Math.PI/2),
	
	/**
	 * second derivative of the desired roll
	 * The bounds are defined in radians/sec^2.
	 */
	DESIRED_ROLL_SECOND_DERIVATIVE(10, -Math.PI/4, Math.PI/4),
	
	/**
	 * second derivative of the desired Pitch
	 * The bounds are defined in radians/sec^2.
	 */
	DESIRED_PITCH_SECOND_DERIVATIVE(11, -Math.PI/4, Math.PI/4),
	
	/**
	 * if the rotor speeds are labelled as RS1, RS2, RS3 and RS4 
	 * then this is -RS1+RS2-RS3+RS4 from the previous time step.
	 * The bounds are defined in radians/sec.
	 */
	NET_PREVIOUS_ROTOR_SPEED(12, -2*Math.PI*12000, 2*Math.PI*12000);
	
	public int index;
	public double minBound, maxBound;
	
	CmacInputParam(int index, double minBound, double maxBound) {
		this.index = index;
		this.minBound = minBound;
		this.maxBound = maxBound;
	}
	
	public static SimpleMatrix getDefaultMinBound() {
		SimpleMatrix minBound = new SimpleMatrix(1, values().length);
		for (int i = 0; i < CmacInputParam.values().length; i++) {
			minBound.set(1, i, values()[i].minBound);
		}
		return minBound;
	}
	
	public static SimpleMatrix getDefaultMaxBound() {
		SimpleMatrix maxBound = new SimpleMatrix(1, values().length);
		for (int i = 0; i < CmacInputParam.values().length; i++) {
			maxBound.set(1, i, values()[i].maxBound);
		}
		return maxBound;
	}
	
	/**
	 * Calculates the state errors from the given matrix that is an input to the CMAC.
	 * The state error matrix is defined between equations 3 and 4 of main-paper.
	 * @param input The input to the CMAC. This matrix is defined as "q" above equation
	 * 7 of the main-paper.
	 * @param currentStateErrorGain the state error gain defined in the equation for z after
	 * equation 3 in the main-paper as upper-case lambda
	 * @return
	 */
	public static SimpleMatrix getStateErrors(SimpleMatrix input, double currentStateErrorGain) {
		if (input.numCols() != values().length || input.numRows() != 1) {
			throw new IllegalArgumentException(String.format("The input matrix must be of size 1-by-%d", values().length));
		}
		return new SimpleMatrix(1, 4, true, new double[] {
			(input.get(0) * currentStateErrorGain) + input.get(4),
			(input.get(1) * currentStateErrorGain) + input.get(5),
			(input.get(2) * currentStateErrorGain) + input.get(6),
			(input.get(3) * currentStateErrorGain) + input.get(7)
		});
	}
}
