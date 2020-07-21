package com.protecmedia.iter.user.scheduler.networks;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.user.util.social.SocialStatisticsQuotaController;

/**
 * Configuracion de particularidades de redes sociales para las tareas
 * de recoleccion de estadisticas.
 * 
 * @author Protecmedia
 *
 */
public abstract class SocialStatisticsConfig
{
	/** Logger. */
	protected static Log log = LogFactoryUtil.getLog(SocialStatisticsConfig.class);
	protected String name;
	protected int statistics_op;
	protected int delay = 300000;
	protected int max_threads   = 1;
	protected int max_articles  = 1;
	
	protected String groupId;
	
	protected int connectTimeout = GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_TIMEOUT_CONNECT), 5000);
	protected int readTimeout = GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_TIMEOUT_READ), 5000);
	
	protected String api_url = StringPool.BLANK;
	protected String response_count_field = StringPool.BLANK;
	protected String post_data = StringPool.BLANK;
	
	protected String quota = null;
	protected SocialStatisticsQuotaController socialQuotaController = null;
	
	public SocialStatisticsConfig(String groupId)
	{
		super();
		this.groupId = groupId;
		init();
		socialQuotaController = new SocialStatisticsQuotaController(quota);
	}
	
	public abstract void init();
	public boolean prepare() { return true; };
	
	protected int checkQuota()
	{
		synchronized (socialQuotaController)
		{
			return socialQuotaController.getIndexBlockedQuota();
		}
	}
	
	protected Long getQuotaTimeToSleep(int quota)
	{
		synchronized (socialQuotaController)
		{
			return socialQuotaController.getTimeToSleep(quota);
		}
	}
	
	public JSONObject getStatistics(AtomicReference<HttpURLConnection> connection, String url)
	{
		JSONObject counts = null;
		int maxAge = 0;
		Long quotaTimeSleep = 0L;
		
		String apiURL = null;
		String postData = null;
		boolean post = false;
		
		if (Validator.isNotNull(post_data))
		{
			postData = String.format(post_data, url);
			apiURL = api_url;
			post = true;
		}
		else
		{
			apiURL = String.format(api_url, url);
		}
		apiURL = apiURL.replace(" ", "%20");
		
		// Comprueba si se ha superado la quota
		int quota = checkQuota();
		
		HttpURLConnection httpConnection = null;  
		if(quota < 0)
		{
			log.debug(getName() + " [getStatistics()] Conecting to API for URL: " + apiURL);
			try
			{
				// Llama a la API y recoge el resultado
				httpConnection = (HttpURLConnection)(new URL(apiURL).openConnection());
				httpConnection.setConnectTimeout(connectTimeout);
				httpConnection.setReadTimeout(readTimeout);
				
				if (post)
				{
					httpConnection.setRequestMethod("POST");
					httpConnection.addRequestProperty("Content-Type", "application/json");
					httpConnection.addRequestProperty("Content-Length", "" + postData.length());
					httpConnection.setDoOutput(true);
					httpConnection.setDoInput(true);
					DataOutputStream outputStream = new DataOutputStream(httpConnection.getOutputStream());
			        outputStream.writeBytes(postData.toString());
			        outputStream.flush();
			        outputStream.close();
				}
				else
				{
					httpConnection.setRequestMethod("GET");	
				}
				connection.set(httpConnection);
				httpConnection.connect();
				HttpUtil.throwIfConnectionFailed(httpConnection, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
				
				// Si la API retorna correctamente, se recuperan los contadores y el tiempo de expiración.
				if ( HttpServletResponse.SC_OK == httpConnection.getResponseCode())
				{
					// Recupera el contador
					counts = getCounts(httpConnection);
					
					// Recupera el tiempo de expiración
					maxAge = getMaxAge(httpConnection) * 1000;
				}
				else
				{
					if (log.isDebugEnabled())
						log.debug(getName() + " [getStatistics()] API returns response code " + httpConnection.getResponseCode() + " for URL: " + apiURL);
				}
			}
			catch (Throwable th)
			{
				if (log.isDebugEnabled())
				{
					log.debug(getName() + " [getStatistics()] Error retrieving statistics from API for URL: " + apiURL);
					log.debug(th);
				}
			}
			finally
			{
				// Cierra el socket
				if (httpConnection != null)
				{
					httpConnection.disconnect();
					connection.set(null);
				}
			}
		}
		else
		{
			// A veces, cuando se supera una quota, puede darse el caso de que los milisegundos que falten
			// se consuman al ir a consultar el tiempo que debe dormirse el hilo o porque otro hilo haya
			// ido a comprobar la quota y al haberse superado reinicie el contador.
			// Si estamos aquí es que el hilo debe repetir el artículo, por lo que como mínimo se retorna 1ms.
			quotaTimeSleep = Math.max(getQuotaTimeToSleep(quota), 1);
		}
		
		return buildResponse(counts, maxAge, quotaTimeSleep);
	}
	
	/**
	 * Conecta con la API de la red social y recupera las estadísticas.
	 * @param httpConnection la httpURLConnection.
	 * @return JSON con las estadísticas recuperadas de la API.
	 */
	private JSONObject getCounts(HttpURLConnection httpConnection)
	{
		int totalCount = 0;
		JSONObject counts = JSONFactoryUtil.createJSONObject();
		if (httpConnection != null)
		{
			try
			{
				String strJson = processResponseString( StreamUtil.toString(httpConnection.getInputStream(), StringPool.UTF8) );
				JSONObject json = JSONUtil.createJSONObject( strJson );
				String[] keys = response_count_field.split(",");
				for (String key : keys)
				{
					JSONObject auxJson = json;
					String[] keyPath = key.split("\\.");
					int i = 0;
					for (i = 0; i < keyPath.length - 1; i++)
					{
						if (keyPath[i].endsWith("[]"))
						{
							keyPath[i] = keyPath[i].substring(0, keyPath[i].length() - 2);
							auxJson = auxJson.getJSONArray(keyPath[i]).getJSONObject(0);
						}
						else
						{
							auxJson = auxJson.getJSONObject(keyPath[i]);
						}
					}
					
					if (auxJson != null)
					{
						int count = auxJson.getInt(keyPath[i]);
						counts.put(keyPath[i], count);
						totalCount += count;
					}
				}
			}
			catch (Throwable th)
			{
				if (log.isDebugEnabled())
				{
					log.debug(getName() + " [getCounts()] API Response 200 but malformed data. Skipping article.");
					log.debug(th);
				}
			}
		}
		if (totalCount > 0)
			return counts;
		else
			return null;
	}
	
	/**
	 * Recupera el elemento max-age del header HTTP de la respuesta de la API.
	 * @param httpConnection la httpURLConnection
	 * @return El tiempo indicado en max-age. 0 si no viene en la cabecera.
	 */
	private int getMaxAge(HttpURLConnection httpConnection)
	{
		int maxAge = 0;
		if (httpConnection != null)
		{
			String cacheControl = httpConnection.getHeaderField("Cache-Control");
			if (Validator.isNotNull(cacheControl))
			{
				Pattern p = Pattern.compile("max-age=(\\d*)");
				Matcher m = p.matcher(cacheControl);
				
				if (m.find())
				{
					maxAge = Integer.valueOf(m.group(1));
				}
			}
		}
		return maxAge;
	}
	
	/**
	 * Construye la respuesta JSON con las estadísticas recuperadas con el formato:
	 * <pre>
	 * {
	 *   "counts": {"share_count":55,"like_count":324, ... },
	 *   "max-age": 0,
	 *   "quota-time-sleep": 0
	 * }
	 * </pre>
	 * Donde "counts" contiene un objeto JSON con las estadísticas recuperadas de la API.
	 * @param counts el objeto JSON con las estadísticas devueltas por la API.
	 * @param maxAge el valor de max-age del header.
	 * @param quotaTimeSleep si se supera la quota, el tiempo para replanificar la tarea.
	 * @return JSON con las estadísticas recuperadas y el tiempo de replanificación.
	 */
	private JSONObject buildResponse(JSONObject counts, int maxAge, long quotaTimeSleep)
	{
		JSONObject result = JSONFactoryUtil.createJSONObject();
		result.put("counts", counts);
		result.put("max-age", maxAge);
		result.put("quota-time-sleep", quotaTimeSleep);
		return result;
	}
	
	/**
	 * Recupera la URL formateada para la API de la red social.
	 * Las redes sociales pueden sobreescribir este método para adaptarlo a sus necesidades.
	 * @param article el artículo del cual se quiere extraer su URL.
	 * @return URL del artículo.
	 */
	public String extractURL(Node article) {
		return ((Element) article).attributeValue("url");
	}
	
	/**
	 * Procesa la respuesta de la API en caso de ser necesario.
	 * Las redes sociales pueden sobreescribir este método para adaptarlo a sus necesidades.
	 * @param in la respuesta original de la API.
	 * @return la respuesta procesada de la API.
	 */
	protected String processResponseString(String in) { return in; }

	public String getName()                   { return name; }
	public int getStatisticsOp()              { return statistics_op; }
	public int getDelay()                     { return delay; }
	public int getMaxThreads()                { return max_threads; }
	public int getMaxArticles()               { return max_articles; }
	public String getGroupId()                { return groupId; }
}
