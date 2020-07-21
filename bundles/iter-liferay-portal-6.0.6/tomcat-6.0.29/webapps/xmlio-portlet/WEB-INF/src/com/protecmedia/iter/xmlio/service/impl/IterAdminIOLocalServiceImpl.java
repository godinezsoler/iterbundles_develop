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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.protecmedia.iter.base.service.util.IterAdmin;
import com.protecmedia.iter.xmlio.service.base.IterAdminIOLocalServiceBaseImpl;
import com.protecmedia.iter.xmlio.util.IterAdminExportMgr;
import com.protecmedia.iter.xmlio.util.IterAdminImportMgr;

/**
 * The implementation of the iter admin i o local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.service.IterAdminIOLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.xmlio.service.IterAdminIOLocalServiceUtil} to access the iter admin i o local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.xmlio.service.base.IterAdminIOLocalServiceBaseImpl
 * @see com.protecmedia.iter.xmlio.service.IterAdminIOLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class IterAdminIOLocalServiceImpl extends IterAdminIOLocalServiceBaseImpl 
{
	private static Log _log 		= LogFactoryUtil.getLog(IterAdminIOLocalServiceImpl.class);
	
	/** En modo <i>Debug</i> deshabilita los bloqueos de importación **/
	private static Log _logImpLock 	= LogFactoryUtil.getLog(IterAdminIOLocalServiceImpl.class.getName() + ".ImportLock");
	
	private static ConcurrentMap<Long, IterAdminExportMgr> _exporting = new ConcurrentHashMap<Long, IterAdminExportMgr>();
	private static ConcurrentMap<Long, IterAdminImportMgr> _importing = new ConcurrentHashMap<Long, IterAdminImportMgr>();
	
	private static List<Long> _ongoingImportSubprocess = new ArrayList<Long>();
	
	synchronized static private void setOngoingImportSubprocess(long siteId) throws ServiceError
	{
		if (!_logImpLock.isDebugEnabled())
		{
			ErrorRaiser.throwIfFalse( !_ongoingImportSubprocess.contains(siteId), IterErrorKeys.XYZ_E_ITERADMIN_IS_IN_ONGOING_IMPORT_SUBPROCESS_ZYX );
			_ongoingImportSubprocess.add(siteId);
		}
	}
	
	synchronized static private void unsetOngoingImportSubprocess(long siteId)
	{
		if (!_logImpLock.isDebugEnabled())
		{
			_ongoingImportSubprocess.remove(siteId);
		}
	}
	
	public void unsetOngoingImportSubprocessWrapper(long siteId)
	{
		unsetOngoingImportSubprocess(siteId);
	}
	
	static private boolean isIterAdminBeingExported(long siteId)
	{
		boolean isBeingExported = _exporting.containsKey(siteId);
		return isBeingExported;
	}
	
	static private IterAdminExportMgr getIterAdminExportMgr(long siteId) throws ServiceError
	{
		IterAdminExportMgr exportMgr = _exporting.get(siteId);
		ErrorRaiser.throwIfNull( exportMgr, IterErrorKeys.XYZ_E_ITERADMIN_IS_NOT_BEING_EXPORTED_ZYX );
		
		return exportMgr;
	}
	
	static private IterAdminExportMgr startExport(long siteId) throws ServiceError, PortalException, SystemException
	{
		ErrorRaiser.throwIfFalse( !isIterAdminBeingExported(siteId), IterErrorKeys.XYZ_E_ITERADMIN_IS_BEING_EXPORTED_ZYX );
		
		IterAdminExportMgr exportMgr = new IterAdminExportMgr(siteId);
		_exporting.put(siteId, exportMgr);
		
		return exportMgr;
	}
	
	public void finishExport(long siteId)
	{
		_exporting.remove(new Long(siteId));
	}

	static private boolean isIterAdminBeingImported(long siteId)
	{
		boolean isBeingImported = _importing.containsKey( siteId );
		return isBeingImported;
	}

	static private IterAdminImportMgr getIterAdminImportMgr(long siteId) throws ServiceError
	{
		IterAdminImportMgr importMgr = _importing.get(siteId);
		ErrorRaiser.throwIfNull( importMgr, IterErrorKeys.XYZ_E_ITERADMIN_IS_NOT_BEING_IMPORTED_ZYX );
		
		return importMgr;
	}

	static private IterAdminImportMgr startImport(long siteId, File pkg) throws Exception
	{
		ErrorRaiser.throwIfFalse( !isIterAdminBeingImported(siteId), IterErrorKeys.XYZ_E_ITERADMIN_IS_BEING_IMPORTED_ZYX );
		
		IterAdminImportMgr importMgr = new IterAdminImportMgr(siteId, pkg);
		_importing.put(siteId, importMgr);

		return importMgr;
	}
	
	public void finishImport(long siteId) throws ServiceError
	{
		getIterAdminImportMgr(siteId).finishImport();
		_importing.remove(new Long(siteId));
	}

	public void exportObjects(HttpServletRequest request, HttpServletResponse response, String objectsSpec) throws Exception
	{
		Document specDom	= SAXReaderUtil.read(objectsSpec);
		long siteId 		= XMLHelper.getLongValueOf(specDom.getRootElement(), "@siteid");
		ErrorRaiser.throwIfFalse(siteId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		IterAdminExportMgr exportMgr = startExport(siteId);
		try
		{
			// Si se está realizando una importación en el sitio, no se puede hacer una exportación
			ErrorRaiser.throwIfFalse( !isIterAdminBeingImported(siteId), IterErrorKeys.XYZ_E_ITERADMIN_IS_BEING_IMPORTED_ZYX );

			byte[] data = exportMgr.exportObjects(specDom);
			OutputStream output = response.getOutputStream();
			output.write( data );
			output.flush();
		}
		finally
		{
			finishExport(siteId);
		}
	}
	
	public void exportAllObjects(HttpServletRequest request, HttpServletResponse response, String siteID) throws Exception
	{
		long siteId = Long.valueOf(siteID);
		
		IterAdminExportMgr exportMgr = startExport(siteId);
		try
		{
			// Si se está realizando una importación en el sitio, no se puede hacer una exportación
			ErrorRaiser.throwIfFalse( !isIterAdminBeingImported(siteId), IterErrorKeys.XYZ_E_ITERADMIN_IS_BEING_IMPORTED_ZYX );

			byte[] data = exportMgr.exportAllObjects();
			OutputStream output = response.getOutputStream();
			output.write( data );
			output.flush();
		}
		finally
		{
			finishExport(siteId);
		}
	}

	public void abortExport(long siteId) throws ServiceError
	{
		getIterAdminExportMgr(siteId).abortExport();
	}

	public Document checkObjectsToImport(HttpServletRequest request, HttpServletResponse response, InputStream is, String siteID, boolean forceUnlock) throws Throwable
	{
		Document result = null;
		long siteId = Long.valueOf(siteID);
		
		if (forceUnlock)
			unsetOngoingImportSubprocess(siteId);
			
		// Se deja la marca que se está ejecutando un subproceso de importación
		setOngoingImportSubprocess(siteId);
		
		try
		{
			@SuppressWarnings("unchecked")
			Iterator<FileItem> files = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request).iterator();
			while (files.hasNext())
			{
				FileItem currentFile = files.next();
				if (!currentFile.isFormField())
				{
					String fileName = currentFile.getName().toLowerCase();
		    		ErrorRaiser.throwIfFalse(Validator.isNotNull(fileName) && fileName.endsWith(IterAdmin.PKG_EXTENSION), IterErrorKeys.XYZ_E_ITERADMIN_INVALID_PKG_NAME_ZYX);
	 
		    		File tmpFile = IterAdminImportMgr.createTmpPkg();
		    		currentFile.write(tmpFile);
		    		
		    		// Antes de comenzar una importación, se cancela el paquete anterior si lo hubiese
		    		try
		    		{
		    			_abortImport(siteId);
		    		}
		    		catch (ServiceError se)
		    		{
		    			// Si el error es que NO hay importación en curso, NO se relanza la excepción
		    			if (!se.getErrorCode().equals(IterErrorKeys.XYZ_E_ITERADMIN_IS_NOT_BEING_IMPORTED_ZYX))
		    				throw se;
		    		}
		    		
					IterAdminImportMgr importMgr = startImport(siteId, tmpFile);
					try
					{
						result = importMgr.checkObjectToImport();
					}
					catch (Throwable th)
					{
						finishImport(siteId);
						throw th;
					}

		    		break;
				}
			}
		}
		finally
		{
			// Se elimina la marca que indica que se está ejecutando un subproceso de importación
			unsetOngoingImportSubprocess(siteId);
		}

		return result;
	}
	
	public void importObjects(long siteId, boolean updtIfExist) throws Exception
	{
		// Se deja la marca que se está ejecutando un subproceso de impotación
		setOngoingImportSubprocess(siteId);
		try
		{
			// Si se está realizando una exportación en el sitio, no se puede hacer una importación
			ErrorRaiser.throwIfFalse( !isIterAdminBeingExported(siteId), IterErrorKeys.XYZ_E_ITERADMIN_IS_BEING_EXPORTED_ZYX );
			
			try
			{
				// Tiene que existir una importación en curso
				getIterAdminImportMgr(siteId).importObjects(updtIfExist);
			}
			finally
			{
				finishImport(siteId);
			}
		}
		finally
		{
			// Se elimina la marca que indica que se está ejecutando un subproceso de importación
			unsetOngoingImportSubprocess(siteId);
		}
	}
	
	public void abortImport(long siteId) throws ServiceError
	{
		// Se deja la marca que se está ejecutando un subproceso de impotación
		setOngoingImportSubprocess(siteId);
		try
		{
			getIterAdminImportMgr(siteId).abortImport();
			finishImport(siteId);
		}
		finally
		{
			// Se elimina la marca que indica que se está ejecutando un subproceso de importación
			unsetOngoingImportSubprocess(siteId);
		}
	}
	
	private void _abortImport(long siteId) throws ServiceError
	{
		getIterAdminImportMgr(siteId).abortImport();
		finishImport(siteId);
	}
}