package com.protecmedia.iter.news.util;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.IterGlobalKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.util.IterHttpClient;

public class RecommendedArticlesSolrUtil
{
	private static final String SOLR_HANDLER_SELECT = "/select";
	private static final String SOLR_HANDLER_MLT = "/mlt";
	
	private static final String SOLR_OPERATOR_OR = " OR ";
	
	private static final String SOLR_ENTRTYCLASSPK_KEY = "entryClassPK";
	private static final String SOLR_ARTICLEID_KEY = "articleId";
	private static final String SOLR_VISITORID_KEY = "visitorId";

	private static final String SOLR_RESPONSE_ROOT_KEY = "response";
	private static final String SOLR_RESPONSE_DOCS_KEY = "docs";
	private static final String SOLR_RESPONSE_FACET_COUNTS_KEY = "facet_counts";
	private static final String SOLR_RESPONSE_FACET_FIELDS_KEY = "facet_fields";
	
	private static final String SOLR_SELECT_PARAMS = new StringBuilder()
	.append("q=%s")
	.append("&fq=entryClassPK:*+scopeGroupId:%d+modifiedDate:%s%s")
	.append("&fl=entryClassPK")
	.append("&wt=json")
	.append("&rows=").append(PropsValues.RECOMMENDATION_SOLR_AMOUNT)
	.toString();
	
	private static final String SOLR_QUERY_BASE = "structureId:STANDARD-ARTICLE";
	private static final String SOLR_QUERY_SECTIONS = " layoutsPlid:(%s)";
	private static final String SOLR_QUERY_METADATA = " assetCategoryIds:(%s)";
	private static final String SOLR_QUERY_QUALIFIC = " qualifId:(%s)";  // <plid>§<qualifiId>
	private static final String SOLR_QUERY_QUALIFIC_NS = " qualificationId:(%s)";
	private static final String SOLR_FILTER_EXCLUDED_ENTRYCLASSPK = " -entryClassPK:(%s)";
	
	private static final String SOLR_MLT_PARAMS = new StringBuilder()
	.append("mlt.fl=%1$s")
	.append("&mlt.mintf=0")
	.append("&mlt.mindf=1")
	.append("&mlt.minwl=2")
	.append("&mlt.boost=true")
	.append("&mlt.qf=%1$s")
	.append("&mlt.match.include=false")
	.append("&fl=entryClassPK")
	.append("&fq=entryClassPK:*+scopeGroupId:%2$d+modifiedDate:%3$s%4$s")
	.append("&wt=json")
	.append("&rows=").append(PropsValues.RECOMMENDATION_SOLR_AMOUNT)
	.append("&q=entryClassPK:%5$s")
	.toString();
	
	private static final String SOLR_VISITOR_ARTICLES_SELECT = new StringBuilder()
	.append("q=visitorId:%s")
	.append("&fq=groupId:%d")
	.append("&facet=true&facet.field=articleId&facet.mincount=1&facet.limit=-1&facet.sort=count")
	.append("&wt=json")
	.append("&rows=0")
	.toString();
	
	private static final String SOLR_RELATED_VISITORS_SELECT = new StringBuilder()
	.append("q=articleId:(%s)")
	.append("&fq=groupId:%d")
	.append("&facet=true&facet.field=visitorId&facet.mincount=%d&facet.limit=11&facet.sort=count")
	.append("&wt=json")
	.append("&rows=0")
	.toString();
	
	private static final String SOLR_RELATED_VISITORS_ARTICLES_SELECT = new StringBuilder()
	.append("q=visitorId:(%s)")
	.append("&fq=groupId:%d+-visitorId:%s%s")
	.append("&fl=articleId")
	.append("&group=true&group.main=true&group.field=articleId")
	.append("&wt=json")
	.append("&rows=").append(PropsValues.RECOMMENDATION_SOLR_AMOUNT)
	.toString();
	
	private static final String SOLR_FILTER_EXCLUDED_ARTICLEID = " -articleId:(%s)";
	
	public static JsonArray solrSelect(Long groupId, String layoutsPlidFilter, String assetCategoryIdsFilter, String qualifIdFilter, String qualificationIdFilter, String dateFilter, List<String> visitedArticles) throws SystemException, ServiceError
	{
		// Crea la query
		String query = StringPool.BLANK;
		try
		{	
			query = String.format(SOLR_SELECT_PARAMS,
				new StringBuilder(SOLR_QUERY_BASE)
				.append(Validator.isNotNull(layoutsPlidFilter) ? URLEncoder.encode(String.format(SOLR_QUERY_SECTIONS, layoutsPlidFilter), StringPool.UTF8) : StringPool.BLANK)
				.append(Validator.isNotNull(assetCategoryIdsFilter) ? URLEncoder.encode(String.format(SOLR_QUERY_METADATA, assetCategoryIdsFilter), StringPool.UTF8) : StringPool.BLANK)
				.append(Validator.isNotNull(qualifIdFilter) ? URLEncoder.encode(String.format(SOLR_QUERY_QUALIFIC, qualifIdFilter), StringPool.UTF8) : StringPool.BLANK)
				.append(Validator.isNotNull(qualificationIdFilter) ? URLEncoder.encode(String.format(SOLR_QUERY_QUALIFIC_NS, qualificationIdFilter), StringPool.UTF8) : StringPool.BLANK)
				.toString(),
				groupId,
				URLEncoder.encode(dateFilter, StringPool.UTF8),
				visitedArticles.size() > 0 ? URLEncoder.encode(String.format(SOLR_FILTER_EXCLUDED_ENTRYCLASSPK, StringUtils.join(visitedArticles, SOLR_OPERATOR_OR)), StringPool.UTF8) : StringPool.BLANK
			);
		}
		catch (Throwable th)
		{
			throw new SystemException(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, th);
		}
		
		// Ejecuta la consulta en el Solr
		String solrResponse = querySolr(SOLR_HANDLER_SELECT, getSolrEndpoint(), query);
		
		// Parsea la respuesta eliminando los artículos vistos
		return parseSolrResponse(solrResponse, SOLR_ENTRTYCLASSPK_KEY, visitedArticles);
	}
	
	public static JsonArray solrMlt(Long groupId, String similarityFields, String articleId, String dateFilter, List<String> visitedArticles) throws SystemException, ServiceError
	{
		// Crea la query
		String query = StringPool.BLANK;
		try
		{
			query = String.format(SOLR_MLT_PARAMS,
				similarityFields,
				groupId,
				URLEncoder.encode(dateFilter, StringPool.UTF8),
				visitedArticles.size() > 0 ? URLEncoder.encode(String.format(SOLR_FILTER_EXCLUDED_ENTRYCLASSPK, StringUtils.join(visitedArticles, SOLR_OPERATOR_OR)), StringPool.UTF8) : StringPool.BLANK,
				articleId
			);
		}
		catch (Throwable th)
		{
			throw new SystemException(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, th);
		}
		
		// Ejecuta la consulta en el Solr
		String solrResponse = querySolr(SOLR_HANDLER_MLT, getSolrEndpoint(), query);
		
		// Parsea la respuesta eliminando los artículos vistos
		return parseSolrResponse(solrResponse, SOLR_ENTRTYCLASSPK_KEY, visitedArticles);
	}
	
	public static long solrVisitorArticlesSelect(Long groupId, String visitorId, List<String> visitedArticles) throws ServiceError
	{
		String query = String.format(SOLR_VISITOR_ARTICLES_SELECT, visitorId.toUpperCase(), groupId);
		
		// Ejecuta la consulta en el Solr
		String solrResponse = querySolr(SOLR_HANDLER_SELECT, getSolrArticleVisitorsEndpoint(groupId), query);
		
		// Parsea la respuesta
		visitedArticles.addAll(parseSolrFacetedResponse(solrResponse, SOLR_ARTICLEID_KEY, false, null));
		
		// Retorna la fecha de la última actualización
		JsonObject json = GroupConfigTools.getGroupConfigJSONField(groupId, "articlevisitors");
		return json.has("lastUpdate") ? json.get("lastUpdate").getAsLong() : -1L;
	}
	
	public static List<String> solrRelatedVisitorsSelect(Long groupId, List<String> articles, String currentVisitorId, int threshold) throws ServiceError
	{
		String query = StringPool.BLANK;
		
		try
		{
			String articlesFilter = URLEncoder.encode(StringUtils.join(articles, SOLR_OPERATOR_OR), StringPool.UTF8) ;
			query = String.format(SOLR_RELATED_VISITORS_SELECT, articlesFilter, groupId, threshold);
		}
		catch (Throwable th)
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
		}
		
		// Ejecuta la consulta en el Solr
		String solrResponse = querySolr(SOLR_HANDLER_SELECT, getSolrArticleVisitorsEndpoint(groupId), query);
		
		// Parsea la respuesta
		return parseSolrFacetedResponse(solrResponse, SOLR_VISITORID_KEY, true, currentVisitorId);
	}
	
	public static JsonArray solrRelatedVisitorsArticlesSelect(Long groupId, List<String> relatedVisitors, String currentVisitorId, List<String> visitedArticles) throws ServiceError
	{
		String query = StringPool.BLANK;
		
		try
		{
			// Crea el filtro de visitantes
			String visitorsFilter = URLEncoder.encode(StringUtils.join(relatedVisitors, SOLR_OPERATOR_OR), StringPool.UTF8);
			
			// Crea el filtro de artículos excluidos
			String articlesFilter = StringPool.BLANK;
			if (visitedArticles.size() > 0) 
				articlesFilter = URLEncoder.encode(String.format(SOLR_FILTER_EXCLUDED_ARTICLEID, StringUtils.join(visitedArticles, SOLR_OPERATOR_OR)),StringPool.UTF8);
			
			// Compone la query
			query = String.format(SOLR_RELATED_VISITORS_ARTICLES_SELECT, visitorsFilter, groupId, currentVisitorId, articlesFilter);
		}
		catch (Throwable th)
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
		}
		
		// Ejecuta la consulta en el Solr
		String solrResponse = querySolr(SOLR_HANDLER_SELECT, getSolrArticleVisitorsEndpoint(groupId), query);
		
		// Parsea la respuesta eliminando los artículos vistos
		return parseSolrResponse(solrResponse, SOLR_ARTICLEID_KEY, null);
	}
	
	/**
	 * Recupera el endpoint de Solr configurado en el fichero {@code portal-ext.properties},
	 * dando error si no está definido.
	 * @return El endpoint de Solr configurado.
	 * @throws ServiceError Si no hay configurado ningún endpoint.
	 */
	private static String getSolrEndpoint() throws ServiceError
	{
		// Recupera el endpoint de Solr
		String solrEndpoint = PropsUtil.get(IterGlobalKeys.PORTAL_PROPERTIES_KEY_ITER_SEARCH_PLUGIN_ENDPOINT);
		if( Validator.isNull(solrEndpoint))
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
		
		// Limpia la / final
		if (solrEndpoint.endsWith(StringPool.SLASH))
			solrEndpoint = solrEndpoint.substring(0, solrEndpoint.length() - 1);
		
		return solrEndpoint;
	}
	
	private static String getSolrArticleVisitorsEndpoint(long groupId) throws ServiceError
	{
		long delegationId = 0L;
		try
		{
			// Obtiene la delegación del grupo
			delegationId = GroupLocalServiceUtil.getGroup(groupId).getDelegationId();
		}
		catch (Throwable th)
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, th.getMessage(), th.getStackTrace());
		}
		// Obtiene el endpoint de Solr
		String solrEndpoint = PropsValues.ITER_SEARCH_PLUGIN_ENDPOINT;
		// Valida los datos necesarios
		ErrorRaiser.throwIfFalse(Validator.isNotNull(solrEndpoint) || delegationId > 0L, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
		// Limpia la / final
		solrEndpoint = solrEndpoint.endsWith(StringPool.SLASH) ? solrEndpoint.substring(0, solrEndpoint.length() - 1) : solrEndpoint;
		// Crea el endpoint de Solr
		return solrEndpoint + "_recommendations" + delegationId;
	}
	
	private static String querySolr(String handler, String solrEndpoint, String query) throws ServiceError
	{
		// Crea la URL
		String url = solrEndpoint + handler;
		// Llama a la URL
		IterHttpClient ihttpc = new IterHttpClient.Builder(IterHttpClient.Method.POST, url)
			.header(HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_X_WWW_FORM_URLENCODED)
			.payLoad(query)
			.build();
		// Retorna el resultado
		return ihttpc.connect();
	}
	
	private static JsonArray parseSolrResponse(String apiResponse, String key, List<String> visitedArticles)
	{
		JsonArray articles = new JsonArray();
		JsonElement root = new JsonParser().parse(apiResponse).getAsJsonObject().get(SOLR_RESPONSE_ROOT_KEY);
		JsonArray docs = root.isJsonObject() ? root.getAsJsonObject().get(SOLR_RESPONSE_DOCS_KEY).getAsJsonArray() : new JsonArray();
	
		for (int i = 0; i < docs.size(); i++)
		{
			if (visitedArticles == null || !visitedArticles.contains(docs.get(i).getAsJsonObject().get(key).getAsString()))
			{
				articles.add(docs.get(i).getAsJsonObject().get(key));
			}
		}
		
		return articles;
	}
	
	private static List<String> parseSolrFacetedResponse(String apiResponse, String key, boolean boost, String currentItemId)
	{
		List<String> items = new ArrayList<String>();
		
		JsonArray itemIds = new JsonParser().parse(apiResponse).getAsJsonObject()
			.get(SOLR_RESPONSE_FACET_COUNTS_KEY).getAsJsonObject()
			.get(SOLR_RESPONSE_FACET_FIELDS_KEY).getAsJsonObject()
			.get(key).getAsJsonArray();
	
		for (int i = 0; i <= itemIds.size() - 2; i += 2)
		{
			String itemId = itemIds.get(i).getAsString();
			String itemCount = itemIds.get(i + 1).getAsString();
			if (!itemId.equalsIgnoreCase(currentItemId))
			{
				items.add(boost ? itemId + StringPool.CARET + itemCount : itemId);
			}
		}
		
		return items;
	}
}
