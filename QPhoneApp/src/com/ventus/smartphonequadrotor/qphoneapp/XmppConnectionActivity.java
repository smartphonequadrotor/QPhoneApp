package com.ventus.smartphonequadrotor.qphoneapp;

import communication.CommunicationMethods;
import communication.NetworkCommunicationManager;
import communication.XmppClient;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class XmppConnectionActivity extends Activity {
	private EditText serverAddress;
	private EditText serverPortNum;
	private EditText password;
	private EditText jabberId;
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
        jabberId = (EditText)findViewById(R.id.jabbelIdTxt);
        connectBtn = (Button)findViewById(R.id.connectBtn);
        
        networkCommunicationManager = new NetworkCommunicationManager();
        
        connectBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				XmppClient xmppClient = new XmppClient(
					serverAddress.getText().toString(),
					Integer.parseInt(serverPortNum.getText().toString()),
					jabberId.getText().toString().split("@")[1]				//the second half of the JID is the serviceName
				);
				networkCommunicationManager.setXmppClient(xmppClient);
				networkCommunicationManager.preferredCommunicationMethod = CommunicationMethods.XMPP;
				try {
					//TODO this stuff should be ideally done in a separate thread
					networkCommunicationManager.connect();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
    }
}