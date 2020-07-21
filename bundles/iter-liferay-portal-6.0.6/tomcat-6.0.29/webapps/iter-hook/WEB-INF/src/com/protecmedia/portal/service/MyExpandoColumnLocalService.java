package com.protecmedia.portal.service;


import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.commons.lang.ArrayUtils;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portlet.expando.model.ExpandoColumn;
import com.liferay.portlet.expando.model.ExpandoColumnConstants;
import com.liferay.portlet.expando.service.ExpandoColumnLocalService;
import com.liferay.portlet.expando.service.ExpandoColumnLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoColumnLocalServiceWrapper;
import com.liferay.portlet.expando.service.ExpandoTableLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.expando.ExpandoColumnXmlIO;


public class MyExpandoColumnLocalService extends ExpandoColumnLocalServiceWrapper {

	private static Log _log = LogFactoryUtil.getLog(MyExpandoColumnLocalService.class);
	private static ItemXmlIO itemXmlIO = new ExpandoColumnXmlIO();
	
	
	public MyExpandoColumnLocalService(ExpandoColumnLocalService expandoColumnLocalService) {
		super(expandoColumnLocalService);		
	}
	
	/*
	 * Add Functions 
	 * --------------
	 */	
	
	@Override
	public ExpandoColumn addColumn(long tableId, String name, int type)
	throws PortalException, SystemException {
		return addColumn(tableId, name, type, null);
	}
	
	//Utilizada por MILENIUM
	@Override
	public ExpandoColumn addColumn(
			long tableId, String name, int type, Object defaultData)
		throws PortalException, SystemException {

		ExpandoColumn column = super.addColumn(tableId, name, type, defaultData);		 
		
		try{		
			if (ArrayUtils.contains(IterKeys.CUSTOMFIELD_CLASSNAME_TYPES, 
					ExpandoTableLocalServiceUtil.getTable(tableId).getClassName())){
				//Add to Live
				itemXmlIO.createLiveEntry(column);			
			}
		}catch(Exception e){
			_log.error("CreateLiveEntry, ExpandoColumn, columnId" + column.getColumnId());
		}
		
		return column;
	}
	
	
	/*
	 * Update Functions 
	 * --------------
	 */
	
	@Override
	public ExpandoColumn updateColumn(long columnId, String name, int type)
	throws PortalException, SystemException {
		
		return updateColumn(columnId, name, type, null);
	}
	
	//Utilizada por MILENIUM
	@Override
	public ExpandoColumn updateColumn(
			long columnId, String name, int type, Object defaultData)
		throws PortalException, SystemException {
		
		ExpandoColumn column = ExpandoColumnLocalServiceUtil.getColumn(columnId);

		try{
			if (ArrayUtils.contains(IterKeys.CUSTOMFIELD_CLASSNAME_TYPES, 
					ExpandoTableLocalServiceUtil.getTable(column.getTableId()).getClassName())){
				//Add to Live
				itemXmlIO.createLiveEntry(column);			
			}
		}catch(Exception e){
			_log.error("CreateLiveEntry, ExpandoColumn, columnId" + column.getColumnId());
		}
				
		//Arreglado: El validate del update no convierte bien string a date. (Liferay's fault)
		if (type == ExpandoColumnConstants.DATE) {
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:SS");			
			defaultData = GetterUtil.getDate((String)defaultData, dateFormat);			
		}
		return super.updateColumn(columnId, name, type, defaultData);
	}
	
	/*
	 * Delete Functions 
	 * --------------
	 */
	
	@Override
	public void deleteColumn(long columnId)
	throws PortalException, SystemException {

		ExpandoColumn column = ExpandoColumnLocalServiceUtil.getColumn(columnId);		
		
		deleteColumn(column);
	}
	
	//Utilizada por MILENIUM
	@Override
	public void deleteColumn(ExpandoColumn column) throws SystemException {
	
		try{
			if (ArrayUtils.contains(IterKeys.CUSTOMFIELD_CLASSNAME_TYPES, 
					ExpandoTableLocalServiceUtil.getTable(column.getTableId()).getClassName())){
				//Delete from Live
				itemXmlIO.deleteLiveEntry(column);	
			}
		}catch(Exception e){
			_log.error("DeleteLiveEntry, ExpandoColumn, columnId" + column.getColumnId());
		}
		
		super.deleteColumn(column);
	}
}