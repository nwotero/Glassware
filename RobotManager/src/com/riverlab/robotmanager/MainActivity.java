package com.riverlab.robotmanager;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.glass.input.VoiceInputHelper;
import com.google.glass.input.VoiceListener;
import com.google.glass.logging.FormattingLogger;
import com.google.glass.logging.FormattingLoggers;
import com.google.glass.logging.Log;
import com.google.glass.voice.VoiceCommand;
import com.google.glass.voice.VoiceConfig;

import java.util.ArrayList;
import java.util.Set;

/*This is the Main Activity for the Robot Manager App.  It displays the
 * main GUI and also continuously listens for user commands. 
 * 
 */

public class MainActivity extends Activity {

	//Bluetooth global variables
	private BluetoothAdapter mBluetoothAdapter;
	private ConnectedThread mConnectedThread;
	private boolean isConnected = false;

	public static final int REQUEST_ROBOT_INFO = 0; //Glass asks server for info
	public static final int ROBOT_INFO_UPDATE = 1;  //Server pushes robot info
	public static final int SEND_COMMAND = 2;		//Glass is sending a command to server
	public static final int RECEIVE_COMMAND = 3;	//Server pushes command to Glass
	public static final int NOTIFICATION = 4;		//Server pushes notification to Glass
	public static final int CONFIRMATION = 5;

	//This Handler handles messages sent from the ConnectedThread for received bluetooth messages
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case REQUEST_ROBOT_INFO:
				break;
			case ROBOT_INFO_UPDATE:
				break;
			case SEND_COMMAND:
				break;
			case RECEIVE_COMMAND:
				break;
			case NOTIFICATION:
				break;
			case CONFIRMATION:
				break;
			}
		}
	};

	
	//Timeline global variables
	private RobotManagerApplication mApplication;
	private RemoteViews aRV;
	private LiveCard mLiveCard;
	private static final String LIVE_CARD_ID = "robot_manager";

	//GUI global variables
	private TextView connectionStatusTextView;
	private ImageView imageView;
	private GestureDetector mGestureDetector;
	private TextView commandView;

	//Global variables for continuous listening
	private VoiceInputHelper mVoiceInputHelper;
	private VoiceConfig mVoiceConfig;

	//This class is used to continuously listen for user input 
	public class MyVoiceListener implements VoiceListener {
		protected final VoiceConfig voiceConfig;

		public MyVoiceListener(VoiceConfig voiceConfig) {
			this.voiceConfig = voiceConfig;
		}

		@Override
		public void onVoiceServiceConnected() {
			mVoiceInputHelper.setVoiceConfig(mVoiceConfig, false);
		}

		@Override
		public void onVoiceServiceDisconnected() {

		}

		//This method handles recognized voice commands
		@Override
		public VoiceConfig onVoiceCommand(VoiceCommand vc) {
			String recognizedStr = vc.getLiteral();
			Log.i("VoiceActivity", "Recognized text: "+recognizedStr);

			//Change the GUI to show the recognized command
			commandView.setText("Command: " + recognizedStr);

			//Check to see if the command is to connect to a bluetooth device
			if (recognizedStr.startsWith("connect to"))
			{
				if (!isConnected) //If already connected do not connect
				{
					for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices())
					{
						if (device.getName().equals(recognizedStr.substring(11)))
						{
							BluetoothSocket socket = mApplication.connectToDevice(device);
							mConnectedThread = new ConnectedThread(socket, mHandler, mApplication);
							connectionStatusTextView.setText(R.string.connected);
							imageView.setImageResource(R.drawable.ic_bluetooth_on_big);
							isConnected = true;
							return voiceConfig;
						}
					}
				}
			}
			else if (recognizedStr.equals("end connection"))
			{
				if (mConnectedThread != null)
				{
					mConnectedThread.write("end connection\n".getBytes());
					isConnected = false;
					connectionStatusTextView.setText(R.string.not_connected);
					imageView.setImageResource(R.drawable.ic_bluetooth_off_big);
					mConnectedThread = null;
				}
			}
			else if (mConnectedThread != null)
			{
				mConnectedThread.write(recognizedStr.getBytes());
			}

			return voiceConfig;
		}

		@Override
		public FormattingLogger getLogger() {
			return FormattingLoggers.getContextLogger();
		}

		@Override
		public boolean isRunning() {
			return true;
		}

		@Override
		public boolean onResampledAudioData(byte[] arg0, int arg1, int arg2) {
			return false;
		}

		//This may be considered in order to differentiate commands from normal
		//speech
		@Override
		public boolean onVoiceAmplitudeChanged(double arg0) {
			return false;
		}

		@Override
		public void onVoiceConfigChanged(VoiceConfig arg0, boolean arg1) {

		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mGestureDetector = createGestureDetector(this);

		setContentView(R.layout.activity_main);
		connectionStatusTextView = (TextView) findViewById(R.id.connectionStatus);
		imageView = (ImageView) findViewById(R.id.imageView);
		commandView = (TextView)findViewById(R.id.commandView);
		mApplication = ((RobotManagerApplication) this.getApplication());
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		//Pre-set commands
		String[] commands = {//"straight", "forward", 
				//"turn left", "left", 
				//"turn right", "right", 
				//"stop", "back", 
				"drive forward", "drive backward", "turn left", "turn right",
				"look up", "look down", "look left", "look right",
				"move arm up", "move arm down", "move arm left", "move arm right",
				"open scoop", "close scoop", "again",
				"end connection"};
		ArrayList<String> commandList = new ArrayList<String>();

		for (String command : commands)
		{
			commandList.add(command);
		}

		//Prefix all bluetooth device names with "connect to " to allow the
		//user to connect through voice control
		for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices())
		{
			commandList.add("connect to " + device.getName());
		}

		//Set up command listener
		mVoiceConfig = new VoiceConfig("MyVoiceConfig", commandList.toArray(new String[commandList.size()]));
		mVoiceInputHelper = new VoiceInputHelper(this, new MyVoiceListener(mVoiceConfig),
				VoiceInputHelper.newUserActivityObserver(this));
	}

	@Override
	protected void onResume() {
		super.onResume();
		imageView.setImageResource(R.drawable.ic_bluetooth_off_big);
		mVoiceInputHelper.addVoiceServiceListener();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mVoiceInputHelper.removeVoiceServiceListener();
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection.
		switch (item.getItemId()) {
		case R.id.viewDevices:
			startActivityForResult(new Intent(this, BluetoothDevicesListActivity.class), 1);
			return true;
		case R.id.viewRobots:
			startActivity(new Intent(this, ViewRobotListActivity.class));
			return true;
		case R.id.close:
			if (mConnectedThread != null)
			{
				mConnectedThread.write("end connection\n".getBytes());
			}
			finish();            	
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == 1) {

			if(resultCode == RESULT_OK){      
				String result = data.getStringExtra("result");

				if (result.equals("Success"))
				{
					connectionStatusTextView.setText(R.string.connected);
					imageView.setImageResource(R.drawable.ic_bluetooth_on_big);
					BluetoothSocket socket = ((RobotManagerApplication)this.getApplication()).getBluetoothSocket();
					mConnectedThread = new ConnectedThread(socket, mHandler, mApplication);
					isConnected = true;
				}
				else
				{
					connectionStatusTextView.setText(R.string.not_connected);
				}
			}
		}
	}

	//Listens for the user to tap on the Card.
	private GestureDetector createGestureDetector(Context context) {
		GestureDetector gestureDetector = new GestureDetector(context);
		//Create a base listener for generic gestures
		gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
			@Override
			public boolean onGesture(Gesture gesture) {
				if (gesture == Gesture.TAP) {
					AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
					audio.playSoundEffect(Sounds.TAP);
					openOptionsMenu();
					return true;
				}
				return false;
			}
		});
		return gestureDetector;
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (mGestureDetector != null) {
			return mGestureDetector.onMotionEvent(event);
		}
		return false;
	}
}