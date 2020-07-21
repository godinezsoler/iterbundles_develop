package com.protecmedia.portal.service;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.service.PortletItemLocalService;
import com.liferay.portal.service.PortletItemLocalServiceWrapper;
import com.protecmedia.iter.base.service.util.PortletPreferencesTools;
import com.protecmedia.iter.base.service.util.ServiceError;

public class MyPortletItemLocalService extends PortletItemLocalServiceWrapper 
{
	public MyPortletItemLocalService(PortletItemLocalService portletItemLocalService) 
	{
		super(portletItemLocalService);
	}
}
