package com.protecmedia.iter.base.community.publisher;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import com.liferay.portal.apache.ApacheHierarchy;
import com.liferay.portal.apache.ApacheUtil;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.HttpMethods;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.IterURLUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.IterMonitor.Event;
import com.protecmedia.iter.base.community.manager.CommunityFactory;
import com.protecmedia.iter.base.community.util.CommunityAuthorizerUtil;

public class CommunityPublisherProcess extends Thread
{
	/** Logger */
	private static Log _log = LogFactoryUtil.getLog(CommunityPublisherProcess.class);

	/** Indicador de proceso activo */
	private AtomicBoolean running;
	
	/** Indicador de necesidad de replanificarse */
	private AtomicBoolean needReschedule;
	
	/** Candado para el acceso a BBDD */
	private final Lock lock;

	/** Sentencia para recuperar el tiempo que falta hasta la próxima publicación programada */
	private static final String SQL_SELECT_NEXT_SCHEDULE = "SELECT TIMESTAMP(schedule) nextSchedule, UNIX_TIMESTAMP(schedule) - UNIX_TIMESTAMP(now()) nextScheduleDelay FROM schedule_publication WHERE processId IS NULL ORDER BY schedule ASC LIMIT 1";
	
	/** Sentencia para recuperar los elementos a publicar */
	private static final String SQL_SELECT_PENDING_PUBLICATIONS  = "SELECT publicationId, groupId, articleId, title, accountId, accountName, accountType, credentials, numRetries FROM schedule_publication WHERE schedule <= now() AND processId IS NULL";
	
	/** Sentencia para borrado de las publicaciones procesadas */
	private static final String SQL_DELETE_PROCESED_PUBLICATIONS = "DELETE FROM schedule_publication WHERE publicationId IN (%s)";
	
	/** Sentencia para actualizar el ID de proceso de las publicaciones en curso */
	private static final String SQL_UPDATE_PUBLICATIONS_IN_PROGRESS = "UPDATE schedule_publication SET processId='%s' WHERE publicationId='%s'";
	
	private static final String SQL_UPDATE_UNAVAILABLES_ARTICLES = new StringBuilder(
		"UPDATE schedule_publication 													\n").append(
		"	SET numRetries=numRetries+1, schedule=DATE_ADD(NOW(), INTERVAL %d SECOND)\n").append(
		"		WHERE publicationId IN (%s)												\n").toString();
				
	
	/**
	 * <p>Establece el candado para el acceso sincronizado a BBDD.</p>
	 * <p>Por defecto, el proceso arranca activo y con replanificación pendiente.</p>
	 * @param lock El candado para el acceso a BBDD sincronizado.
	 */
	public CommunityPublisherProcess(Lock lock)
	{
		super("Community Publisher Process");
		running        = new AtomicBoolean(true);
		needReschedule = new AtomicBoolean(true);
		this.lock      = lock;
	}
	
	@Override
	public void run()
	{
		_log.debug("Community Publisher started");
		while (running.get())
		{
			// Espera hasta la próxima ejecución
			while (running.get() && needReschedule.get())
				waitForNextPublication();
			
			// Para que se replanifique tras la publicación.
			this.needReschedule.set(true);
			
			// Publica el contenido
			if (running.get())
				publishArticles();
		}
		_log.debug("Community Publisher stopped");
	}
	
	/**
	 * <p>Informa de si el proceso está activo o se ha solicitado su parada.</p>
	 * @return {@code true} si el proceso está publicando o esperando a la siguiente publicación programada.
	 *         {@code false} si se ha solicitado su parada o está parado.
	 */
	public boolean isRunning()
	{
		return running.get();
	}
	
	/**
	 * <p>Solicita la parada del proceso de publicación.</p>
	 * <p>Si está publicando, termina la publicación actual, elimina todas las que ha procesado hasta el momento y finaliza de forma controlada.</p>
	 */
	public void halt()
	{
		// Establece el indicador de encendido a falso.
		running.set(false);
		// Manda una interrupcion por si estuviera esperando.
		this.interrupt();
	}
	
	/**
	 * <p>Solicita al proceso que se replanifique.</p>
	 * <p>Sirve para reprogramar al publicador cuando hay cambios en la planificación.</p>
	 */
	public void reschedule()
	{
		// Establece el indicador de cambios en la planificación a verdadero.
		this.needReschedule.set(true);
		// Manda una interrupcion por si estuviera esperando.
		this.interrupt();
	}
	
	/**
	 * <p>Suspende el proceso hasta la siguiente publicación programada.</p>
	 */
	private synchronized void waitForNextPublication()
	{
		long nextPublication = 0;
		while (running.get() && (nextPublication = millisToNextPublication()) > 0)
		{
			try
			{
				this.wait(nextPublication);
			}
			catch (InterruptedException e)
			{
				_log.debug("Interrupted!");
			}
		}
	}
	
	/**
	 * <p>Recupera los milisegundos que faltan hasta la siguiente publicación.</p>
	 * <p>Si no tiene nada que publicar, detiene el proceso para evitar consumir recursos.</p>
	 * @return Los milisegundos que faltan hasta la próxima publicación.
	 */
	private long millisToNextPublication()
	{
		String nextSchedule  = StringPool.BLANK;
		long nextPublication = -1;
		
		// Candado para acceder a la tabla	
		lock.lock();
		
		try
		{
			// Recupera la próxima publicación
			Document nexScheduleDom = PortalLocalServiceUtil.executeQueryAsDom(SQL_SELECT_NEXT_SCHEDULE);
			nextSchedule    = XMLHelper.getStringValueOf(nexScheduleDom.getRootElement(), "/rs/row/@nextSchedule");
			nextPublication = XMLHelper.getLongValueOf(nexScheduleDom.getRootElement(), "/rs/row/@nextScheduleDelay", -1);
			
			if (Validator.isNotNull(nextSchedule))
			{
				// Como acaba de consultar la planificación, el indicador de necesidad de planificación a falso.
				this.needReschedule.set(false);
				
				_log.info("Next publication at: " + nextSchedule + " [" + nextPublication + "s remaining]");
				nextPublication = nextPublication <= 0 ? 0 : nextPublication * 1000;
			}
			else
			{
				_log.info("Nothing to publish, shutting down process.");
				this.halt();
			}
		}
		catch (Throwable t)
		{
			_log.error(t.getMessage());
			_log.debug(t);
		}
		finally
		{
			// Libera el candado
			lock.unlock();
		}
		
		return nextPublication;
	}
	
	/**
	 * <p>Recupera todas las publicaciones con fecha programada anterior al momento actual y las publica en la red social correspondiente.</p>
	 * <p>Tras la publicación correcta o no, se eliminan de la programación.</p>
	 */
	private void publishArticles()
	{
		_log.debug("Publishing...");
		
		Document pendingPublicationsDom = null;
		// Candado para acceder a la tabla
		lock.lock();
		try
		{
			// Recupera las publicaciones programadas que no están en proceso (no tienen processId)
			pendingPublicationsDom = PortalLocalServiceUtil.executeQueryAsDom(SQL_SELECT_PENDING_PUBLICATIONS);
		}
		catch (Throwable th)
		{
			IterMonitor.logEvent(GroupMgr.getGlobalGroupId(), Event.ERROR, new Date(), "Community Publisher: Unable to retrieve pending publications", th);
			_log.error("Unable to retrieve pending publications.");
			_log.error(th);
		}
		finally
		{
			// Libera el candado
			lock.unlock();
		}

		// Publica
		if (pendingPublicationsDom != null)
		{
			List<Node> pendingPublications = pendingPublicationsDom.selectNodes("/rs/row");
			if (pendingPublications.size() > 0)
			{
				List<String> publicationsProcessed 	= new ArrayList<String>();
				List<String> unavailableArticles 	= new ArrayList<String>();
				Map<String, String> publicationsInProcess = new HashMap<String, String>();
				for (Node publication : pendingPublications)
				{
					long groupId       = XMLHelper.getLongValueOf(  publication, "@groupId");
					String articleId   = XMLHelper.getStringValueOf(publication, "@articleId");
					String accountName = XMLHelper.getStringValueOf(publication, "@accountName");
					String accountType = XMLHelper.getStringValueOf(publication, "@accountType");
					String title       = XMLHelper.getStringValueOf(publication, "@title");
					
					// http://jira.protecmedia.com:8080/browse/ITER-709?focusedCommentId=26649&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-26649
					// Solo se publican los artículos disponibles en Apache
					if (isAvailable(groupId, articleId))
					{				
						String accountId   = XMLHelper.getStringValueOf(publication, "@accountId");
						String credentials = XMLHelper.getStringValueOf(publication, "@credentials");
						
						// Publica
						String processId = publish(groupId, articleId, accountId, accountName, accountType, title, credentials);
						
						// Si tiene que procesarse, añade la publicación al listado de en proceso
						if (processId != null)
						{
							publicationsInProcess.put(XMLHelper.getStringValueOf(publication, "@publicationId"), processId);
						}
						// Si se ha publicaco. añade la publicación al listado de procesadas
						else
							publicationsProcessed.add(XMLHelper.getStringValueOf(publication, "@publicationId"));
					}
					else
					{
						long numRetries = XMLHelper.getLongValueOf(publication, "@numRetries")+1;
						String msg = String.format("Community Publisher: The article %s is unavailable in the servers (%d retries| %s | %s | %s)", articleId, numRetries, title, accountType, accountName);
						
						if (numRetries > PropsValues.COMMUNITYPUBLISHER_CHECK_AVAILABLE_NUMRETRIES)
						{
							// Se ha superado el número de reintentos. Se traza y se marca como procesada para el posterior borrado de la tabla
							IterMonitor.logEvent(groupId, Event.ERROR, new Date(), msg);
							_log.error(msg);
							
							publicationsProcessed.add(XMLHelper.getStringValueOf(publication, "@publicationId"));
						}
						else
						{
							_log.debug(msg);

							// No se ha superado el total de reintentos, se marca para ser actualizado posteriormente
							unavailableArticles.add(XMLHelper.getStringValueOf(publication, "@publicationId"));
						}
					}
					
					// Comprueba si se ha solicitado una parada del proceso
					if (running.get() == false)
						break;
				}
				
				// Candado para acceder a la tabla
				lock.lock();
				try
				{
					// Actualiza las publicaciones cuyos artículos no están disponibles
					String publicationIds = StringUtil.merge(unavailableArticles.toArray(), StringPool.COMMA_AND_SPACE, StringPool.APOSTROPHE);
					if (Validator.isNotNull(publicationIds))
						PortalLocalServiceUtil.executeUpdateQuery(String.format(SQL_UPDATE_UNAVAILABLES_ARTICLES, PropsValues.COMMUNITYPUBLISHER_CHECK_AVAILABLE_DELAY, publicationIds));

					// Elimina las programaciones publicadas
					publicationIds = StringUtil.merge(publicationsProcessed.toArray(), StringPool.COMMA_AND_SPACE, StringPool.APOSTROPHE);
					if (Validator.isNotNull(publicationIds))
						PortalLocalServiceUtil.executeUpdateQuery(String.format(SQL_DELETE_PROCESED_PUBLICATIONS, publicationIds));
					
					// Añade los processId de las que están en proceso
					for (String publicationId : publicationsInProcess.keySet())
					{
						String processId = publicationsInProcess.get(publicationId);
						PortalLocalServiceUtil.executeUpdateQuery(String.format(SQL_UPDATE_PUBLICATIONS_IN_PROGRESS, processId, publicationId));
						CommunityPublisherController.INSTANCE.startIAProcessChecker(processId);
					}
				}
				catch (Throwable th)
				{
					IterMonitor.logEvent(GroupMgr.getGlobalGroupId(), Event.ERROR, new Date(), "Community Publisher: Unable to delete processed publications", th);
					_log.error("Unable to delete processed publications.");
					_log.error(th);
				}
				finally
				{
					// Libera el candado
					lock.unlock();
				}
			}
		}
		else
		{
			_log.info("Nothing to publish.");
		}
	}

	/**
	 * @param articleId
	 * @return
	 * @throws NoSuchMethodException 
	 * @throws ServiceError 
	 * @throws SecurityException 
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	private boolean isAvailable(long groupId, String articleId)
	{
		boolean available 					= false;
		String url 							= "";
		HttpURLConnection httpConnection 	= null;
		
		if (_log.isDebugEnabled())
			_log.debug( String.format("Community Publisher: Checking if article %s is available", articleId) );
		
		try
		{
			String[] masterList = ApacheHierarchy.getInstance().getMasterList();
			ErrorRaiser.throwIfFalse( masterList.length > 0, IterErrorKeys.XYZ_E_APACHE_MASTERS_NOT_FOUND_ZYX);
			url = masterList[0].concat(IterURLUtil.getArticleURL(groupId, articleId, null, true, false));
			
			String virtualHost = LayoutSetLocalServiceUtil.getLayoutSet(groupId, false).getVirtualHost();
			ErrorRaiser.throwIfFalse( Validator.isNotNull(virtualHost), IterErrorKeys.XYZ_E_UNDEFINED_VIRTUALHOST_ZYX);

			httpConnection = (HttpURLConnection)(new URL(url).openConnection());
            httpConnection.setConnectTimeout(	ApacheUtil.getApacheConnTimeout());
            httpConnection.setReadTimeout(	 	ApacheUtil.getApacheReadTimeout());
            httpConnection.setRequestProperty (	WebKeys.HOST, 	virtualHost);
            httpConnection.setRequestProperty ("User-Agent", 	WebKeys.USER_AGENT_ITERWEBCMS);

           	httpConnection.setRequestMethod(HttpMethods.HEAD);
            httpConnection.connect();
			
            HttpUtil.throwIfConnectionFailed( httpConnection, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX );
            StreamUtil.toString(httpConnection.getInputStream(), StringPool.UTF8);
            
            available = true;
		}
		catch (Throwable th)
		{
			String msg = String.format("Community Publisher: Unable to check if article %s is available (%s)", articleId, url);
			_log.error(msg, th);
		}
		finally
		{
			if (httpConnection != null)
				httpConnection.disconnect();
		}

		if (_log.isDebugEnabled())
			_log.debug( String.format("Community Publisher: Checked if article %s is available (%s)", articleId, String.valueOf(available)) );

		return available;
	}
	
	/**
	 * <p>Efectua la publicación en la red social usando el CommunityManager adecuado.</p>
	 * 
	 * @param groupId     El grupo que realiza la publicación.
	 * @param articleId   El artículo a publicar.
	 * @param accountType La red social en la que publicar.
	 * @param title       El título a usar en la publicación.
	 * @param credentials Las credenciales de publicación de la red social.
	 */
	private String publish(long groupId, String articleId, String accountId, String accountName, String accountType, String title, String credentials)
	{
		String processId = null;
		
		try
		{
			// Crea la configuración de la publicación.
			HashMap<String, String> communityParams = new HashMap<String, String>();
			communityParams.put("groupId", String.valueOf(groupId));
			communityParams.put("accountId", accountId);
			communityParams.put("accountName", accountName);
			communityParams.put("communityName", accountType);
			communityParams.put("title", title);
			communityParams.put("credentials", credentials);
			
			// Publica el artículo.
			CommunityFactory.getCommunity(communityParams).publish(articleId, communityParams);
			
			// Algunas publicaciones como los Facebook IA tardan en procesarse. Si hay processId lo retorna.
			processId = communityParams.get("processId");
		}
		catch (Throwable th)
		{
			IterMonitor.logEvent(groupId, Event.ERROR, new Date(), "Community Publisher: Unknow social network", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, th));
			_log.error(th);
		}
		
		return processId;
	}
}
