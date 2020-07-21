package com.protecmedia.iter.base.community.shortener;

import java.util.Date;

import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.util.IterHttpClient;
import com.liferay.portal.util.IterMonitor;

public class BitlyURLShortener implements URLShortener
{
	private static final String URL_ENDPOINT = "https://api-ssl.bitly.com/v3/shorten?access_token=%s&longUrl=%s";
	
	private long groupId;
	private String accessToken = null;
	
	private String currentArticleId;
	private String shortUrl;
	
	public BitlyURLShortener(long groupId)
	{
		this.groupId = groupId;
		// Busca la configuración de bity en la configuración de acortadores del grupo.
		this.accessToken = GroupConfigTools.getGroupConfigXMLField(groupId, "shorteners", "/shorteners/shortener[@service='bitly']/@token");
	}
	
	@Override
	public boolean isConfigured()
	{
		return (accessToken != null && !accessToken.isEmpty());
	}
	
	@Override
	public String shorten(String longUrl, String articleId)
	{
		this.shortUrl = longUrl;
		this.currentArticleId = articleId;
		
		if (isConfigured())
		{
			IterHttpClient iterHttpclient = new IterHttpClient.Builder(IterHttpClient.Method.GET, String.format(URL_ENDPOINT, accessToken, longUrl)).build();
			String response = iterHttpclient.connect();
			
			if (iterHttpclient.validResponse())
			{
				getShortURL(response);
			}
			else
			{
				// Registra el error en el monitor
				try
				{
					// Intenta extraer el error de la respuesta
					registerError(parseResponse(response));
				}
				catch (JSONException e)
				{
					// Si no puede, registra un error de conexión
					IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new Date(), currentArticleId, "Unable to short article URL: API connection error.");
				}
			}
		}
		else
		{
			// Registrar en monitor NOT CONFIGURED
			IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new Date(), currentArticleId, "Unable to short article URL: Bitly account is not configured.");
		}
		
		return this.shortUrl;
	}

	private void getShortURL(String response)
	{
		try
		{
			// Parsea la respuesta
			JSONObject jsonResponse = parseResponse(response);
			
			switch (jsonResponse.getInt("status_code"))
			{
				case 200:
				case 304:
					this.shortUrl = jsonResponse.getJSONObject("data").getString("url");
					break;
				default:
					registerError(jsonResponse);
			}
		}
		catch (Throwable th)
		{
			IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new Date(), currentArticleId, "Unable to short article URL: API response error.");
		}
	}
	
	private void registerError(JSONObject jsonResponse)
	{
		String error = jsonResponse.getString("status_txt");
		IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new Date(), currentArticleId, "Unable to short article URL: API request error.", error);
	}
	
	private JSONObject parseResponse(String response) throws JSONException
	{
		return JSONFactoryUtil.createJSONObject(response);
	}
}
