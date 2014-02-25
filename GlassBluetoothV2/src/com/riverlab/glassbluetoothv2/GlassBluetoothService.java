package com.riverlab.glassbluetoothv2;

import java.util.UUID;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;
import com.keyboardr.glassremote.client.RemoteMessenger;
import com.keyboardr.glassremote.client.RemoteMessengerImpl;
import com.keyboardr.glassremote.client.RemoteMessenger.Callback;
import com.keyboardr.glassremote.common.receiver.MessageReceiver;
import com.keyboardr.glassremote.common.receiver.StringMessageReader;
import com.keyboardr.glassremote.common.sender.MessageSender;
import com.keyboardr.glassremote.common.sender.StringMessageSender;
import com.riverlab.glassbluetoothv2.R;

public class GlassBluetoothService extends Service
{
	private UUID mUuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
	private StringMessageSender mSender = new StringMessageSender();
	private StringMessageReader mReceiver = new StringMessageReader();
	private RemoteMessengerImpl<String, String> remoteMessenger = new RemoteMessengerImpl<String, String>(mUuid, mSender, mReceiver);
	private static final String LIVE_CARD_ID = "glass_bluetooth";
	private TimelineManager mTimelineManager;
	private LiveCard mLiveCard;
	private TextToSpeech mSpeech;
	private final IBinder mBinder = new MainBinder();
	private RemoteViews aRV;
	private final CharSequence CONNECTED = "Connected";
	private final CharSequence DISCONNECTED = "Disconnected";
	
	
	public class MainBinder extends Binder implements
	RemoteMessenger<String, String> {

		@Override
		public void setCallback(Callback<? super String> callback) {
			remoteMessenger.setCallback(callback);
		}

		@Override
		public boolean isConnected() {
			return remoteMessenger.isConnected();
		}

		@Override
		public void requestConnect() {
			Log.d("DEBUG", "Requesting Connection");
			remoteMessenger.requestConnect();
			Log.d("DEBUG", "Request Sent");
			while (!remoteMessenger.isConnected());
			remoteMessenger.sendMessage("Connected");
			Log.d("DEBUG", "Message Sent");
		}

		@Override
		public void disconnect() {
			remoteMessenger.disconnect();
		}

		@Override
		public void sendMessage(String message) throws IllegalStateException {
			remoteMessenger.sendMessage(message);		
		}
		
		public void speakConnectionStatus() {
			if (!remoteMessenger.isConnected())
			{
				mSpeech.speak(getString(R.string.bluetooth_not_connected), TextToSpeech.QUEUE_FLUSH, null);
			}
			else
			{
				mSpeech.speak(getString(R.string.bluetooth_connected), TextToSpeech.QUEUE_FLUSH, null);

			}
		}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mTimelineManager = TimelineManager.from(this);
		mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				//do nothing
			}
		});
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		aRV = new RemoteViews(this.getPackageName(),
				R.layout.connection_card_layout);
		if (mLiveCard == null) {
			mLiveCard = mTimelineManager.createLiveCard(LIVE_CARD_ID);
			//aRV.setTextViewText(R.id.main_text, INTRO);
			mLiveCard.setViews(aRV);
			Intent mIntent = new Intent(this, MenuActivity.class);
			mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			mLiveCard.setAction(PendingIntent.getActivity(this, 0, mIntent, 0));
			mLiveCard.publish(LiveCard.PublishMode.REVEAL);
		} 
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		if (mLiveCard != null && mLiveCard.isPublished()) {
			mLiveCard.unpublish();
			mLiveCard = null;
		}
		mSpeech.shutdown();

		mSpeech = null;
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
}
