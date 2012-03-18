package com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth;

import java.util.TreeMap;

import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpCallback;

public class QcfpHandlers {

	public static int QCFP_ASYNC_DATA = 0x10;
	public static int QCFP_CALIBRATE_QUADROTOR = 0x40;
	public static int QCFP_FLIGHT_MODE = 0x41;
	public static int QCFP_RAW_MOTOR_CONTROL = 0xF0;
	
	private TreeMap<Integer, QcfpCallback> callbackMap;
	
	public QcfpHandlers()
	{
		callbackMap = new TreeMap<Integer, QcfpCallback>();
	}
	
	public void dispatch(byte[] incoming_packet, int packet_size) {
		QcfpCallback callback = callbackMap.get(new Integer(incoming_packet[0]));
		if(callback != null)
		{
			callback.run(incoming_packet, packet_size);
		}
	}

	public void registerHandler(int command, QcfpCallback qcfpCallback) {
		callbackMap.put(command, qcfpCallback);
	}

}
