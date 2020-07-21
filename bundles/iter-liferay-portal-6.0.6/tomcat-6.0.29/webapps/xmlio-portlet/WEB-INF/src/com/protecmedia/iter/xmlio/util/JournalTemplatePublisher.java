package com.protecmedia.iter.xmlio.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portlet.journal.service.JournalTemplateLocalServiceUtil;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.util.CacheRefresh;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

public class JournalTemplatePublisher
{
	private static ReentrantLock _lock = new ReentrantLock();
	
	private static void lockProcess() throws ServiceError
	{
		ErrorRaiser.throwIfFalse( _lock.tryLock(), IterErrorKeys.XYZ_E_JOURNALTEMPLATE_PUBLISH_ARE_BUSY_ZYX );
	}
	
	private static void unlockProcess()
	{
		_lock.unlock();
	}
	
	public static void publishTemplates(long groupId, String templateIds) throws Exception
	{
		lockProcess();
		
		try
		{
			// Información de las plantillas a publicar
			Document infoDom = JournalTemplateLocalServiceUtil.getTemplatesAsDOM(groupId, templateIds);
			
			// Se obtiene la configuración del LIVE
			LiveConfiguration liveConf	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(IterGlobal.getCompanyId());
			
			// Se guarda el fichero en el disco
			String fullFilePath  = XMLIOUtil.generateFullFilePath(liveConf.getLocalPath(), "JournalTemplates", ".xml");
			File publishableFile = XMLIOUtil.generateFile(fullFilePath, infoDom.asXML());
			XMLIOUtil.sendFile(liveConf, publishableFile);

			// Se llama al LIVE para importar las plantillas, y posteriormente borrar la caché y notificar a los Apaches
			executeJSONRemoteCalls(liveConf, publishableFile.getName());
		}
		finally
		{
			unlockProcess();
		}
	}
	
	private static void executeJSONRemoteCalls(LiveConfiguration liveConf, String fileName) throws PortalException, SystemException, ClientProtocolException, IOException, DocumentException, NumberFormatException, ServiceError
	{
		// Se notifica al Live para que realice la importación
		List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
		remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.xmlio.service.ChannelServiceUtil"));
		remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"importJournalTemplatesToLive"));
		remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[fileName]"));
		remoteMethodParams.add(new BasicNameValuePair("fileName", 			fileName));
		
		String []url = liveConf.getRemoteIterServer2().split(":");
		HttpHost targetHost = new HttpHost(url[0], Integer.valueOf(url[1]));
		JSONUtil.executeMethod(targetHost, "/xmlio-portlet/secure/json", remoteMethodParams, 
								(int)liveConf.getConnectionTimeOut(),
								(int)liveConf.getOperationTimeOut(),
								liveConf.getRemoteUserName(), liveConf.getRemoteUserPassword());

		// Es preferible que NO se refresquen los Apaches a referescar la caché de TODOS los grupos de TODAS las delegaciones. Se refrescará cada TPU cuando publique cosas concretas.
		// Actualiza únicamente la caché de los Journal Templates.
		CacheRefresh cr = new CacheRefresh(GroupMgr.getGlobalGroupId());
		cr.setRefreshGlobalJournalTemplates(true);
		cr.setRefreshTomcatAndApache(false);
		cr.setRescheduleCacheInvalidation(false);

		XMLIOUtil.deleteRemoteCache(cr);
	}

}
