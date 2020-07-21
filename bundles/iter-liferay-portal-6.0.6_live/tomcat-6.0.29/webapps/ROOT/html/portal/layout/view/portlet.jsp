<%@page import="com.liferay.portal.kernel.util.request.IterRequest"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.model.Layout"%>
<%@page import="com.liferay.portal.service.LayoutLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="java.util.regex.Matcher"%>
<%@page import="java.util.regex.Pattern"%>
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

<%@ include file="/html/portal/init.jsp" %>

<%!
private static Log _log = LogFactoryUtil.getLog("portal-web.docroot.html.portal.layout.view.portlet.jsp");
%>

<c:if test="<%= !IterRequest.isNewsletterPage() && !themeDisplay.isFacebook() && !themeDisplay.isStateExclusive() && !themeDisplay.isStatePopUp() && !themeDisplay.isWidget() && !themeDisplay.isWidgetFragment()%>">

	<%
		for (String portletId : PropsValues.LAYOUT_STATIC_PORTLETS_ALL) 
		{
			if (PortletLocalServiceUtil.hasPortlet(company.getCompanyId(), portletId)) 
			{
	%>

			<liferay-portlet:runtime portletName="<%= portletId %>" />

	<%
			}
		}

	%>

</c:if>

<%

if ( !themeDisplay.isWidgetFragment() && (themeDisplay.isFacebook() || themeDisplay.isStateExclusive() || themeDisplay.isStatePopUp() || themeDisplay.isWidget() || layoutTypePortlet.hasStateMax()) )  
{
	String ppid = ParamUtil.getString(request, "p_p_id");

	String velocityTemplateId = null;
	String velocityTemplateContent = null;

	if (themeDisplay.isFacebook() || themeDisplay.isStateExclusive()) {
		velocityTemplateId = theme.getThemeId() + LayoutTemplateConstants.STANDARD_SEPARATOR + "exclusive";
		velocityTemplateContent = LayoutTemplateLocalServiceUtil.getContent("exclusive", true, theme.getThemeId());
	}
	else if (themeDisplay.isStatePopUp() || themeDisplay.isWidget()) {
		velocityTemplateId = theme.getThemeId() + LayoutTemplateConstants.STANDARD_SEPARATOR + "pop_up";
		velocityTemplateContent = LayoutTemplateLocalServiceUtil.getContent("pop_up", true, theme.getThemeId());
	}
	else {
		ppid = StringUtil.split(layoutTypePortlet.getStateMax())[0];

		velocityTemplateId = theme.getThemeId() + LayoutTemplateConstants.STANDARD_SEPARATOR + "max";
		velocityTemplateContent = LayoutTemplateLocalServiceUtil.getContent("max", true, theme.getThemeId());
	}

	RuntimePortletUtil.processTemplate(application, request, response, pageContext, out, ppid, velocityTemplateId, velocityTemplateContent);
}
else 
{
	String themeId = theme.getThemeId();

	String layoutTemplateId = layoutTypePortlet.getLayoutTemplateId();

	if (Validator.isNull(layoutTemplateId)) 
	{
		layoutTemplateId = PropsValues.DEFAULT_LAYOUT_TEMPLATE_ID;
	}

	LayoutTemplate layoutTemplate = LayoutTemplateLocalServiceUtil.getLayoutTemplate(layoutTemplateId, false, theme.getThemeId());

	if (layoutTemplate != null) 
	{
		themeId = layoutTemplate.getThemeId();
	}

	String velocityTemplateId = themeId + LayoutTemplateConstants.CUSTOM_SEPARATOR + layoutTypePortlet.getLayoutTemplateId();
	String velocityTemplateContent = LayoutTemplateLocalServiceUtil.getContent(layoutTypePortlet.getLayoutTemplateId(), false, theme.getThemeId());

	_log.debug("themeDisplay.isWidgetFragment(): "+themeDisplay.isWidgetFragment());
	if (themeDisplay.isWidgetFragment())
	{
		String widgetColumnId = themeDisplay.getWidgetFragment();
		if (Validator.isNotNull(widgetColumnId))
		{
			Pattern processColumnPattern = Pattern.compile(
					"(processColumn[(]\")(.*?)(\"(?:, *\"(?:.*?)\")?[)])", Pattern.DOTALL);
		
			Matcher processColumnMatcher = processColumnPattern.matcher(velocityTemplateContent);
			
			while (processColumnMatcher.find()) 
			{
				String columnId = processColumnMatcher.group(2);
	
				if (Validator.isNotNull(columnId) && widgetColumnId.equals(columnId)) 
				{
					velocityTemplateContent = new StringBuffer("$processor.").
												append(processColumnMatcher.group(1)).
												append(columnId).
												append(processColumnMatcher.group(3)).toString();
					break;
				}
			}
			_log.debug("widgetColumnId: " +widgetColumnId+ "\nvelocityTemplateContent: " +velocityTemplateContent);
			
			velocityTemplateId = velocityTemplateId.concat("_"+widgetColumnId);
		}
	}

%>

	<c:if test="<%= PropsValues.TAGS_COMPILER_ENABLED && !themeDisplay.isWidgetFragment()%>">
		<liferay-portlet:runtime portletName="<%= PortletKeys.TAGS_COMPILER %>" />
	</c:if>

<%
	themeDisplay.swapToTmpLayout();
	RuntimePortletUtil.processTemplate(application, request, response, pageContext, out, velocityTemplateId, velocityTemplateContent);
	themeDisplay.restoreFromTmpLayout();
}
%>



<%@ include file="/html/portal/layout/view/common.jspf" %>