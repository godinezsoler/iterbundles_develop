package com.protecmedia.iter.advertisement.util;

import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterFriendlyURLMapper;

public class AdvertisementFriendlyURLMapper extends IterFriendlyURLMapper {

	public AdvertisementFriendlyURLMapper()
	{
		super();
		
		setMapping( PortalUtil.getNewsMappingPrefix() );
		setCheckMappingWithPrefix( PortalUtil.getCheckMappingWithPrefix() );
	}

}
