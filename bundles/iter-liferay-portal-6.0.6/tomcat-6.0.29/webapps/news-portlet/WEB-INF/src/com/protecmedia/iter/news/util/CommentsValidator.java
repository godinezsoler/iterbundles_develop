/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.news.util;

import java.util.List;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.Validator;
import com.protecmedia.iter.news.model.Comments;

public class CommentsValidator {
	public static boolean validateComment(Comments comment, List<String> errors) throws SystemException {
		boolean valid = true;
		if (Validator.isNull(comment.getUserName())) {
			errors.add("comment-user-name-required");			
			valid = false;
		} 
		
		if (Validator.isNull(comment.getMessage())) {
			errors.add("comment-message-required");			
			valid = false;
		} 	
		
		if (!Validator.isEmailAddress(comment.getEmail())) {
			errors.add("comment-email-required");			
			valid = false;
		}

		return valid;
	}
		
}
