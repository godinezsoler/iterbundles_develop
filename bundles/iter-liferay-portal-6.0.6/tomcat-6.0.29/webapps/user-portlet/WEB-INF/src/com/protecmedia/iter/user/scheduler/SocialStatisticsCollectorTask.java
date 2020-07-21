package com.protecmedia.iter.user.scheduler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.cluster.Heartbeat;
import com.protecmedia.iter.base.service.StatisticMgrLocalServiceUtil;
import com.protecmedia.iter.base.service.scheduler.Task;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.user.scheduler.networks.SocialStatisticsConfig;

/**
 * Modela las tareas de recoleccion de estadisticas de articulos en redes sociales.
 * 
 * @author Protecmedia
 * 
 */
public class SocialStatisticsCollectorTask extends Task
{
	/** Logger. */
	private static Log log = LogFactoryUtil.getLog(SocialStatisticsCollectorTask.class);
	/** Articulos a actualizar. */
	List<Node> articles;
	/** Semaforo para la actualizacion de las estadisticas en BBDD. */
	private Semaphore SEMAPHORE_UPDATE_STATISTICS;
	/** Flag para el primer lanzamiento de la tarea. */
	private boolean firsTime = false;
	/** Configuracion de la red social a utilizar. */
	private SocialStatisticsConfig networkConfig;
	/** Consulta para obtener el estado y configuracion de la tarea. */
	private static String GET_SOCIAL_TASK_CONFIG = new StringBuffer()
		.append("SELECT collectstats, isStarted \n")
		.append("FROM   itersocialconfig                        \n")
		.append("WHERE  itersocialconfigid = '%s';                ")
		.toString();
	/** Sentencia para actualizar la configuracion de una tarea tras su primer lanzamiento. */
	private static final String UPDATE_SOCIALSTATISTICCONFIG_ISSTARTED = "UPDATE itersocialconfig SET isStarted=1 WHERE itersocialconfigid='%s'";
	
	/**
	 * Constructor.
	 * @param name el nombre de la tarea.
	 * @param delay el tiempo entre lanzamientos.
	 * @param tasks el grupo al que pertenece la tarea.
	 * @param SEMAPHORE_UPDATE_STATISTICS el semaforo para la actualizacion en BBDD.
	 * @param config la configuracion de la red social a utilizar.
	 */
	public SocialStatisticsCollectorTask(String name, long delay, ThreadGroup tasks, final Semaphore SEMAPHORE_UPDATE_STATISTICS, SocialStatisticsConfig networkConfig)
	{
		super(name, delay, tasks);
		this.networkConfig = networkConfig;
		this.articles = new ArrayList<Node>();
		this.SEMAPHORE_UPDATE_STATISTICS = SEMAPHORE_UPDATE_STATISTICS;
	}

	/**
	 * Flujo principal de la tarea.
	 * 
	 * Comprueba si al tarea esta activa, recupera los articulos a actualizar
	 * (o todos los articulos si es su primer lanzamiento) y crea uno o varios
	 * hilos de recoleccion de estadisticas. Cuando todos terminan, reconfigura
	 * el proximo lanzamiento en funcion de la quota o el delay por defecto.
	 */
	@Override
	public void doWork()
	{
		try
		{
			if(log.isDebugEnabled())
				log.debug(networkConfig.getName() + " [doWork()] Starting task...");
			// Comprueba si la tarea esta activa en el servidor actual.
			if(isActiveTask())
			{
				log.info("Launching " + networkConfig.getName() + " social statistics collecting task.");
				// Obtiene artículos a actualizar.
				obtainArticles();
				// Realiza cualquier accion de configuración inicial necesaria
				if (networkConfig.prepare())
				{
					// Lanza los recolectores de estadisticas y espera a que todos terminen.
					startThreads();
				}
				else
				{
					// Si no puede configurarse, se duerme 5 minutos.
					try
					{
						sleep(300000);
					}
					catch (InterruptedException e)
					{
						log.debug("Facebook task interrupted!");
					}
				}
			}
		}
		catch (Throwable e)
		{
			log.error(e);
		}
		// Reconfigura el proximo lanzamiento.
		interrupt(RECONFIG);
	}
	
	/**
	 * Comprueba si la tarea esta activa y si debe ejecutarse en el servidor actual.
	 * 
	 * <p>Ademas, si es la primera ejecucion de la tarea, establece el flag firsTime a
	 * verdadero para que se recuperen todos los articulos.</p>
	 * 
	 * @return true si la tarea esta activa en el servidor actual.
	 * @throws SecurityException si ocurre un error al recuperar la configuracion de iterstatisticsconfig.
	 * @throws NoSuchMethodException si ocurre un error al recuperar la configuracion de iterstatisticsconfig.
	 * @throws ServiceError si ocurre un error al recuperar la configuracion de iterstatisticsconfig.
	 * @throws IOException si ocurre un error al actualizar isStarted en iterstatisticsconfig.
	 * @throws SQLException si ocurre un error al actualizar isStarted en iterstatisticsconfig.
	 */
	private boolean isActiveTask() throws SecurityException, NoSuchMethodException, ServiceError, IOException, SQLException
	{
		// Recupera la configuracion actual de itersocialconfig.
		Document dom = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SOCIAL_TASK_CONFIG, getName().substring(3)));
		
		boolean collectStats = GetterUtil.getBoolean(XMLHelper.getStringValueOf(dom, "/rs/row/@collectstats")) && Heartbeat.canLaunchProcesses();
		boolean isStarted    = GetterUtil.getBoolean(XMLHelper.getStringValueOf(dom, "/rs/row/@isStarted"));

		// Si es la primera vez que se inicia la tarea, actualiza itersocialconfig.isStarted y establece el flag de recoger todos los articulos a verdadero. 
		if (collectStats && !isStarted)
		{
			PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_SOCIALSTATISTICCONFIG_ISSTARTED, this.getName().substring(3)));
			firsTime = true;
		}
		
		return collectStats;
	}
	
	/**
	 * Recupera los articulos a actualizar.
	 * 
	 * <p>Si es la primera vez que se lanza la tarea, recupera todos los articulos y
	 * limpia el flag firstTime estableciendolo a falso.</p>
	 * 
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws ServiceError
	 */
	private void obtainArticles() throws NoSuchMethodException, SecurityException, ServiceError
	{
		if(log.isDebugEnabled())
			log.debug(networkConfig.getName() + " [obtainArticles()]");
		
		if(firsTime)
		{
			if(log.isDebugEnabled())
				log.debug(networkConfig.getName() + " [obtainArticles()] First time: Get all articles.");
			Document articlesDom = StatisticMgrLocalServiceUtil.getArticles(networkConfig.getGroupId());
			ErrorRaiser.throwIfNull(articlesDom);
			articles = articlesDom.selectNodes("/rs/row");
			firsTime = false;
		}
		else
		{
			articles = StatisticMgrLocalServiceUtil.getCanonicalArticlesValidLapsed(getName(), String.valueOf(networkConfig.getStatisticsOp()), networkConfig.getGroupId());
		}

		ErrorRaiser.throwIfNull(articles);
		
		if (log.isDebugEnabled())
			log.debug(networkConfig.getName() + " [obtainArticles()] Processing " + articles.size() + " articles.");
	}
	
	/**
	 * Lanza los hilos de los recolectores de estadisticas y se queda esperando a que todos terminen.
	 */
	private void startThreads()
	{
		if(log.isDebugEnabled())
			log.debug(networkConfig.getName() + " [startThreads()]");
		List<Thread> threads = new ArrayList<Thread>();
		int[] index = new int[]{0};
		
		if(log.isDebugEnabled())
			log.debug(networkConfig.getName() + " [startThreads()] Creating " + networkConfig.getMaxThreads() + " threads.");
		for(int th = 0; th < networkConfig.getMaxThreads(); th++){
			SocialStatisticsCollectorThread thread = new SocialStatisticsCollectorThread(networkConfig, th, articles, index, SEMAPHORE_UPDATE_STATISTICS);
			if(Validator.isNotNull(thread))
			{
				thread.start();
				threads.add(thread);
			}
		}
		
		if(log.isDebugEnabled())
			log.debug(networkConfig.getName() + " [startThreads()] Waiting for threads...");
		for (Thread thread : threads)
		{
			try
			{
				thread.join();
			}
			catch (InterruptedException e)
			{
				if(log.isDebugEnabled())
					log.debug(networkConfig.getName() + " [startThreads()] Interrupted. Stopping collectors...");
				stopCollectors(threads);
				break;
			}
		}
		if(log.isDebugEnabled())
			log.debug(networkConfig.getName() + " [startThreads()] Finish waiting for threads.");
		// Se limpian los campos que ya no son necesarios.
		threads.clear();
		
	}
	
	/**
	 * Detiene todos los recolectores.
	 * @param threads la lista de SocialStatisticsCollectorThread a parar.
	 */
	private void stopCollectors(List<Thread> threads)
	{
		for (Thread t : threads)
		{
			((SocialStatisticsCollectorThread) t).stopCollector();
			t.interrupt();
		}
	}
	
	/**
	 * Establece el proximo lanzamiento de la tarea.
	 */
	@Override
	public void doReconfig()
	{
		setDelay(networkConfig.getDelay());
		if(log.isDebugEnabled())
			log.debug(networkConfig.getName() + " [doReconfig()] Reconfiguring task delay: " + getDelay() + " ms.");
	}
	
	@Override
	public void doDead()
	{
		log.info("Stopping " + networkConfig.getName() + " social statistics collecting task.");
	}
}
