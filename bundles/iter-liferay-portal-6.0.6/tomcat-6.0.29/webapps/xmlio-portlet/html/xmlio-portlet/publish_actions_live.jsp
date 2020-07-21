<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@include file="init.jsp"%>

<%
	ResultRow resultRow = (ResultRow) request.getAttribute(WebKeys.SEARCH_CONTAINER_RESULT_ROW);
	Live liveItem = (Live) resultRow.getObject();
	
	boolean hasPoolChildren = false;
	try{
		List<LivePool> livePoolList = LivePoolLocalServiceUtil.getLivePoolListByPoolId(liveItem.getId());
		hasPoolChildren = livePoolList.size() > 1;
	}
	catch(Exception err){}
%>

<liferay-ui:icon-menu cssClass="">

	<portlet:actionURL name="viewLiveItem" var="viewLiveItemURL">
		<portlet:param name="resourcePrimKey" value="<%= String.valueOf(liveItem.getPrimaryKey()) %>" />
		<portlet:param name="tabs1" value="live" />
	</portlet:actionURL>
	
	<liferay-ui:icon image="edit" message="xmlio-live-actions-view" url="<%= viewLiveItemURL.toString() %>" />
	<%--  
	<c:choose>		
		<c:when test="<%=hasPoolChildren%>">
			<liferay-ui:icon image="edit" message="xmlio-live-actions-view" url="<%= viewLiveItemURL.toString() %>" />
		</c:when>
		<c:otherwise>
			<liferay-ui:icon image="edit" cssClass="disabledLive" message="xmlio-live-actions-view"/>
		</c:otherwise>		
	</c:choose>
	--%>
	
	<portlet:actionURL name="publishToLive" var="publishLiveItemURL">
		<portlet:param name="resourcePrimKey" value="<%= String.valueOf(liveItem.getPrimaryKey()) %>" />
		<portlet:param name="liveRangeType" value="<%= liveItem.getClassNameValue() %>" />
		<portlet:param name="companyId" value="<%=String.valueOf(companyId) %>" />
		<portlet:param name="groupId" value="<%= String.valueOf(liveItem.getGroupId()) %>" />
		<portlet:param name="userId" value="<%= String.valueOf(userId) %>" />		
		<portlet:param name="tabs1" value="live" />
	</portlet:actionURL>
	
	<c:choose>
		<c:when test="<%=environment.equals(IterKeys.ENVIRONMENT_PREVIEW)%>">
			<liferay-ui:icon image="activate" message="xmlio-live-actions-publish" url="<%= publishLiveItemURL.toString() %>" />
		</c:when>
		<c:otherwise>
			<liferay-ui:icon image="activate" message="xmlio-live-actions-publish" cssClass="disabled"/>
		</c:otherwise>
	</c:choose>

</liferay-ui:icon-menu>