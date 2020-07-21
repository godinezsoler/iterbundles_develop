<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" 	 prefix="liferay-theme" %>

<%@ page import="javax.portlet.PortletPreferences" %>

<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>

<%@ page import="com.liferay.portal.util.PortalUtil"%>
<%@ page import="com.liferay.portal.kernel.util.ParamUtil" %>
<%@ page import="com.liferay.portal.kernel.util.Validator" %>
<%@ page import="com.liferay.portal.kernel.util.StringPool" %>
<%@ page import="com.liferay.portal.kernel.servlet.HttpHeaders"%>
<%@ page import="com.liferay.portlet.PortletPreferencesFactoryUtil" %>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%
	PortletPreferences preferences = renderRequest.getPreferences();
	String portletResource = ParamUtil.getString(request, "portletResource");
	if (Validator.isNotNull(portletResource))
		preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);

	String usrAgent 				= 	request.getHeader(HttpHeaders.USER_AGENT);
	String panelTitle	 			= 	preferences.getValue("panelTitle", 				StringPool.BLANK);
	String unregisterButton 		= 	preferences.getValue("unregisterButton", 		StringPool.BLANK);
	String unregisterConfirmButton	= 	preferences.getValue("unregisterConfirmButton", StringPool.BLANK);
	String enterPasswordText		= 	preferences.getValue("enterPasswordText", 		StringPool.BLANK);
	String confirmPasswordText		= 	preferences.getValue("confirmPasswordText", 	StringPool.BLANK);
	String unregisterSuccessMsg		= 	preferences.getValue("unregisterSuccessMsg", 	StringPool.BLANK);
	String unregisterErrorMsg		= 	preferences.getValue("unregisterErrorMsg", 		StringPool.BLANK);
	String badCredentialsMsg		= 	preferences.getValue("badCredentialsMsg", 		StringPool.BLANK);
	String okDialogButton			= 	preferences.getValue("okDialogButton", 			StringPool.BLANK);
%>