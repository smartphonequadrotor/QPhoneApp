package com.ventus.smartphonequadrotor.qphoneapp.util.control;

import com.ventus.smartphonequadrotor.qphoneapp.util.SimpleMatrix;
	

/**
 * This encapsulates the output that will be produced by the CmacLayers.
 * @author abhin
 *
 */
public class CmacOutput {
	/**
	 * This is the number of weights that each CmacLayer returns given any input state.
	 */
	public static final int NUMBER_OF_WEIGHTS = 4;
	
	private SimpleMatrix controlWeights;
	private SimpleMatrix alternateWeights;
	private double activationFunction;	//depending on where in the cell the state vector
										//is, an activation function will be calculated
										//This will be done by the CmacLayer class
	
	public CmacOutput () {
		this(new SimpleMatrix(1, NUMBER_OF_WEIGHTS), new SimpleMatrix(1, NUMBER_OF_WEIGHTS), 0);
	}
	
	public CmacOutput (SimpleMatrix controlWeights, SimpleMatrix alternateWeights) {
		this.controlWeights = controlWeights;
		this.alternateWeights = alternateWeights;
		this.activationFunction = 0;
	}
	
	public CmacOutput(SimpleMatrix controlWeigths, SimpleMatrix alternateWeights, double activationFunction) {
		this.controlWeights = controlWeigths;
		this.alternateWeights = alternateWeights;
		this.activationFunction = activationFunction;
	}
	
	public SimpleMatrix getControlWeights() {
		return this.controlWeights;
	}
	
	public SimpleMatrix getAlternateWeights() {
		return this.alternateWeights;
	}
	
	public double getActivationFunction() {
		return this.activationFunction;
	}
	
	public void setActivationFunction(double activationFunction) {
		this.activationFunction = activationFunction;
	}
	
	/**
	 * This method is used to aggregate various CmacOutputs.
	 * <ol>
	 * 	<li>
	 * 		Normalizes the activation functions for all the layers.
	 * 	</li>
	 * 	<li>
	 * 		Does weighted summation over the weights from the various layers to get
	 * 		a single CmacOutput
	 * 	</li>
	 * </ol>
	 * @param activationFunctions This is a row matrix of length {@value ControlLoop#NUMBER_OF_CMAC_LAYERS}. 
	 * It doesn't need to be normalized
	 * @param controlWeights This is a matrix of dimensions {@value ControlLoop#NUMBER_OF_CMAC_LAYERS}-by-{@value CmacOutput#NUMBER_OF_WEIGHTS}
	 * @param alternateWeights This is a matrix of dimensions {@value ControlLoop#NUMBER_OF_CMAC_LAYERS}-by-{@value CmacOutput#NUMBER_OF_WEIGHTS}
	 * @return
	 */
	public static CmacOutput aggregateOutputs(SimpleMatrix activationFunctions, SimpleMatrix controlWeights, SimpleMatrix alternateWeights) {
		SimpleMatrix normalizedActivationFunctions = activationFunctions.divide(activationFunctions.elementSum());
		return new CmacOutput(
			normalizedActivationFunctions.mult(controlWeights)
			, normalizedActivationFunctions.mult(alternateWeights)
		);
	}
}
