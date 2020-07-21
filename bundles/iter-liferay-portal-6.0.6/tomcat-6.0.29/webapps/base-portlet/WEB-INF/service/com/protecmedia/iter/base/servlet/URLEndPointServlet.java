package com.protecmedia.iter.base.servlet;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.json.JSONException;

import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.xml.DocumentException;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;

public abstract class URLEndPointServlet extends IterServlet implements Servlet 
{
	private static final long serialVersionUID 	= 2740619817625051644L;
	
	public URLEndPointServlet() 
	{
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		try
		{
			super.doGet(request, response);
			
			doWork(request, response);
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
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		try
		{
			super.doPost(request, response);
			
			doWork(request, response);
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
	private void doWork(HttpServletRequest request, HttpServletResponse response) throws IOException, ServiceError, IllegalArgumentException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, JSONException, DocumentException, TransformerException
	{
		String stillAlive = ParamUtil.get(request, "stillAlive", null);
		if ( stillAlive != null )	// simplemente mantener la sesión viva
			responseError(response,  new Throwable());
		
		String className	= ParamUtil.get(request, "clsid", 		"");
		String methodName	= ParamUtil.get(request, "methodName", 	"");
		//String instanceid	= ParamUtil.get(request, "instanceID", 	"");
		String releaseSess	= ParamUtil.get(request, "releaseSess", "");
		String callMode		= ParamUtil.get(request, "callMode", 	"");
		String avoidCache	= ParamUtil.get(request, "avoidCache", 	"");
		boolean isStatic 	= !ParamUtil.get(request, "dispid", "0").equals("0");
		
		ErrorRaiser.throwIfFalse( !className.isEmpty(),  IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse( !methodName.isEmpty(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		
		boolean isPostMethod = request.getMethod().equals("POST"); 
		int	args 			 = request.getParameterMap().entrySet().size();
		
		int firstArgToQS = 4;
		if ( !releaseSess.isEmpty() )	firstArgToQS++;
		if ( !callMode.isEmpty() )		firstArgToQS++;
		if ( !avoidCache.isEmpty() )
		{
			firstArgToQS++;
			avoidCache(response);
		}

		List<Object> params  = new ArrayList<Object>();
		if (callMode.isEmpty())
		{
			// params.add( getServerVariables(request) );
			// params.add( SAXReaderUtil.createDocument() );
			params.add( request );
			params.add( response );
		}
		
		if (isPostMethod)
			params.add( request.getInputStream() );
		
		args -= firstArgToQS;
		for ( int i = 1; i <= args; i++ )
		{
			params.add( request.getParameter("p"+i) );
		}
		
		MethodContext ctx = new MethodContext(className, methodName, params.toArray(), isStatic);
		// Invoka dicho método
		Object methodResult	  = invokeMethod(ctx);
		
		// Si es null la respuesta la habrá gestionado el método recién invocado
		if (methodResult != null)
		{
			// Guarda a respuesta en el buffer de salida
			responseResult( response, methodResult);
		}
		
//		if (callMode.isEmpty())
//		{
//			Document domResponse = (Document)params.get(1);
//			
//			Node nodeRedirect = domResponse.selectSingleNode("response/redirect");
//			String redirect   = (nodeRedirect == null) ? "" : nodeRedirect.getText();
//			
//			if (redirect.isEmpty())
//			{
//				//setResponseFormXml( response, domResponse, instanceid );
//				// setResponseFormXml( xmlResponse, instanceid == "null" ? null : instanceid );
//			}
//			else
//			{
//				response.sendRedirect(redirect);
//			}
//		}
//		else if (callMode.equals("XMLRET") && !instanceid.equals("null"))
//		{
//			
////			System.Xml.XmlDocument xmlRet = new System.Xml.XmlDocument();
////			if ( result.GetType().IsArray )
////				xmlRet.Load( ((byte[])result ).ToString() );
////			else
////				xmlRet.LoadXml( result.ToString() );
////			
////			System.Xml.XmlElement retElem = xmlRet.DocumentElement;
////			retElem.SetAttribute( "instance", instanceid );
////
////			xmlRet.Save( Response.OutputStream );
////			xmlRet = null;
//		}
//		else
//		{
//			if (methodResult instanceof Object[])
//				response.getOutputStream().write( (byte[]) methodResult );
//			else
//				response.getWriter().write( (String)methodResult );
//		}
	}
//	///////////////////////////////////////////////////////////////////////////////////////////////////////////////
//		private void setResponseFormXml( HttpServletResponse response, Document domResponse, String instanceID )
//	{
//		int index = 0;
//		List<Node> listHeaders = domResponse.selectNodes("response/headers");
//		for (index = 0; index < listHeaders.size(); index++)
//		{
//			response.addHeader( (String)listHeaders.get(index).selectObject("LocalName/text()"), listHeaders.get(index).getText() );
//		}
//		
//		if (!instanceID.isEmpty())
//			response.addHeader( "instanceID", instanceID );
//		
//		List<Node> listProperties = domResponse.selectNodes("response/properties");
//		for (index = 0; index < listProperties.size(); index++)
//		{
//			String localName = (String)listProperties.get(index).selectObject("LocalName/text()");
//			String nodeValue = listProperties.get(index).getText();
//			
//			if (localName.isEmpty())
//				continue;
//			
//			if (localName.equals("ContentEncoding"))
//			{
//				response.setCharacterEncoding(nodeValue);
//			}
//			else if (localName.equals("ContentType"))
//			{
//				response.setContentType(nodeValue);
//			}
//			else if (localName.equals("Expires"))
//			{
//				response.setDateHeader("Expires", Long.valueOf(nodeValue));
//			}
//			else if (localName.equals("Status"))
//			{
//				response.setStatus( Long.valueOf(nodeValue).intValue() );
//			}
//		}
//	}
//	
//	private Document getServerVariables(HttpServletRequest request)
//	{
//		Document retDom		= SAXReaderUtil.createDocument();
//		Element httpRpc 	= retDom.addElement("request");
//		
//		//	AUTH_TYPE: Indica el método de autentificación que utiliza el servidor para validar a un usuario
//		Element elemAuthType = httpRpc.addElement("AUTH_TYPE");
//		elemAuthType.addText( request.getAuthType() );		
//		
//		//	CONTENT_TYPE: Tipos de dato del contenido
//		Element elemContentType = httpRpc.addElement("CONTENT_TYPE");
//		elemContentType.addText( request.getContentType() );
//			
//		//	LOGON_USER: Cuenta de Windows NT con la que se ha loginado el usuario
//		request.get	
//		//	QUERY_STRING: Cadena que sigue al signo interrogante (?) en la petición HTTP
//		//	
//		//	REMOTE_ADDR: Dirección IP del equipo remoto que realiza la petición al servidor
//		String remoteAddr = request.getRemoteAddr();
//	
//		//	REMOTE_HOST: Nombre del Host que realiza la petición
//		request.getRemoteHost();
//		
//		//	
//		//	REQUEST_METHOD: Método utilizado en la petición (GET, HEAD, POST)
//		//	
//		//	SCRIPT_MAP: Prefijo de la URL anterior a la pagina
//		//	
//		//	SERVER_NAME: Nombre o IP del servidor
//		String serverName = request.getServerName();
//		
//		
//		ret
//		return retDom;
//		
//		
//		string _return = "";
//		System.Xml.XmlDocument oXml = new System.Xml.XmlDocument();
//		System.Xml.XmlElement oElem = null;
//		oXml.LoadXml("<request/>");
//		foreach (string sv in Request.ServerVariables)
//		{
//			if (Request.ServerVariables[sv]!="" && Request.ServerVariables[sv]!=null)
//			{
//				oElem = oXml.CreateElement(sv);
//				oElem.InnerText = Request.ServerVariables[sv];
//				oXml.DocumentElement.AppendChild(oElem);
//			}
//		}
//		_return = oXml.InnerXml;
//		oXml = null;
//		oElem = null;
//		return _return;
//	}
	
}




















