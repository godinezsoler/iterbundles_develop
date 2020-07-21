<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@include file="init.jsp"%>

<%@page import="com.liferay.portal.kernel.util.StringBundler"%>
<%@page import="com.liferay.portal.kernel.language.LanguageUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>

<div class="set-page-template">

	<portlet:actionURL var="createURL" >
		<portlet:param name="action" value="add" />
	</portlet:actionURL>
	
	<aui:fieldset label="set-page-template-view-add-new-page-template">
		<aui:form action="<%= createURL %>" method="post" name="form1">
			<aui:input type="text" name="name" label="set-page-template-view-name" />
			<aui:input type="textarea" name="description" label="set-page-template-view-description" maxlength="75"  />
		
			<aui:button-row>
				<aui:button type="submit" value="set-page-template-view-add-new-template" />
			</aui:button-row>
		</aui:form>
	</aui:fieldset>
	
	<br />	
	<div class="separator article-separator"><!-- --></div>

	<portlet:actionURL var="loadURL">	
		<portlet:param name="action" value="load" />
	</portlet:actionURL>
	
	<aui:fieldset label="set-page-template-view-page-templates-list" >
		<br />
		<div class="portlet-msg-info">
			<span class="displaying-help-message-page-template-holder">
				<liferay-ui:message key="set-page-template-please-select-a-page-template" />
			</span>
		
			<span class="displaying-page-template-id-holder aui-helper-hidden">
				<liferay-ui:message key="set-page-template-displaying-page-content" />: <span class="displaying-page-template-id"></span>
			</span>
		</div>
		
		<aui:form name="fm" method="post" action="<%= loadURL %>" cssClass="customlayout-viewer">
			<aui:input name="id" type="hidden" value="" />	
			
			<liferay-ui:search-container emptyResultsMessage="set-page-template-view-empty-result"  >
				<liferay-ui:search-container-results >
					<%
						List<PageTemplate> tempResults = PageTemplateLocalServiceUtil.getPageTemplates(scopeGroupId);
						results = ListUtil.subList(tempResults, searchContainer.getStart(), searchContainer.getEnd());	
						total = tempResults.size();							
						pageContext.setAttribute("results", results);
						pageContext.setAttribute("total", total);
					%>
				</liferay-ui:search-container-results>
			
				<liferay-ui:search-container-row className="com.protecmedia.iter.designer.model.PageTemplate" keyProperty="id" modelVar="pageTemplate">	
				
					<%
						pageTemplate = pageTemplate.toEscapedModel();
								
						StringBundler sb = new StringBundler(7);
									
						sb.append("javascript:");
						sb.append(renderResponse.getNamespace());
						sb.append("selectPageTemplate('");
						sb.append(pageTemplate.getId());
						sb.append("','");
						sb.append(pageTemplate.getName());			
						sb.append("');");
					%>	
					<liferay-ui:search-container-column-text href="<%= sb.toString() %>"  name="set-page-template-view-name" value="<%= pageTemplate.getName() %>" />		
					<liferay-ui:search-container-column-text href="<%= sb.toString() %>"  name="set-page-template-view-type" value="<%= pageTemplate.getType() %>" />
				</liferay-ui:search-container-row>
				
				<liferay-ui:search-iterator />
			</liferay-ui:search-container>		
		
			<aui:button-row>
				<aui:button type="submit" value="set-page-template-view-load-template" />
			</aui:button-row>
		</aui:form>
	</aui:fieldset>
</div>

<aui:script>
	Liferay.provide(
		window,
		'<portlet:namespace />selectPageTemplate',
		function(id, name) {
			var A = AUI();

			document.<portlet:namespace />fm.<portlet:namespace />id.value = id;
			
			A.one('.displaying-page-template-id-holder').show();
			A.one('.displaying-help-message-page-template-holder').hide();					

			var displayPageTemplateId = A.one('.displaying-page-template-id');

			displayPageTemplateId.set('innerHTML', name + ' (<%= LanguageUtil.get(pageContext, "modified") %>)');
			displayPageTemplateId.addClass('modified');
		},
		['aui-base']
	);
</aui:script>
