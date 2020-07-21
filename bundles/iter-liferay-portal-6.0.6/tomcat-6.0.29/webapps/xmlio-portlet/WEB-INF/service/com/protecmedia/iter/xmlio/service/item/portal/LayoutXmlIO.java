/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.service.item.portal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.client.ClientProtocolException;

import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.CatalogQueries;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.model.LayoutTypePortlet;
import com.liferay.portal.model.PortletConstants;
import com.liferay.portal.model.PortletPreferences;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.ResourceLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.permission.PortletPermissionUtil;
import com.liferay.portal.service.persistence.LayoutUtil;
import com.liferay.portal.service.persistence.PortletPreferencesUtil;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portlet.expando.model.ExpandoRow;
import com.liferay.portlet.expando.model.ExpandoTable;
import com.liferay.portlet.expando.model.ExpandoValue;
import com.liferay.portlet.expando.service.ExpandoRowLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoTableLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoValueLocalServiceUtil;
import com.liferay.portlet.expando.service.persistence.ExpandoRowUtil;
import com.protecmedia.iter.base.service.FormTransformLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.CatalogPublicationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.ChannelLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LivePoolLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.expando.ExpandoRowXmlIO;
import com.protecmedia.iter.xmlio.service.item.expando.ExpandoValueXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

public class LayoutXmlIO extends ItemXmlIO {
	
	private static Log _log = LogFactoryUtil.getLog(LayoutXmlIO.class);
	private String _className = IterKeys.CLASSNAME_LAYOUT;
	private boolean _isValid = true;
	
	public LayoutXmlIO () {		
		super();
	}
	
	public LayoutXmlIO (XMLIOContext xmlIOContext) {
		super(xmlIOContext);
	}	
	
	@Override
	public String getClassName(){
		return _className;
	}
	
	// Función que recibe el xml de la publicación en el LIVE
	@Override
	public void importContents(Element item, Document doc)
	{
		String operation = item.attributeValue("operation");
		if (operation.equals(IterKeys.CREATE) || operation.equals(IterKeys.UPDATE))
		{			
			modify(item, doc);				
		}
		else if (operation.equals(IterKeys.DELETE))
		{
			delete(item);
		}	
	}
	
	@Override
	protected boolean evaluateDependencies(Element item, Document doc) throws DocumentException
	{
		boolean res = true;
		
		try 
		{
			// Parent params
			String parentSGroupId = getAttribute(item, "groupid");
			String parentGlobalId = getAttribute(item, "globalid");	
			long groupId = getGroupId(parentSGroupId);
			Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, parentGlobalId);
					
			List<Element> dependencies = getParamElementListByType(item, "dependency");				
			for (Element dependency : dependencies)
			{
				String globalId = getAttribute(dependency, "name");
				
				String className = getParamTextByName(dependency, "classname");
				String groupName = getParamTextByName(dependency, "groupname");
				
				if (live != null && !live.getStatus().equals(IterKeys.ERROR))
				{
					boolean partialResult = ChannelLocalServiceUtil.importContent(doc, className, groupName, globalId, xmlIOContext, className.equals(IterKeys.CLASSNAME_DLFILEENTRY));
					res = res && partialResult;
				}
				else
				{
					xmlIOContext.itemLog.addMessage(item, globalId, className, IterKeys.CREATE, "Cannot import parent Layout", IterKeys.ERROR, groupName);	
					res = false;
				}
			}
		} 
		catch (PortalException e)
		{
			_log.error(e);
		}
		catch (SystemException e) 
		{
			_log.error(e);
		}
		
		return res;
	}	
	
	
	/*
	 * Live Functions
	 */
	@Override
	public void populateLive(long groupId, long companyId)
			throws SystemException{
		
		List<Layout> layoutList = LayoutLocalServiceUtil.getLayouts(groupId, false);
		for (Layout layout : layoutList){	
			try {
				//1. Populate Layout's ExpandoRow	
				List<ExpandoTable> expandoTableList = ExpandoTableLocalServiceUtil.getTables(companyId, IterKeys.CLASSNAME_LAYOUT);
				ItemXmlIO expandoRowItem = new ExpandoRowXmlIO();
				for (ExpandoTable expandoTable : expandoTableList){
					ExpandoRow expandoRow = ExpandoRowUtil.fetchByT_C(expandoTable.getTableId(), layout.getPrimaryKey());
					if (expandoRow != null){
						try {
							expandoRowItem.createLiveEntry(expandoRow);
						} catch (PortalException e) {
							_log.error("Can't add Live, ExpandoRow: " + expandoRow.getRowId());
						}					
						
						//2. Populate Layout's ExpandoValue
						List<ExpandoValue> expandoValueList = ExpandoValueLocalServiceUtil.getRowValues(expandoRow.getRowId());
						ItemXmlIO expandoValueItem = new ExpandoValueXmlIO();
						for (ExpandoValue expandoValue : expandoValueList){				
							try {
								expandoValueItem.createLiveEntry(expandoValue);
							} catch (PortalException e) {
								_log.error("Can't add Live, ExpandoValue: " + expandoValue.getValueId());
							}
						}
					}
				}
				
				//3. Populate Layout
				createLiveEntry(layout);
				
				//4. Populate Layout's Portlet
				try {
					createLiveEntryPortlet(layout);
				} catch (PortalException e) {
					_log.error("Cannot populate live with Portlets from Layout " + layout.getFriendlyURL());
				}
				
			} catch (PortalException e) {
				_log.error("Cannot populate live with Layout: " + layout.getFriendlyURL());
			}
			
		}

	}
	
	/**
	 * 
	 * @param localId
	 * @return Devuelve el globalId que le correspondería a dicho localId
	 */
	private String getGlobalId(String localId)
	{
		return IterLocalServiceUtil.getSystemName() + "_" + localId;
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException
	{
		Layout layout 	= (Layout)model;
		Company company = CompanyLocalServiceUtil.getCompany(layout.getCompanyId());
		String id 		= String.valueOf(layout.getPlid());	
		
		boolean continueCreate = true;
		
		// En el preview si el estado es DRAFT no se podrá actualizar a pending en este método. Solo si se selecciona 
		// "Publish on the web" desde MLN (actualmente llama directamente a LiveServiceUtil.changeLiveStatus)
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
		{
			try
			{
				Live parentLive = LiveLocalServiceUtil.getLiveByLocalId(layout.getGroupId(), _className, String.valueOf(layout.getPlid()));
				continueCreate  = parentLive == null || !parentLive.getStatus().equals(IterKeys.DRAFT);
			}
			catch (Exception e)
			{
				_log.error(e.toString());
			}
		}
		
		if (continueCreate)
		{
			/*
			 * Hay elementos dependientes de Layout que se crean antes que él, 
			 * por lo que hay que establecer las dependencias aqui. 
			 */
			
			//1. Añado Layout
			Live liveLayout = LiveLocalServiceUtil.add(_className, layout.getGroupId(), -1, 0, 
					getGlobalId(id), id, 
					IterKeys.CREATE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
			
			if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
			{
				try
				{
					//2. Actualizo el pool en el ExpandoRow del Layout
					ExpandoTable table = ExpandoTableLocalServiceUtil.getDefaultTable(company.getCompanyId(), IterKeys.CLASSNAME_LAYOUT);
					ExpandoRow row = ExpandoRowLocalServiceUtil.getRow(table.getTableId(), layout.getPlid());
					Live liveRow = LiveLocalServiceUtil.getLiveByLocalId(company.getGroup().getGroupId(), IterKeys.CLASSNAME_EXPANDOROW, String.valueOf(row.getRowId()));
					LivePoolLocalServiceUtil.createLivePool(liveLayout.getId(), liveLayout.getId(), liveRow.getId(), false);
					
					//3. Actualizo el pool en los ExpandoValue del Layout
					List<ExpandoValue> expandoValues = ExpandoValueLocalServiceUtil.getRowValues(row.getRowId());
					for(ExpandoValue expandoValue : expandoValues)
					{
						Live liveValue = LiveLocalServiceUtil.getLiveByLocalId(company.getGroup().getGroupId(), IterKeys.CLASSNAME_EXPANDOVALUE, String.valueOf(expandoValue.getValueId()));
						LivePoolLocalServiceUtil.createLivePool(liveLayout.getId(), liveRow.getId(), liveValue.getId(), false);
					}
				}
				catch(Exception err)
				{
					_log.debug("Expando data cannot be added as dependency of Layout " + layout.getFriendlyURL());
				}
				
				createLiveEntryPortlet(model);
			}
		}
	}

	public void createLiveEntryPortlet(BaseModel<?> model) throws PortalException, SystemException{
		
		Layout layout = (Layout)model;
		Live parentLive = LiveLocalServiceUtil.getLiveByLocalId(layout.getGroupId(), _className, String.valueOf(layout.getPlid()));
		
		String id = parentLive.getLocalId();
		
		//insert element in LIVE
		LiveLocalServiceUtil.add(IterKeys.CLASSNAME_PORTLET, parentLive.getGroupId(), parentLive.getId(), parentLive.getId(), 
				IterLocalServiceUtil.getSystemName() + "_" + id, id,
				IterKeys.CREATE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
	}
	
	@Override
	public void deleteLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		Layout layout = (Layout)model;
		
		String id = String.valueOf(layout.getPlid());
		//insert element in LIVE
		LiveLocalServiceUtil.add(_className, layout.getGroupId(), 
				IterLocalServiceUtil.getSystemName() + "_" + id, id, 
				IterKeys.DELETE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
		
		//Para el portlet asociado
		if (LiveLocalServiceUtil.getLiveByLocalId(layout.getGroupId(), IterKeys.CLASSNAME_PORTLET, id) != null){
			LiveLocalServiceUtil.add(IterKeys.CLASSNAME_PORTLET, layout.getGroupId(), 
					IterLocalServiceUtil.getSystemName() + "_" + id, id, 
					IterKeys.DELETE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
			
		}
	}
	
	
	// Se reutiliza el layout para la publicación de las section properties y de los catálogos (cabecera menú y pie)
	@Override
	protected String createItemXML(XMLIOExport xmlioExport, Element root, String operation, Group group, Live live) 
	{
		_log.trace("In LayoutXmlIO.createItemXML");
		
		StringBuffer error = new StringBuffer();
		
		Map<String, String> attributes = new HashMap<String, String>();
		Map<String, String> params = new HashMap<String, String>();

		setCommonAttributes(attributes, group.getName(), live, operation);			

		try 
		{		
			// Las operaciones de actualización serán en lo adelante Creación para poder generar Layouts instrumentales
			if (operation.equals(IterKeys.UPDATE))
				operation = IterKeys.CREATE;
			
			//Put necessary parameters for each kind of operation.
			if (operation.equals(IterKeys.CREATE))
			{
				String parentFriendlyURL = "";			
				Layout layout = null;
			
				layout = LayoutLocalServiceUtil.getLayout(GetterUtil.getLong(live.getLocalId()));
				
				try 
				{
					Layout parentLayout = LayoutLocalServiceUtil.getLayout(group.getGroupId(), false, layout.getParentLayoutId());
					parentFriendlyURL = parentLayout.getFriendlyURL();					
				} 
				catch (Exception e) 
				{
					//It's a root node doesn't have any parent
				}
				
				//Used for type-settings
				LayoutTypePortlet layoutTypePortlet = (LayoutTypePortlet)layout.getLayoutType();
				
				attributes.put("parentid", parentFriendlyURL);
					
				params.put("description",		layout.getDescription());				
				params.put("url", 				layout.getFriendlyURL());
				params.put("layout-type", 		layout.getType());
				params.put("hidden", 			String.valueOf(layout.getHidden()));
				params.put("private", 			String.valueOf(layout.getPrivateLayout()));		
				params.put("theme", 			layout.getThemeId());
				params.put("color-scheme", 		layout.getColorSchemeId());
				params.put("icon-image", 		String.valueOf(layout.getIconImage()));	
				params.put("icon-image-file", 	XMLIOUtil.exportImageFile(xmlioExport, layout.getIconImageId()));			
				params.put("priority", 			String.valueOf(layout.getPriority()));
				
				String tplId = layoutTypePortlet.getLayoutTemplateId();
				params.put("templateid", tplId);
				
				if (Validator.isNotNull(tplId))
					xmlioExport.addTpl(tplId);				
				
				setTypeSettigsParams(group, params, layout);
				
				// Obtenemos el contexto
				XMLIOContext context = this.getXMLIOContext(null);
				
				// Section properties	
				addSectionPropertiesData(layout, params);
				
				// Modelos web (sólo catálogos de cabecera, menu y pie) 
				addWebModelData(layout, params, context);			

				Locale defaultLocale = LocaleUtil.getDefault();	
				params.put("name", 	layout.getName(defaultLocale));
				params.put("title", layout.getTitle(defaultLocale));
				
				Element itemElement = addNode(root, "item", attributes, params);	
				
				//Si no es de tipo portlet, exportamos sus typeSettings
				if (!layout.getType().equals(LayoutConstants.TYPE_PORTLET))
				{
					error.append(getLayoutTypeProperties(itemElement, group, layout.getTypeSettingsProperties(), live.getId()));
				}
				
				addDependencies(itemElement , live.getId());
				_log.debug("Exporting Layout " + layout.getFriendlyURL());
					
			}
			else if (operation.equals(IterKeys.UPDATE))
			{				
				addDependencies(addNode(root, "item", attributes, params) , live.getId());
			}
			else
			{
				addNode(root, "item", attributes, params);
			}
			
		} 
		catch (Exception e) 
		{
			//LiveLocalServiceUtil.setError(live.getId(), IterKeys.CORRUPT, "Can't export item");
			error.append((error.length()==0?"":";") + "Cannot export item");				
		}
		
		_log.debug("INFO: Layout exported");
			
		return error.toString();
	}
	
	
	private void setTypeSettigsParams(Group group, Map<String, String> params, Layout layout) throws PortalException, SystemException
	{
		String languageId = LocaleUtil.toLanguageId(LocaleUtil.getDefault());
		UnicodeProperties props = layout.getTypeSettingsProperties();
		
		params.put(IterKeys.SITEMAP_PRIORITY, 			props.getProperty(IterKeys.SITEMAP_PRIORITY));
		params.put(IterKeys.SITEMAP_CHANGE_FREQUENCY,	props.getProperty(IterKeys.SITEMAP_CHANGE_FREQUENCY));
		params.put(IterKeys.SITEMAP_INCLUDE, 			String.valueOf( GetterUtil.getBoolean(props.getProperty(IterKeys.SITEMAP_INCLUDE), false) ));
		
		String metaKey = String.format(IterKeys.META_ROBOTS, languageId);
		if(props.containsKey(metaKey))
			params.put(metaKey, props.getProperty(metaKey));
		
		metaKey = String.format(IterKeys.META_KEYWORDS, languageId);
		if(props.containsKey(metaKey))
			params.put(metaKey, props.getProperty(metaKey));
		
		metaKey = String.format(IterKeys.META_DESCRIPTION, languageId);
		if(props.containsKey(metaKey))
			params.put(metaKey, props.getProperty(metaKey));
		
		if(props.containsKey(IterKeys.JAVASCRIPT_1))
			params.put(IterKeys.JAVASCRIPT_1, props.getProperty(IterKeys.JAVASCRIPT_1));
		
		if(props.containsKey(IterKeys.JAVASCRIPT_2))
			params.put(IterKeys.JAVASCRIPT_2, props.getProperty(IterKeys.JAVASCRIPT_2));
		
		if(props.containsKey(IterKeys.JAVASCRIPT_3))
			params.put(IterKeys.JAVASCRIPT_3, props.getProperty(IterKeys.JAVASCRIPT_3));
		
		if(props.containsKey(IterKeys.TARGET))
			params.put(IterKeys.TARGET, props.getProperty(IterKeys.TARGET));
		
		if(props.containsKey(IterKeys.URL))
			params.put(IterKeys.LINK_TO_URL, props.getProperty(IterKeys.URL));
		
		if(props.containsKey(IterKeys.LINK_TO_LAYOUT_ID))
		{
			Layout layoutProp = LayoutLocalServiceUtil.getLayout(group.getGroupId(), false, GetterUtil.getLong(props.getProperty(IterKeys.LINK_TO_LAYOUT_ID)));
			Live liveLayoutProp = LiveLocalServiceUtil.getLiveByLocalId(group.getGroupId(), IterKeys.CLASSNAME_LAYOUT, String.valueOf(layoutProp.getPlid()));
			
			params.put(IterKeys.LINK_TO_LAYOUT_ID, liveLayoutProp.getGlobalId());
		}
		
		if(props.containsKey(IterKeys.GROUPID))
		{
			Live liveProp = LiveLocalServiceUtil.getLiveByLocalId(
													GetterUtil.getLong(props.getProperty(IterKeys.GROUPID)), IterKeys.CLASSNAME_GROUP, props.getProperty(IterKeys.GROUPID));
			
			params.put(IterKeys.GROUPID, liveProp.getGlobalId());
		}
		
		if(props.containsKey(IterKeys.PRIVATE_LAYOUT))
			params.put(IterKeys.PRIVATE_LAYOUT, props.getProperty(IterKeys.PRIVATE_LAYOUT));
	}

	// Añade los atributos de la section properties si es necesario
	private void addSectionPropertiesData(Layout layout, Map<String, String> params) throws SecurityException, NoSuchMethodException, PortalException, SystemException, UnsupportedEncodingException, ClientProtocolException, IOException, com.liferay.portal.kernel.error.ServiceError, DocumentException{
		_log.trace("In addSectionPropertiesData");
		
		Document doc = LayoutLocalServiceUtil.getSectionProperties(layout.getGroupId(), layout.getPlid(), true);
		Node sectionProperties = doc.getRootElement().selectSingleNode("/rs/row");
		
		// Solo si hay section properties que publicar
		if (null != sectionProperties)
		{
			_log.debug("Adding section properties values to the exportation xml");
			
			Live layoutLive = LiveLocalServiceUtil.getLiveByLocalId(layout.getGroupId(), IterKeys.CLASSNAME_LAYOUT, Long.toString(layout.getPlid()));
			
			params.put("layoutGlobalId", layoutLive.getGlobalId());

			params.put("section_properties_plid",    XMLHelper.getTextValueOf(sectionProperties, "@plid"   ));
			
			params.put("section_properties_noheader", XMLHelper.getTextValueOf(sectionProperties, "@noheader"));
			params.put("section_properties_headerelementid", XMLHelper.getTextValueOf(sectionProperties, "@headerelementid"));					
			
			params.put("section_properties_nomenu", XMLHelper.getTextValueOf(sectionProperties, "@nomenu"));
			params.put("section_properties_menuelementid", XMLHelper.getTextValueOf(sectionProperties, "@menuelementid"));
			
			params.put("section_properties_nofooter", XMLHelper.getTextValueOf(sectionProperties, "@nofooter"));
			params.put("section_properties_footerelementid", XMLHelper.getTextValueOf(sectionProperties, "@footerelementid"));
			
			// El id del artículo (articleid) es el mismo en el back y en el live, NO hace falta ir por el globalid
			params.put("section_properties_aboutid", GetterUtil.getString(XMLHelper.getTextValueOf(sectionProperties, "@aboutid"),"0"));
			
			String autorss = XMLHelper.getTextValueOf(sectionProperties, "@autorss", null);					
			params.put("section_properties_autorss", (autorss == null || autorss.equals("0") || autorss.equals("false") ? "0" : "1"));	
			
			String xsslId = XMLHelper.getTextValueOf(sectionProperties, "@autorssxsl");
			params.put("section_properties_autorssxsl", xsslId);
			
			/* Como no tenemos mecanismos para saber si la transformación (xsl) está ya en el LIVE 
			   porque no aparece en la tabla xmlio_live ni su tabla (formxsltransform) tiene fechas de actualización ni de modificación)
			   la publicamos siempre */
			if (Validator.isNotNull(xsslId))
			{
				_log.debug(new StringBuilder("Starts the xssl publication: ").append(xsslId));
				FormTransformLocalServiceUtil.publishToLive(xsslId);					
			}					
		}
	}
	
	// Añade los atributos de los modelos web (menu, cabecera y pie) si es necesario
	private void addWebModelData(Layout layout, Map<String, String> params, XMLIOContext context) throws SecurityException, NoSuchMethodException{
		_log.trace("In addWebModelData");
		
		if (null != context && context.getPublishCatalogs()){
			
			// Obtenemos el catálogo
			String sql = String.format(CatalogQueries.GET_CATALOG_BY_CATALOG_PAGE_PLID, layout.getPlid());			
			Document result = PortalLocalServiceUtil.executeQueryAsDom(sql);
			Node catalog = result.getRootElement().selectSingleNode("/rs/row");		
			
			if (null != catalog){
				_log.debug("Adding catalog to the exportation xml");
				
				// Añadimos los datos del catálgo
				params.put("catalog_catalogid",   XMLHelper.getTextValueOf(catalog, "@catalogid"  ));
				params.put("catalog_groupid",     XMLHelper.getTextValueOf(catalog, "@groupid"    ));
				params.put("catalog_catalogname", XMLHelper.getTextValueOf(catalog, "@catalogname"));
				params.put("catalog_description", XMLHelper.getTextValueOf(catalog, "@description"));
				params.put("catalog_catalogtype", XMLHelper.getTextValueOf(catalog, "@catalogtype"));
				
				// Obtenemos las páginas del catálogo
				sql = String.format(CatalogQueries.GET_CATALOG_PAGE_BY_PLID, layout.getPlid()); 
				result = PortalLocalServiceUtil.executeQueryAsDom(sql);				
				
				List<Node> pages = result.getRootElement().selectNodes("//row");
				
				if (Validator.isNotNull(pages)){
					_log.debug("Adding catalog pages to the exportation xml");
					
					// Añadimos los datos de las páginas de catálogo
					for (int p = 0; p < pages.size(); p++){
						Node page = pages.get(p);
						
						// Como puede haber varias páginas para un mismo catálogo, necesitamos la iteración (p) para distinguir los datos. 
						params.put(new StringBuilder("catalog_page_" + p + "_catalogpageid").toString(), XMLHelper.getTextValueOf(page, "@catalogpageid"));
						params.put(new StringBuilder("catalog_page_" + p + "_catalogid"     ).toString(), XMLHelper.getTextValueOf(page, "@catalogid"    ));
						params.put(new StringBuilder("catalog_page_" + p + "_plid"          ).toString(), XMLHelper.getTextValueOf(page, "@plid"         ));
						params.put(new StringBuilder("catalog_page_" + p + "_pos"           ).toString(), XMLHelper.getTextValueOf(page, "@pos"          ));
					}
					
					// Obtenemos los elementos de las páginas del catálogo
					sql = String.format(CatalogQueries.GET_CATALOG_ELEMENTS_BY_CATALOG_PAGE_PLID, layout.getPlid());
					result = PortalLocalServiceUtil.executeQueryAsDom(sql);					
					List<Node> elements = result.getRootElement().selectNodes("//row");
					
					if (Validator.isNotNull(elements)){
						_log.debug("Adding catalog pages elements to the exportation xml");
						
						// Añadimos los elementos de las páginas
						for (int e = 0; e < elements.size(); e++){
							Node element = elements.get(e);
							
							// Como puede haber varios elementos por página, necesitamos la iteración (e) para distinguir los datos. 
							params.put(new StringBuilder("element_page_" + e + "_catalogelementid").toString(), XMLHelper.getTextValueOf(element, "@catalogelementid"));
							params.put(new StringBuilder("element_page_" + e + "_catalogPageId"   ).toString(), XMLHelper.getTextValueOf(element, "@catalogPageId"   ));
							params.put(new StringBuilder("element_page_" + e + "_columnId"        ).toString(), XMLHelper.getTextValueOf(element, "@columnId"        ));
							params.put(new StringBuilder("element_page_" + e + "_pos"             ).toString(), XMLHelper.getTextValueOf(element, "@pos"             ));
						}
					}
				}
			}
		}
	}
	

	/**
	 * 
	 * @param parentNode
	 * @param group
	 * @param layoutProperties
	 */
	private String getLayoutTypeProperties (Element parentNode, Group group, UnicodeProperties layoutProperties, long liveId)
	{
		StringBuffer error = new StringBuffer();
		Iterator<Entry<String, String>> layoutPropertyIt = layoutProperties.entrySet().iterator();
	
		while (layoutPropertyIt.hasNext()) 
		{
			Map<String, String> propertyAttributes = new HashMap<String, String>();
		
			Map.Entry<String, String> entry = (Map.Entry<String, String>) layoutPropertyIt.next();	
					
			String name = (String) entry.getKey();
			propertyAttributes.put("name", name);
			propertyAttributes.put("type", "property");	
			
			try
			{
				// Evitamos las propiedades nulas
				if (entry.getValue() != null)
				{
					// Si los values son de tipo id pueden NO coincidir en diferentes máquinas y debemos pasar en el xml su globalId.
					if (name.equals("linkToLayoutId"))
					{
						String propValue 	= entry.getValue();
						Layout layoutProp 	= LayoutLocalServiceUtil.getLayout(group.getGroupId(), false, GetterUtil.getLong(propValue));
						ErrorRaiser.throwIfNull(layoutProp, IterErrorKeys.XYZ_E_UNDEFINED_LAYOUT_ZYX, propValue);
						
						Live liveLayoutProp = LiveLocalServiceUtil.getLiveByLocalId(group.getGroupId(), IterKeys.CLASSNAME_LAYOUT, String.valueOf(layoutProp.getPlid()));
						ErrorRaiser.throwIfNull(liveLayoutProp, IterErrorKeys.XYZ_E_UNDEFINED_LAYOUT_ZYX, String.valueOf(layoutProp.getPlid()));
						
						addNode(parentNode, "param", propertyAttributes, liveLayoutProp.getGlobalId());
					}
					else if (name.equals("groupId"))
					{						
						String propValue = entry.getValue();
						
						Live liveProp = LiveLocalServiceUtil.getLiveByLocalId(GetterUtil.getLong(propValue), IterKeys.CLASSNAME_GROUP, propValue);
							
						if (liveProp != null)
						{							
							addNode(parentNode, "param", propertyAttributes, liveProp.getGlobalId());
						}
					}
					else
					{
						addNode(parentNode, "param", propertyAttributes, entry.getValue());		
					}				
				}				
			}
			catch (Exception e)
			{
				_log.error(e);
				
				String errMsg = String.format("%s Can't get item property: %s (%s)", error.length()==0 ? "" : ";", name, e.toString());
				error.append(errMsg);	
			}
		}
		
		return error.toString();
	}
	
	
	// Borrado de la sección. Si tuviese un registro de sectionproperties se borraría por cascade
	@Override
	protected void delete(Element item) 
	{
		String sGroupId = getAttribute(item, "groupid");			
		String globalId = getAttribute(item, "globalid");	
	
		try
		{
			long groupId = getGroupId(sGroupId);		
					
			try 
			{
				//Get live to get the element localId
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
								
				try
				{
					long plid = GetterUtil.getLong(live.getLocalId());
					Layout layout = LayoutLocalServiceUtil.getLayout(plid);
					
					try
					{
						layout = deletePortlets(layout, item, globalId, sGroupId, plid, IterKeys.DELETE);
						
						LayoutLocalServiceUtil.deleteLayout(layout, true);
						
						//update entry in live table
						LiveLocalServiceUtil.add(_className, groupId, globalId,
								live.getLocalId(), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
						
						if (validateLog(sGroupId, globalId))
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Done", IterKeys.DONE, sGroupId);
					} 
					catch (Exception e1) 
					{
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Error: " + e1.toString(), IterKeys.ERROR, sGroupId);
					}
				
				} 
				catch (Exception e1) 
				{
					if (live != null)
					{
						//clean entry in live table
						LiveLocalServiceUtil.add(_className, groupId, globalId,
							live.getLocalId(), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
					}
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Element not found", IterKeys.DONE, sGroupId);
				}
			} 
			catch (Exception e1) 
			{
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Element not found", IterKeys.ERROR, sGroupId);				
			}
		} 
		catch (Exception e) 
		{
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}
	}	

	
	/** <br/> Se localiza el layout a partir del globalId de la tabla Xmlio_Live
		<br/> a) 	Si el layout existe:
		<br/> 1- 	Se crea un Layout instrumental con el typesettings del layout en cuestión
		<br/> 2- 	Se borran los portlets del Layout instrumental
		<br/> 3- 	Se actualizan las propiedades del layout instrumental
		<br/> 4- 	Se añaden los portlets especificados como dependencias al layout instrumental
		<br/> 5- 	Se copian todas las propiedades y referencias a los portlets, del instumental al original en cuestión
		<br/>
		<br/> b) 	Si el layout no existe se crea. En dicho caso NO hará falta un layout instrumental por lo que de descartan
		<br/>		los pasos 1 y 5 */
	// Modificación de un layout
	@Override
	protected void modify(Element item, Document doc) 
	{	
		_log.trace("LayoutXmlIO.modify: [Begin]");
		
		String sGroupId = item.attributeValue("groupid");
		String globalId = item.attributeValue("globalid");	

		try
		{
			long groupId = getGroupId(sGroupId);
			Live live 	 = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
			
			Layout layout = null;
			try
			{
				layout = LayoutLocalServiceUtil.getLayout(GetterUtil.getLong(live.getLocalId()));
			}
			catch (Exception e)	{}
			
			if (layout == null)
			{
				// Entrada en Xmlio_Live a un layout que ya no existe. Se borra el anterior
				if (live != null)
					LiveLocalServiceUtil.deleteLiveById(live.getId());

				String newFriendlyUrl 	= getParamTxtByName(item, "url");		
				String layoutType 		= getParamTxtByName(item, "layout-type");
				String description 		= getParamTxtByName(item, "description", StringPool.BLANK);
				boolean privateLayout 	= GetterUtil.getBoolean(getParamTxtByName(item, "private"), false);
				boolean isHidden 		= GetterUtil.getBoolean(getParamTxtByName(item, "hidden"), 	false);
				long parentId 			= getLayoutIdFromFriendlyURL(groupId, getAttribute(item, "parentid"));
				Locale defaultLocale 	= LocaleUtil.getDefault();
				
				// Se recogen los diferentes idiomas para el Name
				Map<Locale, String> localeNamesMap = new HashMap<Locale, String>();
				localeNamesMap.put(defaultLocale, getParamTxtByName(item, "name"));

				// Se recogen los diferentes idiomas para el Title
				Map<Locale,String> 	localeTitlesMap = new HashMap<Locale, String>();
				localeTitlesMap.put(defaultLocale, getParamTxtByName(item, "title"));

				// ServiceContext Init
				ServiceContext serviceContext = new ServiceContext();		
				serviceContext.setAddCommunityPermissions(true);
				serviceContext.setAddGuestPermissions(true);					
				
				layout = LayoutLocalServiceUtil.addLayout(xmlIOContext.getUserId(), groupId, privateLayout, parentId, localeNamesMap, localeTitlesMap, description, layoutType, isHidden, newFriendlyUrl, serviceContext);
				
				// Se crea o actualiza la entrada en live. Al crear se podrá localizar el Layout si otro lo referencia en una preferencia
				LiveLocalServiceUtil.add(_className, groupId, 0, 0, globalId, String.valueOf(layout.getPlid()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
				
				item.addAttribute("isNew", Boolean.TRUE.toString());
			}
			item.addAttribute("livePlid", String.valueOf(layout.getPlid()));
			
		}
		catch (Exception e) 
		{
			_log.error(e);
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, e.toString(), IterKeys.ERROR, sGroupId);
		}	
		_log.trace("LayoutXmlIO.modify: [End]");
	}
	
	public void importProperties(Element item, Document doc) 
	{
		String 	sGroupId 	= item.attributeValue("groupid");
		String 	globalId 	= item.attributeValue("globalid");
		boolean isNewLayout = GetterUtil.getBoolean( XMLHelper.getTextValueOf(item, "@isNew") );
		long 	plid 		= XMLHelper.getLongValueOf(item, "@livePlid");
		
		try
		{
			ErrorRaiser.throwIfFalse(plid > 0);
			Layout layout = LayoutLocalServiceUtil.getLayout(plid);
			long groupId  = getGroupId(sGroupId);
			
			if (isNewLayout)
			{
				layout = updateLayoutProperties(layout, item, globalId, sGroupId, layout.getPlid());
			}
			else
			{
				String newFriendlyUrl 	= getParamTxtByName(item, "url");	
				String layoutType 		= getParamTxtByName(item, "layout-type");	
				String description 		= getParamTxtByName(item, "description", StringPool.BLANK);
				boolean privateLayout 	= GetterUtil.getBoolean(getParamTxtByName(item, "private"), false);
				boolean isHidden 		= GetterUtil.getBoolean(getParamTxtByName(item, "hidden"), 	false);
				long parentId 			= getLayoutIdFromFriendlyURL(groupId, getAttribute(item, "parentid"));
				Locale defaultLocale 	= LocaleUtil.getDefault();
				
				// Se recogen los diferentes idiomas para el Name
				Map<Locale, String> localeNamesMap = new HashMap<Locale, String>();
				localeNamesMap.put(defaultLocale, getParamTxtByName(item, "name"));

				// Se recogen los diferentes idiomas para el Title
				Map<Locale,String> 	localeTitlesMap = new HashMap<Locale, String>();
				localeTitlesMap.put(defaultLocale, getParamTxtByName(item, "title"));

				// ServiceContext Init
				ServiceContext serviceContext = new ServiceContext();		
				serviceContext.setAddCommunityPermissions(true);
				serviceContext.setAddGuestPermissions(true);					

				// Existe el layout, se crea uno temporal
				String tmpFriendlyURL = StringPool.SLASH+PortalUUIDUtil.generate();
				Layout tmpLayout	  = LayoutLocalServiceUtil.addLayout(xmlIOContext.getUserId(), groupId, privateLayout, parentId, localeNamesMap, localeTitlesMap, description, layoutType, isHidden, tmpFriendlyURL, serviceContext);

				try
				{
					tmpLayout.setTypeSettings( layout.getTypeSettings() );
					tmpLayout = LayoutLocalServiceUtil.updateLayout(tmpLayout, false);
					
					tmpLayout = updateLayoutProperties(tmpLayout, item, globalId, sGroupId, layout.getPlid());
									
					// Se transfiere de un golpe la info del layoout temporal al layout real
					layout = LayoutLocalServiceUtil.updateFriendlyURL(layout.getPlid(), newFriendlyUrl);
					layout = transferLayoutData(tmpLayout, layout);

					// Se borra el layout temporal
					_log.trace(String.format("LayoutXmlIO.modify: Updating layout(plid=%d). Before delete tmp layout(plid=%d)", layout.getPlid(), tmpLayout.getPlid()));
				}
				finally
				{
					LayoutLocalServiceUtil.deleteLayout(tmpLayout.getPlid());
					_log.trace(String.format("LayoutXmlIO.modify: Updating layout(plid=%d) [End]", layout.getPlid()));
				}
			}
			
			// Se crea o actualiza la entrada en live
			LiveLocalServiceUtil.add(_className, groupId, 0, 0, globalId, String.valueOf(layout.getPlid()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
			
			// Actualizamos los modelos web (catálogos de menu, cabecera y pie)
			CatalogPublicationLocalServiceUtil.importCatalogPage(layout.getPlid(), groupId, item);
		}
		catch (Throwable th)
		{
			_log.error(th);
			
			// Si ya ha fallado previamante NO se añade este fallo.
			if (validateLog(sGroupId, globalId))
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, th.toString(), IterKeys.ERROR, sGroupId);
			
			// Si falla la publicación de un nuevo Layout se borra
			if (isNewLayout && plid > 0)
			{
				try
				{
					LayoutLocalServiceUtil.deleteLayout(plid);
				}
				catch (Exception e)
				{
					_log.error(e);
				}
			}
		}
		finally
		{
			// Si no Layout no termina correctamente el Portlet tampoco porque se quiere que para la próxima publicación, se envie toda la info de los portlets
			if (!validateLog(sGroupId, globalId))
				xmlIOContext.itemLog.updateLog(IterKeys.CLASSNAME_PORTLET, sGroupId, globalId, IterKeys.ERROR);
		}
	}
	
	public void importSectionProperties(Element item, Document doc)
	{
		String 	sGroupId 	= item.attributeValue("groupid");
		String 	globalId 	= item.attributeValue("globalid");
		boolean isNewLayout = GetterUtil.getBoolean( XMLHelper.getTextValueOf(item, "@isNew") );
		long 	plid 		= XMLHelper.getLongValueOf(item, "@livePlid");
		
		try
		{
			if (validateLog(sGroupId, globalId))
			{
				ErrorRaiser.throwIfFalse(plid > 0);
				Layout layout = LayoutLocalServiceUtil.getLayout(plid);
				long groupId  = getGroupId(sGroupId);
				
				// Se actualizan las sectionproperties
				updateSectionProperties(groupId, layout.getPlid(), item);
				
				if (validateLog(sGroupId, globalId))
					xmlIOContext.itemLog.addMessage(item, globalId, getClassName(), IterKeys.UPDATE, "Done", IterKeys.DONE, sGroupId);
			}
		}
		catch (Throwable th)
		{
			_log.error(th);
			
			// Si ya ha fallado previamante NO se añade este fallo.
			if (validateLog(sGroupId, globalId))
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, th.toString(), IterKeys.ERROR, sGroupId);
			
			// Si falla la publicación de un nuevo Layout se borra
			if (isNewLayout && plid > 0)
			{
				try
				{
					LayoutLocalServiceUtil.deleteLayout(plid);
				}
				catch (Exception e)
				{
					_log.error(e);
				}
			}
		}
		finally
		{
			// Si no Layout no termina correctamente el Portlet tampoco porque se quiere que para la próxima publicación, se envie toda la info de los portlets
			if (!validateLog(sGroupId, globalId))
				xmlIOContext.itemLog.updateLog(IterKeys.CLASSNAME_PORTLET, sGroupId, globalId, IterKeys.ERROR);
		}
	}
	
/*	// Modificación de un layout
	@Override
	protected void modify(Element item, Document doc) 
	{	
		_log.trace("LayoutXmlIO.modify: [Begin]");
		
		String sGroupId = item.attributeValue("groupid");
		String globalId = item.attributeValue("globalid");	
		boolean borrarLayoutSiException = false;
		boolean existeLayoutPadre = false;
		Layout layout 	= null;
		String sParentId = StringPool.BLANK; 
			
		try
		{
			sParentId 		= getAttribute(item, "parentid");
			String newFriendlyUrl 	= getParamTxtByName(item, "url");		
			String layoutType 		= getParamTxtByName(item, "layout-type");	
			String description 		= getParamTxtByName(item, "description", StringPool.BLANK);
			boolean privateLayout 	= GetterUtil.getBoolean(getParamTxtByName(item, "private"), 	false);
			boolean isHidden 		= GetterUtil.getBoolean(getParamTxtByName(item, "hidden"), 	false);
			long groupId			= getGroupId(sGroupId);
			
			long parentId 			= (Validator.isNotNull(sParentId)) ? 
												getLayoutIdFromFriendlyURL(groupId, sParentId) : 
													LayoutConstants.DEFAULT_PARENT_LAYOUT_ID;
			existeLayoutPadre		= true;
			
			// Se recogen los diferentes idiomas para el Name
			Map<Locale,String> 	localeNamesMap 	= new HashMap<Locale, String>();
			List <String[]> 	namelocaleList 	= getParamListByType(item, "name");		
			for (String[] namelocale : namelocaleList)
				localeNamesMap.put(LocaleUtil.fromLanguageId(namelocale[0]), namelocale[1]);

			// Se recogen los diferentes idiomas para el Title
			Map<Locale,String> 	localeTitlesMap = new HashMap<Locale, String>();
			List <String[]> 	titlelocaleList = getParamListByType(item, "title");		
			for (String[] titlelocale : titlelocaleList)
				localeTitlesMap.put(LocaleUtil.fromLanguageId(titlelocale[0]), titlelocale[1]);

			// ServiceContext Init
			ServiceContext serviceContext = new ServiceContext();		
			serviceContext.setAddCommunityPermissions(true);
			serviceContext.setAddGuestPermissions(true);					
	
			Live live 		= LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
			
			try
			{
				layout = LayoutLocalServiceUtil.getLayout(GetterUtil.getLong(live.getLocalId()));
			}
			catch (Exception e)	{}
			
			if (layout != null)
			{
				_log.trace(String.format("LayoutXmlIO.modify: Updating layout(plid=%d) [Begin]", layout.getPlid()));
				// Existe el layout, se crea uno temporal
				String tmpFriendlyURL = StringPool.SLASH+PortalUUIDUtil.generate();
				Layout tmpLayout= LayoutLocalServiceUtil.addLayout(xmlIOContext.getUserId(), groupId, privateLayout, parentId, localeNamesMap, localeTitlesMap, description, layoutType, isHidden, tmpFriendlyURL, serviceContext);
				
				try
				{
					tmpLayout.setTypeSettings( layout.getTypeSettings() );
					tmpLayout = LayoutLocalServiceUtil.updateLayout(tmpLayout, false);
					
					tmpLayout = updateLayoutProperties(tmpLayout, item, globalId, sGroupId, layout.getPlid());
									
					// Se transfiere de un golpe la info del layoout temporal al layout real
					layout = LayoutLocalServiceUtil.updateFriendlyURL(layout.getPlid(), newFriendlyUrl);
					layout = transferLayoutData(tmpLayout, layout);

					// Se borra el layout temporal
					_log.trace(String.format("LayoutXmlIO.modify: Updating layout(plid=%d). Before delete tmp layout(plid=%d)", layout.getPlid(), tmpLayout.getPlid()));
				}
				finally
				{
					LayoutLocalServiceUtil.deleteLayout(tmpLayout.getPlid());
					_log.trace(String.format("LayoutXmlIO.modify: Updating layout(plid=%d) [End]", layout.getPlid()));
				}
			}
			else
			{
				_log.trace("LayoutXmlIO.modify: Creating new layout [Begin]");
				layout = LayoutLocalServiceUtil.addLayout(xmlIOContext.getUserId(), groupId, privateLayout, parentId, localeNamesMap, localeTitlesMap, description, layoutType, isHidden, newFriendlyUrl, serviceContext);
				borrarLayoutSiException = true;
				layout = updateLayoutProperties(layout, item, globalId, sGroupId, layout.getPlid());
				
				// Entrada en Xmlio_Live a un layout que ya no existe. Se borra el anterior
				if (live != null)
					LiveLocalServiceUtil.deleteLiveById(live.getId());
				_log.trace("LayoutXmlIO.modify: Creating new layout [End]");
			}
			
			// Se crea o actualiza la entrada en live
			LiveLocalServiceUtil.add(_className, groupId, 0, 0, globalId, String.valueOf(layout.getPlid()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
			
			if (validateLog(sGroupId, globalId))
				xmlIOContext.itemLog.addMessage(item, globalId, getClassName(), IterKeys.UPDATE, "Done", IterKeys.DONE, sGroupId);
			
			// Actualizamos las sectionproperties
			updateSectionProperties(groupId, layout.getPlid(), item);
			
			// Actualizamos los modelos web (catálogos de menu, cabecera y pie)
			CatalogPublicationLocalServiceUtil.importCatalogPage(layout.getPlid(), groupId, item);
			
			// Esta linea va siempre al final, para que ante cualquier error si el layout es nuevo se elimine.
			borrarLayoutSiException=false;
		}
		catch (Exception e) 
		{
			String errorMsg = "";
			if(e instanceof NoSuchLayoutTemplateException)
				errorMsg = e.toString();
			if( !existeLayoutPadre && e instanceof NoSuchLayoutException)
				errorMsg = "Parent layout '" + sParentId + "' does not exists.";
			else
				errorMsg = "Error creating " + e;
				
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, errorMsg, IterKeys.ERROR, sGroupId);
			if (borrarLayoutSiException)
				MyDeleteLayout(layout);
		}	
		finally
		{
			// Si no Layout no termina correctamente el Portlet tampoco porque se quiere que para la próxima publicación, se envie toda la info de los portlets
			if (!validateLog(sGroupId, globalId))
				xmlIOContext.itemLog.updateLog(IterKeys.CLASSNAME_PORTLET, sGroupId, globalId, IterKeys.ERROR);
		}
		_log.trace("LayoutXmlIO.modify: [End]");
	}
*/	
	
	private void updateLayoutTypeSettings(UnicodeProperties props, Node item, String sGroupId) throws SystemException, PortalException, ServiceError
	{
		long scopeGroupId = 0L;
		String languageId = LocaleUtil.toLanguageId(LocaleUtil.getDefault());
		
		props.setProperty("sitemap-priority", 	getParamTxtByName(item, "sitemap-priority"));
		props.setProperty("sitemap-changefreq", getParamTxtByName(item, "sitemap-changefreq"));
		props.setProperty("sitemap-include", 	getParamTxtByName(item, "sitemap-include"));
		
		String paramTxt = getParamTxtByName(item, String.format(IterKeys.META_ROBOTS, languageId));
		if(Validator.isNotNull(paramTxt))
			props.setProperty(String.format(IterKeys.META_ROBOTS, languageId), paramTxt);
		
		paramTxt = getParamTxtByName(item, String.format(IterKeys.META_KEYWORDS, languageId), StringPool.BLANK);
		props.setProperty(String.format(IterKeys.META_KEYWORDS, languageId), paramTxt);
		
		paramTxt = getParamTxtByName(item, String.format(IterKeys.META_DESCRIPTION, languageId));
		if(Validator.isNotNull(paramTxt))
			props.setProperty(String.format(IterKeys.META_DESCRIPTION, languageId), paramTxt);
		
		paramTxt = getParamTxtByName(item, IterKeys.JAVASCRIPT_1);
		if(Validator.isNotNull(paramTxt))
			props.setProperty(IterKeys.JAVASCRIPT_1, paramTxt);
		
		paramTxt = getParamTxtByName(item, IterKeys.JAVASCRIPT_2);
		if(Validator.isNotNull(paramTxt))
			props.setProperty(IterKeys.JAVASCRIPT_2, paramTxt);
		
		paramTxt = getParamTxtByName(item, IterKeys.JAVASCRIPT_3);
		if(Validator.isNotNull(paramTxt))
			props.setProperty(IterKeys.JAVASCRIPT_3, paramTxt);
		
		paramTxt = getParamTxtByName(item, IterKeys.TARGET);
		if(Validator.isNotNull(paramTxt))
			props.setProperty(IterKeys.TARGET, paramTxt);
		
		paramTxt = getParamTxtByName(item, IterKeys.LINK_TO_URL);
		if(Validator.isNotNull(paramTxt))
			props.setProperty(IterKeys.URL, paramTxt);
		
		paramTxt = getParamTxtByName(item, IterKeys.LINK_TO_LAYOUT_ID);
		if(Validator.isNotNull(paramTxt))
		{
			scopeGroupId = getGroupId(sGroupId);
			Live liveLayoutProp = LiveLocalServiceUtil.getLiveByGlobalId(scopeGroupId, IterKeys.CLASSNAME_LAYOUT, paramTxt);
			ErrorRaiser.throwIfNull(liveLayoutProp, IterErrorKeys.XYZ_E_UNDEFINED_LAYOUT_ZYX, paramTxt);
			
			Layout layoutProp 	= LayoutLocalServiceUtil.getLayout(GetterUtil.getLong(liveLayoutProp.getLocalId()));
						
			props.setProperty(IterKeys.LINK_TO_LAYOUT_ID, String.valueOf(layoutProp.getLayoutId()));
		}
		
		paramTxt = getParamTxtByName(item, IterKeys.GROUPID);
		if(Validator.isNotNull(paramTxt))
		{
			if(scopeGroupId==0)
				scopeGroupId = getGroupId(sGroupId);
			
			Live liveProp = LiveLocalServiceUtil.getLiveByGlobalId(scopeGroupId, IterKeys.CLASSNAME_GROUP, paramTxt);
			
			if(liveProp != null)
			{
				props.setProperty(IterKeys.GROUPID, liveProp.getLocalId() );											
			}
			else
			{
				long referenceGroupId = getGroupId(paramTxt);
				props.setProperty(IterKeys.GROUPID, String.valueOf(referenceGroupId));	
			}
		}
		
		paramTxt = getParamTxtByName(item, IterKeys.PRIVATE_LAYOUT);
		if(Validator.isNotNull(paramTxt))
			props.setProperty(IterKeys.PRIVATE_LAYOUT, paramTxt);
	}

	private Layout updateLayoutProperties(Layout layout, Element item, String globalId, String sGroupId, long plid) throws PortalException, SystemException, UnsupportedEncodingException, SecurityException, com.liferay.portal.kernel.error.ServiceError, NoSuchMethodException
	{
		_log.trace(String.format("LayoutXmlIO.updateLayoutProperties(plid=%d): [Begin]", layout.getPlid()));
		
		String templateId 		= getParamTxtByName(item, "templateid");	
		
		// Si no es de tipo portlet, actualizamos sus propiedades
		if (!getParamTxtByName(item, "layout-type").equals(LayoutConstants.TYPE_PORTLET))
		{
			setLayoutTypeProperties(item, sGroupId, layout, globalId);
		}

		// Update icon image
		layout.setIconImage( GetterUtil.getBoolean(getParamTxtByName(item, "icon-image"), false) );		
		long imageId = XMLIOUtil.importImageFile ( getParamTxtByName(item, "icon-image-file") );
		if (imageId != -1)
		{
			layout.setIconImageId(imageId);
		}
		
		// Update priority
		layout.setPriority( Integer.parseInt(getParamTxtByName(item, "priority")) );
		
		//Update the Layout of the page
		LayoutTypePortlet layoutTypePortlet = (LayoutTypePortlet)layout.getLayoutType();				
		if (!templateId.equals(""))
		{
			layoutTypePortlet.setLayoutTemplateId(0, templateId, false);
		}

		// Se actualizan las propiedades del sitemap
		updateLayoutTypeSettings(layout.getTypeSettingsProperties(), item, sGroupId);
		
		setLookAndFeel( GetterUtil.getBoolean(getParamTxtByName(item, "private"), false), layout, 
						getParamTxtByName(item, "theme"), 
						getParamTxtByName(item, "color-scheme"), "", false);
		
		// 0010858: Edición directa en LIVE: getEditArticle retorna el doble de asignacionea a sección.
		// ITER-74: Tras publicar, los portlets no pintan artículos de la sección actual. Es MUY IMPORTANTE pasar 
		// el plid del Layout definitivo y no del temporal porque se lo contrario se actualizará el XmlIO_Live.localId con el plid del temporal, y 
		// las configuraciones de los portlets que utilicen esta sección, apuntarán al uuid_ del layout temporal, que tras la publicación se borra
		String localId = String.valueOf(plid);
		LiveLocalServiceUtil.add(_className, layout.getGroupId(), 0, 0, globalId, localId, IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
		
		// Se borran TODOS los portlets
		layout = deletePortlets(layout, item, globalId, sGroupId, plid, IterKeys.UPDATE);
		
		// Se añaden los portlets
		layout = importPortlets(layout, item, globalId, sGroupId, plid);
		
		_log.trace(String.format("LayoutXmlIO.updateLayoutProperties(plid=%d): [End]", layout.getPlid()));
		
		return layout;
	}
	
	/**
	 * Método que elimina del Layout <b>TODOS</b> los portlets que tiene asociado
	 * 
	 * @param layout
	 * @param item
	 * @param globalId
	 * @param sGroupId
	 * @param plid
	 * @return El layout con las actualizaciones oportunas
	 */
	private Layout deletePortlets(Layout layout, Element item, String globalId, String sGroupId, long plid, String operation)
	{
		_log.trace(String.format("LayoutXmlIO.deletePortlets(plid=%d): [Begin]", layout.getPlid()));
		if (isValid())
		{
			PortletXmlIO portletXmlIO = new PortletXmlIO();
			portletXmlIO.setXMLIOContext(xmlIOContext);
	
			layout = portletXmlIO.delete(layout, item, globalId, sGroupId, plid);
			
			if (!validateLog(IterKeys.CLASSNAME_PORTLET, sGroupId, globalId))
				xmlIOContext.itemLog.addMessage(item, globalId, getClassName(), operation, "Can't delete portlets", IterKeys.ERROR, sGroupId);		
		}
		_log.trace(String.format("LayoutXmlIO.deletePortlets(plid=%d): [End]", layout.getPlid()));
		return layout;
	}
	
	/**
	 * 
	 * @param layout
	 * @param item
	 * @param globalId
	 * @param sGroupId
	 * @param plid
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws NoSuchMethodException 
	 * @throws com.liferay.portal.kernel.error.ServiceError 
	 * @throws SecurityException 
	 * @throws UnsupportedEncodingException 
	 */
	private Layout importPortlets(Layout layout, Element item, String globalId, String sGroupId, long plid) throws PortalException, SystemException, UnsupportedEncodingException, SecurityException, com.liferay.portal.kernel.error.ServiceError, NoSuchMethodException
	{
		_log.trace(String.format("LayoutXmlIO.importPortlets(plid=%d): [Begin]", layout.getPlid()));
		if (isValid())
		{
			String xpath = String.format("item[@classname='%s' and @globalid='%s' and @groupid=%s and  (@operation='%s' or @operation='%s')]", 
										IterKeys.CLASSNAME_PORTLET, globalId, StringUtil.escapeXpathQuotes(sGroupId), IterKeys.CREATE, IterKeys.UPDATE);
			
			PortletXmlIO portletXmlIO = new PortletXmlIO();
			portletXmlIO.setXMLIOContext(xmlIOContext);
	
			List<Node> portletNodes = item.getParent().selectNodes(xpath);
			for (Node portletNode : portletNodes)
			{
				layout = portletXmlIO.modify(layout, (Element) portletNode, globalId, sGroupId, plid);
			}
			
			layout = ((LayoutTypePortlet) layout.getLayoutType()).checkLayoutColumns();	
			
			if (!validateLog(IterKeys.CLASSNAME_PORTLET, sGroupId, globalId))
				xmlIOContext.itemLog.addMessage(item, globalId, getClassName(), IterKeys.UPDATE, "Can't create dependency", IterKeys.ERROR, sGroupId);		
		}
		_log.trace(String.format("LayoutXmlIO.importPortlets(plid=%d): [End]", layout.getPlid()));
		return layout;
	}
	
	private boolean isValid()
	{
		return _isValid;
	}
	private boolean validateLog(String groupName, String globalId)
	{
		return validateLog(getClassName(), groupName, globalId);
	}
	private boolean validateLog(String className, String groupName, String globalId)
	{
		_isValid = isValid() && xmlIOContext.itemLog.validateLog(className, groupName, globalId);
		return isValid();
	}
	
	/**
	 * 
	 * @param layout
	 * @param portletId
	 * @throws Exception
	 */	
	/*
	private void updateResources(Layout layout, String portletId) throws Exception {

		String rootPortletId = PortletConstants.getRootPortletId(portletId);

		String portletPrimaryKey = PortletPermissionUtil.getPrimaryKey(layout.getPlid(), portletId);

		ResourceLocalServiceUtil.updateResources(layout.getCompanyId(), layout.getName(), 0, rootPortletId,	portletPrimaryKey);
		
	}
	*/	
	
	/**
	 * 
	 * @param layout
	 * @param portletId
	 * @throws Exception
	 */
	
	private void addResources(Layout layout, String portletId) throws Exception 
	{
		String rootPortletId = PortletConstants.getRootPortletId(portletId);

		String portletPrimaryKey = PortletPermissionUtil.getPrimaryKey(layout.getPlid(), portletId);

		ResourceLocalServiceUtil.addResources(layout.getCompanyId(), layout.getGroupId(), 0, rootPortletId,	portletPrimaryKey, true, true, true);
	}	
	
	
	private void setLayoutTypeProperties (Element item, String sGroupId, Layout layout, String globalId) throws PortalException, SystemException, ServiceError
	{
		UnicodeProperties properties = layout.getTypeSettingsProperties();
				
		List<String[]> propList = getParamListByType(item, "property");		
		
		long groupId = getGroupId(sGroupId);
		
		// http://jira.protecmedia.com:8080/browse/ITER-725?focusedCommentId=28446&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-28446
		// La propiedad target no siempre viene, por eso mejor antes se elimina del destino, y si el origen trae una específica se sobrescribe
		// De lo contrario si en origen se elimina el "target", en destino nunca se eliminaría
		properties.remove("target");
		
		for (String[] prop : propList) 
		{			
			String key = prop[0];			
			if(key.equals("linkToLayoutId"))
			{					
				Live liveLayoutProp = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_LAYOUT, prop[1]);
				ErrorRaiser.throwIfNull(liveLayoutProp, IterErrorKeys.XYZ_E_UNDEFINED_LAYOUT_ZYX, prop[1]);
				
				Layout layoutProp 	= LayoutLocalServiceUtil.getLayout(GetterUtil.getLong(liveLayoutProp.getLocalId()));
				ErrorRaiser.throwIfNull(layoutProp, IterErrorKeys.XYZ_E_UNDEFINED_LAYOUT_ZYX, liveLayoutProp.getLocalId());

				properties.setProperty(key, String.valueOf(layoutProp.getLayoutId()));		
			}
			else if(key.equals("groupId"))
			{				
				Live liveProp = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_GROUP, prop[1]);
				
				if(liveProp != null)
				{
					properties.setProperty(key, liveProp.getLocalId());											
				}
				else
				{
					long globalGroupId = CompanyLocalServiceUtil.getCompany(GroupLocalServiceUtil.getGroup(groupId).getCompanyId()).getGroup().getGroupId();
					liveProp = LiveLocalServiceUtil.getLiveByGlobalId(globalGroupId, IterKeys.CLASSNAME_GROUP, prop[1]);
					
					properties.setProperty(key, liveProp.getLocalId());	
				}
			}
			else
			{					
				properties.setProperty(key, prop[1]);										
			}
		}
		
		layout.setTypeSettingsProperties(properties);
	}
	
	
	/*
	 * Funciones especiales para el preprocesamiento de las páginas.
	 * Se hace una primera vuelta de creación de las páginas sin configurarlas para evitar errores por dependencias
	 */
	
	public void preImportLayouts(Element item, Document doc)
	{
		String operation = item.attributeValue("operation");
		if (operation.equals(IterKeys.CREATE))
		{
			createWithoutConfig(item, doc);
		}
	}
	
	protected void createWithoutConfig(Element item, Document doc) 
	{		
		String sParentId 		= getAttribute(item, "parentid");
		String sGroupId 		= getAttribute(item, "groupid");
		String globalId 		= getAttribute(item, "globalid");	
				
		String description 		= GetterUtil.getString(getParamTextByName(item, "description"), StringPool.BLANK);
		String newUrl 			= getParamTextByName(item, "url");		
		//long layoutId = GetterUtil.getLong(getParamTextByName(item, "layoutid"), 1);
		String layoutType 		= getParamTextByName(item, "layout-type");	
		String templateId 		= getParamTextByName(item, "templateid");	
		String themeS 			= getParamTextByName(item, "theme");
		String colorScheme 		= getParamTextByName(item, "color-scheme");	
		String iconImageFile 	= getParamTextByName(item, "icon-image-file");
		boolean iconImage 		= GetterUtil.getBoolean(getParamTextByName(item, "icon-image"),false);	
		boolean privateLayout 	= GetterUtil.getBoolean(getParamTextByName(item, "private"), false);
		boolean isHidden 		= GetterUtil.getBoolean(getParamTextByName(item, "hidden"), false);
		String priority 		= getParamTextByName(item, "priority");		
		
		String trace = "[%s] - LayoutXmlIO( groupid:'%s' | globalid:'%s' ) - createWithoutConfig";
		_log.trace( String.format(trace, "Enter", sGroupId, globalId) );

		try
		{
            // Justo después se borra la caché
			LayoutUtil.clearCache();

			long groupId		= getGroupId(sGroupId);
			long parentId 		= getLayoutIdFromFriendlyURL(groupId, sParentId);			
			
			//Recogemos los diferentes idiomas para el Name y el Title
			Map<Locale,String> localeNamesMap = new HashMap<Locale, String>();
			
			List <String[]> namelocaleList = getParamListByType(item, "name");		
			for (String[] namelocale : namelocaleList)
			{
				localeNamesMap.put(LocaleUtil.fromLanguageId(namelocale[0]), namelocale[1]);
			}		
			
			Map<Locale,String> localeTitlesMap = new HashMap<Locale, String>();
			
			List <String[]> titlelocaleList = getParamListByType(item, "title");		
			for (String[] titlelocale : titlelocaleList)
			{
				localeTitlesMap.put(LocaleUtil.fromLanguageId(titlelocale[0]), titlelocale[1]);
			}		
			
			//ServiceContext Init
			ServiceContext serviceContext = new ServiceContext();		
			serviceContext.setAddCommunityPermissions(true);
			serviceContext.setAddGuestPermissions(true);					
			
			//Get live to get the element localId
			Live live 			= LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
			String layoutTrace 	= "  [%s] Layout( plId:'%s' | URL:'%s' ) typeSettings:\n%s";
			
			try 
			{	
				Layout layout 	= LayoutLocalServiceUtil.getLayout(GetterUtil.getLong(live.getLocalId()));
				
				//Exists, so we try an update.
				try
				{
					_log.trace( String.format(layoutTrace, "Before updateLayout", layout.getPlid(), layout.getFriendlyURL(), layout.getTypeSettings()) );
					
					//Update Ppal
					layout = LayoutLocalServiceUtil.updateLayout(groupId, privateLayout, layout.getLayoutId(), parentId, localeNamesMap, localeTitlesMap, description, layoutType, isHidden, newUrl, serviceContext);

					_log.trace( String.format(layoutTrace, "After updateLayout", layout.getPlid(), layout.getFriendlyURL(), layout.getTypeSettings()) );
					
					//Si no es de tipo portlet, actualizamos sus propiedades
					if (!layoutType.equals(LayoutConstants.TYPE_PORTLET))
					{
						setLayoutTypeProperties(item, sGroupId, layout, globalId);
					}
					
					//Update icon image
					layout.setIconImage(iconImage);		
					long imageId = XMLIOUtil.importImageFile (iconImageFile);
					if (imageId != -1)
					{
						layout.setIconImageId(imageId);
					}
					
					//Update priority
					layout.setPriority(Integer.parseInt(priority));
					
					//Update the Layout of the page
					LayoutTypePortlet layoutTypePortlet = (LayoutTypePortlet)layout.getLayoutType();				
					if (! templateId.equals(""))
					{
						try
						{
							layoutTypePortlet.setLayoutTemplateId(0, templateId, false);
						}
						catch(Exception e)
						{
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't apply layoutTemplate", IterKeys.ERROR, sGroupId);
							//update entry in live table
							LiveLocalServiceUtil.add(_className, groupId, 0, 0, globalId, live.getLocalId(), IterKeys.CREATE, IterKeys.ERROR, new Date(), IterKeys.ENVIRONMENT_LIVE);
						}
					}
					
					_log.trace( String.format(layoutTrace, "After setLayoutTemplateId", layout.getPlid(), layout.getFriendlyURL(), layout.getTypeSettings()) );
					
					setLookAndFeel(privateLayout, layout, themeS, colorScheme, "", false);
					
					// Se fuerza una actualización del TypeSettings(superpufo: 0006727)
					layout.setTypeSettings( layout.getTypeSettings() );
					//To save the changes
					layout = LayoutLocalServiceUtil.updateLayout(layout, false);
					
					_log.trace( String.format(layoutTrace, "After updateLayout", layout.getPlid(), layout.getFriendlyURL(), layout.getTypeSettings()) );
					
					//update entry in live table
					LiveLocalServiceUtil.add(_className,	groupId, 0, 0, globalId, live.getLocalId(), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
				}
				catch(Exception e4)
				{					
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error updating " + e4, IterKeys.ERROR, sGroupId);
					
					// update entry in live table
					LiveLocalServiceUtil.add(_className, groupId, 0, 0, globalId, live.getLocalId(), IterKeys.CREATE, IterKeys.ERROR, new Date(), IterKeys.ENVIRONMENT_LIVE);
				}
			} 
			catch (Exception e) 
			{ 
				//Element doesn't exist. Let's create it.
				try
				{
					if (live == null || !live.getOperation().equals(IterKeys.DELETE))
					{
						//Existe la entrada en Live pero no existe el elemento. Borramos en Live para volver realizar una inserción completa.
						if (live != null)
						{
							LiveLocalServiceUtil.deleteLiveById(live.getId());
						}							
						
						Layout layout = LayoutLocalServiceUtil.addLayout(xmlIOContext.getUserId(), groupId, privateLayout, parentId, localeNamesMap, localeTitlesMap, description, layoutType, isHidden, newUrl, serviceContext);
						_log.trace( String.format(layoutTrace, "After addLayout", layout.getPlid(), layout.getFriendlyURL(), layout.getTypeSettings()) );
						
						
						// Si no es de tipo portlet, actualizamos sus propiedades
						if (!layoutType.equals(LayoutConstants.TYPE_PORTLET))
						{
							setLayoutTypeProperties(item, sGroupId, layout, globalId);
						}
						
						// Update icon image
						layout.setIconImage(iconImage);	
						long imageId = XMLIOUtil.importImageFile (iconImageFile);
						if (imageId != -1)
						{
							layout.setIconImageId(imageId);
						}
						
						layout.setPriority(Integer.parseInt(priority));
						
						//Update the Layout of the page
						LayoutTypePortlet layoutTypePortlet = (LayoutTypePortlet)layout.getLayoutType();
						
						if ( !templateId.equals("") )
						{			
							try
							{
								layoutTypePortlet.setLayoutTemplateId(0, templateId, false);	
							}
							catch(Exception e1)
							{
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't apply layoutTemplate", IterKeys.ERROR, sGroupId);

								//update entry in live table
								LiveLocalServiceUtil.add(_className, groupId, 0, 0, globalId, live.getLocalId(), IterKeys.CREATE, IterKeys.ERROR, new Date(), IterKeys.ENVIRONMENT_LIVE);
							}
						}	
						
						addResources(layout, PortletKeys.DOCKBAR);	
						
						_log.trace( String.format(layoutTrace, "After setLayoutTemplateId", layout.getPlid(), layout.getFriendlyURL(), layout.getTypeSettings()) );
						
						//Now apply the theme
						setLookAndFeel(privateLayout, layout, themeS, colorScheme, "", false);
						
						// Se fuerza una actualización del TypeSettings(superpufo: 0006727)
						layout.setTypeSettings( layout.getTypeSettings() );
						//To save the changes
						layout = LayoutLocalServiceUtil.updateLayout(layout, false);
						
						_log.trace( String.format(layoutTrace, "After updateLayout", layout.getPlid(), layout.getFriendlyURL(), layout.getTypeSettings()) );

						//Create a new entry in live table
						LiveLocalServiceUtil.add(_className,	groupId, 0, 0, globalId,
								String.valueOf(layout.getPlid()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);				
							
						try
						{
							//Update globalId.
							LiveLocalServiceUtil.updateGlobalId(groupId, _className, String.valueOf(layout.getPlid()), globalId);
						}
						catch (Exception e3)
						{
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicated globalId", IterKeys.ERROR, sGroupId);
						
							//update entry in live table
							LiveLocalServiceUtil.add(_className, groupId, 0, 0, globalId, live.getLocalId(), IterKeys.CREATE, IterKeys.ERROR, new Date(), IterKeys.ENVIRONMENT_LIVE);
						}
						
					}
					else
					{
						//Nothing
					}
				}
				catch(Exception e2)
				{
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error creating " + e2, IterKeys.ERROR, sGroupId);
					//update entry in live table
					LiveLocalServiceUtil.add(_className, groupId, 0, 0, globalId, live.getLocalId(), IterKeys.CREATE, IterKeys.ERROR, new Date(), IterKeys.ENVIRONMENT_LIVE);
				
				}	
			}		
		} 
		catch (Exception e) 
		{
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error creating " + e, IterKeys.ERROR, sGroupId);
		}	
		_log.trace( String.format(trace, "Exit", sGroupId, globalId) );
	}
	
	
	private void setLookAndFeel( boolean privateLayout, Layout layout, String themeId,
								 String colorSchemeId, String css, boolean wapTheme) throws PortalException, SystemException 
	{
		if (wapTheme) 
		{
			layout.setWapThemeId(themeId);
			layout.setWapColorSchemeId(colorSchemeId);
		}
		else 
		{
			layout.setThemeId(themeId);
			layout.setColorSchemeId(colorSchemeId);
			layout.setCss(css);
		}
	}

	public Layout transferLayoutData(Layout srclayout, Layout dstlayout) throws PortalException, SystemException, IOException, SQLException, DocumentException
	{
		long srcPlid = srclayout.getPlid();
		long dstPlid = dstlayout.getPlid();

		_log.trace(String.format("LayoutXmlIO.transferLayoutData(plid=%d to plid=%d): [Begin]", srcPlid, dstPlid));
		
		if (isValid())
		{
			
			Document dom = SAXReaderUtil.createDocument();
			Element root = dom.addElement("rs");
			root.addElement("srcLayoutPlid").		addText( Long.toString(srcPlid) );
			root.addElement("privateLayout").		addText( srclayout.getPrivateLayout() ? "1" : "0" );
			root.addElement("parentLayoutId").		addText( Long.toString(srclayout.getParentLayoutId()) );
			root.addElement("name").				addCDATA(srclayout.getName() );
			root.addElement("title").				addCDATA( srclayout.getTitle() );
			root.addElement("description").			addText( srclayout.getDescription() );
			root.addElement("type").				addText( srclayout.getType() );
			root.addElement("typeSettings").		addText( srclayout.getTypeSettings() );
			root.addElement("hidden").				addText( srclayout.getHidden() ? "1" : "0" );
	//		root.addElement("friendlyURL").			addText( srclayout.getFriendlyURL() );
			root.addElement("iconImage").			addText( srclayout.getIconImage() ? "1" : "0"  );
			root.addElement("iconImageId").			addText( Long.toString(srclayout.getIconImageId()) );
			root.addElement("themeId").				addText( srclayout.getThemeId() );
			root.addElement("colorSchemeId").		addText( srclayout.getColorSchemeId() );
			root.addElement("css").					addText( srclayout.getCss() );
			root.addElement("priority").			addText( Long.toString(srclayout.getPriority()) );
			root.addElement("layoutPrototypeId").	addText( Long.toString(srclayout.getLayoutPrototypeId()) );
			root.addElement("dlFolderId").			addText( Long.toString(srclayout.getLayoutPrototypeId()) );
			
			String staticPortlets = GetterUtil.getString(PropsUtil.get(PropsKeys.LAYOUT_STATIC_PORTLETS_ALL), "");
			
			String sql = String.format("CALL ITR_UPDATE_LAYOUT(%d,%d,\"%s\",\"%s\");", dstPlid, srclayout.getGroupId(), root.asXML().replaceAll("\"", "'").replaceAll(":", "@*2-Points*@"),staticPortlets);
			PortalLocalServiceUtil.executeUpdateQuery(sql);
			
			// Se borran ambos layouts y sus correspondientes preferencias
			LayoutUtil.clearCache(srclayout);
			LayoutUtil.clearCache(dstlayout);
			
			List<PortletPreferences> srcPreferences = PortletPreferencesUtil.findByPlid(srcPlid);
			for (PortletPreferences srcPref:srcPreferences)
				PortletPreferencesUtil.clearCache(srcPref);
			
			List<PortletPreferences> dstPreferences = PortletPreferencesUtil.findByPlid(dstPlid);
			for (PortletPreferences dstPref:dstPreferences)
				PortletPreferencesUtil.clearCache(dstPref);
		}
		_log.trace(String.format("LayoutXmlIO.transferLayoutData(plid=%d to plid=%d): [End]", srcPlid, dstPlid));
		return LayoutLocalServiceUtil.getLayout(dstPlid);
	}

	// Actualiza o crea un registro de section properties. Los campos noheader, nomenu y nofooter se autocalculan en LayoutLocalServiceUtil.updateSectionProperties
	private void updateSectionProperties(long groupId, long plid, Element item) throws NumberFormatException, Exception
	{
		_log.trace("In LayoutXMlIO.updateSectionProperties");
		
		// El layout que llega (item) no tiene sectionproperties que actualizar (no se rellenaron sus campos)
		if ( Validator.isNotNull(getParamTxtByName(item, "section_properties_plid")) )
		{
			// Los campos comentados se autocalculan en LayoutLocalServiceUtil.updateSectionProperties
			String headerelementid 	= getParamTxtByName(item, "section_properties_headerelementid");
			String noheader 		= getParamTxtByName(item, "section_properties_noheader"		  );
			
			if ("null".equals(headerelementid) && "true".equals(noheader))
			{			// aspa roja y html
				headerelementid = "0";
			}
			else if ("null".equals(headerelementid) && "false".equals(noheader))
			{
				headerelementid = "-1";
			}
			
			String menuElementId 	= getParamTxtByName(item, "section_properties_menuelementid"  );      
			String noMenu 		    = getParamTxtByName(item, "section_properties_nomenu"	      );
			if ("null".equals(menuElementId) && "true".equals(noMenu))
			{
				menuElementId = "0";
			}
			else if ("null".equals(menuElementId) && "false".equals(noMenu))
			{
				menuElementId = "-1";
			}		
			
			String footerElementId 	= getParamTxtByName(item, "section_properties_footerelementid");
			String noFooter 		= getParamTxtByName(item, "section_properties_nofooter"		  );
			if ("null".equals(footerElementId) && "true".equals(noFooter)){
				footerElementId = "0";
			}else if ("null".equals(footerElementId) && "false".equals(noFooter)){
				footerElementId = "-1";
			}			
			
			// El id del artículo (articleid) es el mismo en el back y en el live, no hace falta ir por el globalid
			String aboutId 			= getParamTxtByName(item, "section_properties_aboutid"	 );
			String autoRss        	= getParamTxtByName(item, "section_properties_autorss"	 );		
			String autorssxsl       = getParamTxtByName(item, "section_properties_autorssxsl");
			
			LayoutLocalServiceUtil.updateSectionProperties(groupId, plid, menuElementId, headerelementid, 
				                                           footerElementId, aboutId, autoRss, autorssxsl, false);	
		}	
	}
}
