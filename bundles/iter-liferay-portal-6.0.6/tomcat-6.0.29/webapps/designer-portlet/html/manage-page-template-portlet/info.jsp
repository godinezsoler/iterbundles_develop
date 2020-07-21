<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@include file="init.jsp"%>

<%@page import="com.liferay.portal.model.LayoutTypePortlet"%>
<%@page import="com.liferay.portal.model.Portlet"%>
<%@page import="com.liferay.portal.model.Theme"%>
<%@page import="com.liferay.portal.model.ColorScheme"%>
<%@page import="com.liferay.portal.model.LayoutTemplate"%>

<jsp:useBean id="pageTemplate" type="com.protecmedia.iter.designer.model.PageTemplate" scope="request" />

<portlet:renderURL var="backURL" />

<%	
	String redirect = "javascript:location.href='" + backURL + "'";
%>

<c:if test="<%= pageTemplate != null %>">
	
	
	<%
		Layout template = LayoutLocalServiceUtil.getLayout(pageTemplate.getLayoutId());
		LayoutTypePortlet ltp = (LayoutTypePortlet) template.getLayoutType();
		
		Theme selTheme = template.getTheme();
		ColorScheme curColorScheme = template.getColorScheme();
		
		LayoutTemplate layoutTemplate = ltp.getLayoutTemplate();
	%>

<div class="page-template-info">

	<liferay-ui:header
		backURL="<%= backURL %>"
		title='<%= template.getName(locale) %>'
	/>
	
	<aui:fieldset label="">		
		<h2><span><liferay-ui:message key="manage-page-template-info-type" />:</span> <%= pageTemplate.getType() %></h2>		
	</aui:fieldset>
	
	<aui:fieldset label="">		
		<h2><span><liferay-ui:message key="manage-page-template-info-theme" />:</span> <%= selTheme.getName() %></h2>
		<img alt="<%= selTheme.getName() %>" class="theme-screenshot" src="<%= selTheme.getContextPath() %><%= selTheme.getImagesPath() %>/thumbnail.png" title="<%= selTheme.getName() %>" />
	</aui:fieldset>
	
	<aui:fieldset label="">
		<h2><span><liferay-ui:message key="manage-page-template-info-colorscheme" />:</span> <%= curColorScheme.getName() %></h2>
		<img alt="<%= curColorScheme.getName() %>" class="theme-thumbnail" src="<%= selTheme.getContextPath() %><%= curColorScheme.getColorSchemeThumbnailPath() %>/thumbnail.png" title="<%= curColorScheme.getName() %>" />
	</aui:fieldset>
	
	<aui:fieldset label="">
		<h2><span><liferay-ui:message key="manage-page-template-info-layout" />:</span> <%= layoutTemplate.getName() %></h2>
		<img src="<%= layoutTemplate.getContextPath() %><%= layoutTemplate.getThumbnailPath() %>">
	</aui:fieldset>
	
	<aui:fieldset label="">
		<h2><span><liferay-ui:message key="manage-page-template-info-number-of-column" />:</span> <%= ltp.getLayoutTemplate().getColumns().size() %></h2>
	</aui:fieldset>
	
	<%		
		for (String column : ltp.getLayoutTemplate().getColumns()) {			
	%>
			<aui:fieldset label="">
				<h3><span><liferay-ui:message key="manage-page-template-info-column" />:</span> <%= column %></h3>
			
				<ul>
	<%	
				for (Portlet portlet: ltp.getAllPortlets(column)) {
	%>				
					<li><%= portlet.getDisplayName() %> - <%= portlet.getInstanceId() %></li>				
	<%
				}
	%>
				</ul>
			</aui:fieldset>	
	<%
		}
	%>
</div>			
	
	
</c:if>

<aui:button-row>
	<aui:button onClick="<%= redirect %>" type="cancel" value="manage-page-template-info-back" />
</aui:button-row>
