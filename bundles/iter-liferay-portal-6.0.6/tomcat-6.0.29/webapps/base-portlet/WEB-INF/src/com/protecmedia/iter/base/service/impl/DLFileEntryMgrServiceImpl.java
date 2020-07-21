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

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.protecmedia.iter.base.service.base.DLFileEntryMgrServiceBaseImpl;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;

/**
 * The implementation of the d l file entry mgr remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.DLFileEntryMgrService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.DLFileEntryMgrServiceUtil} to access the d l file entry mgr remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.DLFileEntryMgrServiceBaseImpl
 * @see com.protecmedia.iter.base.service.DLFileEntryMgrServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class DLFileEntryMgrServiceImpl extends DLFileEntryMgrServiceBaseImpl
{
	public String uploadFileEntry(HttpServletRequest request, HttpServletResponse response, InputStream is, String xmlInitParams) throws Exception
	{
		String result = "";
		
		try
		{
			result =  dlFileEntryMgrLocalService.uploadFileEntry(request, response, is, xmlInitParams);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String getFileEntries(String groupid) throws Exception
	{
		String result = "";
		
		try
		{
			result =  dlFileEntryMgrLocalService.getFileEntries(groupid);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String deleteFileEntries(String xmlData) throws Exception
	{
		String result = "";
		
		try
		{
			result =  dlFileEntryMgrLocalService.deleteFileEntries(xmlData);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
}