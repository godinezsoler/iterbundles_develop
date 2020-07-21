package com.protecmedia.disqus.util;

import java.util.Properties;

import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterFriendlyURLMapper;
import com.protecmedia.iter.base.service.util.IterKeys;

public class DisqusFriendlyURLMapper extends IterFriendlyURLMapper
{
	public DisqusFriendlyURLMapper()
	{
		super();
		
		setMapping( PortalUtil.getNewsMappingPrefix() );
		setCheckMappingWithPrefix( PortalUtil.getCheckMappingWithPrefix() );
	}
}
