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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.liferay.portal.apache.ApacheHierarchy;
import com.liferay.portal.apache.ApacheUtil;
import com.liferay.portal.kernel.concurrent.ExtLinkedBlockingQueue;
import com.liferay.portal.kernel.concurrent.ExtSemaphore;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.IterSecureConfigTools;
import com.liferay.portal.kernel.util.IterURLUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.cluster.Heartbeat;
import com.protecmedia.iter.base.service.util.HotConfigUtil;
import com.protecmedia.iter.base.service.util.IterKeys;



public class URIPteProducer implements Runnable
{
	private static final Log _log = LogFactoryUtil.getLog(URIPteProducer.class);
	
	private static final String	CACHE_SERVICE 					= "/get-uriptes?delete";
	private static final String PERMITS_AVAILABLE				= "The current number of permits available in producer semaphore is %d\t(%s)";
	
	private static final String GET_SECTIONS					= new StringBuilder(
		"SELECT plid, friendlyURL																								\n").append( 
		"FROM Layout																											\n").append( 
		"INNER JOIN News_PageContent ON Layout.uuid_ = News_PageContent.layoutId AND Layout.groupId = News_PageContent.groupId	\n").append( 
		"  WHERE contentId = \"%s\" AND Layout.groupId = %d																		\n").toString();

	private ExtLinkedBlockingQueue<URIPte> 	_queue				= null;
	private long 							_offlineTimeout		= 0;
	private String							_sleepingTrace		= StringPool.BLANK;
	
	private ExtSemaphore 					_semaphore			= new ExtSemaphore(1, true);
		
	
	public URIPteProducer(ExtLinkedBlockingQueue<URIPte> queue) throws ServiceError
	{
		_queue = queue;
		
		_offlineTimeout = HotConfigUtil.getKey(IterKeys.HOTCONFIG_KEY_APACHE_QUEUE_OFFLINETIMEOUT, 1000);
		ErrorRaiser.throwIfFalse(_offlineTimeout >= 0, IterErrorKeys.XYZ_E_APACHE_INVALID_OFFLINETIMEOUT_ZYX);
		
		_sleepingTrace = String.format("Sleeping %d ms", _offlineTimeout);
	}

	@Override
	public void run() 
	{
		_log.info("Starting the URIPte producer process");
		while (_isProductionEnable())
		{
			List<URIPte> newURIPteList = null;
			
			try
			{
				wait4ProducerSemaphore();
				
				if (_semaphore.availablePermits() == 1)
				{
					newURIPteList = acquireURIPtes();
	
					// Se añade a la cola
			        for (int i = 0; i < newURIPteList.size() && _semaphore.availablePermits() == 1; i++)
			        	_queue.put(newURIPteList.get(i));
				}
		        
		        // Wait for consumers
		        _queue.wait4Empty();
			}
			catch (InterruptedException ie)
			{
				Thread.currentThread().interrupt();
				_log.trace(ie.toString());
			}
			catch (Throwable th)
			{
				_log.error(th);
			}
			finally
			{
		        if ((newURIPteList == null || newURIPteList.isEmpty()) && _isProductionEnable())
		        {
		        	// Si ha fallado (la interrupción o parada no se considera un fallo),
		        	// o no existen	elementos en la lista se espera el tiempo configurado
	        		_log.trace(_sleepingTrace);
		        	
		        	try 
		        	{
						Thread.sleep( _offlineTimeout );
					} 
		        	catch (InterruptedException e) 
		        	{
		        		Thread.currentThread().interrupt();
		        		_log.trace(e.toString());
		        	}
		        }
			}
		}
		_log.info("The URIPte producer process has been finished");
	}
	
	/**
	 * Disminuye el contador de "<i>permits</i>" sin quedarse bloqueado. 
	 * Se compensará con la llamada a <code>restartProducerSemaphore</code>.
	 */
	public void pauseProducerSemaphore()
	{
		_semaphore.reducePermits(1);
		_log.debug("The producer semaphore has been paused");
		
		if (_log.isTraceEnabled())
			_log.trace( String.format(PERMITS_AVAILABLE, _semaphore.availablePermits(), "PAUSE") );
	}
	
	/**
	 * Aumenta el contador de "<i>permits</i>". Compensará la llamada a <code>pauseProducerSemaphore</code>.
	 */
	public void restartProducerSemaphore()
	{
		_semaphore.release();
		_log.debug("The producer semaphore has been restarted");

		if (_log.isTraceEnabled())
			_log.trace( String.format(PERMITS_AVAILABLE, _semaphore.availablePermits(), "RESTART") );
	}
	
	/**
	 * Método que dormirá (en el acquier) si desde otro hilo se ha llamado a pauseProducerSemaphore
	 * @throws InterruptedException
	 */
	private void wait4ProducerSemaphore() throws InterruptedException
	{
		if (_log.isTraceEnabled())
		{
			_log.trace("Waiting for the producer semaphore");
			_log.trace( String.format(PERMITS_AVAILABLE, _semaphore.availablePermits(), "WAIT BEFORE") );
		}
		
		_semaphore.acquire();
		_semaphore.release();
		
		if (_log.isTraceEnabled())
		{
			_log.trace( String.format(PERMITS_AVAILABLE, _semaphore.availablePermits(), "WAIT AFTER") );
			_log.trace("The waiting for the producer semaphore has finished");
		}
	}
	
	private boolean _isProductionEnable()
	{
		return 	Thread.currentThread().isAlive() && !Thread.currentThread().isInterrupted() && 
				Heartbeat.canLaunchProcesses();
	}
	
	private String getApacheMaster() throws IOException, ServiceError
	{
		String[] masterList = ApacheHierarchy.getInstance().getMasterList();
		ErrorRaiser.throwIfFalse( masterList.length > 0, IterErrorKeys.XYZ_E_APACHE_MASTERS_NOT_FOUND_ZYX);

		return masterList[0];
	}
	
	private List<URIPte> getURIPtesByITTP(URIPte uripte) throws MalformedURLException, PortalException, SystemException
	{
		List<URIPte> sectionURIPteList = new ArrayList<URIPte>();
		
		String articleId = "";
		
		// Está activado el flag, es una URIPte de artículo y de la URL se obtiene un articleId bien formado
		if (PropsValues.ITER_APACHE_QUEUE_PRODUCER_INCLUDE_ARTICLE_SECTIONS && uripte.isIterURIPte() &&
			!(articleId = IterURLUtil.getArticleId(uripte.getURL())).isEmpty())
		{
			// Se localizan todas las secciones de un artículo
			long groupId 	= uripte.getGroupId();
			String sql 		= String.format(GET_SECTIONS, articleId, groupId);
			_log.trace(sql);
			
			List<Object> listSections = PortalLocalServiceUtil.executeQueryAsList(sql);
			if (listSections.size() > 0)
			{
				String protocol 	= IterSecureConfigTools.getConfiguredHTTPS(groupId) ? Http.HTTPS : Http.HTTP;
				URL articleURL		= new URL(uripte.getOriginalURL().replaceFirst(URIPte.ITER_PROTOCOL, "http://"));
				String relativeURL 	= articleURL.getFile();
				
				// Si la llamada del detalle era móvil, se generan llamadas móviles a las seccions donde esté el artículo
				String token 		= IterGlobal.getMobileToken(groupId);
				boolean isMobile 	= Validator.isNotNull(token) && 
										relativeURL.startsWith(StringPool.FORWARD_SLASH.concat(token).concat(StringPool.FORWARD_SLASH));

				for (Object section : listSections)
				{
					try
					{
						String friendlyURL = ((Object[])section)[1].toString();
						
						if (isMobile)
						{
							Layout layout = LayoutLocalServiceUtil.getLayout(Long.parseLong(((Object[])section)[0].toString()));
							try
							{
								// Si la sección no tiene versión móvil solo se genera la clásica porque la móvil generaría un 404 al consumir la URIPte
								friendlyURL = IterURLUtil.buildMobileURL(groupId, friendlyURL, false, layout); 
							}
							catch (Exception e)
							{
								// Si falla la construcción de la versión móvil se genera una llamada a la versión clásica
								_log.warn( String.format("getURIPtesByITTP [%s %s] (Error %s)", uripte.toString(), friendlyURL, e.toString()) );
								_log.debug(e);
							}
						}
						
						URL url = new URL(protocol, articleURL.getHost(), friendlyURL);
						URIPte sectionURIPte = new URIPte(uripte.getApacheHost(), url.toString(), uripte.getOrder());
						sectionURIPteList.add(sectionURIPte);
					}
					catch (Exception e)
					{
						// Si falla una sección del artículo se pasa a la siguiente
						_log.warn( String.format("getURIPtesByITTP [%s] (Error %s)", uripte.toString(), e.toString()) );
						_log.debug(e);
					}
				}
			}
			
			if (_log.isTraceEnabled() && sectionURIPteList.size() > 0)
			{
				StringBuilder msg = new StringBuilder("getURIPtesByITTP: Sections\n");
				for (URIPte sectionURIPte : sectionURIPteList)
				{
					msg.append(sectionURIPte.toString()).append("\n");
				}
				_log.trace(msg);
			}
		}
		
		return sectionURIPteList;
	}
	
	/**
	 * Si el parámetro está activo elimina los duplicados. Considerará elementos iguales aquellos que tengan la misma URL
	 * @param newURIPteList
	 */
	private void removeDuplicatedURIPtes(List<URIPte> newURIPteList)
	{
		if (PropsValues.ITER_APACHE_QUEUE_PRODUCER_INCLUDE_ARTICLE_SECTIONS && newURIPteList.size() > 1)
		{
			// Se utiliza un HashSet porque el orden solo se marca con el campo order
			Set<URIPte> set = new HashSet<URIPte>(newURIPteList);
			newURIPteList.clear();
			newURIPteList.addAll(set);
		}
	}
	
	/**
	 * Se conecta con el Apache maestro y obtiene la lista de URIPtes.
	 * 
	 * @return Devuelve dicha lista ordenada por la prioridad que marca el Apache.
	 * @throws IOException
	 * @throws ServiceError
	 */
	private List<URIPte> acquireURIPtes() throws IOException, ServiceError
	{
		List<URIPte> newURIPteList = new ArrayList<URIPte>();
		
		String apacheHost = getApacheMaster();

		// Se abre la conección con el Apache y se le pide la lista de operaciones
		URL apacheServiceURL 			 = new URL(apacheHost + CACHE_SERVICE);
        URLConnection connection 		 = apacheServiceURL.openConnection();
        HttpURLConnection httpConnection = (HttpURLConnection)connection;
        httpConnection.setConnectTimeout( ApacheUtil.getApacheConnTimeout() );
        httpConnection.setReadTimeout( ApacheUtil.getApacheReadTimeout() );

        connection.setRequestProperty ("User-Agent", WebKeys.USER_AGENT_ITERWEBCMS);
        httpConnection.connect();	

        if (_log.isTraceEnabled())
        	_log.trace("HttpURLConnection response code:"+ httpConnection.getResponseCode());

        if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK)
        {
        	// Lectura de datos correcta
	        BufferedReader in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
	        String currentLine;
	        
	        
	        while ((currentLine = in.readLine()) != null) 
	        {
	        	if (_log.isTraceEnabled())
	        		_log.trace("URLpte: ".concat(currentLine));
	        	
	        	String[] data = currentLine.split(",");
	        		
	        	if (data != null && data.length == 2)
	        	{
	        		try
	        		{
	        			// La línea contiene un dato válido "ULR, order"
	        			URIPte uripte = new URIPte(apacheServiceURL.getHost(), data[0], Integer.parseInt(data[1]));
	        			
	        			// Se añade la URIPte que proporciona el servidor
	        			newURIPteList.add( uripte );
	        			
	        			// Se añaden las URIPtes de las secciones donde está asignado el artículo
	        			newURIPteList.addAll( getURIPtesByITTP(uripte) );
	        		}
	        		catch (Exception e)
	        		{
	        			_log.warn( String.format("%s %s not cached (Error %s)", data[0], data[1], e.toString()) );
	        			_log.debug(e);
	        		}
		        }
	        }
			in.close();
        }
        else
        {
        	String errorMsg = "";
        	try
        	{
        		// ITER-720 Reutilización de las conexiones HTTP contra los servidores que soportan el header "Connection: keep-alive"
        		errorMsg = GetterUtil.getString( StreamUtil.toString(httpConnection.getErrorStream(), "UTF-8"), "" );
        	}
        	catch (IOException e1) {}

        	 _log.error( errorMsg.concat( GetterUtil.getString(httpConnection.getResponseMessage(), "") ) );
        }
        httpConnection.disconnect();

        // Se borran las URIPtes duplicadas
        removeDuplicatedURIPtes(newURIPteList);
        
        // Se ordena la lista
        Collections.sort(newURIPteList);
        
		if (_log.isTraceEnabled() && newURIPteList.size() > 0)
		{
			StringBuilder msg = new StringBuilder("acquireURIPtes: List\n");
			for (URIPte sectionURIPte : newURIPteList)
			{
				msg.append(sectionURIPte.toString()).append("\n");
			}
			_log.trace(msg);
		}

        return newURIPteList;
	}
}
