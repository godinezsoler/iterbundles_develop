package com.protecmedia.portal.service;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.LayoutServiceWrapper;
import com.liferay.portal.service.LayoutService;
import com.liferay.portal.service.ServiceContext;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;

public class MyLayoutService extends LayoutServiceWrapper {
	/* (non-Java-doc)
	 * @see com.liferay.portal.service.LayoutServiceWrapper#LayoutServiceWrapper(LayoutService layoutService)
	 */
	public MyLayoutService(LayoutService layoutService) {
		super(layoutService);
	}
	
	@Override
	public Layout addLayout(
			long groupId, boolean privateLayout, long parentLayoutId, 
			String name, String title, String description, String type, boolean hidden,
			String friendlyURL, ServiceContext serviceContext) throws PortalException, SystemException{
		try{
			return super.addLayout(groupId, privateLayout, parentLayoutId, name, title, description, type, hidden, friendlyURL, serviceContext);
		} catch(Exception ex){
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_ADD_LAYOUT_ZYX, ex));
		}
	}
	
	@Override
	public Layout updateName(long plid, String name, String languageId) throws PortalException, SystemException{
		try{
			return super.updateName(plid, name, languageId);
		} catch(Exception ex){
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_UPDATE_NAME_ZYX, ex));
		}
	}
	
	@Override
	public void deleteLayout(long plid) throws PortalException, SystemException{
		try{
			super.deleteLayout(plid);
		} catch(Exception ex){
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_DELETE_LAYOUT_ZYX, ex));
		}
	}
}