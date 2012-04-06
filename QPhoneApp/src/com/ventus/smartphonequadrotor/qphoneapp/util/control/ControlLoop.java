package com.ventus.smartphonequadrotor.qphoneapp.util.control;

import java.util.Date;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.ventus.smartphonequadrotor.qphoneapp.services.MainService;
import com.ventus.smartphonequadrotor.qphoneapp.services.MainService.BluetoothCommunicationLooper;
import com.ventus.smartphonequadrotor.qphoneapp.util.SimpleMatrix;
import com.ventus.smartphonequadrotor.qphoneapp.util.json.MoveCommand;

/**
 * This is a looper thread that handles incoming messages from the data aggregator,
 * computes the control signals and sends them back.
 * @author abhin
 *
 */
public class ControlLoop extends Thread {
	public static final String TAG = ControlLoop.class.getSimpleName();
	
	public static final int CMAC_UPDATE_MESSAGE = 1;
	public static final int MOVE_COMMAND_MESSAGE = 2;
	
	public static final int NUMBER_OF_CMAC_LAYERS = 3;
	public static final int QUANTIZATION_NUMBER = 100;
	public static final float ALTERNATE_WEIGHTS_LEARNING_GAIN = 1000;	//Kappa in equation 14 of main-paper
	public static final float LEARNING_ERROR_GAIN = 0.001f;				//alpha in equations 14 and 15 of main-paper
	public static final float ALTERNATE_WEIGHT_DEVIATION_GAIN = 0.001f;	//rho in equation 14 of main-paper
	public static final float LEAKAGE_TERM_GAIN = 0.000001f;			//nu in equation 14 of main-paper
	public static final float CONTROL_WEIGHTS_LEARNING_GAIN = 1000;		//beta in equation 14 of main-paper
	public static final float GUIDE_WEIGHTS_GAIN = 0.000001f;			//eta-1 in equation 14 of main-paper
	public static final float GUIDE_WEIGHTS_GAIN_DEADZONE = 0.00001f;	//eta-2 in equation 14 of main-paper
	public static final float DEADZONE = 10;							//lower-case delta in equations 14 and 15 of main-paper
	public static final double CURRENT_STATE_ERROR_GAIN = 4;			//defined after equation 3 as upper-case lambda in main-paper
	//the following matrix is used to convert the cmac output to motor speeds
	private static final SimpleMatrix CMAC_OUTPUT_TO_MOTOR_SPEED_MATRIX = new SimpleMatrix(
		CmacOutput.NUMBER_OF_WEIGHTS,
		CmacOutput.NUMBER_OF_WEIGHTS,
		true,
		new double[] {
			0.25,	0,		-0.5,	0.25,
			0.25,	-0.5, 	0, 		-0.25,
			0.25,	0,		0.5,	0.25,
			0.25,	0.5,	0,		-0.25
		}
	);
	
	private CmacLayer[] cmacLayers;
	private DataAggregator dataAggregator;
	
	private long lastUpdateTimestamp = -1;
	
	/**
	 * This is the handler that accepts new messages for the control loop to compute.
	 */
	public Handler handler;
	
	private MainService owner;
	
	/**
	 * Constructor
	 */
	public ControlLoop(MainService owner) {
		super("Control Loop");	//set the thread name for debugging
		cmacLayers = new CmacLayer[NUMBER_OF_CMAC_LAYERS];
		this.dataAggregator = new DataAggregator();
		this.owner = owner;
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
		Looper.prepare();
		
		//create a handler to handle all the messages
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == CMAC_UPDATE_MESSAGE) {
					if (msg.obj == null)
						throw new IllegalArgumentException("input parameters missing");
					Object[] inputParams = (Object[])msg.obj;
					SimpleMatrix errorMatrix = dataAggregator.calculateErrors(
						(Long)inputParams[0], 
						(Integer)inputParams[1], 
						(Float)inputParams[2], 
						(Float)inputParams[3], 
						(Float)inputParams[4]
					);
					if (errorMatrix != null) {
						SimpleMatrix output = cmacOutput2MotorSpeeds(triggerCmacUpdate(errorMatrix));
						double netPreviousRotorSpeed = output.elementSum();
						netPreviousRotorSpeed = (Double.isNaN(netPreviousRotorSpeed)) ? 0 : netPreviousRotorSpeed;
						dataAggregator.setNetPreviousRotorSpeed(output.elementSum());
						Message outputMsg = owner.getBtCommunicationLooper().handler
								.obtainMessage(BluetoothCommunicationLooper.MOTOR_SPEEDS_MESSAGE);
						outputMsg.obj = output;
						owner.getBtCommunicationLooper().handler.sendMessage(outputMsg);
					}
				} else if (msg.what == MOVE_COMMAND_MESSAGE) {
					if (msg.obj == null)
						throw new IllegalArgumentException("move commands missing");
					dataAggregator.processMoveCommand((MoveCommand[])msg.obj);
				}
			}
		};
		
		Looper.loop();
	}
	
	/**
	 * This method must be called everytime inputs/state variables change.
	 * This causes the input to be used to get output from the CmacLayers.
	 * Then this output is used to computer the correction for the CMacLayers
	 * and that correction is applied. In addition, that output found in the 
	 * various CmacLayers is aggregated and returned. This output can then be 
	 * used to change the motor speed after further processing by sending it
	 * over bluetooth to the QCB.
	 * 
	 * Note: The output is only composed of the control weights because that is 
	 * all that is needed to update the motor speeds.
	 * @param input 1-by-13 matrix
	 * @return 1-by-{@value CmacOutput#NUMBER_OF_WEIGHTS} matrix
	 */
	public SimpleMatrix triggerCmacUpdate(SimpleMatrix input) {
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
		double activationFunctionSum = activationFunctions.elementSum();
		SimpleMatrix normalizedActivationFunctions;
		if (activationFunctionSum != 0)
			normalizedActivationFunctions = activationFunctions.divide(activationFunctions.elementSum());
		else
			normalizedActivationFunctions = activationFunctions;
		
		//get the weight difference that will later be used to computer the weight corrections
		//(refer to equation 13 in main-paper)
		SimpleMatrix aggregatedControlWeights = normalizedActivationFunctions.mult(controlWeights);
		SimpleMatrix aggregatedAlternateWeights = normalizedActivationFunctions.mult(alternateWeights);
		SimpleMatrix weightDiff = aggregatedControlWeights.minus(aggregatedAlternateWeights);
		
		//get column-wise means of the alternate weights (refer to equation 14 in main-paper)
		SimpleMatrix meanAlternateWeights = SimpleMatrix.ones(1, NUMBER_OF_CMAC_LAYERS)
														.mult(alternateWeights)
														.divide(NUMBER_OF_CMAC_LAYERS);
		
		//update alternate and control weights
		double weightDiffNorm = weightDiff.normF();
		SimpleMatrix deltaControlWeights, deltaAlternateWeights;
		deltaAlternateWeights = meanAlternateWeights
								.repmat(NUMBER_OF_CMAC_LAYERS, 1)
								.minus(alternateWeights)
								.mult(ALTERNATE_WEIGHT_DEVIATION_GAIN);
		deltaAlternateWeights = deltaAlternateWeights.minus(
									alternateWeights.mult(LEAKAGE_TERM_GAIN)
								);
		deltaControlWeights = 	normalizedActivationFunctions.transpose().mult(
									CmacInputParam.getStateErrors(input, CURRENT_STATE_ERROR_GAIN)
								).mult(-1.0);	//TODO talk to Macnab whether this is allright.
		SimpleMatrix lyapunovBoundednessTerm = 	normalizedActivationFunctions	//helps guaranteed bounded signals
												.transpose()
												.mult(weightDiff)
												.mult(LEARNING_ERROR_GAIN);
		if (weightDiffNorm > DEADZONE) {
			deltaAlternateWeights = deltaAlternateWeights.plus(lyapunovBoundednessTerm);
			deltaControlWeights = 	deltaControlWeights.minus(lyapunovBoundednessTerm);
			deltaControlWeights = 	deltaControlWeights.plus(
										alternateWeights.minus(controlWeights).mult(GUIDE_WEIGHTS_GAIN)
									);
		} else {
			deltaControlWeights =	deltaControlWeights.plus(
										alternateWeights.minus(controlWeights).mult(GUIDE_WEIGHTS_GAIN_DEADZONE)
									);
		}
		deltaAlternateWeights = deltaAlternateWeights.mult(ALTERNATE_WEIGHTS_LEARNING_GAIN);
		deltaControlWeights = deltaControlWeights.mult(CONTROL_WEIGHTS_LEARNING_GAIN);

		long timeInterval = 0;
		long currentTimestamp = (new Date()).getTime();
		if (lastUpdateTimestamp != -1) {
			timeInterval = currentTimestamp - lastUpdateTimestamp;
		}
		lastUpdateTimestamp = currentTimestamp;
		for (int i = 0; i < NUMBER_OF_CMAC_LAYERS; i++) {
			cmacLayers[i].applyDeltas(
				input,
				deltaControlWeights.extractMatrix(i, i+1, 0, deltaControlWeights.numCols()),
				deltaAlternateWeights.extractMatrix(i, i+1, 0, deltaAlternateWeights.numCols()),
				timeInterval
			);
		}
		
		return aggregatedControlWeights;
	}
	
	/**
	 * The output from the Cmac Layers cannot be directly used to update the motor speeds.
	 * Equation A.3 from the main-paper needs to be used to convert these control weights
	 * to desired motor speeds.
	 * @param cmacOutput 1-by-{@value CmacOutput#NUMBER_OF_WEIGHTS} matrix
	 * @return 1-by-{@value CmacOutput#NUMBER_OF_WEIGHTS} matrix
	 */
	public SimpleMatrix cmacOutput2MotorSpeeds(SimpleMatrix cmacOutput) {
		SimpleMatrix motorSpeeds = CMAC_OUTPUT_TO_MOTOR_SPEED_MATRIX.mult(cmacOutput.transpose()).transpose();
		//motorSpeeds is also a 1-by-{@value CmacOutput#NUMBER_OF_WEIGHTS} matrix
		//for each item in the motorSpeeds matrix, its square root needs to be taken
		for (int i = 0; i < motorSpeeds.getNumElements(); i++) {
			double motorSpeed = motorSpeeds.get(i);
			motorSpeeds.set(i, Math.sqrt(motorSpeed));
		}
		return motorSpeeds;
	}
}
