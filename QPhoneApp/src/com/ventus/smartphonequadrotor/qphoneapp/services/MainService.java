package com.ventus.smartphonequadrotor.qphoneapp.services;

import com.ventus.smartphonequadrotor.qphoneapp.services.intents.IntentHandler;
import com.ventus.smartphonequadrotor.qphoneapp.util.net.NetworkCommunicationManager;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
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

	@Override
	public void onCreate() {
		super.onCreate();
		networkCommunicationManager = new NetworkCommunicationManager();
		this.intentHandler = new IntentHandler(this);
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		declareIntentFilters();
	}

	public NetworkCommunicationManager getNetworkCommunicationManager() {
		return this.networkCommunicationManager;
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
		registerReceiver(intentHandler, intentFilter);
	}
	
	/**
	 * This uses the network communication manager to send a message to the
	 * controller (using either xmpp or direct connections).
	 * @param message
	 */
	public void sendMessage(String message) {
		try {
			this.networkCommunicationManager.sendMessage(message);
		} catch (Exception e) {
			Log.e(TAG, "Message Could not be sent: " + e.getMessage());
			Toast.makeText(this, "Message could not be sent", Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * Uses the network connection manager to setup connection.
	 * @param intent
	 */
	public void setupXmppConnection(Intent intent) {
		try {
			this.networkCommunicationManager.setupXmppConnection(intent, this);
		} catch (Exception e) {
			Log.e(TAG, "Xmpp connection could not be established: " + e.getMessage());
			Toast.makeText(this, "Xmpp connection failed", Toast.LENGTH_SHORT).show();
		}
	}
}
