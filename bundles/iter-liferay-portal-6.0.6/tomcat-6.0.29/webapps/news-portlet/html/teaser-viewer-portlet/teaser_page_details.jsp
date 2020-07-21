<%@page import="com.liferay.portal.kernel.util.HtmlUtil"%>
<%@page import="com.liferay.portal.kernel.util.PropsValues"%>
<%@ page contentType="text/html; charset=UTF-8" %>

<%		
	
if( layoutIds!=null )
{	
	String layoutUUID = (themeDisplay.getLayout() != null) ? themeDisplay.getLayout().getUuid() : "";

	
	
	String teaserContentId = "";
%>
	<ol class="<%=mainCssClass%>  <%=HtmlUtil.getRemovableClass()%>">
<%			
		if(listResultPage != null && listResultPage.size() > 0)
		{
			

			
			request.setAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_TOTAL_COUNT, 		teasertotalcount);
			request.setAttribute(VelocityContext.VELOCITYVAR_ITER_TEASER_CUR_PAGE_COUNT, 	listResultPage.size());
			
			request.setAttribute( VelocityContext.VELOCITYVAR_ITER_QUALIFICATION, qualificationName );

			for (int i = 0; i < listResultPage.size(); i++) 
			{
				try 
				{

					String[] currentArticle = (String[]) listResultPage.get(i);
					
					JournalArticle teaserContent = JournalArticleLocalServiceUtil.getArticle(globalGroupId, currentArticle[0]);		

					if (teaserContent.isApproved())
					{																		
						teaserContentId = teaserContent.getArticleId();
						
						String faTemplateId = TeaserContentUtil.getTemplateId(teaserContent, templateIdArticle, templateIdGallery, 
																			  templateIdPoll, templateIdMultimedia);
						
						String raTemplateId = TeaserContentUtil.getTemplateId(teaserContent, templateIdArticleRestricted, templateIdGalleryRestricted, 
								  											  templateIdPollRestricted, templateIdMultimediaRestricted);
						
						String viewMode = PageContentLocalServiceUtil.getArticleContextInfo(themeDisplay.getScopeGroupId(), teaserContentId, ctxLayouts);
						
						int templateMode = TeaserContentUtil.getTemplateMode(teaserContent, modeArticle, modeGallery, modePoll, modeMultimedia);
						
						String id = teaserContentId;	
						String idCMS = teaserContentId;
						
						int currentIndex = firstItem + i + 1;
						
						String teaserHTMLContent = PageContentLocalServiceUtil.getArticleContent(teaserContent, faTemplateId, raTemplateId, 
																								 viewMode, themeDisplay, xmlRequest, templateMode, 
																								 request, currentIndex, globalLastItem);
						
						int resultsSize = globalLastItem;
						int resultsPageSize = listResultPage.size();						
%>
						<c:if test='<%= (Validator.isNotNull(teaserHTMLContent)) %>'>	
						
							<%@ include file="components/teaser_container_row.jspf" %>
						
						</c:if>
<%					
					}
				}
				catch (Exception e)
				{
					System.out.println("Error painting paged content");
					e.printStackTrace();
				}							
			}
						
		}
%>		
	<div class="clear <%=HtmlUtil.getRemovableClass()%>" style=""> </div>
</ol>

<%
}
%>