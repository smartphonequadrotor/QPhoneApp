package com.ventus.smartphonequadrotor.qphoneapp.util.control;

import java.io.IOException;

import android.util.Log;

import com.ventus.smartphonequadrotor.qphoneapp.services.MainService;
import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpHandlers;
import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpParser;
import com.ventus.smartphonequadrotor.qphoneapp.util.json.Envelope;
import com.ventus.smartphonequadrotor.qphoneapp.util.net.NetworkCommunicationManager;

/**
 * This is the class that gets all the data from the controller and the
 * QCB and aggregates it so that it could be used by the control systems.
 * @author abhin
 *
 */
public class DataAggregator {
	public static final String TAG = DataAggregator.class.getName();
	public static final int MAX_QCFP_PACKET_SIZE = 32;
	
	private MainService owner;
	private QcfpHandlers packetHandlers;
	
	public DataAggregator(MainService owner) {
		this.owner = owner;
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
			owner.sendBluetoothMessage("g");
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
			byte[] buffer = new byte[2*MAX_QCFP_PACKET_SIZE];
			QcfpParser bluetoothDataParser = new QcfpParser(MAX_QCFP_PACKET_SIZE, packetHandlers);
			
			while (true) {
				try {
					int length = owner.getBluetoothManager().getInputStream().read(buffer);
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
}
