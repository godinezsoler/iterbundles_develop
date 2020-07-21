<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.util.SectionUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletPreferencesTools"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="javax.portlet.PortletPreferences"%>

<%@page import="com.liferay.util.portlet.PortletRequestUtil"%>
<%@page import="com.liferay.portal.kernel.language.LanguageUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil"%>
<%@page import="com.liferay.portlet.journal.model.JournalArticle"%>
<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>

<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.base.service.util.GroupMgr"%>
<%@page import="com.protecmedia.iter.news.service.CategorizeLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.news.service.PageContentLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.news.util.TopicsUtil"%>
<%@page import="com.protecmedia.iter.news.util.TeaserContentUtil"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil" %>
<%@page import="com.liferay.portal.kernel.velocity.VelocityContext"%>

<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%!
	private static Log _log = LogFactoryUtil.getLog("news-portlet.docroot.html.alert-portlet.view.jsp");
%>

<%
	String environment = IterLocalServiceUtil.getEnvironment();

	//Globales
	long globalGroupId = company.getGroup().getGroupId();
	long companyId = company.getCompanyId();
	
	PortletPreferences preferences = renderRequest.getPreferences();
	String portletResource = ParamUtil.getString(request, "portletResource");
	if (Validator.isNotNull(portletResource))
		preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);
	
	//Mostrar
	String title = preferences.getValue("title", "");
	int numAlerts = GetterUtil.getInteger(preferences.getValue("numAlerts", null), 1);
	int time = GetterUtil.getInteger(preferences.getValue("time", null), 1);
	int fade = GetterUtil.getInteger(preferences.getValue("fade", null), 300);

	//Modelo
	boolean defaultMode = GetterUtil.getBoolean(preferences.getValue("defaultMode", null), true);
	long modelId = GetterUtil.getLong(preferences.getValue("modelId", null), -1);
	
	//Plantillas
	String templateId = preferences.getValue("templateId", "");
	String templateIdRestricted = GetterUtil.getString(preferences.getValue("templateIdArticleRestricted", null), "");
	int modeArticle = GetterUtil.getInteger(preferences.getValue("modeArticle", null), -1);
	
	//Calificación
	String[] qualificationId =preferences.getValues("qualificationId", null);
	
	String context = GetterUtil.getString(preferences.getValue("context", null), "");
	
	//Categorías
	int categoryOperation = GetterUtil.getInteger(preferences.getValue("categoryOperation", null), -1);
	long[] contentCategoryIdsLong = PortletPreferencesTools.getContentCategoriesIds(preferences);
	
	//Páginas
	boolean defaultLayout 	= PortletPreferencesTools.getDefaultLayout(preferences);
	int sectionToShow 		= PortletPreferencesTools.getSectionToShow(preferences);
	String[] layoutIds 		= PortletPreferencesTools.getLayoutIds(preferences, SectionUtil.getSectionPlid(request), defaultLayout);
	String[] ctxLayouts		= (sectionToShow != IterKeys.SECTION_TO_SHOW_SOURCE) ? null : layoutIds;
	
	//Estructura
	List<String> structures = new ArrayList<String>();
	structures.add(IterKeys.STRUCTURE_ARTICLE);
	
	//Texto por defecto
	String defaultTextHTML = preferences.getValue("defaultTextHTML", StringPool.BLANK);
	boolean showDefaultTextHTML = GetterUtil.getBoolean(preferences.getValue("showDefaultTextHTML", null), false);
	
%>