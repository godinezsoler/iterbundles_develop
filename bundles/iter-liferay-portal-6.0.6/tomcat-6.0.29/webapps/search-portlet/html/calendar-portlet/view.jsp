<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@ include file="init.jsp" %> 

<%@page import="com.liferay.portal.util.PortalUtil"%>

<% 
	String urlKey = "";
	boolean hasPrefix = PortalUtil.getCheckMappingWithPrefix();
	if(!PortalUtil.getNewsMappingPrefix().equals(""))
	{
		urlKey = PortalUtil.getNewsMappingPrefix().substring(1, PortalUtil.getNewsMappingPrefix().length()-1);
	}
	String language = CalendarUtil.getJQueryLanguageId(themeDisplay.getLocale());
%>

<script src="<%= request.getContextPath() %>/js/datepicker.js" type="text/javascript"></script>

<script>
	jQryIter(document).ready(function() {
		executeDatePicker('<%=urlKey %>', <%=hasPrefix %>, '<%=language %>');
	});
</script>

<div id="calendar-datepicker"></div>