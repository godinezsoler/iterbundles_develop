package com.protecmedia.iter.search.util;

import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterFriendlyURLMapper;

public class SearchFriendlyURLMapper extends IterFriendlyURLMapper {

	public SearchFriendlyURLMapper() 
	{
		super();
		
		setMapping( PortalUtil.getSearchMappingPrefix() );
		setCheckMappingWithPrefix( PortalUtil.getSearchCheckMappingWithPrefix() );
	}
}
