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

package com.protecmedia.iter.user.service.impl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.user.service.base.FormReceivedMgrServiceBaseImpl;

/**
 * The implementation of the form received mgr remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.user.service.FormReceivedMgrService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.user.service.FormReceivedMgrServiceUtil} to access the form received mgr remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see com.protecmedia.iter.user.service.base.FormReceivedMgrServiceBaseImpl
 * @see com.protecmedia.iter.user.service.FormReceivedMgrServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class FormReceivedMgrServiceImpl extends FormReceivedMgrServiceBaseImpl {
	
	private static Log _log = LogFactoryUtil.getLog(FormReceivedMgrServiceImpl.class);
	
	public String getForms( String groupId ) throws ServiceError
	{
		String result = "";
		
		try
		{
			result =  formReceivedMgrLocalService.getForms( groupId );
		}
		catch(ORMException orme)
		{
			_log.error(orme);
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
		
		return result;
	}

	
	public String deleteFormsReceivedFromForm( String xmlData ) throws ServiceError 
	{
		String result = "";
		
		try
		{
			result =  formReceivedMgrLocalService.deleteFormsReceivedFromForm( xmlData );
		}
		catch(ORMException orme)
		{
			_log.error(orme);
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
		
		return result;
	}
	
	public String deleteFormsReceived( String xmlData ) throws ServiceError 
	{
		String result = "";
		
		try
		{
			result =  formReceivedMgrLocalService.deleteFormsReceived( xmlData );
		}
		catch(ORMException orme)
		{
			_log.error(orme);
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
		
		return result;
	}
	
	public String getInputCtrl(String formfieldid)throws ServiceError 
	{
		String result = "";
		
		try
		{
			result =  formReceivedMgrLocalService.getInputCtrl(formfieldid);
		}
		catch(ORMException orme)
		{
			_log.error(orme);
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
		
		return result;
	}
	
	public String getReceivedDataForm(String formid) throws ServiceError 
	{
		String result = null;
		
		try
		{
			result =  formReceivedMgrLocalService.getReceivedDataForm(formid).asXML();
		}
		catch(ORMException orme)
		{
			_log.error(orme);
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
		
		return result;
		
	}
	
	public String getGenericReceivedDataForm(String xmlOrderFilterForm) throws ServiceError 
	{
		String result = null;
		
		try
		{
			result =  formReceivedMgrLocalService.getGenericReceivedDataForm(xmlOrderFilterForm).asXML();
		}
		catch(ORMException orme)
		{
			_log.error(orme);
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
		
		return result;
		
	}
	
	public String putCheck(String xmlData) throws ServiceError 
	{
		String result = "";
		
		try
		{
			result =  formReceivedMgrLocalService.putCheck(xmlData);
		}
		catch(ORMException orme)
		{
			_log.error(orme);
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
		
		return result;
		
	}

	public String putListCheck(String xmlData) throws ServiceError 
	{
		String result = "";
		
		try
		{
			result =  formReceivedMgrLocalService.putListCheck(xmlData);
		}
		catch(ORMException orme)
		{
			_log.error(orme);
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
		
		return result;
		
	}
	
	public String getFilesFromFormReceived(String formReceivedId) throws ServiceError 
	{
		String result = "";
		
		try
		{
			result =  formReceivedMgrLocalService.getFilesFromFormReceived(formReceivedId);
		}
		catch(ORMException orme)
		{
			_log.error(orme);
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
		
		return result;
	}
	
	public void getFormReceivedDetail(HttpServletRequest request, HttpServletResponse response, String formid, String formreceivedid) throws ServiceError 
	{
		try
		{
			formReceivedMgrLocalService.getFormReceivedDetail(request, response, formid, formreceivedid);
		}
		catch(ORMException orme)
		{
			_log.error(orme);
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
	}
	
	public void exportFormsToCSV(	HttpServletRequest request, HttpServletResponse response,  String xmlFiltersOrder, String nameForDateSentColumn, String nameForUserColum	) throws ServiceError 
	{
		try
		{
			formReceivedMgrLocalService.exportFormsToCSV(request, response, xmlFiltersOrder, nameForDateSentColumn, nameForUserColum);
		}
		catch(ORMException orme)
		{
			_log.error(orme);
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
	}
	public String getGridValues(String xmlData) throws ServiceError{
		String result = null;
		try
		{
			result = formReceivedMgrLocalService.getGridValues(xmlData).asXML();
		}
		catch(ORMException orme)
		{
			_log.error(orme);
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
		return result;
	}
	
}