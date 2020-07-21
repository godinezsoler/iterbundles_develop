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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.xml.CDATAUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.service.util.TeaserMgr;
import com.liferay.portal.kernel.xml.XSLUtil;
import com.protecmedia.iter.user.service.base.FormReceivedMgrLocalServiceBaseImpl;
import com.protecmedia.iter.user.util.export_formats.CSVUtil;

/**
 * The implementation of the form received mgr local service.
 * 
 * <p>
 * All custom service methods should be put in this class. Whenever methods are
 * added, rerun ServiceBuilder to copy their definitions into the
 * {@link com.protecmedia.iter.user.service.FormReceivedMgrLocalService}
 * interface.
 * </p>
 * 
 * <p>
 * Never reference this interface directly. Always use
 * {@link com.protecmedia.iter.user.service.FormReceivedMgrLocalServiceUtil} to
 * access the form received mgr local service.
 * </p>
 * 
 * <p>
 * This is a local service. Methods of this service will not have security
 * checks based on the propagated JAAS credentials because this service can only
 * be accessed from within the same VM.
 * </p>
 * 
 * @author Brian Wing Shun Chan
 * @see com.protecmedia.iter.user.service.base.FormReceivedMgrLocalServiceBaseImpl
 * @see com.protecmedia.iter.user.service.FormReceivedMgrLocalServiceUtil
 */
/**
 * @author aruiz
 * 
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class FormReceivedMgrLocalServiceImpl extends FormReceivedMgrLocalServiceBaseImpl
{

	private static Log _log = LogFactoryUtil.getLog(FormReceivedMgrLocalServiceImpl.class);

	private static final String XPATH_INPUTFIELDID = "/rs/row[@fieldid='%s']";
	
	final private SimpleDateFormat SDF_WITH_MLS = new SimpleDateFormat("yyyyMMddhhmmssSSS");
	
	final private SimpleDateFormat SDF_CSV = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss);
	
	private static Map<String, String> operadores = null;

	final List<String> fieldFijos = Arrays.asList(new String[] { "checked", "datesent", "usrname", "formreceivedid" });
	
	/**
	 *  obtener los formularios de un grupo concreto
	 *  format(groupid)
	 */
	private static final String GET_GENERIC_FORM_GROUP_WITH_RECEIVED = new StringBuffer()
		.append("SELECT f.formid, f.name, f.description, COUNT(fr.formreceivedid) 'responsesNumber', \n")
		.append("	(SELECT COUNT(fr2.formreceivedid) FROM formreceived fr2 JOIN form f2 ON fr2.formid = f2.formid WHERE f2.formid = f.formid AND fr2.checked) 'formsReceivedChecked' \n")
		.append("FROM form f \n")
		.append("	INNER JOIN formreceived fr ON f.formid = fr.formid \n")
		.append("WHERE f.groupid= '%s' AND f.formtype = 'generico' \n")
		.append("GROUP BY f.formid")
		.toString();

	/**
	 * Borar todos los formularios recibidos de un formulario concrecto
	 * format('formid', 'formid',...)
	 */
	private static final String DELETE_FORMS_RECEIVED_FROM_FORM = "DELETE FROM formreceived WHERE formid IN %s";
	
	private static final String DELETE_FORMS_RECEIVED = "DELETE FROM formreceived WHERE formreceivedid IN %s";
	
	/**
	 * Obtener el control de entrada de un campo concreto
	 * format(fieldid)
	 */
	private static final String GET_INPUTCTRL = new StringBuffer()
		.append("SELECT fieldid,inputctrl, defaultvalue, htmlname \n")
		.append("FROM formfield \n")
		.append("WHERE fieldid = '%s' \n")
		.append("ORDER BY fieldorder")
		.toString();
	
	private static final String GET_FORMTYPE = new StringBuffer()
		.append("SELECT f.formtype \n")
		.append("FROM formfield ff \n") 
		.append("	INNER JOIN  formtab ft ON ff.tabid = ft.tabid \n")
		.append("	INNER JOIN  form f ON ft.formid = f.formid \n")
		.append("WHERE ff.fieldid = '%s' \n")
		.append("LIMIT 1")
		.toString();
	
	private static final String GET_POSSIBLE_OCURRENCE_GENERIC = new StringBuffer()
		.append("SELECT fdr.fieldvalue \n")
		.append("FROM formfield ff  \n")
		.append("	INNER JOIN  fieldreceived fdr ON fdr.fieldid = ff.fieldid \n")
		.append("WHERE ff.fieldid = '%s' \n")
		.append("group by fieldvalue ")
		.toString();
	
	private static final String GET_POSSIBLE_OCURRENCE_USER = new StringBuffer()
		.append("SELECT fdr.fieldvalue \n")
		.append("FROM formfield ff  \n")
		.append("	INNER JOIN  fieldreceived fdr ON fdr.fieldid = ff.fieldid \n")
		.append("	INNER JOIN  formtab ft ON ff.tabid = ft.tabid \n")
		.append("	INNER JOIN  form f ON ft.formid = ft.formid \n")
		.append("WHERE ff.fieldid = '%s' \n")
		.append("GROUP BY fieldvalue ")
		.toString();
		
	private static final String GET_LABELS_INPUT = new StringBuffer()
		.append("SELECT f.name 'formname', ff.fieldid, \n")		
		
		/* Para el nombre del campo se utilizará el primer valor no nulo en este orden:
		   Nombre HTML (ff.htmlname)
		   El texto anterior al campo (ff.labelbefore)
           El texto posterior al campo (ff.labelafter)
           Tooltip (ff.tooltip)
           Clase CSS (ff.css) */
	    .append("  if(NOT isnull(ff.htmlname) and LENGTH(ff.htmlname)>0, ff.htmlname, \n")
		.append("     if(NOT isnull(extractValue(ff.labelbefore, '/labelbefore/textlabel/text()')) AND LENGTH(extractValue(ff.labelbefore, '/labelbefore/textlabel/text()')), extractValue(ff.labelbefore, '/labelbefore/textlabel/text()'), \n") 
		.append("        if (NOT isnull(extractValue(ff.labelafter, '/labelafter/textlabel/text()')) AND LENGTH(extractValue(ff.labelafter, '/labelafter/textlabel/text()'))>0, extractValue(ff.labelafter, '/labelafter/textlabel/text()'), \n")
		.append("            if (NOT isnull(ff.tooltip) AND LENGTH(ff.tooltip)>0, ff.tooltip, ff.css) \n")
		.append("           ) \n")	   
		.append("       ) \n")    
		.append("    ) 'name' \n")		
		
		.append("FROM form f \n")
		.append("	INNER JOIN formtab ft ON f.formid = ft.formid \n")
		.append("	INNER JOIN formfield ff ON ft.tabid = ff.tabid \n")
		.append("	INNER JOIN datafield df ON ff.datafieldid = df.datafieldid \n")
		.append("WHERE f.formid = '%s' ")
		.toString();
	
	/**
	 *  obtiene los metadatos para el grid
	 *  format(formid)
	 */
	private static final String GET_METADATA = new StringBuffer()
		.append("SELECT ft.tabid, ft.name tabname, ff.fieldid 'id', df.fieldtype , \n")
		
		
		/* Para el nombre del campo se utilizará el primer valor no nulo en este orden:
		   Nombre HTML (ff.htmlname)
		   El texto anterior al campo (ff.labelbefore)
           El texto posterior al campo (ff.labelafter)
           Tooltip (ff.tooltip)
           Clase CSS (ff.css) */
		.append("  if(NOT isnull(ff.htmlname) and LENGTH(ff.htmlname)>0, ff.htmlname, \n")
		.append("     if(NOT isnull(extractValue(ff.labelbefore, '/labelbefore/textlabel/text()')) AND LENGTH(extractValue(ff.labelbefore, '/labelbefore/textlabel/text()')), extractValue(ff.labelbefore, '/labelbefore/textlabel/text()'), \n") 
		.append("        if (NOT isnull(extractValue(ff.labelafter, '/labelafter/textlabel/text()')) AND LENGTH(extractValue(ff.labelafter, '/labelafter/textlabel/text()'))>0, extractValue(ff.labelafter, '/labelafter/textlabel/text()'), \n")
		.append("            if (NOT isnull(ff.tooltip) AND LENGTH(ff.tooltip)>0, ff.tooltip, ff.css) \n")
		.append("           ) \n")	   
		.append("       ) \n")    
		.append("    ) 'name' \n")		
		
		.append(", extractvalue(ff.inputctrl, '/inputctrl/@type') inputctrltype \n")
		.append("FROM form f \n")
		.append("	INNER JOIN formtab ft 		ON ft.formid = f.formid \n")
		.append("	INNER JOIN formfield ff 	ON ff.tabid = ft.tabid \n")
		.append("	INNER JOIN datafield df 	ON ff.datafieldid = df.datafieldid \n")
		.append("WHERE f.formid = '%s'")
		.append("ORDER BY ft.taborder, ff.fieldorder ")
		.toString();
	
	/**
	 * obtiene los datos basicos 
	 * format(formid , varColumnIds , predicates)
	 */
	private static final String GET_QUERY_GRID_VALUES_FETCH_DISCRETE = new StringBuffer()
	.append("SELECT ITR_FETCH_DISCRETE_FUNCT_FORMS_REPORT(\"%s\", \"%s\", \"%s\")").toString();
	
	
	private static final String GET_QUERY_GRID_FETCH = new StringBuffer()
		.append("SELECT ITR_FETCH_FUNCT_FORMS_REPORT( \"%s\" , \"%s\", \"%s\", \"%s\")")
		.toString();


	private static final String PREDICATE_FORMRECEIVEDS_VALUES = new StringBuffer()
	.append(" formreceived.formreceivedid IN %s AND ")
	.toString();

	/**
	 * format (operador [< | =], 	fieldid, operator_value)
	 */
	private static final String PREDICATE_FILTER_ROW = new StringBuffer()
	.append(" 0 %s (SELECT COUNT(*) \n")
	.append("		  FROM fieldreceived fieldreceived1 \n")
	.append("		 WHERE formreceived.formreceivedid=fieldreceived1.formreceivedid \n")
	.append("			 AND ( fieldreceived1.fieldid = '%s' ) \n")
	.append("			 AND  %s  \n")//fieldreceived1.fieldvalue
	.append("	  ) ")
	.toString();

	private static final String TABLE_COLUMN_ROW_VALUE = "fieldreceived1.fieldvalue";
	
	/**
	 * format (operador [< | =], 	fieldid, operator_value)
	 */
	private static final String PREDICATE_FILTER_BINROW = new StringBuffer()
	.append(" 0 %s (SELECT COUNT(*) \n")
	.append("		 FROM fieldreceived fieldreceived1 \n")
	.append("        INNER JOIN dlfileentry dlfileentry1 ON fieldreceived1.binfieldvalueid = dlfileentry1.fileEntryId \n")
	.append("		 WHERE formreceived.formreceivedid=fieldreceived1.formreceivedid \n")
	.append("			  AND ( fieldreceived1.fieldid = '%s' ) \n")
	.append("			 AND  %s  \n")
	.append("	  ) ")
	.toString();
	
	private static final String TABLE_COLUMN_BINROW_VALUE = "dlfileentry1.description";
	
	/**
	 * Acualiza el check un formreceived 
	 */
	private static final String UPDATE_CHECK_FORMRECEIVED	= 	"UPDATE formreceived SET checked=%s  WHERE formreceivedid='%s'";

	/**
	 * Actualiza los check de los formreceived indicados al valor indicado
	 */
	private static final String UPDATE_CHECK_LIST_FORMRECEIVED	= 	"UPDATE formreceived SET checked=%s  WHERE formreceivedid IN %s";
	
	/**
	 * Obtiene el listado de [id, nombre] de los ficheros adjuntos
	 */
	private static final String GET_FILES_FORMRECEIVED = new StringBuffer() 
		.append("SELECT fdr.fieldreceivedid 'id', CONCAT(f.name,'/', ft.name, '/', dlfe.description) 'name' \n")
		.append("FROM formreceived fr \n")
		.append("	INNER JOIN form f 				ON fr.formid = f.formid \n")
		.append("	INNER JOIN formtab ft 			ON f.formid = ft.formid \n")
		.append("	INNER JOIN fieldreceived fdr	ON fdr.formreceivedid = fr.formreceivedid \n")
		.append("	INNER JOIN formfield ff 		ON (fdr.fieldid = ff.fieldid and ff.tabid = ft.tabid) \n")
		.append("	INNER JOIN dlfileentry dlfe 	ON fdr.binfieldvalueid = dlfe.fileEntryId \n")
		.append("WHERE fr.formreceivedid = '%s' ")
		.toString();
	
	public String getForms(String groupId) throws ServiceError, NoSuchMethodException, DocumentException
	{
		String result = "";
		Long groupIdLong = null;
		
		_log.trace(new StringBuffer("Into getForms"));
		ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		try{
			groupIdLong = Long.valueOf(groupId);
		}catch(NumberFormatException nfe){
			_log.error("getForms groupId not is a number");
			ErrorRaiser.throwIfFalse(false, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}
		ErrorRaiser.throwIfFalse(Validator.isNotNull(groupIdLong), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		_log.trace("obtain forms where groupId AND genericType AND with receivedForms");
		_log.debug(new StringBuffer("Query: ").append(String.format(GET_GENERIC_FORM_GROUP_WITH_RECEIVED, groupIdLong)));
		Document dom = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_GENERIC_FORM_GROUP_WITH_RECEIVED, groupIdLong), new String[] { "description" });
		ErrorRaiser.throwIfNull(dom);

		_log.debug("Generated xml forms with receiveds: " + dom.asXML());
		result = dom.asXML();
		return result;
	}
	
	public String putCheck(String xmlData) throws IOException, SQLException, ServiceError, DocumentException
	{
		_log.trace(new StringBuffer("Into putCheck"));
		ErrorRaiser.throwIfFalse(Validator.isNotNull(xmlData), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row");
		Node node = xpath.selectSingleNode(dataRoot);

		String formreceivedid = XMLHelper.getTextValueOf(node, "@formreceivedid");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(formreceivedid), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		boolean checked = GetterUtil.getBoolean(XMLHelper.getTextValueOf(node, "@checked"), true);

		_log.debug(new StringBuffer("Query: ").append(String.format(UPDATE_CHECK_FORMRECEIVED, checked, formreceivedid)));
		PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_CHECK_FORMRECEIVED, checked, formreceivedid));

		return xmlData;

	}

	public String putListCheck(String xmlData) throws IOException, SQLException, ServiceError, DocumentException
	{
		_log.trace(new StringBuffer("Into putListCheck"));
		ErrorRaiser.throwIfFalse(Validator.isNotNull(xmlData), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row");
		List<Node> nodes = xpath.selectNodes(dataRoot);

		List<String> trueNodes = new ArrayList<String>();
		List<String> falseNodes = new ArrayList<String>();

		for (Node node : nodes)
		{
			String formreceivedid = XMLHelper.getTextValueOf(node, "@formreceivedid");
			ErrorRaiser.throwIfNull(formreceivedid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

			boolean checked = GetterUtil.getBoolean(XMLHelper.getTextValueOf(node, "@checked"), true);
			if (checked)
			{
				trueNodes.add(formreceivedid);
			}
			else
			{
				falseNodes.add(formreceivedid);
			}
		}
		if(!trueNodes.isEmpty())
		{
			String inClauseSQL = TeaserMgr.getInClauseSQLForString(trueNodes);
			_log.debug(new StringBuffer("Query: ").append(String.format(UPDATE_CHECK_LIST_FORMRECEIVED, "true", inClauseSQL)));
			PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_CHECK_LIST_FORMRECEIVED, "true", inClauseSQL));
		}
		if(!falseNodes.isEmpty())
		{
			String inClauseSQL = TeaserMgr.getInClauseSQLForString(falseNodes);
			_log.debug(new StringBuffer("Query: ").append(String.format(UPDATE_CHECK_LIST_FORMRECEIVED, "false", inClauseSQL)));
			PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_CHECK_LIST_FORMRECEIVED, "false", inClauseSQL));
		}

		return xmlData;

	}
	
	public String getFilesFromFormReceived(String formReceivedId) throws NoSuchMethodException, SecurityException, ServiceError{
		String result = "";

		_log.trace(new StringBuffer("Into getFilesFromFormReceived"));
		ErrorRaiser.throwIfFalse(Validator.isNotNull(formReceivedId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		_log.trace("obtain files from formreceived");
		_log.debug(new StringBuffer("Query: ").append(String.format(GET_FILES_FORMRECEIVED, formReceivedId)));
		Document dom = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_FILES_FORMRECEIVED, formReceivedId));
		ErrorRaiser.throwIfNull(dom);

		Element rootElement = dom.getRootElement();
		rootElement.addAttribute("id", formReceivedId);
		
		_log.debug("files from formreceived: " + dom.asXML());
		result = dom.asXML();
		return result;
	}
	
	public String deleteFormsReceivedFromForm(String xmlData) throws ServiceError, DocumentException, IOException, SQLException
	{
		_log.trace(new StringBuffer("Into deleteFormsReceivedFromForm"));
		ErrorRaiser.throwIfFalse(Validator.isNotNull(xmlData), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		List<Node> nodes = dataRoot.selectNodes("//row/@formid");
		ErrorRaiser.throwIfFalse(nodes != null && !nodes.isEmpty(),  IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		String inClauseSQL = TeaserMgr.getInClauseSQL(nodes);

		_log.debug(new StringBuffer("Query: ").append(String.format(DELETE_FORMS_RECEIVED_FROM_FORM, inClauseSQL)));
		PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_FORMS_RECEIVED_FROM_FORM, inClauseSQL));

		return xmlData;
	}

	public String deleteFormsReceived(String xmlData) throws ServiceError, DocumentException, IOException, SQLException
	{
		_log.trace(new StringBuffer("Into deleteFormsReceived"));
		ErrorRaiser.throwIfFalse(Validator.isNotNull(xmlData), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		List<Node> nodes = dataRoot.selectNodes("//row/@formreceivedid");
		ErrorRaiser.throwIfFalse(nodes != null && !nodes.isEmpty(),  IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		String inClauseSQL = TeaserMgr.getInClauseSQL(nodes);

		_log.debug(new StringBuffer("Query: ").append(String.format(DELETE_FORMS_RECEIVED, inClauseSQL)));
		PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_FORMS_RECEIVED, inClauseSQL));

		return xmlData;
	}
	
	/**
	 * Incorpora en un input control las ocurrencias de valores que hay en la base de datos para ese listado, ya que el InputCtrl ha podido 
	 */
	public String getInputCtrl(String formfieldid) throws ServiceError, NoSuchMethodException, SecurityException, DocumentException
	{
		_log.trace(new StringBuffer("Into getInputCtrl"));
		ErrorRaiser.throwIfFalse(Validator.isNotNull(formfieldid), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		_log.trace("obtain inputCtrl from formfieldid");
		_log.debug(new StringBuffer("Query: ").append(String.format(GET_INPUTCTRL, formfieldid)));
		Document domFormType = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_FORMTYPE, formfieldid));
		ErrorRaiser.throwIfNull(domFormType, IterErrorKeys.XYZ_FORM_NOTFOUND_ZYX);
		String formType = domFormType.getRootElement().element("row").attributeValue("formtype");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(formType) && (Validator.equals(formType, "generico") || Validator.equals(formType, "registro")), IterErrorKeys.XYZ_FORM_NOTFOUND_ZYX);
		
		_log.trace("obtain inputCtrl from formfieldid");
		_log.debug(new StringBuffer("Query: ").append(String.format(GET_INPUTCTRL, formfieldid)));
		Document dom = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_INPUTCTRL, formfieldid), new String[] { "defaultvalue", "inputctrl", });
		ErrorRaiser.throwIfNull(dom);
		Element inputCrtl = formatInputCrtl(SAXReaderUtil.createXPath("/rs/row").selectSingleNode(dom));
		
		String query;
		if(Validator.equals(formType, "generico"))
		{
			 query = String.format(GET_POSSIBLE_OCURRENCE_GENERIC, formfieldid);
		}
		else
		{
			query = String.format(GET_POSSIBLE_OCURRENCE_USER, formfieldid);
		}
		
	
		_log.debug(new StringBuffer("Query2 ").append(query));
		List<Object> valores = PortalLocalServiceUtil.executeQueryAsList(query);
	 	Element options = inputCrtl.element("options");
	 	for (Element option : options.elements("option"))
		{
			if(Validator.isNotNull(option.attributeValue("value")))
			{
				option.attribute("label").setValue(option.attributeValue("value"));
			}
			else //if(Validator.isNotNull(option.attribute("label")))
			{
				option.attribute("value").setValue(option.attributeValue("label"));
			}
//			else{option.detach();}
		}
	 	String XPATH = "option[@value ='%s']";
		if(Validator.isNotNull(options)){
			for (Object object : valores)
			{
				if(SAXReaderUtil.createXPath(String.format(XPATH, (String)object)).selectNodes(options).size() == 0)
				{
					Element option = options.addElement("option");
					option.addAttribute("value", (String) object);
					option.addAttribute("label", (String) object);
				}
			}
		}
		return inputCrtl.asXML();
	}

	public void getFormReceivedDetail(HttpServletRequest request, HttpServletResponse response, String formid, String formreceivedid) throws Exception 
	{
		_log.trace(new StringBuffer("Into getDetailsFormReceived"));    
		ErrorRaiser.throwIfFalse(Validator.isNotNull(formid), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse(Validator.isNotNull(formreceivedid), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		//obtenemos las cabeceras
		Document metadatas = getMetadata(formid); 
		ErrorRaiser.throwIfFalse(Validator.isNotNull(metadatas), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);//formid incorrecto
		
		//obtenemos los datos
		String predicatesDetail = " formreceived.formreceivedid  = '"+ formreceivedid +"' AND ";
				
		Document dom = getGridValues(formid, predicatesDetail, metadatas);
				
		ErrorRaiser.throwIfNull(dom, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Element rowElem = ((Element) dom.getRootElement()).element("row");
		
		//obtenemos los inputs
		_log.debug(new StringBuffer("Query: ").append(String.format(GET_LABELS_INPUT, formid)));
		Document domInputs = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_LABELS_INPUT, formid));
		ErrorRaiser.throwIfNull(domInputs);
				
		// Creamos el xml
		Document xml = SAXReaderUtil.read("<formdata/>");
		Element formData = xml.getRootElement();					
		formData.addAttribute("formid", formid);
		formData.addAttribute("formname", domInputs.valueOf("/rs/row/@formname"));
		
		List<Element> metaTabs = metadatas.getRootElement().elements("tab");
		for (Element metaTabElem : metaTabs)
		{
			String tabname = metaTabElem.attributeValue("name");

			Element fieldsGroup = formData.addElement("fieldsgroup");
			fieldsGroup.addAttribute("name", tabname);
			
			List<Element> metaFields = metaTabElem.elements("column");
			for(Element metaField : metaFields)
			{
				String fieldid = metaField.attributeValue("id");
				String fieldL = metaField.attributeValue("l");
				String type = metaField.attributeValue("fieldtype");
				
				// Se añade el field correspondiente
				Element field = fieldsGroup.addElement("field");
				field.addAttribute("id", fieldid);
				field.addAttribute("fieldtype",  type);

				Node fieldDetails = domInputs.selectSingleNode(String.format(XPATH_INPUTFIELDID,fieldid));
				ErrorRaiser.throwIfNull(fieldDetails, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
				
				String nameValue = fieldDetails.valueOf("@name");
				
				if (Validator.isNotNull(nameValue)){
					Element labelBefore = field.addElement("labelbefore");
					labelBefore.addCDATA(nameValue);	
				}	

				List<Node> fieldsValue = rowElem.selectNodes(fieldL);
					
				Element data = field.addElement("data");
				if(type.equals("binary")){
					for (Node fieldValue : fieldsValue)
					{
						String value = fieldValue.getText();
						
						if(Validator.isNotNull(value)){
							Element binaryElem = data.addElement("binary");
							Element nameElem = binaryElem.addElement("name");
							nameElem.addText(value);
							Element binlocatorElem = binaryElem.addElement("binlocator");
							binlocatorElem.addAttribute("type", "url");
							URL url = FileFormReceivedMgrLocalServiceImpl.getURLFileReceived(formreceivedid, fieldid, request);
							binlocatorElem.addText(url.toString());
						}
					}
				}else{
					for (Node fieldValue : fieldsValue)
					{
						String value = fieldValue.getText();
						if(Validator.isNotNull(value)){
							Element valueElem = data.addElement("value");
							valueElem.addText(value);
						}
					}
				}
				
			}
		}

		_log.trace("Tranform XML with xslt");
		
		File webappsFile = new File(PortalUtil.getPortalWebDir()).getParentFile();
		
		// Ruta donde esta la xsl
		String xslPath = new StringBuffer(File.separator)
							.append("user-portlet")
							.append(File.separator)
							.append("xsl")
							.append(File.separator)
							.append("iterFormDetail.xsl").toString();
		
		String xslRoute =  new StringBuffer(webappsFile.getAbsolutePath()).append(xslPath).toString();
		File xslFile = new File (xslRoute);
		
		// Comprobamos que la xsl existe y se puede leer
		ErrorRaiser.throwIfFalse(xslFile.exists() && xslFile.canRead(), IterErrorKeys.XYZ_E_IMPORT_XSL_UNAVAILABLE_ZYX);
			
		InputStream xslIS = new FileInputStream(xslFile);
		ErrorRaiser.throwIfNull(xslIS);
		
		Element dataRoot = SAXReaderUtil.read(xslIS).getRootElement();
		ErrorRaiser.throwIfNull(dataRoot, IterErrorKeys.XYZ_E_IMPORT_XSL_UNAVAILABLE_ZYX);
			
		String typeXSLT = XMLHelper.getTextValueOf(dataRoot, "/xsl:stylesheet/xsl:output/@method", null);
		if(Validator.isNull(typeXSLT)){
			_log.trace("XSL not contain 'xsl:output/@method'. default: xml");
			typeXSLT = "xml";
		}
		// Transformamos
		String transformed = XSLUtil.transform(XSLUtil.getSource(xml.asXML()), XSLUtil.getSource(dataRoot.asXML()), typeXSLT);

		ServletOutputStream out                   = null;
		ByteArrayInputStream byteArrayInputStream = null;
		BufferedOutputStream bufferedOutputStream = null;
		
		try{
			// Especificamos que la respuesta 
			response.setHeader("Content-Type", "text/html; charset=UTF-8");
			//response.setHeader("Content-Disposition", disposition);
			// Esta linea dara error si se llama desde un jsp
			out = response.getOutputStream();

			// Escribimos el contenido ya formado
			out.write(transformed.getBytes());		
			out.flush();
			out.close();
		} catch (Exception e) {
			_log.error(new StringBuffer("Error while sending transformed html detail: ").append(e.getStackTrace()));
			throw e;
		} finally {
			if (out != null){
				out.close();
			}
			if (byteArrayInputStream != null) {
				byteArrayInputStream.close();
			}
			if (bufferedOutputStream != null) {
				bufferedOutputStream.close();
			}
		}
	}

	public void exportFormsToCSV(	HttpServletRequest request, HttpServletResponse response, String xmlFiltersOrder, String nameForDateSentColumn, String nameForUserColum	) throws Exception
	{
		_log.trace("in generateCSVTranslated function");
		ErrorRaiser.throwIfNull(request, 									IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(response, 									IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse(Validator.isNotNull(xmlFiltersOrder),IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse(Validator.isNotNull(nameForDateSentColumn),IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse(Validator.isNotNull(nameForUserColum), 	IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		Document domFetch = getGenericReceivedDataForm(xmlFiltersOrder);
		
		Element metaData = domFetch.getRootElement().element("columns");
		ErrorRaiser.throwIfNull(metaData, "domFetch metaData is null");

		String formId = metaData.attributeValue("formid");
		
		List<Node> nodes = domFetch.selectNodes("/form/rs/row/@formreceivedid");
		ErrorRaiser.throwIfFalse(nodes != null && !nodes.isEmpty(),  IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		String inClauseSQL = TeaserMgr.getInClauseSQL(nodes);

		String predicateFixed = String.format(PREDICATE_FORMRECEIVEDS_VALUES, inClauseSQL);
		
		Document gridData = generateXMLGridValues(formId, predicateFixed);
		
		ErrorRaiser.throwIfNull(gridData, "gridData are null");
		
		Element gridElement = gridData.getRootElement(); 
		
		
		List<Element> tabs = metaData.elements("tab");
		List<Element> tabsColumnsMetaData = new ArrayList<Element>();
		for (Element tab : tabs)
		{
			tabsColumnsMetaData.addAll(tab.elements("column"));
		}
		
		List<Element> rows = gridElement.element("rs").elements("row");
		ErrorRaiser.throwIfNull(rows, "gridData rows are null");		
		
		
		// Se comienza a crear el csv
		List< List<Object> > csvValues = new ArrayList< List<Object> >();
		List<Object> headers 		= new ArrayList< Object >();
		
		headers.add(nameForDateSentColumn);
		headers.add(nameForUserColum);
		for (Element column : tabsColumnsMetaData)
		{
			headers.add(column.attributeValue("name"));
		}
		csvValues.add(headers);

		// Cada filas de datos
		for (Element rowData : rows)
		{
			List<Object> csvRow = new ArrayList< Object >();
			
			String datesent = rowData.attributeValue("datesent");
			ErrorRaiser.throwIfNull(datesent);	
			java.util.Date date = SDF_WITH_MLS.parse(datesent);
			csvRow.add(SDF_CSV.format(date));

			String usrname = GetterUtil.getString(rowData.attributeValue("usrname"), "");
			csvRow.add(usrname);
			
			// Cada celda o columna de datos de esa fila
			for (Element columnMetaData : tabsColumnsMetaData)
			{
				String xpath 			= columnMetaData.attributeValue("l");
//				String xpath 			= String.format("i%s", columnMetaData.attributeValue("id"));
				List<Node> columnsData 	= rowData.selectNodes( xpath );
				List<Object> csvCell 	= new ArrayList<Object>();
				
				// Cada línea dentro de la celdao columna de datos
				for (Node columnData : columnsData)
				{
					String text = GetterUtil.getString(columnData.getText(), StringPool.BLANK);
					csvCell.add( Validator.isNotNull(text) ? text.replace(StringPool.QUOTE, StringPool.DOUBLE_QUOTE) : text );
				}
				csvRow.add(csvCell);
			}
			csvValues.add(csvRow);
		}		
		
		String csvValue = CSVUtil.getCSV(csvValues);
		CSVUtil.writeCSV(response, csvValue);
	}
	
	@Override
	public Document getGridValues(String xmlData) throws NoSuchMethodException, SecurityException, ServiceError, DocumentException
	{
		_log.trace(new StringBuffer("Get dataGridValues"));
		ErrorRaiser.throwIfFalse(Validator.isNotNull(xmlData), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		String formid = dataRoot.attributeValue("id");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(formid), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		List<Node> nodes = dataRoot.selectNodes("//row/@id");
		ErrorRaiser.throwIfFalse(nodes != null && !nodes.isEmpty(),  IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		String inClauseSQL = TeaserMgr.getInClauseSQL(nodes);

		String predicateFixed = String.format(PREDICATE_FORMRECEIVEDS_VALUES, inClauseSQL);
		return generateXMLGridValues(formid, predicateFixed);
		
	}
	
	@Override
	public Document getGenericReceivedDataForm(String xmlOrderFilterForm) throws NoSuchMethodException, SecurityException, ServiceError, DocumentException
	{
		_log.trace(new StringBuffer("Into getGenericReceivedDataForm"));
		ErrorRaiser.throwIfFalse(Validator.isNotNull(xmlOrderFilterForm), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		Document domXMLOrder = SAXReaderUtil.read(xmlOrderFilterForm);

		String formid = XMLHelper.getTextValueOf(domXMLOrder, "/rs/@id");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(formid), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		Node orderNode = domXMLOrder.selectSingleNode("/rs/order");
		Node filtersNode = domXMLOrder.selectSingleNode("/rs/filters");
		if (Validator.isNull(orderNode) || Validator.isNull(XMLHelper.getTextValueOf(orderNode, "@columnid", null))){
			Element orderElement = (Validator.isNull(orderNode)) ? domXMLOrder.addElement("order") :  (Element) orderNode;
			orderElement.addAttribute("columnid", "datesent");
			orderElement.addAttribute("asc", "0");
			orderElement.addAttribute("type", "");
		}
		
		if (Validator.isNotNull(orderNode) && Validator.isNotNull(XMLHelper.getTextValueOf(orderNode, "@columnid", null)) && Validator.isNotNull(filtersNode) && filtersNode.selectNodes("//filter").size() > 0)
		{
			return getReceivedDataFormFilterOrder(formid, orderNode, filtersNode);
		}
		else if (Validator.isNotNull(orderNode) && Validator.isNotNull(XMLHelper.getTextValueOf(orderNode, "@columnid", null)))
		{
			return getReceivedDatatFormOrder(formid, orderNode);
		}
		else if (Validator.isNotNull(filtersNode) && filtersNode.selectNodes("//filters/filter").size() > 0)
		{
			return getReceivedDataFormFilter(formid, filtersNode);
		}
		else
		{
			return getReceivedDataForm(formid);
		}
	}

	@Override
	public Document getReceivedDataForm(String formid) throws NoSuchMethodException, SecurityException, ServiceError, DocumentException
	{
		_log.trace(new StringBuffer("Into getFormValues"));
		ErrorRaiser.throwIfFalse(Validator.isNotNull(formid), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		return generateXMLGrid(formid , "" , "", "datesent" , "DESC", null, null);
	}

	private Document getReceivedDataFormFilterOrder(String formid, Node orderNode, Node filtersNode) throws NoSuchMethodException, SecurityException, ServiceError, DocumentException
	{
		List<Node> nodesFilter = filtersNode.selectNodes("filter");
		List<String> predicates = getSQLFilterPredicates(nodesFilter);

		String fieldOrderid = XMLHelper.getTextValueOf(orderNode, "@columnid");
		Boolean asc = GetterUtil.getBoolean(XMLHelper.getTextValueOf(orderNode, "@asc"));

		String sortField = fieldOrderid;
		
		String sortOrder = (asc) ? "ASC" : "DESC";

		return generateXMLGrid(formid, predicates.get(0),predicates.get(1), sortField, sortOrder, orderNode, filtersNode);
	}

	private Document getReceivedDatatFormOrder(String formid, Node orderNode) throws ServiceError, NoSuchMethodException, SecurityException, DocumentException
	{
		String fieldOrderid = XMLHelper.getTextValueOf(orderNode, "@columnid");
		Boolean asc = GetterUtil.getBoolean(XMLHelper.getTextValueOf(orderNode, "@asc"));
		
		String sortField = fieldOrderid;  
						
		String sortOrder = (asc) ? "ASC" : "DESC";
						
		return generateXMLGrid(formid, "", "", sortField, sortOrder, orderNode, null);
	}

	private Document getReceivedDataFormFilter(String formid, Node filtersNode) throws ServiceError, NoSuchMethodException, SecurityException, DocumentException
	{
		List<Node> nodesFilter = filtersNode.selectNodes("filter");
		List<String> predicates = getSQLFilterPredicates(nodesFilter);

		return generateXMLGrid(formid, predicates.get(0),predicates.get(1), "", "", null, filtersNode);
	}

	/**
	 * List[0]  vars
	 * List[1]  fix
	 * @param nodesFilter
	 * @return
	 * @throws ServiceError 
	 */
	private List<String> getSQLFilterPredicates(List<Node> nodesFilter) throws ServiceError
	{
		List<String> result = new ArrayList<String>();
		StringBuffer sqlPredicatesVars = new StringBuffer();
		StringBuffer sqlPredicatesFix = new StringBuffer();
		
		for (Node nodeFilter : nodesFilter)
		{
			StringBuffer predicate = new StringBuffer();

			String fieldFilterId = XMLHelper.getTextValueOf(nodeFilter, "@columnid");
			String operator = XMLHelper.getTextValueOf(nodeFilter, "@operator");
			String type = XMLHelper.getTextValueOf(nodeFilter, "@fieldtype");
			String inputctrltype = XMLHelper.getTextValueOf(nodeFilter, "@inputctrltype");
			List<Node> nodesValue = nodeFilter.selectNodes("values/value");
			
			ErrorRaiser.throwIfFalse(Validator.isNotNull(fieldFilterId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			ErrorRaiser.throwIfFalse(Validator.isNotNull(operator), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			ErrorRaiser.throwIfFalse(Validator.isNotNull(type), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			ErrorRaiser.throwIfFalse(Validator.isNotNull(inputctrltype), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			if (type.equals("array"))
			{
				StringBuffer values = new StringBuffer();
				for (Node nodeValue : nodesValue)
				{
					if (values.length() > 0)
					{
						values.append(", ");
					}
					String nodeValueText = StringEscapeUtils.escapeSql(nodeValue.getText());   
					values.append("'").append(nodeValueText).append("'");
				}
				String predicado = getPredicate(fieldFilterId, operator, type, inputctrltype, values.toString());
				predicate.append(predicado);
			}
			else
			{
				for (Node nodeValue : nodesValue)
				{
					if (predicate.length() > 0)
					{
						predicate.append(" OR ");
					}

					String value = StringEscapeUtils.escapeSql(nodeValue.getText());
					String predicado = getPredicate(fieldFilterId, operator, type, inputctrltype, value);
					predicate.append(predicado);
				}
			}
							
			if (fieldFijos.contains(fieldFilterId)){
				sqlPredicatesFix.append(" ( ");
				sqlPredicatesFix.append(predicate.toString());
				sqlPredicatesFix.append(" ) \n");
				sqlPredicatesFix.append(" AND ");
			}else{
				sqlPredicatesVars.append(" ( ");
				sqlPredicatesVars.append(predicate.toString());
				sqlPredicatesVars.append(" ) \n");
				sqlPredicatesVars.append(" AND ");
			}
		}

		result.add(sqlPredicatesVars.toString());
		result.add(sqlPredicatesFix.toString());
		return result;
	}
	
	private String getPredicate(String fieldFilterId, String operator, String type, String inputctrltype, String value) throws ServiceError
	{
		String predicate;
		if (fieldFijos.contains(fieldFilterId))
		{// campos fijos
			if(Validator.equals(fieldFilterId, "datesent"))
			{
				predicate =  getSpecialDateOperatorPredicate("datesent", operator, type, value);
			}
			else
			{
				if (fieldFilterId.equals("usrname"))
				{//puede ser nula
					predicate = newPredicate("IFNULL(usrname, '')", operator, type, value);
				}
				else
				{
					predicate = newPredicate(fieldFilterId, operator, type, value);
				}
			}
		}
		else
		{// campos variables
			
			if(Validator.equals(type,"date") || Validator.equals(type,"int") || Validator.equals(type,"number") || Validator.equals(type,"boolean")) 	type = "string";//TODO deshacer paso temporal para tratar datos como cadenas de texto hasta futuro formateo de datos

			//obtenemos el operador
			String booleanOperator = " < ";
			if (operator.equals("notcontain") ){
				operator = "contain";
				booleanOperator = " = ";
			}
			else if( operator.equals("distinct") ){
				operator = "equals";
				booleanOperator = " = ";
			}

			if (type.equals("binary"))
			{//columna de dlfileentry
				String operatorPredicate = newPredicate(TABLE_COLUMN_BINROW_VALUE, operator, type, value);
				predicate = (String.format(PREDICATE_FILTER_BINROW, booleanOperator, fieldFilterId, operatorPredicate));
			}
			else
			{//columna de fieldreceived
				String operatorPredicate = newPredicate(TABLE_COLUMN_ROW_VALUE, operator, type, value);
				predicate = (String.format(PREDICATE_FILTER_ROW, booleanOperator, fieldFilterId, operatorPredicate));
			}
		}
		return predicate;
	}
	
	private String newPredicate(String columnname, String operator, String type, String columnvalue)
	{
		String key = new StringBuffer().append(type).append("_").append(operator).toString();
		String predicateOperator = getMapOperators().get(key);
		predicateOperator = predicateOperator.replace("TABLE_COLUMNNAME", columnname);
		predicateOperator = predicateOperator.replace("COLUMNVALUE", columnvalue);
		return predicateOperator;
	}

	private String getSpecialDateOperatorPredicate(String table_columnname, String operator, String type, String columnvalue) throws ServiceError
	{
		if(operator.equals("equals"))
		{
			StringBuffer preValue = new StringBuffer(columnvalue);
			StringBuffer postValue = new StringBuffer(columnvalue);
			switch(columnvalue.length())
			{
				case 8:
					preValue.append("00");//hor
					postValue.append("23");//hor
				case 10:
					preValue.append("00");//min
					postValue.append("59");//min
				case 12:
					preValue.append("00");//seg
					preValue.append("000");//miliseg
					postValue.append("59");//seg
					postValue.append("999");//miliseg
					break;
			}
			return new StringBuffer("(")
					.append(newPredicate(table_columnname, "fromdate", type, preValue.toString()))
					.append(" AND ")
					.append(newPredicate(table_columnname, "todate", type, postValue.toString()))
					.append(")")
					.toString();
		}
		else if(operator.equals("beforedate") || operator.equals("fromdate"))
		{
			StringBuffer dateValue = new StringBuffer(columnvalue);
			switch(columnvalue.length())
			{
				case 8:
					dateValue.append("00");//hor
				case 10:
					dateValue.append("00");//min
				case 12:
					dateValue.append("00");//seg
					dateValue.append("000");//miliseg
					break;
			}
			return newPredicate(table_columnname, operator, type, dateValue.toString());
		}else if(operator.equals("afterdate") || operator.equals("todate"))
		{
			StringBuffer dateValue = new StringBuffer(columnvalue);
			switch(columnvalue.length()){
				case 8:
					dateValue.append("23");//hor
				case 10:
					dateValue.append("59");//min
				case 12:
					dateValue.append("59");//seg
					dateValue.append("999");//miliseg
					break;
			}
			return newPredicate(table_columnname, operator, type, dateValue.toString());
		}
		ErrorRaiser.throwIfFalse(false, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return null;
	}

	private Map<String, String> getMapOperators()
	{
		if (operadores == null)
		{// types string|boolean|int|number|date|binary|array
			_log.trace(new StringBuffer("Create Map Operators"));
			operadores = new HashMap<String, String>();
			operadores.put("string_contain", 	" TABLE_COLUMNNAME like '%COLUMNVALUE%' ");
			operadores.put("string_notcontain", " TABLE_COLUMNNAME not like '%COLUMNVALUE%' ");
			operadores.put("string_equals", 	" TABLE_COLUMNNAME = 'COLUMNVALUE' ");
			operadores.put("string_distinct", 	" TABLE_COLUMNNAME <> 'COLUMNVALUE' ");
			operadores.put("string_startBy", 	" TABLE_COLUMNNAME like 'COLUMNVALUE%' ");
			operadores.put("string_endBy", 		" TABLE_COLUMNNAME like '%COLUMNVALUE' ");

			operadores.put("boolean_equals", 	" TABLE_COLUMNNAME =  'COLUMNVALUE' ");
			operadores.put("boolean_distinct", 	" TABLE_COLUMNNAME <> 'COLUMNVALUE' ");

			operadores.put("int_equals", 		" TRUNCATE(TABLE_COLUMNNAME,0) = TRUNCATE('COLUMNVALUE', 0) ");
			operadores.put("int_smaller", 		" TRUNCATE(TABLE_COLUMNNAME,0) < TRUNCATE('COLUMNVALUE', 0) ");
			operadores.put("int_greater", 		" TRUNCATE(TABLE_COLUMNNAME,0) > TRUNCATE('COLUMNVALUE', 0) ");

			operadores.put("number_equals", 	" CAST( TABLE_COLUMNNAME AS DECIMAL(65, 30) ) = CAST( 'COLUMNVALUE' AS DECIMAL(65, 30) ) ");
			operadores.put("number_smaller", 	" CAST( TABLE_COLUMNNAME AS DECIMAL(65, 30) ) < CAST( 'COLUMNVALUE' AS DECIMAL(65, 30) ) ");
			operadores.put("number_greater", 	" CAST( TABLE_COLUMNNAME AS DECIMAL(65, 30) ) > CAST( 'COLUMNVALUE' AS DECIMAL(65, 30) ) ");

			operadores.put("date_equals", 		" STR_TO_DATE(TABLE_COLUMNNAME, '%Y%m%d%H%i%s%f') =  STR_TO_DATE('COLUMNVALUE', '%Y%m%d%H%i%s%f') ");
			operadores.put("date_beforedate", 	" STR_TO_DATE(TABLE_COLUMNNAME, '%Y%m%d%H%i%s%f') <  STR_TO_DATE('COLUMNVALUE', '%Y%m%d%H%i%s%f') ");
			operadores.put("date_afterdate", 	" STR_TO_DATE(TABLE_COLUMNNAME, '%Y%m%d%H%i%s%f') >  STR_TO_DATE('COLUMNVALUE', '%Y%m%d%H%i%s%f') ");
			operadores.put("date_fromdate", 	" STR_TO_DATE(TABLE_COLUMNNAME, '%Y%m%d%H%i%s%f') >=  STR_TO_DATE('COLUMNVALUE', '%Y%m%d%H%i%s%f') ");
			operadores.put("date_todate", 		" STR_TO_DATE(TABLE_COLUMNNAME, '%Y%m%d%H%i%s%f') <=  STR_TO_DATE('COLUMNVALUE', '%Y%m%d%H%i%s%f') ");

			operadores.put("binary_equals", 	" TABLE_COLUMNNAME = 'COLUMNVALUE' ");
			operadores.put("binary_distinct", 	" TABLE_COLUMNNAME <> 'COLUMNVALUE' ");
			operadores.put("binary_startBy", 	" TABLE_COLUMNNAME like 'COLUMNVALUE%' ");
			operadores.put("binary_endBy", 		" TABLE_COLUMNNAME like '%COLUMNVALUE' ");
			operadores.put("binary_contain", 	" TABLE_COLUMNNAME like '%COLUMNVALUE%' ");
			operadores.put("binary_notcontain", " TABLE_COLUMNNAME not like '%COLUMNVALUE%' ");

			operadores.put("array_contain", 	" TABLE_COLUMNNAME in (COLUMNVALUE) ");
			operadores.put("array_notcontain",	" TABLE_COLUMNNAME not in (COLUMNVALUE) ");
		}
		return operadores;
	}

	private Document getMetadata(String formid) throws NoSuchMethodException, SecurityException, ServiceError, DocumentException
	{
		long t1 = Calendar.getInstance().getTimeInMillis();
		_log.trace(new StringBuffer("Into getMetadata"));
		ErrorRaiser.throwIfFalse(Validator.isNotNull(formid), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		_log.trace("obtain metadata grid");
		_log.debug(new StringBuffer("Query: ").append(String.format(GET_METADATA, formid)).toString());
		Document dom = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_METADATA, formid));
		ErrorRaiser.throwIfNull(dom);

		// Obtenemos el elemento root
		XPath xpath = SAXReaderUtil.createXPath("/rs/row");
		List<Node> nodes = xpath.selectNodes(dom);

		// Obtenemos el nodo padre del resultado
		Document xml = SAXReaderUtil.read("<columns/>");
		Element nodeRoot = xml.getRootElement();
		nodeRoot.addAttribute("formid", formid);
		nodeRoot.addAttribute("checked", "checked");
		nodeRoot.addAttribute("datesent", "datesent");
		nodeRoot.addAttribute("usrname", "usrname");

		String tabIdBefore = null;
		Element tab = null;
		// Recorremos los datos
		for (int i = 0; i < nodes.size(); i++)
		{
			Node nodeRow = nodes.get(i);
			final String tabId = XMLHelper.getTextValueOf(nodeRow, "@tabid");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(tabId));

			final String tabName = XMLHelper.getTextValueOf(nodeRow, "@tabname");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(tabName));

			// El tab es nuevo
			if (!Validator.equals(tabId, tabIdBefore))
			{
				tab = nodeRoot.addElement("tab");
				tab.addAttribute("id", tabId);
				tab.addAttribute("name", tabName);
				tabIdBefore = tabId;
			}

			// Formateamos la columna
			Element column = (Element) nodeRow.detach();
			column.setName("column");
			column.addAttribute("l", "i"+(i+1));
			column.remove(column.attribute("tabid"));
			column.remove(column.attribute("tabname"));

			// Anyadimos la columa al nodo tab
			tab.add(column);
		}
		
		long t2 = Calendar.getInstance().getTimeInMillis();
		_log.debug(new StringBuffer("getMetadata: ").append(t2-t1).append(" ms").toString());

		return xml;
	}
	
	private Document generateXMLGrid(String formid , String predicatesVars ,String predicateFix, String sortField , String sortOrder, Node orderNode, Node filtersNode) throws NoSuchMethodException, SecurityException, ServiceError, DocumentException//(String formid, String sqlQuery, Node orderNode, Node filtersNode) throws NoSuchMethodException, SecurityException, ServiceError, DocumentException
	{
		// Midiendo el rendimiento
		Calendar ahoraSQL = Calendar.getInstance();
		long tiempoSQL = ahoraSQL.getTimeInMillis();
		// Midiendo el rendimiento

		Document domMetadatas = getMetadata(formid);

		String adaptedSortField = sortField;
		
		
		Document dom = getGrid(formid, predicatesVars, predicateFix, adaptedSortField, sortOrder, domMetadatas);
		
		long t1 = Calendar.getInstance().getTimeInMillis(); 
		Document xml = SAXReaderUtil.read("<form/>");
		Element nodeRoot = xml.getRootElement();
		nodeRoot.add(domMetadatas.getRootElement());
		nodeRoot.add(dom.getRootElement());

		if (Validator.isNull(orderNode))
		{
			nodeRoot.add(SAXReaderUtil.read("<order/>").getRootElement());
		}
		else
		{
			nodeRoot.add(SAXReaderUtil.read(orderNode.asXML()).getRootElement());
		}

		if (Validator.isNull(filtersNode))
		{
			nodeRoot.add(SAXReaderUtil.read("<filters/>").getRootElement());
		}
		else
		{
			nodeRoot.add(SAXReaderUtil.read(filtersNode.asXML()).getRootElement());
		}
		long t2 = Calendar.getInstance().getTimeInMillis();
		_log.debug(new StringBuffer("PostProceso XML generateXMLGridValues: ").append(t2-t1).append(" ms").toString());
		
		// Midiendo el rendimiento
		Calendar ahora2SQL = Calendar.getInstance();
		long tiempoSQL2 = ahora2SQL.getTimeInMillis();
		Long difSQL = tiempoSQL2 - tiempoSQL;  
		_log.debug(new StringBuffer("Total generateXMLGridValues: ").append(difSQL).append(" ms").toString());
		// Midiendo el rendimiento
		return xml;
	}
	
	private Document getGrid(String formid , String predicatesVars ,String predicateFix, String sortField , String sortOrder, Document domMetadatas) throws NoSuchMethodException, SecurityException, ServiceError
	{
		long t1 = Calendar.getInstance().getTimeInMillis();
		
		if(Validator.isNull(sortField))
		{
			sortField = "datesent";
			sortOrder = "DESC";
		}
		if(sortOrder == null) sortOrder = "";
						
		String predicates = new StringBuffer(predicateFix).append(" ").append(predicatesVars).toString();
		
		_log.trace("obtain data for grid");
		String sqlQuery = String.format(GET_QUERY_GRID_FETCH, formid, predicates, sortField, sortOrder);
		_log.debug(new StringBuffer("Query: ").append(sqlQuery));
		
		long t3 = Calendar.getInstance().getTimeInMillis();
		Document d = getAndExecuteQuery(sqlQuery, XMLHelper.colAsAttr, XMLHelper.rsTagName, XMLHelper.rowTagName, null);
		long t4 = Calendar.getInstance().getTimeInMillis();
		_log.debug(new StringBuffer("getGridValues executeQueryAsDom: ").append(t4-t3).append(" ms").toString());
		
		long t2 = Calendar.getInstance().getTimeInMillis();
		_log.debug(new StringBuffer("total getGridValues: ").append(t2-t1).append(" ms").toString());
		
		return d;
	}

	private Element formatInputCrtl(Node node) throws DocumentException, ServiceError
	{
		XPath xpath = SAXReaderUtil.createXPath("inputctrl");
		Node inputctrlNode = xpath.selectSingleNode(node);
		Element iCtrl = SAXReaderUtil.read(CDATAUtil.strip(inputctrlNode.getText())).getRootElement();

		Element inputCtrl = SAXReaderUtil.createElement("inputctrl");

		String inputCtrlName = XMLHelper.getTextValueOf(node, "@htmlname");
		if (Validator.isNull(inputCtrlName))
		{
			inputCtrlName = XMLHelper.getTextValueOf(node, "@fieldid");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(inputCtrlName), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}
		
		String fieldid = XMLHelper.getTextValueOf(node, "@fieldid");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(fieldid), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		String iControlType = iCtrl.attributeValue("type");

		inputCtrl.addAttribute("name", inputCtrlName);
		inputCtrl.addAttribute("id", fieldid);
		inputCtrl.addAttribute("type", iControlType);

		if (iControlType.equalsIgnoreCase("radiobutton") || iControlType.equalsIgnoreCase("listctrl") || iControlType.equalsIgnoreCase("dropdownlist") || iControlType.equalsIgnoreCase("checkbox"))
		{
			Element options = SAXReaderUtil.createElement("options");

			Element opts = iCtrl.element("options");
			if (Validator.isNotNull(opts))
			{
				List<Element> optList = opts.elements();

				if (iControlType.equalsIgnoreCase("listctrl") || iControlType.equalsIgnoreCase("dropdownlist"))
					options.addAttribute("multiple", opts.attributeValue("size"));

				options.addAttribute("multiple", opts.attributeValue("multiple", "false"));

				for (Element opt : optList)
				{
					Element option = SAXReaderUtil.createElement("option");
					option.addAttribute("value", opt.attributeValue("value"));
					option.addAttribute("label", opt.attributeValue("name"));
					options.add(option);
				}

				inputCtrl.add(options);
			}
		}

		return inputCtrl;
	}
	
	private Document generateXMLGridValues(String formid , String predicateFix) throws NoSuchMethodException, SecurityException, ServiceError, DocumentException//(String formid, String sqlQuery, Node orderNode, Node filtersNode) throws NoSuchMethodException, SecurityException, ServiceError, DocumentException
	{
		// Midiendo el rendimiento
		Calendar ahoraSQL = Calendar.getInstance();
		long tiempoSQL = ahoraSQL.getTimeInMillis();
		// Midiendo el rendimiento

		Document domMetadatas = getMetadata(formid);
		
		Document dom = getGridValues(formid, predicateFix, domMetadatas);
		
		long t1 = Calendar.getInstance().getTimeInMillis(); 
		Document xml = SAXReaderUtil.read("<form/>");
		Element nodeRoot = xml.getRootElement();
		nodeRoot.add(domMetadatas.getRootElement());
		nodeRoot.add(dom.getRootElement());

		nodeRoot.add(SAXReaderUtil.read("<order/>").getRootElement());
		nodeRoot.add(SAXReaderUtil.read("<filters/>").getRootElement());

		long t2 = Calendar.getInstance().getTimeInMillis();
		_log.debug(new StringBuffer("PostProceso XML generateXMLGridValues: ").append(t2-t1).append(" ms").toString());
		
		// Midiendo el rendimiento
		Calendar ahora2SQL = Calendar.getInstance();
		long tiempoSQL2 = ahora2SQL.getTimeInMillis();
		Long difSQL = tiempoSQL2 - tiempoSQL;  
		_log.debug(new StringBuffer("Total generateXMLGridValues: ").append(difSQL).append(" ms").toString());
		// Midiendo el rendimiento
		return xml;
	}
	
	private Document getGridValues(String formid , String predicatesFixed, Document domMetadatas) throws NoSuchMethodException, SecurityException, ServiceError
	{
		long t1 = Calendar.getInstance().getTimeInMillis();
		
		StringBuffer varColumnIds = new StringBuffer();
		List<String> listVarColumnsNames = new ArrayList<String>();
		
		XPath xpath = SAXReaderUtil.createXPath("/columns/tab/column");
		for (Node nodesColumn :  xpath.selectNodes(domMetadatas))
		{
			String fieldid = ((Element)nodesColumn).attributeValue("id");
			String label = ((Element)nodesColumn).attributeValue("l");
			
			if(varColumnIds.length() > 0)
			{
				varColumnIds.append(",");	
			}
			varColumnIds.append("'").append(fieldid).append("'");
			
			listVarColumnsNames.add(label);
		}
		String[] varColumnNames = new String[listVarColumnsNames.size()];
		listVarColumnsNames.toArray(varColumnNames);
		
		_log.trace("obtain data for grid");
		String sqlQuery = String.format(GET_QUERY_GRID_VALUES_FETCH_DISCRETE, formid , varColumnIds.toString() , predicatesFixed);
		
		long t3 = Calendar.getInstance().getTimeInMillis();
		Document d = getAndExecuteQuery(sqlQuery, XMLHelper.colAsAttr, XMLHelper.rsTagName, XMLHelper.rowTagName, varColumnNames);
		long t4 = Calendar.getInstance().getTimeInMillis();
		_log.debug(new StringBuffer("getGridValues executeQueryAsDom: ").append(t4-t3).append(" ms").toString());
		
		long t2 = Calendar.getInstance().getTimeInMillis();
		_log.debug(new StringBuffer("total getGridValues: ").append(t2-t1).append(" ms").toString());
		
		return d;
	}
	
	private Document getAndExecuteQuery(String sqlQuery, boolean colAsAttr, String rsTagName, String rowTagName, String[] varColumnNames) throws NoSuchMethodException, SecurityException, ServiceError{
		_log.debug(new StringBuffer("Query: ").append(sqlQuery));
		List<Object> sqlQueryResult = PortalLocalServiceUtil.executeQueryAsList(sqlQuery);
		ErrorRaiser.throwIfFalse(Validator.isNotNull(sqlQueryResult) && sqlQueryResult.size() > 0);
		String fetchQuery = (String) sqlQueryResult.get(0);
		ErrorRaiser.throwIfFalse(Validator.isNotNull(fetchQuery));
		_log.debug(new StringBuffer("Query: ").append(fetchQuery));
		Document dom;
		if(varColumnNames == null){
			dom = PortalLocalServiceUtil.executeQueryAsDom(fetchQuery, XMLHelper.colAsAttr, XMLHelper.rsTagName, XMLHelper.rowTagName);
		}else{
			dom = PortalLocalServiceUtil.executeQueryAsDom(fetchQuery, XMLHelper.colAsAttr, XMLHelper.rsTagName, XMLHelper.rowTagName, varColumnNames);
		}
		
		return dom;
	}
	
}
