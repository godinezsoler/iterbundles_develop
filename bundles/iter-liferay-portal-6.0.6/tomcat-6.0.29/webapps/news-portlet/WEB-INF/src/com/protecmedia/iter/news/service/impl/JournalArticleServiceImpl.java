/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.protecmedia.iter.news.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.service.ServiceContext;
import com.protecmedia.iter.news.service.base.JournalArticleServiceBaseImpl;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

/**
 * The implementation of the journal article remote service.
 * 
 * <p>
 * All custom service methods should. be put in this class. Whenever methods are
 * added, rerun ServiceBuilder to copy their definitions into the
 * {@link com.protecmedia.iter.news.service.JournalArticleService}
 * interface.
 * </p>
 * 
 * <p>
 * Never reference this interface directly. Always use
 * {@link com.protecmedia.iter.news.service.JournalArticleServiceUtil}
 * to access the journal article remote service.
 * </p>
 * 
 * <p>
 * This is a remote service. Methods of this service are expected to have
 * security checks based on the propagated JAAS credentials because this service
 * can be accessed remotely.
 * </p>
 * 
 * @author Protecmedia
 * @see com.protecmedia.iter.news.service.base.JournalArticleServiceBaseImpl
 * @see com.protecmedia.iter.news.service.JournalArticleServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor = {SystemException.class, PortalException.class} )
public class JournalArticleServiceImpl extends JournalArticleServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(JournalArticleServiceImpl.class);
	
	public String addContent1(long userId, long groupId, String structureId,
			String templateId, String title, String description, String articleId, String content,
			String imagesName, byte[] imagesBinary, ServiceContext serviceContext)
			throws Exception
	{
		
		try
		{
			Map<String, byte[]> images = new HashMap<String, byte[]>();
			
			if (imagesName != null && !imagesName.equals("") && imagesBinary != null)
				images.put(imagesName, imagesBinary);
			
			return journalArticleLocalService.addContent(userId, groupId, structureId, templateId, title, description, articleId,
												content, images, serviceContext, true, null, true, null);
		}
		catch(Exception ex)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(ex));
		}
	}
	
	
	public String addContent2(long userId, long groupId, String structureId,
			String templateId, String title, String description, String articleId, String content,
			String imagesName, String imagesBinary, ServiceContext serviceContext)
			throws Exception 
	{
		try
		{
			Map<String, byte[]> images = new HashMap<String, byte[]>();
			
			if (imagesName != null && !imagesName.equals("") && imagesBinary != null && !imagesBinary.equals(""))
				images.put(imagesName, Base64.decode(imagesBinary));
			
			return journalArticleLocalService.addContent(userId, groupId, structureId, templateId, title, description, articleId, 
												content, images, serviceContext, true, null, true, null);
		}
		catch(Exception ex)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(ex));
		}
	}
	
	
	public String addContent3(long userId, long groupId, String structureId,
			String templateId, String title, String description, String articleId, String content,
			List<String> imagesName, List<String> imagesBinary, ServiceContext serviceContext) throws Exception 
	{
		return addContent4(userId, groupId, structureId, templateId, title, description, articleId, 
				   content, imagesName, imagesBinary, serviceContext, true);
	}
	
	public String addContent4(long userId, long groupId, String structureId,
			String templateId, String title, String description, String articleId, String content,
			List<String> imagesName, List<String> imagesBinary, ServiceContext serviceContext, boolean indexable) throws Exception 
	{
		return addContent5(userId, groupId, structureId, templateId, title, description, articleId, 
						   content, imagesName, imagesBinary, serviceContext, indexable, null);
	}

	public String addContent5(long userId, long groupId, String structureId,
			String templateId, String title, String description, String articleId, String content,
			List<String> imagesName, List<String> imagesBinary, ServiceContext serviceContext, boolean indexable, long[] mainAssetCategoryIds)
			throws Exception 
	{
		return addContent6(userId, groupId, structureId, templateId, title, description, articleId, 
				content, imagesName, imagesBinary, serviceContext, indexable, mainAssetCategoryIds, true);
	}
	
	public String addContent6(long userId, long groupId, String structureId,
			String templateId, String title, String description, String articleId, String content,
			List<String> imagesName, List<String> imagesBinary, ServiceContext serviceContext, boolean indexable, long[] mainAssetCategoryIds,
			boolean acceptComments)
			throws SystemException 
	{
		return addContent7(userId, groupId, structureId, templateId, title, description, articleId, 
					content, imagesName, imagesBinary, serviceContext, indexable, mainAssetCategoryIds, 
					acceptComments, null);
	}

	public String addContent7(long userId, long groupId, String structureId,
			String templateId, String title, String description, String articleId, String content,
			List<String> imagesName, List<String> imagesBinary, ServiceContext serviceContext, boolean indexable, long[] mainAssetCategoryIds,
			boolean acceptComments, String xml) throws SystemException 
	{
		String result = "";
		try
		{
			Map<String, byte[]> images = new HashMap<String, byte[]>();
			
			if (imagesName != null && imagesBinary != null)
			{
				for (int i = 0; i < imagesName.size(); i++)													
					images.put(imagesName.get(i), Base64.decode(imagesBinary.get(i)));
			}
			
			result = journalArticleLocalService.addContent(userId, groupId, structureId, templateId, title, description, 
														 articleId, content, images, serviceContext, indexable, mainAssetCategoryIds, 
														 acceptComments, xml);
		}
		catch(Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}

	public String updateContent(long userId, long groupId, String structureId,
			String templateId, String title, String articleId, String content,
			List<String> imagesName, List<byte[]> imagesBinary) throws Exception 
	{
		return updateContent2(userId, groupId, structureId, templateId, title, articleId, 
							  content, imagesName, imagesBinary, true);
	}

	public String updateContent2(long userId, long groupId, String structureId,
			String templateId, String title, String articleId, String content,
			List<String> imagesName, List<byte[]> imagesBinary, boolean indexable) throws Exception 
	{
		return updateContent3(userId, groupId, structureId, templateId, title, articleId, 
				  content, imagesName, imagesBinary, indexable, true);
	}
	
	public String updateContent3(long userId, long groupId, String structureId,
			String templateId, String title, String articleId, String content,
			List<String> imagesName, List<byte[]> imagesBinary, boolean indexable,
			boolean acceptComments) throws Exception 
	{
		return updateContent4(userId, groupId, structureId, templateId, title, 
								articleId, content, imagesName, imagesBinary, 
								indexable, acceptComments, null);
	}

	public String updateContent4(long userId, long groupId, String structureId,
			String templateId, String title, String articleId, String content,
			List<String> imagesName, List<byte[]> imagesBinary, boolean indexable,
			boolean acceptComments, String xml) throws Exception 
	{
		try
		{
			Map<String, byte[]> images = new HashMap<String, byte[]>();
			
			if (imagesName != null && imagesBinary != null)
			{
				for (int i = 0; i < imagesName.size(); i++)
					images.put(imagesName.get(i), imagesBinary.get(i));
			}
			
			String temp = journalArticleLocalService.updateContent(userId, groupId, structureId, templateId, title, 
																   articleId, content, images, indexable, acceptComments, xml);

			return temp;
		}
		catch(Exception ex)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(ex));
		}
	}
	
	public void deleteContent(long groupId, String contentId) throws Exception 
	{
		_log.debug("DeleteContent (groupId="+groupId+", contentId="+contentId+") - Start");
		long startTime = System.currentTimeMillis();
		try
		{
			journalArticleLocalService.deleteContent(groupId, contentId);
		} 
		catch(Exception ex)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(ex));
		}
		finally
		{
			_log.debug("DeleteContent (groupId="+groupId+", contentId="+contentId+") - Total time:"+ XMLIOUtil.toHMS(System.currentTimeMillis()-startTime));
		}
	}
	
	public void deleteContent2(long groupId, String contentId, boolean deleteFileEntry, boolean deleteFromLive) throws Exception
	{
		_log.debug("deleteContent2 (groupId="+groupId+", contentId="+contentId+", deleteFileEntry="+deleteFileEntry+", deleteFromLive="+deleteFromLive+") - Start");
		long startTime = System.currentTimeMillis();
		try
		{
			journalArticleLocalService.deleteContent2(groupId, contentId, deleteFileEntry, deleteFromLive);
		} 
		catch (SystemException se)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(se), se);
		}
		catch (PortalException pe)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(pe), pe);
		}
		catch(Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th), th);
		}
		finally
		{
			_log.debug("deleteContent2 (groupId="+groupId+", contentId="+contentId+", deleteFileEntry="+deleteFileEntry+", deleteFromLive="+deleteFromLive+") - Total time:"+ XMLIOUtil.toHMS(System.currentTimeMillis()-startTime));
		}
	}
	
	public boolean deleteJournalArticle(String contentId, boolean deleteFileEntry) throws SystemException
	{
		_log.debug("deleteJournalArticle (contentId="+contentId+", deleteFileEntry="+deleteFileEntry+") - Start");
		long startTime	= System.currentTimeMillis();
		boolean found	= false;
		
		try
		{
			found = journalArticleLocalService.deleteJournalArticle(contentId, deleteFileEntry);
		} 
		catch(Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml( th), th);
		}
		finally
		{
			_log.debug("deleteJournalArticle (contentId="+contentId+", deleteFileEntry="+deleteFileEntry+") - Total time:"+ XMLIOUtil.toHMS(System.currentTimeMillis()-startTime));
		}
		return found;
	}
	
	public boolean deleteJournalArticleAndRefresh(String articleId, String[] urls) throws SystemException
	{
		_log.info( String.format("deleteJournalArticleAndRefresh (articleId='%s') - Start", articleId) );
		
		long startTime	= System.currentTimeMillis();
		boolean found	= false;
		
		try
		{
			found = journalArticleLocalService.deleteJournalArticleAndRefresh(articleId, urls);
		} 
		catch (Throwable th)
		{
			_log.error(th);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml( th), th);
		}
		finally
		{
			_log.info( String.format("deleteJournalArticleAndRefresh (articleId='%s') - Total time:", articleId, XMLIOUtil.toHMS(System.currentTimeMillis()-startTime)) );
		}
		
		return found;
	}
	
	public void deleteContentFromPublicURL(String url, boolean deleteFileEntry) throws Exception
	{
		_log.debug("deleteContentFromPublicURL (url="+url+"\n deleteFileEntry="+deleteFileEntry+") - Start");
		long startTime = System.currentTimeMillis();
		try
		{
			journalArticleLocalService.deleteContentFromPublicURL(url, deleteFileEntry);
		} 
		catch (SystemException se)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(se), se);
		}
		catch (PortalException pe)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(pe), pe);
		}
		catch(Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th), th);
		}
		finally
		{
			_log.debug("deleteContentFromPublicURL (url="+url+"\n deleteFileEntry="+deleteFileEntry+") - Total time:"+ XMLIOUtil.toHMS(System.currentTimeMillis()-startTime));
		}
	}

	public void deleteContentFromPublicURL2(String url) throws Exception
	{
		deleteContentFromPublicURL(url, true);
	}
	
	public String deleteContentFromURL(String url, boolean deleteFileEntry) throws SystemException
	{
		_log.debug("deleteContentFromURL (url="+url+"\n deleteFileEntry="+deleteFileEntry+") - Start");
		long startTime = System.currentTimeMillis();
		String result = "";
		try
		{
			result = journalArticleLocalService.deleteContentFromURL(url, deleteFileEntry).asXML();
		} 
		catch(Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th), th);
		}
		finally
		{
			_log.debug("deleteContentFromURL (url="+url+"\n deleteFileEntry="+deleteFileEntry+") - Total time:"+ XMLIOUtil.toHMS(System.currentTimeMillis()-startTime));
		}
		return result;
	}

	public String reIndexJournalArticles(String xmlData) throws Exception
	{
		try
		{
			return journalArticleLocalService.reIndexJournalArticles(xmlData).asXML();
		}
		catch(Throwable th)
		{
			throw new SystemException(com.liferay.portal.kernel.error.ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void deleteIndexedArticles(String scopeGroupId) throws Exception
	{
		try
		{
			journalArticleLocalService.deleteIndexedArticles(scopeGroupId);
		}
		catch(Throwable th)
		{
			throw new SystemException(com.liferay.portal.kernel.error.ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String getIndexingProgress() throws Exception
	{
		String retVal = "";
		
		try
		{
			retVal = journalArticleLocalService.getIndexingProgress().asXML();
		}
		catch(Throwable th)
		{
			throw new SystemException(com.liferay.portal.kernel.error.ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return retVal;
	}
	
	public boolean stopIndexation() throws Exception
	{
		try
		{
			return journalArticleLocalService.stopIndexation();
		}
		catch(Throwable th)
		{
			throw new SystemException(com.liferay.portal.kernel.error.ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public String getEditArticle(String articleId) throws Exception
	{
		String result = "";
		
		try
		{
			result = journalArticleLocalService.getEditArticle(articleId).asXML();
		}
		catch(Throwable th)
		{
			throw new SystemException(com.liferay.portal.kernel.error.ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		
		return result;
	}

}
