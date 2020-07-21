/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.search.portlet;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.util.CategoriesUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.PortletURLFactoryUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.search.util.SearchOptions;
import com.protecmedia.iter.search.util.SearchResults;
import com.protecmedia.iter.search.util.SearchUtil;

/**
 * Portlet implementation class SearchResultsPortlet
 */
public class SearchResultsPortlet extends MVCPortlet {
	
	private static Log _log = LogFactoryUtil.getLog(SearchResultsPortlet.class);
	
	@Override
	public void processAction(ActionRequest actionRequest, ActionResponse actionResponse) 
			throws IOException, PortletException 
	{
//		super.processAction(actionRequest, actionResponse);
//
//		ThemeDisplay themeDisplay = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);
//		
//		HttpServletRequest request = PortalUtil.getHttpServletRequest(actionRequest);
//
//		String keywords = ParamUtil.getString(actionRequest, "keywords", "");
//		
//		boolean fuzzy = ParamUtil.getBoolean(actionRequest, "fuzzy", false);
//		boolean fuzzyOption = ParamUtil.getBoolean(actionRequest, "fuzzyOption", false);
//		
//		String startDate = ParamUtil.getString(actionRequest, "startDate", "");
//		String endDate = ParamUtil.getString(actionRequest, "endDate", "");
//		
//		String order = ParamUtil.getString(actionRequest, "order", "relevance");
//		
//		boolean checkArticle = ParamUtil.getBoolean(actionRequest, "checkArticle", false);
//		boolean checkGallery = ParamUtil.getBoolean(actionRequest, "checkGallery", false);
//		boolean checkPoll = ParamUtil.getBoolean(actionRequest, "checkPoll", false);
//		boolean checkMultimedia = ParamUtil.getBoolean(actionRequest, "checkMultimedia", false);
//		
//		String categoryIds = ParamUtil.getString(actionRequest, "assetCategoryIds", "0");
//		String layoutsPlid = ParamUtil.getString(actionRequest, "layoutsPlid", "0");
//		
//		String delta = ParamUtil.getString(actionRequest, "delta", "5");
//		String cur = ParamUtil.getString(actionRequest, "cur", "1");
//	
//		// Creamos la URL de la búsqueda
//		PortletURL url = PortletURLFactoryUtil.create(request, SearchUtil.SEARCH_PORTLET_RESULTS_ID, themeDisplay.getPlid(), PortletMode.VIEW.toString());
//		url.setParameter("keywords", keywords);
//		url.setParameter("fuzzy", String.valueOf(fuzzy));	
//		url.setParameter("fuzzyOption", String.valueOf(fuzzyOption));
//		url.setParameter("startDate", startDate);
//		url.setParameter("endDate", endDate);
//		url.setParameter("order", order);
//		url.setParameter("checkArticle", String.valueOf(checkArticle));
//		url.setParameter("checkGallery", String.valueOf(checkGallery));
//		url.setParameter("checkPoll", String.valueOf(checkPoll));
//		url.setParameter("checkMultimedia", String.valueOf(checkMultimedia));				
//		url.setParameter("categoryIds", categoryIds);
//		url.setParameter("layoutsPlid", layoutsPlid);
//		url.setParameter("delta", delta);
//		url.setParameter("cur", cur);
//
//		actionResponse.sendRedirect(url.toString());

	}
	
	@Override
	public void render(RenderRequest request, RenderResponse response)
			throws PortletException, IOException 
	{
		try 
		{			
//			ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
//			
//			String keywords = HttpUtil.decodeURL(ParamUtil.get(request, "keywords", ""));
//			
//			boolean fuzzy = ParamUtil.getBoolean(request, "fuzzy", false);
//			if (fuzzy && !keywords.isEmpty())
//				keywords += "~"; 
//
//			String endDate = ParamUtil.getString(request, "endDate", "");
//			String startDate = ParamUtil.getString(request, "startDate", "");
//			
//			String order = ParamUtil.getString(request, "order", "relevance");
//			
//			boolean checkArticle = ParamUtil.getBoolean(request, "checkArticle", true);
//			boolean checkGallery = ParamUtil.getBoolean(request, "checkGallery", true);
//			boolean checkPoll = ParamUtil.getBoolean(request, "checkPoll", true);
//			boolean checkMultimedia = ParamUtil.getBoolean(request, "checkMultimedia", true);
//
//			String categoryIds = ParamUtil.getString(request, "categoryIds", "0");
//			List<Long> listCategoryIds = CategoriesUtil.getCategoriesIdsLong(categoryIds);
//			List<Long> layoutsPlid = SearchUtil.getLayoutsPidLong(SearchUtil.getLayouts(request));
//		
//			int delta =  ParamUtil.get(request, "delta", 5); 
//			int cur = ParamUtil.get(request, "cur", 1);
//
//			SearchResults results = new SearchResults();
//			SearchOptions so = new SearchOptions();
//			so.setCompanyId(themeDisplay.getCompanyId());
//			so.setText(keywords);
//			so.setCheckArticle(checkArticle);
//			so.setCheckGallery(checkGallery);
//			so.setCheckMultimedia(checkMultimedia);
//			so.setCheckPoll(checkPoll);
//			
//			if(delta > SearchUtil.MAX_ITEMS_PER_PAGE)
//			{
//				String currentURL = PortalUtil.getCurrentURL(request);
//				if(Validator.isNull(currentURL))
//					currentURL = "";	
//				
//				String msg = "Current search: \"" + currentURL + "\" is not allowed. More than " + SearchUtil.MAX_ITEMS_PER_PAGE + " items per page";
//				SessionErrors.add(request, "search-max-elements-error");
//				_log.error(msg);
//			}
//			else
//			{
//				so.setItemsPerPage(delta);
//				so.setPage(cur);
//				so.setOrder(order);
//				so.setCategoriesIds(listCategoryIds);
//				so.setLayoutsPlid(layoutsPlid);
//				
//				try
//				{
//					DateFormat df = new SimpleDateFormat(IterKeys.URL_PARAM_DATE_FORMAT);
//					Date to = df.parse(endDate);
//					Date from = df.parse(startDate);
//					so.setStartDate(from);
//					so.setEndDate(to);
//				}
//				catch(ParseException pe)
//				{
//					so.setStartDate(null);
//					so.setEndDate(null);
//				}
//				
//				results = SearchUtil.search(so, request);
//				request.setAttribute("results", results);
//			}
		}
		catch (Exception e) 
		{		
			_log.error(e.toString());
			_log.trace(e);
		}
		finally
		{
			include(viewJSP, request, response);
		}
	}			
}