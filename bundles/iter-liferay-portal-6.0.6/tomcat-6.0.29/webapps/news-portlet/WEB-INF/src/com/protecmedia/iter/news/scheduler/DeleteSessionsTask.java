package com.protecmedia.iter.news.scheduler;

import java.util.Calendar;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.scheduler.Task;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.news.service.ProductLocalServiceUtil;

public class DeleteSessionsTask extends Task
{
	private static Log _log = LogFactoryUtil.getLog(DeleteSessionsTask.class);

	private static final int DEFAULT_DELETE_FREQUENCY_DAYS = 1;
	
	public DeleteSessionsTask(String name, long delay, ThreadGroup tasks)
	{
		super(name, delay, tasks);
	}
	
	@Override
	public void doWork()
	{
		try
		{
			if(getDelay() > 0)
				ProductLocalServiceUtil.deletePaywallSessions();
			
			calculateDelay();
		}
		catch (Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
	}
	
	@Override
	public void doReconfig()
	{
		_log.trace("Reconfigured " + this.getName() + " - Delay: " + getDelay() + " millis");
	}
	
	@Override
	public void doDead()
	{
		_log.trace("Killed " + this.getName());
	}
	
	public void calculateDelay()
	{
		Calendar calendar = Calendar.getInstance();
		
		long currentTime = calendar.getTimeInMillis();
		
		configCalendar(calendar);
		
		long nextTime = calendar.getTimeInMillis();
		long delay = nextTime - currentTime;
		if(delay < 0)
			delay = 0;
		
		reconfigDelay(delay);
	}
	
	public void configCalendar(Calendar calendar)
	{
		int hour = 0;
		int min = 0;
		try
		{
			String scheduleMeasure = PortalUtil.getPortalProperties().getProperty(
					IterKeys.PORTAL_PROPERTIES_KEY_ITER_PAYWALL_DATACLEANUP_SCHEDULE);
			
			String data[] = scheduleMeasure.split(StringPool.PERIOD);
			hour = Integer.parseInt(data[0]);
			min = Integer.parseInt(data[1]);
		}
		catch(Exception e)
		{
			_log.trace(e);
		}
		
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, min);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		int frecuencyDays = GetterUtil.getInteger(PortalUtil.getPortalProperties().getProperty(
				IterKeys.PORTAL_PROPERTIES_KEY_ITER_PAYWALL_DATACLEANUP_FREQUENCY), DEFAULT_DELETE_FREQUENCY_DAYS);
		
		Calendar currentCalendar = Calendar.getInstance();

		if((getDelay() > 0) || currentCalendar.after(calendar))
			calendar.add(Calendar.DAY_OF_MONTH, frecuencyDays);
	}
	
	public void reconfigDelay(long delay)
	{
		setDelay(delay);
		interrupt(RECONFIG);
	}
}