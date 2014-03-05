package com.riverlab.robotmanager;

import java.util.List;

public class Robot 
{
	private String name;
	private List<String> info;
	
	public Robot()
	{
		//No data
	}
	
	public Robot(String name)
	{
		this.name = name;
	}
	
	public Robot (String name, List<String> info)
	{
		this.name = name;
		this.info = info;
	}
	
	public String getName()
	{
		return name;
	}
	
	public List<String> getInfo()
	{
		return info;
	}
	
	public void setName(String newName)
	{
		name = newName;
	}
	
	public void setInfo(List<String> newInfo)
	{
		info = newInfo;
	}
	
	public void addInfo(String data)
	{
		info.add(data);
	}
}
