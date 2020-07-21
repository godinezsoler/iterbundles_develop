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
<%@page import="com.liferay.portal.kernel.language.UnicodeLanguageUtil"%>

<%@page import="javax.portlet.PortletURL"%>

<portlet:renderURL var="portletURL" />	

<liferay-ui:tabs 
	names="manual,automatic,live"
	param="tabs1"
	url="<%= portletURL.toString() %>"	
/>

<liferay-ui:success key="xmlio-live-group-publish-success" message="xmlio-live-group-publish-success" />
<liferay-ui:error key="xmlio-live-group-publish-error" message="xmlio-live-group-publish-error" />
<liferay-ui:error key="xmlio-live-control-publish-already-in-process" message="xmlio-live-control-publish-already-in-process" />

<liferay-ui:error key="xmlio-live-publish-error-client-protocol" message="xmlio-live-publish-error-client-protocol" />
<liferay-ui:error key="xmlio-live-publish-error-encode" message="xmlio-live-publish-error-encode" />
<liferay-ui:error key="xmlio-live-publish-error-send-ftp" message="xmlio-live-publish-error-send-ftp" />
<liferay-ui:error key="xmlio-live-publish-error-remote-port" message="xmlio-live-publish-error-remote-port" />
<liferay-ui:error key="xmlio-live-publish-error-live" message="xmlio-live-publish-error-live" />
<liferay-ui:error key="xmlio-live-publish-error-send-file-sytem" message="xmlio-live-publish-error-send-file-sytem" />
<liferay-ui:error key="xmlio-live-publish-error-remote-log" message="xmlio-live-publish-error-remote-log" />
<liferay-ui:error key="xmlio-live-publish-error-conection" message="xmlio-live-publish-error-conection" />
<liferay-ui:error key="xmlio-live-publish-error-socket-timeout" message="xmlio-live-publish-error-socket-timeout" />
<liferay-ui:error key="xmlio-live-publish-error-no-http-response" message="xmlio-live-publish-error-no-http-response" />
<liferay-ui:error key="xmlio-live-publish-error-connect-timeout" message="xmlio-live-publish-error-connect-timeout" />

<liferay-ui:success key="xmlio-live-populate-live-success" message="xmlio-live-populate-live-success" />
<liferay-ui:error key="xmlio-live-populate-live-error" message="xmlio-live-populate-live-error" />

<liferay-ui:success key="xmlio-live-clean-live-success" message="xmlio-live-clean-live-success" />
<liferay-ui:error key="xmlio-live-clean-live-error" message="xmlio-live-clean-live-error" />

<liferay-ui:error key="xmlio-manual-no-file-error" message="xmlio-manual-no-file-error" />
<liferay-ui:success key="xmlio-manual-import-success" message="xmlio-manual-import-success" />
<liferay-ui:error key="xmlio-manual-import-error" message="xmlio-manual-import-error" />
<liferay-ui:success key="xmlio-manual-export-success" message="xmlio-manual-export-success" />
<liferay-ui:error key="xmlio-manual-export-error" message="xmlio-manual-export-error" />
<liferay-ui:error key="xmlio-manual-operation-in-process" message="xmlio-manual-operation-in-process" />

<jsp:useBean id="channelControlURL" class="java.lang.String" scope="request" />
		
<%
	String viewChannelControlURL = "self.location = '" + channelControlURL + "'";
%>

<c:choose>
	
	<c:when test='<%= scopeGroupId == globalGroupId  %>'>
		<liferay-ui:message key="xmlio-portlet-not-available-in-global-environment" />
	</c:when>
	
	<c:when test='<%= tabs1.equals("manual") %>'>
		<jsp:useBean id="editURL" class="java.lang.String" scope="request" />
	
		<aui:form name="fm" method="post" action="<%= editURL %>">
			<aui:panel label="xmlio-channel-general" collapsible="true">
			
				<aui:fieldset>		
					<label class="aui-field-label"><liferay-ui:message key="xmlio-channel-select-type" /></label>	
					<aui:input name="type" id="typeInput" checked="true" label="xmlio-channel-input" type="radio" inlineField="true" value="<%=IterKeys.XMLIO_CHANNEL_TYPE_INPUT%>" />
					<aui:input name="type" id="typeOutput" label="xmlio-channel-output" type="radio" inlineField="true" value="<%=IterKeys.XMLIO_CHANNEL_TYPE_OUTPUT %>" />
				</aui:fieldset>
				
				<div id="<portlet:namespace />inputDiv">
					<aui:fieldset>
						<aui:input name="source" label="xmlio-channel-file-input" />
					</aui:fieldset>
				</div>
				
				<div class='aui-helper-hidden' id="<portlet:namespace />outputDiv">
					<aui:fieldset>
						<aui:input name="destination" label="xmlio-channel-file-path" />
					</aui:fieldset>
				</div>
			</aui:panel>
			
			<aui:panel label="xmlio-channel-xsl-transformation" collapsible="true">
				<aui:fieldset>
					<aui:input name="xsl" label="xmlio-channel-xsl-file-path" />
				</aui:fieldset>
			</aui:panel>
			
			<div id="<portlet:namespace />rangeDiv"  class="aui-helper-hidden">
				<aui:panel label="xmlio-channel-range" collapsible="true">
					<aui:fieldset>		
						<aui:input name="channelRangeTimeAll" id="rangeTimeAll" checked="true" label="xmlio-channel-range-time-all" type="radio" inlineField="true" value="true" />
						<aui:input name="channelRangeTimeAll" id="rangeTimeSelect" label="xmlio-channel-range-time-select" type="radio" inlineField="true" value="false" />
					</aui:fieldset>
					<div id="<portlet:namespace/>rangeTimeDiv" class="aui-helper-hidden">
						<aui:fieldset>	
							<label class="aui-field-label"><liferay-ui:message key="xmlio-channel-range-time-set"/></label>
							<aui:input name="channelRangeTimeValue" label="" type="text" inlineField="true"/>
							<aui:select label="xmlio-channel-select-range-time" name="channelRangeTimeUnit" id="channelRangeTypeUnit" inlineField="true">
								<aui:option label="xmlio-channel-range-time-unit-hour" value="<%=IterKeys.XMLIO_CHANNEL_RANGE_TIME_UNIT_HOUR%>" />
								<aui:option label="xmlio-channel-range-time-unit-day" value="<%=IterKeys.XMLIO_CHANNEL_RANGE_TIME_UNIT_DAY%>" />
								<aui:option label="xmlio-channel-range-time-unit-month" value="<%=IterKeys.XMLIO_CHANNEL_RANGE_TIME_UNIT_MONTH%>" />
							</aui:select>
						</aui:fieldset>
						<aui:fieldset>	
							<label class="aui-field-label"><liferay-ui:message key="xmlio-channel-range-type-set"/></label>
							<aui:select label="xmlio-channel-select-range-type" name="channelRangeType" id="channelRangeType" inlineField="true">
								<aui:option label="<%=IterKeys.XMLIO_CHANNEL_RANGE_TYPE_ALL%>" value="<%=IterKeys.XMLIO_CHANNEL_RANGE_TYPE_ALL%>" />
								<%for (String classNameValue : IterKeys.MAIN_CLASSNAME_TYPES_EXPORT){%>
								<aui:option label="<%=classNameValue%>" value="<%=classNameValue%>" />
								<%}%>
							</aui:select>
						</aui:fieldset>
					</div>
				</aui:panel>
			</div>	
			<aui:button-row>
				<aui:button type="submit" value="xmlio-channel-manual-process"/>
				<aui:button onClick="<%= redirect %>" type="cancel" />
				<aui:button onClick="<%= viewChannelControlURL %>" name="xmlio-channel-control" value="xmlio-channel-control" />
			</aui:button-row>
		</aui:form>
		
		<aui:script use="aui-base">
		
			var A = AUI();
		
			A.one('#<portlet:namespace />typeInput').on(
				'change',
				function(event) {
					if (event.currentTarget.get('value') == 'input') {
						A.one('#<portlet:namespace />inputDiv').show();
						A.one('#<portlet:namespace />outputDiv').hide();
						A.one('#<portlet:namespace />rangeDiv').hide();									
					} 		
				}
			);
			
			A.one('#<portlet:namespace />typeOutput').on(
				'change',
				function(event) {
					if (event.currentTarget.get('value') == 'output') {
						A.one('#<portlet:namespace />inputDiv').hide();
						A.one('#<portlet:namespace />outputDiv').show();	
						A.one('#<portlet:namespace />rangeDiv').show();	
					} 		
				}
			);
			
			//Rango de tiempo
			A.one('#<portlet:namespace />rangeTimeAll').on(
					'change',
					function(event) {
						if (event.currentTarget.get('value') == 'true') {
							A.one('#<portlet:namespace />rangeTimeDiv').hide();										
						} 		
					}
				);
				
			A.one('#<portlet:namespace />rangeTimeSelect').on(
				'change',
				function(event) {
					if (event.currentTarget.get('value') == 'false') {
						A.one('#<portlet:namespace />rangeTimeDiv').show();										
					} 		
				}
			);
		</aui:script>

	</c:when>	
	<c:when test='<%= tabs1.equals("automatic") %>'>
	
		<jsp:useBean id="deleteChannelsURL" class="java.lang.String" scope="request" />	
		<jsp:useBean id="addChannelURL" class="java.lang.String" scope="request" />
		
		<%
			String editURL = "self.location = '" + addChannelURL + "'";
		%>
		
		<aui:button-row>													
			<aui:button onClick="<%= editURL %>" name="xmlio-add-new-channel" value="xmlio-add-new-channel" />												
			<aui:button onClick='<%= renderResponse.getNamespace() + "deleteChannels();" %>' name="xmlio-delete-selected" value="xmlio-delete-selected" />
			<aui:button onClick="<%= viewChannelControlURL %>" name="xmlio-channel-control" value="xmlio-channel-control" />			
		</aui:button-row>
		
		<liferay-portlet:renderURL varImpl="iteratorURL">
			<liferay-portlet:param name="tabs1" value="automatic" />
		</liferay-portlet:renderURL>
		
		<aui:form name="fm" method="post" action="<%= deleteChannelsURL %>">
			<liferay-ui:search-container emptyResultsMessage="xmlio-channel-view-empty-result" iteratorURL="<%= iteratorURL %>" rowChecker="<%= new com.liferay.portal.kernel.dao.search.RowChecker(renderResponse) %>">
				<liferay-ui:search-container-results>
					<%
						results = ChannelLocalServiceUtil.getChannelsByGroupId(scopeGroupId, searchContainer.getStart(), searchContainer.getEnd());	
									total = ChannelLocalServiceUtil.getChannelsCountByGroupId(scopeGroupId);							
									pageContext.setAttribute("results", results);
									pageContext.setAttribute("total", total);
					%>
				</liferay-ui:search-container-results>
				
				<liferay-ui:search-container-row  className="com.protecmedia.iter.xmlio.model.Channel" keyProperty="id" modelVar="channel">	
					<%
							channel = channel.toEscapedModel();
														
										PortletURL updateURL = renderResponse.createActionURL();
										updateURL.setParameter("resourcePrimKey", String.valueOf(channel.getPrimaryKey()));
										updateURL.setParameter("tabs1", "automatic");
										updateURL.setParameter("javax.portlet.action", "editChannel");
						%>					
																			
					<liferay-ui:search-container-column-text href="<%= updateURL.toString() %>" name="name"  value="<%= channel.getName() %>"/>
					<liferay-ui:search-container-column-text href="<%= updateURL.toString() %>" name="description" value="<%= channel.getDescription() %>" />
					<liferay-ui:search-container-column-text href="<%= updateURL.toString() %>" name="type" value="<%= channel.getType() %>" />
					<liferay-ui:search-container-column-text href="<%= updateURL.toString() %>" name="status" value='<%= channel.getStatus() ? "ON" : "OFF" %>' />
					<liferay-ui:search-container-column-jsp align="right" path="/html/xmlio-portlet/edit_actions_channel.jsp" />
				</liferay-ui:search-container-row>
					
				<liferay-ui:search-iterator />
			</liferay-ui:search-container>
		</aui:form>
		
		<aui:script use="aui-base">
			Liferay.provide(
				window,
				'<portlet:namespace />deleteChannels',
				function() {
					if (confirm('<%= UnicodeLanguageUtil.get(pageContext, "are-you-sure-you-want-to-delete-the-selected-channels") %>')) {
						document.<portlet:namespace />fm.submit();	
					}
				},
				['aui-base']
			);
		</aui:script>
		
	</c:when>
	<c:when test='<%= tabs1.equals("live") %>'>
		
		<jsp:useBean id="liveConfigurationURL" class="java.lang.String" scope="request" />
		<jsp:useBean id="liveControlURL" class="java.lang.String" scope="request" />
	
		<%
			String viewLiveConfigurationURL = "self.location = '" + liveConfigurationURL + "'";
			String viewLiveControlURL = "self.location = '" + liveControlURL + "'";
			
			//Ordenar la lista
			String orderByCol = ParamUtil.getString(renderRequest, "orderByCol", "modifiedDate");
			String orderByType = ParamUtil.getString(renderRequest, "orderByType", "desc");
		%>
	
		<aui:button-row>													
			<aui:button onClick="<%=viewLiveConfigurationURL%>" name="live-configuration" value="live-configuration" />												
			<aui:button onClick="<%=viewLiveControlURL%>" name="live-control" value="live-control" disabled="<%=environment.equals(IterKeys.ENVIRONMENT_LIVE) %>"/>			
		</aui:button-row>
		
		<aui:panel label="xmlio-view-live-filter" collapsible="true">
			<portlet:actionURL name="setLiveFilter" var="setLiveFilterURL" >
				<portlet:param name="tabs1" value="live" />
				<portlet:param name="orderByCol" value="<%=orderByCol%>" />
				<portlet:param name="orderByType" value="<%=orderByType%>" />
			</portlet:actionURL>
			<aui:form name="fm" method="post" action="<%=setLiveFilterURL%>">					
				<aui:fieldset>
					<aui:select label="xmlio-view-live-select-group" name="liveGroupId" inlineField="true" bean="liveGroupBean">
						<%
							try{
								List<Live> liveGroupEntries = LiveLocalServiceUtil.getLiveByClassNameValue(IterKeys.CLASSNAME_GROUP);
								if(liveGroupEntries.isEmpty()){
						%>
									<aui:option label="xmlio-live-current-channel" value="<%=scopeGroupId%>" selected="true" />
									<%
								}
								else{
									for (Live liveGroupEntry : liveGroupEntries){
									%>
									<aui:option label='<%= liveGroupEntry.getGlobalId().replace(IterLocalServiceUtil.getSystemName() + "_","") %>' value="<%= liveGroupEntry.getGroupId() %>" selected="<%=(liveGroupId==liveGroupEntry.getGroupId())%>" />
									<%
									}
								}
							}
							catch(Exception err){}
									%>				
					</aui:select>
					<aui:select label="xmlio-view-live-select-classname" name="liveClassName" id="liveClassName" inlineField="true">
						<aui:option label="<%=IterKeys.XMLIO_CHANNEL_RANGE_TYPE_ALL%>" value="<%=IterKeys.XMLIO_CHANNEL_RANGE_TYPE_ALL%>" />
						<%for (String className : IterKeys.MAIN_CLASSNAME_TYPES_EXPORT){%>
						<aui:option label="<%=className%>" value="<%=className%>" selected='<%=liveClassName.equals(className)%>'/>
						<%}%>
					</aui:select>
					<aui:input cssClass="xmliolive" name="liveLocalId" label="xmlio-view-live-select-local-id" type="text" inlineField="true" value="<%=liveLocalId %>"/>
					<aui:button-row>				
						<aui:button type="submit" value="xmlio-view-live-set-filter"/>					
					</aui:button-row>	
				</aui:fieldset>		
			</aui:form>
		</aui:panel>
		<c:if test="<%=liveGroupId!=-1 %>">
			<%
				//Obtener Lista
				List<Live> tempResults = LiveLocalServiceUtil.getAllPendingAndErrorLivePool(companyId, liveGroupId, liveClassName, liveLocalId); 
				
				boolean orderByAsc = false;

				if (orderByType.equals("asc")) {
					orderByAsc = true;
				}
				LiveListUtil.orderList( tempResults, orderByCol, orderByAsc );
			%>
			<aui:panel label="xmlio-view-live-status" collapsible="true">
			<%
			   PortletURL liveIteratorURL = renderResponse.createRenderURL();
			   liveIteratorURL.setParameter("tabs1", "live");
			   liveIteratorURL.setParameter("liveGroupId", Long.toString(liveGroupId));
			   liveIteratorURL.setParameter("liveClassName", liveClassName);
			   liveIteratorURL.setParameter("liveLocalId", liveLocalId);
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
						className = LanguageUtil.get(pageContext, live.getClassNameValue());
						if (className.equals("") || className.equals(live.getClassNameValue())){
							try{
								className = live.getClassNameValue().substring(live.getClassNameValue().lastIndexOf(".")+1);}
							catch(Exception err1){}
						}
						try{modifiedDate = live.getModifiedDate().toString();}catch(Exception err){}	
					%>
	
					<liferay-ui:search-container-column-text 														name="xmlio-live-status-groupid" 		value="<%= groupName %>" />	
					<liferay-ui:search-container-column-text orderable="true" orderableProperty="classNameValue"	name="xmlio-live-status-classname" 		value="<%= className %>" />
					<liferay-ui:search-container-column-text orderable="true" orderableProperty="localId" 			name="xmlio-live-status-localid" 		value="<%= live.getLocalId() %>" />	
					<liferay-ui:search-container-column-text orderable="true" orderableProperty="operation" 		name="xmlio-live-status-operation" 		value="<%= live.getOperation() %>" />	
					<liferay-ui:search-container-column-text orderable="true" orderableProperty="status" 			name="xmlio-live-status-status" 		value="<%= live.getStatus() %>" />	
					<liferay-ui:search-container-column-text orderable="true" orderableProperty="modifiedDate"		name="xmlio-live-status-modifieddate"	value="<%= modifiedDate %>" />	
					<liferay-ui:search-container-column-jsp align="right" path="/html/xmlio-portlet/publish_actions_live.jsp" />		
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
				<portlet:param name="resourcePrimKey" value="" />		
			</portlet:actionURL>
			<aui:form name="fm" method="post" action="<%= updateURL %>">					
				<aui:fieldset>	
					<aui:select label="xmlio-channel-select-range-type" name="liveRangeType" id="liveRangeType" inlineField="true">
						<aui:option label="<%=IterKeys.XMLIO_CHANNEL_RANGE_TYPE_ALL%>" value="<%=IterKeys.XMLIO_CHANNEL_RANGE_TYPE_ALL%>" />
						<%for (String className : IterKeys.MAIN_CLASSNAME_TYPES_EXPORT){%>
						<aui:option label="<%=className%>" value="<%=className%>" />
						<%}%>
					</aui:select>
				</aui:fieldset>							
				<aui:button-row>								
					<aui:button type="submit" value="xmlio-view-live-publish-to-live" disabled="<%=environment.equals(IterKeys.ENVIRONMENT_LIVE) %>"/>
				</aui:button-row>
			</aui:form>
		</aui:panel>
		<%-- 
		<aui:panel label="xmlio-view-live-maintenance" collapsible="true">
			<portlet:actionURL name="populateLive" var="updateURL" >
				<portlet:param name="tabs1" value="live" />	
				<portlet:param name="companyId" value="<%= String.valueOf(companyId) %>"/>	
				<portlet:param name="groupId" value="<%= String.valueOf(liveGroupId) %>"/>		
			</portlet:actionURL>
			
			<portlet:actionURL name="cleanLive" var="cleanURL" >
				<portlet:param name="tabs1" value="live" />		
				<portlet:param name="companyId" value="<%= String.valueOf(companyId) %>"/>	
				<portlet:param name="groupId" value="<%= String.valueOf(liveGroupId) %>"/>
			</portlet:actionURL>
			
			<aui:button-row>				
				<aui:button onClick="<%= updateURL %>" type="submit" value="xmlio-view-live-populate-live" disabled="<%=environment.equals(IterKeys.ENVIRONMENT_LIVE) %>"/>
				<aui:button onClick="<%= cleanURL %>" type="submit" value="xmlio-view-live-clean-live" disabled="<%=environment.equals(IterKeys.ENVIRONMENT_LIVE) %>"/>
			</aui:button-row>	
		</aui:panel>
		--%>
		</c:if>
	</c:when>	
</c:choose>

