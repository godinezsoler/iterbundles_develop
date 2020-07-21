package com.protecmedia.iter.news.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.restapi.resource.user.RestApiUserRfvUtil;

public class RecommendedArticlesUtil
{
	public static enum SOURCES
	{
		PROMOTED, MLY, MLT, TRENDS;
		
		public static String[] list()
		{
			return null;
		}
	};
	
	private static final String SQL_GET_PLID_BY_UUID = "SELECT plid FROM layout WHERE uuid_ in ('%s')";
	
	public static JsonArray getPromotedarticles(Long groupId, JsonObject config, List<String> visitedArticles) throws SecurityException, NoSuchMethodException, SystemException, ServiceError
	{
		// Recupera las secciones
		String sections = StringPool.BLANK;
		String[] sectionPlids = new String[0];
		JsonArray sectionsList = config.get("sections").getAsJsonArray();
		if (sectionsList.size() > 0)
		{
			sections = StringUtils.join(new Gson().fromJson(sectionsList, ArrayList.class), "', '");
			Document plidsDoc = PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_GET_PLID_BY_UUID, sections));
			sectionPlids = XMLHelper.getStringValues(plidsDoc.getRootElement().selectNodes("/rs/row/@plid"));
			sections = StringUtils.join(sectionPlids, " OR ");
		}
		
		// Recupera las etiquetas
		String metadata = StringUtils.join(new Gson().fromJson(config.get("metadata").getAsJsonArray(), ArrayList.class), " OR ");
		
		// Recupera las calificaciones
		String qualifIdFilter = StringPool.BLANK;
		String qualificationIdFilter = StringPool.BLANK;
		JsonArray qualificationsList = config.get("qualifications").getAsJsonArray();
		if (qualificationsList.size() > 0 )
		{
			List<String> q = new ArrayList<String>();
			for (int i = 0; i < qualificationsList.size(); i++)
			{
				// Si hay secciones, permuta las secciones con las calificaciones
				if (sectionPlids.length > 0)
				{
					for (int j = 0; j < sectionPlids.length; j++) {
						q.add(sectionPlids[j] + StringPool.SECTION + qualificationsList.get(i).getAsString());
					}
				}
				// Si no hay secciones, añade las calificaciones sueltas
				else
				{
					q.add(qualificationsList.get(i).getAsString());
				}
			}
			
			if (sectionPlids.length > 0) qualifIdFilter = StringUtils.join(q, " OR ");
			else                         qualificationIdFilter = StringUtils.join(q, " OR ");
		}
		
		// Recupera el límite de fecha
		String dateFilter = getSolrDateFilter(config);
		
		// Ejecuta la consulta en el Solr
		return RecommendedArticlesSolrUtil.solrSelect(groupId, sections, metadata, qualifIdFilter, qualificationIdFilter, dateFilter, visitedArticles);
	}
	
	public static JsonArray getRelatedArticles(Long groupId, JsonObject config, String articleId, List<String> visitedArticles) throws Exception
	{
		ErrorRaiser.throwIfFalse(groupId > 0 && Validator.isNotNull(config) && Validator.isNotNull(articleId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		// Recupera los campos a usar para similitud de la configuración del sistema de sugerencias
		String similarityFields = StringPool.BLANK;
		String systemConfig = GroupConfigTools.getGroupConfigField(groupId, "suggestionssettings");
		if (Validator.isNotNull(systemConfig))
		{
			JsonArray jsonFields = new JsonParser().parse(systemConfig).getAsJsonObject().get("recommendations").getAsJsonObject().get("similarityfields").getAsJsonArray();
			similarityFields = StringUtils.join(new Gson().fromJson(jsonFields, ArrayList.class), StringPool.COMMA);
		}

		// Recupera el límite de fecha
		String dateFilter = getSolrDateFilter(config);
		
		// Ejecuta la consulta en el Solr
		return RecommendedArticlesSolrUtil.solrMlt(groupId, similarityFields, articleId, dateFilter, visitedArticles);
	}
	
	public static JsonArray getCustomArticles(Long groupId, JsonObject config, String visitorId, List<String> visitedArticles) throws Exception
	{
		ErrorRaiser.throwIfFalse(groupId > 0 && Validator.isNotNull(config), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Obtiene el modo, el umbral y el límite de fecha
		boolean distributedFilter = config.get("mode").getAsInt() > 0;
		int threshold = config.get("threshold").getAsInt();
		String dateFilter = getSolrDateFilter(config);
		
		if (distributedFilter)
		{	
			// Si ha superado el umbral de artículos consumidos
			if (visitedArticles.size() >= threshold)
			{
				// Pide al Solr los usuarios similares a mi que hayan superado el umbral
				List<String> relatedVisitors = RecommendedArticlesSolrUtil.solrRelatedVisitorsSelect(groupId, visitedArticles, visitorId, threshold);
				
				// Pide al Solr los artículos vistos por esos usuarios que no ha visto el visitante
				if (relatedVisitors.size() > 0)
				{
					return RecommendedArticlesSolrUtil.solrRelatedVisitorsArticlesSelect(groupId, relatedVisitors, visitorId, visitedArticles);
				}
			}
		}
		else
		{
			// Recupera los metadatos del visitante
			String categoriesFilter = RestApiUserRfvUtil.getVisitorMetadata(groupId, visitorId, threshold);
			
			// Si se tienen suficientes metadatos, realiza la consulta al Solr
			if (Validator.isNotNull(categoriesFilter))
			{
				return RecommendedArticlesSolrUtil.solrSelect(groupId, null, categoriesFilter, null, null, dateFilter, visitedArticles);
			}
		}
		return new JsonArray();
	}
	
	public static JsonArray getTrendArticles(Long groupId, JsonObject config, int topicNumArticles, List<String> visitedArticles) throws Exception
	{
		ErrorRaiser.throwIfFalse(groupId > 0 && Validator.isNotNull(config), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		JsonArray rankings = config.get("ranking").getAsJsonArray();
		boolean useTopics = config.get("mode").getAsInt() > 0;
		int numArticles = 100; // TODO parametrizar
		Date dateFilter = getJavaDateFilter(config);
		
		// Recupera los artículos de los rankings indicados
		List<String[]> recentArticles = new ArrayList<String[]>();
		List<String[]> viewedArticles = new ArrayList<String[]>();
		List<String[]> commentedArticles = new ArrayList<String[]>();
		for(int i = 0; i < rankings.size(); i++)
		{
			int ranking = rankings.get(i).getAsInt();
			
			switch (ranking) {
			// Más recientes
			case 0:
				recentArticles = TeaserContentUtil.getFilterArticles(groupId, null, new ArrayList<String>(), 0, numArticles, new Date(), 
					false, new String[]{"-1"}, 1, null, null, null, new String[0], false, "", null, dateFilter, null);
				break;
			// Más leídos
			case 1:
				viewedArticles = TeaserContentUtil.getFilterArticles(groupId, null, new ArrayList<String>(), 0, numArticles, new Date(), 
					false, new String[]{"5"}, 1, null, null, null, new String[0], false, "", null, dateFilter, null);
				break;
			// Más comentados
			case 2:
				commentedArticles = TeaserContentUtil.getFilterArticles(groupId, null, new ArrayList<String>(), 0, numArticles, new Date(), 
				    false, new String[]{"8"}, 0, null, null, null, new String[0], true, "", null, dateFilter, null);
				break;
			}
		}
		
		// Compone el array de artículos intercalando las fuentes
		List<String> trendArticles = new ArrayList<String>();
		int maxArticles = Math.max(Math.max(recentArticles.size(), viewedArticles.size()), commentedArticles.size());
		for (int i = 0; i < maxArticles; i++)
		{
			if (i < recentArticles.size())
			{
				String articleId = recentArticles.get(i)[0];
				if (!trendArticles.contains(articleId))
					trendArticles.add(articleId);
			}
			if (i < viewedArticles.size())
			{
				String articleId = viewedArticles.get(i)[0];
				if (!trendArticles.contains(articleId))
					trendArticles.add(viewedArticles.get(i)[0]);
			}
			if (i < commentedArticles.size())
			{
				String articleId = commentedArticles.get(i)[0];
				if (!trendArticles.contains(articleId))
					trendArticles.add(commentedArticles.get(i)[0]);
			}
		}
		
		// Si se quieren artículos similares
		if (useTopics)
		{
			// Se queda con los N primeros artículos donde N es elnúmero de artículos a mostrar
			if (trendArticles.size() > topicNumArticles)
				trendArticles = trendArticles.subList(0, topicNumArticles);
			
			if (trendArticles.size() > 0)
			{
				List<JsonArray> similarArticles = new ArrayList<JsonArray>();
				for (String articleId : trendArticles)
				{
					similarArticles.add(getRelatedArticles(groupId, config, articleId, visitedArticles));
				}
				
				return collateListItems(trendArticles, similarArticles.toArray(new JsonArray[0]));
			}
			
			return new JsonArray();
		}
		// Si se quieren los artículos del ranking
		else
		{
			return (new Gson()).toJsonTree(trendArticles, new TypeToken<List<String>>() {}.getType()).getAsJsonArray();
		}
	}
	
	private static final String SOLR_DATE_FILTER = "[%s TO *]";
	private static final SimpleDateFormat SOLR_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
	
	private static Date getJavaDateFilter(JsonObject config)
	{
		GregorianCalendar cal = null;
		
		if (!config.get("anyDate").getAsBoolean())
		{
			int amount = config.get("dateAmount").getAsInt();
			int unit = config.get("dateUnit").getAsInt();

			cal = new GregorianCalendar();
			switch (unit) {
			case 0:
				cal.add(Calendar.DAY_OF_MONTH, -amount);
				break;
			case 1:
				cal.add(Calendar.MONTH, -amount);
				break;
			case 2:
				cal.add(Calendar.YEAR, -amount);
				break;
			default:
				cal = null;
				break;
			}
		}
		
		return cal == null ? null : cal.getTime();
	}
	
	private static String getSolrDateFilter(JsonObject config)
	{
		String filter = StringPool.STAR;
		
		Date dateFilter = getJavaDateFilter(config);
		if (dateFilter != null)
			filter = String.format(SOLR_DATE_FILTER, SOLR_DATE_FORMAT.format(dateFilter));
		
		return filter;
	}
	
	private static JsonArray collateListItems(List<String> excludedArticles, JsonArray ... arrays)
	{
		// Inicializa el array intercalado
		List<String> collatedArray = new ArrayList<String>();
		
		if (arrays.length > 0)
		{
			// Obtiene el tamaño del array más largo
			int maxSize = 0;
			for (JsonArray a : arrays)
			{
				if (a.size() > maxSize)
				{
					maxSize = a.size();
				}
			}
			
			// Intercala los elementos
			for (int i = 0; i < maxSize; i++)
			{
				for (JsonArray a : arrays)
				{
					if (i < a.size())
					{
						String articleId = a.get(i).getAsString();
						if (!collatedArray.contains(articleId) && !excludedArticles.contains(articleId))
							collatedArray.add(articleId);
					}
				}
			}
		}
		
		// Retorna el resultado
		JsonElement result = (new Gson()).toJsonTree(collatedArray, new TypeToken<List<String>>() {}.getType());
		return result.isJsonArray() ? result.getAsJsonArray() : new JsonArray();
	}
}
