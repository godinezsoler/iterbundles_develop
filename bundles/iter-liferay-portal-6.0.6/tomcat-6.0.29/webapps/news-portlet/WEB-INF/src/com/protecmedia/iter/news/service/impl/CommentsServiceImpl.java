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

import java.util.List;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.exception.SystemException;
import com.protecmedia.iter.news.model.Comments;
import com.protecmedia.iter.news.service.base.CommentsServiceBaseImpl;

/**
 * @author Protecmedia
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class CommentsServiceImpl extends CommentsServiceBaseImpl {
	
	public List<Comments> getComments(long groupId, String contentId) throws SystemException {
		return commentsLocalService.getComments(groupId, contentId);
	}
	
	public List<Comments> getComments(long groupId, String contentId, int start, int end) throws SystemException {
		return commentsLocalService.getComments(groupId, contentId, start, end);
	}
		
	public Comments enableComment(long commentId) throws SystemException {		
		return commentsLocalService.enableComment(commentId);
	}
	
	public Comments disableComment(long commentId) throws SystemException {
		return commentsLocalService.disableComment(commentId);
	}
	
}
