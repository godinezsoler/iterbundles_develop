<%
	HttpServletRequest disqusOriginalRequest = PortalUtil.getOriginalServletRequest(request);

	String currentURL = PortletMgr.getCurrentURL(disqusOriginalRequest);
	
	boolean isDetailPage = true;
	if(!currentURL.contains(PortalUtil.getNewsMappingPrefix()))
	{
		contentId = String.valueOf(themeDisplay.getLayout().getPlid());
		isDetailPage = false;
	}
	
	Object commentsConfigBeanObject = disqusOriginalRequest.getAttribute(WebKeys.REQUEST_ATTRIBUTE_COMMENTS_CONFIG_BEAN);
	if(commentsConfigBeanObject != null)
	{
		CommentsConfigBean commentsConfig = (CommentsConfigBean)commentsConfigBeanObject;
		out.print(commentsConfig.getHTMLDisqusCode());
		out.print(commentsConfig.getJavascriptDataDisqusCode( "", contentId, isDetailPage));
	}
%>