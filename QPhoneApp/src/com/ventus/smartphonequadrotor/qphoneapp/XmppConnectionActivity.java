package com.ventus.smartphonequadrotor.qphoneapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class XmppConnectionActivity extends Activity {
	public static final String TAG = XmppConnectionActivity.class.getName();
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
			getUserMessage();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void getUserMessage() {
		final EditText userMessage = new EditText(this);
		new AlertDialog.Builder(this)
			.setTitle("Send XMPP message")
			.setMessage("Send XMPP message")
			.setView(userMessage)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					Log.d(TAG, "text used: " + userMessage.getText());
				}
			}).show();
	}
}