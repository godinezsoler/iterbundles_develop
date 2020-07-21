<%--
Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%@ include file="init.jsp" %>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.protecmedia.iter.base.service.util.PortletMgr"%>


<%
	defaultContext = ParamUtil.getBoolean(request, "context", defaultContext);

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

<c:choose>

	<c:when test='<%= (!facebookScreenName.equalsIgnoreCase("")) %>'>
		<div class="fb-page" data-href="https://www.facebook.com/<%=facebookScreenName%>" data-width="<%=width%>" data-height=<%=height%> data-hide-cover="<%=hideHeader%>" data-show-facepile="<%=selectShowFaces%>" data-show-posts="<%=selectStream%>">
            <div class="fb-xfbml-parse-ignore">
                <blockquote cite="https://www.facebook.com/<%=facebookScreenName%>">
                    <a href="https://www.facebook.com/<%=facebookScreenName%>"><%=facebookScreenName%></a>
                </blockquote>
            </div>
        </div>
	</c:when>
	
	<c:otherwise>
 		<c:if test="<%= (themeDisplay.isSignedIn()) %>">
			<span class="portlet-msg-info" style="margin:0px 4px;">
			    <liferay-ui:message key="account-not-configured" />
			</span>		
		</c:if>		     
  	</c:otherwise>
	
</c:choose>
