<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="com.protecmedia.iter.base.service.util.PortletPreferencesTools"%>

<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>

<%@ page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@ page import="javax.portlet.PortletPreferences" %>
<%@ page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%@ page import="com.liferay.portal.service.PortalLocalServiceUtil"%>
<%@ page import="com.liferay.portal.kernel.util.StringPool" %>
<%@ page import="org.apache.commons.lang.StringUtils"%>

<%@ page import="com.protecmedia.iter.news.util.ExternalServiceUtil" %>


<liferay-theme:defineObjects />
<portlet:defineObjects />

<%! private static Log _logVelocityContainer = LogFactoryUtil.getLog("news-portlet.docroot.html.proxy-portlet.proxy-view.jsp"); %>

<%
	String environment = IterLocalServiceUtil.getEnvironment();

	PortletPreferences preferences = PortletPreferencesTools.getPortletPreferences(renderRequest, request);
	String serviceId       = preferences.getValue("service", null);
	
	String htmlContent = StringPool.BLANK;
	
	if (Validator.isNotNull(serviceId))
	{
	
%>
		<%@ include file="proxy-view-print-content.jsp" %>
<%
	}
	else if (IterKeys.ENVIRONMENT_PREVIEW.equals(environment))
	{
%>
		<div class="portlet-msg-info">
			<span class="displaying-help-message-tpl-holder">
				<liferay-ui:message key="please-contact-with-your-administrator-to-configure-this-portlet" />
			</span>
		</div>
<%
	}
%>
