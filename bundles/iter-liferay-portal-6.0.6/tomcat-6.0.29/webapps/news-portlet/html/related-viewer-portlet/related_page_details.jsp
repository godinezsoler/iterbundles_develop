<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.liferay.portal.kernel.util.ArrayUtil"%>
<%@page import="com.liferay.portlet.journal.util.RelatedLinkData"%>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.liferay.portal.kernel.util.HtmlUtil"%>

<%
if( layoutIds!=null )
{	
	String layoutUUID = (themeDisplay.getLayout() != null) ? themeDisplay.getLayout().getUuid() : "";

	//Lista de resultados
	List<String[]> listResultPage = new ArrayList<String[]>();
	List<RelatedLinkData> listRLDResultPage = new ArrayList<RelatedLinkData>();
	int teaserpagetotalcount = 0;
	int startItem = firstItem;
	
	if(relatedBy.equalsIgnoreCase("internallink"))
	{	
		if( ArrayUtil.contains( orderBy, Integer.toString(IterKeys.ORDER_MLN) ))
		{
			 /* startItem indica el index del artículo que debe aparecer en primer lugar, teniendo en cuenta el número de noticias de cada página...  */
			/* Si globalFirstItem > 0 se ha indicado que la primera noticia sea la que está en la posición globalFirstItem, por tanto hay que sumar ese valor a firstItem */
			 if(  globalFirstItem > 0 )
				 startItem = firstItem + globalFirstItem;
			
			//se controla que si nº de resultados por página es mayor que el nº total de artículos a mostrar,se muestre el nº total de artículos indicado
			 if( teaserPerPage > globalLastItem   )
				 listRLDResultPage = TeaserContentUtil.getInternalLinksOrderMLN(globalGroupId, contentId, startItem, globalLastItem );
			 else
				 listRLDResultPage = TeaserContentUtil.getInternalLinksOrderMLN(globalGroupId, contentId, startItem, teaserPerPage);
			
		}
		else
		{
			List<String[]> partialListInternalLinks = TeaserContentUtil.getInternalLinks(themeDisplay.getScopeGroupId(), contentId, structures, 
																			globalFirstItem, globalLastItem, currentDate, 
																			showNonActiveContents, orderBy, orderByType, 
																			firstItem + "," + (lastItem-firstItem), layoutIds); 
															
			listRLDResultPage  = TeaserContentUtil.getInternalLinksNotOrderMLN( globalGroupId, contentId, partialListInternalLinks );
			
		}
		
		teaserpagetotalcount = (listRLDResultPage != null) ? listRLDResultPage.size() : 0;
	}
	else if(relatedBy.equalsIgnoreCase("externallink"))
	{
		 /* startItem indica el index del artículo que debe aparecer en primer lugar, teniendo en cuenta el número de noticias de cada página...  */
		/* Si globalFirstItem > 0 se ha indicado que la primera noticia sea la que está en la posición globalFirstItem, por tanto hay que sumar ese valor a firstItem */
		 if(  globalFirstItem > 0 )
			 startItem = firstItem + globalFirstItem;
		
		//se controla que si nº de resultados por página es mayor que el nº total de artículos a mostrar,se muestre el nº total de artículos indicado
		if( teaserPerPage > globalLastItem   )
			listRLDResultPage = TeaserContentUtil.getExternalLinkContent2(globalGroupId, contentId,  startItem, globalLastItem);
		else
		 	listRLDResultPage = TeaserContentUtil.getExternalLinkContent2(globalGroupId, contentId,  startItem, teaserPerPage);
		
		//listResultObjPage = TeaserContentUtil.getExternalLinkContent(globalGroupId, contentId, firstItem, lastItem);
		
		teaserpagetotalcount = (listRLDResultPage != null) ? listRLDResultPage.size() : 0;
		
	}
	else if( relatedBy.equalsIgnoreCase("internalExternallink")   )
	{
		 /* startItem indica el index del artículo que debe aparecer en primer lugar, teniendo en cuenta el número de noticias de cada página...  */
		/* Si globalFirstItem > 0 se ha indicado que la primera noticia sea la que está en la posición globalFirstItem, por tanto hay que sumar ese valor a firstItem */
		 if(  globalFirstItem > 0 )
			 startItem = firstItem + globalFirstItem;
		
		//se controla que si nº de resultados por página es mayor que el nº total de artículos a mostrar,se muestre el nº total de artículos indicado
		if( teaserPerPage > globalLastItem   )
			 listRLDResultPage = TeaserContentUtil.getRelatedInternalExternalLinks(globalGroupId, contentId,  startItem, globalLastItem);
		else
			 listRLDResultPage = TeaserContentUtil.getRelatedInternalExternalLinks(globalGroupId, contentId,  startItem, teaserPerPage);
		
		
		teaserpagetotalcount = (listRLDResultPage != null) ? listRLDResultPage.size() : 0;
	}
	else if(relatedBy.equalsIgnoreCase("metadata"))
	{
		listResultPage = TeaserContentUtil.getMetadatasLinks(themeDisplay.getCompanyId(), themeDisplay.getScopeGroupId(), contentId, structures, 
														 	 globalFirstItem, globalLastItem, currentDate, 
														 	 showNonActiveContents, orderBy, orderByType, 
														 	 firstItem + "," + (lastItem-firstItem), 
														 	 contentVocabularyIds, contentCategoryIds, layoutIds);
		
		teaserpagetotalcount = (listResultPage != null) ? listResultPage.size() : 0;
	}
	
	if (listResultPage != null && listResultPage.size() > 0 || listRLDResultPage != null && listRLDResultPage.size() > 0  )
	{
		request.setAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_TOTAL_COUNT, 		teasertotalcount     );
		request.setAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_CUR_PAGE_COUNT, 	teaserpagetotalcount );
	}
	
%>

<div class="relatedContent">
	<c:if test='<%= (relatedBy.equalsIgnoreCase("internallink")) %>'>
		<<%=HtmlOptimizer.isEnabled() ? "section" : "ul"%> class="teaser-related-list teaser-related-internal-links">
			<%
				if(listRLDResultPage != null && listRLDResultPage.size() > 0)
				{
					int currentIndex = firstItem  + 1;
					int listResultSize 	   = globalLastItem;
					
					for (RelatedLinkData articleData : listRLDResultPage)
					{
						
			%> 
					 	<%@ include file="components/related_container_internal_related.jspf"%> 
					<% 
						
					}
				}
			%>
		</<%=HtmlOptimizer.isEnabled() ? "section" : "ul"%>>
	</c:if>
	<c:if test='<%= (relatedBy.equalsIgnoreCase("externallink")) %>'>
		<<%=HtmlOptimizer.isEnabled() ? "section" : "ul"%> class="teaser-related-list teaser-related-external-links">
			<% 
				if(listRLDResultPage != null && listRLDResultPage.size() > 0)
				{
					int currentIndex = firstItem + 1;

					for (int i = 0; i < listRLDResultPage.size(); i++)
					{
						String currentCssClass = TeaserContentUtil.getCSSClass(currentIndex, globalLastItem) ;	
						RelatedLinkData articleData = listRLDResultPage.get(i);
			%> 
						<%@ include file="components/related_container_external_related.jspf"%> 
			<% 
			
					}
				}
			%>
		</<%=HtmlOptimizer.isEnabled() ? "section" : "ul"%>>
	</c:if>
	<c:if test='<%= (relatedBy.equalsIgnoreCase("internalExternallink") && (resultEnd > 0)) %>'>
		<<%=HtmlOptimizer.isEnabled() ? "section" : "ul"%> class="teaser-related-list teaser-related-internal-links teaser-related-external-links">
	<% 
		if(listRLDResultPage != null && listRLDResultPage.size() > 0)
		{
								
			int currentIndex = firstItem  + 1;
			int listResultSize 	 = globalLastItem;
					
			for ( RelatedLinkData articleData : listRLDResultPage )
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
						String currentCssClass = TeaserContentUtil.getCSSClass(currentIndex, globalLastItem) ;		
			%>	
						<%@ include file="components/related_container_external_related.jspf"%>
					<% 		
				}
			}
		}
						%>
		</<%=HtmlOptimizer.isEnabled() ? "section" : "ul"%>>
	</c:if>
	<c:if test='<%= (relatedBy.equalsIgnoreCase("metadata")) %>'>
		<<%=HtmlOptimizer.isEnabled() ? "section" : "ul"%> class="teaser-related-list teaser-related-metadata">
			<%						
				if(listResultPage != null && listResultPage.size() > 0)
				{
					int currentIndex = firstItem + 1;
					int listResultSize 	   = globalLastItem;
					
					for (int i = 0; i < listResultPage.size(); i++)
					{
						String[] articleData = listResultPage.get(i);
						
			%>		
						<%@ include file="components/related_container_metadata_related.jspf"%>
			<%	
						
					}
				}
			%>
		</<%=HtmlOptimizer.isEnabled() ? "section" : "ul"%>>
	</c:if> 	
</div>

<%
}
%>