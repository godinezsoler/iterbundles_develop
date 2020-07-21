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

package com.protecmedia.iter.xmlio.service.impl;

import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.xmlio.service.base.WebsiteIOServiceBaseImpl;

/**
 * The implementation of the website i o remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.service.WebsiteIOService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.xmlio.service.WebsiteIOServiceUtil} to access the website i o remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see http://10.15.20.59:8090/x/q4YFAg
 * @see com.protecmedia.iter.xmlio.service.base.WebsiteIOServiceBaseImpl
 * @see com.protecmedia.iter.xmlio.service.WebsiteIOServiceUtil
 */
public class WebsiteIOServiceImpl extends WebsiteIOServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(WebsiteIOServiceImpl.class);
	
	public String computeXPortDependencies(String objectsSpec) throws SystemException
	{
		String result = "";
		
		try
		{
			result = websiteIOLocalService.computeXPortDependencies(objectsSpec).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String exportObjects(String objectsSpec) throws SystemException
	{
		String result = "";
		
		try
		{
			result = websiteIOLocalService.exportObjects(objectsSpec).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public void abortExport(long siteId) throws SystemException
	{
		try
		{
			websiteIOLocalService.abortExport(siteId);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void importPreProcessInfo(long siteId, String mlnstationInfo, String content) throws SystemException
	{
		try
		{
			websiteIOLocalService.importPreProcessInfo(siteId, mlnstationInfo, content);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void importObject(long siteid, String id, String className, String content) throws SystemException
	{
		try
		{
			websiteIOLocalService.importObject(siteid, id, className, content);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void importPostProcessInfo(long siteId, String content, String relationships) throws SystemException
	{
		try
		{
			websiteIOLocalService.importPostProcessInfo(siteId, content, relationships);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void resetImport(long siteid, String mlnstationInfo) throws SystemException
	{
		try
		{
			websiteIOLocalService.resetImport(siteid, mlnstationInfo);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
}