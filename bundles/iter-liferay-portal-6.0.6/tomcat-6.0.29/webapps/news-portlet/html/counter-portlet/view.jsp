<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>

<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>


<portlet:defineObjects />
<liferay-theme:defineObjects />

<liferay-util:html-bottom>

	<%
if (!themeDisplay.isWidget() && !themeDisplay.isWidgetFragment())
{
	
	String contentId = "";
	
	String _url = themeDisplay.getURLCurrent();
	
	String urlSeparator = "";
	
	if(PortalUtil.getCheckMappingWithPrefix())
		urlSeparator += "/-";
	
	if(!PortalUtil.getNewsMappingPrefix().equals(""))
		urlSeparator += PortalUtil.getNewsMappingPrefix();
	else
		urlSeparator += "/";
	
	
	if (  _url.contains( urlSeparator ) )
	{
		int pos = _url.indexOf( urlSeparator );
		if(pos!=-1){
			String auxUrl = _url.substring(pos+urlSeparator.length());
			int endIdx = auxUrl.indexOf("/")!=-1 ? auxUrl.indexOf("/") : auxUrl.length();
			if( !auxUrl.substring(0, endIdx).equals("date") && !auxUrl.substring(0, endIdx).equals("meta") )
			{
				contentId = renderRequest.getParameter(WebKeys.URL_PARAM_CONTENT_ID);
			}
		}
	}
	
	%>
   
   <c:if test='<%= contentId != null && !contentId.equals("") && 
   			!contentId.equals(IterKeys.EXAMPLEARTICLEID) &&  !contentId.equals(IterKeys.EXAMPLEGALLERYID) && 
			!contentId.equals(IterKeys.EXAMPLEMULTIMEDIAID) && !contentId.equals(IterKeys.EXAMPLEPOLLID) %>'>			
		
		<%
		
		String url = "/news-portlet/html/counter-portlet/visit.jsp";
		
		%>
		
		<script type="text/javascript">
	
			var voteFunction = function () {
				jQryIter.ajax(
				{
					type: 'GET',
					url: '<%= url %>',
					method: 'POST',
					data: 
					{
						articleId: '<%= contentId %>',
						groupId: '<%= scopeGroupId %>'
					}
				});
			};	

			voteFunction();
							
		</script>		
	</c:if>
<%
} //if (!themeDisplay.isWidget() && !themeDisplay.isWidgetFragment())
%> 
</liferay-util:html-bottom>
