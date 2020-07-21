
<%@page import="com.protecmedia.iter.user.util.NewsletterPortletMgr"%>
<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%@page import="com.protecmedia.iter.base.service.util.CKEditorUtil"%>
<%@page import="com.liferay.portal.kernel.xml.XSLUtil"%>
<%@page import="com.liferay.portal.kernel.xml.Document"%>
<%@ include file="init.jsp" %>

<%@page import="com.liferay.portal.kernel.json.JSONObject" %>
<%@page import="com.liferay.portal.kernel.json.JSONFactoryUtil" %>

<%@page import="com.liferay.portal.kernel.xml.Attribute" %>
<%@page import="com.liferay.portal.kernel.xml.Document" %>
<%@page import="com.liferay.portal.kernel.xml.DocumentException" %>
<%@page import="com.liferay.portal.kernel.xml.Element" %>
<%@page import="com.liferay.portal.kernel.xml.Node" %>
<%@page import="com.liferay.portal.kernel.xml.SAXReaderUtil" %>
<%@page import="com.liferay.portal.kernel.xml.XMLHelper" %>
<%@page import="com.liferay.portal.kernel.xml.XPath" %>

<%
String result = portletMgr.getNewsletterPortletCode(request, response, scopeGroupId);
_log.debug(result);
%>

<%= result %>
<%= NewsletterPortletMgr.NEWSLETTER_TOGGLE_CODE %>
