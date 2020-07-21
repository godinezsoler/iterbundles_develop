<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@ include file="initUnregister.jsp" %>

<%
	String onClickFunction = "";
	String unregisterButtonEscaped = StringEscapeUtils.escapeHtml(unregisterButton);
	if(PHPUtil.isApacheRequest(request))
	{
		PublicIterParams.set(WebKeys.ITER_RESPONSE_NEEDS_PHP, true);
%>

<?php
		$usrname = getenv("ITER_USER");
		if(strlen($usrname)!==0)
		{ 
?>
			<script type="text/javascript">
				var panelTitle 		 		= '<%=StringEscapeUtils.escapeJavaScript(panelTitle)%>';
				var unregisterConfirmButton = '<%=StringEscapeUtils.escapeJavaScript(unregisterConfirmButton)%>';
				var enterPasswordText		= '<%=StringEscapeUtils.escapeJavaScript(enterPasswordText)%>';
				var confirmPasswordText		= '<%=StringEscapeUtils.escapeJavaScript(confirmPasswordText)%>';
				var unregisterSuccessMsg 	= '<%=StringEscapeUtils.escapeJavaScript(unregisterSuccessMsg)%>';
				var unregisterErrorMsg 		= '<%=StringEscapeUtils.escapeJavaScript(unregisterErrorMsg)%>';
				var badCredentialsMsg 		= '<%=StringEscapeUtils.escapeJavaScript(badCredentialsMsg)%>';
				var okDialogButton			= '<%=StringEscapeUtils.escapeJavaScript(okDialogButton)%>';
			</script>
<%
			onClickFunction = "javascript:showUnregisterDialog(panelTitle, unregisterConfirmButton, enterPasswordText, confirmPasswordText, unregisterSuccessMsg, unregisterErrorMsg, badCredentialsMsg, okDialogButton);";	
%>

<?php
  		} 
?>

<%
	}
%>

<input type="button" class="btnLogin" value="<%=unregisterButtonEscaped %>" onclick="<%=onClickFunction %>" />