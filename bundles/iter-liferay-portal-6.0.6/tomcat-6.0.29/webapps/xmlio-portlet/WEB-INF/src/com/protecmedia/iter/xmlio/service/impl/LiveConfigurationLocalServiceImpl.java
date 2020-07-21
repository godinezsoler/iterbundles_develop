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
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.liferay.counter.service.CounterLocalService;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.cache.MultiVMPoolUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.model.Company;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.base.LiveConfigurationLocalServiceBaseImpl;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

/**
 * The implementation of the live configuration local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.service.LiveConfigurationLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil} to access the live configuration local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.xmlio.service.base.LiveConfigurationLocalServiceBaseImpl
 * @see com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class LiveConfigurationLocalServiceImpl
	extends LiveConfigurationLocalServiceBaseImpl {
	
	private static Log _log = LogFactoryUtil.getLog(LiveConfigurationLocalServiceImpl.class);
	
    public static final String[] COLUMNS_SET_LIVE_CONF = {"gatewayHost"
			    										,"localPath"
			    										,"remoteIterServer"
			    										,"operationTimeOut"
			    										,"connectionTimeOut"
			    										,"outputMethod"
			    										,"ftpPath"
			    										,"ftpUser"
			    										,"ftpPassword"
			    										,"remotePath"
			    										,"remoteUserId"
			    										,"remoteUserName"
			    										,"remoteUserPassword"
			    										,"remoteCompanyId"
			    										,"remoteGlobalGroupId"
														};

	private static final String GET_CONFIG     = "SELECT * FROM xmlio_liveconfiguration WHERE companyId=%s"; 	
	
	private static final String INSERT_XMLIO_LIVECONFIGURATION = new StringBuilder(
			"INSERT INTO Xmlio_LiveConfiguration \n"																						).append(
			"(id_, gatewayHost, localPath, remoteIterServer, operationTimeOut, connectionTimeOut,outputMethod, ftpPath,ftpUser, \n" 		).append(
			"ftpPassword, remotePath, remoteUserId, remoteUserName, remoteUserPassword, remoteCompanyId, remoteGlobalGroupId, \n"			).append(
			"destinationType, remoteChannelId, archive, companyId) \n"																		).append(
		"VALUES \n"																															).append(
				"(%s,'%s','%s','%s', %s, %s, '%s', '%s','%s','%s','%s', %s, '%s', '%s', %s, %s, '%s', %s, %s, %s )  \n"						).append(
		"ON DUPLICATE KEY UPDATE \n"																										).append(
				"gatewayHost=VALUES(gatewayHost), localPath=VALUES(localPath), remoteIterServer=VALUES(remoteIterServer), \n"				).append(
				"operationTimeOut=VALUES(operationTimeOut),connectionTimeOut=VALUES(connectionTimeOut),outputMethod=VALUES(outputMethod),\n").append(
				"ftpPath=VALUES(ftpPath), ftpUser=VALUES(ftpUser), ftpPassword=VALUES(ftpPassword), remotePath=VALUES(remotePath), \n" 		).append(
				"remoteUserId=VALUES(remoteUserId), remoteUserName=VALUES(remoteUserName), remoteUserPassword=VALUES(remoteUserPassword),\n").append(
				"remoteCompanyId=VALUES(remoteCompanyId), remoteGlobalGroupId=VALUES(remoteGlobalGroupId)" ).toString();
				
	
	public String getLiveConfigurationFlex(String companyId) throws ServiceError, SecurityException, NoSuchMethodException {
		return getConfigDom(companyId).asXML();
	}
	
	public Document getConfigDom(String companyId) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(companyId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_CONFIG, companyId));
	}
	
	public String setLiveConfigurationFlex(String xmlData) throws Exception
	{
		ErrorRaiser.throwIfNull(xmlData, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document xmlDataDoc = SAXReaderUtil.read(xmlData);
		
		String companyId = 					getConfigValue(xmlDataDoc, "@companyId");
		String gatewayHost =				getConfigValue(xmlDataDoc, "@gatewayHost");
		String localPath =					getConfigValue(xmlDataDoc, "@localPath");
		String remoteIterServer = 			getConfigValue(xmlDataDoc, "@remoteIterServer");
		String operationTimeOut = 			getConfigValue(xmlDataDoc, "@operationTimeOut");
		String connectionTimeOut = 			getConfigValue(xmlDataDoc, "@connectionTimeOut");
		String outputMethod = 				getConfigValue(xmlDataDoc, "@outputMethod");
		String ftpPath = 					getConfigValue(xmlDataDoc, "@ftpPath");
		String ftpUser = 					getConfigValue(xmlDataDoc, "@ftpUser");
		String ftpPassword = 				getConfigValue(xmlDataDoc, "@ftpPassword");
		String remotePath = 				getConfigValue(xmlDataDoc, "@remotePath");
		String remoteUserId = 				getConfigValue(xmlDataDoc, "@remoteUserId");
		String remoteUserName = 			getConfigValue(xmlDataDoc, "@remoteUserName");
		String remoteUserPassword = 		getConfigValue(xmlDataDoc, "@remoteUserPassword");
		String remoteCompanyId = 			getConfigValue(xmlDataDoc, "@remoteCompanyId");
		String remoteGlobalGroupId = 		getConfigValue(xmlDataDoc, "@remoteGlobalGroupId");
		
		//valores por defecto 
		String destinationType   = "explicit";
		long   remoteChannelId	 =	-1;
		long   archive	 		 =	0;
		
		localPath 		 = correctPath( localPath  );
		remotePath 		 = correctPath( remotePath );
		
		CounterLocalService counter = super.getCounterLocalService();
		long id_ = counter.increment();
		
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(INSERT_XMLIO_LIVECONFIGURATION, 
																id_,
																gatewayHost,
																localPath,
																remoteIterServer,
																operationTimeOut,
																connectionTimeOut,
																outputMethod,
																ftpPath,
																ftpUser,
																ftpPassword,
																remotePath,
																remoteUserId,
																remoteUserName,
																remoteUserPassword,
																remoteCompanyId,
																remoteGlobalGroupId,
																destinationType,
																remoteChannelId,
																archive,
																companyId));
		
		MultiVMPoolUtil.clear();
		
		return getLiveConfigurationFlex(companyId);
	}
	
	public String getConfigValue(Document doc, String field) throws ServiceError
	{
		String data = XMLHelper.getTextValueOf(doc, "/rs/row/" + field, "");
		
		return StringEscapeUtils.escapeSql(data);
	}
	
	/* Sustituye el carácter '\'  por  '\\'.
	 * Es necesario porque '\' se interpreta como una secuencia de escape y se omite al guardar el dato en base de datos.*/
	public String correctPath( String path )
	{
		String cPath = StringUtils.replace(path, StringPool.BACK_SLASH, StringPool.BACK_SLASH.concat(StringPool.BACK_SLASH) );
		
		return cPath;
	}
	
	public LiveConfiguration getLiveConfigurationByCompanyId(long companyId){
		try{
			return liveConfigurationPersistence.findByCompanyId(companyId);
		}
		catch(Exception e){
			return null;
		}
	}
	
	public String getRemoteSystemInfo() throws Throwable{

		String remoteSystemInfo		= "";
		
		// Recupera la configuracion
		List<Company> companies = CompanyLocalServiceUtil.getCompanies();
		long companyId = companies.get(0).getCompanyId();
		LiveConfiguration liveConf 	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(companyId);
		String remoteIterServer 	= liveConf.getRemoteIterServer2();
		long remoteUserId 		= liveConf.getRemoteUserId();
		String remoteUserName 	= liveConf.getRemoteUserName();
		String remoteUserPwd	= liveConf.getRemoteUserPassword();
					
		String remoteSystemInfoJS = XMLIOUtil.executeJSONRemoteGetSystemInfo(companyId, remoteIterServer, remoteUserId, remoteUserName, remoteUserPwd);
			
		//ErrorRaiser.throwIfFalse(remoteSystemInfoJS != null && !remoteSystemInfoJS.equals(""), IterErrorKeys.XYZ_E_PUBRESPONSE_EMPTY_ZYX);
			
		//Añadimos el password de usuario
		Document doc 	 = SAXReaderUtil.read(remoteSystemInfoJS);		
		XPath xpath 	 = SAXReaderUtil.createXPath("//settings/company-settings");
		List<Node> node = xpath.selectNodes(doc);	
		    
		if(node.size() > 0){
		    Element ele = (Element)node.get(0);	
			  
		    Element newElem = ele.addElement("param");
		    newElem.addAttribute("id", "userPassword");
		    newElem.addAttribute("value", remoteUserPwd);
		}
		    
		doc.setXMLEncoding("ISO-8859-1");		    			
		remoteSystemInfo = doc.asXML(); 
		
				
		return remoteSystemInfo;
	}
	
	public boolean getArchiveByCompanyId(long companyId) {
		LiveConfiguration liveConfiguration = getLiveConfigurationByCompanyId(companyId);
		return liveConfiguration.getArchive();
	}
	
	/*
	 * Set Functions
	 */
	
	public void setArchiveByCompanyId(long companyId, boolean value) throws SystemException {
		LiveConfiguration liveConfiguration = getLiveConfigurationByCompanyId(companyId);		
		liveConfiguration.setArchive(value);
		liveConfigurationPersistence.update(liveConfiguration, false);
	}
	
}