<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@page import="com.liferay.portal.util.HtmlOptimizer"%>
<%@page import="com.liferay.portal.kernel.util.HtmlUtil"%>
<%@page import="com.liferay.portlet.journal.util.JournalArticleTools"%>
<%@page import="com.liferay.portal.kernel.util.request.IterRequest"%>
<%@page import="com.liferay.portal.kernel.util.ArrayUtil"%>
<%@page import="com.liferay.portlet.journal.util.RelatedLinkData"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%@ include file="init.jsp" %>

<%!
	private static Log _log = LogFactoryUtil.getLog("news-portlet.docroot.html.related-viewer-portlet.view.jsp");
%>

<%

	//Lista de resultados
	List<String[]> listResult = new ArrayList<String[]>();
	List<RelatedLinkData> listRLDResult = new ArrayList<RelatedLinkData>();
	String contentId = StringPool.BLANK;
	String xmlRequest = StringPool.BLANK;
	String portletItem = StringPool.BLANK;
	String refPreferenceId = StringPool.BLANK;
	String portletId = StringPool.BLANK;
	String dateAjax = StringPool.BLANK;
	long plidValue = 0L;
	Date currentDate = null;
	int teasertotalcount = 0;
	String responseNamespace = "";

	String elemParentTagName = HtmlOptimizer.isEnabled() ? "section" : "ul";
	
	if( layoutIds!=null )
	{
		//Recuperar los parametros de la petición
		contentId 		= (String) renderRequest.getParameter(WebKeys.URL_PARAM_CONTENT_ID);
		xmlRequest 		= PortletRequestUtil.toXML(renderRequest, renderResponse);
		
		portletItem		= GetterUtil.getString(preferences.getValue(IterKeys.PREFS_PORTLETITEM, null), "");
	
		if (portletItem.isEmpty())
		{
			refPreferenceId = GetterUtil.getString(preferences.getValue("refPreference", null), "");
			
			if (refPreferenceId.isEmpty())
				portletId = PortalUtil.getPortletId(request);
		}
		
		// Si el portlet tiene vinculadas las preferencias a las de un PortletItem, y dichas preferencias NO utilizan 
		// la "Sección Actual", se omite el plid para que todas las preferencias vinculadas tengan la misma URL.
		plidValue = themeDisplay.getOriginalPlid();
		if (!portletItem.isEmpty() && !defaultLayout)
		{
			plidValue 	= 0;
			sectionPlid = 0;
		}
	
		
		currentDate = TeaserContentUtil.getDateFromURL(renderRequest, themeDisplay, 
				   GroupMgr.getPublicationDate(scopeGroupId));
	
		
		SimpleDateFormat sdf = new SimpleDateFormat(WebKeys.URL_PARAM_DATE_FORMAT_FULL);
		dateAjax = sdf.format(currentDate);
			
		if(relatedBy.equalsIgnoreCase("internallink") && (resultEnd > 0))
		{	
			if( ArrayUtil.contains( orderBy, Integer.toString(IterKeys.ORDER_MLN) ))
			{
				//obtiene listResult sin pasar por getFilterArticles
				listRLDResult = TeaserContentUtil.getInternalLinksOrderMLN(globalGroupId, contentId, resultStart, resultEnd);
				
			}
			else
			{
				List<String[]> partialListInternalLinks = TeaserContentUtil.getInternalLinks(scopeGroupId, contentId, structures, 
																resultStart, resultEnd, currentDate, 
																showNonActiveContents, orderBy, orderByType, layoutIds); 
																
				listRLDResult  = TeaserContentUtil.getInternalLinksNotOrderMLN( globalGroupId, contentId, partialListInternalLinks  );
				
			}
			
			teasertotalcount = (listRLDResult != null) ? listRLDResult.size() : 0;
		}
		else if(relatedBy.equalsIgnoreCase("externallink") && (resultEnd > 0))
		{
			//listResultObj = TeaserContentUtil.getExternalLinkContent(globalGroupId, contentId, resultStart, resultEnd);
			
			listRLDResult = TeaserContentUtil.getExternalLinkContent2(globalGroupId, contentId, resultStart, resultEnd);
			
			teasertotalcount = (listRLDResult != null) ? listRLDResult.size() : 0;
		}
		else if( relatedBy.equalsIgnoreCase("internalExternallink") && (resultEnd > 0)    )
		{
			listRLDResult = TeaserContentUtil.getRelatedInternalExternalLinks(globalGroupId, contentId, resultStart, resultEnd);
			
			teasertotalcount = (listRLDResult != null) ? listRLDResult.size() : 0;
		}
		else if(relatedBy.equalsIgnoreCase("metadata") && (resultEnd > 0))
		{
			listResult = TeaserContentUtil.getMetadatasLinks(companyId, scopeGroupId, contentId, structures, 
															 resultStart, resultEnd, currentDate, 
															 showNonActiveContents, orderBy, orderByType, 
															 contentVocabularyIds, contentCategoryIds, layoutIds);
			
			teasertotalcount = (listResult != null) ? listResult.size() : 0;
		}
		
		request.setAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_TOTAL_COUNT, teasertotalcount);
		
		responseNamespace = renderResponse.getNamespace();
	}

%>
		

		
<c:choose>
	<c:when test='<%= ( (listResult == null || listResult.size() <= 0) && (listRLDResult == null || listRLDResult.size() <= 0) ) %>'>
	<%-- Caso vacio --%>
	
		<c:if test='<%= showDefaultTextHTML && !defaultTextHTML.equals("")%>'>
			<div>
				<%=defaultTextHTML%>
			</div>
		</c:if>
	
	</c:when>
	<c:otherwise>
	
	<%--Caso no vacio--%>
		
		<c:if test="<%= !paged%>">
			<%--Sin paginación--%>
<%
			request.setAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_CUR_PAGE_COUNT, request.getAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_TOTAL_COUNT));
%>			
			<c:if test='<%= !titleContents.equals("") %>'>	
				<div class="teaser-viewer-title">
					<span><%= titleContents %></span>
				</div>				
			</c:if>
				
			<div class="relatedContent">
				<c:if test='<%= (relatedBy.equalsIgnoreCase("internallink") && (resultEnd > 0)) %>'>
					<<%=elemParentTagName%> class="teaser-related-list teaser-related-internal-links">
						<% 	
								if(listRLDResult != null && listRLDResult.size() > 0)
								{
									int currentIndex = 1;
									int listResultSize 	   = listRLDResult.size();
									
									for (RelatedLinkData articleData : listRLDResult)
									{
										
						%> 
									 	<%@ include file="components/related_container_internal_related.jspf"%> 
									<% 
									
									}
								}			
						%>
					</<%=elemParentTagName%>>
				</c:if>
				<c:if test='<%= (relatedBy.equalsIgnoreCase("externallink") && (resultEnd > 0)) %>'>
					<<%=elemParentTagName%> class="teaser-related-list teaser-related-external-links">
						<% 
							if(listRLDResult != null && listRLDResult.size() > 0)
							{
								int currentIndex = 1;
					
								for (RelatedLinkData articleData : listRLDResult)
								{
									String currentCssClass = TeaserContentUtil.getCSSClass( currentIndex, listRLDResult.size() );	
						%> 
								 	<%@ include file="components/related_container_external_related.jspf"%> 
						<% 
								}
							}
						%>
					</<%=elemParentTagName%>>
				</c:if>
				<c:if test='<%= (relatedBy.equalsIgnoreCase("internalExternallink") && (resultEnd > 0)) %>'>
					<<%=elemParentTagName%> class="teaser-related-list teaser-related-internal-links teaser-related-external-links">
						<% 
							if(listRLDResult != null && listRLDResult.size() > 0)
							{
								
								int currentIndex = 1;
								int listResultSize 	   = listRLDResult.size();
					
								for ( RelatedLinkData articleData : listRLDResult )
								{
									String type = articleData.getLinkType();
									
									if( type.equals("InternalLink")   )
									{
										//ENLACE INTERNO	
						%> 
									 	<%@ include file="components/related_container_internal_related.jspf"%> 
								<% 
									}
									else if( type.equals("ExternalLink") )
									{
										//ENLACE EXTERNO
										String currentCssClass = TeaserContentUtil.getCSSClass( currentIndex, listRLDResult.size() );	
								%>	
										 <%@ include file="components/related_container_external_related.jspf"%>
						<% 		
									}
								}
							}
						%>
					</<%=elemParentTagName%>>
				</c:if>
				<c:if test='<%= (relatedBy.equalsIgnoreCase("metadata") && (resultEnd > 0)) %>'>
					<<%=elemParentTagName%> class="teaser-related-list teaser-related-metadata">
						<%						
							if(listResult != null && listResult.size() > 0)
							{
								int currentIndex = 1;
								int listResultSize 	   = listResult.size();
								
								for (int i = 0; i < listResult.size(); i++)
								{
									String[] articleData = listResult.get(i);
									
						%>		
									<%@ include file="components/related_container_metadata_related.jspf"%>
						<%	
								}
							}
						%>
					</<%=elemParentTagName%>>
				</c:if> 	
			</div>
		</c:if>

		<%--Con paginación--%>
		<c:if test="<%= paged %>">
		<%
			//Solo en caso de paginacion se guarda el themeDisplay para dejarlo accesible a la vista AJAX
			ThemeDisplayCacheUtil.putPortletRequest(PortalUtil.getPortletId(request), themeDisplay);
			String classCarruselCss = "";
			if ( teasertotalcount > 0 )
				classCarruselCss = "carrusel ";
			else
				classCarruselCss = "carrusel-empty";
			
			//cálculo de si hay bastantes artículos para mostrar el control del paginador
			boolean showPaginator = teasertotalcount > teaserPerPage;
		%>
		
		<div class="<%= classCarruselCss %>" id="mylistCarrousel">
			
			<c:if test='<%= !titleContents.equals("") %>'>	
				<div class="teaser-viewer-title">
					<span><%= titleContents %></span>
				</div>	
			</c:if>
			
			<c:if test='<%= ( showPaginator && (paginationButtonsPosition.equalsIgnoreCase("top") 
												|| paginationButtonsPosition.equalsIgnoreCase("both")))%>'>
			<%
					String totalArticle = Integer.toString( teasertotalcount );
					String fewPerPage = Integer.toString(teaserPerPage);
					
					request.setAttribute("paginationhtmlposition", paginationHtmlPosition);			
					request.setAttribute("fewperpage", fewPerPage);
					request.setAttribute("totalarticle", totalArticle);
					request.setAttribute("buttonprev", buttonPrev);			
					request.setAttribute("buttonnext", buttonNext);
					request.setAttribute("buttonshowmore", buttonShowMore);
					request.setAttribute("showmore", showMore);
					request.setAttribute("cur", 1);
					request.setAttribute("responseNamespace", responseNamespace);

					ServletContext baseContext = getServletContext().getContext("/base-portlet");
					baseContext.getRequestDispatcher("/html/components/pagination.jsp").include(request, response);
				
				%>
			
			</c:if>
			
			<div id="<portlet:namespace/>loadingDiv"></div>
			<div class="paged" id="<portlet:namespace />myCarrusel" style="display:none;">
				<%
					
					int firstItem = 0;
					int lastItem = teaserPerPage;
					int globalFirstItem = resultStart;
					int globalLastItem = resultEnd;
					int globalLastIndex = teasertotalcount;
				%>
					
  				<%@ include file="related_page_details.jsp" %> 
 			</div>
			
	
			<c:if test='<%= (showPaginator && (paginationButtonsPosition.equalsIgnoreCase("bottom") 
											|| paginationButtonsPosition.equalsIgnoreCase("both")) )%>'>
			
				<%
					String totalArticle = Integer.toString( teasertotalcount );
					String fewPerPage = Integer.toString(teaserPerPage);
					
					request.setAttribute("paginationhtmlposition", paginationHtmlPosition);			
					request.setAttribute("fewperpage", fewPerPage);
					request.setAttribute("totalarticle", totalArticle);
					request.setAttribute("buttonprev", buttonPrev);			
					request.setAttribute("buttonnext", buttonNext);
					request.setAttribute("buttonshowmore", buttonShowMore);
					request.setAttribute("showmore", showMore);
					request.setAttribute("cur", 1);
					request.setAttribute("responseNamespace", responseNamespace);
					
					ServletContext baseContext = getServletContext().getContext("/base-portlet");
					baseContext.getRequestDispatcher("/html/components/pagination.jsp").include(request, response);
				%>
				
			</c:if>
		</div>
		
		<script>
		
		jQryIter('#<portlet:namespace />mylistCarrousel').addClass("classCarruselCss");
		
		jQryIter('#<portlet:namespace />mylistCarrousel').on("custom", function(event, myselpag) 
			{
				jQryIter("#<portlet:namespace />loadingDiv").addClass("loading-animation");
				
				var totalArticle = <%=teasertotalcount%>;
				var teaserPerPage = <%=teaserPerPage%>;
				var showMore = <%=showMore%>;
				countPagination = totalArticle/teaserPerPage;
				
				if(myselpag<=1)
				{
					myfirst = 1;
					mylast=teaserPerPage+1;
				}
				else
				{
					if (((myselpag-1) * teaserPerPage)<totalArticle)
						myfirst = (myselpag-1) * teaserPerPage + 1;
					else
					{
						myfirst = totalArticle - teaserPerPage + 1;
						myselpag = countPagination + 1;
					}
						
					if ((myfirst + teaserPerPage)<=totalArticle)
						mylast = myfirst + teaserPerPage;
					else
						mylast = (totalArticle+1);
				}
				
				jQryIter.ajax({
					  	  url: '/news-portlet/html/related-viewer-portlet/related_page.jsp',
					  	  data: {
							 portletItem: 					'<%=portletItem%>',
						   	 refPreferenceId: 				'<%=refPreferenceId%>',
						   	 portletId: 					'<%=portletId%>',
						   	 contentId: 					'<%=contentId%>', 
						   	 relatedBy: 					'<%=relatedBy%>',
						   	 date: 							'<%=dateAjax%>',
						   	 teasertotalcount:				'<%=teasertotalcount%>',
						     firstItem: 					myfirst,
						     lastItem: 						mylast,
						     globalFirstItem: 				'<%=resultStart%>',
						     globalLastItem: 				'<%=resultEnd%>',
						     globalLastIndex: 				myselpag,
						     scopeGroupId: 					'<%=themeDisplay.getScopeGroupId()%>',
						     companyId: 					'<%=themeDisplay.getCompanyId()%>',
						     languageId: 					'<%=themeDisplay.getLanguageId()%>',
						     plid: 							'<%=plidValue%>',
						     sectionPlid:					'<%=sectionPlid%>',
						     secure: 						'<%=themeDisplay.isSecure()%>',
						     userId: 						'<%=themeDisplay.getPermissionChecker().getUserId()%>',
						     lifecycleRender: 				'<%=themeDisplay.isLifecycleRender()%>',
						     pathFriendlyURLPublic: 		'<%=themeDisplay.getPathFriendlyURLPublic()%>',
						     pathFriendlyURLPrivateUser: 	'<%=themeDisplay.getPathFriendlyURLPrivateUser()%>',
						     pathFriendlyURLPrivateGroup: 	'<%=themeDisplay.getPathFriendlyURLPrivateGroup()%>',
						     serverName: 					'<%=themeDisplay.getServerName()%>',
						     cdnHost: 						'<%=themeDisplay.getCDNHost()%>',
						     pathImage: 					'<%=themeDisplay.getPathImage()%>',
						     pathMain: 						'<%=themeDisplay.getPathMain()%>',
						     pathContext: 					'<%=themeDisplay.getPathContext()%>',
						     urlPortal: 					'<%=themeDisplay.getURLPortal()%>',
							 isMobileRequest: 				'<%=IterRequest.isMobileRequest()%>',
						     pathThemeImages: 				'<%=themeDisplay.getPathThemeImages()%>'
						   },
					  	}).success(function(data) 
						{
					  		if (showMore)
								
					  			jQryIter('#<portlet:namespace />myCarrusel').append(data);
							else
						  		jQryIter('#<portlet:namespace />myCarrusel').html(data);
					  		
					  		//función a la que se llamará para que se carguen las imágenes configuradas con 'lazyload'
					  		jQryIter.lazyLoadSetup();
					  		
					  		jQryIter("#<portlet:namespace />loadingDiv").removeClass("loading-animation");
					  		var el = jQuery("#<%=HtmlOptimizer.getPortletId(PortalUtil.getPortletId(request))%>");
					  		if (el.hasClass("_tc"))
						  		jQuery(document).trigger("teaserCompleteLoad", el.attr("id"));

					  		<portlet:namespace />loadedsuccess(myfirst, mylast, myselpag);
					  		
					  		if(!showMore)
					  		{
					  			jQryIter.setFragmentIdentifier("p", myselpag);
					  		}
					  		
					  		goVisible('<portlet:namespace />myCarrusel');
					  		
							<%		
							Object showDisqusCountAttr = PublicIterParams.get(TeaserUtil.REQUEST_ATTRIBUTE_SHOW_DISQUS_COUNT);
							if( showDisqusCountAttr != null && GetterUtil.getBoolean(showDisqusCountAttr.toString()) )
							{
								out.print("if (typeof ITRDISQUSWIDGETS != 'undefined') {ITRDISQUSWIDGETS.req(" + scopeGroupId + ");}");
							}
							out.print(JournalArticleTools.getJavascriptArticleVisitsCode(scopeGroupId, false));
							%>
					  	}); 
				});
		
				function goVisible(cual) 
				{
					var elElemento=document.getElementById(cual);
					     elElemento.style.display = 'block';
				}
				
				
				var <portlet:namespace />goToNavPage = function ()
				{
					if(document.location.hash.indexOf(".p:") < 0)
					{
						goVisible('<portlet:namespace />myCarrusel');
					}
					else
					{
						var position = parseInt(jQryIter.getFragmentIdentifier("p"));
						
						var totalArticle = <%=teasertotalcount%>;
						var teaserPerPage = <%=teaserPerPage%>;
						var endpagination = Math.ceil(totalArticle / teaserPerPage);
						var button = 1;
						
						if(endpagination <= 10)
						{	
							if(endpagination >= position)
								button = position;
							else
								button = endpagination;
							
							jQryIter("#<portlet:namespace />myNavButtons li").eq(button-1).click();
						}
						else
						{
							if(position <= 6)
							{
								jQryIter("#<portlet:namespace />myNavButtons li").eq(position - 1).click();
							}
							else
							{
								var button = 1;
								var myini = 1;
								if(position>6)
								{
									if(endpagination > position+4)
									{
										myini = position - 5;
										button = 6;
									}
									else
									{
										myini = endpagination - 9;
										if(position > endpagination)
										{
											button = 10;
										}
										else
										{
											button = endpagination - position;
											button = 10 - button;
										}
									}
								}
									
								jQryIter('#<portlet:namespace />myNavButtons li').each(function(){
									jQryIter(this).attr("data-page", myini);
									jQryIter(this).text(myini);
									myini++;
								});
								
								jQryIter("#<portlet:namespace />myNavButtons li").eq(button-1).click();
							}
						}	
					}
				}

				<portlet:namespace />goToNavPage();
			</script>
		
				
			<div class="spacer  <%=HtmlUtil.getRemovableClass()%>"></div>
		</c:if>
	</c:otherwise>
</c:choose>		

