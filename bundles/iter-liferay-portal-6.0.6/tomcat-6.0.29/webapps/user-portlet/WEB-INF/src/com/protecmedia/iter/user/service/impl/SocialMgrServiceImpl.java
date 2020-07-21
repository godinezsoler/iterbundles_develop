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

package com.protecmedia.iter.user.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.xml.DocumentException;
import com.protecmedia.iter.user.service.base.SocialMgrServiceBaseImpl;

@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class SocialMgrServiceImpl extends SocialMgrServiceBaseImpl
{
	
	private static Log _log = LogFactoryUtil.getLog(SocialMgrServiceImpl.class);
	
	public String getSocialConfig(String xmlData) throws ServiceError, SecurityException, DocumentException, NoSuchMethodException
	{
		String result = "";
		try
		{
			result = socialMgrLocalService.getSocialConfig(xmlData);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String setSocialConfig(String xmlData) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, IOException, SQLException, NumberFormatException, PortalException, SystemException
	{
		String result = "";
		try
		{
			result = socialMgrLocalService.setSocialConfig(xmlData);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}

	public String updateScopeSocialConfig(String xmlData) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, IOException, SQLException
	{
		String result = "";
		try
		{
			result = socialMgrLocalService.updateScopeSocialConfig(xmlData);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String getProfileAndScopes(String xmlData) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, IOException, SQLException
	{
		String result = "";
		try
		{
			result = socialMgrLocalService.getProfileAndScopes(xmlData);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String updateProfileSocialField(String xmlData) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, IOException, SQLException
	{
		String result = "";
		try
		{
			result = socialMgrLocalService.updateProfileSocialField(xmlData);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String getProfileSocialFieldsConnections(String xmlData) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, IOException, SQLException
	{
		String result = "";
		try
		{
			result = socialMgrLocalService.getProfileSocialFieldsConnections(xmlData);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String uploadLoginWithFileEntry(HttpServletRequest request, HttpServletResponse response, InputStream is, String groupId, String socialType, String itersocialconfigid) throws Exception
	{
		String result = "";
		try
		{
			result = socialMgrLocalService.uploadLoginWithFileEntry(request, response, is, Long.parseLong(groupId), socialType, itersocialconfigid);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String deleteLoginWithFileEntry(String xmlData) throws Exception
	{
		String result = "";
		try
		{
			result = socialMgrLocalService.deleteLoginWithFileEntry(xmlData);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String getSocialButtonsHTML(String groupid) throws SecurityException, NoSuchMethodException, ServiceError
	{
		String result = "";
		try
		{
			result = socialMgrLocalService.getSocialButtonsHTML(groupid);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	
	public void initStatisticsTasks() throws ServiceError{
		try
		{
			_log.trace("SocialMgrServiceImpl in initStatisticsTasks");
			socialMgrLocalService.initSocialStatisticsTasks();
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
	}
	
	
	public void updateSocialStatisticsTask(String idConfig, String previousServerAffinity) throws ServiceError, SystemException
	{ 
		try
		{
			_log.trace("SocialMgrServiceImpl in updateSocialStatisticsTask idconfig:" + idConfig);
			socialMgrLocalService.updateSocialStatisticsTask(idConfig, previousServerAffinity);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void stopSocialStatisticsTask(String idConfig, String serverAffinity) throws SystemException
	{
		try
		{
			_log.trace("SocialMgrServiceImpl in stopSocialStatisticsTask idconfig:" + idConfig + ", serverAffinity:" + serverAffinity);
			socialMgrLocalService.stopSocialStatisticsTask(idConfig, serverAffinity);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String exportData(String params) throws SystemException
	{
		try
		{
			return socialMgrLocalService.exportData(params).asXML();
		}
		catch(Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void importData(String data) throws SystemException
	{
		try
		{
			socialMgrLocalService.importData(data);
		}
		catch(Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
}