package com.protecmedia.portal.service;

import java.util.List;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portlet.asset.model.AssetCategoryProperty;
import com.liferay.portlet.asset.service.AssetCategoryPropertyLocalService;
import com.liferay.portlet.asset.service.AssetCategoryPropertyLocalServiceUtil;
import com.liferay.portlet.asset.service.AssetCategoryPropertyLocalServiceWrapper;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.asset.AssetCategoryPropertyXmlIO;

public class MyAssetCategoryPropertyLocalService extends AssetCategoryPropertyLocalServiceWrapper {

	private static Log _log = LogFactoryUtil.getLog(MyAssetCategoryPropertyLocalService.class);
	private static ItemXmlIO itemXmlIO = new AssetCategoryPropertyXmlIO();	

	public MyAssetCategoryPropertyLocalService(AssetCategoryPropertyLocalService assetCategoryPropertyLocalService) {
		super(assetCategoryPropertyLocalService);		
	}
	
	/*
	 * Add Functions 
	 * --------------	
	 */	
	
	//Utilizado por la interfaz Gráfica
	@Override
	public AssetCategoryProperty addCategoryProperty(
			long userId, long categoryId, String key, String value)
		throws PortalException, SystemException {

		AssetCategoryProperty categoryProperty = super.addCategoryProperty(userId, categoryId, key, value);
		
		try {
			//It's necessary to check the environment because in live, the categoryGroupId is not set when you have to use it.
			if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW)){
				
				//Add to Live
				itemXmlIO.createLiveEntry(categoryProperty);	
			}
		} catch (Exception e) {
			_log.error("Live Error", e);
		}	

		return categoryProperty;
	}	
	
	
	/*
	 * Delete functions
	 */	

	//Utilizado por: Interfaz Gráfica e Importación
	@Override
	public void deleteAssetCategoryProperty(AssetCategoryProperty assetCategoryProperty){
		try {			
			//Delete from Live
			itemXmlIO.deleteLiveEntry(assetCategoryProperty);
			
			super.deleteAssetCategoryProperty(assetCategoryProperty);		
			
		} catch (Exception e) {
			_log.error("Live Error", e);
		}
	}
	
	//Utilizado por la Interfaz Gráfica para borrar todas las propiedades pertenecientes a una categoría
	@Override
	public void deleteCategoryProperties(long entryId) throws SystemException {
		List<AssetCategoryProperty> categoryProperties = AssetCategoryPropertyLocalServiceUtil.getCategoryProperties(entryId);

		try {
			for (AssetCategoryProperty categoryProperty : categoryProperties) {
								
				//Delete from Live
				itemXmlIO.deleteLiveEntry(categoryProperty);	
			}
		} catch (Exception e) {
			_log.error("Live Error", e);
		}
		
		super.deleteCategoryProperties(entryId);
	}
}