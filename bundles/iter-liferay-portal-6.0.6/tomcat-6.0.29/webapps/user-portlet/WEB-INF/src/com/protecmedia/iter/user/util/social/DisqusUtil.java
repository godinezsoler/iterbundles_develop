package com.protecmedia.iter.user.util.social;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

import com.liferay.portal.apache.ApacheUtil;
import com.liferay.portal.kernel.comments.CommentsConfigBean;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.user.util.UserUtil;

public class DisqusUtil
{
	private static Log _log = LogFactoryUtil.getLog(DisqusUtil.class);

	private static String AUTHORIZE_ENDPOINT = "https://disqus.com/api/oauth/2.0/authorize/?" + 
													"client_id=%s&" + 
													"response_type=code&" + 
													"%s" + 
													"redirect_uri=%s";
	
	private static String ACCESS_TOKEN_ENDPOINT = "https://disqus.com/api/oauth/2.0/access_token/";
	
	private static String DETAILS_ENDPOINT = "https://disqus.com/api/3.0/users/details.json?" + 
											 		"access_token=%s&" + 
											 		"api_key=%s&" + 
											 		"api_secret=%s";

	public static String SERVLET_PATH = "/user-portlet/login-with/disqus";
	
	private static final Map<String, String> scopes;

	private static final String URL_API_DISQUS = ".disqus.com/count-data.js?";
	private static String INIT_RESPONSE_API = "DISQUSWIDGETS.displayCount(";
	
	
	private static final String QUOTAS 	= GetterUtil.getString(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_DISQUS_QUOTA), "1000/3600000");
	private static SocialStatisticsQuotaController socialQuotaController = new SocialStatisticsQuotaController(QUOTAS);
	
	private static int MAX_API_ATTEMPTS = 3;
	
    static
    {
    	scopes = new HashMap<String, String>();
    	scopes.put("default", "read,email");
    }
	
	private static String getPublicKey(long groupId) throws Exception
	{
		String publicKey = null;
		CommentsConfigBean commentsConfig = new CommentsConfigBean(groupId, null);
		if(commentsConfig.useDisqusConfig())
			publicKey = commentsConfig.getPublicKey();
		
		return publicKey;
	}
	
	private static String getPrivateKey(long groupId) throws Exception
	{
		String privateKey = null;
		CommentsConfigBean commentsConfig = new CommentsConfigBean(groupId, null);
		if(commentsConfig.useDisqusConfig())
			privateKey = commentsConfig.getSecretKey();
		
		return privateKey;
	}

	public static String getCodeURL(HttpServletRequest request, String scope, long groupId) throws Exception
	{
		if(Validator.isNull(scope))
			scope = SocialNetworkUtil.getConfigScope(SocialNetworkUtil.SOCIAL_NAME_DISQUS, groupId, scopes);
			
		return String.format(AUTHORIZE_ENDPOINT, getPublicKey(groupId), SocialNetworkUtil.getScope(scope), UserUtil.getServletURL(request, SERVLET_PATH));
	}

	public static JSONObject getAccessToken(HttpServletRequest request, String code, long groupId) throws JSONException, IOException
	{
		JSONObject accessData = null;
		HttpURLConnection httpConnection = null;
		
		try
		{
			String redirectUri = UserUtil.getServletURL(request, SERVLET_PATH);
			String accessTokenEndpoint = String.format(ACCESS_TOKEN_ENDPOINT, getPublicKey(groupId), getPrivateKey(groupId), redirectUri, code);

			String urlParameters = "grant_type=" 		+ URLEncoder.encode("authorization_code", "UTF-8") 		+ StringPool.AMPERSAND +
			  					   "client_id=" 		+ URLEncoder.encode(getPublicKey(groupId), "UTF-8") 	+ StringPool.AMPERSAND +
			  					   "client_secret=" 	+ URLEncoder.encode(getPrivateKey(groupId), "UTF-8")	+ StringPool.AMPERSAND +
			  					   "code="				+ URLEncoder.encode(code, "UTF-8") 						+ StringPool.AMPERSAND +
								   "redirect_uri=" 		+ URLEncoder.encode(redirectUri, "UTF-8"); 				
			
			httpConnection = (HttpURLConnection)(new URL(accessTokenEndpoint).openConnection());
			httpConnection.setRequestMethod("POST");
			httpConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");		
			httpConnection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));	
			httpConnection.setUseCaches(false);
			httpConnection.setDoInput(true);
			httpConnection.setDoOutput(true);

			DataOutputStream stream = new DataOutputStream (httpConnection.getOutputStream());
			stream.writeBytes(urlParameters);
			stream.flush();
			stream.close();

			HttpUtil.throwIfConnectionFailed( httpConnection, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX );
			accessData = JSONFactoryUtil.createJSONObject( StreamUtil.toString(httpConnection.getInputStream(), StringPool.UTF8) );
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		finally
		{
			if (httpConnection != null)
				httpConnection.disconnect();
		}
		
		return accessData;
	}
	
	public static JSONObject getDetailsByAccessToken(String accessToken, long groupId) throws JSONException, IOException
	{
		JSONObject details = null;
		
		if(	Validator.isNotNull(accessToken))
		{
			HttpURLConnection httpConnection = null;
			
			try
			{
				StringBuffer authorizeEndpoint = new StringBuffer(String.format(DETAILS_ENDPOINT, accessToken, getPublicKey(groupId), getPrivateKey(groupId)));
				
				httpConnection = (HttpURLConnection)(new URL(authorizeEndpoint.toString()).openConnection());
				httpConnection.setConnectTimeout(ApacheUtil.getApacheConnTimeout());
				httpConnection.setReadTimeout(ApacheUtil.getApacheReadTimeout());
		        httpConnection.connect();
	
		        HttpUtil.throwIfConnectionFailed( httpConnection, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX );
		        details = JSONFactoryUtil.createJSONObject( StreamUtil.toString(httpConnection.getInputStream(), StringPool.UTF8) );
			}
			catch(Exception e)
			{
				_log.error(e.toString());
				_log.trace(e);
			}
			finally
			{
				if (httpConnection != null)
					httpConnection.disconnect();
			}
		}
		
		return details;
	}
	
	public static JSONObject getDetailsByCode(HttpServletRequest request, String code, long groupId) throws JSONException, IOException
	{
		JSONObject details = null;
		
		JSONObject accessToken = getAccessToken(request, code, groupId);
		if(accessToken != null)
			details = getDetailsByAccessToken(accessToken.getString(SocialNetworkUtil.ACCESS_TOKEN), groupId);
		
		return details;
	}
	
	public static String getSocialUserId(JSONObject user)
	{
		return user.getJSONObject("response").getString("id");
	}
	
	public static String XYZ_FIELD_AVATARURL_DEFAULT_ZYX(JSONObject user)
	{
		return user.getJSONObject("response").getJSONObject("avatar").getString("permalink");
	}
	
	public static String XYZ_FIELD_AVATARURL_LARGE_ZYX(JSONObject user)
	{
		return user.getJSONObject("response").getJSONObject("avatar").getJSONObject("small").getString("cache");
	}

	public static String XYZ_FIELD_AVATARURL_SMALL_ZYX(JSONObject user)
	{
		return user.getJSONObject("response").getJSONObject("avatar").getJSONObject("large").getString("cache");
	}
	
	public static String XYZ_FIELD_EMAIL_ZYX(JSONObject user)
	{
		return user.getJSONObject("response").getString("email");
	}
	
	public static String XYZ_FIELD_LOCATION_ZYX(JSONObject user)
	{
		return user.getJSONObject("response").getString("location");
	}
	
	public static String XYZ_FIELD_NAME_ZYX(JSONObject user)
	{
		return user.getJSONObject("response").getString("name");
	}
	
	public static String XYZ_FIELD_FIRSTNAME_ZYX(JSONObject user)
	{
		return XYZ_FIELD_NAME_ZYX(user);
	}
	
	public static String XYZ_FIELD_PROFILEURL_ZYX(JSONObject user)
	{
		return user.getJSONObject("response").getString("profileUrl");
	}
	
	public static String XYZ_FIELD_USERNAME_ZYX(JSONObject user)
	{
		return user.getJSONObject("response").getString("username");
	}
	
	public static String XYZ_FIELD_WEB_ZYX(JSONObject user)
	{
		return user.getJSONObject("response").getString("url");
	}
	
	public static String getSocialNetworkId(JSONObject user, int pos)
	{
		String value = null;
		try
		{
			JSONObject connections = user.getJSONObject("response").getJSONObject("connections");
			JSONObject jsonOvejota = null;
			switch (pos)
			{
				case SocialNetworkUtil.SOCIAL_NAME_FACEBOOK_POS:
					jsonOvejota = connections.getJSONObject("facebook");
					if(jsonOvejota!=null)
						value = jsonOvejota.getString("id");
					break;
				case SocialNetworkUtil.SOCIAL_NAME_GOOGLEPLUS_POS:
					jsonOvejota = connections.getJSONObject("google");
					if(jsonOvejota!=null)
						value = jsonOvejota.getString("id");
					break;
				case SocialNetworkUtil.SOCIAL_NAME_TWITTER_POS:
					jsonOvejota = connections.getJSONObject("twitter");
					if(jsonOvejota!=null)
						value = jsonOvejota.getString("id");
					break;
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return value;
	}

	public static JSONObject getStatistics(String shortName, String urls)
	{
		JSONObject result = null;
		String disqusURL = new StringBuffer(Http.HTTPS_WITH_SLASH).append(shortName).append(URL_API_DISQUS).append(urls).toString();

		HttpClient client = null;
		HttpResponse httpResponse = null;
		int responseStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR;
		String responseBody = "";
		Long max_age = 0L;
		Long quotaTimeSleep = 0L;
		
		int quota = -1;

		_log.debug("[getStatistics()] Checking Disqus quota...");
		
		synchronized (socialQuotaController)
		{
			quota = socialQuotaController.getIndexBlockedQuota();
		}

		if(quota < 0)
		{
			try
			{
				client = new DefaultHttpClient();
				// Indicamos el protocolo a usar
				client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		
				HttpGet get = new HttpGet(disqusURL);
				
				// Para no llenar el log de catalina en caso de caida del servicio de Facebook, intentamos conectar, si no lo logramos se prueba más tarde
				boolean conected = false;
				int attempt = 0;
				while (!conected && attempt <= MAX_API_ATTEMPTS)
				{
					try
					{
						_log.debug("[getStatistics()] Retrieving Disqus statistics(" + urls + ") attempt " + attempt + "...");
						httpResponse = client.execute(get);
						conected = true;
					}
					catch(Exception e)
					{
						attempt++;
						_log.info(new StringBuilder("Impossible to connect with Disqus (attempt ")
					       .append(attempt).append(", thread: '").append(Thread.currentThread().getName()).append("'). ")
					       .append("Trying to conect with it in ")
					       .append(IterKeys.NEXT_TRY_TO_CONECT_TO_SOCIAL_NETWORK).append("m"));
						Thread.sleep( (IterKeys.NEXT_TRY_TO_CONECT_TO_SOCIAL_NETWORK * 60 * 1000) );							
					}	
				}				
			
				if(httpResponse != null)
				{
					// Codigo de respuesta
					responseStatus = httpResponse.getStatusLine().getStatusCode();
					//obtenemos el json que contiene el jsonp
					String httpResponseBody = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
					int initIndex = httpResponseBody.indexOf(INIT_RESPONSE_API)+INIT_RESPONSE_API.length();
					if(initIndex >= INIT_RESPONSE_API.length())
					{
						int endIndex = httpResponseBody.length()-4;
						JSONObject obj = JSONFactoryUtil.createJSONObject(httpResponseBody.substring(initIndex, endIndex));
						JSONArray counts = obj.getJSONArray("counts");
						responseBody = (counts.length() == 0) ?  "" : counts.toString();
					}
				}
			}
			catch(Exception e)
			{
				_log.error("Exception in request: "+ disqusURL);
				_log.error(e.toString());
				_log.error(e);
			}
			finally
			{
				if(Validator.isNotNull(client))
				{
					// Cerramos la conexion
					client.getConnectionManager().shutdown();
				}
			}
			if(Validator.equals(HttpStatus.SC_OK,  responseStatus))
			{
				_log.debug("disqus request: " + disqusURL);
				_log.debug("disqus received response code: " + responseStatus);
				_log.debug("disqus received response: " + responseBody);
			}
			else
			{
				_log.error("disqus request: " + disqusURL);
				_log.error("disqus received response code: " + responseStatus);
				_log.error("disqus received response: " + responseBody);
			}
			
			if(Validator.isNotNull(httpResponse) && Validator.equals(HttpStatus.SC_OK, responseStatus) && Validator.isNotNull(responseBody))
			{
				max_age = getMaxAge(httpResponse);
			}
		}
		else
		{
			_log.debug("disqus limit quota: " + quota);
			quotaTimeSleep = socialQuotaController.getTimeToSleep(quota)/1000;
		}
			
		result = JSONFactoryUtil.createJSONObject();
		result.put("max-age", max_age);
		result.put("body", responseBody);
		result.put("quota-time-sleep", quotaTimeSleep);
		return result;
	}
	
	private static Long getMaxAge(HttpResponse httpResponse){
		Long max_age = 0L;
		Header[] headers = httpResponse.getHeaders("Cache-Control");
		for (Header header : headers)
		{
			if(header.getValue().contains("max-age")){
				int indexof_maxage_value = header.getValue().indexOf("max-age=")+8;
				max_age = Long.valueOf(header.getValue().substring(indexof_maxage_value));
			}
		}
		return max_age;
	}
	
}