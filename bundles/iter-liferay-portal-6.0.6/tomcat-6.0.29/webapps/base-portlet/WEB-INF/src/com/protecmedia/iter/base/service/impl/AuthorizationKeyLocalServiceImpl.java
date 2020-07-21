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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.base.AuthorizationKeyLocalServiceBaseImpl;

/**
 * The implementation of the authorization key local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.AuthorizationKeyLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.AuthorizationKeyLocalServiceUtil} to access the authorization key local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.AuthorizationKeyLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.AuthorizationKeyLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class AuthorizationKeyLocalServiceImpl extends AuthorizationKeyLocalServiceBaseImpl
{
	private static final String MD5  = "MD5";
	private static final String UTF8 = "UTF-8";
	
	private static final String SQL_SELECT_KEYS_BY_GROUP      = "SELECT publicid, secretkey, name, enabled FROM authorizationkey WHERE groupId=%s";
	private static final String SQL_INSERT_AUTHORIZATION_KEYS = "INSERT INTO authorizationkey (publicid, secretkey, name, groupId) VALUES ('%s', '%s', '%s', %d)";
	private static final String SQL_UPDATE_AUTHORIZATION_NAME = "UPDATE authorizationkey SET name='%s' WHERE publicid='%s'";
	private static final String SQL_DELETE_AUTHORIZATION_KEYS = "DELETE FROM authorizationkey WHERE publicid IN (%s)";
	private static final String SQL_ENABLE_AUTHORIZATION_KEYS = "UPDATE authorizationkey SET enabled='%s' WHERE publicid='%s'";
	private static final String SQL_SELECT_PRIVKEY_BY_PUBKEY  = "SELECT secretkey FROM authorizationkey WHERE publicid='%s' AND enabled='true'";
	
	private static final String XPATH_ELEMENT_ROW    = "rs/row";
	private static final String XPATH_ATTR_GROUPID   = "@groupid";
	private static final String XPATH_ATTR_NAME      = "@name";
	private static final String XPATH_ATTR_PUBLICID  = "@publicid";
	private static final String XPATH_ATTR_SECRETKEY = "@secretkey";
	private static final String XPATH_ATTR_ENABLED   = "@enable";
	private static final String XPATH_SECRETKEY      = XPATH_ELEMENT_ROW + StringPool.SLASH + XPATH_ATTR_SECRETKEY;
	
	public Document getAuthorizationKeys(String groupId) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_SELECT_KEYS_BY_GROUP, groupId));
	}
	
	public String addAuthorizationKey(String xmlData) throws ServiceError, DocumentException, IOException, SQLException, NoSuchAlgorithmException, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(xmlData, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document data = SAXReaderUtil.read(xmlData);
		
		long groupId = XMLHelper.getLongValueOf(data.getRootElement(), XPATH_ATTR_GROUPID);
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Node row = data.selectSingleNode(XPATH_ELEMENT_ROW);
		
		String name = XMLHelper.getStringValueOf(row, XPATH_ATTR_NAME);
		ErrorRaiser.throwIfNull(name, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String publicId = XMLHelper.getStringValueOf(row, XPATH_ATTR_PUBLICID);
		ErrorRaiser.throwIfNull(publicId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String secretKey = XMLHelper.getStringValueOf(row, XPATH_ATTR_SECRETKEY);
		ErrorRaiser.throwIfNull(secretKey, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		secretKey = Base64.encode(MessageDigest.getInstance(MD5).digest(secretKey.getBytes(UTF8)));
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(SQL_INSERT_AUTHORIZATION_KEYS, publicId, secretKey, name, groupId));
		return getPrivateKeyFromPublicKey(publicId);
	}
	
	public void updateAuthorizationKey(String xmlData) throws ServiceError, DocumentException, IOException, SQLException
	{
		ErrorRaiser.throwIfNull(xmlData, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document data = SAXReaderUtil.read(xmlData);
		
		Node row = data.selectSingleNode(XPATH_ELEMENT_ROW);
		
		String name = XMLHelper.getStringValueOf(row, XPATH_ATTR_NAME);
		ErrorRaiser.throwIfNull(name, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String publicId = XMLHelper.getStringValueOf(row, XPATH_ATTR_PUBLICID);
		ErrorRaiser.throwIfNull(publicId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(SQL_UPDATE_AUTHORIZATION_NAME, name, publicId));
	}
	
	public String deleteAuthorizationKey(String xmlData) throws ServiceError, DocumentException, IOException, SQLException
	{
		ErrorRaiser.throwIfNull(xmlData, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document data = SAXReaderUtil.read(xmlData);
		
		List<Node> authorizations = data.selectNodes(XPATH_ELEMENT_ROW);
		String[] publicids = XMLHelper.getStringValues(authorizations, XPATH_ATTR_PUBLICID);
		
		if (publicids.length > 0)
		{
			PortalLocalServiceUtil.executeUpdateQuery(String.format(SQL_DELETE_AUTHORIZATION_KEYS, StringUtil.merge(publicids, StringPool.COMMA, StringPool.APOSTROPHE)));
		}
		
		return xmlData;
	}
	
	public void enableAuthorizationKey(String xmlData) throws ServiceError, DocumentException, IOException, SQLException
	{
		ErrorRaiser.throwIfNull(xmlData, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document data = SAXReaderUtil.read(xmlData);
		
		String publicId = XMLHelper.getStringValueOf(data.getRootElement(), XPATH_ATTR_PUBLICID);
		String enabled = XMLHelper.getStringValueOf(data.getRootElement(), XPATH_ATTR_ENABLED);
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(SQL_ENABLE_AUTHORIZATION_KEYS, enabled, publicId));
	}
	
	public String getPrivateKeyFromPublicKey(String publicKey) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(publicKey, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document result = PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_SELECT_PRIVKEY_BY_PUBKEY, publicKey));
		return XMLHelper.getStringValueOf(result, XPATH_SECRETKEY);
	}
}