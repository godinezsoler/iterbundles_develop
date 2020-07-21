package com.protecmedia.portal.service;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portlet.expando.model.ExpandoValue;
import com.liferay.portlet.expando.service.ExpandoValueLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoValueService;
import com.liferay.portlet.expando.service.ExpandoValueServiceWrapper;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;


public class MyExpandoValueService extends ExpandoValueServiceWrapper {

	public MyExpandoValueService(ExpandoValueService expandoValueService) {
		super(expandoValueService);		
	}
	
	/*
	 * Add Functions 
	 * --------------	
	 */	
	@Override
	public ExpandoValue addValue(
			long companyId, String className, String tableName,
			String columnName, long classPK, Object data)
		throws PortalException, SystemException {

		try{
			return ExpandoValueLocalServiceUtil.addValue(companyId, className, tableName, columnName, classPK, data);
		} catch(Exception ex){
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_ADD_PAGECONTENT_ZYX, ex));
		}
	}
	
	
}