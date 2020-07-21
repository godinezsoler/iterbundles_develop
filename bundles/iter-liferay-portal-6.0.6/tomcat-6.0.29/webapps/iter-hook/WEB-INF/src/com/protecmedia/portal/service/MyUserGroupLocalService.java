package com.protecmedia.portal.service;

import java.util.Date;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.UserGroup;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.UserGroupLocalService;
import com.liferay.portal.service.UserGroupLocalServiceUtil;
import com.liferay.portal.service.UserGroupLocalServiceWrapper;
import com.liferay.portlet.journal.model.JournalStructure;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.journal.JournalArticleXmlIO;
import com.protecmedia.iter.xmlio.service.item.portal.UserGroupXmlIO;

public class MyUserGroupLocalService extends UserGroupLocalServiceWrapper {
	
	private static Log _log = LogFactoryUtil.getLog(MyUserGroupLocalService.class);
	private static ItemXmlIO itemXmlIO = new UserGroupXmlIO();
	
	public MyUserGroupLocalService(UserGroupLocalService userGroupLocalService) {
		super(userGroupLocalService);		
	}
	
	/*
	 * Add Functions 
	 * --------------	
	 */	
	
	@Override
	public void addGroupUserGroups(long groupId, long[] userGroupIds)
	throws SystemException {
		
		for(long userGroupId : userGroupIds){
			try {
				UserGroup userGroup = UserGroupLocalServiceUtil.getUserGroup(userGroupId);
				itemXmlIO.createLiveEntry(userGroup);		
			} catch (Exception e) {
				_log.error("add UserGroup", e);
			}
		}
		
		super.addGroupUserGroups(groupId, userGroupIds);
	}

	@Override
	public UserGroup addUserGroup(
			long userId, long companyId, String name, String description)
		throws PortalException, SystemException {
	
		UserGroup userGroup = super.addUserGroup(userId, companyId, name, description);
		
		try {
			itemXmlIO.createLiveEntry(userGroup);		
		} catch (Exception e) {
			_log.error("add UserGroup", e);
		}
	
		return userGroup;
	}
	
	
	/*
	 * Delete functions
	 */	
	
	@Override
	public void deleteUserGroup(long userGroupId)
	throws PortalException, SystemException {
	
		try {
			UserGroup userGroup = UserGroupLocalServiceUtil.getUserGroup(userGroupId);
			itemXmlIO.deleteLiveEntry(userGroup);		
		} catch (Exception e) {
			_log.error("add UserGroup", e);
		}
		
		super.deleteUserGroup(userGroupId);
	}
	
	
	/*
	 * Update Functions
	 */
		
	@Override
	public UserGroup updateUserGroup(
			long companyId, long userGroupId, String name,
			String description)
		throws PortalException, SystemException {

		UserGroup userGroup = super.updateUserGroup(companyId, userGroupId, name, description);
		
		try {
			itemXmlIO.createLiveEntry(userGroup);		
		} catch (Exception e) {
			_log.error("add UserGroup", e);
		}

		return userGroup;
	}
}