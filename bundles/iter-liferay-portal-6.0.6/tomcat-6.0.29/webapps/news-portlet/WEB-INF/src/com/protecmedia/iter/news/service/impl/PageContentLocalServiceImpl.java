/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/


package com.protecmedia.iter.news.service.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.liferay.counter.service.CounterLocalService;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.comments.CommentsConfigBean;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.metrics.NewsletterMASTools;
import com.liferay.portal.kernel.render.RenditionMode;
import com.liferay.portal.kernel.util.CategoriesUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.IterGlobalKeys;
import com.liferay.portal.kernel.util.IterURLUtil;
import com.liferay.portal.kernel.util.KeyValuePair;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.PHPUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.util.request.PublicIterParams;
import com.liferay.portal.kernel.velocity.VelocityContext;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.kernel.xml.XSLUtil;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.asset.model.AssetCategory;
import com.liferay.portlet.asset.model.AssetCategoryProperty;
import com.liferay.portlet.asset.service.AssetCategoryPropertyLocalServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portlet.journalcontent.util.JournalContentUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.designer.model.PageTemplate;
import com.protecmedia.iter.designer.service.PageTemplateLocalServiceUtil;
import com.protecmedia.iter.news.DuplicatePageContentIdException;
import com.protecmedia.iter.news.JournalArticleNotApprovedException;
import com.protecmedia.iter.news.NoSuchPageContentException;
import com.protecmedia.iter.news.PageContentExistsException;
import com.protecmedia.iter.news.model.PageContent;
import com.protecmedia.iter.news.service.CategorizeLocalServiceUtil;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;
import com.protecmedia.iter.news.service.base.PageContentLocalServiceBaseImpl;
import com.protecmedia.iter.news.service.item.PageContentXmlIO;
import com.protecmedia.iter.news.util.PageContentComparator;
import com.protecmedia.iter.news.util.PageContentDynamicQuery;
import com.protecmedia.iter.news.util.TeaserContentUtil;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.persistence.LiveUtil;

/**
 * @author Protecmedia
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class PageContentLocalServiceImpl extends PageContentLocalServiceBaseImpl {
	
	private static Log _log = LogFactoryUtil.getLog(PageContentLocalServiceImpl.class);
	private static ItemXmlIO itemXmlIO = new PageContentXmlIO();
	
	private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat(WebKeys.URL_PARAM_DATE_FORMAT_FULL);
	
	private static final String INCREASE_PAGECONTENT_ORDEN = new StringBuffer 
			  ("-- Incrementa en n la posición de todos los PageContents de una sección, cuya posición actual sea mayor que la de referencia.\n").
		append("UPDATE News_PageContent SET orden = orden+%d\n").
		append("  WHERE orden > %d\n").
		append("    AND layoutId = '%s'\n").
		append("    AND pageContentId NOT IN (%s)").toString();

	private static final String GET_MAX_PAGECONTENT_ORDEN = new StringBuffer 
			  ("-- Devuelve la posicion maxima que ocupan los PageContents de la seccion de un determinado grupo. Si no hay seria el primero asi que devuelve 0\n").
		append("SELECT IFNULL(MAX(orden),0)\n").
		append("FROM News_PageContent\n").
		append("  WHERE News_PageContent.layoutId = '%s'\n").
		append("    AND News_PageContent.groupId = %d").toString();

	private static final String UPDT_PAGECONTENT_ORDEN = new StringBuffer 
			  ("-- Actualiza el orden de un conjunto PageContents a partir de una posicion de referencia\n").
		append("-- y respetando el orden de aparicion en una lista.\n").
		append("UPDATE News_PageContent SET orden = %d+FIELD(pageContentId, %s)\n").
		append("  WHERE layoutId = '%s' AND pageContentId IN (%s)").toString();

	private static final String UPDT_PAGECONTENT_XMLIOLIVE_STATUS = new StringBuffer 
			  ("-- Se actualiza a 'pending' el XmlIO_Live.status de los PageContents de una sección, cuya posición sea mayor que la de referencia.\n").
		append("UPDATE Xmlio_Live SET Xmlio_Live.status = '").append(IterKeys.PENDING).append("'\n").
		append("  WHERE classNameValue = '").append(IterKeys.CLASSNAME_PAGECONTENT).append("'\n").
		append("  	AND Xmlio_Live.status IN ('").append(IterKeys.DONE).append("','").append(IterKeys.ERROR).append("')\n").		
		append("    AND localId IN (SELECT pageContentId\n").
		append("                    FROM News_PageContent\n").
		append("                    WHERE layoutId = '%s'\n").
		append("  		              AND orden > %d)").toString();

	private static final String UPDT_JOURNALARTICLE_XMLIOLIVE_STATUS = new StringBuffer 
			  ("-- Se actualiza a 'pending' el XmlIO_Live.status de los JournalArticle que contienen a los PageContents de una sección, cuya posición sea mayor que la de referencia.\n").
		append("UPDATE Xmlio_Live SET Xmlio_Live.status = '").append(IterKeys.PENDING).append("'\n").
		append("  WHERE classNameValue = '").append(IterKeys.CLASSNAME_JOURNALARTICLE).append("'\n").
		append("  	AND Xmlio_Live.status IN ('").append(IterKeys.DONE).append("','").append(IterKeys.ERROR).append("')\n").
		append("    AND localId IN (SELECT articleId\n").
		append("                    FROM JournalArticle\n").
		append("                    INNER JOIN News_PageContent ON (contentId = articleId)\n").
		append("                    WHERE layoutId = '%s'\n").
		append("  		              AND orden > %d)").toString();

	
	private static final String SEL_CATEGORYID = new StringBuilder(
		"SELECT AssetCategory.categoryId, AssetCategory.name, AssetVocabulary.vocabularyId, AssetVocabulary.name vocName,			\n").append(
		"		IF( ExpandoValue.data_ IS NULL, FALSE, GET_MAINCATEGORY(ExpandoValue.data_, AssetCategory.categoryId)) mainCategory	\n").append(
		"FROM 																														\n").append(
		"			AssetEntry																										\n").append(
		"INNER JOIN JournalArticle				 ON JournalArticle.resourcePrimKey 			= AssetEntry.classPK 					\n").append(
		"INNER JOIN AssetEntries_AssetCategories ON AssetEntry.entryId 						= AssetEntries_AssetCategories.entryId	\n").append(	
		"INNER JOIN AssetCategory				 ON AssetEntries_AssetCategories.categoryId = AssetCategory.categoryId				\n").append(
		"INNER JOIN AssetVocabulary			 	 ON AssetCategory.vocabularyId = AssetVocabulary.vocabularyId						\n").append(		
		"INNER JOIN ExpandoColumn 				 ON ExpandoColumn.name						='").append(WebKeys.EXPANDO_COLUMN_NAME_MAIN_METADATAS_IDS).append("'\n").append(
		"LEFT  JOIN ExpandoValue 				 ON (    ExpandoValue.columnId	  = ExpandoColumn.columnId 							\n").append(
		"											 AND ExpandoValue.classpk	  = JournalArticle.id_								\n").append(
		"											 AND LENGTH(ExpandoValue.data_) > 0												\n").append(
		"											)																				\n").append(
		"WHERE 																														\n").append(	
		"		JournalArticle.groupId = %d																							\n").append(
		"	AND JournalArticle.articleId = '%s' 																					").toString();
	
	/**
	 * @return Devuelve la máxima posicion que ocupan los PageContents de la seccion de un grupo.
	 * Si dicha sección <b>no</b> existe devuelve -1
	 */
	public int getMaxPageContentOrden(String layoutUuid, long groupId)
	{
		String sql = String.format(GET_MAX_PAGECONTENT_ORDEN, layoutUuid, groupId);
 		List<Object> info = PortalLocalServiceUtil.executeQueryAsList(sql);
		
 		// Si no hay seria el primero asi que devuelve 0
 		int orden = (info.size() > 0) ? Integer.parseInt( info.get(0).toString() ) : 0;
 		return orden;
	}
	
	/*
	 * Add Functions
	 */
	public PageContent insertPageContentArticleModel(String pageContentId, String contentId, long sectionId,
			long groupId, long contentGroupId, String qualificationId, String type, long articleModelId, long pageContentTargetId, 
			int expirationDateYear, int expirationDateMonth, int expirationDateDay,
			int expirationDateHour, int expirationDateMinute,
			int displayDateYear, int displayDateMonth, int displayDateDay,
			int displayDateHour, int displayDateMinute, 
			boolean online, boolean defaultSection) throws SystemException, PortalException, IOException, SQLException
	{
		// Si el elemento de referencia no existe se añade al final 
		int orden = -1;
		Date expirationDate = (new GregorianCalendar(expirationDateYear,
								expirationDateMonth, expirationDateDay, expirationDateHour, expirationDateMinute)).getTime();
		
		Date displayDate 	= (new GregorianCalendar(displayDateYear,
								displayDateMonth, displayDateDay, displayDateHour, displayDateMinute)).getTime();

 		// Se obtiene la posición del elemento de referencia y de paso se comprueba que exista en BBDD
 		String sql = String.format("SELECT orden, layoutId FROM News_PageContent WHERE pageContentId = '%s'", pageContentTargetId);
 		List<Object> targetInfo = PortalLocalServiceUtil.executeQueryAsList(sql);
 		if (targetInfo.size() > 0)
 		{
 			long targetOrder 	= Long.parseLong( ((Object[])targetInfo.get(0))[0].toString() );
 			String targetLayout	= ((Object[])targetInfo.get(0))[1].toString();

 			// El nuevo PageContent tendrá la posición del elemento de referencia
 			orden				= (int) targetOrder;
 			
 			// Se obtiene cuál será la posición a partir de la cuál se moverán los PageContent (se mueve el de referencia pq se insertará delante de él)
 	 		long afterPos	  	= targetOrder-1;
 	 		
 	 		// Se actualiza la posición de los elementos posteriores a la posición de referencia
 	 		sql = String.format(INCREASE_PAGECONTENT_ORDEN, 1, afterPos, targetLayout, "0");
 	 		_log.trace(sql);
 	 		PortalLocalServiceUtil.executeUpdateQuery(sql);
 	 		// Se borra la caché de los beans modificados
 	 		pageContentPersistence.clearCache();
 	 		
 	 		// Se actualiza el XmlIO_Live.Status de todos los PageContents que hayan modificado su posición (los que se movian, y aquellos que han caido detrás de los movidos)
 	 		sql = String.format(UPDT_PAGECONTENT_XMLIOLIVE_STATUS, targetLayout, afterPos);
 	 		_log.trace(sql);
 	 		PortalLocalServiceUtil.executeUpdateQuery(sql);
 	 		
 	 		// Se actualiza el XmlIO_Live.Status de todos los JournalArticles de los PageContents que hayan modificado su posición (los que se movian, y aquellos que han caido detrás de los movidos)
 	 		sql = String.format(UPDT_JOURNALARTICLE_XMLIOLIVE_STATUS, targetLayout, afterPos);
 	 		_log.trace(sql);
 	 		PortalLocalServiceUtil.executeUpdateQuery(sql);
 	 		
 	 		// Se borra la caché de los beans modificados
 	 		// IterCacheUtil.clearCache("com.protecmedia.iter.xmlio.model.impl.LiveImpl", "com.protecmedia.iter.xmlio.model.impl.LiveImpl.List");
 	 		LiveUtil.clearCache();
 		}

 		return addPageContent(pageContentId, contentId, sectionId, groupId, contentGroupId, qualificationId, type, 
					 articleModelId, orden, displayDate, expirationDate, online, defaultSection);
	}

	public PageContent addPageContent(String contentId, long sectionId,
			long groupId, long contentGroupId, String qualificationId, String type,
			int expirationDateYear, int expirationDateMonth, int expirationDateDay, 
			int expirationDateHour, int expirationDateMinute,
			int displayDateYear, int displayDateMonth, int displayDateDay,
			int displayDateHour, int displayDateMinute) throws SystemException, PortalException {
		
		Date expirationDate = (new GregorianCalendar(expirationDateYear,
				expirationDateMonth, expirationDateDay, expirationDateHour, expirationDateMinute)).getTime();
		Date displayDate = (new GregorianCalendar(displayDateYear,
				displayDateMonth, displayDateDay, displayDateHour, displayDateMinute)).getTime();
		
		return addPageContent("", contentId, sectionId, groupId, contentGroupId, qualificationId, type, -1, 1, displayDate, expirationDate, true);
	}

	//Utilizada por MILENIUM
	public PageContent addPageContent(String pageContentId, String contentId, long sectionId,
			long groupId, long contentGroupId, String qualificationId, String type, long articleModelId, int orden,
			int expirationDateYear, int expirationDateMonth, int expirationDateDay, 
			int expirationDateHour, int expirationDateMinute,
			int displayDateYear, int displayDateMonth, int displayDateDay, 
			int displayDateHour, int displayDateMinute,
			boolean online, boolean defaultSection) throws SystemException, PortalException 
	{
		Date expirationDate = (new GregorianCalendar(expirationDateYear,
								expirationDateMonth, expirationDateDay, expirationDateHour, expirationDateMinute)).getTime();
		Date displayDate 	= (new GregorianCalendar(displayDateYear,
								displayDateMonth, displayDateDay, displayDateHour, displayDateMinute)).getTime();
		
		// El orden de MLN se ignora, siempre se añade al final
		orden = -1;
		return addPageContent(pageContentId, contentId, sectionId, groupId, contentGroupId, qualificationId, type, 
							 articleModelId, orden, displayDate, expirationDate, online, defaultSection);
	}	
	
	public PageContent addPageContent(String pageContentId, String contentId, long sectionId,
			long groupId, long contentGroupId, String qualificationId, String type, long articleModelId, int orden,
			Date displayDate, Date expirationDate, boolean online) throws SystemException, PortalException {

		return addPageContent(pageContentId, contentId, sectionId, groupId, contentGroupId, qualificationId, type, articleModelId, orden,
				displayDate, expirationDate, online, false);

	}
	
	public PageContent addPageContent(String pageContentId, String contentId, long sectionId,
			long groupId, long contentGroupId, String qualificationId, String type, long articleModelId, int orden,
			Date displayDate, Date expirationDate, boolean online, boolean defaultSection) throws SystemException, PortalException {

		String 	pageUuid = LayoutLocalServiceUtil.getLayout(groupId, false, sectionId).getUuid();		
		
		long id = counterLocalService.increment();	
		
		pageContentId.trim().toUpperCase();
		if (pageContentId.equals("")) 
		{
			pageContentId = String.valueOf(id);
		}
		
		if (orden < 0)
		{
			orden = getMaxPageContentOrden(pageUuid, groupId)+1;
		}
		
		validate(groupId, pageContentId, contentGroupId, contentId, pageUuid, type);

		try 
		{
			if( PropsValues.ITER_SHARED_ARTICLES_ENABLED )
				JournalArticleLocalServiceUtil.addScopegroupToArticle(groupId, contentId);
			
			PageContent pageContent = pageContentPersistence.create(id);
			pageContent.setPageContentId(pageContentId);
			pageContent.setContentId(contentId);
			pageContent.setContentGroupId(contentGroupId);
			pageContent.setLayoutId(pageUuid);
			pageContent.setGroupId(groupId);		
			pageContent.setTypeContent(type);
			pageContent.setVigenciadesde(displayDate);
			pageContent.setVigenciahasta(expirationDate);
			pageContent.setOrden(orden);		
			pageContent.setArticleModelId(articleModelId<=0?-1:articleModelId);
			pageContent.setModifiedDate(new Date());
			pageContent.setQualificationId(qualificationId);
			pageContent.setOnline(online);
			pageContent.setDefaultSection(false);
			
			pageContentPersistence.update(pageContent, false);			
		
			//Este metodo hace la inserción en Live
			setDefaultPageContent(groupId, id, defaultSection);
				
			JournalContentUtil.reindexContent(GroupMgr.getGlobalGroupId(), pageContent.getContentId());
			
			return pageContent;
		} 
		catch (Exception e) 
		{
			_log.error("Unable to create the assign", e);
			return null;
		}
	}

	private void validate(long groupId, String pageContentId, long globalId, String contentId, String layoutId, String type)
			throws SystemException, PortalException {
						
		JournalArticle article = JournalArticleLocalServiceUtil.getArticle(globalId, contentId);
		if (article!= null && !article.isApproved()) {
			throw new JournalArticleNotApprovedException();
		}		
		
		PageContent pageContent = pageContentPersistence.fetchByGroupPagecontentId(groupId, pageContentId);

		if (pageContent != null) {
			throw new DuplicatePageContentIdException();
		}
		
		PageContent page = pageContentPersistence.fetchByG_A_L(groupId, layoutId, contentId, type);

		if (page != null) {
			throw new PageContentExistsException();
		}

	}

	/*
	 * 
	 * Get Functions
	 */
	public PageContent getPageContent(String contentId, long layoutId,
			long groupId, String type) throws SystemException {
		
		String 	pageUuid = "";
		try {
			pageUuid = LayoutLocalServiceUtil.getLayout(groupId, false, layoutId).getUuid();
		} catch (Exception e) {_log.error(e);}
				
		PageContent page = pageContentPersistence.fetchByG_A_L(groupId, pageUuid, contentId, type);

		return page;
	}	
	
	public PageContent getPageContent(long id) throws SystemException {
		PageContent page = pageContentPersistence.fetchByPrimaryKey(id);

		return page;
	}
	
	public List<PageContent> getPageContents(long groupId, String contentId) throws SystemException {
		return pageContentPersistence.findByGroupContentIdFinder(groupId, contentId);
	}
		
	public List<PageContent> getPageContents(long groupId) throws SystemException {
		return pageContentPersistence.findByGroupFinder(groupId);
	}
	
	public List<PageContent> getPageContentsByContentId(String contentId) throws SystemException 
	{
		return pageContentPersistence.findByContentIdFinder(contentId);
	}
	
	
	public PageContent getPageContentByContentIdLayoutId(long groupId, String contentId, String layoutId) throws SystemException{
		return pageContentPersistence.fetchByGroupContentIdLayoutId(groupId, contentId, layoutId);
	}
	
	public PageContent getDefaultPageContentByContentId(long groupId, String contentId) throws SystemException{
		return pageContentPersistence.fetchByGroupContentIdDefaultSection(groupId, contentId, true);
	}
	
	/*
	 * 
	 * Update Functions
	 */
	public PageContent updatePageContent(long id_, long layoutId) throws SystemException, PortalException 
	{
		PageContent page = pageContentPersistence.fetchByPrimaryKey(id_);

		if (page == null) 
			throw new NoSuchPageContentException();

		String pageUuid = LayoutLocalServiceUtil.getLayout(page.getGroupId(), false, layoutId).getUuid();

		page.setLayoutId(pageUuid);
		page.setModifiedDate(new Date());
		
		PageContent pageUpdate = pageContentPersistence.update(page, false);

		//Insercion en la tabla de Live
		itemXmlIO.createLiveEntry(page);
		
		return pageUpdate;
	}
	
	//Utilizada por MILENIUM
	public PageContent updatePageContentArticleModel(long id_, long articleModelId) throws SystemException, PortalException 
	{
		PageContent page = pageContentPersistence.fetchByPrimaryKey(id_);
		
		if (page == null) 
			throw new NoSuchPageContentException();
		
		page.setArticleModelId(articleModelId<=0?-1:articleModelId);
		page.setModifiedDate(new Date());
		
		PageContent pageUpdate = pageContentPersistence.update(page, false);
		
		//Insercion en la tabla de Live
		itemXmlIO.createLiveEntry(page);
		
		return pageUpdate;
	}

	public PageContent updatePageContent(int expirationDateYear,
										int expirationDateMonth, int expirationDateDay, 
										int expirationDateHour, int expirationDateMinute,
										int displayDateYear, int displayDateMonth, int displayDateDay,
										int displayDateHour, int displayDateMinute,
										long id_)  throws SystemException, PortalException
	{
		PageContent page = pageContentPersistence.fetchByPrimaryKey(id_);

		if (page == null) 
			throw new NoSuchPageContentException();
		
		Date expirationDate = (new GregorianCalendar(expirationDateYear, expirationDateMonth, expirationDateDay, expirationDateHour, expirationDateMinute)).getTime();
		Date displayDate = (new GregorianCalendar(displayDateYear, displayDateMonth, displayDateDay, displayDateHour, displayDateMinute)).getTime();
		
		page.setModifiedDate(new Date());

		page.setVigenciadesde(displayDate);
		page.setVigenciahasta(expirationDate);
		
		PageContent pageUpdate = pageContentPersistence.update(page, false);
		
		//Insercion en la tabla de Live
		itemXmlIO.createLiveEntry(pageUpdate);
		
		JournalContentUtil.reindexContent(GroupMgr.getGlobalGroupId(), pageUpdate.getContentId());
		
		return pageUpdate;
	}
	
	//Utilizado por MILENIUM
	public PageContent updatePageContent(String qualificationId, long id_) throws SystemException, PortalException 
	{
		PageContent page = pageContentPersistence.fetchByPrimaryKey(id_);

		if (page == null) 
			throw new NoSuchPageContentException();

		page.setQualificationId(qualificationId);
		page.setModifiedDate(new Date());

		PageContent pageUpdate = pageContentPersistence.update(page, false);

		//Insercion en la tabla de Live
		itemXmlIO.createLiveEntry(page);
		
		return pageUpdate;
	}

	public PageContent updatePageContent(String qualificationId,
										int expirationDateYear, int expirationDateMonth, int expirationDateDay, 
										int expirationDateHour, int expirationDateMinute,
										int displayDateYear, int displayDateMonth,int displayDateDay,
										int displayDateHour, int displayDateMinute,
										long id_) throws SystemException, PortalException 
	{
		PageContent page = pageContentPersistence.fetchByPrimaryKey(id_);

		if (page == null) 
			throw new NoSuchPageContentException();
		
		Date expirationDate = (new GregorianCalendar(expirationDateYear, expirationDateMonth, expirationDateDay, expirationDateHour, expirationDateMinute)).getTime();
		Date displayDate = (new GregorianCalendar(displayDateYear, displayDateMonth, displayDateDay, displayDateHour, displayDateMinute)).getTime();
		
		page.setQualificationId(qualificationId);
		page.setModifiedDate(new Date());

		page.setVigenciadesde(displayDate);
		page.setVigenciahasta(expirationDate);
		
		PageContent pageUpdate = pageContentPersistence.update(page, false);
		
		//Insercion en la tabla de Live
		itemXmlIO.createLiveEntry(pageUpdate);
		
		return pageUpdate;		
	}
	
	//Utilizado por MILENIUM
	public PageContent updatePageContent(int online, long articleModelId, String qualificationId,
										int expirationDateYear, int expirationDateMonth, int expirationDateDay, 
										int expirationDateHour, int expirationDateMinute,
										int displayDateYear, int displayDateMonth, int displayDateDay, 
										int displayDateHour, int displayDateMinute,
										long id_) throws SystemException, PortalException 
	{
		Date expirationDate = (new GregorianCalendar(expirationDateYear, expirationDateMonth, expirationDateDay, expirationDateHour, expirationDateMinute)).getTime();
		
		Date displayDate = (new GregorianCalendar(displayDateYear, displayDateMonth, displayDateDay, displayDateHour, displayDateMinute)).getTime();
		
		return updatePageContent(online, articleModelId, qualificationId, expirationDate, displayDate, id_);
	}
	
	public PageContent updatePageContent(int online, long articleModelId, String qualificationId,
										 Date expirationDate, Date displayDate, long id_) throws SystemException, PortalException 
	{
		return updatePageContent(online, articleModelId, qualificationId, expirationDate, displayDate, id_, false, -1);
	}
	
	public PageContent updatePageContent(int online, long articleModelId, String qualificationId, Date expirationDate, 
										 Date displayDate, long id_, boolean defaultSection, int orden) throws SystemException, PortalException 
	{
		PageContent page = pageContentPersistence.fetchByPrimaryKey(id_);

		if (page == null) 
		{
			throw new NoSuchPageContentException();
		}

		if (orden >= 0)
			page.setOrden(orden);
		
		page.setQualificationId(qualificationId);
		page.setModifiedDate(new Date());

		page.setVigenciadesde(displayDate);
		page.setVigenciahasta(expirationDate);
		page.setArticleModelId(articleModelId<=0?-1:articleModelId);
		
		if (online != -1)
		{
			page.setOnline(online == 1);
		}
		
		PageContent pageUpdate = pageContentPersistence.update(page,
				false);
		
		setDefaultPageContent(pageUpdate.getGroupId(), id_, defaultSection);
				
		return pageUpdate;
	}
	
	public void setDefaultPageContent (long groupId, long id_, boolean defaultSection) throws SystemException, PortalException
	{
		// ITER-683 Problemas al editar una nota de la Web y cambiarle la sección predeterminada
		// Se modifica la recuperación del PageContent por id_ en lugar de PageContentId.
		PageContent pageContent = pageContentPersistence.fetchByPrimaryKey(id_);
		
		if (defaultSection == true)
		{
			//Get pageContents with the same article
			List<PageContent> pageContentList = PageContentLocalServiceUtil.getPageContents(groupId, pageContent.getContentId());
		
			//Can be only one default per article
			for(PageContent pc : pageContentList)
			{
				if (pc.getId() != pageContent.getId() && pc.getDefaultSection() == true)
				{
					pc.setDefaultSection(false);					
					pageContentPersistence.update(pc, false);	
					
					//Insercion en la tabla de Live
					itemXmlIO.createLiveEntry(pc);
				}					
			}			
		}
		
		//Set defaultSection
		pageContent.setDefaultSection(defaultSection);
		
		pageContentPersistence.update(pageContent, false);	
		
		//Insercion en la tabla de Live
		itemXmlIO.createLiveEntry(pageContent);
	}
	
	/*
	 * Delete functions
	 */
	public void deletePageContentByArticle(long groupId, String contentId) throws SystemException, PortalException 
	{
		List<PageContent> pageContents = pageContentPersistence.findByGroupContentIdFinder(groupId, contentId);
		
		for (PageContent pageContent : pageContents) 
		{
			try 
			{
				removePageContent(pageContent);
			} 
			catch (NoSuchPageContentException e) 
			{
				_log.error(e);
			}
		}		
	}
	
	public void deletePageContentByContentId(String articleId, boolean deleteJustNow) throws SystemException, PortalException 
	{
		List<PageContent> pageContents = pageContentPersistence.findByContentIdFinder(articleId);
		
		for (PageContent pageContent : pageContents) 
		{
			try 
			{
				removePageContent(pageContent, deleteJustNow);
			} 
			catch (NoSuchPageContentException e) 
			{
				_log.error(e);
			}
		}
	}


	//Utilizada por MILENIUM
	public void deletePageContent(long pageContentId) throws Exception{
		
		try
		{
			PageContent pageContent = pageContentPersistence.fetchByPrimaryKey(pageContentId);
			removePageContent(pageContent);

			JournalContentUtil.reindexContent(GroupMgr.getGlobalGroupId(), pageContent.getContentId());
		}
		catch (Exception e)
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_NO_SUCH_PAGECONTENT_ZYX, "NO PAGECONTENT EXISTS WITH THE PRIMARY KEY "+String.valueOf(pageContentId));
		}
		
	}

	public void deletePageContent(long groupId, String contentId, long layoutId, String type) throws PortalException, SystemException 
	{
		String pageUuid = LayoutLocalServiceUtil.getLayout(groupId, false, layoutId).getUuid();				
		
		PageContent pageContent = pageContentPersistence.findByG_A_L(groupId, pageUuid, contentId, type);
		
		removePageContent(pageContent);

	}

	public void deletePageContent(long groupId, String layoutId) throws SystemException, PortalException 
	{
		List<PageContent> pageContents = pageContentPersistence.findByPageContentLayoutFinder(groupId, layoutId);
		
		for (PageContent pageContent : pageContents) 
		{
			try 
			{
				removePageContent(pageContent);
			} 
			catch (NoSuchPageContentException e) 
			{
				_log.error(e);
			}
		}
	}
	
	private void removePageContent(PageContent pageContent) throws SystemException, PortalException 
	{
		removePageContent(pageContent, false);
	}
	
	private void removePageContent(PageContent pageContent, boolean deleteJustNow) throws SystemException, PortalException 
	{
		if (pageContent != null)
		{
			long scopeGroupId = pageContent.getGroupId();
			String articleId = pageContent.getContentId();
			
			// Borramos el pageContent
			pageContentPersistence.remove(pageContent);
			
			if( PropsValues.ITER_SHARED_ARTICLES_ENABLED )
			{
				List<PageContent> pageContentsInGroup = getPageContents(scopeGroupId, articleId);
				if(pageContentsInGroup.size()==0)
					JournalArticleLocalServiceUtil.removeScopegroupFromArticle(scopeGroupId, articleId);
			}
			
			try 
			{
				// Gestión la tabla Xmlio_Live
				itemXmlIO.deleteLiveEntry(pageContent, deleteJustNow);
			} 
			catch (PortalException e) 
			{
				_log.error(e);
			}
		}
		else
		{
			throw new NoSuchPageContentException();
		}
	}
	
	/*
	 * Content Visibility Control
	 * 
	public void evaluateVisibility(long groupId, String contentId){
		
		 // Obtener los PageContent asociados a este contenido de tipo visible y online. Si hay al menos uno, el articulo tiene estado APROBADO
		 // Si no tiene ninguno pero hay pageContent asociados, el estado se deja a DRAFT
		 // Si no hay absolutamente ninguno se borra. Cuidado con el timing! (cuando se crea por primera vez no hay PC pero no debe
		 // tratarse igual...). SÓLO SI LA ESTRUCTURA ES DE NUESTROS TIPOS
		 
	}
	*/
		
	/*
	 * Advanced Search Functions
	 */
	
	/**
	 * Busca contenidos asociados a un solo layout
	 */
	public List<Object> findPageContent(
			long groupId,
			String layoutId, 
			String qualificationId, 
			String typeContent, 
			int order, 
			int orderType,
			int start, 
			int end, 
			Date date) throws PortalException, SystemException {
			String [] layoutIds;
			if (!(layoutId==null) && !layoutId.equals("")){
				layoutIds = null;
			}
			else{
				layoutIds = new String[]{layoutId};
			}
			return findPageContent(groupId, layoutIds, qualificationId, typeContent, order, orderType, start, end, date);
	}
	
	public List<Object> findPageContent(
		long groupId,
		String[] layoutId, 
		String qualificationId, 
		String typeContent, 
		int order, 
		int orderType, 
		int start, 
		int end,
		Date date) throws PortalException, SystemException 
		{
			return findPageContent(groupId, layoutId, null, qualificationId, typeContent, order, orderType, start, end, date);
	}
	
	//Usado para sacar todos los articulos filtrados por categorias (devuelve lista de objetos de 4 entradas)
	public List<Object> findPageContent(
			long groupId,
			long[] categoryId,
			String typeContent, 
			int order, 
			int orderType, 
			int start, 
			int end,
			Date date) throws PortalException, SystemException {
		return findPageContent(groupId, null, categoryId, "", typeContent, order, orderType, start, end, date);
	}
	
	public List<Object> findPageContent(
			long groupId,
			String[] layoutId, 
			long[] categoryId,
			String qualificationId, 
			String typeContent, 
			int order, 
			int orderType, 
			int start, 
			int end,
			Date date) throws PortalException, SystemException{
				return findPageContent(groupId, layoutId, categoryId, qualificationId, typeContent, order, -1, orderType, start, end, date);
	}
	
	public List<Object> findPageContent(
			long groupId,
			String[] layoutId, 
			long[] categoryId,
			String qualificationId, 
			String typeContent, 
			int order, 
			int operation,
			int orderType, 
			int start, 
			int end,
			Date date) throws PortalException, SystemException {
		
		return findPageContent(groupId, layoutId, -1, categoryId, -1, qualificationId, typeContent, order, operation, orderType, start, end, date);
		
	}
	
	public List<Object> findPageContent(
			long groupId,
			String[] layoutId,
			int layoutOp,
			long[] categoryId,
			int categoryOp,
			String qualificationId, 
			String typeContent, 
			int order, 
			int operation,
			int orderType, 
			int start, 
			int end,
			Date date) throws PortalException, SystemException {
	
		return findPageContent(groupId, layoutId, -1, categoryId, -1, qualificationId, typeContent, order, operation, orderType, start, end, date, null);
		
	}
	
	public List<Object> findPageContent(
			long groupId,
			String[] layoutId,
			int layoutOp,
			long[] categoryId,
			int categoryOp,
			String qualificationId, 
			String typeContent, 
			int order, 
			int operation,
			int orderType, 
			int start, 
			int end,
			Date date,
			Date modifiedDate) throws PortalException, SystemException {

		return findPageContent(groupId, layoutId, layoutOp, categoryId, categoryOp, qualificationId, typeContent, new int[]{order}, operation, orderType, start, end, date, modifiedDate);
		
	}
	
	/**
	 * Busca contenidos asociados a varios layouts
	 */
	/*
		Construir una query de este tipo

		select pc.contentId from News_pagecontent pc, journalArticle ja, assetEntry ae, assetEntries_assetCategories aeac, News_Counters ncou where
		pc.contentId = ja.articleId and
		ja.resourcePrimKey = ae.classpk and
		ae.classnameId = 10083 and
		aeac.entryId = ae.entryId and 
		aeac.categoryId in (37343) and 
		ncou.contentId = pc.contentId and
		ncou.operation = 0
		orderBy ncou.counter DESC;
		
		NUEVO:
		Para poder combinar criterios de ordenación por estadísticos (que pueden ser nulos para algunos artículos) se usa
		
		select ja.articleid from journalarticle ja left join news_counters nc on ja.articleid =  nc.contentid order by coalesce(nc.counter, 0) DESC, ja
		.articleId DESC;
	
	*/
	@Deprecated
	public List<Object> findPageContent(
			long groupId,
			String[] layoutId,
			int layoutOp,
			long[] categoryId,
			int categoryOp,
			String qualificationId, 
			String typeContent, 
			int order[], 
			int operation,
			int orderType, 
			int start, 
			int end,
			Date date,
			Date modifiedDate) throws PortalException, SystemException {

		DateFormat df = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_00);
		PageContentDynamicQuery acdq = new PageContentDynamicQuery();
		StringBuffer sql = new StringBuffer();
		
		boolean hasOperation = (operation != -1);
		boolean hasCategory = (categoryId != null && categoryId.length>0);
		boolean hasModifiedDate = (modifiedDate != null);
		
		//FIELDS AND JOINS
		sql.append("select pc.contentId, pc.articleModelId, pc.layoutId, pc.contentGroupId");
		
		if (hasOperation)
		{
			switch(operation)
			{
				case IterKeys.OPERATION_COMMENT:
					sql.append(",(SELECT IFNULL(COUNT(*),0) FROM News_Comments m1 WHERE m1.contentId=ja.articleId AND m1.active_=TRUE) comentarios");
					break;
				case IterKeys.OPERATION_RATINGS:
					sql.append(",(SELECT IFNULL((c2.value/c2.counter),0) FROM News_Counters c2 WHERE ja.articleId=c2.contentId AND operation=" + IterKeys.OPERATION_RATINGS + ") ratings");
				case IterKeys.OPERATION_VIEW:
					sql.append(",(SELECT IFNULL(SUM(c3.counter),0) FROM News_Counters c3 WHERE ja.articleId=c3.contentId AND operation=" + IterKeys.OPERATION_VIEW + ") visitas");
			}
		}
		
		sql.append(" from News_PageContent pc USE INDEX (XYZ_ITR_IX_NEWS_PAGECONTENT_VIGHASTA_ZYX), JournalArticle ja");
		
		if (hasCategory)
			sql.append(", AssetEntry ae, AssetEntries_AssetCategories aeac");
		
		//CONDITIONS
		sql.append(" where pc.groupId = " + groupId + " and pc.contentId = ja.articleId");
		
		if (!typeContent.equalsIgnoreCase(""))
			sql.append(" and pc.typeContent = '" + typeContent + "'");
		
		if (layoutId != null && layoutId.length>0)
			sql.append(" " + getSQLInCondition(layoutId, String.class, "pc.layoutId", layoutOp));
		
		if (!qualificationId.equalsIgnoreCase(""))
			sql.append(" and pc.qualificationId = '" + qualificationId + "'");
		
		if (date != null)
		{
			sql.append(" and pc.vigenciadesde < '" + df.format(date) + "'");
			sql.append(" and pc.vigenciahasta > '" + df.format(date) + "'");
		}
		
		if (hasModifiedDate)
			sql.append(" and ja.modifiedDate > '" + df.format(modifiedDate) + "'");
		
		if (hasCategory)
		{
			sql.append(" and ja.resourcePrimKey = ae.classPK");
			sql.append(" and aeac.entryId = ae.entryId");
			sql.append(" and ae.classNameId = " + ClassNameLocalServiceUtil.getClassNameId(IterKeys.CLASSNAME_JOURNALARTICLE));
			sql.append(" " + getSQLInCondition(ArrayUtils.toObject(categoryId), Long.class, "aeac.categoryId", categoryOp));
		}
		
		sql.append(" and pc.online_ = true");
		
		// La comprobación de la tabla LIVE era para descartar elementos que estuviesen inconsistentes, que tras una importación se hubiese importado correctamente
		// el artículo pero NO alguna de sus dependencias. Se quita pq es para un caso puntual y penaliza al 99%, que no hay elementos inconsistentes.
		//Condicion de visibilidad: si el elemento se ha creado de forma errónea no se muestra
		// sql.append(" and not exists (select lv.localId from Xmlio_Live lv where lv.localId = pc.contentId and lv.classNameValue = '" + IterKeys.CLASSNAME_JOURNALARTICLE + "' and lv.status = '" + IterKeys.INTERRUPT + "')");
		
		//ORDER
		sql.append(" group by pc.contentId");
		sql.append(" order by");
		
		String sqlOrderType = ((orderType == IterKeys.ORDER_ASC) ? "asc" : "desc");
		boolean firstOrderClause = true;
		
		//Operaciones estadística
		if (hasOperation && firstOrderClause)
		{
			getSQLOrderClauseByOperation(operation, sqlOrderType, sql, firstOrderClause);
			firstOrderClause= false;
		}
		
		//Propiedades del artículo
		if(firstOrderClause)
		{
			for (int orderKey : order)
			{
				getSQLOrderClause(orderKey, sqlOrderType, sql, firstOrderClause);
				firstOrderClause= false;
			}
		}
		
		if(firstOrderClause)
		{
			sql.append(" ja.displayDate " + sqlOrderType); 
		}
		
		sql.append(", ja.articleId DESC");
		
		//RANGE
		sql.append(" limit " + start + ", " + end);
		
		
		//RESULT
		_log.debug(sql.toString());
		
		List<Object> listResult = acdq.executeQuery(sql.toString());

		return listResult;
	}
	
	private void getSQLOrderClause(int orderKey, String sqlOrderType, StringBuffer sql, boolean first){

		if(!first) sql.append(",");
		
		switch(orderKey){
			case IterKeys.ORDER_ORDEN:
				sql.append(" pc.orden");
				break;
			case IterKeys.ORDER_EXPIRATIONDATE:
				sql.append(" max(pc.vigenciahasta)");
				break;
			case IterKeys.ORDER_STARTDATE:
				sql.append(" min(pc.vigenciadesde)");
				break;
			case IterKeys.ORDER_DISPLAYDATE:
				sql.append(" ja.modifiedDate");
				break;
			case IterKeys.ORDER_MODIFICATIONDATE:
				sql.append(" ja.modifiedDate");
				break;
			default:
				sql.append(" ja.displayDate");
		}
		
		sql.append(" " + sqlOrderType);
		
	}
	
	private void getSQLOrderClauseByOperation(int operationKey, String sqlOrderType, StringBuffer sql, boolean first){
		
		if(!first) sql.append(",");
		
		switch(operationKey){
			case IterKeys.OPERATION_COMMENT:
				sql.append(" comentarios");
				break;
			case IterKeys.OPERATION_RATINGS:
				sql.append(" ratings");
				break;
			default:
				sql.append(" visitas");
		}
		
		sql.append(" " + sqlOrderType);
		
	}
	
	@SuppressWarnings("rawtypes")
	private String getSQLInCondition(Object items[], Class classType, String field, int condition){
		String sqlInCondition = "";
		String AP = classType.equals(String.class) ? "'" : "";
		StringBuffer result;
		
		//TODO: Incluir combinaciones como NOT_AND, NOT_OR, visto como "external operation"
		switch (condition){
			//AND
			/*
			case IterKeys.LOGIC_AND:
				break;
				*/
			//OR
			default:
				result = new StringBuffer();
				result.append("and " + field + " in (");
				result.append(AP + items[0] + AP);
				for (int i = 1; i < items.length; i++) {
					result.append(", " + AP + items[i] + AP);
				}
				result.append(")");
				sqlInCondition = result.toString();
				break;
		}
		return sqlInCondition;
	}
	
	/**
	 * Comprueba si una asignacion contenido pagina existe
	 */
	public boolean findPageContentExist(String contentId, String layoutId,
			long groupId, String type) throws SystemException {

		PageContent page = pageContentPersistence.fetchByG_A_L(groupId, layoutId, contentId, type);

		if (page != null) {
			return true;
		}

		return false;

	}

	public List<PageContent> findPageGroupFinder(long groupId)
			throws SystemException {
		return pageContentPersistence.findByGroupFinder(groupId);
	}

	public List<PageContent> findPageLayoutFinder(String layoutId, long groupId)
			throws SystemException {
		if (layoutId == "")
			return null;
		return pageContentPersistence.findByPageContentLayoutFinder(groupId, layoutId);
	}		
	
	/**
	 * Obtiene el PageContent a partir de su Id
	 */
	public PageContent getPageContent(long groupId, String pageContentId) throws NoSuchPageContentException, SystemException{
		return pageContentPersistence.findByGroupPagecontentId(groupId, pageContentId);
	}
	
	/**
	 * Obtiene un listado de PageContent a partir de los ids de grupo, contenido y modelo
	 */
	public List<PageContent> findPageContentsByModel(long groupId, String contentId, long modelId) throws SystemException{
		return pageContentPersistence.findByGroupContentModel(groupId, contentId, modelId);
	}
	
	/**
	 * Obtiene el primer PageContent activo y vigente del contenido en el grupo especificado en la fecha actual
	 */
	public PageContent getFirstPageContent(long groupId, String contentId) throws SystemException {
		Date date = new Date();
		boolean online = true;
		return getFirstPageContent(groupId, contentId, online, date);
	}	
	
	/**
	 * Obtiene el primer PageContent vigente del contenido en el grupo especificado en la fecha actual y el estado indicado
	 */
	public PageContent getFirstPageContent(long groupId, String contentId, boolean online) throws SystemException {
		Date date = new Date();
		return getFirstPageContent(groupId, contentId, online, date);
	}
	
	/**
	 * Obtiene el primer PageContent del contenido en el grupo especificado, en el estado y la fecha indicados
	 */
	public PageContent getFirstPageContent(long groupId, String contentId, boolean online, Date date) throws SystemException {
		OrderByComparator obc = new PageContentComparator();
		try{
			//Esta forma deberia funcionar pero devuelve null
			return pageContentPersistence.findByVisiblePageContent_First(groupId, contentId, online, date, date, obc);
		}	
		catch(Exception e1){
			try{
				return pageContentPersistence.findByVisiblePageContent(groupId, contentId, online, date, date).get(0);
			}
			catch(Exception e2){
				return null;
			}
		}
	}
	
	public PageContent getFirstPageContent(String contentId, boolean online, Date date) throws SystemException 
	{
		OrderByComparator obc 	= new PageContentComparator();
		PageContent pagContent 	= null;
		
		try
		{
			pagContent = pageContentPersistence.findByVisiblePageContentAnyGrp_First(contentId, online, date, date, obc);
		}	
		catch(Exception e1)
		{
			try
			{
				pagContent = pageContentPersistence.findByVisiblePageContentAnyGrp(contentId, online, date, date).get(0);
			}
			catch(Exception e2)
			{
				_log.trace(e2.toString());
			}
		}
		return pagContent;
	}

	/**
	 * Permite realizar una busqueda inversa: a partir del articulo, el grupo y el modelo, obtiene el PageContent del que procede
	 * @param groupId
	 * @param contentId
	 * @param layoutId
	 * @return
	 */
	public PageContent getFirstPageContentByModel(long groupId, String contentId, long layoutPlid){
		try{
			//Primero se prueba con el modelo que corresponde 
			PageTemplate pageTemplate = PageTemplateLocalServiceUtil.getPageTemplateByLayoutId(groupId, layoutPlid);
			List<PageContent> pageContents = findPageContentsByModel(groupId, contentId, pageTemplate.getId());
			return pageContents.get(0);
		}
		catch(Exception e1){
			//Si no, se prueba con los casos por defecto (alguno tiene que tener! :D)
			try{
				List<PageContent> pageContents = findPageContentsByModel(groupId, contentId, 0);
				return pageContents.get(0);
			}
			catch(Exception e2){
				try{
					List<PageContent> pageContents = findPageContentsByModel(groupId, contentId, -1);
					return pageContents.get(0);
				}
				catch(Exception e3){
					return null;
				}
			}
		}
	}
	
	public boolean increaseOrderPageContent(long pageContentId)	throws SystemException 
	{
		// Son llamados desde la interfaz de Iter, se mantiene el punto de entrada por si interesa en el futuro.
		return true;
	}

	public boolean decreaseOrderPageContent(long pageContentId) throws SystemException 
	{
		// Son llamados desde la interfaz de Iter, se mantiene el punto de entrada por si interesa en el futuro.
		return true;
	}
	
	
	public void reorderPageContent(long groupId, String xml) throws DocumentException, SystemException, PortalException
	{		
		boolean checkPrefix = GetterUtil.getBoolean( PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_NEWS_PAGECONTENT_ENABLE_ORDER), true );
		if (checkPrefix)
		{
			//Read xml
			Document xmlDoc = SAXReaderUtil.read(xml);		
			
			String xPathQuery = "//pclist/pc";
			
			XPath xpathSelector = SAXReaderUtil.createXPath(xPathQuery);
			List<Node> nodes = xpathSelector.selectNodes(xmlDoc);
			
			//For each pageContent, change order
			for (Node node : nodes) 
			{
				Element elem = (Element)node;
			
				String pageContentId = elem.attribute("id").getValue();
				int order = GetterUtil.getInteger(elem.attribute("pos").getValue());
				
				PageContent pc = PageContentLocalServiceUtil.getPageContent(groupId, pageContentId);
				
				//Insercion en la tabla de Live
				itemXmlIO.createLiveEntry(pc);
				
				pc.setOrden(order);		
				pc = pageContentPersistence.update(pc,	false);		
			}
		}
	}
	
	
	public boolean activatePageContent(long pageContentId)	throws SystemException 
	{
		PageContent pageContent = pageContentPersistence.fetchByPrimaryKey(pageContentId);
		if (pageContent != null) 
		{
			pageContent.setOnline(true);
			pageContentPersistence.update(pageContent, false);
						
			//Insercion en la tabla de Live
			try 
			{
				itemXmlIO.createLiveEntry(pageContent);
			} 
			catch (PortalException e) 
			{			
				_log.error(e);
			}
			
			return true;	
		}
		
		return false;
	}
	
	public boolean deactivatePageContent(long pageContentId) throws SystemException 
	{
		PageContent pageContent = pageContentPersistence.fetchByPrimaryKey(pageContentId);
		if (pageContent != null) 
		{
			pageContent.setOnline(false);
			pageContentPersistence.update(pageContent, false);
			
			//Insercion en la tabla de Live
			try 
			{
				itemXmlIO.createLiveEntry(pageContent);
			} 
			catch (PortalException e) 
			{
				_log.error(e);
			}
			
			return true;	
		}	
		
		return false;
	}

	public void changeLayoutOrder(long sourceLayoutPlid, long targetPositionPlid) throws PortalException, SystemException 
	{
		int priority = LayoutLocalServiceUtil.getLayout(targetPositionPlid).getPriority();
		LayoutLocalServiceUtil.updatePriority(sourceLayoutPlid, priority);
	}

	public void changeLayoutOrder(long sourceLayoutPlid, long targetPositionPlid, boolean afterTarget) throws PortalException, SystemException 
	{
		int priority = LayoutLocalServiceUtil.getLayout(targetPositionPlid).getPriority();
		Layout sourceLayout = LayoutLocalServiceUtil.getLayout(sourceLayoutPlid);
		
		LayoutLocalServiceUtil.updatePriority(sourceLayout, priority, afterTarget);
	}

	//****** UTILIDADES WEB CONTENT ******//
	public String getWebContentField(JournalArticle article, String field, String language, int pos){
		try{
			return getWebContentField(article, field, language).get(pos);
		}
		catch(Exception e){
			return "";
		}
	}
	
	public List<String> getWebContentField(JournalArticle article, String field, String language){
		return TeaserContentUtil.getWebContentField(article, field, language);
	}
	
	public List<String> getWebContentField(long groupId, String articleId, String field, String language){
		return TeaserContentUtil.getWebContentField(groupId, articleId, field, language);
	}
	
	public List<String> getInternalLinks(JournalArticle article) {
		return TeaserContentUtil.getInternalLinks(article);
	}
	
	public List<String> getInternalLinks(long groupId, String articleId) {
		return TeaserContentUtil.getInternalLinks(groupId, articleId);
	}
	
	public List<Long> getWebContentRelatedElements(JournalArticle article, int maxResults) {
		return TeaserContentUtil.getWebContentRelatedElements(article, maxResults);
	}
	
	public List<Long> getWebContentRelatedElements(String contentId, long groupId, int maxResults) {
		return TeaserContentUtil.getWebContentRelatedElements(contentId, groupId, maxResults);
	}
	
	public long getWebContentDefaultModel(long groupId, String contentId) {
		return TeaserContentUtil.getWebContentDefaultModel(groupId, contentId);
	}
	
	private void putDisqusIdentifierVariable(
			Map<String, String> extraVariables, long scopeGroupId, String contentId, HttpServletRequest request) throws Exception
	{
			HttpServletRequest originalRequest = PortalUtil.getOriginalServletRequest(request);
			Object commentsConfigBeanObject = originalRequest.getAttribute(WebKeys.REQUEST_ATTRIBUTE_COMMENTS_CONFIG_BEAN);
			if(commentsConfigBeanObject != null)
			{
				CommentsConfigBean commentsConfig = (CommentsConfigBean)commentsConfigBeanObject;
				if(commentsConfig.useDisqusConfig())
				{
					if(commentsConfig.getIdentifierType().equals(CommentsConfigBean.DISQUS_IDENTIFIER))
						extraVariables.put("disqus_identifier", CommentsConfigBean.JOURNAL_ARTICLE_PREFIX + contentId);
					else
						extraVariables.put("disqus_identifier", StringPool.BLANK);
				}
			}
	}
	
	public String getArticleContent(JournalArticle journalArticle, String faTemplateId, String raTemplateId, 
									String viewMode, ThemeDisplay themeDisplay, String xmlRequest, int mode,
									HttpServletRequest request, int pos, int last)
	{
		String environment = IterLocalServiceUtil.getEnvironment();
		boolean useFullAccessibleTemplate = false;
		StringBuilder contentHTML = new StringBuilder();
		StringBuilder faOutHTML = new StringBuilder();
		StringBuilder raOutHTML = new StringBuilder();
		
		Map<String, String> extraVariables = new HashMap<String, String>();
		
		try
		{
			String contentId = journalArticle.getArticleId();
			long groupId 	 = journalArticle.getGroupId();
			
			extraVariables.put(VelocityContext.VELOCITYVAR_ITER_TEASER_TOTAL_COUNT, 	GetterUtil.getString(String.valueOf(request.getAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_TOTAL_COUNT)), 	"0"));
			extraVariables.put(VelocityContext.VELOCITYVAR_ITER_TEASER_CUR_PAGE_COUNT, 	GetterUtil.getString(String.valueOf(request.getAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_CUR_PAGE_COUNT)), 	"0"));
			extraVariables.put(VelocityContext.VELOCITYVAR_ITER_QUALIFICATION, 	GetterUtil.getString(String.valueOf(request.getAttribute(VelocityContext.VELOCITYVAR_ITER_QUALIFICATION)), 	""));
			
			extraVariables.put(VelocityContext.VELOCITYVAR_ITER_FB_COUNT, 	GetterUtil.getString(String.valueOf(request.getAttribute(VelocityContext.VELOCITYVAR_ITER_FB_COUNT)), 	"-1"));
			extraVariables.put(VelocityContext.VELOCITYVAR_ITER_GP_COUNT, 	GetterUtil.getString(String.valueOf(request.getAttribute(VelocityContext.VELOCITYVAR_ITER_GP_COUNT)), 	"-1"));
			extraVariables.put(VelocityContext.VELOCITYVAR_ITER_TW_COUNT, 	GetterUtil.getString(String.valueOf(request.getAttribute(VelocityContext.VELOCITYVAR_ITER_TW_COUNT)), 	"-1"));
			
			putDisqusIdentifierVariable(extraVariables, themeDisplay.getScopeGroupId(), contentId, request);
				
			//Modo plantilla accesible
			if(mode == -1)
			{
				useFullAccessibleTemplate = true;
			}
			else
			{
				boolean isNewsletterPage = false;
				Object isNewsletterPageObj = request.getAttribute(WebKeys.REQUEST_ATTRIBUTE_IS_NEWSLETTER_PAGE);
				if(isNewsletterPageObj != null)
					isNewsletterPage = GetterUtil.getBoolean(isNewsletterPageObj.toString());
				
				//Modo Apache
				if(PHPUtil.isApacheRequest(request) && !isNewsletterPage)
				{
					String productsList = PortalLocalServiceUtil.getProductsByArticleId(contentId);
					if(Validator.isNotNull(productsList))
					{
						JournalContentUtil.getContent(groupId, contentId, faTemplateId, raTemplateId, 
													  viewMode, themeDisplay.getLanguageId(), themeDisplay, 
													  xmlRequest, faOutHTML, raOutHTML, mode, pos, last, extraVariables);
						
						PublicIterParams.set(WebKeys.ITER_RESPONSE_NEEDS_PHP, true);

						if ((faOutHTML.length() == 0) && (environment.equals(IterKeys.ENVIRONMENT_PREVIEW)))
							faOutHTML.append("NO CONTENT TO SHOW");
						
						if ((raOutHTML.length() == 0) && (environment.equals(IterKeys.ENVIRONMENT_PREVIEW)))
							raOutHTML.append("NO ACCESS TO CONTENT");
						
						String modifiedDate = simpleDateFormat.format(journalArticle.getModifiedDate());
						
						contentHTML.append("<?php" 																							+ 
												" if (user_has_access_to_any_of_these_products( '" + contentId 		+ 	"', array("			+ 
																									 productsList 	+ 	"),'" 				+ 
																									 modifiedDate 	+ 	"' )===true){ " 	+ 
										   "?>" 																							+ 
										   			faOutHTML.toString() 																	+
										   "<?php" 																							+ 
										   		" }else{ " 																					+
										   "?>" 																							+		
										   			raOutHTML.toString() 																	+																			
										   "<?php" 																							+
												" } " 																						+ 
										   "?>");
					}
					else
					{
						//Modo plantilla accesible
						useFullAccessibleTemplate = true;
					}
				}
				else
				{
					//Modo simulado
					boolean hasSimulatedAccessToArticle = PortalLocalServiceUtil.hasSimulatedAccessToArticle(contentId, request);
					
					if( !themeDisplay.isSignedIn() || hasSimulatedAccessToArticle)
					{
						//Modo plantilla accesible
						useFullAccessibleTemplate = true;
					}
					else
					{
						JournalContentUtil.getContent(groupId, contentId, faTemplateId, raTemplateId, 
													  viewMode, themeDisplay.getLanguageId(), themeDisplay, 
													  xmlRequest, null, raOutHTML, mode, pos, last, extraVariables);

						if ((raOutHTML.length() == 0) && (environment.equals(IterKeys.ENVIRONMENT_PREVIEW)))
							raOutHTML.append("NO ACCESS TO CONTENT");
						
						contentHTML.append(raOutHTML);
					}
				}
			}
			
			if(useFullAccessibleTemplate)
			{
				JournalContentUtil.getContent(groupId, contentId, faTemplateId, null, viewMode, 
	  					  					  themeDisplay.getLanguageId(), themeDisplay, xmlRequest, 
	  					  					  faOutHTML, null, -1, pos, last, extraVariables);
				
				if ((faOutHTML.length() == 0) && (environment.equals(IterKeys.ENVIRONMENT_PREVIEW)))
					faOutHTML.append("NO CONTENT TO SHOW");
				
				contentHTML.append(faOutHTML);
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return contentHTML.toString();
	}
	
	/**
	 * 
	 * @param groupId
	 * @param dom
	 * @param sectionName
	 * @return
	 * @throws ServiceError
	 * @throws ParseException
	 * @throws PortalException
	 * @throws SystemException
	 * @throws UnsupportedEncodingException 
	 */
	private Element getArticleContextSections(long groupId, Document dom, String sectionName) throws ServiceError, ParseException, PortalException, SystemException, UnsupportedEncodingException
	{
		Element sectionsElem = SAXReaderUtil.createElement(IterKeys.VM_SECTION.concat("s"));
		
		if (!sectionName.isEmpty())
		{
			// Si no se ha usado la canónica, se añade la información de las secciones
			Element sectionElem = SAXReaderUtil.createElement(IterKeys.VM_SECTION);
			
			DateFormat df 			= new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			long plid 				= XMLHelper.getLongValueOf(dom, "/rs/row/@plid");
			ErrorRaiser.throwIfFalse(plid > 0);
			
			long defaultSection		= XMLHelper.getLongValueOf(dom, "/rs/row/@defaultSection");
			ErrorRaiser.throwIfFalse(defaultSection == 0 || defaultSection == 1);
			
			String vigenciaDesde	= XMLHelper.getTextValueOf(	dom, "/rs/row/@vigenciadesde");
			ErrorRaiser.throwIfNull(vigenciaDesde);
			
			String vigenciaHasta	= XMLHelper.getTextValueOf(	dom, "/rs/row/@vigenciahasta");
			ErrorRaiser.throwIfNull(vigenciaHasta);
			
			String friendlyURL		= XMLHelper.getTextValueOf(	dom, "/rs/row/@friendlyURL");
			ErrorRaiser.throwIfNull(friendlyURL);
			
			String pageName			= XMLHelper.getTextValueOf(	dom, "/rs/row/@pagename",  "");
			String pageTitle		= XMLHelper.getTextValueOf(	dom, "/rs/row/@pagetitle", "");
			
			
			String url = "";
			if (IterRequest.isNewsletterPage() || !IterRequest.getRenditionMode().equals(RenditionMode.classic))
				url=IterURLUtil.getIterHost();
				
			url = url.concat(IterURLUtil.getIterURLPrefix()).concat(friendlyURL);
			url = NewsletterMASTools.addMark2Section(url, pageTitle, pageName);			
			
			String vigDesde = df.format( new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(vigenciaDesde) );
			String vigHasta = df.format( new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(vigenciaHasta) );
			
			String qualification 	= XMLHelper.getTextValueOf(	dom, "/rs/row/@qualifname");
			
			sectionElem.addAttribute("id", 		String.valueOf(plid));
			sectionElem.addAttribute("name", 	sectionName);
			sectionElem.addAttribute("default", String.valueOf(defaultSection));
			sectionElem.addAttribute("from", 	vigDesde);
			sectionElem.addAttribute("to", 		vigHasta);
			sectionElem.addAttribute("url", 	url);
			sectionElem.addAttribute("q", 		qualification);
			
			// Si no tiene padre lo añadimos a la raiz
			long parentLayoutId = XMLHelper.getLongValueOf(dom, "/rs/row/@parentLayoutId");
			if (parentLayoutId == 0)
			{
				sectionsElem.add(sectionElem);
			}
			else
			{
				//Buscamos el padre
				Element parentSectionElem =  searchParentRecursive(groupId, parentLayoutId, sectionsElem);
				
				//Lo añadimos al padre
				parentSectionElem.add(sectionElem);				
			}
		}
		
		return sectionsElem;
	}
	
	private Element getArticleContextMetadatas(String contentId) throws SecurityException, NoSuchMethodException, ServiceError
	{
		Element metadataElem = SAXReaderUtil.createElement(IterKeys.VM_METADATA);
		String sql = String.format(SEL_CATEGORYID, GroupMgr.getGlobalGroupId(), contentId);
		
		Document domCategories = PortalLocalServiceUtil.executeQueryAsDom(sql);
		List<Node> categories  = domCategories.selectNodes("/rs/row");
		
		for (Node category : categories)
		{
			long vocabularyId = XMLHelper.getLongValueOf(category, "@vocabularyId");
			ErrorRaiser.throwIfFalse(vocabularyId > 0);
			
			Node vocNode 	= metadataElem.selectSingleNode( String.format("%s[@id='%d']", IterKeys.VM_KEY, vocabularyId) );
			Element vocElem = (Element)((vocNode != null) ? vocNode : null);
			if (vocElem == null)
			{
				// Primera vez que se referencia el vocabulario, se añade
				vocElem = SAXReaderUtil.createElement(IterKeys.VM_KEY);
				vocElem.addAttribute( "id", String.valueOf(vocabularyId) );
				
				String vocName = XMLHelper.getTextValueOf(category, "@vocName");
				ErrorRaiser.throwIfNull(vocName);
				// Si viene con ID de Delegación, lo elimina
				vocName = vocName.substring(vocName.indexOf(IterGlobalKeys.DLG_SEPARATOR) + 1);
				vocElem.addAttribute("name", vocName);
				
				vocElem.addAttribute("set",  "1");
				vocElem.addAttribute("main", "0");
				
				metadataElem.add(vocElem);
			}
			
			Element keyElem = SAXReaderUtil.createElement(IterKeys.VM_KEY);
			keyElem.addAttribute( "id", String.valueOf(XMLHelper.getLongValueOf(category, "@categoryId")) );
			
			String name = XMLHelper.getTextValueOf(category, "@name");
			ErrorRaiser.throwIfNull(name);
			
			keyElem.addAttribute("name", name);
			keyElem.addAttribute("friendlyname", CategoriesUtil.normalizeText(name));
			keyElem.addAttribute("set", "1");
			keyElem.addAttribute("main", String.valueOf(XMLHelper.getLongValueOf(category, "@mainCategory")));
			
			vocElem.add(keyElem);
		}

		return metadataElem;
	}
	
	private void getArticleContextInfo_ThrowIfNull(String url, long groupId, String contentId, String[] layoutIds) throws ServiceError
	{
		if (Validator.isNull(url))
		{
			String trace = String.format("groupId(%d) contentId(%s) layoutIds(%s)\n", groupId, contentId,
							Validator.isNull(layoutIds) ? "null" : StringUtils.join(layoutIds));
			
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, trace);
		}
	}
	
	public String getArticleContextInfo(long groupId, String contentId, String[] layoutIds) throws SecurityException, NoSuchMethodException, ServiceError, PortalException, SystemException, ParseException, UnsupportedEncodingException, MalformedURLException, DocumentException
	{     
		Document dom = IterURLUtil.getArticleInfoByLayoutUUID( groupId, contentId, layoutIds, true );
		
		Element root = SAXReaderUtil.createElement(IterKeys.VM_ROOT);
		
		root.addAttribute("id", contentId);
		
		String host = "";
		if (IterRequest.isNewsletterPage() || !IterRequest.getRenditionMode().equals(RenditionMode.classic))
			host = IterURLUtil.getIterHost() + IterURLUtil.getIterURLPrefix();
		
		//--- url
		String url = XMLHelper.getStringValueOf(dom, "/rs/row/@articleContextURL");
		getArticleContextInfo_ThrowIfNull(url, groupId, contentId, layoutIds);
		url = host.concat(url);
		
		//--- canonicalURL
		String canonicalURL = XMLHelper.getStringValueOf(dom, "/rs/row/@canonicalURL");
		getArticleContextInfo_ThrowIfNull(canonicalURL, groupId, contentId, layoutIds);
		canonicalURL = host.concat(canonicalURL);
		
		String[] urls = new String[]{url, canonicalURL};
		NewsletterMASTools.addMark2Details(urls, contentId);
		
		Element urlElem = SAXReaderUtil.createElement(IterKeys.VM_URL);
		urlElem.addText(urls[0]);	
		root.add(urlElem);
		
		Element canonicalURLElem = SAXReaderUtil.createElement(IterKeys.REQUEST_ATTRIBUTE_CANONICAL_URL);	
		canonicalURLElem.addText(urls[1]);
		root.add(canonicalURLElem);
		
		//--- section_name
		String sectionName = XMLHelper.getStringValueOf(dom, "/rs/row/@pagename", StringPool.BLANK);
		Element sectionNameElem = SAXReaderUtil.createElement(IterKeys.VM_SECTION_NAME);				
		sectionNameElem.addText(sectionName);
		root.add(sectionNameElem);

		// Se añade la informacón de las secciones
		root.add( getArticleContextSections(groupId, dom, sectionName) );
							
		// Se añade la información de los metadatos
		root.add( getArticleContextMetadatas(contentId) );
		
		Document doc = SAXReaderUtil.createDocument(root);
		return doc.asXML();
	}
     
	/**
	 * 
	 * @param groupId
	 * @param plid
	 * @param sectionsElem
	 * @param request
	 * @return
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws ServiceError 
	 * @throws UnsupportedEncodingException 
	 */
	private Element searchParentRecursive (long groupId, long parentId, Element rootElem) throws PortalException, SystemException, ServiceError, UnsupportedEncodingException
	{
		Element res;
		
		Layout parentLayout = LayoutLocalServiceUtil.getLayout(groupId, false, parentId);
		String vmname 		= IterKeys.VM_SECTION;
		long id 			= parentLayout.getPlid();
		String name 		= parentLayout.getName(IterRequest.getOriginalRequest().getLocale(), true);
		long parentparentId = parentLayout.getParentLayoutId();
		String elementURL 	= IterURLUtil.getIterURLPrefix().concat( parentLayout.getFriendlyURL() );
		
		if (!elementURL.isEmpty() && (IterRequest.isNewsletterPage() || !IterRequest.getRenditionMode().equals(RenditionMode.classic)))
		{
			elementURL = IterURLUtil.getIterHost().concat(elementURL);
			elementURL = NewsletterMASTools.addMark2Section(elementURL, parentLayout);
		}
		
		//Compruebo si ya está en sectionsElem
		List<Node> node = rootElem.selectNodes("//" + vmname + "[@id='" + id + "']");

		//Si no existe --> lo creamos
		if (node.size() <= 0)
		{		
			Element newElem = SAXReaderUtil.createElement(vmname);
			
			newElem.addAttribute("id", 			String.valueOf(id));
			newElem.addAttribute("name", 		name);
			newElem.addAttribute("friendlyname",CategoriesUtil.normalizeText(name));
			
			if (!elementURL.isEmpty())
				newElem.addAttribute("url", elementURL);
			
			//Si no tiene padre lo añadimos a la raiz
			if (parentparentId == 0)
			{			
				// Se añade también el vocabulario
				rootElem.elements().add(newElem);
			}
			else
			{
				//Buscamos el padre
				Element parentSectionElem = searchParentRecursive(groupId, parentparentId, rootElem);
				
				//Lo añadimos al padre
				parentSectionElem.elements().add(newElem);				
			}
			
			res = newElem;
		}
		else
		{
			res = (Element) node.get(0);
		}
		
		return res;
	}
	
	/**
	 * Obtiene el nivel de acceso del contenido como el valor de la propiedad asociada a cada una de las categorias
	 * del vocabulario de nivel de acceso asociadas al contenido.
	 * @param companyId
	 * @param groupId
	 * @param contentId
	 * @return Nivel de acceso asociado al contenido, si existe; -1 si no existe o hay error
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public int getWebContentAccessLevel(long companyId, long groupId, long contentId){
		try{ 
			List<String> accessLevels = new ArrayList<String>();
			
			//1. Recupera las categorias asociadas al vocabulario reservado para niveles de acceso
			List<AssetCategory> contentCategories = 
				CategorizeLocalServiceUtil.getItemCategories(companyId, groupId, IterKeys.CLASSNAME_JOURNALARTICLE, contentId, IterKeys.USER_ACCESS_LEVEL_VOCABULARY);
			
			//2. Obtiene las propiedades con la clave reservada para el valor numerico del acceso
			for (AssetCategory category : contentCategories){
				AssetCategoryProperty acp = AssetCategoryPropertyLocalServiceUtil.getCategoryProperty(category.getCategoryId(), IterKeys.USER_ACCESS_LEVEL_VALUE_PROPERTY);
				accessLevels.add(acp.getValue());
			}
			
			//3. Recupera la que tiene mayor nivel (más restrictiva)
			Comparator comparator = Collections.reverseOrder();
			Collections.sort(accessLevels, comparator);
			
			return (Integer.valueOf(accessLevels.get(0)));
		}
		catch(Exception err){
			return -1;
		}
	}
	
	/**
     * 
     * @param classNameId
     * @param classPK
     * @return List<String> with vocabularyLiveIds associated to the classPK passed as parameter.
     */
     public List<String> getVocabularyLiveIdsFromJournalArticle (long classNameId, long classPK){
           PageContentDynamicQuery pcdq = new PageContentDynamicQuery();
                       
           StringBuffer sql = new StringBuffer();   
           sql.append("SELECT xl.id_ FROM Xmlio_Live xl WHERE xl.localId IN (");
           sql.append("SELECT DISTINCT(vocabularyId) FROM AssetCategory ac WHERE ac.categoryId IN ");
           sql.append("(SELECT categoryId FROM AssetEntry ae INNER JOIN AssetEntries_AssetCategories ae_ac ");
           sql.append("ON ae.entryId = ae_ac.entryId ");
           sql.append("WHERE ae.classNameId = ");
           sql.append(classNameId);
           sql.append(" AND ae.classPK = ");
           sql.append(classPK);
           sql.append("))AND (xl.STATUS = 'pending' OR xl.STATUS = 'error');");        
           
           List<Object> liveObjList = pcdq.executeQuery(sql.toString());
                 
           List<String> resList = new ArrayList<String>(liveObjList.size());
           for (Object myObj : liveObjList) { 
                 resList.add(String.valueOf(myObj)); 
           }
           
           return resList;         
     }
     public List<Object> sqlQuery(String sql){
 		 PageContentDynamicQuery pcdq = new PageContentDynamicQuery();  
         
         List<Object> objList = pcdq.executeQuery(sql);
         return objList;
     }
     
 	/**
 	 * @param model XML que modela el movimiento. Será de la forma:
 	 * 	<?xml version="1.0"?>
 	 *	<rs>
 	 *	    <target id="1234" afterTarget="0|1"/>
 	 *	    <source id="7654"/>
 	 *	    <source id="9876"/>
 	 *	    <source id="1415"/>
 	 *	</rs>
 	 *
 	 * @see http://10.15.20.59:8090/pages/viewpage.action?pageId=11600340
 	 * @return El método de momento retornará una cadena vacía, pero se deja la opción 
 	 * por si en el futuro se amplian los requerimientos y fuese necesario devolver un 
 	 * XML, no surjan problemas de compatibilidad con versiones previas.
 	 * @throws DocumentException 
 	 * @throws SQLException 
 	 * @throws IOException 
 	 * @throws com.liferay.portal.kernel.error.ServiceError 
 	 */
 	public String movePageContents(String model) throws DocumentException, TransformerException, IOException, SQLException, com.liferay.portal.kernel.error.ServiceError
 	{
 		Document doc = SAXReaderUtil.read(model);
 		
 		// Se obtiene el elemento de referencia
 		long targetId = XMLHelper.getLongValueOf(doc, "/rs/target/@id");
 		ErrorRaiser.throwIfFalse(targetId > 0, IterErrorKeys.XYZ_E_TARGET_NOT_FOUND_ZYX);
 		
 		// Se obtiene la posición del elemento de referencia y de paso se comprueba que exista en BBDD
 		String sql = String.format("SELECT orden, layoutId FROM News_PageContent WHERE pageContentId = %d", targetId);
 		List<Object> targetInfo = PortalLocalServiceUtil.executeQueryAsList(sql);
 		ErrorRaiser.throwIfFalse(targetInfo.size() > 0, IterErrorKeys.XYZ_E_TARGET_NOT_FOUND_ZYX);
 		
 		long targetOrder 	= Long.parseLong( ((Object[])targetInfo.get(0))[0].toString() );
 		String targetLayout	= ((Object[])targetInfo.get(0))[1].toString();
 		
 		// Se obtiene el tipo de inserción
 		boolean moveAfter 	= (XMLHelper.getLongValueOf(doc, "/rs/target/@afterTarget") == 0) ? false : true;

 		// Se obtiene cuál será la posición a partir de la cuál se moverán los PageContent
 		long afterPos	  	= (moveAfter) ? targetOrder : targetOrder-1;
 		
 		// Se obtiene el número de páginas a mover
 		long numPageContents = XMLHelper.getLongValueOf(doc, "count(/rs/source)");
 		ErrorRaiser.throwIfFalse(numPageContents > 0, IterErrorKeys.XYZ_E_SOURCE_NOT_FOUND_ZYX);
 		
 		// Se obtiene la lista de PageContentId, al menos uno tiene que estar bien formado <source id="9876"/>
 		String pageContentsId = XSLUtil.transformToText(model, "/news-portlet/xsl/ExtractSourcesId.xsl");
 		ErrorRaiser.throwIfFalse(pageContentsId.length() > 0, IterErrorKeys.XYZ_E_SOURCE_NOT_FOUND_ZYX);
 		
 		// Se actualiza la posición de los elementos posteriores a la posición de referencia
 		sql = String.format(INCREASE_PAGECONTENT_ORDEN, numPageContents+1, afterPos, targetLayout, pageContentsId);
 		_log.trace(sql);
 		PortalLocalServiceUtil.executeUpdateQuery(sql);
 		 		
 		// Se actualiza la posición de los PageContents a mover
 		sql = String.format(UPDT_PAGECONTENT_ORDEN, afterPos, pageContentsId, targetLayout, pageContentsId);
 		_log.trace(sql);
 		PortalLocalServiceUtil.executeUpdateQuery(sql);
 		pageContentPersistence.clearCache();
 		
 		// Se actualiza el XmlIO_Live.Status de todos los PageContents que hayan modificado su posición (los que se movian, y aquellos que han caido detrás de los movidos)
 		sql = String.format(UPDT_PAGECONTENT_XMLIOLIVE_STATUS, targetLayout, afterPos);
 		_log.trace(sql);
 		PortalLocalServiceUtil.executeUpdateQuery(sql);
 		
 		// Se actualiza el XmlIO_Live.Status de todos los JournalArticles de los PageContents que hayan modificado su posición (los que se movian, y aquellos que han caido detrás de los movidos)
 		sql = String.format(UPDT_JOURNALARTICLE_XMLIOLIVE_STATUS, targetLayout, afterPos);
 		_log.trace(sql);
 		PortalLocalServiceUtil.executeUpdateQuery(sql);

 		LiveUtil.clearCache();
 		// IterCacheUtil.clearCache("com.protecmedia.iter.xmlio.model.impl.LiveImpl", "com.protecmedia.iter.xmlio.model.impl.LiveImpl.List");

 		return "";
 	}
 	
 	public List<String[]> getFilterArticles(long groupId, List<String> structures, int startIndex, int numElements, 
 											Date validityDate, Date modifiedDate, String[] orderFields, int typeOrder, 
 											long[] categoriesId, String[] qualificationId, String[] layoutIds)
	{
 		
 		return TeaserContentUtil.getFilterArticles(groupId, null, structures, startIndex, numElements, validityDate, 
 						  						   false, orderFields, typeOrder, categoriesId, null, qualificationId,
 						  						   layoutIds, false, "", null, modifiedDate, null);
	}
 	
 	
 	public List<String[]> getFilterArticles(long groupId, String contentId, List<String> structures, int startIndex, 
			   int numElements, Date validityDate, boolean showNonActiveContents, 
			   String[] orderFields, int typeOrder, long[] categoriesId, 
			   List<KeyValuePair> internalLinks, String[] qualificationId,
			   String[] layoutIds, boolean excludeContentId, String extraLimit, 
			   Date creationDate, Date modifiedDate)
	{
	
		return TeaserContentUtil.getFilterArticles(groupId, contentId, structures, startIndex, numElements, 
			 validityDate, showNonActiveContents, orderFields, typeOrder, 
			 categoriesId, internalLinks, qualificationId, layoutIds, 
			 excludeContentId, extraLimit, creationDate, modifiedDate, null);
	}
 	
	public int getTemplateMode(JournalArticle article, int articleTemplateMode, int galleryTemplateMode, int pollTemplateMode, int multimediaTemplateMode)
	{

		return TeaserContentUtil.getTemplateMode(article, articleTemplateMode, galleryTemplateMode, pollTemplateMode, multimediaTemplateMode);
	}
	
	public String getTemplateId(JournalArticle article, String templateIdArticle, String templateIdGallery, String templateIdPoll, String templateIdMultimedia)
	{
		return TeaserContentUtil.getTemplateId(article, templateIdArticle, templateIdGallery, templateIdPoll, templateIdMultimedia);
	}
	
	public String getCSSClass(int index, int listSize)
	{
		return TeaserContentUtil.getCSSClass(index, listSize);
	}
	
	public String getCSSAccessClass(JournalArticle journalArticle, HttpServletRequest request) throws ServiceError
	{
		return TeaserContentUtil.getCSSAccessClass(journalArticle, request);
	}
	
	public CounterLocalService getCounter() {
		return counterLocalService;
	}
	
	public void swapOrderAndQualification(String articleId1, String articleid2, long plid) throws PortalException, SystemException, IOException, SQLException
	{
		if(PropsValues.IS_PREVIEW_ENVIRONMENT)
		{
			if(_log.isDebugEnabled())
				_log.debug(String.format("articleId1: %s	articleid2: %s	plid: %s", articleId1, articleid2, plid));
			
			if( Validator.isNotNull(articleId1) && Validator.isNotNull(articleid2) && Validator.isNotNull(plid) )
			{
				String callProcedure = String.format("CALL ITR_SWAP_ARTICLES_IN_PAGECONTENT('%s', '%s', %d)", articleId1, articleid2, plid);
				_log.debug(callProcedure);
				PortalLocalServiceUtil.executeUpdateQuery(callProcedure);
				// Se borra la caché para que se borren los beans
				pageContentPersistence.clearCache();
		 		LiveUtil.clearCache();
				
				LiveLocalServiceUtil.updatePublicationDate(IterKeys.CLASSNAME_PAGECONTENT, LayoutLocalServiceUtil.getLayout(plid).getGroupId());
			}
		}
		else
			_log.debug("Method 'swapOrderAndQualification' available only in preview");
	}
}
