package communication;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

/**
 * This class takes care of all the over-heads of using XMPP as a means of communication.
 * @author Abhin
 *
 */
public class XmppClient {
	private Connection connection;
	private ChatManager chatManager;
	private Chat chat;
	
	public XmppClient(String host, int port, String serviceName){
		ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration(host, port, serviceName);
		connection = new XMPPConnection(connectionConfiguration);
	}
	
	/**
	 * Connects to the XMPP server and sets the presence to available.
	 * @throws XMPPException
	 */
	public void connect() throws XMPPException {
		connection.connect();
		connection.sendPacket(new Presence(Presence.Type.available));
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
	 * @param ownUsername The username of the QPhone on the server.
	 * @param ownPassword The password of the QPhone on the server.
	 * @param ownResource The resource name of the QPhone on the server.
	 * @param targetUsername The username of the controller application on the server.
	 * @param onMessageListener The listener that will be used to redirect the message to the {@link NetworkCommunicationManager}.
	 * @throws XMPPException
	 */
	public void startSession(String ownUsername, String ownPassword, String ownResource, String targetUsername, final OnMessageListener onMessageListener) throws XMPPException {
		if(!connection.isConnected()){
			connect();
		}
		connection.login(ownUsername, ownPassword, ownResource);
		
		chatManager = connection.getChatManager();
		chat = chatManager.createChat(targetUsername, new MessageListener(){
			@Override
			public void processMessage(Chat chat, Message message) {
				onMessageListener.onMessage(message.getBody());
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
}
