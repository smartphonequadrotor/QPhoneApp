package com.ventus.smartphonequadrotor.qphoneapp.util.net;

/**
 * This object is used to redirect incoming messages from the {@link XmppClient} and {@link DirectSocketClient} objects to
 * the {@link NetworkCommunicationManager} class.
 * @author Abhin
 *
 */
public abstract class OnMessageListener {
	public abstract void onMessage(String message);
}
