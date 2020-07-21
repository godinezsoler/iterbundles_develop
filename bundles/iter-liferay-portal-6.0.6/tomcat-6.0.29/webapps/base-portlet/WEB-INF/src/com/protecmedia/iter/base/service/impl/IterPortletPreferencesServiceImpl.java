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

import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.protecmedia.iter.base.service.base.IterPortletPreferencesServiceBaseImpl;
import com.protecmedia.iter.base.service.util.ServiceError;

/**
 * The implementation of the iter portlet preferences remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.IterPortletPreferencesService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.IterPortletPreferencesServiceUtil} to access the iter portlet preferences remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.IterPortletPreferencesServiceBaseImpl
 * @see com.protecmedia.iter.base.service.IterPortletPreferencesServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class IterPortletPreferencesServiceImpl extends IterPortletPreferencesServiceBaseImpl 
{
	public String getPortletPreferences(HttpServletRequest request, String xmlInitParams) throws Exception
	{
		return iterPortletPreferencesLocalService.getPortletPreferences(request, xmlInitParams);
	}
	
	public String setPortletPreferences(long plid, String portletResource, String xmlPreferences) throws Exception
	{
		return iterPortletPreferencesLocalService.setPortletPreferences(plid, portletResource, xmlPreferences);
	}
	
	/**
	 * 
	 * @param request
	 * @param xmlInitParams
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public String linkSetup(HttpServletRequest request, String xmlInitParams, String data) throws Exception
	{
		return iterPortletPreferencesLocalService.linkSetup(request, xmlInitParams, data).asXML();
	}
	
	/**
	 * 
	 * @param scopeGroupId
	 * @param portletId
	 * @return
	 * @throws NumberFormatException
	 * @throws SystemException
	 */
	public String getSetups(String scopeGroupId, String portletId) throws NumberFormatException, SystemException, PortalException, DocumentException
	{
		return iterPortletPreferencesLocalService.getSetups(scopeGroupId, portletId).asXML();
	}
	
	/**
	 * 
	 * @param request
	 * @param xmlInitParams
	 * @param portletId
	 * @param portletItemIds
	 * @return
	 * @throws Exception
	 */
	public String deleteSetup(HttpServletRequest request, String xmlInitParams, String portletId, String portletItemIds, boolean checkNumReferences) throws Exception
	{
		return iterPortletPreferencesLocalService.deleteSetup(request, xmlInitParams, portletId, portletItemIds, checkNumReferences).asXML();
	}
	
	/**
	 * 
	 * @param request
	 * @param xmlInitParams
	 * @param portletId
	 * @param name
	 * @param preferences
	 * @return
	 * @throws Exception
	 */
	public String updateSetup(HttpServletRequest request, String xmlInitParams, String portletId, String portletItemIdOverride ,String name, String preferences, boolean checkNumReferences) throws Exception
	{
		return iterPortletPreferencesLocalService.updateSetup(request, xmlInitParams, portletId, portletItemIdOverride, name, preferences, checkNumReferences).asXML();
	}
	
	/**
	 * 
	 * @param companyId
	 * @param portletItemId
	 * @param portletId
	 * @param name
	 * @param oldPreferences
	 * @return
	 * @throws Exception
	 */
	public String restoreSetup(String companyId, String portletItemId, String portletId, String name, String oldPreferences) throws Exception
	{
		return iterPortletPreferencesLocalService.restoreSetup(companyId, portletItemId, portletId, name, oldPreferences);
	}
	
	/**
	 * Método que a partir de un XML descriptor del PortletItem devuelve un DOM con las preferencias de aquellos portlets vinculados a él
	 * @param portletItem Descriptor de un PortletItem
	 * @return XML con las preferencias de aquellos portlets vinculados a él
	 * @throws DocumentException 
	 * @throws ServiceError 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 */
	public String getLinkedPreferences(String portletItem) throws DocumentException, ServiceError, SecurityException, NoSuchMethodException
	{
		return iterPortletPreferencesLocalService.getLinkedPreferences(portletItem).asXML();
	}
	
}