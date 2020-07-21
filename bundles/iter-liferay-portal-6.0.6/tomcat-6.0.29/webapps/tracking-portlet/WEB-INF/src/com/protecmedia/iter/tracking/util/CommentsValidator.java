package com.protecmedia.iter.tracking.util;

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
		if (comment.getMessage().length() > 3000){
			errors.add("comment-text-exceeded");			
			valid = false;
		}

		return valid;
	}
		
}