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

import java.util.Date;
import java.util.List;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.NoSuchChannelControlException;
import com.protecmedia.iter.xmlio.model.ChannelControl;
import com.protecmedia.iter.xmlio.service.ChannelControlLogLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.base.ChannelControlLocalServiceBaseImpl;

/**
 * The implementation of the channel control local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.service.ChannelControlLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.xmlio.service.ChannelControlLocalServiceUtil} to access the channel control local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.xmlio.service.base.ChannelControlLocalServiceBaseImpl
 * @see com.protecmedia.iter.xmlio.service.ChannelControlLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class ChannelControlLocalServiceImpl extends ChannelControlLocalServiceBaseImpl {
	
	private static Log _log = LogFactoryUtil.getLog(ChannelControlLocalServiceImpl.class);
	private boolean _locked = false;
	
	public boolean lockChannel(){
		synchronized (this)
		{
			if(!_locked)
			{
				_locked = true;
				return true;
			}
			
			return false;
		}
	}
	
	public void unlockChannel(){
		synchronized (this)
		{
			if(_locked)
				_locked = false;
		}
	}
	
	
	public long startOperation( long groupid, long userid, String type, String operation) throws SystemException
	{
		synchronized (this)
		{
			long operationId = 0;
			
			if(channelControlPersistence.findByStatus(IterKeys.PROCESSING).size()==0)
			{
				long id = counterLocalService.increment();
				ChannelControl cc = channelControlPersistence.create(id);
				cc.setGroupId(groupid);
				cc.setUserId(userid);
				cc.setType(type);
				cc.setOperation(operation);
				cc.setStatus(IterKeys.PROCESSING);
				cc.setStartDate(new Date());
				channelControlPersistence.update(cc, false);
				operationId =  cc.getId();
			}
			
			return operationId;
		}
	}

	public void finishOperation(long channelControlId, int operations, int errors, String errorLog) throws SystemException, NoSuchChannelControlException
	{
		synchronized (this) {
			ChannelControl cc = channelControlPersistence.findByPrimaryKey(channelControlId);
			if(cc!=null){
				cc.setErrors(errors);
				cc.setOperations(operations);
				// Se omite la asignacion del log de error, vendra siempre vacío ya que no sabemos si ha habido errores al llamar a esta función.
				//cc.setErrorLog(errorLog);
				//cc.setStatus( (errorLog.equals("")) ? IterKeys.DONE : IterKeys.ERROR );
				cc.setStatus( (cc.getErrorLog().equals("")) ? IterKeys.DONE : IterKeys.ERROR );
				cc.setEndDate(new Date());
				channelControlPersistence.update(cc, false);
			}
		}
	}
	
	public void deleteCurrentOperation(long channelControlId)
	{
		try {
			channelControlPersistence.remove(channelControlId);
		} catch (NoSuchChannelControlException nscce) {
			_log.error(nscce);
		} catch (SystemException se) {
			_log.error(se);
		}
	}
	
	public void setErrorLog(String errorLog) throws SystemException{
		
		List<ChannelControl> ccList = channelControlPersistence.findByStatus(IterKeys.PROCESSING);
		if(ccList.size()==1){
			ChannelControl cc = ccList.get(0);
			StringBuilder errorStr = new StringBuilder( cc.getErrorLog() );
			if( errorStr.length()!=0 )
				errorStr.append(";");
			errorStr.append(errorLog);
			cc.setErrorLog(errorStr.toString());
			channelControlPersistence.update(cc, false);
		}
	}
	
	public void updateFileSize(long channelControlId, long fileSize){
		try {
			ChannelControl cc = channelControlPersistence.findByPrimaryKey(channelControlId);
			cc.setFileSize(fileSize);
		} catch (NoSuchChannelControlException e) {
			_log.error(e);
		} catch (SystemException e) {
			_log.error(e);
		}
	}
	
	/**
	 * @type manual or automatic
	 */
	public void deleteChannelControlHistory(String type) throws Exception{
		List<ChannelControl> channelControlList = channelControlPersistence.findByType(type);
		for ( ChannelControl cc : channelControlList ){
			deleteSelectedRow(cc.getId());
		}
	}
	
	public void deleteSelectedRow(long channelControlId){
		try {
			ChannelControlLogLocalServiceUtil.deleteErrorLog(channelControlId);
			channelControlPersistence.remove(channelControlId);
		} catch (NoSuchChannelControlException nscce) {
			_log.error(nscce);
		} catch (SystemException se) {
			_log.error(se);
		}
		_log.info("Channel Control cleaned");
	}
	
	public List<ChannelControl> getChannelControlByType(String type){
		
		try {
			return channelControlPersistence.findByType(type);
		} catch (SystemException e) {
			_log.error(e);
			return null;
		}
	}
	
	public boolean operationInProgress() throws SystemException{
		return !(channelControlPersistence.findByStatus(IterKeys.PROCESSING).size()==0);
	}
	
}