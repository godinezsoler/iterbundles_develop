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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletPreferences;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.Junction;
import com.liferay.portal.kernel.dao.orm.OrderFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.protecmedia.iter.news.model.Comments;
import com.protecmedia.iter.news.service.CommentsLocalServiceUtil;
import com.protecmedia.iter.news.service.base.CommentsLocalServiceBaseImpl;
import com.protecmedia.iter.news.service.persistence.CommentsPersistence;
import com.protecmedia.iter.news.util.CommentsValidator;

/**
 * @author Protecmedia
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class CommentsLocalServiceImpl extends CommentsLocalServiceBaseImpl {
	
	/*
	 * 
	 */
	public int countByGorupArticleIdComments(long groupId, String articleId) throws SystemException {
		return commentsPersistence.countByGroupContentIdFinder(groupId, articleId);
	}
	
	public int countByGorupArticleIdComments(long groupId, String articleId, boolean active) throws SystemException {
		return commentsPersistence.countByGroupContentIdActiveFinder(groupId, articleId, active);
	}
	
	public int countByGorupArticleIdModeratedComments(long groupId, String articleId, boolean moderated) throws SystemException {
		return commentsPersistence.countByGroupContentIdActiveFinder(groupId, articleId, moderated);
	}
	
	/*
	 * Get functions
	 */
	
	public int getCommentsCount(long groupId, String contentId) throws SystemException {
		return commentsPersistence.countByGroupContentIdFinder(groupId, contentId);
	}
	
	public List<Comments> getComments(long groupId, String contentId) throws SystemException {
		return commentsPersistence.findByGroupContentIdFinder(groupId, contentId);
	}
	
	public List<Comments> getComments(long groupId, String contentId, int start, int end) throws SystemException {
		return commentsPersistence.findByGroupContentIdFinder(groupId, contentId, start, end);
	}
	
	public List<Comments> getComments(long groupId, String contentId, boolean active) throws SystemException {
		return commentsPersistence.findByGroupContentIdActiveFinder(groupId, contentId, active);
	}
	
	public List<Comments> getComments(long groupId, String contentId, boolean active, int start, int end) throws SystemException {
		return commentsPersistence.findByGroupContentIdActiveFinder(groupId, contentId, active, start, end);
	}
	
	public List<Comments> getModeratedComments(long groupId, String contentId, boolean moderated) throws SystemException {
		return commentsPersistence.findByGroupContentIdActiveFinder(groupId, contentId, moderated);
	}
	
	public List<Comments> getModeratedComments(long groupId, String contentId, boolean moderated, int start, int end) throws SystemException {
		return commentsPersistence.findByGroupContentIdActiveFinder(groupId, contentId, moderated, start, end);
	}
	
	/*
	 * 
	 */
	public Comments enableComment(long commentId) throws SystemException {
		Comments comment = commentsPersistence.fetchByPrimaryKey(commentId);
		
		comment.setActive(true);
		comment.setModerated(true);
		
		commentsPersistence.update(comment, false);
		
		return comment;
	}
	
	public Comments disableComment(long commentId) throws SystemException {
		Comments comment = commentsPersistence.fetchByPrimaryKey(commentId);
		
		comment.setActive(false);
		comment.setModerated(true);
		
		commentsPersistence.update(comment, false);
		
		return comment;
	}
	
	/*
	 * Finders functions
	 */
	
	public List<Object []> getArticlesMostCommented(long groupId, int start, int end) throws SystemException {
		List<Object []> list = new ArrayList<Object []>();
				
		DynamicQuery query = DynamicQueryFactoryUtil.forClass(Comments.class, getClass().getClassLoader());
								
		query.add(PropertyFactoryUtil.forName("groupId").eq(groupId));					
		
		query.setProjection(ProjectionFactoryUtil.projectionList()				
				.add(ProjectionFactoryUtil.rowCount(), "numComment")
				.add(ProjectionFactoryUtil.groupProperty("contentId"), "numComment")
						
		)
		.addOrder(OrderFactoryUtil.desc("numComment"));
		
		
		List<?> articulosQuery = (List<?>) CommentsLocalServiceUtil.dynamicQuery(query, start, end);
		
		if (articulosQuery != null) {
			return (List <Object []>) articulosQuery;
		}		
		
		return null;
	}
	
	/*************************************************
	 * 
	 * 		Funciones para Comentarios
	 * 
	 ************************************************/
     public Comments addComment(long groupId, String contentId, long userId, String userName, String email, String message, ArrayList<String> errors) throws SystemException {
    	 
    	int numComments = CommentsLocalServiceUtil.countByGorupArticleIdComments(groupId, contentId);	
		long id = CounterLocalServiceUtil.increment();
			
		Comments comment = commentsLocalService.createComments(id);
		comment.setGroupId(groupId);
		comment.setContentId(contentId);
		comment.setMessage(message);
		comment.setUserName(userName);
		comment.setUserId(userId);
		comment.setEmail(email);
		comment.setPublicationDate(new Date());
		comment.setNumComment(numComments++);
		comment.setActive(false);		
	
		if (CommentsValidator.validateComment(comment, errors)) {
			return commentsPersistence.update(comment, false);
		} 
		return comment;
     }
	
     public void activateComment(long id_) throws PortalException, SystemException {		
 		Comments comment = commentsLocalService.getComments(id_);
 		comment.setActive(true);
 		comment.setModerated(true);
 		
 		commentsLocalService.updateComments(comment);
 	}
 		
 	public void deactivateComment(long id_) throws PortalException, SystemException {		
 		Comments comment = commentsLocalService.getComments(id_);
 		comment.setActive(false);
 		comment.setModerated(true);
 		
 		commentsLocalService.updateComments(comment);
 	}
	
	/*************************************************
	 * 
	 *      Funciones para el modulo tracking
	 * 
	 *************************************************/
	public int getCommentsSearchTrackingCount(long groupId, String text, String moderate, Date startDate, Date endDate, List<Object> users) throws SystemException {
		
		DynamicQuery query = DynamicQueryFactoryUtil.forClass(Comments.class, getClass().getClassLoader());
		
		query.add(PropertyFactoryUtil.forName("groupId").eq(groupId));

		if (!moderate.equals("")) {
			query.add(PropertyFactoryUtil.forName("moderated").eq(Boolean.valueOf(moderate)));
		}
		
		if (startDate != null) {
			query.add(PropertyFactoryUtil.forName("publicationDate").gt(startDate));			
		}
		
		if (endDate != null) {
			query.add(PropertyFactoryUtil.forName("publicationDate").lt(endDate));			
		}
		
		if (text != null && !text.equals("")) {
			String[] keywords = text.split(" ");
			Junction junction = RestrictionsFactoryUtil.disjunction();
			
			for (int i = 0; i < keywords.length; i++) {
				junction.add(PropertyFactoryUtil.forName("message").like("%" + keywords[i] + "%"));				
			}
			query.add(junction);		
		}
		
		if (users.size() > 0) {
			query.add(PropertyFactoryUtil.forName("userId").in(users));			
		}
			
		int total = (int) CommentsLocalServiceUtil.dynamicQueryCount(query);
		return total;
	}
	
	public List<Comments> getCommentsSearchTracking(long groupId, String contentId, String text, String moderate, Date startDate, Date endDate, List<Object> users, String orderBy, String orderType, int start, int end) throws SystemException {
		
		DynamicQuery query = DynamicQueryFactoryUtil.forClass(Comments.class, getClass().getClassLoader());
		
		query.add(PropertyFactoryUtil.forName("contentId").eq(contentId));
		query.add(PropertyFactoryUtil.forName("groupId").eq(groupId));

		if (!moderate.equals("")) {
			query.add(PropertyFactoryUtil.forName("moderated").eq(Boolean.valueOf(moderate)));
		}
		
		if (startDate != null) {
			query.add(PropertyFactoryUtil.forName("publicationDate").gt(startDate));			
		}
		
		if (endDate != null) {
			query.add(PropertyFactoryUtil.forName("publicationDate").lt(endDate));			
		}
		
		if (users != null && users.size() > 0) {
			query.add(PropertyFactoryUtil.forName("userId").in(users));			
		}
		
		if (text != null && !text.equals("")) {
			String[] keywords = text.split(" ");
			Junction junction = RestrictionsFactoryUtil.disjunction();
			
			for (int i = 0; i < keywords.length; i++) {
				junction.add(PropertyFactoryUtil.forName("message").like("%" + keywords[i] + "%"));				
			}
			query.add(junction);		
		}
		
		if (!orderBy.equals("") && !orderType.equals("")) {
			if (orderType.equals("asc")) {
				query.addOrder(OrderFactoryUtil.asc(orderBy));
			} else {
				query.addOrder(OrderFactoryUtil.desc(orderBy));
			}
		}
		
		List<Comments> comments = (List<Comments>) CommentsLocalServiceUtil.dynamicQuery(query, start, end);
		
		return comments;
	}
	
	/*
	 * 
	 */	
	public List<Object> getCommentsSearchContentIdTracking(long groupId, String text, String moderate, List<Object> contentIds) throws SystemException {
		
		DynamicQuery query = DynamicQueryFactoryUtil.forClass(Comments.class, getClass().getClassLoader());
		
		query.add(PropertyFactoryUtil.forName("groupId").eq(groupId));
				
		if (!moderate.equals("")) {
			query.add(PropertyFactoryUtil.forName("moderated").eq(Boolean.valueOf(moderate)));
		}

		if (contentIds != null && contentIds.size() > 0) {
			query.add(PropertyFactoryUtil.forName("contentId").in(contentIds));			
		}		
		
		if (text != null && !text.equals("")) {
			String[] keywords = text.split(" ");
			Junction junction = RestrictionsFactoryUtil.disjunction();
			
			for (int i = 0; i < keywords.length; i++) {
				junction.add(PropertyFactoryUtil.forName("message").like("%" + keywords[i] + "%"));				
			}
			query.add(junction);		
		}
		
		query.setProjection(ProjectionFactoryUtil.projectionList()								
				.add(ProjectionFactoryUtil.groupProperty("contentId"))					
		);
		
		List <Object> comments = (List <Object>) CommentsLocalServiceUtil.dynamicQuery(query);
		
		return comments;
	}
}
