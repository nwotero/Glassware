package com.riverlab.robotmanager.robot;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ScrollView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.widget.CardScrollView;
import com.riverlab.robotmanager.RobotManagerApplication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ViewRobotListActivity extends Activity implements AdapterView.OnItemClickListener {

	private static final int SPEECH_REQUEST = 0;

	CardScrollView mCardScrollView;
	BluetoothDevice mSelectedDevice;
	RobotCardScrollAdapter adapter;
	GestureDetector mGestureDetector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mGestureDetector = createGestureDetector(this);
		mCardScrollView = new CardScrollView(this);
		mCardScrollView.activate();
		mCardScrollView.setOnItemClickListener(this);
		setContentView(mCardScrollView);
	}

	@Override
	protected void onResume() {
		super.onResume();

		Collection<Robot> mRobots = ((RobotManagerApplication)this.getApplication()).getRobots();
		adapter = new RobotCardScrollAdapter(this, mRobots);
		mCardScrollView.setAdapter(adapter);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audio.playSoundEffect(Sounds.TAP);
		displaySpeechRecognizer();
	}

	private GestureDetector createGestureDetector(Context context) {
		GestureDetector gestureDetector = new GestureDetector(context);
		//Create a base listener for generic gestures
		/*gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
			@Override
			public boolean onGesture(Gesture gesture) {
				if (gesture == Gesture.SWIPE_RIGHT) {
					AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
					audio.playSoundEffect(Sounds.SELECTED);

					return true;
				}
				else if (gesture == Gesture.SWIPE_LEFT){
					AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
					audio.playSoundEffect(Sounds.SELECTED);
				}
				else if (gesture == Gesture.TWO_SWIPE_RIGHT){

				}
				else if (gesture == Gesture.TWO_SWIPE_LEFT){

				}
				else if (gesture == Gesture.TAP){

				}
				return false;
			}
		});*/

		gestureDetector.setFingerListener(new GestureDetector.FingerListener() {
			//ScrollView mScrollBody = (ScrollView)findViewById(R.id.scrollBody);
			@Override
			public void onFingerCountChanged(int previousCount, int currentCount) {
				if(currentCount == 2){
					mCardScrollView.deactivate();
				}else{

					mCardScrollView.activate();
					mCardScrollView.requestFocus();
				}
			}
		});
			return gestureDetector;
	}
			
		
	

		@Override
		public boolean onGenericMotionEvent(MotionEvent event) {
			if (mGestureDetector != null) {
				return mGestureDetector.onMotionEvent(event);
			}
			return false;
		}

		private void displaySpeechRecognizer() {
			Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			startActivityForResult(intent, SPEECH_REQUEST);
		}

		@Override
		protected void onActivityResult(int requestCode, int resultCode,
				Intent data) {/*
			if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK) {
				List<String> results = data.getStringArrayListExtra(
						RecognizerIntent.EXTRA_RESULTS);
				String spokenText = results.get(0);

				BluetoothSocket connectedBtSocket = ((RobotManagerApplication)this.getApplication()).getBluetoothSocket();
				OutputStream outStream;
				try {
					outStream = connectedBtSocket.getOutputStream();
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}

				try {
					outStream.write(spokenText.getBytes());
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}

				// Listen for confirmation of receipt.
				Log.d("RobotManagerBluetooth", "Listening for confirmation message from server");
				InputStream inStream;
				try {
					inStream = connectedBtSocket.getInputStream();
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}

				String confirmString = "Copy: " + spokenText + "\n";
				byte[] receivedBytes = new byte[confirmString.getBytes().length];
				try
				{
					inStream.read(receivedBytes);
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
			}
			super.onActivityResult(requestCode, resultCode, data);*/
		}
	}
