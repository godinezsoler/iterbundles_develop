/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.designer.util;

import java.util.List;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.Validator;
import com.protecmedia.iter.designer.model.PageTemplate;
import com.protecmedia.iter.designer.service.PageTemplateLocalServiceUtil;

public class PageTemplateValidator {
	
	public static boolean validatePageTemplate(PageTemplate pageTemplate, List<String> errors) throws SystemException {
		boolean valid = true;
		if (Validator.isNull(pageTemplate.getName())) {
			errors.add("page-template-name-required");			
			valid = false;
		} 		
	
		PageTemplate tmp = PageTemplateLocalServiceUtil.getPageTemplateByPageTemplateId(pageTemplate.getGroupId(), pageTemplate.getPageTemplateId());
		if (tmp != null) {			
			errors.add("page-template-already-exist");			
			valid = false;
		}
		
		return valid;
	}
	
	public static boolean validateUpdatePageTemplate(PageTemplate pageTemplate, List<String> errors) throws SystemException {
		boolean valid = true;
		if (Validator.isNull(pageTemplate.getName())) {
			errors.add("page-template-name-required");			
			valid = false;
		}
		
		return valid;
	}

}
