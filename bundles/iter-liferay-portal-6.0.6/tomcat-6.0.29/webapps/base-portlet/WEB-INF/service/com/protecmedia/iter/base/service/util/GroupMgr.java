package com.protecmedia.iter.base.service.util;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;

public class GroupMgr 
{
	public static void updatePublicationDate(String grpName) throws IOException, SQLException, ServiceError
	{
		try
		{
			com.liferay.portal.kernel.util.GroupMgr.updatePublicationDate(grpName);
		}
		catch (com.liferay.portal.kernel.error.ServiceError serviceError)
		{
			throw new ServiceError(serviceError);
		}
	}

	public static void updatePublicationDate(String grpName, Date pubDate) throws IOException, ServiceError, SQLException
	{
		try
		{
			com.liferay.portal.kernel.util.GroupMgr.updatePublicationDate(grpName, pubDate);
		}
		catch (com.liferay.portal.kernel.error.ServiceError serviceError)
		{
			throw new ServiceError(serviceError);
		}
	}
	
	public static Date getPublicationDate(long groupId)
	{
		return com.liferay.portal.kernel.util.GroupMgr.getPublicationDate(groupId);
	}

	public static long getGlobalGroupId() 
	{
		return com.liferay.portal.kernel.util.GroupMgr.getGlobalGroupId();
	}

	public static void setGlobalGroupId(long value) 
	{
		com.liferay.portal.kernel.util.GroupMgr.setGlobalGroupId(value);
	}
	
	public static long getDefaultUserId()
	{
		return com.liferay.portal.kernel.util.GroupMgr.getDefaultUserId();
	}

	public static void setDefaultUserId(long value) throws SecurityException, NoSuchMethodException 
	{
		com.liferay.portal.kernel.util.GroupMgr.setDefaultUserId(value);
	}
	
	public static String getScopeGroupId(long defaultGroupId)
	{
		return com.liferay.portal.kernel.util.GroupMgr.getScopeGroupId(defaultGroupId);
	}
	
	/**
	 * 
	 * @return
	 * @throws PortalException
	 * @throws SystemException
	 */
	public static long getCompanyId() throws PortalException, SystemException
	{
		return com.liferay.portal.kernel.util.GroupMgr.getCompanyId();
	}
}
