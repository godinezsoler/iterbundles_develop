package com.protecmedia.iter.base.service.thread;

public class ThreadTools
{
	public static String getThreadTrace()
	{
		return "TotalThreads: "+ getCurrentRunningThreads() +" - ThreadId: " + Long.toString(Thread.currentThread().getId());
	}
	
	public static int getCurrentRunningThreads()
	{
		ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
		ThreadGroup parentGroup;
		while ( ( parentGroup = rootGroup.getParent() ) != null ) 
		{
		    rootGroup = parentGroup;
		}

		return rootGroup.activeCount();
	}
}
