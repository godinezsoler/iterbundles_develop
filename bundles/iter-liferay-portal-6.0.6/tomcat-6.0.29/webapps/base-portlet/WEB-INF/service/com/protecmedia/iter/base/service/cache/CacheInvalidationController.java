package com.protecmedia.iter.base.service.cache;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.liferay.portal.apache.ApacheUtil;
import com.liferay.portal.kernel.cluster.ClusterTools;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.protecmedia.iter.base.service.ServerAffinityLocalServiceUtil;
import com.protecmedia.iter.base.service.affinity.IServerAffinityProcess;
import com.protecmedia.iter.base.service.util.ServerAffinityConstants;

public enum CacheInvalidationController implements IServerAffinityProcess
{
	INSTANCE;
	
	private static final Log 				_log 				= LogFactoryUtil.getLog(CacheInvalidationController.class);
	private static Lock 					_lock 				= new ReentrantLock();
	private static ExecutorService			_executorService 	= null;
	private static CacheInvalidationProcess _cacheInvalidator	= null;
	private static FutureTask<Void>			_future				= null;
	
	@Override
	public void start() throws ServiceError
	{
		_lock.lock();
		try
		{
			if (!isInProcess())
			{
				ErrorRaiser.throwIfFalse(canLaunchProcess(), IterErrorKeys.XYZ_E_CACHE_INVALIDATION_CANNOT_BE_STARTED_ZYX);
				
				_executorService = Executors.newSingleThreadExecutor();
				
				_cacheInvalidator= new CacheInvalidationProcess();
				_future	 		 = new FutureTask<Void>(_cacheInvalidator, null);
				
				_executorService.execute(_cacheInvalidator);
				
				_log.info("The Cache Invalidation mechanism has started");
			}
			else
			{
				_log.info("The Cache Invalidation mechanism has already been started");
			}
		}
		finally
		{
			_lock.unlock();
		}
	}

	@Override
	public void halt()
	{
		boolean stopOK = false;
		
		_lock.lock();
		try
		{
			if (isInProcess())
			{
				try
				{
					_executorService.shutdownNow();
					
					// El timeout será lo máximo que podría tardar una comunicación con Apache (tiempo de conexión + tiempo de lectura),
					// más un tiempo extra de margen (por ejemplo el empleado en consumir la respuesta del Apache)
					long timeout = 100 + ApacheUtil.getApacheConnTimeout() + ApacheUtil.getApacheReadTimeout();
					
					int numAttempts = 0;
					do
					{
						stopOK = _executorService.awaitTermination(timeout, TimeUnit.MILLISECONDS);
						numAttempts++;
						
						if (!stopOK)
							_log.error("The Cache Invalidation mechanism is still in process");
					}
					while (!stopOK && numAttempts < 3);
				}
				catch (InterruptedException ie)
				{
					_log.error("The Cache Invalidation mechanism has been interrupted while it was halting");
				}
				finally
				{
					if (stopOK)
					{
						_log.info("The Cache Invalidation mechanism has finished");
		
						_cacheInvalidator	= null;
						_executorService	= null;
					}
				}
			}
			else
			{
				_log.info("The Cache Invalidation mechanism is not running");
			}
		}
		finally
		{
			_lock.unlock();
		}
	}

	static public void reschedule() throws Exception
	{
		if (canLaunchProcess())
			rescheduleNonClustered();
		else
			ClusterTools.notifyCluster(true, CacheInvalidationController.class.getName(), "rescheduleNonClustered");
	}
	static public void rescheduleNonClustered() throws SecurityException, NoSuchMethodException, IOException, SQLException
	{
		_lock.lock();
		try
		{
			// En este servidor es posible lanzar el mecanismo de Invalidación de Cachés
			if (canLaunchProcess())
			{
				if (isInProcess())
				{
					// Se reprograman las invalidaciones
					_cacheInvalidator.reschedule();
				}
				else
				{
					_log.info("The Cache Invalidation mechanism is not running");
				}
			}
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	/**
	 * @return <b>true</b> si la programación de invalidación de caché se puede activar, <b>false</b> en caso contrario.
	 */
	static public boolean canLaunchProcess()
	{
		return ServerAffinityLocalServiceUtil.isServerAffinity(ServerAffinityConstants.TASKKIND.cache.toString()) && !PropsValues.IS_PREVIEW_ENVIRONMENT;
	}
	
	/**
	 * @return <b>true</b> si la programación de invalidación de caché está activa, <b>false</b> en caso contrario.
	 */
	static private boolean isInProcess()
	{
		boolean inProcess = false;
		
		_lock.lock();
		try
		{
			inProcess = _cacheInvalidator != null 		&& _executorService != null 		&& 
						!_executorService.isShutdown() 	&& !_executorService.isTerminated()	&&
						_future != null					&& !_future.isDone() && !_future.isCancelled(); 
		}
		finally
		{
			_lock.unlock();
		}

		return inProcess;
	}
}
