package com.protecmedia.iter.xmlio.service.impl;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.protecmedia.iter.xmlio.service.base.CategoriesPropertiesPublicationServiceBaseImpl;

@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class CategoriesPropertiesPublicationServiceImpl extends CategoriesPropertiesPublicationServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(CategoriesPropertiesPublicationServiceImpl.class);
	
	public String importContents(String scopeGroupName, String exportxml) throws SystemException
	{
		String result = StringPool.BLANK;
		try
		{
			result = categoriesPropertiesPublicationLocalService.importContents(scopeGroupName, exportxml).asXML();
		}
		catch(Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		return result;
	}
}