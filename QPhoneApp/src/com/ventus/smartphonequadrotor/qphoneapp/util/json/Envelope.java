package com.ventus.smartphonequadrotor.qphoneapp.util.json;

/**
 * This class envelopes the response, request and command packets coming from the
 * controller.
 * @author Abhin
 *
 */
public class Envelope {
	private Commands Commands;
	private Request[] Requests;
	private Responses Responses;
	
	public Envelope(Commands commands, Request[] requests, Responses responses) {
		this.Commands = commands;
		this.Requests = requests;
		this.Responses = responses;
	}
	
	public Commands Command() {
		return Commands;
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
			"Commands: %1$s\nRequests: %2$s\nResponses: %3$s\n", 
			Commands.toString(),
			Requests.toString(), 
			Responses.toString()
		);
	}
}
