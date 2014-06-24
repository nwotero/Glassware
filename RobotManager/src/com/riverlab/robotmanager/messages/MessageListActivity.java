package com.riverlab.robotmanager.messages;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;

import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardScrollView;
import com.riverlab.robotmanager.RobotManagerApplication;

public class MessageListActivity extends Activity implements AdapterView.OnItemClickListener
{

	List<RobotMessage> mMessages;
	CardScrollView mCardScrollView;
	RobotMessage mSelectedMessage;
	MessageCardScrollAdapter adapter;
	RobotManagerApplication mApplication;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mCardScrollView = new CardScrollView(this);
		mCardScrollView.activate();
		mCardScrollView.setOnItemClickListener(this);
		mApplication = ((RobotManagerApplication) this.getApplication());
		setContentView(mCardScrollView);
	}

	@Override
	protected void onResume() {
		super.onResume();

		mApplication.setMsgListActivity(this);
		mMessages = mApplication.getMessages();
		adapter = new MessageCardScrollAdapter(this, mMessages);
		mCardScrollView.setAdapter(adapter);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
		mApplication.setMsgListActivity(null);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		mSelectedMessage = (RobotMessage) mCardScrollView.getItemAtPosition(position);
		AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audio.playSoundEffect(Sounds.TAP);
		openOptionsMenu();
	}
	
	public void onMessageAddition()
	{
		int curPosition = 0;
		curPosition = mCardScrollView.getSelectedItemPosition();
		
		mMessages = mApplication.getMessages();
		adapter = new MessageCardScrollAdapter(this, mMessages);
		mCardScrollView.setAdapter(adapter);
		
		if (mMessages.get(0).getPriority() == 0)
		{
			mCardScrollView.setSelection(curPosition);
		}
		else
		{
			mCardScrollView.setSelection(0);
		}
	}
}
