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

package com.protecmedia.iter.user.service.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.DLFileEntryMgrLocalServiceUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.user.service.base.FileFormReceivedMgrLocalServiceBaseImpl;

/**
 * The implementation of the file form received mgr local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.user.service.FileFormReceivedMgrLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.user.service.FileFormReceivedMgrLocalServiceUtil} to access the file form received mgr local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see com.protecmedia.iter.user.service.base.FileFormReceivedMgrLocalServiceBaseImpl
 * @see com.protecmedia.iter.user.service.FileFormReceivedMgrLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class FileFormReceivedMgrLocalServiceImpl
	extends FileFormReceivedMgrLocalServiceBaseImpl {
	
	private static Log _log = LogFactoryUtil.getLog(FileFormReceivedMgrLocalServiceImpl.class);
	private static final MimetypesFileTypeMap MIMETYPES = new MimetypesFileTypeMap();
	
	private static final String GET_INFO_URI_FILE = new StringBuffer()
		.append("SELECT dlfe.groupId groupid, dlfe.folderid folderid, dlfe.title title \n")
		.append("FROM fieldreceived fr \n")
		.append("INNER JOIN dlfileentry dlfe ON fr.binfieldvalueid = dlfe.fileEntryId \n")
		.append("where fr.fieldreceivedid = '%s'")
		.toString();	
	
	private static final String GET_INFO_URI_FILE_FIELD = new StringBuffer()
	.append("SELECT dlfe.groupId groupid, dlfe.folderid folderid, dlfe.title title \n")
	.append("FROM fieldreceived fr \n")
	.append("INNER JOIN dlfileentry dlfe ON fr.binfieldvalueid = dlfe.fileEntryId \n")
	.append("WHERE fr.formreceivedid = '%s' AND fr.fieldid='%s'")
	.toString();
	
	private static final String[] COLUMNS_DLFE_CDATA = new String[]{"title"};
	
	
	public Object getFile(HttpServletRequest request, HttpServletResponse response, String formFieldReceiverId)
	{//para acceder: http://HOST/base-portlet/urlendpoint?clsid=com.protecmedia.iter.user.service.FileFormReceivedMgrServiceUtil&dispid=0&methodName=getFile&instanceID=null&p1=NUM_FORMRECEIVEDID
		try {
			doGetFile(request, response, formFieldReceiverId);
			_log.debug(new StringBuffer().append("The FileFormReceivedMgrService response content-type: ").append(response.getContentType()).toString());
		} catch (NoSuchMethodException e) {
			_log.debug(e.getStackTrace());
			response.setStatus(500);
		} catch (SecurityException e) {
			_log.debug(e.getStackTrace());
			response.setStatus(500);
		} catch (ServiceError e) {
			_log.debug(e.getStackTrace());
			response.setStatus(500);
		} catch (URISyntaxException e) {
			_log.debug(e.getStackTrace());
			response.setStatus(500);
		} catch (IOException e) {
			_log.debug(e.getStackTrace());
			response.setStatus(500);
		}
		return null;
	}
	
	public static void doGetFile(HttpServletRequest request, HttpServletResponse response, String formFieldReceiverId) throws NoSuchMethodException, SecurityException, ServiceError, URISyntaxException, IOException{
		URL urlFile = getURLFileReceived(formFieldReceiverId, request);
			
		response.setContentType(MIMETYPES.getContentType(urlFile.getPath()));
		response.sendRedirect(urlFile.toString());
	}
	
	public static URL getURLFileReceived(String formFieldReceiverId, HttpServletRequest request) throws NoSuchMethodException, SecurityException, ServiceError, MalformedURLException, URISyntaxException{
		Document domDLFileEntryInfo;
		Long group;
		Long folder;
		String title;
		
		String requestURL		= request.getRequestURL().toString();
		URL urlHost				= new URL( requestURL.substring(0, requestURL.indexOf(request.getRequestURI())) );
		boolean secure 			= request.isSecure();
		
		// Comprueba el header X-Forwarded-Proto
		String protocolFromHeader = request.getHeader("X-Forwarded-Proto");
		// Será seguro si se indica https en el header. Si no, se usará el valor del request.
		secure = secure || Http.HTTPS.equalsIgnoreCase(protocolFromHeader);
		
		//obtain all necessary data of handlers
		_log.trace("obtain FileEntry path info");
		_log.debug(new StringBuffer( "Query: ").append(String.format(GET_INFO_URI_FILE, formFieldReceiverId)));
		domDLFileEntryInfo = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_INFO_URI_FILE, formFieldReceiverId),COLUMNS_DLFE_CDATA);
		ErrorRaiser.throwIfNull(domDLFileEntryInfo);
		ErrorRaiser.throwIfNull(group =  XMLHelper.getLongValueOf(domDLFileEntryInfo, "/rs/row/@groupid"));
		ErrorRaiser.throwIfNull(folder = XMLHelper.getLongValueOf(domDLFileEntryInfo, "/rs/row/@folderid"));
		ErrorRaiser.throwIfNull(title =  XMLHelper.getTextValueOf(domDLFileEntryInfo, "/rs/row/title"));
		
		String file = DLFileEntryMgrLocalServiceUtil.getDLFileEntryURL(group, folder, title).concat(PropsValues.IS_PREVIEW_ENVIRONMENT ? "?env=preview" : "?env=live");
		URL url 	= new URL(HttpUtil.getProtocol(secure), urlHost.getHost(), urlHost.getPort(), file);
		
		_log.trace(new StringBuffer().append("url file redirect: ").append(url.toString()));
		return url;
	}
	
	
	
	public static URL getURLFileReceived(String formreceivedid, String fieldid, HttpServletRequest request) throws NoSuchMethodException, SecurityException, ServiceError, MalformedURLException, URISyntaxException
	{
		Document domDLFileEntryInfo;
		Long group;
		Long folder;
		String title;
		
		String requestURL		= request.getRequestURL().toString();
		URL urlHost				= new URL( requestURL.substring(0, requestURL.indexOf(request.getRequestURI())) );
		boolean secure 			= request.isSecure();

		// Comprueba el header X-Forwarded-Proto
		String protocolFromHeader = request.getHeader("X-Forwarded-Proto");
		// Será seguro si se indica https en el header. Si no, se usará el valor del request.
		secure = secure || Http.HTTPS.equalsIgnoreCase(protocolFromHeader);
		
		//obtain all necessary data of handlers
		_log.trace("obtain FileEntry path info");
		_log.debug(new StringBuffer( "Query: ").append(String.format(GET_INFO_URI_FILE_FIELD, formreceivedid, fieldid)));
		domDLFileEntryInfo = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_INFO_URI_FILE_FIELD,  formreceivedid, fieldid),COLUMNS_DLFE_CDATA);
		ErrorRaiser.throwIfNull(domDLFileEntryInfo);
		ErrorRaiser.throwIfNull(group =  XMLHelper.getLongValueOf(domDLFileEntryInfo, "/rs/row/@groupid"));
		ErrorRaiser.throwIfNull(folder = XMLHelper.getLongValueOf(domDLFileEntryInfo, "/rs/row/@folderid"));
		ErrorRaiser.throwIfNull(title =  XMLHelper.getTextValueOf(domDLFileEntryInfo, "/rs/row/title"));
		
		String file = DLFileEntryMgrLocalServiceUtil.getDLFileEntryURL(group, folder, title).concat(PropsValues.IS_PREVIEW_ENVIRONMENT ? "?env=preview" : "?env=live");;
		URL url 	= new URL(HttpUtil.getProtocol(secure), urlHost.getHost(), urlHost.getPort(), file);
		
		_log.trace(new StringBuffer("url file redirect: ").append(url.toString()));
		return url;
	}
}