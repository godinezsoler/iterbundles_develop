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

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.base.service.apache.URIPteMgr;
import com.protecmedia.iter.base.service.base.IterServiceBaseImpl;


/**
 * The implementation of the iter remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.IterService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.IterServiceUtil} to access the iter remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.IterServiceBaseImpl
 * @see com.protecmedia.iter.base.service.IterServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class IterServiceImpl extends IterServiceBaseImpl {
	
	private static Log _log = LogFactoryUtil.getLog(IterServiceImpl.class);
	
	public void testIterError(String echoErrorCode, String echoErrorMessage) throws Exception{
		throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(echoErrorCode, new Exception(echoErrorMessage)));
	}
	
	public String getSystemInfo(long userId) throws Exception {
		try{
			return iterLocalService.getSystemInfo(userId);
		}catch(Exception ex){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_SYSTEM_INFO_ZYX, ex));
		}
	}
	
	public String getSystemInfoEncoded(long userId) throws Exception {
		try{
			return iterLocalService.getSystemInfoEncoded(userId);
		}catch(Exception ex){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_SYSTEM_INFO_ZYX, ex));
		}
	}
	
	@Deprecated
	public String getSystemInfo() throws Exception {
		return iterLocalService.getSystemInfo();
	}
	
	public String getVirtualHostIterAdmin(String groupId/*, boolean refresh*/) throws SystemException
	{
		String result = "";
		try
		{
			result = iterLocalService.getVirtualHostIterAdmin(groupId/*, refresh*/);
		}
		catch(Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String saveConfigIterAdmin(String groupId, String friendly, String virtual, String serverAlias, String staticsServers, String lang, String useSecureConnection) throws SystemException
	{
		String result = "";
		try
		{
			result = iterLocalService.saveConfigIterAdmin(groupId, friendly, virtual, serverAlias, staticsServers, lang, useSecureConnection);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String setMobileVersionConf(String xmlData) throws Exception
	{
		String result = "";
		try
		{
			result = iterLocalService.setMobileVersionConf(xmlData);
		}
		catch (Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
			
		}
		return result;
	}
	
	public String getMobileVersionConf(String groupId) throws Exception
	{
		String result = "";
		try
		{
			result = (iterLocalService.getMobileVersionConf(groupId)).asXML();
		}
		catch(Exception th){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public void publishToLiveMobileVersionConf(String groupId) throws Exception
	{
		try
		{
			iterLocalService.publishToLiveMobileVersionConf(groupId);
		}
		catch(Exception th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
	}
	
	public void importData(String data) throws SystemException
    {
          try
          {
        	  iterLocalService.importData(data);
          }
          catch (Throwable th)
          {
                 _log.error(th);
                 throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
          }
    }
	
	/************************************************************************************/
	public String getSystemProperties() throws Exception {
		String result = "";
		try
		{
			result = iterLocalService.getSystemProperties(null);
		}
		catch(Exception th){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String getTraceLevels() throws Exception {
		String result = "";
		try
		{
			result = iterLocalService.getTraceLevels(null);
		}
		catch (Throwable th){
			_log.error("Error encountered while get TraceLevels: ", th);
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String updateLogLevels(String xmlData) throws Exception {
		String result = "";
		try
		{
			result = iterLocalService.updateLogLevels(xmlData, null);
		}
		catch (Throwable th){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String cacheSingle() throws Exception {
		String result = "";
		try
		{
			result = iterLocalService.cacheSingle(null);
		}
		catch(Exception th){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String cacheMulti() throws Exception {
		String result = "";
		try
		{
			result = iterLocalService.cacheMulti(null);
		}
		catch (Throwable th){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	public String cacheDb() throws Exception {
		String result = "";
		try
		{
			result = iterLocalService.cacheDb(null);
		}
		catch(Exception th){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String reindex() throws Exception {
		String result = "";
		try
		{
			result = iterLocalService.reindex(null);
		}
		catch (Throwable th){
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public void launchURIPteProcess() throws Exception
	{
		try
		{
			URIPteMgr.launchURIPteProcess();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void stopURIPteProcess() throws Exception
	{
		try
		{
			URIPteMgr.stopURIPteProcess();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String getLegacyUrlRules(String scopeGroupId, boolean refresh) throws Exception
	{
		try
		{
			return iterLocalService.getLegacyUrlRules(scopeGroupId, refresh).asXML();
		}
		catch(Throwable th)
		{
			_log.error(th);
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void setLegacyUrlRules(String xml) throws Exception
	{
		try
		{
			iterLocalService.setLegacyUrlRules(xml);
		}
		catch(Throwable th)
		{
			_log.error(th);
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String getGlobalJournalTemplates() throws Exception
	{
		try
		{
			return iterLocalService.getGlobalJournalTemplates();
		}
		catch(Throwable th)
		{
			_log.error(th);
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
}