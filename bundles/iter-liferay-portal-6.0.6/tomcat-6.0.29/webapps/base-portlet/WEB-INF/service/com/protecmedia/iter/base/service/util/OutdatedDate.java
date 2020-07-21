package com.protecmedia.iter.base.service.util;

import java.util.Date;

import com.liferay.portal.kernel.configuration.Filter;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsUtil;

public class OutdatedDate extends TimeOffset 
{
	public OutdatedDate(Date date, int interval)
	{
		super(	date, 
				0-GetterUtil.getInteger(PropsUtil.get(IterKeys.PORTAL_PROPERTIES_KEY_ITER_QUERY_OUTDATED_VALUE,	new Filter(String.valueOf(interval))), 1	), 
				GetterUtil.getString(  	PropsUtil.get(IterKeys.PORTAL_PROPERTIES_KEY_ITER_QUERY_OUTDATED_UNIT,	new Filter(String.valueOf(interval))), "day")	);
	}
}
