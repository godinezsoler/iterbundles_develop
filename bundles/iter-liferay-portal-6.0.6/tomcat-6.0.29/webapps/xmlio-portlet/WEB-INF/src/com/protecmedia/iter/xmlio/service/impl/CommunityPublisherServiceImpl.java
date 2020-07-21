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

package com.protecmedia.iter.xmlio.service.impl;

import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.xmlio.service.base.CommunityPublisherServiceBaseImpl;

/**
 * The implementation of the community publisher remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.service.CommunityPublisherService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.xmlio.service.CommunityPublisherServiceUtil} to access the community publisher remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.xmlio.service.base.CommunityPublisherServiceBaseImpl
 * @see com.protecmedia.iter.xmlio.service.CommunityPublisherServiceUtil
 */
public class CommunityPublisherServiceImpl extends CommunityPublisherServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(CommunityPublisherServiceImpl.class);
	
	/**
	 * <p>Recupera las publicaciones de contenido en redes sociales programadas para un sitio.</p>
	 * <p>Permite ordenar por cualquier columna y aplicar filtros por artículo, cuenta, red social y fecha de publicación plrogramada, así
	 * como controlar los registros devueltos para paginación.</p>
	 * 
	 * @param groupId           El Id del grupo del que se quiere recuperar sus publicaciones programadas.
	 * @param filtersDefinition Los filtros a aplicar a la consulta.
	 * @param sortDefinition    El orden a aplicar a la consulta.
	 * @param beginRegister     El registro inicial a devolver (paginación).
	 * @param maxRegisters      El máximo número de registros a devolver (paginación).
	 * 
	 * @return Document con las publicaciones programadas encontradas.
	 * 
	 * @throws SystemException Si el {@code groupId}, {@code beginRegister} o {@code maxRegisters} no son un número mayor que 0,
	 *                         ocurre un error al acceder a BBDD o no se pueden procesar los filtros o el orden.
	 */
	public String getSchedulePublications(long groupId, String filtersDefinition, String sortDefinition, long beginRegister, long maxRegisters) throws SystemException
	{
		try
		{
			return communityPublisherLocalService.getSchedulePublications(groupId, filtersDefinition, sortDefinition, beginRegister, maxRegisters).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	/**
	 * <p>Permite consultar toda la información referente a una publicación, así como las fechas de vigencia del artículo a publicar y un
	 * resumen de todas sus publicaciones programadas pendientes.</p>
	 * 
	 * @param publicationId El Id de la publicación programada a consultar.
	 * @param articleId     El Id del artículo al que corresponde la programación.
	 * @return String con la información detallada de la publicación.
	 * @throws SystemException Si el Id de publicación no es correcto u ocurre un error al consultar la información.
	 */
	public String getSchedulePublicationDetail(String publicationId, String articleId) throws SystemException
	{
		try
		{
			return communityPublisherLocalService.getSchedulePublicationDetail(publicationId, articleId).asXML();
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	/**
	 * <p>Cancela las publicaciones indicadas, informando al proceso de publicación de que debe replanificarse.</p>
	 * 
	 * @param schedulesToCancel XML con los IDs de laa publicaciones a eliminar.
	 * @throws SystemException Si el {@code publicationId} es menor o igual a 0 u ocurre un error al eliminar la planificación de la BD.
	 */
	public String cancelSchedulePublication(String schedulesToCancel) throws SystemException
	{
		try
		{
			return communityPublisherLocalService.cancelSchedulePublication(schedulesToCancel);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	/**
	 * <p>Comprueba el estado de las publicaciones en curso indicadas.</p>
	 * 
	 * <p>Aplica a los Instant Articles de Facebook, que tardan en publicarse.</p>
	 * <p>Aquellos que se hayan procesado (correcta o incorrectamente), se eliminan y se registran en el Monitor.</p>
	 * 
	 * @param schedulesToCancel XML con los IDs de las publicaciones a comprobar.
	 * @return los {@code publicationId} de las publicaciones procsadas.
	 * @throws SystemException Si el {@code publicationId} es menor o igual a 0 u ocurre un error al comprobar el estado.
	 */
	public String checkProcessStatus(String schedulesToCancel) throws SystemException
	{
		try
		{
			return communityPublisherLocalService.checkProcessStatus(schedulesToCancel);
		}
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
}