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

package com.protecmedia.iter.user.service.impl;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.SQLException;
import java.text.ParseException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.xml.DocumentException;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.user.service.base.IterRegisterServiceBaseImpl;

/**
 * The implementation of the iter register remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.user.service.IterRegisterService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.user.service.IterRegisterServiceUtil} to access the iter register remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see com.protecmedia.iter.user.service.base.IterRegisterServiceBaseImpl
 * @see com.protecmedia.iter.user.service.IterRegisterServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class})
public class IterRegisterServiceImpl extends IterRegisterServiceBaseImpl
{
	public void preRegisterUser(HttpServletRequest request, HttpServletResponse response, String socialName, long groupId, JSONObject user) throws Exception
	{
		try
		{
			iterRegisterLocalService.preRegisterUser(request, response, socialName, groupId, user);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
	}
	
	public void deleteExpiredUsers() throws ServiceError, IOException, SQLException
	{
		try
		{
			iterRegisterLocalService.deleteExpiredUsers();
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
	}
	
	public void initOrUpdateUserToDeleteTask() throws ParseException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		try
		{
			iterRegisterLocalService.initOrUpdateUserToDeleteTask();
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
	}
	
	public String getUserCredentials(long groupid, String email, boolean isnamecheked, boolean ispwdcheked, String refererurl, HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		String result = "";
		try
		{
			result = iterRegisterLocalService.getUserCredentials(groupid, email, isnamecheked, ispwdcheked, refererurl, request, response);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
//	public String checkChallenge(String challresp, HttpServletRequest request, HttpServletResponse response)throws ServiceError, MalformedURLException, PortalException, SystemException, AddressException, UnsupportedEncodingException, SecurityException, NoSuchMethodException, MessagingException, ParseException
//	{
//		String result= "";
//		try
//		{
//			result = iterRegisterLocalService.checkChallenge(challresp, request, response);
//		}
//		catch(ORMException orme)
//		{
//			ServiceErrorUtil.throwSQLIterException(orme);
//		}
//		
//		return result;
//	}
	
	public long getDelayToDeleteExpiredUsers() throws SecurityException, NoSuchMethodException, ParseException, ServiceError
	{
		long result = 0;
		try
		{
			result = iterRegisterLocalService.getDelayToDeleteExpiredUsers();
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String unregisterUser(HttpServletRequest request, HttpServletResponse response, String password1, String password2) throws ServiceError
	{
		String result = JSONFactoryUtil.createJSONObject().toString();
		try
		{
			result = iterRegisterLocalService.unregisterUser(request, response, password1, password2);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
}
