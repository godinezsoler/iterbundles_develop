package com.protecmedia.iter.user.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.portlet.PortletPreferences;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;

import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.CookieUtil;
import com.liferay.portal.kernel.util.DateUtil;
import com.liferay.portal.kernel.util.Digester;
import com.liferay.portal.kernel.util.EncryptUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigConstants;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.IterSecureConfigTools;
import com.liferay.portal.kernel.util.IterUserTools;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.util.request.PublicIterParams;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.model.UserConstants;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.CKEditorUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.MailUtil;
import com.protecmedia.iter.base.service.util.PayCookieUtil;
import com.protecmedia.iter.user.LoginException;
import com.protecmedia.iter.user.service.LoginServiceUtil;
import com.protecmedia.iter.user.service.UserOperationsLocalServiceUtil;
import com.protecmedia.iter.user.servlet.FormReceiver;
import com.protecmedia.iter.user.util.social.SocialNetworkUtil;

public class UserUtil
{
	private static Log _log = LogFactoryUtil.getLog(UserUtil.class);
	
	public static final String MIMETYPE_HTML_UTF8 =  "text/html; charset=utf-8";
	
	public static final int USER_EXPIRES_DEFAULT_HOURS = 24;
	
	private static final String INSERT_USER_READER = new StringBuilder(
		"INSERT INTO iterusers_readers (groupid, usrid, readerid) 	\n").append(
	    "VALUES (%d, '%s', '%s')									\n").append(
	    "ON DUPLICATE KEY UPDATE readerid = VALUES(readerid)		\n").toString();
		    

	//Profile fields
	public static final String PRF_FIELD_USRID				= "usrid";
	public static final String PRF_FIELD_ABOINFOEXPIRES		= "aboinfoexpires";
	public static final String PRF_FIELD_USEREXPIRES		= "userexpires";
	public static final String PRF_FIELD_REGISTERDATE		= "registerdate";
	public static final String PRF_FIELD_DELEGATIONID		= "delegationid";
	public static final String PRF_FIELD_LEVEL				= "level";
	
	public static final String PRF_FIELD_DISQUSID			= "disqusid";
	public static final String PRF_FIELD_FACEBOOKID			= "facebookid";
	public static final String PRF_FIELD_GOOGLEPLUSID		= "googleplusid";
	public static final String PRF_FIELD_TWITTERID			= "twitterid";
	public static final String PRF_FIELD_EXTID				= "extid";
	
	public static final String PRF_FIELD_ENTITLEMENTS		= "entitlements";
	public static final String PRF_FIELD_USRNAME			= "usrname";
	public static final String PRF_FIELD_USRPWD				= "pwd";
	public static final String PRF_FIELD_USREMAIL			= "email";
	public static final String PRF_FIELD_USR1STNAME			= "firstname";
	public static final String PRF_FIELD_USRLASTNAME		= "lastname";
	public static final String PRF_FIELD_USRLASTNAME2		= "secondlastname";
	public static final String PRF_FIELD_AVATARURL			= "avatarurl";
	public static final String PRF_FIELD_AVATARID			= "avatarid";
	public static final String PRF_FIELD_ABOID				= "aboid";
	public static final String PRF_FIELD_PWD_CHALLENGE		= "pwd_challenge";
	public static final String PRF_FIELD_PWD_RESPONSE		= "pwd_response";
	public static final String PRF_FIELD_EXTRAVALIDATOR		= "XYZ_FIELD_EXTRAVALIDATOR_ZYX";
	public static final String PRF_FIELD_NEWSLETTER         = "newsletter";
	
	public static final String PRF_FIELD_AGE			    = "age";
	public static final String PRF_FIELD_BIRTHDAY   	    = "birthday";
	public static final String PRF_FIELD_GENDER      	    = "gender";
	public static final String PRF_FIELD_MARITALSTATUS      = "maritalstatus";
	public static final String PRF_FIELD_LANGUAGE		    = "language";
	public static final String PRF_FIELD_COORDINATES        = "coordinates";
	public static final String PRF_FIELD_COUNTRY      	    = "country";
	public static final String PRF_FIELD_REGION             = "region";
	public static final String PRF_FIELD_CITY          		= "city";
	public static final String PRF_FIELD_ADDRESS            = "address";
	public static final String PRF_FIELD_POSTALCODE         = "postalcode";
	public static final String PRF_FIELD_TELEPHONE          = "telephone";
	public static final String PRF_FIELD_DNI			    = "dni";
	
	public static final String PRF_FIELD_OTP_BUTTON         = UserConstants.PRF_FIELD_OTP_BUTTON;
	public static final String PRF_FIELD_OTP_CODE         	= UserConstants.PRF_FIELD_OTP_CODE;
	
	public static final List<String> PRF_COOKIE_FIELDS = Arrays.asList(new String[]
	{
			PRF_FIELD_USRNAME, PRF_FIELD_USREMAIL, PRF_FIELD_ABOID, PRF_FIELD_USR1STNAME, PRF_FIELD_USRLASTNAME, PRF_FIELD_USRLASTNAME2,
			PRF_FIELD_AVATARURL, PRF_FIELD_AVATARID, PRF_FIELD_AGE, PRF_FIELD_BIRTHDAY, PRF_FIELD_GENDER, PRF_FIELD_MARITALSTATUS, PRF_FIELD_TELEPHONE,
			PRF_FIELD_LANGUAGE, PRF_FIELD_COORDINATES, PRF_FIELD_COUNTRY, PRF_FIELD_REGION, PRF_FIELD_CITY, PRF_FIELD_ADDRESS, PRF_FIELD_POSTALCODE
	});
	
	public static final String GET_ITERUSER_CURRENT_FIELDS = "SELECT * FROM iterusers WHERE usrid = '%s'";
	
	
	
	public static final List<String> PRF_HIDE_FIELDS = Arrays.asList(new String[]
	{
		UserUtil.PRF_FIELD_EXTRAVALIDATOR, UserUtil.PRF_FIELD_NEWSLETTER,
		UserUtil.PRF_FIELD_OTP_BUTTON, UserUtil.PRF_FIELD_OTP_CODE
	});
	
	public static final Set<String> INVALID_FILTER_WITH_ENCRYPTABLE_FIELDS;
	static
	{
		Set<String> set = new HashSet<String>();
		set.add("contain");
		set.add("notcontain");
		set.add("startBy");
		set.add("endBy");
		
		INVALID_FILTER_WITH_ENCRYPTABLE_FIELDS = Collections.unmodifiableSet(set);
	}
	
	public static final Set<String> PRF_ENCRYPTABLE_FIELDS;
	static
	{
		Set<String> set = new HashSet<String>();
		set.add(PRF_FIELD_USR1STNAME);
		set.add(PRF_FIELD_USRLASTNAME);
		set.add(PRF_FIELD_USRLASTNAME2);
		set.add(PRF_FIELD_TELEPHONE);
		set.add(PRF_FIELD_DNI);
		set.add(PRF_FIELD_AVATARURL);
		set.add(PRF_FIELD_GENDER);
		set.add(PRF_FIELD_MARITALSTATUS);
		set.add(PRF_FIELD_LANGUAGE);
		set.add(PRF_FIELD_COORDINATES);
		set.add(PRF_FIELD_COUNTRY);
		set.add(PRF_FIELD_REGION);
		set.add(PRF_FIELD_CITY);
		set.add(PRF_FIELD_POSTALCODE);
		set.add(PRF_FIELD_ABOID);
		set.add(PRF_FIELD_DISQUSID);
		set.add(PRF_FIELD_FACEBOOKID);
		set.add(PRF_FIELD_GOOGLEPLUSID);
		set.add(PRF_FIELD_TWITTERID);
		set.add(PRF_FIELD_EXTID);
		set.add(PRF_FIELD_USRNAME);
		set.add(PRF_FIELD_USREMAIL);
		set.add(PRF_FIELD_ENTITLEMENTS);
		set.add(PRF_FIELD_ADDRESS);
		
		PRF_ENCRYPTABLE_FIELDS = Collections.unmodifiableSet(set);
	}
	
	public static final Map<String, Long> ITERUSERS;
	static
	{
		 Map<String, Long> map = new HashMap<String, Long>();
		 map.put(PRF_FIELD_DISQUSID, 		32L);
		 map.put(PRF_FIELD_FACEBOOKID, 		32L);
		 map.put(PRF_FIELD_GOOGLEPLUSID,	32L);
		 map.put(PRF_FIELD_TWITTERID,		32L);
		 
		 map.put(PRF_FIELD_USRNAME, 		175L);
		 map.put(PRF_FIELD_USRPWD, 			175L);
		 
		 map.put(PRF_FIELD_ABOID, 			75L);
		 map.put(PRF_FIELD_USREMAIL,		175L);
		 
		 map.put(PRF_FIELD_USR1STNAME,		75L);
		 map.put(PRF_FIELD_USRLASTNAME,		75L);
		 map.put(PRF_FIELD_USRLASTNAME2,	75L);
		 map.put(PRF_FIELD_AVATARURL,		255L);
		 
		 map.put("age",						3L);
		 map.put("birthday",				32L);
		 map.put("gender",					32L);
		 map.put("maritalstatus",			64L);
		 map.put("language",				64L);
		 map.put("coordinates",				32L);
		 map.put("country",					64L);
		 map.put("region",					64L);
		 map.put("city",					64L);
		 map.put("address",					Long.MAX_VALUE);
		 map.put("postalcode",				16L);
		 map.put(PRF_FIELD_TELEPHONE,		32L);
		 map.put(PRF_FIELD_OTP_BUTTON,		1L);
		 map.put(PRF_FIELD_OTP_CODE,		32L);
		 map.put("dni",						16L);
		 
		 ITERUSERS = Collections.unmodifiableMap(map);
	}
	

//	public static final Map<String, String> iterusersfields;
//    static
//    {
//    	iterusersfields = new HashMap<String, String>();
//    	iterusersfields.put(XYZ_FIELD_USRNAME_ZYX,			XYZ_FIELD_USRNAME_ZYX);
//    	iterusersfields.put(XYZ_FIELD_USRPWD_ZYX, 			XYZ_FIELD_USRPWD_ZYX);
//    	iterusersfields.put(XYZ_FIELD_USREMAIL_ZYX, 		XYZ_FIELD_USREMAIL_ZYX);
//    	iterusersfields.put(XYZ_FIELD_USR1STNAME_ZYX, 		XYZ_FIELD_USR1STNAME_ZYX);
//    	iterusersfields.put(XYZ_FIELD_USRLASTNAME_ZYX, 		XYZ_FIELD_USRLASTNAME_ZYX);
//    	iterusersfields.put(XYZ_FIELD_USRLASTNAME2_ZYX, 	XYZ_FIELD_USRLASTNAME2_ZYX);
//    	iterusersfields.put(XYZ_FIELD_AVATARURL_ZYX, 		XYZ_FIELD_AVATARURL_ZYX);
//    	iterusersfields.put(XYZ_FIELD_ABOID_ZYX, 			XYZ_FIELD_ABOID_ZYX);
//    	iterusersfields.put(XYZ_FIELD_PWD_CHALLENGE_ZYX,	XYZ_FIELD_PWD_CHALLENGE_ZYX);
//    	iterusersfields.put(XYZ_FIELD_PWD_RESPONSE_ZYX, 	XYZ_FIELD_PWD_RESPONSE_ZYX);
//    	iterusersfields.put(XYZ_FIELD_EXTRAVALIDATOR_ZYX, 	XYZ_FIELD_EXTRAVALIDATOR_ZYX);
//    }
    
    //Group config
    public static final String USER_NAME_EXISTS_MSG 			= "usernameexistsmsg";
    public static final String ABOID_EXISTS_MSG					= "aboidexistsmsg";
    public static final String EMAIL_EXISTS_MSG 				= "emailexistsmsg";
    public static final String REGISTER_MAIL_SUBJ 				= "registermailsubj";
    public static final String REGISTER_CONFIRMATION_MODE		= "registerconfirmationmode";
    public static final String REGISTER_HTML_MAIL 				= "registerhtmlmail";
    public static final String REGISTER_SMTP 					= "registersmptserverid";
    public static final String REGISTER_SUCCESS_LAYOUT			= "registersuccesslayout";
    public static final String REGISTER_SUCCESS_LAYOUT_NAME		= "registersuccesslayoutname";
    public static final String REGISTER_ERROR_LAYOUT 			= "registererrorlayout";
    public static final String REGISTER_ERROR_LAYOUT_NAME		= "registererrorlayoutname";
    public static final String LOGINWITH_ERROR_LAYOUT 			= "loginwitherrorlayout";
    public static final String LOGINWITH_ERROR_LAYOUT_NAME		= "loginwitherrorlayoutname";
    public static final String ENTER_EMAIL_MSG 					= "entermailmsg";
    public static final String FORGET_MSG 						= "forgetmsg";
    public static final String FORGET_USRNAME_MSG 				= "forgetusernamemsg";
    public static final String RESET_CREDENTIALS_MSG 			= "resetcredentialsmsg";
    public static final String EMAIL_NOT_EXISTS_MSG 			= "emailnoexistsmsg";
    public static final String FORGET_SMTP 						= "forgetsmptserverid";
    public static final String FORGET_MAIL_SUBJ 				= "forgetmailsubj";
    public static final String FORGET_HTML_EMAIL 				= "forgethtmlmail";
    public static final String FORGET_SUCCESS_HTML 				= "forgetsuccesshtml";
    public static final String FOGET_ERROR_LAYOUT 				= "forgeterrorlayout";
    public static final String FOGET_ERROR_LAYOUT_NAME			= "forgeterrorlayoutname";
    public static final String FORGET_PWD_MSG 					= "forgetpasswordmsg";
    public static final String FORGET_CHALLENGE_RESPONSE_MSG 	= "forgetchallengeresponsemsg";
    public static final String FORGET_ALERT_TITLE 				= "forgetalertitle";
    public static final String FORGET_ALERT_UNEXPECTED_MSG 		= "forgetalertunexpectedmsg";
    public static final String FORGET_ALERT_OK_BTT 				= "forgetalertokbutton";
    public static final String OTP_GENERATION_HAS_FAILED_MSG	= "otp_generation_errormsg";
    public static final String OTP_VALIDATION_HAS_FAILED_MSG	= "otp_validation_errormsg";
    public static final String OTP_SENDMSG_HAS_FAILED_MSG		= "otp_sendmsg_errormsg";
    public static final String ENTER_PHONE_MSG					= "enterphonemsg";
    public static final String PHONE_NOT_EXISTS_MSG				= "phonenoexistsmsg";
    public static final String PHONE_EXISTS_MSG					= "phoneexistsmsg";
    
    public static final String[] CDATA_COLUMNS = {	 USER_NAME_EXISTS_MSG
													,ABOID_EXISTS_MSG
													,EMAIL_EXISTS_MSG
													,REGISTER_HTML_MAIL
													,REGISTER_SMTP
													,REGISTER_SUCCESS_LAYOUT
													,REGISTER_ERROR_LAYOUT
													,LOGINWITH_ERROR_LAYOUT
													,ENTER_EMAIL_MSG
													,FORGET_MSG
													,FORGET_USRNAME_MSG
													,RESET_CREDENTIALS_MSG
													,EMAIL_NOT_EXISTS_MSG
													,FORGET_SMTP
													,FORGET_HTML_EMAIL
													,FORGET_SUCCESS_HTML
													,FOGET_ERROR_LAYOUT
													,FORGET_PWD_MSG
													,FORGET_CHALLENGE_RESPONSE_MSG
													,FORGET_ALERT_TITLE
													,FORGET_ALERT_UNEXPECTED_MSG
													,FORGET_ALERT_OK_BTT
													,REGISTER_MAIL_SUBJ
													,FORGET_MAIL_SUBJ
													,OTP_GENERATION_HAS_FAILED_MSG
													,OTP_VALIDATION_HAS_FAILED_MSG
													,OTP_SENDMSG_HAS_FAILED_MSG
													,REGISTER_CONFIRMATION_MODE
													,ENTER_PHONE_MSG
													,PHONE_NOT_EXISTS_MSG
													,PHONE_EXISTS_MSG
												};
    
    public static final String[] extraCDATAs = {
											    	 REGISTER_SUCCESS_LAYOUT_NAME
													,REGISTER_ERROR_LAYOUT_NAME
													,LOGINWITH_ERROR_LAYOUT_NAME
													,FOGET_ERROR_LAYOUT_NAME
											    };
    
    
    //Variables para correos de confirmación y olvido de credenciales
    public static final String PROCESSLINK	= "processlink";
    public static final String USRNAME		= "usrname";
    public static final String USREMAIL		= "usremail";
    public static final String USR1STNAME	= "usr1stname";
    public static final String USRLASTNAME	= "usrlastname";
    public static final String USRLASTNAME2	= "usrlastname2";
    
    public static final String SUCCESS_SEND = "successsend";
    public static final String ERROR_SEND = "errorsend";
    
    public static final String GET_USER_PROFILE_INCOMPLETE_REQUIRED_FIELDS = new StringBuilder(
    	"SELECT * 												\n").append(
    	"FROM	( 												\n").append(
    	"			%s											\n").append(
    	"		) result1 										\n").append(
    	"	WHERE required=TRUE 								\n").append(
    	"		AND (defaultvalue IS NULL OR defaultvalue='')	\n").toString();		
    

    private static final String _GET_USER_PROFILE_FIELDS = new StringBuilder(
    	"SELECT 																																		\n").append(
    	"  CONCAT(\"																																	\n").append( 	
		"		SELECT 	structured, formfield.fieldid, formtab.tabid, formtab.name tabname, formfield.required, datafield.fieldtype, 					\n").append( 
		"				userprofile.fieldname, IF (binfieldvalueid, CAST(CONCAT(\'/documents/\', DLFileEntry.groupId,\'/\',								\n").append(
		"				DLFileEntry.folderId,\'/\',DLFileEntry.title) AS CHAR CHARACTER SET utf8), fieldvalue )  defaultvalue							\n").append(
		"		FROM formfield																															\n").append(
		"		INNER JOIN formtab 			ON (formfield.tabid = formtab.tabid)																		\n").append(
		"		INNER JOIN form 			ON (form.formid = formtab.formid)																			\n").append(
		"		INNER JOIN userprofile 		ON (formfield.profilefieldid = userprofile.profilefieldid)													\n").append(
		"		INNER JOIN datafield 		ON (userprofile.datafieldid = datafield.datafieldid)														\n").append(
		"		LEFT JOIN userprofilevalues ON (userprofile.profilefieldid = userprofilevalues.profilefieldid AND userprofilevalues.usrid = \'%%1$s\')	\n").append(
		"		LEFT JOIN DLFileEntry		ON (DLFileEntry.fileEntryId = userprofilevalues.binfieldvalueid)											\n").append(
		"			WHERE form.groupId = %%2$d AND form.formtype = \'%1$s\' 																			\n").append(
		"				%5$s																															\n").append(
		"				AND	structured = FALSE																											\n").append(
		"																																				\n").append(
		"		UNION ALL 																																\n").append(
		"																																				\n").append(
    	"		SELECT  structured, formfield.fieldid, formtab.tabid, formtab.name tabname,																\n").append(
		"				-- El XtraValidator solo será requerido si el Aboid también lo es 																\n").append(
		"				IF (userprofile.fieldname = \'%2$s\',																							\n").append( 
		"					formfield.required * IFNULL((	SELECT formfield.required 																	\n").append(
		"													FROM userprofile 																			\n").append(
		"													INNER JOIN formfield 	ON (formfield.datafieldid = userprofile.datafieldid) 				\n").append( 
		"													INNER JOIN formtab 		ON (formfield.tabid = formtab.tabid) 								\n").append(
		"													INNER JOIN form 		ON (form.formid = formtab.formid) 									\n").append(
		"														WHERE form.groupId = %%2$d AND form.formtype = \'%1$s\' 								\n").append(
		"															AND fieldname = \'%3$s\'), 0), 														\n").append(
		"					formfield.required) required, datafield.fieldtype,																			\n").append(
    	" 				userprofile.fieldname,																											\n").append(
    	"					(CASE userprofile.fieldname 																								\n").append(
    	"						\",	group_concat(caseQuery separator '\n'),																				\n").append(
    	"					\"END) defaultvalue																											\n").append(
    	"		FROM formfield 																															\n").append(
    	"		INNER JOIN userprofile 		ON (formfield.profilefieldid = userprofile.profilefieldid) 													\n").append(
    	"		INNER JOIN datafield 		ON (userprofile.datafieldid = datafield.datafieldid)	 													\n").append(
    	"		INNER JOIN formtab 			ON (formfield.tabid = formtab.tabid)																		\n").append(
    	"		INNER JOIN form 			ON (form.formid = formtab.formid)																			\n").append(
    	"		LEFT JOIN iterusers 		ON (iterusers.usrid = \'%%1$s\') 																			\n").append(
    	"			WHERE form.groupId = %%2$d AND form.formtype = \'%1$s\' 																			\n").append(
    	"				%5$s																															\n").append(					
    	"				AND structured = TRUE 																											\n").append(
		"  \") query																																	\n").append(
		"FROM (																																			\n").append(
		"		select concat('   WHEN \"', userprofile.fieldname, '\" THEN ',																			\n").append(
		"%4$s																																			\n").append(		
		"						' ') caseQuery 																											\n").append(
		"		FROM formfield																															\n").append(
		"		INNER JOIN userprofile 			ON (formfield.profilefieldid = userprofile.profilefieldid)												\n").append(
		"		INNER JOIN formtab 				ON (formfield.tabid = formtab.tabid)																	\n").append(
		"		INNER JOIN form 					ON (form.formid = formtab.formid)																	\n").append(
		"			WHERE form.groupId = %%2$d AND form.formtype = '%1$s' 																				\n").append(
		"				%5$s																															\n").append(				
		"				AND structured = TRUE																											\n").append(
    	"																																				\n").append(		  
		"       UNION ALL																																\n").append(
		"       -- ITER-1161 Cuando todos los campos pendientes son NO ESTRUCTURADOS es necesario incluir un WHEN en CaseQuery 							\n").append(
		"       -- para que se ejecute el SELECT superior devuelva la consulta que se ejecuta posteriormente  						 					\n").append(
		"		SELECT '   WHEN \"XYZ_NOT_STRUCTURED_ZYX\" THEN \"\" ' caseQuery 																		\n").append(
		" 																																				\n").append(
		"	) CaseQuery																																	\n").toString(); 
				
    private static final String EMPTY_PROFILE_FIELDS 				= String.format(new StringBuilder(
        	"						-- Nunca se envia el pwd, ni los campos OTPs													\n").append(														
        	"						IF (userprofile.fieldname IN ('%s','%s','%s','%s'), '\"\"', concat('iterusers.', fieldname)),	\n").toString(),
        	UserUtil.PRF_FIELD_ABOID, UserUtil.PRF_FIELD_USRPWD, UserUtil.PRF_FIELD_OTP_CODE, UserUtil.PRF_FIELD_OTP_BUTTON);
        
    private static final String FIND_BY_FIELD = "				AND	formfield.fieldid IN (%3$s)";
    
    private static final String GET_USER_PROFILE_FIELDS 			= String.format(_GET_USER_PROFILE_FIELDS, 
    																		"registro", UserUtil.PRF_FIELD_EXTRAVALIDATOR, UserUtil.PRF_FIELD_ABOID,
    																		EMPTY_PROFILE_FIELDS, FIND_BY_FIELD);
    private static final String GET_USER_PROFILE_INCOMPLETEFIELDS	= String.format(_GET_USER_PROFILE_FIELDS, 
    																		"registro", UserUtil.PRF_FIELD_EXTRAVALIDATOR, UserUtil.PRF_FIELD_ABOID,
    																		"concat('iterusers.', fieldname), \n","");
			
	public static void addUserIdDogtagCookie(HttpServletRequest request, String userId,String userExpires, String intent)throws ServiceError, InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		addUserIdDogtagCookie(request, userId, userExpires, intent, JSONFactoryUtil.createJSONObject());
	}

	public static void addUserIdDogtagCookie(HttpServletRequest request, String userId, String userExpires, String intent, JSONObject extraData)throws ServiceError, InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		String version = "0";
		String userIdDogtag = createUserIdDogtagToken(version, userExpires, userId, intent, extraData);
		
		HttpServletRequest originalRequest = PortalUtil.getOriginalServletRequest(request);
		PublicIterParams.set(originalRequest, IterKeys.COOKIE_NAME_USERID_DOGTAG, userIdDogtag);
	}
	
	public static String createUserIdDogtagToken(String version, String expires, String usrid, String intent, JSONObject extraData) throws ServiceError, InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		ErrorRaiser.throwIfNull(usrid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(intent, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(extraData, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String userExpiresCookie = GetterUtil.getString(expires, StringPool.BLANK);

		ErrorRaiser.throwIfNull(extraData, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		StringBuilder value = new StringBuilder( version )
								.append(IterKeys.PARAM_SEPARATOR)
								.append(userExpiresCookie)
								.append(IterKeys.PARAM_SEPARATOR)
								.append(usrid)
								.append(IterKeys.PARAM_SEPARATOR)
								.append(intent)
								.append(IterKeys.PARAM_SEPARATOR)
								.append(extraData.toString())
								.append(IterKeys.PARAM_SEPARATOR);

		_log.debug("Plain value without MD5: " + value.toString());

		String encoded = EncryptUtil.encrypt(value.append(EncryptUtil.digest(value.toString())).toString());
		
		return URLEncoder.encode(encoded, Digester.ENCODING);
	}
	
   //**************************************************************************************//
  //	Actualmente la funcionalidad de estos 2 métodos está cubierta por otros que usan  //
 //	    atributos en el request en vez de cookies en el response						 //
//**************************************************************************************//
	
//	public static void addUserIdDogtagCookie(HttpServletResponse response, String userId,String userExpires, String intent)throws ServiceError, InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
//	{
//		addUserIdDogtagCookie(response, userId, userExpires, intent, JSONFactoryUtil.createJSONObject());
//	}
	
//	public static void addUserIdDogtagCookie(HttpServletResponse response, String userId,String userExpires, String intent, JSONObject extraData)throws ServiceError, InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
//	{
//		ErrorRaiser.throwIfNull(userId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
//		ErrorRaiser.throwIfNull(intent, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
//		ErrorRaiser.throwIfNull(extraData, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
//
//		String userExpiresCookie = GetterUtil.getString(userExpires, StringPool.BLANK);
//
//		ErrorRaiser.throwIfNull(extraData, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
//
//		String version =  "0";
//		StringBuilder value = new StringBuilder( version )
//								.append(IterKeys.PARAM_SEPARATOR)
//								.append(userExpiresCookie)
//								.append(IterKeys.PARAM_SEPARATOR)
//								.append(userId)
//								.append(IterKeys.PARAM_SEPARATOR)
//								.append(intent)
//								.append(IterKeys.PARAM_SEPARATOR)
//								.append(extraData.toString());
//
//		_log.debug("Plain value without MD5: " + value.toString());
//
//		String encoded = EncryptUtil.encrypt(value.append(EncryptUtil.digest(value.toString())).toString());
//
//		Cookie cookie = new Cookie(IterKeys.COOKIE_NAME_USERID_DOGTAG, URLEncoder.encode(encoded, Digester.ENCODING));
//		cookie.setPath("/");
//		cookie.setMaxAge(-1);
//
//		response.addCookie(cookie);
//	}
	
	public static void redirectToEditProfilePage(long groupId, HttpServletRequest request, HttpServletResponse response, boolean isMobile) throws IOException, PortalException, SystemException 
	{
		try
		{
			if(_log.isDebugEnabled())
				_log.debug( String.format("redirectToEditProfilePage params: groupId=%s, isMobile=%s", groupId, isMobile) );
			
			LoginUtil lu = new LoginUtil();
			lu.getLoginConfig(groupId);

			String editprofilepage = lu.getValueOfLoginPreference("editprofilepage");
			
			if(_log.isDebugEnabled())
				_log.debug( String.format("editprofilepageUUID=%s", editprofilepage) );
			
			long plid = LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(editprofilepage, groupId).getPlid();
			
			if(_log.isDebugEnabled())
				_log.debug( String.format("editprofilepage plid=%s", plid) );

			ServletContext rootContext = (ServletContext)request.getAttribute(IterKeys.REQUEST_ATTRIBUTE_ROOT_CONTEXT);
			
			if(_log.isDebugEnabled())
				_log.debug( String.format("rootContext is null? %s", Validator.isNull(rootContext)) );
						
			// Se marca la petición con un attribute que sirva de indicador para 
			// que el portlet de edición pueda pintar datos privados del usuario.
			HttpServletRequest originalRequest = PortalUtil.getOriginalServletRequest(request);
			PublicIterParams.set(originalRequest, IterKeys.REQUEST_ATTRIBUTE_IS_FORWARDED_PAGE, "true");
			
			IterRequest.setAttribute(WebKeys.IS_MOBILE_REQUEST, isMobile);
			
			RequestDispatcher rd = rootContext.getRequestDispatcher("/c/portal/layout?p_l_id=" + plid);
			
			if(_log.isDebugEnabled())
				_log.debug( String.format("RequestDispatcher is null? %s", Validator.isNull(rd)) );
			
			rd.forward(request, response);	
		}
		catch(Exception e)
		{
			try
			{
				_log.error(e);
				SocialNetworkUtil.redirectToLoginWithErrorPage(groupId, response);
			}
			catch(Exception e2)
			{
				_log.error(e2);
				response.sendRedirect("/");
			}
		}
	}
	
	public static void redirectToCookieReferer(HttpServletRequest request, HttpServletResponse response) throws PortalException, SystemException, ServletException, IOException
	{
		StringBuilder ssoRedirection = new StringBuilder();
		
		// Recupera la cookie
		String sessioncookie = null;
		int sessionexpires = 0;
		Cookie[] cookies = CookieUtil.getCookies(request, new String[]{IterKeys.COOKIE_NAME_USER_DOGTAG});
		if (cookies.length > 0)
		{
			Cookie userDogtag = cookies[0];
			sessioncookie = userDogtag.getValue();
			sessionexpires = userDogtag.getMaxAge();
		}
		
		// Si hay cookie...
		if (Validator.isNotNull(sessioncookie))
		{
			LoginUtil lu = new LoginUtil();
			lu.getLoginConfig( PortalUtil.getScopeGroupId(request) );
			try
			{	
				// Recupera las preferencias de SSO
				String ssopartnersPreference = lu.getValueOfLoginPreference("ssopartners");
				if (Validator.isNotNull(ssopartnersPreference))
				{// Recupera el propio host
					String ownUrl = IterRequest.getOriginalRequest().getScheme() + "://" + IterRequest.getOriginalRequest().getServerName();
					
					// Procesa la url a la que hay que volver
					String origin = request.getHeader("Referer");
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
					ssoRedirection.append("sessioncookie=").append(sessioncookie).append(StringPool.AMPERSAND);
					ssoRedirection.append("sessionexpires=").append(sessionexpires).append(StringPool.AMPERSAND);
					ssoRedirection.append("userid=").append(PayCookieUtil.getUserId(request)).append(StringPool.AMPERSAND);
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
		}
		
		redirectToCookieReferer(request, response, ssoRedirection.toString());
	}
	
	public static void redirectToCookieReferer(HttpServletRequest request, HttpServletResponse response, String info) throws PortalException, SystemException, ServletException, IOException
	{
		String friendlyURL = "/";
		try
		{
			String redirectURLString = CookieUtil.get(request, IterKeys.COOKIE_NAME_SOCIAL_REDIRECT);
			
			if (Validator.isNotNull(info))
			{
				try
				{
					info = StringPool.OPEN_CURLY_BRACE.concat(info).concat(StringPool.CLOSE_CURLY_BRACE);
					JSONObject json = JSONFactoryUtil.createJSONObject(info);
					String ssoUrl = json.getString("sso");
					if (Validator.isNotNull(ssoUrl))
						friendlyURL = ssoUrl;
					else
						friendlyURL = new URL(redirectURLString).getPath();
				}
				catch (Throwable th)
				{
					_log.error(th);
				}
			}
			else
			{
				if(_log.isDebugEnabled())
					_log.debug("redirectToCookieReferer. Referer: " + redirectURLString);
				
				friendlyURL = new URL(redirectURLString).getPath();
			}
		}
		catch(Exception e)
		{
			_log.error(e);
		}
		
		CookieUtil.deleteCookies( request, response,  new String[]{IterKeys.COOKIE_NAME_SOCIAL_REDIRECT} );
		
		response.sendRedirect(friendlyURL);
	}
	
	public static String getServletURL(HttpServletRequest request, String servlet) throws MalformedURLException, PortalException, SystemException, UnsupportedEncodingException
	{
		return getServletURL(request, servlet, null);
	}
	
	public static String getServletURL(HttpServletRequest request, String servlet, String token) throws MalformedURLException, PortalException, SystemException, UnsupportedEncodingException
	{
		String host = PortalUtil.getPortalURL(request);
		
		host += servlet;
		
		if( Validator.isNotNull(token) && !token.isEmpty() )
		{
			host = String.format(host, token);
		}
		
		return host;
	}
	
	public static Document getUserByPhone( String phone, long delegationId ) throws SecurityException, NoSuchMethodException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		phone = phone.trim();
		
		String sql = String.format( IterRegisterQueries.USER_BY_PHONE, 
						IterUserTools.encryptGDPR(phone), phone, delegationId );
		
		return PortalLocalServiceUtil.executeQueryAsDom( sql );
	}
	
	public static Document getUser( String email, long delegationId ) throws SecurityException, NoSuchMethodException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		email = StringEscapeUtils.escapeSql( cleanEmail(email) );
		
		String query = String.format( IterRegisterQueries.USER_BY_EMAIL, 
						IterUserTools.encryptGDPR(email), email, delegationId );
		
		return PortalLocalServiceUtil.executeQueryAsDom( query );
	}
	
	public static Date getUserExpires(long groupid) throws Exception
	{
		Date expiresTime = null;
		
		String mode = GroupConfigTools.getGroupConfigFieldFromDB(groupid, GroupConfigConstants.FIELD_REGISTER_CONFIRMATION_MODE);
		if (GroupConfigConstants.REGISTER_CONFIRMATION_MODE.email.toString().equals(mode))
		{
			Calendar cal = Calendar.getInstance();
			
			String grpFriendly = GroupLocalServiceUtil.getGroup(groupid).getFriendlyURL().replace(StringPool.SLASH, StringPool.BLANK);
			String userExpiresProp = String.format(IterKeys.PORTAL_PROPERTIES_KEY_ITER_USER_EXPIRES, grpFriendly);
			
			String userExpiresString = GetterUtil.getString(
					PortalUtil.getPortalProperties().getProperty(userExpiresProp, String.valueOf(USER_EXPIRES_DEFAULT_HOURS)));
			
			if( !userExpiresString.equals("0") )
			{
				cal.add(Calendar.HOUR, Integer.valueOf(userExpiresString));
				expiresTime = cal.getTime();
			}
		}
		
		return expiresTime;
	}
	
	public static String getUserExpiresDateFormated( long groupid ) throws Exception
	{
		String expiresDateFormatted = StringPool.NULL;
		Date d = getUserExpires(groupid);
		
		if( Validator.isNotNull(d) )
			expiresDateFormatted = DateUtil.getDBFormat( d );
		
		return expiresDateFormatted;
	}
	
	public static String getInfiniteUserExpiresDateFormated( long groupid ) throws Exception
	{
		String expiresDateFormatted = StringPool.NULL;
		Date d = getUserExpires(groupid);
		
		if( Validator.isNotNull(d) )
		{
			// Se le suman 999 años a la fecha para que el usuario no expire nunca.
			// Solo se usa en el caso de una actualización del correo del usuario ya registrado, 
			//para que nuca se borre con la tarea automatica de eliminación de usuarios no confirmados 
			long t = d.getTime()+31536000000000L;
			expiresDateFormatted = DateUtil.getDBFormat( new Date(t) );
		}
		
		return expiresDateFormatted;
	}
	
	public static String getCopleteProfileExpiresDateFormated( long groupid ) throws ParseException, PortalException, SystemException
	{
		String expiresDateFormatted = StringPool.NULL;

		Date expiresTime = null;
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, 5);
		expiresTime = cal.getTime();
		
		if(Validator.isNotNull(expiresTime))
			expiresDateFormatted = DateUtil.getDBFormat(expiresTime);
		
		return expiresDateFormatted;
	}
	
	public static String getResetCredentialsExpiresToken( long groupid ) throws PortalException, SystemException
	{
		String expiresTime = StringPool.NULL;
		Calendar cal = Calendar.getInstance();
		
		String grpFriendly = GroupLocalServiceUtil.getGroup(groupid).getFriendlyURL().replace(StringPool.SLASH, StringPool.BLANK);
		String userExpiresProp = String.format(IterKeys.PORTAL_PROPERTIES_KEY_ITER_USER_RESET_CREDENTIALS_TIME, grpFriendly);
		
		String userExpiresString = GetterUtil.getString(
				PortalUtil.getPortalProperties().getProperty(userExpiresProp, String.valueOf(USER_EXPIRES_DEFAULT_HOURS)));
		
		if( userExpiresString.equals("0") )
			userExpiresString = String.valueOf(USER_EXPIRES_DEFAULT_HOURS);
			
		cal.add(Calendar.HOUR, Integer.valueOf(userExpiresString));
		expiresTime = String.valueOf( cal.getTimeInMillis() );
		
		return expiresTime;
	}

	public static void sendMail(String smtpServerId, List<String> email, String subject, String body, String mimeType) throws SecurityException, NoSuchMethodException, AddressException, MessagingException, ServiceError, IOException
	{
		String query = String.format(IterRegisterQueries.GET_SMTP, smtpServerId);
		Document smtpDom = PortalLocalServiceUtil.executeQueryAsDom(query);
		XPath xpath = SAXReaderUtil.createXPath("/rs/row");
		Node smtpNode = xpath.selectSingleNode(smtpDom);
		
		MailUtil.sendEmail(smtpNode, subject, body, mimeType, email, RecipientType.TO);
		
	}

	public static String doLogin(HttpServletRequest request, HttpServletResponse response, String usrname, String pwd) throws PortalException, SystemException
	{
		PortalUtil.setVirtualHostLayoutSet(request);
		request.setAttribute(IterKeys.HOTCONFIG_KEY_LOGIN_ALOUMD5, true);
		return LoginServiceUtil.doLogin(usrname, pwd, true, null, request, response);
	}
	
	public static void doLoginAndRedirectToReferer(HttpServletRequest request, HttpServletResponse response, String usrname, String pwd) throws PortalException, SystemException, ServletException, IOException
	{
		//Me autentico
		String info = doLogin(request, response, usrname, pwd);
		
		//Redirijo al referer
		redirectToCookieReferer(request, response, info);
	}
	
	/**
	 * 
	 * @param groupId
	 * @param request
	 * @return Devuelve el formulario HTML para la edición del perfil del usuario
	 * @throws Exception 
	 */
	public static String getEditUserProfileHtml(long groupId, HttpServletRequest request, PortletPreferences preferences) throws Exception
	{
		String userId 	= "", html = "";
		String[] fields = null;
		
		try
		{
			HttpServletRequest originalRequest = PortalUtil.getOriginalServletRequest(request);
			boolean showPrivateInfo = GetterUtil.getBoolean((String)PublicIterParams.get(originalRequest, IterKeys.REQUEST_ATTRIBUTE_IS_FORWARDED_PAGE));
			ErrorRaiser.throwIfFalse(showPrivateInfo, IterErrorKeys.XYZ_ITR_E_USERPROFILE_INVALID_ACCESS_ZYX);
			
			// Se lee el usuario
			Map<String, String> cookieMap = getCookie4EditUserProfile(request);
			userId = cookieMap.get(IterKeys.COOKIE_NAME_USER_ID);
			
			if ( IterKeys.EDIT_PROFILE_INTENT_REGISTRY.equals(cookieMap.get(IterKeys.COOKIE_NAME_INTENT)) ||
				 IterKeys.EDIT_PROFILE_INTENT_FORGOT.equals( cookieMap.get(IterKeys.COOKIE_NAME_INTENT)) )
			{
				// Solo se incluirán los campos obligatorios que quedan por rellenar
				Document dom = getUserProfileIncompleteRequiredFields(groupId, userId); 
				
				// Se obtiene el id de todos aquellos campos obligatorios que no tienen dato
				List<Node> listRequiredFields = dom.selectNodes("//row/@fieldid");
				fields = XMLHelper.getStringValues(listRequiredFields);
				
				if( IterKeys.EDIT_PROFILE_INTENT_FORGOT.equals( cookieMap.get(IterKeys.COOKIE_NAME_INTENT)) )
				{
					// Se incluirán los campos que marque el campo EXTRADATA de la cookie
					JSONObject cookieObj 		= JSONFactoryUtil.createJSONObject( cookieMap.get(IterKeys.COOKIE_NAME_EXTRADATA) );
					List<String> forgotFields 	= new ArrayList<String>();
					
					if (cookieObj.getBoolean(UserKeys.KEY_FORGOT_USR_NAME))
						forgotFields.add(PRF_FIELD_USRNAME);
					
					if (cookieObj.getBoolean(UserKeys.KEY_FORGOT_PWD))
						forgotFields.add(PRF_FIELD_USRPWD);
					
					// Al menos uno de los dos campos tiene que estar activado
					ErrorRaiser.throwIfFalse(forgotFields.size() > 0);
					
					String [] list = forgotFields.toArray(new String[forgotFields.size()]);
					String query = String.format( IterRegisterQueries.GET_REGISTRY_FORMFIELDS, groupId, StringUtils.join(list, "','") );
					
					fields = ArrayUtil.append(fields, XMLHelper.getStringValues( PortalLocalServiceUtil.executeQueryAsDom(query).selectNodes("//row/@fieldid"), null ) );
				}
				
			}
			
			Document dom= FormUtil.getEditUserProfileFormXml(groupId, request, userId, fields, cookieMap.get(IterKeys.COOKIE_NAME_INTENT));
			html 		= FormUtil.applyXSL( dom );
		}
		catch(ServiceError e)
		{
			// Se ha accedido directamente a la página y no mediante la redirección del servlet EditUserProfile
			// o El usuario NO está registrado
			if ( e.getErrorCode().equals(IterErrorKeys.XYZ_ITR_E_USERPROFILE_INVALID_ACCESS_ZYX) ||
				 e.getErrorCode().equals(IterErrorKeys.XYZ_ITR_E_UNREGISTERED_USER_ZYX)	)
			{
				// Se ha accedido directamente a la página y no mediante la redirección del servlet EditUserProfile
				html = preferences.getValue("unregisteredUserMsg", "");

				CKEditorUtil.noInheritThemeCSS(html, request);
			}
			else
			{
				throw e;
			}
		}
		
		return html;
	}
	
	public static boolean hasAllRequieredFieldsFilled(long groupid, String usrid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(usrid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return countUserProfileIncompleteRequiredFields(groupid, usrid) == 0;
	}
	
	public static void redirectTo(HttpServletResponse response, long groupId, String groupConfigLayout)
	{
		String friendlyURL = "/";
		try
		{
			Document doc = UserOperationsLocalServiceUtil.getConfigDom(String.valueOf(groupId));
			friendlyURL = XMLHelper.getTextValueOf(doc, "rs/row/"+groupConfigLayout);
		}
		catch(Exception e)
		{
			_log.error(e);
		}
		
		try
		{
			response.sendRedirect(friendlyURL);
		}
		catch(Exception e)
		{
			_log.error(e);
		}
	}

	public static Map<String, String> getCookie4EditUserProfile(HttpServletRequest request) throws DocumentException, ServiceError, ParseException
	{
		HttpServletRequest originalRequest = PortalUtil.getOriginalServletRequest(request);
		String plainCookie = PayCookieUtil.plainCookieValue( String.valueOf(PublicIterParams.get(originalRequest, IterKeys.COOKIE_NAME_USERID_DOGTAG)) );
		Map<String, String> cookieMap = PayCookieUtil.getCookieAsMap( plainCookie, IterKeys.COOKIE_NAME_USERID_DOGTAG );
		
		if ( IterKeys.EDIT_PROFILE_INTENT_REGISTRY.equals(cookieMap.get(IterKeys.COOKIE_NAME_INTENT)) ||
			 IterKeys.EDIT_PROFILE_INTENT_FORGOT.equals(cookieMap.get(IterKeys.COOKIE_NAME_INTENT)) )
		{
			// Es una edición del perfil del usuario proveniente de una Red Social o un olvido de contraseña. 
			// Se comprueba que NO haya expirado. Si la fecha viene vacía es que NO expira
			String expireDate = cookieMap.get(IterKeys.COOKIE_NAME_EXPIRE);
			SimpleDateFormat sdf = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss);
			if (Validator.isNotNull(expireDate) && sdf.parse(expireDate).getTime() < Calendar.getInstance().getTime().getTime() )
			{
				if (StringPool.TRUE.equals( String.valueOf(PublicIterParams.get(originalRequest, IterKeys.FLAG_COMPLETE_USER_PROFILE))))
					_log.info("User register complete profile mandatory fields time expired for "+ plainCookie);
				
				cookieMap.put(IterKeys.COOKIE_NAME_USER_ID, null);
			}
		}
		else
		{
			// Si no es de edición o de olvido de contraseña, tiene que se la cookie USER_GOGTAG
			cookieMap = PayCookieUtil.getCookieAsMap(request, IterKeys.COOKIE_NAME_USER_DOGTAG);
			cookieMap.put(IterKeys.COOKIE_NAME_INTENT, IterKeys.EDIT_PROFILE_INTENT_EDITION);
		}
		
		ErrorRaiser.throwIfNull(cookieMap.get(IterKeys.COOKIE_NAME_USER_ID), IterErrorKeys.XYZ_ITR_E_UNREGISTERED_USER_ZYX);
		
		return cookieMap;
	}
	
	public static void forwardTo(HttpServletRequest request, HttpServletResponse response, long groupId, String friendlyUrlLayout) throws IOException
	{
		try
		{
			ServletContext rootContext = (ServletContext)request.getAttribute(IterKeys.REQUEST_ATTRIBUTE_ROOT_CONTEXT);
		
			if(Validator.isNotNull(friendlyUrlLayout))
			{
				long plid = LayoutLocalServiceUtil.getFriendlyURLLayout(groupId, false, friendlyUrlLayout).getPlid();
				rootContext.getRequestDispatcher("/c/portal/layout?p_l_id=" + plid).forward(request, response);
			}
			else
			{
				response.setStatus(HttpStatus.SC_MOVED_TEMPORARILY);
				response.sendRedirect("/");
			}
		}
		catch(Exception e)
		{
			_log.error(e);
			_log.debug("Error in forward to layout " + friendlyUrlLayout + " group: " + groupId);
			response.setStatus(HttpStatus.SC_MOVED_TEMPORARILY);
			response.sendRedirect("/");
		}
	}
	
	public static String cleanEmail(String email)
	{
		String retVal = email;
		
		if( Validator.isNotNull(email) && email.contains(StringPool.PLUS) && email.contains(StringPool.AT) )
		{
			String[]emailparts = email.split(StringPool.AT);
			String part1 = StringUtils.substringBefore(emailparts[0], StringPool.PLUS);
			retVal = part1.concat(StringPool.AT).concat(emailparts[1]);
		}
		
		return retVal; 
	}
	
	/**
	 * @return
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws NoSuchProviderException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws UnsupportedEncodingException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	public static Document getUserProfileFields(long groupId, String usrId, String fieldIds) throws SecurityException, NoSuchMethodException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		String sqlPrepare = String.format(GET_USER_PROFILE_FIELDS, usrId, groupId, fieldIds);
		_log.debug(sqlPrepare);
		
		String sqlFinal = PortalLocalServiceUtil.executeQueryAsList(sqlPrepare).get(0).toString();
		_log.debug(sqlFinal);
		
		Document usrProfileFields = PortalLocalServiceUtil.executeQueryAsDom(sqlFinal, new String[]{"defaultvalue"});
		if (_log.isDebugEnabled())
			_log.debug( usrProfileFields.asXML() );
		
		List<Node> values = usrProfileFields.selectNodes("//defaultvalue");
		for (Node node : values)
		{
			String oldText = node.getStringValue();
			String newText = IterUserTools.decryptGDPR(oldText);
			if (oldText != newText)
			{
				// Se borra
				((Element)node).setText("");
				// Se añade como CDATA (No hay forma de sustituir directamente el CDATA)
				((Element)node).addCDATA(newText);
			}
		}
		
		if (_log.isDebugEnabled())
			_log.debug( usrProfileFields.asXML() );

		
		return usrProfileFields;
	}
	
	/**
	 * @param groupId
	 * @param usrId
	 * @param fieldIds
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws NoSuchProviderException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws UnsupportedEncodingException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	public static Document getUserProfileIncompleteRequiredFields(long groupId, String usrId) throws SecurityException, NoSuchMethodException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		String sqlPrepare = String.format(GET_USER_PROFILE_INCOMPLETEFIELDS, usrId, groupId);
		_log.debug(sqlPrepare);

		String sqlPrepare2 = PortalLocalServiceUtil.executeQueryAsList(sqlPrepare).get(0).toString();
		_log.debug(sqlPrepare2);
		
		String sqlFinal = String.format(GET_USER_PROFILE_INCOMPLETE_REQUIRED_FIELDS, sqlPrepare2);
		_log.debug(sqlFinal);
		
		Document usrProfileFields = PortalLocalServiceUtil.executeQueryAsDom(sqlFinal, new String[]{"defaultvalue"});
		if (_log.isDebugEnabled())
			_log.debug( usrProfileFields.asXML() );
		
		List<Node> values = usrProfileFields.selectNodes("//defaultvalue");
		for (Node node : values)
		{
			String oldText = node.getStringValue();
			String newText = IterUserTools.decryptGDPR(oldText);
			if (oldText != newText)
			{
				// Se borra
				((Element)node).setText("");
				// Se añade como CDATA (No hay forma de sustituir directamente el CDATA)
				((Element)node).addCDATA(newText);
			}
		}
		
		if (_log.isDebugEnabled())
			_log.debug( usrProfileFields.asXML() );

		return usrProfileFields;
	}
	
	/**
	 * @param groupId
	 * @param usrId
	 * @param fieldIds
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 */
	public static long countUserProfileIncompleteRequiredFields(long groupId, String usrId) throws SecurityException, NoSuchMethodException
	{
		String sqlPrepare = String.format(GET_USER_PROFILE_INCOMPLETEFIELDS, usrId, groupId);
		_log.debug(sqlPrepare);

		String sqlPrepare2 = PortalLocalServiceUtil.executeQueryAsList(sqlPrepare).get(0).toString();
		_log.debug(sqlPrepare2);
		
		String sqlFinal = String.format("SELECT COUNT(*) result FROM (%s) result2", String.format(GET_USER_PROFILE_INCOMPLETE_REQUIRED_FIELDS, sqlPrepare2));
		_log.debug(sqlFinal);

		long numIncompleteFields = Long.parseLong( PortalLocalServiceUtil.executeQueryAsList(sqlFinal).get(0).toString() );
		_log.debug(numIncompleteFields);
		
		return numIncompleteFields;
	}
	
	public static void setSsoResponse(HttpServletRequest request, JSONObject jsonResponse, String sso) throws JSONException
	{
		if (IterKeys.OK.equals(jsonResponse.getString(FormReceiver.RESULT)) && jsonResponse.getJSONObject(FormReceiver.FURTHERACTION) != null)
		{
			// Recupera el destino de la respuesta
			JSONObject jsonFurtheraction = jsonResponse.getJSONObject(FormReceiver.FURTHERACTION);
			String action = jsonFurtheraction.getString(FormReceiver.ACTION);
			String location = jsonFurtheraction.getString(FormReceiver.LOCATION);
			
			// Si es una redirección, el destino será esa URL
			if (IterKeys.ACTION_REDIRECT.equals(action))
			{
				// Si es una URL interna, se le añade el host
				if (location.startsWith(StringPool.SLASH))
					location = request.getScheme().concat(Http.PROTOCOL_DELIMITER).concat(request.getServerName()).concat(location);
			}
			// Si es un refresh o un close, se redirecciona a la página del perfil
			else
			{
				location = request.getScheme().concat(Http.PROTOCOL_DELIMITER).concat(request.getServerName()).concat("/user-portlet/edit-user-profile/");
			}
			
			// Recupera la url para el SSO
			JSONObject jsonSso = JSONFactoryUtil.createJSONObject(StringPool.OPEN_CURLY_BRACE + sso + StringPool.CLOSE_CURLY_BRACE);
			String ssoUrl = jsonSso.getString("sso");
			// Sustituye el destino final del SSO en función del la configuración del formulario
			ssoUrl = ssoUrl.replaceFirst("origin=.*&", "origin=" + HttpUtil.encodeURL(location) + "&");
			
			// Sustituye la acción a realizar por la redirección SSO
			jsonFurtheraction.put(FormReceiver.ACTION, IterKeys.ACTION_REDIRECT);
			jsonFurtheraction.put(FormReceiver.LOCATION, ssoUrl);
		}
	}
	
	/**
	 * Registra el readerId para el grupo y usuario dado
	 * @throws SQLException 
	 * @throws IOException 
	 * @see ITER-1373 Version plantilla AMP para notas bajo una suscripcion
	 */
	public static void registerReaderId(long groupId, String usrId, String readerId) throws IOException, SQLException
	{
		if (groupId > 0 && Validator.isNotNull(usrId) && Validator.isNotNull(readerId))
		{
			String sql = String.format(INSERT_USER_READER, groupId, usrId, readerId);
			_log.debug(sql);
			PortalLocalServiceUtil.executeUpdateQuery(sql);
		}
	}
}
