package com.protecmedia.iter.base.metrics;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.liferay.portal.apache.ApacheUtil;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ABTestingMgr;
import com.liferay.portal.kernel.util.CategoriesUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.SectionUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.base.cluster.Heartbeat;

public enum VisitsStatisticMgr implements Runnable 
{
	INSTANCE;
	
	private static 			Log 				_log 					= LogFactoryUtil.getLog(VisitsStatisticMgr.class);
	private static 			ExecutorService 	_executorService 		= null;
	private static 			FutureTask<Void>	_future					= null;
	private static final 	String 				VISITS_SERVICE 			= "/get-visits?delete";

	// Patrones para identificar las líneas
	private static final 	Pattern 			adBlockPattern 			= Pattern.compile("groupId=(\\d+)&hadadblock=([0,1])&hasadblock=([0,1])&mode=([0,1])");
	private static final 	Pattern 			generalPattern 			= Pattern.compile("(?:(\\w+)=([^&]+?)(?=&|$))");

	// Constantes para nombres de campos
	private static final 	String 				PLID					= "plid";
	private static final 	String 				CATEGORIES_IDS 			= "categoriesIds";
	private static final 	String 				URL_TYPE       			= "urlType";
	public  static final 	String 				GROUP_ID       			= "groupId";
	public  static final 	String 				ARTICLE_ID     			= "articleId";
	public  static final 	String 				VISITS         			= "visits";
	public  static final 	String 				VARIANT					= ABTestingMgr.VARIANT;
	public  static final 	String 				VARIANTID				= ABTestingMgr.VARIANTID;
	public  static final 	String 				EXTERNAL				= "ext";

	// Constantes de tipos de páginas
	private static final 	String 				TYPE_ARTICLE      		= "detail";
	private static final 	String 				TYPE_SECTION      		= "mainSection";
	private static final 	String 				TYPE_METADATA     		= "meta";
	private static final 	String 				TYPE_READ_ARTICLE 		= "readArticle";
	private static final 	String 				TYPE_IMPRESION 			= "impresion";

	private static final 	String 				articleMessageRequest 	= "Updating article visits (Scheduler task)";
	private static final 	String 				sectionMessageRequest 	= "Updating section visits (Scheduler task)";
	private static final 	String 				metaMessageRequest 		= "Updating metadata visits (Scheduler task)";
	private static final 	String 				adBlockMessageRequest 	= "Updating adBlocker statistics (Scheduler task)";
	private static final 	String 				readingsMessageRequest 	= "Updating article readings statistics (Scheduler task)";
	private static final 	String 				abVisitsMessageRequest	= "Updating abtesting visits (Scheduler task)";
	private static final 	String 				abImpMessageRequest		= "Updating abtesting impresions (Scheduler task)";


	public void run() 
	{
		_log.info("Starting the VisitsStatisticMgr process");
		
		try
		{
			// Cuando está el modo antiguo (ni el Watchdog y ni Heartbeat) se espera un tiempo inicial
			if (_isEnable() && (!PropsValues.CLUSTER_HEARTBEAT_ENABLED && PropsValues.SCHEDULER_ENABLED))
				Thread.sleep( PropsValues.STATISTICS_COLLECTOR_WAIT );
			
			while (_isEnable())
			{
				manageVisits();
				
				if (_log.isTraceEnabled())
					_log.trace( String.format("The VisitsStatisticMgr will sleep %d ms", PropsValues.STATISTICS_COLLECTOR_FREQUENCY) );
				Thread.sleep( PropsValues.STATISTICS_COLLECTOR_FREQUENCY );
			}
		}
		catch (InterruptedException ie)
		{
			Thread.currentThread().interrupt();
			_log.trace(ie.toString());
		}

		_log.info("The VisitsStatisticMgr process has been finished");
	}
	
	synchronized static public void start() throws ServiceError
	{
		_log.trace("start: Begin");
		
		ErrorRaiser.throwIfFalse( !isInProcess(), IterErrorKeys.XYZ_E_VISITSSTATISTICMGR_IS_ALREADY_RUNNING_ZYX ); 

		if (canLaunchProcess())
		{
			_future 		= new FutureTask<Void>(INSTANCE, null);
			_executorService= Executors.newSingleThreadExecutor();
			
			_executorService.execute( _future );
		
			_log.info("The VisitsStatisticMgr mechanism has started");
		}
		else
		{
			_log.warn("The VisitsStatisticMgr is disable in this server (scheduler.enabled/cluster.heartbeat and iter.statistics.collector.enabled)");
		}

		_log.trace("start: End");
	}
	
	synchronized static public void stop() throws ServiceError, InterruptedException
	{
		_log.trace("stop: Begin");
		boolean stopOK = false;

		ErrorRaiser.throwIfFalse( isInProcess(), IterErrorKeys.XYZ_E_VISITSSTATISTICMGR_IS_NOT_RUNNING_ZYX ); 
		
		try
		{
			_executorService.shutdownNow();
			stopOK = _executorService.awaitTermination(PropsValues.STATISTICS_COLLECTOR_TIMEOUT, TimeUnit.MILLISECONDS);
		}
		finally
		{
			if (!stopOK)
				_log.error("The VisitsStatisticMgr mechanism is still in process");
			else
			{
				_log.info("The VisitsStatisticMgr mechanism has finished");

				_future 			= null;
				_executorService 	= null;
			}
		}

		_log.trace("stop: End");
	}

	static private boolean canLaunchProcess()
	{
		return PropsValues.STATISTICS_COLLECTOR_ENABLED && Heartbeat.canLaunchProcesses();
	}
	
	static private boolean isInProcess()
	{
		boolean inProcess = _future != null 				&& _executorService != null 		&& 
							!_executorService.isShutdown() 	&& !_executorService.isTerminated()	&&
							!_future.isCancelled()			&& !_future.isDone();

		return inProcess;
	}


	private boolean _isEnable()
	{
		return 	Thread.currentThread().isAlive() && !Thread.currentThread().isInterrupted() &&
				Heartbeat.canLaunchProcesses();
	}
	
	private void manageVisits()
	{
		String adBlockerProcessDate = new SimpleDateFormat("yyyyMMdd").format(new Date());

		String errorApacheURL = "";
		
		try
	    {
				int apacheConnTimeout = ApacheUtil.getApacheConnTimeout();
				int apacheReadTimeout = ApacheUtil.getApacheReadTimeout();

				List<Map<String, String>> articleVisits  		= new ArrayList<Map<String, String>>();
				List<Map<String, String>> abtestingVisits		= new ArrayList<Map<String, String>>();
				List<Map<String, String>> abtestingImpresions	= new ArrayList<Map<String, String>>();
				List<Map<String, String>> sectionVisits  		= new ArrayList<Map<String, String>>();
				Map<String, Long> metadataVisits         		= new HashMap<String, Long>();
				List<Map<String, String>> adBlockStats   		= new ArrayList<Map<String,String>>();
				List<Map<String, String>> readigsStats   		= new ArrayList<Map<String,String>>();
				
				String apacheURLs[] = ApacheUtil.getAllURLs();
				
				// Se recogen las visitas almacenadas en cada Apache
				for (int i = 0; i < apacheURLs.length && Heartbeat.canLaunchProcesses(); i++)
				{
					 String currentApacheURL = apacheURLs[i];
					 
					HttpURLConnection httpConnection = null;
					
					try
					{
						errorApacheURL 	= currentApacheURL + VISITS_SERVICE;
						URL url 		= new URL(currentApacheURL + VISITS_SERVICE);
				        URLConnection connection = url.openConnection();
				        
				        httpConnection = (HttpURLConnection)connection;
				        httpConnection.setConnectTimeout(apacheConnTimeout*1000);
				        httpConnection.setReadTimeout(apacheReadTimeout*1000);
				        connection.setRequestProperty ("User-Agent", WebKeys.USER_AGENT_ITERWEBCMS);
				        httpConnection.connect();
				        
			        	// ITER-720 Reutilización de las conexiones HTTP contra los servidores que soportan el header "Connection: keep-alive"	
			        	// Hay que consumir los errores para poder reutilizar las conexiones
			        	HttpUtil.throwIfConnectionFailed( httpConnection, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX );
				        
				        if (httpConnection.getResponseCode() == 200)
				        {
				        	// Carga las categorías que permiten registrar visitas
				        	List<String> categoriesTrakingVisits = CategoriesUtil.getCategoriesTrackingVisits();
				        	
				        	// Proces las líneas
					        BufferedReader in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
					        String currentLine;
							
					        while ((currentLine = in.readLine()) != null) 
					        {
					        	// Separa el contador de los datos
					        	String[] stat = currentLine.split(",");
					        	String numVisits = stat[1];
					        	currentLine = stat[0];
					        	
					        	// Estadísticas de adBlock
					        	Matcher matcher = adBlockPattern.matcher(currentLine);
					        	if (matcher.find())
					        	{
					        		extractAdBlockStats(matcher, numVisits, adBlockStats);
					        	}
					        	// Estadísticas de visitas
					        	else
					        	{
					        		Map<String, String> statsData = extractStatsData(currentLine, numVisits);
					        		String urlType = statsData.get(URL_TYPE);
						        	
					        		List<String> categoriesToRegister = new ArrayList<String>();
					        		if (TYPE_ARTICLE.equals(urlType) && checkArticle(statsData))
					        		{
					        			articleVisits.add(statsData);
					        			
					        			// Secciones en las que está asignado el artículo
					        			List<Long> plids = SectionUtil.getArticleSectionsPlid(Long.parseLong(statsData.get(GROUP_ID)), statsData.get(ARTICLE_ID));
					        			for (Long plid : plids)
					        			{
					        				Map<String, String> sectionArticleVisits = new HashMap<String, String>();
					        				sectionArticleVisits.put(GROUP_ID, statsData.get(GROUP_ID));
					        				sectionArticleVisits.put("plid", String.valueOf(plid));
					        				sectionArticleVisits.put("visits", "0");
					        				sectionArticleVisits.put("articleVisits", statsData.get(VISITS));
					        				sectionVisits.add(sectionArticleVisits);
					        			}
					        			
					        			// Visitas a los metadatos del artículo
					        			categoriesToRegister = CategoriesUtil.getCategoriesTrackingVisits(GroupMgr.getGlobalGroupId(), statsData.get(ARTICLE_ID));
					        			for (String categoryId : categoriesToRegister)
					        			{
					        				addMetadataStatistics(categoryId, statsData, metadataVisits);
					        			}
					        			
					        			// Se comprueba si dicho artículo tiene ABTesting. No se comprueba si el ABTesting existe o ha terminado, ya que podria 
					        			// variar justo antes del INSERT
					        			if (statsData.containsKey(VARIANT))
					        				abtestingVisits.add(statsData);
					        			
					        		}
					        		else if (TYPE_SECTION.equals(urlType) && checkSection(statsData))
					        		{
					        			sectionVisits.add(statsData);
					        		}
					        		else if (TYPE_METADATA.equals(urlType) && checkMetadata(statsData, categoriesTrakingVisits, categoriesToRegister))
					        		{
					        			for (String categoryId : categoriesToRegister)
					        			{
					        				addMetadataStatistics(categoryId, statsData, metadataVisits);
					        			}
					        		}
					        		else if (TYPE_READ_ARTICLE.equals(urlType) && checkArticle(statsData))
					        		{
					        			readigsStats.add(statsData);
					        		}
					        		else if (TYPE_IMPRESION.equals(urlType) && checkArticle(statsData))
					        		{
					        			abtestingImpresions.add(statsData);
					        		}
					        	}
					        }
		        			in.close();
				        }
					}
					catch (Throwable th)
					{
						_log.debug(th);
						_log.error("Unable to get visit from Apache: " + currentApacheURL);
					}
					finally
					{
						if (httpConnection != null)
							httpConnection.disconnect();
					}
				}
				
				// Lanza los hilos para actualizar las visitas y las estadísticas de visitas
				List<VisitsThread> visitsThreads = new ArrayList<VisitsThread>();
				
				if(articleVisits.size() > 0)
				{
					VisitsThread thread = new VisitsThread(articleVisits, VisitsThread.OP_MODE_ARTICLE_VISISTS, articleMessageRequest);
					thread.start();
					visitsThreads.add(thread);
				}
				
				if(sectionVisits.size() > 0)
				{
					VisitsThread thread = new VisitsThread(sectionVisits, VisitsThread.OP_MODE_SECTION_VISISTS, sectionMessageRequest);
					thread.start();
					visitsThreads.add(thread);
				}
				
				if(metadataVisits.size() > 0)
				{
					VisitsThread thread = new VisitsThread(metadataVisits, VisitsThread.OP_MODE_METADATA_VISISTS, metaMessageRequest);
					thread.start();
					visitsThreads.add(thread);
				}
				
				if(adBlockStats.size() > 0)
				{
					VisitsThread thread = new VisitsThread(adBlockStats, VisitsThread.OP_MODE_ADBLOCKER_STATS, adBlockMessageRequest, adBlockerProcessDate);
					thread.start();
					visitsThreads.add(thread);
				}
				
				if(readigsStats.size() > 0)
				{
					VisitsThread thread = new VisitsThread(readigsStats, VisitsThread.OP_MODE_READINGS_STATS, readingsMessageRequest);
					thread.start();
					visitsThreads.add(thread);
				}
				
				if (abtestingVisits.size() > 0)
				{
					VisitsThread thread = new VisitsThread(abtestingVisits, VisitsThread.OP_MODE_ABTESTING_VISISTS, abVisitsMessageRequest);
					thread.start();
					visitsThreads.add(thread);
				}
				
				if (abtestingImpresions.size() > 0)
				{
					VisitsThread thread = new VisitsThread(abtestingImpresions, VisitsThread.OP_MODE_ABTESTING_IMPRESIONS, abImpMessageRequest);
					thread.start();
					visitsThreads.add(thread);
				}
				
				// Espera a que terminen todas las inserciones
				for (VisitsThread thread : visitsThreads)
					thread.join();
				
				// Una vez terminadas las actualizaciones de las estadícticas se analiza si debería terminar algún ABTesting
				ABTestingMgr.finishABTestings();
	    }
		catch(Exception e)
		{
	    	_log.error("Unable to get visits (Scheduler task)");
	    	_log.error("Current URL to get visits: " + errorApacheURL);
	    	_log.error(e);
	    }
	}
	
	private void extractAdBlockStats(Matcher matcher, String counter, List<Map<String, String>> adBlockStats)
	{
		Map<String, String> adBlockStat = new HashMap<String, String>();
		// Coge el grupo
		String scopeGroupId = matcher.group(1);
    	// Carga el modo de operación del bloqueador de adBlockers
    	String blockAdBlockMode = GroupConfigTools.getGroupConfigXMLField(Long.valueOf(scopeGroupId), "blockeradblock", "@mode");
    	int mode = "active".equals(blockAdBlockMode) ? 1 : "passive".equals(blockAdBlockMode) ? 0 : -1;
    	if (mode >= 0)
    	{
	    	// Extrae las estadísticas
    		adBlockStat.put(GROUP_ID, scopeGroupId);
    		adBlockStat.put("hadAdblock", matcher.group(2));
    		adBlockStat.put("hasAdblock", matcher.group(3));
    		adBlockStat.put("mode", String.valueOf(mode));
    		adBlockStat.put("num", counter);
    		// Añade las estadísticas
    		adBlockStats.add(adBlockStat);
    	}
	}

	private Map<String, String> extractStatsData(String currentLine, String numVisits)
	{
		Map<String, String> statsData = new HashMap<String, String>();
		
		Matcher matcher = generalPattern.matcher(currentLine);
    	while (matcher.find())
    	{
    		if (matcher.groupCount() == 2)
    		{
    			statsData.put(matcher.group(1), matcher.group(2));
    		}
    	}
    	
    	if (statsData.size() > 0)
    		statsData.put("visits", numVisits);
		
		return statsData;
	}

	/**
	 * Valida que el grupo y el artículo existan.
	 * 
	 * Comprueba el vocabulario de cada categoría y registra visitas de aquellas cuyo vocabulario
	 * esté configurado para registrar visitas siempre o si es principal del artículo.
	 */
	private boolean checkArticle(Map<String, String> stats)
	{
		boolean exist = false;
		
		try
		{
			String articleId = stats.get(ARTICLE_ID);
			long groupId = Long.parseLong(stats.get(GROUP_ID));
			
			JournalArticle journalArticle = JournalArticleLocalServiceUtil.getArticle(GroupMgr.getGlobalGroupId(), articleId);
			Group scopeGroupId = GroupLocalServiceUtil.getGroup(groupId);
		
			if (journalArticle != null && scopeGroupId != null)
			{
				exist = true;
			}
			else
			{
				_log.error("No article found for groupId: " + groupId + " and articleId: " + articleId);
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
		}
		
		return exist;
	}

	private void addMetadataStatistics(String categoryId, Map<String, String> statsData, Map<String, Long> metadataVisits)
	{
		String key = statsData.get(GROUP_ID) + StringPool.COMMA_AND_SPACE + categoryId;
		long value = GetterUtil.getLong(metadataVisits.get(key), 0);
		value = value > 0 ? value + Long.valueOf(statsData.get(VISITS)) : Long.valueOf(statsData.get(VISITS));
		metadataVisits.put(key, value);
	}
	
	private boolean checkSection(Map<String, String> stats)
	{
		boolean exist = false;
		
		try
		{
			long plid 			= Long.parseLong(stats.get(PLID));
			long groupId 		= Long.parseLong(stats.get(GROUP_ID));
			
			// Podría lanzar un NoSuchLayoutException
			long scopeGroupId 	= LayoutLocalServiceUtil.getLayout(plid).getGroupId();
			
			// Podría lanzar NoSuchGroupException
			GroupLocalServiceUtil.getGroup(groupId);
			exist = (scopeGroupId == groupId);
			if (!exist)
			{
				_log.error( String.format("The section %d has different section: %d %d", plid, scopeGroupId, groupId) );
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
		}
		
		return exist;
	}
	
	/**
	 * Comprueba el vocabulario de cada categoría y registra visitas de aquellas cuyo vocabulario
	 * esté configurado para registrar visitas siempre.
	 * @param stats
	 * @param groupCategories
	 * @param categoriesToRegister
	 * @return
	 */
	private boolean checkMetadata(Map<String, String> stats, List<String> categoriesTrakingVisits, List<String> categoriesToRegister)
	{
		String[] categoriesIds = StringUtil.split(stats.get(CATEGORIES_IDS), StringPool.UNDERLINE);
		
		for(String categoryId : categoriesIds)
		{
			if (categoriesTrakingVisits.contains(categoryId))
			{
				categoriesToRegister.add(categoryId);
			}
		}
		
		return categoriesToRegister.size() > 0;
	}

}
