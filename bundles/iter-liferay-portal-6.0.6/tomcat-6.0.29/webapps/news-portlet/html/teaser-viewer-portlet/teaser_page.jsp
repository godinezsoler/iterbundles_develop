<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@page import="com.liferay.portal.kernel.util.Base64"%>
<%@page import="com.liferay.portal.kernel.util.UnrepeatableArticlesMgr"%>
<%@page import="com.liferay.portal.kernel.util.request.IterRequest"%>
<%@page import="com.liferay.portal.kernel.util.PropsUtil"%>
<%@page import="com.liferay.portal.kernel.util.PropsKeys"%>
<%@page import="com.liferay.portal.kernel.util.QualificationTools"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletMgr"%>
<%@page import="com.liferay.portal.kernel.util.ArrayUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.util.SectionUtil"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.kernel.comments.CommentsConfigBean"%>
<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletPreferencesTools"%>
<%@page import="com.liferay.portal.kernel.xml.SAXReaderUtil"%>
<%@page import="com.liferay.portal.kernel.xml.Element"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.Locale"%>
<%@page import="java.math.RoundingMode"%>
<%@page import="java.text.DecimalFormat"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="javax.portlet.PortletPreferences"%>

<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringUtil"%>
<%@page import="com.liferay.portal.theme.ThemeDisplay"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.util.PortletKeys"%>

<%@page import="com.liferay.portal.model.Layout"%>
<%@page import="com.liferay.portal.service.CompanyLocalServiceUtil"%>
<%@page import="com.liferay.portlet.TeaserUtil"%>
<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>
<%@page import="com.liferay.portlet.journal.model.JournalArticle"%>
<%@page import="com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil"%>
<%@page import="com.liferay.portal.service.PortletPreferencesLocalServiceUtil"%>
<%@page import="com.liferay.portal.service.LayoutLocalServiceUtil"%>
<%@page import="com.liferay.util.portlet.PortletRequestUtil"%>

<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.GroupMgr"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.news.util.TopicsUtil"%>
<%@page import="com.protecmedia.iter.news.util.TeaserContentUtil"%>
<%@page import="com.protecmedia.iter.news.util.ThemeDisplayCacheUtil"%>
<%@page import="com.protecmedia.iter.news.service.PageContentLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.news.model.PageContent"%>
<%@page import="com.liferay.portal.kernel.velocity.VelocityContext"%>
<%@page import="com.liferay.portal.kernel.util.IterURLUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.SQLQueries"%>
<%@page import="com.liferay.portal.kernel.util.PropsValues"%>

<%
	//Datos fijos
	ThemeDisplay themeDisplay = IterLocalServiceUtil.rebuildThemeDisplayForAjax(request);
	long scopeGroupId 	= themeDisplay.getScopeGroupId();
	long companyId		= themeDisplay.getCompanyId();
	
	String environment = IterLocalServiceUtil.getEnvironment();	
	Locale locale = request.getLocale();

	//Datos obtenidos de la peticion AJAX desde teaser-viewer/view.jsp 
	
	String serverPort 				= ParamUtil.get(request, 		"serverPort", 				"");
	String scheme 					= ParamUtil.get(request, 		"scheme", 					"");
	String portletId 				= ParamUtil.get(request, 		"portletId", 				"");
	String refPreferenceId			= ParamUtil.get(request, 		"refPreferenceId", 			"");
	String portletItem				= ParamUtil.get(request, 		IterKeys.PREFS_PORTLETITEM, "");
	int firstItem 					= ParamUtil.get(request, 		"firstItem", 				1) - 1;
	int lastItem 					= ParamUtil.get(request, 		"lastItem",					1) - 1;
	int globalFirstItem 			= ParamUtil.get(request, 		"globalFirstItem", 			1);
    int globalLastItem 				= ParamUtil.get(request, 		"globalLastItem", 			1);
    int globalLastIndex 			= ParamUtil.get(request, 		"globalLastIndex", 			1);
	String contentId 				= ParamUtil.get(request, 		"contentId", 				"");
	String sCategoryIds 			= ParamUtil.get(request, 		"categoryIds", 				"");
	String sDate 					= ParamUtil.get(request, 		"date", 					"");
	long teasertotalcount 			= ParamUtil.getLong(request, 	"teasertotalcount", 		0);
	boolean includeCurrentContent 	= ParamUtil.getBoolean(request, "includeCurrentContent");
	String filterBy 				= ParamUtil.get(request, 		"filterBy", 				"");
	String filterOpt 				= ParamUtil.get(request, 		"filterOpt",	 			"");
	
	
	boolean activeUnrepeatable 		= ParamUtil.getBoolean(request, "activeUnrepeatable");
	if (activeUnrepeatable)
	{
		String unrepeatableArticles = new String( Base64.decode( ParamUtil.get(request, "unrepeatableArticles", "") ) );
		IterRequest.setAttribute(WebKeys.REQUEST_UNREPEATABLE_ARTICLES, new StringBuilder(unrepeatableArticles));
		UnrepeatableArticlesMgr.active( Validator.isNull(portletId) ? PortletKeys.PORTLET_TEASERVIEWERNR : portletId );
	}
	
	long[] categoryIds 				= TeaserContentUtil.getCategoryIdsFromURLParam(sCategoryIds, new long[]{});
	Date date 						= TeaserContentUtil.getDateFromURLParam(sDate, GroupMgr.getPublicationDate(themeDisplay.getScopeGroupId()));
	long globalGroupId 				= CompanyLocalServiceUtil.getCompany(themeDisplay.getCompanyId()).getGroup().getGroupId();
	
	
	SectionUtil.setSectionPlid( PortalUtil.getOriginalServletRequest(request), ParamUtil.getLong(request, "sectionPlid", 0) );

	//XML Request
	String xmlRequest = PortletMgr.getXmlRequest(  themeDisplay, scheme, serverPort );
%>

<c:if test="<%=themeDisplay!=null%>">

<%=PHPUtil.getCheckAccessPHPCode(request, response, themeDisplay)%>

<%
	// Se obtienen las preferencias, sino está indicada una preferencia de referencia se intenta obtener la preferencia a partir del portletId
	
	Layout layout = themeDisplay.getLayout();
	
	PortletPreferences preferences = PortletPreferencesTools.getPreferences(request);
	
	//Título
	String titleContents = preferences.getValue("titleContents", "");
	
	//Rango de resultados
	int resultStart = GetterUtil.getInteger(preferences.getValue("resultStart", null), 0);
	int resultEnd = GetterUtil.getInteger(preferences.getValue("resultEnd", null), 1);
	
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
	String[] layoutIds 		= PortletPreferencesTools.getLayoutIds(preferences, SectionUtil.getSectionPlid(request), defaultLayout);
	String[] ctxLayouts		= (sectionToShow != IterKeys.SECTION_TO_SHOW_SOURCE) ? null : layoutIds;
	int[] layoutLimits		= PortletPreferencesTools.getLayoutLimits(preferences);
	themeDisplay.setLayoutIfNotExist(layoutIds);
	
	// Mostrar caducados.
	// Si estamos en el preview o se indica en el properties que se quiere usar el flag, se toma su valor de la preferencia.
	// Si estamos en el live y no se indica en el properties que se quiere usar el flag, toma el valor false.
	// http://jira.protecmedia.com:8080/browse/ITER-1144?focusedCommentId=51484&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-51484
	// En el LIVE si es una página de metadatos sí se permiten contenidos caducados
	boolean showNonActiveContents = PropsValues.IS_PREVIEW_ENVIRONMENT || 
									(SectionUtil.getURLType(request).equals(SectionUtil.URLTYPE_META) || PropsValues.ITER_TEASER_ENABLE_NON_ACTIVE_CONTENT) ?
			                        GetterUtil.getBoolean(preferences.getValue("showNonActiveContents", null), false) :
			                        false;
	boolean showExampleArticle = GetterUtil.getBoolean(preferences.getValue("showExampleArticle", null), true);
	
	//Mostrar comentarios. Si el disyuntor está a false, no se añade al request el atributo, porque no se van a mostrar los comentarios.
	//Al cargar cualquier página esto se hace en LayoutAction.java, pero en el caso de la paginación y del filtro hay que volver a hacerlo.
	
	String scopeGroupFriendlyName = themeDisplay.getScopeGroupFriendlyURL().replace(StringPool.SLASH, StringPool.PERIOD);
	boolean enabledDisqus = 
			GetterUtil.getBoolean(PropsUtil.get(
				String.format(PropsKeys.ITER_DISQUS_ENABLED_FOR_GROUPFRIENDLYURL, scopeGroupFriendlyName)), true);
	if(enabledDisqus)
	{
		CommentsConfigBean commentsConfig = new CommentsConfigBean(themeDisplay.getScopeGroupId(), request);
		request.setAttribute(WebKeys.REQUEST_ATTRIBUTE_COMMENTS_CONFIG_BEAN, commentsConfig);
	} 
	
	
	//Paginación
	boolean paged = true;
	String distribution = preferences.getValue("distribution", "vertical");
	int teaserPerPage = GetterUtil.getInteger(preferences.getValue("teaserPerPage", null), 1);
	String paginationHtmlPosition = preferences.getValue("paginationHtmlPosition", "l-a-s");
	String paginationButtonsPosition = preferences.getValue("paginationButtonsPosition", "top");
	
	//Modos de plantillas
	int modeArticle = GetterUtil.getInteger(preferences.getValue("modeArticle", null), -1);
	int modeGallery = GetterUtil.getInteger(preferences.getValue("modeGallery", null), -1);
	int modePoll = GetterUtil.getInteger(preferences.getValue("modePoll", null), -1);
	int modeMultimedia = GetterUtil.getInteger(preferences.getValue("modeMultimedia", null), -1);
	
	int modeArticleRelated = GetterUtil.getInteger(preferences.getValue("modeArticleRelated", null), -1);
	int modeGalleryRelated = GetterUtil.getInteger(preferences.getValue("modeGalleryRelated", null), -1);
	int modePollRelated = GetterUtil.getInteger(preferences.getValue("modePollRelated", null), -1);
	int modeMultimediaRelated = GetterUtil.getInteger(preferences.getValue("modeMultimediaRelated", null), -1);
	
	
	
	String mainCssClass = "noticias";
	boolean isNewsletterPage = false;
	boolean isHorizontal = distribution.equals("horizontal") ? true : false;
	
	List<String[]> listResult = null;
	List<String[]> listResultPage = null;
	
	//fecha seleccionada en filtro por fecha
	Date filterOptDate  = new Date();
	
	
	
	if( !filterBy.equals(StringPool.BLANK) )   
	{
		//filtro aplicado
		
		//cálculo de secciones y categorías 
		long[] categoryFilter = null;
		long[] categoryConf = categoryIds;
		//true cuando hay que añadir claúsula 'and' a la query con la categoría seleccionada para filtrar
		boolean addCategoryfilter = false;
			
		
		String[] layoutFilter = null;
		String[] layoutConf = layoutIds;
		//true cuando hay que añadir claúsula 'and' a la query con la sección seleccionada para filtrar
		boolean addlayoutfilter = false;
		
		if( !filterBy.equals("reset_filter" ) ) 
		{
			if( filterBy.equalsIgnoreCase(IterKeys.CATEGORIES) )
			{
				categoryFilter = TeaserContentUtil.getCategoryIdsFromURLParam(filterOpt, new long[]{});
				
				boolean categoryFilterIncludeInCategoryconf = true;
				for( int i= 0; i < categoryFilter.length && categoryFilterIncludeInCategoryconf && categoryConf.length >0 ; i++ )
				{
					categoryFilterIncludeInCategoryconf = ArrayUtil.contains( categoryConf, categoryFilter[i] );
				}
			
				if( categoryConf.length == 0 || categoryFilterIncludeInCategoryconf )
					categoryIds = categoryFilter;
				else
					addCategoryfilter = true;
				
			}
			else if(  filterBy.equalsIgnoreCase( IterKeys.SECTIONS ) )
			{
				layoutFilter = new String[]{filterOpt};
				
				boolean layoutFilterIncludeInlayoutconf = true;
				for( int i= 0; i < layoutFilter.length && layoutFilterIncludeInlayoutconf && layoutConf.length >0 ; i++ )
				{
					layoutFilterIncludeInlayoutconf = ArrayUtil.contains( layoutConf, layoutFilter[i] );
				}
			
				if( layoutConf.length == 0 || layoutFilterIncludeInlayoutconf )
				{
					layoutIds	= layoutFilter;
					ctxLayouts	= (sectionToShow != IterKeys.SECTION_TO_SHOW_SOURCE) ? null : layoutIds;
				}
				else
					addlayoutfilter  = true;

			}
		}
	
		//////////////////////////////////////////////////////////////////////////////////////////////////
		//cálculo de listResultPage
		
		if( filterBy.equalsIgnoreCase( IterKeys.DATE )   )
		{
			//en el caso en que el filtro sea por fecha se llama a getFilterArticles con 20 parámetros
			//el último parámetro es la fecha por la que hay que filtrar 'filterOptDate'
			
			//conversión de filterOpt de string a Date
			SimpleDateFormat dateformat = new SimpleDateFormat( IterKeys.DATEFORMAT_YYYY_MM_DD );
			filterOptDate = dateformat.parse( filterOpt );
			
			listResultPage = TeaserContentUtil.getFilterArticles( scopeGroupId, contentId, structures, globalFirstItem, 
					globalLastItem, date, showNonActiveContents, orderBy, 
					orderByType, categoryIds, null, qualificationId, 
					layoutIds, includeCurrentContent, firstItem + "," + (lastItem-firstItem), null, null, null, layoutFilter, filterOptDate, layoutLimits  );
		}
		else if( !addCategoryfilter && !addlayoutfilter   )
		{
			listResultPage = TeaserContentUtil.getFilterArticles(themeDisplay.getScopeGroupId(), contentId, structures, globalFirstItem, 
					globalLastItem, date, showNonActiveContents, orderBy, 
					orderByType, categoryIds, null, qualificationId, 
					layoutIds, includeCurrentContent, firstItem + "," + (lastItem-firstItem), layoutLimits);
		}
		else
		{
				if( addCategoryfilter )  
				{
					
					listResultPage = TeaserContentUtil.getFilterArticles( scopeGroupId, contentId, structures, globalFirstItem, 
							globalLastItem, date, showNonActiveContents, orderBy, 
							orderByType, categoryIds, null, qualificationId, 
							layoutIds, includeCurrentContent, firstItem + "," + (lastItem-firstItem), null, null, categoryFilter, null, layoutLimits );
				}
				else if(  addlayoutfilter    )
				{
					listResultPage = TeaserContentUtil.getFilterArticles( scopeGroupId, contentId, structures, globalFirstItem, 
							globalLastItem, date, showNonActiveContents, orderBy, 
							orderByType, categoryIds, null, qualificationId, 
							layoutIds, includeCurrentContent, firstItem + "," + (lastItem-firstItem), null, null, null, layoutFilter, layoutLimits );
				}
		}	
	}
	else
	{
		//sin filtro
		listResultPage = TeaserContentUtil.getFilterArticles(themeDisplay.getScopeGroupId(), contentId, structures, globalFirstItem, 
				globalLastItem, date, showNonActiveContents, orderBy, 
				orderByType, categoryIds, null, qualificationId, 
				layoutIds, includeCurrentContent, firstItem + "," + (lastItem-firstItem), layoutLimits);
	}
	
	if (activeUnrepeatable)
	{
		UnrepeatableArticlesMgr.add(listResultPage, Validator.isNull(portletId) ? PortletKeys.PORTLET_TEASERVIEWERNR : portletId);
	}
	
	//se pide el qualificationName para que la jsp que se encargue de pintar el resultado, lo inserte en el request
	String qualifId = qualificationId != null ? qualificationId[0]  :  "";
	String qualificationName = QualificationTools.getQualificationName( qualifId );
	
%>

<%@ include file="teaser_page_details.jsp" %>

</c:if>