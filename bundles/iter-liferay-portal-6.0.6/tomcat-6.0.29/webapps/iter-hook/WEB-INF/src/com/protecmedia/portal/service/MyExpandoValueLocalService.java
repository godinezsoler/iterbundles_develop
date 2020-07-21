package com.protecmedia.portal.service;

import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portlet.expando.model.ExpandoRow;
import com.liferay.portlet.expando.model.ExpandoTable;
import com.liferay.portlet.expando.model.ExpandoValue;
import com.liferay.portlet.expando.service.ExpandoRowLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoTableLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoValueLocalService;
import com.liferay.portlet.expando.service.ExpandoValueLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoValueLocalServiceWrapper;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.expando.ExpandoValueXmlIO;


public class MyExpandoValueLocalService extends ExpandoValueLocalServiceWrapper {
	
	private static Log _log = LogFactoryUtil.getLog(MyExpandoValueLocalService.class);
	private static ItemXmlIO itemXmlIO = new ExpandoValueXmlIO();
	
	
	public MyExpandoValueLocalService(ExpandoValueLocalService expandoValueLocalService) {
		super(expandoValueLocalService);		
	}
	
	/*
	 * Add Functions 
	 * --------------
	 * 
	 * Live table:
	 *  LocalId -> rowId
	 */	
	@Override
	public ExpandoValue addValue(long classNameId, long tableId, long columnId, 
			long classPK, String data) throws PortalException, SystemException {

		ExpandoValue value = super.addValue(classNameId, tableId, columnId, classPK, data);
		
		try{
			//Check if the element exists in CUSTOMFIELD_CLASSNAME_TYPES
			if (ArrayUtils.contains(IterKeys.CUSTOMFIELD_CLASSNAME_TYPES, value.getClassName())){			
				itemXmlIO.createLiveEntry(value);
			}
		}catch(Exception e){
			_log.error("CreateLiveEntry, ExpandoValue, valueId=" + value.getValueId());
		}

		return value;
	}
	
	//Utilizada por MILENIUM
	@Override
	public ExpandoValue addValue(
			long companyId, String className, String tableName,
			String columnName, long classPK, Object data)
		throws PortalException, SystemException {
		
		ExpandoValue value = super.addValue(companyId, className, tableName, columnName, classPK, data);	

		try{
			//Check if the element exists in CUSTOMFIELD_CLASSNAME_TYPES
			if (ArrayUtils.contains(IterKeys.CUSTOMFIELD_CLASSNAME_TYPES, value.getClassName())){			
				itemXmlIO.createLiveEntry(value);
			}
		}catch(Exception e){
			_log.error("CreateLiveEntry, ExpandoValue, valueId=" + value.getValueId());
		}		

		return value;
	}
	
	@Override
	public ExpandoValue addValue(
			long companyId, String className, String tableName,
			String columnName, long classPK, String data)
		throws PortalException, SystemException {
		
		ExpandoValue value = super.addValue(companyId, className, tableName, columnName, classPK, data);	

		try{
			//Check if the element exists in CUSTOMFIELD_CLASSNAME_TYPES
			if (ArrayUtils.contains(IterKeys.CUSTOMFIELD_CLASSNAME_TYPES, value.getClassName())){			
				itemXmlIO.createLiveEntry(value);
			}
		}catch(Exception e){
			_log.error("CreateLiveEntry, ExpandoValue, valueId=" + value.getValueId());
		}		

		return value;
	}
	
	/*
	 * Delete Functions
	 */	
	@Override
	public void deleteValue(long valueId) throws PortalException, SystemException 
	{
		try
		{
			ExpandoValue expandoValue = ExpandoValueLocalServiceUtil.getValue(valueId);
						
			//Check if the element exists in CUSTOMFIELD_CLASSNAME_TYPES
			if (ArrayUtils.contains(IterKeys.CUSTOMFIELD_CLASSNAME_TYPES, expandoValue.getClassName()))
			{			
				itemXmlIO.deleteLiveEntry(expandoValue);				
			}
		}
		catch(Exception e)
		{
			_log.error("DeleteLiveEntry, ExpandoValue, valueId=" + valueId);
		}
		
		super.deleteValue(valueId);
	}
	
	@Override
	public void deleteValues(String className, long classPK) throws SystemException 
	{
		deleteValues(className, classPK, false);
	}
	
	public void deleteValues(String className, long classPK, boolean deleteJustNow) throws SystemException
	{
		//Check if the element exists in CUSTOMFIELD_CLASSNAME_TYPES
		if (ArrayUtils.contains(IterKeys.CUSTOMFIELD_CLASSNAME_TYPES, className))
		{
			try
			{
				List<Company> companies = CompanyLocalServiceUtil.getCompanies();		
				Company company = companies.get(0);
				ExpandoTable table = ExpandoTableLocalServiceUtil.getDefaultTable(company.getCompanyId(), className);
				ExpandoRow row = ExpandoRowLocalServiceUtil.getRow(table.getTableId(), classPK);
				List<ExpandoValue> values = ExpandoValueLocalServiceUtil.getRowValues(row.getRowId());
				
				for(ExpandoValue value : values)
				{
					itemXmlIO.deleteLiveEntry(value, deleteJustNow);
				}
				
				//Delete Expando Row
				ExpandoRowLocalServiceUtil.deleteRow(row.getTableId(), classPK, deleteJustNow);
			}
			catch(Exception e)
			{
				_log.debug("delete Values - no expando row exists", e);
			}
		}
		
		super.deleteValues(className, classPK);
	}
}