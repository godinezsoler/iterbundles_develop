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

import java.net.URLEncoder;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.protecmedia.iter.base.service.base.NewsletterMgrServiceBaseImpl;


/**
 * The implementation of the newsletter mgr remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.NewsletterMgrService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.NewsletterMgrServiceUtil} to access the newsletter mgr remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.NewsletterMgrServiceBaseImpl
 * @see com.protecmedia.iter.base.service.NewsletterMgrServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class NewsletterMgrServiceImpl extends NewsletterMgrServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(NewsletterMgrServiceImpl.class);
	
	public String getNewsletters(String groupid) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.getNewsletters(Long.parseLong(groupid));
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String exportData(String params) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.exportData(params).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}

	
	
	public String addNewsletter(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.addNewsletter(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String updateNewsletter(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.updateNewsletter(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String deleteNewsletters(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.deleteNewsletters(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String getScheduleNewsletters(String newsletterid) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.getScheduleNewsletters(newsletterid).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String addScheduleNewsletter(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.addScheduleNewsletter(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String updateScheduleNewsletter(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.updateScheduleNewsletter(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String deleteScheduleNewsletters(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.deleteScheduleNewsletters(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public void initSchedules()
	{
		newsletterMgrLocalService.initSchedules();
	}
	
	public void schedule(String scheduleid, boolean enabled, String days, String hour) throws SystemException
	{
		try
		{
			newsletterMgrLocalService.schedule(scheduleid, enabled, days, hour);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String getScheduleSMTPServers(String scheduleid) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.getScheduleSMTPServers(scheduleid).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String addScheduleSMTPServers(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.addScheduleSMTPServers(xmlData).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public void startAlertNewslettersTask(String groupid, Date lastUpdate) throws SystemException 
	{
		try
		{
			newsletterMgrLocalService.startAlertNewslettersTask(groupid, lastUpdate);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void requestSendAlertNewsletters(String groupid, String lastUpdate) throws SystemException
	{
		try
		{
			newsletterMgrLocalService.requestSendAlertNewsletters(groupid, lastUpdate);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void sendAlertNewsletters(String groupid, String lastUpdate) throws SystemException 
	{
		try
		{
			newsletterMgrLocalService.sendAlertNewsletters(groupid, lastUpdate);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String sendNewsletter(String scheduleid) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.sendNewsletter(scheduleid);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getScheduleUsers(String scheduleid) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.getScheduleUsers(scheduleid);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String addScheduleUser(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.addScheduleUser(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String deleteScheduleUsers(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.deleteScheduleUsers(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getScheduleProducts(long groupId, String scheduleid) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.getScheduleProducts(groupId, scheduleid).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String addScheduleProducts(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.addScheduleProducts(xmlData).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getPageTemplates(String groupid) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.getPageTemplates(groupid);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getLiveServers() throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.getLiveServers();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public void requestSchedule(String scheduleid, boolean enabled, String days, String hour, String type) throws SystemException
	{
		try
		{
			newsletterMgrLocalService.requestSchedule(scheduleid, enabled, days, hour, type);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String requestLiveServers(String groupid) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.requestLiveServers(groupid);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getMyNewsletters(HttpServletRequest request, HttpServletResponse response) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.getMyNewsletters(request, response);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getMyLightNewsletters(String email, boolean licenseAcepted, HttpServletRequest request, HttpServletResponse response) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.getMyLightNewsletters(email, licenseAcepted, request, response);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}

	
	public String manageNewsletter(String optionid, boolean subscribe, HttpServletRequest request, HttpServletResponse response) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.manageNewsletter(optionid, subscribe, request, response);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String manageLightNewsletter(String email, boolean licenseAcepted, String optionid, boolean subscribe, HttpServletRequest request, HttpServletResponse response) throws SystemException
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.manageLightNewsletter(email, licenseAcepted, optionid, subscribe, request, response);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}

	
	public String getNewslettersXML(String groupid) throws SystemException 
	{
		String result = "";
		try
		{
			result = newsletterMgrLocalService.getNewslettersXML(groupid);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public void importData(String data) throws SystemException
	{
		try
		{
			newsletterMgrLocalService.importData(data);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String getNewsletterSchedulesList(long groupId, boolean onlyEnabled) throws SystemException
	{
		try
		{
			return newsletterMgrLocalService.getNewsletterSchedulesList(groupId, onlyEnabled);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String getNewsletterConfig(long groupId) throws SystemException
	{
		try
		{
			return newsletterMgrLocalService.getNewsletterConfig(groupId);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String getNewsletterConfigByName(String groupName) throws SystemException
	{
		try
		{
			long groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
			return URLEncoder.encode(newsletterMgrLocalService.getNewsletterConfig(groupId), "UTF-8");
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String setNewsletterConfig(long groupId, String config) throws SystemException
	{
		try
		{
			return newsletterMgrLocalService.setNewsletterConfig(groupId, config);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
}