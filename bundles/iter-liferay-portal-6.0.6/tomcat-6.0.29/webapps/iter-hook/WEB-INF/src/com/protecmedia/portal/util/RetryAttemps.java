package com.protecmedia.portal.util;

import java.io.Serializable;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.util.PortalUtil;

public class RetryAttemps 
{
	static	private final String	RETRYATTEMPS_ENABLE			= "iter.dlfileentry.retryattemps.enable";
	static	private final String	RETRYATTEMPS_MAXNUMBER		= "iter.dlfileentry.retryattemps.maxnumber";
	static	private final String	RETRYATTEMPS_TIME			= "iter.dlfileentry.retryattemps.time";
	
	static 	private final int 	 	DEFAULT_MAX_NUMBER			= 4;
	static 	private final int 		DEFAULT_SLEEP_TIME 			= 2000;
	static 	private final boolean 	DEFAULT_ENABLE				= false;

	static 	public  int 			maxNumber					= DEFAULT_MAX_NUMBER;
	static 	public  int 			sleepTime					= DEFAULT_SLEEP_TIME;
	static 	public  boolean 		enableRetry					= DEFAULT_ENABLE;
	
	static	private Log 			m_log 						= LogFactoryUtil.getLog(RetryAttemps.class);
	static 	private boolean 		m_initialized				= false;
			private int 			m_numRetryAttempts 			= 0;
			private ServiceContext	m_serviceContext			= null;
			
			
			
	public 	RetryAttemps(ServiceContext serviceContext)
	{
		initConfig();
		
		m_serviceContext = serviceContext;
		loadNumTimes();
	}
	static public void initConfig()
	{
		if (!m_initialized)
		{
			enableRetry = Boolean.parseBoolean(PortalUtil.getPortalProperties().getProperty(RETRYATTEMPS_ENABLE, String.valueOf(DEFAULT_ENABLE)));
			maxNumber 	= Integer.valueOf(PortalUtil.getPortalProperties().getProperty(RETRYATTEMPS_MAXNUMBER, String.valueOf(DEFAULT_MAX_NUMBER))).intValue();
			sleepTime 	= Integer.valueOf(PortalUtil.getPortalProperties().getProperty(RETRYATTEMPS_TIME, String.valueOf(DEFAULT_SLEEP_TIME))).intValue();
			
			m_initialized = true;
		}
	}
	/////////////////////////////////////////////////////////////////////////////////
	// Espera si es necesario
	/////////////////////////////////////////////////////////////////////////////////
	public void sleep()
	{
		if (enableRetry && m_numRetryAttempts > 0)
		{
			try 
			{
				Thread.sleep(sleepTime);
			} 
			catch (InterruptedException e) 
			{
				m_log.error(e);
			}
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////////
	// Determina si es necesario hacer un reintento
	/////////////////////////////////////////////////////////////////////////////////
	public boolean needRetryAttemps()
	{
		return (enableRetry && m_numRetryAttempts < maxNumber);
	}
	
	/////////////////////////////////////////////////////////////////////////////////
	// Devuelve el número de reintentos de la operación
	/////////////////////////////////////////////////////////////////////////////////
	private int loadNumTimes()
	{
		m_numRetryAttempts = 0;
		Serializable serNumTimes = m_serviceContext.getAttribute("numTimes");
		if (serNumTimes != null)
			m_numRetryAttempts = ((Integer)serNumTimes).intValue();

		return m_numRetryAttempts;
	}
	/////////////////////////////////////////////////////////////////////////////////
	// Guarda el número de reintentos de la operación
	/////////////////////////////////////////////////////////////////////////////////
	public void increaseNumTimes()
	{
		m_numRetryAttempts++;
		m_serviceContext.setAttribute("numTimes", m_numRetryAttempts);
	}

}
