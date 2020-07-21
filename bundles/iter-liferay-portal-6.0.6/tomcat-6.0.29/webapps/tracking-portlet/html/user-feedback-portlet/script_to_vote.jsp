<% 
if(PropsValues.ITER_ENVIRONMENT.equals(WebKeys.ENVIRONMENT_LIVE))
{
	String servletPath = TrackingUtil.getportletName(request);
	servletPath = servletPath.concat("/feedback/"); 
	%>
	<script>
	jQryIter.registerOnLoadFunction
	(
		function()
		{
			jQryIter.getFeedback('<%=servletPath%>', '<%=scopeGrpId%>', '<%=contentId%>', '<%=IterKeys.COOKIE_NAME_ITR_COOKIE_USRID%>', '<%=String.format(IterKeys.COOKIE_NAME_CONTENT_VOTED, StringPool.BLANK, StringPool.BLANK)%>');
		}
	);
	jQryIter.registerOnLoadFunction
	(
		function()
		{
			jQryIter.setMessages('<%= feedbackData.get(0).get("thanksmsg") %>', '<%= feedbackData.get(0).get("actcookiesmsg") %>', '<%= feedbackData.get(0).get("existsvotemsg") %>');
		}
	);

	</script>
<% 
}
%>