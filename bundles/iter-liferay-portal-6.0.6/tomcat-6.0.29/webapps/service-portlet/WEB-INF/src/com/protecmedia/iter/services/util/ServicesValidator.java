/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.services.util;

import java.util.ArrayList;
import java.util.List;

import com.liferay.portal.kernel.util.Validator;
import com.protecmedia.iter.services.model.Service;
import com.protecmedia.iter.services.model.impl.ServiceImpl;


public class ServicesValidator {
	public static boolean validateServices(Service service, List<String> errors) {
		boolean valid = true;		
		
		if (Validator.isNull(service.getTitle()) || service.getTitle().equals("")) {
			errors.add("servicetitle-required");
			valid = false;
		}
		
		if (service.getLinkId() == -1) {
			errors.add("serviceurl-required");
			valid = false;
		}
		
		if (service.getImageId() == -1) {
			errors.add("serviceimage-required");
			valid = false;
		}
		
		return valid;
	}

	public static boolean validateServicesAdd(Service service, byte[] imageBytes, ArrayList<String> errors) {
						
		boolean valid = true;		
		
		if (Validator.isNull(service.getTitle()) || service.getTitle().equals("")) {
			errors.add("servicetitle-required");		
			valid = false;
		}
		
		if (service.getLinkId() == -1) {
			errors.add("serviceurl-required");
			valid = false;
		}
		
		if (service.getImageId() == -1 || imageBytes == null) {
			errors.add("serviceimage-required");
			valid = false;
		}				
		
		return valid;
	}
}
