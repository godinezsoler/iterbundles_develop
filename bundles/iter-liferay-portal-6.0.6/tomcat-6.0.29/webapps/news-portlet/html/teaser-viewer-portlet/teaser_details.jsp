<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@page import="com.liferay.portlet.journal.util.RelatedLinkData"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>

<%
	Log _log = LogFactoryUtil.getLog("news-portlet.docroot.html.teaser-viewer-portlet.teaser_details.jsp");
%>

<%
	if( showRelatedContent )
	{
		mainCssClass = "teaser-related-list";
		
		//ENLACES INTERNOS Y EXTERNOS
		if( showInternalExternalLink   && ( maxItemsInternalExternalLinks > 0) )
			mainCssClass = mainCssClass.concat( " teaser-related-internal-links teaser-related-external-links" );

		//ENLACES POR METADATOS
		if( showMetadata && (maxItemsMetadata > 0)  )
			mainCssClass = mainCssClass.concat( " teaser-related-metadata"  );
		
		int pageIdx=1;
		
%>
		<%@ include file="components/teaser_container_start.jspf" %>
		
		<c:if test="<%= (showMetadata && (maxItemsMetadata > 0) && metadataPos.equalsIgnoreCase(IterKeys.BEFORE) ) %>">
			<%@ include file="components/teaser_container_metadata_related.jspf" %> 
		</c:if>
			
			
		<c:if test="<%= ( showInternalExternalLink && (maxItemsInternalExternalLinks > 0) ) %>">
		<% 
			List<RelatedLinkData> relatedInternalExternalLinkArticles  = TeaserContentUtil.getRelatedInternalExternalLinks( globalGroupId, teaserContentId, 0, maxItemsInternalExternalLinks );
					
			if( relatedInternalExternalLinkArticles != null && relatedInternalExternalLinkArticles.size() > 0 )
			{
				/* int pageIdx=1; */
						
				for (int j = 0; j < relatedInternalExternalLinkArticles.size(); j++)
				{
					String html = null;
							
					RelatedLinkData articleData = relatedInternalExternalLinkArticles.get(j);
		
					if(articleData != null)
					{
						if(  articleData.getLinkType().equals("InternalLink")   )
						{
							//ENLACE INTERNO
			%>
		 	 				 <%@ include file="components/teaser_container_internal_related.jspf"%> 
									
			<%			}
						else if( articleData.getLinkType().equals("ExternalLink")  )
						{
							//ENLACE EXTERNO
			%>
							 <%@ include file="components/teaser_container_external_related.jspf" %> 
			<%
						}
					}
				}
									
			}
									
			%>
		</c:if>
		
		<c:if test="<%= (showMetadata && (maxItemsMetadata > 0) &&  metadataPos.equalsIgnoreCase(IterKeys.AFTER) ) %>">
			<%@ include file="components/teaser_container_metadata_related.jspf" %> 
		
		</c:if>
		
		<%@ include file="components/teaser_container_end.jspf" %>
<%
	
	}
%>	

<div class="spacer <%= HtmlUtil.getRemovableClass()%>"></div>		
