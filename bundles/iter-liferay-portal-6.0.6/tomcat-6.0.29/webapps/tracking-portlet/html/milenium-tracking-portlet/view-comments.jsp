<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@ page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@ page import="com.protecmedia.iter.base.service.CommentsConfigServiceUtil"%>

<html>
	<body>
		<div class="comment">	
			<div class="message-box">
				<%
					long groupId = ParamUtil.get(request, "groupId", 0);
					String message = GetterUtil.getString(
							CommentsConfigServiceUtil.getDisqusScript(groupId), StringPool.BLANK);
				%>
				<span><%= message %></span>
			</div>
		</div>
	</body>
</html>