package com.ventus.smartphonequadrotor.qphoneapp.util.json;

/**
 * This class is the JSON template for a response packet that is sent in response
 * to a request packet. This is a generic class that can be used to hold data of any
 * kind.
 * @author Abhin
 *
 */
public abstract class ResponseAbstract {
	private long Timestamp;
	
	public ResponseAbstract (long Timestamp) {
		this.Timestamp = Timestamp;
	}

	public long getTimestamp() {
		return Timestamp;
	}
	
	public String toString() {
		return String.format("Timestamp: %1$d\n", Timestamp);
	}
}
