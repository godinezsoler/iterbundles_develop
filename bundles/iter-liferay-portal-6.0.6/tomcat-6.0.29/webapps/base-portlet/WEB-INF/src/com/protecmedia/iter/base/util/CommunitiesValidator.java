/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.base.util;

import java.util.List;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.Validator;
import com.protecmedia.iter.base.model.Communities;

public class CommunitiesValidator {
	public static boolean validateCommunities(Communities com, List<String> errors) throws SystemException {
		boolean valid = true;
		//Condiciones generales
		if (Validator.isNull(com.getGroupId())) {
			errors.add("base-communities-group-required");			
			valid = false;
		}	
		/*if (Validator.isNull(com.getPrivateSearchUrl())) {
			errors.add("base-communities-private-search-url-required");			
			valid = false;
		}		
		if (Validator.isNull(com.getPublicSearchUrl())) {
			errors.add("base-communities-public-search-url-required");			
			valid = false;
		}*/	
		//Casos especiales
		
		return valid;
	}

}
