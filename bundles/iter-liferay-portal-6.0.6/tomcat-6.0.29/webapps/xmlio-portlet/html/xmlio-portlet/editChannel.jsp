<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@page import="java.text.DateFormat"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.liferay.portal.kernel.util.CalendarFactoryUtil"%>
<%@ include file="init.jsp"%>

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
<%@page import="com.protecmedia.iter.xmlio.model.Channel"%>
<%@page import="java.util.Calendar"%>
<%@page import="com.liferay.portal.kernel.dao.search.RowChecker"%>
<%@page import="com.protecmedia.iter.xmlio.service.ChannelLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>

<jsp:useBean id="channel" type="com.protecmedia.iter.xmlio.model.Channel" scope="request" />
<jsp:useBean class="java.lang.String" id="editChannelURL" scope="request" />

<portlet:renderURL var="cancelChannelURL" >
	<portlet:param name="tabs1" value="automatic" />
</portlet:renderURL>

<liferay-ui:success key="xmlio-channel-updated-success" message="xmlio-channel-updated-success" />
<liferay-ui:success key="xmlio-channel-added-success" message="xmlio-channel-added-success" />

<liferay-ui:error key="xmlio-channel-name-required" message="xmlio-channel-name-required" />
<liferay-ui:error key="xmlio-channel-description-required" message="xmlio-channel-description-required" />
<liferay-ui:error key="xmlio-channel-file-path-required" message="xmlio-channel-file-path-required" />
<liferay-ui:error key="xmlio-channel-program-time-required" message="xmlio-channel-program-time-required" />

<%		

	//Cargamos los datos del canal
	String channelId = GetterUtil.getString(String.valueOf(channel.getPrimaryKey()), "-1");
	String channelName = GetterUtil.getString(channel.getName(), "");
	String channelDescription = GetterUtil.getString(channel.getDescription(), "");
	String channelType = GetterUtil.getString(channel.getType(), IterKeys.XMLIO_CHANNEL_TYPE_INPUT);
	if(channelType.equals(""))
		channelType = IterKeys.XMLIO_CHANNEL_TYPE_INPUT;
	
	String channelMode = GetterUtil.getString(channel.getMode(), IterKeys.XMLIO_CHANNEL_MODE_FILE_SYSTEM);
	String channelFilePath = GetterUtil.getString(channel.getFilePath(), "");
	String channelXslPath = GetterUtil.getString(channel.getXslPath(), "");
	String channelFtpServer = GetterUtil.getString(channel.getFtpServer(), "");
	String channelFtpUser = GetterUtil.getString(channel.getFtpUser(), "");
	String channelFtpPassword = GetterUtil.getString(channel.getFtpPassword(), "");
	boolean channelStatus = GetterUtil.getBoolean(channel.getStatus(), false);
	boolean channelProgram = GetterUtil.getBoolean(channel.getProgram(), false);
	String channelProgramHour = channel.getProgramHour() == -1 ? "" : String.valueOf(channel.getProgramHour());
	String channelProgramMin = channel.getProgramMin() == -1 ? "" : String.valueOf(channel.getProgramMin());
	
	String channelRangeType = GetterUtil.getString(channel.getRangeType(), IterKeys.XMLIO_CHANNEL_RANGE_TYPE_ALL);
	boolean channelRangeTimeAll = GetterUtil.getBoolean(channel.getRangeTimeAll(), false);
	int channelRangeTimeValue = GetterUtil.getInteger(channel.getRangeTimeValue(), -1);
	String channelRangeTimeUnit = GetterUtil.getString(channel.getRangeTimeUnit(), IterKeys.XMLIO_CHANNEL_RANGE_TIME_UNIT_HOUR);
	
%>
	
<liferay-ui:tabs
names="automatic" 
	backURL="<%= cancelChannelURL %>" 
/>
	
<aui:form name="fm1" action="<%= editChannelURL %>" method="post">
	<aui:model-context bean="<%= channel %>" model="<%= Channel.class %>" />
	<aui:input name="channelId" id="channelId" type="hidden" value="<%=channelId%>"/>
	<aui:input name="channelStatus" id="channelStatus" type="hidden" value="<%=channelStatus%>"/>
	<aui:panel label="xmlio-channel-general" collapsible="true">
		<aui:fieldset>	
			<aui:input name="channelName" type="text" id="channelName" value="<%=channelName%>" label="xmlio-channel-name"/>
			<aui:input name="channelDescription" type="textarea" id="channelDescription" value="<%=channelDescription%>" label="xmlio-channel-description" maxlength="75" />	
			<aui:select label="xmlio-channel-select-type" name="channelType" id="channelType">
				<aui:option label="xmlio-channel-input" selected="<%=channelType.equals(IterKeys.XMLIO_CHANNEL_TYPE_INPUT)%>" value="<%=IterKeys.XMLIO_CHANNEL_TYPE_INPUT%>" />
				<aui:option label="xmlio-channel-output" selected="<%=channelType.equals(IterKeys.XMLIO_CHANNEL_TYPE_OUTPUT)%>" value="<%=IterKeys.XMLIO_CHANNEL_TYPE_OUTPUT%>" />
				<aui:option label="xmlio-channel-output-milenium" selected="<%=channelType.equals(IterKeys.XMLIO_CHANNEL_TYPE_OUTPUT_MILENIUM)%>" value="<%=IterKeys.XMLIO_CHANNEL_TYPE_OUTPUT_MILENIUM%>" />
			</aui:select>
		</aui:fieldset>
	</aui:panel>
	
	<aui:panel label="xmlio-channel-mode">
		<aui:fieldset>			
			<aui:input name="channelMode" type="radio" id="fileMode" value="<%=IterKeys.XMLIO_CHANNEL_MODE_FILE_SYSTEM %>" checked='<%= !channelMode.equals(IterKeys.XMLIO_CHANNEL_MODE_FTP) %>' label="xmlio-channel-mode-file-system" inlineField="true" />
			<aui:input name="channelMode" type="radio" id="ftpMode" value="<%=IterKeys.XMLIO_CHANNEL_MODE_FTP%>" checked='<%= channelMode.equals(IterKeys.XMLIO_CHANNEL_MODE_FTP) %>' label="xmlio-channel-mode-ftp" inlineField="true" />
		</aui:fieldset>
		<div class='<%= channelMode.equals(IterKeys.XMLIO_CHANNEL_MODE_FTP) ? "" : "aui-helper-hidden" %>' id="<portlet:namespace />modeFTPDiv">		
			<aui:fieldset>
				<aui:input name="channelFtpServer" label="xmlio-channel-ftp-server" type="text" value="<%= channelFtpServer %>" />
				<aui:input name="channelFtpUser" label="xmlio-channel-ftp-user" type="text" value="<%= channelFtpUser %>" />
				<aui:input name="channelFtpPassword" label="xmlio-channel-ftp-password" type="password" value="<%= channelFtpPassword %>" />								
			</aui:fieldset>	
		</div>
	</aui:panel>
	<div class='<%= ( !channelType.equals(IterKeys.XMLIO_CHANNEL_TYPE_INPUT) && channelMode.equals(IterKeys.XMLIO_CHANNEL_MODE_FTP) ) ? "aui-helper-hidden" : ""  %>' id="<portlet:namespace />outPathDiv">	
		<aui:panel  label="xmlio-channel-source" collapsible="true">
			<aui:fieldset>
				<aui:input name="channelFilePath" type="text" label="xmlio-channel-file-path" value="<%=channelFilePath %>"/>
			</aui:fieldset>
		</aui:panel>
	</div>
	
	<div class="" id="<portlet:namespace />xslDiv">
		<aui:panel label="xmlio-channel-xsl-transformation" collapsible="true">
				<aui:fieldset>
					<aui:input name="channelXslPath" type="text" label="xmlio-channel-xsl-file-path" value="<%=channelXslPath %>"/>
				</aui:fieldset>
		</aui:panel>
	</div>
	
	<div class='<%= ( channelType.equals(IterKeys.XMLIO_CHANNEL_TYPE_INPUT) ) ? "aui-helper-hidden" : "" %>' id="<portlet:namespace />rangeDiv">
		<aui:panel label="xmlio-channel-range" collapsible="true">
			<aui:fieldset>		
				<aui:input name="channelRangeTimeAll" id="rangeTimeAll" checked="<%=channelRangeTimeAll %>" label="xmlio-channel-range-time-all" type="radio" inlineField="true" value="true" />
				<aui:input name="channelRangeTimeAll" id="rangeTimeSelect" checked="<%=!channelRangeTimeAll %>" label="xmlio-channel-range-time-select" type="radio" inlineField="true" value="false" />
			</aui:fieldset>
			<div class='<%= !channelRangeTimeAll ? "" : "aui-helper-hidden" %>' id="<portlet:namespace />rangeTimeDiv">
				<aui:fieldset>	
					<label class="aui-field-label"><liferay-ui:message key="xmlio-channel-range-time-set"/></label>
					<aui:input name="channelRangeTimeValue" label="" type="text" value="<%= channelProgramHour %>" inlineField="true"/>
					<aui:select label="xmlio-channel-select-range-time" name="channelRangeTimeUnit" id="channelRangeTypeUnit" inlineField="true">
						<aui:option label="xmlio-channel-range-time-unit-hour" selected="<%=channelRangeTimeUnit.equals(IterKeys.XMLIO_CHANNEL_RANGE_TIME_UNIT_HOUR)%>" value="<%=IterKeys.XMLIO_CHANNEL_RANGE_TIME_UNIT_HOUR%>" />
						<aui:option label="xmlio-channel-range-time-unit-day" selected="<%=channelRangeTimeUnit.equals(IterKeys.XMLIO_CHANNEL_RANGE_TIME_UNIT_DAY)%>" value="<%=IterKeys.XMLIO_CHANNEL_RANGE_TIME_UNIT_DAY%>" />
						<aui:option label="xmlio-channel-range-time-unit-month" selected="<%=channelRangeTimeUnit.equals(IterKeys.XMLIO_CHANNEL_RANGE_TIME_UNIT_MONTH)%>" value="<%=IterKeys.XMLIO_CHANNEL_RANGE_TIME_UNIT_MONTH%>" />
					</aui:select>
				</aui:fieldset>
				<aui:fieldset>	
					<label class="aui-field-label"><liferay-ui:message key="xmlio-channel-range-type-set"/></label>
					<aui:select label="xmlio-channel-select-range-type" name="channelRangeType" id="channelRangeType" inlineField="true">
						<%for (String className : IterKeys.MAIN_CLASSNAME_TYPES_EXPORT){%>
						<aui:option label="<%=className%>" selected="<%=channelRangeType.equals(className)%>" value="<%=className%>" />
						<%}%>
					</aui:select>
				</aui:fieldset>
			</div>
		</aui:panel>
	</div>
	<aui:panel label="xmlio-channel-schedule" collapsible="true">
		<aui:fieldset>		
			<aui:input name="channelProgram" id="programDefault" checked="<%=!channelProgram %>" label="xmlio-channel-program-default" type="radio" inlineField="true" value="false" />
			<aui:input name="channelProgram" id="programTime" checked="<%=channelProgram %>" label="xmlio-channel-program-time" type="radio" inlineField="true" value="true" />
		</aui:fieldset>
		<div class='<%= channelProgram ? "" : "aui-helper-hidden" %>' id="<portlet:namespace />scheduleDiv">
			<aui:fieldset>	
				<label class="aui-field-label"><liferay-ui:message key="xmlio-channel-program-set-time"/></label>
				<aui:input name="channelProgramHour" label="" type="text" value="<%= channelProgramHour %>" inlineField="true"/>:
				<aui:input name="channelProgramMin" label="" type="text" value="<%= channelProgramMin %>" inlineField="true"/>								
			</aui:fieldset>
		</div>
	</aui:panel>

	<aui:button-row>
		<aui:button type="submit" />
		<aui:button onClick="<%= cancelChannelURL %>" type="cancel" />
	</aui:button-row>
</aui:form>

<aui:script use="aui-base">

	var A = AUI();
	
	//Tipo de canal
	//Liferay.Util.toggleSelectBox('<portlet:namespace />channelType','<%=IterKeys.XMLIO_CHANNEL_TYPE_OUTPUT_MILENIUM%>','<portlet:namespace />xslDiv');
	A.one('#<portlet:namespace />channelType').on(
		'change',
		function(event) {
			if (event.currentTarget.get('value') == '<%=IterKeys.XMLIO_CHANNEL_TYPE_INPUT%>') {
				A.one('#<portlet:namespace />rangeDiv').hide();
				A.one('#<portlet:namespace />xslDiv').show();
				A.one('#<portlet:namespace />outPathDiv').show();
			}else if(event.currentTarget.get('value') == '<%=IterKeys.XMLIO_CHANNEL_TYPE_OUTPUT%>'){
				A.one('#<portlet:namespace />rangeDiv').show();
				A.one('#<portlet:namespace />xslDiv').show();
				if( A.one('#<portlet:namespace />ftpMode').get('checked') == true){
					A.one('#<portlet:namespace />outPathDiv').hide();
				}else{
					A.one('#<portlet:namespace />outPathDiv').show();
				}
			}else{
				A.one('#<portlet:namespace />rangeDiv').show();
	    		A.one('#<portlet:namespace />xslDiv').hide();
	    		if( A.one('#<portlet:namespace />ftpMode').get('checked') == true){
					A.one('#<portlet:namespace />outPathDiv').hide();
				}else{
					A.one('#<portlet:namespace />outPathDiv').show();
				}
			}
		}
	);

	//Programacion
	A.one('#<portlet:namespace />programTime').on(
		'change',
		function(event) {
			if (event.currentTarget.get('value') == 'true') {
				A.one('#<portlet:namespace />scheduleDiv').show();					
			} 		
		}
	);
	
	A.one('#<portlet:namespace />programDefault').on(
		'change',
		function(event) {
			if (event.currentTarget.get('value') == 'false') {
				A.one('#<portlet:namespace />scheduleDiv').hide();					
			} 		
		}
	);

	//Modo de salida
	A.one('#<portlet:namespace />fileMode').on(
			'change',
			function(event) {
				if (event.currentTarget.get('value') == '<%=IterKeys.XMLIO_CHANNEL_MODE_FILE_SYSTEM %>') {
					A.one('#<portlet:namespace />modeFTPDiv').hide();
					A.one('#<portlet:namespace />outPathDiv').show();
				} 		
			}
		);
		
	A.one('#<portlet:namespace />ftpMode').on(
		'change',
		function(event) {
			if (event.currentTarget.get('value') == '<%=IterKeys.XMLIO_CHANNEL_MODE_FTP %>') {
				A.one('#<portlet:namespace />modeFTPDiv').show();
				if( A.one('#<portlet:namespace />channelType').get('value') == '<%=IterKeys.XMLIO_CHANNEL_TYPE_INPUT%>' ){
					A.one('#<portlet:namespace />outPathDiv').show();
				}else{
					A.one('#<portlet:namespace />outPathDiv').hide();
				}
			} 		
		}
	);
	
	//Rango de tiempo
	//Modo de salida
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
