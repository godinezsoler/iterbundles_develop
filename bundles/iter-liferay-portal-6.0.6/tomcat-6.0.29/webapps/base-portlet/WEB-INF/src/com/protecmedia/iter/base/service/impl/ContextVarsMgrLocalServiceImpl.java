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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.io.unsync.UnsyncByteArrayInputStream;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.advertisement.MetadataAdvertisementTools;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.kernel.velocity.IterVelocityTools;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.kernel.zip.ZipWriter;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.ContextVariables;
import com.liferay.portlet.CtxvarUtil;
import com.liferay.portlet.asset.model.AssetVocabularyConstants;
import com.liferay.portlet.documentlibrary.NoSuchFolderException;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.model.DLFolderConstants;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFolderLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.base.ContextVarsMgrLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.IterAdmin;
import com.protecmedia.iter.base.service.util.IterFileUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.TeaserMgr;


/**
 * The implementation of the context vars mgr local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.ContextVarsMgrLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.ContextVarsMgrLocalServiceUtil} to access the context vars mgr local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.ContextVarsMgrLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.ContextVarsMgrLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class})
public class ContextVarsMgrLocalServiceImpl extends ContextVarsMgrLocalServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(ContextVarsMgrLocalServiceImpl.class);
	
	private final String TBL_LAYOUT_CTXVARS		= "ctxvars";
	private final String TBL_CATEGORY_CTXVARS 	= "ctxvarscategory";
	private final String PLID_COLUMN_NAME = "plid";
	private final String CATEGORYID_COLUMN_NAME = "categoryid";
	private final String GET_CATEGORY_FROM_PATH = new StringBuilder(
			"SELECT ITR_GET_CATEGORY_FROM_PATH_FUNC(%d,'%s','").append(StringPool.SECTION).append("') categoryId").toString();

	private static final String SEL_VOCABULARY_WITHOUT_PREFIX = new StringBuilder(
		"SELECT vocabularyId, vocabularyName																			\n").append( 
		"FROM (																											\n").append(
		"		SELECT 	vocabularyId, 																					\n").append(
		"				").append(AssetVocabularyConstants.getNameSQL("AssetVocabulary.name")).append(" vocabularyName	\n").append(	
		"		FROM AssetVocabulary																					\n").append(
		"		").append(AssetVocabularyConstants.getDelegationRestriction("%1$d", "AssetVocabulary.name")).append("	\n").append(		
		"     ) Voc																										\n").append(	
		"	WHERE vocabularyName IN ('%2$s')																			\n").toString();	
	
	private static final String EXPORT_CTXVAR = new StringBuilder(
		"SELECT varid id, globalid, varname name, varvalue value, friendlyURL, source, vartype type, \n").append(
		"       fileentryid, 1 publishfile														\n").append(
		"FROM ctxvars																			\n").append( 
		"LEFT JOIN Layout ON Layout.plid = ctxvars.plid											\n").append(
		"	WHERE ctxvars.groupid=%d															\n").append( 
		"		AND source IN ('").append(IterKeys.ITER).append("')								\n").toString();
	
	private static final String EXPORT_CTXVAR_CATEGORY = new StringBuilder(
	"SELECT	varid id, globalid, varname name, varvalue value,												\n").append(
	"		ITR_GET_CATEGORY_PATH_EXT(categoryid, '").append(StringPool.SECTION).append("', 0) categoryPath,\n").append(
	"		advocabularyid, source, vartype type, fileentryid, 1 publishfile								\n").append(
	"FROM ctxvarscategory 																					\n").append(
	"  WHERE groupid=%d 																					\n").append(
	"    AND source IN ('").append(IterKeys.ITER).append("')												\n").toString();
	
	private static final String EXPORT_ADVOCABULARY = new StringBuilder(
	"SELECT ").append(AssetVocabularyConstants.getNameSQL("AssetVocabulary.name")).append(" vocabularyName	\n").append(		
	"FROM advocabulary																						\n").append(
	"INNER JOIN AssetVocabulary ON AssetVocabulary.vocabularyId = advocabulary.vocabularyId					\n").append(
	"  WHERE advocabulary.groupId = %d																		\n").toString();
	
	private static final String ADD_TO_DELETE_BACK = new StringBuilder(
		"INSERT INTO ctxvarsdeleted(varid, groupid, source, ctxvartable, deleteddate) 	\n").append(
		"SELECT globalid, groupId, source, '%1$s', '%2$s'								\n").append(
		"FROM %1$s	 																	\n").append(
		"	WHERE varid IN ('%3$s') 													\n").append(
		"		AND publicationdate IS NOT NULL				  				 			").toString();

	private static final String ADD_TO_DELETE_LIVE = new StringBuilder(
		"INSERT INTO ctxvarsdeleted(varid, groupid, source, ctxvartable, deleteddate) 	\n").append(
		"SELECT fileentryid, groupId, source, '%1$s', '%2$s'							\n").append(
		"FROM %1$s 																		\n").append(
		"	WHERE varid IN ('%3$s') 													\n").append(
		"		AND vartype = 'image'				  		  							\n").append(
		"		AND fileentryid IS NOT NULL									  			").toString();

	
	private static final String SEL_DELETED_CTXVAR_IMG = new StringBuilder(
		"SELECT DLFileEntry.fileEntryId										\n").append(
		"FROM DLFileEntry													\n").append(
		"	WHERE DLFileEntry.groupId = %d AND DLFileEntry.uuid_ = '%s'		  ").toString();

	private static final String DEL_DELETED_CTXVAR_IMG = new StringBuilder(
		"DELETE FROM ctxvarsdeleted											\n").append(
		"WHERE ctxvarsdeleted.source = 'MILENIUM'							\n").append(
		"	AND groupid = %d AND varid = '%s'								  ").toString();
			
			
	private final String GET_PLID_GLOBALID = "CAST((IF(plid IS NULL, NULL, (SELECT globalid FROM Xmlio_Live WHERE localId=plid AND classNameValue='" + IterKeys.CLASSNAME_LAYOUT + "'))) AS CHAR CHARACTER SET utf8) assignedtoid";
	private final String GET_CATEGORYID_GLOBALID = "CAST((IF(categoryid IS NULL, NULL, (SELECT globalid FROM Xmlio_Live WHERE localId=categoryid AND classNameValue='" + IterKeys.CLASSNAME_CATEGORY + "'))) AS CHAR CHARACTER SET utf8) assignedtoid";
	private final String GET_VOCABULARYID_GLOBALID = "CAST((IF(vocabularyid IS NULL, NULL, (SELECT globalid FROM Xmlio_Live WHERE localId=vocabularyid AND classNameValue='" + IterKeys.CLASSNAME_VOCABULARY + "'))) AS CHAR CHARACTER SET utf8) vocabularyid";
	
	private static final String GET_TO_DELETE 				= "SELECT varid globalid, groupid, ctxvartable FROM ctxvarsdeleted \n WHERE ";
	private static final String DELETE_ALL 					= " groupid=%s and source='%s'";
	private static final String DELETE_BY_VARID				= " varid IN ('%s')";
	
	private static final String UPDATE_TO_DELETE_PUBLISHED 	= "DELETE FROM ctxvarsdeleted WHERE varid IN %s";
	
	private static final String GET_LAYOUT_VARS_TO_PUBLISH	= new StringBuilder(
			"SELECT varid id, globalid, varname name, varvalue value, source, vartype type, fileentryid, publishfile, %s, "																				).append(
			" IF(publicationdate IS NULL, '" + IterKeys.CREATE + "', '" + IterKeys.UPDATE + "') operation \n FROM (\n %%s \n) query\n"																		).toString();
	
	private static final String GET_CATEGORY_VARS_TO_PUBLISH	= new StringBuilder(
			"SELECT varid id, globalid, varname name, varvalue value, advocabularyid, source, vartype type, fileentryid, publishfile, %s, "																				).append(
			" IF(publicationdate IS NULL, '" + IterKeys.CREATE + "', '" + IterKeys.UPDATE + "') operation FROM (\n %%s \n) query\n"																		).toString();


	private static final String PUBLISH_ALL	= new StringBuilder(
		"SELECT * 																						\n").append(
		"FROM %s 																						\n").append(
		"	WHERE groupid=%s 																			\n").append(
		"		AND source='%s' 																		\n").append(
		"		%s																						\n").append(	
		"		AND IF(publicationdate IS NOT NULL AND modifieddate <= publicationdate, false, true)	\n").toString();
		
	private static final String PUBLISH_BY_VARID			= "SELECT * FROM %s WHERE varid IN ('%s') AND IF(publicationdate IS NOT NULL AND modifieddate <= publicationdate, false, true)";
	
	private static final String UPDATE_PUBLISHED_VARS 		= "UPDATE %s SET publicationdate='%s', publishfile=false WHERE varid IN %s";
	private final String UPDATE_ADVOCABULARIES_PUBLISHED	= "UPDATE advocabulary SET publicationdate='%s' WHERE advocabularyid IN %s";
	
	private final String GET_VOCS_TO_PUBLISH = "SELECT advocabularyid, %s FROM advocabulary WHERE groupid=%s AND publicationdate IS NULL \n";
	private final String GET_DELETED_VOCS_TO_PUBLISH = "SELECT adid, adtable FROM addeleted WHERE groupid=%s AND adtable='advocabulary' \n";
	
/*	
	private static final String UPDATE_PUBLISHED_DISABLED_VARS	= new StringBuilder(
			"UPDATE ctxvars SET publicationdate=NULL, publishfile=true 					\n").append(
			"	WHERE varname LIKE '[%%]' 												\n").append(
			"		AND groupid = %d													\n").append(
			"		AND vartype='image'													  ").toString();
*/		
//	private static final String GET_VARIABLE 				= "SELECT varid id, varname name, varvalue value, source FROM ctxvars WHERE varid='%s'";
	private static final String GET_VARIABLE_BY_ID 			= "SELECT * FROM ctxvars WHERE varid='%s'";
	private static final String GET_VARIABLES 				= "SELECT varid id, varname name, varvalue value, source \n FROM %1$s \n WHERE groupid=%2$s AND IFNULL(%3$s, 0) = IFNULL(%4$s, 0) \n ORDER BY varname ASC, varid ASC";
	
	private static final String ADD_LAYOUT_VARIABLE 				= new StringBuilder(
		"INSERT INTO ctxvars(varid, globalid, groupid, varname, plid, varvalue, source, vartype, fileentryid, publishfile, modifieddate, publicationdate) \n").append(
		"	VALUES ('%s', '%s', %s, '%s', %s, '%s', '%s', '%s', %s, %s, '%s', NULL)																			").toString();
	
	private static final String ADD_CATEGORY_VARIABLE 				= new StringBuilder(
		"INSERT INTO ctxvarscategory(varid, globalid, groupid, varname, categoryid, varvalue, advocabularyid, source, vartype, fileentryid, publishfile, modifieddate, publicationdate) \n").append(
		"	VALUES ('%%1$s', '%%2$s', %%3$s, '%%4$s', %%5$s, '%%6$s', (%1$s) ,'%%7$s', '%%8$s', %%9$s, %%10$s, '%%11$s', NULL)																			").toString();
	
	private final String GET_ADVOCID_FROM_CATID = new StringBuilder(
		" SELECT advocabularyid \n"																										).append(
		" FROM advocabulary \n"																											).append(
		" INNER JOIN assetvocabulary ON (advocabulary.vocabularyId=assetvocabulary.vocabularyId) \n"									).append(
		" INNER JOIN assetcategory ON(assetvocabulary.vocabularyId=assetcategory.vocabularyId AND assetcategory.categoryId=%5$s) \n"	).append(
		" WHERE advocabulary.groupid=%3$s"																								).toString();
	
	private static final String UPDATE_VARIABLE 			= new StringBuilder(
		"UPDATE %8$s SET 																								\n").append(
		"	varname='%1$s', varvalue='%2$s', vartype='%3$s', fileentryid=%4$s, 					 						\n").append(
		"	modifieddate='%6$s', 																						\n").append(
		"	publishfile=%5$s																							\n").append(				
		"	WHERE varid='%7$s'																					  		  ").toString();
		
	private static final String UPDATE_VARIABLE_ID 			= new StringBuilder(
		"UPDATE %9$s SET 																								\n").append(
		"	varid='%1$s', varname='%2$s', varvalue='%3$s', vartype='%4$s', fileentryid=%5$s, 							\n").append(
		"	modifieddate='%7$s', 																						\n").append(
		"	publishfile=%6$s																							\n").append(				
		"	WHERE varid='%8$s'																							  ").toString();
		
	private static final String UPDATE_VARIABLE_NAME		= new StringBuilder(
		"UPDATE %4$s SET 																								\n").append(
		"	varname='%1$s', modifieddate='%2$s'																			\n").append(
		"	WHERE varid='%3$s'																							  ").toString();
		
	private static final String DELETE_VARIABLES 			= "DELETE FROM %s WHERE varid IN ('%s')";
	
	private static final String CHECK_DUPLICATE_ON_INSERT 	= new StringBuilder(
			"SELECT c1.varid id							\n").append(
			"FROM %s c1 								\n").append(
			"	WHERE c1.varname='%s' 					\n").append(
			"		AND c1.groupid=%s 					\n").append(
			"		AND IFNULL(c1.%s, 0)=IFNULL(%s, 0)	\n").toString();
	
	private static final String CHECK_DUPLICATE_ON_UPDATE 	= new StringBuilder(
			"SELECT c1.varid id																								\n").append(
			"FROM %1$s c1 																								\n").append(
			"	WHERE c1.varname='%%1$s' 																					\n").append(
			"		AND c1.varid !='%%2$s' 																					\n").append(
			"		AND c1.groupid=(SELECT c2.groupid FROM ctxvars c2 WHERE c2.varid='%%2$s') 								\n").append(
			"		AND (	\n %2$s																			 				\n").append(
			"			)																									  ").toString();

	private final String DUPLICATE_BY_PLID = new StringBuilder(
			"               (SELECT NOT IF((SELECT c2.plid FROM ctxvars c2 WHERE c2.varid='%2$s'), TRUE, FALSE) 			\n").append(
			"				 AND c1.plid IS NULL) 																			\n").append(
			"			 OR 																								\n").append(
			"				(SELECT IF((SELECT c2.plid FROM ctxvars c2 WHERE c2.varid='%2$s'), TRUE, FALSE) 				\n").append(
			"				 AND c1.plid=(SELECT c2.plid FROM ctxvars c2 WHERE c2.varid='%2$s'))							\n").toString();
	
	private final String DUPLICATE_BY_CATEGORYID = new StringBuilder(
			"               (SELECT NOT IF((SELECT c2.categoryid FROM ctxvarscategory c2 WHERE c2.varid='%2$s'), TRUE, FALSE) 	\n").append(
			"				 AND c1.categoryid IS NULL) 																			\n").append(
			"			 OR 																								\n").append(
			"				(SELECT IF((SELECT c2.categoryid FROM ctxvarscategory c2 WHERE c2.varid='%2$s'), TRUE, FALSE) 		\n").append(
			"				 AND c1.categoryid=(SELECT c2.categoryid FROM ctxvarscategory c2 WHERE c2.varid='%2$s'))		\n").toString();
	
	private static final String GET_FILEENTRYID_BY_VARID	= "SELECT fileentryid, vartype FROM ctxvars WHERE varid='%s'";
	
//	private static final String	CHECK_VOCABULARY_EXISTS_IN_LIVE	= "SELECT COUNT(*) FROM advocabulary WHERE advocabularyid IN ('%s')";
	
	private final String FETCH_DISCRETE_ADVOCABULARIES = new StringBuilder(
		"SELECT Voc.name path, Voc.* 																											\n").append(
		"FROM (																																	\n").append(
		"	SELECT 	v.vocabularyId id, 																											\n").append(
		"			").append(AssetVocabularyConstants.getNameSQL("v.name")).append(" name, 													\n").append(
		"			'").append(IterKeys.TREE_TOP_LEVEL).append("' type, 'true' isBranch, 														\n").append(
		"			(SELECT IF((SELECT COUNT(0) FROM AssetCategory c2 WHERE c2.vocabularyId=v.vocabularyId) > 0, 'true', 'false')) hasChildren, \n").append(
		"	 		'true' enabled, advocabulary.advocabularyid 																				\n").append(
		"	FROM AssetVocabulary v INNER JOIN advocabulary ON (v.vocabularyId=advocabulary.vocabularyid)										\n").append(
		"		WHERE advocabulary.advocabularyid IN ('%s') 																					\n").append(																						
		"		ORDER BY v.name ASC, v.vocabularyId ASC 																						\n").append(
		"	  ) Voc																																\n").toString();

	private final String ADD_VOCS = "INSERT INTO advocabulary(advocabularyid, groupid, vocabularyid) VALUES %s \n";
	private final String DELETE_VOCS = "DELETE FROM advocabulary WHERE advocabularyid IN ('%s') \n";
	
	private final String TYPE_TEXT = "text";
	private final String TYPE_IMG = "image";
	private final String IMGS_FOLDER = "iter_contextvars_images";
//	private final String COL_FILEENTRYID = "fileentryid";
//	private final String COL_VARTYPE = "vartype";
	
	public Document getLayoutVariables(String groupid, String plid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse( Long.valueOf(groupid).longValue() > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX, "Group: " + groupid);
		
		if(Validator.isNull(plid))
			plid = null;
		
		String getLayoutVariables = String.format(GET_VARIABLES, TBL_LAYOUT_CTXVARS, groupid, PLID_COLUMN_NAME, plid );
		
		_log.debug("get layout variables: " + getLayoutVariables);
		
		return PortalLocalServiceUtil.executeQueryAsDom( getLayoutVariables );
	}
	
	public Document getCategoryVariables(String groupid, String categoryid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse( Long.valueOf(groupid).longValue() > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX, "Group: " + groupid);
		
		if(Validator.isNull(categoryid) || categoryid.equalsIgnoreCase("ROOT"))
			categoryid = null;
		
		String getCategoryVariables = String.format(GET_VARIABLES, TBL_CATEGORY_CTXVARS, groupid, CATEGORYID_COLUMN_NAME, categoryid );
		
		_log.debug("get category variables: " + getCategoryVariables);
		
		return PortalLocalServiceUtil.executeQueryAsDom( getCategoryVariables );
	}
	
	public Document getVariableById(String varid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(varid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_VARIABLE_BY_ID, varid));
	}
/*	
	public String addVariable(String xmlData) throws Exception
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row");
		Node node = xpath.selectSingleNode(dataRoot);
		
		String name 		= XMLHelper.getTextValueOf(node, "@varname");
		String value 		= XMLHelper.getTextValueOf(node, "@varvalue");
		String groupid 		= XMLHelper.getTextValueOf(node, "@groupid");
		String assignedtoid = XMLHelper.getTextValueOf(node, "@assignedtoid");
		String uuid 		= XMLHelper.getTextValueOf(node, "@varid");
		
		ErrorRaiser.throwIfNull(value, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		uuid = insertVar(groupid, assignedtoid, name, value, uuid, null, IterKeys.ITER, TYPE_TEXT, StringPool.NULL, false);
		
		IterVelocityTools.initContextVariables();
		
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_VARIABLE, uuid)).asXML();
	}
	
	public String updateVariable(String xmlData) throws Exception
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row");
		Node node = xpath.selectSingleNode(dataRoot);
		
		String uuid = XMLHelper.getTextValueOf(node, "@varid");
		String name = XMLHelper.getTextValueOf(node, "@varname");
		String value = XMLHelper.getTextValueOf(node, "@varvalue");
		
		ErrorRaiser.throwIfNull(value, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		updateVar(uuid, "", name, value, TYPE_TEXT, StringPool.NULL, false);
		
		IterVelocityTools.initContextVariables();
		
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_VARIABLE, uuid)).asXML();
	}
	
	public String deleteVariables(String xmlData) throws Exception
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row");
		
		List<Node> nodes = xpath.selectNodes(dataRoot);
		ErrorRaiser.throwIfFalse(nodes != null && nodes.size() > 0);
		
		String[] vocIds = XMLHelper.getStringValues(nodes, "@varid");
		
		deleteVars(vocIds);
		
		IterVelocityTools.initContextVariables();
		
		return xmlData;
	}
*/
	
	public void importData(long groupId, boolean updtIfExist, String filesPath, Element data) throws Exception
	{
		// Por compatibilidad con el código de publicación, se crea el atributo prevId
		List<Node> ctxNodes = data.selectNodes(".//row");
		for (int i = 0; i < ctxNodes.size(); i++)
		{
			Element elem = (Element) ctxNodes.get(i);
			elem.addAttribute("prevId", elem.attributeValue("id"));
		}

		// Imágenes
		importImages( groupId, filesPath, data.selectNodes("ctxvars/row[@type='image']") );

		// Ctxvars
		importCtxVars(groupId, updtIfExist, data.selectNodes("ctxvars/row"));

		// Advocabulary
		importAdVocabularies(groupId, updtIfExist, data.selectNodes("advocabulary/row"));

		// Ctxvarscategory
		importCtxVarsCategory(groupId, updtIfExist, data.selectNodes("ctxvarscategory/row"));
	}

	/**
	 * Método utilizado desde el live durante la publicación.
	 */
	public void createUpdateContextVars(String temporaryPath, long scopeGroupId, Element rs) throws IOException, SQLException, PortalException, SystemException, ServiceError, DocumentException, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse(PropsValues.ITER_ENVIRONMENT.equals(WebKeys.ENVIRONMENT_LIVE));
		
		// imagenes
		importImages( scopeGroupId, temporaryPath, rs.selectNodes("/rs/ctxvars/row[@type='image' and @publishfile='true']") );
		
		// ctxvars
		List<Node> nodes = rs.selectNodes("/rs/ctxvars/row");
		if(nodes != null && nodes.size() > 0)
		{
			for(Node row:nodes)
			{
				String operation = XMLHelper.getTextValueOf(row, "@operation");
				Element xmlData = SQLQueries.getLiveWellFormedRowAsElement(scopeGroupId, row, "assignedtoid", IterKeys.CLASSNAME_LAYOUT, IterErrorKeys.XYZ_E_LAYOUT_NOT_FOUND_IN_LIVE_ZYX);
				
				if(operation.toLowerCase().equals(IterKeys.CREATE))
					addVariable( String.valueOf(scopeGroupId), StringPool.NULL, StringPool.NULL, xmlData.selectSingleNode("//row"), TBL_LAYOUT_CTXVARS );
				else if(operation.toLowerCase().equals(IterKeys.UPDATE))
					updateVariable( String.valueOf(scopeGroupId), xmlData.selectSingleNode("//row"), TBL_LAYOUT_CTXVARS );
			}
		}
		
		//advocabulary
		nodes = rs.selectNodes("/rs/advocabulary/row");
		if(nodes != null && nodes.size() > 0)
		{
			for(Node row:nodes)
			{
				if(_log.isTraceEnabled())
					_log.trace("ad-vocabulary: " + row.asXML());
				
				Element xmlData = SQLQueries.getLiveWellFormedRowAsElement(scopeGroupId, row, "vocabularyid", IterKeys.CLASSNAME_VOCABULARY, IterErrorKeys.XYZ_E_VOCABULARY_NOT_FOUND_IN_LIVE_ZYX);
				xmlData.addAttribute("groupid", String.valueOf(scopeGroupId));
				addAdvertisementVocabulary(xmlData.asXML());
			}
		}
		
		// ctxvarscategory
		nodes = rs.selectNodes("/rs/ctxvarscategory/row");
		if(nodes != null && nodes.size() > 0)
		{
//			checkAdvertisementVocabularies(nodes);
			
			for(Node row:nodes)
			{
				String operation = XMLHelper.getTextValueOf(row, "@operation");
				Element xmlData = SQLQueries.getLiveWellFormedRowAsElement(scopeGroupId, row, "assignedtoid", IterKeys.CLASSNAME_CATEGORY, IterErrorKeys.XYZ_E_CATEGORY_NOT_FOUND_IN_LIVE_ZYX);
				
				if(operation.toLowerCase().equals(IterKeys.CREATE))
					addVariable( String.valueOf(scopeGroupId), StringPool.NULL, StringPool.NULL, xmlData.selectSingleNode("//row"), TBL_CATEGORY_CTXVARS );
				else if(operation.toLowerCase().equals(IterKeys.UPDATE))
					updateVariable( String.valueOf(scopeGroupId), xmlData.selectSingleNode("//row"), TBL_CATEGORY_CTXVARS );
			}
		}
	}
	
	public void importAdVocabularies(long groupId, boolean updtIfExist, List<Node> adVocabularies) throws Exception
	{
		if (adVocabularies.size() > 0)
		{
			String sql   = String.format("SELECT vocabularyid FROM advocabulary WHERE groupid = %d", groupId);
			Document dom = PortalLocalServiceUtil.executeQueryAsDom(sql);
			
			String[] vocabularyNames = XMLHelper.getStringValues(adVocabularies, "@vocabularyName");
			sql = String.format(SEL_VOCABULARY_WITHOUT_PREFIX, groupId, StringUtils.join(vocabularyNames, "','"));
			Document domVoc = PortalLocalServiceUtil.executeQueryAsDom(sql);

			for (int i = 0; i < adVocabularies.size(); i++)
			{
				Element adVocabulary  = (Element)adVocabularies.get(i);
				
				String vocabularyName 	= XMLHelper.getTextValueOf(adVocabulary, "@vocabularyName");
				String vocabularyId		= XMLHelper.getTextValueOf(domVoc, String.format("//row[@vocabularyName = '%s']/@vocabularyId", vocabularyName));
				ErrorRaiser.throwIfNull(vocabularyId, IterErrorKeys.XYZ_E_VOCABULARY_NOT_FOUND_ZYX, vocabularyName);
				adVocabulary.addAttribute("vocabularyid", vocabularyId);

				boolean exist = XMLHelper.getLongValueOf(dom, String.format("count(//row[@vocabularyid = '%s'])", vocabularyId)) > 0;
				ErrorRaiser.throwIfFalse( !exist || updtIfExist, IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, String.format("%s(%s)", IterAdmin.IA_CLASS_ADVOCABULARY, vocabularyId));
				
				if (!exist)
				{
					Element rootAdVocabulary = SAXReaderUtil.read("<rs/>").getRootElement();
					rootAdVocabulary.add( adVocabulary.detach() );
					rootAdVocabulary.addAttribute("groupid", String.valueOf(groupId));
					addAdvertisementVocabulary(rootAdVocabulary);
				}
			}
		}
	}
	
	public Document addAdvertisementVocabulary(String xmlData) throws DocumentException, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		return addAdvertisementVocabulary( SAXReaderUtil.read(xmlData).getRootElement() );
	}
	
	private Document addAdvertisementVocabulary(Element dataRoot) throws DocumentException, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		Document retVal = SAXReaderUtil.read("<rs/>");
		String scopeGroupId = XMLHelper.getTextValueOf(dataRoot, "@groupid");

		XPath xpath = SAXReaderUtil.createXPath("//row");
		List<Node> vocabularyNodes = xpath.selectNodes(dataRoot);
		
		String data = "('%s', %s, %s)";
		StringBuilder insertValues = new StringBuilder();
		String[] newVocabularies = ArrayUtils.EMPTY_STRING_ARRAY;
		String[] vocIds =  ArrayUtils.EMPTY_STRING_ARRAY;
		for(Node vocabulary : vocabularyNodes)
		{
			if(insertValues.length()>0)
				insertValues.append( StringPool.COMMA );
			
			String uuid = XMLHelper.getTextValueOf(vocabulary, "@advocabularyid");
			if(Validator.isNull(uuid))
			{
				uuid = PortalUUIDUtil.newUUID();
				newVocabularies = (String[]) ArrayUtils.add(newVocabularies, uuid);
			}
			
			String vocId = XMLHelper.getTextValueOf(vocabulary, "@vocabularyid");
			vocIds = (String[]) ArrayUtils.add(vocIds, vocId);
			
			insertValues.append(
					String.format(data, uuid, scopeGroupId, vocId)
								);
		}
		
		if(insertValues.length()>0)
		{
			String query = String.format(ADD_VOCS, insertValues.toString());
			PortalLocalServiceUtil.executeUpdateQuery(query);
			MetadataAdvertisementTools.addVocabulary(scopeGroupId, vocIds);
		}
		
		if( !ArrayUtils.isEmpty(newVocabularies) )
		{
			String sql = String.format(FETCH_DISCRETE_ADVOCABULARIES, StringUtil.merge(newVocabularies, "','"));
			retVal = PortalLocalServiceUtil.executeQueryAsDom(sql);
		}
		
		return retVal;
	}
/*	
	private void checkAdvertisementVocabularies(List<Node> nodes) throws ServiceError
	{
		Set<String> advVocabulariesIds = new HashSet<String>();
		for(Node n : nodes)
		{
			String adVocId = XMLHelper.getStringValueOf(n, "@advocabularyid", StringPool.BLANK);
			ErrorRaiser.throwIfFalse( Validator.isNotNull(adVocId), IterErrorKeys.XYZ_ITR_E_CATEGORY_CTXVAR_VOCABULARY_EMPTY_ZYX);
			advVocabulariesIds.add(adVocId);
		}
		
		String query = String.format(CHECK_VOCABULARY_EXISTS_IN_LIVE, StringUtil.merge(advVocabulariesIds.toArray(), "','"));
		
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList(query);
		if( result!=null && result.size()==1 )
		{
			int count = Integer.valueOf(String.valueOf(result.get(0)));
			ErrorRaiser.throwIfFalse( count==advVocabulariesIds.size(), IterErrorKeys.XYZ_ITR_E_ADVERTISEMENT_VOCABULARY_NOT_FOUND_IN_LIVE_ZYX);
		}
	}
*/
	/**
	 * Método utilizado desde el live durante la publicación.
	 * @throws ServiceError 
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws NoSuchMethodException 
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws SecurityException 
	 */
	public void deleteContextVars(Node ctxvarDeleted, String tableName, Node advVocDeleted) throws ServiceError, IOException, SQLException, SecurityException, PortalException, SystemException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse(PropsValues.ITER_ENVIRONMENT.equals(WebKeys.ENVIRONMENT_LIVE));
		
		if (ctxvarDeleted != null)
		{
			List<Node> deleteNodes = ctxvarDeleted.selectNodes("row");
			
			for(Node n : deleteNodes)
			{
				String ctxVarsId = XMLHelper.getStringValueOf(n, "@id");
				String tblName = XMLHelper.getStringValueOf(n, "@ctxvartable", tableName);
				deleteImage(ctxVarsId, tblName);
				deleteVars(ctxVarsId, tblName);
			}
		}
		
		if (advVocDeleted != null)
		{
			List<Node> deleteNodes = advVocDeleted.selectNodes("row/@adid");
			
			PortalLocalServiceUtil.executeUpdateQuery	( 
					String.format( DELETE_VOCS, StringUtil.merge(XMLHelper.getStringValues(deleteNodes, "."), "','") ) 
								);
		}
	}
	
	public void updatePublicationDateContents(long scopeGroupId, Element el) throws ServiceError, IOException, SQLException, DocumentException
	{
		String publicationDate = SQLQueries.getCurrentDate();

		//ctxvarsdeleted
		List<Node> nodes = el.selectNodes("/rs/ctxvarsdeleted/row/@globalid");
		if(nodes != null && nodes.size() > 0)
		{
			String query = String.format(UPDATE_TO_DELETE_PUBLISHED, TeaserMgr.getInClauseSQL(nodes));
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
		//ctxvars
		nodes = el.selectNodes("/rs/ctxvars/row/@id");
		if(nodes != null && nodes.size() > 0)
		{
			String query = String.format(UPDATE_PUBLISHED_VARS, TBL_LAYOUT_CTXVARS, publicationDate, TeaserMgr.getInClauseSQL(nodes));
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
		//ctxvarscategory
		nodes = el.selectNodes("/rs/ctxvarscategory/row/@id");
		if(nodes != null && nodes.size() > 0)
		{
			String query = String.format(UPDATE_PUBLISHED_VARS, TBL_CATEGORY_CTXVARS, publicationDate, TeaserMgr.getInClauseSQL(nodes));
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
		//advocabulary
		nodes = el.selectNodes("/rs/advocabulary/row/@advocabularyid");
		if(nodes != null && nodes.size() > 0)
		{
			String query = String.format(UPDATE_ADVOCABULARIES_PUBLISHED, publicationDate, TeaserMgr.getInClauseSQL(nodes));
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
	//	PortalLocalServiceUtil.executeUpdateQuery( String.format(UPDATE_PUBLISHED_DISABLED_VARS, scopeGroupId) );
	}
	
	public Document exportData(Long groupId) throws DocumentException, SecurityException, NoSuchMethodException
	{
		Element rs = SAXReaderUtil.read("<ctxvars/>").getRootElement();
		
		String sql = String.format(EXPORT_CTXVAR, groupId);
		rs.add( PortalLocalServiceUtil.executeQueryAsDom(sql, true, "ctxvars", XMLHelper.rowTagName).getRootElement().detach() );
		
		sql = String.format(EXPORT_CTXVAR_CATEGORY, groupId);
		rs.add( PortalLocalServiceUtil.executeQueryAsDom(sql, true, "ctxvarscategory", XMLHelper.rowTagName).getRootElement().detach() );
		
		sql = String.format(EXPORT_ADVOCABULARY, groupId);
		rs.add( PortalLocalServiceUtil.executeQueryAsDom(sql, true, "advocabulary", XMLHelper.rowTagName).getRootElement().detach() );

		return rs.getDocument();
	}
	
	public Element generateExportElement(long scopeGroupId, String source, String[] ctxVarIds) throws SecurityException, NoSuchMethodException, IOException, DocumentException, PortalException, SystemException
	{
		Element rs = SAXReaderUtil.read("<rs/>").getRootElement();
		
		if (IterLocalServiceUtil.getEnvironment().equals(WebKeys.ENVIRONMENT_PREVIEW))
		{
			Group scopeGroup = GroupLocalServiceUtil.getGroup(scopeGroupId);
			
			//	<rs groupname="La Razón">
			//		<ctxvarsdeleted/>
			//		<ctxvars/>
			//	</rs>
			rs.addAttribute("groupname", scopeGroup.getName());
			rs.addAttribute("delegationid", String.valueOf(scopeGroup.getDelegationId()));
			
			String deleteQuery = GET_TO_DELETE;
			String publishByLayout = String.format( GET_LAYOUT_VARS_TO_PUBLISH, GET_PLID_GLOBALID );
			String publishByCategory = String.format( GET_CATEGORY_VARS_TO_PUBLISH, GET_CATEGORYID_GLOBALID);
			String publishAdvVocabularies = String.format(GET_VOCS_TO_PUBLISH, GET_VOCABULARYID_GLOBALID, scopeGroupId); 
			String publishDeletedAdvVocabularies = String.format(GET_DELETED_VOCS_TO_PUBLISH, scopeGroupId);
			
			if(Validator.isNull(ctxVarIds))
			{
				deleteQuery += String.format(DELETE_ALL, scopeGroupId, source);
				publishByLayout   = String.format( publishByLayout,   String.format(PUBLISH_ALL, TBL_LAYOUT_CTXVARS,   scopeGroupId, source, CtxvarUtil.PUBLISHED_LAYOUTS) );
				publishByCategory = String.format( publishByCategory, String.format(PUBLISH_ALL, TBL_CATEGORY_CTXVARS, scopeGroupId, source, "") );
			}
			else
			{
				String varids = StringUtil.merge(ctxVarIds, "','");
				deleteQuery += String.format(DELETE_BY_VARID, varids);
				publishByLayout = String.format( publishByLayout, String.format(PUBLISH_BY_VARID, TBL_LAYOUT_CTXVARS, varids) );
				publishByCategory = String.format( publishByCategory, String.format(PUBLISH_BY_VARID, TBL_CATEGORY_CTXVARS, varids) );
			}
			
			Document dom = (PortalLocalServiceUtil.executeQueryAsDom(deleteQuery, true, "ctxvarsdeleted", XMLHelper.rowTagName));
			rs.add(dom.getRootElement().detach());
			
			dom = (PortalLocalServiceUtil.executeQueryAsDom(publishDeletedAdvVocabularies, true, "advocabularydeleted", XMLHelper.rowTagName));
			rs.add(dom.getRootElement().detach());
			
			dom = (PortalLocalServiceUtil.executeQueryAsDom(publishByLayout, true, "ctxvars", XMLHelper.rowTagName));
			rs.add(dom.getRootElement().detach());
			
			dom = (PortalLocalServiceUtil.executeQueryAsDom(publishByCategory, true, "ctxvarscategory", XMLHelper.rowTagName));
			rs.add(dom.getRootElement().detach());
			
			dom = PortalLocalServiceUtil.executeQueryAsDom(publishAdvVocabularies, true, "advocabulary", XMLHelper.rowTagName);
			rs.add(dom.getRootElement().detach());
		}
		
		return rs;
	}
/*
	private void checkDuplicateName(String varid, String varname) throws ServiceError, SecurityException, NoSuchMethodException, PortalException, SystemException, IOException, SQLException
	{
		checkDuplicateName(null, null, varid, varname);
	}
*/	
	private void checkDuplicateName(String sql, String varname, String tableName) throws SecurityException, NoSuchMethodException, ServiceError, PortalException, SystemException, IOException, SQLException 
	{
/*
		if (Validator.isNull(plid))
			plid = " IS NULL";
		else
			plid = " = " + plid;

		// Se obtiene la lista de elementos repetidos por nombre y PLID
		String sql = (Validator.isNotNull(varid)) ? 
						String.format(CHECK_DUPLICATE_ON_UPDATE, varname, varid) :
						String.format(CHECK_DUPLICATE_ON_INSERT, varname, groupid, plid);	
						
*/
		Document dom = PortalLocalServiceUtil.executeQueryAsDom(sql);
		
		if (PropsValues.ITER_ENVIRONMENT.equals(WebKeys.ENVIRONMENT_PREVIEW))
		{
			long numRows = XMLHelper.getLongValueOf(dom, "count(/rs/row)");
			ErrorRaiser.throwIfFalse(numRows == 0, IterErrorKeys.XYZ_ITR_UNQ_CTXVAR_GROUPID_NAME_ZYX, varname);
		}
		else
		{
			// En el LIVE aquellos que estén repetidos se borran previamente
			deleteContextVars(dom.getRootElement(), tableName, null);
		}
	}
	
	public Document setVarContext(String xml) 
			throws Exception
	{
		ErrorRaiser.throwIfNull(xml, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document d = SAXReaderUtil.read(xml);
		
		String groupId = XMLHelper.getTextValueOf(d, "/root/target/@groupid");
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX);
		
		String source = XMLHelper.getTextValueOf(d, "/root/target/@source", IterKeys.MILENIUM);
		
		String assignTo = TBL_LAYOUT_CTXVARS;
		String assignedToId = XMLHelper.getTextValueOf(d, "/root/target/@layoutid");
		if(assignedToId==null)
		{
			assignTo = TBL_CATEGORY_CTXVARS;
			assignedToId = XMLHelper.getTextValueOf(d, "/root/target/@categoryid");
		}
		
		if( assignedToId.equals("0") || assignedToId.equalsIgnoreCase("ROOT") )
			assignedToId = StringPool.NULL;
		
		List<Node> nodes = d.selectNodes("/root/param");
		
		// Las operaciones se han realizar en el mismo orden que indica MLN, de lo contrario puede haber errores.
		for (Node param : nodes)
		{
			String action = XMLHelper.getTextValueOf(param, "@action");
			
			if (action.equalsIgnoreCase("insert"))
			{
				String varid = addVariable(groupId, assignedToId, source, param, assignTo);
				((Element) param).addAttribute("id", varid);			
			}
			else if (action.equalsIgnoreCase("update"))
			{				
				String varid = updateVariable(groupId, param, assignTo);
				if( Validator.isNotNull(varid) )
					((Element) param).addAttribute("newid", varid);
				else
					param.detach();
			}
			else if (action.equalsIgnoreCase("delete"))
			{
				String id = XMLHelper.getTextValueOf(param, "@id");
				ErrorRaiser.throwIfNull(id);
				
				deleteImage(id, assignTo);
				deleteVars(id, assignTo);
				
				param.detach();
			}
		}		
		
		IterVelocityTools.initContextVariables();
		
		d.setXMLEncoding("ISO-8859-1");
		
		return d;
	}
	
	private void importCtxVars(long groupId, boolean updtIfExist, List<Node> ctxVars) throws SecurityException, NoSuchMethodException, NumberFormatException, PortalException, SystemException, ServiceError, IOException, SQLException
	{
		if (ctxVars.size() > 0)
		{
			String sql   = String.format("SELECT varid, IFNULL(plid, 'NULL') plid, varname FROM ctxvars WHERE groupid = %d", groupId);
			Document dom = PortalLocalServiceUtil.executeQueryAsDom(sql);

			String[] friendlyURLs = XMLHelper.getStringValues(ctxVars, "@friendlyURL");
			Document domLayout = null;
			// Podría ser vacío si todos los plids eran nulos
			if (friendlyURLs.length > 0)
			{
				sql = String.format("SELECT plid, friendlyURL FROM Layout WHERE friendlyURL IN ('%s') AND groupId = %d", StringUtils.join(friendlyURLs, "','"), groupId);
				domLayout = PortalLocalServiceUtil.executeQueryAsDom(sql);
			}

			for (int i = 0; i < ctxVars.size(); i++)
			{
				Element ctxvar  = (Element)ctxVars.get(i);
				String ctxvarId = ctxvar.attributeValue("id");
											
				ctxvar.addAttribute("groupid", String.valueOf(groupId));
				
				String friendlyURL = XMLHelper.getTextValueOf(ctxvar, "@friendlyURL");
				if (Validator.isNotNull(friendlyURL))
				{
					long plid = XMLHelper.getLongValueOf(domLayout, String.format("//row[@friendlyURL = '%s']/@plid", friendlyURL));
					ErrorRaiser.throwIfFalse(plid > 0, IterErrorKeys.XYZ_E_UNDEFINED_LAYOUT_ZYX, friendlyURL);
					ctxvar.addAttribute("assignedtoid", String.valueOf(plid));
				}
				
				String xpath = String.format( "/rs/row[@varname='%s' and @plid = '%s']", ctxvar.attributeValue("name"), GetterUtil.getString(ctxvar.attributeValue("assignedtoid"), "NULL") );
				Node node = dom.selectSingleNode(xpath);
				boolean exist = node != null;
				ErrorRaiser.throwIfFalse( !exist || updtIfExist, IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, String.format("%s(%s)", IterAdmin.IA_CLASS_CTXVARS, ctxvarId));

				if (exist)
				{
					ctxvar.addAttribute("id", ((Element)node).attributeValue("varid"));
					updateVariable( String.valueOf(groupId), ctxvar, TBL_LAYOUT_CTXVARS );
				}
				else
				{
					ctxvar.addAttribute("id", PortalUUIDUtil.newUUID());
					addVariable( String.valueOf(groupId), StringPool.NULL, StringPool.NULL, ctxvar, TBL_LAYOUT_CTXVARS );
				}
			}
		}
	}
	
	private void importCtxVarsCategory(long groupId, boolean updtIfExist, List<Node> ctxVars) throws Exception
	{
		if (ctxVars.size() > 0)
		{
			String sql   = String.format("SELECT varid, IFNULL(categoryid, 'NULL') categoryid, varname FROM ctxvarscategory WHERE groupid=%d", groupId);
			Document dom = PortalLocalServiceUtil.executeQueryAsDom(sql);
			
			for (int i = 0; i < ctxVars.size(); i++)
			{
				Element ctxvar  = (Element)ctxVars.get(i);
				String ctxvarId = ctxvar.attributeValue("id");
	
				ctxvar.addAttribute("groupid", String.valueOf(groupId));
				
				String categoryPath = XMLHelper.getTextValueOf(ctxvar, "@categoryPath");
				if (Validator.isNotNull(categoryPath))
				{
					sql = String.format(GET_CATEGORY_FROM_PATH, groupId, categoryPath);
					String categoryId = PortalLocalServiceUtil.executeQueryAsList(sql).get(0).toString();
					
					ctxvar.addAttribute("assignedtoid", categoryId);
				}
				
				String xpath = String.format( "/rs/row[@varname='%s' and @categoryid = '%s']", ctxvar.attributeValue("name"), GetterUtil.getString(ctxvar.attributeValue("assignedtoid"), "NULL") );
				Node node = dom.selectSingleNode(xpath);
				boolean exist = node != null;
				ErrorRaiser.throwIfFalse( !exist || updtIfExist, IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, String.format("%s(%s)", IterAdmin.IA_CLASS_CTXVARS_CATEGORY, ctxvarId));

				if (exist)
				{
					ctxvar.attributeValue("id", ((Element)node).attributeValue("id"));
					updateVariable( String.valueOf(groupId), ctxvar, TBL_CATEGORY_CTXVARS );
				}
				else
				{
					ctxvar.attributeValue("id", PortalUUIDUtil.newUUID());
					addVariable( String.valueOf(groupId), StringPool.NULL, StringPool.NULL, ctxvar, TBL_CATEGORY_CTXVARS );
				}
			}
		}
	}

	private String addVariable(String groupId, String assignedToId, String source, Node varInfo, String tableName) throws NumberFormatException, PortalException, SystemException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		String retVal = "";
		
		String varId			= XMLHelper.getTextValueOf(varInfo, "@id");
		
		String fileEntryId 		= StringPool.NULL;
		String globalid			= XMLHelper.getTextValueOf(varInfo, "@globalid");
		String varName 			= XMLHelper.getTextValueOf(varInfo, "@name");
		String varValue 		= XMLHelper.getTextValueOf(varInfo, "@value", StringPool.BLANK);
		String varType 			= XMLHelper.getTextValueOf(varInfo, "@type");
		String varSource		= XMLHelper.getTextValueOf(varInfo, "@source", source /*IterKeys.MILENIUM*/);
		String assignId			= XMLHelper.getTextValueOf(varInfo, "@assignedtoid", assignedToId);
		String adVocabularyId	= XMLHelper.getTextValueOf(varInfo, "@advocabularyid", StringPool.BLANK);
		
		boolean publishFile	= false;
		
		ErrorRaiser.throwIfFalse( Validator.isNotNull(varName), IterErrorKeys.XYZ_ITR_E_CTXVAR_NAME_EMPTY_ZYX);
		
		if ( varType.equalsIgnoreCase(TYPE_IMG) )
		{
			if ( !varValue.equals("0") && !varValue.equals(StringPool.BLANK) )
			{
				publishFile 	 = true;
				fileEntryId 	 = varValue;
				DLFileEntry dlfe = DLFileEntryLocalServiceUtil.getDLFileEntry(Long.parseLong(fileEntryId));
				
				if (Validator.isNotNull(varId))
				{
					updateDLFileEntryUUID(dlfe, varId);
				}
				else
				{
					varId = dlfe.getUuid();
				}
				
				varValue = WebKeys.CTX_VARS_SERVLET_PREFIX.concat(dlfe.getTitle());
			}
			else
				varValue = StringPool.BLANK;
		}
		else
			varType = TYPE_TEXT;
		
		
		varName = StringEscapeUtils.escapeSql(varName);
		if(!varValue.isEmpty())
			varValue = StringEscapeUtils.escapeSql(varValue);
		
		for(String sysVar : ContextVariables.getSystemCtxVars())
			ErrorRaiser.throwIfFalse( !sysVar.equalsIgnoreCase(varName), IterErrorKeys.XYZ_ITR_SYSTEM_CTXVAR_NAME_ZYX );
		
		retVal = tableName.equalsIgnoreCase(TBL_LAYOUT_CTXVARS) ? 
					insertLayoutVar(groupId, assignId, varName, varValue, varId, globalid, varSource, varType, fileEntryId, publishFile) : 
					insertCategoryVar(groupId, assignId, adVocabularyId, varName, varValue, varId, globalid, varSource, varType, fileEntryId, publishFile);
		
		 
		return retVal;
	}
	
	private String updateVariable(String groupId, Node varInfo, String tableName) throws NumberFormatException, PortalException, SystemException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		String varId 		= XMLHelper.getTextValueOf(varInfo, "@id");
		
		String newVarId 	= StringPool.BLANK;
		String fileEntryId 	= StringPool.NULL;
		String varName 		= XMLHelper.getTextValueOf(varInfo, "@name");
		String varValue 	= XMLHelper.getTextValueOf(varInfo, "@value", StringPool.BLANK);
		String varType 		= XMLHelper.getTextValueOf(varInfo, "@type");
		boolean importfile	= GetterUtil.getBoolean(XMLHelper.getTextValueOf(varInfo, "@publishfile"), true);
		
		if( varType.equalsIgnoreCase(TYPE_IMG) )
		{
			if( Validator.isNotNull(varValue) && importfile )
			{
				deleteImage( varId, tableName );
				
				if ( !varValue.equalsIgnoreCase("0") )
				{
					fileEntryId 	 = varValue;
					newVarId 		 = XMLHelper.getTextValueOf(varInfo, "@prevId");
					DLFileEntry dlfe = DLFileEntryLocalServiceUtil.getDLFileEntry(Long.parseLong(fileEntryId));
					
					if (Validator.isNotNull(newVarId))
					{
						updateDLFileEntryUUID(dlfe, newVarId);
					}
					else
					{
						newVarId = dlfe.getUuid();
					}
					
					varValue = WebKeys.CTX_VARS_SERVLET_PREFIX.concat(dlfe.getTitle());
				}
				else
				{
					varValue = StringPool.BLANK;
				}
				
				updateVar(varId, newVarId, varName, varValue, varType, fileEntryId, true, tableName);
			}
			else
				updateVarName(varId, varName, tableName);
		}
		else
		{
			varType = TYPE_TEXT;
			
			// Si la variable era de tipo imagen, se borra el dlfileentry.
			deleteImage( varId, tableName );
			updateVar(varId, newVarId, varName, varValue, varType, fileEntryId, false, tableName);
		}
		
		return newVarId;
	}
	
	private void deleteImage(String varId, String tableName) throws ServiceError, SecurityException, NoSuchMethodException, PortalException, SystemException, IOException, SQLException
	{
		if (PropsValues.ITER_ENVIRONMENT.equals(WebKeys.ENVIRONMENT_PREVIEW))
		{
			ErrorRaiser.throwIfNull(varId);
			Document dom = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_FILEENTRYID_BY_VARID, varId) );
			
			String varType = XMLHelper.getTextValueOf(dom, "/rs/row/@vartype", StringPool.BLANK);
			
			if (varType.equalsIgnoreCase(TYPE_IMG))
			{
				removeFileEntry( XMLHelper.getLongValueOf(dom, "/rs/row/@fileentryid") );
			}
		}
		else if( Validator.isNotNull(varId) )
		{
			// LIVE: Se apuntan para borrar los DLFileENtries eliminados
			String sql = String.format(ADD_TO_DELETE_LIVE, tableName, SQLQueries.getCurrentDate(), varId);
			PortalLocalServiceUtil.executeUpdateQuery( sql );
		}
	}
	
	private String insertLayoutVar(String groupid, String layoutPlid, String varName, String varValue, String varId, String globalId, String source, String type, String fileEntryId, boolean publishFile) throws ServiceError, IOException, SQLException, SecurityException, PortalException, SystemException, NoSuchMethodException
	{
		String query = String.format(CHECK_DUPLICATE_ON_INSERT, TBL_LAYOUT_CTXVARS, varName, groupid, PLID_COLUMN_NAME, layoutPlid);
		_log.debug("Check duplicates before insert layout contextvar: " + query);
		checkDuplicateName(query, varName, TBL_LAYOUT_CTXVARS);
		
		if (Validator.isNull(varId))
			varId = PortalUUIDUtil.newUUID();
		
		// Cuando esta inserción proceda de una publicación sí llegará globalId, el que tiene el registro correspondiente, en el BACK
		if (Validator.isNull(globalId))
			globalId = varId;
		
		query = String.format(ADD_LAYOUT_VARIABLE, varId, globalId, groupid, varName, layoutPlid, varValue, source, type, fileEntryId, publishFile, SQLQueries.getCurrentDate(), null);
		
		_log.debug("Add layout context variable: " + query);
		
		PortalLocalServiceUtil.executeUpdateQuery(query);
		
		return varId;
	}
	
	private String insertCategoryVar(String groupid, String categoryId, String advVocabularyId, String varName, String varValue, String varId, String globalId, String source, String type, String fileEntryId, boolean publishFile) throws ServiceError, SecurityException, PortalException, SystemException, NoSuchMethodException, IOException, SQLException
	{
		String query = String.format(CHECK_DUPLICATE_ON_INSERT, TBL_CATEGORY_CTXVARS, varName, groupid, CATEGORYID_COLUMN_NAME, categoryId);
		_log.debug("Check duplicates before insert category contextvar: " + query);
		checkDuplicateName(query, varName, TBL_CATEGORY_CTXVARS);
		
		if (Validator.isNull(varId))
			varId = PortalUUIDUtil.newUUID();
		
		// Cuando esta inserción proceda de una publicación sí llegará globalId, el que tiene el registro correspondiente, en el BACK
		if (Validator.isNull(globalId))
			globalId = varId;

		// Si es de una publicación llegará el advocabularyid, en caso contrario se calcula.
		if( PropsValues.ITER_ENVIRONMENT.equals(WebKeys.ENVIRONMENT_LIVE) )
		{
			String adVocId = Validator.isNotNull(advVocabularyId) ? StringUtil.apostrophe(advVocabularyId) : StringPool.NULL;
			query = String.format(ADD_CATEGORY_VARIABLE, adVocId );
		}
		else
			query = String.format(ADD_CATEGORY_VARIABLE, GET_ADVOCID_FROM_CATID);
		
		query = String.format(query, varId, globalId, groupid, varName, categoryId, varValue, source, type, fileEntryId, publishFile, SQLQueries.getCurrentDate(), null);
		
		_log.debug("Add category context variable: " + query);
		
		PortalLocalServiceUtil.executeUpdateQuery(query);
		
		return varId;
	}
/*	
	private String insertVar(String groupid, String layoutPlid, String varName, String varValue, String varId, String globalId, String source, String type, String fileEntryId, boolean publishFile) throws ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException, PortalException, SystemException
	{
		if (Validator.isNull(varId))
			varId = PortalUUIDUtil.newUUID();
		
		// Cuando esta inserción proceda de una publicación sí llegará globalId, el que tiene el registro correspondiente, en el BACK
		if (Validator.isNull(globalId))
			globalId = varId;
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(ADD_VARIABLE, varId, globalId, groupid, varName, layoutPlid, varValue, source, type, fileEntryId, publishFile, SQLQueries.getCurrentDate(), null));
		
		return varId;
	}
*/	
	private void updateVar(String varId, String newVarId, String varName, String varValue, String varType, String fileEntryId, boolean publishfile, String tableName) throws ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException, PortalException, SystemException
	{
		ErrorRaiser.throwIfNull(varId, 		IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse( Validator.isNotNull(varName), IterErrorKeys.XYZ_ITR_E_CTXVAR_NAME_EMPTY_ZYX);
		
		varName = StringEscapeUtils.escapeSql(varName);
		
		String sql = String.format(CHECK_DUPLICATE_ON_UPDATE,
										tableName,
										tableName.equalsIgnoreCase(TBL_LAYOUT_CTXVARS) ? DUPLICATE_BY_PLID : DUPLICATE_BY_CATEGORYID );
		
		sql = String.format(sql, varName, varId);
		_log.debug("check duplicates before update: " + sql);
		checkDuplicateName(sql, varName, tableName);
		
		if(!varValue.isEmpty())
			varValue = StringEscapeUtils.escapeSql(varValue);
		
		if( Validator.isNotNull(newVarId) )
			sql = String.format(UPDATE_VARIABLE_ID, newVarId, varName, varValue, varType, fileEntryId, publishfile, SQLQueries.getCurrentDate(), varId, tableName);
		else
			sql = String.format(UPDATE_VARIABLE, varName, varValue, varType, fileEntryId, publishfile, SQLQueries.getCurrentDate(), varId, tableName);

		_log.debug("Update context variable: " + sql);
		
		PortalLocalServiceUtil.executeUpdateQuery( sql );
	}
	
	private void updateVarName(String varId, String varName, String tableName) throws ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException, PortalException, SystemException
	{
		ErrorRaiser.throwIfFalse( Validator.isNotNull(varName), IterErrorKeys.XYZ_ITR_E_CTXVAR_NAME_EMPTY_ZYX);
		varName = StringEscapeUtils.escapeSql(varName);
		
		String sql = String.format(CHECK_DUPLICATE_ON_UPDATE,
										tableName,
										tableName.equalsIgnoreCase(TBL_LAYOUT_CTXVARS) ? DUPLICATE_BY_PLID : DUPLICATE_BY_CATEGORYID );

		sql = String.format(sql, varName, varId);
		_log.debug("check duplicates before update name: " + sql);
		checkDuplicateName(sql, varName, tableName);
		
		_log.debug("Update context variable name: " + sql);
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_VARIABLE_NAME, varName, SQLQueries.getCurrentDate(), varId, tableName));
	}
	
	private void deleteVars(String varId, String tableName) throws IOException, SQLException, ServiceError
	{
		if ( Validator.isNotNull(varId) )
		{
			// BACK: Se apuntan para borrar en el LIVE las variables previamente publicadas
			if ( PropsValues.ITER_ENVIRONMENT.equals(WebKeys.ENVIRONMENT_PREVIEW) )
			{
				String sql = String.format(ADD_TO_DELETE_BACK, tableName, SQLQueries.getCurrentDate(), varId);
				_log.debug("Add to delete context variable in PREVIEW: " + sql);
				PortalLocalServiceUtil.executeUpdateQuery( sql );
			}
			
			String query = String.format(DELETE_VARIABLES, tableName, varId);
			_log.debug("Delete context variable: " + query);
			
			PortalLocalServiceUtil.executeUpdateQuery( query );
		}
	}
	
	public long uploadCtxVarImg(long groupId, byte[] bytes) throws PortalException, SystemException, ServiceError, IOException, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse( (bytes!=null && bytes.length>0), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Empty image bytes.");
		
		String extension = IterFileUtil.getExtensionFromBytes(bytes);
		long imgsize = bytes.length;
		InputStream is = new UnsyncByteArrayInputStream(bytes);
		
		DLFileEntry df = uploadFile(groupId, is, imgsize, extension, IMGS_FOLDER);
		
		return df.getFileEntryId();
	}
	
	private void updateDLFileEntryUUID(DLFileEntry dlfileEntry, String uuid) throws SystemException
	{
		dlfileEntry.setUuid(uuid);
		dlfileEntry.setTitle( dlfileEntry.getUuid().concat(".").concat(dlfileEntry.getExtension()) );
		DLFileEntryLocalServiceUtil.updateDLFileEntry(dlfileEntry, false);
	}
	
	private DLFileEntry uploadFile(long groupId, InputStream is, long imagesize, String extension, String folderName) throws ServiceError, IOException, PortalException, SystemException
	{
		ErrorRaiser.throwIfFalse( Validator.isNotNull(groupId) , IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX);
		
		DLFileEntry dlfileEntry = null;
		
		try
		{
			long userId 		= GroupMgr.getDefaultUserId();
			long dlFolderId		= -1;
			
			if( Validator.isNotNull(folderName) )
			{
				DLFolder dlFolder = null;
			    try
			    {
			    	dlFolder = DLFolderLocalServiceUtil.getFolder(groupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, folderName);
			    }
			    catch (NoSuchFolderException nsfe)
			    {
			    	dlFolder = DLFolderLocalServiceUtil.addFolder(userId, groupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, folderName, StringPool.BLANK, new ServiceContext());
			    }
			    
			    if( Validator.isNotNull(dlFolder) )
			    	dlFolderId = dlFolder.getFolderId();
			}
			else
				dlFolderId = DLFolderConstants.DEFAULT_PARENT_FOLDER_ID;
			
			if (dlFolderId != -1)
			{
				if (!extension.startsWith(StringPool.PERIOD))
					extension = StringPool.PERIOD.concat(extension);
				
				// Se crea un título temporal çunico para que no falle la validación por GroupId-FolderId-Title que realiza el FileEntry
				String tmpTitle = String.valueOf( new Date().getTime() ).concat(extension);
				dlfileEntry = DLFileEntryLocalServiceUtil.addFileEntry(userId, groupId, dlFolderId, tmpTitle, StringPool.BLANK, StringPool.BLANK, StringPool.BLANK, is, imagesize, new ServiceContext());
				updateDLFileEntryUUID(dlfileEntry, dlfileEntry.getUuid());
			}
			
			ErrorRaiser.throwIfNull(dlfileEntry, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}
		finally
		{
			try
			{
				if(Validator.isNotNull(is))
					is.close();
			}
			catch(Throwable tClose){}
		}
		
		return dlfileEntry;
	}
	
	public void deleteCtxVarImg(long[] fileEntryIds) throws PortalException, SystemException, SecurityException, NoSuchMethodException
	{
		for (long fileEntryId : fileEntryIds)
			removeFileEntry(fileEntryId);
	}
	
	private void removeFileEntry(long fileEntryId) throws PortalException, SystemException
	{
		if (Validator.isNotNull(fileEntryId) && PropsValues.ITER_ENVIRONMENT.equals(WebKeys.ENVIRONMENT_PREVIEW))
		{
			DLFileEntry dlFileEntry = DLFileEntryLocalServiceUtil.getDLFileEntry(fileEntryId);
			DLFileEntryLocalServiceUtil.deleteFileEntryNoHook(dlFileEntry);
		}
	}
	
	public void addImagesToZIP(ZipWriter zipWriter, Node publicationXML) throws PortalException, SystemException, IOException
	{
		List<Node> imgNodes = publicationXML.selectNodes("ctxvars/row[@type='image' and @publishfile='true']");
		long delegationId = XMLHelper.getLongValueOf( publicationXML, "@delegationid" );
		
		for(Node img : imgNodes)
		{
			long fileEntryId = XMLHelper.getLongValueOf( img, "@fileentryid" );
			if( Validator.isNotNull(fileEntryId) )
			{
				DLFileEntry dlfileEntry = DLFileEntryLocalServiceUtil.getDLFileEntry( fileEntryId );
				InputStream is = DLFileEntryLocalServiceUtil.getFileAsStream(delegationId, dlfileEntry.getUserId(), dlfileEntry.getGroupId(), dlfileEntry.getFolderId(), dlfileEntry.getName());
				zipWriter.addEntry(dlfileEntry.getTitle(), IOUtils.toByteArray(is));
				is.close();
				((Element)img).addAttribute("fileentrytitle", dlfileEntry.getTitle());
			}
		}
	}

	private void importImages(long scopeGroupId, String temporaryPath, List<Node> imageNodes) throws PortalException, SystemException, ServiceError, IOException, SecurityException, NoSuchMethodException, SQLException
	{
		for (int i = 0; i < imageNodes.size(); i++)
		{
			Element image 	 = (Element) imageNodes.get(i);
			String varValue  = "0";
			String fileTitle = XMLHelper.getTextValueOf( image, "@fileentrytitle" );
			
			if ( Validator.isNotNull(fileTitle) && (varValue = recoverImage(scopeGroupId, image)).equals("0") )
			{
				File file 		 = new File(temporaryPath + File.separatorChar + fileTitle);
				String extension = FilenameUtils.getExtension(file.getName());
				InputStream is 	 = new FileInputStream(file);
				
				DLFileEntry dlfe = uploadFile(scopeGroupId, is, file.length(), extension, IMGS_FOLDER);
				varValue = String.valueOf(dlfe.getFileEntryId());
			}
			
			image.addAttribute("value", varValue);
		}
	}
	
	/**
	 * Comprueba si la imagen ya existe y está apuntada para borrar.<br/>
	 * En dicho caso la quita de la lista de elementos a borrar y actualiza el <code>value</code> con el <code>fileEntryId</code>.<br/><br/>
	 * 
	 * @param groupId
	 * @param elem
	 * @return <i>"0"</i> si no se ha recuperado la imagen de la lista de pendientes de borrar, o el <code>fileEntryId</code> de la imagen
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws SQLException 
	 * @throws IOException 
	 */
	private String recoverImage(long groupId, Element elem) throws SecurityException, NoSuchMethodException, IOException, SQLException
	{
		String fileEntryId = "0";
		
		String uuid = XMLHelper.getTextValueOf(elem, "@prevId");
		
		if ( Validator.isNotNull(uuid) )
		{
			// Se busca un registro eliminado que coincida
			String sql 	 = String.format(SEL_DELETED_CTXVAR_IMG, groupId, uuid);
			Document dom = PortalLocalServiceUtil.executeQueryAsDom(sql);
			
			fileEntryId = XMLHelper.getTextValueOf(dom, "/rs/row/@fileEntryId", "0");
			if (!fileEntryId.equals("0"))
			{
				// Se elimina el registro de la tabla de pendientes a borrar
				sql = String.format(DEL_DELETED_CTXVAR_IMG, groupId, fileEntryId);
				PortalLocalServiceUtil.executeUpdateQuery(sql);
			}
		}
		return fileEntryId;
	}
	
/*
	private long getFileEntry(String varId)
	{
		long fileEntryId = 0L;
		
		Map<String, Object> result;
		String query = String.format(GET_FILEENTRYID_BY_VARID, varId);
		List<Map<String,Object>> listResult = PortalLocalServiceUtil.executeQueryAsMap(query);
		
		if (listResult!=null && listResult.size()>0 && listResult.get(0)!=null)
		{
			result = listResult.get(0);
			Object obj = result.get(COL_FILEENTRYID);
			if(Validator.isNotNull(obj))
				fileEntryId = (Long) obj;
		}
		
		return fileEntryId;
	}
*/

}
