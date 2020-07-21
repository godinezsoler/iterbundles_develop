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

<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%@page import="com.protecmedia.iter.base.service.util.CKEditorUtil"%>

<%@ include file="init.jsp" %>

<%

	String htmlForm = "";
	boolean restricted = false;
	boolean noInheritThemeCSS = false;
	String products = null;
	
	if( Validator.isNotNull(formId) && !formId.isEmpty()	 )
	{
		Document rawForm = FormUtil.getFormXml(formId, request, null, null);
		Element root = rawForm.getRootElement();
		XPath xpath = SAXReaderUtil.createXPath("/form/restrictions");
		Node formNode = xpath.selectSingleNode(rawForm);
		
		if (XMLHelper.getTextValueOf(formNode, "@restricted") == "true"){
			restricted = true;
		}
		products = XMLHelper.getTextValueOf(formNode, "@products");
		
		htmlForm = FormUtil.applyXSL(rawForm);
	}
	
	//MODO APACHE
	if(PHPUtil.isApacheRequest(request))
	{
		PublicIterParams.set(WebKeys.ITER_RESPONSE_NEEDS_PHP, true);
		StringBuilder contentHTML = new StringBuilder();

	    if(!restricted)
	    {
	    	//MUESTRA FORMULARIO 
%>
	    	 <%= htmlForm %>
<%
	    }
	    else
	    {
	    	noInheritThemeCSS = true;
	    	if(Validator.isNotNull(products))
	    	{
		    	out.print(" <?php if (user_has_access_to_any_of_these_products(NULL, array(" + products + "), NULL)===true){ ?> "); 
		       	//MUESTRA FORMULARIO 
%>
		       	 <%= htmlForm %>
<% 
		    	out.print(" <?php }else{ ?> ");
		       	
		       	//Muestra mensaje de restringidos 
%>
		       	 <%= restrictedMessage %>
<% 
		    	out.print(" <?php } ?> ");
			}
	    	//Restringido con productos vacios = solo visible si se esta logeado
	    	else
	    	{
		    	out.print(" <?php $usrname = getenv('ITER_USER'); ");
		    	out.print(" if(strlen($usrname)!==0){ ?> ");
		     	//MUESTRA FORMULARIO 
%>
		     	 <%= htmlForm %>
<% 
				out.print(" <?php }else{ ?> ");
				//Muestra mensaje de restringidos 
%>
				<%= restrictedMessage %>
<% 
				out.print(" <?php } ?> ");
	    	}
	    }
	}	
	
	//MODO SIMULADO
	else
	{
	    if(!restricted)
	    {
	    	//MUESTRA FORMULARIO 
%>
	    	 <%= htmlForm %>
<%
	    }
	    else
	    {
	    	noInheritThemeCSS = true;
	    	if(Validator.isNotNull(products))
	    	{
				boolean hasSimulatedAccessToForm = PortalLocalServiceUtil.hasSimulatedAccessToForm(FormUtil.transformToObjectsList(products), request);
				if(hasSimulatedAccessToForm)
				{
					//MUESTRA FORMULARIO 
%>
			       	 <%= htmlForm %>
<%
				}
				else
				{
%>
					<%= restrictedMessage %>
<%
				}
	    	}
	    	else{
	    		if(PortalLocalServiceUtil.getIterProductList(request) != null)
	    		{
%>
	    			<%= htmlForm %>
<%
	    		}
	    		else
	    		{
%>
	    			<%= restrictedMessage %>
<%
	    		}
	    	}
			
	    }
			
	}
	
	if(noInheritThemeCSS)
		CKEditorUtil.noInheritThemeCSS(restrictedMessage, request);
	
%>