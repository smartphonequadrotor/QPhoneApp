package com.ventus.smartphonequadrotor.qphoneapp.services;

import java.io.IOException;

import com.ventus.smartphonequadrotor.qphoneapp.services.intents.IntentHandler;
import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.BluetoothManager;
import com.ventus.smartphonequadrotor.qphoneapp.util.control.ControlLoop;
import com.ventus.smartphonequadrotor.qphoneapp.util.control.DataAggregator;
import com.ventus.smartphonequadrotor.qphoneapp.util.net.NetworkCommunicationManager;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * This is the main service that does all the processing related to getting data from the
 * sensors, communicating with the controller and the QCB and processing any and all data.
 * @author Abhin
 *
 */
public class MainService extends Service {
	public static final String TAG = MainService.class.getName();
	private IntentHandler intentHandler;
	private NetworkCommunicationManager networkCommunicationManager;
	private BluetoothManager bluetoothManager;
	private DataAggregator dataAggregator;
	private ControlLoop controlLoop;

	@Override
	public void onCreate() {
		super.onCreate();
		networkCommunicationManager = new NetworkCommunicationManager(this);
		bluetoothManager = new BluetoothManager(this);
		intentHandler = new IntentHandler(this);
		dataAggregator = new DataAggregator(this);
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		declareIntentFilters();
	}

	public NetworkCommunicationManager getNetworkCommunicationManager() {
		return this.networkCommunicationManager;
	}
	
	public BluetoothManager getBluetoothManager() {
		return this.bluetoothManager;
	}
	
	public DataAggregator getDataAggregator() {
		return this.dataAggregator;
	}

	/**
	 * This method simply returns null. This is to avoid any service binding.
	 * We require a background service for this application that keeps running
	 * regardless of the current state of the UI.
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	private void declareIntentFilters() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(IntentHandler.MESSAGE_CONTROLLER_ACTION);
		intentFilter.addAction(IntentHandler.XMPP_CONNECT_ACTION);
		intentFilter.addAction(IntentHandler.BLUETOOTH_CONNECT_ACTION);
		registerReceiver(intentHandler, intentFilter);
	}
	
	/**
	 * This uses the network communication manager to send a message to the
	 * controller (using either xmpp or direct connections).
	 * @param message
	 */
	public void sendNetworkMessage(String message) {
		try {
			this.networkCommunicationManager.sendNetworkMessage(message);
		} catch (Exception e) {
			Log.e(TAG, "Message Could not be sent: " + e.getMessage());
			Toast.makeText(this, "Message could not be sent", Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * This uses the {@link BluetoothManager} to send a message to the QCB
	 * over bluetooth.
	 * @param message
	 */
	public void sendBluetoothMessage(byte[] message) {
		try {
			this.bluetoothManager.write(message);
		} catch (IOException ioEx) {
			Log.e(TAG, "Could not send message", ioEx);
		}
	}
	
	/**
	 * For any components of {@link MainService} that run on a separate thread, this
	 * is the primary means of communication. 
	 */
	public Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.i(TAG, "Message received in the main service: " + msg.obj.toString());
			//TODO
		}
	};
}
