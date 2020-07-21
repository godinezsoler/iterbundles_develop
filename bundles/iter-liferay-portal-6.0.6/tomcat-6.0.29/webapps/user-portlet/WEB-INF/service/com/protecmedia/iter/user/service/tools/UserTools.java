package com.protecmedia.iter.user.service.tools;

import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.protecmedia.iter.user.service.LoginLocalServiceUtil;


public class UserTools 
{

	
	public static String getLoginCustomCSSClassName(long groupId, HttpServletRequest request) throws DocumentException, JSONException
	{
		String customCSSClassName = StringPool.BLANK;
		
		String css = getPreferenceValue(groupId, "portlet-setup-css", request);
		
		if (Validator.isNotNull(css)) 
		{
			
			JSONObject cssJSON = JSONFactoryUtil.createJSONObject(css);

			JSONObject advancedDataJSON = cssJSON.getJSONObject("advancedData");

			if (advancedDataJSON != null) {
				customCSSClassName = advancedDataJSON.getString(
					"customCSSClassName");
			}
		}
		
		return customCSSClassName;
	}
	
	public static String getLoginTitle(long groupId, HttpServletRequest request) throws DocumentException
	{
		String portletTitle = getPreferenceValue(groupId, "portlet-setup-title-es_ES", request);
		return portletTitle;
	}
	
	private static String getPreferenceValue(long groupId, String preferenceName, HttpServletRequest request)throws DocumentException
	{
		String preferenceValue = "";
		
		String loginPreferences = (String) request.getAttribute("loginPreferences" );
		
		if(loginPreferences == null)
		{
			loginPreferences = LoginLocalServiceUtil.getLoginPreferences(String.valueOf(groupId));
			request.setAttribute("loginPreferences", loginPreferences);
		}
			
		if( !loginPreferences.equalsIgnoreCase("<portlet-preferences />"))
		{	
			Document docLoginPreferences = SAXReaderUtil.read(loginPreferences);
		
			if( docLoginPreferences != null )
			{
				Node n = docLoginPreferences.selectSingleNode("//preference[name='" + preferenceName + "']/value");
	
				if( n!= null )
					preferenceValue = n.getStringValue();
			}
		}
		
		return preferenceValue;
	}

}
