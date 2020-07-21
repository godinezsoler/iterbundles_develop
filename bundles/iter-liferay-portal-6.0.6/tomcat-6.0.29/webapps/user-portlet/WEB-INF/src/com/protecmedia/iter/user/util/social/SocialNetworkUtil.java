package com.protecmedia.iter.user.util.social;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.IterUserTools;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.LayoutSet;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.user.service.UserOperationsServiceUtil;

public class SocialNetworkUtil
{
	private static Log _log = LogFactoryUtil.getLog(SocialNetworkUtil.class);

	public static final String SOCIAL_NAME_DISQUS 			= "disqus";
	public static final String SOCIAL_NAME_GOOGLEPLUS 		= "googleplus";
	public static final String SOCIAL_NAME_GOOGLEPLUS_SHORT	= "google";
	public static final String SOCIAL_NAME_FACEBOOK 		= "facebook";
	public static final String SOCIAL_NAME_TWITTER 			= "twitter";
	
	public static final int SOCIAL_NAME_DISQUS_POS 		= 0;
	public static final int SOCIAL_NAME_FACEBOOK_POS 	= 1;
	public static final int SOCIAL_NAME_GOOGLEPLUS_POS 	= 2;
	public static final int SOCIAL_NAME_TWITTER_POS 	= 3;
	
	public static final String PARAM_SCOPE 				= "scope";
	public static final String PARAM_CODE 				= "code";
	public static final String PARAM_GROUPID			= "groupid";
	public static final String PARAM_ERROR 				= "error";
	public static final String PARAM_ERROR_CODE 		= "error_code";
	public static final String PARAM_DENIED 			= "denied";
	public static final String ACCESS_TOKEN				= "access_token";
	public static final String OAUTH_TOKEN				= "oauth_token";
	public static final String OAUTH_VERIFIER			= "oauth_verifier";
	
	//Social fields
	public static final String XYZ_FIELD_AVATARURL_DEFAULT_ZYX 	= "XYZ_FIELD_AVATARURL_DEFAULT_ZYX";
	public static final String XYZ_FIELD_AVATARURL_LARGE_ZYX	= "XYZ_FIELD_AVATARURL_LARGE_ZYX";
	public static final String XYZ_FIELD_AVATARURL_SMALL_ZYX	= "XYZ_FIELD_AVATARURL_SMALL_ZYX";
	public static final String XYZ_FIELD_BIRTHDAY_ZYX 			= "XYZ_FIELD_BIRTHDAY_ZYX";
	public static final String XYZ_FIELD_EMAIL_ZYX 				= "XYZ_FIELD_EMAIL_ZYX";
	public static final String XYZ_FIELD_EMPLOYER_ZYX 			= "XYZ_FIELD_EMPLOYER_ZYX";
	public static final String XYZ_FIELD_FIRSTNAME_ZYX 			= "XYZ_FIELD_FIRSTNAME_ZYX";
	public static final String XYZ_FIELD_GENDER_ZYX 			= "XYZ_FIELD_GENDER_ZYX";
	public static final String XYZ_FIELD_HOMETOWN_ZYX 			= "XYZ_FIELD_HOMETOWN_ZYX";
	public static final String XYZ_FIELD_LASTNAME_ZYX 			= "XYZ_FIELD_LASTNAME_ZYX";
	public static final String XYZ_FIELD_LOCALE_ZYX 			= "XYZ_FIELD_LOCALE_ZYX";
	public static final String XYZ_FIELD_LOCATION_ZYX 			= "XYZ_FIELD_LOCATION_ZYX";
	public static final String XYZ_FIELD_NAME_ZYX 				= "XYZ_FIELD_NAME_ZYX";
	public static final String XYZ_FIELD_PROFILEURL_ZYX 		= "XYZ_FIELD_PROFILEURL_ZYX";
	public static final String XYZ_FIELD_USERNAME_ZYX 			= "XYZ_FIELD_USERNAME_ZYX";
	public static final String XYZ_FIELD_WEB_ZYX 				= "XYZ_FIELD_WEB_ZYX";
	
	public static final String GET_KEY_BY_SOCIAL_TYPE 				= "SELECT c.%s FROM itersocialconfig c " 												+
																			"INNER JOIN itersocial s ON s.itersocialid=c.itersocialid " 					+
																			"WHERE c.groupid=%d AND s.socialname='%s'";
	
	public static final String GET_SCOPE_BY_SOCIAL_TYPE 			= "SELECT p.scopename FROM itersocialscope p " +
																			"INNER JOIN itersocialconfig c ON c.itersocialscopeid=p.itersocialscopeid " 	+
																			"INNER JOIN itersocial s ON s.itersocialid=c.itersocialid " 					+
																			"WHERE c.groupid=%d AND s.socialname='%s'";
	
	
	public static String getConfigPublicKey(String socialType, long groupId) throws SecurityException, NoSuchMethodException
	{
		 return XMLHelper.getTextValueOf(PortalLocalServiceUtil.executeQueryAsDom(
				 							String.format(GET_KEY_BY_SOCIAL_TYPE, "publicKey", groupId, socialType)),
				 							"/rs/row/@publicKey");
	}
	
	public static String getConfigSecretKey(String socialType, long groupId) throws SecurityException, NoSuchMethodException
	{
		 return XMLHelper.getTextValueOf(PortalLocalServiceUtil.executeQueryAsDom(
				 							String.format(GET_KEY_BY_SOCIAL_TYPE, "secretKey", groupId, socialType)),
				 							"/rs/row/@secretKey");
	}
	
	private static String getScopeConnection(String scopeConfig, Map<String, String> scopes)
	{
		String scope = scopes.get(scopeConfig);
		
		if(Validator.isNull(scope))
			scope = StringPool.BLANK;
		
		return scope;
	}
	
	public static String getConfigScope(String socialType, long groupId, Map<String, String> scopes) throws SecurityException, NoSuchMethodException
	{
		String scope = "";
		
		try
		{
			Document doc = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SCOPE_BY_SOCIAL_TYPE, groupId, socialType));
			scope = getScopeConnection(XMLHelper.getTextValueOf(doc, "/rs/row/@scopename"), scopes);
		}
		catch (Exception e)
		{
			_log.debug(e.toString());
			_log.error(e);
		}
		
		return scope;
	}
	
	public static String getScope(String scope)
	{
		String scopeParam = StringPool.BLANK;
		if(Validator.isNotNull(scope))
			scopeParam = PARAM_SCOPE + StringPool.EQUAL + scope + StringPool.AMPERSAND;
		
		return scopeParam;
	}
	
	public static long getGroupIdFromRequest(HttpServletRequest request) throws PortalException, SystemException, MalformedURLException
	{
		long groupId = 0;
		String groupIdParam = request.getParameter(SocialNetworkUtil.PARAM_GROUPID);
		if(Validator.isNotNull(groupIdParam))
		{
			groupId = Long.parseLong(groupIdParam);
		}
		else
		{
			LayoutSet layoutSet = LayoutSetLocalServiceUtil.getLayoutSet(request.getServerName());
			if(layoutSet != null)
				groupId = layoutSet.getGroupId();
		}
		
		return groupId;
	}
	
	public static void addRedirectCookie(HttpServletRequest request, HttpServletResponse response) throws PortalException, SystemException, MalformedURLException
	{
		String referer = request.getHeader("Referer");
		if(Validator.isNotNull(referer))
		{
		    Cookie cookie = new Cookie(IterKeys.COOKIE_NAME_SOCIAL_REDIRECT, referer);
		    cookie.setPath("/");
		    cookie.setMaxAge(-1);
			response.addCookie(cookie);
		}
	}
	
	public static String getSocialFieldNameByProfile(Document socialProfileFields, String profileField)
	{
		return XMLHelper.getTextValueOf(socialProfileFields, "//row[@profilefieldname='" + profileField + "']/@socialfieldname");
	}
	
	public static String getUsernameBySocial(String socialName, JSONObject user) throws ServiceError
	{
		String usrname = invokeFieldGetterMethod(socialName, SocialNetworkUtil.XYZ_FIELD_USERNAME_ZYX, user, false);

		ErrorRaiser.throwIfFalse(Validator.isNotNull(usrname), IterErrorKeys.XYZ_ITR_E_SOCIAL_NETWORK_NO_USERNAME_FOUND_ZYX);
		
		return usrname;
	}
	
	public static String getEmailBySocial(String socialName, JSONObject user)
	{
		return invokeFieldGetterMethod(socialName, SocialNetworkUtil.XYZ_FIELD_EMAIL_ZYX, user);
	}
	
	public static String getFieldBySocial(String socialName, String field, String defaultField, JSONObject user)
	{
		String fieldValue = invokeFieldGetterMethod(socialName, field, user);
		if(Validator.isNull(fieldValue))
			fieldValue = invokeFieldGetterMethod(socialName, defaultField, user);
		
		return fieldValue;
	}
	
	public static String invokeFieldGetterMethod(String socialName, String field, JSONObject user)
	{
		return invokeFieldGetterMethod(socialName, field, user, true);
	}
	
	public static String invokeFieldGetterMethod(String socialName, String field, JSONObject user, boolean encloseWithApostrophes)
	{
		String value = null;
		try
		{
			if(socialName.equalsIgnoreCase(SOCIAL_NAME_DISQUS))
			{
				Method method = DisqusUtil.class.getMethod(field, JSONObject.class);
				value = (String)method.invoke(DisqusUtil.class, user);
			}
			else if(socialName.equalsIgnoreCase(SOCIAL_NAME_FACEBOOK))
			{
				Method method = FacebookUtil.class.getMethod(field, JSONObject.class);
				value = (String)method.invoke(FacebookUtil.class, user);
			}
			else if(socialName.equalsIgnoreCase(SOCIAL_NAME_GOOGLEPLUS))
			{
				Method method = GooglePlusUtil.class.getMethod(field, JSONObject.class);
				value = (String)method.invoke(GooglePlusUtil.class, user);
			}
			else if(socialName.equalsIgnoreCase(SOCIAL_NAME_TWITTER))
			{
				Method method = TwitterUtil.class.getMethod(field, JSONObject.class);
				value = (String)method.invoke(TwitterUtil.class, user);
			}
		}
		catch(Exception e)
		{
			_log.trace(e);
		}

		if(Validator.isNotNull(value))
		{
			if(encloseWithApostrophes)
				value = StringPool.APOSTROPHE + StringEscapeUtils.escapeSql(value) + StringPool.APOSTROPHE;
			else
				value = StringEscapeUtils.escapeSql(value);
		}
		else
		{
			value = null;
		}
		
		return value;
	}
	
	private static String[] getEnclosedSocialId(String[] socialIds)
	{
		String[] newSocialIds = new String[4];
		if(socialIds != null)
		{
			for(int i = 0; i < socialIds.length; i++)
			{
				String currentSocialId = socialIds[i];
				if(Validator.isNotNull(currentSocialId))
					newSocialIds[i] = StringPool.APOSTROPHE + currentSocialId + StringPool.APOSTROPHE;
			}
		}
		
		return newSocialIds;
	}
	
	public static String getSocialIdsSQLFilter(String[] socialIds) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		StringBuilder query = new StringBuilder();
		if (Validator.isNotNull(socialIds[SOCIAL_NAME_DISQUS_POS]))
			query.append( String.format(" disqusid IN (%s,%s) ", 	IterUserTools.encryptGDPR_Quoted(socialIds[SOCIAL_NAME_DISQUS_POS]), 
																	socialIds[SOCIAL_NAME_DISQUS_POS])  );
		
		else if (Validator.isNotNull(socialIds[SOCIAL_NAME_FACEBOOK_POS]))
			query.append( String.format(" facebookid IN (%s,%s) ",	IterUserTools.encryptGDPR_Quoted(socialIds[SOCIAL_NAME_FACEBOOK_POS]),
																	socialIds[SOCIAL_NAME_FACEBOOK_POS]) );
		
		else if (Validator.isNotNull(socialIds[SOCIAL_NAME_GOOGLEPLUS_POS]))
			query.append( String.format(" googleplusid IN (%s,%s) ",IterUserTools.encryptGDPR_Quoted(socialIds[SOCIAL_NAME_GOOGLEPLUS_POS]),
																	socialIds[SOCIAL_NAME_GOOGLEPLUS_POS]) );
		
		else if (Validator.isNotNull(socialIds[SOCIAL_NAME_TWITTER_POS]))
			query.append( String.format(" twitterid IN (%s,%s) ", 	IterUserTools.encryptGDPR_Quoted(socialIds[SOCIAL_NAME_TWITTER_POS]),
																	socialIds[SOCIAL_NAME_TWITTER_POS]) );
		
		return query.toString();
	}
	
	public static String[] socialIdsMerged(String[] socialIds, Document isAlreadyRegister) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		String[] socialDataBaseIds = new String[4];
		socialDataBaseIds[SOCIAL_NAME_DISQUS_POS] 		= IterUserTools.decryptGDPR( XMLHelper.getTextValueOf(isAlreadyRegister, "rs/row/@disqusid") );
		socialDataBaseIds[SOCIAL_NAME_FACEBOOK_POS] 	= IterUserTools.decryptGDPR( XMLHelper.getTextValueOf(isAlreadyRegister, "rs/row/@facebookid") );
		socialDataBaseIds[SOCIAL_NAME_GOOGLEPLUS_POS] 	= IterUserTools.decryptGDPR( XMLHelper.getTextValueOf(isAlreadyRegister, "rs/row/@googleplusid") );
		socialDataBaseIds[SOCIAL_NAME_TWITTER_POS] 		= IterUserTools.decryptGDPR( XMLHelper.getTextValueOf(isAlreadyRegister, "rs/row/@twitterid") );
		
		socialDataBaseIds = getEnclosedSocialId(socialDataBaseIds);
		
		if(Validator.isNotNull(socialIds[SOCIAL_NAME_DISQUS_POS]))
			socialDataBaseIds[SOCIAL_NAME_DISQUS_POS] = socialIds[SOCIAL_NAME_DISQUS_POS];
		
		if(Validator.isNotNull(socialIds[SOCIAL_NAME_FACEBOOK_POS]))
			socialDataBaseIds[SOCIAL_NAME_FACEBOOK_POS] = socialIds[SOCIAL_NAME_FACEBOOK_POS];
		
		if(Validator.isNotNull(socialIds[SOCIAL_NAME_GOOGLEPLUS_POS]))
			socialDataBaseIds[SOCIAL_NAME_GOOGLEPLUS_POS] = socialIds[SOCIAL_NAME_GOOGLEPLUS_POS];
		
		if(Validator.isNotNull(socialIds[SOCIAL_NAME_TWITTER_POS]))
			socialDataBaseIds[SOCIAL_NAME_TWITTER_POS] = socialIds[SOCIAL_NAME_TWITTER_POS];
		
		return socialDataBaseIds;
	}
	
	public static String[] socialIdsFromService(String socialName, JSONObject user)
	{
		String[] socialIds = new String[4];
		if(socialName.equalsIgnoreCase(SocialNetworkUtil.SOCIAL_NAME_DISQUS))
		{
			socialIds[SOCIAL_NAME_DISQUS_POS] 		= DisqusUtil.getSocialUserId(user);
			socialIds[SOCIAL_NAME_FACEBOOK_POS] 	= DisqusUtil.getSocialNetworkId(user, SOCIAL_NAME_FACEBOOK_POS);
			socialIds[SOCIAL_NAME_GOOGLEPLUS_POS]	= DisqusUtil.getSocialNetworkId(user, SOCIAL_NAME_GOOGLEPLUS_POS);
			socialIds[SOCIAL_NAME_TWITTER_POS] 		= DisqusUtil.getSocialNetworkId(user, SOCIAL_NAME_TWITTER_POS);
		}
		else if(socialName.equalsIgnoreCase(SocialNetworkUtil.SOCIAL_NAME_FACEBOOK))
		{
			socialIds[SOCIAL_NAME_FACEBOOK_POS] = getSocialUserId(user);
		}
		else if(socialName.equalsIgnoreCase(SocialNetworkUtil.SOCIAL_NAME_GOOGLEPLUS))
		{
			socialIds[SOCIAL_NAME_GOOGLEPLUS_POS] = getSocialUserId(user);
		}
		else if(socialName.equalsIgnoreCase(SocialNetworkUtil.SOCIAL_NAME_TWITTER))
		{
			socialIds[SOCIAL_NAME_TWITTER_POS] = getSocialUserId(user);
		}
		
		return getEnclosedSocialId(socialIds);
	}
		
	public static String getSocialUserId(JSONObject user)
	{
		return user.getString("id");
	}
	
	public static void redirectToLoginWithErrorPage(long groupId, HttpServletResponse response)
	{
		String friendlyURL = "/";
		try
		{
			Document doc = SAXReaderUtil.read(UserOperationsServiceUtil.getConfig(String.valueOf(groupId)));
			String layoutUUID = XMLHelper.getTextValueOf(doc, "rs/row/loginwitherrorlayout");
			friendlyURL = LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(layoutUUID, groupId).getFriendlyURL();
		}
		catch(Exception e)
		{
			_log.trace(e);
		}
		
		try
		{
			response.sendRedirect(friendlyURL);
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
	}
	
	public static boolean isSocialAccountBinding(String[] socialIds, Document userRegistered) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		// Recupera los IDs de redes sociales que tiene actualmente el usuario
		String currentDisqusId   = StringUtil.apostrophe( IterUserTools.decryptGDPR(XMLHelper.getTextValueOf(userRegistered, "rs/row/@disqusid")) 		);
		String currentFacebookId = StringUtil.apostrophe( IterUserTools.decryptGDPR(XMLHelper.getTextValueOf(userRegistered, "rs/row/@facebookid"))		);
		String currentGoogleId   = StringUtil.apostrophe( IterUserTools.decryptGDPR(XMLHelper.getTextValueOf(userRegistered, "rs/row/@googleplusid"))	);
		String currentTwitterId  = StringUtil.apostrophe( IterUserTools.decryptGDPR(XMLHelper.getTextValueOf(userRegistered, "rs/row/@twitterid"))		);
		
		// Recupera los nuevos IDs de redes sociales
		String disqusId   = socialIds[SOCIAL_NAME_DISQUS_POS];
		String facebookId = socialIds[SOCIAL_NAME_FACEBOOK_POS];
		String googleId   = socialIds[SOCIAL_NAME_GOOGLEPLUS_POS];
		String twitterId  = socialIds[SOCIAL_NAME_TWITTER_POS];
		
		// Se considera que es una vinculación de cuenta cuando alguno de los nuevos IDs de redes sociales
		// no es nulo y su valor difiere con el que tiene actualmente el usuario.
		return (disqusId   != null && !disqusId.equals(currentDisqusId))     ||
               (facebookId != null && !facebookId.equals(currentFacebookId)) ||
               (googleId   != null && !googleId.equals(currentGoogleId))     ||
               (twitterId  != null && !twitterId.equals(currentTwitterId));
	}
}