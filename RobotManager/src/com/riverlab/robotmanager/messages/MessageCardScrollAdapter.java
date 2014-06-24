package com.riverlab.robotmanager.messages;

import java.util.List;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.glass.widget.CardScrollAdapter;
import com.riverlab.robotmanager.R;

public class MessageCardScrollAdapter extends CardScrollAdapter
{
	private List<RobotMessage> mMessages;
	private Context context;
	
	public MessageCardScrollAdapter(Context context, List<RobotMessage> messages) 
	{
        mMessages = messages;
        this.context = context;
    }
	
	public void addDevice(RobotMessage msg) {
        mMessages.add(msg);
    }

	@Override
	public int getCount() {
		return mMessages.size();
	}

	@Override
	public RobotMessage getItem(int index) 
	{
		return mMessages.get(index);
	}

	@Override
	public int getPosition(Object msg)
	{
		return mMessages.indexOf((RobotMessage)msg);
	}
	
	 class ViewHolder {
	        ImageView image;
	        TextView headline;
	        TextView text;
	    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.message_card, null);
            holder = new ViewHolder();

            holder.image = (ImageView) convertView.findViewById(R.id.messageImageView);
            holder.headline = (TextView) convertView.findViewById(R.id.headline);
            holder.text = (TextView) convertView.findViewById(R.id.messageText);

            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        RobotMessage msg = getItem(position);
        holder.headline.setText(msg.getType() + " message from: " + msg.getSender());
        holder.text.setText(msg.getText());

        return convertView;
	}

}
