/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.protecmedia.iter.xmlio.model.impl;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.util.PropsValues;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;

/**
 * The model implementation for the LiveConfiguration service. Represents a row in the &quot;Xmlio_LiveConfiguration&quot; database table, with each column mapped to a property of this class.
 *
 * <p>
 * Helper methods and all application logic should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.model.LiveConfiguration} interface.
 * </p>
 *
 * <p>
 * Never reference this class directly. All methods that expect a live configuration model instance should use the {@link LiveConfiguration} interface instead.
 * </p>
 */
public class LiveConfigurationImpl extends LiveConfigurationModelImpl implements LiveConfiguration 
{
	public LiveConfigurationImpl() 
	{
	}
	
	public String getRemoteIterServer2() throws ServiceError 
	{
		String remoteIterServer = super.getRemoteIterServer();
		
		ErrorRaiser.throwIfFalse( !(PropsValues.IS_PREVIEW_ENVIRONMENT && 
								  remoteIterServer.equalsIgnoreCase("LIVE_DOWN")), IterErrorKeys.XYZ_E_LIVE_DOWN_ZYX );
		
		return remoteIterServer;
	}
}