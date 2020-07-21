package com.protecmedia.portal.service;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupServiceWrapper;
import com.liferay.portal.service.GroupService;
import com.liferay.portal.service.ServiceContext;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;

public class MyGroupService extends GroupServiceWrapper {
	/* (non-Java-doc)
	 * @see com.liferay.portal.service.GroupServiceWrapper#GroupServiceWrapper(GroupService groupService)
	 */
	public MyGroupService(GroupService groupService) {
		super(groupService);
	}
	
	@Override 
	public Group addGroup(
			String name, String description, int type,
			String friendlyURL, boolean active, ServiceContext serviceContext) throws PortalException, SystemException{
		try{
			return super.addGroup(name, description, type, friendlyURL, active, serviceContext);
		} catch(Exception ex){
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_ADD_GROUP_ZYX, ex));
		}
	}
	
	@Override 
	public Group updateGroup(long groupId, String name, String description, int type, 
							 String friendlyURL, boolean active, ServiceContext serviceContext) throws PortalException, SystemException
	{
		try
		{
			return super.updateGroup(groupId, name, description, type, friendlyURL, active, serviceContext);
		} 
		catch(Exception ex)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_UPDATE_GROUP_ZYX, ex));
		}
	}

	@Override 
	public void deleteGroup(long groupId) throws PortalException, SystemException{
		try{
			super.deleteGroup(groupId);
		} catch(Exception ex){
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_DELETE_GROUP_ZYX, ex));
		}
	}
	
}