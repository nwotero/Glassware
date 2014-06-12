package com.riverlab.robotmanager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ConnectedThread extends Thread
{
	private BluetoothSocket mBtSocket;
	private InputStream mInStream;
	private OutputStream mOutStream;
	private Handler mHandler;
	private RobotManagerApplication mApplication;

	public ConnectedThread(BluetoothSocket socket, Handler uiHandler, RobotManagerApplication app) {
		mBtSocket = socket;
		InputStream tmpIn = null;
		OutputStream tmpOut = null;
		mHandler = uiHandler;
		mApplication = app;

		// Get the input and output streams, using temp objects because
		// member streams are final
		try {
			tmpIn = socket.getInputStream();
			tmpOut = socket.getOutputStream();
		} catch (IOException e) { }

		mInStream = tmpIn;
		mOutStream = tmpOut;
	}

	public void run() {
		byte[] buffer = new byte[1024];  // buffer store for the stream
		byte[] bytes; // bytes returned from read()

		// Keep listening to the InputStream until an exception occurs
		while (true) {
			try {
				// Read from the InputStream
				mInStream.read(buffer);

				parseInputSteam(new String(buffer));
				// Send the obtained bytes to the UI activity

			} catch (IOException e) {
				break;
			}
		}
	}

	private void parseInputSteam(String input) 
	{
		if (input.equals("Sending Robot Information:\n"))
		{
			handleRobotInfoMsg();
		}
		else
		{
			return;
		}


	}

	private void handleRobotInfoMsg() 
	{
		String data;
		byte[] buffer = new byte[1024];
		boolean outerDone = false;
		boolean innerDone = true;
		ArrayList<Robot> robotList = new ArrayList<Robot>();

		while (!outerDone)
		{
			Robot newRobot = new Robot();

			try {
				mInStream.read(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}

			data = new String(buffer);

			if (data.equals("_End\n"))
			{
				outerDone = true;
				innerDone = true;
			} else
			{
				newRobot.setName(data);
			}

			while (innerDone)
			{
				try {
					mInStream.read(buffer);
				} catch (IOException e) {
					e.printStackTrace();
				}

				data = new String(buffer);

				if (data.equals("_Next\n"))
				{
					innerDone = true;
				}
				else
				{
					newRobot.addInfo(data);
				}
			}
			robotList.add(newRobot);
		}
		
		mApplication.setRobotList(robotList);
		Message msg = new Message();
		msg.what = MainActivity.ROBOT_INFO_UPDATE;
		mHandler.sendMessage(msg);
	}

	/* Call this from the main activity to send data to the remote device */
	public void write(byte[] bytes) {
		String sentString = new String(bytes);
		String confirmString = "Copy: " + sentString + "\n";
		
		try {
			mOutStream.write(bytes);
		} catch (IOException e) { }
/*
		// Listen for confirmation of receipt.
		Log.d("RobotManagerBluetooth", "Listening for confirmation message from server");

		byte[] receivedBytes = new byte[confirmString.getBytes().length];
		try
		{
			mInStream.read(receivedBytes);
		} catch (IOException e){
			e.printStackTrace();
			return;
		}
		
		String receivedString = new String(receivedBytes);
		if (receivedString.equals(confirmString))
		{
			Log.d("RobotManagerBluetooth", "Receipt confirmed");

		}
		else
		{
			Log.d("RobotManagerBluetooth", "Confirmation of receipt not received");
			return;
		}
		*/
	}

	/* Call this from the main activity to shutdown the connection */
	//    public void cancel() {
	//        try {
	//            mmSocket.close();
	//        } catch (IOException e) { }
	//    }
}
