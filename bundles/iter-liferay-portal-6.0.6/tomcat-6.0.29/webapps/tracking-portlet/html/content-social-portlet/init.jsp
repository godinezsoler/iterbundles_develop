<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="javax.portlet.PortletPreferences"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>

<%@page import="com.liferay.portal.kernel.portlet.LiferayWindowState"%>
<%@page import="com.liferay.portlet.bookmarks.model.BookmarksFolderWrapper"%>

<%@page import="java.util.Date"%>
<%@page import="java.text.DateFormat"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.liferay.portal.kernel.util.LocaleUtil"%>
<%@page import="java.util.Locale"%>

<%@page import="com.liferay.portlet.PortletURLFactoryUtil"%>
<%@page import="javax.portlet.PortletRequest"%>
<%@page import="com.liferay.portlet.journal.model.JournalArticle"%>
<%@page import="com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil"%>
<%@page import="javax.portlet.PortletURL"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="com.liferay.portal.model.Layout"%>
<%@page import="com.liferay.portal.service.LayoutLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.news.service.PageContentLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.Constants"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%
	long companyId = company.getCompanyId();
	long globalGroupId = company.getGroup().getGroupId();	

	PortletPreferences preferences = renderRequest.getPreferences();
	
	String portletResource = ParamUtil.getString(request, "portletResource");
	
	if (Validator.isNotNull(portletResource)) {
		preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);
	}

	String lang = themeDisplay.getLocale().getLanguage();
	
	String twitter_lng = lang;
	String gplus_lng = lang;
	String tuenti_lng = lang;
	String facebook_lng = lang + "_" + lang.toUpperCase();
	
	if(lang.equals("es")){
		tuenti_lng = "es-ES";
	}else{
		tuenti_lng = "en-US";
	}
	
	
	if(lang.toLowerCase().equals("ca")){
		twitter_lng = "es";
		facebook_lng = "ca_ES";
		tuenti_lng = "ca-ES";
	}else if(lang.toLowerCase().equals("pt")){
		tuenti_lng = gplus_lng = "pt-PT";
	}else if(lang.toLowerCase().equals("en")){
		gplus_lng = "en-US";
		facebook_lng = "en_US"; 
	}

	String[] socialNetworks = preferences.getValues("social-networks", new String[]{});

	boolean fbSendButton = GetterUtil.getBoolean(preferences.getValue("facebook-send-button", null), false);
	String fbStyle = preferences.getValue("facebook-layout-style", "standard");
	boolean fbShowFaces = GetterUtil.getBoolean(preferences.getValue("facebook-show-faces", null), true);
	String fbDisplayVerb = preferences.getValue("facebook-verb", "like");
	
	String twShowCounter = preferences.getValue("twitter-counter", "horizontal");
	String twButtonsize = preferences.getValue("twitter-size", "");
	
	String gpSize = preferences.getValue("gplus-size", "standard");
	String gpAnnotation = preferences.getValue("gplus-annotation", "bubble");
	
	String tIcon = preferences.getValue("tuenti-icon", "dark");
	
%>
