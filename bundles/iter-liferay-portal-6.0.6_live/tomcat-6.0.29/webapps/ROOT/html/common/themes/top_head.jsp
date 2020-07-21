<%@page import="com.liferay.portal.kernel.util.ContentTypes"%>
<%@page import="com.protecmedia.iter.base.service.util.Preloading"%>
<%@page import="com.liferay.portal.kernel.util.IterGlobal"%>
<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="com.liferay.portal.kernel.json.JSONFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.util.advertisement.SlotAssignment"%>
<%@page import="com.liferay.portal.kernel.util.advertisement.MetadataAdvertisementTools"%>
<%@page import="com.protecmedia.iter.base.service.ThemeWebResourcesLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%@page import="com.protecmedia.iter.base.service.ThemeWebResourcesServiceUtil"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.protecmedia.iter.base.service.util.ErrorRaiser"%>
<%@page import="com.protecmedia.iter.base.service.util.ServiceError"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.util.SectionUtil"%>
<%@page import="com.liferay.portal.kernel.json.JSONObject"%>
<%@page import="com.liferay.portal.kernel.comments.CommentsConfigBean"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portlet.TeaserUtil"%>
<%@page import="java.io.InputStreamReader"%>
<%@page import="java.io.BufferedReader"%>
<%@page import="java.io.InputStream"%>
<%@page import="java.io.ByteArrayInputStream"%>
<%@page import="com.liferay.portal.kernel.util.HtmlUtil"%>
<%@page import="com.liferay.portal.kernel.util.ArrayUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringBundler"%>
<%@page import="com.liferay.portlet.ContextVariables"%>
<%@page import="com.liferay.portal.service.PortalLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portlet.AdvertisementUtil"%>
<%@page import="com.liferay.portal.kernel.util.PropsKeys"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.base.service.util.WebResourceUtil" %>
<%@page import="com.liferay.portal.kernel.velocity.IterVelocityTools" %>

<%@ include file="/html/common/init.jsp" %>
<%@ include file="/html/common/themes/top_meta.jspf" %>
<%@ include file="/html/common/themes/top_meta-ext.jsp" %>


<%
	boolean themeDatabaseEnabled    = GetterUtil.getBoolean(PropsUtil.get(PropsKeys.ITER_THEME_DATABASE_ENABLED), true) &&
									  	IterKeys.DEFAULT_THEME.equalsIgnoreCase(themeDisplay.getThemeId());

	boolean usePortletsOwnResources = GetterUtil.getBoolean(PropsUtil.get(PropsKeys.ITER_PORTLETS_USE_OWN_RESOURCES), false);

	HttpServletRequest original_request = PortalUtil.getOriginalServletRequest(request);
	Object isNewsletterPageObject = original_request.getAttribute(WebKeys.REQUEST_ATTRIBUTE_IS_NEWSLETTER_PAGE);
	boolean isNewsletterPage = false;
	if(isNewsletterPageObject != null)
		isNewsletterPage = GetterUtil.getBoolean(isNewsletterPageObject.toString());

	String versionRsrc = (themeDisplay.isSignedIn() || !isNewsletterPage) 															? 
							String.format("/base-portlet/webrsrc/%s.js", WebResourceUtil.getMD5WebResource(WebResourceUtil.HEADER))	:
							""; 
%>

<%-- Preloads --%>
<%-- Preload de la versión. Se carga primero el JS de la versión y luego los JS del tema --%> 
<c:if test="<%=themeDisplay.isSignedIn() || !isNewsletterPage%>">
	<c:if test="<%=Preloading.iterRsrc(themeDisplay.getScopeGroupId())%>">
		<%=IterGlobal.getPreloadContent(versionRsrc, ContentTypes.TEXT_JAVASCRIPT) %>
	</c:if>
</c:if>

<%-- Preloads del tema --%>
<c:if test="<%= themeDatabaseEnabled && !layout.isTypeControlPanel() && !isNewsletterPage%>">
	<%=ThemeWebResourcesLocalServiceUtil.getWebResourceByPlidAndPlace(themeDisplay.getPlid(), WebResourceUtil.HEADER, 
			Preloading.themeRsrcContentTypes(themeDisplay.getScopeGroupId()), true)%>
</c:if>
<%-- END OF: Preloads --%>



<c:if test="<%= themeDatabaseEnabled && !layout.isTypeControlPanel()%>">
	<%=ThemeWebResourcesLocalServiceUtil.getWebResourceByPlidAndPlace(themeDisplay.getPlid(), WebResourceUtil.HEADER, ContentTypes.TEXT_HTML)%>
</c:if>

<%-- Portal CSS --%>

<c:if test="<%= themeDisplay.isSignedIn() || !themeDatabaseEnabled || layout.isTypeControlPanel()%>">
	<link href="<%= HtmlUtil.escape(PortalUtil.getStaticResourceURL(request, themeDisplay.getCDNHost() + themeDisplay.getPathContext() + "/html/portal/css.jsp")) %>" rel="stylesheet" type="text/css" />
</c:if>

<%-- Theme CSS --%>

<c:choose>
	<c:when test="<%= !themeDatabaseEnabled || layout.isTypeControlPanel()%>">
		<link class="lfr-css-file" href="<%= HtmlUtil.escape(PortalUtil.getStaticResourceURL(request, themeDisplay.getPathThemeCss() + "/main.css")) %>" rel="stylesheet" type="text/css" />
	</c:when>
	<c:otherwise>
		<c:if test="<%= !isNewsletterPage %>">
			<%=ThemeWebResourcesLocalServiceUtil.getWebResourceByPlidAndPlace(themeDisplay.getPlid(), WebResourceUtil.HEADER, ContentTypes.TEXT_CSS)%>		
		</c:if>
	</c:otherwise>
</c:choose>

<%-- User Inputted Layout CSS --%>

<c:if test="<%= (layout != null) && Validator.isNotNull(layout.getCssText()) %>">
	<style type="text/css">
		<%= layout.getCssText() %>
	</style>
</c:if>


<%
	List<Portlet> portlets = null;
	if (layout != null)
	{
		String ppid = ParamUtil.getString(request, "p_p_id");
	
		if (ppid.equals(PortletKeys.PORTLET_CONFIGURATION)) {
			portlets = new ArrayList<Portlet>();
	
			portlets.add(PortletLocalServiceUtil.getPortletById(company.getCompanyId(), PortletKeys.PORTLET_CONFIGURATION));
	
			ppid = ParamUtil.getString(request, PortalUtil.getPortletNamespace(ppid) + "portletResource");
	
			if (Validator.isNotNull(ppid)) {
				portlets.add(PortletLocalServiceUtil.getPortletById(company.getCompanyId(), ppid));
			}
		}
		else if (layout.isTypePortlet()) {
			portlets = layoutTypePortlet.getAllPortlets();
			
			/*
			 *  ITERWEB	Luis Miguel
			 *  
			 *  Se añaden al array de portlets, donde están los de la página, aquellos que están incrustados
			 *	 en el tema y además en la lista configurada del portal-ext.properties para cargar sus css/js
			 */
			String[] embededportlets = PropsUtil.getArray( String.format(PropsKeys.ITER_EMBEDEDPORTLET_GROUPFRIENDLYURL, themeDisplay.getScopeGroup().getFriendlyURL().replace("/", ".")) );
			for(String portletid : embededportlets  )
			{
				portlets.add( PortletLocalServiceUtil.getPortletById(company.getCompanyId(), portletid) );
			}

			if (themeDisplay.isStateMaximized() || themeDisplay.isStatePopUp()) {
				if (Validator.isNotNull(ppid)) {
					Portlet portlet = PortletLocalServiceUtil.getPortletById(company.getCompanyId(), ppid);
	
					if (!portlets.contains(portlet)) {
						portlets.add(portlet);
					}
				}
			}
		}
		else if ((layout.isTypeControlPanel() || layout.isTypePanel()) && Validator.isNotNull(ppid)) {
			portlets = new ArrayList<Portlet>();
	
			portlets.add(PortletLocalServiceUtil.getPortletById(company.getCompanyId(), ppid));
		}
	
		request.setAttribute(WebKeys.LAYOUT_PORTLETS, portlets);
	}
%>

<%-- Portlet CSS References --%>

<c:if test="<%= portlets != null && (usePortletsOwnResources || layout.isTypeControlPanel())%>">
<%
	Set<String> headerPortalCssSet = new LinkedHashSet<String>();
	
	List<String> portletsCssEnabled = Arrays.asList(GetterUtil.getString(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_ENABLE_CSS_PORTLETS), "").split(","));
	for (Portlet portlet : portlets) 
	{
		if(portletsCssEnabled.contains(portlet.getPortletName()) || layout.isTypeControlPanel()) 
		{
			for (String headerPortalCss : portlet.getHeaderPortalCss()) 
			{
				if (!HttpUtil.hasProtocol(headerPortalCss)) 
				{
					headerPortalCss = PortalUtil.getStaticResourceURL(request, request.getContextPath() + headerPortalCss, portlet.getTimestamp());
				}
	
				if (!headerPortalCssSet.contains(headerPortalCss)) 
				{
					headerPortalCssSet.add(headerPortalCss);
%>
					<link href="<%= HtmlUtil.escape(headerPortalCss) %>" rel="stylesheet" type="text/css" />
<%
				}
			}
		}
	}

	Set<String> headerPortletCssSet = new LinkedHashSet<String>();

	for (Portlet portlet : portlets) {
		if(portletsCssEnabled.contains(portlet.getPortletName()) || layout.isTypeControlPanel()) 
		{
			for (String headerPortletCss : portlet.getHeaderPortletCss()) {
				if (!HttpUtil.hasProtocol(headerPortletCss)) {
					headerPortletCss = PortalUtil.getStaticResourceURL(request, portlet.getContextPath() + headerPortletCss, portlet.getTimestamp());
				}
	
				if (!headerPortletCssSet.contains(headerPortletCss)) {
					headerPortletCssSet.add(headerPortletCss);
%>
					<link href="<%= HtmlUtil.escape(headerPortletCss) %>" rel="stylesheet" type="text/css" />
<%
				}
			}
		}
	}
%>
</c:if>

	<%-- Portal Header JavaScript References --%>

	<%@ include file="/html/common/themes/top_js.jspf" %>

<c:if test="<%= themeDisplay.isSignedIn() || !isNewsletterPage%>">

	<%-- Iter Header & Portlet Header JavaScript References --%>

	<script type='<%=ContentTypes.TEXT_JAVASCRIPT%>' src='<%=versionRsrc%>' ></script>

	<script>
		jQryIter.u = "<%=PHPUtil.isApacheRequest(IterRequest.getOriginalRequest()) ? "<?php echo getenv('ITER_USER_ID');?>" : ""%>";
	</script>

	<%@ include file="/html/common/themes/JQryIterExtension.jspf" %>
</c:if>

<c:if test="<%= IterVelocityTools.canShowDockbar()%>">
	<%-- Se cargan los scripts de la dockbar --%>
	<%@ include file="/html/portlet/dockbar/dockbar_ngportlets.jspf"%>
</c:if>

<c:if test="<%= themeDatabaseEnabled && !layout.isTypeControlPanel() && !isNewsletterPage%>">
	<%-- Theme Header JavaScript --%>
	<%=ThemeWebResourcesLocalServiceUtil.getWebResourceByPlidAndPlace(themeDisplay.getPlid(), WebResourceUtil.HEADER, ContentTypes.TEXT_JAVASCRIPT)%>	
</c:if>

<c:choose>
	<c:when test="<%= !usePortletsOwnResources%>">
		
		<!-- Iter Portlet Header Javascripts -->
	</c:when>
	<c:otherwise>
		<c:if test="<%= portlets != null && !isNewsletterPage %>">
<%
			Set<String> headerPortalJavaScriptSet = new LinkedHashSet<String>();
		
			for (Portlet portlet : portlets) {
				for (String headerPortalJavaScript : portlet.getHeaderPortalJavaScript()) {
					if (!HttpUtil.hasProtocol(headerPortalJavaScript))
					{
						headerPortalJavaScript = PortalUtil.getStaticResourceURL(request, request.getContextPath() + headerPortalJavaScript, portlet.getTimestamp());
					}
		
					if (!headerPortalJavaScriptSet.contains(headerPortalJavaScript) && !themeDisplay.isIncludedJs(headerPortalJavaScript))
					{
						headerPortalJavaScriptSet.add(headerPortalJavaScript);
%> 
						<script src="<%= HtmlUtil.escape(headerPortalJavaScript) %>" type="text/javascript"></script>
<%
					}
				}
			}
		
			Set<String> headerPortletJavaScriptSet = new LinkedHashSet<String>();
		
			for (Portlet portlet : portlets) {
				for (String headerPortletJavaScript : portlet.getHeaderPortletJavaScript()) {
					if (!HttpUtil.hasProtocol(headerPortletJavaScript))
					{
						headerPortletJavaScript = PortalUtil.getStaticResourceURL(request, portlet.getContextPath() + headerPortletJavaScript, portlet.getTimestamp());
					}
		
					if (!headerPortletJavaScriptSet.contains(headerPortletJavaScript))
					{
						headerPortletJavaScriptSet.add(headerPortletJavaScript);
%> 
						<script src="<%= HtmlUtil.escape(headerPortletJavaScript) %>" type="text/javascript"></script>
<%
					}
				}
			}
%> 
		</c:if>
	</c:otherwise>
</c:choose>

<%
	//Javascripts Teaser-Viewer Portlet
	
	if ( !isNewsletterPage )
	{
		//Javascripts Disqus
		Object commentsConfigBeanObject = original_request.getAttribute(WebKeys.REQUEST_ATTRIBUTE_COMMENTS_CONFIG_BEAN);
		if(commentsConfigBeanObject != null )
		{
			CommentsConfigBean commentsConfig = (CommentsConfigBean)commentsConfigBeanObject;
			out.print(commentsConfig.getJavascriptDisqusInitCode());
			out.print(commentsConfig.getPHPDisqusHMACSHA1Code());
		}
	}
%>

<%-- Raw Text --%>

<%
	List<String> markupHeaders = (List<String>)request.getAttribute(MimeResponse.MARKUP_HEAD_ELEMENT);
	if (markupHeaders != null) {
		for (String markupHeader : markupHeaders)
		{
%>
			<%= markupHeader %>
<%
		}
	}

	StringBundler pageTopSB = (StringBundler)request.getAttribute(WebKeys.PAGE_TOP);
%>

<c:if test="<%= pageTopSB != null %>">
<%
	pageTopSB.writeTo(out);
%>
</c:if>


<c:if test="<%= portlets != null && !layout.isTypeControlPanel() %>">

<%
	// Se obtiene la fecha que está en la URL
	String dateToken = "/date/";
	int dateIndex 	 = -1;
	if ( (dateIndex = themeDisplay.getURLCurrent().indexOf(dateToken)) > 0 )
	{
		String[] postDateParams = themeDisplay.getURLCurrent().substring(dateIndex+dateToken.length()).split("/");
		original_request.setAttribute("urlDate", postDateParams[0]);
	}
	
	// Se obtiene el metadato que está en la URL
	String metaToken = "/meta/";
	int metaIndex 	 = -1;
	if ( (dateIndex = themeDisplay.getURLCurrent().indexOf(metaToken)) > 0 )
	{
		String[] postMetaParams = themeDisplay.getURLCurrent().substring(metaIndex+metaToken.length()).split("/");
		original_request.setAttribute("urlMeta", postMetaParams[0]);
	}


	//Se añade el SKIN, 
	//Se añade el javascript necesario para pintar la publicidad 
	//Se calcula el interstitial para pintarlo posteriormente en el bottom.jsp 
	
	if(!isNewsletterPage)
	{
		_log.debug("SKIN");
		long sectionPlid = SectionUtil.getSectionPlid(request);
		
		boolean isEnabled = false;
		boolean allowCtxVars = ContextVariables.ctxVarsEnabled(themeDisplay.getScopeGroupFriendlyURL());
		String advertisementType = AdvertisementUtil.SEGMENTATION_BY_METADATA;
		List<String> advCategories = null; 
		
		SlotAssignment slotAssignment = null;
		String categoryId = StringPool.BLANK;
		String categoryName = StringPool.BLANK;
		String categoryNormalizedName = StringPool.BLANK;
		
		if( AdvertisementUtil.isSkinEnabledForGroup(themeDisplay.getScopeGroup()) )
		{
			_log.debug("SKIN is enabled for this group");
			advCategories = AdvertisementUtil.getAdvertisementCategories(request, themeDisplay.getScopeGroupId());
			
			if( advCategories.size()>0 )
			{
				for(String catId : advCategories)
				{
					if (_log.isDebugEnabled())
						_log.debug( String.format("SKIN for category %s", catId) );
							
					slotAssignment = MetadataAdvertisementTools.getSkin(themeDisplay.getScopeGroupId(),catId);
					if(slotAssignment!=null)
					{
						categoryId = catId;
						categoryName = MetadataAdvertisementTools.getCategoryName(themeDisplay.getScopeGroupId(), catId);
						categoryNormalizedName = MetadataAdvertisementTools.getCategoryNormalizedName(themeDisplay.getScopeGroupId(), catId);
						
						if (_log.isDebugEnabled())
							_log.debug( String.format("SKIN for category %s: categoryName=%s, categoryNormalizedName=%s", catId, categoryName, categoryNormalizedName) );
							
						break;
					}
				}
			}
			
			if(slotAssignment==null)
			{
				advertisementType = AdvertisementUtil.SEGMENTATION_BY_LAYOUT;
				slotAssignment = AdvertisementUtil.getSkin4Slot( request, themeDisplay.getScopeGroup(), sectionPlid );
			}
			
			if(Validator.isNotNull(slotAssignment))
			{
				try
				{
					String imgPath = slotAssignment.getSkinImagePath();
					isEnabled = slotAssignment.getEnabled();
					
					if (_log.isDebugEnabled())
						_log.debug( String.format("SKIN slotAssignment enable=%b, imagePath=%s", isEnabled, GetterUtil.getString2(imgPath, "")) );
					
					if( isEnabled && Validator.isNotNull(imgPath) )
					{
						String skinSuperid	= slotAssignment.getSuperId();
						String bckColor 	= slotAssignment.getSkinBckColor();
						String imgName		= slotAssignment.getSkinName();
						String fEntryId 	= slotAssignment.getSkinFileUuid();
						String dispMode 	= slotAssignment.getSkinDisplayMode();
						String clickUrl 	= slotAssignment.getSkinClickUrl();
						String clickScript	= slotAssignment.getSkinClickScript();
	
						if (_log.isDebugEnabled())
							_log.debug( String.format("SKIN slotAssignment bckColor=%s, dispMode=%s", GetterUtil.getString2(bckColor, ""), GetterUtil.getString2(dispMode, "")) );

						if( allowCtxVars )
						{
							if(Validator.isNotNull(categoryName))
								AdvertisementUtil.add2AttributeValueList(request, ContextVariables.METADATA_NAMES_ARRAY, categoryName);
							if(Validator.isNotNull(categoryNormalizedName))
								AdvertisementUtil.add2AttributeValueList(request, ContextVariables.METADATA_FRIENDLY_NAMES_ARRAY, categoryNormalizedName);
							
							String slotName = slotAssignment.getSlotName();
							
							Map<String, String> localCtxVars = new HashMap<String, String>();
							localCtxVars.put( ContextVariables.ADSLOT_NAME, slotName );
							
							if(Validator.isNotNull(categoryName))
								localCtxVars.put( ContextVariables.METADATA_NAME, categoryName);
							if(Validator.isNotNull(categoryNormalizedName))
								localCtxVars.put( ContextVariables.METADATA_FRIENDLY_NAME, categoryNormalizedName);
							
							int lastSlashIdx = imgName.lastIndexOf("/");
							imgName = imgName.substring( lastSlashIdx!=-1?lastSlashIdx+1:0 );
							int dotIdx = imgName.indexOf(".");
							if( dotIdx!=-1 )
								imgName = imgName.substring(0, dotIdx);
							
							localCtxVars.put( ContextVariables.SKIN_NAME, imgName );
							
							Map<String, String> globalCtxVars = new HashMap<String, String>();
							if( ContextVariables.findCtxVars(clickUrl) || ContextVariables.findCtxVars(clickScript) || 
									(Validator.isNull( fEntryId ) && ContextVariables.findCtxVars(imgPath)) )
							{
								if(advertisementType.equalsIgnoreCase(AdvertisementUtil.SEGMENTATION_BY_LAYOUT))
									globalCtxVars = AdvertisementUtil.getAdvertisementCtxVars(request);
								else
									globalCtxVars = AdvertisementUtil.getAdvertisementCategoryCtxVars(request, themeDisplay.getScopeGroupId(), categoryId);
								
								ErrorRaiser.throwIfNull(globalCtxVars);
							}
								
							//Se sustituyen las variables por el valor correspondiente y se guardan en el request para pintarlas despues.
							if( Validator.isNotNull(clickUrl) && ContextVariables.findCtxVars(clickUrl) )
							{
								clickUrl = ContextVariables.replaceCtxVars(clickUrl, globalCtxVars);
								clickUrl = ContextVariables.replaceCtxVars(clickUrl, localCtxVars);
							}
							
							if( Validator.isNotNull(clickScript) && ContextVariables.findCtxVars(clickScript) )
							{
								clickScript = ContextVariables.replaceCtxVars(clickScript, globalCtxVars);
								clickScript = ContextVariables.replaceCtxVars(clickScript, localCtxVars);
							}
							
							if( Validator.isNull( fEntryId ) && ContextVariables.findCtxVars(imgPath) )
							{
								imgPath = ContextVariables.replaceCtxVars(imgPath, globalCtxVars);
								imgPath = ContextVariables.replaceCtxVars(imgPath, localCtxVars);
							}
						}
						
						if( Validator.isNotNull(clickUrl) )
							original_request.setAttribute("clickUrl", clickUrl);
						
						if( Validator.isNotNull(clickScript) )
							original_request.setAttribute("clickScript", clickScript);
						
						if( Validator.isNotNull( skinSuperid ) )
							//Se añade el id del tag global al request
							AdvertisementUtil.add2AttributeValueList(request, AdvertisementUtil.PARENT_TAGS_IDS, skinSuperid);
				%>
							<style type="text/css">
								body
								{
				<%		if( Validator.isNotNull(bckColor) && !bckColor.isEmpty() ) %>
									background-color: #<%= bckColor %>;
									background-image: url('<%= imgPath %>');
									background-repeat: no-repeat;
				<%		if( Validator.isNotNull(dispMode) && !dispMode.isEmpty() ) %>
									background-attachment: <%= dispMode %>;
									background-position: center top;
								}
							</style>
					
				<%
					
					}
				}
				catch(ServiceError se)
				{
					_log.debug(se);
				}
				catch(Exception e)
				{
					_log.error(e);
				}
			}
		}
		
		if( AdvertisementUtil.isInterstitialEnabledForGroup(themeDisplay.getScopeGroup()) )
		{
			slotAssignment = null;
			categoryId = StringPool.BLANK;
			categoryName = StringPool.BLANK;
			categoryNormalizedName = StringPool.BLANK;
			advertisementType = AdvertisementUtil.SEGMENTATION_BY_METADATA;
			JSONObject intersInfo = JSONFactoryUtil.createJSONObject();
			
			if(advCategories == null)
				advCategories = AdvertisementUtil.getAdvertisementCategories(request, themeDisplay.getScopeGroupId());
			
			if( advCategories.size()>0 )
			{
				for(String catId : advCategories)
				{
					slotAssignment = MetadataAdvertisementTools.getInterstitial(themeDisplay.getScopeGroupId(), catId);
					if(slotAssignment!=null)
					{
						categoryId = catId;
						categoryName = MetadataAdvertisementTools.getCategoryName(themeDisplay.getScopeGroupId(), catId);
						categoryNormalizedName = MetadataAdvertisementTools.getCategoryNormalizedName(themeDisplay.getScopeGroupId(), catId);
						
						intersInfo.put("categoryId", categoryId);
						intersInfo.put("categoryName", categoryName);
						intersInfo.put("categoryNormalizedName", categoryNormalizedName);
						
						break;
					}
				}
			}
			
			if(slotAssignment==null)
			{
				advertisementType = AdvertisementUtil.SEGMENTATION_BY_LAYOUT;
				slotAssignment = AdvertisementUtil.getInterstitial4Slot( request, themeDisplay.getScopeGroup(), sectionPlid );
			}
			
			if(Validator.isNotNull(slotAssignment))
			{
				String tagscript = AdvertisementUtil.hideAdvWithFake() ? slotAssignment.getFakeTagScript() : slotAssignment.getTagScript();
				isEnabled = slotAssignment.getEnabled();
				
				if( isEnabled && Validator.isNotNull(tagscript) )
				{
					intersInfo.put("advertisementType", advertisementType);
					
					String interSuperId = slotAssignment.getSuperId();
					
					if( Validator.isNotNull( interSuperId ) )
						//Se añade el id del tag global al request
						AdvertisementUtil.add2AttributeValueList(request, AdvertisementUtil.PARENT_TAGS_IDS, interSuperId);
					
					if( allowCtxVars )
					{
						if(Validator.isNotNull(categoryName))
							AdvertisementUtil.add2AttributeValueList(request, ContextVariables.METADATA_NAMES_ARRAY, categoryName);
						if(Validator.isNotNull(categoryNormalizedName))
							AdvertisementUtil.add2AttributeValueList(request, ContextVariables.METADATA_FRIENDLY_NAMES_ARRAY, categoryNormalizedName);
						
						String tagname = slotAssignment.getTagName();
						if( Validator.isNotNull( tagname ) )
							AdvertisementUtil.add2AttributeValueList(request, ContextVariables.TAGS_NAMES_ARRAY, tagname);
					}
					
					//Se añade el interstitial al request para pintarlo despues
					original_request.setAttribute(AdvertisementUtil.ADV_INTER, slotAssignment);
					original_request.setAttribute(AdvertisementUtil.ADV_INTER_CAT, intersInfo);
				}
			}
		}
	
		//Pintar todos los script globales
		if( Validator.isNotNull(PublicIterParams.get(original_request, AdvertisementUtil.PARENT_TAGS_IDS) ) )
		{
			String value = "";
			Map<String, String> arrayCtxVars = new HashMap<String, String>();
			
			List<String> adslotNames = (List<String>)PublicIterParams.get(original_request, ContextVariables.ADSLOTS_NAMES_ARRAY);
			if( Validator.isNotNull(adslotNames) )
			{
				value = ContextVariables.getListAsString( adslotNames );
				arrayCtxVars.put( ContextVariables.ADSLOTS_NAMES_ARRAY, value );
			}
			
			List<String> tagNames = (List<String>)PublicIterParams.get(original_request, ContextVariables.TAGS_NAMES_ARRAY);
			if( Validator.isNotNull(tagNames) )
			{
				value = ContextVariables.getListAsString( tagNames );
	
				arrayCtxVars.put( ContextVariables.TAGS_NAMES_ARRAY, value );
			}
			
			List<String> metadataNames = (List<String>)PublicIterParams.get(original_request, ContextVariables.METADATA_NAMES_ARRAY);
			if( Validator.isNotNull(metadataNames) )
			{
				value = ContextVariables.getListAsString( metadataNames );
	
				arrayCtxVars.put( ContextVariables.METADATA_NAMES_ARRAY, value );
			}
			
			List<String> metadataFriendlyNames = (List<String>)PublicIterParams.get(original_request, ContextVariables.METADATA_FRIENDLY_NAMES_ARRAY);
			if( Validator.isNotNull(metadataFriendlyNames) )
			{
				value = ContextVariables.getListAsString( metadataFriendlyNames );
	
				arrayCtxVars.put( ContextVariables.METADATA_FRIENDLY_NAMES_ARRAY, value );
			}
			
			List<String> globalScripts = (List<String>)PublicIterParams.get(original_request, AdvertisementUtil.PARENT_TAGS_IDS);
			if(Validator.isNotNull(globalScripts))
			{
				boolean hideAdvWithFake = AdvertisementUtil.hideAdvWithFake();
				for( String globalScrpt : globalScripts )
				{
					try
					{
						String globalTagScript = "";
						String sql = String.format("select %s from adtags where tagid='%s'", 
										hideAdvWithFake ? "faketagscript" : "tagscript", globalScrpt);
						List<Object> resultList = PortalLocalServiceUtil.executeQueryAsList( String.format( sql, globalScrpt) );
						if (resultList != null && resultList.size() > 0 && Validator.isNotNull(resultList.get(0)))
						{
							globalTagScript = resultList.get(0).toString();
							if (allowCtxVars && ContextVariables.findCtxVars(globalTagScript))
							{
								categoryId 		  = String.valueOf( PublicIterParams.get(original_request, WebKeys.ITER_ADSLOT_CATEGORYID) );
								advertisementType = String.valueOf( PublicIterParams.get(original_request, WebKeys.ITER_ADSLOT_ADTYPE) );
								
								if (Validator.isNull(categoryId) || Validator.isNull(advertisementType))
									advertisementType = AdvertisementUtil.SEGMENTATION_BY_LAYOUT;
								
								Map<String, String> globalCtxVars = null;
								if(advertisementType.equalsIgnoreCase(AdvertisementUtil.SEGMENTATION_BY_LAYOUT))
									globalCtxVars = AdvertisementUtil.getAdvertisementCtxVars(request);
								else
									globalCtxVars = AdvertisementUtil.getAdvertisementCategoryCtxVars(request, scopeGroupId, categoryId);
								ErrorRaiser.throwIfNull(globalCtxVars);
								
								globalTagScript = ContextVariables.replaceCtxVars(globalTagScript, globalCtxVars);
								globalTagScript = ContextVariables.replaceCtxVars(globalTagScript, arrayCtxVars);
							}
%>
							<%= globalTagScript %>
<%
						}
					}
					catch(ServiceError se)
					{
						_log.debug(se);
					}
					catch(Exception e)
					{
						_log.error(e);
					}
				}
			}
		}
	}
%>

</c:if>

<%!
	private static Log _log = LogFactoryUtil.getLog("portal-web.docroot.html.common.themes.top_head_jsp");
%>
