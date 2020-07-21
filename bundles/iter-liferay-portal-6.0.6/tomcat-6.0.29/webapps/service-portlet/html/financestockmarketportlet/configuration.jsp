<%@ include file="init.jsp" %>

<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %>    
 
<liferay-portlet:actionURL portletConfiguration="true" var="configurationActionURL" />
<div class="fnncstckmrkt">
<aui:form name="fm" method="post" action="<%= configurationActionURL %>">
 
	<div class="portlet-msg-info">
		<liferay-ui:message key="Choose The Web Content that you want to search for:" /> 
	</div>
	  
	<aui:panel label="params-config-finance-stock-market" collapsible="true">
	
		<aui:input name="tradesListPlain" size="110" value="<%= tradesListPlain %>" label="params-config-finance-stock-market-tradesListPlain" />
		<aui:input name="updateFrecuency" value="<%= updateFrecuency %>" label="params-config-finance-stock-market-updateFrecuency" />
		<aui:input inlineLabel="left" name="showChart" label="params-config-finance-stock-market-showChart" type="checkbox" value="<%= showChart %>" />
	</aui:panel>
	
	<br />
	
	<aui:button type="submit" value="save" onClick="submitForm(document.fm);"/>
	
</aui:form>
 
</div>
