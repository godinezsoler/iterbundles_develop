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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
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
import com.liferay.portal.kernel.plugin.Version;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.IterGlobalKeys;
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
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.util.xml.CDATAUtil;
import com.protecmedia.iter.base.service.IterFieldLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.IterTabsLocalServiceUtil;
import com.protecmedia.iter.base.service.base.IterFormLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.IterAdmin;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.TeaserMgr;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

/**
 * The implementation of the iter form local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.IterFormLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.IterFormLocalServiceUtil} to access the iter form local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.IterFormLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.IterFormLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class IterFormLocalServiceImpl extends IterFormLocalServiceBaseImpl
{
	private static ReadWriteLock exportGlobalLock = new ReentrantReadWriteLock();
	private static Lock exportWriteLock = exportGlobalLock.writeLock();
	
	private static ReadWriteLock importGlobalLock = new ReentrantReadWriteLock();
	private static Lock importWriteLock = importGlobalLock.writeLock();
	
	private final String GET_PRODUCT_FROM_NAME = new StringBuilder("\n"	).append(
			"SELECT productId, name productname \n"				).append(
			"FROM product \n"									).append(
			" 	WHERE name IN ('%s')").toString();				

	private final String GET_PAGE_FROM_FRIENDLYURL = "SELECT uuid_, friendlyURL layoutRef FROM Layout WHERE privateLayout = FALSE AND groupId = %d AND friendlyURL IN ('%s')";
	private final String GET_PAGE_FROM_GLOBALID = new StringBuilder("\n"																						).append(
					"SELECT uuid_, globalid layoutRef\n"																										).append(
					"FROM xmlio_live \n"																														).append(
					"INNER JOIN layout ON (xmlio_live.localid = layout.plid AND xmlio_live.classnamevalue = '").append(IterKeys.CLASSNAME_LAYOUT).append("') \n").append(
					" 	WHERE globalid IN ('%s')").toString();

	private final String GET_PAGE_FROM_UUID = new StringBuilder("\n"																						).append(
			"SELECT uuid_, globalid layoutRef\n"																														).append(
			"FROM xmlio_live \n"																															).append(
			"INNER JOIN layout ON (xmlio_live.localid = layout.plid AND xmlio_live.classnamevalue = '").append(IterKeys.CLASSNAME_LAYOUT).append("') \n"	).append(
			" 	WHERE uuid_ IN ('%s')").toString();


	private final String GET_LAYOUT_FROM_UUID = "SELECT uuid_, friendlyURL layoutRef FROM Layout WHERE uuid_ IN ('%s')";
	
	private final String GET_FORMS = "SELECT formid, name, formtype, navigationtype, description FROM form WHERE groupid=%s";
	
	private final String GET_FORMS_NAME = new StringBuilder(	"SELECT f.formid, f.name FROM form f "						).append(
																	"INNER JOIN formtab ft ON ft.formid=f.formid "			).append(
																	"INNER JOIN formfield ff ON ff.tabid=ft.tabid "			).append(
																	"WHERE f.groupid=%s AND formtype <> 'registro' "		).append(
																	"GROUP BY f.formid"										).toString();
	
	private final String GET_FORM_BY_FORMID = "SELECT formid, name, formtype, navigationtype, description FROM form WHERE formid='%s'";
	
	
	
	private final String GET_FORM = new StringBuilder(" SELECT group_.name groupName, f.formid,	f.name, f.description, f.formtype, f.navigationtype, f.css, f.usechaptcha, f.successsend AS ok, f.errorsend AS ko, f.submitlabel, f.usesubmit, f.resetlabel, f.restricted, f.oklabel, f.cncllabel, f.confirmpaneltitle, ")
									.append(" %1$s AS dbhandlerid, db.enabled AS dbhandlerenabled, db.critic AS dbhandlercritic, ")
									.append(" %2$s AS filehandlerid, file.enabled AS filehandlerenabled, file.critic AS filehandlercritic, file.processdata AS fileprocessdata, file.transformid AS filetransformid, file.foldername AS filefoldername, file.formname AS fileformname, ft.transformname AS filetransformname, ")
									.append(" %3$s AS servlethandlerid, servlet.enabled AS servlethandlerenabled, servlet.critic AS servlethandlercritic, servlet.processdata AS servletprocessdata, servlet.transformid AS servlettransformid, servlet.url AS servleturl, ft2.transformname AS servlettransformname, ")
									.append(" %4$s AS emailhandlerid, email.enabled AS emailhandlerenabled, email.critic AS emailhandlercritic, email.processdata AS emailprocessdata, email.transformid AS emailtransformid, email.type AS emailtype, email.para AS emailpara, email.cc AS emailcc, email.cco AS emailcco, email.smtpserver AS emailsmtpserver, email.textsubject AS emailtextsubject, email.textbody AS emailtextbody, ft3.transformname AS emailtransformname, smtp.HOST AS smtpservername ")
									.append(" FROM form f ")
									.append(" INNER JOIN group_ ON f.groupid = group_.groupid \n")
									.append(" LEFT JOIN formhandlerdb db ON (db.formid=f.formid)  ")
									.append(" LEFT JOIN formhandlerfile file ON (file.formid=f.formid) LEFT JOIN formxsltransform ft ON (file.transformid=ft.transformid)  ")
									.append(" LEFT JOIN formhandlerservlet servlet ON (servlet.formid=f.formid) LEFT JOIN formxsltransform ft2 ON (servlet.transformid=ft2.transformid)  ")
									.append(" LEFT JOIN formhandleremail email ON (email.formid=f.formid) LEFT JOIN formxsltransform ft3 ON (email.transformid=ft3.transformid) LEFT JOIN smtpserver smtp ON (email.smtpserver=smtp.smtpserverid) ")
									.append(" WHERE f.formid='%5$s' ").toString();
	
	private final String GET_FORM_TYPE_REG_BY_GRPID = "SELECT formid FROM form WHERE groupid=%s AND formtype='registro'";
	private final String GET_FORM_TYPE_REG_BY_FORMID = "SELECT formid FROM form WHERE formtype='registro' AND groupid = (SELECT groupid FROM form WHERE formid='%s') AND formid<>'%s'";
	
	private final String GET_FORM_TYPE = "SELECT CAST(formtype AS CHAR CHARACTER SET utf8) formtype FROM form WHERE formid='%s'";
	private final String GET_FORM_NUM_FIELDS = "SELECT COUNT(fieldid) FROM formfield WHERE tabid IN (SELECT tabid FROM formtab WHERE formid='%s')";
	private final String GET_FORM_NAVIGATION = "SELECT navigationtype FROM form WHERE formid='%s'";
	private final String GET_FORM_NUM_TABS = "SELECT COUNT(tabid) FROM formtab WHERE formid='%s'";
	
	private final String DELETE_FORM = "DELETE FROM form WHERE formid IN %s";
	
	private final String GET_FORM_PRODUCTS_BY_FORM = new StringBuilder("\n").append(
		"SELECT NAME id													\n").append(
		"FROM formproduct 												\n").append(
		"INNER JOIN product ON product.productid = formproduct.productid\n").append(	
		"	WHERE formproduct.formid = '%s'								\n").toString();
				
	private final String GET_FORM_PRODUCTS= new StringBuilder(" SELECT p.productid, p.name, IF(fp.formproductid IS NULL, FALSE, TRUE) AS selected ")
											.append( " FROM formproduct fp RIGHT JOIN product p ON (fp.productid=p.productid AND fp.formid='%s') ORDER BY p.name " ).toString();
	
	private final String DELETE_FORM_DATAFIELDS = "DELETE FROM datafield WHERE fieldclass<>'system' AND datafieldid IN ( SELECT ff.datafieldid FROM formfield ff LEFT JOIN formtab ft ON (ff.tabid=ft.tabid) WHERE ft.formid='%s' ) AND datafieldid NOT IN (SELECT datafieldid FROM userprofile)";
	private final String DELETE_FORM_FIELDS = "DELETE FROM formfield WHERE tabid IN ( SELECT tabid FROM formtab WHERE formid='%s' )";
	
	private final String EXISTS_CAPTCHA = "SELECT COUNT(theme) FROM captcha WHERE groupid=%s";
	
	private final String ADD_FORM = "ADD";
	private final String EDIT_FORM = "EDIT";
	private final String FORM_TYPE_REG= "registro";
	private final String FORM_TYPE_GEN= "generico";
	private final String FORM_NAVTYPE_PAGE = "page";
	
	private final String GET_FORM_DEFINITION = new StringBuffer()
		.append("SELECT\n")
		.append("ft.tabid,\n")
		.append("ft.name 'tabname',\n")
		.append("ff.fieldid,\n")
		.append("if\n")
		.append("(\n")
		.append("  (isnull(extractValue(ff.labelbefore, '/labelbefore/textlabel/text()')) or length(extractValue(ff.labelbefore, '/labelbefore/textlabel/text()'))=0)\n")
		.append("  ,\n")
		.append("	if\n")
		.append("  (\n")
		.append("    (isnull(extractValue(ff.tooltip, '/text()')) or length(extractValue(ff.tooltip, '/text()'))=0)\n")
		.append("    ,\n")
		.append("		if\n")
		.append("    (\n")
		.append("      f.formtype = 'registro'\n")
		.append("      ,\n")
		.append("      df.fieldtype\n")
		.append("      ,\n")
		.append("      up.fieldname\n")
		.append("    )\n")
		.append("		,\n")
		.append("		extractValue(ff.tooltip, '/text()')\n")
		.append("	)\n")
		.append("	,\n")
		.append("	extractValue(ff.labelbefore, '/labelbefore/textlabel/text()')\n")
		.append(")\n")
		.append("'name',\n")
		.append("df.fieldtype 'fieldtype',\n")
		.append("extractvalue(ff.inputctrl, '/inputctrl/@type') inputctrltype,\n")
		.append("ff.editable,\n")
		.append("ff.required,\n")
		.append("ff.needconfirm,\n")
		.append("ff.repeatable 'repeatable',\n")
		.append("ff.labelafter,\n")
		.append("ff.inputctrl,\n")
		.append("ff.validator,\n")
		.append("ff.defaultvalue,\n")
		.append("ff.htmlname\n")
		.append("FROM form f\n")
		.append("INNER JOIN formtab ft ON ft.formid = f.formid\n")
		.append("INNER JOIN formfield ff ON ff.tabid = ft.tabid\n")
		.append("INNER JOIN datafield df ON ff.datafieldid = df.datafieldid\n")
		.append("left outer JOIN userprofile up ON up.datafieldid = df.datafieldid -- OJITO!\n")
		.append("WHERE f.formid = '%s'\n")
		.append("ORDER BY ft.tabid, ff.fieldorder;\n").toString();
//	.append("-- CDATAS: defaultvalue", "inputctrl, laberafter, validator\n");
	
	private Log _log = LogFactoryUtil.getLog(IterFormLocalServiceImpl.class);
	
	public String getForms( long groupId ) throws ServiceError, NoSuchMethodException
	{
		String result = "";
		
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document d = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_FORMS, groupId), new String[]{"description", "navigationtype"} );
		
		String xslpath = new StringBuilder("").append(File.separatorChar).append("base-portlet")
											.append(File.separatorChar).append("xsl")
											.append(File.separatorChar).append("getforms.xsl").toString();
		
		result = XSLUtil.transformXML(d.asXML(), xslpath );
		
		return result;

	}
	
	public String getForm( String formid ) throws ServiceError, NoSuchMethodException
	{
		return getForm(formid, false);
	}
	
	private String getForm( String formid, boolean publish ) throws ServiceError, NoSuchMethodException
	{
		String result = "";
		
		ErrorRaiser.throwIfNull(formid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String[] colsAsNodes = new String[]{"description", "navigationtype", "ok", "ko", "servleturl", "emailpara", "emailcc", "emailcco", "emailtextsubject", "emailtextbody" };
		
		Document d = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_FORM,	(publish ? "db.handlerid" : "''"),
																						(publish ? "file.handlerid" : "''"),
																						(publish ? "servlet.handlerid" : "''"),
																						(publish ? "email.handlerid" : "''"),
																						formid), colsAsNodes );
		
		String xslpath = new StringBuilder("").append(File.separatorChar).append("base-portlet")
											.append(File.separatorChar).append("xsl")
											.append(File.separatorChar).append("getForm.xsl").toString();
		
		result = XSLUtil.transformXML(d.asXML(), xslpath );

		return result;
	}
	
	public String addForm( String xmlData ) throws DocumentException, ServiceError, SecurityException, NoSuchMethodException, IOException, SQLException
	{
		return addForm( SAXReaderUtil.read(xmlData).getRootElement() );
	}
	private String addForm( Element d ) throws DocumentException, ServiceError, SecurityException, NoSuchMethodException, IOException, SQLException
	{
		List<Object> uuidResult = null;
		checkFormData( d, ADD_FORM );
		
		String navType = StringEscapeUtils.escapeSql( d.selectSingleNode("navigationtype").asXML() );
		String ok = StringEscapeUtils.escapeSql( d.selectSingleNode("ok").asXML() );
		String ko = StringEscapeUtils.escapeSql( d.selectSingleNode("ko").asXML() );
		String formXML = StringEscapeUtils.escapeSql( d.asXML().replaceAll("\\\\", "\\\\\\\\") );
		
		String query = String.format("SELECT ITR_ADD_FORM('%s', '%s', '%s', '%s' )", formXML, navType, ok, ko);
		
		uuidResult = PortalLocalServiceUtil.executeQueryAsList( query );
		ErrorRaiser.throwIfFalse((uuidResult != null && uuidResult.size() == 1 && uuidResult.get(0) != null), IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
		
		return getFormById( uuidResult.get(0).toString() );
	}
	
	public String editForm( String xmlData ) throws DocumentException, ServiceError, SecurityException, NoSuchMethodException, IOException, SQLException
	{
		return editForm( SAXReaderUtil.read(xmlData).getRootElement() );
	}
	private String editForm( Element d ) throws DocumentException, ServiceError, SecurityException, NoSuchMethodException, IOException, SQLException
	{
		List<Object> uuidResult = null;
		
		checkFormData( d, EDIT_FORM );
		
		String navType = StringEscapeUtils.escapeSql( d.selectSingleNode("navigationtype").asXML() );
		String ok = StringEscapeUtils.escapeSql( d.selectSingleNode("ok").asXML() );
		String ko = StringEscapeUtils.escapeSql( d.selectSingleNode("ko").asXML() );
		String formXML = StringEscapeUtils.escapeSql( d.asXML().replaceAll("\\\\", "\\\\\\\\") );
		
		String query = String.format("SELECT ITR_EDIT_FORM('%s', '%s', '%s', '%s' )", formXML, navType, ok, ko);
		
		uuidResult = PortalLocalServiceUtil.executeQueryAsList( query );
		ErrorRaiser.throwIfFalse((uuidResult != null && uuidResult.size() == 1 && uuidResult.get(0) != null), IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
		
		return getFormById( uuidResult.get(0).toString() );
	}
	
	public String deleteForms( String xmlData ) throws DocumentException, ServiceError, IOException, SQLException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row/@formid");
		
		List<Node> nodes = xpath.selectNodes(dataRoot);
		ErrorRaiser.throwIfFalse(nodes != null && nodes.size() > 0);
		
		String inClauseSQL = TeaserMgr.getInClauseSQL(nodes);

		PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_FORM, inClauseSQL));

		return xmlData;
	}
	
	private Document getFormProducts2(String formId) throws SecurityException, NoSuchMethodException
	{
		return PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_FORM_PRODUCTS_BY_FORM, formId), true, "products", "product");
	}
	public String getFormProducts( String formId ) throws ServiceError, SecurityException, NoSuchMethodException
	{
		String result = "";
		
		ErrorRaiser.throwIfNull(formId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		result = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_FORM_PRODUCTS, formId) ).asXML();
		
		return result;
	}
	
	private void checkFormData( Element dataRoot, String operationType ) throws DocumentException, ServiceError, IOException, SQLException
	{
		List<Object> listObj = null;
//		XPath xpath = null;
		
		String formId = null;
		String grpId = null;
		
		String formType = dataRoot.attributeValue("formtype");
		ErrorRaiser.throwIfNull( formType, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		if(operationType.equalsIgnoreCase(ADD_FORM))
		{
			grpId = dataRoot.attributeValue("groupid");
			ErrorRaiser.throwIfNull( grpId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			if( formType.equalsIgnoreCase(FORM_TYPE_REG) )
			{
				listObj = PortalLocalServiceUtil.executeQueryAsList( String.format(GET_FORM_TYPE_REG_BY_GRPID, grpId) );
				ErrorRaiser.throwIfFalse( !(listObj != null && listObj.size() == 1 && listObj.get(0) != null), IterErrorKeys.XYZ_ITR_E_REGISTER_FORM_ALREADY_EXISTS_ZYX);
			}
		}

		if(operationType.equalsIgnoreCase(EDIT_FORM))
		{
			formId = dataRoot.attributeValue("formid");
			ErrorRaiser.throwIfNull( formId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			if( formType.equalsIgnoreCase(FORM_TYPE_REG) )
			{
				listObj = PortalLocalServiceUtil.executeQueryAsList( String.format(GET_FORM_TYPE_REG_BY_FORMID, formId, formId) );
				ErrorRaiser.throwIfFalse( !(listObj != null && listObj.size() == 1 && listObj.get(0) != null), IterErrorKeys.XYZ_ITR_E_REGISTER_FORM_ALREADY_EXISTS_ZYX);
			}
			
			String confirmChange = dataRoot.attributeValue("allowchangetype");
			if( Validator.isNull(confirmChange) || confirmChange.equalsIgnoreCase("false") )
			{
				listObj = PortalLocalServiceUtil.executeQueryAsList( String.format(GET_FORM_TYPE, formId) );
				ErrorRaiser.throwIfFalse((listObj != null && listObj.size() == 1 && listObj.get(0) != null), IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
				
				if( !formType.equalsIgnoreCase( listObj.get(0).toString() ) )
				{
					listObj = PortalLocalServiceUtil.executeQueryAsList( String.format(GET_FORM_NUM_FIELDS, formId) );
					ErrorRaiser.throwIfFalse( (listObj != null && listObj.size() == 1 && listObj.get(0) != null && listObj.get(0).toString().equals("0")), IterErrorKeys.XYZ_ITR_W_CHANGE_FORMTYPE_ZYX);
				}
			}
			else if( Validator.isNotNull(confirmChange) && confirmChange.equalsIgnoreCase("true") )
			{
				deleteFormFields(formId);
			}
			
			listObj = PortalLocalServiceUtil.executeQueryAsList( String.format(GET_FORM_NAVIGATION, formId) );
			ErrorRaiser.throwIfFalse((listObj != null && listObj.size() == 1 && listObj.get(0) != null), IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
			Element navigation = SAXReaderUtil.read(listObj.get(0).toString()).getRootElement();
			String navType = navigation.attributeValue("type");
			if( Validator.isNotNull(navType) && !navType.equalsIgnoreCase(FORM_NAVTYPE_PAGE) )
			{
				Node navTypeNode = dataRoot.selectSingleNode("navigationtype");
				String typeNav = XMLHelper.getTextValueOf(navTypeNode, "@type");
				if( Validator.isNotNull(typeNav) && typeNav.equalsIgnoreCase(FORM_NAVTYPE_PAGE) )
				{
					listObj = PortalLocalServiceUtil.executeQueryAsList( String.format(GET_FORM_NUM_TABS, formId) );
					ErrorRaiser.throwIfFalse( (listObj != null && listObj.size() == 1 && listObj.get(0) != null && Integer.parseInt(listObj.get(0).toString())<=1), IterErrorKeys.XYZ_ITR_W_CHANGE_NAVIGATION_TYPE_ZYX);
				}
			}
		}

		ErrorRaiser.throwIfNull( dataRoot.attributeValue("submitlabel"), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String useCaptcha = dataRoot.attributeValue("usechaptcha");
		ErrorRaiser.throwIfNull( useCaptcha, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		if( useCaptcha.equalsIgnoreCase("true") )
		{
			String idGrp = "";
			if( Validator.isNotNull(grpId) && !grpId.isEmpty() )
				idGrp = grpId;
			else
				idGrp = String.format("(SELECT groupid FROM form WHERE formid='%s')", formId);
			
			String query = String.format(EXISTS_CAPTCHA, idGrp);
			listObj = PortalLocalServiceUtil.executeQueryAsList( query );
			ErrorRaiser.throwIfFalse( (listObj != null && listObj.size() == 1 && listObj.get(0) != null && !listObj.get(0).toString().equals("0")), IterErrorKeys.XYZ_ITR_E_CAPTCHA_UNDEFINED_ZYX);
		}
		
		ErrorRaiser.throwIfNull( dataRoot.attributeValue("name"), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		Node navType = dataRoot.selectSingleNode("navigationtype[@type='bypage']");
		if( Validator.isNotNull(navType) )
		{
			Node textLblNode = dataRoot.selectSingleNode("navigationtype[@type='bypage']");
			String attrVal = XMLHelper.getTextValueOf(textLblNode, "@nextlabel");
			ErrorRaiser.throwIfNull( attrVal, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

			attrVal = XMLHelper.getTextValueOf(textLblNode, "@prevlabel");
			ErrorRaiser.throwIfNull( attrVal, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}

//		xpath = SAXReaderUtil.createXPath("/form/ok/msg");
//		Node success = xpath.selectSingleNode(dataRoot);
//		ErrorRaiser.throwIfNull( success, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
//		ErrorRaiser.throwIfFalse( !success.getText().isEmpty(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		Node error = dataRoot.selectSingleNode("ko/invalidfieldmsg");
		ErrorRaiser.throwIfNull( error, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse( !error.getText().trim().isEmpty(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		if( useCaptcha.equalsIgnoreCase("true") )
		{
			error = dataRoot.selectSingleNode("ko/captchafailmsg");
			ErrorRaiser.throwIfNull( error, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			ErrorRaiser.throwIfFalse( !error.getText().trim().isEmpty(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}
		
		if(Validator.isNotNull(formType) && formType.equalsIgnoreCase(FORM_TYPE_GEN))
		{
			checkHandlers( dataRoot );
		}

	}

	private void checkHandlers( Element dataRoot ) throws ServiceError
	{
		boolean handlerEnabledCritic = false;
		
		Node dbHandlerNode = dataRoot.selectSingleNode("handlers/dbhandler");

		String dbHandlerEnabled = XMLHelper.getTextValueOf(dbHandlerNode, "@enabled");
		String dbHandlerCritic = XMLHelper.getTextValueOf(dbHandlerNode, "@critic");

		if( Validator.isNotNull(dbHandlerEnabled) && dbHandlerEnabled.equalsIgnoreCase("true") && 
				Validator.isNotNull(dbHandlerCritic) && dbHandlerCritic.equalsIgnoreCase("true")
		)
		{
			handlerEnabledCritic = true;
		}

		Node fileHandlerNode = dataRoot.selectSingleNode("handlers/filehandler");

		String fileHandlerEnabled = XMLHelper.getTextValueOf(fileHandlerNode, "@enabled");
		String fileHandlerCritic = XMLHelper.getTextValueOf(fileHandlerNode, "@critic");

		if( Validator.isNotNull(fileHandlerEnabled) && fileHandlerEnabled.equalsIgnoreCase("true") )
		{
			ErrorRaiser.throwIfNull( XMLHelper.getTextValueOf(fileHandlerNode, "@formname"), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			ErrorRaiser.throwIfNull( XMLHelper.getTextValueOf(fileHandlerNode, "@foldername"), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			if( Validator.isNotNull(fileHandlerCritic) && fileHandlerCritic.equalsIgnoreCase("true") )
				handlerEnabledCritic = true;
		}

		Node servletHandlerNode = dataRoot.selectSingleNode("handlers/servlethandler");

		String servletHandlerEnabled = XMLHelper.getTextValueOf(servletHandlerNode, "@enabled");
		String servletHandlerCritic = XMLHelper.getTextValueOf(servletHandlerNode, "@critic");

		if( Validator.isNotNull(servletHandlerEnabled) && servletHandlerEnabled.equalsIgnoreCase("true") )
		{
			Node servletHandlerURL = servletHandlerNode.selectSingleNode("url");
			ErrorRaiser.throwIfNull( servletHandlerURL, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			ErrorRaiser.throwIfFalse( !servletHandlerURL.getText().isEmpty(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			if( Validator.isNotNull(servletHandlerCritic) && servletHandlerCritic.equalsIgnoreCase("true") )
				handlerEnabledCritic = true;
		}

		Node emailHandlerNode = dataRoot.selectSingleNode("handlers/emailhandler");

		String emailHandlerEnabled = XMLHelper.getTextValueOf(emailHandlerNode, "@enabled");
		String emailHandlerCritic = XMLHelper.getTextValueOf(emailHandlerNode, "@critic");

		if( Validator.isNotNull(emailHandlerEnabled) && emailHandlerEnabled.equalsIgnoreCase("true") )
		{
			ErrorRaiser.throwIfNull( XMLHelper.getTextValueOf(emailHandlerNode, "@type"), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			Node servletHandlerPara= emailHandlerNode.selectSingleNode("para");
			Node servletHandlerCC = emailHandlerNode.selectSingleNode("cc");
			Node servletHandlerCCO = emailHandlerNode.selectSingleNode("cco");
			
			if(  (Validator.isNull(servletHandlerPara) || servletHandlerPara.getText().isEmpty()) &&
					(Validator.isNull(servletHandlerCC) || servletHandlerCC.getText().isEmpty()) &&
					(Validator.isNull(servletHandlerCCO) || servletHandlerCCO.getText().isEmpty()) )
			{
				ErrorRaiser.throwIfNull( servletHandlerPara, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			}
			
			String servletHandlerSmtpserver = XMLHelper.getTextValueOf(emailHandlerNode, "@smtpserver");
			ErrorRaiser.throwIfNull( servletHandlerSmtpserver, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			ErrorRaiser.throwIfFalse( !servletHandlerSmtpserver.trim().isEmpty(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			Node servletHandlerTxtsubject = emailHandlerNode.selectSingleNode("textsubject");
			ErrorRaiser.throwIfNull( servletHandlerTxtsubject, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			ErrorRaiser.throwIfFalse( !servletHandlerTxtsubject.getText().isEmpty(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			if( Validator.isNotNull(emailHandlerCritic) && emailHandlerCritic.equalsIgnoreCase("true") )
				handlerEnabledCritic = true;
		}
		
		ErrorRaiser.throwIfFalse( handlerEnabledCritic, IterErrorKeys.XYZ_ITR_E_NO_EXISTS_HANDLER_ENABLED_CRITIC_ZYX);
		
		Node error = dataRoot.selectSingleNode("ko/handlerfailmsg");
		ErrorRaiser.throwIfNull( error, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse( !error.getText().isEmpty(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

	}
	
	private String getFormById( String formid ) throws SecurityException, NoSuchMethodException
	{
		String result = "";
		
		String query = String.format(GET_FORM_BY_FORMID, formid);
		Document d = PortalLocalServiceUtil.executeQueryAsDom(query, new String[]{"description", "navigationtype"} );
		
		String xslpath = new StringBuilder("").append(File.separatorChar).append("base-portlet")
										.append(File.separatorChar).append("xsl")
										.append(File.separatorChar).append("getforms.xsl").toString();

		result = XSLUtil.transformXML(d.asXML(), xslpath );
		
		return result;
	}
	
	private void deleteFormFields( String formId ) throws IOException, SQLException
	{
		PortalLocalServiceUtil.executeUpdateQuery( String.format(DELETE_FORM_DATAFIELDS, formId) );
		PortalLocalServiceUtil.executeUpdateQuery( String.format(DELETE_FORM_FIELDS, formId) );
	}
	
	public String getFormsList( long groupId ) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return  PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_FORMS_NAME, groupId) ).asXML();
	}
	
	
	public String getFormDefinition(String formid) throws ServiceError, NoSuchMethodException, SecurityException, DocumentException {
		ErrorRaiser.throwIfNull(formid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		 _log.debug(new StringBuffer("Query: ").append(String.format(GET_FORM_DEFINITION, formid)).toString());
		Document dom = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_FORM_DEFINITION, formid), new String[]{"defaultvalue", "inputctrl", "labelafter", "validator"});
		if (Validator.isNull(dom)){
			_log.debug("Result query is null");
			_log.error("bdd result, dom, is null or empty");
		 }
		ErrorRaiser.throwIfNull(dom);
		 
		// Obtenemos el elemento root
		Element dataRoot =  dom.getRootElement();
		XPath xpath = SAXReaderUtil.createXPath("/rs//row");
		List<Node> rows = xpath.selectNodes(dataRoot);
		
		// xml que devolveremos
		Document xml = SAXReaderUtil.read("<columns/>");
		Element nodeRoot = xml.getRootElement();		
		
		String tabIdBefore = null;		
		Element tab = null;
		// Recorremos los datos
		for (Node row : rows) {
			final String tabId = XMLHelper.getTextValueOf(row, "@tabid");
			if(Validator.isNull(tabId)){
				_log.debug("tabId is null");
				_log.error("tabid is null or empty");
			}
			ErrorRaiser.throwIfNull(tabId);
			
			final String tabName 		= XMLHelper.getTextValueOf(row, "@tabname");
			if(Validator.isNull(tabName)){
				_log.debug("tabName is null");
				_log.error("tab name is null or empty");
			}
			ErrorRaiser.throwIfNull(tabName);
			
			// El tab es nuevo
			if (!Validator.equals(tabId,tabIdBefore) ){
				tab = nodeRoot.addElement("tab");
				tab.addAttribute("id", tabId);
				tab.addAttribute("name", tabName);
				tabIdBefore = tabId;
			}
			
			// Formateamos la columna
			Element column = (Element) row.detach();
			column.setName("column");
			column.remove(column.attribute("tabid"));
			column.remove(column.attribute("tabname"));
			Element ic = formatInputCrtl(row);
			column.remove(SAXReaderUtil.createXPath("inputctrl").selectSingleNode(row));
			column.add(ic); 
			column.remove(SAXReaderUtil.createXPath("defaultvalue").selectSingleNode(row));
			
			//arreglo en el label after el fallo de traerlo como cdata
			String labelAfterText = SAXReaderUtil.read(StringUtil.replace(column.element("labelafter").getText(), "]]]]><![CDATA[>", "]]>") ).getRootElement().element("textlabel").getStringValue();
			column.element("labelafter").setText(labelAfterText);
			Element validator = SAXReaderUtil.read(StringUtil.replace(column.element("validator").getText(), "]]]]><![CDATA[>", "]]>") ).getRootElement();
			column.remove(column.element("validator"));
			column.add(validator);
			
			// Anyadimos la columa al nodo tab
			tab.add(column);
		}
		
		return xml.asXML();
	}
	
	
	private Element formatInputCrtl(Node node) throws DocumentException, ServiceError{
		
			
		XPath xpath = SAXReaderUtil.createXPath("inputctrl");
		Node inputctrlNode = xpath.selectSingleNode(node);
		Element iCtrl =  SAXReaderUtil.read( CDATAUtil.strip( inputctrlNode.getText() ) ).getRootElement();
		
		Element inputCtrl = SAXReaderUtil.createElement("inputctrl");
		
		String inputCtrlName = XMLHelper.getTextValueOf(node, "@htmlname");
		if( Validator.isNull(inputCtrlName) || inputCtrlName.isEmpty() )
			inputCtrlName = XMLHelper.getTextValueOf(node, "@fieldid");
		ErrorRaiser.throwIfNull(inputCtrlName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String fieldid = XMLHelper.getTextValueOf(node, "@fieldid");
		ErrorRaiser.throwIfNull(fieldid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String iControlType = iCtrl.attributeValue("type");
		
		inputCtrl.addAttribute("name", inputCtrlName);
		inputCtrl.addAttribute("id", fieldid);
		inputCtrl.addAttribute("size", iControlType);
		
		if(iControlType.equalsIgnoreCase("radiobutton") || iControlType.equalsIgnoreCase("listctrl") || 
				iControlType.equalsIgnoreCase("dropdownlist") || iControlType.equalsIgnoreCase("checkbox"))
		{
			Element options = SAXReaderUtil.createElement("options");
			
			Element opts = iCtrl.element("options");
			if( Validator.isNotNull(opts) )
			{
				List<Element> optList = opts.elements();
				ArrayList<String> listOptions = new ArrayList<String>();
				
				if(iControlType.equalsIgnoreCase("listctrl") || iControlType.equalsIgnoreCase("dropdownlist"))
					options.addAttribute("multiple", opts.attributeValue("size"));
				
				options.addAttribute("multiple", opts.attributeValue("multiple", "false"));
				
				for( Element opt : optList )
				{
					Element option = SAXReaderUtil.createElement("type");
					option.addAttribute("selected", listOptions.contains( opt.attributeValue("name") )?"true":"false" );
					option.addAttribute("value", opt.attributeValue("value"));
					option.addText(opt.attributeValue("name"));
					options.add(option);
				}
				
				inputCtrl.add(options);
			}
		}
		
		return inputCtrl;
	}

	public void publishToLive(String formIds) throws SecurityException, NoSuchMethodException, ServiceError, PortalException, SystemException, UnsupportedEncodingException, ClientProtocolException, IOException, DocumentException
	{
		ErrorRaiser.throwIfFalse(Validator.isNotNull(formIds), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
		{
			if (exportWriteLock.tryLock())
			{
				try
				{
					_publishToLive(formIds);
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
	private void _publishToLive(String formIds) throws SecurityException, NoSuchMethodException, ServiceError, PortalException, SystemException, UnsupportedEncodingException, ClientProtocolException, IOException, DocumentException
	{
		ErrorRaiser.throwIfFalse(Validator.isNotNull(formIds), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Element root = _exportData(Arrays.asList(formIds.split(",")) , true).getRootElement();
		
		long companyId = GroupMgr.getCompanyId();
		ErrorRaiser.throwIfFalse(companyId > 0);

		LiveConfiguration liveConf	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(companyId);
		String remoteIP 			= liveConf.getRemoteIterServer2().split(":")[0];
		int remotePort 				= Integer.valueOf(liveConf.getRemoteIterServer2().split(":")[1]);
		String remoteMethodPath 	= "/base-portlet/secure/json";
		
		List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
		remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.base.service.IterFormServiceUtil"));
		remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"importForms"));
		remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[forms]"));
		remoteMethodParams.add(new BasicNameValuePair("forms", 		root.asXML()));
		
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
	
	public Document exportData(List<String> ids) throws SecurityException, ServiceError, NoSuchMethodException, DocumentException
	{
		return _exportData(ids, false);
	}
	
	public Document exportData(Long groupId) throws SecurityException, ServiceError, NoSuchMethodException, DocumentException
	{
		String sql = String.format("SELECT formid FROM form WHERE groupid = %d", groupId);
		return _exportData( PortalLocalServiceUtil.executeQueryAsList(sql), false );
	}

	private Document _exportData(List<?> ids, boolean globalIdAsReference) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError
	{
		Element root = SAXReaderUtil.read("<forms/>").getRootElement();
		root.addAttribute("globalIdAsReference", String.valueOf(globalIdAsReference));
		
		// Se construye un XML con cada uno de los formularios
		for (int i = 0; i < ids.size(); i++)
		{
			String id = (String)ids.get(i);
			// Se obtiene la definición del formulario
			Element form = (Element)SAXReaderUtil.read(getForm(id, globalIdAsReference)).getRootElement().detach();
			root.add( form );
			
			boolean isRegistryForm = FORM_TYPE_REG.equalsIgnoreCase( XMLHelper.getTextValueOf(form, "@formtype") );
			
			// Se añaden los productos
			form.add( getFormProducts2(id).getRootElement().detach() );
			
			// Se añade la definición de los tabs
			Element tabs = (Element)SAXReaderUtil.read( IterTabsLocalServiceUtil.getTabs(id)).getRootElement().detach();
			tabs.setName("tabs");
			form.add(tabs);
			
			// Para cada tab, se añaden sus fields
			List<Node> tabList = tabs.selectNodes("row");
			for (int iTab = 0; iTab < tabList.size(); iTab++)
			{
				Element elemTab = (Element)tabList.get(iTab);
				elemTab.addAttribute("formid", id);
				
				String tabId = XMLHelper.getTextValueOf(elemTab, "@tabid");
				ErrorRaiser.throwIfNull(tabId);
				
				Element fields = (Element)SAXReaderUtil.read( IterTabsLocalServiceUtil.getTabFields(tabId)).getRootElement().detach();
				Element formFields = elemTab.addElement("fields");
				
				// Para cada field se añaden sus detalles
				List<Node> fieldList = fields.selectNodes("row");
				for (int iField = 0; iField < fieldList.size(); iField++)
				{
					Element elemField = (Element)fieldList.get(iField);
					String fieldId = XMLHelper.getTextValueOf(elemField, "@fieldid");
					ErrorRaiser.throwIfNull(fieldId);
					
					Element details = (Element)SAXReaderUtil.read( IterFieldLocalServiceUtil.getField(fieldId) ).getRootElement().selectSingleNode("*").detach();
					details.setName("field");
					details.addAttribute("fieldid", fieldId);
					details.addAttribute("tabid", 	tabId);
					details.addAttribute("formid", 	id);
					
					// Solo estará activo en los formularios de registro
					if (isRegistryForm)
					{
						String profilefieldid = XMLHelper.getTextValueOf(elemField, "@profilefieldid");
						ErrorRaiser.throwIfNull(profilefieldid);
						details.addAttribute("profilefieldid", profilefieldid);
						
						String fieldname = XMLHelper.getTextValueOf(elemField, "@fieldname");
						ErrorRaiser.throwIfNull(fieldname);
						details.addAttribute("fieldname", fieldname);
					}						
					
					formFields.add( details);
				}
			}
			
		}
		
		// Se sustituye las UUIDs de las internalPage por su globalId
		List<Node> pageList = root.selectNodes("/forms/form/*[name() = 'ko' or name() = 'ok']/action[@type='internalpage']");
		String[] uuidList	= XMLHelper.getStringValues(pageList, "text()");
		
		String refValues = StringUtil.merge(uuidList, "','");
		String sql = String.format((globalIdAsReference) ? GET_PAGE_FROM_UUID : GET_LAYOUT_FROM_UUID, refValues);
		Document pageDom = PortalLocalServiceUtil.executeQueryAsDom(sql);
		for (int i = 0; i < pageList.size(); i++)
		{
			Node page = pageList.get(i);
			String friendlyURL = XMLHelper.getTextValueOf(pageDom, String.format("/rs/row[@uuid_ = '%s']/@layoutRef", uuidList[i]));
			ErrorRaiser.throwIfNull(friendlyURL);
			page.setText(friendlyURL);
		}
				
		return root.getDocument();
	}

	
	public void importForms(String forms) throws Exception
	{
		if(importWriteLock.tryLock())
		{
			InputStream is = null;
			try
			{
				ErrorRaiser.throwIfFalse(Validator.isNotNull(forms), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
				Document dom = SAXReaderUtil.read( forms );
				boolean updtIfExist 	= GetterUtil.getBoolean(XMLHelper.getStringValueOf(dom.getRootElement(), "@updtIfExist", 	"true"));
				boolean isImportProcess = GetterUtil.getBoolean(XMLHelper.getStringValueOf(dom.getRootElement(), "@importProcess", 	"false"));
				
				List<Node> list = dom.selectNodes("/forms/form");
				if (list.size() > 0)
				{
					long groupId = XMLHelper.getLongValueOf(dom.getRootElement(), "@groupId");
					if (groupId <= 0)
					{
						// Se ASUME que todos los formularios pertenecerán al mismo grupo
						String groupName = XMLHelper.getTextValueOf(dom, "/forms/form/@groupName[1]");
						ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_INVALID_GROUP_NAME_ZYX);
						groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
					}
					
					// Se consulta si los formularios ya existen en el entorno
					Document existDom = PortalLocalServiceUtil.executeQueryAsDom( String.format("SELECT formid, name FROM form WHERE groupid = %d", groupId) );
					
					// Se sustituye los globalId de las internalPage por su UUID
					List<Node> pageList = dom.selectNodes("/forms/form/*[name() = 'ko' or name() = 'ok']/action[@type='internalpage']");
					String[] layoutRefList = XMLHelper.getStringValues(pageList, "text()");
					
					boolean globalIdAsReference = GetterUtil.get(XMLHelper.getTextValueOf(dom, "/forms/@globalIdAsReference"), true);
					String refValues = StringUtil.merge(layoutRefList, "','");
					String sql = (globalIdAsReference) ? String.format(GET_PAGE_FROM_GLOBALID, refValues) : String.format(GET_PAGE_FROM_FRIENDLYURL, groupId, refValues);
					
					Document pageDom = PortalLocalServiceUtil.executeQueryAsDom(sql);
					for (int i = 0; i < pageList.size(); i++)
					{
						Node page = pageList.get(i);
						String uuid_ = XMLHelper.getTextValueOf(pageDom, String.format("/rs/row[@layoutRef = '%s']/@uuid_", layoutRefList[i]));
						ErrorRaiser.throwIfNull(uuid_, IterErrorKeys.XYZ_E_FORM_POSTACTION_LAYOUT_NOT_FOUND_ZYX, layoutRefList[i]);
					
						page.setText(uuid_);
					}
					
					// Se consultan los IDs de los productos que existen en el sistema para los correspondientes productName
					List<Node> productList 			= dom.selectNodes("/forms/form/products/product/@id[string-length(.) > 0]");
					String[] productNameList 		= XMLHelper.getStringValues(productList);
					String[] escapedProductNameList = StringEscapeUtils.escapeSql( StringUtils.join( productNameList, IterKeys.PRODUCT_SEPARATOR ) ).split(IterKeys.PRODUCT_SEPARATOR);
					
					Document productDom = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_PRODUCT_FROM_NAME, StringUtil.merge( escapedProductNameList, "','")) );
					for (int i = 0; i < productList.size(); i++)
					{
						Node product = productList.get(i);
						String productId = XMLHelper.getTextValueOf(productDom, String.format("/rs/row[@productname = \"%s\"]/@productId", productNameList[i]));
						
						ErrorRaiser.throwIfNull(productId, IterErrorKeys.XYZ_E_PRODUCT_NOT_FOUND_ZYX, productId);
						product.setText(productId);
					}
					
					for (int i = 0; i < list.size(); i++)
					{
						Element elem2Imp = (Element)list.get(i);
						String formName = elem2Imp.attributeValue("name");
						String publishedFormId = elem2Imp.attributeValue("formid");
						
						Node oldForm  = null;
						if (isImportProcess)
							oldForm = existDom.selectSingleNode(String.format("/rs/row[@name='%s']", formName));
						else
							oldForm  = existDom.selectSingleNode(String.format("/rs/row[@formid='%s']", publishedFormId));
						boolean exist = (oldForm != null);

						ErrorRaiser.throwIfFalse( updtIfExist || !exist, IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, 
								 					String.format("%s(%s)", IterAdmin.IA_CLASS_FORM, formName));
						
						// Se crea el atributo groupid con el valor de este entorno
						elem2Imp.addAttribute("groupid", String.valueOf(groupId));
	
						boolean isRegistryForm = elem2Imp.attributeValue("formtype").equalsIgnoreCase(FORM_TYPE_REG);
						if (isRegistryForm)
						{
							String query = "select count(*) counter from form where name<>'%s' and formtype='%s' and groupid=%s";
							query = String.format(query, formName, FORM_TYPE_REG, groupId);
							long counter = XMLHelper.getLongValueOf( PortalLocalServiceUtil.executeQueryAsDom(query), "/rs/row/@counter" );
							ErrorRaiser.throwIfFalse( counter == 0, IterErrorKeys.XYZ_ITR_E_REGISTER_FORM_ALREADY_EXISTS_ZYX);
						}
						
						// En una creación o una publicación al LIVE puede interesar mantener el UUID origen
						// Es un proceso de importación:
						//  - Ya existe, se sustituye el formID por el formid que tenga el mismo nombre
						//  - No existe con ese nombre en el LIVE, se borra el UUID para que genere uno nuevo
						if (isImportProcess)
							elem2Imp.addAttribute("formid", (exist) ? ((Element)oldForm).attributeValue("formid") : "");

						Document importedForm = SAXReaderUtil.read( (exist) ? editForm(elem2Imp) : addForm(elem2Imp) );
						
						String formId = XMLHelper.getTextValueOf(importedForm, "/rs/row/@formid");
						ErrorRaiser.throwIfNull(formId);
						
						List<Node> tabsList = elem2Imp.selectNodes("tabs/row");
						
						// Elimina los TABs que no vengan en el XML (Y por tanto sus FIELDs y FIELDRECEIVEDs)
						String tabsIds = StringUtil.merge(XMLHelper.getStringValues(tabsList, "@tabid"), StringPool.COMMA_AND_SPACE,StringPool.APOSTROPHE);
						if (Validator.isNotNull(tabsIds))
							PortalLocalServiceUtil.executeUpdateQuery(String.format("DELETE FROM formtab WHERE formid = '%s' AND tabid NOT IN (%s)", formId, tabsIds));
						
						// Crea / Actualiza los TABs y los FIELDs
						for(int tabIdx = 0; tabIdx < tabsList.size(); tabIdx++)
						{
							Element tab2Imp = (Element)tabsList.get(tabIdx);
							tab2Imp.addAttribute("formid", formId);
							String tabId = XMLHelper.getStringValueOf(tab2Imp, "@tabid");
							ErrorRaiser.throwIfNull(tabId);
							
							// Si existe el TAB, lo actualiza. Si no, lo crea.
							if (existsTab(tabId))
								SAXReaderUtil.read( IterTabsLocalServiceUtil.editTab(tab2Imp) );
							else
								SAXReaderUtil.read( IterTabsLocalServiceUtil.addTab(tab2Imp) );
							
							// Elimina todos los FIELDs que tuviera el TAB y se hayan eliminado.
							List<Node> fieldsList = tab2Imp.selectNodes("fields/field");
							String fieldsIds = StringUtil.merge(XMLHelper.getStringValues(fieldsList, "@fieldid"), StringPool.COMMA_AND_SPACE,StringPool.APOSTROPHE);
							if (Validator.isNotNull(fieldsIds))
								PortalLocalServiceUtil.executeUpdateQuery(String.format("DELETE FROM formfield WHERE tabid = '%s' AND fieldid NOT IN (%s)", tabId, fieldsIds));
							
							// Crea / Actualiza los FIELDs
							for(int fieldIdx = 0; fieldIdx < fieldsList.size(); fieldIdx++)
							{
								Element field2Imp = (Element)fieldsList.get(fieldIdx);
								field2Imp.addAttribute("formid", 	formId);
								field2Imp.addAttribute("tabid", 	tabId);
								field2Imp.addAttribute("groupid", 	String.valueOf(groupId));
								
								// Si existe el FIELD, lo actualiza. Si no, lo crea.
								if (existsField(XMLHelper.getStringValueOf(field2Imp, "@fieldid")))
									IterFieldLocalServiceUtil.updateField(field2Imp);
								else
									IterFieldLocalServiceUtil.addField(field2Imp);
							}
						}
					}
				}
			}
			catch(ORMException orme)
			{
				ServiceErrorUtil.throwSQLIterException(orme);
			}
			finally
			{
				importWriteLock.unlock();
				
				if (is != null)
					is.close();
			}
		}
		else
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_PUBLISH_ALREADY_IN_PROCESS_ZYX);
		}
	}
	
	private boolean existsTab(String tabId) throws SecurityException, NoSuchMethodException
	{
		Document countTab = PortalLocalServiceUtil.executeQueryAsDom(String.format("SELECT COUNT(*) count FROM formtab WHERE tabid = '%s'", tabId));
		long existsTab = XMLHelper.getLongValueOf(countTab.getRootElement(), "/rs/row/@count");
		return existsTab > 0;
	}
	
	private boolean existsField(String fieldId) throws SecurityException, NoSuchMethodException
	{
		Document countField = PortalLocalServiceUtil.executeQueryAsDom(String.format("SELECT COUNT(*) count FROM formfield WHERE fieldid = '%s'", fieldId));
		long existField = XMLHelper.getLongValueOf(countField.getRootElement(), "/rs/row/@count");
		return existField > 0;
	}
	
	public void matchFormFieldsId() throws SecurityException, NoSuchMethodException, ServiceError, DocumentException, PortalException, SystemException, UnsupportedEncodingException, ClientProtocolException, IOException
	{
		// Sólo para el arranque del PREVIEW
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
		{
			// Si no es un sistema nuevo y la versión es menor que la 3.0.0.18
			if ( (Version.compare(IterGlobal.getLastIterWebCmsVersion(), IterGlobalKeys.FIRST_ITERWEBCMS_VERSION) != 0)
			  && (Version.compare(IterGlobal.getLastIterWebCmsVersion(), "3.0.0.18") < 0) )
			{
				if (_log.isDebugEnabled())
					_log.debug("Launching Forms Fields Update...");
				
				// Recupera los TABs de todos los formularios excepto el de registro
				Document tabs = getTabFieldsForSynchronize();
				
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
					remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.base.service.IterFormServiceUtil"));
					remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"updateFormFieldsIds"));
					remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[fields]"));
					remoteMethodParams.add(new BasicNameValuePair("fields", 			tabs.asXML()));
					
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
						_log.debug("Invalid LIVE IP. Skipping Form Fields Update.");
				}
			}
		}
	}
	
	public void updateFormFieldsIds(String fields) throws ServiceError, DocumentException, SecurityException, NoSuchMethodException, IOException, SQLException
	{
		if (_log.isDebugEnabled())
			_log.debug("Checking Form Fields to Update...");
		
		ErrorRaiser.throwIfFalse( !Validator.isNull(fields) );
		
		// Recupera los campos del PREVIEW
		Document backTabs = SAXReaderUtil.read(fields);
		// Recupera los campos del LIVE
		Document liveTabs = getTabFieldsForSynchronize();
		
		// Para cada TAB
		List<Node> tabList = backTabs.selectNodes("rs/row");
		for (int iTab = 0; iTab < tabList.size(); iTab++)
		{
			// Recupera los datos del TAB del Preview
			Element tab = (Element)tabList.get(iTab);
			String tabId    = XMLHelper.getStringValueOf(tab, "@tabid");
			String formId   = XMLHelper.getStringValueOf(tab, "@formid");
			String tabName  = XMLHelper.getStringValueOf(tab, "@name");
			String tabOrder = XMLHelper.getStringValueOf(tab, "@taborder");
			
			// Recupera el Tab del LIVE y comprueba que sea el mismo
			List<Node> candidatesTabs = liveTabs.selectNodes(String.format("/rs/row[@formid = '%s' and @taborder = '%s']", formId, tabOrder));
			Node liveTab = null;
			for (Node candidate : candidatesTabs)
			{
				if ( tabName.equals(XMLHelper.getStringValueOf(candidate, "@name")) )
				{
					liveTab = candidate;
					break;
				}
			}
			if (liveTab != null)
			{
				// Actualiza el ID si es distinto
				String liveTabId = XMLHelper.getTextValueOf(((Element) liveTab), "@tabid");
				if (!tabId.equals(liveTabId))
				{
					_log.info("Updating Tab with Id " + liveTabId + ". New Id is " + tabId);
					PortalLocalServiceUtil.executeUpdateQuery(String.format("UPDATE formtab SET tabId='%s' WHERE tabId='%s'", tabId, liveTabId));
				}
				else
				{
					_log.info("Tab with Id " + liveTabId + " is synchronized");
				}
				
				// Recupera los FIELDs del PREVIEW
				List<Node> fieldList = tab.selectNodes("fields/field");
				// Actualiza los FIELDs
				for (int iField = 0; iField < fieldList.size(); iField++)
				{
					// Recupera los datos del FIELD del Preview
					Element field      = (Element)fieldList.get(iField);
					String fieldId     = XMLHelper.getStringValueOf(field, "@fieldid");
					String htmlname    = XMLHelper.getStringValueOf(field, "@htmlname");
					String labelbefore = "CDATA[" + valueOfFieldLabel(XMLHelper.getStringValueOf(field, "@labelbefore")) + "]";
					String labelafter  = "CDATA[" + valueOfFieldLabel(XMLHelper.getStringValueOf(field, "@labelafter")) + "]";
					String tooltip     = XMLHelper.getStringValueOf(field, "@tooltip");
					String css         = XMLHelper.getStringValueOf(field, "@css");
					String fieldname   = XMLHelper.getStringValueOf(field, "@fieldname");
					String fieldorder   = XMLHelper.getStringValueOf(field, "@fieldorder");
					
					// Recupera el FIELD del LIVE y comprueba que sea el mismo
					List<Node> candidatesFields = liveTab.selectNodes(String.format("fields/field[@htmlname = '%s' and @fieldorder='%s' and @css='%s']", htmlname, fieldorder, css));
					Node liveField = null;
					for (Node candidate : candidatesFields)
					{
						if ( XMLHelper.getStringValueOf(candidate, "@labelbefore").contains(labelbefore)
						  && XMLHelper.getStringValueOf(candidate, "@labelafter").contains(labelafter)
						  && XMLHelper.getStringValueOf(candidate, "@tooltip").equals(tooltip)
						  && XMLHelper.getStringValueOf(candidate, "@fieldname").equals(fieldname)
						   )
						{
							liveField = candidate;
							break;
						}
					}
					
					if (liveField != null)
					{
						// Actualiza el ID si es distinto
						String liveFieldId = XMLHelper.getTextValueOf(((Element) liveField), "@fieldid");
						if (!fieldId.equals(liveFieldId))
						{
							_log.info("Updating Field with Id " + liveFieldId + ". New Id is " + fieldId);
							try
							{
								PortalLocalServiceUtil.executeUpdateQuery(String.format("UPDATE formfield SET fieldid='%s' WHERE fieldid='%s'", fieldId, liveFieldId));
							}
							catch (Exception e)
							{
								_log.error("Field [fieldId=" + fieldId + ", htmlname=" + htmlname + ", labelbefore=" + labelbefore + ", labelafter=" + labelafter + ", labelbefore=" + labelbefore + ", tooltip=" + tooltip + ",fielorder=" + fieldorder + ", css=" + css + "] duplicated. Manual update required!");
							}
						}
						else
						{
							_log.info("Field with Id " + liveFieldId + " is synchronized");
						}
					}
					else
					{
						// Registra el error y aborta la actualización de este FIELD
						_log.error("Field [fieldId=" + fieldId + ", htmlname=" + htmlname + ", labelbefore=" + labelbefore + ", labelafter=" + labelafter + ", labelbefore=" + labelbefore + ", tooltip=" + tooltip + ",fielorder=" + fieldorder + ", css=" + css + "] not found in Live. Skipping...");
					}
				}
			}
			else
			{
				// Registra el error y aborta la actualización de este TAB y sus FIELDs
				_log.error("Tab [tabId=" + tabId + ", name=" + tabName + ", order=" + tabOrder + "] not found in Live. Skipping...");
			}
		}
	}
	
	private String valueOfFieldLabel(String label)
	{
		String value = StringPool.BLANK;
		
		try
		{
			value = XMLHelper.getStringValueOf(SAXReaderUtil.read(label), "//textlabel");
		}
		catch (DocumentException e) { }
		
		return value;
	}
	
	private Document getTabFieldsForSynchronize() throws SecurityException, NoSuchMethodException, ServiceError
	{
		// Recupera los TABs de todos los formularios excepto el de registro
		Document tabs = PortalLocalServiceUtil.executeQueryAsDom("SELECT t.tabid, t.formid, t.name, t.taborder, f.formtype FROM formtab t INNER JOIN form f ON f.formid = t.formid");
		
		// Para cada tab, se añaden sus fields
		List<Node> tabList = tabs.selectNodes("rs/row");
		for (int iTab = 0; iTab < tabList.size(); iTab++)
		{
			Element elemTab = (Element)tabList.get(iTab);
			String tabId = XMLHelper.getTextValueOf(elemTab, "@tabid");
			ErrorRaiser.throwIfNull(tabId);
			
			String sqlFields = new StringBuilder()
				.append(" SELECT fieldid, htmlname, labelbefore, labelafter, tooltip, fieldorder, css, userprofile.fieldname ")
				.append(" FROM formfield ")
				.append(" LEFT JOIN userprofile on userprofile.datafieldid = formfield.datafieldid ")
				.append(" WHERE tabid = '%s'")
				.toString();
			Document fields = PortalLocalServiceUtil.executeQueryAsDom(String.format(sqlFields, tabId), true, "fields", "field");
			elemTab.add(fields.getRootElement().detach());
		}
		
		return tabs;
	}
}
