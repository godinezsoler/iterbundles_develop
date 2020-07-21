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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBFactoryUtil;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.base.ImageResolutionLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

/**
 * The implementation of the image resolution local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.ImageResolutionLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.ImageResolutionLocalServiceUtil} to access the image resolution local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.ImageResolutionLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.ImageResolutionLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class ImageResolutionLocalServiceImpl extends ImageResolutionLocalServiceBaseImpl 
{
	
	public static final String DELETE_RESOLUTIONS = "DELETE FROM ImageResolution";
	public static final String INSERT_RESOLUTION = "INSERT INTO ImageResolution(resolutionName, width, height) VALUES ('%s', %d, %d)";
	
	
	public static void importResolutions(String xmlData) throws Exception{
		
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_LIVE)){
			
			DB db = DBFactoryUtil.getDB();
			
			String query = String.format(DELETE_RESOLUTIONS);
			db.runSQL(query);
			
			Document xmlDoc = SAXReaderUtil.read(xmlData);		
			
			String xPathQuery = "//rs/row";
			
			XPath xpathSelector = SAXReaderUtil.createXPath(xPathQuery);
			List<Node> nodes = xpathSelector.selectNodes(xmlDoc);
			
			String resolutionName = "";
			int width;
			int height;
			
			for (int i = 0; i < nodes.size(); i++){
				
				width = -1;
				height = -1;
				
				Node node = nodes.get(i);
				Element elem = (Element)node;
				if (elem.attribute("resolutionName")!= null){
					resolutionName = elem.attribute("resolutionName").getValue();
				}
				if ((elem.attribute("width")!= null) && (!elem.attribute("width").getValue().equals(""))){
					width = Integer.valueOf(elem.attribute("width").getValue());
				}
				if ((elem.attribute("height")!= null) && (!elem.attribute("height").getValue().equals(""))){
					height = Integer.valueOf(elem.attribute("height").getValue());
				}
				
				query = getQuery(resolutionName,width,height);
				db.runSQL(query);
			}
			
		}
		
	}
	
	public void publishToLive(String xmlData) throws UnsupportedEncodingException, ClientProtocolException, IOException, SystemException, PortalException, ServiceError{
		
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW)) {
		
			long companyId = GroupLocalServiceUtil.getGroup(GroupMgr.getGlobalGroupId()).getCompanyId();

			LiveConfiguration liveConf = LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(companyId);
			String remoteIP = liveConf.getRemoteIterServer2().split(":")[0];
			int remotePort = Integer.valueOf(liveConf.getRemoteIterServer2().split(":")[1]);
			String remoteMethodPath = "/base-portlet/secure/json";

			List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
			remoteMethodParams.add(new BasicNameValuePair("serviceClassName",	"com.protecmedia.iter.base.service.ImageResolutionServiceUtil"));
			remoteMethodParams.add(new BasicNameValuePair("serviceMethodName",	"importResolutions"));
			remoteMethodParams.add(new BasicNameValuePair("serviceParameters",	"[xmlData]"));
			remoteMethodParams.add(new BasicNameValuePair("xmlData",			xmlData));

			String result = XMLIOUtil.executeJSONRemoteMethod2(companyId, remoteIP, remotePort, liveConf.getRemoteUserName(),
															   liveConf.getRemoteUserPassword(), remoteMethodPath,remoteMethodParams);
			JSONObject json = JSONFactoryUtil.createJSONObject(result);

			String errorMsg = json.getString("exception");
			if (!errorMsg.isEmpty()) 
			{
				String iterErrorMsg = ServiceErrorUtil.containIterException(errorMsg);
				throw new SystemException(iterErrorMsg.isEmpty() ? errorMsg : iterErrorMsg);
			}

		}
	}
	
	protected static String getQuery(String name, int width, int height) throws SystemException{
		
		String query = "";
		
		if (height == -1)
			query = String.format(INSERT_RESOLUTION, name, width, null);
		else if (width == -1)
			query = String.format(INSERT_RESOLUTION, name, null, height);
		else
			query = String.format(INSERT_RESOLUTION, name, width, height);
		
		return query;
	}
	
	
}