package com.protecmedia.iter.tracking.util;

import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterFriendlyURLMapper;

public class TrackingFriendlyURLMapper extends IterFriendlyURLMapper {

	public TrackingFriendlyURLMapper() 
	{
		super();
		
		setMapping( PortalUtil.getNewsMappingPrefix() );
		setCheckMappingWithPrefix( PortalUtil.getCheckMappingWithPrefix() );
	}

}