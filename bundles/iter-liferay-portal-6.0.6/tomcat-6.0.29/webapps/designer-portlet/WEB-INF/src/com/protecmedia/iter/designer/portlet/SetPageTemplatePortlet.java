/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.designer.portlet;

import java.io.IOException;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.portlet.LiferayWindowState;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.ColorScheme;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.model.LayoutTemplate;
import com.liferay.portal.model.LayoutTypePortlet;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.Theme;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.PortletURLFactoryUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;
import com.protecmedia.iter.designer.model.PageTemplate;
import com.protecmedia.iter.designer.service.PageTemplateLocalServiceUtil;
import com.protecmedia.iter.designer.util.DesignerUtil;

public class SetPageTemplatePortlet extends MVCPortlet {
	

	@Override
	public void init() throws PortletException {
		
		super.init();
		
		viewNew = getInitParameter("view-new");
		viewLoad = getInitParameter("view-load");	
		
		viewTemplatesResult = getInitParameter("view-templates-result");
	}
	
	public void doView(RenderRequest renderRequest,	RenderResponse renderResponse) throws IOException, PortletException {		
		
		String view = ParamUtil.get(renderRequest, "view", "");
				
		if (view.equals("templatesResult")) {
			include(viewTemplatesResult, renderRequest, renderResponse);
		} else if (view.equals("new")) {
			include(viewNew, renderRequest, renderResponse);
		} else if (view.equals("load")) { 
			include(viewLoad, renderRequest, renderResponse);
		} else { 
			super.doView(renderRequest, renderResponse);
		}
	}

	@Override
	public void processAction(ActionRequest actionRequest,
			ActionResponse actionResponse) throws IOException, PortletException {
		
		super.processAction(actionRequest, actionResponse);
		
		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);
		
		Layout layout = themeDisplay.getLayout();
		
		String action = ParamUtil.getString(actionRequest, "action", "");
		
		if (action.equals("add")) {
			addTemplate(actionRequest, actionResponse);
		} else if (action.equals("load")) {
			loadFromTemplate(actionRequest);
		} else {
			infoTemplate(actionRequest);
		}
		
		try {
			actionResponse.sendRedirect(PortalUtil.getLayoutFullURL(layout, themeDisplay));
		} catch (Exception e) {				
			;
		} 
		
	}
	
		
	private void loadFromTemplate(ActionRequest actionRequest) {
		
		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);
		
		long userId = themeDisplay.getUserId();
		long groupId = themeDisplay.getScopeGroupId();
		
		Layout layout = themeDisplay.getLayout();
		
		long pageTemplateId = ParamUtil.getLong(actionRequest, "id", -1);
		
		if (pageTemplateId != -1) {
			HttpServletRequest httpReq = PortalUtil.getHttpServletRequest(actionRequest);
			try {								
				PageTemplateLocalServiceUtil.loadPageTemplate(userId, groupId, pageTemplateId, layout.getPlid());
				SessionMessages.add(httpReq.getSession(), "succes-loading");
			} catch (Exception e) {
				SessionMessages.add(httpReq.getSession(), "load-failed");
			}
		}
	}

	/**
	 * @param actionRequest
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	private void addTemplate(ActionRequest actionRequest, ActionResponse actionResponse) {
		
		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);
		
		long userId = themeDisplay.getUserId();
		long groupId = themeDisplay.getScopeGroupId();
		
		String name = ParamUtil.getString(actionRequest, "name", "");
		String description = ParamUtil.getString(actionRequest, "description", "");
		String type = ParamUtil.getString(actionRequest, "type", "page-template");
		
		String pageTemplateId = ParamUtil.getString(actionRequest, "pageTemplateId", "");
						
		/* Parent Id only article-template*/
		long parentId = ParamUtil.getLong(actionRequest, "parentId", LayoutConstants.DEFAULT_PARENT_LAYOUT_ID);

		Layout layout = themeDisplay.getLayout();
		
		try {				
			
			if (layout.getType().equals(LayoutConstants.TYPE_PORTLET) && !name.equals("")) {
				
				PageTemplate pageTemplate = PageTemplateLocalServiceUtil.getPageTemplateByPageTemplateId(groupId, pageTemplateId);
				
				HttpServletRequest httpReq = PortalUtil.getHttpServletRequest(actionRequest);
				if (pageTemplate == null) {					
					if (type.equals("page-template")) {
						parentId = DesignerUtil.getPageTemplateParentId(groupId, themeDisplay.getUserId());
					}
									
					ServiceContext serviceContext = new ServiceContext();
			
					Layout layoutTpl = LayoutLocalServiceUtil.addLayout(userId, groupId, false, parentId, name, StringPool.BLANK, StringPool.BLANK, 
							LayoutConstants.TYPE_PORTLET, true, StringPool.BLANK, serviceContext);
			
					PageTemplateLocalServiceUtil.createPageTemplate(userId, groupId, name, description, type, layout.getPlid(), layoutTpl.getPlid());
									
					SessionMessages.add(httpReq.getSession(), "succes-adding");					
				} else {
					// Ya existe un TPL con ese nombre
					SessionMessages.add(httpReq.getSession(), "add-failed");
				}
			}
		} catch (Exception e) {
			;
		}
		
		
	}
	
	protected String viewTemplatesResult;
	protected String viewNew;
	protected String viewLoad;

	/**
	 * @param actionRequest
	 */
	private void infoTemplate(ActionRequest actionRequest) {
		try {		
			ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);
			
			Layout layout = themeDisplay.getLayout();
			System.out.println("Layout: " + layout.getName(themeDisplay.getLocale()));
			
			Theme theme = layout.getTheme();
			System.out.println("Theme: " + theme.getName());
	
			ColorScheme colorScheme = layout.getColorScheme();
			System.out.println("ColorScheme: " + colorScheme.getName());
			
			if (layout.getType().equals(LayoutConstants.TYPE_PORTLET)) {
				LayoutTypePortlet layoutTypePortlet = (LayoutTypePortlet) layout.getLayoutType();
				
				LayoutTemplate layoutTemplate = layoutTypePortlet.getLayoutTemplate();
				
				System.out.println("LayoutTemplate: " + layoutTemplate.getName() + ", NColumns: " + layoutTypePortlet.getNumOfColumns());

				List<String> columns = layoutTemplate.getColumns();
				
				for (String column : columns) {
			
					List<Portlet> portlets = layoutTypePortlet.getAllPortlets(column);
					int i = 0;
					for (Portlet portlet : portlets) {					
						System.out.println("ColumnId: " + column + " Position: " + i++ + " PortletName: " + portlet.getDisplayName() + " PluginId: " + portlet.getPluginId());
					}
				}
			}
		} catch (Exception e) {
			;
		}
	}
	
}

