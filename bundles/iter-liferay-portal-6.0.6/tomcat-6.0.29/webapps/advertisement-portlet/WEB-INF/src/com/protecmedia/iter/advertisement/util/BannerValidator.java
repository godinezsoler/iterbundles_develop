/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.advertisement.util;

import java.util.List;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.protecmedia.iter.base.service.util.IterKeys;

public class BannerValidator {

	private static Log _log = LogFactoryUtil.getLog(BannerValidator.class);
	
	public static boolean validate(String width, String height, String source, String sourceFileName, String type, List<String> errors) {
		boolean valid = true;
			    		
	    if (!Validator.isNumber(height)) {
	    	errors.add("banner-portlet-error-height-must-be-number");	    	
	    	valid = false;
	    }
	    
	    if (!Validator.isNumber(width)) {
	    	errors.add("banner-portlet-error-width-must-be-number");	    
	    	valid = false;
	    }
	    
	    _log.debug(source + " " + sourceFileName);
		
	    if ("file".equals(source) && !"".equals(sourceFileName)) {
	    	
	    	int dotPos = sourceFileName.lastIndexOf(".");
		    String extension = sourceFileName.substring(dotPos);
		    
		    if (type.equals(IterKeys.BANNERFLASH) && !extension.equals(".swf")) {
		    	errors.add("banner-portlet-error-flash-extension");	    
		    	valid = false;	    	
		    }
	    }
	    
		return valid;
	}
	
}
