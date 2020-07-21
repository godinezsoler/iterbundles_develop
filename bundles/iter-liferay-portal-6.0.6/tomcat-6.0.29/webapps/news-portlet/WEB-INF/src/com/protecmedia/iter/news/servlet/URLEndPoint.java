package com.protecmedia.iter.news.servlet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.servlet.URLEndPointServlet;
import com.protecmedia.iter.base.servlet.MethodContext;


public class URLEndPoint extends URLEndPointServlet 
{
	private static Log _log 					= LogFactoryUtil.getLog(URLEndPoint.class);
	private static final long serialVersionUID 	= 2740619817625051111L;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	protected Log getLog()
	{
		return _log;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Object invokeMethod(MethodContext ctx)
	//
	// Instancia un objecto de la clase "className" e invoca el método "methodName" con una los parámetros "methodParams"
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	protected Object invokeMethod(MethodContext ctx) throws 
							ClassNotFoundException, InstantiationException, IllegalAccessException, 
							ServiceError, IllegalArgumentException, InvocationTargetException
	{
		Class<?> comObject = Class.forName(ctx.getClassName());
		Object o = null;
		if (!ctx.isStatic()) 
			o = comObject.newInstance();
		
		String methodName = ctx.getMethodName();
		Method[] methods  = comObject.getDeclaredMethods();
        Method m = null;
        
        for (int i = 0; i < methods.length; i++)
        {
           if ( methods[i].getName().equals(methodName) )
           {
              m = methods[i];
              break;
           }
        }
        
        ErrorRaiser.throwIfNull(m, "METHOD NOT FOUND");
        return m.invoke(o, ctx.getMethodParams());
	}
}
