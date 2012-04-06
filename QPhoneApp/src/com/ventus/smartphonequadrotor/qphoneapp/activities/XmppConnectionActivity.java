package com.ventus.smartphonequadrotor.qphoneapp.activities;

import com.ventus.smartphonequadrotor.qphoneapp.R;
import com.ventus.smartphonequadrotor.qphoneapp.services.intents.IntentHandler;
import com.ventus.smartphonequadrotor.qphoneapp.util.net.NetworkCommunicationManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * This activity lets the user setup all the connection parameters for a connection to the
 * XMPP server.
 * @author Abhin
 *
 */
public class XmppConnectionActivity extends Activity {
	public static final String TAG = XmppConnectionActivity.class.getName();
	public static final String NETWORK_CONNECTION_STATUS_UPDATE = "com.ventus."
			+ "smartphonequadrotor.qphoneapp.activities.XmppConnectionActivity." 
			+ "NETWORK_CONNECTION_STATUS_UPDATE";
	public static final String NETWORK_CONNECTION_STATUS = "NETWORK_CONNECTION_STATUS";
	public static final int NETWORK_STATUS_CONNECTED = 1;
	public static final int NETWORK_STATUS_DISCONNECTED = 0;
	public static final int NETWORK_STATUS_CONNECTION_FAILURE = -1;
	
	private IntentFilter intentFilter;
	private EditText serverAddress;
	private EditText serverPortNum;
	private EditText password;
	private EditText ownJabberId;
	private EditText targetJabberId;
	private Button connectBtn;
	private Button nextBtn;
	
	/*************************************************************
	 * Over-ridden methods from Activity	
	 */
	
    /** 
     * Called when the activity is first created. 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.xmpp_setup);
        
        initBroadcastReceiver();
        
        serverAddress = (EditText)findViewById(R.id.serverAddressTxt);
        serverPortNum = (EditText)findViewById(R.id.serverPortNumberTxt);
        password = (EditText)findViewById(R.id.passwordTxt);
        ownJabberId = (EditText)findViewById(R.id.ownJabberIdTxt);
        targetJabberId = (EditText)findViewById(R.id.targetJabberIdTxt);
        connectBtn = (Button)findViewById(R.id.netConnectBtn);
        nextBtn = (Button)findViewById(R.id.netNextBtn);
        
        connectBtn.setOnClickListener(new ConnectBtn_OnClickListener());
        nextBtn.setOnClickListener(new NextBtn_OnClickListener());
        nextBtn.setEnabled(false);
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
		case R.id.xmpp_menu_sendtestmsg:
			Intent messageControllerIntent = new Intent(IntentHandler.MESSAGE_CONTROLLER_ACTION);
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
			setProgressBarIndeterminateVisibility(true);
			Intent connectionIntent = createConnectionIntent();
			sendBroadcast(connectionIntent);
		}
    }
    
    /**
     * This class is the listener for the next button on the {@link XmppConnectionActivity}.
     * It starts up the "ready-to-fly" activity.
     * @author abhin
     *
     */
    public class NextBtn_OnClickListener implements View.OnClickListener {
    	public void onClick(View v) {
    		
    		Intent intent = new Intent(XmppConnectionActivity.this, ReadyToFlyActivity.class);
    		startActivity(intent);
    	}
    }
    
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(NETWORK_CONNECTION_STATUS_UPDATE)) {
				int connected = intent.getIntExtra(NETWORK_CONNECTION_STATUS, NETWORK_STATUS_DISCONNECTED);
				if (connected == NETWORK_STATUS_CONNECTED) {
					nextBtn.setEnabled(true);
					Toast.makeText(XmppConnectionActivity.this, R.string.net_device_connected, Toast.LENGTH_SHORT).show();
				} else if (connected == NETWORK_STATUS_DISCONNECTED) {
					Toast.makeText(XmppConnectionActivity.this, R.string.net_device_disconnected, Toast.LENGTH_SHORT).show();					
				} else if (connected == NETWORK_STATUS_CONNECTION_FAILURE) {
					Toast.makeText(XmppConnectionActivity.this, R.string.net_device_connection_failure, Toast.LENGTH_SHORT).show();					
				}
				setProgressBarIndeterminateVisibility(false);
			}
		}
    };

    /****************************************************************
     * Helper methods
	
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
	    
    /**
     * This activity needs a broadcast receiver to updates from the {@link NetworkCommunicationManager}.
     */
    private void initBroadcastReceiver() {
    	intentFilter = new IntentFilter();
    	intentFilter.addAction(XmppConnectionActivity.NETWORK_CONNECTION_STATUS_UPDATE);
    	registerReceiver(broadcastReceiver, intentFilter);
    }
}