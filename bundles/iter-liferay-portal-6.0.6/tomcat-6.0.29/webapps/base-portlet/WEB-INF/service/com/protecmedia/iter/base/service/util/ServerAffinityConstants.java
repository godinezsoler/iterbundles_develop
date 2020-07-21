package com.protecmedia.iter.base.service.util;

import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.StringPool;

public interface ServerAffinityConstants
{
	public static final String GET_SERVER_AFFINITY_INFO 			= String.format(new StringBuilder(
		"SELECT kind, serverstate state 							\n").append(
		"FROM serveraffinity 										\n").append(
		"	WHERE (groupid=%%d OR groupid=%d) AND kind='%%s' 		\n").toString(), GroupMgr.getGlobalGroupId());
		
	public static final String SET_SERVER_AFFINITY 					= new StringBuilder(
		"INSERT INTO serveraffinity (groupid, kind, serverstate)	\n").append(
		" VALUES (%s, '%s', '%s') 									\n").append(
		" ON DUPLICATE KEY UPDATE serverstate=VALUES(serverstate)	\n").toString();

	public enum TASKKIND
	{
		social,
		cache;
	}
	
	public static final String ATTR_GROUPID      = "groupid";
	public static final String ATTR_KIND         = "kind";
	public static final String ATTR_SERVERSTATUS = "state";
	public static final String ATTR_SERVER_NAME   = "name";
	
	public static final String XPATH_ATTR_GROUPID      = StringPool.AT + ATTR_GROUPID;
	public static final String XPATH_ATTR_KIND         = StringPool.AT + ATTR_KIND;
	public static final String XPATH_ATTR_SERVERSTATUS = StringPool.AT + ATTR_SERVERSTATUS;
	public static final String XPATH_ATTR_SERVERNAME   = StringPool.AT + ATTR_SERVER_NAME;
	public static final String XPATH_ATTR_SOCIAL       = StringPool.AT + TASKKIND.social;
	public static final String XPATH_ATTR_CACHE        = StringPool.AT + TASKKIND.cache;
	
	public static final String ELEM_ROOT     = "rs";
	public static final String ELEM_SERVER   = "server";
	public static final String ELEM_AFFINITY = "affinity";
	
	public static final String ON  = "on";
	public static final String OFF = "off";

	public static final String START = "startNonClustered";
	public static final String HALT  = "haltNonClustered";
}
