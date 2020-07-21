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
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.base.service.util.PaywallUtil;
import com.protecmedia.iter.news.service.base.ProductServiceBaseImpl;

/**
 * The implementation of the product remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.news.service.ProductService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.news.service.ProductServiceUtil} to access the product remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author protec
 * @see com.protecmedia.iter.news.service.base.ProductServiceBaseImpl
 * @see com.protecmedia.iter.news.service.ProductServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class ProductServiceImpl extends ProductServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(ProductServiceImpl.class);
	
	public String addProduct(String xml) throws SystemException
	{
		String productId = "";
		try
		{
			productId = productLocalService.addProduct(xml);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return productId;
	}
	
	public void deleteProduct(String xml) throws SystemException
	{
		try
		{
			productLocalService.deleteProduct(xml);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}

	public void updateProduct(String xml) throws SystemException
	{
		try
		{
			 productLocalService.updateProduct(xml);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void setProductsOfJournalArticle(String xml) throws SystemException
	{
		try
		{
			 productLocalService.setProductsOfJournalArticle(xml);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void updateProductsOfJournalArticle(String xml) throws SystemException
	{
		try
		{
			 productLocalService.updateProductsOfJournalArticle(xml);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void setProductsOfFileEntry(String xml) throws SystemException
	{
		try
		{
			 productLocalService.setProductsOfFileEntry(xml);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void updateProductsOfFileEntry(String xml) throws SystemException
	{
		try
		{
			 productLocalService.updateProductsOfFileEntry(xml);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String getPaywallProducts(String groupid) throws SystemException
	{
		String result = "";
		try
		{
			result = productLocalService.getPaywallProducts(groupid);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String addPaywallProduct(String xmlData) throws SystemException 
	{		
		String result = "";
		try
		{
			result = productLocalService.addPaywallProduct(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String updatePaywallProduct(String xmlData) throws SystemException
	{		
		String result = "";
		try
		{
			result = productLocalService.updatePaywallProduct(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String deletePaywallProduct(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = productLocalService.deletePaywallProduct(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getSelectedProducts(long groupId, String productsXML) throws SystemException
	{
		String result = "";
		try
		{
			result = productLocalService.getSelectedProducts(groupId, productsXML);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getSessionsByUser(String userid, String start, String quantity, String xmlFilters) throws SystemException
	{
		String result = "";
		try
		{
			result = productLocalService.getPaywallProductsAccessByType(userid, PaywallUtil.SESSION_TYPE, start, quantity, xmlFilters);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getPaywallProductsType1AccessByUser(String userid, String start, String quantity, String xmlFilters) throws SystemException
	{
		String result = "";
		try
		{
			result = productLocalService.getPaywallProductsAccessByType(userid, PaywallUtil.TYPE1, start, quantity, xmlFilters);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getPaywallProductsType2AccessByUser(String userid, String start, String quantity, String xmlFilters) throws SystemException
	{
		String result = "";
		try
		{
			result = productLocalService.getPaywallProductsAccessByType(userid, PaywallUtil.TYPE2, start, quantity, xmlFilters);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public void initPaywallDeleteSessionsTask() throws SystemException
	{
		try
		{
			productLocalService.initPaywallDeleteSessionsTask();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String setPaywallStatusMsgs(String xmlData) throws SystemException
	{
		String result = "";
		try
		{
			result = productLocalService.setPaywallStatusMsgs(xmlData);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getPaywallStatusMsgs(String groupid) throws SystemException
	{
		String result = "";
		try
		{
			result = productLocalService.getPaywallStatusMsgs(groupid);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String updatePaywall(String groupid, String activate) throws SystemException
	{
		String result = "";
		try
		{
			result = productLocalService.updatePaywall(groupid, activate);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String getPaywallMode(String groupid) throws SystemException
	{
		String result = "";
		try
		{
			result = productLocalService.getPaywallMode(groupid);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}
	
	public String exportData(String params) throws SystemException
    {
		try
		{
			return productLocalService.exportData(params).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
    }
	
	public void importData(String data) throws SystemException
    {
		try
		{
			productLocalService.importData(data);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
    }
	
	public void asignArticle(String id, String articleId) throws SystemException
    {
		try
		{
			productLocalService.asignArticle(id, articleId);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
    }
}