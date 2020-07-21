package com.protecmedia.iter.base.servlet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.commons.lang.ClassUtils;

import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.ServiceError;

public class EndPoint extends EndPointServlet 
{
	private static Log _log 					= LogFactoryUtil.getLog(EndPoint.class);
	private static final long serialVersionUID 	= 2740619817625051634L;
	
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
        
        if (_log.isDebugEnabled())
        	_log.debug( String.format("%s.%s: Begin", ctx.getClassName(), methodName) );
        
        Object obj = null;
        try
        {
	        for (int i = 0; i < methods.length; i++)
	        {
	           if ( methods[i].getName().equals(methodName) )
	           {
	        	   // Mismo nombre de método, se comprueba si el método coincide con el número de parámetros
	        	   Class<?>[] paramList = methods[i].getParameterTypes();
	        	   if (paramList.length == ctx.getMethodParams().length)
	        	   {
	        		   boolean match = true;
	        		   for (int iParam = 0; iParam < paramList.length ; iParam++)
	        		   {
	        			   Class<?> srcClass = ctx.getMethodParams()[iParam].getClass();
	        			   Class<?> dstClass = paramList[iParam];
	        			   
	        			   if (!(	// Las clases son iguales
	        					   	dstClass.equals(srcClass) || 
	        					   // Es un wrapper de una primitiva(Boolean -> boolean | Long -> long)
	        					   (dstClass.isPrimitive() && ClassUtils.primitiveToWrapper(dstClass).equals(srcClass)) ||
	        					   // Es una interfaz y una clase que implementa dicha interfaz (RequestFacade -> HttpServletRequest)
	        					   (dstClass.isInterface() && Arrays.asList(srcClass.getInterfaces()).contains(dstClass)) ))
	        			   {
	        				   match = false;
	        				   break;
	        			   }
	        		   }
	        		   
	        		   if (match)
	        		   {
	    	              m = methods[i];
	    	              break;
	        		   }
	        	   }
	           }
	        }
	        
	        ErrorRaiser.throwIfNull(m, IterErrorKeys.XYZ_E_METHOD_NOT_FOUND_ZYX);
	        obj = m.invoke(o, ctx.getMethodParams());
        }
        finally
        {
            if (_log.isDebugEnabled())
            	_log.debug( String.format("%s.%s: End", ctx.getClassName(), methodName) );
        }
        return obj;
	}
}
