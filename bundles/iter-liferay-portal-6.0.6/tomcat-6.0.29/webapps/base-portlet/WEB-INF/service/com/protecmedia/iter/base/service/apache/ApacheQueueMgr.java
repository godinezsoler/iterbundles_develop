package com.protecmedia.iter.base.service.apache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.protecmedia.iter.base.service.thread.ThreadTools;

public class ApacheQueueMgr
{
	
	private static Log _log = LogFactoryUtil.getLog(ApacheQueueMgr.class);
	
	private static final String	CACHE_SERVICE 		= "/get-uriptes?delete";
	private static final String	TRACE_OPERATIONS	= 
      new StringBuffer("\n\t Maximum number of operations: %d				").
	  			append("\n\t Current number of active operations: %d ").
	  			append("\n\t Apache list size: %d							").
	  			append("\n\t Number of new process: %d						").toString();
	
	private static final String	TRACE_EXCEEDED_MAX_OPERATIONS	= 
      new StringBuffer("¡The system exceeded the maximum number of operations!:").append(TRACE_OPERATIONS).toString();
	
	public static final String	ENCODING		= "gzip, deflate";
	
	final private String 				_apacheURL;
	private long						_maxOperations			= 0;
	private long						_activeOperations		= 0;
	static private boolean				_active					= true;
	private int 						_apacheConnTimeout		= 0;
	private int 						_apacheReadTimeout		= 0;
	private long 						_offlineTimeout			= 0;
	private List<ApacheQueueOperation> 	_listApacheOpe 			= new ArrayList<ApacheQueueOperation>();
	private boolean						_isOperationsStopped	= false;
		
	private final Object				_numOperationsMutex		= new Object();
	private final Object				_offlineTimeoutMutex	= new Object();
	
	public ApacheQueueMgr(String apacheURL, int apacheConnTimeout, int apacheReadTimeout, long offlineTimeout, long maxOperations) throws MalformedURLException, ServiceError
	{
		super();
		_apacheURL 			= apacheURL;
		_apacheConnTimeout 	= apacheConnTimeout;
		_apacheReadTimeout 	= apacheReadTimeout;
		_offlineTimeout		= offlineTimeout;

		_listApacheOpe.clear();
		setMaxOperations(maxOperations, false);
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Le pide al apache la lista de operaciones y las añade a la lista de operaciones de este thread.
	// Por el hecho de añadir elementos a la lista de operaciones del thread se comprueba si se pueden lanzar operaciones
	// - Antes de pedirle datos al Apache se espera hasta que la lista de operaciones del thread esté vacía
	// - Si se produce algún fallo en la comunicación con el apache o este no devuelve registros se espera un tiempo 
	//		configurado antes de realizar otra petición.
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public synchronized void doRequestQueue() throws IOException, InterruptedException
	{
		int numNewOperations = 0;
		try
		{
			// Espera a que la cola esté vacia y NO estén paradas/detenidas las operaciones de la cola del Apache (se podrían parar mientras se borra la caché del Apache)
			while (_listApacheOpe.size() > 0 || _isOperationsStopped)
			{
				if (_log.isDebugEnabled())
					_log.debug(getThreadTrace() +"The internal list of URLs is not empty.\nWaiting...");
				wait();
			}
			
			List<ApacheQueueOperation> newListApacheOpe = null;
			
			// Se abre la conección con el Apache y se le pide la lista de operaciones
			URL apacheServiceURL 			 = new URL(_apacheURL + CACHE_SERVICE);
	        URLConnection connection 		 = apacheServiceURL.openConnection();
	        HttpURLConnection httpConnection = (HttpURLConnection)connection;
	        httpConnection.setConnectTimeout(_apacheConnTimeout);
	        httpConnection.setReadTimeout(_apacheReadTimeout);
	        connection.setRequestProperty ("User-Agent", WebKeys.USER_AGENT_ITERWEBCMS);
	        httpConnection.connect();	

	        if (_log.isTraceEnabled())
	        	_log.trace(getThreadTrace() +"HttpURLConnection response code:"+ httpConnection.getResponseCode());
	        
	        if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK)
	        {
	        	// Lectura de datos correcta
		        BufferedReader in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
		        String currentLine;
		        
		        newListApacheOpe = new ArrayList<ApacheQueueOperation>();
		        
		        while ((currentLine = in.readLine()) != null) 
		        {
		        	if (_log.isTraceEnabled())
		        		_log.trace(getThreadTrace() + "URLpte: " + currentLine);
		        	
		        	String[] data = currentLine.split(",");
		        		
		        	if (data != null && data.length == 2)
		        	{
		        		// La línea contiene un dato válido "ULR, order"
		        		newListApacheOpe.add( new ApacheQueueOperation(_apacheConnTimeout, _apacheReadTimeout, apacheServiceURL.getHost(), data[0], Integer.parseInt(data[1]), this) );
			        }
		        }
				in.close();
	        }
	        else
	        {
	        	 _log.error(getThreadTrace() + httpConnection.getResponseMessage());
	        }
	        httpConnection.disconnect();
	        
	        // Se añaden operaciones a la lista
	        numNewOperations = appendOperations(newListApacheOpe);
		}
		catch (Exception e)
		{
			_log.error(getThreadTrace() + e.toString());
		}
		finally
		{
	        if (numNewOperations == 0 && !Thread.currentThread().isInterrupted())
	        {
	        	// Si ha fallado (la interrupción no se considera un fallo, se está parando el proceso),
	        	// o no existen elementos en la lista se espera un segundo
	        	long offlineTimeout = getOfflineTimeout();
	        	
	        	if (_log.isTraceEnabled())
	        	{
	        		StringBuilder sb = new StringBuilder().append( getThreadTrace() ).append( "Apache responded an empty list.\nSleeping " ).append( offlineTimeout ).append( " ms" );
	        		_log.trace(sb.toString());
	        	}
	        	
	        	Thread.sleep( offlineTimeout );
	        }
		}
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Añade operaciones a la lista de operaciones de este thread.
	// Por el hecho de añadir elementos a la lista de operaciones del thread se comprueba si se pueden lanzar operaciones
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private synchronized int appendOperations(List<ApacheQueueOperation> operations) 
	{
		int numNewOperations = 0;
		if(operations != null && operations.size() > 0)
		{
			Collections.sort(operations);
			if ( _listApacheOpe.addAll(operations) )
			{
				numNewOperations = operations.size();
				launchOperations();
			}
		}
		return numNewOperations;
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Si existen operaciones pendientes en la lista se lanzan tantas hasta alcanzar el número máximo establecido
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private synchronized void launchOperations() 
	{
		// No se han desactivado el ApacheQueueMgr ni interrumpidos los threads
		// Tampoco estén paradas/detenidas las operaciones de la cola del Apache (se podrían parar mientras se borra la caché del Apache)
		if (!_isOperationsStopped && ApacheQueueMgr.isActive() && Thread.currentThread().isAlive() && !Thread.currentThread().isInterrupted())
		{
			// Con la sincronización "private synchronized void launchOperations()" se garantiza que solo se está ejecutando un launchOperations
			// al tiempo, y con synchronized(_numOperationsMutex) se garantiza que no habrá errores entre el setMaxOperations y el launchOperations
			long maxOperations 	= getMaxOperations();
			long actOperations 	= getActiveOperations();
			long listSize 		= _listApacheOpe.size();
			
			long toLaunchOperations = Math.min( maxOperations-actOperations, listSize);
			if (_log.isTraceEnabled())
        	{
				String trace = String.format( getThreadTrace() + TRACE_OPERATIONS, maxOperations, actOperations, listSize, toLaunchOperations);
        		_log.trace(trace);
        	}
			
			if (maxOperations < toLaunchOperations || maxOperations < (toLaunchOperations+actOperations))
			{
				String trace = String.format( getThreadTrace() + TRACE_EXCEEDED_MAX_OPERATIONS, maxOperations, actOperations, listSize, toLaunchOperations);
				_log.error(trace);
			}
				
			// long toLaunchOperations = Math.min( getMaxOperations()-getActiveOperations(), _listApacheOpe.size());
			
			for (long i = 0; i < toLaunchOperations; i++)
			{
				incrementActiveOperations();
				ApacheQueueOperation currentOperation = _listApacheOpe.remove(0);

				currentOperation.setParentId( Long.toString(Thread.currentThread().getId()) );
				currentOperation.start();
			}
			
			// Si se ha vaciado la cola se despierta
			if (_listApacheOpe.size() == 0)
				notifyAll();
		}
	}
	
	public long getOfflineTimeout()
	{
		synchronized(_offlineTimeoutMutex)
		{
			return _offlineTimeout;
		}
	}
	
	public void setOfflineTimeout(long value)
	{
		synchronized(_offlineTimeoutMutex)
		{
			_offlineTimeout = value;
		}
	}
	
	private long getMaxOperations()
	{
		synchronized(_numOperationsMutex)
		{
			return _maxOperations;
		}
	}
	
	public void setMaxOperations(long value) throws ServiceError
	{
		setMaxOperations(value, true);
	}

	public void setMaxOperations(long value, boolean tryLaunchOperations) throws ServiceError
	{
		synchronized(_numOperationsMutex)
		{
			ErrorRaiser.throwIfFalse(value >= 0, IterErrorKeys.XYZ_E_APACHE_INVALID_MAXOPERATIONS_ZYX);
			
			// Si el nuevo valor es igual al anterior no se lanza las operaciones
			tryLaunchOperations = (_maxOperations == value) ? false : tryLaunchOperations;
			_maxOperations = value;
		}
		
		// Es posible que se puedan lanzar nuevas operaciones, se intentan lanzar
		if (tryLaunchOperations)
			launchOperations();
	}
	
	private String getThreadTrace()
	{
		return new StringBuffer().append(_apacheURL).append(" ").append(this.toString()).append(" ").append(ThreadTools.getThreadTrace()).append(":\n").toString();
	}
	
	private long getActiveOperations()
	{
		synchronized(_numOperationsMutex)
		{
			return _activeOperations;
		}
	}
	
	private void incrementActiveOperations()
	{
		synchronized(_numOperationsMutex)
		{
			_activeOperations++;
		}
	}
	
	public synchronized void decrementActiveOperations()
	{
		synchronized(_numOperationsMutex)
		{
			_activeOperations--;
		}
		
		// Es posible que se puedan lanzar nuevas operaciones, se intentan lanzar
		launchOperations();
	}
	
	static public synchronized boolean isActive()
	{
		return _active;
	}
	
	static public synchronized void setActive(boolean value)
	{
		_active = value;
	}
	
	/**
	 * Proceso que para/detiene el proceso de operaciones (Ej. antes del borrado de las cachés de los Apaches)
	 * Se activa el flag que comprueban los procesos y se vacía la lista de operaciones pendientes de procesar
	 */
	public synchronized void stopApacheQueueOperations()
	{
		_isOperationsStopped = true;
		_listApacheOpe.clear();
	}
	
	/**
	 * Proceso que reestablece el proceso de operaciones (Ej. tras borrado de las cachés de los Apaches)
	 * Se desactiva el flag que comprueban los procesos y se lanza un notify por si estuviesen dormidos
	 */
	public synchronized void restartApacheQueueOperations()
	{
		_isOperationsStopped = false;
		notifyAll();
	}
}