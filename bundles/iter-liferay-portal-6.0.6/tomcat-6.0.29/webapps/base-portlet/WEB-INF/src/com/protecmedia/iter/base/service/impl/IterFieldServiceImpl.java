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
import java.sql.SQLException;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.xml.DocumentException;
import com.protecmedia.iter.base.service.base.IterFieldServiceBaseImpl;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;

/**
 * The implementation of the iter field remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.IterFieldService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.IterFieldServiceUtil} to access the iter field remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.IterFieldServiceBaseImpl
 * @see com.protecmedia.iter.base.service.IterFieldServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class IterFieldServiceImpl extends IterFieldServiceBaseImpl
{
	
	public String getProfileFields(String groupId) throws ServiceError, SecurityException, NoSuchMethodException, NumberFormatException, PortalException, SystemException
	{
		String result = "";
		
		try
		{
			result =  iterFieldLocalService.getProfileFields(groupId);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String getField( String fieldId ) throws ServiceError, NoSuchMethodException
	{
		String result = "";
		
		try
		{
			result =  iterFieldLocalService.getField( fieldId );
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String addField( String xmlData ) throws Exception
	{
		String result = "";
		
		try
		{
			result =  iterFieldLocalService.addField( xmlData );
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String updateField( String xmlData ) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, IOException, SQLException
	{
		String result = "";
		
		try
		{
			result =  iterFieldLocalService.updateField( xmlData );
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String deleteFields( String xmlData ) throws DocumentException, ServiceError, IOException, SQLException, NoSuchMethodException
	{
		String result = "";
		
		try
		{
			result =  iterFieldLocalService.deleteFields( xmlData );
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String updateFieldOrder( String xmlData ) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		String result = "";
		
		try
		{
			result =  iterFieldLocalService.updateFieldOrder( xmlData );
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public void checkRegisterForm( String formId ) throws ServiceError, SecurityException, NoSuchMethodException
	{
		try
		{
			iterFieldLocalService.checkRegisterForm( formId );
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
	}
	
}