package com.riverlab.robotmanager.voice_recognition;

import java.util.ArrayList;

import com.riverlab.robotmanager.MainActivity;
import com.riverlab.robotmanager.RobotManagerApplication;
import com.riverlab.robotmanager.robot.Robot;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

public class VoiceHelperThread extends HandlerThread
{
	RobotManagerApplication mApplication;
	String expression;
	String rootPhrase;
	String targetRobotName;
	Handler voiceRecognitionThreadHandler;

	String newPhrase;
	boolean newPhraseReceived = false;
	boolean reset;

	public static final int RECEIVE_PHRASE_MESSAGE = 0;

	private final Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case RECEIVE_PHRASE_MESSAGE:
				String phraseText = (String)msg.obj;
				receivePhrase(phraseText);
				break;
			}

		}
	};

	public VoiceHelperThread(RobotManagerApplication app, String rootPhrase, String targetRobotName, Handler voiceRecognitionThreadHandler)
	{
		super("Voice Helper Thread");
		this.mApplication = app;
		this.rootPhrase = rootPhrase;
		this.targetRobotName = targetRobotName;
		this.voiceRecognitionThreadHandler = voiceRecognitionThreadHandler;
	}
	
	public Handler getHandler()
	{
		return mHandler;}

	public void run()
	{
		ArrayList<String> previous = new ArrayList<String>();
		ArrayList<Robot> allRobots = new ArrayList<Robot>(mApplication.getRobots());
		ArrayList<String> nextPhrases = new ArrayList<String>();
		boolean isRequired = false;

		Robot targetRobot = mApplication.getRobot(targetRobotName);

		newPhrase = rootPhrase;
		newPhraseReceived = true;

		outerloop:
			while (!reset)
			{
				if (newPhraseReceived)
				{
					previous.add(newPhrase);
					newPhraseReceived = false;
					sendUIUpdate(previous);

					//Obtain next set of commands
					if (targetRobotName.equals("All"))
					{
						reset = true;
						ArrayList<String> defaultList = new ArrayList<String>();

						for (Robot robot : allRobots)
						{
							NewPhrasesMessage msg = robot.getNextPhrases(new ArrayList<String>(previous));

							if (msg.reset)
							{
								//All robots must agree to reset before reset can be true
								reset &= msg.reset;
								defaultList.addAll(msg.newVocab);
							}
							else
							{
								reset = false;
								nextPhrases.addAll(msg.newVocab);
								//Only one robot must need a required command before a next
								//command to be required
								isRequired |= msg.isRequired;
							}
						}

						if (reset)
						{
							nextPhrases = defaultList;
						}
					}
					else //If specific robot is in focus
					{
						NewPhrasesMessage msg = targetRobot.getNextPhrases(new ArrayList<String>(previous));
						nextPhrases.addAll(msg.newVocab);
						isRequired = msg.isRequired;
						reset = msg.reset;
					}
					sendNewVocabMessage(nextPhrases);

					if (reset)
					{
						break outerloop;
					}

					if (!isRequired) //If command is not required, set time limit
					{
						long start = System.currentTimeMillis();  

						//Pause thread execution until a phrase has been recieved or a second has elapsed
						while (!newPhraseReceived && (System.currentTimeMillis() - start < 1000));

						//Has execution resumed because a new command has been given?
						if (newPhraseReceived)
						{
							continue outerloop;
						}
						else //Time limit reached
						{
							break outerloop;
						}
					}
				}
			}

		//The full command has been said, compile phrases into an expression and
		//send with a reset message
		for (String phrase : previous)
		{
			expression += (" " + phrase);
		}

		sendResetMessage(expression);
	}

	public void receivePhrase(String phrase)
	{
		newPhrase = phrase;
		newPhraseReceived = true;
	}


	private void sendNewVocabMessage(ArrayList<String> newVocab)
	{
		Message msg = voiceRecognitionThreadHandler.obtainMessage(VoiceRecognitionThread.CHANGE_VOCAB_MESSAGE, newVocab);
		voiceRecognitionThreadHandler.sendMessageAtFrontOfQueue(msg);
	}

	private void sendResetMessage(String expression)
	{
		Handler mainHandler = mApplication.getMainActivityHandler();

		Message mainMsg = mainHandler.obtainMessage(MainActivity.COMMAND_MESSAGE, expression);
		Message voiceMsg = voiceRecognitionThreadHandler.obtainMessage(VoiceRecognitionThread.RESET_MESSAGE, expression);

		mainHandler.sendMessageAtFrontOfQueue(mainMsg);
		voiceRecognitionThreadHandler.sendMessageAtFrontOfQueue(voiceMsg);
	}

	private void sendUIUpdate(ArrayList<String> previousPhrases)
	{
		String commandViewText = "";
		for (String phrase : previousPhrases)
		{
			commandViewText += (phrase + " ");
		}
		commandViewText += ("_");

		Handler mainHandler = mApplication.getMainActivityHandler();
		Message mainMsg = mainHandler.obtainMessage(MainActivity.COMMAND_MESSAGE, commandViewText);
		mainHandler.sendMessageAtFrontOfQueue(mainMsg);		
	}

}
