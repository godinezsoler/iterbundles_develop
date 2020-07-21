<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="java.util.Locale" %>
<%@page import="java.util.List"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.text.DateFormatSymbols"%>
<%@page import="java.text.DateFormat"%>
<%@page import="java.util.GregorianCalendar" %>

<%@page import="javax.portlet.PortletPreferences"%>

<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.KeyValuePair"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.CategoriesUtil"%>
<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>

<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.base.service.util.GroupMgr"%>
<%@page import="com.protecmedia.iter.search.util.SearchUtil"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%

	long companyId = themeDisplay.getCompanyId();
	long groupId = GroupMgr.getGlobalGroupId();

	PortletPreferences preferences = renderRequest.getPreferences();
	
	String portletResource = ParamUtil.getString(request, "portletResource");
	if (Validator.isNotNull(portletResource))
		preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);
	
	//Página de resultados de las preferencias
	String layoutUUID = preferences.getValue("layoutPlid", null);
	
	//Páginas a incluir
	String[] layoutIds = null;
	boolean defaultLayout = GetterUtil.getBoolean(preferences.getValue("defaultLayout", null), false);
	if (defaultLayout)
		layoutIds = new String[] {layout.getUuid()};
	else
		layoutIds = preferences.getValues("layoutIds", new String[0]);
	
	List<String> layoutsPlid = SearchUtil.getLayoutsPidLong(layoutIds);
	
	//Filtros a mostrar
	boolean displaySettings = GetterUtil.getBoolean(preferences.getValue("displaySettings", null), false);	
	String[] filterIds = preferences.getValues("filterIds", new String[]{});
	
	//Metadatos a excluir
	String excludedVocabularyIds = SearchUtil.getPreference(preferences, IterKeys.PREF_EXCLUDE_VOC_IDS);
	String excludedCategoryIds = SearchUtil.getPreference(preferences, IterKeys.PREF_EXCLUDE_CAT_IDS);
	
	//Botón búsqueda difusa
	String showFuzzyButton = String.valueOf(SearchUtil.showFuzzyButton(preferences.getValue("fuzzySearch", null), scopeGroupId));
	
	//URL de resultados de búsqueda
	String resultsLayoutURL = SearchUtil.getSearchResultURL(request, themeDisplay, layoutUUID);
	
	//Idioma personalizado
	String customLanguage = preferences.getValue("customLanguage", "es_ES").substring(0,2);
	
	//Literales
	String searchLabel = preferences.getValue("searchLabel", StringPool.BLANK);
	
	String articlesLabel = preferences.getValue("articlesLabel", StringPool.BLANK);
	
	String pollsLabel = preferences.getValue("pollsLabel", StringPool.BLANK);
	
	String orderByLabel = preferences.getValue("orderByLabel", StringPool.BLANK);
	
	String relevanceLabel = preferences.getValue("relevanceLabel", StringPool.BLANK);
	
	String titleLabel = preferences.getValue("titleLabel", StringPool.BLANK);
	
	String dateLabel = preferences.getValue("dateLabel", StringPool.BLANK);
	
	String fromLabel = preferences.getValue("fromLabel", StringPool.BLANK);
	
	String toLabel = preferences.getValue("toLabel", StringPool.BLANK);
	
	String viewsLabel = preferences.getValue("viewsLabel", StringPool.BLANK);
%>
