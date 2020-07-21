package com.protecmedia.iter.base.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;

public abstract class IterServlet extends HttpServlet 
{
	private static final long serialVersionUID 	= 2740619817625051111L;
	private static final String VT_NUM  		= "3";
	private static final String VT_BSTR 		= "8";
	private static final String VT_BOOL 		= "11";

	
	public IterServlet()
	{
		super();
	}
	
	public static boolean isJSON()
	{
		boolean isJSON = false;
		try
		{
			isJSON = ContentTypes.APPLICATION_JSON.equals(IterRequest.getOriginalRequest().getContentType()) ||
			         ContentTypes.APPLICATION_JSON.equals(IterRequest.getOriginalRequest().getHeader("Accept"));
		}
		catch (Exception e){}
		
		return isJSON;
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		IterRequest.setOriginalRequest(request);
	}
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		IterRequest.setOriginalRequest(request);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	abstract protected Log getLog();
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Object invokeMethod(MethodContext ctx)
	//
	// Instancia un objecto de la clase "className" e invoca el método "methodName" con una los parámetros "methodParams"
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	abstract protected Object invokeMethod(MethodContext ctx) throws 
							ClassNotFoundException, InstantiationException, IllegalAccessException, 
							ServiceError, IllegalArgumentException, InvocationTargetException;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// static private void responseResult( HttpServletResponse response, Object methodResult)
	//
	// Guarda en el buffer de salida el resultado de ejecutar el método
	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void responseResult( HttpServletResponse response, Object methodResult) throws IOException, JSONException, DocumentException, TransformerException
	{
		ServletOutputStream out = response.getOutputStream();
		
		String responseData = ".";
		if (IterServlet.isJSON())
		{
			responseData = jsonMarshalResponse(methodResult).toString();
			response.setContentType(ContentTypes.APPLICATION_JSON);
		}
		else
		{
			responseData = xmlMarshalResponse(methodResult).asXML();
			response.setContentType(ContentTypes.TEXT_XML);
		}
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.setCharacterEncoding("UTF-8");
		response.setStatus(HttpServletResponse.SC_OK);
		
		getLog().debug(responseData);				
		
		out.write(responseData.getBytes());
		out.flush();
	}

//	private static String getErrorCode(Throwable errorSource)
//	{
//		if(errorSource!=null)
//		{
//			return errorSource.toString().replaceAll(IterErrorKeys.ITER_ERROR_REGEX, "$1");
//		}
//		return "";
//	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// static private void responseError( HttpServletResponse response, Throwable e ) throws IOException
	//
	// Guarda en el buffer de salida un error
	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void responseError( HttpServletResponse response, Throwable e ) throws IOException
	{
		try
		{
			ServletOutputStream out = response.getOutputStream();
			String errorMsg 		= ServiceErrorUtil.getServiceErrorAsXml(e);
			
			if (IterServlet.isJSON())
			{
				// http://jira.protecmedia.com:8080/browse/ITER-1094?focusedCommentId=51917&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-51917
				// El JSON de repuesta en caso de error mantendrá los saltos de línea.
				errorMsg = xml2JSON2( SAXReaderUtil.read(errorMsg) ).toString();
				response.setContentType(ContentTypes.APPLICATION_JSON);
			}
			else
			{
				response.setContentType(ContentTypes.TEXT_XML);
			}
			
			response.setCharacterEncoding("UTF-8");
			
			getLog().error(errorMsg);
			
			out.write( errorMsg.getBytes(StringPool.UTF8) );
			out.flush();
		}
		catch (Throwable th)
		{
			getLog().error(th);
		}
	}
	
	private JSONObject xml2JSON(Document dom) throws JSONException, UnsupportedEncodingException, DocumentException, TransformerException
	{
		return new org.json.JSONObject(JSONUtil.toJSONString(dom));
	}
	
	private com.google.gson.JsonObject xml2JSON2(Document dom) throws JSONException, UnsupportedEncodingException, DocumentException, TransformerException
	{
		return (new com.google.gson.JsonParser().parse(JSONUtil.toJSONStringWithBreakLines(dom))).getAsJsonObject();
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private com.google.gson.JsonObject jsonMarshalResponse(Object comRet) throws JSONException, UnsupportedEncodingException, DocumentException, TransformerException
	{
		com.google.gson.JsonObject httpRPC = new com.google.gson.JsonObject();
		com.google.gson.JsonObject response = new com.google.gson.JsonObject();
		
		if ( comRet != null )
		{
			if (comRet instanceof Document)
			{
				// Retorna un DOM
				response.add("param", xml2JSON2((Document) comRet));
			}
			else if ( !(comRet instanceof Integer || comRet instanceof Double || comRet instanceof Boolean || comRet instanceof String) )
			{
				if (comRet instanceof com.google.gson.JsonObject)
				{
					response.add("param", (com.google.gson.JsonObject)comRet);
				}
				else
				{
					// Retorna un objeto tipo List<>, [], Map
					response.add("param", xml2JSON2(XMLHelper.encodeBean(comRet)));
				}
			}
			else if (comRet instanceof Integer || comRet instanceof Double)
			{
				// Números
				response.addProperty("param", comRet instanceof Integer ? ((Integer) comRet) : ((Double) comRet));
			}
			else if (comRet instanceof Boolean)
			{
				response.addProperty("param", (Boolean) comRet);
			}
			else
			{
				try
				{
					// Es un String.  La mayoría de las respuestas de tipo cadena son XML y los XMLs se transforman en JSON
					response.add("param", xml2JSON2(SAXReaderUtil.read((String)comRet)));
				}
				catch (Exception e)
				{
					getLog().debug(e);
					response.addProperty("param", comRet.toString());
				}
			}
		}		

//		httpRPC.put( "response", response );
		httpRPC.add( "response", response );
		return httpRPC;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	static private Document xmlMarshalResponse( Object comRet )
	{
		Document retDom		= SAXReaderUtil.createDocument();
		Element httpRpc 	= retDom.addElement("http-rpc");
		Element response	= httpRpc.addElement("response");
		Element paramList	= response.addElement("params");
		Element param		= paramList.addElement("param");
		
		param.addAttribute("index", "-1");
		if ( comRet != null )
		{
			String paramStr 	= "";
			boolean isString 	= true;
			
			// No es un número ni una cadena, se serializa
			if (comRet instanceof Document)
			{
				paramStr = ((Document) comRet).asXML();
			}
			else if ( !(comRet instanceof Integer || comRet instanceof Double || comRet instanceof String) )
			{
				paramStr = XMLHelper.encodeBean(comRet).asXML();
			}
			else if (comRet instanceof Integer || comRet instanceof Double)
			{
				paramStr = String.valueOf(comRet);
				isString = false;
			}
			else
			{
				paramStr = ((String)comRet);
			}
			
			if (isString)
			{
				param.addAttribute("vt", VT_BSTR);
			//	paramStr = paramStr.replaceAll("]]>", "[[<");	// para poder enviar XML con CDATA en el parámetro HTTP-RPC
			}
			else
				param.addAttribute("vt", VT_NUM);

			param.addText(paramStr);
		}		
		
		return retDom;
	}

	static public List<Object> jsonUnMarshalRequest( HttpServletRequest request, JSONArray params ) throws DocumentException, JSONException, UnsupportedEncodingException, TransformerException
	{
		List<Object> comParams = new ArrayList<Object>();
		
		for (int i = 0; params != null && i < params.length(); i++)
		{
			Object param = params.get(i);
			
			if (param instanceof JSONObject)
			{
				comParams.add( XMLHelper.toString((JSONObject) param) );
			}
			else if (param instanceof String && ((String)param).equals("[includeRequest]"))
				comParams.add(request);
			else if (param instanceof Integer)
				comParams.add( ((Integer)param).longValue() );
			else if (param instanceof Boolean)
				comParams.add( ((Boolean)param).booleanValue() );
			else	
				comParams.add(param);
		}
		
		return comParams;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	static public List<Object> xmlUnMarshalRequest(HttpServletRequest request, Element invoke )
	{
		List<Node> paramsList = invoke.selectNodes( "params/param" );
		int paramCount = paramsList.size();
		
		List<Object> comParams = new ArrayList<Object>(paramCount);
		for (int i = 0; i < paramCount; i++)
			comParams.add( new Object() );
		
		if (paramCount > 0)
		{
            Element param;
            String paramVT, paramStr;
            int paramIndex;
            
            for (int i = 0; i < paramCount; i++)
            {
                param 		= (Element)paramsList.get(i);
                paramVT 	= param.attributeValue("vt");
                paramIndex 	= Integer.parseInt( param.attributeValue("index") );

                if (paramVT.equals(VT_BSTR))
                {
                    paramStr = param.getText().replaceAll("\\[\\[<", "\\]\\]>");
                    
                    if (paramStr.equals("[includeRequest]"))
                    	comParams.set( paramIndex, request);
                    else
                    	comParams.set( paramIndex, paramStr);		// para poder enviar XML con CDATA en el parámetro HTTP-RPC
                }
                else if (paramVT.equals(VT_BOOL))
                	comParams.set( paramIndex, param.getText().equalsIgnoreCase("true") );
                else
                    comParams.set( paramIndex, Long.parseLong(param.getText()) );
            }
		}

		return comParams;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	static public void avoidCache(HttpServletResponse response)	{}
}
