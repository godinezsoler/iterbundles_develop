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

import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.LongWrapper;
import com.liferay.portal.kernel.xml.Document;
import com.protecmedia.iter.base.service.base.VisitsStatisticsServiceBaseImpl;
import com.protecmedia.iter.base.service.util.IterAdmin;

/**
 * The implementation of the visits statistics remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.VisitsStatisticsService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.VisitsStatisticsServiceUtil} to access the visits statistics remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.VisitsStatisticsServiceBaseImpl
 * @see com.protecmedia.iter.base.service.VisitsStatisticsServiceUtil
 */
public class VisitsStatisticsServiceImpl extends VisitsStatisticsServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(VisitsStatisticsServiceImpl.class);
	
	/**
	 * Recupera la configuración de las estadísticas de visitas de la caché.
	 * Si no existe configuración para el grupo, retorna nulo.
	 * 
	 * @param groupId			ID del grupo.
	 * @return					xml de configuración de la group_config.
	 * @throws SystemException	Si los datos de entrada son incorrectos.
	 */
	public String getVisitsStatisticsConfig(long groupId) throws SystemException
	{
		try
		{
			return visitsStatisticsLocalService.getVisitsStatisticsConfig(groupId);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	/**
	 * Guarda la configuración de visitas en la tabla group_config y actualiza la caché
	 * de configuraciones de grupos de los Tomcats.
	 * 
	 * @param groupId			ID del grupo.
	 * @param data				xml de configuración a guardar en la group_config.
	 * @throws SystemException	Si los datos de entrada son incorrectos u ocurre algún error
	 * 							durante el guardado de la configuración. 
	 */
	public void setVisitsStatisticsConfig(long groupId, String data) throws SystemException
	{
		try
		{
			visitsStatisticsLocalService.setVisitsStatisticsConfig(groupId, data);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void notifyVocabulariesModificationsToMAS(long groupId) throws SystemException
	{
		try
		{
			visitsStatisticsLocalService.notifyVocabulariesModificationsToMAS(groupId);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String exportData(String params) throws com.liferay.portal.kernel.exception.SystemException 
	{
		String result = null;
		try
		{
			LongWrapper groupId = new LongWrapper(0);
			IterAdmin.processExportImportInfo(params, groupId);

			result = visitsStatisticsLocalService.getVisitsStatisticsConfig(groupId.getValue());
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}

	public void importData(String data) throws com.liferay.portal.kernel.exception.SystemException 
	{
		try
		{
			LongWrapper groupId = new LongWrapper(0);
			Document dom = IterAdmin.processExportImportInfo(data, groupId);

			visitsStatisticsLocalService.setVisitsStatisticsConfig(groupId.getValue(), dom.asXML());
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
}