package com.protecmedia.iter.base.scheduler;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.base.service.NewsletterMgrLocalServiceUtil;
import com.protecmedia.iter.base.service.scheduler.Task;

public class NewsletterTask extends Task
{
	private static Log _log = LogFactoryUtil.getLog(NewsletterTask.class);

	public NewsletterTask(String name, long delay, ThreadGroup tasks)
	{
		super(name, delay, tasks);
	}
	
	@Override
	public void doWork()
	{
		try
		{
			NewsletterMgrLocalServiceUtil.sendNewsletterAndSchedule(getName());
		}
		catch (Exception e)
		{
			_log.error(e);
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
	
	public void reconfigDelay(long delay)
	{
		setDelay(delay);
		interrupt(RECONFIG);
	}
}