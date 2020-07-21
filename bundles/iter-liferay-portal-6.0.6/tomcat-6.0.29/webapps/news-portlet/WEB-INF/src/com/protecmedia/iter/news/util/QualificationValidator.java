/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.news.util;

import java.util.List;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.Validator;
import com.protecmedia.iter.news.model.Qualification;
import com.protecmedia.iter.news.service.QualificationLocalServiceUtil;

public class QualificationValidator {
	public static boolean validateQualification(Qualification qualification, List<String> errors) throws SystemException {
		boolean valid = true;
		if (Validator.isNull(qualification.getName())) {
			errors.add("qualification-name-required");			
			valid = false;
		} 		
	
		Qualification q = QualificationLocalServiceUtil.getQualification(qualification.getGroupId(), qualification.getName());
		if (q != null) {			
			errors.add("qualification-already-exist");			
			valid = false;
		}
		
		return valid;
	}
		
}
