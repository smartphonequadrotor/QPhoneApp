package com.ventus.smartphonequadrotor.qphoneapp.activities;

import com.ventus.smartphonequadrotor.qphoneapp.R;
import com.ventus.smartphonequadrotor.qphoneapp.R.id;
import com.ventus.smartphonequadrotor.qphoneapp.R.layout;
import com.ventus.smartphonequadrotor.qphoneapp.R.menu;
import com.ventus.smartphonequadrotor.qphoneapp.services.MainService;
import com.ventus.smartphonequadrotor.qphoneapp.services.intents.IntentHandler;
import com.ventus.smartphonequadrotor.qphoneapp.util.net.CommunicationMethods;
import com.ventus.smartphonequadrotor.qphoneapp.util.net.NetworkCommunicationManager;
import com.ventus.smartphonequadrotor.qphoneapp.util.net.XmppClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * This activity lets the user setup all the connection parameters for a connection to the
 * XMPP server.
 * @author Abhin
 *
 */
public class XmppConnectionActivity extends Activity {
	public static final String TAG = XmppConnectionActivity.class.getName();
	
	private EditText serverAddress;
	private EditText serverPortNum;
	private EditText password;
	private EditText ownJabberId;
	private EditText targetJabberId;
	private Button connectBtn;
	private NetworkCommunicationManager networkCommunicationManager;
	
	/*************************************************************
	 * Over-ridden methods from Activity	
	 */
	
    /** 
     * Called when the activity is first created. 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.xmpp_setup);
        
        serverAddress = (EditText)findViewById(R.id.serverAddressTxt);
        serverPortNum = (EditText)findViewById(R.id.serverPortNumberTxt);
        password = (EditText)findViewById(R.id.passwordTxt);
        ownJabberId = (EditText)findViewById(R.id.ownJabberIdTxt);
        targetJabberId = (EditText)findViewById(R.id.targetJabberIdTxt);
        connectBtn = (Button)findViewById(R.id.connectBtn);
        
        networkCommunicationManager = new NetworkCommunicationManager();
        
		Intent mainServiceStarter = new Intent(XmppConnectionActivity.this, MainService.class);
		startService(mainServiceStarter);
        
        connectBtn.setOnClickListener(new ConnectBtn_OnClickListener());
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.xmpp_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.xmpp_menu_sendmsg:
			String userMessage = getUserMessage();
			Intent messageControllerIntent = new Intent(IntentHandler.MESSAGE_CONTROLLER_ACTION);
			messageControllerIntent.putExtra(IntentHandler.ActionExtras.MESSAGE_FOR_CONTROLLER.extra, userMessage);
			sendBroadcast(messageControllerIntent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
    
    /***************************************************************
     * Event listeners
     */
    
    /**
     * This class is the listener for the connect button and is responsible for calling 
     * the connect method of {@link NetworkCommunicationManager}, in addition to some other
     * house-keeping.
     * @author Abhin
     *
     */
    public class ConnectBtn_OnClickListener implements View.OnClickListener {
		public void onClick(View v) {
			Intent connectionIntent = createConnectionIntent();
			sendBroadcast(connectionIntent);
		}
    }

    /****************************************************************
     * Helper methods
     */
    
    /**
     * This method asks the user for a message to be sent to the controller.
     * @return userMessage String containing the message from the user.
     */
	private String getUserMessage() {
		final EditText userMessage = new EditText(this);
		new AlertDialog.Builder(this)
			.setTitle("Send XMPP message")
			.setMessage("Send XMPP message")
			.setView(userMessage)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					//use intents to send data from here to the network communication manager
					Log.d(TAG, "text used: " + userMessage.getText());
				}
			}).show();
		return userMessage.getText().toString();
	}
	
	/**
	 * Creates an intent that contains all the data that is needed to successfully establish an XMPP connection.
	 * @return the intent created
	 */
	private Intent createConnectionIntent() {
		Intent intent = new Intent(IntentHandler.XMPP_CONNECT_ACTION);
		intent.putExtra(IntentHandler.ActionExtras.SERVER_ADDRESS.extra, serverAddress.getText().toString());
		intent.putExtra(IntentHandler.ActionExtras.SERVER_PORT.extra, serverPortNum.getText().toString());
		intent.putExtra(IntentHandler.ActionExtras.XMPP_OWN_JID.extra, ownJabberId.getText().toString());
		intent.putExtra(IntentHandler.ActionExtras.XMPP_TARGET_JID.extra, targetJabberId.getText().toString());
		intent.putExtra(IntentHandler.ActionExtras.XMPP_OWN_PASSWORD.extra, password.getText().toString());
		intent.putExtra(IntentHandler.ActionExtras.XMPP_OWN_RESOURCE.extra, "TODO");
		return intent;
	}
}