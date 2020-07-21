package com.protecmedia.iter.advertisement.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.JsonObject;
import com.liferay.cluster.ClusterAdMgr;
import com.liferay.cluster.ClusterMgr;
import com.liferay.cluster.IClusterMgr.ClusterMgrOperation;
import com.liferay.cluster.IClusterMgr.ClusterMgrType;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.advertisement.MetadataAdvertisementTools;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.kernel.zip.ZipWriter;
import com.liferay.portal.kernel.zip.ZipWriterFactoryUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.asset.model.AssetVocabularyConstants;
import com.liferay.portlet.documentlibrary.NoSuchFolderException;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.model.DLFolderConstants;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFolderLocalServiceUtil;
import com.protecmedia.iter.advertisement.service.base.AdvertisementMgrLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.ContextVarsMgrLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterAdmin;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.base.service.util.TeaserMgr;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.util.CDATAUtil;
import com.protecmedia.iter.xmlio.service.util.CacheRefresh;
import com.protecmedia.iter.xmlio.service.util.FTPUtil;
import com.protecmedia.iter.xmlio.service.util.PublishUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;
import com.protecmedia.iter.xmlio.service.util.ZipUtil;


/**
 * The implementation of the advertisement mgr local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.advertisement.service.AdvertisementMgrLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.advertisement.service.AdvertisementMgrLocalServiceUtil} to access the advertisement mgr local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author protec
 * @see com.protecmedia.iter.advertisement.service.base.AdvertisementMgrLocalServiceBaseImpl
 * @see com.protecmedia.iter.advertisement.service.AdvertisementMgrLocalServiceUtil
 */
@Transactional( isolation = Isolation.PORTAL, rollbackFor = { Exception.class } )
public class AdvertisementMgrLocalServiceImpl extends AdvertisementMgrLocalServiceBaseImpl
{
	
	private static Log _log = LogFactoryUtil.getLog(AdvertisementMgrLocalServiceImpl.class);
	
	//Sólo una publicación al mismo tiempo
	private static ReadWriteLock globalLock = new ReentrantReadWriteLock();
	private static Lock writeLock = globalLock.writeLock();
	
	private final String ASSIGNMENTS_TO_LAYOUT_TBL_NAME = "adslotadtags";
	private final String PLID_COLUMN_NAME = "plid";
	private final String ASSIGNMENTS_TO_CATEGORY_TBL_NAME = "adslottagcategory";
	private final String CATEGORYID_COLUMN_NAME = "categoryid";
	private final String TBL_ADVOCABULARY = "advocabulary";
	private final String ADVOCABULARYID_COLUMN_NAME = "advocabularyid";
	
	private final String ITER_ADVERTISEMENT_ZIP_FILE_NAME = IterKeys.XMLIO_ZIP_FILE_PREFIX + "advertisement_%s.zip";
	
	private final String GET_CATEGORY_FROM_PATH = new StringBuilder(
		"SELECT ITR_GET_CATEGORY_FROM_PATH_FUNC(%d,'%s','").append(StringPool.SECTION).append("') categoryId").toString();
	
	//Publish
	private final String SUBQUERY_GET_CONTENT_TO_PUBLISH = new StringBuilder(", IF(publicationdate IS NULL, '" + IterKeys.CREATE + "', '" + IterKeys.UPDATE + "') operation\n"	).append(
																 					"FROM (SELECT * FROM %s WHERE groupid=%s AND\n" 													).append(
																 					"IF(publicationdate IS NOT NULL AND modifieddate <= publicationdate, false, true)) query"			).toString();
	
	//To delete
	private final String ADD_TO_DELETE = "INSERT INTO addeleted(adid, adtable, groupid) SELECT %1$s, '%2$s', groupId FROM %2$s WHERE %1$s IN %3$s AND publicationdate IS NOT NULL";
	private final String GET_TO_DELETE = "SELECT adid, adtable FROM addeleted WHERE groupid=%s";
	private final String UPDATE_TO_DELETE_PUBLISHED =   "DELETE FROM addeleted WHERE adid IN %s";
	
	//References
	private final String SKIN  	= "skin";
	private final String TAG  	= "tag";
	private final String SLOT  	= "slot";
	
	private final String BBDD_FORMAT	= "yyy-MM-dd HH:mm";
	
	private final String GET_TAGS_REFERENCES = "SELECT %s FROM %s WHERE superid='%s'";
	private final String GET_SLOT_TAGS_REFERENCES = new StringBuilder(	
						"SELECT l.friendlyurl AS assignname, s.slotname, t.tagname, 'section' AS assigntype FROM adslotadtags st\n"		).append(
						"LEFT JOIN Layout l ON l.plid=st.plid\n"							).append(
						"INNER JOIN adslot s ON s.slotid=st.slotid\n"						).append(
						"INNER JOIN adtags t ON t.tagid=st.tagid WHERE %1$s='%2$s'\n"		).append(
						"UNION ALL\n"														).append(
						"SELECT c.name AS assignname, s.slotname, t.tagname, 'category' AS assigntype FROM adslottagcategory stc\n"		).append(
						"LEFT JOIN assetcategory c ON c.categoryId=stc.categoryid\n"		).append(
						"INNER JOIN adslot s ON s.slotid=stc.slotid\n"						).append(
						"INNER JOIN adtags t ON t.tagid=stc.tagid WHERE %1$s='%2$s'"		).toString();
						
	
	private final String GET_SLOT_SKINS_REFERENCES = new StringBuilder(	
						"SELECT l.friendlyurl AS assignname, s.slotname, k.imagename, 'section' AS assigntype FROM adslotadtags st\n"	).append(
						"LEFT JOIN Layout l ON l.plid=st.plid\n"								).append(
						"INNER JOIN adslot s ON s.slotid=st.slotid\n"							).append(
						"INNER JOIN adskin k ON k.skinid=st.skinid WHERE %1$s='%2$s'\n"			).append(
						"UNION ALL\n"															).append(	
						"SELECT c.name AS assignname, s.slotname, k.imagename, 'category' AS assigntype FROM adslottagcategory stc\n"	).append(
						"LEFT JOIN assetcategory c ON c.categoryId=stc.categoryid\n"			).append(		
						"INNER JOIN adslot s ON s.slotid=stc.slotid\n"							).append(
						"INNER JOIN adskin k ON k.skinid=stc.skinid WHERE %1$s='%2$s'"			).toString();	
	
	
	private final String GET_ROW_BY_ID = "SELECT * FROM %s WHERE %s='%s'";
	
	//Slots
	private final String GET_SLOTS_BY_GROUP = "SELECT slotid, slotname, slottype FROM adslot WHERE groupid = %s ORDER BY slotname ASC";
	private final String GET_SLOT_BY_SLOTID = "SELECT slotid, slotname, slottype, groupid FROM adslot WHERE slotid = '%s'";
	
	private final String ADD_SLOT = "INSERT INTO adslot(slotid, slotname, slottype, groupid, modifieddate, publicationdate) VALUES ('%s', '%s', '%s', %s, '%s', null)";
	private final String UPDATE_SLOT = "UPDATE adslot SET slotname='%s', slottype='%s', modifieddate='%s' WHERE slotid='%s'";
	private final String DELETE_SLOTS = "DELETE FROM adslot WHERE slotid IN %s";
	
	private final String GET_ALL_SLOTS = "SELECT slotid, slotname, slottype FROM adslot WHERE groupid = %d";
	private final String GET_SLOTS_TO_PUBLISH =  new StringBuilder("SELECT slotid, slotname, slottype").append(SUBQUERY_GET_CONTENT_TO_PUBLISH).toString();
	private final String UPDATE_SLOTS_PUBLISHED =   "UPDATE adslot SET publicationdate='%s' WHERE slotid IN %s";
	
	//Tags
	private final String GLOBAL = ClusterAdMgr.AD_TAGTYPE_GLOBAL;
	private final String HTML   = ClusterAdMgr.AD_TAGTYPE_HTML;
	private final String FLASH  = ClusterAdMgr.AD_TAGTYPE_FLASH;
	private final String IMAGE  = ClusterAdMgr.AD_TAGTYPE_IMAGE;
	
	private final String GET_TAGS_BY_GROUP = 
		new StringBuilder( "SELECT tags1.tagid, tags1.tagname, tags1.tagtype, tags1.tagscript, tags1.faketagscript, tags1.superid, tags2.tagname AS supername " ).
					append( " FROM adtags tags1 LEFT JOIN adtags tags2 ON (tags1.superid=tags2.tagid)" ).
					append( " WHERE tags1.groupid = %s ORDER BY tags1.tagname" ).toString();
	
	private final String GET_TAG_BY_TAGID = 
		new StringBuilder( "SELECT tags1.tagid, tags1.tagname, tags1.tagtype, tags1.tagscript, tags1.faketagscript, tags1.superid, tags2.tagname AS supername, tags1.groupid " ).
					append( " FROM adtags tags1 LEFT JOIN adtags tags2 ON (tags1.superid=tags2.tagid)" ).
					append( " WHERE tags1.tagid = '%s'" ).toString();
	
	private final String ADD_TAG = "INSERT INTO adtags(tagid, tagname, tagtype, tagscript, faketagscript, superid, groupid, modifieddate, publicationdate) VALUES ('%s', '%s', '%s', '%s', %s, %s, %s, '%s', null)";
	private final String UPDATE_TAG = "UPDATE adtags SET tagname='%s', tagtype='%s', tagscript='%s', faketagscript=%s, superid=%s, modifieddate='%s' WHERE tagid='%s'";
	private final String DELETE_TAGS = "DELETE FROM adtags WHERE tagid IN %s";
	
	private final String CHECK_GLOBAL_SUPERID = "SELECT count(*) FROM adtags WHERE tagtype = '" + GLOBAL + "' AND tagid='%s'";
	
	private final String GET_ALL_TAGS = "SELECT tagid, tagname, tagtype, tagscript, faketagscript, superid FROM adtags WHERE groupId = %d"; 
	private final String GET_TAGS_TO_PUBLISH = new StringBuilder("SELECT tagid, tagname, tagtype, tagscript, faketagscript, superid").append(SUBQUERY_GET_CONTENT_TO_PUBLISH).toString();
	private final String UPDATE_TAGS_PUBLISHED =   "UPDATE adtags SET publicationdate='%s' WHERE tagid IN %s";
	
	//Skins
	private final String GET_SKINS =
		new StringBuilder( "SELECT sk.groupid, sk.skinid, sk.backgroundcolor, sk.imagename, sk.fileentryuuid, sk.displaymode, sk.clickurl, sk.clickscript, sk.superid, tg.tagname," 	).append
		 				 ( " IF(sk.fileentryuuid IS NULL, sk.imagename, CAST(CONCAT('/documents/', d.groupId, '/', d.folderId, '/', d.title%s) AS CHAR CHARACTER SET utf8)) imagepath" 	).append
				   		 ( " FROM adskin sk "																																			).append
				   		 ( " LEFT JOIN adtags tg ON (sk.superid=tg.tagid)" 																												).append
				   		 ( " LEFT JOIN adfileentry a ON a.fileentryuuid = sk.fileentryuuid" 																							).append
		 				 ( " LEFT JOIN DLFileEntry d ON d.uuid_ = a.dlfileentryuuid %s" 																								).toString();
	
	private final String WHERE_SKINS_BY_GROUP = "WHERE sk.groupid = %s ORDER BY sk.imagename";
	private final String WHERE_SKINS_BY_SKINID = "WHERE sk.skinid = '%s' ORDER BY sk.imagename";

	private final String ADD_SKIN = "INSERT INTO adskin(skinid, backgroundcolor, imagename, fileentryuuid, displaymode, clickurl, clickscript, superid, groupid, modifieddate, publicationdate) VALUES ('%s', '%s', '%s', %s, '%s', %s, %s, %s, %s, '%s', null)";
	private final String UPDATE_SKIN = "UPDATE adskin SET backgroundcolor='%s', imagename='%s', fileentryuuid=%s, displaymode='%s', clickurl=%s, clickscript=%s, superid=%s, modifieddate='%s' WHERE skinid='%s'";
	private final String DELETE_SKINS = "DELETE FROM adskin WHERE skinid IN %s";
	
	
	private final String GET_ALL_SKINS = "SELECT skinid, backgroundcolor, imagename, fileentryuuid, displaymode, clickurl, clickscript, superid FROM adskin WHERE groupid = %d"; 
	private final String GET_SKINS_TO_PUBLISH = new StringBuilder("SELECT skinid, backgroundcolor, imagename, fileentryuuid, displaymode, clickurl, clickscript, superid").append(SUBQUERY_GET_CONTENT_TO_PUBLISH).toString();
	private final String UPDATE_SKINS_PUBLISHED =   "UPDATE adskin SET publicationdate='%s' WHERE skinid IN %s";
	
	//Slots_tags
	private final String GET_PRIORITY = 
		new StringBuilder( "SELECT IFNULL(MAX(priority),0) \n" ).
					append( " FROM %s st INNER JOIN adslot ON (st.slotid=adslot.slotid) \n" ).
					append( " INNER JOIN adslot ads2 ON ( ads2.slotid='%%s' AND ads2.groupid=adslot.groupid ) \n" ).
					append( " WHERE IFNULL(st.%s, 0) = IFNULL(%%s, 0) \n").toString();
	
	private final String GET_SLOTS_TAGS = 
		new StringBuilder( "SELECT st.slottagid, st.slotid, s.slotname, st.skinid, sk.imagename, st.tagid, t.tagname, st.%2$s, st.priority, IF(st.enabled,'true','false') AS enabled, st.vigenciadesde, st.vigenciahasta \n").
					append( " FROM %1$s st INNER JOIN adslot s ON (st.slotid=s.slotid AND s.groupid=%3$s) \n" ).
					append( " LEFT JOIN adtags t ON (st.tagid=t.tagid) \n" ).
					append( " LEFT JOIN adskin sk ON (st.skinid=sk.skinid) \n" ).
					append( " WHERE IFNULL(st.%2$s, 0) = IFNULL(%4$s, 0) \n").
					append( " ORDER BY st.priority \n" ).toString();
						
	private final String GET_SLOT_TAG_BY_SLOTTAGID =
		new StringBuilder( "SELECT st.slottagid, st.slotid, s.slotname, st.skinid, sk.imagename, st.tagid, t.tagname, st.%s, st.priority, IF(st.enabled,'true','false') AS enabled, st.vigenciadesde, st.vigenciahasta " ).
					append( " FROM %s st INNER JOIN adslot s ON (st.slotid=s.slotid)" ).
					append( " LEFT JOIN adtags t ON (st.tagid=t.tagid)" ).
					append( " LEFT JOIN adskin sk ON (st.skinid=sk.skinid)" ).
					append( " WHERE st.slottagid='%%s'").toString();
	
	private final String ADD_SLOTS_TAGS_LAYOUT = 
		new StringBuilder( " INSERT INTO adslotadtags (slottagid, slotid, skinid, tagid, plid, priority, enabled, vigenciadesde, vigenciahasta, modifieddate) \n").
					append( "\tVALUES ('%1$s', '%2$s', %3$s, %4$s, %5$s, %6$s, %7$s, %8$s, %9$s, '%10$s')\n ON DUPLICATE KEY \n").
					append(" UPDATE slotid=VALUES(slotid), skinid=VALUES(skinid), tagid=VALUES(tagid), plid=VALUES(plid), enabled=VALUES(enabled),").
					append(" vigenciadesde=VALUES(vigenciadesde), vigenciahasta=VALUES(vigenciahasta)%11$s, modifieddate=VALUES(modifieddate) \n").toString();
	
	private final String ADD_SLOTS_TAGS_CATEGORY = new StringBuilder( 
						" INSERT INTO adslottagcategory (slottagid, slotid, skinid, tagid, categoryid, advocabularyid, priority, enabled, vigenciadesde, vigenciahasta, modifieddate) \n"	).append(
						"\tVALUES ('%%1$s', '%%2$s', %%3$s, %%4$s, %%5$s, (%1$s), %%6$s, %%7$s, %%8$s, %%9$s, '%%10$s')\n ON DUPLICATE KEY \n"												).append(
						" UPDATE slotid=VALUES(slotid), skinid=VALUES(skinid), tagid=VALUES(tagid), categoryid=VALUES(categoryid), advocabularyid=VALUES(advocabularyid),\n"					).append(
						"\tenabled=VALUES(enabled), vigenciadesde=VALUES(vigenciadesde), vigenciahasta=VALUES(vigenciahasta)%%11$s, modifieddate=VALUES(modifieddate) \n"					).toString();

	private final String GET_ADVOCID_FROM_CATID = new StringBuilder(
							" SELECT advocabularyid \n"																										).append(
							" FROM advocabulary \n"																											).append(
							" INNER JOIN assetvocabulary ON (advocabulary.vocabularyId=assetvocabulary.vocabularyId) \n"									).append(
							" INNER JOIN assetcategory ON(assetvocabulary.vocabularyId=assetcategory.vocabularyId AND assetcategory.categoryId=%5$s) \n"	).append(
							" WHERE advocabulary.groupid=%12$s"																								).toString();
	
	private final String UPDATE_SLOTS_TAGS_PRIORITY =
		new StringBuilder( "UPDATE %s st INNER JOIN adslot ON (st.slotid=adslot.slotid)" ).
					append( " INNER JOIN adslot ads2 ON ( ads2.slotid='%s' AND ads2.groupid=adslot.groupid )" ).
					append( " SET st.priority=st.priority %s 1, st.modifieddate='%s'" ).
					append( " WHERE st.priority>=%d AND st.priority<=%d AND st.slottagid!='%s' \n AND IFNULL(st.%s, 0) = IFNULL(%s, 0) \n " ).toString();
	
	private final String GET_UPDATED_SLOTS_TAGS = 
		new StringBuilder( "SELECT st.slottagid, st.slotid, s.slotname, st.skinid, sk.imagename, st.tagid, t.tagname, st.%1$s, st.priority, IF(st.enabled,'true','false') AS enabled, st.vigenciadesde, st.vigenciahasta \n" ).
					append( " FROM %2$s st INNER JOIN adslot s ON (st.slotid=s.slotid) \n" ).
					append( " INNER JOIN adslot ads2 ON ( ads2.slotid='%3$s' AND ads2.groupid=s.groupid ) \n" ).
					append( " LEFT JOIN adtags t ON (st.tagid=t.tagid) \n" ).
					append( " LEFT JOIN adskin sk ON (st.skinid=sk.skinid) \n" ).
					append( " WHERE st.priority>=%4$d AND st.priority<=%5$d AND IFNULL(st.%1$s, 0) = IFNULL(%6$s, 0) \n" ).
					append( " ORDER BY st.priority \n " ).toString();
	
//	private static final String SLOT_TAGS_ORDER_BY_PRIORITY = " ORDER BY st.priority ";
	
	private final String DELETE_SLOTS_TAGS = "DELETE FROM %s WHERE slottagid IN %s";
	private final String GET_ALL_SLOTS_TAGS = new StringBuilder(
		"SELECT st.slottagid, st.slotid, st.skinid, st.tagid, friendlyURL, st.priority, \n").append(
		"		st.enabled, st.vigenciadesde, st.vigenciahasta, st.publicationdate		\n").append(
		"FROM adslotadtags st 															\n").append(
		"INNER JOIN adslot s ON s.slotid=st.slotid										\n").append(
		"LEFT JOIN Layout ON Layout.plid = st.plid										\n").append( 
		"  WHERE s.groupid=%d															\n").toString();
		
	private final String GET_SLOTS_TAGS_LAYOUT_TO_PUBLISH = new StringBuilder(
							"SELECT slottagid, slotid, skinid, tagid, %s, priority, enabled, vigenciadesde, vigenciahasta"															).append(
							", IF(publicationdate IS NULL, '" + IterKeys.CREATE + "', '" + IterKeys.UPDATE + "') operation \n"														).append(
							"FROM \n\t(SELECT st.slottagid, st.slotid, st.skinid, st.tagid, st.plid, st.priority, st.enabled, st.vigenciadesde, st.vigenciahasta, st.publicationdate\n"	).append(
							"\t FROM adslotadtags st LEFT JOIN adslot s ON s.slotid=st.slotid WHERE s.groupid=%s AND\n" 																		).append(
							"\t IF(st.publicationdate IS NOT NULL AND st.modifieddate <= st.publicationdate, false, true)) query\n"													).toString();
	
	private final String GET_ALL_SLOTS_TAGS_CATEGORY = new StringBuilder(
		"SELECT st.slottagid, st.slotid, st.skinid, st.tagid, st.advocabularyid, st.priority,						\n").append(
		"		ITR_GET_CATEGORY_PATH_EXT(st.categoryid, '").append(StringPool.SECTION).append("', 0) categoryPath,	\n").append(
		"		st.enabled, st.vigenciadesde, st.vigenciahasta, st.publicationdate									\n").append(
		"FROM adslottagcategory st																					\n").append( 
		"INNER JOIN adslot s ON s.slotid=st.slotid																	\n").append( 
		"  WHERE s.groupid=%s																						\n").toString(); 
		
	private final String GET_SLOTS_TAGS_CATEGORY_TO_PUBLISH = new StringBuilder(
							"SELECT slottagid, slotid, skinid, tagid, %s, advocabularyid, priority, enabled, vigenciadesde, vigenciahasta"															).append(
							", IF(publicationdate IS NULL, '" + IterKeys.CREATE + "', '" + IterKeys.UPDATE + "') operation \n"														).append(
							"FROM \n\t(SELECT st.slottagid, st.slotid, st.skinid, st.tagid, st.categoryid, st.advocabularyid, st.priority, st.enabled, st.vigenciadesde, st.vigenciahasta, st.publicationdate\n"	).append(
							"\t FROM adslottagcategory st LEFT JOIN adslot s ON s.slotid=st.slotid WHERE s.groupid=%s AND\n" 																		).append(
							"\t IF(st.publicationdate IS NOT NULL AND st.modifieddate <= st.publicationdate, false, true)) query\n"													).toString();
	
	private final String GET_PLID_GLOBALID = "CAST((IF(plid IS NULL, NULL, (SELECT globalid FROM Xmlio_Live WHERE localId=plid AND classNameValue='" + IterKeys.CLASSNAME_LAYOUT + "'))) AS CHAR CHARACTER SET utf8) plid";
	private final String GET_CATEGORYID_GLOBALID = "CAST((IF(categoryid IS NULL, NULL, (SELECT globalid FROM Xmlio_Live WHERE localId=categoryid AND classNameValue='" + IterKeys.CLASSNAME_CATEGORY + "'))) AS CHAR CHARACTER SET utf8) categoryid";
	private final String GET_VOCABULARYID_GLOBALID = "CAST((IF(vocabularyid IS NULL, NULL, (SELECT globalid FROM Xmlio_Live WHERE localId=vocabularyid AND classNameValue='" + IterKeys.CLASSNAME_VOCABULARY + "'))) AS CHAR CHARACTER SET utf8) vocabularyid";
	
	private final String UPDATE_SLOT_TAGS_PUBLISHED =   "UPDATE %s SET publicationdate='%s' WHERE slottagid IN %s";
	
	private final String CHECK_UNQ_SLOT_TAG =
		new StringBuilder( 	" SELECT COUNT(*) FROM %1$s \n" 								).
				   append(  " WHERE (slotid='%%1$s' AND %2$s %%2$s AND skinid %%3$s AND tagid %%4$s " 	).
				   append(  " AND vigenciadesde %%5$s AND vigenciahasta %%6$s and %%7$s)\n OR \n" 		).
				   append(	"(slotid='%%1$s' AND %2$s %%2$s AND skinid %%3$s AND tagid %%4$s " 			).
				   append(  " AND vigenciadesde %%5$s AND vigenciahasta %%6$s AND slottagid %%8$s) \n"	).toString();
		
	
	//FileEntries
	private final String GET_ALL_FILE_ENTRIES = new StringBuilder(
		"SELECT fileentryuuid, f.fileentryuuid, d.title, d.description, d.size_ as size \n").append(
		"FROM adfileentry f 															\n").append(
		"INNER JOIN DLFileEntry d ON d.uuid_=f.dlfileentryuuid							\n").append(
		"	WHERE d.groupid=%s 															\n").toString();
		
	private final String GET_FILE_ENTRIES_TO_PUBLISH = new StringBuilder("SELECT fileentryuuid, title, description, size_ size, IF(publicationdate IS NULL, '" + IterKeys.CREATE + "', '" + IterKeys.UPDATE + "') operation\n"			).append(
																 				"FROM (SELECT f.fileentryuuid, d.title, d.description, f.publicationdate, d.size_ FROM adfileentry f LEFT JOIN DLFileEntry d ON d.uuid_=f.dlfileentryuuid\n" 	).append(
																 				"WHERE d.groupid=%s AND IF(f.publicationdate IS NOT NULL AND f.modifieddate <= f.publicationdate, false, true)) query"											).toString();
	private final String UPDATE_FILE_ENTRIES_PUBLISHED =   "UPDATE adfileentry SET publicationdate='%s' WHERE fileentryuuid IN %s";
	
	private final String GET_FILE_ENTRIES_UUIDS = "SELECT dlfileentryuuid FROM adfileentry WHERE fileentryuuid IN %s";
	
	private final String GET_VOCS = new StringBuilder(
		"SELECT AssetVocabulary.vocabularyid, 																																							\n").append(
		"	   ").append(AssetVocabularyConstants.getNameSQL("AssetVocabulary.NAME")).append(" name																										\n").append(
		"FROM AssetVocabulary																																											\n").append(
		AssetVocabularyConstants.getDelegationRestriction("%1$s", "AssetVocabulary.name")						   																						   ).append(
		"LEFT  JOIN AdVocabulary ON (AssetVocabulary.vocabularyid=advocabulary.vocabularyid AND AdVocabulary.groupid=%1$s) 																				\n").append(
		"	WHERE advocabulary.vocabularyid IS NULL 																																					\n").append(
		"	ORDER BY AssetVocabulary.name 																																								\n").toString();
			
			
	private final String FETCH_ADVOCABULARIES = new StringBuilder(
		"-- No es necesario restringir el vocabulario por delegación ya que está acotado al grupo local por advocabulary.groupid				\n").append(
		"SELECT Voc.name path, Voc.* 																											\n").append(
		"FROM (																																	\n").append(
		"	SELECT 	v.vocabularyId id, 																											\n").append(
		"			").append(AssetVocabularyConstants.getNameSQL("v.name")).append(" name, 													\n").append(
		"			'").append(IterKeys.TREE_TOP_LEVEL).append("' type, 'true' isBranch, 														\n").append(
		" 			(SELECT IF((SELECT COUNT(0) FROM AssetCategory c2 WHERE c2.vocabularyId=v.vocabularyId) > 0, 'true', 'false')) hasChildren, \n").append(
		" 			'true' enabled, advocabulary.advocabularyid 																				\n").append(
		"	FROM AssetVocabulary v INNER JOIN advocabulary ON (v.vocabularyId=advocabulary.vocabularyid)										\n").append(
		"		WHERE v.companyId = %s AND v.groupId = %s AND advocabulary.groupid=%s 															\n").append(																						
		"		ORDER BY v.name ASC, v.vocabularyId ASC 																						\n").append(
		"	  ) Voc																																\n").toString();
/*	
	private final String FETCH_DISCRETE_ADVOCABULARIES = new StringBuilder(
							" SELECT v.vocabularyId id, v.name, v.name path, '" + IterKeys.TREE_TOP_LEVEL + "' type, 'true' isBranch, \n").append(
							" \t(SELECT IF((SELECT COUNT(0) FROM AssetCategory c2 WHERE c2.vocabularyId=v.vocabularyId) > 0, 'true', 'false')) hasChildren, \n").append(
							" 'true' enabled, advocabulary.advocabularyid \n").append(
							" FROM AssetVocabulary v INNER JOIN advocabulary ON (v.vocabularyId=advocabulary.vocabularyid)\n").append(
							" WHERE advocabulary.advocabularyid IN ('%s') \n").append(																						
							" ORDER BY v.name ASC, v.vocabularyId ASC \n").toString();
	
	private final String ADD_VOCS = "INSERT INTO advocabulary(advocabularyid, groupid, vocabularyid) VALUES %s \n";
*/
	private final String DELETE_VOCS = "DELETE FROM advocabulary WHERE advocabularyid IN ('%s') \n";
//	private static final String GET_ALL_VOCS = new StringBuilder(
//		"SELECT advocabularyid, AssetVocabulary.name vocabularyName								\n").append(
//		"FROM advocabulary																		\n").append(
//		"INNER JOIN AssetVocabulary ON AssetVocabulary.vocabularyId = advocabulary.vocabularyId	\n").append(
//		"  WHERE advocabulary.groupId = %d														\n").toString();

	private final String GET_VOCS_TO_PUBLISH = "SELECT advocabularyid, %s FROM advocabulary WHERE groupid=%s AND publicationdate IS NULL \n";
	private final String UPDATE_ADVOCABULARIES_PUBLISHED = "UPDATE advocabulary SET publicationdate='%s' WHERE advocabularyid IN %s";
	
	
	/* Sustituye el carácter '\'  por  '\\'.
	 * Necesario porque '\' se interpreta como una secuencia de escape y se omite al guardar el dato en base de datos.*/
	private String scapeBackSlashChar( String str)
	{
		return StringUtils.replace(str, StringPool.BACK_SLASH, StringPool.BACK_SLASH.concat(StringPool.BACK_SLASH) );
	}
	
	
	/*
	 * 
	 *  SLOTS
	 *  
	 */
	public String getSlots(String groupid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse( Long.valueOf(groupid).longValue() > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX, "Group: " + groupid);
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SLOTS_BY_GROUP, groupid)).asXML();
	}
	
	private void importSlots(long groupId, boolean updtIfExist, List<Node> adSlots) throws DocumentException, ServiceError, SecurityException, NoSuchMethodException, PortalException, SystemException, IOException, SQLException
	{
		if (adSlots.size() > 0)
		{
			String[] slotIds = XMLHelper.getStringValues(adSlots, "@slotid");
			ErrorRaiser.throwIfFalse(adSlots.size() == slotIds.length);
			String sql   = String.format("SELECT slotid FROM adslot WHERE slotid IN ('%s')", StringUtils.join(slotIds, "','"));
			Document dom = PortalLocalServiceUtil.executeQueryAsDom(sql);
			
			for (Node adSlot : adSlots)
			{
				String slotId = XMLHelper.getTextValueOf(adSlot, "@slotid");
				ErrorRaiser.throwIfNull(slotId);
				
				boolean exist = XMLHelper.getLongValueOf(dom, String.format("count(//row[@slotid = '%s'])", slotId)) > 0;
				ErrorRaiser.throwIfFalse( !exist || updtIfExist, IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, String.format("%s(%s)", IterAdmin.IA_CLASS_ADSLOT, slotId));
	
				((Element)adSlot).addAttribute("groupid", String.valueOf(groupId));
				
				if (exist)
				{
					Element rootSlot = SAXReaderUtil.read("<rs/>").getRootElement();
					rootSlot.add( adSlot.detach() );
					updateSlot(rootSlot);
				}
				else
				{
					addSlot(adSlot);
				}
			}
		}
	}
	
	public String addSlot(String xmlData) throws Exception
	{
		Document dom = SAXReaderUtil.read(xmlData);
		ClusterMgr.processConfig(ClusterMgrType.AD_SLOT, ClusterMgrOperation.ADD, dom);
		
		return addSlot(dom.getRootElement().selectSingleNode("row"));
	}
	
	private String addSlot(Node node) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException, IOException, SQLException
	{
		String slotname = XMLHelper.getTextValueOf(node, "@slotname");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(slotname), IterErrorKeys.XYZ_E_INVALID_SLOT_NAME_ZYX);
		slotname = StringEscapeUtils.escapeSql(slotname);
		
		String slottype = XMLHelper.getTextValueOf(node, "@slottype");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(slottype), IterErrorKeys.XYZ_E_INVALID_SLOT_TYPE_ZYX);
		slottype = slottype.toLowerCase();

		long groupid = XMLHelper.getLongValueOf(node, "@groupid");
		ErrorRaiser.throwIfFalse(groupid > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX);

		String uuid = XMLHelper.getTextValueOf(node, "@slotid");
		if(Validator.isNull(uuid))
			uuid = SQLQueries.getUUID();
		
		String sql = String.format(ADD_SLOT, uuid, slotname, slottype, groupid, SQLQueries.getCurrentDate());
		
		_log.debug("add slot: " + sql);
		
		PortalLocalServiceUtil.executeUpdateQuery( sql );
		
		String retVal = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SLOT_BY_SLOTID, uuid)).asXML();
		
		return retVal;
	}
	
	public String updateSlot(String xmlData) throws Exception
	{
		Document dom = SAXReaderUtil.read(xmlData);
		ClusterMgr.processConfig(ClusterMgrType.AD_SLOT, ClusterMgrOperation.UPDATE, dom);
		Element root = dom.getRootElement();
		String result = updateSlot(root);

		long groupid = XMLHelper.getLongValueOf(root.selectSingleNode("row"), "@groupid");
		// Actualizamos la fecha de publicación para que los slots de adContainers que estén en catálogos vean los cambios realizados
		GroupMgr.updatePublicationDate(groupid, new Date() );
		
		return result;
	}
	
	private String updateSlot(Element dataRoot) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException, IOException, SQLException
	{
		Node node = dataRoot.selectSingleNode("row");
		
		long groupid = XMLHelper.getLongValueOf(node, "@groupid");
		ErrorRaiser.throwIfFalse(groupid > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX);
		
		String slotid = XMLHelper.getTextValueOf(node, "@slotid");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(slotid), IterErrorKeys.XYZ_E_INVALID_SLOT_ID_ZYX);
		
		String slotname = XMLHelper.getTextValueOf(node, "@slotname");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(slotname), IterErrorKeys.XYZ_E_INVALID_SLOT_NAME_ZYX);
		slotname = StringEscapeUtils.escapeSql(slotname);
		
		String slottype = XMLHelper.getTextValueOf(node, "@slottype");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(slottype), IterErrorKeys.XYZ_E_INVALID_SLOT_TYPE_ZYX);
		slottype = slottype.toLowerCase();
		
		boolean checkreferences = GetterUtil.getBoolean(XMLHelper.getTextValueOf(dataRoot, "@checkreferences"), true);
		checkDependencies(dataRoot, SLOT, true, checkreferences);
		
		String sql = String.format(UPDATE_SLOT, slotname, slottype, SQLQueries.getCurrentDate(), slotid);
		_log.debug("update slot: " + sql);
		
		PortalLocalServiceUtil.executeUpdateQuery( sql );
		
		Document retVal = PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_SLOT_BY_SLOTID, slotid));
		
		MetadataAdvertisementTools.updateSlot(retVal);
		
		return retVal.asXML();
	}
	
	public String deleteSlots(String xmlData) throws Exception
	{
		Document dom = SAXReaderUtil.read(xmlData);
		ClusterMgr.processConfig(ClusterMgrType.AD_SLOT, ClusterMgrOperation.DELETE, dom);

		Element dataRoot = dom.getRootElement();
		XPath xpath = SAXReaderUtil.createXPath("//row/@slotid");
		
		List<Node> nodes = xpath.selectNodes(dataRoot);
		ErrorRaiser.throwIfFalse(nodes != null && nodes.size() > 0);
		
		boolean checkreferences = GetterUtil.getBoolean(XMLHelper.getTextValueOf(dataRoot, "@checkreferences"), true);
		checkDependencies(dataRoot, SLOT, false, checkreferences);

		String inClauseSQL = TeaserMgr.getInClauseSQL(nodes);

		addToDeleteList(inClauseSQL, "adslot", "slotid");
		
		String sql = String.format(DELETE_SLOTS, inClauseSQL);
		
		_log.debug("delete slots: " + sql);
		
		PortalLocalServiceUtil.executeUpdateQuery( sql );

		return xmlData;
	}

	private void checkDependencies(Element rs, String type, boolean update, boolean checkReferences) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError
	{
		if(checkReferences)
		{
			boolean isError = false;
			boolean doCheck = true;
			StringBuffer errorMsg = new StringBuffer();
			
			if(update)
			{
				if(type.equals(TAG))
				{
					String tagid = XMLHelper.getTextValueOf(rs, "/rs/row/@tagid");
					String newType = XMLHelper.getTextValueOf(rs, "/rs/row/@tagtype");
					String oldType = XMLHelper.getTextValueOf(getRowByUUID(tagid, "tagid", "adtags"), "/rs/row/@tagtype");
					if(!oldType.equals(GLOBAL) || (oldType.equals(GLOBAL) && newType.equals(GLOBAL)))
						doCheck = false;
				}
				else if(type.equals(SLOT))
				{
					String slotid = XMLHelper.getTextValueOf(rs, "/rs/row/@slotid");
					String newType = XMLHelper.getTextValueOf(rs, "/rs/row/@slottype");
					String oldType = XMLHelper.getTextValueOf(getRowByUUID(slotid, "slotid", "adslot"), "/rs/row/@slottype");
					if((oldType.equals(SKIN) && newType.equals(SKIN)) || (!oldType.equals(SKIN) && !newType.equals(SKIN)))
						doCheck = false;
				}
			}
			
			if(doCheck)
			{
				XPath xpath = SAXReaderUtil.createXPath("//row");
				List<Node> nodes = xpath.selectNodes(rs);
				for(Node newRow:nodes)
				{
					StringBuffer currentErrorMsg = new StringBuffer();
					
					if(type.equals(SKIN))
					{
						String skinid = XMLHelper.getTextValueOf(newRow, "@skinid");
						String imagename = XMLHelper.getTextValueOf(getRowByUUID(skinid, "skinid", "adskin"), "/rs/row/@imagename");
						
						//Skin asignado a un módulo y a una página(o categoría)
						String query = String.format(GET_SLOT_SKINS_REFERENCES, "k.skinid", skinid);
						Document result = PortalLocalServiceUtil.executeQueryAsDom(query);
						currentErrorMsg.append(getErrorMsg(result, true, true));
						if(checkHasRow(result))
							isError = true;

						if(currentErrorMsg.length() > 0)
							currentErrorMsg.insert(0, "\n" + imagename + ":\n\n");
					}
					else if(type.equals(TAG))
					{
						String tagid = XMLHelper.getTextValueOf(newRow, "@tagid");
						String tagname = XMLHelper.getTextValueOf(getRowByUUID(tagid, "tagid", "adtags"), "/rs/row/@tagname");
						
						//Tag asignado a un módulo y a una página(o categoría)
						String query = String.format(GET_SLOT_TAGS_REFERENCES, "t.tagid", tagid);
						Document result = PortalLocalServiceUtil.executeQueryAsDom(query);
						currentErrorMsg.append(getErrorMsg(result, true, false));
						if(checkHasRow(result))
							isError = true;


						//Tag de tipo "dependencia" del que depende un tag asignado a un módulo y a una página( o categoría)
						query = String.format(GET_SLOT_TAGS_REFERENCES, "t.superid", tagid);
						result = PortalLocalServiceUtil.executeQueryAsDom(query);
						currentErrorMsg.append(getErrorMsg(result, true, false));
						if(checkHasRow(result))		
							isError = true;


						//Tag de tipo "dependencia" del que depende un skin asignado a un módulo y a una página(o categoría)
						query = String.format(GET_SLOT_SKINS_REFERENCES, "k.superid", tagid);
						result = PortalLocalServiceUtil.executeQueryAsDom(query);
						currentErrorMsg.append(getErrorMsg(result, true,  true));
						if(checkHasRow(result))						
							isError = true;

						//Tag de tipo "dependencia" del que depende otro tag
						query = String.format(GET_TAGS_REFERENCES, "tagname", "adtags", tagid);
						result = PortalLocalServiceUtil.executeQueryAsDom(query);
						currentErrorMsg.append(getErrorMsg(result, false, false));

						//Tag de tipo "dependencia" del que depende un skin
						query = String.format(GET_TAGS_REFERENCES, "imagename", "adskin", tagid);
						result = PortalLocalServiceUtil.executeQueryAsDom(query);
						currentErrorMsg.append(getErrorMsg(result, false, true));

						if(currentErrorMsg.length() > 0)
							currentErrorMsg.insert(0, "\n" + tagname + ":\n\n");
					}
					else if(type.equals(SLOT))
					{
						String slotid = XMLHelper.getTextValueOf(newRow, "@slotid");
						String slotname = XMLHelper.getTextValueOf(getRowByUUID(slotid, "slotid", "adslot"), "/rs/row/@slotname");
						
						//Módulo vinculado con un tag y asignado a una página(o categoría)
						String query = String.format(GET_SLOT_TAGS_REFERENCES, "s.slotid", slotid);
						Document result = PortalLocalServiceUtil.executeQueryAsDom(query);
						currentErrorMsg.append(getErrorMsg(result, true, false));
						if(checkHasRow(result))		
							isError = true;
						
						//Módulo vinculado con un skin y asignado a una página(o categoría)
						query = String.format(GET_SLOT_SKINS_REFERENCES, "s.slotid", slotid);
						result = PortalLocalServiceUtil.executeQueryAsDom(query);
						currentErrorMsg.append(getErrorMsg(result, true, true));
						if(checkHasRow(result))		
							isError = true;
						
						
						if(currentErrorMsg.length() > 0)
							currentErrorMsg.insert(0, "\n" + slotname + ":\n\n");
					}
					
					errorMsg.append(currentErrorMsg);
				}
			}
			
			if(errorMsg.length() > 0)
			{
				if(isError)
				{
					throw new ServiceError(IterErrorKeys.XYZ_E_INVALID_ADVERTISEMENT_OPERATION_ZYX, errorMsg.toString());
				}
				else
				{
					 if(type.equals(TAG))
						 throw new ServiceError(IterErrorKeys.XYZ_E_TAG_HAS_REFERENCES_ZYX, errorMsg.toString());
				}
			}
		}
	}
	
	private boolean checkHasRow(Document rs) throws SecurityException, NoSuchMethodException
	{
		boolean hasRows = false;
		
		XPath xpath = SAXReaderUtil.createXPath("//row");
		List<Node> nodes = xpath.selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
			hasRows = true;
		
		return hasRows;
	}
	
	private String getErrorMsg(Document rs, boolean slottags_slotkins, boolean skin) throws SecurityException, NoSuchMethodException
	{
		StringBuffer error = new StringBuffer();
		
		XPath xpath = SAXReaderUtil.createXPath("//row");
		List<Node> nodes = xpath.selectNodes(rs);
		for(Node node:nodes)
		{
			if(slottags_slotkins)
			{
				String assignmentTypeName = GetterUtil.getString(XMLHelper.getTextValueOf(node, "@assigntype")).equals("section")? IterKeys.TRAD_SECTIONNAME_TRAD: IterKeys.TRAD_CATEGORYNAME_TRAD;
				
				error.append(assignmentTypeName + ": " + GetterUtil.getString(XMLHelper.getTextValueOf(node,"@assignname"), IterKeys.TRAD_DEFAULT_VALUE_TRAD) + ", ");
				error.append(IterKeys.TRAD_SLOTNAME_TRAD + ": " + XMLHelper.getTextValueOf(node, "@slotname") + ", ");
			}
			
			if(skin)
				error.append(IterKeys.TRAD_SKINNAME_TRAD + ": " + XMLHelper.getTextValueOf(node, "@imagename") + "\n");
			else
				error.append(IterKeys.TRAD_TAGNAME_TRAD + ": " + XMLHelper.getTextValueOf(node, "@tagname") + "\n");
		}
		
		return error.toString();
	}
	
	
	private Element getRowByUUID(String uuid, String uuidName, String tableName) throws SecurityException, NoSuchMethodException
	{
		return PortalLocalServiceUtil.executeQueryAsDom(String.format(GET_ROW_BY_ID, tableName, uuidName, uuid)).getRootElement();
	}
	
	/*
	 * 
	 *  TAGS
	 *  
	 */
	public String getTags(String groupid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse( Long.valueOf(groupid).longValue() > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX, "Group: " + groupid);
		
		String sql = String.format(GET_TAGS_BY_GROUP, groupid);
		
		_log.debug("get tags: " + sql);
		
		Document result = PortalLocalServiceUtil.executeQueryAsDom( sql, new String[]{"tagscript", "faketagscript"} );

		return result.asXML();
	}
	
	private void importTags(long groupId, boolean updtIfExist, List<Node> adTags) throws Exception
	{
		if (adTags.size() > 0)
		{
			String[] tagIds = XMLHelper.getStringValues(adTags, "@tagid");
			ErrorRaiser.throwIfFalse(adTags.size() == tagIds.length);
			String sql   = String.format("SELECT tagid FROM adtags WHERE tagid IN ('%s')", StringUtils.join(tagIds, "','"));
			Document dom = PortalLocalServiceUtil.executeQueryAsDom(sql);
			
			for (Node adTag : adTags)
			{
				String tagId = XMLHelper.getTextValueOf(adTag, "@tagid");
				ErrorRaiser.throwIfNull(tagId);
				
				boolean exist = XMLHelper.getLongValueOf(dom, String.format("count(//row[@tagid = '%s'])", tagId)) > 0;
				ErrorRaiser.throwIfFalse( !exist || updtIfExist, IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, String.format("%s(%s)", IterAdmin.IA_CLASS_ADTAG, tagId));
	
				((Element)adTag).addAttribute("groupid", String.valueOf(groupId));
				
				if (exist)
				{
					Element rootTag = SAXReaderUtil.read("<rs/>").getRootElement();
					rootTag.add( adTag.detach() );
					updateTag(rootTag);
				}
				else
				{
					addTag(adTag);
				}
			}
		}
	}

	public String addTag(String xmlData) throws Exception
	{
		Document dom = SAXReaderUtil.read(xmlData);
		ClusterMgr.processConfig(ClusterMgrType.AD_TAG, ClusterMgrOperation.ADD, dom);
		
		return addTag(dom.getRootElement().selectSingleNode("row"));
	}
	
	private String addTag(Node node) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException, IOException, SQLException
	{
		String tagname = XMLHelper.getTextValueOf(node, "@tagname");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(tagname), IterErrorKeys.XYZ_E_INVALID_TAG_NAME_ZYX);
		tagname = StringEscapeUtils.escapeSql(tagname);
		
		String tagtype = XMLHelper.getTextValueOf(node, "@tagtype");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(tagtype), IterErrorKeys.XYZ_E_INVALID_TAG_TYPE_ZYX);
		tagtype = tagtype.toLowerCase();
		
		long groupid = XMLHelper.getLongValueOf(node, "@groupid");
		ErrorRaiser.throwIfFalse(groupid>0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX);
		
		String superid = XMLHelper.getTextValueOf(node, "@superid");

		checkTagTypes(tagtype, superid);
		
		if(Validator.isNotNull(superid))
			superid = "'" + superid + "'";

		String uuid = XMLHelper.getTextValueOf(node, "@tagid");
		if(Validator.isNull(uuid))
			uuid = SQLQueries.getUUID();
		
		Node nodeScript = node.selectSingleNode("tagscript");
		ErrorRaiser.throwIfNull(nodeScript, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(nodeScript.getStringValue(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		String tagscript = StringEscapeUtils.escapeSql(CDATAUtil.strip(nodeScript.getStringValue()));
		tagscript = scapeBackSlashChar(tagscript);
		
		String faketagscript = XMLHelper.getTextValueOf(node, "faketagscript", "null");
		faketagscript = StringEscapeUtils.escapeSql(CDATAUtil.strip(faketagscript));
		if (Validator.isNotNull(faketagscript))
			faketagscript = StringUtil.apostrophe( scapeBackSlashChar(faketagscript) );

		String sql = String.format(ADD_TAG, uuid, tagname, tagtype, tagscript, faketagscript, superid, groupid, SQLQueries.getCurrentDate());
		
		_log.debug("add tag: " + sql);
		
		PortalLocalServiceUtil.executeUpdateQuery( sql );
		
		sql = String.format(GET_TAG_BY_TAGID, uuid);
		
		Document result = PortalLocalServiceUtil.executeQueryAsDom( sql , new String[]{"tagscript", "faketagscript"} );
		
		return result.asXML();
	}
	
	public String updateTag(String xmlData) throws Exception
	{
		Document dom = SAXReaderUtil.read(xmlData);
		ClusterMgr.processConfig(ClusterMgrType.AD_TAG, ClusterMgrOperation.UPDATE, dom);
		
		Element root  = dom.getRootElement();
		String result = updateTag(root);

		long groupid = XMLHelper.getLongValueOf(root.selectSingleNode("row"), "@groupid");
		// Actualizamos la fecha de publicación para que los slots de adContainers que estén en catálogos vean los cambios realizados
		GroupMgr.updatePublicationDate(groupid, new Date() );
		return result;
	}
	private String updateTag(Element dataRoot) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException, IOException, SQLException
	{
		XPath xpath = SAXReaderUtil.createXPath("row");
		Node node = xpath.selectSingleNode(dataRoot);
		
		long groupid = XMLHelper.getLongValueOf(node, "@groupid");
		ErrorRaiser.throwIfFalse(groupid > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX);
		
		String tagid = XMLHelper.getTextValueOf(node, "@tagid");
		ErrorRaiser.throwIfNull(tagid, IterErrorKeys.XYZ_E_INVALID_TAG_ID_ZYX);

		String tagname = XMLHelper.getTextValueOf(node, "@tagname");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(tagname), IterErrorKeys.XYZ_E_INVALID_TAG_NAME_ZYX);
		tagname = StringEscapeUtils.escapeSql(tagname);
		
		String tagtype = XMLHelper.getTextValueOf(node, "@tagtype");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(tagtype), IterErrorKeys.XYZ_E_INVALID_TAG_TYPE_ZYX);
		tagtype = tagtype.toLowerCase();
		
		String superid = XMLHelper.getTextValueOf(node, "@superid");

		checkTagTypes(tagtype, superid);
		
		boolean checkreferences = GetterUtil.getBoolean(XMLHelper.getTextValueOf(dataRoot, "@checkreferences"), true);
		checkDependencies(dataRoot, TAG, true, checkreferences);
		
		if(Validator.isNotNull(superid))
			superid = "'" + superid + "'";
		
		xpath = SAXReaderUtil.createXPath("//tagscript");
		node = xpath.selectSingleNode(dataRoot);
		ErrorRaiser.throwIfNull(node, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfNull(node.getStringValue(), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		String tagscript = StringEscapeUtils.escapeSql(CDATAUtil.strip(node.getStringValue()));
		tagscript = scapeBackSlashChar(tagscript);
		
		String faketagscript = XMLHelper.getTextValueOf(dataRoot, "//faketagscript[1]", "null");
		faketagscript = StringEscapeUtils.escapeSql(CDATAUtil.strip(faketagscript));
		if (Validator.isNotNull(faketagscript))
			faketagscript = StringUtil.apostrophe( scapeBackSlashChar(faketagscript) );

		String sql = String.format(UPDATE_TAG, tagname, tagtype, tagscript, faketagscript, superid, SQLQueries.getCurrentDate(), tagid);
		
		_log.debug("update tag: " + sql);
		
		PortalLocalServiceUtil.executeUpdateQuery( sql );
		
		sql = String.format(GET_TAG_BY_TAGID, tagid);
	
		Document result = PortalLocalServiceUtil.executeQueryAsDom( sql , new String[]{"tagscript", "faketagscript"} );
		
		MetadataAdvertisementTools.updateTag(result);
		
		return result.asXML();
	}
	
	private void checkTagTypes(String tagtype, String superid) throws ServiceError
	{
		//Sólo tags de tipo HTML pueder depender de otro tag
		if(tagtype.equals(GLOBAL) || tagtype.equals(FLASH) || tagtype.equals(IMAGE))
		{
			ErrorRaiser.throwIfFalse(!Validator.isNotNull(superid), IterErrorKeys.XYZ_E_ONLY_HTML_TYPE_HAS_SUPERID_ZYX);
		}
		
		//Sólo tags de tipo global pueden ser referenciados
		if((tagtype.equals(HTML) || tagtype.equals(SKIN)) && Validator.isNotNull(superid))
		{
			List<Object> result = PortalLocalServiceUtil.executeQueryAsList(String.format(CHECK_GLOBAL_SUPERID, superid));
			if(result != null && result.size() > 0)
			{
				int count = Integer.valueOf(result.get(0).toString());
				ErrorRaiser.throwIfFalse(count == 1, IterErrorKeys.XYZ_E_ONLY_GLOBAL_TYPE_IS_SUPERID_ZYX);
			}
		}
	}
	
	public String deleteTags(String xmlData) throws Exception
	{
		Document dom = SAXReaderUtil.read(xmlData);
		ClusterMgr.processConfig(ClusterMgrType.AD_TAG, ClusterMgrOperation.DELETE, dom);

		Element dataRoot = dom.getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row/@tagid");
		
		List<Node> nodes = xpath.selectNodes(dataRoot);
		ErrorRaiser.throwIfFalse(nodes != null && nodes.size() > 0);
		
		boolean checkreferences = GetterUtil.getBoolean(XMLHelper.getTextValueOf(dataRoot, "@checkreferences"), true);
		checkDependencies(dataRoot, TAG, false, checkreferences);
		
		String inClauseSQL = TeaserMgr.getInClauseSQL(nodes);

		addToDeleteList(inClauseSQL, "adtags", "tagid");
		
		String sql = String.format(DELETE_TAGS, inClauseSQL);
		
		_log.debug("delete tags: " + sql);
		
		PortalLocalServiceUtil.executeUpdateQuery(sql);
		
		return xmlData;
	}
	
	public String createDefaultTagConfig(String xmlData) throws DocumentException, ServiceError
	{
		Document d = SAXReaderUtil.read(xmlData);
		
		Element dataRoot = d.getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row");
		List<Node> tagsList = xpath.selectNodes(dataRoot);
		
		long groupid = XMLHelper.getLongValueOf(dataRoot, "@groupid");
		ErrorRaiser.throwIfFalse(groupid>0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX);
		
		for (Node tag : tagsList)
		{
			String tagid = XMLHelper.getTextValueOf(tag, "@tagid");
			ErrorRaiser.throwIfNull(tagid, IterErrorKeys.XYZ_E_INVALID_TAG_ID_ZYX);
		}
		
		String query = String.format("select ITR_CREATE_DEFAULT_TAG_CONFIG('%s')", d.asXML());
		
		PortalLocalServiceUtil.executeQueryAsList( query );
		
		return "OK";
	}
	
	/*
	 * 
	 *  SKINS
	 *  
	 */
	public String getSkins(String groupid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse( Long.valueOf(groupid).longValue() > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX, "Group: " + groupid);
		String whereClauseSQL = String.format(WHERE_SKINS_BY_GROUP, groupid);
		String sql = String.format(GET_SKINS, (PropsValues.IS_PREVIEW_ENVIRONMENT ? ", '?env=preview'" : ", '?env=live'"), whereClauseSQL);
		_log.debug("get skins: " + sql);
		return PortalLocalServiceUtil.executeQueryAsDom(sql, new String[]{"clickscript"}).asXML();
	}
	
	private void importSkins(long groupId, boolean updtIfExist, List<Node> adSkins) throws Exception
	{
		if (adSkins.size() > 0)
		{
			String[] skinIds = XMLHelper.getStringValues(adSkins, "@skinid");
			ErrorRaiser.throwIfFalse(adSkins.size() == skinIds.length);
			String sql   = String.format("SELECT skinid FROM adskin WHERE skinid IN ('%s')", StringUtils.join(skinIds, "','"));
			Document dom = PortalLocalServiceUtil.executeQueryAsDom(sql);
			
			for (Node adSkin : adSkins)
			{
				String skinId = XMLHelper.getTextValueOf(adSkin, "@skinid");
				ErrorRaiser.throwIfNull(skinId);
				
				boolean exist = XMLHelper.getLongValueOf(dom, String.format("count(//row[@skinid = '%s'])", skinId)) > 0;
				ErrorRaiser.throwIfFalse( !exist || updtIfExist, IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, String.format("%s(%s)", IterAdmin.IA_CLASS_ADSKIN, skinId));
	
				((Element)adSkin).addAttribute("groupid", String.valueOf(groupId));
				
				if (exist)
					updateSkin(adSkin);
				else
					addSkin(adSkin);
			}
		}
	}
	
	public String addSkin(String xmlData) throws Exception
	{
		Document dom = SAXReaderUtil.read(xmlData);
		ClusterMgr.processConfig(ClusterMgrType.AD_SKIN, ClusterMgrOperation.ADD, dom);
		return addSkin(dom.getRootElement().selectSingleNode("row"));
	}
	
	private String addSkin(Node node) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException, IOException, SQLException
	{
		String backgroundcolor = XMLHelper.getTextValueOf(node, "@backgroundcolor");
		ErrorRaiser.throwIfNull(backgroundcolor, IterErrorKeys.XYZ_E_INVALID_SKIN_BACKGROUNDCOLOR_ZYX);
		
		String imagename = XMLHelper.getTextValueOf(node, "@imagename");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(imagename), IterErrorKeys.XYZ_E_INVALID_SKIN_IMAGE_NAME_ZYX);
		imagename = StringEscapeUtils.escapeSql(imagename);
		
		String fileentryuuid = XMLHelper.getTextValueOf(node, "@fileentryuuid");
		if(Validator.isNotNull(fileentryuuid))
			fileentryuuid = "'" + fileentryuuid + "'";
		
		String displaymode = XMLHelper.getTextValueOf(node, "@displaymode");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(displaymode), IterErrorKeys.XYZ_E_INVALID_SKIN_DISPLAY_MODE_ZYX);
		displaymode = displaymode.toLowerCase();
		
		String clickurl = XMLHelper.getTextValueOf(node, "@clickurl");
		if(Validator.isNotNull(clickurl))
			clickurl = "'" + StringEscapeUtils.escapeSql(clickurl) + "'";
		
		String superid = XMLHelper.getTextValueOf(node, "@superid");

		checkTagTypes(SKIN, superid);
		
		if(Validator.isNotNull(superid))
			superid = "'" + superid + "'";
		
		Long groupid = XMLHelper.getLongValueOf(node, "@groupid");
		ErrorRaiser.throwIfFalse(groupid>0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX);
		
		String uuid = XMLHelper.getTextValueOf(node, "@skinid");
		if(Validator.isNull(uuid))
			uuid = SQLQueries.getUUID();
		
		String clickscript = null;
		try
		{
			node = node.selectSingleNode("clickscript");
			clickscript = "'" + StringEscapeUtils.escapeSql(CDATAUtil.strip(node.getStringValue())) + "'";
			clickscript =  scapeBackSlashChar(clickscript);
		}
		catch(Exception e){}

		String sql  = String.format(ADD_SKIN, uuid, backgroundcolor, imagename, fileentryuuid, displaymode, clickurl, clickscript, superid, groupid, SQLQueries.getCurrentDate());
		
		_log.debug("add Skin: " + sql);
		
		PortalLocalServiceUtil.executeUpdateQuery(sql);
		
		String whereClauseSQL = String.format(WHERE_SKINS_BY_SKINID, uuid);
		sql = String.format(GET_SKINS, (PropsValues.IS_PREVIEW_ENVIRONMENT ? ", '?env=preview'" : ", '?env=live'"), whereClauseSQL);
		
		Document result = PortalLocalServiceUtil.executeQueryAsDom( sql , new String[]{"clickscript"} );
		
		return result.asXML();
	}
	
	public String updateSkin(String xmlData) throws Exception
	{
		Document dom = SAXReaderUtil.read(xmlData);
		ClusterMgr.processConfig(ClusterMgrType.AD_SKIN, ClusterMgrOperation.UPDATE, dom);
		return updateSkin(dom.getRootElement().selectSingleNode("row"));
	}
	
	public String updateSkin(Node node) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException, IOException, SQLException
	{
		String skinid = XMLHelper.getTextValueOf(node, "@skinid");
		ErrorRaiser.throwIfNull(skinid, IterErrorKeys.XYZ_E_INVALID_SKIN_ID_ZYX);
		
		String backgroundcolor = XMLHelper.getTextValueOf(node, "@backgroundcolor");
		ErrorRaiser.throwIfNull(backgroundcolor, IterErrorKeys.XYZ_E_INVALID_SKIN_BACKGROUNDCOLOR_ZYX);
		
		String imagename = XMLHelper.getTextValueOf(node, "@imagename");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(imagename), IterErrorKeys.XYZ_E_INVALID_SKIN_IMAGE_NAME_ZYX);
		imagename = StringEscapeUtils.escapeSql(imagename);
		
		String fileentryuuid = XMLHelper.getTextValueOf(node, "@fileentryuuid");
		if(Validator.isNotNull(fileentryuuid))
			fileentryuuid = "'" + fileentryuuid + "'";
		
		String displaymode = XMLHelper.getTextValueOf(node, "@displaymode");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(displaymode), IterErrorKeys.XYZ_E_INVALID_SKIN_DISPLAY_MODE_ZYX);
		displaymode = displaymode.toLowerCase();
		
		String clickurl = XMLHelper.getTextValueOf(node, "@clickurl");
		if(Validator.isNotNull(clickurl))
			clickurl = "'" + StringEscapeUtils.escapeSql(clickurl) + "'";
		
		String superid = XMLHelper.getTextValueOf(node, "@superid");

		checkTagTypes(SKIN, superid);
		
		if(Validator.isNotNull(superid))
			superid = "'" + superid + "'";
		
		String clickscript = null;
		try
		{
			node = node.selectSingleNode("clickscript");
			clickscript = "'" + StringEscapeUtils.escapeSql(CDATAUtil.strip(node.getStringValue())) + "'";
			clickscript =  scapeBackSlashChar(clickscript);
		}
		catch(Exception e){}

		String sql = String.format(UPDATE_SKIN, backgroundcolor, imagename, fileentryuuid, displaymode, clickurl, clickscript, superid, SQLQueries.getCurrentDate(), skinid);
		
		_log.debug("update Skin" + sql);
		
		PortalLocalServiceUtil.executeUpdateQuery(sql);
		
		String whereClauseSQL = String.format(WHERE_SKINS_BY_SKINID, skinid);
		sql = String.format(GET_SKINS, (PropsValues.IS_PREVIEW_ENVIRONMENT ? ", '?env=preview'" : ", '?env=live'"), whereClauseSQL);
		
		Document result = PortalLocalServiceUtil.executeQueryAsDom( sql , new String[]{"clickscript"} );
		
		MetadataAdvertisementTools.updateSkin(result);
		
		return result.asXML();
	}
	
	public String deleteSkins(String xmlData) throws Exception
	{
		Document dom = SAXReaderUtil.read(xmlData);
		ClusterMgr.processConfig(ClusterMgrType.AD_SKIN, ClusterMgrOperation.DELETE, dom);
		
		Element dataRoot = dom.getRootElement();
		XPath xpath = SAXReaderUtil.createXPath("//row/@skinid");
		
		List<Node> nodes = xpath.selectNodes(dataRoot);
		ErrorRaiser.throwIfFalse(nodes != null && nodes.size() > 0);

		boolean checkreferences = GetterUtil.getBoolean(XMLHelper.getTextValueOf(dataRoot, "@checkreferences"), true);
		checkDependencies(dataRoot, SKIN, false, checkreferences);
		
		String inClauseSQL = TeaserMgr.getInClauseSQL(nodes);

		addToDeleteList(inClauseSQL, "adskin", "skinid");
		
		String sql = String.format(DELETE_SKINS, inClauseSQL);
		
		_log.debug("delete Skins: " + sql);
		
		PortalLocalServiceUtil.executeUpdateQuery(sql);
		
		return xmlData;
	}
	
	public String createDefaultSkinConfig(String xmlData) throws DocumentException, ServiceError
	{
		Document d = SAXReaderUtil.read(xmlData);
		
		Element dataRoot = d.getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row");
		List<Node> skinList = xpath.selectNodes(dataRoot);
		
		long groupid = XMLHelper.getLongValueOf(dataRoot, "@groupid");
		ErrorRaiser.throwIfFalse(groupid>0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX);
		
		for (Node skin : skinList)
		{
			String skinid = XMLHelper.getTextValueOf(skin, "@skinid");
			ErrorRaiser.throwIfFalse( Validator.isNotNull(skinid), IterErrorKeys.XYZ_E_INVALID_SKIN_ID_ZYX);
		}

		String query = String.format("select ITR_CREATE_DEFAULT_SKIN_CONFIG('%s')", d.asXML());
		
		PortalLocalServiceUtil.executeQueryAsList( query );
		
		return "OK";
	}

	/*
	 * 
	 *  SLOTS_TAGS
	 *  
	 */
	public Document getSlotTagLayout(String groupid, String plid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse( Long.valueOf(groupid).longValue() > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX, "Group: " + groupid);

		if(Validator.isNull(plid))
			plid = null;
		
		String getSlotsTags = String.format(GET_SLOTS_TAGS, ASSIGNMENTS_TO_LAYOUT_TBL_NAME, PLID_COLUMN_NAME, groupid, plid);
		
		_log.debug("get SlotTag Layout: " + getSlotsTags);
		
		Document result = PortalLocalServiceUtil.executeQueryAsDom( getSlotsTags );
		
		return result;
	}
	
	public Document getSlotTagCategory(String groupid, String categoryid) throws ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse( Long.valueOf(groupid).longValue() > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX, "Group: " + groupid);
		
		if(Validator.isNull(categoryid) || categoryid.equalsIgnoreCase("ROOT"))
			categoryid = null;
		
		String getSlotsTags = String.format(GET_SLOTS_TAGS, ASSIGNMENTS_TO_CATEGORY_TBL_NAME, CATEGORYID_COLUMN_NAME, groupid, categoryid);
		
		_log.debug("get SlotTag Category: " + getSlotsTags);
		
		Document result = PortalLocalServiceUtil.executeQueryAsDom( getSlotsTags );
		
		return result;
	}
	
	private void importSlotTags(long groupId, boolean updtIfExist, List<Node> slotTags) throws Exception
	{
		if (slotTags.size() > 0)
		{
			String[] slottagIds = XMLHelper.getStringValues(slotTags, "@slottagid");
			ErrorRaiser.throwIfFalse(slotTags.size() == slottagIds.length);
			String sql   = String.format("SELECT slottagid FROM adslotadtags WHERE slottagid IN ('%s')", StringUtils.join(slottagIds, "','"));
			Document dom = PortalLocalServiceUtil.executeQueryAsDom(sql);
			
			String[] friendlyURLs = XMLHelper.getStringValues(slotTags, "@friendlyURL");
			Document domLayout = null;
			// Podría ser vacío si todos los plids eran nulos
			if (friendlyURLs.length > 0)
			{
				sql = String.format("SELECT plid, friendlyURL FROM Layout WHERE friendlyURL IN ('%s') AND groupId = %d", StringUtils.join(friendlyURLs, "','"), groupId);
				domLayout = PortalLocalServiceUtil.executeQueryAsDom(sql);
			}
			
			for (int i = 0; i < slotTags.size(); i++)
			{
				Element slottag = (Element)slotTags.get(i);
				String slottagId = slottagIds[i];
							
				boolean exist = XMLHelper.getLongValueOf(dom, String.format("count(//row[@slottagid = '%s'])", slottagId)) > 0;
				ErrorRaiser.throwIfFalse( !exist || updtIfExist, IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, String.format("%s(%s)", IterAdmin.IA_CLASS_ADSLOTTAG, slottagId));
	
				((Element)slottag).addAttribute("groupid", String.valueOf(groupId));
				
				long plid = 0;
				String friendlyURL = XMLHelper.getTextValueOf(slottag, "@friendlyURL");
				if (Validator.isNotNull(friendlyURL))
				{
					plid = XMLHelper.getLongValueOf(domLayout, String.format("//row[@friendlyURL = '%s']/@plid", friendlyURL));
					ErrorRaiser.throwIfFalse(plid > 0, IterErrorKeys.XYZ_E_UNDEFINED_LAYOUT_ZYX, friendlyURL);
				}
				slottag.addAttribute("plid", String.valueOf(plid));
				slottag.addAttribute("assignedtoid", slottag.attributeValue("plid"));
				
				if (exist)
					updateSlotTagLayout(slottag);
				else
					addSlotTagLayout(slottag);
			}
		}
	}
	
	public Document addSlotTagLayout(String xmlData) throws Exception
	{
		Document dom = SAXReaderUtil.read(xmlData);
		ClusterMgr.processConfig(ClusterMgrType.AD_SLOTTAG_LAYOUT, ClusterMgrOperation.ADD, dom);
		Node node = dom.getRootElement().selectSingleNode("row");
		Document result = addSlotTagLayout(node);

		long groupid = XMLHelper.getLongValueOf(node, "@groupid");
		// Actualizamos la fecha de publicación para que los slots de adContainers que estén en catálogos vean los cambios realizados
		GroupMgr.updatePublicationDate(groupid, new Date() );
		
		return result;
	}
	private Document addSlotTagLayout(Node node) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException, ParseException
	{
		_log.debug("add SlotTag Layout");
		return addSlotTagAssignment(node, 
									ADD_SLOTS_TAGS_LAYOUT,
									String.format(GET_SLOT_TAG_BY_SLOTTAGID, PLID_COLUMN_NAME, ASSIGNMENTS_TO_LAYOUT_TBL_NAME),
									String.format(GET_PRIORITY, ASSIGNMENTS_TO_LAYOUT_TBL_NAME, PLID_COLUMN_NAME),
									String.format(CHECK_UNQ_SLOT_TAG, ASSIGNMENTS_TO_LAYOUT_TBL_NAME, PLID_COLUMN_NAME));
	}
	
	private void importSlotTagsCategory(long groupId, boolean updtIfExist, List<Node> slotTags) throws Exception
	{
		if (slotTags.size() > 0)
		{
			String[] slottagIds = XMLHelper.getStringValues(slotTags, "@slottagid");
			ErrorRaiser.throwIfFalse(slotTags.size() == slottagIds.length);
			String sql   = String.format("SELECT slottagid FROM adslottagcategory WHERE slottagid IN ('%s')", StringUtils.join(slottagIds, "','"));
			Document dom = PortalLocalServiceUtil.executeQueryAsDom(sql);
			
			for (int i = 0; i < slotTags.size(); i++)
			{
				Element slottag = (Element)slotTags.get(i);
				String slottagId = slottagIds[i];
							
				boolean exist = XMLHelper.getLongValueOf(dom, String.format("count(//row[@slottagid = '%s'])", slottagId)) > 0;
				ErrorRaiser.throwIfFalse( !exist || updtIfExist, IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, String.format("%s(%s)", IterAdmin.IA_CLASS_ADSLOTTAG_CATEGORY, slottagId));
	
				((Element)slottag).addAttribute("groupid", String.valueOf(groupId));
				
				String categoryId = "ROOT";
				String categoryPath = XMLHelper.getTextValueOf(slottag, "@categoryPath");
				if (Validator.isNotNull(categoryPath))
				{
					sql = String.format(GET_CATEGORY_FROM_PATH, groupId, categoryPath);
					categoryId = PortalLocalServiceUtil.executeQueryAsList(sql).get(0).toString();
				}
				
				slottag.addAttribute("assignedtoid", categoryId);
				
				if (exist)
					updateSlotTagCategory(slottag);
				else
					addSlotTagCategory(slottag);
			}
		}
	}

	public Document addSlotTagCategory(String xmlData) throws Exception
	{
		Document dom = SAXReaderUtil.read(xmlData);
		ClusterMgr.processConfig(ClusterMgrType.AD_SLOTTAG_CATEGORY, ClusterMgrOperation.ADD, dom);
		Node node = dom.getRootElement().selectSingleNode("row");
		Document result = addSlotTagCategory(node);

		long groupid = XMLHelper.getLongValueOf(node, "@groupid");
		// Actualizamos la fecha de publicación para que los slots de adContainers que estén en catálogos vean los cambios realizados
		GroupMgr.updatePublicationDate(groupid, new Date() );
		
		return result;
	}
	private Document addSlotTagCategory(Node node) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException, ParseException
	{
		_log.debug("add SlotTag Category");
		Document d = addSlotTagAssignment(node, 
									String.format(ADD_SLOTS_TAGS_CATEGORY, GET_ADVOCID_FROM_CATID),
									String.format(GET_SLOT_TAG_BY_SLOTTAGID, CATEGORYID_COLUMN_NAME, ASSIGNMENTS_TO_CATEGORY_TBL_NAME),
									String.format(GET_PRIORITY, ASSIGNMENTS_TO_CATEGORY_TBL_NAME, CATEGORYID_COLUMN_NAME),
									String.format(CHECK_UNQ_SLOT_TAG, ASSIGNMENTS_TO_CATEGORY_TBL_NAME, CATEGORYID_COLUMN_NAME));
		
		MetadataAdvertisementTools.addMetadataAssignment(d);
		
		return d;
	}
	
	private Document addSlotTagAssignment(Node node, String addQuery, String getQuery, String priorityQuery, String checkQuery) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException, ParseException
	{
		long groupid = XMLHelper.getLongValueOf(node, "@groupid");
		ErrorRaiser.throwIfFalse(groupid > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX);
		
		String slotid = XMLHelper.getTextValueOf(node, "@slotid");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(slotid), IterErrorKeys.XYZ_E_INVALID_SLOT_ID_ZYX);
		
		String tagid = XMLHelper.getTextValueOf(node, "@tagid");
		if( Validator.isNotNull(tagid) && !tagid.isEmpty() )
			tagid = "'" + tagid + "'";
		
		String skinid = XMLHelper.getTextValueOf(node, "@skinid");
		if( Validator.isNotNull(skinid) && !skinid.isEmpty() )
			skinid = "'" + skinid + "'";
		
		String assignedToId = XMLHelper.getTextValueOf(node, "@assignedtoid");
		if( assignedToId.equals("0") || assignedToId.equalsIgnoreCase("ROOT") )
			assignedToId = StringPool.NULL;
		
		long priority = GetterUtil.getLong(XMLHelper.getTextValueOf(node, "@priority"), 0);
		if(priority == 0)
			priority = getMaxPriority(priorityQuery, slotid, assignedToId) + 1;

		String enabled = XMLHelper.getTextValueOf(node, "@enabled");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(enabled), IterErrorKeys.XYZ_E_INVALID_SLOTTAG_ENABLED_ZYX);
		
		String vdesde = "";
		String vigenciadesde = XMLHelper.getTextValueOf(node, "@vigenciadesde");
		if( Validator.isNotNull(vigenciadesde) && !vigenciadesde.isEmpty() )
		{
			vdesde = vigenciadesde;
			vigenciadesde = "'" + vigenciadesde + "'";
		}
		
		String vhasta = "";
		String vigenciahasta = XMLHelper.getTextValueOf(node, "@vigenciahasta");
		if( Validator.isNotNull(vigenciahasta) && !vigenciahasta.isEmpty() )
		{
			vhasta = vigenciahasta;
			vigenciahasta = "'" + vigenciahasta + "'";
		}
		
		if( !vdesde.isEmpty() && !vhasta.isEmpty() )
		{
			SimpleDateFormat sdf = new SimpleDateFormat(BBDD_FORMAT);
			long dateini = sdf.parse(vdesde).getTime();
			long datefin = sdf.parse(vhasta).getTime();
			ErrorRaiser.throwIfFalse( dateini<datefin , IterErrorKeys.XYZ_ITR_INVALID_DATE_RANGE_ZYX);
		}

		checkUniqueSlotTag(checkQuery, true, StringPool.BLANK, slotid, tagid, skinid, assignedToId, vigenciadesde, vigenciahasta);
		
		String uuid = XMLHelper.getTextValueOf(node, "@slottagid");
		if(Validator.isNull(uuid))
			uuid = SQLQueries.getUUID();
		
		String sql = String.format(addQuery, uuid, slotid, skinid, tagid, assignedToId, priority, enabled.equalsIgnoreCase("true")?1:0, vigenciadesde, vigenciahasta, SQLQueries.getCurrentDate(), ", priority=VALUES(priority)", groupid);
		
		_log.debug("addSlotTagAssignment: " + sql);
		
		PortalLocalServiceUtil.executeUpdateQuery( sql );
		
		Document result = PortalLocalServiceUtil.executeQueryAsDom( String.format(getQuery, uuid) );
				
		return result;
	}
	
	public Document updateSlotTagLayout(String xmlData) throws Exception
	{
		Document dom = SAXReaderUtil.read(xmlData);
		ClusterMgr.processConfig(ClusterMgrType.AD_SLOTTAG_LAYOUT, ClusterMgrOperation.UPDATE, dom);
		return updateSlotTagLayout(dom.getRootElement().selectSingleNode("row"));
	}
	public Document updateSlotTagLayout(Node node) throws DocumentException, IOException, SQLException, ServiceError, SecurityException, NoSuchMethodException, ParseException
	{
		return updateSlotTagAssignment( node,
										ADD_SLOTS_TAGS_LAYOUT,
										String.format(GET_SLOT_TAG_BY_SLOTTAGID, PLID_COLUMN_NAME, ASSIGNMENTS_TO_LAYOUT_TBL_NAME),
										String.format(CHECK_UNQ_SLOT_TAG, ASSIGNMENTS_TO_LAYOUT_TBL_NAME, PLID_COLUMN_NAME) );
	}
	
	public Document updateSlotTagCategory(String xmlData) throws Exception
	{
		Document dom = SAXReaderUtil.read(xmlData);
		ClusterMgr.processConfig(ClusterMgrType.AD_SLOTTAG_CATEGORY, ClusterMgrOperation.UPDATE, dom);
		return updateSlotTagCategory(dom.getRootElement().selectSingleNode("row")); 
	}
	private Document updateSlotTagCategory(Node node) throws DocumentException, IOException, SQLException, ServiceError, SecurityException, NoSuchMethodException, ParseException
	{
		Document d = updateSlotTagAssignment( node,
										String.format(ADD_SLOTS_TAGS_CATEGORY, GET_ADVOCID_FROM_CATID),
										String.format(GET_SLOT_TAG_BY_SLOTTAGID, CATEGORYID_COLUMN_NAME, ASSIGNMENTS_TO_CATEGORY_TBL_NAME),
										String.format(CHECK_UNQ_SLOT_TAG, ASSIGNMENTS_TO_CATEGORY_TBL_NAME, CATEGORYID_COLUMN_NAME) );
		
		MetadataAdvertisementTools.updateMetadataAssignment(d);
		
		return d;
	}
	
	private Document updateSlotTagAssignment(Node node, String updateQuery, String getQuery, String checkQuery) throws DocumentException, IOException, SQLException, ServiceError, SecurityException, NoSuchMethodException, ParseException
	{
		long groupid = XMLHelper.getLongValueOf(node, "@groupid");
		ErrorRaiser.throwIfFalse(groupid > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX);
		
		String slotTagId = XMLHelper.getTextValueOf(node, "@slottagid");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(slotTagId), IterErrorKeys.XYZ_E_INVALID_SLOTTAG_ID_ZYX);
		
		String slotid = XMLHelper.getTextValueOf(node, "@slotid");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(slotid), IterErrorKeys.XYZ_E_INVALID_SLOT_ID_ZYX);
		
		String tagid = XMLHelper.getTextValueOf(node, "@tagid");
		if( Validator.isNotNull(tagid) && !tagid.isEmpty() )
			tagid = "'" + tagid + "'";
		
		String skinid = XMLHelper.getTextValueOf(node, "@skinid");
		if( Validator.isNotNull(skinid) && !skinid.isEmpty() )
			skinid = "'" + skinid + "'";
		
		String assignedToId = XMLHelper.getTextValueOf(node, "@assignedtoid");
		if( assignedToId.equals("0") || assignedToId.equalsIgnoreCase("ROOT") )
			assignedToId = StringPool.NULL;
		
		String enabled = XMLHelper.getTextValueOf(node, "@enabled");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(enabled), IterErrorKeys.XYZ_E_INVALID_SLOTTAG_ENABLED_ZYX);
		
		String vdesde = "";
		String vigenciadesde = XMLHelper.getTextValueOf(node, "@vigenciadesde");
		if( Validator.isNotNull(vigenciadesde) && !vigenciadesde.isEmpty() )
		{
			vdesde = vigenciadesde;
			vigenciadesde = "'" + vigenciadesde + "'";
		}
		
		String vhasta = "";
		String vigenciahasta = XMLHelper.getTextValueOf(node, "@vigenciahasta");
		if( Validator.isNotNull(vigenciahasta) && !vigenciahasta.isEmpty() )
		{
			vhasta = vigenciahasta;
			vigenciahasta = "'" + vigenciahasta + "'";
		}
		
		if( !vdesde.isEmpty() && !vhasta.isEmpty() )
		{
			SimpleDateFormat sdf = new SimpleDateFormat(BBDD_FORMAT);
			long dateini = sdf.parse(vdesde).getTime();
			long datefin = sdf.parse(vhasta).getTime();
			ErrorRaiser.throwIfFalse( dateini<datefin , IterErrorKeys.XYZ_ITR_INVALID_DATE_RANGE_ZYX);
		}
		
		String priority = XMLHelper.getTextValueOf(node, "@priority");
		String priorityValue = ", priority=VALUES(priority)";
		if(Validator.isNull(priority))
			priorityValue = StringPool.BLANK;
			
		
		checkUniqueSlotTag(checkQuery, false, slotTagId, slotid, tagid, skinid, assignedToId, vigenciadesde, vigenciahasta);

		String sql = String.format(updateQuery, slotTagId, slotid, skinid, tagid, assignedToId, priority, enabled.equalsIgnoreCase("true")?1:0, vigenciadesde, vigenciahasta, SQLQueries.getCurrentDate(), priorityValue, groupid);
		
		_log.debug("Update assigment: " + sql);
		
		PortalLocalServiceUtil.executeUpdateQuery(sql);
		
		Document result = PortalLocalServiceUtil.executeQueryAsDom( String.format(getQuery, slotTagId) );
				
		return result;
	}
	
	public Document updatePrioritySlotTagLayout(String xmlData) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		return updatePrioritySlotTag(xmlData,
									 ASSIGNMENTS_TO_LAYOUT_TBL_NAME,
									 PLID_COLUMN_NAME,
									 String.format(GET_PRIORITY, ASSIGNMENTS_TO_LAYOUT_TBL_NAME, PLID_COLUMN_NAME));
	}
	
	public Document updatePrioritySlotTagCategory(String xmlData) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		Document d =  updatePrioritySlotTag(xmlData,
									 ASSIGNMENTS_TO_CATEGORY_TBL_NAME,
									 CATEGORYID_COLUMN_NAME,
									 String.format(GET_PRIORITY, ASSIGNMENTS_TO_CATEGORY_TBL_NAME, CATEGORYID_COLUMN_NAME));
		
		MetadataAdvertisementTools.updateMetadataAssignment(d);
		
		return d;
	}
	
	private Document updatePrioritySlotTag(String xmlData, String tblName, String columnName, String priorityQuery) throws DocumentException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		Document retVal = SAXReaderUtil.createDocument("rs");
		
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row");
		Node node = xpath.selectSingleNode(dataRoot);
		
		long groupid = XMLHelper.getLongValueOf(node, "@groupid");
		ErrorRaiser.throwIfFalse(groupid > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX);
		
		String assignedToId = XMLHelper.getTextValueOf(node, "@assignedtoid");
		if( assignedToId.equals("0") || assignedToId.equalsIgnoreCase("ROOT") )
			assignedToId = StringPool.NULL;
		
		//Elemento que vamos a mover
		String slotTagId = XMLHelper.getTextValueOf(node, "@slottagid");
		ErrorRaiser.throwIfFalse( Validator.isNotNull(slotTagId), IterErrorKeys.XYZ_E_INVALID_SLOTTAG_ID_ZYX);
		
		String sql = String.format("SELECT slotid, priority FROM %s WHERE slottagid = '%s'", tblName, slotTagId);
 		List<Object> item = PortalLocalServiceUtil.executeQueryAsList(sql);
 		ErrorRaiser.throwIfFalse(item.size() > 0, IterErrorKeys.XYZ_E_SOURCE_NOT_FOUND_ZYX);
 		long currentPriority = Long.parseLong( ((Object[])item.get(0))[1].toString() );
		String currentslot = ((Object[])item.get(0))[0].toString();
 		
		//Elemento de referencia. El elemento a mover quedará encima de este.
		String refid = XMLHelper.getTextValueOf(node, "@refid");
		long refPriority = 0;
		
		if( Validator.isNotNull(refid) && !refid.isEmpty() )
		{
			sql = String.format("SELECT priority FROM %s WHERE slottagid = '%s'", tblName, refid);
			item = PortalLocalServiceUtil.executeQueryAsList(sql);
	 		ErrorRaiser.throwIfFalse(item.size() > 0, IterErrorKeys.XYZ_E_TARGET_NOT_FOUND_ZYX);
	 		refPriority = Long.parseLong( item.get(0).toString() );
		}
		else
		{
			refPriority = getMaxPriority(priorityQuery, currentslot, assignedToId) + 1;
		}
 		
 		long ini = 0;
 		long fin = 0;
 		String oper = "";
 		String updtItemIdx = "";
// 		String getReorderedItems = new StringBuilder(GET_UPDATED_SLOTS_TAGS).append(whereclause[0]).append(SLOT_TAGS_ORDER_BY_PRIORITY).toString();
 		String getReorderedItems = "";
 		String modifiedDate = SQLQueries.getCurrentDate();
 		
 		if( refPriority!=currentPriority )
 		{
 			if( refPriority > currentPriority )
 	 		{
 	 			ini = currentPriority+1;
 	 			fin = refPriority-1;
 	 			oper="-";
 	 			updtItemIdx = String.format("UPDATE %s SET priority=%d, modifieddate='%s' WHERE slottagid='%s'", tblName, fin, modifiedDate, slotTagId);
 	 			getReorderedItems = String.format(GET_UPDATED_SLOTS_TAGS, columnName, tblName, currentslot, ini-1, fin, assignedToId);
 	 		}
 	 		else if ( refPriority < currentPriority )
 	 		{
 	 			ini = refPriority;
 	 			fin = currentPriority-1;
 	 			oper="+";
 	 			updtItemIdx = String.format("UPDATE %s SET priority=%d, modifieddate='%s' WHERE slottagid='%s'", tblName, ini, modifiedDate, slotTagId);
 	 			getReorderedItems = String.format(GET_UPDATED_SLOTS_TAGS, columnName, tblName, currentslot, ini, fin+1, assignedToId);
 	 		}
 	 		
 	 		if( ini <= fin)
 	 		{
	 			String updtSlotTagsPriority = String.format(UPDATE_SLOTS_TAGS_PRIORITY, tblName, currentslot, oper, modifiedDate, ini, fin, slotTagId, columnName, assignedToId);
	 	 		PortalLocalServiceUtil.executeUpdateQuery( updtSlotTagsPriority );
	 	 		
	 	 		PortalLocalServiceUtil.executeUpdateQuery( updtItemIdx );
	 	 		
	 	 		retVal = PortalLocalServiceUtil.executeQueryAsDom( getReorderedItems );
 	 		}
 		}
 		
 		// Actualizamos la fecha de publicación para que los adContainers que estén en catálogos vean los cambios realizados
 		GroupMgr.updatePublicationDate(groupid, new Date() );
 		
 		return retVal;
	}
	
	public Document deleteSlotTagLayout(String xmlData) throws Exception
	{
		Document dom = SAXReaderUtil.read(xmlData);
		ClusterMgr.processConfig(ClusterMgrType.AD_SLOTTAG_LAYOUT, ClusterMgrOperation.DELETE, dom);
		return deleteSlotTagAssignment( xmlData,
										ASSIGNMENTS_TO_LAYOUT_TBL_NAME,
										PLID_COLUMN_NAME,
										"ITR_DELETE_ADSLOTADTAGS");
	}
	
	public Document deleteSlotTagCategory(String xmlData) throws Exception
	{
		Document dom = SAXReaderUtil.read(xmlData);
		ClusterMgr.processConfig(ClusterMgrType.AD_SLOTTAG_CATEGORY, ClusterMgrOperation.DELETE, dom);
		return deleteSlotTagAssignment( xmlData,
										ASSIGNMENTS_TO_CATEGORY_TBL_NAME,
										CATEGORYID_COLUMN_NAME,
										"ITR_DELETE_ADSLOTTAGCATEGORY");
	}
	
	private Document deleteSlotTagAssignment(String xmlData, String tblName, String columnName, String deleteFunc) throws ServiceError, IOException, SQLException, DocumentException, SecurityException, NoSuchMethodException
	{
		Document retVal = null;
		
		Document d = SAXReaderUtil.read(xmlData);
		Element dataRoot = d.getRootElement();
		
		long groupid = XMLHelper.getLongValueOf(dataRoot, "@groupid");
		ErrorRaiser.throwIfFalse(groupid > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX);

		XPath xpath = SAXReaderUtil.createXPath("//row/@slottagid");
		
		List<Node> nodes = xpath.selectNodes(dataRoot);
		ErrorRaiser.throwIfFalse(nodes != null && nodes.size() > 0);
		
		String inClauseSQL = TeaserMgr.getInClauseSQL(nodes);

		String addToDelete = 
			new StringBuilder( "INSERT INTO addeleted(adid, adtable, groupid) " ).
						append( " SELECT st.slottagid, '%1$s', s.groupid  " ).
						append( " FROM %1$s st INNER JOIN adslot s ON (st.slotid=s.slotid) ").
						append( " WHERE slottagid IN %2$s AND st.publicationdate IS NOT NULL" ).toString();
		
		PortalLocalServiceUtil.executeUpdateQuery( String.format(addToDelete, tblName, inClauseSQL) );
		
		String grpid = "";
		String assignedToId = "";
		String slotTagId = nodes.get(0).getStringValue();
		String query = String.format("SELECT s.groupid, st.%s FROM %s st INNER JOIN adslot s ON ( st.slottagid='%s' AND st.slotid=s.slotid)", columnName, tblName, slotTagId);
		List<Object> lnodes = PortalLocalServiceUtil.executeQueryAsList( query );
		if(lnodes != null && lnodes.size() > 0)
		{
			grpid = ((Object[])lnodes.get(0))[0].toString();
			assignedToId = Validator.isNotNull( ((Object[])lnodes.get(0))[1] ) ? ((Object[])lnodes.get(0))[1].toString() : "";
		}
		
		query = String.format("select %s('%s')", deleteFunc, d.asXML());
		PortalLocalServiceUtil.executeQueryAsList( query );
		
		if( !grpid.isEmpty() )
		{
			if(tblName.equals(ASSIGNMENTS_TO_LAYOUT_TBL_NAME))
				retVal = getSlotTagLayout(grpid, assignedToId);
			else
			{
				retVal = getSlotTagCategory(grpid, assignedToId);
				MetadataAdvertisementTools.deleteAssignments(grpid, assignedToId, retVal);
			}
		}
		
		// Actualizamos la fecha de publicación para que los adContainers que estén en catálogos vean los cambios realizados
		 GroupMgr.updatePublicationDate(groupid, new Date() );

		return retVal;
	}
	
	private long getMaxPriority(String query, String slotId, String assignedTo)
	{
		String getPriority = String.format(query, slotId, assignedTo);
		
		List<Object> info = PortalLocalServiceUtil.executeQueryAsList( getPriority );
		
 		long orden = (info.size() > 0) ? Long.valueOf(info.get(0).toString()) : 0;
 		return orden;
	}

	private void addToDeleteList(String inClauseSQL, String tableName, String uuidName) throws ServiceError, IOException, SQLException
	{
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
		{
			String query = String.format(ADD_TO_DELETE, uuidName, tableName, inClauseSQL);
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
	}
	
	public Document exportData(Long groupId) throws DocumentException, SecurityException, NoSuchMethodException
	{
		Element rs = SAXReaderUtil.read("<rs/>").getRootElement();
		
		rs.add( ContextVarsMgrLocalServiceUtil.exportData(groupId).getRootElement() );
		
		// Se exporta la publicidad como tal
		Element ads = rs.addElement("ads");
		//	<rs>
		//		<adslot/>
		//		<adtags/>
		//		<adfileentry/>
		//		<adskin/>
		//		<adslotadtags/>
		//		<adslottagcategory/>
		//	</rs>
		ads.add(getExportContents(String.format(GET_ALL_SLOTS, 				groupId), "adslot"));
		ads.add(getExportContents(String.format(GET_ALL_TAGS, 				groupId), "adtags", new String[]{"tagscript", "faketagscript"}));
		ads.add(getExportContents(String.format(GET_ALL_FILE_ENTRIES, 		groupId), "adfileentry"));
		ads.add(getExportContents(String.format(GET_ALL_SKINS, 				groupId), "adskin", new String[]{"clickscript"}));
		ads.add(getExportContents(String.format(GET_ALL_SLOTS_TAGS, 		groupId), ASSIGNMENTS_TO_LAYOUT_TBL_NAME));
		ads.add(getExportContents(String.format(GET_ALL_SLOTS_TAGS_CATEGORY,groupId), ASSIGNMENTS_TO_CATEGORY_TBL_NAME));
//		ads.add(getExportContents(String.format(GET_ALL_VOCS, 				groupId), TBL_ADVOCABULARY));
		
		Node vocabularies = rs.selectSingleNode("ctxvars/advocabulary").clone();
		ads.add(vocabularies);
		
		if(_log.isTraceEnabled())
			_log.trace("Export XML data: " + rs.asXML());
		
		return rs.getDocument();
	}
	
	public void importData(String data) throws Exception
	{
		Element root = SAXReaderUtil.read( data ).getRootElement();
		long groupId = XMLHelper.getLongValueOf(root, "@groupId");
        if (groupId <= 0)
        {
        	String groupName = XMLHelper.getStringValueOf(root, "@groupName");
        	ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        	groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), groupName).getGroupId();
        }
        ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		boolean updtIfExist		= GetterUtil.getBoolean(XMLHelper.getStringValueOf(root, "@updtIfExist"));
		String filesPath		= XMLHelper.getStringValueOf(root, "@filesPath");
		ErrorRaiser.throwIfNull(filesPath, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);

		// Se importan las variables de contexto
		ContextVarsMgrLocalServiceUtil.importData(groupId, updtIfExist, filesPath, (Element)root.selectSingleNode("ctxvars"));
		
		// Se importa la publicidad en sí
		importData(groupId, updtIfExist, filesPath, (Element)root.selectSingleNode("ads"));
	}
	
	private void importData(long groupId, boolean updtIfExist, String filesPath, Element data) throws Exception
	{
		long userId = GroupMgr.getDefaultUserId();
		
		// Se importan las imágenes
		List<Node> nodes = data.selectNodes("adfileentry/row");
		for (Node img : nodes)
			importImage(groupId, userId, filesPath, img, updtIfExist);
		
		// Slots
		importSlots(groupId, updtIfExist, data.selectNodes("adslot/row"));

		// Tags
		importTags(groupId, updtIfExist, data.selectNodes("adtags/row[@superid='']"));
		importTags(groupId, updtIfExist, data.selectNodes("adtags/row[@superid!='']"));
		
		// Skins
		importSkins(groupId, updtIfExist, data.selectNodes("adskin/row"));

		// SlotTags
		importSlotTags(groupId, updtIfExist, data.selectNodes(String.format("%s/row", ASSIGNMENTS_TO_LAYOUT_TBL_NAME)));
		
		// Advocabulary
		ContextVarsMgrLocalServiceUtil.importAdVocabularies(groupId, updtIfExist, data.selectNodes(String.format("%s/row", TBL_ADVOCABULARY)));
		
		// SlotTagsCategory
		importSlotTagsCategory(groupId, updtIfExist, data.selectNodes(String.format("%s/row", ASSIGNMENTS_TO_CATEGORY_TBL_NAME)));
	}
	
	private void createUpdateContents(String temporaryPath, long scopeGroupId, long userId, Element rs) throws Exception
	{
		// adfileentry
		List<Node> nodes = rs.selectNodes("adfileentry/row");
		for (Node img : nodes)
			importImage(scopeGroupId, userId, temporaryPath, img, true);

		//adslot
		XPath xpath = SAXReaderUtil.createXPath("//adslot/row");
		nodes = xpath.selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
		{
			for(Node row:nodes)
			{
				if(_log.isTraceEnabled())
					_log.trace("slot: " + row.asXML());
				
				String operation = XMLHelper.getTextValueOf(row, "@operation");
				String xmlData = SQLQueries.getLiveWellFormedRow(scopeGroupId, row);
				
				if(operation.toLowerCase().equals(IterKeys.CREATE))
					addSlot(xmlData);
				else if(operation.toLowerCase().equals(IterKeys.UPDATE))
					updateSlot(xmlData);
			}
		}
		
		//adtags sin superid
		xpath = SAXReaderUtil.createXPath("//adtags/row[@superid='']");
		nodes = xpath.selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
		{
			for(Node row:nodes)
			{
				if(_log.isTraceEnabled())
					_log.trace("tag: " + row.asXML());
				
				String operation = XMLHelper.getTextValueOf(row, "@operation");
				String xmlData = SQLQueries.getLiveWellFormedRow(scopeGroupId, row);
				
				if(operation.toLowerCase().equals(IterKeys.CREATE))
					addTag(xmlData);
				else if(operation.toLowerCase().equals(IterKeys.UPDATE))
					updateTag(xmlData);
			}
		}
		
		//adtags con superid
		xpath = SAXReaderUtil.createXPath("//adtags/row[@superid!='']");
		nodes = xpath.selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
		{
			for(Node row:nodes)
			{
				if(_log.isTraceEnabled())
					_log.trace("tag-super : " + row.asXML());
				
				String operation = XMLHelper.getTextValueOf(row, "@operation");
				String xmlData = SQLQueries.getLiveWellFormedRow(scopeGroupId, row);
				
				if(operation.toLowerCase().equals(IterKeys.CREATE))
					addTag(xmlData);
				else if(operation.toLowerCase().equals(IterKeys.UPDATE))
					updateTag(xmlData);
			}
		}
		
		//adskin
		xpath = SAXReaderUtil.createXPath("//adskin/row");
		nodes = xpath.selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
		{
			for(Node row:nodes)
			{
				if(_log.isTraceEnabled())
					_log.trace("skin: " + row.asXML());
				
				String operation = XMLHelper.getTextValueOf(row, "@operation");
				String xmlData = SQLQueries.getLiveWellFormedRow(scopeGroupId, row);
				
				if(operation.toLowerCase().equals(IterKeys.CREATE))
					addSkin(xmlData);
				else if(operation.toLowerCase().equals(IterKeys.UPDATE))
					updateSkin(xmlData);
			}
		}
		
		//adslotadtags
		xpath = SAXReaderUtil.createXPath(String.format("//%s/row", ASSIGNMENTS_TO_LAYOUT_TBL_NAME));
		nodes = xpath.selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
		{
			for(Node row:nodes)
			{
				if(_log.isTraceEnabled())
					_log.trace("slot-tag-layout: " + row.asXML());
				
				String operation = XMLHelper.getTextValueOf(row, "@operation");
				Element xmlData = SQLQueries.getLiveWellFormedRowAsElement(scopeGroupId, row, "plid", IterKeys.CLASSNAME_LAYOUT, IterErrorKeys.XYZ_E_LAYOUT_NOT_FOUND_IN_LIVE_ZYX);
				Element plidRow = (Element)xmlData.selectSingleNode("//row");
				String assignedId = plidRow.attributeValue("plid");
				plidRow.addAttribute("assignedtoid", Validator.isNotNull(assignedId) ? assignedId : "0");
				
				if(operation.toLowerCase().equals(IterKeys.CREATE))
					addSlotTagLayout(xmlData.asXML());
				else if(operation.toLowerCase().equals(IterKeys.UPDATE))
					updateSlotTagLayout(xmlData.asXML());
			}
		}
		
		//advocabulary
		xpath = SAXReaderUtil.createXPath(String.format("//%s/row", TBL_ADVOCABULARY));
		nodes = xpath.selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
		{
			for(Node row:nodes)
			{
				if(_log.isTraceEnabled())
					_log.trace("ad-vocabulary: " + row.asXML());
				
				Element xmlData = SQLQueries.getLiveWellFormedRowAsElement(scopeGroupId, row, "vocabularyid", IterKeys.CLASSNAME_VOCABULARY, IterErrorKeys.XYZ_E_VOCABULARY_NOT_FOUND_IN_LIVE_ZYX);
				xmlData.addAttribute("groupid", String.valueOf(scopeGroupId));
				addAdvertisementVocabulary(xmlData.asXML());
			}
		}
		
		//adslottagCategory
		xpath = SAXReaderUtil.createXPath(String.format("//%s/row", ASSIGNMENTS_TO_CATEGORY_TBL_NAME));
		nodes = xpath.selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
		{
			for(Node row:nodes)
			{
				if(_log.isTraceEnabled())
					_log.trace("slot-tag-category: " + row.asXML());
				
				String operation = XMLHelper.getTextValueOf(row, "@operation");
				Element xmlData = SQLQueries.getLiveWellFormedRowAsElement(scopeGroupId, row, "categoryid", IterKeys.CLASSNAME_CATEGORY, IterErrorKeys.XYZ_E_CATEGORY_NOT_FOUND_IN_LIVE_ZYX);
				Element catidRow = (Element)xmlData.selectSingleNode("//row");
				String assignedId = catidRow.attributeValue("categoryid");
				catidRow.addAttribute("assignedtoid", Validator.isNotNull(assignedId) ? assignedId : "ROOT");
				
				
				if(operation.toLowerCase().equals(IterKeys.CREATE))
					addSlotTagCategory(xmlData.asXML());
				else if(operation.toLowerCase().equals(IterKeys.UPDATE))
					updateSlotTagCategory(xmlData.asXML());
			}
		}
		
	}
	
	private void deleteContents(long scopeGroupId, Element rs) throws IOException, SQLException, PortalException, SystemException
	{
		XPath xpath = SAXReaderUtil.createXPath("//addeleted/row[@adtable='adfileentry']/@adid");
		List<Node> nodes = xpath.selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
		{
			String query = String.format(GET_FILE_ENTRIES_UUIDS, TeaserMgr.getInClauseSQL(nodes));
			List<Object> dlfileentriesUUIDs = PortalLocalServiceUtil.executeQueryAsList(query);
			if(dlfileentriesUUIDs != null && dlfileentriesUUIDs.size() > 0)
			{
				for(Object currentUUID:dlfileentriesUUIDs)
				{
					DLFileEntry dlFileEntry = DLFileEntryLocalServiceUtil.getFileEntryByUuidAndGroupId(currentUUID.toString(), scopeGroupId);
					DLFileEntryLocalServiceUtil.deleteFileEntryNoHook(dlFileEntry);
				}
			}
		}

		xpath = SAXReaderUtil.createXPath("//addeleted/row[@adtable='adslot']/@adid");
		nodes = xpath.selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
			PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_SLOTS, TeaserMgr.getInClauseSQL(nodes)));

		xpath = SAXReaderUtil.createXPath("//addeleted/row[@adtable='adtags']/@adid");
		nodes = xpath.selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
			PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_TAGS, TeaserMgr.getInClauseSQL(nodes)));

		xpath = SAXReaderUtil.createXPath("//addeleted/row[@adtable='adskin']/@adid");
		nodes = xpath.selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
			PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_SKINS, TeaserMgr.getInClauseSQL(nodes)));
		
		xpath = SAXReaderUtil.createXPath(String.format("//addeleted/row[@adtable='%s']/@adid", ASSIGNMENTS_TO_LAYOUT_TBL_NAME));
		nodes = xpath.selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
			PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_SLOTS_TAGS, ASSIGNMENTS_TO_LAYOUT_TBL_NAME, TeaserMgr.getInClauseSQL(nodes)));

		xpath = SAXReaderUtil.createXPath(String.format("//addeleted/row[@adtable='%s']/@adid", TBL_ADVOCABULARY));
		nodes = xpath.selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
			PortalLocalServiceUtil.executeUpdateQuery( 
														String.format( DELETE_VOCS, StringUtil.merge(XMLHelper.getStringValues(nodes, "."), "','") ) 
													);
		
		xpath = SAXReaderUtil.createXPath(String.format("//addeleted/row[@adtable='%s']/@adid", ASSIGNMENTS_TO_CATEGORY_TBL_NAME));
		nodes = xpath.selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
			PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_SLOTS_TAGS, ASSIGNMENTS_TO_CATEGORY_TBL_NAME, TeaserMgr.getInClauseSQL(nodes)));
		
	}

		////////////////////
	   ///				///
	  /// 	PUBLISH	   ///
	 ///			  ///
	////////////////////
	
	public JsonObject publish(String data) throws Exception
	{
		JsonObject result = null;
		boolean processingLastGroup = false;
		Document dom = SAXReaderUtil.read(data);
		
		try
		{
			ClusterMgr.processConfig(ClusterMgrType.AD_TAG, ClusterMgrOperation.PUBLISH, dom);
			processingLastGroup = true;
			publishToLive(XMLHelper.getLongValueOf(dom, "/rs/@groupid"));
		}
		catch (Exception e)
		{
			if (processingLastGroup)
				e = ClusterMgr.processApplyConfigException(e, new ArrayList<String>(Arrays.asList(XMLHelper.getStringValueOf(dom, "/rs/@groupid"))), _log);
			result = new JsonObject();
			if (e instanceof ServiceError)
				result.addProperty("code", ((ServiceError) e).getErrorCode());
			result.addProperty("msg", e.getMessage());
			result.addProperty("techinfo", ExceptionUtils.getStackTrace(e));
		}
		
		return result;
	}
	
	public void publishToLive(long scopeGroupId) throws PortalException, SystemException, SecurityException, NoSuchMethodException, IOException, DocumentException, ServiceError, SQLException
	{
		publishToLive(scopeGroupId, true);
	}
	public void publishToLive(long scopeGroupId, boolean throwEmptyPublication) throws PortalException, SystemException, SecurityException, NoSuchMethodException, IOException, DocumentException, ServiceError, SQLException
	{
		if(writeLock.tryLock())
		{
			File localFile = null;
			
			try
			{
				//Se recupera la configuración de la publicación
				Group scopeGroup = GroupLocalServiceUtil.getGroup(scopeGroupId);
				LiveConfiguration liveConf 	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(scopeGroup.getCompanyId());
				
				//Generamos el .xml de exportación
				Element rs = generateExportElement(scopeGroup);
				
				//Comprobamos que no es una publicación vacía
				PublishUtil.checkEmptyPublication(rs);
				
				//Generamos el .zip a exportar
				localFile = generateExportFile(scopeGroup, liveConf.getLocalPath(), rs);
				
				//Enviamos por FTP/File System el .zip generado
				String importFileName = PublishUtil.sendFile(liveConf, localFile);
				
				//Realizar la llamada al Live para que importe el .zip
				if (callLiveImportContents(scopeGroup, liveConf, importFileName))
				{
					//Actualizar la fecha de publicación de los contenidos exportados
					updatePublicationDateContents(rs);
				}
				else
				{
					ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_XPORTCONTENT_ALL_FAILED_ZYX);
				}
			}
			catch (ServiceError e)
			{
				// Si no es el error EMPTY o está indicado que se lance el error de EMPTY
				if (!e.getErrorCode().equals(IterErrorKeys.XYZ_E_XPORTCONTENT_EMPTY_ZYX) || throwEmptyPublication)
					throw e;
			}
			finally
			{
				writeLock.unlock();
				
				//Borramos el fichero de exportación
				PublishUtil.hotConfigDeleteFile(localFile);	
			}
		}
		else
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_PUBLISH_ALREADY_IN_PROCESS_ZYX);
		}
	}
	
	public boolean importContents(String importFileName) throws Exception
	{
		boolean success = false;
		
		File importFile = null;
		File temporaryDir = null;
		
		if(writeLock.tryLock())
		{
			try
			{
				Group scopeGroup = GroupLocalServiceUtil.getGroup(GroupMgr.getGlobalGroupId());
				LiveConfiguration liveConf 	= LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(scopeGroup.getCompanyId());

				String importFilePath = null;
				
				if (liveConf.getOutputMethod().equals(IterKeys.XMLIO_CHANNEL_MODE_FTP))
				{
					String ftpServer = liveConf.getFtpPath();
					String ftpUser = liveConf.getFtpUser();
					String ftpPassword = liveConf.getFtpPassword();
					String localPath = liveConf.getLocalPath();

					importFilePath = FTPUtil.receiveFile(ftpServer, ftpUser, ftpPassword, importFileName, localPath, StringPool.BLANK);
				} 
				else
				{
					String remotePath = liveConf.getRemotePath();
					importFilePath = remotePath + File.separatorChar + importFileName;	
				}
				
				String zipExtension = ".zip";
				
				String temporaryDirPath = importFilePath.replace(zipExtension, StringPool.BLANK);
				
				importFile = new File(importFilePath);
				temporaryDir = new File(temporaryDirPath);
				
				ZipUtil.unzip(importFile, temporaryDir);
				
				File iterXmlFile = new File(temporaryDirPath + File.separatorChar + IterKeys.XMLIO_XML_MAIN_FILE_NAME);
				Element rs = SAXReaderUtil.read(iterXmlFile).getRootElement();
	
				String groupname = XMLHelper.getTextValueOf(rs, "@groupname");
				ErrorRaiser.throwIfFalse( Validator.isNotNull(groupname), IterErrorKeys.XYZ_E_INVALID_GROUP_NAME_ZYX);
				
				Group globalGroup =  GroupLocalServiceUtil.getGroup(GroupMgr.getGlobalGroupId());
				long scopeGroupId = GroupLocalServiceUtil.getGroup(globalGroup.getCompanyId(), groupname).getGroupId();
				long userId = LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(globalGroup.getCompanyId()).getRemoteUserId();
				
				//DELETE
				deleteContents(scopeGroupId, rs);
				
				//CREATE/UPDATE
				createUpdateContents(temporaryDirPath, scopeGroupId, userId, rs);
				
				success = true;
				
				// Solo en caso satisfactorio se borran los ficheros. Ante fallos persisten para identificar y localizar dichos fallos.
				PublishUtil.hotConfigDeleteFile(temporaryDir);
				PublishUtil.hotConfigDeleteFile(importFile);
			}
			finally
			{
				// Solo se desbloquea si previamente se ha adquirido el bloqueo
				// http://jira.protecmedia.com:8080/browse/ITER-1252?focusedCommentId=53265&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-53265
				writeLock.unlock();
			}
		}
		else
		{
			ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_PUBLISH_ALREADY_IN_PROCESS_ZYX);
		}
		
		return success;
	}
	
	private void updatePublicationDateContents(Element rs) throws ServiceError, IOException, SQLException
	{
		String publicationDate = SQLQueries.getCurrentDate();

		//addeleted
		List<Node> nodes = SAXReaderUtil.createXPath("/rs/addeleted/row/@adid").selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
		{
			String query = String.format(UPDATE_TO_DELETE_PUBLISHED, TeaserMgr.getInClauseSQL(nodes));
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
		//adslot
		nodes = SAXReaderUtil.createXPath("/rs/adslot/row/@slotid").selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
		{
			String query = String.format(UPDATE_SLOTS_PUBLISHED, publicationDate, TeaserMgr.getInClauseSQL(nodes));
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
		//adtags
		nodes = SAXReaderUtil.createXPath("/rs/adtags/row/@tagid").selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
		{
			String query = String.format(UPDATE_TAGS_PUBLISHED, publicationDate, TeaserMgr.getInClauseSQL(nodes));
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
		//adskin
		nodes = SAXReaderUtil.createXPath("/rs/adskin/row/@skinid").selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
		{
			String query = String.format(UPDATE_SKINS_PUBLISHED, publicationDate, TeaserMgr.getInClauseSQL(nodes));
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
		//adslotadtags
		nodes = SAXReaderUtil.createXPath(String.format("/rs/%s/row/@slottagid", ASSIGNMENTS_TO_LAYOUT_TBL_NAME)).selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
		{
			String query = String.format(UPDATE_SLOT_TAGS_PUBLISHED, ASSIGNMENTS_TO_LAYOUT_TBL_NAME, publicationDate, TeaserMgr.getInClauseSQL(nodes));
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
		//adslottagCategory
		nodes = SAXReaderUtil.createXPath(String.format("/rs/%s/row/@slottagid", ASSIGNMENTS_TO_CATEGORY_TBL_NAME)).selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
		{
			String query = String.format(UPDATE_SLOT_TAGS_PUBLISHED, ASSIGNMENTS_TO_CATEGORY_TBL_NAME, publicationDate, TeaserMgr.getInClauseSQL(nodes));
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
		//adfileentry
		nodes = SAXReaderUtil.createXPath("/rs/adfileentry/row/@fileentryuuid").selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
		{
			String query = String.format(UPDATE_FILE_ENTRIES_PUBLISHED, publicationDate, TeaserMgr.getInClauseSQL(nodes));
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
		//advocabulary
		nodes = SAXReaderUtil.createXPath(String.format("/rs/%s/row/@advocabularyid", TBL_ADVOCABULARY)).selectNodes(rs);
		if(nodes != null && nodes.size() > 0)
		{
			String query = String.format(UPDATE_ADVOCABULARIES_PUBLISHED, publicationDate, TeaserMgr.getInClauseSQL(nodes));
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
	}
	
	private Element generateExportElement(Group scopeGroup) throws SecurityException, NoSuchMethodException, IOException, DocumentException, PortalException, SystemException
	{
		Element rs = SAXReaderUtil.read("<rs/>").getRootElement();
		
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
		{
			long scopeGroupId = scopeGroup.getGroupId();
			
			//	<rs groupname="La Razón">
			//		<addeleted/>
			//		<adslot/>
			//		<adtags/>
			//		<adfileentry/>
			//		<adskin/>
			//		<adslotadtags/>
			//		<adslottagcategory/>
			//	</rs>
			rs.addAttribute("groupname", scopeGroup.getName());	
			rs.add(getExportContents(String.format(GET_TO_DELETE, scopeGroupId), "addeleted"));
			rs.add(getExportContents(String.format(GET_SLOTS_TO_PUBLISH, "adslot", scopeGroupId), "adslot"));
			rs.add(getExportContents(String.format(GET_TAGS_TO_PUBLISH, "adtags", scopeGroupId), "adtags", new String[]{"tagscript", "faketagscript"}));
			rs.add(getExportContents(String.format(GET_FILE_ENTRIES_TO_PUBLISH, scopeGroupId), "adfileentry"));
			rs.add(getExportContents(String.format(GET_SKINS_TO_PUBLISH, "adskin", scopeGroupId), "adskin", new String[]{"clickscript"}));
			rs.add(getExportContents(
										String.format(GET_SLOTS_TAGS_LAYOUT_TO_PUBLISH, GET_PLID_GLOBALID, scopeGroupId),
										ASSIGNMENTS_TO_LAYOUT_TBL_NAME)
									);
			rs.add(getExportContents(
										String.format(GET_SLOTS_TAGS_CATEGORY_TO_PUBLISH, GET_CATEGORYID_GLOBALID, scopeGroupId),
										ASSIGNMENTS_TO_CATEGORY_TBL_NAME)
									);
			rs.add(getExportContents(String.format(GET_VOCS_TO_PUBLISH, GET_VOCABULARYID_GLOBALID, scopeGroupId),TBL_ADVOCABULARY));
		}
		
		if(_log.isTraceEnabled())
			_log.trace("Export XML data: " + rs.asXML());
		
		return rs;
	}
	
	private File generateExportFile(Group scopeGroup, String localPath, Element rs) throws SecurityException, NoSuchMethodException, IOException, DocumentException, PortalException, SystemException
	{
		File exportFile = null;
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
		{
			String zipFileName = String.format(ITER_ADVERTISEMENT_ZIP_FILE_NAME, Calendar.getInstance().getTimeInMillis());
			ZipWriter zipWriter = ZipWriterFactoryUtil.getZipWriter(new File(localPath + File.separatorChar + zipFileName));
			
			long scopeGroupId = scopeGroup.getGroupId();

			//Se añade el .xml de publicacion al .zip
			zipWriter.addEntry(IterKeys.XMLIO_XML_MAIN_FILE_NAME, rs.asXML());
			
			//Se añaden los binarios de las imágenes al .zip
			addImagesToZIP(scopeGroupId, zipWriter, rs);

			//Se obtiene el fichero liberado
			exportFile = PublishUtil.getUnlockedFile(zipWriter);
		}
		
		return exportFile;
	}
	
	private Node getExportContents(String sql, String rootName) throws SecurityException, NoSuchMethodException
	{
		return getExportContents(sql, rootName, null);
	}
	
	private Node getExportContents(String sql, String rootName, String[] nameNodes) throws SecurityException, NoSuchMethodException
	{
		Document dom = PortalLocalServiceUtil.executeQueryAsDom(sql, true, rootName, XMLHelper.rowTagName, nameNodes);
		return dom.getRootElement().detach();
	}
	
	public void addImagesToZIP(Long scopeGroupId, ZipWriter zipWriter, Node adfileentryXML) throws PortalException, SystemException, IOException
	{
		List<Node> nodes = adfileentryXML.selectNodes("adfileentry/row/@fileentryuuid");
		if(nodes != null && nodes.size() > 0)
		{
			List<Object> results = PortalLocalServiceUtil.executeQueryAsList(String.format(GET_FILE_ENTRIES_UUIDS, TeaserMgr.getInClauseSQL(nodes)));
			if(results != null && results.size() > 0)
			{
				for(Object currentResult:results)
				{
					String uuid = currentResult.toString();
					DLFileEntry dlfileEntry = DLFileEntryLocalServiceUtil.getDLFileEntryByUuidAndGroupId(uuid, scopeGroupId);
					long delegationId = GroupLocalServiceUtil.getGroup(scopeGroupId).getDelegationId();
					InputStream is = DLFileEntryLocalServiceUtil.getFileAsStream(delegationId, dlfileEntry.getUserId(), dlfileEntry.getGroupId(), dlfileEntry.getFolderId(), dlfileEntry.getName());
					zipWriter.addEntry(dlfileEntry.getTitle(), IOUtils.toByteArray(is));
					is.close();
				}
			}
		}
	}
	
	

	private void importImage(long scopeGroupId, long userId, String temporaryPath, Node adfillentry, boolean updtIfExist) throws PortalException, SystemException, ServiceError, SecurityException, NoSuchMethodException, IOException, SQLException
	{
		if(_log.isTraceEnabled())
			_log.trace("image: " + adfillentry.asXML());

		InputStream is = null;
		
		try
		{
			DLFolder dlFolder = null;
		    try
		    {
		    	dlFolder = DLFolderLocalServiceUtil.getFolder(scopeGroupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, IterKeys.ADVERTISEMENT_SKINS_FOLDER);
		    }
		    catch (NoSuchFolderException nsfe)
		    {
		    	_log.debug("Creating " + IterKeys.ADVERTISEMENT_SKINS_FOLDER + " folder...");
		    	dlFolder = DLFolderLocalServiceUtil.addFolder(userId, scopeGroupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, IterKeys.ADVERTISEMENT_SKINS_FOLDER, StringPool.BLANK, new ServiceContext());
		    }
		    
		    String description = XMLHelper.getTextValueOf(adfillentry, "@description");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(description), IterErrorKeys.XYZ_E_EMPTY_IMAGE_DESCRIPTION_ZYX);
	
		    String title = XMLHelper.getTextValueOf(adfillentry, "@title");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(title), IterErrorKeys.XYZ_E_EMPTY_IMAGE_TITLE_ZYX);
	
		    String fileentryuuid = XMLHelper.getTextValueOf(adfillentry, "@fileentryuuid");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(fileentryuuid), IterErrorKeys.XYZ_E_EMPTY_IMAGE_UUID_ZYX);
			
		    String sizeString = XMLHelper.getTextValueOf(adfillentry, "@size");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(sizeString), IterErrorKeys.XYZ_E_EMPTY_IMAGE_SIZE_ZYX);
			long size = Long.parseLong(sizeString);
			
			is = new FileInputStream(temporaryPath + File.separatorChar + title);
	
			int indexPreriod = title.indexOf(StringPool.PERIOD);
			boolean hasExtension = indexPreriod > 0 && indexPreriod < title.length();
			ErrorRaiser.throwIfFalse(hasExtension, IterErrorKeys.XYZ_E_IMAGE_WITHOUT_EXTENSION_ZYX);
			
			String fileName = SQLQueries.getUUID() + title.substring(indexPreriod, title.length());
			
			String query = String.format(GET_FILE_ENTRIES_UUIDS, "('" + fileentryuuid + "')");
			XPath xpath = SAXReaderUtil.createXPath("/rs/row/@dlfileentryuuid");
			Node dlFileEntryNode = xpath.selectSingleNode(PortalLocalServiceUtil.executeQueryAsDom(query).getRootElement());
			
			String dlfileentryuuid = null;
			if(dlFileEntryNode != null)
				dlfileentryuuid = dlFileEntryNode.getStringValue();

			// Update: Se añade un nuevo DLFileEntry, se actualiza su entrada en adfileentry y se borra el antiguo DLFileEntry
			if(Validator.isNotNull(dlfileentryuuid))
			{
				ErrorRaiser.throwIfFalse( updtIfExist, IterErrorKeys.XYZ_E_ITERADMIN_ELEMENT_ALREADY_EXIST_ZYX, String.format("%s(%s)", IterAdmin.IA_CLASS_AD_FILEENTRY, dlfileentryuuid));
				
				DLFileEntry dlfileEntry = DLFileEntryLocalServiceUtil.addFileEntry(userId, scopeGroupId, dlFolder.getFolderId(), fileName, description, StringPool.BLANK, StringPool.BLANK, is, size, new ServiceContext());
				updateDLFileEntryUUID(dlfileEntry, dlfileEntry.getUuid());
				
				query = String.format(SQLQueries.GET_ADFILEENTRY_FILEENTRYUUIDS_BY_DLFILEENTRYUUID, dlfileentryuuid);
				xpath = SAXReaderUtil.createXPath("//row/@fileentryuuid");
				Node fileEntryNode = xpath.selectSingleNode(PortalLocalServiceUtil.executeQueryAsDom(query).getRootElement());
				ErrorRaiser.throwIfNull(fileEntryNode, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
				
				fileentryuuid = fileEntryNode.getStringValue();
				
				PortalLocalServiceUtil.executeUpdateQuery(String.format(SQLQueries.UPDATE_ADFILEENTRY, dlfileEntry.getUuid(), SQLQueries.getCurrentDate(), dlfileentryuuid));
				
				DLFileEntry oldDlfileEntry = DLFileEntryLocalServiceUtil.getDLFileEntryByUuidAndGroupId(dlfileentryuuid, scopeGroupId);
				ErrorRaiser.throwIfNull(oldDlfileEntry, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
				DLFileEntryLocalServiceUtil.deleteFileEntryNoHook(oldDlfileEntry);
			}
			//Insert: Se añade un nuevo DLFileEntry y se crea su una entrada en adfileentry
			else
			{
				SQLQueries.checkDuplicateNameFileEntry(scopeGroupId, dlFolder.getFolderId(), description, dlfileentryuuid);
				
				DLFileEntry dlfileEntry = DLFileEntryLocalServiceUtil.addFileEntry(userId, scopeGroupId, dlFolder.getFolderId(), fileName, description, StringPool.BLANK, StringPool.BLANK, is, size, new ServiceContext());
				updateDLFileEntryUUID(dlfileEntry, dlfileEntry.getUuid());

				PortalLocalServiceUtil.executeUpdateQuery(String.format(SQLQueries.ADD_ADFILEENTRY, fileentryuuid, dlfileEntry.getUuid(), SQLQueries.getCurrentDate()));
			}
		}
		catch(ORMException orme)
		{
			throw orme;
		}
		catch(ServiceError se)
		{
			throw se;
		}
		catch(Throwable t)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(t.getMessage(), t), t);
		}
		finally
		{
			try
			{
				is.close();
			}
			catch(Throwable tClose){}
		}
	}
	
	private void updateDLFileEntryUUID(DLFileEntry dlfileEntry, String uuid) throws SystemException
	{
		dlfileEntry.setUuid(uuid);
		dlfileEntry.setTitle( dlfileEntry.getUuid().concat(".").concat(dlfileEntry.getExtension()) );
		DLFileEntryLocalServiceUtil.updateDLFileEntry(dlfileEntry, false);
	}

	private void checkUniqueSlotTag(String checkQuery, Boolean insert, String slottagid, String slotid, String tagid, String skinid, String assignedTo, String vigenciadesde, String vigenciahasta) throws ServiceError
	{
		if( Validator.isNull(assignedTo) || Validator.isNull(vigenciadesde) || Validator.isNull(vigenciahasta) )
		{
			tagid 			= 	Validator.isNotNull(tagid) 			? 	"=" 	+ tagid 			: "IS NULL";
			skinid			= 	Validator.isNotNull(skinid) 		? 	"=" 	+ skinid 			: "IS NULL";
			assignedTo 		= 	Validator.isNotNull(assignedTo) 	? 	"=" 	+ assignedTo 		: "IS NULL";
			vigenciadesde 	= 	Validator.isNotNull(vigenciadesde)  ? 	"=" 	+ vigenciadesde		: "IS NULL";
			vigenciahasta 	=	Validator.isNotNull(vigenciahasta)  ? 	"="		+ vigenciahasta 	: "IS NULL";
			slottagid 		= 	Validator.isNotNull(slottagid) 		? 	"!='"	+ slottagid + "'" 	: "IS NULL";

			boolean isCategoryAssignment = checkQuery.contains( ASSIGNMENTS_TO_CATEGORY_TBL_NAME );
			
			String sql = String.format(checkQuery, slotid, assignedTo, skinid, tagid, vigenciadesde, vigenciahasta, insert, slottagid);
			
			_log.debug("check unique slot-tag assignment: " + sql);
			
			List<Object> result = PortalLocalServiceUtil.executeQueryAsList(sql);
			if(result != null && result.size() > 0)
			{
				int count = Integer.valueOf(result.get(0).toString());
				String except = "";
				if(tagid.equalsIgnoreCase("IS NULL"))
					except = isCategoryAssignment ? 
								IterErrorKeys.XYZ_ITR_UNQ_SLOTID_SKINID_CATEGORYID_VIGENCIA_ENABLED_ZYX : 
								IterErrorKeys.XYZ_ITR_UNQ_ADSLOT_ADTAGS_SLOTID_SKINID_VIGENCIA_ENABLED_ZYX;
				else
					except = isCategoryAssignment ? 
								IterErrorKeys.XYZ_ITR_UNQ_SLOTID_TAGID_CATEGORYID_VIGENCIA_ENABLED_ZYX : 
								IterErrorKeys.XYZ_ITR_UNQ_ADSLOT_ADTAGS_SLOTID_TAGID_VIGENCIA_ENABLED_ZYX;
				
				ErrorRaiser.throwIfFalse(count == 0, except);
			}
		}
	}
	
	private static boolean callLiveImportContents(Group scopeGroup, LiveConfiguration liveConf, String importFileName) throws UnsupportedEncodingException, ClientProtocolException, IOException, SystemException, PortalException, DocumentException, ServiceError
	{
		boolean success = false;
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW)) 
		{
			List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
			remoteMethodParams.add(new BasicNameValuePair("serviceClassName",  	"com.protecmedia.iter.advertisement.service.AdvertisementMgrServiceUtil"));
			remoteMethodParams.add(new BasicNameValuePair("serviceMethodName", 	"importContents"));
			remoteMethodParams.add(new BasicNameValuePair("serviceParameters", 	"[importFileName]"));
			remoteMethodParams.add(new BasicNameValuePair("importFileName",		importFileName));
			
			String []url = liveConf.getRemoteIterServer2().split(":");
			HttpHost targetHost = new HttpHost(url[0], Integer.valueOf(url[1]));

			JSONObject json = JSONUtil.executeMethod(targetHost, "/xmlio-portlet/secure/json", remoteMethodParams, 
					(int)liveConf.getConnectionTimeOut(),
					(int)liveConf.getOperationTimeOut(),
					liveConf.getRemoteUserName(), liveConf.getRemoteUserPassword());

			success = Boolean.parseBoolean(json.getString("returnValue"));
			
			CacheRefresh cr = new CacheRefresh( scopeGroup.getGroupId() );
			cr.setRefreshMetadataAdvertisement(true);
			cr.setRescheduleCacheInvalidation(true);
			XMLIOUtil.deleteRemoteCache( cr );
		}
		return success;
	}
	
	public Document getAdVocBranches(String xmlData) throws NumberFormatException, ServiceError, SecurityException, NoSuchMethodException, DocumentException
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		String companyId = XMLHelper.getTextValueOf(dataRoot, "@companyId");
		ErrorRaiser.throwIfNull(companyId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String groupId = String.valueOf(GroupMgr.getGlobalGroupId());
		
		String scopegroupid = XMLHelper.getTextValueOf(dataRoot, "@scopegroupid");
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX);
		
		boolean refreshMetaAdv = GetterUtil.getBoolean( XMLHelper.getTextValueOf(dataRoot, "@refresh"), false );
		
		if(refreshMetaAdv && PropsValues.ITER_ENVIRONMENT.equals(WebKeys.ENVIRONMENT_PREVIEW))
			MetadataAdvertisementTools.initMetadataAdvertisementNonClustered();
		
		String sql = String.format(FETCH_ADVOCABULARIES, companyId, groupId, scopegroupid);
		
		return PortalLocalServiceUtil.executeQueryAsDom(sql);
	}
	
	public Document getVocabularies(String groupid) throws NumberFormatException, ServiceError, SecurityException, NoSuchMethodException
	{
		ErrorRaiser.throwIfFalse( Long.valueOf(groupid).longValue() > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX, "Group: " + groupid);
		
		String sql = String.format(GET_VOCS, groupid);
		_log.debug("Vocabularies: " + sql);
		
		return  PortalLocalServiceUtil.executeQueryAsDom(sql);
	}
	
	public Document addAdvertisementVocabulary(String xmlData) throws DocumentException, IOException, SQLException, SecurityException, NoSuchMethodException
	{
		return ContextVarsMgrLocalServiceUtil.addAdvertisementVocabulary(xmlData);
/*
		Document retVal = SAXReaderUtil.read("<rs/>");
		
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		String scopeGroupId = XMLHelper.getTextValueOf(dataRoot, "@groupid");
		
		XPath xpath = SAXReaderUtil.createXPath("//row");
		List<Node> vocabularyNodes = xpath.selectNodes(dataRoot);
		
		String data = "('%s', %s, %s)";
		StringBuilder insertValues = new StringBuilder();
		String[] newVocabularies = ArrayUtils.EMPTY_STRING_ARRAY;
		for(Node vocabulary : vocabularyNodes)
		{
			if(insertValues.length()>0)
				insertValues.append( StringPool.COMMA );
			
			String uuid = XMLHelper.getTextValueOf(vocabulary, "@advocabularyid");
			if(Validator.isNull(uuid))
			{
				uuid = PortalUUIDUtil.newUUID();
				newVocabularies = (String[]) ArrayUtils.add(newVocabularies, uuid);
			}
			
			insertValues.append(
					String.format(data, uuid, scopeGroupId, XMLHelper.getTextValueOf(vocabulary, "@vocabularyid"))
								);
		}
		
		if(insertValues.length()>0)
		{
			String query = String.format(ADD_VOCS, insertValues.toString());
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
		if( !ArrayUtils.isEmpty(newVocabularies) )
		{
			String sql = String.format(FETCH_DISCRETE_ADVOCABULARIES, StringUtil.merge(newVocabularies, "','"));
			retVal = PortalLocalServiceUtil.executeQueryAsDom(sql);
		}
		
		return retVal;
*/
	}
	
	public String deleteAdvertisementVocabulary(String xmlData) throws DocumentException, IOException, SQLException, ServiceError
	{
		Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();

		XPath xpath = SAXReaderUtil.createXPath("//row");
		List<Node> vocabularyNodes = xpath.selectNodes(dataRoot);
		
		String toDelete = StringUtil.merge(XMLHelper.getStringValues(vocabularyNodes, "@advocabularyid"), "','");
		
		addToDeleteList(String.format("('%s')", toDelete), TBL_ADVOCABULARY, ADVOCABULARYID_COLUMN_NAME);
		
		PortalLocalServiceUtil.executeUpdateQuery( String.format(DELETE_VOCS, toDelete) );
		
		MetadataAdvertisementTools.deleteVocabulary(toDelete);
		
		return xmlData;
	}

}
