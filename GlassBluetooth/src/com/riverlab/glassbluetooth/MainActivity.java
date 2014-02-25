package com.riverlab.glassbluetooth;

import com.riverlab.glassbluetooth.R;
import com.keyboardr.glassremote.client.RemoteMessenger;
import com.keyboardr.glassremote.client.RemoteMessengerService;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {

	private boolean mResumed;
	private GlassBluetoothService.MainBinder mService;
	private BluetoothService.RemoteConnectionBinder mBluetoothService;
	
	//Name is deceptive, this binder is for the voice control
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder binderService) {
			if(binderService instanceof GlassBluetoothService.MainBinder) {
				mService = (GlassBluetoothService.MainBinder) binderService;
				openOptionsMenu();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			// TODO Auto-generated method stub
		}
	};
	
	//Connection for the bluetooth binder
	private ServiceConnection mBluetoothConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder binderService) {
			if(binderService instanceof BluetoothService.RemoteConnectionBinder){
				Log.d("DEBUG", "Instance of True");
				mBluetoothService = (BluetoothService.RemoteConnectionBinder) binderService;
				openOptionsMenu();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			// TODO Auto-generated method stub
		}
	};

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(mService == null) {
			ComponentName speechService = startService(new Intent(this, GlassBluetoothService.class));
			bindService(new Intent(this, GlassBluetoothService.class), mConnection, 0);
		}
		if (mBluetoothService == null)
		{
			ComponentName bluetoothService = startService(new Intent(this, BluetoothService.class));
			bindService(new Intent(this, BluetoothService.class), mBluetoothConnection, 1);
		}

		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mResumed = true;
	}
	
	@Override
    protected void onPause() {
        super.onPause();
        mResumed = false;
    }
	
	@Override
    public void openOptionsMenu() {
        if (mResumed && mService != null) {
            super.openOptionsMenu();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	finish();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.read_connection_status:
            	mService.speakConnectionStatus();
            	//unbindService(mConnection);
            	finish();
                return true;
            case R.id.connect:
            	Intent intent = new Intent(this, ConnectTest.class);
            	startActivity(intent);
            	//mBluetoothService.requestConnect();
            	finish();
            	return true;
            case R.id.stop:
                stopService(new Intent(this, GlassBluetoothService.class));
                stopService(new Intent(this, BluetoothService.class));
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
        unbindService(mConnection);
        unbindService(mBluetoothConnection);
        finish();
    }
}
