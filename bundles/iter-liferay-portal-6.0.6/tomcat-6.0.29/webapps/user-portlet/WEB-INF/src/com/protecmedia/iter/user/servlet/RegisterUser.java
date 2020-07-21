package com.protecmedia.iter.user.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Digester;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.IterUserTools;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.util.request.PublicIterParams;
import com.liferay.portal.kernel.xml.Attribute;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.PayCookieUtil;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.base.service.util.TeaserMgr;
import com.protecmedia.iter.user.service.IterRegisterLocalServiceUtil;
import com.protecmedia.iter.user.service.IterUserMngLocalServiceUtil;
import com.protecmedia.iter.user.service.UserOperationsLocalServiceUtil;
import com.protecmedia.iter.user.util.IterRegisterQueries;
import com.protecmedia.iter.user.util.UserUtil;
import com.protecmedia.iter.user.util.forms.ExtractorXML;

public class RegisterUser extends FormReceiver implements IFormServlet
{
	private static final long serialVersionUID = 1L;
	private static Log _log = LogFactoryUtil.getLog(RegisterUser.class);
	
	protected void doGet( HttpServletRequest request, HttpServletResponse response )
	{
		response.setContentType(WebKeys.CONTENT_TYPE_NO_CACHE);
		
		long groupId = 0;
		
		try
		{
			PortalUtil.setVirtualHostLayoutSet(request);
			groupId = PortalUtil.getScopeGroupId(request);
			
			ErrorRaiser.throwIfNull(request.getParameter("p"));
			String p = URLEncoder.encode( request.getParameter("p"), Digester.ENCODING );
			String plainToken = PayCookieUtil.plainCookieValue(p);
			Map<String, String> tokenMap = PayCookieUtil.getCookieAsMap( plainToken, IterKeys.COOKIE_NAME_USERID_DOGTAG );
			
			if(IterKeys.EDIT_PROFILE_INTENT_CONFIRM.equals(tokenMap.get(IterKeys.COOKIE_NAME_INTENT)))
			{
				IterRegisterLocalServiceUtil.confirmUserRegister( groupId, tokenMap.get(IterKeys.COOKIE_NAME_USER_ID), tokenMap.get(IterKeys.COOKIE_NAME_EXPIRE), response );
			}
		}
		catch (Exception e)
		{
			getLog().error(e);
			UserUtil.redirectTo(response, groupId, UserUtil.REGISTER_ERROR_LAYOUT_NAME);
		}
	}
	
	@SuppressWarnings("rawtypes")
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String formid = request.getParameter("formid");
		
		JSONObject json = null;
		
		try 
		{
			ErrorRaiser.throwIfFalse(Validator.isUnhyphenatedUUID(formid), IterErrorKeys.XYZ_E_INVALID_FORMID_ZYX);
			PortalUtil.setVirtualHostLayoutSet(request);
			long groupId = PortalUtil.getScopeGroupId(request);
			
			//Se obtiene el formulario de registro que acaba de rellenar y enviar el usuario.
			Map<String, ArrayList> adjuntos = new HashMap<String, ArrayList>();
			Document userRegisterFormDOM = ExtractorXML.createXML(request, true, adjuntos);
			
			if( Validator.isNotNull(request.getParameter(IterKeys.COOKIE_NAME_USERID_DOGTAG)) )
			{
				String encoded = URLEncoder.encode(request.getParameter(IterKeys.COOKIE_NAME_USERID_DOGTAG), Digester.ENCODING);
				
				HttpServletRequest originalRequest = PortalUtil.getOriginalServletRequest(request);
				PublicIterParams.set(originalRequest, IterKeys.COOKIE_NAME_USERID_DOGTAG, encoded);
			}
			
			//Se obtiene el formulario de registro para conocer los campos que lo forman 
			List<Node> profileFieldsList = getFormFieldsInfo(userRegisterFormDOM);
			
			// Se registra el usuario
			Document xtraInfo = callFormHandler(request, groupId, profileFieldsList, userRegisterFormDOM, adjuntos);
			
			// Se registran sus suscripciones
			processNewsletterSubscriptions(xtraInfo, userRegisterFormDOM);
			
			json = doOkPostAction(request, response, formid, xtraInfo);
			
			// Recupera el ID del usuario y lo elimina de la salida.
			String usrId = xtraInfo.getRootElement().attribute(IterKeys.XTRADATA_USERID).getValue();
			Document userDom = PortalLocalServiceUtil.executeQueryAsDom(String.format("SELECT usrname, pwd, userexpires FROM iterusers WHERE usrid='%s'", usrId));
			
			// Se registra el readerId
			UserUtil.registerReaderId(groupId, usrId, request.getParameter("rid"));

			// Comprueba si el usuario está pendiente de confirmación. Si no es así, procede al autologin.
			if (Validator.isNull(XMLHelper.getTextValueOf(userDom, "/rs/row/@userexpires")))
			{
				// Recupera el nombre de usuario y la contraseña en md5
				String usrName = IterUserTools.decryptGDPR( XMLHelper.getTextValueOf(userDom, "/rs/row/@usrname") );
				String pwd = XMLHelper.getTextValueOf(userDom, "/rs/row/@pwd");
				
				// Realiza el login
				String sso = UserUtil.doLogin(IterRequest.getOriginalRequest(), response, usrName, pwd);
				
				// Si hay SSO, modifica la respuesta para que se regenere la cookie en todos los sitios
				if (Validator.isNotNull(sso))
					UserUtil.setSsoResponse(request, json, sso);
			}
		}
		catch(Throwable t)
		{
			getLog().error(t);
			json = doKoPostAction(request, response, t, formid, null);
		}
		finally
		{
			addResponse(response, json);
		}
	}
	
	String SQL_UPDATE_USER_NEWSLETTER_FLAG = "UPDATE iterusers SET newsletter = TRUE WHERE usrid = '%s'";
	
	private void processNewsletterSubscriptions(Document xtraInfo, Document userRegisterFormDOM) throws IOException, SQLException
	{
		// Recupera el ID del usuario y lo elimina de la salida.
		Attribute usrId = xtraInfo.getRootElement().attribute(IterKeys.XTRADATA_USERID);
		// Comprueba si hay que suscribir al usuario a newsletters
		Node newsletters = userRegisterFormDOM.selectSingleNode("/formdata/selectednewsletters");
		if (newsletters != null)
		{
			// Añade el ID del usuario al XML
			((Element) newsletters).addAttribute("usrid", usrId.getValue());
			// Suscribe al usuario a las newsletters que seleccionó
			IterUserMngLocalServiceUtil.processNewsletterSubscriptions((Element) newsletters);
		}
		// En cualquier caso, marca al usuario como que ya ha pasado por el formulario para que no se vuelva a mostrar si es un campo obligatorio
		String sql = String.format(SQL_UPDATE_USER_NEWSLETTER_FLAG, usrId.getValue());
		PortalLocalServiceUtil.executeUpdateQuery(sql);
	}
	
	/**
	 * 
	 * @param request
	 * @param groupId
	 * @param profileFieldsList
	 * @param formActions
	 * @param userRegisterFormDOM
	 * @param adjuntos
	 * @throws Throwable 
	 */
	@SuppressWarnings("rawtypes")
	protected Document callFormHandler(HttpServletRequest request, long groupId, List<Node> profileFieldsList, Document userRegisterFormDOM, Map<String, ArrayList> adjuntos) throws Throwable
	{
		return IterRegisterLocalServiceUtil.registerFormHandler(request, groupId, profileFieldsList, userRegisterFormDOM, adjuntos);
	}
	
	private List<Node> getFormFieldsInfo( Document userRegisterFormDOM ) throws SecurityException, NoSuchMethodException, ServiceError, DocumentException, IOException
	{
		//Recuperamos el fieldid de los campos que forman el formulario
		List<Node> inputCtrlIds = userRegisterFormDOM.selectNodes("//fieldsgroup/field/@id");
		String inClause = TeaserMgr.getInClauseSQL(inputCtrlIds);
		
		//Recuperamos la información de los campos que forman el formulario.(fieldid, fieldclass, fieldtype, profilefieldid, fieldname) 
		String query = String.format( IterRegisterQueries.GET_USRPROFILE_FIELDS, inClause );
		Document profileFieldsDom = PortalLocalServiceUtil.executeQueryAsDom( query );
		
		return profileFieldsDom.selectNodes("//row");
	}
	
	@Override
	protected JSONObject _doKoPostAction(HttpServletRequest request, HttpServletResponse response, Throwable t, String formid, Document xtraInfo) throws Exception
	{
		Document groupConfigDom = UserOperationsLocalServiceUtil.getConfigDom( getFormGroupId(request) );
		JSONObject json = null;
		String message 	= null;
		String eCode 	= "";
		
		if (t instanceof ServiceError)
			eCode = ((ServiceError)t).getErrorCode();
		else if (t instanceof ORMException)
			eCode = ServiceErrorUtil.getErrorCode((ORMException)t);

		if (eCode.equals(IterErrorKeys.XYZ_ITR_UNQ_USER_NAME_ZYX))
			message = GetterUtil.getString2(UserOperationsLocalServiceUtil.getConfigValue(groupConfigDom, UserUtil.USER_NAME_EXISTS_MSG), 
					IterErrorKeys.XYZ_ITR_UNQ_USER_NAME_ZYX);

		else if (eCode.equals(IterErrorKeys.XYZ_ITR_UNQ_PHONE_ZYX))
			message = GetterUtil.getString2(UserOperationsLocalServiceUtil.getConfigValue(groupConfigDom, UserUtil.PHONE_EXISTS_MSG),
					IterErrorKeys.XYZ_ITR_UNQ_PHONE_ZYX);

		else if (eCode.equals(IterErrorKeys.XYZ_E_OTP_VALIDATION_HAS_FAILED_ZYX))
			message = GetterUtil.getString2(UserOperationsLocalServiceUtil.getConfigValue(groupConfigDom, UserUtil.OTP_VALIDATION_HAS_FAILED_MSG), 
					IterErrorKeys.XYZ_E_OTP_VALIDATION_HAS_FAILED_ZYX);

		else if (eCode.equals(IterErrorKeys.XYZ_ITR_UNQ_EMAIL_ZYX))
			message = GetterUtil.getString2(UserOperationsLocalServiceUtil.getConfigValue(groupConfigDom, UserUtil.EMAIL_EXISTS_MSG),
					IterErrorKeys.XYZ_ITR_UNQ_EMAIL_ZYX);
		
		else if (eCode.equals(IterErrorKeys.XYZ_ITR_UNQ_ABO_ID_ZYX))
			message = GetterUtil.getString2(UserOperationsLocalServiceUtil.getConfigValue(groupConfigDom, UserUtil.ABOID_EXISTS_MSG),
					IterErrorKeys.XYZ_ITR_UNQ_ABO_ID_ZYX);
		
		else if (eCode.equals(IterErrorKeys.XYZ_ITR_E_AUTHENTICATION_GROUPID_ZYX))
			message = GetterUtil.getString2(XMLHelper.getTextValueOf(getFormActionsDom(request), "/ko/handlerfailmsg"),
					IterErrorKeys.XYZ_ITR_E_AUTHENTICATION_GROUPID_ZYX);
		
		else if(eCode.equals(IterErrorKeys.XYZ_ITR_E_AUTHENTICATING_USER_ZYX))
			message = GetterUtil.getString2( 
							GetterUtil.getString2(t.getMessage(), XMLHelper.getTextValueOf(getFormActionsDom(request), "/ko/handlerfailmsg")),
							IterErrorKeys.XYZ_ITR_E_AUTHENTICATING_USER_ZYX);
		
		else if( eCode.equals(IterErrorKeys.XYZ_ITR_E_AUTHENTICATE_TOKEN_IS_NULL_ZYX) )
			message = GetterUtil.getString2(XMLHelper.getTextValueOf(getFormActionsDom(request), "/ko/handlerfailmsg"), 
					IterErrorKeys.XYZ_ITR_E_AUTHENTICATE_TOKEN_IS_NULL_ZYX);

		if ( Validator.isNull(message) )
		{
			// Si no hay mensaje definido para los errores capturados
			json = super._doKoPostAction(request, response, t, formid, xtraInfo);
		}
		else
		{
			setFormMessage(request, message);
			json = createJsonResponse(request);
		}
		
		return json;
	}
	
	@Override
	protected JSONObject _doOkPostAction(HttpServletRequest request, HttpServletResponse response, String formid, Document xtraInfo) throws Exception
	{
		String xpath = "/rs/@%s";
		String emailChanged = XMLHelper.getTextValueOf( xtraInfo, String.format(xpath, IterKeys.XTRADATA_CHANGED_EMAIL) );

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
			setFormMessage(request, XMLHelper.getTextValueOf(getFormActionsDom(request), "/ok/msg"));
		
		getAction(request, formid);
		
		if( GetterUtil.getBoolean(emailChanged, false) )
		{
			IterRegisterLocalServiceUtil.initOrUpdateUserToDeleteTask();
			
			String email = XMLHelper.getTextValueOf( xtraInfo, String.format(xpath, IterKeys.XTRADATA_EMAIL) );
			
			IterRegisterLocalServiceUtil.sendConfirmMail(getFormGroupId(request), email, request);
		}
		
		return createJsonResponse(request);
	}
	
	@Override
	protected JSONObject createJsonResponse(HttpServletRequest request)
	{
		JSONObject json = super.createJsonResponse(request);
		
		// ITER-1373 Version plantilla AMP para notas bajo una suscripcion.
		// Si se especifica un origin se redirige a este
		String origin = request.getParameter("origin");
		if (Validator.isNotNull(origin))
		{
			JSONObject jsonFurtheraction = json.getJSONObject(FormReceiver.FURTHERACTION);
			
			String redirect = String.format("%s#success=%s", origin, IterKeys.OK.equals(json.getString(FormReceiver.RESULT)) ? "true":"false");
			
			jsonFurtheraction.put(FormReceiver.ACTION, IterKeys.ACTION_REDIRECT);
			jsonFurtheraction.put(FormReceiver.LOCATION, redirect );
		}
		
		getLog().debug("returns : " + json.toString());
		
		return json;
	}

	
	@Override
	public Log getLog()
	{
		return _log;
	}
}
