<%--
/**
* Copyright (c) 2000-2010 Liferay, Inc. All rights reserved.
*
* This library is free software; you can redistribute it and/or modify it under
* the terms of the GNU Lesser General Public License as published by the Free
* Software Foundation; either version 2.1 of the License, or (at your option)
* any later version.
*
* This library is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
* details.
*/
--%>

<%@page import="com.liferay.portal.kernel.util.sectionservers.SectionServersMgr"%>
<%@page import="com.liferay.portal.kernel.util.request.IterRequest"%>
<%@page import="com.liferay.portal.kernel.util.IterGlobal"%>
<%@page import="com.liferay.portal.kernel.util.IterURLUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.xml.SAXReaderUtil"%>
<%@page import="com.liferay.portal.kernel.xml.XMLHelper"%>
<%@page import="com.liferay.portal.kernel.xml.Node"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.protecmedia.iter.news.util.SectionMetadataControlUtil"%>
<%@page import="com.liferay.portal.kernel.util.LocaleUtil"%>
<%@page import="java.util.Locale"%>
<%@page import="com.liferay.portal.kernel.language.LanguageUtil"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.protecmedia.iter.news.util.CategoryMetadataControlUtil"%>
<%@page import="com.liferay.portal.kernel.util.IMetadataControlUtil"%>
<%@page import="com.protecmedia.iter.news.util.TopicsUtil"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.kernel.xml.Element"%>
<%@page import="com.protecmedia.iter.news.util.RobotsControlUtil"%>
<%@page import="com.liferay.portal.kernel.xml.Document"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.kernel.xml.DocumentException"%>
<%@page import="com.liferay.portal.kernel.exception.SystemException"%>
<%@page import="com.liferay.portal.kernel.exception.PortalException"%>
<%@page import="com.liferay.portal.kernel.servlet.SessionErrors"%>
<%@page import="com.liferay.portal.kernel.util.KeyValuePair"%>
<%@page import="com.protecmedia.iter.news.util.MetadataValidator"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.MetadataControlUtil"%>
<%@page import="com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil"%>
<%@page import="com.liferay.portlet.journal.model.JournalArticle"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.protecmedia.iter.news.service.PageContentLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>

<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%!
private static Log _log = LogFactoryUtil.getLog("news-portlet.docroot.html.metadata-page-portlet.viewMetadataPage_jsp");
%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<liferay-util:html-bottom>

	<%
if (!themeDisplay.isWidget() && !themeDisplay.isWidgetFragment())
{
		long globalGroupId 		 = company.getGroup().getGroupId();
		boolean isDateOrMetaURL  = false;
		String urlSeparator 	 = PortalUtil.getUrlSeparator();
		String contentId 		 = null;
		String _url 			 = themeDisplay.getURLCurrent();
		String portletID		 = (String)renderRequest.getAttribute(WebKeys.PORTLET_ID);
		boolean isSectionLayout		 = false;
		IMetadataControlUtil metaUtil = null;
		
		List<String> categoryIds = new ArrayList<String>();
		
		if ( _url.contains( urlSeparator ) )
		{
			int pos = _url.indexOf( urlSeparator );
			if(pos!=-1)
			{
				String auxUrl = _url.substring(pos+urlSeparator.length());
				int endIdx = auxUrl.indexOf("/")!=-1 ? auxUrl.indexOf("/") : auxUrl.length();
				if ( !auxUrl.substring(0, endIdx).equals("date") && !auxUrl.substring(0, endIdx).equals("meta") )
				{
					contentId = request.getParameter(WebKeys.URL_PARAM_CONTENT_ID);
					
					if ((contentId == null || contentId.equals("")) && themeDisplay.isSignedIn() && (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))) 
					{
						contentId = IterKeys.EXAMPLEARTICLEID;
					}
				}
				else if (auxUrl.substring(0, endIdx).equals("meta"))
				{
					List<String> tmpCategoryIds = TopicsUtil.getCategoriesIds(renderRequest);

					if (! (categoryIds != null && categoryIds.size() == 1 && categoryIds.get(0).equals("-1")) )
						categoryIds = tmpCategoryIds;
				}
			}
		}
		else 
			if( !_url.contains( PortalUtil.getSearchSeparator() ) )
				isSectionLayout = (layout!=null);

	%>
   
   <c:if test='<%= !categoryIds.isEmpty() || (contentId != null && !contentId.equals("")) || isSectionLayout%>'>	
   <%
   		// Es una página de detalle de un artículo, de detalle de categoría o una página de sección
	   	try
	    {
	   		Document docMetaRobots		 = null;

	   		if(!categoryIds.isEmpty())
	   		{
	   			_log.debug("CategoryMetadataControlUtil");
	   			
	   			// Detalle de categoría
	   			metaUtil 		= ((IMetadataControlUtil) new CategoryMetadataControlUtil(themeDisplay, categoryIds));
	   		}
	   		else
	   		{
	   			if(contentId != null && !contentId.equals(""))
		   		{
	   				_log.debug("MetadataControlUtil");
	   				
		   			// Detalle de artículo
		   			JournalArticle webContent = JournalArticleLocalServiceUtil.getArticle(globalGroupId, contentId);
		   			metaUtil 		= ((IMetadataControlUtil) new MetadataControlUtil(themeDisplay, webContent, locale.toString()));
		   			try
		   			{
		   	   			docMetaRobots 	= new RobotsControlUtil().getMetaRobots(webContent, themeDisplay, request);
		   			}
		   			catch (Throwable th)
		   			{
		   				_log.error(th);
		   			}
		   		}
	   			else if(isSectionLayout)
	   			{
	   				// Página de sección
	   				_log.debug("SectionMetadataControlUtil");
		   			metaUtil = ( (IMetadataControlUtil) new SectionMetadataControlUtil(themeDisplay, request, layout, (contentId != null && !contentId.equals(""))) );
	   			}
	   		}
	   			   		
	   		if(metaUtil != null)
	   		{
	   	   		String pageTitle = metaUtil.getPageTitle();
	   	   		if (!pageTitle.isEmpty())
	   	   			PortalUtil.setPageTitle( pageTitle, request );
	   	   		
	   	   		String pageDescription = metaUtil.getPageDescription();
	   	   		if (!pageDescription.isEmpty())
	   	   			PortalUtil.setPageDescription( pageDescription,	request );
	   	   		
	   	   		String pageKeywords = metaUtil.getPageKeywords();
	   	   		if (!pageKeywords.isEmpty())
	   	   			PortalUtil.addPageKeywords(pageKeywords, request);
	   	   		
	   	   		Document doc = metaUtil.getPageOpenGraphs(request);
	   	   		
	   	   		//ticket: 0009741. Añadir '<link rel="canonical"...' en las páginas de sección y de metadatos
	   	   		if( !categoryIds.isEmpty() || isSectionLayout )
	   	   		{
	   	   			//Página de sección o detalle de categoría. 
	   	   			
	   	   			//se extrae la url canonica de doc 
	   	   			String canonicalURL = XMLHelper.getStringValueOf( doc, "/rs/meta[@namevalue='og:url']/content");
	   	   			
	   	   			//si layout es la sección home
	   	   			String host = IterURLUtil.getIterHost();
	   	   			
	   	   			String isLayoutHome =  (String) request.getAttribute( WebKeys.IS_LAYOUT_HOME );
	   	   			if( Validator.isNotNull(isLayoutHome) && isLayoutHome.equals("true") )
	   	   			{
	   	   				canonicalURL = host.concat(IterURLUtil.getIterURLPrefix());
	   	   			}
	
	   	   			
	   	   			//se rellena docMetaRobots con la información de la url canónica
	   	   			docMetaRobots  	 = 	SAXReaderUtil.createDocument();
	   	 			Element eleRS	 = 	docMetaRobots.addElement("rs");
	   	 			
	   	 			Element elem = eleRS.addElement("link");
	   			
	   				elem.addAttribute( "rel", "canonical" );
	   				elem.addAttribute( "href", canonicalURL );
	   	   			
	   				if( IterRequest.isMobileRequest()==0 && IterGlobal.getMobileToken(themeDisplay.getScopeGroupId())!="" )
	   				{
	   					String mobileURL = SectionServersMgr.processMobileURL(host, IterURLUtil.buildMobileURL());
	   					
	   					if( !mobileURL.equalsIgnoreCase(canonicalURL) )
	   					{
		   					elem = eleRS.addElement("link");
		   					
		   					elem.addAttribute( "rel", "alternate" );
			   				elem.addAttribute( "href", mobileURL );
			   				elem.addAttribute( "media", "only screen and (max-width: 640px)");
	   					}
	   				}
	   	   		}
	   	   		if (docMetaRobots != null)
	   	   		{
	   	   			Element elemRoot	= docMetaRobots.getRootElement();
	   	   			Element docRoot		= doc.getRootElement();
	   	   			List<Element> eList = elemRoot.elements();
	   	   			
	   	   			for (Element e : eList)
	   	   			{
	   	   			 	docRoot.add(e.createCopy());
	   	   			}
	   	   		}
		   	   		
	   	   		_log.debug(doc.asXML());
	   	   		PortalUtil.setPageOpenGraphs( doc.asXML(),	request );
	   		}
   		}
  		catch(PortalException e)
   		{
  			if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
  			{
		   		SessionErrors.add(renderRequest, MetadataValidator.getKey(MetadataValidator.ERR_INVALID_JOURNALSTRUCT, 	portletID));
  			}
  			_log.error(e);
   		}
   		catch(SystemException e)
   		{
   			if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
   			{
		   		SessionErrors.add(renderRequest, MetadataValidator.getKey(MetadataValidator.ERR_INVALID_JOURNALSTRUCT, 	portletID));
   			}
   			_log.error(e);
   		}
   		catch(DocumentException e)
   		{
   			if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
   			{
		   		SessionErrors.add(renderRequest, MetadataValidator.getKey(MetadataValidator.ERR_INVALID_JOURNALSTRUCT_DOM, portletID));
   			}
   			_log.error(e);
   		}
  		catch(Exception e)
  		{
  			_log.error(e);
  		}
	      

   		// Se pasan al PortletRequest los errores del HttpRequest
   		MetadataValidator.validateRequest(request, renderRequest, portletID);

   		// Se añaden todos los errores de este portlet
   		KeyValuePair[] list = MetadataValidator.getErrors(renderRequest, portletID);
   		
   		for (KeyValuePair pair : list)
   		{
	   		%>
   	        <liferay-ui:error key='<%=pair.getKey()%>' message="<%=pair.getValue()%>" />
   	    	<%
   		}
   		
   		// Se añaden los mensajes
   		list = MetadataValidator.getSuccess(renderRequest, portletID);
   		for (KeyValuePair pair : list)
   		{
   	        %>
   	        <liferay-ui:success key='<%=pair.getKey()%>' message="<%=pair.getValue()%>" />
   	    	<%
   		}
   	%>
	</c:if>
	
	<c:if test='<%= layout!=null %>'>
	<%
		SectionMetadataControlUtil sm = null;	
		if( metaUtil instanceof SectionMetadataControlUtil)
			sm = (SectionMetadataControlUtil)metaUtil;
		else
			sm = new SectionMetadataControlUtil(themeDisplay, request, layout, (contentId != null && !contentId.equals("")));
  		
  		PortalUtil.setPageMetaTags( PortalUtil.getOriginalServletRequest(request), sm.getCommonMetaTags().asXML() );
	%>
	</c:if>
 
<%
} //if (!themeDisplay.isWidget() && !themeDisplay.isWidgetFragment())
%>
</liferay-util:html-bottom>
