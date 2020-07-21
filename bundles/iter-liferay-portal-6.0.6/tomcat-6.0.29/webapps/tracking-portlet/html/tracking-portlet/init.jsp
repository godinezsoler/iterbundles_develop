<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@page import="com.liferay.portal.kernel.util.PrefsPropsUtil"%>
<%@page import="com.liferay.portlet.PortalPreferences"%>
<%@page import="com.liferay.util.portlet.PortletProps"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil"%>
<%@page import="javax.portlet.PortletURL"%>
<%@page import="com.liferay.portal.kernel.dao.search.RowChecker"%>
<%@page import="com.liferay.portlet.journal.model.JournalArticle"%>
<%@page import="com.protecmedia.iter.news.service.CountersLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.news.model.Counters"%>
<%@page import="com.liferay.portlet.ratings.service.RatingsEntryLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.news.service.CommentsLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.news.model.Comments"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.liferay.portlet.ratings.service.RatingsStatsLocalServiceUtil"%>
<%@page import="com.liferay.portlet.ratings.model.RatingsStats"%>
<%@page import="com.protecmedia.iter.tracking.util.TrackingSearchObject"%>
<%@page import="com.liferay.portal.kernel.util.ListUtil"%> 
<%@page import="com.liferay.portal.kernel.dao.search.ResultRow"%>
<%@page import="com.liferay.portal.kernel.util.ArrayUtil"%>
<%@page import="java.util.Arrays"%>
<%@page import="com.protecmedia.iter.tracking.util.TrackingNameComparator"%>
<%@page import="java.util.Comparator"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.protecmedia.iter.tracking.util.TrackingTypeComparator"%>
<%@page import="com.protecmedia.iter.tracking.util.TrackingModerationComparator"%>
<%@page import="com.protecmedia.iter.tracking.util.TrackingCommentsComparator"%>
<%@page import="com.protecmedia.iter.tracking.util.TrackingSentComparator"%>
<%@page import="com.protecmedia.iter.tracking.util.TrackingViewsComparator"%>
<%@page import="com.protecmedia.iter.tracking.util.TrackingRatingComparator"%>
<%@page import="com.protecmedia.iter.tracking.util.TrackingVotingComparator"%>
<%@page import="com.liferay.portal.kernel.dao.orm.DynamicQuery"%>
<%@page import="com.liferay.portal.kernel.util.PortalClassLoaderUtil"%>
<%@page import="com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.workflow.WorkflowConstants"%>
<%@page import="com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.exception.PortalException"%>

<%@page import="com.protecmedia.iter.news.model.PageContent" %>
<%@page import="com.protecmedia.iter.news.service.PageContentLocalServiceUtil" %>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.news.service.QualificationLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.news.model.Qualification"%>

<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.util.HtmlUtil"%>
<%@page import="com.liferay.portal.model.LayoutConstants"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.service.LayoutLocalServiceUtil"%>
<%@page import="com.liferay.portal.model.Layout"%>
<%@page import="com.protecmedia.iter.designer.model.PageTemplate"%>
<%@page import="com.protecmedia.iter.designer.service.PageTemplateLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.LocaleUtil"%>
<%@page import="java.text.DateFormat"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="javax.portlet.PortletURL"%>
<%@page import="javax.portlet.RenderResponse"%>

<portlet:defineObjects />
<liferay-theme:defineObjects /> 

<%
	long globalGroupId = company.getGroup().getGroupId();

	
	// Listado de templates			
	List<Layout> seccionList = LayoutLocalServiceUtil.getLayouts(scopeGroupId, false, 0);
	
	String layoutId = ParamUtil.getString(request, "layout-id", "");
	
	if ("".equals(layoutId) && seccionList.size() > 0) {
		layoutId = seccionList.get(0).getUuid();
	}

%>

