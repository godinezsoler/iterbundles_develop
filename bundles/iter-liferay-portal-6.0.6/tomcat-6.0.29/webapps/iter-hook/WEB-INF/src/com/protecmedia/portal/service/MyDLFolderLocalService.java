package com.protecmedia.portal.service;

import java.util.Date;
import java.util.List;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.model.DLFolderConstants;
import com.liferay.portlet.documentlibrary.service.DLFolderLocalService;
import com.liferay.portlet.documentlibrary.service.DLFolderLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFolderLocalServiceWrapper;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.documentlibrary.DLFileEntryXmlIO;
import com.protecmedia.iter.xmlio.service.item.documentlibrary.DLFolderXmlIO;

public class MyDLFolderLocalService extends DLFolderLocalServiceWrapper {
	
	private static Log _log = LogFactoryUtil.getLog(MyDLFolderLocalService.class);
	private static ItemXmlIO itemXmlIO = new DLFolderXmlIO();
	
	public MyDLFolderLocalService(DLFolderLocalService dlFolderLocalService) {
		super(dlFolderLocalService);		
	}
	
	/*
	 * Add Functions 
	 * --------------
	 * 
	 * Live table:
	 *  GlobalId -> fileEntryId
	 *  LocalId -> fileEntryId
	 */	
	@Override
	public DLFolder addFolder( long userId, long groupId, long parentFolderId, String name,
			String description, ServiceContext serviceContext) throws PortalException, SystemException {

		DLFolder folder = null;
		
		try{
			folder = super.addFolder(userId, groupId, parentFolderId, 
					name, description, serviceContext);

			itemXmlIO.createLiveEntry(folder);
			
		}catch(Exception e){
			_log.error("add Folder", e);
		}
		return folder;
	}
	
	/*
	 * Delete functions
	 */	
	@Override
	public void deleteFolder(DLFolder folder)
	throws PortalException, SystemException {
		
		try{
			
			super.deleteFolder(folder);	
			
			itemXmlIO.deleteLiveEntry(folder);
		
		}catch(Exception e){
			_log.error("delete Folder", e);
		}
	}
	
	@Override
	public void deleteFolder(long folderId)
	throws PortalException, SystemException {
		
		try{
			DLFolder folder = DLFolderLocalServiceUtil.getDLFolder(folderId);
			
			deleteSubfolders(folder);	
			
			super.deleteFolder(folderId);			
			
		}catch(Exception e){
			_log.error("delete Folder", e);
		}
	}
	

	@Override
	public void deleteFolders(long groupId)
	throws PortalException, SystemException {

		try{
			List<DLFolder> folders = DLFolderLocalServiceUtil.getFolders(groupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID);

			for (DLFolder folder : folders) {

				itemXmlIO.deleteLiveEntry(folder);
			
			}
			
			super.deleteFolders(groupId);
			
		}catch(Exception e){
			_log.error("delete Folders", e);
		}
		
	}
	
	/**
	 * 
	 * @param folder
	 */
	private void deleteSubfolders(DLFolder folder){
			
		try{
			
			List<DLFolder> folders = DLFolderLocalServiceUtil.getFolders(folder.getGroupId(), folder.getFolderId());

			for (DLFolder curFolder : folders) {
				deleteSubfolders(curFolder);
			}
			
			itemXmlIO.deleteLiveEntry(folder);
					
		}catch(Exception e){
			_log.error("delete subfolders", e);
		}
	}
	
	/*
	 * Update functions
	 */	
	
	@Override
	public DLFolder updateFolder(long folderId, long parentFolderId, String name,
			String description, ServiceContext serviceContext) throws PortalException, SystemException {
		
		DLFolder folder=null;
		
		try{
			folder = super.updateFolder(folderId, parentFolderId, name, description, 
					serviceContext);
	
			itemXmlIO.createLiveEntry(folder);
	
		}catch(Exception e){
			_log.error("update Folder", e);
		}
		return folder;
	}
}