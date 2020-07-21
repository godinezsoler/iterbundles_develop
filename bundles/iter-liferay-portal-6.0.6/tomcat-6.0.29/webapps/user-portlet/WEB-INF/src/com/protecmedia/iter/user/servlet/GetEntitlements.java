package com.protecmedia.iter.user.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.RaperHttpServletResponse;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.Digester;
import com.liferay.portal.kernel.util.EncryptUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.util.IterHttpClient;
import com.liferay.portal.util.IterMonitor;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.user.service.LoginLocalServiceUtil;
import com.protecmedia.iter.user.util.SSOLoginUtil;
import com.protecmedia.iter.user.util.UserKeys;

public class GetEntitlements extends HttpServlet implements Servlet
{
	private static final long serialVersionUID = 1L;
	private static Log _log = LogFactoryUtil.getLog(GetEntitlements.class);
	
	private final String USUARIO = "usr";
	private final String CONTRASEÑA = "pwd";
	private final String CODE_KO = "KO";
	private final String CODE_OK = "OK";
	
	private final String ELEM_PLIST = "plist";
	private final String ATTR_VERSION = "version";
	private final String ATTR_VERSION_VALUE = "1.0";
	private final String ELEM_DICT = "dict";
	private final String ELEM_KEY = "key";
	private final String ELEM_STRING = "string";
	private final String ELEM_ARRAY = "array";
	
	private final String KEY_CODE = "code";
	private final String KEY_MSG = "msg";
	private final String KEY_OUTPUT = "output";
	

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		if( GetterUtil.getBoolean( PropsUtil.get(PropsKeys.ITER_GET_ENTITLEMENTS_BY_GET), false) )
		{
			try
			{
				getEntitlements(request, response);
			}
			catch (IOException e)
			{
				_log.error(e);
			}
		}
		else
		{
			Document errorXML = createXML(CODE_KO, null, IterErrorKeys.XYZ_UNSUPPORTED_GET_METHOD_ZYX);
			
			try
			{
				addResponse(response, errorXML);
			}
			catch (IOException e)
			{
				_log.error(e);
			}
		}	
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		getEntitlements(request, response);
	}
	
	private void getEntitlements(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		Document retVal = null;
		long groupId = 0;
		try
		{
			// Recupera el payload original
			String payload = IOUtils.toString(request.getReader());
			Map<String, String> payloadParams = IterHttpClient.Util.deserializeQueryString(payload);
			
			String usr = payloadParams.get(USUARIO);
			String pwd = payloadParams.get(CONTRASEÑA);
			
			PortalUtil.setVirtualHostLayoutSet(request);
			
			groupId = PortalUtil.getScopeGroupId(request);
			String friendlyGroupURL = GroupLocalServiceUtil.getGroup(groupId).getFriendlyURL().replace(StringPool.SLASH, StringPool.PERIOD);
			String loginServerUrl = PropsUtil.get(String.format(PropsKeys.ITER_LOGIN_SERVER_URL, friendlyGroupURL));
			
			// Si hay un servidor externo configurado, solicita los datos del usuario al SSO para registrarlo o actualizarlo.
			SSOLoginUtil ssol = null;
			if (Validator.isNotNull(loginServerUrl))
			{
				ssol = new SSOLoginUtil(groupId, true);
				ssol.doRemoteSingleSingOn(loginServerUrl, payload, request);
			}
			
			RaperHttpServletResponse myResponse = new RaperHttpServletResponse(response);
			
			String warningMsg = LoginLocalServiceUtil.doLogin(usr, pwd, true, null, request, myResponse, ssol != null ? ssol.getPwdMd5() : null);
			
			ErrorRaiser.throwIfFalse( 
					GetterUtil.getString(
								(String)request.getAttribute(UserKeys.ATTR_SUBSCRIPTION_SYSTEM_RESPONSE_CODE), StringPool.BLANK
										)==IterKeys.OK,
							IterKeys.KO, warningMsg);
			
			String cookieData = String.valueOf( request.getAttribute("Set-Cookie-Data") );
			ErrorRaiser.throwIfFalse(Validator.isNotNull(cookieData), "XYZ_E_NOT_COOKIE_SETTED_ZYX");
			
			cookieData = EncryptUtil.decrypt(URLDecoder.decode(cookieData, Digester.ENCODING));
			
			retVal = getResult(cookieData, warningMsg);
			
			// Si se realizó un login mediante autoridad externa, añade la posible información adicional.
			if (ssol != null)
			{
				ssol.addExtraLoginData(retVal);
			}
		}
		catch (Exception e)
		{
			retVal = createXML( CODE_KO, null, e.getMessage() );
			IterMonitor.logEvent(groupId, IterMonitor.Event.ERROR, new Date(), "SSO Login process: Error getting entitlements", e);
		}
		
		addResponse(response, retVal);
	}

	private Document getResult( String cookieData, String message )
	{
		Document result = null;
		
		String data[] = cookieData.split("\\" + IterKeys.PARAM_SEPARATOR);
		
		String products = data[IterKeys.PRODUCTS_POS];
		
		if(Validator.isNotNull(products))
		{
			String DATE_FORMAT = "yyyyMMddHHmmss";
			SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
			
			try
			{
				String productsNames = "";
				String productsSplit[] = products.split(IterKeys.PRODUCT_SEPARATOR);
				long now = Calendar.getInstance().getTime().getTime();
				
				for(int i = 0; i < productsSplit.length; i++)
				{
					String currentProduct = productsSplit[i];
					String currentProductSplit[] = currentProduct.split(IterKeys.PRODUCT_DATE_SEPARATOR);
					String productDate = currentProductSplit[1];
					Date currentDate = sdf.parse(productDate);
					if(currentDate.getTime() > now)
						productsNames = StringUtil.add(productsNames, new String(Base64.decode(currentProductSplit[0]), Digester.ENCODING), IterKeys.PRODUCT_DATE_SEPARATOR);
				}
				
				result = createXML( CODE_OK, productsNames.substring(0, productsNames.lastIndexOf(IterKeys.PRODUCT_DATE_SEPARATOR)), message );
			}
			catch(Exception e)
			{
				_log.error("Bad products query string, right syntax: \"p1,20451025;p2,20111012\"");
				_log.trace(e);
			}
		}
		else
		{
			result = createXML( CODE_OK, null, message );
		}
		
		return result;
	}
	
	private Document createXML( String code, String xmlContent, String message )
	{
		Document xmlData = null;
		
		Element rootElement = SAXReaderUtil.createElement(ELEM_PLIST);
		rootElement.addAttribute(ATTR_VERSION, ATTR_VERSION_VALUE);
		
		Element elem_dict = SAXReaderUtil.createElement(ELEM_DICT);
		
		Element elem_code = SAXReaderUtil.createElement(ELEM_KEY);
		elem_code.setText( KEY_CODE );
		elem_dict.add(elem_code);
		
		Element elem_code_value = SAXReaderUtil.createElement(ELEM_STRING);
		elem_code_value.setText( code );
		elem_dict.add(elem_code_value);
		
		if( Validator.isNotNull(message) && !message.isEmpty() )
		{
			Element elem_msg = SAXReaderUtil.createElement(ELEM_KEY);
			elem_msg.setText( KEY_MSG );
			elem_dict.add(elem_msg);
			Element elem_msg_value = SAXReaderUtil.createElement(ELEM_STRING);
			
			try
			{
				message = new String(message.getBytes("UTF-8"));
			}
			catch(UnsupportedEncodingException uce)
			{
				_log.error(uce.toString());
			}
			
			elem_msg_value.setText( message );
			elem_dict.add(elem_msg_value);
		}
		
		if( code.equals(CODE_OK) )
		{
			Element elem_output = SAXReaderUtil.createElement(ELEM_KEY);
			elem_output.setText( KEY_OUTPUT );
			elem_dict.add( elem_output );
			
			Element elem_array = SAXReaderUtil.createElement(ELEM_ARRAY);
			if( Validator.isNotNull(xmlContent) && !xmlContent.isEmpty() )
			{
				for(String productName : xmlContent.split(IterKeys.PRODUCT_DATE_SEPARATOR))
				{
					Element elem_prodName = SAXReaderUtil.createElement(ELEM_STRING);
					elem_prodName.setText(productName);
					elem_array.add( elem_prodName );
				}
			}
			
			elem_dict.add( elem_array );
		}
		
		rootElement.add( elem_dict );
		
		xmlData = SAXReaderUtil.createDocument(rootElement);
		xmlData.addDocType(ELEM_PLIST, "-//Apple Computer//DTD PLIST 1.0//EN", "http://www.apple.com/DTDs/PropertyList-1.0.dtd");
		
		return xmlData;
	}
	
	private void addResponse( HttpServletResponse response, Document retDom ) throws IOException
	{
		response.setContentType("text/xml");
		ServletOutputStream out = response.getOutputStream();
		out.write( retDom.asXML().getBytes() );
		out.flush();
	}
}
