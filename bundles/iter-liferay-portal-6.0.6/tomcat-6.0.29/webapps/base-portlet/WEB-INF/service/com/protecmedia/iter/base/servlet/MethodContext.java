package com.protecmedia.iter.base.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerException;

import org.json.JSONObject;

import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.ServiceError;

public class MethodContext 
{
	private String 		m_className 	= "";
	private String 		m_methodName 	= "";
	private Object[] 	m_methodParams 	= null;
	private boolean		m_isStatic		= false;
	private String 		m_env 			= WebKeys.ENVIRONMENT_PREVIEW;
	
	public MethodContext(HttpServletRequest request) throws ServiceError, DocumentException, IOException, org.json.JSONException, TransformerException
	{
		init(request);
	}
	public MethodContext(String className, String methodName, Object[] methodParams, boolean isStatic) 
	{
		init(className, methodName, methodParams, isStatic, WebKeys.ENVIRONMENT_PREVIEW);
	}
	private void init(HttpServletRequest request) throws ServiceError, DocumentException, IOException, org.json.JSONException, TransformerException
	{
		String className = null, methodName = null, env = WebKeys.ENVIRONMENT_PREVIEW;
		boolean isStatic = false;
		List<Object> methodParams = null;
		
		
		if (IterServlet.isJSON())
		{
			String data = StreamUtil.toString(request.getInputStream(), StringPool.UTF8);
			
			JSONObject json = new org.json.JSONObject( data ).getJSONObject("http-rpc");
			ErrorRaiser.throwIfNull(json, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			json = json.getJSONObject("invoke");
			ErrorRaiser.throwIfNull(json, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			className = json.getString("clsid");
			ErrorRaiser.throwIfFalse( Validator.isNotNull(className), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			methodName = json.getString("methodName");
			ErrorRaiser.throwIfFalse( Validator.isNotNull(methodName), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			isStatic = json.getInt("dispid") == 0;
			methodParams = IterServlet.jsonUnMarshalRequest( request, json.getJSONArray("params") );
			
			if (json.has("environment"))
				env = json.getString("environment");
		}
		else
		{
			Document postDom 	  = SAXReaderUtil.read(request.getInputStream());
			Element invokeElem	  = (Element)postDom.selectSingleNode("*/invoke");
	
			className = invokeElem.attributeValue("clsid", "");
			ErrorRaiser.throwIfFalse(!className.isEmpty(), "INVALID PARAMETER");
			
			methodName = invokeElem.attributeValue( "methodName", "" );
			ErrorRaiser.throwIfFalse(!methodName.isEmpty(), "INVALID PARAMETER");
			
			isStatic = !invokeElem.attributeValue("dispid", "0").equals("0");
					
			methodParams = IterServlet.xmlUnMarshalRequest( request, invokeElem );
		}
				
		init(className, methodName, methodParams.toArray(), isStatic, env);
	}
	private void init(String className, String methodName, Object[] methodParams, boolean isStatic, String env)
	{
		m_className 	= className;
		m_methodName	= methodName;
		m_methodParams 	= methodParams;
		m_isStatic		= isStatic;
		m_env 			= env;
	}

	public String getClassName()
	{
		return m_className;
	}
	public String getMethodName()
	{
		return m_methodName;
	}
	public Object[] getMethodParams()
	{
		return m_methodParams;
	}
	public boolean isStatic()
	{
		return m_isStatic;
	}
	
	public boolean isPreviewEnvironment()
	{
		return m_env.equalsIgnoreCase(WebKeys.ENVIRONMENT_PREVIEW);
	}
}
