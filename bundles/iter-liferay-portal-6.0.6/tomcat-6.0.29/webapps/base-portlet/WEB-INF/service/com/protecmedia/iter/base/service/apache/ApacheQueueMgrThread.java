package com.protecmedia.iter.base.service.apache;

import java.net.MalformedURLException;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

public class ApacheQueueMgrThread extends Thread 
{
	final private static Log 	_log 			= LogFactoryUtil.getLog(ApacheQueueMgrThread.class);
	final public ApacheQueueMgr	apacheQueueMgr;
	
	public ApacheQueueMgrThread(ApacheQueueMgr apacheQueueMgr) throws MalformedURLException
	{
		super();
		this.apacheQueueMgr = apacheQueueMgr;
	}

	@Override
	public void run()
	{
		do
		{
			try
			{
				apacheQueueMgr.doRequestQueue();
			}
			catch(Exception e)
			{
				_log.error(e.toString());
			}
		}
		while (ApacheQueueMgr.isActive() && !this.isInterrupted());
	}
}
