package com.ventus.smartphonequadrotor.qphoneapp.activities;

import java.util.Set;

import com.ventus.smartphonequadrotor.qphoneapp.R;
import com.ventus.smartphonequadrotor.qphoneapp.services.MainService;
import com.ventus.smartphonequadrotor.qphoneapp.services.intents.IntentHandler;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This activity lets the user connect to a bluetooth device.
 * @author abhin
 *
 */
public class BluetoothConnectionActivity extends Activity {
	public static final String TAG = BluetoothConnectionActivity.class.getName();
	private BluetoothAdapter bluetoothAdapter;
	private ArrayAdapter<String> pairedDevicesArrayAdapter;
	private ArrayAdapter<String> newDevicesArrayAdapter;
	private IntentFilter intentFilter;
	
	private ListView pairedDevicesListView, newDevicesListView;
	private Button scanButton, nextButton;
	
	public static final int REQUEST_INTENT_BT = 1;
	public static final String BLUETOOTH_CONNECTION_STATUS_UPDATE = "com.ventus."
			+ "smartphonequadrotor.qphoneapp.activities.BluetoothConnectionActivity." 
			+ "BLUETOOTH_CONNECTION_STATUS_UPDATE";
	public static final String BLUETOOTH_CONNECTION_STATUS = "BLUETOOTH_CONNECTION_STATUS";
	public static final int BLUETOOTH_STATUS_CONNECTED = 1;
	public static final int BLUETOOTH_STATUS_DISCONNECTED = 0;
	public static final int BLUETOOTH_STATUS_CONNECTION_FAILURE = -1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.bluetooth_setup);
        
		initControls();
        initListAdapters();
        initBroadcastReceiver();
        
		//get the bluetooth adapter
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null){
			//device does not support bluetooth. Die, die, die...
			stopService(new Intent(this, MainService.class));
			finish();
		} else {
			if (!bluetoothAdapter.isEnabled()){
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			    startActivityForResult(enableBtIntent, REQUEST_INTENT_BT);
			} else {
				populatePairedDevicesList();
			}
		}
		
		//start the background service
		Intent mainServiceStarter = new Intent(this, MainService.class);
		startService(mainServiceStarter);
	}
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(broadcastReceiver);
		super.onDestroy();
	}

	/*
	 ************************************************************************
	 * HELPER METHODS
	 */

	/**
	 * This method grabs all the controls from the gui.
	 */
	private void initControls() {
        pairedDevicesListView = (ListView)findViewById(R.id.bt_paired_devices_list);
        newDevicesListView = (ListView)findViewById(R.id.bt_scanned_devices_list);
        scanButton = (Button)findViewById(R.id.btScanDevicesBtn);
        scanButton.setOnClickListener(scanButtonOnClickListener);
        nextButton = (Button)findViewById(R.id.btNextBtn);
        nextButton.setOnClickListener(nextButtonOnClickListener);
	}
	
	/**
	 * This method initializes the 2 list adapters: pairedDevicesArrayAdapter and newDevicesArrayAdapter.
	 */
    private void initListAdapters() {
    	pairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.bluetooth_device_name);
        pairedDevicesListView.setAdapter(pairedDevicesArrayAdapter);
        pairedDevicesListView.setOnItemClickListener(deviceSelectedListener);
        
        newDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.bluetooth_device_name);
        newDevicesListView.setAdapter(newDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(deviceSelectedListener);
    }
    
    /**
     * This activity needs a broadcast receiver to receive new devices when the bluetooth
     * scan is being performed.
     */
    private void initBroadcastReceiver() {
    	intentFilter = new IntentFilter();
    	intentFilter.addAction(BluetoothDevice.ACTION_FOUND);	//this is useful for bluetooth scan results
    	intentFilter.addAction(BluetoothConnectionActivity.BLUETOOTH_CONNECTION_STATUS_UPDATE);
    	intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
    	registerReceiver(broadcastReceiver, intentFilter);
    }
    
	/**
	 * This method is called in case the bluetooth device has been ON for a while and contains
	 * a list of devices pre-stored. This method then populates the pairedDevices list with these
	 * devices.
	 */
    private void populatePairedDevicesList(){
		Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
    	for (BluetoothDevice dev : pairedDevices){
    		pairedDevicesArrayAdapter.add(dev.getName() + "\n" + dev.getAddress());
    	}
    }

    /*
     *************************************************************
     * Event handlers
     */
    
    /**
     * If the user does not find the target device in the "paired devices list", then
     * he/she is going to press the scan button. This will then have to search for
     * other Bluetooth devices and add them to the UI.
     */
    private View.OnClickListener scanButtonOnClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			if (bluetoothAdapter.isDiscovering()){
				bluetoothAdapter.cancelDiscovery();
				Log.d(TAG, "Bluetooth adapter was already in discovery mode; restarting discovery");
			}
			if (!bluetoothAdapter.startDiscovery()) {
				//bluetooth discovery failed for some reason
				Log.d(TAG, "Bluetooth discovery failed!");
				Toast.makeText(BluetoothConnectionActivity.this, R.string.bt_discovery_failed, Toast.LENGTH_SHORT).show();
			} else {
				//the discovery for bluetooth devices has started
				scanButton.setEnabled(false);
				setProgressBarIndeterminateVisibility(true);
				findViewById(R.id.bt_scanned_devices).setVisibility(View.VISIBLE);
				newDevicesArrayAdapter.clear();
			}
		}
	};
	
	/**
	 * This method should start the network setting activity.
	 */
	private View.OnClickListener nextButtonOnClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			Intent xmppActIntent = new Intent(BluetoothConnectionActivity.this, XmppConnectionActivity.class);
			startActivity(xmppActIntent);
		}
	};
    
    /**
     * This method listens to the "on-click" event of the list of devices and responds accordingly
     */
    private AdapterView.OnItemClickListener deviceSelectedListener = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> adapterView, View view, int arg2, long arg3) {
			String viewText = ((TextView)view).getText().toString();
			String address = viewText.split("\n")[1];
			
			//if there is an ongoing discovery, then it should be ended
			if (bluetoothAdapter.isDiscovering()) {
				bluetoothAdapter.cancelDiscovery();
				scanButton.setEnabled(true);
			}
			setProgressBarIndeterminateVisibility(true);
			
			//at this point, the device is ready to establish a connection with the 
			//QCB. But this connection will not be established in this activity. It
			//will be done by the BluetoothManager
			Intent btConnectIntent = new Intent(IntentHandler.BLUETOOTH_CONNECT_ACTION);
			btConnectIntent.putExtra(IntentHandler.ActionExtras.BLUETOOTH_ADDRESS.extra, address);
			sendBroadcast(btConnectIntent);
		}
	};

	/**
	 * This method will be called when an activity that was started for a result returns that
	 * result. The current events that this method will have to handle are:
	 * <ol>
	 * 	<li>When the "Switch On Bluetooth" activity returns a value.</li>
	 * </ol>
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_INTENT_BT:
			if (resultCode != RESULT_OK) {
				Log.d(TAG, "User decided to not enable Bluetooth");
				Toast.makeText(this, R.string.bt_disabled_app_shutdown, Toast.LENGTH_LONG).show();
				//shut down the application
				stopService(new Intent(this, MainService.class));
				finish();
			}
			break;
		default:
			Log.d(TAG, "Unknown activity returned result");	
		}
	}
	
	/**
	 * List of broadcasts received by this receiver:
	 * <ol>
	 * 	<li> 
	 * 		{@link BluetoothDevice#ACTION_FOUND} - This leads to the bluetooth device 
	 * 		being added to the newDevicesArrayAdapter.
	 * 	</li>
	 * 	<li> 
	 * 		{@link BluetoothAdapter#ACTION_DISCOVERY_FINISHED} - This leads to the scan button
	 * 		being re-enabled and the indeterminate progress bar re-appearing. 
	 * 	</li>
	 * </ol>
	 */
	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(BluetoothDevice.ACTION_FOUND)) {
				//get the bluetooth device from the intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				//add the new bluetooth device to the list of scanned devices
				String newListItemStr = device.getName() + "\n" + device.getAddress();
				newDevicesArrayAdapter.remove(newListItemStr);	//just in case the item already exists in the list
				newDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
			} else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				scanButton.setEnabled(true);
				setProgressBarIndeterminateVisibility(false);
			} else if (action.equals(BLUETOOTH_CONNECTION_STATUS_UPDATE)) {
				int connected = intent.getIntExtra(BLUETOOTH_CONNECTION_STATUS, BLUETOOTH_STATUS_DISCONNECTED);
				if (connected == BLUETOOTH_STATUS_CONNECTED) {
					nextButton.setEnabled(true);
					Toast.makeText(BluetoothConnectionActivity.this, R.string.bt_device_connected, Toast.LENGTH_SHORT).show();
				} else if (connected == BLUETOOTH_STATUS_DISCONNECTED) {
					Toast.makeText(BluetoothConnectionActivity.this, R.string.bt_device_disconnected, Toast.LENGTH_SHORT).show();
				} else if (connected == BLUETOOTH_STATUS_CONNECTION_FAILURE) {
					Toast.makeText(BluetoothConnectionActivity.this, R.string.bt_device_connection_failure, Toast.LENGTH_SHORT).show();
				}
				setProgressBarIndeterminateVisibility(false);
			}
		}
	};
}
