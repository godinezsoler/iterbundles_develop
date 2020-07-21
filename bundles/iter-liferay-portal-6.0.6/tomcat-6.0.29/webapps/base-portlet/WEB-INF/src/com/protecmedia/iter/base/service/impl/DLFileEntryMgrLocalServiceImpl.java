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

import com.liferay.portlet.documentlibrary.model.DLFolderMgr;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

import com.liferay.cluster.ClusterMgr;
import com.liferay.cluster.IClusterMgr.ClusterMgrOperation;
import com.liferay.cluster.IClusterMgr.ClusterMgrType;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.documentlibrary.NoSuchFolderException;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.model.DLFolderConstants;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFolderLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.persistence.DLFileEntryUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.base.DLFileEntryMgrLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.base.service.util.TeaserMgr;

/**
 * The implementation of the d l file entry mgr local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.DLFileEntryMgrLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.DLFileEntryMgrLocalServiceUtil} to access the d l file entry mgr local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.DLFileEntryMgrLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.DLFileEntryMgrLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class DLFileEntryMgrLocalServiceImpl extends DLFileEntryMgrLocalServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(DLFileEntryMgrLocalServiceImpl.class);
	
	private static final String DOCUMENTS_PATH = "/documents";
	
	private static final String GET_SKIN_IMAGES = "SELECT a.fileentryuuid, d.uuid_ dlfileentryuuid, d.description title, d.size_ size, CAST(CONCAT('/documents/', d.groupId, '/', d.folderId, '/', d.title%s) AS CHAR CHARACTER SET utf8) path\n" +
												  "FROM DLFileEntry d\n" + 
												  "INNER JOIN DLFolder f ON f.folderId=d.folderId\n" + 
												  "INNER JOIN adfileentry a ON a.dlfileentryuuid=d.uuid_\n" + 
												  "WHERE f.name='" + IterKeys.ADVERTISEMENT_SKINS_FOLDER + "' AND f.groupId=%s ORDER BY d.description ASC, d.uuid_ ASC";
	
	private static final String ADD_TO_DELETE = "INSERT INTO addeleted(adid, adtable, groupid)\n" +
												"SELECT a.fileentryuuid, 'adfileentry', f.groupId\n" +
													"FROM adfileentry a INNER JOIN DLFileEntry f ON f.uuid_=a.dlfileentryuuid\n" + 
													"WHERE a.fileentryuuid IN %s AND a.publicationdate IS NOT NULL";
	
	private static final String GET_NAME_BY_UUID = "SELECT description FROM DLFileEntry WHERE uuid_='%s'";
	
	private static final String GET_SKINS_REFERENCES = "SELECT k.imagename FROM adskin k INNER JOIN adfileentry f ON f.fileentryuuid=k.fileentryuuid WHERE f.dlfileentryuuid='%s'";
	private static final String GET_SLOT_SKINS_REFERENCES =new StringBuilder(	
									"SELECT l.friendlyurl AS assignname, s.slotname, k.imagename, 'section' AS 'assigntype' FROM adslotadtags st\n"		).append(
									"LEFT JOIN Layout l ON l.plid=st.plid\n"								).append(
									"INNER JOIN adslot s ON s.slotid=st.slotid\n"							).append(
									"INNER JOIN adskin k ON k.skinid=st.skinid\n"							).append(
									"INNER JOIN adfileentry f ON f.fileentryuuid=k.fileentryuuid\n"			).append(
									"WHERE f.dlfileentryuuid='%1$s'\n"										).append(
									"UNION ALL\n"															).append(
									"SELECT c.name AS assignname, s.slotname, k.imagename, 'category' AS 'assigntype' FROM adslottagcategory stc\n"		).append(
									"LEFT JOIN assetcategory c ON c.categoryId=stc.categoryid\n"			).append(
									"INNER JOIN adslot s ON s.slotid=stc.slotid\n"							).append(
									"INNER JOIN adskin k ON k.skinid=stc.skinid\n"							).append(
									"INNER JOIN adfileentry f ON f.fileentryuuid=k.fileentryuuid\n"			).append(
									"WHERE f.dlfileentryuuid='%1$s'"										).toString();
											
	
	@SuppressWarnings("unchecked")
	public String uploadFileEntry(HttpServletRequest request, HttpServletResponse response, InputStream is, String xmlInitParams) throws Exception
	{
		String result = "<rs/>";
		
		try
		{
			ErrorRaiser.throwIfNull(xmlInitParams, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			Document initDoc = SAXReaderUtil.read(xmlInitParams);
			
			Iterator<FileItem> files = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request).iterator();
		    while (files.hasNext())
		    {
		    	FileItem currentFile = files.next();
		    	if (!currentFile.isFormField())
		    	{
		    		is = currentFile.getInputStream();
		    		result = uploadFileEntry(initDoc, currentFile).asXML();
		    		break;
		    	}
		    }
		}
		catch(ORMException orme)
		{
			throw orme;
		}
		catch(ServiceError se)
		{
			throw se;
		}
		catch(Throwable t)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(t.getMessage(), t), t);
		}
		finally
		{
			try
			{
				is.close();
			}
			catch(Throwable tClose){}
		}
	    
	    return result;
	}
	
	public Document uploadFileEntry(Document initDoc, FileItem currentFile) throws Exception
	{
		long scopeGroupId = XMLHelper.getLongValueOf(initDoc, "rs/scopeGroupId", 0);
		ErrorRaiser.throwIfFalse(scopeGroupId != 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		ClusterMgr.processConfig(ClusterMgrType.AD_IMAGE, 
				(XMLHelper.getLongValueOf(initDoc, "count(//dlfileentryuuid)") == 0) ? ClusterMgrOperation.ADD : ClusterMgrOperation.UPDATE, 
				initDoc, currentFile);
		
		long userId = GroupMgr.getDefaultUserId();
		
	    //Getting skins folder, if don't, create it
	    DLFolder dlFolder = DLFolderMgr.getFolder(scopeGroupId, IterKeys.ADVERTISEMENT_SKINS_FOLDER);
	    
		String fileName = currentFile.getName();
		ErrorRaiser.throwIfFalse(Validator.isNotNull(fileName), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		int indexPreriod = fileName.indexOf(StringPool.PERIOD);
		boolean hasExtension = indexPreriod > 0 && indexPreriod < fileName.length();
		ErrorRaiser.throwIfFalse(hasExtension, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		InputStream is = currentFile.getInputStream();

		String fileentryuuid = SQLQueries.getUUID();
		String title = SQLQueries.getUUID() + fileName.substring(indexPreriod, fileName.length());
		long size = currentFile.getSize();
		
		//Recupero dlfileentryuuid
		String dlfileentryuuid = XMLHelper.getTextValueOf(initDoc, "rs/dlfileentryuuid");

		DLFileEntry dlfileEntry = null;
		
		//Update: Se añade un nuevo DLFileEntry, se actualiza su entrada en adfileentry y se borra el antiguo DLFileEntry
		if(Validator.isNotNull(dlfileentryuuid))
		{
			DLFileEntry oldDlfileEntry = DLFileEntryLocalServiceUtil.getDLFileEntryByUuidAndGroupId(dlfileentryuuid, scopeGroupId);
			ErrorRaiser.throwIfNull(oldDlfileEntry, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			dlfileEntry = DLFileEntryLocalServiceUtil.addFileEntry(userId, scopeGroupId, dlFolder.getFolderId(), title, oldDlfileEntry.getDescription(), StringPool.BLANK, StringPool.BLANK, is, size, new ServiceContext());
			
			String query = String.format(SQLQueries.GET_ADFILEENTRY_FILEENTRYUUIDS_BY_DLFILEENTRYUUID, dlfileentryuuid);
			XPath xpath = SAXReaderUtil.createXPath("//row/@fileentryuuid");
			Node fileEntryNode = xpath.selectSingleNode(PortalLocalServiceUtil.executeQueryAsDom(query).getRootElement());
			ErrorRaiser.throwIfNull(fileEntryNode, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			fileentryuuid = fileEntryNode.getStringValue();
			
			PortalLocalServiceUtil.executeUpdateQuery(String.format(SQLQueries.UPDATE_ADFILEENTRY, dlfileEntry.getUuid(), SQLQueries.getCurrentDate(), dlfileentryuuid));
			DLFileEntryLocalServiceUtil.deleteDLFileEntry(oldDlfileEntry);
		}
		//Insert: Se añade un nuevo DLFileEntry y se crea su entrada en adfileentry
		else
		{
			SQLQueries.checkDuplicateNameFileEntry(scopeGroupId, dlFolder.getFolderId(), fileName, dlfileentryuuid);
			
			dlfileEntry = DLFileEntryLocalServiceUtil.addFileEntry(userId, scopeGroupId, dlFolder.getFolderId(), title, fileName, StringPool.BLANK, StringPool.BLANK, is, size, new ServiceContext());
			
			PortalLocalServiceUtil.executeUpdateQuery(String.format(SQLQueries.ADD_ADFILEENTRY, fileentryuuid, dlfileEntry.getUuid(), SQLQueries.getCurrentDate()));
		}

		ErrorRaiser.throwIfNull(dlfileEntry, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document dom = SAXReaderUtil.createDocument();
		Element rs =  dom.addElement("rs");
		Element row = rs.addElement("row");
		row.addAttribute("fileentryuuid", fileentryuuid);
		row.addAttribute("dlfileentryuuid", dlfileEntry.getUuid());
		row.addAttribute("title", dlfileEntry.getDescription());
		row.addAttribute("size", String.valueOf(size));
		String path = DOCUMENTS_PATH + StringPool.SLASH + scopeGroupId + StringPool.SLASH + dlFolder.getFolderId() + StringPool.SLASH + title;
		if (!PropsValues.IS_PREVIEW_ENVIRONMENT)
			path = path.concat("?env=live");
		else
			path = path.concat("?env=preview");
		row.addAttribute("path", path);
		
		return dom;
	}

	public String getFileEntries(String groupid) throws Exception
	{
		//scopeGroupId
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SKIN_IMAGES, (PropsValues.IS_PREVIEW_ENVIRONMENT ? ", '?env=preview'" : ", '?env=live'"), groupid)).asXML();
	}
	
	private void deleteFileEntry(String dlfileentryuuid, long scopeGroupId) throws PortalException, SystemException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		Element dataRoot = PortalLocalServiceUtil.executeQueryAsDom(String.format(SQLQueries.GET_ADFILEENTRY_FILEENTRYUUIDS_BY_DLFILEENTRYUUID, dlfileentryuuid)).getRootElement();
		
		XPath xpath = SAXReaderUtil.createXPath("//row/@fileentryuuid");
		
		List<Node> nodes = xpath.selectNodes(dataRoot);
		ErrorRaiser.throwIfFalse(nodes != null && nodes.size() > 0);
		
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
			PortalLocalServiceUtil.executeUpdateQuery(String.format(ADD_TO_DELETE, TeaserMgr.getInClauseSQL(nodes)));
		
		DLFileEntry dlfileEntry = DLFileEntryLocalServiceUtil.getDLFileEntryByUuidAndGroupId(dlfileentryuuid, scopeGroupId);
		ErrorRaiser.throwIfNull(dlfileEntry, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		DLFileEntryLocalServiceUtil.deleteFileEntryNoHook(dlfileEntry);
	}
	
	public String deleteFileEntries(String xmlData) throws Exception
	{
		Document dom = SAXReaderUtil.read(xmlData);
		
		ClusterMgr.processConfig(ClusterMgrType.AD_IMAGE, ClusterMgrOperation.DELETE, dom);
		
		Element dataRoot = dom.getRootElement();

		//scopeGroupId
		String scopeGroupId = XMLHelper.getTextValueOf(dataRoot, "@groupid");
		ErrorRaiser.throwIfNull(scopeGroupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		//UUIDs
		XPath xpath = SAXReaderUtil.createXPath("//row/@dlfileentryuuid");
		List<Node> nodes = xpath.selectNodes(dataRoot);
		ErrorRaiser.throwIfFalse(nodes != null && nodes.size() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		boolean checkreferences = GetterUtil.getBoolean(XMLHelper.getTextValueOf(dataRoot, "@checkreferences"), true);
		checkDependencies(dataRoot, checkreferences);
		
		for(Node id:nodes)
			deleteFileEntry(id.getStringValue(), Long.parseLong(scopeGroupId));
		
		return xmlData;
	}
	
	private void checkDependencies(Element rs, boolean checkReferences) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError
	{
		if(checkReferences)
		{
			boolean isError = false;
			StringBuffer errorMsg = new StringBuffer();
			
			XPath xpath = SAXReaderUtil.createXPath("//row");
			List<Node> nodes = xpath.selectNodes(rs);
			for(Node newRow:nodes)
			{
				StringBuffer currentErrorMsg = new StringBuffer();
				String dlfileentryuuid = XMLHelper.getTextValueOf(newRow, "@dlfileentryuuid");
				String query = String.format(GET_NAME_BY_UUID, dlfileentryuuid);
				String dlfileName = XMLHelper.getTextValueOf(PortalLocalServiceUtil.executeQueryAsDom(query).getRootElement(), "/rs/row/@description");
				
				//Imágen vinculada a skin/módulo asociado a página
				query = String.format(GET_SLOT_SKINS_REFERENCES, dlfileentryuuid);
				Document result = PortalLocalServiceUtil.executeQueryAsDom(query);
				currentErrorMsg.append(getErrorMsg(result, true));
				
				if(checkHasRow(result))
					isError = true;
				
				//Imágen vinculada a skin
				query = String.format(GET_SKINS_REFERENCES, dlfileentryuuid);
				result = PortalLocalServiceUtil.executeQueryAsDom(query);
				currentErrorMsg.append(getErrorMsg(result, false));
				
				if(currentErrorMsg.length() > 0)
					currentErrorMsg.insert(0, "\n" + dlfileName + ":\n\n");
				
				errorMsg.append(currentErrorMsg);
			}
			
			if(errorMsg.length() > 0)
			{
				if(isError)
					throw new ServiceError(IterErrorKeys.XYZ_E_INVALID_ADVERTISEMENT_OPERATION_ZYX, errorMsg.toString());
				else
					throw new ServiceError(IterErrorKeys.XYZ_E_IMAGE_HAS_REFERENCES_ZYX, errorMsg.toString());
			}
		}
	}
		
	private String getErrorMsg(Document rs, boolean slottag) throws SecurityException, NoSuchMethodException
	{
		StringBuffer error = new StringBuffer();
		
		XPath xpath = SAXReaderUtil.createXPath("//row");
		List<Node> nodes = xpath.selectNodes(rs);
		for(Node node:nodes)
		{
			if(slottag)
			{
				String assignmentTypeName = GetterUtil.getString(XMLHelper.getTextValueOf(node, "@assigntype")).equals("section")? IterKeys.TRAD_SECTIONNAME_TRAD: IterKeys.TRAD_CATEGORYNAME_TRAD;
				
				error.append(assignmentTypeName + ": " + GetterUtil.getString(XMLHelper.getTextValueOf(node, "@assignname"), IterKeys.TRAD_DEFAULT_VALUE_TRAD) + ", ");
				error.append(IterKeys.TRAD_SLOTNAME_TRAD + ": " + XMLHelper.getTextValueOf(node, "@slotname") + ", ");
			}

			error.append(IterKeys.TRAD_SKINNAME_TRAD + ": " + XMLHelper.getTextValueOf(node, "@imagename") + "\n");
		}
		
		return error.toString();
	}
	
	private boolean checkHasRow(Document rs) throws SecurityException, NoSuchMethodException
	{
		boolean hasRows = false;
		
		XPath xpath = SAXReaderUtil.createXPath("//row");
		List<Node> nodes = xpath.selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
			hasRows = true;
		
		return hasRows;
	}

	public DLFileEntry addDLFileEntry(long groupId, long defaultUserId, String folderName, String fileName, InputStream inputStr) throws ServiceError, IOException, PortalException, SystemException
	{
		/* Si esta función es llamada desde un hilo, GroupMgr.getDefaultUserId() devuelve un 0 debido a que tiene un contexto diferente.
		 * Por eso pasamos el id del usuario por defecto desde afuera. 
		 * long userId = GroupMgr.getDefaultUserId(); */
		
		int indexPreriod = fileName.indexOf(StringPool.PERIOD);
		String imgExtension = fileName.substring(indexPreriod, fileName.length());
		String imgTitle = SQLQueries.getUUID() + imgExtension;
		long imgSize = IOUtils.toByteArray(inputStr).length;
		inputStr.reset();
		long folderId = getFolderId(groupId, defaultUserId, folderName);

		DLFileEntry dlfileEntry = DLFileEntryLocalServiceUtil.addFileEntry(defaultUserId, groupId, folderId, imgTitle, fileName, StringPool.BLANK, StringPool.BLANK, inputStr, imgSize, new ServiceContext());
		
		//Se pone por título el uuid de la imagen     
		dlfileEntry.setTitle( dlfileEntry.getUuid() + imgExtension );
		DLFileEntryUtil.update(dlfileEntry, false);
		
		return dlfileEntry;

	}
	
	private long getFolderId(long groupId, long userId, String folderName) throws PortalException, SystemException, ServiceError
    {
        DLFolder dlFolder = null;
        
        try
        {
           dlFolder = DLFolderLocalServiceUtil.getFolder(groupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, folderName);
        }
        catch (NoSuchFolderException nsfe)
        {
           _log.debug("Creating " + folderName + " folder...");
           dlFolder = DLFolderLocalServiceUtil.addFolder(userId, groupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, folderName, StringPool.BLANK, new ServiceContext());
        }
        
        ErrorRaiser.throwIfNull(dlFolder);
        
        return dlFolder.getFolderId();
    }
	
	public String getDLFileEntryURL(long groupId, long folderId, String dlFileEntryTitle)
	{
		return new StringBuilder("/documents")
					.append(StringPool.SLASH)
					.append(groupId)
					.append(StringPool.SLASH)
					.append(folderId)
					.append(StringPool.SLASH)
					.append(dlFileEntryTitle).toString();
	}
	
}