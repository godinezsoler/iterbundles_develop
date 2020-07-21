<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@ include file="init.jsp"%>

<%@page import="com.liferay.portal.kernel.dao.search.RowChecker"%>
<%@page import="com.protecmedia.iter.xmlio.service.ChannelLocalServiceUtil"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.model.Group"%>
<%@page import="com.liferay.portal.service.GroupServiceUtil"%>
<%@page import="com.liferay.portal.security.permission.ActionKeys"%>
<%@page import="com.liferay.portal.service.OrganizationServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.HtmlUtil"%>
<%@page import="com.liferay.portal.model.Organization"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.kernel.util.HttpUtil"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="javax.portlet.PortletURL"%>


<portlet:renderURL var="cancelLiveItemURL" >
	<portlet:param name="tabs1" value="live" />
</portlet:renderURL>

<%
	//Recuperar el resultado de la lista inicial
	long liveItemId = ParamUtil.getLong(renderRequest, "liveId", -1);
	Live liveItem = LiveLocalServiceUtil.getLive(liveItemId);

	//Obtener la lista de registros miembros del pool
	List<Live> tempResults = LiveLocalServiceUtil.getPoolMemberList(liveItem);
	
	//Ordenar la lista
	String orderByCol = ParamUtil.getString(renderRequest, "orderByCol", "modifiedDate");
	String orderByType = ParamUtil.getString(renderRequest, "orderByType", "desc");
	boolean orderByAsc = false;
	if (orderByType.equals("asc")) {
		orderByAsc = true;
	}
	//LiveListUtil.orderList( tempResults, orderByCol, orderByAsc );
	
	//Renderizar la lista
%>

<liferay-ui:tabs
names="<%=liveItem.getGlobalId() %>" 
	backURL="<%= cancelLiveItemURL %>" 
/>

<aui:panel label="xmlio-view-live-status" collapsible="true">
		<%
	   PortletURL liveIteratorURL = renderResponse.createRenderURL();
	   liveIteratorURL.setParameter("liveId", Long.toString(liveItemId));
	   liveIteratorURL.setParameter("tabs1", "live");
	   liveIteratorURL.setParameter("view", "liveItem");
	   pageContext.setAttribute("liveIteratorURL", liveIteratorURL);
	%>			
	<liferay-ui:search-container iteratorURL="${liveIteratorURL}" emptyResultsMessage="xmlio-live-view-empty-result" 
		orderByCol="<%= orderByCol %>" orderByType="<%= orderByType %>" >
		<liferay-ui:search-container-results >
			<%  
				results = ListUtil.subList(tempResults, searchContainer.getStart(), searchContainer.getEnd());
				total = tempResults.size();							
				pageContext.setAttribute("results", results);
				pageContext.setAttribute("total", total);
			%>
		</liferay-ui:search-container-results>
		
		<liferay-ui:search-container-row className="com.protecmedia.iter.xmlio.model.Live" keyProperty="id" modelVar="live">
		
			<% 
				String groupName = "";
				String className = "";
				String modifiedDate = "";
				String performDate = "";
				try{
					if (live.getGroupId() == company.getGroup().getGroupId()){
						groupName = "Global";
					}
					else{
						groupName = GroupLocalServiceUtil.getGroup(live.getGroupId()).getName();
					}
				}catch(Exception err){
					try{
						groupName = String.valueOf(liveGroupId);
					}catch(Exception err1){
						groupName = "Unknown";
					}
				}
				
				try{
					className = LanguageUtil.get(pageContext, live.getClassNameValue());
			
					if (className.equals("") || className.equals(live.getClassNameValue())){
						className = live.getClassNameValue().substring(live.getClassNameValue().lastIndexOf(".")+1);
					}
				}catch(Exception err2){
					try{
						groupName = String.valueOf(liveGroupId);
					}catch(Exception err1){
						groupName = "Unknown";
					}
				}
				
				try{modifiedDate = live.getModifiedDate().toString();}catch(Exception err){}	
			%>

			<liferay-ui:search-container-column-text orderable="true" orderableProperty="groupId" 		name="xmlio-live-status-groupid" 		value="<%= groupName %>" />	
			<liferay-ui:search-container-column-text orderable="true" orderableProperty="classNameValue"	name="xmlio-live-status-classname" 		value="<%= className %>" />
			<liferay-ui:search-container-column-text orderable="true" orderableProperty="globalId" 			name="xmlio-live-status-globalid" 		value="<%= live.getGlobalId() %>" />	
			<liferay-ui:search-container-column-text orderable="true" orderableProperty="localId" 			name="xmlio-live-status-localid" 		value="<%= live.getLocalId() %>" />	
			<liferay-ui:search-container-column-text orderable="true" orderableProperty="operation" 		name="xmlio-live-status-operation" 		value="<%= live.getOperation() %>" />	
			<liferay-ui:search-container-column-text orderable="true" orderableProperty="status" 			name="xmlio-live-status-status" 		value="<%= live.getStatus() %>" />	
			<liferay-ui:search-container-column-text orderable="true" orderableProperty="modifiedDate"		name="xmlio-live-status-modifieddate"	value="<%= modifiedDate %>" />
			<liferay-ui:search-container-column-jsp align="right" path="/html/xmlio-portlet/view_live_detail_actions.jsp" />	
		</liferay-ui:search-container-row>
		
		<liferay-ui:search-iterator />
	</liferay-ui:search-container>	
</aui:panel>
<aui:panel label="xmlio-view-live-publish" collapsible="true">
	<portlet:actionURL name="publishToLive" var="updateURL" >
		<portlet:param name="tabs1" value="live" />
		<portlet:param name="companyId" value="<%=String.valueOf(companyId) %>" />
		<portlet:param name="groupId" value="<%= String.valueOf(liveGroupId) %>" />
		<portlet:param name="userId" value="<%= String.valueOf(userId) %>" />
		<portlet:param name="resourcePrimKey" value="<%= Long.toString(liveItemId) %>" />
		<portlet:param name="liveRangeType" value="<%=String.valueOf(liveItem.getClassNameValue()) %>" />	
	</portlet:actionURL>
	<aui:form name="fm" method="post" action="<%= updateURL %>">											
		<aui:button-row>								
			<aui:button type="submit" value="xmlio-view-live-publish-to-live"/>
		</aui:button-row>
	</aui:form>
</aui:panel>


<aui:script use="aui-base">

	Liferay.provide( window, '<portlet:namespace />showErrorLog',
		function(errorLog){
			regExp = /;/g;
		    alert(errorLog.replace(regExp,"\n"));
		});
	
</aui:script>

