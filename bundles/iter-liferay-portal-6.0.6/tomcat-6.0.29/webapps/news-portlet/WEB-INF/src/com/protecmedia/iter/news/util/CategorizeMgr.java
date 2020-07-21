package com.protecmedia.iter.news.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.advertisement.MetadataAdvertisementTools;
import com.liferay.portlet.asset.service.AssetCategoryLocalServiceUtil;
import com.liferay.portlet.asset.service.AssetVocabularyLocalServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journalcontent.util.JournalContentUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.news.service.CategorizeLocalServiceUtil;

public class CategorizeMgr implements Callable<JournalArticle>
{
	private static Log _log = LogFactoryUtil.getLog(CategorizeMgr.class);
	
	private Exception _e = null;
	private long _id = 0L;
	private String _className = "";
	
	private long _groupId = 0L;
	private long _userId = 0L;
	private String _articleId = "";
	private long[] _assetCategoryIds = null;
	private long[] _mainAssetCategoryIds = null;
	
	public CategorizeMgr(long id, String className)
	{
		this._id = id;
		this._className = className;
	}

	public CategorizeMgr(long groupId, long userId, String articleId, long[] assetCategoryIds, long[] mainAssetCategoryIds)
	{
		this._groupId 				= groupId;
		this._userId 				= GroupMgr.getDefaultUserId();
		this._articleId 			= articleId;
		this._assetCategoryIds 		= assetCategoryIds;
		this._mainAssetCategoryIds 	= mainAssetCategoryIds;
	}
	
	@Override
	public JournalArticle call()
	{
		JournalArticle ja = null;
		try
		{
			if( _className.equalsIgnoreCase(IterKeys.CLASSNAME_VOCABULARY) )
				AssetVocabularyLocalServiceUtil.deleteVocabulary(_id);
			else if( _className.equalsIgnoreCase(IterKeys.CLASSNAME_CATEGORY) )
					AssetCategoryLocalServiceUtil.deleteCategory(_id);
			else if( !_articleId.equals("") )
					ja = CategorizeLocalServiceUtil.updtContentCategories(_groupId, _userId, _articleId, _assetCategoryIds, _mainAssetCategoryIds);
				
		}
		catch (Exception e)
		{
			_e = e;
		}
		
		return ja;
	}

	public static void deleteVocabulary(long vocabularyId) throws PortalException, SystemException
	{
		CategorizeMgr catMgr = new CategorizeMgr(vocabularyId, IterKeys.CLASSNAME_VOCABULARY);
		catMgr.deleteThread();
	}
	
	public static void deleteCategory(long categoryId) throws PortalException, SystemException
	{
		CategorizeMgr catMgr = new CategorizeMgr(categoryId, IterKeys.CLASSNAME_CATEGORY);
		catMgr.deleteThread();
	}
	
	public static void updateWebContentCategories(long groupId, long userId, String articleId, long[] assetCategoryIds, long[] mainAssetCategoryIds) throws PortalException, SystemException
	{
		CategorizeMgr catMgr = new CategorizeMgr(groupId, userId, articleId, assetCategoryIds, mainAssetCategoryIds);
		catMgr.updateWebContentCategoriesThread();
	}
	
	public void deleteThread() throws PortalException, SystemException
	{
		try
		{
			ExecutorService executorService = Executors.newSingleThreadExecutor();
			executorService.submit(this);
			
			executorService.shutdown();
			
			// Se espera a que termine
			while (!executorService.awaitTermination(1000, TimeUnit.MILLISECONDS))
			{
				_log.debug( String.format("Waiting for delete %s", _id) );
			}
			
			if (_e != null)
				throw _e;
				
			// Regeneramos el mapa de la segmentacion de publicidad por metadatos
			MetadataAdvertisementTools.initMetadataAdvertisementNonClustered();
		}
		catch (PortalException pe)
		{
			throw pe;
		}
		catch (SystemException se)
		{
			throw se;
		}
		catch (Throwable th)
		{
			// Las excepciones que NO sean PortalException ni SystemException, o los errores, se lanzan como SystemException
			throw new SystemException(th);
		}
	}
	
	public void updateWebContentCategoriesThread() throws PortalException, SystemException
	{
		try
		{
			ExecutorService executorService = Executors.newSingleThreadExecutor();
			Future<JournalArticle> futureJA = executorService.submit(this);
			
			executorService.shutdown();
			
			// Se espera a que termine
			while (!executorService.awaitTermination(1000, TimeUnit.MILLISECONDS))
			{
				_log.debug( String.format("Waiting for update article %s categories.", _articleId) );
			}
			
			if (_e != null)
				throw _e;
				
			// Reindexamos el articulo
			JournalContentUtil.reindexContent(futureJA.get());
		}
		catch (PortalException pe)
		{
			throw pe;
		}
		catch (SystemException se)
		{
			throw se;
		}
		catch (Throwable th)
		{
			// Las excepciones que NO sean PortalException ni SystemException, o los errores, se lanzan como SystemException
			throw new SystemException(th);
		}
	}
}
