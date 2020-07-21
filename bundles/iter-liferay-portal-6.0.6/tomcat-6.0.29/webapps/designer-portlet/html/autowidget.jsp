<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.kernel.util.PortletSSIMgr"%>

<%
	String widgetPortletId 	= PortalUtil.getPortletId(request); 
	String widgetContent 	= PortletSSIMgr.getSSIHTML(request, widgetPortletId);
%>	
	<%= widgetContent %>	
