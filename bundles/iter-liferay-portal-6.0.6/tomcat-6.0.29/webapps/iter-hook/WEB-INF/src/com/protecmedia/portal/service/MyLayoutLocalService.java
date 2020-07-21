/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.portal.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.liferay.portal.NoSuchLayoutException;
import com.liferay.portal.RequiredLayoutException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.service.LayoutLocalService;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceWrapper;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.persistence.LayoutUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.journalcontent.util.JournalContentUtil;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.designer.service.PageTemplateLocalServiceUtil;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;
import com.protecmedia.iter.services.service.ServiceLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.portal.LayoutXmlIO;


public class MyLayoutLocalService extends LayoutLocalServiceWrapper {
	
	private static Log _log = LogFactoryUtil.getLog(MyLayoutLocalService.class);
	private static ItemXmlIO itemXmlIO = new LayoutXmlIO();
	
	private static String GET_ARTICLE_BY_PLID = "SELECT contentId FROM News_PageContent WHERE layoutId='%s'";
	
	public MyLayoutLocalService(LayoutLocalService layoutLocalService) throws SystemException {
		super(layoutLocalService);
	}
	
	
	private Layout updateLinkToLayoutId(Layout layout, boolean update) throws SystemException
	{
		if ( layout.getType().equals(LayoutConstants.TYPE_LINK_TO_LAYOUT) )
		{
			layout.setIconImageId( GetterUtil.getLong( layout.getTypeSettingsProperties().getProperty(LayoutConstants.TYPESETTINGS_LINK_TO_LAYOUT_ID), 0) );
			
			if (update)
				layout = super.updateLayout(layout, false);
		}
		return layout;
	}

	/*
	 * Add Functions 
	 * --------------
	 */
	
	@Override
	public Layout addLayout(Layout layout) throws SystemException 
	{
		Layout _layout = null;
		
		try
		{			
			layout = updateLinkToLayoutId(layout, false);
				
			_layout = super.addLayout(layout);
			
			updateFriendlyURL(_layout.getGroupId(), _layout.getPrivateLayout(), _layout.getParentLayoutId(), _layout);
			
			//Add to Live
			itemXmlIO.createLiveEntry(layout);
							
		}
		catch(Exception e)
		{
			_log.error(e);
		}
		
		return _layout;			
	}
			
	@Override	
	public Layout addLayout(long userId, long groupId, boolean privateLayout,
			long parentLayoutId, Map<Locale, String> localeNamesMap,
			Map<Locale, String> localeTitlesMap, String description,
			String type, boolean hidden, String friendlyURL, long dlFolderId,
			ServiceContext serviceContext) throws PortalException,
			SystemException {

		Layout layout = null;
		
		try{
			
			layout = super.addLayout(userId, groupId, privateLayout, parentLayoutId,
					localeNamesMap, localeTitlesMap, description, type, hidden,
					friendlyURL, dlFolderId, serviceContext);
			
			layout = updateLinkToLayoutId(layout, true);
			
			if (friendlyURL == null || friendlyURL.equals("")){
				updateFriendlyURL(groupId, privateLayout, parentLayoutId, layout);
			}
			
			//Add to Live
			itemXmlIO.createLiveEntry(layout);
			
		}catch(Exception e){
			_log.error(e);
		}
		
		return layout;
	}
	
	
	//Utilizada por: INTERFAZ GRAFICA y MILENIUM
	@Override
	public Layout addLayout(long userId, long groupId, boolean privateLayout,
			long parentLayoutId, Map<Locale, String> localeNamesMap,
			Map<Locale, String> localeTitlesMap, String description,
			String type, boolean hidden, String friendlyURL,
			ServiceContext serviceContext) throws PortalException,
			SystemException {

		Layout layout = null;
		
		layout = super.addLayout(userId, groupId, privateLayout, parentLayoutId,
				localeNamesMap, localeTitlesMap, description, type, hidden,
				friendlyURL, serviceContext);
		
		layout = updateLinkToLayoutId(layout, true);
		
		if (friendlyURL == null || friendlyURL.equals("")){
			updateFriendlyURL(groupId, privateLayout, parentLayoutId, layout);
		}			
		
		//Add to Live
		itemXmlIO.createLiveEntry(layout);

		return layout;
	}
	
	@Override
	public Layout addLayout(long userId, long groupId, boolean privateLayout,
			long parentLayoutId, String name, String title, String description,
			String type, boolean hidden, String friendlyURL, long dlFolderId,
			ServiceContext serviceContext) throws PortalException,
			SystemException {

		Layout layout = null;
		
		try{			
			layout = super.addLayout(userId, groupId, privateLayout, parentLayoutId, name,
					title, description, type, hidden, friendlyURL, dlFolderId,
					serviceContext);
			
			layout = updateLinkToLayoutId(layout, true);
			
			if (friendlyURL == null || friendlyURL.equals("")){
				updateFriendlyURL(groupId, privateLayout, parentLayoutId, layout);
			}
			
			//Add to Live
			itemXmlIO.createLiveEntry(layout);
			
		}catch(Exception e){
			_log.error(e);
		}
		
		return layout;
	}
	
	//Executed by the import
	@Override
	public Layout addLayout(long userId, long groupId, boolean privateLayout,
			long parentLayoutId, String name, String title, String description,
			String type, boolean hidden, String friendlyURL,
			ServiceContext serviceContext) throws PortalException,
			SystemException {

		Layout layout = null;
		
		layout = super.addLayout(userId, groupId, privateLayout, parentLayoutId, name,
				title, description, type, hidden, friendlyURL, serviceContext);
		
		layout = updateLinkToLayoutId(layout, true);

		if (friendlyURL == null || friendlyURL.equals("")){
			updateFriendlyURL(groupId, privateLayout, parentLayoutId, layout);
		}

		//Add to Live
		itemXmlIO.createLiveEntry(layout);

		
		return layout;
	}
	
	/**
	 * @param groupId
	 * @param privateLayout
	 * @param parentLayoutId
	 * @param layout
	 * @throws PortalException
	 * @throws SystemException
	 */
	private void updateFriendlyURL(long groupId, boolean privateLayout,	long parentLayoutId, Layout layout) 
			throws PortalException,	SystemException
	{
		String newFriendlyURL = layout.getFriendlyURL().substring( layout.getFriendlyURL().lastIndexOf(StringPool.SLASH) );
		
		if(parentLayoutId != LayoutConstants.DEFAULT_PARENT_LAYOUT_ID)
		{
			Layout parentLayout = super.getLayout(groupId, privateLayout, parentLayoutId);
			
			if(	!( parentLayout != null && 
				   parentLayout.getType().equalsIgnoreCase(IterKeys.CUSTOM_TYPE_TEMPLATE) && 
				   parentLayout.getFriendlyURL().equals(IterKeys.PARENT_LAYOUT_URL))
				 )
			{
				newFriendlyURL = parentLayout.getFriendlyURL() + newFriendlyURL;
			}
		}
		
		super.updateFriendlyURL(layout.getPlid(), newFriendlyURL);
	}

	
	/*
	 * Delete Functions 
	 * -----------------
	 */
	
	//Este es el que se ejecuta por la interfaz gráfica
	@Override
	public void deleteLayout(long groupId, boolean privateLayout, long layoutId){
		
		try {
			Layout layout = LayoutLocalServiceUtil.getLayout(groupId, privateLayout, layoutId);
			
			if( PageTemplateLocalServiceUtil.getPageTemplateByLayoutId(groupId, layout.getPlid()) == null){
				updateLive(layout);
				
				super.deleteLayout(groupId, privateLayout, layout.getLayoutId());
				
				deleteLayoutDependencies(layout);
			}
			else{
				// TODO Enviar el error a capas superiores para mostrarselo al usuario
				_log.warn("Can't delete layout, is associated with a page template ");
			}
		} catch (Exception e) {
			_log.error(e);
		}
	}
	
	//Este es el que se ejecuta por la importación
	@Override
	public void deleteLayout(Layout layout, boolean updateLayoutSet)
	throws PortalException, SystemException {
		
		try {		
						
			if( PageTemplateLocalServiceUtil.getPageTemplateByLayoutId(layout.getGroupId(), layout.getPlid()) == null){
				updateLive(layout);
				
				super.deleteLayout(layout, updateLayoutSet);
				
				deleteLayoutDependencies(layout);
			}
			else{
				// TODO Enviar el error a capas superiores para mostrarselo al usuario
				_log.warn("Can't delete layout, is associated with a page template");
			}
			
		} catch (SystemException e) {
			_log.error("Live Error", e);
		}
	
	}
	
	//Utilizada por MILENIUM
	//También se ejecuta para borrar una página cuando borramos su Page Template
	public void deleteLayout(long plid) throws PortalException, SystemException
	{
		try
		{
			Layout layout = LayoutLocalServiceUtil.getLayout(plid);
			
			String query = String.format(GET_ARTICLE_BY_PLID, layout.getUuid());
			List<Object> contentIds = PortalLocalServiceUtil.executeQueryAsList(query);

			itemXmlIO.deleteLiveEntry(layout);
			
			deleteLayoutDependencies(layout);
			
			if(contentIds != null && contentIds.size() > 0)
			{
				for(Object currentId:contentIds)
					JournalContentUtil.reindexContent(GroupMgr.getGlobalGroupId(), currentId.toString());
			}
		}
		catch (Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		super.deleteLayout(plid);	
	}
	
	/*
	 * UPDATE Functions
	 */
	
	//Este es el que se ejecuta por la importación de layouts
	@Override
	public Layout updateLayout(Layout layout) throws SystemException {
		
		Layout _layout = null;
		
		try
		{
			layout = updateLinkToLayoutId(layout, false);
			
			_layout = super.updateLayout(layout);
			
//			long groupId = _layout.getGroupId();
//			
//			boolean privateLayout = _layout.getPrivateLayout();
//			
//			long layoutId = _layout.getLayoutId();
//								
//			updateChildFriendlyURL(groupId, privateLayout, layoutId);
		
			//Add to Live
			itemXmlIO.createLiveEntry(layout);
			
		}
		catch(PortalException e)
		{
			throw new SystemException(e);
		}
		
		return _layout;
	}
	
	@Override
	public Layout updateLayout(Layout layout, boolean merge)
			throws SystemException {
		
		Layout _layout = null;
		
		try
		{
			layout = updateLinkToLayoutId(layout, false);
			
			_layout = super.updateLayout(layout, merge);		
			
//			long groupId = _layout.getGroupId();
//			
//			boolean privateLayout = _layout.getPrivateLayout();
//			
//			long layoutId = _layout.getLayoutId();
//			
//			updateChildFriendlyURL(groupId, privateLayout, layoutId);
			
			//Add to Live
			itemXmlIO.createLiveEntry(layout);

		}
		catch(Exception e)
		{
			_log.error(e);
		}
		
		return _layout;		
	}
	
	
	//Este es el que se ejecuta por la interfaz gráfica
	@Override
	public Layout updateLayout(long groupId, boolean privateLayout,
			long layoutId, long parentLayoutId,
			Map<Locale, String> localeNamesMap,
			Map<Locale, String> localeTitlesMap, String description,
			String type, boolean hidden, String friendlyURL, Boolean iconImage,
			byte[] iconBytes, ServiceContext serviceContext)
			throws PortalException, SystemException 
	{
		Layout _layout = null;
		
		_layout = super.updateLayout(groupId, privateLayout, layoutId, parentLayoutId,
				localeNamesMap, localeTitlesMap, description, type, hidden,
				friendlyURL, iconImage, iconBytes, serviceContext);		
		
		_layout = updateLinkToLayoutId(_layout, true);
		
//		updateChildFriendlyURL(groupId, privateLayout, layoutId);
		
		Layout layout = LayoutLocalServiceUtil.getLayout(groupId, privateLayout, layoutId);
		
		//Add to Live
		itemXmlIO.createLiveEntry(layout);
		
		return _layout;				
	}
	
	@Override
	public Layout updateLayout(long groupId, boolean privateLayout,
			long layoutId, long parentLayoutId,
			Map<Locale, String> localeNamesMap,
			Map<Locale, String> localeTitlesMap, String description,
			String type, boolean hidden, String friendlyURL,
			ServiceContext serviceContext) throws PortalException,
			SystemException {

		Layout _layout = null;
		
		try
		{
			_layout = super.updateLayout(groupId, privateLayout, layoutId, parentLayoutId,
					localeNamesMap, localeTitlesMap, description, type, hidden,
					friendlyURL, serviceContext);		
			
			_layout = updateLinkToLayoutId(_layout, true);
			
//			updateChildFriendlyURL(groupId, privateLayout, layoutId);
			
			Layout layout = LayoutLocalServiceUtil.getLayout(groupId, privateLayout, layoutId);
			
			//Add to Live
			itemXmlIO.createLiveEntry(layout);

		}
		catch(Exception e)
		{
			_log.error(e);
		}
		
		return _layout;
	}
	
	//Este es el que se ejecuta al mover, crear o borrar algún portlet
	@Override
	public Layout updateLayout(
			long groupId, boolean privateLayout, long layoutId,
			String typeSettings)
		throws PortalException, SystemException 
	{

		Layout layout = null;
		
		try
		{
			layout = super.updateLayout(groupId, privateLayout, layoutId, typeSettings);
			layout = updateLinkToLayoutId(layout, true);
			
//			updateChildFriendlyURL(groupId, privateLayout, layoutId);
				
			((LayoutXmlIO)itemXmlIO).createLiveEntryPortlet(layout);
			itemXmlIO.createLiveEntry(layout);
	
		}
		catch(Exception e)
		{
			_log.error(e);
		}

		return layout;
	}
	
	public Layout updateParentLayoutId(long plid, long parentPlid) throws PortalException, SystemException 
	{
		Layout l = super.updateParentLayoutId(plid, parentPlid);
		
		try
		{
			//Add to Live
			itemXmlIO.createLiveEntry(l);
		}
		catch(PortalException e)
		{
			_log.error("Live Error", e);
		}
		
		return l;
	}
	
	public Layout updateParentLayoutId( long groupId, boolean privateLayout, long layoutId, long parentLayoutId) throws PortalException, SystemException {
		
		Layout l = super.updateParentLayoutId(groupId, privateLayout, layoutId, parentLayoutId);
		
		updateFriendlyURL(groupId, privateLayout, parentLayoutId, l);
		
		try{
			//Add to Live
			itemXmlIO.createLiveEntry(l);
		}catch(PortalException e){
			_log.error("Live Error", e);
		}
		
		return l;
		
	}
	
	/*
	 * aUXILIAR fUNCTIONS
	 */
	
	@Override
	public Layout updateFriendlyURL(long plid, String friendlyURL)
			throws PortalException, SystemException {
		
		Layout _layout = super.updateFriendlyURL(plid, friendlyURL);		
		
//		long groupId = _layout.getGroupId();
//		
//		boolean privateLayout = _layout.getPrivateLayout();
//		
//		long layoutId = _layout.getLayoutId();
//		
//		updateChildFriendlyURL(groupId, privateLayout, layoutId);
		
		itemXmlIO.createLiveEntry(_layout);
		
		return _layout;		
	}
	
	/**
	 * @param groupId
	 * @param privateLayout
	 * @param layoutId
	 * @throws SystemException
	 * @throws PortalException
	 */
	private void updateChildFriendlyURL(long groupId, boolean privateLayout,
			long layoutId) throws SystemException, PortalException {
		List<Layout> top = new ArrayList<Layout>();
		
		List<Layout> layouts = super.getLayouts(groupId, privateLayout, layoutId);		
		top.addAll(layouts);
		
		if (top.size() > 0) {
			do {
				Layout _l = top.get(0);
				top.remove(0);
				
				Layout parentLayout = super.getLayout(groupId, privateLayout, _l.getParentLayoutId());
				
				String oldFriendlyURL = _l.getFriendlyURL();
				
				oldFriendlyURL = oldFriendlyURL.substring(oldFriendlyURL.lastIndexOf('/'));
	
				String newFriendlyURL = parentLayout.getFriendlyURL() + oldFriendlyURL;			
				
				super.updateFriendlyURL(_l.getPlid(), newFriendlyURL);
				
				layouts = super.getLayouts(groupId, privateLayout, _l.getLayoutId());	
				top.addAll(layouts);			
				
			} while (top.size() > 0);
		}
	}
	
	
	/**
	 * 
	 * @param layout
	 */
	private void updateLive(Layout layout){		
		try{
			try {
				//Delete from Live
				itemXmlIO.deleteLiveEntry(layout);
				
			} catch (PortalException e) {
				// TODO Auto-generated catch block
				_log.error(e);
			}
			
			List<Layout> childLayouts = LayoutLocalServiceUtil.getLayouts(layout.getGroupId(), layout.isPrivateLayout(),
					layout.getLayoutId());
			
			for (Layout childLayout : childLayouts) {
				updateLive(childLayout);
			}
			
		}catch (SystemException e) {
			_log.error(e);
		}

	}	
		
	//Utilizada por la GUI
	@Override
	public Layout updatePriority( long groupId, boolean privateLayout, long layoutId, int priority )throws PortalException, SystemException {

		Layout layout = super.updatePriority(groupId, privateLayout, layoutId, priority); //LayoutUtil.getPersistence().findByG_P_L(groupId, privateLayout, layoutId);

		List<Layout> orderedLayouts = getLayouts(layout.getGroupId(), layout.isPrivateLayout(), layout.getParentLayoutId());
		
		try{
			for (Layout l : orderedLayouts) {		
				//Add to Live
				itemXmlIO.createLiveEntry(l);
			}
		}catch(PortalException e){
			_log.error("Live Error", e);
		}
		
		return layout;
	}
	
	public Layout updatePriority(Layout layout, int priority) throws SystemException {
		
		Layout l = super.updatePriority(layout, priority);

		List<Layout> orderedLayouts = getLayouts(l.getGroupId(), l.isPrivateLayout(), l.getParentLayoutId());
		
		try{
			
			for (Layout ly : orderedLayouts) {
				//Add to Live
				itemXmlIO.createLiveEntry(ly);
			}
	
		}catch(PortalException e){
			_log.error("Live Error", e);
		}
		
		return l;
	}	
	
	
	public Layout updatePriority(long plid, int priority)
		throws PortalException, SystemException {
	
		Layout layout = LayoutUtil.findByPrimaryKey(plid);
	
		return updatePriority(layout, priority);
	}
	
	//Utilizada por MILENIUM
	public Layout updatePriority(Layout layout, int priority, boolean afterTarget) throws SystemException
	{
		Layout l = super.updatePriority(layout, priority, afterTarget);
		
		List<Layout> orderedLayouts = getLayouts(l.getGroupId(), l.isPrivateLayout(), l.getParentLayoutId());
		
		try
		{
			for (Layout ly : orderedLayouts)
				itemXmlIO.createLiveEntry(ly);
		}
		catch(PortalException e)
		{
			_log.error("Live Error", e);
		}
		
		return l;
	}
	
	private void deleteLayoutDependencies(Layout layout){

		try{
			// Borrar asignaciones (news_pagecontent) de todos los grupos
			PageContentLocalServiceUtil.deletePageContent(layout.getGroupId(), layout.getUuid());
		}catch(Exception e){
			_log.error("Layout " +  layout.getFriendlyURL() + ". Error removing associated PageContents", e);
		}
		
		try{
			// Borrar asignaciones de servicios (service_services) de todos los grupos
			ServiceLocalServiceUtil.deleteServiceByLinkId(layout.getGroupId(), layout.getPlid());
			
		}catch(Exception e){
			_log.error("Layout " +  layout.getFriendlyURL() + ". Error removing associated Services", e);
		}
	}
	
	//Actualiza la posicion de las páginas
	@Override
	public void setLayouts( long groupId, boolean privateLayout, long parentLayoutId, long[] layoutIds) throws PortalException, SystemException {
		
		
//		super.setLayouts(groupId, privateLayout, parentLayoutId, layoutIds);
		
		/**
		 * 	Se copia en método en lugar de llamar al del padre para que la llamada al deleteLayout use 
		 * el metodo de MyLayoutLocalService y realize el borrado en cascada de las páginas hijas y su entrada correspondiente en live. 
		 */
			
		try{
			if (layoutIds == null) {
				return;
			}
	
			if (parentLayoutId == LayoutConstants.DEFAULT_PARENT_LAYOUT_ID) {
				if (layoutIds.length < 1) {
					throw new RequiredLayoutException(
						RequiredLayoutException.AT_LEAST_ONE);
				}
	
				Layout layout = LayoutUtil.getPersistence().findByG_P_L(
					groupId, privateLayout, layoutIds[0]);
	
				if (!PortalUtil.isLayoutFirstPageable(layout.getType())) {
					throw new RequiredLayoutException(
						RequiredLayoutException.FIRST_LAYOUT_TYPE);
				}
	
				if (layout.isHidden()) {
					throw new RequiredLayoutException(
						RequiredLayoutException.FIRST_LAYOUT_HIDDEN);
				}
			}
	
			Set<Long> layoutIdsSet = new LinkedHashSet<Long>();
	
			for (int i = 0; i < layoutIds.length; i++) {
				layoutIdsSet.add(layoutIds[i]);
			}
	
			Set<Long> newLayoutIdsSet = new HashSet<Long>();
	
			List<Layout> layouts = LayoutUtil.getPersistence().findByG_P_P(
				groupId, privateLayout, parentLayoutId);
	
			for (Layout layout : layouts) {
				if (!layoutIdsSet.contains(layout.getLayoutId())) {
					deleteLayout(layout, true);
				}
				else {
					newLayoutIdsSet.add(layout.getLayoutId());
				}
			}
	
			int priority = 0;
	
			for (long layoutId : layoutIdsSet) {
				Layout layout = LayoutUtil.getPersistence().findByG_P_L(
					groupId, privateLayout, layoutId);
	
				layout.setPriority(priority++);
	
				LayoutUtil.getPersistence().update(layout, false);
			}
	
			LayoutSetLocalServiceUtil.updatePageCount(groupId, privateLayout);
				
		
			List<Layout> orderedLayouts = getLayouts(groupId, privateLayout, parentLayoutId);
			
			for (Layout l : orderedLayouts) {
				//Add to Live
				itemXmlIO.createLiveEntry(l);
			}
			
		}catch(Exception e){
			_log.error("setLayouts Error", e);
		}
		
	}
	
	@Override
	public Layout updateName(long groupId, boolean privateLayout, long layoutId, String name, String languageId) throws PortalException, SystemException {
		
		Layout layout = super.updateName(groupId, privateLayout, layoutId, name, languageId);
		
		// Add to Live
		itemXmlIO.createLiveEntry(layout);
		
		return layout;
	}
	
	@Override
	public Layout updateName(Layout layout, String name, String languageId)throws PortalException, SystemException {
		
		Layout l = super.updateName(layout, name, languageId);
		
		//Add to Live
		itemXmlIO.createLiveEntry(l);
		
		return l;
	}
	
	public Layout updateName(long plid, String name, String languageId) throws PortalException, SystemException {
		
		Layout l = super.updateName(plid, name, languageId);
		
		//Add to Live
		itemXmlIO.createLiveEntry(l);
		
		return l;
	}
	
	@Override
	public Layout updateLookAndFeel(long groupId, boolean privateLayout, long layoutId, String themeId, 
			String colorSchemeId, String css, boolean wapTheme)
					throws PortalException, SystemException
	{
		Layout layout = super.updateLookAndFeel(groupId, privateLayout, layoutId, themeId, 
				colorSchemeId, css, wapTheme);
		
		itemXmlIO.createLiveEntry(layout);

		return layout;
	}
	
	@Override
	public Layout getFriendlyURLLayout(long groupId, boolean privateLayout, String friendlyURL)	throws PortalException, SystemException 
	{
		Layout layout = null;
		
		try
		{
			layout = super.getFriendlyURLLayout(groupId, privateLayout, friendlyURL);
		}
		catch (NoSuchLayoutException e)
		{
			// Si no existe "/custompagestemplates" para el grupo, se crea
			// 0010562: Falla la publicación de newsletter pero desde la interfaz de MLN se indica lo contrario
			if (!privateLayout && IterKeys.PARENT_LAYOUT_URL.equals(friendlyURL))
			{
				ServiceContext serviceContext = new ServiceContext();
				
				layout = addLayout(GroupMgr.getDefaultUserId(), groupId, false, LayoutConstants.DEFAULT_PARENT_LAYOUT_ID, IterKeys.PARENT_LAYOUT_NAME, StringPool.BLANK,
						  StringPool.BLANK, IterKeys.CUSTOM_TYPE_TEMPLATE, true, IterKeys.PARENT_LAYOUT_URL, serviceContext);
			}
			else
				throw e;
		}
		
		return layout;
	}
	
}