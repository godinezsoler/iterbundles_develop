package com.protecmedia.iter.xmlio.service.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;

import com.liferay.portal.NoSuchLayoutException;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
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
import com.liferay.portal.model.LayoutTemplateConstants;
import com.liferay.portal.model.LayoutTypePortlet;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.LayoutTemplateLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.PortletPreferencesLocalServiceUtil;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portal.util.PortletPreferencesTools;
import com.liferay.portlet.asset.model.AssetCategoryConstants;
import com.liferay.portlet.asset.model.AssetVocabularyConstants;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.designer.NoSuchPageTemplateException;
import com.protecmedia.iter.designer.model.PageTemplate;
import com.protecmedia.iter.designer.service.PageTemplateLocalServiceUtil;

public class WebsiteExportMgr extends WebsiteIOMgr
{
	private static Log _log 		= LogFactoryUtil.getLog(WebsiteExportMgr.class);
	
	private AtomicBoolean 	_isAborted	= new AtomicBoolean();
	private Document		_exportDom	= null;
	private Document 		_specDom	= null;
	private Element			_elemObjs	= null;
	
	private Set<String>		_dependenciesLayouts 		= null;
	private Set<String>		_dependenciesPortletItems 	= null;
	private Set<String>		_dependenciesTpls 			= null;
	private Set<String>		_dependenciesServices       = null;
	private Set<String>		_liferayTpls				= null;
	
	private static final String SEL_PORTLETITEM_BY_USERNAME = new StringBuilder(
		"-- Devuelve los campos del primer PortletItem con dicho userName	\n").append(
		"SELECT name, portletId, userName									\n").append(
		"FROM PortletItem													\n").append(
		"    WHERE userName IN ('%s')										\n").append(
		"LIMIT 1").toString();

	private static final String SEL_LAYOUT_BY_UUID = new StringBuilder(
	"SELECT plid, uuid_			\n").append(
	"FROM Layout				\n").append(
	"	WHERE uuid_ IN ('%s')	\n").toString();

	private static final String SEL_TPL_BY_TPLID = new StringBuilder(
	"SELECT tplid, tplname		\n").append(
	"FROM LayoutTemplate		\n").append(
	"	WHERE tplid IN ('%s')	\n").toString();

	private static final String SEL_SERVICE_BY_SERVICEID = new StringBuilder(
	"SELECT serviceId, name, method, url, headers, payload, proxy, disabled \n").append(
	"FROM externalservice WHERE serviceId IN ('%s') "                          ).toString();

	public WebsiteExportMgr(long siteId) throws ServiceError, PortalException, SystemException
	{
		super(siteId);
		
		_dependenciesLayouts 		= new HashSet<String>();
		_dependenciesPortletItems 	= new HashSet<String>();
		_dependenciesTpls			= new HashSet<String>();
		_dependenciesServices       = new HashSet<String>();
		_liferayTpls				= LayoutTemplateLocalServiceUtil.getLiferayLayoutTemplatesIds();
	}
	
	/**
	 * 
	 * @param specDom
	 * @return
	 * @throws DocumentException
	 * @throws ServiceError
	 * @throws SystemException 
	 * @throws NumberFormatException 
	 * @throws IOException 
	 * @throws NoSuchMethodException 
	 * @throws ClientProtocolException 
	 * @throws UnsupportedEncodingException 
	 * @throws SecurityException 
	 * @throws PortalException 
	 */
	public Document exportObjects(Document specDom) throws DocumentException, ServiceError, NumberFormatException, SystemException, PortalException, SecurityException, UnsupportedEncodingException, ClientProtocolException, NoSuchMethodException, IOException
	{
		_log.info( String.format("Starting export from website %d", _siteId) );
		track("ExportObjects-Input", specDom);
		
		_exportDom = SAXReaderUtil.read("<root><pre><content><preinfo/></content></pre><objects/><post/></root>");
		_elemObjs  = (Element)_exportDom.selectSingleNode("/root/objects");
		
		List<Node> objs = specDom.selectNodes( String.format("/objects[@siteid='%d']/obj", _siteId) );
		if (_log.isDebugEnabled()) _log.debug( String.format("The website have to export %d objects", objs.size()) );
		
		for (Node obj : objs)
		{
			exportObject( (Element)obj );
		}
		
		// Tras exportar todos los elementos, se exporta la información que debe viajar en el Pre 
		exportDependenciesPortletItems();
		exportDependenciesLayouts();
		exportDependenciesTpls();
		exportDependenciesServices();
		
		// Una vez exportados todos los elementos, se codifica la información previa (que recibe el pre-proceso "importPreProcessInfo").
		Element preInfo = (Element)_exportDom.selectSingleNode("/root/pre/content/preinfo").detach();
		_exportDom.selectSingleNode("/root/pre/content").setText( Base64.encodeBase64String(preInfo.asXML().getBytes()) );
		
		_exportDom.setXMLEncoding("ISO-8859-1");
		
		track("ExportObjects-Output", _exportDom);
		_log.info( String.format("The export from website %d has finished", _siteId) );
		return _exportDom;
	}
	
	/**
	 * 
	 * @param specDom
	 * @return
	 * @throws DocumentException 
	 * @throws ServiceError 
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws NumberFormatException 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws IOException 
	 */
	public Document computeXPortDependencies(Document specDom) throws DocumentException, NumberFormatException, PortalException, SystemException, ServiceError, SecurityException, NoSuchMethodException, IOException
	{
		_log.info( String.format("Starting compute export dependencies from website %d", _siteId) );
		track("ComputeDependencies-Input", specDom);
		
		_exportDom = SAXReaderUtil.read("<root><objects/></root>");
		_elemObjs  = (Element)_exportDom.selectSingleNode("/root/objects");
		
		_specDom = specDom;
		List<Node> objs = specDom.selectNodes( String.format("/objects[@siteid='%d']/obj", _siteId) );
		if (_log.isDebugEnabled()) _log.debug( String.format("The website have to compute export dependencies for %d objects", objs.size()) );
		
		for (Node obj : objs)
		{
			checkAbort();
			
			String objId 	= XMLHelper.getTextValueOf(obj, "@id");
			ErrorRaiser.throwIfFalse( Validator.isNotNull(objId) && !objId.equals("0"), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			String objClass = XMLHelper.getTextValueOf(obj, "@class", "").toLowerCase();
			ErrorRaiser.throwIfFalse( !objClass.isEmpty() && SUPPORTED_CLASSES.contains(objClass), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			if (objClass.equals(LayoutConstants.CLASS_LAYOUT))
				computeLayoutDependencies( Long.parseLong(objId) );
			
			else if (objClass.equals(LayoutConstants.CLASS_MODEL))
				computeModelDependencies( Long.parseLong(objId) );
			
			else if (objClass.equals(LayoutConstants.CLASS_CATALOG))
				computeCatalogPageDependencies( objId );
		}

		_exportDom.setXMLEncoding("ISO-8859-1");
		
		track("ComputeDependencies-Output", _exportDom);
		_log.info( String.format("The compute export dependencies from website %d has finished", _siteId) );
		return _exportDom;
	}
	
	public void abortExport()
	{
		_isAborted.set(true);
		_log.info( String.format("The export from website %d has been aborted", _siteId) );
	}

	/**
	 * 
	 * @param obj
	 * @throws ServiceError
	 * @throws SystemException 
	 * @throws NumberFormatException 
	 * @throws IOException 
	 * @throws NoSuchMethodException 
	 * @throws ClientProtocolException 
	 * @throws UnsupportedEncodingException 
	 * @throws SecurityException 
	 * @throws PortalException 
	 * @throws DocumentException 
	 */
	private void exportObject(Element obj) throws ServiceError, NumberFormatException, SystemException, PortalException, SecurityException, UnsupportedEncodingException, ClientProtocolException, NoSuchMethodException, IOException, DocumentException
	{
		// Antes de exportar cada objeto comprueba si el proceso de exportación NO ha sido abortado
		checkAbort();
		
		String objId 	= XMLHelper.getTextValueOf(obj, "@id");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(objId) && !objId.equals("0"), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String objClass = XMLHelper.getTextValueOf(obj, "@class", "").toLowerCase();
		ErrorRaiser.throwIfFalse( !objClass.isEmpty() && SUPPORTED_CLASSES.contains(objClass), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document dom = null;
		if (objClass.equals(LayoutConstants.CLASS_LAYOUT))
			dom = exportLayout( Long.parseLong(objId) );
		
		else if (objClass.equals(LayoutConstants.CLASS_MODEL))
			dom = exportModel( Long.parseLong(objId) );
		
		else if (objClass.equals(LayoutConstants.CLASS_CATALOG))
			dom = exportCatalogPage( objId );
		
		if (dom != null)
		{
			// Se ha exportado correctamente un objeto. Se añade el nodo al DOM de resultados y el contenido de la exportación
			Element content = obj.addElement("content");
			content.addText( Base64.encodeBase64String(dom.asXML().getBytes()) );
			_elemObjs.add( obj.detach() );
		}
	}
	
	/**
	 * 
	 * @param plid
	 * @return
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws NoSuchMethodException 
	 * @throws DocumentException 
	 * @throws SecurityException 
	 */
	private void computeLayoutDependencies(long plid) throws PortalException, SystemException, SecurityException, DocumentException, NoSuchMethodException
	{
		if (_log.isDebugEnabled()) _log.debug( String.format("Starting layout compute dependencies (plid=%d) from website %d", plid, _siteId) );

		try
		{
			Layout layout = LayoutLocalServiceUtil.getLayout(plid);
			LayoutTypePortlet layoutTypePortlet = (LayoutTypePortlet) layout.getLayoutType();
			
			// Los TPLs son dependencias a tener en cuenta
			addTpl(layoutTypePortlet.getLayoutTemplateId());
			
			List<Portlet> portlets = layoutTypePortlet.getPortlets();
			for (Portlet portlet : portlets) 
			{
				computePortletDependencies(layoutTypePortlet, portlet);
			}
			
			// Se añade el TPL del layout y los de los portlets anidados
			for (String tplId : _dependenciesTpls)
				addDependencies(tplId, LayoutTemplateConstants.CLASS_TPL);
		}
		catch (NoSuchLayoutException e)
		{
			// Si no existe el Layout NO se exporta
			_log.error( String.format("The layout with plid = %d doesn't exist", plid), e );
		}

		if (_log.isDebugEnabled()) _log.debug( String.format("The layout compute dependencies (plid=%d) from website %d has finished", plid, _siteId) );
	}
	
	/**
	 * 
	 * @param id
	 * @return
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws NoSuchMethodException 
	 * @throws DocumentException 
	 * @throws SecurityException 
	 */
	private void computeModelDependencies(long id) throws PortalException, SystemException, SecurityException, DocumentException, NoSuchMethodException
	{
		if (_log.isDebugEnabled()) _log.debug( String.format("Starting model compute dependencies (id_=%d) from website %d", id, _siteId) );

		try
		{
			PageTemplate template = PageTemplateLocalServiceUtil.getPageTemplate(id);
			computeLayoutDependencies(template.getLayoutId());
		}
		catch (NoSuchPageTemplateException e)
		{
			// Si no existe el modelo NO se exporta
			_log.error( String.format("The model with id = %d doesn't exist", id), e );
		}
		
		if (_log.isDebugEnabled()) _log.debug( String.format("The model compute dependencies (id_=%d) from website %d has finished", id, _siteId) );
	}
	
	/**
	 * 
	 * @param catalogPageId
	 * @return
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws NumberFormatException 
	 * @throws NoSuchMethodException 
	 * @throws DocumentException 
	 * @throws SecurityException 
	 */
	private void computeCatalogPageDependencies(String catalogPageId) throws NumberFormatException, PortalException, SystemException, SecurityException, DocumentException, NoSuchMethodException
	{
		if (_log.isDebugEnabled()) _log.debug( String.format("Starting catalog page compute dependencies (catalogpageid=%d) from website %d", catalogPageId, _siteId) );
		
		List<Object> plid = PortalLocalServiceUtil.executeQueryAsList(String.format(SEL_PLID_BY_CATALOGELEMENTID, catalogPageId));
		if (plid == null || plid.size() == 0)
			_log.error( String.format("The catalogelement with id = %s doesn't exist", catalogPageId) );
		else
			computeLayoutDependencies( Long.valueOf(String.valueOf(plid.get(0))) );
		
		if (_log.isDebugEnabled()) _log.debug( String.format("The catalog page compute dependencies (catalogpageid=%d) from website %d has finished", catalogPageId, _siteId) );
	}

	/**
	 * @param plid
	 * @throws SystemException 
	 * @throws IOException 
	 * @throws NoSuchMethodException 
	 * @throws ClientProtocolException 
	 * @throws UnsupportedEncodingException 
	 * @throws SecurityException 
	 * @throws PortalException 
	 * @throws DocumentException 
	 */
	private Document exportLayout(long plid) throws SystemException, PortalException, SecurityException, UnsupportedEncodingException, ClientProtocolException, NoSuchMethodException, IOException, DocumentException, ServiceError
	{
		if (_log.isDebugEnabled()) _log.debug( String.format("Starting layout export (plid=%d) from website %d", plid, _siteId) );
		Document dom = null;
		
		try
		{
			Layout layout = LayoutLocalServiceUtil.getLayout(plid);
			LayoutTypePortlet layoutTypePortlet = (LayoutTypePortlet) layout.getLayoutType();
			
			dom = SAXReaderUtil.read("<layout/>");
			Element root = dom.getRootElement();
			root.addAttribute("templateid", layoutTypePortlet.getLayoutTemplateId());
			addTpl(layoutTypePortlet.getLayoutTemplateId());
			
			List<Portlet> portlets = layoutTypePortlet.getPortlets();
			for (Portlet portlet : portlets) 
			{
				root.add( exportPortlet(layoutTypePortlet, portlet) );
			}
		}
		catch (NoSuchLayoutException e)
		{
			// Si no existe el Layout NO se exporta
			_log.error( String.format("The layout with plid = %d doesn't exist", plid), e );
		}
		
		if (_log.isDebugEnabled()) _log.debug( String.format("The layout export (plid=%d) from website %d has finished", plid, _siteId) );
		return dom;
	}
	
	/**
	 * 
	 * @param portlet
	 * @return
	 * @throws PortalException 
	 * @throws SystemException 
	 * @throws DocumentException 
	 * @throws ServiceError 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 */
	private Element exportPortlet(LayoutTypePortlet layoutTypePortlet, Portlet portlet) throws SystemException, PortalException, DocumentException, SecurityException, NoSuchMethodException, ServiceError
	{
		Element portletElem = SAXReaderUtil.createElement("portlet");
		
		String pluginId = portlet.getPluginId().equals(PortletKeys.NESTED_PORTLETS) ? portlet.getPortletId() : portlet.getPluginId();
		portletElem.addAttribute("pluginid", pluginId);
		
		portletElem.addAttribute("columnid", layoutTypePortlet.getColumn(portlet.getPortletId()));
		
		javax.portlet.PortletPreferences  preferences = PortletPreferencesLocalServiceUtil.getPreferences(GroupMgr.getCompanyId(), 
															PortletKeys.PREFS_OWNER_ID_DEFAULT, PortletKeys.PREFS_OWNER_TYPE_LAYOUT, 
															layoutTypePortlet.getLayout().getPlid(), portlet.getPortletId());
		
		Document prefDom = SAXReaderUtil.read( PortletPreferencesLocalServiceUtil.toXML(preferences) );
		
		// Comprueba si se trata de una preferencia vincula a un PortletItem
		findDependenciesPortletItems(prefDom);
		
		// Comprueba si se trata de una preferencia asociada a un Servicio Externo
		findDependenciesExternalServices(prefDom);
		
		findDependenciesLayouts(prefDom);
		
		List<Node> layoutTemplates = prefDom.selectNodes(PortletPreferencesTools.PREF_TPLS);
		for (Node layoutTemplate : layoutTemplates)
			addTpl(layoutTemplate.getStringValue());
		
		portletElem.add( prefDom.getRootElement().detach() );
		
		return portletElem;
	}
	
	/**
	 * 
	 * @param layoutTypePortlet
	 * @param portlet
	 * @throws DocumentException 
	 * @throws PortalException 
	 * @throws SystemException 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 */
	private void computePortletDependencies(LayoutTypePortlet layoutTypePortlet, Portlet portlet) throws DocumentException, SystemException, PortalException, SecurityException, NoSuchMethodException
	{
		javax.portlet.PortletPreferences  preferences = PortletPreferencesLocalServiceUtil.getPreferences(GroupMgr.getCompanyId(), 
															PortletKeys.PREFS_OWNER_ID_DEFAULT, PortletKeys.PREFS_OWNER_TYPE_LAYOUT, 
															layoutTypePortlet.getLayout().getPlid(), portlet.getPortletId());
		
		Document prefDom = SAXReaderUtil.read( PortletPreferencesLocalServiceUtil.toXML(preferences) );
		
		if (portlet.getRootPortletId().equalsIgnoreCase(PortletKeys.PORTLET_CATALOG))
		{
			// Catálogos
			List<Node> elementDOMs = prefDom.selectNodes(PortletPreferencesTools.PREFS_VALUE_XPATH_CATALOG_TABS);
			for (Node elementDOM : elementDOMs)
			{
				String elementId = XMLHelper.getTextValueOf( SAXReaderUtil.read(elementDOM.getStringValue()), "/tab/@elementid" );
				addDependencies(elementId, LayoutConstants.CLASS_CATALOG);
			}
		}
		else
		{
			// No se incluyen las secciones configuradas como origen de datos si:
			// Se trata de un Alert y está configurado para alimentarse por metadatos
			// Se trata de otro portlet y se alimenta de la sección actual
			boolean excludeLayoutIDs = (portlet.getRootPortletId().equalsIgnoreCase(PortletKeys.PORTLET_ALERT) &&
										PortletPreferencesTools.isMetadataContextEnable(prefDom)) ||
										PortletPreferencesTools.isDefaultLayoutEnable(prefDom);
			
			// Layouts. Si se está alimentando de la sección actual NO se incluirán las secciones que están desactivadas
			List<Node> layoutUUIDs = (excludeLayoutIDs) 																? 
										prefDom.selectNodes(PortletPreferencesTools.PREFS_VALUE_XPATH_LAYOUTS_NOT_IDS) 	:
										prefDom.selectNodes(PortletPreferencesTools.PREFS_VALUE_XPATH_LAYOUTS);
										
			if (layoutUUIDs.size() > 0)
			{
				String sql = String.format( SEL_LAYOUT_BY_UUID, StringUtils.join(XMLHelper.getStringValues(layoutUUIDs, "."), "','") );
				List<Node> plidList = PortalLocalServiceUtil.executeQueryAsDom(sql).selectNodes("/rs/row/@plid");
				for (Node plid : plidList)
					addDependencies(plid.getStringValue(), LayoutConstants.CLASS_LAYOUT);
			}
			
			// Modelo
			Node modelId = prefDom.selectSingleNode(PortletPreferencesTools.PREFS_VALUE_XPATH_MODELID);
			if (modelId != null)
				addDependencies(modelId.getStringValue(), LayoutConstants.CLASS_MODEL);
			
			// Vocabularios
			List<Node> vocabularies = prefDom.selectNodes(PortletPreferencesTools.PREFS_VALUE_XPATH_VOCABULARIES);
			for (Node vocabulary : vocabularies)
				addDependencies(vocabulary.getStringValue(), AssetVocabularyConstants.CLASS_VOCABULARY);

			// Categorías
			List<Node> categories = prefDom.selectNodes(PortletPreferencesTools.PREFS_VALUE_XPATH_CATEGORIES);
			for (Node category : categories)
				addDependencies(category.getStringValue(), AssetCategoryConstants.CLASS_CATEGORY);
			
			// Calificaciones
			List<Node> qualifications = prefDom.selectNodes(PortletPreferencesTools.PREFS_VALUE_XPATH_QUALIFICATIONS);
			for (Node qualification : qualifications)
				addDependencies(qualification.getStringValue(), "qualification");
			
			// Productos
			List<Node> products = prefDom.selectNodes(PortletPreferencesTools.PREFS_VALUE_XPATH_PRODUCTS);
			for (Node product : products)
				addDependencies(product.getStringValue(), "subscription");
			
			// JournalTemplates
			List<Node> journalTemplates = prefDom.selectNodes(PortletPreferencesTools.PREFS_VALUE_XPATH_JOURNALTEMPLATES);
			for (Node journalTemplate : journalTemplates)
				addDependencies(journalTemplate.getStringValue(), "vmtpl");
			
			// LayoutTemplates
			List<Node> layoutTemplates = prefDom.selectNodes(PortletPreferencesTools.PREF_TPLS);
			for (Node layoutTemplate : layoutTemplates)
				addTpl(layoutTemplate.getStringValue());
		}
	}
	
	/**
	 * Si el objeto no se ha añadido como dependencia se añade, y si además implica una búsqueda recursiva (Layout, Model, Catalog), 
	 * se buscan recursivamente dependencias en él.
	 * 
	 * @param id
	 * @param className
	 * @throws NumberFormatException
	 * @throws PortalException
	 * @throws SystemException
	 * @throws NoSuchMethodException 
	 * @throws DocumentException 
	 * @throws SecurityException 
	 */
	private void addDependencies(String id, String className) throws NumberFormatException, PortalException, SystemException, SecurityException, DocumentException, NoSuchMethodException
	{
		if (	// Sea un ID válido
				Validator.isNotNull(id) && 
				// No se haya incluido como dependencia previamente
				XMLHelper.getLongValueOf(_elemObjs, String.format("count(obj[@id='%s' and @class='%s'])",	id, className)) == 0 &&
				// No sea uno de los elementos de los que se quiere obtener las dependencias
				XMLHelper.getLongValueOf(_specDom,  String.format("count(//obj[@id='%s' and @class='%s'])", id, className)) == 0
		   )
		{
			// No existe, se añade
			Element obj = _elemObjs.addElement("obj");
			obj.addAttribute("id", 		id);
			obj.addAttribute("class", 	className);
			
			if (className.equals(LayoutConstants.CLASS_LAYOUT))
				computeLayoutDependencies( Long.parseLong(id) );
			
			else if (className.equals(LayoutConstants.CLASS_MODEL))
				computeModelDependencies( Long.parseLong(id) );
			
			else if (className.equals(LayoutConstants.CLASS_CATALOG))
				computeCatalogPageDependencies( id );
		}
	}
	
	/**
	 * Si se trata de una preferencia compartida (vinculada a un PortletItem), se exporta la información necesaria de dicho portletItem
	 * @param preferences
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws ServiceError
	 */
	private void findDependenciesPortletItems(Node preferences) throws SecurityException, NoSuchMethodException, ServiceError
	{
		String portletItemUUID = XMLHelper.getTextValueOf(preferences, PortletPreferencesTools.PREFS_PORTLETITEM_VALUE_XPATH);
		if (Validator.isNotNull(portletItemUUID))
			_dependenciesPortletItems.add(portletItemUUID);
	}

	private void findDependenciesExternalServices(Node preferences) throws SecurityException, NoSuchMethodException, ServiceError
	{
		String externalServiceUUID = XMLHelper.getTextValueOf(preferences, PortletPreferencesTools.PREFS_PORTLETITEM_SERVICE_XPATH);
		if (Validator.isNotNull(externalServiceUUID))
			_dependenciesServices.add(externalServiceUUID);
	}
	
	private void findDependenciesLayouts(Node preferences)
	{
		List<Node> layoutUUIDs = preferences.selectNodes(PortletPreferencesTools.PREFS_VALUE_XPATH_LAYOUTS);
		for (Node layoutUUID : layoutUUIDs)
			_dependenciesLayouts.add( layoutUUID.getStringValue() ); 
	}
	
	private void exportDependenciesPortletItems() throws SecurityException, NoSuchMethodException
	{
		String sql = String.format(SEL_PORTLETITEM_BY_USERNAME, StringUtil.merge( _dependenciesPortletItems, "','" ));
		Document dom = PortalLocalServiceUtil.executeQueryAsDom(sql, XMLHelper.colAsAttr, "portletitems", "portletitem");
		
		((Element)_exportDom.selectSingleNode("/root/pre/content/preinfo")).add( dom.getRootElement().detach() );
	}
	
	private void exportDependenciesLayouts() throws SecurityException, NoSuchMethodException
	{
		String sql = String.format(SEL_LAYOUT_BY_UUID, StringUtil.merge( _dependenciesLayouts, "','" ));
		Document dom = PortalLocalServiceUtil.executeQueryAsDom(sql, XMLHelper.colAsAttr, "layouts", "layout");
		
		((Element)_exportDom.selectSingleNode("/root/pre/content/preinfo")).add( dom.getRootElement().detach() );
	}
	
	private void exportDependenciesTpls() throws SecurityException, NoSuchMethodException
	{
		String sql = String.format(SEL_TPL_BY_TPLID, StringUtil.merge( _dependenciesTpls, "','" ));
		Document dom = PortalLocalServiceUtil.executeQueryAsDom(sql, XMLHelper.colAsAttr, "tpls", "tpl");
		
		((Element)_exportDom.selectSingleNode("/root/pre/content/preinfo")).add( dom.getRootElement().detach() );
	}
	
	private void exportDependenciesServices() throws SecurityException, NoSuchMethodException
	{
		String sql = String.format(SEL_SERVICE_BY_SERVICEID, StringUtil.merge( _dependenciesServices, "','" ));
		Document dom = PortalLocalServiceUtil.executeQueryAsDom(sql, XMLHelper.colAsAttr, "services", "service");
		
		((Element)_exportDom.selectSingleNode("/root/pre/content/preinfo")).add( dom.getRootElement().detach() );
	}

	/**
	 * @param id
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws ServiceError 
	 * @throws DocumentException 
	 * @throws IOException 
	 * @throws NoSuchMethodException 
	 * @throws ClientProtocolException 
	 * @throws UnsupportedEncodingException 
	 * @throws SecurityException 
	 */
	private Document exportModel(long id) throws PortalException, SystemException, SecurityException, UnsupportedEncodingException, ClientProtocolException, NoSuchMethodException, IOException, DocumentException, ServiceError
	{
		if (_log.isDebugEnabled()) _log.debug( String.format("Starting model export (id_=%d) from website %d", id, _siteId) );
		Document dom = null;
		
		try
		{
			PageTemplate template = PageTemplateLocalServiceUtil.getPageTemplate(id);
			dom = exportLayout(template.getLayoutId());
		}
		catch (NoSuchPageTemplateException e)
		{
			// Si no existe el modelo NO se exporta
			_log.error( String.format("The model with id = %d doesn't exist", id), e );
		}
		
		if (_log.isDebugEnabled()) _log.debug( String.format("The model export (id_=%d) from website %d has finished", id, _siteId) );
		return dom;
	}

	/**
	 * @param catalogPageId
	 * @throws ServiceError 
	 * @throws DocumentException 
	 * @throws IOException 
	 * @throws NoSuchMethodException 
	 * @throws ClientProtocolException 
	 * @throws UnsupportedEncodingException 
	 * @throws SecurityException 
	 * @throws PortalException 
	 * @throws SystemException 
	 * @throws NumberFormatException 
	 */
	private Document exportCatalogPage(String catalogPageId ) throws NumberFormatException, SystemException, PortalException, SecurityException, UnsupportedEncodingException, ClientProtocolException, NoSuchMethodException, IOException, DocumentException, ServiceError
	{
		if (_log.isDebugEnabled()) _log.debug( String.format("Starting catalog page export (catalogpageid=%d) from website %d", catalogPageId, _siteId) );
		Document dom = null;
		
		List<Object> plid = PortalLocalServiceUtil.executeQueryAsList(String.format(SEL_PLID_BY_CATALOGELEMENTID, catalogPageId));
		if (plid == null || plid.size() == 0)
			_log.error( String.format("The catalogelement with id = %s doesn't exist", catalogPageId) );
		else
			dom = exportLayout( Long.valueOf(String.valueOf(plid.get(0))) );
		
		if (_log.isDebugEnabled()) _log.debug( String.format("The catalog page export (catalogpageid=%d) from website %d has finished", catalogPageId, _siteId) );
		return dom;
	}
	
	/**
	 * Si en algún momento se detecta que la operación ha sido abortada, se lanza un error bien conocido
	 * @throws ServiceError
	 */
	private void checkAbort() throws ServiceError
	{
		ErrorRaiser.throwIfFalse( !_isAborted.get(), IterErrorKeys.XYZ_E_WEBSITE_EXPORT_HAS_BEEN_ABORTED_ZYX);
	}

	@Override
	protected void track(String prefix, Document data) throws IOException
	{
		super.track("WS_Exp_".concat(prefix), data);
	}
	
	private void addTpl(String tplId)
	{
		if (!_liferayTpls.contains(tplId))
			_dependenciesTpls.add(tplId);
	}
}
