package com.protecmedia.iter.news.util;

import java.util.HashMap;
import java.util.Map;

import com.liferay.portal.theme.ThemeDisplay;

/**
 * 
 * Esta clase permite almacenar el themeDisplay de una instancia de portlet para
 * poder ser utilizado en vistas que no se cargan en el contexto del Layout (servlets o JSPs) y
 * que por tanto no tienen acceso a este objeto
 * 
 * El uso está limitado a casos muy particulares, como cargas por ajax en los teasers paginados, para
 * no penalizar la memoria
 * 
 * No se usa ThreadLocal porque precisamente se requiere compartir esta variable entre hilos. Por este
 * motivo sólo puede usarse con portlets instanciables cuyo Id sea unico
 * 
 */
public class ThemeDisplayCacheUtil {

	public static Map<String, ThemeDisplay> requestMap =
		new HashMap<String, ThemeDisplay>();
	
	public static void putPortletRequest(String portletId, ThemeDisplay themeDisplay){
		
		ThemeDisplayCacheUtil.requestMap.put(portletId, themeDisplay);
	}
	
	public static ThemeDisplay getPortletRequest(String portletId){
		
		return ThemeDisplayCacheUtil.requestMap.get(portletId);
		
	}
	
}
