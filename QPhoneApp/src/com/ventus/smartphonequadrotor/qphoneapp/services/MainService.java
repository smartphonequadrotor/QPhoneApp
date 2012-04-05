package com.ventus.smartphonequadrotor.qphoneapp.services;

import java.io.IOException;

import com.ventus.smartphonequadrotor.qphoneapp.services.intents.IntentHandler;
import com.ventus.smartphonequadrotor.qphoneapp.util.SimpleMatrix;
import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.BluetoothManager;
import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpCommands;
import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpCommunication;
import com.ventus.smartphonequadrotor.qphoneapp.util.control.ControlLoop;
import com.ventus.smartphonequadrotor.qphoneapp.util.control.DataAggregator;
import com.ventus.smartphonequadrotor.qphoneapp.util.json.SystemState;
import com.ventus.smartphonequadrotor.qphoneapp.util.net.NetworkCommunicationManager;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
	public static final String TAG = MainService.class.getSimpleName();
	
	private IntentHandler intentHandler;
	private NetworkCommunicationManager networkCommunicationManager;
	private BluetoothManager bluetoothManager;
	private DataAggregator dataAggregator;
	private ControlLoop controlLoop;
	private QcfpCommunication qcfpCommunication;
	private BluetoothCommunicationLooper btCommunicationLooper;
	public MainServiceHandler handler;

	@Override
	public void onCreate() {
		super.onCreate();
		networkCommunicationManager = new NetworkCommunicationManager(this);
		bluetoothManager = new BluetoothManager(this);
		intentHandler = new IntentHandler(this);
		dataAggregator = new DataAggregator(this);
		qcfpCommunication = new QcfpCommunication(bluetoothManager);
		handler = new MainServiceHandler();
		controlLoop = new ControlLoop(handler);
		btCommunicationLooper = new BluetoothCommunicationLooper();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		declareIntentFilters();
		controlLoop.start();
		btCommunicationLooper.start();
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
	
	public QcfpCommunication getQcfpCommunication() {
		return this.qcfpCommunication;
	}
	
	public ControlLoop getControlLoop() {
		return this.controlLoop;
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
	
	public class MainServiceHandler extends Handler {
		/**
		 * The toast cannot be called safely from other threads. For this, they have
		 * to used this handler. The {@link Message#what} will have to be this integer.
		 * The {@link Message#obj} would have to be the message string and {@link Message#arg1}
		 * will have to be the the duration of the toast.
		 */
		public static final int TOAST_MESSAGE = -1;
		/**
		 * This is the message number used when the control loop has computed the desired motor speeds.
		 */
		public static final int MOTOR_SPEEDS_MESSAGE = 1;

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == TOAST_MESSAGE) {
				if (msg.obj == null)
					throw new IllegalArgumentException("Toast message missing");
				Toast.makeText(MainService.this, (String) msg.obj, msg.arg1).show();
			} else if (msg.what == MOTOR_SPEEDS_MESSAGE) {
				if (msg.obj == null)
					throw new IllegalArgumentException("Motor speeds matrix missing");
				SimpleMatrix motorSpeeds = (SimpleMatrix)msg.obj;
				Log.d("MOTOR SPEEDS", motorSpeeds.toString());
				//TODO: send this information to the QCB
			}
		}
		
	}
	
	/**
	 * This method is to be called when a packet from the controller is received
	 * that requires the quadrotor to be armed. This method starts a thread that calls
	 * the {@link QcfpCommunication#sendFlightMode(Boolean)} method.
	 * @param flightMode true if the quadrotor has to be armed, false otherwise
	 */
	public void sendFlightModeToQcb(boolean flightMode) {
		btCommunicationLooper.handler.post(new Runnable(){
			public void run() {
				try {
					qcfpCommunication.sendFlightMode(true);
				} catch (Exception e) {
					String errorStr = "Bluetooth failure: cannot send flight mode";
					Message msg = new Message();
					msg.what = MainServiceHandler.TOAST_MESSAGE;
					msg.obj = errorStr;
					msg.arg1 = Toast.LENGTH_LONG;
					handler.sendMessage(msg);
					Log.e(TAG, errorStr, e);
				}
			}
		});
	}
	
	/**
	 * This method is to be called when a packet from the controller is received that
	 * requires the quadrotor to start calibrating. This method starts a thread that calls
	 * the {@link QcfpCommunication#sendStartStopCalibration(Boolean)}
	 */
	public void sendCalibrateSignalToQcb(final boolean startStopCalibration) {
		btCommunicationLooper.handler.post(new Runnable(){
			public void run() {
				try {
					qcfpCommunication.sendStartStopCalibration(startStopCalibration);
				} catch (Exception e) {
					String errorStr = "Bluetooth failure: cannot send calibration command";
					Message msg = new Message();
					msg.what = MainServiceHandler.TOAST_MESSAGE;
					msg.obj = errorStr;
					msg.arg1 = Toast.LENGTH_LONG;
					handler.sendMessage(msg);
					Log.e(TAG, errorStr, e);
				}
			}
		});
	}
	
	public void sendMotorSpeedsToQcb(final SimpleMatrix motorSpeeds) {
		btCommunicationLooper.handler.post(new Runnable(){
			public void run() {
				if (motorSpeeds.getNumElements() != 4)
					throw new IllegalArgumentException("Number of motor speeds should be 4");
				Log.d(TAG, String.format("MotorSpeeds: %s", motorSpeeds));
				//TODO
			}
		});
	}
	
	/**
	 * When the quadrotor responds back with its current flight mode, this method is called.
	 * If the flight mode is {@link QcfpCommands#QCFP_FLIGHT_MODE_PENDING}, then nothing is done.
	 * Otherwise, the flight mode is reported back to the controller.
	 * @param flightMode
	 */
	public void flightModeReceivedfromQcb(int flightMode) {
		if (flightMode == QcfpCommands.QCFP_FLIGHT_MODE_ENABLE) {
			networkCommunicationManager.sendSystemState(SystemState.ARMED);
		} else if (flightMode == QcfpCommands.QCFP_FLIGHT_MODE_DISABLE) {
			networkCommunicationManager.sendSystemState(SystemState.DISARMED);
		}
	}

	/**
	 * When the quadrotor responds back with its current calibration status, this method is called.
	 * If the calibration status is {@link QcfpCommands#QCFP_CALIBRATE_QUADROTOR_CALIBRATING}, then nothing is done.
	 * Otherwise, the calibration mode is reported back to the controller.
	 * @param calibrationStatus
	 */
	public void calibrationStatusReceivedfromQcb(int calibrationStatus) {
		if (calibrationStatus == QcfpCommands.QCFP_CALIBRATE_QUADROTOR_CALIBRATED) {
			networkCommunicationManager.sendSystemState(SystemState.CALIBRATED);
		} else if (calibrationStatus == QcfpCommands.QCFP_CALIBRATE_QUADROTOR_UNABLE_TO_CALIBRATE
				|| calibrationStatus == QcfpCommands.QCFP_CALIBRATE_QUADROTOR_UNCALIBRATED) {
			networkCommunicationManager.sendSystemState(SystemState.UNABLE_TO_CALIBRATE);
		}
	}
	
	/**
	 * TODO: move this to the bluetooth manager.
	 * @author abhin
	 *
	 */
	public class BluetoothCommunicationLooper extends Thread {
		public static final int BLUETOOTH_UPDATE_MESSAGE = 1;
		public Handler handler;
		
		public BluetoothCommunicationLooper() {
			super("BluetoothCommunicationLooper");
		}
		
		public void run() {
			Looper.prepare();
			handler = new Handler();
			Looper.loop();
		}
	}
}
