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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.portlet.ReadOnlyException;
import javax.portlet.ValidatorException;

import org.apache.http.client.ClientProtocolException;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.protecmedia.iter.xmlio.service.base.WebsiteIOLocalServiceBaseImpl;
import com.protecmedia.iter.xmlio.service.util.WebsiteExportMgr;
import com.protecmedia.iter.xmlio.service.util.WebsiteImportMgr;

/**
 * The implementation of the website i o local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.service.WebsiteIOLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.xmlio.service.WebsiteIOLocalServiceUtil} to access the website i o local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see http://10.15.20.59:8090/x/q4YFAg
 * @see com.protecmedia.iter.xmlio.service.base.WebsiteIOLocalServiceBaseImpl
 * @see com.protecmedia.iter.xmlio.service.WebsiteIOLocalServiceUtil
 */
public class WebsiteIOLocalServiceImpl extends WebsiteIOLocalServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(WebsiteIOLocalServiceImpl.class);
	
	/** En modo <i>Debug</i> deshabilita los bloqueos **/
	private static Log _logLock = LogFactoryUtil.getLog(WebsiteIOLocalServiceImpl.class.getName() + ".Lock");
	
	private static ConcurrentMap<Long, WebsiteExportMgr> _exporting = new ConcurrentHashMap<Long, WebsiteExportMgr>();
	private static ConcurrentMap<Long, WebsiteImportMgr> _importing = new ConcurrentHashMap<Long, WebsiteImportMgr>();
	
	private static List<Long> _ongoingImportSubprocess = new ArrayList<Long>();
	
	
	synchronized static private void setOngoingImportSubprocess(long siteId) throws ServiceError
	{
		if (!_logLock.isDebugEnabled())
		{
			ErrorRaiser.throwIfFalse( !_ongoingImportSubprocess.contains(siteId), IterErrorKeys.XYZ_E_WEBSITE_IS_IN_ONGOING_IMPORT_SUBPROCESS_ZYX );
			_ongoingImportSubprocess.add(siteId);
		}
	}
	
	synchronized static private void unsetOngoingImportSubprocess(long siteId)
	{
		if (!_logLock.isDebugEnabled())
		{
			_ongoingImportSubprocess.remove(siteId);
		}
	}
	
	static private boolean isWebsiteBeingExported(long siteId)
	{
		boolean isBeingExported = _exporting.containsKey(siteId);
		return isBeingExported;
	}
	
	static private WebsiteExportMgr getWebsiteExportMgr(long siteId) throws ServiceError
	{
		WebsiteExportMgr exportMgr = _exporting.get(siteId);
		ErrorRaiser.throwIfNull( exportMgr, IterErrorKeys.XYZ_E_WEBSITE_IS_NOT_BEING_EXPORTED_ZYX );
		
		return exportMgr;
	}
	
	static private WebsiteExportMgr startExport(long siteId) throws ServiceError, PortalException, SystemException
	{
		ErrorRaiser.throwIfFalse( !isWebsiteBeingExported(siteId), IterErrorKeys.XYZ_E_WEBSITE_IS_BEING_EXPORTED_ZYX );
		
		WebsiteExportMgr exportMgr = new WebsiteExportMgr(siteId);
		_exporting.put(siteId, exportMgr);
		
		return exportMgr;
	}
	
	static private void finishExport(long siteId)
	{
		_exporting.remove(new Long(siteId));
	}
	
	static private boolean isWebsiteBeingImported(long siteId)
	{
		boolean isBeingImported = _importing.containsKey( siteId );
		return isBeingImported;
	}
	
	static private WebsiteImportMgr getWebsiteImportMgr(long siteId) throws ServiceError
	{
		WebsiteImportMgr importMgr = _importing.get(siteId);
		ErrorRaiser.throwIfNull( importMgr, IterErrorKeys.XYZ_E_WEBSITE_IS_NOT_BEING_IMPORTED_ZYX );
		
		return importMgr;
	}

	
	static private WebsiteImportMgr startImport(long siteId, String mlnstationInfo) throws ServiceError, PortalException, SystemException
	{
		ErrorRaiser.throwIfFalse( !isWebsiteBeingImported(siteId), IterErrorKeys.XYZ_E_WEBSITE_IS_BEING_IMPORTED_ZYX );
		
		WebsiteImportMgr importMgr = new WebsiteImportMgr(siteId, mlnstationInfo);
		_importing.put(siteId, importMgr);

		return importMgr;
	}
	
	static private void finishImport(long siteId)
	{
		_importing.remove(new Long(siteId));
	}

	public Document computeXPortDependencies(String objectsSpec) throws NumberFormatException, SystemException, PortalException, SecurityException, UnsupportedEncodingException, ClientProtocolException, DocumentException, ServiceError, NoSuchMethodException, IOException 
	{
		Document specDom	= SAXReaderUtil.read(objectsSpec);
		long siteId 		= XMLHelper.getLongValueOf(specDom.getRootElement(), "@siteid");
		ErrorRaiser.throwIfFalse(siteId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document exportDom	= null;
		
		WebsiteExportMgr exportMgr = startExport(siteId);
		try
		{
			// Si se está realizando una importación en el sitio, no se puede determinar las dependencias
			ErrorRaiser.throwIfFalse( !isWebsiteBeingImported(siteId), IterErrorKeys.XYZ_E_WEBSITE_IS_BEING_IMPORTED_ZYX );

			exportDom = exportMgr.computeXPortDependencies(specDom);
		}
		finally
		{
			finishExport(siteId);
		}
		
		return exportDom;
	}
	
	public Document exportObjects(String objectsSpec) throws NumberFormatException, SystemException, PortalException, SecurityException, UnsupportedEncodingException, ClientProtocolException, DocumentException, ServiceError, NoSuchMethodException, IOException 
	{
		Document specDom	= SAXReaderUtil.read(objectsSpec);
		long siteId 		= XMLHelper.getLongValueOf(specDom.getRootElement(), "@siteid");
		ErrorRaiser.throwIfFalse(siteId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document exportDom	= null;
		
		WebsiteExportMgr exportMgr = startExport(siteId);
		try
		{
			// Si se está realizando una importación en el sitio, no se puede hacer una exportación
			ErrorRaiser.throwIfFalse( !isWebsiteBeingImported(siteId), IterErrorKeys.XYZ_E_WEBSITE_IS_BEING_IMPORTED_ZYX );

			exportDom = exportMgr.exportObjects(specDom);
		}
		finally
		{
			finishExport(siteId);
		}
		
		return exportDom;
	}
	
	public void abortExport(long siteId) throws ServiceError
	{
		getWebsiteExportMgr(siteId).abortExport();
	}
	
	public void importPreProcessInfo(long siteId, String mlnstationInfo, String content) throws Throwable
	{
		// Se deja la marca que se está ejecutando un subproceso de impotación
		setOngoingImportSubprocess(siteId);
		try
		{
			// Si se está realizando una exportación en el sitio, no se puede hacer una importación
			ErrorRaiser.throwIfFalse( !isWebsiteBeingExported(siteId), IterErrorKeys.XYZ_E_WEBSITE_IS_BEING_EXPORTED_ZYX );
			
			WebsiteImportMgr importMgr = startImport(siteId, mlnstationInfo);
			try
			{
				importMgr.importPreProcessInfo(content);
			}
			catch (Throwable th)
			{
				finishImport(siteId);
				throw th;
			}
		}
		finally
		{
			// Se elimina la marca que indica que se está ejecutando un subproceso de importación
			unsetOngoingImportSubprocess(siteId);
		}
	}
	
	public void importObject(long siteId, String id, String className, String content) throws ServiceError, UnsupportedEncodingException, DocumentException, NumberFormatException, PortalException, SystemException
	{
		// Se deja la marca que se está ejecutando un subproceso de impotación
		setOngoingImportSubprocess(siteId);
		try
		{
			// Si se está realizando una exportación en el sitio, no se puede hacer una importación
			ErrorRaiser.throwIfFalse( !isWebsiteBeingExported(siteId), IterErrorKeys.XYZ_E_WEBSITE_IS_BEING_EXPORTED_ZYX );
			
			// Tiene que existir una importación en curso
			getWebsiteImportMgr(siteId).importObject(id, className, content);
		}
		finally
		{
			// Se elimina la marca que indica que se está ejecutando un subproceso de importación
			unsetOngoingImportSubprocess(siteId);
		}
	}
	
	public void importPostProcessInfo(long siteId, String content, String relationships) throws ServiceError, DocumentException, NumberFormatException, SecurityException, PortalException, SystemException, NoSuchMethodException, ReadOnlyException, ValidatorException, IOException
	{
		// Se deja la marca que se está ejecutando un subproceso de impotación
		setOngoingImportSubprocess(siteId);
		try
		{
			// Si se está realizando una exportación en el sitio, no se puede hacer una importación
			ErrorRaiser.throwIfFalse( !isWebsiteBeingExported(siteId), IterErrorKeys.XYZ_E_WEBSITE_IS_BEING_EXPORTED_ZYX );
			
			// Tiene que existir una importación en curso
			getWebsiteImportMgr(siteId).importPostProcessInfo(content, relationships);
	
			finishImport(siteId);
		}
		finally
		{
			// Se elimina la marca que indica que se está ejecutando un subproceso de importación
			unsetOngoingImportSubprocess(siteId);
		}
	}

	public void resetImport(long siteId, String mlnstationInfo) throws ServiceError
	{
		// Se deja la marca que se está ejecutando un subproceso de impotación
		setOngoingImportSubprocess(siteId);
		try
		{
			getWebsiteImportMgr(siteId).resetImport(mlnstationInfo);
			finishImport(siteId);
		}
		finally
		{
			// Se elimina la marca que indica que se está ejecutando un subproceso de importación
			unsetOngoingImportSubprocess(siteId);
		}
	}
}