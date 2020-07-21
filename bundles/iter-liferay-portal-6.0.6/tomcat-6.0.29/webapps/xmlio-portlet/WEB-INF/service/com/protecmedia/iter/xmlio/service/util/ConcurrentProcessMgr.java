package com.protecmedia.iter.xmlio.service.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.xml.DocumentException;

public class ConcurrentProcessMgr
{
	private static Log 			_log 			= LogFactoryUtil.getLog(ConcurrentProcessMgr.class);
	private ReentrantLock		_lock			= new ReentrantLock();
	private int 				_processCounter = 0;
	private List<CacheRefresh> 	_okProcesses 	= new ArrayList<CacheRefresh>();
	private List<CacheRefresh>	_koProcesses 	= new ArrayList<CacheRefresh>();
	
	/**
	 * Aumenta en 1 el número de procesos registrados
	 */
	public void increment()
	{
		_log.trace("increment: start");
		
		_lock.lock();
		_processCounter++;
		if (_log.isDebugEnabled())
			_log.debug( String.format("increment: counter is %d", _processCounter) );
			
		_lock.unlock();
		
		_log.trace("increment: finish");
	}
	
	/**
	 * Combina (OR) el valor introducido por parámetros (operación actual) con lo registrado hasta el momento

	 * @param value
	 * @return El resultado general (OR de todos los resultados)
	 */
	public void mergeProcessResult(boolean value, CacheRefresh cacheRefresh)
	{
		_log.trace("mergeProcessResult: start");
		
		_lock.lock();
		
		if (value)
		{
			// Si se ha borrado correctamente la caché, se validan todos los grupos que fallaron anteriormente
			_okProcesses.addAll( _koProcesses );
			_koProcesses.clear();
			
			// Se añaden los grupos que acaban de invalidar correctamente la caché
			_okProcesses.add( cacheRefresh );
		}
		else
		{
			_koProcesses.add( cacheRefresh );
		}
		
		if (_log.isDebugEnabled())
			_log.debug( String.format("mergeProcessResult: value to merge is %s", Boolean.toString(value)) );
			
		_lock.unlock();
		
		_log.trace("mergeProcessResult: finish");
	}
	
	/**
	 * Disminuye en 1 el número de procesos registrados.<br/>
	 * Si es el último proceso registrado (contador == 0) reinicia el flag que registra el resultado de las operaciones
	 * @return 	<code>true</code> si es el último proceso registrado (contador == 0) y al menos uno terminó correctamente,<br/> 
	 * 			<code>false</code> en caso contrario.
	 * @throws DocumentException 
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	public CacheRefresh decrement() throws PortalException, SystemException, DocumentException
	{
		CacheRefresh result = null;
		_log.trace("decrement: start");
		
		_lock.lock();
		
		try
		{
			_processCounter--;
			
			if (_processCounter <= 0 && !_okProcesses.isEmpty())
			{
				result = _okProcesses.get(0);
				if (_okProcesses.size() > 1)
					result.merge( _okProcesses.subList(1, _okProcesses.size()) );
			}
			
			if (_log.isDebugEnabled())
				_log.debug( String.format("decrement: counter is %d, ProcessResult is %s", _processCounter, Boolean.toString(result != null)) );

			if (_processCounter <= 0)
			{
				reset();
			}
		}
		finally
		{
			_lock.unlock();
		}
			
		_log.trace("decrement: finish");
		return result;
	}
	
	public void reset()
	{
		_log.trace("reset: start");
		
		_lock.lock();
		_processCounter= 0;
		_okProcesses.clear();
		_koProcesses.clear();
		_lock.unlock();
		
		_log.trace("reset: finish");
	}
}
