<%@page import="com.liferay.portal.util.HtmlOptimizer"%>
<%@page import="com.liferay.portlet.journal.util.JournalArticleTools"%>
<%@page import="com.protecmedia.iter.tracking.util.TrackingUtil"%>
<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.kernel.util.PropsValues"%>
<%@page import="com.liferay.portal.kernel.util.request.IterRequest"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%
if( layoutIds!=null )
{
	String refPreferenceId 	= "";
	String portletId		= "";
	
	if (portletItem.isEmpty())
	{
		refPreferenceId = GetterUtil.getString(preferences.getValue("refPreference", null), "");
		
		if (refPreferenceId.isEmpty())
			portletId = PortalUtil.getPortletId(request);
	}

	// Si el portlet tiene vinculadas las preferencias a las de un PortletItem, se omite el plid para 
	// que todas las preferencias vinculadas tengan la misma URL (al menos por sección a simular).
	long plidValue = themeDisplay.getOriginalPlid();
	if (!portletItem.isEmpty())
	{
		plidValue 	= 0;
		
		// Si además dichas preferencias NO utilizan la "Sección Actual", también se especifica un 0 en la
		// sección a simular y TODAS las URLs con dicha preferencia compartida independientemente de las 
		// secciones a simular o de los catálogos donde se hayan definido, serán iguales
		if (!defaultLayout)
			sectionPlid = 0;
	}
			
	//Modo widget (Portlet anfitrión de otro portlet huésped)
	widgetContent = PortletMgr.getWidgetContent(request, themeDisplay, false);
	
	if(!widgetContent.isEmpty())
	{
		out.print(widgetContent);
	}
	else
	{
		//Recuperar los parámetros de la petición
		String xmlRequest = PortletRequestUtil.toXML(renderRequest, renderResponse);

		ThemeDisplayCacheUtil.putPortletRequest(PortalUtil.getPortletId(request), themeDisplay);
		ThemeDisplayCacheUtil.putPortletXMLRequest(PortalUtil.getPortletId(request), xmlRequest);
		
		Date modifiedDate = RankingUtil.getModifiedDate(modifiedDateRangeTimeValue, modifiedDateRangeTimeUnit, scopeGroupId);

		//Pestaña de recientes se calcula de forma síncrona (no Ajax)
		String recentTabHTML = "";
		
		String servletPath = TrackingUtil.getportletName(request);
%>

<div class="stats-viewer" id="<portlet:namespace />markupTabs">
	<ul class="iter-tabview-list iter-widget-hd" id="<portlet:namespace />tabsList">
		<%
			for (int i = 0; i < tabsIds.length; i++)
			{
				//estilos
				String clase = "iter-tab";
				
				if (!HtmlOptimizer.isEnabled())
				{
					if (i == 0)
						clase += " first";
					else if (i == tabsIds.length-1)
						clase += " last";
					clase += " n"+(i+1);
					if (((i+1) % 2) != 0)
						clase += " odd";
					else
						clase += " even";
				}
		 %>		
		 
			    <c:if test="<%= (Integer.parseInt(tabsIds[i]) == AnalayzerConstants.TABRATED) %>">	
			    	<%
		    			clase += " iter-rankingtab-ranked-hd";
			    	 %>	
					<li class="<%= clase %>">
						<a class="iter-tab-label" href="javascript:;"><liferay-ui:message key="<%= mostRatedLabel %>" /></a>
					</li>
				</c:if>
				
				<c:if test="<%= (Integer.parseInt(tabsIds[i]) == AnalayzerConstants.TABRECENT) %>">
					<div id="<portlet:namespace />recentTabHTML" style="display: none;">
						<div class="iter-tabview-content-item  <%= (i > 0) ? "iter-helper-hidden" : "" %>">
					<%
					clase += " iter-rankingtab-mostrecent-hd";
					String sb_rankingViewerResultRecent;
					sb_rankingViewerResultRecent  = RankingUtil.getRankingViewerList(" iter-rankingtab-mostrecent-bd", globalGroupId, scopeGroupId, 
							  structures, templateIdArticle, 
							  templateIdGallery, templateIdPoll, templateIdMultimedia,
						      templateIdArticleRestricted, templateIdGalleryRestricted, 
						      templateIdPollRestricted, templateIdMultimediaRestricted,
						      modeArticle, modeGallery, modePoll, modeMultimedia,
						      0, numberOfResults, modifiedDate, new String[] {"-1"}, 
						      IterKeys.ORDER_DESC, contentCategoryIdsLong, qualificationId, 
						      layoutIds, defaultMode, modelId, sectionToShow, request, 
						      themeDisplay, xmlRequest, locale);
				
						if( sb_rankingViewerResultRecent.length() > 0 )
						{
							out.print( sb_rankingViewerResultRecent );
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
					</div>
					
					<li class="<%= clase %>">
						<a class="iter-tab-label" href="javascript:;"><liferay-ui:message key="<%= mostRecentLabel %>" /></a>
					</li>
				</c:if>
				
				<c:if test="<%= (Integer.parseInt(tabsIds[i]) == AnalayzerConstants.TABVIEWED) %>">
					<%
			    		clase += " iter-rankingtab-mostviewed-hd";
			    	 %>	
					<li class="<%= clase %>">
						<a class="iter-tab-label" href="javascript:;"><liferay-ui:message key="<%= mostViewedLabel %>" /></a>
					</li>
				</c:if>
				
				<c:if test="<%= (Integer.parseInt(tabsIds[i]) == AnalayzerConstants.TABCOMMENT) %>">
					<%
			    		clase += " iter-rankingtab-mostcommented-hd";
			    	 %>	
					<li class="<%= clase %>">
						<a class="iter-tab-label" href="javascript:"><liferay-ui:message key="<%= mostCommentedLabel %>" /></a>
					</li>
				</c:if>
				
				<c:if test="<%= (Integer.parseInt(tabsIds[i]) == AnalayzerConstants.TABSHARED) %>">
					<%
			    		clase += " iter-rankingtab-mostshared-hd";
			    	 %>	
					<li class="<%= clase %>">
						<a class="iter-tab-label" href="javascript:"><liferay-ui:message key="<%= mostSharedLabel %>" /></a>
					</li>
				</c:if>
		<%				
			}
		%>
	</ul>
	
	<div class="iter-tabview-content iter-widget-bd" id="<portlet:namespace />tabsContent">
	</div>
	
</div>

<script type="text/javascript">

	var <portlet:namespace />htmltabsReady = function () 
	{
		jQryIter(document).ready(function() 
		{
				jQryIter('#<portlet:namespace />markupTabs').addClass('iter-widget iter-component iter-tabview');
				var aui_id = "<portlet:namespace />_aui_div";
				jQryIter('#<portlet:namespace />markupTabs').prepend('<div id="'+aui_id+'" class="iter-tabview-content">');
				jQryIter('#<portlet:namespace />tabsList').appendTo('#'+aui_id);
				jQryIter('#<portlet:namespace />tabsContent').appendTo('#'+aui_id);
				
				// tabs
				jQryIter.each(jQryIter('#<portlet:namespace />tabsList').children(), 
				function( index, child ) 
				{
					jQryIter(child).addClass(" iter-widget iter-component iter-state-default ")
					if(index == 0)
						jQryIter(child).addClass("iter-state-active iter-tab-active iter-state-hover");
						
					jQryIter(jQryIter(child).children("a")).click(function()
					{
						iterNavTabs(jQryIter('#<portlet:namespace />tabsList'), jQryIter('#<portlet:namespace />tabsContent'), index)
					});
				});
				
				// content
				jQryIter.each(jQryIter('#<portlet:namespace />tabsContent').children(), 
				function( index, childC ) 
				{
					jQryIter(childC).addClass('iter-tabview-content iter-widget-bd');
					jQryIter(childC).removeAttr('id');
				});
				
				jQryIter("#<portlet:namespace />tabsContent").find("script").each(
				function(i) 
				{
	                eval(jQryIter(this).text());
				});
				
		  		var el = jQuery("#<%=HtmlOptimizer.getPortletId(PortalUtil.getPortletId(request))%>");
		  		if (el.hasClass("_rc"))
			  		jQuery(document).trigger("rankingCompleteLoad", el.attr("id"));

				jQryIter.lazyLoadSetup();
				
				<%		
				Object showDisqusCountAttr = PublicIterParams.get(TeaserUtil.REQUEST_ATTRIBUTE_SHOW_DISQUS_COUNT);
				if( showDisqusCountAttr != null && GetterUtil.getBoolean(showDisqusCountAttr.toString()) )
				{
					out.print("if (typeof ITRDISQUSWIDGETS != 'undefined') {ITRDISQUSWIDGETS.req(" + scopeGroupId + ");}");
				}
				
				out.print(JournalArticleTools.getJavascriptArticleVisitsCode(scopeGroupId, false));
				%>	
		});
			
	}

	var <portlet:namespace />getTabsInfo = function () 
	{
		jQryIter.ajax(
		{
			type: 'GET',
			url: '<%=servletPath%>/html/ranking-viewer/ranking_details.jsp',
			data: {
				portletItem: 					'<%=portletItem%>',
				refPreferenceId: 				'<%=refPreferenceId%>',
				portletId: 						'<%=portletId%>',
				scopeGroupId: 					'<%=themeDisplay.getScopeGroupId()%>',
				companyId: 						'<%=themeDisplay.getCompanyId()%>',
				languageId: 					'<%=themeDisplay.getLanguageId()%>',
				plid: 							'<%=plidValue%>',
				sectionPlid:					'<%=sectionPlid%>',
				secure: 						'<%=themeDisplay.isSecure()%>',
				userId: 						'<%=themeDisplay.getPermissionChecker().getUserId()%>',
				lifecycleRender: 				'<%=themeDisplay.isLifecycleRender()%>',
				pathFriendlyURLPublic: 			'<%=themeDisplay.getPathFriendlyURLPublic()%>',
				pathFriendlyURLPrivateUser: 	'<%=themeDisplay.getPathFriendlyURLPrivateUser()%>',
				pathFriendlyURLPrivateGroup: 	'<%=themeDisplay.getPathFriendlyURLPrivateGroup()%>',
				serverName: 					'<%=themeDisplay.getServerName()%>',
				cdnHost: 						'<%=themeDisplay.getCDNHost()%>',
				pathImage: 						'<%=themeDisplay.getPathImage()%>',
				pathMain: 						'<%=themeDisplay.getPathMain()%>',
				pathContext: 					'<%=themeDisplay.getPathContext()%>',
				urlPortal: 						'<%=themeDisplay.getURLPortal()%>',
				isMobileRequest: 				'<%=IterRequest.isMobileRequest()%>',
				pathThemeImages: 				'<%=themeDisplay.getPathThemeImages()%>'
			},
			success: 
  				function(response) 
  				{
						
					var recentTabHTML = jQryIter("#<portlet:namespace />recentTabHTML").html();
						jQryIter("#<portlet:namespace />recentTabHTML").remove();
						
					var responseHTML = response.replace('<div id="recentTab"></div>', recentTabHTML);
						jQryIter("#<portlet:namespace />tabsContent:first").html(responseHTML);
						
					<portlet:namespace />htmltabsReady();
  				},
			error: 
				function(xhr, status, error) 
   				{}
		});
	};
	
	<%
	// ticket 0009969:Ranking viewer con sólo la pestaña de "lo más reciente" genera una petición ajax inútil. 
	if( tabsIds.length  > 1  || ( tabsIds.length == 1  && (Integer.parseInt(tabsIds[0]) != AnalayzerConstants.TABRECENT) )  )
	{
	%>	
		<portlet:namespace />getTabsInfo();
	<%
	}
	else if(  tabsIds.length == 1  && (Integer.parseInt(tabsIds[0]) == AnalayzerConstants.TABRECENT) )
	{
	%>	
		var recentTabHTML = jQryIter("#<portlet:namespace />recentTabHTML").html();
		jQryIter("#<portlet:namespace />recentTabHTML").remove();
		
		jQryIter("#<portlet:namespace />tabsContent:first").html(recentTabHTML);
		
		<portlet:namespace />htmltabsReady();
	<%
	}
	%>
	
	function iterNavTabs(divTabs, divContent, index)
	{
		if (divContent.children("div:not(.iter-helper-hidden)").length > 0)
			jQryIter(divContent.children("div:not(.iter-helper-hidden)")[0]).addClass("iter-helper-hidden")
			
		jQryIter(divContent.children("div")[index]).removeClass("iter-helper-hidden");
		
		if (divTabs.children(".iter-tab.iter-state-active.iter-tab-active.iter-state-hover").length > 0)
			jQryIter(divTabs.children(".iter-tab.iter-state-active.iter-tab-active.iter-state-hover")[0]).removeClass("iter-state-active iter-tab-active iter-state-hover")
			
		jQryIter(divTabs.children(".iter-tab")[index]).addClass("iter-state-active iter-tab-active iter-state-hover");
	}
</script>
<%
	}
}
%>