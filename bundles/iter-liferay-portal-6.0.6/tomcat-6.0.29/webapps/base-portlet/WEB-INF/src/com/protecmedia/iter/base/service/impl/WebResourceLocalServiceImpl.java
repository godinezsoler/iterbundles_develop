package com.protecmedia.iter.base.service.impl;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.net.util.Base64;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.EncryptUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.PortletConstants;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.base.WebResourceLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.service.util.WebResourceUtil;

@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class WebResourceLocalServiceImpl extends WebResourceLocalServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(WebResourceLocalServiceImpl.class);
	
	private static int STORED_VERSIONS = 2;
	
	private final static String SELECT_MD5_WEBRESOURCE 		= "SELECT CONVERT(rsrccontent using utf8) as rsrccontent, rsrctype FROM webresource WHERE rsrcmd5 = '%s'";
	
	private final static String SELECTFORUPDATE_ITERVERSION = "SELECT * FROM iterversion_webresource WHERE iterversion = '%s' FOR UPDATE";

	private final static String SELECT_ORDERED_ITERVERSION 	= "SELECT DISTINCT(iterversion) FROM iterversion_webresource ORDER BY INET_ATON(iterversion) DESC";
	
	private final static String SELECT_ID_MD5_WEBRESOURCE 	= "SELECT rsrcid FROM webresource WHERE rsrcmd5 = '%s'";
	
	private final static String INSERT_WEBRESOURCE 			= "INSERT INTO webresource (rsrcid, rsrcmd5, rsrccontent, rsrctype, intent, createdate) values ('%s','%s','%s','%s','%s', NOW())";
	
	private final static String UPDATE_ITERVERSION 			= "UPDATE iterversion_webresource SET rsrcid = '%s' WHERE iterversion = '%s' AND kind = '%s' ";
	
	private final static String GET_NAMEPORTLETS 			= "SELECT portletId FROM portlet WHERE portletId like '%"+PortletConstants.WAR_SEPARATOR+"%'";
	
	private final static String DELETE_ALL_ITERVERSION		= "DELETE FROM iterversion_webresource";
	
	private final static String DELETE_ITERVERSION			= "%s WHERE iterversion = '%s'";
	
	private final static String DELETE_ALL_ITER_WEBRESOURCE	= "DELETE FROM webresource WHERE intent='ITER'";
	
	private final static String INSERT_ITERVERSION 			= new StringBuffer(	"INSERT INTO iterversion_webresource (uuid_, kind, iterversion) values "	).append(
																				"('%s','%s', '%s') ON DUPLICATE KEY UPDATE kind = VALUES(kind)"				).toString();
	
	private final static String SELECT_MD5_WEBRESRCEVERSION = new StringBuffer(	"SELECT rsrcmd5 FROM webresource INNER JOIN iterversion_webresource ON "	).append(
																				"webresource.rsrcid = iterversion_webresource.rsrcid "						).append(
																				"WHERE iterversion_webresource.iterversion = '%s' " 						).append(
																				"AND iterversion_webresource.kind = '%s'"									).toString();
	
	private final static String DELETE_OLD_WEBRESOURCE		= new StringBuffer(	"DELETE FROM webresource WHERE intent='ITER' AND webresource.rsrcid NOT IN ").append(
																				"(SELECT DISTINCT(iterversion_webresource.rsrcid) FROM "					).append(
																				"iterversion_webresource WHERE iterversion_webresource.rsrcid IS NOT NULL)"	).toString();

	
	public void deleteAllVersionsAndResources() throws IOException, SQLException
	{
		PortalLocalServiceUtil.executeUpdateQuery(DELETE_ALL_ITERVERSION);
		PortalLocalServiceUtil.executeUpdateQuery(DELETE_ALL_ITER_WEBRESOURCE);
	}
	
	public Document getWebResourceFromMD5(String rsrcmd5) throws NoSuchMethodException, SecurityException
	{
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(SELECT_MD5_WEBRESOURCE, rsrcmd5));
	}
	
	public void createEmptyWebResourceVersion(String iterVersion, String kind) throws ServiceError, IOException, SQLException
	{
		PortalLocalServiceUtil.executeUpdateQuery(String.format(INSERT_ITERVERSION, SQLQueries.getUUID(), kind, iterVersion));
	}
	
	public void generateJSWebResourcesVersion(String iterVersion, List<String> kinds, int deleteWindow) 
			throws NoSuchMethodException, SecurityException, IOException, SQLException, 
				   ServiceError, NoSuchAlgorithmException, DocumentException
	{
		Document dom = PortalLocalServiceUtil.executeQueryAsDom(String.format(SELECTFORUPDATE_ITERVERSION, iterVersion));
		String rsrcid = XMLHelper.getTextValueOf(dom, "/rs/row/@rsrcid");
		if(Validator.isNull(rsrcid))
		{
			Map<String, String> contents = WebResourceUtil.getJSWebResourceContent();
			for (String kind : kinds)
			{
				String kindWebResourceId = saveWebResource(contents.get(kind), ContentTypes.TEXT_JAVASCRIPT);
				updateWebResourceVersion(kindWebResourceId, iterVersion, kind);
			}
		}
		else
		{
			deleteOldVersionsAndResources();
		}
	}

	private static void updateWebResourceVersion(String rsrcid, String iterVersion, String kind) throws IOException, SQLException
	{
		PortalLocalServiceUtil.executeUpdateQuery(String.format(UPDATE_ITERVERSION, rsrcid, iterVersion, kind));
	}
	
	public String saveWebResource(String rsrccontent, String rsrctype) throws ServiceError, IOException, SQLException, NoSuchAlgorithmException
	{
		String rsrcid;
		String rsrcmd5 = EncryptUtil.digest(rsrccontent);
		
		List<Object> webResourcesId = PortalLocalServiceUtil.executeQueryAsList(String.format(SELECT_ID_MD5_WEBRESOURCE, rsrcmd5));
		if(webResourcesId.isEmpty())
		{
			rsrcid = SQLQueries.getUUID();
			String base64Content = Base64.encodeBase64String(rsrccontent.getBytes(StringPool.UTF8));
			
			PortalLocalServiceUtil.executeUpdateQuery(String.format(INSERT_WEBRESOURCE, 
					rsrcid, rsrcmd5, base64Content, StringEscapeUtils.escapeSql(rsrctype), IterKeys.ITER));
			
			_log.trace("New entry in iterversion_webresource ");
		}
		else
		{
			rsrcid = (String) webResourcesId.get(0);
		}
		
		return rsrcid;
	}
	
	public void deleteOldVersionsAndResources() throws IOException, SQLException
	{
		List<Object> versions = PortalLocalServiceUtil.executeQueryAsList(SELECT_ORDERED_ITERVERSION);
		if(versions.size() > STORED_VERSIONS)
		{
			for(int i = 0; i < versions.size(); i++)
			{
				if(i >= STORED_VERSIONS)
				{
					String iterversion = versions.get(i).toString();
					PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_ITERVERSION, DELETE_ALL_ITERVERSION, iterversion));
				}
			}
			
			PortalLocalServiceUtil.executeUpdateQuery(DELETE_OLD_WEBRESOURCE);
		}
	}
	
	public Document getPortletsNames() throws NoSuchMethodException, SecurityException
	{
		return PortalLocalServiceUtil.executeQueryAsDom(GET_NAMEPORTLETS);
	}
	
	public Document getMD5WebResoruceVersion(String version, String kind) throws NoSuchMethodException, SecurityException, ServiceError
	{
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(SELECT_MD5_WEBRESRCEVERSION, version, kind));
	}
}