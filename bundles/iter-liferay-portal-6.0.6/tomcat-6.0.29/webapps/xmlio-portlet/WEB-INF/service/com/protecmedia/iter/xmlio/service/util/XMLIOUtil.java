/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.service.util;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.documentlibrary.DuplicateFileException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.model.Image;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.ImageLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.documentlibrary.NoSuchFileEntryException;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFolderLocalServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;


public class XMLIOUtil {
	
	private static Log _log = LogFactoryUtil.getLog(XMLIOUtil.class);
	
	private static String XMLIO_REFLECTION_LIFERAY_ITEM_PACKAGE = "com.protecmedia.iter.xmlio";
	private static String XMLIO_REFLECTION_GENERAL_ITEM_PACKAGE = "service.item";
	private static String XMLIO_REFLECTION_GENERAL_ITEM_SUFFIX = "XmlIO";
	
	public void generateLiveDocument() {
		//Crea un mapa que dice como generar el xml
	}
	
	public void generateDocument() {
		//genera el xml que será necesario para el live
	}

	public static ItemXmlIO getItemByType(String type) throws Exception
	{
		return getItemByType(type, null);
	}
	
	/**
	 * @param type
	 * @param xmlIOContext
	 * @return
	 */
	public static ItemXmlIO getItemByType(String type, XMLIOContext xmlIOContext) throws Exception
	{
		//TODO: Usar expresiones regulares con 3 máscaras
		//Control de impl (Borrar para convertir a com.liferay.portlet.asset.model.AssetVocabulary)
		if (type.contains("impl"))
		type = type.replace(".impl", "").replace("Impl", "");
		
		//Recupera la ruta y la clase
		String baseClassName = type.substring(type.lastIndexOf(".") + 1, type.length());
		String baseClassPackage = type.substring(0, type.indexOf(".model"));
		//Calcula la clase hija de XmlItem adecuada
		StringBuffer sb = new StringBuffer();
		if (baseClassPackage.contains("com.liferay.portal")){
			sb.append(XMLIO_REFLECTION_LIFERAY_ITEM_PACKAGE);
			sb.append("." + XMLIO_REFLECTION_GENERAL_ITEM_PACKAGE);
			sb.append(baseClassPackage.replace("com.liferay", ""));
		} 
		else if (type.contains("com.liferay.portlet")){
			sb.append(XMLIO_REFLECTION_LIFERAY_ITEM_PACKAGE);
			sb.append("." + XMLIO_REFLECTION_GENERAL_ITEM_PACKAGE);
			sb.append(baseClassPackage.replace("com.liferay.portlet", ""));
		}
		else{
			sb.append(baseClassPackage);
			sb.append("." + XMLIO_REFLECTION_GENERAL_ITEM_PACKAGE);
		}
		sb.append("." + baseClassName);
		sb.append(XMLIO_REFLECTION_GENERAL_ITEM_SUFFIX);
	
		//TODO: Ver cómo funciona el ClassLoader de Liferay para resolver este punto ClassLoaderProxy 
		//TODO: Asegurar que la clase se carga desde un unico ClassLoader
		//Get ItemClass Instance
		ItemXmlIO itemXmlIO = null;
		try
		{
			Class<?> itemXmlIOCl = Class.forName(sb.toString());
			itemXmlIO = (ItemXmlIO) itemXmlIOCl.newInstance();
			itemXmlIO.setXMLIOContext(xmlIOContext);
		}
		catch(Exception e)
		{
			_log.error("Casting failed or class not found for " + sb.toString() +": "+ e.toString());
			throw e;
		}
		return itemXmlIO;
	}	
	
	/**
	 * 
	 * @param xmlioExport
	 * @param imageId
	 * @return
	 */
	public static String exportImageFile (XMLIOExport xmlioExport, long imageId){
		String fileUrl = "";
		
		if(imageId != 0){
			Image img = null;
					
			try {
				img = ImageLocalServiceUtil.getImage(imageId);
			} catch (Exception e) {
			}			
				
			if (img != null){
				fileUrl = "/images/img_" + imageId + "." + img.getType();
						
				xmlioExport.addResource(fileUrl, img.getTextObj());		
			}
			else{
				_log.debug("Cannot retrieve binary data for image " + imageId);
			}			
		}
		
		return fileUrl;
	}
	
	/**
	 * 
	 * @param imageId
	 * @param fileUrl
	 * @return
	 */
	public static long importImageFile (String fileUrl){
		long imageId = -1;
		
		if (!fileUrl.isEmpty()){
			try {		
				
				byte[] bytes = XMLIOImport.getFileAsBytes(fileUrl);
				
				if (bytes != null) {	
					imageId = CounterLocalServiceUtil.increment();	
					
					ImageLocalServiceUtil.updateImage(imageId, bytes);	
					//_log.error("XMLIO Utils Image created");
				}			
				
			}catch(Exception e){
				_log.error("XMLIO Utils Error adding image");
			}
		}
		
		return imageId;
	}
	
	
	/**
	 * 
	 * @param folderId
	 * @return
	 */
	public static String buildFolderUrl(long folderId){		
				
		String folderUrl = "";
		
		try {
			
			if (folderId != 0){
			
			DLFolder dlFolder = DLFolderLocalServiceUtil.getDLFolder(folderId);
			
			folderUrl = buildFolderUrl(dlFolder.getParentFolderId()) + "/" + dlFolder.getName();
			
			}		
		} catch (PortalException e) {} 
		catch (SystemException e) {}
		
		return folderUrl;		
	}
	
	/**
	 * 
	 * @param folderId
	 * @return
	 */
	public static String buildFolderLocalUrl(long folderId){		
				
		String folderUrl = "";
		
		try {
			
			if (folderId != 0){
			
			DLFolder dlFolder = DLFolderLocalServiceUtil.getDLFolder(folderId);
			
			folderUrl = buildFolderLocalUrl(dlFolder.getParentFolderId()) + "/" + folderId;
			
			}		
		} catch (PortalException e) {} 
		catch (SystemException e) {}
		
		return folderUrl;		
	}
	
	/**
	 * 
	 * @param folderId
	 * @return
	 */
	public static String buildFolderGlobalIdsUrl(long folderId){		
				
		String folderUrl = "";
		
		try {
			
			if (folderId != 0){
			
			DLFolder dlFolder = DLFolderLocalServiceUtil.getDLFolder(folderId);			
			Live live = LiveLocalServiceUtil.getLiveByLocalId(dlFolder.getGroupId(), IterKeys.CLASSNAME_DLFOLDER, String.valueOf(folderId));
			
			folderUrl = buildFolderUrl(dlFolder.getParentFolderId()) + "/" + live.getGlobalId();
			
			}		
		} catch (Exception e) {
			//Si falla, lo pone en la raiz
			folderUrl = "";
		}
		
		return folderUrl;		
	}
	
	/**
	 * 
	 * @param groupId
	 * @param folder
	 * @param folderId
	 * @return
	 */
	public static long searchFolder(long groupId, String folder, long folderId) {

		try {
			List<DLFolder> dlFolders;

			dlFolders = DLFolderLocalServiceUtil.getFolders(groupId, folderId);

			for (DLFolder dlFolder : dlFolders) {
				if (folder.equals(dlFolder.getName())) {
					folderId = dlFolder.getFolderId();
					break;
				}
			}

		} catch (SystemException e) {
			_log.error("XMLIO Utils Error getting document folder");
		}

		return folderId;
	}
	
	
	/**
	 * 
	 * @param xmlioExport
	 * @param type
	 * @param source
	 * @param file
	 * @param params
	 * @return
	 */
	public static String exportSourceFiles (XMLIOExport xmlioExport, String type, String source, String file, Map<String, String> params){				
		String fileUrl = "";
		
		if(type.equals(ItemConstants.FTYPE_IMAGE) && source.equals(ItemConstants.FSOURCE_FILE)){
			
			String[] urlArray = file.split("img_id=");
			long imageId = GetterUtil.getLong(urlArray[1]);			
						
			fileUrl = exportImageFile(xmlioExport, imageId);
			
		}else if(type.equals(ItemConstants.FTYPE_IMAGE) && source.equals(ItemConstants.FSOURCE_LIBRARY)){
			fileUrl = transformLocalPathToGlobal(file);
		}else if (type.equals(ItemConstants.FTYPE_IMAGE) && source.equals(ItemConstants.FSOURCE_URL)){
			fileUrl = file;			
		}else if (type.equals(ItemConstants.FTYPE_HTML)){
			//Nothing to do
		}else if (type.equals(ItemConstants.FTYPE_FLASH) && source.equals(ItemConstants.FSOURCE_URL)){			
			fileUrl = file;
		}else if (type.equals(ItemConstants.FTYPE_FLASH) && (source.equals(ItemConstants.FSOURCE_FILE) || source.equals(ItemConstants.FSOURCE_LIBRARY))){		
			
			String[] urlArray = file.split("/");
			
			long groupId = GetterUtil.getLong(urlArray[2]);
			long folderId = GetterUtil.getLong(urlArray[3]);
			String title = urlArray[4];
			
			params.put("folder", XMLIOUtil.buildFolderUrl(folderId));	
			params.put("title", title);	
			
			try {
				long delegationId = GroupLocalServiceUtil.getGroup(groupId).getDelegationId();
				DLFileEntry fe = DLFileEntryLocalServiceUtil.getFileEntryByTitle(groupId, folderId, title);
				InputStream is = DLFileEntryLocalServiceUtil.getFileAsStream(delegationId, fe.getUserId(), fe.getGroupId(), fe.getFolderId(), fe.getName());
							
				try {	
					
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();

					int nRead;
					byte[] data = new byte[16384];

					while ((nRead = is.read(data, 0, data.length)) != -1) {
					  buffer.write(data, 0, nRead);
					}

					fileUrl = "/flash/" + title;
					
					xmlioExport.addResource(fileUrl, buffer.toByteArray());	

				} catch (IOException e) {
					_log.error("XMLIO Utils Error reading flash byte");
				}	
			
			} catch (PortalException e) {
				_log.error("XMLIO Utils Can't get flash");
			} catch (SystemException e) {
				_log.error("XMLIO Utils Can't get flash");
			}
		}
		
		return fileUrl;
	}
	
	
	/**
	 * 
	 * @param type
	 * @param fileUrl
	 * @param itemId
	 * @return
	 */
	public static String importSourceFiles (long groupId, long userId, String type, String source, String fileUrl, String itemId, String folder, String title){
		String file = "";
		
		if(type.equals(ItemConstants.FTYPE_IMAGE) && source.equals(ItemConstants.FSOURCE_FILE)){
			long imageId = importImageFile(fileUrl);	
			
			if (imageId != -1){	
				file = "/image/journal/article?img_id=" + imageId;
			}
		}else if(type.equals(ItemConstants.FTYPE_IMAGE) && source.equals(ItemConstants.FSOURCE_URL)){
			file = fileUrl;
		}else if(type.equals(ItemConstants.FTYPE_IMAGE) && source.equals(ItemConstants.FSOURCE_LIBRARY)){
			file = transformGlobalPathToLocal(groupId, fileUrl);
		}else if (type.equals(ItemConstants.FTYPE_HTML)){
			//Nothing to do
		}else if (type.equals(ItemConstants.FTYPE_FLASH) && source.equals(ItemConstants.FSOURCE_URL)){
			file = fileUrl;
		}else if (type.equals(ItemConstants.FTYPE_FLASH) && (source.equals(ItemConstants.FSOURCE_FILE) || source.equals(ItemConstants.FSOURCE_LIBRARY))){
			try {
			
				byte[] bytes = XMLIOImport.getFileAsBytes(fileUrl);
				
				if (bytes != null) {
					
					long folderId = 0; 
					if (folder != null){
						//Search for the folder starting in the root
						String[] folderArray = folder.split("/");					
						for (int i=1; i<folderArray.length ; i++){
							folderId = XMLIOUtil.searchFolder(groupId, folderArray[i], folderId);
						}
					}			
					
					ServiceContext sc = new ServiceContext();
					
					long gId = groupId;
					try{
						DLFileEntry dLFileEntry = DLFileEntryLocalServiceUtil.addFileEntry(userId, groupId, folderId, "", title, title, "", "", bytes, sc);
						gId = dLFileEntry.getGroupId();
					}catch(DuplicateFileException dfe){
						
					}					
					
					file = "/documents/" + gId + "/" + folderId + "/" + title;
					
					//_log.error("XMLIO Utils Flash created");
				}		
				
			}catch(Exception e){
				_log.error("XMLIO Utils Error adding created");
			}
			
		}
		
		return file;
	}
	
	private static String transformLocalPathToGlobal(String path){
		
		String globalFileUrl = "";
		try{
			String[] str = path.split("/");
			if(str!=null && str.length>1){
				String localGrpId = str[2];
				String grpName = GroupLocalServiceUtil.getGroup( GetterUtil.getLong(localGrpId) ).getName();
				String fileName = str[str.length-1];
				String localFolderId = str[str.length-2];
				Live live = LiveLocalServiceUtil.getLiveByLocalId(GetterUtil.getLong(localGrpId), IterKeys.CLASSNAME_DLFOLDER, localFolderId);
				String globalFolderId = "0";
				if(live!=null)
					globalFolderId = live.getGlobalId();
				
				try{
					DLFileEntry dlFile = DLFileEntryLocalServiceUtil.getDLFileEntryByUuidAndGroupId(fileName, GetterUtil.getLong(localGrpId));
					if(dlFile!=null){
						fileName =  dlFile.getTitle();
					}
				}catch(NoSuchFileEntryException e){}
	
				globalFileUrl = "/" + str[1] + "/" + grpName + "/" + globalFolderId + "/" + fileName;
			}
		}catch(Exception e){
			_log.error("Cannot convert local path " + path + " to global");
		}
		
		return globalFileUrl;
	}
	
	private static String transformGlobalPathToLocal(long groupId, String path){
		
		String localFileUrl ="";
		try{
			String[] str = path.split("/");
			if(str!=null && str.length>1){
				//String grpName = str[2];
				//long localGrpId = GroupLocalServiceUtil.getGroup(xmlIOContext.getCompanyId(), grpName).getGroupId();
				String fileName = str[str.length-1];
				String globalFolderId = str[str.length-2];					
				
				String folderPath = "/0";
				if(!globalFolderId.equals("0")){
					Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_DLFOLDER, globalFolderId);
					folderPath = XMLIOUtil.buildFolderLocalUrl(GetterUtil.getLong(live.getLocalId()));
				}
				
				localFileUrl = "/" + str[1] + "/" + groupId + folderPath + "/" + fileName;
			}
		}catch(Exception e){
			_log.error("Cannot convert global path " + path + " to local");
		}
		
		return localFileUrl;
				
	}

	
	/**
	 * Ejecuta la importacion de un contenido ITER en un sistema remoto via JSON WS
	 * @param companyId
	 * @param groupId
	 * @param remoteIterServer
	 * @param remotePath
	 * @param remoteFileName
	 * @param remoteUserId
	 * @param remoteUserName
	 * @param remoteUserPassword
	 * @return
	 * @throws Exception
	 */
	public static String executeJSONRemoteImportContent(
			long companyId,
			long remoteCompanyId,
			long remoteGroupId,
			String remoteIterServer, 
			String remotePath, 
			String remoteFileName, 
			long remoteUserId,
			String remoteUserName, 
			String remoteUserPassword) 
			throws UnsupportedEncodingException, ClientProtocolException, IOException, NumberFormatException{
		
		String remoteIP = remoteIterServer.split(":")[0];
		int remotePort = Integer.valueOf(remoteIterServer.split(":")[1]);
		String remoteMethodPath = "/xmlio-portlet/secure/json";
		//String remoteFilePath = remotePath + "/" + remoteFileName;
		List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
		
		remoteMethodParams.add(new BasicNameValuePair("serviceClassName", "com.protecmedia.iter.xmlio.service.ChannelServiceUtil"));
		remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", "importToLive"));
		remoteMethodParams.add(new BasicNameValuePair("serviceParameters", "[companyId,userId,groupId,fileName]"));
		remoteMethodParams.add(new BasicNameValuePair("companyId", String.valueOf(remoteCompanyId)));
		remoteMethodParams.add(new BasicNameValuePair("userId", String.valueOf(remoteUserId)));
		remoteMethodParams.add(new BasicNameValuePair("groupId", String.valueOf(remoteGroupId)));
		remoteMethodParams.add(new BasicNameValuePair("fileName", remoteFileName));
		
		return executeJSONRemoteMethod(companyId, remoteIP, remotePort, remoteUserName, remoteUserPassword, remoteMethodPath, remoteMethodParams);
		
		//TODO: verificar que el log contiene o comienza como debe 
	}
	
	public static void deleteRemoteCache(long scopeGroupId) throws UnsupportedEncodingException, ClientProtocolException, IOException, PortalException, SystemException, DocumentException, NumberFormatException, com.liferay.portal.kernel.error.ServiceError
	{
		deleteRemoteCache(scopeGroupId, StringPool.BLANK);
	}
	
	public static void deleteRemoteCache(long scopeGroupId, String lastUpdate) throws UnsupportedEncodingException, ClientProtocolException, IOException, PortalException, SystemException, DocumentException, NumberFormatException, com.liferay.portal.kernel.error.ServiceError
	{
		deleteRemoteCache( new CacheRefresh(scopeGroupId, lastUpdate) );
	}

	public static void deleteRemoteCache(CacheRefresh cacheRefresh) throws UnsupportedEncodingException, ClientProtocolException, IOException, JSONException, NumberFormatException, com.liferay.portal.kernel.error.ServiceError
	{
		LiveConfiguration liveConf = LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(IterGlobal.getCompanyId());
		String remoteIP = liveConf.getRemoteIterServer2().split(":")[0];
		int remotePort = Integer.valueOf(liveConf.getRemoteIterServer2().split(":")[1]);

		String remoteMethodPath = "/xmlio-portlet/secure/json";
		
		List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
		remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 			"com.protecmedia.iter.xmlio.service.ChannelServiceUtil"));
		remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 			"refreshCache"));
		remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 			"[cacheRefresh]"));
		remoteMethodParams.add(new BasicNameValuePair("cacheRefresh", 				cacheRefresh.getCacheRefresh().asXML()));

		String result = XMLIOUtil.executeJSONRemoteMethod2(IterGlobal.getCompanyId(), remoteIP, remotePort, liveConf.getRemoteUserName(),
														   liveConf.getRemoteUserPassword(), remoteMethodPath,remoteMethodParams);
		JSONObject json = JSONFactoryUtil.createJSONObject(result);
		
		String errorMsg = json.getString("exception");
		if (!errorMsg.isEmpty()) 
		{
			_log.error(errorMsg);
		}
	}

	/**
	 * Ejecuta un metodo en un servidor remoto con los datos especificados via JSON WS
	 * @param IP 
	 * @param Port
	 * @param user
	 * @param password
	 * @param methodPath Ej. /tunnel-web/secure/json
	 * @param params 
	 *  Example:
	 *  params.add(new BasicNameValuePair("serviceClassName", "com.liferay.portal.service.CountryServiceUtil"));
     *  params.add(new BasicNameValuePair("serviceMethodName", "getCountries"));
	 *
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static String executeJSONRemoteMethod(
			long companyId,
			String ip, 
			int port, 
			String user,
			String password,
			String methodPath,
			List<NameValuePair> params) throws UnsupportedEncodingException, ClientProtocolException, IOException 
	{
		String strValue = executeJSONRemoteMethod2(companyId, ip, port, user, password, methodPath, params);
		strValue = strValue.replace("{\"returnValue\":\"", "").replace("\"}", "");
		
        try
        {
        	strValue = URLDecoder.decode(strValue, "UTF-8");
        }
		catch (UnsupportedEncodingException uee)
		{
			_log.error("Error while trying to decode remote response", uee);
		}

        return strValue;
	}
	
	public static String executeJSONRemoteMethod2(
			long companyId,
			String ip, 
			int port, 
			String user,
			String password,
			String methodPath,
			List<NameValuePair> params) throws UnsupportedEncodingException, ClientProtocolException, IOException 
	{
        HttpHost targetHost = new HttpHost(ip, port, "http");
        
        LiveConfiguration lc = LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(companyId);
        
        //TIMEOUT:
        HttpParams httpParameters = new BasicHttpParams();
		// Set the timeout in milliseconds until a connection is established.
		HttpConnectionParams.setConnectionTimeout(httpParameters, (int)lc.getConnectionTimeOut());
		// Set the default socket timeout (SO_TIMEOUT) 
		// in milliseconds which is the timeout for waiting for data. 
		HttpConnectionParams.setSoTimeout(httpParameters, (int)lc.getOperationTimeOut());

		//Pedro
		//HttpProtocolParams.setContentCharset(httpParameters, "UTF-8");     
		
        DefaultHttpClient httpclient = new DefaultHttpClient(httpParameters);        
        httpclient.getCredentialsProvider().setCredentials(
                new AuthScope(targetHost.getHostName(), targetHost.getPort()),
                new UsernamePasswordCredentials(user, password));

        
        // Create AuthCache instance
        AuthCache authCache = new BasicAuthCache();
        // Generate BASIC scheme object and add it to the local
        // auth cache
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(targetHost, basicAuth);

        // Add AuthCache to the execution context
        BasicHttpContext ctx = new BasicHttpContext();
        ctx.setAttribute(ClientContext.AUTH_CACHE, authCache);
        HttpPost post = new HttpPost(methodPath);
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
        //Pedro
        entity.setContentEncoding("UTF-8");
        post.setEntity(entity);
        
        String callLog = "";
        if (_log.isDebugEnabled())
        {
        	StringBuilder paramsLog = new StringBuilder("\n");
        	for (NameValuePair pair : params)
        		paramsLog.append( String.format("%s:%s\n", pair.getName(), pair.getValue()) );
        	
        	callLog = String.format("%s:%d %s %s", ip, port, methodPath, paramsLog);
        		
        	_log.debug( String.format("Before execute %s", callLog) );
        }
        HttpResponse resp = httpclient.execute(targetHost, post, ctx);
        if (_log.isDebugEnabled())
        	_log.debug( String.format("After execute %s", callLog) );
        
        HttpEntity e = resp.getEntity();
        String strValue = EntityUtils.toString(e, "UTF-8");
        
        httpclient.getConnectionManager().shutdown();  
        return strValue;
	}

	public static Object executeSOAPRemoteMethod(String remoteMethodPath, Map<String, Object> remoteMethodParams){
		return null;
	}
	
	
	public static String executeJSONRemoteGetSystemInfo(
			long companyId,
			String remoteIterServer,			
			long remoteUserId,
			String remoteUserName, 
			String remoteUserPassword) 
			throws UnsupportedEncodingException, ClientProtocolException, IOException, NumberFormatException{
		
		String remoteIP = remoteIterServer.split(":")[0];
		int remotePort = Integer.valueOf(remoteIterServer.split(":")[1]);
		String remoteMethodPath = "/xmlio-portlet/secure/json";
		//String remoteFilePath = remotePath + "/" + remoteFileName;
		List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
		
		remoteMethodParams.add(new BasicNameValuePair("serviceClassName", "com.protecmedia.iter.base.service.IterServiceUtil"));
		remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", "getSystemInfoEncoded"));
		remoteMethodParams.add(new BasicNameValuePair("serviceParameters", "[userId]"));
		remoteMethodParams.add(new BasicNameValuePair("userId", String.valueOf(remoteUserId)));
	
		return executeJSONRemoteMethod(companyId, remoteIP, remotePort, remoteUserName, remoteUserPassword, remoteMethodPath, remoteMethodParams);
		
		//TODO: verificar que el log contiene o comienza como debe 
	}
	
	/**
	 * Copia un fichero en otro
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
	public static void copyFile(File sourceFile, File destFile) throws IOException, ServiceError 
	{
	    if (!destFile.exists()) 
	    {
	    	ErrorRaiser.throwIfFalse( destFile.createNewFile(), IterErrorKeys.XYZ_E_CREATE_NEW_DSTFILE_FAILED_ZYX, destFile.getAbsolutePath());
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try 
	    {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally
	    {
	        if(source != null) 
	        {
	            source.close();
	        }
	        if(destination != null) 
	        {
	            destination.close();
	        }
	    }
	}
	
	public static File getFileByExt(File fileDir, String extension)
	{
		File result 	= null;
		File []fileList = fileDir.listFiles();
		
		for (File file : fileList)
		{
			if (file.getName().toLowerCase().endsWith(extension))
			{
				result = file;
				break;
			}
		}
		return result;
	}
	
	public static String transformXsl(String source, String xslPath) throws TransformerException, FileNotFoundException{

	    StringReader reader = new StringReader(source);
	    StringWriter writer = new StringWriter();
	    TransformerFactory tFactory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl",null);
	    Transformer transformer = tFactory.newTransformer( new javax.xml.transform.stream.StreamSource(xslPath));
	    
	    transformer.setOutputProperty(OutputKeys.INDENT, "yes");

	    transformer.transform(
	            new javax.xml.transform.stream.StreamSource(reader), 
	            new javax.xml.transform.stream.StreamResult(writer));

	   return  writer.toString();
		
	}
	
	public static void transformXsl(File input, File output, File xsl) throws TransformerException, FileNotFoundException{
		TransformerFactory tFactory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl",null);
		
	    Transformer transformer =
	      tFactory.newTransformer
	         (new javax.xml.transform.stream.StreamSource
	            (xsl));

	    transformer.transform
	      (new javax.xml.transform.stream.StreamSource
	            (input),
	       new javax.xml.transform.stream.StreamResult
	            (output));
	}
	
	public static String transformXsl(String source, String xslPath, String outPath) throws TransformerException, FileNotFoundException{
		TransformerFactory tFactory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl",null);
		StringReader reader = new StringReader(source);
	    StringWriter writer = new StringWriter();
	    
	    String path="";
	    if(outPath.startsWith("\\\\")){
			path = "file:\\\\"+outPath;
		}
		else{
			path = "file:\\"+outPath;
		}
	    path = path.replace('\\', '/');
	    
	    Transformer transformer = tFactory.newTransformer( new javax.xml.transform.stream.StreamSource(xslPath) );
	    transformer.setParameter("outpath", path);
	    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	    transformer.transform(
	            new javax.xml.transform.stream.StreamSource(reader), 
	            new javax.xml.transform.stream.StreamResult(writer));

	   return  writer.toString();
	}
	
	public static void deleteDir(File dir) {
		FileUtils.deleteQuietly(dir);
	}
	
	public static File createTempDirectory() throws IOException
	{
		return createTempDirectory("temp");
	}
	public static File createTempDirectory(String preffix) throws IOException
	{
	    final File temp;
	
	    temp = File.createTempFile(preffix, Long.toString(System.nanoTime()));
	
	    if(!(temp.delete()))
	    {
	        throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
	    }
	
	    if(!(temp.mkdir()))
	    {
	        throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
	    }
	
	    return (temp);
	}
	
	/**
	 * 
	 * @param article
	 * @return
	 * @throws DocumentException
	 */
	public static List<String> getWebContentFileEntryTitles(JournalArticle article) throws DocumentException
	{
		String xPathQuery = "//dynamic-element[@type='document_library' and not(starts-with(dynamic-content, '/binrepository/'))]/dynamic-content";
		return getWebContentResourcesTitles(article, xPathQuery);
	}
	
	public static List<String> getWebContentBinaryTitles(JournalArticle article) throws DocumentException
	{
		String xPathQuery = "//dynamic-element[@type='document_library' and starts-with(dynamic-content, '/binrepository/')]/dynamic-content";
		return getWebContentResourcesTitles(article, xPathQuery);
	}
	
	private static List<String> getWebContentResourcesTitles(JournalArticle article, String xPathQuery) throws DocumentException
	{
		List<String> titleList = new ArrayList<String>();
		Document document = SAXReaderUtil.read(article.getContent());
		XPath xpathSelector = SAXReaderUtil.createXPath(xPathQuery);
		List<Node> nodes = xpathSelector.selectNodes(document);
		
		for (Node node : nodes) 
		{
			String path = node.getStringValue();
			if (path != null && path.length() > 0)
			{
				String [] dlURL = path.split(StringPool.SLASH);
				String title = dlURL[dlURL.length-1];
				
				if (!titleList.contains( title )) 
					titleList.add(title);
			}
		}
		
		return titleList;
	}
	/**
	 * Recupera los documentLibrary asociados a un articulo. 
	 * INFO: Esta función es una copia de la que se encuentra en PageContentLocalServiceImpl, para eliminar dependencias entre portlets.
	 * @param article
	 * @return
	 * @throws SystemException 
	 */
	public static List<DLFileEntry> getWebContentFileEntries(JournalArticle article) throws SystemException
	{
		List<DLFileEntry> webContentFileEntries = new ArrayList<DLFileEntry>();
		
		Document document = null;
		try 
		{
			document = SAXReaderUtil.read(article.getContent());
		} 
		catch (DocumentException e) 
		{
			_log.error("Unable to read the WebContent content", e);
			throw new SystemException(e);
		}
		
		String xPathQuery = "//dynamic-element[@type='document_library' and not(starts-with(dynamic-content, '/binrepository/'))]/dynamic-content";
		
		XPath xpathSelector = SAXReaderUtil.createXPath(xPathQuery);
		List<Node> nodes = xpathSelector.selectNodes(document);
		
		for (Node node : nodes) 
		{
			if (node.getStringValue().contains(StringPool.SLASH))
			{
				String [] dlURL = node.getStringValue().split(StringPool.SLASH);
				long folderId = 0;
				//Si el formato es 0/1/2/.../N-2/N-1, 0 es el grupo, N-2 es el folderId y N-1 es el title
				//En cualquier otro caso, el folderId es 0
				if (dlURL.length > 2)
				{
					folderId = Long.valueOf(dlURL[dlURL.length-2]);
				}
				
				DLFileEntry fileEntry = null;
				
				String title = dlURL[dlURL.length-1];
				try
				{
					fileEntry = DLFileEntryLocalServiceUtil.getFileEntryByTitle(article.getGroupId(), folderId, title);
				}
				catch(Exception err1)
				{
					//Comprobar que el title no necesita un URLdecode (por los parentesis de Milenium)
					try
					{
						fileEntry = DLFileEntryLocalServiceUtil.getFileEntryByTitle(article.getGroupId(), folderId, URLDecoder.decode(title, "UTF-8"));
					}
					catch(Exception err2)
					{
						//Si el último miembro de la URL no es el title, es el UUID
						try
						{
							fileEntry = DLFileEntryLocalServiceUtil.getFileEntryByUuidAndGroupId(title, article.getGroupId());
						}
						catch(Exception err3)
						{
							_log.error("File " + title + " not found");
						}
					}					
				}
				
				if (fileEntry != null && !webContentFileEntries.contains(fileEntry))
					webContentFileEntries.add(fileEntry);
			}
		}	
		
		return webContentFileEntries;
	}
	
	/**
	 * 
	 * @param milliseconds
	 * @return
	 */
	public static String toHMS (long milliseconds){
		Formatter fmt = new Formatter();
		
		int seconds = (int) (milliseconds / 1000) % 60 ;
		int minutes = (int) ((milliseconds / (1000*60)) % 60);
		int hours   = (int) ((milliseconds / (1000*60*60)) % 24);
		int milli = (int) milliseconds - (hours * 3600000) - (minutes * 60000) - (seconds * 1000);
		
		fmt.format("%1$02d:%2$02d:%3$02d,%4$03d",hours,minutes,seconds,milli);
		return fmt.toString();
		//return hours + ":" + minutes + ":" + seconds + "," + milli;
	}
	
	/**
	 * 
	 * @param fullFilePath
	 * @param fileData
	 * @return
	 * @throws IOException
	 */
	public static File generateFile(String fullFilePath, String fileData) throws IOException
	{
		File file = new File(fullFilePath);
		BufferedWriter output = new BufferedWriter(new FileWriter(file));
		output.write(fileData);
		output.close();
		
		return file;
	}
	
	/**
	 * 
	 * @param filePath
	 * @param filePrefix
	 * @param fileExt
	 * @return
	 */
	public static String generateFullFilePath(String filePath, String filePrefix, String fileExt)
	{
		if (filePath == null)
			filePath = StringPool.BLANK;

		String fullFilePath = String.format("%s/%s", filePath, generateFileName(filePrefix, fileExt));
		return fullFilePath;
	}
	
	public static String generateFileName(String filePrefix, String fileExt)
	{
		if (filePrefix == null)
			filePrefix = StringPool.BLANK;

		return String.format("%s%s%d%s", IterKeys.XMLIO_ZIP_FILE_PREFIX, filePrefix, (new Date()).getTime(), fileExt);
	}
	
	/**
	 * 
	 * @param liveConf
	 * @param localFile
	 * @throws Exception
	 */
	public static void sendFile(LiveConfiguration liveConf, File localFile) throws Exception
	{
		String fileName = localFile.getName();
		
		//Realiza el envio por el procedimiento elegido
		if (liveConf.getOutputMethod().equals(IterKeys.LIVE_CONFIG_OUTPUT_METHOD_FILE_SYSTEM))
		{
			_log.info( String.format("Process: %s, Sending publish data via file-system...", fileName) );
			// Copia el fichero de la carpeta local a la de destino
			File remoteFile = new File( liveConf.getRemotePath().concat(StringPool.SLASH).concat(fileName) );
			XMLIOUtil.copyFile(localFile, remoteFile);
			_log.info( String.format("Process: %s, Sending publish data via file-system completed", fileName) );
		}
		else
		{
			_log.info( String.format("Process: %s, Sending publish data via ftp...", fileName) );
			String ftpPath 		= liveConf.getFtpPath();
			String ftpUser 		= liveConf.getFtpUser();
			String ftpPassword 	= liveConf.getFtpPassword();
			FTPUtil.sendFile(ftpPath, ftpUser, ftpPassword, localFile.getName(), localFile.getAbsolutePath(), "");
			_log.info( String.format("Process: %s, Sending publish data via ftp completed", fileName) );
		}
	}
	
	public static File getFile(String fileName) throws Exception
	{
		LiveConfiguration lc = LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(IterGlobal.getCompanyId());
		String filePath	= StringPool.BLANK;
		
		if (lc.getOutputMethod().equals(IterKeys.XMLIO_CHANNEL_MODE_FTP))
		{
			filePath = FTPUtil.receiveFile(lc.getFtpPath(), lc.getFtpUser(), lc.getFtpPassword(), 
										   fileName, lc.getLocalPath(), IterKeys.XMLIO_VALID_INPUT_FILE_PATTERN);
		} 
		else
		{
			filePath = lc.getRemotePath().concat("/").concat(fileName);	
		}
		
		File sourceFile = new File(filePath);
		ErrorRaiser.throwIfFalse(FileUtil.exists(sourceFile), IterErrorKeys.XYZ_E_IMPORT_FILE_NOT_FOUND_ZYX);

		return sourceFile;
	}
}
