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

package com.protecmedia.iter.base.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang.StringUtils;
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
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.zip.ZipWriter;
import com.liferay.portal.kernel.zip.ZipWriterFactoryUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.base.ClusterLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.util.CacheRefresh;
import com.protecmedia.iter.xmlio.service.util.FTPUtil;
import com.protecmedia.iter.xmlio.service.util.PublishUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;
import com.protecmedia.iter.xmlio.service.util.ZipUtil;

/**
 * The implementation of the cluster local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.ClusterLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.ClusterLocalServiceUtil} to access the cluster local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.ClusterLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.ClusterLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class ClusterLocalServiceImpl extends ClusterLocalServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(ClusterLocalServiceImpl.class);
	
	private static String GET_CLUSTERS 				= new StringBuilder(
		"SELECT cluster.clusterid, clustername, 							\n").append(
		"		GROUP_CONCAT(Group_.name 	ORDER BY Group_.name) sitenames,\n").append(
		"		GROUP_CONCAT(Group_.groupid ORDER BY Group_.name) siteids	\n").append( 
		"FROM cluster														\n").append( 
		"INNER JOIN cluster_group USING (clusterid)							\n").append( 
		"INNER JOIN Group_ ON cluster_group.groupId = Group_.groupid		\n").append( 
		"  WHERE delegationId = %s											\n").append( 
		"GROUP BY cluster.clusterId											\n").append(
		"ORDER BY clustername												\n").toString();
	
	private static String DEL_CLUSTERS 				= "DELETE FROM cluster WHERE clusterid IN ('%s')";
	private static String DEL_CLUSTERS_DELEGATION 	= "DELETE FROM cluster WHERE delegationid = %d";
	
	private static String INSERT_CLUSTER 			= new StringBuilder(
		"INSERT INTO cluster(clusterid, clustername, delegationid) 	\n").append(
		" 	VALUES ('%s', '%s', %d)									\n").toString();
			
	private static String UPDT_CLUSTER 				= "UPDATE Cluster SET clustername='%s' WHERE clusterId = '%s'";
	
	private static String DEL_CLUSTER_GROUP 		= "DELETE FROM cluster_group WHERE clusterId = '%s'";
	
	private static String INSERT_CLUSTER_GROUP 		= new StringBuilder(
		"INSERT INTO cluster_group(clusterid, groupid) 	\n").append(
		"SELECT '%s', groupId							\n").append(
		"FROM Group_									\n").append(
		"  WHERE groupId IN (%s) AND typeSettings = '%d'\n").toString();
	
	private static String INSERT_CLUSTER_GROUP_BY_GLOBAL = new StringBuilder(
		"INSERT INTO cluster_group(clusterid, groupid)  												\n").append(
		"SELECT '%s', Group_.groupId																	\n").append(
		"FROM Group_																					\n").append(
		"INNER JOIN XMLIO_Live ON (      XMLIO_Live.localId = Group_.groupId							\n").append(
		"                            AND XMLIO_Live.classNameValue = 'com.liferay.portal.model.Group')	\n").append(
		"  WHERE XMLIO_Live.globalId IN (%s)															\n").append(
		"    AND Group_.typeSettings = '%d'																\n").toString();
		
	private static String SELECT_PUBLISHABLE_INFO 	= new StringBuilder(
		"SELECT cluster.clusterid, clustername, 														\n").append(	
		"		CONCAT(\"'\", GROUP_CONCAT(XMLIO_Live.globalId SEPARATOR \"','\"), \"'\") siteids		\n").append(		
		"FROM cluster																					\n").append(																
		"INNER JOIN cluster_group USING (clusterid)														\n").append(									
		"INNER JOIN XMLIO_Live ON (      XMLIO_Live.localId = cluster_group.groupId						\n").append(
		"                            AND XMLIO_Live.classNameValue = 'com.liferay.portal.model.Group')	\n").append(
		"  WHERE delegationId = %s																		\n").append(
		"GROUP BY cluster.clusterId																		\n").toString();
	
	private final String ITER_CLUSTER_ZIP_FILE_NAME = IterKeys.XMLIO_ZIP_FILE_PREFIX + "cluster_%s.zip";
	
	private final String GET_APPLYTO = new StringBuilder(
		"SELECT cluster.clusterid id, cluster.clustername name, if( cluster_applyto.clusterid is null, 0, 1) selected	\n").append(
		"FROM cluster																									\n").append(
		"INNER JOIN cluster_group  ON cluster.clusterid = cluster_group.clusterid										\n").append(
		"LEFT JOIN cluster_applyto ON (     cluster.clusterid = cluster_applyto.clusterid								\n").append( 
		"                               AND cluster_group.groupid = cluster_applyto.groupid								\n").append(
		"                               AND cluster_applyto.idope = %d)													\n").append(
		"	WHERE cluster_group.groupId = %d																			\n").append(
		"	ORDER BY cluster.clustername ASC																			\n").toString();
			
	private final String DEL_APPLYTO 	= "DELETE FROM cluster_applyto WHERE idope = %d AND groupId = %d";
	private final String INSERT_APPLYTO = new StringBuilder(
		"INSERT INTO cluster_applyto (idope, groupid, clusterid)	\n").append(
		"	VALUES (%d, %d, '%s')									\n").toString();

	//Sólo una publicación al mismo tiempo
	private static Lock publishLock = new ReentrantLock();

	public Document getClusters(String delegationId) throws SecurityException, NoSuchMethodException
	{
		String sql = String.format(GET_CLUSTERS, delegationId);
		
		if (_log.isDebugEnabled())
			_log.debug( "getClusters: ".concat(sql) );
		
		return PortalLocalServiceUtil.executeQueryAsDom( sql, new String[]{"sites"} );
	}
	
	public void addCluster(String data) throws DocumentException, ServiceError, IOException, SQLException
	{
		if (_log.isTraceEnabled())
			_log.trace("addCluster: ".concat(GetterUtil.getString(data, "NULL")));

		Element root = SAXReaderUtil.read(data).getRootElement();
		String clusterId = PortalUUIDUtil.newUUID();
		
		String clusterName = XMLHelper.getStringValueOf(root, "@clustername");
		ErrorRaiser.throwIfNull(clusterId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String siteids = XMLHelper.getStringValueOf(root, "siteids");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(siteids), IterErrorKeys.XYZ_E_ITER_CLUSTER_EMPTY_ZYX);
		
		long delegationId = XMLHelper.getLongValueOf(root,  "@delegationId");
		
		// Se crea el cluster
		PortalLocalServiceUtil.executeUpdateQuery( String.format(INSERT_CLUSTER, clusterId, clusterName, delegationId) );
		
		// Se insertan las nuevas asignaciones
		PortalLocalServiceUtil.executeUpdateQuery( String.format(INSERT_CLUSTER_GROUP, clusterId, siteids, delegationId) );
	}
	
	public void updateCluster(String data) throws DocumentException, ServiceError, IOException, SQLException
	{
		if (_log.isTraceEnabled())
			_log.trace("updateCluster: ".concat(GetterUtil.getString(data, "NULL")));
		
		Element root = SAXReaderUtil.read(data).getRootElement();
		String clusterId = XMLHelper.getStringValueOf(root, "@clusterid");
		ErrorRaiser.throwIfNull(clusterId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String clusterName = XMLHelper.getStringValueOf(root, "@clustername");
		ErrorRaiser.throwIfNull(clusterId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String siteids = XMLHelper.getStringValueOf(root, "siteids");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(siteids), IterErrorKeys.XYZ_E_ITER_CLUSTER_EMPTY_ZYX);
		
		long delegationId = XMLHelper.getLongValueOf(root,  "@delegationId");
		
		// Se actualiza el nombre
		PortalLocalServiceUtil.executeUpdateQuery( String.format(UPDT_CLUSTER, clusterName, clusterId) );
		
		// Se borran las asignaciones anteriores
		PortalLocalServiceUtil.executeUpdateQuery( String.format(DEL_CLUSTER_GROUP, clusterId) );
		
		// Se insertan las nuevas asignaciones
		PortalLocalServiceUtil.executeUpdateQuery( String.format(INSERT_CLUSTER_GROUP, clusterId, siteids, delegationId) );
	}
	
	public void deleteClusters(String data) throws DocumentException, IOException, SQLException
	{
		if (_log.isTraceEnabled())
			_log.trace("deleteClusters: ".concat(GetterUtil.getString(data, "NULL")));

		Document dom = SAXReaderUtil.read(data);
		String sql   = String.format(DEL_CLUSTERS, StringUtils.join( XMLHelper.getStringValues(dom.selectNodes("/rs/row/@clusterid")), "','"));
		
		PortalLocalServiceUtil.executeUpdateQuery(sql);
	}
	
	public void publishToLive(String delegationId) throws IOException, UnsupportedEncodingException, ServiceError, PortalException, SystemException, SecurityException, NoSuchMethodException, IOException, DocumentException
	{
		ErrorRaiser.throwIfFalse(PropsValues.IS_PREVIEW_ENVIRONMENT, IterErrorKeys.XYZ_E_UNEXPECTED_ENVIRONMENT_ZYX);
		
		ErrorRaiser.throwIfFalse(publishLock.tryLock(), IterErrorKeys.XYZ_E_PUBLISH_ALREADY_IN_PROCESS_ZYX);

		File localFile = null;
		
		try
		{
			//Se recupera la configuración de la publicación
			LiveConfiguration liveConf 	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId( GroupMgr.getCompanyId() );
			
			//Generamos el .xml de exportación
			Element rs = PortalLocalServiceUtil.executeQueryAsDom( String.format(SELECT_PUBLISHABLE_INFO, delegationId), new String[]{"siteids"} ).getRootElement();
			rs.addAttribute("delegationId", delegationId);
			
			//Comprobamos que no es una publicación vacía
			PublishUtil.checkEmptyPublication(rs);
			
			//Generamos el .zip a exportar
			localFile = generateExportFile(liveConf.getLocalPath(), rs);
			
			//Enviamos por FTP/File System el .zip generado
			String importFileName = PublishUtil.sendFile(liveConf, localFile);
			
			//Realizar la llamada al Live para que importe el .zip
			ErrorRaiser.throwIfFalse(callLiveImportContents(liveConf, importFileName), IterErrorKeys.XYZ_E_XPORTCONTENT_ALL_FAILED_ZYX);
		}
		finally
		{
			publishLock.unlock();
			
			// Borramos el fichero de exportación
			PublishUtil.hotConfigDeleteFile(localFile);	
		}
	}
	
	private File generateExportFile(String localPath, Element rs) throws SecurityException, NoSuchMethodException, IOException, DocumentException, PortalException, SystemException
	{
		File exportFile = null;
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
		{
			String zipFileName = String.format(ITER_CLUSTER_ZIP_FILE_NAME, Calendar.getInstance().getTimeInMillis());
			ZipWriter zipWriter = ZipWriterFactoryUtil.getZipWriter(new File(localPath + File.separatorChar + zipFileName));
			
			//Se añade el .xml de publicacion al .zip
			zipWriter.addEntry(IterKeys.XMLIO_XML_MAIN_FILE_NAME, rs.asXML());

			//Se obtiene el fichero liberado
			exportFile = PublishUtil.getUnlockedFile(zipWriter);
		}
		
		return exportFile;
	}

	private static boolean callLiveImportContents(LiveConfiguration liveConf, String importFileName) throws UnsupportedEncodingException, ClientProtocolException, IOException, SystemException, PortalException, DocumentException, ServiceError
	{
		boolean success = false;
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW)) 
		{
			List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
			remoteMethodParams.add(new BasicNameValuePair("serviceClassName",  	"com.protecmedia.iter.base.service.ClusterServiceUtil"));
			remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"importContents"));
			remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[importFileName]"));
			remoteMethodParams.add(new BasicNameValuePair("importFileName",		importFileName));
			
			String []url = liveConf.getRemoteIterServer2().split(":");
			HttpHost targetHost = new HttpHost(url[0], Integer.valueOf(url[1]));

			JSONObject json = JSONUtil.executeMethod(targetHost, "/xmlio-portlet/secure/json", remoteMethodParams, 
					(int)liveConf.getConnectionTimeOut(),
					(int)liveConf.getOperationTimeOut(),
					liveConf.getRemoteUserName(), liveConf.getRemoteUserPassword());

			success = Boolean.parseBoolean(json.getString("returnValue"));
		}
		return success;
	}

	public boolean importContents(String importFileName) throws Exception
	{
		boolean success = false;
		
		File importFile = null;
		File temporaryDir = null;
		
		ErrorRaiser.throwIfFalse(!PropsValues.IS_PREVIEW_ENVIRONMENT, IterErrorKeys.XYZ_E_UNEXPECTED_ENVIRONMENT_ZYX);
		ErrorRaiser.throwIfFalse(publishLock.tryLock(), IterErrorKeys.XYZ_E_PUBLISH_ALREADY_IN_PROCESS_ZYX);

		try
		{
			Group scopeGroup = GroupLocalServiceUtil.getGroup(GroupMgr.getGlobalGroupId());
			LiveConfiguration liveConf 	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(scopeGroup.getCompanyId());

			String importFilePath = null;
			
			if (liveConf.getOutputMethod().equals(IterKeys.XMLIO_CHANNEL_MODE_FTP))
			{
				String ftpServer = liveConf.getFtpPath();
				String ftpUser = liveConf.getFtpUser();
				String ftpPassword = liveConf.getFtpPassword();
				String localPath = liveConf.getLocalPath();

				importFilePath = FTPUtil.receiveFile(ftpServer, ftpUser, ftpPassword, importFileName, localPath, StringPool.BLANK);
			} 
			else
			{
				String remotePath = liveConf.getRemotePath();
				importFilePath = remotePath + File.separatorChar + importFileName;	
			}
			
			String zipExtension = ".zip";
			
			String temporaryDirPath = importFilePath.replace(zipExtension, StringPool.BLANK);
			
			importFile = new File(importFilePath);
			temporaryDir = new File(temporaryDirPath);
			
			ZipUtil.unzip(importFile, temporaryDir);
			
			File iterXmlFile = new File(temporaryDirPath + File.separatorChar + IterKeys.XMLIO_XML_MAIN_FILE_NAME);
			Element rs = SAXReaderUtil.read(iterXmlFile).getRootElement();

			long delegationId = XMLHelper.getLongValueOf(rs, "@delegationId");

			// Se elimina el contenido anterior
			PortalLocalServiceUtil.executeUpdateQuery( String.format(DEL_CLUSTERS_DELEGATION, delegationId) );
			
			// Inserta el nuevo
			List<Node> clusters = rs.selectNodes("row");
			for (Node cluster : clusters)
			{
				String clusterId 	= XMLHelper.getStringValueOf(cluster, "@clusterid");
				String clustername 	= XMLHelper.getStringValueOf(cluster, "@clustername");
				
				// Crea el cluster
				PortalLocalServiceUtil.executeUpdateQuery( String.format(INSERT_CLUSTER, clusterId, clustername, delegationId) );
				
				String siteids = XMLHelper.getStringValueOf(cluster, "siteids");
				ErrorRaiser.throwIfFalse( Validator.isNotNull(siteids), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
						
				// Se insertan las nuevas asignaciones
				PortalLocalServiceUtil.executeUpdateQuery( String.format(INSERT_CLUSTER_GROUP_BY_GLOBAL, clusterId, siteids, delegationId) );
			}
			
			success = true;
			
			// Solo en caso satisfactorio se borran los ficheros. Ante fallos persisten para identificar y localizar dichos fallos.
			PublishUtil.hotConfigDeleteFile(temporaryDir);
			PublishUtil.hotConfigDeleteFile(importFile);
		}
		finally
		{
			// Solo se desbloquea si previamente se ha adquirido el bloqueo
			publishLock.unlock();
		}
		
		return success;
	}
	
	public Document getApplyTo(String idope, String groupid) throws SecurityException, NoSuchMethodException, ServiceError, PortalException, SystemException
	{
		long idOpe 		= Long.parseLong(idope);
		long groupId 	= Long.parseLong(groupid);
		
		ErrorRaiser.throwIfFalse( groupId > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX, String.format("Group: %d", groupId) );
		
		Element root = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_APPLYTO, idOpe, groupId) ).getRootElement();
		
		// A los datos anteriores se les añade el grupo
		Group group = GroupLocalServiceUtil.getGroup(groupId);
		
		// Si no hay ningún cluster seleccionado se selecciona el grupo
		int selected = XMLHelper.getLongValueOf(root, "count(//row[@selected='1'])") == 0 ? 1 : 0; 
		
		Element elemGroup = root.addElement("row");
		elemGroup.addAttribute("name", 		group.getName());
		elemGroup.addAttribute("id",   		String.valueOf(group.getGroupId()));
		elemGroup.addAttribute("selected",  String.valueOf(selected));
		
		return root.getDocument();
	}
	
	public void setApplyTo(String idope, String groupid, String id) throws IOException, SQLException, ServiceError
	{
		long idOpe 		= Long.parseLong(idope);
		long groupId 	= Long.parseLong(groupid);
		
		ErrorRaiser.throwIfFalse( groupId > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX, String.format("Group: %d", groupId) );

		// Se borra la selección anterior
		PortalLocalServiceUtil.executeUpdateQuery( String.format(DEL_APPLYTO, idOpe, groupId) );
		
		// Si el id es el grupo no es necesario insertar selección alguna
		if (!String.valueOf(groupId).equals(id))
		{
			// Se inserta el ID del cluster seleccionado
			PortalLocalServiceUtil.executeUpdateQuery( String.format(INSERT_APPLYTO, idOpe, groupId, id) );
		}
	}
}