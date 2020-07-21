
<%@ include file="init.jsp" %>

<%
if(PHPUtil.isApacheRequest(request))
{
	PublicIterParams.set(WebKeys.ITER_RESPONSE_NEEDS_PHP, true);


	String typelogin = lu.getValueOfLoginPreference("typelogin");
	request.setAttribute(LoginUtil.GOTO_REFERER, "true");
%>
	
	<?php
		$usrname = getenv("ITER_USER");
	?>
	
<%	
}
%>

<%@ include file="loginForm.jsp" %>
