<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portlet.journal.util.JournalArticleTools"%>

<%@ include file="init.jsp" %>

<%!
	private static Log _log = LogFactoryUtil.getLog("disqus-portlet.docroot.html.disqus-comments.view.jsp");
%>

<%
	String contentId = renderRequest.getParameter(WebKeys.URL_PARAM_CONTENT_ID);
	
	if(Validator.isNotNull(contentId))
	{
		boolean acceptComments = JournalArticleTools.acceptComments(contentId);
		
		// 0009992: Posibilidad para admitir o no comentarios en los art�culos a discreci�n del usuario.
		if (acceptComments)
		{
%>
			<%@ include file="call-disqus.jsp" %>
<%
		}
	}
	else
	{
%>
		<%@ include file="call-disqus.jsp" %>
<%	
	}
%>