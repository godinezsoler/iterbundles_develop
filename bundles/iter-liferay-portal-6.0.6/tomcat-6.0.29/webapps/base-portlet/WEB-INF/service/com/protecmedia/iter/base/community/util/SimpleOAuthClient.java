package com.protecmedia.iter.base.community.util;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;

public class SimpleOAuthClient
{
	private static Log _log = LogFactoryUtil.getLog(SimpleOAuthClient.class);
	DateFormat df;
	
	private String method;
	private String url;
	private String apiKey;
	private String apiSecret;
	private String date;
	private String contentType;
	private String payload;
	private String metadata;
	
	////////////////////////////////////////////////////////////////////////////////////////////
	//                                       CONSTRUCTOR                                      //
	////////////////////////////////////////////////////////////////////////////////////////////
	public static class Builder
	{
		// Required parameters
		private final String method;
		private final String url;
		
		// Optional parameters - initialized to default values
		private String apiKey;
		private String apiSecret;
		private String contentType;
		private String payload;
		private String metadata;
		
		public Builder(String method, String url)
		{
			this.url       = url;
			this.method    = method;
		}

		public Builder apiKey(String apiKey)                 { this.apiKey = apiKey; return this; }
		public Builder apiSecret(String apiSecret)           { this.apiSecret = apiSecret; return this; }
		public Builder contentType(String contentType)       { this.contentType = contentType; return this; }
		public Builder payLoad(String payload)               { this.payload = payload; return this; }
		public Builder metadata(String metadata)             { this.metadata = metadata; return this; }
		
		public SimpleOAuthClient build()    { return new SimpleOAuthClient(this); }
	}
	
	private SimpleOAuthClient(Builder builder)
	{
		// Iniicaliza la fecha
		df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		TimeZone tz = TimeZone.getTimeZone("UTC");
		df.setTimeZone(tz);
		
		// Asigna el valor a los atributos
		this.method      = builder.method;
		this.url         = builder.url;
		this.apiKey      = builder.apiKey;
		this.apiSecret   = builder.apiSecret;
		this.date        = df.format(new Date());
		this.contentType = builder.contentType == null ? ContentType.OCTET_STREAM : builder.contentType;
		this.payload     = builder.payload == null ? StringPool.BLANK : builder.payload;
		this.metadata    = builder.metadata == null ? StringPool.BLANK : builder.metadata;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	//                                       CONSTANTES                                       //
	////////////////////////////////////////////////////////////////////////////////////////////
	public static class Method
	{
		public static final String POST = "POST";
		public static final String GET  = "GET";
	}

	public static class ContentType
	{
		public static final String OCTET_STREAM = "application/octet-stream";
		public static final String JSON         = "application/json";
		public static final String MULTIPART    = "multipart/form-data;";
	}

	public static class Algorithm
	{
		public static final String HMAC_SHA256 = "HmacSHA256";
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	//                                         LOGICA                                         //
	////////////////////////////////////////////////////////////////////////////////////////////

	private static final int CONNECT_TIMEOUT = 5000;
	private static final int READ_TIMEOUT = 5000;
	
	public String connect()
	{
		String response = StringPool.BLANK;
		HttpURLConnection httpConnection = null;
		try
		{
			httpConnection = (HttpURLConnection)(new URL(url).openConnection());
			
			httpConnection.setConnectTimeout(CONNECT_TIMEOUT);
			httpConnection.setReadTimeout(READ_TIMEOUT);
			
			httpConnection.setDoOutput(true);
			
			httpConnection.setRequestMethod(method);
			
			/////////////////////////////////////////
			if (!payload.isEmpty() && ContentType.MULTIPART.equals(contentType))
			{
				MultipartEntity entity = new MultipartEntity();
				
				// Metadatos
				if (Validator.isNotNull(metadata))
				{
					ContentBody body = new StringBody(metadata, ContentType.JSON, Charset.defaultCharset());
					FormBodyPart bodyPart = new FormBodyPart("metadata", body);
					entity.addPart(bodyPart);
				}
				
				// Artículo
				ContentBody body = new StringBody(payload, ContentType.JSON, Charset.defaultCharset());
				FormBodyPart customBodyPart = new FormBodyPart("file", body) {

				    @Override
				    protected void generateContentDisp(final ContentBody body) {
				        addField(MIME.CONTENT_DISPOSITION, "form-data; name=\"article\"; filename=\"article.json\"");
				    }

				};
				entity.addPart(customBodyPart);
				
				// Actualiza el content-type
				this.contentType = entity.getContentType().getValue();
				
				// Actualiza el payload
				ByteArrayOutputStream out = new ByteArrayOutputStream((int)entity.getContentLength());
				entity.writeTo(out);
				this.payload = out.toString();
				out.flush();
				out.close();

				httpConnection.addRequestProperty("Content-Type", contentType);
				httpConnection.addRequestProperty("Content-Length", String.valueOf(payload.length()));
			}
			/////////////////////////////////////////
			httpConnection.addRequestProperty("Authorization", buildAuthorizationHeader());

			// Añade los datos del payLoad.
			if (!payload.isEmpty())
			{
				OutputStream os = null;
				BufferedWriter writer = null;
				try
				{
					os = httpConnection.getOutputStream();
					writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
					writer.write(payload);
					writer.flush();
				}
				finally
				{
					if (writer != null) writer.close();
					if (os != null)     os.close();
				}
			}
			
			httpConnection.connect();
			response = getResponse(httpConnection);
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
		finally
		{
			if (httpConnection != null)
				httpConnection.disconnect();
		}
		return response;
	}
	
	private static final String AUTHORIZATION_HEADER = "HHMAC; key=%s; signature=%s; date=%s ";
	
	public String buildAuthorizationHeader()
	{
		StringBuilder authorizationHeader = new StringBuilder();
		
		// 1. Crea la versión canonica de la petición
		// El método en mayúsculas
		authorizationHeader.append(method);
		// La URL completa de la petición
		authorizationHeader.append(url);
		// La fecha actual en formato ISO 8601
		authorizationHeader.append(date);
		// Si la petición es un POST...
		if (Method.POST.equals(method) && !payload.isEmpty())
		{
			// El valor de Content-Type
			authorizationHeader.append(contentType);
			// El contenido completo de la entidad
			authorizationHeader.append(payload);
		}
		
		String authorization = StringPool.BLANK;
		
		try
		{
			// 2. Decodifica la API KEY SECRET de Base64 a bytes
			byte[] rawByteApiSecret = Base64.decodeBase64(apiSecret);
			
			// 3. Crea el hash usando HMAC SHA-256
			// Obtiene una clave HMAC_SHA1
			SecretKey key = new SecretKeySpec(rawByteApiSecret, Algorithm.HMAC_SHA256);
			// Inicializa el cifrador
			Mac mac = Mac.getInstance(Algorithm.HMAC_SHA256);
			mac.init(key);
			// Cifra la cadena
			byte[] hash = mac.doFinal(authorizationHeader.toString().getBytes());
			
			// 4. Codifica el hash en Base64
			authorization = new String(Base64.encodeBase64(hash)).trim();
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
		
		// 5. Establece la cabecera Authorization
		return String.format(AUTHORIZATION_HEADER, apiKey, authorization, date);
	}
	
	private String getResponse(HttpURLConnection httpConnection) throws IOException
	{
		// Actualiza el estado
		int responseStatus = httpConnection.getResponseCode();
		
		// Recupera la respuesta
		switch (responseStatus)
		{
			// 2xx
			case HttpServletResponse.SC_OK:
			case HttpServletResponse.SC_CREATED:
			case HttpServletResponse.SC_ACCEPTED:
			case HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION:
			case HttpServletResponse.SC_NO_CONTENT:
			case HttpServletResponse.SC_RESET_CONTENT:
			case HttpServletResponse.SC_PARTIAL_CONTENT:
				String contentType = httpConnection.getContentType();
				String charSet = "UTF-8";
				if (contentType.contains("text/html") && contentType.indexOf("charset=") > -1)
					charSet = contentType.substring(contentType.indexOf("charset=") + 8);
				
				return StreamUtil.toString(httpConnection.getInputStream(), charSet);
				
			// 3xx
			// 1xx, 3xx, 4xx, 5xx
			default:
				return StreamUtil.toString(httpConnection.getErrorStream(), StringPool.UTF8);
		} 
	}
}
