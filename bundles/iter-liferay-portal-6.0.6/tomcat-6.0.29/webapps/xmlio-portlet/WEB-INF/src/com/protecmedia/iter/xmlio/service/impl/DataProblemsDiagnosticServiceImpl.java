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
import com.liferay.portal.kernel.util.Validator;
import com.protecmedia.iter.xmlio.service.base.DataProblemsDiagnosticServiceBaseImpl;

/**
 * The implementation of the data problems diagnostic remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.service.DataProblemsDiagnosticService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.xmlio.service.DataProblemsDiagnosticServiceUtil} to access the data problems diagnostic remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.xmlio.service.base.DataProblemsDiagnosticServiceBaseImpl
 * @see com.protecmedia.iter.xmlio.service.DataProblemsDiagnosticServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class DataProblemsDiagnosticServiceImpl extends DataProblemsDiagnosticServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(DataProblemsDiagnosticServiceImpl.class);
	
	private String _downloadInfo = "";
	
	
	public String getStateCaptureProcess() throws SystemException
	{
		String result= "";
		try
		{
			result = dataProblemsDiagnosticLocalService.getStateCaptureProcess();
		}
		catch (Throwable th) 
		{
			_log.error(th);
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public void captureData(String onlyLogs) throws SystemException
	{
		try
		{
			dataProblemsDiagnosticLocalService.captureData(onlyLogs);
		}
		catch (Throwable th) 
		{
			_log.error(th);
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
	}
	
	public void startCaptureData(String onlyLogs,String catFileName) throws SystemException
	{
		try
		{
			dataProblemsDiagnosticLocalService.startCaptureData(onlyLogs,catFileName);
		}
		catch (Throwable th) 
		{
			_log.error(th);
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
	}
	
	public void stopCaptureData(HttpServletRequest request, HttpServletResponse response) throws SystemException
	{
		try
		{
			dataProblemsDiagnosticLocalService.stopCaptureData(request, response);
		}
		catch (Throwable th) 
		{
			_log.error(th);
			
			String errorMsg = ServiceErrorUtil.getServiceErrorAsXml(th);
			_downloadInfo = errorMsg;
			
			response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			throw new SystemException(errorMsg);
		}
		
	}
	
	public void stopProcess() throws SystemException
	{
		try
		{
			dataProblemsDiagnosticLocalService.stopProcess();
		}
		catch (Throwable th) 
		{
			_log.error(th);
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
	}
	
	public void getLastDownloadError() throws SystemException
	{
		try
		{
			String lastError = _downloadInfo;
			if (Validator.isNull(lastError))
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
			else
				throw new SystemException(lastError);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void cleanData() throws SystemException
	{
		try
		{
			dataProblemsDiagnosticLocalService.cleanData();
		}
		catch (Throwable th) 
		{
			_log.error(th);
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void createZipLiveData( String namesPkgImportation, String nameImpFiles  ) throws SystemException
	{
		try
		{
			dataProblemsDiagnosticLocalService.createZipLiveData(namesPkgImportation, nameImpFiles);
		}
		catch (Throwable th) 
		{
			_log.error(th);
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
	}
}