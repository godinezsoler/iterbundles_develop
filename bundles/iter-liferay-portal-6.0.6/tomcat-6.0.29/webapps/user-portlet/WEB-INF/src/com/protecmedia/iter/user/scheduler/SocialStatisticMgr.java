package com.protecmedia.iter.user.scheduler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.http.client.ClientProtocolException;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.cluster.Heartbeat;
import com.protecmedia.iter.base.service.scheduler.Task;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.user.scheduler.networks.SocialStatisticsDisqusConfig;
import com.protecmedia.iter.user.scheduler.networks.SocialStatisticsFacebookConfig;
import com.protecmedia.iter.user.scheduler.networks.SocialStatisticsTwitterConfig;

public class SocialStatisticMgr extends Thread
{
	private static Log _log = LogFactoryUtil.getLog(SocialStatisticMgr.class);
	
	private ThreadGroup tasks;
	
	private static String GET_GROUPS_SOCIAL_STATISTIC_CONFIGS = new StringBuffer()
		.append("SELECT  groupid        \n")
		.append("FROM itersocialconfig  \n")
		.append("WHERE collectstats = 1 \n")
		.append("GROUP BY groupid;        ")
		.toString();

	private static String GET_SOCIALSTATISTIC_CONFIGS_FROMGROUP = new StringBuffer()
		.append("SELECT itersocialconfigid id, itersocial.socialname socialname, isStarted \n")
		.append("FROM itersocialconfig \n")
		.append("	INNER JOIN itersocial on itersocialconfig.itersocialid = itersocial.itersocialid \n")
		.append("WHERE collectstats = 1 AND groupid = '%s' ")
		.toString();
	
	private static String GET_SOCIAL_STATISTIC_CONFIG = new StringBuffer()
		.append("SELECT itersocial.socialname socialname, isStarted, groupid, collectstats \n")
		.append("FROM itersocialconfig \n")
		.append("	INNER JOIN itersocial on itersocialconfig.itersocialid = itersocial.itersocialid \n")
		.append("WHERE itersocialconfigid = '%s' ")
		.toString();
	
	public static String SOCIALSTATISTIC_FACEBOOK 	= "facebook";	
	public static String SOCIALSTATISTIC_TWITTER 	= "twitter";	
	public static String SOCIALSTATISTIC_DISQUS		= "disqus";

	public static Semaphore SEMAPHORE_UPDATE_STATISTICS = new Semaphore(1);
	
	public SocialStatisticMgr(ThreadGroup tasks)
	{
		this.tasks = tasks;
	}
	
	/**
	 * Lanza las tareas de recoleccion de estadisticas durante el arranque del Tomcat.
	 */
	@Override
	public void run()
	{
		_log.info("Initializing statistics tasks");
		try
		{
			if (Heartbeat.canLaunchProcesses())
			{
				List<Object> groups = PortalLocalServiceUtil.executeQueryAsList(GET_GROUPS_SOCIAL_STATISTIC_CONFIGS);
				ErrorRaiser.throwIfNull(groups);
				
				for (Object group : groups)
				{
					if(_log.isDebugEnabled())
						_log.debug("Initializing statistics tasks for "+ group + " group");
					String groupId = group.toString();
					Document configDom = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SOCIALSTATISTIC_CONFIGS_FROMGROUP, groupId));
					ErrorRaiser.throwIfNull(configDom);
					List<Node> configs = configDom.selectNodes("/rs/row");
					
					// Para cada itersocialconfig
					for (Node config : configs)
					{
						String idConfig = ((Element) config).attributeValue("id");
						ErrorRaiser.throwIfNull(idConfig);
						
						String socialName = ((Element) config).attributeValue("socialname");
						ErrorRaiser.throwIfNull(socialName);
						
						boolean isStarted = GetterUtil.getBoolean(((Element) config).attributeValue("isStarted"), false);
						createSocialStatisticTask(socialName, idConfig, groupId, isStarted);
					}
				}
			}
		}
		catch(Throwable e)
		{
			_log.error(e.toString());
		}
		_log.info("Statistics tasks has been initialized");
	}
	
	/**
	 * Actualiza una tarea de recoleccion de estadisticas.
	 * 
	 * <p>Para la tarea en caso de estar arrancada y comprueba si debe arrancarse con la nueva configuracion.</p>
	 * <p>Si hay que arrancar la tarea en otro servidor de afinidad, llama de nuevo a este metodo en el servidor remoto.</p>
	 * 
	 * @param idConfig el id de la tarea en itersocialconfig.
	 * 
	 * @throws SecurityException si ocurre un error durante la recuperacion de la configuracion, la creacion de la tarea o la comprobacion de las tareas de gestion del historico.
	 * @throws NoSuchMethodException si ocurre un error durante la recuperacion de la configuracion, la creacion de la tarea o la comprobacion de las tareas de gestion del historico.
	 * @throws ServiceError si falta configuracion obligatoria de itersocialconfig.
	 * @throws JSONException si ocurre un error durante la llamada al servidor de afinidad.
	 * @throws ClientProtocolException si ocurre un error durante la llamada al servidor de afinidad.
	 * @throws SystemException si ocurre un error durante la llamada al servidor de afinidad.
	 * @throws IOException si ocurre un error durante la comprobacion de las tareas de gestion del historico.
	 * @throws SQLException si ocurre un error durante la comprobacion de las tareas de gestion del historico.
	 */
	public void updateSocialStatisticsTask(String idConfig, boolean checkHistoryTasks) throws SecurityException, NoSuchMethodException, ServiceError, JSONException, ClientProtocolException, SystemException, IOException, SQLException
	{
		// Recupera la configuracion de la tarea
		Document configDom 		= PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SOCIAL_STATISTIC_CONFIG, idConfig));
		Element config 			= configDom.getRootElement().element("row");
		ErrorRaiser.throwIfNull(config);

		String socialName 		= config.attributeValue("socialname");
		ErrorRaiser.throwIfNull(socialName);
		
		String groupId 			= config.attributeValue("groupid");
		ErrorRaiser.throwIfNull(groupId);
		
		Boolean collectstats 	= GetterUtil.getBoolean(config.attributeValue("collectstats"));
		ErrorRaiser.throwIfNull(collectstats);
		
		boolean isStarted 		= GetterUtil.getBoolean(config.attributeValue("isstarted"));
		ErrorRaiser.throwIfNull(isStarted);
		
		// Si la tarea esta arrancada, la para.
		stopSocialStatisticsTask(idConfig);
		
		// La tarea esta activada 
		if (collectstats)
		{
			createSocialStatisticTask(socialName, idConfig, groupId, isStarted);
		}
	}
	
	public void stopSocialStatisticsTasks()
	{
		_log.info("Stopping statistics tasks");
		synchronized(tasks)
		{
			Thread[] taskArray = new Thread[tasks.activeCount()];
			tasks.enumerate(taskArray);
			
			for(int i = taskArray.length-1; i >= 0; i--)
			{
				if (taskArray[i].isAlive())
				{
					((Task)taskArray[i]).interrupt(Task.DEAD);
				}
			}
		}
		_log.info("Statistics tasks has been stopped");
	}
	
	public void stopSocialStatisticsTask(String idConfig) throws JSONException, ClientProtocolException, SystemException, IOException
	{
		Task task = getTask(idConfig);
		if (task != null)
		{
			task.interrupt(Task.DEAD);
		}
	}

	
	/**
	 * Arranca una tarea de recoleccion de estadisticas.
	 * 
	 * @param socialName el nombre de la tarea. Puede ser 'facebook', 'twitter', 'googleplus' o 'disqus'
	 * @param idConfig el identificador de la tarea en itersocialconfig.
	 * @param groupId el grupo de la tarea de itersocialconfig.
	 * @param isStarted el flag que indica que es la primera vez que se arranca la tarea. Solo es util
	 *                  para el modo de compatibilidad.
	 * 
	 * @throws NoSuchMethodException si en el modo de compatibilidad una tarea se inicia por primera vez y ocurre un error al obtener todos los articulos.
	 * @throws ServiceError si en el modo de compatibilidad una tarea se inicia por primera vez y ocurre un error al obtener todos los articulos.
	 * @throws SecurityException si en el modo de compatibilidad una tarea se inicia por primera vez y ocurre un error al obtener todos los articulos.
	 * @throws SQLException si en el modo de compatibilidad ocurre un error al actualizar isstarted.
	 * @throws IOException si en el modo de compatibilidad ocurre un error al actualizar isstarted.
	 */
	private void createSocialStatisticTask(String socialName, String idConfig, String groupId, boolean isStarted) throws SecurityException, ServiceError, NoSuchMethodException, IOException, SQLException
	{
		if (Heartbeat.canLaunchProcesses() && tasks != null)
		{
			_log.trace("create Task " + socialName + " whith config "+ idConfig);
			
			Task task = null;
			synchronized(tasks)
			{
				
				if(SOCIALSTATISTIC_FACEBOOK.equals(socialName))
				{
					task = new SocialStatisticsCollectorTask("FB_"+idConfig, 0, tasks, SEMAPHORE_UPDATE_STATISTICS, new SocialStatisticsFacebookConfig(groupId));
				}
				else if(SOCIALSTATISTIC_TWITTER.equals(socialName))
				{
					task = new SocialStatisticsCollectorTask("TW_"+idConfig, 0, tasks, SEMAPHORE_UPDATE_STATISTICS, new SocialStatisticsTwitterConfig(groupId));
				}
				else if(SOCIALSTATISTIC_DISQUS.equals(socialName))
				{
					SocialStatisticsDisqusConfig disqusConfig = new SocialStatisticsDisqusConfig(groupId);
					if (disqusConfig.test())
						task = new SocialStatisticsCollectorTask("DQ_"+idConfig, 0, tasks, SEMAPHORE_UPDATE_STATISTICS, disqusConfig);
				}
			}
			
			if (Validator.isNotNull(task))
			{
				task.start();
			}
			else
			{
				_log.error("Unable to initialize " + socialName + " Statistics Collector Settings.");
			}
		}
	}
	
	/**
	 * Recupera la tarea taskid del grupo de tareas gestionadas.
	 * 
	 * @param taskid el id de la tarea a recuperar.
	 * @return la tarea en ejecucion o null en caso de no existir.
	 */
	private Task getTask(String taskid)
	{
		Task result = null;
		synchronized(tasks)
		{
			Thread[] taskArray = new Thread[tasks.activeCount()];
			tasks.enumerate(taskArray);
			if(taskArray != null)
			{
				for(int i = 0; i < taskArray.length; i++)
				{
					if (taskArray[i].getName().substring(3).equals(taskid) && taskArray[i].isAlive())
					{
						result = (SocialStatisticsCollectorTask) taskArray[i];
						break;
					}
				}
			}
		}
		return result;
	}
}