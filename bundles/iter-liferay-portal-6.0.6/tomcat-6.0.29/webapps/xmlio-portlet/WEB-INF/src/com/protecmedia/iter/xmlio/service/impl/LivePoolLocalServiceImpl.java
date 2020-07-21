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
import java.util.List;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.persistence.BasePersistence;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.model.LivePool;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.base.LivePoolLocalServiceBaseImpl;

/**
 * The implementation of the live pool local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.service.LivePoolLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.xmlio.service.LivePoolLocalServiceUtil} to access the live pool local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.xmlio.service.base.LivePoolLocalServiceBaseImpl
 * @see com.protecmedia.iter.xmlio.service.LivePoolLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class LivePoolLocalServiceImpl extends LivePoolLocalServiceBaseImpl{
	
	private static Log _log = LogFactoryUtil.getLog(LivePoolLocalServiceImpl.class);
	
	private static final String UPDATE_PROCESSID_TO_NULL = new String("UPDATE Xmlio_LivePool SET Xmlio_LivePool.processId = NULL WHERE processId = %s");
		
	private static final String UPDATE_PROCESSID_BY_CHILDIDS = new StringBuffer 
			  ("-- Actualiza el processId a todos los elementos de los pools a los que pertenezcan los childIds suministrados\n").
		append("UPDATE Xmlio_LivePool\n").
		append("INNER JOIN (\n").
		append("             SELECT pool.livePoolId\n").
		append("             FROM Xmlio_LivePool pool\n").
		append("               WHERE pool.liveChildId IN (%s)\n").
		append("           ) pool2 ON (pool2.livePoolId = Xmlio_LivePool.livePoolId)\n").
		append("SET Xmlio_LivePool.processId = %s").toString();
	
	private static final String COUNT_PENDING_LOCKED_BY_PROCESSID = new StringBuffer 
			  ("-- Se obtiene el nUmero de elementos bloqueados por este ProcessId que se vayan a publicar (esten a Pending o a Error)\n").
		append("SELECT COUNT(*)\n").	  
		append("FROM Xmlio_LivePool\n").
		append("INNER JOIN Xmlio_Live ON (Xmlio_Live.id_ = Xmlio_LivePool.liveChildId)\n").
		append("  WHERE processId = %s\n").
		append("    AND (Xmlio_Live.status = '").append(IterKeys.PENDING).append("' OR Xmlio_Live.status = '").append(IterKeys.ERROR).append("')").toString();

	private static final String SEL_INEXISTENT_POOL_BY_CHILDIDS = new StringBuffer 
			("-- Devuelve los childIds que no tienen un pool asociado\n").
		append("SELECT GROUP_CONCAT(id_) ids\n").
		append("FROM\n").
		append("(\n").
		append("  SELECT Xmlio_Live.id_, Xmlio_LivePool.livePoolId\n").
		append("  FROM Xmlio_Live\n").
		append("  LEFT JOIN Xmlio_LivePool ON (Xmlio_Live.id_ = Xmlio_LivePool.liveChildId)\n").
		append("    WHERE Xmlio_Live.id_ IN (%s)\n").
		append(") pools\n").
		append("  WHERE livePoolId IS NULL").toString();

	private static final String SEL_LOCKED_IDS_BY_CHILDIDS = new StringBuffer 
			("-- Se buscan los Xmlio_LivePool.id_ cuyo pool este bloqueado\n").
		append("SELECT GROUP_CONCAT(Xmlio_LivePool.id_) idList, GROUP_CONCAT(Xmlio_LivePool.processId) processidList\n").
		append("FROM Xmlio_LivePool\n").
		append("INNER JOIN Xmlio_LivePool child ON (Xmlio_LivePool.livePoolId = child.livePoolId)\n").
		append("  WHERE child.liveChildId IN (%s)\n").
		append("    AND LENGTH(Xmlio_LivePool.processId) > 0").toString();
	
	/**
	 * GET
	 */
	public LivePool getLivePoolByChildId(long liveChildId) throws SystemException{
		List<LivePool> lpList = livePoolPersistence.findByliveChild(liveChildId);
		if (lpList.size()>0) return lpList.get(0);
		return null;
	}
	
	public List<LivePool> getLivePoolListByChildId(long liveChildId) throws SystemException{
		return livePoolPersistence.findByliveChild(liveChildId);
	}
	
	public List<LivePool> getLivePoolListByParentId(long liveParentId) throws SystemException{
		return livePoolPersistence.findByliveParent(liveParentId);
	}
	
	public List<LivePool> getLivePoolListByPoolId(long livePoolId) throws SystemException{
		return livePoolPersistence.findBylivePool(livePoolId);
	}
	
	public List<LivePool> getLivePoolListByPoolIdParentId(long livePoolId, long liveParentId) throws SystemException{
		return livePoolPersistence.findBylivePoolParent(livePoolId, liveParentId);
	}
	
	public List<LivePool> getAllLivePoolDescendants(long livePoolId, long liveParentId) throws SystemException{
		List<LivePool> descendentList = new ArrayList<LivePool>();
		for(LivePool lp: livePoolPersistence.findBylivePoolParent(livePoolId, liveParentId)){
			descendentList.add(lp);
			descendentList.addAll(getAllLivePoolDescendants(lp.getLivePoolId(), lp.getLiveChildId()));
		}
		return descendentList;
	}
	
	public List<LivePool> getLivePoolListByProcessId(String processId) throws SystemException{
		return livePoolPersistence.findByprocess(processId);
	}
	
	public long countByProcessId(String processId) throws SystemException{
		return livePoolPersistence.countByprocess(processId);
	}
	
	public long countByProcessIdStatus(String processId, String status) throws SystemException, PortalException{
		int i = 0;
		
		List<LivePool> lplist = livePoolPersistence.findByprocess(processId);		
		for (LivePool lp : lplist){
			if (LiveLocalServiceUtil.getLive(lp.getLiveChildId()).getStatus().equals(status))
				i++;
		}
		
		return i;
	}
	
	/**
	 * CREATE
	 */
	public LivePool createLivePool(long livePoolId, long liveParentId, long liveChildId) throws SystemException{
		return createLivePool(livePoolId, liveParentId, liveChildId, false);
	}
	
	public LivePool createLivePool(long livePoolId, long liveParentId, long liveChildId, boolean overridePool) throws SystemException{
		
		LivePool lp = null;
		if (livePoolId != -1 && liveParentId != -1){
			
			//Delete old parents
			if(overridePool == true){
				List<LivePool> livePoolList = livePoolLocalService.getLivePoolListByChildId(liveChildId);	
				for (LivePool livePool : livePoolList){
					livePoolPersistence.remove(livePool);
				}
			}
				
			if (livePoolPersistence.countBylivePoolEntry(livePoolId, liveParentId, liveChildId)==0){
				long id = counterLocalService.increment();
				lp = livePoolPersistence.create(id);
			}
			else{
				lp = livePoolPersistence.fetchBylivePoolEntry(livePoolId, liveParentId, liveChildId);
			}
			lp.setLivePoolId(livePoolId);
			lp.setLiveParentId(liveParentId);
			lp.setLiveChildId(liveChildId);
			livePoolPersistence.update(lp, true);
		}
		return lp;
		
	}
	
	/**
	 * DELETE
	 */
	/**
	 * Borra las entradas dentro de un pool con un ClassNameValue determinado. Borra sus hijos usando la funcion removeLivePool
	 */
	public void removeLivePoolItemsByClassNameValue (long livePoolId, long liveParentId, String classNameValue) throws SystemException, PortalException
	{
		List<LivePool> livePoolList = getLivePoolListByPoolIdParentId(livePoolId, liveParentId);
		for (LivePool lp : livePoolList)
		{
			try
			{
				Live live = null;
				try
				{
					// Si no existe entrada en el live también se borra
					live = LiveLocalServiceUtil.getLive(lp.getLiveChildId());
				}
				catch(Exception err){}
				
				if (live == null || live.getClassNameValue().equals(classNameValue))
				{
					removeLivePool(lp);
				}
			}
			catch(Exception err)
			{
				// Si falla el borrado de un LivePool se deja la traza y se intenta eliminar el siguiente
				_log.error("Error removing from Live Pool - Id : " + lp.getLiveChildId());
			}
		}
	}
	
	/**
	 * Borra SOLO las entradas en LivePool para el liveEntryId especificado dentro de su pool
	 */
	public void removeLivePool (long livePoolId, long liveParentId, long liveChildId) throws SystemException
	{
		
		LivePool lp = livePoolPersistence.fetchBylivePoolEntry(livePoolId, liveParentId, liveChildId);
		removeLivePool(lp);
	}
	
	/**
	 * Borra SOLO las entradas en LivePool para el liveEntryId especificado dentro de su pool
	 */
	public void removeLivePool (LivePool livePool) throws SystemException
	{
		 
		List<LivePool> lpToRemove = new ArrayList<LivePool>();
		
		try
		{
			//Si es pool, se borra entero
			if (livePool.getLiveParentId() == 0)
			{
				lpToRemove = livePoolPersistence.findBylivePool(livePool.getLiveChildId());
			}
			//Si no es pool y es padre, se borran recursivamente sus hijos
			else
			{
				lpToRemove = getAllLivePoolDescendants(livePool.getLivePoolId(), livePool.getLiveChildId());
				lpToRemove.add(livePool);
			}
		}
		catch(Exception err)
		{
			// Es importante que no se muera antes de intentar el borrado
			_log.error("Error removing from Live Pool - Id : " + ((livePool != null) ? livePool.getLiveChildId() : ""));
		}
		
		for (LivePool lp: lpToRemove )
		{
			try
			{
				livePoolPersistence.remove(lp);
			}
			catch(Exception err)
			{
				_log.error("Error removing from Live Pool - Id : " + lp.getLiveChildId());
			}
		}
	}
	
	/**
	 * Borra TODAS las entradas en LivePool para el liveEntryId especificado
	 */
	public void removeLivePool(long liveEntryId) throws SystemException{
		List<LivePool> lpToRemove = new ArrayList<LivePool>();
		lpToRemove.addAll(livePoolPersistence.findByliveChild(liveEntryId));
		lpToRemove.addAll(livePoolPersistence.findByliveParent(liveEntryId));
		lpToRemove.addAll(livePoolPersistence.findBylivePool(liveEntryId));
		for (LivePool lp: lpToRemove ){
			try{
				livePoolPersistence.remove(lp);
			}
			catch(Exception err){
				_log.error("Error removing from Live Pool - Id : " + lp.getLiveChildId());
			}
		}
	}
		
	
	/*
	 * Functions to control publish process lock
	 */	
	
	/**
	 * Update processId in collection of LivePool
	 * @param processId
	 * @param livePoolIds
	 * @throws SystemException
	 * @throws PortalException 
	 * @throws ServiceError 
	 * @throws SQLException 
	 * @throws IOException 
	 */
	public long updateAllPoolProcessId (String processId, String[] liveChildIdList) throws SystemException, PortalException, ServiceError, IOException, SQLException
	{
		String liveChildIds = StringUtil.merge(liveChildIdList);
		
		// Comprueba que TODOS los childIds pertenezcan al menos a un Pool
		checkPoolByChildId(liveChildIds);
		
		// Comprueba que ninguno de los hijos de los pools implicados estén bloqueados
		checkProcessIdByChildId(liveChildIds);

		// Actualiza el processId de los livePoolIds especificados
		String sql = String.format(UPDATE_PROCESSID_BY_CHILDIDS, liveChildIds, processId);
		DB db = DBFactoryUtil.getDB();
		db.runSQL( sql );
		// PortalLocalServiceUtil.executeUpdateComittedQuery(sql);
		
		BasePersistence<?> persistence = livePoolPersistence;
		persistence.clearCache();
		
		// Se obtiene el número de elementos bloqueados por este ProcessId
		sql = String.format(COUNT_PENDING_LOCKED_BY_PROCESSID, processId);
		List<Object> info = PortalLocalServiceUtil.executeQueryAsList(sql);
		
		return Integer.parseInt( info.get(0).toString() );
	}
		
	/** Comprueba que TODOS los childIds pertenezcan al menos a un Pool
	 * 
	 * @param liveChildIds
	 * @throws ServiceError 
	 */
	private void checkPoolByChildId(String liveChildIds) throws ServiceError
	{
		String sql = String.format(SEL_INEXISTENT_POOL_BY_CHILDIDS, liveChildIds);
		List<Object> info = PortalLocalServiceUtil.executeQueryAsList(sql);
		
		if (info.get(0) != null)
		{
			String errMsg = new String("The items (XmlIO_Live.id_ = ").concat(info.get(0).toString()).concat(") don´t have pool (row in XmlIO_Pool table)");
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_PUBLISH_POOL_NOT_EXIST_ZYX, errMsg);
		}
	}
	
	/** Comprueba que ninguno de los hijos de los pools implicados estén bloqueados
	 * 
	 * @param liveChildIds
	 * @throws ServiceError 
	 */
	private void checkProcessIdByChildId(String liveChildIds) throws ServiceError
	{
		String sql = String.format(SEL_LOCKED_IDS_BY_CHILDIDS, liveChildIds);
		List<Object> info = PortalLocalServiceUtil.executeQueryAsList(sql);
		Object [] row = (Object [])info.get(0);
		
		if (row[0] != null)
		{
			String errMsg = new String("The pool registries (Xmlio_LivePool.id_ = ").concat(row[0].toString()).concat(") are locked (processId = ").concat(row[1].toString()).concat(")");
			ErrorRaiser.throwIfError( IterErrorKeys.XYZ_E_PUBLISH_PARTIAL_ALREADY_IN_PROC_ZYX, errMsg);
		}
	}
	
	/**
	 * Update processId in elements with a LivePool passed as a parameter
	 * @param processId
	 * @param livePoolId
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws ServiceError 
	 */
	public long updatePoolProcessId (String processId, long livePoolId) throws SystemException, PortalException, ServiceError{
		List<LivePool> pools = livePoolPersistence.findBylivePool(livePoolId);
		
		int index = 0;		
		for (LivePool pool : pools){
			//check if already has a value and return an error
			String poolProcessid = pool.getProcessId();
			if (poolProcessid.isEmpty() || poolProcessid == processId)
			{
				try
				{
					if (!poolProcessid.isEmpty())
					{
						_log.trace( "updatePoolProcessId(1) - poolProcessid: " 	+ poolProcessid );
						_log.trace( "updatePoolProcessId(1) - getId: " 			+ pool.getId() );
						_log.trace( "updatePoolProcessId(1) - getLivePoolId: " 	+ pool.getLivePoolId() );
						_log.trace( "updatePoolProcessId(1) - getLiveParentId: "+ pool.getLiveParentId() );
						_log.trace( "updatePoolProcessId(1) - getLiveChildId: " + pool.getLiveChildId() );
					}
				}
				catch(Exception err)
				{
					_log.info( "updatePoolProcessId(1) - Unexpected" );
				}

				pool.setProcessId(processId);
				livePoolPersistence.update(pool, true);
				
				index++;
			}
			else
			{				
				try
				{
					_log.trace( "updatePoolProcessId(2) - poolProcessid: " 	+ poolProcessid );
					_log.trace( "updatePoolProcessId(2) - getId: " 			+ pool.getId() );
					_log.trace( "updatePoolProcessId(2) - getLivePoolId: " 	+ pool.getLivePoolId() );
					_log.trace( "updatePoolProcessId(2) - getLiveParentId: "+ pool.getLiveParentId() );
					_log.trace( "updatePoolProcessId(2) - getLiveChildId: " + pool.getLiveChildId() );
				}
				catch(Exception err)
				{
					_log.info( "updatePoolProcessId(2) - Unexpected" );
				}

				//RollBack
				pools = livePoolPersistence.findBylivePool(livePoolId, 0, index);				
				for (LivePool p : pools)
				{
					p.setProcessId(null);
					livePoolPersistence.update(p, true);
				}
				
				//Get error info and throws it.
				Live itemLive = LiveLocalServiceUtil.getLive( pool.getLiveChildId());
				
				Document doc = SAXReaderUtil.createDocument();
				Element root = doc.addElement("item"); 	
				root.addAttribute("processid", processId);
				root.addAttribute("classname", itemLive.getClassNameValue());
				root.addAttribute("groupid", String.valueOf(itemLive.getGroupId()));
				root.addAttribute("localid", itemLive.getLocalId());				
				
				String xptContent = doc.asXML();				
				
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_POOL_IN_PROCESS_ZYX, xptContent);			
			}			
		}
		
		return (long)index;
	}
	
	public void cleanAllProcessId() throws SystemException
	{
		List<LivePool> livePoolList = livePoolPersistence.findByProcessIsNot("");
		for (LivePool livePool : livePoolList)
		{
			livePool.setProcessId(null);
			livePoolPersistence.update(livePool, true);
		}
	}

	/**
	 * Clean processId from livePool collection passed as parameter
	 * @param livePoolIds
	 * @throws SystemException
	 */
	public void cleanAllProcessId (long[] livePoolIds) throws SystemException{
		for(long livePoolId : livePoolIds)
		{
			cleanProcessId(livePoolId);
		}
	}
	
	/**
	 * Clean processId from livePool passed as parameter
	 * @param livePoolId
	 * @throws SystemException
	 */
	public void cleanProcessId (long livePoolId) throws SystemException{
		List<LivePool> pools = livePoolPersistence.findByPoolidProcessIsNot(livePoolId, "");
		
		for (LivePool pool : pools){		
			pool.setProcessId(null);
			livePoolPersistence.update(pool, true);
		}
		
	}
	
	public void releaseLockByProcessId (String processId) throws SystemException, IOException, SQLException
	{
		String sql = String.format(UPDATE_PROCESSID_TO_NULL, processId);
		DB db = DBFactoryUtil.getDB();
		db.runSQL( sql );
		//PortalLocalServiceUtil.executeUpdateComittedQuery(sql);
		
		BasePersistence<?> persistence = livePoolPersistence;
		persistence.clearCache();
	}

}


