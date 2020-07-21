package com.protecmedia.portal.service;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portlet.expando.model.ExpandoColumn;
import com.liferay.portlet.expando.service.ExpandoColumnServiceWrapper;
import com.liferay.portlet.expando.service.ExpandoColumnService;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;

public class MyExpandoColumnService extends ExpandoColumnServiceWrapper {
	/* (non-Java-doc)
	 * @see com.liferay.portlet.expando.service.ExpandoColumnServiceWrapper#ExpandoColumnServiceWrapper(ExpandoColumnService expandoColumnService)
	 */
	public MyExpandoColumnService(ExpandoColumnService expandoColumnService) {
		super(expandoColumnService);
	}
	
	@Override
	public ExpandoColumn addColumn(long tableId, String name, int type) throws PortalException, SystemException{
		try{
			return super.addColumn(tableId, name, type);
		} catch(Exception ex){
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_ADD_COLUMN_ZYX, ex));
		}
	}
	
	@Override 
	public ExpandoColumn updateColumn(long columnId, String name, int type) throws PortalException, SystemException{
		try{
			return super.updateColumn(columnId, name, type);
		} catch(Exception ex){
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_UPDATE_COLUMN_ZYX, ex));
		}
	}
	
	@Override
	public void deleteColumn(long columnId) throws PortalException, SystemException{
		try{
			super.deleteColumn(columnId);
		} catch(Exception ex){
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_DELETE_COLUMN_ZYX, ex));
		}
	}

}