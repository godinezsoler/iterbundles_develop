<%@page import="com.liferay.portal.kernel.servlet.HttpHeaders"%>
<%@page import="com.liferay.portal.kernel.xml.Element"%>
<%@page import="com.liferay.portal.kernel.xml.SAXReaderUtil"%>
<%@page import="com.liferay.portal.kernel.xml.Document"%>
<%@page import="com.liferay.portal.kernel.xml.Node"%>
<%@page import="com.protecmedia.iter.user.util.LoginUtil"%>
<%@page import="com.liferay.portal.kernel.util.HtmlUtil"%>
<%@page import="com.liferay.portal.service.LayoutLocalServiceUtil"%>
<%@page import="com.liferay.portal.model.Layout"%>
<%@page import="com.liferay.portal.model.Company"%>

<%@page import="java.util.List"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="javax.portlet.PortletPreferences"%>
<%@page import="com.liferay.portal.model.CompanyConstants" %>

<%@page import="com.liferay.portal.kernel.dao.search.RowChecker"%>
<%@page import="java.util.Calendar"%>

<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.liferay.portal.kernel.language.LanguageUtil"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%

	String environment = IterLocalServiceUtil.getEnvironment();
	String usrAgent = request.getHeader(HttpHeaders.USER_AGENT);
	LoginUtil lu = new LoginUtil();
	
	if(themeDisplay!=null)
		lu.getLoginConfig(themeDisplay.getScopeGroupId());

//  mode = 1 popup
//  mode = 2 link

%>
