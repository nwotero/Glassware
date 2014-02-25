package com.riverlab.glassbluetooth;

import java.util.UUID;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;

import com.riverlab.glassbluetooth.R;
import com.keyboardr.glassremote.client.RemoteMessenger;
import com.keyboardr.glassremote.client.RemoteMessengerImpl;
import com.keyboardr.glassremote.client.StringRemoteMessengerService;
import com.keyboardr.glassremote.client.RemoteMessenger.Callback;
import com.keyboardr.glassremote.client.RemoteMessengerService.RemoteConnectionBinder;
import com.keyboardr.glassremote.common.receiver.MessageReceiver;
import com.keyboardr.glassremote.common.receiver.StringMessageReader;
import com.keyboardr.glassremote.common.sender.MessageSender;
import com.keyboardr.glassremote.common.sender.StringMessageSender;

public class BluetoothService extends Service
{
	private final RemoteConnectionBinder mBinder = new RemoteConnectionBinder();

	private RemoteMessenger<String, String> mMessenger;

	public class RemoteConnectionBinder extends Binder implements
			RemoteMessenger<String, String> {

		@Override
		public void setCallback(Callback<? super String> callback) {
			mMessenger.setCallback(callback);
		}

		@Override
		public boolean isConnected() {
			return mMessenger.isConnected();
		}

		@Override
		public void requestConnect() {
			mMessenger.requestConnect();
		}

		@Override
		public void disconnect() {
			mMessenger.disconnect();
		}

		@Override
		public void sendMessage(String arg0) throws IllegalStateException {
			// TODO Auto-generated method stub
			
		}

	}
	

	public void onCreate()
	{
		super.onCreate();
		mMessenger = new RemoteMessengerImpl<String, String>(UUID.fromString("1aefbf9b-ea60-47de-b5a0-ed0e3a36d9a5"), new StringMessageSender(), new StringMessageReader());
	}
	
	public void onStartCommand()
	{
		mMessenger.requestConnect();
		while (!mMessenger.isConnected());
		mMessenger.sendMessage("Connection Succesful");
	}
	
	@Override
	public void onDestroy() {
		mMessenger.disconnect();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		mMessenger.setCallback(null);
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		mMessenger.setCallback(null);
		mMessenger.disconnect();
		return super.onUnbind(intent);
	}

}