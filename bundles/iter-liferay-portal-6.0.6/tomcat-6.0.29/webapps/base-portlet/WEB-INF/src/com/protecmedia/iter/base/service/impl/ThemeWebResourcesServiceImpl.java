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
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.xml.Document;
import com.protecmedia.iter.base.service.base.ThemeWebResourcesServiceBaseImpl;

/**
 * The implementation of the theme web resources remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.ThemeWebResourcesService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.ThemeWebResourcesServiceUtil} to access the theme web resources remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.ThemeWebResourcesServiceBaseImpl
 * @see com.protecmedia.iter.base.service.ThemeWebResourcesServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class ThemeWebResourcesServiceImpl extends ThemeWebResourcesServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(ThemeWebResourcesServiceImpl.class);
	
	public String preDeliverTheme(String themeSpec) throws SystemException 
	{
		String result = "";
		try
		{
			result = themeWebResourcesLocalService.preDeliverTheme(themeSpec).asXML();
		}
		catch(Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public void deliverTheme(String themeSpec) throws SystemException 
	{
		try
		{
			themeWebResourcesLocalService.deliverTheme(themeSpec);
		}
		catch(Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String getWebResourceByPlidAndPlace(long plid, String place, String contentType)	throws SystemException 
	{
		try
		{
			return themeWebResourcesLocalService.getWebResourceByPlidAndPlace(plid, place, contentType);
		}
		catch(Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void unlockProcess(long groupId) throws SystemException
	{
		try
		{
			themeWebResourcesLocalService.unlockProcess(groupId);
		}
		catch(Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}

}