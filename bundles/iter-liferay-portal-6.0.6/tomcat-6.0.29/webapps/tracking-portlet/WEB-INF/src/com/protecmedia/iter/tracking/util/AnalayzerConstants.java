/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.tracking.util;

public class AnalayzerConstants {
	
	public static final String[] TABNAMES = {"Most Recent", "Top Rated" , "Most Viewed", "Most Comment", "Most Shared"};
	
	public static final int TABRECENT = 0;
	
	public static final int TABRATED = 1;
	
	public static final int TABVIEWED = 2;
	
	public static final int TABCOMMENT = 3;
	
	public static final int TABSHARED = 4;
	
	public static final boolean isIn(String s){
		boolean result = false;
		try{
			int aValue = Integer.parseInt(s);
			if(aValue == TABRECENT || aValue == TABRATED || aValue == TABVIEWED || aValue == TABCOMMENT || aValue == TABSHARED)
			{
				result = true;
			}
		}catch(Exception e){
			
		}
		return result;
	}
}

