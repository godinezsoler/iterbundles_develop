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
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.base.service.base.FormTransformServiceBaseImpl;

/**
 * The implementation of the form transform remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.FormTransformService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.FormTransformServiceUtil} to access the form transform remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.FormTransformServiceBaseImpl
 * @see com.protecmedia.iter.base.service.FormTransformServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class FormTransformServiceImpl extends FormTransformServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(FormTransformServiceImpl.class);

	public String getTransforms( long groupId ) throws SystemException
	{
		String result = "";
		
		try
		{
			result =  formTransformLocalService.getTransforms( groupId );
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public Object getUrlXsl(HttpServletRequest request, HttpServletResponse response, String groupid, String  xslUUID) throws SystemException
	{
		try
		{
			formTransformLocalService.getUrlXsl(request, response, groupid, xslUUID);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}

		return null;
	}
	
	public String addTransform( String xmlData ) throws SystemException
	{
		String result = "";
		
		try
		{
			result =  formTransformLocalService.addTransform( xmlData );
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String editTransform( String xmlData ) throws SystemException
	{
		String result = "";
		
		try
		{
			result =  formTransformLocalService.editTransform( xmlData );
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String deleteTransform(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = formTransformLocalService.deleteTransform(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return retVal;
	}
	
	public String cancelOperation(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = formTransformLocalService.cancelOperation(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return retVal;
	}
	
	public String uploadXslFileEntry(HttpServletRequest request, HttpServletResponse response, InputStream is, String xmlData ) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = formTransformLocalService.uploadXslFileEntry(request, response, is, xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return retVal;
	}
	
	public void publishToLive(String transformIds) throws SystemException
	{
		try
		{
			formTransformLocalService.publishToLive(transformIds);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public boolean importTransforms(String transforms) throws SystemException
	{
		boolean success = false;
		try
		{
			formTransformLocalService.importTransforms(transforms);
			success = true;
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return success;
	}
	
	public String getTransformsToSection(long groupId, long plid) throws SystemException
	{
		String result = "";
		try
		{
			result = formTransformLocalService.getTransformsToSection(groupId, plid);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String addTransformToSectionToList(String autorssxsl, String plid) throws SystemException
	{
		String result = "";
		try
		{
			result = formTransformLocalService.addTransformToSectionToList(autorssxsl, plid);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String deleteTransformToSectionToList(String plid) throws SystemException
	{
		String result = "";
		try
		{
			result = formTransformLocalService.deleteTransformToSectionToList(plid);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String addTransformToSection(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = formTransformLocalService.addTransformToSection(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String editTransformToSection(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = formTransformLocalService.editTransformToSection(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getRssAdvancedProperties(long groupId) throws SystemException
	{
		String result = "";
		try
		{
			result = formTransformLocalService.getRssAdvancedProperties(groupId);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String addRssAdvanced(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = formTransformLocalService.addRssAdvanced(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String updateRssAdvanced(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = formTransformLocalService.updateRssAdvanced(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String deleteRssAdvanced(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = formTransformLocalService.deleteRssAdvanced(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getImageFrame(long groupId) throws SystemException
	{
		String result = "";
		try
		{
			result = formTransformLocalService.getImageFrame(groupId);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}

}