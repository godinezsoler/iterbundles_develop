package com.protecmedia.iter.user.scheduler.networks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterKeys;

public class SocialStatisticsDisqusConfig extends SocialStatisticsConfig
{
	private String disqusIdentifierType;
	private String shortName;
	
	private static final String SELECT_DISQUS_CONFIG = "SELECT identifierType, shortName FROM disqusconfig WHERE groupid = '%s'";
	
	public SocialStatisticsDisqusConfig(String groupId)
	{
		super(groupId);
	}
	
	@Override
	public void init()
	{
		name = "Disqus";
		statistics_op = IterKeys.OPERATION_DISQUS;
		
		quota = GetterUtil.getString(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_DISQUS_QUOTA), StringPool.BLANK);
		
		delay = GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_DISQUS_DELAY), 120000);
		max_threads = GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_DISQUS_MAXTHREADS), 1);
		max_articles = GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_DISQUS_MAXARTICLES), 100);
		
		connectTimeout = GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_DISQUS_TIMEOUT_CONNECT), connectTimeout);
		readTimeout = GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SOCIALSTATISTIC_DISQUS_TIMEOUT_READ), readTimeout);
		
		response_count_field = "counts[].comments";
		
		try
		{
			Document dom = PortalLocalServiceUtil.executeQueryAsDom(String.format(SELECT_DISQUS_CONFIG, groupId));
			Element rootElement = dom.getRootElement();		
			Element row = rootElement.element("row");
			String identifierType = row.attributeValue("identifierType", null);
			String shortName = row.attributeValue("shortName", null);
			this.disqusIdentifierType = identifierType;
			this.shortName = shortName;
			api_url = new StringBuilder(Http.HTTPS_WITH_SLASH).append(shortName).append(".disqus.com/count-data.js?%s").toString();
		}
		catch (Throwable e)
		{
			// Config error. Usar test() para comprobar la correcta configuracion.
		}
	}
	
	public boolean test()
	{
		return (Validator.isNotNull(disqusIdentifierType) && Validator.isNotNull(shortName));
	}
	
	@Override
	protected String processResponseString(String in)
	{
		Pattern p = Pattern.compile("\\((\\{.*\\})\\)");
		Matcher m = p.matcher(in);
		if (m.find())
		{
			return m.group(1);
		}
		return in;
	}

	@Override
	public String extractURL(Node article)
	{
		String key = null;
		if(Validator.equals(disqusIdentifierType, IterKeys.DISQUS_IDENTIFIER_TYPE_URL))
		{
			key = new StringBuffer("2=").append(((Element) article).attributeValue("url")).toString();
		}
		else if(Validator.equals(disqusIdentifierType, IterKeys.DISQUS_IDENTIFIER_TYPE_ID))
		{
			key = new StringBuffer("1=").append("JA").append(((Element) article).attributeValue("contentId")).toString();
		}
		return key;
	}
}