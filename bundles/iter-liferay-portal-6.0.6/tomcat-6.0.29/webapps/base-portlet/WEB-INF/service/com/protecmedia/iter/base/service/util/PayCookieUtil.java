package com.protecmedia.iter.base.service.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.CookieUtil;
import com.liferay.portal.kernel.util.Digester;
import com.liferay.portal.kernel.util.EncryptUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.IterSecureConfigTools;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.IterSecureConfig;
import com.liferay.portal.util.PortalUtil;

public class PayCookieUtil
{
	static public final String PARAM_SEPARATOR 			= "|";
	static public final String PRODUCT_SEPARATOR 		= ";";
	static public final String PRODUCT_DATE_SEPARATOR 	= ",";
	
	static private final String DOMAIN = "Domain";
	static private final String PATH = "Path";
	static private final String HTTPONLY = "HttpOnly";
	static private final String EXPIRES = "Expires";
	static private final String SECURE = "Secure";

	private static Log _log = LogFactoryUtil.getLog(PayCookieUtil.class);

	
	public static String getPlainCookie(HttpServletRequest request)
	{
		return getPlainCookie(request, IterKeys.COOKIE_NAME_USER_DOGTAG);
	}
	
	public static String getPlainCookie(HttpServletRequest request, String cookieName)
	{
		return plainCookieValue( CookieUtil.get(request, cookieName) );
	}
	
	public static String plainCookieValue(String valueEncrypt)
	{
		String valueDecrypt = null;

		try
		{
			if(Validator.isNotNull(valueEncrypt))
			{
				valueDecrypt = EncryptUtil.decrypt(URLDecoder.decode(valueEncrypt, "UTF-8"));

				if(_log.isDebugEnabled() && Validator.isNotNull(valueDecrypt))
					_log.info(valueDecrypt);
			}
		}
		catch (Exception e)
		{
			_log.error(e.toString());
			_log.debug(e);
		}

		return valueDecrypt;
	}
	
	/**
	 * 
	 * @param request
	 * @param cookieName
	 * @return Devuelveun mapa con los campos-valores de la cookie
	 * @throws ServiceError 
	 */
	public static Map<String, String> getCookieAsMap(HttpServletRequest request, String cookieName) throws ServiceError
	{
		return getCookieAsMap( getPlainCookie(request, cookieName), cookieName);
	}
	
	public static Map<String, String> getCookieAsMap(String plainCookie, String cookieName) throws ServiceError
	{
		Map<String, String> map = new HashMap<String, String>();
		
		if (Validator.isNotNull(plainCookie))
		{
			String[] cookiePlainArray = null;
			
			if (cookieName.equalsIgnoreCase(IterKeys.COOKIE_NAME_USER_DOGTAG))
			{
				cookiePlainArray = plainCookie.split(StringPool.BACK_SLASH + IterKeys.PARAM_SEPARATOR);
				
				long version = Long.parseLong(cookiePlainArray[0]);
				ErrorRaiser.throwIfFalse((version == 0 && cookiePlainArray.length == 6)  ||
										 (version == 1 && cookiePlainArray.length == 11) ||
										 (version == 2 && cookiePlainArray.length == 13) ||
										 (version == 3 && cookiePlainArray.length == 25));
				
				map.put(IterKeys.COOKIE_NAME, 			cookieName);
				map.put(IterKeys.COOKIE_NAME_VERSION, 	cookiePlainArray[0]);
				map.put(IterKeys.COOKIE_NAME_TIMESTAMP, cookiePlainArray[1]);
				map.put(IterKeys.COOKIE_NAME_USER_ID, 	cookiePlainArray[2]);
				map.put(IterKeys.COOKIE_NAME_USER_NAME, new String(Base64.decode(cookiePlainArray[3])));
				map.put(IterKeys.COOKIE_NAME_PRODUCTS, 	cookiePlainArray[4]);
				
				if (version == 0)
				{
					map.put(IterKeys.COOKIE_NAME_CHECKSUM, cookiePlainArray[5]);
				}
				else
				{
					map.put(IterKeys.COOKIE_NAME_USER_EMAIL, 	new String(Base64.decode(cookiePlainArray[5])));
					map.put(IterKeys.COOKIE_NAME_USR_1ST_NAME, 	new String(Base64.decode(cookiePlainArray[6])));
					map.put(IterKeys.COOKIE_NAME_USR_LASTNAME, 	new String(Base64.decode(cookiePlainArray[7])));
					map.put(IterKeys.COOKIE_NAME_USR_LASTNAME_2,new String(Base64.decode(cookiePlainArray[8])));
					map.put(IterKeys.COOKIE_NAME_USR_AVATAR_URL,new String(Base64.decode(cookiePlainArray[9])));
					
					if(version == 1)
					{
						map.put(IterKeys.COOKIE_NAME_CHECKSUM, 		cookiePlainArray[10]);
					}
					else if(version == 2)
					{
						map.put(IterKeys.COOKIE_NAME_ABO_ID,		new String(Base64.decode(cookiePlainArray[10])));
						map.put(IterKeys.COOKIE_NAME_SESS_ID,		new String(Base64.decode(cookiePlainArray[11])));
						map.put(IterKeys.COOKIE_NAME_CHECKSUM, 		cookiePlainArray[12]);
					}
					else
					{
						map.put(IterKeys.COOKIE_NAME_ABO_ID,		new String(Base64.decode(cookiePlainArray[10])));
						map.put(IterKeys.COOKIE_NAME_SESS_ID,		new String(Base64.decode(cookiePlainArray[11])));
						map.put(IterKeys.COOKIE_NAME_AGE,			cookiePlainArray[12]);
						map.put(IterKeys.COOKIE_NAME_BIRTHDAY,		cookiePlainArray[13]);
						map.put(IterKeys.COOKIE_NAME_GENDER,		cookiePlainArray[14]);
						map.put(IterKeys.COOKIE_NAME_MARITALSTATUS,	cookiePlainArray[15]);
						map.put(IterKeys.COOKIE_NAME_LANGUAGE,		new String(Base64.decode(cookiePlainArray[16])));
						map.put(IterKeys.COOKIE_NAME_COORDINATES,	new String(Base64.decode(cookiePlainArray[17])));
						map.put(IterKeys.COOKIE_NAME_COUNTRY,		new String(Base64.decode(cookiePlainArray[18])));
						map.put(IterKeys.COOKIE_NAME_REGION,		new String(Base64.decode(cookiePlainArray[19])));
						map.put(IterKeys.COOKIE_NAME_CITY,			new String(Base64.decode(cookiePlainArray[20])));
						map.put(IterKeys.COOKIE_NAME_ADDRESS,		new String(Base64.decode(cookiePlainArray[21])));
						map.put(IterKeys.COOKIE_NAME_POSTALCODE,	new String(Base64.decode(cookiePlainArray[22])));
						map.put(IterKeys.COOKIE_NAME_TELEPHONE,		new String(Base64.decode(cookiePlainArray[23])));
						map.put(IterKeys.COOKIE_NAME_CHECKSUM, 		cookiePlainArray[24]);
					}
				}
			}
			else if (cookieName.equalsIgnoreCase(IterKeys.COOKIE_NAME_USERID_DOGTAG))
			{
				cookiePlainArray = plainCookie.split(StringPool.BACK_SLASH + IterKeys.PARAM_SEPARATOR);
				ErrorRaiser.throwIfFalse(cookiePlainArray.length == 6);
				
				map.put(IterKeys.COOKIE_NAME, 			cookieName);
				map.put(IterKeys.COOKIE_NAME_VERSION, 	cookiePlainArray[0]);
				map.put(IterKeys.COOKIE_NAME_EXPIRE, 	cookiePlainArray[1]);
				map.put(IterKeys.COOKIE_NAME_USER_ID, 	cookiePlainArray[2]);
				map.put(IterKeys.COOKIE_NAME_INTENT, 	cookiePlainArray[3]);
				map.put(IterKeys.COOKIE_NAME_EXTRADATA, cookiePlainArray[4]);
				map.put(IterKeys.COOKIE_NAME_CHECKSUM, 	cookiePlainArray[5]);
			}
		}
		
		return map;
	}
	
	public static List<String[]> getProductsAndExpires(HttpServletRequest request)
	{
		List<String[]> productsAndExpires = new ArrayList<String[]>();
		try
		{
			String cookieProducts = getCookieAsMap(
					request, IterKeys.COOKIE_NAME_USER_DOGTAG).get(IterKeys.COOKIE_NAME_PRODUCTS);
			
			String[] productsArray = cookieProducts.split(StringPool.SEMICOLON);
			for(String currentProductArray:productsArray)
			{
				String[] currentProductExpirexArray = currentProductArray.split(StringPool.COMMA);
				if(currentProductExpirexArray != null && currentProductExpirexArray.length > 0)
					productsAndExpires.add(currentProductExpirexArray);
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return productsAndExpires;
	}
	
	public static String getUserId(HttpServletRequest request)
	{
		String userId = null;
		
		try
		{
			userId =  getCookieAsMap(request, IterKeys.COOKIE_NAME_USER_DOGTAG).get(IterKeys.COOKIE_NAME_USER_ID);
		}
		catch (Exception e)
		{
			_log.debug(e);
		}

		return userId;
	}
	
	public static String getCookieAsString(Cookie cookie)
	{
	    StringBuilder cookieString = new StringBuilder( cookie.getName() ).append( StringPool.EQUAL ).append( cookie.getValue() )
	    									.append( StringPool.SEMICOLON ).append( StringPool.SPACE );
	    
	    if(cookie.getMaxAge()!=-1)
	    {
	    	long maxAge = (long)cookie.getMaxAge() * 1000;
	    	Date d = new Date( Calendar.getInstance().getTimeInMillis() + maxAge );
	    	DateFormat df = new SimpleDateFormat( IterKeys.DATEFORMAT_EEE_D_MMM_YYYY_HH_MM_SS_GMT, Locale.US );
	    	df.setTimeZone(TimeZone.getTimeZone("GMT"));
	    	cookieString.append( EXPIRES ).append( StringPool.EQUAL ).append( df.format(d) ).append( StringPool.SEMICOLON ).append( StringPool.SPACE );
	    }
	    cookieString.append( DOMAIN ).append( StringPool.EQUAL ).append( cookie.getDomain() ).append( StringPool.SEMICOLON ).append( StringPool.SPACE )
	    			.append( PATH ).append( StringPool.EQUAL ).append( cookie.getPath() ).append( StringPool.SEMICOLON ).append( StringPool.SPACE )
	    			.append( HTTPONLY );

	    if(cookie.getSecure())
	    	cookieString.append( StringPool.SEMICOLON ).append( StringPool.SPACE ).append( SECURE );

		if(_log.isDebugEnabled())
			_log.debug("Cookie as string: " + cookieString.toString());
		
		return cookieString.toString();
	}
	
	/**
	 * Por defecto la cookie creada es de sesión, si se desea lo contrario se le debe indicar, a posteriori,
	 * un valor mediante <i>setMaxAge</i>.
	 * @param userid
	 * @param username
	 * @param products
	 * @param usr_email
	 * @param usr_1st_name
	 * @param usr_lastname
	 * @param usr_lastname_2
	 * @param usr_avatar_url
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @throws NoSuchPaddingException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 * @throws NoSuchProviderException
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws ParseException 
	 */
	public static Cookie createUserDogTagCookie(HttpServletRequest request, String userid, String username, String products, 
												String usr_email, String usr_1st_name, String usr_lastname, 
												String usr_lastname_2, String usr_avatar_url, String aboid, String sessid,
												String userAge, String userBirthday, String userGender, String userMaritalstatus, String userLanguage,
												String userCoordinates, String userCountry, String userRegion, String userCity, String userAddress,
												String userPostalcode, String userTelephone
			) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, PortalException, SystemException, ParseException
	{
		String data = "";
		if (Validator.isNotNull(username))
		{
			String currentTime =  String.valueOf(Calendar.getInstance().getTime().getTime());
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			DateFormat dfISO = new SimpleDateFormat("yyyyMMdd");
			if (Validator.isNotNull(userBirthday))
				userBirthday = dfISO.format(df.parse(userBirthday));
			
			StringBuilder value = new StringBuilder("3"												).append(PARAM_SEPARATOR).append(
					currentTime																		).append(PARAM_SEPARATOR).append(
					GetterUtil.getString(userid)													).append(PARAM_SEPARATOR).append(
					Base64.encode(GetterUtil.getString(username).getBytes(Digester.ENCODING))		).append(PARAM_SEPARATOR).append(
					GetterUtil.getString(products)													).append(PARAM_SEPARATOR).append(
					Base64.encode(GetterUtil.getString(usr_email).getBytes(Digester.ENCODING))		).append(PARAM_SEPARATOR).append(
					Base64.encode(GetterUtil.getString(usr_1st_name).getBytes(Digester.ENCODING))	).append(PARAM_SEPARATOR).append(
					Base64.encode(GetterUtil.getString(usr_lastname).getBytes(Digester.ENCODING))	).append(PARAM_SEPARATOR).append(
					Base64.encode(GetterUtil.getString(usr_lastname_2).getBytes(Digester.ENCODING))	).append(PARAM_SEPARATOR).append(
					Base64.encode(GetterUtil.getString(usr_avatar_url).getBytes(Digester.ENCODING))	).append(PARAM_SEPARATOR).append(
					Base64.encode(GetterUtil.getString(aboid).getBytes(Digester.ENCODING))			).append(PARAM_SEPARATOR).append(
					Base64.encode(GetterUtil.getString(sessid).getBytes(Digester.ENCODING))			).append(PARAM_SEPARATOR).append(
							
					userAge																			).append(PARAM_SEPARATOR).append(
					userBirthday																	).append(PARAM_SEPARATOR).append(
					userGender																		).append(PARAM_SEPARATOR).append(
					userMaritalstatus																).append(PARAM_SEPARATOR).append(
					Base64.encode(GetterUtil.getString(userLanguage).getBytes(Digester.ENCODING))	).append(PARAM_SEPARATOR).append(
					Base64.encode(GetterUtil.getString(userCoordinates).getBytes(Digester.ENCODING))).append(PARAM_SEPARATOR).append(
					Base64.encode(GetterUtil.getString(userCountry).getBytes(Digester.ENCODING))	).append(PARAM_SEPARATOR).append(
					Base64.encode(GetterUtil.getString(userRegion).getBytes(Digester.ENCODING))		).append(PARAM_SEPARATOR).append(
					Base64.encode(GetterUtil.getString(userCity).getBytes(Digester.ENCODING))		).append(PARAM_SEPARATOR).append(
					Base64.encode(GetterUtil.getString(userAddress).getBytes(Digester.ENCODING))	).append(PARAM_SEPARATOR).append(
					Base64.encode(GetterUtil.getString(userPostalcode).getBytes(Digester.ENCODING))	).append(PARAM_SEPARATOR).append(
					Base64.encode(GetterUtil.getString(userTelephone).getBytes(Digester.ENCODING))	).append(PARAM_SEPARATOR);
					
			
			_log.debug("Plain value without MD5: " + value.toString());
			
			data = value.append(EncryptUtil.digest(value.toString())).toString();
		}
		String encoded  = EncryptUtil.encrypt(data);
	    Cookie cookie 	= new Cookie( IterKeys.COOKIE_NAME_USER_DOGTAG, URLEncoder.encode(encoded, Digester.ENCODING) );
	    cookie.setPath( "/" );
	    
	    // No poner el atributo secure es incompatible con poner el atributo includeSubdomains del header HSTS
	    // http://jira.protecmedia.com:8080/browse/ITER-410?focusedCommentId=16791&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-16791
	    IterSecureConfig secureConfig = IterSecureConfigTools.getConfig(PortalUtil.getScopeGroupId(request));
	    boolean isSecure = secureConfig.https() && (secureConfig.secureCookie() || (secureConfig.hsts() && secureConfig.includeSubdomains()));
	    
	    cookie.setSecure( isSecure );
	    String cookieDomain = CookieUtil.getDomain(request);
	    if( Validator.isNotNull(cookieDomain) )
	    	cookie.setDomain( cookieDomain );
	    
	    // Por defecto es de sesión
	    cookie.setMaxAge( -1 );
	    return cookie;
	}

}