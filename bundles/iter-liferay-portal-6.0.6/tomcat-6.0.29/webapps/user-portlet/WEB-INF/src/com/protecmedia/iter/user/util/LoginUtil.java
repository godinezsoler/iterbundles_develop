/**
 * Copyright (c) Protecmedia All rights reserved.
 */

package com.protecmedia.iter.user.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Digester;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LayoutSetTools;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.service.GroupServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;

public class LoginUtil
{
	private static Log _log = LogFactoryUtil.getLog(LoginUtil.class);

	public static final String POPUP_MODE = "popup"; 
	public static final String LINK_MODE = "link";
	public static final String GOTO_REFERER = "gotoReferer";
	
	public static final String GET_LOGIN_CONF = "SELECT loginconf FROM Base_Communities WHERE groupId = %d";
	public static final String SQL_GROUP_BY_ID = "SELECT friendlyURL FROM Group_ WHERE groupId = '%s'";
	
	private Document loginPreferences = null;
	
	public void getLoginConfig(long groupId)
	{ 
		try
		{
			Document d = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_LOGIN_CONF, groupId), false, "rs", "row"  );
			Node loginconf = d.selectSingleNode("//row/loginconf");
			if( loginconf!=null && !loginconf.getText().isEmpty() )
			{
				loginPreferences = SAXReaderUtil.read(loginconf.getText());
			}
			else
				_log.error("Error loading login preferences for group " + groupId);
		}
		catch(Exception e)
		{
			_log.error(e);
		}
	}
	
	public String getValueOfLoginPreference(String preferenceName)
	{
		String preferenceValue = "";
		
		if( loginPreferences!=null )
		{
			Node n = loginPreferences.selectSingleNode("//preference[name='" + preferenceName + "']/value");

			if( n!= null )
				preferenceValue = n.getStringValue();
		}
		
		return preferenceValue;
	}

	public static String getFirstStringRowFromQuery(String queryFormat)
	{
		List<Object> results = PortalLocalServiceUtil.executeQueryAsList(queryFormat);
		String resultsData = null;
		if(results != null && results.size() > 0 && results.get(0)!=null )
		{
			resultsData = results.get(0).toString();
		}
		return resultsData;
	}
	
	public static String connectServer (String url, String urlParameters, int connTimeout, int readTimeout, String unexpectedError )
	{
		HttpURLConnection httpConnection = null;
		StringBuilder sb = new StringBuilder();
		Document xmlDoc = null;
		
		try
		{
			httpConnection = (HttpURLConnection)new URL(url).openConnection();
		    httpConnection.setRequestMethod("POST"); 
		    httpConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
		    httpConnection.setRequestProperty("charset", Digester.ENCODING);
		    httpConnection.setDoOutput(true);
		    httpConnection.setDoInput(true);
		    httpConnection.setConnectTimeout(connTimeout);
		    httpConnection.setReadTimeout(readTimeout);
		    
		    DataOutputStream out = new DataOutputStream(httpConnection.getOutputStream());
	        out.writeBytes(urlParameters);
	        out.flush();
	        out.close();
	        
	        BufferedReader in = null;
	
			if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK)
			{
				xmlDoc = SAXReaderUtil.read( httpConnection.getInputStream() );
		        String xPathQuery = "//field[@name='code']/string";
		        XPath xpath = SAXReaderUtil.createXPath(xPathQuery);
		        List<Node> nodes = xpath.selectNodes(xmlDoc);
		        
				if(nodes == null || nodes.size() != 1)
				{
					xmlDoc = koResponseXml(unexpectedError, httpConnection.getResponseCode(), httpConnection.getResponseMessage());
				}
			}
	        else
	        {
	        	if(_log.isDebugEnabled())
	        	{
	        		in = new BufferedReader(new InputStreamReader(httpConnection.getErrorStream()));
	        		String currentLine = "";
	    	        while ((currentLine = in.readLine()) != null)
	    	        	sb.append(currentLine);
	    	        
	        		_log.debug(sb.toString());
	        		sb.delete(0, sb.length()-1);
	        	}
	        	
	        	_log.error( "Unable to connect with the remote system : " + url + "  " + httpConnection.getResponseCode() + "  " + httpConnection.getResponseMessage() );
	        	xmlDoc = koResponseXml(unexpectedError, httpConnection.getResponseCode(), httpConnection.getResponseMessage());
	        }
		}
		catch (Exception e)
		{
			_log.error(e);
			try
			{
				xmlDoc = koResponseXml(unexpectedError, httpConnection.getResponseCode(), httpConnection.getResponseMessage());
			}
			catch(Exception e2)
			{
				xmlDoc = koResponseXml(unexpectedError, HttpURLConnection.HTTP_INTERNAL_ERROR, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
			}
		}
		finally
		{
			if(httpConnection!=null)
				httpConnection.disconnect();
		}
		
		return xmlDoc.asXML();
	}
	
	private static Document koResponseXml(String unexpectedError, int responseCode, String responsemessage)
	{
		Document d = SAXReaderUtil.createDocument();
		Element root = d.addElement("itwresponse");
		root.addAttribute( "version" , "1.0" );
		
		Element row = root.addElement("field");
		row.addAttribute( "name" , "code" );
		row.addElement("string").addText("ITER_KO");
		
		row = root.addElement("field");
		row.addAttribute( "name" , "msg" );
		row.addElement("string").addText( unexpectedError );
		
		row = root.addElement("field");
		row.addAttribute( "name" , "httpcode" );
		row.addElement("string").addText( String.valueOf(responseCode) );
		
		row = root.addElement("field");
		row.addAttribute( "name" , "httpmessage" );
		row.addElement("string").addText( responsemessage );
		
		return d;
	}
	
	public String getAvatarURLHTML(long scopeGroupId)
	{
		String result = "";
		try
		{
			boolean useAvatar = GetterUtil.getBoolean(getValueOfLoginPreference("useavatar"));
			if(useAvatar)
			{
				String groupName = GroupServiceUtil.getGroup(scopeGroupId).getFriendlyURL().replace(StringPool.SLASH, StringPool.BLANK);
				
				String avatarDefault = PortalUtil.getPortalProperties().getProperty(
						String.format(IterKeys.PORTAL_PROPERTIES_KEY_ITER_USER_NO_AVATAR_IMAGE, groupName));
				
				// El default de getProperty no contempla la cadena vacía, solo el null
				if (Validator.isNull(avatarDefault))
					avatarDefault = "/user-portlet/img/noavatar.png";

				String host = LayoutSetTools.getStaticServerName(scopeGroupId, avatarDefault);
				
				if (avatarDefault.startsWith("/"))
					avatarDefault = host.concat( avatarDefault );
				
				result = String.format(new StringBuilder(
				"<?php if( isset($avatar) && trim($avatar) != '' )							\n").append(
				"{ 																			\n").append(
				"   if (substr($avatar, 0, 1) === '/')										\n").append(
				"	{																		\n").append(
				"		$avatar = \"%s\".$avatar;												\n").append(
				"	}																		\n").append(
				"?>																			\n").append(
				"	<img alt=\"<?php echo $usrname ?>\" src=\"<?php echo $avatar ?>\"></img>\n").append(
				"<?php																		\n").append(
				"}																			\n").append(
				"else																		\n").append(
				"{ ?>																		\n").append(
				"	<img alt=\"<?php echo $usrname ?>\" src=\"%s\"></img>					\n").append(
				"<?php 																		\n").append(
				"} ?>																		\n").toString(), host, avatarDefault);
				_log.debug(result);
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return result;
	}

}