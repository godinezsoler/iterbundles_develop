<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.Collection"%>
<%@page import="java.util.Locale"%>
<%@page import="java.text.DateFormat"%>
<%@page import="java.text.SimpleDateFormat"%>

<%@page import="com.liferay.portal.kernel.util.ListUtil"%>
<%@page import="com.liferay.portal.kernel.util.LocaleUtil"%>
<%@page import="com.liferay.portlet.journal.model.JournalArticle"%>
<%@page import="com.liferay.portal.kernel.util.Constants"%>
<%@page import="com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil"%>
<%@page import="com.liferay.portal.service.LayoutLocalServiceUtil"%>
<%@page import="com.liferay.portal.model.Layout"%>
<%@page import="com.liferay.portal.kernel.dao.search.ResultRow"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.security.permission.ActionKeys"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>

<%@page import="com.protecmedia.iter.news.model.PageContent" %>
<%@page import="com.protecmedia.iter.news.service.PageContentLocalServiceUtil" %>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.news.util.MyContentChecker"%>
<%@page import="com.protecmedia.iter.news.util.PageContentChecker"%>
<%@page import="com.protecmedia.iter.news.service.util.MyDynamicQueryUtil"%>
<%@page import="com.protecmedia.iter.news.service.QualificationLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.news.model.Qualification"%>

<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.util.HtmlUtil"%>
<%@page import="com.liferay.portal.model.LayoutConstants"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>

<%@page import="javax.portlet.PortletURL"%>
<%@page import="javax.portlet.RenderResponse"%>

<%@page import="com.liferay.portal.kernel.dao.search.RowChecker"%>
<%@page import="com.liferay.portlet.journal.model.JournalTemplate"%>
<%@page import="com.liferay.portlet.journal.service.persistence.JournalArticleUtil"%>
<%@page import="com.liferay.portlet.journal.service.JournalTemplateLocalServiceUtil"%>

<%@page import="com.liferay.portlet.imagegallery.service.IGFolderLocalServiceUtil"%>
<%@page import="com.liferay.portlet.imagegallery.model.IGFolder"%>
<%@page import="com.liferay.portlet.imagegallery.service.IGImageServiceUtil"%>

<%@page import="com.protecmedia.iter.designer.model.PageTemplate"%>
<%@page import="com.protecmedia.iter.designer.service.PageTemplateLocalServiceUtil"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%	
	long globalGroupId = company.getGroup().getGroupId();

	DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm");

	boolean standardArticleCheck = ParamUtil.getBoolean(renderRequest, "standardArticleCheck", true);
	boolean standardGalleryCheck = ParamUtil.getBoolean(renderRequest, "standardGalleryCheck", true);
	boolean standardPollCheck = ParamUtil.getBoolean(renderRequest, "standardPollCheck", true);
	boolean standardMultimediaCheck = ParamUtil.getBoolean(renderRequest, "standardMultimediaCheck", true);
	String keyword = ParamUtil.getString(renderRequest, "keyword", "");
	String orderByCol = ParamUtil.getString(renderRequest, "orderByCol", "title");	
	String orderByType = ParamUtil.getString(renderRequest, "orderByType", "asc");
	long contentGroupId = ParamUtil.getLong(renderRequest, "contentGroupId", themeDisplay.getCompanyGroupId());
		
	List<Object> structures = new ArrayList<Object>();
	
	// Listado de templates			
	List<Layout> seccionList = LayoutLocalServiceUtil.getLayouts(scopeGroupId, false, 0);
	
	// Listado de calificaciones
	List<Qualification> qualifications = QualificationLocalServiceUtil.getQualifications(scopeGroupId);
	
	String layoutId = ParamUtil.getString(request, "layout-id", "");
	
	if ("".equals(layoutId) && seccionList.size() > 0) {
		layoutId = seccionList.get(0).getUuid();
	}

%>
