package com.protecmedia.iter.user.scheduler.networks;

import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterKeys;

public class SocialStatisticsTwitterConfig extends SocialStatisticsConfig
{
	public SocialStatisticsTwitterConfig(String groupId)
	{
		super(groupId);
	}

	@Override
	public void init()
	{
		name = "Twitter";
		statistics_op = IterKeys.OPERATION_TW;

		quota = GetterUtil.getString(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_TWITTER_QUOTA), StringPool.BLANK);
		
		delay = GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_TWITTER_DELAY), 120000);
		max_threads = GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_TWITTER_MAXTHREADS), 50);
		max_articles = GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_TWITTER_MAXARTICLES), 1);
		
		connectTimeout = GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_TWITTER_TIMEOUT_CONNECT), connectTimeout);
		readTimeout = GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_TWITTER_TIMEOUT_READ), readTimeout);
		
		api_url = "http://opensharecount.com/count.json?url=%s";
		response_count_field = "count";
	}
}