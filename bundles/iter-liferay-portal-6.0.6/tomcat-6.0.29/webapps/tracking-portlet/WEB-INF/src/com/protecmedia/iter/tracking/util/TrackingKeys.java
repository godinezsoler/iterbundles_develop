package com.protecmedia.iter.tracking.util;

import java.util.HashMap;
import java.util.Map;

public class TrackingKeys {
	
	public static final String SOCIAL_NETWORK_GOOGLE_PLUS = "Google plus";
	public static final String SOCIAL_NETWORK_FACEBOOK = "Facebook";
	public static final String SOCIAL_NETWORK_TWITTER = "Twitter";
	public static final String SOCIAL_NETWORK_TUENTI = "Tuenti";
	public static final String SOCIAL_NETWORK_MENEAME = "Meneame";
	public static final String SOCIAL_NETWORK_DELICIOUS = "Delicious";
	public static final String SOCIAL_NETWORK_MYSPACE = "Myspace";
	
	@SuppressWarnings("serial")
	public static final Map<String, String> trackingMap = new HashMap<String, String>(){{
		put(	SOCIAL_NETWORK_GOOGLE_PLUS,	"/google_plus.jsp" );
		put(	SOCIAL_NETWORK_FACEBOOK,	"/facebook.jsp" );
		put(	SOCIAL_NETWORK_TWITTER,		"/twitter.jsp" );
		put(	SOCIAL_NETWORK_TUENTI,		"/tuenti.jsp" );
		put(	SOCIAL_NETWORK_MENEAME,		"/meneame.jsp" );
		put(	SOCIAL_NETWORK_DELICIOUS,	"/delicious.jsp" );
		put(	SOCIAL_NETWORK_MYSPACE,		"/myspace.jsp" );
	}};
	
	public static final String[] socialNetworkList = {	SOCIAL_NETWORK_DELICIOUS,
														SOCIAL_NETWORK_FACEBOOK,
														SOCIAL_NETWORK_GOOGLE_PLUS,
														SOCIAL_NETWORK_MENEAME,
														SOCIAL_NETWORK_MYSPACE,
														SOCIAL_NETWORK_TUENTI,
														SOCIAL_NETWORK_TWITTER
													 };
	
}
