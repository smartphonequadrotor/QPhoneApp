package com.ventus.smartphonequadrotor.qphoneapp.util.control;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * This is a looper thread that handles incoming messages from the data aggregator,
 * computes the control signals and sends them back.
 * @author abhin
 *
 */
public class ControlLoop extends Thread {
	public static final String TAG = ControlLoop.class.getName();
	public static final int NUMBER_OF_CMAC_LAYERS = 5;
	public static final int QUANTIZATION_NUMBER = 100;
	public static final float alternateWeightsLearningGain = 1000;		//Kappa in equation 14 of main-paper
	public static final float learningErrorGain = 0.001f;				//alpha in equations 14 and 15 of main-paper
	public static final float alternateWeightDeviationGain = 0.001f;	//rho in equation 14 of main-paper
	public static final float leakageTermGain = 0.000001f;				//nu in equation 14 of main-paper
	public static final float controlWeightsLearningGain = 1000;		//beta in equation 14 of main-paper
	public static final float guideWeightsGain = 0.000001f;				//eta-1 in equation 14 of main-paper
	public static final float guideWeightsGainDeadzone = 0.00001f;		//eta-2 in equation 14 of main-paper
	public static final float deadZone = 10;							//lower-case delta in equations 14 and 15 of main-paper
	public static final double currentStateErrorGain = 4;				//defined after equation 3 as upper-case lambda in main-paper
	
	private Handler controlSignalHandler;	//the handler that will accept the output
											//from this control loop
	private CmacLayer[] cmacLayers;
	
	/**
	 * Constructor
	 * @param controlSignalHandler
	 */
	public ControlLoop(Handler controlSignalHandler) {
		super("Control Loop");	//set the thread name for debugging
		this.controlSignalHandler = controlSignalHandler;
		cmacLayers = new CmacLayer[NUMBER_OF_CMAC_LAYERS];
		// compute the offset increments
		SimpleMatrix offsetIncrement = CmacInputParam.getDefaultMaxBound()
										.minus(CmacInputParam.getDefaultMinBound())
										.divide(NUMBER_OF_CMAC_LAYERS * QUANTIZATION_NUMBER);
		SimpleMatrix currentOffset = new SimpleMatrix(offsetIncrement.numRows(), offsetIncrement.numCols());
		for (int i = 0; i < NUMBER_OF_CMAC_LAYERS; i++) {
			cmacLayers[i] = new CmacLayer(
				QUANTIZATION_NUMBER, 
				CmacInputParam.getDefaultMinBound(), 
				CmacInputParam.getDefaultMaxBound(),
				currentOffset
			);
			currentOffset = currentOffset.plus(offsetIncrement);
		}
	}
	
	public void run() {
		//setup looper stuff
		Looper.prepare();
		Looper.loop();
	}

	//create a handler to handle all the messages
	public Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.i(TAG, "Message received in the control loop: " + msg.obj.toString());
			//TODO
		}
	};
	
	/**
	 * This method must be called everytime inputs/state variables change.
	 * This causes the input to be used to get output from the CmacLayers.
	 * Then this output is used to computer the correction for the CMacLayers
	 * and that correction is applied. In addition, that output found in the 
	 * various CmacLayers is aggregated and returned. This output can then be 
	 * used to change the motor speed by sending it over bluetooth.
	 * @param input
	 * @return
	 */
	public CmacOutput triggerCmacUpdate(SimpleMatrix input) {
		SimpleMatrix controlWeights = new SimpleMatrix(NUMBER_OF_CMAC_LAYERS, CmacOutput.NUMBER_OF_WEIGHTS);
		SimpleMatrix alternateWeights = new SimpleMatrix(NUMBER_OF_CMAC_LAYERS, CmacOutput.NUMBER_OF_WEIGHTS);
		SimpleMatrix activationFunctions = new SimpleMatrix(1, NUMBER_OF_CMAC_LAYERS);
		CmacOutput output;
		for (int i = 0; i < NUMBER_OF_CMAC_LAYERS; i++) {
			output = cmacLayers[i].query(input);
			controlWeights.insertIntoThis(i, 0, output.getControlWeights());
			alternateWeights.insertIntoThis(i, 0, output.getAlternateWeights());
			activationFunctions.set(i, output.getActivationFunction());
		}
		SimpleMatrix normalizedActivationFunctions = activationFunctions.divide(activationFunctions.elementSum());
		
		//get the weight difference that will later be used to computer the weight corrections
		//(refer to equation 13 in main-paper)
		SimpleMatrix aggregatedControlWeights = normalizedActivationFunctions.mult(controlWeights);
		SimpleMatrix aggregatedAlternateWeights = normalizedActivationFunctions.mult(alternateWeights);
		SimpleMatrix weightDiff = aggregatedControlWeights.minus(aggregatedAlternateWeights);
		
		//get column-wise means of the alternate weights (refer to equation 14 in main-paper)
		SimpleMatrix meanAlternateWeights = SimpleMatrix.ones(NUMBER_OF_CMAC_LAYERS)
														.mult(alternateWeights)
														.divide(NUMBER_OF_CMAC_LAYERS);
		
		//update alternate and control weights
		double weightDiffNorm = weightDiff.normF();
		SimpleMatrix deltaControlWeights, deltaAlternateWeights;
		deltaAlternateWeights = meanAlternateWeights
								.repmat(NUMBER_OF_CMAC_LAYERS, 1)
								.minus(alternateWeights)
								.mult(alternateWeightDeviationGain);
		deltaAlternateWeights = deltaAlternateWeights.minus(
									alternateWeights.mult(leakageTermGain)
								);
		deltaControlWeights = 	normalizedActivationFunctions.transpose().mult(
									CmacInputParam.getStateErrors(input, currentStateErrorGain)
								);
		SimpleMatrix lyapunovBoundednessTerm = 	normalizedActivationFunctions	//helps guaranteed bounded signals
												.transpose()
												.mult(weightDiff)
												.mult(learningErrorGain);
		if (weightDiffNorm > deadZone) {
			deltaAlternateWeights = deltaAlternateWeights.plus(lyapunovBoundednessTerm);
			deltaControlWeights = 	deltaControlWeights.minus(lyapunovBoundednessTerm);
			deltaControlWeights = 	deltaControlWeights.plus(
										alternateWeights.minus(controlWeights).mult(guideWeightsGain)
									);
		} else {
			deltaControlWeights =	deltaControlWeights.plus(
										alternateWeights.minus(controlWeights).mult(guideWeightsGainDeadzone)
									);
		}
		deltaAlternateWeights = deltaAlternateWeights.mult(alternateWeightsLearningGain);
		deltaControlWeights = deltaControlWeights.mult(controlWeightsLearningGain);
		
		for (int i = 0; i < NUMBER_OF_CMAC_LAYERS; i++) {
			cmacLayers[i].applyDeltas(
				input,
				deltaControlWeights.extractMatrix(i, i+1, 0, deltaControlWeights.numCols()),
				deltaAlternateWeights.extractMatrix(i, i+1, 0, deltaAlternateWeights.numCols())
			);
		}
		
		return new CmacOutput(aggregatedControlWeights , aggregatedAlternateWeights ,0);
	}
}
