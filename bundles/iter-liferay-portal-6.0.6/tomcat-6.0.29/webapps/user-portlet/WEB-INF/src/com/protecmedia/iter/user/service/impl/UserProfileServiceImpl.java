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

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.user.service.base.UserProfileServiceBaseImpl;

/**
 * The implementation of the user profile remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.user.service.UserProfileService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.user.service.UserProfileServiceUtil} to access the user profile remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see com.protecmedia.iter.user.service.base.UserProfileServiceBaseImpl
 * @see com.protecmedia.iter.user.service.UserProfileServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class UserProfileServiceImpl extends UserProfileServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(UserProfileServiceImpl.class);

	public String getUserProfile( String groupid ) throws SystemException
	{
		String result = "";
		
		try
		{
			result =  userProfileLocalService.getUserProfile(groupid).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String addField( String xmlfield ) throws SystemException
	{
		String result = "";
		
		try
		{
			result =  userProfileLocalService.addField( xmlfield );
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String updateField( String xmlfield ) throws SystemException
	{
		String result = "";
		
		try
		{
			result =  userProfileLocalService.updateField( xmlfield );
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String deleteFields( String xmlfields ) throws SystemException
	{
		String result = "";
		
		try
		{
			result =  userProfileLocalService.deleteFields( xmlfields );
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public void publishToLive(String prfIds, String groupName) throws SystemException
	{
		try
		{
			userProfileLocalService.publishToLive(prfIds, groupName);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void importFields(String fields) throws SystemException
	{
		try
		{
			userProfileLocalService.importFields(fields);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void updateProfileFieldsIds(String fields) throws SystemException
	{
		try
		{
			userProfileLocalService.updateProfileFieldsIds(fields);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
}