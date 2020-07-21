<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@page import="com.protecmedia.iter.base.service.util.ServiceError"%>
<%@page import="com.protecmedia.iter.base.service.util.ErrorRaiser"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@ include file="init.jsp" %>

<%@page import="java.util.List"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>

<%!
private static Log _log = LogFactoryUtil.getLog("advertisement-portlet.docroot.html.banner-portlet.view.jsp");

private String replaceText(HttpServletRequest request, String scopeGroupFriendlyName, String txt) throws ServiceError
{
	
	if( ContextVariables.ctxVarsEnabled(scopeGroupFriendlyName) && ContextVariables.findCtxVars(txt) )
	{
		Map<String, String> globalCtxVars =  AdvertisementUtil.getAdvertisementCtxVars(request);
		ErrorRaiser.throwIfNull(globalCtxVars);
		
		txt = ContextVariables.replaceCtxVars(txt, globalCtxVars);
	}
	
	return txt;
}

%>


<% 
	String getBannerId = "1";
	String bannerFlashTemp = "";
	boolean showFlash = false;
	
	try
	{
%>
		<c:choose>
		
			<c:when test='<%= bannerType.equals(IterKeys.TYPE_IMAGE) %>'>
				<c:choose>
					<c:when test='<%= advertisementLabel %>'>
						<div class="advertisement_banner_show_label" >
							<liferay-ui:message key="advertisement-banner-show-advertisement-label" />
	       				</div>
	       			</c:when>
	       		</c:choose>
	       		<c:choose>		
					<c:when test='<%=bannerSourceImage.equals("url") && !bannerSourceImage.equals("") %>'>
						<%
							bannerURLImage = replaceText(request, themeDisplay.getScopeGroupFriendlyURL(), bannerURLImage);
						%>
						<a href="<%= bannerURLImage %>" target="_blank">
			       			<img width="100%" src="<%= bannerURLImage %>"/>
						</a>
					</c:when>	
					<c:when test='<%= bannerSourceImage.equals("library") && !bannerSourceImage.equals("") %>'>
						<%
							bannerLibraryImage = replaceText(request, themeDisplay.getScopeGroupFriendlyURL(), bannerLibraryImage);
						%>
						<a href="<%= bannerLibraryImage %>" target="_blank">
			       			<img width="100%" src="<%= bannerLibraryImage %>"/>
						</a>
					</c:when>	
				</c:choose>				
			</c:when>
			
			
			<c:when test='<%= bannerType.equals(IterKeys.TYPE_FLASH) %>'>
				<c:choose>		
					<c:when test='<%= bannerSourceFlash.equals("url") && !bannerURLFlash.equals("") %>'>
						<%
							bannerURLFlash = replaceText(request, themeDisplay.getScopeGroupFriendlyURL(), bannerURLFlash);
						%>
						<a href="<%= bannerURLFlash %>" target="_blank">						
							<div id="<portlet:namespace />flashcontent" style="height: <%= flashHeight %>; width: <%= flashWidth %>;"></div>
						</a>
						<% bannerFlashTemp = bannerURLFlash;
						   showFlash = true;
						%>
					</c:when>	
					<c:when test='<%= bannerSourceFlash.equals("library") && !bannerLibraryFlash.equals("") %>'>
						<%
							bannerLibraryFlash = replaceText(request, themeDisplay.getScopeGroupFriendlyURL(), bannerLibraryFlash);
						%>
						<a href="<%= bannerLibraryFlash %>" target="_blank">						
							<div id="<portlet:namespace />flashcontent" style="height: <%= flashHeight %>; width: <%= flashWidth %>;"></div>
						</a>
						<% bannerFlashTemp = bannerLibraryFlash; 
						   showFlash = true;
						%>
					</c:when>	
				</c:choose>					
				<c:choose>
					<c:when test='<%= advertisementLabel %>'>
						<div class="advertisement_banner_show_label" >
		       				<liferay-ui:message key="advertisement-banner-show-advertisement-label" />
		       			</div>
		       		</c:when>
		       	</c:choose>
										
				<% if (showFlash){ %>
					<script type="text/javascript">
					jQryIter(document).ready(function(){
						jQryIter("#<portlet:namespace />flashcontent").flash(
								{
									swf: '<%= bannerFlashTemp %>',
									wmode: 'opaque',
									height: '<%= flashHeight  %>',
									version: 9.115,
									width: '<%= flashWidth  %>',
									flashvars:{
									}
								}		
							);
						});
					</script>		
				
				<% } %>
			</c:when>
			
			<c:when test='<%= bannerType.equals(IterKeys.TYPE_HTML) && !bannerTextHTML.equals("")%>'>
				<c:choose>
					<c:when test='<%= advertisementLabel %>'>
						<div class="advertisement_banner_show_label" >
		       				<liferay-ui:message key="advertisement-banner-show-advertisement-label" />
		       			</div>
		       		</c:when>
		       	</c:choose>			
				<div>
					<%
						bannerTextHTML = replaceText(request, themeDisplay.getScopeGroupFriendlyURL(), bannerTextHTML);
					%>
					<%= bannerTextHTML %>
				</div>
			</c:when>
		</c:choose>

<%
	}
	catch(ServiceError se)
	{
		_log.debug(se);
	}
	catch(Exception e)
	{
		_log.error(e);
	}
%>
