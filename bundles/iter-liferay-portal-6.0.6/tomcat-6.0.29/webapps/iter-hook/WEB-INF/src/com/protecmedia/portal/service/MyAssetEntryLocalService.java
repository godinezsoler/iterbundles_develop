package com.protecmedia.portal.service;

import java.util.Date;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.asset.model.AssetEntry;
import com.liferay.portlet.asset.service.AssetEntryLocalService;
import com.liferay.portlet.asset.service.AssetEntryLocalServiceWrapper;
import com.liferay.portlet.asset.service.persistence.AssetEntryUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.asset.AssetEntryXmlIO;

public class MyAssetEntryLocalService extends AssetEntryLocalServiceWrapper {

	private static Log _log = LogFactoryUtil.getLog(MyAssetEntryLocalService.class);
	private static ItemXmlIO itemXmlIO = new AssetEntryXmlIO();	
	
	
	public MyAssetEntryLocalService(AssetEntryLocalService assetEntryLocalService) {
		super(assetEntryLocalService);		
	}
	
	/*
	 * Delete functions
	 */	
	@Override
	public void deleteEntry(AssetEntry entry)
	throws PortalException, SystemException 
	{
		//Add to live
		itemXmlIO.deleteLiveEntry(entry);
		super.deleteEntry(entry);
	}
	
	@Override
	public void deleteEntry(long entryId)
		throws PortalException, SystemException {
	
		AssetEntry entry = AssetEntryUtil.findByPrimaryKey(entryId);
	
		deleteEntry(entry);
	}
	
	@Override
	public void deleteEntry(String className, long classPK)
		throws PortalException, SystemException {
	
		long classNameId = PortalUtil.getClassNameId(className);
	
		AssetEntry entry = AssetEntryUtil.fetchByC_C(
			classNameId, classPK);
	
		if (entry != null) {
			deleteEntry(entry);
		}
	}
	
	@Override
	public void deleteEntry(String className, long classPK, boolean deleteJustNow)
		throws PortalException, SystemException {
		
		long classNameId = PortalUtil.getClassNameId(className);
		AssetEntry entry = AssetEntryUtil.fetchByC_C(classNameId, classPK);
		
		if (entry != null)
		{
			itemXmlIO.deleteLiveEntry(entry, deleteJustNow);
			super.deleteEntry(entry);
		}
	}
	
	/*
	 * Update Functions
	 */
	@Override
	public AssetEntry updateEntry(
			long userId, long groupId, String className, long classPK,
			long[] categoryIds, String[] tagNames)
		throws PortalException, SystemException {

		return updateEntry(
			userId, groupId, className, classPK, null, categoryIds, tagNames,
			true, null, null, null, null, null, null, null, null, null, 0, 0,
			null, false);
	}

	@Override
	public AssetEntry updateEntry(
			long userId, long groupId, String className, long classPK,
			String classUuid, long[] categoryIds, String[] tagNames,
			boolean visible, Date startDate, Date endDate, Date publishDate,
			Date expirationDate, String mimeType, String title,
			String description, String summary, String url, int height,
			int width, Integer priority, boolean sync)
		throws PortalException, SystemException {

		AssetEntry entry = super.updateEntry(userId, groupId, className, classPK, classUuid,
				categoryIds, tagNames, visible, startDate, endDate, publishDate, 
				expirationDate, mimeType, title, description, summary, url, height, width, 
				priority, sync);

		try {	
			//Add to Live
			itemXmlIO.createLiveEntry(entry);
		} catch (Exception e) {			
			_log.error("Update AssetEntry failed for " + classPK + " of type " + className);
		}

		return entry;
	}
}