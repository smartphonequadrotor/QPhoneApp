package com.ventus.smartphonequadrotor.qphoneapp.util.json;

/**
 * This class envelopes the response, request and command packets coming from the
 * controller.
 * @author Abhin
 *
 */
public class Envelope {
	private Request[] Requests;
	private Responses Responses;
	
	public Envelope(Request[] Requests, Responses Responses) {
		this.Requests = Requests;
		this.Responses = Responses;
	}

	public Request[] getRequests() {
		return Requests;
	}

	public Responses getResponses() {
		return Responses;
	}

	@Override
	public String toString() {
		return String.format(
			"Requests: %1$s\nResponses: %2$s\n", 
			Requests, 
			Responses
		);
	}
}
