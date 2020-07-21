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
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.protecmedia.iter.base.service.base.FramesServiceBaseImpl;

/**
 * The implementation of the frames remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.FramesService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.FramesServiceUtil} to access the frames remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.FramesServiceBaseImpl
 * @see com.protecmedia.iter.base.service.FramesServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class FramesServiceImpl extends FramesServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(FramesServiceImpl.class);
	
	public String addFrame(long groupId, String xml) throws SystemException
	{
		try
		{
			return framesLocalService.addOrUpdate(groupId, null, xml, true);
		}
		catch (Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String updateFrame(String frameId, String xml) throws SystemException
	{
		try
		{
			return framesLocalService.addOrUpdate(0, frameId, xml, true);
		}
		catch (Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String deleteFrame(String frameId) throws SystemException
	{
		try
		{
			return framesLocalService.deleteFrame(frameId, true);
		}
		catch (Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	// Estable un contenido de sustitución
	public void setReplacementContentType(String nameOriginal, String nameReplacement) throws SystemException
	{
		try
		{	
			framesLocalService.setReplacementContentType(nameOriginal, nameReplacement, true);
		}
		catch (Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void setGroupWaterMark(long groupId, byte[] bytes) throws SystemException
	{
		try
		{
			framesLocalService.setGroupWaterMark(groupId, bytes, true);
		}
		catch (Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	// Antigua entrada para MLN para publicar watermarks, frames y contenidos de susticución. Levanta excepción, NO debe utilizarse. Usar LiveLocalServiceImpl.publishToLive
	@Deprecated
	public void publishToLive(long companyId, long groupId, long userId, String processId, String xml) throws SystemException
	{
		try
		{
			throw new Exception(IterErrorKeys.ITER_UPDATE_MILENIUM_ITER);			
		}
		catch (Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String importContents(String scopeGroupName, String xml) throws SystemException
	{
		String result = StringPool.BLANK;
		try
		{
			result = framesLocalService.importContents(scopeGroupName, xml).asXML();
		}
		catch (Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}

	public String getFramesByGroup(long groupId) throws SystemException
	{
		try
		{
			return framesLocalService.getFramesByGroup(groupId);
		}
		catch (Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
}