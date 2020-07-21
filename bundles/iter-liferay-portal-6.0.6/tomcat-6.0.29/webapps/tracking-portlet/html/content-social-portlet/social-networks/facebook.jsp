<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletMgr"%>

<%
	HttpServletRequest hsr = PortalUtil.getOriginalServletRequest(request);

	if (PublicIterParams.get(hsr, IterKeys.REQUEST_PARAMETER_FACEBOOK_LANGUAGE) == null)
	{
		String facebookLanguage = PortletMgr.getGlobalFbLg(String.valueOf(themeDisplay.getScopeGroupId()));
		if (facebookLanguage.equals(""))
		{
			facebookLanguage = "es_ES";
		}
		PublicIterParams.set(hsr, IterKeys.REQUEST_PARAMETER_FACEBOOK_LANGUAGE, facebookLanguage);
	}
%>


<div id="fb-root"></div>

<div class="fb-like" data-send="<%= fbSendButton %>" data-layout="<%= fbStyle %>" 
	data-show-faces="<%= fbShowFaces %>" data-action="<%= fbDisplayVerb %>"></div>


<script>
	var canonical_url= "";
	if (jQryIter("link[rel=canonical]").size()>0)
	{
		canonical_url = jQryIter("link[rel=canonical]").attr("href");
	}
	else
	{
		canonical_url = document.URL; //Url completa y válida de la nota a comentar document.URL
	}
	jQryIter("div.fb-like").attr("data-href", canonical_url);

</script>
