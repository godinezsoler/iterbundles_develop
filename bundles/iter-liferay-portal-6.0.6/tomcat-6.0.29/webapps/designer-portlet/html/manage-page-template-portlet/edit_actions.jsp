<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@include file="init.jsp"%>

<%
	ResultRow row = (ResultRow) request.getAttribute(WebKeys.SEARCH_CONTAINER_RESULT_ROW);
	PageTemplate pageTemplate = (PageTemplate) row.getObject();

	long groupId = themeDisplay.getLayout().getGroupId();
	String name = portletDisplay.getRootPortletId();
	String primKey = String.valueOf(pageTemplate.getPrimaryKey());
%>


<liferay-ui:icon-menu cssClass="">
	
	
	<portlet:actionURL name="infoPageTemplate" var="infoURL">
		<portlet:param name="resourcePrimKey" value="<%= String.valueOf(pageTemplate.getPrimaryKey()) %>" />			
	</portlet:actionURL>
	<liferay-ui:icon image="view" label="manage-page-template-info" message="manage-page-template-info" url="<%= infoURL.toString() %>" />
	
	<portlet:actionURL name="editPageTemplate" var="editURL">
		<portlet:param name="resourcePrimKey" value="<%= String.valueOf(pageTemplate.getPrimaryKey()) %>" />			
	</portlet:actionURL>
	<liferay-ui:icon image="edit" url="<%= editURL.toString() %>" />
	
	<portlet:actionURL name="setDefaultPageTemplate" var="setDefaultURL">
		<portlet:param name="resourcePrimKey" value="<%= String.valueOf(pageTemplate.getPrimaryKey()) %>" />			
	</portlet:actionURL>
	<liferay-ui:icon image="activate" url="<%= setDefaultURL.toString() %>" />
	
	<%
		String editTemplateURL = "";
		try {
			Layout layoutTpl = LayoutLocalServiceUtil.getLayout(pageTemplate.getLayoutId());
			editTemplateURL = PortalUtil.getLayoutFullURL(layoutTpl, themeDisplay);
		} catch (Exception e) {			
		}
	%>
	<c:if test='<%= !editTemplateURL.equals("") %>'>
		<liferay-ui:icon image="preview" label="manage-page-template-edit-template-url" target="_blank" message="manage-page-template-edit-template-url" url="<%= editTemplateURL %>" />
	</c:if>

    <c:if test="<%= permissionChecker.hasPermission(groupId, name, primKey, ActionKeys.DELETE) && !pageTemplate.getDefaultTemplate() %>">
		<portlet:actionURL name="deletePageTemplate" var="deleteURL">			
			<portlet:param name="resourcePrimKey" value="<%= String.valueOf(pageTemplate.getPrimaryKey()) %>" />			
		</portlet:actionURL>
		<liferay-ui:icon image="delete" url="<%=deleteURL.toString() %>" />
	</c:if>
		
</liferay-ui:icon-menu>
