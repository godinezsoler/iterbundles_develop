package com.protecmedia.iter.base.service.util;

public class FormatDateUtil
{
	public static String formatDateFlexToJQuery( String date )
	{
		String formatDate= "";
		if( date.equalsIgnoreCase( "dd/MM/yyyy" ) )
		{
			formatDate  = "dd/mm/yy";
		}
		else if(  date.equalsIgnoreCase( "dd-MM-yyyy" )  )
		{
			formatDate  = "dd-mm-yy";
		}
		else if(  date.equalsIgnoreCase( "dd.MM.yyyy" )  )
		{
			formatDate  = "dd.mm.yy";
		}
		else if(  date.equalsIgnoreCase( "dd/MM/yy" )  )
		{
			formatDate  = "dd/mm/y";
		}
		else if(  date.equalsIgnoreCase( "dd-MM-yy" )  )
		{
			formatDate  = "dd-mm-y";
		}
		else if(  date.equalsIgnoreCase( "dd.MM.yy" )  )
		{
			formatDate  = "dd.mm.y";
		}
		else if(  date.equalsIgnoreCase( "d/MM/yy" )  )
		{
			formatDate  = "d/mm/y";
		}
		else if(  date.equalsIgnoreCase( "d-MM-yy" )  )
		{
			formatDate  = "d-mm-y";
		}
		else if(  date.equalsIgnoreCase( "d.MM.yy" )  )
		{
			formatDate  = "d.mm.y";
		}
		else if(  date.equalsIgnoreCase( "d/M/yy" )  )
		{
			formatDate  = "d/m/y";
		}
		else if(  date.equalsIgnoreCase( "d-M-yy" )  )
		{
			formatDate  = "d-m-y";
		}
		else if(  date.equalsIgnoreCase( "d.M.yy" )  )
		{
			formatDate  = "d.m.y";
		}
		else if(  date.equalsIgnoreCase( "yy/MM/dd" )  )
		{
			formatDate  = "y/mm/dd";
		}
		else if(  date.equalsIgnoreCase( "yy-MM-dd" )  )
		{
			formatDate  = "y-mm-dd";
		}
		else if(  date.equalsIgnoreCase( "yy.MM.dd" )  )
		{
			formatDate  = "y.mm.dd";
		}
		else if(  date.equalsIgnoreCase( "yy/MM/d" )  )
		{
			formatDate  = "y/mm/d";
		}
		else if(  date.equalsIgnoreCase( "yy-MM-d" )  )
		{
			formatDate  = "y-mm-d";
		}
		else if(  date.equalsIgnoreCase( "yy.MM.d" )  )
		{
			formatDate  = "y.mm.d";
		}
		else if(  date.equalsIgnoreCase( "yy/M/d" )  )
		{
			formatDate  = "y/m/d";
		}
		else if(  date.equalsIgnoreCase( "yy-M-d" )  )
		{
			formatDate  = "y-m-d";
		}
		else if(  date.equalsIgnoreCase( "yy.M.d" )  )
		{
			formatDate  = "y.m.d";
		}
		else if(  date.equalsIgnoreCase( "yyyy/MM/dd" )  )
		{
			formatDate  = "yy/mm/dd";
		}
		else if(  date.equalsIgnoreCase( "yyyy-MM-dd" )  )
		{
			formatDate  = "yy-mm-dd";
		}
		else if(  date.equalsIgnoreCase( "yyyy.MM.dd" )  )
		{
			formatDate  = "yy.mm.dd";
		}
		else if(  date.equalsIgnoreCase( "dddd MMMM dd yyyy" )  )
		{
			formatDate  = "DD MM dd yy";
		}
		else if(  date.equalsIgnoreCase( "dddd, MMMM dd yyyy" )  )
		{
			formatDate  = "DD, MM dd yy";
		}
		else if(  date.equalsIgnoreCase( "dddd dd MMMM yyyy" )  )
		{
			formatDate  = "DD dd MM yy";
		}
		else if(  date.equalsIgnoreCase( "dddd, dd MMMM yyyy" )  )
		{
			formatDate  = "DD, dd MM yy";
		}
		else if(  date.equalsIgnoreCase( "dd MMMM yyyy" )  )
		{
			formatDate  = "dd MM yy";
		}
		else if(  date.equalsIgnoreCase( "MMMM dd yyyy" )  )
		{
			formatDate  = "MM dd yy";
		}
	
		return formatDate;
	}
	
}