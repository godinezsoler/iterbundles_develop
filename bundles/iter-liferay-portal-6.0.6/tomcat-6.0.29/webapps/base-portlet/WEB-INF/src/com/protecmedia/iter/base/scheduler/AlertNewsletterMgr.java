package com.protecmedia.iter.base.scheduler;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import com.liferay.portal.apache.ApacheHierarchy;
import com.liferay.portal.apache.ApacheUtil;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.metrics.NewsletterMASTools;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;

/**
 * <p>Gestor de env�os de newsletters.</p>
 * <p>Sincroniza los accesos a las colas de env�os y genera el contenido de las newsletters a enviar.</p>
 * <table border="1">
 * <tr><td>{@link #addToQueue}</td><td>A�ade el env�o de un bolet�n a la cola correspondiente a la newsletter.</td></tr>
 * <tr><td>{@link #notifyFinish}</td><td>Elimina la primera tarea de la cola de la newsletter y arranca la siguiente.</td></tr>
 * </table>
 * <br />
 * @author Protecmedia
 * @see com.protecmedia.iter.base.service.impl.NewsletterMgrLocalServiceImpl#sendAlertNewsletters
 * @see com.protecmedia.iter.base.scheduler.AlertNewsletterTask
 */
public class AlertNewsletterMgr
{
	/** Logger. */
	private static Log log = LogFactoryUtil.getLog(AlertNewsletterMgr.class);

	/** Mapa que contiene una cola de {@code AlertNewsletterTask} para cada {@code scheduleId}. */
	private static Map<String, Queue<AlertNewsletterTask>> newsletterQueueMap = new HashMap<String, Queue<AlertNewsletterTask>>();

	/**
	 * Sentencia {@code SQL} para recuperar el {@code groupid}, el {@code subject}, el {@code newsletterid}r y la {@code friendlyURL} del modelo de una programaci�n de newsletter.
	 * Se usa para generar el contenido {@code HTML} de una newsletter.
	 */
	private static final String SQL_SELECT_SCHEDULE_NEWSLETTER = new StringBuilder()
		.append(" SELECT n.groupid, n.subject, n.newsletterid, l.friendlyURL, \n")
		.append(" 		 n.name newsletterName, sn.name scheduleName 		 \n")
		.append(" FROM schedule_newsletter sn                                \n")
		.append(" INNER JOIN newsletter n ON n.newsletterid=sn.newsletterid  \n")
		.append(" INNER JOIN Layout l ON l.plid = n.plid                     \n")
		.append(" WHERE sn.scheduleid = '%s'                                 \n")
		.toString();
	
	/**
	 * <p>A�ade un env�o de newsletter a la cola correspondiente.</p>
	 * 
	 * <p>El proceso es el siguiente:</p>
	 * <ol>
	 * <li>Genera el contenido de la newsletter.</li>
	 * <li>Crea una tarea de env�o {@code AlertNewsletterTask} y la a�ade a la cola de la {@code scheduleId} en orden {@code FIFO}.</li>
	 * <li>Si es la �nica tarea de la cola, la arranca.</li>
	 * </ol>
	 * 
	 * @param scheduleId El identificador de la programaci�n de la newsletter.
	 * @param lastUpdate Los milisegundos correspondientes a la �ltima fecha de publicaci�n del grupo.
	 * @see com.protecmedia.iter.base.scheduler.AlertNewsletterTask
	 */
	public static void addToQueue(String scheduleId, String lastUpdate)
	{
		// Serializa el acceso tanto al mapa de tareas como a la tabla
		synchronized (newsletterQueueMap)
		{
			log.info("Adding newsletter to the queue: Schedule ".concat(scheduleId));
			
			// Genera el contenido
			String content = generateContent(scheduleId, lastUpdate);
			
			// Obtiene la cola de env�os de la newsletter
			Queue<AlertNewsletterTask> newsletterQueue = getNewsletterQueue(scheduleId);
			
			// Crea una tarea nueva
			AlertNewsletterTask newsletterTask = new AlertNewsletterTask(scheduleId, lastUpdate, content);
			newsletterQueue.add(newsletterTask);
			
			// Si es la �nica tarea, la arranca
			if (newsletterQueue.size() == 1)
				newsletterTask.start();
		}
	}
	
	/**
	 * <p>Informa al controlador de que una tarea de env�o ha finalizado.</p>
	 * 
	 * <p>Es llamada por las propias tareas de env�o {@code AlertNewsletterTask}, para notificar que han terminado.</p>
	 * <ol>
	 * <li>Elimina de la cola de la {@code scheduleId} la primera tarea, que es la que se est� ejecutando.</li>
	 * <li>Si hay m�s tareas encoladas, arranca la siguiente en orden {@code FIFO}.</li>
	 * </ol>
	 * 
	 * @param scheduleId El identificador de la programaci�n de la newsletter.
	 * @param lastUpdate Los milisegundos correspondientes a la �ltima fecha de publicaci�n del grupo.
	 * @see com.protecmedia.iter.base.scheduler.AlertNewsletterTask
	 */
	public static void notifyFinish(String scheduleId, String lastUpdate)
	{
		// Serializa el acceso tanto al mapa de tareas como a la tabla
		synchronized (newsletterQueueMap)
		{
			// Obtiene la cola del grupo
			Queue<AlertNewsletterTask> groupQueue = newsletterQueueMap.get(scheduleId);
			
			if (groupQueue != null)
			{
				// Elimina el elemento que estaba en ejecuci�n
				groupQueue.remove();
				
				// Si hay m�s tareas encoladas, arranca la siguiente
				if (groupQueue.size() > 0)
					groupQueue.peek().start();
			}
		}
	}
	
	/**
	 * Limpia las colas de todas las newsletter de tipo Alert 
	 */
	public static void stop()
	{
		synchronized (newsletterQueueMap)
		{
			log.debug("Stopping alert newsletters");
			for (Map.Entry<String, Queue<AlertNewsletterTask>> entry : newsletterQueueMap.entrySet())
			{
				// Obtiene la cola del grupo
				Queue<AlertNewsletterTask> groupQueue = entry.getValue();
				
				if (groupQueue != null)
				{
					// Vacia la cola
					groupQueue.clear();
				}

			}
		}

	}
	
	/**
	 * <p>Retorna la cola correspondiente a la {@code scheduleId}.</p>
	 * <p>Si no est� inicializada, crea una nueva y la a�ade al mapa de {@code [scheduleId|Queue<AlertNewsletterTask>]}</p>
	 * 
	 * @param scheduleId El identificador de la programaci�n de la newsletter.
	 * @return {@code Queue<AlertNewsletterTask>} correspondiente a la {@code scheduleId}.
	 */
	private static Queue<AlertNewsletterTask> getNewsletterQueue(String scheduleId)
	{
		// Obtiene la cola de env�os de la newsletter
		Queue<AlertNewsletterTask> newsletterQueue = newsletterQueueMap.get(scheduleId);
		
		// Si no existe, la crea 
		if (newsletterQueue == null)
		{
			newsletterQueueMap.put(scheduleId, new LinkedList<AlertNewsletterTask>());
			newsletterQueue = newsletterQueueMap.get(scheduleId);
		}
		
		return newsletterQueue;
	}
	
	/**
	 * <p>Genera el contenido {@code HTML} de la newsletter.</p>
	 * 
	 * <p>Para ello, recupera de BBDD el Id de la newsletter, el grupo al que pertenece, la friendlyUrl del modelo usado y el asunto,
	 * y llama a {@code getURLContent()} para generar el c�digo {@code HTML} del bolet�n.</p>
	 * 
	 * @param scheduleId El identificador de la programaci�n de la newsletter.
	 * @param lastUpdate Los milisegundos correspondientes a la �ltima fecha de publicaci�n del grupo.
	 * @return {@code String} con el contenido de la newsletter.
	 * @see AlertNewsletterMgr#getURLContent
	 */
	private static String generateContent(String scheduleId, String lastUpdate)
	{
		String content = null;
		Node scheduleNode = null;
		try
		{
			Element scheduleRoot = PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_SELECT_SCHEDULE_NEWSLETTER, scheduleId)).getRootElement();
			scheduleNode = scheduleRoot.selectSingleNode("/rs/row");
		}
		catch (Throwable th)
		{
			log.error(th);
		}
		
		if(scheduleNode != null)
		{
			int groupid             = GetterUtil.getInteger(XMLHelper.getTextValueOf(scheduleNode, "@groupid"));
			String newsletterId     = XMLHelper.getTextValueOf(scheduleNode, "@newsletterid");
			String friendlyURL      = XMLHelper.getTextValueOf(scheduleNode, "@friendlyURL");
			String friendlyURLTyped = new StringBuilder(friendlyURL).append("?type=alert").toString();
			StringBuilder subject   = new StringBuilder( GetterUtil.get(XMLHelper.getTextValueOf(scheduleNode, "@subject"), StringPool.BLANK) );
			
			String newsletterName	= XMLHelper.getTextValueOf(scheduleNode, "@newsletterName");
			String scheduleName		= XMLHelper.getTextValueOf(scheduleNode, "@scheduleName");
			
			try
			{
				content = getURLContent(newsletterId, groupid, friendlyURLTyped, lastUpdate, subject, newsletterName, scheduleName);
			}
			catch (Throwable th)
			{
				log.error(th);
			}
		}
		
		return content;
	}
	
	/**
	 * <p>Pide la newsletter a trav�s del {@code Apache} para generar el contenido {@code HTML}.</p>
	 * 
	 * @param newsletterId El Id de la newsletter.
	 * @param groupid      El grupo al que pertenece la newsletter.
	 * @param friendlyURL  La friendlyUrl del modelo de la newsletter.
	 * @param lastUpdate   La fecha de �ltima publicaci�n del grupo en milisegundos.
	 * @param subject      El asunto correspondiente a la newsletter.
	 * @return {@code String} con el c�digo {@code HTML} de la newsletter.
	 * @throws IOException     Si no puede recuperar la jerarqu�a de servidores Apache.
	 * @throws PortalException Si no puede recuperar la jerarqu�a de servidores Apache.
	 * @throws SystemException Si ocurre un error al recuperar el {@code Virtual Host} del grupo {@code groupId}.
	 * @throws ServiceError    {@code XYZ_E_INVALIDARG_ZYX} si no hay servidores Apaches configurados. 
	 */
	private static String getURLContent(String newsletterId, long groupid, String friendlyURL, String lastUpdate, StringBuilder subject,
										String newsletterName, String scheduleName) throws IOException, PortalException, SystemException, ServiceError
	{
		StringBuffer html = new StringBuffer();
		
		int port = GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SERVER_PORT), 80);
		String virtualhost = LayoutSetLocalServiceUtil.getLayoutSet(groupid, false).getVirtualHost();
		String url = virtualhost;
		
		if(IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_LIVE))
		{
			ApacheHierarchy apacheHierarchy = new ApacheHierarchy();
			String[] masterList = apacheHierarchy.getMasterList();
			if(masterList == null || masterList.length == 0)
				log.error("No master Apache available");
			
			ErrorRaiser.throwIfFalse((masterList != null && masterList.length > 0), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
				
			url = new ApacheHierarchy().getMasterList()[0];
		}
		
		if(port != 80)
			url += StringPool.COLON + port;

		url += friendlyURL;
		
		if(!url.contains(Http.HTTP_WITH_SLASH))
			url = Http.HTTP_WITH_SLASH + url;
		
		if(log.isTraceEnabled())
		{
			log.trace("Current URL Newsletter: " + url);
			log.trace("Virtualhost: " + virtualhost);
		}
		
		URLConnection httpConnection = (HttpURLConnection)(new URL(url).openConnection());

		try
		{
			httpConnection.setConnectTimeout(ApacheUtil.getApacheConnTimeout());
			httpConnection.setReadTimeout(ApacheUtil.getApacheReadTimeout());
			
			httpConnection.setRequestProperty(WebKeys.REQUEST_HEADER_ITS_NEWSLETTER, "true");
			
			// Se a�ade, si procede, la informaci�n de la campa�a para poder marcar las URLs de la newsletter.
			NewsletterMASTools.setCampaignValue(httpConnection, groupid, newsletterName, scheduleName);
			
			if(IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_LIVE))
			{
				httpConnection.setRequestProperty(WebKeys.HOST, virtualhost);
				httpConnection.setRequestProperty("User-Agent", WebKeys.ITER_FULL);
			}
			
			if(Validator.isNotNull(lastUpdate))
				httpConnection.setRequestProperty(WebKeys.REQUEST_HEADER_PUB_DATE_BEFORE_CURRENT, lastUpdate);

			httpConnection.connect();
			
			HttpUtil.throwIfConnectionFailed( (HttpURLConnection)httpConnection, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX );
			html.append( StreamUtil.toString(httpConnection.getInputStream(), StringPool.UTF8) );
			
			Boolean discard = GetterUtil.getBoolean(httpConnection.getHeaderField(WebKeys.RESPONSE_HEADER_DISCARD_RESPONSE), false);
			if(discard)
				html = new StringBuffer();
			
			String newsletterSubj = httpConnection.getHeaderField(WebKeys.REQUEST_HEADER_NEWSLETTER_SUBJECT);
			if (Validator.isNotNull(newsletterSubj))
				subject.replace(0, subject.length(), newsletterSubj);
			
			if(log.isDebugEnabled() && discard)
				log.info("Content will be discarded");
		}
		catch(Exception e)
		{
			html = new StringBuffer();
			
			log.error(e.toString());
			log.trace(e);
		}
		
		return html.toString();
	}
}
