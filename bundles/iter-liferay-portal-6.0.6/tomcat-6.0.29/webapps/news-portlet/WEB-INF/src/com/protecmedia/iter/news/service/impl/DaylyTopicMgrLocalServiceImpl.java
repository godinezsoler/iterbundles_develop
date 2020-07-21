package com.protecmedia.iter.news.service.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.news.service.base.DaylyTopicMgrLocalServiceBaseImpl;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;

@Transactional(isolation = Isolation.PORTAL, rollbackFor = {Exception.class} )
public class DaylyTopicMgrLocalServiceImpl extends DaylyTopicMgrLocalServiceBaseImpl {
	
	private static Log _log = LogFactoryUtil.getLog(DaylyTopicMgrLocalServiceImpl.class);

	private static final String NO_DAILYTOPICS     = "0";
	private static final String DEFAULT_NEW_WINDOW = "1";
	private static final String ARTICLE_LINK       = "content";
	private static final String SECTION_LINK       = "layout";
	private static final String CATEGORY_LINK      = "category";
	private static final String URL_LINK           = "url";
	private static final String DEFAULT_SORT       = "1";	
	
	private static final String INSERT_DAILYTOPICDELETED = "INSERT INTO daylytopicdeleted (daylytopicid, groupid, layoutid) VALUES %s";
	private static final String DAILYTOPICS_TO_BE_MOVED_TO_DELETED = new StringBuilder("SELECT daylytopicid, layoutid \n")
																	 		   .append("FROM daylytopic \n")
																 		   	   .append("WHERE groupid = %s \n")
															 		   	   	   .append("  AND layoutid %s \n")
															 		   	   	   .append("  AND publicationdate IS NOT NULL ").toString();
															
	private static final String DELETE_DAILYTOPICS       = new StringBuilder("DELETE FROM daylytopic \n")
	                                                                 .append("WHERE groupid = %s \n")   
																	 .append("  AND layoutid %s").toString();	
	private static final String INSERT_DAILYTOPIC = 
      new StringBuilder("INSERT INTO daylytopic (daylytopicid, groupid, layoutid, modelid, displayname, articleid, sectionid, url, categoryid, ") 
                     				   .append(" targetblank, sort, daylytopics) \n VALUES(%s)").toString();
		
	/* Documentación: http://10.15.20.59:8090/pages/viewpage.action?pageId=22841404 
	   Se establecen los dailytopics */
	public void setDaylyTopics(String xml) throws Exception{
		_log.trace("In setDaiLyTopics");		
		
		ErrorRaiser.throwIfFalse(Validator.isNotNull(xml), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "receibed XML is null or empty");
		
		Document doc = null;
		try{
			doc = SAXReaderUtil.read(xml);
		}catch(DocumentException d){
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Malformed XML receibed");
		}
		
		// El target indica el grupo y la sección
		final List<Node> targets = doc.getRootElement().selectNodes("/root/target");		
		ErrorRaiser.throwIfFalse(null != targets && targets.size() > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "XML with no targets");
		
		String groupId = null;
		
		final int size = targets.size();
		for (int t = 0; t < size; t++){
			
			final Node target = targets.get(t);			
			// Cuando no se quiera sección vendrá un cero
			final String layoutId = XMLHelper.getTextValueOf(target, "@layoutid", null); // seccion
			groupId  = XMLHelper.getTextValueOf(target, "@groupid",  null); 			 // grupo
			final String heritage = XMLHelper.getTextValueOf(target, "@heritage", null); // herencia
			
			ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "groupId is null or empty in the receibed xml");
			
			if(_log.isDebugEnabled())
			_log.debug(new StringBuilder("Target ").append(t).append(" with:\n")
				                        .append("layoutId: ").append( (Validator.isNotNull(layoutId) ? layoutId : "null") ).append("\n")
				                        .append("groupId: '").append(groupId).append("'\n")
				                        .append("heritage: ").append( (Validator.isNotNull(heritage) ? "'" + heritage + "'"  : "null") ) );			
			
			// Borramos todos los dailytopics del grupo y seccion indicados
			if (t == 0){
				deleteDailyTopics(groupId, layoutId);
			}
			
			// No viene el atributo heritage, se especifican los dailytopics que se quieren para la sección
			if (null == heritage){
				_log.debug("Target without attribute heritage");
				
				// Obtenemos los dailytopics
				final List<Node> dailyTopics = target.selectNodes("param");
				if (null != dailyTopics && dailyTopics.size() > 0){
					
					final int sizeD = dailyTopics.size();
					for (int d = 0; d < sizeD; d++){						
						insertDailyTopic(d+1, groupId, layoutId, dailyTopics.get(d));
					}						
				}
				
			// No se quieren dailytopics para la sección actual. Necesitamos insertar un registro con dailytopics = 0 para la seccion dada.
			}else if(null != layoutId && heritage.equals(NO_DAILYTOPICS)){
				_log.debug("No dailytopic for the layout");
				insertDailyTopicForSectionWithNoDailyTopics(groupId, layoutId);
				
			// Se quiere que la seccion herede los dailytopics de sus padres, no es necesario insertar ningun registro
			}else if(null != layoutId && heritage.equals("-1")){
				_log.debug("No dailytopic insert is needed (heritage = -1)");
			
			// Valor no válido para la herencia
			}else{
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, new StringBuilder("Invalid attribute value for heritage: '").append(heritage).append("'").toString() );
			}				
		}	
		
		// Actualizamos la fecha de publicación para los dailytopics que estén en catálogos vean los cambios realizados
		GroupMgr.updatePublicationDate(Long.parseLong(groupId), new Date());
	}
	
	private static void insertDailyTopic(int sort, String groupId, String layoutId, Node dailyTopic) throws ServiceError, IOException, SQLException, NumberFormatException, SystemException, PortalException{
		_log.trace("In insertDailyTopic");
		
		// Datos del dailytopic
		final String displayName = XMLHelper.getTextValueOf(dailyTopic, "@name",           null);
		final String linkType    = XMLHelper.getTextValueOf(dailyTopic, "@type",           null);
		final String newWindow   = XMLHelper.getTextValueOf(dailyTopic, "@newwindow",      DEFAULT_NEW_WINDOW);
		String modelId           = XMLHelper.getTextValueOf(dailyTopic, "@pagetemplateid", null);
		String value             = XMLHelper.getTextValueOf(dailyTopic, "@value",          null);
		
		if(_log.isDebugEnabled())		
			_log.debug(new StringBuilder("Dailytopic " + sort + " with:\n") 
							.append("name: ")          .append( (Validator.isNotNull(displayName) ? "'" + displayName + "'" : "null")).append("\n") 
							.append("type: ")          .append( (Validator.isNotNull(linkType)    ? "'" + linkType    + "'" : "null")).append("\n") 
							.append("newwindow: ")     .append( (Validator.isNotNull(newWindow)   ? "'" + newWindow   + "'" : "null")).append("\n") 
							.append("pagetemplateid: ").append( (Validator.isNotNull(modelId)     ? "'" + modelId     + "'" : "null")).append("\n") 
							.append("value: ")		   .append( (Validator.isNotNull(value)       ? "'" + value       + "'" : "null")) );
		
		// Datos obligatorios
		ErrorRaiser.throwIfFalse(Validator.isNotNull(displayName), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Attribute name is null or empty" );
		ErrorRaiser.throwIfFalse(Validator.isNotNull(linkType),    IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Attribute type is null or emtpy" );
		ErrorRaiser.throwIfFalse(Validator.isNotNull(value),       IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "Attribute value is null or emtpy");	
		
		// Comprobación del tipo de link
		if(!linkType.equals(ARTICLE_LINK) && !linkType.equals(SECTION_LINK) && !linkType.equals(CATEGORY_LINK) && !linkType.equals(URL_LINK)){
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, new StringBuilder("Invalid attribute type: '").append(linkType).append("'").toString() );
		}
		
		// Si el enlace es una categoria tiene que venir el modelo también
		if (linkType.equalsIgnoreCase(CATEGORY_LINK) && (Validator.isNull(modelId) || modelId.equals("0") )){
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "dayly topic with a category link and no model");
		}
		
		// Comprobamos los valores para abrir el enlace
		if(!newWindow.equals("0") && !newWindow.equals(DEFAULT_NEW_WINDOW)){
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_INVALIDARG_ZYX, new StringBuilder("Invalid attribute newwindow: '").append(newWindow).append("' [0|1]").toString() );
		}
		
		String values = new StringBuilder("'").append(PortalUUIDUtil.newUUID()).append("', ")
							.append( (Validator.isNotNull(groupId)                                                           ?       groupId        : "null")).append(", ")
							.append( (Validator.isNotNull(layoutId) && !layoutId.equals("0")                                 ?       layoutId       : "null")).append(", ")
							.append( (Validator.isNotNull(modelId) && linkType.equals(CATEGORY_LINK) && !modelId.equals("0") ?       modelId        : "null")).append(", ")
							.append("'").append(StringEscapeUtils.escapeSql(displayName)).append("', ")
							.append( (linkType.equalsIgnoreCase(ARTICLE_LINK)                								 ? "'" + value    + "'" : "null")).append(", ")
							.append( (linkType.equalsIgnoreCase(SECTION_LINK)                								 ?       value          : "null")).append(", ")
							.append( (linkType.equalsIgnoreCase(URL_LINK)                    								 ? "'" + value    + "'" : "null")).append(", ")
							.append( (linkType.equalsIgnoreCase(CATEGORY_LINK)               								 ?       value          : "null")).append(", ")
							.append(newWindow).append(", ")
							.append(sort).append(", ")
							.append("1").toString();		
		
		final String sql = String.format(INSERT_DAILYTOPIC, values);
		
		if (_log.isDebugEnabled())
			_log.debug(new StringBuilder("Querty to delete dailytopics:\n").append(sql));
		PortalLocalServiceUtil.executeUpdateQuery(sql);
	}
	
	private static void insertDailyTopicForSectionWithNoDailyTopics(String groupId, String layoutId) throws IOException, SQLException{
		_log.trace("In insertDaylyTopicForSectionWithNoDailyTopics");
		
		String values = new StringBuilder("'").append(PortalUUIDUtil.newUUID()).append("', ")
							.append(groupId).append(", ")
							.append( (Validator.isNull(layoutId) || "0".equals(layoutId) ? "null" : layoutId) ).append(", ")
							.append("null, ")
							.append("'', ") // El texto del link no puede ser nulo)
							.append("null, ")
		                    .append("null, ")
	    				    .append("null, ")
						    .append("null, ")
						    .append(DEFAULT_NEW_WINDOW).append(", ")
						    .append(DEFAULT_SORT).append(", ")
						    .append(NO_DAILYTOPICS).toString();
		
		final String sql = String.format(INSERT_DAILYTOPIC, values); 
		
		if(_log.isDebugEnabled())
			_log.debug("Querty to insert a dailytopics with not dailytopics for the section:\n" + sql);
		
		PortalLocalServiceUtil.executeUpdateQuery(sql);
	}
	
	// Borra los dailytopics del grupo y seccion dados
	private static void deleteDailyTopics(String groupId, String layoutId) throws ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException{
		_log.trace("In deleteDailyTopics");
		
		ErrorRaiser.throwIfFalse(Validator.isNotNull(groupId), IterErrorKeys.XYZ_E_INVALIDARG_ZYX, "groupId is null or empty");
		
		// Obtenemos los ids de los dailytopics que se van a mover a daylytopicdeleted	
		String sql = String.format(DAILYTOPICS_TO_BE_MOVED_TO_DELETED, 
								    groupId, 
			                       (Validator.isNull(layoutId) || "0".equals(layoutId) ? " is null " : (new StringBuilder(" = ").append(layoutId))) );
		final Document result = PortalLocalServiceUtil.executeQueryAsDom(sql);
			
		if (null != result){			
			final List<Node> dailyTopics = result.getRootElement().selectNodes("/rs/row");
			
			if(null != dailyTopics && dailyTopics.size() > 0){
				final int size = dailyTopics.size();		
				
				if (_log.isDebugEnabled())
					_log.debug(new StringBuilder("They are ").append(size).append(" dailytopics to delete"));
				
				// Componemos los values
				StringBuilder values = new StringBuilder();	
				for (int i = 0; i < size; i++){					
					final String auxId = XMLHelper.getTextValueOf(dailyTopics.get(i), "@daylytopicid");
					final String layoutId2 = XMLHelper.getTextValueOf(dailyTopics.get(i), "@layoutid");
					values.append("('" + auxId + "', " + groupId + ", " + (Validator.isNotNull(layoutId2) ? layoutId2 : "null") + ")");					
					if (i < size - 1){
						values.append(", ");
					}					
				}
				
				sql = String.format(INSERT_DAILYTOPICDELETED, values);
				
				if (_log.isDebugEnabled())
					_log.debug(new StringBuilder("Query to write dailytopicdeleted").append(sql));
				
				PortalLocalServiceUtil.executeUpdateQuery(sql);					
					
			}else{
				_log.debug("No dailytopics to be moved to daylytopicdeleted");
			}
		}
		
		// Borramos los dailytopics de la sección y grupo indicado
		sql = String.format(DELETE_DAILYTOPICS, 
						    groupId,
						    (Validator.isNull(layoutId) || "0".equals(layoutId) ? " is null " : (new StringBuilder(" = ").append(layoutId))) );
		
		if (_log.isDebugEnabled())
			_log.debug(new StringBuilder("Querty to delete dailytopics:\n").append(sql));
		
		PortalLocalServiceUtil.executeUpdateQuery(sql);		
	}	
}