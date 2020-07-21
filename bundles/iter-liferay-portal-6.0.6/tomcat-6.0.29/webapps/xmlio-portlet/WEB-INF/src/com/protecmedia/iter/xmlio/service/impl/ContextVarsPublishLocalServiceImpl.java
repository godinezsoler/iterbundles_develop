/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.protecmedia.iter.xmlio.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang.ArrayUtils;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.zip.ZipWriter;
import com.liferay.portal.kernel.zip.ZipWriterFactoryUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portlet.CtxvarUtil;
import com.protecmedia.iter.base.service.ContextVarsMgrLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.base.ContextVarsPublishLocalServiceBaseImpl;
import com.protecmedia.iter.xmlio.service.util.CacheRefresh;
import com.protecmedia.iter.xmlio.service.util.FTPUtil;
import com.protecmedia.iter.xmlio.service.util.PublishUtil;
import com.protecmedia.iter.xmlio.service.util.TomcatUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;
import com.protecmedia.iter.xmlio.service.util.ZipUtil;

/**
 * The implementation of the context vars mgr local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.service.ContextVarsMgrLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.xmlio.service.ContextVarsMgrLocalServiceUtil} to access the context vars mgr local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.xmlio.service.base.ContextVarsMgrLocalServiceBaseImpl
 * @see com.protecmedia.iter.xmlio.service.ContextVarsMgrLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class ContextVarsPublishLocalServiceImpl extends ContextVarsPublishLocalServiceBaseImpl
{
	
	private static Log _log = LogFactoryUtil.getLog(ContextVarsPublishLocalServiceImpl.class);
	
	private static final String ITER_CONTEXTVARS_ZIP_FILE_NAME = IterKeys.XMLIO_ZIP_FILE_PREFIX + "contextvars_%s.zip";
	
	private static final String GET_CONTEXT_VARS_IDS = String.format(new StringBuilder(
		"SELECT varid 																					\n").append(
		"FROM ctxvars 																					\n").append(
		"  WHERE groupid = %%s 																			\n").append(
		"	%s																							\n").append(
		"																								\n").append(
		"    AND (publicationdate is null OR (modifieddate > publicationdate) )							\n").append(
		"    AND source = '%%s' 																		\n").append(
		"	%%s																							\n").toString(), CtxvarUtil.PUBLISHED_LAYOUTS);
	
	private static final String GET_LOCAL_IDS = new StringBuilder(
									"SELECT varid, globalid FROM ctxvars WHERE globalid IN ('%1$s') \n"			).append(
									"UNION ALL \n"																).append(
									"SELECT varid, globalid FROM ctxvarscategory WHERE globalid IN ('%1$s') \n"	).toString();
	
	//Sólo una publicación al mismo tiempo
	private static ReadWriteLock globalLock = new ReentrantReadWriteLock();
	private static Lock writeLock = globalLock.writeLock();
	
	// Llamada desde ITER
	public boolean publishToLive(long scopeGroupId) throws Exception
	{
		return publishCtxVars(scopeGroupId, IterKeys.ITER, null, "0", true);
	}
	
	// Antigua llamada desde MLN
	@Deprecated
	public void publishCtxVarsToLive(long scopeGroupId, String[] ctxVarIds) throws Exception
	{
		throw new Exception(IterErrorKeys.ITER_UPDATE_MILENIUM_ITER);
	}
	
	/**
	 * @throws Exception 
	 * 
	 */
	public boolean publishCtxVars(long scopeGroupId, String source, Long plid, String recursive, boolean refreshRemoteCache) throws Exception
	{
		return publishCtxVars2(scopeGroupId, source, new String[]{String.valueOf(plid)}, recursive, refreshRemoteCache);
	}
	public boolean publishCtxVars2(long scopeGroupId, String source, String[] plids, String recursive, boolean refreshRemoteCache) throws Exception
	{
		boolean published = false;
		ErrorRaiser.throwIfFalse( PropsValues.ITER_ENVIRONMENT.equals(WebKeys.ENVIRONMENT_PREVIEW), IterErrorKeys.XYZ_E_XPORTCONTENT_ALL_FAILED_ZYX);
		
		if (writeLock.tryLock())
		{
			File localFile = null;
			try
			{
				// Se recupera la configuración de la publicación
				LiveConfiguration liveConf 	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(IterGlobal.getCompanyId());
				
				List<String> ctxVars = new ArrayList<String>();
				
				for (String plid : plids)
				{
					// ids de las variables de contexto a publicar
					String[] ctxVarIds = getContextVarsIds(scopeGroupId, source, Validator.isNull(plid)?null:Long.parseLong(plid), recursive);
					
					for (String ctxVarId : ctxVarIds)
						ctxVars.add(ctxVarId);
				}
				
				// MUY IMPORTANTE el NO exportar si no hay elementos, ya que generateExportElement exporta TODAS las variables si no se le especifican unas concretas,
				// intentando publicar variables de otras secciones, o de secciones que no son publicables ni existen en el LIVE generando un fallo.
				if (!ctxVars.isEmpty() || source.equals(IterKeys.ITER))
				{
					String[] ctxVarIds = ctxVars.toArray(new String[ctxVars.size()]);
					
					//Generamos el .xml de exportación (el atributo source define si es MLN o ITER quien llama
					Element rs = ContextVarsMgrLocalServiceUtil.generateExportElement(scopeGroupId, source, ctxVarIds);
					
					//Comprobamos que no es una publicación vacía
					PublishUtil.checkEmptyPublication(rs);
					
					//Generamos el .zip a exportar
					localFile = generateExportFile(liveConf.getLocalPath(), rs);
					
					//Enviamos por FTP/File System el .zip generado
					XMLIOUtil.sendFile(liveConf, localFile);
					
					// Realizar la llamada al Live para que importe el .zip
					executeJSONRemoteCalls(liveConf, scopeGroupId, localFile.getName(), refreshRemoteCache);
					
					published = true;
					
					// Actualizar la fecha de publicación de los contenidos exportados
					ContextVarsMgrLocalServiceUtil.updatePublicationDateContents(scopeGroupId, rs);
				}
			}
			finally
			{
				writeLock.unlock();
				
				//Borramos el fichero de exportación
				PublishUtil.hotConfigDeleteFile(localFile);	
			}
		}
		else
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_PUBLISH_ALREADY_IN_PROCESS_ZYX);
		}
		
		return published;
	}
	
	private void executeJSONRemoteCalls(LiveConfiguration liveConf, long groupId, String fileName, boolean refreshRemoteCache) throws PortalException, SystemException, ClientProtocolException, IOException, DocumentException, ServiceError
	{
		// Se notifica al Live para que realice la importación
		String scopeGroupName = GroupLocalServiceUtil.getGroup(groupId).getName();

		List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
		remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.xmlio.service.ContextVarsPublishServiceUtil"));
		remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"importContents"));
		remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[scopeGroupName, fileName]"));
		remoteMethodParams.add(new BasicNameValuePair("scopeGroupName", 	scopeGroupName));
		remoteMethodParams.add(new BasicNameValuePair("fileName", 			fileName));
		
		String []url = liveConf.getRemoteIterServer2().split(":");
		HttpHost targetHost = new HttpHost(url[0], Integer.valueOf(url[1]));
		JSONObject json = JSONUtil.executeMethod(targetHost, "/xmlio-portlet/secure/json", remoteMethodParams, 
									(int)liveConf.getConnectionTimeOut(),
									(int)liveConf.getOperationTimeOut(),
									liveConf.getRemoteUserName(), liveConf.getRemoteUserPassword());

		if (refreshRemoteCache)
		{
			// Actualiza el mapa en memoria, la caché remota y notifica a las URLptes
			Document dom = SAXReaderUtil.read(json.getString("returnValue"));
			String lastUpdate = XMLHelper.getStringValueOf( dom.getRootElement(), IterKeys.XMLIO_XML_LAST_UPDATE_ATTRIBUTE_ATTR, StringPool.BLANK);
			
			CacheRefresh cr = new CacheRefresh(scopeGroupName, lastUpdate);
			cr.setRefreshContextVariables(true);
			
			XMLIOUtil.deleteRemoteCache(cr);
		}
	}
	
	// Obtiene los ids de las variables de contexto que se van a publicar
	private String[] getContextVarsIds(long scopeGroupId, String source, Long plid, String recursive) throws SecurityException, NoSuchMethodException
	{
		_log.trace("In ContextVarsPublishLocalServiceImpl.getContextVarsIds");
		
		String[] cvIds = null;
		
		if (null == plid)
		{
			// Para ITER
			cvIds = ArrayUtils.EMPTY_STRING_ARRAY;
		}
		else
		{		
			// Para MLN
			String sql = null;
	
			// TPU
			if (0 == plid)
			{
				if ("1".equals(recursive))
				{				
					// Busqueda recursiva
					sql = String.format(GET_CONTEXT_VARS_IDS, scopeGroupId, source, "");		
				}
				else
				{
					// Busqueda no recursiva
					sql = String.format(GET_CONTEXT_VARS_IDS, scopeGroupId, source, "  AND plid is null");
				}
			}
			else
			{	
				// Sección dada
				if ("1".equals(recursive))
				{
					// Busqueda recursiva
					sql = String.format(GET_CONTEXT_VARS_IDS, scopeGroupId, source,
										new StringBuilder("  AND (plid = ").append(plid).append(" \n")
											      .append("       OR plid in (SELECT ITR_GET_LAYOUT_PLID_CHILDREN(").append(plid).append(", ").append(scopeGroupId).append(")) )") 
									    );		
				}
				else
				{
					// Busqueda no recursiva
					sql = String.format(GET_CONTEXT_VARS_IDS, scopeGroupId, source, new StringBuilder(" AND plid = ").append(plid));
				}
			}
			
			if (_log.isDebugEnabled())
				_log.debug(new StringBuilder("Query to get contextvarsids:\n").append(sql));
			
			Document doc = PortalLocalServiceUtil.executeQueryAsDom(sql);
			cvIds = XMLHelper.getStringValues(doc.getRootElement().selectNodes("//row"), "@varid");			
		}		
		return cvIds;
	}
	
	public Document importContents(String scopeGroupName, String fileName) throws Exception
	{
		Document dom = SAXReaderUtil.read("<rs/>");
		
		File importFile 	= null;
		File temporaryDir 	= null;
		
		try
		{
			long companyId = IterGlobal.getCompanyId();
			ErrorRaiser.throwIfFalse(writeLock.tryLock(), IterErrorKeys.XYZ_E_PUBLISH_ALREADY_IN_PROCESS_ZYX);
			LiveConfiguration liveConf 	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(companyId);

			String importFilePath = null;
			
			if (liveConf.getOutputMethod().equals(IterKeys.XMLIO_CHANNEL_MODE_FTP))
			{
				String ftpServer = liveConf.getFtpPath();
				String ftpUser = liveConf.getFtpUser();
				String ftpPassword = liveConf.getFtpPassword();
				String localPath = liveConf.getLocalPath();

				importFilePath = FTPUtil.receiveFile(ftpServer, ftpUser, ftpPassword, fileName, localPath, StringPool.BLANK);
			} 
			else
			{
				String remotePath = liveConf.getRemotePath();
				importFilePath = remotePath + File.separatorChar + fileName;	
			}
			
			String zipExtension = ".zip";

			String temporaryDirPath = importFilePath.replace(zipExtension, StringPool.BLANK);
			
			importFile = new File(importFilePath);
			temporaryDir = new File(temporaryDirPath);
			
			ZipUtil.unzip(importFile, temporaryDir);
			
			File iterXmlFile = new File(temporaryDirPath + File.separatorChar + IterKeys.XMLIO_XML_MAIN_FILE_NAME);
			Element rs = SAXReaderUtil.read(iterXmlFile).getRootElement();

			String groupname = XMLHelper.getTextValueOf(rs, "@groupname");
			ErrorRaiser.throwIfNull(groupname, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			long scopeGroupId = GroupLocalServiceUtil.getGroup(companyId, groupname).getGroupId();
			List<Node> ctxNodes = rs.selectNodes("//row");
			
			// Se guarda el varId del PREVIEW como prevId
			for (int i = 0; i < ctxNodes.size(); i++)
			{
				Element elem = (Element) ctxNodes.get(i);
				elem.addAttribute("prevId", elem.attributeValue("id"));
			}

			// A partir del globalId se obtiene el varId del LIVE
			String[] ctxVarsGlobalIds = XMLHelper.getStringValues(ctxNodes, "@globalid");
			String query = String.format(GET_LOCAL_IDS, StringUtil.merge(ctxVarsGlobalIds, "','"));
			List<Map<String, Object>> result = PortalLocalServiceUtil.executeQueryAsMap(query);
			
			if (result != null && result.size() > 0)
			{
				for (Map<String, Object> varIdGlobalId : result)
				{
					String varid 	= (String) varIdGlobalId.get("varid");
					String globalid = (String) varIdGlobalId.get("globalid");
					Element elem 	= ((Element)rs.selectSingleNode("//row[@globalid='"+globalid+"']"));
					if (elem != null)
					{
						elem.addAttribute("id", varid);
					}
				}
			}
			
			//DELETE
			ContextVarsMgrLocalServiceUtil.deleteContextVars(rs.selectSingleNode("ctxvarsdeleted"), StringPool.NULL, rs.selectSingleNode("advocabularydeleted"));
			
			//CREATE/UPDATE
			ContextVarsMgrLocalServiceUtil.createUpdateContextVars(temporaryDirPath, scopeGroupId, rs);
			
			// Se toma la fecha de última publicación y se actualiza dicho campo
			String lastUpdate = String.valueOf(GroupMgr.getPublicationDate(scopeGroupId).getTime());
			dom.getRootElement().addAttribute(IterKeys.XMLIO_XML_LAST_UPDATE_ATTRIBUTE, lastUpdate);
			
			TomcatUtil.updatePublicationDateNoException(IterGlobal.getCompanyId(), scopeGroupId);
		}
		finally
		{
			writeLock.unlock();
			
			//Borramos los ficheros de importación
			PublishUtil.hotConfigDeleteFile(temporaryDir);
			PublishUtil.hotConfigDeleteFile(importFile);
		}
		
		return dom;
	}
	
	private File generateExportFile(String localPath, Element rs) throws SecurityException, NoSuchMethodException, IOException, DocumentException, PortalException, SystemException
	{
		File exportFile = null;
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
		{
			String zipFileName = String.format(ITER_CONTEXTVARS_ZIP_FILE_NAME, Calendar.getInstance().getTimeInMillis());
			ZipWriter zipWriter = ZipWriterFactoryUtil.getZipWriter(new File(localPath + File.separatorChar + zipFileName));

			//Se añaden los binarios de las imágenes al .zip
			ContextVarsMgrLocalServiceUtil.addImagesToZIP(zipWriter, rs);
			
			//Se añade el .xml de publicacion al .zip
			zipWriter.addEntry(IterKeys.XMLIO_XML_MAIN_FILE_NAME, rs.asXML());

			//Se obtiene el fichero liberado
			exportFile = PublishUtil.getUnlockedFile(zipWriter);
		}
		
		return exportFile;
	}
}