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

package com.protecmedia.iter.base.service.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.SendFailedException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.portal.apache.ApacheHierarchy;
import com.liferay.portal.apache.ApacheUtil;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.metrics.NewsletterMASTools;
import com.liferay.portal.kernel.util.EncryptUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.IterUserTools;
import com.liferay.portal.kernel.util.MethodKey;
import com.liferay.portal.kernel.util.PortalClassInvoker;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Attribute;
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
import com.liferay.portal.service.GroupServiceUtil;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.IterMonitor.Event;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.cluster.Heartbeat;
import com.protecmedia.iter.base.metrics.NewslettersMetricsUtil;
import com.protecmedia.iter.base.scheduler.AlertNewsletterMgr;
import com.protecmedia.iter.base.scheduler.NewsletterTask;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.SMTPServerMgrLocalServiceUtil;
import com.protecmedia.iter.base.service.base.NewsletterMgrLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.scheduler.Task;
import com.protecmedia.iter.base.service.util.IterAdmin;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.MailUtil;
import com.protecmedia.iter.base.service.util.PayCookieUtil;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.base.service.util.TeaserMgr;
import com.protecmedia.iter.base.util.NewsletterTools;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

/**
 * The implementation of the newsletter mgr local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.NewsletterMgrLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.NewsletterMgrLocalServiceUtil} to access the newsletter mgr local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.NewsletterMgrLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.NewsletterMgrLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class NewsletterMgrLocalServiceImpl extends NewsletterMgrLocalServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(NewsletterMgrLocalServiceImpl.class);
	
	private static final String BREAK_LOG_NAME = "breaknewsletterloop";
	private static Log _log_break_loop = LogFactoryUtil.getLog(BREAK_LOG_NAME);
	
	private static final String FORCE_SEND_NAME = "newsletter.force_send";
	private static Log _log_force_send = LogFactoryUtil.getLog(FORCE_SEND_NAME);
	
	private static ThreadGroup schedules = new ThreadGroup("NEWSLETTER");

	private static final String DEFAULT_MAX_RECIPIENTS 	= "0";
	private static final String DEFAULT_DAYS 			= "1,2,3,4,5,6,7";
	private static final String DEFAULT_HOUR 			= "0";
	
	private static final String ROUNDROBIN 				= "roundrobin";
	
	private static final String IMPOSSIBLE_TO_SEND 		= "[IMPOSSIBLE TO SEND]";
	
	private static final String COUNT_SUBSCRIPTION 		= new StringBuilder(
		"SELECT COUNT(1) 					\n").append(
		"FROM Schedule_User 				\n").append(
		"  WHERE Schedule_User.userid = '%s'\n").toString();
	
	//NEWSLETTER
	private static final String GET_EXISTING_NEWSLETTER			= new StringBuilder(
		"SELECT newsletter.newsletterid, newsletter.name		\n").append(
		"FROM newsletter										\n").append(
		"	WHERE newsletter.name IN ('%s')	AND groupId = %d	\n").toString();
	
	private static final String DELETE_EXISTING_SCHEDULER_BY_NEWSLETTERID = new StringBuilder(
		"DELETE FROM schedule_newsletter WHERE newsletterId IN ('%s') \n").toString();
	
	private static final String GET_NEWSLETTERS 						= 	new StringBuilder(
		"SELECT n.newsletterid, n.name, n.subject, n.description, 								\n").append(
		"		n.plid, n.groupid, pt.name as pagetemplatename, Layout.friendlyURL 				\n").append(
		"FROM newsletter n 																		\n").append(
		"INNER JOIN Designer_PageTemplate pt ON (pt.layoutId=n.plid AND pt.type_='newsletter')	\n").append(
		"INNER JOIN Layout ON Layout.plid=n.plid												\n").toString();
		
	private static final String GET_NEWSLETTER 							= 	GET_NEWSLETTERS + " WHERE n.newsletterid IN ('%s')";
	private static final String GET_NEWSLETTERS_BY_GROUP 				= 	GET_NEWSLETTERS + " WHERE n.groupid=%s";
	
	private static final String ADD_NEWSLETTER							= 	"INSERT INTO newsletter(newsletterid, name, subject, description, plid, groupid, modifieddate, publicationdate) " +
																				"VALUES ('%s', '%s', %s, %s, %s, %s, '%s', NULL)";
	
	/** Si no existe un Layout con dicho plid se fuerza un error para que salte el constrain de plid y se identifique la tabla con problemas **/
	private static final String GET_PLID_FROM_FRIENDLYURL				= new StringBuilder(
		"(IFNULL((SELECT Layout.plid									\n").append(
		"FROM Layout													\n").append(
		"INNER JOIN designer_pagetemplate d ON d.layoutId = Layout.plid	\n").append(
		"	WHERE Layout.friendlyURL = '%1$s'							\n").append(
		"		AND Layout.groupId = %2$s								\n").append(
		"		AND Layout.privateLayout = FALSE						\n").append(
		"		AND d.type_ = 'newsletter'), -1))						\n").toString();
					
	private static final String UPDATE_NEWSLETTER						= 	"UPDATE newsletter SET name='%s', subject=%s, description=%s, plid=%s, modifieddate='%s' WHERE newsletterid='%s'";
	private static final String DELETE_NEWSLETTERS						= 	"DELETE FROM newsletter WHERE newsletterid IN %s";
	
	//SCHEDULE_NEWSLETTER
	private static final String GET_SCHEDULE_NEWSLETTERS				= 	new StringBuilder(
		"SELECT n.groupid, n.subject, sn.scheduleid, sn.name, sn.description, sn.newsletterid, sn.days, sn.hour, sn.servermode, \n").append(
		" 		sn.maxrecipients, n.name newsletterName, sn.name scheduleName, sn.enabled, sn.lasttriggered, sn.laststatus,  	\n").append(
		" 		sn.type, l.friendlyURL, sn.allowanonymous 	 																	\n").append(
		"FROM schedule_newsletter sn 																							\n").append(
		"INNER JOIN newsletter n ON n.newsletterid=sn.newsletterid 																\n").append(
		"INNER JOIN Layout l ON l.plid=n.plid																					\n").toString();
	
	private static final String GET_ANONYMOUS_USER 						= new StringBuilder(
		"SELECT usrId, CAST(level AS CHAR CHARACTER SET utf8) level \n").append(
		"FROM IterUsers												\n").append(
		"	WHERE email IN ('%s', '%s')								\n").append(
	    "		AND delegationId = %d								\n").toString();
	
	private static final String GET_SCHEDULE_NEWSLETTERS_DEFAULT		= 	GET_SCHEDULE_NEWSLETTERS + " WHERE sn.type='" + IterKeys.NEWSLETTER_SCHEDULE_TYPE_DEFAULT + "'";
	private static final String GET_SCHEDULE_NEWSLETTER					= 	GET_SCHEDULE_NEWSLETTERS + " WHERE sn.scheduleid='%s'";
	private static final String GET_SCHEDULE_NEWSLETTERS_BY_NEWSLETID	= 	GET_SCHEDULE_NEWSLETTERS + " WHERE n.newsletterid='%s'";
	private static final String GET_SCHEDULE_NEWSLETTERS_BY_TYPE		= 	GET_SCHEDULE_NEWSLETTERS + " WHERE n.groupid=%s AND sn.type='" + IterKeys.NEWSLETTER_SCHEDULE_TYPE_ALERT + "' AND sn.enabled=TRUE";
	
	private static final String ADD_SCHEDULE_NEWSLETTER					= 	new StringBuilder(
		"INSERT INTO schedule_newsletter(scheduleid, name, description, newsletterid, days, hour, servermode, 			\n").append(
		" 			 maxrecipients, enabled, lasttriggered, laststatus, modifieddate, publicationdate, 					\n").append(
		" 			 type, allowanonymous)																				\n").append(
		"VALUES ('%s', '%s', %s, '%s', '%s', %s, '%s', %s, %s, NULL, NULL, '%s', NULL, '%s', %s) 						\n").toString();
	
	private static final String UPDATE_SCHEDULE_NEWSLETTER				= 	new StringBuilder(
		"UPDATE schedule_newsletter SET name='%s', description=%s, newsletterid='%s', days='%s', 	\n").append(
		" 		hour=%s, servermode='%s', maxrecipients=%s, enabled=%s, modifieddate='%s',  		\n").append(
		"		type='%s', allowanonymous=%s														\n").append(
		"WHERE scheduleid='%s'																		\n").toString();
	
	private static final String UPDATE_SCHEDULE_NEWSLETTER_STATUS		= 	"UPDATE schedule_newsletter SET lasttriggered=%s, laststatus='%s' WHERE scheduleid='%s'";
	private static final String DELETE_SCHEDULE_NEWSLETTERS				= 	"DELETE FROM schedule_newsletter WHERE scheduleid IN %s";
	private static final String CHECK_SCHEDULE_NEWSLETTER				=	new StringBuilder("SELECT COUNT(*) FROM \n(\n")
																						.append("\tSELECT newsletter.newsletterId, newsletter.plid, schedule_newsletter.scheduleid\n")
																						.append("\tFROM newsletter INNER JOIN schedule_newsletter ON (newsletter.newsletterid=schedule_newsletter.newsletterid)\n")
																						.append("\tWHERE HOUR=%s AND enabled=1 AND (days LIKE '%%%s%%' %s) \n")
																						.append(")tmptable INNER JOIN layout ON ( tmptable.plid=layout.plid AND layout.friendlyURL='%s')\n")
																						.append("WHERE scheduleid!='%s'")
																						.toString();
	
	//SCHEDULE_SMTPSERVER
	private static final String GET_SCHEDULE_SMTPSERVERS				= 	"SELECT host,port,s.enabled,tls,auth,username,password,emailfrom,s.smtpserverid FROM schedule_smtpserver ss INNER JOIN schedule_newsletter sn ON sn.scheduleid=ss.scheduleid INNER JOIN smtpserver s ON s.smtpserverid=ss.smtpserverid";
	private static final String GET_SCHEDULE_SMTPSERVERS_SELECTED		= 	"SELECT s.host, s.smtpserverid, s.port, s.username, s.description, s.enabled, ss.orden, IF(((SELECT COUNT(*) FROM schedule_smtpserver ss2 WHERE ss2.scheduleid='%s' AND ss2.smtpserverid=s.smtpserverid) > 0), TRUE, FALSE) AS selected " +
																				"FROM smtpserver s LEFT JOIN schedule_smtpserver ss ON ss.smtpserverid=s.smtpserverid " + 
																				"WHERE s.groupid=%s GROUP BY s.smtpserverid ORDER BY ss.orden ASC, ss.schedulesmtpid ASC";
	private static final String GET_SCHEDULE_SMTPSERVER_BY_SCHEDULE		= 	GET_SCHEDULE_SMTPSERVERS + " WHERE ss.scheduleid='%s' ORDER BY ss.orden ASC, ss.schedulesmtpid ASC";
//	private static final String GET_SCHEDULE_SMTPSERVER_ENABLED 		= 	GET_SCHEDULE_SMTPSERVERS + " WHERE s.enabled=TRUE AND ss.scheduleid='%s' ORDER BY ss.orden ASC, ss.schedulesmtpid ASC";
	
	private static final String ADD_SCHEDULE_SMTPSERVER					= 	"INSERT INTO schedule_smtpserver(schedulesmtpid, scheduleid, smtpserverid, orden, modifieddate, publicationdate) VALUES %s";
	private static final String DELETE_SCHEDULE_SMTPSERVER				= 	"DELETE FROM schedule_smtpserver WHERE scheduleid = '%s'";
	
	//SCHEDULE_USER
	private static final String GET_SCHEDULE_USERS						= 	"SELECT * FROM schedule_user su INNER JOIN iterusers u ON u.usrid=su.userid";
	private static final String GET_SCHEDULE_USER						= 	GET_SCHEDULE_USERS + " WHERE su.scheduleuserid='%s'\n";
	private static final String GET_SCHEDULE_USER_BY_SCHEDULE			= 	GET_SCHEDULE_USERS + " WHERE su.scheduleid='%s'\n";
	private static final String GET_SCHEDULE_USER_BY_SCHEDULE_ENABLED	= 	new StringBuilder(GET_SCHEDULE_USER_BY_SCHEDULE).append(
				"	AND su.enabled=TRUE 															\n").append(
String.format(	"	AND (u.level = '%s' OR (u.userexpires IS NULL AND u.registerdate IS NOT NULL))	\n", UserConstants.USER_LEVEL_ANONYMOUS)).toString();
	
	private static final String ADD_SCHEDULE_USER						= 	"INSERT INTO schedule_user(scheduleuserid, scheduleid, userid, modifieddate, publicationdate) " +
																				"VALUES ('%s', '%s', '%s', '%s', NULL)";
	
	private static final String DELETE_INVALID_ANONYMOUS_USERS 			= String.format(new StringBuilder( 
		"DELETE 					\n").append( 
		"FROM IterUsers 			\n").append(
		"  WHERE level = '%s' 		\n").append(
		"    AND delegationId = %%d	\n").append(		
		"    AND email IN %%s 		\n").toString(), UserConstants.USER_LEVEL_ANONYMOUS);
	
	private static final String DISABLE_SCHEDULE_USER					= new StringBuilder(
		"UPDATE schedule_user su 							\n").append(
		" 	SET enabled=FALSE, modifieddate='%s' 			\n").append(
		" 	WHERE su.userid IN ( 							\n").append(
		" 						SELECT u.usrid 				\n").append(
		"			 			FROM iterusers u 			\n").append(
		" 						  WHERE u.delegationId = %d	\n").append(
		" 							AND	u.email IN %s) 		\n").toString();
	
	private static final String DELETE_SCHEDULE_USER					= 	"DELETE FROM schedule_user WHERE scheduleuserid IN %s";
	private static final String DELETE_SCHEDULE_USER_BY_SCHE_AND_USER	= 	"DELETE FROM schedule_user WHERE scheduleid='%s' AND userid='%s'";
	
	//SCHEDULE_PRODUCT
	private static final String GET_SCHEDULE_PRODUCT_SELECTED			=	new StringBuilder(
		"SELECT p.name, p.productId,								\n").append(
		"		IF	( (	(	SELECT COUNT(*) 						\n").append(
		"					FROM schedule_product sp2 				\n").append(
		"						WHERE sp2.scheduleid='%s' 			\n").append(
		"							AND sp2.productId=p.productId	\n").append(
		"			  	) > 0 ), TRUE, FALSE ) AS selected 			\n").append(
		"FROM Product p 											\n").append(
		"	WHERE groupid = %d										\n").append(		
		"	ORDER BY p.name ASC, p.productId ASC					\n").toString();
	
	private static final String ADD_SCHEDULE_PRODUCT					= 	"INSERT INTO schedule_product(scheduleproductid, scheduleid, productid, modifieddate, publicationdate) VALUES %s";
	private static final String DELETE_SCHEDULE_PRODUCT					= 	"DELETE FROM schedule_product WHERE scheduleid = '%s'";
	
	//PAGE_TEMPLATES
	private static final String GET_PAGE_TEMPLATES						= 	"SELECT pt.pageTemplateId, pt.name, pt.description, l.plid, l.friendlyURL, " + 
																				"IF(pt.imageId > 0, CAST(CONCAT('/image/journal/template?img_id=', pt.imageId%s) AS CHAR CHARACTER SET utf8), '') imagepath " + 
																				"FROM Designer_PageTemplate pt INNER JOIN Layout l ON l.plid=pt.layoutId WHERE pt.groupId=%s AND pt.type_='newsletter'";
	
	//MANAGE SCHEDULES
	private static final String GET_SCHEDULES_BY_USER					= 	"SELECT scheduleid FROM schedule_user WHERE userid='%s'";
	
	//SCHEDULES XML
	private static final String GET_NEWSLETTERS_XML						= 	"SELECT n.newsletterid id, n.name, n.description FROM newsletter n " +
																				"INNER JOIN schedule_newsletter sn ON sn.newsletterid=n.newsletterid " +
																				"WHERE n.groupid=%s AND sn.enabled=TRUE GROUP BY n.newsletterid";
	
	private static final String GET_SCHEDULES_XML						= 	String.format(new StringBuilder(
		"SELECT sn.scheduleid id, sn.name, sn.description, n.newsletterid, sn.hour, sn.days, 	\n").append(
		"		IF(sn.type='%s', 'scheduled', 'alert') type, 	 								\n").append(
		" 		allowAnonymous			 														\n").append(	
		"FROM schedule_newsletter sn 															\n").append(
		"INNER JOIN newsletter n ON sn.newsletterid=n.newsletterid  							\n").append(
		" 	WHERE n.groupid=%%s 																\n").append(
		" 	  AND sn.enabled = TRUE 															\n").append(
		"GROUP BY sn.scheduleid																	\n").toString(), IterKeys.NEWSLETTER_SCHEDULE_TYPE_DEFAULT);
	
	private static final String GET_PRODUCTS_XML						= 	"SELECT GROUP_CONCAT(DISTINCT p.nameBase64 SEPARATOR ''',''') products " +
																				"FROM Product p " +
																				"INNER JOIN schedule_product sp ON sp.productid=p.productId " +
																				"WHERE scheduleid='%s'";

	private static final String GET_PRODUCT								= new StringBuilder(
		"(IFNULL((SELECT productId			\n").append(
		"FROM Product						\n").append(	
		"	WHERE Product.name = '%1$s' 	\n").append(
		"	  AND groupId=%2$d), '%1$s'))	\n").toString();
	
	private final String SELECTFORUPDATE_BODYMD5						=  "SELECT bodymd5 FROM newsletter WHERE newsletterid='%s' FOR UPDATE";
	private final String UPDATE_BODYMD5									=  "UPDATE newsletter SET bodymd5='%s' WHERE newsletterid='%s'";
	
	    ///////////////////////
	   //				    //
	  //	NEWSLETTER     //
	 //					  //
    ///////////////////////
	
	public String getNewsletters(long groupid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_NEWSLETTERS_BY_GROUP, groupid)).asXML();
	}
	
	public Document exportData(String params) throws SecurityException, NoSuchMethodException, ServiceError, DocumentException, PortalException, SystemException, IOException, SQLException
	{
		Element root 	= SAXReaderUtil.read(params).getRootElement();
		String groupName= XMLHelper.getStringValueOf(root, "@groupName");
		String sql 		= null;
		long groupId 	= 0;
		
		if (Validator.isNotNull(groupName))
		{
			groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
			sql = String.format(GET_NEWSLETTERS_BY_GROUP, groupId);
		}
		else
		{
			String ids = XMLHelper.getStringValueOf(root, "@ids");
			ErrorRaiser.throwIfNull(ids, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			sql = String.format(GET_NEWSLETTER, ids.replaceAll(",", "','"));
		}
			
		Document newsletterDOM = PortalLocalServiceUtil.executeQueryAsDom(sql);
		
		List<Node> newsletters = newsletterDOM.selectNodes("/rs/row");
		for (Node newsletter : newsletters)
		{
			String newsletterId = XMLHelper.getStringValueOf(newsletter, "@newsletterid");
			ErrorRaiser.throwIfNull(newsletterId);
			
			groupId = XMLHelper.getLongValueOf(newsletter, "@groupid");
			ErrorRaiser.throwIfFalse(groupId > 0);
			
			List<Node> schedules = getScheduleNewsletters(newsletterId).selectNodes("/rs/row");
			
			for (Node schedule : schedules)
			{
				String scheduleId = XMLHelper.getStringValueOf(schedule, "@scheduleid");
				ErrorRaiser.throwIfNull(scheduleId);
				
				// Servidores SMTP
				Element elemServers = (Element)getScheduleSMTPServers(scheduleId).getRootElement().detach();
				elemServers.setName("servers");
				((Element)schedule).add( elemServers );
				
				// Productor o subscripciones
				Element elemProducts = (Element)getScheduleProducts(groupId, scheduleId).getRootElement().detach();
				elemProducts.setName("products");
				((Element)schedule).add( elemProducts );
				
				// Se elimian aquellos que elementos que no forman parte de la configuración de este calendario
				List<Node> unconfigNodes = schedule.selectNodes("*/row[@selected='0']");
				for (Node unconfigNode : unconfigNodes)
					unconfigNode.detach();
				
				((Element)newsletter).add( schedule.detach() );
			}
		}
		return newsletterDOM;
	}
	
	public String addNewsletter(String xmlData) throws ServiceError, DocumentException, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row");
		Node node = xpath.selectSingleNode(dataRoot);
		return addNewsletter(node);
	}
	
	private String addNewsletter(Node node) throws ServiceError, DocumentException, IOException, SQLException, SecurityException, NoSuchMethodException	
	{
		String name = XMLHelper.getTextValueOf(node, "@name");
		ErrorRaiser.throwIfNull(name, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		name = StringEscapeUtils.escapeSql(name);
		
		String subject = XMLHelper.getTextValueOf(node, "@subject");
		if(Validator.isNotNull(subject))
		{
			subject = StringPool.APOSTROPHE + 
					  StringEscapeUtils.escapeSql(subject) + 
					  StringPool.APOSTROPHE;
		}
		
		String description = XMLHelper.getTextValueOf(node, "@description");
		if(Validator.isNotNull(description))
		{
			description = StringPool.APOSTROPHE + 
						  StringEscapeUtils.escapeSql(description) + 
						  StringPool.APOSTROPHE;
		}
		
		String groupid = XMLHelper.getTextValueOf(node, "@groupid");
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		String plid 		= null;
		String friendlyURL 	= XMLHelper.getTextValueOf(node, "@friendlyURL");
		if (Validator.isNull(friendlyURL))
		{
			plid = XMLHelper.getTextValueOf(node, "@plid");
			ErrorRaiser.throwIfNull(plid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}
		else
		{
			plid = String.format(GET_PLID_FROM_FRIENDLYURL, friendlyURL, groupid);
		}
		
		String newsletterid = XMLHelper.getTextValueOf(node, "@newsletterid");
		if(Validator.isNull(newsletterid))
			newsletterid = SQLQueries.getUUID();
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(ADD_NEWSLETTER, newsletterid, name, subject, description, 
																				plid, groupid, SQLQueries.getCurrentDate()));
		
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_NEWSLETTER, newsletterid)).asXML();
	}
	
	public String updateNewsletter(String xmlData) throws ServiceError, DocumentException, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row");
		Node node = xpath.selectSingleNode(dataRoot);
		return updateNewsletter(node);
	}
	
	private String updateNewsletter(Node node) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		String newsletterid = XMLHelper.getTextValueOf(node, "@newsletterid");
		ErrorRaiser.throwIfNull(newsletterid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String name = XMLHelper.getTextValueOf(node, "@name");
		ErrorRaiser.throwIfNull(name, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		name = StringEscapeUtils.escapeSql(name);
		
		String subject = XMLHelper.getTextValueOf(node, "@subject");
		if(Validator.isNotNull(subject))
		{
			subject = StringPool.APOSTROPHE + 
					  StringEscapeUtils.escapeSql(subject) + 
					  StringPool.APOSTROPHE;
		}
		
		String description = XMLHelper.getTextValueOf(node, "@description");
		if(Validator.isNotNull(description))
		{
			description = StringPool.APOSTROPHE + 
						  StringEscapeUtils.escapeSql(description) + 
						  StringPool.APOSTROPHE;
		}
		
		String plid 		= null;
		String friendlyURL 	= XMLHelper.getTextValueOf(node, "@friendlyURL");
		if (Validator.isNull(friendlyURL))
		{
			plid = XMLHelper.getTextValueOf(node, "@plid");
			ErrorRaiser.throwIfNull(plid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}
		else
		{
			plid = String.format(GET_PLID_FROM_FRIENDLYURL, friendlyURL, "newsletter.groupid");
		}
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_NEWSLETTER, name, subject, description, plid, SQLQueries.getCurrentDate(), newsletterid));
		
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_NEWSLETTER, newsletterid)).asXML();
	}
	
	public String deleteNewsletters(String xmlData) throws DocumentException, ServiceError, IOException, SQLException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row/@newsletterid");
		
		List<Node> nodes = xpath.selectNodes(dataRoot);
		ErrorRaiser.throwIfFalse((nodes != null && nodes.size() > 0), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_NEWSLETTERS, TeaserMgr.getInClauseSQL(nodes)));
		
		return xmlData;
	}
	
	
	    //////////////////////////////
	   //				   		   //
	  //   SCHEDULE_NEWSLETTER    //
	 //					 	  	 //
    //////////////////////////////
	
	public Document getScheduleNewsletters(String newsletterid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(newsletterid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SCHEDULE_NEWSLETTERS_BY_NEWSLETID, newsletterid));
	}
	
	public String addScheduleNewsletter(String xmlData) throws ServiceError, DocumentException, IOException, SQLException, SecurityException, NoSuchMethodException, NumberFormatException, PortalException, SystemException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row");
		Node node = xpath.selectSingleNode(dataRoot);
		
		return addScheduleNewsletter(node);
	}
	
	private String addScheduleNewsletter(Node node) throws ServiceError, DocumentException, IOException, SQLException, SecurityException, NoSuchMethodException, NumberFormatException, PortalException, SystemException
	{
		String name = XMLHelper.getTextValueOf(node, "@name");
		ErrorRaiser.throwIfNull(name, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		name = StringEscapeUtils.escapeSql(name);
		
		String description = XMLHelper.getTextValueOf(node, "@description");
		if(Validator.isNotNull(description))
		{
			description = StringPool.APOSTROPHE + 
					  	  StringEscapeUtils.escapeSql(description) + 
					  	  StringPool.APOSTROPHE;
		}

		String newsletterid = XMLHelper.getTextValueOf(node, "@newsletterid");
		ErrorRaiser.throwIfNull(newsletterid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		boolean enabled 		= GetterUtil.getBoolean(XMLHelper.getTextValueOf(node, "@enabled"), true);
		boolean allowanonymous 	= GetterUtil.getBoolean(XMLHelper.getTextValueOf(node, "@allowanonymous"));
				
		String days = GetterUtil.getString(XMLHelper.getTextValueOf(node, "@days"), DEFAULT_DAYS);
		String hour = GetterUtil.getString(XMLHelper.getTextValueOf(node, "@hour"), DEFAULT_HOUR);
		String maxrecipients = GetterUtil.getString(XMLHelper.getTextValueOf(node, "@maxrecipients"), String.valueOf(DEFAULT_MAX_RECIPIENTS));

		String servermode = XMLHelper.getTextValueOf(node, "@servermode");
		if(Validator.isNull(servermode))
			servermode = ROUNDROBIN;

		long groupid = XMLHelper.getLongValueOf(node, "@groupid");
		ErrorRaiser.throwIfFalse(groupid > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX);
		
		String scheduleid = XMLHelper.getTextValueOf(node, "@scheduleid");
		if(Validator.isNull(scheduleid))
			scheduleid = SQLQueries.getUUID();
		
		String type = GetterUtil.getString(XMLHelper.getTextValueOf(node, "@type"), IterKeys.NEWSLETTER_SCHEDULE_TYPE_DEFAULT);
		String friendlyUrl = GetterUtil.getString(XMLHelper.getTextValueOf(node, "@friendlyURL"), StringPool.BLANK);
		
		checkSchedulers(enabled, hour, days, scheduleid, friendlyUrl);
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(ADD_SCHEDULE_NEWSLETTER, scheduleid, name, description, newsletterid, 
																						 days, hour, servermode, maxrecipients, 
																						 String.valueOf(enabled), 
																						 SQLQueries.getCurrentDate(), type,
																						 String.valueOf(allowanonymous)));

		//Asociamos la programación con todos los servidores SMTP
		Element serversRoot = SMTPServerMgrLocalServiceUtil.getServers(groupid).getRootElement();
		List<Node> nodes = serversRoot.selectNodes("//row");
		if(nodes.size() > 0)
		{
			Element rs = SAXReaderUtil.read("<rs/>").getRootElement();
			rs.addAttribute("scheduleid", scheduleid);
			
			for(int i = 0; i < nodes.size(); i++)
			{
				Element row = rs.addElement("row");
				row.addAttribute("smtpserverid", XMLHelper.getTextValueOf(nodes.get(i), "@smtpserverid"));
			}
			
			addScheduleSMTPServers(rs.asXML());
		}
		
		requestSchedule(scheduleid, enabled, days, hour, type);
		
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SCHEDULE_NEWSLETTER, scheduleid)).asXML();
	}
	
	public String updateScheduleNewsletter(String xmlData) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException, NumberFormatException, PortalException, SystemException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row");
		Node node = xpath.selectSingleNode(dataRoot);
		
		return updateScheduleNewsletter(node);
	}

	private String updateScheduleNewsletter(Node node) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException, NumberFormatException, PortalException, SystemException
	{
		String scheduleid = XMLHelper.getTextValueOf(node, "@scheduleid");
		ErrorRaiser.throwIfNull(scheduleid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String name = XMLHelper.getTextValueOf(node, "@name");
		ErrorRaiser.throwIfNull(name, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		name = StringEscapeUtils.escapeSql(name);
		
		String description = XMLHelper.getTextValueOf(node, "@description");
		if(Validator.isNotNull(description))
		{
			description = StringPool.APOSTROPHE + 
					  	  StringEscapeUtils.escapeSql(description) + 
					  	  StringPool.APOSTROPHE;
		}

		String newsletterid = XMLHelper.getTextValueOf(node, "@newsletterid");
		ErrorRaiser.throwIfNull(newsletterid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		boolean enabled  		= GetterUtil.getBoolean(XMLHelper.getTextValueOf(node, "@enabled"), true);
		boolean allowanonymous 	= GetterUtil.getBoolean(XMLHelper.getTextValueOf(node, "@allowanonymous"));
		
		String days = GetterUtil.getString(XMLHelper.getTextValueOf(node, "@days"), DEFAULT_DAYS);
		String hour = GetterUtil.getString(XMLHelper.getTextValueOf(node, "@hour"), DEFAULT_HOUR);
		String maxrecipients = GetterUtil.getString(XMLHelper.getTextValueOf(node, "@maxrecipients"), String.valueOf(DEFAULT_MAX_RECIPIENTS));

		String servermode = XMLHelper.getTextValueOf(node, "@servermode");
		if(Validator.isNull(servermode))
			servermode = ROUNDROBIN;
		
		String groupid = XMLHelper.getTextValueOf(node, "@groupid");
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String type = GetterUtil.getString(XMLHelper.getTextValueOf(node, "@type"), IterKeys.NEWSLETTER_SCHEDULE_TYPE_DEFAULT);
		String friendlyUrl = GetterUtil.getString(XMLHelper.getTextValueOf(node, "@friendlyURL"), StringPool.BLANK);
		
		checkSchedulers(enabled, hour, days, scheduleid, friendlyUrl);
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_SCHEDULE_NEWSLETTER, name, description, newsletterid, days, 
																							hour, servermode, maxrecipients, 
																							String.valueOf(enabled), 
																							SQLQueries.getCurrentDate(), 
																							type, String.valueOf(allowanonymous),
																							scheduleid));
		
		requestSchedule(scheduleid, enabled, days, hour, type);
		
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SCHEDULE_NEWSLETTER, scheduleid)).asXML();
	}
	
	private void checkSchedulers(boolean enabled, String hour, String days, String scheduleid, String friendlyUrl) throws ServiceError
	{
		if(enabled)
		{
			String daysLike = " OR days LIKE '%%%s%%'";
			String[] daysArr = days.split(StringPool.COMMA);
			StringBuilder sb = new StringBuilder("");
			int numDays = daysArr.length;
			if(numDays>1)
			{
				for(int i=1; i<numDays; i++)
					sb.append( String.format(daysLike, daysArr[i]) );
			}
			
			
			String checkQuey = String.format(CHECK_SCHEDULE_NEWSLETTER, hour, daysArr[0], sb.toString(), friendlyUrl, scheduleid);
			_log.debug(checkQuey);
			
			
			List<Object> checkResult = PortalLocalServiceUtil.executeQueryAsList(checkQuey);
			ErrorRaiser.throwIfFalse(checkResult!=null && checkResult.size()==1 && GetterUtil.getInteger(String.valueOf(checkResult.get(0)), 0)==0, IterErrorKeys.XYZ_ITR_E_REPEATED_SCHEDULE_ZYX);
		}
	}

	public String deleteScheduleNewsletters(String xmlData) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException, NumberFormatException, PortalException, SystemException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row/@scheduleid");
		
		List<Node> nodes = xpath.selectNodes(dataRoot);
		ErrorRaiser.throwIfFalse((nodes != null && nodes.size() > 0), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		for (Node node:nodes)
		{
			stopSchedule(node.getStringValue());
		}
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_SCHEDULE_NEWSLETTERS, TeaserMgr.getInClauseSQL(nodes)));
		
		return xmlData;
	}
	
	public void initSchedules()
	{
		try
		{
			if (!PropsValues.IS_PREVIEW_ENVIRONMENT)
			{
				Element dataRoot = PortalLocalServiceUtil.executeQueryAsDom(GET_SCHEDULE_NEWSLETTERS_DEFAULT).getRootElement();
				if(dataRoot != null)
				{
					List<Node> nodes = SAXReaderUtil.createXPath("//row").selectNodes(dataRoot);
					if(nodes != null && nodes.size() > 0)
					{
						for(Node node:nodes)
						{
							String scheduleid = XMLHelper.getTextValueOf(node, "@scheduleid");
							ErrorRaiser.throwIfNull(scheduleid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

							boolean enabled = GetterUtil.getBoolean(XMLHelper.getTextValueOf(node, "@enabled"), true);
							if(enabled)
							{
								String type = XMLHelper.getTextValueOf(node, "@type");
								String days = XMLHelper.getTextValueOf(node, "@days");
								String hour = XMLHelper.getTextValueOf(node, "@hour");
			
								requestSchedule(scheduleid, enabled, days, hour, type);
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		finally
		{
			_log.info("The newsletters schedules has started");
		}
	}
	
	public void stopNewsletters()
	{
		_log.info("Stopping newsletters");
		
		// Se paran las newsletters programadas
		stopSchedules();
		
		// http://jira.protecmedia.com:8080/browse/ITER-1091?focusedCommentId=46477&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-46477
		// Se paran las newsletters que sean de tipo Alert
		// AlertNewsletterMgr.stop();
	}
	
	private void stopSchedules()
	{
		try
		{
			if (!PropsValues.IS_PREVIEW_ENVIRONMENT)
			{
				Element dataRoot = PortalLocalServiceUtil.executeQueryAsDom(GET_SCHEDULE_NEWSLETTERS_DEFAULT).getRootElement();
				if(dataRoot != null)
				{
					List<Node> nodes = SAXReaderUtil.createXPath("//row").selectNodes(dataRoot);
					if(nodes != null && nodes.size() > 0)
					{
						for (Node node:nodes)
						{
							stopSchedule( XMLHelper.getTextValueOf(node, "@scheduleid") );
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		finally
		{
			_log.info("The newsletters schedules has stopped");
		}
	}
	
	//Remote call for "sendAlertNewsletters" method
	public void requestSendAlertNewsletters(String groupid, String lastUpdate) throws Exception
	{
		try
		{
			if(IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_LIVE))
			{
				// No se acepta envío de nuevas newsletter si no es el responsable del Cluster
				ErrorRaiser.throwIfFalse(Heartbeat.canLaunchProcesses(), IterErrorKeys.XYZ_E_NEWSLETTER_ISNOT_MASTER_SERVER_ZYX);
				sendAlertNewsletters(groupid, lastUpdate);
			}
		}
		catch (Exception e)
		{
			IterMonitor.logEvent(Long.parseLong(groupid), Event.ERROR, new Date(), String.format("SendAlertNewsletters: %s", e.toString()), e);
			throw e;
		}
	}
	
	public void sendAlertNewsletters(String groupid, String lastUpdate) throws Exception
	{
		Document doc = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SCHEDULE_NEWSLETTERS_BY_TYPE, groupid));
		List<Node> nodes = SAXReaderUtil.createXPath("//row").selectNodes(doc);
		for(Node node:nodes)
		{
			String scheduleid = XMLHelper.getTextValueOf(node, "@scheduleid");
			AlertNewsletterMgr.addToQueue(scheduleid, lastUpdate);
		}
	}
	
	public void startAlertNewslettersTask(String groupid, Date lastUpdate) throws Exception 
	{
		_log.debug("Starting alert newsletters");
		requestSendAlertNewsletters(groupid, String.valueOf(lastUpdate.getTime()));
	}
	
	public String sendNewsletter(String scheduleid) throws Exception
	{
		ErrorRaiser.throwIfFalse(Heartbeat.canLaunchProcesses(), IterErrorKeys.XYZ_E_NEWSLETTER_ISNOT_MASTER_SERVER_ZYX);

		return sendNewsletter(scheduleid, null, false);
	}
	
	public String sendNewsletterAndSchedule(String scheduleid) throws Exception
	{
		if(_log.isDebugEnabled())
		{
			if(Validator.isNotNull(scheduleid))
				_log.debug("Sending and scheduling newsletter: " + scheduleid);
			else
				_log.debug("Sending and scheduling empty newsletter id");
		}
		
		return sendNewsletter(scheduleid, null, true);
	}
	
	private String sendNewsletter(String scheduleid, String lastUpdate, boolean doSchedule) throws Exception
	{
		return sendNewsletter(scheduleid, null, lastUpdate, doSchedule);
	}
	
	public String sendNewsletter(String scheduleid, String content, String lastUpdate, boolean doSchedule) throws Exception
	{
		String prefix = "Sending newsletter: Schedule ".concat(scheduleid);
		_log.info( prefix.concat(": Begin") );
		
		String result = "";
		
		long groupid = GroupMgr.getGlobalGroupId();
		
		try
		{
			XPath xpath = SAXReaderUtil.createXPath("//row");
	
			Node scheduleNode = null;
			Element scheduleRoot = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SCHEDULE_NEWSLETTER, scheduleid)).getRootElement();
			List<Node> scheduleNodes = xpath.selectNodes(scheduleRoot);
			if(scheduleNodes.size() > 0)
				scheduleNode = scheduleNodes.get(0);
			
			boolean scheduleEnabled = GetterUtil.getBoolean(XMLHelper.getTextValueOf(scheduleNode, "@enabled"));
			String type 			= XMLHelper.getTextValueOf(scheduleNode, "@type");
			
			boolean typeCheck 		= (	 type.equalsIgnoreCase(IterKeys.NEWSLETTER_SCHEDULE_TYPE_DEFAULT) || 
										(type.equalsIgnoreCase(IterKeys.NEWSLETTER_SCHEDULE_TYPE_ALERT) && !doSchedule));
			if(scheduleEnabled && typeCheck)
			{
				//Información de la newsletter
				groupid 				= GetterUtil.getInteger(XMLHelper.getTextValueOf(scheduleNode, "@groupid"));
				int maxrecipients 		= GetterUtil.getInteger(XMLHelper.getTextValueOf(scheduleNode, "@maxrecipients"), Integer.parseInt(DEFAULT_MAX_RECIPIENTS));
				StringBuilder subject 	= new StringBuilder( GetterUtil.get(XMLHelper.getTextValueOf(scheduleNode, "@subject"), StringPool.BLANK) );
				String servermode 		= GetterUtil.getString(XMLHelper.getTextValueOf(scheduleNode, "@servermode"), ROUNDROBIN);
				String body = null;
				
				if (Validator.isNotNull(content))
					body = content;
				else
				{
					String friendlyURL 		= XMLHelper.getTextValueOf(scheduleNode, "@friendlyURL");
					String friendlyURLTyped = new StringBuilder(friendlyURL).append("?type=").append(type).toString();
					String newsletterId 	= XMLHelper.getTextValueOf(scheduleNode, "@newsletterid");
					String newsletterName	= XMLHelper.getTextValueOf(scheduleNode, "@newsletterName");
					String scheduleName		= XMLHelper.getTextValueOf(scheduleNode, "@scheduleName");
					
					body		 			= getURLContent(newsletterId, groupid, friendlyURLTyped, lastUpdate, subject, newsletterName, scheduleName);
				}
	
				if(Validator.isNotNull(body))
				{
					//Información de los servidores SMTP
					Element smtpRoot = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SCHEDULE_SMTPSERVER_BY_SCHEDULE, scheduleid)).getRootElement();
					List<Node> smtpNodes = xpath.selectNodes(smtpRoot);
					if(smtpNodes != null && smtpNodes.size() > 0)
					{
						List<Node> smptNodesEnabled = smtpRoot.selectNodes("//row[@enabled='true']");
						if(smptNodesEnabled != null && smptNodesEnabled.size() > 0)
						{
							//Información de los usuarios
							Element userRoot = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SCHEDULE_USER_BY_SCHEDULE_ENABLED, scheduleid)).getRootElement();
							List<Node> userNodes = xpath.selectNodes(userRoot);
							if(maxrecipients == Integer.parseInt(DEFAULT_MAX_RECIPIENTS))
								maxrecipients = userNodes.size();
							
							if(userNodes.size() > 0)
							{
								String status = sendNewsletterForUsers(groupid, scheduleid, subject.toString(), body, servermode, maxrecipients, userNodes, smptNodesEnabled).toString();
								String currentDate = "NULL";
								if(!status.contains(IMPOSSIBLE_TO_SEND))
									currentDate = "'" + SQLQueries.getCurrentDate() + "'";
		
								PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_SCHEDULE_NEWSLETTER_STATUS, currentDate, StringEscapeUtils.escapeSql(status), scheduleid));
								
								result = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SCHEDULE_NEWSLETTER, scheduleid)).asXML();
							}
							else
							{
								_log.error(prefix.concat(" has no recipients assigned"));
								ErrorRaiser.throwIfError(IterErrorKeys.XYZ_ITR_E_SCHEDULE_HAS_NO_RECIPIENTS_ZYX);
							}
						}
						else
						{
							_log.error(prefix.concat(" has no enabled SMTP servers assigned"));
							ErrorRaiser.throwIfError(IterErrorKeys.XYZ_ITR_E_SCHEDULE_HAS_NO_ENABLED_SERVERS_ZYX);
						}
					}
					else
					{
						_log.error(prefix.concat(" has no SMTP servers assigned"));
						ErrorRaiser.throwIfError(IterErrorKeys.XYZ_ITR_E_SCHEDULE_HAS_NO_SERVERS_ZYX);
					}
				}
				else
				{
					_log.error(prefix.concat(" has no content to send"));
					PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_SCHEDULE_NEWSLETTER_STATUS, "NULL", "No content to send", scheduleid));
				}
			}
			else
			{
				doSchedule = false;
				NewsletterTask toSchedule = getSchedule(scheduleid);
				if(toSchedule != null)
					toSchedule.interrupt(Task.DEAD);
			}
			
			if (doSchedule)
			{
				if (_log.isDebugEnabled())
						_log.debug(prefix.concat(": Scheduling"));
				
				schedule(scheduleid, scheduleEnabled, XMLHelper.getTextValueOf(scheduleNode, "@days"), XMLHelper.getTextValueOf(scheduleNode, "@hour"));
			}
		}
		catch (Exception e)
		{
			IterMonitor.logEvent(groupid, Event.ERROR, new Date(), String.format("%s %s", prefix, e.toString()), e);
			throw e;
		}
		finally
		{
			_log.info( prefix.concat(": End") );
		}
		
		return result;
	}

	private StringBuffer sendNewsletterForUsers(long groupId, String scheduleid, String subject, String body, String servermode, 
										  		int maxrecipients, List<Node> userNodes, List<Node> smtps) throws ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException, PortalException, SystemException
	{
		String prefix = "Sending newsletter: Schedule ".concat(scheduleid);
		StringBuffer status 				= new StringBuffer();
		ArrayDeque<String> invalidRecipients= new ArrayDeque<String>();
		ArrayDeque<String> validRecipients 	= new ArrayDeque<String>();
		boolean impossibleToSend 			= true;
		
		filterEmailUsers(scheduleid, userNodes, validRecipients, invalidRecipients);
		
		SMTPMgr smtpMgr = new SMTPMgr(groupId, smtps, servermode);
		
		long chunkNumber = 0;
		// Mientras queden destinatarios
		while ( smtpMgr.hasAvailableServer() && validRecipients.size() > 0 && !_log_break_loop.isTraceEnabled() )
		{
			chunkNumber++;
			int listSize = Math.min(maxrecipients, validRecipients.size());
			List<String> currentEmails = new ArrayList<String>(listSize);
			for (int i = 0; i < listSize; i++)
				currentEmails.add( validRecipients.removeFirst() );
			
			Element smtpNode = smtpMgr.getNextServer();
			
			if (_log.isDebugEnabled())
			{
				_log.debug( String.format("SMTP: %s\nchunk: %d pending Recipients: %d", 
							smtpNode.attributeValue("smtpserverid"), chunkNumber, validRecipients.size()) );
				
				if( _log.isTraceEnabled())
					_log.trace( String.format("SMTP info: %s\nCurrent emails: %s", smtpNode.asXML(), StringUtil.merge(currentEmails.toArray(), ", ")) );
			}
			
			try
			{
				// La ecepción AddressException NO se captura pq ya se validan previamente las direcciones de envío y el remitente
				// En el caso de ser lanzada se trazará y se saldrá del bucle cuando se agoten los servidores SMTP
				MailUtil.sendEmail(smtpNode, subject, body, "text/html; charset=utf-8", currentEmails, RecipientType.BCC);
				impossibleToSend = false;
			}
			catch (SendFailedException sfe)
			{
				// Ha fallado el envío de un bloque de direcciones. Se añaden al final de la lista aquellas que no pudieron ser enviadas, 
				// y se separan las erróneas para que sean descartadas
				_log.error(sfe);
				
				Address[] invalid = sfe.getInvalidAddresses();
				if (Validator.isNotNull(invalid))
				{
					for (int j = 0; j < invalid.length; j++)
					{
						try
						{
							invalidRecipients.add(IterUserTools.encryptGDPR( invalid[j].toString() ));
						}
						catch (Exception e)	{ _log.error(e); }
						
						invalidRecipients.add(invalid[j].toString());
					}
					
					if (_log.isDebugEnabled())
					{
						_log.debug("Invalid recipients added: " + invalid.length);
					
						if (_log.isTraceEnabled())
							_log.trace("Invalid addresses: " + StringUtil.merge(invalid, ", "));
					}
				}
				
				Address[] unsent = sfe.getValidUnsentAddresses();
				if (Validator.isNotNull(unsent))
				{
					for (int j = 0; j < unsent.length; j++)
						validRecipients.add(unsent[j].toString());
					
					if (_log.isDebugEnabled())
					{
						_log.debug("Valid recipients added: " + unsent.length);
					
						if (_log.isTraceEnabled())
							_log.trace("Unsent addresses: " + StringUtil.merge(unsent, ", "));
					}
				}
			}
			catch (Exception e)
			{
				_log.error(e);
				status.append( String.format("[%s]:%s", smtpNode.attributeValue("host"), e.toString()) );
				
				// Se descarta el servidor SMTP actual
				smtpMgr.removeCurrentServer();
				
				// Se añaden las direcciones al final de la lista. Si el problema está en las direcciones 
				// se saldrá del bucle cuando se terminen descartando todos los servidores
				validRecipients.addAll(currentEmails);
			}
		}
		
		if (invalidRecipients.size() > 0)
		{
			long delegationId = GroupLocalServiceUtil.getGroup(groupId).getDelegationId(); 
			
			String inClause = new StringBuilder("('").append( StringUtil.merge(invalidRecipients,"','") ).append("')").toString();
			
			// Se borran los usuarios anónimos cuyos correos han fallado
			String sql = String.format(DELETE_INVALID_ANONYMOUS_USERS, delegationId, inClause);
			_log.debug(sql);
			PortalLocalServiceUtil.executeUpdateQuery(sql);
			
			// Se deshabilitan las programaciones de newsletter del resto de usuarios con email inválidos
			sql = String.format(DISABLE_SCHEDULE_USER, SQLQueries.getCurrentDate(), delegationId, inClause);
			_log.debug(sql);
			PortalLocalServiceUtil.executeUpdateQuery(sql);
			
			String msg = "Invalid recipients for schedule: " + scheduleid +" "+ inClause + " have been disabled\n";
			status.append(msg); 
			_log.error(msg);
						
			// Registra el evento en las métricas
			// ESTA MÉTRICA NO funciona porque en invalidRecipients hay emails NO userIDs
			// Habría que crear un sabor NUEVO de hit que recibiese los correos electrónicos y el delegationId por el que crear una QUERY
			NewslettersMetricsUtil.hit(scheduleid, StringUtil.merge(invalidRecipients.toArray(), StringPool.COMMA, StringPool.APOSTROPHE), NewslettersMetricsUtil.HIT.AUTO_DISABLED_SUBSCRIPTION);
		}
		
		if (impossibleToSend)
		{
			status.insert(0, IMPOSSIBLE_TO_SEND + "\n");
			_log.error(IMPOSSIBLE_TO_SEND + ": " + scheduleid + " at " + SQLQueries.getCurrentDate());
		}
		
		_log.debug(status);
		
		if (status.length() > 0)
			IterMonitor.logEvent(groupId, Event.WARNING, new Date(), String.format("%s %s", prefix, status));
		
		return status;
	}

	private void filterEmailUsers(String scheduleid, List<Node> userNodes, ArrayDeque<String> validRecipients, ArrayDeque<String> invalidRecipients)
	{
		String prefix = "Schedule ".concat(scheduleid);
		
		for (Node user : userNodes)
		{
			String encEmail = XMLHelper.getTextValueOf(user, "@email");
			String email 	= null;
			try
			{
				// ITER-1085 No se envía la newsletter para usuarios anónimos
				// El email podría estar codificado, por lo que antes de gestionarlo y validarlo hay que desencriptarlo
				email = IterUserTools.decryptGDPR( encEmail );
			}
			catch (Exception e)
			{
				_log.error(e);
			}
			
			if ( email != null && MailUtil.isValidEmailAddress(email) )
				validRecipients.add(email);
			else
			{
				// ITER-808GDPR: Encriptación de los datos personales de los usuarios
				// Hay que buscar por datos encriptados
				invalidRecipients.add(encEmail);
				
				if (email != null)
					invalidRecipients.add(email);
			}
		}
		
		if (_log.isDebugEnabled())
		{
			String info = String.format("%s :Total users: %s. Valid recipients: %s. Invalid recipients: %s.", 
										prefix, userNodes.size(), validRecipients.size(), invalidRecipients.size());
			_log.debug(info);
			
			if (_log.isTraceEnabled())
				_log.trace( String.format("%s :Valid recipients: %s", 	prefix, StringUtil.merge(validRecipients.toArray(),	  ", ")) );
		}
		
		if (!invalidRecipients.isEmpty())
			_log.error( String.format("%s :Invalid recipients: %s", prefix, StringUtil.merge(invalidRecipients.toArray(), ", ")) );
		
		if (validRecipients.isEmpty())
			_log.error( String.format("%s hasn't valid recipients", prefix) );
	}

	private String getURLContent(String newsletterId, long groupid, String friendlyURL, String lastUpdate, StringBuilder subject,
								String newsletterName, String scheduleName) throws IOException, PortalException, SystemException, ServiceError
	{
		StringBuffer html = new StringBuffer();
		
		int port = GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SERVER_PORT), 80);
		String virtualhost = LayoutSetLocalServiceUtil.getLayoutSet(groupid, false).getVirtualHost();
		String url = virtualhost;
		
		if(IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_LIVE))
		{
			ApacheHierarchy apacheHierarchy = new ApacheHierarchy();
			String[] masterList = apacheHierarchy.getMasterList();
			if(masterList == null || masterList.length == 0)
				_log.error("No master Apache available");
			
			ErrorRaiser.throwIfFalse((masterList != null && masterList.length > 0), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
				
			url = new ApacheHierarchy().getMasterList()[0];
		}
		
		if(port != 80)
			url += StringPool.COLON + port;

		url += friendlyURL;
		
		if(!url.contains(Http.HTTP_WITH_SLASH))
			url = Http.HTTP_WITH_SLASH + url;
		
		if(_log.isTraceEnabled())
		{
			_log.trace("Current URL Newsletter: " + url);
			_log.trace("Virtualhost: " + virtualhost);
		}
		
		URLConnection httpConnection = (HttpURLConnection)(new URL(url).openConnection());

		try
		{
			httpConnection.setConnectTimeout(ApacheUtil.getApacheConnTimeout());
			httpConnection.setReadTimeout(ApacheUtil.getApacheReadTimeout());
			
			httpConnection.setRequestProperty(WebKeys.REQUEST_HEADER_ITS_NEWSLETTER, "true");
			
			// Se añade, si procede, la información de la campaña para poder marcar las URLs de la newsletter.
			NewsletterMASTools.setCampaignValue(httpConnection, groupid, newsletterName, scheduleName);
			
			if(IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_LIVE))
			{
				httpConnection.setRequestProperty(WebKeys.HOST, virtualhost);
				httpConnection.setRequestProperty("User-Agent", WebKeys.ITER_FULL);
			}
			
			if(Validator.isNotNull(lastUpdate))
				httpConnection.setRequestProperty(WebKeys.REQUEST_HEADER_PUB_DATE_BEFORE_CURRENT, lastUpdate);

			httpConnection.connect();
			HttpUtil.throwIfConnectionFailed( (HttpURLConnection)httpConnection, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX );
			html.append( StreamUtil.toString(httpConnection.getInputStream(), StringPool.UTF8) );
			
			Boolean discard = GetterUtil.getBoolean(httpConnection.getHeaderField(WebKeys.RESPONSE_HEADER_DISCARD_RESPONSE), false);
			if(discard)
				html = new StringBuffer();
			else
			{
				if( !NewsletterTools.checkNewsletterContent(newsletterId, html) )
					html = new StringBuffer();
			}
			
			String newsletterSubj = httpConnection.getHeaderField(WebKeys.REQUEST_HEADER_NEWSLETTER_SUBJECT);
			if (Validator.isNotNull(newsletterSubj))
				subject.replace(0, subject.length(), newsletterSubj);
			
			if(_log.isDebugEnabled() && discard)
				_log.info("Content will be discarded");
		}
		catch(Exception e)
		{
			html = new StringBuffer();
			
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return html.toString();
	}
	
	public boolean checkContentBeforeSend(String newsletterId, StringBuffer body) throws NoSuchAlgorithmException, IOException, SQLException
	{
		boolean send = false;
		
		if (_log_force_send.isTraceEnabled())
		{
			// Mediante la activación de esta traza se evita la comprobación de si ya se ha enviado justo 
			// este contenido, y se envía siempre el email, es útil para escenarios de pruebas y depuración
			send = true;
		}
		else
		{
			String query = String.format(SELECTFORUPDATE_BODYMD5, newsletterId);
			
			if(_log.isDebugEnabled())
			{
				_log.debug( "body html: " + body.toString());
				_log.debug( "Query to get newsletter md5 body: " + query );
			}
			
			List<Object> result = PortalLocalServiceUtil.executeQueryAsList(query);
			if(result!=null && result.size()==1)
			{
				String DBmd5Content = String.valueOf( result.get(0) );
				String md5body = EncryptUtil.digest(body.toString());
				
				if(_log.isDebugEnabled())
					_log.debug("DB md5 content: " + DBmd5Content + " current md5 content: " + md5body);
				
				if( !DBmd5Content.equals( md5body ) )
				{
					send = true;
					query = String.format(UPDATE_BODYMD5, md5body, newsletterId);
					
					if(_log.isDebugEnabled())
						_log.debug("Query to update body md5: " + query);
					
					PortalLocalServiceUtil.executeUpdateQuery(query);
				}
			}
		}
		
		return send;
	}
	
	private void stopSchedule(String scheduleid) throws SecurityException, NoSuchMethodException, ServiceError, NumberFormatException, PortalException, SystemException, UnsupportedEncodingException, ClientProtocolException, IOException
	{
		requestSchedule(scheduleid, false, null, null, IterKeys.NEWSLETTER_SCHEDULE_TYPE_DEFAULT);
	}
	
	public String getLiveServers() throws ServiceError, NumberFormatException, PortalException, SystemException, UnsupportedEncodingException, ClientProtocolException, IOException, DocumentException
	{
		return "<rs/>";
	}
	
	//Remote call for "getLiveServers" method
	public String requestLiveServers(String groupid) throws ServiceError, NumberFormatException, PortalException, SystemException, UnsupportedEncodingException, ClientProtocolException, IOException, DocumentException
	{
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		String liveServers = "";
		
		Group scopeGroup = GroupServiceUtil.getGroup(Long.parseLong(groupid));
		LiveConfiguration liveConf = LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(scopeGroup.getCompanyId());
		
		if(liveConf != null && IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
		{
			String remoteUser = liveConf.getRemoteUserName();
			String remotePass = liveConf.getRemoteUserPassword();
			int remotePort = Integer.valueOf(liveConf.getRemoteIterServer2().split(":")[1]);
			String remoteIP = liveConf.getRemoteIterServer2().split(":")[0];
	
			List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
			remoteMethodParams.add(new BasicNameValuePair("serviceClassName", "com.protecmedia.iter.base.service.NewsletterMgrServiceUtil"));
			remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", "getLiveServers"));
			
			JSONObject json = null;
			
			try
			{
				String result = XMLIOUtil.executeJSONRemoteMethod2(scopeGroup.getCompanyId(), remoteIP, remotePort, remoteUser, 
																   remotePass, "/base-portlet/secure/json", remoteMethodParams);
				
				json = JSONFactoryUtil.createJSONObject(result);
			}
			catch(Exception e)
			{
				_log.error(e.toString());
				_log.error("Getting affinity servers own local URLs");
				_log.trace(e);
				liveServers = getLiveServers();
			}
			
			if(json != null)
			{
				String errorMsg = json.getString("exception");
				if (!errorMsg.isEmpty()) 
				{
					String iterErrorMsg = ServiceErrorUtil.containIterException(errorMsg);
					throw new SystemException(iterErrorMsg.isEmpty() ? errorMsg : iterErrorMsg);
				}
				
				liveServers = json.getString("returnValue");
			}
		}
		else
		{
			liveServers = getLiveServers();
		}
		
		if(_log.isDebugEnabled() && Validator.isNotNull(liveServers))
			_log.debug("Live servers: " + liveServers);

		return liveServers;
	}
	
	public void schedule(String scheduleid, boolean enabled, String days, String hour) throws SecurityException, NoSuchMethodException, ServiceError
	{
		if (Validator.isNotNull(scheduleid))
		{
			List<Object> currentSchedule = 
					PortalLocalServiceUtil.executeQueryAsList(String.format(GET_SCHEDULE_NEWSLETTER, scheduleid));
			if(currentSchedule != null && currentSchedule.size() > 0)
			{
				NewsletterTask toSchedule = getSchedule(scheduleid);
				if (enabled && Heartbeat.canLaunchProcesses())
				{
					long delay = calculateDelay(days, hour);
					if(delay > -1)
					{
						if(toSchedule == null)
						{
							synchronized(schedules)
							{
								_log.trace("Creating newsletter schedule: " + scheduleid + " ...");
								toSchedule = new NewsletterTask(scheduleid, delay, schedules);
							}
							
							toSchedule.start();
						}
						else
						{
							_log.trace("Reconfiguring newsletter schedule: " + scheduleid + " ...");
							toSchedule.reconfigDelay(delay);
						}
					}
					else
					{	
						if(toSchedule != null)
							toSchedule.interrupt(Task.DEAD);
					}
				}
				else if(toSchedule != null)
				{
					String msg = String.format("Killing newsletter schedule: %s ...", scheduleid);
					
					if (!Heartbeat.canLaunchProcesses())
						_log.info(msg);
					else
						_log.trace(msg);
					
					toSchedule.interrupt(Task.DEAD);
				}
			}
			else
			{
				_log.error("Scheduleid '" + scheduleid + "' does not exist in database");
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			}
		}
	}
	
	//Remote call for "schedule" method
	public void requestSchedule(String scheduleid, boolean enabled, String days, String hour, String type) throws ServiceError, NumberFormatException, PortalException, SystemException, UnsupportedEncodingException, ClientProtocolException, IOException, SecurityException, NoSuchMethodException
	{
		if(_log.isDebugEnabled())
		{
			if(Validator.isNotNull(scheduleid))
				_log.debug("Requesting newsletter schedule: " + scheduleid);
			else
				_log.debug("Requesting empty newsletter schedule");
		}
		
		if (!PropsValues.IS_PREVIEW_ENVIRONMENT && type.equals(IterKeys.NEWSLETTER_SCHEDULE_TYPE_DEFAULT))
		{
			schedule(scheduleid, enabled, days, hour);
		}
	}
	
	private NewsletterTask getSchedule(String scheduleid)
	{
		NewsletterTask result = null;
		synchronized(schedules)
		{
			NewsletterTask[] schedulesArray = new NewsletterTask[schedules.activeCount()];
			schedules.enumerate(schedulesArray);
			if(schedulesArray != null)
			{
				for(int i = 0; i < schedulesArray.length; i++)
				{
					if(schedulesArray[i].getName().equals(scheduleid))
					{
						result = schedulesArray[i];
						break;
					}
				}
			}
		}
		
		return result;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//																			 //
	//	Sintaxis de los días de la semana (D-1, L-2, M-3, X-4, J-5, V-6, S-7)	 //
	//	Siempre en orden ascendente. Por ejemplo: 2,4 serían lunes y miércoles	 //
	//																			 //
	//	Sintaxis de las horas (0-23)											 //
	//																			 //
	///////////////////////////////////////////////////////////////////////////////
	
	private long calculateDelay(String days, String hour)
	{
		long delay = -1;
		
		if(Validator.isNotNull(days) && Validator.isNotNull(hour))
		{
			Calendar now = Calendar.getInstance();
			Calendar next = Calendar.getInstance();
			
			int nowDay = now.get(Calendar.DAY_OF_WEEK);
			int nowHour = now.get(Calendar.HOUR_OF_DAY);
			int nextDay = -1;
			int nextHour = Integer.parseInt(hour);
			
			String[] scheduledDays = days.split(StringPool.COMMA);
			for(int i = 0; i < scheduledDays.length; i++)
			{
				int currentDay = Integer.parseInt(scheduledDays[i]);
				if((currentDay == nowDay && nextHour > nowHour) || (currentDay > nowDay))
				{
					nextDay = currentDay;
					break;
				}
			}
			
			if(nextDay == -1)
				nextDay = Integer.parseInt(scheduledDays[0]) + 7;
			
			next.add(Calendar.DAY_OF_MONTH, Math.abs(nextDay - nowDay));
			next.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour));
			next.set(Calendar.MINUTE, 0);
			next.set(Calendar.SECOND, 0);
			next.set(Calendar.MILLISECOND, 0);
			
			_log.trace("Delay calculation: " + now.getTime().toString() + " | " + next.getTime().toString());
			
			delay = next.getTimeInMillis() - now.getTimeInMillis();
		}
		
		return delay;
	}

	
	    //////////////////////////////
	   //				   		   //
	  //   SCHEDULE_SMTPSERVER    //
	 //					 	  	 //
	//////////////////////////////
	
	public Document getScheduleSMTPServers(String scheduleid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(scheduleid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Element rs = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SCHEDULE_NEWSLETTER, scheduleid)).getRootElement();
		String groupid =  XMLHelper.getTextValueOf(rs, "/rs/row/@groupid");
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SCHEDULE_SMTPSERVERS_SELECTED, scheduleid, groupid));
	}
	
	public Document addScheduleSMTPServers(String xmlData) throws ServiceError, DocumentException, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		Element dataRoot  = SAXReaderUtil.read(xmlData).getRootElement();

		List<Node> nodes  = dataRoot.selectNodes("//row");
		String scheduleid = XMLHelper.getTextValueOf(dataRoot, "@scheduleid");
		
		return addScheduleSMTPServers(scheduleid, nodes);
	}

	private Document addScheduleSMTPServers(String scheduleid, List<Node> nodes) throws ServiceError, DocumentException, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		String currentDate = SQLQueries.getCurrentDate();
		
		StringBuffer insertQuery = new StringBuffer();
		for(int i = 0; i < nodes.size(); i++)
		{
			Node node = nodes.get(i);
			
			String schedulesmtpid = XMLHelper.getTextValueOf(node, "@schedulesmtpid");
			if(Validator.isNull(schedulesmtpid))
				schedulesmtpid = SQLQueries.getUUID();
			
			insertQuery.append(	"('" + schedulesmtpid + "','" + 
									   scheduleid 	  + "','" + 
									   XMLHelper.getTextValueOf(node, "@smtpserverid") + "'," + 
									   (i + 1) + ",'" + 
									   currentDate + "',NULL)");
			
			if(i < nodes.size() - 1)
				insertQuery.append(",");
			else
				insertQuery.append(";");
		}

		PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_SCHEDULE_SMTPSERVER, scheduleid));
		
		if(insertQuery.length() > 0)
			PortalLocalServiceUtil.executeUpdateQuery(String.format(ADD_SCHEDULE_SMTPSERVER, insertQuery.toString()));
		
		return getScheduleSMTPServers(scheduleid);
	}

	
	    ///////////////////////
	   //				    //
	  //   SCHEDULE_USER   //
	 //					  //
	///////////////////////
	
	public String getScheduleUsers(String scheduleid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(scheduleid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SCHEDULE_USER_BY_SCHEDULE, scheduleid)).asXML();
	}
	
	public String addScheduleUser(String xmlData) throws ServiceError, DocumentException, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row");
		Node node = xpath.selectSingleNode(dataRoot);
		
		String scheduleid = XMLHelper.getTextValueOf(node, "@scheduleid");
		ErrorRaiser.throwIfNull(scheduleid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String userid = XMLHelper.getTextValueOf(node, "@userid");
		ErrorRaiser.throwIfNull(userid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String scheduleuserid = XMLHelper.getTextValueOf(node, "@scheduleuserid");
		if(Validator.isNull(scheduleuserid))
			scheduleuserid = SQLQueries.getUUID();
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(ADD_SCHEDULE_USER, scheduleuserid, scheduleid, userid, SQLQueries.getCurrentDate()));
		
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SCHEDULE_USER, scheduleuserid)).asXML();
	}
	
	public String deleteScheduleUsers(String xmlData) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row/@scheduleuserid");
		
		List<Node> nodes = xpath.selectNodes(dataRoot);
		ErrorRaiser.throwIfFalse((nodes != null && nodes.size() > 0), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_SCHEDULE_USER, TeaserMgr.getInClauseSQL(nodes)));
		
		return xmlData;
	}

	/**
	 * @param 				email
	 * @param 				delegationId
	 * @return 				Devuelve el userId del usuario asociado a correo electrónico
	 * @throws Exception 
	 */
	private String getAnonymousUserId(String email, Group group, boolean createIfNotExist) throws Exception
	{
		String userId = null;
		
		String sql = String.format(GET_ANONYMOUS_USER, email, IterUserTools.encryptGDPR(email), group.getDelegationId());
		_log.trace(sql);
		
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList(sql);
		if (!Validator.isNull(result))
		{
			userId = String.valueOf( ((Object[])result.get(0))[0] );
			String level = String.valueOf( ((Object[])result.get(0))[1] );
			
			ErrorRaiser.throwIfFalse(Validator.isNotNull(userId) && level.equals(UserConstants.USER_LEVEL_ANONYMOUS), IterErrorKeys.XYZ_EMAIL_REPEATED_ZYX, email);
		}
		else if (createIfNotExist)
		{
			MethodKey method = new MethodKey("com.protecmedia.iter.user.service.IterRegisterLocalServiceUtil", 
					 						 "registerAnonymousUser", String.class, Group.class);
			
			// No existe un usuario registrado con dicho correo para la delegación en cuestión. Se crea.
			userId = String.valueOf(PortalClassInvoker.invoke(false, method, email, group));
		}
		
		return userId;
	}
	
	private void checkLicenseAgreement(long groupId, boolean licenseAcepted) throws Exception
	{
		boolean mustAccept = false;
		
		// Si se ha configurado un mensaje para el License Agreement será obligatorio aceptar las condiciones 
		
		String config = getNewsletterConfig(groupId);
		if (Validator.isNotNull(config))
		{
			Document dom = SAXReaderUtil.read(config);
			String licAgreement = XMLHelper.getTextValueOf(dom, "/portlet-preferences/preference[name/text() = 'nl_license_agreement']/value");
			mustAccept = Validator.isNotNull(licAgreement);
		}
		
		// No es un AND para soportar el caso en que se acepte cuando no es necesario
		// licenseAcepted(TRUE) && mustAccept(FALSE) = FALSE => Caso válido (TRUE)
		// licenseAcepted(FALSE) && mustAccept(TRUE) = TRUE  => Caso no válido (FALSE)
		if (mustAccept)
			ErrorRaiser.throwIfFalse(licenseAcepted, IterErrorKeys.XYZ_E_NEWSLETTER_ACCEPT_LICENSE_ZYX);
	}

	private JSONObject getMyNewsletters(String userId) throws NoSuchMethodException, SecurityException
	{
		JSONArray scheduleIds = JSONFactoryUtil.createJSONArray();
		
		// Si el usuario no existe devuelve una lista vacia
		if (Validator.isNotNull(userId))
		{
			Element dataRoot = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SCHEDULES_BY_USER, userId)).getRootElement();
	
			XPath xpath = SAXReaderUtil.createXPath("//row/@scheduleid");
			List<Node> nodes = xpath.selectNodes(dataRoot);
			
			for(Node node:nodes)
			{
				JSONObject currentSchedule = JSONFactoryUtil.createJSONObject();
				currentSchedule.put("id", node.getStringValue());
				scheduleIds.put(currentSchedule);
			}
		}
		
		JSONObject result = JSONFactoryUtil.createJSONObject();
		result.put("options", scheduleIds);

		return result;
	}
	
	public String getMyNewsletters(HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		String userId = PayCookieUtil.getUserId(request);
		return getMyNewsletters(userId).toString();
	}
	
	public String getMyLightNewsletters(String email, boolean licenseAcepted, HttpServletRequest request, HttpServletResponse response)
	{
		JSONObject result = JSONFactoryUtil.createJSONObject();
		
		try
		{
			Group group = GroupLocalServiceUtil.getGroup(PortalUtil.getScopeGroupId(request));
			
			if (_log.isDebugEnabled())
				_log.debug( String.format("Checking light subscriptions in site %s for email %s", group.getName(), email) );
			
			// Se obtiene el usuario y se comprueba si no existe previamente, y en caso de existir que se anónimo
			String userId	  = getAnonymousUserId(email, group, false);
			checkLicenseAgreement(group.getGroupId(), licenseAcepted);
			
			result 			  = getMyNewsletters(userId);
			
			result.put("result", "OK");
		}
		catch (Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
			
			result.put("cause",  (e instanceof ServiceError) ? ((ServiceError)e).getErrorCode() : e.getMessage());
			result.put("result", "KO");
		}
		
		String resultData = result.toString();
		_log.debug(resultData);
		
		return resultData;
	}
	
	private static String IS_SUBSCRIBED = new StringBuilder(
		"SELECT COUNT(*) 			\n").append(
		"FROM schedule_user			\n").append(
		"  WHERE scheduleid = '%s'	\n").append(
		"    AND userid = '%s'		\n").toString();
		    
	private boolean isSubscribed(String userId, String optionid)
	{
		long numSubs = Long.parseLong( PortalLocalServiceUtil.executeQueryAsList( String.format(IS_SUBSCRIBED, optionid, userId) ).get(0).toString() );
		return numSubs > 0;
	}

	private void manageNewsletter(String userId, String optionid, boolean subscribe) throws ServiceError, IOException, SQLException
	{
		if (subscribe)
		{
			ErrorRaiser.throwIfFalse(Validator.isNotNull(userId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			if (!isSubscribed(userId, optionid))
			{
				PortalLocalServiceUtil.executeUpdateQuery(String.format(ADD_SCHEDULE_USER, SQLQueries.getUUID(), optionid, userId, SQLQueries.getCurrentDate()));
				NewslettersMetricsUtil.hit(optionid, StringUtil.apostrophe(userId), NewslettersMetricsUtil.HIT.USR_SUBSCRIPTION);
			}
		}
		else if (Validator.isNotNull(userId))
		{
			PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_SCHEDULE_USER_BY_SCHE_AND_USER, optionid, userId));
			NewslettersMetricsUtil.hit(optionid, StringUtil.apostrophe(userId), NewslettersMetricsUtil.HIT.USR_CANCEL_SUBSCRIPTION);
		}
	}
	
	public String manageNewsletter(String optionid, boolean subscribe, HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		JSONObject result = JSONFactoryUtil.createJSONObject();
		
		try
		{
			String userId = PayCookieUtil.getUserId(request);

			if(Validator.isNull(userId))
				_log.error("No usrid found");
			
			manageNewsletter(userId, optionid, subscribe);
			
			result.put("result", "OK");
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
			
			result.put("result", "KO");
		}
		
		return result.toString();
	}
	
	public String manageLightNewsletter(String email, boolean licenseAcepted, String optionid, boolean subscribe, HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		JSONObject result = JSONFactoryUtil.createJSONObject();
		
		try
		{
			Group group		  = GroupLocalServiceUtil.getGroup(PortalUtil.getScopeGroupId(request));
			
			if (_log.isDebugEnabled())
				_log.debug( String.format("Checking light subscriptions in site %s for email %s", group.getName(), email) );
			
			checkLicenseAgreement(group.getGroupId(), licenseAcepted);
			
			// Se obtiene el usuario y se comprueba si no existe previamente, y en caso de existir que se anónimo
			String userId = getAnonymousUserId(email, group, subscribe);
			
			manageNewsletter(userId, optionid, subscribe);
			
			// Si elimina la suscripción y era la última a la que estaba suscrito se elimina el usuario
			if (!subscribe && Validator.isNotNull(userId))
			{
				String sql = String.format(COUNT_SUBSCRIPTION, userId);
				boolean notSubscribed = Long.parseLong( PortalLocalServiceUtil.executeQueryAsList(sql).get(0).toString() ) == 0;
				
				if (notSubscribed)
				{
					String xmlData = SAXReaderUtil.read("<rs/>").getRootElement().addElement("row").addAttribute("usrid", userId).getDocument().asXML(); 
					MethodKey method = new MethodKey("com.protecmedia.iter.user.service.IterUserMngLocalServiceUtil", 
							 						 "deleteUsers", String.class);
	
					// No existe un usuario registrado con dicho correo para la delegación en cuestión. Se crea.
					String.valueOf(PortalClassInvoker.invoke(false, method, xmlData));
				}
			}
			
			result.put("result", "OK");
		}
		catch (Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
			
			result.put("cause",  (e instanceof ServiceError) ? ((ServiceError)e).getErrorCode() : e.getMessage());
			result.put("result", "KO");
		}
		
		String resultData = result.toString();
		_log.debug(resultData);
		
		return resultData;
	}

	

	//////////////////////////
	   //				       //
	  //   SCHEDULE_PRODUCT   //
	 //					  	 //
	//////////////////////////
	
	public Document getScheduleProducts(long groupId, String scheduleid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(scheduleid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SCHEDULE_PRODUCT_SELECTED, scheduleid, groupId));
	}
	
	public Document addScheduleProducts(String xmlData) throws ServiceError, DocumentException, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		Element dataRoot  = SAXReaderUtil.read(xmlData).getRootElement();

		List<Node> nodes  = dataRoot.selectNodes("//row");
		String scheduleid = XMLHelper.getTextValueOf(dataRoot, "@scheduleid");
		long groupId	  = XMLHelper.getLongValueOf(dataRoot, "@groupid");
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		// Se añaden los productos por productId, no necesitan el grupo
		return addScheduleProducts(scheduleid, groupId, nodes);
	}

	private Document addScheduleProducts(String scheduleid, long groupId, List<Node> nodes) throws ServiceError, DocumentException, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		String currentDate = SQLQueries.getCurrentDate();
		
		StringBuilder insertQuery = new StringBuilder();
		for(int i = 0; i < nodes.size(); i++)
		{
			Node node = nodes.get(i);
			
			String scheduleproductid = XMLHelper.getTextValueOf(node, "@scheduleproductid");
			if(Validator.isNull(scheduleproductid))
				scheduleproductid = SQLQueries.getUUID();
			
			// Si no existe nombre, tiene que existir productId
			String productId	= null;
			String productName 	= XMLHelper.getTextValueOf(node, "@name");
			if (Validator.isNull(productName))
			{
				productId = XMLHelper.getTextValueOf(node, "@productId");
				ErrorRaiser.throwIfNull(productId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
				productId = StringUtil.apostrophe(productId);
			}
			else
			{
				productId = String.format(GET_PRODUCT, productName, groupId);
			}
			insertQuery.append(	String.format("('%s','%s',%s,'%s',NULL)", scheduleproductid, scheduleid, productId, currentDate) );
			
			if(i < nodes.size() - 1)
				insertQuery.append(",");
			else
				insertQuery.append(";");
		}

		PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_SCHEDULE_PRODUCT, scheduleid));
		
		if(insertQuery.length() > 0)
			PortalLocalServiceUtil.executeUpdateQuery(String.format(ADD_SCHEDULE_PRODUCT, insertQuery.toString()));
		
		return getScheduleProducts(groupId, scheduleid);
	}
	
	
	    ////////////////////////////
	   //				       	 //
	  //   GET_PAGE_TEMPLATES   //
	 //					  	   //
	////////////////////////////
	
	public String getPageTemplates(String groupid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);				
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_PAGE_TEMPLATES, (PropsValues.IS_PREVIEW_ENVIRONMENT ? ", '&env=preview'" : ", '&env=live'"), groupid)).asXML();
	}
	
	public String getNewslettersXML(String groupid) throws ServiceError, DocumentException, IOException, SQLException, SecurityException, NoSuchMethodException 
	{
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String resultXML = "<newsletters/>";
		String[] requiredCDATA = new String[]{"name", "description"};
		
		String sql = String.format(GET_NEWSLETTERS_XML, groupid);
		_log.trace(sql);
		Document newslettersDoc = PortalLocalServiceUtil.executeQueryAsDom( sql, true, "newsletters", "newsletter", requiredCDATA );
		
		sql = String.format(GET_SCHEDULES_XML, groupid);
		_log.trace(sql);
		Document schedulesDoc = PortalLocalServiceUtil.executeQueryAsDom( sql, true, "rs", "option", requiredCDATA );
		
		XPath xpathNewsletter = SAXReaderUtil.createXPath("//newsletter");
		List<Node> nodesNewsletter = xpathNewsletter.selectNodes(newslettersDoc);
		for(Node nodeNewsletter:nodesNewsletter)
		{
			Element eleNewsletter = (Element)(nodeNewsletter);
			Element optionsElement = eleNewsletter.addElement("options");

			String currentNewsletterId = XMLHelper.getTextValueOf(eleNewsletter, "@id");
			XPath xpathSchedules = SAXReaderUtil.createXPath("//option[@newsletterid='" + currentNewsletterId + "']");
			List<Node> nodesSchedules = xpathSchedules.selectNodes(schedulesDoc);
			for(Node nodeSchedule:nodesSchedules)
			{
				String currentScheduleId = XMLHelper.getTextValueOf(nodeSchedule, "@id");
				Element eleScheduleDetached = (Element)nodeSchedule.detach();
				
				//Days
				Attribute attrDays = eleScheduleDetached.attribute("days");
				String[] days = attrDays.getValue().split(",");
				if(days != null && days.length > 0)
				{
					Element eleDays = eleScheduleDetached.addElement(attrDays.getName());
					for(String day:days)
					{
						Element eleDay = eleDays.addElement("day");
						eleDay.addAttribute("id", day);
					}
				}
				
				//Products
				String currentProducts = XMLHelper.getTextValueOf(PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_PRODUCTS_XML, currentScheduleId)), "/rs/row/@products");
				if(Validator.isNotNull(currentProducts))
					eleScheduleDetached.addAttribute("productlist", StringPool.APOSTROPHE + currentProducts + StringPool.APOSTROPHE);
				
				eleScheduleDetached.remove(eleScheduleDetached.attribute("days"));
				eleScheduleDetached.remove(eleScheduleDetached.attribute("newsletterid"));
				optionsElement.add(eleScheduleDetached);
			}
		}
		
		resultXML = newslettersDoc.asXML();
		_log.debug(resultXML);
		
		return resultXML;
	}
	
	public void importData(String data) throws DocumentException, PortalException, SystemException, SecurityException, NoSuchMethodException, ServiceError, IOException, SQLException
	{
		Element root 		= SAXReaderUtil.read( data ).getRootElement();
		long groupId 		= GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), XMLHelper.getStringValueOf(root, "@groupName")).getGroupId();
		boolean updtIfExist = GetterUtil.getBoolean(XMLHelper.getStringValueOf(root, "@updtIfExist"));
		
		// Se importan las newsletters
		List<Node> newsletterList = root.selectNodes("row");
		String[] newsletterNameList = XMLHelper.getStringValues(newsletterList, "@name");
		
		Document existingObjsDOM = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_EXISTING_NEWSLETTER, StringUtil.merge(newsletterNameList, "','"), groupId) );

		// Antes de comenar la importación se consulta si las newsletter o los calendarios YA existen en el entorno
		if (!updtIfExist)
		{
			// No exista ninguna newsletter
			String existingNewsletters = StringUtils.join(XMLHelper.getStringValues( existingObjsDOM.selectNodes("/rs/row[string-length(@name) > 0]/@name") ), ",");
			ErrorRaiser.throwIfFalse( Validator.isNull(existingNewsletters), IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, 
																			 String.format("%s(%s)", IterAdmin.IA_CLASS_NEWSLETTER, existingNewsletters));
		}
		
		for (int i = 0; i < newsletterList.size(); i++)
		{
			Element elem2Imp = (Element)newsletterList.get(i);
			
			// Si "updtIfExist" es falso solo puede haber llegado a este punto si NO existe ningún elemento en el LIVE
			boolean existNewsletter = updtIfExist ? (XMLHelper.getLongValueOf(existingObjsDOM, String.format("count(/rs/row[@name='%s'])", newsletterNameList[i])) > 0) : false;

			// Se crea el atributo groupid con el valor de este entorno
			elem2Imp.addAttribute("groupid", String.valueOf(groupId));

			Document newNewsletter = null;
			if (existNewsletter)
			{
				elem2Imp.addAttribute("newsletterid", XMLHelper.getStringValueOf(existingObjsDOM, String.format("/rs/row[@name='%s']/@newsletterid", newsletterNameList[i])));
				newNewsletter = SAXReaderUtil.read(updateNewsletter(elem2Imp));
			}
			else
			{
				elem2Imp.addAttribute("newsletterid", StringPool.BLANK);
				newNewsletter = SAXReaderUtil.read(addNewsletter(elem2Imp));
			}
			
			// Se borran los calendarios
			String newNewsletterid = XMLHelper.getStringValueOf(newNewsletter.getRootElement(), "//@newsletterid");
			PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_EXISTING_SCHEDULER_BY_NEWSLETTERID, newNewsletterid));
			
			// Se crean/actualizan los calendarios
			List<Node> scheduleList = elem2Imp.selectNodes("row");
			for (Node schedule : scheduleList)
			{
				((Element)schedule).addAttribute("groupid", String.valueOf(groupId));
				((Element)schedule).addAttribute("newsletterid", newNewsletterid);
				((Element)schedule).addAttribute("scheduleid", StringPool.BLANK);
				
				Document newSchedule = SAXReaderUtil.read(addScheduleNewsletter(schedule));
				String scheduleId = XMLHelper.getStringValueOf(newSchedule.getRootElement(), "//@scheduleid");
				
				// Para cada uno de los calendarios, se añaden los servidores SMTP
				addScheduleSMTPServers(scheduleId, schedule.selectNodes("servers/row"));
				
				// Para cada uno de los calendarios, se añaden los productos
				addScheduleProducts(scheduleId, groupId, schedule.selectNodes("products/row"));
			}
		}
	}

	private static final String SQL_GET_NEWSLETTER_SCHEDULES_BY_GROUP = new StringBuilder()
	.append("SELECT sn.scheduleid, CONCAT('[', n.name, '] ', sn.name) name \n")
	.append("FROM schedule_newsletter sn                                   \n")
	.append("INNER JOIN newsletter n ON n.newsletterid = sn.newsletterid   \n")
	.append("                       AND n.groupId = %d %s                  \n")
	.toString();
	
	private static final String SQL_GET_NEWSLETTER_SCHEDULES_ENABLED = "AND sn.enabled = true";
	
	public String getNewsletterSchedulesList(long groupId, boolean onlyEnabled) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		String filterEnabled = onlyEnabled ? SQL_GET_NEWSLETTER_SCHEDULES_ENABLED : StringPool.BLANK;
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_GET_NEWSLETTER_SCHEDULES_BY_GROUP, groupId, filterEnabled)).asXML();
	}
	
	public String getNewsletterConfig(long groupId) throws Exception
	{
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return GroupConfigTools.getGroupConfigField(groupId, "newsletterconfig");
	}
	
	public String setNewsletterConfig(long groupId, String config) throws Exception
	{
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		GroupConfigTools.setGroupConfigField(groupId, "newsletterconfig", config);
		return config;
	}
	
	
	/**
	 * 
	 */
	private class SMTPMgr
	{
		private List<Node> 	_smtps 			= null;
		private String 		_serverMode 	= null;
		private int 		_currentIndex 	= -1;
		
		private SMTPMgr(long groupId, List<Node> smtps, String serverMode)
		{
			checkSMTPEmail(groupId, smtps);
			
			_smtps 		= smtps;
			_serverMode = GetterUtil.getString(serverMode, ROUNDROBIN);
		}
		
		private void checkSMTPEmail(long groupId, List<Node> smtps)
		{
			for (int i = smtps.size()-1; i >= 0; i--)
			{
				Element smtp = (Element)smtps.get(i);
				String email = smtp.attributeValue("emailfrom");
				
				if ( !MailUtil.isValidEmailAddress(email) )
				{
					String trace = String.format("The SMTP server %s has an invalid email address %s", smtp.attributeValue("smtpserverid"), email);
					_log.error( trace );
					IterMonitor.logEvent(groupId, Event.WARNING, new Date(), trace);

					smtps.remove(smtp);
				}
			}
		}

		private boolean hasAvailableServer()
		{
			return !_smtps.isEmpty();
		}
		
		private Element getNextServer() throws ServiceError
		{
			ErrorRaiser.throwIfFalse( hasAvailableServer() );
			
			if (_currentIndex == -1)
				_currentIndex = 0;
			else if (_serverMode.equals(ROUNDROBIN))
				_currentIndex = (_currentIndex+1) % _smtps.size();
			
			return (Element)_smtps.get(_currentIndex);
		}
		
		private void removeCurrentServer() throws ServiceError
		{
			ErrorRaiser.throwIfFalse( hasAvailableServer() );
			
			_smtps.remove(_currentIndex);
			
			// El RounRobin disminuye el contador, pq este tipo de distribución avanza al sigt siempre.
			// Al borrar el n-ésimo, el nuevo n-ésimo será ya el siguiente, así que se resta para que
			// al avanzar se quede en la posición deseada
			if (_serverMode.equals(ROUNDROBIN))
				_currentIndex--;
			
			_currentIndex = Math.min(_currentIndex, _smtps.size()-1);
		}
	}
}
