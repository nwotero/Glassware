package com.riverlab.robotmanager.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.riverlab.robotmanager.MainActivity;
import com.riverlab.robotmanager.RobotManagerApplication;
import com.riverlab.robotmanager.messages.RobotMessage;
import com.riverlab.robotmanager.robot.Robot;
import com.riverlab.robotmanager.voice_recognition.Vocabulary;
import com.riverlab.robotmanager.voice_recognition.VoiceRecognitionThread;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class ConnectedThread extends HandlerThread
{
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothSocket mBtSocket;
	private InputStream mInStream;
	private OutputStream mOutStream;
	private RobotManagerApplication mApplication;
	private boolean isShutdown = false;
	private Object readWriteLock = new Object();

	public static final int CONNECT_MESSAGE = 0;
	public static final int DISCONNECT_MESSAGE = 1;
	public static final int WRITE_MESSAGE = 2;
	public static final int SHUTDOWN_MESSAGE = 3;


	private Handler mHandler = null;
	private Handler mainHandler;
	private Handler voiceHandler;
	
	Thread socketThread;

	public ConnectedThread(Handler mainHandler, RobotManagerApplication app) 
	{
		super("Bluetooth Connection Thread");
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mApplication = app;
	}
	
	@Override
	public void start()
	{
		super.start();
		
		mHandler = new Handler(getLooper()){
			public void handleMessage(Message msg) 
			{
				switch (msg.what)
				{
				case CONNECT_MESSAGE:
					String deviceName = (String)msg.obj;
					connect(deviceName);
					break;
				case DISCONNECT_MESSAGE:
					disconnect();
					break;
				case WRITE_MESSAGE:
					String msgText = (String)msg.obj;
					write(msgText.getBytes());
					break;
				case SHUTDOWN_MESSAGE:
					shutdown();
					break;
				}
			}
		};
		
		socketThread = new Thread()
		{
			@Override
			public void run()
			{
				pollSocket();
			}
		};
		socketThread.start();
	}

	public synchronized boolean isReady()
	{
		return mHandler != null;
	}

	public void setHandlers(Handler mainHandler, Handler voiceHandler) 
	{
		this.mainHandler = mainHandler;
		this.voiceHandler = voiceHandler;
	}

	public Handler getHandler()
	{
		return mHandler;
	}

	public boolean connect(String deviceName)
	{
		for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices())
		{
			if (device.getName().equals(deviceName))
			{				
				Log.d("RobotManagerBluetooth", "Attempting to connect to device");
				// Discovery is resource intensive.  Make sure it isn't going on
				// when you attempt to connect and pass your message.
				mBluetoothAdapter.cancelDiscovery();

				// Create a Rfcomm Socket between the Server and Glass
				Log.d("RobotManagerBluetooth", "Attempting to create Rfcomm Socket");
				Method m;
				try {
					m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}); 
					mBtSocket = (BluetoothSocket) m.invoke(device, 1); 
					Log.d("RobotManagerBluetooth", "Rfcomm Socket created");
				} 
				catch (NoSuchMethodException e) {
					e.printStackTrace();
					return false;
				}
				catch (IllegalArgumentException e) {
					e.printStackTrace();
					return false;
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					return false;
				} catch (InvocationTargetException e) {
					e.printStackTrace();
					return false;
				}

				// Establish the connection.  This will block until it connects.
				Log.d("RobotManagerBluetooth", "Attempting to open socket");
				try {
					mBtSocket.connect();
					Log.d("RobotManagerBluetooth", "Connection established");
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}

				// Create a data stream so we can talk to server.
				Log.d("RobotManagerBluetooth", "Sending confirmation message to server");
				try {
					mOutStream = mBtSocket.getOutputStream();
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}

				String message = "Confirm connection\n";
				byte[] msgBuffer = message.getBytes();
				try {
					Log.d("RobotManagerBluetooth", "Writing message");
					mOutStream.write(msgBuffer);
					Log.d("RobotManagerBluetooth", "Message written");
				} catch (IOException e) {
					e.printStackTrace();   
					return false;
				}

				// Listen for confirmation from the server.
				Log.d("RobotManagerBluetooth", "Listening for confirmation message from server");
				try {
					mInStream = mBtSocket.getInputStream();
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}

				String confirmString = "Connection confirmed\n";
				byte[] receivedBytes = new byte[confirmString.getBytes().length];
				try
				{
					mInStream.read(receivedBytes);
				} catch (IOException e){
					e.printStackTrace();
					return false;
				}
				String receivedString = new String(receivedBytes);
				if (receivedString.equals(confirmString))
				{
					Log.d("RobotManagerBluetooth", "Connection confirmed");
					mApplication.setConnectionStatus(true);

					Message connectionMessage = new Message();
					connectionMessage.what = MainActivity.CONNECTION_MESSAGE;
					connectionMessage.obj = "connected";
					mainHandler.sendMessageAtFrontOfQueue(connectionMessage);

					return true;
				}
				else
				{
					Log.d("RobotManagerBluetooth", "Confirmation not received");
					mApplication.setConnectionStatus(false);
					return false;
				}
			}
		}
		return false;
	}

	public synchronized byte[] read()
	{
		byte[] buffer = new byte[2048];

		try {
			buffer = new byte[2048];
			mInStream.read(buffer);
			return buffer;
		} catch (IOException e) {
			e.printStackTrace();
			Log.d("ConnectedThread", "Error reading message");
			return null;
		}
	}
	
	public void pollSocket() 
	{

		Log.d("ConnectedThread", "Polling socket");

		while (!isShutdown)
		{
			// Keep listening to the InputStream until an exception occurs	
			String bufferString = "";

			while (mApplication.getConnectionStatus()) 
			{
				Log.d("ConnectedThread", "Listening for message");

				byte[] buffer = new byte[2048];  // buffer store for the stream
				String messageString = "";
				boolean incoming = false;

				while(!incoming)
				{
					synchronized (readWriteLock) 
					{
						try {
							if (mInStream.available() > 0)
							{
								buffer = read();
								incoming = true;
							}
						} catch (IOException e) {
							//e.printStackTrace();
							incoming = false;
						}
					}
				}
				
				String stringRep = new String(buffer).trim();
				stringRep = stringRep.replace("_SPACE_", " ");
				
				bufferString += stringRep;
				
				Log.d("ConnectedThread", "Buffer: " + bufferString);

				if (bufferString.contains("_END"))
				{
					messageString = bufferString.substring(0, 
							bufferString.indexOf("_END"));
					bufferString = bufferString.substring(
							bufferString.indexOf("_END")+4);


					Log.d("ConnectedThread", "Message read");

					String[] messageParts = messageString.split("_DELIM_");
					Log.d("ConnectedThread", "Message type: " + messageParts[0]);

					if(messageParts[0].equals("robot_configuration"))
					{
						Log.d("ConnectedThread", "Configuring Robot");
						Robot newRobot = new Robot();
						newRobot.setName(messageParts[1]);
						newRobot.setInfo(messageParts[2]);
						newRobot.setVocabulary(new Vocabulary(messageParts[3]));

						Message msg = voiceHandler.obtainMessage(VoiceRecognitionThread.ADD_VOCAB_MESSAGE, newRobot.getName());
						voiceHandler.sendMessageAtFrontOfQueue(msg);

						mApplication.addRobot(newRobot);

						Log.d("ConnectedThread", "Writing confirmation");
						write("configuration complete\n".getBytes());
					}
					else if(messageParts[0].equals("text_message"))
					{
						Log.d("ConnectedThread", "Reading text message");
						RobotMessage msg = new RobotMessage();
						msg.setType("Text");
						msg.setSender(messageParts[1]);
						msg.setText(messageParts[2]);
						msg.setPriority(Integer.parseInt(messageParts[3]));

						Calendar cal = Calendar.getInstance();
						SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
						msg.setTimestamp(sdf.format(cal.getTime()));

						Log.d("ConnectedThread", "Adding text message to messages");
						mApplication.addMessage(msg);
					}
					else if(messageParts[0].equals("image_message"))
					{
						Log.d("ConnectedThread", "Reading image message");
						RobotMessage msg = new RobotMessage();
						msg.setType("Image");
						msg.setSender(messageParts[1]);
						msg.setText(messageParts[2]);
						msg.setImage(messageParts[3]);
						msg.setPriority(Integer.parseInt(messageParts[4]));

						Calendar cal = Calendar.getInstance();
						SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
						msg.setTimestamp(sdf.format(cal.getTime()));

						Log.d("ConnectedThread", "Adding text message to messages");
						mApplication.addMessage(msg);
					}
				}

			}
			/*
				//Listen for message

				try {
					// Read from the InputStream
					mInStream.read(buffer);

					String messageType = new String(buffer).trim();

					Log.d("ConnectedThread", "Message Received " + messageType);

					if(!(messageType.equals("robot_configuration") | messageType.equals("text_message") | messageType.equals("image_message")))
					{
						Log.d("ConnectedThread", "Invalid message type");
						continue;
					}

					//Parse sender
					Log.d("ConnectedThread", "Parsing message sender");
					buffer = new byte[1024];  // buffer store for the stream
					String sender = "";
					try 
					{
						mInStream.read(buffer);
						sender = new String(buffer).trim();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					if (messageType.equals("robot_configuration"))
					{
						parseRobotConfiguration(sender);
					}
					if (messageType.equals("text_message"))
					{
						parseTextMessage(sender);
					}

					/*msg.fromByteArray(buffer);

					String type = msg.getType();
					if (type.equals("Text"))
					{
						sendMainMessage(msg);
					}

				} catch (IOException e)
				{
					e.printStackTrace();
					break;
				}
			 */

		}
	}

	private void parseRobotConfiguration(String sender)
	{
		Log.d("ConnectedThread", "Parsing configuration message");

		byte[] buffer = new byte[8192];  // buffer store for the stream, 8 KB
		String confirmation = "configuration complete\n";
		String failure = "failed to read ";

		Robot newRobot = new Robot();
		newRobot.setName(sender);

		//Parse robot information
		Log.d("ConnectedThread", "Parsing configuration info");
		String info = "";
		try 
		{
			mInStream.read(buffer);
			info = new String(buffer).trim();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			write(failure.concat("info").getBytes());
			return;
		}
		newRobot.setInfo(info);
		Log.d("ConnectedThread", "Configuration info is: " + info);

		//Parse robot vocabulary
		Log.d("ConnectedThread", "Parsing configuration vocabulary");
		String vocabString = "";
		try {
			mInStream.read(buffer);
			vocabString = new String(buffer).trim();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			write(failure.concat("vocabulary").getBytes());
			return;
		}
		Log.d("ConnectedThread", "Configuration vocabulary is: " + vocabString);
		newRobot.setVocabulary(new Vocabulary(vocabString));

		mApplication.addRobot(newRobot);
		Log.d("ConnectedThread", "Writing confirmation");
		write(confirmation.getBytes());
	}


	private void parseTextMessage(String sender)
	{
		byte[] buffer = new byte[8192];  // buffer store for the stream, 8 KB
		String confirmation = "confirm receipt";
		String failure = "failed to read";

		//Parse message text
		String text = "";
		try 
		{
			mInStream.read(buffer);
			text = new String(buffer);
		} catch (IOException e) {
			e.printStackTrace();
			write(failure.getBytes());
			return;
		}

		//Parse message priority
		int priority = 0;
		try 
		{
			mInStream.read(buffer);
			priority = ByteBuffer.wrap(buffer).getInt();
		} catch (IOException e) {
			e.printStackTrace();
			write(failure.getBytes());
			return;
		}

		RobotMessage inMsg = new RobotMessage(sender, "text", text, priority);
		mApplication.addMessage(inMsg);

		write(confirmation.getBytes());
	}

	private void parseImageMessage(String sender)
	{
		byte[] buffer = new byte[8192];  // buffer store for the stream, 8 KB
		String confirmation = "confirm receipt";
		String failure = "failed to read";

		//Parse message text
		String text = "";
		try 
		{
			mInStream.read(buffer);
			text = new String(buffer);
		} catch (IOException e) {
			e.printStackTrace();
			write(failure.getBytes());
			return;
		}

		//Parse message priority
		int priority = 0;
		try 
		{
			mInStream.read(buffer);
			priority = ByteBuffer.wrap(buffer).getInt();
		} catch (IOException e) {
			e.printStackTrace();
			write(failure.getBytes());
			return;
		}

		//Parse message size
		int numBytes = 0;
		try 
		{
			mInStream.read(buffer);
			priority = ByteBuffer.wrap(buffer).getInt();
		} catch (IOException e) {
			e.printStackTrace();
			write(failure.getBytes());
			return;
		}

		byte[] imgBuffer = new byte[numBytes];
		Bitmap img;
		try 
		{
			mInStream.read(imgBuffer);
			img = BitmapFactory.decodeByteArray(buffer, 0, numBytes);
		} catch (IOException e) {
			e.printStackTrace();
			write(failure.getBytes());
			return;
		}

		RobotMessage inMsg = new RobotMessage(sender, "image", img, priority);
		mApplication.addMessage(inMsg);

		write(confirmation.getBytes());
	}

	public synchronized void write(byte[] bytes) {
		String sentString = new String(bytes);
		String confirmString = "Copy: " + sentString;

		Log.d("RobotManagerBluetooth", "Acquiring read/write lock");
		//synchronized (readWriteLock) 
		//{
		try {
			Log.d("RobotManagerBluetooth", "Writing");
			mOutStream.write(bytes);
		} catch (IOException e) { 
			//Something went wrong, end connection

			e.printStackTrace();
			try {
				mBtSocket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			mApplication.setConnectionStatus(false);
		}
		/*
			// Listen for confirmation of receipt.
			Log.d("RobotManagerBluetooth", "Listening for confirmation message from server");

			byte[] receivedBytes = read();

			String receivedString = new String(receivedBytes).trim();
			if (receivedString.equals(confirmString))
			{
				Log.d("RobotManagerBluetooth", "Receipt confirmed");

			}
			else
			{
				Log.d("RobotManagerBluetooth", "Confirmation of receipt not received, instead: " + receivedString);
				return;
			}
		 */
		//	}
	}

	private void sendMainMessage(RobotMessage msg)
	{
		Message mainMsg = new Message();

		if (msg.getType().equals("simple"))
		{
			mainMsg.what = MainActivity.TEXT_MESSAGE;
		}

		mainMsg.obj = msg;
		mHandler.sendMessageAtFrontOfQueue(mainMsg);
	}

	public void disconnect() 
	{
		try 
		{
			write("end connection\n".getBytes());
			mBtSocket.close();
			mApplication.setConnectionStatus(false);
		} 
		catch (IOException e) { 
			e.printStackTrace();
		}
	}

	public void shutdown()
	{
		if (mApplication.getConnectionStatus())
		{
			disconnect();
		}
		isShutdown = true;
	}
}
