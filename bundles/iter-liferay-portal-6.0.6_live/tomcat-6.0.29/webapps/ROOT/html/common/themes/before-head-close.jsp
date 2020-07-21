<%@page import="com.protecmedia.iter.base.metrics.VisitsStatisticMgr"%>
<%@page import="com.liferay.portal.kernel.util.SectionUtil"%>
<%@page import="com.liferay.portal.kernel.util.ABTestingMgr"%>
<%@page import="com.liferay.portal.util.PropsValues"%>
<%@page import="com.liferay.portal.kernel.util.IterURLUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.util.GroupConfigTools"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.liferay.portal.theme.ThemeDisplay"%>
<%@page import="com.liferay.portal.kernel.util.StringUtil"%>
<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>

<%@page import="com.liferay.portal.kernel.xml.SAXReaderUtil"%>
<%@page import="com.liferay.portal.kernel.xml.XMLHelper"%>
<%@page import="com.liferay.portal.kernel.xml.Document"%>
<%@page import="com.liferay.portal.kernel.util.CategoriesUtil"%>
<%@page import="com.liferay.portal.kernel.metrics.MetricsTools"%>

<%@page import="com.liferay.portal.kernel.util.InstantArticlePageTools"%>

<%@ include file="/html/common/init.jsp" %>

<%
	HttpServletRequest original_request = PortalUtil.getOriginalServletRequest(request);
	Object isNewsletterPageObject = original_request.getAttribute(WebKeys.REQUEST_ATTRIBUTE_IS_NEWSLETTER_PAGE);
	boolean isNewsletterPage = false;
	if(isNewsletterPageObject != null)
		isNewsletterPage = GetterUtil.getBoolean(isNewsletterPageObject.toString());

	
	String scopeGroupFriendlyURL = themeDisplay.getScopeGroupFriendlyURL();
%>	

<%	
	// Si estamos en el LIVE y es la home, inserta los tag para reclamar las URL para los Instant Article de Facebook
	if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_LIVE) && LayoutConstants.isHome(themeDisplay.getLayout()))
	{
		List<Long> facebookPages = InstantArticlePageTools.getInstantArticlePages(scopeGroupId);
		for (Long pageId : facebookPages)
		{
%>
			<meta property="fb:pages" content="<%=pageId%>" />
<%			
		}
	}
%>


<%-- Estadísticas de Visitas --%>
<%
	if ( !isNewsletterPage && IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_LIVE) && PHPUtil.isApacheRequest(request))
	{
		boolean iterStatsEnabled     = PropsValues.ITER_STATISTICS_ENABLED;
		boolean adBlockStatsDisabled = "disabled".equals(GetterUtil.getString(GroupConfigTools.getGroupConfigXMLField(scopeGroupId, "blockeradblock", "@mode"), "disabled"));
		String wpnAppId              = GroupConfigTools.getGroupConfigXMLField(scopeGroupId, "googletools", "/google/metricsmas/@appid");
		boolean masEnabled           = GetterUtil.getBoolean(GroupConfigTools.getGroupConfigXMLField(scopeGroupId, "googletools", "/google/metricsmas/@enablemetrics"), false) && Validator.isNotNull(wpnAppId);
				
		String urlType       = Validator.isNotNull(original_request.getAttribute(WebKeys.REQUEST_ATTRIBUTE_URL_TYPE)) ? original_request.getAttribute(WebKeys.REQUEST_ATTRIBUTE_URL_TYPE).toString() : StringPool.BLANK;
		String articleId     = Validator.isNotNull(original_request.getAttribute(WebKeys.ARTICLEURL_ARTICLEID)) ? original_request.getAttribute(WebKeys.ARTICLEURL_ARTICLEID).toString() : StringPool.BLANK;
		String sectionPlid   = Validator.isNotNull(original_request.getAttribute(WebKeys.SECTION_PLID)) ? original_request.getAttribute(WebKeys.SECTION_PLID).toString() : StringPool.BLANK;
		String categoriesIds = "meta".equals(urlType) ? StringUtil.merge(CategoriesUtil.getCategoriesIds(original_request.getAttribute(WebKeys.REQUEST_ATTRIBUTE_URL_CATEGORIES).toString()), "_") : StringPool.BLANK;
		
		// Estadísticas de IterWeb
		if (iterStatsEnabled)
		{
%>
			<script type="text/javascript">
				jQryIter.addStatisticData(<%=scopeGroupId%>, '<%=VisitsStatisticMgr.ARTICLE_ID%>', '<%=articleId%>');
				jQryIter.addStatisticData(<%=scopeGroupId%>, 'plid', '<%=sectionPlid%>');
				jQryIter.addStatisticData(<%=scopeGroupId%>, 'urlType', '<%=urlType%>');
				jQryIter.addStatisticData(<%=scopeGroupId%>, 'categoriesIds', '<%=categoriesIds%>');
<%
				if (urlType.equals(SectionUtil.URLTYPE_DETAIL))
				{
					%>
					var variant = jQryIter.getQueryParam('<%=ABTestingMgr.VARIANT%>');
					if (variant.length > 0)
					{
						jQryIter.addStatisticData(<%=scopeGroupId%>, '<%=VisitsStatisticMgr.VARIANT%>', variant);
						jQryIter.addStatisticData(<%=scopeGroupId%>, '<%=VisitsStatisticMgr.VARIANTID%>', jQryIter.getQueryParam('<%=ABTestingMgr.VARIANTID%>'));
						jQryIter.addStatisticData(<%=scopeGroupId%>, '<%=VisitsStatisticMgr.EXTERNAL%>', (location.host != jQryIter.getQueryParam('<%=ABTestingMgr.REFERER%>')) ? 1 : 0);
					}
					<%
				}
%>
				jQryIter.sendStatistics();
			</script>
<%
		}
%>

<%= ABTestingMgr.getImpresionStatisticCode() %>
		
<%
		// Estadísticas de MAS
		if (masEnabled)
		{
%>
			<script>
				var MASStatsMgr = new Iter2MAS().initialize({
					piwikUrl:     "<%=PropsValues.ITER_MAS_PIWIK_URL%>",
					siteId:       "<%=wpnAppId%>",
					pageType:     "<%=urlType%>",
					searchPrefix: "<%=PortalUtil.getSearchSeparator()%>",
					uid:		  "<?php echo getenv('ITER_USER_ID');?>"	
				});
				MASStatsMgr.sendVisitHit();
			</script>
<%
		}
%>

<%
		if ("detail".equals(urlType) && (iterStatsEnabled || masEnabled))
		{
			int wpm = GetterUtil.getInteger(GroupConfigTools.getGroupConfigXMLField(scopeGroupId, "visitstatistics", "@wordsperminute"), 0);
			
			if (wpm > 0)
			{
%>
			<script type="text/javascript">
				if (typeof ArticleReadingController == 'function')
				{
					jQryIter.registerOnLoadFunction(function() {
						new ArticleReadingController("<%=scopeGroupId%>", "<%=articleId%>", <%=wpm%>, <%=iterStatsEnabled%>)
					});
				}
			</script>
<%
			}
		}
	}
%>

<%-- Google Analytics --%>
<%	
	if ( !isNewsletterPage && scopeGroupFriendlyURL != "/control_panel"  && scopeGroupFriendlyURL != "/guest"  &&  scopeGroupFriendlyURL != "/null")
	{
		String analyticsCode = MetricsTools.getGoogleAnalyticsSource();
%>
		<%=analyticsCode%>
<%		
	}
%>