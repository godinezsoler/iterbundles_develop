<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@include file="init.jsp"%>

<%
	ResultRow row = (ResultRow) request.getAttribute(WebKeys.SEARCH_CONTAINER_RESULT_ROW);
	Channel channel = (Channel) row.getObject();

	long groupId = themeDisplay.getLayout().getGroupId();
	String name = portletDisplay.getRootPortletId();
	String primKey = String.valueOf(channel.getPrimaryKey());
%>

<liferay-ui:icon-menu cssClass="">

	<c:if test="<%= permissionChecker.hasPermission(groupId, name, primKey, ActionKeys.VIEW) %>">
		<portlet:actionURL name="editChannel" var="editChannelURL">
			<portlet:param name="resourcePrimKey" value="<%= String.valueOf(channel.getPrimaryKey()) %>" />
			<portlet:param name="tabs1" value="automatic" />
		</portlet:actionURL>
		<liferay-ui:icon image="edit" url="<%= editChannelURL.toString() %>" />
	</c:if>

    <c:if test="<%= permissionChecker.hasPermission(groupId, name, primKey, ActionKeys.DELETE) %>">
		<portlet:actionURL name="deleteChannel" var="deleteChannelURL">			
			<portlet:param name="resourcePrimKey" value="<%= String.valueOf(channel.getPrimaryKey()) %>" />
			<portlet:param name="tabs1" value="automatic" />
		</portlet:actionURL>
		<liferay-ui:icon image="delete" url="<%=deleteChannelURL.toString() %>" />
	</c:if>

	<portlet:actionURL name="activateChannel" var="activateChannelURL">			
			<portlet:param name="resourcePrimKey" value="<%= String.valueOf(channel.getPrimaryKey()) %>" />
			<portlet:param name="tabs1" value="automatic" />
	</portlet:actionURL>
	<liferay-ui:icon image="activate" url="<%= activateChannelURL.toString() %>" />
	
	<portlet:actionURL name="deactivateChannel" var="deactivateChannelURL">			
			<portlet:param name="resourcePrimKey" value="<%= String.valueOf(channel.getPrimaryKey()) %>" />
			<portlet:param name="tabs1" value="automatic" />
	</portlet:actionURL>
	<liferay-ui:icon image="deactivate" url="<%= deactivateChannelURL.toString() %>" />
	
</liferay-ui:icon-menu>
