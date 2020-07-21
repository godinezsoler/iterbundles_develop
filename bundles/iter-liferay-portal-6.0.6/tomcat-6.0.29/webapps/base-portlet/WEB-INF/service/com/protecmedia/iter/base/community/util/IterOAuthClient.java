package com.protecmedia.iter.base.community.util;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.TreeMap;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

public class IterOAuthClient
{
	private static Log _log = LogFactoryUtil.getLog(IterOAuthClient.class);
	
	private String url;
	private String publicKey;
	private String secretKey;
	private String method;
	private String queryString;
	private String postBody;
	private String callBackURL;
	private String oAuthToken;
	private String oAuthVerifier;
	private String oAuthTokenSecret;
	
	private String encodedQueryString;
	private String oAuthAuthorizarionHeader;
	
	private String responseContentType;
	
	public static class Builder
	{
		// Required parameters
		private final String url;
		private final String publicKey;
		private final String secretKey;
		private final String method;
		
		// Optional parameters - initialized to default values
		private String queryString;
		private String postBody;
		private String callBackURL;
		private String oAuthToken;
		private String oAuthVerifier;
		private String oAuthTokenSecret;
		
		public Builder(String url, String publicKey, String secretKey, String method)
		{
			this.url       = url;
			this.publicKey = publicKey;
			this.secretKey = secretKey;
			this.method    = method;
		}

		public Builder queryString(String queryString)           { this.queryString = queryString; return this; }
		public Builder postBody(String postBody)                 { this.postBody = postBody; return this; }
		public Builder callBackURL(String callBackURL)           { this.callBackURL = callBackURL; return this; }
		public Builder oAuthToken(String oAuthToken)             { this.oAuthToken = oAuthToken; return this; }
		public Builder oAuthVerifier(String oAuthVerifier)       { this.oAuthVerifier = oAuthVerifier; return this; }
		public Builder oAuthTokenSecret(String oAuthTokenSecret) { this.oAuthTokenSecret = oAuthTokenSecret; return this; }
		
		public IterOAuthClient build() throws SystemException    { return new IterOAuthClient(this); }
	}
	
	private IterOAuthClient(Builder builder) throws SystemException
	{
		url              = builder.url;
		publicKey        = builder.publicKey;
		secretKey        = builder.secretKey;
		method           = builder.method;
		queryString      = builder.queryString;
		postBody         = builder.postBody;
		callBackURL      = builder.callBackURL;
		oAuthToken       = builder.oAuthToken;
		oAuthVerifier    = builder.oAuthVerifier;
		oAuthTokenSecret = builder.oAuthTokenSecret;
		
		oAuthAuthorizarionHeader = buildAuthorizationHeader();
	}

	private static final int CONNECT_TIMEOUT = 5000;
	private static final int READ_TIMEOUT = 5000;
	
	public String post() throws SystemException
	{
		String response = StringPool.BLANK;
		HttpURLConnection httpConnection = null;
		try
		{
			String postURL = encodedQueryString != null ? url + StringPool.QUESTION + encodedQueryString : url;
			httpConnection = (HttpURLConnection)(new URL(postURL).openConnection());
			httpConnection.setConnectTimeout(CONNECT_TIMEOUT);
			httpConnection.setReadTimeout(READ_TIMEOUT);
			
			httpConnection.setRequestMethod(method);
			if ("POST".equals(method))
			{
				httpConnection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				httpConnection.addRequestProperty("Content-Length", "0");
			}
			httpConnection.addRequestProperty("Authorization", oAuthAuthorizarionHeader);
			
			httpConnection.setDoOutput(true);
			httpConnection.setDoInput(true);
			
			httpConnection.connect();
			responseContentType = httpConnection.getContentType();
			
			if ( HttpServletResponse.SC_OK == httpConnection.getResponseCode())
			{
				response = StreamUtil.toString(httpConnection.getInputStream(), StringPool.UTF8);
			}
			else
			{
				response = StreamUtil.toString(httpConnection.getErrorStream(), StringPool.UTF8);
			}
		}
		catch (Throwable th)
		{
			_log.error(th.getMessage());
			_log.trace(th);
			throw new SystemException(th);
		}
		finally
		{
			if (httpConnection != null)
				httpConnection.disconnect();
		}
		return response;
	}
	
	//----------------------------------------------------------------------------------------------------------------------
	// PARÁMETROS DE LA CABECERA OAUTH
	//----------------------------------------------------------------------------------------------------------------------
	private static final String OAUTH                  = "OAuth ";
	private static final String OAUTH_CALLBACK         = "oauth_callback";
	private static final String OAUTH_CONSUMER_KEY     = "oauth_consumer_key";
	private static final String OAUTH_NONCE            = "oauth_nonce";
	private static final String OAUTH_SIGNATURE_METHOD = "oauth_signature_method";
	private static final String OAUTH_SIGNATURE        = "oauth_signature";
	private static final String OAUTH_TIMESTAMP        = "oauth_timestamp";
	private static final String OAUTH_TOKEN            = "oauth_token";
	private static final String OAUTH_VERIFIER         = "oauth_verifier";
	private static final String OAUTH_VERSION          = "oauth_version";
	
	private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
	
	public String buildAuthorizationHeader() throws SystemException
	{
		TreeMap<String, String> signingParameters = new TreeMap<String, String>();
		
		// Recupera los parámetros del query string para la firma.
		retrieveQueryStringParameters(queryString, signingParameters);
		
		// Recupera los parámetros del POST body para la firma
		retrieveParameters(postBody, signingParameters);
		
		// Construye la cabecera HTTP
		StringBuilder header = new StringBuilder(OAUTH);
		
		// URL DE REDIRECCIÓN (OPCIONAL)
		if (null != callBackURL)
			addHeaderParam(header, signingParameters, OAUTH_CALLBACK, callBackURL);
		
		// CONSUMER KEY
		addHeaderParam(header, signingParameters, OAUTH_CONSUMER_KEY, publicKey);
		
		// NONCE
		String nonce = UUID.randomUUID().toString().replaceAll(StringPool.DASH, StringPool.BLANK);
		addHeaderParam(header, signingParameters, OAUTH_NONCE, nonce);
		
		// SIGNATURE METHOD
		addHeaderParam(header, signingParameters, OAUTH_SIGNATURE_METHOD, "HMAC-SHA1");
		
		// TIMESTAMP
		String timestamp = (new Long(Calendar.getInstance().getTimeInMillis()/1000)).toString();
		addHeaderParam(header, signingParameters, OAUTH_TIMESTAMP, timestamp);
		
		// OAUTH_TOKEN
		if (null != oAuthToken)
			addHeaderParam(header, signingParameters, OAUTH_TOKEN, oAuthToken);
		
		// OAUTH_VERIFIER
		if (null != oAuthVerifier)
			addHeaderParam(header, signingParameters, OAUTH_VERIFIER, oAuthVerifier);
		
		// VERSION
		addHeaderParam(header, signingParameters, OAUTH_VERSION, "1.0");
		
		// SIGNATURE
		StringBuilder signatureParamsBaseString = new StringBuilder();
		for (String key : signingParameters.keySet())
		{
			// Añade el parámetro a la firma
			if (!StringPool.BLANK.equals(signatureParamsBaseString.toString())) signatureParamsBaseString.append(StringPool.AMPERSAND);
			buildOAuthParam(signatureParamsBaseString, key, signingParameters.get(key), false);
		}
		// Crea la cadena base de la firma.
		StringBuilder signatureBaseString = new StringBuilder(method)
			.append(StringPool.AMPERSAND)
			.append(percentEncode(url))
			.append(StringPool.AMPERSAND)
			.append(percentEncode(signatureParamsBaseString.toString()));
		// Crea la clave con la que se va a firmar.
		StringBuilder signingKey = new StringBuilder()
			.append(percentEncode(secretKey))
			.append(StringPool.AMPERSAND)
			.append(Validator.isNotNull(oAuthTokenSecret) ? percentEncode(oAuthTokenSecret) : StringPool.BLANK);
		// Añade la firma a la cabecera HTTP.
		addHeaderParam(header, null, OAUTH_SIGNATURE, generateSignature(signatureBaseString.toString(), signingKey.toString()));
		
		return header.toString();
	}
	
	/**
	 * <p>Procesa el {@code queryString} añadiendo los parámetros a {@code signingParameters}.</p>
	 * 
	 * @param queryString		el query string a procesar.
	 * @param signingParameters	el {@code TreeMap} al que añadir los parámetros del query string.
	 * @return					la URL sin el query string.
	 */
	private void retrieveQueryStringParameters(String queryString, TreeMap<String, String> signingParameters)
	{
		StringBuilder encodedQueryStringBuilder = new StringBuilder();
		// Inicializa el mapa de parámetros si es nulo.
		if (null == signingParameters) signingParameters = new TreeMap<String, String>();
		
		// Si tiene QueryString, procesa los parámetros.
		if (queryString != null)
		{
			// Separa los parámetros.
			String[] queryStringParams = queryString.split(StringPool.AMPERSAND);
			for (String param : queryStringParams)
			{
				// Añade el parámetro al TreeMap.
				String[] pair = param.split(StringPool.EQUAL);
				signingParameters.put(pair[0], pair.length == 2 ? pair[1] : StringPool.BLANK);
				// Codifica el parámetro en el query string
				encodedQueryStringBuilder.append(percentEncode(pair[0])).append(StringPool.EQUAL).append(pair.length == 2 ? percentEncode(pair[1]) : StringPool.BLANK);
			}
			encodedQueryString = encodedQueryStringBuilder.toString();
		}
	}
	
	private static void retrieveParameters(String paramString, TreeMap<String, String> signingParameters)
	{
		if (null != paramString)
		{
			// Separa los parámetros.
			String[] params = paramString.split(StringPool.AMPERSAND);
			for (String param : params)
			{
				// Añade el parámetro al TreeMap.
				String[] pair = param.split(StringPool.EQUAL);
				signingParameters.put(pair[0], pair.length == 2 ? pair[1] : StringPool.BLANK);
			}
		}
	}
	
	/**
	 * <p>Añade el parámetro {@code key} con el valor {@code value} al {@code header} separándolo
	 * con el carácter {@code ','} si ya hubiera parámetros.</p>
	 * <p>Además incluye el parámetro en el {@code TreeMap signingParameters} para calcular la firma.
	 * 
	 * @param header			la cadena que representa la cabecera HTTP.
	 * @param signingParameters el listado de parámetros para calcular la firma.
	 * @param key				la clave del parámetro.
	 * @param value				el valor del parámetro.
	 */
	private static void addHeaderParam(StringBuilder header, TreeMap<String, String> signingParameters, String key, String value)
	{
		// Añade el parámetro al header
		if (null != header)
		{
			if (!OAUTH.equals(header.toString())) header.append(StringPool.COMMA_AND_SPACE);
			buildOAuthParam(header, key, value, true);
		}

		// Guarda el parámetro para la firma
		if (null != signingParameters)
		{
			signingParameters.put(key, value);
		}
	}
	
	private static void buildOAuthParam(StringBuilder builder, String paramKey, String paramValue, boolean enclose)
	{
		builder.append(percentEncode(paramKey));
		
		if (null != paramValue)
		{
			builder.append(StringPool.EQUAL);
			builder.append( enclose ? StringUtil.enclose(percentEncode(paramValue), StringPool.QUOTE) : percentEncode(paramValue) );
		}
	}
	
	/**
	 * <p>Encripta la cadena {@code signatureBaseString} con la clave {@code signingKey} usando el 
	 * algoritmo {@code HMAC_SHA1}.</p>
	 * 
	 * @param signatureBaseString
	 * @param signingKey
	 * @return
	 * @throws SystemException
	 */
	private static String generateSignature(String signatureBaseString, String signingKey) throws SystemException
	{
		String signature = StringPool.BLANK;
		
		// Obtiene una clave HMAC_SHA1
		SecretKey secretKey = new SecretKeySpec(signingKey.getBytes(), HMAC_SHA1_ALGORITHM);
		try
		{
			// Inicializa el cifrador
			Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
			mac.init(secretKey);
			// Cifra la cadena
			byte[] rawHmac = mac.doFinal(signatureBaseString.getBytes());
			// Convierte la cadena cifrada a base64
			signature = new String(Base64.encodeBase64(rawHmac)).trim();
		}
		catch (Throwable th)
		{
			throw new SystemException(th);
		}
		return signature;
	}
	
	public static String percentEncode(String s)
	{
		if (s == null)
			return StringPool.BLANK;
		
		try
		{
	        s = URLEncoder.encode(s, "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
	    }
		catch (UnsupportedEncodingException wow)
		{
			_log.error("Unable to percent encode string.");
			_log.trace(wow);
	    }
		
		return s;
	}
	
	public String getResponseContentType() { return responseContentType; }
}
