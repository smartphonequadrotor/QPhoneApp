package com.ventus.smartphonequadrotor.qphoneapp;

import communication.CommunicationMethods;
import communication.NetworkCommunicationManager;
import communication.XmppClient;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
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
	
    /** Called when the activity is first created. */
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
        
        connectBtn.setOnClickListener(new ConnectBtn_OnClickListener());
    }
    
    /**
     * This class is the listener for the connect button and is responsible for calling 
     * the connect method of {@link NetworkCommunicationManager}, in addition to some other
     * house-keeping.
     * @author Abhin
     *
     */
    public class ConnectBtn_OnClickListener implements View.OnClickListener {
		public void onClick(View v) {
			XmppClient xmppClient = new XmppClient(
				serverAddress.getText().toString(),
				Integer.parseInt(serverPortNum.getText().toString()),
				ownJabberId.getText().toString().split("@")[1]				//the second half of the JID is the serviceName
			);
			networkCommunicationManager.setXmppOwnUsername(ownJabberId.getText().toString().split("@")[0]);	//first half of JID
			networkCommunicationManager.setXmppOwnPassword(password.getText().toString());
			networkCommunicationManager.setXmppOwnResource("TODO");
			networkCommunicationManager.setXmppTargetUsername(targetJabberId.getText().toString());
			networkCommunicationManager.setXmppClient(xmppClient);
			networkCommunicationManager.preferredCommunicationMethod = CommunicationMethods.XMPP;
			try {
				//TODO this stuff should be ideally done in a separate thread
				networkCommunicationManager.connect();
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}
		}
    }
}