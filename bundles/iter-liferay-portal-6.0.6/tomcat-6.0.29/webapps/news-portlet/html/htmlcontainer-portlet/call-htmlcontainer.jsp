<%@page import="com.liferay.portal.model.Layout"%>
<%@page import="com.protecmedia.iter.base.service.util.ServiceError"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.protecmedia.iter.base.service.util.ErrorRaiser"%>
<%@page import="com.liferay.portal.kernel.util.SectionUtil"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.liferay.portlet.ContextVariables"%>
<%@page import="java.util.Map"%>
<%@page import="com.liferay.portlet.AdvertisementUtil"%>

<%
try
{
%>
	<div>
		<%
			if(codeType.equals("uniqueCode"))
			{
				if( !textHTML.equals("") )
				{
%>
					<%= replaceHTML(request, themeDisplay.getScopeGroupFriendlyURL(), textHTML) %>
<%
				}
			}
			else if(codeType.equals("dependentCode"))
			{
				PublicIterParams.set(WebKeys.ITER_RESPONSE_NEEDS_PHP, true);
					
				out.print( "<?php if (user_is_signedin()===true){ ?>");
				if(registeredUserCode!="")
				{
%>
					<%= replaceHTML(request, themeDisplay.getScopeGroupFriendlyURL(), registeredUserCode) %>
<%
				}
				
				out.print(" <?php" +" }else{ ?> ");
				if(unregisteredUserCode!="")
				{
%>
					<%= replaceHTML(request, themeDisplay.getScopeGroupFriendlyURL(), unregisteredUserCode) %>
<%
				}
				out.print(" <?php" +" } ?> "); 
			}
%>
	</div>
<%
}
catch(ServiceError se)
{
	_logHtmlContainer.debug(se);
}
catch(Exception e)
{
	_logHtmlContainer.error(e);
}
%>
