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

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.xml.DocumentException;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.xmlio.service.base.LiveControlServiceBaseImpl;

/**
 * The implementation of the live control remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.service.LiveControlService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.xmlio.service.LiveControlServiceUtil} to access the live control remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.xmlio.service.base.LiveControlServiceBaseImpl
 * @see com.protecmedia.iter.xmlio.service.LiveControlServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class LiveControlServiceImpl extends LiveControlServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(LiveControlServiceImpl.class);
	
	//////////////////////////////////////////////////////////////////////////////////////////////////
	private String specialCase(String errorCode, ServiceError errorSource) throws Exception
	{
		String strValue = "";
		
		if ( errorSource.getErrorCode().equals(IterErrorKeys.XYZ_E_CONTROL_IN_PROCESS_NOT_EXIST_ZYX) )
			strValue = errorSource.getMessage();
		else
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(errorCode, errorSource));	
		
		return strValue;
	}

	public void unlockLiveControl(String processId) throws Exception
	{
	/*	try
		{
			liveControlLocalService.unlockLiveControl(processId);
		}
		catch(ServiceError e)
		{
			specialCase(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, e);
		}
		catch(Exception ex)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, ex));
		}
		catch (Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, new Exception(th)));
		}*/
	}
	
	public String getAllRecordsFlex(String xmlFilters, String startIn, String limit, String sort) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException{
		String result = null;
		try
		{			
			result = liveControlLocalService.getAllRecordsFlex(xmlFilters, startIn, limit, sort);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		return result;
	}
	
	public void interruptPublication(String processId) throws Exception
	{
		try
		{
			liveControlLocalService.interruptPublication(processId);		
		}
		catch(Throwable th)
		{
			_log.error(th);
			throw new Exception(com.liferay.portal.kernel.error.ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
}