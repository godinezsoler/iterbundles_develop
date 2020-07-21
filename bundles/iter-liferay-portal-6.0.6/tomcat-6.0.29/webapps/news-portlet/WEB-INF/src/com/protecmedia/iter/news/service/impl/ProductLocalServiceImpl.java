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
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.apache.ApacheHierarchy;
import com.liferay.portal.apache.ApacheUtil;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.CategoriesUtil;
import com.liferay.portal.kernel.util.Digester;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.kernel.xml.Attribute;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.service.BinaryRepositoryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.PaywallUtil;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.TeaserMgr;
import com.protecmedia.iter.news.scheduler.DeleteSessionsTask;
import com.protecmedia.iter.news.service.base.ProductLocalServiceBaseImpl;
import com.protecmedia.iter.news.service.item.Product;
import com.protecmedia.iter.news.service.item.ProductXmlIO;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.documentlibrary.DLFileEntryXmlIO;
import com.protecmedia.iter.xmlio.service.item.journal.JournalArticleXmlIO;

/**
 * The implementation of the product local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.news.service.ProductLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.news.service.ProductLocalServiceUtil} to access the product local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author protec
 * @see com.protecmedia.iter.news.service.base.ProductLocalServiceBaseImpl
 * @see com.protecmedia.iter.news.service.ProductLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class ProductLocalServiceImpl extends ProductLocalServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(ProductLocalServiceImpl.class);
	
	private static final String DELETE_SESSIONS_GROUP_NAME			= "DELETE_PAYWALL_SESSIONS";
	
	private static final String METHOD_HEAD							= "HEAD";
	private static final String METHOD_GET							= "GET";
	
	private static ThreadGroup deleteSessions 						= new ThreadGroup(DELETE_SESSIONS_GROUP_NAME);
	
	private static final int DEFAULT_DELETE_THRESHOLD_DAYS			= 30;
	
	private static ItemXmlIO productXmlIO 							= new ProductXmlIO();
	private static ItemXmlIO journalArticleXmlIO 					= new JournalArticleXmlIO();
	private static ItemXmlIO dLFileEntryXmlIO 						= new DLFileEntryXmlIO();
	
	public static final String GET_UUID 							= "SELECT uuid()";
	
	//Product
	private static final String GET_PRODUCT_ASSIGNMENT				= new StringBuilder(
		"SELECT DISTINCT Group_.groupId, Group_.name, IF (productId IS NULL, 0, 1) assignment	\n").append(
		"FROM Group_																			\n").append(
		"LEFT JOIN Product ON Group_.groupId = Product.groupid									\n").append(
		"	WHERE type_= 0 AND friendlyURL NOT IN ('/null', '/test')							\n").append(
		"	ORDER BY assignment DESC															\n").append(
		"LIMIT 2																				\n").toString();
	
	public static final String GET_PRODUCT_GROUPID					= "SELECT groupid FROM Product WHERE %s LIMIT 1";
	public static final String GET_PRODUCTNAME_BY_ID 				= "SELECT name FROM Product WHERE productId='%s'";
	private static final String GET_PRODUCTID_BY_NAME 				= "SELECT productId FROM Product WHERE groupid=%d AND name='%s'";
	public static final String ADD_PRODUCT 							= "INSERT INTO Product (productId, name, nameBase64, groupid) VALUES('%s', '%s', '%s', %d)";
	public static final String DELETE_PRODUCT 						= "DELETE FROM Product WHERE %s";
	public static final String UPDATE_PRODUCT 						= "UPDATE Product SET NAME='%s' WHERE productId='%s'";
	
	//ArticleProduct
	public static final String ADD_ARTICLE_PRODUCTS 				= "INSERT INTO ArticleProduct VALUES %s";
	public static final String DELETE_ARTICLE_PRODUCTS 				= "DELETE FROM ArticleProduct WHERE articleId='%s'";
	public static final String DELETE_DISCRET_ARTICLE_PRODUCTS		= "DELETE FROM ArticleProduct WHERE %s AND articleId='%s'";
	
	//FileEntryProduct
	public static final String GET_FILEID_BY_NAME 					= "SELECT fileEntryId FROM DLFileEntry WHERE NAME='%s'";
	public static final String ADD_FILEENTRY_PRODUCTS 				= "INSERT INTO FileEntryProduct VALUES %s";
	public static final String DELETE_FILEENTRY_PRODUCTS 			= "DELETE FROM FileEntryProduct WHERE fileEntryId=%s";
	public static final String DELETE_DISCRET_FILEENTRY_PRODUCTS 	= "DELETE FROM FileEntryProduct WHERE %s AND fileEntryId=%s";

	//Live
	public static final String UPDATE_LIVE_STATUS 					= "UPDATE Xmlio_Live SET status='%s' WHERE classnamevalue='%s' AND localId='%s'";
	
	//Paywall Products
	public static final String GET_PAYWALL_PRODUCT_VERSION 			= "SELECT * FROM iterpaywall_version WHERE groupid=%s";
	public static final String ADD_PAYWALL_PRODUCT_VERSION 			= "INSERT INTO iterpaywall_version VALUES(%s, %s, UUID())";
	public static final String UPDATE_PAYWALL_PRODUCT_VERSION 		= "UPDATE iterpaywall_version SET version=UUID() WHERE groupid=%s";
	public static final String GET_LAYOUTSET_BY_GROUPID 			= "SELECT layoutSetId FROM LayoutSet WHERE groupid=%s AND privateLayout=0";
	
	public static final String GET_PAYWALL_PRODUCT_BY_ID = new StringBuilder()
	.append("SELECT ipp.id, ipp.groupid, ipp.pname, ipp.pnamebase64,         \n")
	.append("ipp.pdescription, ipp.id, ipp.ptype, ipp.pquan, ipp.pplazo,     \n")
	.append("IF(ipp.pnoreg = TRUE, '1', '0') pnoreg,                         \n")
	.append("IF(ipp.onlyvalarts = TRUE, '1', '0') onlyvalarts, ipp.maxcon,   \n")
	.append("ipp.urlname, ipp.price, ipp.currencyAlpha, ipp.currencyNumeric, \n")
	.append("ipp.validity, ipp.articleid, ipp.externalid                     \n")
	.append("FROM iterpaywall_product ipp WHERE ipp.id=%d")
	.toString();

	
	public static final String GET_PAYWALL_PRODUCTS = new StringBuilder()
	.append("SELECT ipp.id, ipp.groupid, ipp.pname, ipp.pnamebase64,         \n")
	.append("ipp.pdescription, ipp.id, ipp.ptype, ipp.pquan, ipp.pplazo,     \n")
	.append("IF(ipp.pnoreg = TRUE, '1', '0') pnoreg,                         \n")
	.append("IF(ipp.onlyvalarts = TRUE, '1', '0') onlyvalarts, ipp.maxcon,   \n")
	.append("ipp.urlname, ipp.price, ipp.currencyAlpha, ipp.currencyNumeric, \n")
	.append("ipp.validity, ipp.articleid, ipp.externalid                     \n")
	.append("FROM iterpaywall_product ipp WHERE ipp.groupid=%s")
	.toString();
	
	public static final String GET_PAYWALL_PRODUCT_BY_NAME_GROUPID = GET_PAYWALL_PRODUCTS + " AND ipp.pname='%s'";
	
	public static final String GET_PAYWALL_PRODUCT_BY_URLNAME_GROUPID = GET_PAYWALL_PRODUCTS + " AND ipp.urlname='%s'";
	
	public static final String ADD_PAYWALL_PRODUCT = new StringBuilder()
	.append("INSERT INTO iterpaywall_product (groupid, pname, pnamebase64, pdescription,   \n")
	.append("  ptype, pquan, pplazo, pnoreg, maxcon, version, onlyvalarts,                 \n")
	.append("  urlname, price, currencyAlpha, currencyNumeric, validity, externalId)       \n")
	.append("VALUES (%s, '%s', '%s', %s, %s, %s, %s, %s, %s, 0, %s, '%s', %s, %s, %s, %s, %s)")
	.toString();		
	
	public static final String ADD_PAYWALL_PRODUCT_RELATED 			= "INSERT INTO iterpaywall_product_related (paywallproductid, productid) VALUES ";
	public static final String ADD_PAYWALL_PRODUCT_RELATED_VALUES	= "('%s', '%s')";
	public static final String DELETE_PAYWALL_PRODUCTS_RELATED_ID	= "DELETE FROM iterpaywall_product_related WHERE paywallproductid = '%s'";
	public static final String GET_PAYWALL_PRODUCTS_RELATEDS		= new StringBuilder(	"SELECT ipr.productid, p.name, p.nameBase64 FROM iterpaywall_product_related ipr " ).append(
																							"INNER JOIN Product p ON p.productId = ipr.productid " 							   ).append(
																							"WHERE ipr.paywallproductid = '%s'"												   ).toString();
	
	public static final String DELETE_PAYWALL_PRODUCTS 				= "DELETE FROM iterpaywall_product WHERE id IN %s";
	
	public static final String UPDATE_PAYWALL_PRODUCT = new StringBuilder()
	.append("UPDATE iterpaywall_product SET pname='%s', pnamebase64='%s', pdescription=%s, ptype=%s, pquan=%s, pplazo=%s, pnoreg=%s, \n")
	.append("maxcon=%s, version=version + 1, onlyvalarts=%s, urlname='%s', price=%s, currencyAlpha=%s, currencyNumeric=%s, validity=%s, externalid=%s WHERE id=%s ")
	.toString();

	public static final String GET_SELECTED_PRODUCTS 				= new StringBuilder(	
		"SELECT p.name, p.productId, IF((SELECT p.productId IN %s), TRUE, FALSE) AS selected 	\n").append(
		"FROM Product p 																		\n").append(
		"	WHERE groupid = %d																	\n").append(
		"	ORDER BY p.name ASC, p.productId ASC												\n").toString();
	
	public static final String DELETE_PAYWALL_EXPIRES				= "DELETE FROM %s WHERE fechacaducidad < '%s'";
	
	public static final String UPDATE_PAYWALL_MSGS					= new StringBuilder(	"UPDATE Group_Config SET maxconnexceedmsg='%s', maxdaysexceedmsg='%s', "		).append(
																							"maxarticlesexceedmsg='%s', paywalloutofservice='%s' WHERE groupId=%s"			).toString();
	
	public static final String INSERT_PAYWALL_MSGS					= new StringBuilder(	"INSERT into Group_Config (groupId, lastPublicationDate, "						).append(
																							"maxconnexceedmsg, maxdaysexceedmsg, maxarticlesexceedmsg, "					).append(
																							"paywalloutofservice) values(%s)"												).toString();		
	
	public static final String GET_PAYWALL_MSGS						= new StringBuilder(	"SELECT maxconnexceedmsg, maxdaysexceedmsg, maxarticlesexceedmsg, " 			).append(
																	  						"paywalloutofservice, groupId FROM Group_Config WHERE groupId=%s"				).toString();		
		
	public static final String SERVER_STATUS_URL					= "/server-status";
	public static final String SERVER_STATUS_ACTIVATE_PARAM			= "?paywall_status=Y";
	public static final String SERVER_STATUS_DEACTIVATE_PARAM		= "?paywall_status=N";
	public static final String SERVER_GET_PAYWALL_MODE				= "?getpaywallmode";
	
	//	<?xml version="1.0"?>
	//	<param>
	//		<target name="Premium"/>
	//	</param>
	
	public String addProduct(String xml) throws IOException, SQLException, DocumentException, ServiceError, PortalException, SystemException, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(xml, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Element root = SAXReaderUtil.read(xml).getRootElement();
		
		long groupId = _getProductGroupId(root);
		
		XPath xpath = SAXReaderUtil.createXPath("/param/target");
		
		List<Node> nodes = xpath.selectNodes(root);
		ErrorRaiser.throwIfFalse((nodes != null && nodes.size() == 1), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		Element element = (Element)nodes.get(0);
			
		Attribute attributeName = element.attribute("name");
		ErrorRaiser.throwIfNull(attributeName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String name = attributeName.getValue();
		ErrorRaiser.throwIfNull(name, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String nameBase64 = Base64.encode(name.getBytes(Digester.ENCODING));
		
		String productId = PortalUUIDUtil.generate();
		String query = String.format(ADD_PRODUCT, productId, name.replaceAll("'", "''"), nameBase64, groupId);
		PortalLocalServiceUtil.executeUpdateQuery(query);
		
		productXmlIO.createLiveEntry(new Product(productId, null, null, groupId));
			
		return productId;
	}
	
	private long _getProductGroupId(Element elem) throws SecurityException, NoSuchMethodException, ServiceError
	{
		long groupId = XMLHelper.getLongValueOf(elem, "@siteid");
		
		if (groupId <= 0)
		{
			// Si no se ha especificado un grupo válido se obtiene el grupo al que se han asignado los productos anteriores
			Document dom = PortalLocalServiceUtil.executeQueryAsDom(GET_PRODUCT_ASSIGNMENT);
			
			// Se cuentan cuántos grupos contienen productos
			long numAssignments = XMLHelper.getLongValueOf(dom, "count(/rs/row[@assignment='1'])");
			if (numAssignments == 0)
			{
				// Si ningún grupo contiene productos, será necesario que exista solo UN grupo
				List<Node> groupList = dom.selectNodes("/rs/row");
				if (groupList.size() == 1)
					groupId = XMLHelper.getLongValueOf( groupList.get(0), "@groupId" ); 
			}
			else if (numAssignments == 1)
			{
				// Solo un grupo contiene productos
				groupId = XMLHelper.getLongValueOf(dom, "/rs/row[@assignment='1']/@groupId");
			}
			
			ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_UPDATE_MILENIUM_ZYX, IterErrorKeys.ITER_UPDATE_MILENIUM_ITER);
		}
		
		return groupId;
	}
	
	//	<?xml version="1.0"?>
	//	<param>
	//		<target id="6b5d534d-cead-4f89-bcef-a915defe7595"/>
	//		<target id="7b5d534d-cead-4f89-bcef-a915defe7595"/>
	//	</param>

	public void deleteProduct(String xml) throws IOException, SQLException, DocumentException, ServiceError, PortalException, SystemException, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(xml, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Element root = SAXReaderUtil.read(xml).getRootElement();
		XPath xpath = SAXReaderUtil.createXPath("/param/target");
		
		List<Node> nodes = xpath.selectNodes(root);
		ErrorRaiser.throwIfFalse((nodes != null && nodes.size() > 0), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		String productSubquery = getDeleteValuesSQL(nodes);
		long groupId = XMLHelper.getLongValueOf( PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_PRODUCT_GROUPID, productSubquery)), "/rs/row/@groupid" );
		PortalLocalServiceUtil.executeUpdateQuery( String.format(DELETE_PRODUCT, productSubquery) );
		
		for(Node node:nodes)
		{
			Element element = (Element)node;
			
			Attribute attributeId = element.attribute("id");
			ErrorRaiser.throwIfNull(attributeId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			String productId = attributeId.getValue();
			ErrorRaiser.throwIfNull(productId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			productXmlIO.deleteLiveEntry(new Product(productId, null, null, groupId));
		}
	}
	
	//	<?xml version="1.0"?>
	//	<param>
	//		<target id="6b5d534d-cead-4f89-bcef-a915defe7595" name="Premium2"/>
	//	</param>
	
	public void updateProduct(String xml) throws IOException, SQLException, DocumentException, ServiceError, PortalException, SystemException, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(xml, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Element root = SAXReaderUtil.read(xml).getRootElement();
		XPath xpath = SAXReaderUtil.createXPath("/param/target");
		
		List<Node> nodes = xpath.selectNodes(root);
		ErrorRaiser.throwIfFalse((nodes != null && nodes.size() == 1), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String productSubquery = getDeleteValuesSQL(nodes);
		long groupId = XMLHelper.getLongValueOf( PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_PRODUCT_GROUPID, productSubquery)), "/rs/row/@groupid" );

		Element element = (Element)nodes.get(0);
			
		Attribute attributeId = element.attribute("id");
		ErrorRaiser.throwIfNull(attributeId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Attribute attributeName = element.attribute("name");
		ErrorRaiser.throwIfNull(attributeName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String productId = attributeId.getValue();
		ErrorRaiser.throwIfNull(productId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String name = attributeName.getValue();
		ErrorRaiser.throwIfNull(name, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String query = String.format(UPDATE_PRODUCT, name.replaceAll("'", "''"), productId);
		PortalLocalServiceUtil.executeUpdateQuery(query);
		
		productXmlIO.createLiveEntry(new Product(productId, name, null, groupId));
	}
	
	
	//	<?xml version="1.0"?>
	//	<param>
	//		<target id="1234"/>
	//		<product id="6b5d534d-cead-4f89-bcef-a915defe7595"/>
	//		<product id="7b5d534d-cead-4f89-bcef-a915defe7595"/>
	//		<product id="8b5d534d-cead-4f89-bcef-a915defe7595"/>
	//	</param>
	
	public void setProductsOfJournalArticle(String xml) throws IOException, SQLException, DocumentException, ServiceError, PortalException, SystemException
	{
		ErrorRaiser.throwIfNull(xml, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Element root = SAXReaderUtil.read(xml).getRootElement();
		
		//JournalArticle
		XPath xpath = SAXReaderUtil.createXPath("/param/target");
		
		List<Node> nodes = xpath.selectNodes(root);
		ErrorRaiser.throwIfFalse((nodes != null && nodes.size() == 1), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		Element element = (Element)nodes.get(0);
			
		Attribute attributeId = element.attribute("id");
		ErrorRaiser.throwIfNull(attributeId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
		String articleId = attributeId.getValue();
		ErrorRaiser.throwIfNull(articleId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		//Product
		xpath = SAXReaderUtil.createXPath("/param/product");
		
		nodes = xpath.selectNodes(root);
		ErrorRaiser.throwIfFalse(nodes != null, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		//Delete current ArticleProducts
		String query = String.format(DELETE_ARTICLE_PRODUCTS, articleId);
		PortalLocalServiceUtil.executeUpdateQuery(query);
		
		if(nodes.size() > 0)
		{
			String subQuery = getInsertValuesSQL(nodes, articleId);
			ErrorRaiser.throwIfFalse(!subQuery.isEmpty(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
	
			//Add new ArticleProducts
			query = String.format(ADD_ARTICLE_PRODUCTS, subQuery);
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
		//Live JournalArticle
		JournalArticle ja = JournalArticleLocalServiceUtil.getArticle(GroupMgr.getGlobalGroupId(), articleId);
		ErrorRaiser.throwIfNull(ja, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		journalArticleXmlIO.createLiveEntry(ja);
	}
	
	//	<?xml version="1.0"?>
	//	<param>
	//		<target id="1234"/>
	//		<product action="add" id="6b5d534d-cead-4f89-bcef-a915defe7595"/>
	//		<product action="add" id="7b5d534d-cead-4f89-bcef-a915defe7595"/>
	//		<product action="add" id="8b5d534d-cead-4f89-bcef-a915defe7595"/>
	//		<product action="delete" id="4b5d534d-cead-4f89-bcef-a915defe7595"/>
	//		<product action="delete" id="5b5d534d-cead-4f89-bcef-a915defe7595"/>
	//	</param>
	
	public void updateProductsOfJournalArticle(String xml) throws IOException, SQLException, DocumentException, ServiceError, PortalException, SystemException
	{
		ErrorRaiser.throwIfNull(xml, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Element root = SAXReaderUtil.read(xml).getRootElement();
		
		//JournalArticle
		XPath xpath = SAXReaderUtil.createXPath("/param/target");
		
		List<Node> nodes = xpath.selectNodes(root);
		ErrorRaiser.throwIfFalse((nodes != null && nodes.size() == 1), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Element element = (Element)nodes.get(0);
			
		Attribute attributeId = element.attribute("id");
		ErrorRaiser.throwIfNull(attributeId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
		String articleId = attributeId.getValue();
		ErrorRaiser.throwIfNull(articleId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String query = null;
		String subQuery = null;
		
		//Delete ArticleProducts
		xpath = SAXReaderUtil.createXPath("/param/product[@action='delete']");
		nodes = xpath.selectNodes(root);
		ErrorRaiser.throwIfFalse((nodes != null), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		if(nodes.size() > 0)
		{
			subQuery = getDeleteValuesSQL(nodes);
			ErrorRaiser.throwIfFalse(!subQuery.isEmpty(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			query = String.format(DELETE_DISCRET_ARTICLE_PRODUCTS, subQuery, articleId);
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
		//Add ArticleProducts
		xpath = SAXReaderUtil.createXPath("/param/product[@action='add']");
		nodes = xpath.selectNodes(root);
		ErrorRaiser.throwIfFalse((nodes != null), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		if(nodes.size() > 0)
		{
			subQuery = getInsertValuesSQL(nodes, articleId);
			ErrorRaiser.throwIfFalse(!subQuery.isEmpty(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			query = String.format(ADD_ARTICLE_PRODUCTS, subQuery, articleId);
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
		//Live JournalArticle
		JournalArticle ja = JournalArticleLocalServiceUtil.getArticle(GroupMgr.getGlobalGroupId(), articleId);
		ErrorRaiser.throwIfNull(ja, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		journalArticleXmlIO.createLiveEntry(ja);
	}
	
	
	//	<?xml version="1.0"?>
	//	<param>
	//		<target id="1234"/>
	//		<product id="6b5d534d-cead-4f89-bcef-a915defe7595"/>
	//		<product id="7b5d534d-cead-4f89-bcef-a915defe7595"/>
	//		<product id="8b5d534d-cead-4f89-bcef-a915defe7595"/>
	//	</param>
	
	public void setProductsOfFileEntry(String xml) throws IOException, SQLException, DocumentException, ServiceError, NumberFormatException, PortalException, SystemException
	{
		ErrorRaiser.throwIfNull(xml, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Element root = SAXReaderUtil.read(xml).getRootElement();
		
		//DLFileEntry
		XPath xpath = SAXReaderUtil.createXPath("/param/target");
		
		List<Node> nodes = xpath.selectNodes(root);
		ErrorRaiser.throwIfFalse((nodes != null && nodes.size() == 1), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		Element element = (Element)nodes.get(0);
			
		Attribute attributeId = element.attribute("id");
		ErrorRaiser.throwIfNull(attributeId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String fileEntryName = attributeId.getValue();
		ErrorRaiser.throwIfNull(fileEntryName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String query = String.format(GET_FILEID_BY_NAME, fileEntryName);
		List<Object> fileEntryIdResult = PortalLocalServiceUtil.executeQueryAsList(query);
		ErrorRaiser.throwIfFalse((fileEntryIdResult != null && fileEntryIdResult.size() == 1 && 
								  fileEntryIdResult.get(0) != null), IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
		
		String fileEntryId = fileEntryIdResult.get(0).toString();
		
		//Product
		xpath = SAXReaderUtil.createXPath("/param/product");
		
		nodes = xpath.selectNodes(root);
		ErrorRaiser.throwIfFalse(nodes != null, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		//Delete current FileEntryProducts
		query = String.format(DELETE_FILEENTRY_PRODUCTS, fileEntryId);
		PortalLocalServiceUtil.executeUpdateQuery(query);
		
		if(nodes.size() > 0)
		{
			String subQuery = getInsertValuesSQL(nodes, fileEntryId);
			ErrorRaiser.throwIfFalse(!subQuery.isEmpty(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
	
			//Add new FileEntryProducts
			query = String.format(ADD_FILEENTRY_PRODUCTS, subQuery);
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
		//Live DLFileEntry
		DLFileEntry dlfe = DLFileEntryLocalServiceUtil.getDLFileEntry(Long.parseLong(fileEntryId));
		ErrorRaiser.throwIfNull(dlfe, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		dLFileEntryXmlIO.createLiveEntry(dlfe);
	}
	
	//	<?xml version="1.0"?>
	//	<param>
	//		<target id="1234"/>
	//		<product action="add" id="6b5d534d-cead-4f89-bcef-a915defe7595"/>
	//		<product action="add" id="7b5d534d-cead-4f89-bcef-a915defe7595"/>
	//		<product action="add" id="8b5d534d-cead-4f89-bcef-a915defe7595"/>
	//		<product action="delete" id="4b5d534d-cead-4f89-bcef-a915defe7595"/>
	//		<product action="delete" id="5b5d534d-cead-4f89-bcef-a915defe7595"/>
	//	</param>
	
	public void updateProductsOfFileEntry(String xml) throws IOException, SQLException, DocumentException, ServiceError, NumberFormatException, PortalException, SystemException
	{
		ErrorRaiser.throwIfNull(xml, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Element root = SAXReaderUtil.read(xml).getRootElement();
		
		//DLFileEntry
		XPath xpath = SAXReaderUtil.createXPath("/param/target");
		
		List<Node> nodes = xpath.selectNodes(root);
		ErrorRaiser.throwIfFalse((nodes != null && nodes.size() == 1), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		Element element = (Element)nodes.get(0);
			
		Attribute attributeId = element.attribute("id");
		ErrorRaiser.throwIfNull(attributeId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String fileEntryName = attributeId.getValue();
		ErrorRaiser.throwIfNull(fileEntryName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String query = String.format(GET_FILEID_BY_NAME, fileEntryName);
		List<Object> fileEntryIdResult = PortalLocalServiceUtil.executeQueryAsList(query);
		ErrorRaiser.throwIfFalse((fileEntryIdResult != null && fileEntryIdResult.size() == 1 && 
								  fileEntryIdResult.get(0) != null), IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
		
		String fileEntryId = fileEntryIdResult.get(0).toString();
		
		String subQuery = null;
		
		//Delete FileEntryProducts
		xpath = SAXReaderUtil.createXPath("/param/product[@action='delete']");
		nodes = xpath.selectNodes(root);
		ErrorRaiser.throwIfFalse((nodes != null), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		if(nodes.size() > 0)
		{
			subQuery = getDeleteValuesSQL(nodes);
			ErrorRaiser.throwIfFalse(!subQuery.isEmpty(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			query = String.format(DELETE_DISCRET_FILEENTRY_PRODUCTS, subQuery, fileEntryId);
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
		//Add ArticleProducts
		xpath = SAXReaderUtil.createXPath("/param/product[@action='add']");
		nodes = xpath.selectNodes(root);
		ErrorRaiser.throwIfFalse((nodes != null), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		if(nodes.size() > 0)
		{
			subQuery = getInsertValuesSQL(nodes, fileEntryId);
			ErrorRaiser.throwIfFalse(!subQuery.isEmpty(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			query = String.format(ADD_FILEENTRY_PRODUCTS, subQuery, fileEntryId);
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
		//Live DLFileEntry
		DLFileEntry dlfe = DLFileEntryLocalServiceUtil.getDLFileEntry(Long.parseLong(fileEntryId));
		ErrorRaiser.throwIfNull(dlfe, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		dLFileEntryXmlIO.createLiveEntry(dlfe);
	}
	
	public void setProductsOfFileEntry2(String xml) throws ServiceError, SystemException, NumberFormatException, PortalException, IOException, SQLException
	{
		setBinaryProducts(xml, true);
	}
	
	public void updateProductsOfFileEntry2(String xml) throws ServiceError, SystemException, NumberFormatException, PortalException, IOException, SQLException
	{
		setBinaryProducts(xml, false);
	}
	
	private void setBinaryProducts(String xml, boolean replace) throws ServiceError, SystemException, NumberFormatException, PortalException, IOException, SQLException
	{
		// Valida la entrada.
		ErrorRaiser.throwIfNull(xml, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		try
		{
			// Procesa el XML.
			Document docProducts = SAXReaderUtil.read(xml);
			
			String target = XMLHelper.getStringValueOf(docProducts, "/param/target/@id");
			// Si el ID del binario es numérico, es un DLFileEntry
			if (Validator.isNumber(target))
			{
				if (replace)
					setProductsOfFileEntry(xml);
				else
					updateProductsOfFileEntry(xml);
			}
			// Si no es numérico, es un binario
			else
			{
				// Recupera el artículo al que pertenece el binario.
				String articleId = XMLHelper.getStringValueOf(docProducts, "/param/article/@id");
				ErrorRaiser.throwIfNull(articleId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
				
				// Actualiza los productos.
				BinaryRepositoryLocalServiceUtil.setBinaryProducts(xml, replace);
				
				// Pone a update pending el artículo.
				LiveLocalServiceUtil.setLiveArticleToPending(articleId);
			}
		}
		catch (DocumentException e)
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Malformed products XML", e.getStackTrace());
		}
	}

	public String getProductNameById(String productId) throws IOException, SQLException, DocumentException, ServiceError
	{
		String name = null;
		
		String query = String.format(GET_PRODUCTNAME_BY_ID, productId);
		
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList(query);
		ErrorRaiser.throwIfFalse((result != null && result.size() > 0 && result.get(0) != null), IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
		
		name = result.get(0).toString();
		
		return name;
	}
	
	public boolean existsProduct(String productId) throws IOException, SQLException, DocumentException, ServiceError
	{
		boolean exists = false;

		if(Validator.isNotNull(getProductNameById(productId)))
			exists = true;
		
		return exists;
	}
	
	public String getInsertValuesSQL(List<Node> nodes, String articleId) throws ServiceError
	{
		StringBuffer insertValuesSQL = new StringBuffer();
		for (int i = 0; i < nodes.size(); i++) 
		{
			Element element = (Element)nodes.get(i);
			
			Attribute attributeId = element.attribute("id");
			ErrorRaiser.throwIfNull(attributeId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			String productId = attributeId.getValue();
			ErrorRaiser.throwIfNull(productId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
			insertValuesSQL.append("('"+ productId + "','" + articleId + "')");
			
			if(i < nodes.size() - 1)
				insertValuesSQL.append(",");
			else
				insertValuesSQL.append(";");
		}
		return insertValuesSQL.toString();
	}
	
	public String getDeleteValuesSQL(List<Node> nodes) throws ServiceError
	{
		StringBuffer insertValuesSQL = new StringBuffer();
		
		if(nodes.size() > 0)
			insertValuesSQL.append(" productId IN ");
		
		for (int i = 0; i < nodes.size(); i++) 
		{
			Element element = (Element)nodes.get(i);
			
			Attribute attributeId = element.attribute("id");
			ErrorRaiser.throwIfNull(attributeId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			String productId = attributeId.getValue();
			ErrorRaiser.throwIfNull(productId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

			if(i == 0)
			{
				insertValuesSQL.append("('" + productId + "'");
			}				
			if(i == nodes.size() - 1)
			{
				if(nodes.size() > 1)
				{
					insertValuesSQL.append(", '" + productId + "') ");
				}
				else
				{
					insertValuesSQL.append(") ");
				}
			}
			if (i > 0 && i < nodes.size() - 1)
				insertValuesSQL.append(", '" + productId + "'");
		}
		return insertValuesSQL.toString();
	}
	
	
	    ////////////////////////////
	   //						 //				
	  // 	PAYWALL PRODUCTS    //
	 //					       //
	////////////////////////////
	
	public Document getPaywallProduct(long id) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse(id > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document dataRoot = PortalLocalServiceUtil.executeQueryAsDom(
				String.format(GET_PAYWALL_PRODUCT_BY_ID, id), new String[]{"pname", "pdescription"});
		
		List<Node> nodes = SAXReaderUtil.createXPath("/rs/row").selectNodes(dataRoot);
		for(Node node:nodes)
		{
			Node products = PortalLocalServiceUtil.executeQueryAsDom(
					String.format(GET_PAYWALL_PRODUCTS_RELATEDS, XMLHelper.getTextValueOf(node, "@id")), 
					true, "products", "product").getRootElement().detach();
			
			Element element = (Element)node;
			element.add(products);
		}
		
		return dataRoot;
	}
	
	public String getPaywallProducts(String groupid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document dataRoot = PortalLocalServiceUtil.executeQueryAsDom(
				String.format(GET_PAYWALL_PRODUCTS, groupid), new String[]{"pname", "pdescription"});
		
		List<Node> nodes = SAXReaderUtil.createXPath("/rs/row").selectNodes(dataRoot);
		for(Node node:nodes)
		{
			Node products = PortalLocalServiceUtil.executeQueryAsDom(
					String.format(GET_PAYWALL_PRODUCTS_RELATEDS, XMLHelper.getTextValueOf(node, "@id")), 
					true, "products", "product").getRootElement().detach();
			
			Element element = (Element)node;
			element.add(products);
		}
		
		return dataRoot.asXML();
	}
	
	private Document getPaywallProduct(String pname, String groupid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_PAYWALL_PRODUCT_BY_NAME_GROUPID, groupid, pname), 
				new String[]{"pname", "pdescription"});
	}

	public Document getPaywallProductByUrlName(String groupid, String urlname) throws ServiceError, SecurityException, NoSuchMethodException
	{
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_PAYWALL_PRODUCT_BY_URLNAME_GROUPID, groupid, urlname), 
				new String[]{"pname", "pdescription"});
	}
	
	private Document getPaywallProductVersion(String groupid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_PAYWALL_PRODUCT_VERSION, groupid));
	}
	
	private Document getLayoutSetId(String groupid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_LAYOUTSET_BY_GROUPID, groupid));
	}

	private void checkAndUpdateVersion(String groupid) throws ServiceError, SecurityException, NoSuchMethodException, IOException, SQLException
	{
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		//Actualizo o creo la versión
		Node versionNode = SAXReaderUtil.createXPath("/rs/row").selectSingleNode(getPaywallProductVersion(groupid));
		if(versionNode == null)
		{
			String layoutsetid = XMLHelper.getTextValueOf(getLayoutSetId(groupid), "/rs/row/@layoutSetId");
			PortalLocalServiceUtil.executeUpdateQuery(String.format(ADD_PAYWALL_PRODUCT_VERSION, groupid, layoutsetid));
		}
		else
		{
			PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_PAYWALL_PRODUCT_VERSION, groupid));
		}
	}
	
	private String createPaywallProductRelations(Element dataRoot, String pname, String groupid, String id) 
			throws ServiceError, SecurityException, NoSuchMethodException, DocumentException, IOException, SQLException
	{
		Document product = getPaywallProduct(pname, groupid);
		id = XMLHelper.getTextValueOf(product.getRootElement(), "/rs/row/@id");
		
		StringBuffer query = new StringBuffer();
		
		List<Node> nodes = SAXReaderUtil.createXPath("/rs/row/products/product/@productid").selectNodes(dataRoot);
		ErrorRaiser.throwIfFalse(nodes.size() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		for(int i = 0; i < nodes.size(); i++)
		{
			query.append(String.format(ADD_PAYWALL_PRODUCT_RELATED_VALUES, id, nodes.get(i).getStringValue()));
			if(i == (nodes.size() - 1))
				query.append(StringPool.SEMICOLON);
			else
				query.append(StringPool.COMMA);
		}
		
		if(query.length() > 0)
		{
			query.insert(0, ADD_PAYWALL_PRODUCT_RELATED);
			PortalLocalServiceUtil.executeUpdateQuery(query.toString());
		}

		((Element) product.selectSingleNode("/rs/row")).add(dataRoot.selectSingleNode("/rs/row/products").detach());
		
		return product.asXML();
	}

	//	<?xml version="1.0"?>
	//	<rs>
	//	    <row groupid="10810" pnamebase64="eG1sIGHDsWFkaXIgcHJvZHVjdA==" productid="3e85bd57-a1bc-11e2-a056-454381bc2da9" id="40" ptype="2" pquan="3" pplazo="3d" pnoreg="1" maxcon="0" name="Premium" onlyvalarts="0">
	//	        <pname><![CDATA[xml añadir product]]></pname>
	//	        <pdescription><![CDATA[xml añadir product]]></pdescription>
	//	        <products>
	//				<product productid="54774d62-9615-11e2-b754-9d3a49c1ec5a" />
	//				<product productid="bc4df1b3-c3a1-11e2-aabd-8ef2148973a6" />
	//	        </products>
	//	    </row>
	//	</rs> 

	public String addPaywallProduct(String xmlData) 
			throws DocumentException, ServiceError, SecurityException, NoSuchMethodException, IOException, SQLException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		String groupid = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@groupid");
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		checkAndUpdateVersion(groupid);
		
		String pname = XMLHelper.getTextValueOf(dataRoot, "/rs/row/pname");
		ErrorRaiser.throwIfNull(pname, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String pnamebase64 = Base64.encode(pname.getBytes(Digester.ENCODING));

		pname = StringEscapeUtils.escapeSql(pname);
		
		String pdescription = XMLHelper.getTextValueOf(dataRoot, "/rs/row/pdescription");
		if(Validator.isNotNull(pdescription))
		{
			pdescription = new StringBuilder(StringPool.APOSTROPHE).append(
					StringEscapeUtils.escapeSql(pdescription)).append(StringPool.APOSTROPHE).toString();
		}
		
		String ptype = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@ptype");
		ErrorRaiser.throwIfNull(ptype, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String pquan = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@pquan");
		ErrorRaiser.throwIfNull(pquan, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String pplazo = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@pplazo");
		if(Validator.isNotNull(pplazo))
			pplazo = new StringBuilder(StringPool.APOSTROPHE).append(pplazo).append(StringPool.APOSTROPHE).toString();
		
		String pnoreg = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@pnoreg");
		ErrorRaiser.throwIfNull(pnoreg, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String maxcon = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@maxcon");
		ErrorRaiser.throwIfNull(maxcon, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		boolean onlyvalarts = GetterUtil.getBoolean(XMLHelper.getTextValueOf(dataRoot, "/rs/row/@onlyvalarts"), false);
		
		// Propiedades que pueden ser nulas
		String urlname = CategoriesUtil.normalizeText(pname);
		String price = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@price");
		String currencyA = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@currencyAlpha");
		String currencyN = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@currencyNumeric");
		String validity = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@validity");
		String externalid = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@externalid");

		PortalLocalServiceUtil.executeUpdateQuery(
			String.format(ADD_PAYWALL_PRODUCT,
				groupid, pname, pnamebase64, pdescription,ptype, pquan, pplazo, pnoreg, maxcon, onlyvalarts,
				urlname,
				Validator.isNull(price)      ? StringPool.NULL : price,
				Validator.isNull(currencyA)  ? StringPool.NULL : StringUtil.apostrophe(currencyA),
				Validator.isNull(currencyN)  ? StringPool.NULL : StringUtil.apostrophe(currencyN),
				Validator.isNull(validity)   ? StringPool.NULL : StringUtil.apostrophe(validity),
				Validator.isNull(externalid) ? StringPool.NULL : StringUtil.apostrophe(externalid)
			)
		);
		
		return createPaywallProductRelations(dataRoot, pname, groupid, null);
	}

	public String updatePaywallProduct(String xmlData) 
			throws DocumentException, ServiceError, SecurityException, NoSuchMethodException, IOException, SQLException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		String id = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@id");
		ErrorRaiser.throwIfNull(id, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		String groupid = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@groupid");
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		checkAndUpdateVersion(groupid);

		String pname = XMLHelper.getTextValueOf(dataRoot, "/rs/row/pname");
		ErrorRaiser.throwIfNull(pname, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String pnamebase64 = Base64.encode(pname.getBytes(Digester.ENCODING));

		pname = StringEscapeUtils.escapeSql(pname);

		String pdescription = XMLHelper.getTextValueOf(dataRoot, "/rs/row/pdescription");
		if(Validator.isNotNull(pdescription))
		{
			pdescription = new StringBuilder(StringPool.APOSTROPHE).append(
					StringEscapeUtils.escapeSql(pdescription)).append(StringPool.APOSTROPHE).toString();
		}
		
		String ptype = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@ptype");
		ErrorRaiser.throwIfNull(ptype, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String pquan = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@pquan");
		ErrorRaiser.throwIfNull(pquan, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String pplazo = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@pplazo");
		if(Validator.isNotNull(pplazo))
			pplazo = new StringBuilder(StringPool.APOSTROPHE).append(pplazo).append(StringPool.APOSTROPHE).toString();
		
		String pnoreg = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@pnoreg");
		ErrorRaiser.throwIfNull(pnoreg, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String maxcon = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@maxcon");
		ErrorRaiser.throwIfNull(maxcon, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		boolean onlyvalarts = GetterUtil.getBoolean(XMLHelper.getTextValueOf(dataRoot, "/rs/row/@onlyvalarts"), false);
		
		// Propiedades que pueden ser nulas
		String urlname = CategoriesUtil.normalizeText(pname);
		String price = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@price");
		String currencyA = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@currencyAlpha");
		String currencyN = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@currencyNumeric");
		String validity = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@validity");
		String externalid = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@externalid");
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_PAYWALL_PRODUCTS_RELATED_ID, id));
		PortalLocalServiceUtil.executeUpdateQuery(
			String.format(UPDATE_PAYWALL_PRODUCT,
				pname, pnamebase64, pdescription, ptype, pquan, pplazo, pnoreg, maxcon, onlyvalarts,
				urlname,
				Validator.isNull(price)      ? StringPool.NULL : price,
				Validator.isNull(currencyA)  ? StringPool.NULL : StringUtil.apostrophe(currencyA),
				Validator.isNull(currencyN)  ? StringPool.NULL : StringUtil.apostrophe(currencyN),
				Validator.isNull(validity)   ? StringPool.NULL : StringUtil.apostrophe(validity),
				Validator.isNull(externalid) ? StringPool.NULL : StringUtil.apostrophe(externalid),
				id
			)
		);
	
		return createPaywallProductRelations(dataRoot, pname, groupid, id);
	}
	
	public String deletePaywallProduct(String xmlData) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String groupid = XMLHelper.getTextValueOf(dataRoot, "/rs/@groupid");
		ErrorRaiser.throwIfNull(groupid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		checkAndUpdateVersion(groupid);
		
		List<Node> nodes = SAXReaderUtil.createXPath("/rs/row/@id").selectNodes(dataRoot);
		ErrorRaiser.throwIfFalse((nodes != null && nodes.size() > 0), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_PAYWALL_PRODUCTS, TeaserMgr.getInClauseSQL(nodes)));
		
		return xmlData;
	}
	
	//	<?xml version="1.0"?>
	//  <products>
	//    	<product productid="3e85bd57-a1bc-11e2-a056-454381bc2da9"></product>
	//		<product productid="4e85bd57-a1bc-11e2-a056-454381bc2da9"></product>
	//  </products>

	public String getSelectedProducts(long groupId, String productsXML) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException
	{
		if(Validator.isNull(productsXML))
		{
			productsXML = StringPool.OPEN_PARENTHESIS + StringPool.APOSTROPHE + 
						  StringPool.APOSTROPHE + StringPool.CLOSE_PARENTHESIS;
		}
		else
		{
			List<Node> nodes =  SAXReaderUtil.createXPath("/products/product/@productid").selectNodes(SAXReaderUtil.read(productsXML));
			productsXML = TeaserMgr.getInClauseSQL(nodes);
		}
		
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SELECTED_PRODUCTS, productsXML, groupId)).asXML();
	}
	
	public String getPaywallProductsAccessByType(String userid, int type, String start, String quantity, String xmlFilters) throws ServiceError, SecurityException, NoSuchMethodException, DocumentException
	{
		ErrorRaiser.throwIfNull(userid, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(type, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		int finalStart = 0;
		int finalQuantity = 0;

		try
		{
			if(Validator.isNotNull(start))
				finalStart = Integer.parseInt(start);
			
			if(Validator.isNotNull(quantity))
				finalQuantity = Integer.parseInt(quantity);
		}
		catch(Exception e)
		{
			_log.trace(e);
		}

		Element result = SAXReaderUtil.read("<paywall/>").getRootElement();
		String orderSQL = String.format(PaywallUtil.ORDER_CLAUSE, "i.fecha", "DESC");
		String filtersSQL = StringPool.BLANK;
		
		if(Validator.isNotNull(xmlFilters))
		{
			Document docFilters = SAXReaderUtil.read(xmlFilters);

			XPath xpath = SAXReaderUtil.createXPath("/rs/filters");
			Element filters = (Element)xpath.selectSingleNode(docFilters).detach();
			if(filters != null)
			{
				filtersSQL = SQLQueries.buildFilters(filters.asXML());
				result.add(filters);
			}
			
			String columnid = XMLHelper.getTextValueOf(docFilters, "/rs/order/@columnid");
			String direction = GetterUtil.getBoolean(XMLHelper.getTextValueOf(docFilters, "/rs/order/@asc"), false) ? "ASC" : "DESC";
			
			if(Validator.isNotNull(columnid))
			{
				String alias = "i.";
				if(columnid.equalsIgnoreCase("pname"))
					alias = "ipp.";
				else if(columnid.equalsIgnoreCase("urlTitle"))
					alias  = "j.";
					
				orderSQL = String.format(PaywallUtil.ORDER_CLAUSE, alias + columnid, direction);
			}
			
			xpath = SAXReaderUtil.createXPath("/rs/order");
			Node order = (Element)xpath.selectSingleNode(docFilters).detach();
			if(order != null)
				result.add(order);
		}
		
		String limitSQL = new StringBuilder("LIMIT ").append(finalStart).append(StringPool.COMMA).append(finalQuantity).toString();

		String query = String.format(PaywallUtil.GET_SESSIONS_BY_USER, userid, filtersSQL, orderSQL, limitSQL);
		if(type == PaywallUtil.TYPE1)
			query = String.format(PaywallUtil.GET_PAYWALL_PRODUCTS_ACCESS_TYPE1_BY_USER, userid, filtersSQL, orderSQL, limitSQL);
		else if(type == PaywallUtil.TYPE2)
			query = String.format(PaywallUtil.GET_PAYWALL_PRODUCTS_ACCESS_TYPE2_BY_USER, userid, filtersSQL, orderSQL, limitSQL);	

		result.add((Element)PortalLocalServiceUtil.executeQueryAsDom(query, new String[]{"pname"}).getRootElement().detach());
		
		return result.asXML();
	}
	
	public void initPaywallDeleteSessionsTask() throws SecurityException, NoSuchMethodException, ServiceError, IOException, SQLException
	{
		if(IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_LIVE))
		{
			DeleteSessionsTask task = null;
			List<Object> uuidResult = PortalLocalServiceUtil.executeQueryAsList(GET_UUID);
			ErrorRaiser.throwIfFalse((uuidResult != null && uuidResult.size() > 0 && uuidResult.get(0) != null), IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
	
			synchronized(deleteSessions)
			{
				task = new DeleteSessionsTask(uuidResult.get(0).toString(), 0, deleteSessions);
			}
			
			if(task != null)
				task.start();
		}
	}
	
	public void deletePaywallSessions() throws SecurityException, NoSuchMethodException, ServiceError, IOException, SQLException
	{
		Calendar calendar = Calendar.getInstance();
		
		int thresholdDays = GetterUtil.getInteger(
				PortalUtil.getPortalProperties().getProperty(
						IterKeys.PORTAL_PROPERTIES_KEY_ITER_PAYWALL_DATACLEANUP_THRESHOLD), DEFAULT_DELETE_THRESHOLD_DAYS);
		
		calendar.add(Calendar.DAY_OF_MONTH, -thresholdDays);
		
		String date = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss).format(calendar.getTime());

		_log.trace("Deleting iterpaywall_session, iterpaywall_tipo1, iterpaywall_tipo2 older than " + date);
		
		String query = String.format(DELETE_PAYWALL_EXPIRES, "iterpaywall_session", date);
		PortalLocalServiceUtil.executeUpdateQuery(query);
		
		query = String.format(DELETE_PAYWALL_EXPIRES, "iterpaywall_tipo1", 	date);
		PortalLocalServiceUtil.executeUpdateQuery(query);
		
		query = String.format(DELETE_PAYWALL_EXPIRES, "iterpaywall_tipo2", 	date);
		PortalLocalServiceUtil.executeUpdateQuery(query);
	}
	
	public String setPaywallStatusMsgs(String xmlData) throws DocumentException, ServiceError, IOException, SQLException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		return setPaywallStatusMsgs(dataRoot, true);
	}
	
	private String setPaywallStatusMsgs(Element dataRoot, boolean throwErrorIfEmpty) throws DocumentException, ServiceError, IOException, SQLException
	{
		String groupId = XMLHelper.getTextValueOf(dataRoot, "/rs/row/@groupId");
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		try
		{
			String maxconnexceedmsg = XMLHelper.getTextValueOf(dataRoot, "/rs/row/maxconnexceedmsg");
			ErrorRaiser.throwIfNull(maxconnexceedmsg, IterErrorKeys.XYZ_E_EMPTY_CONFIG_ZYX);
			maxconnexceedmsg = StringEscapeUtils.escapeSql(maxconnexceedmsg);
			
			String maxdaysexceedmsg = XMLHelper.getTextValueOf(dataRoot, "/rs/row/maxdaysexceedmsg");
			ErrorRaiser.throwIfNull(maxdaysexceedmsg, IterErrorKeys.XYZ_E_EMPTY_CONFIG_ZYX);
			maxdaysexceedmsg = StringEscapeUtils.escapeSql(maxdaysexceedmsg);
			
			String maxarticlesexceedmsg = XMLHelper.getTextValueOf(dataRoot, "/rs/row/maxarticlesexceedmsg");
			ErrorRaiser.throwIfNull(maxarticlesexceedmsg, IterErrorKeys.XYZ_E_EMPTY_CONFIG_ZYX);
			maxarticlesexceedmsg = StringEscapeUtils.escapeSql(maxarticlesexceedmsg);
			
			String paywalloutofservice = XMLHelper.getTextValueOf(dataRoot, "/rs/row/paywalloutofservice");
			ErrorRaiser.throwIfNull(paywalloutofservice, IterErrorKeys.XYZ_E_EMPTY_CONFIG_ZYX);
			paywalloutofservice = StringEscapeUtils.escapeSql(paywalloutofservice);
			
			// Comprobamos que existe el registro en la tabla group_config
			boolean groupConfigExists = true;
			try
			{
				// Llamada dinámica para evitar referencias cruzadas									
				Class<?> comObject = Class.forName("com.protecmedia.iter.user.service.UserOperationsLocalServiceUtil");									
	
				Method method = comObject.getMethod("checkConfig", java.lang.String.class);														
				ErrorRaiser.throwIfNull(method, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, "Mehod UserOperationsLocalServiceUtil.checkConfig not found");
				
				// Parametros del metodo
				Object[] methodParams = new Object[1];
				methodParams[0] = groupId;					
				
				// Devuelve una excepcion si no existe el registro en base de datos
				method.invoke(comObject, methodParams);					
			}
			catch(Exception e)
			{
				groupConfigExists = false; 
			}		
					
			if (groupConfigExists)
			{
				PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_PAYWALL_MSGS, 
					maxconnexceedmsg, maxdaysexceedmsg, maxarticlesexceedmsg, paywalloutofservice, groupId));
			}
			else
			{
				StringBuffer values = new StringBuffer()
					.append(groupId                    + ", " )
					.append("sysdate()"                + ", " )
					.append("'" + maxconnexceedmsg     + "', ")
					.append("'" + maxdaysexceedmsg     + "', ")
					.append("'" + maxarticlesexceedmsg + "', ")
					.append("'" + paywalloutofservice  + "'"  );							
								
				PortalLocalServiceUtil.executeUpdateQuery(String.format(INSERT_PAYWALL_MSGS, values)); 
			}
		}
		catch (ServiceError se)
		{
			// Si se indica que se quiere tratar la configuración vacía como Error, se lanza la excepción
			if (throwErrorIfEmpty && se.getErrorCode().equals(IterErrorKeys.XYZ_E_EMPTY_CONFIG_ZYX))
			{
				se.setErrorCode(IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
				throw se;
			}
		}
		return dataRoot.getDocument().asXML();
	}
	
	public String getPaywallStatusMsgs(String groupid) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_PAYWALL_MSGS, groupid), 
				new String[]{"maxconnexceedmsg", "maxdaysexceedmsg", "maxarticlesexceedmsg", "paywalloutofservice"}).asXML();
	}
	
	public String updatePaywall(String groupid, String activate) throws IOException, DocumentException, ServiceError, NumberFormatException, PortalException, SystemException
	{
		String paywallParam = SERVER_STATUS_ACTIVATE_PARAM;
		boolean activatePaywall = GetterUtil.getBoolean(activate, false);
		if(!activatePaywall)
			paywallParam = SERVER_STATUS_DEACTIVATE_PARAM;

		getApacheData(SERVER_STATUS_URL + paywallParam, METHOD_HEAD, groupid);
		
		return getPaywallMode(groupid);
	}
	
	public String getPaywallMode(String groupid) throws IOException, DocumentException, ServiceError, NumberFormatException, PortalException, SystemException
	{
		return getApacheData(SERVER_STATUS_URL + SERVER_GET_PAYWALL_MODE, METHOD_GET, groupid);
	}
	
	private String getApacheData(String url, String method, String groupid) throws DocumentException, ServiceError, IOException, NumberFormatException, PortalException, SystemException
	{
		String[] liveServers = ApacheUtil.getAllURLs();
		ErrorRaiser.throwIfFalse(Validator.isNotNull(liveServers), IterErrorKeys.XYZ_ITR_E_PAYWALL_UPDATE_STATUS_EMPTY_APACHES_ZYX);
		
		ApacheHierarchy apacheHierarchy = new ApacheHierarchy();
		String[] masterList = apacheHierarchy.getMasterList();
		String[] slaveList = apacheHierarchy.getSlaveList();
		
		Element root = SAXReaderUtil.read("<rs/>").getRootElement();
		
		String[] allServers = (String[])ArrayUtils.addAll(masterList, slaveList);
		if(allServers != null && allServers.length > 0)
		{
			String virtualhost = LayoutSetLocalServiceUtil.getLayoutSet(Long.parseLong(groupid), false).getVirtualHost();
			ErrorRaiser.throwIfFalse(Validator.isNotNull(virtualhost), IterErrorKeys.XYZ_ITR_E_PAYWALL_EMPTY_VIRTUAL_HOST_ZYX);
			
			for(int i = 0; i < allServers.length; i++)
			{
				String server = allServers[i];
				String[] result = changeStatusByURL(server + url, method, virtualhost);
				
				Element element = root.addElement("row");
				element.addAttribute("ip", server);
				element.addAttribute("result", 	result[0]);
				element.addAttribute("paywall", result[1]);
			}
		}
		
		if(root.asXML().equalsIgnoreCase("<rs/>"))
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_ITR_E_PAYWALL_IMPOSSIBLE_TO_CONNECT_TO_APACHES_ZYX);
		
		return root.asXML();
	}
	
	private String[] changeStatusByURL(String url, String method, String virtualhost)
	{
		String[] result = new String[]{StringPool.BLANK, StringPool.BLANK};

		try
		{
			HttpURLConnection httpConnection = (HttpURLConnection)(new URL(url).openConnection());
			httpConnection.setConnectTimeout(ApacheUtil.getApacheConnTimeout());
			httpConnection.setReadTimeout(ApacheUtil.getApacheReadTimeout());
			
			// ITER-426. Se añade el header "Host" con el nombre del sitio 
			httpConnection.setRequestProperty (WebKeys.HOST, virtualhost);
			
			// Añade el User Agent *ITERWEBCMS*
			httpConnection.setRequestProperty (HttpHeaders.USER_AGENT, WebKeys.USER_AGENT_ITERWEBCMS);
			
			httpConnection.setRequestMethod(method);
			httpConnection.connect();
			
        	// ITER-720 Reutilización de las conexiones HTTP contra los servidores que soportan el header "Connection: keep-alive"	
        	// Hay que consumir los errores para poder reutilizar las conexiones
        	HttpUtil.throwIfConnectionFailed( httpConnection, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX );
        	
        	String response = StreamUtil.toString(httpConnection.getInputStream(), StringPool.UTF8);
			
			result[0] = new StringBuffer(	String.valueOf(httpConnection.getResponseCode())	).append(
											StringPool.SPACE									).append(
											StringPool.DASH										).append(
											StringPool.SPACE									).append(
											httpConnection.getResponseMessage()					).toString();
			
			if (Validator.isNotNull(response))
				result[1] = response;
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.debug(e);
			
			result[0] = "500 - " + e.toString();
		}
		
		return result;
	}
	
	public Document exportData(String params) throws ServiceError, SecurityException, DocumentException, NoSuchMethodException, IOException, SQLException, PortalException, SystemException
	{
		Element root 	= SAXReaderUtil.read(params).getRootElement();
		String groupName= XMLHelper.getStringValueOf(root, "@groupName");
		ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		long groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		String sGroupId = String.valueOf(groupId);

		// Recupera los datos a exportar.
		Document dom = SAXReaderUtil.read(getPaywallProducts(sGroupId));

		// Recupera e Inserta los mensajes de configuración.
		Document domMsg = SAXReaderUtil.read(getPaywallStatusMsgs(sGroupId));
		dom.getRootElement().addAttribute("maxconnexceedmsg",     XMLHelper.getTextValueOf(domMsg, "/rs/row/maxconnexceedmsg"));
		dom.getRootElement().addAttribute("maxdaysexceedmsg",     XMLHelper.getTextValueOf(domMsg, "/rs/row/maxdaysexceedmsg"));
		dom.getRootElement().addAttribute("maxarticlesexceedmsg", XMLHelper.getTextValueOf(domMsg, "/rs/row/maxarticlesexceedmsg"));
		dom.getRootElement().addAttribute("paywalloutofservice",  XMLHelper.getTextValueOf(domMsg, "/rs/row/paywalloutofservice"));

		// Elimina los groupId y productId para ahorrar espacio.
		List<Node> nodes = SAXReaderUtil.createXPath("/rs/row").selectNodes(dom);
		for (Node n : nodes)
		{
			n.selectSingleNode("@groupid").detach();

			List<Node> productNodes = n.selectNodes("products/product/@productid");
			for (Node p : productNodes)
			{
				p.detach();
			}
		}
		
		return dom;
	}
	
	public void importData(String data) throws DocumentException, ServiceError, PortalException, SystemException, IOException, SQLException, SecurityException, NoSuchMethodException
	{
        Document dom = SAXReaderUtil.read(data);
        // Busca el groupIid mediante el groupName
        String groupName = XMLHelper.getStringValueOf(dom, "/rs/@groupName");
        ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        long groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
        
        // Obtiene la configuración de los mensajes.
        Document domMsg = SAXReaderUtil.createDocument();
        Element msgRow = domMsg.addElement("rs").addElement("row");
        msgRow.addAttribute("groupId", String.valueOf(groupId));
        msgRow.addElement("maxconnexceedmsg")    .addCDATA(XMLHelper.getTextValueOf(dom, "/rs/@maxconnexceedmsg"));
        msgRow.addElement("maxdaysexceedmsg")    .addCDATA(XMLHelper.getTextValueOf(dom, "/rs/@maxdaysexceedmsg"));
        msgRow.addElement("maxarticlesexceedmsg").addCDATA(XMLHelper.getTextValueOf(dom, "/rs/@maxarticlesexceedmsg"));
        msgRow.addElement("paywalloutofservice") .addCDATA(XMLHelper.getTextValueOf(dom, "/rs/@paywalloutofservice"));
        
        // Importa los mensajes.
        setPaywallStatusMsgs(domMsg.getRootElement(), false);
        
        // Importa los productos.
        boolean updtIfExist = GetterUtil.getBoolean(XMLHelper.getStringValueOf(dom.getRootElement(), "@updtIfExist", "true"));
        List<Node> nodes = SAXReaderUtil.createXPath("/rs/row").selectNodes(dom);
        for (Node n : nodes)
		{
        	n.detach();
			Document domProduct = SAXReaderUtil.createDocument();
			domProduct.addElement("rs").add(n);
        	
			// Establece el groupId recuperado, ya que durante la publicación en el LIVE puede ser distinto.
			((Element) n).addAttribute("groupid", String.valueOf(groupId));
			
			// Comprueba si existe los productos. Si alguno no existe inserta un ID = -1.
			List<Node> productNodes = SAXReaderUtil.createXPath("products/product").selectNodes(n);
			for (Node p : productNodes)
			{
				String name = ((Element) p).attributeValue("name");
				((Element) p).addAttribute("productid", getProductIdByName(groupId, name));
			}
			
			// Importa el producto.
			String pname = XMLHelper.getTextValueOf(n, "pname");
	        int exist = existPaywallProduct(pname, groupId);
			ErrorRaiser.throwIfFalse( updtIfExist || exist == 0, IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, String.format("%s(%s)", "iterpaywall_product", pname));
			if (exist > 0)
			{
				domProduct.getRootElement().element("row").attribute("id").setText(String.valueOf(exist));
				updatePaywallProduct(domProduct.asXML());	
			}
			else
			{
				addPaywallProduct(domProduct.asXML());
			}
		}
	}
	
	private int existPaywallProduct(String pname, long groupId) throws ServiceError, SecurityException, NoSuchMethodException, NumberFormatException, DocumentException
	{
		int id = 0;
		Document dom = getPaywallProduct(pname, String.valueOf(groupId));
		
		if (dom.getRootElement().content().size() > 0)
		{
			id = dom.getRootElement().numberValueOf("//row/@id").intValue();
		}
		return id;
	}
	
	private String getProductIdByName(long groupId, String name) throws IOException, SQLException, DocumentException, ServiceError
	{
		String productId = null;
		
		String query = String.format(GET_PRODUCTID_BY_NAME, groupId, name);
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList(query);
		
		if (result != null && result.size() > 0 && result.get(0) != null)
		{
			productId = result.get(0).toString();
		}
		else
		{
			productId = "-1";
		}
		
		return productId;
	}
	
	public void updateGroupIds() throws IOException, SQLException, ServiceError
	{
		try
		{
			PortalLocalServiceUtil.executeUpdateQuery( "CALL ITR_ADD_GROUPID_TO_PRODUCT" );
		}
		catch (ORMException orme)
		{
			_log.debug(orme);
			
			String sqlErrorCode = ServiceErrorUtil.getErrorCode(orme);
			throw ErrorRaiser.buildError(sqlErrorCode, orme.toString(), orme.getStackTrace());
		}
	}
	
	private static final String SQL_ASIGN_ARTICLE = "UPDATE iterpaywall_product SET articleId=%s WHERE id=%s";
	
	public void asignArticle(String id, String articleId) throws IOException, SQLException
	{
		// Si no se informa articleId, se considera una desasignación.
		articleId = Validator.isNull(articleId) ? StringPool.NULL : StringUtil.apostrophe(articleId);
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(SQL_ASIGN_ARTICLE, articleId, id));
	}
}