package com.protecmedia.iter.base.service.apache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;

import com.liferay.portal.apache.ApacheUtil;
import com.liferay.portal.kernel.cluster.ClusterTools;
import com.liferay.portal.kernel.concurrent.ExtLinkedBlockingQueue;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.protecmedia.iter.base.cluster.Heartbeat;
import com.protecmedia.iter.base.service.util.HotConfigUtil;
import com.protecmedia.iter.base.service.util.IterKeys;


public class URIPteMgr 
{
	private static final Log _log 		= LogFactoryUtil.getLog(URIPteMgr.class);
	private static final Log _logPause  = LogFactoryUtil.getLog(URIPteMgr.class.getName() + ".PauseProducer");
	
	private static Lock 							_lock 				= new ReentrantLock();
	private static ExecutorService 					_executorService 	= null;
	private static ExtLinkedBlockingQueue<URIPte> 	_queue				= null;
	private static URIPteProducer					_producer			= null;
	
	static public void launchURIPteProcess() throws com.liferay.portal.kernel.error.ServiceError
	{
		_lock.lock();
		try
		{
			ErrorRaiser.throwIfFalse( !isInProcess(), IterErrorKeys.XYZ_E_URIPTES_IS_ALREADY_RUNNING_ZYX ); 
			
			if (canLaunchURIPteProcess())
			{
				long maxOperations = HotConfigUtil.getKey(IterKeys.HOTCONFIG_KEY_APACHE_QUEUE_MAXOPERATIONS, 0);
				ErrorRaiser.throwIfFalse(maxOperations >= 0, IterErrorKeys.XYZ_E_APACHE_INVALID_MAXOPERATIONS_ZYX);
		
				_executorService = Executors.newFixedThreadPool( (int)(maxOperations+1) );
				
				_queue = new ExtLinkedBlockingQueue<URIPte>();
				
				_producer = new URIPteProducer(_queue);
				
				// Se crea el productor
				_executorService.execute( _producer );
			
				// Se crean los consumidores
				for (int i = 0; i < maxOperations; i++)
					_executorService.execute( new URIPteConsumer(_queue) );
				
				_log.info("The URIPtes mechanism has started");
			}
			else
			{
				_log.warn("The scheduler/heartbeat is disable in this server (scheduler.enabled/cluster.heartbeat)");
			}
		}
		finally
		{
			_lock.unlock();
		}
	}

	static public void pauseURIPteProducer(List<Long> groupIds) throws Exception
	{
		String groups = StringUtil.merge(groupIds);
		
		// 0010479: La caché de las páginas pre-cacheadas tardan demasiado tiempo en refrescarse.
		if (isInProcess())
			pauseURIPteProducerNonClustered(groups);
		else
			ClusterTools.notifyCluster(true, URIPteMgr.class.getName(), "pauseURIPteProducerNonClustered", new Object[] {groups});
	}
	static public void pauseURIPteProducerNonClustered(String groups)
	{
		// El pause estará deshabilitado en función de un nivel de trazas
		if (_logPause.isInfoEnabled())
		{
			_lock.lock();
			try
			{
				if (isInProcess())
				{
					_producer.pauseProducerSemaphore();
					
					clearQueue(groups);
					
					_log.debug("The URIPtes mechanism has been paused");
				}
				else
				{
					_log.debug("The URIPtes mechanism is not running");
				}
			}
			finally
			{
				_lock.unlock();
			}
		}
	}

	static private void clearQueue(String groups)
	{
		String[] groupIds = null;
		if (PropsValues.ITER_APACHE_QUEUE_DISCARD_URIPTES_BY_GROUP_ENABLED && Validator.isNotNull(groupIds = StringUtils.split(groups, ",")))
		{
			// Se eliminan los elementos asociados a los grupos de la lista
			List<String> groupIdsList = new ArrayList<String>(Arrays.asList(groupIds));
			
			if (_log.isDebugEnabled())
				_log.debug( String.format("Discarding URIPtes from %s %s", groups,
							groupIdsList.size() == 1 ? "group" : "groups") );
			
			Iterator<URIPte> iterator = _queue.iterator();
			while(iterator.hasNext())
			{
				URIPte uriPte = iterator.next();
				if ( groupIdsList.contains( String.valueOf(uriPte.getGroupId()) ) )
				{
					if (_log.isTraceEnabled())
						_log.trace( String.format("Discarding URIPte: %s", uriPte.toString()) );
					
					_queue.remove(uriPte);
				}
			}
		}
		else
		{
			_log.debug("Discarding all URIPtes");
			_queue.clear();
		}
	}
	
	static public void restartURIPteProducer() throws Exception
	{
		// 0010479: La caché de las páginas pre-cacheadas tardan demasiado tiempo en refrescarse.
		if (isInProcess())
			restartURIPteProducerNonClustered();
		else
			ClusterTools.notifyCluster(true, URIPteMgr.class.getName(), "restartURIPteProducerNonClustered");
	}
	static public void restartURIPteProducerNonClustered()
	{
		// El restart estará deshabilitado en función de un nivel de trazas
		if (_logPause.isInfoEnabled())
		{
			_lock.lock();
			try
			{
				if (isInProcess())
				{
					_producer.restartProducerSemaphore();
					_log.debug("The URIPtes mechanism has been restarted");
				}
				else
				{
					_log.debug("The URIPtes mechanism is not running");
				}
			}
			finally
			{
				_lock.unlock();
			}
		}
	}
	
	static public boolean stopURIPteProcess() throws ServiceError, InterruptedException
	{
		boolean stopOK = false;
		
		_lock.lock();
		try
		{
			ErrorRaiser.throwIfFalse( isInProcess(), IterErrorKeys.XYZ_E_URIPTES_IS_NOT_RUNNING_ZYX ); 
			
			try
			{
				_executorService.shutdownNow();
				
				// El timeout será lo máximo que podría tardar una comunicación con Apache (tiempo de conexión + tiempo de lectura),
				// más un tiempo extra de margen (por ejemplo el empleado en consumir la respuesta del Apache)
				long timeout = 100 + ApacheUtil.getApacheConnTimeout() + ApacheUtil.getApacheReadTimeout();
				stopOK = _executorService.awaitTermination(timeout, TimeUnit.MILLISECONDS);
			}
			finally
			{
				if (!stopOK)
					_log.error("The URIPtes mechanism is still in process");
				else
				{
					_log.info("The URIPtes mechanism has finished");
	
					_queue = null;
					_producer = null;
					_executorService = null;
				}
			}
		}
		finally
		{
			_lock.unlock();
		}

		return stopOK;
	}
	
	static private boolean isInProcess()
	{
		boolean inProcess = false;
		
		_lock.lock();
		try
		{
			inProcess = _queue != null && _producer != null && _executorService != null && !_executorService.isShutdown() && !_executorService.isTerminated();
		}
		finally
		{
			_lock.unlock();
		}

		return inProcess;
	}
	
	static public boolean canLaunchURIPteProcess()
	{
		// Si está habilitado el Heartbeat no se usa el scheduler
		return PropsValues.ITER_APACHE_QUEUE_PRODUCER_CONSUMER_ENABLED && Heartbeat.canLaunchProcesses();
	}
}
