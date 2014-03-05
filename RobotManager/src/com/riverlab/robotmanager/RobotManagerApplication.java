package com.riverlab.robotmanager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class RobotManagerApplication extends Application
{
	private BluetoothSocket mBluetoothSocket;
	private BluetoothAdapter mBluetoothAdapter;
	private ArrayList<Robot> mRobots;
	
	public BluetoothSocket connectToDevice(BluetoothDevice device) 
	{
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		Log.d("RobotManagerBluetooth", "Attempting to connect to device");
		// Discovery is resource intensive.  Make sure it isn't going on
		// when you attempt to connect and pass your message.
		mBluetoothAdapter.cancelDiscovery();

		// Create a Rfcomm Socket between the Server and Glass
		Log.d("RobotManagerBluetooth", "Attempting to create Rfcomm Socket");
		Method m;
		try {
			m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}); 
			mBluetoothSocket = (BluetoothSocket) m.invoke(device, 1); 
			Log.d("RobotManagerBluetooth", "Rfcomm Socket created");
		} 
		catch (NoSuchMethodException e) {
			e.printStackTrace();
			return null;
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}

		// Establish the connection.  This will block until it connects.
		Log.d("RobotManagerBluetooth", "Attempting to open socket");
		try {
			mBluetoothSocket.connect();
			Log.d("RobotManagerBluetooth", "Connection established");
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		// Create a data stream so we can talk to server.
		Log.d("RobotManagerBluetooth", "Sending confirmation message to server");
		OutputStream outStream;
		try {
			outStream = mBluetoothSocket.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		String message = "Confirm connection\n";
		byte[] msgBuffer = message.getBytes();
		try {
			Log.d("RobotManagerBluetooth", "Writing message");
			outStream.write(msgBuffer);
			Log.d("RobotManagerBluetooth", "Message written");
		} catch (IOException e) {
			e.printStackTrace();   
			return null;
		}

		// Listen for confirmation from the server.
		Log.d("RobotManagerBluetooth", "Listening for confirmation message from server");
		InputStream inStream;
		try {
			inStream = mBluetoothSocket.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		String confirmString = "Connection confirmed\n";
		byte[] receivedBytes = new byte[confirmString.getBytes().length];
		try
		{
			inStream.read(receivedBytes);
		} catch (IOException e){
			e.printStackTrace();
			return null;
		}
		String receivedString = new String(receivedBytes);
		if (receivedString.equals(confirmString))
		{
			Log.d("RobotManagerBluetooth", "Coonnection confirmed");
			return mBluetoothSocket;
		}
		else
		{
			Log.d("RobotManagerBluetooth", "Confirmation not received");
			return null;
		}
	}
	
	
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
