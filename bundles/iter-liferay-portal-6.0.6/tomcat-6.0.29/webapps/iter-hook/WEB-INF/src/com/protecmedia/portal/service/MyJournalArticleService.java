package com.protecmedia.portal.service;

import java.io.File;
import java.util.Map;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleServiceWrapper;
import com.liferay.portlet.journal.service.JournalArticleService;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;

public class MyJournalArticleService extends JournalArticleServiceWrapper {
	/* (non-Java-doc)
	 * @see com.liferay.portlet.journal.service.JournalArticleServiceWrapper#JournalArticleServiceWrapper(JournalArticleService journalArticleService)
	 */
	public MyJournalArticleService(JournalArticleService journalArticleService) {
		super(journalArticleService);
	}
	
	@Override 
	public JournalArticle getArticle(long groupId, String articleId) throws PortalException, SystemException{
		try{
			return super.getArticle(groupId, articleId);
		} catch(Exception ex){
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_GET_ARTICLE_ZYX, ex));
		}
		
	}

}