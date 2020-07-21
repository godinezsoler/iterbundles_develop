package com.protecmedia.iter.xmlio.service.impl;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBFactoryUtil;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.CatalogQueries;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.InstrumentalContentUtil;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.xml.Attribute;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.kernel.xml.XSLUtil;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.asset.NoSuchCategoryException;
import com.liferay.portlet.asset.model.AssetCategory;
import com.liferay.portlet.asset.service.AssetVocabularyLocalServiceUtil;
import com.liferay.portlet.asset.service.persistence.AssetCategoryUtil;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portlet.journal.service.JournalTemplateLocalServiceUtil;
import com.liferay.portlet.journal.util.GlobalJournalTemplateMgr;
import com.protecmedia.iter.base.service.FramesLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.ThemeWebResourcesLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.designer.service.PageTemplateLocalServiceUtil;
import com.protecmedia.iter.news.model.PageContent;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;
import com.protecmedia.iter.news.service.ProductLocalServiceUtil;
import com.protecmedia.iter.services.service.ServiceLocalServiceUtil;
import com.protecmedia.iter.xmlio.NoSuchLiveException;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.model.LiveControl;
import com.protecmedia.iter.xmlio.model.LivePool;
import com.protecmedia.iter.xmlio.service.CategoriesPropertiesPublicationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.CommunityPublisherLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.ContextVarsPublishLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.DaylyTopicsPublicationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LiveControlLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LivePoolLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LiveServiceUtil;
import com.protecmedia.iter.xmlio.service.base.LiveLocalServiceBaseImpl;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.persistence.LiveUtil;
import com.protecmedia.iter.xmlio.service.util.CacheRefresh;
import com.protecmedia.iter.xmlio.service.util.LayoutTemplatePublisher;
import com.protecmedia.iter.xmlio.service.util.LiveSorter;
import com.protecmedia.iter.xmlio.service.util.LiveSorter.LiveSorterCriterion;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;
import com.protecmedia.iter.xmlio.service.util.XmlioKeys;
import com.protecmedia.iter.xmlio.util.JournalTemplatePublisher;
import com.protecmedia.iter.xmlio.util.PublicationProxyBack;
import com.protecmedia.iter.xmlio.util.XmlioLiveDynamicQuery;

@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class LiveLocalServiceImpl extends LiveLocalServiceBaseImpl 
{
	private static Log _log 	= LogFactoryUtil.getLog(LiveLocalServiceImpl.class);
	private static Log _logJA 	= LogFactoryUtil.getLog(LiveLocalServiceImpl.class.getName().concat("_JA"));
	
	public static final String DEFAULT_CONTENT_TO_PUBLISH = "1";
	
	private static final String DAILYTOPICS            = "dayIssues";
	private static final String ARTICLES               = "articles";
	private static final String WATERMARK   		   = "waterMark";
	private static final String FRAMES		           = "frames";
	private static final String REPLACE_CONTENTS_TYPES = "replacementcontenttypes";
	private static final String GROUP				   = "group";
	private static final String CONTEXTVARS			   = "contextvars";
	private static final String SECTION                = "section";
	private static final String WEB_MODEL              = "webmodel";
	private static final String CATEGORY               = "category";
	private static final String VOCABULARY			   = "vocabulary";
	private static final String SUBSCRIPTIONS          = "subscriptions";
	private static final String QUALIFICATIONS		   = "qualifications";
	private static final String WEBTHEME               = "webtheme";
	private static final String JOURNAL_TEMPLATES	   = "journaltemplates";
	private static final String LAYOUT_TEMPLATES	   = "layouttemplates";
		
	private static final Object _publishToLiveMutex = new Object();
	
	private static final String FIELD_TO_ORDER = "modifiedDate";
	private static final String FIELD_ORDER    = "desc";	
	
	private static final String FRAMES_PUBLISH_XML					  = "<root refreshCache='%d'><param waterMark='%d' frames='%d' replacementContentTypes='%d'/></root>";
	private static final String FRAMES_PUBLISH_WATERMARK 			  = String.format(FRAMES_PUBLISH_XML, 1, 1, 0, 0);
	private static final String FRAMES_PUBLISH_FRAMES 				  = String.format(FRAMES_PUBLISH_XML, 1, 0, 1, 0);
	private static final String FRAMES_PUBLISH_REPLACE_CONTENTS_TYPES = String.format(FRAMES_PUBLISH_XML, 1, 0, 0, 1);
	private static final String FRAMES_PUBLISH_ALL					  = String.format(FRAMES_PUBLISH_XML, 0, 1, 1, 1);
	
	private static final String JOURNALARTICLE_SHARED 		= " ExpandoValue.data_ REGEXP '[[:<:]]%d[[:>:]]' ";
	private static final String JOURNALARTICLE_NOT_SHARED 	= " ExpandoValue.data_ = '%d' ";
	
	private static final String GET_NEW_SECTION_MODELS		= new StringBuilder(
		"SELECT concat(catalogpage.catalogpageid,'.',catalogelement.catalogelementid) id											\n").append(
		"FROM catalogpage 																											\n").append(
		"inner join catalogelement ON catalogpage.catalogpageid = catalogelement.catalogPageId										\n").append(
		"INNER JOIN layout ON (catalogpage.plid=layout.plid) 																		\n").append(
		"INNER JOIN xmlio_live ON (layout.plid=xmlio_live.localId AND xmlio_live.classNameValue='com.liferay.portal.model.Layout')  \n").append(
		"  WHERE layout.groupId = %1$d 																								\n").append(
		"    AND xmlio_live.status IN ('pending', 'error') 																			\n").append(
		"    and ITR_UTIL_IS_EMPTY(existInLive)																						\n").append(
		"    and catalogelement.catalogelementid in 																				\n").append(
		"        (																													\n").append(
		"            select headerelementId elementId																				\n").append(
		"            from sectionproperties																							\n").append(
		"              where plid IN (%2$s)																							\n").append(
		"                and sectionproperties.noheader = 0																			\n").append(
		"                and headerelementId is not null																			\n").append(
		"            union all     																									\n").append(
		"            select footerelementid elementId																				\n").append(
		"            from sectionproperties																							\n").append(
		"              where plid IN (%2$s)																							\n").append(
		"                and sectionproperties.nofooter = 0																			\n").append(
		"                and footerelementid is not null																			\n").append(
		"            union all     																									\n").append(
		"            select menuelementid elementId																					\n").append(
		"            from sectionproperties																							\n").append(
		"              where plid IN (%2$s)																							\n").append(
		"                and sectionproperties.nomenu = 0 																			\n").append(
		"                and menuelementid is not null																				\n").append(
		"        )																													\n").toString();
			
	private static final String COUNT_NOT_EXIST_IN_LIVE		= new StringBuilder(
		"select count(1) numNotExist							\n").append(
		"from XmlIO_Live										\n").append(
		"  where groupId = %d									\n").append(
		"    and classNameValue = '%s'							\n").append(
		"    and localId IN ('%s')								\n").append(
		"    and ITR_UTIL_IS_EMPTY(existInLive)					\n").toString();
	
	private static final String GET_JOURNALARTICLE_BY_GROUP	= String.format(new StringBuilder(
		 "SELECT  XmlIO_Live.id_																						\n").append(
		 "FROM JournalArticle 																							\n").append(
		 "INNER JOIN ExpandoValue ON(ExpandoValue.classPK=JournalArticle.id_)											\n").append(
		 "INNER JOIN ExpandoColumn ON (ExpandoColumn.columnId=ExpandoValue.columnId AND ExpandoColumn.name='%s')		\n").append(
		 "INNER JOIN XmlIO_Live ON (XmlIO_Live.classNameValue = '%s' AND XmlIO_Live.localId = JournalArticle.articleId)	\n").append(
		 "    WHERE %s																									\n").append(
		 "		AND XmlIO_Live.status IN ('%s','%s')\n").toString(), 
		 IterKeys.EXPANDO_COLUMN_NAME_SCOPEGROUPID, IterKeys.CLASSNAME_JOURNALARTICLE,
		 (PropsValues.ITER_SHARED_ARTICLES_ENABLED) ? JOURNALARTICLE_SHARED : JOURNALARTICLE_NOT_SHARED,
		 IterKeys.ERROR, IterKeys.PENDING);
	
	private static final String GET_VOCABULARY_BY_GROUP	= String.format(new StringBuilder(
		"SELECT 	XmlIO_Live.id_																																\n").append(
		"FROM Group_																																			\n").append(
		"INNER JOIN AssetVocabulary ON 	IF (typeSettings = 0, '', typeSettings) = 																				\n").append(
		"								IF (LOCATE('|',AssetVocabulary.NAME) > 0, SUBSTRING(AssetVocabulary.NAME, 1, LOCATE('|',AssetVocabulary.NAME)-1), '')	\n").append(
		"INNER JOIN XmlIO_Live ON (XmlIO_Live.localId = AssetVocabulary.vocabularyId AND XmlIO_Live.classNameValue = '%s')										\n").append(
		"	WHERE Group_.groupId = %%d																															\n").append(
		"		AND XmlIO_Live.status IN ('%s', '%s')\n").toString(), IterKeys.CLASSNAME_VOCABULARY, IterKeys.ERROR, IterKeys.PENDING);
	
	
	private static final String GET_PAGETEMPLATEID = "SELECT pageTemplateId FROM Designer_PageTemplate WHERE id_ in(%s)";
	
	private static final String COUNT_ADVOC_MODIFIED = String.format(new StringBuilder(
			"SELECT COUNT(*)																\n").append(
			"FROM advocabulary ad 															\n").append(
			"INNER JOIN Xmlio_Live l ON (l.localid=ad.vocabularyid AND classNameValue='%s') \n").append(
			"	WHERE globalid IN ('%%s') 													\n").toString(), IterKeys.CLASSNAME_VOCABULARY);
	
	private static final String GET_LAYOUT_4_PUBLISH_BY_FRIENDLYURL = new StringBuilder()
		.append("SELECT %s xmlio_live.localid \n")
		.append("FROM layout INNER JOIN xmlio_live \n")
		.append("\t ON (xmlio_live.localid=layout.plid AND xmlio_live.classnamevalue='%%1$s' ) \n")
		.append("WHERE layout.groupId = %%2$s \n")
		.append("  AND (xmlio_live.status='%%3$s' OR xmlio_live.status='%%4$s') \n")
		.append("  AND friendlyurl='%%5$s' \n").toString();
	
	private static final String DESIGN_PAGE_TEMPLATE_LAYOUT_NOT_PUBLISHED = new StringBuilder()
		.append("%s")
		.append("UNION ALL \n")
		.append("SELECT x.localid \n")
		.append("FROM designer_pagetemplate d INNER JOIN  xmlio_live x \n")
		.append("\t ON (x.localId = d.layoutId AND x.classnamevalue='%%1$s') \n")
		.append("WHERE d.groupId = %%2$s \n")
		.append(" AND (x.status='%%3$s' OR x.status='%%4$s') \n")
		.append(" AND %%6$s \n").toString();
	
	private static final String GET_CATALOGPAGE_LAYOUT_NOT_PUBLISHED = new StringBuilder()
		.append("%s")
		.append("UNION ALL \n")
		.append(" SELECT catalogpage.catalogpageid, xmlio_live.localid \n ")
		.append(" FROM catalogpage \n\t INNER JOIN layout ON (catalogpage.plid=layout.plid) \n ")
		.append("\t INNER JOIN xmlio_live ON (layout.plid=xmlio_live.localId AND xmlio_live.classNameValue='%%1$s') ")
		.append(" WHERE layout.groupId = %%2$s \n")
		.append("  AND (xmlio_live.status='%%3$s' OR xmlio_live.status='%%4$s') \n")
		.append("  AND catalogpage.catalogpageid IN ('%%6$s') \n").toString();
	
	private static final String PAGETEMPLATEID_COND = " d.pageTemplateId IN (%s) ";
	private static final String ID_COND = " x.id_=%s ";
		
	private static final String GET_LIVE_VOCABULARY_ID = new StringBuilder("SELECT livepoolid id FROM xmlio_livepool WHERE livechildid=%s").toString();
	
	private ThreadLocal<Boolean> _refreshRemoteCache 			= new ThreadLocal<Boolean>();
	private ThreadLocal<Node>    _groups2Updt 					= new ThreadLocal<Node>();
	private ThreadLocal<Boolean> _refreshGlobalJournalTemplates = new ThreadLocal<Boolean>();
	private ThreadLocal<Boolean> _refreshQualifications			= new ThreadLocal<Boolean>();
	private ThreadLocal<Boolean> _refreshMetadataAdvertisment	= new ThreadLocal<Boolean>();
	private ThreadLocal<Boolean> _refreshScheduledPublications	= new ThreadLocal<Boolean>();
// 	private ThreadLocal<Boolean> _rescheduleCacheInvalidation	= new ThreadLocal<Boolean>();
	
	
	private static final String GET_XMLIO_LIVE_IDS = "SELECT id_ from Xmlio_Live WHERE groupId=%d AND classNameValue='%s' AND localId IN ('%s')";
	
	public static final StringBuilder GET_XMLIO_LIVE_GLOBALIDS = new StringBuilder()
	.append("SELECT groupid, localId backid, globalId gid,	\n")
	.append( "(CASE classNameValue \n")
	.append( " WHEN '"+ IterKeys.CLASSNAME_PAGETEMPLATE + "' 	THEN 'pagetemplate'  \n")
	.append( " WHEN '"+ IterKeys.CLASSNAME_PAGECONTENT + "' 	THEN 'pagecontent'   \n")
	.append( " WHEN '"+ IterKeys.CLASSNAME_DLFILEENTRY + "' 	THEN 'dlfileentry'   \n")
	.append( " WHEN '"+ IterKeys.CLASSNAME_CATEGORY + "' 		THEN 'category'  	 \n")
	.append( " WHEN '"+ IterKeys.CLASSNAME_VOCABULARY + "' 		THEN 'vocabulary'  	 \n")
	.append( " WHEN '"+ IterKeys.CLASSNAME_PRODUCT + "' 		THEN 'suscription'   \n")
	.append( "WHEN '"+ IterKeys.CLASSNAME_LAYOUT + "' 			THEN 'section'   \n")
	.append("ELSE 'Group'		\n")
	.append(" END) class    \n")	
	.append(" FROM XmlIO_Live   \n")	
	.append(" WHERE    			\n");
	
	
	
	public static final String GET_XMLIO_LIVE_SITE_LOCAL_ID = new StringBuilder()
	.append("SELECT globalId,localId sitelocalid	\n")
	.append(" FROM XmlIO_Live  	 		\n")	
	.append(" WHERE globalId IN( '%s' )		\n")	
	.append(" AND classNameValue = '" +  IterKeys.CLASSNAME_GROUP + "'	\n").toString();	
	
	public static final StringBuilder GET_XMLIO_LIVE_LOCAL_IDS  = new StringBuilder()
	.append("SELECT xl.groupid, localId,  globalId gid, l.layoutId AS livelayoutid,	\n")
	.append( "(CASE classNameValue \n")
	.append( "WHEN '"+ IterKeys.CLASSNAME_PAGETEMPLATE + "' THEN 'pagetemplate'  \n")
	.append( "WHEN '"+ IterKeys.CLASSNAME_PAGECONTENT + "' 	THEN 'pagecontent'   \n")
	.append( "WHEN '"+ IterKeys.CLASSNAME_DLFILEENTRY + "' 	THEN 'dlfileentry'   \n")
	.append( "WHEN '"+ IterKeys.CLASSNAME_CATEGORY + "' 	THEN 'category'  	 \n")
	.append( "WHEN '"+ IterKeys.CLASSNAME_VOCABULARY + "' 	THEN 'vocabulary'  	 \n")
	.append( "WHEN '"+ IterKeys.CLASSNAME_PRODUCT + "' 		THEN 'suscription'   \n")
	.append( "WHEN '"+ IterKeys.CLASSNAME_LAYOUT + "' 		THEN 'section'   \n")
	.append("ELSE 'Group'		\n")
	.append("END) class    \n")	
	.append("FROM XmlIO_Live xl   \n")	
	.append("LEFT JOIN layout l ON(classNameValue='com.liferay.portal.model.Layout' AND localId=l.plid ) \n")
	.append("WHERE    			\n");
	
	
	private static final String GET_DESIGNER_PAGETEMPLATE_IDS = new StringBuilder()
	.append("SELECT Id_ as Id, pageTemplateId as localId	\n")
	.append("FROM designer_pagetemplate   \n")	
	.append("WHERE pageTemplateId IN  ( '%s' )	  \n").toString();
	
	private static final String GET_DESIGNER_PAGETEMPLATE_LOCALIDS = new StringBuilder()
	.append("SELECT Id_ as Id, pageTemplateId as localId	\n")
	.append("FROM designer_pagetemplate   \n")	
	.append("WHERE Id_ IN  ( '%s' )	  \n").toString();
	
	private static final String GET_DLFILEENTRY_IDS = new StringBuilder()
	.append("SELECT name as Id, fileEntryId AS localId	\n")
	.append("FROM dlfileentry    \n")	
	.append("WHERE fileentryId IN  ( '%s' )	  \n").toString();
	
	private static final String GET_DLFILEENTRY_LOCALIDS = new StringBuilder()
	.append("SELECT name as Id, fileEntryId AS localId	\n")
	.append("FROM dlfileentry    \n")	
	.append("WHERE NAME IN  ( '%s' )	  \n").toString();
	
	private static final String GET_PAGE_CONTENT_IDS = new StringBuilder()
	.append("SELECT id_ as Id, pagecontentId as localId	\n")
	.append("FROM news_pagecontent   \n")	
	.append("WHERE pageContentId IN  ( '%s' )	  \n").toString();
	
	private static final String GET_PAGE_CONTENT_LOCALIDS = new StringBuilder()
	.append("SELECT id_ as Id, pageContentId as localId	\n")
	.append("FROM news_pagecontent   \n")	
	.append("WHERE id_ IN  ( '%s' )	  \n").toString();
   
	private static final String GET_DEFAULT_SECTION_PROPERTIES = new StringBuilder()
	.append(" SELECT IFNULL(headerelementid,0) AS headerelementid, IFNULL(menuelementid,0) AS menuelementid, IFNULL(footerelementid,0) AS footerelementid, aboutid, autorss, autorssxsl, autorssframe, autorsscontenttype, autorssorderby \n")
	.append(" FROM sectionproperties INNER JOIN catalogelement \n\t")
	.append(" ON (sectionproperties.headerelementid=catalogelement.catalogelementid OR \n\t")
	.append(" sectionproperties.menuelementid=catalogelement.catalogelementid OR \n\t")
	.append(" sectionproperties.footerelementid=catalogelement.catalogelementid) \n")
	.append(" WHERE sectionproperties.plid IS NULL AND sectionproperties.groupid=%s \n\t")
	.append(" AND (sectionproperties.publicationdate IS NULL OR sectionproperties.modifieddate>sectionproperties.publicationdate) \n")
	.append(" %s")
	.append(" %s")
	.append(" LIMIT 1 \n").toString();
	
	private static final String SELECTED_CATALOGPAGEIDS = "AND catalogelement.catalogPageId IN ('%s') \n";
	
	private static final String CLAUSE_NOT_IN_CATALOGPAGE = new StringBuilder()
	.append(" AND 0 =\n (\n\t SELECT COUNT(*) \n\t")
	.append(" FROM sectionproperties INNER JOIN catalogelement \n\t")
	.append(" ON (sectionproperties.headerelementid=catalogelement.catalogelementid OR \n\t")
	.append(" sectionproperties.menuelementid=catalogelement.catalogelementid OR \n\t")
	.append(" sectionproperties.footerelementid=catalogelement.catalogelementid) \n")
	.append(" WHERE sectionproperties.plid IS NULL AND sectionproperties.groupid=%s \n\t")
	.append(" AND catalogelement.catalogPageId IN ('%s')\n ) \n").toString();
	
	
	private static final String UPDATE_DEFAULT_SECTION_PROPERTIES_PUB_DATE = "UPDATE sectionproperties SET publicationdate='%s' WHERE plid IS NULL AND groupId = %s";
	
	private static final String SELECT_PENDING_ELEMENTS = String.format(new StringBuilder(
		"-- Elementos pendientes que NO sean JournalArticles ni Vocabularios, ni elementos del POOL de estos					\n").append(	
		"SELECT l.id_ 																											\n").append(
		"FROM Xmlio_Live l 																										\n").append(
		"INNER JOIN Xmlio_LivePool lp ON l.id_ = lp.livechildid																	\n").append(
		"INNER JOIN Xmlio_Live l2 ON l2.id_ = lp.livepoolid 																	\n").append(
		"	WHERE l.STATUS   = 'pending' 																						\n").append(
		"		AND l2.STATUS != 'draft' 																						\n").append(
		"		AND l.groupid IN (%%1$d, %%2$d)																					\n").append(
		"		AND l2.classNameValue NOT IN ('%1$s','%2$s')																	\n").append(
		"																														\n").append(
		"UNION ALL																												\n").append(
		"																														\n").append(	
		"-- Elementos pendientes que sean JournalArticles, y elementos del POOL de este											\n").append(		
		"SELECT l.id_																											\n").append( 
		"FROM Xmlio_Live l 																										\n").append(
		"INNER JOIN Xmlio_LivePool lp ON l.id_ = lp.livechildid																	\n").append(
		"INNER JOIN Xmlio_Live l2  ON l2.id_ = lp.livepoolid 																	\n").append(
		"INNER JOIN JournalArticle ON l2.localId = JournalArticle.articleId														\n").append(
		"INNER JOIN ExpandoValue   ON ExpandoValue.classPK=JournalArticle.id_													\n").append(									
		"INNER JOIN ExpandoColumn  ON (ExpandoColumn.columnId=ExpandoValue.columnId AND ExpandoColumn.name='expandoScopeGroupId')\n").append(		
		"	WHERE ExpandoValue.data_ = '%%1$d' 																					\n").append(
		"		AND l.STATUS   = 'pending' 																						\n").append(
		"		AND l2.STATUS != 'draft' 																						\n").append(
		"		AND l.groupid IN (%%1$d, %%2$d)																					\n").append(
		"		AND l2.classNameValue = '%1$s'																					\n").append(
		"																														\n").append(
		"UNION ALL																												\n").append(
		"																														\n").append(
		"-- Elementos pendientes que sean Vocabularios, y elementos del POOL de este											\n").append(		
		"SELECT l.id_																											\n").append(																											
		"FROM Xmlio_Live l 																										\n").append(																					
		"INNER JOIN Xmlio_LivePool lp 	ON l.id_ = lp.livechildid																\n").append(																	
		"INNER JOIN Xmlio_Live l2  		ON l2.id_ = lp.livepoolid 																\n").append(
		"INNER JOIN Group_ 				ON (Group_.groupId = %%1$d)																\n").append(
		"INNER JOIN AssetVocabulary 	ON 	IF (Group_.typeSettings = 0, '', typeSettings) = 									\n").append(																				 
		"									IF (LOCATE('|',AssetVocabulary.NAME) > 0, SUBSTRING(AssetVocabulary.NAME, 1, LOCATE('|',AssetVocabulary.NAME)-1), '')	\n").append(	 
		"WHERE l.STATUS   = 'pending' 																							\n").append(													
		"		AND l2.STATUS != 'draft' 																						\n").append(														
		"		AND l.groupid IN (%%1$d, %%2$d)																					\n").append(
		"		AND l2.localId = AssetVocabulary.vocabularyId																	\n").append(														
		"		AND l2.classNameValue = '%2$s'																					\n").toString(),
		IterKeys.CLASSNAME_JOURNALARTICLE, IterKeys.CLASSNAME_VOCABULARY);

	private static final String SELECT_ORPHAN_LAYOUTS = String.format(new StringBuilder(
		"SELECT id_											\n").append(
		"FROM xmlio_live									\n").append(
		"LEFT JOIN Layout ON plid = xmlio_live.localId		\n").append(
		"	WHERE classNameValue IN ('%s', '%s')			\n").append(
		"		AND xmlio_live.groupId = %%d				\n").append(
		"		AND plid is null							\n").append(
		"		AND operation <> '%s'						\n").toString(), IterKeys.CLASSNAME_LAYOUT, IterKeys.CLASSNAME_PORTLET, IterKeys.DELETE);
	
	private boolean isRefreshRemoteCacheEnable()
	{
		return GetterUtil.getBoolean( _refreshRemoteCache.get(), true );
		 
	}
	private void enableRefreshRemoteCache(boolean value)
	{
		_refreshRemoteCache.set(value);
	}
	
	private Node getGroups2Updt()
	{
		return _groups2Updt.get();
	}
	private void setGroups2Updt(Node value)
	{
		_groups2Updt.set(value);
	}
	
	private boolean getRefreshGlobalJournalTemplates()
	{
		return GetterUtil.getBoolean( _refreshGlobalJournalTemplates.get(), false );
	}
	private void setRefreshGlobalJournalTemplates(boolean value)
	{
		_refreshGlobalJournalTemplates.set(value);
	}

	private boolean getRefreshQualifications()
	{
		return GetterUtil.getBoolean( _refreshQualifications.get(), false );
	}
	private void setRefreshQualifications(boolean value)
	{
		_refreshQualifications.set(value);
	}
	
	private boolean getRefreshMetadataAdvertisement()
	{
		return GetterUtil.getBoolean( _refreshMetadataAdvertisment.get(), false );
	}
	private void setRefreshMetadataAdvertisement(boolean value)
	{
		_refreshMetadataAdvertisment.set(value);
	}
	
	private boolean getRefreshScheduledPublications()
	{
		return GetterUtil.getBoolean( _refreshScheduledPublications.get(), false );
	}
	private void setRefreshScheduledPublications(boolean value)
	{
		_refreshScheduledPublications.set(value);
	}
	
//	private boolean getRescheduleCacheInvalidation()
//	{
//		return GetterUtil.getBoolean( _rescheduleCacheInvalidation.get(), false );
//	}
//	private void setRescheduleCacheInvalidation(boolean value)
//	{
//		_rescheduleCacheInvalidation.set(value);
//	}

	
	// GET FUNCTIONS
	public Live getLiveByGlobalId(long groupId, String classNameValue, String globalId) throws SystemException, NoSuchLiveException {
		return livePersistence.fetchByclassNameValueGlobalId(groupId, classNameValue, globalId);
	}	
	
	public List<Live> getLiveByGlobalId(String className, String globalId) throws SystemException, NoSuchLiveException {
		return livePersistence.findByglobalIdClassNameValue(className, globalId);
	}
	
	public List<Live> getLiveByLocalId(long groupId, String localId) throws SystemException, NoSuchLiveException {
		return livePersistence.findBylocalIdGroupId(groupId, localId);
	}	
	
	public Live getLiveByLocalId(long groupId, String classNameValue, String localId) throws SystemException, NoSuchLiveException {
		return livePersistence.fetchByclassNameValueLocalId(groupId, classNameValue, localId);
	}	
	
	public List<Live> getLiveByClassNameValue(String classNameValue) throws SystemException, NoSuchLiveException{
		return livePersistence.findByclassNameValue(classNameValue);
	}
	
	public List<Live> getLiveByStatus(String status) throws SystemException, NoSuchLiveException{
		return livePersistence.findBystatus(status);
	}
	
	public List<Live> getPendingAndErrorLiveByLocalId(long groupId, String localId) throws SystemException, NoSuchLiveException{
		List<Live> result = new ArrayList<Live>();
		result.addAll(livePersistence.findBylocalIdGroupIdStatus(groupId, localId, IterKeys.PENDING));
		result.addAll(livePersistence.findBylocalIdGroupIdStatus(groupId, localId, IterKeys.ERROR));
		
		return result;
	}
	
	public List<Live> getPendingAndErrorLiveByClassNameGroupId(long groupId, String classNameValue) throws SystemException, NoSuchLiveException, SecurityException, NoSuchMethodException 
	{
		List<Live> result = new ArrayList<Live>();
		List<Live> aux    = new ArrayList<Live>();
		long liveGroupId  = (IterKeys.CLASSNAME_JOURNALARTICLE.equals(classNameValue) ||
							 IterKeys.CLASSNAME_VOCABULARY.equals(classNameValue)) ? GroupMgr.getGlobalGroupId() : groupId;
		aux.addAll(livePersistence.findByclassNameValueGroupIdStatus(liveGroupId, classNameValue, IterKeys.PENDING));
		aux.addAll(livePersistence.findByclassNameValueGroupIdStatus(liveGroupId, classNameValue, IterKeys.ERROR));
		
		if (IterKeys.CLASSNAME_JOURNALARTICLE.equals(classNameValue))
		{
			// Se obtiene la lista de artículos pendientes de publicar que están paginados en el grupo actual
			String sql = String.format(GET_JOURNALARTICLE_BY_GROUP, groupId);
			_log.debug(sql);
			Document liveDom = PortalLocalServiceUtil.executeQueryAsDom( sql );
			
			// Solo aquellos elementos del Live que estén en el DOM se agregan a la publicación
			for (Live live : aux)
			{
				long existsJA = XMLHelper.getLongValueOf( liveDom, String.format("count(/rs/row[@id_='%d'])", live.getId()) );
				if (existsJA > 0)
					result.add(live);
			}
		}
		else if (IterKeys.CLASSNAME_VOCABULARY.equals(classNameValue))
		{
			// Se obtiene la lista de vocabularios pendientes de publicar en la delegación del grupo actual
			Document liveDom = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_VOCABULARY_BY_GROUP, groupId) );
			
			// Solo aquellos elementos del Live que estén en el DOM se agregan a la publicación
			for (Live live : aux)
			{
				if ( XMLHelper.getLongValueOf(liveDom, String.format("count(/rs/row[@id_='%d'])", live.getId())) > 0 )
					result.add(live);
			}
		}
		else if (IterKeys.CLASSNAME_LAYOUT.equals(classNameValue))
		{
			// Se ordenan las páginas de modo que antes estén antes los padres que los hijos
			LiveSorter.sort(aux, LiveSorterCriterion.LAYOUT_DEEP);
			result = aux;
		}
		else
		{
			result = aux;
		}
		
		return result;
	}
	
	public List<Live> getPendingAndErrorLiveByOperationClassName(String operation, String classNameValue) throws SystemException, NoSuchLiveException
	{
		List<Live> result = new ArrayList<Live>();
		result.addAll(livePersistence.findByclassNameValueOperationStatus(IterKeys.PENDING, classNameValue, operation));
		result.addAll(livePersistence.findByclassNameValueOperationStatus(IterKeys.ERROR, classNameValue, operation));
		
		return result;
	}
	
	public List<Live> getAllPendingAndErrorLive(long companyId, long groupId) throws Exception
	{
		return getAllPendingAndErrorLive(companyId, groupId, IterKeys.CLASSNAME_TYPES);
	}
	
	public String getLiveStatus(long groupId, String classNameValue, String localId){
		String status = "";
		try{
			Live live = livePersistence.fetchByclassNameValueLocalId(groupId, classNameValue, localId);
			if (live!=null)
				status = live.getStatus(); 
		}
		catch(Exception err){
			_log.error("Get Status failed", err);
		}
		return status;
	}
	
	public long countByStatus(long globalGroupId, long groupId, String status) throws SystemException{
		return livePersistence.countBygroupIdStatus(status, globalGroupId) + livePersistence.countBygroupIdStatus(status, groupId);
	}
	
	/**
	 * Recupera todos los elementos pendientes de publicar en el sistema, en el orden de envio adecuado
	 * @param companyId
	 * @param groupId
	 * @param userId
	 * @return
	 */
	public List<Live> getAllPendingAndErrorLive(long companyId, long groupId, String[] classNames) throws Exception
	{
		long globalGroupId = getGroupId(companyId, "");
		
		List<Live> result = new ArrayList<Live>();
		
		for (String type : classNames) 
		{
			ItemXmlIO itemXmlIO = XMLIOUtil.getItemByType(type);
			ErrorRaiser.throwIfNull(itemXmlIO);

			try
			{
				if (groupId != globalGroupId)
				{
					result.addAll(getPendingAndErrorLiveByClassNameGroupId(globalGroupId, type));
				}
				result.addAll(getPendingAndErrorLiveByClassNameGroupId(groupId, type));
			}
			catch(Exception err)
			{
				_log.error(err);
			}
		}
		return result;
	}

	
	/**
	 * Recupera todos los elementos Pool pendientes de publicar en el sistema, en el orden de envio adecuado
	 * @param companyId
	 * @param groupId
	 * @param userId
	 * @return
	 */
	public List<Live> getAllPendingAndErrorLivePool(long companyId, long groupId, String className, String localId){
		long globalGroupId = getGroupId(companyId, "");
		List<Live> pendingErrorLiveList = new ArrayList<Live>();
		List<Live> result = new ArrayList<Live>();
		try{
			if (!className.equals("") && localId.equals("")){
				if (groupId != globalGroupId){
					pendingErrorLiveList.addAll(getPendingAndErrorLiveByClassNameGroupId(globalGroupId, className));
				}
				pendingErrorLiveList.addAll(getPendingAndErrorLiveByClassNameGroupId(groupId, className));
			
			}
			else if (className.equals("") && !localId.equals("")){
				pendingErrorLiveList = getPendingAndErrorLiveByLocalId(groupId, localId);
				
			}
			else if(!className.equals("") && !localId.equals("")){
				Live live = getLiveByLocalId(groupId, className, localId);
				if (live.getStatus().equals(IterKeys.PENDING) || live.getStatus().equals(IterKeys.ERROR));
					pendingErrorLiveList.add(live);
			}
			else{
				pendingErrorLiveList = getAllPendingAndErrorLive(companyId, groupId, IterKeys.MAIN_CLASSNAME_TYPES_EXPORT);
			}
			for (Live pendingErrorLive : pendingErrorLiveList){
				try{
					LivePool livePool = LivePoolLocalServiceUtil.getLivePoolByChildId(pendingErrorLive.getId());
					if (livePool != null && livePool.getLiveParentId()==0)
						result.add(pendingErrorLive);
				}catch(Exception err){
					_log.error(err);
				}
		}
		}catch(Exception err){
			_log.error(err);
		}
		return result;
	}
	
	public List<Live> getUpdateAndCreateLiveByClassNameGroupId(long groupId, String classNameValue) throws SystemException, NoSuchLiveException {
		return getUpdateAndCreateLiveByClassNameGroupId(groupId, classNameValue, null);
	}
	
	public List<Live> getUpdateAndCreateLiveByClassNameGroupId(long groupId, String classNameValue, Date date) throws SystemException, NoSuchLiveException {
	 	List<Live> result = new ArrayList<Live>();
	 	if (date!=null){
	 		result.addAll(livePersistence.findByclassNameValueGroupIdOperationModifiedDate(groupId, classNameValue, IterKeys.CREATE, date));
	 		result.addAll(livePersistence.findByclassNameValueGroupIdOperationModifiedDate(groupId, classNameValue, IterKeys.UPDATE, date));
	 	}
	 	else{
	 		result.addAll(livePersistence.findByclassNameValueGroupIdOperation(groupId, classNameValue, IterKeys.CREATE));
	 		result.addAll(livePersistence.findByclassNameValueGroupIdOperation(groupId, classNameValue, IterKeys.UPDATE));
	 	}
		return result;
	}
	
	public List<Live> getLiveByParentId(long parentId) throws SystemException{	
		return (getLiveListFromLivePoolList(LivePoolLocalServiceUtil.getLivePoolListByParentId(parentId)));	
	}
	
	public List<Live> getLiveByPoolIdParentId(long poolId, long parentId) throws SystemException{	
		return (getLiveListFromLivePoolList(LivePoolLocalServiceUtil.getLivePoolListByPoolIdParentId(poolId, parentId)));	
	}
	
	public Live getParent(Live live){
		try{
			LivePool lp = LivePoolLocalServiceUtil.getLivePoolByChildId(live.getId());
			if (lp.getLiveParentId() != 0){
				return livePersistence.findByPrimaryKey(lp.getLiveParentId());
			}
		}
		catch(Exception err){;}
		return null;
	}
	
	public List<Live> getParentList(Live live){
		try{
			return (getLiveListFromLivePoolList(LivePoolLocalServiceUtil.getLivePoolListByChildId(live.getId())));
		}
		catch(Exception err){
			return new ArrayList<Live>();
		}
	}
	
	/**
	 * Recupera los miembros del pool, asumiendo que el parámetro live es un pool
	 * @param live
	 * @return
	 */
	public List<Live> getPoolMemberList(Live live){
		try{
			return (getLiveListFromLivePoolList(LivePoolLocalServiceUtil.getLivePoolListByPoolId(live.getId())));
		}
		catch(Exception err){
			return new ArrayList<Live>();
		}
	}
	
	// ADD FUNCTIONS
	public Live add(Live st, String environment) throws SystemException, PortalException {
		return add(st.getClassNameValue(), st.getGroupId(), st.getGlobalId(), st.getLocalId(), st.getOperation(),
				st.getStatus(), st.getModifiedDate(), environment);
	}
	
	//Para UPDATE Y DELETE no se requieren poolId y parentId
	public Live add(String classNameValue, long groupId, String globalId, String localId, String operation, String status, Date modifiedDate, 
			String environment) throws SystemException, PortalException {
		return add(classNameValue, groupId, 0, 0, globalId, localId, operation, status, modifiedDate, environment);
	
	}
	
	public Live add(String classNameValue, long groupId, long poolId, long parentId, String globalId, 
			String localId, String operation, String status, Date modifiedDate, 
			String environment) throws SystemException, PortalException {
	
		return add(classNameValue, groupId, poolId, parentId, globalId, 
				localId, operation, status, modifiedDate, environment, false);
	}
	
	public Live add(String classNameValue, long groupId, long poolId, long parentId, String globalId, 
			String localId, String operation, String status, Date modifiedDate, 
			String environment, boolean overridePool) throws SystemException, PortalException {
		
		Live live = null;
		
		//Check if the environment is correct. In other case, does nothing.
		if (IterLocalServiceUtil.getEnvironment().equals(environment))
		{		
			//There are two different environments with different behavior.
			if (environment.equals(IterKeys.ENVIRONMENT_PREVIEW))
			{
				live = addFromPreview(classNameValue, groupId, poolId, parentId, globalId, localId, operation, status, modifiedDate, overridePool);
			}
			else if (environment.equals(IterKeys.ENVIRONMENT_LIVE))
			{
				live = addFromLive(classNameValue, groupId, poolId, parentId, globalId, localId, operation, status, modifiedDate);
			}
		}
		
		return live;
	}
	
	public void updatePublicationDate(String classNameValue, long groupId)
	{
		if ( !IterKeys.UNUPDATE_PUBLICATION_DATE_CLASSNAMES.contains(classNameValue) &&
			 PropsValues.ITER_ENVIRONMENT.equals(WebKeys.ENVIRONMENT_PREVIEW) )
		{
			long scopeGroupId = groupId;
			
			try
			{
				try
				{
					// Se toma el scopeGroupId del tema, pero si por aluna razón fallase 
					// (Ex. actualización de las preferencias de los portlets de Iter) se toma la del grupo del Xmlio_Live 
					Object themeDisplay = IterRequest.getAttribute(WebKeys.THEME_DISPLAY);
					// El atributo themeDisplay puede no venir
					if (null != themeDisplay)
					{						
						scopeGroupId = ((ThemeDisplay)themeDisplay).getScopeGroupId();
					}
				}
				catch (Exception e)
				{
					_log.debug(e);
				}

				GroupMgr.updatePublicationDate(scopeGroupId, new Date());
			}
			catch (Exception e)
			{
				_log.debug(e);
				_log.error(e.toString());
			}
		}
	}
	
	/**
	 * 
	 * @param classNameValue
	 * @param groupId
	 * @param globalId
	 * @param localId
	 * @param operation
	 * @param status
	 * @param modifiedDate
	 * @return
	 * @throws SystemException
	 * @throws PortalException
	 */
	private Live addFromPreview(String classNameValue, long groupId, long poolId, long parentId,
			String globalId, String localId, String operation, String status,
			Date modifiedDate, boolean overridePool) throws SystemException, PortalException
	{
		Live live = null;

		try 
		{
			updatePublicationDate(classNameValue, groupId);
			
			live = getLiveByLocalId(groupId, classNameValue, localId);
		
			// Si ya existe una entrada para ese elemento en la tabla
			String prevOperation 	= live.getOperation();
			String prevStatus 		= live.getStatus();			
			String newOperation 	= operation;
			String newStatus 		= status;			
			long newPoolId 			= -1;
			long newParentId 		= -1;
			Date performDate 		= live.getPerformDate();
			boolean ignore 			= false;
			
			// Los elementos solo se ponen en Draft cuando no se han publicado nunca.
			// ITER-1075 Artículos sin el check de publicable se publican al Live
			// http://jira.protecmedia.com:8080/browse/ITER-1075?focusedCommentId=45432&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-45432
			if (status.equals(IterKeys.DRAFT) && "S".equalsIgnoreCase(live.getExistInLive()))
			{
				newStatus = IterKeys.PENDING;
			}
			
			if (IterKeys.CREATE.equals(prevOperation) && (IterKeys.PENDING.equals(prevStatus) || IterKeys.ERROR.equals(prevStatus))) 
			{
				if (IterKeys.CREATE.equals(operation)) 
				{
					newOperation= IterKeys.CREATE;
					//newStatus = IterKeys.PENDING;
					newPoolId = poolId;
					newParentId = parentId;	
				}
				else if (IterKeys.UPDATE.equals(operation)) 
				{
					newOperation= prevOperation;
					newStatus = prevStatus;
				}
				else if (IterKeys.DELETE.equals(operation)) 
				{		
					//Si lo pongo a delete y nunca se ha publicado, lo borro.					
					ignore = (performDate == null || performDate.equals(""));
				}
			}
			else if(IterKeys.DELETE.equals(prevOperation))
			{				
				//newOperation= IterKeys.DELETE;
				newStatus = prevStatus;				
			}
			else
			{
				if (IterKeys.CREATE.equals(operation)) 
				{
					newOperation= IterKeys.CREATE;
					//newStatus = IterKeys.PENDING;
					newPoolId = poolId;
					newParentId = parentId;		
				}
				else if (IterKeys.UPDATE.equals(operation))
				{
					if (! IterKeys.PENDING.equals(prevStatus) && ! IterKeys.ERROR.equals(prevStatus))
					{
						newOperation= IterKeys.UPDATE;						
					}
				}
				else if (IterKeys.DELETE.equals(operation))
				{					
					//Si lo pongo a delete y nunca se ha publicado, lo borro.
					ignore = (performDate == null || performDate.equals(""));
				}
			}	
			
			if(ignore)
			{
				 deleteLive(live);
			}
			else
			{
				updateLive(live, newOperation, newStatus, new Date(), newPoolId, newParentId, overridePool);
			}
			
		} 
		catch (Exception e) 
		{
			// Si no existen entradas para ese elemento en la tabla y no es una operación de borrado (CREATE)
			if (!operation.equals(IterKeys.DELETE))
			{			
				live = createLive(classNameValue, groupId, poolId, parentId, globalId, localId, IterKeys.CREATE, status, modifiedDate);
			}
		}

		return live;
	}
	
	/**
	 * 
	 * @param classNameValue
	 * @param groupId
	 * @param globalId
	 * @param localId
	 * @param operation
	 * @param status
	 * @param modifiedDate
	 * @return
	 * @throws SystemException
	 * @throws PortalException
	 */
	private Live addFromLive(String classNameValue, long groupId, long poolId, long parentId,
			String globalId, String localId, String operation, String status,
			Date modifiedDate) throws SystemException, PortalException 
	{
		Live live = null;

		try 
		{
			live = getLiveByGlobalId(groupId, classNameValue, globalId);

			if (live != null)
			{
				// Element already exists in Live.
				if (IterKeys.UPDATE.equals(operation) || IterKeys.CREATE.equals(operation)) 
				{
					live.setOperation(operation);
					live.setStatus(status);
					
					// Se actualiza el localId, porque si ha fallado la publicación anterior puede haberse quedado, 
					// para el mismo elemento del BACK (mismo globalId), una referencia antigua o inexistente de localId en el LIVE.
					// En ocasiones se encuentran registros cuyo localId no existe, de esta forma se solucionaría
					live.setLocalId(localId);
					livePersistence.update(live, false);			
				}
				else if (IterKeys.DELETE.equals(operation)) 
				{				
					//Delete entry from table
					deleteLive(live);
				}
			}
			else if (!IterKeys.DELETE.equals(operation))
			{
				live = createLive(classNameValue, groupId, poolId, parentId, globalId, localId, IterKeys.CREATE, status, modifiedDate);
			}
		} 
		catch (Exception e) 
		{
			if (!IterKeys.DELETE.equals(operation))
				live = createLive(classNameValue, groupId, poolId, parentId, globalId, localId, IterKeys.CREATE, status, modifiedDate);
		}

		return live;
	}
	
	/**
	 * 
	 * @param classNameValue
	 * @param groupId
	 * @param globalId
	 * @param localId
	 * @param operation
	 * @param status
	 * @param modifiedDate
	 * @return
	 * @throws SystemException
	 */
	private Live createLive(String classNameValue, long groupId, long poolId, long parentId, 
			String globalId, String localId, String operation, 
			String status, Date modifiedDate) throws SystemException{
		Live live = null;
		
		//new Live entry (CREATE)
		long id = counterLocalService.increment();
		live = livePersistence.create(id);
		live.setClassNameValue(classNameValue);
		live.setGroupId(groupId);		
		live.setGlobalId(globalId);
		live.setLocalId(localId);
		live.setOperation(operation);			
		live.setStatus(status);
		live.setModifiedDate(modifiedDate);
		livePersistence.update(live, false);
		
		/* Si poolId = 0 --> El elemento no se asocia a ningún Pool. Se asociará posteriormente (ej. DLFileEntry y Content)
		 * Si poolId = -1 --> El elemento pertenece a su propio Pool
		 * Si parentId = 0 --> El elemento no tiene padre (es raíz de un Pool) */
		
		if (poolId !=0){
			//Add LivePool Entry
			poolId = poolId == -1 ? id : poolId;
			LivePoolLocalServiceUtil.createLivePool(poolId, parentId, live.getId(), false);
			//Update parents
			updateLiveParents(live);
		}
		
		return live;		
	}
	
	
	// UPDATE FUNCTIONS
	public void updateStatus(String oldStatus, String newStatus, String processId)
	{
		try
		{
			String pubType = LiveControlLocalServiceUtil.getLiveControlInProcess(processId).getType();
			
			if (pubType.equals(IterKeys.MASIVE)){				
				
				List<Live> liveElems = LiveLocalServiceUtil.getLiveByStatus(oldStatus);				
				for (Live live:liveElems)
				{
					try{
						live.setStatus(newStatus);
						livePersistence.update(live, false);						
					}
					catch(Exception err)
					{
						_log.error(err);
					}
				}
			}else{
				List<LivePool> poolList = LivePoolLocalServiceUtil.getLivePoolListByProcessId(processId);
				for (LivePool lp: poolList){
					Live liveElem = LiveLocalServiceUtil.getLive(lp.getLiveChildId());
					if (liveElem.getStatus().equals(oldStatus)){
						try{
							liveElem.setStatus(newStatus);
							livePersistence.update(liveElem, false);						
						}
						catch(Exception err)
						{
							_log.error(err);
						}
					}
				}
			}
						
		}
		catch(Exception err)
		{
			_log.error(err);
		}
	}
	
	@Deprecated
	public void updateStatus(String oldStatus, String newStatus)
	{
		try
		{
			List<Live> results = getLiveByStatus(oldStatus);
			for (Live result:results)
			{
				result.setStatus(newStatus);
				livePersistence.update(result, false);
			}
		}
		catch(Exception err)
		{
			_log.error(err);
		}
	}
	
	public String updateStatus(long groupId, String classNameValue, String globalId, String status) throws NoSuchLiveException, SystemException{
		return updateStatus (groupId, classNameValue, globalId, status, null);
	}
	
	public String updateStatus(long groupId, String classNameValue, String globalId, String status, Date performDate) throws NoSuchLiveException, SystemException{
		return updateStatus (groupId, classNameValue, globalId, status, null, performDate);	
	}
	
	/**
	 * Actualiza el estado de Live
	 * @return el localId de la entrada actualizada
	 */
	public String updateStatus(long groupId, String classNameValue, String globalId, String status, String preStatus, Date performDate) throws NoSuchLiveException, SystemException
	{ 
		Live row = getLiveByGlobalId(groupId, classNameValue, globalId);
		
		// Puede llegar classNameValue = 'vocabulary' cuando en realidad es 'category', por aquello del trampeo de publicar una categoría como un vocabulario.
		if (null == row){
			row = getLiveByGlobalId(groupId, IterKeys.CLASSNAME_CATEGORY, globalId);
		}
		
		//Solo actualiza si no se especifica estado anterior o si el estado anterior especificado coincide con el del registro
		if (preStatus == null || (row != null && row.getStatus().equals(preStatus))){
			//Si se actualiza con DONE, se comprueba si el estado anterior era DELETE y en tal caso, borra la fila
			if(status.equals(IterKeys.DONE) && row.getOperation().equals(IterKeys.DELETE)){
				deleteLive(row);
			}
			//Solo se actualiza a "Processing" si el estado anterior No es "done"
			else if(!row.getStatus().equals(IterKeys.DONE) || !status.equals(IterKeys.PROCESSING)){
				//Si nos llega un interrupt significa que el elemento se ha creado correctamente, 
				//pero ha fallado alguna de sus dependencias. Por lo que lo ponemos a UPDATE para 
				//posteriores publicaciones.	
				if(status.equals(IterKeys.INTERRUPT))
				{
					row.setOperation(IterKeys.UPDATE);
					row.setStatus(IterKeys.ERROR);
				}
				else if(!row.getStatus().equals(IterKeys.CORRUPT))
				{ //Los corruptos por la exportación solo se actualizan en el updateFromRemoteLog						
					row.setStatus(status);
				}
				if (performDate != null) 
					row.setPerformDate(performDate);
				livePersistence.update(row, false);				
			}
		}
		
		return row.getLocalId();
	}
	
	public void updateGlobalId(long groupId, String classNameValue, String localId, String globalId) throws NoSuchLiveException, SystemException{
			
		Live row = getLiveByLocalId(groupId, classNameValue, localId);
		row.setGlobalId(globalId);
		livePersistence.update(row, false);		
	}
	
	public void updateLive(Live live, String operation, String status, Date modDate, long livePoolId, long liveParentId, boolean overridePool) throws SystemException{
		live.setOperation(operation);
		live.setStatus(status);
		live.setModifiedDate(modDate);
		livePersistence.update(live, false);
		
		/* Si poolId = 0 --> El elemento no se asocia a ningún Pool. Se asociará posteriormente (ej. DLFileEntry y Content)
		 * Si poolId = -1 --> El elemento pertenece a su propio Pool
		 * Si parentId = 0 --> El elemento no tiene padre (es raíz de un Pool) */
		
		if (livePoolId !=0){
			//Add LivePool Entry
			LivePoolLocalServiceUtil.createLivePool(livePoolId, liveParentId, live.getId(), overridePool);
		}
		
		//Update parents
		updateLiveParents(live);		
	}
	
	public void updateLive(long groupId, String classNameValue, String localId, String operation, String status, Date modDate) throws SystemException, NoSuchLiveException{
		Live live = getLiveByLocalId(groupId, classNameValue, localId);
		
		live.setOperation(operation);
		live.setStatus(status);
		live.setModifiedDate(modDate);
		livePersistence.update(live, false);
		
		//Update parents
		updateLiveParents(live);		
	}	
	
	public void setLiveArticleToPending(String articleId) throws SystemException
	{
		List<Live> liveItems = livePersistence.findBylocalIdGroupId(GroupMgr.getGlobalGroupId(), articleId);
		for (Live live : liveItems)
		{
			//Only set to "update" if is not "pending" or "error"
			if (!live.getStatus().equals(IterKeys.PENDING) && !live.getStatus().equals(IterKeys.ERROR))
			{
				//Only change operation if previous state is not DRAFT
				if(!live.getStatus().equals(IterKeys.DRAFT))
				{
					live.setOperation(IterKeys.UPDATE);
					live.setStatus(IterKeys.PENDING);	
				}
				
				live.setModifiedDate(new Date());
				livePersistence.update(live, false);
			}
		}
	}
	
	private void updateLiveParents(Live live) throws SystemException
	{
		//este estatus solo puede ser CREATE o DRAFT
		String status = live.getStatus();
		
		List<LivePool> lpList = LivePoolLocalServiceUtil.getLivePoolListByChildId(live.getId()); 
		for(LivePool lp : lpList)
		{
			//Update parent in LIVE
			try
			{
				if (lp.getLiveParentId() != 0)
				{
					Live liveParent = livePersistence.findByPrimaryKey(lp.getLiveParentId());
					//Only set to "update" if is not "pending" or "error"
					if (!liveParent.getStatus().equals(IterKeys.PENDING) && !liveParent.getStatus().equals(IterKeys.ERROR))
					{
						//Only change operation if previous state is not DRAFT
						if(!liveParent.getStatus().equals(IterKeys.DRAFT))
						{
							liveParent.setOperation(IterKeys.UPDATE);
							liveParent.setStatus(IterKeys.PENDING);	
						}			
						
						//Esto se usa para quitar el estado de DRAFT al articulo o a la campaña. 
						//Pero solo lo pueden hacer los PageContent y PageCampaign, respectivamente.
						if(!status.equals(IterKeys.DRAFT) && ArrayUtils.contains(IterKeys.CUSTOMFIELD_CLASSNAME_DRAFTTOPENDING, live.getClassNameValue()))
						{
							liveParent.setStatus(IterKeys.PENDING);	
						}
						
						// ITER-1063 No se publican algunas noticias
						// En las trazas se ha visto que el origen del problema es que llega un "bean sucio" del XMLIO_Live del JournalArticle, con 
						// estado a draft.
						// La solución será añadir una comprobación más 
							// El estado actual del padre es draft
						if (liveParent.getStatus().equals(IterKeys.DRAFT) 						&& 
							// El padre es un JournalAricle
							liveParent.getClassNameValue().equals(IterKeys.CLASSNAME_JOURNALARTICLE))
						{
							String msg;
							
							// La comprobación adicional indica que NO puede tener estado draft
							if (!canSetJournalArticleToDraft(liveParent))
							{
								 msg = String.format("The article %s will be setted to pending because was draft (%s.localid = %s).", 
											liveParent.getLocalId(), live.getClassNameValue(), live.getLocalId());
								
								 _logJA.warn(msg);
								liveParent.setStatus(IterKeys.PENDING);	
							}
							else if (PropsValues.IS_PREVIEW_ENVIRONMENT)
							{
								 msg = String.format("The article %s is draft and does'nt have online PageContent (%s.localid = %s).", 
											liveParent.getLocalId(), live.getClassNameValue(), live.getLocalId());
								 _logJA.info(msg);
							}
						}
						
						liveParent.setModifiedDate(new Date());
						livePersistence.update(liveParent, false);
					}
					else if (canSetJournalArticleToDraft(liveParent, live))
					{
						liveParent.setStatus(IterKeys.DRAFT);
						
						liveParent.setModifiedDate(new Date());
						livePersistence.update(liveParent, false);
					}
					updateLiveParents(liveParent);
				}
			}
			catch(Exception err){}
		}		
	}

	private boolean canSetJournalArticleToDraft(Live live) throws SystemException
	{
		boolean canSet = false;
		
		if (PropsValues.IS_PREVIEW_ENVIRONMENT 									&&
			live.getClassNameValue().equals(IterKeys.CLASSNAME_JOURNALARTICLE)	&&
			!"S".equalsIgnoreCase(live.getExistInLive())
		   )
		{
			canSet = true;
			
			// ITER-700	NAI172859 / Artículos sin el check de publicable aparecen publicados en el live
			// Si el artículo nunca ha sido publicado y no tiene ningún pageContent online es NO publicable
			List<PageContent> list = PageContentLocalServiceUtil.getPageContentsByContentId(live.getLocalId());
			for (PageContent pageContent : list)
			{
				// El artículo no tiene PageContent publicables
				if (pageContent.isOnline())
				{
					canSet = false;
					break;
				}
			}
		}
		
		return canSet;
	}
	
	/**
	 * Se puede poner a DRAFT si 					<br/>
	 * 	1. Tiene una estructura de MLN 				<br/>
	 *	2. El artículo no se ha creado o			<br/>	
	 *  3. Es PREVIEW y:							<br/>
	 *    3.1 Ya existe el artículo  				<br/>
	 *    3.2 Ninguno de los PageContent es online 	<br/>
	 *    
	 * @param article
	 * @return
	 * @throws SystemException
	 * @throws NoSuchLiveException
	 */
	public boolean canSetJournalArticleToDraft(JournalArticle article) throws SystemException, NoSuchLiveException
	{
		boolean isMLN  = ArrayUtil.contains(IterKeys.MILENIUM_STRUCTURES, article.getStructureId());
		Live live 	   = getLiveByLocalId(article.getGroupId(), IterKeys.CLASSNAME_JOURNALARTICLE, article.getArticleId());
		
		boolean canSet = isMLN && (live == null || canSetJournalArticleToDraft(live));
		
		// Si está activada la traza y hay un cambio a TRUE se registra
		if (canSet && (live == null || !live.getStatus().equals(IterKeys.DRAFT)))
		{
			String msg = String.format("The article %s will be setted to draft.", article.getArticleId());
			_logJA.warn(msg);
		}

		return canSet;
	}
	
	private boolean canSetJournalArticleToDraft(Live liveParent, Live live) throws SystemException
	{
		boolean canSet = (live.getClassNameValue().equals(IterKeys.CLASSNAME_PAGECONTENT) && 
							canSetJournalArticleToDraft(liveParent));
		
		// Si está activada la traza y hay un cambio a TRUE se registra
		if (canSet && !liveParent.getStatus().equals(IterKeys.DRAFT))
		{
			String msg = String.format("The article %s will be setted to draft (%s.localid = %s).", liveParent.getLocalId(), live.getClassNameValue(), live.getLocalId());
			_logJA.warn(msg);
		}
		return canSet;
	}

	public void updateLocalId(Live live, String newLocalId) throws SystemException
	{		
		live.setLocalId(newLocalId);
		livePersistence.update(live, false);
	}
	
	
	public void updateLocalId(long oldGroupId, String oldClassNameId, String oldLocalId, String newLocalId) throws SystemException, NoSuchLiveException{		
		Live oldLive = getLiveByLocalId(oldGroupId, oldClassNameId, oldLocalId);
		oldLive.setLocalId(newLocalId);
		livePersistence.update(oldLive, false);
	}
	
	public void changeLiveStatus(long groupId, String classNameValue, String localId, String status) throws SystemException, NoSuchLiveException{
		Live live = LiveLocalServiceUtil.getLiveByLocalId(groupId, classNameValue, localId);
		live.setStatus(status);
		livePersistence.update(live, false);
	}
	
	public void updateErrorLog(long groupId, String classNameValue, String globalId, String status, String errorLog)throws NoSuchLiveException, SystemException{
		Live row = getLiveByGlobalId(groupId, classNameValue, globalId);
		
		if( status.equals(IterKeys.ERROR) || status.equals(IterKeys.INTERRUPT) ){
			//Si se han producido errores en la importación, se añaden a los de la exportación.
			row.setErrorLog(row.getErrorLog() + (row.getErrorLog().equals("")?"":";") + errorLog);
		}else if( status.equals(IterKeys.DONE) && !row.getStatus().equals(IterKeys.ERROR)){
			//Si no ha habido error ni en la exportación ni en la importación, limpiamos el error.
			row.setErrorLog("");			
		}	
		
		livePersistence.update(row, false);
	}
	
	public void setError(long liveId, String errorLog){
		Live row;
		
		try {
			row = getLive(liveId);		
		
			row.setErrorLog(errorLog);			
			livePersistence.update(row, false);
			
		} catch (PortalException e) {
			_log.error(e);
		} catch (SystemException e) {
			_log.error(e);
		}		
	}
	
	public void clearLog(long liveId){
		Live row;
		try {
			row = getLive(liveId);	
			
			row.setErrorLog("");
			
			livePersistence.update(row, false);
		} catch (PortalException e) {
			_log.error(e);
		} catch (SystemException e) {
			_log.error(e);
		}
	}
	
	// DELETE FUNCTIONS
	/**
	 * Delete all xmlio_live entries
	 * @param groupId	 
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	public void deleteLive(long groupId, long companyId) throws SystemException, PortalException{		
		Company company = CompanyLocalServiceUtil.getCompany(companyId);
		long globalGroupId = company.getGroup().getGroupId();
		List<Live> liveToRemove = new ArrayList<Live>();
		liveToRemove.addAll(livePersistence.findBygroupId(globalGroupId));
		liveToRemove.addAll(livePersistence.findBygroupId(groupId));
		for (Live live : liveToRemove){
			deleteLive(live);
		}
	}
	
	/**
	 * Delete an specific entry from xmlio_live
	 * @param groupId
	 * @param classNameValue
	 * @param globalId
	 * @throws SystemException
	 * @throws PortalException
	 */
	public void deleteLive(long groupId, String classNameValue, String globalId)throws SystemException, PortalException
	{
		// Los borrados de artículos no actualizan la fecha de última modificación en el BACK y por tanto no se actualizan
		// los teasers en modo autowidgets.
		// http://jira.protecmedia.com:8080/browse/ITER-20?focusedCommentId=14485&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-14485
		updatePublicationDate(classNameValue, groupId);
		deleteLive( getLiveByGlobalId(groupId, classNameValue, globalId) );
	}
	
	public void deleteLiveById(long liveId) throws SystemException, PortalException
	{
		deleteLive(getLive(liveId));
	}
	
	public void deleteLive(Live live) throws SystemException
	{
		if (live != null)
		{
			//Elimina dependencias de LivePool
			LivePoolLocalServiceUtil.removeLivePool(live.getId());
			//Elimina el elemento Live
			livePersistence.remove(live);
		}
	}
	
	/**
	 * Elimina todas las secciones referenciadas en el XMLIO_Live pero que ya no existen en el sistema, 
	 * eliminándolas así de cualquier publicación
	 * 
	 * @param groupId
	 * @throws PortalException 
	 * @throws SystemException 
	 * @throws NumberFormatException 
	 * @see http://jira.protecmedia.com:8080/browse/ITER-865?focusedCommentId=43288&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-43288
	 */
	private void deleteOrphanLayouts(long groupId) throws NumberFormatException, SystemException, PortalException
	{
		String sql = String.format(SELECT_ORPHAN_LAYOUTS, groupId);
		if (_log.isDebugEnabled())
			_log.debug( String.format("deleteOrphanLayouts: \n %s", sql) );
		
		List<Object> ids = PortalLocalServiceUtil.executeQueryAsList(sql);
		for (Object id : ids)
		{
			deleteLiveById( Long.valueOf(String.valueOf(id)) );
		}
	}
	
	public void deleteLive(BaseModel<?> model) throws Exception
	{
		ItemXmlIO item = XMLIOUtil.getItemByType(model.getClass().getCanonicalName(), null);
		item.deleteLiveEntry(model);
	}
	
	// PUBLISH LIVE
	
	//Encapsula las llamadas desde el BACK y no desde Milenium
	public String publishToLiveGUI(long companyId, long groupId, long userId, String className, String [] liveItemIds, List<String> errors, long scopeGroupId)
	{
		_log.trace("In formatResultsForFlex.publishToLiveGUI");
		
		String publishLog = "";
		
		try
		{
			publishLog = this.publishToLive(companyId, groupId, userId, null, className, liveItemIds, false, scopeGroupId);
		}
		catch(Throwable e)
		{
			String error = "xmlio-live-publish-error";
			
			// Error in the HTTP protocol
			if (e instanceof ClientProtocolException)
				error = "xmlio-live-publish-error-client-protocol";
			
			// Character Encoding is not supported.
			else if (e instanceof UnsupportedEncodingException)
				error = "xmlio-live-publish-error-encode";

			// Invalid remote port 
			else if (e instanceof NumberFormatException)
				error = "xmlio-live-publish-error-remote-port";
			
			// No existe live 
			else if (e instanceof NoSuchLiveException)
				error = "xmlio-live-publish-error-live";
			
			// Error al conectar con el tomcat de live 
			else if (e instanceof HttpHostConnectException)
				error = "xmlio-live-publish-error-conection";

			// Timeout de conexión
			else if (e instanceof ConnectTimeoutException)
				error = "xmlio-live-publish-error-connect-timeout";

			// Timeout de espera
			else if (e instanceof SocketTimeoutException)
				error = "xmlio-live-publish-error-socket-timeout";

			// Sin respuesta http
			else if (e instanceof NoHttpResponseException)
				error = "xmlio-live-publish-error-no-http-response";
			
			// Error al leer el log que viene de live
			else if (e instanceof DocumentException)
				error = "xmlio-live-publish-error-remote-log";
			
			// Error controlado por Iter
			else if (e instanceof ServiceError)
				error = ((ServiceError)e).getErrorCode(); 
			
			errors.add(error);
			_log.error(e);
		}
		
		return publishLog;
	}
	
	
	public String publishToLive(long companyId, long groupId, long userId, String processId, String className, String [] liveItemIds, boolean milenium, long scopeGroupId) throws Throwable
	{
		return publishToLive(companyId, groupId, userId, processId, className, liveItemIds, milenium, scopeGroupId, DEFAULT_CONTENT_TO_PUBLISH); 
	}	
	
	public String publishToLive(long companyId, long groupId, long userId, String processId, String className, String [] liveItemIds, boolean milenium, long scopeGroupId, String contentsToPublish) throws Throwable
	{
		_log.trace("In formatResultsForFlex.publishToLive");
		
		String result;
		boolean bloquing = GetterUtil.getBoolean( PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_XMLIO_BLOCKING_PUBLICATION), false );
				
		if (bloquing)
		{
			// Sincronización estática. Solo una instancia realiza la publicación.
			synchronized(_publishToLiveMutex)
			{
				result = publishToLiveNonBloquing(companyId, groupId, userId, processId, className, liveItemIds, milenium, scopeGroupId, contentsToPublish);
			}
		}
		else
		{
			result = publishToLiveNonBloquing(companyId, groupId, userId, processId, className, liveItemIds, milenium, scopeGroupId, contentsToPublish);
		}
		return result;
	}
	
	/**
	 * 
	 * @param companyId
	 * @param groupId
	 * @param userId
	 * @param processId
	 * @param className
	 * @param liveItemIds
	 * @param milenium
	 * @param scopeGroupId
	 * @return
	 * @throws Throwable
	 */
	// Proceso que realiza la mayoría de las importaciones
	public String publishToLiveNonBloquing(long companyId, long groupId, long userId, String processId, String className, String [] liveItemIds, boolean milenium, long scopeGroupId, String contentsToPublish) throws Throwable
	{
		_log.trace("In formatResultsForFlex.publishToLiveNonBloquing");
		
		String publishLog	= "";
		String errorInf		= "";		
		
		_log.info("Starting Publishing Process");
		long publicationTime = System.currentTimeMillis();	 
		long lockTime = System.currentTimeMillis();	
		
		ArrayList<String> selectedItem   = null;
		boolean productPublication       = false;
		boolean qualificationPublication = false;
		
		if (Validator.isNotNull(className))
		{
			if (className.equals(IterKeys.CLASSNAME_CATEGORY))
			{
				// Unico assetcategory que actualizará su estado (para que el resto de categorías del vocabulario no acaban con estado "Error")
				selectedItem = new ArrayList<String>();
				selectedItem.add(liveItemIds[0]);
			}
			else if ((className.equals(IterKeys.CLASSNAME_MILENIUMSECTION) || className.equals(IterKeys.CLASSNAME_MILENIUMARTICLE)) )
			{	
				// Obtiene todos los ids relacionados con las secciones y artículos
				// Creamos el contexto
				XMLIOContext context = new XMLIOContext();
					
				// Seteamos aquellas partes de la publicación que no queramos que se hagan
				if (className.equals(IterKeys.CLASSNAME_MILENIUMSECTION) && ("0").equals(contentsToPublish) )
				{
					context.setPublishArticles(false);
					context.setPublishCatalogs(false);
					context.setPublishPageContent(false);
				}			
				
				ItemXmlIO itemXmlIO = XMLIOUtil.getItemByType(className);
				
				// Asignamos el contexto
				itemXmlIO.setXMLIOContext(context);
				
				// Obtenemos los ids a publicar
				liveItemIds = itemXmlIO.getRelatedPoolIds(liveItemIds);
			}
			else
			{
				// Si a la función LiveControlLocalServiceUtil.getLock le llega el classname "PRODUCT" o "QUALIFICATION" genera un error, 
				// la cambiamos temporalmente a null y luego le volvemos a poner su valor
				productPublication       = className.equals(IterKeys.CLASSNAME_PRODUCT);
				qualificationPublication = className.equals(IterKeys.CLASSNAME_QUALIFICATION);			
				
				if(productPublication || qualificationPublication)
				{
					className = null;
				}
			}			
		}		
							
		
		// Reserva la tabla de publicacion
		// En el getLock se capturan todas las excepciones y se serializa dicho resultado. Esto se hace para tener 
		// el processID en los casos de que haya fallado después de haberse insertado el registro en Live_Control
		List<String> lockError = new ArrayList<String>();
		processId = LiveControlLocalServiceUtil.getLock(userId, groupId, className, processId, liveItemIds, lockError);
		_log.info("Process Id is " + processId);
		
		if (!lockError.isEmpty() && (processId == null || processId.isEmpty()))
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_PUBLISH_LOCK_FAILED_ZYX, lockError.get(0));
		
		// Se lanza una excepción si ya existe otra publicación y ni siquiera se ha creado un registro en LiveControl
		ErrorRaiser.throwIfFalse( processId != null && !processId.isEmpty(), IterErrorKeys.XYZ_E_PUBLISH_ALREADY_IN_PROCESS_ZYX);
		
		try
		{	
			// Puede que se haya creado el proceso (LiveControl) pero haya ocurrido un error después
			if (!lockError.isEmpty())
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_PUBLISH_LOCK_FAILED_ZYX, lockError.get(0));
			
			lockTime = System.currentTimeMillis() - lockTime;		
			_log.info("Process: " + processId + ", Publish to Live Control locked, Lock time: " +  XMLIOUtil.toHMS(lockTime));
			
			// Restauramos el nombre de la clase una vez obtenida la reserva de la tabla de la publicación
			if (productPublication)
			{
				className = IterKeys.CLASSNAME_PRODUCT;
			}
			else if(qualificationPublication)
			{
				className = IterKeys.CLASSNAME_QUALIFICATION;
			}
		
			// Recupera la configuracion
			LiveConfiguration liveConf 	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(companyId);
			String localPath 			= liveConf.getLocalPath();
			
			if(_log.isDebugEnabled())
				_log.debug("Getting configuration Live info");
			
			// Generar paquete de exportacion correspondiente en local
			long zipGenerationTime = System.currentTimeMillis();
			File localFile = exportLiveContents(companyId, groupId, userId, className, liveItemIds, localPath, processId, milenium, scopeGroupId);
			zipGenerationTime = System.currentTimeMillis() - zipGenerationTime;
		   	_log.info("Process: " + processId + ", Local file created at " + localFile.getAbsolutePath() + " - Generation time " +  XMLIOUtil.toHMS(zipGenerationTime));
		   	
			// Se actualiza en la interfaz el tamaño del zip
			LiveControlLocalServiceUtil.updateLiveControlFileSize(processId, localFile.length());
			
			// Se envía e importa el paquete
			Document remoteLogDom = PublicationProxyBack.publish(processId, className, localFile);
			
			Node groups2Updt = remoteLogDom.getRootElement().selectSingleNode(IterKeys.GROUPS2UPDT_ROOT);
			setGroups2Updt(groups2Updt);
			
    		setRefreshGlobalJournalTemplates(needToRefreshGlobalJournalTemplates(remoteLogDom));
    		setRefreshQualifications( needToRefreshQualifications(remoteLogDom) );
    		setRefreshMetadataAdvertisement( needToRefreshMetadataAdvertisement(remoteLogDom, scopeGroupId) );
    		setRefreshScheduledPublications( needToRefreshScheduledPublications(remoteLogDom, scopeGroupId) );
    		
			if (isRefreshRemoteCacheEnable())
			{
				CacheRefresh cr = new CacheRefresh(scopeGroupId, groups2Updt);
				cr.setRefreshGlobalJournalTemplates( getRefreshGlobalJournalTemplates() );
				cr.setRefreshQualifications( getRefreshQualifications() );
				cr.setRefreshMetadataAdvertisement( getRefreshMetadataAdvertisement() );
				cr.setRefreshScheduledPublications( getRefreshScheduledPublications() );
				cr.setPublicationId(processId);
				
				XMLIOUtil.deleteRemoteCache(cr);
			}
			
			// Elimina las programaciones que se han publicado al Live.
			deleteArticleScheduledPublications(remoteLogDom);
			
			_log.info("Process: " + processId + ", Remote import (explicit) completed");

			_log.info("Process: " + processId + ", Remote log obtained");
			
			// Se añade a la respuesta el localID que necesita MLN
			String remoteLog = addLocalIDToLiveResponse(remoteLogDom.asXML(), companyId);
			
			//4. A partir del log devuelto por el metodo anterior, actualizar la tabla de Live con (pending->done o pending->error)
			_log.info("Process: " + processId + ", Updating Local Live table from remote log...");
			long remoteLogTime = System.currentTimeMillis();
			
			publishLog = updateLiveFromRemoteLog(remoteLog, companyId, processId, selectedItem);
			_log.info("Process: " + processId + ", Remote log update:\n" + publishLog);
			
			remoteLogTime = System.currentTimeMillis() - remoteLogTime;
		   	_log.info("Process: " + processId + ", Remote log processing time " +  XMLIOUtil.toHMS(remoteLogTime));
		   
		}
		catch(Throwable e)
		{
			_log.trace(e);
			
			errorInf = e.toString();
			
			// Si la operación falló los que están como processing se marcan como pendientes ya que ha sido un fallo global
			_log.info("Process: " + processId + ", Setting remaining processing to PENDING: " + errorInf);
			updateStatus(IterKeys.PROCESSING, IterKeys.PENDING, processId);
			updateStatus(IterKeys.CORRUPT,    IterKeys.PENDING, processId);
			
			throw e;
		}
		finally
		{
			// Errores totales
			try
			{
				long totalErrors = countErrorsInProcess(getGroupId(companyId, ""), groupId, processId);
				LiveControlLocalServiceUtil.updateLiveControlErrors(processId, totalErrors, errorInf);
				_log.info("Process: " + processId + ", Failed operations in current process " + totalErrors);
			}
			catch(Exception err){_log.error(err);}
			
			//Desbloquea la tabla
			try
			{
				long unlockTime = System.currentTimeMillis();	    
				
				LiveControlLocalServiceUtil.releaseLock(processId);
				
				unlockTime = System.currentTimeMillis() - unlockTime;		
				_log.info("Process: " + processId + ", Unlock time: " +  XMLIOUtil.toHMS(unlockTime));
			}
			catch(SystemException err)
			{
				// Solo si no ha ocurrido un error principal, se relanza este
				if (errorInf.isEmpty())
					throw err;
				else
					_log.error(err);
			}
				
			publicationTime = System.currentTimeMillis() - publicationTime;
			
			_log.info("Process: " + processId + ", Publish to Live Control unlocked - TOTAL publication time " + XMLIOUtil.toHMS(publicationTime));
		}

		return publishLog;
	}
	
	/**
	 * Método que determina si se ha publicado satisfactoriamente alguna plantilla global
	 * @param remoteLog
	 * @return true si se ha publicado satisfactoriamente alguna plantilla global 
	 * @throws DocumentException 
	 */
	private boolean needToRefreshGlobalJournalTemplates(Document remoteLogDom)
	{
		boolean refreshGlobalJournalTemplates = false;
		
		String xpath = String.format("count(//item[starts-with(upper-case(@globalId), '%s') and @status='done'])", ItemXmlIO.getGlobalByLocalId(GlobalJournalTemplateMgr.GLOBAL_PREFIX));
		
		refreshGlobalJournalTemplates = XMLHelper.getLongValueOf(remoteLogDom, xpath) > 0;
		
		return refreshGlobalJournalTemplates;
	}
	
	private boolean needToRefreshQualifications(Document remoteLogDom)
	{
		boolean refreshQualifications = false;
		
		String xpath = String.format("count(//item[@classname='%s' and @status='done'])", IterKeys.CLASSNAME_QUALIFICATION);
		
		refreshQualifications = XMLHelper.getLongValueOf(remoteLogDom, xpath) > 0;
		
		return refreshQualifications;
	}
	
	private boolean needToRefreshMetadataAdvertisement(Document remoteLogDom, long scopeGroupId)
	{
		boolean refreshMetasAdv = false;
		
		List<Node> nodes = remoteLogDom.selectNodes( String.format("//item[@classname='%s' and @status='done']/@globalId", IterKeys.CLASSNAME_VOCABULARY) );
		String[] publishedVocs = XMLHelper.getStringValues(nodes);
		
		if (publishedVocs.length > 0)
		{
			String sql = String.format(COUNT_ADVOC_MODIFIED, StringUtil.merge(publishedVocs, "','"));
			if (scopeGroupId > 0)
				sql = sql.concat( String.format(" AND ad.groupid=%d", scopeGroupId) );
			
			refreshMetasAdv = Integer.valueOf(PortalLocalServiceUtil.executeQueryAsList(sql).get(0).toString()) > 0;
		}
		
		return refreshMetasAdv;
	}
	
	private boolean needToRefreshScheduledPublications(Document remoteLogDom, long scopeGroupId)
	{
		List<Node> nodes = remoteLogDom.selectNodes("/iter/scheduledPublications/publications");
		
		String xpath = String.format("count(//item[@classname='%s' and @status='done'])", IterKeys.CLASSNAME_PAGECONTENT);
		boolean refreshPagecontent = XMLHelper.getLongValueOf(remoteLogDom, xpath) > 0;
		
		return nodes.size() > 0 || refreshPagecontent;
	}
	
//	private static boolean needToRescheduleCacheInvalidation(Document remoteLogDom)
//	{
//		String xpath = String.format("count(//item[@classname='%s' and @status='done'])", IterKeys.CLASSNAME_PAGECONTENT);
//		return XMLHelper.getLongValueOf(remoteLogDom, xpath) > 0;
//	}
	
	private String getVocabulariesModifiedToNotify(String remoteLog)
	{
		String vocabularies = StringPool.BLANK;
		
		if (Validator.isNotNull(remoteLog))
		{
			try
			{
				Document remoteLogDom = SAXReaderUtil.read(remoteLog);
				
				List<Node> nodes = remoteLogDom.selectNodes( String.format("//item[@classname='%s' and @status='done']/@globalId", IterKeys.CLASSNAME_VOCABULARY) );
				String[] publishedVocs = XMLHelper.getStringValues(nodes);
				
				if (publishedVocs.length > 0)
				{
					String groupName = XMLHelper.getStringValueOf(remoteLogDom.getRootElement(), "@scopegroupid");
					vocabularies = groupName.concat(StringPool.COLON).concat(StringUtil.merge(publishedVocs));
				}
			}
			catch (DocumentException e)
			{
				_log.error(e);
			}
		}
		
		return vocabularies;
	}
	
	/**
	 * Genera un paquete de exportacion con todos los contenidos pendientes/erroneos del grupo
	 * @param companyId
	 * @param groupId
	 * @param userId
	 * @param destinationPath
	 */
	public File exportLiveContents(long companyId, long groupId, long userId, String className, 
								   String [] liveItemIds, String destinationPath, String processId, 
								   boolean milenium, long scopeGroupId) throws Exception
	{
		_log.trace("In formatResultsForFlex.exportLiveContents");
		
		//Prepara el sistema de exportacion
		XMLIOExport xmlioExport   = new XMLIOExport(scopeGroupId);
		
		XMLIOContext xmlIOContext = new XMLIOContext();	
		xmlIOContext.setCompanyId(companyId);
		xmlIOContext.setUserId(userId);
		xmlIOContext.setGroupId(groupId);
		xmlIOContext.setGlobalGroupId(getGroupId(companyId, ""));
		
		if (Validator.isNotNull(className))
		{
			if (className.equals(IterKeys.CLASSNAME_QUALIFICATION))
			{
				// Publicación de qualifications con la publicación del grupo
				xmlIOContext.setOnlyQualificationsPublication(true);
				className = IterKeys.CLASSNAME_GROUP;
			}
			else if (className.equals(IterKeys.CLASSNAME_PRODUCT))
			{
				// Publicación de productos con la publicación del grupo
				xmlIOContext.setOnlyProductsPublication(true);
				className = IterKeys.CLASSNAME_GROUP;
			}
			else if (className.equals(IterKeys.CLASSNAME_CATEGORY))
			{
				// Publicación de una categoría con la publicación de un vocabulario
				_log.debug("Checking if the category has its vocabulary published");
				
				// Buscamos el registro xmlio_live correspondiente a la categoría
				Live liveCategory = LiveUtil.findByPrimaryKey(Long.parseLong(liveItemIds[0]));
				
				String sql = String.format(GET_LIVE_VOCABULARY_ID, liveCategory.getId());
				if (_log.isDebugEnabled())
				{
					_log.debug(new StringBuilder("Query to get the live register of the vocabulary of the category:\n").append(sql));
				}
				Document doc = PortalLocalServiceUtil.executeQueryAsDom(sql);
				// Este nodo debe existir (no se puede crear en MLN una categoría antes que su vocabulario)
				Node vocabularyLive = doc.getRootElement().selectSingleNode("/rs/row");
				ErrorRaiser.throwIfNull(vocabularyLive,
										IterErrorKeys.XYZ_E_VOCABULARY_NOT_FOUND_ZYX, 
										String.format("Can not find the vocabulary which belongs to category %s", liveCategory.getLocalId()));
				
				// Metemos en el contexto el assetCategory.categoryId
				xmlIOContext.setAssetCategoryId(Long.parseLong(liveCategory.getLocalId()));			
				
				// Cambios para que siga la publicación del vocabulario
				liveItemIds[0] = XMLHelper.getTextValueOf(vocabularyLive, "@id");			
				className      = IterKeys.CLASSNAME_VOCABULARY;
			}
		}		
		
		if(_log.isDebugEnabled())
			_log.debug("ProcessId: " + processId +" Prepara el sistema de exportacion.");
		
		Document doc = SAXReaderUtil.createDocument();
		Element root = doc.addElement(IterKeys.XMLIO_XML_ELEMENT_ROOT);

		long groupToPublish = scopeGroupId == 0 ? groupId : scopeGroupId;
		Live groupLive = getLiveByLocalId(groupToPublish, IterKeys.CLASSNAME_GROUP , String.valueOf(groupToPublish));
		root.addAttribute(IterKeys.XMLIO_XML_SCOPEGROUPID_ATTRIBUTE, groupLive.getGlobalId());
		
		boolean isGlobalPublication = Validator.isNull(className);
		
		// Publicación global/grupo/TPU
		if (isGlobalPublication)
		{
			if(_log.isDebugEnabled())
				_log.debug("ProcessId: " + processId +" exportAllElements");		

			exportAllElements(root, xmlioExport, xmlIOContext);
		}
		else
		{
			//Export By Type
			//If no items specified, all items of this type will be exported
			if (liveItemIds == null || liveItemIds.length == 0)
			{

				if(_log.isDebugEnabled())
					_log.debug("ProcessId: " + processId +" exportAllElementsByClassName");		

				exportAllElementsByClassName(root, xmlioExport, xmlIOContext, className);
			}
			else
			{
				//Export an especific set of items
				if(_log.isDebugEnabled())
					_log.debug("ProcessId: " + processId +" exportElementsByClassNameItemId");
				
				exportElementsByClassNameItemId(root, xmlioExport, xmlIOContext, className, liveItemIds, milenium);
			}
		}
		doc.getRootElement().addAttribute("processid", processId); 
		String xptContent = doc.asXML();
		
		// Número de elementos según la estructura definida
		// _log.debug(doc.asXML());
		
		XPath xpath = SAXReaderUtil.createXPath("count(/iter/list/pool/item)");
		long numItemsOK = xpath.numberValueOf(doc).longValue();
		
		if (numItemsOK <= 0)
		{
			if(_log.isDebugEnabled())
				_log.debug("ProcessId: " + processId +" numItemsOK <= 0");
			
			// Se construye el XML de log de errores
			Document logDoc = SAXReaderUtil.createDocument();
			Element iterElem = logDoc.addElement("iter");
			iterElem.addElement("logs");
			addExportErrorItems (logDoc, processId, IterKeys.CORRUPT, null);
			addExportErrorItems (logDoc, processId, IterKeys.PENDING, null);
			xptContent = logDoc.asXML();
			xpath = SAXReaderUtil.createXPath("count(/iter/logs/item)");
			numItemsOK = xpath.numberValueOf(logDoc).longValue();
			ErrorRaiser.throwIfFalse( numItemsOK <= 0, IterErrorKeys.XYZ_E_XPORTCONTENT_ALL_FAILED_ZYX, xptContent);
			
			// Se comprueba que exista al menos un elemento a exportar
			ErrorRaiser.throwIfError( IterErrorKeys.XYZ_E_XPORTCONTENT_EMPTY_ZYX, xptContent);
		}
		
		// Total de elementos exportables
		xpath = SAXReaderUtil.createXPath("count(//item)");
		long numItems = xpath.numberValueOf(doc).longValue();
				
		// Se comprueba que TODOS los elementos exportables sigan la estructura deseada
		ErrorRaiser.throwIfFalse( numItemsOK == numItems, IterErrorKeys.XYZ_E_XPORTCONTENT_MALFORMED_ZYX, xptContent);
		
		xmlioExport.setContent(xptContent);
		
		// _log.debug("ProcessId: " + processId +" Export Contents " + xptContent + " to " + destinationPath);
		return xmlioExport.generateZip(destinationPath);
	}	
	
	/**
	 * Añade los elementos que quedan en un determinado estado al xml que se le pasa a Milenium y se ponen a error.
	 * @param doc
	 * @param processId
	 * @param status
	 */
	private void addExportErrorItems (Document doc, String processId, String status, ArrayList<String> selectedItem){
		_log.trace("In formatResultsForFlex.addExportErrorItems");
		
		try
		{
			//Obtenemos el tipo de publicación
			LiveControl liveControl = LiveControlLocalServiceUtil.getLiveControlInProcess(processId);

			if(_log.isDebugEnabled())
				_log.debug("ProcessId: " + processId +" getLiveControlInProcess(processId)");
			
			String pubType = liveControl.getType();
				
			List<Live> liveElems = new ArrayList<Live>();
			if (pubType.equals(IterKeys.MASIVE))
			{

				if(_log.isDebugEnabled())
					_log.debug("ProcessId: " + processId +" exportacion masiva");			

				//Controlamos que no se pongan a error los elementos que aún no han sido asignados (DLFileEntry sin entrada en la tabla de pools).
				if (status.equals(IterKeys.PENDING))
				{

					if(_log.isDebugEnabled())
						_log.debug("ProcessId: " + processId +" Entra masiva y pending");			

					//Los PENDING que si están asignados se ponen a ERROR
					long groupId = liveControl.getGroupId();
					long companyId = GroupLocalServiceUtil.getGroup(groupId).getCompanyId();
					
					setAssignedPendingElementsToError(doc, groupId, getGroupId(companyId, ""));		

					if(_log.isDebugEnabled())
						_log.debug("ProcessId: " + processId +" Sale masiva y pending");
				}
				else
				{
					if(_log.isDebugEnabled())
						_log.debug("ProcessId: " + processId +" Entra masiva y NO pending");			

					liveElems.addAll(LiveLocalServiceUtil.getLiveByStatus(status));
					//Los elementos de la pub. masiva que quedan a CORRUPT o PROCESSING se ponen a ERROR		
					setElementsToError (doc, status, liveElems);

					if(_log.isDebugEnabled())
						_log.debug("ProcessId: " + processId +" Sale masiva y No pending");
				}
			}
			else
			{

				if(_log.isDebugEnabled())
					_log.debug("ProcessId: " + processId +" Entra NO masiva");			

				List<LivePool> poolList = LivePoolLocalServiceUtil.getLivePoolListByProcessId(processId);
				for (LivePool lp: poolList)
				{
					Live liveElem = LiveLocalServiceUtil.getLive(lp.getLiveChildId());
					
					if (liveElem.getStatus().equals(status))
					{
						if (selectedItem!=null && liveElem.getClassNameValue().equals(IterKeys.CLASSNAME_CATEGORY))
						{ 						
							if (selectedItem.contains(liveElem.getId()))
								liveElems.add(liveElem);
							else if(_log.isDebugEnabled())
									_log.debug(new StringBuilder("Ignoring updating as 'error' assetcategory ").append(liveElem.getLocalId()));
						}
						else if (selectedItem==null || !liveElem.getClassNameValue().equals(IterKeys.CLASSNAME_VOCABULARY))
							liveElems.add(liveElem);
					}
				}

				if(_log.isDebugEnabled())
					_log.debug("ProcessId: " + processId +" Fin bucle livepool");			

				//En las parciales se ponen a ERROR todos los elementos que han quedado a PENDING, PROCESSING o CORRUPT
				setElementsToError (doc, status, liveElems);

				if(_log.isDebugEnabled())
					_log.debug("ProcessId: " + processId +" Sale NO masiva");
			}			
		}
		catch(Exception err)
		{
			_log.error(err);
		}			
	}	
	
	/**
	 * 
	 * @param doc
	 * @param status
	 * @param liveElems
	 */
	private void setElementsToError (Document doc, String status, List<Live> liveElems){
		_log.trace("In formatResultsForFlex.setElementsToError");
		
		Element docLog = doc.getRootElement().element("logs");			
		for (Live live:liveElems)
		{
			try{					
				//Lo añadimos al xml					
				Element newSourceElement = SAXReaderUtil.createElement("item");									
				newSourceElement.addAttribute("operation", live.getOperation());
				newSourceElement.addAttribute("globalId", live.getGlobalId());
				newSourceElement.addAttribute("localId", live.getLocalId());
				newSourceElement.addAttribute("groupId", GroupLocalServiceUtil.getGroup(live.getGroupId()).getName());
				newSourceElement.addAttribute("classname", live.getClassNameValue());
				newSourceElement.addAttribute("status", IterKeys.ERROR);
				newSourceElement.addCDATA(live.getErrorLog());
				docLog.add(newSourceElement);							
				
				//Actualizamos el error
				live.setStatus(IterKeys.ERROR);
				livePersistence.update(live, false);				
			}
			catch(Exception err)
			{
				_log.error(err);
			}
		}
	}
	
	/**
	 * 
	 * @param doc
	 * @param status
	 */
	private void setAssignedPendingElementsToError (Document doc, long groupId, long globalGroupId)
	{
		_log.trace("In formatResultsForFlex.setAssignedPendingElementsToError");
		
		String sql = String.format(SELECT_PENDING_ELEMENTS, groupId, globalGroupId);
				
		List<Object> liveObjList = new XmlioLiveDynamicQuery().executeSelectQuery(sql);
		
		Element docLog = doc.getRootElement().element("logs");				
		for (Object liveObj : liveObjList)
		{
			try{
				long liveId = ((BigInteger)liveObj).longValue();
				Live live = LiveLocalServiceUtil.getLive(liveId);
				
				//Lo añadimos al xml					
				Element newSourceElement = SAXReaderUtil.createElement("item");									
				newSourceElement.addAttribute("operation", live.getOperation());
				newSourceElement.addAttribute("globalId", live.getGlobalId());
				newSourceElement.addAttribute("localId", live.getLocalId());
				newSourceElement.addAttribute("groupId", GroupLocalServiceUtil.getGroup(live.getGroupId()).getName());
				newSourceElement.addAttribute("classname", live.getClassNameValue());
				newSourceElement.addAttribute("status", IterKeys.ERROR);
				newSourceElement.addCDATA(live.getErrorLog());
				docLog.add(newSourceElement);							
				
				//Actualizamos el error
				live.setStatus(IterKeys.ERROR);
				livePersistence.update(live, false);				
			}
			catch(Exception err)
			{
				_log.error(err);
			}
		}
	}
	
	/**
	 * Exporta todos los elementos del grupo global y local
	 * @param globalGroupId
	 * @param groupId
	 * @param root
	 * @param xmlioExport
	 * @param xmlIOContext
	 */
	private void exportAllElements(Element root, XMLIOExport xmlioExport, XMLIOContext xmlIOContext) throws Exception
	{
		_log.trace("In formatResultsForFlex.exportAllElements");
		
		for (String type : IterKeys.MAIN_CLASSNAME_TYPES_EXPORT) 
		{
			ItemXmlIO itemXmlIO = XMLIOUtil.getItemByType(type, xmlIOContext);
			ErrorRaiser.throwIfNull(itemXmlIO);
			
			itemXmlIO.publishContents(xmlioExport, root, xmlIOContext.getGroupId(), IterKeys.XMLIO_XML_PUBLISH_OPERATION);	
		}			
	}
	
	private void exportAllElementsByClassName(Element root, XMLIOExport xmlioExport, XMLIOContext xmlIOContext, String className) throws Exception
	{
		_log.trace("In formatResultsForFlex.exportAllElementsByClassName");
		
		ItemXmlIO itemXmlIO = XMLIOUtil.getItemByType(className, xmlIOContext);
		ErrorRaiser.throwIfNull(itemXmlIO);
		
		itemXmlIO.publishContents(xmlioExport, root, xmlIOContext.getGroupId(),			IterKeys.XMLIO_XML_PUBLISH_OPERATION); 
	}
	
	private void exportElementsByClassNameItemId(Element root, XMLIOExport xmlioExport, XMLIOContext xmlIOContext, String className, String [] liveItemIds, boolean milenium) throws Exception
	{
		_log.trace("In formatResultsForFlex.exportElementsByClassNameItemId");
		
		ItemXmlIO itemXmlIO = XMLIOUtil.getItemByType(className, xmlIOContext);
		ErrorRaiser.throwIfNull(itemXmlIO);
		
		try
		{
			Element list = root.addElement(IterKeys.XMLIO_XML_ELEMENT_LIST);
			for (String liveItemId : liveItemIds)
			{
				if (milenium)
				{
					itemXmlIO.publishMileniumContent(xmlioExport, list, Long.valueOf(liveItemId));
				}
				else
				{
					itemXmlIO.publishContent(xmlioExport, list, Long.valueOf(liveItemId), IterKeys.XMLIO_XML_PUBLISH_OPERATION);
				}
			}
		}
		catch(Exception e)
		{
			_log.error(e);
		}
	}
	
	/**
	 * Fución que añade al XML que se devolvería a MLN el localID para que este pueda reconocer las operaciones implicadas.
	 * Si falla una operación se intenta añadir el localID a la siguiente dado que ésta ya se ha ejecutado en el LIVE y 
	 * esto sería un fallo secundario, de ambulancia
	 * 
	 * @param response
	 * @return response with localID
	 */
	private String addLocalIDToLiveResponse(String response, long companyId) throws DocumentException
	{
		_log.trace("In formatResultsForFlex.addLocalIDToLiveResponse");
		
		Document doc 	 = SAXReaderUtil.read(response);		
	    XPath xpath 	 = SAXReaderUtil.createXPath("//item");
	    List<Node> nodes = xpath.selectNodes(doc);	
	    
	    for (Node node : nodes) 
	    {
	    	// Si falla la actualización de un nodo se intenta actualizar el siguiente
	    	try
	    	{
		    	Element item = (Element) node;	
				String classNameValue 	= item.attributeValue("classname", "");
				String operation 		= item.attributeValue("operation");
				String globalId 		= item.attributeValue("globalId", "");
				String groupName 		= item.attributeValue("groupId");
				
				long groupId 	= GroupLocalServiceUtil.getGroup(companyId, groupName).getGroupId();
				Live row 	 	= getLiveByGlobalId(groupId, classNameValue, globalId);
				String localID 	= row.getLocalId();
				
				if (classNameValue != IterKeys.CLASSNAME_PORTLET && !operation.equals(IterKeys.DELETE))
				{
					ItemXmlIO itemXmlIO = XMLIOUtil.getItemByType(classNameValue);
					
					//Modificaciones requeridas para el Log que obtiene Milenium (solo CREATE y UPDATE)
					item.addAttribute("localId", itemXmlIO.getMileniumId(groupId, localID));
					if (classNameValue.equals(IterKeys.CLASSNAME_JOURNALARTICLE))
					{
						item.addAttribute("structureId", JournalArticleLocalServiceUtil.getArticle(groupId, localID).getStructureId());
					}
					else if(classNameValue.equals(IterKeys.CLASSNAME_DLFILEENTRY))
					{
						item.addAttribute("title", DLFileEntryLocalServiceUtil.getDLFileEntry(Long.valueOf(localID)).getTitle());
					}
				}
				else
				{
					item.addAttribute("localId", localID);
				}
	    	}
	    	catch(Exception err){}
	    }
		
		return doc.asXML();
	}

	/**
	 * Actualiza la tabla de Live a partir del resultado obtenido en la importacion en el sistema remoto
	 * @param remoteLog el log con las referencias a los elementos globales actualizadas en remoto
	 * @return un log con referencias a los elementos locales actualizados en remoto
	 * @throws NoSuchLiveException
	 * @throws PortalException
	 * @throws SystemException
	 * @throws DocumentException
	 */
	private String updateLiveFromRemoteLog(String remoteLog, long companyId, String processId, ArrayList<String> selectedItem)throws NoSuchLiveException, PortalException, SystemException, DocumentException {
		_log.trace("In formatResultsForFlex.updateLiveFromRemoteLog");
		
    	//1. Creamos un XML Doc a partir del log
	    Document doc = SAXReaderUtil.read(remoteLog);		
	    
	    //2. PORTLETS: Recuperamos todas las entradas de PORTLETS cuya operación sea delete y que tengan status ERROR, DONE o WARN (se ignora INFO)	  
	    String xPath = "//item[@classname='" + IterKeys.CLASSNAME_PORTLET + "' and @operation='" + IterKeys.DELETE + "' and (@status='" + IterKeys.ERROR + "' or @status='" + IterKeys.DONE + "')]";
	    XPath xpathSelector = SAXReaderUtil.createXPath(xPath);
	    List<Node> nodes = xpathSelector.selectNodes(doc);		
		//1.1. Por cada Portlet-delete, se comprueba su status
		List<Node> createNodes = null;
		for (Node node : nodes) 
		{
			try
			{
				Element item = (Element) node;	
				String classNameValue 	= item.attributeValue("classname", 	"");
				String globalId 		= item.attributeValue("globalId", 	"");
				String status 			= item.attributeValue("status", 	"");
				String groupName 		= item.attributeValue("groupId");
				String operation 		= item.attributeValue("operation");
				String sGroupId 		= item.attributeValue("groupId");
				
				try
				{
					long groupId = GroupLocalServiceUtil.getGroup(companyId, groupName).getGroupId();				
					
					//3.2. Mientras el status sea distinto de ERROR, se obtienen todos los Portlet-create que tengan su mismo globalId 
					xPath = "//item[@classname='" + IterKeys.CLASSNAME_PORTLET + "' and @operation!='" + IterKeys.DELETE + "' and @globalId='" + globalId + "' and (@status='" + IterKeys.ERROR + "' or @status='" + IterKeys.DONE + "')]";
					xpathSelector = SAXReaderUtil.createXPath(xPath);
					createNodes = xpathSelector.selectNodes(doc);	
					int i = 0;
					while (!status.equals(IterKeys.ERROR) && i < createNodes.size()) 
					{
						item = (Element) createNodes.get(i);				
						status = item.attributeValue("status", "");	
						i++;
					}		
									
					//Actualiza el status, obtiene el localId y lo añade al log original
					updateErrorLog(groupId, classNameValue, globalId, status, item.getText());					
					updateStatus(groupId, classNameValue, globalId, status, IterKeys.PROCESSING, null);
				}
				catch(Exception e)
				{
					if(operation.equals(IterKeys.DELETE) && status.equals(IterKeys.DONE))
					{
						List<Live> liveGroup = LiveLocalServiceUtil.getLiveByGlobalId(IterKeys.CLASSNAME_GROUP, sGroupId);
						for(Live lg : liveGroup)
						{
							//Actualiza el status, obtiene el localId y lo añade al log original
							updateStatus(lg.getGroupId(), classNameValue, globalId, status, IterKeys.PROCESSING, null);
						}
					}					
				}	
			}
			catch(Exception err)
			{
				_log.error(err);
			}
		}
	    
	    // Binarios de artículos: Se recuperan todos los binarios creados con status DONE
		xPath = "//item[@classname='binary' and @operation='" + IterKeys.CREATE + "' and @status='" + IterKeys.DONE + "']";
		xpathSelector = SAXReaderUtil.createXPath(xPath);
		nodes = xpathSelector.selectNodes(doc);
		if (nodes.size() > 0)
		{
			try
			{
				String query = "UPDATE binary_repository SET publishDate = CURRENT_TIMESTAMP WHERE binaryId IN (%s)";
				List<String> binaries = new ArrayList<String>();
				for (Node node : nodes) 
				{
					try
					{
						binaries.add(((Element) node).attributeValue("id_"));
					}
					catch(Exception err)
					{	
						_log.error("Updating status failed for element " + node.getText());
					}
				}
				query = String.format(query, StringUtil.merge(binaries.toArray(), StringPool.COMMA, StringPool.APOSTROPHE));
				PortalLocalServiceUtil.executeUpdateQuery(query);
			}
			catch(Exception err)
			{	
				_log.error("Updating status failed binaries");
			}
		}
		
		// Binarios de artículos: Se recuperan todos los binarios eliminados con status DONE
		xPath = "//item[@classname='binary' and @operation='" + IterKeys.DELETE + "' and @status='" + IterKeys.DONE + "']";
		xpathSelector = SAXReaderUtil.createXPath(xPath);
		nodes = xpathSelector.selectNodes(doc);
		if (nodes.size() > 0)
		{
			try
			{
				String query = "DELETE FROM binariesdeleted WHERE binaryId IN (%s)";
				List<String> binaries = new ArrayList<String>();
				for (Node node : nodes) 
				{
					try
					{
						binaries.add(((Element) node).attributeValue("id_"));
					}
					catch(Exception err)
					{	
						_log.error("Updating status failed for element " + node.getText());
					}
				}
				query = String.format(query, StringUtil.merge(binaries.toArray(), StringPool.COMMA, StringPool.APOSTROPHE));
				PortalLocalServiceUtil.executeUpdateQuery(query);
			}
			catch(Exception err)
			{	
				_log.error("Updating status failed binaries");
			}
		}
		
	    //3. ELEMENTOS DISTINTOS DE PORTLETS: Recuperamos todas las entradas de elementos distintos de PORTLETS que tengan status ERROR, INTERRUPT o DONE (se ignora INFO)
	    xPath = "//item[@classname!='" + IterKeys.CLASSNAME_PORTLET + "' and (@status='" + IterKeys.ERROR + "' or @status='" + IterKeys.INTERRUPT + "' or @status='" + IterKeys.DONE + "')]";
		xpathSelector = SAXReaderUtil.createXPath(xPath);
		nodes = xpathSelector.selectNodes(doc);
		//3.1. Por cada elemento, se actualiza su estado en LIVE
		for (Node node : nodes) 
		{
			try
			{
				Element item = (Element) node;				
				String classNameValue 	= item.attributeValue("classname", 	"");
				String globalId 		= item.attributeValue("globalId", 	"");
				String status 			= item.attributeValue("status", 	"");
				String groupName 		= item.attributeValue("groupId");
				String operation 		= item.attributeValue("operation");
				String sGroupId 		= item.attributeValue("groupId");
				String errorLog 		= item.getText();
				String localId = item.attributeValue("localId");
				String liveId           = item.attributeValue("id_");
				//ItemXmlIO itemXmlIO = XMLIOUtil.getItemByType(classNameValue);
				try
				{
					long groupId = GroupLocalServiceUtil.getGroup(companyId, groupName).getGroupId();
				
					//Actualiza el status, obtiene el localId y lo añade al log original
					if (null != selectedItem && classNameValue.equals(IterKeys.CLASSNAME_CATEGORY)){ 						
						if (selectedItem.contains(liveId)){
							updateErrorLog(groupId, classNameValue, globalId, status, errorLog);
							updateStatus(groupId, classNameValue, globalId, status, IterKeys.PROCESSING, null);
						}else{
							if(_log.isDebugEnabled())
								_log.debug(new StringBuilder("Not calling updateErrorLog and updateStatus for assetcategory ").append(localId));
						}
					}else{					
						updateErrorLog(groupId, classNameValue, globalId, status, errorLog);
						updateStatus(groupId, classNameValue, globalId, status, IterKeys.PROCESSING, null);
					}
				}
				catch(Exception e)
				{
					if(operation.equals(IterKeys.DELETE) && status.equals(IterKeys.DONE))
					{
						List<Live> liveGroup = LiveLocalServiceUtil.getLiveByGlobalId(IterKeys.CLASSNAME_GROUP, sGroupId);
						for(Live lg : liveGroup)
						{
							//Actualiza el status, obtiene el localId y lo añade al log original
							updateStatus(lg.getGroupId(), classNameValue, globalId, status, IterKeys.PROCESSING, null);
						}
					}					
				}	
			}
			catch(Exception err)
			{	
				_log.error("Updating status failed for element " + node.getText());
			}
		} 
		
		// 4.
		// Se actualiza la columna existInLive de aquellos registros ejecutados correctamente.
		xpathSelector 	= SAXReaderUtil.createXPath( "//item[@status='"+ IterKeys.DONE +"' and @classname!='binary' and @id_!='']/@id_" );
		nodes 		  	= xpathSelector.selectNodes(doc);
		StringBuffer sb = new StringBuffer();
		
		for (Node node : nodes) 
		{
			sb.append( node.getStringValue() ).append(",");
		}
		if (sb.length() > 0)
		{
			// Existen elementos ejecutados correctamente, se actualiza su existInLive
			StringBuffer query = new StringBuffer( "UPDATE Xmlio_Live SET existInLive='" ).append(IterKeys.BOOL_YES).append("' WHERE id_ IN (" );
			query.append(sb.substring(0, sb.length()-1)).append(")");
			
			DB db = DBFactoryUtil.getDB();
			try
			{
				db.runSQL( query.toString() );
				livePersistence.clearCache();
			}
			catch (Exception e)
			{
				// Si falla esta actualización NO debería abortarse toda la operación realizada hasta el momento
				_log.error(e);
			}
		}		
		
		//5. Se ponen a ERROR los elementos que aun siguen a processing (no se ha obtenido ninguna respuesta válida del LIVE o ha fallado durante la interpretación de dicha respuesta en el BACK)
		addExportErrorItems (doc, processId, IterKeys.CORRUPT,    null);
		addExportErrorItems (doc, processId, IterKeys.PROCESSING, null);
		addExportErrorItems (doc, processId, IterKeys.PENDING,    selectedItem);
		
		doc.setXMLEncoding("ISO-8859-1");
		
		return doc.asXML();
	}
	
	// POPULATE FUNCTIONS	
	/**
	 * Regenera la tabla de Live
	 * 
	 * @param groupId
	 * @param companyId
	 */
		
	public void populateLive (long groupId, long companyId) throws SystemException, PortalException{
		
		_log.info("Start populating live table...");
		
		long populateTime = System.currentTimeMillis();
		
		for (String className : IterKeys.CLASSNAME_TYPES_POPULATE)
		{
			_log.info("Populating items of class " + className + "...");
			
			try
			{
				ItemXmlIO itemXmlIO = XMLIOUtil.getItemByType(className);
				itemXmlIO.populateLive(groupId, companyId);
			}
			catch(Exception err)
			{
				_log.error("Populate Live Error for class " + className);
			}
		}
		
		populateTime = System.currentTimeMillis() - populateTime; 
		_log.info("Finish populating live table - Time " + XMLIOUtil.toHMS(populateTime));
	}

	public void populateLive (String groupName, long companyId) throws SystemException, PortalException
	{
		populateLive( getGroupId(companyId, groupName), companyId);
	}
	
	/**
	 * @param companyId
	 * @param groupName
	 * @return
	 */
	private long getGroupId(long companyId, String groupName) {
		_log.trace("In formatResultsForFlex.getGroupId");
		
		long id = -1;
		try {
			Company company = CompanyLocalServiceUtil.getCompany(companyId);
			id = company.getGroup().getGroupId();
		} catch (Exception e) {}
		
		if (!groupName.equals("")) {
			try {
				Group group = GroupLocalServiceUtil.getGroup(companyId, groupName);
				id = group.getGroupId();
			} catch (Exception e) {
				
			}
		}
		return id;
	}
	
	private List<Live>getLiveListFromLivePoolList(List<LivePool> livePoolList){
		return getLiveListFromLivePoolList(livePoolList, null);
	}
	
	private List<Live> getLiveListFromLivePoolList(List<LivePool> livePoolList, String [] status){
		_log.trace("In formatResultsForFlex.getLiveListFromLivePoolList");
		
		List<Live> liveList = new ArrayList<Live>();
		if (livePoolList != null && livePoolList.size()>0){
			
			if (status!=null){
				List<String> statusList = Arrays.asList(status);
				for (LivePool lp : livePoolList){
					try{
						Live live = livePersistence.fetchByPrimaryKey(lp.getLiveChildId());
						if(live!=null)
						{
							if (statusList.contains(live.getStatus())){
								liveList.add(live);
							}
						}
						else
							_log.error("UNEXPECTED: " + lp.getId());
					}
					catch(Exception err){}
				}
			}
			else{
				for (LivePool lp : livePoolList){
					try{
						Live live = livePersistence.fetchByPrimaryKey(lp.getLiveChildId());
						if(live!=null)
							liveList.add(live);
						else
							_log.error("UNEXPECTED: " + lp.getId());
					}
					catch(Exception err){}
				}				
			}
		}
		return liveList;
	}
	
	public long countErrorsInProcess(long globalGroupId, long groupId, String processId) throws SystemException, PortalException{
		_log.trace("In formatResultsForFlex.countErrorsInProcess");
		
		long numError = 0;
			
		String pubType = LiveControlLocalServiceUtil.getLiveControlInProcess(processId).getType();
		
		if (pubType.equals(IterKeys.MASIVE))				
			numError = LiveLocalServiceUtil.countByStatus(globalGroupId, groupId, IterKeys.ERROR);	
		else
			numError = LivePoolLocalServiceUtil.countByProcessIdStatus(processId, IterKeys.ERROR);				
		
		return numError;
	}
	
	// Gestion de Live desde IterAdmin		
	// Consulta para listado de Publicaciones
	private final String GET_PUBLICATION_LIST = new StringBuffer()
		.append("SELECT id_ id, localId li, groupId gi, classNameValue cnv, operation o, status s, modifiedDate md \n")
		.append("FROM xmlio_live xl                                                                       			\n")
		.append("WHERE xl.groupId IN (%s)                                                                 			\n")
		.append("AND xl.status IN ('error', 'pending')                                                    			\n")
		.append("AND xl.classNameValue IN (%s)                                                          			\n")
		// Filtros
		.append("%s")																								
		.append("ORDER BY %s    																       				\n")
		.append("LIMIT %s, %s").toString();
	
	// Consulta para los detalles de una sola publicacion
	private final String GET_PUBLICATION_DETAILS = new StringBuffer()
		.append("SELECT l.id_ id, l.localId li, l.groupId gi, l.classNameValue cnv, l.operation o, l.status s, l.modifiedDate md, l.errorLog el \n")
		.append("FROM xmlio_live l                                                                      		   \n")
		.append("INNER JOIN xmlio_livepool lp ON (l.id_ = lp.liveChildId)                               		   \n")
		.append("WHERE lp.livePoolId = %s                                                                		   \n")
			// Filtros
		.append("%s")			
		.append("ORDER BY %s    																      			   \n")
		.append("LIMIT %s, %s").toString();
	
	// Listado de Publicaciones 
	public String getPublicationListFlex(String globalGroupId, String groupId, String xmlFilters, String startIn, String limit, String sort) 
					throws ServiceError, SecurityException, NoSuchMethodException, DocumentException
	{
		_log.trace("In formatResultsForFlex.getPublicationListFlex");
		
		ErrorRaiser.throwIfFalse(Validator.isNotNull(globalGroupId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "globalGroupId ids is null");			
		ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "groupId ids is null");			
		
		final long t0 = Calendar.getInstance().getTimeInMillis();
		
		String groups = globalGroupId + "," + groupId;
			
		final String sql = String.format(GET_PUBLICATION_LIST, 
				                         groups, 
				                         classNameParser(IterKeys.MAIN_CLASSNAME_TYPES_EXPORT),
				                         SQLQueries.buildFilters(xmlFilters, false),
								         getSqlSort(sort),                             
							         	 (Validator.isNull(startIn) ? "0"                            : StringEscapeUtils.escapeSql(startIn)),				                             
			                             (Validator.isNull(limit)   ? XmlioKeys.DEFAULT_NUMBER_LIMIT : StringEscapeUtils.escapeSql(limit))
			                            );
		_log.debug("Query to get Publication List: \n" + sql);			
		final Document result = PortalLocalServiceUtil.executeQueryAsDom(sql);			
		
		_log.debug("Time to Publication list: " + (Calendar.getInstance().getTimeInMillis() - t0) + " ms\n\n");
		
		return result.asXML();
	}
	
	// Listado de detalles de una publicacion
	public String getPublicationDetailsFlex(String globalGroupId, String groupId, String liveId, String xmlFilters, String startIn, String limit, String sort) 
					throws ServiceError, SecurityException, NoSuchMethodException, DocumentException
	{
		_log.trace("In formatResultsForFlex.getPublicationDetailsFlex");
		
		ErrorRaiser.throwIfFalse(Validator.isNotNull(globalGroupId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "globalGroupId ids is null");			
		ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "groupId ids is null");			
		
		final long t0 = Calendar.getInstance().getTimeInMillis();
		
		//final String groups = globalGroupId + "," + groupId;
					
		final String sql = String.format(GET_PUBLICATION_DETAILS,
										 liveId,
				                         SQLQueries.buildFilters(xmlFilters, false),
								         getSqlSort(sort),                             
							         	 (Validator.isNull(startIn) ? "0"                            : StringEscapeUtils.escapeSql(startIn)),				                             
			                             (Validator.isNull(limit)   ? XmlioKeys.DEFAULT_NUMBER_LIMIT : StringEscapeUtils.escapeSql(limit))
			                            );
		_log.debug("Query to get Publication List: \n" + sql);			
		final Document result = PortalLocalServiceUtil.executeQueryAsDom(sql);			
		
		_log.debug("Time to Publication list: " + (Calendar.getInstance().getTimeInMillis() - t0) + " ms\n\n");
		
		return result.asXML();
	}
	
	public String classNameParser(String[] names){
		_log.trace("In formatResultsForFlex.classNameParser");
		
		StringBuffer parsedString = new StringBuffer();
		for (int a=0;a<names.length;a++){
			parsedString.append("'");
			parsedString.append(names[a]);
			parsedString.append("'");
			if (a< names.length-1)
				parsedString.append(", ");
		}
		return parsedString.toString();
	}
	
	public String getKeyFieldsFlex(String groupId, String globalGroupId, String publicationIds) throws DocumentException, PortalException, SystemException{
		_log.trace("In formatResultsForFlex.getKeyFieldsFlex");
		
		Document doc = SAXReaderUtil.read(publicationIds);
		final List<Node> nodes = doc.selectNodes("/rs/row");
		String id = null;
		String classNameValue = null;
		String keyField = null;
		
		for (int n = 0; n < nodes.size(); n++){
			id = XMLHelper.getTextValueOf(nodes.get(n), "@id");
			Live row = LiveLocalServiceUtil.getLive(Long.valueOf(id));
			classNameValue = row.getClassNameValue();
			keyField = getKeyField(groupId,globalGroupId,row.getLocalId(),classNameValue);
			Node node = nodes.get(n);
			Element elem = (Element)node;
			elem.addAttribute("keyField", keyField);
		}
		
		return doc.asXML();
	}
	
	public String getKeyField(String groupId, String globalGroupId, String id, String classNameValue)
	{
		_log.trace("In formatResultsForFlex.getKeyField");
		
		String keyField = "";
		try 
		{
			if (classNameValue.equalsIgnoreCase(IterKeys.CLASSNAME_GROUP))
			{
				keyField = GroupLocalServiceUtil.getGroup(Long.valueOf(id)).getName();
			}
			else if (classNameValue.equalsIgnoreCase(IterKeys.CLASSNAME_LAYOUT))
			{
				keyField = LayoutLocalServiceUtil.getLayout(Long.valueOf(id)).getFriendlyURL();
			}
			else if (classNameValue.equalsIgnoreCase(IterKeys.CLASSNAME_PAGETEMPLATE))
			{
				keyField = LayoutLocalServiceUtil.getLayout(PageTemplateLocalServiceUtil.getPageTemplateByPageTemplateId(Long.valueOf(groupId), id).getLayoutId()).getFriendlyURL();
			}
			else if (classNameValue.equalsIgnoreCase(IterKeys.CLASSNAME_JOURNALARTICLE))
			{
				keyField = JournalArticleLocalServiceUtil.getArticle(Long.valueOf(globalGroupId),id).getUrlTitle();
			}
			else if (classNameValue.equalsIgnoreCase(IterKeys.CLASSNAME_VOCABULARY))
			{
				keyField = AssetVocabularyLocalServiceUtil.getAssetVocabulary(Long.valueOf(id)).getName();
			}
			else if (classNameValue.equalsIgnoreCase(IterKeys.CLASSNAME_USER))
			{
				keyField = UserLocalServiceUtil.getUserById(Long.valueOf(id)).getScreenName();
			}
			else if (classNameValue.equalsIgnoreCase(IterKeys.CLASSNAME_SERVICE))
			{
				keyField = ServiceLocalServiceUtil.getServiceByServiceId(Long.valueOf(groupId), id).getTitle();
			}
			else if (classNameValue.equalsIgnoreCase(IterKeys.CLASSNAME_PRODUCT))
			{
				keyField = ProductLocalServiceUtil.getProductNameById(id);
			}
		} 
		catch (Exception e) 
		{
			return keyField;
		}
			
		return keyField;
	}
	
	// Obtiene el campo y ordenacion por la que ordenar
	private String getSqlSort(String flexSort){
		_log.trace("In formatResultsForFlex.getSqlSort");			
		
		StringBuffer sqlSort = new StringBuffer("");

		if (Validator.isNotNull(flexSort))
		{
			/* Llega una cadena como esta: columnid=totalOk asc=0 */
			
			// Obtenemos la columna 
			final String column = flexSort.split(" ")[0].split("=")[1];
			// Obtenemos el sentido
			final String auxOrder = flexSort.split(" ")[1].split("=")[1];				
			final String order = auxOrder.equals("1") ? "asc" : "desc";				
			
			sqlSort.append(column + " " + order);
		}
		
		// Utilizamos la ordenacion por defecto
		else
		{
			sqlSort.append(FIELD_TO_ORDER + " " + FIELD_ORDER);
		}
		
		return sqlSort.toString();
	}
	
	public String formatResultsForFlex(String id, String xmlData, String log) throws DocumentException{
		_log.trace("In LiveLocalServiceImpl.formatResultsForFlex");
		
		Document docLog = SAXReaderUtil.read(log);
    	Document docData = SAXReaderUtil.read(xmlData);
	    String xPathLog = "//item[@status='" + IterKeys.ERROR + "' or @status='" + IterKeys.INTERRUPT + "']";
	    String xPathData = "//row[@id='" + id + "']";
	    XPath xpathSelectorLog = SAXReaderUtil.createXPath(xPathLog);
	    XPath xpathSelectorData = SAXReaderUtil.createXPath(xPathData);
	    List<Node> nodesLog = xpathSelectorLog.selectNodes(docLog);
	    List<Node> nodesData = xpathSelectorData.selectNodes(docData);
    	Element elem = (Element)nodesData.get(0);
	    if (nodesLog.size() > 0)
			elem.addAttribute("status", "error");
		else
			elem.addAttribute("status", "done");
		
	    return docData.asXML();
	}
	// Función para la publicación desde Iter
	public String publishToLiveSelectiveFlex(String xmlData) throws ServiceError, DocumentException
	{
		_log.trace("In LiveLocalServiceImpl.publishToLiveSelectiveFlex");
		
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		long companyId = XMLHelper.getLongValueOf(dataRoot, "@companyId");
		ErrorRaiser.throwIfFalse(companyId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		long userId = XMLHelper.getLongValueOf(dataRoot, "@userId");
		ErrorRaiser.throwIfFalse(userId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		long scopeGroupId = XMLHelper.getLongValueOf(dataRoot, "@scopeGroupId");
		ErrorRaiser.throwIfFalse(scopeGroupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document doc = SAXReaderUtil.read(xmlData);
		final List<Node> nodes = doc.selectNodes("/rs/row");
		
		String [] ids = new String [nodes.size()];
		String log;
		String itemId = null;
		String itemGroupId = null;
		String itemClassNameValue = null;
		
		for (int n = 0; n < nodes.size(); n++)
		{
			itemId = XMLHelper.getTextValueOf(nodes.get(n), "@id");
			itemGroupId = XMLHelper.getTextValueOf(nodes.get(n), "@groupId");
			itemClassNameValue = XMLHelper.getTextValueOf(nodes.get(n), "@classNameValue");
			ids[n] = itemId;
			try 
			{				
				// Publicamos antes los layouts de los design page template no publicados
				if(IterKeys.CLASSNAME_PAGETEMPLATE.equalsIgnoreCase(itemClassNameValue))
				{					
					publishDesignPageTemplateLayoutNotPublished(companyId, scopeGroupId, userId, null, itemId);
				}
				
				log = publishToLive(Long.valueOf(companyId), Long.valueOf(itemGroupId), Long.valueOf(userId), null, itemClassNameValue, ids, false, Long.valueOf(scopeGroupId));
				xmlData = formatResultsForFlex(itemId, xmlData, log);
			} 
			catch (NumberFormatException e) 
			{
				e.printStackTrace();
			} 
			catch (Throwable e) 
			{
				e.printStackTrace();
			}
		}
		return SAXReaderUtil.read(xmlData).asXML();
	}
				
	public String getInClauseSQL(List<Node> ids, String columnId)
	{
		StringBuffer query = new StringBuffer();
		
		if(ids != null && ids.size() > 0)
		{
			for(int i = 0; i < ids.size(); i++)
			{
				String currentId = XMLHelper.getTextValueOf(ids.get(i), "@" + columnId);
	
				if(i == 0)
				{
					query.append("('" + currentId + "'");
				}				
				if(i == ids.size() - 1)
				{
					if(ids.size() > 1)
					{
						query.append(", '" + currentId + "') ");
					}
					else
					{
						query.append(") ");
					}
				}
				if (i > 0 && i < ids.size() - 1)
				{
					query.append(", '" + currentId + "'");
				}
			}
		}
		else
		{
			query.append("('')");
		}
		return query.toString();
	}
	
	// Nueva función genérica de publicación a la que llama MLN
	public String publishToLive(String companyId, String groupId, String userId, String processId, String xml) throws Throwable
	{
		_log.trace("In LiveLocalServiceImpl.publishToLive (new centralized method)");
		
		ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId), com.liferay.portal.kernel.error.IterErrorKeys.XYZ_E_IMPORT_INVALID_GRPID_ZYX, "groupId is null or empty");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(xml),     IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "xml is null or empty");
		
		if (_log.isDebugEnabled())
		{
			_log.debug(new StringBuilder("Receibed data to publication:\n")
								 .append("CompanyId: '").append(companyId).append("'\n")
				                 .append("GroupId: '")  .append(groupId)  .append("'\n")
				                 .append("ProcessId: '").append(processId).append("'\n")
				                 .append("Xml: '")      .append(xml)      .append("'\n").toString());
		}	
		
		userId = String.valueOf( GroupMgr.getDefaultUserId() );

		Document document = SAXReaderUtil.read(xml);
		
		// Obtenemos las importaciones a realizar		
		List<Node> importsToDo = document.getRootElement().selectNodes("/root/method");		
		ErrorRaiser.throwIfFalse(null != importsToDo && importsToDo.size() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "No importations to do");		
			
		// Solo puede venir una operación de importación
		final Node importToDo = importsToDo.get(0);
 
		// Tipo de importación a realizar
		final String importationType = XMLHelper.getTextValueOf(importToDo, "@name");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(importationType), com.liferay.portal.kernel.error.IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid importation type (method/@name=)");
		
		String result = "";
		
		if (importationType.equalsIgnoreCase(LAYOUT_TEMPLATES))
		{
			_log.debug("Layout templates publication");
			LayoutTemplatePublisher.publishTemplates( Long.parseLong(groupId), XMLHelper.getStringValueOf(importToDo, "param[@name='templateids']") );
		}
		else if (importationType.equalsIgnoreCase(JOURNAL_TEMPLATES))
		{
			_log.debug("Journal templates publication");
			JournalTemplatePublisher.publishTemplates( Long.parseLong(groupId), XMLHelper.getStringValueOf(importToDo, "param[@name='templateids']") );
		}
		else if (importationType.equalsIgnoreCase(ARTICLES))
		{
			// Artículos
			_log.debug("Articles publication");
			final String[] articlesIds = XMLHelper.getStringValues(importToDo.selectNodes("param"), ".");
			ErrorRaiser.throwIfNull(Validator.isNotNull(articlesIds), com.liferay.portal.kernel.error.IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "No articlesids found");
			
			// Llamamos al serviceImpl porque tiene lógica necesaria.
			result = LiveServiceUtil.publishContentToLive(Long.parseLong(companyId), Long.parseLong(groupId), Long.parseLong(userId), processId, articlesIds);
		}
		else if (importationType.equalsIgnoreCase(DAILYTOPICS))
		{
			// DaylyTopics
			_log.debug("Daily topics publication");
			DaylyTopicsPublicationLocalServiceUtil.publish(groupId, importToDo.selectNodes("param"), getParametersToCallByJson(Long.parseLong(companyId)));			
		}
		else if(importationType.equalsIgnoreCase(WATERMARK))
		{
			// Marca de agua
			_log.debug("Watermark publication");
			FramesLocalServiceUtil.publishToLive(0, Long.parseLong(groupId), FRAMES_PUBLISH_WATERMARK);
		}
		else if(importationType.equalsIgnoreCase(FRAMES))
		{
			// Encuadres
			_log.debug("Frames publication");
			FramesLocalServiceUtil.publishToLive(0, Long.parseLong(groupId), FRAMES_PUBLISH_FRAMES);
		}
		else if(importationType.equalsIgnoreCase(REPLACE_CONTENTS_TYPES))
		{	
			// Tipos de sustitución	
			_log.debug("Replace contents publication");
			long delegationId = XMLHelper.getLongValueOf(importToDo, "param[@name='delegationid']");
			FramesLocalServiceUtil.publishToLive(delegationId, 0, FRAMES_PUBLISH_REPLACE_CONTENTS_TYPES);
		}
		else if(importationType.equalsIgnoreCase(GROUP))
		{
			result = publishGroup(Long.parseLong(companyId), Long.parseLong(groupId), Long.parseLong(userId), processId,
								  XMLHelper.getStringValueOf(importToDo, "param[@name='templateids']"));
		}
		else if(importationType.equalsIgnoreCase(CONTEXTVARS))
		{
			// Variables de contexto
			_log.debug("Context vars publication");
			
			// "0" indica la publicación del TPU
			Long plid = Long.parseLong(importToDo.selectSingleNode("param").getText());
			ErrorRaiser.throwIfFalse(null != plid && !"".equals(plid), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Section plid is invalid");
			
			String recursive = XMLHelper.getTextValueOf(importToDo, "@recursive", "0");
			
			ContextVarsPublishLocalServiceUtil.publishCtxVars(Long.parseLong(groupId), IterKeys.MILENIUM, plid, recursive, true);
		}
		else if(importationType.equalsIgnoreCase(SECTION))
		{
			result = publishSection(Long.parseLong(companyId), Long.parseLong(groupId), Long.parseLong(userId), processId, importToDo);
		}
		else if(importationType.equalsIgnoreCase(WEB_MODEL))
		{
			result = publishWebModel(Long.parseLong(companyId), Long.parseLong(groupId), Long.parseLong(userId), processId, importToDo);
		}
		else if(importationType.equalsIgnoreCase(CATEGORY))
		{
			result = publishCategory(Long.parseLong(companyId), Long.parseLong(groupId), Long.parseLong(userId), processId, importToDo);
		}
		else if(importationType.equalsIgnoreCase(VOCABULARY))
		{
			result = publishVocabulary(Long.parseLong(companyId), Long.parseLong(groupId), Long.parseLong(userId), processId, importToDo);
		}
		else if(importationType.equalsIgnoreCase(SUBSCRIPTIONS))
		{
			// Suscripción de productos
			result = publishSubscriptions(Long.parseLong(companyId), Long.parseLong(groupId), Long.parseLong(userId), processId);
		}
		else if(importationType.equalsIgnoreCase(QUALIFICATIONS))
		{
			result = publishQualifications(Long.parseLong(companyId), Long.parseLong(groupId), Long.parseLong(userId), processId);
		}
		else if(importationType.equalsIgnoreCase(WEBTHEME))
		{
			publishWebThemes(Long.valueOf(groupId), XMLHelper.getStringValueOf(importToDo, "param[@name='templateids']"), XMLHelper.getStringValueOf(importToDo, "param[@name='themeid']"), true);
		}
		else
		{
			ErrorRaiser.throwIfError(com.liferay.portal.kernel.error.IterErrorKeys.XYZ_E_INVALIDARG_ZYX, new StringBuilder("Invalid publish type: ").append(importationType).append("'").toString());
		}		
		
		if (_log.isDebugEnabled())
			_log.debug(Validator.isNull(result) ? "null" : (new StringBuilder("Result returned: \n").append(result)) );
		
		return result;
	}
	
	private String publishQualifications(long companyId, long groupId, long userId, String processId) throws Throwable
	{
		_log.debug("Qualifications publication");
		
		Live grpLive = getLiveByLocalId(groupId, IterKeys.CLASSNAME_GROUP, String.valueOf(groupId));
		ErrorRaiser.throwIfNull(grpLive);

		// No se puede publicar si el grupo está implicado en otra publicación
		ErrorRaiser.throwIfFalse(!grpLive.getStatus().equals(IterKeys.PROCESSING), IterErrorKeys.XYZ_E_PUBLISH_LOCK_FAILED_ZYX);
		
		List<Live> qualifList = getAllPendingAndErrorLive(companyId, groupId, new String[]{IterKeys.CLASSNAME_QUALIFICATION});
		
		// No ha calificaciones pendientes de publicar
		ErrorRaiser.throwIfFalse(!qualifList.isEmpty(), IterErrorKeys.XYZ_E_XPORTCONTENT_EMPTY_ZYX);
		
		grpLive.setStatus(IterKeys.PENDING);
		updateLive(grpLive, false);
		
		String result = publishToLive(companyId, groupId, userId, processId, IterKeys.CLASSNAME_QUALIFICATION, null, true, groupId);			
		return result;
	}
	
	private String publishSubscriptions(long companyId, long groupId, long userId, String processId) throws Throwable
	{
		_log.debug("Suscriptions publication");
		
		Live grpLive = getLiveByLocalId(groupId, IterKeys.CLASSNAME_GROUP, String.valueOf(groupId));
		ErrorRaiser.throwIfNull(grpLive);

		// No se puede publicar si el grupo está implicado en otra publicación
		ErrorRaiser.throwIfFalse(!grpLive.getStatus().equals(IterKeys.PROCESSING), IterErrorKeys.XYZ_E_PUBLISH_LOCK_FAILED_ZYX);
		
		List<Live> productList = getAllPendingAndErrorLive(companyId, groupId, new String[]{IterKeys.CLASSNAME_PRODUCT});
		
		// No ha productos pendientes de publicar
		ErrorRaiser.throwIfFalse(!productList.isEmpty(), IterErrorKeys.XYZ_E_XPORTCONTENT_EMPTY_ZYX);
		
		grpLive.setStatus(IterKeys.PENDING);
		updateLive(grpLive, false);
		
		String result = publishToLive(companyId, groupId, userId, processId, IterKeys.CLASSNAME_PRODUCT, null, true, groupId);
		return result;
	}
	
	/**
	 * 
	 * @param companyId
	 * @param groupId
	 * @param userId
	 * @param processId
	 * @return
	 * @throws Throwable
	 */
	private String publishGroup(long companyId, long groupId, long userId, String processId, String templateIds) throws Throwable
	{
		String result 						= StringPool.BLANK;
		
		boolean refreshCategoryArticles 	= false;
		boolean refreshLayoutTemplates  	= true;
		boolean refreshContextVariables 	= false;
		boolean refreshFramesAndWatermark 	= false;
		boolean refreshCacheGroup			= false;
		
		deleteOrphanLayouts(groupId);
		
		try
		{

			enableRefreshRemoteCache(false);
			try
			{
				// Grupo (TPU)	
				_log.debug("Group publication");
				result = publishToLive(companyId, groupId, userId, processId, null, null, true, groupId);
			
				Document respondeDom   = SAXReaderUtil.read(result);
				refreshLayoutTemplates = Validator.isNotNull(XMLHelper.getTextValueOf(respondeDom, "/iter/@tpls"));
				refreshCacheGroup 	   = true;
			}
			catch (ServiceError se)
			{
				if (!se.getErrorCode().equals(IterErrorKeys.XYZ_E_XPORTCONTENT_EMPTY_ZYX))
					throw se;
			}
		
			// Publicación de las categoryproperties (que no realiza en sí la publicación del grupo)
			refreshCategoryArticles = CategoriesPropertiesPublicationLocalServiceUtil.publishToLiveCategoriesProperties(groupId, null, null);
			
			try
			{
				// Publicación de: Marcas de agua, Frames, Sinónimos y Contenidos de sustitución 
				FramesLocalServiceUtil.publishToLive(GroupLocalServiceUtil.getGroup(groupId).getDelegationId(), groupId, FRAMES_PUBLISH_ALL);
				refreshFramesAndWatermark = true;
			}
			catch (ServiceError se)
			{
				if (!se.getErrorCode().equals(IterErrorKeys.XYZ_E_XPORTCONTENT_EMPTY_ZYX))
					throw se;
			}
			
			try
			{
				refreshContextVariables = ContextVarsPublishLocalServiceUtil.publishCtxVars(groupId, IterKeys.MILENIUM, 0L, "1", false);
			}
			catch (ServiceError se)
			{
				if (!se.getErrorCode().equals(IterErrorKeys.XYZ_E_XPORTCONTENT_EMPTY_ZYX))
					throw se;
			}
			
			try
			{
				Document currentDefaultCatalogPages = getDefaultSectionPropertiesAndPublish(groupId, StringPool.BLANK, StringPool.BLANK);
				if(currentDefaultCatalogPages!=null && currentDefaultCatalogPages.selectNodes("/rs/row").size()>0 )
					refreshCacheGroup = true;
			}
			catch (ServiceError se)
			{
				if (!se.getErrorCode().equals(IterErrorKeys.XYZ_E_XPORTCONTENT_EMPTY_ZYX))
					throw se;
			}
		}
		finally
		{
			try
			{
				// Si se ha realizado una publicación se refresca la caché
				if (refreshCacheGroup || refreshCategoryArticles || refreshFramesAndWatermark || refreshContextVariables)
				{
					// Es necesario detectar cuántos TPUs han sido afectados. Se tendrán que actualizar
					CacheRefresh cr = new CacheRefresh(groupId, getGroups2Updt());
					cr.setRefreshCategoryArticles(refreshCategoryArticles);
					cr.setRefreshSectionArticles(true);
					cr.setRefreshGlobalJournalTemplates( getRefreshGlobalJournalTemplates() );
					cr.setRefreshQualifications( getRefreshQualifications() );
					cr.setRefreshMetadataAdvertisement( getRefreshMetadataAdvertisement() );
					cr.setRefreshLayoutTemplates( refreshLayoutTemplates );
					cr.setRefreshContextVariables( refreshContextVariables );
					cr.setSendMasNotification(getVocabulariesModifiedToNotify(result));
					
					cr.setRefreshImageFrames(		refreshFramesAndWatermark);
					cr.setRefreshFrameSynonyms(		refreshFramesAndWatermark);
					cr.seRefreshAlternativeTypes(	refreshFramesAndWatermark);
					cr.setRefreshWatermarks(		refreshFramesAndWatermark);
					
					XMLIOUtil.deleteRemoteCache(cr);
				}
			}
			catch (Throwable th)
			{
				_log.error(th);
			}
			enableRefreshRemoteCache(true);
		}
		return result;
	}
	
	/**
	 * 
	 * @param companyId
	 * @param groupId
	 * @param userId
	 * @param processId
	 * @param importToDo
	 * @return
	 * @throws Throwable
	 */
	private String publishCategory(long companyId, long scopeGroupId, long userId, String processId, Node importToDo) throws Throwable
	{
		String result 	   = StringPool.BLANK;
		long globalGroupId = GroupMgr.getGlobalGroupId();

		try
		{
			enableRefreshRemoteCache(false);
			
			// Categorías
			_log.debug("Category publication");
			
			// Nos llega el campo assetcategory.categoryId
			String[] catId = XMLHelper.getStringValues(importToDo.selectNodes("param"), ".");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(catId), com.liferay.portal.kernel.error.IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid category identifier");
			String[] liveItemIds = null;
			// Obtenemos la categoría
			try
			{
				AssetCategory category = AssetCategoryUtil.findByPrimaryKey(Long.parseLong(catId[0]));
				
				// Publicamos los artículos instrumentales de la categoría
				liveItemIds = instrumentalArticleCategoryPublication(companyId, userId, globalGroupId, scopeGroupId, null, category);
			}
			catch(NoSuchCategoryException nsce)
			{
				_log.debug("Category " + catId[0] + " does not exists.");
			}
			
			// Buscamos el registro correspondiente en xmlio_live de la categoría
			Live categoryLive = LiveLocalServiceUtil.getLiveByLocalId(GroupMgr.getGlobalGroupId(), IterKeys.CLASSNAME_CATEGORY, catId[0]);
			ErrorRaiser.throwIfFalse(Validator.isNotNull(categoryLive), IterErrorKeys.XYZ_E_NO_LIVE_ENTRY_FOR_CONTENT_ZYX, 
				                     "The category xmlio_live was not found");
			_log.debug(new StringBuilder("xmlio_live.id_ found: ").append(categoryLive.getId()) );
			
			
			// Hacemos una publicación del vocabulario de la categoría. Jugando con el contexto publicaría unicamente la categoría que queremos.
			String[] liveId = (new String[]{Long.toString(categoryLive.getId())});
			result = publishToLive(companyId, globalGroupId, userId, processId, IterKeys.CLASSNAME_CATEGORY, liveId, true, scopeGroupId);
			
			CategoriesPropertiesPublicationLocalServiceUtil.publishToLiveCategoriesProperties(scopeGroupId, null, catId[0]);
			
			// 30/10/2015: No se refresca la cahé porque habría que refrescar la caché de TODOS los TPUs que forman
			// 19/02/2016: Si hay cambios en artículos instrumentales, se refrescan, indicando que no refresque la caché de los Tomcats ni los Apaches.
			if (Validator.isNotNull(liveItemIds))
			{
				CacheRefresh cr = new CacheRefresh(scopeGroupId, getGroups2Updt());
				cr.setRefreshCategoryArticles(true);
				cr.setRefreshTomcatAndApache(false);
				cr.setRescheduleCacheInvalidation(false);
				XMLIOUtil.deleteRemoteCache(cr);
			}
		}
		finally
		{
			enableRefreshRemoteCache(true);
		}
		return result;
	}
	
	/**
	 * 
	 * @param companyId
	 * @param groupId
	 * @param userId
	 * @param processId
	 * @param importToDo
	 * @return
	 * @throws Throwable 
	 * @throws NumberFormatException 
	 */
	private String publishVocabulary(long companyId, long groupId, long userId, String processId, Node importToDo) throws NumberFormatException, Throwable
	{
		String result 					= StringPool.BLANK;

		long scopeGroupId  = 0;
		long globalGroupId = GroupMgr.getGlobalGroupId();
		
		try
		{
			enableRefreshRemoteCache(false);

			// Vocabularios
			_log.debug("Vocabulary publication");
			
			// Nos llega el campo assetvocabulary.vocabularyId
			String[] vocId = XMLHelper.getStringValues(importToDo.selectNodes("param"), ".");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(vocId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid vocabulary identifier.");
			
			// Obtenemos el registro LIVE
			Live vocabularyLive = LiveLocalServiceUtil.getLiveByLocalId(GroupMgr.getGlobalGroupId(), IterKeys.CLASSNAME_VOCABULARY, vocId[0]);
			ErrorRaiser.throwIfFalse(Validator.isNotNull(vocabularyLive), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Vocabulary "+vocId[0]+" not found");			
			
			// Publicamos los artículos instrumentales del vocabulario
			String[] liveItemIds = instrumentalArticleCategoryPublication(companyId, userId, globalGroupId, scopeGroupId, Long.parseLong(vocabularyLive.getLocalId()), null);
			
			// Publicación del vocabulario
			result = publishToLive(companyId, globalGroupId, userId, processId, IterKeys.CLASSNAME_VOCABULARY, 
				                   new String[]{Long.toString(vocabularyLive.getId())}, true, scopeGroupId);
			
			// Publicamos las categoryProperties
			CategoriesPropertiesPublicationLocalServiceUtil.publishToLiveCategoriesProperties(scopeGroupId, vocabularyLive.getLocalId(), null);
			
			// 30/10/2015: No se refresca la cahé porque habría que refrescar la caché de TODOS los TPUs que forman
			// 10/02/2016: Se lanza la notificación de cambios a MAS, pero indicando que no hay que refrescar caché.
			CacheRefresh cr = new CacheRefresh(groupId, getGroups2Updt());
			cr.setSendMasNotification(getVocabulariesModifiedToNotify(result));
			cr.setRefreshTomcatAndApache(false);
			cr.setRescheduleCacheInvalidation(false);
			// 19/02/2016: Si hay cambios en artículos instrumentales, también se refrescan
			if (Validator.isNotNull(liveItemIds))
				cr.setRefreshCategoryArticles(true);
			XMLIOUtil.deleteRemoteCache(cr);
		}
		finally
		{
			enableRefreshRemoteCache(true);
		}
		return result;
	}
	
	/**
	 * 
	 * @param groupId
	 * @param localIds
	 * @param classNameValue
	 * @return false si al menos uno de los elementos NO existe en el LIVE
	 */
	private boolean existAllInLive(long groupId, String[] localIds, String classNameValue)
	{
		String sql = String.format(COUNT_NOT_EXIST_IN_LIVE, groupId, classNameValue, StringUtils.join(localIds, "','"));
		long numNotExist = Long.parseLong( PortalLocalServiceUtil.executeQueryAsList(sql).get(0).toString() );
		
		return numNotExist == 0;
	}
	
	/**
	 * @see http://jira.protecmedia.com:8080/browse/ITER-526?focusedCommentId=19492&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-19492
	 * @param companyId
	 * @param groupId
	 * @param userId
	 * @param processId
	 * @param plids
	 * @return Resultado de la publicación de los modelos nuca publicados al LIVE de las secciones a publicar
	 * @throws Throwable 
	 */
	private String publishSectionWebModel(long companyId, long groupId, long userId, String processId, String[] plids) throws Throwable
	{
		String result = "";
		
		// Se comprueba si las secciones tienen modelos que nunca se han publicado al LIVE
		Document dom = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_NEW_SECTION_MODELS, groupId, StringUtils.join(plids)) );
		String[] ids = XMLHelper.getStringValues(dom.selectNodes("/rs/row/@id"));
		
		if (Validator.isNotNull(ids))
			result = splitWebModelsIdsAndCallToPublication(companyId, groupId, userId, processId, ids, DEFAULT_CONTENT_TO_PUBLISH);		
		
		return result;
	}
	
	/**
	 * @param companyId
	 * @param groupId
	 * @param userId
	 * @param processId
	 * @param importToDo
	 * @return
	 * @throws Throwable
	 */
	private String publishSection(long companyId, long groupId, long userId, String processId, Node importToDo) throws Throwable
	{
		String result 					= StringPool.BLANK;
		boolean refreshSectionArticles	= false;
		boolean refreshContextVariables = false;
		
		deleteOrphanLayouts(groupId);
		
		try
		{
			enableRefreshRemoteCache(false);

			// Secciones
			_log.debug("Sections publication");	
			
			/* Indica que partes de la publicación se realizarán:
			"0" solo se publicarían las propiedades de la sección (section properties), el artículo instrumental(journalarticle) y su layout.
			"1" Se publica todo. Por defecto. */
			String contentsToPublish = XMLHelper.getTextValueOf(importToDo, "@contents", DEFAULT_CONTENT_TO_PUBLISH);
			
			final String[] plids = XMLHelper.getStringValues(importToDo.selectNodes("param"), ".");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(plids), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid plids");
	
			boolean mustPublishWebTheme = !existAllInLive(groupId, plids, IterKeys.CLASSNAME_LAYOUT);
			
			try
			{
				// http://jira.protecmedia.com:8080/browse/ITER-526?focusedCommentId=19492&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-19492
				// Se publican los modelos no publicados al Live antes de publicar las secciones que los utilizan
				String webmodelResult = publishSectionWebModel(companyId, groupId, userId, processId, plids);
				
				result = initPublicationSections(companyId, groupId, userId, processId, plids, contentsToPublish).asXML();	
				refreshSectionArticles = true;
				
				if (Validator.isNotNull(webmodelResult))
				{
					Element logsRoot = (Element)SAXReaderUtil.read(result).selectSingleNode("/iter/logs");
					
					List<Node> logs  = SAXReaderUtil.read(webmodelResult).selectNodes("/iter/logs/item");
					if (Validator.isNotNull(logs))
					{
						for (Node log : logs)
						{
							logsRoot.add( log.detach() );
						}
						result = logsRoot.getDocument().asXML();
					}
				}
			}
			catch (ServiceError se)
			{
				if (!se.getErrorCode().equals(IterErrorKeys.XYZ_E_XPORTCONTENT_EMPTY_ZYX))
					throw se;
			}
			
			try
			{
				refreshContextVariables = ContextVarsPublishLocalServiceUtil.publishCtxVars2(groupId, IterKeys.MILENIUM, plids, "1", false);
			}
			catch (ServiceError se)
			{
				if (!se.getErrorCode().equals(IterErrorKeys.XYZ_E_XPORTCONTENT_EMPTY_ZYX))
					throw se;
			}
			
			// ITER-528	Las nuevas secciones web no se visualizan con el diseño web en LIVE
			// Si es la primera vez que se publica alguna de las secciones, se publica además el tema
			// Se publican todos los temas del grupo
			if (mustPublishWebTheme)
				publishWebThemes(groupId, null, null, false);
		}
		finally
		{
			try
			{
				if (refreshSectionArticles || refreshContextVariables)
				{
					CacheRefresh cr = new CacheRefresh(groupId, getGroups2Updt());
					cr.setRefreshSectionArticles(true);
					cr.setRefreshScheduledPublications( getRefreshScheduledPublications() );
					cr.setRefreshContextVariables( refreshContextVariables );
					cr.setPublicationId(processId);
		
					XMLIOUtil.deleteRemoteCache(cr);
				}
			}
			catch (Throwable th)
			{
				_log.error(th);
			}

			enableRefreshRemoteCache(true);
		}
		return result;
	}
	
	private String publishWebModel(long companyId, long groupId, long userId, String processId, Node importToDo) throws Throwable
	{
		String result 					= StringPool.BLANK;

		try
		{
			enableRefreshRemoteCache(false);

			_log.debug("Web model publication");	
			
			 /* Hay dos tipos:
			     	Antiguos: modelos de artículos, sección y newsletter (metadatos no se publican).
			     		Se publican llamando a la vieja forma (publishToLiveSelectiveFlex)
			     		Recibimos de MLN el designpagetemplate.id_
			     	Recientes: menu, cabecera y pie (catálogos)
			 			Es como la publicación de una sección (layout) 
			 			Recibimos el "catalogPageId.catalogElementId" */
			
			final String[] ids = XMLHelper.getStringValues(importToDo.selectNodes("param"), ".");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(ids), IterErrorKeys.XYZ_E_INVALID_URL_ZYX, "Invalid web model id");
			
			result = splitWebModelsIdsAndCallToPublication(companyId, groupId, userId, processId, ids, DEFAULT_CONTENT_TO_PUBLISH);			
			
			CacheRefresh cr = new CacheRefresh(groupId, getGroups2Updt());
			XMLIOUtil.deleteRemoteCache(cr);
		}
		finally
		{
			enableRefreshRemoteCache(true);
		}
		return result;
	}

	/**
	 * @param groupId
	 * @throws Exception 
	 */
	private void publishWebThemes(long groupId, String templateIds, String themeId, boolean deleteRemoteCache) throws Exception
	{
		// Se bloquea la publicación y aplicación de temas para este grupo
		ThemeWebResourcesLocalServiceUtil.lockProcess(groupId);
		
		try
		{
			// Se obtiene la información a publicar
			Document infoDom = ThemeWebResourcesLocalServiceUtil.getPublishableInfo(groupId, themeId);
			
			// Si se especifican plantillas, se publican estas explícitamente
			if (Validator.isNotNull(templateIds))
				infoDom.getRootElement().add( JournalTemplateLocalServiceUtil.getTemplatesAsDOM(groupId, templateIds).getRootElement() );
				
			if ( !PropsValues.ITER_THEME_RSRC_MINIFY_ON_THE_FLY )
			{
				infoDom.getRootElement().addAttribute("minify", "false");
			}
			
			// Se obtiene la configuración del LIVE
			LiveConfiguration liveConf	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(IterGlobal.getCompanyId());
			
			// Se guarda el fichero en el disco
			String fullFilePath  = XMLIOUtil.generateFullFilePath(liveConf.getLocalPath(), "WebThemes", ".xml");
			File publishableFile = XMLIOUtil.generateFile(fullFilePath, infoDom.asXML());
			XMLIOUtil.sendFile(liveConf, publishableFile);
			
			// Se llama al LIVE para importar los contenidos, y posteriormente borrar la caché y notificar a los Apaches
			publishWebThemes_executeJSONRemoteCalls(liveConf, groupId, publishableFile.getName(), deleteRemoteCache);
			
			// Se actualiza la BBDD del entorno BACK notificando que ya se ha borrado del LIVE
			ThemeWebResourcesLocalServiceUtil.updateFromPublishProcess(groupId);
		}
		finally
		{
			ThemeWebResourcesLocalServiceUtil.unlockProcess(groupId);
		}
	}
	
	/**
	 * Se llama al LIVE para importar los contenidos, y posteriormente borrar la caché y notificar a los Apaches
	 * 
	 * @param liveConf
	 * @param groupId
	 * @param fileName
	 * @throws PortalException
	 * @throws SystemException
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws DocumentException
	 * @throws ServiceError 
	 * @throws NumberFormatException 
	 */
	private void publishWebThemes_executeJSONRemoteCalls(LiveConfiguration liveConf, long groupId, String fileName, boolean deleteRemoteCache) throws PortalException, SystemException, ClientProtocolException, IOException, DocumentException, NumberFormatException, ServiceError
	{
		// Se notifica al Live para que realice la importación
		String scopeGroupName = GroupLocalServiceUtil.getGroup(groupId).getName();

		List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
		remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.xmlio.service.ChannelServiceUtil"));
		remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"importWebThemesToLive"));
		remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[scopeGroupName, fileName]"));
		remoteMethodParams.add(new BasicNameValuePair("scopeGroupName", 	scopeGroupName));
		remoteMethodParams.add(new BasicNameValuePair("fileName", 			fileName));
		
		String []url = liveConf.getRemoteIterServer2().split(":");
		HttpHost targetHost = new HttpHost(url[0], Integer.valueOf(url[1]));
		JSONObject json = JSONUtil.executeMethod(targetHost, "/xmlio-portlet/secure/json", remoteMethodParams, 
									(int)liveConf.getConnectionTimeOut(),
									(int)liveConf.getOperationTimeOut(),
									liveConf.getRemoteUserName(), liveConf.getRemoteUserPassword());

		if (deleteRemoteCache)
		{
			// Actualiza la caché remota y llama a las URLptes
			Document dom = SAXReaderUtil.read(json.getString("returnValue"));
			String lastUpdate 	= XMLHelper.getStringValueOf( dom.getRootElement(), String.format("@%s",IterKeys.XMLIO_XML_LAST_UPDATE_ATTRIBUTE), StringPool.BLANK);
			
			XMLIOUtil.deleteRemoteCache(groupId, lastUpdate);
		}
	}
	
	// Realiza la publicación de los artículos instrumentales asignados a las categorías
	private String[] instrumentalArticleCategoryPublication(long companyId, long userId, long globalGroupId, long scopeGroupId, Long vocabularyId, AssetCategory category) throws Throwable
	{
		_log.trace("In vocabularyOrCategoryInstrumentalArticlesPublication");
		
		// Obtenemos los artículos instrumentales pendientes de publicar
		String[] liveItemIds = InstrumentalContentUtil.getPendingCategoriesInstrumentalArticles(
											vocabularyId == null ? category.getVocabularyId() : vocabularyId,
											vocabularyId == null ? category.getCategoryId()   : null);
		
		if (Validator.isNull(liveItemIds))
		{
			_log.debug("No pending instrumental article");
		}
		else
		{
			// Iniciamos la publicación del artículo instrumental del catálogo
			publishToLive(companyId, globalGroupId, userId, Long.toString(Calendar.getInstance().getTimeInMillis()), 
				          IterKeys.CLASSNAME_MILENIUMARTICLE, liveItemIds, false, scopeGroupId);
		}
		
		return liveItemIds;
	}

	private Document initPublicationSections(long companyId, long groupId, long userId, String processId, String[] ids) throws Throwable
	{
		return initPublicationSections(companyId, groupId, userId, processId, ids, DEFAULT_CONTENT_TO_PUBLISH); 
	}
	
	// Obtención de ids y llamada a la publicación de secciones. También se utiliza para publicar modelos web tanto desde MLN como de Iter
	private Document initPublicationSections(long companyId, long groupId, long userId, String processId, String[] ids, String contentsToPublish) throws Throwable
	{
		_log.trace("In initPublicationSections");
		
		Document resultDom = null;
		
		String sql = String.format(GET_XMLIO_LIVE_IDS, groupId, IterKeys.CLASSNAME_LAYOUT, StringUtil.merge(ids, "','"));
		List<Object> idList  = PortalLocalServiceUtil.executeQueryAsList(sql);
		ArrayList<String> liveItemIdList = new ArrayList<String>();
		for(Object id : idList)
			liveItemIdList.add( String.valueOf(id) );
		String [] liveItemIds = liveItemIdList.toArray( new String[liveItemIdList.size()] );
		
		String result = publishToLive(companyId, groupId, userId, processId, 
			                   IterKeys.CLASSNAME_MILENIUMSECTION, liveItemIds, true, groupId, contentsToPublish);
	
		resultDom = SAXReaderUtil.read(result);
		
		// Actualizamos las fechas de publicacion de las sectionproperties publicadas correctamente
		if(Validator.isNotNull(result))
		{
			String[] correctLayoutsIdsPublished = getCorrectLayoutsIdsPublished(resultDom);
			
			if (Validator.isNotNull(correctLayoutsIdsPublished))
			{
				LayoutLocalServiceUtil.updatePublicationDate(groupId, correctLayoutsIdsPublished);
			}
		}
		resultDom.setXMLEncoding("ISO-8859-1");
		return resultDom;
	}
	
	// Función auxiliar para separar los ids que llegan y lanzar las importaciones de modelos web
	private String splitWebModelsIdsAndCallToPublication(long companyId, long groupId, long userId, String processId, String[] ids, String contentsToPublish) throws Throwable
	{
		_log.trace("In splitWebModelsIdsAndCallToPublication");
		
		ErrorRaiser.throwIfFalse(Validator.isNotNull(ids), com.liferay.portal.kernel.error.IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid web models ids");
		
		String result = "";
		
		// Recorremos los ids viendo de qué tipo son			
		ArrayList<String> newIds = new ArrayList<String>();
		ArrayList<String> oldIds = new ArrayList<String>();
		Map<String, String> catalogMap = new HashMap<String, String>();
		
		for (int i = 0; i < ids.length; i++)
		{
			if (ids[i].contains("."))
			{
				// catalogpageid.catalogelementid
				String[] catalogInfo = ids[i].split("\\.");
				newIds.add( catalogInfo[0] );
				catalogMap.put(catalogInfo[1], ids[i]);
				catalogMap.put(catalogInfo[0], ids[i]);
			}
			else
			{
				oldIds.add(ids[i]);					// design_pagetemplate.id_
			}
		}
		
		// En una misma llamada no se pueden mezcar ids de un tipo de modelo (nuevos) con otros (viejos) 
		if (newIds.size() > 0 && oldIds.size() > 0)
		{
			ErrorRaiser.throwIfError(XmlioKeys.XYZ_E_MIXED_PUBLICATION_MODELS_ZYX, "Mixed publication models");
		}
		else
		{
			if (oldIds.size() > 0)
			{
				// Lanzamos las viejas importaciones
				_log.debug("Model web (article, section or newsletter) ids found");
				
				String[] auxIds = new String[oldIds.size()];
				auxIds = oldIds.toArray(auxIds);
				
				// MLN nos pasan los design_pagetemplate.ids_, obtenemos los design_pagetemplate.pageTemplateId
				String[] designPageTemplateids = getPageTemplateIds(auxIds);
				
				boolean mustPublishWebTheme = !existAllInLive(groupId, designPageTemplateids, IterKeys.CLASSNAME_PAGETEMPLATE);
				
				// Publicamos antes los layouts de los design page template no publicados				
				publishDesignPageTemplateLayoutNotPublished(companyId, groupId, userId, designPageTemplateids, null);
				
				// Obtenemos los ids "live"
				String [] liveItemIds = LiveServiceUtil.getLiveItemIdsFromLocalIds(companyId, 
						                                                           groupId, designPageTemplateids, 
						                                                           IterKeys.CLASSNAME_PAGETEMPLATE);
				
				result = publishToLive(companyId, groupId, userId, null, 
									   IterKeys.CLASSNAME_PAGETEMPLATE, liveItemIds, false, groupId);
				
				// ITER-528	Las nuevas secciones web no se visualizan con el diseño web en LIVE
				// Si es la primera vez que se publica alguno de los modelos, se publica además el tema.
				// Se publican todos los temas del grupo
				if (mustPublishWebTheme)
					publishWebThemes(groupId, null, null, false);
			}
			else
			{
				// Lanzamos las nuevas importaciones
				_log.debug("Model web (catalog) ids found");				
				
				String catalogpageIds = StringUtil.merge(newIds, "','");
				String errorCatalogpageIds = "";
				
				String completeQuery= String.format(GET_CATALOGPAGE_LAYOUT_NOT_PUBLISHED, String.format(GET_LAYOUT_4_PUBLISH_BY_FRIENDLYURL, "0 AS catalogpageid, "));
				String sql = String.format(completeQuery, IterKeys.CLASSNAME_LAYOUT, groupId, IterKeys.PENDING, IterKeys.ERROR, LayoutConstants.FRIENDLY_URL_CATALOG, catalogpageIds);
				
				if (_log.isDebugEnabled())
					_log.debug(new StringBuilder("GET_CATALOGPAGE_LAYOUT_NOT_PUBLISHED:\n").append(sql));

				Document catalogPageLayouts4Publish = PortalLocalServiceUtil.executeQueryAsDom(sql);
				
				Document publishResult = publishCatalogPageLayoutNotPublished(companyId, groupId, userId, catalogPageLayouts4Publish);
				
				if (Validator.isNotNull(publishResult))
				{
					String[] layoutWithErrors = updateWebModelLayoutPubLog(publishResult, catalogPageLayouts4Publish, catalogMap);
					if(layoutWithErrors.length>0)
					{
						// Si hay error en la publicación de un layout de los que pertenecen a los modelos web por defecto
						// no publicaremos el registro de sectionproperties correspondiente al plid null y grupo especificado.
						newIds.removeAll( Arrays.asList(layoutWithErrors) );
						catalogpageIds = StringUtil.merge(newIds, "','");
						errorCatalogpageIds = StringUtil.merge(layoutWithErrors, "','");
					}
				}
				
				String clause = "";
				if ( Validator.isNotNull(errorCatalogpageIds) && Validator.isNotNull(catalogpageIds) )
					clause = String.format(CLAUSE_NOT_IN_CATALOGPAGE, groupId, errorCatalogpageIds);
				
				Document currentDefaultCatalogPages = getDefaultSectionPropertiesAndPublish(groupId, String.format(SELECTED_CATALOGPAGEIDS, catalogpageIds), clause);
				
				publishResult = processWebModelPublicationResult(publishResult, currentDefaultCatalogPages, catalogMap);
				ErrorRaiser.throwIfNull(publishResult, IterErrorKeys.XYZ_E_XPORTCONTENT_EMPTY_ZYX);
				
				result = publishResult.asXML();
			}
		}		
						
		return result;
	}
	
	private Document processWebModelPublicationResult(Document publishResult, Document currentDefaultCatalogPages, Map<String, String> catalogMap)
	{
		// http://jira.protecmedia.com:8080/browse/ITER-377?focusedCommentId=15200&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-15200
		// Si NO han habido cambios en el SectionProperties pero sí en al menos un Layout de los modelos, se devuelve dicho resultado.
		// No se puede lanzar posteriormente un XYZ_E_XPORTCONTENT_EMPTY_ZYX.
		Document retVal = publishResult;
		
		if (currentDefaultCatalogPages!=null && currentDefaultCatalogPages.selectNodes("/rs/row").size() > 0)
		{
			String header 	= XMLHelper.getTextValueOf(currentDefaultCatalogPages, "/rs/row/@headerelementid", 	"");
			String menu		= XMLHelper.getTextValueOf(currentDefaultCatalogPages, "/rs/row/@menuelementid",	"");
			String footer 	= XMLHelper.getTextValueOf(currentDefaultCatalogPages, "/rs/row/@footerelementid", 	"");
			
			if (Validator.isNotNull(publishResult))
			{
				String catalogPageId = catalogMap.get(header);
				updateWebModelPublicationLog(publishResult, catalogPageId);
				
				catalogPageId = catalogMap.get(menu);
				updateWebModelPublicationLog(publishResult, catalogPageId);
				
				catalogPageId = catalogMap.get(footer);
				updateWebModelPublicationLog(publishResult, catalogPageId);
				
				retVal = publishResult;
			}
			else
			{
				retVal = createWebModelPublicationLog( currentDefaultCatalogPages.getRootElement().attributeValue("groupname"), catalogMap.get(header), catalogMap.get(menu), catalogMap.get(footer) );
			}
		}
		
		return retVal;
	}
	
	private Document getDefaultSectionPropertiesAndPublish(long groupId, String selectedCatalogpageIds, String clause) throws NoSuchMethodException, SecurityException, PortalException, SystemException, ClientProtocolException, IOException, DocumentException, SQLException, ServiceError
	{
		String query = String.format(GET_DEFAULT_SECTION_PROPERTIES, groupId, selectedCatalogpageIds, clause);
		if (_log.isDebugEnabled())
			_log.debug("GET_DEFAULT_SECTION_PROPERTIES:\n" + query);
		
		Document currentDefaultCatalogPages = PortalLocalServiceUtil.executeQueryAsDom(query);
		
		if(currentDefaultCatalogPages!=null && currentDefaultCatalogPages.selectNodes("/rs/row").size()>0 )
		{
			String scopeGroupName = GroupLocalServiceUtil.getGroup(groupId).getName();
			currentDefaultCatalogPages.getRootElement().addAttribute("groupname", scopeGroupName);
				
			publishDefaultSectionProperties( currentDefaultCatalogPages );
			
			query = String.format(UPDATE_DEFAULT_SECTION_PROPERTIES_PUB_DATE, SQLQueries.getCurrentDate(), groupId);
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		else if(_log.isDebugEnabled())
			_log.debug("There are not default section properties to publish.");
		
		return currentDefaultCatalogPages;
	}
	
	private String[] updateWebModelLayoutPubLog(Document publishLayoutsResult, Document catalogPageLayouts4Publish, Map<String, String> catalogMap)
	{
		String xpath = "/iter/logs/item[@localId='%s' and @classname='com.liferay.portal.model.Layout']";
		
		List<Node> catalogPageLayoutList = catalogPageLayouts4Publish.selectNodes("/rs/row");
		for(Node catalogPageLayout : catalogPageLayoutList)
		{
			Node layoutNode = publishLayoutsResult.selectSingleNode( String.format(xpath, XMLHelper.getTextValueOf(catalogPageLayout, "@localid", "") ) );
			if(Validator.isNotNull(layoutNode))
			{
				String catalogpageid = XMLHelper.getTextValueOf(catalogPageLayout, "@catalogpageid", "");
				((Element)layoutNode).addAttribute("catalogpageid", catalogpageid);
				((Element)layoutNode).addAttribute("webmodelid", catalogMap.get(catalogpageid));
			}
		}
		
		String[] layoutWithErrors = 
				XMLHelper.getStringValues( 
						publishLayoutsResult.selectNodes("/iter/logs/item[@status!='done' and @classname='com.liferay.portal.model.Layout']"),
						"@catalogpageid");
		
		if( layoutWithErrors.length>0 )
			_log.error("Error in webmodel layout publication:\n" + publishLayoutsResult.asXML());
		else if(_log.isDebugEnabled())
			_log.debug("webmodel layout publication result:\n" + publishLayoutsResult.asXML());
		
		return layoutWithErrors;
	}
	
	private void updateWebModelPublicationLog(Document publishLayoutsResult, String catalogPageId)
	{
		String xpath = "/iter/logs/item[@webmodelid='%s']";
		if( Validator.isNotNull(catalogPageId) && Validator.isNull( publishLayoutsResult.selectSingleNode( String.format(xpath, catalogPageId) ) ) )
		{
			Node logsNode = publishLayoutsResult.selectSingleNode("/iter/logs");
			addItemElement((Element)logsNode, catalogPageId);
		}
	}
	
	private Document createWebModelPublicationLog(String scopeGroupName, String header, String menu, String footer)
	{
		Document logDom = SAXReaderUtil.createDocument();
		logDom.setXMLEncoding("ISO-8859-1");
		
		Element iterElem = logDom.addElement("iter");
		iterElem.addAttribute("scopegroupid", scopeGroupName);
		Element logsElem = iterElem.addElement("logs");
		
		addItemElement(logsElem, header);
		addItemElement(logsElem, menu);
		addItemElement(logsElem, footer);
		
		return logDom;
	}
	
	private void addItemElement(Element logsElement, String catalogPageId)
	{
		if( Validator.isNotNull(catalogPageId) )
		{
			Element newItem = logsElement.addElement("item");
			newItem.addAttribute("classname", "com.liferay.portal.model.Layout");
			newItem.addAttribute("webmodelid", catalogPageId);
			newItem.addAttribute("status", "done");
		}
		else if(_log.isDebugEnabled())
			_log.debug("webmodelid is empty or null: " + catalogPageId);
	}
	
	private void publishDefaultSectionProperties(Document currentDefaultCatalogPages) throws JSONException, ClientProtocolException, SystemException, IOException, DocumentException, ServiceError
	{
		LiveConfiguration liveConf	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(IterGlobal.getCompanyId());

		List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
		remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.xmlio.service.ChannelServiceUtil"));
		remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"importDefaultSectionProperties"));
		remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[xml]"));
		remoteMethodParams.add(new BasicNameValuePair("xml", 				currentDefaultCatalogPages.asXML()));
		
		String []url = liveConf.getRemoteIterServer2().split(":");
		HttpHost targetHost = new HttpHost(url[0], Integer.valueOf(url[1]));
		JSONUtil.executeMethod(targetHost, "/xmlio-portlet/secure/json", remoteMethodParams, 
									(int)liveConf.getConnectionTimeOut(),
									(int)liveConf.getOperationTimeOut(),
									liveConf.getRemoteUserName(), liveConf.getRemoteUserPassword());
		
	}
	
	private Document publishCatalogPageLayoutNotPublished(long companyId, long groupId, long userId, Document doc) throws Throwable
	{
		Document retVal = null;
		
		List<Node> nodes = doc.getRootElement().selectNodes("//row");
		
		if( Validator.isNotNull(nodes) )
		{
			String plids[] = XMLHelper.getStringValues(nodes, "@localid");
			
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder(plids.length).append(" catalog layout/s are not published. Publishing it...") );
				
			retVal = initPublicationSections(companyId, groupId, userId, null, plids);
		}
		else
		{
			_log.debug("All catalog layout are published");
		}
		
		return retVal;
	}
	
	// Publica los layouts de los design page template no publicados antes de publicar el propio desig page template
	private void publishDesignPageTemplateLayoutNotPublished(long companyId, long groupId, long userId, String[] designPageTemplateIds_, String xmlioLiveId) throws Throwable
	{
		_log.debug("In checkPageTemplateLayouts");
		
		// No pueden llegar los dos nulos
		if (Validator.isNull(designPageTemplateIds_) && Validator.isNull(xmlioLiveId))
		{
			ErrorRaiser.throwIfError(com.liferay.portal.kernel.error.IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "design template ids and xmlio_live are null");
		}
		
		String ids = "";
		
		// Llamada desde MLN
		if (Validator.isNotNull(designPageTemplateIds_))
		{
			
			StringBuilder auxIds = new StringBuilder();		
			for (int d = 0; d < designPageTemplateIds_.length; d++)
			{
				auxIds.append("'").append(designPageTemplateIds_[d]).append("'");
				
				if(d < designPageTemplateIds_.length - 1)
				{
					auxIds.append(", ");
				}
			}
			ids = String.format(PAGETEMPLATEID_COND, auxIds.toString());
			
		// Llamada desde Flex
		}
		else
		{
			ids = String.format(ID_COND, xmlioLiveId);
		}
		
		String completeQuery = String.format(DESIGN_PAGE_TEMPLATE_LAYOUT_NOT_PUBLISHED, String.format(GET_LAYOUT_4_PUBLISH_BY_FRIENDLYURL, StringPool.BLANK));
		String sql = String.format(completeQuery, IterKeys.CLASSNAME_LAYOUT, groupId, IterKeys.PENDING, IterKeys.ERROR, IterKeys.PARENT_LAYOUT_URL, ids);
		
		if (_log.isDebugEnabled())
		{
			_log.debug(new StringBuilder("Query to get non published design page template layouts:\n").append(sql));
		}
		
		Document doc = PortalLocalServiceUtil.executeQueryAsDom(sql);
		List<Node> rows = doc.getRootElement().selectNodes("//row");
			
		if (null != rows && rows.size() > 0)
		{
			String[] notPublishedLayouts = XMLHelper.getStringValues(rows, "@localid");
			
			if(_log.isDebugEnabled())
			{
				_log.debug(new StringBuilder(notPublishedLayouts.length).append(" design page template layout/s are not published. Publishing it...") );
			}
				
			initPublicationSections(companyId, groupId, userId, null, notPublishedLayouts);
		}
		else
		{
			_log.debug("All design page template layout are published");
		}
	}
		
	// Con el resultado de la publicación obtiene los ids de los layouts que fueron publicados correctamente
	private String[] getCorrectLayoutsIdsPublished(Document publicationResult) throws DocumentException{
		_log.trace("In updateSectionPropertiesPublicationDate");
		
		String[] result = null;
		
		if (Validator.isNotNull(publicationResult))
		{
			// Obtenemos los ids de los layouts que se han importado correctamente
			List<Node> nodes = publicationResult.getRootElement().selectNodes("/iter/logs/item[@classname='" + IterKeys.CLASSNAME_LAYOUT + "' and @status='done']");
			if (Validator.isNotNull(nodes))
				result = XMLHelper.getStringValues(nodes, "@localId");			
		}
		
		return result;
	}
	
	// Obtiene los datos para hacer una petición json posteriormente
 	public Object[] getParametersToCallByJson(long companyId) throws ServiceError{
		
		/* El método al que se llamaría más adelante es: 
           executeJSONRemoteMethod(companyId, remoteIP, remotePort, remoteUserName, remoteUserPassword, remoteMethodPath, remoteMethodParams); */
		
		LiveConfiguration liveConf 	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(companyId);
		final String remoteIterServer 	= liveConf.getRemoteIterServer2();		
		
		Object[] data = new Object[5];
		data[0] = companyId;										// long companyId
		data[1] = remoteIterServer.split(":")[0];					// String ip
		data[2] = Integer.valueOf(remoteIterServer.split(":")[1]);  // int remote port
		data[3] = liveConf.getRemoteUserName();						// String user
		data[4] = liveConf.getRemoteUserPassword();					// String password
		
		return data;
	}
 	
 	// Obtiene los pageTemplateId desde los identificadores dados por MLN (id_) 
 	private String[] getPageTemplateIds(String[] ids) throws SecurityException, NoSuchMethodException, com.liferay.portal.kernel.error.ServiceError
 	{
 		_log.trace("In getPageTemplateIds");
 		
 		ErrorRaiser.throwIfNull(ids, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid ids"); 		
 		
 		String auxIds = QueryUtil.buildSqlIn(ids, false, false);
 		
 		String sql = String.format(GET_PAGETEMPLATEID, auxIds);
 		if (_log.isDebugEnabled())
 			_log.debug(new StringBuilder("Query to get design_pagetemplate.pageTemplateId:\n").append(sql));
 		
 		Document doc = PortalLocalServiceUtil.executeQueryAsDom(sql);
 		
 		List<Node> nodes = doc.getRootElement().selectNodes("//row");
 		
 		ErrorRaiser.throwIfFalse(null != nodes, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "No design_pagetemplates found with the ids given");
 		
 		String data[] = XMLHelper.getStringValues(nodes, "@pageTemplateId");
 		
 		return data;
 	}
 	
 	
 	// Publicación de catálogos (menu, cabecera y pie. catálogos enteros o sólo páginas de catálogos) para ITER unicamente. MLN llama a publishToLive
 	public String publishCatalogsIter(String xmlData) throws Throwable
 	{
 		_log.trace("In publishCatalogsIter");
 		
 		ErrorRaiser.throwIfFalse(Validator.isNotNull(xmlData), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "No data given.");
 		
 		Document doc = SAXReaderUtil.read(xmlData);
 		Node root = doc.getRootElement();
 		
 		String groupId = XMLHelper.getTextValueOf(root, "/rs/@scopeGroupId");
 		ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid groupId");
 		
 		String companyId = XMLHelper.getTextValueOf(root, "/rs/@companyId");
 		ErrorRaiser.throwIfFalse(Validator.isNotNull(companyId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid companyId");
 		
 		String userId = XMLHelper.getTextValueOf(root, "/rs/@userId");
 		ErrorRaiser.throwIfFalse(Validator.isNotNull(userId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid userId");
 		
 		// Indica si se quiere publicar catálogos enteros o solo páginas de catálogos
 		String publicationType = XMLHelper.getTextValueOf(root, "/rs/@type"); 
 		ErrorRaiser.throwIfFalse(Validator.isNotNull(publicationType), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid publicationType"); 		
 		
		List<Node> rows = root.selectNodes("//row");
		ErrorRaiser.throwIfNull(rows, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "No ids given");
		
		// Indentificadores de catálogo (catalog.catalogid o catalogpage.catalogpageid)
		String[] auxIds = XMLHelper.getStringValues(rows, "@id");
		ErrorRaiser.throwIfNull(auxIds, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Ivalid ids given");
		String ids = QueryUtil.buildSqlIn(auxIds, false, true);
		
		// Obtenemos los layouts a publicar
		String sql = null;
		// Todos los del catálogo
		if (publicationType.equalsIgnoreCase("catalog"))
		{
			sql = String.format(CatalogQueries.GET_PLIDS_FROM_CATALOGS, ids); 
		}
		// Los correspondientes a las páginas indicadas
		else
		{
			sql = String.format(CatalogQueries.GET_PLID_FROM_CATALOG_PAGES, ids);
		}
		if (_log.isDebugEnabled())
			_log.debug(new StringBuilder("Query to get plids:\n").append(sql));
		
		doc = PortalLocalServiceUtil.executeQueryAsDom(sql);
		rows = doc.getRootElement().selectNodes("//row");		
		ErrorRaiser.throwIfFalse(null != rows && rows.size() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "No layouts found from catalogs given");
		
		String[] layoutsIds = XMLHelper.getStringValues(rows, "@plid");
		
		// Iniciamos la Publicación de los layouts (que a su vez publicarían los catálogos correspondientes)
		Document resultDom = initPublicationSections(Long.valueOf(companyId), Long.valueOf(groupId), Long.valueOf(userId), null, layoutsIds, DEFAULT_CONTENT_TO_PUBLISH);
		
		List<Node> errorList = resultDom.getRootElement().selectNodes("/iter/logs/item[@classname='" + IterKeys.CLASSNAME_LAYOUT + "' and @status='error']");
		ErrorRaiser.throwIfFalse( errorList.size()==0 , IterErrorKeys.XYZ_E_PUBLISH_CATALOG_FAILED_ZYX, getInfoMessage(errorList));
		
		return StringPool.BLANK;
	}

	private String getInfoMessage(List<Node> errorList)
	{
		StringBuilder retval = new StringBuilder("\n");
		
		for(Node error : errorList)
		{
			retval.append("\t Layout ")
					.append( XMLHelper.getTextValueOf(error, "@localId") )
					.append(StringPool.COMMA_AND_SPACE)
					.append("message: ")
					.append( XMLHelper.getTextValueOf(error, "text()") )
					.append("\n");
		}
			
		return retval.toString();
	}
	
	public boolean getExistsInLive(long groupId, String classNameValue, String localId) throws ServiceError
	{
		boolean existInLive = false;
		
		ErrorRaiser.throwIfFalse( groupId!=0, IterErrorKeys.XYZ_ITR_E_INVALID_GROUP_ID_ZYX);
		ErrorRaiser.throwIfFalse( Validator.isNotNull(classNameValue), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse( Validator.isNotNull(localId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String query = String.format(
				"SELECT existInLive from xmlio_live WHERE groupId=%s AND classNameValue='%s' AND localId='%s'",
							groupId,
							classNameValue,
							localId);
		
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList(query);
		if(result!=null && result.size()==1)
			existInLive = GetterUtil.getBoolean( String.valueOf( result.get(0) ).equalsIgnoreCase("S"), false);
		
		return existInLive;
	}
	
	/*Dada una relación de identificadores globales, retorna sus correspondientes id locales.
	  Este método opera con los datos existentes en el entorno que procesa la llamada*/
	public Document idGlobalToIdLocal(String xmlGlobalId) throws Throwable
	{
		if(_log.isDebugEnabled())
			_log.debug("INPUTDATA_IDGLOBAL_TO_IDLOCAL: "+ xmlGlobalId );
		
		Document globalIdsDom = SAXReaderUtil.read(xmlGlobalId);
		
		List<Node> sites = globalIdsDom.selectNodes("/map/site");
		String[]  siteglobalids_escapeSql = escapeSql(XMLHelper.getStringValues(sites, "@siteglobalid"));
		String siteglobalids  =  StringUtil.merge( siteglobalids_escapeSql, "','" );
		
		String sql_sitelocalId = String.format(GET_XMLIO_LIVE_SITE_LOCAL_ID, siteglobalids);

		
		Document doc_siteLocalId = PortalLocalServiceUtil.executeQueryAsDom(sql_sitelocalId);
		if(_log.isDebugEnabled())
			_log.debug("GET_LIVE_SITE_LOCAL_ID_RESULT: "+ doc_siteLocalId.asXML());
		
		Boolean executeGetXmlioLiveLocalIdsSql = false;
		StringBuilder sql_localIds = new StringBuilder().append(GET_XMLIO_LIVE_LOCAL_IDS);
		
		Document localIdsBySiteDom = SAXReaderUtil.createDocument();
 		
		Element map = localIdsBySiteDom.addElement("map");
 		
		for(int i = 0; i < sites.size(); i++ )
		{
			Element site = map.addElement("site");
			
			Node siteNode = sites.get(i);
			
			String siteglobalid =  XMLHelper.getTextValueOf(siteNode ,"@siteglobalid");
			ErrorRaiser.throwIfNull(siteglobalid, IterErrorKeys.XYZ_ITR_E_EMPTY_GLOBAL_ID_ZYX);
			 
			
			/*se obtiene para cada item el sitelocalid( para el class Group es el mismo que el groupId). 
			  En caso de no encontrarlo( por ejemplo, si el grupo del back no se publicado ) se retornará una cadena vacía*/
			Node siteData = doc_siteLocalId.selectSingleNode( String.format("/rs/row[@globalId= %s]", StringUtil.escapeXpathQuotes(siteglobalid)  ) );
			String sitelocalid = XMLHelper.getTextValueOf(siteData,"@sitelocalid", "");
			
			
			site.addAttribute("siteglobalid", 	siteglobalid);
			site.addAttribute("sitelocalid", 	sitelocalid);
			
			List<Node> idsBySite = siteNode.selectNodes("./id");
			if( Validator.isNotNull(sitelocalid) && idsBySite.size() > 0  )
			{
				//se va construyendo la sql para obtener los localId de los nodos id del xml de entrada
				StringBuilder sql_append = buildConditionToAppendLocalIdSql( sitelocalid ,siteNode);
				
				if(!executeGetXmlioLiveLocalIdsSql)
					executeGetXmlioLiveLocalIdsSql = true;
				else if( sql_append.length() > 0 )
					sql_localIds.append("\nOR \n");
				
				sql_localIds.append(sql_append.toString());
			}
		}
	
		if(executeGetXmlioLiveLocalIdsSql)
		{
	 		if(_log.isDebugEnabled())
				_log.debug("GET_XMLIO_LIVE_LOCALIDS: " +sql_localIds );
	 		
	 		Document doc_localIds = PortalLocalServiceUtil.executeQueryAsDom(sql_localIds.toString(), true, "map","id");	
	 		
	 		if(_log.isDebugEnabled())
				_log.debug("RESULT_GET_XMLIO_LIVE_LOCALIDS: " +doc_localIds.asXML() );
	 		
	 		//se elimina el atributo livelayoutid de los nodos de doc_localIds cuyo atributo livelayoutid sea ""(serán todos los nodos excepto los que tengan class=section )
	 		List<Node> nodes_livelayoutid_empty  = doc_localIds.selectNodes(String.format("/map/id[@class!='%s']", "section"));
	 		delEmptylivelayoutidAttr(nodes_livelayoutid_empty);
	 		
	 		/*Para los class="dlfileentry" se cambia el valor del atributo localId por DLFileEntry.name
	 		 *Para los class="pagetemplate" se cambia el valor del atributo localId por Designer_PageTemplate.id_ 
	 		 *Para los class="pagecontent" se cambia el valor del atributo localId por news_pagecontent.id_*/
	 		List<Node> nodes_localIds_dpt  = doc_localIds.selectNodes(String.format("/map/id[@class='%s']", "pagetemplate"));
	 		List<Node> nodes_localIds_dlfe  = doc_localIds.selectNodes(String.format("/map/id[@class='%s']", "dlfileentry"));
	 		List<Node> nodes_localIds_pc  = doc_localIds.selectNodes(String.format("/map/id[@class='%s']", "pagecontent"));
	 		
	 		String nameAttrId = "localId";
	 		String stringFormatGetIdS = "//row[@localId ='%s']/@Id";
	 		
	 		checkChangeValueIdAttr( nodes_localIds_dpt, nodes_localIds_dlfe, nodes_localIds_pc, nameAttrId, stringFormatGetIdS , GET_DESIGNER_PAGETEMPLATE_IDS, GET_DLFILEENTRY_IDS, GET_PAGE_CONTENT_IDS, false );
	 		
	 		//se modifica localIdsBySiteDom y se añaden los nodos 'id' correspondientes a cada grupo
	 		List<Node> Nodessite = localIdsBySiteDom.selectNodes("/map/site");
	 		
	 		for(int j =0; j < Nodessite.size(); j++ )
	 		{
	 			Node nodeSite = Nodessite.get(j);
	 			String sitelocalid = XMLHelper.getTextValueOf(nodeSite,"@sitelocalid");
	 			
	 			List<Node> nodes_byGroup = doc_localIds.selectNodes( String.format("/map/id[@groupid= '%s']", sitelocalid  ));
	 			for(int k=0; k< nodes_byGroup.size(); k++)
	 			{
	 				//se elimina el atributo groupid
	 				Element nodeByGroup = ((Element)nodes_byGroup.get(k));
	 				nodeByGroup.remove( nodeByGroup.attribute("groupid")  );
	 			}
	 			
	 			//se añade la lista de nodos id al elemento nodeSite
	 			((Element)nodeSite ).setContent(nodes_byGroup);
	 		}
		}
		
		return localIdsBySiteDom;
	}
	
	private String [] escapeSql(String [] siteglobalids)
	{
		
		for( int i = 0; i < siteglobalids.length; i++  )
		{
			siteglobalids[i] = StringEscapeUtils.escapeSql(siteglobalids[i].toString());
		}
		
		return siteglobalids;
		
	}
	
	private void delEmptylivelayoutidAttr(List<Node> nodesToDelEmptyAttr)
	{
		for(int i=0; i < nodesToDelEmptyAttr.size(); i++ )
		{
			Element node = (Element)nodesToDelEmptyAttr.get(i);
			node.remove(node.attribute("livelayoutid"));
		}
	}
	
	private void checkChangeValueIdAttr( List<Node> nodes_pagetemplate , List<Node> nodes_dlfileentry,  List<Node> nodes_pc, String nameAttrId, String stringFormatGetIdS, 
			String dpt_getIdsSql, String dlfe_getIdsSql, String pc_getIdsSql, boolean addNewIdAttr) throws Exception
	{
		if (nodes_pagetemplate.size() > 0 ) 
 		{
 			//hay nodos con class="pagetemplate", por tanto cambiar el atributo localId por el correspondiente
 			Document doc_dptLocalIds = getDocIdsByClass(nodes_pagetemplate, '@'+nameAttrId, dpt_getIdsSql, "','");
 			changeIdByClass( nodes_pagetemplate, doc_dptLocalIds , nameAttrId, stringFormatGetIdS, addNewIdAttr) ;
 		}
 		if(nodes_dlfileentry.size() > 0 )
 		{
 			//hay nodos con class="dlfileentry", por tanto cambiar el atributo localId por el correspondiente
 			Document doc_dlfeLocalIds = getDocIdsByClass(nodes_dlfileentry, '@'+nameAttrId, dlfe_getIdsSql, "','");
 			changeIdByClass( nodes_dlfileentry, doc_dlfeLocalIds, nameAttrId, stringFormatGetIdS, addNewIdAttr);

 		}
 		if(nodes_pc.size() > 0 )
 		{
 			//hay nodos con class="pagecontent", por tanto cambiar el atributo localId por el correspondiente
 			Document doc_pcLocalIds = getDocIdsByClass(nodes_pc, '@'+nameAttrId, pc_getIdsSql, "','");
 			changeIdByClass( nodes_pc, doc_pcLocalIds, nameAttrId, stringFormatGetIdS, addNewIdAttr);

 		}
	}
	
	private Document getDocIdsByClass( List<Node> nodesByclass, String nameAttrId, String getIdsSql, String mergeDelimiter) throws Exception
	{
		String localIds = StringPool.BLANK;
		localIds = StringUtil.merge( XMLHelper.getStringValues(nodesByclass, nameAttrId), mergeDelimiter );
 		
		String idsSql =  String.format(getIdsSql, localIds);
		if(_log.isDebugEnabled())
			_log.debug("GET_IDS_BYCLASS: " +idsSql );
		
		Document idsDoc = PortalLocalServiceUtil.executeQueryAsDom( idsSql );	
		return idsDoc;
	}
	
	private void changeIdByClass( List<Node> nodesByclass, Document docIds, String nameAttrId , String stringFormatGetIds, boolean addNewAttr)
	{
		for(int j= 0; j < nodesByclass.size(); j++)
 		{
 			Attribute localIdAttr = ((Element)nodesByclass.get(j)).attribute(nameAttrId);
 			
 			String newlocalId = XMLHelper.getTextValueOf(docIds, String.format(stringFormatGetIds, localIdAttr.getValue()), ""); 
 			//si el id correspondiente según la clase no se ha encontrado, se mantiene el id
 			if(!newlocalId.equals(StringPool.BLANK))
 			{
 				if(!addNewAttr)
 					localIdAttr.setValue(newlocalId);
 				else
 					((Element)nodesByclass.get(j)).addAttribute("id", newlocalId);
 			}
 				
 		}	
	}
	
	private StringBuilder buildConditionToAppendLocalIdSql( String sitelocalid, Node siteNode) throws ServiceError 
	{
		StringBuilder sql_append = new StringBuilder();
		
		//se obtiene una lista de globalids clasificados por clases
		String ids_classPageTemplate = getIdsByClass(siteNode, "pagetemplate", "@gid" );
		String ids_classPageContent  = getIdsByClass(siteNode, "pagecontent",  "@gid" );
		String ids_classCategory  	 = getIdsByClass(siteNode, "category",     "@gid" );
		String ids_classVocabulary   = getIdsByClass(siteNode, "vocabulary",   "@gid" );
		String ids_classDlfileentry  = getIdsByClass(siteNode, "dlfileentry",  "@gid" );
		String ids_classSection  	 = getIdsByClass(siteNode, "section",  	   "@gid" );
		String ids_classSuscription  = getIdsByClass(siteNode, "suscription",  "@gid" );
		
		sql_append.append(buildGidClassNameCondition( sql_append, sitelocalid, IterKeys.CLASSNAME_PAGETEMPLATE,  ids_classPageTemplate)	)
		.append(buildGidClassNameCondition( sql_append, sitelocalid,IterKeys.CLASSNAME_PAGECONTENT, ids_classPageContent )	)
		.append(buildGidClassNameCondition( sql_append, sitelocalid,IterKeys.CLASSNAME_DLFILEENTRY, ids_classDlfileentry )	)
		.append(buildGidClassNameCondition( sql_append, sitelocalid, IterKeys.CLASSNAME_CATEGORY, ids_classCategory )	)
		.append(buildGidClassNameCondition( sql_append, sitelocalid, IterKeys.CLASSNAME_VOCABULARY, ids_classVocabulary )	)
		.append(buildGidClassNameCondition( sql_append, sitelocalid, IterKeys.CLASSNAME_LAYOUT, ids_classSection )	)
		.append(buildGidClassNameCondition( sql_append, sitelocalid, IterKeys.CLASSNAME_PRODUCT, ids_classSuscription )).append(") )");
		
		return sql_append;
	}
	
	private String buildGidClassNameCondition(StringBuilder actualsql, String sitelocalid, String classNameValue, String ids)
	{
		String condition = "";
		if( actualsql.length() == 0 )
			condition = Validator.isNotNull(ids) ? String.format(" (xl.groupId= %s AND  ( 	\n  (globalId IN ('%s') AND classNameValue='%s') \n", sitelocalid, ids, classNameValue) : ""; 
		else
			condition = Validator.isNotNull(ids) ? String.format(" OR (globalId IN ('%s') AND classNameValue='%s') \n", ids, classNameValue) : ""; 
	
		return condition;
	}
	
	/*Dada una relación de identificadores locales del entorno PREVIEW, retorna los correspondientes identificadores locales del entorno LIVE.
	  Este método solo funciona en el entorno PREVIEW */
	public String localBackToLocalLive(String xmlLocalBackId) throws Throwable
	{
		
		if(_log.isDebugEnabled())
			_log.debug("INPUTDATA_LOCALBACK_TO_LOCALLIVE: " + xmlLocalBackId );
		
		String localLiveIds = "";
		
		Document dom = SAXReaderUtil.read(xmlLocalBackId);
		
		/*Para los class="dlfileentry" backid es el valor de DLFileEntry.name, se cambia el valor del atributo por el correspondiente xmlio_live.localId
 		 *Para los class="pagetemplate" backid es el valor de Designer_PageTemplate.id_, se cambia el valor del atributo por el correspondiente xmlio_live.localId 
		 *Para los class="pagecontent" backid es el valor de news_pagecontent.id_, se cambia el valor del atributo por el correspondiente xmlio_live.localId */
		List<Node> nodes_pagetemplate  = dom.selectNodes(String.format("/map/site/id[@class='%s']", "pagetemplate"));
 		List<Node> nodes_dlfileentry  = dom.selectNodes(String.format("/map/site/id[@class='%s']", "dlfileentry"));
 		List<Node> nodes_pc  = dom.selectNodes(String.format("/map/site/id[@class='%s']", "pagecontent"));
 		
 		String nameAttrId = "backid";
 		String stringFormatGetIdS = "//row[@Id ='%s']/@localId";
 		
 		checkChangeValueIdAttr( nodes_pagetemplate, nodes_dlfileentry, nodes_pc, nameAttrId, stringFormatGetIdS, GET_DESIGNER_PAGETEMPLATE_LOCALIDS, GET_DLFILEENTRY_LOCALIDS, GET_PAGE_CONTENT_LOCALIDS, true);
 		
		//se obtiene una lista de los nodos <id>
		List<Node> sitebackids = dom.selectNodes("/map/site");
	
		StringBuilder sql = new StringBuilder().append(GET_XMLIO_LIVE_GLOBALIDS);
		
		
		for(int i = 0; i < sitebackids.size(); i++ )
		{
			//de cada nodo se extrae el groupid
			Node siteNode = sitebackids.get(i);
			long sitebackid = XMLHelper.getLongValueOf(siteNode, "@sitebackid");
			ErrorRaiser.throwIfFalse(sitebackid > 0, IterErrorKeys.XYZ_ITR_E_INVALID_GROUP_ID_ZYX);
			
			//por cada grupo se añade la condición correspondiente al sql
			sql.append( getConditionToAppendSql( sitebackid, siteNode ) );
			
			if( i + 1 < sitebackids.size() )
				sql.append("\nOR \n");
	 		
		}
	 	if(_log.isDebugEnabled())
			_log.debug("GET_GLOBAL_IDS: "+ sql);
	 		
	 	
	 	Document globalIdsDom = PortalLocalServiceUtil.executeQueryAsDom(sql.toString(), true, "map","id");
 		
	 	if(_log.isDebugEnabled())
			_log.debug("GET_GLOBAL_IDS_RESULT: "+ globalIdsDom.asXML());
	 		
	 	//se forma el xml de entrada del método idGlobalToIdLocal
	 	Document globalIdsBySiteIdDom = SAXReaderUtil.createDocument();
	 	
	 	Element map = globalIdsBySiteIdDom.addElement("map");
	 		
	 	for(int k= 0; k < sitebackids.size(); k++ )
	 	{
			long sitebackid = XMLHelper.getLongValueOf(sitebackids.get(k), "@sitebackid");
			ErrorRaiser.throwIfFalse(sitebackid > 0, IterErrorKeys.XYZ_ITR_E_INVALID_GROUP_ID_ZYX);
				
			//se extrae el nodo que corresponde al grupo para eliminarlo, obteniendo antes el atributo gid 
			Node node_classGroup 	 =  globalIdsDom.selectSingleNode( String.format("/map/id[@backid= '%s']", String.valueOf(sitebackid)  ) );
			
			if( _log.isDebugEnabled() && Validator.isNull(node_classGroup) )
				_log.debug("groupid not found: "+ String.valueOf(sitebackid));

			if( node_classGroup != null )
			{
				Element site = map.addElement("site");
					
				String siteglobalid  =  XMLHelper.getTextValueOf(node_classGroup,"@gid");
		 	 	ErrorRaiser.throwIfNull( siteglobalid, IterErrorKeys.XYZ_ITR_E_EMPTY_GLOBAL_ID_ZYX);
		 	 	
		 	 		
		 	 	site.addAttribute("siteglobalid", siteglobalid);
		 		site.addAttribute("sitebackid", String.valueOf(sitebackid));
		 	 	node_classGroup.detach();
		 	 		
				List<Node> nodes_byGroup = globalIdsDom.selectNodes( String.format("/map/id[@groupid= '%s']", String.valueOf(sitebackid)  ));
				site.setContent(nodes_byGroup);
			}	
	 	}
	 	
	 	Document domLiveConfig = JSONUtil.getLiveConfiguration();
	 	String remoteIterServer = JSONUtil.getLiveConfRemoteIterServer(domLiveConfig);
	 	int conexionTimeout = JSONUtil.getLiveConfConexionTimeout(domLiveConfig);
	 	int responseTimeout = JSONUtil.getLiveResponseTimeout(domLiveConfig);
	 	
	 	String []url 		= remoteIterServer.split(":");
		HttpHost targetHost = new HttpHost(url[0], Integer.valueOf(url[1]));
		
	 	URL liveServiceURL 			 = new URL( targetHost.toString() +"/base-portlet/live/endpoint");
        URLConnection connection 		 = liveServiceURL.openConnection();
        HttpURLConnection httpConnection = (HttpURLConnection)connection;
        httpConnection.setRequestMethod("POST"); 
        httpConnection.setConnectTimeout( conexionTimeout );
        httpConnection.setReadTimeout( responseTimeout );
	    httpConnection.setRequestProperty("Content-Type", "text/xml"); 
	    httpConnection.setDoOutput(true);
	    httpConnection.setDoInput(true);
	    
        String xml = String.format("<http-rpc><invoke clsid='com.protecmedia.iter.xmlio.service.LiveServiceUtil' dispid='1' lcid='0' flags='1' sessen='0' methodName='idGlobalToIdLocal'><params><param index='0' vt='8'><![CDATA[%s]]></param></params></invoke></http-rpc>", globalIdsBySiteIdDom.getRootElement().asXML());
        byte[] postData = xml.getBytes(StringPool.UTF8);

        DataOutputStream out = new DataOutputStream(httpConnection.getOutputStream());
        out.write(postData);
        out.flush();
        out.close();

		if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK)
		{
			Document localLiveIdsReturnDom = SAXReaderUtil.read( httpConnection.getInputStream() );
			
			Node localLiveIdsReturnNode = localLiveIdsReturnDom.selectSingleNode("//param[@index='-1']");
			String localLiveIdsReturnData = localLiveIdsReturnNode.getStringValue();
			
			if( _log.isDebugEnabled() )
					_log.debug("IDGLOBALTOIDLOCAL_RESULT: "+ localLiveIdsReturnData);
			
			Document localLiveIdsDom  = SAXReaderUtil.read(localLiveIdsReturnData);
			
			List<Node> idGlobalToIdLocalReturn =  localLiveIdsDom.selectNodes("/map/site");
		 	for(int m= 0; m < idGlobalToIdLocalReturn.size(); m++)
		 	{
		 		//se busca en globalIdsBySiteIdDom el 'sitebackid' que corresponde al nodo, y se añade como atributo
		 		Node site_live = idGlobalToIdLocalReturn.get(m);
		 		String siteLive_gid= XMLHelper.getStringValueOf(site_live, "@siteglobalid");
		 		Node node_siteBack 	 =  globalIdsBySiteIdDom.selectSingleNode( String.format("/map/site[@siteglobalid= %s]", StringUtil.escapeXpathQuotes(siteLive_gid)  ) );
		 		String node_siteBack_id =  XMLHelper.getStringValueOf(node_siteBack, "@sitebackid");
		 		
		 		((Element)site_live).addAttribute("sitebackid", node_siteBack_id);
		 		
		 		
		 		//se recorre liveids para obtener class y gid de cada nodo y buscar el backid correspondiente en globalIdsDom
		 		List<Node> liveids =  site_live.selectNodes("./id");
	 			
	 			for(int j= 0; j  < liveids.size(); j++)
	 			{
	 				Node id =  liveids.get(j);
	 				String gid = XMLHelper.getStringValueOf( id, "@gid"); 
	 				String typeClass = XMLHelper.getStringValueOf( id, "@class"); 
	 				
	 				//se obtiene el backid correspondiente de globalIdsDom
	 				String backid =  XMLHelper.getStringValueOf(globalIdsDom, String.format( "//id[@gid ='%s' and @class='%s']/@backid", gid, typeClass ));
	 				if( typeClass.equals("pagetemplate") || typeClass.equals("dlfileentry"))
	 				{
	 					/*backid obtenido es el correspondiente a xmlio_live.localId del preview.
	 					  para el class pagetemplate o dlfileentry, el backid a retornar es el del xml de entrada(se obtiene de dom ) */
	 					backid =  XMLHelper.getStringValueOf(dom,  String.format( "//id[@id ='%s' and @class='%s']/@backid", backid, typeClass ));    
	 				}
	 				
	 				((Element)liveids.get(j)).addAttribute("backid", backid);
	 			}	
		 	}
		 					
		 	localLiveIds = transformLiveIds(localLiveIdsDom);
		}
	 		
 		return localLiveIds;	
	}

	
	private String buildClassNameCondition(String classNameValue, String ids)
	{
		return Validator.isNotNull(ids) ? String.format(" OR (localid IN ('%s') AND classNameValue='%s') \n", ids, classNameValue) : ""; 
	}
	
	private StringBuilder getConditionToAppendSql( long sitebackid, Node siteNode) throws ServiceError 
	{
		StringBuilder sqlClassNames = new StringBuilder()
			.append("(groupId= %1$s AND  ( 	\n")	
			.append( "(localid='%1$s' AND classNameValue= '"	   + IterKeys.CLASSNAME_GROUP +"' ) 		\n"			).append( 
			buildClassNameCondition(IterKeys.CLASSNAME_PAGETEMPLATE, getIdsByClass(siteNode,"pagetemplate", "@id" ))	).append(
			buildClassNameCondition(IterKeys.CLASSNAME_PAGECONTENT,  getIdsByClass(siteNode,"pagecontent", 	"@backid" ))		).append(
			buildClassNameCondition(IterKeys.CLASSNAME_DLFILEENTRY,  getIdsByClass(siteNode, "dlfileentry", "@id" ))).append(
			buildClassNameCondition(IterKeys.CLASSNAME_CATEGORY,  	 getIdsByClass(siteNode,"category", 	"@backid" ))		).append(
			buildClassNameCondition(IterKeys.CLASSNAME_VOCABULARY,   getIdsByClass(siteNode,"vocabulary", 	"@backid" ))		).append(
			buildClassNameCondition(IterKeys.CLASSNAME_LAYOUT,  	 getIdsByClass(siteNode,"section", 		"@backid" ))		).append(
			buildClassNameCondition(IterKeys.CLASSNAME_PRODUCT,      getIdsByClass(siteNode,"suscription", 	"@backid" )) ).append(") )");
		
		String sql_appendStr = String.format(sqlClassNames.toString(), sitebackid);
		
		return new StringBuilder(sql_appendStr);
	}
	
	public String transformLiveIds( Document localids) throws Exception
	{
		String stringResult = "";
		String xslpath = new StringBuilder("").append(File.separatorChar).append("xmlio-portlet")
							.append(File.separatorChar).append("xsl")
							.append(File.separatorChar).append("localidToLiveid.xsl").toString();

		stringResult = XSLUtil.transformXML(localids.asXML(), xslpath);
		return stringResult;
	}
	
	private String getIdsByClass( Node siteNode, String classType, String nameParameterId ) throws ServiceError 
	{
		 String idsByclass = StringPool.BLANK;
		 
		 List<Node> nodesListByClass = siteNode.selectNodes( String.format("./id[@class='%s']", classType ) );
		 idsByclass = StringUtil.merge( XMLHelper.getStringValues(nodesListByClass, nameParameterId), "','" );
		
		 return idsByclass;
	}
	
	private void deleteArticleScheduledPublications(Document remoteLogDom)
	{
		// Recupera las programaciones que se han publicado en el Live
		List<Node> scheduledPublications = remoteLogDom.selectNodes("/iter/scheduledPublications/publications");
		for (Node articlePublications : scheduledPublications)
		{
			try
			{
				// Obtiene el articleId al que corresponden las programaciones
				String articleId = ((Element) articlePublications).attributeValue("articleId");
				// Elimina las programaciones que no han sido modificadas durante la publicación.
				CommunityPublisherLocalServiceUtil.deleteSchedulePublications(articleId, articlePublications.asXML());
			}
			catch (Throwable t)
			{
				_log.error(t);
			}
		}
	}
}
