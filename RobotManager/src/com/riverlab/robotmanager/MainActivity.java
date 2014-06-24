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
import android.os.Parcelable;
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
import com.riverlab.robotmanager.bluetooth.BluetoothDevicesListActivity;
import com.riverlab.robotmanager.bluetooth.ConnectedThread;
import com.riverlab.robotmanager.messages.MessageActivity;
import com.riverlab.robotmanager.robot.ViewRobotListActivity;
import com.riverlab.robotmanager.voice_recognition.VoiceRecognitionThread;

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

	public static final int TEXT_MESSAGE = 0;
	public static final int IMAGE_MESSAGE = 1;
	public static final int VIDEO_MESSAGE = 2;
	public static final int CONNECTION_MESSAGE = 3;
	public static final int FOCUS_MESSAGE = 4;
	public static final int COMMAND_MESSAGE = 5;


	//This Handler handles messages sent from the ConnectedThread for received bluetooth messages
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case TEXT_MESSAGE:
				Intent messageIntent = new Intent(MainActivity.this, MessageActivity.class);
				messageIntent.putExtra("Message", (Parcelable)msg.obj);
				startActivity(messageIntent);
				break;
			case CONNECTION_MESSAGE:
				String text = (String)msg.obj;
				if (text.equals("connected"))
				{
					connectionStatusTextView.setText(R.string.connected);
					imageView.setImageResource(R.drawable.ic_bluetooth_on_big);
				}
				else if (text.equals("disconnected"))
				{
					connectionStatusTextView.setText(R.string.not_connected);
					imageView.setImageResource(R.drawable.ic_bluetooth_off_big);
				}
				break;
			case FOCUS_MESSAGE:
				break;
			case COMMAND_MESSAGE:
				String cmdText = (String)msg.obj;
				
				//Change the GUI to show the recognized command
				commandView.setText("Command: " + cmdText);
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

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		mGestureDetector = createGestureDetector(this);

		setContentView(R.layout.activity_main);
		connectionStatusTextView = (TextView) findViewById(R.id.connectionStatus);
		imageView = (ImageView) findViewById(R.id.imageView);
		commandView = (TextView)findViewById(R.id.commandView);
		mApplication = ((RobotManagerApplication) this.getApplication());

		mApplication.setMainActivity(this);
		mApplication.setConnectedThread(new ConnectedThread(mHandler, mApplication));
		mApplication.setVoiceThread(new VoiceRecognitionThread(mApplication, this));
	}

	@Override
	protected void onResume() {
		super.onResume();
		imageView.setImageResource(R.drawable.ic_bluetooth_off_big);
		//mApplication.getVoiceThread().setListeningStatus(true);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mApplication.getVoiceThread().setListeningStatus(false);
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
			launchBluetoothListActivity();
			return true;
		case R.id.viewRobots:
			launchRobotListActivity();
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
	
	public void setCommandView(String cmdText)
	{
		commandView.setText("Command: " + cmdText);
	}
	
	public void launchBluetoothListActivity()
	{
		startActivity(new Intent(this, BluetoothDevicesListActivity.class));
	}
	
	public void launchRobotListActivity()
	{
		startActivity(new Intent(this, ViewRobotListActivity.class));
	}
}