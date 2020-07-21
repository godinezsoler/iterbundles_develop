/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.protecmedia.iter.news.service.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.portlet.ReadOnlyException;
import javax.portlet.ValidatorException;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.CatalogQueries;
import com.liferay.portal.kernel.util.CatalogUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WidgetUtil;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.model.LayoutTypePortlet;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.news.service.base.CatalogLocalServiceBaseImpl;

/**
 * The implementation of the catalog local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.news.service.CatalogLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.news.service.CatalogLocalServiceUtil} to access the catalog local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author protec
 * @see com.protecmedia.iter.news.service.base.CatalogLocalServiceBaseImpl
 * @see com.protecmedia.iter.news.service.CatalogLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class})
public class CatalogLocalServiceImpl extends CatalogLocalServiceBaseImpl 
{
	private static Log _log = LogFactoryUtil.getLog(CatalogLocalServiceImpl.class);
	
	/**
	 * 
	 * @param groupId
	 * @return
	 * @throws Exception
	 */
	public Document getCatalogs(long groupId, String type) throws Exception
	{
		String query;
		if (type.equalsIgnoreCase(StringPool.BLANK))
			query = String.format(CatalogQueries.GET_CATALOGS_BY_GROUP, groupId);
		else
		{
			ErrorRaiser.throwIfFalse(CatalogUtil.CATALOG_TYPES.contains(type), IterErrorKeys.XYZ_E_CATALOG_INVALID_TYPE_ZYX);
			query = String.format(CatalogQueries.GET_CATALOGS_BY_GROUP_TYPE, groupId, type);
		}
			
		Document dom = PortalLocalServiceUtil.executeQueryAsDom( query, true, XMLHelper.rsTagName, CatalogUtil.rowCatName, new String[]{"description"} );
		
		return dom;
	}

	/**
	 * 
	 * @param catalogid
	 * @return
	 * @throws Exception
	 */
	public Document getCatalog(String catalogid) throws Exception
	{
		String query = String.format(CatalogQueries.GET_CATALOGS_BY_ID, catalogid);
		return PortalLocalServiceUtil.executeQueryAsDom( query, true, XMLHelper.rsTagName, CatalogUtil.rowCatName, new String[]{"description"} );
	}
	
	/**
	 * 
	 * @param groupId
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public Document addCatalog(long groupId, String data) throws Exception
	{
		Document dom 		= SAXReaderUtil.read(data);
		String type 		= XMLHelper.getTextValueOf(dom, "/cat/@catalogtype");
		String catalogname 	= XMLHelper.getTextValueOf(dom, "/cat/@catalogname");
		String description 	= XMLHelper.getTextValueOf(dom, "/cat/description/text()");
		
		return addCatalog(groupId, catalogname, type, description);
	}
	
	public Document addCatalog(long groupId, String catalogname, String type, String description) throws Exception
	{
		if (description == null)
			description = StringPool.NULL;
		
		description = StringUtil.apostrophe( description ); 
		
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Se comprueba que es un tipo permitido y que no estaría repetido
		CatalogUtil.checkTypeInDB(groupId, type);

		ErrorRaiser.throwIfFalse(Validator.isNotNull(catalogname), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String catalogid   = PortalUUIDUtil.newUUID();
		
		String sql = String.format(CatalogQueries.INSERT_CATALOG, groupId, catalogname, description, type, catalogid);
		PortalLocalServiceUtil.executeUpdateQuery(sql);
		
		return getCatalog(catalogid);
	}
	
	/**
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public Document updateCatalog(String data) throws Exception
	{
		Document dom = SAXReaderUtil.read(data);
		
		String catalogid= XMLHelper.getTextValueOf(dom, "/cat/@catalogid");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(catalogid), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String type  = XMLHelper.getTextValueOf(dom, "/cat/@catalogtype");
		
		// Se comprueba que es un tipo permitido y que no estaría repetido
		CatalogUtil.checkTypeInDB(catalogid, type);
		
		String catalogname = XMLHelper.getTextValueOf(dom, "/cat/@catalogname");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(catalogname), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String description = StringUtil.apostrophe( XMLHelper.getTextValueOf(dom, "/cat/description/text()", StringPool.NULL) );

		String sql = String.format(CatalogQueries.UPDATE_CATALOG, catalogname, description, type, catalogid);
		PortalLocalServiceUtil.executeUpdateQuery(sql);
		
		return getCatalog(catalogid);
	}

	/**
	 * 
	 * @param catalogids
	 * @throws Exception
	 */
	public String deleteCatalogs(String catalogids) throws Exception
	{
		ErrorRaiser.throwIfFalse(Validator.isNotNull(catalogids), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document dom = SAXReaderUtil.read(catalogids);
		List<Node> nodeIDs = dom.selectNodes("//cat/@catalogid");
		
		String[] idsList = XMLHelper.getStringValues(nodeIDs);
		ErrorRaiser.throwIfFalse(idsList.length > 0);
		String idsCommaSeparated = StringUtil.merge(idsList, "','");
		
		// Se comprueba que ninguno de los CatalogElements del catálogo esté en uso en alguna configuración
		CatalogUtil.checkCatalogUse(idsList);
		
		//Se comprueba que ningun Catalogo está publicado
		CatalogUtil.checkCatalogPageExistsInLive(idsCommaSeparated, false);
		
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList( String.format(CatalogQueries.GET_CATALOG_PLIDS, idsCommaSeparated) );
		for(Object plid : result)
		{
			LayoutLocalServiceUtil.deleteLayout( Long.valueOf( String.valueOf(plid) ) );
		}
		
		PortalLocalServiceUtil.executeUpdateQuery( String.format(CatalogQueries.DELETE_CATALOGS, idsCommaSeparated) );
		
		return catalogids;
	}

	/**
	 * 
	 * @param catalogid
	 * @param includeElements
	 * @return
	 * @throws Exception
	 */
	public Document getCatalogPages(String catalogid, boolean includeElements) throws Exception
	{
		String sql = (!includeElements) ? 
				String.format(CatalogQueries.GET_CATALOGPAGE_BY_CATALOG,				catalogid) :
				String.format(CatalogQueries.GET_CATALOGPAGE_BY_CATALOG_WITH_ELEMENTS, 	catalogid);
				
		return CatalogUtil.getCatalogPages(sql, includeElements);
	}
	
	/**
	 * 
	 * @param groupId
	 * @param catalogType
	 * @param includeElements
	 * @return
	 * @throws Exception
	 */
	public Document getCatalogPages(long groupId, String catalogType, boolean includeElements) throws Exception
	{
		// SOLo se comprueba en "body" porque NUNCA han tenido más de un elemento por página
		if (catalogType.equals(WidgetUtil.CATALOG_TYPE_BODY) && includeElements)
			CatalogUtil.deleteUnusedElements(groupId, catalogType);

		String sql = (!includeElements) ? 
				String.format(CatalogQueries.GET_CATALOGPAGE_BY_TYPE, 				groupId, catalogType) :
				String.format(CatalogQueries.GET_CATALOGPAGE_BY_TYPE_WITH_ELEMENTS, groupId, catalogType);
				
		Document dom = CatalogUtil.getCatalogPages(sql, includeElements); 				
		
		if (catalogType.equals(WidgetUtil.CATALOG_TYPE_BODY))
		{
			// Se le añade el grupo a cada 
			List<Node> nodes = dom.selectNodes( String.format("//%s", CatalogUtil.rowCatPageName) );
			for (Node node : nodes)
			{
				long numColumns = XMLHelper.getLongValueOf(node, String.format("count(%s)", CatalogUtil.rowCatElemName));
				
				// Se descartan las páginas de catágos que no tengan columnas maquetadas
				if (numColumns == 0)
					node.detach();
				else
					((Element)node).addAttribute("groupid", String.valueOf(groupId));
			}
		}
		return dom;
	}
	
	/**
	 * 
	 */
	public Document getCatalogPage(String catalogpageid, boolean includeElements) throws Exception
	{
		String sql = (!includeElements) ? 
						String.format(CatalogQueries.GET_CATALOGPAGE, catalogpageid) :
						String.format(CatalogQueries.GET_CATALOGPAGE_WITH_ELEMENTS, catalogpageid);
						
		return XMLHelper.collapse( CatalogUtil.getCatalogPages(sql, includeElements) );
	}

	/**
	 * 
	 * @param catalogId
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public Document addCatalogPage(String catalogId, String data) throws Exception
	{
		Document dom = SAXReaderUtil.read(data);
		
		// Parámetros de entrada
		String pagename = XMLHelper.getTextValueOf(dom, "/catpage/@pagename");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(pagename), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String description 			= XMLHelper.getTextValueOf(dom, "/catpage/description/text()", StringPool.BLANK);
		long groupId				= CatalogUtil.getGroupId(catalogId);
		
		long parentLayoutId 		= CatalogUtil.getCatalogPageParentId(groupId);
		String catalogpageid 		= PortalUUIDUtil.newUUID();		
		Layout layoutPageTemplate 	= LayoutLocalServiceUtil.addLayout(GroupMgr.getDefaultUserId(), groupId, false, parentLayoutId, pagename, 
										StringPool.BLANK, description, LayoutConstants.TYPE_PORTLET, true, StringPool.BLANK, new ServiceContext());
		
		PortalLocalServiceUtil.executeUpdateQuery( String.format(CatalogQueries.INSERT_CATALOGPAGE, catalogpageid, catalogId, layoutPageTemplate.getPlid()));
		
		return getCatalogPage(catalogpageid, false);
	}
	
	/**
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public Document addCatalogPageAndElements(String data) throws Exception
	{
		Document dom = SAXReaderUtil.read(data);
		
		long groupId 		= XMLHelper.getLongValueOf(dom, "/catpage/@groupid");
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String catalogType 	= XMLHelper.getTextValueOf(dom, "/catpage/@catalogtype");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(catalogType) && CatalogUtil.CATALOG_TYPES.contains(catalogType), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String description 	= XMLHelper.getTextValueOf(dom, "/catpage/description/text()", StringPool.BLANK);
		String pageName		= XMLHelper.getTextValueOf(dom, "/catpage/@pagename");
		ErrorRaiser.throwIfNull(pageName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Se crea el Layout
		long parentLayoutId 		= CatalogUtil.getCatalogPageParentId(groupId);
		Layout layoutPageTemplate 	= LayoutLocalServiceUtil.addLayout(GroupMgr.getDefaultUserId(), groupId, false, parentLayoutId, pageName, 
				StringPool.BLANK, description, LayoutConstants.TYPE_PORTLET, true, StringPool.BLANK, new ServiceContext());
		
		// A dicho Layout se le asigna el TPL por defecto para los elementos de catálogo de menú, cabecera y pie de página 
		((LayoutTypePortlet)layoutPageTemplate.getLayoutType()).setLayoutTemplateId(0, CatalogUtil.CATALOG_DEFAULT_TPL_ID, false);
		layoutPageTemplate = LayoutLocalServiceUtil.updateLayout(layoutPageTemplate, false);
		
		layoutPageTemplate = cloneCatalogLayout(layoutPageTemplate, catalogType, XMLHelper.getTextValueOf(dom, "/catpage/@srccatalogpageid"));
		
		String catalogPageId = null;
		String catalogId  	 = String.format("ITR_CATALOG_CREATE_IFNOT_EXIST( %d, '%s' )", groupId, catalogType);
		
		List<Object> catalogPageRs = PortalLocalServiceUtil.executeQueryAsList( String.format(CatalogQueries.CREATE_PAGE_AND_ELEMENT, catalogId, layoutPageTemplate.getPlid()) );
		ErrorRaiser.throwIfNull( catalogPageRs != null && catalogPageRs.size() == 1 && Validator.isNotNull(catalogPageId=catalogPageRs.get(0).toString()));
		
		Document result = getCatalogPage(catalogPageId, true);

		result.setXMLEncoding("ISO-8859-1");
		
		return result;
	}
	
	/**
	 * 
	 * @param dstPlid
	 * @param catalogType
	 * @param srcCatalogPageId
	 * @return
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws ValidatorException 
	 * @throws ReadOnlyException 
	 * @throws SystemException 
	 * @throws PortalException 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws com.protecmedia.iter.base.service.util.ServiceError 
	 */
	private Layout cloneCatalogLayout(Layout dstLayout, String catalogType, String srcCatalogPageId) throws PortalException, SystemException, ReadOnlyException, ValidatorException, IOException, SQLException, SecurityException, NoSuchMethodException, com.protecmedia.iter.base.service.util.ServiceError
	{
		if (Validator.isNotNull(srcCatalogPageId))
		{
			long dstPlid = dstLayout.getPlid(); 
			Document dom = PortalLocalServiceUtil.executeQueryAsDom( String.format(CatalogQueries.SEL_SRC_CATALOGPAGE_DATA, srcCatalogPageId) );
			long srcPlid = XMLHelper.getLongValueOf(dom, "/rs/row/@plid");
			
			// Se comprueba que exista la página de catálogo
			ErrorRaiser.throwIfFalse(srcPlid > 0, IterErrorKeys.XYZ_E_CATALOG_SRCCATALOGPAGE_NOT_EXISTS_ZYX);
			
			// Se comprueba que el catálogo de origen y el de destino sean del mismo tipo
			ErrorRaiser.throwIfFalse(catalogType.equals(XMLHelper.getStringValueOf(dom, "/rs/row/@catalogtype","")), IterErrorKeys.XYZ_E_CLONE_MODEL_INVALID_TYPE_ZYX);
			
			dstLayout = com.protecmedia.iter.news.service.LayoutLocalServiceUtil.copyLayout(srcPlid, dstPlid, false, true);
		}
		return dstLayout;
	}
	
	/**
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public Document updateCatalogPage(String data) throws Exception
	{
		Document dom = SAXReaderUtil.read(data);
		
		// Parámetros de entrada
		String catalogpageid = XMLHelper.getTextValueOf(dom, "/catpage/@catalogpageid");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(catalogpageid), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String pagename = XMLHelper.getTextValueOf(dom, "/catpage/@pagename");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(pagename), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String description 	= XMLHelper.getTextValueOf(dom, "/catpage/description/text()", StringPool.BLANK);
		
		// Se actualiza el layout correspondiente
		Document selectDom = PortalLocalServiceUtil.executeQueryAsDom( String.format(CatalogQueries.GET_CATALOGPAGE_PLID_CATALOGTYPE, catalogpageid) );
		long plid = XMLHelper.getLongValueOf(selectDom, "/rs/row/@plid");
		ErrorRaiser.throwIfFalse(plid > 0, IterErrorKeys.XYZ_E_CATALOG_PAGE_PLID_NOT_EXISTS_ZYX);
		
		Layout l = LayoutLocalServiceUtil.getLayout(plid);
		l = cloneCatalogLayout(l, XMLHelper.getTextValueOf(selectDom, "/rs/row/@catalogtype", ""), XMLHelper.getTextValueOf(dom, "/catpage/@srccatalogpageid"));
		
		l.setDescription(description);
		LayoutLocalServiceUtil.updateName(l, pagename, LocaleUtil.getDefault().toString());

		return getCatalogPage(catalogpageid, true);
	}
	
	/**
	 * 
	 * @param catalogPageIds
	 * @throws Exception
	 */
	public String deleteCatalogPages(String catalogPageIds) throws Exception
	{
		try
		{
			ErrorRaiser.throwIfFalse(Validator.isNotNull(catalogPageIds), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
	
			Document dom = SAXReaderUtil.read(catalogPageIds);
			List<Node> nodeIDs = dom.selectNodes("//catpage/@catalogpageid");
			
			String[] idsList = XMLHelper.getStringValues(nodeIDs);
			ErrorRaiser.throwIfFalse(idsList.length > 0);
			String idsCommaSeparated = StringUtil.merge(idsList, "','");
			
			// Se comprueba que ninguno de los CatalogElements del catálogo esté en uso en alguna configuración
			CatalogUtil.checkCatalogPageUse(idsList);
			
			//Se comprueba que ninguno de las CatalogPages está publicada
			CatalogUtil.checkCatalogPageExistsInLive(idsCommaSeparated, true);
			
			List<Object> result = PortalLocalServiceUtil.executeQueryAsList( String.format(CatalogQueries.GET_CATALOGPAGE_PLID, idsCommaSeparated) );
			for(Object plid : result)
			{
				LayoutLocalServiceUtil.deleteLayout( Long.valueOf( String.valueOf(plid) ) );
			}
			
			PortalLocalServiceUtil.executeUpdateQuery( String.format(CatalogQueries.DELETE_CATALOGPAGES, idsCommaSeparated) );
		}
		catch (ServiceError se)
		{
			// Si la página de catálogo NO exise, esto NO supondrá un problema para el borrado
			if (!se.getErrorCode().equals(IterErrorKeys.XYZ_E_CATALOG_PAGE_PLID_NOT_EXISTS_ZYX))
				throw se;
		}
		
		return catalogPageIds;
	}

	/**
	 * 
	 * @param catalogPageId
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public Document addCatalogElements(String data) throws Exception
	{
		Document dom = SAXReaderUtil.read(data);
		String catalogPageId = XMLHelper.getTextValueOf(dom, "/catpage/@catalogpageid");
		ErrorRaiser.throwIfNull(catalogPageId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		List<String> usedColumns = CatalogUtil.getUsedColumns(catalogPageId);
		if (usedColumns.size() > 1)
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_CATALOG_MULTIPLE_DESIGNED_COLUMNS_ZYX, StringUtil.merge(usedColumns));
		
		
		Document domElem = PortalLocalServiceUtil.executeQueryAsDom( String.format(CatalogQueries.GET_CATALOGELEMENTS, catalogPageId) );
		ErrorRaiser.throwIfNull(domElem);
		
		StringBuilder sqlInsert = new StringBuilder();
		StringBuilder sqlDelete = new StringBuilder();
		
		Document updateDom = SAXReaderUtil.read("<rs/>");
		Element  updateRoot= updateDom.getRootElement();

		// Solo se importa el elemento que está maquetado
		if (usedColumns.size() == 1)
		{
			String columnId = usedColumns.get(0);
			Node newNode = dom.selectSingleNode( String.format("%s/%s[@columnid='%s']", CatalogUtil.rowCatPageName, CatalogUtil.rowCatElemName, columnId) );
			
			// Se intenta localizar antes el elemento guardado que tenga dicha posición
			Node oldNode = domElem.selectSingleNode( String.format("rs/row[@columnid='%s']", columnId) );
			
			if ( Validator.isNull(oldNode) )
			{
				// No existe un Elemento guardado con dicha columna, se intenta tomar el primero de los guardados
				oldNode = domElem.selectSingleNode("rs/row[1]");
			}
			
			String pos = XMLHelper.getTextValueOf( newNode, "pos" );
			
			if ( Validator.isNull(oldNode) )
			{
				// El nodo es nuevo, se añade a la lista de las inserciones
				sqlInsert.append( String.format(",(ITR_UUID(),'%s','%s',\"%s\")", catalogPageId, columnId, pos) );
			}
			else
			{
				// El nodo ya existe, se añade a la lista de las actualizaciones
				Element updateElem = (Element)oldNode.detach();
				updateElem.addAttribute("columnid", columnId);
				updateElem.addElement("pos").addCDATA(pos);
				updateRoot.add(updateElem);
			}
		}
		

		// Se realiza la inserción
		if (sqlInsert.length() > 0)
		{
			sqlInsert.deleteCharAt(0);
			sqlInsert.insert(0, "INSERT INTO catalogelement (catalogelementid,catalogPageId,columnId,pos) VALUES ");
			PortalLocalServiceUtil.executeUpdateQuery( sqlInsert.toString() );
		}
		
		// Borrado. A este punto SOLO se puede llegar si la página tiene maquetada una columna o ninguna
		// Si tiene maquetada una columna (1 - usedColumns.size()) == 0: Que se borren todos los CatalogElements si es necesario.
		// Si NO tiene maquetada NINGUNA columna (1 - usedColumns.size()) == 1: Se borran TODOS los catalogElements menos 1, de 
		//		modo que cuando maquete portlets se asigne la columna maquetada a dicho CatalogElement. Si un CatalogElement NO 
		//		tiene portlets en dicha columna simplemente no pinta nada
		List<Node> deleteNodes = domElem.selectNodes("rs/row/@catalogelementid");
		for (int i = 0; i < deleteNodes.size() - (1 - usedColumns.size())  ; i++)
		{
			sqlDelete.append( String.format(",'%s'", deleteNodes.get(i).getStringValue()) );
		}
		
		if (sqlDelete.length() > 0)
		{
			sqlDelete.deleteCharAt(0);
			sqlDelete.insert(0, "DELETE FROM catalogelement WHERE catalogelementid IN (").append(")");
			PortalLocalServiceUtil.executeUpdateQuery( sqlDelete.toString() );
		}
		
		// Se realiza la actualización
		if (updateDom.getRootElement().elements().size() > 0)
			PortalLocalServiceUtil.executeUpdateQuery( String.format(CatalogQueries.CALL_UPDATEELEMENTS, updateDom.asXML().replaceAll("'", "\"")) );
		
		// Se realiza la actualización de la página
		String pos = XMLHelper.getTextValueOf( dom, "/catpage/pos/text()", "null" );
		String sql = String.format( CatalogQueries.UPDT_CATALOGPAGE_POS, pos, catalogPageId);
		PortalLocalServiceUtil.executeUpdateQuery( sql );
		
		return getCatalogPage(catalogPageId, true);
	}
	
	public String getDataCatalogElement(  String elementIds ) throws Exception
    {
		String pref = "";
		
		Document dom = SAXReaderUtil.read(elementIds);
		List<Node> nodeIDs = dom.selectNodes("//tab/@elementid");
		
		String[] idsList = XMLHelper.getStringValues(nodeIDs);
		ErrorRaiser.throwIfFalse(idsList.length > 0);
		
		Document domElem = PortalLocalServiceUtil.executeQueryAsDom( String.format(CatalogQueries.GET_CATALOG_PREFERENCES,  StringUtil.merge(idsList, "','")) );
		ErrorRaiser.throwIfNull(domElem);
			
		pref = domElem.asXML();
		
		return pref;
    }

	/**
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public String getCatalogPageURL(String data) throws Exception
	{
		Element root = SAXReaderUtil.read(data).getRootElement();
		
		String catalogPageURL = CatalogUtil.getCatalogPageDesignerURL( root, false );
		//la llamada a la ventana de maquetación se ha echo desde Milenium, por tanto,se concatena '&callfromMln=y'al final de la URL que permite el diseño de la página del catálogo.
		catalogPageURL = new String( catalogPageURL.concat("&" + IterKeys.REQUEST_PARAMETER_CALL_FROM_MLN + "=") ).concat("y");
		
		return catalogPageURL;
	}
}