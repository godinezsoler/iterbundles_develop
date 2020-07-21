package com.protecmedia.iter.base.service.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import javax.portlet.PortletPreferences;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PHPUtil;
import com.liferay.portal.kernel.util.PortletViewMgr;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.PortletPreferencesLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portlet.PortletPreferencesFactoryUtil;
import com.liferay.portlet.journalcontent.util.JournalContentUtil;
import com.protecmedia.iter.base.service.CommunitiesLocalServiceUtil;


public class PortletMgr 
{
	
	private static Log _log = LogFactoryUtil.getLog(PortletMgr.class);
	
	public static String getPortletPreferences(HttpServletRequest request, String xmlInitParams) throws Exception
	{
		return PortletPreferencesTools.getPreferencesAsXML(request, xmlInitParams);
	}
	
	public static String setPortletPreferences(long plid, String portletResource, String xmlPreferences) throws Exception
	{
		Document dom = SAXReaderUtil.read(xmlPreferences);
		
		PortletPreferencesLocalServiceUtil.updatePreferences(PortletKeys.PREFS_OWNER_ID_DEFAULT,PortletKeys.PREFS_OWNER_TYPE_LAYOUT, 
				plid, portletResource, dom.asXML());
		
		javax.portlet.PortletPreferences preferences = PortletPreferencesLocalServiceUtil.getPreferences(GroupMgr.getCompanyId(), PortletKeys.PREFS_OWNER_ID_DEFAULT, PortletKeys.PREFS_OWNER_TYPE_LAYOUT, plid, portletResource);
		
		// 05/11/2014
		// Al modificar las preferencias de un portlet se borra la caché para que se refresquen las variables que se inyectan en velocity
		JournalContentUtil.clearCache();
		
		return PortletPreferencesLocalServiceUtil.toXML(preferences);
	}
	
	public static final List<String> ACCEPT_LANGUAGE = Arrays.asList(new String[]
	{
		"de","ca","fr","en","it","pl","pt","es"  			
	});
	
	private static String getLanguageFilePath(String root, String lang)
	{
		String filePath = Validator.isNotNull(lang) 									? 
							String.format("%1$s%2$smcm%2$sMCM_U_portlets%3$s%4$s.TXT", 
											root, File.separatorChar, 
											lang.equals("es") ? "" : "-",
											lang.equals("es") ? "" : lang		
											): "not_supported.txt";
		return filePath;
	}
	public static Object getRawProperty(HttpServletRequest request, HttpServletResponse response, String propertyName) throws IOException
	{
		if (propertyName.equals("MCM"))
		{
			int status	  	= HttpServletResponse.SC_NOT_FOUND;
			String result 	= "/mcm/MCM_U_portlets.TXT";
			String root		= new File(PortalUtil.getPortalWebDir()).getParentFile().getAbsolutePath();
			
			String lang 	= null;
			// Example: es-ES,es;q=0.9,en;q=0.8
			// To order: http://www.java2s.com/Code/Java/Collections-Data-Structure/UseCollectionssorttosortcustomclassanduserdefinedComparator.htm
			String   accept_Languages= GetterUtil.getString( request.getHeader("Accept-Language"), "es" );
			String[] acceptLanguages = accept_Languages.split(",");
			for (String lan : acceptLanguages)
			{
				// es-ES;q=0.9 -> es-ES -> es
				String realLanguage = lan.split(";")[0].split("-")[0];
				if (ACCEPT_LANGUAGE.contains(realLanguage))
				{
					lang = realLanguage;
					break;
				}
				else if (realLanguage.equals("*"))
				{
					lang = "es";
					break;
				}
			}
			
			_log.debug(accept_Languages);

			String filePath = getLanguageFilePath(root, lang);
			_log.debug(filePath);
				
			if (new File(filePath).exists())
			{
				status = HttpServletResponse.SC_MOVED_TEMPORARILY;
				result = filePath.substring(root.length()).replaceAll("\\\\", "/");
			}
			else
			{
				_log.error( String.format("Languages '%s' are not supported", accept_Languages) );
			}
			
    		result = (PropsValues.IS_PREVIEW_ENVIRONMENT) ? result.concat("?env=preview") : result.concat("?env=live");

			response.setContentType("text/plain");
			response.setStatus(status);
			response.setHeader("Location", result);
		}

		return null;
	}
	
	/*
	 * 	Devuelve las comunidades del sistema
	 */
	public static List<Group> getGroups(String companyId) throws SystemException
	{
		LinkedHashMap<String, Object> params= new LinkedHashMap<String, Object>(); 
		List<Integer> list = new ArrayList<Integer>();
		list.add(0);
		params.put("types", list);

		List<Group> groupList = GroupLocalServiceUtil.search( Long.parseLong(companyId), null, null, params, -1, -1);
		
		return groupList;
	}
	
	
	/*
	 * 	Devuelve los parámetros de configuración de la comunidad seleccionada. 
	 */
	public static final String GET_COMMUNITY_CONFIG = new StringBuffer("SELECT id_, groupId, privateSearchUrl, publicSearchUrl, fuzzySearch, lastUpdated ").
																append("\nFROM Base_Communities ").
																append("\n\t WHERE groupId = %d").toString();	
	
	public static final String UPDATE_COMMUNITY_CONFIG = new StringBuffer("Update Base_Communities SET ").
																append("\n\t privateSearchUrl = '%s', ").
																append("\n\t publicSearchUrl = '%s', ").
																append("\n\t fuzzySearch = %d, ").
																append("\n\t lastUpdated = '%s' ").
																append("\n\t WHERE groupId = %d").toString();
	
	public static final String CREATE_COMMUNITY_CONFIG = new StringBuffer("INSERT INTO Base_Communities ( id_, groupId, privateSearchUrl, publicSearchUrl, fuzzySearch, lastUpdated ) ").
	append("\n\t VALUES ( %d, %d, '%s', '%s', %d, '%s' )").toString();
	
	public static Document getCommunityConfig(String groupId) throws SecurityException, NoSuchMethodException
	{ 
		return PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_COMMUNITY_CONFIG, Long.parseLong(groupId)), false, "rs", "row" );
	}
	
	public static Document setCommunityConfig(String type, String groupId, String privateSearchUrl, String publicsearchUrl, int fuzzySearch, String lastUpdated ) throws DocumentException, NumberFormatException, IOException, SQLException, SecurityException, NoSuchMethodException, SystemException
	{
		Document dom = SAXReaderUtil.read(lastUpdated);
		if(type.equals("update"))
			PortalLocalServiceUtil.executeUpdateQuery( String.format(UPDATE_COMMUNITY_CONFIG, privateSearchUrl, publicsearchUrl, fuzzySearch, dom.asXML(), Long.parseLong(groupId)) );
		else if(type.equals("insert"))
			PortalLocalServiceUtil.executeUpdateQuery( String.format(CREATE_COMMUNITY_CONFIG, CounterLocalServiceUtil.increment(), Long.parseLong(groupId), privateSearchUrl, publicsearchUrl, fuzzySearch, dom.asXML()) );
		
		return getCommunityConfig(groupId);
	}
	
	public static String getWidgetURL(long scopeGroupId, String postURL)
	{
		String widgetURL = "";
		try
		{
			Group scopeGroup = GroupLocalServiceUtil.getGroup(scopeGroupId);
			if(Validator.isNotNull(scopeGroup))
				widgetURL = "/widget/web" + scopeGroup.getFriendlyURL() + postURL;
		}
		catch (Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return widgetURL;
	}
	
	/**************************************************************************************************/
	/**							  SEARCH PORTLET										  			 **/
	/**************************************************************************************************/
	
	public static final String GET_COMMUNITY_CONFIG_SEARCH = new StringBuffer("SELECT id_, groupId, privateSearchUrl, publicSearchUrl, fuzzySearch").
			append("\nFROM Base_Communities ").
			append("\n\t WHERE groupId = %d").toString();
	
	public static final String INSERT_COMMUNITY_CONFIG_SEARCH = new StringBuffer("INSERT INTO Base_Communities(id_, groupId, privateSearchUrl, publicSearchUrl, fuzzySearch)").
			append("\n\t values ( %d, %d, '%s', '%s', '%s')").
			append("\n\t ON DUPLICATE KEY UPDATE privateSearchUrl=VALUES(privateSearchUrl), publicSearchUrl=VALUES(publicSearchUrl), fuzzySearch=VALUES(fuzzySearch)").toString();
	
	public static Document setSearchComunityConfig(String groupId, String privateSearchUrl, String publicsearchUrl, String fuzzySearch) throws NumberFormatException, SystemException, IOException, SQLException, SecurityException, NoSuchMethodException 
	{
		PortalLocalServiceUtil.executeUpdateQuery(String.format(INSERT_COMMUNITY_CONFIG_SEARCH, CounterLocalServiceUtil.increment(), Long.parseLong(groupId), privateSearchUrl, publicsearchUrl, Integer.parseInt(fuzzySearch)));
		CommunitiesLocalServiceUtil.clearCache();
		return getSearchCommunityConfig(groupId);
	}
	
	public static Document getSearchCommunityConfig(String groupId) throws SecurityException, NoSuchMethodException
	{ 
		return PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_COMMUNITY_CONFIG_SEARCH, Long.parseLong(groupId)), false, "rs", "row" );
	}
	
	/**************************************************************************************************/
	/**							  LAST UPDATE PORTLET										  		 **/
	/**************************************************************************************************/
	
	public static final String GET_COMMUNITY_CONFIG_LASTUPDATE = new StringBuffer("SELECT id_, groupId, lastUpdated").
			append("\nFROM Base_Communities ").
			append("\n\t WHERE groupId = %d").toString();
	
	public static final String INSERT_COMMUNITY_CONFIG_LASTUPDATE = new StringBuffer("INSERT INTO Base_Communities(id_, groupId, lastUpdated)").
			append("\n\t values ( %d, %d, '%s')").
			append("\n\t ON DUPLICATE KEY UPDATE lastUpdated=VALUES(lastUpdated)").toString();
	
	public static Document setLastUpdateConfig(String groupId, String lastUpdated) throws NumberFormatException, SystemException, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		PortalLocalServiceUtil.executeUpdateQuery(String.format(INSERT_COMMUNITY_CONFIG_LASTUPDATE, CounterLocalServiceUtil.increment(), Long.parseLong(groupId), lastUpdated));
		CommunitiesLocalServiceUtil.clearCache();
		return getLastUpdateConfig(groupId);
	}
	
	public static Document getLastUpdateConfig(String groupId) throws SecurityException, NoSuchMethodException
	{ 
		return PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_COMMUNITY_CONFIG_LASTUPDATE, Long.parseLong(groupId)), false, "rs", "row" );
	}
	
	/**
	 * Asume que se trata de un Widget estándar
	 * @param request
	 * @param themeDisplay
	 * @param appendHostLayoutURL
	 * @return
	 */
	public static String getWidgetContent(HttpServletRequest request, ThemeDisplay themeDisplay, boolean appendHostLayoutURL)
	{
		String widgetContent = ""; 
		try
		{
			String postURL = PortalUtil.getPortalProperties().getProperty("iter.host." + PortalUtil.getPortletId(request));
	
			if(!themeDisplay.isWidget() && Validator.isNotNull(postURL))
			{
				String widgetURL = getWidgetURL(themeDisplay.getScopeGroupId(), postURL);
				widgetContent = getWidgetContent(request, themeDisplay, appendHostLayoutURL, widgetURL);
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		return widgetContent;
	}
	
	/**
	 * Contenido de widgets compartidos
	 * @param request
	 * @param themeDisplay
	 * @param portletItemUserName
	 * @return
	 */
	public static String getSharedWidgetContent(HttpServletRequest request, ThemeDisplay themeDisplay, String portletItemUserName)
	{
		String widgetContent = "";
		
		try
		{
			Group scopeGroup = GroupLocalServiceUtil.getGroup(themeDisplay.getScopeGroupId());
			String pubDate	 = new SimpleDateFormat("yyyyMMddHHmmss").format( GroupMgr.getPublicationDate(scopeGroup.getGroupId()) );
			String widgetURL = String.format("/widget%s/web%s/-/%s/%s", WebKeys.WIDGET_SHARED_PREFIX, scopeGroup.getFriendlyURL(), portletItemUserName, pubDate);
			widgetContent	 = getWidgetContent(request, themeDisplay, false, widgetURL);
		}
		catch (Exception e)
		{
			_log.error(e);
		}
		
		return widgetContent;
	}
	
	/**
	 * Se crea el widget como ServerSide o se carga directamente su contenido
	 * @param request
	 * @param themeDisplay
	 * @param appendHostLayoutURL
	 * @param widgetURL
	 * @return
	 * @throws IOException
	 */
	public static String getWidgetContent(HttpServletRequest request, ThemeDisplay themeDisplay, boolean appendHostLayoutURL, String widgetURL) throws IOException
	{
		StringBuffer widgetContent = new StringBuffer(); 
		if(Validator.isNotNull(widgetURL))
		{
			if(PHPUtil.isApacheRequest(request))
			{
				widgetContent.append("<!--#include virtual=\"" + widgetURL + "\" -->");
			}
			else
			{
				StringBuffer urlString = new StringBuffer(PortalUtil.getPortalURL(request, false) + widgetURL);
				if(appendHostLayoutURL)
					urlString.append(cleanPortalGroupURL(request, themeDisplay));
				
				URL url = new URL(urlString.toString());
				
				_log.trace("Widget requested URL: " + url.toString());

		        URLConnection connection = url.openConnection();
		        connection.setRequestProperty(HttpHeaders.COOKIE, request.getHeader(HttpHeaders.COOKIE));
		        
		        HttpURLConnection httpConnection = (HttpURLConnection)connection;
		        httpConnection.connect();	
		        
			    BufferedReader in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
			    String currentLine;
		        while ((currentLine = in.readLine()) != null) 
		        	widgetContent.append(currentLine);
		        
		        httpConnection.disconnect();
			}
		}
		
		return widgetContent.toString();
	}
	
	public static String cleanPortalGroupURL(HttpServletRequest request, ThemeDisplay themeDisplay)
	{
		StringBuffer cleanURL = new StringBuffer();
		try
		{
			String dirtyURL = PortalUtil.getCurrentURL(request);
			if(Validator.isNotNull(dirtyURL))
			{
				Group scopeGroup = GroupLocalServiceUtil.getGroup(themeDisplay.getScopeGroupId());
				if(Validator.isNotNull(scopeGroup))
				{
					String[] pathArray = StringUtil.split(dirtyURL, StringPool.SLASH);
					for(int i = 0; i < pathArray.length; i++)
					{
						if( !pathArray[i].isEmpty() &&
							!pathArray[i].equals("web") && 
							!pathArray[i].equals(scopeGroup.getFriendlyURL().replace(StringPool.SLASH, StringPool.BLANK)))
						{
							cleanURL.append(StringPool.SLASH + pathArray[i]);
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		return cleanURL.toString();
	}
	
	public static String getCurrentURL(HttpServletRequest request)
	{
		String url = "";
		Object orginalRequestUri = request.getAttribute(IterKeys.REQUEST_ATTRIBUTE_ORIGINAL_REQUEST_URI);
		if(orginalRequestUri != null)
			url = orginalRequestUri.toString();
		else
			url = PortalUtil.getCurrentURL(request);
		
		return url;
	}
	
	public static void setGlobalFbLg(String groupid, String language) throws DocumentException, IOException, SQLException, NumberFormatException, SystemException
	{
		String UPDATE_FIELD = "UPDATE Base_Communities SET facebookLanguage = '%s' WHERE groupId = %d";
		String q = String.format( UPDATE_FIELD, language, Long.valueOf(groupid) );
		PortalLocalServiceUtil.executeUpdateQuery(q);
	}
	
	public static String getGlobalFbLg(String groupid) throws DocumentException, IOException, SQLException, NumberFormatException, SystemException, SecurityException, NoSuchMethodException
	{
		String GET_FIELD = "SELECT facebookLanguage FROM Base_Communities WHERE groupId = %d";
		String q = String.format( GET_FIELD, Long.valueOf(groupid) );
		Document tmplDom = PortalLocalServiceUtil.executeQueryAsDom(q);
		List<Node> nodes = tmplDom.selectNodes("rs/row[@facebookLanguage]");
		Node node = nodes.get(0);
		Element elem = (Element)node;
		
		return (elem.attributeValue("facebookLanguage"));
	}
	
	public static String setPortletPreferencesFbLg(long plid, String portletResource, String xmlPreferences) throws Exception
	{
		String groupId = String.valueOf((LayoutLocalServiceUtil.getLayout(plid).getGroupId()));
		
		Document dom = SAXReaderUtil.read(xmlPreferences);
		
		List<Node> nodes = dom.selectNodes("portlet-preferences/preference[name='facebookScreenLanguage']");
		
		if (nodes.size() > 0)
		{
		    Element elem = (Element)nodes.get(0);
		    String facebookLanguage = elem.valueOf("value");
		    setGlobalFbLg(groupId, facebookLanguage);
		    elem.getParent().remove(nodes.get(0));
		}
		
		com.liferay.portal.model.PortletPreferences preferences = PortletPreferencesLocalServiceUtil.updatePreferences(PortletKeys.PREFS_OWNER_ID_DEFAULT,PortletKeys.PREFS_OWNER_TYPE_LAYOUT, 
				plid, portletResource, dom.asXML());
						
		return preferences.getPreferences();
	}
	
	public static String getPortletPreferencesFbLg(HttpServletRequest request, String xmlInitParams, String groupId) throws Exception
	{
		Document dom = SAXReaderUtil.read(xmlInitParams);
		Element root = dom.getRootElement();
		
		String portletResource	= GetterUtil.get(root.selectSingleNode("portletResource").getText(), "");
		
		ThemeDisplay themeDisplay = ThemeDisplayUtil.buildThemeDisplay(dom, request);
			
		request.setAttribute(WebKeys.THEME_DISPLAY, themeDisplay);
		
		PortletPreferences preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);
		
		preferences = mergePreferencesFbLg(preferences,groupId);
		
		String result = PortletPreferencesLocalServiceUtil.toXML(preferences);
		return result;
	}
	
	public static PortletPreferences mergePreferencesFbLg(PortletPreferences preferences, String groupId) throws Exception
	{
		String facebookLanguage = getGlobalFbLg(groupId);
		if (facebookLanguage.equals("")){
			preferences.setValue("facebookScreenLanguage", "es_ES");
		}
		else{
			preferences.setValue("facebookScreenLanguage", facebookLanguage);
		}
		
		return preferences;
	}
	
	/**
	 * Construye un xmlRequest necesario para que no se pierdan los encuadres dictados por la plantilla en el cálculo del html que se pintará como resultado de un portlet
	 * @param themeDisplay
	 * @param scheme
	 * @param serverPort
	 * @return xmlRequest
	 * @throws DocumentException
	 */
	
	public static String getXmlRequest(  ThemeDisplay themeDisplay, String scheme, String serverPort ) throws DocumentException
	{
		String xmlRequest= "";
		
		//XML Request
		Element requestDataXML = SAXReaderUtil.read("<request/>").getRootElement();
		Element row = requestDataXML.addElement("server-name");
		row.addText(themeDisplay.getServerName());
		row = requestDataXML.addElement("scheme");
		row.addText(scheme);
		row = requestDataXML.addElement("server-port");
		row.addText(serverPort);
		
		Element eThemeDisplay = requestDataXML.addElement("theme-display");
		eThemeDisplay.addElement("portal-url")    .addText(new StringBuilder(themeDisplay.getServerName()).append(":").append(serverPort).toString());
		eThemeDisplay.addElement("scope-group-id").addText(Long.toString(themeDisplay.getScopeGroupId()));
		eThemeDisplay.addElement("server-name")   .addText(themeDisplay.getServerName());
		eThemeDisplay.addElement("server-port")   .addText(serverPort);
		eThemeDisplay.addElement("url-portal")    .addText(new StringBuilder(themeDisplay.getServerName()).append(":").append(serverPort).toString());
		
		xmlRequest = requestDataXML.asXML();
		
		return xmlRequest;
	}
	
	static public void enablePortlet(long plid, String portletId, boolean enable) throws Exception
	{
		PortletViewMgr.enablePortlet(plid, portletId, enable);
	}
	
	static public Document getLayoutPortlets(long plid) throws SecurityException, NoSuchMethodException, PortalException, SystemException, DocumentException
	{
		return PortletViewMgr.getLayoutPortlets(plid);
	}
	
	static public JsonObject list2Json(List<String[]> list)
	{
		JsonObject result = new JsonObject();
		JsonArray  array   = new JsonArray();
		
		for (String[] item : list)
		{
			JsonObject obj = new JsonObject();
			obj.addProperty("value", item[0]);
			obj.addProperty("name",  item[1]);
			array.add(obj);
		}
		
		result.add("list", array);
		return result;

	}
	
	static public JsonObject getVisibilityDevices()
	{
		return list2Json(PortletKeys.VISIBILITY_DEVICES);
	}
	
	static public JsonObject getVisibilityDaysOfWeek()
	{
		return list2Json(PortletKeys.VISIBILITY_DAYS);
	}
}
