<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@page import="java.net.URLDecoder"%>
<%@ include file="init.jsp" %>

<%
	String close = ParamUtil.getString(request, "close", "");
	String titleText = LanguageUtil.get(pageContext, "content-tools-send-mail-title");
	String shortURL = ParamUtil.getString(request, "shortURL");
	shortURL = shortURL.replace(" ", "+");
%>
	<script type="text/javascript">
	
		window.onload = setTitle();
		
		function setTitle()
		{
			document.title = "<%=titleText%>";
		}
		
	</script>
	
