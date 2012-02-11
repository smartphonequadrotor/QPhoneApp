package com.ventus.smartphonequadrotor.qphoneapp.util.net;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ventus.smartphonequadrotor.qphoneapp.services.MainService;
import com.ventus.smartphonequadrotor.qphoneapp.services.intents.IntentHandler;
import com.ventus.smartphonequadrotor.qphoneapp.util.json.Envelope;
import com.ventus.smartphonequadrotor.qphoneapp.util.json.ResponseAbstract;
import com.ventus.smartphonequadrotor.qphoneapp.util.json.TriAxisSensorResponse;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class NetworkCommunicationManager {
	public static final String TAG = NetworkCommunicationManager.class.getName();
	
	private XmppClient xmppClient;
	private DirectSocketClient directSocketClient;
	
	private NcmOnMessageListener onMessageListener;
	private Gson gson;
	
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
		gson = new Gson();
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
			Log.d(TAG, "Message from controller: " + message);
			//attempt to decode the message
			Envelope envelope = gson.fromJson(message, Envelope.class);
			Log.d(TAG, envelope.toString());
		}		
	}
	
	/**
	 * This method is used by the {@link MainService} when it receives an intent from
	 * the activity requesting a connection between the smartphone and the controller
	 * through XMPP.
	 * @param intent The intent that contains the connection data.
	 * @throws Exception 
	 */
	public void setupXmppConnection(Intent intent, Context context) throws Exception {
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
		this.connect();
	}
	
	/**
	 * This method is used by the {@link MainService} when it receives an intent (through {@link IntentHandler}).
	 * This class is obviously used to send a message to the controller once a connection has been established.
	 * It uses the best connection between the 2 options depending on the value of the preferredCommunicationMethod
	 * variable.
	 * @param message The message to be sent
	 * @throws Exception If neither direct socket nor xmpp connections are setup
	 */
	public void sendMessage(String message) throws Exception {
		if (xmppClient == null && directSocketClient == null) {
			throw new Exception("Un-initialized communication clients; please initialize one");
		} else if (xmppClient != null && (directSocketClient == null || preferredCommunicationMethod == CommunicationMethods.XMPP)){
			xmppClient.sendMessage(message);
		} else {
			//TODO directSocketClient.sendMessage(message);
		}
	}
}
