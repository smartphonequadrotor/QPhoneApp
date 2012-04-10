package com.ventus.smartphonequadrotor.qphoneapp.util.net;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

import android.util.Log;

/**
 * This class takes care of all the over-heads of using XMPP as a means of communication.
 * @author Abhin
 *
 */
public class XmppClient {
	public static final String TAG = XmppClient.class.getName();
	private Connection connection;
	private ChatManager chatManager;
	private Chat chat;
	
	/**
	 * The listener that will be used to redirect the message to the {@link NetworkCommunicationManager}.
	 */
	public OnMessageListener onMessageListener;
	
	/**
	 * Constructor
	 * @param host The actual host name (IP address or domain name) of the XMPP server
	 * @param port The port that the XMPP server is listening on for connections
	 * @param serviceName The name of the XMPP service that the JIDs are derived using 
	 */
	public XmppClient(String host, int port, String serviceName){
		ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration(host, port, serviceName);
		connectionConfiguration.setSASLAuthenticationEnabled(false);
		connection = new XMPPConnection(connectionConfiguration);
		Connection.addConnectionCreationListener(connectionCreationListener);
	}
	
	/**
	 * Connects to the XMPP server and sets the presence to available.
	 * @throws XMPPException
	 */
	public void connect() throws XMPPException {
		connection.connect();
	}
	
	/**
	 * Disconnects from the XMPP server after setting the presence to unavailable.
	 */
	public void disconnect() {
		connection.sendPacket(new Presence(Presence.Type.unavailable));
		connection.disconnect();
	}
	
	/**
	 * This methods logs the user into the XMPP server and then starts a chat with the specified target.
	 * When a message is received, it is redirected to the {@link NetworkCommunicationManager} using the {@link OnMessageListener}
	 * object.
	 * @param ownJid The Jid of the QPhone on the server.
	 * @param ownPassword The password of the QPhone on the server.
	 * @param ownResource The resource name of the QPhone on the server.
	 * @param targetJid The Jid of the controller application on the server.
	 * @throws XMPPException
	 */
	public void startSession(String ownJid, String ownPassword, String ownResource, String targetJid) throws XMPPException {
		if(!connection.isConnected()){
			connect();
		}
		connection.login(ownJid.split("@")[0], ownPassword, ownResource);
		connection.sendPacket(new Presence(Presence.Type.available));
		
		chatManager = connection.getChatManager();
		chat = chatManager.createChat(targetJid, new MessageListener(){
			public void processMessage(Chat chat, Message message) {
				if (onMessageListener != null) {
					onMessageListener.onMessage(message.getBody());
				}
			}
		});
	}
	
	/**
	 * Ends the XMPP session.
	 */
	public void endSession(){
		if(connection.isConnected()){
			disconnect();
		}
	}
	
	/**
	 * Sends a specified message to the controller application if it is connected.
	 * @param message The message to be sent
	 * @throws XMPPException
	 */
	public void sendMessage(String message) throws XMPPException{
		chat.sendMessage(message);
	}
	
	private ConnectionCreationListener connectionCreationListener = new ConnectionCreationListener() {
		
		public void connectionCreated(Connection connection) {
			Log.d(TAG, "Xmpp Connection Created: " + connection.getHost());
		}
	};
	
	private ConnectionListener connectionListener = new ConnectionListener() {
		
		public void reconnectionSuccessful() {
			Log.d(TAG, "Reconnection Successful");
		}
		
		public void reconnectionFailed(Exception arg0) {
			Log.d(TAG, "Reconnection failed", arg0);
		}
		
		public void reconnectingIn(int arg0) {
			Log.d(TAG, "Reconnecting in..." + arg0);
		}
		
		public void connectionClosedOnError(Exception arg0) {
			Log.d(TAG, "Connection closed on error: ", arg0);
		}
		
		public void connectionClosed() {
			Log.d(TAG, "Connection closed");
		}
	};
}
