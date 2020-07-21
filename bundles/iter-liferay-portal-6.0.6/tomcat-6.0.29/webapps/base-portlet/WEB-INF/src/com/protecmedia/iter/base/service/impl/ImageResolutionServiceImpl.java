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
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.protecmedia.iter.base.service.base.ImageResolutionServiceBaseImpl;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;

/**
 * The implementation of the image resolution remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.ImageResolutionService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.ImageResolutionServiceUtil} to access the image resolution remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.ImageResolutionServiceBaseImpl
 * @see com.protecmedia.iter.base.service.ImageResolutionServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class ImageResolutionServiceImpl extends ImageResolutionServiceBaseImpl 
{
	public void importResolutions(String xmlData) throws Exception
	{
		try
		{
			ImageResolutionLocalServiceImpl.importResolutions(xmlData);
		} 
		catch (SystemException se)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, se), se);
		}
		catch (PortalException pe)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, pe), pe);
		}
		catch(Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, th), th);
		}
	}
}