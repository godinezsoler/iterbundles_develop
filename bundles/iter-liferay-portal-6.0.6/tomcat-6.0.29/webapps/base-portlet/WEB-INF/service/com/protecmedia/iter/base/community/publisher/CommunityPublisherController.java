package com.protecmedia.iter.base.community.publisher;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.codec.binary.Base64;

import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.IterMonitor.Event;
import com.protecmedia.iter.base.service.affinity.IServerAffinityProcess;
import com.protecmedia.iter.base.service.util.GroupMgr;

public enum CommunityPublisherController implements IServerAffinityProcess
{
	INSTANCE;
	
	private static Log _log = LogFactoryUtil.getLog(CommunityPublisherController.class);

	/** Candado para el acceso a BBDD */
	private final Lock BDLock = new ReentrantLock();
	/** Hilo encargado de publicar los contenidos */
	private static CommunityPublisherProcess PUBLISHER;
	
	private AtomicBoolean active = new AtomicBoolean(false);
	
	////////////////////////////////////////////////////////////////////////////////
	//               CONTROL DEL PROCESO DE PUBLICACIÓN AUTOMÁTICA                //
	////////////////////////////////////////////////////////////////////////////////
	@Override
	synchronized public void start() throws ServiceError
	{
		if (!active.get())
		{
			_log.info("Starting Community Publisher...");
			active.set(true);
			// Proceso de publicación
			if (!isRunning())
			{
				PUBLISHER = new CommunityPublisherProcess(BDLock);
				PUBLISHER.start();
			}
			// Inicializa el proceso de comprobación del estado de Instant Articles
			initializeIAProcessChecker();
		}
		else
		{
			_log.info("Publisher is already running!");
		}
	}
	
	@Override
	synchronized public void halt()
	{
		if (active.get())
		{
			_log.info("Stopping Community Publisher...");
			active.set(false);
			// Cancela la publicación
			if (isRunning())
			{
				PUBLISHER.halt();
				try
				{
					PUBLISHER.join();
				}
				catch (InterruptedException e)
				{
					_log.debug("Interrupted while waiting for Community Publisher halt!");
				}
			}
			// Cancela los procesos de comprobación de estado de instants articles
			cancelAllProcessCheckers();
			_log.info("Community Publisher stopped");
		}
		else
		{
			_log.info("Publisher is already stopped!");
		}
	}
	
	/**
	 * <p>Informa si el controlador es el responsable actual de gestionar las publicaciones.</p>
	 * <p>El controlador es el responsable si se está ejecutando en el servidor de afinidad de la tarea.</p>
	 * @return
	 * @throws NoSuchMethodException 
	 * @throws ServiceError 
	 * @throws SecurityException 
	 */
	public boolean isActive()
	{
		return active.get();
	}
	
	/**
	 * <p>Informa si el proceso de publicación está corriendo en el servidor.</p>
	 * <p>El Proceso de Publicación está corriendo si el hilo está vivo, no se ha solicitado parada y tiene publicaciones pendientes.</p> 
	 * @return
	 */
	public boolean isRunning()
	{
		return PUBLISHER != null && PUBLISHER.isAlive() && PUBLISHER.isRunning();
	}
	
	////////////////////////////////////////////////////////////////////////////////
	//              GESTIÓN DE LAS PROGRAMACIONES DE PUBLICACIONES                //
	////////////////////////////////////////////////////////////////////////////////
	private static final String SQL_CANCEL_SCHEDULED_PUBLICATION_DELETE = "DELETE FROM schedule_publication WHERE ";
	private static final String SQL_CANCEL_SCHEDULED_PUBLICATION_WHERE_FROM_MONITOR  = "publicationId IN (%s)";
	private static final String SQL_CANCEL_SCHEDULED_PUBLICATION_FROM_PUBLISH        = "(groupId=%d AND articleId='%s' AND accountId='%s')";
	
	private static final String SQL_INSERT_SCHEDULED_PUBLICATION = " INSERT INTO schedule_publication (publicationId, groupId, articleId, title, accountId, accountName, accountType, schedule, originalSchedule, credentials) VALUES \n";
	private static final String SQL_INSERT_SCHEDULED_PUBLICATION_VALUES = " (UUID(), %d, '%s', %s, '%s', '%s', '%s', '%s', %s, '%s') \n";
	private static final String SQL_INSERT_SCHEDULED_PUBLICATION_UPDATE =" ON DUPLICATE KEY UPDATE title=VALUES(title), accountType=VALUES(accountType), schedule=VALUES(schedule), originalSchedule=VALUES(originalSchedule), credentials=VALUES(credentials)";
	
	private static final String SQL_UPDATE_SCHEDULED_PUBLICATION = "INSERT INTO schedule_publication (publicationId, groupId, articleId, accountId, accountName, accountType, schedule, credentials) VALUES \n";
	private static final String SQL_UPDATE_SCHEDULED_PUBLICATION_VALUES = " ('%s', %d, '%s', 'dummy', 'dummy', 'dummy', '%s', 'dummy') \n";
	private static final String SQL_UPDATE_SCHEDULED_PUBLICATION_UPDATE = "ON DUPLICATE KEY UPDATE schedule=VALUES(schedule)";
	
	private static final String SQL_GET_PUBLICATION_IN_PROGRESS = "SELECT publicationId, processId FROM schedule_publication WHERE groupId=%d AND articleId='%s' AND accountId='%s' AND processId IS NOT NULL";
	private static final String SQL_ABORT_PUBLICATION_IN_PROGRESS = "UPDATE schedule_publication SET processId = NULL WHERE publicationId='%s'";
	
	public void updateSchedule(Document scheduleChanges)
	{
		// Valida el XML.
		if (scheduleChanges != null)
		{
			// Procesa las cancelaciones.
			String deleteSQL = processScheduleCancellations(scheduleChanges);
			
			// Procesa las inserciones.
			String insertSQL = processScheduleInserts(scheduleChanges);
			
			// Procesa las actualizaciones.
			String updateSQL = processScheduleUpdates(scheduleChanges);
			
			// Actualiza las programaciones en BBDD y replanifica el proceso de publicación.
			updateScheduledPublications(deleteSQL, insertSQL, updateSQL);
		}
		else
		{
			_log.error("Invalid schedule changes document.");
		}
	}
	
	private void updateScheduledPublications(String deleteSQL, String insertSQL, String updateSQL)
	{
		BDLock.lock();
		try
		{
			boolean deletes, inserts, updates = false;
			
			// Realiza las cancelaciones
			deletes = executeSQL(deleteSQL);
			
			// Realiza las inserciones
			inserts = executeSQL(insertSQL);
			
			// Realiza las modificaciones
			updates = executeSQL(updateSQL);
			
			// Bloque sincronizado para evitar que entre en conflicto con el arranque o la parada del proceso
			synchronized(this)
			{
				// Si el servicio está arrancado y hay cambios
				if (isActive() && (deletes || inserts || updates))
				{
					// Si el publicador está corriendo, le informa de que debe replanificarse.
					if (isRunning())
						PUBLISHER.reschedule();
					// Puede que el publicador esté parado si no tenía más publicaciones. En tal caso lo arranca.
					else
					{
						PUBLISHER = new CommunityPublisherProcess(BDLock);
						PUBLISHER.start();
					}
				}
			}
		}
		finally
		{
			BDLock.unlock();
		}
	}
	
	private boolean executeSQL(String query)
	{
		boolean changes = false;
		if (Validator.isNotNull(query))
		{
			try
			{
				PortalLocalServiceUtil.executeUpdateQuery(query);
				changes = true;
			}
			catch (Throwable th)
			{
				IterMonitor.logEvent(GroupMgr.getGlobalGroupId(), Event.ERROR, new Date(), "Community Publisher: Unable to update schedule changes", th);
				_log.error(th);
			}
		}
		return changes;
	}
	
	private String processScheduleCancellations(Document scheduleChanges)
	{
		StringBuilder deleteSQL = new StringBuilder();
		
		String rootName = scheduleChanges.getRootElement().getName();
		// Si la raiz es <rs>, es una cancelación desde IterAdmin y vienen los Ids.
		if ("rs".equals(rootName))
		{
			String[] nodeIds = XMLHelper.getStringValues(scheduleChanges.selectNodes("/rs/row"), "@id");
			if (nodeIds.length > 0)
			{
				String publicationIds = String.format(SQL_CANCEL_SCHEDULED_PUBLICATION_WHERE_FROM_MONITOR, StringUtil.merge(nodeIds, StringPool.COMMA_AND_SPACE, StringPool.APOSTROPHE));
				deleteSQL.append(SQL_CANCEL_SCHEDULED_PUBLICATION_DELETE).append(publicationIds);
			}
		}
		// Si la raiz es <publications>, es una moficiación desde la publicación.
		else if ("publications".equals(rootName))
		{
			List<Node> cancellations = scheduleChanges.selectNodes("/publications/publication[@cancel='1' or @cancel='true' or @cancel='y' or @cancel='t' or @cancel='on']");
			if (cancellations.size() > 0)
			{
				deleteSQL.append(SQL_CANCEL_SCHEDULED_PUBLICATION_DELETE);
				
				for (Node cancelation : cancellations)
				{
					long groupId       = XMLHelper.getLongValueOf(cancelation, "@groupid", 0);
					String articleId   = XMLHelper.getStringValueOf(cancelation, "@articleid");
					String accountId   = XMLHelper.getStringValueOf(cancelation, "@accountid");
					
					if (!SQL_CANCEL_SCHEDULED_PUBLICATION_DELETE.equals(deleteSQL.toString()))
						deleteSQL.append(" OR ");
					deleteSQL.append(String.format(SQL_CANCEL_SCHEDULED_PUBLICATION_FROM_PUBLISH, groupId, articleId, accountId));
				}
			}
		}
		return deleteSQL.toString();
	}
	
	private String processScheduleInserts(Document scheduleChanges)
	{
		StringBuilder insertSQL = new StringBuilder();
		
		List<Node> publications = scheduleChanges.selectNodes("/publications/publication[not(@cancel='1' or @cancel='true' or @cancel='y' or @cancel='t' or @cancel='on')]");
		
		if (publications.size() > 0)
		{
			insertSQL.append(SQL_INSERT_SCHEDULED_PUBLICATION);
			
			for (Node publication : publications)
			{
				long groupId            = XMLHelper.getLongValueOf(publication, "@groupid", 0);
				String articleId        = XMLHelper.getStringValueOf(publication, "@articleid");
				String accountId        = XMLHelper.getStringValueOf(publication, "@accountid");
				String accountName      = XMLHelper.getStringValueOf(publication, "@accountname");
				String originalSchedule = XMLHelper.getStringValueOf(publication, "@schedule");
				String credentials      = XMLHelper.getStringValueOf(publication, "@credentials");
				String community        = XMLHelper.getStringValueOf(publication, "@accounttype");
				String title            = XMLHelper.getStringValueOf(publication, "title");
				if (Validator.isNull(title))
					title = "NULL";
				else
				{
					try
					{
						title = new String(Base64.decodeBase64(title), StringPool.UTF8);
						title = title.replaceAll(StringPool.APOSTROPHE, StringPool.DOUBLE_APOSTROPHE);
						title = StringUtil.apostrophe(title);
					}
					catch (UnsupportedEncodingException e)
					{
						IterMonitor.logEvent(GroupMgr.getGlobalGroupId(), Event.WARNING, new Date(), "Community Publisher: Unable to decode custom title [" + title + "] for article " + articleId, articleId, e);
						title = "NULL";
					}
				}
				
				// Se genera el accountType
				String accountType = getCalculatedAccountType(community, credentials);
				
				try
				{
					// Calcula la planificación en función de las vigencias
					String schedule = getCalculatedSchedule(groupId, articleId, originalSchedule);
					originalSchedule = originalSchedule == null ? StringPool.NULL : StringUtil.apostrophe(originalSchedule);
				
					// Crea la inserción
					if (!insertSQL.toString().equals(SQL_INSERT_SCHEDULED_PUBLICATION))
					{
						insertSQL.append(StringPool.COMMA);
					}
					insertSQL.append(String.format(SQL_INSERT_SCHEDULED_PUBLICATION_VALUES, groupId, articleId, title, accountId, accountName, accountType, schedule, originalSchedule, credentials));
					
					// Comprueba si es un Instant Article ya publicado que está en proceso. Si es así lo aborta para volver a publicarlo.
					Document publicationInProgress = PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_GET_PUBLICATION_IN_PROGRESS, groupId, articleId, accountId));
					String publicationId = XMLHelper.getStringValueOf(publicationInProgress.selectSingleNode("/rs/row"), "@publicationId");
					
					if (Validator.isNotNull(publicationId))
					{
						IterMonitor.logEvent(GroupMgr.getGlobalGroupId(), Event.WARNING, new Date(), articleId, "Community Publisher: Instant Article publication is still in progress for article " + articleId + " and will be override.");
						String processId = XMLHelper.getStringValueOf(publicationInProgress.selectSingleNode("/rs/row"), "@processId");
						PortalLocalServiceUtil.executeUpdateQuery(String.format(SQL_ABORT_PUBLICATION_IN_PROGRESS, publicationId));
						cancelProcessChecker(processId);
					}
				}
				catch (Exception e)
				{
					IterMonitor.logEvent(GroupMgr.getGlobalGroupId(), Event.ERROR, new Date(), "Community Publisher: Unable to update schedule changes for article " + articleId, articleId, e);
				}
			}
			
			insertSQL.append(SQL_INSERT_SCHEDULED_PUBLICATION_UPDATE);
		}
		
		return insertSQL.toString();
	}
	
	private String processScheduleUpdates(Document scheduleChanges)
	{
		StringBuilder updateSQL = new StringBuilder();
		
		List<Node> publications = scheduleChanges.selectNodes("/publications/publicationChange");
		
		if (publications.size() > 0)
		{
			
			updateSQL.append(SQL_UPDATE_SCHEDULED_PUBLICATION);
			
			for (Node publication : publications)
			{
				String publicationId    = XMLHelper.getStringValueOf(publication, "@publicationId");
				long groupId            = XMLHelper.getLongValueOf(publication, "@groupId", 0);
				String articleId        = XMLHelper.getStringValueOf(publication, "@articleId");
				String originalSchedule = XMLHelper.getStringValueOf(publication, "@originalSchedule");
				String currentSchedule  = XMLHelper.getStringValueOf(publication, "@schedule");
				
				try
				{	
					// Calcula la planificación en función de las vigencias
					String schedule = getCalculatedSchedule(groupId, articleId, originalSchedule);
					
					if (!schedule.equals(currentSchedule))
					{
						if (!updateSQL.toString().equals(SQL_UPDATE_SCHEDULED_PUBLICATION))
						{
							updateSQL.append(StringPool.COMMA);
						}
						
						updateSQL.append(String.format(SQL_UPDATE_SCHEDULED_PUBLICATION_VALUES, publicationId, groupId, articleId, schedule));
					}
				}
				catch (Exception e)
				{
					IterMonitor.logEvent(GroupMgr.getGlobalGroupId(), Event.ERROR, new Date(), "Community Publisher: Unable to update schedule changes for publication " + publicationId, articleId, e);
				}
			}
			
			updateSQL.append(SQL_UPDATE_SCHEDULED_PUBLICATION_UPDATE);
		}
		
		return updateSQL.toString();
	}
	
	
	private String getCalculatedAccountType(String community, String credentials)
	{
		// Se genera el accountType
		String accountType = Character.toUpperCase(community.charAt(0)) + community.substring(1).toLowerCase();

		if ("facebook".equals(community))
		{
			String[] credentialsInfo = credentials.split(StringPool.COMMA);
			if (credentialsInfo.length == 3 && credentialsInfo[2].startsWith("InstantArticle"))
				accountType = "Instant Article";
			else
				accountType = "Facebook";
		}
		
		return accountType;
	}
	
	private String getCalculatedSchedule(long groupId, String articleId, String originalSchedule) throws ParseException, SystemException, SecurityException, NoSuchMethodException, ClassNotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException
	{
		// Calcula la planificación en función de las vigencias
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date proposedSchedule = null;
		Date finalSchedule = null;
		
		// Calcula la fecha propuesta
		if (Validator.isNull(originalSchedule))
		{
			proposedSchedule = new Date();
		}
		else
		{
			proposedSchedule = df.parse(originalSchedule);
		}
		
		Class<?> clazz = Class.forName("com.protecmedia.iter.news.service.PageContentLocalServiceUtil");
		
		Object[] methodParams = new Object[2];
		methodParams[0] = groupId;
		methodParams[1] = articleId;
		
		Method method = clazz.getMethod("getPageContents", long.class, java.lang.String.class);
		@SuppressWarnings("unchecked")
		List<BaseModel<?>> pcs = (List<BaseModel<?>>) method.invoke(clazz, methodParams);
		
		for (BaseModel<?> pc : pcs)
		{
			Method method2 = pc.getClass().getMethod("getVigenciadesde");
			Date vigenciadesde = (java.util.Date) method2.invoke(pc);
			
			// Si es la primera o es anterior a la actual, se queda con esta fecha de vigencia
			if (finalSchedule == null || vigenciadesde.before(finalSchedule))
				finalSchedule = vigenciadesde;
		}
		
		// Si la fecha propuesta es posterior a la fecha de vigencia desde, se queda con la propuesta
		if (proposedSchedule.after(finalSchedule))
			finalSchedule = proposedSchedule;
		
		return df.format(finalSchedule);
	}

	private static HashMap<String, CommunityPublisherProgressChecker> _publicationsInProgressMap = new HashMap<String, CommunityPublisherProgressChecker>();
	
	private static ReentrantReadWriteLock _lockPublications = new ReentrantReadWriteLock();
	private static Lock _writeLockPublications = _lockPublications.writeLock();
	
	private void initializeIAProcessChecker()
	{
		try
		{
			Document publicationsInProgress = PortalLocalServiceUtil.executeQueryAsDom("SELECT processId FROM schedule_publication WHERE processId IS NOT NULL");
			String[] processIds = XMLHelper.getStringValues(publicationsInProgress.selectNodes("/rs/row"), "@processId");
			
			if (processIds.length > 0)
			{
				for (String processId : processIds)
				{
					_writeLockPublications.lock();
					try
					{	
						CommunityPublisherProgressChecker processChecker = new CommunityPublisherProgressChecker(processId);
						_publicationsInProgressMap.put(processId, processChecker);
						processChecker.start();
					}
					catch (Throwable th)
					{
						_log.error(th);
					}
					finally
					{
						_writeLockPublications.unlock();
					}
				}
			}
		}
		catch (Throwable th)
		{
			_log.error("Unable to initialize Instant articles progress checkers");
		}
	}
	
	private void cleanFinishedPublications()
	{
		_writeLockPublications.lock();
		try
		{	
			Set<String> processIds = _publicationsInProgressMap.keySet();
			for (String processId : processIds)
			{
				if (!_publicationsInProgressMap.get(processId).isAlive())
					_publicationsInProgressMap.remove(processId);
			}
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
		finally
		{
			_writeLockPublications.unlock();
		}
	}
	
	public void startIAProcessChecker(String processId)
	{
		cleanFinishedPublications();
		
		_writeLockPublications.lock();
		try
		{	
			CommunityPublisherProgressChecker processChecker = _publicationsInProgressMap.get(processId);
			if (processChecker == null)
			{
				processChecker = new CommunityPublisherProgressChecker(processId);
				_publicationsInProgressMap.put(processId, processChecker);
				processChecker.start();
			}
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
		finally
		{
			_writeLockPublications.unlock();
		}
	}
	
	public void cancelProcessChecker(String processId)
	{
		cleanFinishedPublications();
		
		_writeLockPublications.lock();
		try
		{	
			CommunityPublisherProgressChecker processChecker = _publicationsInProgressMap.get(processId);
			if (processChecker != null)
			{
				processChecker.halt();
				_publicationsInProgressMap.remove(processId);
			}
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
		finally
		{
			_writeLockPublications.unlock();
		}
	}
	
	public void cancelAllProcessCheckers()
	{
		cleanFinishedPublications();
		
		_writeLockPublications.lock();
		try
		{	
			Set<String> processIds = _publicationsInProgressMap.keySet();
			for (String processId : processIds)
			{
				_publicationsInProgressMap.get(processId).halt();
				_publicationsInProgressMap.remove(processId);
			}
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
		finally
		{
			_writeLockPublications.unlock();
		}
	}
}
