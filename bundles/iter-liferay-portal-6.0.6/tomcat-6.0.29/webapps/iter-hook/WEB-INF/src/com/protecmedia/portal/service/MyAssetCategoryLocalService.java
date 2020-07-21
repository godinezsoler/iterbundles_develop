package com.protecmedia.portal.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.asset.NoSuchCategoryException;
import com.liferay.portlet.asset.model.AssetCategory;
import com.liferay.portlet.asset.model.AssetVocabulary;
import com.liferay.portlet.asset.service.AssetCategoryLocalService;
import com.liferay.portlet.asset.service.AssetCategoryLocalServiceWrapper;
import com.liferay.portlet.asset.service.AssetVocabularyLocalServiceUtil;
import com.liferay.portlet.asset.service.persistence.AssetCategoryPropertyUtil;
import com.liferay.portlet.asset.service.persistence.AssetCategoryUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.asset.AssetCategoryXmlIO;

public class MyAssetCategoryLocalService extends AssetCategoryLocalServiceWrapper {

	private static Log _log = LogFactoryUtil.getLog(MyAssetCategoryLocalService.class);
	private static ItemXmlIO itemXmlIO = new AssetCategoryXmlIO();	
	
	public MyAssetCategoryLocalService(AssetCategoryLocalService assetCategoryLocalService) {
		super(assetCategoryLocalService);		
	}
	
	/*
	 * Add Functions 
	 * --------------	
	 */	
	
	//Utilizado por: Interfaz Gráfica y MILENIUM
	@Override
	public AssetCategory addCategory(long userId, long parentCategoryId, Map<Locale, String> titleMap,
									 long vocabularyId, String[] categoryProperties, ServiceContext serviceContext) throws PortalException, SystemException 
	{
		AssetCategory category = null;
		String name = titleMap.get(LocaleUtil.getDefault());
		
		// 0010628: Fallo al crear desde MLN categorías que ya existen en ITER
		try
		{
			// Se comprueba si no existe ya en ITER una categoría en el padre con el mismo nombre, y en tal caso se reutiliza
			category = AssetCategoryUtil.findByP_N_V(parentCategoryId, name, vocabularyId);
		}
		catch (NoSuchCategoryException nsce){}
			
		if (category == null || !category.getName().equals(name) )	
			// No existe una categoría así, se crea. O si existe una categoría así pero NO tiene EXACTAMENTE el mismo nombre, 
			// se intenta crear para que salten los mecanismos de comprobación de duplicidad. Ver #0010628 (0021171)
			category = super.addCategory(userId, parentCategoryId, titleMap, vocabularyId, categoryProperties, serviceContext);
		else
			category = updateCategory(userId, category.getCategoryId(), parentCategoryId, titleMap, vocabularyId, categoryProperties, serviceContext);
		
		try 
		{
			itemXmlIO.createLiveEntry(category);
		}
		catch (Exception e) 
		{
			_log.error("Live Error!", e);
		}		

		return category;
	}
	
	/*
	 * Delete functions
	 */	

	//Utilizado por: Interfaz Gráfica y MILENIUM
	@Override
	public void deleteCategory(AssetCategory category)
	throws PortalException, SystemException {

		try {			
			//delete Live
			itemXmlIO.deleteLiveEntry(category);
						
			// Categories
			List<AssetCategory> categories = AssetCategoryUtil.findByParentCategoryId(category.getCategoryId());

			for (AssetCategory curCategory : categories) {
				deleteCategory(curCategory);
			}
			
		} catch (Exception e) {
			_log.error("Live Error", e);
		}
		
		super.deleteCategory(category);
	}

	
	@Override
	public void deleteCategory(long categoryId)
	throws PortalException, SystemException {

		AssetCategory category = AssetCategoryUtil.findByPrimaryKey(
			categoryId);
	
		deleteCategory(category);
	}
	
	@Override
	public void deleteVocabularyCategories(long vocabularyId)
		throws PortalException, SystemException {
	
		List<AssetCategory> categories = AssetCategoryUtil.findByVocabularyId(vocabularyId);			
	
		for (AssetCategory category : categories) {
			deleteCategory(category);
		}
	}

	
	
	/*
	 * Update Functions
	 */
	
	//Utilizado por: Interfaz Gráfica y MILENIUM
	@Override
	public AssetCategory updateCategory(
			long userId, long categoryId, long parentCategoryId,
			Map<Locale, String> titleMap, long vocabularyId,
			String[] categoryProperties, ServiceContext serviceContext) throws PortalException, SystemException 
	{
		AssetCategory category = super.updateCategory(userId, categoryId, parentCategoryId, titleMap, vocabularyId, categoryProperties, serviceContext);
		
		AssetVocabulary vocabulary = AssetVocabularyLocalServiceUtil.getAssetVocabulary(vocabularyId);
		
		/** 0009493: Borrado masivo de metadatos genera transaccion de millones de registros
	    En el Back no se desctiva este caso de actualizacion. Solo en el Live, cuyo metodo adecuado de incorporar categorias
	    es la Publicacion/Importacion de contenidos, y al ser en bloque ya se actualizara al final de dicho proceso **/
		if (PropsValues.ITER_ENVIRONMENT.equals(WebKeys.ENVIRONMENT_PREVIEW) ||	PropsValues.ITER_ASSET_CATEGORIES_REBUILDTREE_CONTINUALLY) 
		{
			super.rebuildTree(vocabulary.getGroupId(), true);
		}
		
		try 
		{			
			//Add to live
			itemXmlIO.createLiveEntry(category);
			
			/*
			// Properties
			List<AssetCategoryProperty> oldCategoryProperties = AssetCategoryPropertyUtil.findByCategoryId(categoryId);
				
			for (AssetCategoryProperty categoryProperty : oldCategoryProperties) {
				String idprop = String.valueOf(categoryProperty.getCategoryPropertyId());
				//insercion en la tabla de LIVE
				LiveLocalServiceUtil.add(IterKeys.CLASSNAME_CATEGORYPROPERTY, av.getGroupId(), 
						IterLocalServiceUtil.getSystemName() + "_" + idprop, idprop, 
						IterKeys.DELETE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);		
			}
			 */
		} 
		catch (Exception e) 
		{
			_log.error("Live Error", e);
		}
		
		return category;
	}
}