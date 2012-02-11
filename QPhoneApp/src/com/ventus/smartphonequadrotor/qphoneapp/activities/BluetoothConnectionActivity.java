package com.ventus.smartphonequadrotor.qphoneapp.activities;

import java.util.Set;

import com.ventus.smartphonequadrotor.qphoneapp.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class BluetoothConnectionActivity extends Activity {
	public static final String TAG = BluetoothConnectionActivity.class.getName();
	private BluetoothAdapter bluetoothAdapter;
	private ArrayAdapter<String> pairedDevicesArrayAdapter;
	private ArrayAdapter<String> newDevicesArrayAdapter;
	public static final int REQUEST_INTENT_BT = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bluetooth_setup);
        
        //initialize list adapters
        initListAdapters();
        
		//get the bluetooth adapter
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null){
			//device does not support bluetooth. Die, die, die...
			finish();
		} else {
			if (!bluetoothAdapter.isEnabled()){
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			    startActivityForResult(enableBtIntent, REQUEST_INTENT_BT);
			} else {
				populatePairedDevicesList();
			}
		}
	}

	/*
	 ************************************************************************
	 * HELPER METHODS
	 */
	
	/**
	 * This method initializes the 2 list adapters: pairedDevicesArrayAdapter and newDevicesArrayAdapter.
	 */
    private void initListAdapters(){
    	pairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.bluetooth_device_name);
        ListView pairedListView = (ListView)findViewById(R.id.pairedBtDevicesList);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(deviceSelectedListener);
        
        //TODO initialize newDevicesArrayAdapter
    }
    
    /**
     * This method listens to the "on-click" event of the list of devices and responds accordingly
     */
    private AdapterView.OnItemClickListener deviceSelectedListener = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> adapterView, View view, int arg2, long arg3) {
			String viewText = ((TextView)view).getText().toString();
			String address = viewText.split("\n")[1];
			
			//TODO - do whatever has to be done when a device is selected
		}
	};
    
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

}
