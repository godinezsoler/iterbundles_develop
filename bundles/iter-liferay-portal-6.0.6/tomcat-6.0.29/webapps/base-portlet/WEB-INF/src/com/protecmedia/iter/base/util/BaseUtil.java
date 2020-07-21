/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.base.util;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PrefsPropsUtil;

public class BaseUtil 
{
	private static Log _log = LogFactoryUtil.getLog(BaseUtil.class);
	
	/**
	 * 
	 * @return devuelve la ip del servidor de Base de Datos
	 */	
	public static String getDataBaseIP() throws Exception{
		return getDataBaseIP(
				GetterUtil.getString(PrefsPropsUtil.getString("jdbc.default.url")), 
				GetterUtil.getString(PrefsPropsUtil.getString("jdbc.default.driverClassName")));
	}
	
	/**
	 * 
	 * @return devuelve la ip del servidor de Base de Datos
	 */	
	public static String getDataBaseIP(String jdbDefaultURL, String jdbDefaultDriverClassName) throws Exception{
		String ip = "";

		if (jdbDefaultDriverClassName.equals("com.mysql.jdbc.Driver")) {
			int start = 13;
			jdbDefaultURL = jdbDefaultURL.substring(start);
			
			ip = jdbDefaultURL.substring(0, jdbDefaultURL.indexOf("/"));
		} else if (jdbDefaultDriverClassName.equals("oracle.jdbc.driver.OracleDriver")) {
			int start = 18;
			jdbDefaultURL = jdbDefaultURL.substring(start);
			
			ip = jdbDefaultURL.substring(0, jdbDefaultURL.indexOf(":"));
		}
				
		return ip;
	}
}
