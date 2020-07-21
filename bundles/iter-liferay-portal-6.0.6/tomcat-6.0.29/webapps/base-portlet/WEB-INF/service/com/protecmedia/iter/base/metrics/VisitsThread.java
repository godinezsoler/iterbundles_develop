package com.protecmedia.iter.base.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.JournalIndexerMgr;
import com.liferay.portal.kernel.search.JournalIndexerUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.base.service.BlockerAdBlockLocalServiceUtil;
import com.protecmedia.iter.base.service.VisitsStatisticsLocalServiceUtil;
import com.protecmedia.iter.base.service.util.GroupMgr;

public class VisitsThread extends Thread{
	
	private static Log _log = LogFactoryUtil.getLog(VisitsThread.class);
	
	private List<Map<String, String>> statistics;
	private Map<String, Long> metaStatistics;
	private int opt;
	private String message;
	private String date;

	public static final int OP_MODE_ARTICLE_VISISTS  	= 0;
	public static final int OP_MODE_SECTION_VISISTS  	= 1;
	public static final int OP_MODE_METADATA_VISISTS 	= 2;
	public static final int OP_MODE_ADBLOCKER_STATS  	= 3;
	public static final int OP_MODE_READINGS_STATS   	= 4;
	public static final int OP_MODE_ABTESTING_VISISTS	= 5;
	public static final int OP_MODE_ABTESTING_IMPRESIONS= 6;
		
	private static final String REINDEX_PROPERTY = "iter.statistics.collector.reindex";

	public VisitsThread(List<Map<String, String>> statistics, int opt, String message, String date)
	{
		this.statistics = statistics;
		this.opt = opt;
		this.message = message;
		this.date = date;
	}
	
	public VisitsThread(List<Map<String, String>> statistics, int opt, String message)
	{
		this.statistics = statistics;
		this.opt = opt;
		this.message = message;
	}
	
	public VisitsThread(Map<String, Long> metaStatistics, int opt, String message)
	{
		this.metaStatistics = metaStatistics;
		this.opt = opt;
		this.message = message;
	}
	
	public void run()
	{
		try
		{
			switch(opt)
			{
			case OP_MODE_ARTICLE_VISISTS:
			{
				// Si hay nuevas estadísticas, las actualiza
				if (statistics != null && statistics.size() > 0)
				{
					// Actualiza las visitas de los artículos
					VisitsStatisticsLocalServiceUtil.updateArticle("visits", statistics);
					
					// Reindexación de artículos
					reindexArticlesIfRequired();
				}
				break;
			}

			case OP_MODE_SECTION_VISISTS:
			{
				// Si hay nuevas estadísticas, las actualiza
				if (statistics != null && statistics.size() > 0)
				{
					// Actualiza las visitas de los artículos
					VisitsStatisticsLocalServiceUtil.updateSectionVisits(statistics);
				}
				
				break;
			}

			case OP_MODE_METADATA_VISISTS:
			{
				if (metaStatistics != null && metaStatistics.size() > 0)
				{
					VisitsStatisticsLocalServiceUtil.updateCategoryVisits(metaStatistics);
				}
				break;
			}

			case OP_MODE_ADBLOCKER_STATS:
			{
				if(statistics != null && statistics.size() > 0)
				{
					// Para cada estadística
					for(Map<String,String> stats : statistics)
					{
						try
						{
							long groupId = Long.parseLong((String)stats.get("groupId"));
							// Actualiza las estadísticas
							BlockerAdBlockLocalServiceUtil.updateAdBlockStatistics(groupId, date, Long.parseLong((String)stats.get("mode")), Long.parseLong((String)stats.get("hadAdblock")), Long.parseLong((String)stats.get("hasAdblock")), Long.parseLong((String)stats.get("num")));
						}
						catch (Exception e)
						{
							_log.error(e);
						}
					}
				}
				break;
			}
			
			case OP_MODE_READINGS_STATS:
			{
				if(statistics != null && statistics.size() > 0)
				{
					VisitsStatisticsLocalServiceUtil.updateArticle("readings", statistics);
				}
				break;
			}
			
			case OP_MODE_ABTESTING_VISISTS:
			{
				// Actualiza las visitas de ABTesting
				VisitsStatisticsLocalServiceUtil.updateABtesting("visits", statistics);

				break;
			}

			case OP_MODE_ABTESTING_IMPRESIONS:
			{
				// Actualiza las visitas de ABTesting
				VisitsStatisticsLocalServiceUtil.updateABtesting("impresions", statistics);

				break;
			}
			
			}
		}
		catch(Exception e)
		{
			_log.error(e);
		}
		finally
		{
		    _log.info(message);
		}
	}
	
	/**
	 * <p>Manda reindexar las visitas de los artículos de los grupos que tengan habilitada esta opción.</p>
	 * <p>Para ello, comprueba para cada uno si su grupo tiene habilitada la reindexación de visitas.</p>
	 */
	private void reindexArticlesIfRequired()
	{
		if (opt == OP_MODE_ARTICLE_VISISTS)
		{
			Map<Long, Boolean> reindex = new HashMap<Long, Boolean>();
			long globalGroupId = GroupMgr.getGlobalGroupId();
			
			Map<Long, List<JournalArticle>> articlesToReindex = new HashMap<Long, List<JournalArticle>>();
			
			for(Map<String, String> stats : statistics)
			{
				// Si el grupo permite reindexar, se añade el artículo al listado.
				if (groupReindexEnabled(stats, reindex))
				{
					// Obtiene el articleId y el grupo.
					String articleId = stats.get("articleId");
					long groupId = Long.valueOf(stats.get("groupId"));
					// Si es el primer artículo del grupo, crea el listado.
					if (articlesToReindex.get(groupId) == null)
						articlesToReindex.put(groupId, new ArrayList<JournalArticle>());
					try
					{
						// Añade el artículo al listado.
						articlesToReindex.get(groupId).add(JournalArticleLocalServiceUtil.getArticle(globalGroupId, articleId));
					}
					catch (Throwable th)
					{
						_log.error("Error while retrieving JournalArticle for reindex visits for articleId " + articleId);
						_log.error(th);
					}
				}
			}
			// Reindexa los artículos.
			reindexArticles(articlesToReindex);
		}
	}
	
	/**
	 * <p>Comprueba en el mapa {@code reindex} si el grupo tiene habilitada la reindexación de artículos.</p>
	 * <p>Si aún no se ha cargado el grupo en el mapa, busca en el {@code portal-ext.properties} el valor del
	 * parámetro {@code iter.statistics.collector.reindex.<group-friendly-url>}.</p>
	 * <p>Si no existe la propiedad, comprueba el valor de la propiedad genérica {@code iter.statistics.collector.reindex}.</p>
	 * <p>Si tampoco existe, retorna {@code false}.</p>
	 * @param stats		Map<String, String> con los datos de la estadísticas del artículo.
	 * @param reindex	Map<Long, Boolean> con la información de si los grupos tienen o no habilitada la reindexación.
	 * @return			{@code true} si hay que reindexar el artículo. {@code false} en caso contrario.
	 */
	private boolean groupReindexEnabled(Map<String, String> stats, Map<Long, Boolean> reindex)
	{
		long groupId = Long.valueOf(stats.get("groupId"));
		Boolean reindexArticles;
		
		try
		{
			// Comprueba si el grupo tiene activada la opción de reindexación de artículos.
			reindexArticles = reindex.get(groupId);
			// Si ese grupo aún no se ha comprobado, busca la propiedad y la añade al mapa.
			if (reindexArticles == null)
			{
				// Calcula el nombre de la propiedad.
				String groupReindexProperty = new StringBuilder(REINDEX_PROPERTY)
				.append(GroupLocalServiceUtil.getGroup(groupId).getFriendlyURL().replaceAll(StringPool.FORWARD_SLASH, StringPool.PERIOD))
				.toString();
				// Mira la propiedad para el grupo en concreto.
				String propertyValue = PortalUtil.getPortalProperties().getProperty(groupReindexProperty);
				// Si no hay propiedad para el grupo, busca la genérica.
				if (propertyValue == null)
				{
					propertyValue = PortalUtil.getPortalProperties().getProperty(REINDEX_PROPERTY);
				}
				// Establece el valor, por defecto a false.
				reindexArticles = Boolean.valueOf(GetterUtil.getBoolean(propertyValue, false));
				// Añade al mapa el valor de la propiedad.
				reindex.put(groupId, reindexArticles);
			}
		}
		catch (Throwable th)
		{
			reindexArticles = Boolean.valueOf(false);
			_log.error("Error checking group reindex article visits property");
			_log.error(th);
		}
		
		return reindexArticles;
	}
	
	/**
	 * <p>Reindexa los artículos pasados por parámetro.</p>
	 * @param jaList {@code List<JournalArticle>} con los artículos a indexar.
	 */
	private void reindexArticles(Map<Long, List<JournalArticle>> articles)
	{
		for (Long groupId : articles.keySet())
		{
			List<JournalArticle> jaList = articles.get(groupId);
			int numArticles = jaList.size();
			if (numArticles > 0)
			{
				try
				{
					JournalIndexerUtil jiu = new JournalIndexerUtil();
					JournalArticle ja = jaList.get(0);
					
					JournalIndexerMgr journalIdxMgr = new JournalIndexerMgr();
					journalIdxMgr.setDelegationId(ja.getDelegationId());
					
					for (int i = 0; i < jaList.size(); i++)
					{
						JournalArticle article = jaList.get(i);
						boolean doCommit = (i == jaList.size()-1);
						
						try
						{
							journalIdxMgr.domsToIndex(jiu.createDom(article), doCommit);
						}
						catch (Exception e)
						{
							journalIdxMgr.domToIndex(null, doCommit);
							_log.error("Article Visits: Can not index article: " + article.getArticleId() + ". " + e);
						}
					}
				}
				catch (Exception e)
				{
					_log.error("Article Visits: Can not index articles " + e);
				}
			}
		}
	}
}
