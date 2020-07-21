/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.designer.portlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.service.ImageLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;
import com.protecmedia.iter.designer.model.PageTemplate;
import com.protecmedia.iter.designer.model.impl.PageTemplateImpl;
import com.protecmedia.iter.designer.service.PageTemplateLocalServiceUtil;
import com.protecmedia.iter.designer.service.item.PageTemplateXmlIO;
import com.protecmedia.iter.designer.util.DesignerUtil;
import com.protecmedia.iter.designer.util.PageTemplateValidator;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;

/**
 * Portlet implementation class ManageLayoutTemplatePortlet
 */
public class ManagePageTemplatePortlet extends MVCPortlet {
 
	private static ItemXmlIO itemXmlIO = new PageTemplateXmlIO();
	
	@Override
	public void init() throws PortletException { 
		super.init();
		
		editPageTemplate = getInitParameter("edit-page-template-jsp");
		infoPageTemplate = getInitParameter("info-page-template-jsp");
	}
	
	@Override
	public void doView(RenderRequest renderRequest,
			RenderResponse renderResponse) throws IOException, PortletException {

		String view = (String) renderRequest.getParameter("view");
		
		if (view == null || view.equals("")) {
			try {
				showViewDefault(renderRequest, renderResponse);
			} catch (Exception e) {
				SessionErrors.add(renderRequest, "manage-page-template-error-retrieving-page-template");
			}
		} else if (view.equalsIgnoreCase("infoPageTemplate")) {
			try {				
				showViewInfo(renderRequest, renderResponse);								
			} catch (Exception ex) {
				;
				try {
					showViewDefault(renderRequest, renderResponse);
				} catch (Exception ex1) {
					;
				}
			}
		} else if (view.equalsIgnoreCase("editPageTemplate")) {
			try {				
				showViewEdit(renderRequest, renderResponse);								
			} catch (Exception ex) {
				;
				try {
					showViewDefault(renderRequest, renderResponse);
				} catch (Exception ex1) {
					;
				}
			}
		}
	}		
		
	public void showViewDefault(RenderRequest renderRequest,
			RenderResponse renderResponse) throws IOException, PortletException {	
		ThemeDisplay themeDisplay = (ThemeDisplay) renderRequest.getAttribute(WebKeys.THEME_DISPLAY);

		long groupId = themeDisplay.getScopeGroupId();

		PermissionChecker permissionChecker = themeDisplay.getPermissionChecker();
		boolean hasAddPermission = permissionChecker.hasPermission(groupId, "com.protecmedia.iter.designer.model", groupId, "ADD_PAGETEMPLATE");
		boolean hasDeletePermission = permissionChecker.hasPermission(groupId, "com.protecmedia.iter.designer.model", groupId, ActionKeys.DELETE);
		boolean hasConfigurePermission = permissionChecker.hasPermission(groupId, Group.class.getName(), groupId, ActionKeys.PERMISSIONS);
				
		renderRequest.setAttribute("hasAddPermission", hasAddPermission);
		renderRequest.setAttribute("hasDeletePermission", hasDeletePermission);
		renderRequest.setAttribute("hasConfigurePermission", hasConfigurePermission);
	
		PortletURL addPageTemplateURL = renderResponse.createRenderURL();
		addPageTemplateURL.setParameter("view", "editPageTemplate");
		addPageTemplateURL.setParameter("editType", "add");	
		renderRequest.setAttribute("addPageTemplateURL", addPageTemplateURL.toString());
		
		PortletURL deletePageTemplatesURL = renderResponse.createActionURL();
		deletePageTemplatesURL.setParameter("javax.portlet.action", "deleteSelectedPageTemplates");
		renderRequest.setAttribute("deletePageTemplatesURL", deletePageTemplatesURL.toString());
		
		include(viewJSP, renderRequest, renderResponse);		
	}
	
	
	public void showViewEdit(RenderRequest renderRequest, RenderResponse renderResponse) throws Exception {		
		PortletURL editURL = renderResponse.createActionURL();
		String editType = (String) renderRequest.getParameter("editType");		
		if (editType.equalsIgnoreCase("edit")) {			
			editURL.setParameter("javax.portlet.action", "updatePageTemplate");
			
			PageTemplate errorPageTemplate = (PageTemplate) renderRequest.getAttribute("errorPageTemplate");
			if (errorPageTemplate != null) {
				renderRequest.setAttribute("pageTemplate", errorPageTemplate);
			} else {
				
				long pageTemplateId = ParamUtil.getLong(renderRequest, "pageTemplateId", -1);
				
				PageTemplate pageTemplate = null;
				if (pageTemplateId != -1) {
					pageTemplate = PageTemplateLocalServiceUtil.getPageTemplate(pageTemplateId);
				}
				
				renderRequest.setAttribute("pageTemplate", pageTemplate);
			}			
		} else {
			editURL.setParameter("javax.portlet.action", "addPageTemplate");
			
			PageTemplate errorPageTemplate = (PageTemplate) renderRequest.getAttribute("errorPageTemplate");
			if (errorPageTemplate != null) {
				renderRequest.setAttribute("pageTemplate", errorPageTemplate);
			} else {				
				renderRequest.setAttribute("pageTemplate", new PageTemplateImpl());
			}

		}
		
		renderRequest.setAttribute("editURL", editURL.toString());		
		include(editPageTemplate, renderRequest, renderResponse);			
	}
	
	public void showViewInfo(RenderRequest renderRequest, RenderResponse renderResponse) throws Exception {		
		long pageTemplateId = ParamUtil.getLong(renderRequest, "pageTemplateId", -1);
				
		PageTemplate pageTemplate = PageTemplateLocalServiceUtil.getPageTemplate(pageTemplateId);
		
		renderRequest.setAttribute("pageTemplate", pageTemplate);
		
		include(infoPageTemplate, renderRequest, renderResponse);			
	}
	
	@Override
	public void processAction(ActionRequest actionRequest,
			ActionResponse actionResponse) throws IOException, PortletException {
		String action = ParamUtil.getString(actionRequest, "javax.portlet.action");
		
		try {
			if (action.equals("deletePageTemplate")) {				
				deletePageTemplate(actionRequest, actionResponse);				
			} else if (action.equals("deleteSelectedPageTemplates")) {
				deleteSelectedPageTemplates(actionRequest, actionResponse);
			} else if (action.equals("infoPageTemplate")) {				
				infoPageTemplate(actionRequest, actionResponse);
			} else if (action.equals("setDefaultPageTemplate")) {				
				setDefaultPageTemplate(actionRequest, actionResponse);
			} else if (action.equals("editPageTemplate")) {				
				editPageTemplate(actionRequest, actionResponse);
			} else if (action.equals("updatePageTemplate")) {				
				updatePageTemplate(actionRequest, actionResponse);
			} else if (action.equals("addPageTemplate")) {				
				addPageTemplate(actionRequest, actionResponse);
			}
		} catch (Exception e) {
			;
		}
	}
	
	public void setDefaultPageTemplate(ActionRequest actionRequest, ActionResponse actionResponse) throws PortalException, SystemException {
		long pageTemplateId = ParamUtil.getLong(actionRequest, "resourcePrimKey", -1);
		if (pageTemplateId != -1) {
			PageTemplateLocalServiceUtil.setDefaultPageTemplate(pageTemplateId);
			
			SessionMessages.add(actionRequest, "manage-page-template-set-default");
		} else {
			SessionErrors.add(actionRequest, "manage-page-template-selected-default-error");
		}				
	}
	
	public void deletePageTemplate(ActionRequest actionRequest, ActionResponse actionResponse) throws PortalException, SystemException {
		long pageTemplateId = ParamUtil.getLong(actionRequest, "resourcePrimKey", -1);
		
		PageTemplate pageTemplate = PageTemplateLocalServiceUtil.getPageTemplate(pageTemplateId);
		if (pageTemplate != null && !pageTemplate.getDefaultTemplate()) {					
			PageTemplateLocalServiceUtil.deletePageTemplate(pageTemplateId);
			
			try {
				
				//insercion en tabla de LIVE
				itemXmlIO.deleteLiveEntry(pageTemplate);
				
				LayoutLocalServiceUtil.deleteLayout(pageTemplate.getLayoutId());
				
			} catch (Exception e) {
				
			}
			SessionMessages.add(actionRequest, "manage-page-template-deleted");
		}  else {
			if (pageTemplate.getDefaultTemplate()) {
				SessionErrors.add(actionRequest, "manage-page-template-default-page-template-can-not-be-deleted");
			} else {
				SessionErrors.add(actionRequest, "manage-page-template-id-not-exist");
			}
		}				
	}
	
	public void deleteSelectedPageTemplates(ActionRequest actionRequest, ActionResponse actionResponse) throws PortalException, SystemException, ServiceError
	{		
		long[] pageTemplateIds = ParamUtil.getLongValues(actionRequest, "rowIds");		

		if (pageTemplateIds != null) {
			for (int i = 0; i < pageTemplateIds.length; i++) {
				PageTemplate pageTemplate = PageTemplateLocalServiceUtil.getPageTemplate(pageTemplateIds[i]);
				if (pageTemplate != null && !pageTemplate.getDefaultTemplate()) {					
					PageTemplateLocalServiceUtil.deletePageTemplateId(pageTemplateIds[i]);
				}
			}
			SessionMessages.add(actionRequest, "manage-page-template-deleted-all");
		} else {
			SessionErrors.add(actionRequest, "manage-page-template-deleted-all-error");
		}
	}
	
	public void infoPageTemplate(ActionRequest request, ActionResponse response) throws PortalException, SystemException {
		long pageTemplateId = ParamUtil.getLong(request, "resourcePrimKey");		
		response.setRenderParameter("pageTemplateId", Long.toString(pageTemplateId));
		response.setRenderParameter("view", "infoPageTemplate");		
	}
	
	public void editPageTemplate(ActionRequest request, ActionResponse response) throws PortalException, SystemException {
		long pageTemplateId = ParamUtil.getLong(request, "resourcePrimKey");		
		response.setRenderParameter("pageTemplateId", Long.toString(pageTemplateId));
		response.setRenderParameter("view", "editPageTemplate");		
		response.setRenderParameter("editType", "edit");
	}
		
	public void updatePageTemplate(ActionRequest actionRequest, ActionResponse actionResponse) throws SystemException, PortalException {
		UploadPortletRequest uploadRequest = PortalUtil.getUploadPortletRequest(actionRequest);
		
		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);
		
		long groupId = themeDisplay.getScopeGroupId();
		
		/* PageTemplate ID */
		long pageTemplateId = ParamUtil.getLong(uploadRequest, "resourcePrimKey", -1);
		
		/* PageTemplate Name */
		String name = ParamUtil.getString(uploadRequest, "name", "");		
						
		/* PageTemplate Name */
		String description = ParamUtil.getString(uploadRequest, "description", "");		
		
		/* PageTemplate SelectPageTemplate */
		boolean  selectPageTemplate = ParamUtil.getBoolean(uploadRequest, "selectPageTemplate", false);
		
		/* PageTemplate Type */
		String type = ParamUtil.getString(uploadRequest, "type", "page-template");
		
		/* PageTemplate  */
		long idPageTemplateSelected = ParamUtil.getLong(uploadRequest, "idPageTemplate", -1);
		
		/* Parent Id only article-template*/
		long parentId = ParamUtil.getLong(uploadRequest, "parentId", -1);

		File image = uploadRequest.getFile("imageDetail");
		
		long imageId = insertImage(image);
		
		PageTemplate pageTemplate = PageTemplateLocalServiceUtil.getPageTemplate(pageTemplateId);
		pageTemplate.setName(name.toLowerCase());
		pageTemplate.setDescription(description);		
		pageTemplate.setType(type);
		if (imageId != -1) {
			pageTemplate.setImageId(imageId);
		}
				
		ArrayList<String> errors = new ArrayList<String>();
		if (PageTemplateValidator.validateUpdatePageTemplate(pageTemplate, errors)) 
		{
			long layoutId = PageTemplateLocalServiceUtil.getPageTemplate(pageTemplateId).getLayoutId();
			
			if (type.equals("page-template")) 
			{
				parentId = DesignerUtil.getPageTemplateParentId(groupId, themeDisplay.getUserId());					
			} 
			else if (type.equals("newsletter")) 
			{
				parentId = DesignerUtil.getNewsLetterParentId(groupId, themeDisplay.getUserId());	
			} 
			else 
			{
				if (parentId!=-1)
				{
					parentId = DesignerUtil.getArticleTemplateParentId(groupId, parentId);
					LayoutLocalServiceUtil.updateParentLayoutId(layoutId, parentId);
				}
			}	
			
			if (selectPageTemplate)
			{
				try
				{
					PageTemplateLocalServiceUtil.loadPageTemplate(themeDisplay.getUserId(), groupId, idPageTemplateSelected, layoutId);
				}
				catch (Exception e)
				{
					if ( !(e instanceof PortalException || e instanceof SystemException) )
						throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(e) );
				}
			}
			
			PageTemplateLocalServiceUtil.updatePageTemplate(pageTemplate);
			
			//insercion en tabla de LIVE
			itemXmlIO.createLiveEntry(pageTemplate);
			
			SessionMessages.add(actionRequest, "manage-page-template-updated");	
		} else {
			for (String error : errors) {
				SessionErrors.add(actionRequest, error);
			}
			
			actionResponse.setRenderParameter("view", "editPageTemplate");
			actionResponse.setRenderParameter("editType", "edit");			
			actionRequest.setAttribute("errorPageTemplate", pageTemplate);
		}				
			
	}

	public void addPageTemplate(ActionRequest actionRequest, ActionResponse actionResponse) throws SystemException, PortalException {
		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

		long groupId = themeDisplay.getScopeGroupId();
				
		UploadPortletRequest uploadRequest = PortalUtil.getUploadPortletRequest(actionRequest);
		
		String pageTemplateId = ParamUtil.getString(uploadRequest, "pageTemplateId", "");
		
		/* PageTemplate Name */
		String name = ParamUtil.getString(uploadRequest, "name", "");		
		
		/* PageTemplate Description */
		String description = ParamUtil.getString(uploadRequest, "description", "");
		
		/* PageTemplate SelectPageTemplate */
		boolean  selectPageTemplate = ParamUtil.getBoolean(uploadRequest, "selectPageTemplate", false);
		
		/* PageTemplate Type */
		String type = ParamUtil.getString(uploadRequest, "type", "page-template");
		
		/* PageTemplate  */
		long idPageTemplateSelected = ParamUtil.getLong(uploadRequest, "idPageTemplate", -1);
										
		/* Parent Id only article-template*/
		long parentId = ParamUtil.getLong(uploadRequest, "parentId", LayoutConstants.DEFAULT_PARENT_LAYOUT_ID);
		
		/* Parent Id only article-template*/
		File image = uploadRequest.getFile("imageDetail");
		
		long imageId = insertImage(image);
		
		long id = CounterLocalServiceUtil.increment();
		
		pageTemplateId = pageTemplateId.trim().toUpperCase();
		if (pageTemplateId.equals("")){			
			pageTemplateId = String.valueOf(id);
		}	
		
		PageTemplate pageTemplate = PageTemplateLocalServiceUtil.createPageTemplate(id);
		pageTemplate.setPageTemplateId(pageTemplateId);
		pageTemplate.setName(name.toLowerCase());
		pageTemplate.setDescription(description);
		pageTemplate.setGroupId(groupId);
		pageTemplate.setType(type);
		pageTemplate.setImageId(imageId);
		
		ArrayList<String> errors = new ArrayList<String>();
		if (PageTemplateValidator.validatePageTemplate(pageTemplate, errors)) {
			try {
				
				if (type.equals("page-template")) {
					parentId = DesignerUtil.getPageTemplateParentId(groupId, themeDisplay.getUserId());
				} else if (type.equals("newsletter")) {
					parentId = DesignerUtil.getNewsLetterParentId(groupId, themeDisplay.getUserId());
				}
				
				ServiceContext serviceContext = new ServiceContext();
				
				Layout layoutPageTemplate = LayoutLocalServiceUtil.addLayout(themeDisplay.getUserId(), groupId, false, parentId, name, StringPool.BLANK, StringPool.BLANK, 
						LayoutConstants.TYPE_PORTLET, true, StringPool.BLANK, serviceContext);
				pageTemplate.setLayoutId(layoutPageTemplate.getPlid());
				
				if (selectPageTemplate) {						
					PageTemplateLocalServiceUtil.loadPageTemplate(themeDisplay.getUserId(), groupId, idPageTemplateSelected, layoutPageTemplate.getPlid());
				}
			
				PageTemplateLocalServiceUtil.addPageTemplate(pageTemplate);
				
				//insercion en tabla de LIVE
				itemXmlIO.createLiveEntry(pageTemplate);
				
				SessionMessages.add(actionRequest, "manage-page-template-added");
			} catch (Exception e) {
				SessionErrors.add(actionRequest, "manage-page-template-error-creation-layout-template-page");
			}							
			
		} else {
			for (String error : errors) {
				SessionErrors.add(actionRequest, error);
			}
			actionResponse.setRenderParameter("view", "editPageTemplate");
			actionResponse.setRenderParameter("editType", "add");
			actionRequest.setAttribute("errorPageTemplate", pageTemplate);
		}
		
	}
	
	/*
	 * IMAGES
	 */
	private long insertImage(File srcFile) {		
		try {
			if (srcFile != null && srcFile.exists() && srcFile.isFile()) {
				byte[] bytes = null;
	
				bytes = FileUtil.getBytes(srcFile);
				if (bytes != null) {					
					long imageId = CounterLocalServiceUtil.increment();
					ImageLocalServiceUtil.updateImage(imageId, bytes);					
			
					return imageId;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	protected String infoPageTemplate;
	protected String editPageTemplate;
}

