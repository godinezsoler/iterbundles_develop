package com.protecmedia.iter.base.community.manager;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.Social.FacebookConstants;
import com.liferay.portal.kernel.Social.FacebookTools;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.render.PageRenderer;
import com.liferay.portal.kernel.render.RenditionMode;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.InstantArticlePageTools;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.IterMonitor.Event;
import com.protecmedia.iter.base.community.shortener.URLShortener;
import com.protecmedia.iter.base.community.util.CommunityAuthorizerUtil;
import com.protecmedia.iter.base.community.util.CommunityHttpClient;
import com.protecmedia.iter.base.service.VisitsStatisticsLocalServiceUtil;

public class FacebookCommunityManager implements CommunityManager
{
	private static Log _log = LogFactoryUtil.getLog(FacebookCommunityManager.class);
	
	//---------------------------------------------------------------------------------------------------------
	// Permisos
	//---------------------------------------------------------------------------------------------------------
	private static final String SCOPE_INSTANT_ARTICLES = "pages_manage_instant_articles";
	private static final String SCOPE_PAGES = "pages_show_list,manage_pages,publish_pages";
	private static final String SCOPE_WALL = "publish_actions";
	private static final String SCOPE_MAIL = "email";
	
	//---------------------------------------------------------------------------------------------------------
	// Llamadas a la API de Facebook
	//---------------------------------------------------------------------------------------------------------
	// Petición de login
	private static final String LOGIN_REQUEST_ENDPOINT 		= "https://www.facebook.com/dialog/oauth?client_id=%s&scope=%s&redirect_uri=%s&state=%s&auth_type=rerequest";
	private static final String TOKEN_REQUEST_ENDPOINT 		= FacebookConstants.API_GRAPH.concat("/oauth/access_token");
	private static final String VALIDATE_USER_ENDPOINT 		= FacebookConstants.API_GRAPH.concat("/me");
	private static final String GET_PERMISSIONS_ENDPOINT 	= FacebookConstants.API_GRAPH.concat("/me/permissions");
	private static final String PAGE_TOKEN_ENDPOINT 		= FacebookConstants.API_GRAPH.concat("/me/accounts");

	//---------------------------------------------------------------------------------------------------------
	// Parámetros
	//---------------------------------------------------------------------------------------------------------
	private final String PUBLIC_KEY;
	private final String SECRET_KEY;
	private final String REDIRECT_URI;
	private final long   GROUP_ID;
	private String scopes;

	private String accountId;
	private String accountName;
	private String pageName;
	private boolean instantArticle;
	private String state;
	
	private String  title     		= StringPool.BLANK;
	private String  token     		= StringPool.BLANK;
	private String  _appSecretProof = StringPool.BLANK;
	private String  pageId    		= StringPool.BLANK;
	private boolean pagePost 		= false;
	private boolean debug 			= false;
	private boolean isLiveEventPost	= false;
	private String  postId    		= StringPool.BLANK;
	private String  postDetailToken	= StringPool.BLANK;
	
	public FacebookCommunityManager(String redirectURI, long groupId, String pageName, boolean instantArticle) throws SystemException
	{
		// Carga la configuración de la APP de facebook del grupo.
		try
		{
			this.PUBLIC_KEY = CommunityAuthorizerUtil.getConfigPublicKey(CommunityAuthorizerUtil.FACEBOOK, groupId);
			this.SECRET_KEY = CommunityAuthorizerUtil.getConfigSecretKey(CommunityAuthorizerUtil.FACEBOOK, groupId);
		}
		catch (Throwable th)
		{
			throw new SystemException(IterErrorKeys.XYZ_E_SOCIAL_NETWORK_NOT_CONFIGURED_ZYX);
		}
		
		if (Validator.isNull(this.PUBLIC_KEY) || Validator.isNull(this.PUBLIC_KEY))
			throw new SystemException(IterErrorKeys.XYZ_E_SOCIAL_NETWORK_NOT_CONFIGURED_ZYX);

		if (Validator.isNotNull(pageName))
			this.pageName = HttpUtil.decodeURL(pageName);
		
		this.instantArticle = instantArticle;
		
		// Construye el parámetro de los permisos
		this.scopes = buildPermissions();
		
		// Añade a la URL de redirección
		this.REDIRECT_URI = redirectURI;
		
		// Codifica el Query string en el campo state para recuperarlo tras la redirección
		this.state = HttpUtil.encodeURL("groupId=" + groupId + "&page=" + pageName + "&instantArticle=" + instantArticle);
		
		this.GROUP_ID = groupId;
	}

	@Override
	public void authorize(HttpServletResponse response)
	{
		try
		{
			// Construye la petición
			String loginRequest = String.format(LOGIN_REQUEST_ENDPOINT, PUBLIC_KEY, scopes, REDIRECT_URI, state);
			// Redirecciona a Facebook
			response.sendRedirect(loginRequest);
		}
		catch (Throwable th)
		{
			_log.error(th.getMessage());
			_log.trace(th);
			CommunityAuthorizerUtil.buildResponseErrorPage(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, IterErrorKeys.XYZ_E_COMMUNITY_AUTHORIZATION_FAILED_ZYX, th);
		}
	}

	@Override
	public void grant(HttpServletResponse response, String code) throws SystemException
	{
		try
		{
			// Obtener el token
			String accessToken 		= getAccessToken(code);
			String appSecretProof	= FacebookTools.get_appsecret_proof(accessToken, SECRET_KEY);
			
			// Recupera el correo electrónico del usuario.
			String userMail = getUserId(accessToken, appSecretProof);
			
			// Recuperar permisos
			HashMap<String, Boolean> permissions = getPermissions(accessToken, appSecretProof);
			
			// Comprobar permisos
			if (checkPermissions(permissions))
			{
				// Intercambiar el token de corta vida por el de larga duración
				String longLivedToken 	= exangeToken(accessToken, appSecretProof);
				appSecretProof			= FacebookTools.get_appsecret_proof(longLivedToken, SECRET_KEY);
				
				if (Validator.isNull(this.pageName))
				{
					// Si la autorización es para el muro del usuario, se crea la respuesta para Milenium
					CommunityAuthorizerUtil.buildResponsePage(response, userMail, longLivedToken);
				}
				else
				{
					// Si la autorización es para una página, recupera las páginas administradas, comprueba
					// los permisos del usuario y si es correcto crea la respuesta para Milenium
					String pageToken = getPageToken(longLivedToken, appSecretProof);
					
					if (Validator.isNotNull(pageToken))
					{
						CommunityAuthorizerUtil.buildResponsePage(response, userMail, pageToken);
						
						// Antes de terminar, actualiza el listado de páginas
						InstantArticlePageTools.updateInstantArticlePagesMap(GROUP_ID, Long.valueOf(pageId), pageName);
					}
					// Faltan permisos de edición en la página
					else
					{
						CommunityAuthorizerUtil.buildResponseErrorPage(response, HttpServletResponse.SC_OK, IterErrorKeys.XYZ_E_COMMUNITY_PERMISSIONS_ERROR_ZYX, "El usuario no tiene permisos de edición en la página");
					}
				}
			}
			// Faltan permisos
			else
			{
				CommunityAuthorizerUtil.buildResponseErrorPage(response, HttpServletResponse.SC_OK, IterErrorKeys.XYZ_E_COMMUNITY_PERMISSIONS_ERROR_ZYX, "No se han concedido los permisos solicitados");
			}
		}
		catch (Throwable th)
		{
			_log.error(th);
			CommunityAuthorizerUtil.buildResponseErrorPage(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, IterErrorKeys.XYZ_E_COMMUNITY_AUTHORIZATION_FAILED_ZYX, th);
		}
	}
	
	
	private String getAccessToken(String code) throws SystemException
	{
		CommunityHttpClient tokenRequest = new CommunityHttpClient.Builder(TOKEN_REQUEST_ENDPOINT)
																  .queryString("client_id", PUBLIC_KEY)
																  .queryString("redirect_uri", REDIRECT_URI)
																  .queryString("client_secret", SECRET_KEY)
																  .queryString("code", code)
																  .build();
		JSONObject JSONToken = tokenRequest.get();
		return JSONToken.getString("access_token");
	}
	
	private String getUserId(String accessToken, String appSecretProof) throws SystemException
	{
		CommunityHttpClient validateUserRequest = new CommunityHttpClient.Builder(VALIDATE_USER_ENDPOINT)
																		 .queryString("access_token", accessToken)
																		 .queryString(FacebookConstants.PARAM_APPSECRET_PROOF, appSecretProof)
																		 .queryString("fields", "email")
																		 .build();
		JSONObject JSONValidateUser = validateUserRequest.get();
		return JSONValidateUser.getString("email");
	}
	
	private HashMap<String, Boolean> getPermissions(String accessToken, String appSecretProof) throws SystemException 
	{
		HashMap<String, Boolean> permissions = new HashMap<String, Boolean>();
		
		CommunityHttpClient userPermissionsRequest = new CommunityHttpClient.Builder(GET_PERMISSIONS_ENDPOINT)
																			.queryString("access_token", accessToken)
																			.queryString(FacebookConstants.PARAM_APPSECRET_PROOF, appSecretProof)
																			.build();
		JSONObject JSONUserPermissions = userPermissionsRequest.get();
		
		JSONArray userPermissions = JSONUserPermissions.getJSONArray("data");
		for (int i=0; i<userPermissions.length(); i++)
		{
			JSONObject permission = userPermissions.getJSONObject(i);
			
			permissions.put(permission.getString("permission"), "granted".equals(permission.getString("status")) ? Boolean.TRUE : Boolean.FALSE);
		}
		
		return permissions;
	}
	
	private String exangeToken(String accessToken, String appSecretProof) throws SystemException 
	{
		CommunityHttpClient tokenExangeRequest = new CommunityHttpClient.Builder(TOKEN_REQUEST_ENDPOINT)
																		.queryString("grant_type", "fb_exchange_token")
																		.queryString("client_id", PUBLIC_KEY)
																		.queryString("client_secret", SECRET_KEY)
																		.queryString("fb_exchange_token", accessToken)
																		.queryString(FacebookConstants.PARAM_APPSECRET_PROOF, accessToken)
																		.build();
		JSONObject JSONExange = tokenExangeRequest.get();
		return JSONExange.getString("access_token");
	}
	
	private String getPageToken(String longLivedToken, String appSecretProof) throws SystemException
	{
		String pageToken = StringPool.BLANK;
		

		CommunityHttpClient pageAccessRequest = new CommunityHttpClient.Builder(PAGE_TOKEN_ENDPOINT)
																		.queryString("access_token", longLivedToken)
																		.queryString(FacebookConstants.PARAM_APPSECRET_PROOF, appSecretProof)
																		.build();
		JSONObject JSONPageAccess = pageAccessRequest.get();
		
		JSONArray pages = JSONPageAccess.getJSONArray("data");
		for (int i=0; i<pages.length(); i++)
		{
			JSONObject page = pages.getJSONObject(i);
			String pageName		= page.getString("name");
			JSONArray pagePerms	= page.getJSONArray("tasks");
			
			// Si es la página solicitada
			if (this.pageName.equals(pageName))
			{
				// Si hay permisos de escritura
				for (int j=0; j<pagePerms.length(); j++)
				{
					if ("CREATE_CONTENT".equals(pagePerms.getString(j)))
					{
						pageId = page.getString("id");
						pageToken = page.getString("access_token") + StringPool.COMMA + pageId + StringPool.COMMA + (instantArticle ? "InstantArticle" : "Status");
						break;
					}
				}
				break;
			}
		}
		
		return pageToken;
	}
	
	private boolean checkPermissions(HashMap<String, Boolean> permissions)
	{
		boolean granted = true;
		
		if (_log.isTraceEnabled())
		{
			_log.trace( String.format("Required permissions: %s", scopes) );
			
			StringBuilder otorged = new StringBuilder();
			for (Map.Entry<String, Boolean> entry : permissions.entrySet()) 
				otorged.append( String.format("%s(%b)\n", entry.getKey(), entry.getValue()) );

			_log.trace( String.format("Otorged permissions: %s", otorged.toString()) );
		}
		
		// Para cada permiso solicitado
		for (String userPermission : this.scopes.split(StringPool.COMMA))
		{
			Boolean permission = permissions.get(userPermission);
			// Si no se ha concedido
			if (Validator.isNull(permission) || !permission)
			{
				// Retorna falso
				granted = false;
			}
		}
		
		return granted;
	}
	
	private String buildPermissions()
	{
		StringBuilder scopes = Validator.isNull(pageName) ? new StringBuilder(SCOPE_WALL) : new StringBuilder(SCOPE_PAGES);
		if (instantArticle)
			scopes.append(StringPool.COMMA).append(SCOPE_INSTANT_ARTICLES);
		scopes.append(StringPool.COMMA).append(SCOPE_MAIL);
		return scopes.toString();
	}
	
	@Override
	public void publish(String articleId, HashMap<String, String> params)
	{
		try
		{
			// Comprueba las credenciales y el tipo de publicación.
			setPublicationParams(articleId, params);
		
			// Crea el contenido a publicar.
			String postContent = buildPublication(articleId);
			
			// Construye la URL de la API.
			String url = buildApiURL();
			
			// Publica el artículo.
			JSONObject response = publishArticle(articleId, url, postContent);
			
			// Procesa la respuesta.
			String processId = processPublicationResponse(articleId, response);
			
			// Si hay processId, lo añade al mapa de parámetros
			if (processId != null)
				params.put("processId", processId);
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
	}
	
	private void setPublicationParams(String articleId, HashMap<String, String> params) throws Throwable
	{
		try
		{
			// Recupera el Id de la cuenta para trazarla en el Monitor
			accountId = params.get("accountId");
			ErrorRaiser.throwIfNull(accountId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

			// Recupera el nombre de la cuenta para trazarla en el Monitor
			accountName = params.get("accountName");
			ErrorRaiser.throwIfNull(accountName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			// Recupera las credenciales.
			String sCredentials = params.get("credentials");
			ErrorRaiser.throwIfNull(sCredentials, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			// Recupera el título personalizado
			title = params.get("title");
			
			// Comprueba si es modo debug
			String[] debugCredentials = sCredentials.split(StringPool.UNDERLINE);
			sCredentials = debugCredentials[0];
			if (debugCredentials.length == 2 && "debug".equalsIgnoreCase(debugCredentials[1]))
				debug = true;
			
			// Recupera el token de acceso
			String[] credentials = sCredentials.split(StringPool.COMMA);
			token = credentials[0];
			ErrorRaiser.throwIfNull(token, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			_appSecretProof = FacebookTools.get_appsecret_proof(token, SECRET_KEY);
			
			// Comprueba si es un post en el muro, en una página o un Instant Article.
			switch (credentials.length)
			{
				case 1:
					pagePost = false;
					instantArticle = false;
					break;
				case 3:
					pagePost = true;
					pageId = credentials[1];
					instantArticle = "InstantArticle".equals(credentials[2]);
					break;
				case 5:
					pagePost = true;
					pageId = credentials[1];
					instantArticle = false;
					isLiveEventPost = true;
					postId = credentials[3];
					postDetailToken = credentials[4];
					break;
					
				default:
					ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			}
		}
		catch (Throwable th)
		{
			IterMonitor.logEvent(GROUP_ID, Event.ERROR, new Date(), "Facebook Publisher Error: Wrong credentials", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, th));
			throw th;
		}
	}
	
	private String buildApiURL()
	{
		String url;
		if (instantArticle)
		{
			url = String.format("%s/%s/instant_articles", FacebookConstants.API_GRAPH, pageId);
		}
		else if (pagePost)
			url = String.format("%s/%s/feed", FacebookConstants.API_GRAPH, pageId);
		else
			url = FacebookConstants.API_GRAPH.concat("/me/feed");

		if (_log.isDebugEnabled()) _log.debug("Publishing in Facebook using api url: " + url);
		
		return url;
	}
	
	private String buildPublication(String articleId) throws Throwable
	{
		String postContent = StringPool.BLANK;
		
		try
		{	
			if (instantArticle)
			{
				if (_log.isDebugEnabled()) _log.debug("Creating content for Facebook Instant Article");
				postContent = PageRenderer.getArticle(RenditionMode.ia, Long.valueOf(GROUP_ID), articleId);
			}
			else
			{
				if (_log.isDebugEnabled()) _log.debug("Creating content for Facebook Post");
				// Busca si tiene configurado acortador de URLs
				URLShortener shortener = CommunityAuthorizerUtil.getConfigUrlShortener(CommunityAuthorizerUtil.FACEBOOK, Long.valueOf(GROUP_ID));
				// Recupera el contenido a publicar.
				postContent = isLiveEventPost ?
							  CommunityAuthorizerUtil.getArticle(Long.valueOf(GROUP_ID), postDetailToken, articleId, shortener) + StringPool.SLASH + postId :
							  CommunityAuthorizerUtil.getArticle(Long.valueOf(GROUP_ID), articleId, shortener);
			}
			if (_log.isDebugEnabled()) _log.debug(postContent);
			
			// Construye el contenido a publicar	 
			if (pagePost && instantArticle)
			{
				postContent = "html_source="+HttpUtil.encodeURL(postContent);
			}
			else
			{
				postContent = new StringBuilder()
				.append("link=").append(HttpUtil.encodeURL(postContent))
				.append("&message=").append(Validator.isNotNull(title) ? HttpUtil.encodeURL(title) : HttpUtil.encodeURL(postContent))
				.toString();
			}
		}
		catch (Throwable th)
		{
			IterMonitor.logEvent(GROUP_ID, Event.ERROR, new Date(), "Facebook Publisher Error: Unable to generate content", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, th));
			throw th;
		}

		return postContent;
	}
	
	private JSONObject publishArticle(String articleId, String url, String postContent) throws Throwable
	{
		JSONObject response = null;
		try
		{
			CommunityHttpClient httpClient = null;
			
			if (instantArticle)
			{
				if (debug)
				{
					if (_log.isDebugEnabled()) _log.debug("Publishing Debug Instant Article...");
					httpClient = new CommunityHttpClient.Builder(url)
								.queryString("access_token", token)
								.queryString(FacebookConstants.PARAM_APPSECRET_PROOF, _appSecretProof)
								.queryString("published", "false")
								.queryString("development_mode", "true")
								.header("Content-Type", "application/x-www-form-urlencoded")
								.payload(postContent)
								.build();
				}
				else
				{
					if (_log.isDebugEnabled()) _log.debug("Publishing Instant Article...");
					httpClient = new CommunityHttpClient.Builder(url)
								.queryString("access_token", token)
								.queryString(FacebookConstants.PARAM_APPSECRET_PROOF, _appSecretProof)
								.queryString("published", "true")
								.header("Content-Type", "application/x-www-form-urlencoded")
								.payload(postContent)
								.build();
				}
			}
			else
			{
				if (_log.isDebugEnabled()) _log.debug("Publishing Facebook post...");
				httpClient = new CommunityHttpClient.Builder(url)
								.queryString("access_token", token)
								.queryString(FacebookConstants.PARAM_APPSECRET_PROOF, _appSecretProof)
								.header("Content-Type", "application/x-www-form-urlencoded")
								.payload(postContent)
								.build();
			}
			
			response = httpClient.post();
			if (_log.isDebugEnabled()) _log.debug("Publishing Response: " + response);
		}
		catch (Throwable th)
		{
			IterMonitor.logEvent(GROUP_ID, Event.ERROR, new Date(), "Facebook Publisher Error: API connection error. Maybe the article has not been published", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, th));
			throw th;
		}
		
		return response;
	}
	
	private String processPublicationResponse(String articleId, JSONObject response) throws JSONException
	{
		String processId = null;
		
		try
		{
			JSONObject error = response.getJSONObject("error");
			if (error != null)
			{
				String errorDescription = new StringBuilder("Facebook Publisher Error: ").append(error.getString("message")).toString();
				String errortrace       = error.toString(4);
				IterMonitor.logEvent(GROUP_ID, Event.ERROR, new Date(), errorDescription, articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, errortrace));
			}
			else
			{
				String resultTrace = response.toString(4);
				IterMonitor.logEvent(GROUP_ID, instantArticle ? Event.DEBUG : Event.INFO, new Date(), "Facebook Publisher: Content published", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, resultTrace));
				
				// Registra una anotación en las estadísticas de visitas del artículo
				if (!instantArticle && GetterUtil.getBoolean(PortalUtil.getPortalProperties().getProperty("iter.statistics.enabled"), true))
				{
					String annotationLiteral = GetterUtil.getString(GroupConfigTools.getGroupConfigXMLField(GROUP_ID, "visitstatistics", "/visits/annotations/publication/text()"), "Artículo publicado en");
					VisitsStatisticsLocalServiceUtil.addArticleStatisticsAnnotation(GROUP_ID, articleId, annotationLiteral + StringPool.SPACE + " Facebook");
				}
				
				// Si es un IA, recupera el process-id y lo devuelve
				if (instantArticle && Validator.isNotNull(response.getString("id")))
						processId = response.getString("id");
			}
		}
		catch (JSONException e)
		{
			IterMonitor.logEvent(GROUP_ID, Event.ERROR, new Date(), "Facebook Publisher: Unparseable response", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, e));
			throw e;
		}
		
		return processId;
	}
}
