<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@include file="init.jsp"%>

<%
	ResultRow resultRow = (ResultRow) request.getAttribute(WebKeys.SEARCH_CONTAINER_RESULT_ROW);
	ChannelControl chanelControlItem = (ChannelControl) resultRow.getObject();
	
	//TODO Confirmar que es un error.
	boolean isError = false;
	boolean errors = false;
	boolean allowDelete = false;
	
	try{
		isError = chanelControlItem.getStatus().equals( IterKeys.ERROR ) ? true : false ;
		errors = (chanelControlItem.getErrors()==0 || chanelControlItem.getErrors()==-1) ? false : true;
		allowDelete = (environment.equals(IterKeys.ENVIRONMENT_PREVIEW) || chanelControlItem.getStatus().equals( IterKeys.PROCESSING )) ? true : false;
	}
	catch(Exception err){}
	
%>

<liferay-ui:icon-menu cssClass="">

	<portlet:actionURL name="viewChannelControlLog" var="viewChannelControlLogURL">
		<portlet:param name="resourcePrimKey" value="<%= String.valueOf(chanelControlItem.getPrimaryKey()) %>" />
		<portlet:param name="tabs1" value="<%= tabs1 %>" />
		<portlet:param name="view" value="channelControlLog" />
	</portlet:actionURL>

	<c:choose>
		<c:when test="<%=isError%>">
			<liferay-ui:icon image="edit" message="xmlio-channel-control-actions-view" 
				url="<%= \"javascript:\" + renderResponse.getNamespace() + \"showControlErrorLog('\" + chanelControlItem.getErrorLog().replace(\"'\",\"\") + \"');\" %>" />
		</c:when>
		<c:when test="<%=!errors%>">
			<liferay-ui:icon image="edit" message="xmlio-channel-control-actions-view" cssClass="disabled"/>
		</c:when>
		<c:otherwise>
			<liferay-ui:icon image="edit" message="xmlio-channel-control-actions-view" url="<%= viewChannelControlLogURL.toString() %>" />
		</c:otherwise>
	</c:choose>
	
	<portlet:actionURL name="deleteChannelControlItem" var="deleteChannelControlItemURL">
		<portlet:param name="resourcePrimKey" value="<%= String.valueOf(chanelControlItem.getPrimaryKey()) %>" />
		<portlet:param name="tabs1" value="<%= tabs1 %>" />
	</portlet:actionURL>
	
	<c:choose>
		<c:when test="<%= allowDelete %>">
			<liferay-ui:icon image="delete" message="xmlio-channel-control-actions-delete" url="<%= deleteChannelControlItemURL.toString() %>" />
		</c:when>
		<c:otherwise>
			<liferay-ui:icon image="delete" message="xmlio-channel-control-actions-delete" cssClass="disabled"/>
		</c:otherwise>
	</c:choose>

</liferay-ui:icon-menu>