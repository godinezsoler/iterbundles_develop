package com.protecmedia.iter.xmlio.service.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.portlet.PortletPreferences;
import javax.portlet.ReadOnlyException;
import javax.portlet.ValidatorException;

import org.apache.commons.codec.binary.Base64;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.model.LayoutTypePortlet;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.PortletConstants;
import com.liferay.portal.model.PortletItem;
import com.liferay.portal.model.ResourceConstants;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.LayoutTemplateLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.PortletItemLocalServiceUtil;
import com.liferay.portal.service.PortletPreferencesLocalServiceUtil;
import com.liferay.portal.service.ResourceLocalServiceUtil;
import com.liferay.portal.service.permission.PortletPermissionUtil;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portlet.asset.model.AssetCategoryConstants;
import com.liferay.portlet.asset.model.AssetVocabularyConstants;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.designer.model.PageTemplate;
import com.protecmedia.iter.designer.service.PageTemplateLocalServiceUtil;

public class WebsiteImportMgr extends WebsiteIOMgr
{
	private static Log _log = LogFactoryUtil.getLog(WebsiteImportMgr.class);
	
	String 	_mlnstationInfo								= null;
	private Document _preProcessInfo  					= null;
	private Map<String, List<String>> _importedObjects 	= new HashMap<String, List<String>>();
	private Map<String, String> _portletItemMap			= new HashMap<String, String>();
	private Map<String, String> _tplMap					= new HashMap<String, String>();
	private Document _serviceMap						= null;
	
	private static final String SEL_LAYOUT_BY_PLID = new StringBuilder(
		"SELECT plid, uuid_			\n").append(
		"FROM Layout				\n").append(
		"	WHERE plid IN (%s)	\n").toString();

	private static final String SEL_MODEL_PLID = new StringBuilder(
		"SELECT CONCAT(layoutId) plid 															\n").append(
		"FROM Designer_PageTemplate																\n").append(
		"		WHERE id_ IN (%s)																\n").toString();
				
	private static final String SEL_CATALOG_PLID = new StringBuilder(			
		"SELECT CONCAT(plid)																	\n").append(
		"FROM CatalogPage																		\n").append(
		"INNER JOIN CatalogElement ON CatalogPage.catalogPageId = CatalogElement.catalogPageId	\n").append(	
		"	WHERE catalogElementId IN ('%s')													\n").toString();
	
	private static final String SEL_TPL_BY_TPLNAME = new StringBuilder(
	"SELECT tplid, tplname							\n").append(
	"FROM LayoutTemplate							\n").append(
	"	WHERE groupid = %d AND tplname IN ('%s')	\n").toString();

	public WebsiteImportMgr(long siteId, String mlnstationInfo) throws ServiceError, PortalException, SystemException
	{
		super(siteId);
		_mlnstationInfo = mlnstationInfo;
	}

	public void importPreProcessInfo(String content) throws DocumentException, PortalException, SystemException, IOException, SecurityException, NoSuchMethodException, ServiceError
	{
		_preProcessInfo = SAXReaderUtil.read( new String(Base64.decodeBase64(content), StringPool.UTF8) );
		
		track("PreProcessInfo", _preProcessInfo);
		
		importPortletItems( _preProcessInfo.selectNodes("/preinfo/portletitems/portletitem") );
		importTpls();
		importServices();
	}
	
	public void resetImport(String mlnstationInfo) throws ServiceError
	{
		 ErrorRaiser.throwIfFalse(_mlnstationInfo.equalsIgnoreCase(mlnstationInfo), IterErrorKeys.XYZ_E_WEBSITE_IS_BEING_IMPORTED_OTHER_STATION_ZYX);
	}
	
	private void importPortletItems(List<Node> portletItems) throws PortalException, SystemException
	{
		long userId = GroupMgr.getDefaultUserId();
		
		for (Node portletItemNode : portletItems)
		{
			String portletId	= XMLHelper.getTextValueOf(portletItemNode, "@portletId");
			String name			= XMLHelper.getTextValueOf(portletItemNode, "@name");
			PortletItem portletItem = PortletItemLocalServiceUtil.updatePortletItem_ExceptUserName(userId, _siteId, name, portletId, PortletPreferences.class.getName());
			
			String oldUserName	= XMLHelper.getTextValueOf(portletItemNode, "@userName");
			String newUserName	= portletItem.getUserName();
			_portletItemMap.put(oldUserName, newUserName);
		}
	}
	
	private void importTpls() throws SecurityException, NoSuchMethodException, ServiceError
	{
		List<Node> tpls = _preProcessInfo.selectNodes("/preinfo/tpls/tpl");
		for (Node tpl : tpls)
		{
			String oldTplid = ((Element)tpl).attributeValue("tplid");
			String tplName	= ((Element)tpl).attributeValue("tplname");
			_tplMap.put(oldTplid, tplName);
		}
		
		String sql = String.format(SEL_TPL_BY_TPLNAME, _siteId, StringUtil.merge( _tplMap.values(), "','" ));
		Document dom = PortalLocalServiceUtil.executeQueryAsDom(sql);
		
		for(Entry<String, String> entry : _tplMap.entrySet())
		{
			String tplName	= entry.getValue();
			
			String newTplId	= XMLHelper.getTextValueOf(dom, String.format("/rs/row[@tplname='%s']/@tplid", tplName));
			ErrorRaiser.throwIfNull(newTplId, IterErrorKeys.XYZ_E_LAYOUTTEMPLATE_NOT_FOUND_ZYX, tplName);
			entry.setValue(newTplId);
		}
		
		// Se añade al mapa de TPLs los TPLs de Liferay
		Set<String> liferayTpls = LayoutTemplateLocalServiceUtil.getLiferayLayoutTemplatesIds();
		for (String tpl : liferayTpls)
		{
			_tplMap.put(tpl, tpl);
		}
	}
	
	private void importServices() throws DocumentException
	{
		if (_preProcessInfo.selectNodes("/preinfo/services/service").size() > 0)
		{
			_serviceMap = SAXReaderUtil.createDocument();
			_serviceMap.add(_preProcessInfo.selectSingleNode("/preinfo/services").detach());
		}
	}
	
	public void importObject(String id, String className, String content) throws ServiceError, UnsupportedEncodingException, DocumentException, NumberFormatException, PortalException, SystemException
	{
		_log.info( String.format("Starting import object: id(%s) class(%s) website(%d)", id, className,_siteId) );
		
		ErrorRaiser.throwIfFalse( !className.isEmpty() && SUPPORTED_CLASSES.contains(className), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document domContent = SAXReaderUtil.read( new String(Base64.decodeBase64(content), StringPool.UTF8) );
		
		if (_log.isDebugEnabled())
			_log.debug( domContent.asXML() );

		if (className.equals(LayoutConstants.CLASS_LAYOUT))
			importLayout( Long.parseLong(id), domContent );
		
		else if (className.equals(LayoutConstants.CLASS_MODEL))
			importModel( Long.parseLong(id), domContent );
		
		else if (className.equals(LayoutConstants.CLASS_CATALOG))
			importCatalogPage( id, domContent );
		
		// Se actualiza la lista de objetos correctamente importados
		List<String> listObjs = _importedObjects.get(className);
		if (listObjs == null)
		{
			listObjs = new ArrayList<String>();
			_importedObjects.put(className, listObjs);
		}
		listObjs.add(id);
 
		_log.info( String.format("The import has finished: id(%s) class(%s) website(%d)", id, className,_siteId) );
	}
	
	public void importPostProcessInfo(String content, String relationships) throws DocumentException, NumberFormatException, SecurityException, PortalException, SystemException, NoSuchMethodException, ReadOnlyException, ValidatorException, IOException, ServiceError
	{
		_log.info( String.format("Starting import post process: website(%d)", _siteId) );
		
		//Document domContent 		= SAXReaderUtil.read( new String(Base64.decodeBase64(content), StringPool.UTF8) );
		Document domRelationships 	= SAXReaderUtil.read( relationships );
		track("PostProcessInfo", domRelationships);
		
		if (_log.isDebugEnabled())
		{
		//	_log.debug(domContent.asXML());
			_log.debug(domRelationships.asXML());
		}
		
		_updatePreferences(domRelationships);
		
		_log.info( String.format("The import post process has finished: website(%d)", _siteId) );
	}
	
	/**
	 * Se crea un mapa oldUUID -> newUUID para que sea más rápido al actualizar cada una de las 
	 * preferencias de los portletes de los layouts importados
	 * 
	 * @param relationships
	 * @return
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 */
	private Map<String, String> _buildLayoutRelationshipsMap(Document relationships) throws SecurityException, NoSuchMethodException
	{
		Map<String, String> layoutMap = new HashMap<String, String>();
		
		// Se actualizan todos los Layouts
		List<Node> layoutList = relationships.selectNodes( String.format("/objects/obj[@class = '%s']", LayoutConstants.CLASS_LAYOUT) );
		
		// Se obtiene la lista de nuevos PLIDs, para buscar su UUID en BBDD
		StringBuilder plids = new StringBuilder();
		for (Node layoutNode : layoutList)
			plids.append(XMLHelper.getLongValueOf(layoutNode, "@newid", -1)).append(",");
		
		Document newLayoutDom = null;
		if (plids.length() > 0)
		{
			plids.deleteCharAt(plids.length()-1);
			
			// DOM que contiene los nuevos PLIDs y UUIDs
			newLayoutDom = PortalLocalServiceUtil.executeQueryAsDom( String.format(SEL_LAYOUT_BY_PLID, plids) );
		}
		
		for (Node layoutNode : layoutList)
		{
			long newPlid 	= XMLHelper.getLongValueOf(layoutNode, "@newid", -1);
			String newUUID 	= XMLHelper.getTextValueOf(newLayoutDom, String.format("/rs/row[@plid='%d']/@uuid_", newPlid), "00000000-0000-0000-0000-000000000000");
			long oldPlid 	= XMLHelper.getLongValueOf(layoutNode, "@oldid");
			String oldUUID	= XMLHelper.getTextValueOf(_preProcessInfo, String.format("/preinfo/layouts/layout[@plid='%d']/@uuid_", oldPlid));
			
			if (Validator.isNotNull(oldUUID))
				layoutMap.put(oldUUID, newUUID);
		}

		return layoutMap;
	}
	
	/**
	 * Se crea el mapa de Categorías oldCat -> newCat
	 * 
	 * @param relationships
	 * @return
	 * @throws ServiceError 
	 */
	private Map<String, String> _buildRelationshipsMap(Document relationships, String className) throws ServiceError
	{
		Map<String, String> map = new HashMap<String, String>();
		List<Node> list = relationships.selectNodes( String.format("/objects/obj[@class = '%s']", className) );
		
		for (Node cat : list)
		{
			String oldId = XMLHelper.getStringValueOf(cat, "@oldid");
			ErrorRaiser.throwIfNull(oldId);
			String newId = XMLHelper.getStringValueOf(cat, "@newid");
			ErrorRaiser.throwIfNull(newId);
			
			map.put(oldId, newId);
		}
		return map;
	}
	
	/**
	 * 
	 * @param preferences
	 * @param preferenceKeys
	 * @param idsMap
	 * @return
	 * @throws ReadOnlyException
	 */
	private boolean _updatePortletPreferences(javax.portlet.PortletPreferences preferences, List<String> preferenceKeys, Map<String, String>idsMap) throws ReadOnlyException
	{
		boolean needUpdate = false;
		
		for (int iPref = 0; iPref < preferenceKeys.size(); iPref++)
		{
			String[] oldValues = preferences.getValues(preferenceKeys.get(iPref), null);
			if (oldValues != null && oldValues.length > 0)
			{
				String[] newValues = new String[oldValues.length];
				
				// Se busca cada UUID
				for (int i = 0; i < oldValues.length; i++)
				{
					String newID = idsMap.get(oldValues[i]);
					
					if (Validator.isNotNull(newID))
					{
						newValues[i] = newID;
						
						// Al menos una propiedad se ha modificado, es necesario actualizar
						needUpdate = true;
					}
					else
						newValues[i] = oldValues[i];
				}
				preferences.setValues(preferenceKeys.get(iPref), newValues);
			}
		}

		return needUpdate;
	}
	
	/**
	 * 	<preference>
	 *		<name>tabs</name>
	 *		<value><![CDATA[<tab elementid="d66804fe0c3b11e4bf0f000c296f75ac"><title>FirstTab</title></tab>]]></value>
	 *		<value><![CDATA[<tab elementid="2802fb191e4e11e4bf0f000c296f75ac"><title>SecondTab</title></tab>]]></value>
	 *	</preference>
	 *
	 * @param preferences
	 * @param idsMap
	 * @return
	 * @throws DocumentException 
	 * @throws ServiceError 
	 * @throws ReadOnlyException 
	 */
	private boolean _updatePortletCatalogPreferences(javax.portlet.PortletPreferences preferences, Map<String, String>idsMap) throws DocumentException, ServiceError, ReadOnlyException
	{
		boolean needUpdate = false;
		
		String[] oldValues = preferences.getValues(IterKeys.PREFS_CATALOG_TABS, null);
		if (oldValues != null && oldValues.length > 0)
		{
			String[] newValues = new String[oldValues.length];
			
			// Se busca cada UUID
			for (int i = 0; i < oldValues.length; i++)
			{
				Document dom = SAXReaderUtil.read(oldValues[i]);
				
				String oldId = XMLHelper.getStringValueOf(dom, "/tab/@elementid");
				ErrorRaiser.throwIfNull(oldId);
				
				String newID = idsMap.get(oldId);
				
				if (Validator.isNotNull(newID))
				{
					dom.getRootElement().addAttribute("elementid", newID);
					newValues[i] = dom.getRootElement().asXML();
					
					// Al menos una propiedad se ha modificado, es necesario actualizar
					needUpdate = true;
				}
				else
					newValues[i] = oldValues[i];
			}
			preferences.setValues(IterKeys.PREFS_CATALOG_TABS, newValues);
		}

		return needUpdate;
	}
	
	private List<Object> getImportedPLIDs()
	{
		List<Object> importedIDs = new ArrayList<Object>();
		List<String> importedLayouts = _importedObjects.get(LayoutConstants.CLASS_LAYOUT);
		
		if (importedLayouts != null)
			importedIDs.addAll(importedLayouts);
		
		String importedModels   = StringUtil.merge(_importedObjects.get(LayoutConstants.CLASS_MODEL));
		String importedCatalogs = StringUtil.merge(_importedObjects.get(LayoutConstants.CLASS_CATALOG), "','");

		StringBuilder sql = new StringBuilder();
		if (Validator.isNotNull(importedModels))
			sql.append( String.format(SEL_MODEL_PLID, importedModels) );
		
		if (Validator.isNotNull(importedCatalogs))
		{
			if (sql.length() > 0)
				sql.append("UNION \n");
			
			sql.append( String.format(SEL_CATALOG_PLID, importedCatalogs) );
		}
		
		if (sql.length() > 0)
		{
			List<Object> plids = PortalLocalServiceUtil.executeQueryAsList(sql.toString());
			if (plids != null)
				importedIDs.addAll(plids);
		}

		return importedIDs;
	}
	
	/**
	 * 
	 * @param relationships
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws NumberFormatException
	 * @throws PortalException
	 * @throws SystemException
	 * @throws ReadOnlyException
	 * @throws ValidatorException
	 * @throws IOException
	 * @throws ServiceError 
	 * @throws DocumentException 
	 */
	private void _updatePreferences(Document relationships) throws SecurityException, NoSuchMethodException, NumberFormatException, PortalException, SystemException, ReadOnlyException, ValidatorException, IOException, ServiceError, DocumentException
	{
		// Se construyen los mapas de relaciones
		Map<String, String> layoutMap 	= _buildLayoutRelationshipsMap(relationships);
		Map<String, String> catMap 		= _buildRelationshipsMap(relationships, AssetCategoryConstants.CLASS_CATEGORY);
		Map<String, String> vocMap 		= _buildRelationshipsMap(relationships, AssetVocabularyConstants.CLASS_VOCABULARY);
		Map<String, String> quaMap 		= _buildRelationshipsMap(relationships, "qualification");
		Map<String, String> productMap 	= _buildRelationshipsMap(relationships, "subscription");
		Map<String, String> modMap 		= _buildRelationshipsMap(relationships, LayoutConstants.CLASS_MODEL);
		Map<String, String> catElemMap 	= _buildRelationshipsMap(relationships, LayoutConstants.CLASS_CATALOG);
		
		
		List<String> PREFS_MODS	 = Arrays.asList(new String[] {IterKeys.PREFS_MODELID});
		List<Object> importedIDs = getImportedPLIDs();
		
		for (int iID = 0; iID < importedIDs.size(); iID++)
		{
			long plid = Long.valueOf( String.valueOf(importedIDs.get(iID)) );
			
			// Se recorren las configuraciones de todos sus portlets
			Layout layout = LayoutLocalServiceUtil.getLayout(plid);
			LayoutTypePortlet layoutTypePortlet = (LayoutTypePortlet) layout.getLayoutType();
			
			List<String> portlets = layoutTypePortlet.getPortletIds();
			for (String portletId : portlets) 
			{
				javax.portlet.PortletPreferences  preferences = PortletPreferencesLocalServiceUtil.getPreferences(GroupMgr.getCompanyId(), 
						PortletKeys.PREFS_OWNER_ID_DEFAULT, PortletKeys.PREFS_OWNER_TYPE_LAYOUT, plid, portletId);

				boolean needUpdate = _updatePortletPreferences(preferences, IterKeys.PREFS_LAYOUTS, 			layoutMap);
						needUpdate = _updatePortletPreferences(preferences, IterKeys.PREFS_CATS, 				catMap) 	|| needUpdate;
						needUpdate = _updatePortletPreferences(preferences, IterKeys.PREFS_VOCS, 				vocMap) 	|| needUpdate;
						needUpdate = _updatePortletPreferences(preferences, IterKeys.PREFS_QUAS, 				quaMap) 	|| needUpdate;
						needUpdate = _updatePortletPreferences(preferences, IterKeys.PREFS_SUBSCRIPTIONS_LIST, 	productMap) || needUpdate;
						needUpdate = _updatePortletPreferences(preferences, PREFS_MODS, 						modMap) 	|| needUpdate;
						
				if (PortletConstants.getRootPortletId(portletId).equalsIgnoreCase(PortletKeys.PORTLET_CATALOG))		
						needUpdate = _updatePortletCatalogPreferences(preferences, catElemMap) || needUpdate;
				
				if (PortletConstants.getRootPortletId(portletId).equalsIgnoreCase(PortletKeys.NESTED_PORTLETS))
						needUpdate = _updatePortletPreferences(preferences, PortletKeys.PREF_TPLS, _tplMap) || needUpdate;
				
				if (needUpdate)
					PortletPreferencesLocalServiceUtil.updatePreferences(PortletKeys.PREFS_OWNER_ID_DEFAULT,PortletKeys.PREFS_OWNER_TYPE_LAYOUT, 
																		 plid, portletId, PortletPreferencesLocalServiceUtil.toXML(preferences));
			}
		}
	}
	
	private void importLayout(long plid, Document content) throws PortalException, SystemException, ServiceError
	{
		if (_log.isDebugEnabled()) _log.debug( String.format("Starting layout import (plid=%d) in website %d", plid, _siteId) );
		
		Layout layout = LayoutLocalServiceUtil.getLayout(plid);
		LayoutTypePortlet layoutTypePortlet = (LayoutTypePortlet) layout.getLayoutType();

		String oldTemplateId = XMLHelper.getTextValueOf(content, "/layout/@templateid");
		ErrorRaiser.throwIfNull(oldTemplateId, IterErrorKeys.XYZ_E_LAYOUTTEMPLATE_NOT_FOUND_ZYX);
		String newTemplateId = _tplMap.get(oldTemplateId);
		ErrorRaiser.throwIfNull(newTemplateId, IterErrorKeys.XYZ_E_LAYOUTTEMPLATE_NOT_FOUND_ZYX, oldTemplateId);
		
		// Se actualiza el TPL
		layoutTypePortlet.setLayoutTemplateId(GroupMgr.getDefaultUserId(), newTemplateId, false);
		
		// Se eliminan los portlets actuales de la página
		removePortlets(layoutTypePortlet);
		
		// Se añaden los nuevos
		importPortlets(layoutTypePortlet, content);
		
		// Se añadel los servicios externos
		importExternalServices();
		
		LayoutLocalServiceUtil.updateLayout(layout, false);
		
		if (_log.isDebugEnabled()) _log.debug( String.format("The layout import (plid=%d) in website %d has finished", plid, _siteId) );
	}
	
	private void importPortlets(LayoutTypePortlet layoutTypePortlet, Document content) throws ServiceError, PortalException, SystemException
	{
		importPortlets(layoutTypePortlet, content, _portletItemMap);
	}
	
	public static void importPortlets(LayoutTypePortlet layoutTypePortlet, Document content, Map<String, String> portletItemMap) throws ServiceError, PortalException, SystemException
	{
		long plid 			= layoutTypePortlet.getLayout().getPlid();
		List<Node> portlets = content.selectNodes("/layout/portlet");
		
		for (Node portlet : portlets)
		{
			String pluginId = XMLHelper.getStringValueOf(portlet, "@pluginid");
			ErrorRaiser.throwIfNull(pluginId);
			
			String columnId = XMLHelper.getStringValueOf(portlet, "@columnid");
			ErrorRaiser.throwIfNull(columnId);
			
			String portletId = layoutTypePortlet.addPortletId(0, pluginId, columnId, -1, false);
			
			Node preferences = portlet.selectSingleNode("portlet-preferences");
			
			if (portletItemMap != null)
			{
				// Se verifica si las preferencias tienen un PortletItem, en cuyo caso se actualiza por el nuevo valor
				String xpath = String.format("preference[name='%s']", IterKeys.PREFS_PORTLETITEM);
				Node portletItemNode = preferences.selectSingleNode(xpath);
				if (portletItemNode != null)
				{
					String oldPortletItemUUID = XMLHelper.getTextValueOf(portletItemNode, "value", "");
					String newPortletItemUUID = portletItemMap.get(oldPortletItemUUID);
					
					if (Validator.isNotNull(newPortletItemUUID))
						portletItemNode.selectSingleNode("value").setText(newPortletItemUUID);
				}
			}
			
			PortletPreferencesLocalServiceUtil.updatePreferences(PortletKeys.PREFS_OWNER_ID_DEFAULT,PortletKeys.PREFS_OWNER_TYPE_LAYOUT, 
					plid, portletId, preferences.asXML());
		}
	}
	
	private static final String SQL_SELECT_SERVICES_BY_GROUP = "SELECT serviceId, url FROM externalservice WHERE groupId=%d";
	private static final String SQL_INSERT_SERVICES = "INSERT INTO externalservice (serviceId, groupId, name, method, url, headers, payload, proxy, disabled) VALUES \n";
	private static final String SQL_INSERT_SERVICES_VALUES = "('%s', %d, '%s', '%s', '%s', '%s', '%s', '%s', '%s'),\n";
	private static final String SQL_INSERT_SERVICES_UPDATE = "\nON DUPLICATE KEY UPDATE name=VALUES(name), method=VALUES(method), url=VALUES(url), headers=VALUES(headers), payload=VALUES(payload), proxy=VALUES(proxy), modifiedDate=NOW() ";
	private static final String XPATH_GET_SERVICEID_BY_URL = "/rs/row[@url='%s']/@serviceId";
	
	private void importExternalServices() throws ServiceError
	{
		if (_serviceMap != null)
		{
			try
			{
				List<Node> services = _serviceMap.selectNodes("/services/service");
				
				if (services.size() > 0)
				{
					// Recupera los servicios actuales.
					Document currentServices = PortalLocalServiceUtil.executeQueryAsDom(String.format(SQL_SELECT_SERVICES_BY_GROUP, _siteId));
					
					// Construye la sentencia de inserción.
					StringBuilder sql = new StringBuilder(SQL_INSERT_SERVICES);
					
					for (Node service : services)
					{
						String name     = XMLHelper.getStringValueOf(service, "@name");
						String method   = XMLHelper.getStringValueOf(service, "@method");
						String url      = XMLHelper.getStringValueOf(service, "@url");
						String headers  = XMLHelper.getStringValueOf(service, "@headers");
						String payload  = XMLHelper.getStringValueOf(service, "@payload");
						String proxy    = XMLHelper.getStringValueOf(service, "@proxy");
						String disabled = XMLHelper.getStringValueOf(service, "@disabled");
						
						// Busca el servicio por si ya existe.
						String serviceId = XMLHelper.getStringValueOf(currentServices, String.format(XPATH_GET_SERVICEID_BY_URL, url));
						
						// Si el servicio no existe
						if (Validator.isNull(serviceId))
						{
							// Crea un nuevo Id.
							serviceId = SQLQueries.getUUID();
							
							// Añade el servicio a la query de inserción.
							sql.append(String.format(SQL_INSERT_SERVICES_VALUES, serviceId, _siteId, name, method, url, headers, payload, proxy, disabled));
						}
					}
					
					// Si hay servicios a insertar...
					if (SQL_INSERT_SERVICES.length() < sql.length())
					{
						// Elimina la última coma y el salto de línea y añade la sentencia de actualización.
						sql.deleteCharAt(sql.length() - 2);
						sql.append(SQL_INSERT_SERVICES_UPDATE);
						
						// Inserta / Actualiza el servicio.
						PortalLocalServiceUtil.executeUpdateQuery(sql.toString());
					}
				}
			}
			catch (Throwable th)
			{
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_WEBSITE_IMPORT_SERVICES_ZYX, th.getMessage(), th.getStackTrace());
			}
		}
	}
	
	public static void removePortlets(LayoutTypePortlet layoutTypePortlet) throws SystemException, PortalException
	{
		long plid 		= layoutTypePortlet.getLayout().getPlid();
		long userId 	= GroupMgr.getDefaultUserId();
		long companyId 	= IterGlobal.getCompanyId();
		
		List<Portlet> portlets = layoutTypePortlet.getPortlets();
		for (Portlet portlet : portlets) 
		{	
			layoutTypePortlet.removePortletId(userId, portlet.getPortletId(), true, false);

            ResourceLocalServiceUtil.deleteResource(companyId, portlet.getRootPortletId(), ResourceConstants.SCOPE_INDIVIDUAL, 
            										PortletPermissionUtil.getPrimaryKey(plid, portlet.getPortletId()));
		}
	}
	
	private void importModel(long id, Document content) throws PortalException, SystemException, ServiceError
	{
		if (_log.isDebugEnabled()) _log.debug( String.format("Starting model import (id_=%d) in website %d", id, _siteId) );
		
		PageTemplate template = PageTemplateLocalServiceUtil.getPageTemplate(id);
		importLayout(template.getLayoutId(), content);
		
		if (_log.isDebugEnabled()) _log.debug( String.format("The model import (id_=%d) in website %d has finished", id, _siteId) );
	}
	
	private void importCatalogPage(String catalogPageId, Document content) throws ServiceError, NumberFormatException, PortalException, SystemException
	{
		if (_log.isDebugEnabled()) _log.debug( String.format("Starting catalog page import (catalogpageid=%d) in website %d", catalogPageId, _siteId) );
		
		List<Object> plid = PortalLocalServiceUtil.executeQueryAsList(String.format(SEL_PLID_BY_CATALOGELEMENTID, catalogPageId));
		ErrorRaiser.throwIfFalse(plid != null && plid.size() > 0, IterErrorKeys.XYZ_E_CATALOG_PAGE_PLID_NOT_EXISTS_ZYX);
		
		importLayout( Long.valueOf(String.valueOf(plid.get(0))) , content);
		
		if (_log.isDebugEnabled()) _log.debug( String.format("The catalog page import (catalogpageid=%d) in website %d has finished", catalogPageId, _siteId) );
	}
	
	@Override
	protected void track(String prefix, Document data) throws IOException
	{
		super.track("WS_Imp_".concat(prefix), data);
	}
}
