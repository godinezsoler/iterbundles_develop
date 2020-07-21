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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.user.LoginException;
import com.protecmedia.iter.user.service.base.LoginServiceBaseImpl;

/**
 * The implementation of the login remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.user.service.LoginService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.user.service.LoginServiceUtil} to access the login remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see com.protecmedia.iter.user.service.base.LoginServiceBaseImpl
 * @see com.protecmedia.iter.user.service.LoginServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class LoginServiceImpl extends LoginServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(LoginServiceImpl.class);
	
	public String doLogin( String username, String password, boolean keepAlive, String origin, HttpServletRequest req, HttpServletResponse resp ) throws LoginException
	{
		return loginLocalService.doLogin(username, password, keepAlive, origin, req, resp, null);
	}
	
	public String doLogout( HttpServletRequest req, HttpServletResponse resp ) throws LoginException
	{
		return loginLocalService.doLogout( req, resp );
	}

	public void doSimulationLogout( HttpServletRequest request ) throws LoginException
	{
		loginLocalService.doSimulationLogout(request);
	}
	
	public void importData(String data) throws SystemException
    {
          try
          {
        	  loginLocalService.importData(data);
          }
          catch (Throwable th)
          {
                 _log.error(th);
                 throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
          }
    }
	
	public void publish(long groupId) throws SystemException
    {
          try
          {
        	  loginLocalService.publish(groupId);
          }
          catch (Throwable th)
          {
                 _log.error(th);
                 throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
          }
    }
}