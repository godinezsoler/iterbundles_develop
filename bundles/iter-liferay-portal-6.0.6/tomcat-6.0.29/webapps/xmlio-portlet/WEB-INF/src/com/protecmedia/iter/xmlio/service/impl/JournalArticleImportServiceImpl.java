package com.protecmedia.iter.xmlio.service.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Node;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.xmlio.service.base.JournalArticleImportServiceBaseImpl;

@Transactional(isolation = Isolation.PORTAL, rollbackFor = { Exception.class })
public class JournalArticleImportServiceImpl extends JournalArticleImportServiceBaseImpl {
	
	public void importArticle(Document xmlResult, Node article, String scopeGroupId, long globalGroupId, long defaultUserId, long jaClassNameId, 
        					  String expandoTableId, String expColGrp, String expColMeta, File workingDirectory, Date importationStart, 
        					  Date importationFinish, int maxImgWidth, int maxImgHeight, boolean legacyIsEncoded, boolean ifArticleExists,
        					  String ifNoCategory, boolean ifLayoutNotExists, String ifNoSuscription) throws Exception
    {
		try
		{			
			journalArticleImportLocalService.importArticle(xmlResult, article, scopeGroupId, globalGroupId, defaultUserId, jaClassNameId, 
		                                                   expandoTableId, expColGrp, expColMeta, workingDirectory, importationStart, 
		                             					   importationFinish, maxImgWidth, maxImgHeight, legacyIsEncoded, ifArticleExists,
		                             					  ifNoCategory, ifLayoutNotExists, ifNoSuscription);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}	
	}
	
	public void reindexArticleContent(long globalGrpId, Map<String, String> articlesToIndex) throws PortalException, SystemException, ServiceError
	{
		try
		{			
			journalArticleImportLocalService.reindexArticleContent(globalGrpId, articlesToIndex);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
	}
	
	public void deleteArticle(long groupId, String articleId, boolean deleteFiles) throws PortalException, SystemException, IllegalArgumentException, ClassNotFoundException, IllegalAccessException, InvocationTargetException, IOException, SQLException, com.liferay.portal.kernel.error.ServiceError, DocumentException{
		try
		{			
			
			journalArticleImportLocalService.deleteArticle(groupId, articleId, deleteFiles);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
	}
	
	public void deleteArticle(Document totailImportReport, long scopeGroupId, Node article) throws Exception 
	{
		try
		{			
			
			journalArticleImportLocalService.deleteArticle(totailImportReport, scopeGroupId, article);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
	}

}