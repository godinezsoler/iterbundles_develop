/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.protecmedia.iter.base.service.impl;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.LongWrapper;
import com.liferay.portal.kernel.xml.Document;
import com.protecmedia.iter.base.service.base.GroupConfigServiceBaseImpl;
import com.protecmedia.iter.base.service.util.IterAdmin;

/**
 * The implementation of the group config remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.GroupConfigService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.GroupConfigServiceUtil} to access the group config remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.GroupConfigServiceBaseImpl
 * @see com.protecmedia.iter.base.service.GroupConfigServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class GroupConfigServiceImpl extends GroupConfigServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(GroupConfigServiceImpl.class);
			
	public String getRobots(String groupId) throws SystemException
	{
		try
		{
			return groupConfigLocalService.getRobotsDOM(groupId).asXML();
		}
		catch (Exception e)
		{
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(e) );
		}
	}
	
	public void setRobots(String groupId, String robots) throws SystemException
	{
		try
		{
			groupConfigLocalService.setRobots(groupId, robots);
		}
		catch (Exception e)
		{
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(e) );
		}
	}
	
	public String getGoogleTools(long groupId) throws SystemException
	{
		String result= "";
		try
		{
			result = groupConfigLocalService.getGoogleTools(groupId);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(th) );
		}
		return result;
		
	}
	
	public void setGoogleTools(long groupId, String googleConf) throws SystemException
	{
		try
		{
			groupConfigLocalService.setGoogleTools(groupId,googleConf);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(th) );
		}
		
	}
	
	public String exportRobots(String params) throws SystemException
	{
		try
		{
			LongWrapper groupId = new LongWrapper(0);
			IterAdmin.processExportImportInfo(params, groupId);

			return groupConfigLocalService.getRobotsDOM(String.valueOf(groupId.getValue())).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(th) );
		}
	}
	
	public void importRobots(String data) throws SystemException
	{
		try
		{
			LongWrapper groupId = new LongWrapper(0);
			Document dom = IterAdmin.processExportImportInfo(data, groupId);

			groupConfigLocalService.setRobots(String.valueOf(groupId.getValue()), dom.asXML());
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(th) );
		}
	}
	public String exportMetrics(String params) throws SystemException
	{
		try
		{
			LongWrapper groupId = new LongWrapper(0);
			IterAdmin.processExportImportInfo(params, groupId);

			return groupConfigLocalService.getGoogleTools(groupId.getValue());
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(th) );
		}
	}
	
	public void importMetrics(String data) throws SystemException
	{
		try
		{
			LongWrapper groupId = new LongWrapper(0);
			Document dom = IterAdmin.processExportImportInfo(data, groupId);

			groupConfigLocalService.setGoogleTools(groupId.getValue(), dom.asXML());
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(th) );
		}
	}

	
}