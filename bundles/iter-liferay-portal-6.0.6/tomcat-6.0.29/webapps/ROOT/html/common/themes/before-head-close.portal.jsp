<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.protecmedia.iter.base.service.util.GroupConfigTools"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.liferay.portal.theme.ThemeDisplay"%>
<%@page import="com.liferay.portal.kernel.util.StringUtil"%>
<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>

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
	if ( !isNewsletterPage && scopeGroupFriendlyURL != "/control_panel"  && scopeGroupFriendlyURL != "/guest"  &&  scopeGroupFriendlyURL != "/null")
	{
		//scopeGroupFriendlyURL = "/null" para el grupo con nombre "Global".
				
		if(IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_LIVE))
		{
			//se obtienen las preferencias con GroupConfigTools.java
			String enablemetrics = GroupConfigTools.gaEnablemetrics(scopeGroupId, original_request);
			String gaMonitorId = GroupConfigTools.gaMonitorid(scopeGroupId, original_request);
			if( enablemetrics.equalsIgnoreCase("true") && !gaMonitorId.equals(StringPool.BLANK) )
			{
				String canonicalURLpages= GroupConfigTools.gaCanonicalURLpages(scopeGroupId, original_request);
%>
			<script>
				(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
				(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
				m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
				})(window,document,'script','//www.google-analytics.com/analytics.js','ga');
	
				ga('create', '<%=gaMonitorId%>', 'auto');
<%	
				if(canonicalURLpages.equalsIgnoreCase("true"))
				{
%>	
					var page=jQryIter("link[rel=canonical]");
					if (page.size()>0)
					{
						page = page.attr("href");
						page=page.replace(/htt[p|ps]:\/\//g,"");
						page = page.substring( page.indexOf("/") );
						ga('set', 'page', page);
					}
<%
				}
%>		
				ga('send', 'pageview');
			</script>				
<%		
			}
		}	
	}
%>


