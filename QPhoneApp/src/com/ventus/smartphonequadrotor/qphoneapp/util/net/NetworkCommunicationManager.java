package com.ventus.smartphonequadrotor.qphoneapp.util.net;

import com.ventus.smartphonequadrotor.qphoneapp.services.intents.IntentHandler;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class NetworkCommunicationManager {
	public static final String TAG = NetworkCommunicationManager.class.getName();
	
	private XmppClient xmppClient;
	private DirectSocketClient directSocketClient;
	
	private NcmOnMessageListener onMessageListener;
	
	private String xmppOwnJid;
	private String xmppOwnPassword;
	private String xmppOwnResource;
	private String xmppTargetJid;
	
	/**
	 * In case both the connection clients ( {@link XmppClient} and {@link DirectSocketClient} )
	 * are connected, then one of them will be chosen when sending the message using this variable.
	 */
	public CommunicationMethods preferredCommunicationMethod = CommunicationMethods.DIRECT_SOCKET;
	
	public NetworkCommunicationManager () {
		onMessageListener = new NcmOnMessageListener();
	}
	
	public NetworkCommunicationManager (XmppClient xmppClient) {
		this();
		this.xmppClient = xmppClient;
	}
	
	public NetworkCommunicationManager (DirectSocketClient directSocketClient) {
		this();
		this.directSocketClient = directSocketClient;
	}
	
	public void setXmppClient (XmppClient xmppClient) {
		this.xmppClient = xmppClient;
	}
	
	public void setDirectSocketClient (DirectSocketClient directSocketClient) {
		this.directSocketClient = directSocketClient;
	}
	
	public String getXmppOwnJid() {
		return xmppOwnJid;
	}

	public void setXmppOwnJid(String xmppOwnUsername) {
		this.xmppOwnJid = xmppOwnUsername;
	}

	public String getXmppOwnPassword() {
		return xmppOwnPassword;
	}

	public void setXmppOwnPassword(String xmppOwnPassword) {
		this.xmppOwnPassword = xmppOwnPassword;
	}

	public String getXmppOwnResource() {
		return xmppOwnResource;
	}

	public void setXmppOwnResource(String xmppOwnResource) {
		this.xmppOwnResource = xmppOwnResource;
	}

	public String getXmppTargetJid() {
		return xmppTargetJid;
	}

	public void setXmppTargetJid(String xmppTargetJid) {
		this.xmppTargetJid = xmppTargetJid;
	}

	/**
	 * This method chooses the best communication method available out of {@link XmppClient}
	 * and {@link DirectSocketClient} based on the value of preferredCommunicationMethod and 
	 * whether the clients of this instance have been set by the caller.
	 * @throws Exception
	 */
	public void connect() throws Exception {
		if (xmppClient == null && directSocketClient == null) {
			throw new Exception("Un-initialized communication clients; please initialize one");
		} else if (xmppClient != null && (directSocketClient == null || preferredCommunicationMethod == CommunicationMethods.XMPP)){
			xmppClient.connect();
			xmppClient.startSession(xmppOwnJid, xmppOwnPassword, xmppOwnResource, xmppTargetJid);
			xmppClient.onMessageListener = onMessageListener;
		} else {
			//TODO directSocketClient.connect();
		}
	}
	
	/**
	 * This class serves as the listener for the {@link NetworkCommunicationManager} when either the {@link XmppClient}
	 * or the {@link DirectSocketClient} receive a message over the network.
	 * @author Abhin
	 *
	 */
	public class NcmOnMessageListener extends OnMessageListener {
		@Override
		public void onMessage(String message) {
			Log.d(TAG, message);
		}		
	}
	
	/**
	 * This method is used by the {@link IntentHandler} when it receives an intent from
	 * the activity requesting a connection between the smartphone and the controller
	 * through XMPP.
	 * @param intent The intent that contains the connection data.
	 */
	public void setupXmppConnection(Intent intent, Context context) {
		this.xmppClient = new XmppClient(
			intent.getStringExtra(IntentHandler.ActionExtras.SERVER_ADDRESS.extra), 
			Integer.parseInt(intent.getStringExtra(IntentHandler.ActionExtras.SERVER_PORT.extra)), 
			intent.getStringExtra(IntentHandler.ActionExtras.XMPP_OWN_JID.extra).split("@")[1]
		);
		this.xmppOwnJid = intent.getStringExtra(IntentHandler.ActionExtras.XMPP_OWN_JID.extra);
		this.xmppOwnPassword = intent.getStringExtra(IntentHandler.ActionExtras.XMPP_OWN_PASSWORD.extra);
		this.xmppOwnResource = intent.getStringExtra(IntentHandler.ActionExtras.XMPP_OWN_RESOURCE.extra);
		this.xmppTargetJid = intent.getStringExtra(IntentHandler.ActionExtras.XMPP_TARGET_JID.extra);
		this.preferredCommunicationMethod = CommunicationMethods.XMPP;
		try {
			this.connect();
		} catch (Exception e) {
			Log.e(TAG, "failed to connect xmpp client: " + e.getMessage());
			Toast.makeText(context, "Could not connect to xmpp server", Toast.LENGTH_LONG).show();
		}
	}
}
