<%@page import="com.liferay.portal.kernel.util.CatalogUtil"%>
<%@page import="com.liferay.portal.kernel.util.request.IterRequest"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.kernel.util.PropsValues"%>
<%@page import="java.util.Map"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.util.WidgetUtil"%>
<%@page import="com.liferay.portal.kernel.util.CatalogSSIMgr"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicRequestParamsTools"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.xml.XMLHelper"%>
<%@page import="com.liferay.portal.kernel.xml.Document"%>
<%@page import="com.liferay.portal.kernel.xml.SAXReaderUtil"%>
<%@page import="com.liferay.portal.kernel.xml.SAXReader"%>
<%@page import="javax.portlet.PortletPreferences"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletPreferencesTools"%>
<%@ include file="initCatalog.jsp" %>


<%
	String html = CatalogUtil.getCatalogPortletHTML(request, portletPreferencesValues);
%>

<%= html %>


