package com.ventus.smartphonequadrotor.qphoneapp.util.net;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.util.Log;

public class DirectSocketClient {
	public static final String TAG = DirectSocketClient.class.getSimpleName();
	private HttpURLConnection connection;
	private InputStream inStream;
	private static int READ_BUFFER_SIZE = 128;
	private DirectSocketReaderThread readerThread;
	
	/**
	 * The listener that will be used to redirect the message to the {@link NetworkCommunicationManager}.
	 */
	public OnMessageListener onMessageListener;
	
	public DirectSocketClient(String host, int port) {
		try {
			URL url = new URL(host);
			connection = (HttpURLConnection) url.openConnection();
			inStream = new BufferedInputStream(connection.getInputStream());
			readerThread = new DirectSocketReaderThread();
			readerThread.start();
		} catch (MalformedURLException e) {
			Log.e(TAG, "Malformed URL", e);
		} catch (IOException e) {
			Log.e(TAG, "could not open connection", e);
		}
	}
	
	public void cleanup() {
		this.readerThread.stopReaderThread();
	}
	
	public class DirectSocketReaderThread extends Thread {
		public byte[] buffer = new byte[READ_BUFFER_SIZE];
		boolean shouldRun = true;
		@Override
		public void run() {
			while (shouldRun) {
				try {
					inStream.read(buffer);
				} catch (IOException e) {
					Log.e(TAG, "could not read from connection", e);
				}
			}
		}
		
		public void stopReaderThread() {
			this.shouldRun = false;
		}
	}
}
