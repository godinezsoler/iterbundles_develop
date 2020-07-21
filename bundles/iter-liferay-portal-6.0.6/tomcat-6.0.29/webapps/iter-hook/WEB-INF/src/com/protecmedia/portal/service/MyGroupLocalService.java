/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.portal.service;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.GroupConstants;
import com.liferay.portal.service.GroupLocalService;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceWrapper;
import com.liferay.portal.service.ServiceContext;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.news.service.QualificationLocalServiceUtil;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.portal.GroupXmlIO;

public class MyGroupLocalService extends GroupLocalServiceWrapper {
	
	private static Log _log = LogFactoryUtil.getLog(MyGroupLocalService.class);
	private static ItemXmlIO itemXmlIO = new GroupXmlIO();
	
	public MyGroupLocalService(GroupLocalService groupLocalService) {
		super(groupLocalService);
	}
	
	/*	 
	 * Add functions 
	 */	
	@Override
	public Group addGroup(long userId, String className, long classPK, 
			long liveGroupId, String name, String description, int type, 
			String friendlyURL, boolean active, ServiceContext 
			serviceContext) throws PortalException ,SystemException {
		
		Group group = super.addGroup(userId, className, classPK, liveGroupId, 
				name, description, type, friendlyURL, active, serviceContext);
		
		if (group.getClassName().equals(IterKeys.CLASSNAME_GROUP)){
			try {			
				itemXmlIO.createLiveEntry(group);					
			} catch (Exception e) {
				_log.error("add Group", e);
			}
		}
		
		return group;
	}
	
	
	@Override
	public Group addGroup(Group group) throws SystemException {
		
		Group grp = super.addGroup(group);
		
		if (grp.getClassName().equals(IterKeys.CLASSNAME_GROUP)){
			try {			
				itemXmlIO.createLiveEntry(group);					
			} catch (Exception e) {
				_log.error("add Group", e);
			}
		}
		
		return grp;
	}
	
	@Override
	public Group addGroup(long userId, String className, long classPK, String 
			name, String description, int type, String friendlyURL, boolean 
			active, ServiceContext serviceContext) throws PortalException, 
			SystemException {
		
		return addGroup(
				userId, className, classPK, GroupConstants.DEFAULT_LIVE_GROUP_ID,
				name, description, type, friendlyURL, active, serviceContext);		
		
	}
	
	/*
	 * Update Functions
	 */
	@Override
	public Group updateGroup(long groupId, String name, String description, 
							 int type, String friendlyURL, boolean active, 
							 ServiceContext serviceContext) throws PortalException, SystemException 
	{
		Group group = null;
				
		try
		{
			group = GroupLocalServiceUtil.getGroup(groupId);
			
			Live liveGroup = LiveLocalServiceUtil.getLiveByLocalId(groupId, IterKeys.CLASSNAME_GROUP, String.valueOf(groupId));
			
			ErrorRaiser.throwIfFalse( !(Validator.isNotNull(liveGroup.getExistInLive()) && 
									  liveGroup.getExistInLive().equals("S")), 
									  IterErrorKeys.XYZ_E_NO_GROUP_UPDATES_AFTER_PUBLISHED_ZYX );

			group = super.updateGroup(groupId, name, description, type, friendlyURL, active, serviceContext);
			
			// ITER-1041 Error "No Group exists with the key" al intentar publicar un sitio importado desde un mln
			// http://jira.protecmedia.com:8080/browse/ITER-1041?focusedCommentId=42821&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-42821
			// El grupo ya tiene que tener actualizado el nombre al crear/actualizar el XMLIO.
			if(group.getClassName().equals(IterKeys.CLASSNAME_GROUP))
				itemXmlIO.createLiveEntry(group);
		}
		catch (Exception e)
		{
			_log.trace(e);
			
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, e));
		}
		
		return group;
	}

	@Override
	public Group updateGroup(long groupId, String typeSettings)
		throws PortalException, SystemException {

		try {
			Group grp = GroupLocalServiceUtil.getGroup(groupId);
			if (grp.getClassName().equals(IterKeys.CLASSNAME_GROUP)){
				itemXmlIO.createLiveEntry(grp);
			}
		} catch (Exception e) {
			_log.error("Update Group", e);
		}
		
		return super.updateGroup(groupId, typeSettings);
	}
	
	
	
	/*	 
	 * Delete functions 
	 */
	@Override
	public void deleteGroup(long groupId) throws PortalException, SystemException {
		try{
			
			try {
				Group grp = GroupLocalServiceUtil.getGroup(groupId);
				if (grp.getClassName().equals(IterKeys.CLASSNAME_GROUP)){
					itemXmlIO.deleteLiveEntry(grp);
				}
			} catch (Exception e) {
				_log.error("Delete Group", e);
			}
			
			/*
			String jdbDefaultURL = GetterUtil.getString(PrefsPropsUtil.getString("jdbc.default.url"));
			String jdbDefaultDriverClassName = GetterUtil.getString(PrefsPropsUtil.getString("jdbc.default.driverClassName"));			
			*/
			QualificationLocalServiceUtil.deleteQualifications(groupId);
			super.deleteGroup(groupId);

		}catch(Exception e){		
		}		
	
	}
}
