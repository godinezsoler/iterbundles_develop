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

package com.protecmedia.iter.news.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.documentlibrary.DuplicateFileException;
import com.liferay.portal.apache.ApacheUtil;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.documentlibrary.NoSuchFileEntryException;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.service.BinaryRepositoryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.news.service.base.DLFileEntryLocalServiceBaseImpl;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

/**
 * The implementation of the d l file entry local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.news.service.DLFileEntryLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.news.service.DLFileEntryLocalServiceUtil} to access the d l file entry local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author protec
 * @see com.protecmedia.iter.news.service.base.DLFileEntryLocalServiceBaseImpl
 * @see com.protecmedia.iter.news.service.DLFileEntryLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class DLFileEntryLocalServiceImpl extends DLFileEntryLocalServiceBaseImpl 
{
	private static final String PATH_QSTR_1_PARAM	= "%s/%s?%s=%s";
	private static final String PATH_QSTR_2_PARAM	= "%s/%s?%s=%s&%s=%s";
	private static final String PATH_URL_FMT		= "%s/%s/%s"; 
	private static final String PATH_URL_WH			= "%s/%sx%s/%s";

	private static final String COL_WIDTH			= "width";
	private static final String COL_HEIGHT			= "height";
	private static final String COL_FMT				= "fmt";
	
	private static final String GET_DATA_FILEENTRY = new StringBuffer()
	.append("SELECT companyId, userId, groupId, folderId, name ")
	.append("FROM dlfileentry ")
	.append("WHERE uuid_ = '%s'")
	.toString();

	
	//private static final String GET_ALL_RESOLUTIONS = String.format("SELECT resolutionName as %s,%s,%s FROM ImageResolution", COL_FMT, COL_WIDTH, COL_HEIGHT);
	private static final String GET_ALL_RESOLUTIONS = String.format(
													new StringBuilder("SELECT resolutionName AS fmt,			\n").
																append(" CASE   WHEN 	(width IS NULL)  THEN 0	\n").
																append(" 		WHEN   width < 0 		 THEN 0	\n").
																append(" 		ELSE   width					\n").
																append(" END width,								\n").
																append(" CASE 	WHEN 	(height IS NULL) THEN 0	\n").
																append(" 		WHEN 	height < 0 		 THEN 0	\n").
																append(" 		ELSE	height					\n").
																append(" END height								\n").
																append("FROM ImageResolution").toString(), 
													COL_FMT, 
													COL_WIDTH,	COL_WIDTH,	COL_WIDTH,	COL_WIDTH,
													COL_HEIGHT, COL_HEIGHT, COL_HEIGHT, COL_HEIGHT);	
	
	
	private static Log _log = LogFactoryUtil.getLog(DLFileEntryLocalServiceImpl.class);
	private static final String _NO_SUCH_ENTITY_WITH_GLOBALID = "No DLFileEntry exists in the group %d whith the globalId %s";
	
	private static final String INSERT_BINARY_FOR_LIVE_DELETE = "INSERT INTO binariesdeleted (binaryId, articleId) VALUES ('%s', '%s') ON DUPLICATE KEY UPDATE deleteDate=CURRENT_TIMESTAMP";
	private static final String DELETE_BINARY_FOR_LIVE_DELETE = "DELETE FROM binariesdeleted WHERE binaryId = '%s'";
	
	public void deleteFileEntry(long groupId, String globalID) throws PortalException, SystemException 
	{
		String sql = String.format(SQLQueries.SEL_FILEENTRY_BY_GLOBALID, groupId, globalID );
		
		List<Object> listFileEntry = PortalLocalServiceUtil.executeQueryAsList( sql );
		
		// Se comprueba si existe un FileEntry con ese globalId
		if (listFileEntry.size() == 0)
			throw new NoSuchFileEntryException( String.format(_NO_SUCH_ENTITY_WITH_GLOBALID, groupId, globalID) );
		
		Object [] result= (Object[]) listFileEntry.get(0);
		long fileGroupId= Long.parseLong(result[0].toString());
		long folderId	= Long.parseLong(result[1].toString());
		String name		= result[2].toString();
		
		com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil.deleteFileEntry(fileGroupId, folderId, name);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public Document deleteFileEntryFromURL(String url) throws Exception
	{
		boolean isFileEntryURL 	= false;
		String virtualHost		= HttpUtil.getDomain(url);
		String currentURL		= HttpUtil.removeDomain( HttpUtil.fixPath(url) );
		
		if (currentURL.contains("?")) 
		{
			currentURL = currentURL.substring(0, currentURL.indexOf('?'));
		}
		String docSeparator		= "/documents/";
		
		
		Document document 		= SAXReaderUtil.createDocument();
		Element rootElement 	= document.addElement("rs");

		if (currentURL.startsWith(docSeparator))
		{
			String auxUrl 		= currentURL.substring(currentURL.indexOf(docSeparator) + docSeparator.length());
			String[] pathArray 	= auxUrl.split("/");
			
			if (pathArray.length > 0)
			{
				isFileEntryURL = true;
				DLFileEntry dlFileEntry = null;
				
				try
				{
//					// Version en el que contemplan varios tipos de URLs de imágenes
//					if (Validator.isNumber(pathArray[0]))
//						dlFileEntry = getFileFromURL(pathArray);
//					else 
//						dlFileEntry = getDocumentLibraryFromURL(pathArray);
					
					if (Validator.isNumber(pathArray[0]) && pathArray.length > 2)
					{
						long groupId 	= GetterUtil.getLong(pathArray[0]);
						long folderId 	= GetterUtil.getLong(pathArray[1]);
						String title = HttpUtil.decodeURL(pathArray[pathArray.length - 1], true);
						
						Element elemArticle = rootElement.addElement("fileentry");
						elemArticle.addAttribute( "groupName", 	GroupLocalServiceUtil.getGroup(groupId).getName() );
						elemArticle.addAttribute( "folderId", 	Long.toString(folderId));
						elemArticle.addAttribute( "title",   	title);
						
						dlFileEntry = DLFileEntryLocalServiceUtil.getFileEntryByTitle(groupId, folderId, title);
						DLFileEntryLocalServiceUtil.deleteFileEntry(dlFileEntry, new ServiceContext());
						
						
						notifyDeleteFileEntryToApaches(title, virtualHost);
					}
				}
				catch (NoSuchFileEntryException e)
				{
					_log.debug(e.toString());
				}
				
				boolean found = (dlFileEntry != null);
				rootElement.addAttribute("type",	IterKeys.CLASSNAME_DLFILEENTRY);
				rootElement.addAttribute("found",	Boolean.toString(found));
				
//				// Version en el que contemplan varios tipos de URLs de imágenes
//				if (found)
//				{
//					Element elemArticle = rootElement.addElement("fileentry");
//					elemArticle.addAttribute( "groupName", 	GroupLocalServiceUtil.getGroup(dlFileEntry.getGroupId()).getName() );
//					elemArticle.addAttribute( "folderId", 	Long.toString(dlFileEntry.getFolderId()) );
//					elemArticle.addAttribute( "name",   	dlFileEntry.getName());
//				}
			}
		}

		if (!isFileEntryURL)
		{
			rootElement.addAttribute("type",	IterKeys.CLASSNAME_UNKNOWN);
			rootElement.addAttribute("found",	"false");
		}
		return document;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Llama por JSON al deleteFileEntry del entorno Live
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private boolean deleteRemoteFileEntry2(long groupId, long folderId, String title) throws UnsupportedEncodingException, ClientProtocolException, IOException, SystemException, PortalException, com.liferay.portal.kernel.error.ServiceError
	{
		boolean found = false;
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW)) 
		{
			Group group					= GroupLocalServiceUtil.getGroup(groupId);
			
			LiveConfiguration liveConf	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(group.getCompanyId());
			String remoteIP 			= liveConf.getRemoteIterServer2().split(":")[0];
			int remotePort 				= Integer.valueOf(liveConf.getRemoteIterServer2().split(":")[1]);
			String remoteMethodPath 	= "/news-portlet/secure/json";
			
			List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
			remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.news.service.DLFileEntryServiceUtil"));
			remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"deleteDLFileEntry"));
			remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[groupName, folderId, title]"));
			remoteMethodParams.add(new BasicNameValuePair("groupName", 			group.getName()));
			remoteMethodParams.add(new BasicNameValuePair("folderId", 			Long.toString(folderId)));
			remoteMethodParams.add(new BasicNameValuePair("title", 				title));
			
			String result = XMLIOUtil.executeJSONRemoteMethod2(group.getCompanyId(), remoteIP, remotePort, liveConf.getRemoteUserName(), liveConf.getRemoteUserPassword(), remoteMethodPath, remoteMethodParams);
			JSONObject json = JSONFactoryUtil.createJSONObject(result);
			
			String errorMsg = json.getString("exception");
			if (!errorMsg.isEmpty()) 
			{
				// Puede ser una excepción de tipo Iter, si no lo es devuelve
				// todo el texto y también se lanza porque era una excepción del
				// sistema
				String iterErrorMsg = ServiceErrorUtil.containIterException(errorMsg);
				throw new SystemException(iterErrorMsg.isEmpty() ? errorMsg : iterErrorMsg);
			}
	
			found = Boolean.parseBoolean(json.getString("returnValue"));
		}
		return found;
	}

	/**
	 * 
	 * @param groupId
	 * @param folderId
	 * @param name
	 * @param deleteFromLive
	 * @throws Exception
	 */
	public void deleteFileEntry2(long groupId, long folderId, String name, boolean deleteFromLive) throws Exception
	{
		Exception eLive = null;
		boolean foundInLive = !deleteFromLive;
		boolean foundInBack = false;

		try
		{
			DLFileEntry dlFileEntry = DLFileEntryLocalServiceUtil.getFileEntry(GroupMgr.getGlobalGroupId(), folderId, name);
			
			if (deleteFromLive)
			{
				try 
				{
					// Se intenta borrar del LIVE
					foundInLive = deleteRemoteFileEntry2(groupId, folderId, dlFileEntry.getTitle());
				} 
				catch (Exception e) 
				{
					eLive = e;
				}
			}

			com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil.deleteFileEntry(dlFileEntry, new ServiceContext());
			foundInBack = true;
		}
		catch (NoSuchFileEntryException e)
		{
			_log.debug(e.toString());
		}

		// Esta excepción NO se quiere que provoque un rollback
		if (eLive != null)
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_DELETE_CONTENT_ZYX, eLive), eLive);

		// No se encuentra en ninguno de los dos sistemas. Esta excepción NO se quiere que provoque un rollback
		ErrorRaiser.throwIfFalse(!(!foundInLive && !foundInBack), IterErrorKeys.XYZ_E_CONTENT_NOT_FOUND_IN_BACK_LIVE_ZYX);

		// No se encuentra en el LIVE. Esta excepción NO se quiere que provoque un rollback
		ErrorRaiser.throwIfFalse(foundInLive, IterErrorKeys.XYZ_E_CONTENT_NOT_FOUND_IN_LIVE_ZYX);

		// No se encuentra en el BACK. Esta excepción NO se quiere que provoque un rollback
		ErrorRaiser.throwIfFalse(foundInBack, IterErrorKeys.XYZ_E_CONTENT_NOT_FOUND_IN_BACK_ZYX);

	}
	
	/**
	 * 
	 * @param groupName
	 * @param folderId
	 * @param name
	 * @return
	 * @throws PortalException
	 * @throws SystemException
	 */
	public boolean deleteDLFileEntry(String groupName, long folderId, String title) throws PortalException, SystemException
	{
		long globalGroupId 	= GroupMgr.getGlobalGroupId();
		long companyId 		= GroupLocalServiceUtil.getGroup(globalGroupId).getCompanyId();
		long scopeGroupId	= GroupLocalServiceUtil.getGroup(companyId, groupName).getGroupId();
		boolean found 		= false;
		try 
		{
			DLFileEntry dlFileEntry = com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil.getFileEntryByTitle(globalGroupId, folderId, title);
			
			com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil.deleteFileEntry(dlFileEntry, new ServiceContext());
			found = true;
			
			notifyDeleteFileEntryToApaches(dlFileEntry.getTitle(), scopeGroupId);
		}
		catch (NoSuchFileEntryException e)
		{
			_log.debug(e.toString());
		}

		return found;
	}
	
	public void notifyDeleteFileEntryToApaches(JournalArticle article, long scopeGroupId)
	{
		notifyDeleteBinaryToApaches(article, scopeGroupId, true);
	}
	
	public void notifyDeleteBinaryToApaches(JournalArticle article, long scopeGroupId)
	{
		notifyDeleteBinaryToApaches(article, scopeGroupId, false);	
	}
	
	private void notifyDeleteBinaryToApaches(JournalArticle article, long scopeGroupId, boolean isDLFileEntry)
	{
		try 
		{
			List<String> titleList = isDLFileEntry ?
									 XMLIOUtil.getWebContentFileEntryTitles(article) :
									 XMLIOUtil.getWebContentBinaryTitles(article);
			String virtualhost 		= LayoutSetLocalServiceUtil.getLayoutSet(scopeGroupId, false).getVirtualHost();
			
			for (String title : titleList)
			{
				notifyDeleteFileEntryToApaches(title, virtualhost, isDLFileEntry);
			}
		}
		catch (Exception e) 
		{
			_log.error(e.toString());
		} 
	}
	/**
	 * Método que genera todas posibles URLs que podrían terminar llamando a este DLFileEntry
	 * y llama a los Apaches configurados con cada una de ellas
	 * 
	 * @param fileEntry
	 */
	private void notifyDeleteFileEntryToApaches(String title, long scopeGroupId)
	{
		try 
		{
			String virtualhost = LayoutSetLocalServiceUtil.getLayoutSet(scopeGroupId, false).getVirtualHost();
			notifyDeleteFileEntryToApaches(title, virtualhost);
		} 
		catch (Exception e) 
		{
			_log.error(e.toString());
		} 
	}
	/**
	 * Método que genera todas posibles URLs que podrían terminar llamando a este DLFileEntry
	 * y llama a los Apaches configurados con cada una de ellas
	 * 
	 * @param fileEntry
	 */
	private void notifyDeleteFileEntryToApaches(String title, String virtualHost)
	{
		notifyDeleteFileEntryToApaches(title, virtualHost, true);
	}
	
	private void notifyDeleteFileEntryToApaches(String title, String virtualHost, boolean isDLFileEntry)
	{
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_LIVE) && virtualHost != null && virtualHost.length() > 0) 
		{
			List<String> urlList = new ArrayList<String>();
			String commonURL = isDLFileEntry ?
							   String.format("http://%s/documents/%d/0", virtualHost, GroupMgr.getGlobalGroupId()) :
							   String.format("http://%s/binrepository", virtualHost);
			
			// Se crea y añade la URL canónica
			urlList.add( new StringBuilder(commonURL).append("/").append(title).toString() );
			
			// Se crean y añaden, si están configuradas, las URLs con reescalado
			String preferedMode = GetterUtil.get( PropsUtil.get(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SCALE_ON_THE_FLY_PREFEREDMODE), IterKeys.SCALE_ON_THE_FLY_MODE_QSTRFMT );
			if (preferedMode.length() > 0)
			{
				String width = "", height = "";
				int i = 0;
				Map<String, Object> resolution;
				List<Map<String, Object>> resolutions = PortalLocalServiceUtil.executeQueryAsMap(GET_ALL_RESOLUTIONS);
				
				for (i = 0; i < resolutions.size(); i++)
				{
					resolution 					= resolutions.get(i);
					String[] preferedModeList	= preferedMode.split(",");
					
					for (int iMode = 0; iMode < preferedModeList.length; iMode++)
					{
						String url = "";
						
						if (preferedModeList[iMode].equalsIgnoreCase(IterKeys.SCALE_ON_THE_FLY_MODE_QSTRFMT))
						{
							// Formato en el QueryString
							url = String.format(PATH_QSTR_1_PARAM, commonURL, title, COL_FMT, resolution.get(COL_FMT).toString());
						}
						else if (preferedModeList[iMode].equalsIgnoreCase(IterKeys.SCALE_ON_THE_FLY_MODE_QSTRWH))
						{
							// Ancho y alto en el QueryString
							width	= resolution.get(COL_WIDTH).toString();
							height	= resolution.get(COL_HEIGHT).toString();
							
							if (!width.equals("0") && !height.equals("0"))
								url = String.format(PATH_QSTR_2_PARAM, commonURL, title, COL_WIDTH, width, COL_HEIGHT, height);
							
							else if (width.equals("0") && !height.equals("0"))
								url = String.format(PATH_QSTR_1_PARAM, commonURL, title, COL_HEIGHT, height);
							
							else if (!width.equals("0") && height.equals("0"))
								url = String.format(PATH_QSTR_1_PARAM, commonURL, title, COL_WIDTH, width);
						}
						else if (preferedModeList[iMode].equalsIgnoreCase(IterKeys.SCALE_ON_THE_FLY_MODE_URLFMT))
						{
							// Formato en el medio de la URL
							url = String.format(PATH_URL_FMT, commonURL, resolution.get(COL_FMT).toString(), title);
						}
						else if (preferedModeList[iMode].equalsIgnoreCase(IterKeys.SCALE_ON_THE_FLY_MODE_URLWH))
						{
							// Ancho y alto en el medio de la URL
							width	= resolution.get(COL_WIDTH).toString();
							height	= resolution.get(COL_HEIGHT).toString();
							
							if ( !(width.equals("0") && height.equals("0")) )
								url = String.format(PATH_URL_WH, commonURL, width, height, title);
						}
						
						if (url.length() > 0)
							urlList.add(url);
					}
				}
			}
			
			ApacheUtil.notifyToAllApaches(urlList);
		}
	}

	/**
	 * 
	 * @param uuid
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws ServiceError
	 * @throws PortalException
	 * @throws SystemException
	 */
	public InputStream getFileAsStreamByUuid(String uuid, long delegationId) throws NoSuchMethodException, SecurityException, ServiceError, PortalException, SystemException
	{
		Document dom = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_DATA_FILEENTRY, uuid));
		ErrorRaiser.throwIfNull(dom);

		long company = XMLHelper.getLongValueOf(dom, "rs/row/@companyId", -1);
		ErrorRaiser.throwIfFalse(company >= 0);
		
		long userId = XMLHelper.getLongValueOf(dom, "rs/row/@userId", -1);
		ErrorRaiser.throwIfFalse(userId >= 0);
		
		long groupId = XMLHelper.getLongValueOf(dom, "rs/row/@groupId", -1);
		ErrorRaiser.throwIfFalse(groupId >= 0);
		
		long folderId = XMLHelper.getLongValueOf(dom, "rs/row/@folderId", -1);
		ErrorRaiser.throwIfFalse(folderId >= 0);
		
		String name = XMLHelper.getTextValueOf(dom, "rs/row/@name", null);
		ErrorRaiser.throwIfNull(name);
		
		return DLFileEntryLocalServiceUtil.getFileAsStream(delegationId,userId, groupId, folderId, name);
	}
	
	public String addFileEntry2(HttpServletRequest request, HttpServletResponse response, InputStream is, String title, String delegationId) throws ServiceError, DocumentException, PortalException, SystemException  
	{
		// Valida la entrada
		ErrorRaiser.throwIfNull(title, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(delegationId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(is, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Establece los parámetros estáticos
		String result = StringPool.BLANK;
		long userId = GroupMgr.getDefaultUserId();
		long groupId = GroupMgr.getGlobalGroupId();
		long folderId = 0;
		    		
		ServiceContext sc = new ServiceContext();
		sc.setDelegationId(Long.valueOf(delegationId));
		
		try
		{
			DLFileEntry dlfileEntry = DLFileEntryLocalServiceUtil.addFileEntry(userId, groupId, folderId, title, title, StringPool.BLANK, StringPool.BLANK, StringPool.BLANK, is, request.getContentLength(), sc);
			result = String.valueOf(dlfileEntry.getName());
		}
		catch (NoSuchFileEntryException e)
		{
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(new DuplicateFileException(e)) );
		}

		return result;
	}
	
	public String addFileEntry3(HttpServletRequest request, HttpServletResponse response, InputStream is, String title, String delegationId) throws SystemException, com.liferay.portal.kernel.error.ServiceError 
	{
		// Añade el binario. No es necesario validar la entrada, ya que se realiza en el servicio del repositorio.
		String binaryId = BinaryRepositoryLocalServiceUtil.addBinary(is, title, delegationId);
		
		// Si estamos en el BACK...
		if (PropsValues.IS_PREVIEW_ENVIRONMENT)
		{
			try
			{
				// Si está anotado para borrarse en el LIVE, lo cancela.
				PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_BINARY_FOR_LIVE_DELETE, binaryId));
			}
			catch (Throwable th)
			{
				throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
			}
		}
		
		return binaryId;
	}
	
	public String updateFileEntry2(HttpServletRequest request, HttpServletResponse response, InputStream is, String name, String title, String delegationId) throws ServiceError, DocumentException, SystemException, PortalException
	{
		// Valida la entrada
		ErrorRaiser.throwIfNull(name, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(title, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(delegationId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(is, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Establece los parámetros estáticos
		String result = StringPool.BLANK;
		long userId = GroupMgr.getDefaultUserId();
		long groupId = GroupMgr.getGlobalGroupId();
		long folderId = 0;
		
		// Procesa la imagen a subir
		ServiceContext sc = new ServiceContext();
		sc.setDelegationId(Long.valueOf(delegationId));
		
		DLFileEntry dlfileEntry = DLFileEntryLocalServiceUtil.updateFileEntry(userId, groupId, folderId, name, title, title, StringPool.BLANK, StringPool.BLANK, true, StringPool.BLANK, is, request.getContentLength(), sc);
		result = String.valueOf(dlfileEntry.getName());

		return result;
	}
	
	public String updateFileEntry3(HttpServletRequest request, HttpServletResponse response, InputStream is, String name, String title, String delegationId) throws SystemException, com.liferay.portal.kernel.error.ServiceError 
	{
		// Si name es numérico, es un DLFileEntry, y hay que borrarlo primero.
		if (Validator.isNumber(name))
		{
			deleteFileEntry3(name, Long.valueOf(delegationId));
		}
		// No es necesario validar la entrada, ya que se realiza en el servicio del repositorio.
		String binaryId = BinaryRepositoryLocalServiceUtil.addBinary(is, title, delegationId);
		
		// Si estamos en el BACK...
		if (PropsValues.IS_PREVIEW_ENVIRONMENT)
		{
			try
			{
				// Si está anotado para borrarse en el LIVE, lo cancela.
				PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_BINARY_FOR_LIVE_DELETE, binaryId));
			}
			catch (Throwable th)
			{
				throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
			}
		}
		
		return binaryId;
	}
	
	public String deleteFileEntry3(String name, long delegationId) throws SystemException
	{
		try
		{
			// Valida la entrada
			ErrorRaiser.throwIfNull(name, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			long groupId = GroupMgr.getGlobalGroupId();
			long folderId = 0;
			
			DLFileEntry fileEntry = DLFileEntryLocalServiceUtil.getFileEntry(groupId, folderId, name);
			ServiceContext serviceContext = new ServiceContext();
			serviceContext.setDelegationId(delegationId);
			DLFileEntryLocalServiceUtil.deleteFileEntry(fileEntry, serviceContext);
		}
		catch (Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return StringPool.BLANK;
	}
	
	public String deleteFileEntry4(String name, String delegationId, String articleId) throws SystemException, com.liferay.portal.kernel.error.ServiceError
	{
		// Si name es numérico, es un DLFileEntry
		if (Validator.isNumber(name))
		{
			deleteFileEntry3(name, Long.valueOf(delegationId));
		}
		else
		{
			// Elimina el binario. No es necesario validar la entrada, ya que se realiza en el servicio del repositorio.
			BinaryRepositoryLocalServiceUtil.deleteBinary(name, delegationId);
			
			// Si es un borrado en el BACK...
			if (PropsValues.IS_PREVIEW_ENVIRONMENT)
			{
				try
				{
					// Anota el borrado para que se efectuúe en el LIVE en la próxima publicación.
					PortalLocalServiceUtil.executeUpdateQuery(String.format(INSERT_BINARY_FOR_LIVE_DELETE, name, articleId));
				}
				catch (Throwable th)
				{
					throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
				}
			}
		}
		
		return StringPool.BLANK;
	}
}