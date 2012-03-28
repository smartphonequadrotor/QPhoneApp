package com.ventus.smartphonequadrotor.qphoneapp.util.net;

import com.google.gson.Gson;
import com.ventus.smartphonequadrotor.qphoneapp.activities.XmppConnectionActivity;
import com.ventus.smartphonequadrotor.qphoneapp.services.MainService;
import com.ventus.smartphonequadrotor.qphoneapp.services.MainService.MainServiceHandler;
import com.ventus.smartphonequadrotor.qphoneapp.services.intents.IntentHandler;
import com.ventus.smartphonequadrotor.qphoneapp.util.json.Envelope;
import com.ventus.smartphonequadrotor.qphoneapp.util.json.Responses;
import com.ventus.smartphonequadrotor.qphoneapp.util.json.SystemState;
import com.ventus.smartphonequadrotor.qphoneapp.util.json.TriAxisSensorResponse;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * This class provides a common interface for using the network communication to interface with
 * the controller. This class does not require the user to differentiate between the Xmpp or Direct
 * Socket methods for basic reads and writes. Thus, once the initial setup is done, the users of this
 * class can be blissfully ignorant of the actual implementation details.
 * @author abhin
 *
 */
public class NetworkCommunicationManager {
	public static final String TAG = NetworkCommunicationManager.class.getName();
	private MainService owner;
	
	private XmppClient xmppClient;
	private DirectSocketClient directSocketClient;
	
	private NcmOnMessageListener onMessageListener;
	private Gson gson;
	private NetworkCommunicationLooper networkCommunicationLooper;
	
	private String xmppOwnJid;
	private String xmppOwnPassword;
	private String xmppOwnResource;
	private String xmppTargetJid;
	
	/**
	 * In case both the connection clients ( {@link XmppClient} and {@link DirectSocketClient} )
	 * are connected, then one of them will be chosen when sending the message using this variable.
	 */
	public CommunicationMethods preferredCommunicationMethod = CommunicationMethods.DIRECT_SOCKET;
	
	public NetworkCommunicationManager (MainService owner) {
		this.owner = owner;
		onMessageListener = new NcmOnMessageListener();
		gson = new Gson();
		networkCommunicationLooper = new NetworkCommunicationLooper();
		networkCommunicationLooper.start();
	}
	
	public NetworkCommunicationManager (XmppClient xmppClient, MainService owner) {
		this(owner);
		this.xmppClient = xmppClient;
	}
	
	public NetworkCommunicationManager (DirectSocketClient directSocketClient, MainService owner) {
		this(owner);
		this.directSocketClient = directSocketClient;
	}
	
	/**
	 * This method should be called by the {@link MainService} when it is ending. This method will
	 * stop the networkCommunicationHandler;
	 */
	public void cleanup() {
		if (networkCommunicationLooper.handler != null && networkCommunicationLooper.handler.getLooper() != null)
			networkCommunicationLooper.handler.getLooper().quit();
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
			//attempt to decode the message
			Envelope envelope = gson.fromJson(message, Envelope.class);
			if (envelope != null) {
				if (envelope.getCommands() != null) {
					if (envelope.getCommands().getMoveCommandArray() != null 
							&& envelope.getCommands().getMoveCommandArray().length > 0) {
						owner.getDataAggregator().processMoveCommand(envelope.getCommands().getMoveCommandArray());
					} else if (!envelope.getCommands().getSystemState().equals("")) {
						if (envelope.getCommands().getSystemState().equals(SystemState.ARMED.toString())) {
							//the user wishes to arm the quadrotor
							owner.sendFlightModeToQcb(true);
						} else if (envelope.getCommands().getSystemState().equals(SystemState.CALIBRATING.toString())) {
							//the user wishes to calibrate the quadrotor
							owner.sendCalibrateSignalToQcb(true);
						}
					}
				}
			}
		}		
	}
	
	/**
	 * This method is used by the {@link MainService} when it receives an intent from
	 * the activity requesting a connection between the smartphone and the controller
	 * through XMPP.
	 * @param intent The intent that contains the connection data.
	 * @throws Exception 
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
		networkCommunicationLooper.handler.post(new Runnable(){
			public void run() {
				try {
					connect();
					sendConnectionSuccess();
				} catch (Exception e) {
					Log.e(TAG, "Could not connect to xmpp server", e);
					sendConnectionFailure();
				}
			}
		});
	}
	
	/**
	 * This method is used by the {@link MainService} when it receives an intent (through {@link IntentHandler}).
	 * This class is obviously used to send a message to the controller once a connection has been established.
	 * It uses the best connection between the 2 options depending on the value of the preferredCommunicationMethod
	 * variable.
	 * @param message The message to be sent
	 * @throws Exception If neither direct socket nor xmpp connections are setup
	 */
	public void sendNetworkMessage(String message) throws Exception {
		if (xmppClient == null && directSocketClient == null) {
			throw new Exception("Un-initialized communication clients; please initialize one");
		} else if (xmppClient != null && (directSocketClient == null || preferredCommunicationMethod == CommunicationMethods.XMPP)){
			xmppClient.sendMessage(message);
		} else {
			//TODO directSocketClient.sendMessage(message);
		}
	}

	/**
	 * This method is used by the {@link MainService} when it receives an intent (through {@link IntentHandler}).
	 * This class is obviously used to send a message to the controller once a connection has been established.
	 * It uses the best connection between the 2 options depending on the value of the preferredCommunicationMethod
	 * variable.
	 * @param message The message to be sent
	 * @throws Exception If neither direct socket nor xmpp connections are setup
	 */
	public void sendNetworkMessage(Envelope envelope) throws Exception {
		sendNetworkMessage(gson.toJson(envelope));
	}
	
	private void sendConnectionFailure() {
		Intent intent = new Intent(XmppConnectionActivity.NETWORK_CONNECTION_STATUS_UPDATE);
		intent.putExtra(
			XmppConnectionActivity.NETWORK_CONNECTION_STATUS, 
			XmppConnectionActivity.NETWORK_STATUS_CONNECTION_FAILURE
		);
		owner.sendBroadcast(intent);
	}
	
	private void sendConnectionSuccess() {
		Intent intent = new Intent(XmppConnectionActivity.NETWORK_CONNECTION_STATUS_UPDATE);
		intent.putExtra(
			XmppConnectionActivity.NETWORK_CONNECTION_STATUS,
			XmppConnectionActivity.NETWORK_STATUS_CONNECTED
		);
		owner.sendBroadcast(intent);
	}
	
	/**
	 * Sends the system state to the controller over the network.
	 * @param state the state of the quadrotor.
	 */
	public void sendSystemState(final SystemState state) {
		networkCommunicationLooper.handler.post(new Runnable(){
			public void run() {
				Responses responses = new Responses(null, null, null, null, null, state.toString(), null);
				Envelope envelope = new Envelope(null, null, responses);
				try {
					sendNetworkMessage(envelope);
				} catch (Exception e) {
					String errorStr = "Network failure: could not send system status";
					Message msg = new Message();
					msg.what = MainServiceHandler.TOAST_MESSAGE;
					msg.obj = errorStr;
					msg.arg1 = Toast.LENGTH_LONG;
					owner.handler.sendMessage(msg);
					Log.e(TAG, errorStr, e);
				}
			}
		});
	}
	
	public class NetworkCommunicationLooper extends Thread {
		public Handler handler = new Handler();
		
		public NetworkCommunicationLooper() {
			super("NetworkCommunicationLooper");
		}
		
		public void run() {
			//setup looper stuff
			Looper.prepare();
			Looper.loop();
		}
	}
}
