package com.ventus.smartphonequadrotor.qphoneapp.util.control;

import java.io.IOException;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.ventus.smartphonequadrotor.qphoneapp.services.MainService;
import com.ventus.smartphonequadrotor.qphoneapp.util.SimpleMatrix;
import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpHandlers;
import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpParser;
import com.ventus.smartphonequadrotor.qphoneapp.util.json.Envelope;
import com.ventus.smartphonequadrotor.qphoneapp.util.json.MoveCommand;
import com.ventus.smartphonequadrotor.qphoneapp.util.net.NetworkCommunicationManager;

/**
 * This is the class that gets all the data from the controller and the
 * QCB and aggregates it so that it could be used by the control systems.
 * @author abhin
 */
public class DataAggregator {
	public static final String TAG = DataAggregator.class.getName();
	public static final int BT_BUFFER_SIZE = 1024;
	
	private MainService owner;
	private byte[] btBuffer = new byte[BT_BUFFER_SIZE];
	private QcfpHandlers packetHandlers;
	
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
	}
	
	/**
	 * This method receives envelopes immediately after the {@link NetworkCommunicationManager}
	 * gets a message from the controller. The messages have to be processed and the correct
	 * actions have to be taken. This method is called as part of an event listener, so operations
	 * involving heavy lifting are not recommended without using external threads.
	 * @param envelope
	 */
	public void processControllerMessage(Envelope envelope) {
		if (envelope.getCommands().getMoveCommandArray().length != 0) {
			//the controller just sent a series of move commands
			for (int i = 0; i < envelope.getCommands().getMoveCommandArray().length; i++) {
				MoveCommand moveCommand = envelope.getCommands().getMoveCommandArray()[i];
				SimpleMatrix deltaDisplacement = new SimpleMatrix(3, 1, true, new double[]{
					moveCommand.getXVector(),
					moveCommand.getYVector(),
					moveCommand.getZVector()
				});
				//normalize deltaDisplacement to make it a direction vector
				deltaDisplacement = deltaDisplacement.divide(deltaDisplacement.normF());
				//multiply the direction vector with the speed and time to get the actual
				//delta displacement
				deltaDisplacement = deltaDisplacement.mult(
					moveCommand.getSpeed()*moveCommand.getDuration()
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
		bluetoothReader.start();
	}
	
	/**
	 * This thread is an infinite loop that contains a blocking call to the
	 * bluetooth input stream. 
	 */
	Thread bluetoothReader = new Thread() {
		@Override
		public void run() {
			byte[] buffer = new byte[2*QcfpParser.QCFP_MAX_PACKET_SIZE];
			QcfpParser bluetoothDataParser = new QcfpParser(QcfpParser.QCFP_MAX_PACKET_SIZE, packetHandlers);
			
			while (true) {
				try {
					//the following is a blocking call.
					int length = owner.getBluetoothManager().getInputStream().read(btBuffer);
					if(length > 0) {
						// Decodes data and dispatches packet handler if a full packet is received
						bluetoothDataParser.addData(buffer, length);
					}
				} catch (IOException e) {
					Log.e(TAG, "Could not read from QCB", e);
				}
			}
		}
	};
	
	/**
	 * The bluetooth reader thread will use this handler to send messages to the service
	 * thread. These messages will contain the object parsed from the bluetooth strings.
	 */
	private Handler bluetoothMessageHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			//TODO: get the accelerometer, magnetometer and gyro values
		}
	};
}
