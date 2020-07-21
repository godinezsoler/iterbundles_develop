package com.protecmedia.iter.base.metrics;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.model.VisitsStatisticsRequest;

public class UserMetricsUtil
{
	private static Log _log = LogFactoryUtil.getLog(UserMetricsUtil.class);
	
	public enum Hit {REGISTRATION, SOCIAL_REGISTRATION, RECOVERY, PROFILE_EDIT, BINDING, DELETE}
	
	public static void Hit(long groupId, Hit type)
	{	
		try
		{
			Map<String, Long> metricData = initData(groupId);
			
			switch (type)
			{
				case REGISTRATION:        metricData.put(METRIC_REGISTRATIONS,           1L); break;
				case SOCIAL_REGISTRATION: metricData.put(METRIC_SOCIAL_REGISTRATIONS,    1L); break;
				case RECOVERY:            metricData.put(METRIC_RECOVERIES,              1L); break;
				case PROFILE_EDIT:        metricData.put(METRIC_PROFILE_MODIFICATIONS,   1L); break;
				case BINDING:             metricData.put(METRIC_SOCIAL_ACCOUNT_BINDINGS, 1L); break;
				case DELETE:              metricData.put(METRIC_ACCOUNT_DELETES,         1L); break;
			}
			
			registerUserMetrics(metricData);
		}
		catch (ServiceError e)
		{
			// Parámetros incorrectos
			_log.error(e);
		}
		catch (Throwable th)
		{
			// Error en SQL
			_log.error(th);
		}
	}
	
	private static Map<String, Long> initData(long groupId) throws ServiceError
	{
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Map<String, Long> metricData = new HashMap<String, Long>();
		metricData.put(METRIC_GROUP_ID, groupId);
		
		return metricData;
	}
	
	private static final Object writeAccessLock = new Object();

	public static final String METRIC_GROUP_ID                = "groupId";
	public static final String METRIC_REGISTRATIONS           = "registrations";
	public static final String METRIC_SOCIAL_REGISTRATIONS    = "socialRegistrations";
	public static final String METRIC_RECOVERIES              = "recoveries";
	public static final String METRIC_PROFILE_MODIFICATIONS   = "profileModifications";
	public static final String METRIC_SOCIAL_ACCOUNT_BINDINGS = "socialAccountBindings";
	public static final String METRIC_ACCOUNT_DELETES         = "accountDeletes";
	
	private static final String SQL_INSERT_USER_METRICS = new StringBuilder()
	.append("INSERT INTO user_metrics (groupId, registrations, socialRegistrations, recoveries,     \n")
	.append("                          profileModifications, socialAccountBindings, accountDeletes, \n")
	.append("                          modifiedDate, updatePending) VALUES                          \n")
	.append("(%d, %d, %d, %d, %d, %d, %d, NOW(), 1)                                                 \n")
	.append("ON DUPLICATE KEY UPDATE                                                                \n")
	.append("registrations = registrations + VALUES(registrations),                                 \n")
	.append("socialRegistrations = socialRegistrations + VALUES(socialRegistrations),               \n")
	.append("recoveries = recoveries + VALUES(recoveries),                                          \n")
	.append("profileModifications = profileModifications + VALUES(profileModifications),            \n")
	.append("socialAccountBindings = socialAccountBindings + VALUES(socialAccountBindings),         \n")
	.append("accountDeletes = accountDeletes + VALUES(accountDeletes),                              \n")
	.append("modifiedDate = NOW(), updatePending = 1 ")
	.toString();
	
	public static void registerUserMetrics(Map<String, Long> metrics) throws ServiceError, IOException, SQLException
	{
		// Procesa los datos de entrada
		String sql = processInputData(metrics);
		
		// Serializa el acceso
		synchronized (writeAccessLock)
		{
			_log.info("Updating user metrics");
			// Actualiza los datos
			PortalLocalServiceUtil.executeUpdateQuery(sql);
		}
	}
	
	private static String processInputData(Map<String, Long> metrics) throws ServiceError
	{
		// Valida la entrada
		ErrorRaiser.throwIfFalse(Validator.isNotNull(metrics) && metrics.size() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		long groupId = GetterUtil.getLong(metrics.get(METRIC_GROUP_ID));
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		long registrations = GetterUtil.getLong(metrics.get(METRIC_REGISTRATIONS));
		long socialRegistrations = GetterUtil.getLong(metrics.get(METRIC_SOCIAL_REGISTRATIONS));
		long recoveries = GetterUtil.getLong(metrics.get(METRIC_RECOVERIES));
		
		long profileModifications = GetterUtil.getLong(metrics.get(METRIC_PROFILE_MODIFICATIONS));
		long socialAccountBindings = GetterUtil.getLong(metrics.get(METRIC_SOCIAL_ACCOUNT_BINDINGS));
		long accountDeletes = GetterUtil.getLong(metrics.get(METRIC_ACCOUNT_DELETES));
		
		ErrorRaiser.throwIfFalse(registrations          > 0 || socialRegistrations   > 0 || recoveries     > 0 ||
                                 profileModifications   > 0 || socialAccountBindings > 0 || accountDeletes > 0,
                                 IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		// Crea la query
		return String.format(SQL_INSERT_USER_METRICS, groupId, registrations, socialRegistrations, recoveries, profileModifications, socialAccountBindings, accountDeletes);
	}
	
	/////////////////////////////////////////////////////////////
	// RECUPERACION DE DATOS DE ESTADISTICAS PARA LOS GRAFICOS //
	/////////////////////////////////////////////////////////////
	
	private static final String SQL_GET_METRICS = new StringBuilder()
	.append("SELECT statisticsDate, registrations, socialRegistrations, recoveries, \n")
	.append("profileModifications, socialAccountBindings, accountDeletes,           \n")
	.append("favorites, favoriteTopics, suggestedArticles                           \n")
	.append("FROM %s                                  \n")
	.append("WHERE groupId = %d                       \n")
	.append("AND statisticsDate BETWEEN '%s' AND '%s' \n")
	.append("ORDER BY statisticsDate ASC")
	.toString();
	
	private static final String SQL_GET_REAL_TIME_METRICS = new StringBuilder()
	.append("SELECT statisticsDate,                                                             \n")
	.append("       SUM(registrations) registrations,                                           \n")
	.append("       SUM(socialRegistrations) socialRegistrations,                               \n")
	.append("       SUM(recoveries) recoveries,                                                 \n")
	.append("       SUM(profileModifications) profileModifications,                             \n")
	.append("       SUM(socialAccountBindings) socialAccountBindings,                           \n")
	.append("       SUM(accountDeletes) accountDeletes,                                         \n")
	.append("       MAX(favorites) favorites,                                                   \n")
	.append("       MAX(favoriteTopics) favoriteTopics,                                         \n")
	.append("       MAX(suggestedArticles) suggestedArticles                                    \n")
	.append("FROM (                                                                             \n")
	.append("    %1$s                                                                           \n")
	.append("    UNION ALL                                                                      \n")
	.append("    SELECT                                                                         \n")
	.append("    DATE_FORMAT(NOW(), '%2$s') statisticsDate,                                     \n")
	.append("    registrations - lastUpdateRegistrations registrations,                         \n")
	.append("    socialRegistrations - lastUpdateSocialRegistrations socialRegistrations,       \n")
	.append("    recoveries - lastUpdateRecoveries recoveries,                                  \n")
	.append("    profileModifications - lastUpdateProfileModifications profileModifications,    \n")
	.append("    socialAccountBindings - lastUpdateSocialAccountBindings socialAccountBindings, \n")
	.append("    accountDeletes - lastUpdateAccountDeletes accountDeletes,                      \n")
	.append("    (SELECT count(*) FROM favorite_articles WHERE groupid = %3$d) favorites,       \n")
	.append("    (SELECT count(id) FROM favorite_topics WHERE groupid = %3$d AND categoryid > 0)\n")
	.append("    favoritesTopics,                                                               \n")
	.append("    (SELECT count(pa.id) FROM favorite_topics ft                                   \n")
	.append("    INNER JOIN pending_articles pa ON pa.favoritetopicid = ft.id                   \n")
	.append("    WHERE groupid = %3$d AND categoryid > 0) suggestedArticles                     \n")
	.append("    FROM user_metrics                                                              \n")
	.append("    WHERE groupId = %3$d                                                           \n")
	.append(") t                                                                                \n")
	.append("GROUP BY statisticsDate                                                            \n")
	.append("ORDER BY statisticsDate ASC"                                                          )
	.toString();
	
	private static final String SQL_GET_FAVORITE_RESUME = new StringBuilder()
	.append("SELECT                             \n")
	.append("(                                  \n")
	.append("    SELECT COUNT(usrid)            \n")
	.append("    FROM iterusers                 \n")
	.append("    WHERE delegationid = %2$d      \n")
	.append("      AND registerdate <= '%3$s'   \n")
	.append(") totalUsers,                      \n")
	.append("(                                  \n")
	.append("    SELECT COUNT(DISTINCT(userid)) \n")
	.append("    FROM favorite_articles         \n")
	.append("    WHERE groupid = %1$d           \n")
	.append("      AND added <= '%3$s'          \n")
	.append(") usersUsingFavorites"                )
	.toString();
	
	public static JSONObject getMetrics(VisitsStatisticsRequest request) throws SecurityException, NoSuchMethodException, ServiceError, PortalException, SystemException
	{
		String table = null;
		DateFormat df = null;
		DateFormat dfLabel = null;
		String dateFormat = null;
		int interval = 0;
		
		switch (request.getResolution()) {
		case HOUR:
			table = "user_metrics_statistics";
			df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			dfLabel = new SimpleDateFormat("HH:00'h'");
			dateFormat = "%Y-%m-%d %H:00:00";
			interval = Calendar.HOUR_OF_DAY;
			break;
		case DAY:
			table = "user_metrics_daily_statistics";
			df = new SimpleDateFormat("yyyy-MM-dd");
			dfLabel = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat = "%Y-%m-%d 00:00:00";
			interval = Calendar.DAY_OF_MONTH;
			break;
		case MONTH:
			table = "user_metrics_monthly_statistics";
			df = new SimpleDateFormat("yyyy-MM");
			dfLabel = new SimpleDateFormat("yyyy-MM");
			dateFormat = "%Y-%m-01 00:00:00";
			interval = Calendar.MONTH;
			break;
		default:
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		}
		
		String sql = String.format(SQL_GET_METRICS, table, request.getGroupId(), request.getSqlStartDate(), request.getSqlEndDate());
		if (request.isRealTime())
			sql = String.format(SQL_GET_REAL_TIME_METRICS, sql.substring(0, sql.lastIndexOf("\n")-1), dateFormat, request.getGroupId());

		// Recupera las métricas para la fecha y grupación indicadas
		Document d = PortalLocalServiceUtil.executeQueryAsDom(sql);
		
		Calendar fromDate = (Calendar) request.getStartDate().clone();
		Calendar toDate = (Calendar) request.getEndDate().clone();
		
		JSONObject jsonUsersMetricsData = JSONFactoryUtil.createJSONObject();
		JSONArray jsonDates = JSONFactoryUtil.createJSONArray();
		JSONArray jsonRegistrations = JSONFactoryUtil.createJSONArray();
		long resumeRegistrations = 0;
		JSONArray jsonSocialRegistrations = JSONFactoryUtil.createJSONArray();
		long resumeSocialRegistrations = 0;
		JSONArray jsonRecoveries = JSONFactoryUtil.createJSONArray();
		long resumeRecoveries = 0;
		JSONArray jsonProfileModifications = JSONFactoryUtil.createJSONArray();
		long resumeProfileModifications = 0;
		JSONArray jsonAccountBindigns = JSONFactoryUtil.createJSONArray();
		long resumeAccountBindings = 0;
		JSONArray jsonAccountDeletes = JSONFactoryUtil.createJSONArray();
		long resumeAccountDeletes = 0;
		
		JSONObject jsonFavoritesMetricsData = JSONFactoryUtil.createJSONObject();
		JSONArray jsonFavorites = JSONFactoryUtil.createJSONArray();
		JSONArray jsonFavoriteTopics = JSONFactoryUtil.createJSONArray();
		JSONArray jsonSuggestedArticles = JSONFactoryUtil.createJSONArray();
		
		for (Node n : d.selectNodes("/rs/row"))
		{
			Calendar currentDate = Calendar.getInstance();
			try
			{
				currentDate.setTime(df.parse(XMLHelper.getStringValueOf(n, "@statisticsDate")));
			}
			catch (ParseException e)
			{
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
			}
			
			if (!toDate.before(currentDate))
			{
				
			}
			
			if (!toDate.before(currentDate))
			{
				// Si faltan horas, las rellena a 0
				while (fromDate.before(currentDate))
				{
					jsonDates.put(dfLabel.format(fromDate.getTime()));
					jsonRegistrations.put(0);
					jsonSocialRegistrations.put(0);
					jsonRecoveries.put(0);
					jsonProfileModifications.put(0);
					jsonAccountBindigns.put(0);
					jsonAccountDeletes.put(0);
					jsonFavorites.put(0);
					jsonFavoriteTopics.put(0);
					jsonSuggestedArticles.put(0);
					fromDate.add(interval, 1);
				}
				
				// Rellena las estadísticas de visitas
				jsonDates.put(dfLabel.format(fromDate.getTime()));
				
				jsonRegistrations.put(XMLHelper.getLongValueOf(n, "@registrations"));
				resumeRegistrations += XMLHelper.getLongValueOf(n, "@registrations");
				
				jsonSocialRegistrations.put(XMLHelper.getLongValueOf(n, "@socialRegistrations"));
				resumeSocialRegistrations += XMLHelper.getLongValueOf(n, "@socialRegistrations");
				
				jsonRecoveries.put(XMLHelper.getLongValueOf(n, "@recoveries"));
				resumeRecoveries += XMLHelper.getLongValueOf(n, "@recoveries");
				
				jsonProfileModifications.put(XMLHelper.getLongValueOf(n, "@profileModifications"));
				resumeProfileModifications += XMLHelper.getLongValueOf(n, "@profileModifications");
				
				jsonAccountBindigns.put(XMLHelper.getLongValueOf(n, "@socialAccountBindings"));
				resumeAccountBindings += XMLHelper.getLongValueOf(n, "@socialAccountBindings");
				
				jsonAccountDeletes.put(XMLHelper.getLongValueOf(n, "@accountDeletes"));
				resumeAccountDeletes += XMLHelper.getLongValueOf(n, "@accountDeletes");

				jsonFavorites.put(XMLHelper.getLongValueOf(n, "@favorites"));
				jsonFavoriteTopics.put(XMLHelper.getLongValueOf(n, "@favoriteTopics"));
				jsonSuggestedArticles.put(XMLHelper.getLongValueOf(n, "@suggestedArticles"));
				
				fromDate.add(interval, 1);
			}
			
		}
		
		// Si faltan horas, las rellena a 0
		while (!fromDate.after(toDate))
		{
			jsonDates.put(dfLabel.format(fromDate.getTime()));
			jsonRegistrations.put(0);
			jsonSocialRegistrations.put(0);
			jsonRecoveries.put(0);
			jsonProfileModifications.put(0);
			jsonAccountBindigns.put(0);
			jsonAccountDeletes.put(0);
			jsonFavorites.put(0);
			jsonFavoriteTopics.put(0);
			jsonSuggestedArticles.put(0);
			fromDate.add(interval, 1);
		}

		// Métricas de acciones de usuarios
		jsonUsersMetricsData.put("labels", jsonDates);
		jsonUsersMetricsData.put("registrations", jsonRegistrations);
		jsonUsersMetricsData.put("socialRegistrations", jsonSocialRegistrations);
		jsonUsersMetricsData.put("recoveries", jsonRecoveries);
		jsonUsersMetricsData.put("profileModifications", jsonProfileModifications);
		jsonUsersMetricsData.put("socialAccountBindings", jsonAccountBindigns);
		jsonUsersMetricsData.put("accountDeletes", jsonAccountDeletes);
		
		// Metricas para el resumen
		JSONObject jsonUsersMetricsResume = JSONFactoryUtil.createJSONObject();
		jsonUsersMetricsResume.put("registrations", resumeRegistrations);
		jsonUsersMetricsResume.put("socialRegistrations", resumeSocialRegistrations);
		jsonUsersMetricsResume.put("recoveries", resumeRecoveries);
		jsonUsersMetricsResume.put("profileModifications", resumeProfileModifications);
		jsonUsersMetricsResume.put("socialAccountBindings", resumeAccountBindings);
		jsonUsersMetricsResume.put("accountDeletes", resumeAccountDeletes);
		
		// Métricas de favoritos
		jsonFavoritesMetricsData.put("resume", getFavoriteResume(request));
		JSONObject jsonFavoriteTrend = JSONFactoryUtil.createJSONObject();
		jsonFavoriteTrend.put("labels", jsonDates);
		jsonFavoriteTrend.put("articles", jsonFavorites);
		jsonFavoritesMetricsData.put("trend", jsonFavoriteTrend);
		
		JSONObject jsonFavoriteTopicsTrend = JSONFactoryUtil.createJSONObject();
		jsonFavoriteTopicsTrend.put("labels", jsonDates);
		jsonFavoriteTopicsTrend.put("topics", jsonFavoriteTopics);
		jsonFavoritesMetricsData.put("topicsTrend", jsonFavoriteTopicsTrend);
		
		JSONObject jsonSuggestedArticlesTrend = JSONFactoryUtil.createJSONObject();
		jsonSuggestedArticlesTrend.put("labels", jsonDates);
		jsonSuggestedArticlesTrend.put("articles", jsonSuggestedArticles);
		jsonFavoritesMetricsData.put("suggestionsTrend", jsonSuggestedArticlesTrend);
		
		// Compone el resultado
		JSONObject jsonUsersMetrics = JSONFactoryUtil.createJSONObject();
		jsonUsersMetrics.put("data", jsonUsersMetricsData);
		jsonUsersMetrics.put("resume", jsonUsersMetricsResume);
		jsonUsersMetrics.put("bindings", getSocialBindings(request));
		jsonUsersMetrics.put("favorites", jsonFavoritesMetricsData);
		
		return jsonUsersMetrics;
	}
	
	private static JSONObject getFavoriteResume(VisitsStatisticsRequest request) throws NoSuchMethodException, SecurityException
	{
		JSONObject jsonFavoriteResume = JSONFactoryUtil.createJSONObject();
		
		String sql = String.format(SQL_GET_FAVORITE_RESUME, request.getGroupId(), request.getDelegationId(), request.getSqlEndDate());
		Document d = PortalLocalServiceUtil.executeQueryAsDom(sql);
		
		jsonFavoriteResume.put("total", XMLHelper.getLongValueOf(d, "/rs/row/@totalUsers"));
		jsonFavoriteResume.put("usingFavorites", XMLHelper.getLongValueOf(d, "/rs/row/@usersUsingFavorites"));
		
		return jsonFavoriteResume;
	}
	
	private static JSONObject getSocialBindings(VisitsStatisticsRequest request) throws PortalException, SystemException, SecurityException, NoSuchMethodException
	{
		String sql = new StringBuilder()
		.append("SELECT label, data FROM (                                                                                                     \n")
		.append("SELECT 'Disqus'   label, count(disqusid)     data FROM iterusers WHERE delegationid = %1$d AND registerdate IS NOT NULL UNION \n")
		.append("SELECT 'Facebook' label, count(facebookid)   data FROM iterusers WHERE delegationid = %1$d AND registerdate IS NOT NULL UNION \n")
		.append("SELECT 'Twitter'  label, count(twitterid)    data FROM iterusers WHERE delegationid = %1$d AND registerdate IS NOT NULL UNION \n")
		.append("SELECT 'Google+'  label, count(googleplusid) data FROM iterusers WHERE delegationid = %1$d AND registerdate IS NOT NULL UNION \n")
		.append("SELECT 'None'     label, count(usrid)        data FROM iterusers WHERE delegationid = %1$d AND registerdate IS NOT NULL       \n")
		.append("AND ISNULL(disqusid) AND ISNULL(facebookid) AND ISNULL(googleplusid) AND ISNULL(twitterid)                                    \n")
		.append(") t ORDER BY data DESC, label ASC"                                                                                               )
		.toString();
		
		long delegationId = GroupLocalServiceUtil.getGroup(request.getGroupId()).getDelegationId();
		Document d = PortalLocalServiceUtil.executeQueryAsDom(String.format(sql, delegationId));

		JSONArray jsonUsersSocialAccountsLabels = JSONFactoryUtil.createJSONArray();
		JSONArray jsonUsersSocialAccountsValues = JSONFactoryUtil.createJSONArray();
		JSONArray jsonUsersSocialAccountsColors = JSONFactoryUtil.createJSONArray();
		for (Node n : d.selectNodes("/rs/row"))
		{
			String label = XMLHelper.getStringValueOf(n, "@label");
			if ("None".equals(label))
				label = GetterUtil.getString(GroupConfigTools.getGroupConfigXMLField(request.getGroupId(), "userstatistics", "/users/texts/withoutSocialBinding/text()"),  "Ninguna");
			
			     if ("Disqus".equals(label))   jsonUsersSocialAccountsColors.put("rgba(46, 159, 255, 1)");
			else if ("Facebook".equals(label)) jsonUsersSocialAccountsColors.put("rgba(59, 89, 152, 1)");
			else if ("Twitter".equals(label))  jsonUsersSocialAccountsColors.put("rgba(0, 176, 237, 1)");
			else if ("Google+".equals(label))  jsonUsersSocialAccountsColors.put("rgba(220, 78, 65, 1)");
			else                               jsonUsersSocialAccountsColors.put("rgba(200, 200, 200, 1)");
			
			jsonUsersSocialAccountsLabels.put(label);
			jsonUsersSocialAccountsValues.put(XMLHelper.getStringValueOf(n, "@data"));
		}
		
		JSONObject jsonUsersSocialAccounts = JSONFactoryUtil.createJSONObject();
		jsonUsersSocialAccounts.put("labels", jsonUsersSocialAccountsLabels);
		jsonUsersSocialAccounts.put("data", jsonUsersSocialAccountsValues);
		jsonUsersSocialAccounts.put("colors", jsonUsersSocialAccountsColors);
		return jsonUsersSocialAccounts;
	}
	
	////////////////////////////////////////////////
	// CONFIGURACION DE LOS TEXTOS DE LA INTERFAZ //
	////////////////////////////////////////////////
	public static String getUIConfiguration(long groupId) throws Exception
	{
		JSONObject jsonUI = JSONFactoryUtil.createJSONObject();
		jsonUI.put("metrics", getMetricsConfiguration(groupId));
		jsonUI.put("titles", getTitlesConfiguration(groupId));
		jsonUI.put("texts", getTextsConfiguration(groupId));
		jsonUI.put("thousandSeparator", getThousandSeparatorConfiguration(groupId));
		return jsonUI.toString();
	}
	
	private static JSONObject getMetricsConfiguration(long groupId) throws Exception
	{
		
		JSONObject jsonMetrics = JSONFactoryUtil.createJSONObject();
		
		JSONObject jsonMetric = JSONFactoryUtil.createJSONObject();
		jsonMetric.put("title", GetterUtil.getString(GroupConfigTools.getGroupConfigXMLField(groupId, "userstatistics", "/users/metrics/registrations/title/text()"), "Registros desde el sitio"));
		jsonMetric.put("color", getColor(groupId, "registrations", "#3FBFBF"));
		jsonMetrics.put("registrations", jsonMetric);
		
		jsonMetric = JSONFactoryUtil.createJSONObject();
		jsonMetric.put("title", GetterUtil.getString(GroupConfigTools.getGroupConfigXMLField(groupId, "userstatistics", "/users/metrics/socialRegistrations/title/text()"), "Registros desde Redes Sociales"));
		jsonMetric.put("color", getColor(groupId, "socialRegistrations", "#3FBF3F"));
		jsonMetrics.put("socialRegistrations", jsonMetric);
		
		jsonMetric = JSONFactoryUtil.createJSONObject();
		jsonMetric.put("title", GetterUtil.getString(GroupConfigTools.getGroupConfigXMLField(groupId, "userstatistics", "/users/metrics/profileModifications/title/text()"), "Modificaciones del perfil"));
		jsonMetric.put("color", getColor(groupId, "profileModifications", "#2196F3"));
		jsonMetrics.put("profileModifications", jsonMetric);
		
		jsonMetric = JSONFactoryUtil.createJSONObject();
		jsonMetric.put("title", GetterUtil.getString(GroupConfigTools.getGroupConfigXMLField(groupId, "userstatistics", "/users/metrics/socialAccountBindings/title/text()"), "Vinculación con Redes Sociales"));
		jsonMetric.put("color", getColor(groupId, "socialAccountBindings", "#97BBCD"));
		jsonMetrics.put("socialAccountBindings", jsonMetric);

		jsonMetric = JSONFactoryUtil.createJSONObject();
		jsonMetric.put("title", GetterUtil.getString(GroupConfigTools.getGroupConfigXMLField(groupId, "userstatistics", "/users/metrics/recoveries/title/text()"), "Restablecimiento de credenciales"));
		jsonMetric.put("color", getColor(groupId, "recoveries", "#BF7F3F"));
		jsonMetrics.put("recoveries", jsonMetric);
		
		jsonMetric = JSONFactoryUtil.createJSONObject();
		jsonMetric.put("title", GetterUtil.getString(GroupConfigTools.getGroupConfigXMLField(groupId, "userstatistics", "/users/metrics/accountDeletes/title/text()"), "Bajas de usuarios"));
		jsonMetric.put("color", getColor(groupId, "accountDeletes", "#C14242"));
		jsonMetrics.put("accountDeletes", jsonMetric);
		
		return jsonMetrics;
	}
	
	private static String getColor(long groupId, String metric, String defaultValue)
	{
		String color = GetterUtil.getString(GroupConfigTools.getGroupConfigXMLField(groupId, "userstatistics", "/users/metrics/" + metric + "/color/text()"), defaultValue);
		if (!color.startsWith(StringPool.POUND))
			return StringPool.POUND + color;
		return color;
	}
	
	private static JSONObject getTitlesConfiguration(long groupId) throws Exception
	{
		JSONObject jsonTitles = JSONFactoryUtil.createJSONObject();
		jsonTitles.put("registrationComparisonTitle", GetterUtil.getString(GroupConfigTools.getGroupConfigXMLField(groupId, "userstatistics", "/users/titles/registrationComparisonTitle/text()"),  "Origen de los registros"));
		jsonTitles.put("socialAccountsRankingTitle", GetterUtil.getString(GroupConfigTools.getGroupConfigXMLField(groupId, "userstatistics", "/users/titles/socialAccountsRankingTitle/text()"),  "Vinculación de redes sociales"));
		jsonTitles.put("favoritesResumeTitle", "Usuarios usando favoritos");
		jsonTitles.put("favoritesTrendTitle", "Artículos añadidos como favoritos");
		jsonTitles.put("favoriteTopicsTrendTitle", "Temas añadidos como favoritos");
		jsonTitles.put("suggestedArticlesTrendTitle", "Artículos sugeridos a los usuarios");
		return jsonTitles;
	}
	
	private static JSONObject getTextsConfiguration(long groupId) throws Exception
	{
		JSONObject jsonTexts = JSONFactoryUtil.createJSONObject();
		jsonTexts.put("rankingTooltip", GetterUtil.getString(GroupConfigTools.getGroupConfigXMLField(groupId, "userstatistics", "/users/texts/userAccounts/text()"),  "Usuarios"));
		jsonTexts.put("favoriteResumeTotalTooltip", "Usuarios sin favoritos guardados");
		jsonTexts.put("favoriteResumeUsingTooltip", "Usuarios con favoritos guardados");
		jsonTexts.put("favoriteTrendTooltip", "Artículos");
		jsonTexts.put("favoriteTopicsTrendTooltip", "Temas");
		jsonTexts.put("suggestedArticlesTrendTooltip", "Artículos");
		return jsonTexts;
	}
	
	private static String getThousandSeparatorConfiguration(long groupId) throws Exception
	{
		String thousandSeparator = GetterUtil.getString( GroupConfigTools.getGroupConfigXMLField(groupId, "userstatistics", "/visits/@thousandseparator"), StringPool.BLANK);
		return thousandSeparator.equals("point") ? StringPool.PERIOD : thousandSeparator.equals("comma") ? StringPool.COMMA : StringPool.BLANK;
	}
	
	//////////////////////////////////////////////////
	//         CONFIGURACION INTERADMIN             //
	//////////////////////////////////////////////////
	
	/**
	* Recupera la configuración de las estadísticas de usuarios de la caché.
	* Si no existe configuración para el grupo, retorna nulo.
	* 
	* @param groupId	 ID del grupo.
	* @return			 xml de configuración de la group_config.
	* @throws Exception Si los datos de entrada son incorrectos.
	*/
	public String getUsersStatisticsConfig(long groupId) throws Exception
	{
		// Valida los parámetros de entrada
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Recupera la configuración
		return GroupConfigTools.getGroupConfigField(groupId, "userstatistics");
	}
	
	/**
	* Guarda la configuración de estadísticas de usuarios en la tabla group_config y
	* actualiza la caché de configuraciones de grupos de los Tomcats.
	* 
	* @param groupId	 ID del grupo.
	* @param data		 xml de configuración a guardar en la group_config.
	* @throws Exception Si los datos de entrada son incorrectos u ocurre algún error
	* 					 durante el guardado de la configuración. 
	*/
	public void setUsersStatisticsConfig(long groupId, String data) throws Exception
	{
		// Valida los parámetros de entrada
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(data, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		// Guarda la configuración
		GroupConfigTools.setGroupConfigField(groupId, "userstatistics", data);
	}
}
