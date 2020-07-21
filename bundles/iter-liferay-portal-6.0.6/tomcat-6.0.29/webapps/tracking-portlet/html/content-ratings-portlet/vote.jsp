<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="java.net.URL"%>

<%@page import="com.liferay.portal.kernel.util.HttpUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.service.LayoutSetLocalServiceUtil"%>
<%@page import="com.liferay.portal.apache.ApacheUtil"%>
<%@page import="com.liferay.portal.kernel.servlet.HttpHeaders"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.liferay.portal.kernel.json.JSONFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.json.JSONObject"%>

<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.tracking.util.TrackingUtil"%>
<%@page import="com.protecmedia.iter.news.service.CountersLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.news.model.Counters"%>

<%
	String contentId = ParamUtil.get(request, "contentId", "");
	long scopeGroupId = ParamUtil.get(request, "scopeGroupId", -1);
	int star = ParamUtil.get(request, "star", 0);

	long currentTotal = 0;
	double currentScore = 0;
	
	//0 - Already voted
	//1 - Thanks for voting
	//2 - Cannot vote
	int messageKey = 2;
	
	if (contentId != null && !contentId.isEmpty() && scopeGroupId > 0 && !contentId.equals(IterKeys.EXAMPLEARTICLEID))
	{
		if(TrackingUtil.isCookie(request, IterKeys.COOKIE_NAME_ITR_COOKIE_USRID))
		{
			if(!TrackingUtil.isCookie(request, String.format(IterKeys.COOKIE_NAME_ALREADY_RATED, contentId)))
			{
				CountersLocalServiceUtil.incrementRating(scopeGroupId, contentId, star);
				long results[] = CountersLocalServiceUtil.getRating(scopeGroupId, contentId);
				if(results != null)
				{
					currentTotal = results[0];
					currentScore = TrackingUtil.round((double)results[1]/currentTotal, 1);
					messageKey = 1;
					
					Cookie cookieAlready = new Cookie(String.format(IterKeys.COOKIE_NAME_ALREADY_RATED, contentId), null);
					cookieAlready.setPath(TrackingUtil.getportletName(request) + "/ratings");
					cookieAlready.setMaxAge(-1);
					response.addCookie(cookieAlready);
					
					if(PHPUtil.isApacheRequest(request))
					{
						String virtualHost = LayoutSetLocalServiceUtil.getLayoutSet(scopeGroupId, false).getVirtualHost();
						if(Validator.isNotNull(virtualHost))
						{
							// http://jira.protecmedia.com:8080/browse/ITER-410
							// "Las peticiones que realiza el tomcat al apache DEBEN seguir siendo http (y puerto 80) ..."
							String invalidateURL = new StringBuilder("http://")
														.append(virtualHost)
														.append(TrackingUtil.getportletName(request))
														.append("/ratings/")
														.append(scopeGroupId)
														.append("/")
														.append(contentId)
														.toString();
							ApacheUtil.notifyToAllApaches(invalidateURL);
						}
					}
				}
				else
				{
					messageKey = 0;
				}
			}
			else
			{
				messageKey = 0;
			}
		}
	}
	
	JSONObject jsonObj = JSONFactoryUtil.createJSONObject();
	jsonObj.put("average", currentScore);
	jsonObj.put("total", currentTotal);
	jsonObj.put("message", messageKey);

	out.print(jsonObj);
	out.flush();
%>