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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.portlet.ReadOnlyException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.crypto.DrupalEncryption;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.CookieConstants;
import com.liferay.portal.kernel.util.CookieUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.IterGlobalKeys;
import com.liferay.portal.kernel.util.IterSecureConfigTools;
import com.liferay.portal.kernel.util.IterUserTools;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.xml.Attribute;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.HotConfigUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.PayCookieUtil;
import com.protecmedia.iter.base.service.util.PortletMgr;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.user.LoginException;
import com.protecmedia.iter.user.service.base.LoginLocalServiceBaseImpl;
import com.protecmedia.iter.user.util.LoginUtil;
import com.protecmedia.iter.user.util.UserEntitlementsMgr;
import com.protecmedia.iter.user.util.UserKeys;
import com.protecmedia.iter.user.util.UserUtil;

/**
 * The implementation of the login local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.user.service.LoginLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.user.service.LoginLocalServiceUtil} to access the login local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @see com.protecmedia.iter.user.service.base.LoginLocalServiceBaseImpl
 * @see com.protecmedia.iter.user.service.LoginLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class LoginLocalServiceImpl extends LoginLocalServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(LoginLocalServiceImpl.class);
	
	static private final String SQL_LAYOUTS_FROM_GLOBALID	= new StringBuilder(
		"SELECT globalId, uuid_ localId													\n").append(
		"FROM XmlIO_Live															\n").append(
		"INNER JOIN Layout ON XmlIO_Live.localId = Layout.plid						\n").append(
		"	WHERE XmlIO_Live.groupId = %d											\n").append(
		"		AND classNameValue = '").append(IterKeys.CLASSNAME_LAYOUT).append("'\n").append(
		"		AND globalId IN ('%s')												\n").toString();

	static private final String SQL_LAYOUTS_FROM_LOCALID	= new StringBuilder(
		"SELECT globalId, uuid_ localId													\n").append(
		"FROM XmlIO_Live															\n").append(
		"INNER JOIN Layout ON XmlIO_Live.localId = Layout.plid						\n").append(
		"	WHERE XmlIO_Live.groupId = %d											\n").append(
		"		AND classNameValue = '").append(IterKeys.CLASSNAME_LAYOUT).append("'\n").append(
		"		AND uuid_ IN ('%s')												\n").toString();

	static private final String SQL_LAYOUTS_FROM_FRIENDLYURL = "SELECT uuid_ localId, friendlyURL globalId FROM Layout WHERE friendlyURL IN ('%s') AND groupId = %d";
	static private final String SQL_LAYOUTS_FROM_UUID		 = "SELECT uuid_ localId, friendlyURL globalId FROM Layout WHERE uuid_ IN ('%s') AND groupId = %d";
	
	
	static private final String XPATH_LOGIN_LAYOUTS 		 = "/portlet-preferences/preference[(name='editprofilepage' or name='signin' or name='forget' or name='loginpage') and string-length(value) > 0]/value";
	static private final String SQL_SEL_PRODUCTS 			 = "SELECT name, nameBase64 FROM Product WHERE groupid=%d";
	static private final String SQL_SEL_BASE64 				 = "SELECT nameBase64 FROM Product WHERE groupid=%d AND %s";
	
	static private final String CLEAR_RETRIES				 = new StringBuilder(
		"DELETE FROM iterusers_retries 											\n").append(
		"	WHERE usrid = '%s' 													\n").append(
		"		OR TIMESTAMPDIFF(SECOND,retrydate, '%s' ) > %d 					\n").toString();
	
	static private final String GET_USRID 					 = new StringBuilder(
		"SELECT usrid, aboid, entitlements, aboinfoexpires, userexpires, pwd, 	\n").append(
		"		(																\n").append(
		"			SELECT COUNT(1)												\n").append(
		"			FROM iterusers_retries										\n").append(
		"				WHERE iterusers_retries.usrid = iterusers.usrid			\n").append(
		"					AND TIMESTAMPDIFF(SECOND,retrydate, '%1$s' ) < %2$d	\n").append(		
		"		) numRetries													\n").append(
		"FROM iterusers															\n").append(
		"	WHERE (usrname IN ('%3$s','%4$s') OR email IN ('%3$s','%4$s')) AND delegationid = %5$d	  ").toString();
			
	
	static private final String INSERT_RETRY = new StringBuilder(
		"INSERT INTO iterusers_retries(usrid, retrydate) 						\n").append(
		"SELECT usrid, '%s'														\n").append(
		"FROM iterusers															\n").append(
		"	WHERE usrname = '%s'												  ").toString();
		
	static private final String GET_DATA_FOR_COOKIE = "SELECT * FROM iterusers WHERE usrid = '%s'";
	static private final String UPDATE_LAST_LOGIN 	= "UPDATE iterusers SET lastlogindate='%s' WHERE usrid='%s'";
	
	static private final String DB_DATE_FORMAT 		= "yyyy-MM-dd HH:mm:ss.S";
	static private final String GIFT_TIME_UNIT 		= "iter.suscription.gifttime.unit";
	static private final String GIFT_TIME_VALUE 	= "iter.suscription.gifttime.value";
	
	static private final String SUBSCRIPTION_XML 	= "subscriptionXML";
	static private final String DATA_FROM_CACHE 	= "dataFromCache";

	public String doLogin( String username, String password, boolean keepAlive, String origin, HttpServletRequest req, HttpServletResponse resp, String ssopwd ) throws LoginException
	{
		return doLogin( username, password, keepAlive, origin, req, resp, ssopwd, null);
	}
	public String doLogin( String username, String password, boolean keepAlive, String origin, HttpServletRequest req, HttpServletResponse resp, String ssopwd, String readerId) throws LoginException
	{
		String retVal = "";
		long groupId = 0;
		
		try
		{
			groupId = PortalUtil.getScopeGroupId(req);
		}
		catch (Exception e)
		{
			_log.error(e);
			throw new LoginException();
		}
		
		long delegationId = 0;
		if(groupId==0)
			throw new LoginException(IterErrorKeys.XYZ_ITR_E_UNSUPPORTED_HOST_HEADER_ZYX);
		else
		{
			try
			{
				delegationId = GroupLocalServiceUtil.getGroup(groupId).getDelegationId();
			}
			catch (Exception e)
			{
				_log.error(e);
				throw new LoginException();
			}
		}
		
		LoginUtil lu = new LoginUtil();
		lu.getLoginConfig( groupId );
		
		if( username==null || password==null || username.trim().isEmpty() || password.trim().isEmpty())
		{
			_log.info("User or password empty");
			throw new LoginException( lu.getValueOfLoginPreference("requiredfield") );
		}
		
		try
		{
			Object aloudMD5Object = req.getAttribute(IterKeys.HOTCONFIG_KEY_LOGIN_ALOUMD5);
			boolean aloudMD5 = false;
			if(aloudMD5Object != null)
			{
				aloudMD5 = Boolean.parseBoolean(aloudMD5Object.toString());
			}
			
			Document userDoc = getUserData(username, lu, delegationId);
			String userPwd 	 = XMLHelper.getTextValueOf(userDoc, "/rs/row/@pwd");
			
			if (Validator.isNotNull(ssopwd))
			{
				if ( !ssopwd.equals(userPwd) )
					throwUserNotExist(username, lu);
			}
			else
			{
				if (PropsValues.ITER_USER_PASSWORD_ENCRYPTION_DRUPAL_ENABLED)
				{
					if (!(DrupalEncryption.user_check_password(password, userPwd) || IterUserTools.encryptPwd(password).equals(userPwd)))
					{
						if (IterUserTools.isEncryptedPwd(password) && (HotConfigUtil.getKey(IterKeys.HOTCONFIG_KEY_LOGIN_ALOUMD5, false) || aloudMD5))
						{
							// Se intenta asumiendo que la clave esta directamente en MD5
							if ( !StringEscapeUtils.escapeSql(password).equals(userPwd) )
								throwUserNotExist(username, lu);
						}
						else
						{
							throwUserNotExist(username, lu);
						}
					}
				}
				else if (!IterUserTools.encryptPwd(password).equals(userPwd))
				{
					if (IterUserTools.isEncryptedPwd(password) && (HotConfigUtil.getKey(IterKeys.HOTCONFIG_KEY_LOGIN_ALOUMD5, false) || aloudMD5))
					{
						// Se intenta asumiendo que la clave esta directamente en MD5
						if ( !StringEscapeUtils.escapeSql(password).equals(userPwd) )
							throwUserNotExist(username, lu);
					}
					else
					{
						throwUserNotExist(username, lu);
					}
				}
			}
			
			// Existe usuario, no está en modo anti-robots y tiene el password correcto. Se comprueba que NO sea un usuario temporal
			checkTemporaryUser(userDoc, lu);
			
			String usrId 		= userDoc.selectSingleNode("/rs/row/@usrid").getStringValue();
			String cacheXml 	= userDoc.selectSingleNode("/rs/row/@entitlements").getStringValue();
			String infoExpires 	= userDoc.selectSingleNode("/rs/row/@aboinfoexpires").getStringValue();
			String suscriberid 	= userDoc.selectSingleNode("/rs/row/@aboid").getStringValue();
			
			// Se registra el readerId
			UserUtil.registerReaderId(groupId, usrId, readerId);
			
			// Autentificación correcta, se eliminan los intentos fallidos que activan el modo anti-robots
			clearRetries(usrId, username);

			if(UserUtil.hasAllRequieredFieldsFilled(groupId, usrId))
			{
				long aboinfoexpires = 0L;
				if(infoExpires!=null && !infoExpires.isEmpty())
					aboinfoexpires = new SimpleDateFormat(DB_DATE_FORMAT).parse( infoExpires ).getTime();
				// Si el aboid es NULL no se verifica al usuario con el sistema de suscripciones. 
				if(Validator.isNotNull(suscriberid))
					retVal = loadUserProducts(groupId, suscriberid, usrId, req, resp, keepAlive, cacheXml, aboinfoexpires, lu, origin);
				else
					retVal = addCookie( req, resp, "", Integer.parseInt( UserEntitlementsMgr.getExpires4Cookie(null, 0) ), keepAlive, usrId, GetterUtil.getString(String.valueOf(req.getAttribute(IterKeys.REQUEST_ATTRIBUTE_COOKIE_ID)), StringPool.BLANK), origin, lu );
				
				//Actualizo la última fecha de acceso del usuario
				PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_LAST_LOGIN, SQLQueries.getCurrentDate(), usrId));
			}
			else
			{
				String userexpires  = UserUtil.getCopleteProfileExpiresDateFormated(groupId);
				String userIdDogtag = URLEncoder.encode(UserUtil.createUserIdDogtagToken("0", userexpires, usrId, IterKeys.EDIT_PROFILE_INTENT_REGISTRY, JSONFactoryUtil.createJSONObject()), "UTF-8");
				
				String url = String.format("/user-portlet/edit-user-profile?%s=%s", IterKeys.COOKIE_NAME_USERID_DOGTAG, userIdDogtag);
				retVal = String.format("\"result\": \"OK\", \"msg\": \"\", \"furtheraction\": { \"action\": \"redirect\", \"location\": \"%s\" }", url);
			}
		}
		catch (LoginException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			_log.error(e);
			throw new LoginException( lu.getValueOfLoginPreference("unexpectederror") );
		}
		
		return retVal;
	}
	
	public String doLogout( HttpServletRequest req, HttpServletResponse resp ) throws LoginException
	{
		String retVal = "";
		long groupId = 0;
		
		try
		{
			groupId = PortalUtil.getScopeGroupId(req);
		}
		catch (Exception e)
		{
			_log.error(e);
			throw new LoginException();
		}
		
		LoginUtil lu = new LoginUtil();
		lu.getLoginConfig( groupId );
		
		try 
		{
			retVal = addCookie( req, resp, "", 0, true, "", "", req.getHeader("referer"), lu );
		}
		catch (Exception e)
		{
			_log.error(e);
			throw new LoginException( lu.getValueOfLoginPreference("unexpectederror") );
		}
		
		return retVal;
	}
	
	public void doSimulationLogout(HttpServletRequest request) throws LoginException
	{
		PortalLocalServiceUtil.removeIterProductList(request.getSession());
	}
	
	public String getLoginPreferences(String groupid)
	{
		String q = String.format( LoginUtil.GET_LOGIN_CONF, Long.valueOf(groupid) );
		String pref = LoginUtil.getFirstStringRowFromQuery(q);
		
		if (Validator.isNull(pref))
			pref = "<portlet-preferences />";
		
		return pref;
	}
	
	public void setLoginPreferences(String groupid, String prefs) throws DocumentException, IOException, SQLException, NumberFormatException, SystemException
	{
		Document dom = SAXReaderUtil.read(prefs);
		String INSERT_OR_UPDATE_LOGIN_CONF = "INSERT INTO Base_Communities(id_, groupId, loginconf) values ( %d, %d, '%s') ON DUPLICATE KEY UPDATE loginconf=VALUES(loginconf)";
		String q = String.format( INSERT_OR_UPDATE_LOGIN_CONF, CounterLocalServiceUtil.increment(), Long.valueOf(groupid), StringEscapeUtils.escapeSql(dom.asXML()) );
		PortalLocalServiceUtil.executeUpdateQuery(q);
	}
	
	/**
	 * Devuelve el DOM con la información asociada al usuario.
	 * Lanza una excepción si el usuario NO exite o, por superar el límite de reintentos permitidos, está en modo anti-robots
	 * 
	 * @param userName
	 * @param lu
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws LoginException
	 * @throws NoSuchProviderException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws UnsupportedEncodingException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	private Document getUserData(String userName, LoginUtil lu, long delegationId) throws SecurityException, NoSuchMethodException, LoginException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		String currentTime = new SimpleDateFormat(IterGlobalKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss).format( new Date() );
		
		String name = StringEscapeUtils.escapeSql(userName);
		
		String sql 		= String.format(GET_USRID, currentTime, 
							PropsValues.ITER_USER_LOGIN_ANTIROBOTS_TIMEDIFF, 
							IterUserTools.encryptGDPR(userName), name, delegationId);
		Document dom 	= PortalLocalServiceUtil.executeQueryAsDom(sql);

		// Se comprueba que exista el usrId, es como comprobar que exista un registro válido
		String userId 	= XMLHelper.getTextValueOf(dom, "/rs/row/@usrid");
		if ( Validator.isNull(userId) )
			throwUserNotExist(userName, lu);
		
		// Se comprueba, si está habilitado el modo antirobots, que NO se haya superado el número de reintentos en el tiempo estipulado
		long numRetries = XMLHelper.getLongValueOf(dom, "/rs/row/@numRetries");

		if (PropsValues.ITER_USER_LOGIN_ANTIROBOTS_ENABLED && numRetries > PropsValues.ITER_USER_LOGIN_ANTIROBOTS_MAX_RETRIES) 
		{
			if ( _log.isWarnEnabled() )
				_log.warn( String.format("ANTI-ROBOTS: %s (%d retries in the last %d seconds)", userName, numRetries, PropsValues.ITER_USER_LOGIN_ANTIROBOTS_TIMEDIFF) );
			
			throwUserNotExist(userName, lu);
		}
			
		return dom;
	}
	
	/**
	 * Lanza la excepción de que "<i>El usuario no existe</i>" e inserta un registro como 
	 * reintento fallido para el sistema anti-robots
	 * 
	 * @param userName
	 * @param lu
	 * @throws LoginException
	 */
	private void throwUserNotExist(String userName, LoginUtil lu) throws LoginException
	{
		// Si el modo antirobots está activado se inserta un registro en iterusers_retries
		_log.info( String.format("User %s does not exists.", userName) );

		insertRetry(userName);
		
		throw new LoginException( lu.getValueOfLoginPreference("usernotexists") );
	}
	
	/**
	 * Comprueba que el usuario a loguear NO sea temporal
	 * 
	 * @param dom
	 * @param lu
	 * @throws LoginException
	 */
	private void checkTemporaryUser(Document dom, LoginUtil lu) throws LoginException
	{
		// Se comprueba que no sea un usuario temporal
		String userExpires = XMLHelper.getTextValueOf(dom, "/rs/row/@userexpires");
		if (Validator.isNotNull(userExpires))
		{
			throw new LoginException( lu.getValueOfLoginPreference("temporaryuser") );
		}
	}
	
	/**
	 * Inserta un reintento más para ser analizado en el mcanismo anti-robots
	 * @param userName
	 */
	private void insertRetry(String userName)
	{
		if (PropsValues.ITER_USER_LOGIN_ANTIROBOTS_ENABLED)
		{
			try
			{
				String currentTime = new SimpleDateFormat(IterGlobalKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss).format( new Date() );
				String sql = String.format(INSERT_RETRY, currentTime, userName);
				PortalLocalServiceUtil.executeUpdateComittedQuery(sql);
			}
			catch (Throwable th)
			{
				_log.error(th);
			}
		}
	}
	
	/**
	 * Eliminan los intentos fallidos que activan el modo anti-robots
	 * @see http://jira.protecmedia.com:8080/browse/ITER-379 (El mecanismo antirobots deja registros en BBDD sin purgar)
	 * @param userId
	 */
	private void clearRetries(String userId, String userName)
	{
		if (PropsValues.ITER_USER_LOGIN_ANTIROBOTS_ENABLED)
		{
			try
			{
				if (_log.isDebugEnabled())
					_log.debug( String.format("Deleting iterusers_retries registers for %s (userid='%s')", userName, userId) );

				String currentTime 	= new SimpleDateFormat(IterGlobalKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss).format( new Date() );
				String sql 			= String.format(CLEAR_RETRIES, userId, currentTime, PropsValues.ITER_USER_LOGIN_ANTIROBOTS_TIMEDIFF);
				_log.debug(sql);
				
				PortalLocalServiceUtil.executeUpdateQuery(sql);
			}
			catch (Throwable th)
			{
				_log.error(th);
			}
		}
	}
	
	private String loadUserProducts(long groupId, String suscriberid, String usrId, HttpServletRequest req, HttpServletResponse resp, boolean keepAlive, String cacheXml, long aboinfoexpires, LoginUtil lu, String origin) throws Exception
	{
		String returnValue = "";
		String warningMsg = "";
		String ssoUrl = "";
		
		JSONObject subscriptionInfo = getSubscriptionXML(groupId, suscriberid, cacheXml, aboinfoexpires, lu);
		JSONObject cookData = null;
		
		String cookieId = GetterUtil.getString(String.valueOf(req.getAttribute(IterKeys.REQUEST_ATTRIBUTE_COOKIE_ID)), StringPool.BLANK);
		
		if(subscriptionInfo.getString(SUBSCRIPTION_XML).isEmpty())
		{
			// No está definida la url de conexión al sistema de suscripciones
			int defExpires = Integer.parseInt( UserEntitlementsMgr.getExpires4Cookie(null, 0) );
			ssoUrl = addCookie( req, resp, "", defExpires, keepAlive, usrId, cookieId, origin, lu );
		}
		else
		{
			cookData = UserEntitlementsMgr.getEntitlementsInfo(subscriptionInfo.getString(SUBSCRIPTION_XML), suscriberid, 0L, subscriptionInfo.getBoolean(DATA_FROM_CACHE));
			
			String ssrc = cookData.getString(UserKeys.SUBSCRIPTION_SYSTEM_RESPONSE_CODE);
			
			if(ssrc.equalsIgnoreCase(IterKeys.OK))
			{
				if( !subscriptionInfo.getBoolean(DATA_FROM_CACHE) && HotConfigUtil.getKey(UserKeys.TIMESTAMP_VALUE,1L)>0L)
					UserEntitlementsMgr.cacheEntitlements(suscriberid, subscriptionInfo.getString(SUBSCRIPTION_XML), cookData.getLong(UserKeys.MIN_VALID_PROD));
				
				if( cookData.getString(UserKeys.EXPIRES)!="" )
				{
					ssoUrl = addCookie( req, resp, cookData.getString(UserKeys.ENCODED_PRODUCT_LIST), Integer.parseInt(cookData.getString(UserKeys.EXPIRES)), keepAlive, usrId, cookieId, origin, lu );
					if( Validator.isNotNull(cookData.getString(UserKeys.SUBSCRIPTION_SYSTEM_MSG)) )
						warningMsg = cookData.getString(UserKeys.SUBSCRIPTION_SYSTEM_MSG);
				}
				else
				{
					//Los últimos datos de suscripción son validos pero hay suscripciones caducadas -> Se llama al servicio de suscripciones
					returnValue = loadUserProducts(groupId, suscriberid, usrId, req, resp, keepAlive, cacheXml, 0L, lu, origin);
				}
			}
			else if(ssrc.equalsIgnoreCase(IterKeys.KO)) 
			{
				// El sistema de suscripciones no valida al usuario.
				//Se genera cookie sin productos y con el expires configurado en el sistema.
				int defExpires = Integer.parseInt( UserEntitlementsMgr.getExpires4Cookie(null, 0) );
				ssoUrl = addCookie( req, resp, "", defExpires, keepAlive, usrId, cookieId, origin, lu );
				if( Validator.isNotNull(cookData.getString(UserKeys.SUBSCRIPTION_SYSTEM_MSG)) )
					warningMsg = cookData.getString(UserKeys.SUBSCRIPTION_SYSTEM_MSG);
			}
			else if(ssrc.equalsIgnoreCase(UserKeys.ITER_KO))
			{
				if( !cacheXml.isEmpty() && !subscriptionInfo.getBoolean(DATA_FROM_CACHE) )
				{
					String giftTimeUnit = HotConfigUtil.getKey(GIFT_TIME_UNIT, "day");
					long  giftTimeValue = HotConfigUtil.getKey(GIFT_TIME_VALUE, 1L);
					long msecGiftTime = UserEntitlementsMgr.getMsecTime(giftTimeUnit, giftTimeValue);
					
					cookData = UserEntitlementsMgr.getEntitlementsInfo(cacheXml, suscriberid, msecGiftTime, subscriptionInfo.getBoolean(DATA_FROM_CACHE));
					ssoUrl = addCookie( req, resp, cookData.getString(UserKeys.ENCODED_PRODUCT_LIST), Integer.parseInt(cookData.getString(UserKeys.EXPIRES)), keepAlive, usrId, cookieId, origin, lu );
					if(msecGiftTime==0)
						warningMsg = lu.getValueOfLoginPreference("unexpectederror");
				}
				else
				{
					//No se puede completar con éxito la validación de las suscripciones del usuario por primera vez.
					ssoUrl = addCookie( req, resp, "", -1, true, usrId, cookieId, origin, lu );
					warningMsg = lu.getValueOfLoginPreference("unexpectederror");
				}
			}
			
			req.setAttribute(UserKeys.ATTR_SUBSCRIPTION_SYSTEM_RESPONSE_CODE, ssrc);
			
		}
		
		if (Validator.isNull(returnValue))
		{
			if (Validator.isNotNull(warningMsg))
			{
				returnValue = "\"infomsg\": \"" + warningMsg + StringPool.QUOTE;
			}
			if (Validator.isNotNull(ssoUrl))
			{
				if (Validator.isNotNull(returnValue))
					returnValue = returnValue.concat(StringPool.COMMA_AND_SPACE);
				returnValue = returnValue.concat(ssoUrl);
			}
		}
		
		return returnValue;
	}
	
	private JSONObject getSubscriptionXML(long groupId, String subscriberid, String cachexml, long aboinfoexpires, LoginUtil lu) throws IOException, PortalException, SystemException, DocumentException, ParseException, NoSuchMethodException, SecurityException
	{
		JSONObject retval = JSONFactoryUtil.createJSONObject();
		String suscriptionXml = "";
		boolean dataFromCache = false;
		long now = Calendar.getInstance().getTime().getTime();
		
		if( cachexml.isEmpty() || aboinfoexpires<=now )
		{
			dataFromCache = false;
			String friendlyGroupURL = GroupLocalServiceUtil.getGroup(groupId).getFriendlyURL().replace(StringPool.SLASH, StringPool.PERIOD);
			suscriptionXml = UserEntitlementsMgr.getEntitlements(friendlyGroupURL, subscriberid, lu.getValueOfLoginPreference("unexpectederror"));
		}
		else
		{
			dataFromCache = true;
			suscriptionXml = cachexml;
		}
		
		retval.put(SUBSCRIPTION_XML, suscriptionXml);
		retval.put(DATA_FROM_CACHE, dataFromCache);
		
		return retval;
	}
	
	private String addCookie(HttpServletRequest request, HttpServletResponse response, String cookieData, int cookieExpires, boolean keepalive, String userId, String cookieId, String origin, LoginUtil lu) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, SecurityException, NoSuchMethodException, NoSuchProviderException, com.liferay.portal.kernel.error.ServiceError, IOException, SQLException, PortalException, SystemException, ParseException 
	{
		String ssoData = "";
		
		if (Validator.isNull(userId))
		{
			// Si no existe el usuario se quiere borrar dicha cookie
			CookieUtil.deleteCookies( request, response, new String[]{IterKeys.COOKIE_NAME_USER_DOGTAG, CookieConstants.COOKIE_NAME_VISITOR_ID} );
			// SSO para meliminar la cookie
		    ssoData = getSsoFlow(lu, origin, StringPool.BLANK, -1, StringPool.BLANK, request);
		}
		else
		{	
			Document rs = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_DATA_FOR_COOKIE, userId) );
			
			String email = IterUserTools.decryptGDPR( XMLHelper.getTextValueOf(rs, "/rs/row/@email") );
			ErrorRaiser.throwIfNull(email, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			String usrName			= IterUserTools.decryptGDPR( GetterUtil.getString(XMLHelper.getTextValueOf(rs, "/rs/row/@usrname"), 		StringPool.BLANK) );
			String firstname 		= IterUserTools.decryptGDPR( GetterUtil.getString(XMLHelper.getTextValueOf(rs, "/rs/row/@firstname"), 		StringPool.BLANK) );
			String lastname 		= IterUserTools.decryptGDPR( GetterUtil.getString(XMLHelper.getTextValueOf(rs, "/rs/row/@lastname"), 		StringPool.BLANK) );
			String secondlastname 	= IterUserTools.decryptGDPR( GetterUtil.getString(XMLHelper.getTextValueOf(rs, "/rs/row/@secondlastname"), 	StringPool.BLANK) );
			String avatarurl 		= IterUserTools.decryptGDPR( GetterUtil.getString(XMLHelper.getTextValueOf(rs, "/rs/row/@avatarurl"), 		StringPool.BLANK) );
			String aboid			= IterUserTools.decryptGDPR( GetterUtil.getString(XMLHelper.getTextValueOf(rs, "/rs/row/@aboid"), "0")						  );
			
			String userAge			= 							 GetterUtil.getString(XMLHelper.getTextValueOf(rs, "/rs/row/@age"), 			StringPool.BLANK);
			String userBirthday 	= 							 GetterUtil.getString(XMLHelper.getTextValueOf(rs, "/rs/row/@birthday"), 		StringPool.BLANK);
			String userGender 		= IterUserTools.decryptGDPR( GetterUtil.getString(XMLHelper.getTextValueOf(rs, "/rs/row/@gender"), 			StringPool.BLANK) );
			String userMaritalstatus= IterUserTools.decryptGDPR( GetterUtil.getString(XMLHelper.getTextValueOf(rs, "/rs/row/@maritalstatus"), 	StringPool.BLANK) );
			String userLanguage 	= IterUserTools.decryptGDPR( GetterUtil.getString(XMLHelper.getTextValueOf(rs, "/rs/row/@language"), 		StringPool.BLANK) );
			String userCoordinates	= IterUserTools.decryptGDPR( GetterUtil.getString(XMLHelper.getTextValueOf(rs, "/rs/row/@coordinates"), 	StringPool.BLANK) );
			String userCountry		= IterUserTools.decryptGDPR( GetterUtil.getString(XMLHelper.getTextValueOf(rs, "/rs/row/@country"), 		StringPool.BLANK) );
			String userRegion 		= IterUserTools.decryptGDPR( GetterUtil.getString(XMLHelper.getTextValueOf(rs, "/rs/row/@region"), 			StringPool.BLANK) );
			String userCity 		= IterUserTools.decryptGDPR( GetterUtil.getString(XMLHelper.getTextValueOf(rs, "/rs/row/@city"), 			StringPool.BLANK) );
			String userAddress 		= IterUserTools.decryptGDPR( GetterUtil.getString(XMLHelper.getTextValueOf(rs, "/rs/row/@address"), 		StringPool.BLANK) );
			String userPostalcode 	= IterUserTools.decryptGDPR( GetterUtil.getString(XMLHelper.getTextValueOf(rs, "/rs/row/@postalcode"), 		StringPool.BLANK) );
			String userTelephone	= IterUserTools.decryptGDPR( GetterUtil.getString(XMLHelper.getTextValueOf(rs, "/rs/row/@telephone"), 		StringPool.BLANK) );
			
			if( Validator.isNull(cookieId) )
				cookieId = SQLQueries.getUUID();
	
			Cookie cookie = PayCookieUtil.createUserDogTagCookie(
					request, userId, usrName, cookieData, email, firstname, lastname, secondlastname, avatarurl, aboid, cookieId,
					userAge, userBirthday, userGender, userMaritalstatus, userLanguage, userCoordinates, userCountry, userRegion,
					userCity, userAddress, userPostalcode, userTelephone
			);
			
			Cookie visitorId = CookieUtil.createVisitorIdCookie(request, userId);
			
			if (!keepalive)
				cookieExpires=-1;
			
		    cookie.setMaxAge( cookieExpires );
		    visitorId.setMaxAge( cookieExpires );
		    
		    // Al no hacer un addCookie se pierde el cookie en RaperHttpServletResponse.java y no funciona
		    // el servlet GetEntitlements para el login en Tablets porque falla el RaperHttpServletResponse.getCookieData 
		    request.setAttribute("Set-Cookie-Data", cookie.getValue());
		    response.addHeader("Set-Cookie", PayCookieUtil.getCookieAsString(cookie) );
		    response.addHeader("Set-Cookie", PayCookieUtil.getCookieAsString(visitorId) );
		    
		    // SSO para añadir la cookie
		    ssoData = getSsoFlow(lu, origin, cookie.getValue(), cookie.getMaxAge(), userId, request);
		}
		
		return ssoData;
	}

	public String getProductsPreferences(HttpServletRequest request, String xmlInitParams) throws DocumentException, ServiceError
    {
		Document initParamsDOM = SAXReaderUtil.read(xmlInitParams);
		long groupId = XMLHelper.getLongValueOf(initParamsDOM.getRootElement(), "scopeGroupId");
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String query = String.format(SQL_SEL_PRODUCTS, groupId);
		
		Document dom = SAXReaderUtil.createDocument();
		Element root1 =  dom.addElement("portlet-preferences");
		Element root2 = root1.addElement("preference");
		Element row = root2.addElement("name");
		row.addText("productsList");
		
		List<Object> results = PortalLocalServiceUtil.executeQueryAsList(query);
		List<String> listProducts = new ArrayList<String>();
		List<String> listBase64 = new ArrayList<String>();
		if(results != null && results.size() > 0)
		{
			for (int i = 0; i < results.size(); i++) 
			{
				Object[] resultData = (Object[])results.get(i);
				Element rows = root2.addElement("value");
				rows.addText(String.valueOf(resultData[0].toString()));
				listProducts.add(resultData[0].toString());
				listBase64.add(resultData[1].toString());
			}
		}
  
		if(PortalLocalServiceUtil.getIterProductList(request) != null)
		{
			Element rootList = root1.addElement("preference");
			Element rowList = rootList.addElement("name");
			rowList.addText("selectedproductlist");
			
			List<String> list = PortalLocalServiceUtil.getIterProductList(request);
			
			for(int i=0; i < list.size(); i++)
			{
				for(int j=0; j < listBase64.size(); j++)
				{
					if(list.get(i).toString().equals(listBase64.get(j).toString()))
					{
						Element rowsList = rootList.addElement("value");
						rowsList.addText(String.valueOf(listProducts.get(j)));
					}
				}
			}      
		}

		String s = "";
		try
		{
			s = dom.formattedString();
		}
		catch (IOException e)
		{
			_log.error(e);
		}
		
		return s;
    }
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setProducts(HttpServletRequest request, String selectedproducts)
    {
		try 
		{
			List results = null;
			if(Validator.isNotNull(selectedproducts))
			{
				Element dataPreferences = SAXReaderUtil.read(selectedproducts).getRootElement();
				List<Node> nodes = dataPreferences.selectNodes("//value");
				
				if (nodes != null && nodes.size() > 0)
				{
					long groupId = XMLHelper.getLongValueOf(dataPreferences, "@groupid");
					ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
					StringBuffer inQuery = new StringBuffer();
					for (int i = 0; i < nodes.size(); i++) 
					{
						Node node = nodes.get(i);
						String currentValue = node.getStringValue().replaceAll("'", "''");
						
						if(i == 0)
							inQuery.append("NAME ='" + currentValue + "'");                      
						else
							inQuery.append(" OR NAME = '" + currentValue + "'");
					}
	
					String query = String.format(SQL_SEL_BASE64, groupId, inQuery.toString());
					results = PortalLocalServiceUtil.executeQueryAsList(query);		
				}
			}

			PortalLocalServiceUtil.setIterProductList(request, results);

		}
		catch (Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
    }
	
	public Document exportData(Long groupId) throws com.liferay.portal.kernel.error.ServiceError, DocumentException, SecurityException, NoSuchMethodException
	{
		return exportData(groupId, true);
	}
	private Document exportData(Long groupId, boolean isExport) throws com.liferay.portal.kernel.error.ServiceError, DocumentException, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document dom = SAXReaderUtil.read( getLoginPreferences(String.valueOf(groupId)) );
		
        List<Node> layouts = dom.selectNodes(XPATH_LOGIN_LAYOUTS);
        String sql = (isExport) ?
        					String.format(SQL_LAYOUTS_FROM_UUID, 	StringUtils.join(XMLHelper.getStringValues(layouts), "','"), groupId) :
        					String.format(SQL_LAYOUTS_FROM_LOCALID, groupId, StringUtils.join(XMLHelper.getStringValues(layouts), "','"), groupId) ;
        					
        Document layoutsDom = PortalLocalServiceUtil.executeQueryAsDom(sql);
        
        for (Node node:layouts)
        {
        	Element layout  = (Element)node;
        	String localId  = layout.getText();
        	String globalId = XMLHelper.getTextValueOf(layoutsDom, String.format("/rs/row[@localId='%s']/@globalId", localId));
        	
        	ErrorRaiser.throwIfNull(globalId, IterErrorKeys.XYZ_E_UNDEFINED_LAYOUT_ZYX, localId);
        	layout.setText(globalId);
        }
        
        // Si es una exportación de Iteradmin, elimina el nodo 'ssopartners' si lo tuviera configurado,
        // ya que los grupos no tienen por que existir en el sitio en el que se importe
        if (isExport)
        {
	        Node ssopartners = dom.selectSingleNode("/portlet-preferences/preference[name[text()='ssopartners']]");
	        if (Validator.isNotNull(ssopartners))
	        	ssopartners.detach();
        }
			
		return dom;
	}
	
	public void importData(String data) throws DocumentException, ServiceError, PortalException, SystemException, IOException, SQLException, SecurityException, NoSuchMethodException
	{
        Document dom = SAXReaderUtil.read(data);
        // Busca primero el groupIid en el xml, y si no existe, se busca por groupName
        long groupId = XMLHelper.getLongValueOf(dom, "/portlet-preferences/@groupId");
        if (groupId <= 0)
        {
        	String groupName = XMLHelper.getStringValueOf(dom, "/portlet-preferences/@groupName");
        	ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        	groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
        }
        ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
        // Elimina los atributos groupId, groupName y updtIfExist que pueden llegar de la publicación o importación.
        Attribute attToDelete = dom.getRootElement().attribute("groupId");
        if (attToDelete != null) attToDelete.detach();
        attToDelete = dom.getRootElement().attribute("groupName");
        if (attToDelete != null) attToDelete.detach();
        attToDelete = dom.getRootElement().attribute("updtIfExist");
        if (attToDelete != null) attToDelete.detach();
        
        List<Node> layouts = dom.selectNodes(XPATH_LOGIN_LAYOUTS);
        boolean isImport = GetterUtil.getBoolean( XMLHelper.getTextValueOf(dom.getRootElement(), "@importProcess") );
        String sql = (isImport) ?
        					String.format(SQL_LAYOUTS_FROM_FRIENDLYURL, StringUtils.join(XMLHelper.getStringValues(layouts), "','"), groupId) :
        					String.format(SQL_LAYOUTS_FROM_GLOBALID, 	groupId, StringUtils.join(XMLHelper.getStringValues(layouts), "','"), groupId);
        					
        Document layoutsDom = PortalLocalServiceUtil.executeQueryAsDom(sql);
        
        for (Node node:layouts)
        {
        	Element layout = (Element)node;
        	String globalId = layout.getText();
        	String localId = XMLHelper.getTextValueOf(layoutsDom, String.format("/rs/row[@globalId='%s']/@localId", globalId));
        	
        	ErrorRaiser.throwIfNull(localId, IterErrorKeys.XYZ_E_UNDEFINED_LAYOUT_ZYX, globalId);
        	layout.setText(localId);
        }
        
        // Si es una importación de IterAdmin, elimina el atributo importProcess
        if (isImport)
        	dom.getRootElement().attribute("importProcess").detach();
        
        // Guarda las preferencias.
        setLoginPreferences(String.valueOf(groupId), dom.asXML());
	}
	
	public void publish(long groupId) throws ServiceError, PortalException, SystemException, DocumentException, ClientProtocolException, SecurityException, IOException, NoSuchMethodException, ReadOnlyException
	{
		Document dom = exportData(groupId, false);
		
		// Recupera el nombre del grupo para la publicación.
		dom.getRootElement().addAttribute("groupName", GroupLocalServiceUtil.getGroup(groupId).getName());
		dom.setXMLEncoding("ISO-8859-1");
		
		String data = dom.asXML();
		if ( _log.isDebugEnabled() )
			_log.debug("Login portlet preferences: " + data);
		
		publishToLive("com.protecmedia.iter.user.service.LoginServiceUtil", "importData", data);
	}
	
	private void publishToLive(String className, String methodName, String data) throws JSONException, ClientProtocolException, SystemException, SecurityException, IOException, NoSuchMethodException, ServiceError 
    {
          List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
          remoteMethodParams.add(new BasicNameValuePair("serviceClassName", className));
          remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", methodName));
          remoteMethodParams.add(new BasicNameValuePair("serviceParameters", "[data]"));
          remoteMethodParams.add(new BasicNameValuePair("data", data));
          JSONUtil.executeMethod("/c/portal/json_service", remoteMethodParams);
    }
	
	public String getSsoFlow(long groupId, String origin, String sessionCookie, int sessionexpires, String userid, HttpServletRequest request) throws LoginException
	{
		LoginUtil lu = new LoginUtil();
		lu.getLoginConfig(groupId);
		String ssoUrlChain = getSsoFlow(lu, origin, sessionCookie, sessionexpires, userid, request);
		if (Validator.isNotNull(ssoUrlChain))
			ssoUrlChain = ssoUrlChain.substring(7, ssoUrlChain.length() - 1);
		return ssoUrlChain;
	}
	
	private String getSsoFlow(LoginUtil lu, String origin, String sessionCookie, int sessionexpires, String userid, HttpServletRequest request) throws LoginException
	{
		StringBuilder ssoRedirection = new StringBuilder();
		
		try
		{	
			// Recupera las preferencias de SSO
			String ssopartnersPreference = lu.getValueOfLoginPreference("ssopartners");
			if (Validator.isNotNull(ssopartnersPreference))
			{

				// Recupera el propio host
				String ownUrl = IterRequest.getOriginalRequest().getScheme() + "://" + IterRequest.getOriginalRequest().getServerName();
				
				// Procesa la url a la que hay que volver
				if (Validator.isNull(origin))
					origin = request.getHeader("Referer");
				if (Validator.isNull(origin))
				{
					Cookie[] cookies = CookieUtil.getCookies(request, new String[]{IterKeys.COOKIE_NAME_SOCIAL_REDIRECT});
					if (cookies.length > 0)
						origin = cookies[0].getValue();
				}
				if (Validator.isNull(origin))
				{
					if (Validator.isNotNull(request.getAttribute(IterKeys.REQUEST_ATTRIBUTE_SSO_DESTINATION_URL)))
					origin = ownUrl + request.getAttribute(IterKeys.REQUEST_ATTRIBUTE_SSO_DESTINATION_URL).toString();
				}
				URL url = new URL(origin);
				String originHost = url.getProtocol() + "://" + url.getHost();
				
				// Crea el listado de URLs en los que realizar el Login / Logout
				StringBuilder ssopartners = new StringBuilder();
				String[] ssoPartnersList = ssopartnersPreference.split(StringPool.COMMA);
				
				// Traduce los groupsId a sus urls de los sitios en los que hay que realizar el sso
				for (int i = 0; i < ssoPartnersList.length; i++)
				{
					long groupId = Long.parseLong(ssoPartnersList[i]);
					
					// Recupera el protocolo a usar y el virtualhost
					String protocol = IterSecureConfigTools.getConfiguredHTTPS(groupId) ? Http.HTTPS_WITH_SLASH : Http.HTTP_WITH_SLASH;
					String host = LayoutSetLocalServiceUtil.getLayoutSet(groupId, false).getVirtualHost();
					
					// Crea la URL del sitio
					ssoPartnersList[i] = protocol.concat(host);
				}
				
				// Si no está la url del sitio que gestiona los usuarios, la añade
				if (!ArrayUtil.contains(ssoPartnersList, ownUrl))
					ssopartners.append(ownUrl);
				
				// Añade las urls de los sitios en los que hay que realizar el sso
				for (String groupUrl : ssoPartnersList)
				{	
					// Si no es la URL a la que se llamará inicialmente, la añade al listado
					if (!originHost.equals(groupUrl))
						ssopartners.append( ssopartners.length() == 0 ? groupUrl : StringPool.COMMA.concat(groupUrl) );
				}
				
				// Añade las URLs que se hayan podido indicar en el portal-ext.properties
				long groupId = PortalUtil.getScopeGroupId(request);
				String friendlyGroupURL = GroupLocalServiceUtil.getGroup(groupId).getFriendlyURL().replace(StringPool.SLASH, StringPool.PERIOD);
				String externalSsoPartners = PropsUtil.get(String.format(PropsKeys.ITER_SSOPARTNERS, friendlyGroupURL));
				if (Validator.isNotNull(externalSsoPartners))
				{
					for(String externalPartner : externalSsoPartners.split(StringPool.COMMA))
					{
						// Si no está ya incluída, la añade
						if (!ArrayUtil.contains(ssoPartnersList, externalPartner) && !ownUrl.equals(externalPartner))
							ssopartners.append( ssopartners.length() == 0 ? externalPartner : StringPool.COMMA.concat(externalPartner) );
					}
				}
				
				// Crea el JSON con la llamada que desencadena el redireccionamiento
				ssoRedirection.append(StringPool.QUOTE).append("sso").append(StringPool.QUOTE).append(StringPool.COLON);
				ssoRedirection.append(StringPool.QUOTE);
				String impersonateUrl = originHost.concat("/restapi/user/impersonate");
				ssoRedirection.append(impersonateUrl).append(StringPool.QUESTION);
				ssoRedirection.append("sessioncookie=").append(sessionCookie).append(StringPool.AMPERSAND);
				ssoRedirection.append("sessionexpires=").append(sessionexpires).append(StringPool.AMPERSAND);
				ssoRedirection.append("userid=").append(userid).append(StringPool.AMPERSAND);
				ssoRedirection.append("origin=").append(HttpUtil.encodeURL(origin)).append(StringPool.AMPERSAND);
				ssoRedirection.append("ssopartners=").append(HttpUtil.encodeURL(ssopartners.toString()));
				ssoRedirection.append(StringPool.QUOTE);
			}
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new LoginException( lu.getValueOfLoginPreference("unexpectederror") );
		}
		
		return ssoRedirection.toString();
	}
	
	public String getSsoCandidates(long groupId) throws DocumentException, SystemException
	{
		Element rs = SAXReaderUtil.read("<rs/>").getRootElement();
		
		try
		{
			// Recupera el grupo actual
			Group currentGroup = GroupLocalServiceUtil.getGroup(groupId);
			long delegationId = currentGroup.getDelegationId();
			
			// Recupera todas las compañías
			List<Company> companies = CompanyLocalServiceUtil.getCompanies();
			if(companies != null && companies.size() > 0)
			{
				// Procesa todos los grupos de la comñía
				String companyId = String.valueOf(companies.get(0).getCompanyId());
				List<Group> groups = PortletMgr.getGroups(companyId);
				for(Group group:groups)
				{
					// Si no es el grupo actual y pertenece a la misma delegación, lo añade al listado
					if (group.getGroupId() != groupId && delegationId == group.getDelegationId())
					{
						Element row = SAXReaderUtil.read("<row/>").getRootElement();
						row.addAttribute("groupid", String.valueOf(group.getGroupId()));
						row.addAttribute("virtualhost", group.getPublicLayoutSet().getVirtualHost());
						rs.add(row);
					}
				}
			}
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return rs.asXML();
	}
}
