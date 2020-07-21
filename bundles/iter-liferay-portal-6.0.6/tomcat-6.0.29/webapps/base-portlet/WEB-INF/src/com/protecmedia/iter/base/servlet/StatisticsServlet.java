package com.protecmedia.iter.base.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.IterGlobalKeys;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portlet.asset.model.AssetVocabulary;
import com.liferay.portlet.asset.service.AssetVocabularyLocalServiceUtil;
import com.liferay.util.survey.IterSurveyUtil;
import com.protecmedia.iter.base.metrics.NewslettersMetricsUtil;
import com.protecmedia.iter.base.metrics.UserMetricsUtil;
import com.protecmedia.iter.base.model.VisitsStatisticsRequest;
import com.protecmedia.iter.base.service.FeedbackLocalServiceUtil;
import com.protecmedia.iter.base.service.StatisticMgrLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.util.VisitsStatisticsUtil;
import com.protecmedia.iter.news.service.CountersLocalServiceUtil;

public class StatisticsServlet extends HttpServlet
{
	private static final long serialVersionUID = 5154792529342466860L;
	private static Log _log = LogFactoryUtil.getLog(StatisticsServlet.class);
	DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		IterRequest.setOriginalRequest(request);
		
		try
		{
			VisitsStatisticsRequest statisticsRequest = new VisitsStatisticsRequest(request);
			JSONObject jsonResult = JSONFactoryUtil.createJSONObject();
			
			// Tipo de gráfico: Trend
			if ("trend".equals(statisticsRequest.getChartType()))
			{
				List<Object[]> 	counters 		= null;
				List<Object[]> 	sectionCounters = null;
				JSONObject 		jsonABTesting 	= null;
				
				boolean hasReads = true;
				switch (statisticsRequest.getPageType())
				{
				case GROUP:
				case SECTION:
					// Recupera las estadísticas de visitas de artículos
					counters = VisitsStatisticsUtil.getArticleVisits(statisticsRequest);
					// Recupera las estadísticas de visitas de secciones
					sectionCounters = VisitsStatisticsUtil.getSectionVisits(statisticsRequest);
					break;
				case ARTICLE:
					counters = VisitsStatisticsUtil.getArticleVisits(statisticsRequest);
					jsonABTesting = VisitsStatisticsUtil.getABTestingStatistics(statisticsRequest);
					break;
					
				case METADATA:
					// Recupera las estadísticas de visitas al vocabulario
					counters = VisitsStatisticsUtil.getCategoryVisits(statisticsRequest);
					hasReads = false;
					break;
					
				case SURVEY:
					// Recupera las estadísticas de votaciones de la encuesta
					counters = VisitsStatisticsUtil.getSurveyVotes(statisticsRequest);
					hasReads = false;
					break;
					
				default:
				}
				
				// Añade los contadores a la respuesta
				getVisitsStatistics(jsonResult, counters, sectionCounters, statisticsRequest, hasReads, jsonABTesting);
			}
			
			// Tipo de gráfico: Ranking
			else if ("ranking".equals(statisticsRequest.getChartType()))
			{
				String chartTitle = null;
				Document rankingResults = null;
				
				switch (statisticsRequest.getItem())
				{
				case SECTION:
					chartTitle = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(statisticsRequest.getGroupId(), "visitstatistics", "/visits/texts/topsections/text()"), "Secciones más vistas");
					rankingResults = VisitsStatisticsUtil.getSectionRanking(statisticsRequest);
					break;
					
				case ARTICLE:
					if ("visits".equals(statisticsRequest.getCriteria()))
						chartTitle = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(statisticsRequest.getGroupId(), "visitstatistics", "/visits/texts/articlesviewed/text()"), "Artículos más vistos");
					else if ("readings".equals(statisticsRequest.getCriteria()))
						chartTitle = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(statisticsRequest.getGroupId(), "visitstatistics", "/visits/texts/articlesread/text()"), "Artículos más leídos");
					else if ("sharings".equals(statisticsRequest.getCriteria()))
						chartTitle = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(statisticsRequest.getGroupId(), "visitstatistics", "/visits/texts/articlesshared/text()"), "Artículos más compartidos");
					else if ("comments".equals(statisticsRequest.getCriteria()))
						chartTitle = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(statisticsRequest.getGroupId(), "visitstatistics", "/visits/texts/articlescomments/text()"), "Artículos más comentados");
					else if ("feedback".equals(statisticsRequest.getCriteria()))
						chartTitle = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(statisticsRequest.getGroupId(), "visitstatistics", "/visits/texts/articlesvalued/text()"), "Artículos mejor valorados");
					
					rankingResults = VisitsStatisticsUtil.getArticleRanking(statisticsRequest);
					break;
					
				case METADATA:
					AssetVocabulary vocabulary = AssetVocabularyLocalServiceUtil.getAssetVocabulary(statisticsRequest.getVocabularyId());
					rankingResults = VisitsStatisticsUtil.getCategoriesRanking(statisticsRequest);
					String vocabularyName = vocabulary.getName();
					chartTitle = vocabularyName.substring(vocabularyName.indexOf(IterGlobalKeys.DLG_SEPARATOR) + 1);
					break;
					
				default:
				}
	
				// Crea el JSON de respuesta
				jsonResult = buildRankingJSON(rankingResults, chartTitle, statisticsRequest.getCriteria());
			}
			else
			{
				// Inyecta la configuración usada en la petición para informar a MLN
				JSONObject jsonConfigObject = JSONFactoryUtil.createJSONObject();
				jsonConfigObject.put("resolution", statisticsRequest.getResolution().name().toLowerCase());
				jsonConfigObject.put("date", statisticsRequest.getDateLimit());
				jsonConfigObject.put("displayedHours", statisticsRequest.getDisplayedHours());
				jsonConfigObject.put("realTime", statisticsRequest.isRealTimeDay());
				jsonResult.put("config", jsonConfigObject);
				
				// Añade la hora del servidor
				jsonResult.put("serverTime", df.format(new Date()));
				// Añade la hora inicial de las estadísticas
				jsonResult.put("statisticsStartDate", df.format(statisticsRequest.getStartDate().getTime()));
				// Añade la hora final de las estadísticas
				jsonResult.put("statisticsEndDate", df.format(statisticsRequest.getEndDate().getTime()));
				// Añade los textos
				String emptyVisitsMessage = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(statisticsRequest.getGroupId(), "visitstatistics", "/visits/texts/emptyVisitsMessage/text()"), "No se dispone de información de visitas.");
				jsonResult.put("emptyVisitsMessage", emptyVisitsMessage);
				

				if (statisticsRequest.isArticleRequest())
				{
					jsonResult.put("abtesting", VisitsStatisticsUtil.getABTestingInfo(statisticsRequest));
					
					// Recupera las estadisticas de redes sociales
					List<Object> socialStats = StatisticMgrLocalServiceUtil.getArticleSocialStatistics(statisticsRequest.getGroupId(), statisticsRequest.getArticleId());
					getSocialStatistics(jsonResult, socialStats);
					
					// Recupera las estadísticas del portlet de valoración
					getFeedbackStatistics(jsonResult, statisticsRequest);
				}
				else if (statisticsRequest.isUserRequest())
				{
					jsonResult.put("users", UserMetricsUtil.getMetrics(statisticsRequest));
				}
				else if (statisticsRequest.isNewsletterRequest())
				{
					jsonResult.put("newsletters", NewslettersMetricsUtil.getMetrics(statisticsRequest));
				}
			}
			
			// Tipo de gráfico: Percent
			
			// Construye la respuesta del servlet
			buildResponse(response, HttpServletResponse.SC_OK, jsonResult);
		}
		catch (Throwable th)
		{
			_log.debug(th.toString());
			_log.error(th);
			buildResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
		}
	}
	
	/**
	 * Redirigido a {@link #doGet(HttpServletRequest, HttpServletResponse)}
	 */
	protected void doPost (HttpServletRequest request, HttpServletResponse response)
	{
		this.doGet(request, response);
	}
	
	private void buildResponse(HttpServletResponse response, int status, JSONObject result)
	{
		response.setStatus(status);
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setCharacterEncoding("UTF-8");
		
		PrintWriter out = null;
		try
		{
			out = response.getWriter();
			if (null == result)
			{
				response.setContentLength(1);
				out.print(StringPool.PERIOD);
			}
			else
			{
				response.setContentType("application/json;");
				out.print(result.toString());
			}
			out.flush();
		}
		catch (IOException e)
		{
		}
		finally
		{
			if (null != out)
				out.close();
		}
	}
	
	private JSONObject buildRankingJSON(Document rawData, String chartTitle, String criteria)
	{
		JSONArray jsonIds      = JSONFactoryUtil.createJSONArray();
		JSONArray jsonTitles   = JSONFactoryUtil.createJSONArray();
		JSONArray jsonDataset1 = JSONFactoryUtil.createJSONArray();
		JSONArray jsonDataset2 = JSONFactoryUtil.createJSONArray();
		
		List<Node> results = rawData.selectNodes("/rs/row");
		for (Node result : results)
		{
			jsonIds.put(XMLHelper.getTextValueOf(result, "@id"));
			jsonTitles.put(XMLHelper.getTextValueOf(result, "@label"));
			jsonDataset1.put(XMLHelper.getTextValueOf(result, "@dataset1"));
			jsonDataset2.put(XMLHelper.getTextValueOf(result, "@dataset2"));
		}
		
		JSONObject jsonResult = JSONFactoryUtil.createJSONObject();
		JSONObject jsonResultData = JSONFactoryUtil.createJSONObject();
		jsonResultData.put("id",       jsonIds);
		jsonResultData.put("label",    jsonTitles);
		jsonResultData.put("dataset1", jsonDataset1);
		if (jsonDataset2.length() > 0 && jsonDataset2.get(0) != null)
			jsonResultData.put("dataset2", jsonDataset2);
		jsonResult.put("title", chartTitle);
		jsonResult.put("data", jsonResultData);
		
		return jsonResult;
	}
	
	private void getVisitsStatistics( JSONObject response, List<Object[]> counters, List<Object[]> otherCounters, 
									  VisitsStatisticsRequest statisticsRequest, boolean reads, JSONObject jsonABTesting) throws NumberFormatException, ServiceError, SecurityException, NoSuchMethodException
	{	
		// Forma la respuesta JSON
		JSONObject jsonVisitsObject = JSONFactoryUtil.createJSONObject();
		JSONArray  jsonHoursArray 	= JSONFactoryUtil.createJSONArray();
		JSONArray  jsonVisitsArray 	= JSONFactoryUtil.createJSONArray();
		JSONArray  jsonReadsArray 	= JSONFactoryUtil.createJSONArray();
		JSONArray  jsonTotalArray 	= JSONFactoryUtil.createJSONArray();
		
		VisitsStatisticsRequest.Resolution resolution = statisticsRequest.getResolution();
		
		for (int i=0; i<counters.size(); i++)
		{
			Object[] counter = counters.get(i);
			switch (resolution) 
			{
			case HOUR:
				jsonHoursArray.put(String.format("%02d:00h", Integer.valueOf((counter[0].toString()))));
				break;
			case MINUTE:	
			case DAY:
			case MONTH:
				jsonHoursArray.put(counter[0].toString());
				break;
			}
			
			if (VisitsStatisticsRequest.StatisticItem.METADATA == statisticsRequest.getItem())
				jsonTotalArray.put(Integer.valueOf(counter[1].toString()));
			else
			{
				jsonVisitsArray.put(Integer.valueOf(counter[1].toString()));
				// Si hay lecturas, las añade
				if (reads)
					jsonReadsArray.put(Integer.valueOf(counter[2].toString()));
				// Si hay otros contadores, los suma al total
				if (Validator.isNotNull(otherCounters))
				{
					Object[] otherCounter = otherCounters.get(i);
					jsonTotalArray.put( Integer.valueOf(counter[1].toString()) + Integer.valueOf(otherCounter[1].toString()) );
				}
			}
		}
		
		jsonVisitsObject.put("hours", jsonHoursArray);
		
		if (jsonVisitsArray.length() > 0)
			jsonVisitsObject.put("visits", jsonVisitsArray);
		
		if (jsonReadsArray.length() > 0)
			jsonVisitsObject.put("reads", jsonReadsArray);
		
		if (jsonTotalArray.length() > 0)
			jsonVisitsObject.put("totals", jsonTotalArray);
		
		// Añade las anotaciones si se solocitan
		if (VisitsStatisticsRequest.StatisticItem.SURVEY != statisticsRequest.getPageType() && VisitsStatisticsRequest.Annotation.NONE != statisticsRequest.getAnnotations())
		{
			getAnnotations(jsonVisitsObject, statisticsRequest);
			// Si se están pidiendo estadísticas de un artículo agrupadas por horas o días, pide también las anotaciones del automáticas
			if (VisitsStatisticsRequest.StatisticItem.ARTICLE == statisticsRequest.getItem() && VisitsStatisticsRequest.Resolution.MONTH != statisticsRequest.getResolution())
			{
				getSysAnnotations(jsonVisitsObject, statisticsRequest);
			}
		}
		
		jsonVisitsObject.put("abtesting", jsonABTesting);
		
		response.put("data", jsonVisitsObject);
	}
	
	private void getAnnotations(JSONObject jsonVisitsObject, VisitsStatisticsRequest statisticsRequest) throws SecurityException, ServiceError, NoSuchMethodException
	{
		
		JSONArray jsonAnotations = JSONFactoryUtil.createJSONArray();
		
		Document annotationsDoc = VisitsStatisticsUtil.getAnnotations(statisticsRequest);
		List<Node> annotations = annotationsDoc.selectNodes("rs/row");
		for (Node annotation : annotations)
		{	
			JSONObject jsonAnnotation = JSONFactoryUtil.createJSONObject();
		
			// Añade la información necesaria para recuperar las notas
			String startDate  = XMLHelper.getStringValueOf(annotation, "@minDate");
			String endDate    = XMLHelper.getStringValueOf(annotation, "@maxDate");
			JSONObject annotationInfo = JSONFactoryUtil.createJSONObject();
			annotationInfo.put("groupId", statisticsRequest.getGroupId());
			annotationInfo.put("startDate", startDate);
			annotationInfo.put("endDate", endDate);
			annotationInfo.put("idart", statisticsRequest.getArticleId());
			annotationInfo.put("mode", statisticsRequest.getAnnotations().toString());
			jsonAnnotation.put("info", annotationInfo);

			// Etiqueta del eje x en la que colocar la notificación
			String label = XMLHelper.getStringValueOf(annotation, "@label");
			jsonAnnotation.put("label", label);
			
			jsonAnotations.put(jsonAnnotation);
		}
		
		if (jsonAnotations.length() > 0)
		{
			jsonVisitsObject.put("annotations", jsonAnotations);
		}
	}
	
	private void getSysAnnotations(JSONObject jsonVisitsObject, VisitsStatisticsRequest statisticsRequest) throws SecurityException, ServiceError, NoSuchMethodException
	{
		JSONArray jsonAnotations = JSONFactoryUtil.createJSONArray();
		
		Document annotationsDoc = VisitsStatisticsUtil.getSystemAnnotations(statisticsRequest);
		List<Node> annotations = annotationsDoc.selectNodes("rs/row");
		for (Node annotation : annotations)
		{	
			JSONObject jsonAnnotation = JSONFactoryUtil.createJSONObject();
			jsonAnnotation.put("label", XMLHelper.getStringValueOf(annotation, "@label"));
			jsonAnnotation.put("note", XMLHelper.getStringValueOf(annotation, "@note"));
			jsonAnotations.put(jsonAnnotation);
		}
		
		if (jsonAnotations.length() > 0)
		{
			jsonVisitsObject.put("sysannotations", jsonAnotations);
		}
	}
	
	private void getSocialStatistics(JSONObject response, List<Object> socialStats)
	{
		int fb = 0;
		int tw = 0;
		int gp = 0;
		int dq = 0;
		
		for (int i = socialStats.size() - 1; i >= 0; i--)
		{
			Object[] stat = ((Object[]) socialStats.get(i));
			int socialNetwork = Integer.parseInt(stat[0].toString());
			
			switch (socialNetwork) {
			case IterKeys.OPERATION_FB:
				fb = Integer.parseInt(stat[1].toString());
				break;
			case IterKeys.OPERATION_TW:
				tw = Integer.parseInt(stat[1].toString());
				break;
			case IterKeys.OPERATION_GP:
				gp = Integer.parseInt(stat[1].toString());
				break;
			case IterKeys.OPERATION_DISQUS:
				dq = Integer.parseInt(stat[1].toString());
			}
		}
		
		JSONObject jsonSocialNetworks = JSONFactoryUtil.createJSONObject();
		jsonSocialNetworks.put("facebook", fb);
		jsonSocialNetworks.put("twitter", tw);
		jsonSocialNetworks.put("google", gp);
		jsonSocialNetworks.put("disqus", dq);
		response.put("socialStatistics", jsonSocialNetworks);
	}
	
	private void getFeedbackStatistics(JSONObject jsonMainObject, VisitsStatisticsRequest statisticsRequest) throws SecurityException, ServiceError, NoSuchMethodException, JSONException
	{
		// Busca si hay configuración del feedback portlet
		Document feedbackDoc = FeedbackLocalServiceUtil.getFeedbackConf(statisticsRequest.getGroupId());
		if (null != feedbackDoc)
		{
			Map<String, String> choicesMap = new HashMap<String, String>();
			for (Node choice : feedbackDoc.selectNodes("/feedback/question/choices/choice"))
			{
				choicesMap.put(XMLHelper.getStringValueOf(choice, "@choiceid"), XMLHelper.getStringValueOf(choice, "choicelabel"));
			}
			
			// Busca las votaciones del artículo
			Map<String, String> votesMap = new HashMap<String, String>();
			JSONObject feedbackStats = CountersLocalServiceUtil.getUsersFeedback(String.valueOf(statisticsRequest.getGroupId()), statisticsRequest.getArticleId());
			if(Validator.isNotNull(feedbackStats))
			{
				JSONArray options = feedbackStats.getJSONArray("options");
				if (options != null && options.length() > 0)
				{
					for (int i=0; i<options.length(); i++)
					{
						JSONObject option = options.getJSONObject(i);
						votesMap.put(option.getString("optionid"), option.getString("votes"));
					}
				}
			}
			
			// Formatea el JSON para las estadísticas
			JSONArray feedbackStatisticsLabels = JSONFactoryUtil.createJSONArray();
			JSONArray feedbackStatisticsVotes = JSONFactoryUtil.createJSONArray();
			for (String choiceid : choicesMap.keySet())
			{
				feedbackStatisticsLabels.put(choicesMap.get(choiceid));
				feedbackStatisticsVotes.put(GetterUtil.getFloat(votesMap.get(choiceid), 0));
			}
			
			// Añade las estadísticas al resto
			JSONObject feedbackStatistics = JSONFactoryUtil.createJSONObject();
			feedbackStatistics.put("labels", feedbackStatisticsLabels);
			feedbackStatistics.put("votes", feedbackStatisticsVotes);
			jsonMainObject.put("feedbackStatistics", feedbackStatistics);
		}
	}
}
