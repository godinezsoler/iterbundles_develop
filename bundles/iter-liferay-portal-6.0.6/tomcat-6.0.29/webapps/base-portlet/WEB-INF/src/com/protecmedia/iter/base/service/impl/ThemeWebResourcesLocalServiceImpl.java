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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.util.Base64;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.cache.MultiVMPoolUtil;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.LayoutSetTools;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.SectionUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.MinifyUtil;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.base.ThemeWebResourcesLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.WebResourceUtil;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;

/**
 * The implementation of the theme web resources local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.ThemeWebResourcesLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.ThemeWebResourcesLocalServiceUtil} to access the theme web resources local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.ThemeWebResourcesLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.ThemeWebResourcesLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class ThemeWebResourcesLocalServiceImpl extends ThemeWebResourcesLocalServiceBaseImpl
{
	private static Log _log 	= LogFactoryUtil.getLog(ThemeWebResourcesLocalServiceImpl.class);
	
	/** En modo <i>Debug</i> deshabilita los bloqueos **/
	private static Log _logLock = LogFactoryUtil.getLog(ThemeWebResourcesLocalServiceImpl.class.getName() + ".Lock");
	
	private final static String CLASS_LAYOUT                        = LayoutConstants.CLASS_LAYOUT;	
	private final static String CLASS_MODEL                         = LayoutConstants.CLASS_MODEL;
	private final static String CLASS_DEFAULT						= "default";
	
	private static final String WEBRSRC_PATH 						= "/base-portlet/webrsrc/theme/";
	
	private static final String CONTENT_SPEC_EMBEDED 				= "embeded";
	private static final String CONTENT_SPEC_OWNED					= "owned";
	private static final String CONTENT_SPEC_THIRDPTY				= "thirdpty";
	
	private static final String CSS_BASIC_SOURCE 					= "<style type='%s'>%s</style>";
	private static final String CSS_BASIC_URL 						= "<link href='%s' rel='stylesheet' type='%s'>";
	
	private static final String JAVASCRIPT_BASIC_SOURCE 			= "<script type='%s'>%s</script>";
	private static final String JAVASCRIPT_BASIC_URL 				= "<script type='%s' src='%s'></script>";

	private static final String JAVASCRIPT_BASIC_ASYNC				= "jQryIter.ajax({url:'%s',dataType:'script',cache:true,%s});";
	
	private static final String JAVASCRIPT_BASIC_CALLBACK			= "success:function(){%s}";

	// private static final String XSL_GET_PUBLISHABLE_INFO			= String.format("%1$sbase-portlet%1$sxsl%1$sThemeWebResource_getPublishableInfo.xsl", File.separatorChar);


	private final static String GET_WEBRESOURCE_BY_PLID_PLACE_TYPE 	= new StringBuilder(	
	"SELECT	t.rsrcmd5, t.rsrccontent, t.rsrccontenttype,    			    															\n").append( 
	"		t.rsrccontentspec, t.callback, t.async																						\n").append( 
	"FROM (																																\n").append(
	"    SELECT FIELD(IFNULL(l.plid,0), %1$s) sortField, IFNULL(l.plid,0) plid															\n").append(
	"    FROM layout_webresource l																										\n").append(
	"     WHERE IFNULL(l.plid,0) IN (%1$s)																								\n").append(
	"     ORDER BY sortField ASC																										\n").append(
	"    LIMIT 1																														\n").append(
	"   ) LayoutTmp																														\n").append(
	"INNER JOIN layout_webresource l ON (IFNULL(l.plid,0) = LayoutTmp.plid)																\n").append(
	"INNER JOIN theme_webresource t ON t.rsrcid = l.rsrcid 																				\n").append(
	"WHERE 																																\n").append(
	"  l.place='%2$s'																													\n").append(
	" AND rsrccontenttype IN ('%3$s') %4$s																								\n").append(
	"ORDER BY l.rsrcorder ASC																											\n").toString();
	
	private final static String GET_EXISTING_RESOURCES = new StringBuilder(
		" -- Se coloca un DISTINCT para las migraciones que tengan más de un md5 para la misma delegación 	\n").append(
		" -- Al pasar de md5 por grupo a md5 por delegación pueden haber repeticiones. 						\n").append(
		" -- Si el resultado se utiliza para hacer detach de nodos sería un problema.						\n").append(
		"SELECT DISTINCT rsrcmd5																			\n").append(
		"FROM theme_webresource																				\n").append(
		"  WHERE rsrcmd5 IN ('%s')																			\n").append(
		"    AND delegationId = %d																			\n").toString();	

	private final static String EXCLUDE_NON_PRELOADABLES			= String.format(new StringBuilder(
		" -- Los recursos incrustados NO pueden ser precargados							\n").append(
		" AND t.rsrccontentspec <> '%s' 												\n").append(
		" -- Los JS son los únicos que se cargan asíncronamente, y los JS que se cargan \n").append(
		" -- así tampoco deben precargarse porque NO bloquean el pintado de la página	\n").append(		
		" AND (t.rsrccontenttype <> '%s' OR t.async = 0)								\n").toString(), CONTENT_SPEC_EMBEDED, ContentTypes.TEXT_JAVASCRIPT);
	
	private final static String INSERT_THEME_WEBRESOURCES 			= new StringBuilder(	
			"INSERT INTO theme_webresource (rsrcid, rsrcmd5, rsrccontent, rsrccontenttype, 	\n").append(
			" 								rsrccontentspec, async, callback, delegationid) \n").append(
			"		VALUES %s"					    											   		   ).toString();
	
	private final static String VALUES_THEME_WEBRESOURCES 			= " (ITR_UUID(), %s, '%s', '%s', '%s', %s, %s, %d)";

	
	private final static String INSERT_LAYOUT_WEBRESOURCES 			= new StringBuilder(	
			"INSERT INTO layout_webresource (groupid, plid, rsrcid, place, rsrcorder, moddate, themeid)	\n").append(
			"		VALUES%s"																	  		   ).toString();

	private final static String VALUES_LAYOUT_PLID					= "IF ('%2$s' = 'model', (SELECT layoutId FROM Designer_PageTemplate WHERE id_=%3$s), %3$s)"; 
	
	private final static String VALUES_LAYOUT_PLID_BY_GLOBAL		= new StringBuilder( 
			"(SELECT XmlIO_Live.localId 																	\n").append(
			"FROM XmlIO_Live 																				\n").append(
			"	WHERE classNameValue='").append(IterKeys.CLASSNAME_LAYOUT).append("'						\n").append(
			"		AND XmlIO_Live.groupId=%1$d																\n").append(
			"		AND XmlIO_Live.globalId='%3$s')															\n").toString();
	
	private final static String DELETE_DEFAULT_LAYOUT_WEBRESOURCES  = new StringBuilder(
		"DELETE FROM layout_webresource 		\n").append(
		"	WHERE 	groupId=%d AND plid IS NULL \n").toString();
	
	private final static String DELETE_GLOBAL_LAYOUT_WEBRESOURCES 	= new StringBuilder(
		"DELETE l.*																						\n").append(
		"FROM layout_webresource l																		\n").append(
		"INNER JOIN XmlIO_Live x ON (    l.plid = x.localId 											\n").append(
		"                            AND classNameValue='").append(IterKeys.CLASSNAME_LAYOUT).append("'	\n").append(
		"                            AND l.groupId = x.groupId											\n").append(
		"                            )																	\n").append(
		"  WHERE l.groupId = %d																			\n").append(
		"    AND x.globalId IN ('%s')																	\n").toString();
		

	private final static String DELETE_SECTIONL_LAYOUT_WEBRESOURCES = new StringBuilder(
		"DELETE FROM layout_webresource 									\n").append(
		"	WHERE groupId=%d 												\n").append(
		" 	  AND plid IN (%s)												\n").toString();
	
	private final static String DELETE_MODEL_LAYOUT_WEBRESOURCES 	= new StringBuilder(
		"DELETE l.*															\n").append(
		"FROM layout_webresource l											\n").append(
		"INNER JOIN Designer_PageTemplate d ON (	l.plid = d.layoutId 	\n").append(
		"										AND l.groupId = d.groupId	\n").append(
		"										)							\n").append(
		"  WHERE l.groupId = %d												\n").append(
		"	 AND d.id_ IN (%s)												\n").toString();
	
	/**
	 * Es importante el LIMIT 1 de los recursos porque tras la migración y 
	 * pasar de md5 por grupo a md5 por delegación pueden haber repeticiones
	 */
	private final static String _VALUES_LAYOUT_WEBRESOURCES 			= new StringBuilder(	
			" (%%1$d, 																\n").append(
			"	%s,																	\n").append(
			"   (	SELECT rsrcid 													\n").append(
			"		FROM theme_webresource											\n").append(
			"		 WHERE rsrcmd5 = '%%4$s' AND theme_webresource.delegationid = %%9$d	LIMIT 1\n").append(
			"	), '%%5$s', %%6$s, '%%7$s', %%8$s)									  ").toString();
	
	private final static String VALUES_LAYOUT_WEBRESOURCES_BY_GLOBAL = String.format(_VALUES_LAYOUT_WEBRESOURCES, VALUES_LAYOUT_PLID_BY_GLOBAL);
	private final static String VALUES_LAYOUT_WEBRESOURCES = String.format(_VALUES_LAYOUT_WEBRESOURCES, VALUES_LAYOUT_PLID);
	
	private final static String DEL_UNUSED_ORPHAN_RSRC = new StringBuilder(
		"DELETE FROM theme_webresource														\n").append(
		"WHERE orphandate IS NOT NULL														\n").append(
		"	AND delegationid = %d															\n").append(		
		"	AND DATEDIFF(NOW(), orphandate) > %d											\n").toString();
//		"DELETE FROM theme_webresource														\n").append(
//		"WHERE orphandate IS NOT NULL														\n").append(
//		"	AND groupid = %d																\n").append(		
//		"	AND DATEDIFF(NOW(), orphandate) > %d											\n").toString();

	/**
	 * Se localizan los recursos NO huérfanos que SÍ están marcados como tal
	 */
	private final static String UPDT_REUSED_ORPHAN_RSRC = new StringBuilder(	
		"UPDATE theme_webresource SET orphandate = NULL										\n").append(
		"  WHERE delegationid = %d															\n").append(
		"    AND 0 < ( SELECT COUNT(1)														\n").append(
		"              FROM layout_webresource												\n").append(
		"                WHERE layout_webresource.rsrcid = theme_webresource.rsrcid			\n").append(
		"            )																		\n").append(
		"    AND orphandate IS NOT NULL       												\n").toString();
//			"UPDATE theme_webresource SET orphandate = NULL									\n").append( 
//			"WHERE theme_webresource.groupid = %d											\n").append(
//			"	AND theme_webresource.orphandate IS NOT NULL								\n").append(
//			"	AND theme_webresource.rsrcmd5 IN	 										\n").append(
//			"		(																		\n").append(
//			"			%s																	\n").append(
//			"		) 																		  ").toString();
	
	/**
	 * Se localizan los recursos huérfanos que NO están marcados como tal  
	 */
	private final static String UPDT_ORPHAN_RSRC = new StringBuilder(	
		"UPDATE theme_webresource SET orphandate = '%s'										\n").append( 	
		"  WHERE delegationid = %d															\n").append( 
		"    AND 0 = ( SELECT COUNT(1)														\n").append( 
		"              FROM layout_webresource												\n").append( 
		"                WHERE layout_webresource.rsrcid = theme_webresource.rsrcid			\n").append( 
		"            )																		\n").append( 
		"    AND orphandate IS NULL            												\n").toString();
//			"UPDATE theme_webresource SET orphandate = '%s'									\n").append( 
//			"WHERE theme_webresource.groupid = %d											\n").append(
//			"	AND theme_webresource.orphandate IS NULL									\n").append(
//			"	AND theme_webresource.rsrcmd5 NOT IN 										\n").append(
//			"		(																		\n").append(
//			"			%s																	\n").append(
//			"		) 																		  ").toString();

	private final static String UPDT_PREDELIVER_ORPHAN_RSRC = new StringBuilder(
			"UPDATE theme_webresource SET orphandate = '%s'									\n").append( 
			"  WHERE orphandate IS NOT NULL													\n").append( 
			"    AND rsrcmd5 IN ('%s')														\n").append( 
			"	 AND delegationId = %d														\n").toString();
	
	private final static String DELETE_GROUP_LAYOUT_WEBRESOURCE		= "DELETE FROM layout_webresource WHERE groupid = %d";
	private final static String DELETE_THEME_LAYOUT_WEBRESOURCE		= "DELETE FROM layout_webresource WHERE groupid = %d AND themeid = %s";
	
	
	private static final String VALUES_SEPARATOR = ",\n\t";
	
	private static final String SEL_MD5_BY_DELEGATION = new StringBuilder(
			"SELECT DISTINCT rsrcmd5	\n").append(
			"FROM theme_webresource		\n").append(
			"	WHERE delegationid = %d	\n").toString();	

	private static final String SEL_PUBLISHABLE_RESOURCES = new StringBuilder(
	    " -- Recursos utilizados por el grupo - tema 																				\n").append(
	    " -- Si el recurso está referenciado no será huérfano, así que no es necesario comprobar el orphandate 						\n").append(
		"SELECT DISTINCT rsrcmd5 md5																								\n").append(
	    "FROM theme_webresource 																									\n").append(
	    "INNER JOIN layout_webresource USING (rsrcid) 																				\n").append(
	    "  WHERE layout_webresource.groupId = %1$d																					\n").append(
		"	%2$s  																													\n").toString();		

	private static final String SEL_PUBLISHABLE_RESOURCES_INFO = new StringBuilder(
	    " -- Recursos utilizados por el grupo - tema 																				\n").append(
	    " -- Si el recurso está referenciado no será huérfano, así que no es necesario comprobar el orphandate 						\n").append(
		"SELECT DISTINCT rsrcmd5 md5, rsrccontenttype type , rsrccontentspec kind, async, rsrccontent content, callback jscallback	\n").append(
	    "FROM theme_webresource 																									\n").append(
	    "INNER JOIN layout_webresource USING (rsrcid) 																				\n").append(
	    "  WHERE layout_webresource.groupId = %1$d																					\n").append(
	    "   AND theme_webresource.rsrcmd5 IN ('%3$s')																				\n").append(		
		"	%2$s  																													\n").toString();			

	private static final String SEL_PUBLISHABLE_LAYOUTS_INFO = new StringBuilder(
		"SELECT * FROM ( 																											\n").append(
		" -- Asignaciones a Layouts (layout_webresource.plid IS NOT NULL) de Layouts que ya estén publicados (existInLive = 'S')	\n").append(				
		"SELECT themeid, layout_webresource.plid, XmlIO_Live.globalId id, rsrcmd5 rsrc, place, rsrcorder, 'layout' class			\n").append(
		"FROM layout_webresource																									\n").append(
		"INNER JOIN theme_webresource ON layout_webresource.rsrcid = theme_webresource.rsrcid										\n").append(
		"INNER JOIN XmlIO_Live ON 																									\n").append(
		"		(																													\n").append(
		"				classNameValue='").append(IterKeys.CLASSNAME_LAYOUT).append("'												\n").append(
		"			AND XmlIO_Live.groupId 		= layout_webresource.groupid														\n").append(
		"			AND XmlIO_Live.localId 		= layout_webresource.plid															\n").append(
		"			AND XmlIO_Live.existInLive 	= 'S'																				\n").append(				
		"		)																													\n").append(
		"	WHERE layout_webresource.groupId = %1$d																		  			\n").append(
		"	%2$s  																													\n").append(				
		"																															\n").append(
		" -- Asignaciones al tema por defecto (layout_webresource.plid IS NULL)														\n").append(				
		"UNION ALL																													\n").append(
		"SELECT themeid, layout_webresource.plid, NULL id, rsrcmd5 rsrc, place, rsrcorder, 'default' class							\n").append(
		"FROM layout_webresource																									\n").append(
		"INNER JOIN theme_webresource ON layout_webresource.rsrcid = theme_webresource.rsrcid										\n").append(
		"	WHERE layout_webresource.groupId = %1$d	AND layout_webresource.plid IS NULL									  			\n").append(
		"	%2$s  																													\n").append(				
		"																															\n").append(				
		") tmpLayout																												\n").append(					
		"ORDER BY plid, rsrcorder 																									\n").toString();

//	private static final String SEL_PUBLISHABLE_INFO = new StringBuilder(
//		"SELECT * FROM ( 																											\n").append(
//		"																															\n").append(
//	    " -- Recursos utilizados por el grupo - tema 																				\n").append(
//	    " -- Si el recurso está referenciado no será huérfano, así que no es necesario comprobar el orphandate 						\n").append(
//		"SELECT  rsrcmd5 md5, rsrccontenttype type , rsrccontentspec kind, async, rsrccontent content, callback jscallback, 		\n").append(
//		"		 NULL themeid, NULL plid, NULL id, NULL rsrc, NULL place, NULL rsrcorder			 								\n").append(
//	    "FROM theme_webresource 																									\n").append(
//	    "INNER JOIN layout_webresource USING (rsrcid) 																				\n").append(
//	    "  WHERE layout_webresource.groupId = %1$d																					\n").append(
//		"	%2$s  																													\n").append(		
//		"																															\n").append(		
//		" -- Asignaciones a Layouts (layout_webresource.plid IS NOT NULL) de Layouts que ya estén publicados (existInLive = 'S')	\n").append(				
//		"UNION ALL																													\n").append(
//		"SELECT NULL md5, NULL type, NULL kind, NULL async, NULL content, NULL jscallback,											\n").append(
//		"		themeid, layout_webresource.plid, XmlIO_Live.globalId id, rsrcmd5 rsrc, place, rsrcorder 							\n").append(
//		"FROM layout_webresource																									\n").append(
//		"INNER JOIN theme_webresource ON layout_webresource.rsrcid = theme_webresource.rsrcid										\n").append(
//		"INNER JOIN XmlIO_Live ON 																									\n").append(
//		"		(																													\n").append(
//		"				classNameValue='").append(IterKeys.CLASSNAME_LAYOUT).append("'												\n").append(
//		"			AND XmlIO_Live.groupId 		= layout_webresource.groupid														\n").append(
//		"			AND XmlIO_Live.localId 		= layout_webresource.plid															\n").append(
//		"			AND XmlIO_Live.existInLive 	= 'S'																				\n").append(				
//		"		)																													\n").append(
//		"	WHERE layout_webresource.groupId = %1$d																		  			\n").append(
//		"	%2$s  																													\n").append(				
//		"																															\n").append(
//		" -- Asignaciones al tema por defecto (layout_webresource.plid IS NULL)														\n").append(				
//		"UNION ALL																													\n").append(
//		"SELECT NULL md5, NULL type, NULL kind, NULL async, NULL content, NULL jscallback,											\n").append(
//		"		themeid, layout_webresource.plid, NULL id, rsrcmd5 rsrc, place, rsrcorder 											\n").append(
//		"FROM layout_webresource																									\n").append(
//		"INNER JOIN theme_webresource ON layout_webresource.rsrcid = theme_webresource.rsrcid										\n").append(
//		"	WHERE layout_webresource.groupId = %1$d	AND layout_webresource.plid IS NULL									  			\n").append(
//		"	%2$s  																													\n").append(				
//		"																															\n").append(				
//		") tmpLayout																												\n").append(					
//		"ORDER BY plid, rsrcorder 																									\n").toString();
	
//		"SELECT * FROM ( \n").append(
//		"SELECT themeid, rsrcmd5 md5, rsrccontenttype type , rsrccontentspec kind, async, rsrccontent content, callback jscallback, \n").append(
//		"		NULL plid, NULL id, NULL rsrc, NULL place, NULL rsrcorder															\n").append(
//		"FROM theme_webresource  																									\n").append(
//		"	WHERE groupId = %1$d AND theme_webresource.orphandate IS NULL															\n").append(
//		"																															\n").append(
//		" -- Asignaciones a Layouts (layout_webresource.plid IS NOT NULL) de Layouts que ya estén publicados (existInLive = 'S')	\n").append(				
//		"UNION ALL																													\n").append(
//		"SELECT NULL themeid, NULL md5, NULL type, NULL kind, NULL async, NULL content, NULL jscallback,							\n").append(
//		"		layout_webresource.plid, XmlIO_Live.globalId id, rsrcmd5 rsrc, place, rsrcorder 									\n").append(
//		"FROM layout_webresource																									\n").append(
//		"INNER JOIN theme_webresource ON layout_webresource.rsrcid = theme_webresource.rsrcid										\n").append(
//		"INNER JOIN XmlIO_Live ON 																									\n").append(
//		"		(																													\n").append(
//		"				classNameValue='").append(IterKeys.CLASSNAME_LAYOUT).append("'												\n").append(
//		"			AND XmlIO_Live.groupId 		= layout_webresource.groupid														\n").append(
//		"			AND XmlIO_Live.localId 		= layout_webresource.plid															\n").append(
//		"			AND XmlIO_Live.existInLive 	= 'S'																				\n").append(				
//		"		)																													\n").append(
//		"	WHERE layout_webresource.groupId = %1$d																		  			\n").append(
//		"																															\n").append(
//		" -- Asignaciones al tema por defecto (layout_webresource.plid IS NULL)														\n").append(				
//		"UNION ALL																													\n").append(
//		"SELECT NULL themeid, NULL md5, NULL type, NULL kind, NULL async, NULL content, NULL jscallback,							\n").append(
//		"		layout_webresource.plid, NULL id, rsrcmd5 rsrc, place, rsrcorder 													\n").append(
//		"FROM layout_webresource																									\n").append(
//		"INNER JOIN theme_webresource ON layout_webresource.rsrcid = theme_webresource.rsrcid										\n").append(
//		"	WHERE layout_webresource.groupId = %1$d	AND layout_webresource.plid IS NULL									  			\n").append(
//		"																															\n").append(				
//		") tmpLayout																												\n").append(					
//		"ORDER BY plid, rsrcorder 																									  ").toString();
	
	
	private static ConcurrentMap<Long, Boolean> _process 	= new ConcurrentHashMap<Long, Boolean>();
	
	public Document preDeliverTheme(String themeSpec) throws ServiceError, DocumentException, IOException, SecurityException, NoSuchMethodException, SQLException
	{
		_log.trace("preDeliverTheme begins");
		
		ErrorRaiser.throwIfNull(themeSpec, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		_log.debug(themeSpec);

		Document dom = SAXReaderUtil.read(themeSpec);
		
		if (_log.isTraceEnabled())
			writeFile("preDeliverTheme.xml", dom.asXML());
		
		// Se obtiene la delegación
		long delegationId = XMLHelper.getLongValueOf(dom, "/rsrc-list/@delegationid");
		
		// Se obtiene el listado de md5 a comprobar
		String md5Values = StringUtils.join(XMLHelper.getStringValues( dom.selectNodes("/rsrc-list/rsrc"), "@md5" ), "','");
		
		String sql = String.format(GET_EXISTING_RESOURCES, md5Values, delegationId);
		_log.debug(sql);
		Document domExist = PortalLocalServiceUtil.executeQueryAsDom( sql );
		if (_log.isDebugEnabled())
			_log.debug(domExist.asXML());
		
		List<String> listMD5 = new ArrayList<String>();
		List<Node> existingRsrcs = domExist.selectNodes("/rs/row/@rsrcmd5");
		for (Node rsrc : existingRsrcs)
		{
			String md5 = XMLHelper.getStringValueOf(rsrc, ".");
			listMD5.add(md5);
			
			// Se elimina este nodo del XML final
			dom.selectSingleNode( String.format("/rsrc-list/rsrc[@md5='%s']", md5) ).detach();
		}
		
		// Se obtiene la lista de los que quedan, para actualizar la fecha de orfandad en el caso de que sean huéfanos
		md5Values = StringUtils.join(listMD5, "','");
		
		// Se actualiza la fecha de orfandad
		sql = String.format(UPDT_PREDELIVER_ORPHAN_RSRC, SQLQueries.getCurrentDate(), md5Values, delegationId);
		_log.debug(sql);
		PortalLocalServiceUtil.executeUpdateQuery(sql);
		
		if (_log.isDebugEnabled())
			_log.debug(dom.asXML());

		_log.trace("preDeliverTheme ends");
		
		return dom;
	}
		
	public void deliverTheme(String themeSpec) throws Exception
	{
		ErrorRaiser.throwIfNull(themeSpec, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		_log.debug(themeSpec);

		deliverTheme(SAXReaderUtil.read(themeSpec));
	}
	
	/**
	 * 
	 * @param fileName
	 * @param fileData
	 * @throws IOException 
	 */
	private void writeFile(String fileName, String fileData) throws IOException
	{
        String pathRoot = new File(PortalUtil.getPortalWebDir()).getParentFile().getAbsolutePath();
        String path		= new StringBuilder(pathRoot).append(File.separatorChar)
								  .append("base-portlet").append(File.separatorChar)
								  .append("xsl").append(File.separatorChar)
								  .append(fileName).toString();
        
        FileWriter file = new FileWriter(path);
        file.write(fileData);
        file.close();
	}
	
	public void deliverTheme(Document dom) throws Exception
	{
		_log.trace("deliverTheme begins");

		Element root 	 = dom.getRootElement();
		String groupName = XMLHelper.getTextValueOf(root, "@groupname");
		
		// Se intenta identificar el grupo por nombre (para el caso de las publicaciones) y como segunda opción por id
		long groupId = (Validator.isNotNull(groupName)) 													? 
						GroupLocalServiceUtil.getGroup(IterGlobal.getCompanyId(), groupName).getGroupId()	:
						XMLHelper.getLongValueOf(root, "@siteid");
						
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Los temas de un GRUPO (site) no se podrán APLICAR ni PUBLICAR.
		lockProcess(groupId);
		
		try
		{
			if (_log.isTraceEnabled())
				writeFile("deliverTheme.xml", dom.asXML());
			
			// A partir del grupo se obtiene la delegación
			long delegationId = GroupLocalServiceUtil.getGroup(groupId).getDelegationId();
			
			String themeId = StringUtil.apostrophe( XMLHelper.getStringValueOf(root, "@themeid") );
			
			boolean minify = false;
			if ( !PropsValues.ITER_THEME_RSRC_MINIFY_ON_THE_FLY )
			{
				// Modo nuevo: Minifica si no existe el atributo o si es falso
				minify = GetterUtil.getBoolean(XMLHelper.getStringValueOf(root, "@minify"), true);
			}

			addThemeWebresources (delegationId, root.selectSingleNode("rsrc-list"), minify);
			addLayoutWebresources(groupId, delegationId, themeId, root.selectSingleNode("layout-rsrc"));
			
			// A los recursos que se reutilizan se les quita la marca de huérfanos ya que vuelven a formar parte, del tema o de los temas del grupo.
			// Se buscan las asignaciones realizadas dentro de la delegación, y a aquellos recursos que figuren como huérfamos se les quita la fecha.
			String sql = String.format(UPDT_REUSED_ORPHAN_RSRC, delegationId);
			_log.debug(sql);
			PortalLocalServiceUtil.executeUpdateQuery(sql);
			
			// Se borran aquellos recursos que sean huérfanos y no hayan sido accedidos desde hace más de orphanDays días
			sql = String.format(DEL_UNUSED_ORPHAN_RSRC, delegationId, PropsValues.ITER_THEME_RSRC_ORPHAN_DAYS);
			_log.debug(sql);
			PortalLocalServiceUtil.executeUpdateQuery(sql);
			
			// Se actualiza la fecha de orfandad
			sql = String.format(UPDT_ORPHAN_RSRC, SQLQueries.getCurrentDate(), delegationId);
			_log.debug(sql);
			PortalLocalServiceUtil.executeUpdateQuery(sql);
			
			if (PropsValues.ITER_ENVIRONMENT.equals(WebKeys.ENVIRONMENT_PREVIEW))
				MultiVMPoolUtil.clear();
				
			_log.trace("deliverTheme ends");
		}
		finally
		{
			unlockProcess(groupId);
		}
	}
	
	/**
	 * Se comprueba que no se pase un recurso vacío
	 * @see ITER-675 Error al publicar diseño web - Argumentos inválidos
	 * @param rsrcContent
	 * @param groupId
	 * @param rsrcmd5
	 * @param rsrcContentType
	 * @throws PortalException
	 * @throws SystemException
	 * @throws ServiceError 
	 */
	private void checkEmptyContent(String rsrcContent, long delegation, String rsrcmd5, String rsrcContentType) throws PortalException, SystemException, ServiceError
	{
		if (Validator.isNull(rsrcContent))
		{
			String trace = String.format("Delegation=%d - ContentType=%s - md5=%s", delegation, rsrcContentType, rsrcmd5);
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_SITE_THEMES_EMPTY_RESOURCE_ZYX, trace);
		}
	}
	
	/**
	 * <ol>
	 * 	<li>Se obtienen todos los recursos (theme_webresource) de la delegación.</li>
	 * 	<li>Se insertan los nuevos, aquellos que no estén en la lista obtenida en el paso anterior (todos los recursos de la delegación), 
	 * 		y sí estén en la lista recibida por parámetros del deliverTheme.
	 * 	</li>
	 * </ol>
	 * @param delegationId
	 * @param rsrcList
	 * @param minify
	 * @throws ServiceError
	 * @throws IOException
	 * @throws SQLException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws PortalException
	 * @throws SystemException
	 */
	private void addThemeWebresources(long delegationId, Node rsrcList, boolean minify) throws ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException, PortalException, SystemException
	{
		_log.trace("addThemeWebresources begins");
		
		// Al aplicar un tema o todos los temas del sitio podrían no llegar recursos porque ya estuviesen en la delegación
		if (rsrcList != null)
		{
			// Se obtienen todos los recursos (theme_webresource) de la delegación.
			Document currentTWRDom = PortalLocalServiceUtil.executeQueryAsDom( String.format(SEL_MD5_BY_DELEGATION, delegationId) );
			
			List<Node> nodes = rsrcList.selectNodes("rsrc");
			if(!nodes.isEmpty())
			{
				StringBuilder values = new StringBuilder();
				
				for(int i = 0; i < nodes.size(); i++)
				{
					Element rsrc = (Element)nodes.get(i);

					String rsrcmd5 = StringUtil.apostrophe( XMLHelper.getTextValueOf(rsrc, "@md5") );
					ErrorRaiser.throwIfNull(rsrcmd5, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
					
					// Si el recurso ya existe en BBDD se descarta
					if ( XMLHelper.getLongValueOf(currentTWRDom, String.format("count(/rs/row[@rsrcmd5=%s])", rsrcmd5)) > 0 )
						continue;
					
					String type = XMLHelper.getTextValueOf(rsrc, "@type");
					ErrorRaiser.throwIfNull(type, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
					if (type.indexOf("/") == -1)
						type = WebResourceUtil.getContentTypeByType(type);
					
					String rsrccontenttype 	= StringEscapeUtils.escapeSql(type);
					String rsrccontentspec 	= GetterUtil.getString((String)XMLHelper.getTextValueOf(rsrc, "@kind"), CONTENT_SPEC_OWNED);
					
					boolean async 			= GetterUtil.getBoolean(XMLHelper.getTextValueOf(rsrc, "@async"), true);
					String callback 		= StringUtil.apostrophe( XMLHelper.getTextValueOf(rsrc, "jscallback") );

					String rsrccontent 		= XMLHelper.getTextValueOf(rsrc, "content");
					checkEmptyContent(rsrccontent, delegationId, rsrcmd5, rsrccontenttype);
					
					if ( minify && (type.equals(ContentTypes.TEXT_CSS) || type.equals(ContentTypes.TEXT_JAVASCRIPT)) &&
						(rsrccontentspec.equals(CONTENT_SPEC_OWNED) || rsrccontentspec.equals(CONTENT_SPEC_EMBEDED)) )
					{
						rsrccontent = MinifyUtil.minifyContentOnDeliverTheme(rsrccontent, type);
						checkEmptyContent(rsrccontent, delegationId, rsrcmd5, rsrccontenttype);
					}
					
					values.append(VALUES_SEPARATOR).append( String.format(VALUES_THEME_WEBRESOURCES, 
							rsrcmd5, rsrccontent, rsrccontenttype, rsrccontentspec, async, callback, delegationId) );
				}
				
				// Se insertan los nuevos Theme_WebResources
				if (values.length() > 0)
				{
					values.delete(0, VALUES_SEPARATOR.length());
					PortalLocalServiceUtil.executeUpdateQuery( String.format(INSERT_THEME_WEBRESOURCES, values.toString()) );
				}
			}
		}
	}
		
	private void addLayoutWebresources(long groupId, long delegationId, String themeId, Node layoutRsrc) throws ServiceError, IOException, SQLException, NumberFormatException, SystemException, SecurityException, NoSuchMethodException
	{
		_log.trace("addLayoutWebresources begins");
		ErrorRaiser.throwIfNull(layoutRsrc, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Se eliminan las asignaciones (layout_webresource)
		String sql = null;					
		if (themeId == null)
		{
			// Del grupo si es la "Aplicación" de todo el sitio.
			sql = String.format(DELETE_GROUP_LAYOUT_WEBRESOURCE, groupId);
		}
		else
		{
			// Del tema PARA EL GRUPO EN CUESTIÓN si es la "Aplicación" de un tema específico.
			sql = String.format(DELETE_THEME_LAYOUT_WEBRESOURCE, groupId, themeId);
		}
		PortalLocalServiceUtil.executeUpdateQuery( sql );

		List<Node> nodes = layoutRsrc.selectNodes("itm");
		if (!nodes.isEmpty())
		{
			StringBuilder values= new StringBuilder();
			String currentDate 	= SQLQueries.getCurrentDate();
			
			boolean globalids = GetterUtil.getBoolean( XMLHelper.getTextValueOf(layoutRsrc, "@globalids"));
			String valuesQuery = (globalids) ? VALUES_LAYOUT_WEBRESOURCES_BY_GLOBAL : VALUES_LAYOUT_WEBRESOURCES;
			
			Set<String> layoutList 		= new HashSet<String>();
			Set<String> modelList 	 	= new HashSet<String>();
			Set<String> globalIdList 	= new HashSet<String>();
			Set<String> md5List 		= new HashSet<String>();
			boolean cleanDefault 		= false;
			
			
			for (int i = 0; i < nodes.size(); i++)
			{
				Node itm = nodes.get(i);
				
				// Si es una aplicación por grupo se obtiene el tema de cada layout.
				String localThemeId = (themeId == null) ? StringUtil.apostrophe( XMLHelper.getStringValueOf(itm, "@themeid") ) : themeId;
				if (Validator.isNull(localThemeId))
					localThemeId = "NULL";
								
				String classType = XMLHelper.getTextValueOf(itm, "@class");
				ErrorRaiser.throwIfFalse(	classType.equals(CLASS_LAYOUT) 	|| 
											classType.equals(CLASS_MODEL)	||
											classType.equals(CLASS_DEFAULT), 
					                     IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid atributte class for layout-rsrc item");
				
				String plid  = XMLHelper.getTextValueOf(itm, "@id");
				
				cleanDefault = cleanDefault || classType.equals(CLASS_DEFAULT);
				if (globalids)
				{
					 if (Validator.isNotNull(plid))
						 globalIdList.add(plid);
				}
				else if (classType.equals(CLASS_LAYOUT))
					layoutList.add(plid);
				else if (classType.equals(CLASS_MODEL))
					modelList.add(plid);
				
				String place = GetterUtil.getString((String)XMLHelper.getTextValueOf(itm, "@place"), WebResourceUtil.HEADER);
				
				String rsrc  = XMLHelper.getTextValueOf(itm, "@rsrc");
				ErrorRaiser.throwIfNull(rsrc, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
				
				md5List.add(rsrc);
			
				values.append(VALUES_SEPARATOR).append( String.format(valuesQuery, groupId, classType, plid, rsrc, place, i, currentDate, localThemeId, delegationId) );
			}

			// Necesario eliminar las asignaciones por defecto anteriores
			// Hablado/confirmado con Olaya el 24/04/2020. Solo un tema puede tener recursos por defecto en el grupo, así que si ha habido una asignación 
			// por defecto se DEBEN eliminar todas las asignaciones por defecto que existan para dicho grupo, antes de asignar las nuevas.
			if (cleanDefault)
			{
				PortalLocalServiceUtil.executeUpdateQuery( String.format(DELETE_DEFAULT_LAYOUT_WEBRESOURCES, groupId) );
			}
			
			if (!globalIdList.isEmpty())
			{
				// Necesario eliminar las secciones mediante GLOBALID
				PortalLocalServiceUtil.executeUpdateQuery( String.format(DELETE_GLOBAL_LAYOUT_WEBRESOURCES, groupId, StringUtils.join(globalIdList, "','")) );
				checkLayoutByGlobalIds(groupId, globalIdList);
			}
			else
			{
				if (!layoutList.isEmpty())
				{
					// Necesario eliminar las secciones 
					PortalLocalServiceUtil.executeUpdateQuery( String.format(DELETE_SECTIONL_LAYOUT_WEBRESOURCES, groupId, StringUtils.join(layoutList, ",")) );
					checkLayouts(layoutList);
				}
				if (!modelList.isEmpty())
				{
					// Necesario eliminar los modelos 
					PortalLocalServiceUtil.executeUpdateQuery( String.format(DELETE_MODEL_LAYOUT_WEBRESOURCES, groupId, StringUtils.join(modelList, ",")) );
					checkModels(modelList);
				}
			}
			
			checkResources(delegationId, md5List);
				
			if (values.length() > 0)
			{
				values.delete(0, VALUES_SEPARATOR.length());
				
				sql = String.format(INSERT_LAYOUT_WEBRESOURCES, values.toString());
				_log.debug(sql);
				
				PortalLocalServiceUtil.executeUpdateQuery( sql );
			}
		}
	}
	
	private static final String SELECT_LAYOUTS = "SELECT plid id FROM Layout WHERE plid IN (%s)";
	/**
	 * Comprueba que existan todas las secciones listadas. 
	 * En caso contrario lanza un error <code>XYZ_E_SITE_THEMES_LAYOUT_NOT_FOUND_ZYX</code> indicando las secciones NO encontradas
	 * 
	 * @param values
	 * @param globalids
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws ServiceError 
	 */
	private void checkLayouts(Set<String> values) throws SecurityException, NoSuchMethodException, ServiceError
	{
		if (_log.isDebugEnabled() && !values.isEmpty())
		{
			// Se eliminan los elementos que existen
			Document dom = PortalLocalServiceUtil.executeQueryAsDom( String.format(SELECT_LAYOUTS, StringUtils.join(values, ",")) );
			for (Node layout : dom.selectNodes("/rs/row"))
			{
				values.remove( ((Element)layout).attributeValue("id") );
			}
			
			ErrorRaiser.throwIfFalse(values.isEmpty(), IterErrorKeys.XYZ_E_SITE_THEMES_LAYOUT_NOT_FOUND_ZYX, StringUtils.join(values, ","));
		}
	}
	
	private static final String SELECT_GLOBALIDS = String.format(new StringBuilder(
		"SELECT globalid id																					\n").append(
		"FROM XMLIO_Live																					\n").append(
		"INNER JOIN Layout ON (		XMLIO_Live.localId = Layout.plid AND XMLIO_Live.classNameValue = '%s'	\n").append(
		"                      AND 	XMLIO_Live.groupId = Layout.groupId)									\n").append(
		"  WHERE XMLIO_Live.groupId = %%d																	\n").append(
		"    AND globalId IN ('%%s')																		\n").toString(), IterKeys.CLASSNAME_LAYOUT);
	private void checkLayoutByGlobalIds(long groupId, Set<String> values) throws ServiceError, SecurityException, NoSuchMethodException
	{
		if (_log.isDebugEnabled() && !values.isEmpty())
		{
			if (_log.isDebugEnabled() && !values.isEmpty())
			{
				// Se eliminan los elementos que existen
				Document dom = PortalLocalServiceUtil.executeQueryAsDom( String.format(SELECT_GLOBALIDS, groupId, StringUtils.join(values, "','")) );
				for (Node layout : dom.selectNodes("/rs/row"))
				{
					values.remove( ((Element)layout).attributeValue("id") );
				}
				
				ErrorRaiser.throwIfFalse(values.isEmpty(), IterErrorKeys.XYZ_E_SITE_THEMES_LAYOUT_NOT_FOUND_ZYX, StringUtils.join(values, ","));
			}
		}
	}
	
	private static final String SELECT_MODELS = new StringBuilder(
		"SELECT id_	id														\n").append(
		"FROM Designer_PageTemplate											\n").append(
		"INNER JOIN Layout ON Designer_PageTemplate.layoutId = Layout.plid	\n").append(
		"  WHERE id_ IN (%s)												\n").toString();
	private void checkModels(Set<String> values) throws ServiceError, SecurityException, NoSuchMethodException
	{
		if (_log.isDebugEnabled() && !values.isEmpty())
		{
			if (_log.isDebugEnabled() && !values.isEmpty())
			{
				// Se eliminan los elementos que existen
				Document dom = PortalLocalServiceUtil.executeQueryAsDom( String.format(SELECT_MODELS, StringUtils.join(values, ",")) );
				for (Node layout : dom.selectNodes("/rs/row"))
				{
					values.remove( ((Element)layout).attributeValue("id") );
				}
				
				ErrorRaiser.throwIfFalse(values.isEmpty(), IterErrorKeys.XYZ_E_SITE_THEMES_MODEL_NOT_FOUND_ZYX, StringUtils.join(values, ","));
			}
		}
	}
	
	private static final String SELECT_RESOURCES = new StringBuilder(
		"SELECT rsrcmd5 id									\n").append(
		"FROM theme_webresource								\n").append(
		"  WHERE delegationId = %d AND rsrcmd5 IN ('%s')	\n").toString();
	private void checkResources(long delegationId, Set<String> values) throws ServiceError, SecurityException, NoSuchMethodException
	{
		if (_log.isDebugEnabled() && !values.isEmpty())
		{
			if (_log.isDebugEnabled() && !values.isEmpty())
			{
				// Se eliminan los elementos que existen
				Document dom = PortalLocalServiceUtil.executeQueryAsDom( String.format(SELECT_RESOURCES, delegationId, StringUtils.join(values, "','")) );
				for (Node layout : dom.selectNodes("/rs/row"))
				{
					values.remove( ((Element)layout).attributeValue("id") );
				}
				
				ErrorRaiser.throwIfFalse(values.isEmpty(), IterErrorKeys.XYZ_E_SITE_THEMES_RSRC_NOT_FOUND_ZYX, StringUtils.join(values, ","));
			}
		}
	}

	
	public String getWebResourceByPlidAndPlace(long plid, String place, String contentType) 
			throws ServiceError, NoSuchAlgorithmException, IOException, SQLException, 
				   SecurityException, NoSuchMethodException, DecoderException
   {
		return getWebResourceByPlidAndPlace(plid, place,  contentType, false);
   }
				   
	public String getWebResourceByPlidAndPlace(long plid, String place, String contentTypes, boolean preloading) 
			throws ServiceError, NoSuchAlgorithmException, IOException, SQLException, 
				   SecurityException, NoSuchMethodException, DecoderException

	{
		StringBuilder webresources = new StringBuilder();
		
		ErrorRaiser.throwIfNull(place);
		
		if (Validator.isNull(contentTypes))
			return webresources.toString();

		
		String sections  = String.valueOf(plid);
		long sectionPlid = SectionUtil.getSectionPlid( IterRequest.getOriginalRequest() );
		Object layoutType= IterRequest.getAttribute(WebKeys.LAYOUT_TYPE);
		
		// layoutType == null: 							  No es un Layout. Podría ser un detalle, una página de búsqueda o de categorías
		// !String.valueOf(layoutType).equals("section"): Es un layout pero NO una sección. Puede ser un modelo de detalle, de sección o un catálogo
		if (layoutType == null || !String.valueOf(layoutType).equals("section"))
		{
			// 0010202: Las secciones añaden scripts del "tema por defecto" si el tema asignado a la página no tiene recursos.
			sections = String.format("%s,%d,0", sections, sectionPlid);
		}
		// plid != sectionPlid: Es una sección pero realmente se trata de un catálogo simulando una sección donde 
		//						"plid" es el plid del CatalogPage y "sectionPlid" es el plid de la sección simulada.
		//						- Se incluye el plid por si en algún momento a los CatalogPage se les asignan recursos del tema. 
		//						- NO se incluye el 0 porque las secciones NO deben ir a buscar la configuración por defecto
		else if (plid != sectionPlid && sectionPlid > 0)
		{
			sections = String.format("%s,%d", sections, sectionPlid);
		}
		
		// Los embebidos o incrustados NO tienen preloadig. Si está en modo preload NO se tienen en cuenta
		String sql = String.format(GET_WEBRESOURCE_BY_PLID_PLACE_TYPE, sections, place, contentTypes, (preloading) ? EXCLUDE_NON_PRELOADABLES : "");
		_log.debug(sql);
		
		Document document = PortalLocalServiceUtil.executeQueryAsDom(sql);
		
		List<Node> nodes = SAXReaderUtil.createXPath("/rs/row").selectNodes(document);
		for(Node node:nodes)
		{
			boolean async 		= GetterUtil.getBoolean(XMLHelper.getTextValueOf(node, "@async"), true);
			String contentType	= XMLHelper.getTextValueOf(node, "@rsrccontenttype");
			
			String callback = GetterUtil.getString((String)XMLHelper.getTextValueOf(node, "@callback"), StringPool.BLANK);
			if(Validator.isNotNull(callback))
			{
				callback = new String(Base64.decodeBase64(callback), StringPool.UTF8);
				callback = String.format(JAVASCRIPT_BASIC_CALLBACK, callback);
			}
			
			String rsrccontentspec 	= XMLHelper.getTextValueOf(node, "@rsrccontentspec");
			
			if (rsrccontentspec.equalsIgnoreCase(CONTENT_SPEC_EMBEDED) || rsrccontentspec.equalsIgnoreCase(CONTENT_SPEC_THIRDPTY))
			{
				String rsrccontent = XMLHelper.getTextValueOf(node, "@rsrccontent");
				if(Validator.isNotNull(rsrccontent))
				{
					rsrccontent = new String(Base64.decodeBase64(rsrccontent), StringPool.UTF8);
					
					//Modo código fuente
					if(rsrccontentspec.equalsIgnoreCase(CONTENT_SPEC_EMBEDED))
					{
						if(contentType.equalsIgnoreCase(ContentTypes.TEXT_JAVASCRIPT))
						{
							webresources.append(String.format(JAVASCRIPT_BASIC_SOURCE, ContentTypes.TEXT_JAVASCRIPT, rsrccontent));
						}
						else if(contentType.equalsIgnoreCase(ContentTypes.TEXT_CSS))
						{
							webresources.append(String.format(CSS_BASIC_SOURCE, ContentTypes.TEXT_CSS, rsrccontent));
						}
						else if(contentType.equalsIgnoreCase(ContentTypes.TEXT_HTML))
						{
							webresources.append(rsrccontent);
						}
					}
					//Modo URL externa
					else
					{
						if (preloading)
						{
							// En rsrccontent va la URL
							webresources.append( IterGlobal.getPreloadContent(rsrccontent, contentType) );
						}
						else if(contentType.equalsIgnoreCase(ContentTypes.TEXT_JAVASCRIPT))
						{
							if(async)
							{
								webresources.append(String.format(JAVASCRIPT_BASIC_SOURCE, ContentTypes.TEXT_JAVASCRIPT, 
										String.format(JAVASCRIPT_BASIC_ASYNC, rsrccontent, callback)));
							}
							else
							{
								webresources.append(String.format(JAVASCRIPT_BASIC_URL, ContentTypes.TEXT_JAVASCRIPT, rsrccontent));
							}
						}
						else if(contentType.equalsIgnoreCase(ContentTypes.TEXT_CSS))
						{
							webresources.append(String.format(CSS_BASIC_URL, rsrccontent, ContentTypes.TEXT_CSS));
						}
					}		
				}
			}
			//Modo URL interna
			else if(rsrccontentspec.equalsIgnoreCase(CONTENT_SPEC_OWNED))
			{
				String rsrcmd5 = XMLHelper.getTextValueOf(node, "@rsrcmd5");
				if(Validator.isNotNull(rsrcmd5))
				{
					String host = StringPool.BLANK;
					long groupId = GetterUtil.getLong( (Serializable) IterRequest.getAttribute(WebKeys.SCOPE_GROUP_ID) );
					if( PropsValues.ITER_ENVIRONMENT.equalsIgnoreCase(WebKeys.ENVIRONMENT_LIVE) )
						host = LayoutSetTools.getStaticServerName(groupId, WEBRSRC_PATH + rsrcmd5);
						
					String iterPath = host + WEBRSRC_PATH + rsrcmd5;
					if (preloading)
					{
						// Hay que obtener la extensión a partir del ContentType
						webresources.append( IterGlobal.getPreloadContent(iterPath.concat(IterGlobal.getExtension(contentType)), contentType) );
					}
					else if(contentType.equalsIgnoreCase(ContentTypes.TEXT_JAVASCRIPT))
					{
						iterPath = iterPath + ".js";
						if(async)
						{
							webresources.append(String.format(JAVASCRIPT_BASIC_SOURCE, ContentTypes.TEXT_JAVASCRIPT, 
									String.format(JAVASCRIPT_BASIC_ASYNC, iterPath, callback)));
						}
						else
						{
							webresources.append(String.format(
									JAVASCRIPT_BASIC_URL, ContentTypes.TEXT_JAVASCRIPT, iterPath));
						}
					}
					else if(contentType.equalsIgnoreCase(ContentTypes.TEXT_CSS))
					{
						iterPath = iterPath + ".css";
						webresources.append(String.format(CSS_BASIC_URL, iterPath, ContentTypes.TEXT_CSS));
					}
				}
			}
		}
		
		return webresources.toString();
	}
	
	/**
	 * La gestión de la XSL XSL_GET_PUBLISHABLE_INFO es lenta por el volumen del XML al transportar los recursos. Se elimina para obtenerlo por la select
	 * 
	 * @param groupId Grupo del que se obtendrá los recursos publicables
	 * @return Devuelve un DOM cuyo XML tiene el mismo formato que el recibido en <code>deliverTheme</code>.
	 * 
	 * @throws DocumentException
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws ServiceError 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public Document getPublishableInfo(long groupId, String themeId) throws DocumentException, SecurityException, NoSuchMethodException, PortalException, SystemException, ServiceError, ClientProtocolException, IOException
	{
		String sql = String.format(SEL_PUBLISHABLE_LAYOUTS_INFO, groupId, Validator.isNotNull(themeId) ? String.format(" AND layout_webresource.themeid = '%s'", themeId) : "");
		_log.debug(sql);
		
		Document domLayout 		= PortalLocalServiceUtil.executeQueryAsDom( sql, true, "layout-rsrc", "itm");

		// Si se publica un tema concreto, se comprueba que existan registros asociados
		ErrorRaiser.throwIfFalse( !(Validator.isNotNull(themeId) && 0 == XMLHelper.getLongValueOf(domLayout, "count(/layout-rsrc/itm)")), IterErrorKeys.XYZ_E_SITE_THEMES_THEME_NOT_DELIVERED_ZYX);
		
		Document domResources 	= getPublishableResources(groupId, themeId);
		Document infoDom 		= SAXReaderUtil.read("<root/>");
		Element  infoRoot 		= infoDom.getRootElement();
		
		// Se añaden las secciones y los recursos
		infoRoot.add( domLayout.getRootElement().detach() );
		infoRoot.add( domResources.getRootElement().detach() );
		
		// La gestión de la XSL es lenta por el volumen del XML al transportar los recursos. Se elimina para obtenerlo por la select
		// infoDom = SAXReaderUtil.read( XSLUtil.transformXML(infoDom.asXML(), XSL_GET_PUBLISHABLE_INFO) );
		
		Group scopeGroup = GroupLocalServiceUtil.getGroup(groupId);
		infoRoot.addAttribute("groupname", scopeGroup.getName());
		
		// Si se publica solo un tema se especifica
		if (Validator.isNotNull(themeId))
			infoRoot.addAttribute("themeid", themeId);
		
		// Se indica que los recursos son globales (XmlIO_Live.globalId)
		((Element)infoDom.selectSingleNode("/root/layout-rsrc")).addAttribute("globalids", "true");
		
		if (_log.isTraceEnabled())
			writeFile("getPublishableInfo.xml", infoDom.asXML());

		return infoDom;
	}
	
	/**
	 * Método que determina los recursos que necesita el tema o grupo a publicar, consulta cuáles de estos NO existen
	 * en el LIVE, y crea un DOM con toda la información necesaria de ellos para que sean publicados
	 * 
	 * @param groupId
	 * @param themeId
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws PortalException
	 * @throws SystemException
	 * @throws ServiceError
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws DocumentException
	 */
	private Document getPublishableResources(long groupId, String themeId) throws SecurityException, NoSuchMethodException, PortalException, SystemException, ServiceError, ClientProtocolException, IOException, DocumentException
	{
		long delegationId = GroupLocalServiceUtil.getGroup(groupId).getDelegationId();
		
		// Se comprueba qué recursos son necesarios enviar
		String themeCondition = Validator.isNotNull(themeId) ? String.format(" AND layout_webresource.themeid = '%s'", themeId) : "";
		String sql = String.format(SEL_PUBLISHABLE_RESOURCES, groupId, themeCondition);
		_log.debug(sql);
		
		Document domResources = PortalLocalServiceUtil.executeQueryAsDom( sql, true, "rsrc-list", "rsrc" );
		domResources.getRootElement().addAttribute("delegationid", String.valueOf(delegationId));
		
		// Se obtiene la configuración del LIVE
		LiveConfiguration liveConf	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(IterGlobal.getCompanyId());

		List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
		remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.base.service.ThemeWebResourcesServiceUtil"));
		remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"preDeliverTheme"));
		remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[themeSpec]"));
		remoteMethodParams.add(new BasicNameValuePair("themeSpec", 			domResources.asXML()));
		
		String []url = liveConf.getRemoteIterServer2().split(":");
		HttpHost targetHost = new HttpHost(url[0], Integer.valueOf(url[1]));
		JSONObject json = JSONUtil.executeMethod(targetHost, "/xmlio-portlet/secure/json", remoteMethodParams, 
									(int)liveConf.getConnectionTimeOut(),
									(int)liveConf.getOperationTimeOut(),
									liveConf.getRemoteUserName(), liveConf.getRemoteUserPassword());
		
		Document dom = SAXReaderUtil.read(json.getString("returnValue"));
		
		if (dom.getRootElement().elements().size() > 0)
		{
			// Si el LIVE necesita elementos, se obtiene la información de estos
			String ids = StringUtils.join(XMLHelper.getStringValues(dom.selectNodes("/rsrc-list/rsrc/@md5")), "','");
			
			sql = String.format(SEL_PUBLISHABLE_RESOURCES_INFO, groupId, themeCondition, ids);
			_log.debug(sql); 
			
			dom = PortalLocalServiceUtil.executeQueryAsDom( sql, true, "rsrc-list", "rsrc", new String[] {"content", "jscallback"}, true );
		}
		return dom;
	}

	
	public void updateFromPublishProcess(long groupId) throws IOException, SQLException, com.protecmedia.iter.base.service.util.ServiceError, PortalException, SystemException
	{
		if (PropsValues.ITER_ENVIRONMENT.equals(WebKeys.ENVIRONMENT_PREVIEW))
		{
			// Se borran aquellos recursos que sean huérfanos y no hayan sido accedidos desde hace más de orphanDays días
			String sql = String.format(DEL_UNUSED_ORPHAN_RSRC, GroupLocalServiceUtil.getGroup(groupId).getDelegationId(), PropsValues.ITER_THEME_RSRC_ORPHAN_DAYS);
			_log.debug(sql);
			PortalLocalServiceUtil.executeUpdateQuery(sql);
		}
	}
	
	public void lockProcess(long groupId) throws ServiceError
	{
		if (!_logLock.isDebugEnabled())
		{
			Boolean lockStatus = _process.get(groupId);
			ErrorRaiser.throwIfFalse( lockStatus == null || lockStatus.booleanValue() == Boolean.FALSE, IterErrorKeys.XYZ_E_SITE_THEMES_ARE_BUSY_ZYX);
			
			_process.put(groupId, true);
		}
	}
	
	public void unlockProcess(long groupId) throws ServiceError
	{
		if (!_logLock.isDebugEnabled())
		{
			_process.put(groupId, false);
		}
	}
	
	/**
	 * Método que copia todos los recursos del tema del Layout origen al layout destino
	 * 
	 * @param srcPlid
	 * @param dstPlid
	 * @throws SQLException 
	 * @throws IOException 
	 */
	public void copyWebResources(long srcPlid, long dstPlid) throws IOException, SQLException
	{
		PortalLocalServiceUtil.executeUpdateQuery( String.format("CALL ITR_COPY_WEB_RESOURCES( %d, %d )", srcPlid, dstPlid) );
	}
}