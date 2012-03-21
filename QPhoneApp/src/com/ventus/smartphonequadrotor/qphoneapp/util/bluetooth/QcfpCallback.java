package com.ventus.smartphonequadrotor.qphoneapp.util.bluetooth;

public abstract class QcfpCallback {
	
	public abstract void run(byte[] packet, int length);
}
