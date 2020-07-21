<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portlet.TeaserUtil"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="java.util.List"%>
<%@page import="javax.portlet.PortletPreferences"%>
<%@page import="javax.portlet.PortletURL"%>

<%@page import="com.liferay.util.portlet.PortletRequestUtil"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.HttpUtil"%>
<%@page import="com.liferay.portal.kernel.search.Hits"%>
<%@page import="com.liferay.portal.kernel.search.Field"%>
<%@page import="com.liferay.portal.kernel.servlet.SessionErrors"%>
<%@page import="com.liferay.portal.kernel.dao.search.ResultRow"%>
<%@page import="com.liferay.portal.kernel.dao.search.SearchContainer"%>
<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>
<%@page import="com.liferay.portlet.journal.model.JournalArticle"%>
<%@page import="com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil"%>

<%@page import="com.protecmedia.iter.base.model.Communities"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.base.service.CommunitiesLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.news.model.PageContent"%>
<%@page import="com.protecmedia.iter.news.service.PageContentLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.search.util.SearchUtil"%>
<%@page import="com.protecmedia.iter.search.util.SearchResults"%>

<%@page import="com.liferay.portal.kernel.util.CategoriesUtil"%>
<%@page import="java.text.ParseException"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.text.DateFormat"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.Date"%>
<%@page import="com.protecmedia.iter.search.util.SearchOptions"%>
<%@page import="com.protecmedia.iter.search.util.SearchResults"%>
<%@page import="com.liferay.portal.kernel.velocity.VelocityContext"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%
	final String TOP = "top";
	final String BOTH = "both";
	final String BOTTOM = "bottom";

	long globalGroupId = company.getGroup().getGroupId();
	
	PortletPreferences preferences = renderRequest.getPreferences();
	String portletResource = ParamUtil.getString(request, "portletResource");
	if (Validator.isNotNull(portletResource)) 
		preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);
	
	//Plantillas
	String templateIdArticle = preferences.getValue("template-article", "");
	String templateIdPoll = preferences.getValue("template-poll", "");
	
	String templateIdArticleRestricted = GetterUtil.getString(preferences.getValue("templateIdArticleRestricted", null), "");
	String templateIdPollRestricted = GetterUtil.getString(preferences.getValue("templateIdPollRestricted", null), "");
	
	int modeArticle = GetterUtil.getInteger(preferences.getValue("modeArticle", null), -1);
	int modePoll = GetterUtil.getInteger(preferences.getValue("modePoll", null), -1);
	
	//Modelos
	boolean defaultMode = GetterUtil.getBoolean(preferences.getValue("defaultMode", null), true);
	long modelId = GetterUtil.getLong(preferences.getValue("modelId", null), -1);
	
	//Paginacion
	int resultStart = GetterUtil.getInteger(preferences.getValue("resultStart", null), 0);
	int resultEnd = GetterUtil.getInteger(preferences.getValue("resultEnd", null), 1);
	boolean paged = GetterUtil.getBoolean(preferences.getValue("paged", "true"), true);
	String paginationButtonsPosition = GetterUtil.getString(preferences.getValue("paginationButtonsPosition", BOTTOM),BOTTOM);
	String paginationHtmlPosition = GetterUtil.getString(preferences.getValue("paginationHtmlPosition", "l-a-s"), "l-a-s");
	boolean showNonActiveContents = GetterUtil.getBoolean(preferences.getValue("showNonActiveContents", null), false);
	String buttonPrev = GetterUtil.getString(preferences.getValue("buttonPrev", null),"Retroceder");		
	String buttonNext = GetterUtil.getString(preferences.getValue("buttonNext", null),"Avanzar");	
	String buttonShowMore = "";
	boolean showMore = false;
	int delta = GetterUtil.getInteger(preferences.getValue("teaserPerPage", null), 1);
	
	//Etiquetas
	String fuzzyButtonLabel = preferences.getValue("fuzzyButtonLabel", StringPool.BLANK);
	
	String suggestFuzzy = preferences.getValue("fuzzySuggestion", StringPool.BLANK);
	
	String noResults = preferences.getValue("noResults", StringPool.BLANK);
	
	String searchResults = preferences.getValue("searchResultsLabel", StringPool.BLANK);
	
	String resultsWereFound = preferences.getValue("resultsWereFoundLabel", StringPool.BLANK);
%>
