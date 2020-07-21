package com.protecmedia.iter.xmlio.service.util;

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
import com.liferay.portal.model.LayoutTemplateConstants;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutTemplateLocalServiceUtil;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.util.CacheRefresh;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

public class LayoutTemplatePublisher
{
	private static ReentrantLock _lock = new ReentrantLock();
	
	private static void lockProcess() throws ServiceError
	{
		ErrorRaiser.throwIfFalse( _lock.tryLock(), IterErrorKeys.XYZ_E_LAYOUTTEMPLATE_PUBLISH_ARE_BUSY_ZYX );
	}
	
	private static void unlockProcess()
	{
		_lock.unlock();
	}
	
	public static File sendPublishableInfo(long groupId, String templateIds) throws Exception
	{
		File publishableFile = null;
		lockProcess();
		
		try
		{
			publishableFile = _sendPublishableInfo(groupId, templateIds);
		}
		finally
		{
			unlockProcess();
		}
		return publishableFile;
	}
	
	private static File _sendPublishableInfo(long groupId, String templateIds) throws Exception
	{
		// Información de las plantillas a publicar
		Document infoDom = LayoutTemplateLocalServiceUtil.getTemplateAsDOM(LayoutTemplateConstants.TplType.INTERNAL, templateIds);
		
		infoDom.getRootElement().addAttribute(LayoutTemplateConstants.FIELD_GROUPNAME, GroupLocalServiceUtil.getGroup(groupId).getFriendlyURL());
		
		// Se obtiene la configuración del LIVE
		LiveConfiguration liveConf	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(IterGlobal.getCompanyId());
		
		// Se guarda el fichero en el disco
		String fullFilePath  = XMLIOUtil.generateFullFilePath(liveConf.getLocalPath(), "LayoutTemplates", ".xml");
		File publishableFile = XMLIOUtil.generateFile(fullFilePath, infoDom.asXML());
		XMLIOUtil.sendFile(liveConf, publishableFile);

		return publishableFile;
	}
	
	public static void publishTemplates(long groupId, String templateIds) throws Exception
	{
		lockProcess();
		
		try
		{
			File publishableFile = _sendPublishableInfo(groupId, templateIds);
			
			// Se llama al LIVE para importar las plantillas, y posteriormente borrar la caché y notificar a los Apaches
			executeJSONRemoteCalls(groupId, publishableFile.getName());
		}
		finally
		{
			unlockProcess();
		}
	}
	
	private static void executeJSONRemoteCalls(long groupId, String fileName) throws PortalException, SystemException, ClientProtocolException, IOException, DocumentException, ServiceError
	{
		LiveConfiguration liveConf	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(IterGlobal.getCompanyId());
		
		// Se notifica al Live para que realice la importación
		List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
		remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.xmlio.service.ChannelServiceUtil"));
		remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"importLayoutTemplatesToLive"));
		remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[fileName]"));
		remoteMethodParams.add(new BasicNameValuePair("fileName", 			fileName));
		
		String []url = liveConf.getRemoteIterServer2().split(":");
		HttpHost targetHost = new HttpHost(url[0], Integer.valueOf(url[1]));
		JSONUtil.executeMethod(targetHost, "/xmlio-portlet/secure/json", remoteMethodParams, 
								(int)liveConf.getConnectionTimeOut(),
								(int)liveConf.getOperationTimeOut(),
								liveConf.getRemoteUserName(), liveConf.getRemoteUserPassword());

		// Actualiza la caché remota y llama a las URLptes
		CacheRefresh cr = new CacheRefresh(groupId);
		cr.setRefreshLayoutTemplates(true);

		XMLIOUtil.deleteRemoteCache(cr);
	}



}
