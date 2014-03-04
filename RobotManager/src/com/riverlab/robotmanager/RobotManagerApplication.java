package com.riverlab.robotmanager;

import java.util.ArrayList;

import android.app.Application;
import android.bluetooth.BluetoothSocket;

public class RobotManagerApplication extends Application
{
	private BluetoothSocket mBluetoothSocket;
	private ArrayList<Robot> mRobots;
	
	public BluetoothSocket getBluetoothSocket()
	{
		return mBluetoothSocket;
	}
	
	public void setBluetoothSocket(BluetoothSocket newBluetoothSocket)
	{
		mBluetoothSocket = newBluetoothSocket;
	}
	
	public ArrayList<Robot> getRobotList()
	{
		return mRobots;
	}
	
	public void setRobotList(ArrayList<Robot> newRobotList)
	{
		mRobots = newRobotList;
	}
}
