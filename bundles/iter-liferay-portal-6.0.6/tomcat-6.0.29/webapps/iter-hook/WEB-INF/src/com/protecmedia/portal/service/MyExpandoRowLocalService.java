package com.protecmedia.portal.service;


import org.apache.commons.lang.ArrayUtils;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portlet.expando.model.ExpandoRow;
import com.liferay.portlet.expando.service.ExpandoRowLocalService;
import com.liferay.portlet.expando.service.ExpandoRowLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoRowLocalServiceWrapper;
import com.liferay.portlet.expando.service.ExpandoTableLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.expando.ExpandoRowXmlIO;


public class MyExpandoRowLocalService extends ExpandoRowLocalServiceWrapper {

	private static Log _log = LogFactoryUtil.getLog(MyExpandoRowLocalService.class);
	private static ItemXmlIO itemXmlIO = new ExpandoRowXmlIO();
	
	public MyExpandoRowLocalService(ExpandoRowLocalService expandoRowLocalService) {
		super(expandoRowLocalService);		
	}
			
	/*
	 * Delete Functions
	 */
	@Override
	public void deleteRow(long tableId, long classPK) throws PortalException, SystemException 
	{
		deleteRow(tableId, classPK, false);
	}
	
	@Override
	public void deleteRow(long tableId, long classPK, boolean deleteJustNow) throws PortalException, SystemException 
	{
		try
		{
			ExpandoRow expandoRow = ExpandoRowLocalServiceUtil.getRow(tableId, classPK);
			
			if (ArrayUtils.contains(IterKeys.CUSTOMFIELD_CLASSNAME_TYPES, ExpandoTableLocalServiceUtil.getTable(tableId).getClassName()))
			{
				itemXmlIO.deleteLiveEntry(expandoRow, deleteJustNow);
			}
		}
		catch(Exception e)
		{
			_log.error("DeleteLiveEntry, ExpandoRow, tableId)" + tableId + ", classPK=" + classPK);
		}
	
		super.deleteRow(tableId, classPK);
	}
}