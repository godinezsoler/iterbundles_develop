<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@include file="init.jsp"%>

<%@page import="com.liferay.portal.kernel.dao.search.ResultRow"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>

<%  
	//Vemos si estamos en la vista de milenium
	String milenium = ParamUtil.getString(request, "milenium", "");

	ResultRow row = (ResultRow) request.getAttribute(WebKeys.SEARCH_CONTAINER_RESULT_ROW);	
	Comments comment = (Comments) row.getObject();

	long groupId = themeDisplay.getCompanyGroupId();
	String name = portletDisplay.getRootPortletId();
	long primKey = comment.getId();
%>

<liferay-ui:icon-menu cssClass="">

	<portlet:actionURL name="activateCommentTracking" var="activateURL">			
		<portlet:param name="resourcePrimKey" value="<%= String.valueOf(primKey) %>" />
		<portlet:param name="contentId" value="<%= comment.getContentId() %>" />
		<portlet:param name="milenium" value="<%= milenium %>" />		
	</portlet:actionURL>
	<liferay-ui:icon image="activate" url="<%= activateURL.toString() %>" />


	<portlet:actionURL name="deactivateCommentTracking" var="deactivateURL">			
		<portlet:param name="resourcePrimKey" value="<%= String.valueOf(primKey) %>" />
		<portlet:param name="contentId" value="<%= comment.getContentId() %>" />	
		<portlet:param name="milenium" value="<%= milenium %>" />
	</portlet:actionURL>
	<liferay-ui:icon image="deactivate" url="<%= deactivateURL.toString() %>" />

</liferay-ui:icon-menu>
