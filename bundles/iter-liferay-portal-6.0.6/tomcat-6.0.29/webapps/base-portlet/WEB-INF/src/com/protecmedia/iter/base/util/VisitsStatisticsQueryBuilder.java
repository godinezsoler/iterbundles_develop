package com.protecmedia.iter.base.util;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.protecmedia.iter.base.model.VisitsStatisticsRequest;
import com.protecmedia.iter.base.model.VisitsStatisticsRequest.Resolution;

public class VisitsStatisticsQueryBuilder
{
	private static Log _log = LogFactoryUtil.getLog(VisitsStatisticsQueryBuilder.class);
	
	private static final String TABLE_ARTICLE_STATISTICS_REAL_TIME = "article_visits";
	
	private static final String TABLE_ARTICLE_STATISTICS_MINUTE    = "article_visits_minute_statistics";
	private static final String TABLE_ARTICLE_STATISTICS_HOUR      = "article_visits_statistics";
	private static final String TABLE_ARTICLE_STATISTICS_DAILY     = "article_visits_daily_statistics";
	private static final String TABLE_ARTICLE_STATISTICS_MONTHLY   = "article_visits_monthly_statistics";
	private static final String TABLE_ARTICLE_STATISTICS_ANNUAL    = "article_visits_annual_statistics";
	
	private static final String TABLE_SECTION_STATISTICS_REAL_TIME = "section_visits";
	private static final String TABLE_SECTION_STATISTICS_MINUTE    = "section_visits_minute_statistics";
	private static final String TABLE_SECTION_STATISTICS_HOUR      = "section_visits_statistics";
	private static final String TABLE_SECTION_STATISTICS_DAILY     = "section_visits_daily_statistics";
	private static final String TABLE_SECTION_STATISTICS_MONTHLY   = "section_visits_monthly_statistics";
	private static final String TABLE_SECTION_STATISTICS_ANNUAL    = "section_visits_annual_statistics";

	private static final String TABLE_METADATA_STATISTICS_REAL_TIME = "metadata_visits";
	private static final String TABLE_METADATA_STATISTICS_MINUTE   	= "metadata_visits_minute_statistics";
	private static final String TABLE_METADATA_STATISTICS_HOUR    	= "metadata_visits_statistics";
	private static final String TABLE_METADATA_STATISTICS_DAILY     = "metadata_visits_daily_statistics";
	private static final String TABLE_METADATA_STATISTICS_MONTHLY   = "metadata_visits_monthly_statistics";
	private static final String TABLE_METADATA_STATISTICS_ANNUAL    = "metadata_visits_annual_statistics";
	
	private static final String SQL_GET_VISITS_SELECT = new StringBuilder()
	.append("SELECT statisticsDate,                                      \n")
	.append("       SUM(visits) visits                                   \n")
	.toString();
	
	private static final String SQL_GET_VISITS_SELECT_RT = new StringBuilder()
	.append("SELECT DATE_FORMAT(now(), '%%Y-%%m-%%d %%H:%%i:00') statisticsDate, \n")
	.append("       SUM(visits - lastUpdateVisits) visits                       \n")
	.toString();
	
	private static final String SQL_GET_READS_SELECT = new StringBuilder()
	.append("SELECT statisticsDate,                                      \n")
	.append("       SUM(visits) visits,                                  \n")
	.append("       SUM(readings) readings                               \n")
	.toString();
	
	private static final String SQL_GET_READS_SELECT_RT = new StringBuilder()
	.append("SELECT DATE_FORMAT(now(), '%%Y-%%m-%%d %%H:%%i:00') statisticsDate, \n")
	.append("       SUM(visits - lastUpdateVisits) visits,                      \n")
	.append("       SUM(readings - lastUpdateReadings) readings                 \n")
	.toString();
	
	private static final String SQL_GET_VISIST_NO_RT = new StringBuilder()
	.append("%1$s                                                          \n")
	.append("FROM %2$s                                                     \n")
	.append("%3$s                                                         \n")
	.append("WHERE %2$s.groupId = %%1$d %%2$s                                 \n")
	.append("AND statisticsDate >= '%%3$s' AND statisticsDate <= '%%4$s' \n")
	.append("GROUP BY statisticsDate                                     \n")
	.append("ORDER BY statisticsDate ASC"                                   )
	.toString();
	
	private static final String SQL_GET_VISIST_RT = new StringBuilder()
	.append("%1$s                                                              \n")
	.append("FROM %2$s                                                         \n")
	.append("%5$s                                                         \n")
	.append("WHERE %2$s.groupId = %%1$d %%2$s                                     \n")
	.append("AND statisticsDate >= '%%3$s' AND statisticsDate <= '%%4$s'     \n")
	.append("GROUP BY statisticsDate                                         \n")
	.append("UNION ALL                                                       \n")
	.append("%3$s                                                              \n")
	.append("FROM %4$s                                                         \n")
	.append("%5$s                                                         \n")
	.append("WHERE %4$s.groupId = %%1$d AND updatePending=1 %%2$s                 \n")
	.append("GROUP BY statisticsDate                                         \n")
	.append("ORDER BY statisticsDate ASC"                                       )
	.toString();
	
	private static final String SQL_SECTION_ARTICLES_JOINS = new StringBuilder()
	.append("INNER JOIN News_PageContent ON News_PageContent.contentId = articleId \n")
	.append("INNER JOIN layout ON layout.uuid_ =  News_PageContent.layoutId          \n")
	.toString();
	
	
	private static final String SQL_GET_ABTESTING = new StringBuilder(
		"SELECT variantid, DATE_FORMAT(statisticsDate, '%s') statisticsDate, \n").append(
		"		prints, visits, extvisits													\n").append(
		"FROM %s																			\n").append(
		"	WHERE variantid IN (%%1$s)                        								\n").append(             
		"	AND statisticsDate >= '%%2$s' AND statisticsDate <= '%%3$s' 					\n").toString();
	
	private static final String SQL_GET_ABTESTING_ORDER 		= " ORDER BY variantid, statisticsDate ASC \n";
	private static final String SQL_GET_ABTESTING_BY_MINUTE		= String.format(SQL_GET_ABTESTING, "%%Y-%%m-%%d %%H:%%i:00", "abtesting_statistics");
	private static final String SQL_GET_ABTESTING_BY_MINUTE_NO_RT = SQL_GET_ABTESTING_BY_MINUTE.concat(SQL_GET_ABTESTING_ORDER);
	
	private static final String SQL_GET_ABTESTING_BY_HOUR 		= String.format(SQL_GET_ABTESTING, "%%Y-%%m-%%d %%H:00:00",  "abtesting_hourly_statistics").
			concat(SQL_GET_ABTESTING_ORDER);

	private static final String SQL_GET_ABTESTING_BY_DAY 		= String.format(SQL_GET_ABTESTING, "%%Y-%%m-%%d 00:00:00",  "abtesting_daily_statistics").
																	concat(SQL_GET_ABTESTING_ORDER);
	
	private static final String SQL_GET_ABTESTING_BY_MINUTE_RT 	= new StringBuilder(SQL_GET_ABTESTING_BY_MINUTE).append(
		"UNION ALL																									\n").append(
		"																											\n").append(					
		"SELECT variantid, DATE_FORMAT(now(), '%%Y-%%m-%%d %%H:%%i:00') statisticsDate,								\n").append(
		"		prints, visits, extvisits																			\n").append(
		"FROM abtesting_variant																						\n").append(
		"  WHERE variantid IN (%1$s)																				\n").append(                                    
		"    AND updatePending=1																					\n").append(SQL_GET_ABTESTING_ORDER).toString();

	
	private static final String SQL_GET_ARTICLE_VISIST_BY_MINUTE_NO_RT  = String.format(SQL_GET_VISIST_NO_RT, SQL_GET_READS_SELECT, TABLE_ARTICLE_STATISTICS_MINUTE, 	StringPool.BLANK);
	private static final String SQL_GET_ARTICLE_VISIST_BY_MINUTE_RT     = String.format(SQL_GET_VISIST_RT, 	  SQL_GET_READS_SELECT, TABLE_ARTICLE_STATISTICS_MINUTE, 	SQL_GET_READS_SELECT_RT,  TABLE_ARTICLE_STATISTICS_REAL_TIME, StringPool.BLANK);
	private static final String SQL_GET_ARTICLE_VISIST_BY_HOUR        	= String.format(SQL_GET_VISIST_NO_RT, SQL_GET_READS_SELECT, TABLE_ARTICLE_STATISTICS_HOUR, 		StringPool.BLANK);
	private static final String SQL_GET_ARTICLE_VISIST_BY_DAY         	= String.format(SQL_GET_VISIST_NO_RT, SQL_GET_READS_SELECT, TABLE_ARTICLE_STATISTICS_DAILY, 	StringPool.BLANK);
	private static final String SQL_GET_ARTICLE_VISIST_BY_MONTH       	= String.format(SQL_GET_VISIST_NO_RT, SQL_GET_READS_SELECT, TABLE_ARTICLE_STATISTICS_MONTHLY, 	StringPool.BLANK);
	
	private static final String SQL_GET_SECTION_ARTICLE_VISIST_BY_MINUTE_NO_RT  = String.format(SQL_GET_VISIST_NO_RT, SQL_GET_READS_SELECT, TABLE_ARTICLE_STATISTICS_MINUTE, 	SQL_SECTION_ARTICLES_JOINS);
	private static final String SQL_GET_SECTION_ARTICLE_VISIST_BY_MINUTE_RT     = String.format(SQL_GET_VISIST_RT, 	  SQL_GET_READS_SELECT, TABLE_ARTICLE_STATISTICS_MINUTE, 	SQL_GET_READS_SELECT_RT,  TABLE_ARTICLE_STATISTICS_REAL_TIME, SQL_SECTION_ARTICLES_JOINS);
	private static final String SQL_GET_SECTION_ARTICLE_VISIST_BY_HOUR        	= String.format(SQL_GET_VISIST_NO_RT, SQL_GET_READS_SELECT, TABLE_ARTICLE_STATISTICS_HOUR, 		SQL_SECTION_ARTICLES_JOINS);
	private static final String SQL_GET_SECTION_ARTICLE_VISIST_BY_DAY         	= String.format(SQL_GET_VISIST_NO_RT, SQL_GET_READS_SELECT, TABLE_ARTICLE_STATISTICS_DAILY, 	SQL_SECTION_ARTICLES_JOINS);
	private static final String SQL_GET_SECTION_ARTICLE_VISIST_BY_MONTH       	= String.format(SQL_GET_VISIST_NO_RT, SQL_GET_READS_SELECT, TABLE_ARTICLE_STATISTICS_MONTHLY, 	SQL_SECTION_ARTICLES_JOINS);
	
	private static final String SQL_GET_SECTION_VISIST_BY_MINUTE_NO_RT  = String.format(SQL_GET_VISIST_NO_RT, SQL_GET_VISITS_SELECT, TABLE_SECTION_STATISTICS_MINUTE, 	StringPool.BLANK);
	private static final String SQL_GET_SECTION_VISIST_BY_MINUTE_RT     = String.format(SQL_GET_VISIST_RT, 	  SQL_GET_VISITS_SELECT, TABLE_SECTION_STATISTICS_MINUTE, 	SQL_GET_VISITS_SELECT_RT,  TABLE_SECTION_STATISTICS_REAL_TIME, StringPool.BLANK);
	private static final String SQL_GET_SECTION_VISIST_BY_HOUR        	= String.format(SQL_GET_VISIST_NO_RT, SQL_GET_VISITS_SELECT, TABLE_SECTION_STATISTICS_HOUR, 	StringPool.BLANK);
	private static final String SQL_GET_SECTION_VISIST_BY_DAY         	= String.format(SQL_GET_VISIST_NO_RT, SQL_GET_VISITS_SELECT, TABLE_SECTION_STATISTICS_DAILY, 	StringPool.BLANK);
	private static final String SQL_GET_SECTION_VISIST_BY_MONTH       	= String.format(SQL_GET_VISIST_NO_RT, SQL_GET_VISITS_SELECT, TABLE_SECTION_STATISTICS_MONTHLY, 	StringPool.BLANK);

	private static final String SQL_GET_CATEGORY_VISIST_BY_MINUTE_NO_RT = String.format(SQL_GET_VISIST_NO_RT, SQL_GET_VISITS_SELECT, TABLE_METADATA_STATISTICS_MINUTE, 	StringPool.BLANK);
	private static final String SQL_GET_CATEGORY_VISIST_BY_MINUTE_RT    = String.format(SQL_GET_VISIST_RT,    SQL_GET_VISITS_SELECT,  TABLE_METADATA_STATISTICS_MINUTE, SQL_GET_VISITS_SELECT_RT,  TABLE_METADATA_STATISTICS_REAL_TIME, StringPool.BLANK);
	private static final String SQL_GET_CATEGORY_VISIST_BY_HOUR        	= String.format(SQL_GET_VISIST_NO_RT, SQL_GET_VISITS_SELECT, TABLE_METADATA_STATISTICS_HOUR, 	StringPool.BLANK);
	private static final String SQL_GET_CATEGORY_VISIST_BY_DAY        	= String.format(SQL_GET_VISIST_NO_RT, SQL_GET_VISITS_SELECT, TABLE_METADATA_STATISTICS_DAILY, 	StringPool.BLANK);
	private static final String SQL_GET_CATEGORY_VISIST_BY_MONTH      	= String.format(SQL_GET_VISIST_NO_RT, SQL_GET_VISITS_SELECT, TABLE_METADATA_STATISTICS_MONTHLY, StringPool.BLANK);
	
	private static final String SQL_FILTER_ARTICLE  = "AND articleId = '%s' ";
	private static final String SQL_FILTER_SECTION  = "AND plid = %d ";
	private static final String SQL_FILTER_CATEGORY = "AND categoryId = %d ";
	
	public static final String GET_TOTAL_ARTICLEVISITS_BY_GROUP		= new StringBuilder(
		"select articleid, visits		\n").append(
		"from article_visits			\n").append(
		"  where articleId in ('%s')	\n").append(
		"    and groupId = %d			\n").toString();
	
	public static String buildQuery(VisitsStatisticsRequest.StatisticItem item, VisitsStatisticsRequest request) throws ServiceError
	{
		String sql = null;
		// Construye el filtro por elemento si es necesario
		String filter = buildFilter(item, request);
		
		// Construye la query
		switch (request.getResolution())
		{
		case MINUTE:
			switch (item)
			{
			case ABTESTING:
					       if (request.isRealTime()) 	 sql = formatABTestingQuery(request, SQL_GET_ABTESTING_BY_MINUTE_RT,    	filter);
		                   else                       	 sql = formatABTestingQuery(request, SQL_GET_ABTESTING_BY_MINUTE_NO_RT, 	filter);
					       break;	
				
			case ARTICLE:  if (request.isSectionRequest())
						       if (request.isRealTime()) sql = formatQuery(request, SQL_GET_SECTION_ARTICLE_VISIST_BY_MINUTE_RT, 	filter);
		                       else                      sql = formatQuery(request, SQL_GET_SECTION_ARTICLE_VISIST_BY_MINUTE_NO_RT, filter);
			               else
							   if (request.isRealTime()) sql = formatQuery(request, SQL_GET_ARTICLE_VISIST_BY_MINUTE_RT, 			filter);
				               else                      sql = formatQuery(request, SQL_GET_ARTICLE_VISIST_BY_MINUTE_NO_RT, 		filter);
				           break;
				           
			case SECTION:  if (request.isRealTime()) 	 sql = formatQuery(request, SQL_GET_SECTION_VISIST_BY_MINUTE_RT, 			filter);
				           else                       	 sql = formatQuery(request, SQL_GET_SECTION_VISIST_BY_MINUTE_NO_RT, 		filter);
				           break;
				           
			case METADATA: if (request.isRealTime()) 	 sql = formatQuery(request, SQL_GET_CATEGORY_VISIST_BY_MINUTE_RT, 			filter);
				           else                      	 sql = formatQuery(request, SQL_GET_CATEGORY_VISIST_BY_MINUTE_NO_RT, 		filter);
				           break;
				           
			default: ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			}
			break;

		case HOUR:
			switch (item)
			{
			case ABTESTING:
		                   								 sql = formatABTestingQuery(request, SQL_GET_ABTESTING_BY_HOUR, 			filter);
					       break;	
				
			case ARTICLE:  if (request.isSectionRequest())
		                       							 sql = formatQuery(request, SQL_GET_SECTION_ARTICLE_VISIST_BY_HOUR,	 		filter);
			               else							 sql = formatQuery(request, SQL_GET_ARTICLE_VISIST_BY_HOUR,		 			filter);
				           break;
				           
			case SECTION:  								 sql = formatQuery(request, SQL_GET_SECTION_VISIST_BY_HOUR, 				filter);
				           break;
				           
			case METADATA: 								 sql = formatQuery(request, SQL_GET_CATEGORY_VISIST_BY_HOUR, 			    filter);
				           break;
				           
			default: ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			}
			break;
			
		case DAY:
			switch (item) 
			{
			case ABTESTING:
			       										 sql = formatABTestingQuery(request, SQL_GET_ABTESTING_BY_DAY,  		  	filter);
			       			break;	

			case ARTICLE:  if (request.isSectionRequest())
				               							 sql = formatQuery(request, SQL_GET_SECTION_ARTICLE_VISIST_BY_DAY, 			filter);
			               else                          sql = formatQuery(request, SQL_GET_ARTICLE_VISIST_BY_DAY, 					filter);
			               break;
			               
			case SECTION:  								 sql = formatQuery(request, SQL_GET_SECTION_VISIST_BY_DAY, 					filter); 
						   break;
						   
			case METADATA: 								 sql = formatQuery(request, SQL_GET_CATEGORY_VISIST_BY_DAY, 				filter); 
			               break;
			default: ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			}
			break;
			
		case MONTH:
			switch (item) 
			{
			case ARTICLE:  if (request.isSectionRequest())
				               							 sql = formatQuery(request, SQL_GET_SECTION_ARTICLE_VISIST_BY_MONTH, 		filter);
		                   else                          sql = formatQuery(request, SQL_GET_ARTICLE_VISIST_BY_MONTH, 				filter);
			               break;
			
			case SECTION:  								 sql = formatQuery(request, SQL_GET_SECTION_VISIST_BY_MONTH, 				filter); 
						   break;
						   
			case METADATA: 								 sql = formatQuery(request, SQL_GET_CATEGORY_VISIST_BY_MONTH, 				filter); 
						   break;
			default: ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			}
			break;
			
		default: ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}
		
		if (_log.isDebugEnabled())
			_log.debug(sql);
		
		return sql;
	}
	
	private static String buildFilter(VisitsStatisticsRequest.StatisticItem item, VisitsStatisticsRequest request) throws ServiceError
	{
		String filter = null;
		switch (item)
		{
		case ABTESTING:
			filter = request.getABTesting().getVariantIDs();
			break;
			
		case ARTICLE:
			filter = Validator.isNotNull(request.getArticleId()) ? String.format(SQL_FILTER_ARTICLE, request.getArticleId()) : StringPool.BLANK;
			if (request.isSectionRequest()) filter = filter.concat(String.format(SQL_FILTER_SECTION, request.getPlid()));
			break;
		case SECTION:
			filter = request.isSectionRequest() ? String.format(SQL_FILTER_SECTION, request.getPlid()) : StringPool.BLANK;
			break;
		case METADATA:
			filter = String.format(SQL_FILTER_CATEGORY, request.getCategoryId());
			break;

		default:
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}
		return filter;
	}
	
	private static String formatABTestingQuery(VisitsStatisticsRequest request, String queryPattern, String filter)
	{
		return String.format(queryPattern, filter, request.getSqlStartDate(), request.getSqlEndDate());	
	}
	private static String formatQuery(VisitsStatisticsRequest request, String queryPattern, String filter)
	{
		return	String.format(queryPattern, request.getGroupId(), filter, request.getSqlStartDate(), request.getSqlEndDate());
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	//                                                                        //
	//                 QUERY BUILDER PARA EL TOP DE ARTÍCULOS                 //
	//                                                                        //
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	
	private static final String SQL_GET_TOP_ARTICLES_GROUPED = new StringBuilder()
	.append(" SELECT p.articleId id, ja.title label, %%1$s FROM (        \n")
	.append("     SELECT stats.articleId, %%2$s                          \n")
	.append("     FROM %s stats                                          \n")
	.append("     %%3$s                                                  \n")
	.append("     WHERE stats.groupId = %%4$d %%5$s                      \n")
	.append("       AND stats.statisticsDate BETWEEN '%%6$s' AND '%%7$s' \n")
	.append("     %%8$s                                                  \n")
	.append("     ORDER BY %%9$s                                         \n")
	.append("     LIMIT %%10$d                                           \n")
	.append(" ) p                                                        \n")
	.append(" INNER JOIN journalarticle ja ON ja.articleid=p.articleid   \n")
	.append(" %%11$s                                                     \n")
	.append(" ORDER BY %%9$s, label ASC "                                   )
	.toString();
	
	private static final String SQL_GET_TOP_ARTICLES_REAL_TIME = new StringBuilder()
	.append(" SELECT p.articleId id, ja.title label, %%1$s FROM (        \n")
	.append("   SELECT articleId, %%2$s                                  \n")
	.append("   FROM (                                                   \n")
	.append("     SELECT stats.articleId, %%3$s                          \n")
	.append("     FROM %s stats                                          \n")
	.append("     %%4$s                                                  \n")
	.append("     WHERE stats.groupId = %%5$d %%6$s                      \n")
	.append("       AND stats.statisticsDate BETWEEN '%%7$s' AND '%%8$s' \n")
	.append("     UNION ALL                                              \n")
	.append("     SELECT stats.articleId, %%9$s                          \n")
	.append("	  FROM %s stats                                          \n")    
	.append("     %%4$s                                                  \n")                
	.append("	  WHERE updatePending = 1                                \n")
	.append("	    AND stats.groupId = %%5$d %%6$s                      \n")
	.append("   ) AS T                                                   \n")
	.append("   GROUP BY articleId                                       \n")
	.append("   ORDER BY %%10$s                                          \n")
	.append("   LIMIT %%11$d                                             \n")
	.append(" ) p                                                        \n")
	.append(" INNER JOIN journalarticle ja ON ja.articleid=p.articleid   \n")
	.append(" %%12$s                                                     \n")
	.append(" ORDER BY %%10$s, label ASC                                 \n")
	.toString();
	
	private static final String SQL_GET_TOP_ARTICLES_BY_LAST_MINUTE_RT = String.format(SQL_GET_TOP_ARTICLES_REAL_TIME, TABLE_ARTICLE_STATISTICS_MINUTE, TABLE_ARTICLE_STATISTICS_REAL_TIME);
	private static final String SQL_GET_TOP_ARTICLES_BY_LAST_MINUTE    = String.format(SQL_GET_TOP_ARTICLES_GROUPED, TABLE_ARTICLE_STATISTICS_MINUTE);
	private static final String SQL_GET_TOP_ARTICLES_BY_MINUTE_RT      = String.format(SQL_GET_TOP_ARTICLES_REAL_TIME, TABLE_ARTICLE_STATISTICS_MINUTE, TABLE_ARTICLE_STATISTICS_REAL_TIME);
	private static final String SQL_GET_TOP_ARTICLES_BY_MINUTE         = String.format(SQL_GET_TOP_ARTICLES_GROUPED, TABLE_ARTICLE_STATISTICS_HOUR);
	private static final String SQL_GET_TOP_ARTICLES_BY_HOUR           = String.format(SQL_GET_TOP_ARTICLES_GROUPED, TABLE_ARTICLE_STATISTICS_DAILY);
	private static final String SQL_GET_TOP_ARTICLES_BY_DAY            = String.format(SQL_GET_TOP_ARTICLES_GROUPED, TABLE_ARTICLE_STATISTICS_MONTHLY);
	private static final String SQL_GET_TOP_ARTICLES_BY_MONTH          = String.format(SQL_GET_TOP_ARTICLES_GROUPED, TABLE_ARTICLE_STATISTICS_ANNUAL);
	
	private static final String SQL_GET_TOP_ARTICLES_SECTION_JOINS = new StringBuilder()
	.append(" INNER JOIN News_PageContent ON News_PageContent.contentId = stats.articleId \n")
	.append("       INNER JOIN layout ON layout.uuid_ =  News_PageContent.layoutId     \n")
	.toString();
	
	private static final String SQL_GET_TOP_ARTICLES_METADATA_JOINS = new StringBuilder()
	.append(" INNER JOIN JournalArticle                ON stats.articleId                           = JournalArticle.articleId              \n")
	.append("       INNER JOIN AssetEntry                    ON JournalArticle.resourcePrimKey          = AssetEntry.classPK                    \n")
	.append("       INNER JOIN AssetEntries_AssetCategories  ON AssetEntry.entryId                      = AssetEntries_AssetCategories.entryId  \n")
	.append("       INNER JOIN AssetCategory				 ON AssetEntries_AssetCategories.categoryId = AssetCategory.categoryId                \n")
	.toString();
	
	public static String buildTopArticleQuery(VisitsStatisticsRequest request) throws ServiceError
	{
		String tableJoins = StringPool.BLANK;
		String sectionFilter = StringPool.BLANK;

		if (request.isMetadataRequest())
		{
			tableJoins = SQL_GET_TOP_ARTICLES_METADATA_JOINS;
			sectionFilter = String.format("AND AssetCategory.categoryId = %d", request.getCategoryId());
		}
		else if (request.isSectionRequest())
		{
			tableJoins = SQL_GET_TOP_ARTICLES_SECTION_JOINS;
			sectionFilter = String.format("AND layout.plid = %d", request.getPlid());
		}

		String datasets        = StringPool.BLANK;
		String agregateFields  = StringPool.BLANK;
		String lastHoursFields = StringPool.BLANK;
		String fields          = StringPool.BLANK;
		String realTimeFields  = StringPool.BLANK;
		String criteria        = StringPool.BLANK;
		String topFilter       = StringPool.BLANK;
		if ("visits".equals(request.getCriteria()) || "readings".equals(request.getCriteria()))
		{
			datasets       = "dataset1, dataset2";
			agregateFields = "SUM(dataset1) dataset1, SUM(dataset2) dataset2";
			lastHoursFields= "SUM(visits) dataset1, SUM(readings) dataset2";
			fields         = "visits dataset1, readings dataset2";
			realTimeFields = "visits - lastUpdateVisits dataset1, readings - lastUpdateReadings dataset2";
			criteria       = "visits".equals(request.getCriteria()) ? "dataset1 DESC, dataset2 DESC" : "dataset2 DESC, dataset1 DESC";
			topFilter      = "visits".equals(request.getCriteria()) ? "WHERE dataset1 > 0" : "WHERE dataset2 > 0";
		}
		else if ("sharings".equals(request.getCriteria()))
		{
			datasets       = "dataset1";
			agregateFields = "SUM(dataset1) dataset1";
			lastHoursFields= "SUM(sharings) dataset1";
			fields         = "sharings dataset1";
			realTimeFields = "sharings - lastUpdateSharings dataset1";
			criteria       = "dataset1 DESC";
			topFilter      = "WHERE dataset1 > 0";
		}
		else if ("comments".equals(request.getCriteria()))
		{
			datasets       = "dataset1";
			agregateFields = "SUM(dataset1) dataset1";
			lastHoursFields= "SUM(comments) dataset1";
			fields         = "comments dataset1";
			realTimeFields = "comments - lastUpdateComments dataset1";
			criteria       = "dataset1 DESC";
			topFilter      = "WHERE dataset1 > 0";
		}
		else if ("feedback".equals(request.getCriteria()))
		{
			datasets       = "dataset1";
			lastHoursFields= "TRUNCATE(AVG(qualification), 2) dataset1 ";
			fields         = "qualification dataset1";
			sectionFilter  = sectionFilter.concat(" AND qualification > 0");
			criteria       = "dataset1 DESC";
			topFilter      = "WHERE dataset1 > 0";
		}
		
		String sql = null;
		switch (request.getResolution()) {
		case MINUTE:
			// Si es un ranking de valoración de usuarios, no se aplica tiempo real
			if ("feedback".equals(request.getCriteria()))
			{
				if (request.isRealTimeDay())
					sql = String.format(SQL_GET_TOP_ARTICLES_BY_LAST_MINUTE, datasets, lastHoursFields, tableJoins, request.getGroupId(), sectionFilter, request.getSqlStartDate(), request.getSqlEndDate(), "GROUP BY articleId", criteria, request.getMaxItems(), topFilter);
				else
					sql = String.format(SQL_GET_TOP_ARTICLES_BY_MINUTE, 	 datasets, fields, tableJoins, request.getGroupId(), sectionFilter, request.getSqlStartDate(), request.getSqlEndDate(), StringPool.BLANK, criteria, request.getMaxItems(), topFilter);
			}
			// El resto de rankings aplican tiempo real
			else
			{
				if (request.isRealTimeDay())
				{
					if (request.isRealTime()) sql = String.format(SQL_GET_TOP_ARTICLES_BY_LAST_MINUTE_RT, 	datasets, agregateFields, fields, tableJoins, request.getGroupId(), sectionFilter, request.getSqlStartDate(), request.getSqlEndDate(), realTimeFields, criteria, request.getMaxItems(), topFilter);
					else                      sql = String.format(SQL_GET_TOP_ARTICLES_BY_LAST_MINUTE, 		datasets, lastHoursFields, tableJoins, request.getGroupId(), sectionFilter, request.getSqlStartDate(), request.getSqlEndDate(), "GROUP BY articleId", criteria, request.getMaxItems(), topFilter);
				}
				else
				{
					if (request.isRealTime()) sql = String.format(SQL_GET_TOP_ARTICLES_BY_MINUTE_RT, 	datasets, agregateFields, fields, tableJoins, request.getGroupId(), sectionFilter, request.getSqlStartDate(), request.getSqlEndDate(), realTimeFields, criteria, request.getMaxItems(), topFilter);
					else                      sql = String.format(SQL_GET_TOP_ARTICLES_BY_MINUTE, 		datasets, fields, tableJoins, request.getGroupId(), sectionFilter, request.getSqlStartDate(), request.getSqlEndDate(), StringPool.BLANK, criteria, request.getMaxItems(), topFilter);
				}
			}
			break;
		case HOUR: 	sql = String.format(SQL_GET_TOP_ARTICLES_BY_HOUR, 	datasets, fields, tableJoins, request.getGroupId(), sectionFilter, request.getSqlStartDate(), request.getSqlEndDate(), StringPool.BLANK, criteria, request.getMaxItems(), topFilter); break;
		case DAY: 	sql = String.format(SQL_GET_TOP_ARTICLES_BY_DAY, 	datasets, fields, tableJoins, request.getGroupId(), sectionFilter, request.getSqlStartDate(), request.getSqlEndDate(), StringPool.BLANK, criteria, request.getMaxItems(), topFilter); break;
		case MONTH: sql = String.format(SQL_GET_TOP_ARTICLES_BY_MONTH, 	datasets, fields, tableJoins, request.getGroupId(), sectionFilter, request.getSqlStartDate(), request.getSqlEndDate(), StringPool.BLANK, criteria, request.getMaxItems(), topFilter); break;
		default:
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}
		
		if (_log.isDebugEnabled())
			_log.debug(sql);
		
		return sql;
	}
	
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	//                                                                        //
	//                 QUERY BUILDER PARA EL TOP DE SECCIONES                 //
	//                                                                        //
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	
	private static final String SQL_GET_TOP_SECTIONS_GROUPED = new StringBuilder()
	.append(" SELECT p.plid id,                                                       \n")
	.append(" friendlyUrl label,                                                      \n")
	.append(" visits dataset1,                                                        \n")
	.append(" articleVisits dataset2                                                  \n")
	.append(" FROM (                                                                  \n")
	.append("     SELECT plid, %%1$s                                                  \n")
	.append("     FROM %s stats                                                       \n")
	.append("     WHERE stats.groupId = %%2$d                                         \n")
	.append("       AND stats.statisticsDate BETWEEN '%%3$s' AND '%%4$s'              \n")
	.append("     %%5$s                                                               \n")
	.append("     ORDER BY visits + articleVisits DESC                                \n")
	.append("     LIMIT %%6$d                                                         \n")
	.append(" ) p                                                                     \n")
	.append(" INNER JOIN layout l ON l.plid=p.plid                                    \n")
	.append(" ORDER BY dataset1 + dataset2 DESC, label ASC                            \n")
	.toString();
	
	private static final String SQL_GET_TOP_SECTIONS_REAL_TIME = new StringBuilder()
	.append(" SELECT p.plid id,                                                       \n")
	.append(" friendlyUrl label,                                                      \n")
	.append(" visits dataset1,                                                        \n")
	.append(" articleVisits dataset2                                                  \n")
	.append(" FROM (                                                                  \n")
	.append("     SELECT plid, SUM(visits) visits, SUM(articleVisits) articleVisits   \n")
	.append("     FROM (                                                              \n")
	.append("       SELECT plid, SUM(visits) visits, SUM(articleVisits) articleVisits \n")
	.append("       FROM %s stats                                                     \n")
	.append("       WHERE stats.groupId = %%1$d                                       \n")
	.append("         AND stats.statisticsDate BETWEEN '%%2$s' AND '%%3$s'            \n")
	.append("       GROUP BY stats.groupId, plid                                      \n")
	.append("       UNION ALL                                                         \n")
	.append("       SELECT plid,                                                      \n")
	.append("              visits - lastUpdateVisits visits,                          \n")
	.append("              articleVisits - lastUpdateArticleVisits articleVisits      \n")
	.append("       FROM section_visits sv                                            \n")
	.append("       WHERE updatePending = 1                                           \n")
	.append("         AND sv.groupId = %%1$d                                          \n")
	.append("     ) AS T                                                              \n")
	.append("     GROUP BY plid                                                       \n")
	.append("     ORDER BY visits + articleVisits DESC                                \n")
	.append("     LIMIT %%4$d                                                         \n")
	.append(" ) p                                                                     \n")
	.append(" INNER JOIN layout l ON l.plid=p.plid                                    \n")
	.append(" ORDER BY dataset1 + dataset2 DESC, label ASC                            \n")
	.toString();

	private static final String SQL_GET_TOP_SECTIONS_BY_LAST_MINUTE_RT 	= String.format(SQL_GET_TOP_SECTIONS_REAL_TIME, TABLE_SECTION_STATISTICS_MINUTE);
	private static final String SQL_GET_TOP_SECTIONS_BY_LAST_MINUTE  	= String.format(SQL_GET_TOP_SECTIONS_GROUPED,   TABLE_SECTION_STATISTICS_MINUTE);
	private static final String SQL_GET_TOP_SECTIONS_BY_MINUTE_RT    	= String.format(SQL_GET_TOP_SECTIONS_REAL_TIME, TABLE_SECTION_STATISTICS_HOUR);
	private static final String SQL_GET_TOP_SECTIONS_BY_MINUTE_NO_RT 	= String.format(SQL_GET_TOP_SECTIONS_GROUPED,   TABLE_SECTION_STATISTICS_HOUR);
	private static final String SQL_GET_TOP_SECTIONS_BY_HOUR         	= String.format(SQL_GET_TOP_SECTIONS_GROUPED,   TABLE_SECTION_STATISTICS_DAILY);
	private static final String SQL_GET_TOP_SECTIONS_BY_DAY          	= String.format(SQL_GET_TOP_SECTIONS_GROUPED,   TABLE_SECTION_STATISTICS_MONTHLY);
	private static final String SQL_GET_TOP_SECTIONS_BY_MONTH        	= String.format(SQL_GET_TOP_SECTIONS_GROUPED,   TABLE_SECTION_STATISTICS_ANNUAL);
	
	public static String buildTopSectionsQuery(VisitsStatisticsRequest request) throws ServiceError
	{
		String sql = null;
		String fields = "visits, articleVisits";
		String groupBy = StringPool.BLANK;
		
		if (Resolution.HOUR.equals(request.getResolution()) && request.isRealTimeDay() && !request.isRealTime())
		{
			fields = "SUM(visits) visits, SUM(articleVisits) articleVisits";
			groupBy = "GROUP BY groupId, plid";
		}
		
		switch (request.getResolution()) {
		case MINUTE:
			if (request.isRealTimeDay())
			{
				if (request.isRealTime()) sql = String.format(SQL_GET_TOP_SECTIONS_BY_LAST_MINUTE_RT, 		request.getGroupId(), request.getSqlStartDate(), request.getSqlEndDate(), request.getMaxItems());
				else                      sql = String.format(SQL_GET_TOP_SECTIONS_BY_LAST_MINUTE, fields, 	request.getGroupId(), request.getSqlStartDate(), request.getSqlEndDate(), groupBy, request.getMaxItems());
			}
			else
			{
				if (request.isRealTime()) sql = String.format(SQL_GET_TOP_SECTIONS_BY_MINUTE_RT, 			request.getGroupId(), request.getSqlStartDate(), request.getSqlEndDate(), request.getMaxItems());
				else                      sql = String.format(SQL_GET_TOP_SECTIONS_BY_MINUTE_NO_RT, fields, request.getGroupId(), request.getSqlStartDate(), request.getSqlEndDate(), groupBy, request.getMaxItems());
			}
			break;
		case HOUR: 	sql = String.format(SQL_GET_TOP_SECTIONS_BY_HOUR, 	fields, request.getGroupId(), request.getSqlStartDate(), request.getSqlEndDate(), groupBy, request.getMaxItems()); break;
		case DAY: 	sql = String.format(SQL_GET_TOP_SECTIONS_BY_DAY, 	fields, request.getGroupId(), request.getSqlStartDate(), request.getSqlEndDate(), groupBy, request.getMaxItems()); break;
		case MONTH: sql = String.format(SQL_GET_TOP_SECTIONS_BY_MONTH, 	fields, request.getGroupId(), request.getSqlStartDate(), request.getSqlEndDate(), groupBy, request.getMaxItems()); break;
		default:
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}
		
		if (_log.isDebugEnabled())
			_log.debug(sql);
		
		return sql;
	}
	
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	//                                                                        //
	//                 QUERY BUILDER PARA EL TOP DE METADATOS                 //
	//                                                                        //
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	
	private static final String SQL_GET_TOP_CATEGORIES_GROUPED = new StringBuilder()
	.append(" SELECT assetcategory.categoryId id, assetcategory.name label, visits dataset1 \n")
	.append(" FROM %1$s                                                                     \n")
	.append(" INNER JOIN assetcategory ON assetcategory.categoryId = %1$s.categoryId        \n")
	.append("        AND assetcategory.vocabularyId = %%2$d                                 \n")
	.append("        AND %1$s.groupId = %%1$d                                               \n")
	.append("        AND statisticsDate BETWEEN '%%3$s' AND '%%4$s'                         \n")
	.append(" ORDER BY dataset1 DESC, label ASC "                                              )
	.toString();
	
	private static final String SQL_GET_TOP_CATEGORIES_NO_REAL_TIME = new StringBuilder()
	.append(" SELECT assetcategory.categoryId id, assetcategory.name label, SUM(visits) dataset1 \n")
	.append(" FROM %1$s                                                                          \n")
	.append(" INNER JOIN assetcategory ON assetcategory.categoryId = %1$s.categoryId             \n")
	.append("        AND assetcategory.vocabularyId = %%2$d                                      \n")
	.append("        AND %1$s.groupId = %%1$d                                                    \n")
	.append("        AND statisticsDate BETWEEN '%%3$s' AND '%%4$s'                              \n")
	.append(" GROUP BY id                                                                        \n")
	.append(" ORDER BY dataset1 DESC, label ASC "                                                   )
	.toString();
	
	private static final String SQL_GET_TOP_CATEGORIES_REAL_TIME = new StringBuilder()
	.append(" SELECT categoryId id, name label, SUM(visits) dataset1                                  \n")
	.append(" FROM (                                                                                  \n")
	.append("   SELECT assetcategory.categoryId, assetcategory.name, visits                           \n")
	.append("   FROM %s mvs                                                                           \n")
	.append("   INNER JOIN assetcategory ON assetcategory.categoryId = mvs.categoryId                 \n")
	.append("          AND assetcategory.vocabularyId = %%2$d                                         \n")
	.append("          AND mvs.groupId = %%1$d                                                        \n")
	.append("          AND statisticsDate BETWEEN '%%3$s' AND '%%4$s'                                 \n")
	.append("   UNION ALL                                                                             \n")
	.append("   SELECT assetcategory.categoryId, assetcategory.name, visits - lastUpdateVisits visits \n")
	.append("   FROM metadata_visits                                                                  \n")
	.append("   INNER JOIN assetcategory ON assetcategory.categoryId = metadata_visits.categoryId     \n")
	.append("     AND assetcategory.vocabularyId = %%2$d                                              \n")
	.append("     AND metadata_visits.updatePending = 1                                               \n")
	.append("     AND metadata_visits.groupId = %%1$d                                                 \n")
	.append(" ) AS T                                                                                  \n")
	.append(" GROUP BY id                                                                             \n")
	.append(" ORDER BY dataset1 DESC, label ASC "                                                        )
	.toString();

	private static final String SQL_GET_TOP_CATEGORIES_BY_MINUTE_RT    	= String.format(SQL_GET_TOP_CATEGORIES_REAL_TIME, 		TABLE_METADATA_STATISTICS_MINUTE);
	private static final String SQL_GET_TOP_CATEGORIES_BY_MINUTE_NO_RT 	= String.format(SQL_GET_TOP_CATEGORIES_NO_REAL_TIME, 	TABLE_METADATA_STATISTICS_MINUTE);
	private static final String SQL_GET_TOP_CATEGORIES_BY_HOUR       	= String.format(SQL_GET_TOP_CATEGORIES_GROUPED, TABLE_METADATA_STATISTICS_DAILY);
	private static final String SQL_GET_TOP_CATEGORIES_BY_DAY        	= String.format(SQL_GET_TOP_CATEGORIES_GROUPED, TABLE_METADATA_STATISTICS_MONTHLY);
	private static final String SQL_GET_TOP_CATEGORIES_BY_MONTH      	= String.format(SQL_GET_TOP_CATEGORIES_GROUPED, TABLE_METADATA_STATISTICS_ANNUAL);
	
	private static final String SQL_GET_TOP_CATEGORIES_LIMIT = new StringBuilder(" LIMIT %d").toString();
	
	public static String buildTopCategoriesQuery(VisitsStatisticsRequest request, long vocabularyId, int limit) throws ServiceError
	{
		String sql = null;
		
		switch (request.getResolution()) {
		case MINUTE:
			if (request.isRealTime())
				sql = String.format(SQL_GET_TOP_CATEGORIES_BY_MINUTE_RT, request.getGroupId(), vocabularyId, request.getSqlStartDate(), request.getSqlEndDate());
			else
				sql = String.format(SQL_GET_TOP_CATEGORIES_BY_MINUTE_NO_RT, request.getGroupId(), vocabularyId, request.getSqlStartDate(), request.getSqlEndDate());
			break;
		case HOUR:
			sql = String.format(SQL_GET_TOP_CATEGORIES_BY_HOUR, 	request.getGroupId(), vocabularyId, request.getSqlStartDate(), request.getSqlEndDate());
			break;
		case DAY:
			sql = String.format(SQL_GET_TOP_CATEGORIES_BY_DAY, 		request.getGroupId(), vocabularyId, request.getSqlStartDate(), request.getSqlEndDate());
			break;
		case MONTH:
			sql = String.format(SQL_GET_TOP_CATEGORIES_BY_MONTH, 	request.getGroupId(), vocabularyId, request.getSqlStartDate(), request.getSqlEndDate());
			break;
		default:
			break;
		}
		
		// Aplica el límite
		if (limit > 0)
			sql = sql.concat(String.format(SQL_GET_TOP_CATEGORIES_LIMIT, limit));
		
		if (_log.isDebugEnabled())
			_log.debug(sql);
		
		return sql;
	}
}
