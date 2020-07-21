<%@page import="com.protecmedia.iter.base.service.FeedbackServiceUtil"%>
<%@page import="com.protecmedia.iter.tracking.util.TrackingUtil"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.liferay.portlet.journal.model.JournalArticle"%>
<%@page import="com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="com.liferay.portal.kernel.util.PropsValues"%>
<%@page import="com.liferay.portal.util.PortletKeys"%>

<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="com.liferay.portal.service.PortalLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="javax.portlet.RenderRequest"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>

<%@page import="org.apache.commons.lang.StringUtils"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%!
private static Log _log = LogFactoryUtil.getLog("tracking-portlet.docroot.html.user-feedback-portlet.UserFeedbackView.jsp");
%>

<%
	if (!themeDisplay.isWidget() && !themeDisplay.isWidgetFragment())
	{
		long scopeGrpId =  themeDisplay.getScopeGroupId();
		String contentId = renderRequest.getParameter(WebKeys.URL_PARAM_CONTENT_ID);
		String productsList = PortalLocalServiceUtil.getProductsByArticleId(contentId);
		
		
		//se obtiene la configuración para la visualización del portlet
		List<Map<String,Object>> feedbackData = UserFeedbackTools.getQuestionAnswers(request, scopeGrpId);
		boolean allowAnonymousVote = false;
		if (feedbackData.size() > 0)
			allowAnonymousVote = Boolean.parseBoolean(feedbackData.get(0).get("anonymousrating").toString());
		
		if(PHPUtil.isApacheRequest(request))
		{
%>
			<%@ include file="feedback.jspf" %>
			<%@ include file="allowvote_apache.jspf"%>
<%
		}
		else
		{
%>
			<%@ include file="feedback.jspf" %>
			<%@ include file="allowvote.jspf"%>
<%
		}
	}
%>