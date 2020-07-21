package com.protecmedia.iter.base.service.util;

import java.util.Calendar;
import java.util.Date;

public class TimeOffset 
{
	private Calendar _cal = null;
	
	public TimeOffset(Date date, int offsetValue, String offsetUnit)
	{
		_cal = Calendar.getInstance();
		_cal.setTime(date);
		_cal.add( getCalendarField(offsetUnit), offsetValue );
	}
	
	public Date getTime()
	{
		return _cal.getTime();
	}
	
	public static int getCalendarField(String unit)
	{
		int field = Calendar.MONTH;
		
		if ( unit.equalsIgnoreCase(IterKeys.CALENDAR_UNIT_DAY) )
			field = Calendar.DAY_OF_YEAR;
		else if ( unit.equalsIgnoreCase(IterKeys.CALENDAR_UNIT_HOUR) )
			field = Calendar.HOUR_OF_DAY;
		else if ( unit.equalsIgnoreCase(IterKeys.CALENDAR_UNIT_YEAR) )
			field = Calendar.YEAR;
		
		return field;
	}
}
