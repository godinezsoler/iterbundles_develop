package com.protecmedia.portal.service;

import java.io.File;
import java.io.InputStream;

import com.liferay.documentlibrary.DuplicateFileException;
import com.liferay.documentlibrary.service.DLLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalService;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceWrapper;

import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.documentlibrary.DLFileEntryXmlIO;

public class MyDLFileEntryLocalService extends DLFileEntryLocalServiceWrapper 
{
	public static final String SEL_LIVE	= new StringBuffer
			  ("-- Devuelve info de XmlIO_Live a partir de un grupo, localId y classNameValue\n").
		append("SELECT Xmlio_Live.id_, Xmlio_Live.globalId, Xmlio_Live.groupId, Xmlio_Live.existInLive, Xmlio_LivePool.livePoolId\n").
		append("FROM Xmlio_Live\n").
		append("LEFT JOIN Xmlio_LivePool ON (liveChildId = Xmlio_Live.id_)\n").
		append("    WHERE Xmlio_Live.groupId = %d AND Xmlio_Live.classNameValue='%s' AND Xmlio_Live.localId='%s'\n").
		append("    LIMIT 1").toString();

	private static Log _log = LogFactoryUtil.getLog(MyDLFileEntryLocalService.class);
	private static ItemXmlIO itemXmlIO = new DLFileEntryXmlIO();
	
	public MyDLFileEntryLocalService(DLFileEntryLocalService dlFileEntryLocalService) {
		super(dlFileEntryLocalService);
	}
	
	/*
	 * Add Functions 
	 * --------------
	 */	
	
	@Override
	public DLFileEntry addFileEntry(long userId, long groupId, long folderId, String name, String title, String description, 
									String changeLog, String extraSettings, InputStream is, long size, ServiceContext serviceContext)
											throws SystemException, PortalException
	{
		
		DLFileEntry fileEntry = null;
		
		try
		{
			fileEntry = super.addFileEntry(userId, groupId, folderId, name, title, description, changeLog, extraSettings, is, size, serviceContext);
			itemXmlIO.createLiveEntry(fileEntry);
		}
		catch(DuplicateFileException dfe)
		{
			fileEntry = updateFileEntry(userId, groupId, folderId, name, "", title, description, changeLog, false, extraSettings, is, size, serviceContext);
		}
		catch (SystemException se)
		{
			throw se;
		}
		catch (PortalException pe)
		{
			throw pe;
		}
		catch (Exception e)
		{
			_log.error("Cannot add file entry (1)" + title, e);
		} 
		
		return fileEntry;
	}
	
	
	//Este es el que se llama
	@Override
	public DLFileEntry addFileEntry(long userId, long groupId, long folderId, String name, String title, String description, 
									String changeLog, String extraSettings, File file, ServiceContext serviceContext) 
											throws SystemException, PortalException
	{
		DLFileEntry fileEntry = null;
				
		try
		{
			fileEntry = super.addFileEntry(userId, groupId, folderId, name, title, description, changeLog, extraSettings, file, serviceContext);
			itemXmlIO.createLiveEntry(fileEntry);	
		}
		catch (SystemException se)
		{
			throw se;
		}
		catch (PortalException pe)
		{
			throw pe;
		}
		catch (Exception e)
		{
			_log.error("Cannot add file entry (2)" + title, e);
		} 
		
		return fileEntry;
	}
	
	//Utilizada por MILENIUM
	@Override
	public DLFileEntry addFileEntry(long userId, long groupId, long folderId, String name, String title, String description, 
									String changeLog, String extraSettings, byte[] bytes, ServiceContext serviceContext) 
											throws SystemException, PortalException
	{
		DLFileEntry fileEntry = null;
		
		try 
		{
			fileEntry = super.addFileEntry(userId, groupId, folderId, name, title, description, changeLog, extraSettings, bytes, serviceContext);
			itemXmlIO.createLiveEntry(fileEntry);
		}
		catch (SystemException se)
		{
			throw se;
		}
		catch (PortalException pe)
		{
			throw pe;
		}
		catch (Exception e)
		{
			_log.error("Cannot add file entry (3)" + title, e);
		}

		return fileEntry;
	}
	
	
	/*
	 * Update functions
	 */	
	
	//Este es el que se ejecuta
	@Override
	public DLFileEntry updateFileEntry(long userId, long groupId, long folderId, String name, String sourceFileName, 
									   String title, String description, String changeLog, boolean majorVersion, 
									   String extraSettings, File file, ServiceContext serviceContext) 
											   throws SystemException, PortalException
	{
		DLFileEntry fileEntry = null;
		
		try
		{
			fileEntry = super.updateFileEntry(userId, groupId, folderId, name, sourceFileName, 
											  title, description, changeLog, majorVersion, 
											  extraSettings, file, serviceContext);
	
			itemXmlIO.createLiveEntry(fileEntry);
		}
		catch (SystemException se)
		{
			throw se;
		}
		catch (PortalException pe)
		{
			throw pe;
		}
		catch(Exception e)
		{
			_log.error("Cannot update file entry (1)" + title, e);
		}		
		
		return fileEntry;
	}
	
	//Utilizada por MILENIUM
	@Override
	public DLFileEntry updateFileEntry(long userId, long groupId, long folderId, String name, String sourceFileName, 
									   String title, String description, String changeLog, boolean majorVersion, 
									   String extraSettings, byte[] bytes, ServiceContext serviceContext) 
											   throws SystemException, PortalException
	{
		DLFileEntry fileEntry = null;
		
		try
		{
			fileEntry = super.updateFileEntry(userId, groupId, folderId, name, sourceFileName, title, 
											  description, changeLog, majorVersion, extraSettings, 
											  bytes, serviceContext);
			
			itemXmlIO.createLiveEntry(fileEntry);
		
		}
		catch (SystemException se)
		{
			throw se;
		}
		catch (PortalException pe)
		{
			throw pe;
		}
		catch(Exception e)
		{
			_log.error("Cannot update file entry (2)" + title, e);
		}
			
		return fileEntry;
	
	}

	@Override
	public DLFileEntry updateFileEntry(long userId, long groupId, long folderId, String name, String sourceFileName, 
									   String title, String description, String changeLog, boolean majorVersion, 
									   String extraSettings, InputStream is, long size, ServiceContext serviceContext)
											   throws SystemException, PortalException
	{
		DLFileEntry fileEntry = null;
		
		try
		{
			fileEntry = super.updateFileEntry(userId, groupId, folderId, name, sourceFileName, title, 
											  description, changeLog, majorVersion, extraSettings, 
											  is, size, serviceContext);
			
			itemXmlIO.createLiveEntry(fileEntry);
		
		}
		catch (SystemException se)
		{
			throw se;
		}
		catch (PortalException pe)
		{
			throw pe;
		}
		catch(Exception e)
		{
			_log.error("Cannot update file entry (3) " + title, e);
		}
		 
		return fileEntry;
	}
	
	
	/*
	 * Delete functions
	 */
	
	//Utilizada por MILENIUM	
	@Override
	public void deleteFileEntry(long groupId, long folderId, String name) throws PortalException, SystemException 
	{
		deleteFileEntry(groupId, folderId, name, null);
	}	

	//This procedure deletes a fileEntry by the graphical interface
	@Override
	public void deleteFileEntry(long groupId, long folderId, String name, String version) throws PortalException, SystemException 
	{
		DLFileEntry fileEntry = DLFileEntryLocalServiceUtil.getFileEntry(groupId, folderId, name);
		deleteFileEntry(fileEntry, new ServiceContext());
	}
	
	//This procedure deletes a fileEntry during import process
	@Override
	public void deleteFileEntry(DLFileEntry fileEntry, ServiceContext serviceContext) throws PortalException, SystemException 
	{
		try
		{
			// La imagen siempre se borra totalmente del entorno en el que esté
			itemXmlIO.deleteLiveEntry(fileEntry, true);
			super.deleteFileEntry(fileEntry, serviceContext);
		}
		catch (PortalException pe)
		{
			throw pe;
		}
		catch (SystemException se)
		{
			throw se;
		}
		catch (Exception e)
		{
			throw new SystemException(e);
		}
	}
	
	@Override
	public InputStream getFileAsStream(
			long companyId, long userId, long groupId, long folderId,
			String name, String version)
		throws PortalException, SystemException {

		DLFileEntry fileEntry = super.getFileEntry(groupId, folderId, name);
		if (Validator.isNotNull(version)) {
			return DLLocalServiceUtil.getFileAsStream(
				companyId, fileEntry.getRepositoryId(), name, version);
		}
		else {
			return DLLocalServiceUtil.getFileAsStream(
				companyId, fileEntry.getRepositoryId(), name,
				fileEntry.getVersion());
		}
	}
}