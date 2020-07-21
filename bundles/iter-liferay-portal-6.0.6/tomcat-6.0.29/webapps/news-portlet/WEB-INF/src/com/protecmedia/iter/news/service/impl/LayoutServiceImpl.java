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

package com.protecmedia.iter.news.service.impl;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.news.service.base.LayoutServiceBaseImpl;

/**
 * The implementation of the layout remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.news.service.LayoutService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.news.service.LayoutServiceUtil} to access the layout remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author protec
 * @see com.protecmedia.iter.news.service.base.LayoutServiceBaseImpl
 * @see com.protecmedia.iter.news.service.LayoutServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class LayoutServiceImpl extends LayoutServiceBaseImpl
{

	public void setHidden(long plid, boolean hidden) throws Exception 
	{	
		try
		{
			layoutLocalService.setHidden(plid, hidden);
		}
		catch(Exception ex)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_UPDATE_LAYOUT_ZYX, ex));
		}
	}
	
	public String addLayout(long groupId, long parentLayoutId, String xml) throws SystemException
	{
		try
		{
			return layoutLocalService.addLayout(groupId, parentLayoutId, xml).asXML();
		}
		catch(Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String updateLayout(long plid, String xml) throws SystemException
	{
		try
		{
			return layoutLocalService.updateLayout(plid, xml).asXML();
		}
		catch(Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void setGroupDefaultProperties(long groupId, String xml) throws SystemException
	{
		try
		{
			layoutLocalService.setGroupDefaultProperties(groupId, xml);
		}
		catch(Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
}