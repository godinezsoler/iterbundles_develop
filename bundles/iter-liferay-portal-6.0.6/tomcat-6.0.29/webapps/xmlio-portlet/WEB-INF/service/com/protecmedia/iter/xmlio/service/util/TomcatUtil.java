package com.protecmedia.iter.xmlio.service.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.liferay.portal.apache.ApacheHierarchy;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.sectionservers.SectionServersMgr;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.protecmedia.iter.base.service.NewsletterMgrServiceUtil;
import com.protecmedia.iter.base.service.PageRendererLocalServiceUtil;
import com.protecmedia.iter.base.service.apache.URIPteMgr;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.xmlio.service.CommunityPublisherLocalServiceUtil;

public class TomcatUtil extends Thread 
{
	private static Log _log = LogFactoryUtil.getLog(TomcatUtil.class);  
	private Element _info	= null;
	private CacheRefresh _cacheRefresh = null;
	
	public TomcatUtil(CacheRefresh cacheRefresh)
	{
		_cacheRefresh = cacheRefresh;
		_info = cacheRefresh.getGroups2Update();
	}
	
	/**
	 * Método utilizado para invalidar la caché desde IterAdmin
	 * @see http://confluence.protecmedia.com:8090/x/xQfhAg
	 * @param groupId
	 * @throws PortalException
	 * @throws SystemException
	 * @throws DocumentException
	 */
	static public void invalidate(long groupId) throws PortalException, SystemException, DocumentException
	{
		TomcatUtil tomcatUtil = new TomcatUtil( new CacheRefresh(groupId) );
		tomcatUtil.openApaches();
	}
	
	public void run() 
	{ 
		openApaches();
		
		try
		{
			// Se rearranca el productor de URIPtes
			URIPteMgr.restartURIPteProducer();
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// private void updatePublicationDate()
	//
	// Con respecto a la actualización hay varios puntos "dudosos":
	//	1- 	Se borra la caché del Apache siempre que haya habido un "intento" de operación. Nunca se comprueba 
	//		si dicha operación se realizó correctamente.
	//
	//	2-	Se actualiza la fecha siempre que se hayan mandado a refrescar los Apaches, no se comprueba si alguno
	//		o todos los Apaches fallaron durante dicho intento. Debería hacerse si almenos un Apache limpió la 
	//		caché correctamente y entonces mostrará contenidos nuevos (asumiendo al menos una operación de publicación
	//		se realizó correctamente. ver punto dudoso #1)
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	
	private static void updatePublicationDate(long companyId, long groupId) throws IOException, SQLException, ServiceError, SystemException, Exception
	{
		Group group = GroupLocalServiceUtil.getGroup(groupId);
		_log.info( String.format("Updating %s publication date",  group.getName()) );
		GroupMgr.updatePublicationDate(group.getName(), new Date());
	}
	
	public static void updatePublicationDateNoException(long companyId, long groupId)
	{
		try
		{
			updatePublicationDate(companyId, groupId);
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
	}
	
	private void openApaches()
	{
		_log.trace("openApaches: start");
		try
		{
			// Se recuperan las URLs de los Apaches
			ApacheHierarchy ah = new ApacheHierarchy();
			String[] currentApacheURLs = (String[])ArrayUtils.addAll(ah.getMasterList(), ah.getSlaveList());
			
			if (currentApacheURLs.length > 0)
			{
				List <Node> groups = _info.selectNodes(IterKeys.GROUPS2UPDT_NODE);
				invalidateApacheCache(currentApacheURLs, groups);
				
				// Después de invalidar la caché de los Tomcats y los Apaches:
				// 1- Se actualizan los temas por artículos
				// 2- Se actualiza el calendario de publicaciones de los artículos
				if (_cacheRefresh.refreshScheduledPublications() && Validator.isNotNull(_cacheRefresh.publicationId()))
				{
					PageRendererLocalServiceUtil.updateArticlesRenderers();
					CommunityPublisherLocalServiceUtil.updateSchedule();
				}
				

				for (Node grp : groups)
				{
					String lastUpdate 	= XMLHelper.getTextValueOf(grp, "@lastUpdate");
					
					// Inicio la tarea de enviar los boletines de noticias de tipo alerta
					if (Validator.isNotNull(lastUpdate))
					{
				    	 Calendar cal = Calendar.getInstance();
						 cal.setTimeInMillis(Long.parseLong(lastUpdate));
						 Date lastUpdateDate = cal.getTime();

					    _log.trace("Starting alert newsletter tasks");
						NewsletterMgrServiceUtil.startAlertNewslettersTask(XMLHelper.getTextValueOf(grp, "@groupId"), lastUpdateDate);
					}
					else
					{
						_log.debug( String.format("Last update in %s is null, alert newsletters task won't be started", XMLHelper.getTextValueOf(grp, "@groupName")) );
					}
				}
			}
			else
			{
				//_log.error("Current value for apache.servers.urls: " + apacheURLProperty);
				_log.error("Apache cache will be not invalidated");
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		_log.trace("openApaches: end");
	}
	
	private void invalidateApacheCache(String[] currentApacheURLs, List <Node>groups)
	{
		for (Node grp : groups)
		{
			try
			{
				long groupId = XMLHelper.getLongValueOf(grp, "@groupId");
				String virtualhost = LayoutSetLocalServiceUtil.getLayoutSet(groupId, false).getVirtualHost();
				
				if (Validator.isNull(virtualhost))
				{
					_log.error("Empty virtualhost for groupId: " + groupId);
					continue;
				}
				
				// Para dicho grupo, se pide el listado de URIPtes al servlet para que el Apache lo cachee
				for (String url : currentApacheURLs)
					callGetURIPtesServlet(url, virtualhost);
				
				// Para dicho grupo, se borra la caché de todos los apaches (primero los maestros y luego los esclavos)
				for (String url : currentApacheURLs)
				{
					// Primero se invalida la caché del sitio movil en caso de tener. 
					// Es necesario que en el Apache esté la "reglaRewriteCond %{REQUEST_URI} !^/delete-cache"
					if (SectionServersMgr.needProcessMobile(groupId))
						deleteCache(url, IterGlobal.getMobileConfig(groupId).getMobileServer());
					
					deleteCache(url, virtualhost);
				}
			}
			catch(Exception e)
			{
				_log.error(e.toString());
				_log.trace(e);
			}
		}
	}
	
	private void callGetURIPtesServlet(String url, String virtualhost) throws IOException
	{
		if (PropsValues.ITER_APACHE_QUEUE_GETURIPTES_SERVLET_ENABLED)
		{
			HttpURLConnection httpConnection = null;
			
			try
			{
				ErrorRaiser.throwIfNull(url);
				
				if (_log.isTraceEnabled())
					_log.trace( String.format("GetURIPtesServlet: start(%s)", url) );
				
				URL apacheURL	= new URL(url.concat("/geturiptes")); 
				httpConnection 	= (HttpURLConnection)apacheURL.openConnection();
		
				httpConnection.setRequestProperty (WebKeys.HOST, virtualhost);
				httpConnection.setRequestProperty (HttpHeaders.USER_AGENT, WebKeys.USER_AGENT_ITERWEBCMS);
				
				httpConnection.connect();
				int responseCode = httpConnection.getResponseCode();
				
				// Se consume la respuesta
				if (responseCode == HttpURLConnection.HTTP_OK)
				{
					_log.info("Get URIPtes finished: " + url + " (" + virtualhost + ")");
					StreamUtil.toString(httpConnection.getInputStream(), StringPool.UTF8);
				}
		        else 
	        	{
					_log.error("Get URIPtes failed: " + url + " (" + virtualhost + ")");
					_log.error(httpConnection.getResponseCode() + ": " + httpConnection.getResponseMessage());	
					StreamUtil.toString(httpConnection.getErrorStream(), StringPool.UTF8);
	        	}
			}
			catch (Exception e)
			{
				_log.error(e.toString());
				_log.trace(e);
			}
			finally
			{
				if (httpConnection != null)
					httpConnection.disconnect();
				
				if (_log.isTraceEnabled())
					_log.trace( String.format("GetURIPtesServlet: end(%s)", url) );
			}
		}
	}

	private void deleteCache(String url, String virtualhost) throws IOException
	{
		HttpURLConnection httpConnection = null;
		
		try
		{
			ErrorRaiser.throwIfNull(url);
			
			if (_log.isTraceEnabled())
				_log.trace( String.format("invalidateApacheCache: start(%s %s)", virtualhost, url) );
			
			URL apacheURL	= new URL(url + IterKeys.DELETE_CACHE_URL); 
			httpConnection 	= (HttpURLConnection)apacheURL.openConnection();
	
			httpConnection.setRequestProperty (WebKeys.HOST, virtualhost);
			httpConnection.setRequestProperty (HttpHeaders.USER_AGENT, WebKeys.USER_AGENT_ITERWEBCMS);
			httpConnection.connect();
			
			if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK)
			{
				_log.info("Apache cache invalidated: " + url + " (" + virtualhost + ")");
				StreamUtil.toString(httpConnection.getInputStream(), StringPool.UTF8);
			}
			else
			{
				_log.error("Apache cache not invalidated: " + url + " (" + virtualhost + ")");
				_log.error(httpConnection.getResponseCode() + ": " + httpConnection.getResponseMessage());	
				StreamUtil.toString(httpConnection.getErrorStream(), StringPool.UTF8);
			}
		}
		catch (Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		finally
		{
			if (httpConnection != null)
				httpConnection.disconnect();
			
			if (_log.isTraceEnabled())
				_log.trace( String.format("invalidateApacheCache: end(%s %s)", virtualhost, url) );
		}
	}
}