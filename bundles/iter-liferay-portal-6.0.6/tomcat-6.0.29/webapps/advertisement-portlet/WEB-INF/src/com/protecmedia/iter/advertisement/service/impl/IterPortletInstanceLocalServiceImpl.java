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

package com.protecmedia.iter.advertisement.service.impl;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.protecmedia.iter.advertisement.service.base.IterPortletInstanceLocalServiceBaseImpl;

/**
 * The implementation of the iter portlet instance local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.advertisement.service.IterPortletInstanceLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.advertisement.service.IterPortletInstanceLocalServiceUtil} to access the iter portlet instance local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author protec
 * @see com.protecmedia.iter.advertisement.service.base.IterPortletInstanceLocalServiceBaseImpl
 * @see com.protecmedia.iter.advertisement.service.IterPortletInstanceLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class IterPortletInstanceLocalServiceImpl extends IterPortletInstanceLocalServiceBaseImpl 
{
	public javax.portlet.Portlet getPortletInstance(String instanceClass) throws ClassNotFoundException, InstantiationException, IllegalAccessException
	{
		Class<?> comObject = Class.forName(instanceClass);
		return (javax.portlet.Portlet)comObject.newInstance();
	}
}