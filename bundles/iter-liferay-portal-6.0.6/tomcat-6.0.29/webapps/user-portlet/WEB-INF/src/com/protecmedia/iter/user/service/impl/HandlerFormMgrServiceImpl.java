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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.xml.Document;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.user.service.base.HandlerFormMgrServiceBaseImpl;

/**
 * The implementation of the handler form mgr remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.user.service.HandlerFormMgrService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.user.service.HandlerFormMgrServiceUtil} to access the handler form mgr remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see com.protecmedia.iter.user.service.base.HandlerFormMgrServiceBaseImpl
 * @see com.protecmedia.iter.user.service.HandlerFormMgrServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class HandlerFormMgrServiceImpl extends HandlerFormMgrServiceBaseImpl
{
	public void startHandlerDatabaseForm(Map<String, ArrayList> adjuntos, Document xmlDom, Long groupId, String formReceivedId) throws ServiceError, IOException, SQLException, PortalException, SystemException
	{
		try
		{
			handlerFormMgrLocalService.startHandlerDatabaseForm(adjuntos, xmlDom, groupId, formReceivedId);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
	}
}