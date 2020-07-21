package com.protecmedia.iter.user.scheduler;

import java.net.HttpURLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.protecmedia.iter.base.service.StatisticMgrLocalServiceUtil;
import com.protecmedia.iter.user.scheduler.networks.SocialStatisticsConfig;
/**
 * Modela los recolectores de estadisticas de redes sociales.
 * 
 * @author Protecmedia
 *
 */
public class SocialStatisticsCollectorThread extends Thread
{
	/** Logger. */
	private static Log log = LogFactoryUtil.getLog(SocialStatisticsCollectorThread.class);
	/** Longitud maxima de la query para actualizar estadisticas. */
	private static int MAX_SQL_QUERY_LENGTH = 1000000;
	/** Articulos a actualizar. */
	private List<Node> articles;
	/** Indice compartido para obtener los articulos a actualizar por iteracion. */
	private int[] index;
	/** Semaforo para el acceso a BBDD. */
	private Semaphore SEMAPHORE_UPDATE_STATISTICS;
	/** Configuracion de la red social a utilizar. */
	private SocialStatisticsConfig networkConfig;
	/** Tiempo de espera si se supera la cuota. */
	private Long quotaTimeSleep = 0L;
	/** Control de parada controlada. */
	private boolean run;
	/** Wrapper para acceder a los socket por los que se conecta a las APIs. */
	private AtomicReference<HttpURLConnection> httpConnection;
	
	/**
	 * Constructor.
	 * 
	 * @param config la configuracion de la red social de la cual se recolectan estadisticas.
	 * @param threadNum el numero del recolector actual para crear el nombre identificativo.
	 * @param max_ages lista de tiempos de espera para volver a consultar estadisticas.
	 * @param articles los articulos a actualizar.
	 * @param index el indice compartido para obtener los articulos a actualizar por iteracion.
	 * @param SEMAPHORE_UPDATE_STATISTICS el semaforo para el acceso a BBDD.
	 */
	public SocialStatisticsCollectorThread(SocialStatisticsConfig networkConfig, int threadNum, List<Node> articles, int[] index, final Semaphore SEMAPHORE_UPDATE_STATISTICS)
	{
		super(networkConfig.getName()+threadNum);
		this.articles = articles;
		this.index = index;
		this.SEMAPHORE_UPDATE_STATISTICS = SEMAPHORE_UPDATE_STATISTICS;
		this.networkConfig = networkConfig;
		this.run = true;
	}
	
	/**
	 * Ejecucion del recolector. Si ocurre cualquier error se detiene.
	 */
	@Override
	public void run()
	{
		if(log.isDebugEnabled())
			log.debug("Thread: " + getName() + " Start");
		try
		{
			this.collectStatistics();
		}
		catch (Throwable e)
		{
			log.error(e);
		}
		if(log.isDebugEnabled())
			log.debug("Thread: " + getName() + " Finish");
	}
	
	/**
	 * Recolecta las estadisticas de los articulos por bloques y actualiza los contadores en BBDD.
	 */
	private void collectStatistics()
	{
		if(log.isDebugEnabled())
			log.debug("Thread: " + getName() + " [collectStatistics()]");
		Map<String, String> myArticles;
		
		// Se recorren los artículos
		while(run && (myArticles = getWorkingSet()) != null)		
		{
			// Inicializa el mapa y obtiene el listado de urls
			Map<String, JSONObject> statistics = new HashMap<String, JSONObject>();
			Long max_age = 0L;
			
			// Recoge las estadisticas de los articulos.
			max_age = getStatistics(myArticles, statistics);
			
			try
			{
				// Actualizacion de estadisticas.
				updateStatistics(statistics, max_age);
			}
			catch (InterruptedException e)
			{
				log.error("Thread: " + getName() + " interrupted");
				stopCollector();
			}
		}
	}

	/**
	 * Obtiene el bloque o sublista de artículos de todos los artículos a actualizar.
	 * 
	 * @return el mapa de url canonicas y articleId a actualizar en esta iteracion.
	 */
	private Map<String, String> getWorkingSet()
	{
		if(log.isDebugEnabled())
			log.debug("Thread: " + getName() + " [getWorkingSet()]");
		List<Node> subList = null;
		Map<String, String> work = null;
		
		synchronized (index)
		{
			int max_articles = networkConfig.getMaxArticles();
			if(index[0] >= 0 && max_articles >= 0 && index[0] < articles.size())
			{
				subList = articles.subList(index[0], (index[0]+max_articles > articles.size())? articles.size() : index[0]+max_articles);
				index[0] += max_articles;
			}
		}
		
		if (Validator.isNotNull(subList))
		{
			work = new HashMap<String, String>();
			for (Node node : subList)
			{
				String url = networkConfig.extractURL(node); // Si es Disqus, a veces solo necesita el id en lugar de la url.
				String contentId = ((Element) node).attributeValue("contentId"); 
				work.put(url, contentId);
			}
		}
		
		return work;
	}
	
	/**
	 * Conecta mediante JSON con la API de la red social y recupera las estadisticas de los articulos.
	 * 
	 * <p>Si la noticia no tiene compartidos, no se incluye en las estadisticas.</p>
	 * 
	 * <p>Calcula el tiempo para la proxima ejecucion de la tarea y comprueba si se ha superado la cuota de acceso,
	 * lanzando un InterruptedException en caso aformativo.</p>
	 * 
	 * @param myArticles el mapa de url canonicas y articleId a actualizar en esta iteracion.
	 * @param statistics el mapa de articleId y objetos JSON con las estadisticas a actualizar.
	 * @return el tiempo calculado para el proximo lanzamiento de la tarea.
	 */
	private Long getStatistics(Map<String, String> myArticles, Map<String, JSONObject> statistics)
	{
		if(log.isDebugEnabled())
			log.debug("Thread: " + getName() + " [getStatistics()] Connecting to API (" + myArticles.size() + " Articles)...");
		Long max_age = 0L;

		String url = null;
		quotaTimeSleep = 0L;
		Iterator<String> iter = myArticles.keySet().iterator();
		
		// Itera mientras queden URLs de artículos
		while (iter.hasNext() || quotaTimeSleep > 0)
		{
			// Si en la iteración anterior superó la quota, vuelve a procesar la misma URL.
			if (quotaTimeSleep == 0)
				url = iter.next();
			
			if(log.isDebugEnabled())
				log.debug("Thread: " + getName() + " Processing URL: " + url);
			
			httpConnection = new AtomicReference<HttpURLConnection>();
			JSONObject response = networkConfig.getStatistics(httpConnection, url);
			quotaTimeSleep = response.getLong("quota-time-sleep");
			if(quotaTimeSleep > 0)
			{
				if(log.isDebugEnabled())
					log.debug("Thread: " + getName() + " Thread Sleep due to Quota: " + quotaTimeSleep + " ms");
				
				try
				{
					sleepIfQuotaReached(quotaTimeSleep);
				}
				catch (InterruptedException e)
				{
					log.error("Thread: " + getName() + " interrupted");
					stopCollector();
				}
			}

			if (!run) break;
			
			JSONObject counts = response.getJSONObject("counts");
			if (counts != null)
			{
				String id = myArticles.get(url);
				statistics.put(id, counts);
				max_age = response.getLong("max-age");
			}
		}
		
		return max_age;
	}
	
	/**
	 * Actualiza las estadisticas de los articulos en BBDD.
	 * 
	 * @param statistics el mapa de articleId y objetos JSON con las estadisticas a actualizar.
	 * @param max_age el tiempo de vigencia de la estadistica en el articulo para la proxima actualizacion.
	 * @throws InterruptedException si se interrumpe al recolector mientras espera a adquirir el semaforo.
	 */
	private void updateStatistics(Map<String, JSONObject> statistics, Long max_age) throws InterruptedException
	{
		if(log.isDebugEnabled())
			log.debug("Thread: " + getName() + " [updateStatistics()] Updating " + statistics.size() + " statistics in BBDD...");
		
		if(statistics.size() > 0)
		{
			Date date = new Date();
			Date dateExpire = new Date(date.getTime()+max_age);				
			
			SEMAPHORE_UPDATE_STATISTICS.acquire();
			try
			{
				StatisticMgrLocalServiceUtil.updateStatistics(statistics, MAX_SQL_QUERY_LENGTH, networkConfig.getGroupId(), date, dateExpire, networkConfig.getStatisticsOp());
			}
			catch (Throwable th)
			{
				log.error(th);
			}
			finally
			{
				SEMAPHORE_UPDATE_STATISTICS.release();	
			}
		}
	}
	
	/**
	 * Si se ha alcanzado la quota, duerme al hilo actual el tiempo restante establecido en la quota.
	 * 
	 * @param max_age el tiempo calculado para el proximo lanzamiento de la tarea.
	 * @throws InterruptedException 
	 */
	private void sleepIfQuotaReached(Long max_age) throws InterruptedException
	{
		if(log.isDebugEnabled())
			log.debug("Thread: " + getName() + "  Sleepping for " + max_age + " ms");
		
		if (max_age > 0) {
			Thread.sleep(max_age);
		}
	}
	
	/**
	 * Para el proceso actual de recoleccion de estadisticas y lanza una interrupcion
	 * para que despierte si esta esperando al semaforo.
	 */
	public void stopCollector()
	{
		if(log.isDebugEnabled())
			log.debug("Thread: " + getName() + " Stopping...");
		
		// Para la tarea
		run = false;
		
		// Si está esperando en un socket, lo cierra para que salte excepción y se entere de la interrupción.
		if (httpConnection != null && httpConnection.get() != null)
		{
			log.info("Thread: " + getName() + " Closing Social Statistic Thread Socket...");
			try
			{
				httpConnection.get().disconnect();
			}
			catch (Throwable th)
			{
				if (log.isDebugEnabled())
					log.debug("Thread: " + getName() + " Connection already closed.");
			}
		}
		
		this.interrupt();
	}
}