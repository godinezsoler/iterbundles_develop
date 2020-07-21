package com.protecmedia.iter.base.metrics;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.model.VisitsStatisticsRequest;

public class NewslettersMetricsUtil
{
	private static Log _log = LogFactoryUtil.getLog(NewslettersMetricsUtil.class);
	
	
	/////////////////////////////////////////////////////////////
	//                                                         //
	// UTILIDADES PARA EL REGISTRO DE EVENTOS DE LOS USUARIOS  //
	//                                                         //
	/////////////////////////////////////////////////////////////
	
	
	public enum HIT
	{
		USR_SUBSCRIPTION(101), USR_CANCEL_SUBSCRIPTION(102), USR_CANCEL_ACCOUNT(103),
		ADMIN_SUBSCRIPTION(201), ADMIN_CANCEL_SUBSCRIPTION(202), ADMIN_CANCEL_ACCOUNT(203),
		ADMIN_ENABLED_SUBSCRIPTION(204), ADMIN_DISABLED_SUBSCRIPTION(205), AUTO_DISABLED_SUBSCRIPTION(305);
		
		private final int value;
		
		HIT(final int newValue) { value = newValue; }
		public int valueOf() { return value; }
		public boolean equals(int i){return value == i;}
		public boolean isGainedEvent() { return value == 101 || value == 201 || value == 204; }
		
		public static HIT get(int id)
        {
			HIT[] HITs = HIT.values();
            for(int i = 0; i < HITs.length; i++)
            {
                if(HITs[i].equals(id))
                    return HITs[i];
            }
            return null;
        }
	}
	
	private static final String SQL_SELECT_USERS = new StringBuilder()
	.append("        SELECT usrid FROM iterusers \n")
	.append("        WHERE usrid IN (%s)         \n")
	.toString();
	
	public static void hit(String scheduleIds, String userIds, HIT type)
	{	
		try
		{
			for (String scheduleId : scheduleIds.split(StringPool.COMMA))
			{
				if (!scheduleId.contains(StringPool.APOSTROPHE))
					scheduleId = StringUtil.apostrophe(scheduleId);
				registerUserMetrics(scheduleId, String.format(SQL_SELECT_USERS, userIds), type);
			}
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
	
	/** Registro de suscripciones masivas */
	private static final String SQL_SELECT_NOT_SUBSCRIBED_USERS = new StringBuilder()
	.append("SELECT usrid                                     \n")
	.append("FROM iterusers iu                                \n")
	.append("LEFT JOIN schedule_user su ON su.scheduleId = %s \n")
	.append("         AND su.userId = iu.usrId                \n")
	.append("WHERE su.scheduleId IS NULL                      \n")
	.append("  AND iu.usrId IN (                              \n")
	.append("      %s                                         \n")
	.append("  )                                              \n")
	.toString();
	
	/** Registro de activaciones masivas */
	private static final String SQL_SELECT_DISABLED_USERS = new StringBuilder()
	.append("SELECT usrid                                     \n")
	.append("FROM iterusers iu                                \n")
	.append("LEFT JOIN schedule_user su ON su.scheduleId = %s \n")
	.append("         AND su.userId = iu.usrId                \n")
	.append("WHERE su.enabled=0                               \n")
	.append("  AND iu.usrId IN (                              \n")
	.append("      %s                                         \n")
	.append("  )                                              \n")
	.toString();
	
	public static void multipleUsersSubscriptionHit(String scheduleIds, String userIdsSelect)
	{	
		try
		{
			for (String scheduleId : scheduleIds.split(StringPool.COMMA))
			{
				// Registra ADMIN_SUBSCRIPTION a aquellos usuarios que no estén ya suscritos
				registerUserMetrics(scheduleId, String.format(SQL_SELECT_NOT_SUBSCRIBED_USERS, scheduleId, userIdsSelect), HIT.ADMIN_SUBSCRIPTION);
				// Registra ADMIN_ENABLED_SUBSCRIPTION a aquellos usuarios que estén ya suscritos pero desactivados
				registerUserMetrics(scheduleId, String.format(SQL_SELECT_DISABLED_USERS, scheduleId, userIdsSelect), HIT.ADMIN_ENABLED_SUBSCRIPTION);
			}
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
	
	/** Registro de evento indicado en todas las newsletters en las que están suscritos los usuarios indicados */
	private static final String SQL_CREATE_EVENTS_ON_ALL_USERS_NEWSLETTERS = new StringBuilder()
	.append("SELECT su.scheduleId, u.usrid userId, NOW() date, %2$d event        \n")
	.append("FROM iterusers u INNER JOIN schedule_user su ON su.userId = u.usrid \n")
	.append("WHERE usrid IN (%1$s) \n")
	.toString();
	
	/**
	 *  Realiza un hit en todas las newsletter en las que está suscrito cada usuario,
	 *  independientemente de que estén activados o no.
	 *  
	 *  @param userIds son los IDs de los usuarios separados por coma.
	 *  @param event   es el tipo de evento a registrar.
	 */
	public static void allUsersNewslettersHit(String userIds, HIT event)
	{
		String[] users = userIds.split(StringPool.COMMA);
		String sql_uses = userIds.contains(StringPool.APOSTROPHE) ? StringUtil.merge(users, StringPool.COMMA) : StringUtil.merge(users, StringPool.COMMA, StringPool.APOSTROPHE);
		String sql_inserts = String.format(SQL_CREATE_EVENTS_ON_ALL_USERS_NEWSLETTERS, sql_uses, event.valueOf());
		String sql = String.format(SQL_INSERT_NEWSLETTER_METRICS, sql_inserts);
		
		try
		{
			// Serializa el acceso
			synchronized (writeAccessLock)
			{
				_log.info("Updating newsletter metrics");
				// Actualiza los datos
				PortalLocalServiceUtil.executeUpdateQuery(sql);
			}
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
	}
	
	private static final Object writeAccessLock = new Object();
	
	private static final String SQL_INSERT_NEWSLETTER_METRICS = new StringBuilder()
	.append("INSERT INTO newsletter_statistics (scheduleId, userId, date, event) \n")
	.append("(                                                                   \n")
	.append("%s                                                                  \n")
	.append(")                                                                   \n")
	.append("ON DUPLICATE KEY UPDATE event = VALUES(event) "                        )
	.toString();

	private static final String	SQL_INSERT_VALUES = new StringBuilder()
	.append("    SELECT %1$s scheduleid, usrid userid, \n")
	.append("           NOW() date, %3$s type FROM     \n")
	.append("    (                                     \n")
	.append("        %2$s                              \n") // Usuarios
	.append("    ) X                                   \n")
	.toString();
	
	public static void registerUserMetrics(String scheduleId, String userIds, HIT type) throws ServiceError, IOException, SQLException
	{
		// Procesa los datos de entrada
		String sql = processInputData(scheduleId, userIds, type);
		
		// Serializa el acceso
		synchronized (writeAccessLock)
		{
			_log.info("Updating newsletter metrics");
			// Actualiza los datos
			PortalLocalServiceUtil.executeUpdateQuery(sql);
		}
	}
	
	private static String processInputData(String scheduleId, String userIds, HIT type) throws ServiceError
	{
		// Valida la entrada
		ErrorRaiser.throwIfNull(scheduleId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(userIds, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(type, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Crea la query
		String sqlInsertValues = String.format(SQL_INSERT_VALUES, scheduleId, userIds, type.valueOf());
		return String.format(SQL_INSERT_NEWSLETTER_METRICS, sqlInsertValues);
	}
	
	
	/////////////////////////////////////////////////////////////
	//                                                         //
	// RECUPERACION DE DATOS DE ESTADISTICAS PARA LOS GRAFICOS //
	//                                                         //
	/////////////////////////////////////////////////////////////
	
	
	private static final String SQL_GET_SCHEDULE_METRICS = new StringBuilder()
	.append("SELECT date, subscribers, users  \n")
	.append("FROM newsletter_daily_report     \n")
	.append("WHERE scheduleid = '%s'          \n")
	.append("  AND date BETWEEN '%s' AND '%s' \n")
	.append("ORDER BY date ASC "                 )
	.toString();
	
	public static JSONObject getMetrics(VisitsStatisticsRequest request) throws SecurityException, NoSuchMethodException, ServiceError, PortalException, SystemException
	{
		String sql = String.format(SQL_GET_SCHEDULE_METRICS, request.getScheduleId(), request.getSqlStartDate(), request.getSqlEndDate());
		// Recupera las métricas para la fecha y grupación indicadas
		Document d = PortalLocalServiceUtil.executeQueryAsDom(sql);
		Calendar fromDate = (Calendar) request.getStartDate().clone();
		Calendar toDate = (Calendar) request.getEndDate().clone();
		
		JSONObject jsonNewsletterMetricsData = generateReportData(d, fromDate, toDate);

		JSONObject jsonNewslettersMetrics = JSONFactoryUtil.createJSONObject();
		jsonNewslettersMetrics.put("data", jsonNewsletterMetricsData);
		return jsonNewslettersMetrics;
	}
	
	private static JSONObject generateReportData(Document data, Calendar fromDate, Calendar toDate) throws ServiceError
	{
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		
		JSONObject jsonNewsletterMetricsData = JSONFactoryUtil.createJSONObject();
		JSONArray jsonDates       = JSONFactoryUtil.createJSONArray();
		JSONArray jsonSubscribers = JSONFactoryUtil.createJSONArray();
		JSONArray jsonUsers       = JSONFactoryUtil.createJSONArray();
		
		for (Node n : data.selectNodes("/rs/row"))
		{
			Calendar currentDate = getCurrentDate(n, df);
			
			if (!toDate.before(currentDate))
			{
				// Si faltan dias, los rellena a 0
				while (fromDate.before(currentDate))
				{
					jsonDates.put(df.format(fromDate.getTime()));
					jsonSubscribers.put(0);
					jsonUsers.put(0);
					fromDate.add(Calendar.DAY_OF_MONTH, 1);
				}
				
				// Rellena las estadísticas de visitas
				jsonDates.put(df.format(fromDate.getTime()));

				jsonSubscribers.put(XMLHelper.getLongValueOf(n, "@subscribers"));
				jsonUsers.put(XMLHelper.getLongValueOf(n, "@users"));
				
				fromDate.add(Calendar.DAY_OF_MONTH, 1);
			}
			
		}
		
		// Si faltan horas, las rellena a 0
		while (!fromDate.after(toDate))
		{
			jsonDates.put(df.format(fromDate.getTime()));
			jsonSubscribers.put(0);
			jsonUsers.put(0);
			fromDate.add(Calendar.DAY_OF_MONTH, 1);
		}

		jsonNewsletterMetricsData.put("labels", jsonDates);
		jsonNewsletterMetricsData.put("subscribers", jsonSubscribers);
		jsonNewsletterMetricsData.put("users", jsonUsers);
		
		return jsonNewsletterMetricsData;
	}
	
	private static Calendar getCurrentDate(Node n, DateFormat df) throws ServiceError
	{
		Calendar currentDate = Calendar.getInstance();
		try
		{
			currentDate.setTime(df.parse(XMLHelper.getStringValueOf(n, "@date")));
		}
		catch (ParseException e)
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
		}
		return currentDate;
	}
	
	
	/////////////////////////////////////////////////////////////
	//                                                         //
	//  SERVICIOS PARA EL INFORME DE NEWSLETTERS DE ITERADMIN  //
	//                                                         //
	/////////////////////////////////////////////////////////////
	
	
	public static String getNewsletterReport(String scheduleId, String fromDate, String toDate) throws DocumentException, ServiceError, ParseException, SecurityException, NoSuchMethodException
	{
		Document newsletter = SAXReaderUtil.read("<newsletter></newsletter>");
		
		generateXMLReportData(newsletter.getRootElement(), scheduleId, fromDate, toDate);
		getNewsletterUsersReport(newsletter.getRootElement(), scheduleId, fromDate, toDate);
		
		return newsletter.asXML();
	}
	
	private static void generateXMLReportData(Element newsletter, String scheduleId, String fromDate, String toDate) throws ServiceError, SecurityException, NoSuchMethodException, ParseException
	{
		String sql = String.format(SQL_GET_SCHEDULE_METRICS, scheduleId, fromDate, toDate);
		Document d = PortalLocalServiceUtil.executeQueryAsDom(sql);
		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Calendar fromCalendar = Calendar.getInstance();
		fromCalendar.setTime(df.parse(fromDate));
		Calendar toCalendar = Calendar.getInstance();
		toCalendar.setTime(df.parse(toDate));
		JSONObject jsonReport = generateReportData(d, fromCalendar, toCalendar);
		JSONArray jsonDates       = jsonReport.getJSONArray("labels");
		JSONArray jsonSubscribers = jsonReport.getJSONArray("subscribers");
		JSONArray jsonUsers       = jsonReport.getJSONArray("users");
		
		Element report = newsletter.addElement("report");
		
		for (int i = jsonDates.length() - 1; i >= 0; --i)
		{
			Element day = report.addElement("day");
			day.addAttribute("date",        jsonDates.getString(i));
			day.addAttribute("subscribers", jsonSubscribers.getString(i));
			day.addAttribute("users",       jsonUsers.getString(i));
		}
	}
	
	/**
	 * Recupera la última acción de un usuario en el periodo indicado
	 */
	private static final String SQL_GET_LAST_EVENT_BY_USER = new StringBuilder()
	.append("SELECT IFNULL(A.userId, B.userId) userId, P.event previous,            \n")
	.append("A.event first, B.event last, C.gain, C.loss, C.total                   \n")
    .append("FROM                                                                   \n")
	.append("(                                                                      \n")
	.append("SELECT ns1.userId, ns1.event                                           \n")
	.append("FROM newsletter_statistics ns1                                         \n")
	.append("LEFT JOIN newsletter_statistics ns2                                    \n")
	.append("    ON ns1.scheduleId = ns2.scheduleId                                 \n")
	.append("   AND ns1.userId = ns2.userId                                         \n")
	.append("   AND ns1.date < ns2.date                                             \n")
	.append("   AND ns2.date < '%2$s'                                               \n")
	.append("WHERE ns1.scheduleId = '%1$s'                                          \n")
	.append("  AND ns1.date < '%2$s'                                                \n")
	.append("  AND ns2.scheduleId IS NULL                                           \n")
	.append(") P                                                                    \n")
	.append("RIGHT JOIN                                                             \n")
	.append("(                                                                      \n")
	.append("SELECT ns1.userId, ns1.event                                           \n")
	.append("FROM newsletter_statistics ns1                                         \n")
	.append("LEFT JOIN newsletter_statistics ns2                                    \n")
	.append("    ON ns1.scheduleId = ns2.scheduleId                                 \n")
	.append("   AND ns1.userId = ns2.userId                                         \n")
	.append("   AND ns1.date > ns2.date                                             \n")
	.append("   AND ns2.date BETWEEN '%2$s' AND '%3$s'                              \n")
	.append("WHERE ns1.scheduleId = '%1$s'                                          \n")
	.append("  AND ns1.date BETWEEN '%2$s' AND '%3$s'                               \n")
	.append("  AND ns2.scheduleId IS NULL                                           \n")
	.append(") A                                                                    \n")
	.append("ON P.userId = A.userId                                                 \n")
	.append("INNER JOIN                                                             \n")
	.append("(                                                                      \n")
	.append("SELECT ns1.userId, ns1.event                                           \n")
	.append("FROM newsletter_statistics ns1                                         \n")
	.append("LEFT JOIN newsletter_statistics ns2                                    \n")
	.append("    ON ns1.scheduleId = ns2.scheduleId                                 \n")
	.append("   AND ns1.userId = ns2.userId                                         \n")
	.append("   AND ns1.date < ns2.date                                             \n")
	.append("   AND ns2.date BETWEEN '%2$s' AND '%3$s'                              \n")
	.append("WHERE ns1.scheduleId = '%1$s'                                          \n")
	.append("  AND ns1.date BETWEEN '%2$s' AND '%3$s'                               \n")
	.append("  AND ns2.scheduleId IS NULL                                           \n")
	.append(")B                                                                     \n")
	.append("ON A.userId = B.userId                                                 \n")
	.append("INNER JOIN                                                             \n")
	.append("(                                                                      \n")
	.append("SELECT userId,                                                         \n")
	.append("       SUM(IF(event IN (101, 201, 204), 1, 0)) AS gain,                \n")
	.append("       SUM(IF(event IN (102, 103, 202, 203, 205, 305), 1, 0)) AS loss, \n")
	.append("       COUNT(userId) AS total                                          \n")
	.append("FROM newsletter_statistics                                             \n")
	.append("WHERE scheduleId = '%1$s'                                              \n")
	.append("  AND date BETWEEN '%2$s' AND '%3$s'                                   \n")
	.append("GROUP BY userId                                                        \n")
	.append(") C                                                                    \n")
	.append("ON A.userId = C.userId                                                 \n")
	.toString();
	
	private static void getNewsletterUsersReport(Element newsletter, String scheduleId, String dateFrom, String dateTo) throws DocumentException, SecurityException, NoSuchMethodException
	{
		Element users = newsletter.addElement("users");
		Element gainedUsers = users.addElement("gained");
		Element lostUsers = users.addElement("lost");
		Element convertedUsers = users.addElement("converted");
		Element bouncedUsers = users.addElement("bounced");
		
		String sql = String.format(SQL_GET_LAST_EVENT_BY_USER, scheduleId, dateFrom, dateTo);
		Document xmlReport = PortalLocalServiceUtil.executeQueryAsDom(sql);
		
		for (Node u : xmlReport.selectNodes("/rs/row"))
		{
			String userId = XMLHelper.getStringValueOf(u, "@userId");               // Id del usuario
			HIT previous = HIT.get((int) XMLHelper.getLongValueOf(u, "@previous")); // Último evento antes del informe
			HIT first    = HIT.get((int) XMLHelper.getLongValueOf(u, "@first"));    // Primer evento del informe
			HIT last     = HIT.get((int) XMLHelper.getLongValueOf(u, "@last"));     // Último evento del informe
			long gain    = XMLHelper.getLongValueOf(u, "@gain");                    // Nº de eventos de 'ganancia'
			long loss    = XMLHelper.getLongValueOf(u, "@loss");                    // Nº de eventos de 'pérdida'
			
			// Si sólo hay eventos de ganancia o de pérdida...
			if (gain == 0 || loss == 0)
			{
				// Si sólo hay eventos de ganancia, es un suscriptor ganado (ya que no es posible suscribirse en estado 'deshabilitado', el usuario
				// no debía estar suscrito o su suscripción estaba deshabilitada.)
				if (gain > 0)
				{
					gainedUsers.addElement("user").addAttribute("usrid", userId);
				}
				// Si sólo ha eventos de pérdida y no hay registrados eventos anteriores (se supone que estaba suscrito y activo) o el último evento
				// fue de ganancia, es un suscriptor perdido
				else if (null == previous || previous.isGainedEvent())
				{
					lostUsers.addElement("user").addAttribute("usrid", userId);
				}
				// En cualquier otro caso, no se consideran cambios (e.g. El último evento previo fue una desactivación y en el periodo del informe
				// se ha cancelado la suscripción. El usuario ya se había perdido antes de las fechas del informe.)
			}
			// Si hay eventos de varios tipos...
			else
			{
				// Lo primero que hizo ha sido una ganancia y al final del informe sigue suscrito. Es un usuario ganado.
				if (first.isGainedEvent() && last.isGainedEvent())
				{
					gainedUsers.addElement("user").addAttribute("usrid", userId);
				}
				// Lo primero que hizo ha sido una ganancia pero al final del informe no está suscrito. Es un usuario rebotado.
				else if (first.isGainedEvent() && !last.isGainedEvent())
				{
					bouncedUsers.addElement("user").addAttribute("usrid", userId);
				}
				// Lo primero que ha hecho ha sido una pérdida...
				else if (!first.isGainedEvent())
				{
					// Si antes estaba suscrito...
					if (null == previous || previous.isGainedEvent())
					{
						// ...y al final del informe está suscrito, ha sido un converso.
						if (last.isGainedEvent())
							convertedUsers.addElement("user").addAttribute("usrid", userId);
						// ... y al final del informe no está suscrito, es un usuario perdido.
						else
							lostUsers.addElement("user").addAttribute("usrid", userId);
					}
					// Si ya estaba deshabilitado pero al final del informe está suscrito, es un usuario ganado.
					else if (last.isGainedEvent())
					{
						gainedUsers.addElement("user").addAttribute("usrid", userId);
					}
					// En cualquier otro caso, no se consideran cambios (e.g. El usuario estaba deshabilitado antes del informe, canceló su suscripción,
					// posteriormente volvió a suscribirse pero antes del fin del informe volvió a cancelarla. El usuario ya estaba perdido de antes.)
				}
			}
		}
	}
	
	/** Codificacion del fichero CSV. */
	final static private byte[] BOM_UTF8 = new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF};
	
	public static void exportToCsv(HttpServletRequest request, HttpServletResponse response, String scheduleId, String fromDate, String toDate) throws SystemException
	{
		try
		{
			// Valida la entrada
			ErrorRaiser.throwIfNull(scheduleId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			ErrorRaiser.throwIfNull(fromDate, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			ErrorRaiser.throwIfNull(toDate, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			// Pide el informe
			_log.debug("Exporting newsletter report to CSV...");
			String sql = String.format(SQL_GET_SCHEDULE_METRICS, scheduleId, fromDate, toDate);
			Document d = PortalLocalServiceUtil.executeQueryAsDom(sql);
			
			// Crea el fichero CSV
			// Cabecera del response
			response.setHeader("Content-Type", "text/csv");
			response.setHeader("Content-Disposition", "attachment;filename=\"data.csv\"");
			response.setContentType("application/csv");
			
			// Inicializacion del fichero
			ServletOutputStream csvout = response.getOutputStream();
			csvout.write( BOM_UTF8 );
			
			// Escribe el fichero
			for (Node n : d.selectNodes("/rs/row"))
			{
				String row = new StringBuilder()
				.append(XMLHelper.getStringValueOf(n, "@date").substring(0, 10)).append(StringPool.SEMICOLON)
				.append(XMLHelper.getStringValueOf(n, "@subscribers")).append(StringPool.SEMICOLON)
				.append(XMLHelper.getStringValueOf(n, "@users")).append(StringPool.RETURN_NEW_LINE)
				.toString();
				
				csvout.write( new StringBuilder(row).toString().getBytes() );
			}
			
			// Finaliza el fichero
			csvout = response.getOutputStream();
			csvout.flush();
		}
		catch (Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
        finally
        {
        	try
        	{
		        ServletOutputStream csvout = response.getOutputStream();
	    		if (csvout != null)
	    			csvout.close();
        	}
        	catch (IOException e)
        	{
        		_log.error("Unable to close output stream");
        	}
        }
	}
}
