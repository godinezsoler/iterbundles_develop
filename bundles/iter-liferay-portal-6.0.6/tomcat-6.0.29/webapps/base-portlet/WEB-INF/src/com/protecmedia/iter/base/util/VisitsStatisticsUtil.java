package com.protecmedia.iter.base.util;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.util.survey.IterSurveyUtil;
import com.protecmedia.iter.base.model.VisitsStatisticsRequest;
import com.protecmedia.iter.base.model.VisitsStatisticsRequest.ABTesting;
import com.protecmedia.iter.base.model.VisitsStatisticsRequest.ABTestingVariant;
import com.protecmedia.iter.base.model.VisitsStatisticsRequest.Resolution;

public class VisitsStatisticsUtil
{
	private static Log _log = LogFactoryUtil.getLog(VisitsStatisticsUtil.class);
	
	public static Document getTotalArticleVisits(long groupId, String[] articleIds) throws SecurityException, NoSuchMethodException, ServiceError
	{
		ErrorRaiser.throwIfFalse( Validator.isNotNull(articleIds), IterErrorKeys.XYZ_E_INVALIDARG_ZYX );

		String sql = String.format(VisitsStatisticsQueryBuilder.GET_TOTAL_ARTICLEVISITS_BY_GROUP, 
						StringUtils.join(articleIds, "','"), groupId);
		
		_log.debug(sql);
		
		long startTime = _log.isDebugEnabled() ? Calendar.getInstance().getTimeInMillis() : 0;
		Document result = PortalLocalServiceUtil.executeQueryAsDom(sql);
		if (_log.isDebugEnabled()) _log.debug("SQL Time article visits: " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");

		return result;
	}
	
	public static JSONObject getABTestingInfo(VisitsStatisticsRequest request) throws ServiceError, SecurityException, NoSuchMethodException
	{
		JSONObject jsonResult = null;
		ABTesting abTesting   = request.getABTesting(); 

		// Los ABTesting solo tienen resolución de horas o días
		if (abTesting.hasExperiment && 
			(request.getResolution().equals(Resolution.MINUTE) || request.getResolution().equals(Resolution.HOUR) || request.getResolution().equals(Resolution.DAY)))
		{
			// Tiene experimento por lo que se crea el objeto
			jsonResult = JSONFactoryUtil.createJSONObject();
			
			// Fecha de inicio y fin del experimento
			Calendar calc = abTesting.getStartDate();
			if (calc != null)
			{
				// El experimento ha comenzado
				jsonResult.put("startdate", calc.getTimeInMillis());
				
				calc = abTesting.getFinishDate();
				if (calc != null)
				{
					// EL experimento ha concluido
					jsonResult.put("finishdate", calc.getTimeInMillis());
					
					// Si ha concluido es posible que haya ganador
					if (abTesting.getWinner() > 0)
						jsonResult.put("winner", abTesting.getVariants().get(abTesting.getWinner()).name);
				}
			}
		}
		return jsonResult;
	}
	
	public static JSONObject getABTestingStatistics(VisitsStatisticsRequest request) throws ServiceError, SecurityException, NoSuchMethodException
	{
		JSONObject jsonResult = null;
		ABTesting abTesting   = request.getABTesting(); 

		// Los ABTesting solo tienen resolución de horas o días
		if (abTesting.hasExperiment)
		{
			// Valida los parámetros de entrada
			ErrorRaiser.throwIfFalse(request.getGroupId() > 0, 	IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			ErrorRaiser.throwIfNull(request.getStartDate(), 	IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			ErrorRaiser.throwIfNull(request.getEndDate(), 		IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			ErrorRaiser.throwIfNull(request.getArticleId(), 	IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			// Recupera las estadísticas del ABTesting
			String sql 		= VisitsStatisticsQueryBuilder.buildQuery(VisitsStatisticsRequest.StatisticItem.ABTESTING, request);
			long startTime 	= _log.isDebugEnabled() ? Calendar.getInstance().getTimeInMillis() : 0;
			
			Document dom = PortalLocalServiceUtil.executeQueryAsDom(sql);
			if (_log.isDebugEnabled()) _log.debug("SQL Time ABTesting: " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
			
			jsonResult = buildABTestingStatistics(dom, request);
			if (_log.isDebugEnabled()) _log.debug("JAVA Time ABTesting: " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
		}
		
		return jsonResult;
	}
	
	/**
	 * Modela el JSON que espera la interfaz a partir de los datos de la BBDD y la parametrización de fechas/hora
	 * @param dom
	 * @param request
	 * @return
	 * @throws ServiceError 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 */
	public static JSONObject buildABTestingStatistics(Document dom, VisitsStatisticsRequest request) throws ServiceError, SecurityException, NoSuchMethodException
	{
		JSONObject jsonResult = getABTestingInfo(request);
		
		ABTesting abTesting  = request.getABTesting(); 
		
		Calendar fromDate 	= (Calendar) request.getStartDate().clone();
		Calendar toDate 	= (Calendar) request.getEndDate().clone();
		
		DateFormat sqlDF    = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		int interval 		= Calendar.DAY_OF_MONTH;
		
		switch(request.getResolution())
		{
		case MINUTE:
			interval = Calendar.MINUTE;
			break;
		case HOUR:
			interval = Calendar.HOUR_OF_DAY;
			break;
		default:
			// DAY
			interval = Calendar.DAY_OF_MONTH;
		}
		
		// Variantes del experimento
		JSONArray jsonVariantsArray = JSONFactoryUtil.createJSONArray();
		for (Map.Entry<Long, ABTestingVariant> entry : abTesting.getVariants().entrySet())
		{
			ABTestingVariant variant = entry.getValue();
			
			JSONObject jsonVariant = JSONFactoryUtil.createJSONObject();
			jsonVariant.put("variantid", 	variant.id);
			jsonVariant.put("variantname", 	variant.name);
			jsonVariant.put("ctr", 			variant.ctr);
			jsonVariant.put("totalvisits", 	variant.visits);
			jsonVariant.put("totalprints", 	variant.prints);
			jsonVariant.put("prints", 	 	JSONFactoryUtil.createJSONArray());
			jsonVariant.put("visits", 	 	JSONFactoryUtil.createJSONArray());
			jsonVariant.put("extvisits", 	JSONFactoryUtil.createJSONArray());
			
			jsonVariantsArray.put(jsonVariant);
		}
		jsonResult.put("variants", jsonVariantsArray);


		while (!fromDate.after(toDate))
		{
			// Se busca para cada variante el valor de impresión, visitas y ctr con esa fecha
			for (int i = 0; i < jsonVariantsArray.length(); i++)
			{
				JSONObject jsonVariant = jsonVariantsArray.getJSONObject(i);
				
				long variantid = jsonVariant.getLong("variantid");
//				ABTestingVariant variant = abTesting.getVariants().get(variantid);
				
				String xpath = String.format("/rs/row[@statisticsDate='%s' and @variantid='%d']", 
								sqlDF.format(fromDate.getTime()), variantid);
				
				Element statistic = (Element)dom.selectSingleNode(xpath);
				
				long prints 	= 0;
				long visits 	= 0;
				long extvisits 	= 0;
				
				if (statistic != null)
				{
					prints 		= XMLHelper.getLongValueOf(statistic, "@prints");
					visits 		= XMLHelper.getLongValueOf(statistic, "@visits");
					extvisits 	= XMLHelper.getLongValueOf(statistic, "@extvisits");
				}
				
				jsonVariant.getJSONArray("prints").put(prints);
				jsonVariant.getJSONArray("visits").put(visits);
				jsonVariant.getJSONArray("extvisits").put(extvisits);
				
//				variant.addPrints(prints);
//				variant.addVisits(visits-extvisits);
			}
			
			// Se rellenan todos los posibles valores en el array de horas que se devolverá
			fromDate.add(interval, 1);
		}
		
//		// Se rellena el ctr de cada experimento
//		for (int i = 0; i < jsonVariantsArray.length(); i++)
//		{
//			JSONObject jsonVariant = jsonVariantsArray.getJSONObject(i);
//			
//			long variantid = jsonVariant.getLong("variantid");
//			ABTestingVariant variant = abTesting.getVariants().get(variantid);
//			
//			jsonVariant.put("ctr", 	variant.getCtr());
//		}
		
		return jsonResult;
	}
	
	/**
	 * <p>Recupera las visitas y lectura de Artículos.</p>
	 * <p>Si no se informa articleId, suma las visitas y lecturas de todos los artículos. Se llama de esta forma desde las estadísticas de sitio.</p>
	 */
	public static List<Object[]> getArticleVisits(VisitsStatisticsRequest request) throws ServiceError
	{
		// Valida los parámetros de entrada
		ErrorRaiser.throwIfFalse(request.getGroupId() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(request.getStartDate(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(request.getEndDate(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Recupera las estadísticas de visitas
		String sql = VisitsStatisticsQueryBuilder.buildQuery(VisitsStatisticsRequest.StatisticItem.ARTICLE, request);
		long startTime = _log.isDebugEnabled() ? Calendar.getInstance().getTimeInMillis() : 0;
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList(sql);
		if (_log.isDebugEnabled()) _log.debug("SQL Time article visits: " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
		return buildVisitStatisticsList(result, request);
	}
	
	public static List<Object[]> getSectionVisits(VisitsStatisticsRequest request) throws ServiceError
	{
		// Valida los parámetros de entrada
		ErrorRaiser.throwIfFalse(request.getGroupId() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(request.getStartDate(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(request.getEndDate(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Recupera las estadísticas de visitas
		String sql = VisitsStatisticsQueryBuilder.buildQuery(VisitsStatisticsRequest.StatisticItem.SECTION, request);
		long startTime = _log.isDebugEnabled() ? Calendar.getInstance().getTimeInMillis() : 0;
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList(sql);
		if (_log.isDebugEnabled()) _log.debug("SQL Time section visits: " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
		return buildVisitStatisticsList(result, request);
	}
	
	public static List<Object[]> getCategoryVisits(VisitsStatisticsRequest request) throws ServiceError
	{
		// Valida los parámetros de entrada
		ErrorRaiser.throwIfFalse(request.getGroupId() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse(request.getCategoryId() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(request.getStartDate(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(request.getEndDate(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Recupera las estadísticas de visitas
		String sql = VisitsStatisticsQueryBuilder.buildQuery(VisitsStatisticsRequest.StatisticItem.METADATA, request);
		long startTime = _log.isDebugEnabled() ? Calendar.getInstance().getTimeInMillis() : 0;
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList(sql);
		if (_log.isDebugEnabled()) _log.debug("SQL Time category visits: " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
		return buildVisitStatisticsList(result, request);
	}
	
	public static List<Object[]> getSurveyVotes(VisitsStatisticsRequest request) throws ServiceError
	{
		String groupByFormat = null;
		switch (request.getResolution()) 
		{
		case MINUTE:
			groupByFormat = "%Y-%m-%d %H:%i";
			break;
		case HOUR:
			groupByFormat = "%Y-%m-%d %H";
			break;
		case DAY:
			groupByFormat = "%Y-%m-%d";
			break;
		case MONTH:
			groupByFormat = "%Y-%m";
			break;
		default:
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}
		
		List<Object> result = IterSurveyUtil.getVoteTrend(request.getSurveyId(), request.getSqlStartDate(), request.getSqlEndDate(), groupByFormat);
		return buildVisitStatisticsList(result, request);
	}
	
	private static List<Object[]> buildVisitStatisticsList(List<Object> rawStats, VisitsStatisticsRequest request) throws ServiceError
	{
		// Valida los parámetros de entrada
		ErrorRaiser.throwIfFalse(request.getStartDate().before(request.getEndDate()), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		if (Validator.isNull(rawStats))
			rawStats = new ArrayList<Object>();
		
		List<Object[]> visitsList = new ArrayList<Object[]>();
		
		DateFormat df = null;
		DateFormat dfHour = null;
		int interval = 0;
		
		switch (request.getResolution()) 
		{
		case MINUTE:
			df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			dfHour = new SimpleDateFormat("HH:mm");
			interval = Calendar.MINUTE;
			break;
		case HOUR:
			df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			dfHour = new SimpleDateFormat("HH");
			interval = Calendar.HOUR_OF_DAY;
			break;
		case DAY:
			df = new SimpleDateFormat("yyyy-MM-dd");
			dfHour = new SimpleDateFormat("yyyy-MM-dd");
			interval = Calendar.DAY_OF_MONTH;
			break;
		case MONTH:
			df = new SimpleDateFormat("yyyy-MM");
			dfHour = new SimpleDateFormat("yyyy-MM");
			interval = Calendar.MONTH;
			break;
		default:
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}
		
		
		// Construye el listado de estadísticas
		Calendar fromDate = (Calendar) request.getStartDate().clone();
		Calendar toDate = (Calendar) request.getEndDate().clone();
		
		for (int i = 0; i < rawStats.size(); i++)
		{
			Object[] stat = ((Object[]) rawStats.get(i));
			Calendar statisticsDate = Calendar.getInstance();
			try
			{
				statisticsDate.setTime(df.parse(stat[0].toString()));
			}
			catch (ParseException e)
			{
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
			}
			
			if (!toDate.before(statisticsDate))
			{
				long visits = Long.valueOf(stat[1].toString());
				long reads = stat.length < 3 ? 0 : Long.valueOf(stat[2].toString());
				
				// Si faltan horas, las rellena a 0
				while (fromDate.before(statisticsDate))
				{
					visitsList.add(new Object[] {dfHour.format(fromDate.getTime()), 0, 0});
					fromDate.add(interval, 1);
				}
				
				// Rellena las estadísticas de visitas
				visitsList.add(new Object[] {dfHour.format(fromDate.getTime()), visits, reads});
				fromDate.add(interval, 1);
			}
		}
		
		// Si faltan horas, las rellena a 0
		while (!fromDate.after(toDate))
		{
			visitsList.add(new Object[] {dfHour.format(fromDate.getTime()), 0, 0});
			fromDate.add(interval, 1);
		}
		
		return visitsList;
	}
	
	public static Document getArticleRanking(VisitsStatisticsRequest request) throws ServiceError, SecurityException, NoSuchMethodException
	{
		// Valida los parámetros de entrada
		ErrorRaiser.throwIfFalse(request.getGroupId() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse(request.getPlid() >= 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse(request.getMaxItems() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(request.getStartDate(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(request.getEndDate(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Recupera los artículos
		String sql = VisitsStatisticsQueryBuilder.buildTopArticleQuery(request);
		long startTime = _log.isDebugEnabled() ? Calendar.getInstance().getTimeInMillis() : 0;
		Document result = PortalLocalServiceUtil.executeQueryAsDom(sql);
		if (_log.isDebugEnabled()) _log.debug("SQL Time top article: " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
		return result;
	}
	
	public static Document getSectionRanking(VisitsStatisticsRequest request) throws ServiceError, SecurityException, NoSuchMethodException
	{
		// Valida los parámetros de entrada
		ErrorRaiser.throwIfFalse(request.getGroupId() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse(request.getMaxItems() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(request.getStartDate(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(request.getEndDate(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Recupera las secciones
		String sql = VisitsStatisticsQueryBuilder.buildTopSectionsQuery(request);
		long startTime = _log.isDebugEnabled() ? Calendar.getInstance().getTimeInMillis() : 0;
		Document result = PortalLocalServiceUtil.executeQueryAsDom(sql);
		if (_log.isDebugEnabled()) _log.debug("SQL Time top sections: " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
		return result;
	}
	
	public static Document getCategoriesRanking(VisitsStatisticsRequest request) throws ServiceError, SecurityException, NoSuchMethodException
	{
		// Valida los parámetros de entrada
		ErrorRaiser.throwIfFalse(request.getGroupId() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse(request.getVocabularyId() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse(request.getMaxItems() >= 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(request.getStartDate(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(request.getEndDate(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Recupera los metadatos más vistos
		String sql = VisitsStatisticsQueryBuilder.buildTopCategoriesQuery(request, request.getVocabularyId(), request.getMaxItems());
		long startTime = _log.isDebugEnabled() ? Calendar.getInstance().getTimeInMillis() : 0;
		Document result = PortalLocalServiceUtil.executeQueryAsDom(sql);
		if (_log.isDebugEnabled()) _log.debug("SQL Time top categories: " + (Calendar.getInstance().getTimeInMillis() - startTime) + "ms");
		return result;
	}

	private static final String SQL_UNION_ALL = " UNION ALL \n";
	
	private static final String SQL_TABLE_GLOBAL_ANNOTATIONS  = "visits_statistics_annotation";
	private static final String SQL_TABLE_ARTICLE_ANNOTATIONS = "visits_statistics_article_annotation";
	
	private static final String SQL_RETRIEVE_ANNOTATIONS = new StringBuilder()
	.append(" SELECT DATE_FORMAT(MIN(annotationDate), '%%%%Y-%%%%m-%%%%d %%%%H:%%%%i:%%%%s') minDate, \n")
	.append("        DATE_FORMAT(MAX(annotationDate), '%%%%Y-%%%%m-%%%%d %%%%H:%%%%i:%%%%s') maxDate, \n")
	.append("        DATE_FORMAT(annotationDate, '%%1$s') label                                       \n")
	.append(" FROM %s                                                                                 \n")
	.append(" WHERE groupId = %%2$d AND annotationDate BETWEEN '%%3$s' AND '%%4$s' %s                 \n")
	.append(" GROUP BY label                                                                            ")
	.toString();
	
	private static final String SQL_RETRIEVE_GLOBAL_ANNOTATIONS  = new StringBuilder().append(String.format(SQL_RETRIEVE_ANNOTATIONS, SQL_TABLE_GLOBAL_ANNOTATIONS, StringPool.BLANK)).toString(); 
	private static final String SQL_RETRIEVE_ARTICLE_ANNOTATIONS = new StringBuilder().append(String.format(SQL_RETRIEVE_ANNOTATIONS, SQL_TABLE_ARTICLE_ANNOTATIONS, "AND articleId='%5$s' AND system=0")).toString();
	private static final String SQL_RETRIEVE_SYSTEM_ANNOTATIONS = new StringBuilder()
		.append(" SELECT DATE_FORMAT(annotationDate, '%s') label,                                  \n")
		.append("        CONCAT('[', DATE_FORMAT(annotationDate, '%%H:%%i:%%s'), '] ',  note) note \n")
		.append(" FROM visits_statistics_article_annotation                                        \n")
		.append(" WHERE groupId = %d AND annotationDate BETWEEN '%s' AND '%s'                      \n")
		.append(" AND articleId='%s' AND system=1"                                                    )
		.toString();
	private static final String SQL_RETRIEVE_SYSTEM_ANNOTATIONS_VALIDITY = new StringBuilder()
		.append(" SELECT DATE_FORMAT(n.vigenciahasta, '%s') label,                                                \n")
		.append("         CONCAT('[', DATE_FORMAT(n.vigenciahasta, '%%H:%%i:%%s'), '] %%s ',  l.friendlyURL) note \n")
		.append(" FROM news_pagecontent n                                                                         \n")
		.append(" INNER JOIN layout l ON l.uuid_ = n.layoutId                                                     \n")
		.append(" WHERE n.contentid = '%s'                                                                        \n")
		.append("   AND n.vigenciahasta BETWEEN '%s' AND '%s' "                                                      )
		.toString();
	
	public static Document getSystemAnnotations(VisitsStatisticsRequest request) throws ServiceError, SecurityException, NoSuchMethodException
	{
		// Valida los parámetros de entrada
		ErrorRaiser.throwIfFalse(request.getGroupId() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(request.getSqlStartDate(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(request.getSqlEndDate(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(request.getArticleId(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Crea el formato de la etiqueta por la que buscar la posición en el gráfico
		String dateFormat = null;
		switch (request.getResolution()) {
			case MINUTE:
			case HOUR:  dateFormat = "%H:%i";    break;
			case DAY:   dateFormat = "%Y-%m-%d"; break;
			case MONTH: dateFormat = "%Y-%m";    break;
			default: ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}
		
		// Notas automáticas
		Document resultSystemNotes = PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_RETRIEVE_SYSTEM_ANNOTATIONS, dateFormat, request.getGroupId(), request.getSqlStartDate(), request.getSqlEndDate(), request.getArticleId()));
		
		// Salida de vigencia del artículo
		Document resultValidity = PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_RETRIEVE_SYSTEM_ANNOTATIONS_VALIDITY, dateFormat, request.getArticleId(), request.getSqlStartDate(), request.getSqlEndDate()));
		List<Node> validityNotes = resultValidity.selectNodes("/rs/row");
		if (null != validityNotes && validityNotes.size() > 0)
		{
			String validityLiteral = GetterUtil.getString(GroupConfigTools.getGroupConfigXMLField(request.getGroupId(), "visitstatistics", "/visits/annotations/validity/text()"), "Article out of validity period in section");
			for (Node validityNote : validityNotes)
			{
				// Añade al texto el literal configurado
				((Element) validityNote).addAttribute("note", String.format(((Element) validityNote).attributeValue("note"), validityLiteral));
				
				// Añade la nota a las notas automáticas
				resultSystemNotes.getRootElement().add(validityNote.detach());
			}
		}
		
		return resultSystemNotes;
	}
	
	public static Document getAnnotations(VisitsStatisticsRequest request) throws ServiceError, SecurityException, NoSuchMethodException
	{
		// Valida los parámetros de entrada
		ErrorRaiser.throwIfFalse(request.getGroupId() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(request.getSqlStartDate(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(request.getSqlEndDate(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Crea el formato de la etiqueta por la que agrupar
		String dateFormat = null;
		switch (request.getResolution()) 
		{
			case MINUTE:  	
			case HOUR:
							dateFormat = "%H:%ih"; 		break;
			case DAY:   	dateFormat = "%Y-%m-%d"; 	break;
			case MONTH: 	dateFormat = "%Y-%m";    	break;
			default: ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}
		
		// Comprueba si deben consultarse anotaciones globales, de artículo o ambas
		String sql = null;
		switch (request.getAnnotations()) {
		case GLOBAL:
			sql = new StringBuilder().append(SQL_RETRIEVE_GLOBAL_ANNOTATIONS).toString();
			break;
		case ARTICLE:
			sql = new StringBuilder().append(SQL_RETRIEVE_ARTICLE_ANNOTATIONS).toString();
			break;
		
		default: // ALL
			sql = new StringBuilder().append(SQL_RETRIEVE_GLOBAL_ANNOTATIONS).append(SQL_UNION_ALL).append(SQL_RETRIEVE_ARTICLE_ANNOTATIONS).toString();
		}
		
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(sql, dateFormat, request.getGroupId(), request.getSqlStartDate(), request.getSqlEndDate(), request.getArticleId()));
	}

	private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH");
	private static final DateFormat dfSql = new SimpleDateFormat("yyyy-MM-dd HH:00:00");
	
	private static final String SQL_SET_ANNOTATION = new StringBuilder()
	.append(" INSERT INTO visits_statistics_annotation (groupId, annotationDate, note, modifiedDate) \n")
	.append(" VALUES (%d, '%s', '%s', NOW() )                                                        \n")
	.append(" ON DUPLICATE KEY UPDATE note=VALUES(note), modifiedDate=NOW()                            ")
	.toString();
	
	private static final String SQL_SET_ARTICLE_ANNOTATION = new StringBuilder()
	.append(" INSERT INTO visits_statistics_article_annotation (groupId, annotationDate, articleId, system, note, modifiedDate) \n")
	.append(" VALUES (%d, %s, '%s', %d, '%s', NOW())                                                                            \n")
	.append(" ON DUPLICATE KEY UPDATE note=IF(system=0, VALUES(note), CONCAT(note, '\n', VALUES(note))), modifiedDate=NOW()       ")
	.toString();
	
	public static void setSystemAnnotation(Long groupId, String idart, String note)
	{
		try
		{
			setStatisticsAnnotation(groupId, null, idart, note, true);
		}
		catch (Throwable th)
		{
			_log.debug("Unable to add statistics system annotation for article " + idart);
			_log.error(th);
		}
	}
	
	public static void setAnnotation(Long groupId, String date, String idart, String note) throws ServiceError, IOException, SQLException
	{
		setStatisticsAnnotation(groupId, date, idart, note, false);
	}
	
	public static void setStatisticsAnnotation(Long groupId, String date, String idart, String note, boolean system) throws ServiceError, IOException, SQLException
	{
		// Valida los parámetros de entrada
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		if (system)
		{
			date = "NOW()";
		}
		else
		{
			ErrorRaiser.throwIfNull(date, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			try { date = dfSql.format(df.parse(date)); } catch (ParseException e) { ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX); }
			date = StringUtil.apostrophe(date);
		}
		ErrorRaiser.throwIfNull(note, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		note = note.replaceAll(StringPool.APOSTROPHE, StringPool.DOUBLE_APOSTROPHE);
		
		// Crea la query de inserción / actualización
		String sql = null;
		if (Validator.isNull(idart))
			sql = String.format(SQL_SET_ANNOTATION, groupId, date, note);
		else
			sql = String.format(SQL_SET_ARTICLE_ANNOTATION, groupId, date, idart, system ? 1 : 0, note);

		// Inserta / Actualiza la nota
		PortalLocalServiceUtil.executeUpdateQuery(sql);
	}
	
	private static final String SQL_DELETE_ANNOTATION = new StringBuilder()
	.append(" DELETE FROM visits_statistics_annotation     \n")
	.append(" WHERE groupId = %d AND annotationDate = '%s'   ")
	.toString();
	
	private static final String SQL_DELETE_ARTICLE_ANNOTATION = new StringBuilder()
	.append(" DELETE FROM visits_statistics_article_annotation                               \n")
	.append(" WHERE groupId = %d AND annotationDate = '%s' AND articleId = '%s' AND system=0   ")
	.toString();
	
	public static void removeAnnotation(Long groupId, String date, String idart) throws ServiceError, IOException, SQLException
	{
		// Valida los parámetros de entrada
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(date, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		try { date = dfSql.format(df.parse(date)); } catch (ParseException e) { ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX); }
		
		// Crea la query de inserción / actualización
		String sql = null;
		if (Validator.isNull(idart))
			sql = String.format(SQL_DELETE_ANNOTATION, groupId, date);
		else
			sql = String.format(SQL_DELETE_ARTICLE_ANNOTATION, groupId, date, idart);
		
		// Inserta / Actualiza la nota
		PortalLocalServiceUtil.executeUpdateQuery(sql);
	}

	private static final String SQL_GET_ANNOTATIONS = new StringBuilder()
	.append(" SELECT groupid, DATE_FORMAT(annotationDate, '%%Y-%%m-%%d %%H:%%i:%%s') annotationdate,       \n")
	.append("        '' articleid, note, DATE_FORMAT(modifiedDate, '%%Y-%%m-%%d %%H:%%i:%%s') modifieddate \n")
	.append(" FROM visits_statistics_annotation                                                            \n")
	.append(" WHERE groupId = %d AND annotationDate BETWEEN '%s' AND '%s'                                    ")
	.toString();
	
	private static final String SQL_GET_ARTICLE_ANNOTATIONS = new StringBuilder()
	.append(" SELECT groupid, DATE_FORMAT(annotationDate, '%%Y-%%m-%%d %%H:%%i:%%s') annotationdate,        \n")
	.append("        articleid, note, DATE_FORMAT(modifiedDate, '%%Y-%%m-%%d %%H:%%i:%%s') modifieddate     \n")
	.append(" FROM visits_statistics_article_annotation                                                     \n")
	.append(" WHERE groupId = %d AND annotationDate BETWEEN '%s' AND '%s' AND articleId = '%s' AND system=0   ")
	.toString();
	
	public static String getAnnotations(Long groupId, String startDate, String endDate, String idart, String mode) throws ServiceError, SecurityException, NoSuchMethodException
	{
		// Valida los parámetros de entrada
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(startDate, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		try { startDate = dfSql.format(df.parse(startDate)); } catch (ParseException e) { ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX); }
		if (Validator.isNull(endDate))
			endDate = startDate;
		else
			try { endDate = dfSql.format(df.parse(endDate)); } catch (ParseException e) { ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX); }
		VisitsStatisticsRequest.Annotation retrieveMode = null;
		try { retrieveMode = VisitsStatisticsRequest.Annotation.valueOf(mode.toUpperCase()); } catch (Throwable e) { }
		ErrorRaiser.throwIfNull(retrieveMode, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		boolean needArticleId = VisitsStatisticsRequest.Annotation.ARTICLE.equals(retrieveMode) || VisitsStatisticsRequest.Annotation.ALL.equals(retrieveMode);
		ErrorRaiser.throwIfFalse(!(needArticleId && Validator.isNull(idart)), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String sql = null;
		switch (retrieveMode) {
		case GLOBAL:
			sql = String.format(SQL_GET_ANNOTATIONS, groupId, startDate, endDate);
			break;
		case ARTICLE:
			sql = String.format(SQL_GET_ARTICLE_ANNOTATIONS, groupId, startDate, endDate, idart);
			break;
		default: // ALL
			sql = new StringBuilder()
			.append(String.format(SQL_GET_ANNOTATIONS, groupId, startDate, endDate))
			.append(SQL_UNION_ALL)
			.append(String.format(SQL_GET_ARTICLE_ANNOTATIONS, groupId, startDate, endDate, idart))
			.toString();
			break;
		}
			
		
		return PortalLocalServiceUtil.executeQueryAsDom(sql, true, "annotations", "annotation", new String[]{"note"}).asXML();
	}
}
