package com.ventus.smartphonequadrotor.qphoneapp.util.control;

import java.io.IOException;

import org.jivesoftware.smack.packet.PacketExtension;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.ventus.smartphonequadrotor.qphoneapp.services.MainService;
import com.ventus.smartphonequadrotor.qphoneapp.util.SimpleMatrix;
import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpCallback;
import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpCommands;
import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpCommunication;
import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpHandlers;
import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpParser;
import com.ventus.smartphonequadrotor.qphoneapp.util.json.MoveCommand;
import com.ventus.smartphonequadrotor.qphoneapp.util.net.NetworkCommunicationManager;

/**
 * This is the class that gets all the data from the controller and the
 * QCB and aggregates it so that it could be used by the control systems.
 * @author abhin
 */
public class DataAggregator {
	public static final String TAG = DataAggregator.class.getName();
	
	private MainService owner;
	private QcfpHandlers packetHandlers;
	private QcfpParser bluetoothDataParser;
	
	/*
	 * variables that will be used to generate the error matrix for the control loop
	 */
	
	/**
	 * A row matrix containing the last recorded height, roll, pitch and yaw values.
	 */
	private double[] historicHrpy;
	/**
	 * A 2-D matrix containing the last 3 recorded height, roll, pitch and yaw values.
	 * This is supposed to be a 3-row and 4-column matrix.
	 */
	private double[][] desiredHrpyHistory;
	/**
	 * The sum of the absolute motor speeds from the previous update.
	 */
	private double netPreviousRotorSpeed;
	/**
	 * The timestamps from the last 2 updates. 
	 * Index 0 contains the timestamp from the last update and index 1 contains the timestamp from 
	 * the update before that.
	 */
	private long[] historicTimestamps;
	
	private static final int CURRENT_INDEX = 0;
	private static final int LAST_UPDATE_INDEX = 1;
	private static final int PREVIOUS_TO_LAST_UPDATE_INDEX = 2;
	private static final int HEIGHT_INDEX = 0;
	private static final int ROLL_INDEX = 1;
	private static final int PITCH_INDEX = 2;
	private static final int YAW_INDEX = 3;
	
	public DataAggregator(MainService owner) {
		this.owner = owner;
		this.historicHrpy = new double[4];
		this.desiredHrpyHistory = new double[][]{
			{0, 0, 0, 0}, 
			{0, 0, 0, 0}, 
			{0, 0, 0, 0}
		};
		this.netPreviousRotorSpeed = 0;
		this.historicTimestamps = new long[]{-1, -1};
		this.packetHandlers = new QcfpHandlers();
		bluetoothDataParser =  new QcfpParser(QcfpParser.QCFP_MAX_PACKET_SIZE, packetHandlers);
		packetHandlers.registerHandler(QcfpCommands.QCFP_ASYNC_DATA, asyncDataCallback);
		packetHandlers.registerHandler(QcfpCommands.QCFP_FLIGHT_MODE, flightModeCallback);
		packetHandlers.registerHandler(QcfpCommands.QCFP_CALIBRATE_QUADROTOR, calibrationStatusCallback);
	}
	
	/**
	 * This method receives envelopes immediately after the {@link NetworkCommunicationManager}
	 * gets a message from the controller. The messages have to be processed and the correct
	 * actions have to be taken. This method is called as part of an event listener, so operations
	 * involving heavy lifting are not recommended without using external threads.
	 * @param MoveCommands[]
	 */
	public void processMoveCommand(MoveCommand[] moveCommands) {
		if (moveCommands.length != 0) {
			//the controller just sent a series of move commands
			for (int i = 0; i < moveCommands.length; i++) {
				SimpleMatrix deltaDisplacement = new SimpleMatrix(3, 1, true, new double[]{
					moveCommands[i].getXVector(),
					moveCommands[i].getYVector(),
					moveCommands[i].getZVector()
				});
				//normalize deltaDisplacement to make it a direction vector
				deltaDisplacement = deltaDisplacement.divide(deltaDisplacement.normF());
				//multiply the direction vector with the speed and time to get the actual
				//delta displacement
				deltaDisplacement = deltaDisplacement.mult(
					moveCommands[i].getSpeed()*moveCommands[i].getDuration()
				);
				//TODO: use the speed and time in the move command to actually affect the speed of the quadrotor
				//In the future, a more complicated outer control-loop could be build that takes
				//into account the speed and time while deciding the angles for the quadrotor.
			}
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
	 */
	private void calculateErrorAndUpdateCmac(long timestamp, int height, float roll, float pitch, float yaw) {
		if (historicTimestamps[CURRENT_INDEX] != -1 && historicTimestamps[LAST_UPDATE_INDEX] != -1) {
			double lastTimestampInterval = timestamp - historicTimestamps[LAST_UPDATE_INDEX];
			double previousToLastTimestampInterval = historicTimestamps[CURRENT_INDEX] - historicTimestamps[LAST_UPDATE_INDEX];
			//if this is not the first reading then,
			double[] errors = new double[CmacInputParam.count];
			errors[CmacInputParam.HEIGHT_ERROR.index] = height - desiredHrpyHistory[CURRENT_INDEX][HEIGHT_INDEX];
			errors[CmacInputParam.ROLL_ERROR.index] = roll - desiredHrpyHistory[CURRENT_INDEX][ROLL_INDEX];
			errors[CmacInputParam.PITCH_ERROR.index] = pitch - desiredHrpyHistory[CURRENT_INDEX][PITCH_INDEX];
			errors[CmacInputParam.YAW_ERROR.index] = yaw - desiredHrpyHistory[CURRENT_INDEX][YAW_INDEX];
			errors[CmacInputParam.HEIGHT_ERROR_DERIVATIVE.index] = (yaw - historicHrpy[HEIGHT_INDEX]
					+ desiredHrpyHistory[CURRENT_INDEX][HEIGHT_INDEX] - desiredHrpyHistory[LAST_UPDATE_INDEX][HEIGHT_INDEX])
					/ lastTimestampInterval;
			errors[CmacInputParam.ROLL_ERROR_DERIVATIVE.index] = (yaw - historicHrpy[ROLL_INDEX]
					+ desiredHrpyHistory[CURRENT_INDEX][ROLL_INDEX] - desiredHrpyHistory[LAST_UPDATE_INDEX][ROLL_INDEX])
					/ lastTimestampInterval;
			errors[CmacInputParam.PITCH_ERROR_DERIVATIVE.index] = (yaw - historicHrpy[PITCH_INDEX]
					+ desiredHrpyHistory[CURRENT_INDEX][PITCH_INDEX] - desiredHrpyHistory[LAST_UPDATE_INDEX][PITCH_INDEX])
					/ lastTimestampInterval;
			errors[CmacInputParam.YAW_ERROR_DERIVATIVE.index] = (yaw - historicHrpy[YAW_INDEX]
					+ desiredHrpyHistory[CURRENT_INDEX][YAW_INDEX] - desiredHrpyHistory[LAST_UPDATE_INDEX][YAW_INDEX])
					/ lastTimestampInterval;
			errors[CmacInputParam.DESIRED_ROLL_DERIVATIVE.index] = (desiredHrpyHistory[CURRENT_INDEX][ROLL_INDEX] 
					- desiredHrpyHistory[LAST_UPDATE_INDEX][ROLL_INDEX]) / lastTimestampInterval;
			errors[CmacInputParam.DESIRED_PITCH_DERIVATIVE.index] = (desiredHrpyHistory[CURRENT_INDEX][PITCH_INDEX] 
					- desiredHrpyHistory[LAST_UPDATE_INDEX][PITCH_INDEX]) / lastTimestampInterval;
			errors[CmacInputParam.DESIRED_ROLL_SECOND_DERIVATIVE.index] = (desiredHrpyHistory[CURRENT_INDEX][ROLL_INDEX]
					- (2*desiredHrpyHistory[LAST_UPDATE_INDEX][ROLL_INDEX]) + desiredHrpyHistory[PREVIOUS_TO_LAST_UPDATE_INDEX][ROLL_INDEX])
					/ (lastTimestampInterval * previousToLastTimestampInterval);
			errors[CmacInputParam.DESIRED_PITCH_SECOND_DERIVATIVE.index] = (desiredHrpyHistory[CURRENT_INDEX][PITCH_INDEX]
					- (2*desiredHrpyHistory[LAST_UPDATE_INDEX][PITCH_INDEX]) + desiredHrpyHistory[PREVIOUS_TO_LAST_UPDATE_INDEX][PITCH_INDEX])
					/ (lastTimestampInterval * previousToLastTimestampInterval);
			errors[CmacInputParam.NET_PREVIOUS_ROTOR_SPEED.index] = netPreviousRotorSpeed;
			Message msg = owner.getControlLoop().handler.obtainMessage(ControlLoop.CMAC_UPDATE_MESSAGE);
			msg.obj = new SimpleMatrix(1, CmacInputParam.count, true, errors);
			//if the handler already contains such messages, then write over these messages. This lets the 
			//QCB not overwhelm the phone with messages.
			if (owner.getControlLoop().handler.hasMessages(ControlLoop.CMAC_UPDATE_MESSAGE)) {
				owner.getControlLoop().handler.removeMessages(ControlLoop.CMAC_UPDATE_MESSAGE);
			}
			owner.getControlLoop().handler.sendMessage(msg);
		}
		timeshiftCurrentVariables(timestamp, height, roll, pitch, yaw);
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
	private void timeshiftCurrentVariables(long timestamp, int height, float roll, float pitch, float yaw) {
		historicTimestamps[LAST_UPDATE_INDEX] = historicTimestamps[CURRENT_INDEX];
		historicTimestamps[CURRENT_INDEX] = timestamp;
		
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
	private void timeshiftDesiredVariables(int height, float roll, float pitch, float yaw) {
		for (int i = HEIGHT_INDEX; i < YAW_INDEX; i++) {
			desiredHrpyHistory[PREVIOUS_TO_LAST_UPDATE_INDEX][i] = desiredHrpyHistory[LAST_UPDATE_INDEX][i];
			desiredHrpyHistory[LAST_UPDATE_INDEX][i] = desiredHrpyHistory[CURRENT_INDEX][i];
		}
		desiredHrpyHistory[CURRENT_INDEX][HEIGHT_INDEX] = height;
		desiredHrpyHistory[CURRENT_INDEX][ROLL_INDEX] = roll;
		desiredHrpyHistory[CURRENT_INDEX][PITCH_INDEX] = pitch;
		desiredHrpyHistory[CURRENT_INDEX][YAW_INDEX] = yaw;
	}
	
	/**
	 * The bluetooth reader thread will use this handler to send messages to the service
	 * thread. These messages will contain the object parsed from the bluetooth strings.
	 */
	public Handler bluetoothMessageHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int length = msg.arg1;
			if (length > 0) {
				bluetoothDataParser.addData((byte[])msg.obj, length);
			}
		}
	};
	
	/**
	 * This callback receives data from the bluetooth. The data is in the form of a 
	 * byte array and needs to be parsed according to the QCFB protocol guide in the
	 * project documents folder on google docs.
	 * The data contains information regarding whether the QCB has entered flightmode
	 * (armed state) or not.
	 */
	private QcfpCallback flightModeCallback = new QcfpCallback() {
		private static final int CMD41_ENABLE_INDEX = 1;
		
		@Override
		public void run(byte[] packet, int length) {
			//check if the length of the command is appropriate. 
			if (length == 2) {
				int flightMode = packet[CMD41_ENABLE_INDEX];
				owner.flightModeReceivedfromQcb(flightMode);
			}
		}
	};
	
	/**
	 * This callback receives data from the bluetooth. The data is in the form of a 
	 * byte array and needs to be parsed according to the QCFB protocol guide in the
	 * project documents folder on google docs.
	 * The data contains information regarding whether the QCB has entered calibration mode
	 * or not.
	 */
	private QcfpCallback calibrationStatusCallback = new QcfpCallback() {
		private static final int CMD40_ENABLE_INDEX = 1;
		
		@Override
		public void run(byte[] packet, int length) {
			//check if the length of the command is appropriate. 
			if (length == 2) {
				int calibrationStatus = packet[CMD40_ENABLE_INDEX];
				owner.calibrationStatusReceivedfromQcb(calibrationStatus);
			}
		}
	};
	
	/**
	 * This callback receives data from the bluetooth. The data is in the form of a 
	 * byte array and needs to be parsed according to the QCFB protocol guide in the
	 * project documents folder on google docs.
	 */
	private QcfpCallback asyncDataCallback = new QcfpCallback() {
		private static final int CMD10_DATA_SOURCE_INDEX = 1;
		private static final int DATA_SOURCE_ACCEL = 0x01;
		private static final int DATA_SOURCE_GYRO = 0x02;
		private static final int DATA_SOURCE_MAG = 0x03;
		private static final int DATA_SOURCE_KIN = 0x06;
		
		private static final int ACCEL_PAYLOAD_LENGTH = 12;
		private static final int GYRO_PAYLOAD_LENGTH = 12;
		private static final int MAG_PAYLOAD_LENGTH = 12;
		private static final int KIN_PAYLOAD_LENGTH = 18;
		
		private static final int TIMESTAMP_START_INDEX = 2;
		
		private static final int X_INDEX_LSB = 6;
		private static final int X_INDEX_MSB = 7;
		private static final int Y_INDEX_LSB = 8;
		private static final int Y_INDEX_MSB = 9;
		private static final int Z_INDEX_LSB = 10;
		private static final int Z_INDEX_MSB = 11;
		
		private static final int ROLL_START_INDEX = 6;
		private static final int PITCH_START_INDEX = 10;
		private static final int YAW_START_INDEX = 14;
		
		@Override
		public void run(byte[] packet, int length) {
			// Require command id, data source, 4 timestamp, and at least 1 payload
			if(length >= 7)
			{
				// values from tri axis sensors
				int x, y, z;
				
				// Timestamp is unsigned
				long timestamp =
						((packet[TIMESTAMP_START_INDEX]   <<  0) & 0x00000000FF) |
						((packet[TIMESTAMP_START_INDEX+1] <<  8) & 0x000000FF00) |
						((packet[TIMESTAMP_START_INDEX+2] << 16) & 0x0000FF0000) |
						((packet[TIMESTAMP_START_INDEX+3] << 24) & 0x00FF000000);
				
				// sensor data is signed
				switch(packet[CMD10_DATA_SOURCE_INDEX])
				{
				case DATA_SOURCE_ACCEL:
					if(length == ACCEL_PAYLOAD_LENGTH)
					{
						x = (packet[X_INDEX_LSB] & 0x00FF) | (packet[X_INDEX_MSB] << 8);
						y = (packet[Y_INDEX_LSB] & 0x00FF) | (packet[Y_INDEX_MSB] << 8);
						z = (packet[Z_INDEX_LSB] & 0x00FF) | (packet[Z_INDEX_MSB] << 8);
						//kinematicsEstimator.registerAccelValues(x, y, z, timestamp);
						Log.d(TAG, String.format("Accelerometer: X: %f Y: %f Z: %f", x, y, z));
					}
					break;
				case DATA_SOURCE_GYRO:
					if(length == GYRO_PAYLOAD_LENGTH)
					{
						x = (packet[X_INDEX_LSB] & 0x00FF) | (packet[X_INDEX_MSB] << 8);
						y = (packet[Y_INDEX_LSB] & 0x00FF) | (packet[Y_INDEX_MSB] << 8);
						z = (packet[Z_INDEX_LSB] & 0x00FF) | (packet[Z_INDEX_MSB] << 8);
						//kinematicsEstimator.registerGyroValues(x, y, z, timestamp);
						Log.d(TAG, String.format("Gyroscope: X: %f Y: %f Z: %f", x, y, z));
					}
					break;
				case DATA_SOURCE_MAG:
					if(length == MAG_PAYLOAD_LENGTH)
					{
						x = (packet[X_INDEX_LSB] & 0x00FF) | (packet[X_INDEX_MSB] << 8);
						y = (packet[Y_INDEX_LSB] & 0x00FF) | (packet[Y_INDEX_MSB] << 8);
						z = (packet[Z_INDEX_LSB] & 0x00FF) | (packet[Z_INDEX_MSB] << 8);
						//kinematicsEstimator.registerMagValues(x, y, z, timestamp);
						Log.d(TAG, String.format("Magnetometer: X: %f Y: %f Z: %f", x, y, z));
					}
					break;
				case DATA_SOURCE_KIN:
					if(length == KIN_PAYLOAD_LENGTH)
					{
						float yaw, pitch, roll;
						// Assuming roll, pitch, yaw corresponds to x, y, z and that that is
						// the order the values are sent in.
						// Kinematics angles are in radians.
						yaw = QcfpCommunication.decodeFloat(packet, ROLL_START_INDEX);
						pitch = QcfpCommunication.decodeFloat(packet, PITCH_START_INDEX);
						roll = QcfpCommunication.decodeFloat(packet, YAW_START_INDEX);
						
						//Log.d(TAG, String.format("Kinematics: X: %f Y: %f Z: %f", yaw*180/Math.PI, pitch*180/Math.PI, roll*180/Math.PI));
						calculateErrorAndUpdateCmac(timestamp, 0, roll, pitch, yaw);
						owner.getNetworkCommunicationManager().sendKinematicsData(timestamp, roll, pitch, yaw);
					}
					break;
				default:
					break;
				}
			}
		}
	};
}
