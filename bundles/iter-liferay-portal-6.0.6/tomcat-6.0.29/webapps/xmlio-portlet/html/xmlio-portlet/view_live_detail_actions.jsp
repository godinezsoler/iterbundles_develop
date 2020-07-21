<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@include file="init.jsp"%>

<%
	ResultRow resultRow = (ResultRow) request.getAttribute(WebKeys.SEARCH_CONTAINER_RESULT_ROW);
	Live liveItem = (Live) resultRow.getObject();
	
%>

<liferay-ui:icon-menu cssClass="">

	<c:choose>
		<c:when test="<%=liveItem.getStatus().equals(IterKeys.ERROR) %>">
			<aui:button onClick='<%= renderResponse.getNamespace() + "showErrorLog(\'" + liveItem.getErrorLog().replace("\'","") + "\');" %>' value="xmlio-live-detail-error-view" disabled="false"/>
		</c:when>
		<c:otherwise>
			<aui:button value="xmlio-live-detail-error-view" disabled="true"/>
		</c:otherwise>
	</c:choose>

</liferay-ui:icon-menu>