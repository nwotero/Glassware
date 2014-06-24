package com.riverlab.robotmanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import com.riverlab.robotmanager.bluetooth.ConnectedThread;
import com.riverlab.robotmanager.messages.MessageListActivity;
import com.riverlab.robotmanager.messages.RobotMessage;
import com.riverlab.robotmanager.robot.Robot;
import com.riverlab.robotmanager.voice_recognition.VoiceRecognitionThread;

import android.app.Application;


public class RobotManagerApplication extends Application
{
	private ConnectedThread mConnectedThread;
	private VoiceRecognitionThread mVoiceThread;
	private MainActivity mMainActivity;
	private HashMap<String, Robot> mRobotMap = new HashMap<String, Robot>();
	private boolean isConnected;
	private Robot robotInFocus;
	private ArrayList<RobotMessage> msgs = new ArrayList<RobotMessage>();
	private MessageListActivity msgListActivity = null;
	
	public MainActivity getMainActivity()
	{
		return mMainActivity;
	}
	
	public ConnectedThread getConnectedThread()
	{
		return mConnectedThread;
	}
	
	public VoiceRecognitionThread getVoiceThread()
	{
		return mVoiceThread;
	}
	
	
	public void setMainActivity(MainActivity act)
	{
		mMainActivity = act;
	}
	
	public void setConnectedThread(ConnectedThread thread)
	{
		mConnectedThread = thread;
	}
	
	public void setVoiceThread(VoiceRecognitionThread thread)
	{
		mVoiceThread = thread;
	}
	
	public void setMsgListActivity(MessageListActivity mla)
	{
		msgListActivity = mla;
	}
	
	public boolean getConnectionStatus()
	{
		return isConnected;
	}
	
	
	public void setConnectionStatus(boolean isConnected)
	{
		this.isConnected = isConnected;
	}
	
	
	public Set<String> getRobotNames()
	{
		return mRobotMap.keySet();
	}
	
	public Collection<Robot> getRobots()
	{
		return mRobotMap.values();
	}
	
	public Robot getRobot(String name)
	{
		return mRobotMap.get(name);
	}
	
	public void addRobot(Robot newRobot)
	{
		mRobotMap.put(newRobot.getName(), newRobot);
		mVoiceThread.onRobotAddition(newRobot.getName());
	}
	
	public void removeRobot(Robot robot)
	{
		mRobotMap.remove(robot.getName());
		mVoiceThread.onRobotRemoval(robot.getName());
	}
	
	public Robot getRobotInFocus()
	{
		return robotInFocus;
	}
	
	public void setRobotInFocus(String robotName)
	{
		if (robotName.equals("All"))
		{
			robotInFocus = null;
		}
		robotInFocus = mRobotMap.get(robotName);

	}
	
	public void addMessage(RobotMessage newMsg)
	{
		msgs.add(0, newMsg);
		if (msgListActivity != null)
		{
			msgListActivity.onMessageAddition();
		}
	}
	
	public ArrayList<RobotMessage> getMessages()
	{
		return msgs;
	}
}
