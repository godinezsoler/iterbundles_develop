package com.protecmedia.iter.base.service.util;

import java.util.HashMap;
import java.util.Map;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.DefaultFriendlyURLMapper;
import com.liferay.portal.kernel.portlet.LiferayPortletURL;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.util.PortalUtil;

public class IterFriendlyURLMapper extends DefaultFriendlyURLMapper {

	private static Log _log = LogFactoryUtil.getLog(IterFriendlyURLMapper.class);
	private static final String ITER_MAPPING_REGEX	= "(//*)([\\w|\\s|\\S]+)(//*)";
	
	private boolean _checkMappingWithPrefix = true;
	
	protected void setCheckMappingWithPrefix(boolean check) {
		_checkMappingWithPrefix = check;
	}
	
	@Override
	public boolean isCheckMappingWithPrefix(){
		return _checkMappingWithPrefix;
	}
	
	@Override
	public String buildPath(LiferayPortletURL liferayPortletURL) {
		Map<String, String> routeParameters = new HashMap<String, String>();

		buildRouteParameters(liferayPortletURL, routeParameters);

		String friendlyURLPath = router.parametersToUrl(routeParameters);

		if (friendlyURLPath == null) {
			return null;
		}

		addParametersIncludedInPath(liferayPortletURL, routeParameters);
		
		String mapping = getMapping();

		if (mapping!=null && !mapping.equals(""))
		{
			if (!mapping.startsWith(StringPool.SLASH))
				mapping = StringPool.SLASH.concat(mapping);
			
			friendlyURLPath = mapping.concat(friendlyURLPath);
		}

		return friendlyURLPath;
	}
	
	@Override
	public void populateParams(String friendlyURLPath, Map<String, String[]> parameterMap, Map<String, Object> requestContext) 
	{
		if (!getMapping().equals(""))	
			friendlyURLPath = friendlyURLPath.substring(getMapping().length() + 1);

		if (friendlyURLPath.endsWith(StringPool.SLASH))	
		{
			friendlyURLPath = friendlyURLPath.substring(0, friendlyURLPath.length() - 1);
		}

		Map<String, String> routeParameters = new HashMap<String, String>();

		if (!router.urlToParameters(friendlyURLPath, routeParameters)) 
		{
			if (_log.isWarnEnabled()) 
			{
				_log.warn("No route could be found to match URL " + friendlyURLPath);
			}

			return;
		}

		String portletId = getPortletId(routeParameters);

		if (portletId == null) 
		{
			return;
		}

		String namespace = PortalUtil.getPortletNamespace(portletId);

		addParameter(namespace, parameterMap, "p_p_id", portletId);

		populateParams(parameterMap, namespace, routeParameters);
	}
	
	@Override
	public void setMapping(String mapping) 
	{
		if (mapping!=null)
		{
			if ( mapping.matches(ITER_MAPPING_REGEX) )
			{
				mapping = mapping.replaceAll(ITER_MAPPING_REGEX, "$2");
			}
			super.setMapping(mapping);
		}
	}

}