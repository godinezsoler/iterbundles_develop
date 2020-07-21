<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@include file="init.jsp"%>

<%
	ResultRow resultRow = (ResultRow) request.getAttribute(WebKeys.SEARCH_CONTAINER_RESULT_ROW);
	LiveControl liveCtrl = (LiveControl) resultRow.getObject();
%>

<liferay-ui:icon-menu cssClass="">

	<portlet:actionURL name="unlockLiveControl" var="unlockLiveControlURL">
		<portlet:param name="resourcePrimKey" value="<%= String.valueOf(liveCtrl.getPrimaryKey()) %>" />
		<portlet:param name="processId" value="<%= liveCtrl.getProcessId() %>" />
		<portlet:param name="tabs1" value="live" />
		<portlet:param name="view" value="liveControl" />
	</portlet:actionURL>

	<c:choose>
		<c:when test="<%=(liveCtrl.getStatus().equals(IterKeys.INTERRUPT) || liveCtrl.getStatus().equals(IterKeys.ERROR)) %>">
			<liferay-ui:icon image="view"
				url='<%= \"javascript:\" + renderResponse.getNamespace() + \"showControlErrorLog(\'\" + liveCtrl.getErrorLog().replace(\"\'\",\"\") + \"\');" %>' 
				message="xmlio-live-detail-error-view" cssClass="enabled"/>
		</c:when>
		<c:otherwise>
			<liferay-ui:icon image="view" message="xmlio-live-detail-error-view" cssClass="disabled"/>
		</c:otherwise>
	</c:choose>
	
	<c:choose>
		<c:when test="<%=liveCtrl.getStatus().equals(IterKeys.PROCESSING)%>">
			<liferay-ui:icon image="delete" message="xmlio-live-control-unlock" url="<%= unlockLiveControlURL.toString() %>" />
		</c:when>
		<c:otherwise>
			<liferay-ui:icon image="delete" message="xmlio-live-control-unlock" cssClass="disabled"/>
		</c:otherwise>
	</c:choose>
	
	
</liferay-ui:icon-menu>