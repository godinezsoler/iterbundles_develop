<%@page import="java.net.URLEncoder"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="java.util.List"%>
<%@page import="javax.portlet.PortletPreferences"%>

<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>

<%@page import="com.protecmedia.iter.news.util.TopicsUtil"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%
	long globalGroupId = company.getGroup().getGroupId();
	String environment = IterLocalServiceUtil.getEnvironment();
	
	PortletPreferences preferences = renderRequest.getPreferences();
	String portletResource = ParamUtil.getString(request, "portletResource");
	if (Validator.isNotNull(portletResource)) 
		preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);
	
	String title						= preferences.getValue("title", StringPool.BLANK);
	long modelId 						= GetterUtil.getLong(preferences.getValue("modelId", null), -1);
	long milliseconds					= GetterUtil.getLong(preferences.getValue("milliseconds", null), 100);
	int numMetadata						= GetterUtil.getInteger(preferences.getValue("numMetadata", null), 5);
	int numCharacters  					= GetterUtil.getInteger(preferences.getValue("numCharacters", null), 2);
	boolean onlyMetadataLastLevel 		= GetterUtil.getBoolean(preferences.getValue("onlyMetadataLastLevel", null), false);
	
	List<String> contentVocabularyIds 	= TopicsUtil.getListPreference(preferences, "contentVocabularyIds");
	String contentVocabularyIdsJoin 	= StringPool.DASH;
	if(contentVocabularyIds != null && contentVocabularyIds.size() > 0)
		contentVocabularyIdsJoin = StringUtils.join(contentVocabularyIds.iterator(), StringPool.DASH);
	
	List<String> contentCategoryIds 	= TopicsUtil.getListPreference(preferences, "contentCategoryIds");
	String contentCategoryIdsJoin 		= StringPool.DASH;
	if(contentCategoryIds != null && contentCategoryIds.size() > 0)
		contentCategoryIdsJoin = StringUtils.join(contentCategoryIds.iterator(), StringPool.DASH);
	
	String contentType = URLEncoder.encode(GetterUtil.getString(
			PortalUtil.getPortalProperties().getProperty(
					IterKeys.PORTAL_PROPERTIES_KEY_ITER_METALOCATOR_RESPONSE_CONTENT_TYPE), "application/json").replace(
							StringPool.SLASH, StringPool.UNDERLINE), "UTF-8");
%>