package com.protecmedia.iter.news.servlet;

import java.io.PrintWriter;
import java.net.URLDecoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.util.CategoriesUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PHPUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.news.util.TopicsUtil;

public class Metalocator extends HttpServlet implements Servlet
{
	private static final long serialVersionUID = 1L;
	
	private static Log _log = LogFactoryUtil.getLog(Metalocator.class);
	
	private final static int NUM_PARAMS = 9;
	private final static int MAX_METAS  = 100;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
	{
		response.setContentType(WebKeys.CONTENT_TYPE_NO_CACHE);
		response.setCharacterEncoding("UTF-8");
		
		try
		{
			//Se recuperan los criterios de la búsqueda de metadatos
			String url = request.getRequestURL().toString();
			url = url.substring(url.indexOf("/news-portlet/") + 1, url.length());
			
			String path[] = url.split(StringPool.SLASH);
			if(path != null && path.length == NUM_PARAMS)
			{
				String term 					= StringEscapeUtils.escapeSql(request.getParameter("term"));
				int numMetadata 				= Integer.parseInt(path[2]);
				List<String> vocabularyIds 		= Arrays.asList(path[3].split(StringPool.DASH));
				List<String> categoryIds 		= Arrays.asList(path[4].split(StringPool.DASH));
				boolean onlyMetadataLastLevel 	= GetterUtil.getBoolean(path[5].toString());
				long modelId	 				= GetterUtil.getLong(path[6]);
				long scopeGroupId	 			= GetterUtil.getLong(path[7]);
				String contentType	 			= URLDecoder.decode(StringEscapeUtils.unescapeJavaScript(
														path[8]), "UTF-8").replace(StringPool.UNDERLINE, StringPool.SLASH);
				
				// Es necesario para que TopicsUtil.getTopicURLById incluya correctamente el prefijo (/web/groupName) de las URLs
				PortalUtil.setVirtualHostLayoutSet(request, scopeGroupId);
				
				if(numMetadata <= MAX_METAS)
				{
					//Se recuperan los metadatos que cumplen con los criterios
					StringBuilder query = new StringBuilder(
						"SELECT c.categoryId id, c.name label, c.name value 	\n").append(
						"FROM AssetCategory c 									\n").append(
						CategoriesUtil.getDiscreteCategoriesJoin(categoryIds)	   ).append(
						"	WHERE c.name LIKE '%").append(term).append("%'		\n");
					
					if(onlyMetadataLastLevel)
						query.append(" AND (c.rightCategoryId - c.leftCategoryId = 1) ");
					
					query.append(TopicsUtil.getSelectedExcludedTopicsSQL(vocabularyIds, Validator.isNotNull(categoryIds), null, null));
					query.append(" GROUP BY c.name ").append("LIMIT ").append(numMetadata);
					List<Map<String, Object>> databaseMetadatas = PortalLocalServiceUtil.executeQueryAsMap(query.toString());
					
	//				Se crea un listado de objetos JSON tipo:
					
	//				[{
	//  				"id": 109463,
	//				    "value": "Aaron Eckhart",
	//				    "label": "Aaron Eckhart",
	//				    "url": "http://localhost:8080/web/la-razon/etiquetas/content/meta/aaron-eckhart"
	//				}, {
	//				   	"id": 124988,
	//				   	"value": "Aaron Klug",
	//				   	"label": "Aaron Klug",
	//				   	"url": "http://localhost:8080/web/la-razon/etiquetas/content/meta/aaron-klug"
	//				}]
					
					JSONArray jsonMetadatasWithURL = JSONFactoryUtil.createJSONArray();
					JSONArray jsonMetadatasWithoutURL = JSONFactoryUtil.createJSONObject(JSONFactoryUtil.serialize(databaseMetadatas)).getJSONArray("list");
					for(int i = 0; i < jsonMetadatasWithoutURL.length(); i++)
					{
						JSONObject currentJSON = jsonMetadatasWithoutURL.getJSONObject(i).getJSONObject("map");
						String currentURL = TopicsUtil.getTopicURLById(modelId, currentJSON.getString("label"));
						
						String normalizedLabel = Normalizer.normalize(currentJSON.getString("label"), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
						String normalizedTerm  = Normalizer.normalize(term, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
						
						StringBuilder highlightedLabel = new StringBuilder(currentJSON.getString("label"));
						
						String spanInit = "<span class=\"hl_results\">";
						String spanEnd  = "</span>";
						
						Matcher matcher = Pattern.compile("(?i)(" + normalizedTerm + ")").matcher(normalizedLabel);
						
						List<Integer[]> sustitutions = new ArrayList<Integer[]>();
						while(matcher.find())
						{
							Integer [] currentIndexes = {matcher.start(), matcher.end()}; 
							sustitutions.add(currentIndexes);
						}

						for(int j = sustitutions.size() - 1; j > -1; j--)
						{
							highlightedLabel.insert(sustitutions.get(j)[1], spanEnd);
							highlightedLabel.insert(sustitutions.get(j)[0], spanInit);	
					    }
						
						currentJSON.put("label", highlightedLabel.toString()); 

						jsonMetadatasWithURL.put(currentJSON.put("url", currentURL));
					}
		
					response.setContentType(contentType);
					
				    PrintWriter out = response.getWriter();
				    out.print(jsonMetadatasWithURL.toString());
				    out.flush();
				}
				else
				{
					_log.error("Maximum number of metadatas is 100, current is " + numMetadata);
				}
			}
			else
			{
				_log.error("Bad URL: " + request.getRequestURL() + 
						new StringBuilder("\nExpected URL: /news-portlet/metalocator/"  ).append(
										  "[numMetadata]/[vocabularyIds]/"				).append(
										  "[categoryIds]/[onlyMetadataLastLevel]/"		).append(
										  "[modelId]/[scopeGroupId]/[contentType]"		).append(
										  "?term=term"									).toString());
			}
		}
		catch(Exception e)
		{
			_log.trace(e);
			_log.error(e.toString());
		}
	}
}
