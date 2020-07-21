package com.protecmedia.iter.base.community.manager;

import java.util.Date;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.IterMonitor.Event;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.community.shortener.URLShortener;
import com.protecmedia.iter.base.community.util.CommunityAuthorizerUtil;
import com.protecmedia.iter.base.community.util.IterOAuthClient;
import com.protecmedia.iter.base.service.VisitsStatisticsLocalServiceUtil;
import com.protecmedia.iter.base.service.util.MimeTypeTools;

public class TwitterCommunityManager implements CommunityManager
{
	private static Log _log = LogFactoryUtil.getLog(TwitterCommunityManager.class);
	
	//---------------------------------------------------------------------------------------------------------
	// END POINTS de la API de Twitter
	//---------------------------------------------------------------------------------------------------------
	private static final String TOKEN_REQUEST = "https://api.twitter.com/oauth/request_token";
	private static final String LOGIN_REQUEST = "https://api.twitter.com/oauth/authorize?oauth_token=%s";
	private static final String TOKEN_ACCESS  = "https://api.twitter.com/oauth/access_token";
	private static final String POST_URL      = "https://api.twitter.com/1.1/statuses/update.json";
	
	//---------------------------------------------------------------------------------------------------------
	// Parámetros de configuración de Twitter
	//---------------------------------------------------------------------------------------------------------
	private final String PUBLIC_KEY;
	private final String SECRET_KEY;
	private final String REDIRECT_URI;
	private final String OAUTH_VERIFIER;
	private final long   GROUP_ID;
	private String accountId;
	private String accountName;
	private boolean isLiveEventPost = false;
	private String postId;
	private String postDetailToken;
	
	public TwitterCommunityManager(String redirectURI, long groupId, String oauthVerifier) throws SystemException
	{
		// Carga la configuración de la APP de twitter del grupo.
		try
		{
			this.PUBLIC_KEY = CommunityAuthorizerUtil.getConfigPublicKey(CommunityAuthorizerUtil.TWITTER, groupId);
			this.SECRET_KEY = CommunityAuthorizerUtil.getConfigSecretKey(CommunityAuthorizerUtil.TWITTER, groupId);
		}
		catch (Throwable th)
		{
			throw new SystemException(IterErrorKeys.XYZ_E_SOCIAL_NETWORK_NOT_CONFIGURED_ZYX);
		}
		
		if (Validator.isNull(this.PUBLIC_KEY) || Validator.isNull(this.PUBLIC_KEY))
			throw new SystemException(IterErrorKeys.XYZ_E_SOCIAL_NETWORK_NOT_CONFIGURED_ZYX);
		
		// Añade a la URL de redirección
		this.REDIRECT_URI = redirectURI + "?groupId=" + groupId;
		
		// Añade el verificador de tokens
		this.OAUTH_VERIFIER = oauthVerifier;
		
		// Guarda el grupo
		this.GROUP_ID = groupId;
	}
	
	@Override
	public void authorize(HttpServletResponse response)
	{
		try
		{
			// Inilializa el cliente de OAuth.
			IterOAuthClient iterOAuth = new IterOAuthClient.Builder(TOKEN_REQUEST, PUBLIC_KEY, SECRET_KEY, "POST").callBackURL(REDIRECT_URI).build();
		
			// Solicita el request token.
			String tokenRequestResponse = iterOAuth.post();
			
			// Procesa la respuesta
			HashMap<String, String> responseInfo = CommunityAuthorizerUtil.mapQueryString(tokenRequestResponse);
			String oauthToken = responseInfo.get("oauth_token");
		
			if (Validator.isNull(oauthToken))
			{
				// Crea la página de error.
				String error = tokenRequestResponse;
				if (iterOAuth.getResponseContentType().equals(MimeTypeTools.MIME_APPLICATION_JSON))
				{
					JSONObject json = JSONFactoryUtil.createJSONObject(tokenRequestResponse);
					error = json.getJSONArray("errors").getJSONObject(0).getString("message");	
				}
				else  if (iterOAuth.getResponseContentType().startsWith(MimeTypeTools.MIME_TEXT_HTML))
				{
					Document xml = SAXReaderUtil.read(tokenRequestResponse);
					error = XMLHelper.getStringValueOf(xml, "/hash/error", tokenRequestResponse);
				}
				CommunityAuthorizerUtil.buildResponseErrorPage(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, IterErrorKeys.XYZ_E_COMMUNITY_AUTHORIZATION_FAILED_ZYX, error);
			}
			else
			{
				// Redirecciona al login.
				response.sendRedirect(String.format(LOGIN_REQUEST, oauthToken));
			}
		}
		catch (Throwable th)
		{
			_log.error(th);
			CommunityAuthorizerUtil.buildResponseErrorPage(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, IterErrorKeys.XYZ_E_COMMUNITY_AUTHORIZATION_FAILED_ZYX, th);
		}
	}

	@Override
	public void grant(HttpServletResponse response, String code) throws SystemException
	{
		// Inilializa el cliente de OAuth.
		IterOAuthClient iterOAuth = new IterOAuthClient.Builder(TOKEN_ACCESS, PUBLIC_KEY, SECRET_KEY, "POST")
													   .oAuthToken(code)
													   .oAuthVerifier(OAUTH_VERIFIER)
													   .build();
		// Solicita el access token.
		String tokenRequestResponse = iterOAuth.post();

		try
		{
			// Procesa la respuesta.
			HashMap<String, String> responseInfo = CommunityAuthorizerUtil.mapQueryString(tokenRequestResponse);
			String oAuthToken = responseInfo.get("oauth_token");
			String oAuthTokenSecret = responseInfo.get("oauth_token_secret");
			if (Validator.isNotNull(oAuthToken) && Validator.isNotNull(oAuthTokenSecret))
			{
				// Recupera el nombre del usuario.
				String screenName = getUserScreenName(oAuthToken, oAuthTokenSecret);
				
				// Crea la página de respuesta.
				CommunityAuthorizerUtil.buildResponsePage(response, screenName, oAuthToken + StringPool.COMMA + oAuthTokenSecret);
			}
			else
			{
				// Crea la página de error.
				CommunityAuthorizerUtil.buildResponseErrorPage(response, HttpServletResponse.SC_OK, IterErrorKeys.XYZ_E_COMMUNITY_PERMISSIONS_ERROR_ZYX, tokenRequestResponse);
			}
		}
		catch (Throwable th)
		{
			_log.error(th);
			CommunityAuthorizerUtil.buildResponseErrorPage(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, IterErrorKeys.XYZ_E_COMMUNITY_AUTHORIZATION_FAILED_ZYX, th);
		}
	}
	
	private String oAuthToken = null;
	private String oAuthTokenSecret = null;
	private String title = StringPool.BLANK;
	
	@Override
	public void publish(String articleId, HashMap<String, String> params)
	{
		try
		{
			// Recupera los parámetros. Tienen que llegar 2 credenciales.
			setPublicationParams(articleId, params);
			
			// Recupera el contenido a publicar.
			String postContent = buildPublication(articleId);
			
			// Publica el artículo.
			String response = publishArticle(articleId, oAuthToken, oAuthTokenSecret, postContent);
			
			// Procesa la respuesta
			processPublicationResponse(articleId, response);
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
			String[] credentials = params.get("credentials").split(StringPool.COMMA);
			ErrorRaiser.throwIfFalse(credentials.length == 2 || credentials.length == 4, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			oAuthToken = credentials[0];
			oAuthTokenSecret = credentials[1];
			
			if (credentials.length == 4)
			{
				isLiveEventPost = true;
				postId = credentials[2];
				postDetailToken = credentials[3];
			}
			
			// Recupera el título personalizado
			title = params.get("title");
		}
		catch (Throwable th)
		{
			IterMonitor.logEvent(GROUP_ID, Event.ERROR, new Date(), "Twitter Publisher Error: Wrong credentials", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, th));
			throw th;
		}
	}
	
	private String buildPublication(String articleId) throws Throwable
	{
		// Busca si tiene configurado acortador de URLs
		URLShortener shortener = CommunityAuthorizerUtil.getConfigUrlShortener(CommunityAuthorizerUtil.TWITTER, Long.valueOf(GROUP_ID));
		
		// Recupera el contenido a publicar.
		String articleUrl = isLiveEventPost ?
							CommunityAuthorizerUtil.getArticle(Long.valueOf(GROUP_ID), postDetailToken, articleId, shortener) + StringPool.SLASH + postId :
							CommunityAuthorizerUtil.getArticle(Long.valueOf(GROUP_ID), articleId, shortener);
		
		// Concatena el título personalizado si se indica alguno.
		return Validator.isNull(title) ? articleUrl : new StringBuilder(title).append(StringPool.NEW_LINE).append(articleUrl).toString();
	}
	
	private String publishArticle(String articleId, String oAuthToken, String oAuthTokenSecret, String postContent) throws Throwable
	{
		String response = null;
		
		try
		{
			// Inilializa el cliente de OAuth.
			IterOAuthClient iterOAuth = new IterOAuthClient.Builder(POST_URL, PUBLIC_KEY, SECRET_KEY, "POST")
														   .queryString("status="+postContent)
														   .oAuthToken(oAuthToken)
														   .oAuthTokenSecret(oAuthTokenSecret)
														   .build();
			// Postea el Twit.
			response = iterOAuth.post();
		}
		catch (Throwable th)
		{
			IterMonitor.logEvent(GROUP_ID, Event.ERROR, new Date(), "Twitter Publisher Error: API connection error. Maybe the article has not been published", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, th));
			throw th;
		}
		
		return response;
	}
	
	private void processPublicationResponse(String articleId, String response) throws JSONException
	{
		try
		{
			JSONObject jsonResponse = JSONFactoryUtil.createJSONObject(response);
			JSONArray errors = jsonResponse.getJSONArray("errors");
			if (errors != null && errors.length() > 0)
			{
				JSONObject error = errors.getJSONObject(0);
				String errorDescription = new StringBuilder("Twitter Publisher Error: ").append(error.getString("message")).toString();
				String errortrace       = error.toString(4);
				IterMonitor.logEvent(GROUP_ID, Event.ERROR, new Date(), errorDescription, articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, errortrace));
			}
			else
			{
				IterMonitor.logEvent(GROUP_ID, Event.INFO, new Date(), "Twitter Publisher: Content published", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, response));

				// Registra una anotación en las estadísticas de visitas del artículo
				if (GetterUtil.getBoolean(PortalUtil.getPortalProperties().getProperty("iter.statistics.enabled"), true))
				{
					String annotationLiteral = GetterUtil.getString(GroupConfigTools.getGroupConfigXMLField(GROUP_ID, "visitstatistics", "/visits/annotations/publication/text()"), "Artículo publicado en");
					VisitsStatisticsLocalServiceUtil.addArticleStatisticsAnnotation(GROUP_ID, articleId, annotationLiteral + StringPool.SPACE + " Twitter");
				}
			}
		}
		catch (JSONException e)
		{
			IterMonitor.logEvent(GROUP_ID, Event.ERROR, new Date(), "Twitter Publisher: Unparseable response", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, e));
			throw e;
		}
	}
	
	private String getUserScreenName(String oAuthToken, String oAuthTokenSecret) throws SystemException, JSONException
	{
		// Inilializa el cliente de OAuth.
		IterOAuthClient iterOAuth = new IterOAuthClient.Builder("https://api.twitter.com/1.1/account/verify_credentials.json", PUBLIC_KEY, SECRET_KEY, "GET")
														.oAuthToken(oAuthToken)
														.oAuthTokenSecret(oAuthTokenSecret)
														.build();
		// Solicita la información del usuario.
		String userInfo = iterOAuth.post();
		JSONObject jsonUserInfo = JSONFactoryUtil.createJSONObject(userInfo);
		
		// Recupera el nombre del usuario.
		return jsonUserInfo.getString("screen_name");
	}
}
