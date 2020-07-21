package com.protecmedia.iter.base.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.xml.DocumentException;


public abstract class EndPointServlet extends IterServlet implements Servlet  
{
	private static final long serialVersionUID 	= 2740619817625051633L;
	
	public EndPointServlet() 
	{
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		try
		{
			super.doGet(request, response);
						
			//response.setContentType("text/plain");
		    PrintWriter out = response.getWriter();
		    out.println( "Not implemented" );
		    out.flush();
		}
		catch(Throwable err)
		{
			getLog().error("Cannot render System Status");
			responseError( response, err );
		}
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		try
		{
			super.doPost(request, response);
			
			doInternalPost(request, response);
		}
		catch(InvocationTargetException ite)
		{
			 Throwable th = ite.getTargetException();
			 getLog().error(th);
			 responseError( response, th );
		}
		catch (Throwable err)
		{
			getLog().error(err);
			responseError( response, err );
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	protected void doInternalPost(HttpServletRequest request, HttpServletResponse response) throws 
						DocumentException, IOException, ServiceError, IllegalArgumentException, 
						ClassNotFoundException, InstantiationException, IllegalAccessException, 
						InvocationTargetException, JSONException, org.json.JSONException, TransformerException
	{
		// Obtiene el contexto del método: cuál es su clase, método y parámetros
		MethodContext ctx = new MethodContext(request);
		
		// Invoka dicho método
		Object methodResult	  = invokeMethod(ctx);
		
		// Guarda a respuesta en el buffer de salida
		responseResult( response, methodResult);
	}
	
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
	    //The following are CORS headers. Max age informs the 
	    //browser to keep the results of this call for 1 day.
	    resp.setHeader("Access-Control-Allow-Origin", "*");
	    resp.setHeader("Access-Control-Allow-Methods", "GET, POST");
	    resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
	    resp.setHeader("Access-Control-Max-Age", "86400");
	    //Tell the browser what requests we allow.
	    resp.setHeader("Allow", "GET, HEAD, POST, TRACE, OPTIONS");
	}
	
//	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
//	{
//		String instanceID	= "";
//		String releaseAfter	= "";
//		Class<?> comObject	= null;
//
//		try
//		{
//			Document postDom = SAXReaderUtil.read(request.getInputStream());
//			
//			String clsID = "";
//			Element invokeElem = (Element)postDom.selectSingleNode("*/invoke");
//			if (invokeElem == null )
//			{
//				invokeElem = (Element)postDom.selectSingleNode("*/release");
//				if (invokeElem == null )
//					throw new SystemException("INVALID PARAMETER");
//			}
//			else
//			{
//				clsID = invokeElem.attributeValue("clsid", "");
//				if (clsID.isEmpty())
//					throw new SystemException("INVALID PARAMETER");
//				else
//					releaseAfter = invokeElem.attributeValue( "releaseAfter" );
//			}
//			instanceID = invokeElem.attributeValue("instance", "");
//			
//			if ( clsID.isEmpty() )	// llaman sólo para liberar objeto previamete guardado en sesión
//			{
//				if ( !instanceID.isEmpty() )
//				{
//					// webAdapter.deleteCOMObject( Session[ instanceID ] );
//					// Session[ instanceID ] = null;
//				}
//			}
//			else
//			{
//				
//				ServletTools.invokeMethod(response, invokeElem, comObject, clsID);
//				
//                if ( instanceID.isEmpty() || !releaseAfter.isEmpty() )	// gestión del objeto en sesión
//				{
//					//webAdapter.deleteCOMObject( comObject );	// no había que coservarlo o había que liberarlo después de la llamada
//					comObject = null;
//					//if ( !instanceID.isEmpty() )
//						//Session[ instanceID ] = null;
//				}
//				//else											// hay que guardarlo en sesión
//					//Session[ instanceID ] = comObject;
//
//			}
//		}
//		catch(Exception err)
//		{
//			_log.error("Cannot render System Status");
//		}
//	}
}
