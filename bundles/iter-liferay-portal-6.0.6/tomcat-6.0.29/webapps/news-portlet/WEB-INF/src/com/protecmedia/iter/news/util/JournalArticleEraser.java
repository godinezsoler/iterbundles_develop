package com.protecmedia.iter.news.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.news.service.JournalArticleLocalServiceUtil;

public class JournalArticleEraser implements Callable<Boolean>
{
	private static Log _log = LogFactoryUtil.getLog(JournalArticleEraser.class);
	
	String 	_articleId 		= "";
	boolean _deleteFileEntry= false;
	
	public JournalArticleEraser(String articleId, boolean deleteFileEntry)
	{
		_articleId 		 = articleId;
		_deleteFileEntry = deleteFileEntry;
	}
	
	@Override
	public Boolean call() throws Exception
	{
		return JournalArticleLocalServiceUtil.deleteJournalArticle(_articleId, _deleteFileEntry);
	}
	
	/**
	 * Se ejecuta el borrado de un artículo y sus dependencias en un hilo aparte, se garantiza así tras esta operación 
	 * el commit del borrado
	 * 
	 * @param articleId
	 * @param deleteFileEntry
	 * @return
	 * @throws Exception
	 */
	static public boolean erase(String articleId, boolean deleteFileEntry) throws Exception
	{
		// Se lanza la tarea
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		Future<Boolean> future  = executorService.submit( new JournalArticleEraser(articleId, deleteFileEntry) );
		
		executorService.shutdown();
		
		// Se espera a que termine
		while (!executorService.awaitTermination(1000, TimeUnit.MILLISECONDS))
		{
			_log.warn( String.format("Waiting for JournalArticleLocalServiceUtil.deleteJournalArticle('%s', %s)", articleId, Boolean.toString(deleteFileEntry)) );
		}
		
		boolean found = future.get();
		if (found)
			_log.info( String.format("The article %s has been deleted", articleId) );
		else
			_log.warn( String.format("The article %s hasn't been found", articleId) );
			
		return found;
	}
}
