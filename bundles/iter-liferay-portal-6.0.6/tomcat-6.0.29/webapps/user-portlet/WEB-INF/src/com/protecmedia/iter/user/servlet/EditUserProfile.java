package com.protecmedia.iter.user.servlet;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.CookieConstants;
import com.liferay.portal.kernel.util.CookieUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.PublicIterParams;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.PayCookieUtil;
import com.protecmedia.iter.user.service.IterRegisterLocalServiceUtil;
import com.protecmedia.iter.user.util.UserUtil;

public class EditUserProfile extends RegisterUser 
{
	private static final long	serialVersionUID	= 1L;
	private static Log _log = LogFactoryUtil.getLog(EditUserProfile.class);

	protected void doGet( HttpServletRequest request, HttpServletResponse response )
	{
		response.setContentType(WebKeys.CONTENT_TYPE_NO_CACHE);
		
		long groupId = 0;
		
		try
		{
			PortalUtil.setVirtualHostLayoutSet(request);
			groupId = PortalUtil.getScopeGroupId(request);
			
			request.setAttribute(IterKeys.REQUEST_ATTRIBUTE_ROOT_CONTEXT, getServletContext().getContext("/"));
			
			String userIdDogtag = request.getParameter(IterKeys.COOKIE_NAME_USERID_DOGTAG);
			if (Validator.isNotNull(userIdDogtag))
			{
				HttpServletRequest originalRequest = PortalUtil.getOriginalServletRequest(request);
				PublicIterParams.set(originalRequest, IterKeys.COOKIE_NAME_USERID_DOGTAG, userIdDogtag);
				PublicIterParams.set(originalRequest, IterKeys.FLAG_COMPLETE_USER_PROFILE, StringPool.TRUE);
			}
			
			UserUtil.redirectToEditProfilePage(groupId, request, response, false);
		}
		catch (Exception e)
		{
			getLog().error(e);
			UserUtil.redirectTo(response, groupId, UserUtil.REGISTER_ERROR_LAYOUT_NAME);
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	protected Document callFormHandler(HttpServletRequest request, long groupId, List<Node> profileFieldsList, Document userRegisterFormDOM, Map<String, ArrayList> adjuntos) throws Throwable
	{
		return IterRegisterLocalServiceUtil.editUserProfileFormHandler(request, groupId, profileFieldsList, userRegisterFormDOM, adjuntos);
	}

	@Override
	protected JSONObject _doOkPostAction(HttpServletRequest request, HttpServletResponse response, String formid, Document xtraInfo) throws Exception
	{
		JSONObject jsonObj = null;
		String sso = null;
		
		// Tarea http://mdesa2:8008/mantis/view.php?id=8401
		// Si el modo es "edición normal" (IterKeys.EDIT_PROFILE_INTENT_EDITION):
		//	- Regenerará y enviará la cookie USER-DOGTAG si los cambios afectan a alguno de los datos que van incrustados en la cookie.
		//	- Si el cambio afecta al correo electrónico, entonces hay que proceder como durante la fase de registro.
		// 	- Si el cambio NO afecta al correo electrónico, el mensaje de terminación es el configurado como específico para la edición del perfil
		
		// Si el modo es "completar registro" (IterKeys.EDIT_PROFILE_INTENT_REGISTRY):
		//	- La cookie USER-DOGTAG no se regenera, de hecho se borra si existe
		//	- Si el cambio afecta al correo electrónico, entonces hay que proceder como durante la fase de registro.
		//	- Si el cambio NO afecta al correo electrónico:
		//		1- El mensaje de terminación es el configurado como específico para la edición del perfil
		//		2- Generar una acción posterior igual a "redirect" a la url indicada en la cookie metareferer
		
		// Si el modo es "reset de credenciales" (IterKeys.EDIT_PROFILE_INTENT_FORGOT):
		//	- En caso de éxito o error se comportará como el formulario de registro
		final String xpath = "/rs/@%s"; 
		
		String mode = XMLHelper.getTextValueOf(xtraInfo, String.format(xpath, IterKeys.XTRADATA_EDIT_MODE));
		ErrorRaiser.throwIfNull(mode);
		
		boolean sendEmail = Boolean.parseBoolean(XMLHelper.getTextValueOf(xtraInfo, String.format(xpath, IterKeys.XTRADATA_CHANGED_EMAIL)));
		
		if (mode.equals(IterKeys.EDIT_PROFILE_INTENT_EDITION))
		{
			boolean generateCookie = Boolean.parseBoolean(XMLHelper.getTextValueOf(xtraInfo, String.format(xpath, IterKeys.XTRADATA_CHANGED_COOKIE_FIELDS)));
			
			// Si ha modificado un campo de la cookie y NO ha sido el correo electrónico
			if (generateCookie && !sendEmail)
				sso = regenerateCookie(request, response, XMLHelper.getTextValueOf(xtraInfo, String.format(xpath, IterKeys.XTRADATA_USERID)));
			
			if (sendEmail)
			{
				CookieUtil.deleteCookies( request, response, new String[]{IterKeys.COOKIE_NAME_USER_DOGTAG, CookieConstants.COOKIE_NAME_VISITOR_ID});
				jsonObj = super._doOkPostAction(request, response, formid, xtraInfo);
			}
			else
			{
				// Mensaje personalizado para edición y acción según configuración
				String msg = XMLHelper.getTextValueOf( xtraInfo, String.format(xpath, IterKeys.XTRADATA_MSG) );
				if( Validator.isNotNull(msg) )
				{
					if( msg.equals(IterErrorKeys.XYZ_ITR_E_AUTHENTICATION_GROUPID_ZYX) )
						msg = GetterUtil.getString2(XMLHelper.getTextValueOf(getFormActionsDom(request), "/ko/handlerfailmsg"), IterErrorKeys.XYZ_ITR_E_AUTHENTICATION_GROUPID_ZYX);
					else if( msg.equals(IterErrorKeys.XYZ_ITR_E_AUTHENTICATE_TOKEN_IS_NULL_ZYX) )
						msg = GetterUtil.getString2(XMLHelper.getTextValueOf(getFormActionsDom(request), "/ko/handlerfailmsg"), IterErrorKeys.XYZ_ITR_E_AUTHENTICATE_TOKEN_IS_NULL_ZYX);
					else if( msg.equals(IterErrorKeys.XYZ_ITR_E_AUTHENTICATING_USER_ZYX) )
						msg = GetterUtil.getString2(XMLHelper.getTextValueOf(getFormActionsDom(request), "/ko/handlerfailmsg"), IterErrorKeys.XYZ_ITR_E_AUTHENTICATING_USER_ZYX);
					
					setFormMessage(request, msg);
				}
				else 
					setFormMessage( request, XMLHelper.getTextValueOf(getFormActionsDom(request), "/ok/msgeditprofile/text()") );
				getAction(request, formid);
			}
		}
		else if (mode.equals(IterKeys.EDIT_PROFILE_INTENT_REGISTRY))
		{
			if (sendEmail)
				jsonObj = super._doOkPostAction(request, response, formid, xtraInfo);	
			else
			{
				String redirectURLString = CookieUtil.get(request, IterKeys.COOKIE_NAME_SOCIAL_REDIRECT);
				String friendlyURL;
				
				if (Validator.isNotNull(redirectURLString))
				{
					friendlyURL = new URL(redirectURLString).getPath();
				}
				else
				{
					// No existe referer, se redirección será a la página raíz
					friendlyURL = new URL(request.getRequestURL().toString()).getPath();
				}
				
				// Mensaje personalizado para edición y redirección a referer
				setFormMessage( request, XMLHelper.getTextValueOf(getFormActionsDom(request), "/ok/msgeditprofile/text()") );
				setFormAction( 	request, IterKeys.ACTION_REDIRECT );
				setFormLocation(request, friendlyURL);
				
				// Se borran la cookie del referer
				CookieUtil.deleteCookies( request, response, new String[]{IterKeys.COOKIE_NAME_SOCIAL_REDIRECT} );
				
				// Se realiza el login
				doLogin(request, response, XMLHelper.getTextValueOf(xtraInfo, String.format(xpath, IterKeys.XTRADATA_USERID)));
			}
		}
		else if (mode.equals(IterKeys.EDIT_PROFILE_INTENT_FORGOT)) 
		{
			setFormMessage(request, XMLHelper.getTextValueOf(getFormActionsDom(request), "/ok/msgeditprofile/text()"));
			request.setAttribute( IterKeys.XTRADATA_REFERER, XMLHelper.getTextValueOf(xtraInfo, String.format(xpath, IterKeys.XTRADATA_REFERER)) );
			getAction(request, formid);
		}
		else
		{
			// cualquier caso no registrado 
			jsonObj = super._doOkPostAction(request, response, formid, xtraInfo);	
		}
		
		JSONObject jsonResponse = (jsonObj == null) ? createJsonResponse(request) : jsonObj;
		
		// Si hay SSO, modifica la respuesta para que se regenere la cookie en todos los sitios
		if (jsonObj == null && Validator.isNotNull(sso))
			UserUtil.setSsoResponse(request, jsonResponse, sso);
		
		return jsonResponse;
	}

	@Override
	public Log getLog()
	{
		return _log;
	}
	
	/**
	 * 
	 * @param userId
	 * @throws ServiceError 
	 * @throws NoSuchProviderException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws UnsupportedEncodingException 
	 * @throws InvalidKeyException 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	private String regenerateCookie(HttpServletRequest request, HttpServletResponse response, String userId) throws ServiceError, InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, SecurityException, NoSuchMethodException, PortalException, SystemException
	{
		// Añadimos al request un atributo con el id de la cookie
		// Borra la cookie actual 
		// Inicia un login que internamente se conectará al sistema de suscripción si es necesario y regenarará la cookie.
		Cookie oldCookie = (Cookie)ArrayUtil.getValue(CookieUtil.getCookies(request, new String[]{IterKeys.COOKIE_NAME_USER_DOGTAG}), 0);
		Map<String, String> oldCookieMap = PayCookieUtil.getCookieAsMap( PayCookieUtil.plainCookieValue(oldCookie.getValue()), IterKeys.COOKIE_NAME_USER_DOGTAG );
		request.setAttribute( IterKeys.REQUEST_ATTRIBUTE_COOKIE_ID, oldCookieMap.get(IterKeys.COOKIE_NAME_SESS_ID) );
		
		CookieUtil.deleteCookies( request, response, new String[]{IterKeys.COOKIE_NAME_USER_DOGTAG, CookieConstants.COOKIE_NAME_VISITOR_ID} );
		return doLogin(request, response, userId);
	}
	
	/**
	 * 
	 * @param request
	 * @param response
	 * @param xtraInfo
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws ServiceError 
	 */
	private String doLogin(HttpServletRequest request, HttpServletResponse response, String userId) throws PortalException, SystemException, SecurityException, NoSuchMethodException, ServiceError
	{
		ErrorRaiser.throwIfNull(userId);
		Document dom = PortalLocalServiceUtil.executeQueryAsDom(String.format("SELECT usrname, pwd from iterusers WHERE usrid='%s'", userId));

		String username = XMLHelper.getTextValueOf(dom, "/rs/row/@usrname");
		ErrorRaiser.throwIfNull(username);

		String pwd = XMLHelper.getTextValueOf(dom, "/rs/row/@pwd");
		ErrorRaiser.throwIfNull(username);

		return UserUtil.doLogin(request, response, username, pwd);
	}
}
