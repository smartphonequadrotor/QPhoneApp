package com.ventus.smartphonequadrotor.qphoneapp;

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
        
        connectBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				//TODO create connection
			}
		});
    }
}