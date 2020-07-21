<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet"%>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme"%>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui"%>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util"%>

<%@page import="javax.portlet.PortletPreferences"%>

<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.comments.CommentsConfigBean"%>
<%@page import="com.liferay.portal.service.PortalLocalServiceUtil"%>
<%@page import="com.liferay.portlet.TeaserUtil"%>
<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>

<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletMgr"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%

PortletPreferences preferences = renderRequest.getPreferences();

String portletResource = ParamUtil.getString(request, "portletResource");
	
if (Validator.isNotNull(portletResource))
	preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);


// OJO, esta propiedad era un boolean, y ahora es un string con una opción.
// Si viene un true o vacio es como si viniera el valor "always".
// Si viene un false es como si viniera el valor "access".

String showDisqus 		= preferences.getValue("show", "");
if(showDisqus.equals("") || showDisqus.equals("true"))
	showDisqus = "always";
else if( showDisqus.equals("false") )
	showDisqus = "access";

String[] subscriptions = preferences.getValues("subscriptions4show", null);
String[] subscriptions4notShow = preferences.getValues("subscriptions4notShow", null);
%>
