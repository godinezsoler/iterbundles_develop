<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@page import="com.liferay.portal.util.PortletKeys"%>
<%@page import="com.protecmedia.iter.tracking.util.TrackingUtil"%>
<%@page import="com.liferay.portal.kernel.util.PropsValues"%>
<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil"%>
<%@page import="com.liferay.portlet.journal.model.JournalArticle"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@ include file="init.jsp" %>

<script type="text/javascript">
	function showRedStar(star_number)
	{
		hideRedStar();
		for(star_ctr = 1; star_ctr <= star_number; star_ctr++)
		{
		    jQryIter("a span[name='star-rating-" + star_ctr + "']").each(function(){
		    	jQryIter(this).removeClass('star-grey').addClass('star-red');
		    });
		}
	}
	
	function hideRedStar()
	{
		for(star_ctr = 5; star_ctr >= 1; star_ctr--)
		{
			jQryIter("a span[name='star-rating-" + star_ctr + "']").each(function(){
				jQryIter(this).removeClass('star-red').addClass('star-grey');
			});
		}
	}
</script>

<%
	String contentId = renderRequest.getParameter(WebKeys.URL_PARAM_CONTENT_ID);
	String productsList = PortalLocalServiceUtil.getProductsByArticleId(contentId);

	String cookieAlready = String.format(IterKeys.COOKIE_NAME_ALREADY_RATED, contentId);
	String servletPath = TrackingUtil.getportletName(request);
	
	servletPath = servletPath.concat("/ratings");
	String path =  servletPath + "/" + themeDisplay.getScopeGroupId() + "/" + contentId;
	
	//Modo Apache
	if(PHPUtil.isApacheRequest(request))
	{
		if(show.equals( PortletKeys.PREFS_SHOW_ALWAYS))
		{
			PublicIterParams.set(WebKeys.ITER_RESPONSE_NEEDS_PHP, true);
%>			
			<%@ include file="ratings.jsp"%>
			<%@ include file="allowvote_apache.jspf"%>
			
<%		}
		else if(show.equals( PortletKeys.PREFS_SHOW_ACCESS))
		{
			PublicIterParams.set(WebKeys.ITER_RESPONSE_NEEDS_PHP, true);
			StringBuilder contentHTML = new StringBuilder();
			
		    if(Validator.isNotNull(productsList))
		    {
		    	JournalArticle journalArticle = JournalArticleLocalServiceUtil.getArticle(globalGroupId, contentId);		
		    	
		    	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(WebKeys.URL_PARAM_DATE_FORMAT_FULL);
		    	String modifiedDate = simpleDateFormat.format(journalArticle.getModifiedDate());
		    	
		    	out.print(" <?php if (user_has_access_to_any_of_these_products( '" + contentId 		+ 	"', array("			+ 
																					 productsList 	+ 	"),'" 				+ 
																					 modifiedDate 	+ 	"' )===true) { ?> "); 
		    	
%>
		       	<%@ include file="ratings.jsp"%>
		        <%@ include file="scripts_full.jsp"%>
<%
	       		out.print(" <?php } ?> ");
			}
		    else 
		    {
		    	//Restringido con productos vacíos. Se muestra el portlet. Se puede valorar si: se permiten valoraciones anónimas o se está presentado
%>
		        <%@ include file="ratings.jsp"%>
		       	<%@ include file="allowvote_apache.jspf"%>
<%
		    }
		}	
		else if(show.equals( PortletKeys.PREFS_SHOW_REGISTERED))
		{
			PublicIterParams.set(WebKeys.ITER_RESPONSE_NEEDS_PHP, true);

			out.print( "<?php if (user_is_signedin()===true){ ?>");
%>			
			<%@ include file="ratings.jsp"%>
			<%@ include file="scripts_full.jsp"%>	
<%	
			out.print(" <?php" +" } ?> ");
		}
		else if(show.equals(PortletKeys.PREFS_SHOW_NOT4REGISTERED))
		{
			PublicIterParams.set(WebKeys.ITER_RESPONSE_NEEDS_PHP, true);
			out.print( "<?php if (user_is_signedin()===false){ ?>");
			//si no es un usuario registrado se muestra, pero solo se puede valorar si se permiten valoraciones anónimas
%>
			<%@ include file="ratings.jsp"%>
<%	
				out.print( "<?php if (" + allowAnonymousVote +" ===true ) { ?>" );
%>			
				<%@ include file="scripts_full.jsp"%>
<%				
				out.print(" <?php }else{ ?> ");
%>			
				<%@ include file="scripts_restricted.jsp"%>
<%
				out.print(" <?php" +" } ?> ");
			out.print(" <?php" +" } ?> ");
		}
		else if(show.equalsIgnoreCase(PortletKeys.PREFS_SHOW_SUBSCRIBER))
		{
			String[] products  = renderRequest.getPreferences().getValues(PortletKeys.PREFS_SUBSCRIPTIONS4SHOW, null);
			if(products!=null && products.length>0)
			{
				PublicIterParams.set(WebKeys.ITER_RESPONSE_NEEDS_PHP, true);
				String productList = StringUtils.join(PortalLocalServiceUtil.getProductsByProductsId( products ).iterator(), "','");
				if(Validator.isNotNull(productList))
				{
					out.print("<?php if (user_has_access_to_any_of_these_products( NULL, array('" + productList + "'), NULL )===true) { ?>");
	%>
					<%@ include file="ratings.jsp"%>
					<%@ include file="scripts_full.jsp"%>
	<%
					out.print(" <?php } ?> ");
				}
			}
		}
		else if (show.equals(PortletKeys.PREFS_SHOW_NOT4SUBSCRIBER))
		{
			String[] products  = renderRequest.getPreferences().getValues(PortletKeys.PREFS_SUBSCRIPTIONS4NOTSHOW, null);
			if(products!=null && products.length>0)
			{
				PublicIterParams.set(WebKeys.ITER_RESPONSE_NEEDS_PHP, true);
				String productList = StringUtils.join(PortalLocalServiceUtil.getProductsByProductsId( products ).iterator(), "','");
				if(Validator.isNotNull(productList))
				{
					out.print("<?php if (user_has_access_to_any_of_these_products( NULL, array('" + productList + "'), NULL )===false) { ?>");
	%>
					<%@ include file="ratings.jsp"%>
					<%@ include file="allowvote_apache.jspf"%>
	<%
					out.print(" <?php } ?> ");
				}	
			}
		}
		
	}
	//Modo simulado
	else
	{
		if(show.equals( PortletKeys.PREFS_SHOW_ALWAYS))
		{
%>			
			<%@ include file="ratings.jsp"%>
			<%@ include file="allowvote.jspf"%>
			
<%		}	
		else if(show.equals( PortletKeys.PREFS_SHOW_ACCESS))
		{
			 if(Validator.isNotNull(productsList))
			 {
				boolean hasSimulatedAccessToArticle = PortalLocalServiceUtil.hasSimulatedAccessToArticle(contentId, request);
				if(hasSimulatedAccessToArticle)
				{
%>
					<%@ include file="ratings.jsp"%>
		        	<%@ include file="scripts_full.jsp"%>      
<%
				}
			 }
			 else
			 {
				//Restringido con productos vacíos. Se muestra el portlet. Se puede valorar si: se permiten valoraciones anónimas o se está presentado
%>
				 <%@ include file="ratings.jsp"%>
				 <%@ include file="allowvote.jspf"%>
<%
			 }
		}
		else if(show.equals( PortletKeys.PREFS_SHOW_REGISTERED))
		{
			if( PortalLocalServiceUtil.getIterProductList(request) != null)
			{
%>
				<%@ include file="ratings.jsp"%>
				<%@ include file="scripts_full.jsp"%>	
<%
			}
		}
		else if(show.equals(PortletKeys.PREFS_SHOW_NOT4REGISTERED))
		{
			if( PortalLocalServiceUtil.getIterProductList(request) == null)
			{
%>				
				<%@ include file="ratings.jsp"%>
<%	
				if (allowAnonymousVote) 
				{
%>
					<%@ include file="scripts_full.jsp"%>
<%	
				}
				else
				{
%>				
					<%@ include file="scripts_restricted.jsp"%>
<%
				}

			}
		}
		else if(show.equals(PortletKeys.PREFS_SHOW_SUBSCRIBER))
		{
			String[] products = renderRequest.getPreferences().getValues(PortletKeys.PREFS_SUBSCRIPTIONS4SHOW, null);
			if(products!=null && products.length>0)
			{
				boolean hasProducts = PortalLocalServiceUtil.hasSuscriptions( products, request );
				if(hasProducts)
				{
	%>
					<%@ include file="ratings.jsp"%>
					<%@ include file="scripts_full.jsp"%>
	<%
				}
			}
		}
		else if (show.equals(PortletKeys.PREFS_SHOW_NOT4SUBSCRIBER))
		{
			String[] products  = renderRequest.getPreferences().getValues(PortletKeys.PREFS_SUBSCRIPTIONS4NOTSHOW, null);
			if(products!=null && products.length>0)
			{
				boolean hasProducts = PortalLocalServiceUtil.hasSuscriptions( products, request );
				if(!hasProducts)
				{
	%>
					<%@ include file="ratings.jsp"%>
					<%@ include file="allowvote.jspf"%>
	<%
				}
			}
		}
		
	}
%>