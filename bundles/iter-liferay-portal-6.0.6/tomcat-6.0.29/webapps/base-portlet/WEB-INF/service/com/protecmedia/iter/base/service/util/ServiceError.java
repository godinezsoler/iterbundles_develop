package com.protecmedia.iter.base.service.util;

public class ServiceError extends com.liferay.portal.kernel.error.ServiceError
{
	private static final long serialVersionUID = 1L;
	
	public ServiceError()
	{
		super();
	}
	public ServiceError(String errorCode, String errorMsg)
	{
		super(errorCode, errorMsg);
	}
	public ServiceError(com.liferay.portal.kernel.error.ServiceError serviceError)
	{
		super(serviceError.getErrorCode(), serviceError.getMessage());
		setStackTrace( serviceError.getStackTrace() );
	}
}
