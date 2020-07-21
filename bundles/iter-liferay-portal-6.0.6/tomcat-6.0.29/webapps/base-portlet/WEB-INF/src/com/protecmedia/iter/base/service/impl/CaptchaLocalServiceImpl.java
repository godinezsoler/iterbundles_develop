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
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.base.CaptchaLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.ServiceError;

/**
 * The implementation of the captcha local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.CaptchaLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.CaptchaLocalServiceUtil} to access the captcha local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.CaptchaLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.CaptchaLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class CaptchaLocalServiceImpl extends CaptchaLocalServiceBaseImpl
{
	
	private final String GET_CAPTCHA = "SELECT publickey, secretkey, languagecode, theme FROM captcha WHERE groupid=%s";
	
	private final String SET_CAPTCHA = new StringBuilder(" INSERT INTO captcha(groupid, publickey, secretkey, languagecode, theme, modifieddate) ")
												.append(" VALUES( %s, '%s', '%s', '%s', '%s', NOW() ) ")
												.append(" ON DUPLICATE KEY UPDATE publickey=VALUES(publickey), secretkey=VALUES(secretkey), languagecode=VALUES(languagecode), theme=VALUES(theme), modifieddate=VALUES(modifieddate) ").toString();
	
	private final String UPD_PUBLICATIONDATE = "UPDATE captcha SET publicationdate = NOW() WHERE groupid = %s";
	
	public String getCaptcha( long groupId ) throws ServiceError, SecurityException, NoSuchMethodException
	{
		String result = "";
		
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		result = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_CAPTCHA, groupId) ).asXML();
		
		return result;
	}
	
	public String setCaptcha( String xmlData ) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, IOException, SQLException
	{
		long groupId = insertCaptcha(xmlData);
		return getCaptcha(groupId);
	}
	
	private long insertCaptcha(String xmlData) throws DocumentException, ServiceError, IOException, SQLException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		Element row = dataRoot.element("row");
		
		long groupId = 0;
		if (row != null)
		{
			String grpid = row.attributeValue("groupid");
			ErrorRaiser.throwIfNull(grpid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			groupId = Long.parseLong(grpid);
			
			String publickey = row.attributeValue("publickey");
			ErrorRaiser.throwIfNull(publickey, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			String privatekey =row.attributeValue("secretkey");
			ErrorRaiser.throwIfNull(privatekey, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			String lang = row.attributeValue("languagecode");
			ErrorRaiser.throwIfNull(lang, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			String theme = row.attributeValue("theme");
			ErrorRaiser.throwIfNull(theme, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			String query = String.format(SET_CAPTCHA, grpid, publickey, privatekey, lang, theme);
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
		return groupId;
	}
	
	public Document exportData(Long groupId) throws ServiceError, SecurityException, DocumentException, NoSuchMethodException, PortalException, SystemException
	{
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document dom = SAXReaderUtil.read(getCaptcha(groupId));
		return dom;
	}
	
	public void importData(String data) throws DocumentException, ServiceError, PortalException, SystemException, IOException, SQLException
	{
        Document dom = SAXReaderUtil.read(data);
        // Busca primero el groupIid en el xml, y si no existe, se busca por groupName
        long groupId = XMLHelper.getLongValueOf(dom, "/rs/@groupId");
        if (groupId <= 0)
        {
        	String groupName = XMLHelper.getStringValueOf(dom, "/rs/@groupName");
        	ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        	groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
        }
        ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
        Element row = dom.getRootElement().element("row");
        
        if (row != null)
        {
	        // Establece el groupId recuperado, ya que durante la publicación en el LIVE puede ser distinto.
        	row.addAttribute("groupid", String.valueOf(groupId));
	        
			insertCaptcha(dom.asXML());
        }
	}
	
	public void publish(long groupId) throws SecurityException, PortalException, SystemException, DocumentException, NoSuchMethodException, ClientProtocolException, IOException, SQLException, com.liferay.portal.kernel.error.ServiceError
	{
		// Genera el XML a publicar. 
		Document dom = exportData(groupId);
		
		// Recupera el nombre del grupo para la publicación.
		Group group = GroupLocalServiceUtil.getGroup(groupId);
		String groupName = group.getName();
		dom.getRootElement().addAttribute("groupName", groupName);
		dom.setXMLEncoding("ISO-8859-1");
		
		
		// Manda la publicación al LIVE.
		publishToLive("com.protecmedia.iter.base.service.CaptchaServiceUtil", "importData", dom.asXML());
		// Actualiza la fecha de publicación.
		PortalLocalServiceUtil.executeUpdateQuery(String.format(UPD_PUBLICATIONDATE, groupId));
	}
	
	private void publishToLive(String className, String methodName, String data) throws JSONException, ClientProtocolException, SystemException, SecurityException, IOException, NoSuchMethodException, com.liferay.portal.kernel.error.ServiceError 
    {
          List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
          remoteMethodParams.add(new BasicNameValuePair("serviceClassName", className));
          remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", methodName));
          remoteMethodParams.add(new BasicNameValuePair("serviceParameters", "[data]"));
          remoteMethodParams.add(new BasicNameValuePair("data", data));
          
          JSONUtil.executeMethod("/c/portal/json_service", remoteMethodParams);
    }
}