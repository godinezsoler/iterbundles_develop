<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@ include file="init.jsp" %>

<%@page import="com.protecmedia.iter.news.util.BreadCrumbUtil"%>
<%@page import="com.liferay.portal.service.GroupLocalServiceUtil"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />


<%

String breadCrumbString = BreadCrumbUtil.getBreadcrumb(request,globalGroupId,showWebName, webName, separator);

%>

<%= breadCrumbString %>



