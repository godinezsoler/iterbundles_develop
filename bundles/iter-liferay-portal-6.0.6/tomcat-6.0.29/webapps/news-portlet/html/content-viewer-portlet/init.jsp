<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portlet.TeaserUtil"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="javax.portlet.PortletPreferences"%>

<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil"%>
<%@page import="com.liferay.portlet.journal.model.JournalArticle"%>
<%@page import="com.liferay.util.portlet.PortletRequestUtil"%>

<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.news.service.PageContentLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.news.util.TeaserContentUtil"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%
	//Globales
	long globalGroupId = company.getGroup().getGroupId();	
	
	PortletPreferences preferences = renderRequest.getPreferences();
	String portletResource = ParamUtil.getString(request, "portletResource");
	if (Validator.isNotNull(portletResource))
		preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);
	
	//PLantillas
	String templateIdArticle = preferences.getValue("template-article", "");
	String templateIdGallery = preferences.getValue("template-gallery", "");
	String templateIdPoll = preferences.getValue("template-poll", "");
	String templateIdMultimedia = preferences.getValue("template-multimedia", "");	

	String templateIdArticleRestricted = GetterUtil.getString(preferences.getValue("templateIdArticleRestricted", null), "");
	String templateIdGalleryRestricted = GetterUtil.getString(preferences.getValue("templateIdGalleryRestricted", null), "");
	String templateIdPollRestricted = GetterUtil.getString(preferences.getValue("templateIdPollRestricted", null), "");
	String templateIdMultimediaRestricted = GetterUtil.getString(preferences.getValue("templateIdMultimediaRestricted", null), "");
	
	//Modos de plantillas
	int modeArticle = GetterUtil.getInteger(preferences.getValue("modeArticle", null), -1);
	int modeGallery = GetterUtil.getInteger(preferences.getValue("modeGallery", null), -1);
	int modePoll = GetterUtil.getInteger(preferences.getValue("modePoll", null), -1);
	int modeMultimedia = GetterUtil.getInteger(preferences.getValue("modeMultimedia", null), -1);
	
	//Texto por defecto
	String defaultTextHTML = preferences.getValue("defaultTextHTML", StringPool.BLANK);
	boolean showDefaultTextHTML = GetterUtil.getBoolean(preferences.getValue("showDefaultTextHTML", null), false);

%>