package com.protecmedia.iter.news.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.liferay.portal.apache.ApacheHierarchy;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.util.IterHttpClient;
import com.protecmedia.iter.base.cluster.Heartbeat;

public enum ArticleVisitorsCollector implements Runnable
{
	INSTANCE;
	
	private static Log _log = LogFactoryUtil.getLog(ArticleVisitorsCollector.class);
	private static ExecutorService  _executorService = null;
	private static FutureTask<Void> _future = null;
	
	// Constantes para la gestión de las fechas
	private static final String GROUP_CONFIG_COLUMN = "articlevisitors";
	private static final String GROUP_CONFIG_FIELD_LASTUPDATE = "lastUpdate";
	
	// Constantes para MAS
	private static final String MAS_REQUEST_ENDPOINT = "/restapi/user/getArticlesVisitors/";
	private static final String MAS_REQUEST_FIELD_STATUS = "status";
	private static final String MAS_REQUEST_FIELD_DATA = "data";
	
	// Constantes para Solr
	private static final String SOLR_UPDATE_ENDPOINT = "/update?commit=true";
	private static final String SOLR_FIELD_ID = "id";
	private static final String SOLR_FIELD_GRPUPID = "groupId";
	private static final String SOLR_FIELD_ARTICLEID = "articleId";
	private static final String SOLR_FIELD_VISITORID = "visitorId";
	
	private String solrEndpoint = null;
	private String masEndpoint = null;

	@Override
	public void run()
	{
		_log.info("Starting the ArticleVisitorsCollector process");
		
		try
		{
			// Cuando está el modo antiguo (ni el Watchdog y ni Heartbeat) se espera un tiempo inicial
			if (isEnable() && (!PropsValues.CLUSTER_HEARTBEAT_ENABLED && PropsValues.SCHEDULER_ENABLED))
				Thread.sleep( PropsValues.ARTICLE_VISITORS_COLLECTOR_WAIT );
			
			while (isEnable())
			{
				collectArticleVisitors();
				
				if (_log.isTraceEnabled())
					_log.trace( String.format("The ArticleVisitorsCollector will sleep %d ms", PropsValues.ARTICLE_VISITORS_COLLECTOR_FREQUENCY) );
				Thread.sleep( PropsValues.ARTICLE_VISITORS_COLLECTOR_FREQUENCY );
			}
		}
		catch (InterruptedException ie)
		{
			Thread.currentThread().interrupt();
			_log.trace(ie.toString());
		}

		_log.info("The ArticleVisitorsCollector process has been finished");
	}
	
	synchronized static public void start() throws ServiceError
	{
		_log.trace("start: Begin");
		
		ErrorRaiser.throwIfFalse( !isInProcess(), IterErrorKeys.XYZ_E_ARTICLEVISITORSCOLLECTOR_IS_ALREADY_RUNNING_ZYX ); 
		
		if (canLaunchProcess())
		{
			_future 		= new FutureTask<Void>(INSTANCE, null);
			_executorService= Executors.newSingleThreadExecutor();
			
			_executorService.execute( _future );
		
			_log.info("The ArticleVisitorsCollector mechanism has started");
		}
		else
		{
			_log.warn("The ArticleVisitorsCollector is disable in this server (scheduler.enabled/cluster.heartbeat and " + PropsKeys.ARTICLE_VISITORS_COLLECTOR_ENABLED + ")");
		}

		_log.trace("start: End");
	}
	
	synchronized static public void stop() throws ServiceError, InterruptedException
	{
		_log.trace("stop: Begin");
		boolean stopOK = false;

		ErrorRaiser.throwIfFalse( isInProcess(), IterErrorKeys.XYZ_E_ARTICLEVISITORSCOLLECTOR_IS_NOT_RUNNING_ZYX ); 
		
		try
		{
			_executorService.shutdownNow();
			stopOK = _executorService.awaitTermination(PropsValues.ARTICLE_VISITORS_COLLECTOR_TIMEOUT, TimeUnit.MILLISECONDS);
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
		return PropsValues.ARTICLE_VISITORS_COLLECTOR_ENABLED && Heartbeat.canLaunchProcesses();
	}
	
	static private boolean isInProcess()
	{
		boolean inProcess = _future != null 				&& _executorService != null 		&& 
							!_executorService.isShutdown() 	&& !_executorService.isTerminated()	&&
							!_future.isCancelled()			&& !_future.isDone();

		return inProcess;
	}

	private boolean isEnable()
	{
		return 	Thread.currentThread().isAlive() && !Thread.currentThread().isInterrupted() &&
				Heartbeat.canLaunchProcesses();
	}
	
	private void collectArticleVisitors()
	{
		_log.debug("Collecting relationship of articles and visitors: START");
		try
		{
			// Comprueba los endpoints de MAS y de Solr
			_log.debug("Checking endpoints configuration");
			configureEndpoints();
			
			// Obtiene los sitios con un idSite de MAS configurado
			_log.debug("Getting groups");
			Map<Group, String> sites = getSites();

			// Calcula la fecha actual
			long currentDate = System.currentTimeMillis() / 1000L;
			
			// Calcula una fecha límite para usar si es la primera llamada de un grupo.
			// Será la fecha actual menos el tiempo configurado de lanzamiento del proceso.
			long defaultMinDate = currentDate - (PropsValues.ARTICLE_VISITORS_COLLECTOR_FREQUENCY / 1000L);

			_log.debug(String.format("Procesing %d groups", sites.size()));
			for (Group group : sites.keySet())
			{
				_log.debug(String.format("Collecting relationship of articles and visitors for group %d", group.getGroupId()));
				long groupId = group.getGroupId();
				try
				{
					// Obtiene el idSite de MAS
					String idSite = sites.get(group);
					
					// Obtiene la fecha desde la última petición o la fecha por defecto
					long lastUpdate = getLastUpdate(groupId, defaultMinDate);
					
					// Pide la relación de artículos-visitantes desde la fecha indicada
					_log.debug(String.format("Requesting MAS data for site %s and date range %d - %d", idSite, lastUpdate, currentDate));
					Map<String, List<String>> articles = requestMas(groupId, idSite, lastUpdate, currentDate);
					
					// Almacena la información en Solr
					_log.debug(String.format("Updating %d documents in Solr", articles.size()));
					updateSolr(group, articles);
				}
				catch (Throwable th)
				{
					handleError(th);
				}
				finally
				{
					// Actualiza la fecha de la última petición
					_log.debug("Updating last process date");
					setLastUpdate(groupId, currentDate);
				}
			}
		}
		catch (Throwable th)
		{
			handleError(th);
		}
		_log.debug("Collecting relationship of articles and visitors: END");
	}

	/**
	 * Crea el endpoint de MAS para pedir las relaciones y el de la colección Solr en la que guardarlas.
	 * @throws ServiceError {@code XYZ_E_ARTICLEVISITORSCOLLECTOR_MAS_ENDPOINT_ZYX} si no se puede crear el endpoint de MAS por problemas al recuperar el Apache maestro.
	 * @throws ServiceError {@code XYZ_E_ARTICLEVISITORSCOLLECTOR_SOLR_ENDPOINT_ZYX} si el endpoint de Solr no está configurado.
	 */
	private void configureEndpoints() throws ServiceError
	{
		// Crea el endpoint de MAS
		String[] masterList = null;
		try
		{
			masterList = ApacheHierarchy.getInstance().getMasterList();
			ErrorRaiser.throwIfFalse( Validator.isNotNull(masterList) && masterList.length > 0, IterErrorKeys.XYZ_E_ARTICLEVISITORSCOLLECTOR_MAS_ENDPOINT_ZYX);
			this.masEndpoint = masterList[0] + MAS_REQUEST_ENDPOINT;
		}
		catch (Throwable th)
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_ARTICLEVISITORSCOLLECTOR_MAS_ENDPOINT_ZYX);
		}
		
		String solrEndpoint = PropsValues.ITER_SEARCH_PLUGIN_ENDPOINT;
		ErrorRaiser.throwIfFalse(Validator.isNotNull(solrEndpoint), IterErrorKeys.XYZ_E_ARTICLEVISITORSCOLLECTOR_SOLR_ENDPOINT_ZYX);
		solrEndpoint = solrEndpoint.endsWith(StringPool.SLASH) ? solrEndpoint.substring(0, solrEndpoint.length() - 1) : solrEndpoint;
		this.solrEndpoint = solrEndpoint + "_recommendations";
	}
	
	/**
	 * Recupera todos los grupos que tienen integración con MAS (tienen configurado un idsite y las métricas están habilitadas).
	 * @return Mapa de identificadores de grupos en ITER con su respectivo identificador en MAS.
	 * @throws ServiceError Si ocurre un error al recuperar los grupos.
	 */
	private Map<Group, String> getSites() throws ServiceError
	{
		Map<Group, String> sites = new HashMap<Group, String>();
		
		try
		{
			// Obtiene los grupos
			List<Group> groups = GroupLocalServiceUtil.getScopeGroups();
			for (Group group : groups)
			{
				// Recupera el Id del grupo
				long groupId = group.getGroupId();
				// Obtiene el idSite de MAS
				String idSite = GroupConfigTools.getGroupConfigXMLField(groupId, "googletools", "/google/metricsmas/@appid");
				// Comprueba que esté habilitada la integración con MAS
				boolean masEnabled = GetterUtil.getBoolean(GroupConfigTools.getGroupConfigXMLField(groupId, "googletools", "/google/metricsmas/@enablemetrics"), false) && Validator.isNotNull(idSite);
				// Si está configurada y habilitada la integración con MAS, añade el idSite a la lista
				if (masEnabled)
					sites.put(group, idSite);
			}
		}
		catch (Throwable th)
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_ARTICLEVISITORSCOLLECTOR_GET_SITES_ZYX, th.getMessage(), th.getStackTrace());
		}
		
		return sites;
	}
	
	/**
	 * Obtiene la última fecha de petición para el grupo.
	 * @param groupId El identificador del grupo.
	 * @param defaultMinDate La fecha por defecto a utilizar si no se encuentra otra, en formato {@code UNIX EPOCH}.
	 * @return La fecha de la última ejecución del proceso en formato {@code UNIX EPOCH}, o la fecha por defecto si no se encuentra.
	 */
	private long getLastUpdate(long groupId, long defaultMinDate)
	{
		long minDate = defaultMinDate;
		
		JsonObject json = GroupConfigTools.getGroupConfigJSONField(groupId, GROUP_CONFIG_COLUMN);
		if (json.has(GROUP_CONFIG_FIELD_LASTUPDATE))
			minDate = json.get(GROUP_CONFIG_FIELD_LASTUPDATE).getAsLong();

		// Si la fecha de la última petición difiere con respecto a la fecha por defecto en más de la mitad del tiempo de lanzamiento del proceso,
		// es que ha habido un problema y el proceso se está ejecutando después de lo previsto.
		// Se pondrá en marcha el proceso de recuperación, usándose para esta petición la fecha por defecto.
		long diff = minDate - defaultMinDate;
		if (diff < 0 && Math.abs(diff) > ((PropsValues.ARTICLE_VISITORS_COLLECTOR_FREQUENCY / 1000L) / 2))
		{
			setFailedUpdate(groupId, minDate, defaultMinDate);
			minDate = defaultMinDate;
		}
		
		return minDate;
	}
	
	/**
	 * Pide a MAS la lista de artículos y sus visitantes de un sitio desde la fecha indicada.
	 * @param groupId El identificador del grupo.
	 * @param idSite El identificador del sitio en MAS.
	 * @param minDate La fecha desde la que recuperar relaciones.
	 * @return {@code Map<String, List<String>>} con los artículos y sus visitantes.
	 * @throws ServiceError {@code XYZ_E_ARTICLEVISITORSCOLLECTOR_MAS_CONNECTION_ZYX} si no se pudo contactar con MAS.
	 * @throws ServiceError {@code XYZ_E_ARTICLEVISITORSCOLLECTOR_MAS_WRONGRESPONSE_ZYX} si la respuesta de MAS no es la esperada.
	 */
	private Map<String, List<String>> requestMas(long groupId, String idSite, long minDate, long maxDate) throws ServiceError
	{
		Map<String, List<String>> articles = new HashMap<String, List<String>>();

		// Recupera el virtualhost
		String virtualHost = getVirtualHost(groupId);
		
		// Crea la conexión con MAS
		String url = masEndpoint + idSite + StringPool.SLASH + minDate + StringPool.SLASH + maxDate;
		IterHttpClient ihttpc = new IterHttpClient.Builder(IterHttpClient.Method.GET, url)
		.connectionTimeout(20000)
		.readTimeout(20000)
		.header(WebKeys.HOST, virtualHost)
		.header(HttpHeaders.USER_AGENT, WebKeys.USER_AGENT_ITERWEBCMS)
		.build();
		
		// Realiza la petición
		String response = ihttpc.connect();
		
		// Comprueba que la respuesta sea válida
		ErrorRaiser.throwIfFalse(ihttpc.validResponse(), IterErrorKeys.XYZ_E_ARTICLEVISITORSCOLLECTOR_MAS_CONNECTION_ZYX);
		JsonObject jsonResponse = new JsonObject();
		try
		{
			// Parsea la respuesta
			JsonElement masResponse = (new JsonParser()).parse(response);
			if (masResponse.isJsonArray())
				masResponse = masResponse.getAsJsonArray().get(0);
			jsonResponse = masResponse.getAsJsonObject();
		}
		catch (Throwable th)
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_ARTICLEVISITORSCOLLECTOR_MAS_WRONGRESPONSE_ZYX, th.getMessage(), th.getStackTrace());
		}
			
		// Comprueba que sea correcta
		JsonObject data = new JsonObject();
		if (jsonResponse.has(MAS_REQUEST_FIELD_STATUS) && jsonResponse.get(MAS_REQUEST_FIELD_STATUS).getAsBoolean() && jsonResponse.has(MAS_REQUEST_FIELD_DATA))
		{
			if (jsonResponse.get(MAS_REQUEST_FIELD_DATA).isJsonObject())
				data = jsonResponse.get(MAS_REQUEST_FIELD_DATA).getAsJsonObject();
		}
		else
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_ARTICLEVISITORSCOLLECTOR_MAS_WRONGRESPONSE_ZYX);
		}
		
		// Procesa la respuesta de MAS
		Set<Entry<String, JsonElement>> entrySet = data.entrySet();
		for(Map.Entry<String,JsonElement> entry : entrySet)
		{
			List<String> visitors = new Gson().fromJson(entry.getValue(), new TypeToken<List<String>>() {}.getType());
		    articles.put(entry.getKey(), visitors);
		}
		
		return articles;
	}
	
	/**
	 * Indexa en Solr un documento por cada artículo con sus visitantes.
	 * @param groupId El identificador del grupo.
	 * @param articles El mapa de artículos y sus listas de visitantes.
	 * @throws ServiceError {@code XYZ_E_ARTICLEVISITORSCOLLECTOR_SOLR_CONNECTION_ZYX} Si ocurre un eror al conectar con el Solr.
	 */
	private void updateSolr(Group group, Map<String, List<String>> articles) throws ServiceError
	{
		List<String> articleIds = new ArrayList<String>(articles.keySet());
		
		while (articleIds.size() > 0)
		{
			// Obtiene un bloque
			List<String> block = articleIds.subList(0, Math.min(PropsValues.ARTICLE_VISITORS_COLLECTOR_UPDATE_BLOCK, articleIds.size()));
			
			// Crea el payload
			JsonArray payload = new JsonArray();
			for (String articleId : block)
			{
				JsonObject doc = new JsonObject();
				doc.addProperty(SOLR_FIELD_ID, PortalUUIDUtil.generate());
				doc.addProperty(SOLR_FIELD_GRPUPID, group.getGroupId());
				doc.addProperty(SOLR_FIELD_ARTICLEID, articleId);
				doc.add(SOLR_FIELD_VISITORID, new Gson().toJsonTree(articles.get(articleId), new TypeToken<List<String>>() {}.getType()));
				payload.add(doc);
			}
			
			// Manda los documentos al Solr
			String updateEndpoint = solrEndpoint + group.getDelegationId() + SOLR_UPDATE_ENDPOINT;
			IterHttpClient ihttpc = new IterHttpClient.Builder(IterHttpClient.Method.POST, updateEndpoint)
			.header(HttpHeaders.CONTENT_TYPE, ContentTypes.APPLICATION_JSON)
			.payLoad(payload.toString())
			.build();
			
			String response = ihttpc.connect();
			
			if (!ihttpc.validResponse())
			{
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_ARTICLEVISITORSCOLLECTOR_SOLR_CONNECTION_ZYX, response);
			}
			
			// Elimina los procesados del listado principal
			block.clear();
		}
	}
	
	/**
	 * Actualiza la fecha de la última petición para el grupo.
	 * @param groupId
	 * @param date
	 */
	private void setLastUpdate(long groupId, long date)
	{
		JsonObject json = GroupConfigTools.getGroupConfigJSONField(groupId, GROUP_CONFIG_COLUMN);
		json.addProperty(GROUP_CONFIG_FIELD_LASTUPDATE, date);

		try
		{
			GroupConfigTools.setGroupConfigField(groupId, GROUP_CONFIG_COLUMN, json.toString());
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
	}
	
	/**
	 * Anota un rango de fechas para reprocesar
	 * @param groupId
	 * @param fromDate
	 * @param toDate
	 */
	private void setFailedUpdate(long groupId, long fromDate, long toDate)
	{
		// Recupera el array de fallos o crea uno nuevo si no existe.
		JsonObject json = GroupConfigTools.getGroupConfigJSONField(groupId, GROUP_CONFIG_COLUMN);
		JsonArray failedUpdates = new JsonArray();
		if (json.has("failedUpdates") && json.get("failedUpdates").isJsonArray())
			failedUpdates = json.get("failedUpdates").getAsJsonArray();
		
		// Crea el objeto de error
		JsonObject error = new JsonObject();
		error.addProperty("from", fromDate);
		error.addProperty("to", toDate);
		failedUpdates.add(error);
		json.add("failedUpdates", failedUpdates);

		// Lo guarda
		try
		{
			GroupConfigTools.setGroupConfigField(groupId, GROUP_CONFIG_COLUMN, json.toString());
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
	}
	
	private void handleError(Throwable th)
	{
		_log.error(th);
//		if (th instanceof ServiceError)
//		{
//			String error = ((ServiceError) th).getErrorCode();
//			// No se ha podido recuperar el Apache maestro para crear el endpoint de MAS
//			if (IterErrorKeys.XYZ_E_ARTICLEVISITORSCOLLECTOR_MAS_ENDPOINT_ZYX.equals(error))
//			{
//				// No ejecutar el proceso
//			}
//			// No hay endpoint de Solr configurado
//			else if (IterErrorKeys.XYZ_E_ARTICLEVISITORSCOLLECTOR_SOLR_ENDPOINT_ZYX.equals(error))
//			{
//				// No ejecutar el proceso
//			}
//			// No se han podido recuperar los grupos a procesar
//			else if (IterErrorKeys.XYZ_E_ARTICLEVISITORSCOLLECTOR_GET_SITES_ZYX.equals(error))
//			{
//				// registrar error y replanificar el proceso
//			}
//			// No se han podido recuperar el virtualhost del grupo
//			else if (IterErrorKeys.XYZ_E_ARTICLEVISITORSCOLLECTOR_GET_VIRTUALHOST_ZYX.equals(error))
//			{
//				// No ejecutar el proceso para el grupo
//			}
//			// Falló la conexión con MAS para recuperar las relaciones
//			else if (IterErrorKeys.XYZ_E_ARTICLEVISITORSCOLLECTOR_MAS_CONNECTION_ZYX.equals(error))
//			{
//				// No ejecutar el proceso para el grupo
//			}
//			// La respuesta de MAS de la petición de relaciones no es la esperada
//			else if (IterErrorKeys.XYZ_E_ARTICLEVISITORSCOLLECTOR_MAS_WRONGRESPONSE_ZYX.equals(error))
//			{
//				// No ejecutar el proceso para el grupo
//			}
//			// Error al cargar los documentos en el Solr
//			else if (IterErrorKeys.XYZ_E_ARTICLEVISITORSCOLLECTOR_SOLR_CONNECTION_ZYX.equals(error))
//			{
//				// No ejecutar el proceso para el grupo
//			}
//			else
//			{
//			}
//		}
//		else
//		{
//		}
	}
	
	/**
	 * Recupera el virtualhost del grupo indicado.
	 * @param groupId El identificador del grupo.
	 * @return {@code String} con el virtualhost del grupo.
	 * @throws ServiceError {@code XYZ_E_ARTICLEVISITORSCOLLECTOR_GET_VIRTUALHOST_ZYX} Si no se puede recuperar
	 *         el virtualhost por no existir el grupo o no estar configurado.
	 */
	private String getVirtualHost(long groupId) throws ServiceError
	{
		String virtualHost = null;
		try
		{
			virtualHost = LayoutSetLocalServiceUtil.getLayoutSet(groupId, false).getVirtualHost();
		}
		catch (Throwable th)
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_ARTICLEVISITORSCOLLECTOR_GET_VIRTUALHOST_ZYX);
		}
		ErrorRaiser.throwIfFalse(Validator.isNotNull(virtualHost), IterErrorKeys.XYZ_E_ARTICLEVISITORSCOLLECTOR_GET_VIRTUALHOST_ZYX);
		return virtualHost;
	}
}
