package com.riverlab.robotmanager.voice_recognition;

import java.util.ArrayList;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Parcelable;

import com.google.glass.input.VoiceInputHelper;
import com.google.glass.input.VoiceListener;
import com.google.glass.logging.FormattingLogger;
import com.google.glass.logging.FormattingLoggers;
import com.google.glass.logging.Log;
import com.google.glass.voice.VoiceCommand;
import com.google.glass.voice.VoiceConfig;
import com.riverlab.robotmanager.MainActivity;
import com.riverlab.robotmanager.RobotManagerApplication;
import com.riverlab.robotmanager.bluetooth.ConnectedThread;
import com.riverlab.robotmanager.robot.Robot;

public class VoiceRecognitionThread extends HandlerThread
{
	//Globals
	private RobotManagerApplication mApplication;

	private boolean isShutdown = false;
	private boolean isListening = true;
	private boolean isConnected = false;
	private boolean usingSystemCommands = true;
	private BluetoothAdapter mBluetoothAdapter;
	private Context mContext;

	//Continuous listening
	private VoiceInputHelper mVoiceInputHelper;
	private VoiceConfig mVoiceConfig;
	private ArrayList<String> mSystemCommands;
	private ArrayList<String> mVoiceConfigList;
	private String lastCommand = "";
	private long millisFromLastCommand;

	//Thread globals
	private VoiceHelperThread mHelper = null;
	private Handler mHelperHandler = null;

	public static final int ADD_VOCAB_MESSAGE = 0;
	public static final int REMOVE_VOCAB_MESSAGE = 1;
	public static final int CHANGE_VOCAB_MESSAGE = 2;
	public static final int RESET_MESSAGE = 3;
	public static final int LISTENING_MESSAGE = 4;
	public static final int SHUTDOWN_MESSAGE = 5;

	//Handlers
	private Handler mainHandler;
	private Handler connectedHandler;
	private final Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case ADD_VOCAB_MESSAGE:
				String addition = (String)msg.obj;
				addToVocab(addition);
				break;
			case REMOVE_VOCAB_MESSAGE:
				String deletion = (String)msg.obj;
				removeFromVocab(deletion);
				break;
			case CHANGE_VOCAB_MESSAGE:
				ArrayList<String> newVocab = (ArrayList<String>)msg.obj;
				changeVocab(newVocab);
				break;
			case RESET_MESSAGE:
				String resetMessage = (String)msg.obj;
				reset(resetMessage);
				break;
			case LISTENING_MESSAGE:
				boolean isListening = (Boolean) msg.obj;
				VoiceRecognitionThread.this.isListening = isListening;
			case SHUTDOWN_MESSAGE:
				shutdown();
				break;
			}

		}
	};

	public VoiceRecognitionThread(RobotManagerApplication app, Context context)
	{
		super("Voice Recognition Thread");
		this.mApplication  = app;
		this.mContext = context;

		mSystemCommands = new ArrayList<String>();

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		//Prefix all bluetooth device names with "connect to " to allow the
		//user to connect through voice control
		for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices())
		{
			mSystemCommands.add("Connect to " + device.getName());
		}

		mSystemCommands.add("All");
		mSystemCommands.addAll(app.getRobotNames());

		//Hard coded system commands
		mSystemCommands.add("Start listening");
		mSystemCommands.add("Stop listening");
		mSystemCommands.add("End connection");
		mSystemCommands.add("Close robot manager");
		mSystemCommands.add("View robots");
		mSystemCommands.add("Create group");
		mSystemCommands.add("View messages");
		mSystemCommands.add("View map");

		mVoiceConfigList = new ArrayList<String>(mSystemCommands);

		//Set up command listener
		mVoiceConfig = new VoiceConfig("MyVoiceConfig", mSystemCommands.toArray(new String[mSystemCommands.size()]));
		mVoiceInputHelper = new VoiceInputHelper(mContext, new MyVoiceListener(mVoiceConfig),
				VoiceInputHelper.newUserActivityObserver(mContext));

		mVoiceInputHelper.addVoiceServiceListener();
	}

	public void setHandlers(Handler mainHandler, Handler connectedHandler)
	{
		this.mainHandler = mainHandler;
		this.connectedHandler = connectedHandler;
	}

	public Handler getHandler()
	{
		return mHandler;
	}

	public void setListeningStatus(boolean isListening)
	{
		this.isListening = isListening;
	}

	public ArrayList<String> getDefaultRobotCommands()
	{
		ArrayList<String> newVocab = new ArrayList<String>();

		Robot robot = mApplication.getRobotInFocus();
		if (robot == null)
		{
			for (Robot rbt : mApplication.getRobots())
			{
				newVocab.addAll(rbt.getNextPhrases(new ArrayList<String>()).newVocab);
			}
			return newVocab;
		}
		else
		{
			NewPhrasesMessage msg = robot.getNextPhrases(new ArrayList<String>());
			return msg.newVocab;
		}
	}

	public void addToVocab(String newVoiceCommand)
	{
		Log.d("VoiceThread", "Adding " + newVoiceCommand + " to vocab");

		mVoiceConfigList.add(newVoiceCommand);

		Log.d("VoiceThread", "Vocab: " + mVoiceConfigList.toString());


		mVoiceConfig = new VoiceConfig("MyVoiceConfig", mVoiceConfigList.toArray(new String[mVoiceConfigList.size()]));
		mVoiceInputHelper = new VoiceInputHelper(mContext, new MyVoiceListener(mVoiceConfig),
				VoiceInputHelper.newUserActivityObserver(mContext));

		mVoiceInputHelper.addVoiceServiceListener();
	}

	public void removeFromVocab(String vcToRemove)
	{
		mVoiceConfigList.remove(vcToRemove);

		mVoiceConfig = new VoiceConfig("MyVoiceConfig", mVoiceConfigList.toArray(new String[mVoiceConfigList.size()]));
		mVoiceInputHelper = new VoiceInputHelper(mContext, new MyVoiceListener(mVoiceConfig),
				VoiceInputHelper.newUserActivityObserver(mContext));

		mVoiceInputHelper.addVoiceServiceListener();
	}

	public void changeVocab(ArrayList<String> newVocab)
	{
		mVoiceConfigList = new ArrayList<String>();

		mVoiceConfigList.addAll(newVocab);
		if (usingSystemCommands)
		{
			mVoiceConfigList.addAll(mSystemCommands);
			mVoiceConfigList.addAll(mApplication.getRobotNames());
		}

		mVoiceConfig = new VoiceConfig("MyVoiceConfig", mVoiceConfigList.toArray(new String[mVoiceConfigList.size()]));
		mVoiceInputHelper = new VoiceInputHelper(mContext, new MyVoiceListener(mVoiceConfig),
				VoiceInputHelper.newUserActivityObserver(mContext));

		mVoiceInputHelper.addVoiceServiceListener();

		Log.d("VoiceRecognitionThread", "New listening list: " + mVoiceConfigList.toString());
	}

	public void reset(String expression)
	{
		Message msg = connectedHandler.obtainMessage(ConnectedThread.WRITE_MESSAGE, expression);
		connectedHandler.sendMessageAtFrontOfQueue(msg);

		mHelper = null;
		usingSystemCommands = true;
		changeVocab(getDefaultRobotCommands());
	}


	public void run()
	{
		while (!isShutdown);
	}

	private void sendVoiceCommand(String command)
	{
		Message msg = connectedHandler.obtainMessage(ConnectedThread.WRITE_MESSAGE, command);
		connectedHandler.sendMessageAtFrontOfQueue(msg);
	}

	public void shutdown()
	{
		isShutdown = true;
	}


	//This class is used to continuously listen for user input 
	public class MyVoiceListener implements VoiceListener {
		protected final VoiceConfig voiceConfig;

		public MyVoiceListener(VoiceConfig voiceConfig) {
			this.voiceConfig = voiceConfig;
		}

		@Override
		public void onVoiceServiceConnected() {
			mVoiceInputHelper.setVoiceConfig(mVoiceConfig);
		}

		@Override
		public void onVoiceServiceDisconnected() {	

		}

		//This method handles recognized voice commands
		@Override
		public VoiceConfig onVoiceCommand(VoiceCommand vc) {
			String recognizedStr = vc.getLiteral();
			
			//Eliminate voice recognition bounce
			if (recognizedStr.equals(lastCommand) && (System.currentTimeMillis() - millisFromLastCommand < 1000))
			{
				Log.i("VoiceRecognitionThread", "Voice recognition debounced");
				millisFromLastCommand = System.currentTimeMillis();
				return null;
			}
			lastCommand = recognizedStr;
			millisFromLastCommand = System.currentTimeMillis();
			
			Log.i("VoiceRecognitionThread", "Recognized text: "+recognizedStr);

			if (isListening)
			{				
				//Check to see if it is a robot focus
				if (recognizedStr.equals("All"))
				{
					Log.d("VoiceRecognitionThread", "Setting focus on All");
					//focus on all robots
					mApplication.setRobotInFocus("All");
					
					Message msg = mainHandler.obtainMessage(MainActivity.FOCUS_MESSAGE, "All");
					mainHandler.sendMessageAtFrontOfQueue(msg);
					
					changeVocab(getDefaultRobotCommands());
				}

				else if (mApplication.getRobotNames().contains(recognizedStr))
				{
					Log.d("VoiceRecognitionThread", "Setting focus on: " + recognizedStr);
					//Set focus on robot whos name was recognized
					mApplication.setRobotInFocus(recognizedStr);
					

					Message msg = mainHandler.obtainMessage(MainActivity.FOCUS_MESSAGE, recognizedStr);
					mainHandler.sendMessageAtFrontOfQueue(msg);
					
					changeVocab(getDefaultRobotCommands());
				}
				else
				{
					//Print Command to screen
					Message msg = mainHandler.obtainMessage(MainActivity.COMMAND_MESSAGE, recognizedStr);
					mainHandler.sendMessageAtFrontOfQueue(msg);

					//Check to see if it is a system command.
					if (recognizedStr.startsWith("Connect to"))
					{
						if (!isConnected) //If already connected do not connect
						{
							Message msgConnected = connectedHandler.obtainMessage(ConnectedThread.CONNECT_MESSAGE, recognizedStr.substring(11));
							connectedHandler.sendMessageAtFrontOfQueue(msgConnected);
						}
					}
					else if (recognizedStr.equals("End connection"))
					{
						Message msgConnected = connectedHandler.obtainMessage();
						msgConnected.what = ConnectedThread.DISCONNECT_MESSAGE;
						connectedHandler.sendMessageAtFrontOfQueue(msg);
					}

					else if (recognizedStr.equals("Close robot manager"))
					{
						mApplication.onShutdown();
					}

					else if (recognizedStr.equals("Stop listening"))
					{
						setListeningStatus(false);
					}

					else if (recognizedStr.equals("View robots"))
					{
						Log.i("VoiceRecognitionThread", "Requesting launch of RobotListActivity");
						Message msgView = mainHandler.obtainMessage();
						msgView.what = MainActivity.ROBOT_LIST_MESSAGE;
						mainHandler.sendMessage(msgView);
					}

					else if (recognizedStr.equals("Create group"))
					{

					}

					else if (recognizedStr.equals("View messages"))
					{

					}

					else if (recognizedStr.equals("View map"))
					{

					}
					//If the recognized string is not a system command, it must be a robot command
					else if (!recognizedStr.equals("Start listening"))
					{
						if (mHelper == 	null)
						{
							Robot focus = mApplication.getRobotInFocus();
							if (focus == null)
							{
								mHelper = new VoiceHelperThread(mApplication, recognizedStr, 
										"All", mHandler);
							}
							else
							{
								mHelper = new VoiceHelperThread(mApplication, recognizedStr, 
										focus.getName(), mHandler);
							}
							mHelperHandler = mHelper.getHandler();
							mHelper.start();

							usingSystemCommands = false;
						}
						else
						{
							Message newPhraseMsg = mHelperHandler.obtainMessage(VoiceHelperThread.RECEIVE_PHRASE_MESSAGE, recognizedStr);
							mHelperHandler.sendMessageAtFrontOfQueue(newPhraseMsg);
						}
					}
				}
			}
			else if (recognizedStr.equals("Start listening"))
			{
				Message msgShow = mainHandler.obtainMessage(MainActivity.COMMAND_MESSAGE, recognizedStr);
				mainHandler.sendMessageAtFrontOfQueue(msgShow);
				setListeningStatus(true);
			}

			return null;
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
		public void onVoiceConfigChanged(VoiceConfig arg0, boolean arg1) 
		{
		}
	}
}
