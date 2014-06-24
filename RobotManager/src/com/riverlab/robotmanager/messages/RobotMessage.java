package com.riverlab.robotmanager.messages;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class RobotMessage implements Parcelable
{
	private String sender;
	private String type;		//Text, Image, Video
	private String text;
	private Bitmap img;	
	private int priority;		//Zero means unimportant, One means important
	
	public RobotMessage()
	{}

	public RobotMessage(String sender, String type, String text, int prioirty) 
	{
		this.sender = sender;
		this.type = type;
		this.text = text;
	}
	
	public RobotMessage(String sender, String type, Bitmap img, int priority)
	{
		this.sender = sender;
		this.type = type;
		this.img = img;
	}
	
    private RobotMessage(Parcel in) {
        sender = in.readString();
        type = in.readString();
        text = in.readString();
        img = (Bitmap)in.readParcelable(null);
    }
	
	public void fromByteArray(byte[] data)
	{
		String strData = new String(data);
		String[] parts = strData.split(";");
		
		setSender(parts[0]);
		setType(parts[1]);
		setText(parts[2]);
	}
	
	public byte[] toByteArray()
	{
		String catString = getSender() + ";" + 
				getType() + ";" + 
				getText();
		return catString.getBytes();
	}

	/**
	 * @return the sender
	 */
	public String getSender() {
		return sender;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the text
	 */
	public String getText() {
		return text;
	}
	
	/**
	 * @return the priority
	 */
	public int getPriority() {
		return priority;
	}


	/**
	 * @param sender the sender to set
	 */
	public void setSender(String sender) {
		this.sender = sender;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @param text the text to set
	 */
	public void setText(String text) {
		this.text = text;
	}
	
	/**
	 * @param priority the priority to set
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) 
	{
		dest.writeString(sender);
		dest.writeString(type);
		dest.writeString(text);
	}
	
	public static final Parcelable.Creator<RobotMessage> CREATOR = new Parcelable.Creator<RobotMessage>() {
        public RobotMessage createFromParcel(Parcel in) {
            return new RobotMessage(in);
        }

        public RobotMessage[] newArray(int size) {
            return new RobotMessage[size];
        }
    };

}
