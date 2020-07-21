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

package com.protecmedia.iter.user.service.impl;

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
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.PropsValues;
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
import com.protecmedia.iter.base.service.util.IterAdmin;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.TeaserMgr;
import com.protecmedia.iter.user.service.base.UserProfileLocalServiceBaseImpl;
import com.protecmedia.iter.user.util.UserUtil;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

/**
 * The implementation of the user profile local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.user.service.UserProfileLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.user.service.UserProfileLocalServiceUtil} to access the user profile local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see com.protecmedia.iter.user.service.base.UserProfileLocalServiceBaseImpl
 * @see com.protecmedia.iter.user.service.UserProfileLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class UserProfileLocalServiceImpl extends UserProfileLocalServiceBaseImpl
{
	private Log _log = LogFactoryUtil.getLog(UserProfileLocalServiceImpl.class);
	
	private static ReadWriteLock exportGlobalLock = new ReentrantReadWriteLock();
	private static Lock exportWriteLock = exportGlobalLock.writeLock();
	
	private static ReadWriteLock importGlobalLock = new ReentrantReadWriteLock();
	private static Lock importWriteLock = importGlobalLock.writeLock();

	
	private final String GET_FIELDS = new StringBuilder()
		.append(" SELECT profilefieldid, userprofile.datafieldid, required, fieldtype, IF(structured, 'system', 'user') fieldclass, fieldname \n")
		.append(" FROM userprofile LEFT JOIN datafield ON userprofile.datafieldid = datafield.datafieldid                                     \n")
		.append(" WHERE (structured = TRUE OR IFNULL(delegationid, 0) = %d)                                                                   \n")
		.append("   AND fieldname NOT IN ('").append(StringUtil.merge(UserUtil.PRF_HIDE_FIELDS, "','")).append("')")
		.toString();
	
	private final String GET_FIELD_BY_ID = new StringBuilder()
		.append(" SELECT profilefieldid, userprofile.datafieldid, fieldname, required, fieldtype, IF(structured, 'system', 'user') fieldclass ")
		.append(" FROM userprofile LEFT JOIN datafield ON userprofile.datafieldid = datafield.datafieldid ")
		.append( " WHERE profilefieldid IN ('%s') " ).toString();
	
	private final String ADD_FIELD = new StringBuilder()
		.append("INSERT INTO userprofile (profilefieldid, datafieldid, fieldname, structured, required, delegationid) \n")
		.append("VALUES ('%s', (SELECT datafieldid FROM datafield WHERE fieldtype = '%s'), '%s', %b, %s, %d) ").toString();
	
	private final String UPDT_FIELD = "UPDATE userprofile SET fieldname = '%s', datafieldid = (SELECT datafieldid FROM datafield WHERE fieldtype = '%s') WHERE profilefieldid='%s'";
	
	private final String CHECK_SYTEM_FIELD = "SELECT COUNT(profilefieldid) structured FROM userprofile WHERE profilefieldid='%s' AND structured=true";
	
	private final String CHECK_FIELDS = "SELECT COUNT(profilefieldid) FROM userprofile WHERE profilefieldid IN %s AND structured=true";
	
	private final String DELETE_PROFILEFIELD = "DELETE FROM userprofile WHERE structured=false AND profilefieldid IN %s";
	
	private final String CHECK_FIELDS_REFERENCES = new StringBuilder()
	.append(" SELECT fieldname FROM userprofile                                             \n")
	.append(" INNER JOIN formfield ON (formfield.profilefieldid=userprofile.profilefieldid) \n")
	.append(" WHERE userprofile.profilefieldid IN %s")
	.toString();
	
	private final String CHECK_FIELD_TYPE = new StringBuilder()
	.append("SELECT CAST(fieldtype AS CHAR(10)) fieldtype FROM userprofile                 \n")
	.append("INNER JOIN datafield ON datafield.datafieldid = userprofile.datafieldid       \n")
	.append("INNER JOIN formfield ON formfield.profilefieldid = userprofile.profilefieldid \n")
	.append("WHERE userprofile.profilefieldid = '%s';")
	.toString();
	
	private final String CLASS_USER = "user";
	
	public Document getUserProfile(String groupid) throws ServiceError, NoSuchMethodException, NumberFormatException, PortalException, SystemException
	{
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		long delegationId = GroupLocalServiceUtil.getGroup(Long.valueOf(groupid)).getDelegationId();
		
		return PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_FIELDS, delegationId), new String[]{"fieldname"} );
	}
	
	public String addField( String xmlfield )throws DocumentException, ServiceError, NoSuchMethodException, IOException, SQLException, NumberFormatException, PortalException, SystemException
	{
		xmlfield = StringEscapeUtils.escapeSql(xmlfield);
		
		return addField( SAXReaderUtil.read(xmlfield).selectSingleNode("/rs/row") );
	}
	private String addField( Node fieldNode )throws DocumentException, ServiceError, NoSuchMethodException, IOException, SQLException, NumberFormatException, PortalException, SystemException
	{
		String groupid = XMLHelper.getTextValueOf(fieldNode, "@groupid");
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String fieldtype = XMLHelper.getTextValueOf(fieldNode, "@fieldtype");
		ErrorRaiser.throwIfNull(fieldtype, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String fieldname = fieldNode.selectSingleNode("fieldname").getStringValue();
		ErrorRaiser.throwIfNull(fieldname, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		boolean structured = CLASS_USER.equals(XMLHelper.getTextValueOf(fieldNode, "@fieldclass", CLASS_USER)) ? false : true;
		
		// Los campos estructurados se comparten para todas las delegaciones y siempre tienen delegación 0
		long delegationId = structured ? 0L : GroupLocalServiceUtil.getGroup(Long.valueOf(groupid)).getDelegationId();
		
		String profieldFieldUUID = XMLHelper.getTextValueOf(fieldNode, "@profilefieldid", SQLQueries.getUUID());
		
		boolean required = GetterUtil.getBoolean( XMLHelper.getTextValueOf(fieldNode, "@required") );
		
		PortalLocalServiceUtil.executeUpdateQuery( String.format(ADD_FIELD, profieldFieldUUID, fieldtype, fieldname, structured, required, delegationId) );
		
		return getFieldById( profieldFieldUUID ).asXML();
	}
	
	public String updateField( String xmlfield )throws DocumentException, ServiceError, SecurityException, NoSuchMethodException, IOException, SQLException
	{
		xmlfield = StringEscapeUtils.escapeSql(xmlfield);
		
		Element dataRoot = SAXReaderUtil.read(xmlfield).getRootElement();
		
		XPath xpath = SAXReaderUtil.createXPath("/rs/row");
		Node fieldNode = xpath.selectSingleNode(dataRoot);

		return updateField(fieldNode);
	}
	private String updateField( Node fieldNode )throws DocumentException, ServiceError, SecurityException, NoSuchMethodException, IOException, SQLException
	{
		String profilefieldid = XMLHelper.getTextValueOf(fieldNode, "@profilefieldid");
		ErrorRaiser.throwIfNull(profilefieldid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String fieldtype = XMLHelper.getTextValueOf(fieldNode, "@fieldtype");
		ErrorRaiser.throwIfNull(fieldtype, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String fieldname = fieldNode.selectSingleNode("fieldname").getStringValue();
		ErrorRaiser.throwIfNull(fieldname, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Si es un campo estructurado, no hace nada, ya que no pueden ser modificados
		Document upField = PortalLocalServiceUtil.executeQueryAsDom( String.format(CHECK_SYTEM_FIELD, profilefieldid) );
		if (XMLHelper.getLongValueOf(upField, "/rs/row/@structured") == 0)
		{
			// Comprueba si se está usando el campo en algún formulario para impedir que se cambie el tipo de datos.
			upField = PortalLocalServiceUtil.executeQueryAsDom( String.format(CHECK_FIELD_TYPE, profilefieldid) );
			String typeInUse = XMLHelper.getStringValueOf(upField, "/rs/row/@fieldtype");
				ErrorRaiser.throwIfFalse(Validator.isNull(typeInUse) || fieldtype.equals(typeInUse), IterErrorKeys.XYZ_ITR_E_CANT_UPDATE_FIELD_ZYX);
			
			PortalLocalServiceUtil.executeUpdateQuery( String.format(UPDT_FIELD, fieldname, fieldtype, profilefieldid) );
		}
		return getFieldById(profilefieldid).asXML();
	}
	
	public String deleteFields( String xmlfields )throws DocumentException, ServiceError, IOException, SQLException, SystemException, PortalException
	{
		Document d = SAXReaderUtil.read(xmlfields);
		Element dataRoot = d.getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row/@profilefieldid");
		
		List<Node> nodes = xpath.selectNodes(dataRoot);
		ErrorRaiser.throwIfFalse(nodes != null && nodes.size() > 0);
		
		///////////////////////////////////////////////////////////////////
		if (PropsValues.ITER_ENVIRONMENT.equals(IterKeys.ENVIRONMENT_PREVIEW))
		{
			long companyId = GroupMgr.getCompanyId();
			ErrorRaiser.throwIfFalse(companyId > 0);
	
			LiveConfiguration liveConf	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(companyId);
			String remoteIP 			= liveConf.getRemoteIterServer2().split(":")[0];
			int remotePort 				= Integer.valueOf(liveConf.getRemoteIterServer2().split(":")[1]);
			String remoteMethodPath 	= "/base-portlet/secure/json";
			
			List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
			remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.user.service.UserProfileServiceUtil"));
			remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"deleteFields"));
			remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[xmlfields]"));
			remoteMethodParams.add(new BasicNameValuePair("xmlfields", 		xmlfields));
			
			String result = XMLIOUtil.executeJSONRemoteMethod2(companyId, remoteIP, remotePort, liveConf.getRemoteUserName(), liveConf.getRemoteUserPassword(), remoteMethodPath, remoteMethodParams);
			JSONObject json = JSONFactoryUtil.createJSONObject(result);
			
			String errorMsg = json.getString("exception");
			if (!errorMsg.isEmpty()) 
			{
				String iterErrorMsg = ServiceErrorUtil.containIterException(errorMsg);
				throw new SystemException(iterErrorMsg.isEmpty() ? errorMsg : iterErrorMsg);
			}
		}
		///////////////////////////////////////////////////////////////////
		
		String inClauseSQL = TeaserMgr.getInClauseSQL(nodes);
		
		// Comprueba que no sea un campo estructurado
		List<Object> upFields = PortalLocalServiceUtil.executeQueryAsList( String.format(CHECK_FIELDS, inClauseSQL) );
		ErrorRaiser.throwIfFalse((upFields != null && upFields.size() == 1 && upFields.get(0) != null && upFields.get(0).toString().equals("0") ), IterErrorKeys.XYZ_ITR_E_SYSTEM_REQUIRED_FIELDS_ZYX);

		String checkRef = dataRoot.attributeValue("checkreferences");
		ErrorRaiser.throwIfNull(checkRef, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		if( checkRef.equals("1") )
		{
			upFields = PortalLocalServiceUtil.executeQueryAsList( String.format(CHECK_FIELDS_REFERENCES, inClauseSQL) );
			String exception = PropsValues.ITER_ENVIRONMENT.equals(IterKeys.ENVIRONMENT_PREVIEW) ? IterErrorKeys.XYZ_ITR_E_FIELD_HAS_REFERENCES_ZYX : IterErrorKeys.XYZ_ITR_E_FIELD_HAS_REFERENCES_IN_LIVE_ZYX;
			ErrorRaiser.throwIfFalse((upFields != null && upFields.size()==0), exception, getFieldsName(upFields));
		}
		
		PortalLocalServiceUtil.executeUpdateQuery( String.format(DELETE_PROFILEFIELD, inClauseSQL) );
		
		return xmlfields;
	}
	
	private String getFieldsName( List<Object> fieldsname )
	{
		StringBuilder retVal = new StringBuilder("\n");
		
		for( Object obj : fieldsname )
		{
			retVal.append("\n\t").append( obj.toString() );
		}
		
		return retVal.toString();
	}

	private Document getFieldById( String fieldId ) throws SecurityException, NoSuchMethodException
	{
		Document result = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_FIELD_BY_ID, fieldId), new String[]{"fieldname"} );
		
		if (_log.isDebugEnabled())
			_log.debug(result.asXML());
		
		return result;
	}
	
	/**
	 * 
	 * @param prfIds
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws ServiceError
	 * @throws PortalException
	 * @throws SystemException
	 * @throws UnsupportedEncodingException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public void publishToLive(String prfIds, String groupName) throws SecurityException, NoSuchMethodException, ServiceError, PortalException, SystemException, UnsupportedEncodingException, ClientProtocolException, IOException
	{
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
		{
			if (exportWriteLock.tryLock())
			{
				try
				{
					_publishToLive(prfIds, groupName);
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
	
	public Document exportData(Long groupId) throws ServiceError, DocumentException, SecurityException, NoSuchMethodException, NumberFormatException, PortalException, SystemException
	{
		Document dom = getUserProfile(String.valueOf(groupId));
		_processExportData(dom);
		
		return dom;
	}
	
	public Document exportData(List<String> ids) throws ServiceError, DocumentException, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse( Validator.isNotNull(ids), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document dom = getFieldById( StringUtil.merge(ids, "','") );
		_processExportData(dom);
		
		return dom;
	}
	
	/**
	 * Se procesasn los datos de los USERPROFILE que serán exportados.
	 * Se eliminan los DataFields de sistema porque ya se han creado en el sistema destino
	 * @param dom
	 */
	private void _processExportData(Document dom)
	{
		// Los DataFields de sistema NO se exportan porque ya se han creado en el sistema destino
		List<Node> systemNodes = dom.selectNodes("/rs/row[@fieldclass='system']");
		for (Node systemNode : systemNodes)
		{
			systemNode.detach();
		}
	}
	
	/**
	 * 
	 * @param prfIds
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws ServiceError
	 * @throws PortalException
	 * @throws SystemException
	 * @throws UnsupportedEncodingException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private void _publishToLive(String prfIds, String groupName) throws SecurityException, NoSuchMethodException, ServiceError, PortalException, SystemException, UnsupportedEncodingException, ClientProtocolException, IOException
	{
		ErrorRaiser.throwIfFalse(Validator.isNotNull(prfIds), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document domData = getFieldById( prfIds.replaceAll(",", "','") );
		domData.getRootElement().addAttribute("groupName", groupName);
		String data = domData.asXML();
		
		long companyId = GroupMgr.getCompanyId();
		ErrorRaiser.throwIfFalse(companyId > 0);

		LiveConfiguration liveConf	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(companyId);
		String remoteIP 			= liveConf.getRemoteIterServer2().split(":")[0];
		int remotePort 				= Integer.valueOf(liveConf.getRemoteIterServer2().split(":")[1]);
		String remoteMethodPath 	= "/base-portlet/secure/json";
		
		List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
		remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.user.service.UserProfileServiceUtil"));
		remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"importFields"));
		remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[fields]"));
		remoteMethodParams.add(new BasicNameValuePair("fields", 		data));
		
		String result = XMLIOUtil.executeJSONRemoteMethod2(companyId, remoteIP, remotePort, liveConf.getRemoteUserName(), liveConf.getRemoteUserPassword(), remoteMethodPath, remoteMethodParams);
		JSONObject json = JSONFactoryUtil.createJSONObject(result);
		
		String errorMsg = json.getString("exception");
		if (!errorMsg.isEmpty()) 
		{
			// Puede ser una excepción de tipo Iter, si no lo es devuelve
			// todo el texto y también se lanza porque era una excepción del
			// sistema
			String iterErrorMsg = ServiceErrorUtil.containIterException(errorMsg);
			throw new SystemException(iterErrorMsg.isEmpty() ? errorMsg : iterErrorMsg);
		}
	}
	/**
	 * 
	 * @param fields
	 * @throws ServiceError
	 * @throws DocumentException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws IOException
	 * @throws SQLException
	 * @throws SystemException
	 * @throws PortalException 
	 */
	public void importFields(String fields) throws ServiceError, DocumentException, NoSuchMethodException, SecurityException, IOException, SQLException, SystemException, PortalException
	{
		if (importWriteLock.tryLock())
		{
			try
			{
				_importFields(fields);
			}
			finally
			{
				importWriteLock.unlock();
			}
		}
		else
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_PUBLISH_ALREADY_IN_PROCESS_ZYX);
		}
	}
	/**
	 * 
	 * @param fields
	 * @throws ServiceError
	 * @throws DocumentException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws IOException
	 * @throws SQLException
	 * @throws SystemException
	 * @throws PortalException 
	 */
	private void _importFields(String fields) throws ServiceError, DocumentException, NoSuchMethodException, SecurityException, IOException, SQLException, SystemException, PortalException
	{
		ErrorRaiser.throwIfFalse(Validator.isNotNull(fields), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Element root = SAXReaderUtil.read( StringEscapeUtils.escapeSql(fields) ).getRootElement();
		long groupId = XMLHelper.getLongValueOf(root, "@groupId");
		if (groupId <= 0)
		{
			String groupName = XMLHelper.getTextValueOf(root, "@groupName");
			ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_INVALID_GROUP_NAME_ZYX);
			groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
		}
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		long delegationId = GroupLocalServiceUtil.getGroup(Long.valueOf(groupId)).getDelegationId();
		boolean updtIfExist = GetterUtil.getBoolean(XMLHelper.getStringValueOf(root, "@updtIfExist", "true"));
		boolean isImport = GetterUtil.getBoolean( XMLHelper.getTextValueOf(root, "@importProcess") );
		
		List<Node> list = root.selectNodes("row");
		String[] compareList = XMLHelper.getStringValues(list, isImport ? "./fieldname" : "@profilefieldid");
		String compareField = isImport ? "fieldname" : "profilefieldid";
		
		// Se consulta si los campos ya existen en el entorno
		Document existDom = PortalLocalServiceUtil.executeQueryAsDom( String.format("SELECT fieldname, profilefieldid, datafieldid FROM userprofile WHERE ((delegationid = %d AND structured = 0) OR structured = 1) AND %s IN ('%s')", delegationId, compareField, StringUtil.merge(compareList, "','")) );
		
		for (int i = 0; i < list.size(); i++)
		{
			Element elem2Imp = (Element)list.get(i);
			boolean exist = (XMLHelper.getLongValueOf(existDom, String.format("count(/rs/row[lower-case(@%s)='%s'])", compareField, compareList[i].toLowerCase())) > 0);
			ErrorRaiser.throwIfFalse( updtIfExist || !exist, IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, 
			 		 String.format("%s(%s)", IterAdmin.IA_CLASS_USERPROFILE, compareList[i]));

			elem2Imp.addAttribute("groupid", String.valueOf(groupId));
			if (exist)
			{
				// Los campos estructurados no cambian. sólo es necesario actualizar los de usuario.
				if (CLASS_USER.equals(XMLHelper.getTextValueOf(elem2Imp, "@fieldclass", CLASS_USER)))
				{
					if (isImport)
					{
						elem2Imp.addAttribute("profilefieldid", XMLHelper.getStringValueOf(existDom, String.format("/rs/row[lower-case(@fieldname)='%s']/@profilefieldid", compareList[i].toLowerCase())));
						elem2Imp.addAttribute("datafieldid", XMLHelper.getStringValueOf(existDom, String.format("/rs/row[lower-case(@fieldname)='%s']/@datafieldid", compareList[i].toLowerCase()))); // TODO quitar
					}
					updateField(elem2Imp);
				}
			}
			else
			{
				if (isImport)
				{
					elem2Imp.addAttribute("profilefieldid", StringPool.BLANK);
				}
				addField(elem2Imp);
			}
		}
	}
	
	public void matchProfileFieldsId() throws SecurityException, NoSuchMethodException, UnsupportedEncodingException, ClientProtocolException, IOException, PortalException, SystemException, ServiceError
	{
		// Sólo para el arranque del PREVIEW
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
		{	
			// Se comprueba la versión
			String versionStr = IterGlobal.getLastIterWebCmsVersion();
			String version = versionStr.substring(0, versionStr.lastIndexOf("."));
			int revision = Integer.parseInt(versionStr.substring(versionStr.lastIndexOf(".") + 1));
			
			// Si es menor que la 1.5.0.24
			if ("1.5.0".equals(version) && revision < 24)
			{
				if (_log.isDebugEnabled())
					_log.debug("Launching Profile Fields Update...");
				
				// Recupera los profilefieldid y fieldname de userprofile
				Document fields = PortalLocalServiceUtil.executeQueryAsDom( "SELECT profilefieldid, fieldname FROM userprofile" );
				
				// Los manda al LIVE por JSON
				long companyId = GroupMgr.getCompanyId();
				ErrorRaiser.throwIfFalse(companyId > 0);
				LiveConfiguration liveConf	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(companyId);
				String remoteIP 			= liveConf.getRemoteIterServer2().split(":")[0];
				
				if (Validator.isNotNull(remoteIP))
				{
					int remotePort 				= Integer.valueOf(liveConf.getRemoteIterServer2().split(":")[1]);
					String remoteMethodPath 	= "/base-portlet/secure/json";
					
					List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
					remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.user.service.UserProfileServiceUtil"));
					remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"updateProfileFieldsIds"));
					remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[fields]"));
					remoteMethodParams.add(new BasicNameValuePair("fields", 		fields.asXML()));
					
					String result = XMLIOUtil.executeJSONRemoteMethod2(companyId, remoteIP, remotePort, liveConf.getRemoteUserName(), liveConf.getRemoteUserPassword(), remoteMethodPath, remoteMethodParams);
					JSONObject json = JSONFactoryUtil.createJSONObject(result);
					
					String errorMsg = json.getString("exception");
					if (!errorMsg.isEmpty()) 
					{
						String iterErrorMsg = ServiceErrorUtil.containIterException(errorMsg);
						throw new SystemException(iterErrorMsg.isEmpty() ? errorMsg : iterErrorMsg);
					}
				}
				else
				{
					if (_log.isDebugEnabled())
						_log.debug("Invalid LIVE IP. Skipping Profile Fields Update.");
				}
			}
		}
	}
	
	public void updateProfileFieldsIds(String fields) throws SecurityException, NoSuchMethodException, DocumentException, IOException, SQLException, ServiceError
	{
		if (_log.isDebugEnabled())
			_log.debug("Checking Profile Fields to Update...");

		ErrorRaiser.throwIfFalse( !Validator.isNull(fields) );
		// Recupera los campos del PREVIEW
		Document backFields = SAXReaderUtil.read(fields);
		// Recupera los campos del LIVE
		Document liveFields = PortalLocalServiceUtil.executeQueryAsDom( "SELECT profilefieldid, fieldname FROM userprofile" );
		
		String query = new StringBuilder("UPDATE UserProfile t \n")
                                 .append("SET profilefieldid = ELT( FIELD( profilefieldid, %1$s), %2$s) \n")
                                 .append("WHERE profilefieldid IN (%1$s)")
                                 .toString();

		StringBuilder backIds = new StringBuilder();
		StringBuilder liveIds = new StringBuilder();
		
		// Compara los campos del Back y del Live y determina si necesita actualizar el ID
		for (Node field : backFields.selectNodes("/rs/row"))
		{
			String backFieldId   = XMLHelper.getStringValueOf(field, "@profilefieldid");
			String fieldName = XMLHelper.getStringValueOf(field, "@fieldname");
			
			// Busca por nombre el campo del Preview en el Live
			String liveFieldId = XMLHelper.getStringValueOf(liveFields, "/rs/row[lower-case(@fieldname)=\"" + fieldName.toLowerCase() + "\"]/@profilefieldid");
			
			// Si los IDs son distintos, inserta la actualización en la query
			if ( Validator.isNotNull(liveFieldId) && !backFieldId.equals(liveFieldId) )
			{
				if (backIds.length() > 0)
				{
					backIds.append(StringPool.COMMA_AND_SPACE);
					liveIds.append(StringPool.COMMA_AND_SPACE);
				}
				backIds.append(StringUtil.apostrophe(backFieldId));
				liveIds.append(StringUtil.apostrophe(liveFieldId));
			}
		}
		// Si hay campos que actualizar, termina de montar la sentencia y la lanza.
		if (backIds.length() > 0)
		{
			if (_log.isDebugEnabled())
				_log.debug("Updating Profile Fields...");
			PortalLocalServiceUtil.executeUpdateQuery(String.format(query, liveIds.toString(), backIds.toString()));
		}
		else
		{
			if (_log.isDebugEnabled())
				_log.debug("No Profile Fields to update.");
		}
	}
}