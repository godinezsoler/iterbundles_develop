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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.cluster.ClusterMgr;
import com.liferay.cluster.IClusterMgr.ClusterMgrOperation;
import com.liferay.cluster.IClusterMgr.ClusterMgrType;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.base.BlockerAdBlockLocalServiceBaseImpl;

/**
 * The implementation of the blocker ad block local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.BlockerAdBlockLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.BlockerAdBlockLocalServiceUtil} to access the blocker ad block local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.BlockerAdBlockLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.BlockerAdBlockLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class BlockerAdBlockLocalServiceImpl extends BlockerAdBlockLocalServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(BlockerAdBlockLocalServiceImpl.class);
	/****************************************
	 *       SERVICIOS PARA ITERADMIN       *
	 ****************************************/
	
	private static final Map<String, String> columns;
	private static final Map<String, String> modes;
	private static final Map<String, String> dateOperators;
	private static final Map<String, String> selectOperators;
	static
	{
		columns = new HashMap<String, String>();
		columns.put("fecha",             "date");
		columns.put("requestAdBlock",    "withadblock");
		columns.put("requestNotAdBlock", "withoutadblock");
		columns.put("conversions",       "conversions");
		columns.put("total",             "total");
		columns.put("mode",              "mode");

		modes = new HashMap<String, String>();
		modes.put("active",  "1");
		modes.put("passive", "0");
		
		dateOperators = new HashMap<String, String>();
		dateOperators.put("equals",     " = ");
		dateOperators.put("beforedate", " < ");
		dateOperators.put("afterdate",  " > ");
		dateOperators.put("fromdate",   " >= ");
		dateOperators.put("todate",     " <= ");
		
		selectOperators = new HashMap<String, String>();
		selectOperators.put("equals",     " IN (%s) ");
		selectOperators.put("distinct",   " NOT IN (%s) ");
	}
	
	private static final String SQL_GET_STATISTICS = new StringBuilder()
	.append("SELECT date, withadblock, withoutadblock, conversions, total,                      \n")
	.append("       CASE WHEN mode = 0 THEN 'passive' ELSE 'active' END mode                    \n")
	.append("FROM adblock_statistics                                                            \n")
	.append("WHERE groupId = %d                                     \n") // Group filter
	.append("%s "                                                      ) // Where
	.append("%s "                                                      ) // Order
	.append("%s "                                                      ) // Limits
	.toString();
	
	private static final String BLOCKERADBLOCK_GROUP_CONFIG_COLUM_NAME = "blockeradblock";
	
	public String getDataBlockerAdBlock(long groupId, String filters, String beginRegister, String maxRegisters, String actualSort ) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException
	{
		// Valida la entrada
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_ITR_E_INVALID_GROUP_ID_ZYX);
		ErrorRaiser.throwIfFalse(Long.valueOf(beginRegister) >= 0 && Long.valueOf(maxRegisters) >= 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Calcula los filtros
		StringBuilder where = buildWhere(filters);
		
		// Calcula el orden
		StringBuilder orderBy = buildOrderBy(actualSort);
		
		// Calcula los límites
		StringBuilder limits = new StringBuilder("LIMIT ").append(beginRegister).append(StringPool.COMMA_AND_SPACE).append(maxRegisters);
		
		// Recupera los datos
		String query = String.format(SQL_GET_STATISTICS, groupId, where, orderBy, limits);
		Document statistics = PortalLocalServiceUtil.executeQueryAsDom(query);
		
		return statistics.asXML();
	}
	
	private StringBuilder buildOrderBy(String actualSort) throws DocumentException
	{
		StringBuilder orderBy = new StringBuilder(StringPool.BLANK);
		
		if (Validator.isNotNull(actualSort))
		{
			Document domSort = SAXReaderUtil.read(actualSort);
			String column = columns.get(XMLHelper.getTextValueOf(domSort, "/order/@columnid"));
			String order = XMLHelper.getLongValueOf(domSort, "/order/@asc") == 0? "DESC" : "ASC";
			
			if (Validator.isNotNull(column) && Validator.isNotNull(order))
			{
				orderBy.append("ORDER BY ").append(column).append(StringPool.SPACE).append(order).append(StringPool.NEW_LINE);
			}
		}
		
		return orderBy;
	}
	
	private StringBuilder buildWhere(String filters) throws DocumentException
	{
		StringBuilder where = new StringBuilder(StringPool.BLANK);
		
		if (Validator.isNotNull(filters))
		{
			Document domFilter = SAXReaderUtil.read(filters);
			List<Node> filterNodes = domFilter.getRootElement().selectNodes("filter");
			for (Node filter : filterNodes)
			{
				String column = columns.get(((Element) filter).attributeValue("columnid"));
				
				if (Validator.isNotNull(column))
				{
					// Fechas
					if ("date".equals(column))
					{
						String operator = dateOperators.get(((Element) filter).attributeValue("operator"));
						String value = XMLHelper.getTextValueOf(filter, "values/value/text()");
						if (Validator.isNotNull(operator) && Validator.isNotNull(value))
							where.append("  AND ").append(column).append(operator).append(value).append(StringPool.NEW_LINE);
					}
					// Modo
					else if ("mode".equals(column))
					{
						String operator = selectOperators.get(((Element) filter).attributeValue("operator"));
						List<Node> modeNodes = ((Element) filter).selectNodes("values/value");
						StringBuilder value = new StringBuilder(StringPool.BLANK);
						for (Node mode : modeNodes)
						{
							if (value.length() > 0)
								value.append(StringPool.COMMA_AND_SPACE);	
							value.append(modes.get(((Element) mode).getText()));
						}
						if (Validator.isNotNull(operator) && value.length() > 0)
							where.append("  AND ").append(column).append(String.format(operator, value)).append(StringPool.NEW_LINE);
					}
				}
			}
		}
		return where;
	}
	
	/****************************************
	 * GUARDADO Y RECUPERACIÓN DE CONFIGURACIÓN
	 ****************************************/
	public String getConfBlockerAdBlock(long groupId) throws Exception
	{
		String result = null;
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_ITR_E_INVALID_GROUP_ID_ZYX);

		result = GroupConfigTools.getGroupConfigField(groupId, BLOCKERADBLOCK_GROUP_CONFIG_COLUM_NAME);
		if (Validator.isNull(result))
			result = "<rs />";
		
		return result;
	}
	
	public String setConfBlockerAdBlock(String xmlData) throws Exception
	{
		Document dom = SAXReaderUtil.read(xmlData);
		ClusterMgr.processConfig(ClusterMgrType.AD_BLOCK, ClusterMgrOperation.SET, dom);
		Element root = dom.getRootElement();
		long groupId = XMLHelper.getLongValueOf(root, "@groupid");
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_ITR_E_INVALID_GROUP_ID_ZYX);
		root.remove(root.attribute("groupid"));
		
		setConfBlockerAdBlock(groupId, root.getDocument());
		
		return "";
	}
	
	public void setConfBlockerAdBlock(long groupId, Document data) throws Exception
	{
		GroupConfigTools.setGroupConfigField(groupId, BLOCKERADBLOCK_GROUP_CONFIG_COLUM_NAME, StringEscapeUtils.escapeSql(data.asXML()));
	}
	
	/****************************************
	 * RECOGIDA Y OBTENCION DE ESTADISTICAS *
	 ****************************************/
	
	private static final String SQL_UPDATE_STATISTICS = new StringBuilder()
	.append("INSERT INTO adblock_statistics (groupId, date, mode, withadblock, withoutadblock, conversions, total, modifiedDate) \n")
	.append("VALUES(%d, %s, %d, %d, %d, %d, %d, NOW()) \n")
	.append("ON DUPLICATE KEY UPDATE withadblock = withadblock + VALUES(withadblock), \n")
	.append("                        withoutadblock = withoutadblock + VALUES(withoutadblock), \n")
	.append("                        conversions = conversions + VALUES(conversions), \n")
	.append("                        total = total + VALUES(total), \n")
	.append("                        modifiedDate = NOW() \n")
	.toString();
	
	public void updateAdBlockStatistics(long groupId, String date, long mode, long hadadblock, long hasadblock, long total) throws IOException, SQLException
	{
		long withadblock    = (hasadblock == 1 ? total : 0);
		long withoutadblock = (hadadblock == 0 && hasadblock == 0 ? total : 0);
		long conversions    = (hadadblock == 1 && hasadblock == 0 ? total : 0);
		
		String sql = String.format(SQL_UPDATE_STATISTICS, groupId, date, mode, withadblock, withoutadblock, conversions, total);
		PortalLocalServiceUtil.executeUpdateQuery(sql);
	}
	
	private static final String SQL_GET_COUNTERS = new StringBuilder()
	.append("SELECT date, SUM(total), SUM(withadblock), SUM(withoutadblock), SUM(conversions) \n")
	.append("FROM adblock_statistics \n")
	.append("WHERE groupId = %d \n")
    .append("  AND date >= %s AND date <= %s \n")
    .append("GROUP BY date \n")
	.append("ORDER BY date ASC")
	.toString();
	
	public List<Object> getCounters (long groupId, String initialDate, String endDate)
	{
		String query = String.format(SQL_GET_COUNTERS, groupId, initialDate, endDate);
		return PortalLocalServiceUtil.executeQueryAsList(query);
	}
	
	private static final String SQL_GET_MODE_CHANGES = new StringBuilder()
	.append("SELECT date, mode \n")
	.append("FROM adblock_statistics \n")
	.append("WHERE groupId = %1$d \n")
	.append("  AND modifiedDate IN ( \n")
	.append("	SELECT MAX(modifiedDate) \n")
	.append("	FROM adblock_statistics \n")
	.append("	WHERE groupId = %1$d \n")
	.append("     AND date >= %2$s AND date <= %3$s \n")
	.append("	GROUP BY date \n")
	.append("  ) ORDER BY date ASC")
	.toString();
	
	public List<Object> getModeChanges (long groupId, String initialDate, String endDate)
	{
		String query = String.format(SQL_GET_MODE_CHANGES, groupId, initialDate, endDate);
		return PortalLocalServiceUtil.executeQueryAsList(query);
	}
}