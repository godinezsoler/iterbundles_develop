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
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.WidgetUtil;
import com.protecmedia.iter.news.service.base.CatalogServiceBaseImpl;

/**
 * The implementation of the catalog remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.news.service.CatalogService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.news.service.CatalogServiceUtil} to access the catalog remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author protec
 * @see com.protecmedia.iter.news.service.base.CatalogServiceBaseImpl
 * @see com.protecmedia.iter.news.service.CatalogServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class})
public class CatalogServiceImpl extends CatalogServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(CatalogServiceImpl.class);
	
	/**
	 * 
	 * @param groupId
	 * @return
	 * @throws Exception
	 */
	public String getCatalogs(long groupId, String type) throws Exception
	{
		String result = "";
		
		try
		{
			result = catalogLocalService.getCatalogs(groupId, type).asXML();
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		return result;
	}

	/**
	 * 
	 * @param groupId
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public String addCatalog(long groupId, String data) throws Exception
	{
		String result = "";
		
		try
		{
			result = catalogLocalService.addCatalog(groupId, data).asXML();
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		return result;
	}
	
	/**
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public String updateCatalog(String data) throws Exception
	{
		String result = "";
		
		try
		{
			result = catalogLocalService.updateCatalog(data).asXML();
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		return result;
	}

	/**
	 * 
	 * @param catalogid
	 * @throws Exception
	 */
	public String deleteCatalogs(String catalogids) throws Exception
	{
		String result = null;
		try
		{
			result = catalogLocalService.deleteCatalogs(catalogids);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}

	/**
	 * 
	 * @param catalogid
	 * @param includeElements
	 * @return
	 * @throws Exception
	 */
	public String getCatalogPages(String catalogid, boolean includeElements) throws Exception
	{
		String result = "";
		
		try
		{
			result = catalogLocalService.getCatalogPages(catalogid, includeElements).asXML();
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		return result;
	}
	
	/**
	 * 
	 * @param catalogpageid
	 * @param includeElements
	 * @return
	 * @throws Exception
	 */
	public String getCatalogPage(String catalogpageid, boolean includeElements) throws Exception
	{
		String result = "";
		
		try
		{
			result = catalogLocalService.getCatalogPage(catalogpageid, includeElements).asXML();
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		return result;
	}

	/**
	 * 
	 * @param groupId
	 * @return
	 * @throws Exception
	 */
	public String getHeaderPages(long groupId) throws Exception
	{
		String result = "";
		
		try
		{
			result = catalogLocalService.getCatalogPages(groupId, WidgetUtil.CATALOG_TYPE_HEADER, true).asXML();
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		return result;
	}

	/**
	 * 
	 * @param groupId
	 * @return
	 * @throws Exception
	 */
	public String getMenuPages(long groupId) throws Exception
	{
		String result = "";
		
		try
		{
			result = catalogLocalService.getCatalogPages(groupId, WidgetUtil.CATALOG_TYPE_MENU, true).asXML();
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		return result;
	}

	/**
	 * 
	 * @param groupId
	 * @return
	 * @throws Exception
	 */
	public String getFooterPages(long groupId) throws Exception
	{
		String result = "";
		
		try
		{
			result = catalogLocalService.getCatalogPages(groupId, WidgetUtil.CATALOG_TYPE_FOOTER, true).asXML();
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		return result;
	}
	
	public String getBodyPages(long groupId) throws SystemException
	{
		String result = "";
		
		try
		{
			result = catalogLocalService.getCatalogPages(groupId, WidgetUtil.CATALOG_TYPE_BODY, true).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}

	/**
	 * 
	 * @param catalogId
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public String addCatalogPage(String catalogId, String data) throws Exception
	{
		String result = "";
		
		try
		{
			result = catalogLocalService.addCatalogPage(catalogId, data).asXML();
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		return result;
	}
	
	/**
	 * 
	 */
	public String addCatalogPageAndElements(String data) throws Exception
	{
		String result = "";
		
		try
		{
			result = catalogLocalService.addCatalogPageAndElements(data).asXML();
		}
		catch (Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getCatalogPageURL(String data) throws Exception
	{
		String result = "";
		
		try
		{
			result = catalogLocalService.getCatalogPageURL(data);
		}
		catch (Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	

	/**
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public String updateCatalogPage(String data) throws Exception
	{
		String result = "";
		
		try
		{
			result = catalogLocalService.updateCatalogPage(data).asXML();
		}
		catch(Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	/**
	 * 
	 * @param catalogPageIds
	 * @throws Exception
	 */
	public String deleteCatalogPages(String catalogPageIds) throws Exception
	{
		String result = null;
		try
		{
			result = catalogLocalService.deleteCatalogPages(catalogPageIds);
		}
		catch(Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param catalogPageId
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public String addCatalogElements(String data) throws Exception
	{
		String result = "";
		
		try
		{
			result = catalogLocalService.addCatalogElements(data).asXML();
		}
		catch (Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getDataCatalogElement( String elementIds  ) throws Exception
	{
		String result = "";
		
		try
		{
			result = catalogLocalService.getDataCatalogElement( elementIds);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		return result;
	}


}