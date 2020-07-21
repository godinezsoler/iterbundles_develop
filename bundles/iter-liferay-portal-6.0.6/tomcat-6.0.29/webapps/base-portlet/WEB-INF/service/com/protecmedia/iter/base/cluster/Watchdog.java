package com.protecmedia.iter.base.cluster;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.service.ClusterGroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.HealthMgr;
import com.liferay.portal.util.HealthMgr.Level;
import com.protecmedia.iter.base.service.util.HotConfigUtil;

public class Watchdog implements Runnable
{
	private static final 	Log 				_log 				= LogFactoryUtil.getLog(Watchdog.class);
	private static final 	Log 				_logTest			= LogFactoryUtil.getLog("com.protecmedia.iter.base.cluster.Watchdog.test");
	
	private static 			Lock 				_lock 				= new ReentrantLock();
	private static 			ExecutorService 	_executorService 	= null;
	private static 			FutureTask<Void>	_watchdog			= null;

	
	static public boolean launchWatchdog() throws Throwable
	{
		_log.trace("launchWatchdog: Begin");
		boolean startOK = false;
		
		_lock.lock();
		try
		{
			if (canLaunchWatchdogProcess())
			{
				ErrorRaiser.throwIfFalse( !isInProcess(), 		IterErrorKeys.XYZ_E_WATCHDOG_IS_ALREADY_RUNNING_ZYX ); 
				ErrorRaiser.throwIfFalse( existMasterInDB(), 	IterErrorKeys.XYZ_E_WATCHDOG_NO_MASTER_IN_DB_ZYX );

				_watchdog 		= new FutureTask<Void>(new Watchdog(), null);
				_executorService= Executors.newSingleThreadExecutor();
				
				// Se crea el Watchdog
				_executorService.execute( _watchdog );
			
				startOK = true;
				_log.info("The Watchdog mechanism has started");
			}
			else
			{
				_log.warn("The Watchdog is disable in this server (check cluster.heartbeat properties)");
			}
		}
		finally
		{
			_lock.unlock();
		}
		_log.trace("launchWatchdog: End");
		
		return startOK;
	}
	
	static public boolean stopWatchdog() throws ServiceError, InterruptedException
	{
		_log.trace("stopWatchdog: Start");
		boolean stopOK = false;
		
		_lock.lock();
		try
		{
			ErrorRaiser.throwIfFalse( isInProcess(), IterErrorKeys.XYZ_E_WATCHDOG_IS_NOT_RUNNING_ZYX ); 
			
			try
			{
				_executorService.shutdownNow();
				
				long timeout = PropsValues.CLUSTER_HEARTBEAT_DELAY * PropsValues.CLUSTER_HEARTBEAT_THRESHOLD;
				stopOK = _executorService.awaitTermination(timeout, TimeUnit.MILLISECONDS);
			}
			finally
			{
				if (!stopOK)
					_log.error("The Watchdog mechanism is still in process");
				else
				{
					_log.info("The Watchdog mechanism has finished");
	
					_watchdog = null;
					_executorService = null;
				}
			}
		}
		finally
		{
			_lock.unlock();
		}

		_log.trace("stopWatchdog: End");
		return stopOK;
	}
	
	static private boolean isInProcess()
	{
		boolean inProcess = false;
		
		_lock.lock();
		try
		{
			inProcess = _watchdog != null 				&& _executorService != null 		&& 
						!_executorService.isShutdown() 	&& !_executorService.isTerminated()	&&
						!_watchdog.isCancelled()		&& !_watchdog.isDone();
		}
		finally
		{
			_lock.unlock();
		}

		return inProcess;
	}
	
	static private boolean canLaunchWatchdogProcess()
	{
		return PropsValues.CLUSTER_HEARTBEAT_ENABLED && !PropsValues.IS_PREVIEW_ENVIRONMENT;
	}
	
	@Override
	public void run() 
	{
		_log.info("Starting the Watchdog process");
		boolean acqControl = true;
		
		while (_isEnable())
		{
			try
			{
				long time2Sleep = updateDB(acqControl);
				if (time2Sleep > 0)
				{
					if (!acqControl)
						_log.warn("The server has lost the control");
					
					if (_log.isTraceEnabled())
						_log.trace( String.format("The Watchdog will sleep %d ms", time2Sleep) );
					
					// Duerme el tiempo límite que tiene el Master para emitir el Heartbeat
					Thread.sleep( time2Sleep );
					
					// Actualiza la fecha de la tabla con la fecha actual cuando toma el control
					acqControl = true;
				}
				else
				{
					_log.info("The server has acquired the control");
					if (acqControl = launchHeartbeat())
						break;
				}
			}
			catch (InterruptedException ie)
			{
				Thread.currentThread().interrupt();
				_log.trace(ie.toString());
			}
			catch (Throwable th)
			{
				_log.error(th);
				HealthMgr.track(Level.ERROR, "Watchdog problem", th);
				
				// Si se produce un fallo se realiza una espera antes de volver a intentarlo
				try 
				{
					Thread.sleep( PropsValues.CLUSTER_HEARTBEAT_DELAY );
				} 
				catch (InterruptedException e) 
				{
					Thread.currentThread().interrupt();
				}
			}
		}
		_log.info("The Watchdog process has been finished");
	}

	private boolean _isEnable()
	{
		return Thread.currentThread().isAlive() && !Thread.currentThread().isInterrupted();
	}

	private long updateDB(final boolean acqControl) throws InterruptedException, ExecutionException
	{
		_log.trace("updateDB: Begin");
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Long> future = executor.submit(new Callable<Long>()
		{
			public Long call() throws Exception
			{
				if (_logTest.isFatalEnabled()) HotConfigUtil.wait4Properties("cluster.watchdog.test.updtdb");
					
				// Se llama a un ServiceUtil dentro de un thread para crear y cerrar una transacción dentro de la que se utiliza un SELECT FOR UPDATE 
				return ClusterGroupLocalServiceUtil.sendWatchdog(acqControl);
			}
		});
		
		_log.debug("updateDB: Waiting for Watchdog in DB");
		long time2Sleep = future.get();
		
		if (time2Sleep <= 0)
			_log.debug("updateDB: Database indicates that this server has acquired the control");
		
		_log.trace("updateDB: End");
		return time2Sleep;
	}

	private boolean launchHeartbeat()
	{
		boolean launched = false;
		try
		{
			launched = Heartbeat.launchHeartbeat();
		}
		catch (Throwable th)
		{
			// Si ya está ejecutándose no se hace nada
			if (th instanceof ServiceError && ((ServiceError)th).getErrorCode().equals(IterErrorKeys.XYZ_E_HEARTBEAT_IS_ALREADY_RUNNING_ZYX))
			{
				_log.warn(th.toString());
			}
			else
			{
				_log.error(th);
				HealthMgr.track(Level.ERROR, "Heartbeat problem", th);
			}
		}
		
		if (launched)
			_log.info("The Watchdog has launched the Heartbeat");
		else
			_log.warn("The Watchdog can't launch the Heartbeat");
		
		return launched;
	}
	
	private static boolean existMasterInDB()
	{
		return Integer.parseInt(((List<Object>)PortalLocalServiceUtil.executeQueryAsList("SELECT COUNT(*) FROM Heartbeat")).get(0).toString()) > 0;
	}
}
