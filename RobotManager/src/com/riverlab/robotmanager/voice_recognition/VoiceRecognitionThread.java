package com.riverlab.robotmanager.voice_recognition;

import java.util.ArrayList;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;

import com.google.glass.input.VoiceInputHelper;
import com.google.glass.input.VoiceListener;
import com.google.glass.logging.FormattingLogger;
import com.google.glass.logging.FormattingLoggers;
import com.google.glass.logging.Log;
import com.google.glass.voice.VoiceCommand;
import com.google.glass.voice.VoiceConfig;
import com.riverlab.robotmanager.RobotManagerApplication;
import com.riverlab.robotmanager.robot.Robot;

public class VoiceRecognitionThread extends Thread
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

	private VoiceHelperThread mHelper = null;

	public static final int CONNECTION_STATUS = 0;

	//Handlers
	private Handler mainHandler;
	private Handler connectedHandler;
	private final Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CONNECTION_STATUS:
				isConnected = (msg.arg1 == 1);
				break;
			}

		}
	};

	public VoiceRecognitionThread(RobotManagerApplication app, Context context)
	{
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
		mSystemCommands.add("View robots");
		mSystemCommands.add("Create group");
		mSystemCommands.add("View messages");
		mSystemCommands.add("View map");

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

	public void changeVocab(ArrayList<String> newVocab)
	{
		if (usingSystemCommands)
		{
			newVocab.addAll(mSystemCommands);
		}

		mVoiceConfig = new VoiceConfig("MyVoiceConfig", newVocab.toArray(new String[newVocab.size()]));
		mVoiceInputHelper = new VoiceInputHelper(mContext, new MyVoiceListener(mVoiceConfig),
				VoiceInputHelper.newUserActivityObserver(mContext));
	}

	public void reset(String expression)
	{
		mApplication.getConnectedThread().write(expression.getBytes());
		mHelper = null;
		usingSystemCommands = true;
		changeVocab(getDefaultRobotCommands());
	}


	public void run()
	{
		while (!isShutdown);
	}

	public void onRobotAddition(String robotName)
	{
		mSystemCommands.add(robotName);
	}

	public void onRobotRemoval(String robotName)
	{
		mSystemCommands.remove(robotName);
	}

	private void sendVoiceCommand(String command)
	{
		mApplication.getConnectedThread().write(command.getBytes());
	}


	//This class is used to continuously listen for user input 
	public class MyVoiceListener implements VoiceListener {
		protected final VoiceConfig voiceConfig;

		public MyVoiceListener(VoiceConfig voiceConfig) {
			this.voiceConfig = voiceConfig;
		}

		@Override
		public void onVoiceServiceConnected() {
			mVoiceInputHelper.setVoiceConfig(voiceConfig, false);
		}

		@Override
		public void onVoiceServiceDisconnected() {

		}

		//This method handles recognized voice commands
		@Override
		public VoiceConfig onVoiceCommand(VoiceCommand vc) {
			String recognizedStr = vc.getLiteral();
			Log.i("VoiceActivity", "Recognized text: "+recognizedStr);

			if (isListening)
			{				
				mApplication.getMainActivity().setCommandView(recognizedStr);
				//Check to see if it is a system command.
				if (recognizedStr.startsWith("Connect to"))
				{
					if (!isConnected) //If already connected do not connect
					{
						mApplication.getConnectedThread().connect(recognizedStr.substring(11));
					}
				}
				else if (recognizedStr.equals("End connection"))
				{
					mApplication.getConnectedThread().disconnect();
				}

				else if (recognizedStr.equals("All"))
				{
					//focus on all robots
					mApplication.setRobotInFocus("All");
					changeVocab(getDefaultRobotCommands());
				}

				else if (mApplication.getRobotNames().contains(recognizedStr))
				{
					//Set focus on robot whos name was recognized
					mApplication.setRobotInFocus(recognizedStr);
					changeVocab(getDefaultRobotCommands());
				}

				else if (recognizedStr.equals("Stop listening"))
				{
					setListeningStatus(false);
				}

				else if (recognizedStr.equals("View Robots"))
				{
					mApplication.getMainActivity().launchRobotListActivity();				
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
				else 
				{
					if (mHelper == null)
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

						usingSystemCommands = false;
					}
					else
					{
						mHelper.receivePhrase(recognizedStr);
					}
				}
			}
			else if (recognizedStr.equals("Start listening"))
			{
				mApplication.getMainActivity().setCommandView(recognizedStr);
				setListeningStatus(true);
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
}
