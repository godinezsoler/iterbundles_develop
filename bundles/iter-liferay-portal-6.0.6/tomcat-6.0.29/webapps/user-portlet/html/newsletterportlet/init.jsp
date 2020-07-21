<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.protecmedia.iter.user.util.NewsletterPortletMgr"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="javax.portlet.PortletPreferences"%>

<%@page import="com.liferay.portal.kernel.util.StringPool" %>
<%@page import="com.liferay.portal.kernel.util.StringUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil" %>
<%@page import="com.liferay.portal.kernel.util.PrefsPropsUtil" %>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>

<%@page import="com.liferay.portal.kernel.bean.BeanParamUtil" %>
<%@page import="com.liferay.portal.kernel.util.Constants" %>

<%@page import="com.liferay.portal.kernel.portlet.LiferayWindowState"%>
<%@page import="javax.portlet.WindowState"%>
<%@ page import="javax.portlet.PortletURL" %>

<%@page import="com.protecmedia.iter.user.util.NewsletterUtil"%>

<%@page import="com.protecmedia.iter.user.util.UserUtil"%>
<%@page import="com.liferay.portal.service.UserGroupLocalServiceUtil"%>
<%@page import="com.liferay.portal.model.UserGroup"%>

<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.liferay.portal.service.UserLocalServiceUtil"%>

<%@page import="com.liferay.portal.kernel.util.GroupConfigTools"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%!
private static Log _log = LogFactoryUtil.getLog("user-portlet.docroot.html.newsletterportlet.init.jsp");
%>

<%
//Busca las preferencias de la configuración global de newsletters
String preferences = GroupConfigTools.getGroupConfigField(scopeGroupId, "newsletterconfig");

NewsletterPortletMgr portletMgr = null;
//Si hay configuración global, obtiene el código HTML
if (Validator.isNotNull(preferences))
{
	portletMgr = new NewsletterPortletMgr(preferences);
}
//Si está vacía, coge las preferencias del portlet
else
{
	PortletPreferences portletPrefs = renderRequest.getPreferences();
	
	long globalGroupId = company.getGroup().getGroupId();	
	long companyId = PortalUtil.getCompanyId(request);
	
	String portletResource = ParamUtil.getString(request, "portletResource");
	
	if (Validator.isNotNull(portletResource)) {
		portletPrefs = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);
	}
	
	portletMgr = new NewsletterPortletMgr(portletPrefs);
}
%>
