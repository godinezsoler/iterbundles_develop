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

<portlet:renderURL var="cancelLiveConfigurationURL" >
	<portlet:param name="tabs1" value="live" />
</portlet:renderURL>

<liferay-ui:success key="xmlio-live-configuration-updated" message="xmlio-live-configuration-updated" />
<liferay-ui:error key="xmlio-live-configuration-local-path-required" message="xmlio-live-configuration-local-path-required" />
<liferay-ui:error key="xmlio-live-configuration-remote-path-required" message="xmlio-live-configuration-remote-path-required" />
<liferay-ui:error key="xmlio-live-configuration-iter-remote-server-required" message="xmlio-live-configuration-iter-remote-server-required" />

<liferay-ui:error key="xmlio-live-configuration-all-ftp-data-required" message="xmlio-live-configuration-all-ftp-data-required" />
<liferay-ui:error key="xmlio-live-configuration-remote-channel-id-required" message="xmlio-live-configuration-remote-channel-id-required" />
<liferay-ui:error key="xmlio-live-configuration-all-remote-user-data-required" message="xmlio-live-configuration-all-remote-user-data-required" />
<liferay-ui:error key="xmlio-live-configuration-remote-path-cannot-be-same-as-local-required" message="xmlio-live-configuration-remote-path-cannot-be-same-as-local-required" />

<liferay-ui:tabs
names="live-configuration" 
	backURL="<%= cancelLiveConfigurationURL %>" 
/>

<%
String liveConfigAction = "";
long primaryKey = -1;
//Default=FILE-SYSTEM
String outputMethod = IterKeys.LIVE_CONFIG_OUTPUT_METHOD_FILE_SYSTEM;
//Default=EXPLICIT
String destinationType = IterKeys.LIVE_CONFIG_DESTINATION_TYPE_EXPLICIT;
String remoteChannelId = "";
String localPath = "";
String remotePath = "";
String remoteIterServer = "";
String gatewayHost = "";
String remoteUserId = "";
String remoteUserName = "";
String remoteUserPassword = "";
String remoteCompanyId = "";
String remoteGlobalGroupId = "";
String ftpPath = "";
String ftpUser = "";
String ftpPassword ="";		
String archive = "";
String connectionTimeout = "";
String operationTimeout = "";

LiveConfiguration liveConf = LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(companyId);					
if (liveConf != null){
	primaryKey = liveConf.getPrimaryKey();
	localPath = liveConf.getLocalPath();
	remotePath = liveConf.getRemotePath();
	remoteIterServer = liveConf.getRemoteIterServer2();
	gatewayHost = liveConf.getGatewayHost();
	outputMethod = liveConf.getOutputMethod();
	destinationType = liveConf.getDestinationType();
	remoteChannelId = String.valueOf(liveConf.getRemoteChannelId());
	remoteUserId = String.valueOf(liveConf.getRemoteUserId());
	remoteUserName = liveConf.getRemoteUserName();
	remoteUserPassword = liveConf.getRemoteUserPassword();
	remoteCompanyId = String.valueOf(liveConf.getRemoteCompanyId());
	remoteGlobalGroupId = String.valueOf(liveConf.getRemoteGlobalGroupId());
	ftpPath = liveConf.getFtpPath();
	ftpUser = liveConf.getFtpUser();
	ftpPassword = liveConf.getFtpPassword();
	archive = String.valueOf(liveConf.getArchive());
	connectionTimeout = String.valueOf(liveConf.getConnectionTimeOut()/1000);
	operationTimeout = String.valueOf(liveConf.getOperationTimeOut()/1000);
}

%>	

<portlet:actionURL name="updateLiveConfiguration" var="updateURL" >
	<portlet:param name="tabs1" value="live" />
	<portlet:param name="view" value="liveConfiguration" />
	<portlet:param name="resourcePrimKey" value="<%= String.valueOf(primaryKey) %>" />
</portlet:actionURL>
<aui:form name="fm" method="post" action="<%= updateURL %>">
	<aui:panel label="xmlio-live-configuration-general" collapsible="true">					
		<aui:fieldset>
			<aui:input name="companyId" type="hidden" value="<%= companyId %>" />
			<aui:input name="gatewayHost" label="xmlio-live-configuration-gateway-host" value="<%= gatewayHost %>"/>												
			<aui:input name="localPath" label="xmlio-live-configuration-local-path" value="<%= localPath %>" />									
			<aui:input name="remoteIterServer" label="xmlio-live-configuration-remote-server" value="<%= remoteIterServer %>"/>												
		</aui:fieldset>	
	</aui:panel>
	<aui:panel label="xmlio-live-configuration-output-method" collapsible="true">	
		<aui:fieldset>			
			<aui:input name="outputMethod" type="radio" id="output-method-ftp" value="<%=IterKeys.LIVE_CONFIG_OUTPUT_METHOD_FTP %>" checked='<%= outputMethod.equals(IterKeys.LIVE_CONFIG_OUTPUT_METHOD_FTP) %>' label="xmlio-live-configuration-output-method-ftp" inlineField="true" />
			<aui:input name="outputMethod" type="radio" id="output-method-file-system" value="<%=IterKeys.LIVE_CONFIG_OUTPUT_METHOD_FILE_SYSTEM %>" checked='<%= outputMethod.equals(IterKeys.LIVE_CONFIG_OUTPUT_METHOD_FILE_SYSTEM) %>' label="xmlio-live-configuration-output-method-file-system" inlineField="true" />
		</aui:fieldset>
		<div class='<%= outputMethod.equals(IterKeys.LIVE_CONFIG_OUTPUT_METHOD_FILE_SYSTEM) ? "" : "aui-helper-hidden" %>' id="<portlet:namespace />outputMethodFileDiv">		
			<aui:fieldset>
				<aui:input name="remotePath" label="xmlio-live-configuration-remote-path" value="<%= remotePath %>" />								
			</aui:fieldset>	
		</div>
		<div class='<%= outputMethod.equals(IterKeys.LIVE_CONFIG_OUTPUT_METHOD_FTP) ? "" : "aui-helper-hidden" %>' id="<portlet:namespace />outputMethodFTPDiv">		
			<aui:fieldset>
				<aui:input name="ftpPath" label="xmlio-live-configuration-ftp-path" value="<%= ftpPath %>" />
				<aui:input name="ftpUser" label="xmlio-live-configuration-ftp-user" value="<%= ftpUser %>" />
				<aui:input name="ftpPassword" label="xmlio-live-configuration-ftp-password" type="password" value="<%= ftpPassword %>" />								
			</aui:fieldset>	
		</div>
	</aui:panel>
	<aui:panel label="xmlio-live-configuration-destination-type" collapsible="true">	
		<aui:fieldset>			
			<aui:input name="destinationType" type="radio" id="destination-type-explicit" value="<%=IterKeys.LIVE_CONFIG_DESTINATION_TYPE_EXPLICIT %>" checked='<%= destinationType.equals(IterKeys.LIVE_CONFIG_DESTINATION_TYPE_EXPLICIT) %>' label="xmlio-live-configuration-destination-type-explicit" inlineField="true" />
			<aui:input name="destinationType" type="radio" id="destination-type-channel" value="<%=IterKeys.LIVE_CONFIG_DESTINATION_TYPE_CHANNEL %>" checked='<%= destinationType.equals(IterKeys.LIVE_CONFIG_DESTINATION_TYPE_CHANNEL) %>' label="xmlio-live-configuration-destination-type-channel" inlineField="true" />
		</aui:fieldset>
		<aui:fieldset>
			<aui:input name="remoteUserId" label="xmlio-live-configuration-remote-user-id" value="<%= remoteUserId %>" />
			<aui:input name="remoteUserName" label="xmlio-live-configuration-remote-user-name" value="<%= remoteUserName %>" />
			<aui:input name="remoteUserPassword" label="xmlio-live-configuration-remote-user-password" type="password" value="<%= remoteUserPassword %>" />	
			<aui:input name="remoteCompanyId" label="xmlio-live-configuration-remote-company-id" type="text" value="<%= remoteCompanyId %>" />	
			<aui:input name="remoteGlobalGroupId" label="xmlio-live-configuration-remote-global-group-id" type="text" value="<%= remoteGlobalGroupId %>" />								
		</aui:fieldset>	
		<div class='<%= destinationType.equals(IterKeys.LIVE_CONFIG_DESTINATION_TYPE_CHANNEL) ? "" : "aui-helper-hidden" %>' id="<portlet:namespace />destinationTypeChannelDiv">	
			<aui:fieldset>
				<aui:input name="remoteChannelId" label="xmlio-live-configuration-remote-channel-id" value="<%= remoteChannelId %>" />								
			</aui:fieldset>
		</div>			
	</aui:panel>
	<aui:panel label="xmlio-live-configuration-timeout-settings" collapsible="true">
		<aui:fieldset>			
			<aui:input name="operationTimeout" label="xmlio-live-configuration-operation-timeout" id="operationTimeout" value="<%=operationTimeout%>" />
			<aui:input name="connectionTimeout" label="xmlio-live-configuration-connection-timeout" id="connectionTimeout" value="<%=connectionTimeout%>" />
		</aui:fieldset>
		</aui:panel>
	<aui:button-row>								
		<aui:button type="submit" value="xmlio-live-configuration-update"/>
	</aui:button-row>
</aui:form>
	
	
<aui:script use="aui-base">

	var A = AUI();

	A.one('#<portlet:namespace />output-method-file-system').on(
		'change',
		function(event) {
			if (event.currentTarget.get('value') == '<%=IterKeys.LIVE_CONFIG_OUTPUT_METHOD_FILE_SYSTEM %>') {
				A.one('#<portlet:namespace />outputMethodFTPDiv').hide();
				A.one('#<portlet:namespace />outputMethodFileDiv').show();											
			} 		
		}
	);
	
	A.one('#<portlet:namespace />output-method-ftp').on(
		'change',
		function(event) {
			if (event.currentTarget.get('value') == '<%=IterKeys.LIVE_CONFIG_OUTPUT_METHOD_FTP %>') {
				A.one('#<portlet:namespace />outputMethodFTPDiv').show();	
				A.one('#<portlet:namespace />outputMethodFileDiv').hide();									
			} 		
		}
	);
	
	A.one('#<portlet:namespace />destination-type-channel').on(
		'change',
		function(event) {
			if (event.currentTarget.get('value') == '<%=IterKeys.LIVE_CONFIG_DESTINATION_TYPE_CHANNEL %>') {
				A.one('#<portlet:namespace />destinationTypeChannelDiv').show();	
			} 		
		}
	);
	
	A.one('#<portlet:namespace />destination-type-explicit').on(
		'change',
		function(event) {
			if (event.currentTarget.get('value') == '<%=IterKeys.LIVE_CONFIG_DESTINATION_TYPE_EXPLICIT %>') {
				A.one('#<portlet:namespace />destinationTypeChannelDiv').hide();			
			} 		
		}
	);

</aui:script>