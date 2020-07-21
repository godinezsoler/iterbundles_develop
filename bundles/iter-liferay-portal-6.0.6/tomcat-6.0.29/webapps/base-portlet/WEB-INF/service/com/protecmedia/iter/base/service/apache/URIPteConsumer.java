package com.protecmedia.iter.base.service.apache;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;

import com.liferay.portal.apache.ApacheUtil;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.cluster.Heartbeat;

public class URIPteConsumer implements Runnable 
{
	private static final Log _log = LogFactoryUtil.getLog(URIPteConsumer.class);
	
	static private final String DELETE_URIPETES = "DELETE FROM uriptes WHERE uripte = '%s'";
	
	private BlockingQueue<URIPte> 	_queue				= null;

	public URIPteConsumer(BlockingQueue<URIPte> queue)
	{
		_queue = queue;
	}
	
	@Override
	public void run() 
	{
		_log.info("Starting the URIPte consumer process");
		while (_isConsumerEnable())
		{	
			HttpURLConnection httpConnection = null;
			
			int responseCode 	= 0;
			String hostProperty = "";
			String urlURIPte	= "";
			
			try
			{
				URIPte uriPte = _queue.take();
				
				hostProperty 		= uriPte.getHost();
				urlURIPte 			= uriPte.getURL().replaceFirst(hostProperty, uriPte.getApacheHost());
				URL operationURL 	= new URL(HttpUtil.forceProtocol(urlURIPte, Http.HTTP_WITH_SLASH));

		        httpConnection = (HttpURLConnection)operationURL.openConnection();
		        httpConnection.setConnectTimeout( ApacheUtil.getApacheConnTimeout() );
		        httpConnection.setReadTimeout( ApacheUtil.getApacheReadTimeout() );
		        // 0010306: Mensajes de error "URIPteConsumer: java.net.ConnectException: Connection refused" (Conexión rehusada) 
		        // Las peticiones a 30x (migradas) redirigian a direcciones absolutas que podían resolverse  (con el archivo hosts o con los DNS) 
		        // a un HOST que no fuese el Apache y fallarían
		        httpConnection.setInstanceFollowRedirects(false);
		        
		        httpConnection.setRequestProperty (WebKeys.HOST, hostProperty);
		        httpConnection.setRequestProperty ("User-Agent", 		WebKeys.USER_AGENT_ITERWEBCMS);
		        httpConnection.setRequestProperty ("Accept-Encoding", 	"gzip, deflate");

				if (_log.isTraceEnabled())
					_log.trace( String.format("%s: %s", WebKeys.HOST, httpConnection.getRequestProperty(WebKeys.HOST)));

		        httpConnection.connect();
		        responseCode = httpConnection.getResponseCode();

				// Se consume el buffer, necesario para la reutilización de las conexiones HTTP
		        // ITER-720 Reutilización de las conexiones HTTP contra los servidores que soportan el header "Connection: keep-alive"
		        try
		        {
		        	StreamUtil.toString(httpConnection.getInputStream(), StringPool.UTF8);
		        }
		        catch (IOException e)
		        {
		        	try
		        	{
		        		StreamUtil.toString(httpConnection.getErrorStream(), StringPool.UTF8);
		        	}
		        	catch (IOException e1) {}
		        }
		        
				// Se notifica que se ha consumido la traza, y se le notifica a la BBDD si es necesario 
				// para que borre la URIPte
				notifyConsumption(uriPte, hostProperty, urlURIPte, httpConnection);
				// La traza se muestra una vez consumido el buffer
				if (_log.isDebugEnabled())
				{
					if (responseCode == HttpURLConnection.HTTP_OK)
						_log.debug( String.format("%s cached (host:%s)", urlURIPte, hostProperty) );
					else
			        	_log.debug( String.format("%s not cached (%s: %d) (host:%s)", urlURIPte, getResponseMessage(httpConnection), responseCode, hostProperty) );
				}
			}
			catch (InterruptedException ie)
			{
				_log.debug( String.format("%s not cached (%s: %d) (host:%s)", urlURIPte, getResponseMessage(httpConnection), responseCode, hostProperty) );
				
				Thread.currentThread().interrupt();
				_log.trace(ie.toString());
			}
			catch (Throwable th)
			{
				_log.debug( String.format("%s not cached (%s: %d) (host:%s)", urlURIPte, getResponseMessage(httpConnection), responseCode, hostProperty) );
				
				_log.error(th.toString());
				_log.trace(th);
			}
			finally
			{
				try
				{
					if (httpConnection != null)
						httpConnection.disconnect();
				}
				catch(Throwable e2){}
			}
		}
		_log.info("The URIPte consumer process has been finished");
	}
	
	/**
	 * Traza la URIpte consumida
	 * Si es una URIPte de las almacenadas en BBDD se borra de la tabla
	 * 
	 * @param hostProperty
	 * @param urlURIPte
	 * @param httpConnection
	 * @throws IOException
	 * @throws SQLException 
	 */
	private void notifyConsumption(URIPte uriPte, String hostProperty, String urlURIPte, HttpURLConnection httpConnection) throws IOException
	{
		int responseCode = httpConnection.getResponseCode();
		
		// La traza se muestra una vez consumido el buffer
		if (_log.isDebugEnabled())
		{
			if (responseCode == HttpURLConnection.HTTP_OK)
				_log.debug( String.format("%s cached (host:%s)", urlURIPte, hostProperty) );
			else
	        	_log.debug( String.format("%s not cached (%s: %d) (host:%s)", urlURIPte, getResponseMessage(httpConnection), responseCode, hostProperty) );
		}
		
		if (PropsValues.ITER_APACHE_QUEUE_GETURIPTES_SERVLET_ENABLED && uriPte.isIterURIPte())
		{
			if (_log.isDebugEnabled())
				_log.debug( String.format("Deleting from DB: %s", uriPte.getOriginalURL()) );
			
			try
			{
				PortalLocalServiceUtil.executeUpdateQuery( String.format(DELETE_URIPETES, uriPte.getOriginalURL()) );
			}
			catch (Exception e)
			{
				// Un fallo en el borrado no afecta al mecanismo, simplemente se traza 
				// y al no poder borrarse se mantendrá como URIPte por refrescar 
				_log.error(String.format("%s %s", urlURIPte, e.toString()), e);
			}
		}
	}

	private String getResponseMessage(HttpURLConnection httpConnection)
	{
		String responseMsg = "";
		if (httpConnection != null)
		{
			try
			{
				responseMsg = GetterUtil.getString(httpConnection.getResponseMessage(), IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
			}
			catch (Throwable th)
			{
				responseMsg = IterErrorKeys.XYZ_E_UNEXPECTED_ZYX.concat(th.toString());
				_log.trace(th);
			}
		}
		return responseMsg;
	}
	
	private boolean _isConsumerEnable()
	{
		return 	Thread.currentThread().isAlive() && !Thread.currentThread().isInterrupted() &&
				Heartbeat.canLaunchProcesses();
	}
}
