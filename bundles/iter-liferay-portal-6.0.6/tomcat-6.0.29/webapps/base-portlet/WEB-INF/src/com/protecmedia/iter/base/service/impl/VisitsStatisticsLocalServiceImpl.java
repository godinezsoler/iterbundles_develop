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
import java.util.List;
import java.util.Map;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ABTestingMgr;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.Validator;
//import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.service.PortalLocalServiceUtil;
//import com.liferay.portlet.asset.model.AssetVocabulary;
//import com.liferay.portlet.asset.service.AssetVocabularyLocalServiceUtil;
import com.protecmedia.iter.base.metrics.VisitsStatisticMgr;
import com.protecmedia.iter.base.service.base.VisitsStatisticsLocalServiceBaseImpl;
//import com.protecmedia.iter.base.service.util.GroupMgr;
//import com.protecmedia.iter.base.service.util.MASUtil;
import com.protecmedia.iter.base.util.VisitsStatisticsUtil;

/**
 * The implementation of the visits statistics local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.VisitsStatisticsLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.VisitsStatisticsLocalServiceUtil} to access the visits statistics local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.VisitsStatisticsLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.VisitsStatisticsLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class VisitsStatisticsLocalServiceImpl extends VisitsStatisticsLocalServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(VisitsStatisticsLocalServiceImpl.class);
	
	private static final Object articleLock 	= new Object();
	private static final Object abtestingLock 	= new Object();
	
// -- Esta opción no funciona si:
// -- 	- Deja de existir algún test porque falla la sentencia y se anulan todas las operaciones que contenía 
// -- 	- Cambia el experimento y el nuevo tiene menos variantes, de esta forma una estadística del experimento anterior
// -- 	  insertaría una variante que NO existe en el nuevo	
//INSERT INTO abtesting_variant (testid, variantname, modifiedDate, updatePending, visits, extvisits)
//	  VALUES 
//	    ( (SELECT testid FROM abtesting WHERE groupId = 158185 AND articleId = '404706'), 'A', NOW(), 1, 2, 2 ),
//	    ( (SELECT testid FROM abtesting WHERE groupId = 158185 AND articleId = '404706'), 'B', NOW(), 1, 2, 2 )
//	ON DUPLICATE KEY UPDATE visits        = visits    + VALUES(visits), 
//	                        extvisits     = extvisits + VALUES(extvisits),
//	                        modifiedDate  = NOW(),
//	                        updatePending = 1

//UPDATE  abtesting_variant
//JOIN 
//      (
//        SELECT testid, 'A' as variantname, 4 as _visits, 4 as _extvisits  FROM abtesting WHERE groupId = 158185 AND articleId = '404706' 
//        UNION ALL
//        SELECT testid, 'B' as variantname, 5 as _visits, 5 as _extvisits  FROM abtesting WHERE groupId = 158185 AND articleId = '44404706'
//      ) vals ON abtesting_variant.variantname = vals.variantname AND abtesting_variant.testid = vals.testid
//  SET visits    = visits    + _visits, 
//      extvisits = extvisits + _extvisits,
//      modifiedDate  = NOW(),
//      updatePending = 1

private static final String SQL_ABTESTING_SELECT_VISITS = "SELECT %s as variantid, %s as _visits, %d as _extvisits \n";

private static final String SQL_ABTESTING_UPDT_VISITS = new StringBuilder(
	"UPDATE  abtesting_variant																						\n").append(
	"JOIN 																											\n").append(
	"      (																										\n").append(
	"		%s																										\n").append(
	"      ) vals USING (variantid)																					\n").append(
	"  SET visits    = visits    + _visits, 																		\n").append(
	"      extvisits = extvisits + _extvisits,																		\n").append(
	"      modifiedDate  = NOW(),																					\n").append(
	"      updatePending = 1																						\n").toString();
		
private static final String SQL_ABTESTING_SELECT_IMPRESIONS = "SELECT %s as variantid, %s as _prints \n";

private static final String SQL_ABTESTING_UPDT_IMPRESIONS = new StringBuilder(
	"UPDATE  abtesting_variant																						\n").append(
	"JOIN 																											\n").append(
	"      (																										\n").append(
	"		%s																										\n").append(
	"      ) vals USING (variantid)																					\n").append(
	"  SET prints    = prints    + _prints, 																		\n").append(
	"      modifiedDate  = NOW(),																					\n").append(
	"      updatePending = 1																						\n").toString();
	
	
	public void updateArticle(String type, List<Map<String, String>> statistics) throws IOException, SQLException
	{
		synchronized (articleLock)
		{
			if ("visits".equals(type))
				updateArticleVisits(statistics);
			else if ("readings".equals(type))
				updateArticleReadings(statistics);
			else if ("social".equals(type))
				updateArticleSocial(statistics);
		}
	}
	
	public void updateABtesting(String type, List<Map<String, String>> statistics) throws IOException, SQLException
	{
		synchronized (abtestingLock)
		{
			if ("visits".equals(type))
				updateABtestingVisits(statistics);
			else if ("impresions".equals(type))
				updateABtestingImpresions(statistics);
		}
	}
	
	private void updateABtestingVisits(List<Map<String, String>> statistics) throws IOException, SQLException
	{
		if (Validator.isNotNull(statistics))
		{
			StringBuilder values = new StringBuilder();
			
			for (Map<String, String> stats : statistics)
			{
				if (values.length() > 0)
					values.append("UNION ALL \n");
				
				values.append( String.format(SQL_ABTESTING_SELECT_VISITS, 	
								stats.get(VisitsStatisticMgr.VARIANTID),		stats.get(VisitsStatisticMgr.VISITS),		
								Integer.valueOf(stats.get(VisitsStatisticMgr.VISITS))*Integer.valueOf(stats.get(VisitsStatisticMgr.EXTERNAL))) );
			}
			
			if (values.length() > 0)
			{
				String sql = String.format(SQL_ABTESTING_UPDT_VISITS, values.toString());
				_log.trace(sql);
				
				PortalLocalServiceUtil.executeUpdateQuery(sql);
			}
		}
	}

	private void updateABtestingImpresions(List<Map<String, String>> statistics) throws IOException, SQLException
	{
		if (Validator.isNotNull(statistics))
		{
			StringBuilder values = new StringBuilder();
			
			for (Map<String, String> stats : statistics)
			{
				if (values.length() > 0)
					values.append("UNION ALL \n");
				
				values.append( String.format(SQL_ABTESTING_SELECT_IMPRESIONS, 	stats.get(VisitsStatisticMgr.VARIANTID), 
								stats.get(VisitsStatisticMgr.VISITS)) );
			}
			
			if (values.length() > 0)
			{
				String sql = String.format(SQL_ABTESTING_UPDT_IMPRESIONS, values.toString());
				_log.trace(sql);
				
				PortalLocalServiceUtil.executeUpdateQuery(sql);
			}
		}
	}

	
	
	private static final String SQL_UPDATE_ARTICLE_VISITS = new StringBuilder()
	.append("INSERT INTO article_visits (groupId, articleId, visits, modifiedDate, updatePending) VALUES \n")
	.append("%s \n")
	.append("ON DUPLICATE KEY UPDATE visits = visits + VALUES(visits), modifiedDate = NOW(), updatePending = 1")
	.toString();
	
	private void updateArticleVisits(List<Map<String, String>> statistics) throws IOException, SQLException
	{
		if (statistics.size() > 0)
		{
			StringBuilder values = new StringBuilder();
			
			for (Map<String, String> stats : statistics)
			{
				if (values.length() > 0)
					values.append(", \n");
				
				values.append("(").append(stats.get("groupId")).append(", '").append(stats.get("articleId")).append("', ").append(stats.get("visits")).append(", now(), 1)");
			}
			
			String sql = String.format(SQL_UPDATE_ARTICLE_VISITS, values.toString());
			PortalLocalServiceUtil.executeUpdateQuery(sql);
		}
	}
	
	private static final String SQL_UPDATE_ARTICLE_READS = new StringBuilder()
	.append("INSERT INTO article_visits (groupId, articleId, readings, modifiedDate, updatePending) VALUES \n")
	.append("%s \n")
	.append("ON DUPLICATE KEY UPDATE readings = readings + VALUES(readings), modifiedDate = NOW(), updatePending = 1")
	.toString();
	
	private void updateArticleReadings(List<Map<String, String>> statistics) throws IOException, SQLException
	{
		if (statistics.size() > 0)
		{
			StringBuilder values = new StringBuilder();
			
			for (Map<String, String> stats : statistics)
			{
				if (values.length() > 0)
					values.append(", \n");
				
				values.append("(").append(stats.get("groupId")).append(", '").append(stats.get("articleId")).append("', ").append(stats.get("visits")).append(", now(), 1)");
			}
			
			String sql = String.format(SQL_UPDATE_ARTICLE_READS, values.toString());
			PortalLocalServiceUtil.executeUpdateQuery(sql);
		}
	}
	
	private static final String INSERT_OR_UPDATE_STATISTICS_COUNTER = new StringBuffer()
	.append("INSERT INTO article_visits (groupId, articleId, sharings, comments, modifiedDate, updatePending) VALUES \n")
	.append("%s \n")
	.append("ON DUPLICATE KEY UPDATE sharings=sharings+VALUES(sharings), comments=comments+VALUES(comments), modifiedDate=NOW(), updatePending=1 \n")
	.toString();
	
	private void updateArticleSocial(List<Map<String, String>> statistics) throws IOException, SQLException
	{
		if (statistics.size() > 0)
		{
			StringBuilder values = new StringBuilder();
			
			for (Map<String, String> stats : statistics)
			{
				if (values.length() > 0)
					values.append(", \n");
				
				values.append("(").append(stats.get("groupId")).append(", '").append(stats.get("articleId")).append("', ").append(stats.get("sharings")).append(", ").append(stats.get("comments")).append(", now(), 1)");
			}
			
			String sql = String.format(INSERT_OR_UPDATE_STATISTICS_COUNTER, values.toString());
			PortalLocalServiceUtil.executeUpdateQuery(sql);
		}
	}
	
	/***************************************************
	 * ESTADISTICAS DE VISITAS A PORTADAS DE SECCIONES *
	 ***************************************************/
	private static final String SQL_UPDATE_SECTION_VISITS = new StringBuilder("INSERT INTO section_visits (groupId, plid, visits, articleVisits, modifiedDate, updatePending) VALUES \n")
	 .append("%s \n")
	 .append("ON DUPLICATE KEY UPDATE visits = visits + VALUES(visits), articleVisits = articleVisits + VALUES(articleVisits), modifiedDate = NOW(), updatePending = 1")
	 .toString();

	public void updateSectionVisits(List<Map<String, String>> statistics) throws IOException, SQLException
	{
		StringBuilder values = new StringBuilder();
		
		for (Map<String, String> stats : statistics)
		{
			if (values.length() > 0)
				values.append(", \n");
			
			values.append("(").append(stats.get("groupId")).append(", ").append(stats.get("plid")).append(", ").append(stats.get("visits")).append(", ").append(GetterUtil.getLong(stats.get("articleVisits"))).append(", now(), 1)");
		}
	
		String sql = String.format(SQL_UPDATE_SECTION_VISITS, values.toString());
		PortalLocalServiceUtil.executeUpdateQuery(sql);
	}
	
	private static final String SQL_UPDATE_SECTION_VISIST_STATS = new StringBuilder()
		.append("INSERT INTO section_visits_statistics (groupId, plid, statisticsDate, visits)")
		.append("SELECT groupId, plid, '%s' statisticsDate, visits - lastUpdateVisits visits FROM section_visits WHERE visits - lastUpdateVisits > 0")
		.toString();
	private static final String SQL_UPDATE_SECTION_LASTUPDATEVISIST = "UPDATE section_visits SET lastUpdateVisits = visits";
	
	public void updateSectionVisitsStatistics(String date) throws ParseException, IOException, SQLException
	{
		// Vuelca las visitas a la tabla de estadísticas
		PortalLocalServiceUtil.executeUpdateQuery(String.format(SQL_UPDATE_SECTION_VISIST_STATS, date));
		
		// Actualiza los contadores de ultimas visitas actualizadas
		PortalLocalServiceUtil.executeUpdateQuery(SQL_UPDATE_SECTION_LASTUPDATEVISIST);
	}
	
	/***************************************
	 * ESTADISTICAS DE VISITAS A METADATOS *
	 ***************************************/
	private static final String SQL_UPDATE_CATEGORY_VISITS = new StringBuilder("INSERT INTO metadata_visits (groupId, categoryId, visits, modifiedDate, updatePending) VALUES \n")
	 .append("%s \n")
	 .append("ON DUPLICATE KEY UPDATE visits = visits + VALUES(visits), modifiedDate = NOW(), updatePending = 1")
	 .toString();

	public void updateCategoryVisits(Map<String, Long> statistics) throws IOException, SQLException
	{
		StringBuilder values = new StringBuilder();
		
		for (String key : statistics.keySet())
		{
			if (values.length() > 0)
				values.append(", \n");
			
			values.append("(").append(key).append(", ").append(statistics.get(key)).append(", now(), 1)");
		}
	
		String sql = String.format(SQL_UPDATE_CATEGORY_VISITS, values.toString());
		PortalLocalServiceUtil.executeUpdateQuery(sql);
	}

	//////////////////////////////////////////////////
	//         CONFIGURACION INTERADMIN             //
	//////////////////////////////////////////////////
	
	/**
	 * Recupera la configuración de las estadísticas de visitas de la caché.
	 * Si no existe configuración para el grupo, retorna nulo.
	 * 
	 * @param groupId	 ID del grupo.
	 * @return			 xml de configuración de la group_config.
	 * @throws Exception Si los datos de entrada son incorrectos.
	 */
	public String getVisitsStatisticsConfig(long groupId) throws Exception
	{
		// Valida los parámetros de entrada
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Recupera la configuración
		return GroupConfigTools.getGroupConfigField(groupId, "visitstatistics");
	}
	
	/**
	 * Guarda la configuración de estadísticas de visitas en la tabla group_config y actualiza la caché
	 * actualiza la caché de configuraciones de grupos de los Tomcats.
	 * 
	 * @param groupId	 ID del grupo.
	 * @param data		 xml de configuración a guardar en la group_config.
	 * @throws Exception Si los datos de entrada son incorrectos u ocurre algún error
	 * 					 durante el guardado de la configuración. 
	 */
	public void setVisitsStatisticsConfig(long groupId, String data) throws Exception
	{
		// Valida los parámetros de entrada
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(data, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		// Guarda la configuración
		GroupConfigTools.setGroupConfigField(groupId, "visitstatistics", data);
	}
	
	/**
	 * Notifica a MAS que hay cambios en vocabularios y debe realizar una petición
	 * de actualización de la jerarquía de categorías.
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws ServiceError 
	 */
	public void notifyVocabulariesModificationsToMAS(long groupId) throws PortalException, SystemException, ServiceError
	{
//		// Valida los parámetros de entrada
//		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
//		
//		// Recupera los vocabularios
//		List<AssetVocabulary> vocabularies = AssetVocabularyLocalServiceUtil.getGroupVocabularies(GroupMgr.getGlobalGroupId());
//		
//		if(vocabularies.size() > 0)
//		{
//			StringBuilder sb = new StringBuilder();
//			// Concatena el ID del grupo y lo separa con dos puntos.
//			sb.append(groupId).append(StringPool.COLON);
//			// Concatena los IDs de los vocabularios separados por comas.
//			for (AssetVocabulary vocabulary : vocabularies)
//			{
//				sb.append(vocabulary.getVocabularyId()).append(StringPool.COMMA);
//			}
//			// Elimina la última coma.
//			sb.setLength(sb.length() - 1);
//			
//			// Ejecuta el proceso de regeneración de caches y notificación de cambios.
//			MASUtil masUtil = new MASUtil(sb.toString(), false);
//			masUtil.start();
//		}
	}
	
	//////////////////////////////////////////////////
	//          ANOTACIONES AUTOMÁTICAS             //
	//////////////////////////////////////////////////
	public void addArticleStatisticsAnnotation(Long groupId, String idart, String note)
	{
		VisitsStatisticsUtil.setSystemAnnotation(groupId, idart, note);
	}
}