/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
/**
 * Copyright (c) 2000-2010 Liferay, Inc. All rights reserved.
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

package com.protecmedia.iter.news.service.impl;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.protecmedia.iter.news.service.base.CountersServiceBaseImpl;

/**
 * @author Protecmedia
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class CountersServiceImpl extends CountersServiceBaseImpl {
}