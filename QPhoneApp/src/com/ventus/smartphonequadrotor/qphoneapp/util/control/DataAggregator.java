package com.ventus.smartphonequadrotor.qphoneapp.util.control;

import java.io.IOException;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.ventus.smartphonequadrotor.qphoneapp.services.MainService;
import com.ventus.smartphonequadrotor.qphoneapp.util.SimpleMatrix;
import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpCallback;
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
	//private KinematicsEstimator kinematicsEstimator;
	
	/**
	 * This represents the current error in displacement. Thus, it is desiredDisplacement - currentDisplacement.
	 * When the controller sends a move command, its displacement is added to this. When the 
	 * bluetooth sends acceleration, it is used to calculate the actual deltaDisplacement of the 
	 * quadrotor. That value is subtracted from this desplacementError.
	 */
	private SimpleMatrix displacementError;
	
	public DataAggregator(MainService owner) {
		this.owner = owner;
		displacementError = SimpleMatrix.zeros(3, 1);
		this.packetHandlers = new QcfpHandlers();
		bluetoothDataParser =  new QcfpParser(QcfpParser.QCFP_MAX_PACKET_SIZE, packetHandlers);
		packetHandlers.registerHandler(QcfpHandlers.QCFP_ASYNC_DATA, asyncDataCallback);
		//kinematicsEstimator = new KinematicsEstimator();
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
	 * This method is called once the bluetooth connection is setup and the thread for 
	 * listening to messages can start.
	 */
	public void startListening() {
		owner.getBluetoothManager().bluetoothReader.start();
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
	 */
	private QcfpCallback asyncDataCallback = new QcfpCallback() {
		private static final int CMD10_DATA_SOURCE_INDEX = 1;
		private static final int DATA_SOURCE_ACCEL = 0x01;
		private static final int DATA_SOURCE_GYRO = 0x02;
		private static final int DATA_SOURCE_MAG = 0x03;
		
		private static final int ACCEL_PAYLOAD_LENGTH = 12;
		private static final int GYRO_PAYLOAD_LENGTH = 12;
		private static final int MAG_PAYLOAD_LENGTH = 12;
		
		private static final int TIMESTAMP_START_INDEX = 2;
		
		private static final int X_INDEX_LSB = 6;
		private static final int X_INDEX_MSB = 7;
		private static final int Y_INDEX_LSB = 8;
		private static final int Y_INDEX_MSB = 9;
		private static final int Z_INDEX_LSB = 10;
		private static final int Z_INDEX_MSB = 11;
		
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
				
				Log.d(TAG, String.format("Timestamp: %u", timestamp));
				
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
						Log.d(TAG, String.format("Accelerometer: X: %d Y: %d Z: %d", x, y, z));
					}
					break;
				case DATA_SOURCE_GYRO:
					if(length == GYRO_PAYLOAD_LENGTH)
					{
						x = (packet[X_INDEX_LSB] & 0x00FF) | (packet[X_INDEX_MSB] << 8);
						y = (packet[Y_INDEX_LSB] & 0x00FF) | (packet[Y_INDEX_MSB] << 8);
						z = (packet[Z_INDEX_LSB] & 0x00FF) | (packet[Z_INDEX_MSB] << 8);
						//kinematicsEstimator.registerGyroValues(x, y, z, timestamp);
						Log.d(TAG, String.format("Gyroscope: X: %d Y: %d Z: %d", x, y, z));
					}
					break;
				case DATA_SOURCE_MAG:
					if(length == MAG_PAYLOAD_LENGTH)
					{
						x = (packet[X_INDEX_LSB] & 0x00FF) | (packet[X_INDEX_MSB] << 8);
						y = (packet[Y_INDEX_LSB] & 0x00FF) | (packet[Y_INDEX_MSB] << 8);
						z = (packet[Z_INDEX_LSB] & 0x00FF) | (packet[Z_INDEX_MSB] << 8);
						//kinematicsEstimator.registerMagValues(x, y, z, timestamp);
						Log.d(TAG, String.format("Magnetometer: X: %d Y: %d Z: %d", x, y, z));
					}
					break;
				default:
					break;
				}
			}
		}
	};
}
