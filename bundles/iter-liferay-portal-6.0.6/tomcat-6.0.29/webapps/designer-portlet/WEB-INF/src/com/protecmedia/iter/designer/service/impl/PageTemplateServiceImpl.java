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
import java.util.List;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.designer.NoSuchPageTemplateException;
import com.protecmedia.iter.designer.model.PageTemplate;
import com.protecmedia.iter.designer.service.base.PageTemplateServiceBaseImpl;
import com.protecmedia.iter.news.NoSuchPageContentException;

/**
 * @author Protecmedia
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class PageTemplateServiceImpl extends PageTemplateServiceBaseImpl {
		
	public List<PageTemplate> getPageTemplates(long groupId) throws Exception {
		try{
			return pageTemplateLocalService.getPageTemplates(groupId);
		} catch(Exception ex){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_GET_PAGETEMPLATES_ZYX, ex));
		}
	}
		
	public List<PageTemplate> getPageTemplatesType(long groupId, String type) throws Exception {
		try{
			return pageTemplateLocalService.getPageTemplatesByType(groupId, type);
		} catch(Exception ex){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_GET_PAGETEMPLATES_BY_TYPE_ZYX, ex));
		}
	}
	
	public PageTemplate getPageTemplateByPageTemplateId(long groupId, String pageTemplateId) throws Exception {
		try{
			return pageTemplateLocalService.getPageTemplateByPageTemplateId(groupId, pageTemplateId);
		} catch(Exception ex){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_GET_PAGETEMPLATES_BY_TEMPLATEID_ZYX, ex));
		}
	}		
	
	public PageTemplate loadPageTemplate(long userId, long groupId, long pageTemplateId, long layoutId) throws Exception
	{
		try
		{
			return pageTemplateLocalService.loadPageTemplate(userId, groupId, pageTemplateId, layoutId);
		}
		catch(Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public PageTemplate addPageTemplate(long groupId, String name, String description, String type, long userId) throws Exception {
		try{
			return pageTemplateLocalService.createPageTemplate(groupId, name, description, type, userId);
		} catch(Exception ex){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_ADD_PAGETEMPLATE_ZYX, ex));
		}
	}
	
	public PageTemplate addPageTemplate2(long groupId, String name, String description, String type,long userId, String header, String footer, String menu) throws Exception 
	{
		return addPageTemplate3(groupId, name, description, type, userId, header, footer, menu, null);
	}
	
	public PageTemplate addPageTemplate3(long groupId, String name, String description, String type,long userId, String header, String footer, String menu, String extraData) throws Exception 
	{
		try
		{
			return pageTemplateLocalService.createPageTemplate(groupId, name, description, type, userId, header, footer, menu, extraData);
		}
		catch(Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}	
	}

	public PageTemplate addPageTemplateParentId(long groupId, String name, String description, String type, long userId, long parentId) throws Exception {
		try{
			return pageTemplateLocalService.createPageTemplate_PI_DT(groupId, name, description, type, userId, parentId, false);
		} catch(Exception ex){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_ADD_PAGETEMPLATE_PARENTID_ZYX, ex));
		}
	
	}
	
	public void deletePageTemplate(long pageTemplateId) throws SystemException
	{
		try
		{
			pageTemplateLocalService.deletePageTemplateId(pageTemplateId);
		}
		catch(Throwable th)
		{
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(th) );
//			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_DELETE_PAGETEMPLATE_ZYX, ex));
		}
	}
	
	public void clearLayout(long userId, long layoutId) throws Exception {
		try{
			pageTemplateLocalService.clearLayout(userId, layoutId);
		} catch(Exception ex){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_CLEAR_LAYOUT_ZYX, ex));
		}
	}
	
	public PageTemplate updatePageTemplateName(long pageTemplateId, String name) throws Exception {
		try{
			return pageTemplateLocalService.updatePageTemplateName(pageTemplateId, name);
		} catch(Exception ex){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_UPDATE_PAGETEMPLATE_NAME_ZYX, ex));
		}
	}
	
	public void updatePageTemplate(long id, String name, String header, String footer, String menu) throws Exception
	{
		try
		{
			pageTemplateLocalService.updatePageTemplate(id, name, header, footer, menu);
		}
		catch(Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void updatePageTemplate1(long id, String name, String header, String footer, String menu, String extraData) throws Exception
	{
		try
		{
			pageTemplateLocalService.updatePageTemplate1(id, name, header, footer, menu, extraData);
		}
		catch(Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String getURLPageTemplate(long pageTemplateId) throws Exception{
		try{
			return pageTemplateLocalService.getPageTemplateURL(pageTemplateId);
		} catch(Exception ex){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_GET_URL_PAGETEMPLATE_ZYX, ex));
		}
	}
	
	public boolean compareLayoutPageTemplate(long groupId, long plid, String pageTemplateId) throws Exception
	{
		try
		{
			return pageTemplateLocalService.compareLayoutPageTemplate(groupId, plid, pageTemplateId);
		}
		catch(Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void setDefaultPageTemplate(String pageTemplateId_) throws Exception
	{
		try
		{
			pageTemplateLocalService.setDefaultPageTemplate(pageTemplateId_);
		}
		catch(Exception ex)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(ex));
		}
	}	
	
	public void setDefaultPageTemplateMobile(String pageTemplateId) throws Exception
	{
		try
		{
			pageTemplateLocalService.setDefaultPageTemplateMobile(pageTemplateId);
		}
		catch(Exception ex)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(ex));
		}
	}		
}