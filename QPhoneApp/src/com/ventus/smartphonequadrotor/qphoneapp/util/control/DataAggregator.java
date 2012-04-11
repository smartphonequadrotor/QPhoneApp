package com.ventus.smartphonequadrotor.qphoneapp.util.control;

import java.util.Calendar;

import android.util.Log;

import com.ventus.smartphonequadrotor.qphoneapp.util.KeyValuePair;
import com.ventus.smartphonequadrotor.qphoneapp.util.SimpleMatrix;
import com.ventus.smartphonequadrotor.qphoneapp.util.json.MoveCommand;
import com.ventus.smartphonequadrotor.qphoneapp.util.net.NetworkCommunicationManager;

/**
 * This is the class that gets all the data from the controller and the
 * QCB and aggregates it so that it could be used by the control systems.
 * This class will mostly be operated by the controlLoop thread.
 * @author abhin
 */
public class DataAggregator {
	public static final String TAG = DataAggregator.class.getName();
	
	/*
	 * variables that will be used to generate the error matrix for the control loop
	 */
	
	/**
	 * A row matrix containing the current known height, roll, pitch and yaw values
	 * along with their respective timestamps
	 */
	private KeyValuePair<Long, Double>[] acquiredHrpyHistory;
	/**
	 * This is the set of errors for height, roll, pitch and yaw from the last calculation of errors.
	 */
	private KeyValuePair<Long, Double[]> previousHrpyErrors;
	/**
	 * A 2-D matrix containing the current and last 2 recorded desired height, desired roll, desired pitch 
	 * and desired yaw values along with their respective timestamps
	 * This is supposed to be a 3-row and 4-column matrix.
	 */
	private KeyValuePair<Long, Double[]>[] desiredHrpyHistory;
	/**
	 * The sum of the absolute motor speeds from the previous update.
	 * Index 0 contains the timestamp from the last update and index 1 contains the timestamp from 
	 * the update before that.
	 */
	private double netPreviousRotorSpeed;
	
	private static final int DESIRED_DATA_HISTORY_LENGTH = 3;
	private static final int ACQUIRED_DATA_HISTORY_LENGTH = 2;
	private static final int LAST_UPDATE_INDEX = 0;
	private static final int HEIGHT_INDEX = 0;
	private static final int ROLL_INDEX = 1;
	private static final int PITCH_INDEX = 2;
	private static final int YAW_INDEX = 3;

	private static final int MAX_SPEED = 100;
	private static final double MAX_TILT = Math.PI / 4;
	/**
	 * This is the gain that is multiplied to the product of the x component of the
	 * desired direction vector and the minimum of abs(speed) and MAX_SPEED.
	 */
	private static final double TILT_GAIN = MAX_TILT / MAX_SPEED;
	/**
	 * This is the gain that is multiplied to the z component of the desired direction
	 * vector.
	 */
	private static final double HEIGHT_GAIN = 1;
	
	public DataAggregator() {
		this.acquiredHrpyHistory = new KeyValuePair[4];
		this.previousHrpyErrors = new KeyValuePair<Long, Double[]>(0L, new Double[]{0d, 0d, 0d, 0d});
		this.desiredHrpyHistory = new KeyValuePair[DESIRED_DATA_HISTORY_LENGTH];
		this.netPreviousRotorSpeed = 0;
		
		//now we initialize the timestamps of the desired values to something large
		//enough that it doesn't cause errors (from being zero) but small enough that 
		//its impact on the math is insignificant
		for (int i = 0; i < DESIRED_DATA_HISTORY_LENGTH; i++) {
			this.desiredHrpyHistory[i] = new KeyValuePair<Long, Double[]>(
				(long) (DESIRED_DATA_HISTORY_LENGTH - i),
				new Double[]{0d, 0d, 0d, 0d}
			);
		}
		
		for (int i = 0; i < 4; i++) {
			this.acquiredHrpyHistory[i] = new KeyValuePair<Long, Double>(0L, 0D);
		}
	}
	
	public synchronized void setNetPreviousRotorSpeed(double netPreviousRotorSpeed) {
		this.netPreviousRotorSpeed = netPreviousRotorSpeed;
	}
	
	/**
	 * This method receives envelopes immediately after the {@link NetworkCommunicationManager}
	 * gets a message from the controller. The messages have to be processed and the correct
	 * actions have to be taken.
	 * 
	 * Note:
	 * <ol> 
	 * 	<li>
	 * 		This method will some day be able to process paths. But currently it just takes the 
	 * 		first move command it receives and uses it to decide the desired roll, pitch and height.
	 * 	</li>
	 * 	<li>
	 * 		The move command consists of an <x,y,z> vector, speed and duration. The <x,y> vector is
	 * 		used purely for the direction. In fact, the numbers in this vector will get normalized before
	 * 		being used. The rotation in a directions x and y will be calculated using:
	 * 			R_x = TILT_GAIN*x*min(abs(speed), MAX_SPEED)
	 * 			R_y = TILT_GAIN*y*min(abs(speed), MAX_SPEED)
	 * 		where MAX_SPEED is the maximum allowed speed and TILT_GAIN is a constant.
	 * 		These calculated angles will be kept as the desired angles of the system for a duration specified
	 * 		by the duration variable in the move command.
	 * 
	 * 		The z component of the move command will be used to set the desired height for the given duration.
	 * 	</li>
	 * </ol>
	 * @param MoveCommands[]
	 */
	public synchronized void processMoveCommand(MoveCommand[] moveCommands) {
		if (moveCommands != null && moveCommands.length != 0) {
			//use the first move command and ignore the rest for now
			MoveCommand cmd = moveCommands[0].normalizeDirection();
			int speed = Math.min(Math.abs(cmd.getSpeed()), MAX_SPEED);
			double desiredPitch = TILT_GAIN * cmd.getXVector() * speed;
			double desiredRoll = TILT_GAIN * cmd.getYVector() * speed;
			//ensure that the desired roll and pitch are in the range of [-MAX_TILT, MAX_TILT]
			desiredPitch = Math.max(-MAX_TILT, Math.min(MAX_TILT, desiredPitch));
			desiredRoll = Math.max(-MAX_TILT, Math.min(MAX_TILT, desiredRoll));
			double desiredHeight = HEIGHT_GAIN * speed * moveCommands[0].getZVector();
			registerDesiredVariables(desiredHeight, desiredRoll, desiredPitch, 0);
			
		}
	}
	
	/**
	 * This method stores the newly acquired kinematics values so that they may be used by the control
	 * loop in its next iteration.
	 */
	public synchronized void processNewKinematicsData(long timestamp, float roll, float pitch, float yaw) {
		registerCurrentRpyData(timestamp, roll, pitch, yaw);
	}
	
	/**
	 * This method stores the newly acquired height data so that they may be used by the control loop in
	 * its next iteration.
	 * @param timestamp
	 * @param height
	 */
	public synchronized void processNewHeightData(long timestamp, int height) {
		registerCurrentHeightDatum(timestamp, height);
	}
	
	/**
	 * This method sets the previousHrpyErrors in the data aggregator using the current errors
	 * in the given matrix
	 * @param currentErrors The simpleMatrix instance returned by {@link DataAggregator#calculateErrors()}.
	 */
	public synchronized void updatePreviousHrpyErrors(SimpleMatrix currentErrors) {
		if (currentErrors == null)
			throw new IllegalArgumentException("error matrix is null");
		if (currentErrors.getNumElements() != CmacInputParam.count)
			throw new IllegalArgumentException("error matrix has incorrect size");
		
		previousHrpyErrors.value[HEIGHT_INDEX] = currentErrors.get(CmacInputParam.HEIGHT_ERROR.index);
		previousHrpyErrors.value[ROLL_INDEX] = currentErrors.get(CmacInputParam.ROLL_ERROR.index);
		previousHrpyErrors.value[PITCH_INDEX] = currentErrors.get(CmacInputParam.PITCH_ERROR.index);
		previousHrpyErrors.value[YAW_INDEX] = currentErrors.get(CmacInputParam.YAW_ERROR.index);
	}
	
	/**
	 * This method must be called by the {@link ControlLoop} periodically to calculate the motor speeds.
	 */
	public synchronized SimpleMatrix calculateErrors() {
		SimpleMatrix errorMatrix = null;
		try {
			if (enoughAcquiredHrpyHistoryExists()) {
				//if this is not the first reading then,
				double[] errors = new double[CmacInputParam.count];
				updateHrpyErrors(errors);
				updateHrpyErrorDerivatives(errors);
				updateDesiredDerivatives(errors);
				
				errors[CmacInputParam.NET_PREVIOUS_ROTOR_SPEED.index] = netPreviousRotorSpeed;
				errorMatrix = new SimpleMatrix(1, CmacInputParam.count, true, errors);
			}
		} catch (Exception ex) {
			Log.e(TAG, "Error in calculating errors", ex);
		}
		return errorMatrix;
	}
	
	/**
	 * Checks if more data needs to be acquired before the Cmac computations can take place.
	 * @return True if all the timestamps in acquiredHrpyHistory are not zero and false otherwise.
	 */
	private boolean enoughAcquiredHrpyHistoryExists() {
		for (int j = 0; j <= YAW_INDEX; j++) {
			if (acquiredHrpyHistory[j].key == 0)
				return false;
		}
		
		return true;
	}
	
	/**
	 * This method computes the current HRPY errors.
	 * @param errors
	 */
	private void updateHrpyErrors(double[] errors) {
		errors[CmacInputParam.HEIGHT_ERROR.index] = acquiredHrpyHistory[HEIGHT_INDEX].value 
				- desiredHrpyHistory[LAST_UPDATE_INDEX].value[HEIGHT_INDEX];
		errors[CmacInputParam.ROLL_ERROR.index] = acquiredHrpyHistory[ROLL_INDEX].value
				- desiredHrpyHistory[LAST_UPDATE_INDEX].value[ROLL_INDEX];
		errors[CmacInputParam.PITCH_ERROR.index] = acquiredHrpyHistory[PITCH_INDEX].value
				- desiredHrpyHistory[LAST_UPDATE_INDEX].value[PITCH_INDEX];
		errors[CmacInputParam.YAW_ERROR.index] = acquiredHrpyHistory[YAW_INDEX].value
				- desiredHrpyHistory[LAST_UPDATE_INDEX].value[YAW_INDEX];
	}
	
	/**
	 * This method uses the currently calculated HRPY errors along with the HRPY errors computed in the 
	 * last iteration to compute the error derivatives for HRPY.
	 * @param errors
	 */
	private void updateHrpyErrorDerivatives(double[] errors) {
		Calendar cal = Calendar.getInstance();
		long deltaT = cal.getTimeInMillis();
		errors[CmacInputParam.HEIGHT_ERROR_DERIVATIVE.index] = (errors[CmacInputParam.HEIGHT_ERROR.index] 
				- previousHrpyErrors.value[HEIGHT_INDEX]) / deltaT;
		errors[CmacInputParam.ROLL_ERROR_DERIVATIVE.index] = (errors[CmacInputParam.ROLL_ERROR.index] 
				- previousHrpyErrors.value[ROLL_INDEX]) / deltaT;
		errors[CmacInputParam.PITCH_ERROR_DERIVATIVE.index] = (errors[CmacInputParam.PITCH_ERROR.index] 
				- previousHrpyErrors.value[PITCH_INDEX]) / deltaT;
		errors[CmacInputParam.YAW_ERROR_DERIVATIVE.index] = (errors[CmacInputParam.YAW_ERROR.index] 
				- previousHrpyErrors.value[YAW_INDEX]) / deltaT;
	}
	
	/**
	 * This method computes the first and second order derivatives of the desired variables. 
	 * @param errors
	 */
	private void updateDesiredDerivatives(double[] errors) {
		errors[CmacInputParam.DESIRED_ROLL_DERIVATIVE.index] = (desiredHrpyHistory[LAST_UPDATE_INDEX].value[ROLL_INDEX] 
				- desiredHrpyHistory[LAST_UPDATE_INDEX+1].value[ROLL_INDEX]) 
				/ (desiredHrpyHistory[LAST_UPDATE_INDEX].key 
				- desiredHrpyHistory[LAST_UPDATE_INDEX+1].key);
		errors[CmacInputParam.DESIRED_PITCH_DERIVATIVE.index] = (desiredHrpyHistory[LAST_UPDATE_INDEX].value[PITCH_INDEX] 
				- desiredHrpyHistory[LAST_UPDATE_INDEX+1].value[PITCH_INDEX]) 
				/ (desiredHrpyHistory[LAST_UPDATE_INDEX].key 
				- desiredHrpyHistory[LAST_UPDATE_INDEX+1].key);
		/*
		 * For the three time, value pairs (y0, t0), (y1, t1), (y2, t2), the correct second order derivative is:
		 * 
		 *  2[(y0-y1)(t1-t2) - (y1-y2)(t0-t1)]
		 * ------------------------------------
		 *        (t0-t1)(t1-t2)(t0-t2)
		 * 
		 * But this is too computationally intensive and we approximate it using (t1-t2)==(t0-t1)==(t0-t2)/2
		 * to get the approximation
		 * 
		 *  y0 - 2*y1 + y2
		 * ----------------
		 *  (t0-t1)(t1-t2)
		 *  
		 * which is much more computationally efficient and a good enough approximation.
		 */
		errors[CmacInputParam.DESIRED_ROLL_SECOND_DERIVATIVE.index] = (desiredHrpyHistory[LAST_UPDATE_INDEX].value[ROLL_INDEX]
				- (2*desiredHrpyHistory[LAST_UPDATE_INDEX+1].value[ROLL_INDEX]) + desiredHrpyHistory[LAST_UPDATE_INDEX+2].value[ROLL_INDEX]) 
				/ ((desiredHrpyHistory[LAST_UPDATE_INDEX].key - desiredHrpyHistory[LAST_UPDATE_INDEX+1].key) 
				* (desiredHrpyHistory[LAST_UPDATE_INDEX+1].key - desiredHrpyHistory[LAST_UPDATE_INDEX+2].key));
		errors[CmacInputParam.DESIRED_PITCH_SECOND_DERIVATIVE.index] = (desiredHrpyHistory[LAST_UPDATE_INDEX].value[PITCH_INDEX]
				- (2*desiredHrpyHistory[LAST_UPDATE_INDEX+1].value[PITCH_INDEX]) + desiredHrpyHistory[LAST_UPDATE_INDEX+2].value[PITCH_INDEX]) 
				/ ((desiredHrpyHistory[LAST_UPDATE_INDEX].key - desiredHrpyHistory[LAST_UPDATE_INDEX+1].key) 
				* (desiredHrpyHistory[LAST_UPDATE_INDEX+1].key - desiredHrpyHistory[LAST_UPDATE_INDEX+2].key));
	}
	
	/**
	 * This method registers the currently recorded RPY values.
	 * @param height
	 * @param roll
	 * @param pitch
	 * @param yaw
	 */
	private void registerCurrentRpyData(long timestamp, double roll, double pitch, double yaw) {
		acquiredHrpyHistory[ROLL_INDEX].key = 
				acquiredHrpyHistory[PITCH_INDEX].key = 
				acquiredHrpyHistory[YAW_INDEX].key = timestamp;
		
		acquiredHrpyHistory[ROLL_INDEX].value = roll;
		acquiredHrpyHistory[PITCH_INDEX].value = pitch;
		acquiredHrpyHistory[YAW_INDEX].value = yaw;
	}
	
	/**
	 * This method registers the currently recorded height value.
	 * @param timestamp
	 * @param height
	 */
	private void registerCurrentHeightDatum(long timestamp, int height) {
		acquiredHrpyHistory[HEIGHT_INDEX].key = timestamp;
		acquiredHrpyHistory[HEIGHT_INDEX].value = (double) height;
	}
	
	/**
	 * This method moves the values in the desired variables from the LAST_UPDATE_INDEX
	 * position to the PREVIOUS_TO_LAST_UPDATE_INDEX position and updates the values at the
	 * LAST_UPDATE_INDEX.
	 * @param height
	 * @param roll
	 * @param pitch
	 * @param yaw
	 */
	private void registerDesiredVariables(double height, double roll, double pitch, double yaw) {
		long timestamp = Calendar.getInstance().getTimeInMillis();
		for (int i = DESIRED_DATA_HISTORY_LENGTH; i > 1; i--) {
			desiredHrpyHistory[i-1].key = desiredHrpyHistory[i-2].key;
			desiredHrpyHistory[i-1].value = desiredHrpyHistory[i-2].value;
		}
		desiredHrpyHistory[LAST_UPDATE_INDEX].key = timestamp;
		desiredHrpyHistory[LAST_UPDATE_INDEX].value = new Double[] {height, roll, pitch, yaw};
	}
}
