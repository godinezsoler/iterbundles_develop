<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@include file="init.jsp"%>

<%
	ResultRow row = (ResultRow) request.getAttribute(WebKeys.SEARCH_CONTAINER_RESULT_ROW);
	Qualification qualification = (Qualification) row.getObject();

	long groupId = themeDisplay.getLayout().getGroupId();
	String name = portletDisplay.getRootPortletId();
	String primKey = String.valueOf(qualification.getPrimaryKey());
%>

<liferay-ui:icon-menu cssClass="">

	<c:if test="<%= permissionChecker.hasPermission(groupId, name, primKey, ActionKeys.VIEW) %>">
		<portlet:actionURL name="editQualification" var="editURL">
			<portlet:param name="resourcePrimKey" value="<%= String.valueOf(qualification.getPrimaryKey()) %>" />
			<portlet:param name="tabs1" value="qualification" />
		</portlet:actionURL>
		<liferay-ui:icon image="edit" url="<%= editURL.toString() %>" />
	</c:if>

    <c:if test="<%= permissionChecker.hasPermission(groupId, name, primKey, ActionKeys.DELETE) %>">
		<portlet:actionURL name="deleteQualification" var="deleteURL">			
			<portlet:param name="resourcePrimKey" value="<%= String.valueOf(qualification.getPrimaryKey()) %>" />
			<portlet:param name="tabs1" value="qualification" />
		</portlet:actionURL>
		<liferay-ui:icon image="delete" url="<%= deleteURL.toString() %>" />
	</c:if>	
	
</liferay-ui:icon-menu>
