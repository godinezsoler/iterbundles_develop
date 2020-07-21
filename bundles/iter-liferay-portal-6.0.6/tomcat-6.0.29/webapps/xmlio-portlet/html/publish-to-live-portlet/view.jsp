<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@include file="init.jsp"%>

<%@page import="javax.portlet.PortletURL"%>

<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil"%>

<liferay-ui:success key="xmlio-live-publish-success" message="xmlio-live-publish-success" />
<liferay-ui:error key="xmlio-live-publish-error" message="xmlio-live-publish-error" />

<%-- Dejamos los estilos inline hasta que funcione situarlos en .css --%>

<div class="publish-to-live" style="padding:10px; border:1px solid #CCCCCC;text-align:center;">

<%
	if (LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(company.getCompanyId()) != null){
%>

	<portlet:actionURL var="publishURL" >
		<portlet:param name="action" value="publish" />
	</portlet:actionURL>

	<aui:form name="fm" method="post" action="<%=publishURL%>">
	
		<aui:fieldset label="">			
			<aui:input name="publishMode" type="radio" id="publishPage" value="publish-page" label="xmlio-live-publish-page" inlineField="true" disabled="true"/>
			<aui:input name="publishMode" type="radio" id="publishGroup" value="publish-group" label="xmlio-live-publish-group" inlineField="true" />
		</aui:fieldset>
		<br />
		
		<aui:button-row>
			<aui:button type="submit" value="xmlio-view-live-publish-to-live"/>
		</aui:button-row>
	</aui:form>

<%
}else{
%>

<liferay-ui:message key="xmlio-live-no-live-configuration"/>

<%
}
%>

</div>
