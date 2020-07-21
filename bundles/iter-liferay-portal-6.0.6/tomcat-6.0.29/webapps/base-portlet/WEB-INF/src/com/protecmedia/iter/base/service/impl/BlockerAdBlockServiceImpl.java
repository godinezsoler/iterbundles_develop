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
import com.liferay.portal.kernel.util.LongWrapper;
import com.liferay.portal.kernel.xml.Document;
import com.protecmedia.iter.base.service.base.BlockerAdBlockServiceBaseImpl;
import com.protecmedia.iter.base.service.util.IterAdmin;

/**
 * The implementation of the blocker ad block remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.BlockerAdBlockService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.BlockerAdBlockServiceUtil} to access the blocker ad block remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.BlockerAdBlockServiceBaseImpl
 * @see com.protecmedia.iter.base.service.BlockerAdBlockServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class BlockerAdBlockServiceImpl extends BlockerAdBlockServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(BlockerAdBlockServiceImpl.class);
	
	public String getDataBlockerAdBlock( long groupId, String filters, String beginRegister, String maxRegisters, String actualSort )throws SystemException
	{
		String result= "";
		try
		{
			result = blockerAdBlockLocalService.getDataBlockerAdBlock( groupId, filters, beginRegister, maxRegisters, actualSort);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getConfBlockerAdBlock(long groupId) throws SystemException
	{
		String result= "";
		try
		{
			result = blockerAdBlockLocalService.getConfBlockerAdBlock(groupId);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;	
	}
	
	public String setConfBlockerAdBlock(String xmlData) throws SystemException
	{
		String result= "";
		try
		{
			result = blockerAdBlockLocalService.setConfBlockerAdBlock(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;	
	}
	
	public String exportConf(String params) throws SystemException
	{
		try
		{
			// Se exporta
			LongWrapper groupId = new LongWrapper(0);
			IterAdmin.processExportImportInfo(params, groupId);

			return blockerAdBlockLocalService.getConfBlockerAdBlock(groupId.getValue());
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(th) );
		}
	}
	
	public void importConf(String data) throws SystemException
	{
		try
		{
			LongWrapper groupId = new LongWrapper(0);
			Document dom = IterAdmin.processExportImportInfo(data, groupId);

			blockerAdBlockLocalService.setConfBlockerAdBlock(groupId.getValue(), dom);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(th) );
		}
	}

}