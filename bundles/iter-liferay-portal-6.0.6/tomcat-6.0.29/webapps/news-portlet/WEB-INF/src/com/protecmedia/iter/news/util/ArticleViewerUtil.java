package com.protecmedia.iter.news.util;

import java.io.File;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.render.RenditionMode;
import com.liferay.portal.kernel.util.DateUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.ImageFramesUtil;
import com.liferay.portal.kernel.util.ImageProviderUtil;
import com.liferay.portal.kernel.util.IterSecureConfigTools;
import com.liferay.portal.kernel.util.IterURLUtil;
import com.liferay.portal.kernel.util.KeyValuePair;
import com.liferay.portal.kernel.util.PHPUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.ProcessingInstruction;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XSLUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.asset.model.AssetVocabularyConstants;
import com.liferay.portlet.asset.service.AssetEntryLocalServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterKeys;

public class ArticleViewerUtil
{
	private static Log _log = LogFactoryUtil.getLog(ArticleViewerUtil.class);
	
	private final static String	ELEM_PROTEC_Q		= "protec:q";
	private final static String	ELEM_PROTEC_VALIDFROM	= "protec:validfrom";
	private final static String	ELEM_PROTEC_VALIDTHRU	= "protec:validthru";
	private final static String	ELEM_PRODUCTS		= "products";
	private final static String	DATE_FORMAT			= "yyyy-MM-dd'T'HH:mm:ssZ";
	private final static String	VALUE_INTERNAL		= "internal";
	private final static String	VALUE_EXTERNAL		= "external";
	private final static String NS_XSI 				= "xmlns:xsi";
	private final static String NS_XSI_URI 			= "http://www.w3.org/2001/XMLSchema-instance";
	private final static String XSI_SCHEMA			= "xsi:schemaLocation";
	private final static String XSI_SCHEMA_URI 		= "http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd";
	private final static String NS					= "xmlns";
	private final static String NS_URI				= "http://www.sitemaps.org/schemas/sitemap/0.9";
	private final static String NS_PROTEC			= "xmlns:protec";
	private final static String NS_PROTEC_URI		= "http://schemas.protecmedia.com/schemas/iterwebcms";
	private final static String pattern				= "(?:([\\\\+\\\\-])([0-9]{2})([0-9]{2}))";
	private final static String replacement			= "$1$2:$3";

	private final static String GET_ARTICLE_CATEGORIES = new StringBuilder(
			"SELECT DISTINCT	AssetCategory.categoryId, 																					\n").append(
			"					").append(AssetVocabularyConstants.getNameSQL("voc.name")).append(" vocname,								\n").append(					
			"					AssetCategory.vocabularyId, AssetCategory.parentCategoryId, AssetCategory.name,								\n").append(
			"					(	SELECT IF(INSTR(data_, AssetCategory.categoryId)=0, FALSE, TRUE) 										\n").append(
			"						FROM ExpandoValue 																						\n").append(
			"						INNER JOIN ExpandoColumn ON (ExpandoValue.columnid=ExpandoColumn.columnid AND ExpandoColumn.name='%1$s')\n").append(
			"							WHERE classnameid=%2$s AND classpk=%3$s	) AS maincategory, 											\n").append(
			"					IF (AssetEntries_AssetCategories.entryid IS NULL, FALSE, TRUE) AS selected, 								\n").append(
			"					categoryproperties.aboutid 																					\n").append(
			"FROM AssetVocabulary voc 																										\n").append(
			"INNER JOIN AssetCategory ON (voc.vocabularyId = AssetCategory.vocabularyId) 													\n").append(
			"																																\n").append(			
			"INNER JOIN AssetCategory ChildsAssetCategory ON																				\n").append(
			"	( (		AssetCategory.leftCategoryId <  (SELECT leftCategoryId 																\n").append(
			"											 FROM AssetCategory																	\n").append(
			"											 	WHERE categoryId IN (ChildsAssetCategory.categoryId)							\n").append(
			"									   		)																					\n").append(
			"	   AND 	AssetCategory.rightCategoryId > (SELECT rightCategoryId																\n").append(
			"											 FROM AssetCategory																	\n").append(
			"												WHERE categoryId IN (ChildsAssetCategory.categoryId)							\n").append(
			"										    )																					\n").append(
			"	  )																															\n").append(
			"	  OR AssetCategory.categoryId = ChildsAssetCategory.categoryId																\n").append(
			"	)																															\n").append(
			"																																\n").append(		
			"LEFT JOIN AssetEntries_AssetCategories ON 																						\n").append(
			"	( AssetCategory.categoryId = AssetEntries_AssetCategories.categoryId AND AssetEntries_AssetCategories.entryid=%4$s ) 		\n").append(
			"																																\n").append(		
			"LEFT JOIN categoryproperties ON 																								\n").append(
			"	( categoryproperties.categoryid=AssetCategory.categoryId AND categoryproperties.groupid=%6$s) 								\n").append(
			"																																\n").append(	
			"	WHERE ChildsAssetCategory.categoryId IN (%5$s)																				\n").append(
			"	ORDER BY AssetCategory.leftCategoryId ASC																					\n").toString();

	
	public static Element getMetadataXml(JournalArticle article, long scopegroupId, HttpServletRequest request, String requestURL) throws Throwable
	{
		Element metadata = SAXReaderUtil.createDocument().addElement(IterKeys.ELEM_METADATA);
		
		String articleId = article.getArticleId();
		
		_log.debug("\nGet article metadata: " + articleId);
		
		@SuppressWarnings("serial")
		List<String> artId = new ArrayList<String>(){};
		artId.add(articleId);
		
		List<String> metadatos = TopicsUtil.getAllCategoriesByContentIds(article.getGroupId(), artId);
		
		requestURL = requestURL.substring(0, requestURL.lastIndexOf(StringPool.SLASH)+1).concat("%s");
		
		if(metadatos.size()>0)
		{
			long entryId = AssetEntryLocalServiceUtil.getEntry(IterKeys.CLASSNAME_JOURNALARTICLE, article.getResourcePrimKey()).getEntryId();
			String categories = StringUtils.join(metadatos.iterator(), ',');
			String query = String.format(GET_ARTICLE_CATEGORIES, WebKeys.EXPANDO_COLUMN_NAME_MAIN_METADATAS_IDS, PortalUtil.getClassNameId(JournalArticle.class), article.getId(), entryId, categories, scopegroupId);
			
			_log.debug("\nQuery to get categories path : \n" + query);
			
			Document result = PortalLocalServiceUtil.executeQueryAsDom( query );
			if(result!=null)
			{
				String xpath = "/rs/row[@parentCategoryId=0]";
				List<Node> rootNodes = result.selectNodes(xpath);
				for(Node rootNode : rootNodes)
				{
					String vocId = XMLHelper.getTextValueOf(rootNode, "@vocabularyId");
					xpath = String.format("/metadata/%s[@%s='%s']", IterKeys.ELEM_VOCABULARY, IterKeys.ATTR_HREF, vocId);
					
					Element vocElem = (Element)metadata.selectSingleNode(xpath);
					if(vocElem==null)
					{
						vocElem = metadata.addElement( IterKeys.ELEM_VOCABULARY );
						vocElem.addAttribute(IterKeys.ATTR_NAME, XMLHelper.getTextValueOf(rootNode, "@vocname"));
						vocElem.addAttribute(IterKeys.ATTR_HREF, vocId);
						vocElem.addAttribute("id", vocId);
					}
					
					String cid = XMLHelper.getTextValueOf(rootNode, "@categoryId");
					
					Element categoryElem = vocElem.addElement( IterKeys.ELEM_CATEGORY );
					categoryElem.addAttribute(IterKeys.ATTR_NAME, 	XMLHelper.getTextValueOf(rootNode, "@name"));
					categoryElem.addAttribute(IterKeys.ATTR_MAIN, 	XMLHelper.getTextValueOf(rootNode, "@maincategory"));
					categoryElem.addAttribute(IterKeys.ATTR_SET, 	XMLHelper.getTextValueOf(rootNode, "@selected"));
					categoryElem.addAttribute("id", 				cid);
					
					String aboutId = XMLHelper.getTextValueOf(rootNode, "@aboutid");
					if( Validator.isNotNull(aboutId) )
						categoryElem.addAttribute( IterKeys.ATTR_ABOUT, String.format(requestURL, aboutId) );
					
					
					addCategoryChildren(result, categoryElem, cid, requestURL);
				}
				
				xpath = String.format("/metadata/%s", IterKeys.ELEM_VOCABULARY);
				List<Node> vocNodes = metadata.selectNodes(xpath);
				for(Node voc : vocNodes)
				{
					((Element)voc).remove( ((Element)voc).attribute(IterKeys.ATTR_HREF) );
				}
			}
		}
		
		Element canonicalUrlElem = metadata.addElement(IterKeys.ELEM_LINK);
		String host = IterURLUtil.getIterHost();
		if (Validator.isNull(host))
		{
			StringBuilder sb = new StringBuilder();
			sb.append( request.isSecure() ? Http.HTTPS_WITH_SLASH : Http.HTTP_WITH_SLASH );
			sb.append( request.getServerName() );
			
			int serverPort = request.getServerPort();
			if ((serverPort != Http.HTTP_PORT) && (serverPort != Http.HTTPS_PORT)) 
				sb.append( StringPool.COLON ).append( serverPort );
			
			host = sb.toString();
		}
		
		String canonicalURL = host + IterURLUtil.getArticleURL(scopegroupId, articleId, "canonical", false);
		canonicalUrlElem.addAttribute(IterKeys.ATTR_REL, IterKeys.ATTR_VALUE_SELF);
		canonicalUrlElem.addAttribute(IterKeys.ATTR_HREF,  canonicalURL);
		
		String ampURL = IterURLUtil.getRendererArticleURL(RenditionMode.amp, scopegroupId, article.getArticleId());
		if (Validator.isNotNull(ampURL))
		{
			Element ampUrlElem = metadata.addElement(IterKeys.ELEM_LINK);
			ampUrlElem.addAttribute(IterKeys.ATTR_REL, 	"amphtml");
			ampUrlElem.addAttribute(IterKeys.ATTR_HREF, host.concat(ampURL));
		}
			
		Element properties = metadata.addElement(IterKeys.ELEM_PROPERTIES);
		DateFormat df = new SimpleDateFormat(DATE_FORMAT);
		String createDateFormated = df.format( article.getCreateDate() );
		String modifiedDateFormated = df.format( article.getModifiedDate() );
		properties.addAttribute(IterKeys.ATTR_CREATEDATE, createDateFormated.replaceFirst(pattern, replacement));
		properties.addAttribute(IterKeys.ATTR_MODIFIEDDATE, modifiedDateFormated.replaceFirst(pattern, replacement));

		Element sections = metadata.addElement(IterKeys.ELEM_SECTIONS);
		addArticleSections( articleId, scopegroupId, sections, requestURL );
		
		addArticleProducts(scopegroupId, article.getArticleId(), metadata);
		
		if(_log.isDebugEnabled())
			_log.debug("\n Metadata node: \n\t" + metadata.asXML() );
		
		return metadata;
	}
	
	private static final String SQL_GET_ARTICLE_PRODUCT = new StringBuilder()
	.append(" SELECT group_concat(DISTINCT p.name) mlnproducts, group_concat(DISTINCT ipp.pname) paywallproducts \n")
	.append(" FROM articleproduct ap                                                                             \n")
	.append(" LEFT OUTER JOIN product p ON p.productId = ap.productId AND p.groupid=%1$d                         \n")
	.append(" LEFT OUTER JOIN iterpaywall_product_related ippr ON ippr.productid = p.productId                   \n")
	.append(" LEFT OUTER JOIN iterpaywall_product ipp ON ipp.id = ippr.paywallproductid AND ipp.groupid=%1$d     \n")
	.append(" WHERE p.groupId=%1$d AND ap.articleId = '%2$s'                                                     \n")
	.toString();
	
	private static void addArticleProducts(long groupid, String articleid, Element metadata) throws SecurityException, NoSuchMethodException
	{
		String sql = String.format(SQL_GET_ARTICLE_PRODUCT, groupid, articleid);
		Document d = PortalLocalServiceUtil.executeQueryAsDom(sql);
		String mlnProducts = XMLHelper.getStringValueOf(d, "/rs/row/@mlnproducts");
		String paywallProducts = XMLHelper.getStringValueOf(d, "/rs/row/@paywallproducts");
		
		String[] products = Validator.isNotNull(paywallProducts) ? paywallProducts.split(StringPool.COMMA) : Validator.isNotNull(mlnProducts) ? mlnProducts.split(StringPool.COMMA) : (new String[] {});
		if (products.length > 0)
		{
			for (String product : products)
			{
				metadata.addElement(ELEM_PRODUCTS).setText(product);
			}
		}
	}
	
	private static void addCategoryChildren(Document dom, Element elem, String categoryid, String requestURL)
	{
		String xpath = String.format("/rs/row[@parentCategoryId=%s]", categoryid);
		List<Node> children = dom.selectNodes( xpath );
		for(Node child : children)
		{
			String cid = XMLHelper.getTextValueOf(child, "@categoryId");
			
			Element categoryElem = elem.addElement( IterKeys.ELEM_CATEGORY );
			categoryElem.addAttribute(IterKeys.ATTR_NAME, 	XMLHelper.getTextValueOf(child, "@name"));
			categoryElem.addAttribute(IterKeys.ATTR_MAIN, 	XMLHelper.getTextValueOf(child, "@maincategory"));
			categoryElem.addAttribute(IterKeys.ATTR_SET, 	XMLHelper.getTextValueOf(child, "@selected"));
			categoryElem.addAttribute("id", 				cid);
			
			String aboutId = XMLHelper.getTextValueOf(child, "@aboutid");
			if( Validator.isNotNull(aboutId) )
				categoryElem.addAttribute( IterKeys.ATTR_ABOUT, String.format(requestURL, aboutId) );

				
			addCategoryChildren(dom, categoryElem, cid, requestURL);	
		}
	}
	
	private static final String ARTICLE_SECTIONS = new StringBuilder(
		"SELECT l.friendlyurl, n.defaultSection, n.vigenciahasta AS validthru, n.vigenciadesde AS validfrom, 						\n").append(
		"		q.name AS qualif, ExtractValue(l.name, '/root/name[1]/text()') layoutname, sectionproperties.aboutid,				\n").append(
		"       if(n.articleModelId <> -1, Tpl.name, DefaultTpl.name) model,														\n").append(
		"       if(n.articleModelId <> -1, 0, 1) defaultmodel,																		\n").append(
		"       if(n.articleModelId <> -1, Tpl.id_, DefaultTpl.id_) modelid															\n").append(
		"FROM 	News_Qualification q																								\n").append(
		"INNER 	JOIN News_PageContent n ON (n.qualificationId=q.qualifId AND q.groupId=%s AND n.contentId='%s' AND n.online_=TRUE)	\n").append(
		"INNER 	JOIN Layout l ON (n.layoutId=l.uuid_)																				\n").append(
		"LEFT 	JOIN sectionproperties ON (sectionproperties.plid = l.plid)															\n").append(
		"LEFT 	JOIN designer_pagetemplate DefaultTpl ON (DefaultTpl.type_ = 'article-template'										\n").append(
		"                 								  and l.groupId = DefaultTpl.groupId										\n").append(
		"                  								  and DefaultTpl.defaultTemplate = 1)										\n").append(
		"LEFT 	JOIN designer_pagetemplate Tpl ON (n.articleModelId = Tpl.id_) 														\n").toString();

	private static void addArticleSections( String articleId, long scopegroupId, Element sections, String requestURL ) throws ParseException
	{
		String sql = String.format(ARTICLE_SECTIONS, scopegroupId, articleId);
		_log.debug("\n Get article sections: \n" + sql);
		
		List<Map<String, Object>> queryResult = PortalLocalServiceUtil.executeQueryAsMap(sql);
		
		if(queryResult!=null && queryResult.size()>0)
		{
			DateFormat df = new SimpleDateFormat(DATE_FORMAT);
			
			for(Map<String, Object> row : queryResult)
			{
				String layoutName 		= String.valueOf( row.get("layoutname") );
				String layoutUrl 		= String.valueOf( row.get("friendlyurl") );
				boolean mainSection 	= GetterUtil.getBoolean( String.valueOf( row.get("defaultSection") ) ) ;
				String qualification 	= String.valueOf( row.get("qualif") );
				Date articleValidFrom 	= DateUtil.getDBFormat().parse( String.valueOf( row.get("validfrom")) );
				Date articleValidThru 	= DateUtil.getDBFormat().parse( String.valueOf( row.get("validthru")) );
				String aboutId 			= String.valueOf( row.get("aboutid") );
				
				// ITER-938 REST API: proporcionar el modelo de artículo para cada sección asignada
				String model			= String.valueOf( row.get("model") );	
				String defaultmodel		= String.valueOf( row.get("defaultmodel") );	
				String modelid			= String.valueOf( row.get("modelid") );	
				
				Element sectionElem = SAXReaderUtil.createElement(IterKeys.ELEM_SECTION);
				
				sectionElem.addAttribute(IterKeys.ATTR_NAME, layoutName);
				sectionElem.addAttribute(IterKeys.ATTR_URL, layoutUrl);
				if(mainSection)
					sectionElem.addAttribute(IterKeys.ATTR_MAIN, "1");
				String articleValidFromFormated = df.format( articleValidFrom );
				sectionElem.addAttribute(IterKeys.ATTR_VALIDFROM, articleValidFromFormated.replaceFirst(pattern, replacement) );
				String articleValidThruFormated = df.format( articleValidThru );
				sectionElem.addAttribute(IterKeys.ATTR_VALIDTHRU, articleValidThruFormated.replaceFirst(pattern, replacement) );
				sectionElem.addAttribute(IterKeys.ATTR_Q, qualification);
				
				if( Validator.isNotNull(aboutId) )
					sectionElem.addAttribute( IterKeys.ATTR_ABOUT, String.format(requestURL, aboutId) );

				sectionElem.addAttribute(IterKeys.ATTR_MODEL, 			model);
				sectionElem.addAttribute(IterKeys.ATTR_DEFAULTMODEL, 	defaultmodel);
				sectionElem.addAttribute(IterKeys.ATTR_MODELID, 		modelid);
				
				sections.add(sectionElem);
			}
		}
	}
	
	public static Element getRelatedContent(JournalArticle article, String requestURL)
	{
		String ext = FilenameUtils.getExtension(requestURL);
		if (!ext.isEmpty())
			ext = ".".concat(ext);
			
		requestURL = requestURL.substring(0, requestURL.lastIndexOf(StringPool.SLASH)+1);
		
		Element relatedContent = SAXReaderUtil.createDocument().addElement(IterKeys.ELEM_RELATEDCONTENT);
		
		List<Object> externalLinks = TeaserContentUtil.getRelatedExternalLinks( article.getContent() );
		List<KeyValuePair> internalLinks = TeaserContentUtil.getRelatedInternalLinks( article );
		
		for(KeyValuePair intLink : internalLinks)
		{
			Element link = relatedContent.addElement(IterKeys.ELEM_LINK);
			link.addAttribute(IterKeys.ATTR_REL, VALUE_INTERNAL);
			link.addAttribute(IterKeys.ATTR_HREF, requestURL.concat(intLink.getKey()).concat(ext));
			link.setText( intLink.getValue() );
		}
		
		for(Object obj : externalLinks)
		{
			Object[] extLink = (Object[]) obj;
			Element link = relatedContent.addElement(IterKeys.ELEM_LINK);
			link.addAttribute(IterKeys.ATTR_REL, VALUE_EXTERNAL);
			link.addAttribute(IterKeys.ATTR_HREF, String.valueOf(extLink[1]));
			link.setText(String.valueOf(extLink[0]));
		}
		
		return relatedContent;
	}
	
	public static Node applyRenditions( Node content, long scopegroupId, long globalGroupId, String articleId ) throws Throwable
	{
		String emptyImagesPath = "/root/dynamic-element/dynamic-element[@type='document_library' and @name='_img-Binary_']/dynamic-content[string-length(text()) = 0]";
		List<Node> nodesToDelete = content.selectNodes(emptyImagesPath);
		if(nodesToDelete!=null && nodesToDelete.size()>0)
		{
			for(Node toDelete : nodesToDelete)
			{
				Node parent = toDelete.getParent();
				if(parent!=null)
					parent = parent.getParent();
				if(parent!=null)
					parent.detach();
			}
		}
		
		String xpathImages = "/root/dynamic-element/dynamic-element[@type='document_library' and @name='_img-Binary_']";
		Group grp = GroupLocalServiceUtil.getGroup(scopegroupId);
		String[] renditions = PropsUtil.getArray( 
											String.format(PropsKeys.ITER_JOURNALALACARTA_IMAGE_RENDITIONS_GROUPFRIENDLYURL, 
											grp.getFriendlyURL().replaceAll(StringPool.SLASH, StringPool.PERIOD)) 
												);
		
		List<Node> imageNodes = content.selectNodes(xpathImages);
		
		if(renditions!=null && renditions.length>0)
		{
			ImageProviderUtil imageProvider = new ImageProviderUtil((Element)content, globalGroupId, scopegroupId, articleId, false);
			HashMap<String, Integer> dynamicElementsMap = new HashMap<String, Integer>();
			
				for(Node imgNode : imageNodes)
				{
					Node imgParent = imgNode.getParent();
					String contentType = XMLHelper.getTextValueOf(imgParent, "@name");

					int idx = 0;
					Integer dynamicElementsIdx = dynamicElementsMap.get(contentType);
					if(dynamicElementsIdx!=null)
						idx = dynamicElementsIdx.intValue()+1;
					dynamicElementsMap.put(contentType, idx);
					
					for(String rendition : renditions)
					{
						String imgPath = imageProvider.getImageURL( contentType, String.valueOf(idx), rendition);
						
						Element dynamicElement = ((Element)imgParent).addElement("dynamic-element");
						dynamicElement.addAttribute("type", "document_library");
						dynamicElement.addAttribute("name", rendition);
						Element dynamicContent = dynamicElement.addElement("dynamic-content");
						dynamicContent.setText(imgPath);
					}
			}
			
			for(Node imgNode : imageNodes)
			{
				imgNode.detach();
			}
			
		}
		else
		{
			if(imageNodes!=null && imageNodes.size()>0)
			{
				for(Node imgNode : imageNodes)
				{
					Node parent = imgNode.getParent();
					if(parent!=null)
						parent.detach();
				}
			}
		}
		
		return content;
	}
	
	public static String transformArticleContent(HttpServletRequest request, Node content, String articleId, long scopegroupId) throws Exception
	{
		String stringResult = "";
		String xslpath = new StringBuilder("").append(File.separatorChar).append("news-portlet")
							.append(File.separatorChar).append("xsl")
							.append(File.separatorChar).append("articleViewer.xsl").toString();
		
		Map<String, String> params = new HashMap<String, String>();
		params.put( "articleid", articleId );
		params.put( "groupid", String.valueOf(scopegroupId));
		
		if( PropsValues.ITER_ENVIRONMENT.equalsIgnoreCase(com.liferay.portal.kernel.util.WebKeys.ENVIRONMENT_LIVE) )
			params.put( "url", StringPool.BLANK );
		else
			params.put( "url", PortalUtil.getPortalURL(request) );

		stringResult = XSLUtil.transformXML(content.asXML(), xslpath, params );
		stringResult = fillReadingTime(articleId, scopegroupId, stringResult);

		return stringResult;
	}
	
	/**
	 * Se añade el tiempo en segundos pasado el cuál se considerará leído el artículo.
	 * http://jira.protecmedia.com:8080/browse/ITER-694?focusedCommentId=26145&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-26145
	 * @throws DocumentException 
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	static private String fillReadingTime(String articleId, long scopegroupId, String stringResult) throws DocumentException, PortalException, SystemException
	{
		double wordsPerSeconds = GetterUtil.getInteger(GroupConfigTools.getGroupConfigXMLField(scopegroupId, "visitstatistics", "@wordsperminute"), 0)/60.;
		if (wordsPerSeconds > 0)
		{
			Document dom = SAXReaderUtil.read(stringResult);
			String xpath = "/article/content/component[@name=\"%s\"]";
			
			// Se localizan los componentes Headline
			Map<String, String> alternativeTypesByGroup = null;
			String contentType = "Headline";
			List<Node> headlineList = dom.selectNodes( String.format(xpath, contentType) );
			
			if (headlineList.isEmpty() && 
				(alternativeTypesByGroup = ImageFramesUtil.getAlternativeTypes( GroupLocalServiceUtil.getGroup(scopegroupId).getDelegationId() )) != null)
			{
				do
				{
					// Se localizan los alternativos a Headline
					contentType = alternativeTypesByGroup.get(contentType);
					if (contentType != null)
						headlineList = dom.selectNodes( String.format(xpath, contentType) );
				}
				while (headlineList.isEmpty() && contentType != null);
			}
			StringBuilder text  = new StringBuilder();
			
//			// Si no tiene Headline se tomará el título, tal y como se hace en el RSS
			// No se toma el título del artículo como en los RSS porque el título del artículo no está disponible en el getArticle, no se devolverá
//			if (headlineList.isEmpty())
//			{
//				text.append( JournalArticleLocalServiceUtil.getArticle(GroupMgr.getGlobalGroupId(), articleId).getTitle() );
//			}
//			else
//			{
				for (Node txtNode : headlineList)
					text.append(XMLHelper.getStringValueOf(txtNode, ".", "")).append(" ");
//			}
			
			// Se toman los Text
			List<Node> txtList 	= dom.selectNodes(String.format(xpath, "Text") );
			for (Node txtNode : txtList)
				text.append(XMLHelper.getStringValueOf(txtNode, ".", "")).append(" ");
			
			// Se calcula total de palabras y tiempo de lectura
			int numWords 		= text.toString().split("\\s+").length;
			long readingTime	= Math.round(Math.ceil( numWords / wordsPerSeconds ));

			if (readingTime > 0)
			{
				Element propsElem = (Element)dom.selectSingleNode("/article/metadata/properties");
				if (propsElem != null)
				{
					propsElem.addAttribute("readingtime", String.valueOf(readingTime));
					stringResult = dom.asXML();
				}
			}
		}
		return stringResult;
	}
	
	public static String setPhp(HttpServletRequest request, HttpServletResponse response, String articleId, long scopeGroupId, String article, Date articleModifiedDate, boolean isJSON) throws Exception
	{
		String result = "";
		
		if(PHPUtil.isApacheRequest(request))
		{
			String productsList = PortalLocalServiceUtil.getProductsByArticleId(articleId);
			
			if( productsList.length()>0 )
			{
				Document doc = SAXReaderUtil.createDocument();
				
				String contentType = String.format("header(\"Content-Type: %s\");", isJSON ? "application/json" : "text/xml");
				
				ProcessingInstruction phpHeaderXML = SAXReaderUtil.createProcessingInstruction("php", contentType);
				doc.add( phpHeaderXML );
				
				ProcessingInstruction pi = SAXReaderUtil.createProcessingInstruction("php", 
									PHPUtil.getCheckAccessPHPCode(request, response, scopeGroupId, false));
				doc.add(pi);
				
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(WebKeys.URL_PARAM_DATE_FORMAT_FULL);
				String modifiedDate = simpleDateFormat.format(articleModifiedDate);
				
				ProcessingInstruction phpCheckUser = 
							 SAXReaderUtil.createProcessingInstruction("php", "if (user_has_access_to_any_of_these_products( '" + 
																	articleId 								+ 	"', array("		+ 
																	productsList 							+ 	"),'" 			+ 
																	modifiedDate 							+ 	"' )===true){");
				doc.add(phpCheckUser);
				
				ProcessingInstruction xmlDeclaration = 
								SAXReaderUtil.createProcessingInstruction("xml", "version=\"1.0\" encoding=\"UTF-8\"");
				
				doc.add(xmlDeclaration);
				
				doc.add( SAXReaderUtil.read(article).getRootElement().detach() );
				
				doc.add( SAXReaderUtil.createProcessingInstruction( "php","}else{ header('HTTP/1.0 403 Forbidden');") );
				doc.add( SAXReaderUtil.createProcessingInstruction( "php","}") );
				
				result = doc.formattedStringWithoutXMLDeclaration();
			}
		}
		
		return result;
	}

	public static String getArticlesOfSection(HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		long scopeGroupId = PortalUtil.getScopeGroupId(request);
		
		String sectionFriendlyUrl = request.getPathInfo();
		
		String requestURL = request.getRequestURL().toString();
		
		// ITER-889	getArticle no usa el protocolo correcto en los links de relacionados ni artículos instrumentales
		URL urlObj = new URL(requestURL);
		requestURL = new URL(IterSecureConfigTools.getConfiguredHTTPS(scopeGroupId) ? Http.HTTPS : Http.HTTP, urlObj.getHost(), urlObj.getFile()).toString();

		
		String servletPath = request.getServletPath();
		int idx = requestURL.indexOf(servletPath);
		
		if(Validator.isNull(sectionFriendlyUrl))
		{
			if(Validator.isNotNull(requestURL))
			{
				int length = servletPath.length();
				sectionFriendlyUrl = requestURL.substring( idx+length );
			}
		}
		
		String ext 		= FilenameUtils.getExtension(sectionFriendlyUrl);
		boolean isJSON	= "json".equalsIgnoreCase(ext);
		if ("xml".equalsIgnoreCase(ext) || isJSON)
			sectionFriendlyUrl = FilenameUtils.removeExtension(sectionFriendlyUrl);
		
		String url = requestURL.substring(0, idx);
		
		LayoutLocalServiceUtil.getFriendlyURLLayout(scopeGroupId, false, sectionFriendlyUrl);
		
		String query = new StringBuilder("SELECT DISTINCT CONCAT('%1$s','/getArticle/', j.articleId) AS url , j.modifiedDate, q.name AS qualif, n.vigenciadesde AS validfrom, n.vigenciahasta AS validthru, \n")
				.append(" group_concat(DISTINCT p.nameBase64) productslist, group_concat(DISTINCT p.name) mlnproducts, group_concat(DISTINCT ipp.pname) paywallproducts \n")
				.append(" FROM JournalArticle j  USE INDEX (XYZ_ITR_IX_JOURNALARTICLE_ARTICLEID_ZYX)\n")
				.append(" INNER JOIN News_PageContent n  USE INDEX (XYZ_ITR_IX_NEWS_PAGECONTENT_VIGHASTA_ZYX)  ON (n.contentId=j.articleId AND n.online_=TRUE)\n")
				.append(" INNER JOIN News_Qualification q ON (n.qualificationId=q.qualifId AND q.groupId=%2$s)\n")
				.append(" LEFT OUTER JOIN articleproduct ap ON ap.articleId = j.articleId                                \n")
				.append(" LEFT OUTER JOIN product p ON p.productId = ap.productId AND p.groupid=%2$s                     \n")
				.append(" LEFT OUTER JOIN iterpaywall_product_related ippr ON ippr.productid = p.productId               \n")
				.append(" LEFT OUTER JOIN iterpaywall_product ipp ON ipp.id = ippr.paywallproductid AND ipp.groupid=%2$s \n")
				.append(" WHERE n.groupId=%2$s\n")
				.append(" AND n.layoutId = (SELECT uuid_ FROM Layout WHERE groupid=%2$s AND friendlyurl='%3$s')\n")
				.append(" AND j.structureId='STANDARD-ARTICLE'\n")
				.append(" AND ('%4$s' BETWEEN n.vigenciadesde AND n.vigenciahasta)")
				.append(" GROUP BY j.articleid \n")
				.append(" ORDER BY n.vigenciadesde DESC, j.articleId DESC")
				.toString();
		
		Date validityDate = GroupMgr.getPublicationDate(scopeGroupId);
		SimpleDateFormat format  = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_00);
		String referenDateString = format.format(validityDate);
		
		query = String.format(query, url, scopeGroupId, sectionFriendlyUrl, referenDateString);
		
		List<Map<String, Object>> queryResult = PortalLocalServiceUtil.executeQueryAsMap(query);

		return createDomResult( queryResult, request, response, scopeGroupId, isJSON );
	}
	
	private static String createDomResult( List<Map<String, Object>> listMap, HttpServletRequest request, HttpServletResponse response, long scopeGroupId, boolean isJSON ) throws Exception
	{
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(WebKeys.URL_PARAM_DATE_FORMAT_FULL);
		
		boolean phpEnabled = PHPUtil.isApacheRequest(request);
		
		Document doc = SAXReaderUtil.createDocument();

		if (phpEnabled)
		{
			String contentType = String.format("header(\"Content-Type: %s\");", isJSON ? "application/json" : "text/xml");

			ProcessingInstruction phpHeaderXML = SAXReaderUtil.createProcessingInstruction("php", contentType);
			doc.add( phpHeaderXML );
			
			ProcessingInstruction phpfunctions = SAXReaderUtil.createProcessingInstruction("php", 
												PHPUtil.getCheckAccessPHPCode(request, response, scopeGroupId, false));
			doc.add( phpfunctions );
		}
		
		ProcessingInstruction xmlDeclaration = 
			SAXReaderUtil.createProcessingInstruction("xml", "version=\"1.0\" encoding=\"UTF-8\"");
		
		doc.add(xmlDeclaration);
		
		Element urlSet = SAXReaderUtil.createElement(IterKeys.ELEM_URLSET);
		urlSet.addAttribute(NS_XSI, NS_XSI_URI);
		urlSet.addAttribute(XSI_SCHEMA, XSI_SCHEMA_URI);
		urlSet.addAttribute(NS, NS_URI);
		urlSet.addAttribute(NS_PROTEC, NS_PROTEC_URI);
		
		doc.add(urlSet);
		
		if(listMap!=null && listMap.size()>0)
		{
			DateFormat df = new SimpleDateFormat(DATE_FORMAT);
			
			for(Map<String, Object> row : listMap)
			{
				String articleUrl = String.valueOf( row.get("url") );
				Date articleModDate = DateUtil.getDBFormat().parse( String.valueOf( row.get("modifiedDate")) );
				String qualification = String.valueOf( row.get("qualif") );
				Date articleValidFrom = DateUtil.getDBFormat().parse( String.valueOf( row.get("validfrom")) );
				Date articleValidThru = DateUtil.getDBFormat().parse( String.valueOf( row.get("validthru")) );
				String productsList = String.valueOf( row.get("productslist") );
				String mlnproducts = String.valueOf( row.get("mlnproducts") );
				String paywallProducts = String.valueOf( row.get("paywallproducts") );
				
				boolean articleHasProducts = Validator.isNotNull(productsList);
				
				if( phpEnabled && articleHasProducts )
				{
					String articleId = articleUrl.substring( articleUrl.lastIndexOf(StringPool.SLASH)+1 );
					productsList = StringUtil.apostrophe(StringUtil.merge(productsList.split(StringPool.COMMA), "','"));
					String modifiedDate = simpleDateFormat.format(articleModDate);
					
					com.liferay.portal.kernel.xml.ProcessingInstruction phpIF = SAXReaderUtil.createProcessingInstruction("php", "if (user_has_access_to_any_of_these_products( '" 				+ 
																articleId 								+ 	"', array("		+ 
																productsList 							+ 	"),'" 			+ 
																modifiedDate 							+ 	"' )===true){");
					urlSet.add(phpIF);
				}
				
				Element urlElem = SAXReaderUtil.createElement(IterKeys.ELEM_URL);
				
				Element loc = SAXReaderUtil.createElement(IterKeys.ELEM_LOC);
				if (isJSON)
					articleUrl = articleUrl.concat(".json");
				loc.setText(articleUrl);
				urlElem.add(loc);
				
				Element lastMod = SAXReaderUtil.createElement(IterKeys.ELEM_LASTMOD);
				String articleModDateFormated = df.format( articleModDate );
				lastMod.setText( articleModDateFormated.replaceFirst(pattern, replacement) );
				urlElem.add(lastMod);
				
				Element qElem = SAXReaderUtil.createElement(ELEM_PROTEC_Q);
				qElem.setText(qualification);
				urlElem.add(qElem);
				
				Element validFrom = SAXReaderUtil.createElement(ELEM_PROTEC_VALIDFROM);
				String articleValidFromFormated = df.format( articleValidFrom );
				validFrom.setText( articleValidFromFormated.replaceFirst(pattern, replacement) );
				urlElem.add(validFrom);
				
				Element validThru = SAXReaderUtil.createElement(ELEM_PROTEC_VALIDTHRU);
				String articleValidThruFormated = df.format( articleValidThru );
				validThru.setText( articleValidThruFormated.replaceFirst(pattern, replacement) );
				urlElem.add(validThru);
				
				if (articleHasProducts)
				{
					String[] products = Validator.isNotNull(paywallProducts) ? paywallProducts.split(StringPool.COMMA) : Validator.isNotNull(mlnproducts) ? mlnproducts.split(StringPool.COMMA) : (new String[] {});
					for (String product : products)
					{
						Element productElement = SAXReaderUtil.createElement(ELEM_PRODUCTS);
						productElement.setText(product);
						urlElem.add(productElement);
					}
				}
				
				urlSet.add(urlElem);

				if( phpEnabled && articleHasProducts )
				{
					urlSet.add( SAXReaderUtil.createProcessingInstruction("php", "}") );
				}
			}
		}

		String result = doc.formattedStringWithoutXMLDeclaration();
		if (isJSON)
			result = JSONUtil.toJSONString(result);

		return result;
	}
	
}
