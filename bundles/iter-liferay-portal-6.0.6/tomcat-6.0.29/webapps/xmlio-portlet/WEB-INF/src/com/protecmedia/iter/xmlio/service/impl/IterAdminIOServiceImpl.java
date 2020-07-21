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

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;
import com.protecmedia.iter.xmlio.service.base.IterAdminIOServiceBaseImpl;

/**
 * The implementation of the iter admin i o remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.service.IterAdminIOService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.xmlio.service.IterAdminIOServiceUtil} to access the iter admin i o remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.xmlio.service.base.IterAdminIOServiceBaseImpl
 * @see com.protecmedia.iter.xmlio.service.IterAdminIOServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class IterAdminIOServiceImpl extends IterAdminIOServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(IterAdminIOServiceImpl.class);
	
	private ConcurrentMap<String, String> _exportInfo = new ConcurrentHashMap<String, String>();
	
	public void finishImport(long siteId) throws SystemException
	{
		try
		{
			iterAdminIOLocalService.finishImport(siteId);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}

	public void unsetOngoingImportSubprocess(long siteId) throws SystemException
	{
		try
		{
			iterAdminIOLocalService.unsetOngoingImportSubprocessWrapper(siteId);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void finishExport(long siteId) throws SystemException
	{
		try
		{
			iterAdminIOLocalService.finishExport(siteId);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}

	
	public void exportObjects(HttpServletRequest request, HttpServletResponse response, String siteID, String objectsSpec) throws SystemException
	{
		try
		{
			iterAdminIOLocalService.exportObjects(request, response, objectsSpec);
		}
		catch (Throwable th)
		{
			_log.error(th);
			
			String errorMsg = ServiceErrorUtil.getServiceErrorAsXml(th);
			_exportInfo.put(siteID, errorMsg);
			
			response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void exportAllObjects(HttpServletRequest request, HttpServletResponse response, String siteID) throws SystemException
	{
		try
		{
			iterAdminIOLocalService.exportAllObjects(request, response, siteID);
		}
		catch (Throwable th)
		{
			_log.error(th);
			
			String errorMsg = ServiceErrorUtil.getServiceErrorAsXml(th);
			_exportInfo.put(siteID, errorMsg);
			
			response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			throw new SystemException(errorMsg);
		}
	}

	public void getLastExportError(long siteID) throws SystemException
	{
		try
		{
			String lastError = _exportInfo.get( String.valueOf(siteID) );
			if (Validator.isNull(lastError))
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_ITERADMIN_EXPORT_UNEXPECTED_ZYX);
			else
				throw new SystemException(lastError);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public Object checkObjectsToImport(HttpServletRequest request, HttpServletResponse response, InputStream is, String siteId, String forceUnlock) throws IOException, SystemException
	{
		Object result = null;
		try
		{
			result = iterAdminIOLocalService.checkObjectsToImport(request, response, is, siteId, GetterUtil.getBoolean(forceUnlock));
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public void importObjects(long siteId, boolean updtIfExist) throws SystemException
	{
		try
		{
			iterAdminIOLocalService.importObjects(siteId, updtIfExist);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}

	public void abortImport(long siteId) throws SystemException
	{
		try
		{
			iterAdminIOLocalService.abortImport(siteId);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
}