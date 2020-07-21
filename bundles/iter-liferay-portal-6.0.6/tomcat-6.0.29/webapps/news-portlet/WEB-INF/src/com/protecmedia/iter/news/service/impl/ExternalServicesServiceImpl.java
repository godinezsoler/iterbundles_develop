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

package com.protecmedia.iter.news.service.impl;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.news.service.base.ExternalServicesServiceBaseImpl;

/**
 * The implementation of the external services remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.news.service.ExternalServicesService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.news.service.ExternalServicesServiceUtil} to access the external services remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author protec
 * @see com.protecmedia.iter.news.service.base.ExternalServicesServiceBaseImpl
 * @see com.protecmedia.iter.news.service.ExternalServicesServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class ExternalServicesServiceImpl extends ExternalServicesServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(ExternalServicesServiceImpl.class);
	
	public String getExternalServices(String groupId) throws SystemException
	{
		try
		{
			return externalServicesLocalService.getExternalServices(groupId).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String getExternalServices(long groupId) throws SystemException
	{
		return getExternalServices(String.valueOf(groupId));
	}
	
	public String setExternalServices(String xmlData) throws SystemException
	{
		try
		{
			return externalServicesLocalService.setExternalServices(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String deleteExternalService(String xmlData) throws SystemException
	{
		try
		{
			return externalServicesLocalService.deleteExternalServices(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void disableExternalService(String xmlData) throws SystemException
	{
		try
		{
			externalServicesLocalService.disableExternalService(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void contentRequest(String serviceid, String environment) throws SystemException
	{
		try
		{
			externalServicesLocalService.contentRequest(serviceid, environment);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	

	
	public String searchDependencies(String serviceid) throws SystemException
	{
		try
		{
			return externalServicesLocalService.searchDependencies(serviceid).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void publishToLive(String groupId, String serviceids) throws SystemException
	{
		try
		{
			externalServicesLocalService.publishToLive(groupId, serviceids);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void importData(String data) throws SystemException
	{
		try
		{
			externalServicesLocalService.importData(data);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
}