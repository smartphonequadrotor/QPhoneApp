package communication;

import android.util.Log;

public class NetworkCommunicationManager {
	public static final String TAG = NetworkCommunicationManager.class.getName();
	
	private XmppClient xmppClient;
	private DirectSocketClient directSocketClient;
	
	private NcmOnMessageListener onMessageListener;
	
	private String xmppOwnUsername;
	private String xmppOwnPassword;
	private String xmppOwnResource;
	private String xmppTargetUsername;
	
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
	
	public String getXmppOwnUsername() {
		return xmppOwnUsername;
	}

	public void setXmppOwnUsername(String xmppOwnUsername) {
		this.xmppOwnUsername = xmppOwnUsername;
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

	public String getXmppTargetUsername() {
		return xmppTargetUsername;
	}

	public void setXmppTargetUsername(String xmppTargetUsername) {
		this.xmppTargetUsername = xmppTargetUsername;
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
			xmppClient.startSession(xmppOwnUsername, xmppOwnPassword, xmppOwnResource, xmppTargetUsername);
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
}
