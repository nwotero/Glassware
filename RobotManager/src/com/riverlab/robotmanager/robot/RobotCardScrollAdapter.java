package com.riverlab.robotmanager.robot;

import android.app.ActionBar.LayoutParams;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.glass.widget.CardScrollAdapter;
import com.riverlab.robotmanager.R;
import com.riverlab.robotmanager.R.id;
import com.riverlab.robotmanager.R.layout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RobotCardScrollAdapter extends CardScrollAdapter {
	List<Robot> mRobotList;
	Context context;

	public RobotCardScrollAdapter(Context context, Collection<Robot> robots) {
		mRobotList = new ArrayList<Robot>(robots);
		this.context = context;
	}

	public void addRobot(Robot robot) {
		mRobotList.add(robot);
	}

	@Override
	public int getCount() {
		return mRobotList.size();
	}

	@Override
	public Robot getItem(int position) {
		return mRobotList.get(position);
	}

	class ViewHolder {
		TextView name;
		TextView infoTextView;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		int nextID = 3;

		if (convertView == null)
		{
			convertView = View.inflate(context, R.layout.robot_card, null);
			holder = new ViewHolder();

			Robot robot = mRobotList.get(position);

			LinearLayout ll = (LinearLayout) convertView
					.findViewById(R.id.scrollLinearLayout);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

			holder.name = new TextView(this.context);
			holder.name.setId(nextID);
			nextID++;
			holder.name.setTextSize(50);
			holder.name.setText(robot.getName());
			holder.name.setLayoutParams(lp);
			holder.name.setGravity(Gravity.CENTER);
			ll.addView(holder.name);

			holder.infoTextView = new TextView(this.context);
			holder.infoTextView.setId(nextID);
			holder.infoTextView.setTextSize(24);
			holder.infoTextView.setText(robot.getInfo());
			holder.infoTextView.setLayoutParams(lp);
			holder.infoTextView.setGravity(Gravity.LEFT);
			ll.addView(holder.infoTextView);
			
			convertView.setTag(holder);
		} else 
		{
			holder = (ViewHolder) convertView.getTag();
		}

		return convertView;
	}

	@Override
	public int getPosition(Object arg0) {
		// TODO Auto-generated method stub
		return 0;
	}
}
