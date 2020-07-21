package com.protecmedia.iter.base.cluster;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.service.ClusterGroupLocalServiceUtil;
import com.liferay.portal.util.HealthMgr;
import com.liferay.portal.util.HealthMgr.Level;
import com.protecmedia.iter.base.community.publisher.CommunityPublisherController;
import com.protecmedia.iter.base.metrics.VisitsStatisticMgr;
import com.protecmedia.iter.base.service.NewsletterMgrLocalServiceUtil;
import com.protecmedia.iter.base.service.apache.URIPteMgr;
import com.protecmedia.iter.base.service.cache.CacheInvalidationController;
import com.protecmedia.iter.base.service.util.HotConfigUtil;
import com.protecmedia.iter.user.service.SocialMgrLocalServiceUtil;

public class Heartbeat implements Runnable 
{
	private static final Log _log 		= LogFactoryUtil.getLog(Heartbeat.class);
	private static final Log _logTest	= LogFactoryUtil.getLog("com.protecmedia.iter.base.cluster.Heartbeat.test");
	
	private static 	Lock 				_lock 					= new ReentrantLock();
	private static 	ExecutorService 	_executorService 		= null;
	private static 	FutureTask<Void>	_heartbeat				= null;
	private static 	AtomicBoolean		_isMaster				= new AtomicBoolean();
	

	static public void isMaster(boolean newValue)
	{
		if (PropsValues.CLUSTER_HEARTBEAT_ENABLED)
			_isMaster.set(newValue);
		
		if (_log.isInfoEnabled())
			_log.info( String.format("The server has become %s.", isMaster() ? "master" : "slave") );
	}
	
	/**
	 * @return <b>true</b> si el servidor es el responsable de los procesos desatendidos, y <b>false</b> en caso contrario.
	 */
	static public boolean isMaster()
	{
		return _isMaster.get();
	}

	static public boolean launchHeartbeat() throws Throwable
	{
		_log.trace("launchHeartbeat: Begin");
		boolean startOK = false;
		
		_lock.lock();
		try
		{
			ErrorRaiser.throwIfFalse( !isInProcess(), IterErrorKeys.XYZ_E_HEARTBEAT_IS_ALREADY_RUNNING_ZYX ); 

			if (canLaunchHeartbeatProcess())
			{
				try
				{
					isMaster(true);
					
					_heartbeat 		= new FutureTask<Void>(new Heartbeat(), null);
					_executorService= Executors.newSingleThreadExecutor();
					
					// Se crea el heartbeat
					_executorService.execute( _heartbeat );
				
					startOK = true;
					_log.info("The Heartbeat mechanism has started");
				}
				catch (Throwable th)
				{
					// Si ha fallado algo del arranque del heartbeat NO será el maestro
					isMaster(false);
					throw th;
				}
			}
			else
			{
				_log.warn("The Heartbeat is disable in this server (check cluster.heartbeat properties)");
			}
		}
		finally
		{
			_lock.unlock();
		}
		_log.trace("launchHeartbeat: End");
		
		return startOK;
	}
	
	static public boolean stopHeartbeat() throws ServiceError, InterruptedException
	{
		_log.trace("stopHeartbeat: Start");
		boolean stopOK = false;
		
		_lock.lock();
		try
		{
			ErrorRaiser.throwIfFalse( isInProcess(), IterErrorKeys.XYZ_E_HEARTBEAT_IS_NOT_RUNNING_ZYX ); 
			
			try
			{
				_executorService.shutdownNow();
				
				long timeout = PropsValues.CLUSTER_HEARTBEAT_DELAY * PropsValues.CLUSTER_HEARTBEAT_THRESHOLD;
				stopOK = _executorService.awaitTermination(timeout, TimeUnit.MILLISECONDS);
			}
			finally
			{
				// Aunque el proceso haya fallado, se haya detenido o no, el servidor NO debe ser el maestro
				isMaster(false);
				
				if (!stopOK)
					_log.error("The Heartbeat mechanism is still in process");
				else
				{
					_log.info("The Heartbeat mechanism has finished");
	
					_heartbeat = null;
					_executorService = null;
				}
			}
		}
		finally
		{
			_lock.unlock();
		}

		_log.trace("stopHeartbeat: End");
		return stopOK;
	}
	
	static private boolean isInProcess()
	{
		boolean inProcess = false;
		
		_lock.lock();
		try
		{
			inProcess = _heartbeat != null 				&& _executorService != null 		&& 
						!_executorService.isShutdown() 	&& !_executorService.isTerminated() &&
						!_heartbeat.isCancelled()		&& !_heartbeat.isDone();
		}
		finally
		{
			_lock.unlock();
		}

		return inProcess;
	}
	
	static private boolean canLaunchHeartbeatProcess()
	{
		return PropsValues.CLUSTER_HEARTBEAT_ENABLED && !PropsValues.IS_PREVIEW_ENVIRONMENT;
	}
	
	public Heartbeat() throws ServiceError
	{
		startProcesses();
	}
	
	@Override
	public void run() 
	{
		_log.info("Starting the Heartbeat process");
		boolean hasControl 	= true;

		while (hasControl && _isEnable())
		{
			try
			{
				// Comprueba en BBDD que es propietario y actualiza la fecha en BBDD
				if ( hasControl = (HealthMgr.isHealthy() && updateDB()) )
				{
					if (_log.isTraceEnabled())
						_log.trace( String.format("The Heartbeat will sleep %d ms", PropsValues.CLUSTER_HEARTBEAT_DELAY) );

					Thread.sleep( PropsValues.CLUSTER_HEARTBEAT_DELAY );
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
				HealthMgr.track(Level.ERROR, "Heartbeat problem", th);
				
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
		
		if (!hasControl)
			_log.warn("The server has lost the control");
		
		// Intenta detener los procesos desatendidos ya que no tiene la responsabilidad dentro del clúster
		stopProcesses();
		
		isMaster(false);
		
		// Intenta lanzar el Watchdog para luche por la responsabilidad
		launchWatchdog();
				
		_log.info("The Heartbeat process has been finished");
	}
	
	private boolean launchWatchdog()
	{
		boolean launched = false;
		try
		{
			launched = Watchdog.launchWatchdog();
		}
		catch (Throwable th) 
		{
			// Si ya está ejecutándose no se hace nada
			if (th instanceof ServiceError && ((ServiceError)th).getErrorCode().equals(IterErrorKeys.XYZ_E_WATCHDOG_IS_ALREADY_RUNNING_ZYX))
			{
				_log.warn(th.toString());
			}
			else
			{
				_log.error(th);
				HealthMgr.track(Level.ERROR, "Watchdog problem", th);
			}
		}
		
		if (launched)
			_log.info("The Heartbeat has launched the Watchdog");
		else
			_log.warn("The Heartbeat can't launch the Watchdog");
		
		return launched;
	}

	
	private boolean updateDB() throws InterruptedException, ExecutionException
	{
		_log.trace("updateDB: Begin");
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Boolean> future = executor.submit(new Callable<Boolean>()
		{
			public Boolean call() throws Exception
			{
				if (_logTest.isFatalEnabled()) HotConfigUtil.wait4Properties("cluster.heartbeat.test.updtdb");
				
				// Se llama a un ServiceUtil dentro de un thread para crear y cerrar una transacción dentro de la que se utiliza un SELECT FOR UPDATE 
				return ClusterGroupLocalServiceUtil.sendHeartbeat();
			}
		});
		
		_log.debug("updateDB: Waiting for Heartbeat in DB");
		boolean hasControl = future.get();
		
		if (!hasControl)
			_log.debug("updateDB: Database indicates that this server has lost the control");
		
		_log.trace("updateDB: End");
		return hasControl;
	}
	
	private boolean _isEnable()
	{
		return Thread.currentThread().isAlive() && !Thread.currentThread().isInterrupted();
	}
	
	/**
	 * No se controlan las excepciones delos procedimientos uno a uno porque con que el uno 
	 * falle se debe abortar el arranque del Heartbeat.
	 * @throws ServiceError
	 */
	private void startProcesses() throws ServiceError
	{
		_log.trace("startProcesses: Begin");
		NewsletterMgrLocalServiceUtil.initSchedules();
		URIPteMgr.launchURIPteProcess();
		CommunityPublisherController.INSTANCE.start();
		CacheInvalidationController.INSTANCE.start();	
		SocialMgrLocalServiceUtil.initSocialStatisticsTasks();
		VisitsStatisticMgr.start();
		_log.trace("startProcesses: End");
	}
	
	/**
	 * Las excepciones se trazan pero no se elevan porque ya se está perdiendo el control 
	 * en el sistema y se trata de intentar de parar todos los procesos desatendidos.
	 * 
	 *  Se marcan en el controlador de salud, porque si no se pueden parar es un indicador 
	 *  negativo de que algo va mal en el servidor.
	 * 
	 * @throws ServiceError
	 * @throws InterruptedException
	 */
	private void stopProcesses()
	{
		_log.trace("stopProcesses: Begin");
		
		NewsletterMgrLocalServiceUtil.stopNewsletters();
		try
		{
			URIPteMgr.stopURIPteProcess();
		}
		catch (Throwable th)
		{
			// Si ya está parado no se hace nada
			if (th instanceof ServiceError && ((ServiceError)th).getErrorCode().equals(IterErrorKeys.XYZ_E_URIPTES_IS_NOT_RUNNING_ZYX))
			{
				_log.warn(th.toString());
			}
			else
			{
				_log.error(th);
				HealthMgr.track(Level.ERROR, "URIPte problem", th);
			}
		}
		
		// No deben lanzar excepciones
		CommunityPublisherController.INSTANCE.halt();
		CacheInvalidationController.INSTANCE.halt();
		try
		{
			SocialMgrLocalServiceUtil.stopSocialStatisticsTasks();
		}
		catch (Throwable th)
		{
			_log.warn(th.toString());
		}
		
		try
		{
			VisitsStatisticMgr.stop();
		}
		catch (Throwable th)
		{
			// Si ya está parado no se hace nada
			if (th instanceof ServiceError && ((ServiceError)th).getErrorCode().equals(IterErrorKeys.XYZ_E_VISITSSTATISTICMGR_IS_NOT_RUNNING_ZYX))
			{
				_log.warn(th.toString());
			}
			else
			{
				_log.error(th);
				HealthMgr.track(Level.ERROR, "VisitsStatisticMgr problem", th);
			}
		}
		
		_log.trace("stopProcesses: End");
	}
	
	public static boolean canLaunchProcesses()
	{
		return !PropsValues.IS_PREVIEW_ENVIRONMENT && 
				(Heartbeat.isMaster() || (!PropsValues.CLUSTER_HEARTBEAT_ENABLED && PropsValues.SCHEDULER_ENABLED));
	}
}
