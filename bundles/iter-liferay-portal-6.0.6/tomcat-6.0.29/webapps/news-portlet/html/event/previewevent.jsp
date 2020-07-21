<%@page import="com.liferay.portal.kernel.util.PropsValues"%>
<%@page import="com.protecmedia.iter.base.service.ThemeWebResourcesServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.kernel.util.IterGlobal"%>
<%@page import="com.liferay.portal.kernel.util.ContentTypes"%>
<%@page import="com.protecmedia.iter.base.service.util.WebResourceUtil"%>
<%@page import="com.protecmedia.iter.base.service.ThemeWebResourcesLocalServiceUtil"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>

<%!
	private static Log _log = LogFactoryUtil.getLog("news-portlet.docroot.errors.previewevent.jsp");
%>

<%
	// ID del artículo
	String articleId = GetterUtil.getString((String) request.getAttribute("articleId"), "");

	// ID de a sección principal del artículo
	long plid = ((Long) request.getAttribute("plid")).longValue();
	
	// ID del post
	String postId = GetterUtil.getString((String) request.getAttribute("postId"), "");
%>
<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	
	<%=ThemeWebResourcesLocalServiceUtil.getWebResourceByPlidAndPlace(plid, WebResourceUtil.HEADER, ContentTypes.TEXT_CSS)%>
	
	<script type='text/javascript' src='/base-portlet/webrsrc/<%out.print(WebResourceUtil.getMD5WebResource(WebResourceUtil.HEADER));%>.js' ></script>
	
	<%=ThemeWebResourcesLocalServiceUtil.getWebResourceByPlidAndPlace(plid, WebResourceUtil.HEADER, ContentTypes.TEXT_JAVASCRIPT)%>
	
	<script type='text/javascript' src='/news-portlet/js/event/liveEvents.js?v=<%=IterGlobal.getLastIterWebCmsVersion()%>' ></script>
	
	<title>Live event <%=articleId%> preview </title>
</head>
<body>

<% if (Validator.isNull(postId)) { %>
	<div class="event-preview">
	</div>
	<script>
		jQryIter.registerOnLoadFunction(function() {
			ITER.EVENT.initLiveEvent("<%=articleId%>", jQryIter(".event-preview").first(), <%=PropsValues.ITER_EVENTS_POSTS_REFRESH_DELAY%>);
		});
	</script>
<% } else { %>
	<div id="event-post-preview-container" style="overflow: auto;">
		<%= GetterUtil.getString((String) request.getAttribute("html"), "") %>
	</div>
	<script>
		jQryIter.registerOnLoadFunction(function() {
			ITER.EVENT.ACTION.notifyReady({
				id: "<%=postId%>",
				height: jQryIter("#event-post-preview-container").outerHeight(true)
			});
		});
	</script>
<% } %>

	<script type='text/javascript' src='/base-portlet/webrsrc/<%out.print(WebResourceUtil.getMD5WebResource(WebResourceUtil.FOOTER));%>.js' ></script>
	<%=ThemeWebResourcesServiceUtil.getWebResourceByPlidAndPlace(plid, WebResourceUtil.FOOTER, ContentTypes.TEXT_JAVASCRIPT)%>
</body>
</html>