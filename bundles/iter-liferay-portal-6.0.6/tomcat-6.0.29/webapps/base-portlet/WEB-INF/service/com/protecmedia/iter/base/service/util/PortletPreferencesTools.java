package com.protecmedia.iter.base.service.util;

import javax.portlet.PortletPreferences;
import javax.portlet.RenderRequest;
import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.service.PortletPreferencesLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portlet.PortletPreferencesFactoryUtil;

public class PortletPreferencesTools 
{
	// Constantes de las preferencias
	public static final String PREFS_CATALOG_TABS 	= com.liferay.portal.util.PortletPreferencesTools.PREFS_CATALOG_TABS;
	
//	private static final String PREFS_NODE_XPATH	= com.liferay.portal.util.PortletPreferencesTools.PREFS_NODE_XPATH;
//	private static final String PREFS_VALUE_XPATH 	= com.liferay.portal.util.PortletPreferencesTools.PREFS_VALUE_XPATH;

	public static final String PREFS_PORTLETITEM_NODE_XPATH			= com.liferay.portal.util.PortletPreferencesTools.PREFS_PORTLETITEM_NODE_XPATH;
	public static final String PREFS_PORTLETITEM_VALUE_XPATH 		= com.liferay.portal.util.PortletPreferencesTools.PREFS_PORTLETITEM_VALUE_XPATH;
	
	public static final String PREFS_VALUE_XPATH_LAYOUTS			= com.liferay.portal.util.PortletPreferencesTools.PREFS_VALUE_XPATH_LAYOUTS;
	public static final String PREFS_VALUE_XPATH_LAYOUTS_NOT_IDS	= com.liferay.portal.util.PortletPreferencesTools.PREFS_VALUE_XPATH_LAYOUTS_NOT_IDS;
	
	public static final String PREFS_VALUE_XPATH_JOURNALTEMPLATES 	= com.liferay.portal.util.PortletPreferencesTools.PREFS_VALUE_XPATH_JOURNALTEMPLATES;
	public static final String PREFS_VALUE_XPATH_CATEGORIES			= com.liferay.portal.util.PortletPreferencesTools.PREFS_VALUE_XPATH_CATEGORIES;
	public static final String PREFS_VALUE_XPATH_VOCABULARIES		= com.liferay.portal.util.PortletPreferencesTools.PREFS_VALUE_XPATH_VOCABULARIES;
	public static final String PREFS_VALUE_XPATH_QUALIFICATIONS		= com.liferay.portal.util.PortletPreferencesTools.PREFS_VALUE_XPATH_QUALIFICATIONS;
	public static final String PREFS_VALUE_XPATH_CATALOG_TABS		= com.liferay.portal.util.PortletPreferencesTools.PREFS_VALUE_XPATH_CATALOG_TABS;
	public static final String PREFS_VALUE_XPATH_MODELID			= com.liferay.portal.util.PortletPreferencesTools.PREFS_VALUE_XPATH_MODELID;
	public static final String PREFS_VALUE_XPATH_PRODUCTS			= com.liferay.portal.util.PortletPreferencesTools.PREFS_VALUE_XPATH_PRODUCTS;

	//private static final String GET_CLASSIC_SECTION_UUID = com.liferay.portal.util.PortletPreferencesTools.GET_CLASSIC_SECTION_UUID;

	public static final String SEL_PORTLETITEM_NUM_LINKS = com.liferay.portal.util.PortletPreferencesTools.SEL_PORTLETITEM_NUM_LINKS;

	public static final String SEL_PORTLETITEM_BY_USERNAME = com.liferay.portal.util.PortletPreferencesTools.SEL_PORTLETITEM_BY_USERNAME;
	
	// Esta sería la Query a utilizar si se quisiese obtener todas las preferencias vinculadas a un PortletItem a partir del PortletItemId
	// No se utiliza porque en el contexto actual ya se tienen los datos del PortletItem y al omitir un Join y tener de antemano el "PortletItem%"
	// específico, se agiliza mucho dicha Query
//	SELECT PortletItem.userName, PortletItem.name, PortletPreferences.portletId, friendlyURL
//	FROM 	PortletItem
//	INNER JOIN PortletPreferences ON (PortletPreferences.portletId LIKE CONCAT(PortletItem.portletId, "%")
//																		AND ownerId = 0 AND ownerType = 3
//																		AND userName = ExtractValue(preferences, "/portlet-preferences/preference[name='portletItem']/value/text()"))
//	INNER JOIN Layout ON (PortletPreferences.plid = Layout.plid)				
//		WHERE portletItemId = 9610195
	public static final String SEL_LINKED_PREFERENCES = com.liferay.portal.util.PortletPreferencesTools.SEL_LINKED_PREFERENCES;

	/**
	 * Crea o actualiza un elemento de preferencia con el siguiente <b>name</b> y <b>value</b>
	 * 
	 * @param dom 	Document con las preferencias del portlet
	 * @param name	Nombre de la preferencia a actualizar
	 * @param value	Valor de la preferencia
	 * 
	 * @return El elemento de preferencia recién actualizado
	 */
	public static Element updatePrefsValue(Document dom, String name, String value)
	{
		return com.liferay.portal.util.PortletPreferencesTools.updatePrefsValue(dom, name, value);
	}
	
//	private static Node getPrefsNode(Document dom, String name)
//	{
//		return com.liferay.portal.util.PortletPreferencesTools.getPrefsNode(dom, name);
//	}
	
//	private static String getPrefsValueXPath(String name)
//	{
//		return com.liferay.portal.util.PortletPreferencesTools.getPrefsValueXPath(name);
//	}
	
//	private static String getPrefsValueXPath(List<String> names)
//	{
//		return com.liferay.portal.util.PortletPreferencesTools.getPrefsValueXPath(names);
//	}

	/**
	 * 		
	 * @param userName
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws ServiceError 
	 */
	static public Document getPortletItem(String userName) throws SecurityException, NoSuchMethodException, com.liferay.portal.kernel.error.ServiceError
	{
		return com.liferay.portal.util.PortletPreferencesTools.getPortletItem(userName);
	}
	
//	/**
//	 * 
//	 * @param portletItemId
//	 * @return
//	 * @throws SecurityException
//	 * @throws NoSuchMethodException
//	 */
//	static public Long getNumPreferencesLinks(long portletItemId) throws SecurityException, NoSuchMethodException
//	{
//		Document portletItemDom = PortalLocalServiceUtil.executeQueryAsDom( String.format(SEL_PORTLETITEM_NUM_LINKS, portletItemId));
//		long numLinks = XMLHelper.getLongValueOf(portletItemDom, "/rs/row/@numLinks");
//		return numLinks;
//	}
//	
	/**
	 * 
	 * @param portletItemId
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws com.liferay.portal.kernel.error.ServiceError 
	 */
	static public String checkNumReferencesLinks(String portletItemIds) throws SecurityException, NoSuchMethodException, com.liferay.portal.kernel.error.ServiceError
	{
		return com.liferay.portal.util.PortletPreferencesTools.checkNumReferencesLinks(portletItemIds);
	}
	
	/**
	 * 
	 * @param renderRequest
	 * @param request
	 * @return
	 * @throws SystemException 
	 */
	static public PortletPreferences getPortletPreferences(RenderRequest renderRequest, HttpServletRequest request) throws SystemException
	{
		return com.liferay.portal.util.PortletPreferencesTools.getPortletPreferences(renderRequest, request);
	}
	
	static public PortletPreferences getPreferences(HttpServletRequest request) throws SystemException, SecurityException, PortalException, NoSuchMethodException, com.liferay.portal.kernel.error.ServiceError
	{
		return com.liferay.portal.util.PortletPreferencesTools.getPreferences(request);
	}

	/**
	 * Método que a partir del PortletItem.userName (que en realidad será un UUID) devuelve las preferencias asociadas a dicho PortletItem
	 * 
	 * @param portletItemUserName
	 * @return Las preferencias asociadas al PortletItem
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws SystemException
	 * @throws PortalException
	 * @throws com.liferay.portal.kernel.error.ServiceError 
	 */
	static public PortletPreferences getPreferences(String portletItemUserName) throws SecurityException, NoSuchMethodException, SystemException, PortalException, com.liferay.portal.kernel.error.ServiceError
	{
		return com.liferay.portal.util.PortletPreferencesTools.getPreferences(portletItemUserName);
	}
	
	/**
	 * 
	 * @param request
	 * @param xmlInitParams
	 * @return
	 * @throws Exception
	 */
	public static PortletPreferences getPreferences(HttpServletRequest request, String xmlInitParams) throws Exception
	{
		Document dom = SAXReaderUtil.read(xmlInitParams);
		Element root = dom.getRootElement();
		
		String portletResource	= GetterUtil.get(root.selectSingleNode("portletResource").getText(), "");
		
		ThemeDisplay themeDisplay = ThemeDisplayUtil.buildThemeDisplay(dom, request);
			
		request.setAttribute(WebKeys.THEME_DISPLAY, themeDisplay);
		
		return PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);
	}

	/**
	 * 
	 * @param request
	 * @param xmlInitParams
	 * @return
	 * @throws Exception
	 */
	public static String getPreferencesAsXML(HttpServletRequest request, String xmlInitParams) throws Exception
	{
		PortletPreferences preferences = getPreferences(request, xmlInitParams);
		
		String result = PortletPreferencesLocalServiceUtil.toXML(preferences);
		return result;
	}

	/**
	 * Método que determina si la preferencia de "Sección actual" (defaultLayout) está activada o no
	 * @param doc XML de preferencias
	 * @return True si existe y está está activada o False en caso contrario.
	 */
	public static boolean isDefaultLayoutEnable(Document doc)
	{
		return com.liferay.portal.util.PortletPreferencesTools.isDefaultLayoutEnable(doc);
	}
	
	public static boolean isMetadataContextEnable(Document doc)
	{
		return com.liferay.portal.util.PortletPreferencesTools.isMetadataContextEnable(doc);
	}

	/**
	 * Estas preferencias NO podrán tener configurada una sección por defecto
	 * @param doc XML con las preferencias
	 * @throws ServiceError 
	 */
	public static void checkDefaultLayoutPreferences(Document doc) throws ServiceError
	{
		// Michel Godínez. 2013/09/12
		// De momento se desactiva, NO se quiere forzar a que por el hecho de tener Sección actual NO se puedan compartir 
		// preferencias. Si se quisiese forzar (idea inicial), basta con descomentar el código que aparece a continuación

		// Estas preferencias NO podrán tener configurada una sección por defecto
		// return com.liferay.portal.util.PortletPreferencesTools.checkDefaultLayoutPreferences(doc);
	}
	
	/**
	 * Método que a partir del userName y el portletId de un PortletItem devuelve un DOM con las preferencias de aquellos portlets vinculados a él
	 * 
	 * @param 	userName	UUID que identifica al PortletItem en las preferencias de los Portlets
	 * @param 	portletId	PortletId (sin instancia) por el que se buscará
	 * @return				DOM con las preferencias de aquellos portlets vinculados a él
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 */
	public static Document getLinkedPreferences(String userName, String portletId) throws SecurityException, NoSuchMethodException
	{
		return com.liferay.portal.util.PortletPreferencesTools.getLinkedPreferences(userName, portletId);
	}

	/**
	 * @param preferences
	 * @return La sección a visualizar según las preferencias configuradas.		<br/>
	 * Internamente comprueba que si no se filtra por la sección por defecto 	<br/>
	 * ni por otra, la sección a visualizar sea la principal del artículo.
	 */
	public static int getSectionToShow(PortletPreferences preferences)
	{
		return com.liferay.portal.util.PortletPreferencesTools.getSectionToShow(preferences);
	}
	
	/**
	 * @param preferences
	 * @return El flag que indica si se filtra o no por la sección actual
	 */
	public static boolean getDefaultLayout(PortletPreferences preferences)
	{
		return com.liferay.portal.util.PortletPreferencesTools.getDefaultLayout(preferences);
	}
	
	public static String[] getLayoutIds(PortletPreferences preferences, long sectionPlid, boolean defaultLayout) throws SecurityException, PortalException, SystemException, NoSuchMethodException
	{
		return com.liferay.portal.util.PortletPreferencesTools.getLayoutIds(preferences, sectionPlid, defaultLayout);
	}
	
	public static long[] getContentCategoriesIds(PortletPreferences preferences)
	{
		return com.liferay.portal.util.PortletPreferencesTools.getContentCategoriesIds(preferences);
	}
	
	public static int[] getLayoutLimits(PortletPreferences preferences)
	{
		return com.liferay.portal.util.PortletPreferencesTools.getLayoutLimits(preferences);
	}

}
