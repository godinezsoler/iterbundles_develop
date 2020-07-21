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

import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LongWrapper;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.protecmedia.iter.base.service.base.URLShortenerServiceBaseImpl;
import com.protecmedia.iter.base.service.util.IterAdmin;

/**
 * The implementation of the u r l shortener remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.URLShortenerService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.URLShortenerServiceUtil} to access the u r l shortener remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.URLShortenerServiceBaseImpl
 * @see com.protecmedia.iter.base.service.URLShortenerServiceUtil
 */
public class URLShortenerServiceImpl extends URLShortenerServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(URLShortenerServiceImpl.class);
	
	public String setShorteners(long groupId, String data) throws SystemException
	{
		try
		{
			return urlShortenerLocalService.setShorteners(groupId, data);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String getShorteners(long groupId) throws SystemException
	{
		try
		{
			return urlShortenerLocalService.getShorteners(groupId);
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
			LongWrapper groupId = new LongWrapper(0);
			IterAdmin.processExportImportInfo(params, groupId);
			return urlShortenerLocalService.getShorteners(groupId.getValue());
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
			Document dom = SAXReaderUtil.read(data);
			boolean updtIfExist = GetterUtil.getBoolean(XMLHelper.getStringValueOf(dom.getRootElement(), "@updtIfExist"), false);
			LongWrapper groupId = new LongWrapper(0);
			
			// Elimina del xml los atributos propios de la importación
			dom = IterAdmin.processExportImportInfo(data, groupId);
			
			urlShortenerLocalService.mergeShorteners(groupId.getValue(), dom, updtIfExist);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
}