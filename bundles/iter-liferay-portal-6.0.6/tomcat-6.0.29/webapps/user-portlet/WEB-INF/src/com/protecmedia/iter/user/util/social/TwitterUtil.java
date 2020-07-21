package com.protecmedia.iter.user.util.social;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
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
import com.protecmedia.iter.base.service.util.ServiceError;

public class TwitterUtil
{
	private static Log _log = LogFactoryUtil.getLog(TwitterUtil.class);

	private static String REQUEST_TOKEN_ENDPOINT 	= "https://api.twitter.com/oauth/request_token";
	
	private static String ACCESS_TOKEN_ENDPOINT 	= "https://api.twitter.com/oauth/access_token";
	
	private static String AUTHENTICATE_ENDPOINT 	= "https://api.twitter.com/oauth/authenticate?oauth_token=%s";
	
	private static String DETAILS_ENDPOINT 			= "https://api.twitter.com/1.1/users/show.json";
	
	private static String PARAMETERS_STRING			= "oauth_consumer_key=%s&" + 
											  		  "oauth_nonce=%s&" +
											  		  "oauth_signature_method=HMAC-SHA1&" + 
											  		  "oauth_timestamp=%s&";
	private static String PARAMETER_TOKEN_STRING 	= "oauth_token=%s&";
	private static String PARAMETER_VERSION_STRING 	= "oauth_version=1.0";
	
	private static String AUTHORIZATION_HEADER_STRING =  "OAuth oauth_consumer_key=\"%s\"," + 
														   	   "oauth_signature_method=\"HMAC-SHA1\"," + 
														   	   "oauth_timestamp=\"%s\"," + 
														   	   "oauth_nonce=\"%s\"," + 
														   	   "oauth_version=\"1.0\"," +
														   	   "oauth_signature=\"%s\"";
	private static String AUTHORIZATION_TOKEN_HEADER_STRING = ",oauth_token=\"%s\"";

	private static String OAUTH_SECRET_TOKEN			 = "oauth_token_secret";
	private static String OAUTH_SECRET_TOKEN_WITH_EQUAL	 = "oauth_token_secret=";
	private static String OAUTH_TOKEN			 		 = "oauth_token";
	private static String OAUTH_TOKEN_WITH_EQUAL 		 = "oauth_token=";
	private static String SCREEN_NAME 			 		 = "screen_name";
	private static String SCREEN_NAME_WITH_EQUAL 		 = "screen_name=";
	
	public static String SERVLET_PATH = "/user-portlet/login-with/twitter";
	
	private static final String URL_API_TWITTER = "http://urls.api.twitter.com/1/urls/count.json?url=";
	
	private static HttpParams httpParams = null;
	
	private static final String QUOTAS 	= GetterUtil.getString(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_TWITTER_QUOTA), "");
	
	private static SocialStatisticsQuotaController socialQuotaController = new SocialStatisticsQuotaController(QUOTAS);

	private static int MAX_API_ATTEMPTS = 3;
	
	private static String getPublicKey(long groupId) throws SecurityException, NoSuchMethodException
	{
		return SocialNetworkUtil.getConfigPublicKey(SocialNetworkUtil.SOCIAL_NAME_TWITTER, groupId);
	}
	
	private static String getPrivateKey(long groupId) throws SecurityException, NoSuchMethodException
	{
		return SocialNetworkUtil.getConfigSecretKey(SocialNetworkUtil.SOCIAL_NAME_TWITTER, groupId);
	}
	
	public static String getAuthenticateURL(long groupId) throws PortalException, SystemException, MalformedURLException, IOException
	{
		return String.format(AUTHENTICATE_ENDPOINT, getRequestToken(groupId));
	}
	
	public static String getRequestToken(long groupId) throws PortalException, SystemException
	{
		String accessToken = "";
		DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
		
		try
		{
			URL requestTokenURL = new URL(REQUEST_TOKEN_ENDPOINT);
				 
			HttpProcessor httpproc = new ImmutableHttpProcessor(
										new HttpRequestInterceptor[] {
											new RequestContent(), new RequestTargetHost(), 
											new RequestConnControl(), new RequestUserAgent(), 
											new RequestExpectContinue()});
	
			HttpRequestExecutor httpexecutor = new HttpRequestExecutor();
			HttpHost host = new HttpHost(requestTokenURL.getHost(), 443);
			
			HttpContext context = new BasicHttpContext(null);
			context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
			context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);

			SSLContext sslcontext = SSLContext.getInstance("TLSv1.2");
			sslcontext.init(null, null, null);
			
			SSLSocketFactory ssf = sslcontext.getSocketFactory();
			Socket socket = ssf.createSocket();
			socket.connect(new InetSocketAddress(host.getHostName(), host.getPort()), 0);
			
			conn.bind(socket, getHTTPParameters());
			
			BasicHttpEntityEnclosingRequest request2 = new BasicHttpEntityEnclosingRequest("POST", requestTokenURL.getPath());
			request2.setEntity(new StringEntity(StringPool.BLANK, "application/x-www-form-urlencoded", "UTF-8"));
			request2.setParams(getHTTPParameters());
			request2.addHeader("Authorization", getAuthorizationHeader(groupId, requestTokenURL.toString()));
			
			httpexecutor.preProcess(request2, httpproc, context);
			
			HttpResponse response2 = httpexecutor.execute(request2, conn, context);
			response2.setParams(getHTTPParameters());
			
			httpexecutor.postProcess(response2, httpproc, context);
			
			if(response2.getStatusLine().toString().indexOf("200") != -1)
			{
				StringTokenizer st = new StringTokenizer(EntityUtils.toString(response2.getEntity()), StringPool.AMPERSAND);
				String currenttoken = "";
				while(st.hasMoreTokens())
				{
					currenttoken = st.nextToken();
					if(currenttoken.startsWith(OAUTH_TOKEN_WITH_EQUAL))
					{
						accessToken = currenttoken.substring(currenttoken.indexOf("=") + 1);
						break;
					}
				}
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		finally 
		{
			try
			{
				conn.close();
			}
			catch(Exception e){}
		}
		
		return accessToken;
	}
	
	public static JSONObject getAccessDetails(String verifier_or_pin, String oauth_token, long groupId)
	{
		JSONObject details = JSONFactoryUtil.createJSONObject();
		DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
		
		try
		{
			URL accessTokenURL = new URL(ACCESS_TOKEN_ENDPOINT);
				 
			HttpProcessor httpproc = new ImmutableHttpProcessor(
										new HttpRequestInterceptor[] {
											new RequestContent(), new RequestTargetHost(), 
											new RequestConnControl(), new RequestUserAgent(), 
											new RequestExpectContinue()});
	
			HttpRequestExecutor httpexecutor = new HttpRequestExecutor();
			HttpHost host = new HttpHost(accessTokenURL.getHost(), 443);
			
			HttpContext context = new BasicHttpContext(null);
			context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
			context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);

			SSLContext sslcontext = SSLContext.getInstance("TLSv1.2");
			sslcontext.init(null, null, null);
			
			SSLSocketFactory ssf = sslcontext.getSocketFactory();
			Socket socket = ssf.createSocket();
			socket.connect(new InetSocketAddress(host.getHostName(), host.getPort()), 0);
			
			conn.bind(socket, getHTTPParameters());
			
			BasicHttpEntityEnclosingRequest request2 = new BasicHttpEntityEnclosingRequest("POST", accessTokenURL.getPath());
			request2.setEntity( new StringEntity("oauth_verifier=" + URLEncoder.encode(verifier_or_pin, "UTF-8"), "application/x-www-form-urlencoded", "UTF-8"));
			request2.setParams(getHTTPParameters());
			request2.addHeader("Authorization", getAuthorizationHeader(groupId, accessTokenURL.toString(), oauth_token));
			
			httpexecutor.preProcess(request2, httpproc, context);
			
			HttpResponse response2 = httpexecutor.execute(request2, conn, context);
			response2.setParams(getHTTPParameters());
			
			httpexecutor.postProcess(response2, httpproc, context);
			
			if(response2.getStatusLine().toString().indexOf("200") != -1)
			{
				StringTokenizer st = new StringTokenizer(EntityUtils.toString(response2.getEntity()), StringPool.AMPERSAND);
				String currenttoken = "";
				while(st.hasMoreTokens())
				{
					currenttoken = st.nextToken();
					if(currenttoken.startsWith(OAUTH_TOKEN_WITH_EQUAL))
					{
						details.put(OAUTH_TOKEN, currenttoken.substring(currenttoken.indexOf("=") + 1));
					}
					else if(currenttoken.startsWith(SCREEN_NAME_WITH_EQUAL))
					{
						details.put(SCREEN_NAME, currenttoken.substring(currenttoken.indexOf("=") + 1));
					}
					else if(currenttoken.startsWith(OAUTH_SECRET_TOKEN_WITH_EQUAL))
					{
						details.put(OAUTH_SECRET_TOKEN, currenttoken.substring(currenttoken.indexOf("=") + 1));
					}
				}
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		finally 
		{
			try
			{
				conn.close();
			}
			catch(Exception e){}
		}
		
		return details;
	}
	
	public static JSONObject getDetails(String verifier_or_pin, String oauth_token, long groupId)
	{
		JSONObject details = JSONFactoryUtil.createJSONObject();
		JSONObject accessDetails = getAccessDetails(verifier_or_pin, oauth_token, groupId);
		
		String screenName = accessDetails.getString(SCREEN_NAME);
		String oauthToken = accessDetails.getString(OAUTH_TOKEN);
		String oauthSecretToken = accessDetails.getString(OAUTH_SECRET_TOKEN);
		if(Validator.isNotNull(oauthToken) && Validator.isNotNull(screenName) && Validator.isNotNull(oauthSecretToken))
		{
			HttpURLConnection httpConnection = null;
			
			try
			{
				String detailsEndpoint = DETAILS_ENDPOINT + StringPool.QUESTION + SCREEN_NAME_WITH_EQUAL + screenName;

				httpConnection = (HttpURLConnection)(new URL(detailsEndpoint.toString()).openConnection());
				httpConnection.setConnectTimeout(ApacheUtil.getApacheConnTimeout());
				httpConnection.setReadTimeout(ApacheUtil.getApacheReadTimeout());
				httpConnection.setRequestMethod("GET");
				httpConnection.setRequestProperty("Authorization", getAuthorizationHeader(groupId, DETAILS_ENDPOINT, oauthToken, oauthSecretToken, screenName, "GET"));
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
	
	private static String computeSignature(String baseString, String keyString) throws GeneralSecurityException, UnsupportedEncodingException 
	{
	    byte[] keyBytes = keyString.getBytes();
	    
	    SecretKey secretKey = new SecretKeySpec(keyBytes, "HmacSHA1");

	    Mac mac = Mac.getInstance("HmacSHA1");
	    mac.init(secretKey);

	    byte[] text = baseString.getBytes();

	    return new String(Base64.encodeBase64(mac.doFinal(text))).trim();
	}
	
	private static String getAuthorizationHeader(long groupId, String accessTokenURL) throws UnsupportedEncodingException, GeneralSecurityException, PortalException, SystemException, SecurityException, NoSuchMethodException
	{
		return getAuthorizationHeader(groupId, accessTokenURL, null, null, null, "POST");
	}
	
	private static String getAuthorizationHeader(long groupId, String accessTokenURL, String accessToken) throws UnsupportedEncodingException, GeneralSecurityException, PortalException, SystemException, SecurityException, NoSuchMethodException
	{
		return getAuthorizationHeader(groupId, accessTokenURL, accessToken, null, null, "POST");
	}
	
	private static String getAuthorizationHeader(long groupId, String accessTokenURL, String oauthToken, String oauthSecretToken, String screen_name, String httpMethod) throws UnsupportedEncodingException, GeneralSecurityException, PortalException, SystemException, SecurityException, NoSuchMethodException
	{
		String oauth_nonce = UUID.randomUUID().toString().replaceAll(StringPool.DASH, StringPool.BLANK);
		String oauth_timestamp = (new Long(Calendar.getInstance().getTimeInMillis()/1000)).toString();
		
		String parameter_string = String.format(PARAMETERS_STRING, getPublicKey(groupId), oauth_nonce, oauth_timestamp);
		
		if(Validator.isNotNull(oauthToken))
			parameter_string += String.format(PARAMETER_TOKEN_STRING, oauthToken);
		
		parameter_string += PARAMETER_VERSION_STRING;
		
		if(Validator.isNotNull(screen_name))
			parameter_string += StringPool.AMPERSAND + SCREEN_NAME_WITH_EQUAL + URLEncoder.encode(screen_name, "UTF-8");
		
		String signature_base_string = httpMethod + StringPool.AMPERSAND + 
									   URLEncoder.encode(accessTokenURL.toString(), "UTF-8") + StringPool.AMPERSAND + 
									   URLEncoder.encode(parameter_string, "UTF-8");
		
		String privateKey = getPrivateKey(groupId) + StringPool.AMPERSAND;
		
		if(Validator.isNotNull(oauthToken) && Validator.isNotNull(oauthSecretToken))
			privateKey = URLEncoder.encode(getPrivateKey(groupId), "UTF-8") + StringPool.AMPERSAND + URLEncoder.encode(oauthSecretToken, "UTF-8");
			
		String oauth_signature = computeSignature(signature_base_string, privateKey);
		
		String authorizationHeader = String.format(AUTHORIZATION_HEADER_STRING, getPublicKey(groupId), oauth_timestamp, oauth_nonce, URLEncoder.encode(oauth_signature, "UTF-8"));
		if(Validator.isNotNull(oauthToken))
			authorizationHeader += String.format(AUTHORIZATION_TOKEN_HEADER_STRING, oauthToken);
		
		return authorizationHeader;
	}
	
	private static HttpParams getHTTPParameters()
	{
		if(httpParams == null)
		{
			httpParams = new SyncBasicHttpParams();
			HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(httpParams, "UTF-8");
			HttpProtocolParams.setUserAgent(httpParams, "HttpCore/1.1");
			HttpProtocolParams.setUseExpectContinue(httpParams, false);
		}
		
		return httpParams;
	}
	
	public static String XYZ_FIELD_USERNAME_ZYX(JSONObject user)
	{
		return user.getString("screen_name");
	}
	
	public static String XYZ_FIELD_AVATARURL_DEFAULT_ZYX(JSONObject user)
	{
		return user.getString("profile_image_url");
	}
	
	public static String XYZ_FIELD_LOCALE_ZYX(JSONObject user)
	{
		return user.getString("lang");
	}
	
	public static String XYZ_FIELD_LOCATION_ZYX(JSONObject user)
	{
		return user.getString("location");
	}
	
	public static String XYZ_FIELD_NAME_ZYX(JSONObject user)
	{
		return user.getString("name");
	}
	
	public static String XYZ_FIELD_FIRSTNAME_ZYX(JSONObject user)
	{
		return XYZ_FIELD_NAME_ZYX(user);
	}
	
	public static String XYZ_FIELD_PROFILEURL_ZYX(JSONObject user)
	{
		return user.getString("url");
	}
	
	public static String XYZ_FIELD_TIMEZONE_ZYX(JSONObject user)
	{
		return user.getString("time_zone");
	}

	public static JSONObject getStatistic(String url) throws ServiceError
	{
		JSONObject result = null;
		String twitterURL = URL_API_TWITTER + url;
		
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
			
				HttpGet get = new HttpGet(twitterURL);
				
				// Para no llenar el log de catalina en caso de caida del servicio de Twitter, intentamos conectar, si no lo logramos se prueba más tarde
				boolean conected = false;
				int attempt = 0;
				while (!conected && attempt <= MAX_API_ATTEMPTS)
				{
					try
					{
						httpResponse = client.execute(get); 
						conected = true;
					}
					catch(Exception e)
					{
						attempt++;
						_log.info(new StringBuilder("Impossible to connect with Twitter (attempt ")
					       .append(attempt).append(", thread: '").append(Thread.currentThread().getName()).append("'). ")
					       .append("Trying to conect with it in ")
					       .append(IterKeys.NEXT_TRY_TO_CONECT_TO_SOCIAL_NETWORK).append("m"));
						Thread.sleep( (IterKeys.NEXT_TRY_TO_CONECT_TO_SOCIAL_NETWORK * 60 * 1000) );							
					}	
				}		
					
				// Codigo de respuesta
				if(httpResponse != null)
				{
					responseStatus = httpResponse.getStatusLine().getStatusCode();
					responseBody = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
				}
			}
			catch(Exception e)
			{
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
			
			_log.debug(new StringBuffer("twitter received response code: ").append(responseStatus));
			_log.debug("twitter received response: " + (null == responseBody ? "null" : responseBody));
				
			
			if(Validator.isNotNull(httpResponse) && Validator.equals(HttpStatus.SC_OK,  responseStatus) && Validator.isNotNull(responseBody))
			{
				max_age = getMaxAge(httpResponse);
			}
		}
		else
		{
			_log.debug("twitter limit quota: " + quota);
			quotaTimeSleep = socialQuotaController.getTimeToSleep(quota)/1000;
		}
		
		JSONArray body = null;
		try
		{
			body = JSONFactoryUtil.createJSONArray();
			body.put(JSONFactoryUtil.createJSONObject(responseBody));
		}
		catch (JSONException e)
		{
			_log.error("Error creating a json with the twitter response");
			_log.error(e);
		}
		result = JSONFactoryUtil.createJSONObject();
		result.put("max-age", max_age);
		result.put("body", (body == null) ? "" : body.toString());
		result.put("quota-time-sleep", quotaTimeSleep);
		return result;
	}
	
	private static Long getMaxAge(HttpResponse httpResponse)
	{
		Long max_age = 0L;
		Header[] headers = httpResponse.getHeaders("Cache-Control");
		for (Header header : headers)
		{
			if(header.getValue().contains("max-age")){
				int indexof_maxage_value = header.getValue().indexOf("max-age=")+8;
				if(max_age < Long.valueOf(header.getValue().substring(indexof_maxage_value)))
				{
					max_age = Long.valueOf(header.getValue().substring(indexof_maxage_value));
				}
			}
		}
		return max_age;
	}
}