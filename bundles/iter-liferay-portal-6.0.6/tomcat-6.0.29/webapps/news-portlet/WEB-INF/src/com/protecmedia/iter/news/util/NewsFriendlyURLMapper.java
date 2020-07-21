package com.protecmedia.iter.news.util;

import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterFriendlyURLMapper;

public class NewsFriendlyURLMapper extends IterFriendlyURLMapper 
{
	public NewsFriendlyURLMapper() 
	{
		super();
		
		setMapping( PortalUtil.getNewsMappingPrefix() );
		setCheckMappingWithPrefix( PortalUtil.getCheckMappingWithPrefix() );
	}
}