package com.dennis.vitalsigns;



import java.util.ArrayList;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class DeviceScanActivity extends Activity {

	private LeDeviceListAdapter mLeDeviceListAdapter;
	private BluetoothAdapter mBluetoothAdapter;
	private boolean mScanning;
	private Handler mHandler;
	private Handler handlerDeviceScanResult;
	private Button buttonScan;
	private TextView textViewDeviceSelected;
	private TextView textViewDeviceListMessage;
	private ListView listviewHearRateMonitors;
	private BlueToothMethods mBlueToothMethods;
	private CommonMethods mCommonMethods;

	private ProgressDialog progress;


	private static final int REQUEST_ENABLE_BT = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device_scan);
		initializeVariables();
	}
	private void initializeVariables(){

		mHandler = new Handler();
		buttonScan = (Button) findViewById(R.id.buttonScan);
		textViewDeviceSelected=(TextView) findViewById(R.id.textViewDeviceSelected);
		textViewDeviceListMessage=(TextView) findViewById(R.id.textViewDeviceListMessage);
		progress = new ProgressDialog(this);
		listviewHearRateMonitors = (ListView) findViewById(R.id.listviewHearRateMonitors);
		mBlueToothMethods= new BlueToothMethods(DeviceScanActivity.this);
		mCommonMethods = new CommonMethods(this);
		handlerDeviceScanResult = new Handler(){
			@Override
			public void handleMessage(Message msg){
				if(msg.what == 0){
					// this entire text must be placed in R.strings
					//mBlueToothMethods.getHeartRateDeviceName()
					textViewDeviceListMessage.setText(R.string.bluetooth_not_found); 
				}else{
					textViewDeviceListMessage.setText("Please select a device (by touching the device name) from the list below.");
				}
				 
			}
		};

		buttonScan.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				progress.setMessage((DeviceScanActivity.this).getString(R.string.scanning_heart_device));
				progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				progress.setIndeterminate(true);
				CommonMethods.Log("about to show progress bar");
				progress.show();
				
				Thread t = new Thread() {
					@Override
					public void run(){
						startScan();
					}   
				};
				t.start();

			}
		});
		
		// Initializes list view adapter.
		mLeDeviceListAdapter = new LeDeviceListAdapter();

		


		listviewHearRateMonitors.setAdapter(mLeDeviceListAdapter);

		listviewHearRateMonitors.setOnItemClickListener(
				new AdapterView.OnItemClickListener() {   	
					@Override
					public void onItemClick(AdapterView<?> parent, final View view,
							int position, long id) {

						final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
						if (device == null) {
							return;
						}

						//put device into storage I guess
						textViewDeviceSelected.setText("You have selected the device: " + device.getName() + "  " + device.getAddress());

						
						mBlueToothMethods.setHeartRateDevice(device.getName(), device.getAddress());		            

						if (mScanning) {
							mBluetoothAdapter.stopLeScan(mLeScanCallback);
							mScanning = false;
						}           

					}
				});//end of setOnItemClickListener
		
	}

	private void startScan(){

		// Use this check to determine whether BLE is supported on the device.  Then you can
		// selectively disable BLE-related features.
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			mCommonMethods.showAlertDialogOnUiThread("Error bluetooth not supported");
			progress.dismiss();
			return;
		}

		// Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
		// BluetoothAdapter through BluetoothManager.
		final BluetoothManager bluetoothManager =
				(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		// Checks if Bluetooth is supported on the device.
		if (mBluetoothAdapter == null) {
			mCommonMethods.showAlertDialogOnUiThread("Error bluetooth not supported");
			progress.dismiss();

			return;
		}else{
			CommonMethods.Log("BlueTooth supported!");
		}

		mLeDeviceListAdapter.clear();


		// Stops scanning after a pre-defined scan period.
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				
				
				mScanning = false;
				mBluetoothAdapter.stopLeScan(mLeScanCallback);               
						if(mLeDeviceListAdapter.getCount()>0){
							handlerDeviceScanResult.sendEmptyMessage(1);
						}else{
							handlerDeviceScanResult.sendEmptyMessage(0);
						}     

						progress.dismiss();
				
			}// end of outer run
		}, Preferences.deviceScanTime*1000);

		mScanning = true;
		mBluetoothAdapter.startLeScan(mLeScanCallback);

	}

	private void stopScan(){
		mScanning = false;
		mBluetoothAdapter.stopLeScan(mLeScanCallback);

	}

	@Override
	protected void onPause() {
		super.onPause();
		stopScan();
		mLeDeviceListAdapter.clear();
		progress.dismiss();
	}

	// Adapter for holding devices found through scanning.
	private class LeDeviceListAdapter extends BaseAdapter {
		private ArrayList<BluetoothDevice> mLeDevices;
		private LayoutInflater mInflator;

		public LeDeviceListAdapter() {
			super();
			mLeDevices = new ArrayList<BluetoothDevice>();
			mInflator = DeviceScanActivity.this.getLayoutInflater();
		}

		public void addDevice(BluetoothDevice device) {
			if(!mLeDevices.contains(device)) {
				mLeDevices.add(device);
			}
		}

		public BluetoothDevice getDevice(int position) {
			return mLeDevices.get(position);
		}

		public void clear() {
			mLeDevices.clear();
		}

		@Override
		public int getCount() {
			return mLeDevices.size();
		}

		@Override
		public Object getItem(int i) {
			return mLeDevices.get(i);
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			ViewHolder viewHolder;
			// General ListView optimization code.
			if (view == null) {
				view = mInflator.inflate(R.layout.listitem_device, null);
				viewHolder = new ViewHolder();
				viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
				viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
				view.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) view.getTag();
			}

			BluetoothDevice device = mLeDevices.get(i);
			final String deviceName = device.getName();
			if (deviceName != null && deviceName.length() > 0)
				viewHolder.deviceName.setText(deviceName);
			else
				viewHolder.deviceName.setText("Unknown device");
			viewHolder.deviceAddress.setText(device.getAddress());

			return view;
		}
	}


	private BluetoothAdapter.LeScanCallback mLeScanCallback =
			new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			CommonMethods.Log("A blue tooth LE device found.");
			runOnUiThread(new Runnable() {
				@Override
				public void run() {

					mLeDeviceListAdapter.addDevice(device);
					mLeDeviceListAdapter.notifyDataSetChanged();

				}
			});
		}
	};


	static class ViewHolder {
		TextView deviceName;
		TextView deviceAddress;
	}

	
	public void showMessageOnUIThread(final String mess, final Context context){
		runOnUiThread(new Runnable() {
			@Override
			public void run() {

		AlertDialog.Builder alertDialog = new AlertDialog.Builder(context); 
		alertDialog.setMessage(mess);	      	
		alertDialog
		.setIcon(0)
		.setTitle("")
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				return; //don't do anything.
			}
		})
		.create();
		alertDialog.show();	
		//return alertDialog;
		
			}
		});
	}

}
