<%@page import="com.liferay.portal.kernel.util.request.IterRequest"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%@ include file="initPaywallStatus.jsp" %>

<%
	//Modo Apache
	String phpenabled = "";
	HttpServletRequest originalRequest = IterRequest.getOriginalRequest();
	if(PHPUtil.isApacheRequest(originalRequest))
	{
		PublicIterParams.set(originalRequest, WebKeys.ITER_RESPONSE_NEEDS_PHP, true);
		phpenabled = "true";
	}
	else
	{
		phpenabled = "false";
	}
	
	String result = PaywallUtil.getPaywallStatusHTML(phpenabled, viewMode, PayCookieUtil.getUserId(originalRequest), title, 
													 cookieName, dateName, userAgentName, ipName, productName, noSignedMsg, 
													 articleName, daysBefore, expiresDateName, originalRequest);
	
	CKEditorUtil.noInheritThemeCSS(result, request);
%>

<%=result%>