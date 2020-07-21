package com.protecmedia.iter.news.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.liferay.portal.kernel.dao.orm.IterRS;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.JournalIndexerMgr;
import com.liferay.portal.kernel.search.JournalIndexerUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;


public class JournalArticleIndexer implements Runnable
{
	private static final Log _log 					= LogFactoryUtil.getLog("JournalArticleIndexer");
	private static final Log _logMethodGetArticle 	= LogFactoryUtil.getLog("JournalArticleIndexer_getarticle");
	
	
	private long _scopeGroupId = 0;
	
	private static AtomicInteger _totalArticles= new AtomicInteger();
	private static AtomicInteger _currentIndexingArticle = new AtomicInteger();
	private static JournalIndexerMgr journalIdxMgr = null;
	private static JournalIndexerUtil journalIdxUtil = null;
	
	/**
	 * Se contabilizan todos los artículos porque TODOS se indexrán, lo que algunos se marcarán como NO buscables
	 * @see http://jira.protecmedia.com:8080/browse/ITER-1305?focusedCommentId=56794&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-56794
	 */
	private String COUNT_INDEXABLE_ARTICLES = new StringBuilder(
		"SELECT COUNT(*) 																								\n").append(
		"FROM JournalArticle 																							\n").append(
		"INNER JOIN ExpandoValue  ON (ExpandoValue.classPK=JournalArticle.id_) 											\n").append(
		"INNER JOIN ExpandoColumn ON (ExpandoColumn.columnId=ExpandoValue.columnId AND ExpandoColumn.name='%s')			\n").append(
		"	WHERE ExpandoValue.data_ %s																					\n").toString();
	
	/**
	 * Se obtienen todos los artículos porque TODOS se indexrán, lo que algunos se marcarán como NO buscables
	 * @see http://jira.protecmedia.com:8080/browse/ITER-1305?focusedCommentId=56794&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-56794
	 */
	private String GET_INDEXABLE_ARTICLES = new StringBuilder(
		"SELECT JournalArticle.articleId, ExpandoValue.data_ scopeGroupId, JournalArticle.resourcePrimKey,				\n").append(
		"		JournalArticle.modifiedDate, JournalArticle.content, JournalArticle.structureId, JournalArticle.uuid_, 	\n").append(
		"		JournalArticle.indexable																				\n").append(
		"FROM 	JournalArticle 																							\n").append(
		"INNER JOIN ExpandoValue  ON (ExpandoValue.classPK = JournalArticle.id_)										\n").append(
		"INNER JOIN ExpandoColumn ON (ExpandoColumn.columnId = ExpandoValue.columnId AND ExpandoColumn.name='%s')		\n").append(
		"	WHERE ExpandoValue.data_ %s 																				\n").append(
		"	ORDER BY JournalArticle.modifiedDate DESC																	\n").toString();
	
	private String GET_INDEXABLE_ARTICLES_ID = new StringBuilder(
		"SELECT JournalArticle.id_																						\n").append(
		"FROM 	JournalArticle 																							\n").append(
		"INNER JOIN ExpandoValue  ON (ExpandoValue.classPK = JournalArticle.id_)										\n").append(
		"INNER JOIN ExpandoColumn ON (ExpandoColumn.columnId = ExpandoValue.columnId AND ExpandoColumn.name='%s')		\n").append(
		"	WHERE ExpandoValue.data_ %s 																				\n").append(
		"	ORDER BY JournalArticle.modifiedDate DESC																	\n").toString();

	
	private String REG_EXPR = "REGEXP '[[:<:]]%s[[:>:]]'";
	private String EQUALS = "= %s";
	
	private static long indexArticles_time = 0L;
	
	public JournalArticleIndexer( long scopeGroupId, int packageSize, int commitWithInMs ) throws PortalException, SystemException
	{
		this._scopeGroupId = scopeGroupId;
		long delegationId = GroupLocalServiceUtil.getGroup(scopeGroupId).getDelegationId();
		
		journalIdxMgr = new JournalIndexerMgr();
		journalIdxMgr.setDelegationId(delegationId);
		journalIdxMgr.setArticlesByPackage( packageSize );
		journalIdxMgr.setCommitWithInMs( commitWithInMs );
		
		journalIdxUtil = new JournalIndexerUtil();
	}
	
	public void reindexArticles() throws SecurityException, ServiceError, NoSuchMethodException
	{
		long ini = Calendar.getInstance().getTimeInMillis();
		
		String groupWhereClause = EQUALS;
		if(PropsValues.ITER_SHARED_ARTICLES_ENABLED)
			groupWhereClause = REG_EXPR;
		
		groupWhereClause = String.format(groupWhereClause, _scopeGroupId);
		
		String sql = String.format(COUNT_INDEXABLE_ARTICLES, IterKeys.EXPANDO_COLUMN_NAME_SCOPEGROUPID, groupWhereClause);
		
		long ini2 = System.currentTimeMillis();
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList(sql);
		if(_log.isTraceEnabled())
			_log.trace("Count articles time: " + (System.currentTimeMillis()-ini2));
		
		if(result!=null && result.size()==1)
			_totalArticles.set( GetterUtil.getInteger(String.valueOf(result.get(0))) );
		
		_currentIndexingArticle = new AtomicInteger();
		
		if(_log.isInfoEnabled())
			_log.info("Total articles to index: " + _totalArticles.get());
		
		// Como medida de seguridad, mediante este FLAG se puede volver al proceso de indexación anterior
		sql = String.format(
				PropsValues.ITER_INDEXATION_ALLOW_EDITIONS && _logMethodGetArticle.isFatalEnabled() ? GET_INDEXABLE_ARTICLES_ID : GET_INDEXABLE_ARTICLES,
				IterKeys.EXPANDO_COLUMN_NAME_SCOPEGROUPID, groupWhereClause);
		
		if(_log.isTraceEnabled())
			_log.trace("GET_INDEXABLE_ARTICLES: " + sql);
		
		String method = PropsValues.ITER_INDEXATION_ALLOW_EDITIONS 	?
						(_logMethodGetArticle.isFatalEnabled() 		? 
							"indexArticleWithEditions_GetArticle" 	: 
							"indexArticleWithEditions")				:
							"indexArticle";
						
		PortalLocalServiceUtil.executeQueryAsResultSet(sql, JournalArticleIndexer.class, method);
		
		if(_log.isInfoEnabled())
		{
			long fin = Calendar.getInstance().getTimeInMillis();
			_log.info("Reindex total time: " + (fin-ini) + "ms.");
		}
	}
	
	public static void indexArticleWithEditions_GetArticle(IterRS resultset)
	{
		if( !Thread.currentThread().isInterrupted() && Thread.currentThread().isAlive())
		{
			long ini 		= Calendar.getInstance().getTimeInMillis();
			long ini2 		= System.currentTimeMillis();
			long timeBean 	= 0;
			
			String articleId = "0";
			
			_currentIndexingArticle.getAndIncrement();
			boolean doCommit = (_totalArticles.get() == _currentIndexingArticle.get());
			
			try
			{
				long id = ((Long)resultset.getObject(1)).longValue();
				JournalArticle article = JournalArticleLocalServiceUtil.getArticle(id); 
				articleId = article.getArticleId();
				
				if (_log.isTraceEnabled())
					timeBean = Calendar.getInstance().getTimeInMillis();
				
				_log.trace("Time to get article bean " + articleId + " : " + (Calendar.getInstance().getTimeInMillis()-ini) + "ms.");
	
				journalIdxMgr.domsToIndex(journalIdxUtil.createDom(article), doCommit);
			}
			catch (Exception e)
			{
				journalIdxMgr.domToIndex(null, doCommit);
					
				_log.error("Can not index article: " + articleId + ". " + e);
				_log.debug(e);
			}
			
			if (_log.isTraceEnabled())
			{
				long fin = Calendar.getInstance().getTimeInMillis();
				indexArticles_time += System.currentTimeMillis()-ini2;
				
				_log.trace( String.format("Times for article %s:\n\t - Get bean=%d\n\t - Process article=%d\n\t - Total for this article=%d\n\t - Total for indexation process=%d", 
							articleId, timeBean-ini, fin-timeBean, fin-ini, indexArticles_time) );
			}
		}
		else
			_log.debug("Indexing interrupted while articles are being processing.");
	}
	
	public static void indexArticleWithEditions(IterRS resultset)
	{
		if( !Thread.currentThread().isInterrupted() && Thread.currentThread().isAlive())
		{
			long ini = Calendar.getInstance().getTimeInMillis();
			long ini2 = System.currentTimeMillis();
			
			String articleId = "0";
			Document d = null;
			
			_currentIndexingArticle.getAndIncrement();
			boolean doCommit = (_totalArticles.get() == _currentIndexingArticle.get());
			
			try
			{
				Object columVal = null;
				
				columVal = resultset.getObject(1);
				articleId = (columVal == null) ? "" : columVal.toString();
				
				columVal = resultset.getObject(2);
				String scopeGroupIdsColumn = (columVal == null) ? "" : columVal.toString();
				List<Long> scopeGroupIds = new ArrayList<Long>();
				String[] scopegroups = scopeGroupIdsColumn.split(",");
				// ITER-1156 No aparecen noticias en páginas del sistema (metadatos, búsqueda, sección)
				// Se descartan las asignaciones vacías
				for(String scopeGroupId : scopegroups)
					if(Validator.isNotNull(scopeGroupId))
						scopeGroupIds.add( Long.valueOf(scopeGroupId) );
				
				columVal = resultset.getObject(3);
				long resourcePrimKey = (columVal == null) ? 0L : ((Long)columVal).longValue();
				
				columVal = resultset.getObject(4);
				Date modifiedDate = (Date)columVal;
				
				columVal = resultset.getObject(5);
				String content = (columVal == null) ? "" : columVal.toString();
				
				columVal = resultset.getObject(6);
				String structureId = (columVal == null) ? "" : columVal.toString();
				
				columVal = resultset.getObject(7);
				String uuid_ = (columVal == null) ? "" : columVal.toString();
				
				boolean isIndexable = GetterUtil.getBoolean(resultset.getObject(8), false);
	
				journalIdxMgr.domsToIndex(journalIdxUtil.createDoms(	uuid_, articleId, scopeGroupIds, resourcePrimKey, 
													structureId, content, modifiedDate, isIndexable ), doCommit
										 );
			}
			catch (Exception e)
			{
				journalIdxMgr.domToIndex(null, doCommit);
					
				_log.error("Can not index article: " + articleId + ". " + e);
				_log.debug(e);
			}
			
			if (_log.isTraceEnabled())
			{
				long fin = Calendar.getInstance().getTimeInMillis();
				indexArticles_time += System.currentTimeMillis()-ini2;
				
				_log.trace( String.format("Times for article %s:\n\t - Total for this article=%d\n\t - Total for indexation process=%d", 
							articleId, fin-ini, indexArticles_time) );
			}
		}
		else
			_log.debug("Indexing interrupted while articles are being processing.");
	}

	
	public static void indexArticle(IterRS resultset)
	{
		if( !Thread.currentThread().isInterrupted() && Thread.currentThread().isAlive())
		{
			long ini = Calendar.getInstance().getTimeInMillis();
			long ini2 = System.currentTimeMillis();
			
			String articleId = "0";
			Document d = null;
			
			_currentIndexingArticle.getAndIncrement();
			boolean doCommit = (_totalArticles.get() == _currentIndexingArticle.get());
			
			try
			{
				Object columVal = null;
				
				columVal = resultset.getObject(1);
				articleId = (columVal == null) ? "" : columVal.toString();
				
				columVal = resultset.getObject(2);
				String scopeGroupIdsColumn = (columVal == null) ? "" : columVal.toString();
				List<Long> scopeGroupIds = new ArrayList<Long>();
				String[] scopegroups = scopeGroupIdsColumn.split(",");
				
				// ITER-1156 No aparecen noticias en páginas del sistema (metadatos, búsqueda, sección)
				// Se descartan las asignaciones vacías
				for(String scopeGroupId : scopegroups)
					if(Validator.isNotNull(scopeGroupId))
						scopeGroupIds.add( Long.valueOf(scopeGroupId) );
				
				columVal = resultset.getObject(3);
				long resourcePrimKey = (columVal == null) ? 0L : ((Long)columVal).longValue();
				
				columVal = resultset.getObject(4);
				Date modifiedDate = (Date)columVal;
				
				columVal = resultset.getObject(5);
				String content = (columVal == null) ? "" : columVal.toString();
				
				columVal = resultset.getObject(6);
				String structureId = (columVal == null) ? "" : columVal.toString();
				
				columVal = resultset.getObject(7);
				String uuid_ = (columVal == null) ? "" : columVal.toString();
				
				boolean isIndexable = GetterUtil.getBoolean(resultset.getObject(8), false);
	
				d = journalIdxUtil.createDom(	uuid_, articleId, scopeGroupIds, resourcePrimKey, 
						structureId, content, modifiedDate, isIndexable);
				
				journalIdxMgr.domToIndex(d, doCommit);
			}
			catch (Exception e)
			{
				journalIdxMgr.domToIndex(d, doCommit);
					
				_log.error("Can not index article: " + articleId + ". " + e);
				_log.debug(e);
			}
			
			if (_log.isTraceEnabled())
			{
				long fin = Calendar.getInstance().getTimeInMillis();
				indexArticles_time += System.currentTimeMillis()-ini2;
				
				_log.trace( String.format("Times for article %s:\n\t - Total for this article=%d\n\t - Total for indexation process=%d", 
							articleId, fin-ini, indexArticles_time) );
			}
		}
		else
			_log.debug("Indexing interrupted while articles are being processing.");
	}

	
	public com.liferay.portal.kernel.xml.Document getIndexingStatus()
	{
		Element rootElement = SAXReaderUtil.createElement("rs");
		
		Element row = rootElement.addElement("row");
		
		row.addAttribute( "total", 			String.valueOf(_totalArticles) );
		row.addAttribute( "process", 		String.valueOf(_currentIndexingArticle.get()) );
		row.addAttribute( "indexed", 		String.valueOf(journalIdxMgr.getTotalIndexed()) );
		row.addAttribute( "packagesize", 	String.valueOf(journalIdxMgr.getArticlesByPackage()) );
		row.addAttribute( "readytoindex", 	String.valueOf(journalIdxMgr.getReady2Index()) );
		
		com.liferay.portal.kernel.xml.Document d = SAXReaderUtil.createDocument( rootElement );
		
		return d;
	}
	
	@Override
	public void run()
	{
		try
		{
			reindexArticles();
		}
		catch (Exception e)
		{
			_log.error(e);
		}
	}

}
