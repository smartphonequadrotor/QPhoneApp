package com.ventus.smartphonequadrotor.qphoneapp.services.intents;

import com.ventus.smartphonequadrotor.qphoneapp.services.MainService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This method handles all the intents that the service can receive.
 * The intent filters are defined in the {@link MainService} service but this class
 * acts as the manager for them.
 * @author Abhin
 *
 */
public class IntentHandler extends BroadcastReceiver {
	public static final String TAG = IntentHandler.class.getName();
	
	MainService owner;
	
	/**
	 * This is the part of the action string that is common between all the action strings.
	 */
	private static final String ACTION_PRE_STRING = "com.ventus.smartphonequadrotor.qphoneapp.Intents.IntentHandler.";
	
	/**
	 * This action string is used to send a message to the controller (Message is used as a verb here
	 * and not a noun).
	 */
	public static final String MESSAGE_CONTROLLER_ACTION = ACTION_PRE_STRING + "MESSAGE_CONTROLLER_ACTION";
	
	/**
	 * This action string is used to connect to the controller using XMPP
	 */
	public static final String XMPP_CONNECT_ACTION = ACTION_PRE_STRING + "XMPP_CONNECT_ACTION";
	
	/**
	 * This action string is used to connect to the controller using a direct connection
	 */
	public static final String DIRECT_CONNECT_ACTION = ACTION_PRE_STRING + "DIRECT_CONNECT_ACTION";
	
	public IntentHandler(MainService owner) {
		this.owner = owner;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		
		if (action.equals(MESSAGE_CONTROLLER_ACTION)) {
			Log.d(TAG, intent.getStringExtra(ActionExtras.MESSAGE_FOR_CONTROLLER.extra));
			//get the string that has to be sent to the controller
		} else if (action.equals(XMPP_CONNECT_ACTION)) {
			owner.getNetworkCommunicationManager().setupXmppConnection(intent, owner);
		}
	}
	
	/**
	 * This enum contains all the key strings for all the key-value extras that can be attached to
	 * various intents when invoking a certain action.
	 * @author Abhin
	 *
	 */
	public enum ActionExtras {
		/**
		 * The address of the server (both for XMPP and Direct connections).
		 */
		SERVER_ADDRESS("SERVER_ADDRESS"),
		
		/**
		 * The port of the server (both for XMPP and Direct connections).
		 */
		SERVER_PORT("SERVER_PORT"),
		
		/**
		 * The Jabber ID of the phone.
		 */
		XMPP_OWN_JID("XMPP_OWN_JID"),
		
		/**
		 * The Jabber ID of the controller.
		 */
		XMPP_TARGET_JID("XMPP_TARGET_JID"),
		
		/**
		 * The password of the phone on the XMPP server.
		 */
		XMPP_OWN_PASSWORD("XMPP_OWN_PASSWORD"),
		
		/**
		 * The resource name of the phone.
		 */
		XMPP_OWN_RESOURCE("XMPP_OWN_RESOURCE"),
		
		/*
		 * This is the message to be sent to the controller.
		 */
		MESSAGE_FOR_CONTROLLER("MESSAGE_FOR_CONTROLLER");
		
		public final String extra;
		ActionExtras(String extra) {
			this.extra = extra;
		}
	}

}
