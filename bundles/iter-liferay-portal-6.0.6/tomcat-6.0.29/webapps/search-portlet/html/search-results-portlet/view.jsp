<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@page import="com.liferay.portal.kernel.util.HtmlUtil"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@ include file="init.jsp" %>  

<%!
private static Log _log = LogFactoryUtil.getLog("search-portlet.docroot.html.search-results-portlet.view.jsp");
%>


<%
	long tIni = System.currentTimeMillis();
	if(_log.isTraceEnabled())
	{
		_log.trace("SOLR-TIME portlet de resultado de busqueda");
		tIni = System.currentTimeMillis();
	}

	String requestURL = PortalUtil.getCurrentURL(request);
	int total = 0;
	Hits results = null;
	String responseNamespace = "";
	
	if(Validator.isNotNull(requestURL) && requestURL.contains(PortalUtil.getSearchSeparator()))
	{
		String keywords = ParamUtil.getString(renderRequest, "keywords", "");
		//Filtros
		try
		{
			keywords = HttpUtil.decodeURL(keywords);
		}
		catch(Exception e){}
	
		boolean showFuzzyButton = ParamUtil.getBoolean(renderRequest, "fuzzyOption", false);
		boolean fuzzy = ParamUtil.getBoolean(renderRequest, "fuzzy", false);
	 	
		String endDate = ParamUtil.getString(renderRequest, "endDate", "");
		String startDate = ParamUtil.getString(renderRequest, "startDate", "");
		
		String order = ParamUtil.getString(renderRequest, "order", SearchUtil.SEARCH_ORDER_DEFAULT);
		
		boolean checkArticle = ParamUtil.getBoolean(renderRequest, "checkArticle", false);
		boolean checkPoll = ParamUtil.getBoolean(renderRequest, "checkPoll", false);
		
		String filterquery = ParamUtil.getString(renderRequest, "filterquery", "");
		String wildcard = ParamUtil.getString(renderRequest, "wildcard", "");
		
		List<Long> listCategoryIds = new ArrayList<Long>();
		String categoryIds = ParamUtil.getString(renderRequest, "categoryIds", "0");
		if( !categoryIds.equals("0") )
		{
			String[] categoriesArr = categoryIds.split(CategoriesUtil.URL_PARAM_TAGS_SEPARATOR);
			for(String cat : categoriesArr)
			{
				try
				{
					listCategoryIds.add( Long.valueOf(cat) );
				}
				catch(Exception e)
				{}
			}
		}
		
		String layoutsPlid = SearchUtil.getLayouts(renderRequest);
		List<Long> listlayoutsPlid = SearchUtil.getLayoutsPidLong(layoutsPlid);

		String cur = ParamUtil.getString(renderRequest, "cur", "1");
		int selectedCur = Integer.parseInt(cur);
	 		
		SearchOptions so = new SearchOptions();
		so.setDelegationId( themeDisplay.getScopeGroup().getDelegationId() );
		so.setText(keywords);
		so.setOrder(order);
		so.setCheckArticle(checkArticle);
		so.setCheckPoll(checkPoll);
		so.setFuzzy(fuzzy);
		
		if(delta > SearchUtil.MAX_ITEMS_PER_PAGE)
		{
			String currentURL = PortalUtil.getCurrentURL(request);
			if(Validator.isNull(currentURL))
				currentURL = "";	
				
			String msg = "Current search: \"" + currentURL + "\" is not allowed. More than " + SearchUtil.MAX_ITEMS_PER_PAGE + " items per page";
			SessionErrors.add(request, "search-max-elements-error");
		}
		else
		{
			so.setItemsPerPage(delta);
			so.setPage(selectedCur);
			so.setOrder(order);
			so.setCategoriesIds(listCategoryIds);
			so.setLayoutsPlid(listlayoutsPlid);
			
			// Las fechas de inicio y de fin nos llegan con un mes de más debido a javascript (para Java, enero = 0, no 1)
			if (Validator.isNotNull(startDate))
			{
				Calendar initDate = Calendar.getInstance();
				initDate.setTime(SearchUtil.getDate(startDate));
				so.setStartDate(initDate.getTime());
			}
			else
			{
				so.setStartDate(null);
			}
			
			if (Validator.isNotNull(endDate))
			{
				Calendar finishDate = Calendar.getInstance();
				finishDate.setTime(SearchUtil.getDate(endDate));						
				so.setEndDate(finishDate.getTime());
			}
			else
			{
				so.setEndDate(null);
			}
			
			so.setFilterquery(filterquery);
			so.setWildcard(wildcard);
		}
		
		long tIni2 = 0L;
		if(_log.isTraceEnabled())
		{
			_log.trace("SOLR-TIME Inicio de búsqueda");
			tIni2 = System.currentTimeMillis();
		}
		
		SearchResults shearchResults = SearchUtil.search(so, renderRequest, selectedCur, delta, paged);
		
		if(_log.isTraceEnabled())
		{
			_log.trace( String.format("SOLR-TIME Fin de la búsqueda. Tiempo: %s ms.", System.currentTimeMillis()-tIni2) );
		}
		
		results = shearchResults.getResults();
		total = shearchResults.getTotal();
		
		request.setAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_TOTAL_COUNT, 		total);
		request.setAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_CUR_PAGE_COUNT, 	(results != null) ? results.getDocs().length : 0);
		PortalUtil.getOriginalServletRequest(request).setAttribute(WebKeys.SEARCH_RESULT, results);

			
		String fuzzyResultsLayoutURL = SearchUtil.getSearchResultURL(request, themeDisplay, themeDisplay.getLayout().getUuid());
		String fuzzyResultsParamURL = SearchUtil.getSearchResultParams(String.valueOf(true), String.valueOf(false), startDate, endDate,
																	   order, String.valueOf(checkArticle),String.valueOf(checkPoll),
																	   filterquery, wildcard, categoryIds, layoutsPlid, String.valueOf(delta), cur);
		
		
		int index = requestURL.lastIndexOf('/');
		int selPage = Integer.parseInt(cur);
		int ini = 0;
		responseNamespace = renderResponse.getNamespace();
		//cálculo de si hay bastantes artículos para mostrar el control del paginador
		boolean showPaginator = total > delta;
	%>

<c:choose>

	<c:when test="<%=delta > SearchUtil.MAX_ITEMS_PER_PAGE%>">
		<liferay-ui:error key="search-max-elements-error" message="search-max-elements-error"/>
	</c:when>
	
	<c:otherwise>
	
		<%--Con paginación--%>
		<c:if test="<%= paged %>">
		<%	if(selPage<=1)
			{
				ini = 1;
			}
			else
			{
				if (((selectedCur-1) * delta)<total)
					ini = (selectedCur-1) * delta + 1;
				else
					ini = total - delta + 1;
			}
		
		
		
		 %>
		</c:if>
		
		<c:if test="<%= (paged && showPaginator  &&
				(paginationButtonsPosition.equalsIgnoreCase(TOP) || 
				paginationButtonsPosition.equalsIgnoreCase(BOTH)))%>">
		
		<%
			String classCarruselCss = "";
			if (total>0)
					classCarruselCss = "carrusel ";
			else
					classCarruselCss = "carrusel-empty";
			%>
			<div class="<%= classCarruselCss %>" id="mylistCarrousel">
			<%
			String totalArticle = Integer.toString(total);
			String fewPerPage = Integer.toString(delta);
						
			request.setAttribute("paginationhtmlposition", paginationHtmlPosition);			
			request.setAttribute("fewperpage", fewPerPage);
			request.setAttribute("totalarticle", totalArticle);
			request.setAttribute("buttonprev", buttonPrev);			
			request.setAttribute("buttonnext", buttonNext);
			request.setAttribute("buttonshowmore", buttonShowMore);
			request.setAttribute("showmore", showMore);
			
			request.setAttribute("cur", selPage);
			request.setAttribute("responseNamespace", responseNamespace);
					
			ServletContext baseContext = getServletContext().getContext("/base-portlet");
			baseContext.getRequestDispatcher("/html/components/pagination.jsp").include(request, response);
			%>
		</div>
		</c:if>
		
		<c:choose>
			<c:when test='<%=SessionErrors.isEmpty(request)%>'>
				<c:choose>
					<c:when test='<%= (results == null || total <= 0)%>'>
						<div>
							<span class="portlet-msg-info" style="margin:0px 4px;">
					    		<liferay-ui:message key="<%=noResults%>" />
					    	</span>
				    	</div>		
				    	
				    	<br>
				    	<c:if test="<%= showFuzzyButton %>">
	    					<script type="text/javascript">
	    					
	    						<%@ include file="../commons/javascripts.jsp" %>
	    						
								function onClickFuzzySearch()
								{
									<%
										if(Validator.isNotNull(fuzzyResultsLayoutURL))
										{
									%>
											var keywords = <portlet:namespace />cleanKeywords('<%= keywords.replace("'", "\\'") %>');
											if(keywords.length > 0)
											{
												var fuzzyResultsLayoutURL = '<%= fuzzyResultsLayoutURL %>';
												var fuzzyResultsParamURL = '<%= fuzzyResultsParamURL %>';
												window.location.href = fuzzyResultsLayoutURL + keywords + fuzzyResultsParamURL;
											}
									<%
										}
									%>
								}
							</script> 
				    	
					    	<div class="searchNoResults">
								<span class="searchNoResultsLabel">
									<liferay-ui:message key="<%=suggestFuzzy%>"/>
								</span>
								<br>
								<br>
								<span class="aui-button aui-button-submit searchNoResultsButtonsWrapper"> 
									<span class="aui-button-content" > 
										<input class="aui-button-input aui-button-input-submit searchNoResultsSubmit" id="_searchresultsportlet_WAR_searchportlet_search" type="submit" value="<%=fuzzyButtonLabel%>" onclick="onClickFuzzySearch()"> 
									</span> 
								</span>
					    	</div> 
					    </c:if>
						<div class="spacer <%=HtmlUtil.getRemovableClass()%>"></div>
					</c:when>
					<c:otherwise>
						<div class="searchHeader">
							<h2><liferay-ui:message key="<%= searchResults %>" /> <span class="keyword"><%= keywords %></span> (<%= total %> <liferay-ui:message key="<%= resultsWereFound %>" />)</h2>
							<div class="results-separator"></div>
						</div>							
					</c:otherwise>
				</c:choose>
			</c:when>
		</c:choose>
		
		<%

		if(results != null)
		{
			if(_log.isTraceEnabled())
				tIni2 = System.currentTimeMillis();
			
			int idx = 1;
			
			%><ul class="noticias">	<%	
			for (int i = 0; i < results.getDocs().length; i++)
			{
				try
				{			
					String articleId = SearchUtil.getJournalArticleId(results.doc(i));
					JournalArticle article = JournalArticleLocalServiceUtil.getArticle(globalGroupId, articleId);
					String structureId = article.getStructureId();
			
					if(!articleId.equals(IterKeys.EXAMPLEGALLERYID) && !articleId.equals(IterKeys.EXAMPLEPOLLID) &&
						   !articleId.equals(IterKeys.EXAMPLEMULTIMEDIAID) && !articleId.equals(IterKeys.EXAMPLEARTICLEID))
					{

						PageContent pc = null;
						try
						{
							pc = PageContentLocalServiceUtil.getDefaultPageContentByContentId(scopeGroupId, articleId);
							if (pc == null)
								pc = PageContentLocalServiceUtil.getFirstPageContent(scopeGroupId, articleId);
						}catch(Exception e){}
						
						String[] layoutIds = null;
							
						if (pc == null)
							layoutIds = new String[] {layout.getUuid()};
						else
								layoutIds = new String[] {pc.getLayoutId()};
							
							String faTemplateId = PageContentLocalServiceUtil.getTemplateId(article, templateIdArticle, StringPool.BLANK,
																							templateIdPoll, StringPool.BLANK);

							String raTemplateId = PageContentLocalServiceUtil.getTemplateId(article, templateIdArticleRestricted, 
									StringPool.BLANK, templateIdPollRestricted, 
									StringPool.BLANK);
							
							String xmlRequest = PortletRequestUtil.toXML(renderRequest, renderResponse);
							
							String viewMode = PageContentLocalServiceUtil.getArticleContextInfo(scopeGroupId, articleId, null);
								
							int templateMode = PageContentLocalServiceUtil.getTemplateMode(article, modeArticle, -1, modePoll, -1);
							
							String html = PageContentLocalServiceUtil.getArticleContent(article, faTemplateId, raTemplateId, viewMode, 
																						themeDisplay, xmlRequest, templateMode, request,
																		  				i + 1, results.getDocs().length);
							
			        		String id = renderResponse.getNamespace() + i;
			        		String idCMS = article.getArticleId();
			        		int resultIdx = ini + i;
			        		
							%>								
								<c:if test="<%= (html != null && !html.isEmpty()) %>">																			
									<div class="spacer <%=HtmlUtil.getRemovableClass()%>"></div>
									
									<li class="element <%= PageContentLocalServiceUtil.getCSSClass(resultIdx, total) %>  <%= PageContentLocalServiceUtil.getCSSAccessClass(article, request) %>">
										<span class='teaserItemPosition <%=HtmlUtil.getRemovableClass()%>'><%=resultIdx%></span>
										<%=html%>
									</li>
									
								</c:if>
							<%
						}
					}
					catch (Exception e) 
					{			
						System.out.println("Error al pintar resultado de búsqueda");
						e.printStackTrace();
					}
				}
				%></ul>	<%
				if(_log.isTraceEnabled())
					_log.trace( String.format("SOLR-TIME Fin del pintando del resultado de la búsqueda. %s artículos. Tiempo %s ms.", results.getDocs().length, System.currentTimeMillis()-tIni2));
			}
				
			%>
			
		<%--Con paginación--%>
		<c:if test="<%= (paged && showPaginator &&
				(paginationButtonsPosition.equalsIgnoreCase(BOTTOM) || 
				paginationButtonsPosition.equalsIgnoreCase(BOTH)))%>">
		<%
			String classCarruselCss = "";
			if (total>0)
				classCarruselCss = "carrusel ";
			else
				classCarruselCss = "carrusel-empty";
		%>
		<div class="<%= classCarruselCss %>" id="mylistCarrousel">
	
		<%
			String totalArticle = Integer.toString(total);
			String fewPerPage = Integer.toString(delta);
			
			request.setAttribute("paginationhtmlposition", paginationHtmlPosition);			
			request.setAttribute("fewperpage", fewPerPage);
			request.setAttribute("totalarticle", totalArticle);
			request.setAttribute("buttonprev", buttonPrev);			
			request.setAttribute("buttonnext", buttonNext);
			request.setAttribute("buttonshowmore", buttonShowMore);
			request.setAttribute("showmore", showMore);
			request.setAttribute("cur", selPage);
			request.setAttribute("responseNamespace", responseNamespace);
			
			ServletContext baseContext = getServletContext().getContext("/base-portlet");
			baseContext.getRequestDispatcher("/html/components/pagination.jsp").include(request, response);
		%>
		</div>
	</c:if>
	
	<script>	
		jQryIter('#<portlet:namespace />mylistCarrousel').addClass("classCarruselCss");
		
		jQryIter('#<portlet:namespace />mylistCarrousel').on("custom", function(c, a)
		{
		      window.location.href = window.location.href.substring(0, window.location.href.lastIndexOf('/'))+"/"+a;
		});
		</script>
		
	</c:otherwise>
</c:choose>
<%
	if(_log.isTraceEnabled())
		_log.trace("SOLR-TIME Fin del portlet de resultado de busqueda: " + (System.currentTimeMillis()-tIni));
}
%>
