package com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import com.ventus.smartphonequadrotor.qphoneapp.activities.BluetoothConnectionActivity;
import com.ventus.smartphonequadrotor.qphoneapp.services.MainService;
import com.ventus.smartphonequadrotor.qphoneapp.util.control.ControlLoop;
import com.ventus.smartphonequadrotor.qphoneapp.util.control.DataAggregator;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Manager bluetooth connection and data transfer.
 * @author abhin
 *
 */
public class BluetoothManager {
	private static final String TAG = BluetoothManager.class.getName();
	private static final UUID bluetoothConnectionUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private BluetoothAdapter adapter;
	private BluetoothDevice device;
	private BluetoothSocket socket;
	private InputStream inputStream;
	private OutputStream outputStream;
	private Object socketLock = new Object();
	private Object inputStreamLock = new Object();
	private Object outputStreamLock = new Object();
	private MainService owner;
	
	public BluetoothManager(MainService owner) {
		this.owner = owner;
		adapter = BluetoothAdapter.getDefaultAdapter();
	}
	
	/**
	 * This getter is for {@link DataAggregator} so that it can read from the bluetooth
	 * input stream.
	 * @return
	 */
	public InputStream getInputStream() {
		return this.inputStream;
	}
	
	/**
	 * This method obviously connects the bluetooth device to the 
	 * phone.
	 * @param macAddress
	 * @throws Exception 
	 */
	public void connect(String macAddress) {
		device = adapter.getRemoteDevice(macAddress);
		
		try {
			synchronized(socketLock) {
				socket = device.createRfcommSocketToServiceRecord(bluetoothConnectionUuid);
			}
			
			new Thread(new Runnable(){
				public void run() {
					InputStream tempIn = null;
					OutputStream tempOut = null;
					try {
						synchronized (socketLock) {
							socket.connect();						
						}
						tempIn = socket.getInputStream();
						tempOut = socket.getOutputStream();
						
						synchronized(inputStreamLock) {
							inputStream = tempIn;
						}
						synchronized(outputStreamLock) {
							outputStream = tempOut;
						}
						sendConnectionSuccess();
					} catch (IOException ex) {
						sendConnectionFailure();
					}
				}
			}, "BluetoothConnectionThread").start();
		} catch (Exception ex) {
			sendConnectionFailure();
		}
	} 
	
	private void sendConnectionFailure() {
		Intent intent = new Intent(BluetoothConnectionActivity.BLUETOOTH_CONNECTION_STATUS_UPDATE);
		intent.putExtra(
			BluetoothConnectionActivity.BLUETOOTH_CONNECTION_STATUS, 
			BluetoothConnectionActivity.BLUETOOTH_STATUS_CONNECTION_FAILURE
		);
		owner.sendBroadcast(intent);
	}
	
	private void sendConnectionSuccess() {
		Intent intent = new Intent(BluetoothConnectionActivity.BLUETOOTH_CONNECTION_STATUS_UPDATE);
		intent.putExtra(
			BluetoothConnectionActivity.BLUETOOTH_CONNECTION_STATUS, 
			BluetoothConnectionActivity.BLUETOOTH_STATUS_CONNECTED
		);
		owner.sendBroadcast(intent);
		bluetoothReader.start();
	}
	
	/**
	 * This method can be used to send messages to the QCB over bluetooth. This method
	 * is synchronous and thus can be accessed from the control loop directly.
	 * @param message
	 * @throws IOException
	 */
	public void write(byte[] message) throws IOException {
		synchronized(outputStreamLock){
			if (outputStream != null)
				outputStream.write(message);
		}
	}
	
	/**
	 * This thread is an infinite loop that contains a blocking call to the
	 * bluetooth input stream. 
	 */
	public Thread bluetoothReader = new Thread("BluetoothReaderThread") {
		public static final int BUFFER_SIZE = 2*QcfpParser.QCFP_MAX_PACKET_SIZE;
		
		@Override
		public void run() {
			byte[] buffer = new byte[BUFFER_SIZE];
			
			while (true) {
				try {
					if (owner.getBluetoothManager().getInputStream() == null) {
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
							//whatever
						}
						continue;
					}
					//the following is a blocking call.
					int length = owner.getBluetoothManager().getInputStream().read(buffer);
					//send this data to the service thread
					if (owner.getDataAggregator().bluetoothMessageHandler != null) {
						Message btMsg = owner.getDataAggregator().bluetoothMessageHandler.obtainMessage();
						btMsg.obj = buffer.clone();
						btMsg.arg1 = length;
						owner.getDataAggregator().bluetoothMessageHandler.sendMessage(btMsg);
					}
				} catch (IOException e) {
					Log.e(TAG, "Could not read from QCB", e);
				}
			}
		}
	};
}
