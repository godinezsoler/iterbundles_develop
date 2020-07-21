package com.protecmedia.iter.xmlio.util;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.xmlio.service.ChannelLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.util.CacheRefresh;

public class RefreshCacheThread extends Thread 
{
	private static Log _log = LogFactoryUtil.getLog(RefreshCacheThread.class);
	
	private CacheRefresh _cacheRefresh;
	
	public RefreshCacheThread(CacheRefresh cacheRefresh)
	{
		this._cacheRefresh = cacheRefresh;
	}
	
	public void run() 
	{ 
		try 
		{
			ChannelLocalServiceUtil.refreshCacheSynchronously(_cacheRefresh);
		} 
		catch (Exception e) 
		{
			_log.error(e);
		}
	}
}
