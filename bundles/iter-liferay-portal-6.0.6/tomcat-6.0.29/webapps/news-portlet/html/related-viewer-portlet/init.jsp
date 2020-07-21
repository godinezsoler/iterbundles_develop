<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.kernel.util.SectionUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletPreferencesTools"%>
<%@page import="com.liferay.portlet.TeaserUtil"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="java.util.Date"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="javax.portlet.PortletPreferences"%>

<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portlet.journal.model.JournalArticle"%>
<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>
<%@page import="com.liferay.util.portlet.PortletRequestUtil"%>

<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.GroupMgr"%>
<%@page import="com.protecmedia.iter.news.service.PageContentLocalServiceUtil"%>
<%@page import="com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.news.util.TopicsUtil"%>
<%@page import="com.protecmedia.iter.news.util.TeaserContentUtil"%>
<%@page import="com.protecmedia.iter.news.util.ThemeDisplayCacheUtil"%>
<%@page import="com.protecmedia.iter.news.model.PageContent"%>
<%@page import="com.liferay.portal.kernel.velocity.VelocityContext"%>
<%@page import="com.liferay.portlet.AdvertisementUtil"%>
<%@ page import="com.liferay.portal.service.LayoutLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.IterURLUtil"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%

	HttpServletRequest originalRequest = PortalUtil.getOriginalServletRequest(request);

	//Globales
	long globalGroupId = company.getGroup().getGroupId();
	long companyId = company.getCompanyId();
	String environment = IterLocalServiceUtil.getEnvironment();
	
	PortletPreferences preferences = renderRequest.getPreferences();
	String portletResource = ParamUtil.getString(request, "portletResource");
	if (Validator.isNotNull(portletResource))
		preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);
	
	//Mostrar
	int resultStart = GetterUtil.getInteger(preferences.getValue("resultStart", null), 0);
	int resultEnd = GetterUtil.getInteger(preferences.getValue("resultEnd", null), 1);
	String relatedBy = preferences.getValue("relatedBy", "metadata");
	String titleContents = preferences.getValue("titleContents", "");
	boolean paged = GetterUtil.getBoolean(preferences.getValue("paged", null), false);
	String paginationButtonsPosition = preferences.getValue("paginationButtonsPosition", "top");
	String paginationHtmlPosition = preferences.getValue("paginationHtmlPosition", "l-a-s");
	int teaserPerPage = GetterUtil.getInteger(preferences.getValue("teaserPerPage", null), 1);
	boolean showNonActiveContents = GetterUtil.getBoolean(preferences.getValue("showNonActiveContents", null), false);
	
	
	String buttonPrev = GetterUtil.getString(preferences.getValue("buttonPrev", null),"Retroceder");		
	String buttonNext = GetterUtil.getString(preferences.getValue("buttonNext", null),"Avanzar");	
	String buttonShowMore = GetterUtil.getString(preferences.getValue("buttonShowMore", null),"Mostrar más");
	boolean showMore = GetterUtil.getBoolean(preferences.getValue("showMore", null), false);
	
	//Modelo
	boolean defaultMode = GetterUtil.getBoolean(preferences.getValue("defaultMode", null), true);	
	long modelId = GetterUtil.getLong(preferences.getValue("modelId", null), -1);

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
	boolean articleFilter = GetterUtil.getBoolean(preferences.getValue("articleFilter", null), false);
	boolean galleryFilter = GetterUtil.getBoolean(preferences.getValue("galleryFilter", null), false);
	boolean pollFilter = GetterUtil.getBoolean(preferences.getValue("pollFilter", null), false);
	boolean multimediaFilter = GetterUtil.getBoolean(preferences.getValue("multimediaFilter", null), false);
	
	List<String> structures = new ArrayList<String>();
	
	if(articleFilter)
		structures.add(IterKeys.STRUCTURE_ARTICLE);

	if(galleryFilter)
		structures.add(IterKeys.STRUCTURE_GALLERY);
	
	if(pollFilter)
		structures.add(IterKeys.STRUCTURE_POLL);

	if(multimediaFilter)
		structures.add(IterKeys.STRUCTURE_MULTIMEDIA);
	
	//Ordenación
	String[] orderBy = preferences.getValues("orderBy", new String[0]);	
	int orderByType = GetterUtil.getInteger(preferences.getValue("orderByType", null), IterKeys.ORDER_ASC);
	
	//Categorías
	List<String> contentVocabularyIds = TopicsUtil.getListPreference(preferences, "contentVocabularyIds");
	List<String> contentCategoryIds = TopicsUtil.getListPreference(preferences, "contentCategoryIds");
	
	//Páginas
	boolean defaultLayout 	= PortletPreferencesTools.getDefaultLayout(preferences);
	int sectionToShow 		= PortletPreferencesTools.getSectionToShow(preferences);
	long sectionPlid 		= SectionUtil.getSectionPlid(request);
	String[] layoutIds 		= PortletPreferencesTools.getLayoutIds(preferences, sectionPlid, defaultLayout);
	String[] ctxLayouts		= (sectionToShow != IterKeys.SECTION_TO_SHOW_SOURCE) ? null : layoutIds;
	
	//Texto por defecto
	String defaultTextHTML = preferences.getValue("defaultTextHTML", StringPool.BLANK);
	boolean showDefaultTextHTML = GetterUtil.getBoolean(preferences.getValue("showDefaultTextHTML", null), false);
%>
