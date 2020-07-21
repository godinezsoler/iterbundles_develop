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

package com.protecmedia.iter.designer.service.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.portlet.PortletPreferences;
import javax.portlet.ReadOnlyException;
import javax.portlet.ValidatorException;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.Junction;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portlet.PortletPreferencesFactoryUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.designer.DuplicatePageTemplateException;
import com.protecmedia.iter.designer.NoSuchPageTemplateException;
import com.protecmedia.iter.designer.TypeErrorPageTemplateException;
import com.protecmedia.iter.designer.model.PageTemplate;
import com.protecmedia.iter.designer.model.impl.PageTemplateImpl;
import com.protecmedia.iter.designer.service.base.PageTemplateLocalServiceBaseImpl;
import com.protecmedia.iter.designer.service.item.PageTemplateXmlIO;
import com.protecmedia.iter.designer.util.DesignerUtil;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;

/**
 * @author Protecmedia
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class PageTemplateLocalServiceImpl
	extends PageTemplateLocalServiceBaseImpl {
	
	private static Log _log = LogFactoryUtil.getLog(PageTemplateLocalServiceImpl.class);
	private static ItemXmlIO itemXmlIO = new PageTemplateXmlIO();
	
	private static final String CHECK_NEXT_DEFAULT_PAGETEMPLATE_EXISTS = new StringBuilder()
		.append("SELECT groupId \n")
		.append("FROM Designer_PageTemplate \n")
		.append("WHERE id_ = %s \n")
		.append("  AND type_ = '").append(IterKeys.DESIGNER_PAGE_TEMPLATE_ARTICLE_TYPE).append("'").toString();
	
	/**
	 * Query para eliminar el default de todos las entradas de tipo article-template
	 * del grupo indicado.
	 * 
	 * Sirve tanto para la columna defaultTemplate como para defaultMobiletemplate.
	 */
	private static final String UNSET_DEFAULT_PAGETEMPLATE = new StringBuilder()
		.append("UPDATE Designer_PageTemplate \n")
		.append("SET %1$s = 0 \n")
		.append("WHERE groupId = %2$s \n")
		.append("  AND type_ = '").append(IterKeys.DESIGNER_PAGE_TEMPLATE_ARTICLE_TYPE).append("'")
		.append("  AND %1$s = 1").toString();
	
	/**
	 * Query para establecer a 1 defaulttemplate o defaultMobiletemplate de la
	 * entrada indicada por ID. 
	 */
	private static final String SET_DEFAULT_PAGETEMPLATE = new StringBuilder()
		.append("UPDATE Designer_PageTemplate \n")
		.append("SET %s = 1 \n")
		.append("WHERE id_ = %s").toString();
	
	/** Columna Designer_PageTemplate.defaultTemplate */
	private static final String DEFAULT_TEMPLATE = "defaultTemplate";
	
	/** Columna Designer_PageTemplate.defaultMobileTemplate */
	private static final String DEFAULT_MOBILE_TEMPLATE = "defaultMobileTemplate";
	
	/*
	 * ADD Functions
	 */
	
	public PageTemplate createPageTemplate(long groupId, String name,
			String description, String type, long userId) throws SystemException, PortalException {
		
		String pageTemplateId = String.valueOf(counterLocalService.increment());
		
		return createPageTemplate_PTI_PI_DT(groupId, pageTemplateId, name, description, type,userId, LayoutConstants.DEFAULT_PARENT_LAYOUT_ID, false);
	}
	
	public PageTemplate createPageTemplate(	long groupId, String name, String description, String type, long userId, 
											String header, String footer, String menu, String extraData ) throws Exception
	{
		PageTemplate pt = null;
		try
		{
			String pageTemplateId = String.valueOf(counterLocalService.increment());
		
			// Recupera extraData en caso de informarse.
			Document domExtraData = Validator.isNotNull(extraData) ? SAXReaderUtil.read(extraData) : null;

			// Recupera el parámetro firendlyurl de extraData
			String friendlyUrl = Validator.isNotNull(domExtraData) ? XMLHelper.getStringValueOf( domExtraData, "/rs/@friendlyurl" ) : StringPool.BLANK;
			
			pt = createPageTemplate_PTI_PI_DT_IMG_URL(groupId, pageTemplateId, name, description, type,userId, LayoutConstants.DEFAULT_PARENT_LAYOUT_ID, false, -1, friendlyUrl);
		
			if (Validator.isNotNull(domExtraData))
			{
				long srcId = XMLHelper.getLongValueOf( domExtraData, "/rs/@srcid" );
				if (srcId > 0)
					_loadPageTemplate(0, 0, srcId, pt.getLayoutId(), true);
				else if ( (srcId = XMLHelper.getLongValueOf( domExtraData, "/rs/@srcplid" )) > 0 )
					com.protecmedia.iter.news.service.LayoutLocalServiceUtil.copyLayout(srcId, pt.getLayoutId(), true, true);
			}
			
			LayoutLocalServiceUtil.updateSectionProperties( groupId, pt.getLayoutId(), menu, header, footer, null, null );
		}
		catch(Exception e)
		{
			if( pt!=null )
			{
				try
				{
					deletePageTemplateId(pt.getId());
				}
				catch (Exception e2)
				{
					_log.error("Error when trying to delete the page template.\n\t Cause:" + e2.getCause());
					_log.debug(e2);
				}
			}
			
			throw e;
		}
		
		return pt;
	}
	
	public PageTemplate createPageTemplate_DT(long groupId, String name,
			String description, String type, long userId, boolean defaultTemplate) throws SystemException, PortalException {
		
		String pageTemplateId = String.valueOf(counterLocalService.increment());
		
		return createPageTemplate_PTI_PI_DT(groupId, pageTemplateId, name, description, type, userId, LayoutConstants.DEFAULT_PARENT_LAYOUT_ID, defaultTemplate);
	}
	
	public PageTemplate createPageTemplate_PI_DT(long groupId, String name,
			String description, String type, long userId, long parentId, boolean defaultTemplate) throws SystemException, PortalException {
		
		String pageTemplateId = String.valueOf(counterLocalService.increment());
		
		return createPageTemplate_PTI_PI_DT(groupId, pageTemplateId, name, description, type, userId, parentId, defaultTemplate);
	}
	
	public PageTemplate createPageTemplate_PTI_PI_DT(long groupId, String pageTemplateId, String name,
			String description, String type, long userId, long parentId, boolean defaultTemplate) throws SystemException, PortalException {
				
		return createPageTemplate_PTI_PI_DT_IMG(groupId, pageTemplateId, name, description, type, userId, parentId, defaultTemplate, -1);
	}	

	//Utilizada por MILENIUM
	public PageTemplate createPageTemplate_PTI_PI_DT_IMG(long groupId, String pageTemplateId, String name,
			String description, String type, long userId, long parentId, boolean defaultTemplate, long imageId) throws SystemException, PortalException {		
		return createPageTemplate_PTI_PI_DT_IMG_URL(groupId, pageTemplateId, name, description, type, userId, parentId, defaultTemplate, -1, StringPool.BLANK);
	}
	
	public PageTemplate createPageTemplate_PTI_PI_DT_IMG_URL(long groupId, String pageTemplateId, String name, String description,
			String type, long userId, long parentId, boolean defaultTemplate, long imageId, String friendlyUrl) throws SystemException, PortalException 
	{	
		return createPageTemplate_PTI_PI_DT_IMG_URL_DMT(groupId, pageTemplateId, name, description, type, userId, parentId, defaultTemplate, imageId, friendlyUrl, false);
	}
	
	public PageTemplate createPageTemplate_PTI_PI_DT_IMG_URL_DMT(long groupId, String pageTemplateId, String name, String description,
												String type, long userId, long parentId, boolean defaultTemplate,
												long imageId, String friendlyUrl, boolean defaultMobileTemplate) throws SystemException, PortalException
	{
		userId = GroupMgr.getDefaultUserId();
		ServiceContext serviceContext = new ServiceContext();
		
		parentId = DesignerUtil.getPageTemplateParentId(groupId, userId);
		
		long plid = 0;
		Layout layout = LayoutLocalServiceUtil.addLayout(userId, groupId, false, parentId, name, StringPool.BLANK,
			 											  StringPool.BLANK, LayoutConstants.TYPE_PORTLET, true, friendlyUrl, serviceContext);
		plid = layout.getPlid();
		
		long id = counterLocalService.increment();
		PageTemplate pageTemplate = pageTemplatePersistence.create(id);
		
		pageTemplateId.trim().toUpperCase();
		if (pageTemplateId.equals("")) {
			pageTemplateId = String.valueOf(id);
		}
		
		validate(groupId, pageTemplateId, type);

		pageTemplate.setPageTemplateId(pageTemplateId);
		pageTemplate.setGroupId(groupId);
		pageTemplate.setLayoutId(plid);
		pageTemplate.setName(name);
		pageTemplate.setDescription(description);
		pageTemplate.setType(type);
		pageTemplate.setDefaultTemplate(defaultTemplate);
		pageTemplate.setDefaultMobileTemplate(defaultMobileTemplate);

		if (imageId != -1){
			pageTemplate.setImageId(imageId);
		}
		
		pageTemplatePersistence.update(pageTemplate, false);
		
		//Insercion en la tabla de Live
		itemXmlIO.createLiveEntry(pageTemplate);

		return pageTemplate;
	}
	
	/* */

	public PageTemplate addPageTemplate(long groupId, long layoutId, String name,
			String description, String type) throws SystemException, PortalException {

		String pageTemplateId = String.valueOf(counterLocalService.increment());
		
		return addPageTemplate_PTI_DT(groupId, layoutId, pageTemplateId, name, description, type, false);
	}
	
	public PageTemplate addPageTemplate_DT(long groupId, long layoutId, String name,
			String description, String type, boolean defaultTemplate) throws SystemException, PortalException {

		String pageTemplateId = String.valueOf(counterLocalService.increment());
		
		return addPageTemplate_PTI_DT(groupId, layoutId, pageTemplateId, name, description, type, defaultTemplate);
	}
	
	public PageTemplate addPageTemplate_PTI_DT(long groupId, long layoutId, String pageTemplateId, String name,
			String description, String type, boolean defaultTemplate) throws SystemException, PortalException {
		
		return addPageTemplate_PTI_DT_IMG(groupId, layoutId, pageTemplateId, name, description, type, defaultTemplate, -1);
	}
	
	public PageTemplate addPageTemplate_PTI_DT_IMG(long groupId, long layoutId, String pageTemplateId, String name,
			String description, String type, boolean defaultTemplate, long imageId) throws SystemException, PortalException
	{
		return addPageTemplate_PTI_DT_IMG_DMT(groupId, layoutId, pageTemplateId, name, description, type, defaultTemplate, imageId, false);
	}
	
	public PageTemplate addPageTemplate_PTI_DT_IMG_DMT(long groupId, long layoutId, String pageTemplateId, String name,
								String description, String type, boolean defaultTemplate, long imageId, boolean defaultMobileTemplate) throws SystemException, PortalException
	{
		pageTemplateId.trim().toUpperCase();

		long id = counterLocalService.increment();
		PageTemplate pageTemplate = pageTemplatePersistence.create(id);
		
		if (pageTemplateId.equals("")) {
			pageTemplateId = String.valueOf(id);
		}

		validate(groupId, pageTemplateId, type);
		
		pageTemplate.setPageTemplateId(pageTemplateId);
		pageTemplate.setGroupId(groupId);
		pageTemplate.setLayoutId(layoutId);
		pageTemplate.setName(name);
		pageTemplate.setDescription(description);
		pageTemplate.setType(type);
		pageTemplate.setDefaultTemplate(defaultTemplate);
		pageTemplate.setDefaultMobileTemplate(defaultMobileTemplate);

		if (imageId != -1){
			pageTemplate.setImageId(imageId);
		}
		
		pageTemplatePersistence.update(pageTemplate, false);
		
		//Insercion en la tabla de Live
		itemXmlIO.createLiveEntry(pageTemplate);

		return pageTemplate;
	}
	
	public PageTemplate addDefaultPageTemplate(long groupId){
		
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW)){
			
			try{
				ServiceContext serviceContext = new ServiceContext();
				serviceContext.setAddCommunityPermissions(true);
				serviceContext.setAddGuestPermissions(true);
				serviceContext.setScopeGroupId(groupId);
				
				Group group = GroupLocalServiceUtil.getGroup(groupId);
				long companyId = group.getCompanyId();
				
				long userId = UserLocalServiceUtil.getDefaultUserId(companyId);	
				
				/*
				 * Add default Viewer (Preview only)		
				 */	
				Layout defaultViewer = LayoutLocalServiceUtil.addLayout(userId,
						groupId, false, LayoutConstants.DEFAULT_PARENT_LAYOUT_ID, IterKeys.DEFAULT_VIEWER_NAME,
						StringPool.BLANK, StringPool.BLANK,
						LayoutConstants.TYPE_PORTLET, true, IterKeys.DEFAULT_VIEWER_URL,
						serviceContext);
				
				//Actualizamos el globalId para que coincida en todas las máquinas		
				LiveLocalServiceUtil.updateGlobalId(groupId, IterKeys.CLASSNAME_LAYOUT, String.valueOf(defaultViewer.getPlid()), IterKeys.DEFAULT_VIEWER_NAME);
			
				/*
				 * Create and set default PageTemplate
				 */		
				PageTemplate defaultPageTemplate = addPageTemplate(groupId, defaultViewer.getPlid(), IterKeys.DEFAULT_VIEWER_NAME, StringPool.BLANK, "article-template");
				
				//Actualizamos el globalId para que coincida en todas las máquinas
				LiveLocalServiceUtil.updateGlobalId(groupId, IterKeys.CLASSNAME_PAGETEMPLATE, String.valueOf(defaultPageTemplate.getPageTemplateId()), IterKeys.DEFAULT_VIEWER_NAME);
							
				setDefaultPageTemplate(defaultPageTemplate.getId());
				
				return defaultPageTemplate;
			
			}
			catch(Exception err){
				_log.error("Cannot create default pageTemplate");
			}
		}	
		return null;
	}

	private void validate(long groupId, String pageTemplateId, String type)
			throws DuplicatePageTemplateException, TypeErrorPageTemplateException, SystemException {

		PageTemplate pageTemplate = pageTemplatePersistence.fetchByGroupIdPageTemplateId(groupId, pageTemplateId);

		if (pageTemplate != null) {
			throw new DuplicatePageTemplateException();
		}
		
		if (!type.equals("page-template") && !type.equals("article-template") && !type.equals("newsletter")) {
			throw new TypeErrorPageTemplateException();
		}
	}
	
	/*
	 * UPDATE Funtions
	 */
	
	public PageTemplate updatePageTemplate(long id, long layoutId, String name, String description,
			String type, boolean defaultTemplate, long imageId) throws PortalException, SystemException 
	{
		return updatePageTemplate(id, layoutId, name, description, type, defaultTemplate, imageId, false);
	}
	
	public PageTemplate updatePageTemplate(long id, long layoutId, String name, String description, String type, boolean defaultTemplate, long imageId, boolean defaultMobileTemplate) throws PortalException, SystemException
	{
		PageTemplate pageTemplate = null;
		
		if (id!=-1)
			pageTemplate = pageTemplatePersistence.findByPrimaryKey(id);
		
		if (pageTemplate == null) 
			throw new NoSuchPageTemplateException();
		
		pageTemplate.setName(name);
		pageTemplate.setLayoutId(layoutId);	
		pageTemplate.setDescription(description);
		pageTemplate.setType(type);
		pageTemplate.setDefaultTemplate(defaultTemplate);
		pageTemplate.setDefaultMobileTemplate(defaultMobileTemplate);

		if (imageId != -1){
			pageTemplate.setImageId(imageId);
		}
		
		pageTemplatePersistence.update(pageTemplate, false);
		
		//Insercion en la tabla de Live
		itemXmlIO.createLiveEntry(pageTemplate);
		
		return pageTemplate;
	}
	
	
	//Utilizada por MILENIUM
	public PageTemplate updatePageTemplateName(long id, String name) throws PortalException, SystemException {
		
		PageTemplate pageTemplate = null;
		
		if(_log.isDebugEnabled())
			_log.debug( String.format("updatePageTemplateName id=%s name=%s", id, name) );
		
		if (id!=-1)
		{
			pageTemplate = pageTemplatePersistence.findByPrimaryKey(id);
			if(_log.isDebugEnabled() && pageTemplate!=null)
				_log.debug("Before update name: " + pageTemplate.toString());
		}
		
		if (pageTemplate == null)
		{
			if(_log.isDebugEnabled())
				_log.debug("pagetemplate is null. PrimaryKey id="+id);
			
			throw new NoSuchPageTemplateException();
		}
		
		pageTemplate.setName(name);
		
		pageTemplatePersistence.update(pageTemplate, false);
		
		//Insercion en la tabla de Live
		itemXmlIO.createLiveEntry(pageTemplate);
		
		if(_log.isDebugEnabled())
			_log.debug("After update name: " + pageTemplate.toString() );
		
		return pageTemplate;
	}	
	
	public void updatePageTemplate(long id, String name, String header, String footer, String menu) throws Exception
	{
		updatePageTemplate1(id, name, header, footer, menu, null);
	}
	
	public void updatePageTemplate1(long id, String name, String header, String footer, String menu, String extraData) throws Exception
	{
		PageTemplate pageTemplate = null;
		
		if(_log.isDebugEnabled())
			_log.debug( String.format("updatePageTemplate1 id_=%s name=%s header=%s footer=%s menu=%s extraData=%s", id, name, header, footer, menu, extraData) );
		
		if( Validator.isNotNull(name) )
			pageTemplate = updatePageTemplateName(id, name);
		
		if (pageTemplate == null)
		{
			if(_log.isDebugEnabled())
				_log.debug("pageTemplate is null");
			
			pageTemplate = pageTemplatePersistence.findByPrimaryKey(id);
			if (pageTemplate == null)
				throw new NoSuchPageTemplateException();
		}
		
		// Campos extra
		Document domExtraData = Validator.isNotNull(extraData) ? SAXReaderUtil.read(extraData) : null;
		
		if (Validator.isNotNull(domExtraData))
		{
			if(_log.isDebugEnabled())
				_log.debug("update from extradata");
			
			// FriendlyURL
			String friendlyURL = XMLHelper.getStringValueOf( domExtraData, "/rs/@friendlyurl" );
			if (Validator.isNotNull(friendlyURL))
			{
				if(_log.isDebugEnabled())
					_log.debug("New friendlyurl: " + friendlyURL);
				LayoutLocalServiceUtil.updateFriendlyURL(pageTemplate.getLayoutId(), friendlyURL);
			}
			else if(_log.isDebugEnabled())
				_log.debug("Friendlyurl is empty or null");
			
			long srcId = XMLHelper.getLongValueOf( domExtraData, "/rs/@srcid" );
			if (srcId > 0)
				_loadPageTemplate(0, 0, srcId, pageTemplate.getLayoutId(), true);
			else if ( (srcId = XMLHelper.getLongValueOf( domExtraData, "/rs/@srcplid" )) > 0 )
			{
				if(_log.isDebugEnabled())
					_log.debug( String.format("Copy layout params: srcPlid=%s dstPlid=%s copySectionProperties=%s", srcId, pageTemplate.getLayoutId(), "true") );
				
				com.protecmedia.iter.news.service.LayoutLocalServiceUtil.copyLayout(srcId, pageTemplate.getLayoutId(), true, true);
				
				if(_log.isDebugEnabled())
					_log.debug("Layout copied");
			}
		}

		// Cabecera, menu y pie
		if( Validator.isNotNull(header) || Validator.isNotNull(footer) || Validator.isNotNull(menu) )
		{
			if(_log.isDebugEnabled())
				_log.debug("Call updateSectionProperties.");
			
			LayoutLocalServiceUtil.updateSectionProperties(pageTemplate.getGroupId(), pageTemplate.getLayoutId(), menu, header, footer, null, null);
			
			if(_log.isDebugEnabled())
				_log.debug("SectionProperties upadted.");
		}
		else if(_log.isDebugEnabled())
			_log.debug("No section properties to update.");
	}
	
	public void setDefaultPageTemplate(long id) throws SystemException {
		PageTemplate pageTemplate = pageTemplatePersistence.fetchByPrimaryKey(id);
		
		if (pageTemplate != null) {
			
			PageTemplate selectedPageTemplate = getDefaultPageTemplate(pageTemplate.getGroupId(), pageTemplate.getType());
			
			if (selectedPageTemplate != null)
			{
				selectedPageTemplate.setDefaultTemplate(false);
				
				pageTemplatePersistence.update(selectedPageTemplate, false);
			}
			
			pageTemplate.setDefaultTemplate(true);
			
			pageTemplatePersistence.update(pageTemplate, false);			
		}
	}
	
	
	/*
	 * FINDER Funtions
	 */
	public PageTemplate getPageTemplateById(long id) throws SystemException {
		return pageTemplatePersistence.fetchByPrimaryKey(id);
	}
	
	public PageTemplate getDefaultPageTemplate(long groupId, String type) throws SystemException
	{
		PageTemplate pt = null;
		
		List<PageTemplate> pageTemplates = pageTemplatePersistence.findByGroupIdTypeDefaultTemplate(groupId, type, true);
		
		if (pageTemplates != null && pageTemplates.size() > 0)
			pt =  pageTemplates.get(0);
		
		return pt;
	}
	
	public PageTemplate getDefaultMobilePageTemplate(long groupId, String type) throws SystemException 
	{
		PageTemplate pt = null;
		
		List<PageTemplate> pageTemplates = pageTemplatePersistence.findByGroupIdTypeDefaultMobileTemplate(groupId, type, true);
		
		if (pageTemplates != null && pageTemplates.size() > 0)
			pt = pageTemplates.get(0);
		
		return pt;
	}
	
	public List<PageTemplate> getPageTemplates(long groupId) throws SystemException {
		return pageTemplatePersistence.findByGroupId(groupId);
	}
	
	public List<PageTemplate> getPageTemplates(long groupId, int start, int end) throws SystemException {
		return pageTemplatePersistence.findByGroupId(groupId, start, end);
	}
	
	public int getPageTemplatesCount(long groupId) throws SystemException {
		return pageTemplatePersistence.countByGroupId(groupId);
	}

	public PageTemplate getPageTemplateByLayoutId(long groupId, long layoutId) throws SystemException {
		return pageTemplatePersistence.fetchByGroupIdLayoutId(groupId, layoutId);
	}		
	
	public PageTemplate getPageTemplateByPageTemplateId(long groupId, String pageTemplateId) throws SystemException {
		return pageTemplatePersistence.fetchByGroupIdPageTemplateId(groupId, pageTemplateId);
	}
	
	public int getPageTemplatesByTypeCount(long groupId, String type) throws SystemException { 
		return pageTemplatePersistence.countByGroupIdType(groupId, type);
	}
	
	public List<PageTemplate> getPageTemplatesByType(long groupId, String type) throws SystemException { 
		return pageTemplatePersistence.findByGroupIdType(groupId, type);
	}
	
	public List<PageTemplate> getPageTemplatesByType(long groupId, String type, int start, int end) throws SystemException { 
		return pageTemplatePersistence.findByGroupIdType(groupId, type, start, end);
	}
	
	
	public int getPageTemplatesCount(long groupId, String type, String keyword) throws SystemException {

		//Listado de articulos
		ClassLoader cl = PortalClassLoaderUtil.getClassLoader();				
		
		// Buscar por group, structureId, status 
		DynamicQuery query = DynamicQueryFactoryUtil.forClass(PageTemplateImpl.class, cl);
		
		query.add(PropertyFactoryUtil.forName("type").eq(type));
		query.add(PropertyFactoryUtil.forName("groupId").eq(groupId));
		
		if (keyword != null && !keyword.equals("")) {
			String[] keywords = keyword.split(" ");
			Junction junction = RestrictionsFactoryUtil.disjunction();

			for (int i = 0; i < keywords.length; i++) {
				junction.add(PropertyFactoryUtil.forName("name").like("%" + keywords[i] + "%"));
				junction.add(PropertyFactoryUtil.forName("description").like("%" + keywords[i] + "%"));
			}
			query.add(junction);
		}
		
		return (int) pageTemplatePersistence.countWithDynamicQuery(query);
	}
	
	public List<PageTemplate> getPageTemplates(long groupId, String type, String keyword, int start, int end) throws SystemException {				
		//Listado de articulos
		ClassLoader cl = PortalClassLoaderUtil.getClassLoader();				
		
		// Buscar por group, structureId, status 
		DynamicQuery query = DynamicQueryFactoryUtil.forClass(PageTemplateImpl.class, cl);
		
		query.add(PropertyFactoryUtil.forName("type").eq(type));
		query.add(PropertyFactoryUtil.forName("groupId").eq(groupId));
		
		if (keyword != null && !keyword.equals("")) {
			String[] keywords = keyword.split(" ");
			Junction junction = RestrictionsFactoryUtil.disjunction();

			for (int i = 0; i < keywords.length; i++) {
				junction.add(PropertyFactoryUtil.forName("name").like("%" + keywords[i] + "%"));
				junction.add(PropertyFactoryUtil.forName("description").like("%" + keywords[i] + "%"));
			}
			query.add(junction);
		}
		
		return pageTemplatePersistence.findWithDynamicQuery(query, start, end);
	}
	
	
	public String getPageTemplateURL(long id) {

		String url = "";
		try {			
			if (id!=-1){
				PageTemplate pageTemplate = pageTemplatePersistence.findByPrimaryKey(id);
				Layout l = LayoutLocalServiceUtil.getLayout(pageTemplate.getLayoutId());
				url = l.getFriendlyURL();
			}
		} catch (Exception e) {
			_log.error(e);			
		}
		
		return url;
	}
	
	/*
	 * 
	 */
	
	//Utilizado por MILENIUM
	public void deletePageTemplateId(long id) throws SystemException, PortalException, ServiceError
	{
		PageTemplate page = pageTemplatePersistence.fetchByPrimaryKey(id);
		
		if (page != null) 
		{
			boolean existInLive = LiveLocalServiceUtil.getExistsInLive(page.getGroupId(), IterKeys.CLASSNAME_PAGETEMPLATE, page.getPageTemplateId());
			ErrorRaiser.throwIfFalse( !existInLive , IterErrorKeys.XYZ_E_PAGETEMPLATE_EXISTS_IN_LIVE_ZYX);
			
			LayoutLocalServiceUtil.deleteLayout(page.getLayoutId());
		
			//Insercion en la tabla de Live
			itemXmlIO.deleteLiveEntry(page);
			
			pageTemplatePersistence.remove(id);
		}
	}
	
	public void clearLayout(long userId, long layoutId) throws PortalException, SystemException 
	{
		Layout layout = LayoutLocalServiceUtil.getLayout(layoutId);
		layout = com.protecmedia.iter.news.service.LayoutLocalServiceUtil.removeLayoutPortlets(layout);
		try
		{
			LayoutLocalServiceUtil.updateSectionProperties( layout.getGroupId(), layoutId, "-1", "-1", "-1", null, null);
		}
		catch (Exception e)
		{
			throw new SystemException(e);
		}
	}

	/*
	 * 
	 */
	public PageTemplate createPageTemplate(long userId, long groupId, String name, String description, String type, long layoutId, long layoutTemplateId) throws PortalException, SystemException, ReadOnlyException, ValidatorException, IOException, SQLException 
	{
		PageTemplate pageTemplate = addPageTemplate(groupId, layoutTemplateId, name, description, type);
		
		if (pageTemplate != null) 
			com.protecmedia.iter.news.service.LayoutLocalServiceUtil.copyLayout(layoutId, layoutTemplateId, true, true);
		
		return pageTemplate;
	}
	
	/*
	 * 
	 */
	public PageTemplate loadPageTemplate(long userId, long groupId, long id, long layoutId) throws PortalException, SystemException, ServiceError, ReadOnlyException, ValidatorException, IOException, SQLException
	{
		return _loadPageTemplate(userId, groupId, id, layoutId, false);
	}
	private PageTemplate _loadPageTemplate(long userId, long groupId, long id, long layoutId, boolean copyWebResources) throws PortalException, SystemException, ServiceError, ReadOnlyException, ValidatorException, IOException, SQLException
	{
		if(_log.isDebugEnabled())
			_log.debug( String.format("loadPageTemplate id=%s layoutId=%s", id, layoutId) );
	
		userId = GroupMgr.getDefaultUserId();
		
		PageTemplate pageTemplate = pageTemplatePersistence.fetchByPrimaryKey(id);
		ErrorRaiser.throwIfNull(pageTemplate, IterErrorKeys.XYZ_E_PAGETEMPLATE_NOT_FOUND_ZYX);

		com.protecmedia.iter.news.service.LayoutLocalServiceUtil.copyLayout(pageTemplate.getLayoutId(), layoutId, true, copyWebResources);
		
		if(_log.isDebugEnabled())
			_log.debug( "PageTemplate loaded" );
		
		return pageTemplate;
	}
	
	public boolean compareLayoutPageTemplate(long groupId, long plid, String pageTemplateId) throws Exception
	{
		try
		{
			//Actual layout
			Layout actualLayout = LayoutLocalServiceUtil.getLayout(plid);
			UnicodeProperties typeSettingsLayout = actualLayout.getTypeSettingsProperties();
			//PageTemplate
			PageTemplate pageTemplate;
			
			//pageTemplate = pageTemplatePersistence.findByGroupIdPageTemplateId(groupId, pageTemplateId);
			pageTemplate = pageTemplatePersistence.findByPrimaryKey(Long.valueOf(pageTemplateId));
			
			Layout pageTemplateLayout = LayoutLocalServiceUtil.getLayout(pageTemplate.getLayoutId());
			UnicodeProperties typeSettingsPageTemplate = pageTemplateLayout.getTypeSettingsProperties();
			
			long layoutPlid = plid;
			long pageTemplatePlid = pageTemplateLayout.getPlid();
			
			//Recorrido superficial (Sin mirar las preferencias de los portlets)
			if (!compareLayoutsStructure(layoutPlid,pageTemplatePlid,typeSettingsLayout,typeSettingsPageTemplate,"superficial")){
				return false;
			}
			else{
				//Recorrido profundo (Mirando la configuracion de los portlets)
				if (!compareLayoutsStructure(layoutPlid,pageTemplatePlid,typeSettingsLayout,typeSettingsPageTemplate,"deep")){
					return false;
				}
			}
			
			return true;
			
		} catch (NoSuchPageTemplateException e) {
			//No existe el templateId
			throw new Exception(e);
			
		} catch (SystemException e) {
			throw new Exception(e);
		}
		
	}
	
	
	private boolean compareLayoutsStructure(long layoutPlid, long pageTemplatePlid, UnicodeProperties typeSettingsLayout, UnicodeProperties typeSettingsPageTemplate, String type) throws SystemException, PortalException{
		
		Set<String> layoutSet = typeSettingsLayout.keySet();
		Set<String> pageTemplateSet = typeSettingsPageTemplate.keySet();
		
		//Si no tienen el mismo numero de portlets, entonces no son iguales
		if(layoutSet.size() != pageTemplateSet.size())
		{
			return false;
		}
		else
		{
			Iterator<String> layoutIterator = layoutSet.iterator();
			Iterator<String> pageTemplateIterator = pageTemplateSet.iterator();
			while (layoutIterator.hasNext() && pageTemplateIterator.hasNext()){
				String layoutKey = layoutIterator.next();
				String pageTemplateKey = pageTemplateIterator.next();
				if (!layoutKey.equals(pageTemplateKey)){
					return false;
				}
				String layoutColumn = typeSettingsLayout.getProperty(layoutKey);
				String pageTemplateColumn = typeSettingsPageTemplate.getProperty(pageTemplateKey);
				if (!sameColumnContent(layoutPlid,pageTemplatePlid,layoutColumn,pageTemplateColumn,type)){
					return false;
				}
			}
		}
		
		return true;
	}
	
	private boolean compareArrays(String[] array1, String[] array2){
		
		if ((array1 == null) && (array2 == null)){
			return true;
		}
		if ((array1 == null) || (array2 == null)){
			return false;
		}
		if (array1.length != array2.length){
			return false;
		}
		for (int a=0;a<array1.length;a++){
			if (!array1[a].equals(array2[a])){
				return false;
			}
		}
		
		return true;
	}
	
	
	private boolean samePreferences(long layoutPlid, long pageTemplatePlid, String portlet1, String portlet2) throws SystemException, PortalException{
		
		Layout layout1 = LayoutLocalServiceUtil.getLayout(layoutPlid);
		Layout layout2 = LayoutLocalServiceUtil.getLayout(pageTemplatePlid);
		
		PortletPreferences pref1 = PortletPreferencesFactoryUtil.getLayoutPortletSetup(layout1, portlet1);
		PortletPreferences pref2 = PortletPreferencesFactoryUtil.getLayoutPortletSetup(layout2, portlet2);
		Map<String, String[]> map1 = pref1.getMap();
		Map<String, String[]> map2 = pref2.getMap();
		if (map1.size() != map2.size()){
			return false;
		}
		for (String key : map1.keySet()){
			if (map2.containsKey(key)){
				String[] value1 = map1.get(key);
				String[] value2 = map2.get(key);
				if (!compareArrays(value1, value2)){
					return false;
				}
			}
			else{
				return false;
			}
		}

		return true;
	}
	
	
	private boolean sameColumnContent(long layoutPlid, long pageTemplatePlid, String column1, String column2, String type) throws SystemException, PortalException{
		StringTokenizer st1 = new StringTokenizer(column1,",");
		StringTokenizer st2 = new StringTokenizer(column2,",");
		while (st1.hasMoreTokens() && st2.hasMoreTokens()){
			String instance1 = st1.nextToken();
			String instance2 = st2.nextToken();
			StringTokenizer st11 = new StringTokenizer(instance1,"_");
			StringTokenizer st22 = new StringTokenizer(instance2,"_");
			if (st11.hasMoreTokens() && st22.hasMoreTokens()){
				String mainName1 = st11.nextToken();
				String mainName2 = st22.nextToken();
				if (!mainName1.equals(mainName2)){
					return false;
				}
				//Miro si las preferencias de los portlets son las mismas
				if (type.equals("deep")){
					if (!samePreferences(layoutPlid,pageTemplatePlid,instance1,instance2)){
						return false;
					}
				}
			}
			
		}
		return !(st1.hasMoreTokens() || st2.hasMoreTokens());
	}
	
	/**
	 * Establece el Template por defecto indicado por parametro.
	 * Solo sirve para article-template.
	 * 
	 * @param pageTemplateId_ el id_ a establecer por defecto.
	 * @throws Exception si pageTemplateId es nulo o no es valido.
	 */
	public void setDefaultPageTemplate(String pageTemplateId_) throws Exception
	{
		_log.trace("In setDefaultPageTemplate");
		
		// Comprueba si la entrada es valida y en caso afirmativo recoge el groupId asociado.
		String groupId = _checkPageTemplate(pageTemplateId_);
		
		List<PageTemplate> oldDefaultPgTmplList = pageTemplatePersistence.findByGroupIdTypeDefaultTemplate(Long.valueOf(groupId), IterKeys.DESIGNER_PAGE_TEMPLATE_ARTICLE_TYPE, true);
		if(oldDefaultPgTmplList != null)
			for(PageTemplate pgTmpl : oldDefaultPgTmplList)
			{
				pgTmpl.setDefaultTemplate(false);
				pageTemplatePersistence.update(pgTmpl, false);
			}
		
		PageTemplate newDefaultPgTmpl = pageTemplatePersistence.fetchByPrimaryKey( Long.valueOf(pageTemplateId_) );
		newDefaultPgTmpl.setDefaultTemplate(true);
		pageTemplatePersistence.update(newDefaultPgTmpl, false);

		//Insercion en la tabla de Live
		itemXmlIO.createLiveEntry(newDefaultPgTmpl);
	}
	
	/**
	 * Establece el mobileTemplate por defecto indicado por parametro.
	 * Solo sirve para article-template.
	 * 
	 * @param pageTemplateId el id_ a establecer por defecto.
	 * @throws Exception si pageTemplateId es nulo o no es valido.
	 */
	public void setDefaultPageTemplateMobile(String pageTemplateId) throws Exception
	{
		_log.trace("In setDefaultPageTemplateMobile");
		
		// Comprueba si la entrada es valida y en caso afirmativo recoge el groupId asociado.
		String groupId = _checkPageTemplate(pageTemplateId);
		
		List<PageTemplate> oldDefaultPgTmplList = pageTemplatePersistence.findByGroupIdTypeDefaultMobileTemplate(Long.valueOf(groupId), IterKeys.DESIGNER_PAGE_TEMPLATE_ARTICLE_TYPE, true);
		if(oldDefaultPgTmplList!=null)
			for(PageTemplate pgTmpl : oldDefaultPgTmplList)
			{
				pgTmpl.setDefaultMobileTemplate(false);
				pageTemplatePersistence.update(pgTmpl, false);
			}
		
		PageTemplate newDefaultPgTmpl = pageTemplatePersistence.fetchByPrimaryKey( Long.valueOf(pageTemplateId) );
		newDefaultPgTmpl.setDefaultMobileTemplate(true);
		pageTemplatePersistence.update(newDefaultPgTmpl, false);

		//Insercion en la tabla de Live
		itemXmlIO.createLiveEntry(newDefaultPgTmpl);
	}
	
	/**
	 * Comprueba que el page template es valido, lanzando una excepcion de tipo XYZ_E_INVALIDARG_ZYX
	 * en caso negativo. Si es valido, retorna el groupId al que esta asociado.
	 * 
	 * @param pageTemplateId el ID del pageTemplate a comprobar.
	 * @return el groupId del Pagetemplate.
	 * @throws Exception si el parametro pageTemplateId es nulo o si no es valido.
	 */
	private String _checkPageTemplate(String pageTemplateId) throws Exception
	{
		ErrorRaiser.throwIfFalse(Validator.isNotNull(pageTemplateId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid pageTemplateId");
		
		// Comprueba que el modelo dado es válido (existe y es de tipo article-template).
		String sql = String.format(CHECK_NEXT_DEFAULT_PAGETEMPLATE_EXISTS, pageTemplateId);
		if (_log.isDebugEnabled())
			_log.debug(new StringBuilder("Query to check the pagetemplate given:\n").append(sql));
		Node row = PortalLocalServiceUtil.executeQueryAsDom(sql).getRootElement().selectSingleNode("//row");
		
		ErrorRaiser.throwIfNull(row, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid pagetemplate given");
		// Retorna el groupId del template.
		String groupId = XMLHelper.getTextValueOf(row, "@groupId");
		return groupId;
	}
}