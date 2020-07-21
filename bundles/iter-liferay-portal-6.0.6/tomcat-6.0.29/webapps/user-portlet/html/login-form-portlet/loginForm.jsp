<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="com.protecmedia.iter.user.service.SocialMgrServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.liferay.portal.kernel.servlet.HttpHeaders"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.exception.PortalException"%>
<%@page import="com.liferay.portal.kernel.util.Http"%>
<%@page import="javax.portlet.PortletMode"%>
<%@page import="com.liferay.portlet.PortletURLFactoryUtil"%>
<%@page import="javax.portlet.PortletURL"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.service.LayoutLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.protecmedia.iter.user.util.LoginUtil"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>

<%@page import="com.liferay.portal.kernel.util.WidgetUtil"%>

<%

PublicIterParams.set(WebKeys.HAS_LOGINGFORM, true);

String wndTitle			= lu.getValueOfLoginPreference( "logintitle" );
String signinlabel		= lu.getValueOfLoginPreference( "signinlabel" );
String userLbl			= lu.getValueOfLoginPreference( "user" );
String password 		= lu.getValueOfLoginPreference( "password" );
String keeploggedlabel 	= lu.getValueOfLoginPreference( "keeploggedlabel" );
String submit			= lu.getValueOfLoginPreference( "submit" );
String forgetlabel 		= lu.getValueOfLoginPreference( "forgetlabel" );
String required			= lu.getValueOfLoginPreference( "required" );
String errortit 		= lu.getValueOfLoginPreference( "errortitle" );

String onClickFunction 	= "alert('Option available only in public web')";
String registerPortletPage 		= "";
String forgetpassPortletPage 	= "";

String pName = (String) request.getAttribute(WebKeys.PORTLET_ID);
PortletURL url = null;
long layoutPlid = 0;
String registerpage = lu.getValueOfLoginPreference( "signin" );
if( !registerpage.isEmpty() && registerpage.indexOf(Http.PROTOCOL_DELIMITER)==-1 )
{
	try
	{
		layoutPlid = LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(registerpage, themeDisplay.getScopeGroupId()).getPlid();
		url = PortletURLFactoryUtil.create(request, pName, layoutPlid, PortletMode.VIEW.toString());
		registerPortletPage = url.toString().split("\\?")[0];
		
		registerPortletPage = WidgetUtil.getPortalURL(registerPortletPage, themeDisplay);
	}
	catch(PortalException pe)
	{
		_log_F.error("Register layout " + registerpage + " does not exists in group " + themeDisplay.getScopeGroupId());
	}
}
else
	registerPortletPage = registerpage;

String forgetpasspage = lu.getValueOfLoginPreference( "forget" );
if( !forgetpasspage.isEmpty() && forgetpasspage.indexOf(Http.PROTOCOL_DELIMITER)==-1 )
{
	try
	{
		layoutPlid = LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(forgetpasspage, themeDisplay.getScopeGroupId()).getPlid();
		url = PortletURLFactoryUtil.create(request, pName, layoutPlid, PortletMode.VIEW.toString());			
		forgetpassPortletPage = url.toString().split("\\?")[0];
		forgetpassPortletPage = WidgetUtil.getPortalURL(forgetpassPortletPage, themeDisplay);
	}
	catch(PortalException pe)
	{
		_log_F.error("Fotget password layout " + forgetpasspage + " does not exists in group " + themeDisplay.getScopeGroupId());
	}
}
else
	forgetpassPortletPage = forgetpasspage;

boolean gotoReferer = GetterUtil.getBoolean( (String)request.getAttribute(LoginUtil.GOTO_REFERER) , false );
	
if(PHPUtil.isApacheRequest(request))
{
%>
	<c:choose>
		<c:when test="<%= gotoReferer %>">
			<?php
				$ref = "";
				if (isset($_SERVER['HTTP_REFERER']))
				{
					$ref = $_SERVER['HTTP_REFERER'];
				}
			?>
		</c:when>
		<c:otherwise>
			<?php
				$ref = "";
			?>
		</c:otherwise>
	</c:choose>
<%
	onClickFunction = "javascript:LoginForm.sendForm(event, '<?php echo $ref ?>' )";
}
%>

<input class="errTitle" type="hidden" value="<%= StringEscapeUtils.escapeHtml(errortit) %>"/>

<form class="box login" name="fm">
	
	<div class="title">
		<%= wndTitle %> 
		<%
		if( !registerPortletPage.isEmpty() )
		{
		%>
			<a href="<%=registerPortletPage%>" class="registerLnk"> <%=signinlabel%> </a>
		<%
		}
		%>
	</div>
	<fieldset class="boxBody">
	
		<div class="fieldsWrapper">
			<div class="userWrapper">
				<label class="userLabel"><%= userLbl %></label>
				<input class="userInput" type="text" onkeydown="javascript:LoginForm.onKeyDown(event)" onkeypress="javascript:LoginForm.onKeyPress(event)" required>
				<span class="requiredElement"> <%= required %> </span>
			</div>
			
			<div class="passwordWrapper">
				<label class="passwordLabel"><%= password %>
				<%
				if( !forgetpassPortletPage.isEmpty() )
				{
				%>
					<a href="<%= forgetpassPortletPage %>" class="rLink"><%= forgetlabel %></a></label>
				<%
				}
				%>
				<input class="inputPassword" type="password" onkeypress="javascript:LoginForm.onKeyPress(event)" required>
				<span class="requiredElement"> <%= required %> </span>
			</div>
		</div>
		
		<%=SocialMgrServiceUtil.getSocialButtonsHTML(String.valueOf(themeDisplay.getScopeGroupId())) %>

	</fieldset>
	<footer>
		<%
		if( !keeploggedlabel.isEmpty() )
		{
		%>
			<label class="keepMeLoggedLabel"><input class="chkKeep" type="checkbox" onkeypress="javascript:LoginForm.onKeyPress(event)"/><%= keeploggedlabel %></label>
	  	<%
		}
		%>
	  <input type ="button" class="btnLogin" value="<%= StringEscapeUtils.escapeHtml(submit) %>" onkeydown="javascript:LoginForm.onKeyDown(event)" onclick="<%= onClickFunction %>"/>
	</footer>
</form>

<script type="text/javascript">
	jQryIter(".box.login a").click(function(event)
	{
   		if (jQryIter(this).attr('disabled') == 'disabled')
   			event.preventDefault();
	});
</script>

<%!
private static Log _log_F = LogFactoryUtil.getLog("user-portlet.docroot.html.login-form-portlet.loginForm.jsp");
%>
