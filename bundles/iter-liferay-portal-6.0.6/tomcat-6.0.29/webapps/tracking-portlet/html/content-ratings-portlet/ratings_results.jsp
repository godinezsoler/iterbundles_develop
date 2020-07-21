<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.json.JSONObject"%>
<%@page import="com.liferay.portal.kernel.json.JSONFactoryUtil"%>

<%@page import="com.protecmedia.iter.news.model.Counters"%>
<%@page import="com.protecmedia.iter.news.service.CountersLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.tracking.util.TrackingUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>

<%
	String contentId = (String)request.getAttribute("contentId");
	long scopeGroupId = Long.parseLong((String)request.getAttribute("scopeGroupId"));
	
	long counterTotal = 0;
	double avgScore = 0.0;
	
	if(Validator.isNotNull(contentId) && scopeGroupId > 0)
	{
		long results[] = CountersLocalServiceUtil.getRating(scopeGroupId, contentId);
		if(results != null)
		{
			counterTotal = results[0];
			avgScore = TrackingUtil.round((double)results[1]/counterTotal, 1);
		}
	}

	// Header
	response.setHeader(WebKeys.ITER_RESPONSE_NEEDS_PHP, "1");
	
	//Content-Type
	String contentType = GetterUtil.getString(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_RATING_CONTENT_TYPE), "");
	if(contentType.isEmpty())
		contentType = "application/x-feedback";
	
	response.setContentType(contentType);
	
	//Cookie
	Cookie stickyCookie = new Cookie(IterKeys.COOKIE_NAME_ITR_COOKIE_USRID, null);
	stickyCookie.setPath("/");
	stickyCookie.setMaxAge(-1);
	response.addCookie(stickyCookie);
	
	JSONObject jsonObj = JSONFactoryUtil.createJSONObject();
	jsonObj.put("average", avgScore);
	jsonObj.put("total", counterTotal);

	out.print(jsonObj);
	out.flush();
%>