package com.riverlab.glassbluetooth;

import java.io.StringReader;
import java.util.UUID;

import com.riverlab.glassbluetooth.R;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;
import com.keyboardr.glassremote.client.RemoteMessenger;
import com.keyboardr.glassremote.client.RemoteMessengerImpl;
import com.keyboardr.glassremote.common.receiver.MessageReceiver;
import com.keyboardr.glassremote.common.receiver.StringMessageReader;
import com.keyboardr.glassremote.common.sender.MessageSender;
import com.keyboardr.glassremote.common.sender.StringMessageSender;
import com.keyboardr.glassremote.server.MessageService;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.widget.RemoteViews;

public class GlassBluetoothService extends Service//MessageService<String, String> {
{
	/*protected GlassBluetoothService(String name, UUID uuid,
			MessageSender<String> sender, MessageReceiver<String> receiver) {
		super(name, uuid, sender, receiver);
	}*/

	private static final String LIVE_CARD_ID = "glass_bluetooth";
	private TimelineManager mTimelineManager;
	private LiveCard mLiveCard;
	private TextToSpeech mSpeech;
	private RemoteMessenger<String, String> mBluetoothClient;
	private final IBinder mBinder = new MainBinder();
	boolean bluetoothConnected = false;
	private RemoteViews aRV;
	private final CharSequence CONNECTED = "Connected";
	private final CharSequence DISCONNECTED = "Disconnected";

	public class MainBinder extends Binder {
		public void speakConnectionStatus() {
			if (!bluetoothConnected)
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

		//mBluetoothClient = new RemoteMessengerImpl<String, String>(UUID.randomUUID(), new StringMessageSender(), new StringMessageReader());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		aRV = new RemoteViews(this.getPackageName(),
				R.layout.connection_card_layout);
		if (mLiveCard == null) {
			mLiveCard = mTimelineManager.createLiveCard(LIVE_CARD_ID);
			//aRV.setTextViewText(R.id.main_text, INTRO);
			mLiveCard.setViews(aRV);
			Intent mIntent = new Intent(this, MainActivity.class);
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

	/*@Override
	protected void onConnected(BluetoothDevice remoteDevice) 
	{
		bluetoothConnected = true;
        aRV.setTextViewText(R.id.main_text, CONNECTED);
	}

	@Override
	protected void onDisconnected(BluetoothDevice remoteDevice) 
	{
		bluetoothConnected = true;
        aRV.setTextViewText(R.id.main_text, DISCONNECTED);
	}

	@Override
	protected void onReceiveMessage(String message) {
		// TODO Auto-generated method stub

	}
	 */
}
