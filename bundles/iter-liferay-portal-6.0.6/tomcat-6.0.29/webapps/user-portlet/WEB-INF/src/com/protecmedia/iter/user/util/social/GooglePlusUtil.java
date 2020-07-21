package com.protecmedia.iter.user.util.social;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

import com.liferay.portal.apache.ApacheUtil;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.user.util.UserUtil;

public class GooglePlusUtil
{
	private static Log _log = LogFactoryUtil.getLog(GooglePlusUtil.class);

	private static String AUTHORIZE_ENDPOINT    = "https://accounts.google.com/o/oauth2/auth?" + 
													"client_id=%s&" + 
													"response_type=code&" + 
													"%s" + 
													"redirect_uri=%s";
	
	private static String ACCESS_TOKEN_ENDPOINT = "https://accounts.google.com/o/oauth2/token";
	
	private static String DETAILS_ENDPOINT      = "https://www.googleapis.com/oauth2/v1/userinfo?access_token=%s";
	
	public static String SERVLET_PATH           = "/user-portlet/login-with/googleplus";

	private static final Map<String, String> scopes;

	// Url de la petición
	private static final String URL_API_GOOGLEPLUS = "https://clients6.google.com/rpc?key=AIzaSyCKSbrvQasunBoV16zDH9R33D88CeLr9gQ";

	// Parte inicial del json para pedir estadísticas
	private static final String POSTBODY_PART1 = "[{'method':'pos.plusones.get','id':'p','params':{'nolog':true,'id':'";
	// Parte final del json para pedir estadísticas
	private static final String POSTBODY_PART2 = "','source':'widget','userId':'@viewer','groupId':'@self'},'jsonrpc':'2.0','key':'p','apiVersion':'v1'}]";
	
    static
    {
    	scopes = new HashMap<String, String>();
    	scopes.put("default", "https://www.googleapis.com/auth/userinfo.profile+https://www.googleapis.com/auth/userinfo.email");
    }
	
	private static final String QUOTAS = GetterUtil.getString(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_GOOGLEPLUS_QUOTA), "10000/86400000");
	
	private static SocialStatisticsQuotaController socialQuotaController = new SocialStatisticsQuotaController(QUOTAS);

	private static int MAX_API_ATTEMPTS = 3;
	
	private static String getPublicKey(long groupId) throws SecurityException, NoSuchMethodException
	{
		return SocialNetworkUtil.getConfigPublicKey(SocialNetworkUtil.SOCIAL_NAME_GOOGLEPLUS, groupId);
	}
	
	private static String getPrivateKey(long groupId) throws SecurityException, NoSuchMethodException
	{
		return SocialNetworkUtil.getConfigSecretKey(SocialNetworkUtil.SOCIAL_NAME_GOOGLEPLUS, groupId);
	}
	
	public static String getCodeURL(HttpServletRequest request, String scope, long groupId) throws PortalException, SystemException, MalformedURLException, IOException, SecurityException, NoSuchMethodException
	{
		if(Validator.isNull(scope))
			scope = SocialNetworkUtil.getConfigScope(SocialNetworkUtil.SOCIAL_NAME_GOOGLEPLUS, groupId, scopes);
		
		return String.format(AUTHORIZE_ENDPOINT, getPublicKey(groupId), SocialNetworkUtil.getScope(scope),  UserUtil.getServletURL(request, SERVLET_PATH));
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
			httpConnection.setUseCaches (false);
			httpConnection.setDoInput(true);
			httpConnection.setDoOutput(true);

			DataOutputStream stream = new DataOutputStream (httpConnection.getOutputStream());
			stream.writeBytes (urlParameters);
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
			StringBuffer authorizeEndpoint = new StringBuffer(String.format(DETAILS_ENDPOINT, accessToken));

			HttpURLConnection httpConnection = (HttpURLConnection)(new URL(authorizeEndpoint.toString()).openConnection());
			
			try
			{
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
	
	public static String XYZ_FIELD_USERNAME_ZYX(JSONObject user)
	{
		String userName = null;
		try
		{
			String email = XYZ_FIELD_EMAIL_ZYX(user);
			userName = email.substring(0, email.indexOf(StringPool.AT));
		}
		catch(Exception e)
		{
			_log.debug(e);
		}
		
		return userName;
	}
	
	public static String XYZ_FIELD_AVATARURL_DEFAULT_ZYX(JSONObject user)
	{
		return user.getString("picture");
	}
	
	public static String XYZ_FIELD_EMAIL_ZYX(JSONObject user)
	{
		return user.getString("email");
	}
	
	public static String XYZ_FIELD_FIRSTNAME_ZYX(JSONObject user)
	{
		return user.getString("given_name");
	}
	
	public static String XYZ_FIELD_FULL_NAME_ZYX(JSONObject user)
	{
		return user.getString("name");
	}
	
	public static String XYZ_FIELD_GENDER_ZYX(JSONObject user)
	{
		return user.getString("gender");
	}
	
	public static String XYZ_FIELD_LASTNAME_ZYX(JSONObject user)
	{
		return user.getString("family_name");
	}
	
	public static String XYZ_FIELD_LOCALE_ZYX(JSONObject user)
	{
		return user.getString("locale");
	}
	
	public static String XYZ_FIELD_PROFILEURL_ZYX(JSONObject user)
	{
		return user.getString("link");
	}

	public static JSONObject getStatistic(String url)
	{
		JSONObject result = null;
		String googlePluseURL = new StringBuilder(POSTBODY_PART1).append(url).append(POSTBODY_PART2).toString();
		
		if (_log.isDebugEnabled())
			_log.debug(new StringBuilder("Request to google plus: '").append(googlePluseURL).append("'"));
		
		HttpClient client = null;
		HttpResponse httpResponse = null;
		int responseStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR;
		String responseBody = null;
		Long max_age = 0L;
		Long quotaTimeSleep = 0L;
		
		int quota = -1;
		synchronized (socialQuotaController)
		{
			quota = socialQuotaController.getIndexBlockedQuota();
		}
		
		if(quota < 0)
		{
			try
			{
				client = new DefaultHttpClient();
				client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
				HttpPost post = new HttpPost(URL_API_GOOGLEPLUS);
				post.addHeader(HttpHeaders.CONTENT_TYPE, "application/json-rpc");
				
				HttpEntity entity = new StringEntity(googlePluseURL);
				post.setEntity(entity);				
				
				// Para no llenar el log de catalina en caso de caida del servicio de Google+, intentamos conectar, si no lo logramos se prueba más tarde
				boolean conected = false;
				int attempt = 0;
				while (!conected && attempt <= MAX_API_ATTEMPTS)
				{
					try
					{
						httpResponse = client.execute(post);
						conected = true;
					}
					catch(Exception e)
					{
						attempt++;
						_log.info(new StringBuilder("Impossible to connect with Google+ (attempt ")
					       .append(attempt).append(", thread: '").append(Thread.currentThread().getName()).append("'). ")
					       .append("Trying to conect with it in ")
					       .append(IterKeys.NEXT_TRY_TO_CONECT_TO_SOCIAL_NETWORK).append("m"));
						Thread.sleep( (IterKeys.NEXT_TRY_TO_CONECT_TO_SOCIAL_NETWORK * 60 * 1000) );							
					}	
				}								
				
				if(httpResponse != null)
				{
					responseStatus = httpResponse.getStatusLine().getStatusCode();
					responseBody = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
				}
			}
			catch(Exception e)
			{
				_log.error(e);
				//marcamos todas las urls como negativas para poder identificarlas posteriormente
			}
			finally
			{
				if(Validator.isNotNull(client))
				{
					// Cerramos la conexion
					client.getConnectionManager().shutdown();
				}
			}
			
			if (_log.isDebugEnabled())
			{
				_log.debug(new StringBuffer("google plus received response code: ").append(responseStatus));
				_log.debug("google plus received response: " + (null == responseBody ? "null" : responseBody));
			}
			if(Validator.isNotNull(httpResponse) && Validator.equals(HttpStatus.SC_OK,  responseStatus) && Validator.isNotNull(responseBody))
			{
				max_age = getMaxAge(httpResponse);
			}
		}
		else
		{
			_log.debug("google plus limit quota: " + quota);
			quotaTimeSleep = socialQuotaController.getTimeToSleep(quota)/1000;
		}
		
		JSONArray body = null;
		JSONArray responseArray = null;
		
		if(responseBody != null && !responseBody.isEmpty())
		{
			try
			{
				responseArray = JSONFactoryUtil.createJSONArray(responseBody);
			}
			catch(JSONException e)
			{
				_log.error("Error creating a json with the Google+ response");
				_log.error(e);
			}
			
			if(responseArray != null && responseArray.getJSONObject(0) != null && responseArray.getJSONObject(0).getJSONObject("result") != null)
			{
				body = JSONFactoryUtil.createJSONArray();
				body.put(responseArray.getJSONObject(0));
			}
		}
		
		result = JSONFactoryUtil.createJSONObject();
		result.put("max-age", max_age);
		result.put("body", (body == null) ? "" : body.toString());
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
				String max_age_str = header.getValue().substring(indexof_maxage_value);
				if(max_age_str.contains(","))
					max_age_str =  max_age_str.substring(0, max_age_str.indexOf(","));
				if(max_age < Long.valueOf(max_age_str))
				{
					max_age = Long.valueOf(header.getValue().substring(indexof_maxage_value));
				}
			}
		}
		return max_age;
	}
}