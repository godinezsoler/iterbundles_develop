package com.protecmedia.portal.service;

import java.util.Date;
import java.util.List;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Website;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.WebsiteLocalService;
import com.liferay.portal.service.WebsiteLocalServiceUtil;
import com.liferay.portal.service.WebsiteLocalServiceWrapper;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.news.model.Qualification;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.journal.JournalArticleXmlIO;
import com.protecmedia.iter.xmlio.service.item.portal.WebsiteXmlIO;

public class MyWebsiteLocalService extends WebsiteLocalServiceWrapper {
	
	private static Log _log = LogFactoryUtil.getLog(MyWebsiteLocalService.class);
	private static ItemXmlIO itemXmlIO = new WebsiteXmlIO();
	
	public MyWebsiteLocalService(WebsiteLocalService websiteLocalService) {
		super(websiteLocalService);		
	}
	
	/*
	 * Add Functions 
	 * --------------	
	 */	
	
	@Override
	public Website addWebsite(
			long userId, String className, long classPK, String url, int typeId,
			boolean primary)
		throws PortalException, SystemException {
		
		Website website = super.addWebsite(userId, className, classPK, url, typeId, primary);

		if(className.equals(IterKeys.CLASSNAME_CONTACT)){
			try {			
				itemXmlIO.createLiveEntry(website);					
			} catch (Exception e) {
				_log.error(e);
			}
		}
		
		return website;
	}
	
	/*
	 * Delete functions
	 */	
	
	@Override
	public void deleteWebsite(long websiteId)
	throws PortalException, SystemException {

		try {
			Website website = WebsiteLocalServiceUtil.getWebsite(websiteId);
			
			if(website.getClassName().equals(IterKeys.CLASSNAME_CONTACT)){
				itemXmlIO.deleteLiveEntry(website);
			}
		} catch (Exception e) {
			_log.error(e);
		}

		super.deleteWebsite(websiteId);
	}
	
	@Override
	public void deleteWebsites(long companyId, String className, long classPK)
		throws SystemException {
	
		if(className.equals(IterKeys.CLASSNAME_CONTACT)){
			try {
				List <Website> websites = WebsiteLocalServiceUtil.getWebsites(companyId, className, classPK);
				
				for (Website website : websites){
					itemXmlIO.deleteLiveEntry(website);
				}
			} catch (Exception e) {
				_log.error(e);
			}
		}
		
		super.deleteWebsites(companyId, className, classPK);
	}
	
	/*
	 * Update Functions
	 */
		
	@Override
	public Website updateWebsite(
			long websiteId, String url, int typeId, boolean primary)
		throws PortalException, SystemException {

		try {
			Website website = WebsiteLocalServiceUtil.getWebsite(websiteId);
			
			if(website.getClassName().equals(IterKeys.CLASSNAME_CONTACT)){
				itemXmlIO.createLiveEntry(website);
			}
		} catch (Exception e) {
			_log.error(e);
		}

		return super.updateWebsite(websiteId, url, typeId, primary);
	}
}