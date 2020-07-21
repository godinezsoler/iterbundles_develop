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
import com.liferay.portlet.asset.service.AssetCategoryLocalServiceUtil;
import com.protecmedia.iter.base.service.base.DevelopmentToolsServiceBaseImpl;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;

/**
 * The implementation of the development tools remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.DevelopmentToolsService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.DevelopmentToolsServiceUtil} to access the development tools remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.DevelopmentToolsServiceBaseImpl
 * @see com.protecmedia.iter.base.service.DevelopmentToolsServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class DevelopmentToolsServiceImpl extends DevelopmentToolsServiceBaseImpl 
{
	/**
	 * 
	 * @param plid
	 * @return
	 * @throws PortalException
	 * @throws SystemException
	 */
	public String getLayoutTypeSettings(long plid) throws PortalException, SystemException
	{
		return developmentToolsLocalService.getLayoutTypeSettings(plid);
	}
	
	/**
	 * 
	 */
	public void clearDBCache() throws SystemException
	{
		try
		{
			developmentToolsLocalService.clearDBCache();
		}
		catch(Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, th), th);
		}
	}
	
	/**
	 * 
	 */
	public void clearVMCache() throws SystemException
	{
		try
		{
			developmentToolsLocalService.clearVMCache();
		}
		catch(Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, th), th);
		}
	}
    	
    public void clearAllCaches() throws SystemException
    {
    	try
    	{
    		developmentToolsLocalService.clearAllCaches();
		}
		catch(Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, th), th);
		}
    }
    
    public void rebuildAssetCategoryTree(long groupId) throws SystemException
    {
    	try
    	{
    		AssetCategoryLocalServiceUtil.rebuildTree(groupId, true);
		}
		catch(Throwable th)
		{
			throw new SystemException(com.liferay.portal.kernel.error.ServiceErrorUtil.getServiceErrorAsXml(th), th);
		}

    }
}