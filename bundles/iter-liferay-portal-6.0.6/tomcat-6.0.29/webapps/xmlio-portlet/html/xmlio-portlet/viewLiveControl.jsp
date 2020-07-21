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

<portlet:renderURL var="cancelLiveControlURL" >
	<portlet:param name="tabs1" value="live" />
</portlet:renderURL>

<liferay-ui:success key="xmlio-live-control-clear-success" message="xmlio-live-control-clear-success" />
<liferay-ui:error key="xmlio-live-control-clear-error" message="xmlio-live-control-clear-error" />

<liferay-ui:tabs
names="live-control" 
	backURL="<%= cancelLiveControlURL %>" 
/>

<aui:panel label="xmlio-view-live-control-status" collapsible="true">
	<%
	   PortletURL liveControlIteratorURL = renderResponse.createRenderURL();
	   liveControlIteratorURL.setParameter("tabs1", "live");
	   liveControlIteratorURL.setParameter("view", "liveControl");
	   pageContext.setAttribute("liveControlIteratorURL", liveControlIteratorURL);
	   List<LiveControl> tempResultsLC = LiveControlLocalServiceUtil.getAllLiveControl();
	   
	   //Ordenar la lista
	   String orderByCol = ParamUtil.getString(renderRequest, "orderByCol", "startDate");
	   String orderByType = ParamUtil.getString(renderRequest, "orderByType", "desc");
	   
	   boolean orderByAsc = false;

		if (orderByType.equals("asc")) {
			orderByAsc = true;
		}
		
		LiveListUtil.orderList( tempResultsLC, orderByCol, orderByAsc );
	%>
	
	<liferay-ui:search-container iteratorURL="${liveControlIteratorURL}" emptyResultsMessage="xmlio-live-control-view-empty-result" 
		orderByCol="<%= orderByCol %>" orderByType="<%= orderByType %>" >
		<liferay-ui:search-container-results >
			<%  
				results = ListUtil.subList(tempResultsLC, searchContainer.getStart(), searchContainer.getEnd());	
				total = tempResultsLC.size();							
				pageContext.setAttribute("results", results);
				pageContext.setAttribute("total", total);
			%>
		</liferay-ui:search-container-results>
		
		<liferay-ui:search-container-row className="com.protecmedia.iter.xmlio.model.LiveControl" keyProperty="id" modelVar="liveControl">
		
			<% 
				String status = liveControl.getStatus();
				String startDate = "";
				String endDate = "";
				String fileSize = "";
				String operations = "";
				String errors = "";
				String userName = "";
				String groupName = "";
				String pId = liveControl.getProcessId()!="" ? liveControl.getProcessId() : "-";
				String type = liveControl.getType()!="" ? liveControl.getType() : "-";
				try{startDate = liveControl.getStartDate().toString();}catch(Exception err){}
				try{endDate = liveControl.getEndDate().toString();}catch(Exception err){}
				try{fileSize = String.valueOf(liveControl.getFileSize()/1024);}catch(Exception err){}
				try{operations = String.valueOf(liveControl.getOperations());}catch(Exception err){}
				try{errors = String.valueOf(liveControl.getErrors());}catch(Exception err){}
				try{userName = UserLocalServiceUtil.getUserById(liveControl.getUserId()).getScreenName();}catch(Exception err){}
				try{
					if (liveControl.getGroupId() == company.getGroup().getGroupId()){
						groupName = "Global";
					}
					else{
						groupName = GroupLocalServiceUtil.getGroup(liveControl.getGroupId()).getName();
					}
				}catch(Exception err){groupName="Unknown";}
			%>

			<liferay-ui:search-container-column-text 												name="xmlio-live-control-status-Group" 		value="<%= groupName %>" />
			<liferay-ui:search-container-column-text 												name="xmlio-live-control-status-process-id" value="<%= pId %>" />
			<liferay-ui:search-container-column-text 												name="xmlio-live-control-status-type" 		value="<%= type %>" />
			<liferay-ui:search-container-column-text 												name="xmlio-live-control-status-status" 	value="<%= status %>" />	
			<liferay-ui:search-container-column-text orderable="true" orderableProperty="startDate"	name="xmlio-live-control-status-startdate" 	value="<%= startDate %>" />
			<liferay-ui:search-container-column-text orderable="true" orderableProperty="endDate"	name="xmlio-live-control-status-enddate" 	value="<%= endDate %>" />	
			<liferay-ui:search-container-column-text 												name="xmlio-live-control-status-filesize" 	value="<%= fileSize %>" />	
			<liferay-ui:search-container-column-text 												name="xmlio-live-control-status-operations" value="<%= operations %>" />
			<liferay-ui:search-container-column-text 												name="xmlio-live-control-status-errors" 	value="<%= errors %>" />
			<liferay-ui:search-container-column-text 												name="xmlio-live-control-status-user-name" 	value="<%= userName %>" />
			<liferay-ui:search-container-column-jsp align="right" path="/html/xmlio-portlet/view_live_control_actions.jsp" />
		</liferay-ui:search-container-row>
		
		<liferay-ui:search-iterator />
	</liferay-ui:search-container>	
	
	<%-- Limpiar publicaciones --%>
	<portlet:actionURL name="clearLiveControl" var="updateURL" >
		<portlet:param name="tabs1" value="live" />
		<portlet:param name="view" value="liveControl" />
	</portlet:actionURL>	

	<%-- Desbloquear publicación en curso --%>
	<portlet:actionURL name="unlockLiveControl" var="unlockURL" >
		<portlet:param name="tabs1" value="live" />
		<portlet:param name="view" value="liveControl" />
		<portlet:param name="processId" value="all" />
	</portlet:actionURL>	
		
	<aui:button-row>				
		<aui:button onClick="<%= updateURL %>" type="submit" value="xmlio-live-control-clear" />
		<aui:button onClick="<%= unlockURL %>" type="submit" value="xmlio-live-control-unlock" />
	</aui:button-row>
	
</aui:panel>

<aui:script use="aui-base">

	Liferay.provide( window, '<portlet:namespace />showControlErrorLog',
		function(errorLog){
		     alert(errorLog);
		});
</aui:script>