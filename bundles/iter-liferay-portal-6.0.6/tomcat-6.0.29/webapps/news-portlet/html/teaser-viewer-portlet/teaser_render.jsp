<%@page import="org.getopt.util.hash.FNV1a32"%>
<%@page import="com.liferay.portal.util.PortletKeys"%>
<%@page import="com.liferay.portal.util.HtmlOptimizer"%>
<%@page import="com.liferay.portal.kernel.util.HtmlUtil"%>
<%@page import="com.liferay.portal.kernel.util.UnrepeatableArticlesMgr"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.kernel.util.CategoriesUtil"%>
<%@page import="com.liferay.portal.kernel.util.QualificationTools"%>
<%@page import="com.protecmedia.iter.base.service.util.SQLQueries"%>
<%	
	if( PublicIterParams.get(WebKeys.RESPONSE_HEADER_DISCARD_RESPONSE) == null && layoutIds!=null )
	{
		//Recuperar los parámetros de la petición
		
		String contentId = (String) renderRequest.getParameter(WebKeys.URL_PARAM_CONTENT_ID);
		String scheme = 	renderRequest.getScheme();
		
		
		// Si el portlet tiene vinculadas las preferencias a las de un PortletItem, y dichas preferencias NO utilizan 
		// la "Sección Actual", se omite el plid para que todas las preferencias vinculadas tengan la misma URL.
		long plidValue = themeDisplay.getOriginalPlid();
		if (!portletItem.isEmpty() && !defaultLayout)
		{
			plidValue 	= 0;
			sectionPlid = 0;
		}
	
		String xmlRequest = PortletRequestUtil.toXML(renderRequest, renderResponse);
		
		//dateForURLFilter será 0,excepto cuando teaser esté configurado para que utilice los parámetros de la url en este caso será el valor indicado en URL.
		//De esta forma cuando la fecha no se indique en la URL, la URL generada al filtrar será la misma para configuraciones iguales y filtro por el mismo criterio
		String dateForURLFilter = "0";
		
		//Date
		Date date = GroupMgr.getPublicationDate(scopeGroupId);
		SimpleDateFormat sdf = new SimpleDateFormat(WebKeys.URL_PARAM_DATE_FORMAT_FULL);
		
		if( categoryOperation != IterKeys.LOGIC_IGNORE )
		{
			date = TeaserContentUtil.getDateFromURL(renderRequest, themeDisplay, date);
			dateForURLFilter = sdf.format(date);
		}
		
		String dateAjax = sdf.format(date);
			
		//Categorías
		long[] categoryIds = CategoriesUtil.getCategoryIdsFromURL(renderRequest, contentCategoryIdsLong, categoryOperation);
		
		String auxPortletId = PortalUtil.getPortletId(request);
		boolean activeUnrepeatable = auxPortletId.startsWith(PortletKeys.PORTLET_TEASERVIEWERNR) || !paged;
%>					
		<c:if test="<%=activeUnrepeatable%>">
<%		
		UnrepeatableArticlesMgr.active(auxPortletId);
%>		
		</c:if>
		
<%		
		//Lista de resultados
		List<String[]> listResult = TeaserContentUtil.getTeaserArticles(scopeGroupId, contentId, structures, 
																		resultStart, resultEnd, date, showNonActiveContents, 
																		orderBy, orderByType, categoryIds, qualificationId, 
																		layoutIds, includeCurrentContent, lastPublication, layoutLimits);
		
%>					
		<c:if test="<%=activeUnrepeatable%>">
<%		
		UnrepeatableArticlesMgr.add(listResult, auxPortletId);
%>		
		</c:if>
		
<%		
		
		int teasertotalcount = (listResult != null) ? listResult.size() : 0;
		request.setAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_TOTAL_COUNT, teasertotalcount);
		
		//necesario para pagination.jsp( utilizado en portlet paginado, en teaser_paged_content.jspf y no paginado en teaser_content.jspf)
		String responseNamespace = null;
		if (HtmlOptimizer.isEnabled())
		{
			responseNamespace = renderResponse.getNamespace();
		}
		else
		{
			FNV1a32 hash = new FNV1a32();
			hash.init(auxPortletId);
			responseNamespace = "_" + String.valueOf(hash.getHash()) + "_";
		}
		
		
		if((listResult == null || teasertotalcount == 0) && canDiscardResponse)
		{
			PublicIterParams.set(WebKeys.RESPONSE_HEADER_DISCARD_RESPONSE, "1");
		}
		else
		{
%>

<c:choose>

	<c:when test='<%= ((( listResult == null || teasertotalcount <= 0 ) )) %>'>
	
	<%-- Caso vacío --%>
<%		
		if( showExampleArticle  && (environment.equals(IterKeys.ENVIRONMENT_PREVIEW) )  )
		{
			
				// Caso vacío cuando está marcado el check 'Mostrar contenido de ejemplo si no hay resultados'

			try
			{												
				JournalArticle teaserContent = TeaserContentUtil.getExampleJournal(globalGroupId, articleFilter, galleryFilter, multimediaFilter, pollFilter);
				
				String viewMode = PageContentLocalServiceUtil.getArticleContextInfo(scopeGroupId, teaserContent.getArticleId(), ctxLayouts);
	
				String faTemplateId = TeaserContentUtil.getTemplateId(teaserContent, templateIdArticle, templateIdGallery, 
																	  templateIdPoll, templateIdMultimedia);
				
				String teaserHTMLContent = PageContentLocalServiceUtil.getArticleContent(teaserContent, faTemplateId, null, viewMode, 
																						 themeDisplay, xmlRequest, -1, request, 1, 1);	
			
%>
					<div class="last <%= HtmlUtil.getRemovableClass()%> <%= TeaserContentUtil.getStructureCssClass(teaserContent.getStructureId()) %>">
						<%= teaserHTMLContent %>
					</div>			
<%

			}
			catch (Exception e)
			{
				System.out.println("Error painting empty content");
				e.printStackTrace();
			}
	
		}
		if( showDefaultTextHTML  && !defaultTextHTML.equals("")  )
		{
%>			
			<div>
				<%=defaultTextHTML%>
			</div>	
<%
		}
%>
		
	</c:when>
		
	<c:otherwise>
	<%--Caso no vacio--%>
<%
	String filterData = "";	
	String userFilterText = "";
	String teaserData = "";
	
	if( !usrFilterBy.equalsIgnoreCase(IterKeys.WITHOUT_FILTER) )
	{
		filterData = TeaserContentUtil.encodeUsrFilterOpts( usrFilterBy, usrFilterLayouts, usrFilterCategories, usrFilterVocabularies, displayOption );
		if( usrFilterBy.equalsIgnoreCase("date") )
		{
			userFilterText = usrFilterTextByDate;
		}
		else
		{
			userFilterText = usrFilterBy.equalsIgnoreCase("sections") ? usrFilterTextByLayout : usrFilterTextByCat;
		}			
	}
	
	//si hay calificación se pide el qualificationName para que la jsp que se encargue de pintar lo inserte en el request.
	//Ticket 0009957.En el caso de varias calificaciones configuradas, la variable velocity $setting_qualification tendrá como valor la primera calificación
	String qualifId = qualificationId != null ? qualificationId[0]  :  "";
	String qualificationName = QualificationTools.getQualificationName( qualifId );
	
	
	
%>
	
	
	
		<c:if test="<%= !paged%>">
		
			<%--Sin paginación--%>
<%		
			
			request.setAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_CUR_PAGE_COUNT, request.getAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_TOTAL_COUNT));

			if(!isNewsletterPage)
			{
				if(  Validator.isNotNull(titleContents) && listResult != null && teasertotalcount > 0 )
				{
%>			
					<div class="teaser-viewer-title">
						<span><%= titleContents %></span>
					</div>				
<%
				}
%>
				<div class="<%= distribution %> <%= HtmlUtil.getRemovableClass()%> ">
<%
			}
			
			if( !usrFilterBy.equalsIgnoreCase(IterKeys.WITHOUT_FILTER) )
			{
				teaserData = TeaserContentUtil.encodeTeaserConfigParams(portletItem, refPreferenceId, portletId, contentId, TeaserContentUtil.getURLParamFromCategories(categoryIds),
						dateForURLFilter, teasertotalcount, themeDisplay.getScopeGroupId(), themeDisplay.getCompanyId(), themeDisplay.getLanguageId(),
						plidValue, sectionPlid, themeDisplay.isSecure(), themeDisplay.getPermissionChecker().getUserId(), themeDisplay.isLifecycleRender(),
						themeDisplay.getPathFriendlyURLPublic(), themeDisplay.getPathFriendlyURLPrivateUser(), themeDisplay.getPathFriendlyURLPrivateGroup(), themeDisplay.getServerName(), 
						themeDisplay.getCDNHost(), themeDisplay.getPathImage(), themeDisplay.getPathMain(), themeDisplay.getPathContext(), themeDisplay.getURLPortal(),
						themeDisplay.getPathThemeImages(), themeDisplay.getServerPort(), scheme , includeCurrentContent);
			}
			
			if( !usrFilterBy.equalsIgnoreCase(IterKeys.WITHOUT_FILTER) &&
                (usrFilterPos.equalsIgnoreCase(IterKeys.BEFORE)||usrFilterPos.equalsIgnoreCase(IterKeys.BEFORE_AND_AFTER)))
			{
%>
				<%@ include file="components/teaser_user_filter.jspf" %>
<%				
			}
%>
			<%@ include file="components/teaser_container_start.jspf" %>
			
			<%@ include file="components/teaser_content.jspf" %>
			
			<%@ include file="components/teaser_container_end.jspf" %>
<%	
			if(  !usrFilterBy.equalsIgnoreCase(IterKeys.WITHOUT_FILTER) &&
			 	(usrFilterPos.equalsIgnoreCase(IterKeys.AFTER)||usrFilterPos.equalsIgnoreCase(IterKeys.BEFORE_AND_AFTER ) )  )
			{
%>
				<%@ include file="components/teaser_user_filter.jspf" %>
<%				
			}			

			if(!isNewsletterPage)
			{
%>
				</div>
<%		
			}
%>					
		</c:if>

		<c:if test="<%= paged %>">

		<%--Con paginación--%>
		
		
<%
			//Solo en caso de paginacion se guarda el themeDisplay para dejarlo accesible a la vista AJAX
			ThemeDisplayCacheUtil.putPortletRequest(PortalUtil.getPortletId(request), themeDisplay);

			//filterBy y filterOpt son "" para indicar que se llega a teaser_paged_content desde teaser_render.jsp
			String filterBy 		= "";
			String filterOpt 		= "";
		
			//Elementos paginacion
			int endPagination;
			if ((teasertotalcount % teaserPerPage) != 0){
				endPagination = (teasertotalcount / teaserPerPage) + 1;
			}
			else{
				endPagination = teasertotalcount / teaserPerPage;
			}
			
			if( !usrFilterBy.equalsIgnoreCase(IterKeys.WITHOUT_FILTER) )
			{
				teaserData = TeaserContentUtil.encodeTeaserConfigParams(portletItem, refPreferenceId, portletId, contentId, TeaserContentUtil.getURLParamFromCategories(categoryIds),
						dateForURLFilter, teasertotalcount, themeDisplay.getScopeGroupId(), themeDisplay.getCompanyId(), themeDisplay.getLanguageId(),
						plidValue, sectionPlid, themeDisplay.isSecure(), themeDisplay.getPermissionChecker().getUserId(), themeDisplay.isLifecycleRender(),
						themeDisplay.getPathFriendlyURLPublic(), themeDisplay.getPathFriendlyURLPrivateUser(), themeDisplay.getPathFriendlyURLPrivateGroup(),
						themeDisplay.getServerName(), themeDisplay.getCDNHost(), themeDisplay.getPathImage(), themeDisplay.getPathMain(), themeDisplay.getPathContext(),
						themeDisplay.getURLPortal(), themeDisplay.getPathThemeImages(), themeDisplay.getServerPort(), scheme , includeCurrentContent);
			}
			
			if( !usrFilterBy.equalsIgnoreCase(IterKeys.WITHOUT_FILTER) && (usrFilterPos.equalsIgnoreCase(IterKeys.BEFORE)||usrFilterPos.equalsIgnoreCase(IterKeys.BEFORE_AND_AFTER)))
			{
				%>
					<%@ include file="components/teaser_user_filter.jspf" %>
				<%
			}
%>
			
			<div id="<portlet:namespace />teaser_paged">
				<%@ include file="components/teaser_paged_content.jspf" %>
			</div>				
<%
			
			if( !usrFilterBy.equalsIgnoreCase(IterKeys.WITHOUT_FILTER) && (usrFilterPos.equalsIgnoreCase(IterKeys.AFTER)||usrFilterPos.equalsIgnoreCase(IterKeys.BEFORE_AND_AFTER)))
			{
%>
					<%@ include file="components/teaser_user_filter.jspf" %>
			<%				
			}			
%>	

			<div class="spacer <%=HtmlUtil.getRemovableClass()%>"></div>

		</c:if>
		
		<c:if test="<%= !usrFilterBy.equalsIgnoreCase(IterKeys.WITHOUT_FILTER) %>">	
		<%
			/*si hay filtro, se comprueba si PublicIterParams tiene el atributo que indica que se muestren los comentarios
				(el set del atributo se hará en 'PageContentLocalServiceUtil.getArticleContent()', esto es en, 'teaser_content.jspf' para no paginados
						y 'teaser_paged_content.jspf' para paginados  ).
						Hay que tener en cuenta que aunque el teaser actual no tenga que mostrar comentarios,en el momento que haya un sólo teaser con comentarios en la página y se haya pintado antes que el teaser actual
						,se habrá echo el set del atributo*/
			Object showDisqusCountAttr_filterSelect = PublicIterParams.get(TeaserUtil.REQUEST_ATTRIBUTE_SHOW_DISQUS_COUNT);
			long   scopegroupid = -2;
		
			if ( GetterUtil.getBoolean(String.valueOf(showDisqusCountAttr_filterSelect)) ||
				 JournalArticleTools.showArticleVisits())
		 	{
				scopegroupid = themeDisplay.getScopeGroupId();
		 	}
		
			if( usrFilterBy.equalsIgnoreCase(IterKeys.SECTIONS) || usrFilterBy.equalsIgnoreCase(IterKeys.CATEGORIES) )
			{
		%>
				<script>			
					jQryIter( "#<portlet:namespace/>" ).bind(
							'click',
							function ()
							{
								var el = jQryIter(this);
								jQryIter.handClickFilterSelector(el, '<%= usrFilterBy %>', '<%= scopegroupid %>');
							}
					);
				</script>
		<%
			}
			else if( usrFilterBy.equalsIgnoreCase( IterKeys.DATE )  )
			{
				/*se modifica el html generado en'teaser_user_filter.jspf' añadiendo scopegroupid como parámetro. 
					'scopegroupid' será utilizado en el momento de instanciar cada datepicker para mostrar o no comentarios tras filtrar */
		%>
				<script>			
					jQryIter("#<portlet:namespace/>itrchosen-dp-reg").data("scopegroupid",'<%= scopegroupid %>');
				</script>	
		<%
			}
		%>
			
		</c:if>
		
	</c:otherwise>
	
</c:choose>

<%
		}
	}
%>
