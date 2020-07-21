<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@include file="init.jsp"%>

<%@page import="com.protecmedia.iter.services.model.Service"%>

<%@page import="com.protecmedia.iter.services.service.ServiceLocalServiceUtil"%><jsp:useBean id="addServiceURL" class="java.lang.String" scope="request" />

<liferay-ui:success key="service-added"	message="service-added-successfully" />
<liferay-ui:success key="service-updated" message="service-updated-successfully" />
<liferay-ui:success key="service-deleted" message="service-deleted-successfully" />

<liferay-ui:tabs
	names="services"	 
/>

<c:choose>

	<c:when test="<%=scopeGroupId == globalGroupId%>">
		<liferay-ui:message key="service-portlet-not-available-in-global-environment" />
	</c:when>
	
	<c:otherwise>

		<c:if test='<%= (Boolean)request.getAttribute("hasAddPermission") %>'>
			<input type="button" name="addServiceButton" value="<liferay-ui:message key="service-back-end-view-button-add-service" />" onClick="self.location = '<%=addServiceURL%>';">
		</c:if>
		
		<br />
		<br />
		
		<liferay-ui:search-container emptyResultsMessage="serviceEmptyResultsMessage">
			<liferay-ui:search-container-results>
				<%			
					results = ServiceLocalServiceUtil.getServices(scopeGroupId, searchContainer.getStart(), searchContainer.getEnd());
					total = ServiceLocalServiceUtil.getServicesCount(scopeGroupId);;
					pageContext.setAttribute("results", results);
					pageContext.setAttribute("total", total);
				%>
			</liferay-ui:search-container-results>
		
			<liferay-ui:search-container-row className="com.protecmedia.iter.services.model.Service" keyProperty="id" modelVar="service">
				<liferay-ui:search-container-column-text name="service-back-end-edit-title"	value="<%= service.getTitle() %>" />
				<%
					Layout layoutService = null;
					try {
						layoutService = LayoutLocalServiceUtil.getLayout(service.getLinkId());
					} catch (Exception e) {
						;
					}
					String layoutName = "service-layout-link-have-been-deleted";
					if (layoutService != null) {
						layoutName = layoutService.getName(locale);
					}
				%>
				<liferay-ui:search-container-column-text name="service-back-end-edit-url" value="<%= layoutName %>" />
				<c:if test="<%= service.getImageId() != -1 %>">
					<liferay-ui:search-container-column-text name="service-back-end-edit-image">
						<img src="/image/journal/article?img_id=<%= service.getImageId()  %>&r=<%= Math.random() %>" width="50px" height="50px"/>
					</liferay-ui:search-container-column-text>
				</c:if>			
				<liferay-ui:search-container-column-jsp align="right" path="/html/service-portlet/edit_actions.jsp" />
			</liferay-ui:search-container-row>
			<liferay-ui:search-iterator />
		
		</liferay-ui:search-container>
	</c:otherwise>
</c:choose>
