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

import com.liferay.portal.NoSuchLayoutException;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.comments.CommentsConfigUtil;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.documentlibrary.NoSuchFolderException;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.model.DLFolderConstants;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFolderLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.persistence.DLFileEntryUtil;
import com.protecmedia.iter.base.service.base.CommentsConfigLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.xmlio.service.util.CDATAUtil;

/**
 * The implementation of the comments config local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.CommentsConfigLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.CommentsConfigLocalServiceUtil} to access the comments config local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.CommentsConfigLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.CommentsConfigLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class})
public class CommentsConfigLocalServiceImpl	extends CommentsConfigLocalServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(CommentsConfigLocalServiceImpl.class);
	
	
	private static final String GET_DISQUS_CONFIG_BY_GROUPID		= "SELECT disqusCfgUUID FROM disqusconfig WHERE groupId=%s";
	private static final String GET_SMS_MILENIUM_BY_GROUPID			= "SELECT disqusScript FROM disqusconfig WHERE groupId=%s";
	private static final String GET_COMMENTS_HTML_BY_GROUPID		= "SELECT zerocommentshtml, onecommentshtml, ncommentshtml FROM disqusconfig WHERE groupId=%s";
	
	private static final String INSERT_DISQUS_CONFIG 				= "INSERT INTO disqusconfig(disqusCfgUUID, identifierType, shortName, secretKey, publicKey, " + 
																				   				"useSSO, loginWidth, loginHeight, loginDlFileEntryUUID, loginURL, " +
																				   				"logoutURL, listPopularInterval, groupId, useDisqusCfg, disqusScript, " + 
																				   				"zerocommentshtml, onecommentshtml, ncommentshtml) VALUES (" +
																				   				"'%s', '%s', '%s', '%s', '%s', %s, %s, %s, '%s', '%s', '%s', '%s', %s, %s, " + 
																				   				"'%s', '%s', '%s', '%s')";
	
	private static final String INSERT_DISQUS_CONFIG_USE_DISQUS		= "INSERT INTO disqusconfig(disqusCfgUUID, identifierType, shortName, secretKey, " + 
																								"publicKey, useSSO, groupId, useDisqusCfg, listPopularInterval, disqusScript, " + 
																								"zerocommentshtml, onecommentshtml, ncommentshtml) VALUES (" +
																								"'%s', '%s', '%s', '%s', '%s', %s, %s, %s, '%s', '%s', '%s', '%s', '%s')";
	
	private static final String UPDATE_DISQUS_CONFIG  				= "UPDATE disqusconfig SET identifierType='%s', shortName='%s', secretKey='%s', publicKey='%s', useSSO=%s, " + 
																				  			   "loginWidth=%s, loginHeight=%s, loginDlFileEntryUUID='%s', loginURL='%s', logoutURL='%s', " + 
																				  			   "listPopularInterval='%s', groupId=%s, useDisqusCfg=%s, disqusScript='%s', " + 
																				  			   "zerocommentshtml='%s', onecommentshtml='%s', ncommentshtml='%s' " +
																				  			   "WHERE disqusCfgUUID = '%s'";
	
	private static final String UPDATE_DISQUS_CONFIG_USE_DISQUS  	= "UPDATE disqusconfig SET useDisqusCfg=%s WHERE disqusCfgUUID = '%s'";

	private static final String UPDATE_DISQUS_CONFIG_USE_SSO  		= "UPDATE disqusconfig SET identifierType='%s', shortName='%s', secretKey='%s', publicKey='%s', useSSO=%s, " + 
																							   "listPopularInterval='%s', groupId=%s, useDisqusCfg=%s, disqusScript='%s', " + 
																				  			   "zerocommentshtml='%s', onecommentshtml='%s', ncommentshtml='%s' " +
																							   "WHERE disqusCfgUUID = '%s'";
	
	private static final String UPDT_DISQUS_LOGINDLFILEENTRY 		= "UPDATE disqusconfig SET loginDlFileEntryUUID='%s' WHERE disqusCfgUUID = '%s'"; 

	
	private static final String GET_DISQUS_CONFIG_LOGINWITH			= "SELECT loginwith FROM itersocialconfig INNER JOIN itersocial ON itersocialconfig.itersocialid = itersocial.itersocialid " +
																								"WHERE itersocial.socialname = 'disqus' AND groupid = '%s'";
	
	/**
	 * 
	 * @param request
	 * @param response
	 * @param is
	 * @param xmlInitParams
	 * @return El nuevo path de la imagen
	 * @throws Exception
	 */
	public String uploadLoginFileEntry(HttpServletRequest request, HttpServletResponse response, InputStream is, long groupId ) throws Exception
	{
		String result = "";
		
		@SuppressWarnings("unchecked")
		Iterator<FileItem> files = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request).iterator();
		while (files.hasNext())
		{
	    	FileItem currentFile = files.next();
	    	if (!currentFile.isFormField())
	    	{
	    		is = currentFile.getInputStream();
	    		result = setLoginFileEntry(groupId, currentFile.getName(), currentFile.getSize(), is) ;
	    		break;
	    	}
		}
		return result;
	}
	
	private String setLoginFileEntry(long groupId, String fileName, long imgSize, InputStream is) throws PortalException, SystemException, ServiceError, SecurityException, NoSuchMethodException, IOException, SQLException
	{
		ErrorRaiser.throwIfNull(fileName, 		IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse(imgSize > 0, 	IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		long userId 	= GroupMgr.getDefaultUserId();
		long folderId 	= getFolderId(groupId, userId);
		
		int indexPeriod = fileName.indexOf(StringPool.PERIOD);
		boolean hasExtension = indexPeriod > 0 && indexPeriod < fileName.length();
		ErrorRaiser.throwIfFalse(hasExtension, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			    		
		String imgExtension = fileName.substring(indexPeriod, fileName.length());
		
		String imgTitle	= SQLQueries.getUUID() + imgExtension;
		
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
		// Se pone por título el uuid de la imagen	
		dlfileEntry.setTitle( dlfileEntry.getUuid() + imgExtension );
		DLFileEntryUtil.update(dlfileEntry, false);

		String disqusCfgUUID = getDisqusIdByGroup(String.valueOf(groupId));
		
		// Si en BBDD hay una configuración de Disqus asociada al grupo se actualiza su loginDlFileEntryUUID
		if (disqusCfgUUID != null)
			PortalLocalServiceUtil.executeUpdateQuery( String.format(UPDT_DISQUS_LOGINDLFILEENTRY, dlfileEntry.getUuid(), disqusCfgUUID) );

		String result = new StringBuilder("/documents").append(StringPool.SLASH).append(groupId).append(StringPool.SLASH).append(folderId).append(StringPool.SLASH).append(dlfileEntry.getTitle()).toString(); 
	    if (!PropsValues.IS_PREVIEW_ENVIRONMENT)
	    	result = result.concat("?env=live");
		else
	    	result = result.concat("?env=preview");

		return result;
	}
	
	private String getDisqusIdByGroup(String groupId) throws SecurityException, NoSuchMethodException
	{
		Document domUUID = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_DISQUS_CONFIG_BY_GROUPID, groupId) );
		return XMLHelper.getTextValueOf(domUUID, "/rs/row/@disqusCfgUUID");
	}
	
	public String getDisqusScript(long groupId) throws Exception
	{
		Document disqusScript = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_SMS_MILENIUM_BY_GROUPID, groupId) );
		return XMLHelper.getTextValueOf(disqusScript, "/rs/row/@disqusScript");
	}
	
	//zerocommentshtml, onecommentshtml, ncommentshtml   
	public Document getDisqusCommentsHTML(long groupId) throws Exception
	{
		List<String> elementList = new ArrayList<String>();
		elementList.add("zerocommentshtml");
		elementList.add("onecommentshtml");
		elementList.add("ncommentshtml");
		
		String []elements = new String[elementList.size()];
		elementList.toArray(elements);
		
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_COMMENTS_HTML_BY_GROUPID, groupId), elements);
	}
	
	/**
	 * 
	 * @param groupId
	 * @return
	 * @throws Exception
	 */
	public String getConfig(long groupId) throws Exception
	{
		return CommentsConfigUtil.getConfig(groupId).asXML();
	}
	
	/**
	 * 
	 * @param xmlData
	 * @return
	 * @throws Exception
	 */
	public String setConfig( String xmlData ) throws Exception
	{
		//Extracción y validación de los datos del formulario
		Document doc = SAXReaderUtil.read(xmlData);
		
		String groupId = XMLHelper.getTextValueOf(doc, "/rs/row/@groupId");
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		setConfig(doc.selectSingleNode("/rs/row"), groupId);
		return xmlData;
	}
	
	private void setConfig(Node node, String groupId) throws Exception
	{		
		ErrorRaiser.throwIfNull(node);
		
		long userId = GroupMgr.getDefaultUserId();
		
		String disqusCfgUUID = getDisqusIdByGroup(groupId);
		
		boolean useDisqusCfg = GetterUtil.getBoolean(XMLHelper.getTextValueOf(node, "@useDisqusCfg"));
		if(useDisqusCfg)
		{
			String identifierType = XMLHelper.getTextValueOf(node, "@identifierType");
			ErrorRaiser.throwIfFalse(identifierType != null && (identifierType.equals(IterKeys.DISQUS_IDENTIFIER_TYPE_URL) || identifierType.equals(IterKeys.DISQUS_IDENTIFIER_TYPE_ID)), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			String shortName = XMLHelper.getTextValueOf(node, "@shortName");
			ErrorRaiser.throwIfNull(shortName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			String secretKey = "";
			String secretKey2 = XMLHelper.getTextValueOf(node, "@secretKey");
			if (secretKey2 != null)
				secretKey = secretKey2;
			
//			String secretKey = XMLHelper.getTextValueOf(doc, "/rs/row/@secretKey");
//			ErrorRaiser.throwIfNull(secretKey, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
//		
			String publicKey = "";
			String publicKey2 = XMLHelper.getTextValueOf(node, "@publicKey");
			if (publicKey2 != null)
				publicKey = publicKey2;

//			String publicKey = XMLHelper.getTextValueOf(doc, "/rs/row/@publicKey");
//			ErrorRaiser.throwIfNull(publicKey, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

			String listPopularInterval = XMLHelper.getTextValueOf(node, "@listPopularInterval");
			ErrorRaiser.throwIfNull(CommentsConfigUtil.isValidListPopularInterval(listPopularInterval), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			String disqusScript = "";
			String script = XMLHelper.getTextValueOf(node, "disqusScript");
			if (script != null)
				disqusScript = StringEscapeUtils.escapeSql(CDATAUtil.strip(script));
			
			ErrorRaiser.throwIfNull(disqusScript, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			String zerocommentshtml = "";
			String zerocommentsParam = XMLHelper.getTextValueOf(node, "zerocommentshtml");
			if (zerocommentsParam != null)
				zerocommentshtml = StringEscapeUtils.escapeSql(CDATAUtil.strip(zerocommentsParam));
			
			String onecommentshtml = "";
			String onecommentshtmlParam = XMLHelper.getTextValueOf(node, "onecommentshtml");
			if (onecommentshtmlParam != null)
				onecommentshtml = StringEscapeUtils.escapeSql(CDATAUtil.strip(onecommentshtmlParam));
			
			String ncommentshtml = "";
			String ncommentshtmlParam = XMLHelper.getTextValueOf(node, "ncommentshtml");
			if (ncommentshtmlParam != null)
				ncommentshtml = StringEscapeUtils.escapeSql(CDATAUtil.strip(ncommentshtmlParam));
			
			boolean useSSO = GetterUtil.getBoolean(XMLHelper.getTextValueOf(node, "@useSSO"));
			if(useSSO)
			{
				String loginImagePath = XMLHelper.getTextValueOf(node, "loginImagePath");
				ErrorRaiser.throwIfNull(loginImagePath, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

				String loginURL 	= null;
				String friendlyURL 	= XMLHelper.getTextValueOf(node, "friendlyURL");
				if (Validator.isNull(friendlyURL))
				{
					loginURL = XMLHelper.getTextValueOf(node, "loginURL");
					ErrorRaiser.throwIfNull(loginURL, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
				}
				else
				{
					try
					{	
						Layout layout = LayoutLocalServiceUtil.getFriendlyURLLayout(Long.parseLong(groupId), false, friendlyURL);
						loginURL = layout.getUuid();
					}
					catch (NoSuchLayoutException e)
					{
						ErrorRaiser.throwIfError(IterErrorKeys.XYZ_DISQUS_LOGIN_LAYOUT_NOT_FOUND_ZYX, e.toString(), e.getStackTrace());
					}
				}

				//Se actuliza la descripción de la imagen con el tooltip
				long folderId = getFolderId(Long.parseLong(groupId), userId);
				
				List<DLFileEntry>imgList = DLFileEntryLocalServiceUtil.getFileEntries(Long.parseLong(groupId), folderId);
				
				//Si está deshabilitado el uso de DISQUS no es obligatorio la imagen
				ErrorRaiser.throwIfFalse(imgList.size() > 0, IterErrorKeys.XYZ_DISQUS_LOGIN_DLFILENTRY_NOT_FOUND_ZYX);
				
				String loginDlFileEntryUUID = imgList.get(0).getUuid();
				
				String loginImageTooltip = XMLHelper.getTextValueOf(node, "loginImageTooltip/text()");
				imgList.get(0).setDescription(loginImageTooltip);
				DLFileEntryUtil.update(imgList.get(0), false);
				
				if(Validator.isNull(disqusCfgUUID))
				{
					//Insert 
					disqusCfgUUID = SQLQueries.getUUID();

					PortalLocalServiceUtil.executeUpdateQuery(String.format(INSERT_DISQUS_CONFIG, SQLQueries.getUUID(), identifierType, shortName, 
																								  secretKey, publicKey, useSSO, 0,0,//loginWidth, loginHeight, 
																								  loginDlFileEntryUUID, loginURL, "",//logoutURL, 
																								  listPopularInterval, groupId, useDisqusCfg, 
																								  disqusScript, zerocommentshtml, onecommentshtml, 
																								  ncommentshtml));
				}
				else
				{
					//Update
					Document domLoginWith = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_DISQUS_CONFIG_LOGINWITH, groupId));
					ErrorRaiser.throwIfNull(domLoginWith);
					boolean loginWith = GetterUtil.getBoolean(XMLHelper.getTextValueOf(node, "@useDisqusCfg"));
					ErrorRaiser.throwIfFalse(!(loginWith && (secretKey2 == null || publicKey2 == null)), IterErrorKeys.XYZ_DISQUS_LOGIN_PUBLICSECRETKEY_ISNULL_ZYX);
					
					PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_DISQUS_CONFIG, identifierType, shortName, secretKey, 
																								  publicKey, useSSO, 0,0,//loginWidth, loginHeight, 
																								  loginDlFileEntryUUID, loginURL, "",//logoutURL, 
																								  listPopularInterval, groupId, useDisqusCfg, 
																								  disqusScript, zerocommentshtml, onecommentshtml, 
																								  ncommentshtml, disqusCfgUUID));
				}
			}
			else if(Validator.isNotNull(disqusCfgUUID))
			{
				//Update SSO Config
				Document domLoginWith = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_DISQUS_CONFIG_LOGINWITH, groupId));
				ErrorRaiser.throwIfNull(domLoginWith);
				boolean loginWith = GetterUtil.getBoolean(XMLHelper.getTextValueOf(node, "@useDisqusCfg"));
				ErrorRaiser.throwIfFalse(!(loginWith && (secretKey2 == null || publicKey2 == null)), IterErrorKeys.XYZ_DISQUS_LOGIN_PUBLICSECRETKEY_ISNULL_ZYX);
				
				PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_DISQUS_CONFIG_USE_SSO, identifierType, shortName, secretKey, 
																							  		  publicKey, useSSO, listPopularInterval, 
																							  		  groupId, useDisqusCfg, disqusScript, 
																							  		  zerocommentshtml, onecommentshtml, 
																									  ncommentshtml, disqusCfgUUID));
			}
			else
			{
				//Insert useDisqus
				disqusCfgUUID = SQLQueries.getUUID();
				PortalLocalServiceUtil.executeUpdateQuery(String.format(INSERT_DISQUS_CONFIG_USE_DISQUS, disqusCfgUUID, identifierType,
																										 shortName, secretKey, publicKey, 
																										 useSSO, groupId, useDisqusCfg, 
																										 listPopularInterval, disqusScript, 
																										 zerocommentshtml, onecommentshtml, 
																										 ncommentshtml));
			}
		}
		else if(Validator.isNotNull(disqusCfgUUID))
		{
			//Update useDisqus
			PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_DISQUS_CONFIG_USE_DISQUS, useDisqusCfg, disqusCfgUUID));
		}
	}
	
	static private long getFolderId(long groupId, long userId) throws PortalException, SystemException, ServiceError
	{
	    DLFolder dlFolder = null;
	    
	    try
	    {
	    	dlFolder = DLFolderLocalServiceUtil.getFolder(groupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, IterKeys.DISQUS_LOGIN_DLFILENTRY_FOLDER);
	    }
	    catch (NoSuchFolderException nsfe)
	    {
	    	_log.debug("Creating " + IterKeys.DISQUS_LOGIN_DLFILENTRY_FOLDER + " folder...");
	    	dlFolder = DLFolderLocalServiceUtil.addFolder(userId, groupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, IterKeys.DISQUS_LOGIN_DLFILENTRY_FOLDER, StringPool.BLANK, new ServiceContext());
	    }
	    
	    ErrorRaiser.throwIfNull(dlFolder);
	    
	    return dlFolder.getFolderId();
	}
	
	public Document exportData(String params) throws Exception
	{
		String groupName = XMLHelper.getStringValueOf(SAXReaderUtil.read(params).getRootElement(), "@groupName");
		ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		long groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
		return exportData(groupId);
	}
	public Document exportData(Long groupId) throws Exception
	{
		Document cfgDom = CommentsConfigUtil.getConfig(groupId);
		Element cfgElem = (Element)cfgDom.selectSingleNode("/rs/row");
		
		// Hay que enviar la imagen 
		if ( GetterUtil.getBoolean(XMLHelper.getTextValueOf(cfgElem, "@useDisqusCfg")) && 
			 GetterUtil.getBoolean(XMLHelper.getTextValueOf(cfgElem, "@useSSO")) )
		{
			String disqusCfgUUID = XMLHelper.getTextValueOf(cfgElem, "@loginImageUUID");
			ErrorRaiser.throwIfNull(disqusCfgUUID);
			
			DLFileEntry dlfileEntry = DLFileEntryLocalServiceUtil.getDLFileEntryByUuidAndGroupId(disqusCfgUUID, groupId);
			long delegationId = GroupLocalServiceUtil.getGroup(groupId).getDelegationId();
			InputStream is = DLFileEntryLocalServiceUtil.getFileAsStream(delegationId, dlfileEntry.getUserId(), dlfileEntry.getGroupId(), dlfileEntry.getFolderId(), dlfileEntry.getName());

			String loginImageContent = Base64.encodeBase64String(IOUtils.toByteArray(is));
			is.close();
			
			cfgElem.addElement("loginImageContent").setText(loginImageContent);
			
			// Se añade la friendlyURL de la página de login, ya que en el sistema destino no existirá una sección on el mismo uuid
			String loginURL = XMLHelper.getTextValueOf(cfgElem, "loginURL");
			ErrorRaiser.throwIfNull(loginURL);
			
			try
			{
				Layout layout = LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(loginURL, groupId);
				cfgElem.addElement("friendlyURL").setText(layout.getFriendlyURL());
			}
			catch (NoSuchLayoutException e)
			{
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_DISQUS_LOGIN_LAYOUT_NOT_FOUND_ZYX, e.toString(), e.getStackTrace());
			}
		}

		return cfgDom;
	}
	
	public void importData(String data) throws Exception
	{
		Element root = SAXReaderUtil.read( data ).getRootElement();
		long groupId = XMLHelper.getLongValueOf(root, "@groupId");
		if (groupId <= 0)		
			groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), XMLHelper.getStringValueOf(root, "@groupName")).getGroupId();
		
		Node cfgNode = root.selectSingleNode("row");
		if (cfgNode != null)
		{
			Element cfgElem = (Element)cfgNode;
			
			// Primero se crea/importa la imagen
			if ( GetterUtil.getBoolean(XMLHelper.getTextValueOf(cfgElem, "@useDisqusCfg")) && 
				 GetterUtil.getBoolean(XMLHelper.getTextValueOf(cfgElem, "@useSSO")) )
			{
				String loginImageContent = XMLHelper.getTextValueOf(cfgElem, "loginImageContent");
				ErrorRaiser.throwIfNull(loginImageContent);
				
				InputStream is = new ByteArrayInputStream(Base64.decodeBase64(loginImageContent));
				
				setLoginFileEntry(groupId, XMLHelper.getTextValueOf(cfgElem, "@loginImageName"), XMLHelper.getLongValueOf(cfgElem, "@loginImageSize"), is) ;
			}
			
			setConfig(cfgElem, String.valueOf(groupId));
		}
	}
}	
