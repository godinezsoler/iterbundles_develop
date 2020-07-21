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
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.protecmedia.iter.base.model.Communities;
import com.protecmedia.iter.base.model.impl.CommunitiesImpl;
import com.protecmedia.iter.base.service.base.CommunitiesLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.PortletMgr;

/**
 * The implementation of the communities local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.CommunitiesLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.CommunitiesLocalServiceUtil} to access the communities local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.CommunitiesLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.CommunitiesLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class CommunitiesLocalServiceImpl extends CommunitiesLocalServiceBaseImpl
{
	
	public Communities getCommunitiesByGroup(long groupId) throws SystemException{
		return communitiesPersistence.fetchByGroupId(groupId);
	}
	
	public Communities getEmptyCommunity(){
		return new CommunitiesImpl();
	}
	
	public void clearCache()
	{
		communitiesPersistence.clearCache();
	}
	
	public Document exportDataSearch(Long groupId) throws ServiceError, SecurityException, NoSuchMethodException, PortalException, SystemException
	{
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Recupera los datos a exportar.
		Document dom = PortletMgr.getSearchCommunityConfig(String.valueOf(groupId));
		
		// Elimina datos innecesarios.
		if (dom.getRootElement().content().size() > 0)
		{
			dom.getRootElement().element("row").remove(dom.getRootElement().element("row").element("id_"));
			dom.getRootElement().element("row").remove(dom.getRootElement().element("row").element("groupId"));
		}

		return dom;
	}
	
	public void importDataSearch(String data) throws DocumentException, ServiceError, PortalException, SystemException, NumberFormatException, SecurityException, IOException, SQLException, NoSuchMethodException
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
        
        // Recupera los datos a importar y valida que vengan informados.
		String privateSearchUrl = GetterUtil.getString(XMLHelper.getStringValueOf(dom, "/rs/row/privateSearchUrl"), StringPool.BLANK);
		String publicSearchUrl = GetterUtil.getString(XMLHelper.getStringValueOf(dom, "/rs/row/publicSearchUrl"), StringPool.BLANK);
		String fuzzySearch = GetterUtil.getString2(XMLHelper.getStringValueOf(dom, "/rs/row/fuzzySearch"), "0");
		
		// Inserta / Actualiza el registro.
		PortletMgr.setSearchComunityConfig(String.valueOf(groupId), privateSearchUrl, publicSearchUrl, fuzzySearch);
	}
	
	public void publishSearch(long groupId) throws PortalException, SystemException, SecurityException, ServiceError, NoSuchMethodException, ClientProtocolException, IOException
	{
		Document dom = exportDataSearch(groupId);
		// Recupera el nombre del grupo e inserta el groupName para la publicación.
		Group group = GroupLocalServiceUtil.getGroup(groupId);
		String groupName = group.getName();
		dom.getRootElement().addAttribute("groupName", groupName);
		// Publica en el Live.
		publishToLive("com.protecmedia.iter.base.service.CommunitiesServiceUtil", "importDataSearch", dom.asXML());
	}
	
	public Document exportDataLastUpdate(Long groupId) throws ServiceError, SecurityException, NoSuchMethodException, PortalException, SystemException
	{
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Recupera los datos a exportar.
		Document dom = PortletMgr.getLastUpdateConfig(String.valueOf(groupId));
		
		// Elimina datos innecesarios.
		if (dom.getRootElement().content().size() > 0)
		{
			dom.getRootElement().element("row").remove(dom.getRootElement().element("row").element("id_"));
			dom.getRootElement().element("row").remove(dom.getRootElement().element("row").element("groupId"));
		}

		return dom;
	}
	
	public void importDataLastUpdate(String data) throws DocumentException, ServiceError, PortalException, SystemException, NumberFormatException, SecurityException, IOException, SQLException, NoSuchMethodException
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
        
        // Recupera los datos a importar y valida que vengan informados
		String lastUpdated = XMLHelper.getStringValueOf(dom, "/rs/row/lastUpdated");
		ErrorRaiser.throwIfNull(lastUpdated, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Inserta / Actualiza el registro
		PortletMgr.setLastUpdateConfig(String.valueOf(groupId), lastUpdated);
	}
	
	public void publishLastUpdate(long groupId) throws PortalException, SystemException, SecurityException, ServiceError, NoSuchMethodException, ClientProtocolException, IOException
	{
		Document dom = exportDataLastUpdate(groupId);
		Group group = GroupLocalServiceUtil.getGroup(groupId);
		String groupName = group.getName();
		dom.getRootElement().addAttribute("groupName", groupName);
		publishToLive("com.protecmedia.iter.base.service.CommunitiesServiceUtil", "importDataLastUpdate", dom.asXML());
	}
	
	private void publishToLive(String className, String methodName, String data) throws JSONException, ClientProtocolException, SystemException, SecurityException, IOException, NoSuchMethodException, ServiceError 
    {
          List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
          remoteMethodParams.add(new BasicNameValuePair("serviceClassName", className));
          remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", methodName));
          remoteMethodParams.add(new BasicNameValuePair("serviceParameters", "[data]"));
          remoteMethodParams.add(new BasicNameValuePair("data", data));
          
          JSONUtil.executeMethod("/c/portal/json_service", remoteMethodParams);
    }
	
}