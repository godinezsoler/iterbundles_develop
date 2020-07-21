package com.protecmedia.portal.service;


import org.apache.commons.lang.ArrayUtils;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portlet.expando.model.ExpandoTable;
import com.liferay.portlet.expando.service.ExpandoTableLocalService;
import com.liferay.portlet.expando.service.ExpandoTableLocalServiceWrapper;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.expando.ExpandoTableXmlIO;


public class MyExpandoTableLocalService extends ExpandoTableLocalServiceWrapper {

	private static Log _log = LogFactoryUtil.getLog(MyExpandoTableLocalService.class);
	private static ItemXmlIO itemXmlIO = new ExpandoTableXmlIO();
	
	
	public MyExpandoTableLocalService(ExpandoTableLocalService expandoTableLocalService) {
		super(expandoTableLocalService);		
	}
	
	/*
	 * Add Functions 
	 * --------------	
	 */	
	@Override
	public ExpandoTable addDefaultTable(long companyId, String className)
	throws PortalException, SystemException {
		
		ExpandoTable expandoTable = super.addDefaultTable(companyId, className);
		
		if (ArrayUtils.contains(IterKeys.CUSTOMFIELD_CLASSNAME_TYPES, className)){
			//Add to Live
			try{
				itemXmlIO.createLiveEntry(expandoTable);
			}catch(Exception e){
				_log.error("CreateLiveEntry, className=" + className + ", tableId" + expandoTable.getTableId());
			}
		}
		
		return expandoTable;
	}
	
	//Utilizada por MILENIUM
	@Override
	public ExpandoTable addTable(long companyId, String className, String name)
	throws PortalException, SystemException {

		ExpandoTable expandoTable = super.addTable(companyId, className, name);
		
		if (ArrayUtils.contains(IterKeys.CUSTOMFIELD_CLASSNAME_TYPES, className)){
			//Add to Live
			try{
				itemXmlIO.createLiveEntry(expandoTable);
			}catch(Exception e){
				_log.error("CreateLiveEntry, className=" + className + ", tableId" + expandoTable.getTableId());
			}
		}
		
		return expandoTable;
	}
	
}