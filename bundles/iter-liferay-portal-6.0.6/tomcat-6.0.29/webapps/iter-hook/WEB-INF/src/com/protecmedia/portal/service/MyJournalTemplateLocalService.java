package com.protecmedia.portal.service;



import java.io.File;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.journal.model.JournalTemplate;
import com.liferay.portlet.journal.service.JournalTemplateLocalService;
import com.liferay.portlet.journal.service.JournalTemplateLocalServiceWrapper;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.journal.JournalTemplateXmlIO;
import com.protecmedia.portal.util.JournalTemplateAdd;
import com.protecmedia.portal.util.JournalTemplateDelete;
import com.protecmedia.portal.util.JournalTemplateUpdate;


public class MyJournalTemplateLocalService extends JournalTemplateLocalServiceWrapper
{
	public MyJournalTemplateLocalService(JournalTemplateLocalService journalTemplateLocalService) 
	{
		super(journalTemplateLocalService);
	}
	
	
	/*
	 * Add Functions 
	 * -------------- 
	 */	
	@Override 
	public JournalTemplate addTemplate(long userId, long groupId, String templateId, boolean autoTemplateId, 
			String structureId, String name, String description, String xsl, boolean formatXsl, String langType, 
			boolean cacheable, boolean smallImage, String smallImageURL, File smallFile, ServiceContext serviceContext) throws PortalException, SystemException
	{
		return JournalTemplateAdd.invoke(userId, groupId, templateId, autoTemplateId, 
										 structureId, name, description, xsl, formatXsl, langType, 
										 cacheable, smallImage, smallImageURL, smallFile, serviceContext);
	}
	
	/*
	 * Update functions
	 */	
	public JournalTemplate updateTemplate(long groupId, String templateId, String structureId, String name, 
			String description, String xsl, boolean formatXsl, String langType, boolean cacheable, 
			boolean smallImage, String smallImageURL, File smallFile, ServiceContext serviceContext) throws PortalException, SystemException
	{
		return JournalTemplateUpdate.invoke(groupId, templateId, structureId, name, 
					 						description, xsl, formatXsl, langType, cacheable, 
					 						smallImage, smallImageURL, smallFile, serviceContext);
	}
	
	/*
	 * Delete functions
	 */
	@Override
	public void deleteTemplate(long groupId, String templateId) throws PortalException, SystemException
	{
		JournalTemplateDelete.invoke(groupId, templateId);
	}
}
