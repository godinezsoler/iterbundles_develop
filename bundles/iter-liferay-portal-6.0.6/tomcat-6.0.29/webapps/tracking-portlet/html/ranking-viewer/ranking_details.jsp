<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@page import="com.liferay.portal.kernel.util.PropsUtil"%>
<%@page import="com.liferay.portal.kernel.util.PropsKeys"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletMgr"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.util.SectionUtil"%>
<%@page import="com.liferay.portal.kernel.comments.CommentsConfigBean"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portlet.TeaserUtil"%>
<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletPreferencesTools"%>
<%@ page contentType="text/html-by-ajax; charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.servlet.HttpHeaders"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.util.PortletKeys"%>
<%@page import="com.liferay.portal.service.PortletPreferencesLocalServiceUtil"%>
<%@page import="com.liferay.portal.service.ThemeLocalServiceUtil"%>
<%@page import="com.liferay.taglib.util.ThemeUtil"%>
<%@page import="com.liferay.util.bridges.jsf.common.ThemeDisplayManagedBean"%>

<%@page import="javax.portlet.RenderRequest"%>
<%@page import="javax.portlet.PortletPreferences"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Locale"%>
<%@page import="java.util.Date"%>

<%@page import="com.liferay.portal.kernel.util.StringUtil"%>
<%@page import="com.liferay.portal.model.Layout"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.theme.ThemeDisplay"%>
<%@page import="com.liferay.portal.service.LayoutLocalServiceUtil"%>
<%@page import="com.liferay.portal.service.CompanyLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>
<%@page import="com.protecmedia.iter.tracking.util.ThemeDisplayCacheUtil"%>
<%@page import="com.protecmedia.iter.tracking.util.AnalayzerConstants"%>
<%@page import="com.protecmedia.iter.tracking.util.RankingUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil" %>
<%@page import="com.protecmedia.iter.base.service.util.GroupMgr"%>
<%@page import="com.protecmedia.iter.tracking.util.TrackingUtil"%>

<%
	ThemeDisplay themeDisplay = IterLocalServiceUtil.rebuildThemeDisplayForAjax(request);
	long scopeGroupId 	= themeDisplay.getScopeGroupId();
	String serverPort   = Integer.toString( themeDisplay.getServerPort() );
	
	String scheme 						= request.getScheme();
	Locale locale 						= request.getLocale();

	String portletId 					= ParamUtil.get(request, "portletId", "");
	String refPreferenceId				= ParamUtil.get(request, "refPreferenceId", "");
	String portletItem					= ParamUtil.get(request, IterKeys.PREFS_PORTLETITEM, "");
	long globalGroupId 					= CompanyLocalServiceUtil.getCompany(themeDisplay.getCompanyId()).getGroup().getGroupId();
	
	SectionUtil.setSectionPlid( PortalUtil.getOriginalServletRequest(request), ParamUtil.getLong(request, "sectionPlid", 0) );
	
	//XML Request
	String xmlRequest 					= PortletMgr.getXmlRequest(  themeDisplay, scheme, serverPort );
%>

<c:if test="<%=themeDisplay!=null%>">

<%=PHPUtil.getCheckAccessPHPCode(request, response, themeDisplay)%>

<%
	Layout layout = themeDisplay.getLayout();
	
	// Se obtienen a partir de la preferencia de referencia, sino está indicada una preferencia de referencia se intenta obtener la preferencia a partir del portletId
	PortletPreferences preferences = PortletPreferencesTools.getPreferences(request);
	
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
	String[] layoutIds 		= PortletPreferencesTools.getLayoutIds(preferences, SectionUtil.getSectionPlid(request), defaultLayout);
	themeDisplay.setLayoutIfNotExist(layoutIds);

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
	
	//Texto por defecto
	String defaultTextHTML = preferences.getValue("defaultTextHTML", StringPool.BLANK);
	boolean showDefaultTextHTML = GetterUtil.getBoolean(preferences.getValue("showDefaultTextHTML", null), false);
	
	Date modifiedDate = RankingUtil.getModifiedDate(modifiedDateRangeTimeValue, modifiedDateRangeTimeUnit, themeDisplay.getScopeGroupId());

	for (int i = 0; i < tabsIds.length; i++)
	{		
		
%>		

		<c:if test="<%= (Integer.parseInt(tabsIds[i]) == AnalayzerConstants.TABRECENT) %>">		
				<div id="recentTab"></div>
		</c:if>
		
		<c:if test="<%= (Integer.parseInt(tabsIds[i]) == AnalayzerConstants.TABVIEWED) %>">				
			<div class="iter-tabview-content-item  <%= (i > 0) ? "iter-helper-hidden" : "" %>">
			<%
				String sb_rankingViewerResultViewed = RankingUtil.getRankingViewerList(" iter-rankingtab-mostviewed-bd",globalGroupId, themeDisplay.getScopeGroupId(), structures,
						templateIdArticle, templateIdGallery, 
						templateIdPoll, templateIdMultimedia,
						templateIdArticleRestricted, templateIdGalleryRestricted, 
						templateIdPollRestricted, templateIdMultimediaRestricted,
						modeArticle, modeGallery, 
						modePoll, modeMultimedia,
						0, numberOfResults, modifiedDate, 
						new String[] {String.valueOf(IterKeys.ORDER_VIEW)}, 
						IterKeys.ORDER_DESC, 
						contentCategoryIdsLong, 
						qualificationId, layoutIds, defaultMode,
						modelId, sectionToShow, request, 
						themeDisplay, xmlRequest, locale);
				
				if( sb_rankingViewerResultViewed.length() > 0 )
				{
			%>
					<%=sb_rankingViewerResultViewed%>
					
			<%
				}
				else if(  showDefaultTextHTML && !defaultTextHTML.equals("")   )
				{
					//caso en que para este tab no hay resultados
			%>
					<div>
						<%=defaultTextHTML%>
					</div>	
			<% 
				}
			%>	
			
			</div>
		</c:if>
		
		<c:if test="<%= (Integer.parseInt(tabsIds[i]) == AnalayzerConstants.TABCOMMENT) %>">
			<div class="iter-tabview-content-item  <%= (i > 0) ? "iter-helper-hidden" : "" %>">
			
			<%
				String sb_rankingViewerResultComment = RankingUtil.getStatisticHTML(" iter-rankingtab-mostcommented-bd", globalGroupId, 
						themeDisplay.getScopeGroupId(), structures,
						templateIdArticle, templateIdGallery, 
						templateIdPoll, templateIdMultimedia,
						templateIdArticleRestricted, templateIdGalleryRestricted, 
						templateIdPollRestricted, templateIdMultimediaRestricted,
						modeArticle, modeGallery, 
						modePoll, modeMultimedia,
						0, numberOfResults, modifiedDate, 
						Integer.parseInt(tabsIds[i]),
						contentCategoryIdsLong, 
						qualificationId, layoutIds, defaultMode,
						modelId, sectionToShow, request, 
						themeDisplay, xmlRequest, locale);
			
			
			
				if( sb_rankingViewerResultComment.length() > 0 )
				{
			%>
					<%=sb_rankingViewerResultComment%>
					
			<%
				}
				else if(  showDefaultTextHTML && !defaultTextHTML.equals("")   )
				{
					//caso en que para este tab no hay resultados
			%>
					<div>
						<%=defaultTextHTML%>
					</div>	
			<% 
				}
			%>	
				
			</div>
		</c:if>
		
		<c:if test="<%= (Integer.parseInt(tabsIds[i]) == AnalayzerConstants.TABRATED) %>">
			<div class="iter-tabview-content-item  <%= (i > 0) ? "iter-helper-hidden" : "" %>">
				
			<%
				String sb_rankingViewerResultRated = RankingUtil.getRankingViewerList(" iter-rankingtab-ranked-bd",globalGroupId, 
						themeDisplay.getScopeGroupId(), structures,
						templateIdArticle, templateIdGallery, 
						templateIdPoll, templateIdMultimedia,
						templateIdArticleRestricted, templateIdGalleryRestricted, 
						templateIdPollRestricted, templateIdMultimediaRestricted,
						modeArticle, modeGallery, 
						modePoll, modeMultimedia,
						0, numberOfResults, modifiedDate, 
						new String[] {String.valueOf(IterKeys.ORDER_RATINGS)},
						IterKeys.ORDER_DESC, 
						contentCategoryIdsLong, 
						qualificationId, layoutIds, defaultMode,
						modelId, sectionToShow, request, 
						themeDisplay, xmlRequest, locale);
			
			
			
				if( sb_rankingViewerResultRated.length() > 0 )
				{
			%>
					<%=sb_rankingViewerResultRated%>
					
			<%
				}
				else if(  showDefaultTextHTML && !defaultTextHTML.equals("")   )
				{
					//caso en que para este tab no hay resultados
			%>
					<div>
						<%=defaultTextHTML%>
					</div>	
			<% 
				}
			%>	
		
			</div>
		</c:if>
		
		<c:if test="<%= (Integer.parseInt(tabsIds[i]) == AnalayzerConstants.TABSHARED) %>">
			<div class="iter-tabview-content-item  <%= (i > 0) ? "iter-helper-hidden" : "" %>">
			
			<%
				String sb_rankingViewerResultShared = RankingUtil.getStatisticHTML(" iter-rankingtab-mostshared-bd", globalGroupId, 
						themeDisplay.getScopeGroupId(), structures,
						templateIdArticle, templateIdGallery, 
						templateIdPoll, templateIdMultimedia,
						templateIdArticleRestricted, templateIdGalleryRestricted, 
						templateIdPollRestricted, templateIdMultimediaRestricted,
						modeArticle, modeGallery, 
						modePoll, modeMultimedia,
						0, numberOfResults, modifiedDate, 
						Integer.parseInt(tabsIds[i]),
						contentCategoryIdsLong, 
						qualificationId, layoutIds, defaultMode,
						modelId, sectionToShow, request, 
						themeDisplay, xmlRequest, locale);

				if( sb_rankingViewerResultShared.length() > 0 )
				{
			%>
					<%=sb_rankingViewerResultShared%>
					
			<%
				}
				else if(  showDefaultTextHTML && !defaultTextHTML.equals("")   )
				{
					//caso en que para este tab no hay resultados
			%>
					<div>
						<%=defaultTextHTML%>
					</div>	
			<% 
				}
			%>	
					
			</div>
		</c:if>
	<%
	}
	%>
	
</c:if>