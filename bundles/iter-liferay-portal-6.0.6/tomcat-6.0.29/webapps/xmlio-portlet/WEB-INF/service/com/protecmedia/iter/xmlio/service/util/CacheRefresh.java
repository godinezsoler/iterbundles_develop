package com.protecmedia.iter.xmlio.service.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;

public class CacheRefresh
{
	/**
	 * No se filtra por estado de la operación, de modo que aunque haya fallado la operación de un artículo, 
	 * este se tiene en cuenta para la actualización dada las inconsistencias que puede haber dejado dicho fallo. 
	 */
	private static String XPATH_JA_GLOBALIDS = String.format("//item[@classname='%s']/@globalId", IterKeys.CLASSNAME_JOURNALARTICLE);

	private static String GET_GROUPS_BY_ARTICLES_GLOBALID = String.format(new StringBuilder(
		"SELECT Group_.groupId, Group_.name groupName																					\n").append(
		"FROM Group_																													\n").append(
		"INNER JOIN (	-- Se obtiene una única cadena con todos los grupos (groupIds) donde están paginados los artículos				\n").append(
		"				SELECT  GROUP_CONCAT( ExpandoValue.data_) groupValues															\n").append(
		"				FROM JournalArticle 																							\n").append(
		"				INNER JOIN ExpandoValue ON(ExpandoValue.classPK=JournalArticle.id_)												\n").append(
		"				INNER JOIN ExpandoColumn ON (ExpandoColumn.columnId=ExpandoValue.columnId AND ExpandoColumn.name='%s')			\n").append(
		"				INNER JOIN XmlIO_Live ON (XmlIO_Live.classNameValue = '%s' AND XmlIO_Live.localId = JournalArticle.articleId)	\n").append(
		"						WHERE XmlIO_Live.globalId IN ('%%s')																	\n").append(
		"			) GroupList 																										\n").append(
		"						-- Según la expresión regular, el groupId debe ser una de las 'palabras' (groupId) que están separadas 	\n").append(
		"						-- por coma																								\n").append(						
		"						ON GroupList.groupValues REGEXP CONCAT('[[:<:]]',groupId,'[[:>:]]' )\n").toString(),
		IterKeys.EXPANDO_COLUMN_NAME_SCOPEGROUPID, IterKeys.CLASSNAME_JOURNALARTICLE);

	private final String ATTR_REFRESH_CATEGORY_ARTICLES 		= "refreshcategoryarticles";
	private final String ATTR_REFRESH_SECTION_ARTICLES 			= "refreshsectionarticles";
	private final String ATTR_REFRESH_GLOBAL_JOURNAL_TEMPLATES 	= "refreshglobaljournaltemplates";
	private final String ATTR_REFRESH_LAYOUT_TEMPLATES 			= "refreshlayouttemplates";
	private final String ATTR_REFRESH_QUALIFICATIONS 			= "refreshqualifications";
	private final String ATTR_REFRESH_METADATA_ADVERTISEMENT 	= "refreshmetadataadvertisement";
	private final String ATTR_REFRESH_IMAGE_FRAMES 				= "refreshimageframes";
	private final String ATTR_REFRESH_FRAMESYNONYMS				= "refreshframesynonyms";
	private final String ATTR_REFRESH_ALTERNATIVE_TYPES 		= "refreshalternativetypes";
	private final String ATTR_REFRESH_WATERMARKS 				= "refreshwatermarks";
	private final String ATTR_REFRESH_CONTEXT_VARIABLES 		= "refreshcontextvariables";
	private final String ATTR_SEND_MAS_NOTIFICATION				= "sendmasnotification";
	private final String ATTR_REFRESH_SCHEDULED_PUBLICATIONS	= "refreshscheduledpublications";
	private final String ATTR_RESCHEDULE_CACHE_INVALIDATION		= "rescheduleCacheInvalidation";
	private final String ATTR_REFRESH_TOMCAT_AND_APACHE			= "refreshTomcatAndApache";
	private final String ATTR_PUBLICATION_ID					= "publicationProcessId";

	private boolean _refreshCategoryArticles  		= false;
	private boolean _refreshSectionArticles  		= false;
	private boolean _refreshGlobalJournalTemplates	= false;
	private boolean _refreshLayoutTemplates			= false;
	private boolean _refreshQualifications 			= false;
	private boolean _refreshMetadataAdvertisement	= false;
	private boolean _refreshImageFrames 			= false;
	private boolean _refreshFrameSynonyms			= false;
	private boolean _refreshAlternativeTypes 		= false;
	private boolean _refreshWatermarks	 			= false;
	private boolean _refreshContextVariables		= false;
	private String  _sendMASNotification			= StringPool.BLANK;
	private boolean _refreshScheduledPublications	= false;
	private boolean _rescheduleCacheInvalidation	= true;
	private boolean _refreshTomcatAndApache			= true;
	private String  _publicationId					= StringPool.BLANK;
	
	private Document _dom							= null;
	
	public CacheRefresh(long scopeGroupId) throws PortalException, SystemException, DocumentException
	{
		this(scopeGroupId, "");
	}
	
	public CacheRefresh(String scopeGroupName, String lastUpdate) throws PortalException, SystemException, DocumentException
	{
		this(GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), scopeGroupName).getGroupId(), lastUpdate);
	}

	public CacheRefresh(long scopeGroupId, String lastUpdate) throws PortalException, SystemException, DocumentException
	{
		initDom( getGroups2Update(scopeGroupId, lastUpdate).getRootElement() );
	}
	
	public CacheRefresh(List<Long> groupList) throws PortalException, SystemException, DocumentException
	{
		initDom( getGroups2Update(groupList).getRootElement() );
	}
	
	public CacheRefresh(long scopeGroupId, Node groups2Updt) throws PortalException, SystemException, DocumentException
	{
		initDom( (groups2Updt != null) 											?
					SAXReaderUtil.read(groups2Updt.asXML()).getRootElement() 	:
					getGroups2Update(scopeGroupId, "").getRootElement() );
	}
	private void initDom(Element groups2Updt) throws DocumentException
	{
		_dom = SAXReaderUtil.read("<rs><row/></rs>");
		_dom.getRootElement().add(groups2Updt);
		_dom.setXMLEncoding("ISO-8859-1");
	}
	
	public CacheRefresh(String xml) throws DocumentException, ServiceError, PortalException, SystemException
	{
		_dom = SAXReaderUtil.read(xml);
		
		String xpath = "/rs/@%s";
			
		Node groups2Updt = _dom.getRootElement().selectSingleNode(IterKeys.GROUPS2UPDT_ROOT);
		ErrorRaiser.throwIfNull(groups2Updt);
		
		// Se actualiza el groupId de los grupos a refrescar
		List<Node> groups = groups2Updt.selectNodes(IterKeys.GROUPS2UPDT_NODE);
		for (Node group : groups)
		{
			((Element)group).addAttribute("groupId",  String.valueOf(GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), ((Element)group).attributeValue("groupName")).getGroupId()) );
		}
		
		xpath = "/rs/row/@%s";
			
		_refreshCategoryArticles 		= GetterUtil.getBoolean( XMLHelper.getStringValueOf(_dom, String.format(xpath, ATTR_REFRESH_CATEGORY_ARTICLES)));
		_refreshSectionArticles 		= GetterUtil.getBoolean( XMLHelper.getStringValueOf(_dom, String.format(xpath, ATTR_REFRESH_SECTION_ARTICLES)));
		_refreshGlobalJournalTemplates	= GetterUtil.getBoolean( XMLHelper.getStringValueOf(_dom, String.format(xpath, ATTR_REFRESH_GLOBAL_JOURNAL_TEMPLATES)));
		_refreshLayoutTemplates			= GetterUtil.getBoolean( XMLHelper.getStringValueOf(_dom, String.format(xpath, ATTR_REFRESH_LAYOUT_TEMPLATES)));
		_refreshQualifications 			= GetterUtil.getBoolean( XMLHelper.getStringValueOf(_dom, String.format(xpath, ATTR_REFRESH_QUALIFICATIONS)));
		_refreshMetadataAdvertisement	= GetterUtil.getBoolean( XMLHelper.getStringValueOf(_dom, String.format(xpath, ATTR_REFRESH_METADATA_ADVERTISEMENT)));
		_refreshImageFrames				= GetterUtil.getBoolean( XMLHelper.getStringValueOf(_dom, String.format(xpath, ATTR_REFRESH_IMAGE_FRAMES)));
		_refreshFrameSynonyms			= GetterUtil.getBoolean( XMLHelper.getStringValueOf(_dom, String.format(xpath, ATTR_REFRESH_FRAMESYNONYMS)));
		_refreshAlternativeTypes		= GetterUtil.getBoolean( XMLHelper.getStringValueOf(_dom, String.format(xpath, ATTR_REFRESH_ALTERNATIVE_TYPES)));
		_refreshWatermarks				= GetterUtil.getBoolean( XMLHelper.getStringValueOf(_dom, String.format(xpath, ATTR_REFRESH_WATERMARKS)));
		_refreshContextVariables		= GetterUtil.getBoolean( XMLHelper.getStringValueOf(_dom, String.format(xpath, ATTR_REFRESH_CONTEXT_VARIABLES)));
		_sendMASNotification			= GetterUtil.getString( XMLHelper.getStringValueOf(_dom, String.format(xpath, ATTR_SEND_MAS_NOTIFICATION)));
		_refreshScheduledPublications	= GetterUtil.getBoolean( XMLHelper.getStringValueOf(_dom, String.format(xpath, ATTR_REFRESH_SCHEDULED_PUBLICATIONS)));
		_rescheduleCacheInvalidation	= GetterUtil.getBoolean( XMLHelper.getStringValueOf(_dom, String.format(xpath, ATTR_RESCHEDULE_CACHE_INVALIDATION)), 	true);
		_refreshTomcatAndApache			= GetterUtil.getBoolean( XMLHelper.getStringValueOf(_dom, String.format(xpath, ATTR_REFRESH_TOMCAT_AND_APACHE)), 		true);
		_publicationId					= GetterUtil.getString( XMLHelper.getStringValueOf(_dom, String.format(xpath, ATTR_PUBLICATION_ID)));
	}

	public static Document getGroups2Update(long groupId, Node operationLog) throws PortalException, SystemException, DocumentException, SecurityException, NoSuchMethodException
	{
		Document groupsDom = null;
		
		 // No se filtra por estado de la operación, de modo que aunque haya fallado la operación de un artículo, 
		 // este se tiene en cuenta para la actualización dada las inconsistencias que puede haber dejado dicho fallo.
		String jaGlobalIds = StringUtils.join(XMLHelper.getStringValues(operationLog.selectNodes(XPATH_JA_GLOBALIDS), "."), "','");
		if (Validator.isNotNull(jaGlobalIds) && PropsValues.ITER_SHARED_ARTICLES_ENABLED)
		{
			groupsDom = PortalLocalServiceUtil.executeQueryAsDom( String.format(GET_GROUPS_BY_ARTICLES_GLOBALID, jaGlobalIds), true, IterKeys.GROUPS2UPDT_ROOT, IterKeys.GROUPS2UPDT_NODE);
		}
		else
		{
			groupsDom = getGroups2Update(groupId, "");
		}
		return groupsDom;
	}
	private static Document getGroups2Update(long groupId, String lastUpdate) throws DocumentException, PortalException, SystemException
	{
		Document groupsDom = SAXReaderUtil.read( String.format("<%s/>", IterKeys.GROUPS2UPDT_ROOT) );
		Group group = GroupLocalServiceUtil.getGroup(groupId);
		
		Element groupElem = groupsDom.getRootElement().addElement(IterKeys.GROUPS2UPDT_NODE);
		groupElem.addAttribute("groupId", 	String.valueOf(group.getGroupId()));
		groupElem.addAttribute("groupName", group.getName());
		
		if (Validator.isNotNull(lastUpdate))
			groupElem.addAttribute("lastUpdate", lastUpdate);

		return groupsDom;
	}
	
	private static Document getGroups2Update(List<Long> groupIdList) throws DocumentException, PortalException, SystemException
	{
		Document groupsDom = SAXReaderUtil.read( String.format("<%s/>", IterKeys.GROUPS2UPDT_ROOT) );
		
		// Se pasa por un set para evitar duplicados
		Set<Long> list = new HashSet<Long>(groupIdList);
		for (long groupId : list)
		{
			Group group = GroupLocalServiceUtil.getGroup(groupId);
			
			Element groupElem = groupsDom.getRootElement().addElement(IterKeys.GROUPS2UPDT_NODE);
			groupElem.addAttribute("groupId", 	String.valueOf(group.getGroupId()));
			groupElem.addAttribute("groupName", group.getName());
		}
		
		return groupsDom;
	}
	
	public Element getGroups2Update()
	{
		return (Element)_dom.getRootElement().selectSingleNode(IterKeys.GROUPS2UPDT_ROOT);
	}
	
	public List<Long> getGroupIDs()
	{
		List<Long> groupIds = new ArrayList<Long>();
		
		List<Node> groups = getGroups2Update().selectNodes(String.format("%s/@groupId", IterKeys.GROUPS2UPDT_NODE));
		for (Node groupId : groups)
			groupIds.add( GetterUtil.getLong(groupId.getStringValue()) );
		
		return groupIds;
	}
	
	public void setGroups2Update(List<Long> groupIdList) throws PortalException, SystemException, DocumentException
	{
		Node groups2Updt = _dom.getRootElement().selectSingleNode(IterKeys.GROUPS2UPDT_ROOT);
		if (groups2Updt != null)
			groups2Updt.detach();
		
		_dom.getRootElement().add( getGroups2Update(groupIdList).getRootElement() );
	}


//	public void setScopeGroupName(long scopeGroupId) throws PortalException, SystemException
//	{
//		Group scopeGroup = GroupLocalServiceUtil.getGroup(scopeGroupId);
//		this._scopeGroupName = scopeGroup.getName();
//	}
	
//	public void setScopeGroupName(String scopeGroupName)
//	{
//		this._scopeGroupName = scopeGroupName;
//	}

//	public String getLastUpdateMilis()
//	{
//		return _lastUpdateMilis;
//	}

//	public void setLastUpdateMilis(String lastUpdateMilis)
//	{
//		this._lastUpdateMilis = lastUpdateMilis;
//	}

	public boolean refreshCategoryArticles()
	{
		return _refreshCategoryArticles;
	}

	public void setRefreshCategoryArticles(boolean refreshCategoryArticles)
	{
		this._refreshCategoryArticles = refreshCategoryArticles;
	}

	public boolean refreshSectionArticles()
	{
		return _refreshSectionArticles;
	}

	public void setRefreshSectionArticles(boolean refreshSectionArticles)
	{
		this._refreshSectionArticles = refreshSectionArticles;
	}

	public boolean refreshGlobalJournalTemplates()
	{
		return _refreshGlobalJournalTemplates;
	}

	public void setRefreshGlobalJournalTemplates(boolean refreshGlobalJournalTemplates)
	{
		this._refreshGlobalJournalTemplates = refreshGlobalJournalTemplates;
	}

	public boolean refreshLayoutTemplates()
	{
		return _refreshLayoutTemplates;
	}

	public void setRefreshLayoutTemplates(boolean refreshLayoutTemplates)
	{
		_refreshLayoutTemplates = refreshLayoutTemplates;
	}

	public boolean refreshMetadataAdvertisement()
	{
		return _refreshMetadataAdvertisement;
	}

	public void setRefreshMetadataAdvertisement(boolean refreshMetadataAdvertisement)
	{
		this._refreshMetadataAdvertisement = refreshMetadataAdvertisement;
	}

	public boolean refreshImageFrames()
	{
		return _refreshImageFrames;
	}

	public void setRefreshImageFrames(boolean refreshImageFrames)
	{
		this._refreshImageFrames = refreshImageFrames;
	}

	public boolean refreshFrameSynonyms()
	{
		return _refreshFrameSynonyms;
	}

	public void setRefreshFrameSynonyms(boolean refreshFrameSynonyms)
	{
		this._refreshFrameSynonyms = refreshFrameSynonyms;
	}
	
	public boolean refreshAlternativeTypes()
	{
		return _refreshAlternativeTypes;
	}

	public void seRefreshAlternativeTypes(boolean refreshAlternativeTypes)
	{
		this._refreshAlternativeTypes = refreshAlternativeTypes;
	}

	public boolean refreshWatermarks()
	{
		return _refreshWatermarks;
	}

	public void setRefreshWatermarks(boolean refreshWatermarks)
	{
		this._refreshWatermarks = refreshWatermarks;
	}

	public boolean refreshQualifications()
	{
		return _refreshQualifications;
	}

	public void setRefreshQualifications(boolean refreshQualifications)
	{
		this._refreshQualifications = refreshQualifications;
	}
	
	public boolean refreshContextVariables()
	{
		return _refreshContextVariables;
	}

	public void setRefreshContextVariables(boolean refreshContextVariables)
	{
		this._refreshContextVariables = refreshContextVariables;
	}
	
	public String sendMasNotification()
	{
		return _sendMASNotification;
	}

	public void setSendMasNotification(String sendMASNotification)
	{
		this._sendMASNotification = sendMASNotification;
	}
	
	public boolean refreshScheduledPublications()
	{
		return _refreshScheduledPublications;
	}

	public void setRefreshScheduledPublications(boolean refreshScheduledPublications)
	{
		this._refreshScheduledPublications = refreshScheduledPublications;
	}
	
	public boolean rescheduleCacheInvalidation()
	{
		return _rescheduleCacheInvalidation;
	}
	
	public void setRescheduleCacheInvalidation(boolean rescheduleCacheInvalidation)
	{
		this._rescheduleCacheInvalidation = rescheduleCacheInvalidation;
	}
	
	public boolean refreshTomcatAndApache()
	{
		return _refreshTomcatAndApache;
	}

	public void setRefreshTomcatAndApache(boolean refreshTomcatAndApache)
	{
		this._refreshTomcatAndApache = refreshTomcatAndApache;
	}
	
	public String publicationId()
	{
		return _publicationId;
	}

	public void setPublicationId(String publicationId)
	{
		this._publicationId = publicationId;
	}
	
	public Document getCacheRefresh()
	{
		Element row = (Element)_dom.selectSingleNode("/rs/row");
		
		row.addAttribute(ATTR_REFRESH_CATEGORY_ARTICLES, 		String.valueOf(_refreshCategoryArticles) );
		row.addAttribute(ATTR_REFRESH_SECTION_ARTICLES, 		String.valueOf(_refreshSectionArticles) );
		row.addAttribute(ATTR_REFRESH_GLOBAL_JOURNAL_TEMPLATES,	String.valueOf(_refreshGlobalJournalTemplates) );
		row.addAttribute(ATTR_REFRESH_LAYOUT_TEMPLATES,			String.valueOf(_refreshLayoutTemplates) );
		row.addAttribute(ATTR_REFRESH_QUALIFICATIONS, 			String.valueOf(_refreshQualifications) );
		row.addAttribute(ATTR_REFRESH_METADATA_ADVERTISEMENT, 	String.valueOf(_refreshMetadataAdvertisement) );
		row.addAttribute(ATTR_REFRESH_IMAGE_FRAMES, 			String.valueOf(_refreshImageFrames) );
		row.addAttribute(ATTR_REFRESH_FRAMESYNONYMS, 			String.valueOf(_refreshFrameSynonyms) );
		row.addAttribute(ATTR_REFRESH_ALTERNATIVE_TYPES, 		String.valueOf(_refreshAlternativeTypes) );
		row.addAttribute(ATTR_REFRESH_WATERMARKS, 				String.valueOf(_refreshWatermarks) );
		row.addAttribute(ATTR_REFRESH_CONTEXT_VARIABLES, 		String.valueOf(_refreshContextVariables) );
		row.addAttribute(ATTR_SEND_MAS_NOTIFICATION, 			String.valueOf(_sendMASNotification) );
		row.addAttribute(ATTR_REFRESH_SCHEDULED_PUBLICATIONS, 	String.valueOf(_refreshScheduledPublications) );
		row.addAttribute(ATTR_RESCHEDULE_CACHE_INVALIDATION, 	String.valueOf(_rescheduleCacheInvalidation) );
		row.addAttribute(ATTR_REFRESH_TOMCAT_AND_APACHE, 		String.valueOf(_refreshTomcatAndApache) );
		row.addAttribute(ATTR_PUBLICATION_ID, 					String.valueOf(_publicationId) );
		
		return _dom;
	}
	
	/**
	 * Se mezclan los parámetros necesarios de los CacheRefresh
	 * 
	 * @param list
	 * @throws PortalException
	 * @throws SystemException
	 * @throws DocumentException
	 */
	public void merge(List<CacheRefresh> list) throws PortalException, SystemException, DocumentException
	{
		List<Long> 	groupList 						= getGroupIDs();
		boolean 	refreshScheduledPublications 	= refreshScheduledPublications();
		
		String 		publicationId					= publicationId();
		Set<String>publicationIdSet					= new HashSet<String>();
		if (Validator.isNotNull(publicationId))
			publicationIdSet.add(publicationId);
		
		
		for (CacheRefresh cacheRefresh : list)
		{
			groupList.addAll( cacheRefresh.getGroupIDs() );
			refreshScheduledPublications |= cacheRefresh.refreshScheduledPublications();
			
			publicationId = cacheRefresh.publicationId();
			if (Validator.isNotNull(publicationId))
				publicationIdSet.add(publicationId);
		}
		
		setGroups2Update(groupList);
		setRefreshScheduledPublications(refreshScheduledPublications);
		
		if (!publicationIdSet.isEmpty())
			setPublicationId( StringUtil.merge(publicationIdSet) );
	}
	
}
