/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.util;

import java.util.List;

import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.Validator;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
public class LiveConfigurationValidator {

	public static boolean validateLiveConfiguration(LiveConfiguration liveConfiguration, List<String> errors) throws SystemException, ServiceError {
		boolean valid = true;
		//Condiciones generales
		if (Validator.isNull(liveConfiguration.getLocalPath())) {
			errors.add("xmlio-live-configuration-local-path-required");			
			valid = false;
		}
		if (Validator.isNull(liveConfiguration.getRemoteIterServer2())) 
		{
			errors.add("xmlio-live-configuration-iter-remote-server-required");			
			valid = false;
		}		
		if(liveConfiguration.getRemotePath().equals(liveConfiguration.getLocalPath())){
			errors.add("xmlio-live-configuration-remote-path-cannot-be-same-as-local-required");			
			valid = false;
		}
		if (Validator.isNull(liveConfiguration.getRemoteUserId()) ||
			Validator.isNull(liveConfiguration.getRemoteUserPassword()) ||
			Validator.isNull(liveConfiguration.getRemoteUserName())) {
				errors.add("xmlio-live-configuration-all-remote-user-data-required");			
				valid = false;
		}
		//Si el metodo de salida es File System, el campo remote path no puede ser nulo
		if (liveConfiguration.getOutputMethod().equals(IterKeys.LIVE_CONFIG_OUTPUT_METHOD_FILE_SYSTEM) &&
				Validator.isNull(liveConfiguration.getRemotePath()))	{
			errors.add("xmlio-live-configuration-remote-path-required");			
			valid = false;
		}
		//Si el metodo de salida es FTP, los campos no pueden ser nulos
		if (liveConfiguration.getOutputMethod().equals(IterKeys.LIVE_CONFIG_OUTPUT_METHOD_FTP) && (
			Validator.isNull(liveConfiguration.getFtpUser()) ||
			Validator.isNull(liveConfiguration.getFtpPassword()) ||
			Validator.isNull(liveConfiguration.getFtpPath()))) {
				errors.add("xmlio-live-configuration-all-ftp-data-required");			
				valid = false;
		}
		//Si el tipo de destino es canal, el campo no puede ser nulo
		if (liveConfiguration.getDestinationType().equals(IterKeys.LIVE_CONFIG_DESTINATION_TYPE_CHANNEL) &&
			Validator.isNull(liveConfiguration.getRemoteChannelId())){
				errors.add("xmlio-live-configuration-remote-channel-id-required");			
				valid = false;
		}
		return valid;
	}
}	

