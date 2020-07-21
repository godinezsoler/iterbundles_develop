package com.protecmedia.iter.base.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.service.PortalLocalServiceUtil;

public class OrphanResourceThread extends Thread
{
	private static Log _log = LogFactoryUtil.getLog(OrphanResourceThread.class);
	
	private static final String THEME_UPDATE_ORPHANDATE 	= new StringBuilder(
		"UPDATE Theme_WebResource SET orphandate = NOW()\n").append(
		"	WHERE orphandate IS NOT NULL				\n").append(
		"		AND rsrcid = '%s'						\n").toString();
		
	private static final String RENDERER_UPDATE_ORPHANDATE 	= new StringBuilder(
		"UPDATE RendererRsrc SET orphandate = NOW()	\n").append(
		"	WHERE orphandate IS NOT NULL			\n").append(
		"		AND rsrcid = '%s'					\n").toString();
	
	private static final Map<RsrcKind,String> UPDATE_ORPHANDATE;
	static
	{
		 Map<RsrcKind, String> map = new HashMap<RsrcKind, String>();
		 
		 map.put(RsrcKind.theme, 	THEME_UPDATE_ORPHANDATE); 
		 map.put(RsrcKind.renderer, RENDERER_UPDATE_ORPHANDATE); 
		 UPDATE_ORPHANDATE = Collections.unmodifiableMap(map);
	}
	
	private String 		_rsrcId = null;
	private RsrcKind	_kind 	= null;
	
	public OrphanResourceThread(String rsrcId, RsrcKind kind)
	{
		_rsrcId = rsrcId;
		_kind = kind;
	}
	
	@Override
	public void run()
	{
		if (!PropsValues.IS_PREVIEW_ENVIRONMENT && UPDATE_ORPHANDATE.containsKey(_kind))
		{
			try
			{
				if (_log.isDebugEnabled())
					_log.debug( String.format("Updating orphan date for resource %s (%s)", _rsrcId, _kind.toString()) );
				
				String sql = String.format(UPDATE_ORPHANDATE.get(_kind), _rsrcId);
				_log.trace(sql);
				
				PortalLocalServiceUtil.executeUpdateQuery(sql);
			}
			catch (Exception e)
			{
				_log.error(e);
			}
		}
	}
}
