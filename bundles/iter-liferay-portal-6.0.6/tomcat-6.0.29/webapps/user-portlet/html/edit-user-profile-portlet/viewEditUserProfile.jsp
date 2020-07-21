<%@page import="com.protecmedia.iter.user.util.UserUtil"%>
<%@page import="com.liferay.portal.kernel.xml.Document"%>
<%@page import="com.protecmedia.iter.user.util.FormUtil"%>
<%@ include file="initEditUserProfile.jsp" %>

<%
	String htmlForm = UserUtil.getEditUserProfileHtml(scopeGroupId, request, preferences);
%>

<%= htmlForm %>

