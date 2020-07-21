
<%@page import="com.protecmedia.iter.base.service.util.PortletPreferencesTools"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@ page import="javax.portlet.PortletURL" %>
<%@ page import="javax.portlet.ActionRequest" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="javax.portlet.PortletPreferences" %>
<%@ page import="javax.portlet.PortletSession" %>
<%@	page import="javax.portlet.RenderResponse"%>

<%@ page import="java.util.ArrayList"%>
<%@ page import="java.util.List"%>

<%@ page import="com.liferay.portal.kernel.language.UnicodeLanguageUtil" %>
<%@ page import="com.liferay.portal.kernel.util.Constants" %>
<%@ page import="com.liferay.portal.kernel.util.ParamUtil" %>
<%@ page import="com.liferay.portal.kernel.util.StringPool" %>
<%@ page import="com.liferay.portal.kernel.util.Validator" %>
<%@ page import="com.liferay.portlet.PortletPreferencesFactoryUtil" %>
<%@ page import="com.liferay.portal.kernel.util.GetterUtil" %>
<%@ page import="com.liferay.portal.kernel.xml.Document"%>
<%@ page import="com.liferay.portal.model.Layout"%>
<%@ page import="com.liferay.portal.model.Image"%>
<%@ page import="com.liferay.portal.service.ImageLocalServiceUtil"%>
<%@ page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@ page import="com.liferay.portal.service.LayoutLocalServiceUtil"%>
<%@ page import="com.liferay.portal.theme.ThemeDisplay"%>
<%@ page import="com.liferay.portal.kernel.util.ListUtil"%>
<%@ page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@ page import="com.liferay.portal.kernel.xml.DocumentException"%>
<%@ page import="com.liferay.portal.kernel.exception.SystemException"%>
<%@ page import="com.liferay.portal.kernel.exception.PortalException"%>
<%@ page import="com.liferay.portal.kernel.servlet.SessionErrors"%>
<%@ page import="com.liferay.portal.kernel.util.KeyValuePair"%>
<%@ page import="com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil"%>
<%@ page import="com.liferay.portlet.journal.model.JournalArticle"%>
<%@ page import="com.liferay.portal.util.PortalUtil"%>
<%@ page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@ page import="com.liferay.portal.kernel.util.KeyValuePair"%>
<%@ page import="com.liferay.portal.kernel.xml.Element"%>

<%@ page import="org.w3c.dom.NodeList"%>
<%@ page import="org.w3c.dom.Node"%>

<%@ page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@ page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@ page import="com.protecmedia.iter.news.service.PageContentLocalServiceUtil"%>
<%@ page import="com.liferay.portal.kernel.util.MetadataControlUtil"%>
<%@ page import="com.protecmedia.iter.news.util.MetadataValidator"%>
<%@ page import="com.protecmedia.iter.news.util.TopicsUtil"%>
<%@ page import="com.protecmedia.iter.news.util.CategoryMetadataControlUtil"%>
<%@ page import="com.liferay.portal.kernel.util.IMetadataControlUtil"%>
<%@ page import="com.protecmedia.iter.news.util.TopicsUtil"%>
<%@ page import="com.protecmedia.iter.news.util.RobotsControlUtil"%>

<liferay-theme:defineObjects />
<portlet:defineObjects />

<%
	PortletPreferences preferences = PortletPreferencesTools.getPortletPreferences(renderRequest, request);
	String environment = IterLocalServiceUtil.getEnvironment();
	
	String textHTML = preferences.getValue("textHTML", "");
	String registeredUserCode = preferences.getValue("registeredUserCode", "");
	String unregisteredUserCode = preferences.getValue("unregisteredUserCode", "");
	String codeType = preferences.getValue("codeType", "uniqueCode");
	
	
%>


