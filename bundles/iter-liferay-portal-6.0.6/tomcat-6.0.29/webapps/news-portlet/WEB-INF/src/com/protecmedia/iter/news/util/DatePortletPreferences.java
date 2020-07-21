package com.protecmedia.iter.news.util;

public class DatePortletPreferences 
{

	private String textBefore = "";
	private String textAfter = "";
	private String dateWithoutHourFormat = "";
	private String hourFormat = "";
	
	public String getTextBefore() 
	{
		return textBefore;
	}
	
	public void setTextBefore(String textBefore) 
	{
		this.textBefore = textBefore;
	}
	
	public String getTextAfter() 
	{
		return textAfter;
	}
	
	public void setTextAfter(String textAfter) 
	{
		this.textAfter = textAfter;
	}
	
	public String getDateWithoutHourFormat() 
	{
		return dateWithoutHourFormat;
	}
	
	public void setDateWithoutHourFormat(String dateWithoutHourFormat) 
	{
		this.dateWithoutHourFormat = dateWithoutHourFormat;
	}
	
	public String getHourFormat() 
	{
		return hourFormat;
	}
	
	public void setHourFormat(String hourFormat) 
	{
		this.hourFormat = hourFormat;
	}
	
}
