package com.protecmedia.portal.service;

import java.util.List;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portlet.asset.model.AssetCategoryProperty;
import com.liferay.portlet.asset.service.AssetCategoryPropertyServiceWrapper;
import com.liferay.portlet.asset.service.AssetCategoryPropertyService;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;

public class MyAssetCategoryPropertyService extends AssetCategoryPropertyServiceWrapper {
	/* (non-Java-doc)
	 * @see com.liferay.portlet.asset.service.AssetCategoryPropertyServiceWrapper#AssetCategoryPropertyServiceWrapper(AssetCategoryPropertyService assetCategoryPropertyService)
	 */
	public MyAssetCategoryPropertyService(AssetCategoryPropertyService assetCategoryPropertyService) {
		super(assetCategoryPropertyService);
	}
	
	@Override
	public AssetCategoryProperty addCategoryProperty(long entryId, String key, String value) throws PortalException, SystemException{
		try{
			return super.addCategoryProperty(entryId, key, value);
		} catch(Exception ex){
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_ADD_CATEGORY_PROPERTY_ZYX, ex));
		}
	}
	
	@Override
	public List<AssetCategoryProperty> getCategoryProperties(long entryId) throws SystemException {
		try{
			return super.getCategoryProperties(entryId);
		} catch(Exception ex){
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_GET_CATEGORY_PROPERTIES_ZYX, ex));
		}
	}

	@Override
	public AssetCategoryProperty updateCategoryProperty(long categoryPropertyId, String key, String value) throws PortalException, SystemException{
		try{
			return super.updateCategoryProperty(categoryPropertyId, key, value);
		} catch(Exception ex){
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_UPDATE_CATEGORY_PROPERTY_ZYX, ex));
		}
	}
}