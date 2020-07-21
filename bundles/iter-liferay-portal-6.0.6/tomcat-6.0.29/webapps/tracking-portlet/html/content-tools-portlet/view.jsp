<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.service.LayoutSetLocalServiceUtil"%>
<%@page import="com.liferay.portal.service.PortalLocalServiceUtil"%>
<%@ include file="init.jsp"%>		

<%
String contentId = renderRequest.getParameter(WebKeys.URL_PARAM_CONTENT_ID);

if(PHPUtil.isApacheRequest(request))
{
	String productsList = PortalLocalServiceUtil.getProductsByArticleId(contentId);
    StringBuilder contentHTML = new StringBuilder();
    if(Validator.isNotNull(productsList))
    {
    	JournalArticle journalArticle = JournalArticleLocalServiceUtil.getArticle(globalGroupId, contentId);
    	
    	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(WebKeys.URL_PARAM_DATE_FORMAT_FULL);
    	String modifiedDate = simpleDateFormat.format(journalArticle.getModifiedDate());
    	
    	PublicIterParams.set(WebKeys.ITER_RESPONSE_NEEDS_PHP, true);

        out.print("<?php" + " if (user_has_access_to_any_of_these_products( '" + contentId 		+ 	"', array("			+ 
																				 productsList 	+ 	"),'" 				+ 
																				 modifiedDate 	+ 	"' )===true){ " + "?>"); 
        %>
        <%@ include file="call-content-tools.jsp" %>
        <%
        out.print(" <?php" +" } ?> ");
	}
    else
    {
    	%>
		<%@ include file="call-content-tools.jsp" %>
		<%
    }
}
else
{
//Modo simulado
		boolean hasSimulatedAccessToArticle = PortalLocalServiceUtil.hasSimulatedAccessToArticle(contentId, request);
		if(hasSimulatedAccessToArticle)
		{
			%>
			<%@ include file="call-content-tools.jsp" %>
			<%
		}
}
%>