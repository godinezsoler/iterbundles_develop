package com.protecmedia.portal.service;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.ArrayUtils;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.model.UserGroupRole;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserLocalService;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceWrapper;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.portal.UserXmlIO;

public class MyUserLocalService extends UserLocalServiceWrapper {
	
	private static Log _log = LogFactoryUtil.getLog(MyUserLocalService.class);
	private static ItemXmlIO itemXmlIO = new UserXmlIO();
	
	public MyUserLocalService(UserLocalService userLocalService) {
		super(userLocalService);		
	}
	
	/*
	 * Add Functions 
	 * --------------	
	 */	
	
	@Override
	public User addUser(
			long creatorUserId, long companyId, boolean autoPassword,
			String password1, String password2, boolean autoScreenName,
			String screenName, String emailAddress, long facebookId,
			String openId, Locale locale, String firstName, String middleName,
			String lastName, int prefixId, int suffixId, boolean male,
			int birthdayMonth, int birthdayDay, int birthdayYear,
			String jobTitle, long[] groupIds, long[] organizationIds,
			long[] roleIds, long[] userGroupIds, boolean sendEmail,
			ServiceContext serviceContext)
		throws PortalException, SystemException {

		
		User user = super.addUser(creatorUserId, companyId, autoPassword, password1, password2, 
				autoScreenName, screenName, emailAddress, facebookId, openId, locale, firstName, 
				middleName, lastName, prefixId, suffixId, male, birthdayMonth, birthdayDay, 
				birthdayYear, jobTitle, groupIds, organizationIds, roleIds, userGroupIds, 
				sendEmail, serviceContext);

		try {			
			//NO se publican los usuarios con rol administrador ni el usuario por defecto
			if(!ArrayUtils.contains(user.getRoleIds(), RoleLocalServiceUtil.getRole(companyId, IterKeys.ROLE_ADMINISTRATOR).getRoleId()) &&
					!user.getEmailAddress().equals(IterKeys.DEFAULT_USER_EMAIL)){
				itemXmlIO.createLiveEntry(user);		
			}
		} catch (Exception e) {
			_log.error(e);
		}
		
		return user;
	}
	
	/*
	 * Delete functions
	 */	
	
	@Override
	public void deleteUser(long userId)
	throws PortalException, SystemException {
		
		try {
			User user = UserLocalServiceUtil.getUser(userId);
			itemXmlIO.deleteLiveEntry(user);	
		} catch (Exception e) {
			_log.error(e);
		}
		
		super.deleteUser(userId);	
	}
	
	
	/*
	 * Update Functions
	 */
		
	@Override
	public User updateUser(
			long userId, String oldPassword, String newPassword1,
			String newPassword2, boolean passwordReset,
			String reminderQueryQuestion, String reminderQueryAnswer,
			String screenName, String emailAddress, long facebookId,
			String openId, String languageId, String timeZoneId,
			String greeting, String comments, String firstName,
			String middleName, String lastName, int prefixId, int suffixId,
			boolean male, int birthdayMonth, int birthdayDay, int birthdayYear,
			String smsSn, String aimSn, String facebookSn, String icqSn,
			String jabberSn, String msnSn, String mySpaceSn, String skypeSn,
			String twitterSn, String ymSn, String jobTitle, long[] groupIds,
			long[] organizationIds, long[] roleIds,
			List<UserGroupRole> userGroupRoles, long[] userGroupIds,
			ServiceContext serviceContext)
		throws PortalException, SystemException {

		try {
			User user = UserLocalServiceUtil.getUser(userId);
			//NO se publican los usuarios con rol administrador
			if(!ArrayUtils.contains(roleIds, RoleLocalServiceUtil.getRole(user.getCompanyId(), IterKeys.ROLE_ADMINISTRATOR).getRoleId())){
				itemXmlIO.createLiveEntry(user);	
			}else{
				Company company = CompanyLocalServiceUtil.getCompanyById(user.getCompanyId());
				Live live = LiveLocalServiceUtil.getLiveByLocalId(company.getGroup().getGroupId(), IterKeys.CLASSNAME_USER, String.valueOf(user.getUserId()));
				LiveLocalServiceUtil.deleteLive(live.getId());
			}
		} catch (Exception e) {
			_log.error(e);
		}
		
		return super.updateUser(userId, oldPassword, newPassword1, newPassword2, 
				passwordReset, reminderQueryQuestion, reminderQueryAnswer, screenName, 
				emailAddress, facebookId, openId, languageId, timeZoneId, greeting, comments, 
				firstName, middleName, lastName, prefixId, suffixId, male, birthdayMonth, 
				birthdayDay, birthdayYear, smsSn, aimSn, facebookSn, icqSn, jabberSn, msnSn, 
				mySpaceSn, skypeSn, twitterSn, ymSn, jobTitle, groupIds, organizationIds, 
				roleIds, userGroupRoles, userGroupIds, serviceContext);
	}
	
	
	public User updateActive(long userId, boolean active)
	throws PortalException, SystemException {

		try {
			User user = UserLocalServiceUtil.getUser(userId);
			itemXmlIO.createLiveEntry(user);	
		} catch (Exception e) {
			_log.error(e);
		}
	
		return super.updateActive(userId, active);
	}
}