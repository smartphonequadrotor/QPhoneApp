package com.ventus.smartphonequadrotor.qphoneapp.util.control;

import org.ejml.simple.SimpleMatrix;

/**
 * This encapsulates the data that the CMAC layers need to compute the output.
 * @author abhin
 * @deprecated use {@link SimpleMatrix} directly.
 */
public class CmacInput {
	public SimpleMatrix stateVariables;	//row matrix
	public static final float PI = (float)Math.PI;

	public CmacInput(double heightError, double rollError, double pitchError,
			double yawError, double heightErrorDerivative, double rollErrorDerivative,
			double pitchErrorDerivative, double yawErrorDerivative,
			double desiredRollDerivative, double desiredPitchDerivative,
			double desiredRollSecondDerivative,
			double desiredPitchSecondDerivative, double netPreviousRotorSpeed) {
		stateVariables = new SimpleMatrix (
			1,
			13,
			true,
			heightError,
			rollError,
			pitchError,
			yawError,
			heightErrorDerivative,
			rollErrorDerivative,
			pitchErrorDerivative,
			yawErrorDerivative,
			desiredRollDerivative,
			desiredPitchDerivative,
			desiredRollSecondDerivative,
			desiredPitchSecondDerivative,
			netPreviousRotorSpeed
		);
	}
	
	public CmacInput() {
		stateVariables = new SimpleMatrix(1, 13);
	}
	
	public CmacInput(SimpleMatrix stateVariables) {
		this.stateVariables = stateVariables;
	}

	public double getStateVariable(CmacInputParam stateVar) {
		return this.stateVariables.get(stateVar.index);
	}
	
	public void setStateVariable(CmacInputParam stateVar, double var) {
		this.stateVariables.set(stateVar.index, var);
	}
	
	/*
	 * STATIC HELPER METHODS
	 * ================================================
	 */
	
	public static CmacInput add (CmacInput arg1, CmacInput arg2) {
		CmacInput sum = new CmacInput();
		for (CmacInputParam param : CmacInputParam.values()) {
			sum.setStateVariable(param, arg1.getStateVariable(param) + arg2.getStateVariable(param));
		}
		return sum;
	}
	
	public static CmacInput subtract (CmacInput arg1, CmacInput arg2) {
		CmacInput sum = new CmacInput();
		for (CmacInputParam param : CmacInputParam.values()) {
			sum.setStateVariable(param, arg1.getStateVariable(param) - arg2.getStateVariable(param));
		}
		return sum;
	}
	
	public static CmacInput getDefaultMinBound() {
		CmacInput minBound = new CmacInput();
		for (CmacInputParam param : CmacInputParam.values()) {
			minBound.setStateVariable(param, param.minBound);
		}
		return minBound;
	}
	
	public static CmacInput getDefaultMaxBound() {
		CmacInput maxBound = new CmacInput();
		for (CmacInputParam param : CmacInputParam.values()) {
			maxBound.setStateVariable(param, param.maxBound);
		}
		return maxBound;
	}
}
