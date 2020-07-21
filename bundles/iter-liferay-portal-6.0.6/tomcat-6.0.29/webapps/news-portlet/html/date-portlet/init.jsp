<%--
*Copyright (c) 2012 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="com.sun.jndi.toolkit.url.UrlUtil"%>
<%@page import="sun.misc.URLClassPath"%>
<%@page import="com.sun.org.omg.SendingContext.CodeBasePackage.URLHelper"%>

<%@page import="java.util.Set"%>
<%@page import="java.util.HashSet"%>
<%@page import="com.liferay.portal.model.Group"%>
<%@page import="com.liferay.portal.model.Layout"%>
<%@page import="com.liferay.portal.kernel.util.KeyValuePair"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.liferay.portal.service.GroupLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.HtmlUtil"%>
<%@page import="java.util.Arrays"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.liferay.portal.kernel.util.ListUtil"%>
<%@page import="com.liferay.portal.kernel.util.KeyValuePairComparator"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.util.StringUtil"%>
<%@page import="com.liferay.portal.kernel.language.LanguageUtil"%>
<%@page import="com.liferay.portal.kernel.servlet.SessionErrors"%>
<%@page import="com.protecmedia.iter.news.model.Qualification"%>

<%@page import="com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.dao.orm.DynamicQuery"%>
<%@page import="com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.util.PortalClassLoaderUtil"%>
<%@page import="com.protecmedia.iter.designer.model.PageTemplate"%>
<%@page import="com.protecmedia.iter.designer.service.PageTemplateLocalServiceUtil"%>

<%@page import="com.liferay.portal.kernel.util.LocaleUtil"%>
<%@page import="java.util.Locale"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>

<%@page import="java.util.List"%>
<%@page import="java.util.Date"%>
<%@page import="javax.portlet.PortletPreferences"%>
<%@page import="javax.portlet.PortletURL"%>

<%@page import="com.protecmedia.iter.base.service.util.GroupMgr"%>
<%@page import="com.protecmedia.iter.news.util.TopicsUtil"%>
<%@page import="com.protecmedia.iter.news.util.TeaserContentUtil"%>
<%@page import="com.protecmedia.iter.news.util.DateUtil"%>

<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.servlet.SessionErrors"%> 
<%@page import="com.liferay.portal.kernel.servlet.SessionMessages"%>

<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletMgr"%>
<%@page import="com.protecmedia.iter.news.util.DatePortletPreferences"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%

	long globalGroupId = company.getGroup().getGroupId();
	String environment = IterLocalServiceUtil.getEnvironment();
	
	PortletPreferences preferences = renderRequest.getPreferences();
	String portletResource = ParamUtil.getString(request, "portletResource");
	if (Validator.isNotNull(portletResource)) 
	{
		preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);
	}

	Date currentDate = GroupMgr.getPublicationDate(scopeGroupId);
	
	String textBefore = preferences.getValue("textBefore", "");
	String textAfter = preferences.getValue("textAfter", "");
	String dateWithoutHourFormat = preferences.getValue("dateWithoutHourFormat", "");
	String hourFormat = preferences.getValue("hourFormat", "");

	if( textBefore.isEmpty() && 
		textAfter.isEmpty() && 
		dateWithoutHourFormat.isEmpty() && 
		hourFormat.isEmpty() )
	{
		DatePortletPreferences serverPrefs = DateUtil.fillPreferencesPortlet(scopeGroupId);
		textBefore = serverPrefs.getTextBefore();
		textAfter = serverPrefs.getTextAfter();
		dateWithoutHourFormat = serverPrefs.getDateWithoutHourFormat();
		hourFormat = serverPrefs.getHourFormat();
	}
	
%>
