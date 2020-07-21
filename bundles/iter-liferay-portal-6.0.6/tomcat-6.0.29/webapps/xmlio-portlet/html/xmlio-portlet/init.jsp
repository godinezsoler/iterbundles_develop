<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%@page import="com.liferay.portal.kernel.util.Constants"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys" %>
<%@page import="com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil" %>
<%@page import="com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.xmlio.service.LivePoolLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.xmlio.service.LiveControlLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.xmlio.model.Live" %>
<%@page import="com.protecmedia.iter.xmlio.model.LiveControl" %>
<%@page import="com.protecmedia.iter.xmlio.model.LiveConfiguration" %>
<%@page import="com.protecmedia.iter.xmlio.model.LivePool" %>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil" %>
<%@page import="com.liferay.portal.kernel.util.ListUtil"%>
<%@page import="com.liferay.portal.service.GroupLocalServiceUtil"%>
<%@page import="com.liferay.portal.service.UserLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.dao.search.ResultRow"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.security.permission.ActionKeys"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="javax.portlet.PortletURL"%>
<%@page import="com.protecmedia.iter.xmlio.model.Channel"%>
<%@page import="com.protecmedia.iter.xmlio.model.ChannelControl"%>
<%@page import="com.protecmedia.iter.xmlio.model.ChannelControlLog"%>
<%@page import="com.protecmedia.iter.xmlio.service.ChannelControlLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.xmlio.service.ChannelControlLogLocalServiceUtil"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.kernel.dao.search.RowChecker"%>
<%@page import="com.protecmedia.iter.xmlio.service.ChannelLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.protecmedia.iter.xmlio.service.util.XMLIOUtil"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.protecmedia.iter.xmlio.util.LiveListUtil"%>
<%@page import="com.liferay.portal.kernel.language.LanguageUtil"%>
<%@page import="java.util.ArrayList"%>

<%
long companyId = themeDisplay.getCompanyId();
long userId = themeDisplay.getUserId();
long globalGroupId = company.getGroup().getGroupId();
String environment = IterLocalServiceUtil.getEnvironment();
String redirect = ParamUtil.getString(request, "redirect");	
String tabs1 = ParamUtil.getString(request, "tabs1", "manual");	
long liveGroupId = ParamUtil.getLong(request, "liveGroupId", -1);	
if (liveGroupId == -1) liveGroupId = scopeGroupId;
String liveClassName = ParamUtil.getString(request, "liveClassName", "");
if (liveClassName.equals(IterKeys.XMLIO_CHANNEL_RANGE_TYPE_ALL)) liveClassName = "";
String liveLocalId = ParamUtil.getString(request, "liveLocalId", "");
String tabName = tabs1.equals(IterKeys.XMLIO_XML_MANUAL)? "xmlio-channel-control-manual" : "xmlio-channel-control-automatic";
%>
