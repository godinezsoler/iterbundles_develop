<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@page import="com.liferay.portal.model.Layout"%>
<%@page import="com.liferay.portal.kernel.util.SectionUtil"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletMgr"%>
<%@ include file="init.jsp" %>

<%
	String environment = IterLocalServiceUtil.getEnvironment();	
	String contentId = renderRequest.getParameter(WebKeys.URL_PARAM_CONTENT_ID);
	String html = null; 
%>

<c:if test="<%= Validator.isNotNull(contentId) %>">	

<%
	//Modo widget (Portlet anfitrión de otro portlet huésped)
	String widgetContent = PortletMgr.getWidgetContent(request, themeDisplay, true);
	
	if(!widgetContent.isEmpty())
	{
		out.print(widgetContent);
	}
	else
	{
		JournalArticle article = JournalArticleLocalServiceUtil.getArticle(globalGroupId, contentId);
		
		try
		{
			String faTemplateId = TeaserContentUtil.getTemplateId(article, templateIdArticle, templateIdGallery, 
																  templateIdPoll, templateIdMultimedia);
			
			String raTemplateId = TeaserContentUtil.getTemplateId(article, templateIdArticleRestricted, templateIdGalleryRestricted,
																  templateIdPollRestricted, templateIdMultimediaRestricted);

			Layout sectionLayout = SectionUtil.getSection(request);
			// Se contempla el caso de un artículo que NO esté asociado a secciones
			// 0010635: Error null en el detalle
			String[] layoutIds = (sectionLayout != null) ? new String[]{sectionLayout.getUuid()} : new String[]{}; 
			String viewMode = PageContentLocalServiceUtil.getArticleContextInfo(scopeGroupId, article.getArticleId(), layoutIds);

			String xmlRequest = PortletRequestUtil.toXML(renderRequest, renderResponse);

			int templateMode = TeaserContentUtil.getTemplateMode(article, modeArticle, modeGallery, modePoll, modeMultimedia);
			
			html = PageContentLocalServiceUtil.getArticleContent(article, faTemplateId, raTemplateId, viewMode, 
																 themeDisplay, xmlRequest, templateMode, request, 1, 1);	
%>

<%
		}
		catch(Exception e)
		{
			System.out.println("Error al pintar el detalle del contenido");
			e.printStackTrace();
		}
		finally
		{
			if( (Validator.isNull(html) )  )
			{
				if(  showDefaultTextHTML && !defaultTextHTML.equals("")   )
				{
					html= "<div>" + defaultTextHTML + "</div>";							
				}
				else if( environment.equals( IterKeys.ENVIRONMENT_PREVIEW )  )
					html = "CONTENT EMPTY";
			}	
		}
		
%>
		<div class="last td-viewer <%= TeaserContentUtil.getCSSAccessClass(article, request) %>"><%= html %></div>
<%
	}
%>
	
</c:if>
