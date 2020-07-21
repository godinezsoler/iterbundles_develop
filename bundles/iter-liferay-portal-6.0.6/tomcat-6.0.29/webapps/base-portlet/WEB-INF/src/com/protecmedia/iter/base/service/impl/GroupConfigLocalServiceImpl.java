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
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.util.DateUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigConstants;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.restapi.resource.user.RestApiUserRfvUtil;
import com.protecmedia.iter.base.service.base.GroupConfigLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.Preloading;

/**
 * The implementation of the group config local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.GroupConfigLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.GroupConfigLocalServiceUtil} to access the group config local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.GroupConfigLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.GroupConfigLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class GroupConfigLocalServiceImpl extends GroupConfigLocalServiceBaseImpl
{
	private static final String COL_ROBOTS = "robots";
	
	private static final String CHECK_CONFIG 	= "SELECT COUNT(*) result FROM Group_Config WHERE groupId=%s";
	private static final String GET_ROBOTS		= "SELECT robots FROM Group_Config WHERE groupId=%s";
	
	private static final String INSERT_ROBOTS	= new StringBuilder()
									.append(" INSERT INTO Group_Config(groupId, lastPublicationDate, robots) \n")
									.append(" \t values(%s, NOW(), '%s')")
									.append(" ON DUPLICATE KEY UPDATE robots=VALUES(robots) ").toString();
	private static final String GET_GOOGLEPING		= "SELECT googleping FROM Group_Config WHERE groupId=%s";
	private static final String INSERT_GOOGLEPING	= new StringBuilder()
									.append(" INSERT INTO Group_Config(groupId, lastPublicationDate, googleping) \n")
									.append(" \t values(%s, NOW(), NOW())")
									.append(" ON DUPLICATE KEY UPDATE googleping=VALUES(googleping) ").toString();
	
	public boolean checkGroupConfig(long groupId) throws SecurityException, NoSuchMethodException
	{
		boolean existsConfig = false;
		if( Validator.isNotNull(groupId) )
		{
			Document docData = PortalLocalServiceUtil.executeQueryAsDom(String.format(CHECK_CONFIG, groupId));
			String count = GetterUtil.getString( XMLHelper.getTextValueOf(docData, "rs/row/@result"), "0" );
			
			existsConfig = Integer.valueOf(count) == 1;
		}
		return existsConfig;
	}
	
	public Document getRobotsDOM(String groupId) throws SecurityException, NoSuchMethodException
	{
		return PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_ROBOTS, groupId), new String[]{COL_ROBOTS} );
	}
	
	public String getRobots(long groupId) throws SecurityException, NoSuchMethodException
	{
		String robotsTxt = "";
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList( String.format(GET_ROBOTS, groupId) );
		if(result!=null && result.size() > 0)
			robotsTxt = String.valueOf( result.get(0) );
		
		return robotsTxt;
	}
	
	public void setRobots(String groupId, String robots) throws IOException, SQLException, ServiceError, DocumentException
	{
		ErrorRaiser.throwIfFalse( Validator.isNotNull(groupId), IterErrorKeys.XYZ_ITR_E_INVALID_GROUP_ID_ZYX);
		ErrorRaiser.throwIfFalse( Validator.isNotNull(robots), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document robotsDom = SAXReaderUtil.read(robots);
		String robotsInfo = XMLHelper.getTextValueOf(robotsDom, "/rs/row/robots", "");
		
		PortalLocalServiceUtil.executeUpdateQuery( String.format(INSERT_ROBOTS, groupId, StringEscapeUtils.escapeSql(robotsInfo) ) );
	}
	
	public void setPreloading(long groupId, String preloading) throws Exception
	{
		Preloading.setConfig(groupId, preloading);
	}
	
	public String getPreloading(long groupId) throws Exception
	{
		return Preloading.getConfig(groupId);
	}
	
	public void setABTesting(long groupId, String abtesting) throws Exception
	{
		ErrorRaiser.throwIfFalse( Validator.isNotNull(groupId), 	IterErrorKeys.XYZ_ITR_E_INVALID_GROUP_ID_ZYX);
		ErrorRaiser.throwIfFalse( Validator.isNotNull(abtesting), 	IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document dom = SAXReaderUtil.read(abtesting);
		
		// Guarda la configuración
		GroupConfigTools.setGroupConfigField(groupId, GroupConfigConstants.FIELD_ABTESTING, StringEscapeUtils.escapeSql(dom.asXML()));
	}
	
	public String getABTesting(long groupId) throws Exception
	{
		return GroupConfigTools.getGroupConfigField(groupId, GroupConfigConstants.FIELD_ABTESTING);
	}

	
	public Date getGooglePing(long groupId) throws SecurityException, NoSuchMethodException, ParseException, ServiceError
	{
		Date lastPing = null;
		ErrorRaiser.throwIfFalse( Validator.isNotNull(groupId), IterErrorKeys.XYZ_ITR_E_INVALID_GROUP_ID_ZYX);
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList( String.format(GET_GOOGLEPING, groupId) );
		if(result!=null && result.size() > 0)
		{
			String ping = String.valueOf( result.get(0) );
			if( Validator.isNotNull(ping) )
				lastPing = DateUtil.getDBFormat().parse(ping);
		}
		
		return lastPing;
	}
	
	public void setGooglePing(long groupId) throws ServiceError, IOException, SQLException
	{
		ErrorRaiser.throwIfFalse( Validator.isNotNull(groupId), IterErrorKeys.XYZ_ITR_E_INVALID_GROUP_ID_ZYX);
		PortalLocalServiceUtil.executeUpdateQuery( String.format(INSERT_GOOGLEPING, groupId) );
	}
	
	public String getGoogleTools(long groupId) throws Exception
	{
		return GetterUtil.getString(GroupConfigTools.getGroupConfigField(groupId, "googletools"), "<google />");
	}
	
	public void setGoogleTools(long groupId, String googleConf) throws Exception
	{
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_ITR_E_INVALID_GROUP_ID_ZYX);
		
		Document dom = SAXReaderUtil.read(googleConf);
		
		// Valida, si se han activado las métricas de MAS, que llegue un siteId y sea correcto.
		validateMasSiteId(dom);
		
		// Guarda la configuración.
		GroupConfigTools.setGroupConfigField(groupId, "googletools", StringEscapeUtils.escapeSql(dom.asXML()));
		
		// Sincroniza los segmentos RFV con MAS si es necesario.
		RestApiUserRfvUtil.syncronizeRfvSegments(groupId);
	}
	
	/**
	 * Indica si las notificaciones están activadas para el grupo y valida que la configuración sea correcta.
	 * @param groupId El Id del grupo
	 * @return {@code true} si las notificaciones están habilitadas y se ha indicado un {@code Application ID) y un {@code Sender Id}.
	 */
	public boolean isNotificationsEnabled(long groupId)
	{
		String masEnabled = GroupConfigTools.getGroupConfigXMLField(groupId, "googletools", "/google/metricsmas/@enablemetrics");
		String wpnAppId = GroupConfigTools.getGroupConfigXMLField(groupId, "googletools", "/google/metricsmas/@appid");
		String wpnEnableUse = GroupConfigTools.getGroupConfigXMLField(groupId, "googletools", "/google/metricsmas/notifications/@enableuse");
		String wpnSenderId = GroupConfigTools.getGroupConfigXMLField(groupId, "googletools", "/google/metricsmas/notifications/@senderid");
		
		return "true".equalsIgnoreCase(masEnabled) && Validator.isNotNull(wpnAppId) && "true".equalsIgnoreCase(wpnEnableUse) && Validator.isNotNull(wpnSenderId);
	}
	
	private void validateMasSiteId(Document metricsConfig) throws ServiceError
	{
		// Comprueba que estén activadas las estadísticas de MAS. si no es así, no valida nada.
		if (!GetterUtil.getBoolean(XMLHelper.getStringValueOf(metricsConfig.getRootElement(), "/google/metricsmas/@enablemetrics"), false))
			return;
		
		// Recupera el siteId
		String siteId = XMLHelper.getStringValueOf(metricsConfig.getRootElement(), "/google/metricsmas/@appid");
		
		// Valida que no sea nulo, tenga valor y sea de longitud suficiente (Al menos 3 dígitos de CRC y otros 3 para calcularlo)
		ErrorRaiser.throwIfFalse(Validator.isNotNull(siteId) && siteId.length() >= 6, IterErrorKeys.XYZ_ITR_E_INVALID_MAS_SITE_ID_ZYX);
		
		// Separa el CRC
		int index = siteId.length() - 3;
		String idWithoutCRC = siteId.substring(0, index);
		String currentCRC = siteId.substring(index);
		
		// Calcula el CRC
		StringBuilder crc = new StringBuilder();
		
		int lengthAdjust = idWithoutCRC.length() % 3;
		if (lengthAdjust != 0)
			idWithoutCRC = idWithoutCRC.substring(0, idWithoutCRC.length() - lengthAdjust);
		
		int blockLength = idWithoutCRC.length() / 3;
		for (int i = 0; i < 3; i++)
		{
			// Extrae el bloque
			int blockIni = i * blockLength;
			int blockEnd = blockIni + blockLength;
			String block = idWithoutCRC.substring(blockIni, blockEnd);
			
			// Calcula la suma XOR
			int accumulated = (int) block.charAt(0);
			for (int k = 1; k < blockLength; k++)
			{
				accumulated = ((int) block.charAt(k)) ^ accumulated;
			}
			crc.append( (char) ((accumulated % 25) + 65) );
		}
		
		// Valida que sea igual que el introducido
		ErrorRaiser.throwIfFalse(currentCRC.equals(crc.toString()), IterErrorKeys.XYZ_ITR_E_INVALID_MAS_SITE_ID_ZYX);
	}
}