<%/**
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
*/%>
<%@ include file="init.jsp"%>

<liferay-ui:success key="xmlio-channel-control-clear-history-success" message="xmlio-channel-control-clear-history-success" />
<liferay-ui:error key="xmlio-channel-control-clear-history-error" message="xmlio-channel-control-clear-history-error" />
<liferay-ui:success key="xmlio-channel-control-deleted" message="xmlio-channel-control-deleted" />
<liferay-ui:error key="xmlio-channel-control-id-delete-error" message="xmlio-channel-control-id-delete-error" />
<liferay-ui:error key="xmlio-channel-control-id-not-exist" message="xmlio-channel-control-id-not-exist" />

<%
	String currentTab = (String) renderRequest.getParameter("tabs1");
%>

<portlet:renderURL var="cancelChannelControlURL" >
	<portlet:param name="tabs1" value="<%= currentTab %>" />
</portlet:renderURL>

<liferay-ui:tabs names="<%= tabName %>" backURL="<%= cancelChannelControlURL %>" />

<aui:panel label="xmlio-view-live-control-status" collapsible="true">
	<%
	   PortletURL channelControlIteratorURL = renderResponse.createRenderURL();
	   channelControlIteratorURL.setParameter("tabs1", currentTab);
	   channelControlIteratorURL.setParameter("view", "channelControl");
	   pageContext.setAttribute("channelControlIteratorURL", channelControlIteratorURL);
	   List<ChannelControl> tempResultsCC = new ArrayList<ChannelControl>(ChannelControlLocalServiceUtil.getChannelControlByType(currentTab));
	   
	   //Ordenar la lista
	   String orderByCol = ParamUtil.getString(renderRequest, "orderByCol", "startDate");
	   String orderByType = ParamUtil.getString(renderRequest, "orderByType", "desc");
	   
	   boolean orderByAsc = false;

		if (orderByType.equals("asc")) {
			orderByAsc = true;
		}
		
		LiveListUtil.orderList( tempResultsCC, orderByCol, orderByAsc );
	%>			
	<liferay-ui:search-container iteratorURL="${channelControlIteratorURL}" emptyResultsMessage="xmlio-view-empty-result" 
		orderByCol="<%= orderByCol %>" orderByType="<%= orderByType %>" >
		<liferay-ui:search-container-results >
			<%
				results = ListUtil.subList(tempResultsCC, searchContainer.getStart(), searchContainer.getEnd());	
				total = tempResultsCC.size();							
				pageContext.setAttribute("results", results);
				pageContext.setAttribute("total", total);
			%>
		</liferay-ui:search-container-results>
		
		<liferay-ui:search-container-row className="com.protecmedia.iter.xmlio.model.ChannelControl" keyProperty="id" modelVar="channelControl">
		
			<% 
				String status = channelControl.getStatus();
				String startDate = "";
				String endDate = "";
				String fileSize = "";
				String operation = "";
				String operations = "";
				String errors = "";
				String userName = "";
				String groupName = "";
				try{startDate = channelControl.getStartDate().toString();}catch(Exception err){}
				try{endDate = channelControl.getEndDate().toString();}catch(Exception err){endDate ="-";}
				try{fileSize = String.valueOf(channelControl.getFileSize()/1024);}catch(Exception err){}
				try{operations = channelControl.getOperations()!=-1?String.valueOf(channelControl.getOperations()):"-";}catch(Exception err){}
				try{errors = channelControl.getErrors()!=-1?String.valueOf(channelControl.getErrors()):"-";}catch(Exception err){}
				try{userName = UserLocalServiceUtil.getUserById(channelControl.getUserId()).getScreenName();}catch(Exception err){}
				try{
					if (channelControl.getGroupId() == company.getGroup().getGroupId()){
						groupName = "Global";
					}
					else{
						groupName = GroupLocalServiceUtil.getGroup(channelControl.getGroupId()).getName();
					}
				}catch(Exception err){groupName="Unknown";}
				if (channelControl.getOperation().equals(IterKeys.XMLIO_XML_IMPORT_OPERATION)){
					operation = LanguageUtil.get(pageContext, "xmlio-channel-input" );
				}
				else{
					operation = LanguageUtil.get(pageContext, "xmlio-channel-output" );
				}
			%>

			<liferay-ui:search-container-column-text orderable="true" orderableProperty="groupId" name="xmlio-live-control-status-Group" value="<%= groupName %>" />
			<liferay-ui:search-container-column-text orderable="true" orderableProperty="operation" name="xmlio-live-control-status-operation" value="<%= operation %>" />
			<liferay-ui:search-container-column-text orderable="true" orderableProperty="status" name="xmlio-live-control-status-status" value="<%= status %>" />	
			<liferay-ui:search-container-column-text orderable="true" orderableProperty="startDate" name="xmlio-live-control-status-startdate" value="<%= startDate %>" />
			<liferay-ui:search-container-column-text orderable="true" orderableProperty="endDate" name="xmlio-live-control-status-enddate" value="<%= endDate %>" />	
			<liferay-ui:search-container-column-text name="xmlio-live-control-status-operations" value="<%= operations %>" />
			<liferay-ui:search-container-column-text name="xmlio-live-control-status-errors" value="<%= errors %>" />
			<liferay-ui:search-container-column-text name="xmlio-live-control-status-user-name" value="<%= userName %>" />
			<liferay-ui:search-container-column-jsp align="right" path="/html/xmlio-portlet/view_channel_control_actions.jsp" />
		</liferay-ui:search-container-row>
		
		<liferay-ui:search-iterator />
	</liferay-ui:search-container>	
	
	<!-- Limpiar historial -->
	<portlet:actionURL name="clearChannelControl" var="updateURL" >
		<portlet:param name="tabs1" value="<%= currentTab %>" />
		<portlet:param name="view" value="channelControl" />
	</portlet:actionURL>	
		
	<aui:button-row>				
		<aui:button onClick="<%= updateURL %>" type="submit" value="xmlio-channel-control-clear" />
	</aui:button-row>
	
</aui:panel>

<aui:script use="aui-base">

	Liferay.provide( window, '<portlet:namespace />showControlErrorLog',
		function(errorLog){
		     regExp = /;/g;
		     alert(errorLog.replace(regExp,"\n"));
		});
</aui:script>