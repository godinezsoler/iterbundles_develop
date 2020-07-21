package com.protecmedia.iter.user.util;

import com.liferay.portal.model.UserConstants;

public class IterRegisterQueries
{
	public static String DELETE_OPTIONS_USRPROFILE_VALUES = new StringBuilder("DELETE \n" 																						).append(
			"FROM userprofilevalues \n" 																																		).append(
			"WHERE userprofilevalues.usrid = '%s' \n"																															).append(
			"	AND userprofilevalues.profilefieldid IN ( \n"																													).append(
			"												SELECT userprofile.`profilefieldid` \n"																				).append(
			"												FROM userprofile \n"																								).append(
			"												INNER JOIN formfield ON userprofile.datafieldid = formfield.datafieldid \n"											).append(
			"													WHERE ExtractValue(inputctrl, '/inputctrl/@type') IN ('radiobutton','listctrl','dropdownlist','checkbox') \n" 	).append(
			"														%s \n"																										).append(
			"											)"																														).toString();
		
			
	public static String USER_BY_EMAIL = new StringBuilder(
		" SELECT usrid, usrname, email, firstname, lastname, secondlastname, userexpires, telephone \n").append(
		" FROM iterusers 																			\n").append(
		" 	WHERE email IN ('%s', '%s')																\n").append(
		" 		AND delegationid = %d 																\n").toString();
	
	public static String USER_BY_PHONE = new StringBuilder(
		"SELECT usrid, usrname, email, firstname, lastname, secondlastname, userexpires, telephone 	\n").append(
		"FROM iterusers 																			\n").append(
		"	WHERE telephone IN ('%s', '%s')															\n").append(
		" 		AND delegationid = %d 																\n").toString();
	
	public static String GET_SMTP = " SELECT smtpserverid, host, port, enabled, tls, auth, username, password, emailfrom FROM smtpserver WHERE smtpserverid='%s' and enabled='1' ";
	
	public static final String INSERT_USER 	= 	new StringBuffer("INSERT INTO iterusers (usrid, usrname, pwd, aboid, entitlements, aboinfoexpires, " 					).append(
																"email, firstname, lastname, secondlastname, avatarurl, userexpires, " 			).append(
																"registerdate, disqusid, facebookid, googleplusid, twitterid, "					).append(
																"avatarid, delegationid) VALUES  "												).append(
																"('%s', '%s', '%s', %s,  %s,  %s, "												).append(
																  "%s,   %s,   %s,  %s,  %s,  %s, "												).append(
																  "%s, %s,   %s,   %s,  %s, "													).append(
																  "%s,  %d)"														).toString();
	
	public static final String INSERT_USER_PROFILE_VALUES 	= "INSERT INTO userprofilevalues (profilevalueid, usrid, profilefieldid, fieldvalue, binfieldvalueid) VALUES %s";
	public static final String VALUES_FOR_USER_PROFILE 		= "(ITR_UUID(), '%s', '%s', %s, %s)";
	
	public static final String DELETE_USER_PROFILE_VALUES				= "DELETE FROM userprofilevalues WHERE usrid = '%s'";
	public static final String DELETE_USER_PROFILE_VALUES_PROFILECLAUSE	= " AND profilefieldid IN ('%s')";
	
	public static final String GET_SOCIAL_CONFIG_FIELDS_BY_SOCIAL_AND_GROUP 		= 	new StringBuffer(
		"SELECT sf.fieldname socialfieldname, cf.profilefieldid, u.fieldname profilefieldname, structured 	\n").append(
		"FROM itersocialfield sf 																			\n").append(
		"INNER JOIN itersocialconfigfield cf ON sf.itersocialfieldid=cf.itersocialfieldid 					\n").append(
		"INNER JOIN itersocialconfig sc ON sc.itersocialconfigid=cf.itersocialconfigid						\n").append(
		"INNER JOIN itersocial s ON s.itersocialid=sc.itersocialid 											\n").append(
		"INNER JOIN userprofile u ON u.profilefieldid=cf.profilefieldid 									\n").append(
		" 	WHERE s.socialname='%s' 																		\n").append(
		" 		AND sc.groupid=%s 																			\n").toString();
	
	public static final String GET_USERS				 							= 	"SELECT * FROM iterusers ";
	
	public static final String GET_USER_BY_EMAIL				 					= 	GET_USERS + "WHERE email IN (%s, %s) ";
	
	public static final String GET_USER_BY_SOCIALID				 					= 	GET_USERS + "WHERE %s ";
	
	public static final String UPDATE_USER_SOCIALIDS								= 	"UPDATE iterusers SET disqusid=%s, facebookid=%s, googleplusid=%s, twitterid=%s WHERE usrid='%s'";
	
	public static final String UPDATE_ANONUSER_EXPIRES								= 	String.format(new StringBuilder(
			"UPDATE iterusers 						\n").append(
			"SET userexpires=%%s, registerdate=%%s, \n").append(
			"	 updateprofiledate=%%s, level='%s',	\n").append(
			" 	 usrname='%%s' 						\n").append(			
			"	WHERE usrid='%%s'					\n").toString(), UserConstants.USER_LEVEL_STANDARD);

	public static final String UPDATE_USER_EXPIRES									= 	"UPDATE iterusers SET userexpires=%s, registerdate=%s, updateprofiledate=%s WHERE usrid='%s'";
	
	public static final String USER_MIN_EXPIRES										= 	"SELECT MIN(userexpires) userexpires FROM iterusers";
	
	public static final String DELETE_USERS_EXPIRED				 					= 	"DELETE FROM iterusers WHERE userexpires <= '%s'";
	
	public static final String GET_USRPROFILE_FIELDS = new StringBuilder(
		"SELECT formfield.fieldid, formfield.required, IF(structured, 'system', 'user') fieldclass, \n").append(
		" 		datafield.fieldtype, userprofile.profilefieldid, userprofile.fieldname, structured  \n").append(
		"FROM userprofile                                                                           \n").append(
		"INNER JOIN datafield ON datafield.datafieldid = userprofile.datafieldid                    \n").append(
		"LEFT  JOIN formfield ON userprofile.profilefieldid = formfield.profilefieldid              \n").append(
		" 	WHERE formfield.fieldid IN %s															\n").toString();
	
	public static final String GET_USER_BY_USERID = GET_USERS + "WHERE usrid='%s'";

	public static final String CHECK_USER_BY_CREDENTIALS 	= "SELECT COUNT(*) result FROM iterusers WHERE usrid='%s' AND pwd='%s'";
	public static final String GET_USER_PWD				 	= "SELECT pwd FROM iterusers WHERE usrid='%s'";
	public static final String DELETE_USER 					= "DELETE FROM iterusers WHERE usrid='%s'";
	
	public static final String GET_REGISTRY_OTP_FIELDS = String.format(new StringBuilder(
	    "SELECT FormField.fieldid, UserProfile.fieldname									\n").append(
	    "FROM Form																			\n").append(
	    "INNER JOIN FormTab      ON FormTab.formId  = Form.formId 							\n").append(
	    "INNER JOIN FormField    ON FormField.tabId = FormTab.tabId 						\n").append(
	    "INNER JOIN UserProfile  ON UserProfile.profileFieldId = FormField.profileFieldId 	\n").append(
	    "INNER JOIN DataField    ON DataField.dataFieldId = UserProfile.dataFieldId 		\n").append(
	    "  WHERE formType = 'registro' 														\n").append(
	    "    AND UserProfile.structured 													\n").append(
	    "    AND UserProfile.fieldName IN ('%s', '%s')										\n").append(
	     "   AND Form.groupId = %%d").toString(), UserUtil.PRF_FIELD_TELEPHONE, UserUtil.PRF_FIELD_OTP_CODE);		
	
	public static final String GET_REGISTRY_FORMFIELDS = new StringBuilder(
			"SELECT 	formfield.fieldid, userprofile.fieldname \n"								).append(
			"FROM formfield \n"																		).append(
			"INNER JOIN formtab 		ON (formfield.tabid = formtab.tabid) \n"					).append(
			"INNER JOIN form 			ON (form.formid = formtab.formid) \n"						).append(
			"INNER JOIN userprofile 	ON (formfield.datafieldid = userprofile.datafieldid) \n"	).append(
			" 	WHERE form.groupId = %1$d AND form.formtype = 'registro' \n"						).append(
			" 		AND fieldname 		IN ('%2$s')").toString();
	
	public static final String GET_COUNT_FORMFIELD_REGISTRY_REQUIRED = new StringBuilder(	"SELECT COUNT(*) numregistries FROM formfield ff					\n").append(
																							"INNER JOIN userprofile up ON up.profilefieldid = ff.profilefieldid	\n").append(
																							"INNER JOIN formtab ft ON ft.tabid = ff.tabid						\n").append(
																							"INNER JOIN form f ON f.formid = ft.formid							\n").append(
																							" 	WHERE up.fieldname IN ('%s') AND ff.required=TRUE				\n").append(
																							" 		AND f.groupid=%%1$s 										\n").toString();
	
	public static final String GET_USER_FORMFIELD_REGISTRY_TO_SET_NULL = new StringBuilder("SELECT up.profilefieldid FROM userprofile up 						\n").append(
																						   "INNER JOIN formfield ff ON ff.profilefieldid = up.profilefieldid 	\n").append(
																						   "INNER JOIN formtab ft ON ff.tabid = ft.tabid 						\n").append(
																						   "INNER JOIN form f ON f.formid = ft.formid 							\n").append(
																						   "	WHERE f.groupId = %s 											\n").append(
																						   "		AND f.formtype = 'registro' AND up.structured=FALSE 		\n").append(
																						   " %s																	\n").toString();
	
	public static final String GET_ABOID 	= "SELECT aboid FROM iterusers WHERE usrid='%s'";
	public static final String UPDATE_ABOID = "UPDATE iterusers SET aboid='%s' WHERE usrid='%s'";
}
