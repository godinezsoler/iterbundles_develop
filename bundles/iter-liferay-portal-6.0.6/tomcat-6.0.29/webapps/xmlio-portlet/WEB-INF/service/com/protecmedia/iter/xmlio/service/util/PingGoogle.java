package com.protecmedia.iter.xmlio.service.util;

import java.net.HttpURLConnection;
import java.net.URL;

import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.HttpMethods;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.IterSecureConfigTools;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.protecmedia.iter.base.service.GroupConfigLocalServiceUtil;

public class PingGoogle extends Thread
{
	private static final Log _log = LogFactoryUtil.getLog(PingGoogle.class);
	private final String _url = "http://www.google.com/webmasters/tools/ping?sitemap=%s";
	private final String _servletUrlPattern = "%s%s/sitemapforgoogle.xml";
	private long _scopeGroupId = 0;
	
	public PingGoogle( long scopeGroupId )
	{
		this._scopeGroupId = scopeGroupId;
	}
	
	public void run()
	{
		callGoogle();
	}
	
	private void callGoogle()
	{
		HttpURLConnection httpConnection = null;
		
		try
		{
			String host 	= LayoutSetLocalServiceUtil.getLayoutSet(_scopeGroupId, false).getVirtualHost();
			String protocol = IterSecureConfigTools.getConfiguredHTTPS(_scopeGroupId) ? Http.HTTPS_WITH_SLASH : Http.HTTP_WITH_SLASH;
			String url 		= String.format(_url, String.format(_servletUrlPattern, protocol, host));
			
			_log.debug("ping google url: " + url);
			
			httpConnection = (HttpURLConnection)(new URL(url).openConnection());
	        httpConnection.setConnectTimeout( PropsValues.ITER_PINGGOOGLESITEMAP_CONEXIONTIMEOUT );
	        httpConnection.setReadTimeout( PropsValues.ITER_PINGGOOGLESITEMAP_RESPONSETIMEOUT );
	       	httpConnection.setRequestMethod( HttpMethods.GET );
	       
	        httpConnection.connect();
	        
	        HttpUtil.throwIfConnectionFailed(httpConnection, IterErrorKeys.XYZ_E_PING_GOOGLE_URLCONNECTION_FAILED_ZYX);
	        
	        _log.debug("Ping google response code: " + httpConnection.getResponseCode());
	        
	        GroupConfigLocalServiceUtil.setGooglePing( _scopeGroupId );

		}
		catch(Exception e)
		{
			_log.error("Ping google error: " + e.toString());
			_log.trace("Ping google error: " + e);
		}
		finally
		{
			if (httpConnection != null)
				httpConnection.disconnect();
		}
	}
}
