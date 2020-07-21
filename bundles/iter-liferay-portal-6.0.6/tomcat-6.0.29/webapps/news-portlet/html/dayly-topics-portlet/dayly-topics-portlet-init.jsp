<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>

<%@ page import="javax.portlet.PortletPreferences"%>
<%@ page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>
<%@ page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@ page import="com.protecmedia.iter.news.util.MenuUtil"%>
<%@ page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@ page import="com.liferay.portal.util.PortalUtil"%>
<%@ page import="com.liferay.portal.theme.RequestVars"%>
<%@ page import="com.liferay.portal.theme.ThemeDisplay"%>
<%@ page import="com.protecmedia.iter.news.util.DaylyTopic"%>

<%@ page import="java.util.ArrayList"%>

<portlet:defineObjects/> 
<liferay-theme:defineObjects/>

<%	// Obtenemos las preferencias del portlet
	PortletPreferences preferences = renderRequest.getPreferences();
	
	final String beforeText = preferences.getValue("textBefore", "");
	final String afterText  = preferences.getValue("textAfter",  "");
%>