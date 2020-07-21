<%@page import="com.liferay.util.survey.IterSurveyUtil"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.liferay.portal.kernel.util.request.IterRequest"%>
<%@page import="com.liferay.portal.util.PortletKeys"%>
<%@page import="com.liferay.portal.util.HtmlOptimizer"%>
<%@page import="com.liferay.portal.kernel.util.GroupConfigTools"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.util.PortletViewMgr"%>
<%@page import="com.protecmedia.iter.user.service.tools.UserTools"%>
<%@page import="com.liferay.portal.model.impl.GroupImpl"%>
<%@page import="com.liferay.portal.kernel.log.RequestLogger"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.service.PortletPreferencesLocalServiceUtil"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="java.util.Arrays"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.liferay.portal.util.PropsUtil"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.liferay.portlet.AdvertisementUtil"%>
<%@page import="com.liferay.portal.kernel.util.SectionUtil"%>
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
// Lista de portlets que usan angular
String [] NG_PORTLETS = {
	"breadcrumbportlet_WAR_newsportlet",
	"daylytopicsportlet_WAR_newsportlet",
	"paywallportlet_WAR_newsportlet",
	"proxyportlet_WAR_newsportlet",
	"teaserviewerportlet_WAR_newsportlet",
	"dateportlet_WAR_newsportlet",
	"advancedsearchportlet_WAR_searchportlet",
	"contentviewerportlet_WAR_newsportlet",     
	"htmlcontainerportlet_WAR_newsportlet",     
	"velocitycontainerportlet_WAR_newsportlet",
	"relatedviewerportlet_WAR_newsportlet",
	"rankingviewerportlet_WAR_trackingportlet",
	"adcontainerportlet_WAR_advertisementportlet",
	"teaserviewernrportlet_WAR_newsportlet", 
	"118",
	"menuportlet_WAR_newsportlet",
	"catalogportlet_WAR_newsportlet",
	"unregisteruserportlet_WAR_userportlet",
	"edituserprofileportlet_WAR_userportlet",
	"formportlet_WAR_userportlet",
	"paywallstatusportlet_WAR_userportlet",
	"disquscomments_WAR_Disqusportlet",
	"contentratingsportlet_WAR_trackingportlet",
	"searchresultsportlet_WAR_searchportlet",
	"articletopicsportlet_WAR_newsportlet",
	"metadatalocatorportlet_WAR_newsportlet",
	"searchfiltersportlet_WAR_searchportlet",
	"facetedresultsportlet_WAR_searchportlet",
	"alertportlet_WAR_newsportlet",
	"hottopicsportlet_WAR_newsportlet",
	"articlerecommendationsportlet_WAR_newsportlet",
	PortletKeys.PORTLET_USERFEEDBACK
};

//Lista de portlets que usan configuracion flex
String [] FLEX_PORTLETS = 
						 {
						  "bannerportlet_WAR_advertisementportlet",
						  "contentmapportlet_WAR_serviceportlet",
						  "contentsocialportlet_WAR_trackingportlet",
						  "facebookportlet_WAR_serviceportlet",
						  "RSSPortlet_WAR_RSSportlet",
						  "serviceviewerportlet_WAR_serviceportlet",
						  "Sudoku_WAR_serviceportlet",
						  "registerportlet_WAR_userportlet",
						  "userprofileportlet_WAR_userportlet"
					  	 };

try
{
	Portlet portlet = (Portlet)request.getAttribute(WebKeys.RENDER_PORTLET);
	
	String portletId = portlet.getPortletId();
	String rootPortletId = portlet.getRootPortletId();
	String instanceId = portlet.getInstanceId();
	
	long portletRenderStarted = System.currentTimeMillis();
	
	String portletPrimaryKey = PortletPermissionUtil.getPrimaryKey(plid, portletId);
	
	String queryString = (String)request.getAttribute(WebKeys.RENDER_PORTLET_QUERY_STRING);
	String columnId = (String)request.getAttribute(WebKeys.RENDER_PORTLET_COLUMN_ID);
	Integer columnPos = (Integer)request.getAttribute(WebKeys.RENDER_PORTLET_COLUMN_POS);
	Integer columnCount = (Integer)request.getAttribute(WebKeys.RENDER_PORTLET_COLUMN_COUNT);
	Boolean renderPortletResource = (Boolean)request.getAttribute(WebKeys.RENDER_PORTLET_RESOURCE);
	
	boolean runtimePortlet = (renderPortletResource != null) && renderPortletResource.booleanValue();
	
	boolean access = false;
	
	if (PortalUtil.isAllowAddPortletDefaultResource(request, portlet)) {
		PortalUtil.addPortletDefaultResource(request, portlet);
	
		access = PortletPermissionUtil.contains(permissionChecker, plid, portlet, ActionKeys.VIEW);
	}
	
	if (portlet.isUndeployedPortlet()) {
		access = true;
	}
	
	boolean stateMax = layoutTypePortlet.hasStateMaxPortletId(portletId);
	boolean stateMin = layoutTypePortlet.hasStateMinPortletId(portletId);
	
	boolean modeAbout = layoutTypePortlet.hasModeAboutPortletId(portletId);
	boolean modeConfig = layoutTypePortlet.hasModeConfigPortletId(portletId);
	boolean modeEdit = layoutTypePortlet.hasModeEditPortletId(portletId);
	boolean modeEditDefaults = layoutTypePortlet.hasModeEditDefaultsPortletId(portletId);
	boolean modeEditGuest = layoutTypePortlet.hasModeEditGuestPortletId(portletId);
	boolean modeHelp = layoutTypePortlet.hasModeHelpPortletId(portletId);
	boolean modePreview = layoutTypePortlet.hasModePreviewPortletId(portletId);
	boolean modePrint = layoutTypePortlet.hasModePrintPortletId(portletId);
	
	InvokerPortlet invokerPortlet = null;
	
	try {
		invokerPortlet = PortletInstanceFactoryUtil.create(portlet, application);
	}
	/*catch (UnavailableException ue) {
		ue.printStackTrace();
	}*/
	catch (PortletException pe) {
		pe.printStackTrace();
	}
	catch (RuntimeException re) {
		re.printStackTrace();
	}
	
	PortletPreferences portletSetup = PortletPreferencesFactoryUtil.getLayoutPortletSetup(layout, portletId);
	
	PortletPreferencesIds portletPreferencesIds = PortletPreferencesFactoryUtil.getPortletPreferencesIds(request, portletId);
	
	//bifurcación para portlet login
	PortletPreferences portletPreferences = PortletPreferencesLocalServiceUtil.getPreferences(portletPreferencesIds);
	
	
	PortletConfig portletConfig = PortletConfigFactoryUtil.create(portlet, application);
	PortletContext portletCtx = portletConfig.getPortletContext();
	
	WindowState windowState = WindowState.NORMAL;
	
	if (themeDisplay.isStateExclusive()) {
		windowState = LiferayWindowState.EXCLUSIVE;
	}
	else if (themeDisplay.isStatePopUp()) {
		windowState = LiferayWindowState.POP_UP;
	}
	else if (stateMax) {
		windowState = WindowState.MAXIMIZED;
	}
	else if (stateMin) {
		windowState = WindowState.MINIMIZED;
	}
	
	PortletMode portletMode = PortletMode.VIEW;
	
	if (modeAbout) {
		portletMode = LiferayPortletMode.ABOUT;
	}
	else if (modeConfig) {
		portletMode = LiferayPortletMode.CONFIG;
	}
	else if (modeEdit) {
		portletMode = PortletMode.EDIT;
	}
	else if (modeEditDefaults) {
		portletMode = LiferayPortletMode.EDIT_DEFAULTS;
	}
	else if (modeEditGuest) {
		portletMode = LiferayPortletMode.EDIT_GUEST;
	}
	else if (modeHelp) {
		portletMode = PortletMode.HELP;
	}
	else if (modePreview) {
		portletMode = LiferayPortletMode.PREVIEW;
	}
	else if (modePrint) {
		portletMode = LiferayPortletMode.PRINT;
	}
	
	HttpServletRequest originalRequest = PortalUtil.getOriginalServletRequest(request);
	
	RenderRequestImpl renderRequestImpl = RenderRequestFactory.create(originalRequest, portlet, invokerPortlet, portletCtx, windowState, portletMode, portletPreferences, plid);
	
	if (Validator.isNotNull(queryString)) {
		DynamicServletRequest dynamicRequest = (DynamicServletRequest)renderRequestImpl.getHttpServletRequest();
	
		String[] params = StringUtil.split(queryString, StringPool.AMPERSAND);
	
		for (int i = 0; i < params.length; i++) {
			String[] kvp = StringUtil.split(params[i], StringPool.EQUAL);
	
			if (kvp.length > 1) {
				dynamicRequest.setParameter(kvp[0], kvp[1]);
			}
			else {
				dynamicRequest.setParameter(kvp[0], StringPool.BLANK);
			}
		}
	}
	
	StringServletResponse stringResponse = new StringServletResponse(response);
	
	RenderResponseImpl renderResponseImpl = RenderResponseFactory.create(renderRequestImpl, stringResponse, portletId, company.getCompanyId(), plid);
	
	if (stateMin) {
		renderResponseImpl.setUseDefaultTemplate(true);
	}
	
	renderRequestImpl.defineObjects(portletConfig, renderResponseImpl);
	
	String responseContentType = renderRequestImpl.getResponseContentType();
	
	String currentURL = PortalUtil.getCurrentURL(request);
	
	Portlet portletResourcePortlet = null;
	
	if (portletId.equals(PortletKeys.PORTLET_CONFIGURATION)) {
		String portletResource = ParamUtil.getString(request, "portletResource");
	
		if (Validator.isNull(portletResource)) {
			portletResource = ParamUtil.getString(renderRequestImpl, "portletResource");
		}
	
		if (Validator.isNotNull(portletResource)) {
			portletResourcePortlet = PortletLocalServiceUtil.getPortletById(company.getCompanyId(), portletResource);
		}
	}
	
	boolean showCloseIcon = true;
	boolean showConfigurationIcon = false;
	boolean showEditIcon = false;
	boolean showEditDefaultsIcon = false;
	boolean showEditGuestIcon = false;
	boolean showExportImportIcon = false;
	boolean showHelpIcon = portlet.hasPortletMode(responseContentType, PortletMode.HELP);
	boolean showMaxIcon = portlet.hasWindowState(responseContentType, WindowState.MAXIMIZED);
	boolean showMinIcon = portlet.hasWindowState(responseContentType, WindowState.MINIMIZED);
	boolean showMoveIcon = !stateMax && !themeDisplay.isStateExclusive();
	boolean showPortletCssIcon = false;
	boolean showPortletIcon = (portletResourcePortlet != null) ? Validator.isNotNull(portletResourcePortlet.getIcon()) : Validator.isNotNull(portlet.getIcon());
	boolean showPrintIcon = portlet.hasPortletMode(responseContentType, LiferayPortletMode.PRINT);
	boolean showRefreshIcon = portlet.isAjaxable() && (portlet.getRenderWeight() == 0);
	
	Boolean portletParallelRender = (Boolean)request.getAttribute(WebKeys.PORTLET_PARALLEL_RENDER);
	
	if ((portletParallelRender != null) && (portletParallelRender.booleanValue() == false)) {
		showRefreshIcon = false;
	}
	
	Group group = layout.getGroup();
	
	//Only authenticated with the correct permissions can configure portlets
	if ( themeDisplay.isSignedIn() && !portletId.equals(PortletKeys.PORTLET_CONFIGURATION)) 
	{
		boolean hideCfgBtt = GetterUtil.getBoolean(portletPreferences.getValue("hideConfigurationButton", null));
		originalRequest.setAttribute("hideConfigurationButton", String.valueOf(hideCfgBtt));
		
		if ((!group.hasStagingGroup() || group.isStagingGroup()) &&
			(PortletPermissionUtil.contains(permissionChecker, plid, portlet, ActionKeys.CONFIGURATION))) {
	
			showConfigurationIcon = true;
	
			boolean supportsLAR = Validator.isNotNull(portlet.getPortletDataHandlerClass());
			boolean supportsSetup = Validator.isNotNull(portlet.getConfigurationActionClass());
	
			if (supportsLAR || (supportsSetup && !group.isControlPanel())) {
				showExportImportIcon = true;
			}
	
			if (PropsValues.PORTLET_CSS_ENABLED) {
				showPortletCssIcon = true;
			}
		}
	}
	
	if (group.isLayoutPrototype()) {
		showExportImportIcon = false;
	}
	
	if (portlet.hasPortletMode(responseContentType, PortletMode.EDIT)) {
		if (PortletPermissionUtil.contains(permissionChecker, plid, portletId, ActionKeys.PREFERENCES)) {
			showEditIcon = true;
		}
	}
	
	if (portlet.hasPortletMode(responseContentType, LiferayPortletMode.EDIT_DEFAULTS)) {
		if (showEditIcon && !layout.isPrivateLayout() && themeDisplay.isShowAddContentIcon()) {
			showEditDefaultsIcon = true;
		}
	}
	
	if (portlet.hasPortletMode(responseContentType, LiferayPortletMode.EDIT_GUEST)) {
		if (showEditIcon && !layout.isPrivateLayout() && themeDisplay.isShowAddContentIcon()) {
			showEditGuestIcon = true;
		}
	}
	
	boolean supportsMimeType = portlet.hasPortletMode(responseContentType, portletMode);
	
	if (responseContentType.equals(ContentTypes.XHTML_MP) && portlet.hasMultipleMimeTypes()) {
		supportsMimeType = GetterUtil.getBoolean(portletSetup.getValue("portlet-setup-supported-clients-mobile-devices-" + portletMode, String.valueOf(supportsMimeType)));
	}
	
	// Only authenticated with the correct permissions can update a layout. If
	// staging is activated, only staging layouts can be updated.
	
	if ((!themeDisplay.isSignedIn()) ||
		(group.hasStagingGroup() && !group.isStagingGroup()) ||
		(!LayoutPermissionUtil.contains(permissionChecker, layout, ActionKeys.UPDATE))) {
	
		showCloseIcon = false;
		showMaxIcon = PropsValues.LAYOUT_GUEST_SHOW_MAX_ICON;
		showMinIcon = PropsValues.LAYOUT_GUEST_SHOW_MIN_ICON;
		showMoveIcon = false;
	}
	
	// Portlets cannot be moved unless they belong to the layout
	
	if (!layoutTypePortlet.hasPortletId(portletId)) {
		showCloseIcon = false;
		showMoveIcon = false;
	}
	
	// Portlets in the Control Panel cannot be moved
	
	if (layout.isTypeControlPanel()) {
		showCloseIcon = false;
		showMoveIcon = false;
	}
	
	// Static portlets cannot be moved
	
	if (portlet.isStatic()) {
		showCloseIcon = false;
		showMoveIcon = false;
	}
	
	// Deny access to edit mode if you do not have permission
	
	if (!PropsValues.TCK_URL && portletMode.equals(PortletMode.EDIT) && !PortletPermissionUtil.contains(permissionChecker, plid, portletId, ActionKeys.PREFERENCES)) {
		access = false;
	}
	
	// Deny access
	
	if (!access) {
		showCloseIcon = false;
		showConfigurationIcon = false;
		showEditIcon = false;
		showEditDefaultsIcon = false;
		showEditGuestIcon = false;
		showExportImportIcon = false;
		showHelpIcon = false;
		showMaxIcon = false;
		showMinIcon = false;
		showMoveIcon = false;
		showPortletCssIcon = false;
		showPrintIcon = false;
	}
	
	long previousScopeGroupId = themeDisplay.getScopeGroupId();
	
	if (portletId.equals(PortletKeys.PORTLET_CONFIGURATION) && portletResourcePortlet != null) {
		themeDisplay.setScopeGroupId(PortalUtil.getScopeGroupId(request, portletResourcePortlet.getPortletId()));
	}
	else {
		themeDisplay.setScopeGroupId(PortalUtil.getScopeGroupId(request, portletId));
	}
	
	portletDisplay.recycle();
	
	portletDisplay.setId(portletId);
	portletDisplay.setRootPortletId(rootPortletId);
	portletDisplay.setInstanceId(instanceId);
	portletDisplay.setResourcePK(portletPrimaryKey);
	portletDisplay.setPortletName(portletConfig.getPortletName());
	portletDisplay.setNamespace(PortalUtil.getPortletNamespace(portletId));
	
	portletDisplay.setAccess(access);
	portletDisplay.setActive(portlet.isActive());
	
	portletDisplay.setColumnId(columnId);
	portletDisplay.setColumnPos(columnPos.intValue());
	portletDisplay.setColumnCount(columnCount.intValue());
	
	portletDisplay.setStateExclusive(themeDisplay.isStateExclusive());
	portletDisplay.setStateMax(stateMax);
	portletDisplay.setStateMin(stateMin);
	portletDisplay.setStateNormal(windowState.equals(WindowState.NORMAL));
	portletDisplay.setStatePopUp(themeDisplay.isStatePopUp());
	
	portletDisplay.setModeAbout(modeAbout);
	portletDisplay.setModeConfig(modeConfig);
	portletDisplay.setModeEdit(modeEdit);
	portletDisplay.setModeEditDefaults(modeEditDefaults);
	portletDisplay.setModeEditGuest(modeEditGuest);
	portletDisplay.setModeHelp(modeHelp);
	portletDisplay.setModePreview(modePreview);
	portletDisplay.setModePrint(modePrint);
	
	portletDisplay.setShowCloseIcon(showCloseIcon);
	portletDisplay.setShowConfigurationIcon(showConfigurationIcon);
	portletDisplay.setShowEditIcon(showEditIcon);
	portletDisplay.setShowEditDefaultsIcon(showEditDefaultsIcon);
	portletDisplay.setShowEditGuestIcon(showEditGuestIcon);
	portletDisplay.setShowExportImportIcon(showExportImportIcon);
	portletDisplay.setShowHelpIcon(showHelpIcon);
	portletDisplay.setShowMaxIcon(showMaxIcon);
	portletDisplay.setShowMinIcon(showMinIcon);
	portletDisplay.setShowMoveIcon(showMoveIcon);
	portletDisplay.setShowPortletCssIcon(showPortletCssIcon);
	portletDisplay.setShowPortletIcon(showPortletIcon);
	portletDisplay.setShowPrintIcon(showPrintIcon);
	portletDisplay.setShowRefreshIcon(showRefreshIcon);
	
	portletDisplay.setWebDAVEnabled(portlet.getWebDAVStorageInstance() != null);
	portletDisplay.setRestoreCurrentView(portlet.isRestoreCurrentView());
	
	portletDisplay.setPortletSetup(portletSetup);
	
	// Es un portlet de newsletter y tiene configuración por grupo
	boolean allowNewsletterConf = rootPortletId.equals(PortletKeys.PORTLET_NEWSLETTER) && Validator.isNull(GroupConfigTools.getGroupConfigField(themeDisplay.getScopeGroupId(), "newsletterconfig"));
	
	// Portlet custom CSS class name
	String customCSSClassName= "";
	
	if ( portlet.getPortletName().equals( "login-portlet" ) || portlet.getPortletName().equals( "login-form-portlet" ) )
	{
		//Para el portlet login,las preferencias se obtienen de la tabla base_communities
		 customCSSClassName = UserTools.getLoginCustomCSSClassName(previousScopeGroupId,request);	
	}
	else
	{
		customCSSClassName = PortletConfigurationUtil.getPortletCustomCSSClassName(portletPreferences);
	}
	 
	
	
	portletDisplay.setCustomCSSClassName(customCSSClassName);
	
	// Si contiene la clase 'itr-wlod', activará la carga bajo demanda del widget
	if (Arrays.asList(customCSSClassName.split(StringPool.SPACE)).contains("itr-wlod"))
		IterRequest.setAttribute(WebKeys.WIDGET_LAZYLOAD_ENABLED, Boolean.TRUE);
	
	// Portlet icon
	
	String portletIcon = null;
	
	if (portletResourcePortlet != null) {
		portletIcon = portletResourcePortlet.getContextPath() + portletResourcePortlet.getIcon();
	}
	else {
		portletIcon = portlet.getContextPath() + portlet.getIcon();
	}
	
	portletDisplay.setURLPortlet(themeDisplay.getCDNHost() + portletIcon);
	
	// URL close
	
	String urlClose = themeDisplay.getPathMain() + "/portal/update_layout?p_l_id=" + plid + "&p_p_id=" + portletDisplay.getId() + "&doAsUserId=" + HttpUtil.encodeURL(themeDisplay.getDoAsUserId()) + "&" + Constants.CMD + "=" + Constants.DELETE + "&referer=" + HttpUtil.encodeURL(themeDisplay.getPathMain() + "/portal/layout?p_l_id=" + plid + "&doAsUserId=" + themeDisplay.getDoAsUserId()) + "&refresh=1";
	
	portletDisplay.setURLClose(urlClose.toString());
	
	// URL configuration
	
	/*****************************************************************************************************************/
	/*
	 *  ITERWEB	Luis Miguel
	 *  
	 *  Comprobación de si el portlet de configuración es de tipo FLEX. Para ello debe estar en la lista del portal-ext.properties
	 *	Si es de tipo FLEX se obtiene la url del swf y las variables que necesitará. 
	 */
	
	//boolean isFlexPortlet = Arrays.asList( PropsUtil.getArray(IterKeys.PORTAL_PROPERTIES_KEY_ITER_FLEX_PORTLETS_REGISTRY) ).contains(rootPortletId);
	
	String cloneRootId 		= portlet.getCloneRootPortletId();
	// Portlet con configuración o es un portlet de newsletter y tiene configuración por grupo
	boolean isFlexPortlet 	= Arrays.asList(FLEX_PORTLETS).contains(cloneRootId) || allowNewsletterConf;
	// Es un portlet angular
	boolean isNgPortlet		= Arrays.asList(NG_PORTLETS).contains(cloneRootId);
	
	String urlParams = "";
	String urlSWF = "";
	
	if(isFlexPortlet || isNgPortlet)
	{
		if (themeDisplay.isSignedIn()){
			// Calcula la ruta al SWF
			String folder = cloneRootId.equalsIgnoreCase("118") ? "base-portlet" : portletCtx.getPortletContextName();
			// Guarda la ruta
			urlSWF = IterLocalServiceUtil.getSWF("/"+folder+"/swf/"+ cloneRootId);
			// Si tiene configuración Angular, la concatena
			if (isNgPortlet)
			{
				urlSWF += "|" + (cloneRootId.equalsIgnoreCase("118") ? "nestedportlet" : cloneRootId.substring(0, cloneRootId.indexOf("_WAR_")));
			}
			
			String scopeGroup_FriendlyURL 	=  group.getFriendlyURL();
			String scopeGroup_Name 			=  StringEscapeUtils.escapeXml(group.getName());
		
			StringBuilder sbParams = new StringBuilder();
		
			sbParams.append("&portletResource="				).append(portletId);
			sbParams.append("&scopeGroupId="				).append(themeDisplay.getScopeGroupId());
			sbParams.append("&scopeGroupName="				).append(scopeGroup_Name);
			sbParams.append("&companyId="					).append(themeDisplay.getCompanyId());
			sbParams.append("&languageId="					).append(themeDisplay.getLanguageId());
			sbParams.append("&plid="						).append(themeDisplay.getOriginalPlid());
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
			sbParams.append("&phpMode="                     ).append(true);
			sbParams.append("&mainSection="                 ).append(request.getAttribute(WebKeys.MAIN_SECTION));
			sbParams.append("&iterSurveys="                 ).append(IterSurveyUtil.isEnabled(themeDisplay.getScopeGroupId()));
			
			//Se calcula si es seccion de detalle o no
			HttpServletRequest extendedRequest = PortalUtil.getHttpServletRequest(renderRequestImpl);
			String contentId = GetterUtil.getString(SectionUtil.getPublicRenderParameter(extendedRequest, WebKeys.URL_PARAM_CONTENT_ID), "");
			String detailSection = "true";
			if (contentId.equals("") || contentId.equals("0"))
				detailSection = "false";
			
			sbParams.append("&detailSection="               ).append(detailSection);
			
			urlParams = sbParams.toString();
		}
	
		portletDisplay.setURLConfiguration("#");
	}
	else
	{
	/*****************************************************************************************************************/
	
		PortletURLImpl urlConfiguration = new PortletURLImpl(request, PortletKeys.PORTLET_CONFIGURATION, plid, PortletRequest.RENDER_PHASE);
		
		urlConfiguration.setWindowState(LiferayWindowState.POP_UP);
		
		urlConfiguration.setEscapeXml(false);
		
		if (Validator.isNotNull(portlet.getConfigurationActionClass()))
		{
			urlConfiguration.setParameter("struts_action", "/portlet_configuration/edit_configuration");
		}
		else {
			urlConfiguration.setParameter("struts_action", "/portlet_configuration/edit_sharing");
		}
		
		urlConfiguration.setParameter("redirect", currentURL);
		urlConfiguration.setParameter("returnToFullPageURL", currentURL);
		urlConfiguration.setParameter("portletResource", portletDisplay.getId());
		urlConfiguration.setParameter("resourcePrimKey", PortletPermissionUtil.getPrimaryKey(plid, portlet.getPortletId()));
		
		portletDisplay.setURLConfiguration(urlConfiguration.toString() + "&" + PortalUtil.getPortletNamespace(PortletKeys.PORTLET_CONFIGURATION));
	}
	
	
	// URL edit
	
	PortletURLImpl urlEdit = new PortletURLImpl(request, portletDisplay.getId(), plid, PortletRequest.RENDER_PHASE);
	
	if (portletDisplay.isModeEdit()) {
		urlEdit.setWindowState(WindowState.NORMAL);
		urlEdit.setPortletMode(PortletMode.VIEW);
	}
	else {
		if (portlet.isMaximizeEdit() || portletDisplay.isStateMax()) {
			urlEdit.setWindowState(WindowState.MAXIMIZED);
		}
		else {
			urlEdit.setWindowState(WindowState.NORMAL);
		}
	
		urlEdit.setPortletMode(PortletMode.EDIT);
	}
	
	urlEdit.setEscapeXml(false);
	
	portletDisplay.setURLEdit(urlEdit.toString());
	
	// URL edit defaults
	
	PortletURLImpl urlEditDefaults = new PortletURLImpl(request, portletDisplay.getId(), plid, PortletRequest.RENDER_PHASE);
	
	if (portletDisplay.isModeEditDefaults()) {
		urlEditDefaults.setWindowState(WindowState.NORMAL);
		urlEditDefaults.setPortletMode(PortletMode.VIEW);
	}
	else {
		if (portlet.isMaximizeEdit()) {
			urlEditDefaults.setWindowState(WindowState.MAXIMIZED);
		}
		else {
			urlEditDefaults.setWindowState(WindowState.NORMAL);
		}
	
		urlEditDefaults.setPortletMode(LiferayPortletMode.EDIT_DEFAULTS);
	}
	
	urlEditDefaults.setEscapeXml(false);
	
	portletDisplay.setURLEditDefaults(urlEditDefaults.toString());
	
	// URL edit guest
	
	PortletURLImpl urlEditGuest = new PortletURLImpl(request, portletDisplay.getId(), plid, PortletRequest.RENDER_PHASE);
	
	if (portletDisplay.isModeEditGuest()) {
		urlEditGuest.setWindowState(WindowState.NORMAL);
		urlEditGuest.setPortletMode(PortletMode.VIEW);
	}
	else {
		if (portlet.isMaximizeEdit()) {
			urlEditGuest.setWindowState(WindowState.MAXIMIZED);
		}
		else {
			urlEditGuest.setWindowState(WindowState.NORMAL);
		}
	
		urlEditGuest.setPortletMode(LiferayPortletMode.EDIT_GUEST);
	}
	
	urlEditGuest.setEscapeXml(false);
	
	portletDisplay.setURLEditGuest(urlEditGuest.toString());
	
	// URL export / import
	
	PortletURLImpl urlExportImport = new PortletURLImpl(request, PortletKeys.PORTLET_CONFIGURATION, plid, PortletRequest.RENDER_PHASE);
	
	urlExportImport.setWindowState(WindowState.MAXIMIZED);
	
	urlExportImport.setParameter("struts_action", "/portlet_configuration/export_import");
	urlExportImport.setParameter("redirect", currentURL);
	urlExportImport.setParameter("returnToFullPageURL", currentURL);
	urlExportImport.setParameter("portletResource", portletDisplay.getId());
	
	urlExportImport.setEscapeXml(false);
	
	portletDisplay.setURLExportImport(urlExportImport.toString() + "&" + PortalUtil.getPortletNamespace(PortletKeys.PORTLET_CONFIGURATION));
	
	// URL help
	
	PortletURLImpl urlHelp = new PortletURLImpl(request, portletDisplay.getId(), plid, PortletRequest.RENDER_PHASE);
	
	if (portletDisplay.isModeHelp()) {
		urlHelp.setWindowState(WindowState.NORMAL);
		urlHelp.setPortletMode(PortletMode.VIEW);
	}
	else {
		if (portlet.isMaximizeHelp()) {
			urlHelp.setWindowState(WindowState.MAXIMIZED);
		}
		else {
			urlHelp.setWindowState(WindowState.NORMAL);
		}
	
		urlHelp.setPortletMode(PortletMode.HELP);
	}
	
	urlHelp.setEscapeXml(false);
	
	portletDisplay.setURLHelp(urlHelp.toString());
	
	// URL max
	
	String lifecycle = PortletRequest.RENDER_PHASE;
	
	if (!portletDisplay.isRestoreCurrentView()) {
		lifecycle = PortletRequest.ACTION_PHASE;
	}
	
	PortletURLImpl urlMax = new PortletURLImpl(request, portletDisplay.getId(), plid, lifecycle);
	
	if (portletDisplay.isStateMax()) {
		urlMax.setWindowState(WindowState.NORMAL);
	}
	else {
		urlMax.setWindowState(WindowState.MAXIMIZED);
	}
	
	urlMax.setEscapeXml(false);
	
	if (lifecycle.equals(PortletRequest.RENDER_PHASE)) {
		String portletNamespace = portletDisplay.getNamespace();
	
		Set<String> publicRenderParameterNames = SetUtil.fromEnumeration(portletConfig.getPublicRenderParameterNames());
	
		Map renderParameters = RenderParametersPool.get(request, plid, portletDisplay.getId());
	
		Iterator itr = renderParameters.entrySet().iterator();
	
		while (itr.hasNext()) {
			Map.Entry entry = (Map.Entry)itr.next();
	
			String key = (String)entry.getKey();
	
			if (key.startsWith(portletNamespace) || publicRenderParameterNames.contains(key)) {
				if (key.startsWith(portletNamespace)) {
					key = key.substring(portletNamespace.length(), key.length());
				}
	
				String[] values = (String[])entry.getValue();
	
				urlMax.setParameter(key, values);
			}
		}
	}
	
	portletDisplay.setURLMax(urlMax.toString());
	
	// URL min
	
	String urlMin = themeDisplay.getPathMain() + "/portal/update_layout?p_l_id=" + plid + "&p_p_id=" + portletDisplay.getId() + "&p_p_restore=" + portletDisplay.isStateMin() + "&doAsUserId=" + HttpUtil.encodeURL(themeDisplay.getDoAsUserId()) + "&" + Constants.CMD + "=minimize&referer=" + HttpUtil.encodeURL(themeDisplay.getPathMain() + "/portal/layout?p_l_id=" + plid + "&doAsUserId=" + themeDisplay.getDoAsUserId()) + "&refresh=1";
	
	portletDisplay.setURLMin(urlMin);
	
	// URL portlet css
	
	String urlPortletCss = "javascript:;";
	
	portletDisplay.setURLPortletCss(urlPortletCss.toString());
	
	// URL print
	
	PortletURLImpl urlPrint = new PortletURLImpl(request, portletDisplay.getId(), plid, PortletRequest.RENDER_PHASE);
	
	if (portletDisplay.isModePrint()) {
		urlPrint.setWindowState(WindowState.NORMAL);
		urlPrint.setPortletMode(PortletMode.VIEW);
	}
	else {
		if (portlet.isPopUpPrint()) {
			urlPrint.setWindowState(LiferayWindowState.POP_UP);
		}
		else {
			urlPrint.setWindowState(WindowState.NORMAL);
		}
	
		urlPrint.setPortletMode(LiferayPortletMode.PRINT);
	}
	
	urlPrint.setEscapeXml(false);
	
	portletDisplay.setURLPrint(urlPrint.toString());
	
	// URL refresh
	
	String urlRefresh = "javascript:;";
	
	portletDisplay.setURLRefresh(urlRefresh);
	
	// URL back
	
	String urlBack = null;
	
	if (portletDisplay.isModeEdit()) {
		urlBack = urlEdit.toString();
	}
	else if (portletDisplay.isModeEditDefaults()) {
		urlBack = urlEditDefaults.toString();
	}
	else if (portletDisplay.isModeEditGuest()) {
		urlBack = urlEditGuest.toString();
	}
	else if (portletDisplay.isModeHelp()) {
		urlBack = urlHelp.toString();
	}
	else if (portletDisplay.isModePrint()) {
		urlBack = urlPrint.toString();
	}
	else if (portletDisplay.isStateMax()) {
		//if (portletDisplay.getId().equals(PortletKeys.PORTLET_CONFIGURATION)) {
			/*String portletResource = ParamUtil.getString(request, "portletResource");
	
			urlMax.setAnchor(false);
	
			urlBack = urlMax.toString() + "#p_" + portletResource;*/
	
			//urlBack = ParamUtil.getString(renderRequestImpl, "returnToFullPageURL");
		//}
		//else {
		//	urlBack = urlMax.toString();
		//}
	
		if (portletDisplay.getId().startsWith("WSRP_")) {
			urlBack = portletDisplay.getURLBack();
		}
		else {
			urlBack = ParamUtil.getString(renderRequestImpl, "returnToFullPageURL");
			urlBack = HtmlUtil.stripHtml(urlBack);
			urlBack = PortalUtil.escapeRedirect(urlBack);
		}
	
		if (Validator.isNull(urlBack)) {
			urlBack = urlMax.toString();
		}
	}
	
	if (urlBack != null) {
		portletDisplay.setShowBackIcon(true);
		portletDisplay.setURLBack(urlBack);
	}
	
	if (themeDisplay.isWidget() || themeDisplay.isWidgetFragment()) {
		portletDisplay.setShowBackIcon(false);
	}
	
	if (group.isControlPanel()) {
		portletDisplay.setShowBackIcon(false);
		portletDisplay.setShowConfigurationIcon(false);
		portletDisplay.setShowMaxIcon(false);
		portletDisplay.setShowMinIcon(false);
		portletDisplay.setShowMoveIcon(false);
		portletDisplay.setShowPortletCssIcon(false);
	
		if (!portlet.isPreferencesUniquePerLayout() && Validator.isNotNull(portlet.getConfigurationActionClass())) {
			portletDisplay.setShowConfigurationIcon(true);
		}
	}
	
	// Make sure the Tiles context is reset for the next portlet
	
	if ((invokerPortlet != null) && invokerPortlet.isStrutsPortlet()) {
		request.removeAttribute(ComponentConstants.COMPONENT_CONTEXT);
	}
	%>
	
	<%@ include file="/html/portal/render_portlet-ext.jsp" %>
	
	<%
	
	// Render portlet
	
	boolean portletException  = false;
	Boolean portletVisibility = null;
	
	if (portlet.isActive() && access && supportsMimeType && PortletViewMgr.isEnabled(renderRequestImpl, request, portletId)) 
	{
		try 
		{
			PortletViewMgr.addPreViewData(renderRequestImpl, request, portletId);
			invokerPortlet.render(renderRequestImpl, renderResponseImpl);
	
			portletVisibility = (Boolean)renderRequestImpl.getAttribute(WebKeys.PORTLET_CONFIGURATOR_VISIBILITY);
	
			if (portletVisibility != null) 
			{
				request.setAttribute(WebKeys.PORTLET_CONFIGURATOR_VISIBILITY, portletVisibility);
			}
	
			if (themeDisplay.isFacebook() || themeDisplay.isStateExclusive()) 
			{
				renderRequestImpl.setAttribute(WebKeys.STRING_SERVLET_RESPONSE, stringResponse);
			}
		}
		catch (UnavailableException ue) 
		{
			portletException = true;
			PortletInstanceFactoryUtil.destroy(portlet);
		}
		catch (Exception e) 
		{
			portletException = true;
			LogUtil.log(_log, e);
		}
	}
	
	if ((layout.isTypePanel() || layout.isTypeControlPanel()) && !portletDisplay.getId().equals(PortletKeys.CONTROL_PANEL_MENU)) {
		PortalUtil.setPageTitle(portletDisplay.getTitle(), request);
	}
	%>
	
	<c:if test="<%= !themeDisplay.isFacebook() && !themeDisplay.isStateExclusive() && !themeDisplay.isWapTheme()%>">
	
		<%
		if (themeDisplay.isStatePopUp()) {
			PortalUtil.setPageTitle(portletDisplay.getTitle(), request);
		}
	
		String freeformStyles = StringPool.BLANK;
		String cssClasses = StringPool.BLANK;
	
		if (themeDisplay.isFreeformLayout() && !runtimePortlet && !layoutTypePortlet.hasStateMax()) {
			StringBundler sb = new StringBundler(7);
	
			Properties freeformStyleProps = PropertiesUtil.load(portletSetup.getValue("portlet-freeform-styles", StringPool.BLANK));
	
			sb.append("style=\"left: ");
			sb.append(GetterUtil.getString(freeformStyleProps.getProperty("left"), "0"));
			sb.append("; position: absolute; top: ");
			sb.append(GetterUtil.getString(freeformStyleProps.getProperty("top"), "0"));
			sb.append("; width: ");
			sb.append(GetterUtil.getString(freeformStyleProps.getProperty("width"), "400px"));
			sb.append(";\"");
	
			freeformStyles = sb.toString();
		}
	
		if (portletVisibility != null) 
		{
			cssClasses += " lfr-configurator-visibility";
		}
	
		if (portletDisplay.isStateMin()) 
		{
			cssClasses += " portlet-minimized";
		}
	
		if (!portletDisplay.isShowMoveIcon()) 
		{
			if (portlet.isStaticStart()) 
			{
				cssClasses += " portlet-static portlet-static-start";
			}
			else if (portlet.isStaticEnd()) 
			{
				cssClasses += " portlet-static portlet-static-end";
			}
		}
	
		if (!HtmlOptimizer.isEnabled())
		{
			cssClasses += " portlet-boundary" + HtmlUtil.escapeAttribute(PortalUtil.getPortletNamespace(rootPortletId));
		}
// 		else
// 		{
// 			// Si es optimizable se añade la clase que indica que el portlet es optimizable.
// 			// Es útil porque una vez optimizado se elimina dicha clase y se evita que se intenten optimizar más de una vez.
// 			cssClasses += " ".concat(HtmlOptimizer.OPTIMIZABLE_CLASS);
// 		}
		
		String data_portletid = (HtmlOptimizer.isLogEnabled()) ? String.format(" data-portletid=\"%s\" ", portletId) : "";
	
		cssClasses = "portlet-boundary " + StringPool.SPACE + cssClasses + StringPool.SPACE + portlet.getCssClassWrapper() + StringPool.SPACE + customCSSClassName;
		%>
	
		<c:if test="<%= !themeDisplay.isWidget()%>">
			<div id="<%= (!HtmlOptimizer.isEnabled() || PortletKeys.NESTED_PORTLETS.equals(rootPortletId)) ? "p_p_id"+HtmlUtil.escapeAttribute(renderResponseImpl.getNamespace()) : HtmlOptimizer.getPortletId(portletId) %>" class="<%= cssClasses %>" <%= freeformStyles %> <%=data_portletid%>>
				<a id="p_<%= HtmlUtil.escapeAttribute(portletId) %>" class="<%= HtmlUtil.getRemovableClass()%>"></a>
		</c:if>
	</c:if>
	
	<c:choose>
		<c:when test="<%= !supportsMimeType %>">
		</c:when>
		<c:when test="<%= !access && !portlet.isShowPortletAccessDenied() %>">
		</c:when>
		<c:when test="<%= !portlet.isActive() && !portlet.isShowPortletInactive() %>">
		</c:when>
		<c:otherwise>
	
			<%
			boolean useDefaultTemplate = portlet.isUseDefaultTemplate();
			Boolean useDefaultTemplateObj = renderResponseImpl.getUseDefaultTemplate();
	
			if (useDefaultTemplateObj != null) {
				useDefaultTemplate = useDefaultTemplateObj.booleanValue();
			}
	
			if ((invokerPortlet != null) && invokerPortlet.isStrutsPortlet()) {
				if (!access || portletException) {
					PortletRequestProcessor portletReqProcessor = (PortletRequestProcessor)portletCtx.getAttribute(WebKeys.PORTLET_STRUTS_PROCESSOR);
	
					ActionMapping actionMapping = portletReqProcessor.processMapping(request, response, (String)portlet.getInitParams().get("view-action"));
	
					ComponentDefinition definition = null;
	
					if (actionMapping != null) {
	
						// See action path /weather/view
	
						String definitionName = actionMapping.getForward();
	
						if (definitionName == null) {
	
							// See action path /journal/view_articles
	
							String[] definitionNames = actionMapping.findForwards();
	
							for (int definitionNamesPos = 0; definitionNamesPos < definitionNames.length; definitionNamesPos++) {
								if (definitionNames[definitionNamesPos].endsWith("view")) {
									definitionName = definitionNames[definitionNamesPos];
	
									break;
								}
							}
	
							if (definitionName == null) {
								definitionName = definitionNames[0];
							}
						}
	
						definition = TilesUtil.getDefinition(definitionName, request, application);
					}
	
					String templatePath = StrutsUtil.TEXT_HTML_DIR + "/common/themes/portlet.jsp";
	
					if (definition != null) {
						templatePath = StrutsUtil.TEXT_HTML_DIR + definition.getPath();
					}
			%>
	
					<tiles:insert template="<%= templatePath %>" flush="false">
						<tiles:put name="portlet_content" value="/portal/portlet_error.jsp" />
					</tiles:insert>
	
			<%
				}
				else {
					if (useDefaultTemplate) {
						renderRequestImpl.setAttribute(WebKeys.PORTLET_CONTENT, stringResponse.getString());
			%>
	
						<tiles:insert template='<%= StrutsUtil.TEXT_HTML_DIR + "/common/themes/portlet.jsp" %>' flush="false">
							<tiles:put name="portlet_content" value="<%= StringPool.BLANK %>" />
						</tiles:insert>
	
			<%
					}
					else {
						pageContext.getOut().print(stringResponse.getString());
					}
				}
			}
			else {
				renderRequestImpl.setAttribute(WebKeys.PORTLET_CONTENT, stringResponse.getString());
	
				String portletContent = StringPool.BLANK;
	
				if (portletException) {
					portletContent = "/portal/portlet_error.jsp";
				}
			%>
	
				<c:choose>
					<c:when test="<%= useDefaultTemplate || portletException %>">
						<tiles:insert template='<%= StrutsUtil.TEXT_HTML_DIR + "/common/themes/portlet.jsp" %>' flush="false">
							<tiles:put name="portlet_content" value="<%= portletContent %>" />
						</tiles:insert>
					</c:when>
					<c:otherwise>
						<%= renderRequestImpl.getAttribute(WebKeys.PORTLET_CONTENT) %>
					</c:otherwise>
				</c:choose>
	
			<%
			}
			%>
	
		</c:otherwise>
	</c:choose>
	
	<%
	String staticVar = "yes";
	
	if (portletDisplay.isShowMoveIcon()) {
		staticVar = "no";
	}
	else {
		if (portlet.isStaticStart()) {
			staticVar = "start";
		}
	
		if (portlet.isStaticEnd()) {
			staticVar = "end";
		}
	}
	%>
	
	<c:if test="<%= !themeDisplay.isFacebook() && !themeDisplay.isStateExclusive() && !themeDisplay.isWapTheme()%>">
	
			<%
			String modules = StringPool.BLANK;
	
			if (showConfigurationIcon) {
				modules += "aui-editable";
			}
			%>
	
	<%--
		/*
		 *  ITERWEB	Luis Miguel
		 *  
		 *  En el onLoad se pasan dos nuevas variables con la ruta del swf y sus variables. 
		 */ 
	 --%>
	
			<c:if test="<%= themeDisplay.isSignedIn() %>">
				<aui:script position='<%= themeDisplay.isIsolated() ? "inline" : "auto" %>' use="<%= modules %>">
					Liferay.Portlet.onLoad(
						{
							canEditTitle: false,
							columnPos: <%= columnPos %>,
							isStatic: '<%= staticVar %>',
							namespacedId: 'p_p_id<%= HtmlUtil.escapeJS(renderResponseImpl.getNamespace()) %>',
							portletId: '<%= HtmlUtil.escapeJS(portletDisplay.getId()) %>',
							refreshURL: '<%= HtmlUtil.escapeJS(PortletURLUtil.getRefreshURL(request, themeDisplay)) %>',
							flashVars: '<%= urlParams %>',
							urlSWF: '<%= urlSWF %>'
						}
					);
				</aui:script>
			</c:if>
		<c:if test="<%= !themeDisplay.isWidget()%>">	
			</div>
		</c:if>	
	</c:if>
	
	<%
	themeDisplay.setScopeGroupId(previousScopeGroupId);
	
	if (showPortletCssIcon) {
		themeDisplay.setIncludePortletCssJs(true);
	}
	
	SessionMessages.clear(renderRequestImpl);
	SessionErrors.clear(renderRequestImpl);
	
	if (themeDisplay.isFacebook() || themeDisplay.isStateExclusive()) {
		request.setAttribute(JavaConstants.JAVAX_PORTLET_REQUEST, renderRequestImpl);
		request.setAttribute(JavaConstants.JAVAX_PORTLET_RESPONSE, renderResponseImpl);
	}
	else {
		renderRequestImpl.cleanUp();
	}
	
	if (RequestLogger.logPortlet.isInfoEnabled() && themeDisplay.getRequestLogger().isPageRenderStarted())
		themeDisplay.getRequestLogger().logPortlet(portletId, System.currentTimeMillis() - portletRenderStarted);
}
finally
{
	IterRequest.removeAttribute(WebKeys.WIDGET_LAZYLOAD_ENABLED);
}
%>

<%!
private static Log _log = LogFactoryUtil.getLog("portal-web.docroot.html.portal.render_portlet.jsp");
%>
