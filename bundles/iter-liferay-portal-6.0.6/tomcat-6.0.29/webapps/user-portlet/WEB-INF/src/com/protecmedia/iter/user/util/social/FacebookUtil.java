package com.protecmedia.iter.user.util.social;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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
import com.liferay.portal.kernel.Social.FacebookConstants;
import com.liferay.portal.kernel.Social.FacebookTools;
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
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.user.util.UserUtil;

public class FacebookUtil
{
	private static Log _log = LogFactoryUtil.getLog(FacebookUtil.class);

	private static String AUTHORIZE_ENDPOINT    = "https://www.facebook.com/dialog/oauth?" 	+ 
													"client_id=%s&" 						+ 
													"%s" 									+ 
													"redirect_uri=%s";
	
	private static String ACCESS_TOKEN_ENDPOINT = FacebookConstants.API_GRAPH +"/oauth/access_token?" 	+ 
														"client_id=%s&" 								+ 
														"client_secret=%s&" 							+ 
														"code=%s&" 										+
														"redirect_uri=%s";
	
	private static String DETAILS_ENDPOINT      = FacebookConstants.API_GRAPH.concat("/me?%s");
	
	public static String SERVLET_PATH           = "/user-portlet/login-with/facebook";

    private static final String DEFAULT_SCOPE			= "email,user_gender,user_hometown,user_location,user_link";
    private static final String BIRTHDAY_ALL_SCOPE		= "email,user_gender,user_hometown,user_location,user_link,user_birthday";
    private static final String WEB_ALL_SCOPE			= "email,user_gender,user_hometown,user_location,user_link,user_website";
    private static final String WEB_BIRTHDAY_ALL_SCOPE	= "email,user_gender,user_hometown,user_location,user_link,user_website,user_birthday";
    
	private static final Map<String, String> scopes;
    static
    {
    	scopes = new HashMap<String, String>();
    	scopes.put("default", 			DEFAULT_SCOPE);
    	scopes.put("birthday_all", 		BIRTHDAY_ALL_SCOPE);
    	scopes.put("web_all", 			WEB_ALL_SCOPE);
    	scopes.put("web_birthday_all", 	WEB_BIRTHDAY_ALL_SCOPE);
    }

    
    private static final String DEFAULT_SCOPE_FIELDS 			= "id,email,first_name,gender,last_name,link,locale,middle_name,name,timezone,updated_time,verified,picture,location,hometown,work";
    private static final String BIRTHDAY_ALL_SCOPE_FIELDS 		= DEFAULT_SCOPE_FIELDS.concat(StringPool.COMMA).concat("birthday");
    private static final String WEB_ALL_SCOPE_FIELDS 			= DEFAULT_SCOPE_FIELDS.concat(StringPool.COMMA).concat("website");;
    private static final String WEB_BIRTHDAY_ALL_SCOPE_FIELDS 	= WEB_ALL_SCOPE_FIELDS.concat(StringPool.COMMA).concat("birthday");;
    
    private static String FIELDS_EXTRA = "&fields=%s";
    
    private static final String URL_API_FB = "https://api.facebook.com/method/links.getStats?format=json&urls=";
   
	private static final String QUOTAS 	= GetterUtil.getString(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_FACEBOOK_QUOTA), "");
	
	private static SocialStatisticsQuotaController socialQuotaController = new SocialStatisticsQuotaController(QUOTAS);

	private static int MAX_API_ATTEMPTS = 3;
	
	private static String getPublicKey(long groupId) throws SecurityException, NoSuchMethodException
	{
		return SocialNetworkUtil.getConfigPublicKey(SocialNetworkUtil.SOCIAL_NAME_FACEBOOK, groupId);
	}
	
	private static String getPrivateKey(long groupId) throws SecurityException, NoSuchMethodException
	{
		return SocialNetworkUtil.getConfigSecretKey(SocialNetworkUtil.SOCIAL_NAME_FACEBOOK, groupId);
	}
	
	public static String getCodeURL(HttpServletRequest request, String scope, long groupId) throws SecurityException, NoSuchMethodException, MalformedURLException, PortalException, SystemException, UnsupportedEncodingException
	{
		if(Validator.isNull(scope))
			scope = getScope(groupId);
		
		String url = String.format(AUTHORIZE_ENDPOINT, getPublicKey(groupId), SocialNetworkUtil.getScope(scope), getServletURL(request));
		_log.debug(url);
		
		return url;
	}
	
	/**
	 * @see http://jira.protecmedia.com:8080/browse/ITER-859<br/>
	 * 		ITER-859 Error Login With: Insecure Login Blocked: You can't get an access token or log in to this app from an insecure page.
	 * @param request
	 * @return
	 * @throws MalformedURLException
	 * @throws PortalException
	 * @throws SystemException
	 * @throws UnsupportedEncodingException
	 */
	private static String getServletURL(HttpServletRequest request) throws MalformedURLException, PortalException, SystemException, UnsupportedEncodingException
	{
		URL servletURL = new URL( UserUtil.getServletURL(request, SERVLET_PATH) );
		return new URL(Http.HTTPS, servletURL.getHost(), servletURL.getFile()).toString();
	}

	public static String getScope(long groupId) throws SecurityException, NoSuchMethodException
	{
		return SocialNetworkUtil.getConfigScope(SocialNetworkUtil.SOCIAL_NAME_FACEBOOK, groupId, scopes);
	}

	public static String getAccessToken(HttpServletRequest request, String code, long groupId) throws JSONException, IOException
	{
		StringBuffer accessData = new StringBuffer();
		HttpURLConnection httpConnection = null;
		
		try
		{
			String privateKey = getPrivateKey(groupId);
			String accessTokenEndpoint = String.format(ACCESS_TOKEN_ENDPOINT, getPublicKey(groupId), privateKey, code, getServletURL(request));
			_log.debug(accessTokenEndpoint);
			
			httpConnection = (HttpURLConnection)(new URL(accessTokenEndpoint.toString()).openConnection());
			httpConnection.setConnectTimeout(ApacheUtil.getApacheConnTimeout());
			httpConnection.setReadTimeout(ApacheUtil.getApacheReadTimeout());
	        httpConnection.connect();
	        
			HttpUtil.throwIfConnectionFailed( httpConnection, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX );

			JSONObject jsonApiResponse = JSONFactoryUtil.createJSONObject( StreamUtil.toString(httpConnection.getInputStream(), StringPool.UTF8) );
			String access_token 	= jsonApiResponse.getString("access_token");
			String appSecretProof 	= FacebookTools.get_appsecret_proof(access_token, privateKey);
			
			accessData.append("access_token=").append(access_token).append("&").append(FacebookConstants.PARAM_APPSECRET_PROOF).append("=").append(appSecretProof);
		}
		catch(Exception e)
		{
			_log.error(e);
		}
		finally
		{
			if (httpConnection != null)
				httpConnection.disconnect();
		}
		
		return accessData.toString();
	}
	
	private static JSONObject getJSONByURL(String url) throws JSONException, IOException
	{
		JSONObject details = null;
		
		HttpURLConnection httpConnection = (HttpURLConnection)(new URL(url).openConnection());
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
			_log.error(e);
		}
		finally
		{
			if (httpConnection != null)
				httpConnection.disconnect();
		}
		
		return details;
	}
	
	public static JSONObject getDetailsByAccessToken(String accessToken, long groupId) throws JSONException, IOException, SecurityException, NoSuchMethodException
	{
		JSONObject details = null;
		
		if(	Validator.isNotNull(accessToken))
		{
			StringBuffer authorizeEndpoint = new StringBuffer(String.format(DETAILS_ENDPOINT, accessToken));
			
			String scope = getScope(groupId);
			
			if(Validator.isNull(scope) || scope.equalsIgnoreCase(DEFAULT_SCOPE))
				authorizeEndpoint.append( String.format(FIELDS_EXTRA, DEFAULT_SCOPE_FIELDS) );
			else 
				if(scope.equalsIgnoreCase(BIRTHDAY_ALL_SCOPE))
					authorizeEndpoint.append(String.format(FIELDS_EXTRA, BIRTHDAY_ALL_SCOPE_FIELDS) );
			else 
				if(scope.equalsIgnoreCase(WEB_ALL_SCOPE))
					authorizeEndpoint.append(String.format(FIELDS_EXTRA, WEB_ALL_SCOPE_FIELDS) );
			else 
				if(scope.equalsIgnoreCase(WEB_BIRTHDAY_ALL_SCOPE))
					authorizeEndpoint.append(String.format(FIELDS_EXTRA, WEB_BIRTHDAY_ALL_SCOPE_FIELDS) );
			
			details = getJSONByURL(authorizeEndpoint.toString());
		}
		
		return details;
	}
	
	public static JSONObject getDetailsByCode(HttpServletRequest request, String code, long groupId) throws JSONException, IOException, SecurityException, NoSuchMethodException
	{
		JSONObject details = null;
		
		String accessTokenParam = getAccessToken(request, code, groupId);
		if(Validator.isNotNull(accessTokenParam))
			details = getDetailsByAccessToken(accessTokenParam, groupId);
		
		return details;
	}

	public static String XYZ_FIELD_AVATARURL_DEFAULT_ZYX(JSONObject user)
	{
		return user.getJSONObject("picture").getJSONObject("data").getString("url");
	}
	
	public static String XYZ_FIELD_USERNAME_ZYX(JSONObject user)
	{
		return user.getString("first_name");
	}

	public static String XYZ_FIELD_NAME_ZYX(JSONObject user)
	{
		return user.getString("name");
	}
	
	public static String XYZ_FIELD_EMAIL_ZYX(JSONObject user)
	{
		return user.getString("email");
	}
	
	public static String XYZ_FIELD_FIRSTNAME_ZYX(JSONObject user)
	{
		String resultName = null;
		String firstName = user.getString("first_name");
		String middleName = user.getString("middle_name");
		if(Validator.isNotNull(firstName) && Validator.isNotNull(middleName))
		{
			resultName = new StringBuilder().append(firstName).append(StringPool.SPACE).append(middleName).toString();
		}
		else if(Validator.isNotNull(firstName))
		{
			resultName = new StringBuilder().append(firstName).toString();
		}
		
		return resultName;
	}
	
	public static String XYZ_FIELD_LASTNAME_ZYX(JSONObject user)
	{
		return user.getString("last_name");
	}
	
	public static String XYZ_FIELD_BIRTHDAY_ZYX(JSONObject user)
	{
		return user.getString("birthday");
	}
	
	public static String XYZ_FIELD_EMPLOYER_ZYX(JSONObject user)
	{
		String work = null;
		
		JSONArray works = user.getJSONArray("work");
		if(works != null && works.length() > 0)
			work = works.getJSONObject(0).getJSONObject("employer").getString("name");

		return work;
	}

	
	public static String XYZ_FIELD_GENDER_ZYX(JSONObject user)
	{
		return user.getString("gender");
	}
	
	public static String XYZ_FIELD_HOMETOWN_ZYX(JSONObject user)
	{
		return user.getJSONObject("hometown").getString("name");
	}
	
	public static String XYZ_FIELD_LOCALE_ZYX(JSONObject user)
	{
		return user.getString("locale");
	}
	
	public static String XYZ_FIELD_LOCATION_ZYX(JSONObject user)
	{
		return user.getJSONObject("location").getString("name");
	}
	
	public static String XYZ_FIELD_PROFILEURL_ZYX(JSONObject user)
	{
		return user.getString("link");
	}
	
	public static String XYZ_FIELD_WEB_ZYX(JSONObject user)
	{
		return user.getString("web");
	}


	/**
	 * 
	 * @param urls
	 * @return
	 * {
	 * "max_age":value,
	 * "body":StringBodyQueRepresentaJSON
	 * }
	 * @throws ServiceError
	 */
	public static JSONObject getStatistics(String urls) throws ServiceError
	{
		if (_log.isDebugEnabled())
		_log.debug(new StringBuilder("In FacebookUtil.getStatistics with urls: '").append(urls).append("'"));
		
		JSONObject result = null;
		String facebookURL = new StringBuffer()
			.append(URL_API_FB)
			.append(urls)
			.toString();
		
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
				int api_intent = 3;
				do
				{
					if(client != null)
					{
						client.getConnectionManager().shutdown();
					}
					client = new DefaultHttpClient();
					// Indicamos el protocolo a usar
					client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
			
					HttpGet get = new HttpGet(facebookURL);
					
					// Para no llenar el log de catalina en caso de caida del servicio de Facebook, intentamos conectar, si no lo logramos se prueba más tarde
					boolean conected = false;
					int attempt = 0;
					while (!conected && attempt <= MAX_API_ATTEMPTS)
					{
						try
						{
							httpResponse = client.execute(get);
							conected = true;
						}catch(Exception e){
							attempt++;
							_log.info(new StringBuilder("Impossible to connect with Facebook (attempt ")
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
					api_intent--;
					//existen llamadas que al realizarse son correctas pero la api no devuelve contenido, por lo que lo mas probable es que al volver a realizarse se obtengan los datos requeridos.
				}
				while(httpResponse != null && HttpStatus.SC_OK == responseStatus && responseBody.isEmpty() && api_intent > 0);
			}
			catch(Throwable th)
			{
				_log.error(th);
			}
			finally
			{
				if(Validator.isNotNull(client))
				{
					// Cerramos la conexion
					client.getConnectionManager().shutdown();
				}
			}

			_log.debug(new StringBuffer("facebook received response code: ").append(responseStatus));
			_log.debug("facebook received response: " + (null == responseBody ? "null" : responseBody));
			
			if(Validator.isNotNull(httpResponse) && Validator.equals(HttpStatus.SC_OK,  responseStatus) && Validator.isNotNull(responseBody))
			{
				max_age = getMaxAge(httpResponse);
			}
		}
		else
		{
			_log.debug("facebook limit quota: " + quota);
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