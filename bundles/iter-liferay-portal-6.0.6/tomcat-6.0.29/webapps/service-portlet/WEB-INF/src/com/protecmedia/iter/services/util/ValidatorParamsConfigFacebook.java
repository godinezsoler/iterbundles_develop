/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.services.util;

import java.util.List;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.Validator;



public class ValidatorParamsConfigFacebook {
	public static boolean validateScreenNameAndFacebook(String facebookScreenName, int numFeeds, List<String> errors) throws SystemException {
		boolean valid = true;
		if (Validator.isNull(facebookScreenName)) {
			errors.add("params-config-emailUser-error-null");			
			valid = false;
		}else if (facebookScreenName.equalsIgnoreCase("")) 	{
			errors.add("params-config-emailUser-error-empty-string");			
			valid = false;
		}
		
		if ( numFeeds == 0  ) {
			errors.add("error");			
			valid = false;
		}
		return valid;
	}
}
