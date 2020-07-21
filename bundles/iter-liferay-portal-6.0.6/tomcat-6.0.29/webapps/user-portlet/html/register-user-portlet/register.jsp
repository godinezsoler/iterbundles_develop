<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.liferay.portal.kernel.xml.Document"%>
<%@page import="com.protecmedia.iter.user.util.FormUtil"%>

<%@ include file="initRegister.jsp" %>

<%
	String htmlForm = "";

	if( Validator.isNotNull(grpId) && grpId!=0	 )
	{
		String endpoint = "/user-portlet/confirm-email";
		Document d = FormUtil.getFormXml(grpId, request, endpoint, null, null);
		FormUtil.updateCSSAttrWithEditProfileIntent(IterKeys.EDIT_PROFILE_INTENT_REGISTRY, d);
		htmlForm = FormUtil.applyXSL( d );
	}
%>

<%= htmlForm %>

