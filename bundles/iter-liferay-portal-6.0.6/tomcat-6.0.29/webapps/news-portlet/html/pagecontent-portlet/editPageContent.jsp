<%@page import="com.protecmedia.iter.news.service.util.MyDynamicQueryUtil"%>
<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@ include file="init.jsp" %>

<jsp:useBean id="pageContent" type="com.protecmedia.iter.news.model.PageContent" scope="request" />
<jsp:useBean class="java.lang.String" id="editURL" scope="request" />

<portlet:renderURL var="cancelPageContentURL" >
	<portlet:param name="layout-id" value="<%= String.valueOf(layoutId) %>" />
</portlet:renderURL>

<liferay-portlet:renderURL varImpl="iteratorURL">
	<liferay-portlet:param name="view" value="editPageContent" />
	<liferay-portlet:param name="editType" value="add" />
	<liferay-portlet:param name="layout-id" value="<%= String.valueOf(layoutId) %>" />
	<liferay-portlet:param name="standardArticleCheck" value="<%= String.valueOf(standardArticleCheck) %>" />
	<liferay-portlet:param name="standardGalleryCheck" value="<%= String.valueOf(standardGalleryCheck) %>" />
	<liferay-portlet:param name="standardPollCheck" value="<%= String.valueOf(standardPollCheck) %>" />
	<liferay-portlet:param name="standardMultimediaCheck" value="<%= String.valueOf(standardMultimediaCheck) %>" />
	<liferay-portlet:param name="keyword" value="<%= keyword %>" />
	<liferay-portlet:param name="contentGroupId" value="<%= String.valueOf(contentGroupId) %>" />
</liferay-portlet:renderURL>

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
		expirationDate.add(Calendar.MONTH, 1);		
	}
	
	List<JournalArticle> contents = null;
	if (standardArticleCheck || standardPollCheck || standardGalleryCheck || standardMultimediaCheck) {
		if (standardArticleCheck) {
			structures.add(IterKeys.STRUCTURE_ARTICLE);
		}
		
		if (standardPollCheck) {
			structures.add(IterKeys.STRUCTURE_POLL);
		}
		
		if (standardGalleryCheck) {
			structures.add(IterKeys.STRUCTURE_GALLERY);
		}
		
		if (standardMultimediaCheck) {
			structures.add(IterKeys.STRUCTURE_MULTIMEDIA);
		}
	
		contents = MyDynamicQueryUtil.getContents(contentGroupId, structures, keyword, orderByCol, orderByType);							
	} else {
		contents = new ArrayList<JournalArticle>();
	}
%>

<div class="page-content-edit-portlet">
	
	<liferay-ui:tabs
	names="<%= LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(layoutId, scopeGroupId).getName(locale) %>" 
		backURL="<%= cancelPageContentURL %>" 
	/>
	
	<liferay-ui:error key="page-content-already-exist" message="page-content-already-exist" />
	<liferay-ui:error key="page-content-journal-id-required" message="page-content-journal-id-required" />
	<liferay-ui:error key="page-content-template-id-required" message="page-content-template-id-required" />
	<liferay-ui:error key="page" message="page-content-error-date" />
	<liferay-ui:error key="page-content-article-model-id-required" 	message="page-content-article-model-id-required" />
	<liferay-ui:error key="page-content-qualification-id-required" 	message="page-content-qualification-id-required" />
	<liferay-ui:error key="page-content-type-content-required" 		message="page-content-type-content-required" />
	
	
	<%-- Filter by structure --%>
		
	<%	
		PortletURL addPageContentURL = renderResponse.createRenderURL();
		addPageContentURL.setParameter("view", "editPageContent");
		addPageContentURL.setParameter("editType", "add");
		addPageContentURL.setParameter("layout-id", String.valueOf(layoutId));
		renderRequest.setAttribute("addPageContentURL", addPageContentURL.toString());		
	%>
	
	<aui:fieldset label="page-content-edit-filter-contents">
		<aui:form name="fm1" method="post" action="<%= addPageContentURL %>">		
			<aui:input type="hidden" name="action" value="filter"/>
			
			<aui:input type="hidden" name="contentGroupId" id="contentGroupIdFm1" value="<%= contentGroupId %>"/>
				
			<aui:input type="text" name="keyword" label="page-content-edit-search" value="<%= keyword %>" />
				
			<aui:input inlineLabel="left" name="standardArticleCheck" label="page-content-edit-standard-article" type="checkbox" value="<%= standardArticleCheck %>" />		
			<aui:input inlineLabel="left" name="standardGalleryCheck" label="page-content-edit-standard-gallery" type="checkbox" value="<%= standardGalleryCheck %>" />
			<aui:input inlineLabel="left" name="standardPollCheck" label="page-content-edit-standard-poll" type="checkbox" value="<%= standardPollCheck %>" />
			<aui:input inlineLabel="left" name="standardMultimediaCheck" label="page-content-edit-standard-multimedia" type="checkbox" value="<%= standardMultimediaCheck %>" />
	
			<aui:button-row>
				<aui:button type="submit" value="page-content-edit-filter" />
			</aui:button-row>
		</aui:form>
	</aui:fieldset>
	
	<br />
	
	<aui:form name="fm" method="post" action="<%= editURL %>">
		<aui:input name="layoutId" type="hidden" value="<%= layoutId %>" />
			
		<aui:model-context bean="<%= pageContent %>" model="<%= PageContent.class %>" />
		
		<aui:fieldset label="page-content-edit-display-settings">
	
			<%--  Calificación --%>
			<aui:select name="qualificationId" label="page-content-edit-qualification">
				<%
					for (Qualification qualification : qualifications) {

				%>
						<aui:option selected="<%= qualification.getQualifId() == pageContent.getQualificationId()  %>" 
									value="<%= qualification.getId() %>"><%=qualification.getName()%></aui:option>
				<%
					}
				%>
			</aui:select>
			
			<%--  Template Model --%>
			<aui:select name="articleModelId" label="page-content-edit-article-model">				
				<%
					List<PageTemplate> pageTemplates = PageTemplateLocalServiceUtil.getPageTemplatesByType(scopeGroupId, "article-template");
					for (PageTemplate pageTemplate : pageTemplates) {
				%>
						<aui:option selected="<%= pageTemplate.getId() == pageContent.getArticleModelId() %>" 
									value="<%= pageTemplate.getId() %>"><%= pageTemplate.getName() %></aui:option>
				<%
					}
				%>
			</aui:select>
			
			<%-- Vigencia Desde --%>		
			<aui:input name="vigenciadesde" value="<%= displayDate %>" label="page-content-edit-display-date" />	
			
			<%-- Vigencia Hasta --%>
			<aui:input name="vigenciahasta" value="<%= expirationDate %>" label="page-content-edit-expiration-date" />					
		</aui:fieldset>		
		
		<aui:fieldset label="page-content-edit-contents">	
			<br />
			<aui:select name="contentGroupId" label="page-content-edit-scope">
				<aui:option label="page-content-edit-global-scope" selected="<%= contentGroupId == themeDisplay.getCompanyGroupId() %>" value="<%= themeDisplay.getCompanyGroupId() %>" />
				<aui:option label="page-content-edit-actual-scope" selected="<%= contentGroupId == scopeGroupId %>" value="<%= scopeGroupId %>" />
			</aui:select>	
			
			<%-- ContentId --%>										
			<liferay-ui:search-container orderByCol="<%= orderByCol %>" orderByType="<%= orderByType %>" emptyResultsMessage="page-content-view-empty-result" iteratorURL="<%= iteratorURL %>" rowChecker="<%= new MyContentChecker(renderRequest, renderResponse, layoutId, scopeGroupId) %>" >
				<liferay-ui:search-container-results>
					<%										    
						results = ListUtil.subList(contents, searchContainer.getStart(), searchContainer.getEnd());	
						total = contents.size();							
						pageContext.setAttribute("results", results);
						pageContext.setAttribute("total", total);
					%>
				</liferay-ui:search-container-results>
				
				<liferay-ui:search-container-row className="com.liferay.portlet.journal.model.JournalArticle" keyProperty="articleId" modelVar="articulo" >							
					<liferay-ui:search-container-column-text orderableProperty="title"  orderable="<%= true %>" name="page-content-view-journal-article" value="<%= articulo.getTitle() %>" />
					<liferay-ui:search-container-column-text orderableProperty="structureId" orderable="<%= true %>" name="page-content-view-journal-type" value="<%= articulo.getStructureId() %>" />																								
				</liferay-ui:search-container-row>
					
				<liferay-ui:search-iterator />
			</liferay-ui:search-container>			
			
			<aui:button-row>	
				<aui:button type="submit" />	
				<aui:button onClick="<%= redirect %>" type="cancel" />
			</aui:button-row>
			
		</aui:fieldset>	
		
	</aui:form>
</div>

<aui:script use="aui-base">
	A = AUI();

	A.on('change', selectType, '#<portlet:namespace />contentGroupId');
	function selectType() {										
		document.<portlet:namespace />fm1.<portlet:namespace />contentGroupIdFm1.value = A.one('#<portlet:namespace />contentGroupId').val();
		submitForm(document.<portlet:namespace />fm1);
	}

	

</aui:script>
