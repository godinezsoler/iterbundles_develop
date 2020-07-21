package com.protecmedia.iter.base.service.scheduler;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

public class Task extends Thread
{
	private static Log _log = LogFactoryUtil.getLog(Task.class);
	
	public static final int DEAD 		= 0;
	public static final int WORK 		= 1;
	public static final int RECONFIG 	= 2;
	
	private boolean _mustDie 			= false;
	private long 	_delay	 			= 86400000;
	
	public Task(String name, long delay, ThreadGroup tasks)
	{
		super(tasks, name);
		setDelay(delay);
	}
	
	@Override
	public void run()
	{
		while(!mustDie())
		{			
			if(waitForTimeout())
			{
				doWork();
			}
			else
			{
				if(!mustDie())
					doReconfig();
			}
		}
		
		doDead();
	}
	
	private boolean waitForTimeout()
	{
		boolean status = true;
		
		try
		{
			_log.trace("Sleeping " + this.getName() + " for " + getDelay() + " millis");
			sleep(getDelay());
		}
		catch(InterruptedException e)
		{
			status = false;
		}
		
		return status;
	}
	
	public void interrupt(int status)
	{
		if(!mustDie())
		{
			if(status == DEAD)
				setMustDie(true);
			else if(status == RECONFIG)
				setMustDie(false);

			if(!isInterrupted())
				interrupt();
		}
	}

	public synchronized long getDelay()
	{
		return _delay;
	}

	public synchronized void setDelay(long delay)
	{
		_delay = delay;
	}

	public synchronized boolean mustDie()
	{
		return _mustDie;
	}

	public synchronized void setMustDie(boolean mustDie)
	{
		this._mustDie = mustDie;
	}

	public void doDead(){}
	
	public void doWork(){}
	
	public void doReconfig(){}
}