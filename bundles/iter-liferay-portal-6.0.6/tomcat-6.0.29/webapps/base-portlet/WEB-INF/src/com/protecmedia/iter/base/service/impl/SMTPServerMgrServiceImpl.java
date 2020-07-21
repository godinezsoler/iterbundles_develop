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

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.xml.DocumentException;
import com.protecmedia.iter.base.service.base.SMTPServerMgrServiceBaseImpl;

/**
 * The implementation of the s m p t server mgr remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.SMPTServerMgrService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.SMPTServerMgrServiceUtil} to access the s m p t server mgr remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.SMPTServerMgrServiceBaseImpl
 * @see com.protecmedia.iter.base.service.SMPTServerMgrServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class SMTPServerMgrServiceImpl extends SMTPServerMgrServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(SMTPServerMgrServiceImpl.class);
	
	public String getServers(String groupid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		String result = "";
		try
		{
			result = smtpServerMgrLocalService.getServers( Long.parseLong(groupid) ).asXML();
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String exportData(String params) throws SystemException
	{
		String result = "";
		try
		{
			result = smtpServerMgrLocalService.exportData(params).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String exportData(Long groupId) throws SystemException
	{
		String result = "";
		try
		{
			result = smtpServerMgrLocalService.exportData(groupId).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String exportData(List<String> ids) throws SystemException
	{
		String result = "";
		try
		{
			result = smtpServerMgrLocalService.exportData(ids).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String addServer(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = smtpServerMgrLocalService.addServer(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String updateServer(String xmlData) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, IOException, SQLException
	{
		String result = "";
		try
		{
			result = smtpServerMgrLocalService.updateServer(xmlData);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String deleteServers(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = smtpServerMgrLocalService.deleteServers(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String importData(String data) throws SystemException
	{
		try
		{
			 return smtpServerMgrLocalService.importData(data).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void publishToLive(String serverIds) throws SystemException
	{
		try
		{
			 smtpServerMgrLocalService.publishToLive(serverIds);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
}