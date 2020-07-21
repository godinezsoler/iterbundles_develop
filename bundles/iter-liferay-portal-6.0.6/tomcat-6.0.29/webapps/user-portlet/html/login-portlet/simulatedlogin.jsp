<%@page import="com.liferay.portal.service.PortalLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.user.util.LoginUtil"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.model.Portlet"%>
<%@page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>
<%
Portlet portlet = (Portlet)request.getAttribute(WebKeys.RENDER_PORTLET);

String urlSWF = IterLocalServiceUtil.getSWF( portlet.getContextPath() +"/swf/simulatedLoginportlet_WAR_userportlet");

StringBuilder sbParams = new StringBuilder();

sbParams.append("&portletResource="				).append(portlet.getPortletId());
sbParams.append("&scopeGroupId="				).append(themeDisplay.getScopeGroupId());
sbParams.append("&companyId="					).append(themeDisplay.getCompanyId());
sbParams.append("&languageId="					).append(themeDisplay.getLanguageId());
sbParams.append("&plid="						).append(themeDisplay.getLayout().getPlid());
sbParams.append("&secure="						).append(themeDisplay.isSecure());
sbParams.append("&userId="						).append(themeDisplay.getPermissionChecker().getUserId());
sbParams.append("&lifecycleRender="				).append(themeDisplay.isLifecycleRender());
sbParams.append("&pathFriendlyURLPublic="		).append(themeDisplay.getPathFriendlyURLPublic());
sbParams.append("&pathFriendlyURLPrivateUser="	).append(themeDisplay.getPathFriendlyURLPrivateUser());
sbParams.append("&pathFriendlyURLPrivateGroup="	).append(themeDisplay.getPathFriendlyURLPrivateGroup());
sbParams.append("&serverName="					).append(themeDisplay.getServerName());
sbParams.append("&cdnHost="						).append(themeDisplay.getCDNHost());
sbParams.append("&pathImage="					).append(themeDisplay.getPathImage());
sbParams.append("&pathMain="					).append(themeDisplay.getPathMain());
sbParams.append("&pathContext="					).append(themeDisplay.getPathContext());
sbParams.append("&urlPortal="					).append(themeDisplay.getURLPortal());
sbParams.append("&pathThemeImages="				).append(themeDisplay.getPathThemeImages());

sbParams.append("&groupId="						).append(company.getGroup().getGroupId());
sbParams.append("&layoutUuid="					).append(themeDisplay.getLayout().getUuid());
sbParams.append("&phpMode="                     ).append(true);

%>
<input id="swfpath" type="hidden" value="<%= urlSWF %>"/>
<input id="swfvars" type="hidden" value="<%= sbParams.toString() %>"/>

<c:if test="<%= !themeDisplay.isSignedIn() %>">
	<div id="currentPortletConfig" style="position: fixed; top: 0px; left: 0px; right: 0px; bottom: 0px; z-index: 999999; display: none ">
		<div id="flex-msie"> </div>
	</div>
</c:if>

<%
if(PortalLocalServiceUtil.getIterProductList(request) == null)
{
%>
	<div id="testlogin"> <%= login %> </div>
<%
}
else
{
	String demoname = lu.getValueOfLoginPreference("demo");
%>
	<div class="logedWrapper">
		<div id="testusername" class="usernameLabel"> <%=usrprefix %> <%= demoname %> <%= usrsuffix %> </div>
		<div id="testlogout" class="logoutLabel"> <%= logout %> </div>
	</div>	
<%
}
%>

<script type="text/javascript">
	var urlService = "/c/portal/json_service";
	jQryIter(document).ready(function(){
		if( (jQryIter("#testlogin").size() > 0) && (jQryIter("#currentPortletConfig").size() > 0) )
		{
			jQryIter("#testlogin").removeClass("testAccesLabel");
			jQryIter("#testlogin").addClass("accesLabel");
		}
		else
		{
			jQryIter("#testlogin").removeClass("accesLabel");
			jQryIter("#testlogin").addClass("testAccesLabel");
		}
	});
	
	jQryIter("#testlogin").click(function(){
		if( (jQryIter("#currentPortletConfig").size() > 0) && (jQryIter("#swfpath").val()!="") )
		{
			loginloadSWF(jQryIter("#swfpath").val(), jQryIter("#swfvars").val());
		}
	});
	
	jQryIter("#testlogout").click(function()
	{
		function cbDoLogoutTest(msg) 
		 {
			var exception = msg.exception;
			if(!exception)
			{
				window.location.reload(true);
			}
			else
			{
				var idx = msg.exception.indexOf(":");
				var strExcep = msg.exception.substring(idx+1);
				showError(jQryIter(".errTitle")[0].val(), "OK", strExcep);
			}	            
		 }
	
		var dataService = 
		{
			ITER_HquetpesZ3rvl3tR3qu3z_: "",
			serviceClassName: "com.protecmedia.iter.user.service.LoginServiceUtil",
			serviceMethodName:"doSimulationLogout",
			serviceParameters:"['ITER_HquetpesZ3rvl3tR3qu3z_']",
			doAsUserId: ""
		};
	
		jQryIter.ajax(
		{
			type: "POST",
			url: urlService,
			data: dataService,
			dataType: "json",
			error: function(xhr, status, error) {
				jQryIter.showAlert("error", error);
			},
			success: function(data){
				cbDoLogoutTest(data);
			}
		});
	});
	
	jQryIter("#testusername").click(function(){
		if( (jQryIter("#currentPortletConfig").size() > 0) && (jQryIter("#swfpath").val()!="") )
		{
			loginloadSWF(jQryIter("#swfpath").val(), jQryIter("#swfvars").val());
		}
	});
</script>