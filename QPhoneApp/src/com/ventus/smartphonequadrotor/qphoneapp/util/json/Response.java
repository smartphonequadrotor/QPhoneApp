package com.ventus.smartphonequadrotor.qphoneapp.util.json;

/**
 * This class is the JSON template for a response packet that is sent in response
 * to a request packet. This is a generic class that can be used to hold data of any
 * kind.
 * @author Abhin
 *
 */
public class Response <DataType> {
	private long Timestamp;
	private DataType data;
	private String type;
	
	public Response (long timestamp, DataType data, String type) {
		this.Timestamp = timestamp;
		this.data = data;
		this.type = type;
	}

	public long getTimestamp() {
		return Timestamp;
	}

	public DataType getData() {
		return data;
	}

	public String getType() {
		return type;
	}
}
