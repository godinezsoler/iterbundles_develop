/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.util;

import java.util.List;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.Validator;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Channel;

public class ChannelValidator {
	
	public static boolean validateChannel(Channel channel, List<String> errors) throws SystemException {
		boolean valid = true;
		//Condiciones generales
		if (Validator.isNull(channel.getName())) {
			errors.add("xmlio-channel-name-required");			
			valid = false;
		}
		
		if (Validator.isNull(channel.getDescription())) {
			errors.add("xmlio-channel-description-required");			
			valid = false;
		}
		if (Validator.isNull(channel.getFilePath())) {
			errors.add("xmlio-channel-file-path-required");			
			valid = false;
		}
		//Casos especiales
		if (channel.getMode().equals(IterKeys.XMLIO_CHANNEL_MODE_FTP) && 
				(
				Validator.isNull(channel.getFtpUser()) ||
				Validator.isNull(channel.getFtpPassword()) ||
				Validator.isNull(channel.getFtpServer()))){
				errors.add("xmlio-channel-all-ftp-data-required");			
				valid = false;
		}
		if (channel.getProgram() && (channel.getProgramHour() == -1 || channel.getProgramMin() == -1)){
				errors.add("xmlio-channel-program-time-required");			
				valid = false;
		}
		return valid;
	}
		
}
