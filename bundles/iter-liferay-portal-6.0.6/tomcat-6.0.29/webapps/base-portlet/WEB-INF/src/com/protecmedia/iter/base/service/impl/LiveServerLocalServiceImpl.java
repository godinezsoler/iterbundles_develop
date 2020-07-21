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
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.base.LiveServerLocalServiceBaseImpl;

/**
 * The implementation of the live server local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.LiveServerLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.LiveServerLocalServiceUtil} to access the live server local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.LiveServerLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.LiveServerLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class LiveServerLocalServiceImpl extends LiveServerLocalServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(LiveServerLocalServiceImpl.class);
	static final private String SELECT 	= "SELECT serverid, host, port, enabled FROM LiveServer";
	static final private String INSERT	= new StringBuilder(
		"INSERT INTO LiveServer (serverId, host, port, enabled) 	\n").append(
		"	VALUES ('%1$s', '%2$s', %3$d, %4$s) 					\n").toString();
	
	static final private String UPDATE  = new StringBuilder(
		"UPDATE LiveServer SET host='%1$s', port=%2$d, enabled=%3$s \n").append(
		"	WHERE serverId='%4$s'	 								\n").toString();
	
	static final private String DELETE	= "DELETE FROM LiveServer WHERE serverId = '%s'";		
	
	
	public Document getServers() throws NoSuchMethodException, SecurityException
	{
		Document dom = PortalLocalServiceUtil.executeQueryAsDom(SELECT);
		
		if (_log.isDebugEnabled())
			_log.debug(dom.asXML());
		
		return dom;
	}
	
	public void insert(String data) throws DocumentException, ServiceError, IOException, SQLException
	{
		_log.debug(data);
		
		Document dom 	= SAXReaderUtil.read(data);
		String host  	= XMLHelper.getTextValueOf(dom, "/rs/row/@host");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(host), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		long port  		= XMLHelper.getLongValueOf(dom, "/rs/row/@port");
		ErrorRaiser.throwIfFalse(port > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		boolean enabled = GetterUtil.getBoolean(XMLHelper.getTextValueOf(dom, "/rs/row/@enabled"), true);
		
		PortalLocalServiceUtil.executeUpdateQuery( String.format(INSERT, PortalUUIDUtil.newUUID(), host, port, enabled) );
	}
	
	public void update(String data) throws DocumentException, ServiceError, IOException, SQLException
	{
		_log.debug(data);
		
		Document dom 	= SAXReaderUtil.read(data);
		
		String serverId = XMLHelper.getTextValueOf(dom, "/rs/row/@serverid");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(serverId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		String host  	= XMLHelper.getTextValueOf(dom, "/rs/row/@host");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(host), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		long port  		= XMLHelper.getLongValueOf(dom, "/rs/row/@port");
		ErrorRaiser.throwIfFalse(port > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		boolean enabled = GetterUtil.getBoolean(XMLHelper.getTextValueOf(dom, "/rs/row/@enabled"), true);
		
		PortalLocalServiceUtil.executeUpdateQuery( String.format(UPDATE, host, port, enabled, serverId) );
	}
	
	public void delete(String serverId) throws DocumentException, ServiceError, IOException, SQLException
	{
		_log.debug(serverId);
		
		PortalLocalServiceUtil.executeUpdateQuery( String.format(DELETE, serverId) );
	}
}