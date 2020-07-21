/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.news.portlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Layout;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;
import com.protecmedia.iter.news.model.impl.PageContentImpl;
import com.protecmedia.iter.news.model.impl.QualificationImpl;
import com.protecmedia.iter.news.model.PageContent;
import com.protecmedia.iter.news.model.Qualification;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;
import com.protecmedia.iter.news.service.QualificationLocalServiceUtil;
import com.protecmedia.iter.news.service.item.PageContentXmlIO;
import com.protecmedia.iter.news.service.item.QualificationXmlIO;
import com.protecmedia.iter.news.util.PageContentValidator;
import com.protecmedia.iter.news.util.QualificationValidator;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;

public class PageContentPortlet extends MVCPortlet {
	
	private static PageContentXmlIO pageContentXmlIO = new PageContentXmlIO();
	private static QualificationXmlIO qualificationXmlIO = new QualificationXmlIO();

	public void init() {
		try {			
			super.init();
			editPage = getInitParameter("edit-pagecontent-jsp");			
			editQualification = getInitParameter("edit-qualification-jsp");
			
			updatePage = getInitParameter("update-pagecontent-jsp");
		} catch (PortletException e) {
			;
		}		
	}	
	
	public void doView(RenderRequest renderRequest,	RenderResponse renderResponse) throws IOException, PortletException {		
		
		String tab = ParamUtil.get(renderRequest, "tabs1", "page-content");
		
		if (tab.equals("page-content")) {
			showTabPageContent(renderRequest, renderResponse);
		} else { 
			showTabQualification(renderRequest, renderResponse);
		}
	}
	
	public void processAction(ActionRequest actionRequest, ActionResponse actionResponse) {
		
		String tab = ParamUtil.get(actionRequest, "tabs1", "page-content");
		
		if (tab.equals("page-content")) {
			try {
				processActionTabPageContent(actionRequest, actionResponse);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				processActionTabQualification(actionRequest, actionResponse);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	
	/* **************************************************************************************************
	 * 
	 * Tab Qualification
	 * 
	 * **************************************************************************************************/

	/**
	 * 
	 * @param renderRequest
	 * @param renderResponse
	 * @throws IOException
	 * @throws PortletException
	 */
	private void showTabQualification(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException {
		String view = (String) renderRequest.getParameter("view");
		
		if (view == null || view.equals("")) {
			try {
				showViewDefaultQualification(renderRequest, renderResponse);
			} catch (SystemException e) {
				SessionErrors.add(renderRequest, "error-retrieving-qualification");
			}
		} else if (view.equalsIgnoreCase("editQualification")) {
			try {				
				showViewEditQualification(renderRequest, renderResponse);								
			} catch (Exception ex) {
				;
				try {
					showViewDefaultQualification(renderRequest, renderResponse);
				} catch (SystemException ex1) {
					;
				}
			}
		}
	}
	
	public void showViewDefaultQualification(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException, SystemException {
		ThemeDisplay themeDisplay = (ThemeDisplay) renderRequest.getAttribute(WebKeys.THEME_DISPLAY);

		long groupId = themeDisplay.getScopeGroupId();

		PermissionChecker permissionChecker = themeDisplay.getPermissionChecker();
		boolean hasAddPermission = permissionChecker.hasPermission(groupId, "com.protecmedia.iter.news.model", groupId, "ADD_QUALIFICATION");
		boolean hasConfigurePermission = permissionChecker.hasPermission(groupId, Group.class.getName(), groupId, ActionKeys.PERMISSIONS);

		List<Qualification> tempResults = QualificationLocalServiceUtil.getQualifications(groupId);

		renderRequest.setAttribute("tempResults", tempResults);
		renderRequest.setAttribute("hasAddPermission", hasAddPermission);
		renderRequest.setAttribute("hasConfigurePermission", hasConfigurePermission);		
		
		PortletURL addQualificationURL = renderResponse.createRenderURL();
		addQualificationURL.setParameter("view", "editQualification");
		addQualificationURL.setParameter("editType", "add");	
		addQualificationURL.setParameter("tabs1", "qualification");
		
		renderRequest.setAttribute("tabs1", "qualification");
		renderRequest.setAttribute("addQualificationURL", addQualificationURL.toString());

		include(viewJSP, renderRequest, renderResponse);
	}

	public void showViewEditQualification(RenderRequest renderRequest, RenderResponse renderResponse) throws Exception {		
		PortletURL editURL = renderResponse.createActionURL();
		String editType = (String) renderRequest.getParameter("editType");		
		if (editType.equalsIgnoreCase("edit")) {			
			editURL.setParameter("javax.portlet.action", "updateQualification");
			editURL.setParameter("tabs1", "qualification");
			Qualification errorQualification = (Qualification) renderRequest.getAttribute("errorQualification");
			if (errorQualification != null) {
				renderRequest.setAttribute("qualification", errorQualification);
			} else {
				long qualificationId = Long.parseLong(renderRequest.getParameter("qualificationId"));
				Qualification content = QualificationLocalServiceUtil.getQualification(qualificationId);
				renderRequest.setAttribute("qualification", content);
			}			
		} else {
			editURL.setParameter("javax.portlet.action", "addQualification");
			editURL.setParameter("tabs1", "qualification");
			Qualification errorQualification = (Qualification) renderRequest.getAttribute("errorQualification");
			if (errorQualification != null) {
				renderRequest.setAttribute("qualification", errorQualification);
			} else {				
				renderRequest.setAttribute("qualification", new QualificationImpl());
			}

		}
		
		renderRequest.setAttribute("editURL", editURL.toString());
		renderRequest.setAttribute("tabs1", "qualification");
		include(editQualification, renderRequest, renderResponse);			
	}	
	
	
	
	/**
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws Exception 
	 */	
	private void processActionTabQualification(ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {
		String action = ParamUtil.getString(actionRequest, "javax.portlet.action");
		try {
			if (action.equals("deleteQualification")) {				
				deleteQualification(actionRequest, actionResponse);
			} else if (action.equals("editQualification")) {								
				editQualification(actionRequest, actionResponse);				
			} else if (action.equals("updateQualification")) {
				updateQualification(actionRequest, actionResponse);
			} else if (action.equals("addQualification")) {
				addQualification(actionRequest, actionResponse);
			}
		} catch (PortalException e) {
			;
		} catch (SystemException e) {
			;
		}
	}
	
	
	
	/**
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws Exception 
	 */
	public void deleteQualification(ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {
		long qualificationId = ParamUtil.getLong(actionRequest, "resourcePrimKey", -1);
		if (qualificationId != -1) {
			QualificationLocalServiceUtil.deleteQualification(qualificationId);
			SessionMessages.add(actionRequest, "qualification-deleted");
		} else {
			SessionErrors.add(actionRequest, "qualification-id-not-exist");
		}
		
		actionResponse.setRenderParameter("tabs1", "qualification");
	}
		
	
	
	/**
	 * 
	 * @param request
	 * @param response
	 * @throws PortalException
	 * @throws SystemException
	 */
	public void editQualification(ActionRequest request, ActionResponse response) throws PortalException, SystemException {
		long id = ParamUtil.getLong(request, "resourcePrimKey");		
		response.setRenderParameter("qualificationId", Long.toString(id));
		response.setRenderParameter("view", "editQualification");
		response.setRenderParameter("tabs1", "qualification");
		response.setRenderParameter("editType", "edit");
	}
	
	
	
	/**
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws SystemException
	 * @throws PortalException
	 */
	public void updateQualification(ActionRequest actionRequest, ActionResponse actionResponse) throws SystemException, PortalException {
		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

		long groupId = themeDisplay.getScopeGroupId();
		
		/* Qulification ID */
		long qualificationId = ParamUtil.getLong(actionRequest, "resourcePrimKey", -1);
		
		/* Qualification Name */
		String qualificationName = ParamUtil.getString(actionRequest, "qualification", "");		
						
		Qualification qualification = QualificationLocalServiceUtil.getQualification(qualificationId);
		qualification.setName(qualificationName.toLowerCase());
		qualification.setGroupId(groupId);
		qualification.setModifiedDate(new Date());
		
		ArrayList<String> errors = new ArrayList<String>();
		if (QualificationValidator.validateQualification(qualification, errors)) {
			QualificationLocalServiceUtil.updateQualification(qualification);
			
			//insercion en la tabla de Live
			qualificationXmlIO.createLiveEntry(qualification);
			
			SessionMessages.add(actionRequest, "qualification-updated");	
		} else {
			for (String error : errors) {
				SessionErrors.add(actionRequest, error);
			}
			
			actionResponse.setRenderParameter("view", "editQualification");
			actionResponse.setRenderParameter("editType", "edit");			
			actionRequest.setAttribute("errorQualification", qualification);
		}
		
		actionResponse.setRenderParameter("tabs1", "qualification");
			
	}

	
	
	/**
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws SystemException
	 * @throws PortalException
	 */
	public void addQualification(ActionRequest actionRequest, ActionResponse actionResponse) throws SystemException, PortalException {
		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

		long groupId = themeDisplay.getScopeGroupId();
				
		/* Qualification Name */
		String qualificationName = ParamUtil.getString(actionRequest, "qualification", "");		
						
		long id = CounterLocalServiceUtil.increment();
		
		Qualification qualification = QualificationLocalServiceUtil.createQualification(id);
		qualification.setName(qualificationName.toLowerCase());
		qualification.setGroupId(groupId);
		qualification.setQualifId("qual_"+String.valueOf(qualification.getId()));
		
		ArrayList<String> errors = new ArrayList<String>();
		if (QualificationValidator.validateQualification(qualification, errors)) {
			QualificationLocalServiceUtil.addQualification(qualification);
			
			//insercion en la tabla de Live
			qualificationXmlIO.createLiveEntry(qualification);
			
			SessionMessages.add(actionRequest, "qualification-added");
		} else {
			for (String error : errors) {
				SessionErrors.add(actionRequest, error);
			}
			actionResponse.setRenderParameter("view", "editQualification");
			actionResponse.setRenderParameter("editType", "add");
			actionRequest.setAttribute("errorQualification", qualification);
		}
		
		actionResponse.setRenderParameter("tabs1", "qualification");
	}	
	
	/* ****************************************************
	 *   
	 * Tab Page content
	 *   
	 * ****************************************************/
	/**
	 * @param renderRequest
	 * @param renderResponse
	 * @throws IOException
	 * @throws PortletException
	 */
	private void showTabPageContent(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException {
		String view = (String) renderRequest.getParameter("view");
				
		if (view == null || view.equals("")) 
		{
			try 
			{
				showViewDefault(renderRequest, renderResponse);
			} 
			catch (SystemException e) 
			{
				SessionErrors.add(renderRequest, "error-retrieving-page-content");
			}
		} 
		else if (view.equalsIgnoreCase("editPageContent")) 
		{
			try 
			{
				showViewEditPageContent(renderRequest, renderResponse);			
			} 
			catch (Exception ex) 
			{
				try 
				{
					showViewDefault(renderRequest, renderResponse);
				} 
				catch (SystemException ex1) 
				{					
					;
				}
			}
		}
	}
	
	
	
	/**
	 * 
	 * @param renderRequest
	 * @param renderResponse
	 * @throws IOException
	 * @throws PortletException
	 * @throws SystemException
	 */	
	public void showViewDefault(RenderRequest renderRequest, RenderResponse renderResponse) throws IOException, PortletException, SystemException {
		ThemeDisplay themeDisplay = (ThemeDisplay) renderRequest.getAttribute(WebKeys.THEME_DISPLAY);

		long groupId = themeDisplay.getScopeGroupId();
		String layoutId = ParamUtil.getString(renderRequest, "layout-id", "");		

		PermissionChecker permissionChecker = themeDisplay.getPermissionChecker();
		boolean hasAddPermission = permissionChecker.hasPermission(groupId, "com.protecmedia.iter.news.model", groupId, "ADD_PAGE_CONTENT");
		boolean hasDeletePermission = permissionChecker.hasPermission(groupId, "com.protecmedia.iter.news.model", groupId, ActionKeys.DELETE);
		boolean hasConfigurePermission = permissionChecker.hasPermission(groupId, Group.class.getName(), groupId, ActionKeys.PERMISSIONS);

		List<Layout> seccionList = LayoutLocalServiceUtil.getLayouts(groupId, false, 0);
		
		if ("".equals(layoutId) && seccionList.size() > 0) {
			layoutId = seccionList.get(0).getUuid();
		}
		
		renderRequest.setAttribute("hasAddPermission", hasAddPermission);
		renderRequest.setAttribute("hasDeletePermission", hasAddPermission);
		renderRequest.setAttribute("hasConfigurePermission", hasConfigurePermission);		
		
		PortletURL addPageContentURL = renderResponse.createRenderURL();
		addPageContentURL.setParameter("view", "editPageContent");
		addPageContentURL.setParameter("editType", "add");
		addPageContentURL.setParameter("layout-id", String.valueOf(layoutId));
		renderRequest.setAttribute("addPageContentURL", addPageContentURL.toString());
		
		PortletURL deletePageContentURL = renderResponse.createActionURL();		
		deletePageContentURL.setParameter("javax.portlet.action", "deleteSelectedPageContents");
		deletePageContentURL.setParameter("layout-id", String.valueOf(layoutId));
		renderRequest.setAttribute("deletePageContentURL", deletePageContentURL.toString());

		include(viewJSP, renderRequest, renderResponse);
	}

	public void showViewEditPageContent(RenderRequest renderRequest, RenderResponse renderResponse) throws Exception {		
		PortletURL editURL = renderResponse.createActionURL();
		
		String layoutId = (String) renderRequest.getParameter("layout-id");
		String editType = (String) renderRequest.getParameter("editType");	
		if (editType.equalsIgnoreCase("edit")) 
		{			
			editURL.setParameter("javax.portlet.action", "updatePageContent");
			PageContent errorPageContent = (PageContent) renderRequest.getAttribute("errorPageContent");
			if (errorPageContent != null) {
				renderRequest.setAttribute("pageContent", errorPageContent);
			} else {
				long pageContentId = Long.parseLong(renderRequest.getParameter("pageContentId"));
				PageContent content = PageContentLocalServiceUtil.getPageContent(pageContentId);				
				renderRequest.setAttribute("pageContent", content);
			}
			renderRequest.setAttribute("layout-id", layoutId);
			renderRequest.setAttribute("editURL", editURL.toString());
			include(updatePage, renderRequest, renderResponse);	
		} 
		else 
		{
			editURL.setParameter("javax.portlet.action", "addPageContent");
			PageContent errorPageContent = (PageContent) renderRequest.getAttribute("errorPageContent");
			if (errorPageContent != null) 
			{
				renderRequest.setAttribute("pageContent", errorPageContent);
			} 
			else 
			{				
				renderRequest.setAttribute("pageContent", new PageContentImpl());
			}
			
			@SuppressWarnings("unchecked")
			ArrayList<String> errorContentIDs = (ArrayList<String>) renderRequest.getAttribute("errorContentIDs");
			if (errorContentIDs != null)
			{
				renderRequest.setAttribute("selectedContentIDs", errorContentIDs);
			}
			else
			{
				renderRequest.setAttribute("selectedContentIDs", new ArrayList<String>());
			}
			
			boolean standardArticleCheck = ParamUtil.getBoolean(renderRequest, "standardArticleCheck", true);
			boolean standardGalleryCheck = ParamUtil.getBoolean(renderRequest, "standardGalleryCheck", true);
			boolean standardPollCheck = ParamUtil.getBoolean(renderRequest, "standardPollCheck", true);			
			
			String keyword = ParamUtil.getString(renderRequest, "keyword", "");
			renderRequest.setAttribute("keyword", keyword);
			
			renderRequest.setAttribute("standardArticleCheck", standardArticleCheck);
			renderRequest.setAttribute("standardGalleryCheck", standardGalleryCheck);
			renderRequest.setAttribute("standardPollCheck", standardPollCheck);
			
			renderRequest.setAttribute("layout-id", layoutId);
			renderRequest.setAttribute("editURL", editURL.toString());
			include(editPage, renderRequest, renderResponse);	
		}
			
	}		

	/**
	 * @param actionRequest
	 * @param actionResponse
	 * @throws Exception 
	 */
	private void processActionTabPageContent(ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {
		String action = ParamUtil.getString(actionRequest, "javax.portlet.action");
		try {
			if (action.equals("deletePageContent")) {				
				deletePageContent(actionRequest, actionResponse);
			} else if (action.equals("deleteSelectedPageContents")) {
				deleteSelectedPageContents(actionRequest, actionResponse);
			} else if (action.equals("editPageContent")) {								
				editPageContent(actionRequest, actionResponse);				
			} else if (action.equals("updatePageContent")) {
				updatePageContent(actionRequest, actionResponse);
			} else if (action.equals("addPageContent")) {
				addPageContent(actionRequest, actionResponse);
			} else if (action.equals("increaseOrderPageContent")) {
				increaseOrderPageContent(actionRequest, actionResponse);
			} else if (action.equals("decreaseOrderPageContent")) {				
				decreaseOrderPageContent(actionRequest, actionResponse);
			} else if (action.equals("activatePageContent")) {				
				activatePageContent(actionRequest, actionResponse);
			} else if (action.equals("deactivatePageContent")) {				
				deactivatePageContent(actionRequest, actionResponse);
			}
		} catch (PortalException e) {
			;
		} catch (SystemException e) {
			;
		}
	}
	
	public void increaseOrderPageContent(ActionRequest actionRequest, ActionResponse actionResponse) throws PortalException, SystemException {
		long pageContentId = ParamUtil.getLong(actionRequest, "resourcePrimKey", -1);
		
		String layoutId = ParamUtil.getString(actionRequest, "layout-id", "-1");
		actionResponse.setRenderParameter("layout-id", layoutId);			
		
		if (pageContentId != -1) {
			PageContentLocalServiceUtil.increaseOrderPageContent(pageContentId);
			SessionMessages.add(actionRequest, "page-content-order-changed");
		} else {
			SessionErrors.add(actionRequest, "page-content-id-not-exist");
		}
	}
	
	public void decreaseOrderPageContent(ActionRequest actionRequest, ActionResponse actionResponse) throws PortalException, SystemException {
		long pageContentId = ParamUtil.getLong(actionRequest, "resourcePrimKey", -1);
		
		String layoutId = ParamUtil.getString(actionRequest, "layout-id", "-1");
		actionResponse.setRenderParameter("layout-id", layoutId);		
		
		if (pageContentId != -1) {
			PageContentLocalServiceUtil.decreaseOrderPageContent(pageContentId);
			SessionMessages.add(actionRequest, "page-content-order-changed");
		} else {
			SessionErrors.add(actionRequest, "page-content-id-not-exist");
		}
	}
	
	public void activatePageContent(ActionRequest actionRequest, ActionResponse actionResponse) throws PortalException, SystemException {
		long pageContentId = ParamUtil.getLong(actionRequest, "resourcePrimKey", -1);
		
		String layoutId = ParamUtil.getString(actionRequest, "layout-id", "-1");
		actionResponse.setRenderParameter("layout-id", layoutId);		
		
		if (PageContentLocalServiceUtil.activatePageContent(pageContentId)) {
			SessionMessages.add(actionRequest, "page-content-activate");
		} else {
			SessionErrors.add(actionRequest, "page-content-id-not-exist");
		}
	}
	
	public void deactivatePageContent(ActionRequest actionRequest, ActionResponse actionResponse) throws PortalException, SystemException {
		long pageContentId = ParamUtil.getLong(actionRequest, "resourcePrimKey", -1);
		
		String layoutId = ParamUtil.getString(actionRequest, "layout-id", "-1");
		actionResponse.setRenderParameter("layout-id", layoutId);		
		
		if (PageContentLocalServiceUtil.deactivatePageContent(pageContentId)) {
			SessionMessages.add(actionRequest, "page-content-deactivate");
		} else {
			SessionErrors.add(actionRequest, "page-content-id-not-exist");
		}
	}
	
	/***
	 * 	
	 * Eliminamos un PageContent
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws Exception 
	 */
	public void deletePageContent(ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {
		long pageContentId = ParamUtil.getLong(actionRequest, "resourcePrimKey", -1);
		
		String layoutId = ParamUtil.getString(actionRequest, "layout-id", "-1");
		actionResponse.setRenderParameter("layout-id", layoutId);
		
		if (pageContentId != -1) {
			PageContentLocalServiceUtil.deletePageContent(pageContentId);
			
			SessionMessages.add(actionRequest, "page-content-deleted");
		} else {
			SessionErrors.add(actionRequest, "page-content-id-not-exist");
		}
	}
	
	/***
	 * 	
	 * Eliminamos varios PageContent
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws Exception 
	 */
	public void deleteSelectedPageContents(ActionRequest actionRequest, ActionResponse actionResponse) throws Exception {		
		long[] articleIds = ParamUtil.getLongValues(actionRequest, "rowIds");
		String layoutId = ParamUtil.getString(actionRequest, "layout-id", "-1");
		actionResponse.setRenderParameter("layout-id", layoutId);
		
		if (articleIds != null) {
			for (int i = 0; i < articleIds.length; i++) {
				PageContentLocalServiceUtil.deletePageContent(articleIds[i]);
			}
			SessionMessages.add(actionRequest, "page-content-deleted-all");
		} else {
			SessionErrors.add(actionRequest, "page-content-deleted-all-error");
		}
	}
	
	
	/***
	 * 
	 * Editamos un PageContent
	 * 
	 * @param request
	 * @param response
	 * @throws PortalException
	 * @throws SystemException
	 */
	public void editPageContent(ActionRequest actionRequest, ActionResponse actionResponse) throws PortalException, SystemException {
		long pageContentId = ParamUtil.getLong(actionRequest, "resourcePrimKey");
		
		String layoutId = ParamUtil.getString(actionRequest, "layout-id", "-1");
		actionResponse.setRenderParameter("layout-id", layoutId);
		
		actionResponse.setRenderParameter("pageContentId", Long.toString(pageContentId));
		actionResponse.setRenderParameter("view", "editPageContent");
		actionResponse.setRenderParameter("editType", "edit");
	}
		
	/***
	 * 
	 * Actualizamos un PageContent
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws SystemException
	 * @throws PortalException
	 */
	public void updatePageContent(ActionRequest actionRequest, ActionResponse actionResponse) throws SystemException, PortalException {
		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

		long groupId = themeDisplay.getScopeGroupId();
		
		/* Journal Article ID */
		long pageContentId = ParamUtil.getLong(actionRequest, "resourcePrimKey", -1);
		
		/* ID */
		String id = ParamUtil.getString(actionRequest, "id", "");		
		
		/* Section ID */
		String sectionId = ParamUtil.getString(actionRequest, "layoutId", "");
		
		/* Qualification ID */
		String qualificationId = "";
		long calificacionId = ParamUtil.getLong(actionRequest, "qualificationId", -1);
		if (calificacionId > 0)
		{
			Qualification qualification = QualificationLocalServiceUtil.getQualification(calificacionId);
			qualificationId = qualification.getQualifId();
		}

		
		/* Article Model ID */
		long articleModelId = ParamUtil.getLong(actionRequest, "articleModelId", -1);		
		
		/* Display Date */							
		Calendar now = Calendar.getInstance();			
		
		int displayDateMonth = ParamUtil.getInteger(actionRequest, "vigenciadesdeMonth", now.get(Calendar.MONTH));
		int displayDateDay = ParamUtil.getInteger(actionRequest, "vigenciadesdeDay", now.get(Calendar.DAY_OF_MONTH));
		int displayDateYear = ParamUtil.getInteger(actionRequest, "vigenciadesdeYear", now.get(Calendar.YEAR));
		int displayDateHour = ParamUtil.getInteger(actionRequest, "vigenciadesdeHour", now.get(Calendar.HOUR));
		int displayDateMinute = ParamUtil.getInteger(actionRequest, "vigenciadesdeMinute", now.get(Calendar.MINUTE));
		int displayDateAmPm = ParamUtil.getInteger(actionRequest, "vigenciadesdeAmPm", now.get(Calendar.AM_PM));

		if (displayDateAmPm == Calendar.PM) {
			displayDateHour += 12;
		}
		// int year, int month, int date, int hour, int minute
		Date displayDate = (new GregorianCalendar(displayDateYear, displayDateMonth, displayDateDay, displayDateHour, displayDateMinute)).getTime();
		
		/* Expiration Date */		
		now.add(Calendar.MONTH, 1);
		
		int expirationDateMonth = ParamUtil.getInteger(actionRequest, "vigenciahastaMonth", now.get(Calendar.MONTH));
		int expirationDateDay = ParamUtil.getInteger(actionRequest, "vigenciahastaDay", now.get(Calendar.DAY_OF_MONTH));
		int expirationDateYear = ParamUtil.getInteger(actionRequest, "vigenciahastaYear", now.get(Calendar.YEAR));
		int expirationDateHour = ParamUtil.getInteger(actionRequest, "vigenciahastaHour", now.get(Calendar.HOUR));
		int expirationDateMinute = ParamUtil.getInteger(actionRequest, "vigenciahastaMinute", now.get(Calendar.MINUTE));
		int expirationDateAmPm = ParamUtil.getInteger(actionRequest, "vigenciahastaAmPm", now.get(Calendar.AM_PM));

		if (expirationDateAmPm == Calendar.PM) {
			expirationDateHour += 12;
		}
		Date expirationDate = (new GregorianCalendar(expirationDateYear, expirationDateMonth, expirationDateDay, expirationDateHour, expirationDateMinute)).getTime();
		
		PageContent pageContent = PageContentLocalServiceUtil.getPageContent(pageContentId);
		pageContent.setContentId(id);
		pageContent.setLayoutId(sectionId);		
		pageContent.setVigenciadesde(displayDate);
		pageContent.setVigenciahasta(expirationDate);
		pageContent.setGroupId(groupId);		
		pageContent.setModifiedDate(new Date());
		pageContent.setQualificationId(qualificationId);	
		pageContent.setArticleModelId(articleModelId);
		
		ArrayList<String> errors = new ArrayList<String>();
		if (PageContentValidator.validatePageContent(pageContent, errors)) 
		{
			PageContentLocalServiceUtil.updatePageContent(pageContent);
			
			//insercion en la tabla de Live
			pageContentXmlIO.createLiveEntry(pageContent);
			
			SessionMessages.add(actionRequest, "page-content-updated");	
		} 
		else 
		{
			for (String error : errors) 
				SessionErrors.add(actionRequest, error);
			
			actionResponse.setRenderParameter("view", "editPageContent");
			actionResponse.setRenderParameter("editType", "edit");
			actionRequest.setAttribute("errorPageContent", pageContent);
		}
			
		actionResponse.setRenderParameter("layout-id", String.valueOf(sectionId));
	}
	
	/***
	 * 
	 * AÃ±ade un PageContent
	 * 
	 * @param actionRequest
	 * @param actionResponse
	 * @throws SystemException
	 * @throws PortalException
	 */
	public void addPageContent(ActionRequest actionRequest, ActionResponse actionResponse) throws SystemException, PortalException {		
		
		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);		
		
		long groupId = themeDisplay.getScopeGroupId();
		
		/* Section ID */
		String sectionId = ParamUtil.getString(actionRequest, "layoutId", "");

		/* Qualification ID */
		long calificacionId = ParamUtil.getLong(actionRequest, "qualificationId", -1);
		
		/* Article Model ID */
		long articleModelId = ParamUtil.getLong(actionRequest, "articleModelId", -1);					
		
		/* Display Date */							
		Calendar now = Calendar.getInstance();			
		
		int displayDateMonth = ParamUtil.getInteger(actionRequest, "vigenciadesdeMonth", now.get(Calendar.MONTH));
		int displayDateDay = ParamUtil.getInteger(actionRequest, "vigenciadesdeDay", now.get(Calendar.DAY_OF_MONTH));
		int displayDateYear = ParamUtil.getInteger(actionRequest, "vigenciadesdeYear", now.get(Calendar.YEAR));
		int displayDateHour = ParamUtil.getInteger(actionRequest, "vigenciadesdeHour", now.get(Calendar.HOUR));
		int displayDateMinute = ParamUtil.getInteger(actionRequest, "vigenciadesdeMinute", now.get(Calendar.MINUTE));
		int displayDateAmPm = ParamUtil.getInteger(actionRequest, "vigenciadesdeAmPm", now.get(Calendar.AM_PM));

		if (displayDateAmPm == Calendar.PM) {
			displayDateHour += 12;
		}
		// int year, int month, int date, int hour, int minute
		Date displayDate = (new GregorianCalendar(displayDateYear, displayDateMonth, displayDateDay, displayDateHour, displayDateMinute)).getTime();
		
		/* Expiration Date */		
		now.add(Calendar.YEAR, 1);
		now.add(Calendar.MONTH, 1);	
		
		int expirationDateMonth = ParamUtil.getInteger(actionRequest, "vigenciahastaMonth", now.get(Calendar.MONTH));
		int expirationDateDay = ParamUtil.getInteger(actionRequest, "vigenciahastaDay", now.get(Calendar.DAY_OF_MONTH));
		int expirationDateYear = ParamUtil.getInteger(actionRequest, "vigenciahastaYear", now.get(Calendar.YEAR));
		int expirationDateHour = ParamUtil.getInteger(actionRequest, "vigenciahastaHour", now.get(Calendar.HOUR));
		int expirationDateMinute = ParamUtil.getInteger(actionRequest, "vigenciahastaMinute", now.get(Calendar.MINUTE));
		int expirationDateAmPm = ParamUtil.getInteger(actionRequest, "vigenciahastaAmPm", now.get(Calendar.AM_PM));

		if (expirationDateAmPm == Calendar.PM) {
			expirationDateHour += 12;
		}
		
		Date expirationDate = (new GregorianCalendar(expirationDateYear, expirationDateMonth, expirationDateDay, expirationDateHour, expirationDateMinute)).getTime();				

		String[] contentIds =  actionRequest.getParameterValues("rowIds");
		ArrayList<String> contentIDs = (contentIds != null) ? 
										new ArrayList<String>( Arrays.asList(contentIds) ) : 
										new ArrayList<String>();
		
		long contentGroupId = ParamUtil.getLong(actionRequest, "contentGroupId", -1);;
		
		if (contentIDs.isEmpty())
		{
			// Si no se seleccionó un contenido será error
			addPageContent(actionRequest, actionResponse,
					contentIDs, null, contentGroupId, sectionId, displayDate, expirationDate, 
					groupId, calificacionId, articleModelId);
		}
		else
		{
			for (String contentID:contentIDs)
			{
				JournalArticle content = JournalArticleLocalServiceUtil.getArticle(themeDisplay.getCompanyGroupId(), contentID);
				
				if (!addPageContent(actionRequest, actionResponse,
						contentIDs, content, contentGroupId, sectionId, displayDate, expirationDate, 
						groupId, calificacionId, articleModelId))
					break;
			}
		}
				
		actionResponse.setRenderParameter("layout-id", String.valueOf(sectionId));
	}
	////////////////////////////////////////////////////////////////////////////////////////
	private boolean addPageContent(ActionRequest actionRequest, ActionResponse actionResponse,
			ArrayList<String> contentIDs, JournalArticle content,
			long contentGroupId, String layoutId, Date vigenciadesde, Date vigenciahasta, 
			long groupId, long qualificationID, long articleModelId)  throws SystemException, PortalException 
	{
		String contentId = ""; String typeContent = ""; String qualificationId = "";
		if (content != null)
		{
			contentId 	= content.getArticleId();
			typeContent = content.getStructureId();
		}
		if (qualificationID > 0)
		{
			Qualification qualification = QualificationLocalServiceUtil.getQualification(qualificationID);
			qualificationId = qualification.getQualifId();
		}
		
		long id = CounterLocalServiceUtil.increment();
		String pageContentId = String.valueOf(CounterLocalServiceUtil.increment());	
		PageContent pageContent = PageContentLocalServiceUtil.createPageContent(id);

		pageContent.setPageContentId(pageContentId);
		pageContent.setContentGroupId(contentGroupId);
		pageContent.setLayoutId(layoutId);					
		pageContent.setVigenciadesde(vigenciadesde);
		pageContent.setVigenciahasta(vigenciahasta);
		pageContent.setGroupId(groupId);
		pageContent.setOrden( PageContentLocalServiceUtil.getMaxPageContentOrden(layoutId, groupId)+1 );			
		pageContent.setQualificationId(qualificationId);	
		pageContent.setModifiedDate(new Date());
		pageContent.setArticleModelId(articleModelId);
		pageContent.setOnline(true);
		pageContent.setContentId(contentId);
		pageContent.setTypeContent(typeContent);
		
		boolean added = false;
		ArrayList<String> errors = new ArrayList<String>();
		if (PageContentValidator.validatePageContent(pageContent, errors)) 
		{
			PageContentLocalServiceUtil.addPageContent(pageContent);
			
			//insercion en la tabla de Live
			pageContentXmlIO.createLiveEntry(pageContent);
			
			SessionMessages.add(actionRequest, "page-content-added");
			added = true;
		} 
		else 
		{
			for (String error : errors) 
				SessionErrors.add(actionRequest, error);

			actionResponse.setRenderParameter("view", "editPageContent");
			actionResponse.setRenderParameter("editType", "add");
			actionRequest.setAttribute("errorPageContent", 	pageContent);
			actionRequest.setAttribute("errorContentIDs", 	contentIDs);
		}
		return added;
	}

	protected String editPage;
	protected String editQualification;	
	
	protected String updatePage;
	
}
