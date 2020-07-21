<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@include file="init.jsp"%>

<%
	ResultRow row = (ResultRow) request.getAttribute(WebKeys.SEARCH_CONTAINER_RESULT_ROW);
	PageContent pageContent = (PageContent) row.getObject();

	long groupId = themeDisplay.getLayout().getGroupId();
	String name = portletDisplay.getRootPortletId();
	String primKey = String.valueOf(pageContent.getPrimaryKey());
%>

<liferay-ui:icon-menu cssClass="">

	<c:if test="<%= permissionChecker.hasPermission(groupId, name, primKey, ActionKeys.VIEW) %>">
		<portlet:actionURL name="editPageContent" var="editURL">
			<portlet:param name="resourcePrimKey" value="<%= String.valueOf(pageContent.getPrimaryKey()) %>" />
			<portlet:param name="layout-id" value="<%= String.valueOf(layoutId) %>" />
		</portlet:actionURL>
		<liferay-ui:icon image="edit" url="<%= editURL.toString() %>" />
	</c:if>

    <c:if test="<%= permissionChecker.hasPermission(groupId, name, primKey, ActionKeys.DELETE) %>">
		<portlet:actionURL name="deletePageContent" var="deleteURL">			
			<portlet:param name="resourcePrimKey" value="<%= String.valueOf(pageContent.getPrimaryKey()) %>" />
			<portlet:param name="layout-id" value="<%= String.valueOf(layoutId) %>" />
		</portlet:actionURL>
		<liferay-ui:icon image="delete" url="<%=deleteURL.toString() %>" />
	</c:if>

	<portlet:actionURL name="activatePageContent" var="activateURL">			
			<portlet:param name="resourcePrimKey" value="<%= String.valueOf(pageContent.getPrimaryKey()) %>" />
			<portlet:param name="layout-id" value="<%= String.valueOf(layoutId) %>" />
	</portlet:actionURL>
	<liferay-ui:icon image="activate" url="<%= activateURL.toString() %>" />
	
	<portlet:actionURL name="deactivatePageContent" var="deactivateURL">			
			<portlet:param name="resourcePrimKey" value="<%= String.valueOf(pageContent.getPrimaryKey()) %>" />
			<portlet:param name="layout-id" value="<%= String.valueOf(layoutId) %>" />
	</portlet:actionURL>
	<liferay-ui:icon image="deactivate" url="<%= deactivateURL.toString() %>" />

	<portlet:actionURL name="increaseOrderPageContent" var="increaseOrderURL">			
			<portlet:param name="resourcePrimKey" value="<%= String.valueOf(pageContent.getPrimaryKey()) %>" />
			<portlet:param name="layout-id" value="<%= String.valueOf(layoutId) %>" />
	</portlet:actionURL>
	<liferay-ui:icon message="page-content-view-increase-order" image="top" url="<%=increaseOrderURL.toString() %>" />
	
	<portlet:actionURL name="decreaseOrderPageContent" var="decreaseOrderURL">			
			<portlet:param name="resourcePrimKey" value="<%= String.valueOf(pageContent.getPrimaryKey()) %>" />
			<portlet:param name="layout-id" value="<%= String.valueOf(layoutId) %>" />
	</portlet:actionURL>	
	<liferay-ui:icon message="page-content-view-decrease-order" image="bottom" url="<%= decreaseOrderURL.toString() %>" />
	
</liferay-ui:icon-menu>
