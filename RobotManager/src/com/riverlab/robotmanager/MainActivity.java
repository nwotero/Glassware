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
import com.riverlab.robotmanager.messages.MessageListActivity;
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
	private ConnectedThread mConnectedThread;
	private VoiceRecognitionThread mVoiceThread;
	
	//Handler constants and definition
	public static final int MESSAGE_LIST_MESSAGE = 0;
	public static final int IMAGE_MESSAGE = 1;
	public static final int VIDEO_MESSAGE = 2;
	public static final int CONNECTION_MESSAGE = 3;
	public static final int FOCUS_MESSAGE = 4;
	public static final int COMMAND_MESSAGE = 5;
	public static final int SHUTDOWN_MESSAGE = 6;
	public static final int ROBOT_LIST_MESSAGE = 7;
	public static final int NEW_MESSAGE_MESSAGE = 8;


	//This Handler handles messages sent from the ConnectedThread for received bluetooth messages
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_LIST_MESSAGE:
				launchMessageListActivity();
				break;
			case CONNECTION_MESSAGE:
				String text = (String)msg.obj;
				if (text.equals("connected"))
				{
					imageView.setImageResource(R.drawable.ic_bluetooth_on_big);
				}
				else if (text.equals("disconnected"))
				{
					imageView.setImageResource(R.drawable.not_connected_cross);
				}
				break;
			case FOCUS_MESSAGE:
				String focus = (String)msg.obj;
				setFocusView(focus);
				break;
			case COMMAND_MESSAGE:
				String cmdText = (String)msg.obj;
				setCommandView(cmdText);
				break;
			case SHUTDOWN_MESSAGE:
				shutdown();
				break;
			case ROBOT_LIST_MESSAGE:
				launchRobotListActivity();
				break;
			case NEW_MESSAGE_MESSAGE:
				updateMessageView(1);
			}
		}
	};

	//Timeline global variables
	private RobotManagerApplication mApplication;
	private RemoteViews aRV;
	private LiveCard mLiveCard;
	private static final String LIVE_CARD_ID = "robot_manager";

	//GUI global variables
	private ImageView imageView;
	private GestureDetector mGestureDetector;
	private TextView commandView;
	private TextView focusView;
	private TextView messageView;
	private int numMsgs = 0;
	private int numNewMsgs = 0;
	private boolean msgFlag = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

		//android.os.Debug.waitForDebugger();
		
		mGestureDetector = createGestureDetector(this);

		setContentView(R.layout.activity_main);
		imageView = (ImageView) findViewById(R.id.imageView);
		messageView = (TextView) findViewById(R.id.messageView);
		commandView = (TextView)findViewById(R.id.commandView);
		focusView = (TextView)findViewById(R.id.focusView);
		mApplication = ((RobotManagerApplication) this.getApplication());
		
		imageView.setImageResource(R.drawable.not_connected_cross);
		
		mConnectedThread = new ConnectedThread(mHandler, mApplication);
		mVoiceThread = new VoiceRecognitionThread(mApplication, this);

		mConnectedThread.start();
		mVoiceThread.start();
		
		while(!mConnectedThread.isReady() && !mVoiceThread.isReady());
		
		mApplication.setMainThreadHandler(mHandler);
		mApplication.setConnectedThreadHandler(mConnectedThread.getHandler());
		mApplication.setVoiceThreadHandler(mVoiceThread.getHandler());
		
		mConnectedThread.setHandlers(mHandler, mVoiceThread.getHandler());
		mVoiceThread.setHandlers(mHandler, mConnectedThread.getHandler());
		
		Thread.currentThread().setName("Main Activity Thread");
		mConnectedThread.setName("Connected Thread");
		mVoiceThread.setName("Voice Recognition Thread");
	}

	@Override
	protected void onResume() {
		super.onResume();
		//mApplication.getVoiceThread().setListeningStatus(true);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		Handler voiceHandler = mApplication.getVoiceThreadHandler();
		
		Message msg = voiceHandler.obtainMessage(
				VoiceRecognitionThread.CONTEXT_MESSAGE, 
				this);
		voiceHandler.sendMessage(msg);
		
		msg = voiceHandler.obtainMessage(
				VoiceRecognitionThread.ENABLE_SYSTEM_CMD_MESSAGE, 
				true);
		voiceHandler.sendMessage(msg);
		
		msg = voiceHandler.obtainMessage(
				VoiceRecognitionThread.CHANGE_VOCAB_MESSAGE,
				new ArrayList<String>());
		voiceHandler.sendMessage(msg);
		
		msg = voiceHandler.obtainMessage(
				VoiceRecognitionThread.LISTENING_MESSAGE, 
				true);
		voiceHandler.sendMessage(msg);
		
		if (msgFlag)
		{
			numNewMsgs = 0;
			msgFlag = false;
			updateMessageView(0);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Handler voiceHandler = mApplication.getVoiceThreadHandler();
		if (voiceHandler != null)
		{
			Message msg = voiceHandler.obtainMessage(VoiceRecognitionThread.LISTENING_MESSAGE, false);
			voiceHandler.sendMessageAtFrontOfQueue(msg);
		}
		
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
		case R.id.viewMessages:
			launchMessageListActivity();
			return true;
		case R.id.close:
			mApplication.onShutdown();
			return true;
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
	
	public void setFocusView(String robotName)
	{
		focusView.setText("Focus: " + robotName);
	}
	
	public void updateMessageView(int change) 
	{
		numMsgs += change;
		numNewMsgs += change;
		
		messageView.setText("\u2709\t" + numMsgs + "\tNew: " + numNewMsgs);
	}
	
	public void launchBluetoothListActivity()
	{
		startActivity(new Intent(this, BluetoothDevicesListActivity.class));
	}
	
	public void launchRobotListActivity()
	{
		startActivity(new Intent(this, ViewRobotListActivity.class));
	}
	
	public void launchMessageListActivity()
	{
		msgFlag = true;
		startActivity(new Intent(this, MessageListActivity.class));
	}
	
	public void shutdown()
	{
		//Make sure all threads are done before shutting down
		while (mApplication.getConnectedThreadHandler() != null &&
				mApplication.getVoiceThreadHandler() != null);
		mConnectedThread.interrupt();
		mVoiceThread.interrupt();
		mConnectedThread = null;
		mVoiceThread = null;
		
		finish();            	
	}
}