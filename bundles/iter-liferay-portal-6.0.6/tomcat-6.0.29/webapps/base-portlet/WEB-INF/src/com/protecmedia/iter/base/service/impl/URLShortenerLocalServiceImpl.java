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

import java.util.List;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.protecmedia.iter.base.service.base.URLShortenerLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.IterAdmin;

/**
 * The implementation of the u r l shortener local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.URLShortenerLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.URLShortenerLocalServiceUtil} to access the u r l shortener local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.URLShortenerLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.URLShortenerLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class URLShortenerLocalServiceImpl extends URLShortenerLocalServiceBaseImpl
{
	private String GC_FIELD_NAME = "shorteners";
	private String XPATH_SHORTENER_PATH = "/shorteners/shortener";
	private String XPATH_ATTR_SERVICE = "@service";
	private String XPATH_GET_SHORTENER_BY_SERVICE = "shortener[@service='%s']";
	
	/**
	 * Guarda la configuración de acortadores de un grupo.
	 * @param groupId el Id del grupo.
	 * @param data    XML con los acortadores a configurar.
	 */
	public String setShorteners(long groupId, String data) throws Exception
	{
		// Valida que se informe grupo y que los datos sean un XML
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document config = null;
		try { config = SAXReaderUtil.read(data); }
		catch (DocumentException e) { ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX); }
		
		// Guarda la configuración
		GroupConfigTools.setGroupConfigField(groupId, GC_FIELD_NAME, config.asXML());
		
		// Retorna la configuración guardada del grupo
		return getShorteners(groupId);
	}
	
	/**
	 * Recupera la configuracion de acortadores de un grupo.
	 * @param groupId el Id del grupo.
	 */
	public String getShorteners(long groupId) throws Exception
	{
		// Valida que se informe grupo y que los datos sean un XML
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Recupera la configuración de la caché de la group_config
		return GroupConfigTools.getGroupConfigField(groupId, GC_FIELD_NAME);
	}
	
	/**
	 * Se llama desde la importación de IterAdmin, cuando no se pueden sobreescribir los registros existentes.
	 * @param groupId       el Id del grupo.
	 * @param newShorteners el XML con los acortadores a configurar.
	 * @param updtIfExist   indicador de si se permite actualizar la configuración existente.
	 * @throws Exception    Si ocurre un error al actualiar la configuración de acortadores del grupo.
	 */
	public void mergeShorteners(long groupId, Document newShorteners, boolean updtIfExist) throws Exception
	{
		// Si se pueden actualizar los existentes o no hay configuración, inserta todo
		if (updtIfExist || Validator.isNull(getShorteners(groupId)))
		{
			setShorteners(groupId, newShorteners.asXML());
		}
		// Si no, comprueba si ya existe algún acortador y, si es así, lanza un error
		else
		{
			// Recupera los acortadores actuales
			Element currentShorteners = SAXReaderUtil.read(getShorteners(groupId)).getRootElement();
			
			// Recupera los acortadores a importar
			List<Node> shorteners = newShorteners.selectNodes(XPATH_SHORTENER_PATH);
			
			for (Node shortener : shorteners)
			{
				String service = XMLHelper.getTextValueOf(shortener, XPATH_ATTR_SERVICE);
				
				// Si ya existe configuración para el acortador, lanza un error 
				if (currentShorteners.selectSingleNode(String.format(XPATH_GET_SHORTENER_BY_SERVICE, service)) != null)
				{
					ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, String.format("%s(%s)", IterAdmin.IA_CLASS_URL_SHORTENER, service));
				}
				// Si no, lo inserta
				else
				{
					currentShorteners.add(shortener.detach());
				}
			}
			
			// Graba la configuración
			setShorteners(groupId, currentShorteners.asXML());
		}
	}
}