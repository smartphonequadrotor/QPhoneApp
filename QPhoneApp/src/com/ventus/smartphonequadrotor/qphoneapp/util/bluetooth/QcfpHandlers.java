package com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth;

import java.util.TreeMap;
import com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth.QcfpCallback;

public class QcfpHandlers {

	private TreeMap<Integer, QcfpCallback> callbackMap;

	public QcfpHandlers() {
		callbackMap = new TreeMap<Integer, QcfpCallback>();
	}

	public void dispatch(byte[] incomingPacket, int packetSize) {
		QcfpCallback callback = callbackMap.get(new Integer(incomingPacket[0]));
		if (callback != null) {
			callback.run(incomingPacket, packetSize);
		}
	}

	public void registerHandler(int command, QcfpCallback qcfpCallback) {
		callbackMap.put(command, qcfpCallback);
	}

}
