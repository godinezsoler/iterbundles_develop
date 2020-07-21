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
import java.sql.SQLException;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.xml.DocumentException;
import com.protecmedia.iter.user.service.base.UserOperationsServiceBaseImpl;

/**
 * The implementation of the user operations remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.user.service.UserOperationsService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.user.service.UserOperationsServiceUtil} to access the user operations remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see com.protecmedia.iter.user.service.base.UserOperationsServiceBaseImpl
 * @see com.protecmedia.iter.user.service.UserOperationsServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class})
public class UserOperationsServiceImpl extends UserOperationsServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(UserOperationsServiceImpl.class);
	
	public String getConfig(String groupid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		String result = "";
		try
		{
			result = userOperationsLocalService.getConfig(groupid);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String setConfig(String xmlData) throws Exception
	{
		String result = "";
		try
		{
			result = userOperationsLocalService.setConfig(xmlData);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String exportData(String params) throws SystemException
    {
          try
          {
        	  return userOperationsLocalService.exportData(params).asXML();
          }
          catch (Throwable th)
          {
                 _log.error(th);
                 throw new SystemException(com.liferay.portal.kernel.error.ServiceErrorUtil.getServiceErrorAsXml(th));
          }
    }
	
	public void importData(String data) throws SystemException
    {
          try
          {
        	  userOperationsLocalService.importData(data);
          }
          catch (Throwable th)
          {
                 _log.error(th);
                 throw new SystemException(com.liferay.portal.kernel.error.ServiceErrorUtil.getServiceErrorAsXml(th));
          }
    }
}