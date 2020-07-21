package com.protecmedia.portal.service;

import java.util.Locale;
import java.util.Map;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.asset.model.AssetVocabulary;
import com.liferay.portlet.asset.service.AssetVocabularyLocalService;
import com.liferay.portlet.asset.service.AssetVocabularyLocalServiceWrapper;
import com.liferay.portlet.asset.service.persistence.AssetVocabularyUtil;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.asset.AssetVocabularyXmlIO;

public class MyAssetVocabularyLocalService extends AssetVocabularyLocalServiceWrapper {

	private static Log _log = LogFactoryUtil.getLog(MyAssetVocabularyLocalService.class);
	private static ItemXmlIO itemXmlIO = new AssetVocabularyXmlIO();	
	
	public MyAssetVocabularyLocalService(AssetVocabularyLocalService assetVocabularyLocalService) {
		super(assetVocabularyLocalService);		
	}
	
	/*
	 * Add Functions 
	 * --------------	
	 */	
	
	//Utilizada por: Interfaz Gráfica y MILENIUM
	@Override
	public AssetVocabulary addVocabulary(
			long userId, Map<Locale, String> titleMap,
			Map<Locale, String> descriptionMap, String settings,
			ServiceContext serviceContext)
		throws PortalException, SystemException {

		AssetVocabulary vocabulary = super.addVocabulary(userId, titleMap, descriptionMap, settings, serviceContext);
		
		try {		
			//Add to Live
			itemXmlIO.createLiveEntry(vocabulary);
								
		} catch (Exception e) {
			_log.error("Live Error", e);
		}

		return vocabulary;
	}
	
	/*
	 * Delete functions
	 */	
	@Override
	public void deleteVocabulary(long vocabularyId)
	throws PortalException, SystemException {

		AssetVocabulary vocabulary =
			AssetVocabularyUtil.findByPrimaryKey(vocabularyId);
	
		deleteVocabulary(vocabulary);
	}
	
	//Utilizada por: Interfaz Gráfica, importación y MILENIUM
	@Override
	public void deleteVocabulary(AssetVocabulary vocabulary)
	throws PortalException, SystemException {

		// Vocabulary
		try {		
			//Delete from Live
			LiveLocalServiceUtil.deleteLive(vocabulary);	
			
		} catch (Exception e) {
			_log.error("Live Error", e);
		}
			
		super.deleteVocabulary(vocabulary);
	}
	
	/*
	 * Update Functions
	 */
	
	//Utilizada por: Interfaz Gráfica y MILENIUM
	@Override
	public AssetVocabulary updateVocabulary(
			long vocabularyId, Map<Locale, String> titleMap,
			Map<Locale, String> descriptionMap, String settings,
			ServiceContext serviceContext)
		throws PortalException, SystemException {

		AssetVocabulary vocabulary =super.updateVocabulary(vocabularyId, titleMap, descriptionMap, settings, serviceContext);

		try {			
			//Add to Live
			itemXmlIO.createLiveEntry(vocabulary);
			
		} catch (Exception e) {
			_log.error("Live Error", e);
		}

		return vocabulary;
	}
	
}