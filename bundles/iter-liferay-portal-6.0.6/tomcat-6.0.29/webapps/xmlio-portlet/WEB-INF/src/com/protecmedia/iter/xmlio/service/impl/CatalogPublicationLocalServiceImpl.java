package com.protecmedia.iter.xmlio.service.impl;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.CatalogQueries;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.util.xml.CDATAUtil;
import com.protecmedia.iter.xmlio.service.base.CatalogPublicationLocalServiceBaseImpl;

@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class CatalogPublicationLocalServiceImpl extends CatalogPublicationLocalServiceBaseImpl {
	
	private static Log _log = LogFactoryUtil.getLog(CatalogPublicationLocalServiceImpl.class);
	
	// Publica un catálogo (menu, cabecera y pie) con sus páginas y elementos
	public void importCatalogPage(long layoutPlid, long scopegroupid, Element xmlData) throws IOException, SQLException, ServiceError, SystemException{		
		_log.trace("In CatalogPublicationLocalServiceImpl.publishToLive");
		
		ErrorRaiser.throwIfFalse(layoutPlid > 0,               IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid layoutPlid");
		ErrorRaiser.throwIfFalse(Validator.isNotNull(xmlData), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid importationXmlElement");
		
		Node xmlDataNode = (Node)xmlData;
		// Catálogo.
		String catalogId = XMLHelper.getTextValueOf(xmlDataNode, "param[@name='catalog_catalogid']");
		
		if (Validator.isNotNull(catalogId))
		{
			catalogId = CDATAUtil.strip(catalogId);			
			
			String name    = CDATAUtil.strip(XMLHelper.getTextValueOf(xmlDataNode, "param[@name='catalog_catalogname']"));
			String type    = CDATAUtil.strip(XMLHelper.getTextValueOf(xmlDataNode, "param[@name='catalog_catalogtype']"));
			
			String description = XMLHelper.getTextValueOf(xmlDataNode, "param[@name='catalog_description']");
			if(Validator.isNotNull(description))
				description = CDATAUtil.strip(description);	
							
			String sql = String.format(CatalogQueries.INSERT_OR_UPDATE_CATALOG, catalogId, scopegroupid, name, 
				                                                                ("null".equals(description) ? "null" : "'" + StringEscapeUtils.escapeSql(description) + "'"), 
				                                                                type);
			if (_log.isDebugEnabled())
				_log.debug(new StringBuilder("Query to insert/update catalog:\n").append(sql));	
			
			PortalLocalServiceUtil.executeUpdateQuery(sql);
			
			// Página de catálogo
			String values = "";
			StringBuilder auxValues = new StringBuilder("");
			String auxValuesSql = "('%s','%s', %s, %s),";
			
			String catalogPageId = CDATAUtil.strip(XMLHelper.getTextValueOf(xmlDataNode, "param[@name='catalog_page_0_catalogpageid']"));
			String pos                  = CDATAUtil.strip(XMLHelper.getTextValueOf(xmlDataNode, "param[@name='catalog_page_0_pos']"      ));
			
			auxValues.append(
					String.format(auxValuesSql, catalogPageId, catalogId, layoutPlid, 
										                 ("null".equals(pos) ? "null" : "'" + StringEscapeUtils.escapeSql(pos) + "'")));
			
			if( Validator.isNotNull(auxValues.toString()) )
			{
				// Quitamos la última coma porque sobra
				values = auxValues.toString().substring(0, auxValues.toString().length() -1);
				
				sql = String.format(CatalogQueries.INSERT_OR_UPDATE_CATALOG_PAGE, values);
				if (_log.isDebugEnabled())
					_log.debug(new StringBuilder("Query to insert/update catalog pages:\n").append(sql));		
				PortalLocalServiceUtil.executeUpdateQuery(sql);
			}
			
			// Elementos de catálogo
			auxValues = new StringBuilder("");
			auxValuesSql = "('%s', '%s', '%s', %s),";
			int e = 0;
			String catalogElementid = XMLHelper.getTextValueOf(xmlDataNode, "param[@name='element_page_" + e + "_catalogelementid']");
			String[] catalogElements = new String[]{};
			
			// Mientras haya elementos de páginas de catálogo
			while(Validator.isNotNull(catalogElementid))
			{		
				catalogElementid            = CDATAUtil.strip(catalogElementid);
				String columnId             = CDATAUtil.strip(XMLHelper.getTextValueOf(xmlDataNode, "param[@name='element_page_" + e + "_columnId']"     ));
				String elementPos           = CDATAUtil.strip(XMLHelper.getTextValueOf(xmlDataNode, "param[@name='element_page_" + e + "_pos']"          ));
					
				auxValues.append(
						String.format(auxValuesSql, catalogElementid, catalogPageId, columnId, 
															 ("null".equals(elementPos) ? "null": "'" + StringEscapeUtils.escapeSql(elementPos) + "'")));
				e++;
				catalogElements = ArrayUtil.append(catalogElements, catalogElementid);
				catalogElementid = XMLHelper.getTextValueOf(xmlDataNode, "param[@name='element_page_" + e + "_catalogelementid']");
			}
			
			if( Validator.isNotNull(auxValues.toString()) )
			{
				// Quitamos la última coma porque sobra
				values = auxValues.toString().substring(0, auxValues.toString().length() -1);
				
				sql = String.format(CatalogQueries.DELETE_CATALOG_PAGE_ELEMENTS, catalogPageId, catalogElements.length>1 ? StringUtil.merge(catalogElements, "','") : catalogElements[0]);
				PortalLocalServiceUtil.executeUpdateQuery(sql);
				
				sql = String.format(CatalogQueries.INSERT_OR_UPDATE_CATALOG_ELEMENT, values);
				if (_log.isDebugEnabled())
					_log.debug(new StringBuilder("Query to insert/update catalog pages:\n").append(sql));		
				PortalLocalServiceUtil.executeUpdateQuery(sql);
			}
			
		}else if(_log.isDebugEnabled()){
			_log.debug(new StringBuilder("No catalog found in ").append(layoutPlid).append(" layout"));
		}
	}
}