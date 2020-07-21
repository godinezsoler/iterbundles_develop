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
import java.sql.SQLException;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.xml.DocumentException;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.base.LiveConfigurationServiceBaseImpl;

/**
 * The implementation of the live configuration remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.service.LiveConfigurationService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.xmlio.service.LiveConfigurationServiceUtil} to access the live configuration remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.xmlio.service.base.LiveConfigurationServiceBaseImpl
 * @see com.protecmedia.iter.xmlio.service.LiveConfigurationServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class LiveConfigurationServiceImpl
	extends LiveConfigurationServiceBaseImpl {

	public String getLiveConfiguration(String companyId) throws ServiceError, SecurityException, NoSuchMethodException
	{
		String result = "";
		try
		{
			result = liveConfigurationLocalService.getLiveConfigurationFlex(companyId);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String setLiveConfiguration(String xmlData) throws Exception
	{
		String result = "";
		try
		{
			result = liveConfigurationLocalService.setLiveConfigurationFlex(xmlData);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String getRemoteSystemInfo() throws Exception{
		try{
			return LiveConfigurationLocalServiceUtil.getRemoteSystemInfo();
		}		
		catch(Exception ex)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_GET_REMOTE_SYSTEM_INFO_ZYX, ex));
		}
		catch (Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_GET_REMOTE_SYSTEM_INFO_ZYX, new Exception(th)));
		}	
	}
	
	public boolean getArchiveByCompanyId(long companyId) throws Exception{		
		try{
			return LiveConfigurationLocalServiceUtil.getArchiveByCompanyId(companyId);
		} catch(Exception ex){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_GET_ARCHIVE_BY_COMPANYID_ZYX, ex));
		}
	}
	
	public void setArchiveByCompanyId(long companyId, boolean value) throws Exception {
		try{
			LiveConfigurationLocalServiceUtil.setArchiveByCompanyId(companyId, value);
		} catch(Exception ex){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_SET_ARCHIVE_BY_COMPANYID_ZYX, ex));
		}
	}	
}