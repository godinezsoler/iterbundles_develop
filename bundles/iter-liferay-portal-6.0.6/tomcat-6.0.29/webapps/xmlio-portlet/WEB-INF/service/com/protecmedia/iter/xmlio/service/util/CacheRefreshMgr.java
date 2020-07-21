package com.protecmedia.iter.xmlio.service.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.lang.ArrayUtils;

import com.liferay.portal.apache.ApacheHierarchy;
import com.liferay.portal.apache.ApacheUtil;
import com.liferay.portal.kernel.cache.MultiVMPoolUtil;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.ImageFramesUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.QualificationTools;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.advertisement.MetadataAdvertisementTools;
import com.liferay.portal.kernel.velocity.IterVelocityTools;
import com.liferay.portal.service.LayoutTemplateLocalServiceUtil;
import com.liferay.portlet.journal.util.GlobalJournalTemplateMgr;
import com.protecmedia.iter.base.service.apache.URIPteMgr;
import com.protecmedia.iter.base.service.cache.CacheInvalidationController;
import com.protecmedia.iter.base.service.util.ErrorRaiser;

public class CacheRefreshMgr
{
	private static Log _log = LogFactoryUtil.getLog(CacheRefreshMgr.class);
	private static ConcurrentProcessMgr		_processMgr = new ConcurrentProcessMgr();
	
	public static void refresh(CacheRefresh cacheRefresh) throws Exception
	{
		_log.info("Refresh cache process is starting");
		
		if(cacheRefresh.refreshCategoryArticles())
			IterVelocityTools.executeInitAbouCategoryArticles();

		if(cacheRefresh.refreshSectionArticles())
			IterVelocityTools.executeInitAboutSectionArticles();
		
		if(cacheRefresh.refreshGlobalJournalTemplates())
			GlobalJournalTemplateMgr.modify();
		
		if(cacheRefresh.refreshQualifications())
			QualificationTools.initQualifications();
		
		if(cacheRefresh.refreshMetadataAdvertisement())
			MetadataAdvertisementTools.initMetadataAdvertisement();
		
		if(cacheRefresh.refreshImageFrames())
			ImageFramesUtil.loadImageFrames();
		
		if(cacheRefresh.refreshFrameSynonyms())
			ImageFramesUtil.loadFrameSynonyms();
		
		if(cacheRefresh.refreshAlternativeTypes())
			ImageFramesUtil.loadAlternativeTypes();
		
		if(cacheRefresh.refreshWatermarks())
			ImageFramesUtil.loadWatermarks();
		
		if(cacheRefresh.refreshContextVariables())
			IterVelocityTools.initContextVariables();
		
		if(cacheRefresh.refreshLayoutTemplates())
			LayoutTemplateLocalServiceUtil.loadLayoutTemplates();

//		if(Validator.isNotNull(cacheRefresh.sendMasNotification()))
//		{
//			MASUtil masUtil = new MASUtil(cacheRefresh.sendMasNotification(), true);
//			masUtil.start();
//		}

		// Si no se refrescan los Apaches no tiene sentido recalcular la invalidación de cachés
		if (cacheRefresh.refreshTomcatAndApache() && cacheRefresh.rescheduleCacheInvalidation())
			CacheInvalidationController.reschedule();

		// Siempre lo hará salvo que se indique explícitamente que no se efectúe
		if(cacheRefresh.refreshTomcatAndApache())
			refreshTomcatAndApache(cacheRefresh);
		
		_log.info("Refresh cache process has finished");
	}
	
	private static void refreshTomcatAndApache(CacheRefresh cacheRefresh) throws Exception
	{
		_processMgr.increment();
		
		// Se limpia la cola de URIPtes y se pausa el productor
		URIPteMgr.pauseURIPteProducer(cacheRefresh.getGroupIDs());
		
		_processMgr.mergeProcessResult( refreshTomcat(), cacheRefresh );
		
		// Termina el borrado de la caché de los tomcats
		cacheRefresh = _processMgr.decrement();
		
		if (Validator.isNotNull(cacheRefresh))
		{
			try
			{
				TomcatUtil tomcatUtil = new TomcatUtil(cacheRefresh);
				tomcatUtil.start();
			}
			catch (Exception e)
			{
				try
				{
					// Solo en caso de fallo se rearranca el productor de URIPtes. Si no hay fallo se rearrancará tras borrar la caché de los Apaches
					URIPteMgr.restartURIPteProducer();
				}
				catch (Throwable th)
				{
					_log.error(th);
				}


				throw e;
			}
		}
		else
		{
			// No es el último de los refrescos en paralelo, o es el último pero ninguno borró la caché correctamente
			URIPteMgr.restartURIPteProducer();
		}
	}
	
	/**
	 * Borra la caché de TODOS los TOMCATs
	 * @return true si se borró correctamente la caché de todos los TOMCATs, false en caso contrario
	 */
	public static boolean refreshTomcat()
	{
		boolean refreshOK = false;
		
		// Se borra la caché del cluster
		try
		{
			refreshOK = MultiVMPoolUtil.clear();
		}
		catch (Throwable th)
		{
			_log.error(th.toString());
			_log.trace(th);
		}

		return refreshOK;
	}
	
	
	static public void refreshContentCache(String url) throws IOException, ServiceError
	{
		ErrorRaiser.throwIfFalse(!PropsValues.IS_PREVIEW_ENVIRONMENT, IterErrorKeys.XYZ_E_UNEXPECTED_ENVIRONMENT_ZYX);
		ErrorRaiser.throwIfNull(url, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Se recuperan las URLs de los Apaches
		ApacheHierarchy ah = new ApacheHierarchy();
		String[] currentApacheURLs = (String[])ArrayUtils.addAll(ah.getMasterList(), ah.getSlaveList());

		for (String apacheURL : currentApacheURLs)
		{
			String apacheHost = new URL(apacheURL).getHost();
			HttpURLConnection httpConnection = null;
			
			String hostProperty = "";
			String urlURIPte	= "";
			
			try
			{
				hostProperty 		= new URL(url).getHost();
				urlURIPte 			= url.replaceFirst(hostProperty, apacheHost);
				URL operationURL 	= new URL( HttpUtil.forceProtocol(urlURIPte, Http.HTTP_WITH_SLASH) );

		        httpConnection = (HttpURLConnection)operationURL.openConnection();
		        httpConnection.setConnectTimeout( ApacheUtil.getApacheConnTimeout() );
		        httpConnection.setReadTimeout( ApacheUtil.getApacheReadTimeout() );
		        httpConnection.setInstanceFollowRedirects(false);
		        
		        httpConnection.setRequestProperty (WebKeys.HOST, hostProperty);
		        httpConnection.setRequestProperty ("User-Agent", 		WebKeys.USER_AGENT_ITERWEBCMS_FULL);
		        httpConnection.setRequestProperty ("Accept-Encoding", 	"gzip, deflate");

				if (_log.isTraceEnabled())
					_log.trace( String.format("%s: %s", WebKeys.HOST, httpConnection.getRequestProperty(WebKeys.HOST)));

		        httpConnection.connect();
	            HttpUtil.throwIfConnectionFailed( httpConnection, IterErrorKeys.XYZ_E_CATALOG_URLCONNECTION_FAILED_ZYX );
	            
	        	// Se consumen el HTML. Es importante consumirlo de esta forma porque de lo contrario NO se reaproechan las conexiones HTTP
	            // ITER-720 Reutilización de las conexiones HTTP contra los servidores que soportan el header "Connection: keep-alive"
	            _log.debug( StreamUtil.toString(httpConnection.getInputStream(), StringPool.UTF8) );
			}
			finally
			{
				if (httpConnection != null)
					httpConnection.disconnect();
			}
		}
	}

}
