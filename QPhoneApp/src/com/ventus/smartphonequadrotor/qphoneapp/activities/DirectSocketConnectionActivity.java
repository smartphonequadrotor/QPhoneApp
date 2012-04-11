package com.ventus.smartphonequadrotor.qphoneapp.activities;

import com.ventus.smartphonequadrotor.qphoneapp.R;
import com.ventus.smartphonequadrotor.qphoneapp.activities.XmppConnectionActivity.ConnectBtn_OnClickListener;
import com.ventus.smartphonequadrotor.qphoneapp.activities.XmppConnectionActivity.NextBtn_OnClickListener;
import com.ventus.smartphonequadrotor.qphoneapp.activities.XmppConnectionActivity.connectionTypeOnItemSelectedListener;
import com.ventus.smartphonequadrotor.qphoneapp.services.intents.IntentHandler;
import com.ventus.smartphonequadrotor.qphoneapp.util.net.NetworkCommunicationManager;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class DirectSocketConnectionActivity extends Activity {
	public static final String TAG = DirectSocketConnectionActivity.class.getSimpleName();

	private static final String DIRECT_CONNECTION_STATUS_UPDATE = "com.ventus."
			+ "smartphonequadrotor.qphoneapp.activities.DirectSocketConnectionActivity." 
			+ "NETWORK_CONNECTION_STATUS_UPDATE";
	public static final String NETWORK_CONNECTION_STATUS = "NETWORK_CONNECTION_STATUS";
	public static final int NETWORK_STATUS_CONNECTED = 1;
	public static final int NETWORK_STATUS_DISCONNECTED = 0;
	public static final int NETWORK_STATUS_CONNECTION_FAILURE = -1;

	private IntentFilter intentFilter;
	private EditText serverAddress;
	private EditText serverPortNum;
	private Button connectBtn;
	private Button nextBtn;
	private Spinner connectionTypeSpinner;
	
	/** 
     * Called when the activity is first created. 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.direct_socket_setup);

        initBroadcastReceiver();
        
        serverAddress = (EditText)findViewById(R.id.serverAddressTxt);
        serverPortNum = (EditText)findViewById(R.id.serverPortNumberTxt);
        connectBtn = (Button)findViewById(R.id.netConnectBtn);
        nextBtn = (Button)findViewById(R.id.netNextBtn);
        connectionTypeSpinner = (Spinner) findViewById(R.id.commMethodSelectorSpinner);
        
        connectBtn.setOnClickListener(new ConnectBtn_OnClickListener());
        nextBtn.setOnClickListener(new NextBtn_OnClickListener());
        nextBtn.setEnabled(false);
        
        ArrayAdapter<CharSequence> connectionTypeArrayAdapter = ArrayAdapter.createFromResource(
                this, R.array.comm_methods_array, android.R.layout.simple_spinner_item);
        connectionTypeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        connectionTypeSpinner.setAdapter(connectionTypeArrayAdapter);
        connectionTypeSpinner.setOnItemSelectedListener(new connectionTypeOnItemSelectedListener());
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
    		Intent intent = new Intent(DirectSocketConnectionActivity.this, ReadyToFlyActivity.class);
    		startActivity(intent);
    	}
    }
    
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(DIRECT_CONNECTION_STATUS_UPDATE)) {
				int connected = intent.getIntExtra(NETWORK_CONNECTION_STATUS, NETWORK_STATUS_DISCONNECTED);
				if (connected == NETWORK_STATUS_CONNECTED) {
					nextBtn.setEnabled(true);
					Toast.makeText(DirectSocketConnectionActivity.this, R.string.net_device_connected, Toast.LENGTH_SHORT).show();
				} else if (connected == NETWORK_STATUS_DISCONNECTED) {
					Toast.makeText(DirectSocketConnectionActivity.this, R.string.net_device_disconnected, Toast.LENGTH_SHORT).show();					
				} else if (connected == NETWORK_STATUS_CONNECTION_FAILURE) {
					Toast.makeText(DirectSocketConnectionActivity.this, R.string.net_device_connection_failure, Toast.LENGTH_SHORT).show();					
				}
				setProgressBarIndeterminateVisibility(false);
			}
		}
    };
    
    public class connectionTypeOnItemSelectedListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			if (parent.getItemAtPosition(position).toString().equalsIgnoreCase("Direct Socket")) {
				//we are already in the correct view... do nothing
			} else if (parent.getItemAtPosition(position).toString().equalsIgnoreCase("XMPP")) {
				//we need to start a new activity
				Intent intent = new Intent(DirectSocketConnectionActivity.this, XmppConnectionActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		}
		public void onNothingSelected(AdapterView parent) {
			//oh well... not required to do anything
		}
    }

    /****************************************************************
     * Helper methods
	
	/**
	 * Creates an intent that contains all the data that is needed to successfully establish an XMPP connection.
	 * @return the intent created
	 */
	private Intent createConnectionIntent() {
		Intent intent = new Intent(IntentHandler.DIRECT_CONNECT_ACTION);
		intent.putExtra(IntentHandler.ActionExtras.SERVER_ADDRESS.extra, serverAddress.getText().toString());
		intent.putExtra(IntentHandler.ActionExtras.SERVER_PORT.extra, serverPortNum.getText().toString());
		return intent;
	}
	    
    /**
     * This activity needs a broadcast receiver to updates from the {@link NetworkCommunicationManager}.
     */
    private void initBroadcastReceiver() {
    	intentFilter = new IntentFilter();
    	intentFilter.addAction(DirectSocketConnectionActivity.DIRECT_CONNECTION_STATUS_UPDATE);
    	registerReceiver(broadcastReceiver, intentFilter);
    }
}
