<%@page import="com.liferay.restapi.resource.article.RestApiRecommendationsUtil"%>
<%@page import="com.liferay.portal.kernel.util.IterGlobal"%>
<%@page import="com.protecmedia.iter.news.paywall.provider.PaypalMgr"%>
<%@page import="com.liferay.portal.kernel.util.request.IterRequest"%>
<%@page import="com.liferay.portal.kernel.util.WidgetLazyload"%>
<%@page import="com.protecmedia.iter.base.service.GroupConfigLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.liferay.portlet.journal.util.JournalArticleTools"%>
<%@page import="com.liferay.portal.model.Layout"%>
<%@page import="com.liferay.portal.util.PropsValues"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.Date"%>
<%@page import="com.liferay.portal.kernel.util.DateUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.util.StringUtil"%>
<%@page import="com.liferay.portal.kernel.util.advertisement.SlotAssignment"%>
<%@page import="com.liferay.portal.kernel.util.ContentTypes"%>
<%@page import="com.protecmedia.iter.base.service.ThemeWebResourcesServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.HtmlUtil"%>
<%@page import="com.liferay.portal.kernel.util.PropsKeys"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.protecmedia.iter.base.service.util.ServiceError"%>
<%@page import="com.protecmedia.iter.base.service.util.ErrorRaiser"%>
<%@page import="com.liferay.portal.kernel.json.JSONObject"%>
<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="com.liferay.portal.kernel.servlet.HttpHeaders"%>
<%@page import="com.liferay.portal.kernel.comments.CommentsConfigBean"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portlet.TeaserUtil"%>
<%@page import="com.liferay.portlet.AdvertisementUtil"%>
<%@page import="com.liferay.portlet.ContextVariables"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.base.service.util.WebResourceUtil"%>
<%@page import="com.liferay.portal.kernel.util.GroupConfigTools"%>

<%@ include file="/html/common/init.jsp" %>

<%-- Portlet CSS References --%>

<%
	boolean themeDatabaseEnabled    = GetterUtil.getBoolean(PropsUtil.get(PropsKeys.ITER_THEME_DATABASE_ENABLED), true) &&
										IterKeys.DEFAULT_THEME.equalsIgnoreCase(themeDisplay.getThemeId());

	boolean usePortletsOwnResources = GetterUtil.getBoolean(PropsUtil.get(PropsKeys.ITER_PORTLETS_USE_OWN_RESOURCES), false);

	HttpServletRequest original_request = PortalUtil.getOriginalServletRequest(request);
	Object isNewsletterPageObject = original_request.getAttribute(WebKeys.REQUEST_ATTRIBUTE_IS_NEWSLETTER_PAGE);
	boolean isNewsletterPage = false;
	if(isNewsletterPageObject != null)
		isNewsletterPage = GetterUtil.getBoolean(isNewsletterPageObject.toString());
		 
	List<Portlet> portlets = (List<Portlet>)request.getAttribute(WebKeys.LAYOUT_PORTLETS);
%>

<c:if test="<%= portlets != null && usePortletsOwnResources%>">
<%
	Set<String> footerPortalCssSet = new LinkedHashSet<String>();

	for (Portlet portlet : portlets) {
		for (String footerPortalCss : portlet.getFooterPortalCss()) {
			if (!HttpUtil.hasProtocol(footerPortalCss)) {
				footerPortalCss = PortalUtil.getStaticResourceURL(request, request.getContextPath() + footerPortalCss, portlet.getTimestamp());
			}

			if (!footerPortalCssSet.contains(footerPortalCss)) {
				footerPortalCssSet.add(footerPortalCss);
	%>

				<link href="<%= HtmlUtil.escape(footerPortalCss) %>" rel="stylesheet" type="text/css" />

	<%
			}
		}
	}

	Set<String> footerPortletCssSet = new LinkedHashSet<String>();

	for (Portlet portlet : portlets) {
		for (String footerPortletCss : portlet.getFooterPortletCss()) {
			if (!HttpUtil.hasProtocol(footerPortletCss)) {
				footerPortletCss = PortalUtil.getStaticResourceURL(request, portlet.getContextPath() + footerPortletCss, portlet.getTimestamp());
			}

			if (!footerPortletCssSet.contains(footerPortletCss)) {
				footerPortletCssSet.add(footerPortletCss);
	%>
				<link href="<%= HtmlUtil.escape(footerPortletCss) %>" rel="stylesheet" type="text/css" />
	<%
			}
		}
	}
	%>

</c:if>

<%-- Portlet JavaScript References --%>

<c:choose>
	<c:when test="<%= !usePortletsOwnResources%>">
		<c:if test="<%= !isNewsletterPage%>">
			<script type='text/javascript' src='/base-portlet/webrsrc/<%out.print(WebResourceUtil.getMD5WebResource(WebResourceUtil.FOOTER));%>.js' ></script>
			
			<!-- Iter Footer Portlet Javascripts -->
			<c:if test="<%= !PHPUtil.isApacheRequest(request) %>">
				<script type='text/javascript' src='/user-portlet/js/iter_js_login-simulation.js'></script>
			</c:if>
		</c:if>
	</c:when>
	<c:otherwise>
		<c:if test="<%= portlets != null %>"> 
<%
				Set<String> footerPortalJavaScriptSet = new LinkedHashSet<String>();
			
				for (Portlet portlet : portlets) {
					for (String footerPortalJavaScript : portlet.getFooterPortalJavaScript())
					{
						if (!HttpUtil.hasProtocol(footerPortalJavaScript))
						{
							footerPortalJavaScript = PortalUtil.getStaticResourceURL(request, request.getContextPath() + footerPortalJavaScript, portlet.getTimestamp());
						}
			
						if (!footerPortalJavaScriptSet.contains(footerPortalJavaScript) && !themeDisplay.isIncludedJs(footerPortalJavaScript)) 
						{
							footerPortalJavaScriptSet.add(footerPortalJavaScript);
%>
							<script src="<%= HtmlUtil.escape(footerPortalJavaScript) %>" type="text/javascript"></script>
<%
						}	
					}
				}
			
				Set<String> footerPortletJavaScriptSet = new LinkedHashSet<String>();
			
				for (Portlet portlet : portlets) {
					for (String footerPortletJavaScript : portlet.getFooterPortletJavaScript())
					{
						if (!HttpUtil.hasProtocol(footerPortletJavaScript))
						{
							footerPortletJavaScript = PortalUtil.getStaticResourceURL(request, portlet.getContextPath() + footerPortletJavaScript, portlet.getTimestamp());
						}
			
						if (!footerPortletJavaScriptSet.contains(footerPortletJavaScript))
						{
							footerPortletJavaScriptSet.add(footerPortletJavaScript);
%>
							<script src="<%= HtmlUtil.escape(footerPortletJavaScript) %>" type="text/javascript"></script>
<%
						}
					}
				}
%> 
		</c:if>
	</c:otherwise>
</c:choose>

<c:if test="<%= themeDisplay.isSignedIn() %>">
	<%@ include file="/html/common/themes/bottom_js.jspf" %>
</c:if>

<%@ include file="/html/common/themes/session_timeout.jspf" %>

<%@ include file="/html/taglib/aui/script/page.jsp" %>

<%-- Raw Text --%>

<%
	StringBundler pageBottomSB = (StringBundler)request.getAttribute(WebKeys.PAGE_BOTTOM);
%>

<c:if test="<%= pageBottomSB != null %>">
<%
	pageBottomSB.writeTo(out);
%>
</c:if>

<%-- Theme JavaScript --%>

<c:choose>
	<c:when test="<%= !themeDatabaseEnabled || layout.isTypeControlPanel()%>">
		<script src="<%= HtmlUtil.escape(PortalUtil.getStaticResourceURL(request, themeDisplay.getPathThemeJavaScript() + "/main.js")) %>" type="text/javascript"></script>
	</c:when>
	<c:otherwise>
		<c:if test="<%= !isNewsletterPage %>">
			<%=ThemeWebResourcesServiceUtil.getWebResourceByPlidAndPlace(themeDisplay.getPlid(), WebResourceUtil.FOOTER, ContentTypes.TEXT_JAVASCRIPT)%>
		</c:if>
	</c:otherwise>
</c:choose>

<%-- User Inputted Layout JavaScript --%>

<c:if test="<%= layout != null && !isNewsletterPage %>">
<%
	UnicodeProperties typeSettings = layout.getTypeSettingsProperties();
%>

	<% // Solo ponemos el tag de javascript si se va a sacar algo
	String javascript1 = GetterUtil.getString(typeSettings.getProperty("javascript-1"));
	String javascript2 = GetterUtil.getString(typeSettings.getProperty("javascript-2"));
	String javascript3 = GetterUtil.getString(typeSettings.getProperty("javascript-3"));
	
	if (Validator.isNotNull(javascript1) || Validator.isNotNull(javascript2) || Validator.isNotNull(javascript3))
	{ %>
		<script type="text/javascript">
			// <![CDATA[
				<%= javascript1 %>
				<%= javascript2 %>
				<%= javascript3 %>
			// ]]>
		</script>
<% } %>
</c:if>

<%-- INTERSTITIAL --%>

<%
	boolean isApacheRequest = PHPUtil.isApacheRequest(original_request);

	if( !isNewsletterPage && Validator.isNotNull( original_request.getAttribute(AdvertisementUtil.ADV_INTER) ) )
	{
		try
		{
			SlotAssignment sa 	= (SlotAssignment)original_request.getAttribute(AdvertisementUtil.ADV_INTER);
			String superid 		= sa.getSuperId();
			String tagname 		= sa.getTagName();
			String tagtype 		= sa.getTagtype();
			String slotName 	= sa.getSlotName();
			String tagscript 	= AdvertisementUtil.hideAdvWithFake() ? sa.getFakeTagScript() : sa.getTagScript();
			String tagid 		= sa.getTagId();
		
			if( ContextVariables.ctxVarsEnabled(themeDisplay.getScopeGroupFriendlyURL()) && ContextVariables.findCtxVars(tagscript) )
			{
				JSONObject categoryInfo = (JSONObject)original_request.getAttribute(AdvertisementUtil.ADV_INTER_CAT);
				
				String categoryId = categoryInfo.getString("categoryId");
				String categoryName = categoryInfo.getString("categoryName");
				String categoryNormalizedName = categoryInfo.getString("categoryNormalizedName");
				String advertisementType = categoryInfo.getString("advertisementType");
				
				//Se añaden las variables de sistema especificas del interstitial
				Map<String, String> localCtxVars = new HashMap<String, String>();
				localCtxVars.put( ContextVariables.ADSLOT_NAME, slotName );
				localCtxVars.put( ContextVariables.TAG_NAME, tagname );
				
				if(Validator.isNotNull(categoryName))
					localCtxVars.put( ContextVariables.METADATA_NAME, categoryName);
				if(Validator.isNotNull(categoryNormalizedName))
					localCtxVars.put( ContextVariables.METADATA_FRIENDLY_NAME, categoryNormalizedName);
				
				Map<String, String> globalCtxVars = null;
				if(advertisementType.equalsIgnoreCase(AdvertisementUtil.SEGMENTATION_BY_LAYOUT))
					globalCtxVars = AdvertisementUtil.getAdvertisementCtxVars(request);
				else
					globalCtxVars = AdvertisementUtil.getAdvertisementCategoryCtxVars(request, themeDisplay.getScopeGroupId(), categoryId);
				
				ErrorRaiser.throwIfNull(globalCtxVars);
				
				//Se sustituyen las variables por el valor correspondiente
				tagscript = ContextVariables.replaceCtxVars(tagscript,globalCtxVars);
				tagscript = ContextVariables.replaceCtxVars(tagscript, localCtxVars);
			}

%>


	<div style="display: none; height: 100%; position: fixed; top: 0; width: 100%; z-index: 1000;" class="interstitial-banner" id="interstitialBannerId">
		<div style="display: table-cell; vertical-align: middle;" class="img">
			<div style="display: table; margin: 0 auto;" class="interstitial-img-wrapper">
				<div style="cursor: pointer;" class="interstitial-close" id="interstitialCloseId"><span>Cerrar</span></div>
				<a target="_blank" href="" style="display: block;">
					<div class="moveScriptHere"> 
						<c:choose>
							<c:when test='<%= tagtype.equalsIgnoreCase("html") %>'>
								<div class="bannerToMove">
									<%= tagscript %>
								</div>
							</c:when>
							<c:when test='<%= tagtype.equalsIgnoreCase("image") %>'>
								<img id="picture" src="<%= tagscript %>" width="500px" />
							</c:when>
							<c:when test='<%= tagtype.equalsIgnoreCase("flash") %>'>
								<div id="flashcontent_interstitial <%=tagid%>" class="aui-swf-content aui-widget aui-component aui-swf aui-widget-content-expanded" style="height: 100%; width: 100%;">
									<object height="100%" width="100%" data="<%= tagscript %>" type="application/x-shockwave-flash" id="aui-3-1-1-12670">
										<param value="opaque" name="wmode">
									    <param value="sameDomain" name="allowScriptAccess">
									    <param value="YUISwfId=aui-3-1-1-12670&amp;YUIBridgeCallback=YUI.AUI._SWF.eventHandler" name="flashVars">
									</object>
								</div>
							</c:when>
						</c:choose>
					</div>
				</a>
			</div>
		</div>
	</div>
	
	
	<script type="text/javascript" charset="utf-8">
		jQryIter(document).ready(function(){
			var hideBanner = function(event) {
				jQryIter('#div1:first');
				jQryIter('html:first').removeClass("interstitial-hidden");
			};
			
			jQryIter('#interstitialCloseId').click(hideBanner);
			
			jQryIter('#interstitialBannerId').css('display','table');
			
			jQryIter('html:first').addClass("interstitial-hidden");
			
			setTimeout(hideBanner, 15000);	
		});
	</script>
<%
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
	
	if ( !isNewsletterPage )
	{
		//Facebook Global Language
		Object obj = PublicIterParams.get(original_request, IterKeys.REQUEST_PARAMETER_FACEBOOK_LANGUAGE);
		String facebookLanguage = "";
		if (obj != null)
		{ 
			facebookLanguage = (String) obj;
			if (facebookLanguage.equals(""))
			{
				facebookLanguage = "es_ES";
			}
	%>


			<script type="text/javascript">
				(function(d, s, id) {
					  var js, fjs = d.getElementsByTagName(s)[0];
					  if (d.getElementById(id)) return;
					  js = d.createElement(s); js.id = id;
					  js.src = "//connect.facebook.net/<%= facebookLanguage %>/all.js#xfbml=1";
					  fjs.parentNode.insertBefore(js, fjs);
				}(document, 'script', 'facebook-jssdk'));
			</script>
	<%
		}
	}

	if ( !isNewsletterPage )
	{
		//Disqus Javascripts
		Object commentsConfigBeanObject = original_request.getAttribute(WebKeys.REQUEST_ATTRIBUTE_COMMENTS_CONFIG_BEAN);
		if(commentsConfigBeanObject != null)
		{
			CommentsConfigBean commentsConfig = (CommentsConfigBean)commentsConfigBeanObject;
			out.print(commentsConfig.getJavascriptDisqusExecuteCode());
			
			out.print(commentsConfig.getJavascriptDisqusConfigCode( original_request, themeDisplay.getScopeGroupId() ));
			
			out.print(commentsConfig.getJavascriptDisqusCountCode());
		}
		
		out.print(JournalArticleTools.getJavascriptArticleVisitsCode(themeDisplay.getScopeGroupId(), true));
	}
	
	if ( !isNewsletterPage )
	{
		//CKEditor Javascript
		Object noInheritThemeCssObject = PublicIterParams.get(original_request, WebKeys.REQUEST_ATTRIBUTE_NO_INHERIT_THEME_CSS);
		if(noInheritThemeCssObject != null && GetterUtil.getBoolean(noInheritThemeCssObject.toString()))
		{
	%>


			<script src="/base-portlet/js/ckEditorWrapperContent.js" type="text/javascript"></script>
			<script type="text/javascript">
				jQryIter(document).ready(function(){
					ckEditorWrapperContent();
				});
			</script>
<%
		}
	}
	
	if ( !isNewsletterPage )
	{
		//datePicker Javascript de los teaser con filtro
		Object includeDatepickerFilterJSObject = PublicIterParams.get(original_request, WebKeys.REQUEST_ATTRIBUTE_INCLUDE_DP_FILTER_JS);
		if( includeDatepickerFilterJSObject != null && GetterUtil.getBoolean(includeDatepickerFilterJSObject.toString()) )
		{
%>
			<script type="text/javascript">
				jQryIter(document).ready( jQryIter.datepickerSetup );
			</script>	

<%
		}
	}
%>

<%-- Notificaciones Web Push --%>
<%
	if(IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_LIVE) && !isNewsletterPage)
	{
		String masEnabled = GroupConfigTools.getGroupConfigXMLField(scopeGroupId, "googletools", "/google/metricsmas/@enablemetrics");
		String wpnAppId = GroupConfigTools.getGroupConfigXMLField(scopeGroupId, "googletools", "/google/metricsmas/@appid");
		String wpnEnableUse = GroupConfigTools.getGroupConfigXMLField(scopeGroupId, "googletools", "/google/metricsmas/notifications/@enableuse");
		String wpnSenderId = GroupConfigTools.getGroupConfigXMLField(scopeGroupId, "googletools", "/google/metricsmas/notifications/@senderid");
		
		if ("true".equalsIgnoreCase(masEnabled) && Validator.isNotNull(wpnAppId) && "true".equalsIgnoreCase(wpnEnableUse) && Validator.isNotNull(wpnSenderId))
		{
			String workerPath = "/firebase-messaging-sw/" + wpnSenderId;
%>
			<script src="https://www.gstatic.com/firebasejs/5.10.0/firebase-app.js"></script>
			<script src="https://www.gstatic.com/firebasejs/5.10.0/firebase-messaging.js"></script>
			<script>
				ITER.WPN.initialize("<%=PropsValues.ITER_WEB_PUSH_NOTIFICATIONS_APP_SERVER_URL%>", "<%=wpnAppId%>", "<%=wpnSenderId%>", "<%=workerPath%>");
			</script>
<%
		}
	}
%>

<%-- reCaptcha --%>
<%
	if ( !isNewsletterPage )
	{
%>
	<script>
		if (jQryIter(".g-recaptcha").size() > 0)
		{
			var iterOnloadCaptcha = function ()
			{
				jQryIter('.g-recaptcha').each(function (i, captcha)
		    	{
					var captchaId = grecaptcha.render(captcha, {});
					var iterId = captcha.getAttribute("data-iterid");
					if (typeof iterId !== 'undefined' && iterId != null)
					{
						jQryIter.trackCaptcha(iterId, captchaId);
					}
		        });
			};
	
			jQryIter.getScript( "https://www.google.com/recaptcha/api.js?onload=iterOnloadCaptcha&render=explicit" );
		}
	</script>

<%
	}
%>

<%-- Artículos favoritos --%>
<%
	if(IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_LIVE) && !isNewsletterPage)
	{
		if (Validator.isNotNull(original_request.getAttribute(WebKeys.ARTICLEURL_ARTICLEID)))
		{
			String articleId = original_request.getAttribute(WebKeys.ARTICLEURL_ARTICLEID).toString();
%>
			<script>
			if (typeof MASStatsMgr !== 'undefined')
			{
				// Tras la carga de artículos favoritos
				ITER.FAVORITE.ARTICLES.onLoad(
					function()
					{
						// Comprueba si es un artículo favorito
						if (ITER.FAVORITE.ARTICLES.isFavorite("<%=articleId%>"))
						{
							// Manda el hit del objetivo de artículo favorito visitado
							MASStatsMgr.notifyFavoriteArticleVisited();
						}
					}
				);
			}
				
			// Tras la carga de los temas favoritos y artículos pendientes
			ITER.FAVORITE.TOPICS.onLoad(
				function()
				{
					// Comprueba si se está viendo un artículo pendiente (sugerido o marcado para leer más tarde)
					if (ITER.FAVORITE.TOPICS.isPending("<%=articleId%>") || ITER.FAVORITE.TOPICS.isReadLater("<%=articleId%>"))
					{
						if (typeof MASStatsMgr !== 'undefined')
						{
							// Manda el hit del objetivo de artículo pendiente visitado
							MASStatsMgr.notifyFavoriteTopicArticleVisited();
						}
						
						// Se marca como visitado
		 				ITER.FAVORITE.TOPICS.markAsRead("<%=articleId%>");
					}
				}
			);
			</script>
<%
		}
	}
%>

<%-- Widgets LazyLoad --%>
<%
	if(WidgetLazyload.isEnable())
	{
%>
		<%-- Llamada a WidgetLayload (ITER-866 Carga bajo demanda de módulos) --%>
		<%=WidgetLazyload.call()%>
<%
	}
%>

<%-- Paypal code --%>
<%
	if(GetterUtil.getBoolean(IterRequest.getAttribute(WebKeys.CONTAINS_PAYPAL_BUTTON), false))
	{
%>
		<%=PaypalMgr.INSTANCE.getPaypalRenderButtonCode(scopeGroupId)%>
<%
	}
%>

<%-- JavaScript del controlador de directos --%>
<%
	if(Validator.isNotNull(IterRequest.getAttribute(WebKeys.INCLUDE_LIVE_EVENT_CODE)))
	{
		String eventContainer = IterRequest.getAttribute(WebKeys.INCLUDE_LIVE_EVENT_CODE).toString();
		if (Validator.isNotNull(original_request.getAttribute(WebKeys.ARTICLEURL_ARTICLEID)))
		{
			String articleId = original_request.getAttribute(WebKeys.ARTICLEURL_ARTICLEID).toString();
%>
			<script src="/news-portlet/js/event/liveEvents.js?v=<%=IterGlobal.getLastIterWebCmsVersion()%>"></script>
			<script>
				jQryIter.registerOnLoadFunction(function() {
					ITER.EVENT.initLiveEvent("<%=articleId%>", jQryIter("<%=eventContainer%>"), <%=PropsValues.ITER_EVENTS_POSTS_REFRESH_DELAY%>);
				});
			</script>
<%
		}
	}
%>

<%-- Recomendaciones de artículos --%>
<%
	if(IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_LIVE) && !isNewsletterPage)
	{
		// Obtiene el literal para el nombre de la acción de los eventos de MAS
		String masStatsConfig = RestApiRecommendationsUtil.getMasRecommendationsConfig(scopeGroupId);
%>
		<script>
			ITER.RECOMMENDATIONS.CORE.init(<%=masStatsConfig%>);
		</script>
<%
	}
%>

<%@ include file="/html/common/themes/disableLinks.jspf" %>
<%@ include file="/html/common/themes/loginComplete.jspf" %>

<%
	// 	Se eliminan todos los atributos que se han añadido al request
	
	original_request.removeAttribute( IterKeys.REQUEST_ATTRIBUTE_CANONICAL_URL );
	original_request.removeAttribute( WebKeys.SECTION_PLID );
	original_request.removeAttribute( WebKeys.MAIN_SECTION );
	original_request.removeAttribute( AdvertisementUtil.ADV_CTX_VARS );
	original_request.removeAttribute( AdvertisementUtil.ADV_INTER );
	original_request.removeAttribute( WebKeys.HIERARCHY_PLIDS );
	original_request.removeAttribute( WebKeys.REQUEST_ATTRIBUTE_COMMENTS_CONFIG_BEAN );
	original_request.removeAttribute( WebKeys.REQUEST_ATTRIBUTE_CATALOG_TIMESTAMP);
	original_request.removeAttribute( IterKeys.REQUEST_ATTRIBUTE_FACETS );
	original_request.removeAttribute( WebKeys.SEARCH_RESULT );

%>

<%!
	private static Log _log = LogFactoryUtil.getLog("portal-web.docroot.html.common.themes.bottom.jsp");
%>