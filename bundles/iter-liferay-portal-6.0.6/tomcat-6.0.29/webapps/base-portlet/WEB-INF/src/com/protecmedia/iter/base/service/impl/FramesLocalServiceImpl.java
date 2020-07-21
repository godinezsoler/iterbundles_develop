package com.protecmedia.iter.base.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.util.Base64;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.ImageFramesUtil;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.documentlibrary.NoSuchFolderException;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.model.DLFolderConstants;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFolderLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.persistence.DLFileEntryUtil;
import com.protecmedia.iter.base.service.base.FramesLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.IterFileUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.util.CacheRefresh;
import com.protecmedia.iter.xmlio.service.util.TomcatUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

/**
 * The implementation of the frames local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.FramesLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.FramesLocalServiceUtil} to access the frames local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.FramesLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.FramesLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class FramesLocalServiceImpl extends FramesLocalServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(FramesLocalServiceImpl.class);

	private static Lock writeLock = new ReentrantReadWriteLock().writeLock();

	private static final String XPATH 					= "%s/param";
	
	//Frames	
	private static final String FRAMES					= "frames";
	
	private static final String GET_FRAME_GROUP			= "SELECT groupid from imageframe WHERE imageframeid = '%s'";
	
	private static final String ADD_FRAME 				= new StringBuilder(
			"INSERT INTO imageframe (imageframeid, name, width, height, placemode, oversize,				\n").append(
			"						 backcolor, watermark, lazyload, css, groupid, modifieddate				\n").append(
			"						)																		\n").append(
			"	VALUES 	('%s', '%s', %s, %s, %s, %s,														\n").append(
			"			 %s, %s, %s, %s, %s, '%s')															\n").toString();
	
	private static final String UPDATE_FRAME 				= new StringBuilder(
	"UPDATE imageframe SET name	= '%s', width = %s, height = %s,  placemode = %s, oversize = %s,			\n").append(
	"					   backcolor = %s,  watermark = %s, lazyload = %s, css = %s,  modifieddate	= '%s'	\n").append(
	"WHERE imageframeid = '%s'																				\n").toString();
	
	
	private static final String UPDATE_FRAME_PUBLISH	= new StringBuilder(	"UPDATE imageframe SET publicationdate = '%s' WHERE imageframeid IN %s"	).toString();
	
	private static final String DELETE_FRAME 			= new StringBuilder(	"DELETE FROM imageframe WHERE imageframeid = '%s'"						).toString();
	
	private static final String ADD_DELETE_FRAME 		= new StringBuilder(	"INSERT INTO imageframedeleted (imageframeid, groupid) " 				).append(
																				"VALUES ('%1$s', (SELECT groupid "										).append(
																				"FROM imageframe WHERE imageframeid='%1$s'))"							).toString();
	
	private static final String GET_FRAME 				= new StringBuilder(
		"SELECT imageframeid, groupid, publicationdate	\n").append(
		"FROM imageframe								\n").append(
		"	WHERE imageframeid = '%s'					\n").toString();
	
	private static final String DEL_FRAME				= "DELETE FROM imageframe WHERE imageframeid IN ('%s')";
			
	private static final String CHECK_CONFIGURED_FRAME 	= new StringBuilder(
		"SELECT ExtractValue(Layout.name, '/root/name[1]/text()') label, 'rsslayout' class	\n").append(
		"FROM SectionProperties																\n").append(
		"INNER JOIN Layout ON Layout.plid = SectionProperties.plid							\n").append(
		"	WHERE autorssframe = '%1$s'														\n").append(
		"																					\n").append(	
		"UNION ALL																			\n").append( 
		"																					\n").append(	
		"SELECT NAME label, 'rssadvance' class 												\n").append(
		"FROM rssadvancedproperties															\n").append(
		"	WHERE imageframe = '%1$s'														\n").toString();
	
			
	// Sinónimos de los encuadres
	private static final String SYNONYMS 				= "synonyms";
	
	
	private static final String ADD_IMGFRAME_SYNONYM_VALUES = " \n (%s, '%s', '%s', %d, '%s'),";
	private static final String ADD_IMGFRAME_SYNONYM		= new StringBuilder(	
		"INSERT INTO ImageFrameSynonym (synonymId, synonymName, imageFrameName, groupId, modifiedDate) 		\n").append(
		"VALUES %s																							\n").toString();
	
	
	private static final String GET_SYNONYMS_BY_FRAME 	= new StringBuilder(
		"SELECT synonymname																	\n").append(
		"FROM ImageFrameSynonym																\n").append( 
		"INNER JOIN ImageFrame ON ( 	ImageFrame.name = ImageFrameSynonym.imageframename 	\n").append(
		"							AND ImageFrame.groupId = ImageFrameSynonym.groupId )	\n").append(
		"	WHERE ImageFrame.imageframeid = '%s'											\n").toString();
	
	/** Se insertan en ImageFrameSynonymDeleted los sinónimos que han sido publicados **/
	private static final String ADD_IMGFRAME_SYNONYM_DELETED = new StringBuilder(	
		"INSERT INTO ImageFrameSynonymDeleted(synonymid, groupid)	\n").append(
		"SELECT synonymid, groupid									\n").append(
		"FROM ImageFrameSynonym										\n").append(
		"	WHERE groupId = %d										\n").append(
		"		AND synonymname IN ('%s')							\n").append(
		"		AND publicationDate IS NOT NULL						\n").toString();

	/** Se eliminan los sinónimos **/
	private static final String DEL_IMGFRAME_SYNONYM = new StringBuilder(
		"DELETE FROM ImageFrameSynonym 	\n").append(
		"WHERE groupId = %d				\n").append(
		"	AND synonymname IN ('%s')	\n").toString();
	
	/** Se eliminan los sinónimos de un determinado ID de encuadre **/
	private static final String DEL_IMGFRAME_SYNONYM_BY_FRAME = new StringBuilder(
		"DELETE FROM ImageFrameSynonym											\n").append(
		"WHERE 0 < (SELECT COUNT(*)												\n").append( 
		"			FROM ImageFrame												\n").append(
		"			  WHERE ImageFrame.name = ImageFrameSynonym.imageframename	\n").append(
		"				AND ImageFrame.groupId = ImageFrameSynonym.groupId		\n").append(
		"				AND ImageFrame.imageframeid = '%s'						\n").append(
		"		   ) 															\n").toString();
	
	/** Se insertan en <i>ImageFrameSynonymDeleted</i> los sinónimos que han sido publicados, asociados a un <i>frameId</i>. **/
	private static final String ADD_IMGFRAME_SYNONYM_DELETED_BY_FRAME = new StringBuilder(
		"INSERT INTO ImageFrameSynonymDeleted(synonymid, groupid)								\n").append(		
		"SELECT synonymid, ImageFrameSynonym.groupid											\n").append(
		"FROM ImageFrameSynonym														 			\n").append(
		"INNER JOIN ImageFrame ON (			ImageFrame.name = ImageFrameSynonym.imageframename	\n").append(
		"							AND 	ImageFrame.groupId = ImageFrameSynonym.groupId )	\n").append(
		"	WHERE ImageFrame.imageframeid = '%s'												\n").append(
		"		AND ImageFrameSynonym.publicationDate IS NOT NULL								\n").toString();	
	
	private static final String UPDATE_SYNONYMS_PUBLISH	= new StringBuilder(	
		"UPDATE imageframesynonym SET publicationdate = '%s'	\n").append(
		"	WHERE synonymid IN %s								\n").toString();

	
	// Alternatives types	
	private static final String ALTERNATIVES 			= "alternatives";
	private static final String ADD_ALTERNATIVE_CONTENT = new StringBuilder(	
		"INSERT INTO TypConAlternative(typConAlternativeId, NAME, typConAlternativeName, modifiedDate, delegationId) 		\n").append( 
		"	VALUES(%s, '%s', '%s', '%s', %d)																				\n").append( 
		"ON DUPLICATE KEY UPDATE typConAlternativeName = VALUES(typConAlternativeName), modifiedDate = VALUES(modifiedDate)	\n").toString(); 
	
	private static final String DEL_ALTERNATIVE_CONTENT = new StringBuilder(
		"DELETE FROM TypConAlternative 	\n").append(
		"WHERE delegationId = %d		\n").append(
		"	AND NAME = '%s'				\n").toString();
			
	private static final String ADD_ALTERNATIVE_CONTENT_DELETED = new StringBuilder(	
		"INSERT INTO TypConAlternativeDeleted(typConAlternativeId, delegationId)	\n").append(
		"SELECT typConAlternativeId, delegationId									\n").append(
		"FROM TypConAlternative														\n").append(
		"	WHERE delegationId = %d													\n").append(
		"		AND NAME = '%s'														\n").append(
		"		AND publicationDate IS NOT NULL										\n").toString();

	private static final String UPDATE_ALT_PUBLISH		= new StringBuilder(	
		"UPDATE typconalternative SET publicationdate = '%s'\n").append(
		"	WHERE typconalternativeid IN %s					\n").toString();
	
	// Watermark	
	private static final String WATERMARK	 							= "watermark";
	
	private static final String INSERT_OR_UPDATE_WATERMARK_MODIFIEDDATE = new StringBuilder(	"INSERT INTO Base_Communities (id_, groupId, " 				).append(
																								"watermarkmodifieddate)  VALUES ( %s, %s, '%3$s') "	 		).append(
																								"ON DUPLICATE KEY UPDATE watermarkmodifieddate='%3$s'"		).toString();
	
	private static final String UPDATE_WATERMARK_PUBLISH				= new StringBuilder(	
		"UPDATE Base_Communities SET watermarkpublicationdate = '%s'	\n").append(
		"	WHERE groupId=%s											\n").toString();
	
	//Publish	
	private static final String TO_PUBLISH_FRAMES		= new StringBuilder(	
		"SELECT i.*, FALSE deleted 																							\n").append(
		"FROM imageframe i 																									\n").append(
		"	WHERE i.groupid=%1$s 																							\n").append(
		"		AND (i.publicationdate IS NULL OR (i.publicationdate IS NOT NULL AND i.modifieddate > i.publicationdate)) 	\n").append(
		"		AND i.imageframeid NOT IN (	SELECT ifd.imageframeid 														\n").append(
		"									FROM imageframedeleted ifd 														\n").append(
		"										WHERE i.groupid = ifd.groupid												\n").append(
		"								  ) 																				\n").append(
		"																													\n").append(
		"UNION ALL 																											\n").append(
		"																													\n").append(
		"SELECT ifd2.imageframeid, NULL name, 0 width, 0 height, NULL placemode, 0 oversize, NULL backcolor, NULL watermark,\n").append(
		"		0 lazyload, NULL css, 0 groupid, NULL modifieddate, NULL publicationdate, TRUE deleted						\n").append(
		"FROM imageframedeleted ifd2																						\n").append(
		"	WHERE ifd2.groupid=%1$s																							\n").toString();
	
	private static final String TO_PUBLISH_FRAMES_SYNONYMS	= new StringBuilder(
		"SELECT synonymid, synonymname, imageframename, FALSE deleted			\n").append( 						
		"FROM ImageFrameSynonym													\n").append( 																
		"	WHERE (publicationdate IS NULL OR modifieddate > publicationdate)	\n").append( 	
		"	  AND groupid = %1$d												\n").append(															
		"																		\n").append(																					
		"UNION ALL																\n").append( 																				
		"																		\n").append(																					
		"SELECT synonymid, NULL synonymname, NULL imageframename, TRUE deleted	\n").append( 				
		"FROM ImageFrameSynonymDeleted											\n").append(												
		"	WHERE groupid=%1$d													\n").toString();	

	private static final String TO_PUBLISH_ALTERNATIVES	= new StringBuilder(	
		"SELECT typconalternativeid, name, typconalternativename, delegationid, FALSE deleted 			\n").append(								
		"FROM typconalternative t 																		\n").append(										
		"	WHERE (t.publicationdate IS NULL OR	t.publicationdate < t.modifieddate)						\n").append( 	
		"		  AND t.delegationid = %1$d 															\n").append(													
		"																								\n").append(																						
		"UNION ALL 																						\n").append(																			
		"																								\n").append(																						
		"SELECT typconalternativeid, NULL name, NULL typconalternativename, delegationid, TRUE deleted 	\n").append( 
		"FROM typconalternativedeleted																	\n").append( 		
		"	WHERE delegationid=%1$d																		\n").toString();
	
	private static final String HAS_TO_PUBLISH_WATERMAK	= new StringBuilder(	
		"SELECT * 																									\n").append(
		"FROM Base_Communities 																						\n").append(
		"	WHERE watermarkmodifieddate IS NOT NULL 																\n").append(
		"		AND groupid = %s 																					\n").append(
		"		AND ((watermarkpublicationdate IS NULL) 															\n").append(
		"			OR (watermarkpublicationdate IS NOT NULL AND watermarkmodifieddate > watermarkpublicationdate))	\n").toString();
	
	private static final String DELETE_MULT_ROWS = "DELETE FROM %s WHERE %s IN %s";
	
	
	public String addOrUpdate(long groupId, String frameId, String xml, boolean invalidateModel) throws Exception
	{
		ErrorRaiser.throwIfNull(xml, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		if (groupId <= 0)
		{
			// Si NO existe el grupo hay que obtenerlo a partir del frameId, pero NO se puede obtener el grupo a partir del frameId
			// durante una importación ya que aún NO se ha importado
			ErrorRaiser.throwIfFalse(Validator.isNotNull(frameId) , IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			
			groupId = XMLHelper.getLongValueOf(PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_FRAME_GROUP,frameId)), "/rs/row/@groupid");
			ErrorRaiser.throwIfFalse(groupId > 0);
		}

		Node param = SAXReaderUtil.createXPath("root/param").selectSingleNode((SAXReaderUtil.read(xml)));

		String frameName = "";
		String name 	 = XMLHelper.getTextValueOf(param, "@name");
		ErrorRaiser.throwIfNull(name, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String width 	 = XMLHelper.getTextValueOf(param, "@width");
		ErrorRaiser.throwIfNull(width, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String height 	 = XMLHelper.getTextValueOf(param, "@height");
		ErrorRaiser.throwIfNull(height, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String oversize  = XMLHelper.getTextValueOf(param, "@oversize");
		String lazyload  = XMLHelper.getTextValueOf(param, "@lazyload");
		String placemode = StringUtil.apostrophe( XMLHelper.getTextValueOf(param, "@placemode") );
		String backcolor = StringUtil.apostrophe( XMLHelper.getTextValueOf(param, "@backcolor") );
		String watermark = StringUtil.apostrophe( XMLHelper.getTextValueOf(param, "@watermark") );
		String css 		 = StringUtil.apostrophe( XMLHelper.getTextValueOf(param, "@css") );
		String publicationDate = XMLHelper.getTextValueOf(param, "@publicationdate");

		String[] synonyms 			= null;
		List<Object> currentSynonyms= null;
		
		if( name.contains(StringPool.COMMA) )
		{
			synonyms = name.split(StringPool.COMMA);
			ErrorRaiser.throwIfFalse( synonyms.length>0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			frameName = synonyms[0];
			
			ErrorRaiser.throwIfFalse( Validator.isNotNull(frameName), IterErrorKeys.XYZ_E_INVALIDARG_ZYX );
		}
		else
		{
			frameName = name;
		}
		
		if (Validator.isNull(frameId))
		{
			// Creación de un encuadre
			frameId = PortalUUIDUtil.newUUID();
			
			PortalLocalServiceUtil.executeUpdateQuery( String.format(ADD_FRAME, frameId, frameName, width, height, placemode, oversize, 
					   backcolor, watermark, lazyload, css, groupId, SQLQueries.getCurrentDate()));
			
		}
		else if (invalidateModel)
		{
			// Tiene frameId y es necesario invalidar el modelo (actualización de un encuadre)
			currentSynonyms = PortalLocalServiceUtil.executeQueryAsList( String.format(GET_SYNONYMS_BY_FRAME, frameId) );
			
			PortalLocalServiceUtil.executeUpdateQuery( String.format(UPDATE_FRAME, frameName, width, height, placemode, oversize, 
					   backcolor, watermark, lazyload, css, SQLQueries.getCurrentDate(), frameId));
			
			
		}
		else
		{
			if( Validator.isNotNull(publicationDate) )
			{
				// Actualización de un encuadre al publicarlo en el live.
				PortalLocalServiceUtil.executeUpdateQuery( String.format(UPDATE_FRAME, frameName, width, height, placemode, oversize, 
						   backcolor, watermark, lazyload, css, SQLQueries.getCurrentDate(), frameId));
			}
			else
			{
				// Creación de un encuadre al publicarlo en el live.
				PortalLocalServiceUtil.executeUpdateQuery( String.format(ADD_FRAME, frameId, frameName, width, height, placemode, oversize, 
						   backcolor, watermark, lazyload, css, groupId, SQLQueries.getCurrentDate()));
			}
		}
			
		
		if (invalidateModel)
		{
			updateImageFrameSynonyms(groupId, frameName, synonyms, currentSynonyms);
			ImageFramesUtil.loadImageFrames();
		}

		return frameId;
	}
	
	/**
	 * Se <b>ASUME</b> que este método <b>SOLO</b> será llamado para los encuadres, no para los tipos de contenido
	 * 
	 * @param groupId
	 * @param frameName
	 * @param synonyms
	 * @param currentAltNames
	 * @param updtTypConAltName
	 * @throws Exception
	 */
	private void updateImageFrameSynonyms(long groupId, String frameName, String[] synonyms, List<Object> currentSynonyms) throws Exception
	{
		boolean needUpdtSynonyms = false;
		String currentDate = SQLQueries.getCurrentDate();
		
		String[] newsynonyms = getNewsSynonyms(synonyms, currentSynonyms);
		
		// Se añaden los sinónimos nuevos
		StringBuilder insertValues = new StringBuilder();
		for (int i = 1; newsynonyms != null && i < newsynonyms.length; i++)
		{
			String synonym = StringUtils.trim(newsynonyms[i]);
			
			if ( Validator.isNotNull(synonym) )
			{
				insertValues.append( String.format(ADD_IMGFRAME_SYNONYM_VALUES, "ITR_UUID()", synonym, frameName, groupId, currentDate) );
			}
		}
		
		if (insertValues.length() > 0)
		{
			insertValues.deleteCharAt(insertValues.length()-1);
			PortalLocalServiceUtil.executeUpdateQuery( String.format(ADD_IMGFRAME_SYNONYM, insertValues.toString()) );
			
			needUpdtSynonyms = true;
		}
		
		
		// Se detectan cuántos sinónimos hay que borrar
		List<String> synonymsToDel = new ArrayList<String>();
		for (int iCurrentSynonym = 0; currentSynonyms != null && iCurrentSynonym < currentSynonyms.size(); iCurrentSynonym++)
		{
			String currentSynonym = String.valueOf(currentSynonyms.get(iCurrentSynonym));
			boolean delCurrentSynonym = true;
			
			for (int i = 1; synonyms != null && i < synonyms.length; i++)
			{
				if ( currentSynonym.equalsIgnoreCase(StringUtils.trim(synonyms[i])) )
				{
					delCurrentSynonym = false;
					break;
				}
			}
			
			if (delCurrentSynonym)
				synonymsToDel.add(currentSynonym);
		}
		
		if (!synonymsToDel.isEmpty())
		{
			String ids = StringUtil.merge(synonymsToDel, "','");
			
			// Se insertan en ImageFrameSynonymDeleted los sinónimos que han sido publicados
			PortalLocalServiceUtil.executeUpdateQuery( String.format(ADD_IMGFRAME_SYNONYM_DELETED, groupId, ids) );
			
			// Se eliminan los sinónimos
			PortalLocalServiceUtil.executeUpdateQuery( String.format(DEL_IMGFRAME_SYNONYM, groupId, ids) );
			
			needUpdtSynonyms = true;
		}

		if (needUpdtSynonyms)
			ImageFramesUtil.loadFrameSynonyms();
	}
	
	//De todos los sinonimos a añadir, elimina los que ya están añadidos y retorna un array con los sinónimos nuevos. 
	private String[] getNewsSynonyms(String[] synonyms, List<Object> currentSynonyms)
	{
		String[] newsynonyms = synonyms.clone();
		
		if(currentSynonyms !=null)
		{
			for(int i = 1; i < synonyms.length; i++)
			{
				String syn = StringUtils.trim(synonyms[i]);
				Boolean syn_found = false;
				
				for(int j = 0; j < currentSynonyms.size() && !syn_found; j++)
				{
					if(String.valueOf(currentSynonyms.get(j)).equalsIgnoreCase(syn))
					{
						newsynonyms = ArrayUtil.remove(newsynonyms, syn);
						syn_found = true;
					}
						
				}
			}
		}
		
		return newsynonyms;
	}

	public String deleteFrame(String frameId, boolean invalidateModel) throws Exception
	{
		ErrorRaiser.throwIfNull(frameId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		// Se obtienen los datos del encuadre a borrar
		Document dom = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_FRAME, frameId) );
		if ( Validator.isNotNull(XMLHelper.getTextValueOf(dom, "/rs/row/@publicationdate")) )
		{
			// El encuadre ha sido publicado
			// Se comprueba que NO esté configurado en el LIVE
			checkConfiguredFramesInLive(frameId);
			
			// Se inserta en la tabla de elementos pendientes de borrar
			PortalLocalServiceUtil.executeUpdateQuery(String.format(ADD_DELETE_FRAME, frameId));
		}

		deleteFrameSynonyms(frameId);
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_FRAME, frameId));

		if(invalidateModel)
			ImageFramesUtil.loadImageFrames();

		return frameId;
	}
	
	/**
	 * Se comprueba que el encuadre NO esté configurado en el LIVE ni en RSS por layouts ni en RSS avanzados
	 * 
	 * @param frameId
	 * @throws NoSuchMethodException 
	 * @throws IOException 
	 * @throws DocumentException 
	 * @throws ClientProtocolException 
	 * @throws SecurityException 
	 * @throws SystemException 
	 * @throws JSONException 
	 * @throws ServiceError 
	 */
	private void checkConfiguredFramesInLive(String frameId) throws JSONException, SystemException, SecurityException, ClientProtocolException, DocumentException, IOException, NoSuchMethodException, ServiceError
	{
		Document remoteDom = computeFrameDependencies(frameId);
		
		String rssLayouts  = StringUtils.join( XMLHelper.getStringValues(remoteDom.selectNodes("/rs/row[@class = 'rsslayout']/@label")),  "\n" );
		ErrorRaiser.throwIfFalse( Validator.isNull(rssLayouts), IterErrorKeys.XYZ_E_RSSLAYOUT_IMGFRAME_CONFIGURED_ZYX,   rssLayouts);
		
		String rssAdvance  = StringUtils.join( XMLHelper.getStringValues(remoteDom.selectNodes("/rs/row[@class = 'rssadvance']/@label")), "\n" );
		ErrorRaiser.throwIfFalse( Validator.isNull(rssAdvance), IterErrorKeys.XYZ_E_RSSADVANCED_IMGFRAME_CONFIGURED_ZYX, rssAdvance);
	}
	
	public Document computeFrameDependencies(String frameId) throws JSONException, SystemException, SecurityException, ClientProtocolException, DocumentException, IOException, NoSuchMethodException, ServiceError
	{
		Document dom = PortalLocalServiceUtil.executeRemoteQueryAsDom( String.format(CHECK_CONFIGURED_FRAME, frameId) );
		return dom;
	}
	
	private void deleteFrameSynonyms(String frameId) throws SecurityException, NoSuchMethodException, IOException, SQLException
	{
		//  Se insertan en <i>ImageFrameSynonymDeleted</i> los sinónimos que han sido publicados, asociados a un <i>frameId</i>.  
		PortalLocalServiceUtil.executeUpdateQuery( String.format(ADD_IMGFRAME_SYNONYM_DELETED_BY_FRAME, frameId) );
		
		// Se eliminan los sinónimos de un determinado ID de encuadre
		PortalLocalServiceUtil.executeUpdateQuery( String.format(DEL_IMGFRAME_SYNONYM_BY_FRAME, frameId) );
	}

	public void deleteMultipleRows(List<Node> ids, String tableName, String columnIdName) throws IOException, SQLException
	{
		if(ids != null && ids.size() > 0)
		{
			PortalLocalServiceUtil.executeUpdateQuery(
					String.format(DELETE_MULT_ROWS, 
							tableName, columnIdName, getInClauseSQL(ids, columnIdName)));
		}
	}

	/**
	 * Se <b>ASUME</b> que este método <b>SOLO</b> será llamado para los los tipos de contenido de sustitución, no para encuadres de sustitución.<br/>
	 * 
	 * Establece un contenido de sustitución: (tipo de contenido original, tipo de contenido de reemplazo, recargar en memoria los tipos de contenidos de reemplazo) 
	 */
	public void setReplacementContentType(String nameOriginal, String nameReplacement, boolean invalidateModel)	throws Exception
	{
		setReplacementContentType(0, nameOriginal, nameReplacement, invalidateModel);
	}
	
	public void setReplacementContentType(long delegationId, String nameOriginal, String nameReplacement, boolean invalidateModel)	throws Exception
	{
		ErrorRaiser.throwIfNull(nameOriginal, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		if(Validator.isNotNull(nameReplacement))
		{
			if(PropsValues.IS_PREVIEW_ENVIRONMENT)
				checkLoops(delegationId, nameOriginal, nameReplacement);
				
			String sql = String.format(ADD_ALTERNATIVE_CONTENT, "ITR_UUID()", nameOriginal, nameReplacement, SQLQueries.getCurrentDate(), delegationId);
			PortalLocalServiceUtil.executeUpdateQuery(sql);
		}
		else
		{
			// Se insertan en TypConAlternativeDeleted los contenidos de sustitución que han sido publicados
			String sql = String.format(ADD_ALTERNATIVE_CONTENT_DELETED, delegationId, nameOriginal);
			PortalLocalServiceUtil.executeUpdateQuery(sql);
			
			// Se eliminan los contenidos de sustitución de TypConAlternative
			PortalLocalServiceUtil.executeUpdateQuery( String.format(DEL_ALTERNATIVE_CONTENT, delegationId, nameOriginal) );
		}
		
		// Recargar el modelo de tipos alternativos
		if (invalidateModel)	
			ImageFramesUtil.loadAlternativeTypes();
	}
	
	private void checkLoops(long delegationId, String nameOriginal, String nameReplacement) throws ServiceError
	{
		Map<String, String> alternativeTypes = ImageFramesUtil.getAlternativeTypes(delegationId);
		if(Validator.isNotNull(alternativeTypes))
		{
			List<String> alternativeTypesList = new ArrayList<String>();
			alternativeTypesList.add(nameOriginal);
			alternativeTypesList.add(nameReplacement);
			String alternativeType = alternativeTypes.get(nameReplacement);
			while(Validator.isNotNull(alternativeType))
			{
				ErrorRaiser.throwIfFalse( !alternativeTypesList.contains(alternativeType), 
										IterErrorKeys.XYZ_E_ALTERNATIVE_CONTENT_TYPE_LOOP_ZYX, 
										StringUtil.merge(alternativeTypesList, StringPool.COMMA_AND_SPACE).concat(StringPool.COMMA_AND_SPACE).concat(alternativeType) );
				
				alternativeTypesList.add(alternativeType);
				alternativeType = alternativeTypes.get(alternativeType);
			}
		}
	}

	// Establece la marca de agua.
	public void setGroupWaterMark(long groupId, byte[] bytes, boolean invalidateModel) throws Exception
	{
		ErrorRaiser.throwIfNull(bytes, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		long userId 		= GroupMgr.getDefaultUserId();
		String extension 	= IterFileUtil.getExtensionFromBytes(bytes);
		String title 		= PortalUUIDUtil.newUUID() + extension;

		//DLFolder
	    DLFolder dlFolder = null;

	    try
	    {
	    	dlFolder = DLFolderLocalServiceUtil.getFolder(
	    			groupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, ImageFramesUtil.WATERMARK_FOLDER);
	    }
	    catch (NoSuchFolderException nsfe)
	    {
	    	dlFolder = DLFolderLocalServiceUtil.addFolder(
	    			userId, groupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, ImageFramesUtil.WATERMARK_FOLDER, StringPool.BLANK, new ServiceContext());
	    }
	    
	    ErrorRaiser.throwIfNull(dlFolder);

	    long folderId = dlFolder.getFolderId();
		List<DLFileEntry>imgList = DLFileEntryLocalServiceUtil.getFileEntries(groupId, folderId);
		
		//DLFileEntry
		DLFileEntry dlfileEntry = null;
		if(imgList.size() > 0)
		{
			DLFileEntry oldDlfileEntry = imgList.get(0);
			dlfileEntry = DLFileEntryLocalServiceUtil.addFileEntry(userId, groupId, folderId, title, title, StringPool.BLANK,
																   StringPool.BLANK, null, bytes, new ServiceContext());
			
			DLFileEntryLocalServiceUtil.deleteDLFileEntry(oldDlfileEntry);
		}
		else
		{
			dlfileEntry = DLFileEntryLocalServiceUtil.addFileEntry(userId, groupId, folderId, title, title, StringPool.BLANK, 
																   StringPool.BLANK, null, bytes, new ServiceContext());
		}

		DLFileEntryUtil.update(dlfileEntry, false);
		
		PortalLocalServiceUtil.executeUpdateQuery(String.format(INSERT_OR_UPDATE_WATERMARK_MODIFIEDDATE,
												  CounterLocalServiceUtil.increment(), groupId, SQLQueries.getCurrentDate()));
		
		if(invalidateModel)
			ImageFramesUtil.loadWatermarks();
	}
	
	
	// Publicacion de frames, watermarks y contenidos de sustitución
	public void publishToLive(long delegationId, long groupId, String xml) throws ServiceError, ClientProtocolException, SystemException, IOException, SecurityException, PortalException, DocumentException, NoSuchMethodException, SQLException 
	{
		ErrorRaiser.throwIfNull(xml, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		if(writeLock.tryLock())
		{
			try
			{
				ErrorRaiser.throwIfNull(xml, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
				Document dom = SAXReaderUtil.read(xml);

				Element exportXML = generateExportElement(delegationId, groupId, dom);
				// Comprueba que hay algo que publicar
				checkEmptyPublication(exportXML);
				
				executeJSONRemoteCalls(groupId, exportXML, dom);
				
				updatePublicationDateContents(groupId, exportXML);
			}
			finally
			{
				writeLock.unlock();
			}
		}
		else
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_PUBLISH_ALREADY_IN_PROCESS_ZYX);
		}
	}
	
	private void executeJSONRemoteCalls(long groupId, Element exportXML, Document dom) throws ClientProtocolException, IOException, SystemException, PortalException, DocumentException, ServiceError
	{
		String scopeGroupName = (groupId > 0) ? GroupLocalServiceUtil.getGroup(groupId).getName() : "";
		LiveConfiguration liveConf = LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(GroupMgr.getCompanyId());
		
		List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
		remoteMethodParams.add(new BasicNameValuePair("serviceClassName",	"com.protecmedia.iter.base.service.FramesServiceUtil"));
		remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"importContents"));
		remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[scopeGroupName, exportxml]"));
		remoteMethodParams.add(new BasicNameValuePair("scopeGroupName", 	scopeGroupName));
		remoteMethodParams.add(new BasicNameValuePair("exportxml", 			exportXML.asXML()));
		
		String []url = liveConf.getRemoteIterServer2().split(":");
		HttpHost targetHost = new HttpHost(url[0], Integer.valueOf(url[1]));
		JSONObject json = JSONUtil.executeMethod(targetHost, "/base-portlet/secure/json", remoteMethodParams, 
									(int)liveConf.getConnectionTimeOut(),
									(int)liveConf.getOperationTimeOut(),
									liveConf.getRemoteUserName(), liveConf.getRemoteUserPassword());

		// Actualiza el mapa en memoria, la caché remota y notifica a las URLptes
		boolean refreshCache = GetterUtil.getBoolean(XMLHelper.getStringValueOf(dom.getRootElement(), "@refreshCache"), true);
		if (refreshCache)
		{
			boolean refreshImageFrames 		= XMLHelper.getLongValueOf(exportXML, String.format("count(%s)", getFramesXPath())) 		> 0;
			boolean refreshSynonyms			= XMLHelper.getLongValueOf(exportXML, String.format("count(%s)", getSynonymsXPath())) 		> 0;
			boolean refreshAlternativeTypes	= XMLHelper.getLongValueOf(exportXML, String.format("count(%s)", getAlternativesXPath())) 	> 0;
			boolean refreshWatermarks		= XMLHelper.getLongValueOf(exportXML, String.format("count(%s)", getWatermarkXPath())) 		> 0;
			
			
			// Se refrescará la caché si se le permite (atributo refreshCache) y se está publicando alguno de los elemntos que van por TPU
			boolean refreshTomcatAndApache	= (refreshImageFrames || refreshSynonyms || refreshWatermarks);

			Document returnDom = SAXReaderUtil.read(json.getString("returnValue"));
			String lastUpdate = XMLHelper.getStringValueOf( returnDom.getRootElement(), IterKeys.XMLIO_XML_LAST_UPDATE_ATTRIBUTE_ATTR, StringPool.BLANK);
			
			CacheRefresh cr = new CacheRefresh( (groupId > 0) ? groupId : GroupMgr.getGlobalGroupId(), lastUpdate);
			cr.setRefreshImageFrames(refreshImageFrames);
			cr.setRefreshFrameSynonyms(refreshSynonyms);
			cr.seRefreshAlternativeTypes(refreshAlternativeTypes);
			cr.setRefreshWatermarks(refreshWatermarks);
			cr.setRefreshTomcatAndApache(refreshTomcatAndApache);
			// Si invalida la caché de los Apaches será necesario replanificar las invalidaciones programadas
			cr.setRescheduleCacheInvalidation(refreshTomcatAndApache);
			
			XMLIOUtil.deleteRemoteCache(cr);
		}
	}
	
	// Funcion que mira que ha de publicarse (frames y/o contenidos de sustitucion y/o marcas de agua)
	private Element generateExportElement(long delegationId, long scopeGroupId, Document dom) 
			throws DocumentException, ServiceError, SecurityException, NoSuchMethodException, SystemException, PortalException, IOException
	{
		Element rs = SAXReaderUtil.read("<rs/>").getRootElement();
		Node param = dom.selectSingleNode("/root/param");
		
		boolean publishFrames 		= GetterUtil.getBoolean(XMLHelper.getTextValueOf(param, "@frames"), 	false);
		boolean publishWatermark 	= GetterUtil.getBoolean(XMLHelper.getTextValueOf(param, "@waterMark"), 	false);
		boolean publishContentTypes = GetterUtil.getBoolean(XMLHelper.getTextValueOf(param, "@replacementContentTypes"), false);
		
		if(publishFrames)
		{
			Element ele = PortalLocalServiceUtil.executeQueryAsDom(String.format(TO_PUBLISH_FRAMES, scopeGroupId), true, FRAMES, "param").getRootElement();
			rs.add(ele.detach());
			
			// Obtencion de los sinónimos.
			ele = PortalLocalServiceUtil.executeQueryAsDom( String.format(TO_PUBLISH_FRAMES_SYNONYMS, scopeGroupId), true, SYNONYMS, "param").getRootElement();
			rs.add(ele.detach());
		}
		
		if (publishContentTypes)
		{
			Element ele = PortalLocalServiceUtil.executeQueryAsDom( String.format(TO_PUBLISH_ALTERNATIVES, delegationId), true, ALTERNATIVES, "param").getRootElement();
			rs.add(ele.detach());
		}
		
		if (publishWatermark)
		{
			addWatermarkDataToXML(scopeGroupId, rs);
		}
		
		return rs;
	}
	
	private void addWatermarkDataToXML(long scopeGroupId, Element rs) throws PortalException, SystemException, IOException
	{
		List<Object> hasToPublish = PortalLocalServiceUtil.executeQueryAsList(String.format(HAS_TO_PUBLISH_WATERMAK, scopeGroupId));
		if(hasToPublish != null && hasToPublish.size() > 0)
		{
			InputStream is = null;
		    try
		    {
		    	DLFolder dlFolder = DLFolderLocalServiceUtil.getFolder(
		    			scopeGroupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, ImageFramesUtil.WATERMARK_FOLDER);
		    	
			    long folderId = dlFolder.getFolderId();
			    
				List<DLFileEntry>imgList = DLFileEntryLocalServiceUtil.getFileEntries(scopeGroupId, folderId);
				if(imgList.size() > 0)
				{
					DLFileEntry dlfileEntry = imgList.get(0);
					long delegationId = GroupLocalServiceUtil.getGroup(scopeGroupId).getDelegationId();
					is = DLFileEntryLocalServiceUtil.getFileAsStream(
							delegationId, dlfileEntry.getUserId(), scopeGroupId, folderId, dlfileEntry.getName());

					Element root = rs.addElement(WATERMARK);
					Element ele = root.addElement("param");
					ele.setText(Base64.encodeBase64String(IOUtils.toByteArray(is)));
				}
		    }
		    catch (NoSuchFolderException nsfe)
		    {
		    	_log.debug(nsfe);
		    }
		    finally
		    {
		    	if(is != null)
		    		is.close();
		    }
		}
	}
	
	// Realizar la importación de encuadres, marcas de agua o contenidos de sustitución en el LIVE llamado desde el BACK
	public Document importContents(String scopeGroupName, String xml) throws Exception
	{
		ErrorRaiser.throwIfNull(xml, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Element dataRoot 		= SAXReaderUtil.read(xml).getRootElement();
		List<Node> frames 		= dataRoot.selectNodes( getFramesXPath().concat("[@deleted!='1']") );
		List<Node> synonyms 	= dataRoot.selectNodes( getSynonymsXPath() );
		List<Node> alternatives = dataRoot.selectNodes( getAlternativesXPath() );
		Node watermark 			= dataRoot.selectSingleNode( getWatermarkXPath() );
		
		long scopeGroupId = 0;
		if (frames.size() > 0 || synonyms.size() > 0 || watermark != null)
		{
			// Estos elementos van por TPU
			scopeGroupId = GroupLocalServiceUtil.getGroup(IterGlobal.getCompanyId(), scopeGroupName).getGroupId();
		}
		
		Document dom = SAXReaderUtil.read("<rs/>");
				
		// Encuadres
		// No se pueden borrar previamente TODOS los encuadres porque podrían existir algunos configurados en los RSS, 
		// que pretenden ser actualizados, y que al borrar o salta un error o se perdería la referencia a dicho registro 
		// Se borran los indicados para borrar
		String frameIDs = StringUtils.join( XMLHelper.getStringValues(dataRoot.selectNodes( getFramesXPath().concat("[@deleted='1']/@imageframeid") )), "','" );
		if (Validator.isNotNull(frameIDs))
			PortalLocalServiceUtil.executeUpdateQuery( String.format(DEL_FRAME, frameIDs) );
		
		for(Node frame:frames)
		{
			Element framesRoot = SAXReaderUtil.read("<root/>").getRootElement();
			framesRoot.add(frame.detach());
			addOrUpdate(scopeGroupId, XMLHelper.getTextValueOf(frame, "@imageframeid"), framesRoot.asXML(), false);
		}
		
		
		// Sinónimos
		deleteMultipleRows(synonyms, "imageframesynonym", "synonymid");
		
		// Se añaden/actualizan los sinónimos nuevos 
		String currentDate 			= SQLQueries.getCurrentDate();
		StringBuilder insertValues 	= new StringBuilder();
		for (Node synonym:synonyms)
		{
			boolean toDelete = GetterUtil.getBoolean(XMLHelper.getTextValueOf(synonym, "@deleted"));
			if (!toDelete)
			{
				String 	synonymId 		= StringUtil.apostrophe(XMLHelper.getTextValueOf(synonym, "@synonymid"));
				String 	synonymName 	= XMLHelper.getTextValueOf(synonym, "@synonymname");
				String 	imageFrameName 	= XMLHelper.getTextValueOf(synonym, "@imageframename");
				
				insertValues.append( String.format(ADD_IMGFRAME_SYNONYM_VALUES, synonymId, synonymName, imageFrameName, scopeGroupId, currentDate) );
			}
		}
		
		if (insertValues.length() > 0)
		{
			insertValues.deleteCharAt(insertValues.length()-1);
			PortalLocalServiceUtil.executeUpdateQuery( String.format(ADD_IMGFRAME_SYNONYM, insertValues.toString()) );
		}

		
		// Contenidos de sustitución
		deleteMultipleRows(alternatives, "typconalternative", "typconalternativeid");
		for(Node alternative:alternatives)
		{
			boolean toDelete = GetterUtil.getBoolean(XMLHelper.getTextValueOf(alternative, "@deleted"));
			if (!toDelete)
			{
				String 	typconalternativeid = XMLHelper.getTextValueOf(alternative, "@typconalternativeid");
				String 	nameOriginal 		= XMLHelper.getTextValueOf(alternative, "@name");
				String 	nameReplacement 	= XMLHelper.getTextValueOf(alternative, "@typconalternativename");
				long 	delegationId		= XMLHelper.getLongValueOf(alternative, "@delegationid");
				
				String sql = String.format(ADD_ALTERNATIVE_CONTENT, StringUtil.apostrophe(typconalternativeid), nameOriginal, 
																	nameReplacement, SQLQueries.getCurrentDate(), delegationId);
				PortalLocalServiceUtil.executeUpdateQuery(sql);
			}
		}

		
		// Marca de agua
		if (watermark != null)
		{
			byte[] bytes = Base64.decodeBase64(watermark.getText());
			setGroupWaterMark(scopeGroupId, bytes, false);
		}
		
		// Se actualiza la fecha de última publicación pero NO se manda la anterior porque NO interesa
		// La fecha se utiliza para las newsletter y no se quiere accionar el mecanismo por publicar alguno 
		// de estos elementos
		// Se toma la fecha de última publicación y se actualiza dicho campo
		if (scopeGroupId > 0)
		{
			String lastUpdate = String.valueOf(GroupMgr.getPublicationDate(scopeGroupId).getTime());
			dom.getRootElement().addAttribute(IterKeys.XMLIO_XML_LAST_UPDATE_ATTRIBUTE, lastUpdate);
			
			TomcatUtil.updatePublicationDateNoException(IterGlobal.getCompanyId(), scopeGroupId);
		}
		
		return dom;
	}
	
	private String getFramesXPath()
	{
		return String.format(XPATH, FRAMES);
	}
	private String getSynonymsXPath()
	{
		return String.format(XPATH, SYNONYMS);
	}
	private String getAlternativesXPath()
	{
		return String.format(XPATH, ALTERNATIVES);
	}
	private String getWatermarkXPath()
	{
		return String.format(XPATH, WATERMARK);
	}

	
	
	public void checkEmptyPublication(Element element) throws ServiceError
	{
		long numElements = XMLHelper.getLongValueOf(element, "count(//param)");
		ErrorRaiser.throwIfFalse(numElements > 0, IterErrorKeys.XYZ_E_XPORTCONTENT_EMPTY_ZYX);
	}
	
	public void updatePublicationDateContents(long groupId, Element exportXML) throws IOException, SQLException, ServiceError
	{
		String xpath = getFramesXPath();
		List<Node> frames = exportXML.selectNodes(xpath);
		if(frames != null && frames.size() > 0)
		{
			PortalLocalServiceUtil.executeUpdateQuery(
					String.format(UPDATE_FRAME_PUBLISH, SQLQueries.getCurrentDate(), getInClauseSQL(frames, "imageframeid")));
			
			List<Node> deletedFrames = exportXML.selectNodes(xpath.concat("[@deleted='1']"));
			if(deletedFrames.size() > 0)
				deleteMultipleRows(frames, "imageframedeleted", "imageframeid");
		}
		
		xpath = getSynonymsXPath();
		List<Node> synonyms = exportXML.selectNodes(xpath);
		if (synonyms != null && synonyms.size() > 0)
		{
			PortalLocalServiceUtil.executeUpdateQuery(
					String.format(UPDATE_SYNONYMS_PUBLISH, SQLQueries.getCurrentDate(), getInClauseSQL(synonyms, "synonymid")));
			
			List<Node> deletedAlternatives = exportXML.selectNodes(xpath.concat("[@deleted='1']"));
			if(deletedAlternatives.size() > 0)
				deleteMultipleRows(synonyms, "ImageFrameSynonymDeleted", "synonymid");
		}

		xpath = getAlternativesXPath();
		List<Node> alternatives = exportXML.selectNodes(xpath);
		if(alternatives != null && alternatives.size() > 0)
		{
			PortalLocalServiceUtil.executeUpdateQuery(
					String.format(UPDATE_ALT_PUBLISH, SQLQueries.getCurrentDate(), getInClauseSQL(alternatives, "typconalternativeid")));
			
			List<Node> deletedAlternatives = exportXML.selectNodes(xpath.concat("[@deleted='1']"));
			if(deletedAlternatives.size() > 0)
				deleteMultipleRows(alternatives, "typconalternativedeleted", "typconalternativeid");
		}
		
		List<Node> watermark = exportXML.selectNodes(getWatermarkXPath());
		if(watermark != null && watermark.size() > 0)
		{
			PortalLocalServiceUtil.executeUpdateQuery(
					String.format(UPDATE_WATERMARK_PUBLISH, SQLQueries.getCurrentDate(), groupId));
		}
	}
	
	public String getInClauseSQL(List<Node> ids, String columnId)
	{
		StringBuffer query = new StringBuffer();
		
		if(ids != null && ids.size() > 0)
		{
			for(int i = 0; i < ids.size(); i++)
			{
				String currentId = XMLHelper.getTextValueOf(ids.get(i), "@" + columnId);
	
				if(i == 0)
				{
					query.append("('" + currentId + "'");
				}				
				if(i == ids.size() - 1)
				{
					if(ids.size() > 1)
					{
						query.append(", '" + currentId + "') ");
					}
					else
					{
						query.append(") ");
					}
				}
				if (i > 0 && i < ids.size() - 1)
				{
					query.append(", '" + currentId + "'");
				}
			}
		}
		else
		{
			query.append("('')");
		}
		return query.toString();
	}
	
	public String getFramesByGroup(long groupId) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse( groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		return PortalLocalServiceUtil.executeQueryAsDom(String.format("SELECT imageframeid, name FROM imageframe WHERE groupid = %d;", groupId) ).asXML();
	}
}