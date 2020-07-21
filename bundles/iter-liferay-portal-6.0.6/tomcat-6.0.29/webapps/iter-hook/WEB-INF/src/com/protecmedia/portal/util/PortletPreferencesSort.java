package com.protecmedia.portal.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.service.PortletPreferencesLocalServiceUtil;


public class PortletPreferencesSort implements Runnable
{
	private static Log _log = LogFactoryUtil.getLog(PortletPreferencesSort.class);
	
	private SystemException _exception 	= null;
	private long 	  		_plid		= 0;

	public PortletPreferencesSort(long plid)
	{
		_plid = plid;
	}
	
	@Override
	public void run()
	{
		try
		{
			PortletPreferencesLocalServiceUtil.sortPreferencesByPlid(_plid);
		}
		catch (SystemException e)
		{
			_exception = e;
		}
	}
	
	static public void sort(long plid) throws SystemException, InterruptedException
	{
		PortletPreferencesSort portletPreferencesSort = new PortletPreferencesSort(plid);
		
		// Se lanza la tarea
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.submit(portletPreferencesSort);
		
		executorService.shutdown();
		
		// Se espera a que termine
		while (!executorService.awaitTermination(1000, TimeUnit.MILLISECONDS))
		{
			_log.warn( String.format("Waiting for sortPreferencesByPlid(%s)", plid) );
		}
		
		if (portletPreferencesSort._exception != null)
			throw portletPreferencesSort._exception;
	}

}
