package com.protecmedia.iter.news.servlet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.servlet.EndPointServlet;
import com.protecmedia.iter.base.servlet.MethodContext;


public class EndPoint extends EndPointServlet  
{
	private static Log _log 					= LogFactoryUtil.getLog(EndPoint.class);
	private static final long serialVersionUID 	= 2740619817625051633L;
	
	public EndPoint() 
	{
		super();
	}

//	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
//	{
//		try
//		{
//			//response.setContentType("text/plain");
//		    PrintWriter out = response.getWriter();
//		    out.println("com.protecmedia.iter.news.servlet.InterfaceServlet.doGet");
//		    out.flush();
//		}
//		catch(Exception err)
//		{
//			_log.error("Cannot render System Status");
//		}
//	}
//	
//	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
//	{
//		try
//		{
//			Document postDom 	  = SAXReaderUtil.read(request.getInputStream());
//			Element invokeElem	  = (Element)postDom.selectSingleNode("*/invoke");
//			
//			// Obtiene el contexto del método: cuál es su clase, método y parámetros
//			MethodContext ctx = ServletTools.buildMethodContext(invokeElem);
//			
//			// Invoka dicho método
//			Object methodResult	  = invokeMethod(ctx);
//			
//			// Guarda a respuesta en el buffer de salida
//			ServletTools.responseResult( response, methodResult);
//		}
//		catch (Exception err)
//		{
//			_log.error(err);
//			ServletTools.responseError( response, err );
//		}
//	}	

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
