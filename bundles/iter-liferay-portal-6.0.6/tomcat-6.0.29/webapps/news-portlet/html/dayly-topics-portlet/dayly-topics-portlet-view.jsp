<%@ include file="dayly-topics-portlet-init.jsp" %>

<%
// Id de la instancia del portlet
//final String portletId = themeDisplay.getPortletDisplay().getId();

// Obtenemos el html para los dayly topics
DaylyTopic.getDaylyTopicsHtml(themeDisplay, response, beforeText, afterText);
%>