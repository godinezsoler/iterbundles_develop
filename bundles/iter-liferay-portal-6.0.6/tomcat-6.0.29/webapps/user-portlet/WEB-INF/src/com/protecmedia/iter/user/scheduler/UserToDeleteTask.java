package com.protecmedia.iter.user.scheduler;

import java.io.IOException;
import java.sql.SQLException;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.base.service.scheduler.Task;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.user.service.IterRegisterServiceUtil;

public class UserToDeleteTask extends Task
{
	private static Log _log = LogFactoryUtil.getLog(UserToDeleteTask.class);

	public UserToDeleteTask(String name, long delay, ThreadGroup tasks) throws ServiceError, IOException, SQLException
	{
		super(name, delay, tasks);
		_log.trace( String.format("Creating %s with delay %d", this.getName(), delay) );
	}
	
	@Override
	public void doWork()
	{
		try
		{
			IterRegisterServiceUtil.deleteExpiredUsers();
			long delay = IterRegisterServiceUtil.getDelayToDeleteExpiredUsers();
			if(delay > 0)
			{
				_log.trace("Reconfiguring " + this.getName() + " - Delay: " + delay + " millis");
				reconfigDelay(delay);
			}
			else
			{
				_log.trace("Killing " + this.getName());
				interrupt(DEAD);
			}
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
	
	public void reconfigDelay(long delay) throws ServiceError, IOException, SQLException
	{
		setDelay(delay);
		interrupt(RECONFIG);
	}
}