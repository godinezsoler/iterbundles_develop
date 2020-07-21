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
import com.protecmedia.iter.base.service.base.ApacheMgrServiceBaseImpl;

/**
 * The implementation of the apache mgr remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.ApacheMgrService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.ApacheMgrServiceUtil} to access the apache mgr remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.ApacheMgrServiceBaseImpl
 * @see com.protecmedia.iter.base.service.ApacheMgrServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class ApacheMgrServiceImpl extends ApacheMgrServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(ApacheMgrServiceImpl.class);
	
	public void stopApacheQueueOperations() throws SystemException
	{
		try
		{
			apacheMgrLocalService.stopApacheQueueOperations();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
}