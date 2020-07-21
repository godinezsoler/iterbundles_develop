<%@page import="javax.portlet.PortletPreferences"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>

<portlet:defineObjects />

<%
	String environment = IterLocalServiceUtil.getEnvironment();
	PortletPreferences preferences = renderRequest.getPreferences();
	String configId = preferences.getValue("configId", "");
	String templateId = preferences.getValue("templateId", "");
%>

<div data-config-id="<%=configId%>" data-template-id="<%=templateId%>"></div>