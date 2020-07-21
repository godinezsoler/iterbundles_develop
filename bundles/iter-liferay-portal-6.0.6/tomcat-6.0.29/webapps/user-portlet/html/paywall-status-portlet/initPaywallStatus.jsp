<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="javax.portlet.PortletPreferences"%>

<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>

<%@page import="com.protecmedia.iter.base.service.util.PaywallUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.CKEditorUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.PayCookieUtil"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%

PortletPreferences preferences = renderRequest.getPreferences();

long globalGroupId = company.getGroup().getGroupId();	
long companyId = PortalUtil.getCompanyId(request);

String portletResource = ParamUtil.getString(request, "portletResource");
String environment = IterLocalServiceUtil.getEnvironment();

if(Validator.isNotNull(portletResource))
	preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);

String title			= preferences.getValue("title", 			StringPool.BLANK);
String cookieName		= preferences.getValue("cookieName", 		StringPool.BLANK);
String dateName			= preferences.getValue("dateName", 			StringPool.BLANK);
String userAgentName	= preferences.getValue("userAgentName", 	StringPool.BLANK);
String ipName			= preferences.getValue("ipName", 			StringPool.BLANK);
String productName		= preferences.getValue("productName", 		StringPool.BLANK);
String noSignedMsg		= preferences.getValue("noSignedMsg", 		StringPool.BLANK);
String articleName		= preferences.getValue("articleName", 		StringPool.BLANK);
String expiresDateName	= preferences.getValue("expiresDateName", 	StringPool.BLANK);
int viewMode			= GetterUtil.getInteger(preferences.getValue("viewMode", String.valueOf(PaywallUtil.SESSION_TYPE)));
int daysBefore			= GetterUtil.getInteger(preferences.getValue("daysBefore", String.valueOf(PaywallUtil.DEFAULT_DAYS_BEFORE)));

%>
