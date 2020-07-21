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

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.xml.Document;
import com.protecmedia.iter.base.service.base.CommentsConfigServiceBaseImpl;

/**
 * The implementation of the comments config remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.CommentsConfigService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.CommentsConfigServiceUtil} to access the comments config remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.CommentsConfigServiceBaseImpl
 * @see com.protecmedia.iter.base.service.CommentsConfigServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class})
public class CommentsConfigServiceImpl extends CommentsConfigServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(CommentsConfigServiceImpl.class);

	/**
	 * 
	 * @param request
	 * @param response
	 * @param is
	 * @param xmlInitParams
	 * @return
	 * @throws Exception
	 */
	public String uploadLoginFileEntry(HttpServletRequest request, HttpServletResponse response, InputStream is, String groupId) throws Exception
	{
		String result = "";
		
		try
		{
			result =  commentsConfigLocalService.uploadLoginFileEntry(request, response, is, Long.parseLong(groupId));
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param groupId
	 * @return
	 * @throws Exception
	 */
	public String getConfig(long groupId) throws Exception
	{
		String result = "";
		
		try
		{
			result =  commentsConfigLocalService.getConfig(groupId);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param xmlData
	 * @return
	 * @throws Exception
	 */
	public String setConfig(String xmlData) throws Exception
	{
		String result = "";
		
		try
		{
			result =  commentsConfigLocalService.setConfig( xmlData);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param groupId
	 * @return
	 * @throws Exception
	 */
	public String getDisqusScript(long groupId) throws Exception
	{
		String result = "";
		
		try
		{
			result =  commentsConfigLocalService.getDisqusScript(groupId);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public Document getDisqusCommentsHTML(long groupId) throws Exception
	{
		Document result = null;
		
		try
		{
			result = commentsConfigLocalService.getDisqusCommentsHTML(groupId);
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
			result = commentsConfigLocalService.exportData(params).asXML();
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
			result = commentsConfigLocalService.exportData(groupId).asXML();
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
			commentsConfigLocalService.importData(data);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
}