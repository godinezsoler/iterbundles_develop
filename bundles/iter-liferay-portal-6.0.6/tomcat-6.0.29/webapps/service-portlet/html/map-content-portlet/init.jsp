<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.HashSet"%>
<%@page import="javax.portlet.WindowState"%>
<%@page import="javax.portlet.PortletPreferences"%>

<%@page import="com.liferay.portlet.journal.model.JournalArticle" %>
<%@page import="com.liferay.portlet.journal.service.JournalTemplateLocalServiceUtil"%>
<%@page import="com.liferay.portlet.journal.model.JournalTemplate"%>

<%@page import="com.protecmedia.iter.news.model.PageContent" %>
<%@page import="com.protecmedia.iter.news.service.PageContentLocalServiceUtil" %>
<%@page import="com.protecmedia.iter.news.service.CategorizeLocalServiceUtil" %>
<%@page import="com.protecmedia.iter.news.model.Qualification"%>
<%@page import="com.protecmedia.iter.news.service.QualificationLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>

<%@page import="com.liferay.portlet.journal.model.JournalArticleDisplay"%>
<%@page import="com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil"%>
<%@page import="java.util.Locale"%>
<%@page import="com.liferay.portlet.journalcontent.util.JournalContent"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>


<%@page import="com.liferay.portal.kernel.portlet.LiferayPortletURL"%>
<%@page import="javax.portlet.PortletURL"%>
<%@page import="com.liferay.portlet.PortletURLFactoryUtil"%>
<%@page import="com.liferay.portal.service.LayoutLocalServiceUtil"%>

<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>

<%@page import="com.liferay.portal.kernel.util.Constants"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil" %>
<%@page import="com.liferay.portal.kernel.language.LanguageUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringPool" %>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%
	PortletPreferences preferences = renderRequest.getPreferences();
	
	long globalGroupId = company.getGroup().getGroupId();	
	long companyId = PortalUtil.getCompanyId(request);
	String articleId = (String) renderRequest.getParameter(WebKeys.URL_PARAM_CONTENT_ID);	
	String environment = IterLocalServiceUtil.getEnvironment();
	
	String portletResource = ParamUtil.getString(request, "portletResource");
	
	if (Validator.isNotNull(portletResource)) {
		preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);
	}
	
	String business = preferences.getValue("business", StringPool.BLANK);
	String zoom = preferences.getValue("zoom", StringPool.BLANK);
	String zoomControl = preferences.getValue("zoomControl", StringPool.BLANK);
	String zoomControlStyle = preferences.getValue("zoomStyle", StringPool.BLANK);
	String zoomControlPosition = preferences.getValue("zoomPosition", StringPool.BLANK);
	String mapType = preferences.getValue("mapType", StringPool.BLANK);
	String mapTypeControl = preferences.getValue("mapTypeControl", StringPool.BLANK);
	String mapTypeControlStyle = preferences.getValue("mapTypeStyle", StringPool.BLANK);
	String mapTypeControlPosition = preferences.getValue("mapTypePosition", StringPool.BLANK);
	String panControl = preferences.getValue("panControl", StringPool.BLANK);
	String panPosition = preferences.getValue("panPosition", StringPool.BLANK);
	String scaleControl = preferences.getValue("scaleControl", StringPool.BLANK);
	String scalePosition = preferences.getValue("scalePosition", StringPool.BLANK);
	String overviewControl = preferences.getValue("overviewControl", StringPool.BLANK);
	String overviewOpened = preferences.getValue("overviewOpened", StringPool.BLANK);
	String streetViewControl = preferences.getValue("streetViewControl", StringPool.BLANK);
	String streetViewPosition = preferences.getValue("streetViewPosition", StringPool.BLANK);
	String rotateControl = preferences.getValue("rotateControl", StringPool.BLANK);
	String rotatePosition = preferences.getValue("rotatePosition", StringPool.BLANK);
	
	//Texto por defecto
	String defaultTextHTML = preferences.getValue("defaultTextHTML", StringPool.BLANK);
	boolean showDefaultTextHTML = GetterUtil.getBoolean(preferences.getValue("showDefaultTextHTML", null), false);

%>