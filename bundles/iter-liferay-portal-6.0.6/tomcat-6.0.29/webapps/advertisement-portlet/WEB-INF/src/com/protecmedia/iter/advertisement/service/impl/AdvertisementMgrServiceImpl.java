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

package com.protecmedia.iter.advertisement.service.impl;

import com.google.gson.JsonObject;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.protecmedia.iter.advertisement.service.base.AdvertisementMgrServiceBaseImpl;

/**
 * The implementation of the advertisement mgr remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.advertisement.service.AdvertisementMgrService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.advertisement.service.AdvertisementMgrServiceUtil} to access the advertisement mgr remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author protec
 * @see com.protecmedia.iter.advertisement.service.base.AdvertisementMgrServiceBaseImpl
 * @see com.protecmedia.iter.advertisement.service.AdvertisementMgrServiceUtil
 */
@Transactional( isolation = Isolation.PORTAL, rollbackFor = { Exception.class } )
public class AdvertisementMgrServiceImpl extends AdvertisementMgrServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(AdvertisementMgrServiceImpl.class);
	
	public String getSlots(String groupid) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.getSlots(groupid);
		}
        catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String addSlot(String xmlData) throws SystemException
	{
		String retVal = "";

		try
		{
			retVal = advertisementMgrLocalService.addSlot(xmlData);
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String updateSlot(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.updateSlot(xmlData);
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String deleteSlots(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.deleteSlots(xmlData);
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String getTags(String groupid) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.getTags(groupid);
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String addTag(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.addTag(xmlData);
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String updateTag(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.updateTag(xmlData);
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String deleteTags(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.deleteTags(xmlData);
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String getSkins(String groupid) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.getSkins(groupid);
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String addSkin(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.addSkin(xmlData);
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String updateSkin(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.updateSkin(xmlData);
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String deleteSkins(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.deleteSkins(xmlData);
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String getSlotTagLayout(String groupid, String plid) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.getSlotTagLayout(groupid, plid).asXML();
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String addSlotTagLayout(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.addSlotTagLayout(xmlData).asXML();
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String updateSlotTagLayout(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.updateSlotTagLayout(xmlData).asXML();
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String updatePrioritySlotTagLayout(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.updatePrioritySlotTagLayout(xmlData).asXML();
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String deleteSlotTagLayout(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.deleteSlotTagLayout(xmlData).asXML();
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String createDefaultTagConfig(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.createDefaultTagConfig(xmlData);
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String createDefaultSkinConfig(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.createDefaultSkinConfig(xmlData);
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public JsonObject publish(String data) throws SystemException
	{
		try
		{
			return advertisementMgrLocalService.publish(data);
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
	}
	
	public void publishToLive(long scopeGroupId) throws SystemException
	{
		try
		{
			advertisementMgrLocalService.publishToLive(scopeGroupId);
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
	}
	
	public boolean importContents(String importFileName) throws SystemException
	{
		boolean success = false;
		
		try
		{
			success = advertisementMgrLocalService.importContents(importFileName);
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return success;
	}
	
	public String getSlotTagCategory(String groupid, String categoryid) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.getSlotTagCategory(groupid, categoryid).asXML();
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String addSlotTagCategory(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.addSlotTagCategory(xmlData).asXML();
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String updateSlotTagCategory(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.updateSlotTagCategory(xmlData).asXML();
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String updatePrioritySlotTagCategory(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.updatePrioritySlotTagCategory(xmlData).asXML();
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String deleteSlotTagCategory(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			Document dom = advertisementMgrLocalService.deleteSlotTagCategory(xmlData);
			if(Validator.isNotNull(dom))
				retVal = dom.asXML();
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String getAdVocBranches(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.getAdVocBranches(xmlData).asXML();
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String getVocabularies(String groupid) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.getVocabularies(groupid).asXML();
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String addAdvertisementVocabulary(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.addAdvertisementVocabulary(xmlData).asXML();
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
	public String deleteAdvertisementVocabulary(String xmlData) throws SystemException
	{
		String retVal = "";
		
		try
		{
			retVal = advertisementMgrLocalService.deleteAdvertisementVocabulary(xmlData);
		}
		catch (Throwable th)
        {
               _log.error(th);
               throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		
		return retVal;
	}
	
}