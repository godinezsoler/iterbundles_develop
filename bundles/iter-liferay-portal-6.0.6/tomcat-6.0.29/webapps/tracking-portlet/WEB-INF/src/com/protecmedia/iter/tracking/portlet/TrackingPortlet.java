/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.tracking.portlet;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;
import com.protecmedia.iter.news.model.Comments;
import com.protecmedia.iter.news.service.CommentsLocalServiceUtil;

public class TrackingPortlet extends MVCPortlet {
	
	
	@Override
	public void init() throws PortletException {	
		super.init();
		trackingEdit = getInitParameter("edit-tracking-jsp");
	}
	
	@Override
	public void doView(RenderRequest renderRequest,
			RenderResponse renderResponse) throws IOException, PortletException {
		
		String milenium = ParamUtil.get(renderRequest, "milenium", "");
		
		
		PortletURL addArticleURL = renderResponse.createRenderURL();
		addArticleURL.setParameter(milenium, "milenium");
		renderRequest.setAttribute("addArticleURL", addArticleURL.toString());
		String tab = ParamUtil.get(renderRequest, "tabs1", "pages");
		String view = ParamUtil.get(renderRequest, "view", "");
		if (view.equals("edit")) {						
			showEditComment(renderRequest, renderResponse);
		} else if (view.equals("search")) {	
			showFilterContents(renderRequest, renderResponse);			
		} else if (view.equals("filter")) {	
			showFilterComment(renderRequest, renderResponse);		
		} else if (tab.equals("pages")) {	
			showTabPages(renderRequest, renderResponse);	
		} else {			
			showTabArticles(renderRequest, renderResponse);						
		}
		
	}
	
	public void showTabArticles(RenderRequest renderRequest,
			RenderResponse renderResponse) throws IOException, PortletException {
		
		PortletURL addArticleURL = renderResponse.createRenderURL();
		addArticleURL.setParameter("tabs1", "articles");
		renderRequest.setAttribute("tabs1", "articles");
		renderRequest.setAttribute("addArticleURL", addArticleURL.toString());
		include(viewJSP, renderRequest, renderResponse);
	}
	
	public void showTabPages(RenderRequest renderRequest,
			RenderResponse renderResponse) throws IOException, PortletException {
		
		PortletURL addPageURL = renderResponse.createRenderURL();
		addPageURL.setParameter("tabs1", "pages");
		renderRequest.setAttribute("tabs1", "pages");
		renderRequest.setAttribute("addPageURL", addPageURL.toString());
		include(viewJSP, renderRequest, renderResponse);
	
	}
	
	public void showFilterContents(RenderRequest renderRequest,
			RenderResponse renderResponse) throws IOException, PortletException {
		
		String contentId = ParamUtil.getString(renderRequest, "contentId");
		String tabs1 = ParamUtil.getString(renderRequest, "tabs1");
		String keyword = ParamUtil.getString(renderRequest, "keyword", "");
		String screenName = ParamUtil.getString(renderRequest, "screenName", "");
		String emailAddress = ParamUtil.getString(renderRequest, "emailAddress", "");
		String pendingModeration = ParamUtil.getString(renderRequest, "pendingModeration", "");
		String startDate = ParamUtil.getString(renderRequest, "startDate", "");
		String endDate = ParamUtil.getString(renderRequest, "endDate", "");
		
		renderRequest.setAttribute("keyword", keyword);
		renderRequest.setAttribute("screenName", screenName);
		renderRequest.setAttribute("emailAddress", emailAddress);
		renderRequest.setAttribute("pendingModeration", pendingModeration);
		renderRequest.setAttribute("startDate", startDate);
		renderRequest.setAttribute("endDate", endDate);
		renderRequest.setAttribute("contentId", contentId);
		renderRequest.setAttribute("tabs1", tabs1);
		
		include(viewJSP, renderRequest, renderResponse);		
	}
	
	public void showEditComment(RenderRequest renderRequest,
			RenderResponse renderResponse) throws IOException, PortletException {
		String contentId = ParamUtil.getString(renderRequest, "contentId");
		String tabs1 = ParamUtil.getString(renderRequest, "tabs1");
		
		renderRequest.setAttribute("contentId", contentId);
		renderRequest.setAttribute("tabs1", tabs1);
		
		include(trackingEdit, renderRequest, renderResponse);
	}
	
	public void showFilterComment(RenderRequest renderRequest,
			RenderResponse renderResponse) throws IOException, PortletException {
		
		String tabs1 = ParamUtil.getString(renderRequest, "tabs1");
		String keyword = ParamUtil.getString(renderRequest, "keyword");
		String keywordComment = ParamUtil.getString(renderRequest, "keywordComment");
		String standardArticleCheck = ParamUtil.getString(renderRequest, "standardArticleCheck", "true");
		String standardGalleryCheck = ParamUtil.getString(renderRequest, "standardGalleryCheck", "true");
		String standardPollCheck = ParamUtil.getString(renderRequest, "standardPollCheck", "true");
		String standardMultimediaCheck = ParamUtil.getString(renderRequest, "standardMultimediaCheck", "true");
		String pendingModeration = ParamUtil.getString(renderRequest, "pendingModeration", "true");
		String startDate = ParamUtil.getString(renderRequest, "startDate", "");
		String endDate = ParamUtil.getString(renderRequest, "endDate", "");
		
		renderRequest.setAttribute("keyword", keyword);
		renderRequest.setAttribute("keywordComment", keywordComment);
		renderRequest.setAttribute("standardArticleCheck", standardArticleCheck);
		renderRequest.setAttribute("standardPollCheck", standardPollCheck);
		renderRequest.setAttribute("standardGalleryCheck", standardGalleryCheck);
		renderRequest.setAttribute("standardMultimediaCheck", standardMultimediaCheck);
		renderRequest.setAttribute("pendingModeration", pendingModeration);
		renderRequest.setAttribute("startDate", startDate);
		renderRequest.setAttribute("endDate", endDate);
		renderRequest.setAttribute("tabs1", tabs1);

		
		include(trackingEdit, renderRequest, renderResponse);		
	}
	
	
	@Override
	public void processAction(ActionRequest actionRequest,
			ActionResponse actionResponse) throws IOException, PortletException {
		
		String milenium = ParamUtil.getString(actionRequest, "milenium");
		actionResponse.setRenderParameter("milenium",milenium);	
		
		String action = ParamUtil.getString(actionRequest, "javax.portlet.action");
		try {
			if (action.equals("editTracking")) {								
				editTracking(actionRequest, actionResponse);				
			} else if (action.equals("activateCommentTracking")) {								
				activateCommentTracking(actionRequest, actionResponse);				
			} else if (action.equals("deactivateCommentTracking")) {								
				deactivateCommentTracking(actionRequest, actionResponse);				
			} else if (action.equals("filterCommentsTracking")) {								
				filterCommentsTracking(actionRequest, actionResponse);				
			} else if (action.equals("filterContentTracking")) {								
				filterContentTracking(actionRequest, actionResponse);				
			}
			
			
		} catch (PortalException e) {
			;
		} catch (SystemException e) {
			;
		}
	}
	
	public void editTracking(ActionRequest request, ActionResponse response) throws PortalException, SystemException {		
		String id = ParamUtil.getString(request, "contentId");
		String tabs1 = ParamUtil.getString(request, "tabs1");
		response.setRenderParameter("contentId", id);
		response.setRenderParameter("view", "edit");
		response.setRenderParameter("tabs1",tabs1);		
	}
	
	public void filterContentTracking(ActionRequest request, ActionResponse response) throws PortalException, SystemException {
		
		String tabs1 = ParamUtil.getString(request, "tabs1","articles");
		response.setRenderParameter("tabs1",tabs1);		
		/* Filter fields */
		String keyword = ParamUtil.getString(request, "keyword", "");
		String keywordComment = ParamUtil.getString(request, "keywordComment", "");
		
		String standardArticleCheck = ParamUtil.getString(request, "standardArticleCheck", "");
		String standardGalleryCheck = ParamUtil.getString(request, "standardGalleryCheck", "");
		String standardPollCheck = ParamUtil.getString(request, "standardPollCheck", "");
		String standardMultimediaCheck = ParamUtil.getString(request, "standardMultimediaCheck", "");
		
		String pendingModeration = ParamUtil.getString(request, "pendingModeration", "");

		/* Start Date */									
		int dateStartMonth = ParamUtil.getInteger(request, "dateStartMonth", 0);
		int dateStartDay = ParamUtil.getInteger(request, "dateStartDay", 0);
		int dateStartYear = ParamUtil.getInteger(request, "dateStartYear", 0);
		int dateStartHour = ParamUtil.getInteger(request, "dateStartHour", 0);
		int dateStartMinute = ParamUtil.getInteger(request, "dateStartMinute", 0);
		int dateStartAmPm = ParamUtil.getInteger(request, "dateStartAmPm", 0);

		if (dateStartAmPm == Calendar.PM) {
			dateStartHour += 12;
		}
		// int year, int month, int date, int hour, int minute
		Date startDate = (new GregorianCalendar(dateStartYear, dateStartMonth, dateStartDay, dateStartHour, dateStartMinute)).getTime();
		
		/* End Date */
		Calendar now = Calendar.getInstance();
		now.add(Calendar.MONTH, 1);	
		
		int dateEndMonth = ParamUtil.getInteger(request, "dateEndMonth", now.get(Calendar.MONTH));
		int dateEndDay = ParamUtil.getInteger(request, "dateEndDay", now.get(Calendar.DAY_OF_MONTH));
		int dateEndYear = ParamUtil.getInteger(request, "dateEndYear", now.get(Calendar.YEAR));
		int dateEndHour = ParamUtil.getInteger(request, "dateEndHour", now.get(Calendar.HOUR));
		int dateEndMinute = ParamUtil.getInteger(request, "dateEndMinute", now.get(Calendar.MINUTE));
		int dateEndAmPm = ParamUtil.getInteger(request, "dateEndAmPm", now.get(Calendar.AM_PM));

		if (dateEndAmPm == Calendar.PM) {
			dateEndHour += 12;
		}
		
		Date endDate = (new GregorianCalendar(dateEndYear, dateEndMonth, dateEndDay, dateEndHour, dateEndMinute)).getTime();
		
		DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		
		response.setRenderParameter("keyword", keyword);
		response.setRenderParameter("keywordComment", keywordComment);
		response.setRenderParameter("standardArticleCheck", standardArticleCheck);
		response.setRenderParameter("standardGalleryCheck", standardGalleryCheck);
		response.setRenderParameter("standardPollCheck", standardPollCheck);
		response.setRenderParameter("standardMultimediaCheck", standardMultimediaCheck);
		response.setRenderParameter("pendingModeration", pendingModeration);
		
		response.setRenderParameter("startDate", df.format(startDate));
		response.setRenderParameter("endDate", df.format(endDate));
		
		response.setRenderParameter("view", "search");
	}
	
	public void filterCommentsTracking(ActionRequest request, ActionResponse response) throws PortalException, SystemException {		
		String id = ParamUtil.getString(request, "contentId");
		String tabs1 = ParamUtil.getString(request, "tabs1");
		
		response.setRenderParameter("contentId", id);
		response.setRenderParameter("view", "filter");
		response.setRenderParameter("tabs1",tabs1);		
		
		/* Filter fields */
		String keyword = ParamUtil.getString(request, "keyword", "");
		String screenName = ParamUtil.getString(request, "screenName", "");
		String emailAddress = ParamUtil.getString(request, "emailAddress", "");
		String pendingModeration = ParamUtil.getString(request, "pendingModeration", "");
		
		/* Start Date */									
		int dateStartMonth = ParamUtil.getInteger(request, "dateStartMonth", 0);
		int dateStartDay = ParamUtil.getInteger(request, "dateStartDay", 0);
		int dateStartYear = ParamUtil.getInteger(request, "dateStartYear", 0);
		int dateStartHour = ParamUtil.getInteger(request, "dateStartHour", 0);
		int dateStartMinute = ParamUtil.getInteger(request, "dateStartMinute", 0);
		int dateStartAmPm = ParamUtil.getInteger(request, "dateStartAmPm", 0);

		if (dateStartAmPm == Calendar.PM) {
			dateStartHour += 12;
		}
		// int year, int month, int date, int hour, int minute
		Date startDate = (new GregorianCalendar(dateStartYear, dateStartMonth, dateStartDay, dateStartHour, dateStartMinute)).getTime();
		
		/* End Date */
		Calendar now = Calendar.getInstance();
		now.add(Calendar.MONTH, 1);	
		
		int dateEndMonth = ParamUtil.getInteger(request, "dateEndMonth", now.get(Calendar.MONTH));
		int dateEndDay = ParamUtil.getInteger(request, "dateEndDay", now.get(Calendar.DAY_OF_MONTH));
		int dateEndYear = ParamUtil.getInteger(request, "dateEndYear", now.get(Calendar.YEAR));
		int dateEndHour = ParamUtil.getInteger(request, "dateEndHour", now.get(Calendar.HOUR));
		int dateEndMinute = ParamUtil.getInteger(request, "dateEndMinute", now.get(Calendar.MINUTE));
		int dateEndAmPm = ParamUtil.getInteger(request, "dateEndAmPm", now.get(Calendar.AM_PM));

		if (dateEndAmPm == Calendar.PM) {
			dateEndHour += 12;
		}
		
		Date endDate = (new GregorianCalendar(dateEndYear, dateEndMonth, dateEndDay, dateEndHour, dateEndMinute)).getTime();
		
		DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		
		response.setRenderParameter("keyword", keyword);
		response.setRenderParameter("screenName", screenName);
		response.setRenderParameter("emailAddress", emailAddress);
		response.setRenderParameter("pendingModeration", pendingModeration);
		response.setRenderParameter("startDate", df.format(startDate));
		response.setRenderParameter("endDate", df.format(endDate));
	}
	
	public void activateCommentTracking(ActionRequest request, ActionResponse response) throws PortalException, SystemException {		
		long id = ParamUtil.getLong(request, "resourcePrimKey");
		CommentsLocalServiceUtil.activateComment(id);
		
		String contentId = ParamUtil.getString(request, "contentId");
		response.setRenderParameter("contentId", contentId);
		response.setRenderParameter("view", "edit");
		response.setRenderParameter("tabs1", "comments");	
	}
		
	public void deactivateCommentTracking(ActionRequest request, ActionResponse response) throws PortalException, SystemException {		
		long id = ParamUtil.getLong(request, "resourcePrimKey");
		CommentsLocalServiceUtil.deactivateComment(id);
		
		String contentId = ParamUtil.getString(request, "contentId");
		response.setRenderParameter("contentId", contentId);
		response.setRenderParameter("view", "edit");
		response.setRenderParameter("tabs1", "comments");	
	}
	
	private String trackingEdit = "";
	
}
