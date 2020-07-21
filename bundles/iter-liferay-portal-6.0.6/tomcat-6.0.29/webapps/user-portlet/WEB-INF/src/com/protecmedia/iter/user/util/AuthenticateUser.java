package com.protecmedia.iter.user.util;

import java.util.List;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceError;

public class AuthenticateUser
{
	private static Log _log = LogFactoryUtil.getLog(AuthenticateUser.class);

	private static final String AUTH_RESP_TEMPLATE = "<?xml version=\"1.0\"?><itwresponse version=\"1.0\"><field name=\"code\"><string>OK</string></field><field name=\"output\"><string>%s</string></field></itwresponse>";
	
	public static JSONObject doAuthentication(String friendlyGroupURL, String token, String validator, String unexpectedError) throws ServiceError, DocumentException
	{
		ErrorRaiser.throwIfNull(token, IterErrorKeys.XYZ_ITR_E_AUTHENTICATE_TOKEN_IS_NULL_ZYX);
		
		String data = "";
		
		friendlyGroupURL = friendlyGroupURL.replace("/", ".");
		String url = PropsUtil.get( String.format(IterKeys.PORTAL_PROPERTIES_KEY_ITER_AUTHENTICATION_URL, friendlyGroupURL) );
		
		// Modo autónomo
		if ("self".equals(url))
		{
			data = String.format(AUTH_RESP_TEMPLATE, token);
		}
		// Modo sistema externo
		else
		{
			String urlParameters = new StringBuilder("accesstoken=").append(token).append("&extravalidator=").append(validator).toString();
			int connTimeout = GetterUtil.getInteger(PropsUtil.get( String.format(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SUBSCRIPTION_CONN_TIMEOUT, friendlyGroupURL) ), 15000);
			int readTimeout = GetterUtil.getInteger(PropsUtil.get( String.format(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SUBSCRIPTION_READ_TIMEOUT, friendlyGroupURL) ), 15000);
			
			if(Validator.isNotNull(url))
			{
				data = LoginUtil.connectServer(url, urlParameters, connTimeout, readTimeout, unexpectedError);
			}
			else
			{
				_log.debug("Not found \"iter.authentication.server.url" + friendlyGroupURL + "\" in portal-ext.properties");
			}
		}

		return getAuthenticationId(data);
	}
	
	private static JSONObject getAuthenticationId(String authentication) throws DocumentException
	{
		JSONObject authenticationObj = JSONFactoryUtil.createJSONObject();
		
		if( Validator.isNotNull(authentication) )
		{
			Document xmlDoc = SAXReaderUtil.read(authentication);
			
			Node node = null;
			
			List<Node> nodes = xmlDoc.selectNodes("/itwresponse/field[@name='code']/string");
			if(nodes != null && nodes.size() == 1)
			{
				node = nodes.get(0);
				String code = node.getStringValue();
				if(code.toUpperCase().equals("OK"))
				{
					authenticationObj.put(UserKeys.SUBSCRIPTION_SYSTEM_RESPONSE_CODE, IterKeys.OK);
					
					nodes = xmlDoc.selectNodes("/itwresponse/field[@name='output']/string");
					if(nodes != null && nodes.size() == 1)
						authenticationObj.put(UserKeys.SUSCRIPTOR_ID, nodes.get(0).getStringValue());
				}
				else if(code.toUpperCase().equals(IterKeys.KO))
				{
					authenticationObj.put(UserKeys.SUBSCRIPTION_SYSTEM_RESPONSE_CODE, IterKeys.KO);
					
					nodes = xmlDoc.selectNodes("/itwresponse/field[@name='msg']/string");
					String msg = "";
					if(nodes != null && nodes.size() == 1)
						msg = nodes.get(0).getStringValue();
					
					authenticationObj.put(UserKeys.SUBSCRIPTION_SYSTEM_MSG, msg);
					authenticationObj.put(UserKeys.SUSCRIPTOR_ID, StringPool.NULL);
				}
				else if(code.toUpperCase().equals(UserKeys.ITER_KO))
				{
					authenticationObj.put(UserKeys.SUBSCRIPTION_SYSTEM_RESPONSE_CODE, UserKeys.ITER_KO);
					
					nodes = xmlDoc.selectNodes("/itwresponse/field[@name='msg']/string");
					String msg = "";
					if(nodes != null && nodes.size() == 1)
						msg = nodes.get(0).getStringValue();
					
					authenticationObj.put(UserKeys.SUBSCRIPTION_SYSTEM_MSG, msg);
					authenticationObj.put(UserKeys.HTTP_STATUS_CODE, 
							GetterUtil.getString( XMLHelper.getTextValueOf(xmlDoc, "/itwresponse/field[@name='httpcode']/string"), StringPool.BLANK) );
					authenticationObj.put(UserKeys.HTTP_STATUS_LINE, 
							GetterUtil.getString( XMLHelper.getTextValueOf(xmlDoc, "/itwresponse/field[@name='httpmessage']/string"), StringPool.BLANK) );
					authenticationObj.put(UserKeys.SUSCRIPTOR_ID, StringPool.NULL);
				}
			}
		}
		
		return authenticationObj;
	}
}
