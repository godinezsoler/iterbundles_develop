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

<%@page import="java.util.Date"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.text.DateFormat"%>
<%@page import="java.text.SimpleDateFormat"%>

<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>

<%@page import="javax.portlet.PortletPreferences"%>
<%@page import="javax.portlet.PortletURL"%>

<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.search.util.SearchUtil"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%

	PortletPreferences preferences = renderRequest.getPreferences();
	
	String portletResource = ParamUtil.getString(request, "portletResource");
	if (Validator.isNotNull(portletResource))
		preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);
	
	//Página de resultados de las preferencias
	String layoutUUID = preferences.getValue("layoutPlid", null);

	//Entorno
	boolean isPreview = false;
	if(IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
		isPreview = true;

	//Botón búsqueda difusa
	String showFuzzyButton = String.valueOf(SearchUtil.showFuzzyButton(preferences.getValue("fuzzySearch", null), scopeGroupId));
	
	//Ordenación
	String order = GetterUtil.getString(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SEARCH_TYPE_BASIC), SearchUtil.SEARCH_ORDER_DEFAULT);
	
	//URL de resultados de búsqueda
	String resultsLayoutURL = SearchUtil.getSearchResultURL(request, themeDisplay, layoutUUID);
	String resultsParamURL = SearchUtil.getSearchResultBasicParams(showFuzzyButton, order);
	
	//Texto del botón buscar
	String searchLabel = preferences.getValue("searchButtonLabel", StringPool.BLANK);
	
%>