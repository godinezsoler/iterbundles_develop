<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@page import="com.liferay.portal.kernel.util.request.IterRequest"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%@page import="com.liferay.portal.kernel.util.SectionUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletPreferencesTools"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="java.util.Arrays" %>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Date"%>
<%@page import="java.math.RoundingMode"%>
<%@page import="java.text.DecimalFormat"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="javax.portlet.PortletPreferences"%>

<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringUtil"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portlet.TeaserUtil"%>
<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>
<%@page import="com.liferay.portlet.journal.model.JournalArticle"%>
<%@page import="com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil"%>
<%@page import="com.liferay.util.portlet.PortletRequestUtil"%>

<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.GroupMgr"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.base.service.util.FormatDateUtil"%>
<%@page import="com.protecmedia.iter.news.util.TopicsUtil"%>
<%@page import="com.protecmedia.iter.news.util.TeaserContentUtil"%>
<%@page import="com.protecmedia.iter.news.util.ThemeDisplayCacheUtil"%>
<%@page import="com.protecmedia.iter.news.service.PageContentLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.news.model.PageContent"%>
<%@page import="com.liferay.portal.kernel.velocity.VelocityContext"%>
<%@page import="com.liferay.portal.service.PortalLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletMgr"%>
<%@page import="com.liferay.portlet.AdvertisementUtil"%>
<%@ page import="com.liferay.portal.service.LayoutLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.liferay.portal.kernel.util.IterURLUtil"%>
<%@page import="com.liferay.portal.kernel.util.PropsValues"%>


<portlet:defineObjects />
<liferay-theme:defineObjects />

<%
final String TOP = "top";
final String BOTH = "both";
final String BOTTOM = "bottom";

	HttpServletRequest originalRequest = PortalUtil.getOriginalServletRequest(request);
	
	//Globales
	long companyId = company.getCompanyId();
	long globalGroupId = company.getGroup().getGroupId();	
	String environment = IterLocalServiceUtil.getEnvironment();	

	PortletPreferences preferences = PortletPreferencesTools.getPortletPreferences(renderRequest, request);
	
	//Excluir de la lista el propio articulo (Sólo en el caso de la vista de detalle)
	boolean includeCurrentContent = GetterUtil.getBoolean(preferences.getValue("listCurrentContent", null), false);
	String mainSection = "";
	if( Validator.isNotNull(originalRequest.getAttribute(WebKeys.MAIN_SECTION)) )
		mainSection = originalRequest.getAttribute(WebKeys.MAIN_SECTION).toString();
	
	if (mainSection.equalsIgnoreCase("true"))
		includeCurrentContent = true;

	String portletItem	 = GetterUtil.getString(preferences.getValue(IterKeys.PREFS_PORTLETITEM, null), "");
	
	String refPreferenceId 	= "";
	String portletId		= "";
	
	if (portletItem.isEmpty())
	{
		refPreferenceId = GetterUtil.getString(preferences.getValue("refPreference", null), "");
		
		if (refPreferenceId.isEmpty())
			portletId = PortalUtil.getPortletId(request);
	}

	
	//Título
	String titleContents = preferences.getValue("titleContents", "");
	
	//Calificación
	String[] qualificationId = preferences.getValues("qualificationId", null);
	
	//Relacionados
	/* boolean showInternalLink = GetterUtil.getBoolean(preferences.getValue("showInternalLink", null), false);
	boolean showExternalLink = GetterUtil.getBoolean(preferences.getValue("showExternalLink", null), false); */
	boolean showInternalExternalLink = GetterUtil.getBoolean(preferences.getValue("showInternalExternalLink", null), false);
	boolean showMetadata = GetterUtil.getBoolean(preferences.getValue("showMetadata", null), false);
	/* int maxItemsInternalLinks = GetterUtil.getInteger(preferences.getValue("maxItemsInternalLinks", null), 0);
	int maxItemsExternalLinks = GetterUtil.getInteger(preferences.getValue("maxItemsExternalLinks", null), 0); */
	int maxItemsInternalExternalLinks = GetterUtil.getInteger(preferences.getValue("maxItemsInternalExternalLinks", null), 0);
	int maxItemsMetadata = GetterUtil.getInteger(preferences.getValue("maxItemsMetadata", null), 0);
	String metadataPos =  GetterUtil.getString(preferences.getValue("metadataPos", null), IterKeys.BEFORE);
	
	boolean showRelatedContent = ((showInternalExternalLink && maxItemsInternalExternalLinks > 0) || 
			 					  (showMetadata && maxItemsMetadata>0));
	
	//Relacionados accesibles
	String templateIdRelatedArticle = GetterUtil.getString(preferences.getValue("templateIdRelatedArticle", null), "");
	String templateIdRelatedGallery = GetterUtil.getString(preferences.getValue("templateIdRelatedGallery", null), "");
	String templateIdRelatedPoll = GetterUtil.getString(preferences.getValue("templateIdRelatedPoll", null), "");
	String templateIdRelatedMultimedia = GetterUtil.getString(preferences.getValue("templateIdRelatedMultimedia", null), "");

	boolean articleRelatedFilter = GetterUtil.getBoolean(preferences.getValue("articleRelatedFilter", null), false);
	boolean galleryRelatedFilter = GetterUtil.getBoolean(preferences.getValue("galleryRelatedFilter", null), false);
	boolean pollRelatedFilter = GetterUtil.getBoolean(preferences.getValue("pollRelatedFilter", null), false);
	boolean multimediaRelatedFilter = GetterUtil.getBoolean(preferences.getValue("multimediaRelatedFilter", null), false);

	List<String> structuresRelated = new ArrayList<String>();
	
	if(articleRelatedFilter)
		structuresRelated.add(IterKeys.STRUCTURE_ARTICLE);
	
	if(galleryRelatedFilter)
		structuresRelated.add(IterKeys.STRUCTURE_GALLERY);

	if(pollRelatedFilter)
		structuresRelated.add(IterKeys.STRUCTURE_POLL);

	if(multimediaRelatedFilter)
		structuresRelated.add(IterKeys.STRUCTURE_MULTIMEDIA);
	
	//Relacionados restringidos
	String templateIdRelatedArticleRestricted = GetterUtil.getString(preferences.getValue("templateIdRelatedArticleRestricted", null), "");
	String templateIdRelatedGalleryRestricted = GetterUtil.getString(preferences.getValue("templateIdRelatedGalleryRestricted", null), "");
	String templateIdRelatedPollRestricted = GetterUtil.getString(preferences.getValue("templateIdRelatedPollRestricted", null), "");
	String templateIdRelatedMultimediaRestricted = GetterUtil.getString(preferences.getValue("templateIdRelatedMultimediaRestricted", null), "");
	
	//Principales accesibles
	String templateIdArticle = GetterUtil.getString(preferences.getValue("templateIdArticle", null), "");
	String templateIdGallery = GetterUtil.getString(preferences.getValue("templateIdGallery", null), "");
	String templateIdPoll = GetterUtil.getString(preferences.getValue("templateIdPoll", null), "");
	String templateIdMultimedia = GetterUtil.getString(preferences.getValue("templateIdMultimedia", null), "");
	
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
	
	//Principales restringidos
	String templateIdArticleRestricted = GetterUtil.getString(preferences.getValue("templateIdArticleRestricted", null), "");
	String templateIdGalleryRestricted = GetterUtil.getString(preferences.getValue("templateIdGalleryRestricted", null), "");
	String templateIdPollRestricted = GetterUtil.getString(preferences.getValue("templateIdPollRestricted", null), "");
	String templateIdMultimediaRestricted = GetterUtil.getString(preferences.getValue("templateIdMultimediaRestricted", null), "");

	//Modelo
	boolean defaultMode = GetterUtil.getBoolean(preferences.getValue("defaultMode", null), true);	
	long modelId = GetterUtil.getLong(preferences.getValue("modelId", null), -1);
	
	//Ordenación
	String[] orderBy = preferences.getValues("orderBy", new String[0]);
	int orderByType = GetterUtil.getInteger(preferences.getValue("orderByType", null), IterKeys.ORDER_ASC);
	
	//Páginas
	boolean defaultLayout 	= PortletPreferencesTools.getDefaultLayout(preferences);
	int sectionToShow 		= PortletPreferencesTools.getSectionToShow(preferences);
	long sectionPlid 		= SectionUtil.getSectionPlid(request);
	String[] layoutIds 		= PortletPreferencesTools.getLayoutIds(preferences, sectionPlid, defaultLayout);
	String[] ctxLayouts		= (sectionToShow != IterKeys.SECTION_TO_SHOW_SOURCE) ? null : layoutIds;
	int[] layoutLimits		= PortletPreferencesTools.getLayoutLimits(preferences);
	
	//Categorías
	int categoryOperation = GetterUtil.getInteger(preferences.getValue("categoryOperation", null), -1);
	long[] contentCategoryIdsLong = PortletPreferencesTools.getContentCategoriesIds(preferences);
	
	// Mostrar caducados.
	// Si estamos en el preview o se indica en el properties que se quiere usar el flag, se toma su valor de la preferencia.
	// Si estamos en el live y no se indica en el properties que se quiere usar el flag, toma el valor false.
	// http://jira.protecmedia.com:8080/browse/ITER-1144?focusedCommentId=51484&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-51484
	// En el LIVE si es una página de metadatos sí se permiten contenidos caducados
	boolean showNonActiveContents = PropsValues.IS_PREVIEW_ENVIRONMENT || 
									(SectionUtil.getURLType(request).equals(SectionUtil.URLTYPE_META) || PropsValues.ITER_TEASER_ENABLE_NON_ACTIVE_CONTENT) ?
			                        GetterUtil.getBoolean(preferences.getValue("showNonActiveContents", null), false) :
			                        false;
	boolean showExampleArticle = GetterUtil.getBoolean(preferences.getValue("showExampleArticle", null), false);
	
	//Paginación
	boolean paged = GetterUtil.getBoolean(preferences.getValue("paged", null), false);
	String distribution = preferences.getValue("distribution", "vertical");
	int teaserPerPage = GetterUtil.getInteger(preferences.getValue("teaserPerPage", null), 1);
	String paginationHtmlPosition = GetterUtil.getString(preferences.getValue("paginationHtmlPosition", "l-a-s"), "l-a-s");
	String paginationButtonsPosition = GetterUtil.getString(preferences.getValue("paginationButtonsPosition", BOTTOM),BOTTOM);
	String buttonPrev = GetterUtil.getString(preferences.getValue("buttonPrev", null),"Retroceder");		
	String buttonNext = GetterUtil.getString(preferences.getValue("buttonNext", null),"Avanzar");	
	String buttonShowMore = GetterUtil.getString(preferences.getValue("buttonShowMore", null),"Mostrar más");
	boolean showMore = GetterUtil.getBoolean(preferences.getValue("showMore", null), false);
	
	//Rango de resultados
	int resultStart = GetterUtil.getInteger(preferences.getValue("resultStart", null), 0);
	int resultEnd = GetterUtil.getInteger(preferences.getValue("resultEnd", null), 1);
	
	//Modos de plantillas
	int modeArticle = GetterUtil.getInteger(preferences.getValue("modeArticle", null), -1);
	int modeGallery = GetterUtil.getInteger(preferences.getValue("modeGallery", null), -1);
	int modePoll = GetterUtil.getInteger(preferences.getValue("modePoll", null), -1);
	int modeMultimedia = GetterUtil.getInteger(preferences.getValue("modeMultimedia", null), -1);
	
	//Modos de plantillas
	int modeArticleRelated = GetterUtil.getInteger(preferences.getValue("modeArticleRelated", null), -1);
	int modeGalleryRelated = GetterUtil.getInteger(preferences.getValue("modeGalleryRelated", null), -1);
	int modePollRelated = GetterUtil.getInteger(preferences.getValue("modePollRelated", null), -1);
	int modeMultimediaRelated = GetterUtil.getInteger(preferences.getValue("modeMultimediaRelated", null), -1);
	
	PublicIterParams.set(originalRequest, TeaserUtil.REQUEST_ATTRIBUTE_TEASER_IN_PAGE, "true");
	List<String> portletsCssEnabled = Arrays.asList(GetterUtil.getString(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_ENABLE_CSS_PORTLETS), "").split(","));
	
	String mainCssClass = "noticias";
	
	boolean isNewsletterPage = IterRequest.isNewsletterPage();
	
	boolean isHorizontal = distribution.equals("horizontal") ? true : false;
	
	boolean useCreationDateFromHeader = GetterUtil.getBoolean(preferences.getValue("useCreationDateFromHeader", null), false);
	boolean canDiscardResponse = GetterUtil.getBoolean(preferences.getValue("canDiscardResponse", null), false);
	
	Date lastPublication = null;
	if(useCreationDateFromHeader)
	{
		String lastPublicationParam = String.valueOf( PublicIterParams.get(WebKeys.REQUEST_HEADER_PUB_DATE_BEFORE_CURRENT) );
		if( Validator.isNotNull(lastPublicationParam) )
			lastPublication = new Date(Long.parseLong(lastPublicationParam));
	}
	
	//Filtro para usuarios
	String usrFilterBy = preferences.getValue("filterBy", "withoutFilter");
	if( usrFilterBy.equalsIgnoreCase( IterKeys.DATE )  )
		PublicIterParams.set(WebKeys.REQUEST_ATTRIBUTE_INCLUDE_DP_FILTER_JS, true);
	String usrFilterPos = preferences.getValue("ctrlPos", null);
	String usrFilterDateLanguage = preferences.getValue("dateLanguage", null);
	String usrFilterDateFotmat = preferences.getValue("dateFormat", "dd/MM/yyyy");
	String usrFilterRangeType = preferences.getValue("rangeType", "withoutLimit");
	String usrFilterMinDate = preferences.getValue("minDate", null);
	String usrFilterBackward = preferences.getValue("backward", "0d");
	String usrFilterTextByLayout = preferences.getValue("textlayoutSelector", null);
	String usrFilterTextByCat = preferences.getValue("textCategorySelector", null);
	String usrFilterTextByDate = preferences.getValue("textDateSelector", StringPool.BLANK);
	String[] usrFilterLayouts = preferences.getValues("layoutIdsFilter", null);
				//metadatos
	String[] usrFilterCategories = preferences.getValues("categoryIdsFilter", null);
	String[] usrFilterVocabularies = preferences.getValues("vocabularyIdsFilter", null);
	
	String displayOption = preferences.getValue("displayOptions", null);
	
	//Texto por defecto
	String defaultTextHTML = preferences.getValue("defaultTextHTML", StringPool.BLANK);
	boolean showDefaultTextHTML = GetterUtil.getBoolean(preferences.getValue("showDefaultTextHTML", null), false);
	
	
%>
