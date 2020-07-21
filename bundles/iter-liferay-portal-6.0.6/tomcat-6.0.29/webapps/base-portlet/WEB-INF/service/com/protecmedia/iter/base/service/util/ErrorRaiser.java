package com.protecmedia.iter.base.service.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ErrorRaiser extends ServiceErrorUtil 
{
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static void throwIfError(String errCode) throws ServiceError
	{
		throwIfError(errCode, "");
	}
	public static void throwIfError(String errCode, String errMsg) throws ServiceError
	{
		if (errCode != null && !errCode.isEmpty())
		{
			throwError(errCode, errMsg);
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static void throwIfFalse(Boolean condition) throws ServiceError
	{
		throwIfFalse(condition, "", "");
	}
	public static void throwIfFalse(Boolean condition, String errCode) throws ServiceError
	{
		throwIfFalse(condition, errCode, "");
	}
	public static void throwIfFalse(Boolean condition, String errCode, String errMsg) throws ServiceError
	{
		if (!condition)
		{
			throwError(errCode, errMsg);
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static void throwIfNull(Object obj) throws ServiceError
	{
		throwIfNull(obj, "", "");
	}
	public static void throwIfNull(Object obj, String errCode) throws ServiceError
	{
		throwIfNull(obj, errCode, "");
	}
	public static void throwIfNull(Object obj, String errCode, String errMsg) throws ServiceError
	{
		if (obj == null)
		{
			throwError(errCode, errMsg);
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static ServiceError buildError(String errCode, String errMsg, StackTraceElement[] stackTraces)
	{
		// Se crea la excepción
		ServiceError e = new ServiceError(errCode, errMsg);
		
		// Se elimina del stack las entradas de esta clase, no son importantes
		if (stackTraces == null)
		{
			e.fillInStackTrace();
			List<StackTraceElement> stack = new ArrayList<StackTraceElement>(Arrays.asList(e.getStackTrace()));
			
			String refClassName = stack.get(0).getClassName();
			while (stack.get(0).getClassName() == refClassName)
			{
				stack.remove(0);
			}
			e.setStackTrace(stack.toArray(new StackTraceElement[stack.size()]));
		}
		else
		{
			e.setStackTrace( stackTraces );
		}
		
		return e;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private static void throwError(String errCode, String errMsg) throws ServiceError
	{
		// Se lanza el error
		throw buildError(errCode, errMsg, null);
	}
}
