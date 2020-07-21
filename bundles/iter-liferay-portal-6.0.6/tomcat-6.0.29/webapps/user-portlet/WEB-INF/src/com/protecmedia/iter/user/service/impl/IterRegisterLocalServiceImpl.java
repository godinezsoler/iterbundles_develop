/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.protecmedia.iter.user.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpStatus;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.crypto.DrupalEncryption;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.HeaderCacheServletResponse;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.CookieUtil;
import com.liferay.portal.kernel.util.DateUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigConstants;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.IterUserBackupMgr;
import com.liferay.portal.kernel.util.IterUserTools;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.UserConstants;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.ContextVariables;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.protecmedia.iter.base.metrics.NewslettersMetricsUtil;
import com.protecmedia.iter.base.metrics.UserMetricsUtil;
import com.protecmedia.iter.base.service.DLFileEntryMgrLocalServiceUtil;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.HotConfigUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.PayCookieUtil;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.user.OTPMgr;
import com.protecmedia.iter.user.scheduler.UserToDeleteTask;
import com.protecmedia.iter.user.service.LoginLocalServiceUtil;
import com.protecmedia.iter.user.service.LoginServiceUtil;
import com.protecmedia.iter.user.service.UserOperationsLocalServiceUtil;
import com.protecmedia.iter.user.service.base.IterRegisterLocalServiceBaseImpl;
import com.protecmedia.iter.user.util.AuthenticateUser;
import com.protecmedia.iter.user.util.FormUtil;
import com.protecmedia.iter.user.util.IterRegisterQueries;
import com.protecmedia.iter.user.util.UserEntitlementsMgr;
import com.protecmedia.iter.user.util.UserKeys;
import com.protecmedia.iter.user.util.UserUtil;
import com.protecmedia.iter.user.util.forms.UserProfileValuesMgr;
import com.protecmedia.iter.user.util.social.SocialNetworkUtil;

/**
 * The implementation of the iter register local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.user.service.IterRegisterLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.user.service.IterRegisterLocalServiceUtil} to access the iter register local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see com.protecmedia.iter.user.service.base.IterRegisterLocalServiceBaseImpl
 * @see com.protecmedia.iter.user.service.IterRegisterLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class})
public class IterRegisterLocalServiceImpl extends IterRegisterLocalServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(IterRegisterLocalServiceImpl.class);
	
	private static ThreadGroup usersToDelete = new ThreadGroup("USERS_TO_DELETE");
	
	private final String SERVLET_FORGOT_PWD		= "/user-portlet/reset-credentials?p=%s";
	private final String SERVLET_CONFIRM_USER	= "/user-portlet/confirm-email?p=%s";
	
	private final String SQL_FORM_DATE_FORMAT = "SELECT ExtractValue(validator, '/validator/@format') format FROM formfield WHERE fieldid='%s'";
	
	public void preRegisterUser(HttpServletRequest request, HttpServletResponse response, String socialName, long groupId, JSONObject user) throws Exception
	{
		ErrorRaiser.throwIfNull(socialName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(user, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Recuperación del identificador de Delegación
		long delegationId = GroupLocalServiceUtil.getGroup(groupId).getDelegationId();
		
		Document socialProfileFields = PortalLocalServiceUtil.executeQueryAsDom(
				String.format(IterRegisterQueries.GET_SOCIAL_CONFIG_FIELDS_BY_SOCIAL_AND_GROUP, socialName, String.valueOf(groupId)));
		
		String usrname 				= SocialNetworkUtil.getUsernameBySocial(socialName, user);
		String email   				= SocialNetworkUtil.getEmailBySocial(socialName, user);

		String userexpires 			= UserUtil.getUserExpiresDateFormated(groupId);
		String userexpiresEnclosed 	= StringUtil.apostrophe(userexpires);

		String[] socialIds =  SocialNetworkUtil.socialIdsFromService(socialName, user);
		
		String queryAlreadyRegister = String.format(IterRegisterQueries.GET_USER_BY_SOCIALID, SocialNetworkUtil.getSocialIdsSQLFilter(socialIds));
		_log.debug("GET_USER_BY_SOCIALID: " + queryAlreadyRegister);
		
		Document isAlreadyRegister = PortalLocalServiceUtil.executeQueryAsDom(queryAlreadyRegister);
		
		if(isAlreadyRegister.selectNodes("//row").size()==0 && Validator.isNotNull(email))
		{
			queryAlreadyRegister = String.format(IterRegisterQueries.GET_USER_BY_EMAIL, IterUserTools.encryptGDPR_Quoted(email), email);
			_log.debug("GET_USER_BY_EMAIL: " + queryAlreadyRegister);
			isAlreadyRegister = PortalLocalServiceUtil.executeQueryAsDom(queryAlreadyRegister);
		}
		
		socialIds = SocialNetworkUtil.socialIdsMerged(socialIds, isAlreadyRegister);

		XPath xpath = SAXReaderUtil.createXPath("//row");
		List<Node> isAlreadyRegisterNode = xpath.selectNodes(isAlreadyRegister);
		
		String usrid = XMLHelper.getTextValueOf(isAlreadyRegister, "rs/row/@usrid");
		String pwd = XMLHelper.getTextValueOf(isAlreadyRegister, "rs/row/@pwd");
		String userexpiresSaved = XMLHelper.getTextValueOf(isAlreadyRegister, "rs/row/@userexpires");
		String updateprofiledate = XMLHelper.getTextValueOf(isAlreadyRegister, "rs/row/@updateprofiledate");

		//Estoy pendiente y he actualizado el perfil
		if(Validator.isNotNull(userexpiresSaved) && Validator.isNotNull(updateprofiledate))
		{
			_log.error("Temporary user \"" + usrid + "\" try to login with after a recreation update");
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_ITR_E_LOGIN_WITH_TEMPORARY_USER_AFTER_UPDATE_ZYX);
		}
		else
		{
			//No estoy registrado ni pendiente de completar
			if (isAlreadyRegisterNode.isEmpty())
			{
				List<Node> socialProfileFieldsList = xpath.selectNodes(socialProfileFields);
				UserProfileValuesMgr profileValues = new UserProfileValuesMgr();
				Map<String, String> iterUsersFields= new HashMap<String, String>();
				
				// Campos fijos
				usrid = SQLQueries.getUUID();
				pwd = IterUserTools.encryptPwd("1t3r35muybu3n0!!");
				usrname = IterUserTools.getUniqueUserName(usrname);
				iterUsersFields.put(UserUtil.PRF_FIELD_USRID, 		 	StringUtil.apostrophe(usrid));
				iterUsersFields.put(UserUtil.PRF_FIELD_USRNAME,		 	StringUtil.apostrophe(usrname));
				iterUsersFields.put(UserUtil.PRF_FIELD_USRPWD, 		 	StringUtil.apostrophe(pwd));
				iterUsersFields.put(UserUtil.PRF_FIELD_USREMAIL,	 	UserUtil.cleanEmail(email));
				iterUsersFields.put(UserUtil.PRF_FIELD_USEREXPIRES,  	userexpiresEnclosed);
				iterUsersFields.put(UserUtil.PRF_FIELD_DELEGATIONID, 	String.valueOf(delegationId));
				iterUsersFields.put(UserUtil.PRF_FIELD_DISQUSID, 		socialIds[SocialNetworkUtil.SOCIAL_NAME_DISQUS_POS]);
				iterUsersFields.put(UserUtil.PRF_FIELD_FACEBOOKID, 		socialIds[SocialNetworkUtil.SOCIAL_NAME_FACEBOOK_POS]);
				iterUsersFields.put(UserUtil.PRF_FIELD_GOOGLEPLUSID, 	socialIds[SocialNetworkUtil.SOCIAL_NAME_GOOGLEPLUS_POS]);
				iterUsersFields.put(UserUtil.PRF_FIELD_TWITTERID, 		socialIds[SocialNetworkUtil.SOCIAL_NAME_TWITTER_POS]);
				
				// Campos mapeados por el usuario
				for (Node socialProfileField:socialProfileFieldsList)
				{
					final boolean structured = GetterUtil.getBoolean( XMLHelper.getTextValueOf(socialProfileField, "@structured") );
					String profilefieldname  = XMLHelper.getTextValueOf(socialProfileField, "@profilefieldname");
					String profilefieldid   = XMLHelper.getTextValueOf(socialProfileField, "@profilefieldid");
					String socialfieldname  = XMLHelper.getTextValueOf(socialProfileField, "@socialfieldname");
					String fieldValue       = SocialNetworkUtil.invokeFieldGetterMethod(socialName, socialfieldname, user);
					
					if (Validator.isNotNull(fieldValue))
					{
						// Excepción para la fecha de cumpleaños: Se pasa a formato SQL
						if (UserUtil.PRF_FIELD_BIRTHDAY.equals(profilefieldname))
						{
							DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
							fieldValue = StringUtil.apostrophe(DateUtil.getDBFormat(df.parse(StringUtil.unquote(fieldValue))));
						}
						
						// Almacena el valor para ser actualizado
						if (structured)
							iterUsersFields.put(profilefieldname, fieldValue);
						else
							profileValues.add2Insert(profilefieldid, fieldValue, null);
					}
				}
				
				// Añade al usuario
				addUser(iterUsersFields, profileValues);

				// Registra la métrica
				UserMetricsUtil.Hit(groupId, UserMetricsUtil.Hit.SOCIAL_REGISTRATION);
			}
			//Estoy registrado o pendiente de completar
			else
			{
				usrname = XMLHelper.getTextValueOf(isAlreadyRegister, "rs/row/@usrname");
				
				PortalLocalServiceUtil.executeUpdateQuery(String.format(IterRegisterQueries.UPDATE_USER_SOCIALIDS, 
						IterUserTools.encryptGDPR_Quoted(socialIds[SocialNetworkUtil.SOCIAL_NAME_DISQUS_POS]), 
						IterUserTools.encryptGDPR_Quoted(socialIds[SocialNetworkUtil.SOCIAL_NAME_FACEBOOK_POS]), 
						IterUserTools.encryptGDPR_Quoted(socialIds[SocialNetworkUtil.SOCIAL_NAME_GOOGLEPLUS_POS]), 
						IterUserTools.encryptGDPR_Quoted(socialIds[SocialNetworkUtil.SOCIAL_NAME_TWITTER_POS]), 
														  usrid));
				
				if (SocialNetworkUtil.isSocialAccountBinding(socialIds, isAlreadyRegister))
					UserMetricsUtil.Hit(groupId, UserMetricsUtil.Hit.BINDING);
			}
	
			if(UserUtil.hasAllRequieredFieldsFilled(groupId, usrid))
			{
				if(isAlreadyRegisterNode.size() == 0)
				{
					SimpleDateFormat sdf = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss);
					String registerDate = StringUtil.apostrophe(sdf.format(new Date()));
					
					PortalLocalServiceUtil.executeUpdateQuery(String.format(IterRegisterQueries.UPDATE_USER_EXPIRES, null, registerDate, "updateprofiledate", usrid));
				}
				
				UserUtil.doLoginAndRedirectToReferer(request, response, usrname, pwd);
			}
			else
			{
				UserUtil.addUserIdDogtagCookie(request, usrid, userexpires, IterKeys.EDIT_PROFILE_INTENT_REGISTRY);
				UserUtil.redirectToEditProfilePage(groupId, request, response, false);
			}
		}
	}
	
	public String getUserCredentials(long groupid, String data, boolean isnamecheked, boolean ispwdcheked, String refererurl, HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		String retVal = "";
		JSONObject jsonResponse = JSONFactoryUtil.createJSONObject();
		Document groupConfigDom = null;
		String errorMsg = "";
		
		try
		{
			groupConfigDom = UserOperationsLocalServiceUtil.getConfigDom(String.valueOf(groupid));
			
			String mode = GroupConfigTools.getGroupConfigFieldFromDB(groupid, GroupConfigConstants.FIELD_REGISTER_CONFIRMATION_MODE);
			boolean isOTP 	= GroupConfigConstants.REGISTER_CONFIRMATION_MODE.otp.toString().equals(mode);
			boolean isEMAIL	= !isOTP;
			
			long delegationId = GroupLocalServiceUtil.getGroup(groupid).getDelegationId();
			Document usrDom = isEMAIL ? UserUtil.getUser(data, delegationId) : UserUtil.getUserByPhone(data, delegationId);
			
			String usrId = XMLHelper.getTextValueOf(usrDom, "/rs/row/@usrid");
			if(Validator.isNull(usrId))
			{
				errorMsg = UserOperationsLocalServiceUtil.getConfigValue(groupConfigDom, isEMAIL ? 	UserUtil.EMAIL_NOT_EXISTS_MSG : 
																									UserUtil.PHONE_NOT_EXISTS_MSG);
				throw new Exception();
			}
			
			if (isOTP)
			{
				// Envío del mensaje por el servicio OTP
				String phone = IterUserTools.decryptGDPR( XMLHelper.getTextValueOf(usrDom, "/rs/row/@telephone") );
				sendResetCredentialsPhone(groupid, usrId, usrDom, groupConfigDom, request, phone, isnamecheked, ispwdcheked, refererurl);
			}
			else
			{
				String userExpires = XMLHelper.getTextValueOf(usrDom, "/rs/row/@userexpires");
				String email = IterUserTools.decryptGDPR( XMLHelper.getTextValueOf(usrDom, "/rs/row/@email") );
				
				if ( Validator.isNull(userExpires) )
				{
					//El usuario esta confirmado, enviar email de recuerdo de contraseña.
					sendResetCredentialsMail(groupid, usrId, usrDom, groupConfigDom, request, email, isnamecheked, ispwdcheked, refererurl);
				}
				else
				{
					//El usuario es temporal, enviar email de confirmacion de registro.
					_sendConfirmMail(usrDom, groupConfigDom, request, email);
				}
			}
			
			String forgetsuccessHTML = UserOperationsLocalServiceUtil.getConfigValue(groupConfigDom, UserUtil.FORGET_SUCCESS_HTML);
			forgetsuccessHTML 		 = ContextVariables.replaceCtxVars(forgetsuccessHTML, getUserVars(usrDom));
			
			jsonResponse.put( "email", forgetsuccessHTML );
			
			retVal = jsonResponse.toString();
		}
		catch (Exception th)
		{
			_log.error(th);
			
			if (th instanceof ServiceError && ((ServiceError)th).getErrorCode().equals(IterErrorKeys.XYZ_E_OTP_SENDMSG_HAS_FAILED_ZYX))
			{
				errorMsg = GetterUtil.getString2(UserOperationsLocalServiceUtil.getConfigValue(groupConfigDom, UserUtil.OTP_SENDMSG_HAS_FAILED_MSG), 
							IterErrorKeys.XYZ_E_OTP_SENDMSG_HAS_FAILED_ZYX);
			}
			
			if (Validator.isNull(errorMsg))
				errorMsg = UserOperationsLocalServiceUtil.getConfigValue(groupConfigDom, UserUtil.FORGET_ALERT_UNEXPECTED_MSG);
			
			IterMonitor.logEvent(groupid, IterMonitor.Event.ERROR, new java.util.Date(), errorMsg, null, th);
			throw new Exception( errorMsg );
		}
		
		return retVal;
	}
	
	public void deleteExpiredUsers() throws ServiceError, IOException, SQLException
	{
		Date currentDate = Calendar.getInstance().getTime();
		SimpleDateFormat format = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss);
		String currentDateString = format.format(currentDate);
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(IterRegisterQueries.DELETE_USERS_EXPIRED, currentDateString));
	}
	
	/**
	 * @return 	Long.MIN_VALUE si no hay usuarios pendientes de purgar, en caso contrario, cuánto queda para refrescar el siguiente usuario.
	 *  		Podría ser negativo si el próximo usuario ya debería de haber refrescado
	 */
	public long getDelayToDeleteExpiredUsers() throws SecurityException, NoSuchMethodException, ParseException, ServiceError
	{
		long delay = Long.MIN_VALUE;
		
		Document userexpiresDoc = PortalLocalServiceUtil.executeQueryAsDom(String.format(IterRegisterQueries.USER_MIN_EXPIRES));
		String userexpires = XMLHelper.getTextValueOf(userexpiresDoc, "rs/row/@userexpires");

		if(Validator.isNotNull(userexpires))
		{
			SimpleDateFormat format = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss);
			Date minUserexpiresDate = format.parse(userexpires);
			
			delay = minUserexpiresDate.getTime() - Calendar.getInstance().getTimeInMillis();
		}
		
		return delay;
	}
	
	public void initOrUpdateUserToDeleteTask() throws ParseException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		long delay = getDelayToDeleteExpiredUsers();
		
		if (delay != Long.MIN_VALUE)
		{
			// Es importante que NO sea negativo para que no falle el Sleep
			delay = Math.max(0, delay);
			
			// Es necesario purgar al menos un usuario, inmediatamente o a futuro
			UserToDeleteTask userToDelete = getUserToDeleteTask();
			
			if (userToDelete == null)
			{
				synchronized(usersToDelete)
				{
					userToDelete = new UserToDeleteTask(SQLQueries.getUUID(), delay, usersToDelete);
				}
				userToDelete.start();
			}
			else
			{
				userToDelete.reconfigDelay(delay);
			}
		}
	}
	
	private UserToDeleteTask getUserToDeleteTask()
	{
		UserToDeleteTask result = null;
		synchronized(usersToDelete)
		{
			UserToDeleteTask[] usersToDeleteArray = new UserToDeleteTask[usersToDelete.activeCount()];
			usersToDelete.enumerate(usersToDeleteArray);
			if(usersToDeleteArray != null && usersToDeleteArray.length > 0)
				result = usersToDeleteArray[0];
		}
		
		return result;
	}
	
	private void addUser(Map<String, String> iterUsersFields, UserProfileValuesMgr profileValues) throws ServiceError, IOException, SQLException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
//		String query = String.format(IterRegisterQueries.INSERT_USER, usrid, userName, password, 
//				aboid, entitlements, aboinfoexpires, UserUtil.cleanEmail(email), firstname, lastname, 
//				secondlastname, avatarurl, userexpiresEnclosed, registerDate,
//				disqusid, facebookid, googleplusid, twitterid,
//				avatarid, delegationId);
		try
		{
			StringBuilder fields = new StringBuilder("INSERT INTO iterusers (");
			
			StringBuilder values = new StringBuilder(")\n VALUES (");
			
			for (Map.Entry<String, String> entry : iterUsersFields.entrySet()) 
			{
			    String columnName  = entry.getKey();
			    String columnValue = entry.getValue();
			    
			    fields.append(columnName).append(",");
			    
			    if (Validator.isNotNull(columnValue) && UserUtil.PRF_ENCRYPTABLE_FIELDS.contains(columnName))
			    	columnValue = IterUserTools.encryptGDPR_Quoted(columnValue);
			    
			    values.append(columnValue).append(",");
			}
			
			int length = fields.length();
			fields.delete(length-1, length);
			
			length = values.length();
			values.delete(length-1, length);
			String query = fields.append(values.append(")")).toString();
			
			_log.debug(query);
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		catch (ORMException e)
		{
			String eCode = ServiceErrorUtil.getErrorCode(e);
			
			ErrorRaiser.throwIfFalse( !eCode.equalsIgnoreCase(IterErrorKeys.XYZ_ITR_UNQ_USER_NAME_ZYX),	IterErrorKeys.XYZ_ITR_UNQ_USER_NAME_ZYX );
			ErrorRaiser.throwIfFalse( !eCode.equalsIgnoreCase(IterErrorKeys.XYZ_ITR_UNQ_EMAIL_ZYX),		IterErrorKeys.XYZ_ITR_UNQ_EMAIL_ZYX);
			ErrorRaiser.throwIfFalse( !eCode.equalsIgnoreCase(IterErrorKeys.XYZ_ITR_UNQ_PHONE_ZYX),		IterErrorKeys.XYZ_ITR_UNQ_PHONE_ZYX);
			ErrorRaiser.throwIfFalse( !eCode.equalsIgnoreCase(IterErrorKeys.XYZ_ITR_UNQ_ABO_ID_ZYX),	IterErrorKeys.XYZ_ITR_UNQ_ABO_ID_ZYX);
			
			throw e;
		}

		// Se añade el valor de los campos del userprofilevalues
		profileValues.doInsert( StringUtil.unquote(iterUsersFields.get(UserUtil.PRF_FIELD_USRID)) );
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
	public Document editUserProfileFormHandler(HttpServletRequest request, long groupId, List<Node> profileFieldsList, Document userRegisterFormDOM, Map<String, ArrayList> adjuntos) throws Throwable
	{
		boolean aboidRequired = false;
		Map<String, String> cookieMap = UserUtil.getCookie4EditUserProfile(request);
		List<String> profileFieldIdsWithValue = new ArrayList<String>();

		String userId = cookieMap.get(IterKeys.COOKIE_NAME_USER_ID);
		
		checkOTPCode(groupId, userRegisterFormDOM, userId);
		
		
		String fields = request.getParameter(IterKeys.ALL_FIELDS);
		// Se borra el valor (UserProfileValues) de todos inputCtrls con options que estén definidos en el formulario pintado
		// Solo los campos pintados, NO todo el formulario
		String formQuery = (fields != null) ? String.format(" AND formfield.fieldid IN ('%s') ", fields.replaceAll(",", "','")) : "";
		PortalLocalServiceUtil.executeUpdateQuery( String.format(IterRegisterQueries.DELETE_OPTIONS_USRPROFILE_VALUES, userId, formQuery) );
		
		Map<String, String> iterUsersFields 	= new HashMap<String, String>();
		UserProfileValuesMgr profileValuesMgr 	= new UserProfileValuesMgr();
		// Para cada campo relleno 
		
		//Para cada campo que forma parte de la definición del formulario recuperamos su valor del formulario enviado por el usuario.
		//Si es un campo de tipo system, se almacena el nombre del campo y el valor en iterUsersFields. Si es un campo para guardar
		//	en iterusers no será repetible y no podrá tener mas de un valor.
		//Si es un campo de tipo user, se almacena el nombre del campo y sus valores en userProfileFields.
		for(Node profileField : profileFieldsList)
		{
			String fieldid 		= profileField.selectSingleNode("@fieldid").getText();
			String fieldClass 	= profileField.selectSingleNode("@fieldclass").getText();
			
			String xpath 		= String.format("/formdata/fieldsgroup/field[@id='%s']", fieldid);
			String fieldType 	= userRegisterFormDOM.selectSingleNode(xpath.concat("/@fieldtype")).getText();				

			if (fieldClass.equalsIgnoreCase(IterKeys.FIELD_CLASS_SYSTEM))
			{
				String fieldName  = profileField.selectSingleNode("@fieldname").getText();
				String fieldValue = "";
				if (fieldName.equals(UserUtil.PRF_FIELD_AVATARURL))
				{
					Node fieldNode = userRegisterFormDOM.selectSingleNode( xpath );
					DLFileEntry dlfileEntry = addAttachment(groupId, fieldNode, adjuntos);
					
					iterUsersFields.put( UserUtil.PRF_FIELD_AVATARURL, 	DLFileEntryMgrLocalServiceUtil.getDLFileEntryURL(groupId, dlfileEntry.getFolderId(), dlfileEntry.getTitle()) );
					iterUsersFields.put( UserUtil.PRF_FIELD_AVATARID, 	String.valueOf(dlfileEntry.getFileEntryId()) );
				}
				else
				{
					fieldValue = userRegisterFormDOM.selectSingleNode( xpath.concat("/data/value") ).getText();
					
					if ( fieldName.equals(UserUtil.PRF_FIELD_USRNAME) )
						ErrorRaiser.throwIfFalse( !Validator.isEmailAddress(fieldValue), IterErrorKeys.XYZ_ITR_UNQ_USER_NAME_ZYX );
					
					if ( !fieldName.equals(UserUtil.PRF_FIELD_USRPWD) )
						fieldValue = StringEscapeUtils.escapeSql( fieldValue );
					
					// Si es de tipo fecha, se recupera su validador para adaptar el formato a MySQL
					if (Validator.isNotNull(fieldValue) && "date".equals(fieldType))
					{
						Document validator = PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_FORM_DATE_FORMAT, fieldid));
						String format = XMLHelper.getStringValueOf(validator, "/rs/row/@format");
						if (Validator.isNotNull(format))
						{
							DateFormat formDf = new SimpleDateFormat(format);
							fieldValue = DateUtil.getDBFormat(formDf.parse(fieldValue));
						}
					}
					
					iterUsersFields.put( fieldName, fieldValue );
					
					if ( fieldName.equals(UserUtil.PRF_FIELD_ABOID) )
						aboidRequired = GetterUtil.getBoolean(XMLHelper.getTextValueOf(profileField, "@required"));
				}
			}
			else if (!fieldType.equalsIgnoreCase(IterKeys.FIELD_TYPE_BINARY))
			{
				String profileFieldId = profileField.selectSingleNode("@profilefieldid").getText();
				profileValuesMgr.add2Delete(profileFieldId);
				profileFieldIdsWithValue.add(profileFieldId);
				
				List<Node> regFormFields = userRegisterFormDOM.selectNodes( xpath.concat("/data/value") );
				for (Node field : regFormFields)
				{
					String fieldVale = StringEscapeUtils.escapeSql( field.getText() );
					
					profileValuesMgr.add2Insert(profileFieldId, fieldVale, null);
				}
			}
		}
		
		Document xtraData 	= SAXReaderUtil.read("<rs/>");
		Element rootEl 		= xtraData.getRootElement();
		rootEl.addAttribute( IterKeys.XTRADATA_EDIT_MODE, 	cookieMap.get(IterKeys.COOKIE_NAME_INTENT));
		rootEl.addAttribute( IterKeys.XTRADATA_USERID, 		userId);
		rootEl.addAttribute( IterKeys.XTRADATA_EMAIL, 		StringUtil.unquote(iterUsersFields.get(UserUtil.PRF_FIELD_USREMAIL)) );
		
		if ( IterKeys.EDIT_PROFILE_INTENT_FORGOT.equalsIgnoreCase(cookieMap.get(IterKeys.COOKIE_NAME_INTENT)) )
			rootEl.addAttribute( IterKeys.XTRADATA_REFERER, JSONFactoryUtil.createJSONObject( cookieMap.get(IterKeys.COOKIE_NAME_EXTRADATA) ).getString(UserKeys.KEY_REFERER) );

		// Se actualiza el IterUser
		updateUser(userId, groupId, iterUsersFields, aboidRequired, rootEl);
				
		// Se actualiza el UserProfileValues
		profileValuesMgr.doDelete(userId);
		profileValuesMgr.doInsert(userId);
		
		return xtraData;
	}
	
	private void checkOTPCode(long groupId, Document userRegisterFormDOM, String userId) throws Throwable
	{
		try
		{
			String mode = GroupConfigTools.getGroupConfigFieldFromDB(groupId, GroupConfigConstants.FIELD_REGISTER_CONFIRMATION_MODE);
			if (GroupConfigConstants.REGISTER_CONFIRMATION_MODE.otp.toString().equals(mode))
			{
				Document dom = PortalLocalServiceUtil.executeQueryAsDom( String.format(IterRegisterQueries.GET_REGISTRY_OTP_FIELDS, groupId) );
				
				// Está configurado el teléfono en el formulario
				String phoneFieldId = XMLHelper.getTextValueOf(dom, String.format("/rs/row[@fieldname='%s']/@fieldid", UserUtil.PRF_FIELD_TELEPHONE));
				ErrorRaiser.throwIfNull(phoneFieldId, IterErrorKeys.XYZ_ITR_E_PHONE_FIELD_NOT_CONFIGURED_ZYX);
	
				// Está configurado el código OTP en el formulario
				String otpCodeFieldId = XMLHelper.getTextValueOf(dom, String.format("/rs/row[@fieldname='%s']/@fieldid", UserUtil.PRF_FIELD_OTP_CODE));
				ErrorRaiser.throwIfNull(otpCodeFieldId, IterErrorKeys.XYZ_ITR_E_OTPCODE_FIELD_NOT_CONFIGURED_ZYX);
				
				// Se ha introducido un valor de teléfono en el formulario
				String phoneValue = XMLHelper.getTextValueOf(userRegisterFormDOM, String.format("/formdata/fieldsgroup/field[@id='%s']/data/value", phoneFieldId));
				ErrorRaiser.throwIfFalse(Validator.isNotNull(phoneValue), IterErrorKeys.XYZ_ITR_E_PHONE_FIELD_EMPTY_ZYX);
				
				// Si llega userId, es una modificación. Se valida el OTP sólo si se ha modificado el teléfono
				boolean skipValidation = false;
				if (Validator.isNotNull(userId))
				{
					// Recupera el teléfono actual del usuario
					Document phonedom = PortalLocalServiceUtil.executeQueryAsDom(String.format("SELECT telephone FROM iterusers WHERE usrid='%s'", userId));
					String currentPhoneValue = XMLHelper.getTextValueOf(phonedom, "/rs/row/@telephone");
					skipValidation = phoneValue.equals(currentPhoneValue);
				}
				
				if (!skipValidation)
				{
					// Se ha introducido un valor de OTPCode en el formulario
					String otpCodeValue = XMLHelper.getTextValueOf(userRegisterFormDOM, String.format("/formdata/fieldsgroup/field[@id='%s']/data/value", otpCodeFieldId));
					ErrorRaiser.throwIfFalse(Validator.isNotNull(otpCodeValue), IterErrorKeys.XYZ_ITR_E_OTPCODE_FIELD_EMPTY_ZYX);
		
					// Se llama al servicio externo para realizar la validación
					OTPMgr.doValidation(groupId, String.format("otp_phone=%s&otp_code=%s", HttpUtil.encodeURL(phoneValue), HttpUtil.encodeURL(otpCodeValue)));
				}
			}
		}
		catch (Throwable th)
		{
			if (th instanceof ServiceError && ((ServiceError)th).getErrorCode().equals(IterErrorKeys.XYZ_E_OTP_VALIDATION_HAS_FAILED_ZYX))
			{
				// Si es XYZ_E_OTP_VALIDATION_HAS_FAILED_ZYX se relanza porque es el que se traducirá por el 
				// mensaje configurado por el administrador cuando se produce un error durante la validación
				throw th;
			}
			else
			{
				// Si NO es XYZ_E_OTP_VALIDATION_HAS_FAILED_ZYX se traza el error y se lanza XYZ_E_OTP_VALIDATION_HAS_FAILED_ZYX porque es 
				// el que se traducirá por el mensaje configurado por el administrador cuando se produce un error durante la validación.
				_log.error(th);
				
				String msg = GetterUtil.getString2( (th instanceof ServiceError) ? ((ServiceError)th).toString() : th.getMessage(), th.toString() );
				
				IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new java.util.Date(), msg, null, th);
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_OTP_VALIDATION_HAS_FAILED_ZYX, msg);
			}
		}
	}
	
	/**
	 * @param request
	 * @param groupId
	 * @param profileFieldsList Lista de campos que definen el formulario de registro
	 * @param formActions Mensajes de error, de ok y acciones posteriores a realizar
	 * @param userRegisterFormDOM Formulario completado por el usuario
	 * @param adjuntos Ficheros adjuntos al formulario
	 * @throws Throwable 
	 */
	@SuppressWarnings("rawtypes")
	public Document registerFormHandler(HttpServletRequest request, long groupId, List<Node> profileFieldsList, Document userRegisterFormDOM, Map<String, ArrayList> adjuntos) throws Throwable
	{
		Map<String, String> iterUsersFields 	= new HashMap<String, String>();
		UserProfileValuesMgr userProfileFields 	= new UserProfileValuesMgr();
		List<String> profileFieldIdsWithValue 	= new ArrayList<String>();
		boolean aboidRequired = false;
		
		checkOTPCode(groupId, userRegisterFormDOM, null);
		
		//Para cada campo que forma parte de la definición del formulario recuperamos su valor del formulario enviado por el usuario.
		//Si es un campo de tipo system, se almacena el nombre del campo y el valor en iterUsersFields. Si es un campo para guardar
		//	en iterusers no será repetible y no podrá tener mas de un valor.
		//Si es un campo de tipo user, y está vacío, no se almacena el nombre del campo y sus valores en userProfileFields.
		for(Node profileField : profileFieldsList)
		{
			String fieldid = profileField.selectSingleNode("@fieldid").getText();
			String fieldClass = profileField.selectSingleNode("@fieldclass").getText();
			
			String xpath = String.format("/formdata/fieldsgroup/field[@id='%s']", fieldid);
			String fieldType = userRegisterFormDOM.selectSingleNode(xpath+"/@fieldtype").getText();				
			
			if (fieldClass.equalsIgnoreCase(IterKeys.FIELD_CLASS_SYSTEM))
			{
				String fieldName = profileField.selectSingleNode("@fieldname").getText();
				String fieldValue = "";
				if(fieldType.equalsIgnoreCase(IterKeys.FIELD_TYPE_BINARY))
				{
					Node fieldNode = userRegisterFormDOM.selectSingleNode( xpath );
					DLFileEntry dlfileEntry = addAttachment(groupId, fieldNode, adjuntos);
					
					iterUsersFields.put( UserUtil.PRF_FIELD_AVATARURL, StringUtil.apostrophe( DLFileEntryMgrLocalServiceUtil.getDLFileEntryURL(groupId, dlfileEntry.getFolderId(), dlfileEntry.getTitle()) ) );
					iterUsersFields.put( UserUtil.PRF_FIELD_AVATARID,  String.valueOf(dlfileEntry.getFileEntryId()) );
				}
				else
				{
					fieldValue = userRegisterFormDOM.selectSingleNode( xpath+"/data/value" ).getText();
					
					if ( fieldName.equals(UserUtil.PRF_FIELD_USRNAME) )
					{
						ErrorRaiser.throwIfFalse( !Validator.isEmailAddress(fieldValue), IterErrorKeys.XYZ_ITR_UNQ_USER_NAME_ZYX );
						fieldValue = StringUtil.apostrophe(StringEscapeUtils.escapeSql( fieldValue ));
					}
					else if( !fieldName.equals(UserUtil.PRF_FIELD_USRPWD) )
						fieldValue = StringEscapeUtils.escapeSql( fieldValue );
					
					// Si es de tipo fecha, se recupera su validador para adaptar el formato a MySQL
					if (Validator.isNotNull(fieldValue) && IterKeys.FIELD_TYPE_DATE.equals(fieldType))
					{
						Document validator = PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_FORM_DATE_FORMAT, fieldid));
						String format = XMLHelper.getStringValueOf(validator, "/rs/row/@format");
						if (Validator.isNotNull(format))
						{
							DateFormat formDf = new SimpleDateFormat(format);
							fieldValue = DateUtil.getDBFormat(formDf.parse(fieldValue));
						}
					}
					
					if( !fieldName.equals(UserUtil.PRF_FIELD_USRNAME) && !fieldName.equals(UserUtil.PRF_FIELD_USRPWD) &&  
						!fieldName.equals(UserUtil.PRF_FIELD_ABOID)   && !fieldName.equals(UserUtil.PRF_FIELD_EXTRAVALIDATOR))
						fieldValue = GetterUtil.getString2( StringUtil.apostrophe( fieldValue ), StringPool.NULL );
					
					iterUsersFields.put( fieldName, fieldValue );
					
					if( fieldName.equals(UserUtil.PRF_FIELD_ABOID) )
						aboidRequired = GetterUtil.getBoolean(XMLHelper.getTextValueOf(profileField, "@required"));
				}
			}
			else
			{
				List<Node> regFormFields = null;
				String profileFieldId = profileField.selectSingleNode("@profilefieldid").getText();
				profileFieldIdsWithValue.add(profileFieldId);
				
				if(fieldType.equalsIgnoreCase(IterKeys.FIELD_TYPE_BINARY))
					regFormFields = userRegisterFormDOM.selectNodes( xpath );
				else
					regFormFields = userRegisterFormDOM.selectNodes( xpath+"/data/value" );
				
				for(Node field : regFormFields)
				{
					if(fieldType.equalsIgnoreCase(IterKeys.FIELD_TYPE_BINARY))
					{
						DLFileEntry dlfileEntry = addAttachment(groupId, field, adjuntos);
						if( Validator.isNotNull(dlfileEntry) )
							userProfileFields.add2Insert(profileFieldId, dlfileEntry.getDescription(), String.valueOf(dlfileEntry.getFileEntryId()));
					}
					else
					{
						if (Validator.isNotNull(field.getText()))
						{
							String profileFieldValue = StringEscapeUtils.escapeSql( field.getText() );
							userProfileFields.add2Insert(profileFieldId, profileFieldValue, StringPool.NULL);
						}
					}
				}
			}
		}
		
		// Devolver un xml con los datos necesarios para enviar el correo de confirmación al usuario.
		Document xtraData 	= SAXReaderUtil.read("<rs/>");
		Element rootEl 		= xtraData.getRootElement();
		rootEl.addAttribute( IterKeys.XTRADATA_EMAIL, StringUtil.unquote(iterUsersFields.get(UserUtil.PRF_FIELD_USREMAIL)) );
		
		// Registrar al usuario
		registerUser(groupId, request, iterUsersFields, userProfileFields, aboidRequired, rootEl);
		
		return xtraData;
	}
	
	public String registerAnonymousUser(String email, Group group) throws NoSuchAlgorithmException, UnsupportedEncodingException, ServiceError, IOException, SecurityException, NoSuchMethodException, SQLException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		UserProfileValuesMgr profileValues = new UserProfileValuesMgr();
		Map<String, String> iterUsersFields= new HashMap<String, String>();
		
		// Campos fijos
		String usrid 	= SQLQueries.getUUID();
		
		// El password será el usrid
		String pwd 		= IterUserTools.encryptPwd(usrid);
		
		// El usrname será el correo electrónico
		String usrname  = IterUserTools.getUniqueUserName(email);
		
		// Se crea la fecha de expiración
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, PropsValues.ITER_ANONUSER_EXPIRES);
		String expiresDate = DateUtil.getDBFormat( cal.getTime() );
		
		iterUsersFields.put(UserUtil.PRF_FIELD_USRID, 		 	StringUtil.apostrophe(usrid));
		iterUsersFields.put(UserUtil.PRF_FIELD_USRNAME,		 	StringUtil.apostrophe(usrname));
		iterUsersFields.put(UserUtil.PRF_FIELD_USRPWD, 		 	StringUtil.apostrophe(pwd));
		iterUsersFields.put(UserUtil.PRF_FIELD_USREMAIL,	 	StringUtil.apostrophe(UserUtil.cleanEmail(email)));
		iterUsersFields.put(UserUtil.PRF_FIELD_USEREXPIRES,  	StringUtil.apostrophe(expiresDate));
		iterUsersFields.put(UserUtil.PRF_FIELD_DELEGATIONID, 	String.valueOf(group.getDelegationId()));
		iterUsersFields.put(UserUtil.PRF_FIELD_LEVEL, 		 	StringUtil.apostrophe(UserConstants.USER_LEVEL_ANONYMOUS));
		
		addUser(iterUsersFields, profileValues);
		
		// Se emite el HIT como u nuevo usuario registrado
		UserMetricsUtil.Hit(group.getGroupId(), UserMetricsUtil.Hit.REGISTRATION);

		return usrid;
	}

	/**
	 * 
	 * @param groupId
	 * @param request
	 * @param iterUsersFields
	 * @param userProfileFields
	 * @param formActions
	 * @throws Exception 
		 * @throws AddressException
	 * @throws MessagingException
	 */
	private void updateUser(String userId, long groupId, Map<String, String> iterUsersFields, boolean aboidRequired, Element xtraData) throws Exception
	{
		long oldAvatarId			= 0;
		boolean changedMail 		= false;
		boolean changedCookieParams = false;
		String usrExpires 			= StringPool.NULL;
		
		// Se obtienen los parámetros actuales de IterUser para comprobar si han modificado los valores
		Document dom = PortalLocalServiceUtil.executeQueryAsDom( String.format(UserUtil.GET_ITERUSER_CURRENT_FIELDS, userId));
				
		StringBuilder query = new StringBuilder( String.format("UPDATE iterusers SET updateprofiledate='%s' ", DateUtil.getDBFormat(new Date())) );
		for (String key : iterUsersFields.keySet())
		{
			String value = GetterUtil.getString(iterUsersFields.get(key), "");
			String xpath = String.format("/rs/row/@%s", key);
			String addCol= String.format("\n ,%s = '%%s'", key);
			
			// Recupera el valor actual del campo
			String currentValue = IterUserTools.decryptGDPR( XMLHelper.getTextValueOf(dom, xpath, "") );
			// Si es la fecha de nacimiento, elimina los milisegundos
			if (key.equals(UserUtil.PRF_FIELD_BIRTHDAY) && Validator.isNotNull(currentValue))
				currentValue = currentValue.substring(0, currentValue.length() - 2);
			
			// Comprueba si es un campo incluido en la cookie y ha cambiado el valor
			if (UserUtil.PRF_COOKIE_FIELDS.contains(key) && !currentValue.equals(value))
			{
				// Si es el ABOID, tiene que tener un valor, ya que en caso contrario no se va a actualizar (Es un campo que no se vacía)
				// OJO! El código de suscriptor no tiene por que ser igual que la cookie, por lo que es posible que esta se regenere aunque no haya cambios.
				if (key.equals(UserUtil.PRF_FIELD_ABOID))
					changedCookieParams = Validator.isNotNull(value) && !currentValue.equals(value);
				else
					changedCookieParams = true;
			}
			
			// Si es la contraseña...
			if (key.equals(UserUtil.PRF_FIELD_USRPWD))
			{
				// Si viene vacía, no se actualiza
				if (Validator.isNotNull(value))
				{
					// Aplica el MD5
					value = IterUserTools.encryptPwd(value);
					// Si se ha cambiado, la actualiza
					if (!currentValue.equals(value))
						query.append( String.format(addCol, value) );
				}
			}
			// Si es el código de suscriptor
			else if (key.equals(UserUtil.PRF_FIELD_ABOID))
			{
				// Si viene vacía, no se actualiza
				if (Validator.isNotNull(value))
				{
					value = StringUtil.unquote(authenticateUser(groupId, currentValue, aboidRequired, iterUsersFields.get(UserUtil.PRF_FIELD_ABOID), iterUsersFields.get(UserUtil.PRF_FIELD_USREMAIL), iterUsersFields.get(UserUtil.PRF_FIELD_EXTRAVALIDATOR), xtraData));
					
					if ( Validator.isNotNull(value) && !currentValue.equals(value) )
					{
						query.append( String.format(addCol, value) ); 
						changedCookieParams = true;
					}
				}
			}
			// Para el resto de campos...
			else
			{
				// Si ha cambiado el valor...
				if (!currentValue.equals(value))
				{
					if (key.equals(UserUtil.PRF_FIELD_AVATARURL))
					{
						value = IterUserTools.encryptGDPR(value);
						query.append( String.format("\n ,%s = '%s', %s = %s", key, value, 
								UserUtil.PRF_FIELD_AVATARID, GetterUtil.getString(iterUsersFields.get(UserUtil.PRF_FIELD_AVATARID), "")) );
						
						oldAvatarId = XMLHelper.getLongValueOf(dom, String.format("/rs/row/@%s", UserUtil.PRF_FIELD_AVATARID));
					}
					else if (key.equals(UserUtil.PRF_FIELD_USREMAIL))
					{
						value = IterUserTools.encryptGDPR(UserUtil.cleanEmail(value));
						String expires4User = UserUtil.getInfiniteUserExpiresDateFormated(groupId);
						usrExpires = Validator.isNull(expires4User) ? StringPool.NULL : StringUtil.apostrophe( expires4User );
						query.append( String.format("\n ,%s = '%s', userexpires = %s ", key, value, usrExpires) );
						changedMail  = true;
					}
					else  if (!key.equals(UserUtil.PRF_FIELD_AVATARID))
					{
						if (UserUtil.PRF_ENCRYPTABLE_FIELDS.contains(key))
							value = IterUserTools.encryptGDPR(value);
							
						query.append( String.format(addCol, value) );
					}
				}
			}
		}
		
		// Si se ha cambiado el email se marca el usuario como temporal
		if (changedMail)
		{
			changedMail = !usrExpires.equals(StringPool.NULL);
		}
		else if (IterKeys.EDIT_PROFILE_INTENT_REGISTRY.equals( XMLHelper.getTextValueOf(xtraData, String.format("@%S", IterKeys.XTRADATA_EDIT_MODE)) ))
		{
			// Si no ha modificado el email y se ha completado el registro, YA NO será temporal
			query.append("\n ,userexpires = null" );  
		}
		
		PortalLocalServiceUtil.executeUpdateQuery( query.append(String.format("\n 	WHERE usrid='%s'", userId)).toString() );
		UserMetricsUtil.Hit(groupId, UserMetricsUtil.Hit.PROFILE_EDIT);
		
		// Se borra el antiguo DLFileEntry
		if (oldAvatarId > 0)
			PortalLocalServiceUtil.executeUpdateQuery( String.format("DELETE FROM DLFileEntry WHERE dlfileentry.fileEntryId = %d", oldAvatarId) );

		
		xtraData.addAttribute( IterKeys.XTRADATA_CHANGED_EMAIL,			String.valueOf(changedMail) );
		xtraData.addAttribute( IterKeys.XTRADATA_CHANGED_COOKIE_FIELDS,	String.valueOf(changedCookieParams) );
	}
	
	private void registerUser(long groupId, HttpServletRequest request, Map<String, String> iterUsersFields, UserProfileValuesMgr userProfileFields, boolean aboidRequired, Element xtraData) throws Exception
	{
		// id del usuario que se va a añadir.
		String usrid = SQLQueries.getUUID();
		
		String usrExpires = UserUtil.getUserExpiresDateFormated(groupId);
		xtraData.addAttribute( IterKeys.XTRADATA_CHANGED_EMAIL, usrExpires.equals(StringPool.NULL) ? "false" : "true" );
		// Fecha límite en la que el usuario debe confirmar el registro.
		String userexpiresEnclosed = StringUtil.apostrophe( usrExpires );
		
		// Codificación de la contraseña.
		iterUsersFields.put(UserUtil.PRF_FIELD_USRPWD, StringUtil.apostrophe(IterUserTools.encryptPwd(iterUsersFields.get(UserUtil.PRF_FIELD_USRPWD))));
		
		// Autenticación del usuario contra el sistema de suscripciones.
		iterUsersFields.put(UserUtil.PRF_FIELD_ABOID, authenticateUser(	groupId, StringPool.BLANK, aboidRequired, 
																		iterUsersFields.get(UserUtil.PRF_FIELD_ABOID), 
																		StringUtil.unquote(iterUsersFields.get(UserUtil.PRF_FIELD_USREMAIL)), 
																		iterUsersFields.get(UserUtil.PRF_FIELD_EXTRAVALIDATOR), xtraData));
		
		// Recuperación del identificador de Delegación
		long delegationId = GroupLocalServiceUtil.getGroup(groupId).getDelegationId();
		
		// Fecha de registro en caso de no requerir confirmación de email
		String registerDate = StringPool.NULL;
		if (usrExpires.equals(StringPool.NULL))
		{
			SimpleDateFormat sdf = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss);
			registerDate = StringUtil.apostrophe(sdf.format(new Date()));
		}
		
		iterUsersFields.put(UserUtil.PRF_FIELD_USRID, 		 StringUtil.apostrophe(usrid));
		iterUsersFields.put(UserUtil.PRF_FIELD_USEREXPIRES,  userexpiresEnclosed);
		iterUsersFields.put(UserUtil.PRF_FIELD_DELEGATIONID, String.valueOf(delegationId));
		iterUsersFields.put(UserUtil.PRF_FIELD_REGISTERDATE, registerDate);
		
		// Registro del usuario en el sistema
		addUser(iterUsersFields, userProfileFields);
		
		// Añade el Id del usuario a la salida, por si hubiera que suscribirlo a newsletters
		xtraData.addAttribute(IterKeys.XTRADATA_USERID, usrid);
		
		// Si no hay configurada expiración de usuario, se anota el registro.
		// (Si la hay, se anotará en la validación del enlace de confirmación de correo)
		if (usrExpires.equals(StringPool.NULL))
			UserMetricsUtil.Hit(groupId, UserMetricsUtil.Hit.REGISTRATION);
	}
	
	private String authenticateUser(long groupId, String userAboId, boolean aboidRequired, String aboCode, String usrEmail, String extraValidator, Element xtraData) throws ServiceError, DocumentException, SecurityException, NoSuchMethodException, PortalException, SystemException
	{
		//Si no existe el sistema de autenticación el aboId es vacio, asi que se asigna null para el INSERT posterior
		String aboId = StringPool.NULL;
		String token = "";
		String message = "";
		boolean authenticateByMail = false;
		boolean ifAuthenticationFailedContinue = true;
		JSONObject authenticationInfo = null;
		boolean authenticateUser = false;
		
		ErrorRaiser.throwIfFalse(Validator.isNotNull(usrEmail), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String friendlyGroupURL = GroupLocalServiceUtil.getGroup(groupId).getFriendlyURL().replace(StringPool.SLASH, StringPool.PERIOD);

		token = Validator.isNotNull(userAboId) ? userAboId : usrEmail;
		
		if( Validator.isNotNull(aboCode) )
		{
			extraValidator = aboCode;
			authenticateUser = true;
		}
		else
		{
			authenticateByMail = GetterUtil.getBoolean( PropsUtil.get(String.format(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SUBSCRIPTION_AUTHENTICATION_ON_REGISTER, friendlyGroupURL)), false );
			if( authenticateByMail )
			{
				authenticateUser = true;
				ifAuthenticationFailedContinue = GetterUtil.getBoolean(PropsUtil.get(String.format(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SUBSCRIPTION_AUTHENTICATION_ON_FAILED_CONTINUE, friendlyGroupURL)), true);
			}
		}

		if( authenticateUser )
		{
			try
			{
				authenticationInfo = AuthenticateUser.doAuthentication(friendlyGroupURL, token, extraValidator, StringPool.BLANK);
			}
			catch (ServiceError se)
			{
				if( aboidRequired || !ifAuthenticationFailedContinue )
					throw se;
				else
				{
					String eCode = se.getErrorCode();
					
					if(eCode.equals(IterErrorKeys.XYZ_ITR_E_AUTHENTICATE_TOKEN_IS_NULL_ZYX))
						message = IterErrorKeys.XYZ_ITR_E_AUTHENTICATE_TOKEN_IS_NULL_ZYX;
				}
			}
			
			if(Validator.isNotNull(authenticationInfo))
			{
				if(authenticationInfo.getString(UserKeys.SUBSCRIPTION_SYSTEM_RESPONSE_CODE).equalsIgnoreCase(IterKeys.OK))
					aboId = StringUtil.apostrophe( StringEscapeUtils.escapeSql(authenticationInfo.getString(UserKeys.SUSCRIPTOR_ID)) );
				else
				{
					if( aboidRequired || !ifAuthenticationFailedContinue )
						ErrorRaiser.throwIfError(IterErrorKeys.XYZ_ITR_E_AUTHENTICATING_USER_ZYX, authenticationInfo.getString(UserKeys.SUBSCRIPTION_SYSTEM_MSG));
					
					message = GetterUtil.getString2(authenticationInfo.getString(UserKeys.SUBSCRIPTION_SYSTEM_MSG), IterErrorKeys.XYZ_ITR_E_AUTHENTICATING_USER_ZYX);
				}
			}
		}
		
		xtraData.addAttribute( IterKeys.XTRADATA_MSG, message );
		
		return aboId;
	}
	
	@SuppressWarnings("rawtypes")
	private DLFileEntry addAttachment( long groupId, Node field, Map<String, ArrayList> adjuntos ) throws ServiceError, PortalException, SystemException, IOException
	{
		Map<String,	Object> attachment = FormUtil.getAttachment(field, adjuntos);
		String name = (String) attachment.get(FormUtil.KEY_NAME);
		InputStream is = (InputStream) attachment.get(FormUtil.KEY_ATTACH);
		DLFileEntry dlfileEntry = DLFileEntryMgrLocalServiceUtil.addDLFileEntry(groupId, GroupMgr.getDefaultUserId(), IterKeys.FORMS_ATTACHMENTS_FOLDER, name, is);
		
		return dlfileEntry;
	}
	
	public void confirmUserRegister( long groupId, String userId, String userExpires, HttpServletResponse response ) throws SecurityException, NoSuchMethodException, ParseException, IOException, SQLException, ServiceError, DocumentException, PortalException, SystemException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		boolean redirect = false;
		long currentDate = new Date().getTime();
		long expiresTime = Long.valueOf(userExpires);
		
		if( currentDate > expiresTime )
		{
			UserUtil.redirectTo(response, groupId, UserUtil.REGISTER_ERROR_LAYOUT_NAME);
			redirect = true;
		}
		else
		{
			String query = String.format( IterRegisterQueries.GET_USER_BY_USERID, userId );
			Document userDom = PortalLocalServiceUtil.executeQueryAsDom( query );
			if(userDom.selectNodes("//row").size()==1)
			{
				userExpires = XMLHelper.getTextValueOf(userDom, "/rs/row/@userexpires");
				if(Validator.isNotNull(userExpires))
				{
					boolean isAnonymous			= XMLHelper.getTextValueOf(userDom, "/rs/row/@level", "").equals(UserConstants.USER_LEVEL_ANONYMOUS);	
					boolean isMailModification 	= XMLHelper.getTextValueOf(userDom, "/rs/row/@registerdate") != null;
					String registerDate 		= isMailModification ? "registerdate" : StringUtil.apostrophe(DateUtil.getDBFormat(new Date()));
					String updateProfileDate 	= isMailModification ? StringUtil.apostrophe(DateUtil.getDBFormat(new Date())) : "updateprofiledate";
					
					// Recupera el nombre de usuario y la contraseña en md5
					String usrName 	= IterUserTools.decryptGDPR(XMLHelper.getTextValueOf(userDom, "/rs/row/@usrname"));
					String pwd 		= XMLHelper.getTextValueOf(userDom, "/rs/row/@pwd");

					if (!isAnonymous)
						query = String.format( IterRegisterQueries.UPDATE_USER_EXPIRES, StringPool.NULL, registerDate, updateProfileDate, userId );
					else
					{
						// El nombre de usuario NO puede ser un correo electrónico. El sistema no permite nombre de usuarios stándar 
						// con formato de correo electrónico
						usrName = IterUserTools.getUniqueUserName(usrName.split(StringPool.AT)[0]);
						((Element)userDom.getRootElement().selectSingleNode("row")).addAttribute("usrname", usrName);
						query = String.format( 	IterRegisterQueries.UPDATE_ANONUSER_EXPIRES, StringPool.NULL, registerDate, 
												updateProfileDate, IterUserTools.encryptGDPR(usrName), userId );
					}
					PortalLocalServiceUtil.executeUpdateQuery(query);

					// Recupera la URL de destino
					Document d = UserOperationsLocalServiceUtil.getConfigDom(String.valueOf(groupId));
					String redirectUrl = XMLHelper.getTextValueOf(d, "rs/row/" + UserUtil.REGISTER_SUCCESS_LAYOUT_NAME);
					IterRequest.getOriginalRequest().setAttribute(IterKeys.REQUEST_ATTRIBUTE_SSO_DESTINATION_URL, redirectUrl);
					
					// Realiza el login
					String sso = UserUtil.doLogin(IterRequest.getOriginalRequest(), response, usrName, pwd);
					
					// Comprueba si hay redirección para SSO
					if (Validator.isNotNull(sso))
					{
						try
						{
							JSONObject jsonSSO = JSONFactoryUtil.createJSONObject(StringPool.OPEN_CURLY_BRACE + sso + StringPool.CLOSE_CURLY_BRACE);
							if (Validator.isNotNull(jsonSSO.getString("sso")))
							{
								redirectUrl = jsonSSO.getString("sso");
							}
							else if (isAnonymous && 
									 jsonSSO.get("result").equals("OK") && jsonSSO.getJSONObject("furtheraction").get("action").equals("redirect"))
							{
								redirectUrl = jsonSSO.getJSONObject("furtheraction").get("location").toString();
							}
						}
						catch(Throwable th)
						{
							_log.error(th);
						}
					}

					response.sendRedirect(redirectUrl);
					redirect = true;
					
					// Si el usuario tiene fecha de login, se considera una modificación del correo y no hay que anotar el HIT
					if (!isMailModification)
						UserMetricsUtil.Hit(groupId, UserMetricsUtil.Hit.REGISTRATION);
				}
			}
		}
		
		if( !redirect )
			UserUtil.redirectTo(response, groupId, UserUtil.REGISTER_ERROR_LAYOUT_NAME);

	}
	
	public void resetUserCredentials(HttpServletRequest request, HttpServletResponse response, ServletContext context, long groupId, String userId, String userExpires, JSONObject extraData) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, ServiceError, PortalException, SystemException, IOException
	{
		long tokenDate = Long.valueOf(userExpires);
		long currentDate = new Date().getTime();
		
		if( currentDate > tokenDate)
		{
			UserUtil.redirectTo(response, groupId, UserUtil.FOGET_ERROR_LAYOUT_NAME);
		}
		else
		{
			request.setAttribute(IterKeys.REQUEST_ATTRIBUTE_ROOT_CONTEXT, context);
			
			UserUtil.addUserIdDogtagCookie(request, userId, "", IterKeys.EDIT_PROFILE_INTENT_FORGOT, extraData);
			
			UserUtil.redirectToEditProfilePage( groupId, request, response, false);
		}
	}
	
	public void sendConfirmMail(String groupid, String email, HttpServletRequest request) throws SecurityException, ServiceError, NoSuchMethodException, ParseException, PortalException, SystemException, AddressException, MessagingException, IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		ErrorRaiser.throwIfFalse( Validator.isNotNull(email) );
		long delegationId = GroupLocalServiceUtil.getGroup(Long.parseLong(groupid)).getDelegationId();
		
		Document usrDom = UserUtil.getUser(email, delegationId);
		Document groupConfigDom = UserOperationsLocalServiceUtil.getConfigDom(groupid);
		
		_sendConfirmMail(usrDom, groupConfigDom, request, IterUserTools.decryptGDPR(XMLHelper.getTextValueOf(usrDom, "/rs/row/@email")));
	}
	
	private void _sendConfirmMail(Document usrDom, Document groupConfigDom, HttpServletRequest request, String email ) throws ParseException, ServiceError, AddressException, SecurityException, NoSuchMethodException, MessagingException, IOException, PortalException, SystemException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		List<String> mailList = new Vector<String>();
		
		String usrId = XMLHelper.getTextValueOf(usrDom, "/rs/row/@usrid");
		ErrorRaiser.throwIfNull(usrId);
		String userExpires =  XMLHelper.getTextValueOf(usrDom, "/rs/row/@userexpires");
		ErrorRaiser.throwIfNull(userExpires);
		String expires = String.valueOf(DateUtil.getDBFormat().parse(userExpires).getTime());
		
		Map<String, String> userVars = getUserVars(usrDom);
		
		JSONObject extradata = JSONFactoryUtil.createJSONObject();
		
		String token = UserUtil.createUserIdDogtagToken(IterKeys.INTENT_CONFIRM_VERSION, expires, usrId, IterKeys.EDIT_PROFILE_INTENT_CONFIRM, extradata);
		userVars.put( UserUtil.PROCESSLINK, UserUtil.getServletURL(request, SERVLET_CONFIRM_USER, token) );
		//Obtener el cuerpo del mensaje y sustituir las variables incrustadas para personalizar el correo
		String bodyTemplate = UserOperationsLocalServiceUtil.getConfigValue(groupConfigDom, UserUtil.REGISTER_HTML_MAIL);
		bodyTemplate = ContextVariables.replaceCtxVars(bodyTemplate, userVars);
		//Obtener el asunto del correo y sustituir las variables incrustadas para personalizarlo
		String subject = UserOperationsLocalServiceUtil.getConfigValue(groupConfigDom, UserUtil.REGISTER_MAIL_SUBJ);
		subject = ContextVariables.replaceCtxVars(subject, userVars);
		String smtpId = UserOperationsLocalServiceUtil.getConfigValue(groupConfigDom, UserUtil.REGISTER_SMTP);
		
		mailList.add(email);
		UserUtil.sendMail(smtpId, mailList, subject, bodyTemplate, UserUtil.MIMETYPE_HTML_UTF8);
	}
	
	private void sendResetCredentialsPhone(	long groupid, String usrId,Document usrDom, Document groupConfigDom, HttpServletRequest request, 
											String phone, boolean isnamecheked, boolean ispwdcheked, String refererurl ) throws Exception
	{
		try
		{
			Map<String, String> userVars = getUserVars(usrDom);
			String expires = UserUtil.getResetCredentialsExpiresToken(groupid);
			
			JSONObject extradata = JSONFactoryUtil.createJSONObject();
			extradata.put(UserKeys.KEY_FORGOT_USR_NAME, isnamecheked);
			extradata.put(UserKeys.KEY_FORGOT_PWD, ispwdcheked);
			extradata.put(UserKeys.KEY_REFERER, GetterUtil.getString2(refererurl, "/"));
			
			String token = UserUtil.createUserIdDogtagToken(IterKeys.INTENT_FORGOT_VERSION, expires, usrId, IterKeys.EDIT_PROFILE_INTENT_FORGOT, extradata);
			userVars.put( UserUtil.PROCESSLINK, UserUtil.getServletURL(request, SERVLET_FORGOT_PWD, token) );
	
			String subject = UserOperationsLocalServiceUtil.getConfigValue(groupConfigDom, UserUtil.FORGET_MAIL_SUBJ);
			subject = ContextVariables.replaceCtxVars(subject, userVars);
			
			String otp_msg = String.format("%s \n %s", subject, userVars.get(UserUtil.PROCESSLINK));
			
			// Se llama al servicio externo para realizar la validación
			OTPMgr.doSendMsg(groupid, String.format("otp_phone=%s&otp_msg=%s", HttpUtil.encodeURL(phone), HttpUtil.encodeURL(otp_msg)));
		}
		catch (Exception th)
		{
			if (th instanceof ServiceError && ((ServiceError)th).getErrorCode().equals(IterErrorKeys.XYZ_E_OTP_SENDMSG_HAS_FAILED_ZYX))
			{
				// Si es XYZ_E_OTP_VALIDATION_HAS_FAILED_ZYX se relanza porque es el que se traducirá por el 
				// mensaje configurado por el administrador cuando se produce un error durante la validación
				throw th;
			}
			else
			{
				// Si NO es XYZ_E_OTP_VALIDATION_HAS_FAILED_ZYX se traza el error y se lanza XYZ_E_OTP_VALIDATION_HAS_FAILED_ZYX porque es 
				// el que se traducirá por el mensaje configurado por el administrador cuando se produce un error durante la validación.
				_log.error(th);
				
				String msg = GetterUtil.getString2( (th instanceof ServiceError) ? ((ServiceError)th).toString() : th.getMessage(), th.toString() );
				
				IterMonitor.logEvent(groupid, IterMonitor.Event.ERROR, new java.util.Date(), msg, null, th);
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_OTP_SENDMSG_HAS_FAILED_ZYX, msg);
			}
		}
	}
	
	private void sendResetCredentialsMail(long groupid, String usrId,Document usrDom, Document groupConfigDom, HttpServletRequest request, String email, boolean isnamecheked, boolean ispwdcheked, String refererurl ) throws ParseException, ServiceError, AddressException, SecurityException, NoSuchMethodException, MessagingException, IOException, PortalException, SystemException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		List<String> mailList = new Vector<String>();
		
		Map<String, String> userVars = getUserVars(usrDom);
		
		String expires = UserUtil.getResetCredentialsExpiresToken(groupid);
		
		JSONObject extradata = JSONFactoryUtil.createJSONObject();
		extradata.put(UserKeys.KEY_FORGOT_USR_NAME, isnamecheked);
		extradata.put(UserKeys.KEY_FORGOT_PWD, ispwdcheked);
		extradata.put(UserKeys.KEY_REFERER, GetterUtil.getString2(refererurl, "/"));
		
		String token = UserUtil.createUserIdDogtagToken(IterKeys.INTENT_FORGOT_VERSION, expires, usrId, IterKeys.EDIT_PROFILE_INTENT_FORGOT, extradata);
		userVars.put( UserUtil.PROCESSLINK, UserUtil.getServletURL(request, SERVLET_FORGOT_PWD, token) );
		String bodyTemplate = UserOperationsLocalServiceUtil.getConfigValue(groupConfigDom, UserUtil.FORGET_HTML_EMAIL);
		//Sustituir las variables incrustadas para personalizar el correo
		bodyTemplate = ContextVariables.replaceCtxVars(bodyTemplate, userVars);
		//Obtener el asunto del correo y sustituir las variables incrustadas para personalizarlo
		String subject = UserOperationsLocalServiceUtil.getConfigValue(groupConfigDom, UserUtil.FORGET_MAIL_SUBJ);
		subject = ContextVariables.replaceCtxVars(subject, userVars);
		String smtpId = UserOperationsLocalServiceUtil.getConfigValue(groupConfigDom, UserUtil.FORGET_SMTP);
		
		mailList.add(email);
		UserUtil.sendMail(smtpId, mailList, subject, bodyTemplate, UserUtil.MIMETYPE_HTML_UTF8);
	}
	
	private Map<String, String> getUserVars(Document userDom) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		Map<String, String> userVars = new HashMap<String, String>();
		userVars.put(UserUtil.USRNAME, 				IterUserTools.decryptGDPR( XMLHelper.getTextValueOf(userDom, "/rs/row/@usrname", 		StringPool.BLANK)) );
		userVars.put(UserUtil.USREMAIL, 			IterUserTools.decryptGDPR( XMLHelper.getTextValueOf(userDom, "/rs/row/@email", 			StringPool.BLANK)) );
		userVars.put(UserUtil.USR1STNAME, 			IterUserTools.decryptGDPR( XMLHelper.getTextValueOf(userDom, "/rs/row/@firstname", 		StringPool.BLANK)) );
		userVars.put(UserUtil.USRLASTNAME, 			IterUserTools.decryptGDPR( XMLHelper.getTextValueOf(userDom, "/rs/row/@lastname", 		StringPool.BLANK)) );
		userVars.put(UserUtil.USRLASTNAME2, 		IterUserTools.decryptGDPR( XMLHelper.getTextValueOf(userDom, "/rs/row/@secondlastname", StringPool.BLANK)) );
		userVars.put(UserUtil.PRF_FIELD_TELEPHONE, 	IterUserTools.decryptGDPR( XMLHelper.getTextValueOf(userDom, "/rs/row/@telephone", 		StringPool.BLANK)) );
		
		return userVars;
	}
	
	public String unregisterUser(HttpServletRequest request, HttpServletResponse response, String password1, String password2)
	{
		JSONObject result = JSONFactoryUtil.createJSONObject();
		boolean badCredentialsError = false;
		try
		{
			String userId = PayCookieUtil.getUserId(request);

			if(Validator.isNull(userId))
			{
				_log.error("No usrid found");
				badCredentialsError = true;
			}

			if(Validator.isNull(password1) || Validator.isNull(password2))
			{
				_log.error("Password empty");
				badCredentialsError = true;
			}
			else if (!password1.equals(password2))
			{
				_log.error("Different passwords");
				badCredentialsError = true;
			}
			
			if(!badCredentialsError)
			{
				int count = 0;
				if (PropsValues.ITER_USER_PASSWORD_ENCRYPTION_DRUPAL_ENABLED)
				{
					Document docData = PortalLocalServiceUtil.executeQueryAsDom( String.format(IterRegisterQueries.GET_USER_PWD, userId) );
					String encryptedPwd = XMLHelper.getStringValueOf(docData, "rs/row/@pwd");
					// Si tiene codificación Drupal o la configurada por defecto en el sistema 
					if (DrupalEncryption.user_check_password(password1, encryptedPwd) ||
						IterUserTools.encryptPwd(password1).equals(encryptedPwd))
						count = 1;
				}
				else
				{
					Document docData = PortalLocalServiceUtil.executeQueryAsDom(
							String.format(IterRegisterQueries.CHECK_USER_BY_CREDENTIALS, 
									userId, IterUserTools.encryptPwd(password1)));
					
					count = Integer.valueOf(GetterUtil.getString(XMLHelper.getTextValueOf(docData, "rs/row/@result"), "0"));
				}
				
				if (count == 1)
				{
					// Se realiza una copia de la información del usuario antes de ser borrado
					IterUserBackupMgr.backup(userId);

					PortalLocalServiceUtil.executeUpdateQuery(String.format(IterRegisterQueries.DELETE_USER, userId));
					result.put("result", "OK");
					// Anota el hit de borrado en las métricas de usuarios.
					UserMetricsUtil.Hit(PortalUtil.getScopeGroupId(request), UserMetricsUtil.Hit.DELETE);
					// Anota el hit de borrado en las métricas de newsletter.
					NewslettersMetricsUtil.allUsersNewslettersHit(userId, NewslettersMetricsUtil.HIT.USR_CANCEL_ACCOUNT);
					String sso = LoginServiceUtil.doLogout(request, response);
					if (Validator.isNotNull(sso))
					{
						JSONObject jsonSSO = JSONFactoryUtil.createJSONObject(StringPool.OPEN_CURLY_BRACE + sso + StringPool.CLOSE_CURLY_BRACE);
						result.put("sso", jsonSSO.getString("sso"));
					}
				}
				else
				{
					badCredentialsError = true;
				}
			}

			if(badCredentialsError)
			{
				_log.error("Bad credentials");
				result.put("result", "badCredentialsError");
			}
		}
		catch(Exception e)
		{
			_log.error(e);
			
			result.put("result", "KO");
		}
		
		
		return result.toString();
	}
	
	public String refreshUserEntitlements(String friendlyGroupURL, String redirectPage, String errorPage, HttpServletRequest request, HttpServletResponse response, boolean sso ) throws ServiceError, SecurityException, NoSuchMethodException, PortalException, SystemException, ServletException, IOException, DocumentException, ParseException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, SQLException
	{
		boolean doRedirect = false;
		String userId = null;
		
		String hasSuccess = redirectPage.equals( request.getParameter(IterKeys.ACTION_REDIRECT) ) ? "#success=false" : "";
		
		// Recuperar la cookie USER-DOGTAG para obtener el id del usuario conectado.
		Cookie[] requestCookies = CookieUtil.getCookies(request, new String[]{IterKeys.COOKIE_NAME_USER_DOGTAG});
		Cookie dogTagCookie = (Cookie)ArrayUtil.getValue(requestCookies, 0);
		
		if (sso)
		{
			dogTagCookie = new Cookie(IterKeys.COOKIE_NAME_USER_DOGTAG, String.valueOf(request.getAttribute("Set-Cookie-Data")));
			if (Validator.isNotNull(dogTagCookie))
				dogTagCookie.setMaxAge(0);
		}
		
		// Si no hay cookie USER-DOGTAG, el usuario no está presentado en el sistema, se realiza al redirección a la página configurada 
		if( Validator.isNull(dogTagCookie) )
			doRedirect = true;
		else
		{
			Map<String, String> dogTag = PayCookieUtil.getCookieAsMap( PayCookieUtil.plainCookieValue(dogTagCookie.getValue()), IterKeys.COOKIE_NAME_USER_DOGTAG );
			userId = dogTag.get(IterKeys.COOKIE_NAME_USER_ID);
			
			// Si no hay USER_ID en la cookie USER-DOGTAG, el usuario no está presentado en el sistema, se realiza al redirección a la página configurada
			if(Validator.isNull(userId))
				doRedirect = true;
			else
			{
				// Se registra el readerId
				UserUtil.registerReaderId(PortalUtil.getScopeGroupId(request), userId, request.getParameter("rid"));

				String aboid = getUserAboId(userId, friendlyGroupURL, dogTag.get(IterKeys.COOKIE_NAME_USER_EMAIL), redirectPage, request);
				
				if(Validator.isNotNull(aboid))
				{
					String userEntitlements = UserEntitlementsMgr.getEntitlements(friendlyGroupURL, aboid, StringPool.BLANK);
					JSONObject entitlementsInfo = UserEntitlementsMgr.getEntitlementsInfo(userEntitlements, aboid, 0L, false);
					ErrorRaiser.throwIfNull(entitlementsInfo, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
					
					if( entitlementsInfo.getString(UserKeys.SUBSCRIPTION_SYSTEM_RESPONSE_CODE).equalsIgnoreCase(IterKeys.OK) )
					{
						// Si ha ido bien lo indica en la URL de vuelta
						if (!hasSuccess.isEmpty())
							hasSuccess = "#success=true";
							
						// Se refresca la cookie
						dogTagCookie = refreshCookieEntitlements(dogTag, dogTagCookie.getMaxAge()==-1, aboid,
								entitlementsInfo.getString(UserKeys.ENCODED_PRODUCT_LIST),
								Integer.parseInt(entitlementsInfo.getString(UserKeys.EXPIRES)), request, response);
						
						// Si es necesario, se almacenan los nuevos derechos del usuario en BBDD.
						if(HotConfigUtil.getKey(UserKeys.TIMESTAMP_VALUE,1L)>0L)
							UserEntitlementsMgr.cacheEntitlements(aboid, entitlementsInfo.getString(UserKeys.RAW_PRODUCT_LIST), entitlementsInfo.getLong(UserKeys.MIN_VALID_PROD));
						
						// Si el sistema de suscripciones no devuelve un mensaje para el usuario, se redirecciona a la página especificada 
						if( Validator.isNull(entitlementsInfo.getString(UserKeys.SUBSCRIPTION_SYSTEM_MSG)) )
							doRedirect = true;
					}
					
					if( entitlementsInfo.getString(UserKeys.SUBSCRIPTION_SYSTEM_RESPONSE_CODE).equalsIgnoreCase(IterKeys.KO) ||
							entitlementsInfo.getString(UserKeys.SUBSCRIPTION_SYSTEM_RESPONSE_CODE).equalsIgnoreCase(UserKeys.ITER_KO) ||
							Validator.isNotNull(entitlementsInfo.getString(UserKeys.SUBSCRIPTION_SYSTEM_MSG)) )
					{
						String respCod = entitlementsInfo.getString(UserKeys.SUBSCRIPTION_SYSTEM_RESPONSE_CODE);
						if(respCod.equalsIgnoreCase(UserKeys.ITER_KO))
							respCod = IterKeys.KO;
						
						// Añadir las variables como atributos al request
						Map<String, String> subscriptionCtxVars = ContextVariables.getRequestCtxVars(request, IterKeys.SUBSCRIPTION_SYSTEM_CONTEXT_VARS);
						subscriptionCtxVars.put(UserKeys.ITER_SUBSCRIPTION_SYSTEM_CODE, respCod);
						subscriptionCtxVars.put(UserKeys.ITER_SUBSCRIPTION_SYSTEM_MSG, GetterUtil.getString2(entitlementsInfo.getString(UserKeys.SUBSCRIPTION_SYSTEM_MSG), entitlementsInfo.getString(UserKeys.HTTP_STATUS_LINE)));
						subscriptionCtxVars.put(UserKeys.ITER_REDIRECTURL_PARAM, redirectPage);
						subscriptionCtxVars.put(UserKeys.ITER_SUBSCRIPTION_SYSTEM_HTTPSTATUSCODE, GetterUtil.getString2(entitlementsInfo.getString(UserKeys.HTTP_STATUS_CODE), String.valueOf(HttpStatus.SC_OK)) );
						subscriptionCtxVars.put(UserKeys.ITER_SUBSCRIPTION_SYSTEM_HTTPSTATUSLINE, GetterUtil.getString2(entitlementsInfo.getString(UserKeys.HTTP_STATUS_LINE), IterKeys.OK) );
						subscriptionCtxVars.put(UserKeys.ITER_SUBSCRIPTION_SYSTEM_TECH_CODE, "Unable to retrieve entitlements for aboid " + aboid);
						ContextVariables.setRequestCtxVars(request, IterKeys.SUBSCRIPTION_SYSTEM_CONTEXT_VARS, subscriptionCtxVars);
					}
				}
			}
		}
		
		// Se añade el correspondiente "success" en caso de haber recibido un redirect
		redirectPage = redirectPage.concat(hasSuccess);
		if (doRedirect && HeaderCacheServletResponse.canRedirect(response, redirectPage) && !sso)
		{
			// Redirección 302 a la página establecida
			response.setStatus(HttpStatus.SC_MOVED_TEMPORARILY);
			response.sendRedirect(redirectPage);
		}
		
		return Validator.isNull(userId) ? null : LoginLocalServiceUtil.getSsoFlow(PortalUtil.getScopeGroupId(request), redirectPage, dogTagCookie.getValue(), dogTagCookie.getMaxAge(), userId, request);
	}
	
	public String getUserAboId(String userId, String friendlyGroupURL, String userEmail, String redirectPage, HttpServletRequest request) throws SecurityException, NoSuchMethodException, ServiceError, DocumentException, IOException, SQLException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		// Recuperamos el aboid del usuario
		String sql = String.format( IterRegisterQueries.GET_ABOID, userId );
		_log.debug("Query to get aboid:\n\t" + sql);
		Document d = PortalLocalServiceUtil.executeQueryAsDom( sql );
		String aboid = IterUserTools.decryptGDPR( XMLHelper.getTextValueOf(d, "/rs/row/@aboid") );
		
		if( Validator.isNull(aboid) )
		{
			JSONObject authenticationInfo = AuthenticateUser.doAuthentication(friendlyGroupURL, userEmail, StringPool.BLANK, StringPool.BLANK);
			
			if(authenticationInfo.getString(UserKeys.SUBSCRIPTION_SYSTEM_RESPONSE_CODE).equalsIgnoreCase(IterKeys.OK))
			{
				// Se recupera el aboid
				aboid = IterUserTools.encryptGDPR( StringEscapeUtils.escapeSql(authenticationInfo.getString(UserKeys.SUSCRIPTOR_ID)) );
				
				// Se actualiza la BBDD con el aboid
				sql = String.format( IterRegisterQueries.UPDATE_ABOID, aboid, userId);
				_log.debug("Query to update aboid:\n\t" + sql);
				PortalLocalServiceUtil.executeUpdateQuery(sql);
			}
			else 
			{
				// Añadir las variables como atributos al request
				Map<String, String> subscriptionCtxVars = ContextVariables.getRequestCtxVars(request, IterKeys.SUBSCRIPTION_SYSTEM_CONTEXT_VARS);
				subscriptionCtxVars.put(UserKeys.ITER_SUBSCRIPTION_SYSTEM_CODE, IterKeys.KO);
				subscriptionCtxVars.put(UserKeys.ITER_SUBSCRIPTION_SYSTEM_MSG, GetterUtil.getString2(authenticationInfo.getString(UserKeys.SUBSCRIPTION_SYSTEM_MSG), authenticationInfo.getString(UserKeys.HTTP_STATUS_LINE)));
				subscriptionCtxVars.put(UserKeys.ITER_REDIRECTURL_PARAM, redirectPage);
				subscriptionCtxVars.put(UserKeys.ITER_SUBSCRIPTION_SYSTEM_HTTPSTATUSCODE, GetterUtil.getString2(authenticationInfo.getString(UserKeys.HTTP_STATUS_CODE), String.valueOf(HttpStatus.SC_OK)) );
				subscriptionCtxVars.put(UserKeys.ITER_SUBSCRIPTION_SYSTEM_HTTPSTATUSLINE, GetterUtil.getString2(authenticationInfo.getString(UserKeys.HTTP_STATUS_LINE), IterKeys.OK) );
				subscriptionCtxVars.put(UserKeys.ITER_SUBSCRIPTION_SYSTEM_TECH_CODE, "Unable to retrieve aboid for user " + userEmail);
				ContextVariables.setRequestCtxVars(request, IterKeys.SUBSCRIPTION_SYSTEM_CONTEXT_VARS, subscriptionCtxVars);
			}
		}
		
		return aboid;
	}
	
	private Cookie refreshCookieEntitlements(Map<String, String> CookieMap, boolean isSessionCookie, String aboid, String encodedProducts, int cookieExpires, HttpServletRequest request, HttpServletResponse response) throws InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, PortalException, SystemException, ParseException
	{
		//Se genera la cookie con los nuevos productos, y el nuevo expires
		String userId 			= CookieMap.get(IterKeys.COOKIE_NAME_USER_ID);
		String usrName 			= CookieMap.get(IterKeys.COOKIE_NAME_USER_NAME);
		String userMail 		= CookieMap.get(IterKeys.COOKIE_NAME_USER_EMAIL);
		String user1Name 		= CookieMap.get(IterKeys.COOKIE_NAME_USR_1ST_NAME);
		String userLastName 	= CookieMap.get(IterKeys.COOKIE_NAME_USR_LASTNAME);
		String userLastName2	= CookieMap.get(IterKeys.COOKIE_NAME_USR_LASTNAME_2);
		String userAvatar 		= CookieMap.get(IterKeys.COOKIE_NAME_USR_AVATAR_URL);
		String ssid				= CookieMap.get(IterKeys.COOKIE_NAME_SESS_ID);

		String userAge 			= CookieMap.get(IterKeys.COOKIE_NAME_AGE);
		String userBirthday 	= CookieMap.get(IterKeys.COOKIE_NAME_BIRTHDAY);
		String userGender 		= CookieMap.get(IterKeys.COOKIE_NAME_GENDER);
		String userMaritalstatus= CookieMap.get(IterKeys.COOKIE_NAME_MARITALSTATUS);
		String userLanguage 	= CookieMap.get(IterKeys.COOKIE_NAME_LANGUAGE);
		String userCoordinates	= CookieMap.get(IterKeys.COOKIE_NAME_COORDINATES);
		String userCountry 		= CookieMap.get(IterKeys.COOKIE_NAME_COUNTRY);
		String userRegion 		= CookieMap.get(IterKeys.COOKIE_NAME_REGION);
		String userCity 		= CookieMap.get(IterKeys.COOKIE_NAME_CITY);
		String userAddress		= CookieMap.get(IterKeys.COOKIE_NAME_ADDRESS);
		String userPostalcode 	= CookieMap.get(IterKeys.COOKIE_NAME_POSTALCODE);
		String userTelephone	= CookieMap.get(IterKeys.COOKIE_NAME_TELEPHONE);
		
		// Creamos la nueva cookie USER-DOGTAG.
		Cookie newDogTag = PayCookieUtil.createUserDogTagCookie(
				request, userId, usrName, encodedProducts, userMail, user1Name, userLastName, userLastName2, userAvatar, aboid, ssid,
				userAge, userBirthday, userGender, userMaritalstatus, userLanguage, userCoordinates, userCountry, userRegion,
				userCity, userAddress, userPostalcode, userTelephone
		);
		
		Cookie visitorId = CookieUtil.createVisitorIdCookie(request, userId);
		
		if( !isSessionCookie )
		{
			newDogTag.setMaxAge(cookieExpires);
			visitorId.setMaxAge(cookieExpires);
		}
		
		// Añadimos la cookie a la respuesta
		response.setHeader("Set-Cookie", PayCookieUtil.getCookieAsString(newDogTag) );
		response.addHeader("Set-Cookie", PayCookieUtil.getCookieAsString(visitorId) );
		
		return newDogTag;
	}
}
