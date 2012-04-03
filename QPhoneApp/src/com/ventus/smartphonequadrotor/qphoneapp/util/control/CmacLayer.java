package com.ventus.smartphonequadrotor.qphoneapp.util.control;

import java.util.HashMap;

import android.util.Log;

import com.ventus.smartphonequadrotor.qphoneapp.util.SimpleMatrix;


/**
 * This class contains two CMAC weight sets that will store the control weights and the
 * alternate weights as described in reference-1. This class will also be responsible for
 * the lookup of output values from the layers and for updating the layers.
 * 
 * References:
 * <ol>
 * 	<li>
 * 		Nicol, C.; Macnab, C.J.B.; Ramirez-Serrano, A.; , 
 * 		"Robust neural network control of a quadrotor helicopter,
 * 		" Electrical and Computer Engineering, 2008. CCECE 2008. 
 * 		Canadian Conference on , vol., no., pp.001233-001238, 4-7 May 2008 
 * 		doi: 10.1109/CCECE.2008.4564736 
 * 		URL: http://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=4564736&isnumber=4564482
 * 	</li>
 * 	<li>
 * 		Albus. A new approach to manipulator control: the cerebellar model articulation
 * 		controller (CMAC). J Dyn Syst Meas Control 1975;97:220-7
 * 	</li>
 * 	<li>
 * 		Albus J. Data storage in the cerebellar model articulation controller (CMAC).
 * 		J Dyn Syst Meas Control 1975;97:228-33
 * 	</li>
 * 	<li>
 * 		Yuan Y, Gu W, Yu J. Advances in neural networks - ISNN 2004. Berlin/Heidelberg:
 * 		Springer; 2004. p. 117-22
 * 	</li>
 * </ol>
 * @author abhin
 *
 */
public class CmacLayer {
	public static final String TAG = CmacLayer.class.getSimpleName();
	public static final int DEFAULT_QUANTIZATION_NUMBER = 100;
	private HashMap<SimpleMatrix, SimpleMatrix> controlWeights, alternateWeights;
	private SimpleMatrix offset;					//the inputs will be added by this before evaluation
	private int quantizationNumber;					//this is the number of cells in the defined range
	private SimpleMatrix lowerBound;				//the lower bound for the state space
	private SimpleMatrix upperBound;				//the upper bound for the state space
	private SimpleMatrix stateInterval;				//the interval between multiple quanta of the state space
													//in the various dimensions
	
	public CmacLayer(int quantizationNumber, SimpleMatrix lowerBound, SimpleMatrix upperBound, SimpleMatrix offset) {
		this.quantizationNumber = quantizationNumber;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.offset = offset;
		//pre-compute the state intervals
		stateInterval = upperBound.minus(lowerBound).divide(quantizationNumber);
		
		//if the offsets are greater than the state interval, then throw an exception
		for (int i = 0; i < stateInterval.getNumElements(); i++) {
			if (offset.get(i) > stateInterval.get(i)) {
				throw new IllegalArgumentException("Offset cannot be greater than state interval");
			}
		}
		
		//init the hash maps
		controlWeights = new HashMap<SimpleMatrix, SimpleMatrix>();
		alternateWeights = new HashMap<SimpleMatrix, SimpleMatrix>();
	}
	
	public CmacLayer(SimpleMatrix offset) {
		this(
			DEFAULT_QUANTIZATION_NUMBER, 
			CmacInputParam.getDefaultMinBound(), 
			CmacInputParam.getDefaultMaxBound(), 
			offset
		);
	}
	
	/**
	 * This method is used to query the CMAC to get the output. This method takes into
	 * account the upper and lower bounds and the quantization number.
	 * @param input
	 * @return
	 */
	public CmacOutput query(SimpleMatrix input) {
		SimpleMatrix roundedOffsettedInput = roundAndOffsetInput(input);
		
		//get the normalized position matrix for the activation function
		double activationFunction = calculateActivationFunction(input, roundedOffsettedInput);
		
		//query the layer and compute the return values
		SimpleMatrix control, alternate;
		if (controlWeights.containsKey(roundedOffsettedInput))
			control = controlWeights.get(roundedOffsettedInput);
		else
			control = new SimpleMatrix(1, CmacOutput.NUMBER_OF_WEIGHTS);
		if (alternateWeights.containsKey(roundedOffsettedInput))
			alternate = alternateWeights.get(roundedOffsettedInput);
		else
			alternate = new SimpleMatrix(1, CmacOutput.NUMBER_OF_WEIGHTS);
				
		return new CmacOutput(control, alternate, activationFunction);
	}
	
	/**
	 * This method calculates the position of the input in the state space cell. Then it uses a 
	 * spline polynomial f(x) = x^2 + 2*x^3 + x^4 to compute the activation function of the input
	 * in each of the dimensions of the state space. It then returns the product of those activation
	 * functions. 
	 * This works because the product of the activation function in each of the dimensions is
	 * the activation function of the point in the state space (in all the dimensions combined).
	 * 
	 * @param input
	 * @param roundedOffsettedInput
	 * @return
	 */
	public double calculateActivationFunction(SimpleMatrix input, SimpleMatrix roundedOffsettedInput) {
		double activationFunction = 1;	//this is the product of the activation functions
										//of each of the dimensions in the state vector
		
		if (roundedOffsettedInput == null)
			roundedOffsettedInput = roundAndOffsetInput(input);

		SimpleMatrix halfStateInterval = stateInterval.divide(2);
		SimpleMatrix normalizedPos = SimpleMatrix.ones(input.numRows(), input.numCols());
		normalizedPos = normalizedPos.minus(
			input.minus(offset).minus(roundedOffsettedInput).minus(halfStateInterval).elementWiseAbs().elementwiseDivision(halfStateInterval)
		);
		//calculate the activation function using the spline polynomial f(x) = x^2 + 2*x^3 + x^4
		for (int i = 0; i < normalizedPos.getNumElements(); i++) {
			activationFunction *= (
				Math.pow(normalizedPos.get(i), 2) 
				- 2*Math.pow(normalizedPos.get(i), 3) 
				+ Math.pow(normalizedPos.get(i), 4)
			);
		}
		
		return activationFunction;
	}
	
	/**
	 * This matrix adds the given matrices to the given index (input) of the CMAC.
	 * This method is used to update the control and alternate weights of the CMAC. 
	 * The given matrices are first weighted using the timeInterval provided.
	 * @param input
	 * @param deltaControlWeights
	 * @param deltaAlternateWeights
	 * @param timeInterval The time (milliseconds) that have passed since the last update
	 */
	public void applyDeltas(SimpleMatrix input, SimpleMatrix deltaControlWeights, SimpleMatrix deltaAlternateWeights, long timeInterval) {
		if (input.numCols() != CmacInputParam.values().length || input.numRows() != 1)
			throw new IllegalArgumentException("Size of the input matrix is illegal");
		if (deltaControlWeights.numCols() != CmacOutput.NUMBER_OF_WEIGHTS || deltaControlWeights.numRows() != 1)
			throw new IllegalArgumentException("Size of the delta control weights is illegal");
		if (deltaAlternateWeights.numCols() != CmacOutput.NUMBER_OF_WEIGHTS || deltaAlternateWeights.numRows() != 1)
			throw new IllegalArgumentException("Size of the alternate control weights is illegal");
		
		//scale the deltas according to the timeInterval
		deltaControlWeights = deltaControlWeights.mult(timeInterval/1000.0);
		deltaAlternateWeights = deltaAlternateWeights.mult(timeInterval/1000.0);
		
		//update the weights
		SimpleMatrix correctedInput = roundAndOffsetInput(input);
		if (controlWeights.containsKey(correctedInput)) {
			controlWeights.put(correctedInput, controlWeights.get(correctedInput).plus(deltaControlWeights));
		} else {
			controlWeights.put(correctedInput, deltaControlWeights);
		}
		if (alternateWeights.containsKey(correctedInput)) {
			alternateWeights.put(correctedInput, alternateWeights.get(correctedInput).plus(deltaAlternateWeights));
		} else {
			alternateWeights.put(correctedInput, deltaAlternateWeights);
		}
	}
	
	public SimpleMatrix roundAndOffsetInput(SimpleMatrix input) {
		//first round the input DOWN using the quantizationNumber, 
		//lower bound and upper bound. The rounded version of the input
		//will be used for looking up in the hash map
		SimpleMatrix roundedOffsettedInput = input.minus(offset).minus(lowerBound);
		//do a term-wise division and round of the matrix with stateInterval
		for (int i = 0; i < input.getNumElements(); i++) {
			roundedOffsettedInput.set(
				i,
				(int)(roundedOffsettedInput.get(i) / stateInterval.get(i))
			);
		}
		roundedOffsettedInput = roundedOffsettedInput.elementMult(stateInterval).plus(lowerBound);
		return roundedOffsettedInput.round();
	}
	
	public HashMap<SimpleMatrix, SimpleMatrix> getRawControlWeights () {
		return this.controlWeights;
	}
	
	public HashMap<SimpleMatrix, SimpleMatrix> getRawAlternateWeights () {
		return this.alternateWeights;
	}
}
