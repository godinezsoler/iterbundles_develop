<%@page import="com.liferay.portal.kernel.util.GroupConfigConstants"%>
<%@page import="com.liferay.portal.kernel.util.GroupConfigTools"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.kernel.util.PHPUtil"%>
<%@page import="com.liferay.portal.kernel.util.request.PublicIterParams"%>
<%@page import="com.protecmedia.iter.user.service.IterRegisterServiceUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.CKEditorUtil"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.protecmedia.iter.user.util.UserUtil"%>
<%@page import="com.protecmedia.iter.user.service.UserOperationsLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.xml.Document"%>
<%@page import="com.liferay.portal.kernel.xml.SAXReaderUtil"%>
<%@page import="com.protecmedia.iter.user.service.UserOperationsServiceUtil"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@ include file="initForgotPwd.jsp" %>

<%
	String configString = UserOperationsLocalServiceUtil.getConfig(String.valueOf(scopeGroupId) );
	Document groupPref = SAXReaderUtil.read( configString ); 
	
	if( groupPref.getRootElement().elements().size() > 0 )
	{
		if(PHPUtil.isApacheRequest(request))
		{
			PublicIterParams.set(WebKeys.ITER_RESPONSE_NEEDS_PHP, true);
		}

		CKEditorUtil.noInheritThemeCSS(UserOperationsLocalServiceUtil.getConfigValue(groupPref, UserUtil.FORGET_SUCCESS_HTML), request);
		
		String mode = GroupConfigTools.getGroupConfigFieldFromDB(scopeGroupId, GroupConfigConstants.FIELD_REGISTER_CONFIRMATION_MODE);
		boolean isOTP 	= GroupConfigConstants.REGISTER_CONFIRMATION_MODE.otp.toString().equals(mode);
		String forgetText = (!isOTP) ? UserOperationsLocalServiceUtil.getConfigValue(groupPref, UserUtil.ENTER_EMAIL_MSG) :
									   UserOperationsLocalServiceUtil.getConfigValue(groupPref, UserUtil.ENTER_PHONE_MSG);

%>
		<?php
			$ref = "";
			if (isset($_SERVER['HTTP_REFERER']))
			{
				$ref = $_SERVER['HTTP_REFERER'];
			}
		?>


<input id="forgotTitle"   type="hidden" value="<%= UserOperationsLocalServiceUtil.getConfigValue(groupPref, UserUtil.FORGET_ALERT_TITLE) %>"/>
<input id="forgotBttText" type="hidden" value="<%= UserOperationsLocalServiceUtil.getConfigValue(groupPref, UserUtil.FORGET_ALERT_OK_BTT) %>"/>

<form id="forgotpasswordform" name='form_forget'>
	<div class="forgetWrapper">
		<div class='field-forget'>
			<div class='text-email-forget'><%= forgetText %></div>
			<div class='input_email'>
				<input id="emailinput" type='text' name='email' size="30" tabindex="1"/>
			</div>
		</div>
		<div class='field-check-forget'>
			<div class='text-info-forget'><%= UserOperationsLocalServiceUtil.getConfigValue(groupPref, UserUtil.FORGET_MSG) %></div>
			<div class='checks-forget'>
				<div class='check-password-forget'>
					<input id="pwdcheck" type='checkbox' name='checkpassword' tabindex="2"/>
					<div class='text-check-password-forget'><%= UserOperationsLocalServiceUtil.getConfigValue(groupPref, UserUtil.FORGET_PWD_MSG) %></div>
				</div>
				<div class='checks-user-forget'>
					<input id="namecheck" type='checkbox' name='checkuser' tabindex="3" />
					<div class='text-check-user-forget'><%= UserOperationsLocalServiceUtil.getConfigValue(groupPref, UserUtil.FORGET_USRNAME_MSG) %></div>
				</div>
			</div>
		</div>
		<div class='input-submit'>
			<input id="sendbtt" type='button' name='restforget' value='<%= UserOperationsLocalServiceUtil.getConfigValue(groupPref, UserUtil.RESET_CREDENTIALS_MSG) %>' 
				onkeydown="javascript:onKeyDown(event)" onclick="javascript:sendReminder(<%= scopeGroupId %>, '<?php echo $ref ?>')" tabindex="4"/>
		</div>
	</div>
</form>

<%
	}
%>

<script type="text/javascript">
	jQryIter(document).ready(function(){
		jQryIter("#sendbtt").addClass('disabled');
		jQryIter("#sendbtt").attr("disabled", "disabled");
		jQryIter("#emailinput").on('input', function() {enableSendButton();});
		jQryIter(".field-check-forget input").change(function(){enableSendButton();});
	});
</script>