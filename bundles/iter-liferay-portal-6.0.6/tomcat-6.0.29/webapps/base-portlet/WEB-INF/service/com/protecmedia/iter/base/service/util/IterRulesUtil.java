package com.protecmedia.iter.base.service.util;


public class IterRulesUtil
{	
	/**
	 * <p>Llama al servlet {@code IterRulesServlet} en todos los Apaches empezando por
	 * el que se encuentra en modo AJP.</p>
	 * 
	 * <p>Si alguna petición falla, registra el error en el log y el Monitor y continua
	 * con el siguiente Apache.</p>
	 */
	public static void regenerateApacheIterRules(long groupId)
	{
		com.liferay.portal.util.IterRulesUtil.regenerateApacheIterRules(groupId);
	}
}
