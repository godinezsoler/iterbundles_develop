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

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GroupConfigConstants;
import com.liferay.portal.kernel.util.GroupConfigTools;
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
import com.liferay.portal.kernel.xml.XSLUtil;
import com.liferay.portal.model.UserConstants;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.IterTabsLocalServiceUtil;
import com.protecmedia.iter.base.service.base.IterFieldLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.service.util.TeaserMgr;

/**
 * The implementation of the iter field local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.IterFieldLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.IterFieldLocalServiceUtil} to access the iter field local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.IterFieldLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.IterFieldLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class IterFieldLocalServiceImpl extends IterFieldLocalServiceBaseImpl
{	
	private static Log _log = LogFactoryUtil.getLog(IterFieldLocalServiceImpl.class);
	
	private final static String GET_CONFIRMATION_MODE = new StringBuilder(
		"SELECT registerconfirmationmode							\n").append(
		"FROM Group_Config											\n").append(
		"INNER JOIN Group_ ON Group_.groupId = Group_Config.groupId	\n").append(
		"	WHERE Group_.name = '%s'								\n").toString();
	
	private final String GET_FIELD = new StringBuilder()
	.append(" SELECT IFNULL(ff.datafieldid, up.profilefieldid) datafieldid, htmlname,      \n")
	.append("        IFNULL(df.fieldtype, udf.fieldtype) fieldtype,                        \n")
	.append("        IF(IFNULL(structured, false), 'system', 'user') fieldclass,           \n")
	.append("        inputctrl, validator, labelbefore, labelafter, tooltip, defaultvalue, \n")
	.append("        editable, ff.required, needconfirm, repeatable, fieldorder, css       \n")
	.append(" FROM formfield ff                                                            \n")
	.append(" LEFT JOIN datafield df ON ff.datafieldid=df.datafieldid                      \n")
	.append(" LEFT JOIN userprofile up ON ff.profilefieldid = up.profilefieldid            \n")
	.append(" LEFT JOIN datafield udf ON up.datafieldid=df.datafieldid                     \n")
	.append(" WHERE fieldid='%s'                                                           \n")
	.toString();
	
	private final String DELETE_FIELDS = "DELETE FROM formfield WHERE fieldid IN %s";
	
	private final String GET_FIELD_BY_ID = new StringBuilder()
	.append(" SELECT fieldid, IFNULL(udf.fieldtype, df.fieldtype) fieldtype, fieldname, fieldorder,           \n")
	.append("        labelbefore, labelafter, formfield.required, IF(structured, 'system', 'user') fieldclass \n")
	.append(" FROM formfield                                                                                  \n")
	.append(" LEFT JOIN datafield df ON formfield.datafieldid = df.datafieldid                                \n")
	.append(" LEFT JOIN userprofile ON formfield.profilefieldid = userprofile.profilefieldid                  \n")
	.append(" LEFT JOIN datafield udf ON userprofile.datafieldid = udf.datafieldid                            \n")
	.append(" WHERE fieldid='%s'                                                                              \n")
	.toString();
	
	private final String CHEK_NEW_HTML_NAME = new StringBuilder("SELECT COUNT(htmlname) ")
									.append(" FROM formfield LEFT JOIN formtab ON (formfield.tabid=formtab.tabid) ")
									.append(" WHERE htmlname='%s' AND formtab.formid='%s' ").toString();

	private final String CHEK_EDIT_HTML_NAME = new StringBuilder("SELECT COUNT(htmlname) ")
									.append(" FROM formfield LEFT JOIN formtab ON (formfield.tabid=formtab.tabid) ")
									.append(" WHERE htmlname='%s' AND formtab.formid='%s' AND fieldid<>'%s'").toString();
	
	private final String GET_UPDATED_FIELDS = "SELECT fieldid, tabid, fieldorder FROM formfield WHERE fieldorder>=%d AND fieldorder<=%d AND tabid='%s' ORDER BY fieldorder";
	private final String UPDATE_FIELDS_PRIORITY = "UPDATE formfield SET fieldorder=fieldorder %s 1 WHERE tabid='%s' AND fieldid!='%s' AND fieldorder>=%d AND fieldorder<=%d";
	
	private final String GET_PROFILE_FIELDS = new StringBuilder()
	.append(" SELECT up.profilefieldid datafieldid, up.required, IF(structured, 'system', 'user') fieldclass, df.fieldtype, up.fieldname \n")
	.append(" FROM userprofile up INNER JOIN datafield df ON up.datafieldid=df.datafieldid \n ")
	.append(" WHERE structured=true OR up.delegationid = %d ").toString();
	
	private final String CHECK_REGISTER_FORM = new StringBuilder()
	.append( "SELECT fieldname                                          \n")
	.append( "FROM userprofile                                          \n")
	.append( "WHERE required AND structured AND profilefieldid NOT IN ( \n")
	.append( "    SELECT ff.profilefieldid                              \n")
	.append( "   	FROM formfield ff                                   \n")
	.append( "    LEFT JOIN formtab ft ON (ff.tabid=ft.tabid)  	        \n")
	.append( "    WHERE ft.formid = '%s'                                \n")
	.append( ")                                                         \n")
	.toString();
	
	private final String CHECK_FIELDS = new StringBuilder()
	.append(" SELECT COUNT(up.profilefieldid)                                  \n")
	.append(" FROM userprofile up                                              \n")
	.append(" INNER JOIN formfield ff ON ff.profilefieldid = up.profilefieldid \n")
	.append(" WHERE up.required AND structured AND ff.fieldid IN %s            \n")
	.toString();
	
	private final String CHECK_REGISTER_NEW_FIELD = new StringBuilder(" SELECT COUNT(fieldid) ")
									.append(" FROM formfield ")
									.append(" LEFT JOIN formtab ON (formfield.tabid=formtab.tabid) ")
									.append(" RIGHT JOIN userprofile ON (formfield.profilefieldid = userprofile.profilefieldid) ")
									.append(" WHERE formfield.profilefieldid = '%s' AND formtab.formid='%s' ").toString();
	
	private final String CHECK_REGISTER_EDIT_FIELD = new StringBuilder()
	.append(" SELECT COUNT(fieldid)                                                             \n")
	.append(" FROM formfield                                                                    \n")
	.append(" INNER JOIN userprofile ON formfield.profilefieldid = userprofile.profilefieldid   \n")
	.append(" INNER JOIN formtab     ON formfield.tabid = formtab.tabid                         \n")
	.append(" WHERE formfield.profilefieldid = '%s' AND fieldid <> '%s' AND formtab.formid='%s'   ")
	.toString();
	
	private final String INSERT_INTO_FIELD_RECEIVED = new StringBuilder(" INSERT INTO fieldreceived(fieldreceivedid, formreceivedid, fieldid, fieldvalue, binfieldvalueid) ")
									.append(" SELECT ITR_UUID(), formreceivedid, '%s', NULL, NULL ")
									.append(" FROM formreceived ")
									.append(" WHERE formid='%s' ").toString();
	
	private final String GET_FORMTYPE = "select formtype from form where formid='%s'";
	
	private final String GET_SYSTEM_PROFILEFIELDID = new StringBuilder()
	.append(" SELECT profilefieldid FROM userprofile ")
	.append(" WHERE fieldname = '%s' AND %s ")
	.toString();
	
	private final String RADIOBTT = "radiobutton";
	private final String LISTCTRL = "listctrl";
	private final String DROPDOWNLIST = "dropdownlist";
	private final String CHECKBOX = "checkbox";
	
	private final String[] inputCtrlTypes = new String[]{RADIOBTT, LISTCTRL, DROPDOWNLIST, CHECKBOX};
	
	public String getProfileFields(String groupId) throws ServiceError, SecurityException, NoSuchMethodException, NumberFormatException, PortalException, SystemException
	{
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		long delegationId = GroupLocalServiceUtil.getGroup(Long.valueOf(groupId)).getDelegationId();
		
		String result = "";
		
		result = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_PROFILE_FIELDS, delegationId) ).asXML();
		
		return result;
	}
	
	public String getField( String fieldId ) throws ServiceError, NoSuchMethodException
	{
		String result = "";
		
		ErrorRaiser.throwIfNull(fieldId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document d = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_FIELD, fieldId), new String[]{"labelbefore", "labelafter", "tooltip", "defaultvalue", "inputctrl", "validator"} );
		
		String xslpath = new StringBuilder("").append(File.separatorChar).append("base-portlet")
										.append(File.separatorChar).append("xsl")
										.append(File.separatorChar).append("getField.xsl").toString();
		
		result = XSLUtil.transformXML(d.asXML(), xslpath );
		
		return result;
	}
	
	public String addField( String xmlData ) throws Exception
	{
		return addField( SAXReaderUtil.read(xmlData).getRootElement() );
	}
	
	
	
	/**
	 * Lanza el error XYZ_ITR_E_REGISTER_CONFIRM_BY_OTPCODE_UNSELECTED_ZYX si se configura un 'Modo de confirmación de registro' 
	 * distinto de 'Código OTP', y se añade el campo "otp_button" u "otp_code".  
	 * @param fieldtype
	 * @throws Exception 
	 */
	private void checkOTPFields(long groupId, String fieldtype) throws Exception
	{
		if (PropsValues.IS_PREVIEW_ENVIRONMENT && (fieldtype.equals(UserConstants.PRF_FIELD_OTP_BUTTON) || fieldtype.equals(UserConstants.PRF_FIELD_OTP_CODE)))
		{
			String sql = String.format(GET_CONFIRMATION_MODE, GroupLocalServiceUtil.getGroup(groupId).getName());
			_log.debug(sql);
			
			Element root = PortalLocalServiceUtil.executeRemoteQueryAsDom( sql ).getRootElement();
			String mode  = "";
			
			// Si no tiene el atributo es porque posiblemente NO exista el LIVE, caso de los ordenadores de demo
			if (root.attributeCount() == 0 && root.elements().size() == 0 && root.getName().equals("rs"))
			{
				_log.debug("Checking OTP fields in a demostration system");
				mode = GroupConfigTools.getGroupConfigFieldFromDB(groupId, GroupConfigConstants.FIELD_REGISTER_CONFIRMATION_MODE);
			}
			else
			{
				_log.debug("Getting register confirmation mode in Live");
				mode = XMLHelper.getTextValueOf(root, "row/@registerconfirmationmode", "");
			}
			
			ErrorRaiser.throwIfFalse( GroupConfigConstants.REGISTER_CONFIRMATION_MODE.otp.toString().equals(mode), IterErrorKeys.XYZ_ITR_E_REGISTER_CONFIRM_BY_OTPCODE_UNSELECTED_ZYX);
		}
	}
	
	public String addField( Element e ) throws Exception
	{
		String query = "";
		
		// Si la operacion de añadir trae fieldid, es porque se trata de una operacion importacion.
		String fieldId = e.attributeValue("fieldid");
		if(Validator.isNull(fieldId))
			fieldId = StringPool.NULL;
		else
			fieldId = StringUtil.apostrophe(fieldId);
		
		String tabid = e.attributeValue("tabid");
		ErrorRaiser.throwIfNull(tabid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		String formid = e.attributeValue("formid");
		ErrorRaiser.throwIfNull(formid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		String groupid = e.attributeValue("groupid");
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		long delegationId = GroupLocalServiceUtil.getGroup(Long.valueOf(groupid)).getDelegationId();
		
		Document formtypeDom = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_FORMTYPE, formid) );
		String formtype = XMLHelper.getTextValueOf( formtypeDom.selectSingleNode("/rs/row"), "@formtype" );
		
		boolean isRegistryForm = formtype.equalsIgnoreCase("registro");
		
		String datafieldid = e.attributeValue("datafieldid");
		if (isRegistryForm)
		{
			datafieldid = updateSystemProfileField(e, datafieldid, delegationId);
			query = String.format(CHECK_REGISTER_NEW_FIELD, datafieldid, formid);
			List<Object> existsField = PortalLocalServiceUtil.executeQueryAsList( query );
			ErrorRaiser.throwIfFalse((existsField != null && existsField.size() == 1 && existsField.get(0) != null && existsField.get(0).toString().equals("0") ), IterErrorKeys.XYZ_ITR_E_DUPLICATE_FIELD_ZYX);
			datafieldid = "'"+datafieldid+"'";
		}
		else
		{
			datafieldid = StringPool.NULL;
		}
		
		String editable = e.attributeValue("editable");
		String css = e.attributeValue("css");
		if( Validator.isNotNull(css) )
			css = StringUtil.apostrophe( StringEscapeUtils.escapeSql(css) );
		else
			css = StringPool.NULL;
		
		String fieldtype = e.attributeValue("fieldtype");
		String htmlname = StringEscapeUtils.escapeSql( e.attributeValue("htmlname") );
		String needconfirm = e.attributeValue("needconfirm");
		String repeatable = e.attributeValue("repeatable");
		String required = e.attributeValue("required");
		
		checkOTPFields( Long.valueOf(groupid), fieldtype);
		
		String lblBefore = StringEscapeUtils.escapeSql( e.selectSingleNode("labelbefore").asXML() );
		String lblAfter = StringEscapeUtils.escapeSql( e.selectSingleNode("labelafter").asXML() );

		Node defValNode =  e.selectSingleNode("defaultvalue");
		if (defValNode == null)
			defValNode = SAXReaderUtil.read("<defaultvalue><value name=''/></defaultvalue>").getRootElement().detach();

		Node inputCtrlNode = e.selectSingleNode("inputctrl");
		String inputCtrlType = XMLHelper.getTextValueOf(inputCtrlNode, "@type");
		
		if(  ArrayUtil.contains(inputCtrlTypes, inputCtrlType) )
			CheckDefaultOptions( inputCtrlNode, defValNode );
		
		String defVal = StringEscapeUtils.escapeSql( defValNode.asXML() );
		String inputCtrl = StringEscapeUtils.escapeSql( inputCtrlNode.asXML() );
		
		String validator = StringEscapeUtils.escapeSql( e.selectSingleNode("validator").asXML() );
		Node tooltipNode = e.selectSingleNode("tooltip");
		String tooltip = StringPool.NULL;
		if( Validator.isNotNull(tooltipNode) )
			tooltip = StringEscapeUtils.escapeSql(tooltipNode.getText());
		
		if( Validator.isNotNull(htmlname) )
		{
			query = String.format(CHEK_NEW_HTML_NAME, htmlname, formid);
			List<Object> existsHtmlName = PortalLocalServiceUtil.executeQueryAsList( query );
			ErrorRaiser.throwIfFalse((existsHtmlName != null && existsHtmlName.size() == 1 && existsHtmlName.get(0) != null && existsHtmlName.get(0).toString().equals("0") ), IterErrorKeys.XYZ_ITR_E_DUPLICATE_FORM_HTMLNAME_ZYX);
			htmlname = StringUtil.apostrophe(htmlname);
		}
		else
			htmlname = StringPool.NULL;
		
		int fieldPos=0;
		String strPos = e.attributeValue("fieldorder");
		if( Validator.isNotNull(strPos) )
			fieldPos = Integer.parseInt( strPos );
		
		query = String.format("SELECT ITR_ADD_FIELD(%s, '%s', %s, '%s', %s, '%s','%s','%s','%s','%s','%s', %s, %s, %s, %s, %s, %s )", 
				fieldId, tabid, datafieldid, fieldtype, htmlname, inputCtrl, validator, lblBefore, 
				lblAfter, tooltip, defVal, editable, required, needconfirm, repeatable, css, fieldPos);

		List<Object> uuidResult = PortalLocalServiceUtil.executeQueryAsList( query );
		ErrorRaiser.throwIfFalse((uuidResult != null && uuidResult.size() == 1 && uuidResult.get(0) != null), IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
		fieldId = uuidResult.get(0).toString();
		
		// Añadir a la tabla fieldreceived el nuevo campo con valor NULL para que las consultas tarden 3 segundos menos.
		if (!isRegistryForm)
		{
			query = String.format(INSERT_INTO_FIELD_RECEIVED, fieldId, formid);
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
		return getTabField( fieldId );
	}
	
	private void CheckDefaultOptions( Node inputCtrl, Node defaultValue ) throws ServiceError
	{
		XPath xpath = SAXReaderUtil.createXPath("options");
		Node options = xpath.selectSingleNode(inputCtrl);
		boolean multiple = XMLHelper.getTextValueOf(options, "@multiple").equalsIgnoreCase("true");
		
		xpath = SAXReaderUtil.createXPath("option/@name");
		List<Node> optNodes = xpath.selectNodes(options);
		ErrorRaiser.throwIfFalse(optNodes != null && optNodes.size()>0, IterErrorKeys.XYZ_ITR_E_EMPTY_INPUTCTRL_ZYX);
		
		xpath = SAXReaderUtil.createXPath("value");
		List<Node> defValNodes = xpath.selectNodes(defaultValue);
		ErrorRaiser.throwIfFalse( !(!multiple && defValNodes.size()>1), IterErrorKeys.XYZ_ITR_E_MULTIPLE_DEFAULT_VALUES_INPUTCTRL_ZYX );
		
		ArrayList<String> listOptions = new ArrayList<String>();
		for(Node opt : optNodes)
		{
			listOptions.add( opt.getStringValue() );
		}
		
		for( Node defVal : defValNodes )
		{
			if( !listOptions.contains( XMLHelper.getTextValueOf(defVal, "@name")) )
				defVal.detach();
		}
	}
	
	private String getTabField( String fieldId ) throws ServiceError, SecurityException, NoSuchMethodException
	{
		String result = "";
		
		ErrorRaiser.throwIfNull(fieldId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document d = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_FIELD_BY_ID, fieldId), new String[]{"labelbefore", "labelafter"}  );
		
		String xslpath = new StringBuilder("").append(File.separatorChar).append("base-portlet")
									.append(File.separatorChar).append("xsl")
									.append(File.separatorChar).append("getTabFields.xsl").toString();

		result = XSLUtil.transformXML(d.asXML(), xslpath );
		
		return result;
	}
	
	/**
	 * Si es un formulario de registro, y el datafield corresponde a un UserProfile de tipo "system", se actualiza el valor
	 * del datafield con el valor de dicho datafield en el sistema destino.
	 * 
	 * @param field
	 * @param isRegistryForm
	 * @param defaultValue
	 * @return
	 * @throws ServiceError
	 */
	private String updateSystemProfileField(Element field, String defaultValue, long delegationId) throws ServiceError
	{
		String profilefieldid = defaultValue;
		
		// Si es un formulario de registro los UserProfile se actualizarán con el datafield del sistema destino
		String fieldname = field.attributeValue("fieldname");
		boolean structured = "system".equals(XMLHelper.getStringValueOf(field, "@fieldclass"));
		if ( Validator.isNotNull(fieldname) )
		{
			// Se obtiene el profilefieldid en el sistema destino
			String structuredclausule = structured ? "structured" : String.format("structured=FALSE AND userprofile.delegationid = %d", delegationId);
			List<Object> newProfileField = PortalLocalServiceUtil.executeQueryAsList( String.format(GET_SYSTEM_PROFILEFIELDID, fieldname, structuredclausule) );
			ErrorRaiser.throwIfFalse(newProfileField != null && newProfileField.size() == 1 && newProfileField.get(0) != null, IterErrorKeys.XYZ_ITR_FK_FORMFIELD_DATAFIELDID_ZYX);
			
			profilefieldid = String.valueOf(newProfileField.get(0));
		}

		return profilefieldid;
	}
	
	public String updateField( String xmlData ) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, IOException, SQLException
	{
		return updateField( SAXReaderUtil.read(xmlData).getRootElement() );
	}
	public String updateField( Element e ) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, IOException, SQLException
	{
		String formid = e.attributeValue("formid");
		ErrorRaiser.throwIfNull(formid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		String fieldId = e.attributeValue("fieldid");
		ErrorRaiser.throwIfNull(fieldId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		String dataFieldId = e.attributeValue("datafieldid");
		ErrorRaiser.throwIfNull(dataFieldId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String query = String.format(CHECK_REGISTER_EDIT_FIELD, dataFieldId, fieldId, formid);
		List<Object> existsField = PortalLocalServiceUtil.executeQueryAsList( query );
		ErrorRaiser.throwIfFalse((existsField != null && existsField.size() == 1 && existsField.get(0) != null && existsField.get(0).toString().equals("0") ), IterErrorKeys.XYZ_ITR_E_DUPLICATE_FIELD_ZYX);
		
		String editable = e.attributeValue("editable");
		String css = e.attributeValue("css");
		if( Validator.isNotNull(css) )
			css = StringUtil.apostrophe( StringEscapeUtils.escapeSql(css) );
		else
			css = StringPool.NULL;
		
		String fieldtype = e.attributeValue("fieldtype");
		String htmlname = StringEscapeUtils.escapeSql( e.attributeValue("htmlname") );
		String needconfirm = e.attributeValue("needconfirm");
		String repeatable = e.attributeValue("repeatable");
		String required = e.attributeValue("required");
		
		String lblBefore = StringEscapeUtils.escapeSql( e.selectSingleNode("labelbefore").asXML() );
		String lblAfter = StringEscapeUtils.escapeSql( e.selectSingleNode("labelafter").asXML() );
		
		Node defValNode = e.selectSingleNode("defaultvalue");

		Node inputCtrlNode = e.selectSingleNode("inputctrl");
		String inputCtrlType = XMLHelper.getTextValueOf(inputCtrlNode, "@type");
		
		if(  ArrayUtil.contains(inputCtrlTypes, inputCtrlType) )
			CheckDefaultOptions( inputCtrlNode, defValNode );
		
		String defVal = StringEscapeUtils.escapeSql( defValNode.asXML() );
		String inputCtrl = StringEscapeUtils.escapeSql( inputCtrlNode.asXML() );

		
		String validator = StringEscapeUtils.escapeSql( e.selectSingleNode("validator").asXML() );
		Node tooltipNode = e.selectSingleNode("tooltip");
		String tooltip = StringPool.NULL;
		if( Validator.isNotNull(tooltipNode) )
			tooltip = StringEscapeUtils.escapeSql(tooltipNode.getText());
		
		if( Validator.isNotNull(htmlname) )
		{
			query = String.format(CHEK_EDIT_HTML_NAME, htmlname, formid, fieldId);
			List<Object> existsHtmlName = PortalLocalServiceUtil.executeQueryAsList( query );
			ErrorRaiser.throwIfFalse((existsHtmlName != null && existsHtmlName.size() == 1 && existsHtmlName.get(0) != null && existsHtmlName.get(0).toString().equals("0") ), IterErrorKeys.XYZ_ITR_E_DUPLICATE_FORM_HTMLNAME_ZYX);
			htmlname = StringUtil.apostrophe(htmlname);
		}
		else
			htmlname = StringPool.NULL;
		
		int fieldPos=0;
		String strPos = e.attributeValue("fieldorder");
		if( Validator.isNotNull(strPos) )
			fieldPos = Integer.parseInt( strPos );
		
		query = String.format("CALL ITR_UPDT_FIELD('%s', '%s', '%s', %s, '%s','%s','%s','%s','%s','%s', %s, %s, %s, %s, %s, %s )", 
								fieldId, dataFieldId, fieldtype, htmlname, inputCtrl, validator, lblBefore, lblAfter, 
								tooltip, defVal, editable, required, needconfirm, repeatable, css, fieldPos);
		PortalLocalServiceUtil.executeUpdateQuery( query );

		return getTabField( fieldId );
	}
	
	public String deleteFields( String xmlData ) throws DocumentException, ServiceError, IOException, SQLException, NoSuchMethodException
	{
		Document d = SAXReaderUtil.read(xmlData);
		Element dataRoot = d.getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row/@fieldid");
		
		List<Node> nodes = xpath.selectNodes(dataRoot);
		ErrorRaiser.throwIfFalse(nodes != null && nodes.size() > 0);
		
		String inClauseSQL = TeaserMgr.getInClauseSQL(nodes);
		
		List<Object> systemFields = PortalLocalServiceUtil.executeQueryAsList( String.format(CHECK_FIELDS, inClauseSQL) );
		ErrorRaiser.throwIfFalse((systemFields != null && systemFields.size() == 1 && systemFields.get(0) != null && systemFields.get(0).toString().equals("0") ), IterErrorKeys.XYZ_ITR_E_SYSTEM_REQUIRED_FIELDS_ZYX);

		String query = String.format("SELECT ITR_DELETE_FIELDS('%s')", d.asXML());
		List<Object> tabIdResult = PortalLocalServiceUtil.executeQueryAsList( query );
		ErrorRaiser.throwIfFalse((tabIdResult != null && tabIdResult.size() == 1 && tabIdResult.get(0) != null), IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);

		PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_FIELDS, inClauseSQL));
		
		return IterTabsLocalServiceUtil.getTabFields( tabIdResult.get(0).toString() );
	}
	
	public String updateFieldOrder( String xmlData ) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		String retVal = "<rs/>";
		
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row");
		Node node = xpath.selectSingleNode(dataRoot);
		
		String idtab = XMLHelper.getTextValueOf(node, "@tabid");
		
		//Elemento que vamos a mover
		String fieldId = XMLHelper.getTextValueOf(node, "@fieldid");
		ErrorRaiser.throwIfNull(fieldId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String sql = String.format("SELECT fieldid, fieldorder FROM formfield WHERE fieldid = '%s'", fieldId);
 		List<Object> item = PortalLocalServiceUtil.executeQueryAsList(sql);
 		ErrorRaiser.throwIfFalse(item.size() > 0, IterErrorKeys.XYZ_E_SOURCE_NOT_FOUND_ZYX);
 		long currentPriority = Long.parseLong( ((Object[])item.get(0))[1].toString() );
		String currentField = ((Object[])item.get(0))[0].toString();
 		
		//Elemento de referencia. El elemento a mover quedará encima de este.
		String refid = XMLHelper.getTextValueOf(node, "@refid");
		long refPriority = 0;
		
		if( Validator.isNotNull(refid) && !refid.isEmpty() )
		{
			sql = String.format("SELECT fieldorder FROM formfield WHERE fieldid = '%s'", refid);
			item = PortalLocalServiceUtil.executeQueryAsList(sql);
	 		ErrorRaiser.throwIfFalse(item.size() > 0, IterErrorKeys.XYZ_E_TARGET_NOT_FOUND_ZYX);
	 		refPriority = Long.parseLong( item.get(0).toString() );
		}
		else
		{
			sql = String.format("SELECT IFNULL(MAX(fieldorder),0) FROM formfield WHERE tabid='%s'", idtab);
			item = PortalLocalServiceUtil.executeQueryAsList(sql);
	 		ErrorRaiser.throwIfFalse(item.size() > 0, IterErrorKeys.XYZ_E_TARGET_NOT_FOUND_ZYX);
	 		refPriority = Long.parseLong( item.get(0).toString() )+1;
		}
 		
 		long ini = 0;
 		long fin = 0;
 		String oper = "";
 		String updtItemIdx = "";
 		String getReorderedItems = "";
// 		String modifiedDate = SQLQueries.getCurrentDate();
 		
 		if( refPriority!=currentPriority )
 		{
 			if( refPriority > currentPriority )
 	 		{
 	 			ini = currentPriority+1;
 	 			fin = refPriority-1;
 	 			oper="-";
 	 			updtItemIdx = String.format("UPDATE formfield SET fieldorder=%d WHERE fieldid='%s'", fin, currentField);
 	 			getReorderedItems = String.format(GET_UPDATED_FIELDS, ini-1, fin, idtab);
 	 		}
 	 		else if ( refPriority < currentPriority )
 	 		{
 	 			ini = refPriority;
 	 			fin = currentPriority-1;
 	 			oper="+";
 	 			updtItemIdx = String.format("UPDATE formfield SET fieldorder=%d WHERE fieldid='%s'", ini, currentField);
 	 			getReorderedItems = String.format(GET_UPDATED_FIELDS, ini, fin+1, idtab);
 	 		}
 	 		
 	 		if( ini <= fin)
 	 		{
	 	 		PortalLocalServiceUtil.executeUpdateQuery( String.format(UPDATE_FIELDS_PRIORITY, oper, idtab, currentField, ini, fin) );
	 	 		
	 	 		PortalLocalServiceUtil.executeUpdateQuery( updtItemIdx );
	 	 		
	 	 		Document result = PortalLocalServiceUtil.executeQueryAsDom( getReorderedItems );
	 	 		retVal = result.asXML();
 	 		}
 		}
 		
 		return retVal;
	}

	public void checkRegisterForm( String formId ) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(formId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document d = PortalLocalServiceUtil.executeQueryAsDom( String.format(CHECK_REGISTER_FORM, formId) );

		XPath xpath = SAXReaderUtil.createXPath("//row/@fieldname");
		
		List<Node> nodes = xpath.selectNodes(d.getRootElement());
		ErrorRaiser.throwIfFalse( (nodes != null && nodes.size()==0), IterErrorKeys.XYZ_ITR_E_INVALID_FORM_ZYX, getFieldsName(nodes));
	}
	
	private String getFieldsName( List<Node> fieldsname )
	{
		StringBuilder retVal = new StringBuilder("\n");
		retVal.append(IterErrorKeys.XYZ_ITR_E_REQUIRED_FIELDS_ZYX).append(":");
		
		for( Node n : fieldsname )
			retVal.append("\n\t").append( n.getStringValue() );
		
		return retVal.toString();
	}
}
