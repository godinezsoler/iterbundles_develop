<%/**
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
*/%>
<%@include file="init.jsp"%>

<%
	ResultRow resultRow = (ResultRow) request.getAttribute(WebKeys.SEARCH_CONTAINER_RESULT_ROW);
	ChannelControlLog chanelControlLogItem = (ChannelControlLog) resultRow.getObject();
%>

<liferay-ui:icon-menu cssClass="">
	<aui:button onClick='<%= renderResponse.getNamespace() + "showControlErrorLog(\'" + chanelControlLogItem.getErrorLog().replace("\'","") + "\');" %>' value="xmlio-live-detail-error-view"/>
</liferay-ui:icon-menu>
