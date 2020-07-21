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
import com.protecmedia.iter.base.service.base.CommunitiesServiceBaseImpl;

/**
 * The implementation of the communities remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.CommunitiesService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.CommunitiesServiceUtil} to access the communities remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.CommunitiesServiceBaseImpl
 * @see com.protecmedia.iter.base.service.CommunitiesServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class CommunitiesServiceImpl extends CommunitiesServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(CommunitiesServiceImpl.class);

	public void importDataSearch(String data) throws SystemException
    {
          try
          {
        	  communitiesLocalService.importDataSearch(data);
          }
          catch (Throwable th)
          {
                 _log.error(th);
                 throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
          }
    }
	
	public void publishSearch(long groupId) throws SystemException
    {
          try
          {
        	  communitiesLocalService.publishSearch(groupId);
          }
          catch (Throwable th)
          {
                 _log.error(th);
                 throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
          }
    }
	
	public void importDataLastUpdate(String data) throws SystemException
    {
          try
          {
        	  communitiesLocalService.importDataLastUpdate(data);
          }
          catch (Throwable th)
          {
                 _log.error(th);
                 throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
          }
    }
	
	public void publishLastUpdate(long groupId) throws SystemException
    {
          try
          {
        	  communitiesLocalService.publishLastUpdate(groupId);
          }
          catch (Throwable th)
          {
                 _log.error(th);
                 throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
          }
    }
}