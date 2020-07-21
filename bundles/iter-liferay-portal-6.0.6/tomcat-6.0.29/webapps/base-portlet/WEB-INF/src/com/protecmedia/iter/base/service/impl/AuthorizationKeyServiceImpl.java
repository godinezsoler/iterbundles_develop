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
import com.liferay.portal.kernel.xml.Document;
import com.protecmedia.iter.base.service.base.AuthorizationKeyServiceBaseImpl;

/**
 * The implementation of the authorization key remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.AuthorizationKeyService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.AuthorizationKeyServiceUtil} to access the authorization key remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.AuthorizationKeyServiceBaseImpl
 * @see com.protecmedia.iter.base.service.AuthorizationKeyServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class AuthorizationKeyServiceImpl extends AuthorizationKeyServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(AuthorizationKeyServiceImpl.class);
	
	public String getAuthorizationKeys(String groupId) throws SystemException
	{
		 try
         {
			 return authorizationKeyLocalService.getAuthorizationKeys(groupId).asXML();
         }
         catch (Throwable th)
         {
                _log.error(th);
                throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
         }
	}
	
	public String addAuthorizationKey(String xmlData) throws SystemException
	{
		 try
         {
			 return authorizationKeyLocalService.addAuthorizationKey(xmlData);
         }
         catch (Throwable th)
         {
                _log.error(th);
                throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
         }
	}
	
	public void updateAuthorizationKey(String xmlData) throws SystemException
	{
		 try
         {
			 authorizationKeyLocalService.updateAuthorizationKey(xmlData);
         }
         catch (Throwable th)
         {
                _log.error(th);
                throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
         }
	}
	
	public String deleteAuthorizationKey(String xmlData) throws SystemException
	{
		 try
         {
			 return authorizationKeyLocalService.deleteAuthorizationKey(xmlData);
         }
         catch (Throwable th)
         {
                _log.error(th);
                throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
         }
	}
	
	public void enableAuthorizationKey(String xmlData) throws SystemException
	{
		 try
         {
			 authorizationKeyLocalService.enableAuthorizationKey(xmlData);
         }
         catch (Throwable th)
         {
                _log.error(th);
                throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
         }
	}
}