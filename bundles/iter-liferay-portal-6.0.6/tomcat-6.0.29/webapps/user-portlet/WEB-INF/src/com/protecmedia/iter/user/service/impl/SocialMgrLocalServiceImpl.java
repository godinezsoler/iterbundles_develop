package com.protecmedia.iter.user.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.documentlibrary.NoSuchFolderException;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.model.DLFolderConstants;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFolderLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.persistence.DLFileEntryUtil;
import com.protecmedia.iter.base.cluster.Heartbeat;
import com.protecmedia.iter.base.service.util.IterAdmin;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.user.scheduler.SocialStatisticMgr;
import com.protecmedia.iter.user.service.base.SocialMgrLocalServiceBaseImpl;
import com.protecmedia.iter.user.util.UserUtil;
import com.protecmedia.iter.user.util.social.DisqusUtil;
import com.protecmedia.iter.user.util.social.FacebookUtil;
import com.protecmedia.iter.user.util.social.GooglePlusUtil;
import com.protecmedia.iter.user.util.social.SocialNetworkUtil;
import com.protecmedia.iter.user.util.social.TwitterUtil;
import com.protecmedia.iter.xmlio.service.util.CDATAUtil;

@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class})
public class SocialMgrLocalServiceImpl extends SocialMgrLocalServiceBaseImpl
{	
	private static Log _log = LogFactoryUtil.getLog(SocialMgrLocalServiceImpl.class);
	
	private static final String IMAGE_FOLDER_SUFIX 							= "_login_with_image";
	
	private static final String[] PROFILE_FIELDS_NO_SHOWN_ON_SOCIAL_CONN;
	static
	{
		List<String> NO_SHOWN_ON_SOCIAL_CONN = new ArrayList<String>();
		NO_SHOWN_ON_SOCIAL_CONN.addAll( UserUtil.PRF_HIDE_FIELDS 	);
		NO_SHOWN_ON_SOCIAL_CONN.add(	UserUtil.PRF_FIELD_USRNAME	);
		NO_SHOWN_ON_SOCIAL_CONN.add(	UserUtil.PRF_FIELD_USREMAIL	);
		NO_SHOWN_ON_SOCIAL_CONN.add(	UserUtil.PRF_FIELD_ABOID	);
		NO_SHOWN_ON_SOCIAL_CONN.add(	UserUtil.PRF_FIELD_USRPWD	);
		
		PROFILE_FIELDS_NO_SHOWN_ON_SOCIAL_CONN = NO_SHOWN_ON_SOCIAL_CONN.toArray(new String[0]);
	}
	
	
	private static final String GET_SOCIAL_CONFIG_BY_GROUP = new StringBuilder()
	.append("SELECT isl.socialname, itersocialscope.scopename, c.collectstats, c.publicKey, c.secretKey, c.applicationname, ")
    .append("c.loginwith, c.loginwithfileentry, c.loginwithtooltip, c.shortener ")
    .append("FROM itersocialconfig c ")
    .append("INNER JOIN itersocial isl ON isl.itersocialid = c.itersocialid ")
    .append("INNER JOIN itersocialscope ON itersocialscope.itersocialscopeid = c.itersocialscopeid ")
    .append("WHERE groupid=%d ")
    .toString();
	

	private static final String _GET_SOCIALCONFIGFIELD_BY_GROUP = new StringBuilder(
		"SELECT itersocialconfig.itersocialconfigid, itersocialfield.itersocialfieldid, itersocialconfigfieldid, 					\n").append(
		"		socialname, scopename, itersocialfield.fieldname, UserProfile.fieldname profileFieldName							\n").append(
		"FROM itersocial																											\n").append(
		"INNER JOIN itersocialconfig		ON itersocialconfig.itersocialid = itersocial.itersocialid								\n").append(
		"INNER JOIN itersocialscope 		ON itersocial.itersocialid = itersocialscope.itersocialid								\n").append(
		"INNER JOIN itersocialfield 		ON itersocialscope.itersocialscopeid = itersocialfield.itersocialscopeid				\n").append(
		"%1$s JOIN itersocialconfigfield 	ON (	itersocialconfigfield.itersocialconfigid = itersocialconfig.itersocialconfigid	\n").append( 
		"										AND itersocialconfigfield.itersocialfieldid = itersocialfield.itersocialfieldid)	\n").append(
		"%1$s JOIN UserProfile				ON itersocialconfigfield.profilefieldid = UserProfile.profilefieldid					\n").append(
		"	WHERE itersocialconfig.groupId = %%d																					\n").toString();	

	/** Todos los itersocialconfigfield del grupo **/
	private static final String GET_SOCIALCONFIGFIELD_BY_GROUP 	= String.format(_GET_SOCIALCONFIGFIELD_BY_GROUP, "INNER");
	
	/** Todos los itersocialconfig y sus posibles itersocialconfigfield del grupo **/
	private static final String GET_SOCIALCONFIGS_BY_GROUP  	= String.format(_GET_SOCIALCONFIGFIELD_BY_GROUP, "LEFT");
	
	//SOCIAL NETWORK
	private static final String GET_SOCIAL_ID 						= "SELECT itersocialid FROM itersocial WHERE socialname='%s'";

	//SOCIAL NETWORK CONFIG
	private static final String GET_SOCIAL_CONFIG					= new StringBuilder(
			"SELECT * FROM	(																																			\n").append(
			"					SELECT 	c.itersocialconfigid, c.itersocialid, c.itersocialscopeid, s.socialname, 														\n").append(
			"							c.collectstats, c.publicKey, c.secretKey, c.applicationname,																	\n").append(
			"							c.groupid, c.loginwith, c.loginwithtooltip, FALSE as useDisqusCfg,																\n").append(
			"							CAST(CONCAT('/documents/', d.groupId, '/', d.folderId, '/', d.title%1$s) AS CHAR CHARACTER SET utf8) loginwithimagepath,		\n").append(
			"							CASE WHEN IFNULL(shortener, '') = ''  THEN 'false' ELSE 'true' END AS shortenlinks, shortener									\n").append(
			"					FROM itersocialconfig c																													\n").append(
			"					LEFT JOIN DLFileEntry d ON c.loginwithfileentry = d.uuid_																				\n").append(
			"					INNER JOIN itersocial s ON s.itersocialid = c.itersocialid																				\n").append(
			"						WHERE s.socialname != 'disqus'																										\n").append(
			"																																							\n").append(
			"					UNION ALL																																\n").append(
			"																																							\n").append(
			"					SELECT 	c.itersocialconfigid, c.itersocialid, c.itersocialscopeid, 'disqus' socialname, IFNULL(c.collectstats, 0) collectstats,			\n").append( 
			"							q.publicKey, q.secretKey, q.shortname applicationname, q.groupId, 																\n").append(
			"							IFNULL(c.loginwith, 0), c.loginwithtooltip, IFNULL(q.useDisqusCfg, FALSE), 														\n").append(
			"							CAST(CONCAT('/documents/', d.groupId, '/', d.folderId, '/',  d.title%1$s) AS CHAR CHARACTER SET utf8) loginwithimagepath,		\n").append(
			"							'false' shortenlinks, '' shortener																								\n").append(
			"					FROM disqusconfig q 																													\n").append(
			"					LEFT JOIN itersocial s ON s.socialname='disqus'																							\n").append(
			"					LEFT JOIN itersocialconfig c ON c.groupid = q.groupid AND s.itersocialid = c.itersocialid												\n").append(
			"					LEFT JOIN DLFileEntry d ON c.loginwithfileentry = d.uuid_ 																				\n").append(
			"				) result																																	\n").toString();
	

	private static final String GET_SOCIAL_CONFIG_BY_NAME_GROUP		= GET_SOCIAL_CONFIG + "WHERE groupid=%2$s AND socialname='%3$s' LIMIT 1";
	
	private static final String GET_SOCIAL_CONFIG_BY_ID 			= GET_SOCIAL_CONFIG + "WHERE itersocialconfigid = '%2$s' LIMIT 1";
	
	private static final String GET_SOCIAL_CONFIG_FORSTATISTIC 		= "SELECT collectstats FROM itersocialconfig  WHERE itersocialconfigid = '%s'" ;
	
	private static final String INSERT_SOCIAL_CONFIG 				= "INSERT INTO itersocialconfig(itersocialconfigid, itersocialid, itersocialscopeid, " 										+
																									"collectstats, publicKey, secretKey, applicationname, "										+
																									"groupid, loginwith, loginwithfileentry, " 													+
																									"loginwithtooltip, modifieddate, publicationdate, isStarted, shortener) "	+
																									"VALUES ('%s', '%s', '%s', %s, %s, %s, %s, %s, %s, %s, %s, '%s', NULL, 0, %s)";

	private static final String UPDATE_SOCIAL_CONFIG 				= "UPDATE itersocialconfig SET itersocialid='%s', itersocialscopeid='%s', collectstats=%s, "					+
																								   "publicKey=%s, secretKey=%s, groupid=%s, loginwith=%s, " 						+
																								   "loginwithfileentry=%s, loginwithtooltip=%s, modifieddate='%s', " 				+
																								   "applicationname=%s, shortener=%s  "												+
																								   "WHERE itersocialconfigid='%s'";
	
	private static final String UPDATE_SOCIAL_SCOPE_CONFIG 			= "UPDATE itersocialconfig SET itersocialscopeid='%s' " 														+
																			"WHERE itersocialconfigid='%s'";
	
	private static final String UPDT_SOCIAL_LOGINWITHDLFILEENTRY 		= "UPDATE itersocialconfig SET loginwithfileentry='%s' WHERE itersocialconfigid = '%s'"; 
	
	//SOCIAL SCOPE
	private static final String GET_DEFAULT_SCOPE_ID 				= "SELECT itersocialscopeid FROM itersocialscope c " 															+
																			"INNER JOIN itersocial s ON s.itersocialid = c.itersocialid " 											+
																			"WHERE s.socialname='%s' AND c.scopename='default'";
	
	private static final String GET_SCOPES_BY_SOCIALNAME 			= "SELECT * FROM itersocialscope c " 																			+
																			"INNER JOIN itersocial s ON s.itersocialid=c.itersocialid " 											+
																			"WHERE s.socialname='%s'";
	
	//USER PROFILE
	private static final String GET_PROFILE_FIELDS 					= String.format(new StringBuilder(	
		"SELECT up.fieldname, up.profilefieldid, IF (structured, 'system', 'user') fieldclass 	\n").append(
		"FROM form f 																			\n").append(
		"INNER JOIN formtab ft ON ft.formid=f.formid 											\n").append(
		"INNER JOIN formfield ff ON ff.tabid=ft.tabid 											\n").append(
		"INNER JOIN userprofile up ON up.profilefieldid=ff.profilefieldid 						\n").append(
		"	WHERE f.formtype = 'registro' 														\n").append(
		"		AND f.groupid=%%s																\n").append(
		"		AND up.fieldname NOT IN ('%s')").toString(), StringUtils.join(PROFILE_FIELDS_NO_SHOWN_ON_SOCIAL_CONN, "','"));
	
	//SOCIAL FIELDS
	private static final String GET_FIELDS_BY_SCOPEID 				= "SELECT * FROM itersocialfield f WHERE f.itersocialscopeid='%s' OR f.itersocialscopeid IN " 					+
																	  		"(SELECT c.itersocialscopebaseid FROM itersocialscope c WHERE c.itersocialscopeid='%s')";
	
	private static final String GET_PROFILE_SOCIAL_FIELDS			= "SELECT * FROM itersocialconfigfield WHERE itersocialconfigid='%s'";
	
	private static final String GET_PROFILE_SOCIAL_FIELD_BY_ID		= "SELECT * FROM itersocialconfigfield WHERE itersocialconfigfieldid='%s'";
	
	private static final String INSERT_PROFILE_SOCIAL_FIELD			= "INSERT INTO itersocialconfigfield (itersocialconfigfieldid, itersocialconfigid, itersocialfieldid, " 		+
																										  "profilefieldid, modifieddate, publicationdate) VALUES (" 				+
																										  "'%s', '%s', '%s', '%s', '%s', NULL)";
	
	private static final String UPDATE_PROFILE_SOCIAL_FIELD			= "UPDATE itersocialconfigfield SET itersocialconfigid='%s', itersocialfieldid='%s', profilefieldid='%s', " 	+
																										"modifieddate='%s' WHERE itersocialconfigfieldid='%s'";
	
	private static final String CHECK_SOCIAL_FIELDS_CONNECTIONS 	= "SELECT COUNT(*) FROM itersocialconfigfield WHERE itersocialconfigid='%s'";
	
	private static final String DELETE_SOCIAL_FIELDS_CONNECTIONS 	= "DELETE FROM itersocialconfigfield WHERE itersocialconfigid='%s'";
	
	private static final String DELETE_SOCIAL_FIELD_CONNECTION 		= "DELETE FROM itersocialconfigfield WHERE itersocialconfigfieldid='%s'";

	
	private static ThreadGroup tasks = new ThreadGroup("SOCIALSTATISTICS");
	
	
	private String getSocialId(String socialname) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(socialname, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return XMLHelper.getTextValueOf(PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SOCIAL_ID, socialname)), "/rs/row/@itersocialid");
	}
	
	private String getDefaultScope(String socialname) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(socialname, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return XMLHelper.getTextValueOf(PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_DEFAULT_SCOPE_ID, socialname)), "/rs/row/@itersocialscopeid");
	}
	
	private String getSocialConfigById(String itersocialconfigid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(itersocialconfigid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SOCIAL_CONFIG_BY_ID, StringPool.BLANK, itersocialconfigid), new String[]{"loginwithtooltip"}).asXML();
	}
	
	private boolean checkSocialFieldsHasConnections(String itersocialconfigid) throws ServiceError
	{
		boolean hasConnections = false;
		
		ErrorRaiser.throwIfNull(itersocialconfigid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		List<Object> results = PortalLocalServiceUtil.executeQueryAsList(String.format(CHECK_SOCIAL_FIELDS_CONNECTIONS, itersocialconfigid));
		if(results != null && results.size() == 1 && GetterUtil.getInteger(results.get(0).toString()) > 0)
			hasConnections = true;
			
		return hasConnections;
	}
	
	private long getFolderId(long groupId, long userId, String type) throws PortalException, SystemException, ServiceError
	{
	    DLFolder dlFolder = null;
	    
	    String folderName = type + IMAGE_FOLDER_SUFIX;
	    
	    try
	    {
	    	dlFolder = DLFolderLocalServiceUtil.getFolder(groupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, folderName);
	    }
	    catch (NoSuchFolderException nsfe)
	    {
	    	dlFolder = DLFolderLocalServiceUtil.addFolder(userId, groupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, folderName, StringPool.BLANK, new ServiceContext());
	    }
	    
	    ErrorRaiser.throwIfNull(dlFolder);
	    
	    return dlFolder.getFolderId();
	}
	
	public String deleteLoginWithFileEntry(String xmlData) throws Exception
	{
		String result = xmlData;
		Document xmlDataDoc = SAXReaderUtil.read(xmlData);
		
		String socialname = XMLHelper.getTextValueOf(xmlDataDoc, "/rs/row/@socialname");
		ErrorRaiser.throwIfNull(socialname, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String groupid = XMLHelper.getTextValueOf(xmlDataDoc, "/rs/row/@groupid");
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		long groupIdLong = Long.parseLong(groupid);
		long userId = GroupMgr.getDefaultUserId();
		long folderId = getFolderId(groupIdLong, userId, socialname);
		
		List<DLFileEntry> fileEntries = DLFileEntryLocalServiceUtil.getFileEntries(groupIdLong, folderId);
		if (fileEntries != null && fileEntries.size() > 0)
		{
			for(DLFileEntry fileEntry:fileEntries)
				DLFileEntryLocalServiceUtil.deleteDLFileEntry(fileEntry);
		}
		
		return result;
	}
	
	public String uploadLoginWithFileEntry(HttpServletRequest request, HttpServletResponse response, InputStream is, long groupId, String socialType, String itersocialconfigid) throws Exception
	{	
		String result = "";
		
		@SuppressWarnings("unchecked")
		Iterator<FileItem> files = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request).iterator();
		while (files.hasNext())
		{
	    	FileItem currentFile = files.next();
	    	if (!currentFile.isFormField())
	    	{
	    		String fileName = currentFile.getName();
	    		ErrorRaiser.throwIfFalse(Validator.isNotNull(fileName), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
	    		
	    		long imgSize = currentFile.getSize();

	    		is = currentFile.getInputStream();
	    		result =  setLoginFileEntry(groupId, fileName, imgSize, is, socialType, itersocialconfigid);
	    		
	    		if (!PropsValues.IS_PREVIEW_ENVIRONMENT)
	    			result = result.concat("?env=live");
				else
	    			result = result.concat("?env=preview");
	    		
	    		break;
	    	}
		}
		return result;
	}
	
	private String setLoginFileEntry(long groupId, String fileName, long imgSize, InputStream is, String socialType, String itersocialconfigid) throws ServiceError, PortalException, SystemException, IOException, SQLException
	{
		ErrorRaiser.throwIfNull(fileName, 		IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse(imgSize > 0, 	IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(socialType, 	IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		int indexPreriod = fileName.indexOf(StringPool.PERIOD);
		boolean hasExtension = indexPreriod > 0 && indexPreriod < fileName.length();
		ErrorRaiser.throwIfFalse(hasExtension, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		String imgExtension = fileName.substring(indexPreriod, fileName.length());
		String imgTitle		= SQLQueries.getUUID() + imgExtension;
		long userId = GroupMgr.getDefaultUserId();
		long folderId = getFolderId(groupId, userId, socialType);
		
		List<DLFileEntry>imgList = DLFileEntryLocalServiceUtil.getFileEntries(groupId, folderId);
		
		DLFileEntry dlfileEntry = null;
		if (imgList.size() > 0)
		{
			DLFileEntry oldDlfileEntry = imgList.get(0);
			dlfileEntry = DLFileEntryLocalServiceUtil.addFileEntry(userId, groupId, folderId, imgTitle, oldDlfileEntry.getDescription(), StringPool.BLANK, StringPool.BLANK, is, imgSize, new ServiceContext());
			DLFileEntryLocalServiceUtil.deleteFileEntryNoHook(oldDlfileEntry);
		}
		else
		{
			dlfileEntry = DLFileEntryLocalServiceUtil.addFileEntry(userId, groupId, folderId, imgTitle, fileName, StringPool.BLANK, StringPool.BLANK, is, imgSize, new ServiceContext());
		}

		dlfileEntry.setTitle( dlfileEntry.getUuid() + imgExtension );
		DLFileEntryUtil.update(dlfileEntry, false);

		// Si en BBDD hay una configuración de la red social asociada al grupo se actualiza su loginwithfileentry
		if ( Validator.isNotNull(itersocialconfigid) )
			PortalLocalServiceUtil.executeUpdateQuery( String.format(UPDT_SOCIAL_LOGINWITHDLFILEENTRY, dlfileEntry.getUuid(), itersocialconfigid) );
		
		return new StringBuilder("/documents").append(StringPool.SLASH).append(groupId).append(StringPool.SLASH).append(folderId).append(StringPool.SLASH).append(dlfileEntry.getTitle()).toString();
	}
	
	public String getSocialConfig(String xmlData) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String socialname = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@socialname");
		ErrorRaiser.throwIfNull(socialname, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String groupid = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@groupid");
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		return executeSocialConfigQuery(groupid, socialname, true).asXML();
	}
	
	private Document executeSocialConfigQuery(String groupid, String socialname, boolean addEnvQS) throws SecurityException, NoSuchMethodException
	{
		String envQS = StringPool.BLANK;
		if(addEnvQS)
			envQS = PropsValues.IS_PREVIEW_ENVIRONMENT ? ", '?env=preview'" : ", '?env=live'";
		
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SOCIAL_CONFIG_BY_NAME_GROUP, envQS, groupid, socialname), new String[]{"loginwithtooltip"});
	}
	
	public String updateScopeSocialConfig(String xmlData) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, IOException, SQLException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String itersocialconfigid = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@itersocialconfigid");
		ErrorRaiser.throwIfNull(itersocialconfigid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String itersocialscopeid = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@itersocialscopeid");
		ErrorRaiser.throwIfNull(itersocialscopeid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		boolean checkreferences = GetterUtil.getBoolean(XMLHelper.getTextValueOf(dataRoot, "/rs/row/@checkreferences"), true);
		if(checkreferences)
			ErrorRaiser.throwIfFalse(!checkSocialFieldsHasConnections(itersocialconfigid), IterErrorKeys.XYZ_ITR_E_SOCIAL_CONFIG_SCOPE_HAS_CONNECTIONS_ZYX);
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_SOCIAL_FIELDS_CONNECTIONS, itersocialconfigid));

		PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_SOCIAL_SCOPE_CONFIG, itersocialscopeid, itersocialconfigid));
		
		return getSocialConfigById(itersocialconfigid);
	}
	
	public String getProfileAndScopes(String xmlData) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, IOException, SQLException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String socialname = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@socialname");
		ErrorRaiser.throwIfNull(socialname, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String groupid = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@groupid");
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Element profileRoot = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_PROFILE_FIELDS, groupid)).getRootElement();
		XPath xpath = SAXReaderUtil.createXPath("//row");
		List<Node> profileNodes = xpath.selectNodes(profileRoot);
		
		Element scopeRoot = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SCOPES_BY_SOCIALNAME, socialname)).getRootElement();
		xpath = SAXReaderUtil.createXPath("//row");
		List<Node> scopeNodes = xpath.selectNodes(scopeRoot);
		
		Element rs = SAXReaderUtil.read("<rs/>").getRootElement();
		
		Element row = rs.addElement("profile");
		for(int i = 0; i < profileNodes.size(); i++)
			row.add(profileNodes.get(i).detach());
		
		row = rs.addElement("scope");
		for(int i = 0; i < scopeNodes.size(); i++)
			row.add(scopeNodes.get(i).detach());
	
		return rs.asXML();
	}
	
	public String updateProfileSocialField(String xmlData) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, IOException, SQLException
	{
		String result = null;
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String itersocialconfigid = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@itersocialconfigid");
		ErrorRaiser.throwIfNull(itersocialconfigid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String profilefieldid = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@profilefieldid");
		ErrorRaiser.throwIfNull(itersocialconfigid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String itersocialconfigfieldid = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@itersocialconfigfieldid");
		String itersocialfieldid = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@itersocialfieldid");
		
		if(Validator.isNotNull(itersocialconfigfieldid) && Validator.isNull(itersocialfieldid))
		{
			PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_SOCIAL_FIELD_CONNECTION, itersocialconfigfieldid));
			result = xmlData;
		}
		else
		{
			if(Validator.isNull(itersocialconfigfieldid))
			{
				itersocialconfigfieldid = SQLQueries.getUUID();
				PortalLocalServiceUtil.executeUpdateQuery(String.format(INSERT_PROFILE_SOCIAL_FIELD, itersocialconfigfieldid, itersocialconfigid, 
																							  		 itersocialfieldid, profilefieldid, 
																							  		 SQLQueries.getCurrentDate()));
			}
			else
			{
				PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_PROFILE_SOCIAL_FIELD, itersocialconfigid, itersocialfieldid, profilefieldid, 
																									 SQLQueries.getCurrentDate(), itersocialconfigfieldid));
			}
		
			result = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_PROFILE_SOCIAL_FIELD_BY_ID, itersocialconfigfieldid)).asXML();
		}
		
		return result;
	}
	
	public String getProfileSocialFieldsConnections(String xmlData) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, IOException, SQLException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String itersocialconfigid = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@itersocialconfigid");
		ErrorRaiser.throwIfNull(itersocialconfigid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String itersocialscopeid = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@itersocialscopeid");
		ErrorRaiser.throwIfNull(itersocialscopeid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Element socialFieldRoot = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_FIELDS_BY_SCOPEID, itersocialscopeid, itersocialscopeid)).getRootElement();
		XPath xpath = SAXReaderUtil.createXPath("//row");
		List<Node> socialFieldNodes = xpath.selectNodes(socialFieldRoot);
		
		Element rs = SAXReaderUtil.read("<rs/>").getRootElement();
		
		Element row = rs.addElement("socialfields");
		for(int i = 0; i < socialFieldNodes.size(); i++)
			row.add(socialFieldNodes.get(i).detach());
		
		Element profileSocialFieldRoot = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_PROFILE_SOCIAL_FIELDS, itersocialconfigid)).getRootElement();
		xpath = SAXReaderUtil.createXPath("//row");
		List<Node> profileSocialFieldNodes = xpath.selectNodes(profileSocialFieldRoot);

		row = rs.addElement("profilesocialfields");
		for(int i = 0; i < profileSocialFieldNodes.size(); i++)
			row.add(profileSocialFieldNodes.get(i).detach());
	
		return rs.asXML();
	}
	
	public String getSocialButtonsHTML(String groupid) throws SecurityException, NoSuchMethodException, ServiceError
	{
		Document disqusDoc     = executeSocialConfigQuery(groupid, SocialNetworkUtil.SOCIAL_NAME_DISQUS, false);
		Document facebookDoc   = executeSocialConfigQuery(groupid, SocialNetworkUtil.SOCIAL_NAME_FACEBOOK, false);
		Document googleplusDoc = executeSocialConfigQuery(groupid, SocialNetworkUtil.SOCIAL_NAME_GOOGLEPLUS, false);
		Document twitterDoc    = executeSocialConfigQuery(groupid, SocialNetworkUtil.SOCIAL_NAME_TWITTER, false);

		StringBuilder html = new StringBuilder();
		html.append(getSocialInnerHTML(disqusDoc,     SocialNetworkUtil.SOCIAL_NAME_DISQUS));
		html.append(getSocialInnerHTML(facebookDoc,   SocialNetworkUtil.SOCIAL_NAME_FACEBOOK));
		html.append(getSocialInnerHTML(twitterDoc,    SocialNetworkUtil.SOCIAL_NAME_TWITTER));
		html.append(getSocialInnerHTML(googleplusDoc, SocialNetworkUtil.SOCIAL_NAME_GOOGLEPLUS_SHORT));
		
		if(html.length() > 0)
		{
			html.insert(0, "<div class='block_socials_login'><div class='socials_login'>");
			html.append("</div></div>");
		}
		
		return html.toString();
	}
	
	private String getSocialImageHTML(Document socialDoc, String socialname) throws SecurityException, NoSuchMethodException, ServiceError
	{
		StringBuilder html = new StringBuilder();
		if(Validator.isNotNull(socialname))
		{
			html.append("<div class='socials_").append(socialname).append("_icon_login'>");
			
			String loginwithimagepath = XMLHelper.getTextValueOf(socialDoc, "rs/row/@loginwithimagepath");
			if(Validator.isNotNull(loginwithimagepath))
				html.append("<img src='").append(loginwithimagepath).append("'></img>");

			html.append("</div>");
		}
		
		return html.toString();
	}
	
	private String getSocialTooltipHTML(Document socialDoc, String socialname) throws SecurityException, NoSuchMethodException, ServiceError
	{
		StringBuilder html = new StringBuilder();
		if(Validator.isNotNull(socialname))
		{
			html.append("<div class='socials_").append(socialname).append("_text_login'>");
			
			String loginwithtooltip = GetterUtil.getString(
					XMLHelper.getTextValueOf(socialDoc, "rs/row/loginwithtooltip"), StringPool.BLANK);

			html.append(StringEscapeUtils.escapeHtml(loginwithtooltip));
			html.append("</div>");
		}
		
		return html.toString();
	}
	
	private String getSocialInnerHTML(Document socialDoc, String socialname) throws SecurityException, NoSuchMethodException, ServiceError
	{
		StringBuilder html = new StringBuilder();
		if(Validator.isNotNull(socialname))
		{
			boolean loginwith = GetterUtil.getBoolean(XMLHelper.getTextValueOf(socialDoc, "rs/row/@loginwith"));
			if(loginwith)
			{
				html.append("<div class='socials_").append(socialname).append("_login'><a href='");
				
				if(socialname.equalsIgnoreCase(SocialNetworkUtil.SOCIAL_NAME_DISQUS))
					html.append(DisqusUtil.SERVLET_PATH);
				else if(socialname.equalsIgnoreCase(SocialNetworkUtil.SOCIAL_NAME_FACEBOOK))
					html.append(FacebookUtil.SERVLET_PATH);
				else if(socialname.equalsIgnoreCase(SocialNetworkUtil.SOCIAL_NAME_GOOGLEPLUS_SHORT))
					html.append(GooglePlusUtil.SERVLET_PATH);
				else if(socialname.equalsIgnoreCase(SocialNetworkUtil.SOCIAL_NAME_TWITTER))
					html.append(TwitterUtil.SERVLET_PATH);
				
				// ITER-769 Las URLs de loginWith se indexan aunque el robots.txt las hayan bloqueado
				// http://jira.protecmedia.com:8080/browse/ITER-769?focusedCommentId=28107&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-28107
				html.append("' rel='nofollow' >");
				
				html.append(getSocialImageHTML(socialDoc, socialname));
				html.append(getSocialTooltipHTML(socialDoc, socialname));
				html.append("</a></div>");
			}
		}
		return html.toString();
	}
	
	public String setSocialConfig(String xmlData) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, IOException, SQLException, NumberFormatException, PortalException, SystemException
	{
		_log.trace("In setSocialConfig");
		
		if (_log.isDebugEnabled())
			_log.debug(new StringBuilder("XML received:\n").append(xmlData));
		
		ErrorRaiser.throwIfFalse(Heartbeat.canLaunchProcesses(), IterErrorKeys.XYZ_ITR_E_SOCIAL_ISNOT_MASTER_SERVER_ZYX);
		
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String socialname = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@socialname");
		ErrorRaiser.throwIfNull(socialname, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String itersocialid = getSocialId(socialname);

		String itersocialscopeid = GetterUtil.getString2(XMLHelper.getTextValueOf(dataRoot, "/rs/row/@itersocialscopeid"), getDefaultScope(socialname));
		
		String collectstats = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@collectstats");
		ErrorRaiser.throwIfNull(collectstats, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String publicKey = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@publicKey");
		if(Validator.isNotNull(publicKey))
		{
			if(socialname.equals(SocialStatisticMgr.SOCIALSTATISTIC_DISQUS))
				publicKey = "NULL";
			else
				publicKey = StringPool.APOSTROPHE + publicKey + StringPool.APOSTROPHE;
		}
		
		String secretKey = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@secretKey");
		if(Validator.isNotNull(secretKey))
		{
			if(socialname.equals(SocialStatisticMgr.SOCIALSTATISTIC_DISQUS))
				secretKey = "NULL";
			else
				secretKey = StringPool.APOSTROPHE + secretKey + StringPool.APOSTROPHE;
		}
		
		String applicationname = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@applicationname");
		ErrorRaiser.throwIfNull(applicationname, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		if(socialname.equals(SocialStatisticMgr.SOCIALSTATISTIC_DISQUS))
			applicationname = "NULL";
		else
			applicationname = StringPool.APOSTROPHE + applicationname + StringPool.APOSTROPHE;
		
		String groupid = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@groupid");
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String loginwith = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@loginwith");
		ErrorRaiser.throwIfNull(loginwith, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String loginwithimagepath = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@loginwithimagepath");
		String loginwithtooltip = XMLHelper.getTextValueOf(dataRoot, "/rs/row/loginwithtooltip");
		
		boolean shortenlinks = GetterUtil.getBoolean(XMLHelper.getTextValueOf(dataRoot, "/rs/row/@shortenlinks"), false);
		String shortener = StringPool.NULL;
		if (shortenlinks)
			shortener = StringUtil.apostrophe(XMLHelper.getTextValueOf(dataRoot, "/rs/row/@shortener", StringPool.BLANK));
		
		boolean checkLoginWith = (Validator.isNotNull(loginwithimagepath) || Validator.isNotNull(loginwithtooltip));
		if(GetterUtil.getBoolean(loginwith))
			ErrorRaiser.throwIfFalse(checkLoginWith, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		String loginwithfileentry = null;
		if(Validator.isNotNull(loginwithimagepath))
		{
			long folderId = getFolderId(Long.parseLong(groupid), GroupMgr.getDefaultUserId(), socialname);
			
			String[] splitSlash = loginwithimagepath.split(StringPool.SLASH);
			String lastElement = splitSlash[ splitSlash.length -1 ].toString();
			int index = lastElement.indexOf("?");
			String titleimg = index != -1? lastElement.substring(0, index)  : lastElement;
			
			
			DLFileEntry img = DLFileEntryLocalServiceUtil.getFileEntryByTitle(Long.parseLong(groupid), folderId, titleimg);
			if(img != null )
				loginwithfileentry = StringPool.APOSTROPHE + img.getUuid() + StringPool.APOSTROPHE;
		}
		
		if(Validator.isNotNull(loginwithtooltip))
		{
			loginwithtooltip = StringPool.APOSTROPHE + 
							   StringEscapeUtils.escapeSql(CDATAUtil.strip(loginwithtooltip)) + 
							   StringPool.APOSTROPHE;
		}
		
		String itersocialconfigid = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@itersocialconfigid");
		if(Validator.isNull(itersocialconfigid))
		{
			itersocialconfigid = SQLQueries.getUUID();
			
			String sql = String.format(INSERT_SOCIAL_CONFIG, itersocialconfigid, itersocialid, itersocialscopeid, 
					  				   collectstats, publicKey, secretKey, applicationname,
					  				   groupid, loginwith, loginwithfileentry, 
					  				   loginwithtooltip, SQLQueries.getCurrentDate(), shortener);
			
			if(_log.isDebugEnabled())
				_log.debug(new StringBuilder("New itersocialconfig:\n").append(sql));
			
			PortalLocalServiceUtil.executeUpdateComittedQuery(sql);
			new SocialStatisticMgr(tasks).updateSocialStatisticsTask(itersocialconfigid, true);
		}
		else
		{
			Document previousConfig = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SOCIAL_CONFIG_FORSTATISTIC, itersocialconfigid));
			ErrorRaiser.throwIfNull(previousConfig);
			String previousCollectstats = XMLHelper.getTextValueOf(previousConfig, "/rs/row/@collectstats");
			ErrorRaiser.throwIfNull(previousCollectstats);
			
			String sql = String.format(UPDATE_SOCIAL_CONFIG, itersocialid, itersocialscopeid, 
					  				   collectstats, publicKey, secretKey, 
					  				   groupid, loginwith, loginwithfileentry, 
					  				   loginwithtooltip, SQLQueries.getCurrentDate(),
					  				   applicationname, shortener, itersocialconfigid);
			
			if (_log.isDebugEnabled())
				_log.debug(new StringBuilder("Update itersocialconfig:\n").append(sql));
			
			PortalLocalServiceUtil.executeUpdateComittedQuery(sql);
			
			if (!Validator.equals(collectstats, previousCollectstats))
			{
				new SocialStatisticMgr(tasks).updateSocialStatisticsTask(itersocialconfigid, true);
			}
		}
		return getSocialConfigById(itersocialconfigid);
	}
	
	public void initSocialStatisticsTasks() throws ServiceError
	{
		ErrorRaiser.throwIfFalse(Heartbeat.canLaunchProcesses(), IterErrorKeys.XYZ_ITR_E_SOCIAL_ISNOT_MASTER_SERVER_ZYX);
		new SocialStatisticMgr(tasks).start();
	}
	
	public void updateSocialStatisticsTask(String idConfig, String previousServerAffinity) throws ServiceError, SecurityException, NoSuchMethodException, IOException, SQLException, JSONException, SystemException
	{
		ErrorRaiser.throwIfFalse(Heartbeat.canLaunchProcesses(), IterErrorKeys.XYZ_ITR_E_SOCIAL_ISNOT_MASTER_SERVER_ZYX);
		new SocialStatisticMgr(tasks).updateSocialStatisticsTask(idConfig, false);
	}
	
	public void stopSocialStatisticsTasks() throws ServiceError
	{
		ErrorRaiser.throwIfFalse(Heartbeat.canLaunchProcesses(), IterErrorKeys.XYZ_ITR_E_SOCIAL_ISNOT_MASTER_SERVER_ZYX);
		new SocialStatisticMgr(tasks).stopSocialStatisticsTasks();
	}
	
	public void stopSocialStatisticsTask(String idConfig, String serverAffinity) throws JSONException, ClientProtocolException, SystemException, IOException, ServiceError
	{
		ErrorRaiser.throwIfFalse(Heartbeat.canLaunchProcesses(), IterErrorKeys.XYZ_ITR_E_SOCIAL_ISNOT_MASTER_SERVER_ZYX);
		new SocialStatisticMgr(tasks).stopSocialStatisticsTask(idConfig);
	}
	
	public Document exportData(String params) throws DocumentException, ServiceError, PortalException, SystemException, SecurityException, NoSuchMethodException, IOException
	{
		Element root = SAXReaderUtil.read(params).getRootElement();
		String groupName = XMLHelper.getStringValueOf(root, "@groupName");
		ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		long groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		// Se recupera el ID de la delegación del grupo para saber en qué carpeta están físicamente las imágenes
		long delegationId = GroupLocalServiceUtil.getGroup(groupId).getDelegationId();
		
		Document dom = SAXReaderUtil.createDocument();
		dom.addElement("social");
		
		Document domSocialStatistics = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SOCIAL_CONFIG_BY_GROUP, groupId));
		
		for(Node n : domSocialStatistics.getRootElement().content())
		{
			if ( GetterUtil.getBoolean(XMLHelper.getTextValueOf(n, "@loginwith")))
			{
				String imageUUID = XMLHelper.getTextValueOf(n, "@loginwithfileentry");
				
				if (Validator.isNotNull(imageUUID))
				{
					DLFileEntry dlfileEntry = DLFileEntryLocalServiceUtil.getDLFileEntryByUuidAndGroupId(imageUUID, groupId);
					InputStream is = DLFileEntryLocalServiceUtil.getFileAsStream(delegationId, dlfileEntry.getUserId(), dlfileEntry.getGroupId(), dlfileEntry.getFolderId(), dlfileEntry.getName());

					String loginImageContent = Base64.encodeBase64String(IOUtils.toByteArray(is));
					is.close();
				
					Element image = ((Element) n).addElement("loginImageContent");
					image.setText(loginImageContent);
					image.addAttribute("imageName", dlfileEntry.getTitle());
					image.addAttribute("imageSize", String.valueOf(dlfileEntry.getSize()));
				}
			}
		}
		domSocialStatistics.getRootElement().addAttribute("table", "itersocialconfig");
		dom.getRootElement().add(domSocialStatistics.getRootElement());

		Document domSocialConfigField = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_SOCIALCONFIGFIELD_BY_GROUP, groupId) );
		domSocialConfigField.getRootElement().addAttribute("table", "itersocialconfigfield");
		dom.getRootElement().add(domSocialConfigField.getRootElement());
		
		return dom;
	}
	
	public void importData(String data) throws DocumentException, IOException, SQLException, PortalException, SystemException, ServiceError, SecurityException, NoSuchMethodException
	{
		Document dom = SAXReaderUtil.read(data);
		Element root = dom.getRootElement();
		
		String groupName = XMLHelper.getStringValueOf(root, "@groupName");
        ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
        long groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
		boolean updtIfExist = GetterUtil.getBoolean(XMLHelper.getStringValueOf(root, "@updtIfExist", 	"true"));
		
		// ITERSOCIALCONFIG
		List<Node> itersocialconfig = root.selectNodes("rs[@table='itersocialconfig']/row");
		if (Validator.isNotNull(itersocialconfig) && itersocialconfig.size() > 0)
		{
			Document socialDom= PortalLocalServiceUtil.executeQueryAsDom("SELECT itersocialid, socialname FROM itersocial");
			Document scopeDom = PortalLocalServiceUtil.executeQueryAsDom("SELECT itersocialscopeid, scopename, itersocialid FROM IterSocialScope");
			Document existDom = PortalLocalServiceUtil.executeQueryAsDom( String.format("SELECT itersocialconfigid, itersocialid FROM itersocialconfig WHERE groupid = %d", groupId) );
			for (int i = 0; i < itersocialconfig.size(); i++)
			{
				Element socialConfig = (Element)itersocialconfig.get(i);
				String socialName 	 = socialConfig.attributeValue("socialname");
				String scopeName 	 = socialConfig.attributeValue("scopename");
				ErrorRaiser.throwIfFalse(Validator.isNotNull(socialName) && Validator.isNotNull(scopeName), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

				String iterSocialId  = XMLHelper.getStringValueOf(socialDom, String.format("/rs/row[@socialname = '%s']/@itersocialid", socialName));
				ErrorRaiser.throwIfNull(iterSocialId, IterErrorKeys.XYZ_ITR_E_SOCIAL_NOT_FOUND_ZYX, socialName);
				
				Node oldConfig = existDom.selectSingleNode(String.format("/rs/row[@itersocialid='%s']", iterSocialId));
				boolean exist  = (oldConfig != null);
				
				ErrorRaiser.throwIfFalse( updtIfExist || !exist, IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, 
	 					String.format("%s(%s)", IterAdmin.IA_CLASS_SOCIAL_CONFIG, iterSocialId));
				
				String publicKey 		= GetterUtil.getString2( StringUtil.apostrophe(socialConfig.attributeValue("publicKey")), "NULL" );
				String secretKey 		= GetterUtil.getString2( StringUtil.apostrophe(socialConfig.attributeValue("secretKey")), "NULL" );
				String applicationname 	= GetterUtil.getString2( StringUtil.apostrophe(socialConfig.attributeValue("applicationname")), "NULL" ); 
				String loginwith 		= socialConfig.attributeValue("loginwith");
				
				String loginwithfileentry= null;
				String loginImageContent = XMLHelper.getTextValueOf(socialConfig, "loginImageContent");
				if (Validator.isNotNull(loginImageContent))
				{
					String fileName = XMLHelper.getTextValueOf(socialConfig, "loginImageContent/@imageName");
					long imgSize 	= XMLHelper.getLongValueOf(socialConfig, "loginImageContent/@imageSize");
					InputStream is  = new ByteArrayInputStream(Base64.decodeBase64(loginImageContent));
					setLoginFileEntry(groupId, fileName, imgSize, is, socialName, XMLHelper.getTextValueOf(oldConfig, "@itersocialconfigid"));
					is.close();
					
					long folderId = getFolderId(groupId, GroupMgr.getDefaultUserId(), socialName);
					
					List<DLFileEntry>imgList = DLFileEntryLocalServiceUtil.getFileEntries(groupId, folderId);
					if (imgList != null && imgList.size() > 0)
						loginwithfileentry = imgList.get(0).getUuid();
				}
				
				loginwithfileentry 		= GetterUtil.getString2( StringUtil.apostrophe(loginwithfileentry), "NULL" ); 
				String loginwithtooltip = GetterUtil.getString2( StringUtil.apostrophe(socialConfig.attributeValue("loginwithtooltip")), "NULL" );
				
				String shortener = socialConfig.attributeValue("shortener");
				shortener = Validator.isNull(shortener) ? StringPool.NULL : StringUtil.apostrophe(shortener); 
				
				String collectstats 	= socialConfig.attributeValue("collectstats");
				String xpath			= String.format("/rs/row[@scopename='%s' and @itersocialid='%s']/@itersocialscopeid", scopeName, iterSocialId);
				String iterSocialScopeId= XMLHelper.getTextValueOf(scopeDom, xpath);
				ErrorRaiser.throwIfNull(iterSocialScopeId, IterErrorKeys.XYZ_ITR_E_SOCIALSCOPE_NOT_FOUND_ZYX, xpath);
				
				String sql = (!exist) ? String.format(INSERT_SOCIAL_CONFIG, 
											SQLQueries.getUUID(), iterSocialId, iterSocialScopeId, collectstats, publicKey, 
											secretKey, applicationname, groupId, loginwith, loginwithfileentry, loginwithtooltip, 
											SQLQueries.getCurrentDate(), shortener) :
												
										String.format(UPDATE_SOCIAL_CONFIG, 
											iterSocialId, iterSocialScopeId, collectstats, publicKey, secretKey, groupId, 
											loginwith, loginwithfileentry, loginwithtooltip, SQLQueries.getCurrentDate(),
											applicationname, shortener, ((Element)oldConfig).attributeValue("itersocialconfigid"));
				
				PortalLocalServiceUtil.executeUpdateQuery(sql);
			}
		}
		
		// ITERSOCIALCONFIGFIELD
		List<Node> itersocialconfigfield = root.selectNodes("rs[@table='itersocialconfigfield']/row");
		if (Validator.isNotNull(itersocialconfigfield) && itersocialconfigfield.size() > 0)
		{
			Document existDom 	= PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_SOCIALCONFIGS_BY_GROUP, groupId) );
			Document profileDom = PortalLocalServiceUtil.executeQueryAsDom( "SELECT profilefieldid, fieldname FROM UserProfile" );

			for (int i = 0; i < itersocialconfigfield.size(); i++)
			{
				Element socialConfigField = (Element)itersocialconfigfield.get(i);
				String socialName 		= socialConfigField.attributeValue("socialname");
				String scopeName 		= socialConfigField.attributeValue("scopename");
				String fieldName 		= socialConfigField.attributeValue("fieldname");
				String profileFieldName = socialConfigField.attributeValue("profileFieldName");
				ErrorRaiser.throwIfFalse(Validator.isNotNull(socialName) && Validator.isNotNull(scopeName) &&
										 Validator.isNotNull(fieldName)  && Validator.isNotNull(profileFieldName), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
				
				Node socialConfig = existDom.selectSingleNode( String.format("rs/row[@socialname='%s' and @scopename='%s' and @fieldname='%s']", socialName, scopeName, fieldName) );
				ErrorRaiser.throwIfNull(socialConfig, IterErrorKeys.XYZ_ITR_E_SOCIAL_CONFIG_NOT_FOUND_ZYX, String.format("%s:%s:%s", socialName, scopeName, fieldName));
				Element socialConfigElem = (Element)socialConfig;
				
				String iterSocialConfigFieldId = socialConfigElem.attributeValue("itersocialconfigfieldid");
				ErrorRaiser.throwIfFalse( updtIfExist || Validator.isNull(iterSocialConfigFieldId), IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, String.format("%s(%s)", IterAdmin.IA_CLASS_SOCIAL_CONFIG, iterSocialConfigFieldId));
				
				String iterSocialConfigId = socialConfigElem.attributeValue("itersocialconfigid");
				String iterSocialFieldId  = socialConfigElem.attributeValue("itersocialfieldid");
				String profileFieldId 	  = XMLHelper.getTextValueOf(profileDom, String.format("/rs/row[@fieldname='%1$s']/@profilefieldid", profileFieldName));
				ErrorRaiser.throwIfNull(profileFieldId, IterErrorKeys.XYZ_ITR_E_USERPROFILE_NOT_FOUND_ZYX);
				
				if (Validator.isNull(iterSocialConfigFieldId))
				{
					iterSocialConfigFieldId = SQLQueries.getUUID();
					PortalLocalServiceUtil.executeUpdateQuery(String.format(INSERT_PROFILE_SOCIAL_FIELD, iterSocialConfigFieldId, iterSocialConfigId, iterSocialFieldId, profileFieldId, SQLQueries.getCurrentDate()));
				}
				else
				{
					PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_PROFILE_SOCIAL_FIELD, iterSocialConfigId, iterSocialFieldId, profileFieldId, SQLQueries.getCurrentDate(), iterSocialConfigFieldId));
				}
			}
		}
	}
}
