<%--
*Copyright (c) 2012 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="java.util.Arrays"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Date"%>
<%@page import="javax.portlet.PortletPreferences"%>
<%@page import="javax.portlet.PortletURL"%>

<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.servlet.SessionErrors"%> 
<%@page import="com.liferay.portal.kernel.servlet.SessionMessages"%>

<%@page import="com.protecmedia.iter.base.service.util.GroupMgr"%>
<%@page import="com.protecmedia.iter.news.util.TopicsUtil"%>
<%@page import="com.protecmedia.iter.news.util.TeaserContentUtil"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>

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
	
	List<String> contentTypes = TopicsUtil.getListPreference(preferences, "contentTypes");
	List<String> layoutIds = TopicsUtil.getListPreference(preferences, "layoutIds");
	List<String> contentVocabularyIds = TopicsUtil.getListPreference(preferences, "contentVocabularyIds");
	List<String> contentCategoryIds = TopicsUtil.getListPreference(preferences, "contentCategoryIds");
	List<String> stickyCategoryIds = TopicsUtil.getListPreference(preferences, "stickyCategoryIds");
	List<String> excludeCategoryIds = TopicsUtil.getListPreference(preferences, "excludeCategoryIds");
	
	String title = preferences.getValue("title", null);
	List<String> qualificationId = TopicsUtil.getListPreference(preferences, "qualificationId");
	String orderDirection = preferences.getValue("orderDirection", "ASC");
	
	int orderType = GetterUtil.getInteger(preferences.getValue("orderType", null), 0);
	int numTopics = GetterUtil.getInteger(preferences.getValue("numTopics", null), 0);
	
	long modelId = GetterUtil.getLong(preferences.getValue("modelId", null), 0);
	
	//Texto por defecto
	String defaultTextHTML = preferences.getValue("defaultTextHTML", StringPool.BLANK);
	boolean showDefaultTextHTML = GetterUtil.getBoolean(preferences.getValue("showDefaultTextHTML", null), false);
	
%>
