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

	boolean isApacheRequest = PHPUtil.isApacheRequest(original_request);

%>
<%	
	if ( !isNewsletterPage )
	{
		//Lazzyload Javascript
		Object includeLazyloadJSObject = PublicIterParams.get(original_request, WebKeys.REQUEST_ATTRIBUTE_INCLUDE_LAZYLOAD_JS);
		if((includeLazyloadJSObject != null && GetterUtil.getBoolean(includeLazyloadJSObject.toString())) || !isApacheRequest)
		{
%>
			<script type="text/javascript">
                document.body.addEventListener
				(
					"load",
					function(event)
					{
					   var tgt = event.target;
					   if( tgt.tagName == "IMG")
					   {
						   var srcVal = jQryIter(tgt).attr("src");
						   if ( srcVal == "/news-portlet/img/transparentPixel.png" )
							   jQryIter.lazyLoadSetup( tgt );
					   }
                    },
                    true 
                );
			</script>	
<%
		}
	}
%>
