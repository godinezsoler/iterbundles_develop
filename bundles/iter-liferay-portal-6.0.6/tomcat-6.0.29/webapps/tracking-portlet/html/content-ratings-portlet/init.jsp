<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="javax.portlet.PortletPreferences"%>

<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.theme.ThemeDisplay"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.language.LanguageUtil"%>
<%@page import="com.liferay.portal.kernel.servlet.HttpHeaders"%>
<%@page import="com.liferay.portal.service.PortalLocalServiceUtil"%>
<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>

<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
  
<portlet:defineObjects />
<liferay-theme:defineObjects />

<%

	long globalGroupId = company.getGroup().getGroupId();
	long companyId = company.getCompanyId();
	
	PortletPreferences preferences = renderRequest.getPreferences();
	
	String portletResource = ParamUtil.getString(request, "portletResource");
	
	if (Validator.isNotNull(portletResource))
		preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);
	
	String thanksMessage = GetterUtil.getString(preferences.getValue("thanksMessage", null), "");
	if (thanksMessage.isEmpty())
		thanksMessage = LanguageUtil.get(pageContext, "ratings-thanks-for-your-voting");
				
	String alreadyVoted = GetterUtil.getString(preferences.getValue("alreadyVoted", null), "");
	if (alreadyVoted.isEmpty())
		alreadyVoted = LanguageUtil.get(pageContext, "ratings-you-have-already-voted");
					
	String deactivatedCookies = GetterUtil.getString(preferences.getValue("deactivatedCookies", null), "");
	if (deactivatedCookies.isEmpty())
		deactivatedCookies = LanguageUtil.get(pageContext, "ratings-cannot-vote-this-content");
				
	boolean allowAnonymousVote 	= GetterUtil.getBoolean(preferences.getValue("showAnonymous", null), false);
	String  show = preferences.getValue("show", "always");
		
%>