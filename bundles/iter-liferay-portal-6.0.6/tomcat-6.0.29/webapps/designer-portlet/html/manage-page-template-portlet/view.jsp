<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@include file="init.jsp"%>

<%@page import="javax.portlet.PortletURL"%>

<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.servlet.ImageServletTokenUtil"%><jsp:useBean id="addPageTemplateURL" class="java.lang.String" scope="request" />

<c:choose>	

<c:when test='<%= scopeGroupId == globalGroupId  %>'>
	
	<liferay-ui:message key="page-template-portlet-not-available-in-global-environment" />

</c:when>
<c:otherwise>

<jsp:useBean id="deletePageTemplatesURL" class="java.lang.String" scope="request" />

<%	
	String editURL = "self.location = '" + addPageTemplateURL + "'";
%>

<liferay-ui:success key="manage-page-template-updated" message="manage-page-template-updated" />
<liferay-ui:success key="manage-page-template-added" message="manage-page-template-added" />

<liferay-ui:success key="manage-page-template-deleted" message="manage-page-template-deleted" />
<liferay-ui:success key="manage-page-template-deleted-all" message="manage-page-template-deleted-all" />

<liferay-ui:success key="manage-page-template-set-default" message="manage-page-template-set-default" />

<liferay-ui:error key="manage-page-template-default-page-template-can-not-be-deleted" message="manage-page-template-default-page-template-can-not-be-deleted" />
<liferay-ui:error key="manage-page-template-selected-default-error" message="manage-page-template-selected-default-error" />

<liferay-ui:error key="manage-page-template-id-not-exist" message="manage-page-template-id-not-exist" />
<liferay-ui:error key="manage-page-template-deleted-all-error" message="manage-page-template-deleted-all-error" />

<liferay-ui:error key="manage-page-template-error-creation-layout-template-page" message="manage-page-template-error-creation-layout-template-page" />
<liferay-ui:error key="manage-page-template-error-default-parent-template-page" message="manage-page-template-error-default-parent-template-page" />

<aui:button-row>				
	<c:if test='<%= (Boolean)request.getAttribute("hasAddPermission") %>'>						
		<aui:button onClick="<%= editURL %>" value="manage-page-template-view-new-page-template" />			
	</c:if>
	<c:if test='<%= (Boolean)request.getAttribute("hasDeletePermission") %>'>						
		<aui:button onClick='<%= renderResponse.getNamespace() + "deletePageTemplates();" %>' value="manage-page-template-view-delete-page-template" />			
	</c:if>
</aui:button-row>

<div class="separator article-separator"><!-- --></div>	

<aui:form name="fm" method="post" action="<%= deletePageTemplatesURL %>">
	<liferay-ui:search-container emptyResultsMessage="manage-page-template-view-empty-result" rowChecker="<%= new RowChecker(renderResponse) %>" >
		<liferay-ui:search-container-results >
			<%
				List<PageTemplate> tempResults = PageTemplateLocalServiceUtil.getPageTemplates(scopeGroupId);
				results = ListUtil.subList(tempResults, searchContainer.getStart(), searchContainer.getEnd());	
				total = tempResults.size();							
				pageContext.setAttribute("results", results);
				pageContext.setAttribute("total", total);
			%>
		</liferay-ui:search-container-results>
	
		<liferay-ui:search-container-row className="com.protecmedia.iter.designer.model.PageTemplate" keyProperty="id" modelVar="pageTemplate" >
			<%
				PortletURL editPageTemplate = renderResponse.createActionURL();
				editPageTemplate.setParameter("resourcePrimKey", String.valueOf(pageTemplate.getPrimaryKey()));
				editPageTemplate.setParameter("javax.portlet.action", "editPageTemplate");				
			
				String description = pageTemplate.getDescription();			
				description.substring(0, (description.length() < DesignerConstantsUtil.MAX_STRING_SIZE) ? description.length() : DesignerConstantsUtil.MAX_STRING_SIZE);			
			%>
			
			<liferay-ui:search-container-column-text href="<%= editPageTemplate.toString() %>" name="manage-page-template-view-name" value="<%= pageTemplate.getName() %>" />				
			<liferay-ui:search-container-column-text href="<%= editPageTemplate.toString() %>" name="manage-page-template-view-description" value="<%=description %>" />
			<liferay-ui:search-container-column-text href="<%= editPageTemplate.toString() %>" name="manage-page-template-view-type" value="<%= pageTemplate.getType() %>" />
			
			<liferay-ui:search-container-column-text href="<%= editPageTemplate.toString() %>" name="manage-page-template-view-image" >
				<c:if test="<%= pageTemplate.getImageId() != 0 && pageTemplate.getImageId() != -1 %>">
					<img border="0" src="<%= themeDisplay.getPathImage() + "/journal/template?img_id=" + pageTemplate.getImageId() + "&t=" + ImageServletTokenUtil.getToken(pageTemplate.getImageId()) %>" />
				</c:if>
			</liferay-ui:search-container-column-text>
			
			<liferay-ui:search-container-column-text href="<%= editPageTemplate.toString() %>" name="manage-page-template-view-default" value="<%= String.valueOf(pageTemplate.getDefaultTemplate()) %>" />
			
			<liferay-ui:search-container-column-jsp align="right" path="/html/manage-page-template-portlet/edit_actions.jsp" />
		</liferay-ui:search-container-row>
		
		<liferay-ui:search-iterator />
	</liferay-ui:search-container>
</aui:form>

<aui:script>
	
	Liferay.provide(
		window,
		'<portlet:namespace />deletePageTemplates',
		function() {
			if (confirm('<%= UnicodeLanguageUtil.get(pageContext, "manage-page-template-are-you-sure-you-want-to-delete-the-selected-page-templates") %>')) {
				document.<portlet:namespace />fm.submit();	
			}
		},
		['aui-base']
	);

</aui:script>


</c:otherwise>

</c:choose>
