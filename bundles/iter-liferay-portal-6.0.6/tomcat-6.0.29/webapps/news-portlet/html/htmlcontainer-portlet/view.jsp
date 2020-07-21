
<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%@page import="com.liferay.portal.service.PortalLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.xml.SAXReaderUtil"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>

<%@ include file="init.jsp" %>

<%!

private static Log _logHtmlContainer = LogFactoryUtil.getLog("news-portlet.docroot.html.htmlcontainer-portlet.view.jsp");

private String replaceHTML(HttpServletRequest request, String scopeGroupFriendlyName, String txt) throws ServiceError
{
	HttpServletRequest original_request = PortalUtil.getOriginalServletRequest( request );
	
	if( ContextVariables.ctxVarsEnabled(scopeGroupFriendlyName) && ContextVariables.findCtxVars(txt) )
	{
		Map<String, String> globalCtxVars =  AdvertisementUtil.getAdvertisementCtxVars(request);
		ErrorRaiser.throwIfNull(globalCtxVars);
		
		txt = ContextVariables.replaceCtxVars(txt, globalCtxVars);
	}

	txt = ContextVariables.replaceCtxVars(txt, ContextVariables.getRequestCtxVars(original_request, IterKeys.SUBSCRIPTION_SYSTEM_CONTEXT_VARS));
	
	if( ContextVariables.findPHPCtxVars(txt) )
	{
		if(PHPUtil.isApacheRequest(request))
		{
			PublicIterParams.set(WebKeys.ITER_RESPONSE_NEEDS_PHP, true);
			txt = ContextVariables.replaceCtxVars(txt, ContextVariables.getPhpCtxVars());
		}
		else
		{
			txt = ContextVariables.replaceCtxVars(txt, ContextVariables.getPhpCtxVars(), true);
		}
	}
	
	return txt;
}

%>


<%
	
	if( PHPUtil.isApacheRequest(request) )
	{
%>
		<%@ include file="call-htmlcontainer.jsp" %>
<%
	}
	else
	{
		//Modo simulado
		
		if(codeType.equals("uniqueCode"))
		{
%>
			<%@ include file="call-htmlcontainer.jsp" %>
<%
		}
		else if(codeType.equals("dependentCode"))
		{
			if( PortalLocalServiceUtil.getIterProductList(request) == null)
			{
				if(unregisteredUserCode!="")
%>
					<%= replaceHTML(request, themeDisplay.getScopeGroupFriendlyURL(), unregisteredUserCode) %>
<%
			}
			else
			{
				if(registeredUserCode!="")
%>
					<%= replaceHTML(request, themeDisplay.getScopeGroupFriendlyURL(), registeredUserCode) %>
<%
			}
		}
	}
%>	