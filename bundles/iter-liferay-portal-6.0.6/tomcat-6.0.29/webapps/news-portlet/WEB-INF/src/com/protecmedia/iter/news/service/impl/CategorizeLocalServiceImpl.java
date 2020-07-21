/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
/**
 * Copyright (c) 2000-2010 Liferay, Inc. All rights reserved.
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

package com.protecmedia.iter.news.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.CategoriesUtil;
import com.liferay.portal.kernel.util.InstrumentalContentUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.advertisement.MetadataAdvertisementTools;
import com.liferay.portal.kernel.velocity.IterVelocityTools;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.asset.model.AssetCategory;
import com.liferay.portlet.asset.model.AssetEntry;
import com.liferay.portlet.asset.model.AssetVocabulary;
import com.liferay.portlet.asset.service.AssetCategoryLocalServiceUtil;
import com.liferay.portlet.asset.service.AssetCategoryServiceUtil;
import com.liferay.portlet.asset.service.AssetEntryLocalServiceUtil;
import com.liferay.portlet.asset.service.AssetTagLocalServiceUtil;
import com.liferay.portlet.asset.service.AssetVocabularyLocalServiceUtil;
import com.liferay.portlet.asset.service.persistence.AssetEntryQuery;
import com.liferay.portlet.expando.model.ExpandoTable;
import com.liferay.portlet.expando.model.ExpandoTableConstants;
import com.liferay.portlet.expando.model.ExpandoValue;
import com.liferay.portlet.expando.service.ExpandoTableLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoValueLocalServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.news.model.PageContent;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;
import com.protecmedia.iter.news.service.base.CategorizeLocalServiceBaseImpl;
import com.protecmedia.iter.news.util.CategorizeMgr;
import com.protecmedia.iter.news.util.TeaserContentUtil;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;

/**
 * @author Protecmedia
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class CategorizeLocalServiceImpl extends CategorizeLocalServiceBaseImpl 
{	

	private static final String SEL_CATEGORY_NAMES = new StringBuffer 
			  ("-- Devuelve la lista con los nombre de un grupo de categorías. Sin\n").
		append("-- repeticiones y sin importar si las categorías pertenecen a vocabularios distintos.\n").
		append("-- Ordenados por su jerarquía en su árbol de categorías.\n").
		append("SELECT GROUP_CONCAT(DISTINCT AssetCategory.name ORDER BY vocabularyId ASC, leftCategoryId ASC, rightCategoryId DESC) namelist\n").
		append("FROM AssetCategory\n").
		append("  WHERE AssetCategory.categoryId IN (%s)").toString();

	private static final String SEL_CATEGORY_ANCESTOR_NAMES = new StringBuffer 
			  ("-- Devuelve la lista con los nombre de los antecesores de un grupo de categorías. Sin\n").
		append("-- repeticiones y sin importar si las categorías pertenecen a vocabularios distintos.\n").
		append("-- Ordenados por su jerarquía en su árbol de categorías\n").
		append("SELECT GROUP_CONCAT(DISTINCT ancestors.name ORDER BY ancestors.vocabularyId ASC, ancestors.leftCategoryId ASC, ancestors.rightCategoryId DESC) namelist\n").
		append("FROM AssetCategory\n").
		append("INNER JOIN AssetCategory ancestors ON (AssetCategory.vocabularyId    = ancestors.vocabularyId\n").
		append("                                       AND ( -- La categoría se incluirá en los resultados\n").
		append("                                       		 AssetCategory.categoryId = ancestors.categoryId\n").
		append("                                       		 -- Los ancestros de dicha categoría\n").
		append("                                       		 OR (ancestors.leftCategoryId < AssetCategory.leftCategoryId AND\n").
		append("                                       		     ancestors.rightCategoryId > AssetCategory.rightCategoryId)\n").
		append("                                           ))\n").
		append("  WHERE AssetCategory.categoryId IN (%s)").toString();
	
	private static final String GET_GLOBALID = "SELECT globalid \nFROM Xmlio_Live \nWHERE localId IN (%s) \n\tAND classNameValue='%s'";
	private static final String COUNT_LIVE_CATEGORIES = "SELECT COUNT(*) numchildren \nFROM Xmlio_Live \nWHERE globalId IN ('%s') \n\tAND classNameValue='%s'";

	private static Log _log = LogFactoryUtil.getLog(CategorizeLocalServiceImpl.class);
	
	/* ***********************************
	 * 
	 *  VOCABULARY
	 * 
	 * ***********************************/
	/*
	 * ADD
	 */
	public AssetVocabulary addVocabulary(long groupId, long userId, String title, String description) throws PortalException, SystemException {

		ServiceContext serviceContext = new ServiceContext();
		serviceContext.setAddCommunityPermissions(true);
		serviceContext.setAddGuestPermissions(true);
		serviceContext.setScopeGroupId(groupId);
		
		Map<Locale, String> titleMap = new HashMap<Locale, String>();				
		titleMap.put(LocaleUtil.getDefault(), title);
		
		Map<Locale, String> descriptionMap = new HashMap<Locale, String>();		
		descriptionMap.put(LocaleUtil.getDefault(), description);
		
		String settings = "";

		return AssetVocabularyLocalServiceUtil.addVocabulary(userId, titleMap, descriptionMap, settings, serviceContext);
	}
	
	public String addVocabulary2(String title, String trackVisits) throws PortalException, SystemException
	{
		ServiceContext serviceContext = new ServiceContext();
		serviceContext.setAddCommunityPermissions(true);
		serviceContext.setAddGuestPermissions(true);
		serviceContext.setScopeGroupId(GroupMgr.getGlobalGroupId());
		
		Map<Locale, String> titleMap = new HashMap<Locale, String>();				
		titleMap.put(LocaleUtil.getDefault(), title);
		
		Map<Locale, String> descriptionMap = new HashMap<Locale, String>();		
		descriptionMap.put(LocaleUtil.getDefault(), StringPool.BLANK);
		
		try
		{
			int aux = Integer.parseInt(trackVisits);
			if (aux < 0 || aux > 1)
				trackVisits = StringPool.BLANK;
		}
		catch (Throwable th)
		{
			trackVisits = StringPool.BLANK;
		}
		
		String settings = trackVisits;

		AssetVocabulary vocabulary =  AssetVocabularyLocalServiceUtil.addVocabulary(GroupMgr.getDefaultUserId(), titleMap, descriptionMap, settings, serviceContext);
		return String.valueOf(vocabulary.getVocabularyId());
	}

	/*
	 * DELETE
	 */
	public void deleteVocabulary(long vocabularyId) throws PortalException, SystemException
	{
		CategorizeMgr.deleteVocabulary(vocabularyId);
	}

	/*
	 * GET
	 */

	public AssetVocabulary getVocabulary(long vocabularyId)
		throws PortalException, SystemException {

		return AssetVocabularyLocalServiceUtil.getVocabulary(vocabularyId);
	}

	/*
	 * UPDATE
	 */
	public AssetVocabulary updateVocabulary(long groupId, long vocabularyId, String title, String description)
		throws PortalException, SystemException {

		ServiceContext serviceContext = new ServiceContext();
		serviceContext.setAddCommunityPermissions(true);
		serviceContext.setAddGuestPermissions(true);
		serviceContext.setScopeGroupId(groupId);
		
		Map<Locale, String> titleMap = new HashMap<Locale, String>();				
		titleMap.put(LocaleUtil.getDefault(), title);
		
		Map<Locale, String> descriptionMap = new HashMap<Locale, String>();		
		descriptionMap.put(LocaleUtil.getDefault(), description);

		String settings = "";
		
		return AssetVocabularyLocalServiceUtil.updateVocabulary(vocabularyId, titleMap, descriptionMap, settings, serviceContext);
	}
	
	public String updateVocabulary2(long vocabularyId, String title, String trackVisits) throws PortalException, SystemException
	{
		ServiceContext serviceContext = new ServiceContext();
		serviceContext.setAddCommunityPermissions(true);
		serviceContext.setAddGuestPermissions(true);
		serviceContext.setScopeGroupId(GroupMgr.getGlobalGroupId());
		
		Map<Locale, String> titleMap = new HashMap<Locale, String>();				
		titleMap.put(LocaleUtil.getDefault(), title);
		
		Map<Locale, String> descriptionMap = new HashMap<Locale, String>();		
		descriptionMap.put(LocaleUtil.getDefault(), StringPool.BLANK);

		try
		{
			int aux = Integer.parseInt(trackVisits);
			if (aux < 0 || aux > 1)
				trackVisits = StringPool.BLANK;
		}
		catch (Throwable th)
		{
			trackVisits = StringPool.BLANK;
		}
		
		String settings = trackVisits;

		AssetVocabulary vocabulary = AssetVocabularyLocalServiceUtil.updateVocabulary(vocabularyId, titleMap, descriptionMap, settings, serviceContext);
		return String.valueOf(vocabulary.getVocabularyId());
	}
	
	
	/* ***********************************
	 * 
	 *  CATEGORY
	 * 
	 * ***********************************/
	
	/*
	 * GET 
	 */
	public AssetCategory getCategory(long categoryId) throws PortalException, SystemException {	
		return AssetCategoryLocalServiceUtil.getCategory(categoryId);
	}	
	
	/*
	 * ADD 
	 */
	public AssetCategory addCategory(long groupId, long userId,	long parentCategoryId, long vocabularyId, String title)	throws PortalException, SystemException {

		ServiceContext serviceContext = new ServiceContext();
		serviceContext.setAddCommunityPermissions(true);
		serviceContext.setAddGuestPermissions(true);
		serviceContext.setScopeGroupId(groupId);
		
		Map<Locale, String> titleMap = new HashMap<Locale, String>();
		
		titleMap.put(LocaleUtil.getDefault(), title);
		
		String[] categoryProperties = new String[] {};
		
		AssetCategory cat = AssetCategoryLocalServiceUtil.addCategory(userId, parentCategoryId, titleMap, vocabularyId,categoryProperties, serviceContext);
		
		MetadataAdvertisementTools.updateAdvCategoriesMap(vocabularyId, cat.getCategoryId(), title, parentCategoryId);
		
		return cat;
	}
	
	/*
	 * UPDATE
	 */
	public AssetCategory updateCategory(long groupId, long userId, long categoryId, long parentCategoryId, long vocabularyId, String title) throws PortalException, SystemException {

		ServiceContext serviceContext = new ServiceContext();
		serviceContext.setAddCommunityPermissions(true);
		serviceContext.setAddGuestPermissions(true);
		serviceContext.setScopeGroupId(groupId);
		
		Map<Locale, String> titleMap = new HashMap<Locale, String>();
		
		titleMap.put(LocaleUtil.getDefault(), title);
		
		String[] categoryProperties = new String[] {};

		AssetCategory cat = AssetCategoryLocalServiceUtil.updateCategory(
			userId, categoryId, parentCategoryId, titleMap, vocabularyId,
			categoryProperties, serviceContext);
		
		MetadataAdvertisementTools.updateAdvCategoriesMap(vocabularyId, cat.getCategoryId(), title, parentCategoryId);
		
		return cat;
	}

	/*
	 * DELETE 
	 */
	public void deleteCategory(long categoryId)	throws PortalException, SystemException
	{
		CategorizeMgr.deleteCategory(categoryId);
	}
	
	
	/* ***********************************
	 * 
	 *  CUSTOM FIELDS
	 * 
	 * ***********************************/
	
	/*
	 * ADD
	 */
	public ExpandoTable addExpandoTable(long companyId, String className) throws PortalException, SystemException {
		
		ExpandoTable exTable = ExpandoTableLocalServiceUtil.addTable(companyId, className, ExpandoTableConstants.DEFAULT_TABLE_NAME);
		
		return exTable;
	}
	
	/*
	 * GET
	 */
	public ExpandoTable getExpandoTable(long companyId, String className) throws PortalException, SystemException {
		return ExpandoTableLocalServiceUtil.getTable(companyId, className, ExpandoTableConstants.DEFAULT_TABLE_NAME);
	}
	
	public ExpandoTable getExpandoTable(long tableId) throws PortalException, SystemException {
		return ExpandoTableLocalServiceUtil.getTable(tableId);
	}
	
	/*
	 * DELETE
	 */
	public void deleteExpandoTable(long companyId, String className) throws PortalException, SystemException {
		ExpandoTableLocalServiceUtil.deleteTable(companyId, className, ExpandoTableConstants.DEFAULT_TABLE_NAME);
	}
	
	public void deleteExpandoTable(long tableId) throws PortalException, SystemException {
		ExpandoTableLocalServiceUtil.deleteTable(tableId);
	}
	
	/*
	 * OPERACIONES COMUNES
	 */
	/**
	 * 
	 * Method to obtain a ExpandoValue of a Custom Field in a WebContent
	 * 
	 * @param companyId Company we are working with
	 * @param webContentId Id of the WebContent where we have to look for the Custom Field
	 * @param columnName Name of the value we want to obtain
	 * @return It returns a Expando Value with the value of the Custom Field in the WebContent
	 */
	public ExpandoValue getWebContentCustomField(long companyId, long webContentId, String columnName){
		String webContentClassName = "com.liferay.portlet.journal.model.JournalArticle";
		return getCustomField(companyId, webContentClassName, webContentId, columnName);
	}
	
	/**
	 * 
	 * Method to obtain a ExpandoValue of a Custom Field in a Layout
	 * 
	 * @param companyId Company we are working with
	 * @param plid Id of the Layout where we have to look for the Custom Field
	 * @param columnName Name of the value we want to obtain
	 * @return It returns a Expando Value with the value of the Custom Field in the Layout
	 */
	public ExpandoValue getLayoutCustomField(long companyId, long plid, String columnName){
		String layoutClassName = "com.liferay.portal.model.Layout";
		return getCustomField(companyId, layoutClassName, plid, columnName);
	}
	
	/**
	 * 
	 * Method to obtain a ExpandoValue of a Custom Field in a Document Library Entry
	 * 
	 * @param companyId Company we are working with
	 * @param documentLibraryId Id of the Document Library Entry where we have to look for the Custom Field
	 * @param columnName Name of the value we want to obtain
	 * @return It returns a Expando Value with the value of the Custom Field in the Document Library Entry
	 */
	public ExpandoValue getDocumentLibraryCustomField(long companyId, long documentLibraryId, String columnName){
		String documentLibraryClassName = "com.liferay.portlet.documentlibrary.model.DLFileEntry";
		return getCustomField(companyId, documentLibraryClassName, documentLibraryId, columnName);
	}
	
	/**
	 * 
	 * Method to obtain a ExpandoValue of a Custom Field in a User
	 * 
	 * @param companyId Company we are working with
	 * @param userId Id of the User where we have to look for the Custom Field
	 * @param columnName Name of the value we want to obtain
	 * @return It returns a Expando Value with the value of the Custom Field in the Document Library Entry
	 */
	public ExpandoValue getUserCustomField(long companyId, long userId, String columnName){
		String documentLibraryClassName = "com.liferay.portal.model.User";
		return getCustomField(companyId, documentLibraryClassName, userId, columnName);
	}
	
	/**
	 * 
	 * Method to obtain a ExpandoValue of a Custom Field (generic for different Class Names)
	 * 
	 * @param companyId Company we are working with
	 * @param className In wich type of objects we have to search
	 * @param classPK Primary Key of the instance of the ClassName
	 * @param columnName Name of the value we want to obtain
	 * @return It returns a Expando Value with the value of the Custom Field
	 */
	public ExpandoValue getCustomField(long companyId, String className, long classPK, String columnName){
		String tableName = "CUSTOM_FIELDS";
		
		ExpandoValue value;
		try {
			value = ExpandoValueLocalServiceUtil.getValue(companyId, className, tableName, columnName, classPK);
		} catch (Exception e) {
			value = null;
		}
		
		return value;
	}
	
	/**
	 * 
	 * Method to obtain the id of a category from its name
	 * 
	 * @param groupId Id of the group where the Category is
	 * @param categoria Name of the category
	 * @param vocabulario Vocabulary where the Category is
	 * @return Long value with the id of the Category
	 */
	public long getCategoryByName(long groupId, String categoria, String vocabulario) {
		long resultado = 0l;
		List<AssetCategory> categories = null;
		long vocabularioId = 0l;
		try {
			categories = AssetCategoryLocalServiceUtil.getCategories();
			vocabularioId = AssetVocabularyLocalServiceUtil.getGroupVocabulary(groupId, vocabulario).getVocabularyId();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (categories != null && (categories.size() > 0)) {
			for(AssetCategory category : categories) {
				if (category.getName().equals(categoria) && (category.getGroupId() == groupId) && (category.getVocabularyId() == vocabularioId) ) {
					resultado = category.getCategoryId();
				}
			}
		}
		return resultado;
	}
	
	/**
	 * Obtiene las categorías de un vocabulario
	 * @param groupId
	 * @param vocabularyName
	 */
	public List<AssetCategory> getVocabularyCategories(long groupId, String vocabularyName){
		List<AssetCategory> categories = new ArrayList<AssetCategory>();
		try{
			AssetVocabulary vocabulary = AssetVocabularyLocalServiceUtil.getGroupVocabulary(groupId, vocabularyName);
			categories = AssetCategoryServiceUtil.getVocabularyRootCategories(vocabulary.getVocabularyId(), QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);
		}
		catch(Exception err){
			_log.debug("Error al recuperar las categorias del Vocabulario " + vocabularyName);
		}
		return categories;
	}
	
		/**
	 * Obtiene las categorías asociadas a un objeto del sistema 
	 * (identificado por su clase y su PrimaryKey)
	 * @param companyId
	 * @param groupId
	 * @param className
	 * @param classPK
	 * @return
	 */
	public List<AssetCategory> getItemCategories(
			long companyId, 
			long groupId, 
			String className, 
			long classPK){
		
		return getItemCategories(companyId, groupId, className, classPK, "");
	}
	
	/**
	 * Obtiene las categorías asociadas a un objeto del sistema 
	 * (identificado por su clase y su PrimaryKey), para un vocabulario concreto
	 * @param companyId
	 * @param groupId
	 * @param className
	 * @param classPK
	 * @param vocabularyName
	 * @return
	 */
	public List<AssetCategory> getItemCategories(
			long companyId, 
			long groupId, 
			String className, 
			long classPK,
			String vocabularyName){
		
		List<AssetCategory> categories = new ArrayList<AssetCategory>();

		try{
			List<AssetCategory> allCategories = AssetCategoryLocalServiceUtil.getCategories(className, classPK);
			
			if (!vocabularyName.equals("")){
				long vocabularyId = AssetVocabularyLocalServiceUtil.getGroupVocabulary(groupId, vocabularyName).getVocabularyId();
			
				for (AssetCategory category : allCategories){
					if (category.getVocabularyId() == vocabularyId) categories.add(category);
				}
			}
			else{
				categories = allCategories;
			}
		}
		catch(Exception e){
			_log.error("Error recovering item categories", e);
		}
		return categories;
	}

	/**
	 * Actualiza las categorías asignadas a un contenido web
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	public void updateWebContentCategories(long groupId, long userId, String articleId, long[] assetCategoryIds, long[] mainAssetCategoryIds) throws PortalException, SystemException 
	{
		CategorizeMgr.updateWebContentCategories(groupId, userId, articleId, assetCategoryIds, mainAssetCategoryIds);
	}
	
	public JournalArticle updtContentCategories(long groupId, long userId, String articleId, long[] assetCategoryIds, long[] mainAssetCategoryIds) throws PortalException, SystemException
	{
		userId = com.liferay.portal.kernel.util.GroupMgr.getDefaultUserId();
		JournalArticle article = JournalArticleLocalServiceUtil.getArticle(groupId, articleId);
		String [] tagNames = AssetTagLocalServiceUtil.getTagNames("com.liferay.portlet.journal.model.JournalArticle", article.getResourcePrimKey());
		JournalArticleLocalServiceUtil.updateAsset(userId, article, assetCategoryIds, tagNames);
		
		if(mainAssetCategoryIds!=null)
		{
			List<Long> mainCategoryIdList = new ArrayList<Long>();
			// Se quitan las categorias marcadas como principales pero que no están en el array assetCategoryIds
			for(long mainCat : mainAssetCategoryIds)
			{
				if( ArrayUtil.contains(assetCategoryIds, mainCat) )
					mainCategoryIdList.add( new Long(mainCat) );
			}
			
			long[] mainCategoryId = ArrayUtil.toArray(mainCategoryIdList.toArray( new Long[mainCategoryIdList.size()] ));
			
			com.protecmedia.iter.news.service.JournalArticleLocalServiceUtil.updateMainCategoriesIds(groupId, article, mainCategoryId);
		}
		
		return article;
	}

	/* ***********************************
	 * 
	 *  FIND
	 * 
	 * ***********************************
	
	/**
	 * Obtiene los contenidos web asociados a las categorias indicadas, con estado vigente en la fecha indicada
	 */
	public List<Object> findCategorizeContent(long scopeGroupId, long contentGroupId, long[] categoryIds, int start, int end, boolean online, Date date) {											
		
		List<Object> obj = new ArrayList<Object>();
		
		try {									
			AssetEntryQuery query = new AssetEntryQuery();

			long[] groupIds = { contentGroupId };				

			query.setGroupIds(groupIds);
			query.setAllCategoryIds(categoryIds);
			query.setOrderByCol1("publishDate");
			query.setStart(start);
			query.setEnd(end);

			List<AssetEntry> results = AssetEntryLocalServiceUtil.getEntries(query);

			//Se comprueba si existe al menos un pageContent asociado valido y se recuperan sus datos
			for (AssetEntry entry : results) {					
				JournalArticle ja = JournalArticleLocalServiceUtil.getLatestArticle(entry.getClassPK());
				PageContent pc = PageContentLocalServiceUtil.getFirstPageContent(scopeGroupId, ja.getArticleId(), online, date);
				if (pc != null)
					obj.add(new Object[]{ja.getArticleId(), pc.getArticleModelId(), pc.getLayoutId()});
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return obj;
	}
	
	/* ***********************************
	 * 
	 *  LINKS
	 * 
	 * ***********************************/
	
	public List<Object> getMetadataRelatedContent(JournalArticle article, long scopeGroupId, int start, int end) {											
		return TeaserContentUtil.getMetadataRelatedContent(article.getGroupId(), article.getArticleId(), article.getStructureId(), scopeGroupId, start, end);
	}
	
	public List<Object> getMetadataRelatedContent(long globalGroupId, String contentId, String structureId, long scopeGroupId, int start, int end) {											
		return TeaserContentUtil.getMetadataRelatedContent(globalGroupId, contentId, structureId, scopeGroupId, start, end);
	}

	/**
	 * @param categoryIds
	 * @return Devuelve la lista con los nombre de un grupo de categorias. Sin repeticiones y sin 
	 * importar si las categorias pertenecen a vocabularios distintos. Ordenados por su jerarquia 
	 * en su arbol de categorias y separados por coma (,).
	 */
	public String getCategoryNames(String categoryIds) 
	{
		String result = "";
		
		List<Object> listNames = PortalLocalServiceUtil.executeQueryAsList( String.format(SEL_CATEGORY_NAMES, categoryIds) );
		if(listNames != null && listNames.size() > 0 && Validator.isNotNull(listNames.get(0)))
			result = listNames.get(0).toString();
		
		return result;
	}
	
	/**
	 * @param categoryIds
	 * @return Devuelve la lista con los nombre de los antecesores de un grupo de categorias. Sin
	 * repeticiones y sin importar si las categorias pertenecen a vocabularios distintos. Ordenados 
	 * por su jerarquia en su árbol de categorias y separados por coma (,).
	 */
	public String getCategoryAncestorNames(String categoryIds) 
	{
		String result = "";
		
		List<Object> listNames = PortalLocalServiceUtil.executeQueryAsList( String.format(SEL_CATEGORY_ANCESTOR_NAMES, categoryIds) );
		if(listNames != null && listNames.size() > 0 && Validator.isNotNull(listNames.get(0)))
			result = listNames.get(0).toString();
		
		return result;
	}
	
	// Asigna el artículo instrumental a la categoría (categoryproperties)
	public void asignArticleAsAboutCategoryArticle(long groupid, long categoryId, String articleId) throws Exception
	{
		ErrorRaiser.throwIfFalse( Validator.isNotNull(groupid), 	IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse( Validator.isNotNull(categoryId), 	IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse( Validator.isNotNull(articleId), 	IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String asignArticleToCategory = new StringBuilder(" INSERT INTO categoryproperties(groupid, categoryid, aboutid) VALUES (%s, %s, '%s') \n").append(
									"\t ON DUPLICATE KEY UPDATE aboutid=VALUES(aboutid) \n").toString();
		
		String query = String.format(asignArticleToCategory, groupid, categoryId, articleId);		
		PortalLocalServiceUtil.executeUpdateQuery(query);
		
		String assetCategorycategoryId = InstrumentalContentUtil.getCategoryIdFromInstrumentalArticle(groupid, articleId);
		if (Validator.isNotNull(assetCategorycategoryId))
		{
			Live categoryLive = LiveLocalServiceUtil.getLiveByLocalId(GroupMgr.getGlobalGroupId(), IterKeys.CLASSNAME_CATEGORY, assetCategorycategoryId);
			categoryLive.setStatus(IterKeys.PENDING);
			LiveLocalServiceUtil.updateLive(categoryLive);				
		}
		
		IterVelocityTools.executeInitAbouCategoryArticles();
	}
	
	public void deleteVocabulary2(long vocabularyId) throws PortalException, SystemException, NoSuchMethodException, SecurityException, ClientProtocolException, DocumentException, IOException, ServiceError
	{
		checkBeforeDelete(vocabularyId, IterKeys.CLASSNAME_VOCABULARY, IterErrorKeys.XYZ_E_VOCABULARY_HAS_CHILDREN_IN_LIVE_ZYX);
		CategorizeMgr.deleteVocabulary(vocabularyId);
	}

	public void deleteCategory2(long categoryId)	throws PortalException, SystemException, NoSuchMethodException, SecurityException, ClientProtocolException, DocumentException, IOException, ServiceError
	{
		checkBeforeDelete(categoryId, IterKeys.CLASSNAME_CATEGORY, IterErrorKeys.XYZ_E_CATEGORY_HAS_CHILDREN_IN_LIVE_ZYX);
		CategorizeMgr.deleteCategory(categoryId);
	}
	
	private void checkBeforeDelete(long nodeId, String classNameValue, String error) throws JSONException, SystemException, NoSuchMethodException, SecurityException, ClientProtocolException, DocumentException, IOException, ServiceError
	{
		String query = String.format(GET_GLOBALID, nodeId, classNameValue);
		_log.debug(query);
		
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList(query);
		
		if( result!= null && result.size()>0 )
		{
			if(result.size()==1)
			{
				Document domResult = null;
				if(classNameValue.equals(IterKeys.CLASSNAME_CATEGORY))
					domResult = CategoriesUtil.getLiveEnvironmentCategoryChildren( String.valueOf(result.get(0)) );
				else
					domResult = CategoriesUtil.getLiveEnvironmentVocabularyChildren( String.valueOf(result.get(0)) );
				
				ErrorRaiser.throwIfFalse( domResult.selectNodes("//row").size()==0, error );
				
				String[] categoriesList = null;
				if(classNameValue.equals(IterKeys.CLASSNAME_CATEGORY))
					categoriesList = XMLHelper.getStringValues(CategoriesUtil.getCategoryChildren(nodeId).selectNodes("//row"), "@categoryid");
				else
					categoriesList = XMLHelper.getStringValues(CategoriesUtil.getVocabularyChildren(nodeId).selectNodes("//row"), "@categoryid");
				
				if(categoriesList.length>0)
				{
					query = String.format(GET_GLOBALID, StringUtil.merge(categoriesList, ","), IterKeys.CLASSNAME_CATEGORY);
					_log.debug(query);
					result = PortalLocalServiceUtil.executeQueryAsList(query);
					query = String.format(COUNT_LIVE_CATEGORIES, StringUtils.join(result.iterator(), "','"), IterKeys.CLASSNAME_CATEGORY);
					_log.debug(query);
					domResult = PortalLocalServiceUtil.executeRemoteQueryAsDom(query);
					long childrenInLive = XMLHelper.getLongValueOf(domResult.selectSingleNode("/rs/row"), "@numchildren");
					ErrorRaiser.throwIfFalse(childrenInLive==0L, error);
				}
			}
			else
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_MORE_THAN_ONE_RESULT_ZYX);
		}
		else
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, "Element with {classNameValue:" + classNameValue + ", localid:"+ nodeId +"} not found.");
	}
	
	/**
	 * 
	 * @param categoryId
	 * @param check
	 * @return
	 * @throws DocumentException
	 */
	/*
	public void fullDeleteCategory(long categoryId, boolean check)
	{
		ErrorRaiser.throwIfFalse(check, IterErrorKeys.XYZ_E_CATEGORY_ELEMENTS_AFECTED_BY_PROCESS_ZYX, errMsg);
	}
	
	public void fullMergeCategories(long[] srcCategories, long dstCategory, boolean check)
	{
	}
	
	public void fullChangeHierarchy(long categoryId, long parentCategoryId, long parentVocabularyId)
	{
	}
	
	public void fullRenameCategory(long categoryId, String title)
	{
	}
	*/
}
