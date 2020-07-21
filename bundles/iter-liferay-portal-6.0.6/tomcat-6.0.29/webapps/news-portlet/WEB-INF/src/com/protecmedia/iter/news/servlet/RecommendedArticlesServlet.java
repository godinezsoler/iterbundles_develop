package com.protecmedia.iter.news.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.DateUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.restapi.resource.article.RestApiRecommendationsUtil;
import com.liferay.restapi.resource.user.RestApiUserRfvUtil;
import com.protecmedia.iter.news.util.RecommendedArticlesSolrUtil;
import com.protecmedia.iter.news.util.RecommendedArticlesUtil;

public class RecommendedArticlesServlet extends HttpServlet implements Servlet
{
	private static final long serialVersionUID = 1L;
	private static Log log = LogFactoryUtil.getLog(RecommendedArticlesServlet.class);
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		PortalUtil.setVirtualHostLayoutSet(request);
		
		try
		{
			// Obtiene los parámetros
			String requestPath = request.getPathInfo().startsWith(StringPool.SLASH) ? request.getPathInfo().substring(1) : request.getPathInfo();
			String[] params = requestPath.split(StringPool.SLASH);
			ErrorRaiser.throwIfFalse(params.length > 0 && params.length <= 3, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			String configId = params[0];
			String visitorId = params.length > 1 ? params[1] : StringPool.BLANK;
			String articleId = params.length > 2 ? params[2] : StringPool.BLANK;
			Long groupId = Long.valueOf(PortalUtil.getScopeGroupId(request));
			ErrorRaiser.throwIfFalse(Validator.isNotNull(configId) && groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			// Si se informa un visitante, pide al Solr el segmento y la puntuación RFV del visitante
			JsonObject visitorInfo = Validator.isNotNull(visitorId) ? RestApiUserRfvUtil.getVisitorRfvSegmentId(groupId, visitorId) : new JsonObject();
			String rfvSegmentId = visitorInfo.get(RestApiUserRfvUtil.MAS_SEGMENTID_KEY).getAsString();
			String rfvSegmentName = visitorInfo.get(RestApiUserRfvUtil.MAS_SEGMENTNAME_KEY).getAsString();
			
			// Obtiene la configuración
			JsonObject recommendationConfig = PortalUUIDUtil.isValidUUID(configId) ?
					RestApiRecommendationsUtil.getRecommendationConfig(configId) :
					RestApiRecommendationsUtil.getRecommendationConfigByName(groupId, new String(Base64.decode(configId)));
			String configName = recommendationConfig.get("name").getAsString();
			JsonObject config = recommendationConfig.get("config").getAsJsonObject();
			int numArticles = config.get("numArticles").getAsInt();
			int sortingMode = config.get("sorting").getAsInt();
			
			// Obtiene el perfil de la recomendación
			JsonObject profileConfig = RestApiRecommendationsUtil.findRecommendationProfile(recommendationConfig, rfvSegmentId);
			
			// Pide a MAS la lista de artículos consumidos por el visitante
			List<String> visitedArticles = new ArrayList<String>();
			long lastVisitedArticlesUpdate = Validator.isNotNull(visitorId) ? RecommendedArticlesSolrUtil.solrVisitorArticlesSelect(groupId, visitorId, visitedArticles) : -1L;
			
			// Inicializa la respuesta
			JsonObject resultPayload = new JsonObject();
			if (Validator.isNotNull(rfvSegmentName))
				configName += StringPool.SPACE + StringPool.OPEN_PARENTHESIS + rfvSegmentName + StringPool.CLOSE_PARENTHESIS;
			resultPayload.addProperty("configName", configName);
			resultPayload.addProperty("visitorId", visitorId);
			resultPayload.addProperty("randomSort", sortingMode > 0);
			resultPayload.addProperty("lastUpdate", lastVisitedArticlesUpdate);
			JsonArray sourceArray = new JsonArray();
			// Variables para distriburir el número de artículos a mostrar
			JsonArray activeSources = new JsonArray();
			int distributedNumArticles = numArticles;
			
			// Procesa los orígenes
			for (RecommendedArticlesUtil.SOURCES sourceType : RecommendedArticlesUtil.SOURCES.values())
			{
				String sourceName = sourceType.toString().toLowerCase();
				JsonObject sourceConfig = RestApiRecommendationsUtil.findRecommendationSource(profileConfig, rfvSegmentId, sourceName);
				if (sourceConfig != null)
				{
					int sourceArticleAmmount = sourceConfig.get("numArticles").getAsInt();
					
					if (sourceArticleAmmount > 0 || (sourceArticleAmmount == 0 && sourceConfig.get("padding").getAsBoolean()))
					{
						// Calcula el número de artículos
						int sourceNumArticles = numArticles * sourceArticleAmmount / 100;
						distributedNumArticles -= sourceNumArticles;
						
						// Busca los artículos
						JsonArray sourceArticles = new JsonArray();
						switch (sourceType) {
						case PROMOTED:
							sourceArticles = RecommendedArticlesUtil.getPromotedarticles(groupId, sourceConfig, visitedArticles);
							break;
						case MLY:
							if (Validator.isNotNull(visitorId))
								sourceArticles = RecommendedArticlesUtil.getCustomArticles(groupId, sourceConfig, visitorId, visitedArticles);
							break;
						case MLT:
							if (Validator.isNotNull(articleId))
								sourceArticles = RecommendedArticlesUtil.getRelatedArticles(groupId, sourceConfig, articleId, visitedArticles);
							break;
						case TRENDS:
							sourceArticles = RecommendedArticlesUtil.getTrendArticles(groupId, sourceConfig, sourceNumArticles, visitedArticles);
							break;
						}

						// Crea el listado de artículos para la respuesta
						JsonObject sourceResponse = new JsonObject();
						sourceResponse.addProperty("name", sourceName);
						sourceResponse.addProperty("amount", sourceNumArticles);
						sourceResponse.add("articles", sourceArticles);
						sourceArray.add(sourceResponse);
						
						// Si no es para relleno, lo añade a la lista para distribuir los que faltan
						if (sourceArticleAmmount > 0)
							activeSources.add(sourceResponse);
					}
				}
			}
			
			// Redistribuye los artículos a mostrar que faltan
			if (activeSources.size() > 0)
			{
				for (int i = 0; i < distributedNumArticles; i++)
				{
					JsonObject source = activeSources.get(i % activeSources.size()).getAsJsonObject();
					source.addProperty("amount", source.get("amount").getAsInt() + 1);
				}
			}
			
			// Crea la respuesta
			resultPayload.add("sources", sourceArray);
			printResponse(resultPayload.toString(), response);
		}
		catch (Throwable th)
		{
			log.error(th);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private static String CONTENT_TYPE_CONTENT = "application/x-json-mly";
	private static String CONTENT_TYPE_HEADER = "<?php header('Content-Type: application/json; charset=utf-8');?>\n";
	private static String MAX_AGE_CONTENT = "max-age=%s";
	private static String MAX_AGE_HEADER = "<?php header('Cache-Control: max-age=300');?>\n";
	private static String EXPIRES_HEADER = "<?php header('Expires: %s');?>\n";
	private static String EXPIRES_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";
	private void printResponse(String resultPayload, HttpServletResponse response)
	{
		response.setStatus(HttpServletResponse.SC_OK);
		response.setHeader(WebKeys.ITER_RESPONSE_NEEDS_PHP, "1");	
		response.setContentType(CONTENT_TYPE_CONTENT);
		response.setCharacterEncoding(StringPool.UTF8);
		response.setHeader(HttpHeaders.CACHE_CONTROL, String.format(MAX_AGE_CONTENT, 300));
		Date date = DateUtils.addMinutes(new Date(), 5);
		SimpleDateFormat dateFormat = new SimpleDateFormat(EXPIRES_DATE_FORMAT, Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone(StringPool.UTC));
		String expires = dateFormat.format(date);
		response.setHeader(HttpHeaders.EXPIRES, expires);
		
	    PrintWriter out = null;
		try
		{
			out = response.getWriter();
			out.print(CONTENT_TYPE_HEADER + MAX_AGE_HEADER + String.format(EXPIRES_HEADER, expires) + resultPayload);
		}
		catch (IOException e)
		{
			log.error(e);
		}
		finally
		{
			if (out != null)
			{
				out.flush();
				out.close();
			}
		}
	}
}
