<%@page import="com.protecmedia.iter.designer.model.PageTemplate"%>
<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@ include file="init.jsp" %>

<jsp:useBean id="pageContent" type="com.protecmedia.iter.news.model.PageContent" scope="request" />
<jsp:useBean class="java.lang.String" id="editURL" scope="request" />

<portlet:renderURL var="cancelPageContentURL" >
	<portlet:param name="layout-id" value="<%= String.valueOf(layoutId) %>" />
</portlet:renderURL>

<%
 
	String redirect = "javascript:location.href='" + cancelPageContentURL + "'";
		
	Calendar displayDate = Calendar.getInstance();
	if (pageContent.getVigenciadesde() != null) {
		displayDate.setTime(pageContent.getVigenciadesde());
	}

	Calendar expirationDate = Calendar.getInstance();
	if (pageContent.getVigenciahasta() != null) {
		expirationDate.setTime(pageContent.getVigenciahasta());
	} else {
		int anio = expirationDate.get(Calendar.YEAR);
		int mes = expirationDate.get(Calendar.MONTH);
		expirationDate.set((((mes + 1) % 12) == 0) ? anio + 1 : anio, (mes + 1) % 12, expirationDate.get(Calendar.DAY_OF_MONTH));
	}
	
%>

<liferay-ui:tabs
	names="<%= LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(layoutId, scopeGroupId).getName(locale) %>" 
	backURL="<%= redirect %>" 
/>

<liferay-ui:error key="page-content-already-exist" message="page-content-already-exist" />
<liferay-ui:error key="page-content-journal-id-required" message="page-content-journal-id-required" />
<liferay-ui:error key="page-content-template-id-required" message="page-content-template-id-required" />
<liferay-ui:error key="page-content-error-date" message="page-content-error-date" />
<liferay-ui:error key="page-content-article-model-id-required" 	message="page-content-article-model-id-required" />
<liferay-ui:error key="page-content-qualification-id-required" 	message="page-content-qualification-id-required" />
<liferay-ui:error key="page-content-type-content-required" 		message="page-content-type-content-required" />


<aui:form name="fm" method="post" action="<%= editURL %>">
	<aui:input name="resourcePrimKey" type="hidden" value="<%= pageContent.getPrimaryKey() %>" />
	<aui:input name="id" type="hidden" value="<%= pageContent.getContentId() %>" />
	<aui:input name="layoutId" type="hidden" value="<%= layoutId %>" />
	<aui:input name="orden" type="hidden" value="<%= pageContent.getOrden() %>" />
		
	<aui:model-context bean="<%= pageContent %>" model="<%= PageContent.class %>" />
	
	<aui:fieldset label="<%= JournalArticleLocalServiceUtil.getArticle(globalGroupId, pageContent.getContentId()).getTitle() %>">	    	    
	    
		<%--  Calificación --%>
		<aui:select name="qualificationId" label="page-content-edit-qualification">
			<%				
				for (Qualification qualification : qualifications) {					
			%>
					<aui:option selected="<%= qualification.getQualifId().equals(pageContent.getQualificationId())  %>" 
								value="<%= qualification.getQualifId() %>"><%= qualification.getName() %></aui:option>
			<%	
				}
			%>
		</aui:select>
		
		<%--  Template Model --%>
		<aui:select name="articleModelId" label="page-content-edit-article-model">
			<aui:option value=""><liferay-ui:message key="page-content-edit-default" /></aui:option>
			<%
				List<PageTemplate> pageTemplates = PageTemplateLocalServiceUtil.getPageTemplatesByType(scopeGroupId, "article-template");
				for (PageTemplate pageTemplate : pageTemplates) {					
			%>
					<aui:option selected="<%= pageTemplate.getId() == pageContent.getArticleModelId()  %>" 
								value="<%= pageTemplate.getId() %>"><%= pageTemplate.getName() %></aui:option>
			<%
				}
			%>
		</aui:select>
		
		<%-- Vigencia Desde --%>		
		<aui:input name="vigenciadesde" value="<%= displayDate %>" label="page-content-edit-display-date" />	
		
		<%-- Vigencia Hasta --%>
		<aui:input name="vigenciahasta" value="<%= expirationDate %>" label="page-content-edit-expiration-date" />
		
		<aui:button-row>	
			<aui:button type="submit" />	
			<aui:button onClick="<%= redirect %>" type="cancel" />
		</aui:button-row>
		
	</aui:fieldset>	
	
</aui:form>
