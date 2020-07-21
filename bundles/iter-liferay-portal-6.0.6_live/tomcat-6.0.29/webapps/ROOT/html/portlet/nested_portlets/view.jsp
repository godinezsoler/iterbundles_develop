<%
/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
%>

<%@ include file="/html/portlet/nested_portlets/init.jsp" %>
<%
try {
	String velocityTemplateId = (String)request.getAttribute(WebKeys.NESTED_PORTLET_VELOCITY_TEMPLATE_ID);
	String velocityTemplateContent = (String)request.getAttribute(WebKeys.NESTED_PORTLET_VELOCITY_TEMPLATE_CONTENT);

	RuntimePortletUtil.processTemplate(application, request, response, pageContext, out, velocityTemplateId, velocityTemplateContent);
}
catch (Exception e) {
	_log.error("Cannot render Nested Portlets portlet", e);
}
finally {
	RenderRequestImpl renderRequestImpl = (RenderRequestImpl)renderRequest;

	renderRequestImpl.defineObjects(portletConfig, renderResponse);
}
%>

<%!
private static Log _log = LogFactoryUtil.getLog("portal-web.docroot.html.portlet.nested_portlets.view.jsp");
%>