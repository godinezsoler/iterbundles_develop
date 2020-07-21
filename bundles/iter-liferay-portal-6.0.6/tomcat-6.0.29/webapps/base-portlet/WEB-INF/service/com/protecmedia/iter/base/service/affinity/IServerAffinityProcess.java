package com.protecmedia.iter.base.service.affinity;

import com.liferay.portal.kernel.error.ServiceError;

public interface IServerAffinityProcess
{
	public void start() throws ServiceError;
	public void halt();
}
