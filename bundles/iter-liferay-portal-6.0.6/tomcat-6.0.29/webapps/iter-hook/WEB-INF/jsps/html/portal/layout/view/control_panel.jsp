<%@page import="com.liferay.portal.model.Group"%>
<%@page import="java.util.List"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletMgr"%>
<%@page import="java.util.Arrays"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.liferay.portal.service.GroupLocalServiceUtil"%>

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

<%
String ppid = ParamUtil.getString(request, "p_p_id");

if (ppid.equals(PortletKeys.PORTLET_CONFIGURATION)) {
	String portletResource = ParamUtil.getString(request, PortalUtil.getPortletNamespace(ppid) + "portletResource");

	if (Validator.isNull(portletResource)) {
		portletResource = ParamUtil.getString(request, "portletResource");
	}

	if (Validator.isNotNull(portletResource)) {
		String strutsAction = ParamUtil.getString(request, PortalUtil.getPortletNamespace(ppid) + "struts_action");

		if (!strutsAction.startsWith("/portlet_configuration/")) {
			ppid = portletResource;
		}
	}
}

if (ppid.equals(PortletKeys.EXPANDO)) {
	String modelResource = ParamUtil.getString(request, PortalUtil.getPortletNamespace(ppid) + "modelResource");

	if (modelResource.equals(User.class.getName())) {
		ppid = PortletKeys.ENTERPRISE_ADMIN_USERS;
	}
	else if (modelResource.equals(Organization.class.getName())) {
		ppid = PortletKeys.ENTERPRISE_ADMIN_ORGANIZATIONS;
	}
}

if (ppid.equals(PortletKeys.PLUGIN_INSTALLER)) {
	ppid = PortletKeys.ADMIN_PLUGINS;
}

String category = PortalUtil.getControlPanelCategory(ppid, themeDisplay);

List<Layout> scopeLayouts = new ArrayList<Layout>();

Portlet portlet = null;

boolean denyAccess = false;

if (Validator.isNotNull(ppid)) {
	portlet = PortletLocalServiceUtil.getPortletById(ppid);

	if ((portlet == null) ||
		(!portlet.isSystem() && !PortalUtil.isControlPanelPortlet(ppid, category, themeDisplay)) && !PortalUtil.isAllowAddPortletDefaultResource(request, portlet)) {

		denyAccess = true;
	}
}

request.setAttribute("control_panel.jsp-ppid", ppid);
%>

<c:choose>
	<c:when test="<%= !themeDisplay.isStateExclusive() && !themeDisplay.isStatePopUp() %>">

		<%
		String panelBodyCssClass = "panel-page-body";
		String panelCategory = "lfr-ctrl-panel";
		String categoryTitle = Validator.isNotNull(category) ? LanguageUtil.get(pageContext, "category." + category) : StringPool.BLANK;

		if (!layoutTypePortlet.hasStateMax()) {
			panelBodyCssClass += " panel-page-frontpage";
		}
		else {
			panelBodyCssClass += " panel-page-application";
		}

		if (category.equals(PortletCategoryKeys.CONTENT)) {
			panelCategory += " panel-manage-content";
		}
		else if (category.equals(PortletCategoryKeys.MY)) {
			panelCategory += " panel-manage-my";
			categoryTitle = user.getFullName();
		}
		else if (category.equals(PortletCategoryKeys.PORTAL)) {
			panelCategory += " panel-manage-portal";

			if (CompanyLocalServiceUtil.getCompaniesCount(false) > 1) {
				categoryTitle += " " + company.getName();
			}
		}
		else if (category.equals(PortletCategoryKeys.SERVER)) {
			panelCategory += " panel-manage-server";
		}
		else {
			panelCategory += " panel-manage-frontpage";
		}

		Layout scopeLayout = null;
		Group curGroup = themeDisplay.getScopeGroup();

		if (curGroup.isLayout()) {
			scopeLayout = LayoutLocalServiceUtil.getLayout(curGroup.getClassPK());
			curGroup = scopeLayout.getGroup();
		}

		if (Validator.isNotNull(categoryTitle) && !category.equals(PortletCategoryKeys.CONTENT)) {
			PortalUtil.addPortletBreadcrumbEntry(request, categoryTitle, null);
		}
		
		/*
		 *  ITERWEB	Luis Miguel
		 *  
		 *  Comprobación de si el portlet de configuración es de tipo FLEX. Para ello debe estar en la lista CONTROL_PANEL_FLEX
		 *	Si es de tipo FLEX se obtiene la url del swf y las variables que necesitará. 
		 */
		
		String[] CONTROL_PANEL_FLEX =
		{
				
		};
		
		String swf = "";
		long scopeGroupIdFlex = themeDisplay.getScopeGroupId();
		
		boolean isFlexControlPanel = Arrays.asList(CONTROL_PANEL_FLEX).contains(ppid);
		if (isFlexControlPanel)
		{
			swf = IterLocalServiceUtil.getSWF("/"+portlet.getPluginPackage().getContext()+"/swf/"+ppid);
			
			//Global y Guest no son grupos útiles para portlets FLex
			List<Group> validGroups = PortletMgr.getGroups(String.valueOf(themeDisplay.getCompanyId()));
			boolean validGroup = false;
			for(Group currentGroup:validGroups)
			{
				if(currentGroup.getGroupId() == scopeGroupIdFlex)
				{
					validGroup = true;
					break;
				}
			}
			
			if(!validGroup)
				scopeGroupIdFlex = 0;
		}

		StringBuilder sbParams = new StringBuilder();
		
		sbParams.append("&scopeGroupId="				).append(scopeGroupIdFlex);
		sbParams.append("&scopeGroupName="				).append(GroupLocalServiceUtil.getGroup(themeDisplay.getScopeGroupId()).getName());
		sbParams.append("&companyId="					).append(themeDisplay.getCompanyId());
		sbParams.append("&languageId="					).append(themeDisplay.getLanguageId());
		sbParams.append("&environment="					).append(IterLocalServiceUtil.getEnvironment());
		sbParams.append("&plid="						).append(themeDisplay.getLayout().getPlid());
		sbParams.append("&secure="						).append(themeDisplay.isSecure());
		sbParams.append("&userId="						).append(themeDisplay.getPermissionChecker().getUserId());
		sbParams.append("&lifecycleRender="				).append(themeDisplay.isLifecycleRender());
		sbParams.append("&pathFriendlyURLPublic="		).append(themeDisplay.getPathFriendlyURLPublic());
		sbParams.append("&pathFriendlyURLPrivateUser="	).append(themeDisplay.getPathFriendlyURLPrivateUser());
		sbParams.append("&pathFriendlyURLPrivateGroup="	).append(themeDisplay.getPathFriendlyURLPrivateGroup());
		sbParams.append("&serverName="					).append(themeDisplay.getServerName());
		sbParams.append("&cdnHost="						).append(themeDisplay.getCDNHost());
		sbParams.append("&pathImage="					).append(themeDisplay.getPathImage());
		sbParams.append("&pathMain="					).append(themeDisplay.getPathMain());
		sbParams.append("&pathContext="					).append(themeDisplay.getPathContext());
		sbParams.append("&urlPortal="					).append(themeDisplay.getURLPortal());
		sbParams.append("&pathThemeImages="				).append(themeDisplay.getPathThemeImages());
		sbParams.append("&groupId="						).append(company.getGroup().getGroupId());
		sbParams.append("&layoutUuid="					).append(themeDisplay.getLayout().getUuid());

		String urlParams = sbParams.toString();

		%>

		<div id="content-wrapper">
			<aui:layout cssClass="<%= panelCategory %>">
				<aui:column columnWidth="<%= 25 %>" cssClass="panel-page-menu" first="<%= true %>">
					<liferay-portlet:runtime portletName="160" />
				</aui:column>
<%--
	/*
	 *  ITERWEB	Luis Miguel
	 *  
	 *	Se comprueba si el panel de control del portlet que se va a configurar esta en FLEX. En ese caso, 
	 *		en la columna derecha del panel de control se carga un swf, si no es asi, se incluye el panel_content.jspf 
	 *		como se hacia anteriormente a ésta modificación
	 */
 --%>
				<aui:column columnWidth="<%= 75 %>" cssClass="<%= panelBodyCssClass %>" last="<%= true %>">
					<c:choose>
						<c:when test="<%= isFlexControlPanel %>">
							<div id="swf-wrapper">
								<object id='mySwf'
									classid='clsid:D27CDB6E-AE6D-11cf-96B8-444553540000'>
									<param name='src' value='<%=swf%>' />
									<param name='flashVars' value='<%=urlParams%>' />
									<embed name='mySwf' src='<%=swf%>' pluginspage='http://www.adobe.com/go/getflashplayer'
										flashVars='<%=urlParams%>' height="600" width="100%" />
								</object>

							</div>
						</c:when>
						<c:otherwise>
							<%@ include file="/html/portal/layout/view/panel_content.jspf" %>
						</c:otherwise>
					</c:choose>
				</aui:column>
			</aui:layout>
		</div>
	</c:when>
	<c:otherwise>
		<%@ include file="/html/portal/layout/view/panel_content.jspf" %>
	</c:otherwise>
</c:choose>

<%@ include file="/html/portal/layout/view/common.jspf" %>