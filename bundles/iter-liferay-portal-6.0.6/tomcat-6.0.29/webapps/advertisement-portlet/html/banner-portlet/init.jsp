<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@page import="com.liferay.portal.kernel.util.SectionUtil"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="java.util.Map"%>
<%@page import="com.liferay.portlet.AdvertisementUtil"%>
<%@page import="com.liferay.portlet.ContextVariables"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@ page import="com.liferay.portal.kernel.language.UnicodeLanguageUtil" %>
<%@ page import="com.liferay.portal.kernel.util.Constants" %>
<%@ page import="com.liferay.portal.kernel.util.ParamUtil" %>
<%@ page import="com.liferay.portal.kernel.util.StringPool" %>
<%@ page import="com.liferay.portal.kernel.util.Validator" %>
<%@ page import="com.liferay.portlet.PortletPreferencesFactoryUtil" %>
<%@ page import="com.liferay.portal.kernel.util.GetterUtil" %>
<%@ page import="com.protecmedia.iter.base.service.util.IterKeys"%>

<%@ page import="javax.portlet.PortletURL" %>
<%@ page import="javax.portlet.ActionRequest" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="javax.portlet.PortletPreferences" %>
<%@ page import="javax.portlet.PortletSession" %>

<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.model.Layout"%>
<%@page import="com.liferay.portal.model.Image"%>
<%@page import="com.liferay.portal.service.ImageLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.service.LayoutLocalServiceUtil"%>
<%@page import="com.liferay.portal.theme.ThemeDisplay"%>
<%@page import="com.liferay.portal.kernel.exception.PortalException"%>
<%@page import="com.liferay.portal.kernel.exception.SystemException"%>
<%@page import="com.liferay.portal.kernel.util.ListUtil"%>

<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="javax.portlet.PortletURL"%>
<%@page import="javax.portlet.RenderResponse"%>

<%@ page import="com.protecmedia.iter.base.service.util.IterKeys"%>




<liferay-theme:defineObjects />
<portlet:defineObjects />

<%
	PortletPreferences preferences = renderRequest.getPreferences();
	
	String portletResource = ParamUtil.getString(request, "portletResource");
	
	if (Validator.isNotNull(portletResource)) {
		preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);
	}
	
	
	String bannerType = preferences.getValue("bannerType", "");
	String bannerTextHTML = preferences.getValue("bannerTextHTML", "");
	String bannerSourceImage = preferences.getValue("bannerSourceImage", "");
	String bannerURLImage = preferences.getValue("bannerURLImage", "");
	String bannerLibraryImage = preferences.getValue("bannerLibraryImage", "");
	String bannerSourceFlash = preferences.getValue("bannerSourceFlash", "");
	String bannerURLFlash = preferences.getValue("bannerURLFlash", "");
	String bannerLibraryFlash = preferences.getValue("bannerLibraryFlash", "");
	String flashWidth = preferences.getValue("flashWidth", "");
	if (flashWidth == ""){
		flashWidth = "200";
	}
	String flashHeight = preferences.getValue("flashHeight", "");
	if (flashHeight == ""){
		flashHeight = "200";
	}
	boolean advertisementLabel = GetterUtil.getBoolean(preferences.getValue("advertisementLabel", "false"), false);	
	
%>

