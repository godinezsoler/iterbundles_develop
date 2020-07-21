<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@page import="com.liferay.portal.kernel.util.PropsUtil"%>
<%@page import="com.liferay.portal.kernel.util.PropsKeys"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.util.QualificationTools"%>
<%@page import="com.protecmedia.iter.base.service.util.SQLQueries"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletMgr"%>
<%@page import="com.liferay.portal.kernel.util.ArrayUtil"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
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
<%@page import="com.liferay.portal.kernel.util.PropsValues"%>



<%
	//Datos fijos
	ThemeDisplay themeDisplay = IterLocalServiceUtil.rebuildThemeDisplayForAjax(request);
	long scopeGroupId 	= themeDisplay.getScopeGroupId();
	long companyId		= themeDisplay.getCompanyId();
	
	String environment = IterLocalServiceUtil.getEnvironment();	
	Locale locale = request.getLocale();

	//Datos obtenidos de la peticion AJAX desde teaser_user_filter.jspf
	
	String serverPort 				= ParamUtil.get(request, 		IterKeys.SERVERPORT, 		"");
	String scheme 					= ParamUtil.get(request, 		IterKeys.SCHEME, 			"");
	String portletId 				= ParamUtil.get(request, 		IterKeys.PORTLETID, 		"");
	String refPreferenceId			= ParamUtil.get(request, 		IterKeys.REFPREFERENCEID, 	"");
	String portletItem				= ParamUtil.get(request, 		IterKeys.PREFS_PORTLETITEM, "");
	String contentId 				= ParamUtil.get(request, 		IterKeys.CONTENTID, 		"");
	String sCategoryIds 			= ParamUtil.get(request, 		IterKeys.CATEGORYIDS, 		"");
	String sDate 					= ParamUtil.get(request, 		IterKeys.DATE, 				"");
	int teasertotalcount 			= ParamUtil.get(request, 		IterKeys.TEASERTOTALCOUNT, 	 0);
	boolean includeCurrentContent 	= ParamUtil.getBoolean(request, IterKeys.INCLUDECURRENTCONTENT);
	long scopeGroupIdAjax			= ParamUtil.getLong(request, 	IterKeys.SCOPEGROUPID);
	long companyIdAjax				= ParamUtil.getLong(request, 	IterKeys.COMPANYID);
			
	//responseNamespace a partir de ahora será un uuid único y no el portletId, para poder calcularlo en esta jsp y que no tenga que viajar en la petición ajax
	String responseNamespace  =  ("_").concat(SQLQueries.getUUID()).concat("_");
	
	String filterBy 				= ParamUtil.get(request,	IterKeys.FILTER_TYPE,	IterKeys.WITHOUT_FILTER);
	String filterOpt 				= ParamUtil.get(request,	IterKeys.FILTER_DATA,	"");
	
	
	//si sDate = "0" --> date = fecha actual. Si sDate es una fecha --> date = sDate
	Date date 						= TeaserContentUtil.getDateFromURLParam(sDate, GroupMgr.getPublicationDate(themeDisplay.getScopeGroupId()));
	long globalGroupId 				= CompanyLocalServiceUtil.getCompany(themeDisplay.getCompanyId()).getGroup().getGroupId();
	
	SectionUtil.setSectionPlid( PortalUtil.getOriginalServletRequest(request), ParamUtil.getLong(request, "sectionPlid", 0) );

	//XML Request
	String xmlRequest = PortletMgr.getXmlRequest(  themeDisplay, scheme, serverPort );
%>

<c:if test="<%=themeDisplay!=null%>">

<%=PHPUtil.getCheckAccessPHPCode(request, response, themeDisplay)%>

<%
	
	Layout layout = themeDisplay.getLayout();
	
	//Se obtienen las preferencias, sino está indicada una preferencia de referencia se intenta obtener la preferencia a partir del portletId
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
	long sectionPlid 		= SectionUtil.getSectionPlid(request);
	
		
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
	boolean paged = GetterUtil.getBoolean(preferences.getValue("paged", null), false);;
	String distribution = preferences.getValue("distribution", "vertical");
	int teaserPerPage = GetterUtil.getInteger(preferences.getValue("teaserPerPage", null), 1);
	String paginationHtmlPosition = preferences.getValue("paginationHtmlPosition", "l-a-s");
	String paginationButtonsPosition = preferences.getValue("paginationButtonsPosition", "top");
	String buttonPrev = GetterUtil.getString(preferences.getValue("buttonPrev", null),"Retroceder");		
	String buttonNext = GetterUtil.getString(preferences.getValue("buttonNext", null),"Avanzar");	
	String buttonShowMore = GetterUtil.getString(preferences.getValue("buttonShowMore", null),"Mostrar más");
	boolean showMore = GetterUtil.getBoolean(preferences.getValue("showMore", null), false);
	
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
	
	//lastPublication será null. Un teaser con filtro no va a ir nunca en una newsletter.
	Date lastPublication = null;
	
	// Si el portlet tiene vinculadas las preferencias a las de un PortletItem, y dichas preferencias NO utilizan 
	// la "Sección Actual", se omite el plid para que todas las preferencias vinculadas tengan la misma URL.
	long plidValue = themeDisplay.getOriginalPlid();
	if (!portletItem.isEmpty() && !defaultLayout)
	{
		plidValue 	= 0;
		sectionPlid = 0;
	}
	

	//dateAjax( utilizando date que se lee de la petición ajax..)
	SimpleDateFormat sdf = new SimpleDateFormat(WebKeys.URL_PARAM_DATE_FORMAT_FULL);
	String dateAjax = sdf.format(date);
	
	//secciones y categorías 
	long[] categoryIds = null;
	long[] categoryFilter = null;
	long[] categoryConf = TeaserContentUtil.getCategoryIdsFromURLParam(sCategoryIds, new long[]{});
	categoryIds = categoryConf;
	//true cuando hay que añadir claúsula 'and' a la query con la categoría seleccionada para filtrar
	boolean addCategoryfilter = false;
	
	
	String[] layoutFilter = null;
	String[] layoutConf = PortletPreferencesTools.getLayoutIds(preferences, SectionUtil.getSectionPlid(request), defaultLayout);
	String[] layoutIds	= layoutConf;
	String[] ctxLayouts = (sectionToShow != IterKeys.SECTION_TO_SHOW_SOURCE) ? null : layoutIds;
	int[] layoutLimits		= PortletPreferencesTools.getLayoutLimits(preferences);
	//true cuando hay que añadir claúsula 'and' a la query con la sección seleccionada para filtrar
	boolean addlayoutfilter = false;
	
	if( !filterOpt.equalsIgnoreCase("reset_filter") )
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
				layoutIds = layoutFilter;
			else
				addlayoutfilter  = true;

		}
	}

	themeDisplay.setLayoutIfNotExist(layoutIds);
	
	
	//cálculo de resultados
	List<String[]> listResult = null;
	
	if( filterBy.equalsIgnoreCase( IterKeys.DATE )   )
	{
		//en el caso en que el filtro sea por fecha se llama a getFilterArticles con 20 parámetros
		//el último parámetro es la fecha por la que hay que filtrar 'filterOptDate'
		
		//conversión de filterOpt de string a Date
		SimpleDateFormat dateformat = new SimpleDateFormat( IterKeys.DATEFORMAT_YYYY_MM_DD );
		Date filterOptDate = dateformat.parse( filterOpt ); 
		
		listResult = TeaserContentUtil.getFilterArticles(scopeGroupId, contentId, structures, 
						resultStart, resultEnd, date, showNonActiveContents, 
						orderBy, orderByType, categoryIds, null, 
						qualificationId, layoutIds, includeCurrentContent, "", null, null, null, layoutFilter, filterOptDate, layoutLimits );	
		
	}
	else if( !addCategoryfilter && !addlayoutfilter   )
	{
		listResult = TeaserContentUtil.getFilterArticles(scopeGroupId, contentId, structures, 
				resultStart, resultEnd, date, showNonActiveContents, 
				orderBy, orderByType, categoryIds, null, 
				qualificationId, layoutIds, includeCurrentContent, "", lastPublication, layoutLimits);	
	}
	else
	{
			if( addCategoryfilter )  
			{
				listResult = TeaserContentUtil.getFilterArticles(scopeGroupId, contentId, structures, 
						resultStart, resultEnd, date, showNonActiveContents, 
						orderBy, orderByType, categoryIds, null, 
						qualificationId, layoutIds, includeCurrentContent, "", lastPublication, null, categoryFilter, null, layoutLimits );	
			}
			else if(  addlayoutfilter    )
			{
				listResult = TeaserContentUtil.getFilterArticles(scopeGroupId, contentId, structures, 
						resultStart, resultEnd, date, showNonActiveContents, 
						orderBy, orderByType, categoryIds, null, 
						qualificationId, layoutIds, includeCurrentContent, "", lastPublication, null, null, layoutFilter, layoutLimits );	
			}
	}
	
	/* teasertotalcount que se envía por ajax tiene el tamaño de listResult inicial(al pintarse la página), por tanto no es válida.
	   hay que volver a calcularla según la lista de resultados una vez que ha aplicado el filtro */
	teasertotalcount = (listResult != null) ? listResult.size() : 0;
	
	//se pide el qualificationName para que la jsp que se encargue de pintar el resultado, lo inserte en el request
	String qualificationName = "";
	if( teasertotalcount > 0)
	{
		String qualifId = qualificationId != null  ? qualificationId[0]  :  "";
		qualificationName = QualificationTools.getQualificationName( qualifId);
	}

	
	
	if( paged )
	{
		/*ITER-551. Inserta script que actualiza en el html generado en 'teaser_user_filter.jspf' el valor del atributo 'data-responsenamespace' con el valor actual de 'responseNamespace'.
				Es necesario para que se añada el class 'loading-animation' en la espera de resultados del filtrado */
		
		String pid_dp = '_'+ refPreferenceId + '_';
		if( filterBy.equalsIgnoreCase( IterKeys.DATE ) )
		{
		%>
			<script>			
				jQryIter("#<%= pid_dp %>itrchosen-dp-reg").attr("data-responsenamespace", '<%= responseNamespace %>');
			
			</script>
		<%
		}
		else if( filterBy.equalsIgnoreCase( IterKeys.SECTIONS )  || filterBy.equalsIgnoreCase( IterKeys.CATEGORIES ) )
		{
		%>
			<script>			
				jQryIter("#<%= pid_dp %>itrchosen-select").attr("data-responsenamespace", '<%= responseNamespace %>');
			</script>
		<%	
		}
	}
	
%>
<script>
 if( document.location.hash != "")
 {
	 var pag = document.location.hash.match(/(\.p:){1}\d{1,3}/);
	 if(pag != null)
	 	document.location.hash = document.location.hash.replace(pag[0], "");
 }
	

</script>

<c:if test="<%= paged %>">
		<%@ include file="components/teaser_paged_content.jspf" %>
</c:if>
<c:if test="<%= !paged %>">
	<%@ include file="components/teaser_content.jspf" %>
</c:if>



</c:if>


