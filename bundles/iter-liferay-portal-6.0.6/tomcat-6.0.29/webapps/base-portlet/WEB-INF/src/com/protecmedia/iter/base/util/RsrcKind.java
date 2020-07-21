package com.protecmedia.iter.base.util;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

public enum RsrcKind
{
	theme,
	renderer,
	ctxvar,
	system,
	undefined;
	
	public static RsrcKind getKind(Object obj)
	{
		RsrcKind kind = RsrcKind.undefined;
		
		if (obj != null)
		{
			if (obj instanceof RsrcKind)
				kind = (RsrcKind) obj;
			else
			{
				try
				{
					kind = RsrcKind.valueOf( obj.toString().toLowerCase() );
				}
				catch (Exception e)
				{
					_log.error( e.toString() );
					_log.debug( e );
				}
			}
		}
		return kind;
	}
	
	@Override
	public String toString()
	{
		return name();
	}
	
	private static Log _log = LogFactoryUtil.getLog(RsrcKind.class);
}
