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
import java.sql.SQLException;
import java.util.HashMap;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigConstants;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.user.service.base.UserOperationsLocalServiceBaseImpl;
import com.protecmedia.iter.user.util.UserUtil;

/**
 * The implementation of the user operations local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.user.service.UserOperationsLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.user.service.UserOperationsLocalServiceUtil} to access the user operations local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see com.protecmedia.iter.user.service.base.UserOperationsLocalServiceBaseImpl
 * @see com.protecmedia.iter.user.service.UserOperationsLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class})
public class UserOperationsLocalServiceImpl extends UserOperationsLocalServiceBaseImpl
{
	private static final String GET_CONFIG 		= new StringBuffer(	"SELECT ").append(StringUtils.join(UserUtil.CDATA_COLUMNS, ",")	).append(
																		", l1.friendlyURL as registersuccesslayoutname, " 			).append(
																		"l2.friendlyURL as registererrorlayoutname, " 				).append(
																		"l3.friendlyURL as loginwitherrorlayoutname, " 				).append(
																		"l4.friendlyURL as forgeterrorlayoutname " 					).append(
																		"FROM Group_Config "										).append(
																		"INNER JOIN Layout l1 ON l1.uuid_= registersuccesslayout "	).append(
																		"INNER JOIN Layout l2 ON l2.uuid_= registererrorlayout "	).append(
																		"INNER JOIN Layout l3 ON l3.uuid_= loginwitherrorlayout "	).append(
																		"INNER JOIN Layout l4 ON l4.uuid_= forgeterrorlayout "		).append(
																		"WHERE Group_Config.groupId=%s "							).toString();
	
	private static final String CHECK_CONFIG 	= "SELECT COUNT(*) result FROM Group_Config WHERE groupId=%s";

	private static final String UPDATE_CONFIG 	= new StringBuffer(	"UPDATE Group_Config SET "										).append(	
																	StringUtils.join(UserUtil.CDATA_COLUMNS, "=%s, ")				).append(	
																	" =%s WHERE groupId=%s"										).toString();	
	
	private static final String INSERT_CONFIG = "INSERT INTO Group_Config (groupId, lastPublicationDate, " + 
	                                                                       StringUtils.join(UserUtil.CDATA_COLUMNS, ", ") + ") values(%s)";	
	
	private static final String GET_LAYOUT_UUID = "SELECT friendlyURL, uuid_ FROM layout WHERE groupId=%d AND friendlyURL in (%s);";
	
	public String getConfig(String groupid) throws SecurityException, ServiceError, NoSuchMethodException
	{
		return getConfigDom(groupid).asXML();
	}
	
	public Document getConfigDom(String groupid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String[] allCDATAs = (String[]) ArrayUtils.addAll(UserUtil.CDATA_COLUMNS, UserUtil.extraCDATAs);
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_CONFIG, groupid), allCDATAs);
	}
	
	    
	private static final String CHECK_OTP_FIELDS = new StringBuilder(
		"select count(*)																	\n").append(
		"from Form																			\n").append(
		"inner join FormTab on Form.formid = FormTab.formid									\n").append(
		"inner join FormField on FormTab.tabId = FormField.tabId							\n").append(
		"inner join userprofile on userprofile.profilefieldid = formField.profilefieldid	\n").append(
		"inner join datafield on datafield.dataFieldId = userprofile.dataFieldId			\n").append(
		"  where form.groupId = %d															\n").append(
		"    and formtype = 'registro'														\n").append(		
		"    and fieldType like 'otp_%%'													\n").toString();
		
	private void checkOTPFields(long groupId, String confirmationMode) throws Exception
	{
		if ( !GroupConfigConstants.REGISTER_CONFIRMATION_MODE.otp.toString().equals(confirmationMode) )
		{
			long numOTPFields = Long.parseLong( String.valueOf(PortalLocalServiceUtil.executeQueryAsList( String.format(CHECK_OTP_FIELDS, groupId) ).get(0)) );
			ErrorRaiser.throwIfFalse(numOTPFields == 0, IterErrorKeys.XYZ_ITR_E_REGISTER_OPTFIELDS_CONFIGURED_ZYX);
		}
	}

	
	public String setConfig(String xmlData) throws NumberFormatException, Exception
	{
		ErrorRaiser.throwIfNull(xmlData, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document xmlDataDoc = SAXReaderUtil.read(xmlData);
		
		String groupid = getConfigValue(xmlDataDoc, "@groupId");		
		String usernameexistsmsg 			= getConfigValue(xmlDataDoc, UserUtil.CDATA_COLUMNS[0]);
		String aboidexistsmsg 	 			= getConfigValue(xmlDataDoc, UserUtil.CDATA_COLUMNS[1]);
		String emailexistsmsg 	 			= getConfigValue(xmlDataDoc, UserUtil.CDATA_COLUMNS[2]);
		String registersuccesslayout  		= getConfigValue(xmlDataDoc, UserUtil.CDATA_COLUMNS[5]);
		String registererrorlayout  		= getConfigValue(xmlDataDoc, UserUtil.CDATA_COLUMNS[6]);
		String loginwitherrorlayout  		= getConfigValue(xmlDataDoc, UserUtil.CDATA_COLUMNS[7]);
		String forgetmsg  					= getConfigValue(xmlDataDoc, UserUtil.CDATA_COLUMNS[9]);
		String forgetusernamemsg  			= getConfigValue(xmlDataDoc, UserUtil.CDATA_COLUMNS[10]);
		String resetcredentialsmsg  		= getConfigValue(xmlDataDoc, UserUtil.CDATA_COLUMNS[11]);
		String forgetsuccesshtml  			= getConfigValue(xmlDataDoc, UserUtil.CDATA_COLUMNS[15]);
		String forgeterrorlayout  			= getConfigValue(xmlDataDoc, UserUtil.CDATA_COLUMNS[16]);
		String forgetpasswordmsg  			= getConfigValue(xmlDataDoc, UserUtil.CDATA_COLUMNS[17]);
		String forgetchallengeresponsemsg  	= StringPool.DOUBLE_APOSTROPHE;
/**
 * 		Esta opción está oculta en el interfaz, en el momento de mostrarla descomentar la linea inferior, donde se recoge el valor de un DOM
 * 	y borrar la superior, que está poniendo el valor a cadena vacia.
 * 
 * 	Tarea: 0008390, comentario: 0017837
 */
//		String forgetchallengeresponsemsg  	= getConfigValue(xmlDataDoc, UserUtil.CDATA_COLUMNS[18]);
		String forgetalertitle  			= getConfigValue(xmlDataDoc, UserUtil.CDATA_COLUMNS[19]);
		String forgetalertunexpectedmsg  	= getConfigValue(xmlDataDoc, UserUtil.CDATA_COLUMNS[20]);
		String forgetalertokbutton  		= getConfigValue(xmlDataDoc, UserUtil.CDATA_COLUMNS[21]);
		String forgetmailsubj  				= getConfigValue(xmlDataDoc, UserUtil.CDATA_COLUMNS[23]);
		
		String phoneexistsmsg 				= getConfigValue(xmlDataDoc, UserUtil.PHONE_EXISTS_MSG);

		String confirmationMode				= XMLHelper.getTextValueOf(xmlDataDoc, "/rs/row/" + UserUtil.REGISTER_CONFIRMATION_MODE, "");
		
		//lectura de campos obligatorios para modo de confirmacion por correo
		String registermailsubj  			= getConfigValue_ReqMailMode(xmlDataDoc, UserUtil.CDATA_COLUMNS[22], confirmationMode);
		String registerhtmlmail  			= getConfigValue_ReqMailMode(xmlDataDoc, UserUtil.CDATA_COLUMNS[3],  confirmationMode);
		String registersmptserverid  		= getConfigValue_ReqMailMode(xmlDataDoc, UserUtil.CDATA_COLUMNS[4],  confirmationMode);
		
		//lectura de campos obligatorios para modo de confirmacion por correo y sin modo
		String entermailmsg  				= getConfigValue_ReqMailWithoutMode(xmlDataDoc, UserUtil.CDATA_COLUMNS[8],  confirmationMode);
		String emailnoexistsmsg  			= getConfigValue_ReqMailWithoutMode(xmlDataDoc, UserUtil.CDATA_COLUMNS[12], confirmationMode);
		String forgethtmlmail  				= getConfigValue_ReqMailWithoutMode(xmlDataDoc, UserUtil.CDATA_COLUMNS[14], confirmationMode);
		String forgetsmptserverid  			= getConfigValue_ReqMailWithoutMode(xmlDataDoc, UserUtil.CDATA_COLUMNS[13], confirmationMode);
		
		
		//lectura de campos obligatorios para modo de confirmacion por código OTP
		String otpGenerationFail 			= getConfigValue_ReqOTPCodeMode(xmlDataDoc, UserUtil.OTP_GENERATION_HAS_FAILED_MSG, confirmationMode);
		String otpValidationFail 			= getConfigValue_ReqOTPCodeMode(xmlDataDoc, UserUtil.OTP_VALIDATION_HAS_FAILED_MSG, confirmationMode);
		String enterphonemsg 				= getConfigValue_ReqOTPCodeMode(xmlDataDoc, UserUtil.ENTER_PHONE_MSG, confirmationMode);
		String phoneNotExistsMsg 			= getConfigValue_ReqOTPCodeMode(xmlDataDoc, UserUtil.PHONE_NOT_EXISTS_MSG, confirmationMode);
		String otpSendMsgFail 				= getConfigValue_ReqOTPCodeMode(xmlDataDoc, UserUtil.OTP_SENDMSG_HAS_FAILED_MSG, confirmationMode);
		
		// Comprobamos que existe el registro en la tabla group_config
		boolean groupConfigExists = true;
		try{
			checkConfig(groupid);
		}catch(Exception e){
			groupConfigExists = false; 
		}		
		
		checkOTPFields( Long.valueOf(groupid), confirmationMode);
		
		
		if (groupConfigExists)
		{
			PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_CONFIG,
													  StringUtil.apostrophe(usernameexistsmsg),
													  StringUtil.apostrophe(aboidexistsmsg),
													  StringUtil.apostrophe(emailexistsmsg), 
													  Validator.isNull(registerhtmlmail) ? StringPool.NULL : StringUtil.apostrophe(registerhtmlmail),
													  Validator.isNull(registersmptserverid) ? StringPool.NULL : StringUtil.apostrophe(registersmptserverid),
													  StringUtil.apostrophe(registersuccesslayout),
													  StringUtil.apostrophe(registererrorlayout),
													  StringUtil.apostrophe(loginwitherrorlayout),
													  StringUtil.apostrophe(entermailmsg),
													  StringUtil.apostrophe(forgetmsg), 
													  StringUtil.apostrophe(forgetusernamemsg), 
													  StringUtil.apostrophe(resetcredentialsmsg),
													  StringUtil.apostrophe(emailnoexistsmsg), 
													  Validator.isNull(forgetsmptserverid) ? StringPool.NULL : StringUtil.apostrophe(forgetsmptserverid),
													  Validator.isNull(forgethtmlmail) ? StringPool.NULL : StringUtil.apostrophe(forgethtmlmail),
													  StringUtil.apostrophe(forgetsuccesshtml), 
													  StringUtil.apostrophe(forgeterrorlayout), 
													  StringUtil.apostrophe(forgetpasswordmsg),
													  forgetchallengeresponsemsg,
													  StringUtil.apostrophe(forgetalertitle), 
													  StringUtil.apostrophe(forgetalertunexpectedmsg) ,
													  StringUtil.apostrophe(forgetalertokbutton), 
													  StringUtil.apostrophe(registermailsubj),
													  StringUtil.apostrophe(forgetmailsubj), 
													  StringUtil.apostrophe(otpGenerationFail),
													  StringUtil.apostrophe(otpValidationFail ),
													  Validator.isNull(otpSendMsgFail) ? StringPool.NULL : StringUtil.apostrophe(otpSendMsgFail),
													  Validator.isNull(confirmationMode)? StringPool.NULL : StringUtil.apostrophe(confirmationMode),
													  StringUtil.apostrophe(enterphonemsg),
													  StringUtil.apostrophe(phoneNotExistsMsg),
													  StringUtil.apostrophe(phoneexistsmsg),
													  groupid));
		}
		else
		{
			final String values = groupid + ", sysdate(), " +
			(Validator.isNull(usernameexistsmsg)          ? "null" : "'" + StringEscapeUtils.escapeSql(usernameexistsmsg)          + "'") + ", " +
			(Validator.isNull(aboidexistsmsg)             ? "null" : "'" + StringEscapeUtils.escapeSql(aboidexistsmsg)             + "'") + ", " +
			(Validator.isNull(emailexistsmsg)             ? "null" : "'" + StringEscapeUtils.escapeSql(emailexistsmsg)             + "'") + ", " +
			(Validator.isNull(registerhtmlmail)           ? "null" : "'" + StringEscapeUtils.escapeSql(registerhtmlmail)           + "'") + ", " +
			(Validator.isNull(registersmptserverid)       ? "null" : "'" + StringEscapeUtils.escapeSql(registersmptserverid)       + "'") + ", " +
			(Validator.isNull(registersuccesslayout)      ? "null" : "'" + StringEscapeUtils.escapeSql(registersuccesslayout)      + "'") + ", " +
			(Validator.isNull(registererrorlayout)        ? "null" : "'" + StringEscapeUtils.escapeSql(registererrorlayout)        + "'") + ", " +
			(Validator.isNull(loginwitherrorlayout)       ? "null" : "'" + StringEscapeUtils.escapeSql(loginwitherrorlayout)       + "'") + ", " +
			(Validator.isNull(entermailmsg)               ? "null" : "'" + StringEscapeUtils.escapeSql(entermailmsg)               + "'") + ", " +
			(Validator.isNull(forgetmsg)                  ? "null" : "'" + StringEscapeUtils.escapeSql(forgetmsg)                  + "'") + ", " +
			(Validator.isNull(forgetusernamemsg)          ? "null" : "'" + StringEscapeUtils.escapeSql(forgetusernamemsg)          + "'") + ", " +
			(Validator.isNull(resetcredentialsmsg)        ? "null" : "'" + StringEscapeUtils.escapeSql(resetcredentialsmsg)        + "'") + ", " +
			(Validator.isNull(emailnoexistsmsg)           ? "null" : "'" + StringEscapeUtils.escapeSql(emailnoexistsmsg)           + "'") + ", " +
			(Validator.isNull(forgetsmptserverid)         ? "null" : "'" + StringEscapeUtils.escapeSql(forgetsmptserverid)         + "'") + ", " +
			(Validator.isNull(forgethtmlmail)             ? "null" : "'" + StringEscapeUtils.escapeSql(forgethtmlmail)             + "'") + ", " +
			(Validator.isNull(forgetsuccesshtml)          ? "null" : "'" + StringEscapeUtils.escapeSql(forgetsuccesshtml)          + "'") + ", " +
			(Validator.isNull(forgeterrorlayout)          ? "null" : "'" + StringEscapeUtils.escapeSql(forgeterrorlayout)          + "'") + ", " +
			(Validator.isNull(forgetpasswordmsg)          ? "null" : "'" + StringEscapeUtils.escapeSql(forgetpasswordmsg)          + "'") + ", " +
			(Validator.isNull(forgetchallengeresponsemsg) ? "null" : "'" + StringEscapeUtils.escapeSql(forgetchallengeresponsemsg) + "'") + ", " +
			(Validator.isNull(forgetalertitle)            ? "null" : "'" + StringEscapeUtils.escapeSql(forgetalertitle)            + "'") + ", " +
			(Validator.isNull(forgetalertunexpectedmsg)   ? "null" : "'" + StringEscapeUtils.escapeSql(forgetalertunexpectedmsg)   + "'") + ", " +
			(Validator.isNull(forgetalertokbutton)        ? "null" : "'" + StringEscapeUtils.escapeSql(forgetalertokbutton)        + "'") + ", " +
			(Validator.isNull(registermailsubj)           ? "null" : "'" + StringEscapeUtils.escapeSql(registermailsubj)           + "'") + ", " +
			(Validator.isNull(forgetmailsubj)             ? "null" : "'" + StringEscapeUtils.escapeSql(forgetmailsubj)             + "'") + ", " +
			(Validator.isNull(otpGenerationFail)          ? "null" : "'" + StringEscapeUtils.escapeSql(otpGenerationFail)          + "'") + ", " +
			(Validator.isNull(otpValidationFail)          ? "null" : "'" + StringEscapeUtils.escapeSql(otpValidationFail)          + "'") + ", " +
			(Validator.isNull(otpSendMsgFail)             ? "null" : "'" + StringEscapeUtils.escapeSql(otpSendMsgFail)         	   + "'") + ", " +
			(Validator.isNull(confirmationMode)           ? "null" : "'" + StringEscapeUtils.escapeSql(confirmationMode)           + "'") + ", " +
			(Validator.isNull(enterphonemsg)              ? "null" : "'" + StringEscapeUtils.escapeSql(enterphonemsg)              + "'") + ", " +
			(Validator.isNull(phoneNotExistsMsg)          ? "null" : "'" + StringEscapeUtils.escapeSql(phoneNotExistsMsg)          + "'") + ", " +
			(Validator.isNull(phoneexistsmsg)             ? "null" : "'" + StringEscapeUtils.escapeSql(phoneexistsmsg)             + "'").toString();
		
			
			PortalLocalServiceUtil.executeUpdateQuery(String.format(INSERT_CONFIG, values, groupid));
		}
		
		return getConfig(groupid);
	}
	
	public String getConfigValue(Document doc, String field) throws ServiceError
	{
		String data = XMLHelper.getTextValueOf(doc, "/rs/row/" + field);
		ErrorRaiser.throwIfNull(data, IterErrorKeys.XYZ_ITR_E_GROUP_INVALID_CONFIG_ZYX);
		
		return StringEscapeUtils.escapeSql(data);
	}
	
	/* Retorna el valor de los campos obligatorios para el modo de confirmación de registro por correo. 
	 * Comprueba que en ese modo los datos no se reciban vacíos, lanzando un error si se da el caso */
	public String getConfigValue_ReqMailMode(Document doc, String field, String confirmationMode) throws ServiceError
	{
		String data = XMLHelper.getTextValueOf(doc, "/rs/row/" + field);
		
		if(Validator.isNull(data))
		{
			if( GroupConfigConstants.REGISTER_CONFIRMATION_MODE.email.toString().equals(confirmationMode) )
				ErrorRaiser.throwIfNull(data, IterErrorKeys.XYZ_ITR_E_GROUP_INVALID_CONFIG_ZYX);
		}
		else
		{
			data = StringEscapeUtils.escapeSql(data);
		}
		
		return data;
	}
	
	/* Retorna el valor de los campos obligatorios para el modo de confirmación de registro por correo y sin modo. 
	 * Comprueba que en cualquiera de esos modos los datos no se reciban vacíos, lanzando un error si se da el caso */
	public String getConfigValue_ReqMailWithoutMode(Document doc, String field, String confirmationMode) throws ServiceError
	{
		String data = XMLHelper.getTextValueOf(doc, "/rs/row/" + field);
		
		if(Validator.isNull(data))
		{
			if( !GroupConfigConstants.REGISTER_CONFIRMATION_MODE.otp.toString().equals(confirmationMode) )
				ErrorRaiser.throwIfNull(data, IterErrorKeys.XYZ_ITR_E_GROUP_INVALID_CONFIG_ZYX);
		}
		else
		{
			data = StringEscapeUtils.escapeSql(data);
		}
		
		return data;
	}
	
	/* Retorna el valor de los campos obligatorios para el modo de confirmación de registro por código OTP. 
	 * Comprueba que en ese modo los datos no se reciban vacíos, lanzando un error si se da el caso */
	public String getConfigValue_ReqOTPCodeMode(Document doc, String field, String confirmationMode) throws ServiceError
	{
		String data = XMLHelper.getTextValueOf(doc, "/rs/row/" + field);
		
		if(Validator.isNull(data))
		{
			if( GroupConfigConstants.REGISTER_CONFIRMATION_MODE.otp.toString().equals(confirmationMode) )
				ErrorRaiser.throwIfNull(data, IterErrorKeys.XYZ_ITR_E_GROUP_INVALID_CONFIG_ZYX);
		}
		else
		{
			data = StringEscapeUtils.escapeSql(data);
		}
		
		return data;
	}
	
	public void checkConfig(String groupId) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document docData = PortalLocalServiceUtil.executeQueryAsDom(String.format(CHECK_CONFIG, groupId));
		String count = GetterUtil.getString(XMLHelper.getTextValueOf(docData, "rs/row/@result"), "0");
		
		ErrorRaiser.throwIfFalse(Integer.valueOf(count) == 1, IterErrorKeys.XYZ_ITR_E_GROUP_CONFIG_NOT_FOUND_ZYX);
	}
	
	public Document exportData(String params) throws DocumentException, ServiceError, PortalException, SystemException, SecurityException, NoSuchMethodException
	{
		Element root 	= SAXReaderUtil.read(params).getRootElement();
		String groupName= XMLHelper.getStringValueOf(root, "@groupName");
		ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		long groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return getConfigDom(String.valueOf(groupId));
	}
	
	public void importData(String data) throws NumberFormatException, Exception
	{
        Document dom = SAXReaderUtil.read(data);
        
        int numNodes = dom.selectNodes("/rs/row").size();
        ErrorRaiser.throwIfFalse(numNodes == 0 || numNodes == 1, com.liferay.portal.kernel.error.IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        
        if (numNodes == 1)
        {
	        String groupName = XMLHelper.getStringValueOf(dom, "/rs/@groupName");
	        ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
	        long groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
	        ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
	        // Elimima el atributo groupName que viene al importar e introduce el groupId.
	        dom.getRootElement().attribute("groupName").detach();
	        Element row = dom.getRootElement().element("row");
	        row.addAttribute("groupId", String.valueOf(groupId));
	        
	        // Recupera los UUID de los layout
	        StringBuilder names = new StringBuilder();
	        HashMap<String, String> LUT = new HashMap<String, String>();

	        setLayoutToImport(row, "//registersuccesslayoutname", "registersuccesslayout", names, LUT);
	        setLayoutToImport(row, "//registererrorlayoutname", "registererrorlayout", names, LUT);
	        setLayoutToImport(row, "//loginwitherrorlayoutname", "loginwitherrorlayout", names, LUT);
	        setLayoutToImport(row, "//forgeterrorlayoutname", "forgeterrorlayout", names, LUT);
	        
	        if (Validator.isNotNull(names.toString()))
	        {
	        	Document domLayouts = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_LAYOUT_UUID, groupId, names.toString()));
	        	String uuid = null;
	        	for (String key : LUT.keySet())
	        	{
	        		String friendlyURL = LUT.get(key);
	        		uuid = XMLHelper.getStringValueOf(domLayouts.getRootElement(), "//row[@friendlyURL='" + friendlyURL + "']/@uuid_");
	        		if (Validator.isNotNull(uuid))
	        			row.element(key).setText(uuid);
	        		else
	        			row.element(key).setText(friendlyURL);
	        	}
	        }
	        
	        // Importa la configuración
	        setConfig(dom.asXML());
        }
	}
	
	private void setLayoutToImport(Element row, String layouttag, String idtag, StringBuilder names, HashMap<String, String> LUT) throws ServiceError
	{
		ErrorRaiser.throwIfFalse(Validator.isNotNull(names) && Validator.isNotNull(LUT), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		String layoutname = null;
		layoutname = XMLHelper.getStringValueOf(row, layouttag);
        if (Validator.isNull(layoutname))
        	ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        else
        {
        	names.append(Validator.isNull(names.toString()) ? StringPool.BLANK : StringPool.COMMA_AND_SPACE).append(StringUtil.apostrophe(layoutname));
        	LUT.put(idtag, layoutname);
        }
	}
}
