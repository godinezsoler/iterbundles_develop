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
import java.sql.SQLException;

import org.apache.http.client.ClientProtocolException;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.xml.DocumentException;
import com.protecmedia.iter.base.service.base.IterFormServiceBaseImpl;

/**
 * The implementation of the iter form remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.IterFormService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.IterFormServiceUtil} to access the iter form remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.IterFormServiceBaseImpl
 * @see com.protecmedia.iter.base.service.IterFormServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class IterFormServiceImpl extends IterFormServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(IterFormServiceImpl.class);

	public String getForms( long groupId ) throws SystemException
	{
		String result = "";
		
		try
		{
			result =  iterFormLocalService.getForms( groupId );
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String getForm( String formid ) throws SystemException
	{
		String result = "";
		
		try
		{
			result =  iterFormLocalService.getForm( formid );
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String addForm( String xmlData ) throws SystemException
	{
		String result = "";
		
		try
		{
			result =  iterFormLocalService.addForm( xmlData );
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String editForm( String formId ) throws SystemException
	{
		String result = "";
		
		try
		{
			result =  iterFormLocalService.editForm( formId );
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String deleteForms( String xmlData ) throws SystemException
	{
		String result = "";
		
		try
		{
			result =  iterFormLocalService.deleteForms( xmlData );
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String getFormProducts( String formId ) throws SystemException
	{
		String result = "";
		
		try
		{
			result =  iterFormLocalService.getFormProducts( formId );
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String getFormsList( long groupId ) throws ServiceError, SecurityException, NoSuchMethodException
	{
		String result = "";
		
		try
		{
			result =  iterFormLocalService.getFormsList( groupId );
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String getFormDefinition(String formid) throws ServiceError, NoSuchMethodException, SecurityException, DocumentException
	{
		String result = "";
		
		try
		{
			result =  iterFormLocalService.getFormDefinition(formid);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public void publishToLive(String formIds) throws SecurityException, NoSuchMethodException, ServiceError, PortalException, SystemException, UnsupportedEncodingException, ClientProtocolException, IOException
	{
		try
		{
			iterFormLocalService.publishToLive(formIds);
		}
		catch(Throwable t)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(t.getMessage(), t), t);
		}
	}
	
	public void importForms(String forms) throws ServiceError, DocumentException, NoSuchMethodException, SecurityException, IOException, SQLException, SystemException
	{
		try
		{
			iterFormLocalService.importForms(forms);
		}
		catch (ServiceError se)
		{
			if (se.getErrorCode().equals(IterErrorKeys.XYZ_ITR_FK_FORM_HANDLER_EMAIL_SMTPSERVER_ZYX))
				se.setErrorCode(IterErrorKeys.XYZ_ITR_FK_FORM_HANDLER_EMAIL_SMTPSERVER2_ZYX);
			
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(se.getMessage(), se), se);
		}
		catch(Throwable t)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(t.getMessage(), t), t);
		}
	}

	public void updateFormFieldsIds(String fields) throws SystemException
	{
		try
		{
			iterFormLocalService.updateFormFieldsIds(fields);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
}