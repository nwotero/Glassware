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
	        TextView headline;
	        TextView text;
	        ImageView img;
	        TextView timestamp;
	    }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.message_card, null);
            holder = new ViewHolder();
            holder.headline = (TextView) convertView.findViewById(R.id.headline);
            holder.text = (TextView) convertView.findViewById(R.id.messageText);
            holder.img = (ImageView) convertView.findViewById(R.id.testImageView);
            holder.timestamp = (TextView) convertView.findViewById(R.id.timestampText);
            
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        RobotMessage msg = getItem(position);
        holder.headline.setText(msg.getType() + " message from: " + msg.getSender());
        holder.text.setText(msg.getText());
        if (msg.getType().equals("Image"))
        	holder.img.setImageBitmap(msg.getImage());
        holder.timestamp.setText("Received: " + msg.getTimestamp());

        return convertView;
	}

}
