package com.protecmedia.iter.xmlio.service.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.xmlio.service.base.DaylyTopicsPublicationLocalServiceBaseImpl;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;
import com.protecmedia.iter.xmlio.service.util.XmlioKeys;

@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class DaylyTopicsPublicationLocalServiceImpl extends DaylyTopicsPublicationLocalServiceBaseImpl {
	
private static Log _log = LogFactoryUtil.getLog(DaylyTopicsPublicationLocalServiceImpl.class);

	// Para la llamada json	
	private static final String CLASS_PATH 							= "com.protecmedia.iter.xmlio.service.DaylyTopicsPublicationServiceUtil";	
	private static final String PUBLISH_IN_LIVE 					= "publishInLive";
	private static final String PARAMETERS_TO_PUBLISHINLIVE_METHOD 	= "[groupId,daylyTopicsToBePublished,daylyTopicsIdsToBeRemoved]";
	private static final String DAYLYTOPICS_TO_BE_PUBLISHED 		= "daylyTopicsToBePublished";
	private static final String DAYLYTOPICS_TO_BE_MOVED 			= "daylyTopicsIdsToBeRemoved";	
	private static final String CLASS_NAME_LAYOUT                   = "com.liferay.portal.model.Layout";
	private static final String CLASS_NAME_ARTICLE                  = "com.liferay.portlet.journal.model.JournalArticle";
	private static final String CLASS_NAME_CATEGORY					= "com.liferay.portlet.asset.model.AssetCategory";
	private static final String CLASS_NAME_TEMPLATE                 = "com.protecmedia.iter.designer.model.PageTemplate";
	
	private static final String PLID = "plid";	
	private static final String COLUMNS_MUST_BE_CDATA[] = {"displayname","url","publicationdate"};
	private static final String REMOTE_METHOD_PATH     = "/xmlio-portlet/secure/json";
	
	private static String DELETE_DAYLY_TOPICS        = "DELETE FROM daylytopic WHERE daylytopicid in(%s)";	
	private static String DELETE_DAYLY_TOPICSDELETED = "DELETE FROM daylytopicdeleted WHERE daylytopicid in(%s)";	
	private static String UPDATE_PUBLICATION_DATE    = "UPDATE daylytopic SET publicationdate = SYSDATE() WHERE daylytopicid IN (%s)";
	
	private static final String INSERT_DAILYTOPIC = new StringBuilder()
		.append("INSERT INTO daylytopic (daylytopicid, groupid, layoutid, modelid, displayname, articleid, sectionid, url, categoryid, ") 
	    .append(" targetblank, sort, daylytopics, publicationdate) \n VALUES %s \n")
	    .append("ON DUPLICATE KEY UPDATE layoutid = values(layoutid), modelid = values(modelid), displayname = values(displayname), \n")
	    .append("  articleid = values(articleid), sectionid = values(sectionid), url = values(url), categoryid = values(categoryid), \n")
	    .append("  targetblank = values(targetblank), sort = values(sort), daylytopics = values(daylytopics), \n")
	    .append("  publicationdate = sysdate()").toString();
	
	private static String GET_DAYLYTOPICS_WITH_NO_PUBLICATION_DATE = new StringBuilder()
		.append("SELECT daylytopicid, xmlio4.globalId AS layoutid, xmlio2.globalId AS modelid, displayname, articleid, xmlio3.globalId AS sectionid, url, Xmlio_Live.globalId AS categoryid, targetblank, sort, daylytopics \n")
		.append("FROM daylytopic \n")
		.append("\t LEFT JOIN Xmlio_Live ON ( daylytopic.categoryid=Xmlio_Live.localid AND Xmlio_Live.classNameValue='%s' ) \n")
		.append("\t LEFT JOIN Designer_PageTemplate ON (daylytopic.modelid=Designer_PageTemplate.id_) \n")
		.append("\t LEFT JOIN Xmlio_Live xmlio2 ON ( Designer_PageTemplate.pageTemplateId=xmlio2.localid AND xmlio2.classNameValue='%s' ) \n")
		.append("\t LEFT JOIN Xmlio_Live xmlio3 ON ( daylytopic.sectionid=xmlio3.localid AND xmlio3.classNameValue='%s' ) \n")
		.append("\t LEFT JOIN Xmlio_Live xmlio4 ON ( daylytopic.layoutid=xmlio4.localid AND xmlio4.classNameValue='%s' ) \n")
		.append("WHERE daylytopic.groupid = %s \n")
		.append("  AND daylytopic.layoutid %s \n")
		.append("  AND daylytopic.publicationdate is null").toString();
	
	private static String GET_DAYLYTOPICS_TO_BE_DELETED = new StringBuilder()
		.append("SELECT daylytopicid \n")
		.append("FROM daylytopicdeleted \n")
		.append("WHERE groupid = %s \n")
		.append("  AND layoutid %s").toString();	
	
	// Consulta para ver si los daylytopics están asociados a elementos aun no publicados
	private static String CHECK_DAYLYTOPICS = new StringBuilder()
		.append("SELECT ifnull(count(*), 0) count, 'layout' type \n")
		.append("FROM Xmlio_Live \n")
		.append("WHERE classNameValue = '").append(CLASS_NAME_LAYOUT).append("' \n")
		.append("  AND groupId = %s \n")
		.append("  AND existInLive != 'S' \n")
		.append("  AND (localId in (SELECT DISTINCT(sectionid) \n")
		.append("                  FROM daylytopic \n")
		.append("                  WHERE groupid = %s \n")
		.append("                    AND layoutid %s \n")
		.append("                    AND sectionid IS NOT NULL \n") 
		.append("                    AND publicationdate IS NULL) \n")
		.append("%s")
  
		.append("UNION ALL \n")
	
		.append("SELECT ifnull(count(*), 0) count, 'article' type \n")
		.append("FROM Xmlio_Live \n")
		.append("WHERE classNameValue = '").append(CLASS_NAME_ARTICLE).append("' \n")
		.append("  AND groupId = %s \n")
		.append("  AND existInLive != 'S' \n")
		.append("  AND localId in (SELECT DISTINCT(articleid) \n")
		.append("	               FROM daylytopic \n")
		.append("	               WHERE groupid = %s \n")
		.append("	                 AND layoutid %s \n")
		.append("                    AND articleid IS NOT NULL \n") 
		.append("                    AND publicationdate IS NULL) \n")
		.append("UNION ALL \n")
	
		.append("SELECT ifnull(count(*), 0) count, 'category' type \n")
		.append("FROM Xmlio_Live \n")
		.append("WHERE classNameValue = '").append(CLASS_NAME_CATEGORY).append("' \n")
		.append("  AND groupId = %s \n")
		.append("  AND existInLive != 'S' \n")
		.append("  AND localId in (SELECT DISTINCT(categoryid) \n")
		.append("                  FROM daylytopic \n")
		.append("                  WHERE groupid = %s \n")
		.append("                    AND layoutid %s \n")
		.append("                    AND categoryid IS NOT NULL \n") 
		.append("                    AND publicationdate IS NULL) \n")
		.append("UNION ALL \n")
	
		.append("SELECT ifnull(count(*), 0) count, 'designtemplate' type \n")
		.append("FROM Xmlio_Live \n")
		.append("WHERE classNameValue = '").append(CLASS_NAME_TEMPLATE).append("' \n")
		.append("  AND groupId = %s \n")
		.append("  AND existInLive != 'S' \n")
		.append("  AND localId in (SELECT DISTINCT(dp.pageTemplateId) \n")
		.append("                  FROM daylytopic d, designer_pagetemplate dp \n")
		.append("                  WHERE d.modelid = dp.id_ \n")
		.append("                    AND d.groupid = %s \n")
		.append("                    AND d.layoutid %s \n")
		.append("                    AND modelid IS NOT NULL \n")
		.append("                    AND publicationdate IS NULL) ").toString();
	
	private static String GET_LOCALID = "select Xmlio_Live.classNameValue, Xmlio_Live.globalid, Xmlio_Live.localid, Designer_PageTemplate.id_ as localmodelid \n from Xmlio_Live \n\t left join Designer_PageTemplate ON (Xmlio_Live.localid=Designer_PageTemplate.pageTemplateId) \n where Xmlio_Live.globalid in ('%s')";
	
	public synchronized void publish(String scopeGroupId, List<Node> section, Object[] dataToJson) throws Exception{
		_log.trace("In DaylyTopicsPublication.publish");
		
		ErrorRaiser.throwIfFalse(null != section && section.size() == 1, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid section receibed");
		
		final Node param = section.get(0);
		final String name = XMLHelper.getTextValueOf(param, "@name");
		ErrorRaiser.throwIfFalse(name.equalsIgnoreCase(PLID), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid parameter 'name'");
		
		String sectionFilter = XMLHelper.getTextValueOf(param, "text()");
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder("Plid receibed: '").append(sectionFilter).append("'"));
		
		// Si llega "0" obtenemos los daylytopics con sección nula
		if ("0".equals(sectionFilter)){
			sectionFilter = "is NULL";
		}else{
			sectionFilter = new StringBuilder("= ").append(sectionFilter).toString();
		}
		
		// Obtenemos los daylytopics a publicar y a borrar
		final Document daylyTopicsToBePublished = getDaylytopicsWithNoPublicationDate(scopeGroupId, sectionFilter);		
		final String daylyTopicsIdsToBeRemoved  = getDaylytopicsIdsToBeDeleted(scopeGroupId, sectionFilter);
		
		// Hay que hacer cambios
		if ((null != daylyTopicsToBePublished && daylyTopicsToBePublished.getRootElement().selectNodes("//row").size() > 0) || 
			 Validator.isNotNull(daylyTopicsIdsToBeRemoved)){
			
			// Comprobamos que las foreign keys de los daylytopics están ya publicados
			checkDaylytopics(scopeGroupId, sectionFilter);
			
			_log.debug("Starting publication daily topics");
						
			List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();			
			remoteMethodParams.add(new BasicNameValuePair(XmlioKeys.SERVICE_CLASS_NAME,          CLASS_PATH));
			remoteMethodParams.add(new BasicNameValuePair(XmlioKeys.SERVICE_METHOD_NAME,         PUBLISH_IN_LIVE));
			remoteMethodParams.add(new BasicNameValuePair(XmlioKeys.SERVICE_PARAMETERS,         PARAMETERS_TO_PUBLISHINLIVE_METHOD));
			remoteMethodParams.add(new BasicNameValuePair("groupId",                    GroupLocalServiceUtil.getGroup(Long.valueOf(scopeGroupId).longValue()).getName()));
			remoteMethodParams.add(new BasicNameValuePair(DAYLYTOPICS_TO_BE_PUBLISHED,  daylyTopicsToBePublished.asXML()));
			remoteMethodParams.add(new BasicNameValuePair(DAYLYTOPICS_TO_BE_MOVED, daylyTopicsIdsToBeRemoved));			
			
			// Realizamos la petición al live mediante json		
			if(_log.isDebugEnabled()){
				_log.debug(new StringBuilder("Json call:\n")
							.append(dataToJson[0])     .append(",")
							.append(dataToJson[1])     .append(",")
							.append(dataToJson[2])     .append(",")
							.append(dataToJson[3])     .append(",")
							.append(dataToJson[4])     .append(",")
							.append(REMOTE_METHOD_PATH).append(",")
							.append("remoteMethodParams").toString());
			
				_log.debug(new StringBuilder("Where remoteMethodParams is a list of pair values:\n")
				.append("'").append(XmlioKeys.SERVICE_CLASS_NAME) .append("', '").append(CLASS_PATH)                        .append("'\n")
				.append("'").append(XmlioKeys.SERVICE_METHOD_NAME).append("', '").append(PUBLISH_IN_LIVE)                   .append("'\n")
				.append("'").append(XmlioKeys.SERVICE_PARAMETERS) .append("', '").append(PARAMETERS_TO_PUBLISHINLIVE_METHOD).append("'\n")
				.append("'").append("groupId")                    .append("', '").append(scopeGroupId)					    .append("'\n")
				.append("'").append(DAYLYTOPICS_TO_BE_PUBLISHED)  .append("', '").append(daylyTopicsToBePublished.asXML())  .append("'\n")
				.append("'").append(DAYLYTOPICS_TO_BE_MOVED)      .append("', '").append(daylyTopicsIdsToBeRemoved).append("'"));
			}
			
			// Importante no llamar a executeJSONRemoteMethod ya que en caso de excepción devuelve un json no válido.
			String result = XMLIOUtil.executeJSONRemoteMethod2((Long)dataToJson[0],		// Compañía 
				                                              (String)dataToJson[1],	// ip 
				                                              (Integer)dataToJson[2],	// puerto 
															  (String)dataToJson[3],	// usuario 
															  (String)dataToJson[4], 	// contraseña
															  REMOTE_METHOD_PATH, 		// ruta de quien lee el la peticion
															  remoteMethodParams);		// Parametros que necesita el método
			
			JSONObject jsonResponse = JSONFactoryUtil.createJSONObject(result);
			
			// Comprobamos el resultado de la operación.
			String errorMsg = jsonResponse.getString("exception");
			if (!errorMsg.isEmpty()) 
			{	
				// Puede ser una excepción de tipo Iter, si no lo es devuelve todo el texto y también se lanza porque era una excepción del sistema.
				String iterErrorMsg = ServiceErrorUtil.containIterException(errorMsg);
				_log.debug(new StringBuilder("There was an error in the LIVE server:\n").append(iterErrorMsg.isEmpty() ? errorMsg : iterErrorMsg));
				throw new SystemException(iterErrorMsg.isEmpty() ? errorMsg : iterErrorMsg);
				
			// La operación en el live ha ido bien	
			}else{					
				// Actualizamos la fecha de publicación de los daylytopics en el back 
				updateDaylyTopicsPublicationDate(daylyTopicsToBePublished);
				
				// Borramos los daylytopicdeleted en el back	
				if (Validator.isNotNull(daylyTopicsIdsToBeRemoved)){
					deleteDaylyTopicsDelted(daylyTopicsIdsToBeRemoved);
				}		
								
				/* Actualizamos la caché del live. 
				 El último parámetro es vacío (última fecha de publicación) para que no se lancen newsletters 
				 (no se publican nada que tenga que ver con contenidos/artículos) */
				XMLIOUtil.deleteRemoteCache(Long.parseLong(scopeGroupId), "");
				_log.info("Publication of daily topics finished");
			}
		}else if(_log.isDebugEnabled()){
			_log.debug(new StringBuilder("No daily topics to published"));
		}
	}	
	
	private static Document getDaylytopicsWithNoPublicationDate(String groupId, String filter) throws SecurityException, NoSuchMethodException
	{
		_log.trace("In DaylyTopicsPublication.getDaylytopicsWithNoPublicationDate");
		
		final String sql = String.format(GET_DAYLYTOPICS_WITH_NO_PUBLICATION_DATE, IterKeys.CLASSNAME_CATEGORY, IterKeys.CLASSNAME_PAGETEMPLATE, IterKeys.CLASSNAME_LAYOUT, IterKeys.CLASSNAME_LAYOUT, groupId, filter);
		if (_log.isDebugEnabled())
			_log.debug(new StringBuilder("Query to get daylytopics to be published:\n").append(sql));
		
		return PortalLocalServiceUtil.executeQueryAsDom(sql, COLUMNS_MUST_BE_CDATA);	
	}
	
	private static String getDaylytopicsIdsToBeDeleted(String groupId, String filter) throws SecurityException, NoSuchMethodException{
		_log.trace("In DaylyTopicsPublication.getDaylytopicsWithNoPublicationDate");
		
		StringBuilder result = new StringBuilder("");
		
		final String sql = String.format(GET_DAYLYTOPICS_TO_BE_DELETED, groupId, filter);
		if (_log.isDebugEnabled())
			_log.debug(new StringBuilder("Query to get daylytopics to be deleted:\n").append(sql));
		
		final Document doc = PortalLocalServiceUtil.executeQueryAsDom(sql);
		
		if (null != doc){
			final List<Node> daylyTopicsIdsToBeDeleted = doc.getRootElement().selectNodes("//row");
			
			if (null != daylyTopicsIdsToBeDeleted && daylyTopicsIdsToBeDeleted.size() > 0){
				
				
				for (int i = 0; i < daylyTopicsIdsToBeDeleted.size(); i++){
					result.append("'").append(XMLHelper.getTextValueOf(daylyTopicsIdsToBeDeleted.get(i), "@daylytopicid")).append("'");
					
					if(i < daylyTopicsIdsToBeDeleted.size() -1){
						result.append(", ");
					}
				}
			}else{
				_log.debug("No daylytopics to be deleted");
			}
		}		
		return result.toString();
	}
	
	private static void deleteDaylyTopicsDelted(String daylyTopicsDeletedIds) throws IOException, SQLException{
		_log.trace("In DaylyTopicsPublication.deleteDaylytopicsDelted");
		
		final String sql = String.format(DELETE_DAYLY_TOPICSDELETED, daylyTopicsDeletedIds);
		if (_log.isDebugEnabled())
			_log.debug(new StringBuilder("Query to delete daylytopicsdeleted:\n").append(sql));
		
		PortalLocalServiceUtil.executeUpdateQuery(sql);		
	}
	
	private static void deleteDaylyTopics(String daylyTopicsIds) throws IOException, SQLException{
		_log.trace("In DaylyTopicsPublication.deleteDaylyTopics");
		
		final String sql = String.format(DELETE_DAYLY_TOPICS, daylyTopicsIds);
		if (_log.isDebugEnabled()){
			_log.debug(new StringBuilder("Query to delete daylytopics in Live server:\n").append(sql));
		}
		PortalLocalServiceUtil.executeUpdateQuery(sql);
		_log.debug("Dayly topics removed");
	}
	
	private static void updateDaylyTopicsPublicationDate(Document daylyTopicsToBePublished) throws IOException, SQLException{
		_log.trace("In DaylyTopicsPublication.updateDaylyTopicsPublicationDate");
		
		if (null != daylyTopicsToBePublished){
			Element root = daylyTopicsToBePublished.getRootElement();
			
			List<Node> rows = root.selectNodes("//row");
			
			if (null != rows && rows.size() > 0){
				String[] ids = XMLHelper.getStringValues(rows, "@daylytopicid");				
				
				StringBuilder auxIds = new StringBuilder();
				for (int i = 0; i < ids.length; i++){
					auxIds.append("'").append(ids[i]).append("'");
					if (i < ids.length -1){
						auxIds.append(", ");
					}
				}
				
				final String sql = String.format(UPDATE_PUBLICATION_DATE, auxIds.toString());
				if (_log.isDebugEnabled())
					_log.debug(new StringBuilder("Query to update daylytopics publication date in BACK server:\n").append(sql));
				
				PortalLocalServiceUtil.executeUpdateQuery(sql);
			}
		}		
	}
	
	// Da de alta y borra los daylytopics que le dice el back
	public void publishInLive(String groupName, String daylyTopicsToBePublished, String daylyTopicsIdsToBeRemoved) 
					throws SystemException, IOException, SQLException, DocumentException, NumberFormatException, com.liferay.portal.kernel.error.ServiceError, SecurityException, NoSuchMethodException, PortalException
	{
		_log.trace("In DaylyTopicsPublication.publishInLive");
		
		ErrorRaiser.throwIfFalse(Validator.isNotNull(groupName),  IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Invalid groupId");
		
		long scopeGrpId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
		
		if (Validator.isNotNull(daylyTopicsIdsToBeRemoved)){
			deleteDaylyTopics(daylyTopicsIdsToBeRemoved);			
		}else{
			_log.debug("No daylytopics to be removed");
		}
		
		insertDaylyTopics(daylyTopicsToBePublished, scopeGrpId);	
		
		// Actualizamos la fecha de publicacion en el live
		_log.debug("Updating publication date in live");
		GroupMgr.updatePublicationDate(scopeGrpId, new Date());
	}
	
	private void insertDaylyTopics(String daylyTopicsToBePublished, long scopeGroupId) throws DocumentException, IOException, SQLException, SecurityException, NoSuchMethodException{
		_log.trace("In DaylyTopicsPublication.insertDaylyTopics");
		
		final Document document = SAXReaderUtil.read(daylyTopicsToBePublished);
		final List<Node> rows = document.getRootElement().selectNodes("//row");
		
		
		String[] categories = XMLHelper.getStringValues(rows, "@categoryid");
		String[] models = XMLHelper.getStringValues(rows, "@modelid");
		String[] layouts = XMLHelper.getStringValues(rows, "@layoutid");
		String[] sections = XMLHelper.getStringValues(rows, "@sectionid");
		String[] globalids = ArrayUtil.append(categories, models);
		globalids = ArrayUtil.append(globalids, layouts);
		globalids = ArrayUtil.append(globalids, sections);
		
		Element resultRoot = null;
		if( globalids!=null && globalids.length>0 )
		{
			String query = String.format(GET_LOCALID, StringUtil.merge(globalids, "','"));
			if(_log.isDebugEnabled())
				_log.debug("Get localids from globalids: \n " + query);
			Document result = PortalLocalServiceUtil.executeQueryAsDom(query);
			resultRoot = result.getRootElement();
		}
		
		if (null != rows && rows.size() > 0){
			
			StringBuilder values = new StringBuilder();
			
			// Montamos todos los inserts
			for (int i = 0; i < rows.size(); i++){
				final Node row = rows.get(i);
				
				final String daylyTopicId = XMLHelper.getTextValueOf(row, "@daylytopicid");
				final String displayName  = XMLHelper.getTextValueOf(row, "displayname/text()");
				final String articleId    = XMLHelper.getTextValueOf(row, "@articleid");
				final String url          = XMLHelper.getTextValueOf(row, "url/text()");
				final String targetblank  = XMLHelper.getTextValueOf(row, "@targetblank");
				final String sort         = XMLHelper.getTextValueOf(row, "@sort");
				final String daylytopics  = XMLHelper.getTextValueOf(row, "@daylytopics");
				
				String layoutId     = XMLHelper.getTextValueOf(row, "@layoutid");
				if(Validator.isNotNull(layoutId) && resultRoot!=null)
					layoutId = XMLHelper.getTextValueOf(resultRoot, String.format("/rs/row[@globalid='%s' and @classNameValue='%s']/@localid", layoutId, IterKeys.CLASSNAME_LAYOUT ) );
				
				String sectionid    = XMLHelper.getTextValueOf(row, "@sectionid");
				if(Validator.isNotNull(sectionid) && resultRoot!=null)
					sectionid = XMLHelper.getTextValueOf(resultRoot, String.format("/rs/row[@globalid='%s' and @classNameValue='%s']/@localid", sectionid, IterKeys.CLASSNAME_LAYOUT ) );
				
				String modelId      = XMLHelper.getTextValueOf(row, "@modelid");
				if(Validator.isNotNull(modelId) && resultRoot!=null)
					modelId = XMLHelper.getTextValueOf(resultRoot, String.format("/rs/row[@globalid='%s' and @classNameValue='%s']/@localmodelid", modelId, IterKeys.CLASSNAME_PAGETEMPLATE ) );
				
				String categoryid   = XMLHelper.getTextValueOf(row, "@categoryid");
				if(Validator.isNotNull(categoryid) && resultRoot!=null)
					categoryid = XMLHelper.getTextValueOf(resultRoot, String.format("/rs/row[@globalid='%s' and @classNameValue='%s']/@localid", categoryid, IterKeys.CLASSNAME_CATEGORY ) );
		
				values.append("(")
				.append("'").append(daylyTopicId).append("', ")
				.append(scopeGroupId).append(", ")
				.append( (Validator.isNotNull(layoutId) && !layoutId.equals("0") ? layoutId : "null")).append(", ")
				.append( (Validator.isNotNull(modelId) ? modelId : "null"))							  .append(", ")
				// El display name no puede ser nullo
				.append( (Validator.isNotNull(displayName) ? ("'" + displayName.replaceAll(StringPool.APOSTROPHE, StringPool.DOUBLE_APOSTROPHE) + "'") : "''")).append(", ") 
				.append( (Validator.isNotNull(articleId)   ? ("'" + articleId   + "'") : "null"))     .append(", ")
				.append( (Validator.isNotNull(sectionid)   ?                 sectionid : "null"))	  .append(", ")
				.append( (Validator.isNotNull(url)         ? "'"  + url         + "'"  : "null"))     .append(", ")
				.append( (Validator.isNotNull(categoryid)  ? categoryid                : "null"))     .append(", ")
				.append( (targetblank.equalsIgnoreCase("true") ? "1" : "0") )						  .append(", ")
				.append(sort)																		  .append(", ")
				.append( (daylytopics.equalsIgnoreCase("true") ? "1" : "0") )					      .append(", ")
				.append("sysdate()")
				.append(")").toString();
				
				if (i < rows.size() -1){
					values.append(",");
				}
			}
			
			final String sql = String.format(INSERT_DAILYTOPIC, values);
			
			if(_log.isDebugEnabled()){
				_log.debug(new StringBuilder("Query to insert new daylytopics in LIVE server:\n").append(sql));
			}
			PortalLocalServiceUtil.executeUpdateQuery(sql);
		}else{
			_log.debug("No new daily topics to be published");
		}	
	}
	
	// Cuenta si hay elementos asociados a los daylytopics aun no publicados en el LIVE
	private void checkDaylytopics(String scopeGroupId, String sectionFilter) throws SecurityException, NoSuchMethodException, ServiceError{
		_log.trace("In DaylyTopicsPublicationLocalServiceImpl.checkDaylytopics");
		
		String sql = String.format(CHECK_DAYLYTOPICS, 
				// Secciones
				scopeGroupId, scopeGroupId, sectionFilter,				
				new StringBuilder(sectionFilter.equalsIgnoreCase("is NULL") ? "" : "       OR localId = '" + sectionFilter.replaceAll("= ", "") + "'")
		                          .append(") \n"),
				
				// Artículos
                GroupMgr.getGlobalGroupId(), scopeGroupId, sectionFilter,
                
                // Categorías
                GroupMgr.getGlobalGroupId(), scopeGroupId, sectionFilter,
                
                // PageTemplate
                scopeGroupId, scopeGroupId, sectionFilter);			
						
		if (_log.isDebugEnabled())
			_log.debug(new StringBuilder("Query to check daylytopics: \n").append(sql));
		
		Document result = PortalLocalServiceUtil.executeQueryAsDom(sql);
		
		Node root = result.getRootElement();
		
		long notPublishedLayouts  		= XMLHelper.getLongValueOf(root.selectSingleNode("/rs/row[@type='layout']"),         "@count");
		ErrorRaiser.throwIfFalse(notPublishedLayouts == 0, XmlioKeys.XYZ_E_SECTION_NOT_PUBLISHED_IN_LIVE_ZYX, "They are sections not published");
		
		long notPublishedArticles 		= XMLHelper.getLongValueOf(root.selectSingleNode("/rs/row[@type='article']"),        "@count");
		ErrorRaiser.throwIfFalse(notPublishedArticles == 0, XmlioKeys.XYZ_E_ARTICLE_NOT_PUBLISHED_IN_LIVE_ZYX, "They are articles not published");
		
		long notPublishedCategory 		= XMLHelper.getLongValueOf(root.selectSingleNode("/rs/row[@type='category']"), 		 "@count");
		ErrorRaiser.throwIfFalse(notPublishedCategory == 0, XmlioKeys.XYZ_E_ASSET_CATEGORY_NOT_PUBLISHED_IN_LIVE_ZYX, "They are asset categories not published");
		
		long notPublishedDesigntemplate = XMLHelper.getLongValueOf(root.selectSingleNode("/rs/row[@type='designtemplate']"), "@count");		
		ErrorRaiser.throwIfFalse(notPublishedDesigntemplate == 0, XmlioKeys.XYZ_E_ASSET_DESIGN_TEMPLATE_NOT_PUBLISHED_IN_LIVE_ZYX, "They are design template not published");		
	}
}