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
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.cluster.ClusterMgr;
import com.liferay.cluster.IClusterMgr.ClusterMgrOperation;
import com.liferay.cluster.IClusterMgr.ClusterMgrType;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.AdvancedRssTools;
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
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.base.RssLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.IterAdmin;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.news.model.Qualification;
import com.protecmedia.iter.news.service.QualificationLocalServiceUtil;

/**
 * The implementation of the rss local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.RssLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.RssLocalServiceUtil} to access the rss local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.RssLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.RssLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class RssLocalServiceImpl extends RssLocalServiceBaseImpl
{
	
	private static final String GET_SECTION_BASED_RSS = new StringBuilder()
	.append("SELECT                                                                                      \n")
	.append("layout.plid                                         AS sectionid,                           \n")
	.append("layout.friendlyURL									 AS sectionfriendlyurl,					 \n")
	.append("ExtractValue(layout.name, '/root/name[1]/text()')   AS sectionname,                         \n")
	.append("formxsltransform.transformid                        AS xslid,                               \n")
	.append("formxsltransform.transformname                      AS xslname,                             \n")
	.append("imageframe.imageframeid                             AS frameid,                             \n")
	.append("imageframe.name                                     AS framename,                           \n")
	.append("sectionproperties.autorsscontenttype                AS contenttype,                         \n")
	.append("sectionproperties.autorssorderby                    AS orderby,                             \n")
	.append("sectionproperties.autorssprecacheable               AS precacheable                         \n")
	.append("FROM sectionproperties                                                                      \n")
	.append("INNER JOIN layout           ON layout.plid = sectionproperties.plid                         \n")
	.append("LEFT JOIN  formxsltransform ON sectionproperties.autorssxsl = formxsltransform.transformid  \n")
	.append("LEFT JOIN  imageframe       ON imageframe.imageframeid = sectionproperties.autorssframe     \n")
	.append("WHERE sectionproperties.groupid = %d                                                        \n")
	.append("  AND sectionproperties.autorss = 1                                                         \n")
	.append("  AND (IFNULL(autorssxsl, '') <> '' OR IFNULL(autorssframe, '') <> ''                       \n")
	.append("       OR IFNULL(autorssorderby, '') <> '')                                                 \n")
	.toString();
	
	private static final String GET_SECTIONPROPERTIES = new StringBuilder(
		"SELECT Layout.plid, Layout.uuid_, autorss								\n").append(
		"FROM Layout															\n").append(
		"LEFT JOIN SectionProperties ON SectionProperties.plid = Layout.plid	\n").append(
		"	WHERE Layout.groupId = %d											\n").append(
		"		AND friendlyURL = '%s'											\n").toString();
					
	public Document getSectionBasedRss(long groupId) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SECTION_BASED_RSS, groupId));
	}
	
	private static final String SET_SECTION_BASED_RSS = new StringBuilder()
	.append(" INSERT INTO sectionproperties (groupid, plid, modifieddate, autorssxsl, autorssframe, autorsscontenttype, autorssorderby, autorssprecacheable) \n")
	.append(" VALUES (%1$d, %2$s, NOW(), %3$s, %4$s, %5$s, %6$s, %7$s)  \n")
	.append(" ON DUPLICATE KEY UPDATE                                   \n")
	.append("     modifieddate=NOW(),                                   \n")
	.append("     autorssxsl=VALUES(autorssxsl),                        \n")
	.append("     autorssframe=VALUES(autorssframe),                    \n")
	.append("     autorsscontenttype=VALUES(autorsscontenttype),        \n")
	.append("     autorssorderby=VALUES(autorssorderby),                \n")
	.append("     autorssprecacheable=VALUES(autorssprecacheable);      \n")
	.toString();
	
	public String setSectionBasedRss(String xmlData) throws Exception
	{
		ErrorRaiser.throwIfNull(xmlData, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document dom = SAXReaderUtil.read(xmlData);
		
		ClusterMgr.processConfig(ClusterMgrType.RSS_SECTION, ClusterMgrOperation.SET, dom);
		
		long groupId = XMLHelper.getLongValueOf(dom, "/rs/@groupid");
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		_setSectionBasedRss(groupId, false, dom.getRootElement());
		
		return xmlData;
	}
	
	private void _setSectionBasedRss(long groupId, boolean isImport, Node node) throws ServiceError, DocumentException, SecurityException, NoSuchMethodException, IOException, SQLException, PortalException, SystemException
	{	
		for (Node row : node.selectNodes("row"))
		{
			long sectionId		= getPlid(groupId, isImport, row);
			
			String xslId		= StringUtil.apostrophe(XMLHelper.getTextValueOf(row, "@xslid"));
			String frameId		= _getFrameColumnValue(groupId, isImport, row);
			String contenttype	= StringUtil.apostrophe(XMLHelper.getTextValueOf(row, "@contenttype"));
			String orderby		= StringUtil.apostrophe(XMLHelper.getTextValueOf(row, "@orderby"));
			String precacheable	= XMLHelper.getTextValueOf(row, "@precacheable");
			
			PortalLocalServiceUtil.executeUpdateQuery(String.format(SET_SECTION_BASED_RSS, groupId, sectionId, xslId, frameId, contenttype, orderby, precacheable));
		}
	}
	
	private static final String GET_SECTION_BASED_RSS_GROUP_CONF = new StringBuilder()
	.append("SELECT formxsltransform.transformid    AS xslid,                                      \n")
	.append("	    formxsltransform.transformname  AS xslname,                                    \n")
	.append("       imageframe.imageframeid         AS frameid,                                    \n")
	.append("       imageframe.name                 AS framename,                                  \n")
	.append("       group_config.autorsscontenttype AS contenttype,                                \n")
	.append("       group_config.autorssorderby     AS orderby                                     \n")
	.append("FROM group_config                                                                     \n")
	.append("LEFT JOIN  formxsltransform ON formxsltransform.transformid = group_config.autorssxsl \n")
	.append("LEFT JOIN  imageframe       ON imageframe.imageframeid = group_config.autorssframe    \n")
	.append("WHERE group_config.groupid = %d ")
	.toString();
	
	public Document getSectionBasedRssGroupConfig(long groupId) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SECTION_BASED_RSS_GROUP_CONF, groupId));
	}
	
	private static final String SET_SECTION_BASED_RSS_GROUP_CONF = new StringBuilder()
	.append("UPDATE group_config SET autorssxsl = %s, autorssframe = %s, autorsscontenttype = %s, autorssorderby = %s WHERE groupid = %d")
	.toString();
	
	public Document setSectionBasedRssGroupConfig(String xmlData) throws Exception
	{
		ErrorRaiser.throwIfNull(xmlData, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document dom = SAXReaderUtil.read(xmlData);
		
		ClusterMgr.processConfig(ClusterMgrType.RSS_SECTION, ClusterMgrOperation.SET_GENERAL, dom);
		
		Element root = dom.getRootElement();
		
		long groupId = XMLHelper.getLongValueOf(root, "@groupid");
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		return _setSectionBasedRssGroupConfig(groupId, false, root);
	}
	
	private String _getFrameColumnValue(long groupId, boolean isImport, Node node)
	{
		String frameId = "";
		
		if (isImport)
		{
			frameId = String.format(" (SELECT imageframeid FROM imageframe WHERE groupid=%d AND name='%s') ", 
						groupId, XMLHelper.getTextValueOf(node, "@framename", ""));
		}
		else
		{
			frameId = StringUtil.apostrophe(XMLHelper.getTextValueOf(node, "@frameid"));
		}
		return frameId;
	}
	private Document _setSectionBasedRssGroupConfig(long groupId, boolean isImport, Node node) throws ServiceError, DocumentException, SecurityException, NoSuchMethodException, IOException, SQLException
	{
		String xslId		= StringUtil.apostrophe(XMLHelper.getTextValueOf(node, "@xslid"));
		String frameId		= _getFrameColumnValue(groupId, isImport, node);
		String contenttype	= StringUtil.apostrophe(XMLHelper.getTextValueOf(node, "@contenttype"));
		String orderby		= StringUtil.apostrophe(XMLHelper.getTextValueOf(node, "@orderby"));
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(SET_SECTION_BASED_RSS_GROUP_CONF, xslId, frameId, contenttype, orderby, groupId));
		return getSectionBasedRssGroupConfig(groupId);
	}

	
	private static final String GET_ADVANCED_RSS_LIST = new StringBuilder()
	.append("SELECT advancedrssid,                                                                     \n")
	.append("       rss,                                                                               \n")
	.append("       rssadvancedproperties.name,                                                        \n")
    .append("       htmltitle,                                                                         \n")
	.append("       url,                                                                               \n")
	.append("       formxsltransform.transformid    AS transformid,                                    \n")
	.append("       formxsltransform.transformname  AS transformname,                                  \n")
    .append("       description,                                                                       \n")
    .append("       imageframe.imageframeid         AS frameid,                                        \n")
    .append("       imageframe.name                 AS framename,                                      \n")
	.append("	    contenttype,                                                                       \n")
    .append("       articlelimit,                                                                      \n")
    .append("       orderby,                                                                           \n")
    .append("       shownonactivecontents,                                                             \n")
    .append("       precacheable                                                                       \n")
	.append("FROM rssadvancedproperties                                                                \n")
	.append("LEFT JOIN  formxsltransform ON formxsltransform.transformid = rssadvancedproperties.xsl   \n")
	.append("LEFT JOIN  imageframe       ON imageframe.imageframeid = rssadvancedproperties.imageframe \n")
	.append("WHERE rssadvancedproperties.groupid = %d ")
	.toString();
	
	private static final String _GET_ADVANCED_RSS_PREFERENCES = new StringBuilder(
		"SELECT rssadvancedproperties.advancedrssid, articleslimit,															\n").append(
		"		rssarticlespreferences.layoutid, rssarticlespreferences.qualifid,											\n").append( 
		"		categoryid,	parentrss, parentsectionrss																		\n").append( 
		"		%s																											\n").append(
		"FROM rssadvancedproperties																							\n").append(
		"INNER JOIN rssarticlespreferences ON rssarticlespreferences.advancedrssid = rssadvancedproperties.advancedrssid	\n").append(
		"%s																													\n").append(
		"	WHERE rssadvancedproperties.groupid = %%d																		\n").append(
		"	ORDER BY rssadvancedproperties.advancedrssid, rssarticlespreferences.priority ASC								\n").toString();
	
	private static final String _GET_ADVANCED_RSS_PREFERENCES_EXTRA_COLUMNS	= new StringBuilder(
		",Layout.friendlyURL sectionfriendlyurl, News_Qualification.name qualifName,										\n").append( 
		"ITR_GET_CATEGORY_PATH(rssarticlespreferences.categoryid) categoryPath,												\n").append( 
		"ParentLayout.friendlyURL parentfriendlyurl																			\n").toString();

	private static final String _GET_ADVANCED_RSS_PREFERENCES_EXTRA_TABLES	= new StringBuilder(
		"LEFT JOIN Layout ON (Layout.uuid_ = rssarticlespreferences.layoutid AND Layout.groupId = rssadvancedproperties.groupId)									\n").append( 
		"LEFT JOIN News_Qualification ON (News_Qualification.id_ = rssarticlespreferences.qualifid AND News_Qualification.groupId = rssadvancedproperties.groupId)	\n").append( 
		"LEFT JOIN Layout ParentLayout ON (ParentLayout.uuid_ = parentsectionrss AND ParentLayout.groupId = rssadvancedproperties.groupId)							\n").toString();

	private static final String GET_ADVANCED_RSS_PREFERENCES 			= String.format(_GET_ADVANCED_RSS_PREFERENCES, " "," ");
	private static final String GET_ADVANCED_RSS_PREFERENCES_EXTRA_INFO = String.format(_GET_ADVANCED_RSS_PREFERENCES, _GET_ADVANCED_RSS_PREFERENCES_EXTRA_COLUMNS, _GET_ADVANCED_RSS_PREFERENCES_EXTRA_TABLES);
	
	
	public Document getAdvancedRssList(long groupId, boolean updateRssMap) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException
	{
		return _getAdvancedRssList(groupId, null, updateRssMap, false);
	}
	
	private Document _getAdvancedRssList(long groupId, String rssId, boolean updateRssMap, boolean showExtraInfo) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException
	{
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		StringBuilder sql = new StringBuilder(String.format(GET_ADVANCED_RSS_LIST, groupId));
		if (rssId != null)
			sql.append( String.format(" AND advancedrssid = '%s' ", rssId) );
		
		Document result 	 = PortalLocalServiceUtil.executeQueryAsDom(sql.toString());
		
		String sqlPref = showExtraInfo ? GET_ADVANCED_RSS_PREFERENCES_EXTRA_INFO : GET_ADVANCED_RSS_PREFERENCES;
		Document preferences = PortalLocalServiceUtil.executeQueryAsDom(String.format(sqlPref, groupId) );
		
		List<Node> rssList = result.getRootElement().selectNodes("/rs/row");
		for (Node rss : rssList)
		{
			String advancedRssId = XMLHelper.getTextValueOf(rss, "@advancedrssid");
			List<Node> rssPreferences = preferences.selectNodes("/rs/row[@advancedrssid='" + advancedRssId + "']");
			
			// Orden
			String orderby = XMLHelper.getTextValueOf(rss, "@orderby");
			if (Validator.isNotNull(orderby))
			{
				Document domOrder = SAXReaderUtil.read(orderby);
				((Element) rss).add(domOrder.getRootElement());
				((Element) rss).attribute("orderby").detach();
			}
			
			Element sections = ((Element) rss).addElement("sections");
			Element qualifications = ((Element) rss).addElement("qualifications");
			Element categories = ((Element) rss).addElement("categories");
			Element parents = ((Element) rss).addElement("parents");
			
			for (Node preference : rssPreferences)
			{
				String value;
				// Secciones
				if (Validator.isNotNull( value = XMLHelper.getTextValueOf(preference, "@layoutid")))
				{
					Element elem = sections.addElement("section").addAttribute("layout", value).
						addAttribute("limit", XMLHelper.getStringValueOf(preference, "@articleslimit"));
					
					if (showExtraInfo)
						elem.add(preference.selectSingleNode("@sectionfriendlyurl").detach());
				}
				// Calificaciones
				else if (Validator.isNotNull( value = XMLHelper.getTextValueOf(preference, "@qualifid")))
				{
					Element elem = qualifications.addElement("qualification").addAttribute("qualifid", value);
					
					if (showExtraInfo)
						elem.add( preference.selectSingleNode("@qualifName").detach() );
				}
				// Metadatos
				else if (Validator.isNotNull( value = XMLHelper.getTextValueOf(preference, "@categoryid")))
				{
					Element elem = categories.addElement("category").addAttribute("categoryid", value);
					
					if (showExtraInfo)
						elem.add( preference.selectSingleNode("@categoryPath").detach() );
				}
				// Herencia
				else if (Validator.isNotNull( value = XMLHelper.getTextValueOf(preference, "@parentrss")))
				{
					parents.addElement("rss").addAttribute("rssid", value);
				}
				else if (Validator.isNotNull( value = XMLHelper.getTextValueOf(preference, "@parentsectionrss")))
				{
					Element elem = parents.addElement("rss").addAttribute("rssid", value);
					
					if (showExtraInfo)
						elem.add( preference.selectSingleNode("@parentfriendlyurl").detach() );
				}
			}
		}
		
		if (updateRssMap)
		{
			// Actualizar mapa de RSSs
			try
			{
				AdvancedRssTools.initAdvancedRss();
			}
			catch (Exception e)
			{
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_RSS_UNABLE_TO_UPDATE_URLS_MAP_ZYX);
			}
		}
		
		return result;
	}
	
	private static final String SET_ADVANCED_RSS = new StringBuilder()
	.append(" INSERT INTO rssadvancedproperties (advancedrssid, groupid, rss, xsl, name, htmltitle, description, url, imageframe, contenttype, articlelimit, orderby, shownonactivecontents, precacheable) ")
	.append(" VALUES ('%s', %s, '%s', %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) ")
	.toString();
	
	private static final String UPD_ADVANCED_RSS = new StringBuilder()
	.append(" UPDATE rssadvancedproperties SET rss='%s', xsl=%s, name=%s, htmltitle=%s, description=%s, url=%s, imageframe=%s, contenttype=%s, articlelimit=%s, orderby=%s, shownonactivecontents=%s, precacheable=%s")
	.append(" WHERE advancedrssid = '%s' ")
	.toString();
	
	private static final String CLEAN_ADVANCED_RSS_PREFERENCES = new StringBuilder()
	.append(" DELETE FROM rssarticlespreferences WHERE advancedrssid = '%s' ").toString();
	
	private static final String ADD_ADVANCED_RSS_PREFERENCES = new StringBuilder()
	.append(" INSERT INTO rssarticlespreferences (advancedrssid, layoutid, articleslimit, qualifid, categoryid, parentrss, parentsectionrss, priority) ")
	.append(" VALUES ('%s', %s, %s, %s, %s, %s, %s, %s)").toString();
	
	private static final String GET_ADVANCEDRSS_BY_RSS = "SELECT advancedrssid FROM rssadvancedproperties WHERE groupid=%d AND rss='%s'";
	private String getAdvancedRSSIdByRSS(long groupId, String rss)
	{
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList( String.format(GET_ADVANCEDRSS_BY_RSS, groupId, rss) );
		return Validator.isNotNull(result) ? result.get(0).toString() : null;
	}
	
	public Document setAdvancedRss(String xmlData) throws Exception
	{
		Document result = null;
				
		ErrorRaiser.throwIfNull(xmlData, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document dom = SAXReaderUtil.read(xmlData);
		
		ClusterMgr.processConfig(ClusterMgrType.RSS_ADVANCED, ClusterMgrOperation.SET, dom);
		
		Element root = SAXReaderUtil.read(xmlData).getRootElement();
		
		long groupId = XMLHelper.getLongValueOf(root, "@groupid");
        if (groupId <= 0)
        {
        	String groupName = XMLHelper.getStringValueOf(root, "@groupName");
        	ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        	groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
        }
        ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        
		boolean updtIfExist = GetterUtil.getBoolean(XMLHelper.getTextValueOf(root, "@updtIfExist"));
		boolean isImport 	= GetterUtil.getBoolean(XMLHelper.getTextValueOf(root, "@importProcess"));

		
		List<Node> rows = dom.selectNodes("//row");
		ErrorRaiser.throwIfFalse(rows.size() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);	
		
		// Si existe más de un elemento a crear/actualizar, se inicializará el mapa de RSSs
		boolean reinitializeRSSMap = rows.size() > 1;

		// Se crean todos los RSS avanzados
		for (Node node : rows)
		{
			Element rssElem = (Element)node;
			
			String newRSS = XMLHelper.getTextValueOf(rssElem, "@rss");
			ErrorRaiser.throwIfNull(newRSS, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			String ext = FilenameUtils.getExtension(newRSS);
			if ( "xml".equalsIgnoreCase(ext) || "json".equalsIgnoreCase(ext))
			{
				newRSS = FilenameUtils.removeExtension(newRSS);
				rssElem.addAttribute("rss", newRSS);
			}
			
			String name = StringUtil.apostrophe(StringEscapeUtils.escapeSql(XMLHelper.getTextValueOf(rssElem, "@name")));
			ErrorRaiser.throwIfNull(name, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			String htmltitle	= StringUtil.apostrophe(StringEscapeUtils.escapeSql(XMLHelper.getTextValueOf(rssElem, "@htmltitle")));
			String url			= StringUtil.apostrophe(StringEscapeUtils.escapeSql(XMLHelper.getTextValueOf(rssElem, "@url")));
			String transformid	= StringUtil.apostrophe(XMLHelper.getTextValueOf(rssElem, "@transformid"));
			String description	= StringUtil.apostrophe(StringEscapeUtils.escapeSql(XMLHelper.getTextValueOf(rssElem, "@description")));
			String frameid		= StringUtil.apostrophe(XMLHelper.getTextValueOf(rssElem, "@frameid"));
			String contenttype	= StringUtil.apostrophe(StringEscapeUtils.escapeSql(XMLHelper.getTextValueOf(rssElem, "@contenttype")));
			String articlelimit	= StringUtil.apostrophe(XMLHelper.getTextValueOf(rssElem, "@articlelimit"));
			String orderby		= Validator.isNotNull(rssElem.element("orderby")) ? StringUtil.apostrophe(rssElem.element("orderby").asXML()) : null;
			String nonactive	= XMLHelper.getTextValueOf(rssElem, "@shownonactivecontents", StringPool.FALSE);
			String precache		= XMLHelper.getTextValueOf(rssElem, "@precacheable", StringPool.FALSE);
			String advRssId 	= (isImport) ? getAdvancedRSSIdByRSS(groupId, newRSS) : XMLHelper.getTextValueOf(rssElem, "@advancedrssid");
			
			// Se lanza un error si existe un RSS y se ha marcado 'NO actualizar'.
			ErrorRaiser.throwIfFalse(!isImport || (updtIfExist || Validator.isNull(advRssId)), IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, 
					String.format("%s(%s)", IterAdmin.IA_CLASS_RSS_ADVANCED, newRSS));

			if (Validator.isNull(advRssId))
			{
				// Generar ID
				advRssId = SQLQueries.getUUID();
				
				// Insertar configuración de RSS
				PortalLocalServiceUtil.executeUpdateQuery(String.format(SET_ADVANCED_RSS, advRssId, groupId, StringEscapeUtils.escapeSql(newRSS), transformid, name, htmltitle, description, url, frameid, contenttype, articlelimit, orderby, nonactive, precache));
			}
			else
			{
				if (!isImport && !reinitializeRSSMap)
				{
					// Si no es un proceso de importación se obtiene el RSS anterior para actualizar el 
					List<Object> rssObject = PortalLocalServiceUtil.executeQueryAsList(String.format("SELECT rss FROM rssadvancedproperties WHERE advancedrssid = '%s'", advRssId));
					if (Validator.isNotNull(rssObject.size()))
						rssElem.addAttribute("oldRss", rssObject.get(0).toString());
				}
				
				// Si es una modificación desde IterAdmin, comprueba que no se intente heredar en un RSS del que otros están heredando
				String dependencies = null;
				if (!isImport && rssElem.selectNodes("./parents/rss").size() > 0 && (dependencies = checkInheritance(StringUtil.apostrophe(advRssId))).length() > 0)
				{
					ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_RSS_NO_HERITABLE_ZYX, dependencies);
				}
				
				// Actualizar configuración de RSS
				PortalLocalServiceUtil.executeUpdateQuery(String.format(UPD_ADVANCED_RSS, StringEscapeUtils.escapeSql(newRSS), transformid, name, htmltitle, description, url, frameid, contenttype, articlelimit, orderby, nonactive, precache, advRssId));
			}
			
			String currentAdvRssId = XMLHelper.getTextValueOf(rssElem, "@advancedrssid");
			if (!advRssId.equals(currentAdvRssId))
			{
				rssElem.addAttribute("advancedrssid", advRssId);
				
				if (Validator.isNotNull(currentAdvRssId))
				{
					// Si se ha modificado el advancedrssid, se actualizan todas las referencias al valor anterior
					List<Node> referenceList = dom.selectNodes( String.format("//row/parents/rss[@rssid='%s']/@rssid", currentAdvRssId) );
					for (Node reference : referenceList)
					{
						reference.setText(advRssId);
					}
				}
			}
			
			// Borrar preferencias de selección de artículos
			PortalLocalServiceUtil.executeUpdateQuery(String.format(CLEAN_ADVANCED_RSS_PREFERENCES, advRssId));
		}
		
		// Se crean todas las preferencias de dichos RSS avanzados
		for (Node node : rows)
		{
			Element rssElem 	= (Element)node;
			String advRssId = XMLHelper.getTextValueOf(rssElem, "@advancedrssid");
			String newRSS 	= XMLHelper.getTextValueOf(rssElem, "@rss");

			// Insertar preferencias de selección de artículos
			int i;
			// Secciones
			Element sections = rssElem.element("sections");
			if (Validator.isNotNull(sections))
			{
				i = 0;
				for (Node section : sections.selectNodes("./section"))
				{
					String parentsectionUUID = (isImport) ? 
							LayoutLocalServiceUtil.getFriendlyURLLayout(groupId, false, XMLHelper.getTextValueOf(section, "@sectionfriendlyurl")).getUuid() :
							XMLHelper.getTextValueOf(section, "@layout");	
								
					PortalLocalServiceUtil.executeUpdateQuery(String.format(ADD_ADVANCED_RSS_PREFERENCES, advRssId, 
							StringUtil.apostrophe(parentsectionUUID), XMLHelper.getTextValueOf(section, "@limit"),
							StringPool.NULL, StringPool.NULL, StringPool.NULL, StringPool.NULL, i++));	
				}
			}
			// Calificaciones
			Element qualifications = rssElem.element("qualifications");
			if (Validator.isNotNull(qualifications))
			{
				i = 0;
				for (Node qualification : qualifications.selectNodes("./qualification"))
				{
					String qualifId = "";
					if (isImport)
					{
						String qualifName = XMLHelper.getTextValueOf(qualification, "@qualifName", "");
						Qualification qualif = QualificationLocalServiceUtil.getQualification(groupId, qualifName);
						
						ErrorRaiser.throwIfNull(qualif, IterErrorKeys.XYZ_E_ITERADMIN_IMPORT_REF_NON_FOUND_ELEMENT_ZYX,
								String.format("%s(%s)", IterAdmin.IA_CLASS_QUALIFICATION, qualifName));
						qualifId = qualif.getQualifId();
					}
					else
					{
						qualifId = XMLHelper.getTextValueOf(qualification, "@qualifid");
					}
					PortalLocalServiceUtil.executeUpdateQuery(String.format(ADD_ADVANCED_RSS_PREFERENCES, advRssId, StringPool.NULL, StringPool.NULL,
							StringUtil.apostrophe(qualifId), StringPool.NULL, StringPool.NULL, StringPool.NULL, i++));	
				}
			}
			// Categorías
			Element categories = rssElem.element("categories");
			if (Validator.isNotNull(categories))
			{
				for (Node category : categories.selectNodes("./category"))
				{
					String categoryId = (isImport) ? 
											String.format("ITR_GET_CATEGORY_FROM_PATH_FUNC(%d, '%s', '/')", groupId, XMLHelper.getTextValueOf(category, "@categoryPath", "")) :
											StringUtil.apostrophe(XMLHelper.getTextValueOf(category, "@categoryid"));
											
					PortalLocalServiceUtil.executeUpdateQuery(String.format(ADD_ADVANCED_RSS_PREFERENCES, advRssId, StringPool.NULL, StringPool.NULL,
							StringPool.NULL, categoryId, StringPool.NULL,
							StringPool.NULL, StringPool.NULL));	
				}
			}
			// Herencia
			Element parents = rssElem.element("parents");
			if (Validator.isNotNull(parents))
			{
				i = 0;
				for (Node parent : parents.selectNodes("./rss"))
				{
					boolean isAdvancedRSS = ( (isImport && Validator.isNull(XMLHelper.getTextValueOf(parent, "@parentfriendlyurl"))) ||
							                  AdvancedRssTools.isAdvancedRss(groupId, XMLHelper.getTextValueOf(parent, "@url")) );
					
					if (isAdvancedRSS)
					{
						PortalLocalServiceUtil.executeUpdateQuery(String.format(ADD_ADVANCED_RSS_PREFERENCES, advRssId, StringPool.NULL, StringPool.NULL,
								StringPool.NULL, StringPool.NULL, StringUtil.apostrophe(XMLHelper.getTextValueOf(parent, "@rssid")), StringPool.NULL, i++));
					}
					else
					{
						String parentsectionUUID = getLayoutUUID(groupId, isImport, parent);
								
						PortalLocalServiceUtil.executeUpdateQuery(String.format(ADD_ADVANCED_RSS_PREFERENCES, advRssId, StringPool.NULL, StringPool.NULL,
								StringPool.NULL, StringPool.NULL, StringPool.NULL, StringUtil.apostrophe(parentsectionUUID), i++));
					}
				}
			}
			
			result = _getAdvancedRssList(groupId, advRssId, false, false);
			
			String oldRss = XMLHelper.getTextValueOf(rssElem, "@oldRss", "");
			// Actualizar mapa de RSSs
			if (!reinitializeRSSMap && !newRSS.equals(oldRss))
			{
				try
				{
					AdvancedRssTools.updateAdvancedRssMap(String.valueOf(groupId), newRSS, oldRss);
				}
				catch (Exception e)
				{
					ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_RSS_UNABLE_TO_UPDATE_URLS_MAP_ZYX, e.toString(), e.getStackTrace());
				}
			}
		}
		
		if (reinitializeRSSMap)
		{
			// En un proceso de importación se reconstruye el mapa de RSS avanzados al final. 
			// Así se evitan problemas de rollback y se actualizan todos de una vez
			try
			{
				AdvancedRssTools.initAdvancedRss();
			}
			catch (Exception e)
			{
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_RSS_UNABLE_TO_UPDATE_URLS_MAP_ZYX, e.toString(), e.getStackTrace());
			}
		}
		
		return result;
	}
	
	private static final String CHECK_RSS_INHERITANCE = new StringBuilder()
	.append(" SELECT CONCAT(b.name, ' (', b.advancedrssid, ') ',' [ ', rss, ' ]') rss \n")
	.append(" FROM rssarticlespreferences a                                           \n")
	.append(" INNER JOIN rssadvancedproperties b ON a.advancedrssid = b.advancedrssid \n")
	.append(" WHERE a.parentrss = %s "                                                   )
	.toString();
	
	private String checkInheritance(String advRssId)
	{
		String rssNames = StringPool.BLANK;
		String sql = String.format(CHECK_RSS_INHERITANCE, advRssId);
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList(sql);
		if (result != null && result.size() > 0)
		{
			rssNames = "Dependencies:" + StringPool.RETURN_NEW_LINE + StringUtil.merge(result, StringPool.RETURN_NEW_LINE);
		}
		return rssNames;
	}
	
	private long getPlid(long groupId, boolean isImport, Node row) throws ServiceError, SecurityException, NoSuchMethodException
	{
		long plid = 0;
		
		if (isImport)
		{
			String friendlyURL = XMLHelper.getTextValueOf(row, "@sectionfriendlyurl");
			ErrorRaiser.throwIfNull(friendlyURL, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, IterAdmin.IA_CLASS_RSS_SECTIONS);
			
			Document dom = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_SECTIONPROPERTIES, groupId, friendlyURL) );
			plid = XMLHelper.getLongValueOf(dom, "/rs/row/@plid");
			ErrorRaiser.throwIfFalse(plid > 0, IterErrorKeys.XYZ_E_UNDEFINED_LAYOUT_ZYX, friendlyURL);
			
			boolean autorss = GetterUtil.getBoolean(XMLHelper.getStringValueOf(dom, "/rs/row/@autorss"));
			ErrorRaiser.throwIfFalse(autorss, IterErrorKeys.XYZ_E_RSSLAYOUT_DISABLED_ZYX, friendlyURL);
		}
		else
		{
			plid = XMLHelper.getLongValueOf(row, "@sectionid");
		}
		
		return plid;
	}
	
	private String getLayoutUUID(long groupId, boolean isImport, Node row) throws ServiceError, SecurityException, NoSuchMethodException
	{
		String uuid = "";
		
		if (isImport)
		{
			String friendlyURL = XMLHelper.getTextValueOf(row, "@parentfriendlyurl");
			ErrorRaiser.throwIfNull(friendlyURL, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, IterAdmin.IA_CLASS_RSS_SECTIONS);
			
			Document dom = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_SECTIONPROPERTIES, groupId, friendlyURL) );
			uuid = XMLHelper.getTextValueOf(dom, "/rs/row/@uuid_");
			ErrorRaiser.throwIfNull(uuid, IterErrorKeys.XYZ_E_UNDEFINED_LAYOUT_ZYX, friendlyURL);
			
			boolean autorss = GetterUtil.getBoolean(XMLHelper.getStringValueOf(dom, "/rs/row/@autorss"));
			ErrorRaiser.throwIfFalse(autorss, IterErrorKeys.XYZ_E_RSSLAYOUT_DISABLED_ZYX, friendlyURL);
		}
		else
		{
			uuid = XMLHelper.getTextValueOf(row, "@rssid");
		}
		
		return uuid;
	}

	
	private static final String DELETE_ADVANCED_RSS = " DELETE FROM rssadvancedproperties WHERE advancedrssid IN (%s) ";
	
	public String deleteAdvancedRss(String xmlData) throws Exception
	{
		ErrorRaiser.throwIfNull(xmlData, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document dom = SAXReaderUtil.read(xmlData);
		
		ClusterMgr.processConfig(ClusterMgrType.RSS_ADVANCED, ClusterMgrOperation.DELETE, dom);
		
		long groupId = XMLHelper.getLongValueOf(dom.getRootElement(), "@groupid");
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		String advancedRssIds  = StringPool.BLANK;
		
		for (Node row : dom.selectNodes("/rs/row"))
		{
			String advancedRssId = StringUtil.apostrophe(XMLHelper.getTextValueOf(row, "@advancedrssid"));
			advancedRssIds += Validator.isNull(advancedRssIds) ? advancedRssId : StringPool.COMMA_AND_SPACE + advancedRssId;
		}
		
		// Comprueba si hay dependencias de herencia
		String dependencies = null;
		if ((dependencies = checkInheritanceOnDelete(groupId, advancedRssIds)).length() > 0)
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_RSS_CHILDREN_DEPENDENCIES_ZYX, dependencies);
		}
		
		List<Object> advancedRssUrls = PortalLocalServiceUtil.executeQueryAsList(String.format("SELECT rss FROM rssadvancedproperties WHERE advancedrssid IN (%s)", advancedRssIds));
		PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_ADVANCED_RSS, advancedRssIds));
		
		// Actualizar mapa de RSSs
		try
		{
			AdvancedRssTools.deleteAdvancedRss(String.valueOf(groupId), StringUtil.merge(advancedRssUrls));
		}
		catch (Exception e)
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_RSS_UNABLE_TO_UPDATE_URLS_MAP_ZYX, e.toString(), e.getStackTrace());
		}
		
		return xmlData;
	}
	
	private static final String CHECK_INHERITANCE_ON_DELETE = new StringBuilder()
	.append("SELECT parent.advancedrssid parentId,              \n")
	.append("       parent.name          parentName,            \n")
	.append("       parent.rss           parentUrl,             \n")
	.append("       prop.advancedrssid   childrenId,            \n")
	.append("       prop.name            childrenName,          \n")
	.append("       prop.rss             childrenUrl            \n")
	.append("FROM rssadvancedproperties prop                    \n")
	.append("INNER JOIN rssarticlespreferences pref             \n")
	.append("        ON prop.advancedrssid = pref.advancedrssid \n")
	.append("INNER JOIN rssadvancedproperties parent            \n")
	.append("        ON pref.parentrss = parent.advancedrssid   \n")
	.append("WHERE prop.groupid = %1$d                          \n")
	.append("  AND prop.advancedrssid not in (%2$s)             \n")
	.append("  AND pref.parentrss in (%2$s)                     \n")
	.append("ORDER BY parentId "                                   )
	.toString();
	
	private String checkInheritanceOnDelete(long groupId, String advancedRssIds) throws SecurityException, NoSuchMethodException
	{
		StringBuilder dependencies = new StringBuilder();
		String sql = String.format(CHECK_INHERITANCE_ON_DELETE, groupId, advancedRssIds);
		Document result = PortalLocalServiceUtil.executeQueryAsDom(sql);
		if (result != null)
		{
			for (Node rss : result.getRootElement().selectNodes("row"))
			{
				String parentId     = XMLHelper.getStringValueOf(((Element) rss), "@parentId");
				String parentName   = XMLHelper.getStringValueOf(((Element) rss), "@parentName");
				String parentUrl    = XMLHelper.getStringValueOf(((Element) rss), "@parentUrl");
				String childrenId   = XMLHelper.getStringValueOf(((Element) rss), "@childrenId");
				String childrenName = XMLHelper.getStringValueOf(((Element) rss), "@childrenName");
				String childrenUrl  = XMLHelper.getStringValueOf(((Element) rss), "@childrenUrl");
				
				dependencies.append(StringPool.RETURN_NEW_LINE)
							.append(parentName).append(StringPool.SPACE).append(StringPool.OPEN_PARENTHESIS).append(parentId).append(StringPool.CLOSE_PARENTHESIS)
							.append(StringPool.SPACE).append(StringPool.OPEN_BRACKET).append(parentUrl).append(StringPool.CLOSE_BRACKET)
							.append(" used in RSS ")
							.append(childrenName).append(StringPool.SPACE).append(StringPool.OPEN_PARENTHESIS).append(childrenId).append(StringPool.CLOSE_PARENTHESIS)
							.append(StringPool.SPACE).append(StringPool.OPEN_BRACKET).append(childrenUrl).append(StringPool.CLOSE_BRACKET)
							.append(StringPool.RETURN_NEW_LINE);
			}
		}
		return dependencies.toString();
	}
	
	private static final String GET_INHERITABLE_RSS = new StringBuilder()
	.append(" SELECT layout.uuid_ AS rssid,                                                                             \n")
	.append(" 		ExtractValue(layout.name, '/root/name[1]/text()') AS rssname,                                       \n")
	.append("         CONCAT('/rss', friendlyURL, '.xml') AS url,                                                       \n")
	.append("         ExtractValue(layout.name, '/root/name[1]/text()') AS sections,                                    \n")
	.append("         NULL AS categories,                                                                               \n")
	.append(" 	    NULL AS qualifications,                                                                             \n")
	.append("         ExtractValue(autorssorderby, '/orderby/order/@field | /orderby/order/@type') AS orderby,          \n")
	.append("         NULL AS articlelimit,                                                                             \n")
	.append("         NULL AS advancedinherit,                                                                          \n")
	.append("         NULL AS sectionsinherit                                                                           \n")
	.append(" FROM sectionproperties INNER JOIN layout ON layout.plid = sectionproperties.plid                          \n")
	.append(" WHERE sectionproperties.groupid = %1$d AND sectionproperties.autorss = 1                                  \n")
	.append(" AND (IFNULL(autorssxsl, '') <> '' OR IFNULL(autorssframe, '') <> '' OR IFNULL(autorssorderby, '') <> '')  \n")
	.append(" UNION ALL                                                                                                 \n")
	.append(" SELECT rap.advancedrssid AS rssid,                                                                        \n")
	.append("        rap.name AS rssname,                                                                               \n")
	.append(" 	   rap.rss AS rssurl,                                                                                   \n")
	.append("        GROUP_CONCAT(ExtractValue(l.name, '/root/name[1]/text()') SEPARATOR ', ') AS sections,             \n")
	.append("        GROUP_CONCAT(a.name SEPARATOR ', ') AS categories,                                                 \n")
	.append("        GROUP_CONCAT(n.name SEPARATOR ', ') AS qualifications,                                             \n")
	.append("        ExtractValue(rap.orderby, '/orderby/order/@field | /orderby/order/@type') AS orderby,              \n")
	.append("        CAST(                                                                                              \n")
	.append("          CASE WHEN SUM(pref.articleslimit) > 0 THEN 'por sección'                                         \n")
	.append("               WHEN rap.articlelimit > 0 THEN rap.articlelimit                                             \n")
	.append(" 		      ELSE NULL                                                                                     \n")
	.append("          END AS CHAR(11) CHARACTER SET utf8) AS articlelimit,                                             \n")
	.append("        GROUP_CONCAT(inhadv.name SEPARATOR ', ') AS advancedinherit,                                       \n")
	.append("        GROUP_CONCAT(ExtractValue(inhsect.name, '/root/name[1]/text()') SEPARATOR ', ') AS sectionsinherit \n")
	.append(" FROM rssadvancedproperties rap                                                                            \n")
	.append(" INNER JOIN rssarticlespreferences pref ON pref.advancedrssid = rap.advancedrssid                          \n")
	.append(" LEFT JOIN layout l ON l.uuid_ = pref.layoutid                                                             \n")
	.append(" LEFT JOIN AssetCategory a ON a.categoryId = pref.categoryid                                               \n")
	.append(" LEFT JOIN News_Qualification n ON n.qualifId = pref.qualifid                                              \n")
	.append(" LEFT JOIN rssadvancedproperties inhadv ON inhadv.advancedrssid = pref.parentrss                           \n")
	.append(" LEFT JOIN layout inhsect ON inhsect.uuid_ = pref.parentsectionrss                                         \n")
	.append(" WHERE rap.groupid = %1$d                                                                                  \n")
	.append("   AND NOT EXISTS (SELECT * FROM rssarticlespreferences                                                    \n")
	.append("                   WHERE advancedrssid = rap.advancedrssid                                                 \n")
	.append("                   AND IFNULL(parentrss, '') <> '')                                                        \n")
	.toString();
	
	private static final String GET_INHERITABLE_RSS_EDIT_FILTER = new StringBuilder()
	.append("   AND rap.advancedrssid <> '%1$s'                                                                         \n")
	.toString();
	
	private static final String GET_INHERITABLE_RSS_ORDER_AND_GROUP = new StringBuilder()
	.append(" GROUP BY rap.advancedrssid                                                                                \n")
	.append(" ORDER BY rssname ASC;                                                                                     \n")
	.toString();
	
	public String getInheritableRss(long groupId, String advancedRssId) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		StringBuilder sql = new StringBuilder(String.format(GET_INHERITABLE_RSS, groupId));
		if (Validator.isNotNull(advancedRssId))
			sql.append(String.format(GET_INHERITABLE_RSS_EDIT_FILTER, advancedRssId));
		sql.append(GET_INHERITABLE_RSS_ORDER_AND_GROUP);
		
		String result = PortalLocalServiceUtil.executeQueryAsDom(sql.toString()).asXML();
		
		return result;
	}
	
	public Document exportRSSAdvanced(String params) throws SystemException, DocumentException, ServiceError, PortalException, SecurityException, NoSuchMethodException
	{
		Element root = SAXReaderUtil.read(params).getRootElement();
		String groupName = XMLHelper.getStringValueOf(root, "@groupName");
		ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		long groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();

		return _getAdvancedRssList(groupId, null, false, true);
	}
	
	public Document exportRSSSections(String params) throws SystemException, DocumentException, ServiceError, PortalException, SecurityException, NoSuchMethodException
	{
		Element root = SAXReaderUtil.read(params).getRootElement();
		String groupName = XMLHelper.getStringValueOf(root, "@groupName");
		ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		long groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();

		Element groupCfg	= getSectionBasedRssGroupConfig(groupId).getRootElement();
		groupCfg.setName("group");
		
		Element sectionsCfg = getSectionBasedRss(groupId).getRootElement();
		sectionsCfg.setName("sections");
		
		Document result = SAXReaderUtil.read("<rs/>");
		result.getRootElement().add( groupCfg.detach() );
		result.getRootElement().add( sectionsCfg.detach() );
		
		return result;
	}
	
	public void importRSSSections(String data) throws SystemException, SecurityException, ServiceError, DocumentException, NoSuchMethodException, IOException, SQLException, PortalException
	{
		Element root = SAXReaderUtil.read(data).getRootElement();
		
		long groupId = XMLHelper.getLongValueOf(root, "@groupid");
        if (groupId <= 0)
        {
        	String groupName = XMLHelper.getStringValueOf(root, "@groupName");
        	ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        	groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
        }
        ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        
		// Se importa la configuración del RSS de sección del grupo
		_setSectionBasedRssGroupConfig(groupId, true, root.selectSingleNode("group/row"));
		
		// Se importa la configuraciones de los RSS por sección
		_setSectionBasedRss(groupId, true, root.selectSingleNode("sections"));
	}
}