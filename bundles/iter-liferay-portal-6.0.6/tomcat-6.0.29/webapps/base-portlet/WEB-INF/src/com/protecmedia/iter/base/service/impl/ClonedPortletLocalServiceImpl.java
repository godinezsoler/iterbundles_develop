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

import java.util.List;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.base.model.ClonedPortlet;
import com.protecmedia.iter.base.service.base.ClonedPortletLocalServiceBaseImpl;

/**
 * The implementation of the cloned portlet local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.ClonedPortletLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.ClonedPortletLocalServiceUtil} to access the cloned portlet local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.ClonedPortletLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.ClonedPortletLocalServiceUtil
 */
@Transactional( isolation = Isolation.PORTAL, rollbackFor = { Exception.class } )
public class ClonedPortletLocalServiceImpl extends ClonedPortletLocalServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(ClonedPortletLocalServiceImpl.class);

	/**
	 * 
	 */
	public void destroyClones() throws Exception
	{
		try
		{
			List<ClonedPortlet> list= ClonedPortlet.readClonesFromDB();
			for (int i = 0; i < list.size(); i++)
			{
				list.get(i).destroyClone();
			}
		}
		catch (Exception e) 
		{
			throw e;
		}
		catch (Throwable th) 
		{
			throw new SystemException(th);
		}
	}

	/**
	 * 
	 */
	public void initClones() throws Exception
	{
		try
		{
			List<ClonedPortlet> list= ClonedPortlet.readClonesFromDB();
			
			for (int i = 0; i < list.size(); i++)
			{
				list.get(i).initClone();
			}
		}
		catch (Exception e) 
		{
			throw e;
		}
		catch (Throwable th) 
		{
			throw new SystemException(th);
		}
	}

}