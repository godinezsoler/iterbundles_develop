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
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.base.IterTabsLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.liferay.portal.kernel.xml.XSLUtil;

/**
 * The implementation of the iter tabs local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.IterTabsLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.IterTabsLocalServiceUtil} to access the iter tabs local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.IterTabsLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.IterTabsLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class IterTabsLocalServiceImpl extends IterTabsLocalServiceBaseImpl
{
	
	private Log _log = LogFactoryUtil.getLog(IterTabsLocalServiceImpl.class);
	
	private final String GET_TABS = "SELECT tabid, name as tabname, taborder FROM formtab WHERE formid='%s' ORDER BY taborder";
	private final String GET_TAB_BY_ID = "SELECT tabid, name as tabname, taborder FROM formtab WHERE tabid='%s'";
	
	private final String GET_TAB_FIELDS = new StringBuilder()
	.append(" SELECT fieldid, IF(userprofile.profilefieldid IS NULL, df.fieldtype, pdf.fieldtype) fieldtype,      \n")
	.append("     userprofile.profilefieldid, fieldname, fieldorder, labelbefore, labelafter, formfield.required, \n")
	.append("        IF(structured, 'system', 'user') fieldclass                                                  \n")
	.append(" FROM formfield                                                                                      \n")
	.append(" LEFT JOIN datafield df  ON df.datafieldid = formfield.datafieldid                                   \n")
	.append(" LEFT JOIN userprofile   ON formfield.profilefieldid = userprofile.profilefieldid                    \n")
	.append(" LEFT JOIN datafield pdf ON pdf.datafieldid = userprofile.datafieldid                                \n")
	.append(" WHERE formfield.tabid='%s'                                                                          \n")
	.append(" ORDER BY fieldorder ASC                                                                               ")
	.toString();

	private final String ADD_TAB = "INSERT INTO formtab(tabid, formid, name, taborder) SELECT '%s', '%s', '%s', IFNULL(MAX(taborder),0)+1 FROM formtab WHERE formid='%s'";
	private final String UPDATE_TAB = "UPDATE formtab SET name='%s' %s WHERE tabid='%s'";
	private final String DELETE_TABS = "DELETE FROM formtab WHERE tabid IN %s";
	private final String GET_UPDATED_TABS = "SELECT tabid, formid, name as tabname, taborder FROM formtab WHERE taborder>=%d AND taborder<=%d AND formid='%s' ORDER BY taborder";
	private final String UPDATE_TABS_PRIORITY = "UPDATE formtab SET taborder=taborder %s 1 WHERE formid='%s' AND tabid!='%s' AND taborder>=%d AND taborder<=%d";
		
	public String getTabs( String formId ) throws ServiceError, NoSuchMethodException
	{
		String result = "";
		
		ErrorRaiser.throwIfNull(formId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		result = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_TABS, formId) ).asXML();
		
		return result;
	}
	
	public String getTabFields( String tabId ) throws ServiceError, NoSuchMethodException
	{
		String result = "";
		
		ErrorRaiser.throwIfNull(tabId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document d = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_TAB_FIELDS, tabId), new String[]{"labelbefore", "labelafter"} );
		
		String xslpath = new StringBuilder("").append(File.separatorChar).append("base-portlet")
						.append(File.separatorChar).append("xsl")
						.append(File.separatorChar).append("getTabFields.xsl").toString();

		result = XSLUtil.transformXML(d.asXML(), xslpath );
		
		return result;
	}
	
	private String getTab( String tabId ) throws ServiceError, SecurityException, NoSuchMethodException
	{
		String result = "";
		
		ErrorRaiser.throwIfNull(tabId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		result = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_TAB_BY_ID, tabId) ).asXML();
		
		return result;
	}
	
	public String addTab( String xmlData ) throws DocumentException, ServiceError, NoSuchMethodException, IOException, SQLException
	{
		return addTab( (Element)SAXReaderUtil.read(xmlData).getRootElement().selectSingleNode("//row") );
	}
	
	public String addTab(Element e) throws DocumentException, ServiceError, NoSuchMethodException, IOException, SQLException
	{
		String formid = e.attributeValue("formid");
		ErrorRaiser.throwIfNull( formid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String tabname = StringEscapeUtils.escapeSql( e.attributeValue("tabname") );
		ErrorRaiser.throwIfNull( tabname, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String tabid = e.attributeValue("tabid");
		String query = "";
		
		if( Validator.isNull(tabid) )
		{
			tabid = SQLQueries.getUUID();
			query = String.format(ADD_TAB, tabid, formid, tabname, formid);
		}
		else
		{
			String tabOrder = e.attributeValue("taborder");
			query = String.format("INSERT INTO formtab(tabid, formid, name, taborder) VALUES('%s', '%s', '%s', %s)", tabid, formid, tabname, tabOrder);
		}
		
		PortalLocalServiceUtil.executeUpdateQuery( query );
		
		return getTab( tabid );
	}
	
	public String editTab( String xmlData ) throws DocumentException, ServiceError, SecurityException, NoSuchMethodException, IOException, SQLException
	{
		return editTab( (Element)SAXReaderUtil.read(xmlData).getRootElement().selectSingleNode("//row") );
	}
	
	public String editTab( Element e ) throws DocumentException, ServiceError, SecurityException, NoSuchMethodException, IOException, SQLException
	{
		String tabid = e.attributeValue("tabid");
		ErrorRaiser.throwIfNull( tabid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String tabname = StringEscapeUtils.escapeSql(e.attributeValue("tabname") );
		ErrorRaiser.throwIfNull( tabname, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String tabOrder = "";
		String strPos = e.attributeValue("taborder");
		if( Validator.isNotNull(strPos) )
			tabOrder = " , taborder="+strPos+" ";
		
		PortalLocalServiceUtil.executeUpdateQuery( String.format(UPDATE_TAB, tabname, tabOrder, tabid) );
		
		return getTab( tabid );
	}
	
	public String deleteTabs( String xmlData ) throws DocumentException, ServiceError, IOException, SQLException
	{
		Document d = SAXReaderUtil.read(xmlData);
		Element dataRoot = d.getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row/@tabid");
		
		List<Node> nodes = xpath.selectNodes(dataRoot);
		ErrorRaiser.throwIfFalse(nodes != null && nodes.size() > 0);
		
//		String inClauseSQL = TeaserMgr.getInClauseSQL(nodes);
//		PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_TABS, inClauseSQL));
		
		PortalLocalServiceUtil.executeUpdateQuery( String.format("CALL ITR_DELETE_TABS('%s')", d.asXML()) );
		
		return xmlData;
	}
	
	public String updateTabOrder( String xmlData ) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		String retVal = "<rs/>";
		
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row");
		Node node = xpath.selectSingleNode(dataRoot);
		
		String idform = XMLHelper.getTextValueOf(node, "@formid");
		
		//Elemento que vamos a mover
		String tabId = XMLHelper.getTextValueOf(node, "@tabid");
		ErrorRaiser.throwIfNull(tabId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String sql = String.format("SELECT tabid, taborder FROM formtab WHERE tabid = '%s'", tabId);
 		List<Object> item = PortalLocalServiceUtil.executeQueryAsList(sql);
 		ErrorRaiser.throwIfFalse(item.size() > 0, IterErrorKeys.XYZ_E_SOURCE_NOT_FOUND_ZYX);
 		long currentPriority = Long.parseLong( ((Object[])item.get(0))[1].toString() );
		String currentTab = ((Object[])item.get(0))[0].toString();
 		
		//Elemento de referencia. El elemento a mover quedará encima de este.
		String refid = XMLHelper.getTextValueOf(node, "@refid");
		long refPriority = 0;
		
		if( Validator.isNotNull(refid) && !refid.isEmpty() )
		{
			sql = String.format("SELECT taborder FROM formtab WHERE tabid = '%s'", refid);
			item = PortalLocalServiceUtil.executeQueryAsList(sql);
	 		ErrorRaiser.throwIfFalse(item.size() > 0, IterErrorKeys.XYZ_E_TARGET_NOT_FOUND_ZYX);
	 		refPriority = Long.parseLong( item.get(0).toString() );
		}
		else
		{
			sql = String.format("SELECT IFNULL(MAX(taborder),0) FROM formtab WHERE formid='%s'", idform);
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
 	 			updtItemIdx = String.format("UPDATE formtab SET taborder=%d WHERE tabid='%s'", fin, currentTab);
 	 			getReorderedItems = String.format(GET_UPDATED_TABS, ini-1, fin, idform);
 	 		}
 	 		else if ( refPriority < currentPriority )
 	 		{
 	 			ini = refPriority;
 	 			fin = currentPriority-1;
 	 			oper="+";
 	 			updtItemIdx = String.format("UPDATE formtab SET taborder=%d WHERE tabid='%s'", ini, currentTab);
 	 			getReorderedItems = String.format(GET_UPDATED_TABS, ini, fin+1, idform);
 	 		}
 	 		
 	 		if( ini <= fin)
 	 		{
	 	 		PortalLocalServiceUtil.executeUpdateQuery( String.format(UPDATE_TABS_PRIORITY, oper, idform, currentTab, ini, fin) );
	 	 		
	 	 		PortalLocalServiceUtil.executeUpdateQuery( updtItemIdx );
	 	 		
	 	 		Document result = PortalLocalServiceUtil.executeQueryAsDom( getReorderedItems );
	 	 		retVal = result.asXML();
 	 		}
 		}
 		
 		return retVal;
	}
	
}