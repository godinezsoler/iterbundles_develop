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

package com.protecmedia.iter.news.service.impl;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portlet.documentlibrary.NoSuchFileEntryException;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.news.service.DLFileEntryLocalServiceUtil;
import com.protecmedia.iter.news.service.base.DLFileEntryServiceBaseImpl;

/**
 * The implementation of the d l file entry remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.news.service.DLFileEntryService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.news.service.DLFileEntryServiceUtil} to access the d l file entry remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author protec
 * @see com.protecmedia.iter.news.service.base.DLFileEntryServiceBaseImpl
 * @see com.protecmedia.iter.news.service.DLFileEntryServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class DLFileEntryServiceImpl extends DLFileEntryServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(DLFileEntryServiceImpl.class);	
	
	public void deleteFileEntry(long groupId, String globalID) throws PortalException, SystemException 
	{
		try 
		{
			DLFileEntryLocalServiceUtil.deleteFileEntry(groupId, globalID);
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
	
	public boolean deleteDLFileEntry(String groupName, long folderId, String title) throws SystemException
	{
		boolean found	= false;
		
		try
		{
			found = dlFileEntryLocalService.deleteDLFileEntry(groupName, folderId, title);
		} 
		catch(Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_DELETE_FILEENTRY_ZYX, th), th);
		}
		return found;
	}
	
	/**
	 * 
	 * @param groupId
	 * @param folderId
	 * @param name
	 * @param deleteFromLive
	 * @throws PortalException
	 * @throws SystemException
	 */
	public void deleteFileEntry2(long groupId, long folderId, String name, boolean deleteFromLive) throws Exception
	{
		try
		{
			dlFileEntryLocalService.deleteFileEntry2(groupId, folderId, name, deleteFromLive);
		} 
		catch (SystemException se)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_DELETE_FILEENTRY_ZYX, se), se);
		}
		catch (PortalException pe)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_DELETE_FILEENTRY_ZYX, pe), pe);
		}
		catch(Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_DELETE_FILEENTRY_ZYX, th), th);
		}
	}
}