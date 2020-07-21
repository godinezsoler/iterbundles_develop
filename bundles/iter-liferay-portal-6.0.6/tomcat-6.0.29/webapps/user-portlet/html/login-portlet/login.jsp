<%@page import="sun.net.util.URLUtil"%>
<%@page import="java.net.URL"%>
<%@page import="com.liferay.portal.kernel.util.IterURLUtil"%>
<%@page import="com.liferay.portal.kernel.util.PropsValues"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="com.liferay.portal.kernel.log.LogUtil"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.exception.PortalException"%>
<%@page import="javax.portlet.PortletMode"%>
<%@page import="com.liferay.portlet.PortletURLFactoryUtil"%>
<%@page import="javax.portlet.PortletURL"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.protecmedia.iter.user.service.LoginLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.WidgetUtil"%>

<%
if(PHPUtil.isApacheRequest(request))
{
	PublicIterParams.set(WebKeys.ITER_RESPONSE_NEEDS_PHP, true);
}

String typelogin 	= lu.getValueOfLoginPreference( "typelogin"	 );
String errortitle 	= lu.getValueOfLoginPreference( "errortitle" );
String loginpage 	= lu.getValueOfLoginPreference( "loginpage"  );
String closeLbl		= lu.getValueOfLoginPreference( "closelabel" );
String avatarHTML 	= lu.getAvatarURLHTML(themeDisplay.getScopeGroupId());

%>

<?php
	$usrname  	  = getenv("ITER_USER");

	if(strlen($usrname)!==0)
	{
		$avatar = getenv('ITER_USER_AVATAR_URL');
?>

<form name="form_login">
	<div class="logedWrapper">
		<div class="conect_logout">
			<%= usrprefix %>
		</div>
		<div class="user_logued">
			<div id="userimage" class="userimage">
				<%= avatarHTML %>
			</div>
			<div id="username" class="usernameLabel"><?php echo $usrname ?></div>
		</div>
		<div class="message_logout">
			<%= usrsuffix %>
		</div>
		<div class="logoutLabel" onclick="javascript:doLogout()"> <%= logout %> </div>
	</div>
</form>

<input class="errTitle" type="hidden" value="<%= StringEscapeUtils.escapeHtml(errortitle) %>"/>
<input id="editprofilepage" type="hidden" value="/user-portlet/edit-user-profile"/>

<?php
	}
	else
	{
?>

	<%
		String loginPortletPage = "";
		if( LoginUtil.LINK_MODE.equalsIgnoreCase(typelogin) )
		{
			if( !loginpage.isEmpty() && loginpage.indexOf(Http.PROTOCOL_DELIMITER)==-1)
			{
				try
				{
					long layoutPlid = LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(loginpage, themeDisplay.getScopeGroupId()).getPlid();
					
					String pName = (String) request.getAttribute(WebKeys.PORTLET_ID);
					
					PortletURL url = PortletURLFactoryUtil.create(request, pName, layoutPlid, PortletMode.VIEW.toString());
					
					loginPortletPage = url.toString().split("\\?")[0];
					loginPortletPage = WidgetUtil.getPortalURL(loginPortletPage, themeDisplay);
					
				}
				catch(PortalException pe)
				{
					_log.error("Login layout " + loginpage + " does not exists in group " + themeDisplay.getScopeGroupId());
				}
			}
			else
				loginPortletPage = loginpage;
		}
	%>
		
	<div id="login" class="accesLabel"> <%= login %> </div>
	
	<c:if test="<%= LoginUtil.LINK_MODE.equalsIgnoreCase(typelogin) %>">
		<input id="loginpage" type="hidden" value="<%= loginPortletPage %>"/>
	</c:if>	

	<c:choose>
		<c:when test="<%= LoginUtil.POPUP_MODE.equalsIgnoreCase(typelogin) %>">
			<div class="login_form_wrapper">
				<div class="login_overlay" style="display:none">
					<div class="closeWrapper"><%= closeLbl %></div>
					<%@ include file="../login-form-portlet/loginForm.jsp" %>
				</div>
			</div>
		</c:when>
		<c:otherwise>
			<script type="text/javascript">
				jQryIter(".box.login a").click(function(event)
				{
		    		if (jQryIter(this).attr('disabled') == 'disabled')
		    			event.preventDefault();
				});
			</script>
		</c:otherwise>
	</c:choose>
<?php
	}
?>

<script type="text/javascript">
	jQryIter(document).ready(function()
	{
	
		if(jQryIter(".login_form_wrapper").size() > 0)
		{
			var loginFormVar = "";
			
			jQryIter("#login").click(function(event)
			{
				loginFormVar = jQryIter(".login_form_wrapper").html();
				jQryIter(".login_form_wrapper").html("");
				jQryIter("body").append(loginFormVar);
				jQryIter(".login_overlay .box.login .title").before( jQryIter(".login_overlay .closeWrapper").detach() );
				jQryIter(".login_overlay").fadeIn('fast');
				jQryIter(".login_overlay .userInput").focus();
				
				if(jQryIter("#registerform").size() > 0)
				{
					jQryIter("#registerform input").attr('disabled', true);
					jQryIter("#registerform .infoWrapper").attr('disabled', true);
				}
			
				jQryIter(".box.login .closeWrapper").click(function()
				{
					jQryIter(".login_overlay").fadeOut('fast');
					jQryIter(".login_overlay").remove();
					jQryIter(".login_form_wrapper").append(loginFormVar);
					if(jQryIter("#registerform").size() > 0)
					{
						jQryIter("#registerform input").attr('disabled', false);
						jQryIter("#registerform .infoWrapper").attr('disabled', false);
					}
				});
			
				jQryIter(".login_overlay .box.login").click(function(event)
				{
					event.stopPropagation();
				});
				
				jQryIter(document).keypress(function(event)
				{
					var keycode = (event.keyCode ? event.keyCode : event.which);
					if(keycode==27 && jQryIter(".login_form_wrapper").html()==="")
						jQryIter(".box.login .closeWrapper").click();
				});
			});
		}
		else
		{
			jQryIter("#login").click(function()
			{
				if(jQryIter("#loginpage").val()!=="")
					window.location.href = jQryIter("#loginpage").val();
			});
		}
		
		jQryIter("#username").click(function()
		{
			if(jQryIter("#editprofilepage").val()!=="")
				window.location.href = jQryIter("#editprofilepage").val();
		});
	});
</script>

<%!
private static Log _log = LogFactoryUtil.getLog("user-portlet.docroot.html.login-portlet.login.jsp");
%>