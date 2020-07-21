<%
/**
 * Copyright (c) Protecmedia All rights reserved.
 */
%>

<%@ include file="init.jsp" %>

<%

String login 		= lu.getValueOfLoginPreference( "login" );
String usrprefix	= lu.getValueOfLoginPreference( "usrprefix" );
String usrsuffix 	= lu.getValueOfLoginPreference( "usrsuffix" );
String logout		= lu.getValueOfLoginPreference( "logout" );

if( PHPUtil.isApacheRequest(request) )
{
%>
	<%@ include file="login.jsp" %>
<%
}
else
{
%>
	<%@ include file="simulatedlogin.jsp" %>
<%
}
%>