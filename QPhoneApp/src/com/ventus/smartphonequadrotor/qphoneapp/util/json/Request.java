package com.ventus.smartphonequadrotor.qphoneapp.util.json;

/**
 * This class is the JSON template for a request packet. If the period of this
 * packet is null, then the packet can be used to poll a certain resource. If this
 * period is not null, then it refers to the period (in milliseconds) that the device
 * receiving this packet is supposed to push the value of the given resource.
 * @author Abhin
 *
 */
public class Request {
	private String Resource;
	private int Period;
	
	public Request(String Resource) {
		this.Resource = Resource;
	}
	
	public Request(String Resource, int Period) {
		this.Resource = Resource;
		this.Period = Period;
	}
	
	@Override
	public String toString() {
		return String.format("Resource: %1$s\nPeriod: %2$d\n", Resource, Period);
	}
}
