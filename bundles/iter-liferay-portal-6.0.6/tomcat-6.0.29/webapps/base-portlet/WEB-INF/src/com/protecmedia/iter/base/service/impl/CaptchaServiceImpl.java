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

import java.io.IOException;
import java.sql.SQLException;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.xml.DocumentException;
import com.protecmedia.iter.base.service.base.CaptchaServiceBaseImpl;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;

/**
 * The implementation of the captcha remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.CaptchaService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.CaptchaServiceUtil} to access the captcha remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.CaptchaServiceBaseImpl
 * @see com.protecmedia.iter.base.service.CaptchaServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class CaptchaServiceImpl extends CaptchaServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(CaptchaServiceImpl.class);
	
	public String getCaptcha( long groupId ) throws ServiceError, SecurityException, NoSuchMethodException
	{
		String result = "";
		
		try
		{
			result =  captchaLocalService.getCaptcha( groupId );
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String setCaptcha( String xmlData ) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, IOException, SQLException
	{
		String result = "";
		
		try
		{
			result =  captchaLocalService.setCaptcha( xmlData );
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public void importData(String data) throws SystemException
    {
          try
          {
        	  captchaLocalService.importData(data);
          }
          catch (Throwable th)
          {
                 _log.error(th);
                 throw new SystemException(com.liferay.portal.kernel.error.ServiceErrorUtil.getServiceErrorAsXml(th));
          }
    }
	
	public void publish(long groupId) throws SystemException
    {
          try
          {
        	  captchaLocalService.publish(groupId);
          }
          catch (Throwable th)
          {
                 _log.error(th);
                 throw new SystemException(com.liferay.portal.kernel.error.ServiceErrorUtil.getServiceErrorAsXml(th));
          }
    }
}