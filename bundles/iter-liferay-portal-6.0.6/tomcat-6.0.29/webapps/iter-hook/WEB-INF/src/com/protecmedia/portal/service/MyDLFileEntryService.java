package com.protecmedia.portal.service;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.documentlibrary.NoSuchFileEntryException;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFileEntryService;
import com.liferay.portlet.documentlibrary.service.DLFileEntryServiceWrapper;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;

public class MyDLFileEntryService extends DLFileEntryServiceWrapper {
	
	private static Log _log = LogFactoryUtil.getLog(MyDLFileEntryService.class);	
	
	
	/* (non-Java-doc)
	 * @see com.liferay.portlet.documentlibrary.service.DLFileEntryServiceWrapper#DLFileEntryServiceWrapper(DLFileEntryService dlFileEntryService)
	 */
	public MyDLFileEntryService(DLFileEntryService dlFileEntryService) {
		super(dlFileEntryService);
	}
	
	@Override
	public DLFileEntry addFileEntry(
			long groupId, long folderId, String name, String title, 
			String description, String changeLog, String extraSettings, 
			byte[] bytes, ServiceContext serviceContext) throws PortalException, SystemException{
		try{
			return super.addFileEntry(groupId, folderId, name, title, description, changeLog, extraSettings, bytes, serviceContext);
		}
		catch(Exception ex){
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_ADD_FILEENTRY_ZYX, ex));
		}
	}
	
	@Override
	public DLFileEntry updateFileEntry(
			long groupId, long folderId, String name, String sourceFileName,
			String title, String description, String changeLog,
			boolean majorVersion, String extraSettings, byte[] bytes,
			ServiceContext serviceContext)
		throws PortalException, SystemException {
		
		/*
		DLFileEntryPermission.check(
			getPermissionChecker(), groupId, folderId, name, ActionKeys.UPDATE);*/

		boolean hasLock = false;
		
		try{
			hasLock = hasFileEntryLock(groupId, folderId, name);

			if (!hasLock) {
	
				// Lock
				lockFileEntry(groupId, folderId, name);
			}
			
			return super.updateFileEntry(groupId, folderId, name, sourceFileName, title, description, changeLog, 
															majorVersion, extraSettings, bytes, serviceContext);
		}
		catch(Exception ex){
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_UPDATE_FILEENTRY_ZYX, ex));
		}
		finally {
			if (!hasLock) {
				// Unlock
				unlockFileEntry(groupId, folderId, name);
			}
		}

	}
	
	@Override
	public void deleteFileEntry(long groupId, long folderId, String name) throws PortalException, SystemException
	{
		try 
		{
			super.deleteFileEntry(groupId, folderId, name);
		} 
		catch (NoSuchFileEntryException feException)
		{
			// Es muy probable que MLN intente realizar borrados de DLFileEntries que ya han sido eliminados. 
			// Para no generar trazas excesivas se deja constacia de una forma sencilla.
			_log.error(feException.toString());
		}
		catch (Exception ex) 
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_DELETE_FILEENTRY_ZYX, ex));
		} 
	}
	
	@Override
	public void deleteFileEntryByTitle(long groupId, long folderId, String titleWithExtension) throws PortalException, SystemException
	{
		try 
		{
			super.deleteFileEntryByTitle(groupId, folderId, titleWithExtension);
		} 
		catch (NoSuchFileEntryException feException)
		{
			// Es muy probable que MLN intente realizar borrados de DLFileEntries que ya han sido eliminados. 
			// Para no generar trazas excesivas se deja constacia de una forma sencilla.
			_log.error(feException.toString());
		}
		catch (Exception ex) 
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_DELETE_FILEENTRY_BY_TITLE_ZYX, ex));
		} 
	}
	
	@Override
	public DLFileEntry getFileEntryByUuidAndGroupId(String uuid, long groupId)
		throws PortalException, SystemException {

		try{
			DLFileEntry fileEntry = DLFileEntryLocalServiceUtil.getFileEntryByUuidAndGroupId(
				uuid, groupId);
			return fileEntry;
		} 
		catch(Exception ex){
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_GET_FILEENTRY_BY_UUID_GROUPID_ZYX, ex));
		}
	}

}