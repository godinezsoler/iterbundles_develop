<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%@page import="com.protecmedia.iter.news.util.PaywallUtil"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.protecmedia.iter.news.util.PaywallUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.util.GroupMgr"%>
<%@page import="com.liferay.portal.kernel.velocity.VelocityEngineUtil"%>
<%@page import="com.liferay.portal.kernel.util.IterTemplateContent"%>
<%@page import="com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil"%>
<%@page import="com.liferay.portlet.journal.model.JournalArticle"%>
<%@page import="com.liferay.portal.kernel.xml.XMLHelper"%>
<%@page import="com.liferay.portal.kernel.velocity.VelocityContext"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.kernel.xml.Node"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.kernel.xml.SAXReaderUtil"%>
<%@page import="com.liferay.portal.kernel.xml.Document"%>
<%@page import="com.protecmedia.iter.news.service.ProductLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletPreferencesTools"%>
<%@page import="javax.portlet.PortletPreferences"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>

<portlet:defineObjects />
<%
	if( PHPUtil.isApacheRequest(request) )
	{
		PublicIterParams.set(WebKeys.ITER_RESPONSE_NEEDS_PHP, true);
	}
	// Obtiene el entorno
	String environment = IterLocalServiceUtil.getEnvironment();
	
	// Carga las preferencias del portlet
	PortletPreferences preferences = PortletPreferencesTools.getPortletPreferences(renderRequest, request);

	// Recupera la plantilla a aplicar
	String templateId = preferences.getValue("templateId", null);
	
	// Recupera la página de error en el pago
	String errorLayout = preferences.getValue("layoutIds", null);
	
	if (Validator.isNotNull(templateId) && Validator.isNotNull(errorLayout))
	{
%>
        <%=PaywallUtil.renderProducts(request, templateId, errorLayout) %>
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