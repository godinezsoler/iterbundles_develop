package com.protecmedia.iter.base.community.manager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;

import com.google.gson.JsonObject;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.util.IterHttpClient;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.IterMonitor.Event;
import com.protecmedia.iter.base.community.util.CommunityAuthorizerUtil;
import com.protecmedia.iter.base.service.VisitsStatisticsLocalServiceUtil;

public class NotificationsCommunityManager implements CommunityManager
{
	private static Log _log = LogFactoryUtil.getLog(NotificationsCommunityManager.class);
	
	//---------------------------------------------------------------------------------------------------------
	// END POINTS de la API de Notificaciones
	//---------------------------------------------------------------------------------------------------------
	private static final String APPLICATION_ENDPOINT  = "/WebPushNotification/application/%s";
	private static final String NOTIFICATION_ENDPOINT = "/WebPushNotification/notification/%s";
	
	//---------------------------------------------------------------------------------------------------------
	// Parámetros de configuración de Notificaciones
	//---------------------------------------------------------------------------------------------------------
	private final long GROUP_ID;
	private String masSiteId;
	private String wpnSenderId;
	private static final String MAS_DEFAULT_PK_CAMPAIGN = "#pk_campaign=MASwpn";
	
	public NotificationsCommunityManager(long groupId) throws SystemException
	{
		this.GROUP_ID = groupId;
		
		boolean masEnabled = GetterUtil.getBoolean(GroupConfigTools.getGroupConfigXMLField(GROUP_ID, "googletools", "/google/metricsmas/@enablemetrics"), false);
		masSiteId = GroupConfigTools.getGroupConfigXMLField(GROUP_ID, "googletools", "/google/metricsmas/@appid");
		boolean wpnEnableUse = GetterUtil.getBoolean(GroupConfigTools.getGroupConfigXMLField(GROUP_ID, "googletools", "/google/metricsmas/notifications/@enableuse"), false);
		wpnSenderId = GroupConfigTools.getGroupConfigXMLField(GROUP_ID, "googletools", "/google/metricsmas/notifications/@senderid");
		
		// Comprueba que la configuración en el sitio sea correcta
		if ( !(masEnabled && Validator.isNotNull(masSiteId) && wpnEnableUse && Validator.isNotNull(wpnSenderId)))
		{	
			throw new SystemException(IterErrorKeys.XYZ_E_SOCIAL_NETWORK_NOT_CONFIGURED_ZYX);
		}
	}
	
	@Override
	public void authorize(HttpServletResponse response)
	{
		try
		{
			URL wpnServerUrl = new URL(Http.HTTP, new URL(PropsValues.ITER_WEB_PUSH_NOTIFICATIONS_APP_SERVER_URL).getAuthority(), String.format(APPLICATION_ENDPOINT, masSiteId));
			
			// Comprueba que exista la aplicación en el servidor de notificaciones
			IterHttpClient ihc = new IterHttpClient.Builder(IterHttpClient.Method.GET, wpnServerUrl.toString()).build();
			ihc.connect();
			if (HttpStatus.SC_OK == ihc.getResponseStatus())
			{
				// Crea la página de respuesta.
				CommunityAuthorizerUtil.buildResponsePage(response, masSiteId, com.liferay.portal.util.Obfuscator.generateKey(masSiteId));
			}
			else
			{
				// Crea la página de error.
				CommunityAuthorizerUtil.buildResponseErrorPage(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, IterErrorKeys.XYZ_E_SOCIAL_NETWORK_NOT_CONFIGURED_ZYX, "Notification app does not exist");
			}
		}
		catch (MalformedURLException e)
		{
			CommunityAuthorizerUtil.buildResponseErrorPage(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, IterErrorKeys.XYZ_E_COMMUNITY_AUTHORIZATION_FAILED_ZYX, "Malformed notification server URL");
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
	}
	
	@Override
	public void publish(String articleId, HashMap<String, String> params)
	{
		_log.debug("Sending notification");

		// Recupera los datos de la cuenta
		String accountId = params.get("accountId");
		String accountName = params.get("accountName");
		
		try
		{
			// Recupera las credenciales (token y nombre de usuario)
			String[] credentials = params.get("credentials").split(StringPool.COMMA);
			if (credentials.length != 2 && credentials.length != 4)
				throw new SystemException("XYZ_WRONG_CREDENTIALS_ZYX");
			
			String token = credentials[0];
			String username = credentials[1];
			
			if (token.trim().length() == 0 || username.trim().length() == 0)
				throw new SystemException("XYZ_WRONG_CREDENTIALS_ZYX");
		
			// Crea la URL para enviar la notificación
			URL wpnServerUrl = new URL(PropsValues.ITER_WEB_PUSH_NOTIFICATIONS_APP_SERVER_URL);
			URL wpnServerEndpoint = new URL(Http.HTTP, wpnServerUrl.getHost(), wpnServerUrl.getPort(), String.format(NOTIFICATION_ENDPOINT, token));
			
			// Forma la url del artículo
			String articleUrl = (
				credentials.length == 2 ?
					CommunityAuthorizerUtil.getArticle(GROUP_ID, articleId) :
					CommunityAuthorizerUtil.getArticle(GROUP_ID, credentials[3], articleId, null) + StringPool.SLASH + credentials[2]
				)
			+ MAS_DEFAULT_PK_CAMPAIGN;
			
			// Crea el payload
			JsonObject data = new JsonObject();
			data.addProperty("click_action", articleUrl);
			String title = params.get("title");
			if (Validator.isNotNull(title))
				data.addProperty("title", title);
			JsonObject payload = new JsonObject();
			payload.addProperty("username", username);
			payload.add("data", data);
			
			// Manda la notificación
			IterHttpClient ihc = new IterHttpClient
					.Builder(IterHttpClient.Method.POST, wpnServerEndpoint.toString())
					.header("Content-Type", "application/json; charset=utf-8")
					.payLoad(payload.toString())
					.build();
			String response = ihc.connect();
			
			if (HttpStatus.SC_OK == ihc.getResponseStatus())
			{
				// Si se envió la notificación, registra el envío en el Monitor
				IterMonitor.logEvent(GROUP_ID, Event.INFO, new Date(), "Notification Publisher: Notification sent", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, response));
				
				// Realiza una anotación en las estadísticas del artículo
				String annotationLiteral = GetterUtil.getString(GroupConfigTools.getGroupConfigXMLField(GROUP_ID, "visitstatistics", "/visits/annotations/publication/text()"), "Artículo publicado en");
				VisitsStatisticsLocalServiceUtil.addArticleStatisticsAnnotation(GROUP_ID, articleId, annotationLiteral + StringPool.SPACE + " WPN");
				
				_log.debug("Notification sent");
			}
			else
			{
				IterMonitor.logEvent(GROUP_ID, Event.ERROR, new Date(), "Notification Publisher Error: API responded with an error", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, response));
				_log.error("Error sending notification");
			}
		}
		catch (MalformedURLException e)
		{
			IterMonitor.logEvent(GROUP_ID, Event.ERROR, new Date(), "Notification Publisher Error: Application server URL is not valid", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, e));
			_log.error(e);
		}
		catch (SystemException e)
		{
			IterMonitor.logEvent(GROUP_ID, Event.ERROR, new Date(), "Notification Publisher Error", articleId, CommunityAuthorizerUtil.getMonitorTrace(accountId, accountName, e));
			_log.error(e);
		}
	}
}
