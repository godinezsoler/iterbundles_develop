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
import com.protecmedia.iter.base.service.base.IterTabsServiceBaseImpl;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;

/**
 * The implementation of the iter tabs remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.IterTabsService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.IterTabsServiceUtil} to access the iter tabs remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.IterTabsServiceBaseImpl
 * @see com.protecmedia.iter.base.service.IterTabsServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class IterTabsServiceImpl extends IterTabsServiceBaseImpl
{
	
	public String getTabs( String formId ) throws ServiceError, NoSuchMethodException
	{
		String result = "";
		
		try
		{
			result =  iterTabsLocalService.getTabs( formId );
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String getTabFields( String tabId ) throws ServiceError, NoSuchMethodException
	{
		String result = "";
		
		try
		{
			result =  iterTabsLocalService.getTabFields( tabId );
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String addTab( String xmlData ) throws DocumentException, ServiceError, NoSuchMethodException, IOException, SQLException
	{
		String result = "";
		
		try
		{
			result =  iterTabsLocalService.addTab( xmlData );
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String editTab( String xmlData ) throws DocumentException, ServiceError, SecurityException, NoSuchMethodException, IOException, SQLException
	{
		String result = "";
		
		try
		{
			result =  iterTabsLocalService.editTab( xmlData );
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String deleteTabs( String xmlData ) throws DocumentException, ServiceError, IOException, SQLException
	{
		String result = "";
		
		try
		{
			result =  iterTabsLocalService.deleteTabs( xmlData );
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String updateTabOrder( String xmlData ) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{
String result = "";
		
		try
		{
			result =  iterTabsLocalService.updateTabOrder( xmlData );
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
}