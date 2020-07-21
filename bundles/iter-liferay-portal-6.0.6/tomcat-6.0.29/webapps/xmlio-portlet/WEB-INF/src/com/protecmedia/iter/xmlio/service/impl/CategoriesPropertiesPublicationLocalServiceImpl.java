package com.protecmedia.iter.xmlio.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.velocity.IterVelocityTools;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.ChannelLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.base.CategoriesPropertiesPublicationLocalServiceBaseImpl;
import com.protecmedia.iter.xmlio.service.util.TomcatUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;
import com.protecmedia.iter.xmlio.service.util.XmlioKeys;

@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class CategoriesPropertiesPublicationLocalServiceImpl extends CategoriesPropertiesPublicationLocalServiceBaseImpl {
	
	private static Log _log = LogFactoryUtil.getLog(CategoriesPropertiesPublicationLocalServiceImpl.class);
	
	private static final String GET_CATEGORIES_PROPERTIES_TO_EXPORT = String.format(new StringBuilder( 
		"SELECT cp.groupid,                                                 \n").append(
		"       (SELECT globalid 									        \n").append(
		"        FROM Xmlio_Live x0                                         \n").append(
		"        WHERE x0.classNameValue = '%s' 							\n").append(
		"          AND x0.localId = cp.groupid) globalId,                   \n").append(
		"																	\n").append(
		"       (SELECT globalid                                            \n").append(
		"        FROM Xmlio_Live x                                          \n").append(
		"        WHERE x.localid = cp.categoryid                            \n").append(
		"          AND x.classNameValue = '%s'                              \n").append(
		"          AND x.groupId = c.groupId                                \n").append(
		"		 LIMIT 1) categoryGlobalId,                                 \n").append(
		"																	\n").append(							    
		"       (SELECT globalid                                            \n").append(
		"		 FROM Xmlio_Live x1                                         \n").append(
		"        WHERE x1.localid = cp.aboutid                              \n").append(
		"          AND x1.groupId = c.groupId                               \n").append(
		"          AND x1.classNameValue = '%s'                             \n").append(
		"        LIMIT 1) articleGlobalId                                   \n").append(
		"FROM categoryproperties cp, AssetCategory c, AssetVocabulary v     \n").append( 
		"	WHERE c.vocabularyId = v.vocabularyId 							\n").append( 
		"  		AND c.categoryId = cp.categoryid 							\n").toString(), 
		IterKeys.CLASSNAME_GROUP, IterKeys.CLASSNAME_CATEGORY, IterKeys.CLASSNAME_JOURNALARTICLE);
	
	private static final String INSERT_CATEGORIES_PROPERTIES = new StringBuilder()
		.append("INSERT INTO categoryproperties (groupid, categoryid, aboutid) \n")
		.append("VALUES %s \n")
		.append("ON DUPLICATE KEY UPDATE groupid=VALUES(groupid), categoryid=VALUES(categoryid), aboutid=VALUES(aboutid)").toString();
	
	private static final String CHECK_GROUPS_PUBLISHED = new StringBuilder()
		.append("SELECT IFNULL(COUNT(*), 0) groupsNotPublished \n")
		.append("FROM Xmlio_Live \n")
		.append("WHERE classNameValue = 'com.liferay.portal.model.Group' \n") 
		.append("  AND groupid IN(%s)  \n")
		.append("  AND existInLive != 'S' \n").toString();
	
	/**
	 * Inicia la publicación de categoriesproperties.<br/><br/>
	 * 
	 * La publicación de las categoriesproperties se hace despues de la publicación del catálogo o del vocabulario, con lo que no nos queda otra
	 * que llevarnos todas las categoriesproperties del propio vocabulario o categoría pese a que algunas puede estar ya publicadas en el LIVE
	 */
	public boolean publishToLiveCategoriesProperties(long scopeGroupId, String assetVocabularyId, String assetCategoryId) throws Exception
	{
		_log.trace("In publishToLiveCategoriesProperties");
		
		StringBuilder sql = new StringBuilder(GET_CATEGORIES_PROPERTIES_TO_EXPORT);
		if (scopeGroupId > 0)
			sql.append( String.format(" AND cp.groupId = %d\n", scopeGroupId) );
		
		if (Validator.isNotNull(assetVocabularyId))
			sql.append( String.format(" AND v.vocabularyId = %s\n", assetVocabularyId) );
		
		if (Validator.isNotNull(assetCategoryId))
			sql.append( String.format(" AND c.categoryId = %s\n", assetCategoryId) );
		
		
		if (_log.isDebugEnabled())
			_log.debug(new StringBuilder("Query to get categoriesproperties:\n").append(sql));	
		
		
		Document doc = PortalLocalServiceUtil.executeQueryAsDom(sql.toString(), new String[]{"globalId"});
		List<Node> rows = doc.getRootElement().selectNodes("/rs/row");
		if (rows.size() > 0)
		{
			// Comprobamos que los grupos a los que pertenecen las categorías ya están publicados			
			checkAllGroupsPublished(XMLHelper.getStringValues(rows, "@groupid"));			
			
			executeJSONRemoteCalls(scopeGroupId, doc.asXML());
		}
		
		return (rows.size() > 0);
	}
	
	private void executeJSONRemoteCalls(long groupId, String exportxml) throws ClientProtocolException, IOException, SystemException, PortalException, DocumentException, ServiceError
	{
		String scopeGroupName = GroupLocalServiceUtil.getGroup(groupId == 0 ? GroupMgr.getGlobalGroupId() : groupId).getName();
		LiveConfiguration liveConf = LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(IterGlobal.getCompanyId());
		
		List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();			
		remoteMethodParams.add(new BasicNameValuePair(XmlioKeys.SERVICE_CLASS_NAME,  	"com.protecmedia.iter.xmlio.service.CategoriesPropertiesPublicationServiceUtil"));
		remoteMethodParams.add(new BasicNameValuePair(XmlioKeys.SERVICE_METHOD_NAME, 	"importContents"));
		remoteMethodParams.add(new BasicNameValuePair(XmlioKeys.SERVICE_PARAMETERS,  	"[scopeGroupName, exportxml]"));
		remoteMethodParams.add(new BasicNameValuePair("scopeGroupName", 				scopeGroupName));
		remoteMethodParams.add(new BasicNameValuePair("exportxml", 						exportxml));
		
		String []url = liveConf.getRemoteIterServer2().split(":");
		HttpHost targetHost = new HttpHost(url[0], Integer.valueOf(url[1]));
		JSONUtil.executeMethod(targetHost, "/xmlio-portlet/secure/json", remoteMethodParams, 
									(int)liveConf.getConnectionTimeOut(),
									(int)liveConf.getOperationTimeOut(),
									liveConf.getRemoteUserName(), liveConf.getRemoteUserPassword());
	}
	
	public Document importContents(String scopeGroupName, String exportxml) throws Exception
	{
		Document dom 		= SAXReaderUtil.read("<rs/>");
		long scopeGroupId 	= GroupLocalServiceUtil.getGroup(IterGlobal.getCompanyId(), scopeGroupName).getGroupId();

		List<Node> rows 	= SAXReaderUtil.read(exportxml).getRootElement().selectNodes("//row");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(rows), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Xml with no data");
		
		Long globalId = GroupMgr.getGlobalGroupId();
		
		StringBuilder values = new StringBuilder();
		StringBuilder auxValue = new StringBuilder()
									.append("(%s, ")
									
									// Obtenemos el id de la categoría correspondiente en el LIVE mediante el globalgroupid
									.append("(SELECT localid             \n")
									.append("   FROM Xmlio_Live x        \n")
									.append("   WHERE x.globalId = '%s'  \n") 
									.append("     AND x.groupId = %s     \n")
									.append("     AND x.classNameValue = '").append(IterKeys.CLASSNAME_CATEGORY).append("'), ")
									
									// Obtenemos el id del artículo correspondiente en el LIVE mediante el globalgroupId
									.append("(SELECT localid             \n")
									.append("   FROM Xmlio_Live x        \n")
									.append("   WHERE x.globalId = '%s'  \n") 
									.append("     AND x.groupId = %s     \n")
									.append("     AND x.classNameValue = '").append(IterKeys.CLASSNAME_JOURNALARTICLE).append("') )");
														
		int size = rows.size();
		for (int n = 0; n < size; n++)
		{
			Node row = rows.get(n);
			
			// Nos llega el globalId del grupo. Obtenemos su identificador en el LIVE
			String groupGlobalId = XMLHelper.getTextValueOf(row, "globalId");
			String groupId = LiveLocalServiceUtil.getLiveByGlobalId(IterKeys.CLASSNAME_GROUP, groupGlobalId).get(0).getLocalId();
			String categoryGlobalId = XMLHelper.getTextValueOf(row, "@categoryGlobalId");
			String articleGlobalId  = XMLHelper.getTextValueOf(row, "@articleGlobalId" );
			
			values.append(String.format(auxValue.toString(), groupId, categoryGlobalId, globalId, articleGlobalId, globalId));
			
			if (n < rows.size() -1)
			{
				values.append(", ");
			}
		}	
		
		String sql = String.format(INSERT_CATEGORIES_PROPERTIES, values);
		
		if (_log.isDebugEnabled())
			_log.debug(new StringBuilder("Query to insert categoriesproperties:\n").append(sql) );		
		
		PortalLocalServiceUtil.executeUpdateQuery(sql);
		
		if (scopeGroupId != GroupMgr.getGlobalGroupId())
		{
			// Se toma la fecha de última publicación y se actualiza dicho campo
			String lastUpdate = String.valueOf(GroupMgr.getPublicationDate(scopeGroupId).getTime());
			dom.getRootElement().addAttribute(IterKeys.XMLIO_XML_LAST_UPDATE_ATTRIBUTE, lastUpdate);
			
			TomcatUtil.updatePublicationDateNoException(IterGlobal.getCompanyId(), scopeGroupId);
		}
		return dom;
	}
	// Comprueba que los grupos a los que pertenecen las categorías ya están publicados
	private void checkAllGroupsPublished(String[] groupsIds) throws ServiceError
	{
		_log.trace("In checkAllGroupsPublished");	
				
		if (Validator.isNotNull(groupsIds))
		{			
			StringBuilder aux = new StringBuilder();
			for (int i = 0; i < groupsIds.length; i++)
			{
				aux.append(groupsIds[i]);
				if(i < groupsIds.length - 1)
				{
					aux.append(", ");
				}
			}
			
			String sql = String.format(CHECK_GROUPS_PUBLISHED, aux);
			
			if (_log.isDebugEnabled())
				_log.debug(new StringBuilder("Query to chek if all groups are published:\n").append(sql));
			
			List<Object> list = PortalLocalServiceUtil.executeQueryAsList(sql);
			
			int groupsNotPublished = ((Number)list.get(0)).intValue();
			
			ErrorRaiser.throwIfFalse(groupsNotPublished == 0, XmlioKeys.XYZ_E_GROUP_NOT_PUBLISHED_IN_LIVE_ZYX, "Trying to publish a category with a not published group");
		}
		else
		{
			_log.debug("No groups to check");
		}	
	}
}