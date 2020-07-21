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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.documentlibrary.NoSuchFileException;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.documentlibrary.NoSuchFolderException;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.model.DLFolderConstants;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFolderLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.persistence.DLFileEntryUtil;
import com.protecmedia.iter.base.service.DLFileEntryMgrLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.base.FormTransformLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.IterAdmin;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.TeaserMgr;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

/**
 * The implementation of the form transform local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.FormTransformLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.FormTransformLocalServiceUtil} to access the form transform local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.FormTransformLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.FormTransformLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class FormTransformLocalServiceImpl extends FormTransformLocalServiceBaseImpl
{
	
	private static Log _log = LogFactoryUtil.getLog(FormTransformLocalServiceImpl.class);
	
	// Sólo una publicación al mismo tiempo
	private static ReadWriteLock exportGlobalLock = new ReentrantReadWriteLock();
	private static Lock exportWriteLock = exportGlobalLock.writeLock();
	
	private static ReadWriteLock importGlobalLock = new ReentrantReadWriteLock();
	private static Lock importWriteLock = importGlobalLock.writeLock();

	
	private static final String GET_XSL_TRANSFORMS = new StringBuilder( "SELECT transformid, transformname, transformdesc, xsluuid, (DLFileEntry.description) xslname" )
															.append( " FROM formxsltransform" )
															.append( " LEFT JOIN DLFileEntry ON (formxsltransform.xsluuid = DLFileEntry.uuid_ AND DLFileEntry.groupId = formxsltransform.groupid)" )
															.append( " WHERE formxsltransform.groupId = %s ORDER BY transformname ASC" ).toString();


	private static final String GET_XSL_TRANSFORM_BY_TRANSFORMID = new StringBuilder( "SELECT transformid, transformname, transformdesc, xsluuid, (DLFileEntry.description) xslname" )
															.append( " FROM formxsltransform" )
															.append( " LEFT JOIN DLFileEntry ON (formxsltransform.xsluuid = DLFileEntry.uuid_ AND DLFileEntry.groupId = formxsltransform.groupid)" )
															.append( " WHERE formxsltransform.transformid IN ('%s')" ).toString();

	private static final String GET_XSL_TRANSFORM_BY_TRANSFORMID2 = new StringBuilder( "\n"														).append(
			"SELECT 	group_.name groupName, transformid, formxsltransform.groupid, transformname, transformdesc, dlfileentry.companyId, \n"	).append(
			"			userId, folderId, dlfileentry.name dlFileEntryName, dlfileentry.description dlFileEntryDesc, size_ \n"					).append(
			"FROM formxsltransform \n"																											).append(
			"INNER JOIN group_ ON formxsltransform.groupId = group_.groupId \n"																	).append(
			"LEFT JOIN dlfileentry ON (formxsltransform.xsluuid = dlfileentry.uuid_ AND formxsltransform.groupId = dlfileentry.groupId) \n"		).append(
			" 	WHERE formxsltransform.transformid IN ('%s') \n").toString();
	
	private static final String ADD_XSL_TRANSFORM = "INSERT INTO formxsltransform(transformid, groupid, transformname, transformdesc, xsluuid) VALUES ('%s', %s, '%s', '%s', '%s')";
	private static final String UPDATE_XSL_TRANSFORM = "UPDATE formxsltransform SET transformname='%s', transformdesc='%s', xsluuid='%s' WHERE transformid='%s'";
	private static final String DELETE_XSL_TRANSFORMS = "DELETE FROM formxsltransform WHERE transformid IN %s";
	private static final String UPDATE_XSLUUID = "UPDATE formxsltransform SET xsluuid='%s' where transformid='%s'";
	
	private static final String GET_INFO_URI_FILE = new StringBuffer()
	.append("SELECT dlfe.groupId groupid, dlfe.folderid folderid, dlfe.title title \n")
	.append("FROM dlfileentry dlfe \n")
	.append("where dlfe.uuid_ = '%s'")
	.toString();
	
	private static final MimetypesFileTypeMap MIMETYPES = new MimetypesFileTypeMap();
	
	public String getTransforms( long groupId ) throws ServiceError, NoSuchMethodException
	{
		String result = "";
		
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		result = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_XSL_TRANSFORMS, groupId), new String[]{"transformdesc"} ).asXML();
		
		return result;
	}
	
	public void getUrlXsl(HttpServletRequest request, HttpServletResponse response, String groupid, String xslUUID ) throws NoSuchMethodException, SecurityException, ServiceError, IOException
	{
		Document domDLFileEntryInfo;
		Long group;
		Long folder;
		String title;
		
		String requestURL	= request.getRequestURL().toString();
		URL urlHost			= new URL( requestURL.substring(0, requestURL.indexOf(request.getRequestURI())) );
		boolean secure 		= request.isSecure();

		// Comprueba el header X-Forwarded-Proto
		String protocolFromHeader = request.getHeader("X-Forwarded-Proto");
		// Será seguro si se indica https en el header. Si no, se usará el valor del request.
		secure = secure || Http.HTTPS.equalsIgnoreCase(protocolFromHeader);
		
		if (_log.isDebugEnabled())
			_log.debug("Query: ".concat(String.format(GET_INFO_URI_FILE, xslUUID)));
		
		domDLFileEntryInfo = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_INFO_URI_FILE, xslUUID),new String[]{"title"});
		ErrorRaiser.throwIfNull(domDLFileEntryInfo);
		ErrorRaiser.throwIfNull(group =  XMLHelper.getLongValueOf(domDLFileEntryInfo, "/rs/row/@groupid"));
		ErrorRaiser.throwIfNull(folder = XMLHelper.getLongValueOf(domDLFileEntryInfo, "/rs/row/@folderid"));
		ErrorRaiser.throwIfNull(title =  XMLHelper.getTextValueOf(domDLFileEntryInfo, "/rs/row/title"));
		
		String envQS = PropsValues.IS_PREVIEW_ENVIRONMENT?   "?env=preview"  : "?env=live";
		String file  = DLFileEntryMgrLocalServiceUtil.getDLFileEntryURL(group, folder, title).concat(envQS);
		URL url 	 = new URL(HttpUtil.getProtocol(secure), urlHost.getHost(), urlHost.getPort(), file);
		
		response.setContentType(MIMETYPES.getContentType(url.getPath()));
		String redirect = url.toString();
		
		if (_log.isTraceEnabled())
			_log.trace( String.format("url file redirect: %s", redirect) );
		
		response.sendRedirect(redirect);
	}
	
	public String addTransform( String xmlData  ) throws DocumentException, ServiceError, SQLException, NoSuchMethodException, IOException
	{
		return addTransform( SAXReaderUtil.read(xmlData).selectSingleNode("/rs/row") );
	}
	private String addTransform( Node node  ) throws DocumentException, ServiceError, SQLException, NoSuchMethodException, IOException
	{
		String transformname = XMLHelper.getTextValueOf(node, "@transformname");
		ErrorRaiser.throwIfNull(transformname, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		transformname = StringEscapeUtils.escapeSql(transformname);
		
		String xsluuid = XMLHelper.getTextValueOf(node, "@xsluuid");
		ErrorRaiser.throwIfNull(xsluuid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String groupid = XMLHelper.getTextValueOf(node, "@groupid");
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		String uuid = XMLHelper.getTextValueOf(node, "@transformid");
		if (Validator.isNull(uuid))
			uuid = SQLQueries.getUUID();
		
		node = node.selectSingleNode("transformdesc");
		
		String transformdesc = CDATA_strip(node.getStringValue());
		transformdesc = StringEscapeUtils.escapeSql(transformdesc);
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(ADD_XSL_TRANSFORM, uuid, groupid, transformname, transformdesc, xsluuid));
		
		return PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_XSL_TRANSFORM_BY_TRANSFORMID, uuid), new String[]{"transformdesc"} ).asXML();
	}
	
	public String editTransform( String xmlData ) throws DocumentException, ServiceError, SQLException, NoSuchMethodException, IOException
	{
		return editTransform( SAXReaderUtil.read(xmlData).selectSingleNode("/rs/row") );
	}
	private String editTransform( Node node ) throws DocumentException, ServiceError, SQLException, NoSuchMethodException, IOException
	{
		String transformid = XMLHelper.getTextValueOf(node, "@transformid");
		ErrorRaiser.throwIfNull(transformid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String transformname = XMLHelper.getTextValueOf(node, "@transformname");
		ErrorRaiser.throwIfNull(transformname, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		transformname = StringEscapeUtils.escapeSql(transformname);
		
		String xsluuid = XMLHelper.getTextValueOf(node, "@xsluuid");
		ErrorRaiser.throwIfNull(xsluuid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		node = node.selectSingleNode("transformdesc");
		
		String transformdesc = CDATA_strip(node.getStringValue());
		transformdesc = StringEscapeUtils.escapeSql(transformdesc);
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_XSL_TRANSFORM, transformname, transformdesc, xsluuid, transformid));
		
		return PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_XSL_TRANSFORM_BY_TRANSFORMID, transformid), new String[]{"transformdesc"} ).asXML();
	}
	
	public String deleteTransform( String xmlData ) throws DocumentException, ServiceError, IOException, SQLException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row/@transformid");
		
		List<Node> nodes = xpath.selectNodes(dataRoot);
		ErrorRaiser.throwIfFalse(nodes != null && nodes.size() > 0);
		
		String inClauseSQL = TeaserMgr.getInClauseSQL(nodes);

		PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_XSL_TRANSFORMS, inClauseSQL));

		return xmlData;
	}
	
	public String cancelOperation(String xmlData) throws DocumentException, ServiceError, NumberFormatException, PortalException, SystemException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row");
		Node node = xpath.selectSingleNode(dataRoot);
		
		String transformid = XMLHelper.getTextValueOf(node, "@transformid");

		String xsluuid = XMLHelper.getTextValueOf(node, "@xsluuid");
		
		String groupid = XMLHelper.getTextValueOf(node, "@groupid");
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		if( (Validator.isNull(transformid) || transformid.isEmpty()) && (Validator.isNotNull(xsluuid) && !xsluuid.isEmpty()) )
		{
			DLFileEntry dlfe = DLFileEntryLocalServiceUtil.getFileEntryByUuidAndGroupId(xsluuid, Long.parseLong(groupid));
			if( Validator.isNotNull( dlfe ) )
			{
    			DLFileEntryLocalServiceUtil.deleteDLFileEntry(dlfe);
			}
		}
		
		return xmlData;
	}
	
	@SuppressWarnings("unchecked")
	public String uploadXslFileEntry(HttpServletRequest request, HttpServletResponse response, InputStream is, String xmlData ) throws Exception
	{
		ErrorRaiser.throwIfNull(xmlData, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document dom = SAXReaderUtil.read(xmlData);
		
		Iterator<FileItem> files = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request).iterator();
		while (files.hasNext())
		{
	    	FileItem currentFile = files.next();
	    	if (!currentFile.isFormField())
	    	{
	    		uploadXslFileEntry(	GroupMgr.getDefaultUserId(), (Element)dom.selectSingleNode("/rs/row"), 
	    							currentFile.getName(), currentFile.getSize(), currentFile.getInputStream());
	    		break;
	    	}
		}
		
		return dom.asXML();
	}
	
	public void uploadXslFileEntry(long userId, Element transform, String fileName, long fileSize, InputStream is) throws Exception
	{
		long groupId = XMLHelper.getLongValueOf(transform, "@groupid");
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
				
		String xsluuid 		= XMLHelper.getTextValueOf(transform, "@xsluuid");
		String transformid 	= XMLHelper.getTextValueOf(transform, "@transformid");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(fileName), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		int indexPreriod 	= fileName.indexOf(StringPool.PERIOD);
		boolean hasExtension= indexPreriod > 0 && indexPreriod < fileName.length();
		ErrorRaiser.throwIfFalse(hasExtension, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		String xslExtension = fileName.substring(indexPreriod, fileName.length());
		String xslTitle		= SQLQueries.getUUID() + xslExtension;

		long folderId = getFolderId(groupId, userId);
		
		// Se recupera la xsl anterior
		DLFileEntry dlfeOld = Validator.isNotNull(xsluuid) ? DLFileEntryLocalServiceUtil.getFileEntryByUuidAndGroupId(xsluuid, groupId) : null;
		
		// Se crea la nueva xsl
		DLFileEntry dlfe = DLFileEntryLocalServiceUtil.addFileEntry(userId, groupId, folderId, xslTitle, fileName, StringPool.BLANK, StringPool.BLANK, is, fileSize, new ServiceContext());
		
		if( Validator.isNotNull( dlfeOld ) )
		{
			//Se elimina la xsl vieja
			DLFileEntryLocalServiceUtil.deleteFileEntryNoHook(dlfeOld);
		}
		
		// Se pone por título el uuid de la xsl, el nombre del fichero va en el campo descripcion
		String dlfeuuid = dlfe.getUuid();
		dlfe.setTitle( dlfeuuid + xslExtension );
		DLFileEntryUtil.update(dlfe, false);
		
		if( Validator.isNotNull(transformid) && !transformid.isEmpty() )
		{
			// Existe una transformación asociada a la XSL, hay que actualizar su referencia en la tabla
			PortalLocalServiceUtil.executeUpdateQuery( String.format(UPDATE_XSLUUID, dlfeuuid, transformid) );
		}
		
		transform.addAttribute("xsluuid", dlfeuuid);
		transform.addAttribute("xslname", fileName);
	}
	
	private static long getFolderId(long groupId, long userId) throws PortalException, SystemException, ServiceError
	{
	    DLFolder dlFolder = null;
	    try
	    {
	    	dlFolder = DLFolderLocalServiceUtil.getFolder(groupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, IterKeys.XSL_FORM_FOLDER);
	    }
	    catch (NoSuchFolderException nsfe)
	    {
	    	_log.debug("Creating " + IterKeys.XSL_FORM_FOLDER + " folder...");
	    	dlFolder = DLFolderLocalServiceUtil.addFolder(userId, groupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, IterKeys.XSL_FORM_FOLDER, StringPool.BLANK, new ServiceContext());
	    }
	    ErrorRaiser.throwIfNull(dlFolder);
	    
	    return dlFolder.getFolderId();
	}
	
	private static String CDATA_strip(String s)
	{
		String retVal;

		if (s.startsWith(StringPool.CDATA_OPEN) && s.endsWith(StringPool.CDATA_CLOSE))
			retVal = s.substring( StringPool.CDATA_OPEN.length(), s.length() - StringPool.CDATA_CLOSE.length());
		else
			retVal = s;

		return retVal;
	}

	public Document exportData(List<String> ids) throws SecurityException, ServiceError, NoSuchMethodException, DocumentException, PortalException, SystemException, IOException
	{
		return _exportData(ids);
	}
	
	public Document exportData(Long groupId) throws SecurityException, ServiceError, NoSuchMethodException, DocumentException, PortalException, SystemException, IOException
	{
		String sql = String.format("SELECT transformid FROM formxsltransform WHERE groupid = %d", groupId);
		return _exportData( PortalLocalServiceUtil.executeQueryAsList(sql) );
	}

	private Document _exportData(List<?> ids) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException, IOException
	{
		Document transformsDom = null;
		if (ids == null || ids.size() == 0)
		{
			transformsDom = SAXReaderUtil.read("<rs/>");
		}
		else
		{
			transformsDom = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_XSL_TRANSFORM_BY_TRANSFORMID2, StringUtil.merge(ids, "','")), new String[]{"transformdesc"} );
			
			// Para cada una de las XSLs con fichero se obtiene su texto y se añade como parte del XML descriptor de la transformación
			List<Node> transformList = transformsDom.selectNodes("/rs/row");
			
			long companyId = GroupMgr.getCompanyId();
			ErrorRaiser.throwIfFalse(companyId > 0);
			
			for (int i = 0; i < transformList.size(); i++)
			{
				Node attrCompanyId 	= transformList.get(i).selectSingleNode("@companyId");
				Node attrUserId 	= transformList.get(i).selectSingleNode("@userId");
				Node attrGroupId 	= transformList.get(i).selectSingleNode("@groupid");
				Node attrFolderId 	= transformList.get(i).selectSingleNode("@folderId");
				Node attrDLFEName 	= transformList.get(i).selectSingleNode("@dlFileEntryName");
				
				// Se recupera el ID de la delegación del grupo para saber en qué carpeta está físicamente la XSL
				long delegationId = GroupLocalServiceUtil.getGroup(Long.valueOf(attrGroupId.getText())).getDelegationId();
	
				// Se comprueba si existe fichero asociado a la transformación
				if ( Validator.isNotNull(attrCompanyId.getText()) )
				{
					try
					{
						InputStream is = DLFileEntryLocalServiceUtil.getFileAsStream(
											delegationId, Long.valueOf(attrUserId.getText()), 
											Long.valueOf(attrGroupId.getText()), Long.valueOf(attrFolderId.getText()), 
											attrDLFEName.getText());
						
						((Element)transformList.get(i)).addElement("bin").addCDATA( Base64.encodeBase64String(IOUtils.toByteArray(is)) );
						is.close();
					}
					catch (NoSuchFileException fileException)
					{
						String transformName 	= XMLHelper.getTextValueOf(transformList.get(i), "@transformname", 	"");
						String dlFileEntryDesc	= XMLHelper.getTextValueOf(transformList.get(i), "@dlFileEntryDesc","");
						
						ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_TRANSFORM_FILE_NOT_FOUND_ZYX, String.format("%s(%s)", transformName, dlFileEntryDesc));
					}
				}
	
				// No interesa que esta información viaje al LIVE
				attrCompanyId.detach();
				attrUserId.detach();
				attrGroupId.detach();
				attrFolderId.detach();
				attrDLFEName.detach();
			}
		}

		return transformsDom;
	}
	
	/**
	 * 
	 * @param transformIds
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws ServiceError
	 * @throws PortalException
	 * @throws SystemException
	 * @throws UnsupportedEncodingException
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public void publishToLive(String transformIds) throws SecurityException, NoSuchMethodException, ServiceError, PortalException, SystemException, UnsupportedEncodingException, ClientProtocolException, IOException, DocumentException
	{
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
		{
			if (exportWriteLock.tryLock())
			{
				try
				{
					ErrorRaiser.throwIfFalse(Validator.isNotNull(transformIds), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
					
					Document transformsDom = _exportData( Arrays.asList(transformIds.split(",")) );
					
					LiveConfiguration liveConf	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(GroupMgr.getCompanyId());
					String remoteIP 			= liveConf.getRemoteIterServer2().split(":")[0];
					int remotePort 				= Integer.valueOf(liveConf.getRemoteIterServer2().split(":")[1]);
					String remoteMethodPath 	= "/base-portlet/secure/json";
					
					List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
					remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.base.service.FormTransformServiceUtil"));
					remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"importTransforms"));
					remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[transforms]"));
					remoteMethodParams.add(new BasicNameValuePair("transforms", 		transformsDom.asXML()));
					
					String result = XMLIOUtil.executeJSONRemoteMethod2(GroupMgr.getCompanyId(), remoteIP, remotePort, liveConf.getRemoteUserName(), liveConf.getRemoteUserPassword(), remoteMethodPath, remoteMethodParams);
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
				}
				finally
				{
					exportWriteLock.unlock();
				}
			}
			else
			{
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_PUBLISH_ALREADY_IN_PROCESS_ZYX);
			}
		}
	}
	
	/**
	 * 
	 * @param transforms
	 * @throws Exception 
	 */
	public Document importTransforms(String transforms) throws Exception
	{
		Document result = null;;
		
		if (importWriteLock.tryLock())
		{
			InputStream is = null;
			try
			{
				ErrorRaiser.throwIfFalse(Validator.isNotNull(transforms), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
				Document dom = SAXReaderUtil.read( transforms );
				boolean updtIfExist 	= GetterUtil.getBoolean(XMLHelper.getStringValueOf(dom.getRootElement(), "@updtIfExist", 	"true"));
				boolean isImportProcess = GetterUtil.getBoolean(XMLHelper.getStringValueOf(dom.getRootElement(), "@importProcess", 	"false"));
				
				long groupId = XMLHelper.getLongValueOf(dom.getRootElement(), "@groupId");
				
				List<Node> list 	= dom.selectNodes("/rs/row");
				
				// Se consultan los IDs de los grupos para los correspondientes nombres
				if (groupId <= 0)
				{
					String[] grpNameList= XMLHelper.getStringValues(list, "@groupName");
					Document groupDom = PortalLocalServiceUtil.executeQueryAsDom( String.format("SELECT groupid, name groupName FROM group_ WHERE name IN ('%s')", StringUtil.merge(grpNameList, "','")) );

					groupId = XMLHelper.getLongValueOf(groupDom, String.format("/rs/row[@groupName = '%s']/@groupid", grpNameList[0]));
					ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALID_GROUP_NAME_ZYX);
				}
				
				// Se consulta si las transformaciones ya existen en el entorno
				Document existDom = PortalLocalServiceUtil.executeQueryAsDom( String.format("SELECT transformid, transformname, xsluuid FROM formxsltransform WHERE groupid = %d", groupId) );
				
				for (int i = 0; i < list.size(); i++)
				{
					Element elem2Imp 	= (Element)list.get(i);
					
					// http://jira.protecmedia.com:8080/browse/ITER-1347?focusedCommentId=63111&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-63111
					// Si es una publicación el duplicado se verifica por ID (transformid), si es una importación se realiza por nombre (transformname).
					String transformKey = isImportProcess 							?
											elem2Imp.attributeValue("transformname"):
											elem2Imp.attributeValue("transformid")	;
					
					String xpathExist 	= String.format(isImportProcess 			? 
											"/rs/row[@transformname='%s']"			:
											"/rs/row[@transformid='%s']", transformKey);	
											
					Node oldTransform   = existDom.selectSingleNode(xpathExist);
					boolean exist = (oldTransform != null);
					
					ErrorRaiser.throwIfFalse( updtIfExist || !exist, IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, 
		 					String.format("%s(%s)", IterAdmin.IA_CLASS_TRANSFORM, transformKey));

					elem2Imp.addAttribute("groupid", String.valueOf(groupId));
					
					// En una creación o una publicación al LIVE puede interesar mantener el UUID origen
					// Es un proceso de importación:
					//  - Ya existe, se sustituye el UUID por el UUID que tenga el mismo nombre
					//  - No existe con ese nombre en el LIVE, se borra el UUID para que genere uno nuevo
					if (isImportProcess)
						elem2Imp.addAttribute("transformid", (exist) ? ((Element)oldTransform).attributeValue("transformid") : SQLQueries.getUUID());

					// Se actualiza el binario
					String binData	= XMLHelper.getTextValueOf(elem2Imp, "bin/text()");
					if (Validator.isNotNull(binData))
					{
						long userId 	= GroupMgr.getDefaultUserId();
						String fileName	= XMLHelper.getTextValueOf(elem2Imp, "@dlFileEntryDesc");
						long fileSize	= XMLHelper.getLongValueOf(elem2Imp, "@size_");
						is = new ByteArrayInputStream(Base64.decodeBase64(binData));
						
						if (exist)
							elem2Imp.addAttribute("xsluuid", ((Element)oldTransform).attributeValue("xsluuid"));

						uploadXslFileEntry(userId, elem2Imp, fileName, fileSize, is);
					}
					
					// Se actualizan o crean los datos
					if (exist)
						editTransform(elem2Imp); 
					else 
						addTransform(elem2Imp);
				}
				result = exportData(groupId);
			}
			finally
			{
				importWriteLock.unlock();
				
				if (is != null)
					is.close();
			}
		}
		else
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_PUBLISH_ALREADY_IN_PROCESS_ZYX);
		}
		
		return result;
	}
	
	
	/**************************************************************************************************************/
	/********************				GESTION RSS												 ******************/
	/**************************************************************************************************************/
	private static final String GET_XSL_TRANSFORMS_RSS = 
			new StringBuilder( "SELECT  transformid, transformname, transformdesc, xsluuid, (d.description) xslname" )
					  .append( " FROM formxsltransform f" )
					  .append( " INNER JOIN sectionproperties s ON s.autorssxsl = f.transformid" )
					  .append( " LEFT JOIN DLFileEntry d ON (f.xsluuid = d.uuid_ AND d.groupId = f.groupid)" )
					  .append( " WHERE f.groupId = %s AND s.plid = %s ORDER BY transformname ASC" ).toString();	
	
	private static final String UPDATE_XSL_TRANSFORMS_RSS = 
			new StringBuilder( "UPDATE sectionproperties SET autorssxsl='%s' ")
					  .append( " WHERE plid = %s" ).toString();
	
	private static final String DELETE_XSL_TRANSFORMS_RSS = 
			new StringBuilder( "UPDATE sectionproperties SET autorssxsl=NULL ")
					  .append( " WHERE plid = %s" ).toString();
	
	public String getTransformsToSection( long groupId, long plid ) throws ServiceError, NoSuchMethodException
	{
		String result = "";
		
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		result = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_XSL_TRANSFORMS_RSS, groupId, plid), new String[]{"transformdesc"} ).asXML();
		
		return result;
	}
	
	public String addTransformToSection( String xmlData  ) throws DocumentException, ServiceError, SQLException, NoSuchMethodException, IOException
	{
		String result = "";
		
		Node node = SAXReaderUtil.read(xmlData).selectSingleNode("/rs/row");
		
		String plid = XMLHelper.getTextValueOf(node, "@plid");
		ErrorRaiser.throwIfNull(plid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		plid = StringEscapeUtils.escapeSql(plid);
		
		result = addTransform( node );
		if(result!=null)
		{
			Node transf = SAXReaderUtil.read(result).selectSingleNode("/rs/row");
			String autorssxsl = XMLHelper.getTextValueOf(transf, "@transformid");
			ErrorRaiser.throwIfNull(autorssxsl, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			autorssxsl = StringEscapeUtils.escapeSql(autorssxsl);
			
			PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_XSL_TRANSFORMS_RSS, autorssxsl, plid));
		}
	
		return result;	
	}
	
	public String addTransformToSectionToList( String autorssxsl, String plid ) throws DocumentException, ServiceError, SQLException, NoSuchMethodException, IOException
	{
		PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_XSL_TRANSFORMS_RSS, autorssxsl, plid));
		return PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_XSL_TRANSFORM_BY_TRANSFORMID, autorssxsl), new String[]{"transformdesc"} ).asXML();
	}
	
	public String deleteTransformToSectionToList( String plid ) throws DocumentException, ServiceError, SQLException, NoSuchMethodException, IOException
	{
		String result = "";	
		PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_XSL_TRANSFORMS_RSS, plid));
		return result;	
	}
	
	public String editTransformToSection( String xmlData ) throws DocumentException, ServiceError, SQLException, NoSuchMethodException, IOException
	{
		String result = "";
		
		Node node = SAXReaderUtil.read(xmlData).selectSingleNode("/rs/row");
		
		String plid = XMLHelper.getTextValueOf(node, "@plid");
		ErrorRaiser.throwIfNull(plid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		plid = StringEscapeUtils.escapeSql(plid);
		
		result = editTransform( node );
		if(result!=null)
		{
			Node transf = SAXReaderUtil.read(result).selectSingleNode("/rs/row");
			String autorssxsl = XMLHelper.getTextValueOf(transf, "@transformid");
			ErrorRaiser.throwIfNull(autorssxsl, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			autorssxsl = StringEscapeUtils.escapeSql(autorssxsl);
			
			PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_XSL_TRANSFORMS_RSS, autorssxsl, plid));
		}
	
		return result;
	}
	
	/**********************************************************************************/
	private static final String GET_ALL_RSS_ADVANCEDS = new StringBuilder( "SELECT  rs.advancedrssid, rs.urlrss, rs.autorssxsl, f.transformname, ")
															.append( " rs.namerss, rs.titlehtml, rs.description, rs.urlweb,")
															.append( " rs.selsections, rs.imageframe, rs.orderrss")
															.append( " FROM  rssadvancedproperties rs" )
															.append( " INNER JOIN formxsltransform f ON rs.autorssxsl = f.transformid" )
															.append( " WHERE rs.groupId = %s ORDER BY urlrss ASC" ).toString();
	
	private static final String GET_RSS_ADVANCED = new StringBuilder( "SELECT  rs.advancedrssid, rs.urlrss, rs.autorssxsl, f.transformname, ")
															.append( " rs.namerss, rs.titlehtml, rs.description, rs.urlweb, ")
															.append( " rs.selsections, rs.imageframe, rs.orderrss")
															.append( " FROM  rssadvancedproperties rs")
															.append( " INNER JOIN formxsltransform f ON rs.autorssxsl = f.transformid")
															.append( " WHERE rs.advancedrssid = '%s'").toString();
	
	private static final String INSERT_RSS_ADVANCED = new StringBuilder( "INSERT INTO rssadvancedproperties(advancedrssid, urlrss, groupid, autorssxsl, ")
															.append( " namerss, titlehtml, description, urlweb, selsections, imageframe, orderrss)")
															.append( " VALUES ('%s', '%s', %s, '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')").toString();
															
	private static final String UPDATE_RSS_ADVANCED = new StringBuilder( "UPDATE rssadvancedproperties ")
															.append( " SET urlrss='%s', autorssxsl='%s', ")
															.append( " namerss='%s', titlehtml='%s', description='%s', urlweb='%s',")
															.append( " selsections='%s', imageframe='%s', orderrss='%s'")
															.append( " WHERE advancedrssid = '%s'").toString();
	
	private static final String DELETE_RSS_ADVANCED = new StringBuilder( "DELETE FROM rssadvancedproperties WHERE advancedrssid = '%s'").toString();
	private static final String GET_IMAGEFRAME = new StringBuffer("SELECT imageframeid, NAME FROM imageframe WHERE groupid = %s").toString();

	/******************************************************************************************************************************************************/
	
	public String getRssAdvancedProperties( long groupId) throws ServiceError, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document dom = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_ALL_RSS_ADVANCEDS, groupId));
		
		List<Element> rssAdvs = dom.getRootElement().elements("row");
		for (Element rss : rssAdvs)
		{
			String sections = rss.valueOf("@selsections");
			rss.remove(rss.attribute("selsections"));
			
			if (Validator.isNotNull(sections)){
				Element selsections = rss.addElement("selsections");
				selsections.addCDATA(sections);	
			}
			
			String orderss = rss.valueOf("@orderrss");
			rss.remove(rss.attribute("orderrss"));
			
			if (Validator.isNotNull(sections)){
				Element orders = rss.addElement("orderrss");
				orders.addCDATA(orderss);	
			}
		}
		return dom.asXML();
	}
	
	public String getRssAdvanced(String uuid) throws ServiceError, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(uuid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document dom = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_RSS_ADVANCED, uuid));
		
		List<Element> rssAdvs = dom.getRootElement().elements("row");
		for (Element rss : rssAdvs)
		{
			String sections = rss.valueOf("@selsections");
			rss.remove(rss.attribute("selsections"));
			
			if (Validator.isNotNull(sections)){
				Element selsections = rss.addElement("selsections");
				selsections.addCDATA(sections);	
			}
			
			String orderss = rss.valueOf("@orderrss");
			rss.remove(rss.attribute("orderrss"));
			
			if (Validator.isNotNull(sections)){
				Element orders = rss.addElement("orderrss");
				orders.addCDATA(orderss);	
			}
		}
		return dom.asXML();
	}
	
	public String addRssAdvanced(String xmlData) throws ServiceError, DocumentException, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String uuid = XMLHelper.getTextValueOf(dataRoot, "@advancedrssid");
		if (Validator.isNull(uuid))
			uuid = SQLQueries.getUUID();
		
		String urlrss = XMLHelper.getTextValueOf(dataRoot, "@urlrss");
		ErrorRaiser.throwIfNull(urlrss, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		urlrss = StringEscapeUtils.escapeSql(urlrss);
		
		String groupid = XMLHelper.getTextValueOf(dataRoot, "@groupid");
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		String autorssxsl = XMLHelper.getTextValueOf(dataRoot, "@autorssxsl");
		ErrorRaiser.throwIfNull(autorssxsl, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String namerss = XMLHelper.getTextValueOf(dataRoot, "@namerss");
		ErrorRaiser.throwIfNull(namerss, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String titlehtml = XMLHelper.getTextValueOf(dataRoot, "@titlehtml");
		ErrorRaiser.throwIfNull(titlehtml, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String description = XMLHelper.getTextValueOf(dataRoot, "@description");
		ErrorRaiser.throwIfNull(description, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String urlweb = XMLHelper.getTextValueOf(dataRoot, "@urlweb");
		ErrorRaiser.throwIfNull(urlweb, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		//imageframe puede ser nulo
		String imageframe = XMLHelper.getTextValueOf(dataRoot, "@imageframe");

		XPath xpathArticles = SAXReaderUtil.createXPath("//selsections");
		Node nodeArticles = xpathArticles.selectSingleNode(dataRoot);
		
		String articles = StringEscapeUtils.escapeSql(nodeArticles.asXML().toString());
		
		XPath xpathOrder = SAXReaderUtil.createXPath("//orderrss");
		Node nodeorder = xpathOrder.selectSingleNode(dataRoot);
		
		String order = StringEscapeUtils.escapeSql(nodeorder.asXML().toString());
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(INSERT_RSS_ADVANCED, uuid, urlrss, groupid, autorssxsl, namerss, titlehtml, 
				description, urlweb, articles, imageframe, order));
		
		return getRssAdvanced(uuid);
	}
	
	public String updateRssAdvanced(String xmlData) throws ServiceError, DocumentException, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String advancedrssid = XMLHelper.getTextValueOf(dataRoot, "@advancedrssid");
		ErrorRaiser.throwIfNull(advancedrssid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		advancedrssid = StringEscapeUtils.escapeSql(advancedrssid);
		
		String urlrss = XMLHelper.getTextValueOf(dataRoot, "@urlrss");
		ErrorRaiser.throwIfNull(urlrss, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		urlrss = StringEscapeUtils.escapeSql(urlrss);
		
		String groupid = XMLHelper.getTextValueOf(dataRoot, "@groupid");
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		String autorssxsl = XMLHelper.getTextValueOf(dataRoot, "@autorssxsl");
		ErrorRaiser.throwIfNull(autorssxsl, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		//imageframe puede ser nulo
		String imageframe = XMLHelper.getTextValueOf(dataRoot, "@imageframe");
		
		String namerss = XMLHelper.getTextValueOf(dataRoot, "@namerss");
		ErrorRaiser.throwIfNull(namerss, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String titlehtml = XMLHelper.getTextValueOf(dataRoot, "@titlehtml");
		ErrorRaiser.throwIfNull(titlehtml, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String description = XMLHelper.getTextValueOf(dataRoot, "@description");
		ErrorRaiser.throwIfNull(description, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String urlweb = XMLHelper.getTextValueOf(dataRoot, "@urlweb");
		ErrorRaiser.throwIfNull(urlweb, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		XPath xpathArticles = SAXReaderUtil.createXPath("//selsections");
		Node nodeArticles = xpathArticles.selectSingleNode(dataRoot);
		
		String articles = StringEscapeUtils.escapeSql(nodeArticles.asXML().toString());
		
		XPath xpathOrder = SAXReaderUtil.createXPath("//orderrss");
		Node nodeorder = xpathOrder.selectSingleNode(dataRoot);
		
		String order = StringEscapeUtils.escapeSql(nodeorder.asXML().toString());
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_RSS_ADVANCED, urlrss, autorssxsl, namerss, titlehtml, 
				description, urlweb, articles, imageframe, order, advancedrssid));
		
		return getRssAdvanced(advancedrssid);
	}
	
	public String deleteRssAdvanced( String xmlData ) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{	
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row/@advancedrssid");
		
		List<Node> nodes = xpath.selectNodes(dataRoot);
		ErrorRaiser.throwIfFalse((nodes != null && nodes.size() > 0), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		for(Node node:nodes)
		{
			String advancedrssid = node.getStringValue();
			PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_RSS_ADVANCED, advancedrssid));
		}
		return xmlData;
	}
	
	public String getImageFrame(long groupId) throws ServiceError, NoSuchMethodException
	{
		String result = "";
		
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		result = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_IMAGEFRAME, groupId), new String[]{"imageframe"} ).asXML();
		
		return result;
	}
	
}