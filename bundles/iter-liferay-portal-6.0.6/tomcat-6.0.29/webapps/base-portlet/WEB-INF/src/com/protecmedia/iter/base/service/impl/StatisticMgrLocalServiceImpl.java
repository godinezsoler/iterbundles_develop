package com.protecmedia.iter.base.service.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.IterSecureConfigTools;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.VisitsStatisticsLocalServiceUtil;
import com.protecmedia.iter.base.service.base.StatisticMgrLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceError;

@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class StatisticMgrLocalServiceImpl extends StatisticMgrLocalServiceBaseImpl {
	
	private static Log _log = LogFactoryUtil.getLog(StatisticMgrLocalServiceImpl.class);
	
	private static final SimpleDateFormat sDF = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss);
	
	private static final String GET_STATISTIC_IN_IDS = new StringBuilder().append(
		"SELECT contentId, statisticCounter 					\n").append(
		"FROM iter_statistics_journalarticle  					\n").append(
		"	where contentId IN (%s) AND statisticOperation = ").append(IterKeys.OPERATION_DISQUS).toString();

	//String.format(QUERY, Http.HTTP_WITH_SLASH, PortalUtil.getUrlSeparator(), groupId);	
	private static final String GET_ARTICLES_VALID_LAPSED = new StringBuffer()
		.append("SELECT ifnull((SELECT sum(ifnull(facebookCounter, 0) + ifnull(twitterCounter, 0) + ifnull(googlePlusCounter, 0)) \n") 
		.append("               FROM iter_statistics_journalarticle \n")
	    .append("               WHERE contentid = News_PageContent.contentId), 0) shared, \n")
	    .append("       (SELECT indexable FROM JournalArticle WHERE articleId = News_PageContent.contentId) indexable, \n")
		.append("       News_PageContent.contentId, concat('%s', virtualHost, %s) url \n")
		.append("FROM Group_Config \n")
		.append("	INNER JOIN LayoutSet ON Group_Config.groupId = LayoutSet.groupId  and length(ifnull(LayoutSet.virtualHost, '')) > 0 \n")
		.append("   INNER JOIN News_PageContent ON News_PageContent.groupId = Group_Config.groupId \n")
		.append("	INNER JOIN Layout ON  News_PageContent.layoutid = Layout.uuid_ \n")
		.append("  	LEFT JOIN iter_statistics_journalarticle ON iter_statistics_journalarticle.contentId = News_PageContent.contentId AND iter_statistics_journalarticle.statisticOperation in (%s) \n")
		.append("WHERE Group_Config.groupId = '%s' \n")
		.append("	AND (iter_statistics_journalarticle.statisticDate IS NULL OR iter_statistics_journalarticle.statisticDate < '%s') \n")
		.append("	AND News_PageContent.vigenciahasta > Group_Config .lastPublicationDate \n")
		.append("	AND Group_Config .lastPublicationDate > News_PageContent.vigenciadesde \n")
		.append("HAVING indexable != 0 \n")
		.append("ORDER BY shared desc, News_PageContent.vigenciadesde DESC , iter_statistics_journalarticle.statisticDate ASC, iter_statistics_journalarticle.contentId ASC ")
		.toString();
	
	private static final String GET_CANONICALARTICLES_VALID_LAPSED = new StringBuffer()
		.append("SELECT shared, indexable, contentId, 																									\n")
		.append("       contentId, concat('%1$s', virtualHost, %2$s) url 																				\n")
		.append("FROM																																					\n")
		.append("(																																						\n")
		.append("SELECT IFNULL(statisticCounter, 0) shared,                               																				\n")
		.append("       indexable,                                                        																				\n")
		.append("       		                                                        																				\n")
		.append("       -- ITER-916 Consulta SQL demasiado costosa																										\n")
		.append("       virtualHost, News_PageContent.contentId, News_PageContent.groupId,																				\n")
		.append("       -- Se quieren los de mayor fecha de vigencia primero, por ello se toma la mayor por artículo													\n")
		.append("       MAX(News_PageContent.vigenciadesde) vigenciadesde,																								\n")
		.append("       -- statisticDate es único por artículo y red social, no es necesario hacer un MIN																\n")
		.append("       iter_statistics_journalarticle.statisticDate																									\n")
		.append("       		                                                        																				\n")
		.append("FROM Group_Config                                                        																				\n")
		.append("	INNER JOIN LayoutSet ON Group_Config.groupId = LayoutSet.groupId  and length(ifnull(LayoutSet.virtualHost, '')) > 0     							\n")
		.append("   INNER JOIN News_PageContent  ON News_PageContent.groupId = Group_Config.groupId                                         							\n")
		.append("   INNER JOIN JournalArticle ON JournalArticle.articleId = News_PageContent.contentId AND indexable != 0                   							\n")
		.append("  	LEFT JOIN iter_statistics_journalarticle ON iter_statistics_journalarticle.contentId = News_PageContent.contentId       							\n")
		.append("                                           AND iter_statistics_journalarticle.statisticOperation in (%3$s)                 							\n")
		.append("WHERE Group_Config.groupId = %4$s                                                                                          							\n")
		.append("   AND (iter_statistics_journalarticle.statisticDate IS NULL OR iter_statistics_journalarticle.statisticDateExpire < '%5$s') 							\n")
		.append("	AND News_PageContent.vigenciahasta > Group_Config .lastPublicationDate 																				\n")
		.append("	AND Group_Config .lastPublicationDate > News_PageContent.vigenciadesde 																				\n")
		.append("   %6$s 																																				\n")
		.append("GROUP BY News_PageContent.contentId 																													\n")
		.append(") News_PageContent																																			\n")
		.append("ORDER BY shared desc, vigenciadesde DESC, statisticDate ASC, contentId ASC ")
		.toString();
	
	private static final String GET_CANONICALARTICLES_VALID_LAPSED_RANGE_FILTER = new StringBuffer()
		.append(" AND JournalArticle.modifiedDate > (                                                                                                                                     \n")
		.append("   SELECT * FROM                                                                                                                                                         \n")
		.append("   (                                                                                                                                                                     \n")
		.append("     (                                                                                                                                                                   \n")
		.append(" 	  SELECT                                                                                                                                                              \n")
		.append(" 	    IF (ExtractValue(prefs.preferences, '/portlet-preferences/preference[name=\"modifiedDateRangeTimeUnit\"]/value') <> '',                                           \n")
		.append(" 		  CASE ExtractValue(prefs.preferences, '/portlet-preferences/preference[name=\"modifiedDateRangeTimeUnit\"]/value')                                               \n")
		.append(" 			WHEN 'hour' THEN                                                                                                                                              \n")
		.append(" 				IF (ExtractValue(prefs.preferences, '/portlet-preferences/preference[name=\"modifiedDateRangeTimeValue\"]/value') = 0,                                    \n")
		.append(" 					STR_TO_DATE('1947-07-03', '%%Y-%%m-%%d'),                                                                                                             \n")
		.append(" 					DATE_ADD(now(), INTERVAL -ExtractValue(prefs.preferences, '/portlet-preferences/preference[name=\"modifiedDateRangeTimeValue\"]/value') hour)         \n")
		.append(" 				)                                                                                                                                                         \n")
		.append(" 			WHEN 'day' THEN                                                                                                                                               \n")
		.append(" 				IF (ExtractValue(prefs.preferences, '/portlet-preferences/preference[name=\"modifiedDateRangeTimeValue\"]/value') = 0,                                    \n")
		.append(" 					STR_TO_DATE('1947-07-03', '%%Y-%%m-%%d'),                                                                                                             \n")
		.append(" 					DATE_ADD(now(), INTERVAL -ExtractValue(prefs.preferences, '/portlet-preferences/preference[name=\"modifiedDateRangeTimeValue\"]/value') day)          \n")
		.append(" 				)                                                                                                                                                         \n")
		.append(" 			WHEN 'month' THEN                                                                                                                                             \n")
		.append(" 				IF (ExtractValue(prefs.preferences, '/portlet-preferences/preference[name=\"modifiedDateRangeTimeValue\"]/value') = 0,                                    \n")
		.append(" 					STR_TO_DATE('1947-07-03', '%%Y-%%m-%%d'),                                                                                                             \n")
		.append(" 					DATE_ADD(now(), INTERVAL -ExtractValue(prefs.preferences, '/portlet-preferences/preference[name=\"modifiedDateRangeTimeValue\"]/value') month)        \n")
		.append(" 				)                                                                                                                                                         \n")
		.append(" 			ELSE STR_TO_DATE('1947-07-03', '%%Y-%%m-%%d')                                                                                                                 \n")
		.append(" 		  END,                                                                                                                                                            \n")
		.append("           CASE ExtractValue(sharedPrefs.preferences, '/portlet-preferences/preference[name=\"modifiedDateRangeTimeUnit\"]/value')                                       \n")
		.append(" 			WHEN 'hour' THEN                                                                                                                                              \n")
		.append(" 				IF (ExtractValue(sharedPrefs.preferences, '/portlet-preferences/preference[name=\"modifiedDateRangeTimeValue\"]/value') = 0,                              \n")
		.append(" 					STR_TO_DATE('1947-07-03', '%%Y-%%m-%%d'),                                                                                                             \n")
		.append(" 					DATE_ADD(now(), INTERVAL -ExtractValue(prefs.preferences, '/portlet-preferences/preference[name=\"modifiedDateRangeTimeValue\"]/value') hour)         \n")
		.append(" 				)                                                                                                                                                         \n")
		.append(" 			WHEN 'day' THEN                                                                                                                                               \n")
		.append(" 				IF (ExtractValue(sharedPrefs.preferences, '/portlet-preferences/preference[name=\"modifiedDateRangeTimeValue\"]/value') = 0,                              \n")
		.append(" 					STR_TO_DATE('1947-07-03', '%%Y-%%m-%%d'),                                                                                                             \n")
		.append(" 					DATE_ADD(now(), INTERVAL -ExtractValue(sharedPrefs.preferences, '/portlet-preferences/preference[name=\"modifiedDateRangeTimeValue\"]/value') day)    \n")
		.append(" 				)                                                                                                                                                         \n")
		.append(" 			WHEN 'month' THEN                                                                                                                                             \n")
		.append(" 				IF (ExtractValue(sharedPrefs.preferences, '/portlet-preferences/preference[name=\"modifiedDateRangeTimeValue\"]/value') = 0,                              \n")
		.append(" 					STR_TO_DATE('1947-07-03', '%%Y-%%m-%%d'),                                                                                                             \n")
		.append(" 					DATE_ADD(now(), INTERVAL -ExtractValue(sharedPrefs.preferences, '/portlet-preferences/preference[name=\"modifiedDateRangeTimeValue\"]/value') month)  \n")
		.append(" 				)                                                                                                                                                         \n")
		.append(" 			ELSE STR_TO_DATE('1947-07-03', '%%Y-%%m-%%d')                                                                                                                 \n")
		.append(" 		  END                                                                                                                                                             \n")
		.append(" 	    ) AS modifiedDate                                                                                                                                                 \n")
		.append(" 	  FROM portletpreferences prefs                                                                                                                                       \n")
		.append("                                                                                                                                                                         \n")
		.append(" 	  -- ITER-916 Consulta SQL demasiado costosa																														  \n")
		.append(" 	  INNER JOIN layout ON prefs.plid = Layout.plid AND Layout.groupId = %1$s																							  \n")
		.append("                                                                                                                                                                         \n")
		.append(" 	  LEFT JOIN PortletItem ON PortletItem.userName = ExtractValue(prefs.preferences, '/portlet-preferences/preference[name=\"portletItem\"]/value')                      \n")
		.append(" 	  LEFT JOIN PortletPreferences sharedPrefs ON sharedPrefs.ownerId = PortletItem.portletItemId                                                                         \n")
		.append("                                                                                                                                                                         \n")
		.append(" 	  WHERE prefs.portletId LIKE 'rankingviewerportlet_WAR_trackingportlet%%'                                                                                             \n")
		.append(" 	    AND prefs.ownerType = 3                                                                                                                                           \n")
		.append(" 	    -- ITER-916 Consulta SQL demasiado costosa																														  \n")
		.append(" 	    AND 0 = ( SELECT COUNT(*) 																																		  \n")
		.append(" 	              FROM portletpreferencesinfo																															  \n")
		.append(" 	                WHERE portletpreferencesinfo.portletPreferencesId = prefs.portletPreferencesId																		  \n")
		.append("               )																																						  \n")
		.append("                                                                                                                                                                         \n")
		.append("     ORDER BY modifiedDate ASC LIMIT 1                                                                                                                                   \n")
		.append("   )                                                                                                                                                                     \n")
		.append("   UNION ALL                                                                                                                                                             \n")
		.append("   (                                                                                                                                                                     \n")
		.append(" 	  SELECT STR_TO_DATE('1947-07-03', '%%Y-%%m-%%d')                                                                                                                     \n")
		.append("   )                                                                                                                                                                     \n")
		.append(" ) AS T                                                                                                                                                                  \n")
		.append(" ORDER BY modifiedDate DESC LIMIT 1                                                                                                                                      \n")
		.append(" )                                                                                                                                                                       \n")
		.toString();
	
	private static final String GET_ARTICLES = new StringBuffer()
		.append("SELECT contentId, concat('%s', virtualHost, %s) url \n")
		.append("FROM LayoutSet \n")
		.append("	INNER JOIN News_PageContent  ON LayoutSet.groupId = News_PageContent.groupId \n")
		.append("	INNER JOIN Layout ON  News_PageContent.layoutid = layout.uuid_ \n")
		.append("WHERE LayoutSet.groupId = '%s' AND length(ifnull(LayoutSet.virtualHost, '')) > 0 \n")
		.toString();	
		
	private static final String DEFAULT_STATISTIC_VALUE = "0";
	private static final String SHARE_COLUMN_NAME 		= "share_count";
	private static final String LIKE_COLUMN_NAME 		= "like_count";
	private static final String COMMENT_COLUMN_NAME 	= "comment_count";
	private static final String CLICK_COLUMN_NAME 		= "click_count";
	private static final String COMMENTSBOX_COLUMN_NAME = "commentsbox_count";
	
	private static final String FB_COUNTER_COLUMN_NAME	= "facebookCounter";
	private static final String GP_COUNTER_COLUMN_NAME	= "googlePlusCounter";
	private static final String TW_COUNTER_COLUMN_NAME	= "twitterCounter";

	private static final String DATA_INSERT_OR_UPDATE_COUNTER 	 = "(ITR_UUID(), '%s','%s','%s','%s','%s','%s','%s') \n";
	private static final String FB_DATA_INSERT_OR_UPDATE_COUNTER = "(ITR_UUID(), '%s','%s','%s','%s','%s','%s','%s','%s','%s','%s','%s','%s','%s') \n";
	private static final String TW_DATA_INSERT_OR_UPDATE_COUNTER = "(ITR_UUID(), '%s','%s','%s','%s','%s','%s','%s','%s') \n";
	private static final String GP_DATA_INSERT_OR_UPDATE_COUNTER = "(ITR_UUID(), '%s','%s','%s','%s','%s','%s','%s','%s') \n";
	
	private static final String FB_INSERT_OR_UPDATE_COUNTER_INSERTINTO = new StringBuffer()
		.append(",").append(SHARE_COLUMN_NAME)
		.append(",").append(LIKE_COLUMN_NAME)
		.append(",").append(COMMENT_COLUMN_NAME)
		.append(",").append(CLICK_COLUMN_NAME)
		.append(",").append(COMMENTSBOX_COLUMN_NAME)
		.append(",").append(FB_COUNTER_COLUMN_NAME)
		.toString();
	private static final String TW_INSERT_OR_UPDATE_COUNTER_INSERTINTO = new StringBuffer()
		.append(",").append(TW_COUNTER_COLUMN_NAME)
		.toString();
	private static final String GP_INSERT_OR_UPDATE_COUNTER_INSERTINTO = new StringBuffer()
		.append(",").append(GP_COUNTER_COLUMN_NAME)
		.toString();
	
	private static final String FB_INSERT_OR_UPDATE_COUNTER_UPDATES = new StringBuffer()
		.append(", statisticCounter")   .append("=VALUES(").append(FB_COUNTER_COLUMN_NAME) .append("),")
		.append(SHARE_COLUMN_NAME)      .append("=VALUES(").append(SHARE_COLUMN_NAME)      .append("),")
		.append(LIKE_COLUMN_NAME)       .append("=VALUES(").append(LIKE_COLUMN_NAME)       .append("),")
		.append(COMMENT_COLUMN_NAME)    .append("=VALUES(").append(COMMENT_COLUMN_NAME)    .append("),")
		.append(CLICK_COLUMN_NAME)      .append("=VALUES(").append(CLICK_COLUMN_NAME)      .append("),")
		.append(COMMENTSBOX_COLUMN_NAME).append("=VALUES(").append(COMMENTSBOX_COLUMN_NAME).append("),")
		.append(FB_COUNTER_COLUMN_NAME) .append("=VALUES(").append(FB_COUNTER_COLUMN_NAME) .append(")" )
		.toString();
	private static final String TW_INSERT_OR_UPDATE_COUNTER_UPDATES = new StringBuffer()
		.append(", statisticCounter")  .append("=VALUES(").append(TW_COUNTER_COLUMN_NAME).append("),")
		.append(TW_COUNTER_COLUMN_NAME).append("=VALUES(").append(TW_COUNTER_COLUMN_NAME).append(")")
		.toString();
	private static final String GP_INSERT_OR_UPDATE_COUNTER_UPDATES = new StringBuffer()
	    .append(", statisticCounter")  .append("=VALUES(").append(GP_COUNTER_COLUMN_NAME).append("),")
	    .append(GP_COUNTER_COLUMN_NAME).append("=VALUES(").append(GP_COUNTER_COLUMN_NAME).append(")")
	    .toString();
	
	private static final String INSERT_OR_UPDATE_COUNTER = new StringBuffer()
		.append("INSERT INTO iter_statistics_journalarticle (id, contentId, groupId, statisticOperation, statisticValue, statisticDate, statisticDateExpire, statisticCounter %s) \n")  
		.append("values %s \n")
		.append("ON DUPLICATE KEY UPDATE statisticValue=VALUES(statisticValue), statisticDate=VALUES(statisticDate), statisticDateExpire=VALUES(statisticDateExpire) %s")
		.toString();

	// Obtiene los artículos con vigencia con y sin url canónica
	@Override	
	public List<Node> getArticlesValidLapsed(String name, String operators, String groupId ) throws NoSuchMethodException, SecurityException, ServiceError
	{
		List<Node> nodes = new ArrayList<Node>();
		
		_log.trace(" obtainArticles isStarted -> get articles with layouts valid&lapsed");
		
		String canonicalURL = getCanonicalURL(true);

		String query = String.format(GET_ARTICLES_VALID_LAPSED, getIterProtocol(Long.parseLong(groupId)), canonicalURL, operators,  groupId, sDF.format(Calendar.getInstance().getTime()));
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuffer("Query to get not canonical valid articles:\n").append( query ));
		Document articlesDom = PortalLocalServiceUtil.executeQueryAsDom( query );
		ErrorRaiser.throwIfNull(articlesDom);		
		
		nodes.addAll(articlesDom.selectNodes("/rs/row"));
		nodes.addAll(getCanonicalArticlesValidLapsed(name, operators, groupId));
		return nodes;
	}
	
	// Obtiene los artículos con vigencia con url canónica
	@Override	
	public List<Node> getCanonicalArticlesValidLapsed(String name, String operators, String groupId ) throws NoSuchMethodException, SecurityException, ServiceError
	{
		List<Node> nodes = new ArrayList<Node>();
		
		String canonicalURL = getCanonicalURL(false);

		String query = Integer.valueOf(operators) == IterKeys.OPERATION_DISQUS ?
					   String.format(GET_CANONICALARTICLES_VALID_LAPSED, getIterProtocol(Long.parseLong(groupId)), canonicalURL, operators, groupId, sDF.format(Calendar.getInstance().getTime()), StringPool.BLANK) :
					   String.format(GET_CANONICALARTICLES_VALID_LAPSED, getIterProtocol(Long.parseLong(groupId)), canonicalURL, operators, groupId, sDF.format(Calendar.getInstance().getTime()), String.format(GET_CANONICALARTICLES_VALID_LAPSED_RANGE_FILTER, groupId));
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuffer("Query to get canonical valid articles:\n").append( query ));
		Document articlesDom = PortalLocalServiceUtil.executeQueryAsDom( query );
		ErrorRaiser.throwIfNull(articlesDom);
		
		nodes.addAll(articlesDom.selectNodes("/rs/row"));
		return nodes;
	}
	
	// Actualiza en base de datos las estadísticas. Se llama desde SocialStatisticsThread
	@Override
	public void updateStatistics(Map<String, JSONObject> values, Integer max_sql_query_length, String groupId, Date date, Date dateExpire, Integer operator) throws IOException, SQLException, JSONException, NoSuchMethodException, SecurityException
	{	
		StringBuffer dataSqlQuery = new StringBuffer();
		List<Map<String, String>> statisticsData = new ArrayList<Map<String, String>>();
		for(Entry<String, JSONObject> entry : values.entrySet()){
			
			// El buffer ha llegado al maximo posible para jdbc, ejecutamos la sentencia y seguimos con el buffer limpio (creamos una consulta nueva)
			if(dataSqlQuery.length() >= max_sql_query_length)
			{
				
				String query;
				switch(operator){
					case IterKeys.OPERATION_FB:
						query = String.format(INSERT_OR_UPDATE_COUNTER, FB_INSERT_OR_UPDATE_COUNTER_INSERTINTO, dataSqlQuery.toString(), FB_INSERT_OR_UPDATE_COUNTER_UPDATES);
						break;
					case IterKeys.OPERATION_TW:
						query = String.format(INSERT_OR_UPDATE_COUNTER, TW_INSERT_OR_UPDATE_COUNTER_INSERTINTO, dataSqlQuery.toString(), TW_INSERT_OR_UPDATE_COUNTER_UPDATES);
						break;
					case IterKeys.OPERATION_GP:
						query = String.format(INSERT_OR_UPDATE_COUNTER, GP_INSERT_OR_UPDATE_COUNTER_INSERTINTO, dataSqlQuery.toString(), GP_INSERT_OR_UPDATE_COUNTER_UPDATES);
						break;
					default:
						query = String.format(INSERT_OR_UPDATE_COUNTER, "", dataSqlQuery.toString(),", statisticCounter=VALUES(statisticCounter)");
				}
				
				if(_log.isDebugEnabled())
					_log.debug(new StringBuffer("UpdateQuery: ").append(query));
				
				// Para evitar deadlocks al llamar a esta función múltiples hilos concurrentemente
				synchronized(this)
				{
					PortalLocalServiceUtil.executeUpdateQuery(query);
					if (statisticsData.size() > 0)
					{
						VisitsStatisticsLocalServiceUtil.updateArticle("social", statisticsData);
						statisticsData.clear();
					}
				}
				
				dataSqlQuery = new StringBuffer(); 
			}
			if(dataSqlQuery.length() > 0){
				dataSqlQuery.append(", \n");
			}
			dataSqlQuery.append(getQueryFromEntry(entry, operator ,groupId, date, dateExpire));
			getStatistcisFromEntry(entry, operator ,groupId, statisticsData);
		}
		
		String socialNetworkName = "";
		if(dataSqlQuery.length() > 0)
		{			
			String query;
			switch(operator){
				case IterKeys.OPERATION_FB:
					socialNetworkName = "FACEBOOK";
					query = String.format(INSERT_OR_UPDATE_COUNTER, FB_INSERT_OR_UPDATE_COUNTER_INSERTINTO, dataSqlQuery.toString(), FB_INSERT_OR_UPDATE_COUNTER_UPDATES);
					break;
				case IterKeys.OPERATION_TW:
					socialNetworkName = "TWITTER";
					query = String.format(INSERT_OR_UPDATE_COUNTER, TW_INSERT_OR_UPDATE_COUNTER_INSERTINTO, dataSqlQuery.toString(), TW_INSERT_OR_UPDATE_COUNTER_UPDATES);
					break;
				case IterKeys.OPERATION_GP:
					socialNetworkName = "GOOGLE+";
					query = String.format(INSERT_OR_UPDATE_COUNTER, GP_INSERT_OR_UPDATE_COUNTER_INSERTINTO, dataSqlQuery.toString(), GP_INSERT_OR_UPDATE_COUNTER_UPDATES);
					break;
				default:
					socialNetworkName = "DEFAULT (DISQUS or OPERATION_SHARED)";
					query = String.format(INSERT_OR_UPDATE_COUNTER, "", dataSqlQuery.toString(),", statisticCounter=VALUES(statisticCounter)");
			}
			
			if(_log.isDebugEnabled())
				_log.debug(new StringBuffer("Query to update " + socialNetworkName + " statistics:\n").append(query));
			
			// Para evitar deadlocks al llamar a esta función múltiples hilos concurrentemente
			synchronized(this)
			{
				PortalLocalServiceUtil.executeUpdateQuery(query);
				if (statisticsData.size() > 0)
					VisitsStatisticsLocalServiceUtil.updateArticle("social", statisticsData);
			}
		}
	}
	
	// Lee un json y crea una linea de inserción (values)
	private String getQueryFromEntry(Entry<String, JSONObject> entry, Integer operator, String groupId, Date date, Date dateExpire) throws JSONException
	{
		String dateDB = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss).format(date.getTime());
		String dateExpireDB = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss).format(dateExpire.getTime());
		StringBuffer query = new StringBuffer();
		JSONObject values = entry.getValue();
		switch(operator){
			case IterKeys.OPERATION_FB:
				Integer valueShare      = values.getInt(SHARE_COLUMN_NAME);
				Integer valueLike       = values.getInt(LIKE_COLUMN_NAME);
				Integer valueComment    = values.getInt(COMMENT_COLUMN_NAME);
				Integer valueClick      = values.getInt(CLICK_COLUMN_NAME);
				Integer valueCommentBox = values.getInt(COMMENTSBOX_COLUMN_NAME);
				Integer fb_total		= valueShare;
				query.append(String.format(FB_DATA_INSERT_OR_UPDATE_COUNTER, entry.getKey(), groupId, IterKeys.OPERATION_FB , DEFAULT_STATISTIC_VALUE, dateDB, dateExpireDB, fb_total, valueShare, valueLike, valueComment, valueClick, valueCommentBox, fb_total));
			break;
			case IterKeys.OPERATION_TW:
				Integer tw_total = values.getInt("count");
				query.append(String.format(TW_DATA_INSERT_OR_UPDATE_COUNTER, entry.getKey(), groupId, IterKeys.OPERATION_TW , DEFAULT_STATISTIC_VALUE, dateDB, dateExpireDB, tw_total, tw_total));
				break;
			case IterKeys.OPERATION_GP:
				final int gp_total = values.getInt("count");
				query.append(String.format(GP_DATA_INSERT_OR_UPDATE_COUNTER, entry.getKey(), groupId, IterKeys.OPERATION_GP , DEFAULT_STATISTIC_VALUE, dateDB, dateExpireDB, gp_total, gp_total));
				break;
			default:
				query.append(String.format(DATA_INSERT_OR_UPDATE_COUNTER, entry.getKey(), groupId, operator , DEFAULT_STATISTIC_VALUE, dateDB, dateExpireDB, values.getInt("comments")));
		}
		return query.toString();
	}
	
	private static final String SQL_GET_CURRENT_COUNTER = "SELECT statisticCounter FROM iter_statistics_journalarticle WHERE contentId=%s AND groupId=%s AND statisticOperation=%d";
	private void getStatistcisFromEntry(Entry<String, JSONObject> entry, Integer operator, String groupId, List<Map<String, String>> statisticsData) throws SecurityException, NoSuchMethodException
	{
		// Recupera el contador actual
		Document dom = PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_GET_CURRENT_COUNTER, entry.getKey(), groupId, operator));
		long currentCounter = XMLHelper.getLongValueOf(dom, "/rs/row/@statisticCounter");
		
		JSONObject values = entry.getValue();
		long sharings = 0;
		long comments = 0;
		
		switch(operator)
		{
			case IterKeys.OPERATION_FB:
				sharings = values.getInt(SHARE_COLUMN_NAME) - currentCounter;
				break;
			
			case IterKeys.OPERATION_TW:
			case IterKeys.OPERATION_GP:
				sharings = values.getInt("count") - currentCounter;
				break;
			
			default:
				comments = values.getInt("comments") - currentCounter;
		}
		
		if (sharings > 0 || comments > 0)
		{
			Map<String, String> statistics = new HashMap<String, String>();
			statistics.put("groupId", groupId);
			statistics.put("articleId", entry.getKey());
			statistics.put("sharings", String.valueOf(sharings));
			statistics.put("comments", String.valueOf(comments));
			statisticsData.add(statistics);
		}
	}
	
	@Override
	public Document getStatisticsInIds(String[] ids) throws NoSuchMethodException, SecurityException, ServiceError
	{
		StringBuilder inClause = new StringBuilder();
		for (String id: ids)
		{
			if(inClause.length() > 0)
				inClause.append(",");
			inClause.append("'").append(id).append("'");
		}
		Document dom = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_STATISTIC_IN_IDS, inClause));
		ErrorRaiser.throwIfNull(dom);
		return dom;
	}
	
	// Obtiene todos los artículos del mundo mundial (para la primera vez que corre la recolección de estadísticas)
	@Override 
	public Document getArticles(String groupId) throws ServiceError, NoSuchMethodException, SecurityException
	{
		String canonicalURL = getCanonicalURL(true);
		String query = String.format(GET_ARTICLES, getIterProtocol(Long.parseLong(groupId)), canonicalURL, groupId);
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuffer("Query to get all articles:\n").append( query ));
		
		Document dom = PortalLocalServiceUtil.executeQueryAsDom( query );
		ErrorRaiser.throwIfNull(dom);
		return dom;
	}
	
	private static String getIterProtocol(long groupId)
	{
		return IterSecureConfigTools.getConfiguredHTTPS(groupId) ? Http.HTTPS_WITH_SLASH : Http.HTTP_WITH_SLASH;
	}
	
	private String getCanonicalURL(boolean withSection)
	{
		String canonicalURL = "";
		if(PropsValues.ITER_SEMANTICURL_ENABLED)
			canonicalURL = " ITR_GET_CANONICAL_SEMANTICURL(News_PageContent.contentId, News_PageContent.groupId) ";
		else
		{
			canonicalURL = String.format(" ITR_GET_CANONICAL_URL(News_PageContent.contentId, News_PageContent.groupId, '%s') ", PortalUtil.getUrlSeparator());
			if(withSection)
				canonicalURL = String.format("replace(%s, News_PageContent.contentId, concat(News_PageContent.contentId, '/',replace(substring(friendlyurl, 2), '/', '+') ))", canonicalURL);
		}
		
		return canonicalURL;
	}
	
	private static final String GET_ARTICLE_SOCIAL_STATISTICS = new StringBuffer()
	.append("SELECT statisticOperation, statisticCounter \n")
	.append("FROM iter_statistics_journalarticle         \n")
	.append("WHERE groupId = %d AND contentId = '%s'       ")
	.toString();
	
	public List<Object> getArticleSocialStatistics(long groupId, String articleId) throws ServiceError
	{
		// Valida los parámetros de entrada
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(articleId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		// Recupera las estadísticas
		String sql = String.format(GET_ARTICLE_SOCIAL_STATISTICS, groupId, articleId);
		return PortalLocalServiceUtil.executeQueryAsList(sql);
	}
}