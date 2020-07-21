<%/**
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
*/%>
<%@ include file="init.jsp"%>

<%
	String currentTab = (String) renderRequest.getParameter("tabs1");
	long channelControlId = Long.valueOf(renderRequest.getParameter("channelControlId"));
%>

<portlet:renderURL var="cancelChannelControlLogURL" >
	<portlet:param name="tabs1" value="<%= currentTab %>" />
	<portlet:param name="view" value="channelControl" />
</portlet:renderURL>

<liferay-ui:tabs names="<%= tabName %>" backURL="<%= cancelChannelControlLogURL %>" />

<aui:panel label="xmlio-view-live-control-log" collapsible="true">
	
	<%
	   	PortletURL channelControlLogIteratorURL = renderResponse.createRenderURL();
		channelControlLogIteratorURL.setParameter( "tabs1", currentTab );
		channelControlLogIteratorURL.setParameter( "view", "channelControlLog" );
		channelControlLogIteratorURL.setParameter( "channelControlId", String.valueOf(channelControlId) );
	   	pageContext.setAttribute("channelControlLogIteratorURL", channelControlLogIteratorURL);
	   	List<ChannelControlLog> tempResultsCCL = new ArrayList<ChannelControlLog>(ChannelControlLogLocalServiceUtil.getOperationLog( channelControlId ));
	   	
		//Ordenar la lista
		String orderByCol = ParamUtil.getString(renderRequest, "orderByCol", "classNameValue");
		String orderByType = ParamUtil.getString(renderRequest, "orderByType", "desc");
		
		boolean orderByAsc = false;
		
		if (orderByType.equals("asc")) {
			orderByAsc = true;
		}
		
	%>
	
	<liferay-ui:search-container iteratorURL="${channelControlLogIteratorURL}" emptyResultsMessage="xmlio-view-empty-result" 
		orderByCol="<%= orderByCol %>" orderByType="<%= orderByType %>" >
		<liferay-ui:search-container-results >
			<%  
				LiveListUtil.orderList( tempResultsCCL, orderByCol, orderByAsc );
				
				results = ListUtil.subList(tempResultsCCL, searchContainer.getStart(), searchContainer.getEnd());	
				total = tempResultsCCL.size();							
				pageContext.setAttribute("results", results);
				pageContext.setAttribute("total", total);
			%>
		</liferay-ui:search-container-results>
		
		<liferay-ui:search-container-row className="com.protecmedia.iter.xmlio.model.ChannelControlLog" keyProperty="id" modelVar="channelControlLog">
		
			<% 
				String operation = channelControlLog.getOperation();
				String globalid = channelControlLog.getGlobalId();
				String groupName = "";
				String classname = LanguageUtil.get(pageContext, channelControlLog.getClassNameValue());
				
				try{
					if (channelControlLog.getGroupId() == company.getGroup().getGroupId()){
						groupName = "Global";
					}
					else{
						groupName = GroupLocalServiceUtil.getGroup(channelControlLog.getGroupId()).getName();
					}
				}catch(Exception err){groupName="Unknown";}
			%>

			<liferay-ui:search-container-column-text orderable="true" orderableProperty="operation" name="xmlio-live-status-operation" value="<%= operation %>" />
			<liferay-ui:search-container-column-text orderable="true" orderableProperty="globalId" name="xmlio-live-status-globalid" value="<%= globalid %>" />
			<liferay-ui:search-container-column-text name="xmlio-live-status-groupid" value="<%= groupName %>" />
			<liferay-ui:search-container-column-text orderable="true" orderableProperty="classNameValue" name="xmlio-live-status-classname" value="<%= classname %>" />
			<liferay-ui:search-container-column-jsp align="right" path="/html/xmlio-portlet/view_channel_control_log_actions.jsp" />
		</liferay-ui:search-container-row>
		
		<liferay-ui:search-iterator />
	</liferay-ui:search-container>	
	
	
</aui:panel>

<aui:script use="aui-base">

	Liferay.provide( window, '<portlet:namespace />showControlErrorLog',
		function(errorLog){
		     alert(errorLog);
		});
</aui:script>