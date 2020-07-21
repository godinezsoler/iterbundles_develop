<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@page import="com.liferay.portal.kernel.util.PropsUtil"%>
<%@page import="com.liferay.portal.kernel.util.PropsKeys"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletMgr"%>
<%@page import="com.liferay.portal.kernel.util.SectionUtil"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.kernel.comments.CommentsConfigBean"%>
<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="com.liferay.portlet.TeaserUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletPreferencesTools"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="java.util.Date"%>
<%@page import="java.util.Locale"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="javax.portlet.PortletPreferences"%>

<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.util.PortletKeys"%>
<%@page import="com.liferay.portal.model.Layout"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.theme.ThemeDisplay"%>
<%@page import="com.liferay.util.portlet.PortletRequestUtil"%>
<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>
<%@page import="com.liferay.portlet.journal.model.JournalArticle"%>
<%@page import="com.liferay.portal.service.LayoutLocalServiceUtil"%>
<%@page import="com.liferay.portal.service.PortletPreferencesLocalServiceUtil"%>
<%@page import="com.liferay.portal.service.CompanyLocalServiceUtil"%>

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
<%@page import="com.liferay.portal.kernel.util.IterURLUtil"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>

<%!
	private static Log _log = LogFactoryUtil.getLog("news-portlet.docroot.html.related-viewer-portlet.related_page.jsp");
%>

<%
	
	//Datos fijos
	ThemeDisplay themeDisplay = IterLocalServiceUtil.rebuildThemeDisplayForAjax(request);
	long scopeGroupId 	= themeDisplay.getScopeGroupId();
	long companyId		= themeDisplay.getCompanyId();
	String serverPort   = Integer.toString( themeDisplay.getServerPort() );
	String scheme 		= request.getScheme();
	
	Locale locale = request.getLocale();
		
	//Datos obtenidos de la peticion AJAX desde related-viewer/view.jsp 
	
	String portletId 		= ParamUtil.get(request, 		"portletId", 				"");
	String refPreferenceId	= ParamUtil.get(request, 		"refPreferenceId", 			"");
	String portletItem		= ParamUtil.get(request, 		IterKeys.PREFS_PORTLETITEM, "");
	String relatedBy 		= ParamUtil.get(request, 		"relatedBy", 				"");
	String sDate 			= ParamUtil.get(request, 		"date", 					"");
	String  contentId 		= ParamUtil.get(request, 		"contentId",				"");
	int firstItem 			= ParamUtil.get(request, 		"firstItem", 				1) - 1;
	int lastItem 			= ParamUtil.get(request, 		"lastItem", 				1) - 1;
	int globalFirstItem 	= ParamUtil.get(request, 		"globalFirstItem", 			1);
    int globalLastItem 		= ParamUtil.get(request, 		"globalLastItem", 			1);
    int globalLastIndex 	= ParamUtil.get(request, 		"globalLastIndex", 			1);
	int teasertotalcount 	= ParamUtil.get(request, 		"teasertotalcount", 		0);
	
	Date currentDate 		= TeaserContentUtil.getDateFromURLParam(sDate, GroupMgr.getPublicationDate(themeDisplay.getScopeGroupId()));
	long globalGroupId 		= CompanyLocalServiceUtil.getCompany(themeDisplay.getCompanyId()).getGroup().getGroupId();
	
	SectionUtil.setSectionPlid( PortalUtil.getOriginalServletRequest(request), ParamUtil.getLong(request, "sectionPlid", 0) );
	
	//XML Request
	String xmlRequest = PortletMgr.getXmlRequest(  themeDisplay, scheme, serverPort );
%>

<c:if test="<%=themeDisplay!=null%>">

<%=PHPUtil.getCheckAccessPHPCode(request, response, themeDisplay)%>

<%
	
	Layout layout = themeDisplay.getLayout();

	// Se obtienen a partir de la preferencia de referencia, sino está indicada una preferencia de referencia se intenta obtener la preferencia a partir del portletId
	PortletPreferences preferences = PortletPreferencesTools.getPreferences(request);
	
	//Mostrar
	int resultStart = GetterUtil.getInteger(preferences.getValue("resultStart", null), 0);
	int resultEnd = GetterUtil.getInteger(preferences.getValue("resultEnd", null), 1);
	String titleContents = preferences.getValue("titleContents", "");
	boolean paged = GetterUtil.getBoolean(preferences.getValue("paged", null), false);
	String paginationButtonsPosition = preferences.getValue("paginationButtonsPosition", "top");
	String paginationHtmlPosition = preferences.getValue("paginationHtmlPosition", "l-a-s");
	int teaserPerPage = GetterUtil.getInteger(preferences.getValue("teaserPerPage", null), 1);
	boolean showNonActiveContents = GetterUtil.getBoolean(preferences.getValue("showNonActiveContents", null), false);
	
	//Mostrar comentarios. Si el disyuntor está a false, no se añade al request el atributo, porque no se van a mostrar los comentarios.
	//Al cargar cualquier página esto se hace en LayoutAction.java, pero en el caso de la paginación hay que volver a hacerlo.
	String scopeGroupFriendlyName = themeDisplay.getScopeGroupFriendlyURL().replace(StringPool.SLASH, StringPool.PERIOD);
	boolean enabledDisqus = 
			GetterUtil.getBoolean(PropsUtil.get(
					String.format(PropsKeys.ITER_DISQUS_ENABLED_FOR_GROUPFRIENDLYURL, scopeGroupFriendlyName)), true);
	if(enabledDisqus)
	{
		CommentsConfigBean commentsConfig = new CommentsConfigBean(themeDisplay.getScopeGroupId(), request);
			request.setAttribute(WebKeys.REQUEST_ATTRIBUTE_COMMENTS_CONFIG_BEAN, commentsConfig);
	} 
	
	
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
	String[] layoutIds 		= PortletPreferencesTools.getLayoutIds(preferences, SectionUtil.getSectionPlid(request), defaultLayout);
	String[] ctxLayouts		= (sectionToShow != IterKeys.SECTION_TO_SHOW_SOURCE) ? null : layoutIds;
	themeDisplay.setLayoutIfNotExist(layoutIds);

%>

<%@ include file="related_page_details.jsp" %>

</c:if>