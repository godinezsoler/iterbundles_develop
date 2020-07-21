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

import java.util.List;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.protecmedia.iter.xmlio.model.ChannelControlLog;
import com.protecmedia.iter.xmlio.service.base.ChannelControlLogLocalServiceBaseImpl;

/**
 * The implementation of the channel control log local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.service.ChannelControlLogLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.xmlio.service.ChannelControlLogLocalServiceUtil} to access the channel control log local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.xmlio.service.base.ChannelControlLogLocalServiceBaseImpl
 * @see com.protecmedia.iter.xmlio.service.ChannelControlLogLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class ChannelControlLogLocalServiceImpl extends ChannelControlLogLocalServiceBaseImpl {
	
	private static Log _log = LogFactoryUtil.getLog(ChannelControlLogLocalServiceImpl.class);
	
	public void addErrorLog(long companyId, long channelControlId, List<Node> errorNodes){
		
		try{
			for (Node errorNode : errorNodes){
				Element errorElement = (Element)errorNode;
				long id = counterLocalService.increment();
				String op = errorElement.attribute("operation").getValue();
				String gblId = errorElement.attribute("globalId").getValue();
				String grpId = errorElement.attribute("groupId").getValue();
				String className = errorElement.attribute("classname").getValue();
				
				ChannelControlLog ccl = channelControlLogPersistence.create(id);
				ccl.setChannelControlId(channelControlId);
				ccl.setOperation( op );
				ccl.setGlobalId( gblId );
				try {
					long gId = GroupLocalServiceUtil.getGroup(companyId, grpId).getGroupId();
					ccl.setGroupId( gId );
				} catch (PortalException e1) {
					ccl.setGroupId( -1 );
					_log.error(e1);
				}
				ccl.setClassNameValue( className );
				ccl.setErrorLog( errorNode.getText() );
				
				channelControlLogPersistence.update(ccl, false);
			}
		}catch (SystemException se) {
			_log.error(se);
		}
	}
	
	public void deleteErrorLog(long channelControlId){
		try {
			channelControlLogPersistence.removeBychannelControlId(channelControlId);
		} catch (SystemException se) {
			_log.error(se);
		}
	}
	
	public List<ChannelControlLog> getOperationLog(long channelControlId){
		try {
			return channelControlLogPersistence.findBychannelControlId(channelControlId);
		} catch (SystemException e) {
			_log.error(e);
		}
		return null;
	}
}