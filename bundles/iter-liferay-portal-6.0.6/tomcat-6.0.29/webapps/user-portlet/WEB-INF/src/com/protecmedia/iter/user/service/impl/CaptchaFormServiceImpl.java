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

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.xml.DocumentException;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.user.service.base.CaptchaFormServiceBaseImpl;

/**
 * The implementation of the captcha form remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.user.service.CaptchaFormService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.user.service.CaptchaFormServiceUtil} to access the captcha form remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see com.protecmedia.iter.user.service.base.CaptchaFormServiceBaseImpl
 * @see com.protecmedia.iter.user.service.CaptchaFormServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class CaptchaFormServiceImpl extends CaptchaFormServiceBaseImpl {
	
	public boolean isValid(Long groupId, String responseValue, String remoteAddress) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException{
		try{
			
			return captchaFormLocalService.isValid(groupId, responseValue, remoteAddress);
			
		}catch(ORMException orme){
			ServiceErrorUtil.throwSQLIterException(orme);
			return false;
		}		
	}	
}