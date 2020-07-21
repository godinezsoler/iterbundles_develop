package com.protecmedia.iter.base.service.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.protecmedia.iter.base.service.apache.ApacheQueueMgrThread;
import com.protecmedia.iter.base.service.thread.ThreadTools;

public class ApacheQueueUtil 
{
	private static Log _log = LogFactoryUtil.getLog(ApacheQueueUtil.class);
	private static List<ApacheQueueMgrThread> 	_listApacheMgr 			= null;

	private static boolean						_cleanApacheQueue 		= false;
	private static Object						_cleanApacheQueueMutex 	= new Object();
	
	private static Date							_lastPubDate 			= null; 
	private static long							_lastChecked			= 0;

	///////////////////////////////////////////////////////////////////////////////////////////////////
	// private static boolean getCleanApacheQueue()
	//
	// Devuelve el flag que indica que en el siguiente golpe de reloj se desactivarán los ApacheMgr
	///////////////////////////////////////////////////////////////////////////////////////////////////
	public static boolean getCleanApacheQueue() 
	{
		synchronized (_cleanApacheQueueMutex) 
		{
			return _cleanApacheQueue;
		}
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////
	// public static synchronized void setCleanApacheQueue(boolean cleanApacheQueue) 
	// 
	// Indica que en el siguiente golpe de reloj se desactivarán los ApacheMgr
	///////////////////////////////////////////////////////////////////////////////////////////////////
	public static synchronized void setCleanApacheQueue(boolean cleanApacheQueue) 
	{
		synchronized (_cleanApacheQueueMutex) 
		{
			_cleanApacheQueue = cleanApacheQueue;
		}
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////
	// public synchronized void interruptApacheQueueMgr()
	//
	// Interrumple los ApacheMgr en curso, espera a que terminen y limpia la lista de estos
	///////////////////////////////////////////////////////////////////////////////////////////////////
	public static synchronized void interruptApacheQueueMgrs()
	{
		if (_listApacheMgr.size() > 0)
		{
			_log.info(ThreadTools.getThreadTrace() +": Stopping the treatment of apaches´s URL queues");
			
			// Se interrumpen
			for (ApacheQueueMgrThread currentAQM:_listApacheMgr)
				currentAQM.interrupt();
			
			// Se esperan
			for (ApacheQueueMgrThread currentAQM:_listApacheMgr)
			{
				try 
				{
					currentAQM.join();
				} 
				catch (InterruptedException e) {}
			}
			
			_listApacheMgr.clear();
			
			_log.info(ThreadTools.getThreadTrace() +": The treatment of apaches´s URL queues was stopped");
		}
	}


	
	public static synchronized void initApacheQueueMgrs()
	{
		if (_listApacheMgr == null)
			_listApacheMgr = new ArrayList<ApacheQueueMgrThread>();
	}
	public static synchronized int getApacheQueueMgrsSize()
	{
		return _listApacheMgr.size();
	}
	
	public static synchronized void addApacheQueueMgr(ApacheQueueMgrThread mgr)
	{
		_listApacheMgr.add(mgr);
		mgr.start();
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////
	public static synchronized void setApacheQueueMgrsHotconfig(long maxOperations, long offlineTimeout) throws ServiceError
	{
		for (ApacheQueueMgrThread currentAQM:_listApacheMgr)
		{
			currentAQM.apacheQueueMgr.setMaxOperations(maxOperations);
			currentAQM.apacheQueueMgr.setOfflineTimeout(offlineTimeout);
		}
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////
	public static synchronized void stopApacheQueueOperations()
	{
		_log.info("Stopping the Apache queue operations");
		// Se obtiene la fecha de publicación antes de intentar parar, por si el acceso del synchronize 
		// o durante el parado de cada apacheQueueMgr se VUELVE a actualizar la fecha de úlima publicación. 
		_lastPubDate = GroupMgr.getMoreRecentLastPubDate();
		for (ApacheQueueMgrThread currentAQM:_listApacheMgr)
		{
			currentAQM.apacheQueueMgr.stopApacheQueueOperations();
		}
		// Al final de proceso se toma la fecha de comparación
		_lastChecked = System.currentTimeMillis();
		_log.info("The Apache queue operations has been stopped");
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////
	public static synchronized void restartApacheQueueOperations()
	{
		_log.info("Reestarting the Apache queue operations");
		for (ApacheQueueMgrThread currentAQM:_listApacheMgr)
		{
			currentAQM.apacheQueueMgr.restartApacheQueueOperations();
		}
		_lastChecked = 0;
		_lastPubDate = null;
		_log.info("The Apache queue operations has been restarted");
	}

	public static synchronized void checkApacheQueueOperationsKeepAlive()
	{
		// Se ha hecho un Stop y aún no se ha hecho un restart (_lastPubDate != null && _lastChecked != null)
		if (_lastPubDate != null && _lastChecked > 0)
		{
			// Y se ha superado el umbral de comprobación
			long timeout = HotConfigUtil.getKey(IterKeys.HOTCONFIG_KEY_APACHE_QUEUE_KEEPALIVE_TIMEOUT, 5000);
			if ( (System.currentTimeMillis() - _lastChecked) > timeout )
			{
				// Se comprueba la fecha de última publicación
				Date currentLastPubDate = GroupMgr.getMoreRecentLastPubDate();
				if (currentLastPubDate.after(_lastPubDate))
				{
					// Ha cambiado la fecha de publicación, se restaura el funcionamiento
					restartApacheQueueOperations();
				}
				else
				{
					// Se resetea el timeout para la siguiente comprobación del lastPubDate
					_lastChecked = System.currentTimeMillis();
				}
			}
		}
	}
}
