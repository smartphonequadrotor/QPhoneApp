package com.ventus.smartphonequadrotor.qphoneapp.util.control;

import java.util.Calendar;

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
	 * A row matrix containing the last recorded height, roll, pitch and yaw values.
	 */
	private double[] historicHrpy;
	/**
	 * A 2-D matrix containing the last 3 recorded desired height, desired roll, desired pitch and desired yaw values.
	 * This is supposed to be a 3-row and 4-column matrix.
	 */
	private double[][] desiredHrpyHistory;
	/**
	 * The timestamps from the last 3 desired value updates.
	 */
	private long[] desiredHrpyTimestamps;
	/**
	 * The sum of the absolute motor speeds from the previous update.
	 * Index 0 contains the timestamp from the last update and index 1 contains the timestamp from 
	 * the update before that.
	 */
	private double netPreviousRotorSpeed;
	/**
	 * The timestamp of the last update.
	 */
	private long previousUpdateTimestamp;
	
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
		this.historicHrpy = new double[4];
		this.desiredHrpyHistory = new double[][]{
			{0, 0, 0, 0}, 
			{0, 0, 0, 0}, 
			{0, 0, 0, 0}
		};
		this.netPreviousRotorSpeed = 0;
		this.previousUpdateTimestamp = -1L;
		this.desiredHrpyTimestamps = new long[] {2, 1, 0};
	}
	
	public void setNetPreviousRotorSpeed(double netPreviousRotorSpeed) {
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
	public void processMoveCommand(MoveCommand[] moveCommands) {
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
			timeshiftDesiredVariables(desiredHeight, desiredRoll, desiredPitch, 0);
		}
	}

	/**
	 * This method uses the old values of the height, roll, pitch, yaw and their corresponding
	 * desired values in conjunction with the current values of height, roll, pitch and yaw to calculate
	 * the error matrix for the {@link ControlLoop#triggerCmacUpdate(SimpleMatrix)} method.
	 * @param timestamp The timestamp when the sensor update was received.
	 * @param height The current height of the quadrotor
	 * @param roll The current roll angle of the quadrotor
	 * @param pitch The current pitch angle of the quadrotor
	 * @param yaw The current yaw angle of the quadrotor
	 * @return The error matrix
	 */
	public SimpleMatrix calculateErrors(long timestamp, int height, double roll, double pitch, double yaw) {
		SimpleMatrix errorMatrix = null;
		if (previousUpdateTimestamp != -1) {
			double lastTimestampInterval = timestamp - previousUpdateTimestamp;
			double lastDesiredTimestampInterval = desiredHrpyTimestamps[LAST_UPDATE_INDEX] - desiredHrpyTimestamps[LAST_UPDATE_INDEX+1];
			double previousToLastDesiredTimestampInterval = desiredHrpyTimestamps[LAST_UPDATE_INDEX+1] 
					- desiredHrpyTimestamps[LAST_UPDATE_INDEX+2];
			//if this is not the first reading then,
			double[] errors = new double[CmacInputParam.count];
			errors[CmacInputParam.HEIGHT_ERROR.index] = height - desiredHrpyHistory[LAST_UPDATE_INDEX][HEIGHT_INDEX];
			errors[CmacInputParam.ROLL_ERROR.index] = roll - desiredHrpyHistory[LAST_UPDATE_INDEX][ROLL_INDEX];
			errors[CmacInputParam.PITCH_ERROR.index] = pitch - desiredHrpyHistory[LAST_UPDATE_INDEX][PITCH_INDEX];
			errors[CmacInputParam.YAW_ERROR.index] = yaw - desiredHrpyHistory[LAST_UPDATE_INDEX][YAW_INDEX];
			errors[CmacInputParam.HEIGHT_ERROR_DERIVATIVE.index] = (height - historicHrpy[HEIGHT_INDEX]
					+ desiredHrpyHistory[LAST_UPDATE_INDEX][HEIGHT_INDEX] - desiredHrpyHistory[LAST_UPDATE_INDEX+1][HEIGHT_INDEX])
					/ lastTimestampInterval;
			errors[CmacInputParam.ROLL_ERROR_DERIVATIVE.index] = (roll - historicHrpy[ROLL_INDEX]
					+ desiredHrpyHistory[LAST_UPDATE_INDEX][ROLL_INDEX] - desiredHrpyHistory[LAST_UPDATE_INDEX+1][ROLL_INDEX])
					/ lastTimestampInterval;
			errors[CmacInputParam.PITCH_ERROR_DERIVATIVE.index] = (pitch - historicHrpy[PITCH_INDEX]
					+ desiredHrpyHistory[LAST_UPDATE_INDEX][PITCH_INDEX] - desiredHrpyHistory[LAST_UPDATE_INDEX+1][PITCH_INDEX])
					/ lastTimestampInterval;
			errors[CmacInputParam.YAW_ERROR_DERIVATIVE.index] = (yaw - historicHrpy[YAW_INDEX]
					+ desiredHrpyHistory[LAST_UPDATE_INDEX][YAW_INDEX] - desiredHrpyHistory[LAST_UPDATE_INDEX+1][YAW_INDEX])
					/ lastTimestampInterval;
			errors[CmacInputParam.DESIRED_ROLL_DERIVATIVE.index] = (desiredHrpyHistory[LAST_UPDATE_INDEX][ROLL_INDEX] 
					- desiredHrpyHistory[LAST_UPDATE_INDEX+1][ROLL_INDEX]) / lastDesiredTimestampInterval;
			errors[CmacInputParam.DESIRED_PITCH_DERIVATIVE.index] = (desiredHrpyHistory[LAST_UPDATE_INDEX][PITCH_INDEX] 
					- desiredHrpyHistory[LAST_UPDATE_INDEX+1][PITCH_INDEX]) / lastDesiredTimestampInterval;
			errors[CmacInputParam.DESIRED_ROLL_SECOND_DERIVATIVE.index] = (desiredHrpyHistory[LAST_UPDATE_INDEX][ROLL_INDEX]
					- (2*desiredHrpyHistory[LAST_UPDATE_INDEX+1][ROLL_INDEX]) + desiredHrpyHistory[LAST_UPDATE_INDEX+2][ROLL_INDEX])
					/ (lastDesiredTimestampInterval * previousToLastDesiredTimestampInterval);
			errors[CmacInputParam.DESIRED_PITCH_SECOND_DERIVATIVE.index] = (desiredHrpyHistory[LAST_UPDATE_INDEX][PITCH_INDEX]
					- (2*desiredHrpyHistory[LAST_UPDATE_INDEX+1][PITCH_INDEX]) + desiredHrpyHistory[LAST_UPDATE_INDEX+2][PITCH_INDEX])
					/ (lastDesiredTimestampInterval * previousToLastDesiredTimestampInterval);
			errors[CmacInputParam.NET_PREVIOUS_ROTOR_SPEED.index] = netPreviousRotorSpeed;
			errorMatrix = new SimpleMatrix(1, CmacInputParam.count, true, errors);
		}
		timeshiftCurrentVariables(timestamp, height, roll, pitch, yaw);
		return errorMatrix;
	}
	
	/**
	 * This method moves the values in the state variables from the LAST_UPDATE_INDEX
	 * position to the PREVIOUS_TO_LAST_UPDATE_INDEX position and updates the values at the
	 * LAST_UPDATE_INDEX.
	 * @param height
	 * @param roll
	 * @param pitch
	 * @param yaw
	 */
	private void timeshiftCurrentVariables(long timestamp, int height, double roll, double pitch, double yaw) {
		previousUpdateTimestamp = timestamp;
		historicHrpy[HEIGHT_INDEX] = height;
		historicHrpy[ROLL_INDEX] = roll;
		historicHrpy[PITCH_INDEX] = pitch;
		historicHrpy[YAW_INDEX] = yaw;
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
	private void timeshiftDesiredVariables(double height, double roll, double pitch, double yaw) {
		Calendar cal = Calendar.getInstance();
		long timestamp = cal.getTimeInMillis();
		desiredHrpyTimestamps[LAST_UPDATE_INDEX+2] = desiredHrpyTimestamps[LAST_UPDATE_INDEX+1];
		desiredHrpyTimestamps[LAST_UPDATE_INDEX+1] = desiredHrpyTimestamps[LAST_UPDATE_INDEX];
		desiredHrpyTimestamps[LAST_UPDATE_INDEX] = timestamp;
		
		for (int i = HEIGHT_INDEX; i < YAW_INDEX; i++) {
			desiredHrpyHistory[LAST_UPDATE_INDEX+2][i] = desiredHrpyHistory[LAST_UPDATE_INDEX+1][i];
			desiredHrpyHistory[LAST_UPDATE_INDEX+1][i] = desiredHrpyHistory[LAST_UPDATE_INDEX][i];
		}
		desiredHrpyHistory[LAST_UPDATE_INDEX][HEIGHT_INDEX] = height;
		desiredHrpyHistory[LAST_UPDATE_INDEX][ROLL_INDEX] = roll;
		desiredHrpyHistory[LAST_UPDATE_INDEX][PITCH_INDEX] = pitch;
		desiredHrpyHistory[LAST_UPDATE_INDEX][YAW_INDEX] = yaw;
	}
}
