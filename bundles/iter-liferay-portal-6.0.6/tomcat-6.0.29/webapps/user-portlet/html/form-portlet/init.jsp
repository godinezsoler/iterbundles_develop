<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>


<%@ page import="com.liferay.portal.kernel.util.Validator" %>
<%@ page import="com.liferay.portal.kernel.util.ParamUtil" %>
<%@ page import="com.liferay.portal.kernel.util.StringPool" %>
<%@ page import="com.liferay.portal.kernel.util.Constants" %>
<%@ page import="com.liferay.portal.kernel.util.GetterUtil" %>
<%@ page import="com.liferay.portal.kernel.dao.search.SearchContainer" %>
<%@ page import="com.liferay.portal.service.GroupLocalServiceUtil" %>
<%@ page import="com.liferay.portal.model.Group" %>
<%@ page import="com.liferay.portlet.PortletPreferencesFactoryUtil" %>

<%@ page import="javax.portlet.PortletPreferences" %>
<%@ page import="javax.portlet.WindowState" %>
<%@ page import="javax.portlet.PortletURL" %>
<%@ page import="com.liferay.portal.util.PortalUtil" %>

<%@ page import="com.protecmedia.iter.user.util.FormUtil"%>
<%@ page import="com.liferay.portal.kernel.xml.Document"%>
<%@ page import="com.liferay.portal.kernel.xml.Element"%>
<%@ page import="com.liferay.portal.kernel.xml.Node"%>
<%@ page import="com.liferay.portal.kernel.xml.SAXReaderUtil"%>
<%@ page import="com.liferay.portal.kernel.xml.XMLHelper"%>
<%@ page import="com.liferay.portal.kernel.xml.XPath"%>
<%@ page import="com.liferay.portal.service.PortalLocalServiceUtil"%>
<%@ page import="java.util.List"%>

<portlet:defineObjects />

<liferay-theme:defineObjects />

<%

PortletURL portletURL = renderResponse.createRenderURL();

PortletPreferences preferences = renderRequest.getPreferences();

String portletResource = ParamUtil.getString(request, "portletResource");

if (Validator.isNotNull(portletResource)) {
	preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);
}

boolean defaultContext = GetterUtil.getBoolean(preferences.getValue("context", null), true);
String formId = preferences.getValue("selectedFormId", StringPool.BLANK);
String restrictedMessage = preferences.getValue("restrictedFormMsg", StringPool.BLANK);

%>
