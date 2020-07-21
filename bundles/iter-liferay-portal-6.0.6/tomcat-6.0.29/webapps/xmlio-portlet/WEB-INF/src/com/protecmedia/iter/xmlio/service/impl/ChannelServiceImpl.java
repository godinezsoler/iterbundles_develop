/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.xml.Document;
import com.protecmedia.iter.xmlio.service.base.ChannelServiceBaseImpl;
import com.protecmedia.iter.xmlio.service.util.CacheRefresh;

/**
 * The implementation of the channel remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.service.ChannelService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.xmlio.service.ChannelServiceUtil} to access the channel remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author Protecmedia
 * @see com.protecmedia.iter.xmlio.service.base.ChannelServiceBaseImpl
 * @see com.protecmedia.iter.xmlio.service.ChannelServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class ChannelServiceImpl extends ChannelServiceBaseImpl {
	
	private static Log _log = LogFactoryUtil.getLog(ChannelServiceImpl.class);
	
	public String importToLive(long companyId, long userId, long groupId, String fileName)
	{
		if (_log.isTraceEnabled())
			_log.trace( String.format("\nBefore importToLive: %d %s", groupId, fileName) );
			
		String result = "";
		try
		{
			result = channelLocalService.importToLive(companyId, userId, groupId, fileName);
		}
		catch(Exception e)
		{
			_log.error(e);
			result = ServiceErrorUtil.getServiceErrorAsXml(e);
		}
		
		_log.trace(result);
		
		// Encoding de URL
		try
		{
			result = URLEncoder.encode( result, "UTF-8"); 
		}
		catch (UnsupportedEncodingException e)
		{
			// Si falla la codificación pero la operación hasta este punto no, se guarda la traza y se envia tal cual.
			// Será el BACK quien maneje esta cadena como pueda
			_log.error("Error while trying to import to live: "+e.toString());
		}
		
		if (_log.isTraceEnabled())
			_log.trace( String.format("\nAfter importToLive: %d %s \n%s", groupId, fileName, result) );

		return result;
	}
	
	public String importWebThemesToLive(String scopeGroupName, String fileName) throws SystemException
	{
		if (_log.isTraceEnabled())
			_log.trace( String.format("\nBefore importWebThemesToLive: %s %s", scopeGroupName, fileName) );

		String result = StringPool.BLANK;
		try
		{
			Document dom = channelLocalService.importWebThemesToLive(scopeGroupName, fileName);
			result = dom.asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		finally
		{
			if (_log.isTraceEnabled())
				_log.trace( String.format("\nAfter importWebThemesToLive: %s %s \n%s", scopeGroupName, fileName, result) );
		}
		
		return result;
	}
	
	public void importJournalTemplatesToLive(String fileName) throws SystemException
	{
		if (_log.isTraceEnabled())
			_log.trace( String.format("\nBefore importJournalTemplatesToLive: %s", fileName) );

		try
		{
			channelLocalService.importJournalTemplatesToLive(fileName);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		finally
		{
			if (_log.isTraceEnabled())
				_log.trace( String.format("\nAfter importJournalTemplatesToLive: %s", fileName) );
		}
	}

	public void importLayoutTemplatesToLive(String fileName) throws SystemException
	{
		if (_log.isTraceEnabled())
			_log.trace( String.format("\nBefore importLayoutTemplatesToLive: %s", fileName) );

		try
		{
			channelLocalService.importLayoutTemplatesToLive(fileName);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		finally
		{
			if (_log.isTraceEnabled())
				_log.trace( String.format("\nAfter importLayoutTemplatesToLive: %s", fileName) );
		}
	}

	public void refreshCache(String cacheRefresh) throws Exception
	{
		channelLocalService.refreshCache(cacheRefresh);
	}
	
	public void importDefaultSectionProperties(String xml) throws SystemException
	{
		if (_log.isTraceEnabled())
			_log.trace( String.format("\nBefore importDefaultSectionProperties: \n%s", xml) );

		try
		{
			channelLocalService.importDefaultSectionProperties(xml);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		finally
		{
			if (_log.isTraceEnabled())
				_log.trace( String.format("\nAfter importDefaultSectionProperties: \n%s", xml) );
		}
	}
}
