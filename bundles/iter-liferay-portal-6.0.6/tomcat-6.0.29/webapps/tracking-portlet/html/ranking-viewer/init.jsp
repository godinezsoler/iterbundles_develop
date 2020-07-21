<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.util.SectionUtil"%>
<%@page import="com.liferay.portlet.TeaserUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletPreferencesTools"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="org.apache.commons.lang.StringEscapeUtils"%>

<%@page import="java.util.Arrays"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Date"%>
<%@page import="javax.portlet.PortletPreferences"%>

<%@page import="com.liferay.util.portlet.PortletRequestUtil"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>

<%@page import="com.protecmedia.iter.tracking.util.AnalayzerConstants"%>
<%@page import="com.protecmedia.iter.tracking.util.ThemeDisplayCacheUtil"%>
<%@page import="com.protecmedia.iter.tracking.util.RankingUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.liferay.portal.kernel.velocity.VelocityContext"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletPreferencesTools"%>
<%@page import="java.io.InputStreamReader"%>
<%@page import="java.io.BufferedReader"%>
<%@page import="java.net.HttpURLConnection"%>
<%@page import="java.net.URLConnection"%>
<%@page import="java.net.URL"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletMgr"%>
<%@page import="com.liferay.portal.service.PortletPreferencesLocalServiceUtil"%>
<%@page import="com.liferay.portal.model.Group"%>
<%@page import="com.liferay.portal.service.GroupLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%
	//Globales
	long globalGroupId = company.getGroup().getGroupId();
	long companyId = company.getCompanyId();
	
	PortletPreferences preferences = PortletPreferencesTools.getPortletPreferences(renderRequest, request);

	//Mostrar
	int numberOfResults = GetterUtil.getInteger(preferences.getValue("number-of-results", null), 1);
	String titleContents = preferences.getValue("titleContents", "");
	String modifiedDateRangeTimeValue = GetterUtil.getString(preferences.getValue("modifiedDateRangeTimeValue", null), "");
	String modifiedDateRangeTimeUnit = GetterUtil.getString(preferences.getValue("modifiedDateRangeTimeUnit", null), "");
	String[] tabsIds = preferences.getValues("tabs-ids", new String[] {String.valueOf(AnalayzerConstants.TABRECENT)});
	if(tabsIds != null && tabsIds.length > 0)
	{
		List<String> newTabsIds = new ArrayList<String>();
		for(int i = 0; i < tabsIds.length; i++)
		{
			String item = tabsIds[i];
			if(!newTabsIds.contains(item) && AnalayzerConstants.isIn(item))
			{
				newTabsIds.add(item);
			}
		}
		tabsIds = newTabsIds.toArray(new String[newTabsIds.size()]);
	}
	
	//Calificación
	String[] qualificationId =preferences.getValues("qualificationId", null);
	
	//Modelo
	boolean defaultMode = GetterUtil.getBoolean(preferences.getValue("defaultMode", null), true);
	long modelId = GetterUtil.getLong(preferences.getValue("modelId", null), -1);

	//Página
	boolean defaultLayout 	= PortletPreferencesTools.getDefaultLayout(preferences);
	int sectionToShow 		= PortletPreferencesTools.getSectionToShow(preferences);
	long sectionPlid 		= SectionUtil.getSectionPlid(request);
	String[] layoutIds 		= PortletPreferencesTools.getLayoutIds(preferences, sectionPlid, defaultLayout);

	//Categorías
	long[] contentCategoryIdsLong = PortletPreferencesTools.getContentCategoriesIds(preferences);

	//Plantillas
	String templateIdArticle = GetterUtil.getString(preferences.getValue("templateIdArticle", null), "");
	String templateIdGallery = GetterUtil.getString(preferences.getValue("templateIdGallery", null), "");
	String templateIdPoll = GetterUtil.getString(preferences.getValue("templateIdPoll", null), "");
	String templateIdMultimedia = GetterUtil.getString(preferences.getValue("templateIdMultimedia", null), "");
	
	String templateIdArticleRestricted = GetterUtil.getString(preferences.getValue("templateIdArticleRestricted", null), "");
	String templateIdGalleryRestricted = GetterUtil.getString(preferences.getValue("templateIdGalleryRestricted", null), "");
	String templateIdPollRestricted = GetterUtil.getString(preferences.getValue("templateIdPollRestricted", null), "");
	String templateIdMultimediaRestricted = GetterUtil.getString(preferences.getValue("templateIdMultimediaRestricted", null), "");
	
	//Modos de plantillas
	int modeArticle = GetterUtil.getInteger(preferences.getValue("modeArticle", null), -1);
	int modeGallery = GetterUtil.getInteger(preferences.getValue("modeGallery", null), -1);
	int modePoll = GetterUtil.getInteger(preferences.getValue("modePoll", null), -1);
	int modeMultimedia = GetterUtil.getInteger(preferences.getValue("modeMultimedia", null), -1);
	
	//Estructuras
	String structureId = GetterUtil.getString(preferences.getValue("structureId", null), "");
	List<String> structures = new ArrayList<String>();
	if(Validator.isNotNull(structureId))
		structures.add(structureId);
	
	String portletItem		= GetterUtil.getString(preferences.getValue(IterKeys.PREFS_PORTLETITEM, null), "");
	
	//Literales
	String mostRecentLabel = GetterUtil.getString(preferences.getValue("mostRecentLabel", null), "");
	if (mostRecentLabel.equals(""))
		mostRecentLabel = "ranking-viewer-view-most-recent";
	String mostViewedLabel = GetterUtil.getString(preferences.getValue("mostViewedLabel", null), "");
	if (mostViewedLabel.equals(""))
		mostViewedLabel = "ranking-viewer-view-most-viewed";
	String mostRatedLabel = GetterUtil.getString(preferences.getValue("mostRatedLabel", null), "");
	if (mostRatedLabel.equals(""))
		mostRatedLabel = "ranking-viewer-view-top-rated";
	String mostCommentedLabel = GetterUtil.getString(preferences.getValue("mostCommentedLabel", null), "");
	if (mostCommentedLabel.equals(""))
		mostCommentedLabel = "ranking-viewer-view-most-comment";
	String mostSharedLabel = GetterUtil.getString(preferences.getValue("mostSharedLabel", null), "");
	if (mostSharedLabel.equals(""))
		mostSharedLabel = "ranking-viewer-view-most-shared";
	
	//Texto por defecto
	String defaultTextHTML = preferences.getValue("defaultTextHTML", StringPool.BLANK);
	boolean showDefaultTextHTML = GetterUtil.getBoolean(preferences.getValue("showDefaultTextHTML", null), false);
	
%>
