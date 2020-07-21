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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.base.SMTPServerMgrLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.IterAdmin;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.TeaserMgr;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

/**
 * The implementation of the s m p t server mgr local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.SMPTServerMgrLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.SMPTServerMgrLocalServiceUtil} to access the s m p t server mgr local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.SMPTServerMgrLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.SMPTServerMgrLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class SMTPServerMgrLocalServiceImpl extends SMTPServerMgrLocalServiceBaseImpl
{
	private static ReadWriteLock importGlobalLock = new ReentrantReadWriteLock();
	private static Lock importWriteLock = importGlobalLock.writeLock();

	// Sólo una publicación al mismo tiempo
	private static ReadWriteLock exportGlobalLock = new ReentrantReadWriteLock();
	private static Lock exportWriteLock = exportGlobalLock.writeLock();

	
	private static final String GET_SERVER 						= 	"SELECT * FROM smtpserver WHERE smtpserverid IN ('%s')";
	private static final String GET_SERVER2 					= 	new StringBuffer("\n"				).append(
			"SELECT smtpserverid, smtpserver.description, host, port, enabled, tls, auth, username, \n" ).append(
			" 		password, emailfrom, modifieddate, publicationdate, group_.name groupName \n" 		).append(
			"FROM smtpserver \n" 																		).append(
			"LEFT JOIN group_ ON (group_.groupid = smtpserver.groupid) \n"								).append(
			" 	WHERE smtpserverid IN ('%s')").toString();
	
	private static final String GET_SERVERS 					= 	"SELECT * FROM smtpserver WHERE groupid=%s";
	
	
	private static final String ADD_SERVER						= 	"INSERT INTO smtpserver(smtpserverid, description, host, port, tls, auth, username, password, groupid, enabled, modifieddate, emailfrom, publicationdate) " + 
																	"VALUES ('%s', %s, '%s', %s, %s, %s, %s, %s, %s, %s, '%s', %s, NULL)";
	private static final String UPDATE_SERVER					= 	"UPDATE smtpserver SET description=%s, host='%s', port=%s, tls=%s, auth=%s, username=%s, password=%s, enabled=%s, modifieddate='%s', emailfrom=%s WHERE smtpserverid='%s'";
	private static final String DELETE_SERVERS					= 	"DELETE FROM smtpserver WHERE smtpserverid IN %s";
	
	private static final String CHECK_REFERENCES_SCHEDULE_NEWSLETTER = 	new StringBuffer(
			"SELECT s.host AS smtphost, sch_nltt.name AS referencesname, 'schedule_newsletter' AS reftype FROM smtpserver s \n"	).append(
		 	"LEFT JOIN schedule_smtpserver sch_smpt ON s.smtpserverid=sch_smpt.smtpserverid \n"									).append(
		 	"INNER JOIN schedule_newsletter sch_nltt ON sch_smpt.scheduleid = sch_nltt.scheduleid \n"							).append(
		 	"WHERE \n"																											).append(
		 	"s.enabled=TRUE AND s.smtpserverid=%1$s \n"																			).toString();
	
	private static final String CHECK_REFERENCES_FORMHANDLEREMAIL = 	new StringBuffer(
			"SELECT s.host AS smtphost, f.name AS referencesname, 'form' AS reftype FROM smtpserver s  \n"		).append(
		 	"LEFT JOIN formhandleremail form_hle ON s.smtpserverid=form_hle.smtpserver \n"						).append(
		 	"INNER JOIN form f ON form_hle.formid = f.formid \n"												).append(
		 	"WHERE \n"																							).append(
		 	"form_hle.enabled=TRUE AND s.enabled=TRUE AND s.smtpserverid=%1$s \n"								).toString();
	
	private static final String CHECK_REFERENCES_USER = 	new StringBuffer(
			"SELECT s.host AS smtphost, 'user' AS referencesname, 'user' AS reftype FROM smtpserver s   \n"								).append(
		 	"INNER JOIN Group_Config g_c ON (s.smtpserverid=g_c.registersmptserverid OR s.smtpserverid=g_c.forgetsmptserverid) \n"		).append(
		 	"WHERE \n"																													).append(
		 	"s.enabled=TRUE AND s.smtpserverid=%1$s \n"																					).toString();
	
	
	private static final String CHECK_UNIQUE					= 	"SELECT COUNT(*) FROM smtpserver WHERE host='%s' AND port=%s AND tls=%s AND auth=%s AND username%s AND password%s AND groupid=%s AND smtpserverid != '%s'";
	
	private static final String CHECK_REFERENCES		= 	new StringBuffer(
			 									CHECK_REFERENCES_SCHEDULE_NEWSLETTER	).append(
			 									"\nUNION ALL \n"						).append(		
			 									CHECK_REFERENCES_FORMHANDLEREMAIL		).append(					
			 									"\nUNION ALL \n"						).append(	
			 									CHECK_REFERENCES_USER					).toString();	
																		
	
	public Document exportData(Long groupId) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId) || groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return getServers(groupId);
	}
	
	public Document exportData(List<String> ids) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse(Validator.isNotNull(ids) || ids.size() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return getDiscreteServers(StringUtil.merge(ids, ","));
	}
	
	public Document exportData(String params) throws DocumentException, SecurityException, ServiceError, NoSuchMethodException, PortalException, SystemException
	{
		Document result = null;
		Element root 	= SAXReaderUtil.read(params).getRootElement();
		String groupName= XMLHelper.getStringValueOf(root, "@groupName");
		
		if (Validator.isNotNull(groupName))
		{
			long groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
			result = getServers(groupId);
		}
		else
			result = getDiscreteServers( XMLHelper.getStringValueOf(root, "@ids") );
		
		return result;
	}
	
	public Document getServers(long groupid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(groupid > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SERVERS, groupid));
	}
	
	private Document getDiscreteServers(String serverIds) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse(Validator.isNotNull(serverIds), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document serversDom = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_SERVER2, serverIds.replaceAll(",", "','")) );
		
		return serversDom;
	}
	
	public String addServer(String xmlData) throws ServiceError, DocumentException, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		return addServer( SAXReaderUtil.read(xmlData).selectSingleNode("/rs/row") );
	}
	public String addServer(Node node) throws ServiceError, DocumentException, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		String description = XMLHelper.getTextValueOf(node, "@description");
		if(Validator.isNotNull(description))
		{
			description = StringPool.APOSTROPHE + 
						  StringEscapeUtils.escapeSql(description) + 
						  StringPool.APOSTROPHE;
		}

		String emailfrom = XMLHelper.getTextValueOf(node, "@emailfrom");
		if(Validator.isNotNull(emailfrom))
		{
			emailfrom = StringPool.APOSTROPHE + 
						emailfrom + 
						StringPool.APOSTROPHE;
		}
		
		String host = XMLHelper.getTextValueOf(node, "@host");
		ErrorRaiser.throwIfNull(host, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String port = XMLHelper.getTextValueOf(node, "@port");
		ErrorRaiser.throwIfNull(port, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		try
		{
			Integer.parseInt(port);
		}
		catch (Exception e)
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}

		boolean tls = GetterUtil.getBoolean(XMLHelper.getTextValueOf(node, "@tls"), true);
		boolean auth =  GetterUtil.getBoolean(XMLHelper.getTextValueOf(node, "@auth"), true);
		boolean enabled =  GetterUtil.getBoolean(XMLHelper.getTextValueOf(node, "@enabled"), true);
		
		String username = XMLHelper.getTextValueOf(node, "@username");
		if(Validator.isNotNull(username))
		{
			username = StringPool.APOSTROPHE + 
					   StringEscapeUtils.escapeSql(username) + 
					   StringPool.APOSTROPHE;
		}
		
		String password = XMLHelper.getTextValueOf(node, "@password");
		if(Validator.isNotNull(password))
		{
			password = StringPool.APOSTROPHE + 
					   password + 
					   StringPool.APOSTROPHE;
		}
		
		String groupid = XMLHelper.getTextValueOf(node, "@groupid");
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		String smtpserverid = XMLHelper.getTextValueOf(node, "@smtpserverid");
		if(Validator.isNull(smtpserverid))
			smtpserverid = SQLQueries.getUUID();
		
		checkUnique(host, port, tls, auth, username, password, groupid, smtpserverid);
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(ADD_SERVER, smtpserverid, description, host, port, tls, auth, username, password, groupid, enabled, SQLQueries.getCurrentDate(), emailfrom));
		
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SERVER, smtpserverid)).asXML();
	}
	
	public String updateServer(String xmlData) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		return updateServer(SAXReaderUtil.read(xmlData).selectSingleNode("/rs/row"));
	}
	public String updateServer(Node node) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		String smtpserverid = XMLHelper.getTextValueOf(node, "@smtpserverid");
		ErrorRaiser.throwIfNull(smtpserverid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String description = XMLHelper.getTextValueOf(node, "@description");
		if(Validator.isNotNull(description))
		{
			description = StringPool.APOSTROPHE + 
						  StringEscapeUtils.escapeSql(description) + 
						  StringPool.APOSTROPHE;
		}
		
		String emailfrom = XMLHelper.getTextValueOf(node, "@emailfrom");
		if(Validator.isNotNull(emailfrom))
		{
			emailfrom = StringPool.APOSTROPHE + 
						emailfrom + 
						StringPool.APOSTROPHE;
		}

		String host = XMLHelper.getTextValueOf(node, "@host");
		ErrorRaiser.throwIfNull(host, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String port = XMLHelper.getTextValueOf(node, "@port");
		ErrorRaiser.throwIfNull(port, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		try
		{
			Integer.parseInt(port);
		}
		catch (Exception e)
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}

		boolean tls = GetterUtil.getBoolean(XMLHelper.getTextValueOf(node, "@tls"), true);
		boolean auth =  GetterUtil.getBoolean(XMLHelper.getTextValueOf(node, "@auth"), true);
		boolean enabled =  GetterUtil.getBoolean(XMLHelper.getTextValueOf(node, "@enabled"), true);
		
		//comprobar referencias sólo en caso de smtp desactivado
		if(!enabled)
			checkReferencesSMTPserv(smtpserverid, true);
		
		String username = XMLHelper.getTextValueOf(node, "@username");
		if(Validator.isNotNull(username))
		{
			username = StringPool.APOSTROPHE + 
					   StringEscapeUtils.escapeSql(username) + 
					   StringPool.APOSTROPHE;
		}
		
		String password = XMLHelper.getTextValueOf(node, "@password");
		if(Validator.isNotNull(password))
		{
			password = StringPool.APOSTROPHE + 
					   password +
					   StringPool.APOSTROPHE;
		}

		String groupid = XMLHelper.getTextValueOf(node, "@groupid");
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		checkUnique(host, port, tls, auth, username, password, groupid, smtpserverid);
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_SERVER, description, host, port, tls, auth, username, password, enabled, SQLQueries.getCurrentDate(), emailfrom, smtpserverid));
		
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SERVER, smtpserverid)).asXML();
	}
	
	public String deleteServers(String xmlData) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException, JSONException, PortalException, SystemException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row/@smtpserverid");
		
		List<Node> nodes = xpath.selectNodes(dataRoot);
		ErrorRaiser.throwIfFalse((nodes != null && nodes.size() > 0), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		if (GetterUtil.getBoolean( XMLHelper.getStringValueOf(dataRoot, "@checkReferences"), true) )
			checkReferences(nodes);
		
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
		{
			long companyId = GroupMgr.getCompanyId();
			ErrorRaiser.throwIfFalse(companyId > 0);
			
			LiveConfiguration liveConf	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(companyId);
			String remoteIP 			= liveConf.getRemoteIterServer2().split(":")[0];
			int remotePort 				= Integer.valueOf(liveConf.getRemoteIterServer2().split(":")[1]);
			String remoteMethodPath 	= "/base-portlet/secure/json";
			
			List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
			remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.base.service.SMTPServerMgrServiceUtil"));
			remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"deleteServers"));
			remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[data]"));
			remoteMethodParams.add(new BasicNameValuePair("data", 				xmlData));
			
			String result = XMLIOUtil.executeJSONRemoteMethod2(companyId, remoteIP, remotePort, liveConf.getRemoteUserName(), liveConf.getRemoteUserPassword(), remoteMethodPath, remoteMethodParams);
			JSONObject json = JSONFactoryUtil.createJSONObject(result);
			
			String errorMsg = json.getString("exception");
			if(!errorMsg.isEmpty()) 
			{
				// Puede ser una excepción de tipo Iter, si no lo es devuelve
				// todo el texto y también se lanza porque era una excepción del
				// sistema
				String iterErrorMsg = ServiceErrorUtil.containIterException(errorMsg);
				throw new SystemException(iterErrorMsg.isEmpty() ? errorMsg : iterErrorMsg);
			}
			else
				PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_SERVERS, TeaserMgr.getInClauseSQL(nodes)));
		}
		else
			PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_SERVERS, TeaserMgr.getInClauseSQL(nodes)));
		
		return xmlData;
	}
	
	/**
	 * Importa los servidores parametrizados en el XML. Se insertan o actualizan en función de si ya existen en el entorno
	 * @param servers
	 * @throws ServiceError
	 * @throws DocumentException
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws SecurityException 
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	public Document importData(String data) throws ServiceError, DocumentException, NoSuchMethodException, SecurityException, IOException, SQLException, PortalException, SystemException
	{
		if (importWriteLock.tryLock())
		{
			try
			{
				Element root = SAXReaderUtil.read( data ).getRootElement();
				long groupId = XMLHelper.getLongValueOf(root, "@groupId");
		        if (groupId <= 0)
		        {
		        	String groupName = XMLHelper.getStringValueOf(root, "/rs/@groupName");
		        	ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		        	groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
		        }
		        ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
				boolean updtIfExist = GetterUtil.getBoolean(XMLHelper.getStringValueOf(root, "@updtIfExist"));
				
				boolean isImport = GetterUtil.getBoolean( XMLHelper.getTextValueOf(root, "@importProcess") );
				
				List<Node> serversList = root.selectNodes("row");
				
				for (int i = 0; i < serversList.size(); i++)
				{
					Element elem2Imp = (Element)serversList.get(i);
					
					// Se comprueba si el servidor ya existe
					String smtpserverid = "";
					boolean exist = false;
					
					if (isImport)
					{
						smtpserverid = getSMTPServerId(elem2Imp, groupId);
						exist = Validator.isNotNull(smtpserverid);
					}
					else
					{
						exist = existSMTPServer(elem2Imp);
					}
					
					ErrorRaiser.throwIfFalse(updtIfExist || !exist, IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, 
							String.format("%s(%s | %s | %s | %s)", IterAdmin.IA_CLASS_SMTPSERVER, 
									XMLHelper.getStringValueOf(elem2Imp, "@host",		""),
									XMLHelper.getStringValueOf(elem2Imp, "@port", 		""),
									XMLHelper.getStringValueOf(elem2Imp, "@username",	""),
									XMLHelper.getStringValueOf(elem2Imp, "@description","")));
					
					// Se crea el atributo groupid con el valor de este entorno
					elem2Imp.addAttribute("groupid", String.valueOf(groupId));

					if (exist)
					{
						if (isImport)
							elem2Imp.attribute("smtpserverid").setValue(smtpserverid);
						
						updateServer(elem2Imp);
					}
					else
					{
						if (isImport)
							elem2Imp.attribute("smtpserverid").detach();
						
						addServer(elem2Imp);
					}
				}
				return exportData(groupId);
			}
			finally
			{
				importWriteLock.unlock();
			}
		}
		else
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_PUBLISH_ALREADY_IN_PROCESS_ZYX);
			return null;
		}
	}
	
	/**
	 * Comprueba que exista un servidor SMTP con el id indicado. 
	 * @return true si existe un servidor con ese Id. false en caso contrario.
	 */
	private boolean existSMTPServer(Element server)
	{
		String smtpserverid = XMLHelper.getStringValueOf(server, "@smtpserverid");
		String sql = "SELECT smtpserverid FROM smtpserver WHERE smtpserverid='%s'";
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList(String.format(sql, smtpserverid));
		
		return Validator.isNotNull(result) && result.size() > 0 ? true : false;
	}

	/**
	 * Recupera el smtpserverid mediante su clave secundaria.
	 * @param server Element con la clave secundaria como atributos. El nombre del elemento es indiferente. Ej:
	 * {@code<row host="10.15.4.117" port="25" tls="true" auth="true" username="jmartin" password="drz1007nm" />}
	 * @param groupId el id del grupo. 
	 * @return el smtpserverid del servidor. Si no existe retorna null.
	 */
	private String getSMTPServerId(Element server, long groupId)
	{
		String host 	= XMLHelper.getStringValueOf(server, "@host");
		String port 	= XMLHelper.getStringValueOf(server, "@port");
		String tls 		= XMLHelper.getStringValueOf(server, "@tls");
		String auth 	= XMLHelper.getStringValueOf(server, "@auth");
		String username = XMLHelper.getStringValueOf(server, "@username");
		String password = XMLHelper.getStringValueOf(server, "@password");
		
		String sql = "SELECT smtpserverid FROM smtpserver WHERE host='%s' AND port=%s AND tls=%s AND auth=%s AND IFNULL(username, '')='%s' AND IFNULL(password, '')='%s' AND groupid=%d";
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList(String.format(sql, host, port, tls, auth, username, password, groupId));
		
		return Validator.isNotNull(result) && result.size() > 0 ? result.get(0).toString() : null;
	}
	
	private void checkReferences(List<Node> nodes) throws SecurityException, NoSuchMethodException, ServiceError 
	{
		StringBuffer currentErrorMsg = new StringBuffer();
		for(Node node:nodes)
		{
			String smtpserverid = node.getStringValue();
			
			StringBuffer errorStr = checkReferencesSMTPserv(smtpserverid, false);
			if(errorStr.length() > 0)
				currentErrorMsg.append(errorStr.append("\n\n"));	
		}
		ErrorRaiser.throwIfFalse(currentErrorMsg.length() == 0, IterErrorKeys.XYZ_ITR_SMTPSERVER_IS_BEEN_USED_ZYX, currentErrorMsg.toString());
	}
	
	private void checkUnique(String host, String port, boolean tls, boolean auth, String username, String password, String groupid, String smtpserverid) throws ServiceError
	{
		String usernameQuery = " IS NULL";
		if(Validator.isNotNull(username))
			usernameQuery = " = " + username;
		
		String passwordQuery = " IS NULL";
		if(Validator.isNotNull(password))
			passwordQuery = " = " + password;
		
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList(String.format(CHECK_UNIQUE, host, port, tls, auth, usernameQuery, passwordQuery, groupid, smtpserverid));
		if(result != null && result.size() > 0)
		{
			int count = Integer.valueOf(result.get(0).toString());
			if(count > 0)
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_ITR_UNQ_SMTPSERVER_HOST_PORT_TLS_AUTH_USER_PASS_GROUPID_ZYX);
		}
	}
	
	private StringBuffer checkReferencesSMTPserv(String smtpserverid,Boolean updateOperation) throws SecurityException, NoSuchMethodException, ServiceError 
	{
		StringBuffer errorMsg = new StringBuffer();
		ErrorRaiser.throwIfNull(smtpserverid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String inClause = StringPool.OPEN_PARENTHESIS + StringPool.APOSTROPHE + smtpserverid + StringPool.APOSTROPHE + StringPool.CLOSE_PARENTHESIS;
		
		String query  = String.format(CHECK_REFERENCES, inClause);
		Document docResultDoc = PortalLocalServiceUtil.executeQueryAsDom(query);
		
		errorMsg.append( getErrorMsg(docResultDoc));
		if(updateOperation)
			ErrorRaiser.throwIfFalse(errorMsg.length() == 0, IterErrorKeys.XYZ_ITR_SMTPSERVER_IS_BEEN_USED_ZYX, errorMsg.toString());
		
		return errorMsg;
	}
	
	private String getErrorMsg(Document rs)
	{
		StringBuffer error = new StringBuffer();
		
		XPath xpath = SAXReaderUtil.createXPath("//row");
		List<Node> nodes = xpath.selectNodes(rs);
		
		if( nodes.size() > 0 )
		{
			String smtphost = GetterUtil.getString(XMLHelper.getTextValueOf(nodes.get(0), "@smtphost"));
			error.append(smtphost + ": \n\n");
		}
		for(Node node:nodes)
		{
			String refTypeName = "";
			
			String reftype = GetterUtil.getString(XMLHelper.getTextValueOf(node, "@reftype"));
			String refname = "";
			if(reftype.equals("schedule_newsletter"))
			{
				refTypeName = IterKeys.TRAD_SCHEDULE_NEWSLETTER_TRAD;
				refname =  ": " +  GetterUtil.getString(XMLHelper.getTextValueOf(node, "@referencesname"));
			}
			else if(reftype.equals("form"))
			{
				refTypeName = IterKeys.TRAD_FORM_TRAD;
				refname =  ": " +  GetterUtil.getString(XMLHelper.getTextValueOf(node, "@referencesname"));	
			}
			else if(reftype.equals("user"))
				refTypeName = IterKeys.TRAD_REGISTER_FORGOT_USER_TRAD;
			
			
			error.append(refTypeName  + refname  + "\n");
		}
	
		return error.toString();
	}
	
	/**
	 * Publica al LIVE los servidores SMTP incluídos en la lista.
	 * @param serverIds: IDs de servidores SMTP separados por coma
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws ServiceError 
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 * @throws UnsupportedEncodingException 
	 */
	public void publishToLive(String serverIds) throws SecurityException, NoSuchMethodException, ServiceError, PortalException, SystemException, IOException
	{
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW) && Validator.isNotNull(serverIds))
		{
			if (exportWriteLock.tryLock())
			{
				try
				{
					// No se llama al exportData porque ya tenemos una lista de ids separados por comas. 
					Document serversDom = getDiscreteServers( serverIds );
					
					Element rootElement = serversDom.getRootElement();
					rootElement.addAttribute("groupName", XMLHelper.getStringValueOf(serversDom, "/rs/row/@groupName"));
					rootElement.addAttribute("updtIfExist", "true");
					
					long companyId = GroupMgr.getCompanyId();
					ErrorRaiser.throwIfFalse(companyId > 0);
		
					LiveConfiguration liveConf	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(companyId);
					String remoteIP 			= liveConf.getRemoteIterServer2().split(":")[0];
					int remotePort 				= Integer.valueOf(liveConf.getRemoteIterServer2().split(":")[1]);
					String remoteMethodPath 	= "/base-portlet/secure/json";
					
					List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
					remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.base.service.SMTPServerMgrServiceUtil"));
					remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"importData"));
					remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[data]"));
					remoteMethodParams.add(new BasicNameValuePair("data", 				serversDom.asXML()));
					
					String result = XMLIOUtil.executeJSONRemoteMethod2(companyId, remoteIP, remotePort, liveConf.getRemoteUserName(), liveConf.getRemoteUserPassword(), remoteMethodPath, remoteMethodParams);
					JSONObject json = JSONFactoryUtil.createJSONObject(result);
					
					String errorMsg = json.getString("exception");
					if(!errorMsg.isEmpty()) 
					{
						// Puede ser una excepción de tipo Iter, si no lo es devuelve
						// todo el texto y también se lanza porque era una excepción del
						// sistema
						String iterErrorMsg = ServiceErrorUtil.containIterException(errorMsg);
						throw new SystemException(iterErrorMsg.isEmpty() ? errorMsg : iterErrorMsg);
					}
				}
				finally
				{
					exportWriteLock.unlock();
				}
			}
			else
			{
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_PUBLISH_ALREADY_IN_PROCESS_ZYX);
			}
		}
	}

}