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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.cache.MultiVMPoolUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.xmlio.PoolInProcessException;
import com.protecmedia.iter.xmlio.model.LiveControl;
import com.protecmedia.iter.xmlio.service.LiveControlLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LivePoolLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.base.LiveControlLocalServiceBaseImpl;
import com.protecmedia.iter.xmlio.service.util.XmlioKeys;

/**
 * The implementation of the live control local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.service.LiveControlLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.xmlio.service.LiveControlLocalServiceUtil} to access the live control local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.xmlio.service.base.LiveControlLocalServiceBaseImpl
 * @see com.protecmedia.iter.xmlio.service.LiveControlLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class LiveControlLocalServiceImpl extends LiveControlLocalServiceBaseImpl {

	private static Log _log = LogFactoryUtil.getLog(LiveControlLocalServiceImpl.class);
	
	private static final String FIELD_TO_ORDER = "startDate";
	private static final String FIELD_ORDER    = "desc";	
	
	/*
	 * Lock/Release functions
	 */	
	public String getLock(long userId, long groupId, String className, String refProcessId, String [] liveItemIds, List<String> lockError) throws PoolInProcessException, SystemException, PortalException, ServiceError
	{	
		String processId = "";
		
		try
		{
			// Si es vacío o 0 se quiere generar un nuevo processId
			if (refProcessId != null && (refProcessId.equals("0") || refProcessId.isEmpty()))
				refProcessId = null;
				
			//Check that there is no masive publication in progress.
			if (!masivePublicationInProcess(groupId))
			{
				//Vamos a comprobar el tipo de publicación para bloquear la tabla de publicación		
				if (className != null && !className.equals("") && liveItemIds != null && liveItemIds.length != 0)
				{
					//PARTIAL export					
					long id = counterLocalService.increment();
					LiveControl liveControl = liveControlPersistence.create(id);					
					
					if (refProcessId == null)
						processId = String.valueOf(counterLocalService.increment());
					else 
						processId = refProcessId;
					
					liveControl.setProcessId(processId);					
					
					//TODO: Bloquear subprocesos
					liveControl.setSubprocessId("0");
					liveControl.setType(IterKeys.PARTIAL);
					liveControl.setStatus(IterKeys.PROCESSING);				
					liveControl.setStartDate(new Date());
					liveControl.setUserId(userId);
					liveControl.setGroupId(groupId);
					liveControlPersistence.update(liveControl, false);
									
					// Add processId to all the pools. Comprueba que todos los elementos pertenezcan al mismo pool 
					// y que ninguno de dichos elementos esté bloqueado por otro processId
					long totalOperations = LivePoolLocalServiceUtil.updateAllPoolProcessId(processId, liveItemIds);
		
					_log.info("Publish to Live Control locked");
					
					// Operaciones totales		
					LiveControlLocalServiceUtil.updateLiveControlOperations(processId, totalOperations);
					_log.info("Process: " + processId + ", Operations to be performed " + totalOperations);
				}
				else if (className == null || className.equals(""))
				{
					// Default: MASIVE export	
					// Comprobar que no se está publicando ningún elemento del grupo.
					ErrorRaiser.throwIfFalse( !publicationInProcessByGroup(groupId), IterErrorKeys.XYZ_E_PUBLISH_SAME_GRP_ALREADY_IN_PROC_ZYX);
					
					// Comprobar que no se está publicando ningún elemento del grupo global.
					long globalGroupId = CompanyLocalServiceUtil.getCompanyById( GroupLocalServiceUtil.getGroup(groupId).getCompanyId() ).getGroup().getGroupId();
					ErrorRaiser.throwIfFalse( !publicationInProcessByGroup(globalGroupId), IterErrorKeys.XYZ_E_PUBLISH_GLOBAL_GRP_ALREADY_IN_PROC_ZYX);
					
					LiveControl liveControl = liveControlPersistence.create( counterLocalService.increment() );					
					
					if (refProcessId == null)
						processId = String.valueOf(counterLocalService.increment());
					else
						processId = refProcessId;
					
					liveControl.setProcessId(processId);	
					
					liveControl.setSubprocessId("0");
					liveControl.setType(IterKeys.MASIVE);
					liveControl.setStatus(IterKeys.PROCESSING);				
					liveControl.setStartDate(new Date());
					liveControl.setUserId(userId);
					liveControl.setGroupId(groupId);
					liveControlPersistence.update(liveControl, false);
					
					_log.info("Publish to Live Control locked");				
					
					// Operaciones totales			
					long totalOperations = LiveLocalServiceUtil.countByStatus(globalGroupId, groupId, IterKeys.PENDING);
					totalOperations += LiveLocalServiceUtil.countByStatus(globalGroupId, groupId, IterKeys.ERROR);
					LiveControlLocalServiceUtil.updateLiveControlOperations(processId, totalOperations);
					_log.info("Process: " + processId + ", Operations to be performed " + totalOperations);
				}
				else
				{
					ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_XPORTCONTENT_EMPTY_ZYX, className);				
				}
			}
		}
		catch (Exception e)
		{
			_log.error("Error locking operations: " + e.toString());
			lockError.add( ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_PUBLISH_LOCK_FAILED_ZYX, e) );
		}
		return processId;
	}
	
	public void releaseLock(String processId) throws SystemException, IOException, SQLException
	{
		LiveControl liveControl = getLiveControlInProcess(processId);
		
		// Se desbloquean los pools
		LivePoolLocalServiceUtil.releaseLockByProcessId(processId);	
	
		if (liveControl != null)
		{
			if (liveControl.getStatus().equals(IterKeys.PROCESSING)) 
				liveControl.setStatus(IterKeys.DONE);
			
			liveControl.setEndDate(new Date());
			
			liveControlPersistence.update(liveControl, false);
		}
		
	}
	
	/*
	 * Update Functions
	 */
	public void updateLiveControlFileSize(String processId, long fileSize) {
		try{	
			//TODO: Tener en cuenta subprocesos
			LiveControl liveControl = getLiveControlInProcess(processId);
			if(liveControl!=null){
				liveControl.setFileSize(fileSize);
				liveControlPersistence.update(liveControl, false);
			}
		}
		catch(Exception err){
			_log.error(err);
		}
	}
	
	public void updateLiveControlOperations(String processId, long operations){
		try{		
			//TODO: Tener en cuenta subprocesos
			LiveControl liveControl = getLiveControlInProcess(processId);
			liveControl.setOperations(operations);
			liveControlPersistence.update(liveControl, false);
		}
		catch(Exception err){
			_log.error(err);
		}
	}
	
	public void updateLiveControlErrors(String processId, long totalErrors, List<String> errors)
	{
		try
		{		
			//TODO: Tener en cuenta subprocesos
			LiveControl liveControl = getLiveControlInProcess(processId);
			if (liveControl != null)
			{
				// Si hay errores se marca como errónea la operación
				if (errors.size() > 0)
					liveControl.setStatus(IterKeys.ERROR);
				
				liveControl.setErrors(totalErrors);
				liveControl.setErrorLog( errors.size()>0 ? errors.get(0) : "" );
				liveControlPersistence.update(liveControl, false);
			}
		}
		catch(Exception err){
			_log.error(err);
		}
	}
	
	public LiveControl updateLiveControlErrors(String processId, long totalErrors, String errorLog) throws SystemException, ServiceError
	{
		//TODO: Tener en cuenta subprocesos
		LiveControl liveControl = getLiveControlInProcess(processId);
		ErrorRaiser.throwIfNull(liveControl, IterErrorKeys.XYZ_E_CONTROL_IN_PROCESS_NOT_EXIST_ZYX, processId);
		
		// Si hay errores se marca como errónea la operación
		if (!errorLog.isEmpty())
			liveControl.setStatus(IterKeys.ERROR);
		
		if( liveControl.getErrors()==0 )
			liveControl.setErrors(totalErrors);
		if(liveControl.getErrorLog().equals(""))
			liveControl.setErrorLog( errorLog );
		
		liveControlPersistence.update(liveControl, false);
		
		return liveControl;
	}

	
	/*
	 * Delete Functions
	 */
	public void clearLiveControl() throws Exception
	{
		// Las operaciones bloqueadas serán aquellas que NO tengan fecha de finalización, sean Processing, Done, o Error
		liveControlPersistence.removeByEndDateIsNot(null);
		_log.info("Live Control log cleaned");
	}
	
	public void unlockAllLiveControl() throws Exception
	{	
		// Primero buscamos por los que no tienen fecha de fin
		List<LiveControl> liveControlList = liveControlPersistence.findByEndDate(null);
		for (LiveControl liveControl : liveControlList)
			unlockLiveControl(liveControl.getProcessId());
		
		// Buscamos los que estén en estado processing (es por seguridad, deberían de haber estado en el cojunto anterior)
		liveControlList = liveControlPersistence.findByStatus(IterKeys.PROCESSING);
		for (LiveControl liveControl : liveControlList)
			unlockLiveControl(liveControl.getProcessId());
		
		// Se actualizan a null aquellos pool que después de este proceso siguen con un processID, esto será pq existen pool 
		// bloqueados por procesos que no existen ya en la tabla LifeControl (no debería pero de existir se desbloquean).
		LivePoolLocalServiceUtil.cleanAllProcessId();
	}
	
	public void unlockLiveControl(String processId) throws Exception
	{
		// TODO: Tener en cuenta subprocesos
		String error = "xmlio-live-control-unlock-by-user";	
		
		LiveControl liveControl = getLiveControlInProcess(processId);
		ErrorRaiser.throwIfNull(liveControl, IterErrorKeys.XYZ_E_CONTROL_IN_PROCESS_NOT_EXIST_ZYX, processId);
		
		if (liveControl.getStatus().equals(IterKeys.PROCESSING))
		{
			//Desbloqueamos los items de Live
			LiveLocalServiceUtil.updateStatus(IterKeys.PROCESSING, IterKeys.PENDING, processId);
			
			//Desbloqueamos los pools
			LivePoolLocalServiceUtil.releaseLockByProcessId(processId);				
			
			//Desbloqueamos la entrada de control					
			liveControl = updateLiveControlErrors(processId, 0L, error);			
			liveControl.setEndDate(new Date());
			
			liveControl.setStatus(IterKeys.INTERRUPT);			
			liveControlPersistence.update(liveControl, false);
		}		
		
	}
	
	/*
	 * Getters
	 */
	public List<LiveControl> getAllLiveControl() throws SystemException
	{
		return liveControlPersistence.findAll();
	}
	
	public List<LiveControl> getAllLiveControlByStatus(String status){
		try{
			//TODO: Tener en cuenta subprocesos
			List<LiveControl> _results = liveControlPersistence.findByStatus(status);
			List<LiveControl> results = new ArrayList<LiveControl>();				
			results.addAll(_results);			
			return results;
		}
		catch(Exception err){
			return new ArrayList<LiveControl>();
		}
	}
	
	private boolean masivePublicationInProcess(long groupId) throws SystemException
	{
		List<LiveControl> llc = liveControlPersistence.findByGroupTypeStatus(groupId, IterKeys.MASIVE, IterKeys.PROCESSING);
		return llc.size() > 0;
	}	
	
	public boolean publicationInProcessByGroup(long groupId) throws SystemException
	{
		List<LiveControl> llc = liveControlPersistence.findByGroupIdStatus(groupId, IterKeys.PROCESSING);
		return llc.size() > 0;
	}
	
	public LiveControl getLiveControlInProcess(String processId) throws SystemException{
		return liveControlPersistence.fetchByProcessId(processId);
	}
	
	
	
	
	/*************************************************************************
		Gestion de Live desde IterAdmin	
	*************************************************************************/
	
	// Obtiene el campo y ordenacion por la que ordenar
	private String getSqlSort(String flexSort){
		_log.trace("In getSqlSort");			
		
		StringBuffer sqlSort = new StringBuffer("");

		if (Validator.isNotNull(flexSort))
		{
			/* Llega una cadena como esta:
			columnid=totalOk asc=0 */
			
			// Obtenemos la columna 
			final String column = flexSort.split(" ")[0].split("=")[1];
			// Obtenemos el sentido
			final String auxOrder = flexSort.split(" ")[1].split("=")[1];				
			final String order = auxOrder.equals("1") ? "asc" : "desc";				
			
			sqlSort.append(column + " " + order);
		}
		
		// Utilizamos la ordenacion por defecto
		else
		{
			sqlSort.append(FIELD_TO_ORDER + " " + FIELD_ORDER);
		}
		
		return sqlSort.toString();
	}
	
	//Consulta para Historial Total
	private final String GET_ALL_RECORDS = new StringBuffer()
		.append("SELECT id_ id, g.name gn, processId pi, xl.type_ t, status s, startDate sd, endDate ed, fileSize fs, operations o, errors e, u.screenName un, SUBSTRING(errorLog, 1, 100) el \n")
		.append("FROM xmlio_livecontrol xl                                                                       \n")
		.append("INNER JOIN group_ g ON (g.groupId = xl.groupId)                               		   			 \n")
		.append("INNER JOIN user_ u ON (u.userId = xl.userId)                               		   			 \n")
		// Filtros
		.append("%s")																								
		.append("ORDER BY %s    																       			 \n")
		.append("LIMIT %s, %s").toString();
	
	//Historial Total
	public String getAllRecordsFlex(String xmlFilters, String startIn, String limit, String sort) 
					throws ServiceError, SecurityException, NoSuchMethodException, DocumentException
	{
		_log.debug("In getAllRecordsFlex");
			
		final String sql = String.format(GET_ALL_RECORDS, 
				                         SQLQueries.buildFilters(xmlFilters, false),
								         getSqlSort(sort),                             
							         	 (Validator.isNull(startIn) ? "0"                            : StringEscapeUtils.escapeSql(startIn)),				                             
			                             (Validator.isNull(limit)   ? XmlioKeys.DEFAULT_NUMBER_LIMIT : StringEscapeUtils.escapeSql(limit))
			                            );
		
		final Document result = PortalLocalServiceUtil.executeQueryAsDom(sql);			
		
		return result.asXML();
	}
	
	
	/* *************************************** */
	/* Desbloquar publicación desde IterAdmin */
	/* ************************************* */
	
	//Consulta para obtener la publicación en estado 'Procesando' seleccionada desde el listado de publicaciones de IterAdmin
	private final String GET_LIVE_CONTROL_IN_PROCESS = new StringBuffer()
		.append("SELECT COUNT(*) livecontrol								\n")
		.append("FROM xmlio_livecontrol										\n")
		.append("WHERE processId= '%s'").toString();
	
	//Consulta para cambiar el estado a 'Interrrumpido', de la publicación en estado 'Procesando' seleccionada desde el listado de publicaciones de IterAdmin
	private final String SET_INTERRUPT_STATUS = new StringBuffer()
		.append("UPDATE xmlio_livecontrol											\n")
		.append("SET status='interrupt', errorLog='%s', endDate='%s'				\n")
		.append("WHERE status='processing' AND processId= '%s'").toString();


	//Consulta para poner a NULL el processid de los registros de XMLio_LivePool que tengan el processid de la publicación seleccionada
	private final String SET_LIVEPOOL_PROCESSID = new StringBuffer()
		.append("UPDATE xmlio_livepool									\n")
		.append("SET processId=NULL 									\n")
		.append("WHERE processId= '%s'").toString();
		

	public void interruptPublication(String processId) throws IOException, SQLException, ServiceError, Exception
	{
		String errorLog = "xmlio-live-control-unlock-by-user";	
		
		int count_liveCtrl = Integer.parseInt(PortalLocalServiceUtil.executeQueryAsList( String.format(GET_LIVE_CONTROL_IN_PROCESS, processId) ).get(0).toString());
		ErrorRaiser.throwIfFalse(count_liveCtrl >0, IterErrorKeys.XYZ_E_CONTROL_IN_PROCESS_NOT_EXIST_ZYX);
		
		PortalLocalServiceUtil.executeUpdateQuery( String.format(SET_INTERRUPT_STATUS, errorLog, SQLQueries.getCurrentDate(), processId ) );
		PortalLocalServiceUtil.executeUpdateQuery( String.format(SET_LIVEPOOL_PROCESSID,  processId) );
		MultiVMPoolUtil.clear(false);
	}
			
}