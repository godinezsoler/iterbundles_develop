<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@ page import="javax.portlet.PortletPreferences" %>
<%@ page import="com.liferay.portal.kernel.util.GetterUtil" %>

<liferay-theme:defineObjects />
<portlet:defineObjects />

<%

	PortletPreferences preferences = renderRequest.getPreferences();
	
	String slotId = preferences.getValue("advContainer", "");
	boolean showAdvMsg = GetterUtil.getBoolean(preferences.getValue("showAdvLabel", "false"), false);
	String advMsg = preferences.getValue("advLabel", "");
%>