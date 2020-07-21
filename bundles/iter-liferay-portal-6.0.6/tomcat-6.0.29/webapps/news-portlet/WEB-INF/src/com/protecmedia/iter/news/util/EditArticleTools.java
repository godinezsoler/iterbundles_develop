package com.protecmedia.iter.news.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;

public class EditArticleTools
{
	private static Log _log = LogFactoryUtil.getLog(EditArticleTools.class);
	
	private final String GET_ARTICLE_METAS = new StringBuilder()
		.append( "SELECT AssetCategory.categoryId AS localid, \n" )
		.append( "\t(SELECT IF(INSTR(data_, AssetCategory.categoryId)=0, FALSE, TRUE) FROM ExpandoValue INNER JOIN ExpandoColumn ON(ExpandoValue.columnid=ExpandoColumn.columnid AND ExpandoColumn.name='%s') WHERE classnameid=%s AND classpk=%s ) AS main \n" )
		.append( "FROM AssetCategory \n" )
		.append( "INNER JOIN AssetEntries_AssetCategories ON AssetEntries_AssetCategories.categoryId=AssetCategory.categoryId \n" )
		.append( "INNER JOIN AssetEntry ON (AssetEntry.entryId=AssetEntries_AssetCategories.entryId AND AssetEntry.classPK=%s) \n" )
		.toString();

	private final String GET_GLOBAL_IDS = new StringBuilder()
		.append("SELECT Xmlio_Live.localid AS lid, Xmlio_Live.globalid AS gid \n")
		.append("FROM Xmlio_Live \n")
		.append("WHERE classNameValue='%s' \n\tAND Xmlio_Live.localid IN('%s')")
		.toString();
	
	private final String GET_ARTICLE_SECTIONS = new StringBuilder(
		"SELECT  xmlio_live_group.localid AS sitelocalid, xmlio_live_group.globalid AS siteglobalid, n.id_ AS localid, xmlio_live_pgcnt.globalid AS gid, 														\n").append(
		"		 n.online_ AS publicable, n.defaultSection AS main, n.vigenciahasta AS validthru, n.vigenciadesde AS validfrom, 																				\n").append(
		"		 q.qualifId AS q, n.articleModelId AS modellocalid , IFNULL(xmlio_live_model.globalid, -1) AS modelgid, 																						\n").append(
		"		 l.layoutId AS sectionlocalid, xmlio_live_layout.globalId AS sectiongid																															\n").append(
		"FROM News_Qualification q																																												\n").append(
		"INNER JOIN  News_PageContent n            ON (n.qualificationId=q.qualifId AND n.contentId='%s')																										\n").append(
		"INNER JOIN  layout l                      ON (n.layoutid= l.uuid_) 																																	\n").append(
		"INNER JOIN  group_                        ON (n.groupid=group_.groupid)																																\n").append(
		"INNER JOIN  Xmlio_Live xmlio_live_group   ON (xmlio_live_group.groupId  = group_.groupid AND xmlio_live_group.classNameValue ='%s' AND xmlio_live_group.localid  = group_.groupid)						\n").append(
		"INNER JOIN  Xmlio_Live xmlio_live_layout  ON (xmlio_live_layout.groupId = group_.groupid AND xmlio_live_layout.classNameValue='%s' AND xmlio_live_layout.localId = l.plid)  							\n").append(
		"LEFT JOIN   Xmlio_Live xmlio_live_pgcnt   ON (xmlio_live_pgcnt.groupId  = group_.groupid AND xmlio_live_pgcnt.classNameValue ='%s' AND xmlio_live_pgcnt.localid  =n.pageContentId) 					\n").append(
		"LEFT JOIN   designer_pagetemplate         ON (n.articleModelId=designer_pagetemplate.id_)																												\n").append(
		"LEFT JOIN   Xmlio_Live xmlio_live_model   ON (xmlio_live_model.groupId  = group_.groupid AND xmlio_live_model.classNameValue='%s'  AND xmlio_live_model.localid  =designer_pagetemplate.pageTemplateId)\n").toString();
			
	private final String GET_ARTICLE_SUBSCRIPTIONS = new StringBuilder(
		"SELECT xmlio_live_group.localid AS sitelocalid, xmlio_live_group.globalid AS siteglobalid,							\n").append(
		"		Xmlio_Live.localid, Xmlio_Live.globalid AS gid 																\n").append(
		"FROM group_ INNER JOIN product ON (group_.groupId=product.groupid) 												\n").append(
		"INNER JOIN  Xmlio_Live xmlio_live_group ON (xmlio_live_group.groupId = group_.groupid 								\n").append(
		"				AND xmlio_live_group.classNameValue ='%s' 															\n").append(
		"				AND xmlio_live_group.localid = group_.groupid)														\n").append(				
		"INNER JOIN  articleproduct ON (articleproduct.productId=product.productId AND articleproduct.articleId='%s') 		\n").append(
		"INNER JOIN  Xmlio_Live ON( Xmlio_Live.classNameValue='%s' AND Xmlio_Live.localid=articleproduct.productid ) 		\n").toString();
		
	
	private final String GET_DLFILEENTRIES_INFO = new StringBuilder()
		.append( "SELECT dlfileentry.name localid, dlfileentry.fileentryid, dlfileentry.title \n" )
		.append( "FROM dlfileentry \n" )
		.append( "WHERE dlfileentry.title IN (%s) \n" )
		.toString();
	
	private final String GET_DLFILEENTRIES_SUBSCRIPTIONS = new StringBuilder()
		.append( "SELECT group_.groupId AS sitelocalid, group_.name AS siteglobalid, fileentryproduct.fileEntryId, fileentryproduct.productId \n" )
		.append( "FROM group_ INNER JOIN product ON (group_.groupId=product.groupid) \n" )
		.append( "INNER JOIN fileentryproduct ON (fileentryproduct.productId=product.productId) \n")
		.append( "WHERE fileentryproduct.fileEntryId IN ('%s')" )
		.toString();
	
	private final String GET_BINARIES_INFO = new StringBuilder()
		.append( "SELECT binaryId fileentryid, title \n" )
		.append( "FROM binary_repository             \n" )
		.append( "WHERE title IN (%s)                \n" )
		.toString();

	private final String GET_BINARIES_SUBSCRIPTIONS = new StringBuilder()
		.append( "SELECT group_.groupId AS sitelocalid, group_.name AS siteglobalid, binary_product.binaryId fileEntryId, binary_product.productId \n" )
		.append( "FROM group_ INNER JOIN product ON (group_.groupId=product.groupid) \n" )
		.append( "INNER JOIN binary_product ON (binary_product.productId=product.productId) \n" )
		.append( "WHERE binary_product.binaryId IN ('%s')" )
		.toString();
	
	private String _articleId = "";
	private Document _articleDom = null;
	private long _resourcePrimKey = 0L;
	private long _id_ = 0L;
	private Document _articleContent = null;
	private DateFormat _df = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss);
	
	public EditArticleTools(String articleId)
	{
		this._articleId = articleId;
	}

	public Document getEditArticle() throws DocumentException, ServiceError, NoSuchMethodException, SecurityException, ParseException, SystemException, PortalException
	{
		_articleDom = SAXReaderUtil.createDocument( SAXReaderUtil.createElement(IterKeys.ELEM_ARTICLE) );
		_articleDom.setXMLEncoding(StringPool.UTF8);
		
		Element articleElem =  _articleDom.getRootElement();
		
		JournalArticle article = JournalArticleLocalServiceUtil.getArticle(GroupMgr.getGlobalGroupId(), _articleId);
		
		_id_				= article.getId();
		_resourcePrimKey	= article.getResourcePrimKey();
		_articleContent		= SAXReaderUtil.read( article.getContent() );
		
		long scopeGroupId = article.getCanonicalGroupId();
		ErrorRaiser.throwIfFalse(scopeGroupId > 0, IterErrorKeys.XYZ_E_ARTICLE_WITHOUT_SCOPE_GROUP_ZYX);	
		
		articleElem.addAttribute(IterKeys.ATTR_ARTICLE_ID, 		_articleId);
		articleElem.addAttribute(IterKeys.ATTR_SITEGLOBAL_ID, 	LiveLocalServiceUtil.getLiveByLocalId(scopeGroupId, IterKeys.CLASSNAME_GROUP, String.valueOf(scopeGroupId)).getGlobalId());
		articleElem.addAttribute(IterKeys.ATTR_SITELOCALID, 	String.valueOf(scopeGroupId));
		
		Element metadataElem = articleElem.addElement(IterKeys.ELEM_METADATA);
		getMetadataXML(metadataElem, article);
		
		Element contentElement = articleElem.addElement(IterKeys.ELEM_CONTENT);
		processContent(contentElement);
		
		if(_log.isDebugEnabled())
			_log.debug( "\n".concat(_articleDom.asXML()) );
		
		return _articleDom;
	}

	private void getMetadataXML(Element metadataElem, JournalArticle article) throws ParseException, NoSuchMethodException, SecurityException
	{
		Element propertiesElem = metadataElem.addElement(IterKeys.ELEM_PROPERTIES);
		
		propertiesElem.addAttribute( IterKeys.ATTR_MODIFIEDDATE	, _df.format(article.getModifiedDate())		);
		propertiesElem.addAttribute( IterKeys.ATTR_COMMENTS		, String.valueOf(article.getSmallImageId())	);
		propertiesElem.addAttribute( IterKeys.ATTR_INDEXABLE	, String.valueOf(article.getIndexable())	);
		propertiesElem.addAttribute( IterKeys.ATTR_STRUCTURE	, article.getStructureId()					);
		propertiesElem.addAttribute( IterKeys.ATTR_TITLE		, article.getTitle()						);
		propertiesElem.addAttribute( IterKeys.ATTR_HIDEADV		, article.isSmallImage() ? "1" : "0"		);
		
		Element categoriesElem = metadataElem.addElement(IterKeys.ELEM_CATEGORIES);
		getArticleCategories(categoriesElem);
		
		Element sectionsElem = metadataElem.addElement(IterKeys.ELEM_SECTIONS);
		getArticleSections(sectionsElem);
		
		Element subscriptionsElem = metadataElem.addElement(IterKeys.ELEM_SUBSCRIPTIONS);
		getArticleSubscriptions(subscriptionsElem);
	}

	private void getArticleCategories(Element categoriesElem) throws NoSuchMethodException, SecurityException
	{
		String query = String.format(GET_ARTICLE_METAS, 
										WebKeys.EXPANDO_COLUMN_NAME_MAIN_METADATAS_IDS, 
										PortalUtil.getClassNameId(JournalArticle.class),
										_id_,
										_resourcePrimKey);
		long ini = 0L;
		if(_log.isDebugEnabled())
		{
			_log.debug("GET_ARTICLE_METAS "+ _articleId +" : " + query);
			ini = System.currentTimeMillis();
		}
		
		List<Map<String, Object>> articleCategoriesMap =  PortalLocalServiceUtil.executeQueryAsMap( query );
		
		if(_log.isDebugEnabled())
			_log.debug("Query GET_ARTICLE_METAS time: " + (System.currentTimeMillis()-ini) + " ms.");
		
		if(articleCategoriesMap!=null && articleCategoriesMap.size()>0)
		{
			int numCategories = articleCategoriesMap.size();
			int i=0;
			StringBuilder categoriesIds = new StringBuilder();
			
			for (Map<String, Object> row : articleCategoriesMap)
			{
				categoriesIds.append( String.valueOf(row.get(IterKeys.ATTR_LOCALID)) );
				if (i + 1 != numCategories)
					categoriesIds.append("','");
				i++;
			}
			
			query = String.format(GET_GLOBAL_IDS, IterKeys.CLASSNAME_CATEGORY, categoriesIds.toString());
			
			ini = 0L;
			if(_log.isDebugEnabled())
			{
				_log.debug("GET_METAS_GLOBAL_IDS: " + query);
				ini = System.currentTimeMillis();
			}
			
			Element globalCategoriesIds = PortalLocalServiceUtil.executeQueryAsDom(query).getRootElement();
			
			if(_log.isDebugEnabled())
				_log.debug("Query GET_METAS_GLOBAL_IDS time: " + (System.currentTimeMillis()-ini) + " ms.");
			
			for(Map<String, Object> row : articleCategoriesMap)
			{
				Element categoryElem = categoriesElem.addElement(IterKeys.ELEM_CATEGORY);
				
				String categoryId = String.valueOf(row.get(IterKeys.ATTR_LOCALID));
				
				categoryElem.addAttribute( IterKeys.ATTR_GID	, XMLHelper.getStringValueOf(globalCategoriesIds, String.format("/rs/row[@lid='%s']/@gid", categoryId)));
				categoryElem.addAttribute( IterKeys.ATTR_LOCALID, categoryId);
				categoryElem.addAttribute( IterKeys.ATTR_MAIN	, String.valueOf(row.get(IterKeys.ATTR_MAIN)) 	);
			}
		}
	}

	private void getArticleSections(Element sectionsElem) throws ParseException
	{
		String query = String.format(GET_ARTICLE_SECTIONS, _articleId,
									 IterKeys.CLASSNAME_GROUP, 			IterKeys.CLASSNAME_LAYOUT,
									 IterKeys.CLASSNAME_PAGECONTENT, 	IterKeys.CLASSNAME_PAGETEMPLATE);
		long ini = 0L;
		if(_log.isDebugEnabled())
		{
			_log.debug("GET_ARTICLE_SECTIONS "+ _articleId +" : " + query);
			ini = System.currentTimeMillis();
		}
		
		List<Map<String, Object>> articleSectionsMap =  PortalLocalServiceUtil.executeQueryAsMap( query );
		
		if(_log.isDebugEnabled())
			_log.debug("Query GET_ARTICLE_SECTIONS time: " + (System.currentTimeMillis()-ini) + " ms.");
		
		if(articleSectionsMap!=null && articleSectionsMap.size()>0)
		{
			for(Map<String, Object> row : articleSectionsMap)
			{
				Element sectionElem = sectionsElem.addElement(IterKeys.ELEM_SECTION);
				
				Date validFrom = _df.parse(String.valueOf(row.get(IterKeys.ATTR_VALIDFROM)));
				Date validThru = _df.parse(String.valueOf(row.get(IterKeys.ATTR_VALIDTHRU)));
				
				/* gid no se recupera cuando se asigna el artículo a una sección directamente en el LIVE,
				 * por el hecho de no existir ese registro con classNameValue='com.protecmedia.iter.news.model.PageContent'en la tabla Xmlio_Live*/
				if(!( String.valueOf(row.get(IterKeys.ATTR_GID)).equals("null")) )
					sectionElem.addAttribute( IterKeys.ATTR_GID			, String.valueOf(row.get(IterKeys.ATTR_GID))			);
				sectionElem.addAttribute( IterKeys.ATTR_SITELOCALID		, String.valueOf(row.get(IterKeys.ATTR_SITELOCALID))	);
				sectionElem.addAttribute( IterKeys.ATTR_SITEGLOBAL_ID	, String.valueOf(row.get(IterKeys.ATTR_SITEGLOBAL_ID))	);
				sectionElem.addAttribute( IterKeys.ATTR_LOCALID			, String.valueOf(row.get(IterKeys.ATTR_LOCALID))		);
				sectionElem.addAttribute( IterKeys.ATTR_MAIN			, String.valueOf(row.get(IterKeys.ATTR_MAIN))			);
				sectionElem.addAttribute( IterKeys.ATTR_VALIDFROM		, _df.format(validFrom)									);
				sectionElem.addAttribute( IterKeys.ATTR_VALIDTHRU		, _df.format(validThru)									);
				sectionElem.addAttribute( IterKeys.ATTR_Q				, String.valueOf(row.get(IterKeys.ATTR_Q))				);
				sectionElem.addAttribute( IterKeys.ATTR_MODELGID		, String.valueOf(row.get(IterKeys.ATTR_MODELGID))		);
				sectionElem.addAttribute( IterKeys.ATTR_MODELLOCALID	, String.valueOf(row.get(IterKeys.ATTR_MODELLOCALID))	);
				sectionElem.addAttribute( IterKeys.ATTR_SECTIONGID		, String.valueOf(row.get(IterKeys.ATTR_SECTIONGID))		);
				sectionElem.addAttribute( IterKeys.ATTR_SECTIONLOCALID	, String.valueOf(row.get(IterKeys.ATTR_SECTIONLOCALID))	);
				sectionElem.addAttribute( IterKeys.ATTR_PUBLICABLE		, String.valueOf(row.get(IterKeys.ATTR_PUBLICABLE))		);
				
			}
		}
	}
	
	private void getArticleSubscriptions(Element subscriptionsElem)
	{
		String query = String.format(GET_ARTICLE_SUBSCRIPTIONS, IterKeys.CLASSNAME_GROUP, _articleId, IterKeys.CLASSNAME_PRODUCT);
		long ini = 0L;
		if(_log.isDebugEnabled())
		{
			_log.debug("GET_ARTICLE_SUBSCRIPTIONS "+ _articleId +" : " + query);
			ini = System.currentTimeMillis();
		}
		
		List<Map<String, Object>> articleSubscriptionsMap =  PortalLocalServiceUtil.executeQueryAsMap( query );
		
		if(_log.isDebugEnabled())
			_log.debug("Query GET_ARTICLE_SUBSCRIPTIONS time: " + (System.currentTimeMillis()-ini) + " ms.");
		
		if(articleSubscriptionsMap!=null && articleSubscriptionsMap.size()>0)
		{
			for(Map<String, Object> row : articleSubscriptionsMap)
			{
				Element subscriptionElem = subscriptionsElem.addElement(IterKeys.ELEM_SUBSCRIPTION);
				
				subscriptionElem.addAttribute( IterKeys.ATTR_GID, 			String.valueOf(row.get(IterKeys.ATTR_GID)) 	);
				subscriptionElem.addAttribute( IterKeys.ATTR_LOCALID, 		String.valueOf(row.get(IterKeys.ATTR_LOCALID)));
				subscriptionElem.addAttribute( IterKeys.ATTR_SITELOCALID,	String.valueOf(row.get(IterKeys.ATTR_SITELOCALID)));
				subscriptionElem.addAttribute( IterKeys.ATTR_SITEGLOBAL_ID,	String.valueOf(row.get(IterKeys.ATTR_SITEGLOBAL_ID)));
			}
		}
	}
	
	private void processContent(Element contentElement) throws NoSuchMethodException, SecurityException
	{
		String xpath = "/root/dynamic-element/dynamic-element[@type='document_library']/dynamic-content";
		String xpath2 = "//row[@fileEntryId='%s']";
		
		String[] filePaths = XMLHelper.getStringValues(_articleContent.selectNodes(xpath), null);
		if( filePaths.length>0 )
		{
			StringBuilder fileentryTitles = new StringBuilder(); 
			for(String fp : filePaths)
				fileentryTitles.append( StringUtil.apostrophe(fp.substring(fp.lastIndexOf(StringPool.SLASH)+1)) ).append(StringPool.COMMA);
			
			if(fileentryTitles.length()>0)
				fileentryTitles.deleteCharAt( fileentryTitles.length()-1 );
			
			String query = null;
			boolean binRepository = _articleContent.selectNodes("//dynamic-element[@type='document_library' and starts-with(dynamic-content, '/binrepository/')]").size() > 0;
			
			if (binRepository)
				query = String.format(GET_BINARIES_INFO, fileentryTitles.toString());
			else
				query = String.format(GET_DLFILEENTRIES_INFO, fileentryTitles.toString());
			
			long ini = 0L;
			if(_log.isDebugEnabled())
			{
				_log.debug("GET_DLFILEENTRIES_INFO "+ _articleId +" : " + query);
				ini = System.currentTimeMillis();
			}
			
			Document fileentriesInfoDom = PortalLocalServiceUtil.executeQueryAsDom(query);
			
			if(_log.isDebugEnabled())
				_log.debug("Query GET_DLFILEENTRIES_INFO time: " + (System.currentTimeMillis()-ini) + " ms.");
			
			List<Node> rows = fileentriesInfoDom.selectNodes("//row");
			if(Validator.isNotNull(rows))
			{
				String[] fileentriesIds = XMLHelper.getStringValues(rows, "@fileentryid");
				
				Element fileentriesGlobalIds = null;
				if (!binRepository)
				{
					query = String.format(GET_GLOBAL_IDS, IterKeys.CLASSNAME_DLFILEENTRY, StringUtil.merge(fileentriesIds, "','"));
					ini = 0L;
					if(_log.isDebugEnabled())
					{
						_log.debug("GET_DLFILEENTRIES_GLOBAL_IDS "+ _articleId +" : " + query);
						ini = System.currentTimeMillis();
					}
					
					fileentriesGlobalIds = PortalLocalServiceUtil.executeQueryAsDom(query).getRootElement();
					
					if(_log.isDebugEnabled())
						_log.debug("Query GET_DLFILEENTRIES_GLOBAL_IDS time: " + (System.currentTimeMillis()-ini) + " ms.");
				}
				
				if (binRepository)
					query = String.format(GET_BINARIES_SUBSCRIPTIONS, StringUtil.merge(fileentriesIds, "','"));
				else
					query = String.format(GET_DLFILEENTRIES_SUBSCRIPTIONS, StringUtil.merge(fileentriesIds, "','"));
				
				ini = 0;
				if(_log.isDebugEnabled())
				{
					_log.debug("GET_DLFILEENTRIES_SUBSCRIPTIONS "+ _articleId +" : " + query);
					ini = System.currentTimeMillis();
				}
				
				Document fileentriesSubscriptionsDom = PortalLocalServiceUtil.executeQueryAsDom(query);
				
				if(_log.isDebugEnabled())
					_log.debug("Query GET_DLFILEENTRIES_SUBSCRIPTIONS time: " + (System.currentTimeMillis()-ini) + " ms.");
				
				Document fileentriesSubscriptionsGlobalIds = null;
				List<Node> productIdsNodes = fileentriesSubscriptionsDom.selectNodes("//row");
				if(productIdsNodes.size()>0)
				{
					query = String.format(GET_GLOBAL_IDS, IterKeys.CLASSNAME_PRODUCT, StringUtil.merge( XMLHelper.getStringValues(productIdsNodes, "@productId"), "','") );
					ini = 0L;
					if(_log.isDebugEnabled())
					{
						_log.debug("GET_DLFILEENTRIES_SUBSCRIPTIONS_GLOBAL_IDS: " + query);
						ini = System.currentTimeMillis();
					}
					
					fileentriesSubscriptionsGlobalIds = PortalLocalServiceUtil.executeQueryAsDom(query);
					
					if(_log.isDebugEnabled())
						_log.debug("Query GET_DLFILEENTRIES_SUBSCRIPTIONS_GLOBAL_IDS time: " + (System.currentTimeMillis()-ini) + " ms.");
				}
				
				xpath = xpath.concat("[contains(.,'%s')]");
				
				for(Node row : rows)
				{
					String fileEntryTitle = XMLHelper.getStringValueOf(row, "@title");
					String localId = XMLHelper.getStringValueOf(row, "@localid");
					String fileentryId = XMLHelper.getStringValueOf(row, "@fileentryid");
					String gid = "";
					if(Validator.isNotNull(fileentriesGlobalIds))
						gid = XMLHelper.getStringValueOf(fileentriesGlobalIds, String.format("/rs/row[@lid='%s']/@gid", fileentryId));
					
					List<Node> fileEntrySubscriptions = fileentriesSubscriptionsDom.selectNodes( String.format(xpath2, fileentryId) );
					Element subscriptionsElem = null;
					if( fileEntrySubscriptions.size()>0 )
					{
						subscriptionsElem = SAXReaderUtil.createElement(IterKeys.ELEM_SUBSCRIPTIONS);
						for(Node subscription : fileEntrySubscriptions)
						{
							Node productIds = null;
							if(fileentriesSubscriptionsGlobalIds!=null)
							{
								productIds = fileentriesSubscriptionsGlobalIds.selectSingleNode( String.format("//row[@lid='%s']", XMLHelper.getStringValueOf(subscription, "@productId")));
							}
							
							if(productIds!=null)
							{
								Element subscriptionElem = subscriptionsElem.addElement(IterKeys.ELEM_SUBSCRIPTION);
								
								subscriptionElem.addAttribute( IterKeys.ATTR_GID,			XMLHelper.getStringValueOf(productIds, "@gid")	);
								subscriptionElem.addAttribute( IterKeys.ATTR_LOCALID,		XMLHelper.getStringValueOf(productIds, "@lid")	);
								subscriptionElem.addAttribute( IterKeys.ATTR_SITELOCALID,	XMLHelper.getStringValueOf(subscription, "@sitelocalid")	);
								subscriptionElem.addAttribute( IterKeys.ATTR_SITEGLOBAL_ID,	XMLHelper.getStringValueOf(subscription, "@siteglobalid")	);
							}
						}
					}
					
					Element find = (Element)_articleContent.selectSingleNode( String.format(xpath, fileEntryTitle) );
					if(find!=null)
					{
						Element documentLibraryElement = find.getParent();
						if (binRepository)
						{
							documentLibraryElement.addAttribute( IterKeys.ATTR_GID, "-1");
							documentLibraryElement.addAttribute( IterKeys.ATTR_LOCALID, fileentryId);
						}
						else
						{
							/* gid es "" cuando se añade una imagen a un artículo directamente en el LIVE, por el hecho de no existir ese registro en la tabla Xmlio_Live*/
							if(Validator.isNotNull(gid))
								documentLibraryElement.addAttribute( IterKeys.ATTR_GID	, gid);
							documentLibraryElement.addAttribute( IterKeys.ATTR_LOCALID	, localId	);
						}
						
						if(subscriptionsElem!=null)
						{
							Element dynamicElement = documentLibraryElement.getParent();
							dynamicElement.add( subscriptionsElem );
						}
					}
				}
			}
		}
		
		contentElement.appendContent(_articleContent.getRootElement());
	}
	 
}
