package com.protecmedia.iter.base.community.util;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;

public class CommunityHttpClient
{
	private static Log _log = LogFactoryUtil.getLog(CommunityHttpClient.class);
			
	private static final int CONNECT_TIMEOUT = 5000;
	private static final int READ_TIMEOUT    = 5000;
	
	private static final String GET  = "GET";
	private static final String POST = "POST";
	
	private String url;
	private HashMap<String, String> httpHeader;
	private HashMap<String, String> queryString;
	private String payload;
	
	
	public static class Builder
	{
		// Parámetros obligatorios.
		private final String url;
		
		// Parámetros opcionales.
		private HashMap<String, String> httpHeader;
		private HashMap<String, String> queryString;
		private String payload;
		
		// Constructor con los parámetros obligatorios.
		public Builder(String url)
		{
			this.url = url;
			this.httpHeader  = new HashMap<String, String>(); 
			this.queryString = new HashMap<String, String>(); 
		}

		public Builder header(String key, String value)           { this.httpHeader.put(key, value); return this; }
		public Builder queryString(String key, String value)      { this.queryString.put(key, value); return this; }
		public Builder payload(String payload)                    { this.payload = payload; return this; }
		
		public CommunityHttpClient build() throws SystemException { return new CommunityHttpClient(this); }
	}
	
	private CommunityHttpClient(Builder builder)
	{
		this.url         = builder.url;
		this.httpHeader  = builder.httpHeader;
		this.queryString = builder.queryString;
		this.payload     = builder.payload;
	}
	
	public JSONObject get() throws SystemException
	{
		JSONObject communityResponse = JSONFactoryUtil.createJSONObject();
		HttpURLConnection httpConnection = null;
		
		// Forma la URL final añadiendo los parámetros del query string codificados
		StringBuilder finalUrl = new StringBuilder(url).append(serializeQueryString(queryString));
		
		try
		{
			// Inicializa la conexión.
			URL url = new URL(finalUrl.toString());
			httpConnection = (HttpURLConnection) url.openConnection();
			httpConnection.setConnectTimeout(CONNECT_TIMEOUT);
			httpConnection.setReadTimeout(READ_TIMEOUT);

			// Establece el método POST.
			httpConnection.setRequestMethod(GET);
			// Realiza la petición.
			httpConnection.connect();
			
			// Procesa la respuesta.
			if ( HttpServletResponse.SC_OK == httpConnection.getResponseCode())
			{
				communityResponse = processResponse(StreamUtil.toString(httpConnection.getInputStream(), StringPool.UTF8));
			}
			else
			{
				communityResponse = processResponse(StreamUtil.toString(httpConnection.getErrorStream(), StringPool.UTF8));
			}
		}
		catch (Throwable th)
		{
			_log.error(th.getMessage());
			_log.trace(th);
			throw new SystemException("Connection error", th);
		}
		finally
		{
			if (httpConnection != null) httpConnection.disconnect();
		}
		
		return communityResponse;
	}
	
	/**
	 * 
	 * @return
	 * @throws SystemException
	 */
	public JSONObject post() throws SystemException
	{	
		JSONObject communityResponse = JSONFactoryUtil.createJSONObject();
		HttpURLConnection httpConnection = null;
		
		// Forma la URL final añadiendo los parámetros del query string codificados
		StringBuilder finalUrl = new StringBuilder(url).append(serializeQueryString(queryString));
		
		try
		{
			// Inicializa la conexión.
			URL url = new URL(finalUrl.toString());
			httpConnection = (HttpURLConnection) url.openConnection();
			httpConnection.setConnectTimeout(CONNECT_TIMEOUT);
			httpConnection.setReadTimeout(READ_TIMEOUT);
			httpConnection.setDoOutput(true);
			
			// Establece el método POST.
			httpConnection.setRequestMethod(POST);
			
			// Añade los parámetros de la cabecera.
			for (String key : httpHeader.keySet())
			{
				String value = httpHeader.get(key);
				httpConnection.addRequestProperty(key, value == null ? StringPool.BLANK : value);
			}
			
			// Añade los datos del payLoad.
			if (Validator.isNotNull(payload))
			{
				// Calcula el content lenght.
				httpConnection.addRequestProperty("Content-Length", String.valueOf(payload.length()));
				// Vuelca el payload.
				httpConnection.setDoInput(true);
				DataOutputStream outputStream = new DataOutputStream(httpConnection.getOutputStream());
		        outputStream.writeBytes(payload);
		        outputStream.flush();
		        outputStream.close();
			}
			
			// Realiza la petición.
			httpConnection.connect();
			
			// Procesa la respuesta.
			if ( HttpServletResponse.SC_OK == httpConnection.getResponseCode())
			{
				communityResponse = processResponse(StreamUtil.toString(httpConnection.getInputStream(), StringPool.UTF8));
			}
			else
			{
				communityResponse = processResponse(StreamUtil.toString(httpConnection.getErrorStream(), StringPool.UTF8));
			}
		}
		catch (Throwable th)
		{
			_log.error(th.getMessage());
			_log.trace(th);
			throw new SystemException("Connection error", th);
		}
		finally
		{
			if (httpConnection != null) httpConnection.disconnect();
		}
		
		return communityResponse;
	}
	
	private static JSONObject processResponse(String response)
	{
		JSONObject jsonResponse;
		try
		{
			jsonResponse = JSONFactoryUtil.createJSONObject(response);
		}
		catch (JSONException e)
		{
			_log.debug("Response is not JSON");
			jsonResponse = JSONFactoryUtil.createJSONObject();
			HashMap<String, String> responseParams = deserializeQueryString(response);
			for (String key : responseParams.keySet())
			{
				String value = responseParams.get(key);
				jsonResponse.put(key, value == null ? StringPool.BLANK : value);
			}
		}
		return jsonResponse;
	}
	
	public static HashMap<String, String> deserializeQueryString(String queryString)
	{
		// Inicializa el mapa de parámetros.
		HashMap<String, String> parameters = new HashMap<String, String>();
		
		// Si tiene QueryString, procesa los parámetros.
		if (Validator.isNotNull(queryString))
		{
			// Si empieza por ?, lo elimina
			if (queryString.startsWith(StringPool.QUESTION))
				queryString = queryString.substring(1);
			
			// Separa los parámetros.
			String[] queryStringParams = queryString.split(StringPool.AMPERSAND);
			for (String param : queryStringParams)
			{
				// Añade el parámetro al TreeMap.
				String[] pair = param.split(StringPool.EQUAL);
				parameters.put(pair[0], pair.length == 2 ? pair[1] : StringPool.BLANK);
			}
		}
		// Devuelve el mapa de parámetros.
		return parameters;
	}
	
	public static String serializeQueryString(HashMap<String, String> queryStringParams)
	{
		StringBuilder queryString = new StringBuilder();
		
		if (queryStringParams != null && queryStringParams.size() > 0)
		{
			queryString.append(StringPool.QUESTION);
			for (String key : queryStringParams.keySet())
			{
				String value = queryStringParams.get(key);
				if (queryString.length() > 1) queryString.append(StringPool.AMPERSAND);
				queryString.append(HttpUtil.encodeURL(key)).append(StringPool.EQUAL).append( value == null ? StringPool.BLANK : HttpUtil.encodeURL(value));
			}
		}
		
		return queryString.toString();
	}
}
