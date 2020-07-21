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

package com.protecmedia.iter.xmlio.service.impl;

import java.io.IOException;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.protecmedia.iter.xmlio.service.base.ContextVarsPublishServiceBaseImpl;

/**
 * The implementation of the context vars publish remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.service.ContextVarsPublishService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.xmlio.service.ContextVarsPublishServiceUtil} to access the context vars publish remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.xmlio.service.base.ContextVarsPublishServiceBaseImpl
 * @see com.protecmedia.iter.xmlio.service.ContextVarsPublishServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class ContextVarsPublishServiceImpl extends ContextVarsPublishServiceBaseImpl
{	
	private static Log _log = LogFactoryUtil.getLog(ContextVarsPublishServiceImpl.class);
	
	// Punto de entrada para la publicación desde ITER
	public void publishToLive(long scopeGroupId) throws SystemException
	{
		try
		{
			contextVarsPublishLocalService.publishToLive(scopeGroupId);
		}
		catch (Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	@Deprecated
	public void publishCtxVarsToLive(long scopeGroupId, String[] ctxVarIds) throws Exception
	{
		throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(new Exception(IterErrorKeys.ITER_UPDATE_MILENIUM_ITER)));
	}
	
	public String importContents(String scopeGroupName, String fileName) throws SystemException
	{
		String result = StringPool.BLANK;
		
		try
		{
			result = contextVarsPublishLocalService.importContents(scopeGroupName, fileName).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}

}