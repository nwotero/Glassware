package com.riverlab.robotmanager.voice_recognition;

import java.util.ArrayList;

import com.riverlab.robotmanager.RobotManagerApplication;
import com.riverlab.robotmanager.robot.Robot;

import android.os.Handler;
import android.os.Message;

public class VoiceHelperThread extends Thread
{
	RobotManagerApplication mApplication;
	String expression;
	String rootPhrase;
	String targetRobotName;
	Handler voiceRecognitionThreadHandler;

	String newPhrase;
	boolean newPhraseReceived = false;
	boolean reset;

	public VoiceHelperThread(RobotManagerApplication app, String rootPhrase, String targetRobotName, Handler voiceRecognitionThreadHandler)
	{
		this.mApplication = app;
		this.rootPhrase = rootPhrase;
		this.targetRobotName = targetRobotName;
		this.voiceRecognitionThreadHandler = voiceRecognitionThreadHandler;
	}
	@Override
	public void run()
	{
		ArrayList<String> previous = new ArrayList<String>();
		previous.add(rootPhrase);
		ArrayList<Robot> allRobots = new ArrayList<Robot>(mApplication.getRobots());
		ArrayList<String> nextPhrases = new ArrayList<String>();
		boolean isRequired = false;

		Robot targetRobot = mApplication.getRobot(targetRobotName);

		outerloop:
			while (!reset)
			{
				//Obtain next set of commands
				if (targetRobotName.equals("All"))
				{
					reset = true;
					ArrayList<String> defaultList = new ArrayList<String>();
					
					for (Robot robot : allRobots)
					{
						NewPhrasesMessage msg = robot.getNextPhrases(previous);

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
					NewPhrasesMessage msg = targetRobot.getNextPhrases(previous);
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
						previous.add(newPhrase);
						newPhraseReceived = false;
					}
					else //Time limit reached
					{
						break outerloop;
					}
				}
				else //If command is required, pause execution until a commmand is recieved
				{
					while (!newPhraseReceived);
					previous.add(newPhrase);
					newPhraseReceived = false;
				}
				sendUIUpdate(previous);
			}

		//The full command has been said, compile phrases into an expression and
		//send with a reset message
		for (String phrase : previous)
		{
			expression.concat(" " + phrase);
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
		mApplication.getVoiceThread().changeVocab(newVocab);
	}

	private void sendResetMessage(String expression)
	{
		mApplication.getMainActivity().setCommandView(expression);
		mApplication.getVoiceThread().reset(expression);
	}
	
	private void sendUIUpdate(ArrayList<String> previousPhrases)
	{
		for (String phrase : previousPhrases)
		{
			expression.concat(" " + phrase);
		}
		expression.concat(" _");
		
		mApplication.getMainActivity().setCommandView(expression);
		
	}

}
