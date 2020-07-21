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
import com.protecmedia.iter.base.service.base.RssServiceBaseImpl;
import com.protecmedia.iter.base.service.util.IterAdmin;

/**
 * The implementation of the rss remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.RssService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.RssServiceUtil} to access the rss remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.RssServiceBaseImpl
 * @see com.protecmedia.iter.base.service.RssServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class RssServiceImpl extends RssServiceBaseImpl {
	private static Log _log = LogFactoryUtil.getLog(RssServiceImpl.class);

	public String getSectionBasedRss(long groupId) throws SystemException
	{
		String result = "";
		try
		{
			result = rssLocalService.getSectionBasedRss(groupId).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String setSectionBasedRss(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = rssLocalService.setSectionBasedRss(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getSectionBasedRssGroupConfig(long groupId) throws SystemException
	{
		String result = "";
		try
		{
			result = rssLocalService.getSectionBasedRssGroupConfig(groupId).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String setSectionBasedRssGroupConfig(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = rssLocalService.setSectionBasedRssGroupConfig(xmlData).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getAdvancedRssList(long groupId, boolean updateRssMap) throws SystemException
	{
		String result = "";
		try
		{
			result = rssLocalService.getAdvancedRssList(groupId, updateRssMap).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String setAdvancedRss(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = rssLocalService.setAdvancedRss(xmlData).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String deleteAdvancedRss(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = rssLocalService.deleteAdvancedRss(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getInheritableRss(long groupId, String advancedRssId) throws SystemException
	{
		String result = "";
		try
		{
			result = rssLocalService.getInheritableRss(groupId, advancedRssId);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String exportRSSAdvanced(String params) throws SystemException
	{
		String data = "";
		try
		{
			data = rssLocalService.exportRSSAdvanced(params).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(th) );
		}
		return data;
	}
	
	public String exportRSSSections(String params) throws SystemException
	{
		String data = "";
		try
		{
			data = rssLocalService.exportRSSSections(params).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(th) );
		}
		return data;
	}
	
	public void importRSSSections(String data) throws SystemException
	{
		try
		{
			rssLocalService.importRSSSections(data);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(th) );
		}
	}
}