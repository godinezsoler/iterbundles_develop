/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.news.util;

import java.io.UnsupportedEncodingException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.portlet.RenderRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.SolrFilterArticles;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.CategoriesUtil;
import com.liferay.portal.kernel.util.Digester;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.KeyValuePair;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PHPUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.UnrepeatableArticlesMgr;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.util.request.PublicIterParams;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.HtmlOptimizer;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.asset.model.AssetCategory;
import com.liferay.portlet.asset.model.AssetEntry;
import com.liferay.portlet.asset.service.AssetCategoryLocalServiceUtil;
import com.liferay.portlet.asset.service.AssetEntryLocalServiceUtil;
import com.liferay.portlet.asset.service.persistence.AssetEntryQuery;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portlet.journal.util.JournalArticleTools;
import com.liferay.portlet.journal.util.RelatedLinkData;
import com.liferay.util.survey.IterSurveyUtil;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.HotConfigUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.OutdatedDate;
import com.protecmedia.iter.news.service.CategorizeLocalServiceUtil;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;

public class TeaserContentUtil 
{
	private static Log _catUseOnlyParents	= LogFactoryUtil.getLog("filterArticles.categories.useOnlyParents");
	private static Log _log 			 	= LogFactoryUtil.getLog(TeaserContentUtil.class);
	private static Log _reloadTeaserIndex	= LogFactoryUtil.getLog("teaser.indexes.reload");
	
	private static ResourceBundle rb =  ResourceBundle.getBundle("content.Language");
	
	private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat(WebKeys.URL_PARAM_DATE_FORMAT_FULL);
	
	private static List<String> ordersValuesIsUsedInIterStatistics = Arrays.asList(new String[]{Integer.toString(IterKeys.ORDER_COMMENT), Integer.toString(IterKeys.ORDER_SHARED)});
	
	private static int OUTDATED_NUMINTERVALS = GetterUtil.getInteger(PropsUtil.get(IterKeys.PORTAL_PROPERTIES_KEY_ITER_QUERY_OUTDATED_NUMINTERVALS), 1	);
	
	private static final String 		PAGECONTENT_TOKEN	  = "XYZ_PAGECONTENT_TOKEN_ZYX";
	private static final StringBuilder  PAGECONTENT_SUBSELECT = new StringBuilder(
		" j.articleid in (  SELECT distinct n.contentid	\n").append(
		"                   FROM News_PageContent n 	\n").append(
		"                     WHERE n.online_=TRUE		\n").append(
	    "                       AND n.groupId=%d		\n").append(
	    "                )\n"							   );
	
	private static final String SQL_USER_CATEGORY_FILTER_JOINS = "LEFT JOIN AssetEntries_AssetCategories a2 ON a2.entryId=e.entryId \nLEFT JOIN AssetCategory c2 ON c2.categoryId=a2.categoryId \n";
	
	//vínculos internos y externos
	public static List<RelatedLinkData> getRelatedInternalExternalLinks( long globalGroupId, String contentId, int start, int maxLinks )
	{
		List<RelatedLinkData> relatedLinks = new ArrayList<RelatedLinkData>();
		
		try
		{
			relatedLinks = JournalArticleTools.getInternalExternalLinks(contentId, start, maxLinks);
		}
		catch (Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return relatedLinks;	
	}
	
	public static JournalArticle getJournalArticle( long globalGroupId, String contentId  )
	{
		JournalArticle article = null;

		try
		{
			article = JournalArticleLocalServiceUtil.getArticle( globalGroupId, contentId );
		}
		catch (Exception e)
		{
			_log.error("TeaserContentUtil.getJournalArticle. Unable to find the JournalArticle with articleId " + contentId );
			e.printStackTrace();
		}
		
		return article;
	}

	private static RelatedLinkData createRelatedLink(String type, String title, String lClass, String  cId, String link)
	{
		return createRelatedLink(type, title, lClass, null, cId, link);
	}
	
	private static RelatedLinkData createRelatedLink( String type, String title, String lClass, String rel, String  cId, String link   )
	{
		RelatedLinkData relatedLink = new RelatedLinkData( type, title, lClass, rel, cId, link  );
		
		return relatedLink;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	//vínculos internos
	//
	public static List<RelatedLinkData> getInternalLinksOrderMLN(long groupId, String contentId, int start, int end) 
	{
		
		List<RelatedLinkData> relatedLinks = new ArrayList<RelatedLinkData>();
		
		JournalArticle article = null;
		article = getJournalArticle( groupId, contentId );
		if ( article != null )
			relatedLinks = getInternalLinksOrderMLN( article, start, end );
		
		return relatedLinks;
	}
	
	public static List<RelatedLinkData> getInternalLinksOrderMLN( JournalArticle article, int start, int maxLinks) 
	{
		
		List<RelatedLinkData> relatedLinks = new ArrayList<RelatedLinkData>();
		
		Document doc = null;
	
		try {
			doc = SAXReaderUtil.read( article.getContent() );
			
			List<Node> nodes = doc.selectNodes("/root/dynamic-element[@name='InternalLink']");
			if( nodes != null  && nodes.size() > 0)
			{
				int limit = Math.min( start + maxLinks, nodes.size() );
					
				for( int i = start; i < limit;  i++  )
				{
					//InternalLink
//					String type = XMLHelper.getStringValueOf( nodes.get(i), "@name" );
					String title = XMLHelper.getStringValueOf( nodes.get(i), "dynamic-content/text()");
					String linkClass = "";
					String linkClassElement = XMLHelper.getStringValueOf( nodes.get(i), "dynamic-element[@name='Class']/dynamic-content/text()");
					if(   linkClassElement != null )
						linkClass  = linkClassElement;
					
					String contentId = XMLHelper.getStringValueOf( nodes.get(i), "dynamic-element[@name='ContentId']/dynamic-content/text()");
					// Se descarta el vacío y él como link interno a si mismo 
					if (!contentId.equals(StringPool.BLANK) && !contentId.equals(article.getArticleId() ) )
					{
						RelatedLinkData relatedLink = createRelatedLink( "InternalLink", title, linkClass, contentId, "" );
						relatedLinks.add( relatedLink );
					}	
				}
			}
		} 
		catch (Exception e) 
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return relatedLinks;	
	}
	
	
	public static List<RelatedLinkData> getInternalLinksNotOrderMLN(long groupId, String contentId, List<String[]> partialListInternalLinks ) 
	{
		List<RelatedLinkData> relatedLinks = new ArrayList<RelatedLinkData>();
		
		JournalArticle article = null;
		article = getJournalArticle( groupId, contentId );
		if ( article != null )
		{
			Document doc = null;
			try 
			{
				doc = SAXReaderUtil.read( article.getContent() );
				for(  int i = 0; i < partialListInternalLinks.size();  i++)
				{
					String[] link = partialListInternalLinks.get(i);
					String contentId_link = link[0];
					String title = link[1];
					
					String linkClass = getLinkClassForInternalLink( doc,  contentId_link );
					RelatedLinkData relatedLink = createRelatedLink( "InternalLink", title, linkClass, contentId_link, "" );
					relatedLinks.add( relatedLink );
				}
			}
			catch (Exception e) 
			{
				_log.error(e.toString());
				_log.trace(e);
			}
		}
			
		
		return relatedLinks;
	}
	
	private static String getLinkClassForInternalLink( Document doc, String contentId ) 
	{
		String linkClass = "";
		List<Node> nodes = doc.selectNodes(String.format("/root/dynamic-element[@name='InternalLink' and dynamic-element[@name='ContentId']/dynamic-content/text()= '%s']" , contentId));
		if( nodes != null  && nodes.size() > 0)
		{
			String linkClassElement = XMLHelper.getStringValueOf( nodes.get(0), "dynamic-element[@name='Class']/dynamic-content/text()");
			if(   linkClassElement != null )
				linkClass  = linkClassElement;	
		}
		
		return linkClass;	
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static List<KeyValuePair> getInternalLinkContent(long groupId, String contentId, String structureId, int start, int end) 
	{
		List<KeyValuePair> articles = new ArrayList<KeyValuePair>();
		try 
		{
			JournalArticle content = JournalArticleLocalServiceUtil.getArticle(groupId, contentId);
			List<KeyValuePair> relatedLinks = getRelatedInternalLinks(content);	
			
			for (int i = start; i < end && i < relatedLinks.size(); i++) 
			{				
				String link = relatedLinks.get(i).getKey();				
				JournalArticle article = JournalArticleLocalServiceUtil.getArticle(content.getGroupId(), link);
			
				if (structureId.equalsIgnoreCase("") || article.getStructureId().equalsIgnoreCase(structureId))
					articles.add(relatedLinks.get(i));
			}
		} 
		catch (Exception e) {}
		
		return articles;
	}	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	//vínculos externos, eliminar los dos métodos getExternalLinkContent cuando acabe con las pruebas...
	public static List<Object> getExternalLinkContent(JournalArticle content, int start, int end) {
		
		List<Object> relatedLinks = new ArrayList<Object>();
		try {
			relatedLinks = getRelatedExternalLinks(content.getContent());
			int limit = Math.min( end, relatedLinks.size() );
			return relatedLinks.subList(start, limit);
			
		} catch (Exception e) {
			return relatedLinks;
		}
	}
	
	public static List<Object> getExternalLinkContent(long groupId, String contentId, int start, int end) {
		JournalArticle content;
		try {
			content = JournalArticleLocalServiceUtil.getArticle(groupId, contentId);
			
		} catch (Exception e) {
			content = null;
		}
		return getExternalLinkContent(content, start, end);
	}

	
	public static List<RelatedLinkData> getExternalLinkContent2(long groupId, String contentId, int start, int end) {
		
		List<RelatedLinkData> relatedLinks = new ArrayList<RelatedLinkData>();
		
		JournalArticle article = null;
		article = getJournalArticle( groupId, contentId );
		if ( article != null )
			relatedLinks = getExternalLinkContent2( article, start, end );
		
		return relatedLinks;
	}
	
	public static List<RelatedLinkData> getExternalLinkContent2( JournalArticle article, int start, int maxLinks) 
	{
		
		List<RelatedLinkData> relatedLinks = new ArrayList<RelatedLinkData>();
		
		Document doc = null;
	
		try {
			doc = SAXReaderUtil.read( article.getContent() );
			
			List<Node> nodes = doc.selectNodes("/root/dynamic-element[@name='ExternalLink']");
			if( nodes != null  && nodes.size() > 0)
			{
				int limit = Math.min( start + maxLinks, nodes.size() );
					
				for( int i = start; i < limit;  i++  )
				{
					//ExternalLink
//					String type = XMLHelper.getStringValueOf( nodes.get(i), "@name" );
					String title = XMLHelper.getStringValueOf( nodes.get(i), "dynamic-content/text()");
					String linkClass = "";
					String linkClassElement = XMLHelper.getStringValueOf( nodes.get(i), "dynamic-element[@name='Class']/dynamic-content/text()");
					if(   linkClassElement != null )
						linkClass  = linkClassElement;
					
					// Atributo Rel
					String linkRel = XMLHelper.getStringValueOf( nodes.get(i), "dynamic-element[@name='Rel']/dynamic-content/text()");
					
					String link = XMLHelper.getStringValueOf( nodes.get(i), "dynamic-element[@name='Link']/dynamic-content/text()");
					if(  !title.equals(StringPool.BLANK) &&  !link.equals(StringPool.BLANK)   )
					{
						RelatedLinkData relatedLink = createRelatedLink( "ExternalLink", title, linkClass, linkRel, "", link);
						relatedLinks.add( relatedLink );
					}
				}
			}
		} 
		catch (Exception e) 
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return relatedLinks;	
	}
	
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Obtiene los articulos relacionados con otro por categoria
	 */
	public static List<Object> getMetadataRelatedContent(long globalGroupId, String contentId, String structureId, long scopeGroupId, int start, int end) {											
		
		JournalArticle article;
		try {
			article = JournalArticleLocalServiceUtil.getArticle(globalGroupId, contentId);
			
		} catch (Exception e) {
			article = null;
		} 
		
		List<Object> articles = new ArrayList<Object>();
		
		try {	
			
			List<AssetCategory> categories = CategorizeLocalServiceUtil.getItemCategories(article.getCompanyId(), article.getGroupId(), "com.liferay.portlet.journal.model.JournalArticle", article.getResourcePrimKey());
			
			long[] categoryIds = new long[categories.size()];
			for (int i = 0; i < categories.size(); i++) {
				categoryIds[i] = categories.get(i).getCategoryId();				
			}
			if (categoryIds.length > 0) {
				articles = PageContentLocalServiceUtil.
								findPageContent(scopeGroupId, categoryIds, structureId, 
								IterKeys.ORDER_STARTDATE, IterKeys.ORDER_DESC, start, end+1, new Date());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return articles;
	}
	
	/*
	 * 
	 */
	public static String getHeadLine(String content, Locale locale) {
		String headline = "";
		
		Document doc = null;

		try {
			doc = SAXReaderUtil.read(content);

			Element root = doc.getRootElement();

			for (Element el : root.elements()) {
				String elName = el.attributeValue("name", StringPool.BLANK);

				if (elName.equals("Headline")) {
					Map<Locale, String> headlines = new HashMap<Locale, String>();
					headlines = getElementText(el);
					
					headline = headlines.get(locale);
					break;
				}
			}
		} catch (Exception e) {
			
		}
		
		return headline;
	
	}
	
	/*
	 * 
	 */
	public static List<KeyValuePair> getRelatedInternalLinks(JournalArticle article) 
	{
		List<KeyValuePair> relatedLinks = new ArrayList<KeyValuePair>();
		
		Document doc = null;

		try 
		{
			doc = SAXReaderUtil.read(article.getContent());

			Element root = doc.getRootElement();

			for (Element el : root.elements()) 
			{
				String elName = el.attributeValue("name", StringPool.BLANK);

				if (elName.equals("InternalLink")) 
				{
					Element eleLink = el.element("dynamic-element");
					Element elementContentId 	= eleLink.element("dynamic-content");
					Element elementLinkName		= el.element("dynamic-content");
					
					// Se descarta el vacío y él como link interno a si mismo 
					if (!elementContentId.getText().equals(StringPool.BLANK) && 
						!elementContentId.getText().equals(article.getArticleId()))
					{
						KeyValuePair pair = new KeyValuePair();
						pair.setKey(elementContentId.getText());
						pair.setValue(elementLinkName.getText());
						relatedLinks.add(pair);	
					}
				}
			}
		} 
		catch (Exception e)	{}
		
		return relatedLinks;	
	}

	/*
	 * 
	 */
	public static List<Object> getRelatedExternalLinks(String content) {
		List<Object> relatedLinks = new ArrayList<Object>();
		
		Document doc = null;

		try {
			doc = SAXReaderUtil.read(content);

			Element root = doc.getRootElement();

			for (Element el : root.elements()) {
				String elName = el.attributeValue("name", StringPool.BLANK);

				if (elName.equals("ExternalLink")) {
					
					Element extLinkTitleEle = el.element("dynamic-content");
					
					String title = extLinkTitleEle.getText();					
					
					Element eleAnswer = el.element("dynamic-element");
					Element element = eleAnswer.element("dynamic-content");
					
					String link = element.getText();					
					
					if (!title.equals("") && !link.equals("")) {
						relatedLinks.add(new Object[] {extLinkTitleEle.getText(), element.getText()});
					}
				}
			}
		} catch (Exception e) {
			
		}
		
		return relatedLinks;	
	}

	private static Map<Locale, String> getElementText(Element el) {
		Map<Locale, String> texts = new HashMap<Locale, String>();
	
		List<Element> contents = el.elements("dynamic-content");
		Locale locale = LocaleUtil.getDefault();
	
		for (Element ele : contents) {
			texts.put(locale, ele.getText());
		}
	
		return texts;
	}
	
	/**
	 * 
	 * Method to obtain the value(s) of a specific field un a WebContent
	 * 
	 * @param content JournalArticle element
	 * @param field Path of the field we that want to recover, 
	 * @param language Localized version of the field  we are looking for
	 * @return Returns a List with the values that meet the conditions
	 */
	public static List<String> getWebContentField(JournalArticle article, String field, String language)
	{
		Document document = null;
		try 
		{
			document = SAXReaderUtil.read(article.getContent());
		} 
		catch (DocumentException e) 
		{
			e.printStackTrace();
			_log.error("Unable to read the WebContent content");
		}

		return JournalArticleTools.getWebContentField(document, field, language);
	}
	
	/**
	 * 
	 * Method to obtain the value(s) of a specific field un a WebContent
	 * 
	 * @param groupId Id of the group we are working with
	 * @param articleId Id of the article where we have to search
	 * @param field Path of the field we that want to recover, 
	 * @param language Localized version of the field  we are looking for
	 * @return Returns a List with the values that meet the conditions
	 */
	public static List<String> getWebContentField(long groupId, String articleId, String field, String language){
		JournalArticle article;
		try {
			article = JournalArticleLocalServiceUtil.getArticle(groupId, articleId);
			return getWebContentField(article, field, language);
		} catch (Exception e) {
			e.printStackTrace();
			_log.error("The WebContent doesn't exists");
			return null;
		} 
	}
	
	
	/**
	 * 
	 * Method to obtain the internal links of a WebContent
	 * 
	 * @param article JournalArticle object
	 * @return A list of the articleIds of the WebContents linked
	 */
	public static List<String> getInternalLinks(JournalArticle article) {
		if (article != null && article.getContent() != null){
			return getInternalLinks(article.getContent());
		} else {
			return null;
		}
	}
	
	/**
	 * 
	 * Method to obtain the internal links of a WebContent
	 * 
	 * @param groupId Id of the group
	 * @param articleId Id of the WebContent
	 * @return A list of the articleIds of the WebContents linked
	 */
	public static List<String> getInternalLinks(long groupId, String articleId) {
		JournalArticle article = null;
		try {
			article = JournalArticleLocalServiceUtil.getArticle(groupId, articleId);
			return getInternalLinks(article);
		} catch (Exception e) {
			_log.error("Unable to find the WebContent");
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 
	 * Method to obtain the internal links of a WebContent
	 * 
	 * @param content Content of the WebContent to explore
	 * @return A list of the articleIds of the WebContents linked
	 */
	public static List<String> getInternalLinks(String content) {
		List<String> relatedLinks = new ArrayList<String>();
		
		Document doc = null;

		try {
			doc = SAXReaderUtil.read(content);

			Element root = doc.getRootElement();

			for (Element el : root.elements()) {
				String elName = el.attributeValue("name", StringPool.BLANK);

				if (elName.equals("InternalLink")) {
					
					Element eleAnswer = el.element("dynamic-element");
					Element element = eleAnswer.element("dynamic-content");
						
					relatedLinks.add(element.getText());				
				}
			}
		} catch (Exception e) {
			_log.error("Unable to find Internal Links.");
		}
		return relatedLinks;	
	}
	
	
	/**
	 * 
	 * Method to obtain the external links of a WebContent
	 * 
	 * @param article JournalArticle object
	 * @return A list of objects that give us the title and the url for the external links
	 */
	public static List<String[]> getExternalLinks(JournalArticle article) {
		if (article != null && article.getContent() != null){
			return getExternalLinks(article.getContent());
		} else {
			return null;
		}
	}
	
	/**
	 * 
	 * Method to obtain the external links of a WebContent
	 * 
	 * @param groupId Id of the group
	 * @param articleId Id of the WebContent
	 * @return A list of objects that give us the title and the url for the external links
	 */
	public static List<String[]> getExternalLinks(long groupId, String articleId) {
		JournalArticle article = null;
		try {
			article = JournalArticleLocalServiceUtil.getArticle(groupId, articleId);
			return getExternalLinks(article);
		} catch (Exception e) {
			_log.error("Unable to find the WebContent Id " + articleId );
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 
	 * Method to obtain the external links of a WebContent
	 * 
	 * @param content Content of the WebContent to explore
	 * @return A list of objects that give us the title and the url for the external links
	 */
	public static List<String[]> getExternalLinks(String content) {
		List<String[]> relatedLinks = new ArrayList<String[]>();
		
		Document doc = null;

		try {
			doc = SAXReaderUtil.read(content);

			Element root = doc.getRootElement();

			for (Element el : root.elements()) {
				String elName = el.attributeValue("name", StringPool.BLANK);

				if (elName.equals("ExternalLink")) {
					
					Element extLinkTitleEle = el.element("dynamic-content");
					
					String title = extLinkTitleEle.getText();					
					
					Element eleAnswer = el.element("dynamic-element");
					Element element = eleAnswer.element("dynamic-content");
					
					String link = element.getText();					
					
					if (!title.equals("") && !link.equals("")) {
						relatedLinks.add(new String[] {extLinkTitleEle.getText(), element.getText()});
					}
				}
			}
		} catch (Exception e) {
			_log.error("Unable to find External Links.");
		}
		return relatedLinks;	
	}
	
	/**
	 * 
	 * Method to obtain WebContents related with the given one
	 * 
	 * @param article JournalArticle object
	 * @param maxResults Max number of results to obtain
	 * @return A list of id's of the related WebContents obtained 
	 */
	public static List<Long> getWebContentRelatedElements(JournalArticle article, int maxResults) {
		List<Long> resultado = new ArrayList<Long>();
		//obtenemos las categorias del articulo
		List<AssetCategory> categories = new ArrayList<AssetCategory>();
		try {
			long classNameId = PortalUtil.getClassNameId(JournalArticle.class.getName());
			categories = AssetCategoryLocalServiceUtil.getCategories(classNameId, article.getResourcePrimKey());
			List<Long> categoriesIdsAux = new ArrayList<Long>();
			for (AssetCategory categorie : categories) {
				categoriesIdsAux.add(categorie.getCategoryId());
			}
			long[] categoriesIds = new long[categoriesIdsAux.size()];
			for (int i = 0; i < categoriesIds.length; i++) {
				categoriesIds[i] = categoriesIdsAux.get(i);
			}
			
			//buscamos los contenidos webs con esos tags (alguno o todos los tags)
			if (categories != null && categories.size() > 0) {	
				
				AssetEntryQuery query = new AssetEntryQuery();
	
				long[] groupIds = { article.getGroupId() };
	
				query.setGroupIds(groupIds);
				query.setAllCategoryIds(categoriesIds);
				query.setOrderByCol1("publishDate");
				query.setStart(0);
				query.setEnd(maxResults);
				
				List<AssetEntry> results = AssetEntryLocalServiceUtil
						.getEntries(query);
				
				for (AssetEntry entry : results) {
					JournalArticle ja = JournalArticleLocalServiceUtil.getLatestArticle(entry.getClassPK());
					if (article != null && !ja.getArticleId().equals(article.getArticleId())) {
						resultado.add(ja.getId());
					}
				}			
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultado;
	}
	
	/**
	 * 
	 * Method to obtain WebContents related with the given one
	 * 
	 * @param contentId Id of the WebContent
	 * @param groupId Id of the group where the WebContent is
	 * @param maxResults Max number of results to obtain
	 * @return A list of id's of the related WebContents obtained 
	 */
	public static List<Long> getWebContentRelatedElements(String contentId, long groupId, int maxResults) {
		try {
			JournalArticle article = JournalArticleLocalServiceUtil.getArticle(groupId, contentId);
			return getWebContentRelatedElements(article, maxResults);
			
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * 
	 * Method to obtain the Default Model of a WebContent
	 * 
	 * @param groupId Id of the group where the WebContent belongs to
	 * @param contentId Id of the WebContent
	 * @return A long value with the id of the Default Model applied to the WebContent
	 */
	public static long getWebContentDefaultModel(long groupId, String contentId) {
		long model = 0l;
		try {
			model = PageContentLocalServiceUtil.getFirstPageContent(groupId, contentId).getArticleModelId();
		} catch (SystemException e) {
		}
		return model;
	}
	

	/**
	 * Method to obtain the date from the Friendly URL date param
	 * Format options:
	 * yyyy-MM-dd
	 * @param dateParam URL date param
	 * @param defaultValue default return value
	 * @return Date obtained by parsing Friendly URL date param
	 */
	public static Date getDateFromURL(RenderRequest renderRequest, ThemeDisplay themeDisplay, Date defaultValue){

		Date resultDate = defaultValue;
		
		if(resultDate == null)
		{
			Calendar paramCalendar = Calendar.getInstance();
			resultDate = paramCalendar.getTime();
		}
		
		if (themeDisplay.getURLCurrent().contains("/date/")){
			String dateParam = renderRequest.getParameter(WebKeys.URL_PARAM_CONTENT_DATE);
			try{
				String dateFormat = IterKeys.URL_PARAM_DATE_FORMAT;
				if (dateParam.length() >= IterKeys.URL_PARAM_DATE_FORMAT_EXT.length()){
					dateParam = dateParam.substring(0, IterKeys.URL_PARAM_DATE_FORMAT_EXT.length());
				}
					
				if (dateParam.length() ==  IterKeys.URL_PARAM_DATE_FORMAT_EXT.length()){
					dateFormat = IterKeys.URL_PARAM_DATE_FORMAT_EXT;
				}
				SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
				Date paramDate = sdf.parse(dateParam);
				
				Calendar paramCalendar = Calendar.getInstance();
				paramCalendar.setTime(paramDate);
				
				if(dateFormat.equals(IterKeys.URL_PARAM_DATE_FORMAT))
				{
					Calendar currentCalendar = Calendar.getInstance();
					paramCalendar.set(Calendar.HOUR_OF_DAY, currentCalendar.get(Calendar.HOUR_OF_DAY));
					paramCalendar.set(Calendar.MINUTE, currentCalendar.get(Calendar.MINUTE));
				}
				
				resultDate = paramCalendar.getTime();
			}
			catch (Exception err)
			{
				_log.debug(err);
			}	
		}
		return resultDate;
	}
	
	/**
	 * Method to obtain the string param from a Date
	 * yyyy-MM-dd
	 * @param date The date to be parsed into the String
	 * @return Date obtained by parsing Friendly URL date param
	 */
	public static String getURLParamFromDate(Date date){
			SimpleDateFormat sdf = new SimpleDateFormat(IterKeys.URL_PARAM_DATE_FORMAT);
			return sdf.format(date);
	}
		
	public static Date getDateFromURLParam(String dateParam, Date defaultValue){

		Date resultDate = defaultValue;
		
		if(resultDate == null)
		{
			Calendar paramCalendar = Calendar.getInstance();
			resultDate = paramCalendar.getTime();
		}
		
		try{
			String dateFormat = IterKeys.URL_PARAM_DATE_FORMAT;
			if (dateParam.length() >= IterKeys.URL_PARAM_DATE_FORMAT_EXT.length()){
				dateParam = dateParam.substring(0, IterKeys.URL_PARAM_DATE_FORMAT_EXT.length());
			}
				
			if (dateParam.length() ==  IterKeys.URL_PARAM_DATE_FORMAT_EXT.length()){
				dateFormat = IterKeys.URL_PARAM_DATE_FORMAT_EXT;
			}
			
			SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
			Date paramDate = sdf.parse(dateParam);
			
			Calendar paramCalendar = Calendar.getInstance();
			paramCalendar.setTime(paramDate);
			
			if(dateFormat.equals(IterKeys.URL_PARAM_DATE_FORMAT))
			{
				Calendar currentCalendar = Calendar.getInstance();
				paramCalendar.set(Calendar.HOUR_OF_DAY, currentCalendar.get(Calendar.HOUR_OF_DAY));
				paramCalendar.set(Calendar.MINUTE, currentCalendar.get(Calendar.MINUTE));
			}
			
			resultDate = paramCalendar.getTime();
		}catch (Exception err){
			_log.debug(err);
		}
		return resultDate;
	}
	
	public static long [] getCategoryIdsFromURLParam(String categoriesParam, long[] defaultValue)
	{
		long [] result = defaultValue;
		
		if ( categoriesParam!=null && !categoriesParam.equals("") )
		{
			ArrayList<Long> categoryIds = new ArrayList<Long>();
			String[] tagTokens = categoriesParam.split(CategoriesUtil.URL_PARAM_TAGS_SEPARATOR);
			
			for (String token : tagTokens)
			{
				try
				{
					if (Long.valueOf(token) != 0)
						categoryIds.add(Long.valueOf(token));
				}
				catch (Exception err)
				{
					_log.error(err);
				}
			}
			
			result = new long[categoryIds.size()];
			
			for(int i = 0; i < categoryIds.size(); i++)
			{
				result[i] = categoryIds.get(i).longValue();
			}
		}
		
		return result;
	}
	
	public static String getURLParamFromCategories(long[] categoryIds){
		
		StringBuilder sb = new StringBuilder();
		if(categoryIds != null)
		{
			boolean first = true;
			for (long categoryId : categoryIds)
			{
				sb.append(first?"":CategoriesUtil.URL_PARAM_TAGS_SEPARATOR);
				sb.append(String.valueOf(categoryId));
				first = false;
			}
		}
		return sb.toString();
	}
	
	public static List<String[]> getFilterArticles(long groupId, String contentId, List<String> structures, 
												   int startIndex, int numElements, Date validityDate, boolean showNonActiveContents, 
												   String[] orderFields, int typeOrder, long[] categoriesId, 
												   List<KeyValuePair> internalLinks, String[] qualificationId,
												   String[] layoutIds, boolean excludeContentId)
	{
		return getFilterArticles(groupId, contentId, structures, startIndex, numElements, validityDate, showNonActiveContents, 
				   				 orderFields, typeOrder, categoriesId, internalLinks, qualificationId,
				   				 layoutIds, excludeContentId, "", null);
	}
	
	public static List<String[]> getFilterArticles(long groupId, String contentId, List<String> structures, 
			   int startIndex, int numElements, Date validityDate, boolean showNonActiveContents, 
			   String[] orderFields, int typeOrder, long[] categoriesId, 
			   List<KeyValuePair> internalLinks, String[] qualificationId,
			   String[] layoutIds, boolean excludeContentId, String extraLimit, int[] layoutLimits)
	{
		return getFilterArticles(groupId, contentId, structures, startIndex, numElements, 
								 validityDate, showNonActiveContents, orderFields, typeOrder, 
								 categoriesId, internalLinks, qualificationId, layoutIds, 
								 excludeContentId, extraLimit, null, null, layoutLimits);
	}
	
	public static List<String[]> getFilterArticles(long groupId, String contentId, List<String> structures, 
			   int startIndex, int numElements, Date validityDate, boolean showNonActiveContents, 
			   String[] orderFields, int typeOrder, long[] categoriesId, 
			   List<KeyValuePair> internalLinks, String[] qualificationId,
			   String[] layoutIds, boolean excludeContentId, String extraLimit,
			   Date creationDate, int[] layoutLimits)
	{
		return getFilterArticles(groupId, contentId, structures, startIndex, numElements, 
								 validityDate, showNonActiveContents, orderFields, typeOrder, 
								 categoriesId, internalLinks, qualificationId, layoutIds, 
								 excludeContentId, extraLimit, creationDate, null, layoutLimits);
	}
	public static List<String[]> getFilterArticles(long groupId, String contentId, List<String> structures, int startIndex, 
			   int numElements, Date validityDate, boolean showNonActiveContents, 
			   String[] orderFields, int typeOrder, long[] categoriesId, 
			   List<KeyValuePair> internalLinks, String[] qualificationId,
			   String[] layoutIds, boolean excludeContentId, String extraLimit, 
			   Date creationDate, Date modifiedDate, int[] layoutLimits)
			   {
		return getFilterArticles(groupId, contentId, structures, startIndex, numElements, 
				 validityDate, showNonActiveContents, orderFields, typeOrder, 
				 categoriesId, internalLinks, qualificationId, layoutIds, 
				 excludeContentId, extraLimit, creationDate, modifiedDate, null, null, layoutLimits );
	}
	
	/*  getFilterArticles con 19 parámetros sólo se llamará en el Teaser con filtro por secciones o metadatos (desde teaser_filter.jsp o teaser_page.jsp ),
		indicando los parámetros categoryFilter o layoutFilter*/ 
	public static List<String[]> getFilterArticles(long groupId, String contentId, List<String> structures, int startIndex, 
			   int numElements, Date validityDate, boolean showNonActiveContents, 
			   String[] orderFields, int typeOrder, long[] categoriesId, 
			   List<KeyValuePair> internalLinks, String[] qualificationId,
			   String[] layoutIds, boolean excludeContentId, String extraLimit, 
			   Date creationDate, Date modifiedDate, long[] categoryFilter, String[] layoutFilter, int[] layoutLimits )
	{
		return getFilterArticles(groupId, contentId, structures, startIndex, numElements, 
				 validityDate, showNonActiveContents, orderFields, typeOrder, 
				 categoriesId, internalLinks, qualificationId, layoutIds, 
				 excludeContentId, extraLimit, creationDate, modifiedDate, categoryFilter, layoutFilter, null, layoutLimits );
	}
	
	/*  getFilterArticles con 20 parámetros sólo se llamará en el Teaser con filtro por fecha (desde teaser_filter.jsp o teaser_page.jsp ),
	 	modifiedDateFilter será la fecha seleccionada en el datepicker y se filtrará por los artículos modificados en esa fecha*/
	
	// ATENCIÓN: LOS TEASER CONFIGURADOS CON LÍMITES POR SECCIONES SON SENSIBLES A LA CONSULTA SQL, YA QUE EL
	// PROCESO LA ANALIZA Y MODIFICA. SI SE CAMBIA LA CONSULTA, COMPROBAR QUE LOS LÍMITES POR SECCIÓN NO SE VEAN AFECTADOS.
	// EL METODO A COMPROBAR ES: splitQueryBySectionLimits()
	public static List<String[]> getFilterArticles(long groupId, String contentId, List<String> structures, int startIndex, 
			   									   int numElements, Date validityDate, boolean showNonActiveContents, 
			   									   String[] orderFields, int typeOrder, long[] categoriesId, 
			   									   List<KeyValuePair> internalLinks, String[] qualificationId,
			   									   String[] layoutIds, boolean excludeContentId, String extraLimit, 
			   									   Date creationDate, Date modifiedDate, long[] categoryFilter, String[] layoutFilter, Date modifiedDateFilter, int[] layoutLimits )
{
		List<String[]> results = new ArrayList<String[]>();
		
		try
		{
			List<Object> resultsQuery = SolrFilterArticles.search(
											groupId, contentId, structures, startIndex, 
											numElements, validityDate, showNonActiveContents, 
											orderFields, typeOrder, categoriesId, 
											internalLinks, qualificationId,
											layoutIds, excludeContentId, extraLimit, 
											creationDate, modifiedDate, categoryFilter, layoutFilter, 
											modifiedDateFilter, layoutLimits);
			
			boolean isSharedOrder = false;
			if ( resultsQuery == null )
			{
				StringBuffer sqlSelectQuery 	= new StringBuffer();	
				StringBuffer sqlQuery 			= new StringBuffer();
				StringBuffer sqlOrderQuery 		= new StringBuffer();	
				boolean checkInternalLinks  	= (internalLinks != null && internalLinks.size() > 0);
				boolean pageContentAsSubselect 	= includePageContentAsSubselect(orderFields);
				
				sqlSelectQuery.append("SELECT DISTINCT j.articleId, j.title ");
				
				// Si orderFields contiene al criterio de ordenación IterKeys.ORDER_MLN,
				// los artículos se ordenarán según orden indicado en MLN y por tanto no hay que añadir sentencias 'ORDER BY' a la query.
				// Actualmente ese criterio de ordenación sólo está en RelatedViewer y el interfaz ya se encarga de que en el caso que se seleccione no se pase por aquí...
				if (orderFields != null && orderFields.length > 0 && !ArrayUtil.contains( orderFields, Integer.toString(IterKeys.ORDER_MLN) )  )
				{
					for(int k = 0; k < orderFields.length; k++)
					{
						String currentOrderSelect = getSQLOrderSelect(Integer.parseInt(orderFields[k]));
						if(currentOrderSelect != null && !currentOrderSelect.isEmpty())
						{
							sqlSelectQuery.append(", " + currentOrderSelect);
						}
					}
				}
								  
				sqlQuery.append("\nFROM JournalArticle j ");
				
				// Se añade el índice correcto
				sqlQuery.append( getIndexToUse(categoriesId, orderFields) );
	
				String QUERY_INDEX_PAGECONTENT_VIGHASTA = String.format("  USE INDEX (%s) ", HotConfigUtil.getKey("teaser.indexes", "XYZ_ITR_IX_NEWS_PAGECONTENT_VIGHASTA_ZYX", _reloadTeaserIndex.isFatalEnabled())); 
				
				if (!pageContentAsSubselect)
					sqlQuery.append("\nINNER JOIN News_PageContent n ").append(QUERY_INDEX_PAGECONTENT_VIGHASTA).append(" ON (n.contentId=j.articleId AND n.online_=TRUE) \n");
				
				List<String> statisticOperations = getStatisticsOperation(orderFields);
				isSharedOrder = statisticOperations.contains(Integer.toString(IterKeys.OPERATION_SHARED));
				
				if(statisticOperations.size() > 0)  
					sqlQuery.append("INNER JOIN  iter_statistics_journalarticle ON iter_statistics_journalarticle.contentId = j.articleId \n");
				
				if (categoriesId != null && categoriesId.length > 0)
				{
					sqlQuery.append("LEFT JOIN AssetEntry e ON e.classPK=j.resourcePrimKey \n" 				+
									"LEFT JOIN AssetEntries_AssetCategories a ON a.entryId=e.entryId \n" 	+
									"LEFT JOIN AssetCategory c ON c.categoryId=a.categoryId \n");
				}
	
				sqlQuery.append("\n\tWHERE ").append( PAGECONTENT_TOKEN );
						
				StringBuilder sqlPageContent = new StringBuilder( String.format(!pageContentAsSubselect ? "n.groupId=%d  \n" : PAGECONTENT_SUBSELECT.toString(), groupId) ); 
				
				if(statisticOperations.size() > 0)
				{
					String inClause = statisticOperations.contains(Integer.toString(IterKeys.OPERATION_DISQUS)) ?
							          "(" + Integer.toString(IterKeys.OPERATION_DISQUS) + ")" :
									  "(" + IterKeys.OPERATION_FB + ", " + IterKeys.OPERATION_TW + ", " + IterKeys.OPERATION_GP + ")";
					sqlQuery.append("\n\t AND iter_statistics_journalarticle.statisticOperation IN  ").append(inClause).append(" ");
				}
					 		
	
				if (creationDate != null)
				{
					SimpleDateFormat format = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_00);
					String creationDateString = format.format(creationDate);
					sqlQuery.append("\n\tAND j.reviewDate > '" + creationDateString + "' ");
				}
				
				// ITER-792 No repetir artículos en los distintos teaser viewers de la página.
				StringBuilder  unrepeatableArticles = UnrepeatableArticlesMgr.get();
				if (unrepeatableArticles != null && unrepeatableArticles.length() > 0)
				{
					sqlQuery.append("\n\tAND j.articleId NOT IN (").append(unrepeatableArticles).append(") ");
				}
				
				if( modifiedDateFilter != null )
				{
					SimpleDateFormat format = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_00);
					String modifiedDateFilterString = format.format(modifiedDateFilter);
					String[] dateSplit = modifiedDateFilterString.split("-", 3);
					String dayWidthTime = dateSplit[2];
					String[] daySplit = dayWidthTime.split(" ");
					
					String timeLimit = "23:59:59";
					String modifiedDateFilterLimitString = dateSplit[0]+ "-" + dateSplit[1]+ "-" + daySplit[0] + " " + timeLimit;
					
					sqlQuery.append("\n\tAND j.modifiedDate >= '" + modifiedDateFilterString + "' AND j.modifiedDate <= '" + modifiedDateFilterLimitString + "' ");
				}
				
				if (modifiedDate != null)
				{
					SimpleDateFormat format = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_00);
					String modifiedDateString = format.format(modifiedDate);
					sqlQuery.append("\n\tAND j.modifiedDate >= '" + modifiedDateString + "' ");
				}
				
				if (!excludeContentId)
				{
					sqlQuery.append("\n\tAND j.articleId <> '" + contentId + "' ");
				}
				
				if (layoutIds != null && layoutIds.length > 0)
				{
					StringBuilder sqlAux = new StringBuilder("\n\tAND n.layoutId IN");
					
					for(int i = 0; i < layoutIds.length; i++)
					{
						String currentLayout = layoutIds[i];
						if (i == 0)
						{
							sqlAux.append("('" + currentLayout + "'");
						}				
						if (i == layoutIds.length - 1)
						{
							if (layoutIds.length > 1)
							{
								sqlAux.append(", '" + currentLayout + "') ");
							}
							else
							{
								sqlAux.append(") ");
							}
						}
						
						if(i > 0 && i < layoutIds.length - 1)
						{
							sqlAux.append(", '" + currentLayout + "'");
						}
					}
					
					addPageContentCondition(sqlPageContent, sqlAux.toString());
				}
				if( layoutFilter != null && layoutFilter.length > 0)
				{
					StringBuilder sqlAux = new StringBuilder("\n\tAND n.layoutId IN");
					
					for(int i = 0; i < layoutFilter.length; i++)
					{
						String currentLayout = layoutFilter[i];
						if (i == 0)
						{
							sqlAux.append("('" + currentLayout + "'");
						}				
						if (i == layoutFilter.length - 1)
						{
							if (layoutFilter.length > 1)
							{
								sqlAux.append(", '" + currentLayout + "') ");
							}
							else
							{
								sqlAux.append(") ");
							}
						}
						
						if(i > 0 && i < layoutFilter.length - 1)
						{
							sqlAux.append(", '" + currentLayout + "'");
						}
					}
					
					addPageContentCondition(sqlPageContent, sqlAux.toString());
				}
				
				if (qualificationId != null && qualificationId.length > 0 && !qualificationId[0].isEmpty())
				{
					StringBuilder sqlAux = new StringBuilder("\n\tAND n.qualificationId IN");
					
					for(int i = 0; i < qualificationId.length; i++)
					{
						String currentQualification = qualificationId[i];
						if (i == 0)
						{
							sqlAux.append("('" + currentQualification + "'");
						}				
						if (i == qualificationId.length - 1)
						{
							if (qualificationId.length > 1)
							{
								sqlAux.append(", '" + currentQualification + "') ");
							}
							else
							{
								sqlAux.append(") ");
							}
						}
						
						if(i > 0 && i < qualificationId.length - 1)
						{
							sqlAux.append(", '" + currentQualification + "'");
						}
					}
					
					addPageContentCondition(sqlPageContent, sqlAux.toString());
				}
				if (checkInternalLinks)
				{
					sqlQuery.append("\n\tAND j.articleId IN");
					
					for(int i = 0; i < internalLinks.size(); i++)
					{
						KeyValuePair currentLink = internalLinks.get(i);
						if (i == 0)
						{
							sqlQuery.append("('" + currentLink.getKey() + "'");
						}				
						if (i == internalLinks.size() - 1)
						{
							if (internalLinks.size() > 1)
							{
								sqlQuery.append(", '" + currentLink.getKey() + "') ");
							}
							else
							{
								sqlQuery.append(") ");
							}
						}
						if (i > 0 && i < internalLinks.size() - 1)
						{
							sqlQuery.append(", '" + currentLink.getKey() + "'");
						}
					}
				}
					
				if ( categoriesId != null && categoriesId.length > 0 && categoriesId[0] != 0 )
				{
					// Se añaden las categorías
					List<String> categoryValues = new ArrayList<String>();
					
					// Se tienen en cuenta o no los hijos de las categorías
					// ITER-915 La versión AMP de algunos artículos tarda mucho en generarse
					// http://jira.protecmedia.com:8080/browse/ITER-915?focusedCommentId=35838&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-35838
					if ( GetterUtil.getBoolean( IterRequest.getAttribute(WebKeys.FILTERARTICLES_CATEGORIES_USE_ONLY_PARENTS), false ) )
					{
						for (long cat : categoriesId)
						{
							categoryValues.add( String.valueOf(cat) );
						}
					}
					else
					{
						List<Object> leftRightIds = CategoriesUtil.getLeftRightIds(categoriesId);
						if(leftRightIds == null || leftRightIds.size() == 0)
						{
							leftRightIds = new ArrayList<Object>();
							String leftRightIdsAux[] = {"0","0"};
							leftRightIds.add(leftRightIdsAux);
						}

						for(int i = 0; i < leftRightIds.size(); i++)
						{
							Object[] currentDataArray = (Object[])leftRightIds.get(i);
							categoryValues.addAll(  getCategoryList(currentDataArray[0].toString(), currentDataArray[1].toString()) );
						}
					}
					
					if (categoryValues.size() > 0)
					{
						sqlQuery.append( String.format("\n\tAND c.categoryId IN (%s) ", StringUtil.merge( categoryValues.toArray(new String[categoryValues.size()]), "," ) ) );
					}
					// END OF: Se añaden las categorías
	
					sqlQuery.append("\n\tAND e.classNameId=" + ClassNameLocalServiceUtil.getClassNameId(IterKeys.CLASSNAME_JOURNALARTICLE) + " ");
				}
				if ( categoryFilter != null && 
						categoryFilter.length > 0 && 
						categoryFilter[0] != 0 )
					{
						List<Object> leftRightIds = CategoriesUtil.getLeftRightIds( categoryFilter );
						if(leftRightIds == null || leftRightIds.size() == 0)
						{
							leftRightIds = new ArrayList<Object>();
							String leftRightIdsAux[] = {"0","0"};
							leftRightIds.add(leftRightIdsAux);
						}
						
						// Se añaden las categorías
						List<String> categoryValues = new ArrayList<String>();
						for(int i = 0; i < leftRightIds.size(); i++)
						{
							Object[] currentDataArray = (Object[])leftRightIds.get(i);
							categoryValues.addAll(  getCategoryList(currentDataArray[0].toString(), currentDataArray[1].toString()) );
						}
						if (categoryValues.size() > 0)
						{
							sqlQuery.insert(sqlQuery.indexOf(StringPool.WHERE), SQL_USER_CATEGORY_FILTER_JOINS);
							sqlQuery.append( String.format("\n\tAND c2.categoryId IN (%s) ", StringUtil.merge( categoryValues.toArray(new String[categoryValues.size()]), "," ) ) );
						}
						// END OF: Se añaden las categorías
					}
		
				if (structures != null && structures.size() > 0)
				{
					sqlQuery.append("\n\tAND j.structureId IN");
					
					for(int j = 0; j < structures.size(); j++)
					{
						String currentStructure = structures.get(j);
						if (j == 0)
						{
							sqlQuery.append("('" + currentStructure + "'");
						}				
						if (j == structures.size() - 1)
						{
							if(structures.size() > 1)
							{
								sqlQuery.append(", '" + currentStructure + "') ");
							}
							else
							{
								sqlQuery.append(") ");
							}
						}
						
						if (j > 0 && j < structures.size() - 1)
						{
							sqlQuery.append(", '" + currentStructure + "'");
						}
					}
				}
				
				//				if (validityDate != null)
				//				{
				//					//2012-04-12 08:00:00
				//					SimpleDateFormat format = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_00);
				//					String dateString = format.format(validityDate);
				//					sqlQuery.append("AND '" + dateString + "' BETWEEN n.vigenciadesde AND n.vigenciahasta ");
				//				}
				
				// La comprobación de la tabla LIVE era para descartar elementos que estuviesen inconsistentes, que tras una importación se hubiese importado correctamente
				// el artículo pero NO alguna de sus dependencias. Se quita pq es para un caso puntual y penaliza al 99%, que no hay elementos inconsistentes.
				// sqlQuery += "AND NOT EXISTS (SELECT lv.localId FROM Xmlio_Live lv WHERE lv.localId = j.articleId and lv.classNameValue = '" + IterKeys.CLASSNAME_JOURNALARTICLE + "' and lv.status = '" + IterKeys.INTERRUPT + "') ";
				// Ya se hace un SELECT DISTINCT j.articleId, mejor que el GroupBy pq este no se comporta igual en PL/SQL que en MySQL
				// sqlQuery += "GROUP BY j.articleId ";
				
				if (statisticOperations.size() > 0)
				{
					sqlOrderQuery.append("\nGROUP BY j.articleId ");
				}
				
				// Si orderFields contiene al criterio de ordenación IterKeys.ORDER_MLN,
				// los artículos se ordenarán según orden indicado en MLN y por tanto no hay que añadir sentencias 'ORDER BY' a la query.
				// Actualmente ese criterio de ordenación sólo está en RelatedViewer y el interfaz ya se encarga de que en el caso que se seleccione no se pase por aquí...
				if (orderFields != null && orderFields.length > 0 && !ArrayUtil.contains( orderFields, Integer.toString(IterKeys.ORDER_MLN) ) )
				{
					for(int k = 0; k < orderFields.length; k++)
					{
						String having = getSQLHavingField(Integer.parseInt(orderFields[k]));
						if(Validator.isNotNull(having))
							sqlOrderQuery.append( having );
					}
					
					sqlOrderQuery.append("\n\tORDER BY ");
					for(int k = 0; k < orderFields.length; k++)
					{
						if( Integer.parseInt(orderFields[k]) ==  IterKeys.ORDER_QUALIFICATION   )
						{
							if( qualificationId != null && qualificationId.length > 1 )
							{
								sqlOrderQuery.append("FIELD(n.qualificationId,  ");
								
								for(int j = 0; j < qualificationId.length; j++)
								{
									String currentQualification = qualificationId[j];
									if (j == 0)
									{
										sqlOrderQuery.append("'" + currentQualification + "'");
									}
									if (j == qualificationId.length - 1)
									{
										if (qualificationId.length > 1)
										{
											sqlOrderQuery.append(", '" + currentQualification + "') ");
										}
										else
										{
											sqlOrderQuery.append(") ");
										}
									}
									if(j > 0 && j < qualificationId.length - 1)
									{
										sqlOrderQuery.append(", '" + currentQualification + "'");
									}
								}
							}	
						}
						else
						{
							sqlOrderQuery.append( getSQLOrderField(Integer.parseInt(orderFields[k]), typeOrder) );
						}
						
						
						/*Si el próximo criterio de ordenación es 'calificaciones' y hay una o menos de una calificación seleccionada, no se añadirá el orden por calificaciones en la query
							y por tanto en este punto no se debe poner una ','. Este caso no se debe dar porque se está controlando en el interfaz, pero se comprueba por si el interfaz falla */
	 					if (k < orderFields.length - 1 &&  !( Integer.parseInt(orderFields[k+1] ) ==  IterKeys.ORDER_QUALIFICATION && qualificationId.length <= 1 ) )
						{
							sqlOrderQuery.append(", ");
						}
					}
				}
				else
				{
					sqlOrderQuery.append("\n\tORDER BY j.createDate DESC");
				}
				sqlOrderQuery.append(", j.articleId DESC");
				sqlOrderQuery.append("\n LIMIT " + startIndex + "," + numElements);
							
				int numResults = getNumResults(startIndex, numElements, extraLimit);
				
				// Si se comprueban los links internos NO se realiza una validación por fecha
				// Se comprobarán las fechas de vigencias
				if (!checkInternalLinks)
					addPageContentCondition(sqlPageContent, IterKeys.QUERY_TOKEN_CHK_VALIDITYDATE);
	
				addPageContentConditions(sqlQuery, sqlPageContent);
				
				resultsQuery = executeFilterArticlesSQL(sqlSelectQuery, sqlQuery, sqlOrderQuery, 
						 !checkInternalLinks, validityDate, showNonActiveContents, 
						 numResults, extraLimit, layoutIds, layoutLimits);
			}
			
			HashMap<String, String> articlePlid = getPageContentPlid(resultsQuery, orderFields, layoutIds, groupId);
			
			for (Object currentResult:resultsQuery)
			{
				Object [] currentData = (Object[])currentResult;
				String[] toAddResult  = new String[currentData.length + 1];
				
				String articleId = null;
				String title = null;
				
				if (currentData[0] != null)
				{
					articleId = currentData[0].toString();
				}
				
				if (currentData[1] != null)
				{
					title = currentData[1].toString();
				}
			
				if (articleId != null && !articleId.isEmpty())
				{
					if(internalLinks != null && internalLinks.size() > 0)
					{
						KeyValuePair checkLink = getkeyInList(articleId, internalLinks);
						//articleId
						toAddResult[0] = checkLink.getKey();
						//linkTitle
						toAddResult[1] = checkLink.getValue();
					}
					else
					{
						toAddResult[0] = articleId;
						toAddResult[1] = title;
						if(Validator.isNotNull(articlePlid))
							toAddResult[2] = articlePlid.get(articleId);
					}
					
					if (isSharedOrder)
					{
						for(int i = 2; i < currentData.length && i < toAddResult.length;i++)
						{
							toAddResult[i] = currentData[i].toString();
						}
					}
					
					results.add(toAddResult);
				}
			}
		}
		catch(Exception e)
		{
			_log.error(e);
		}
		
		return results;
	}
	
	private static HashMap<String, String> getPageContentPlid(List<Object> resultsQuery, String[] orderFields, String[] layoutIds, long groupId)
	{
		HashMap<String, String> retVal = null;
		
		try
		{
			String query = new StringBuilder(" SELECT layout.plid, News_PageContent.contentId \n")
									.append( " FROM layout INNER JOIN News_PageContent ON (layout.uuid_=news_pagecontent.layoutId AND News_PageContent.online_=TRUE) \n")
									.append( " WHERE layout.uuid_ IN ('%s') AND News_PageContent.groupId=%s AND News_PageContent.contentId IN ('%s') ")
									.toString();
			
			if(_log.isDebugEnabled())
				_log.debug( String.format("IS_PREVIEW_ENVIRONMENT: %s  orderFields: %s  layoutIds: %s", 
								PropsValues.IS_PREVIEW_ENVIRONMENT, StringUtil.merge(orderFields, ", "), StringUtil.merge(layoutIds, ", ")) );
			
			if( PropsValues.IS_PREVIEW_ENVIRONMENT && 
				ArrayUtil.contains(orderFields, String.valueOf(IterKeys.ORDER_ORDEN)) &&
				Validator.isNotNull(layoutIds) &&
				Validator.isNotNull(resultsQuery) )
			{
				StringBuilder articleIdsList = new StringBuilder();
				ListIterator<Object> itr = resultsQuery.listIterator();
				while(itr.hasNext())
				{
					Object[] obj = (Object[])itr.next();
					articleIdsList.append( String.valueOf(obj[0]) );
					if(itr.hasNext())
						articleIdsList.append("', '");
				}
				
				String sql = String.format(query, StringUtil.merge(layoutIds, "', '"), groupId, articleIdsList.toString());
				
				if(_log.isDebugEnabled())
					_log.debug("Query: " + sql);
				
				List<Object> plidArticleList = PortalLocalServiceUtil.executeQueryAsList(sql);
				
				if(Validator.isNotNull(plidArticleList))
				{
					retVal = new HashMap<String, String>();
					for (Object plidArticleObj : plidArticleList)
					{
						Object[] plidArticle = (Object[])plidArticleObj;
						String articleId = String.valueOf(plidArticle[1]);
						if( Validator.isNull(retVal.get(articleId)) )
							retVal.put( articleId, String.valueOf(plidArticle[0]) );
						else
							retVal.put( articleId, StringPool.BLANK );
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.error(e);
		}
		
		return retVal;
	}

	/**
	 * Determina cuál es el mejor índice a forzar en la Query
	 * 
	 * @see http://jira.protecmedia.com:8080/browse/ITER-202
	 * http://jira.protecmedia.com:8080/browse/ITER-497
	 * 
	 * @param categoriesId
	 * @param orderFields
	 * 
	 * @return Índice a forzar, o vacío si es mejor no forzar uno concreto
	 */
	private static String getIndexToUse(long[] categoriesId, String[] orderFields)
	{
		String index = PropsValues.ITER_PORTLET_QUERY_INDEX_TO_USE;
		
		// http://jira.protecmedia.com:8080/browse/ITER-497
		// Se desactiva el forzado de índices en función de la ordenación
//		// Empíricamente se ha visto que si hay filtrado por categoría el uso del índice es muchísimo más lento
//		if (Validator.isNull(index) && ArrayUtils.isEmpty(categoriesId))
//		{
//			// Se ha comprobado también que si existen más campos de ordenación estos índices 
//			// pierden la eficiencia, y resulta mejor XYZ_ITR_IX_JOURNALARTICLE_ARTICLEID_ZYX
//			if (orderFields.length == 2 												&& 
//				orderFields[0].equals(String.valueOf(IterKeys.ORDER_DISPLAYDATE)) 		&&
//				orderFields[1].equals(String.valueOf(IterKeys.ORDER_MODIFICATIONDATE)))
//				index = " USE INDEX (XYZ_ITR_IX_JOURNALARTICLE_DISPLAYMODIFIEDDATE_ARTICLEID_ZYX) ";
//			
//			else if (orderFields.length == 2 											&& 
//				orderFields[0].equals(String.valueOf(IterKeys.ORDER_MODIFICATIONDATE)) 	&&
//				orderFields[1].equals(String.valueOf(IterKeys.ORDER_DISPLAYDATE)))
//				index = " USE INDEX (XYZ_ITR_IX_JOURNALARTICLE_MODIFIEDDISPLAYDATE_ARTICLEID_ZYX) ";
//			
//			else if (orderFields.length == 1 											&& 
//				orderFields[0].equals(String.valueOf(IterKeys.ORDER_DISPLAYDATE)))
//				index = " USE INDEX (XYZ_ITR_IX_JOURNALARTICLE_DISPLAYDATE_ARTICLEID_ZYX) ";
//				
//			else if (orderFields.length == 1 											&& 
//				orderFields[0].equals(String.valueOf(IterKeys.ORDER_MODIFICATIONDATE)))
//				index = " USE INDEX (XYZ_ITR_IX_JOURNALARTICLE_MODIFIEDDATE_ARTICLEID_ZYX) ";
//			
//			else
//				index = " USE INDEX (XYZ_ITR_IX_JOURNALARTICLE_ARTICLEID_ZYX) ";
//		}
		return index;
	}
	
	private static void addPageContentCondition(StringBuilder sqlPageContent, String condition)
	{
		sqlPageContent.insert(sqlPageContent.length()-3, condition);
	}
	
	private static void addPageContentConditions(StringBuffer sqlQuery, StringBuilder sqlPageContent)
	{
		int index = sqlQuery.indexOf(PAGECONTENT_TOKEN);
		sqlQuery.replace(index, index+PAGECONTENT_TOKEN.length(), sqlPageContent.toString()); 
	}

	
	/**
	 * Método que determina si la QUERY puede incluir al PageContent como una "subselect", o
	 * en su defecto como "INNER JOIN".
	 * 
	 * @see http://jira.protecmedia.com:8080/browse/ITER-202
	 * @param orderFields
	 * @return Verdadero si la query puede incluir al News_PageContent como una subselect.
	 */
	private static boolean includePageContentAsSubselect(String[] orderFields)
	{
		boolean asSubselect = 	PropsValues.ITER_PORTLET_QUERY_PAGECONTENT_AS_SUBSELECT							&&
								!PropsValues.IS_PREVIEW_ENVIRONMENT												&&
								!ArrayUtil.contains(orderFields, String.valueOf(IterKeys.ORDER_ORDEN))			&&
								!ArrayUtil.contains(orderFields, String.valueOf(IterKeys.ORDER_EXPIRATIONDATE))	&&
								!ArrayUtil.contains(orderFields, String.valueOf(IterKeys.ORDER_STARTDATE))		&&
								!ArrayUtil.contains(orderFields, String.valueOf(IterKeys.ORDER_QUALIFICATION));
		return asSubselect;
	}
	
	/**
	 * 
	 * @return
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 */
	private static List<String> getCategoryList(String leftCategoryId, String rightCategoryId) throws SecurityException, NoSuchMethodException
	{
		String select = String.format("SELECT categoryId from AssetCategory where %s <= leftcategoryId and rightcategoryId <= %s", leftCategoryId, rightCategoryId);
		
		List<Object> resultsQuery = PageContentLocalServiceUtil.sqlQuery(select);
		List<String> stringValues = new ArrayList<String>();
		
		for (int i = 0; i < resultsQuery.size(); i++)
		{
			String stringValue = resultsQuery.get(i).toString();
			
			if (Validator.isNotNull(stringValue))
				stringValues.add(stringValue);
		}
		
		return stringValues;
	}
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Determina el número máximo de elementos que debería retornar la query
	// Método que recibe el rango del límite interno de una Query y el rango de otro posible límite externo
	//
	// Se encapsula como función pq este valor podría cambiar. En el pasado la query se ejecutaba primero sin 
	// límites con un Select(COUNT()) y luego se aplicaban dichos límites, así que la lógica del numResults
	// era otra.
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private static int getNumResults(int startIndex, int numElements, String extraLimit)
	{
		int numResults = 0;
		
		if (!extraLimit.isEmpty())
		{
			// Es un paginado
			numResults = startIndex + numElements;
		}
		else
		{
			numResults = numElements;
		}
				
		return numResults;
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Devuelve la porción de sentencia SQL necesaria para acotar la búsqueda por dicha fecha
	// Aquellos cuya fecha Hasta sea mayor que la indicada por referencia y la fecha Desde menor que la actual
	// Es decir, aquellos que han estado vigente desde validityDate.
	// Se añade la fecha desde para acotar por fecha desde porque de lo contrario se disparaban los tiempos
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private static String getSubqueryDateFromDate(Date validityDate, int interval)
	{
		String subQuery = "";
		
		if (validityDate != null)
		{
			// 2012-04-12 08:34:00
			// http://jira.protecmedia.com:8080/browse/ITER-205?focusedCommentId=14771&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-14771
			SimpleDateFormat format  = new SimpleDateFormat(PropsValues.IS_PREVIEW_ENVIRONMENT 		? 
															IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_00 :
															IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss);
			String majorDateString  = format.format(validityDate);
			
			if (interval >= 0)
			{
				OutdatedDate outdated = new OutdatedDate(validityDate, interval);
				String minorDateString = format.format(outdated.getTime());
				
				subQuery = "\n\tAND n.vigenciadesde between '"+ minorDateString +"' AND '"+ majorDateString +"'\n\t "+IterKeys.QUERY_CHUNK_VIGHASTA+" '"+ minorDateString +"' ";
			}
			else
			{
				subQuery = "\n\tAND n.vigenciadesde <= '" + majorDateString +"' ";
			}
		}
		
		return subQuery;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private static int getNexInterval(int interval)
	{
		interval++;
		
		if (interval < 0 || interval >= OUTDATED_NUMINTERVALS)
			interval = -1;
		
		return interval;
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private static String getSubqueryDate(StringBuffer bodyQuery, Date validityDate, int numResults, int interval)
	{
		// Se obtiene las subquery que acota por fecha
		String queryDate = getSubqueryDateFromDate(validityDate, interval);
		
		if ( interval >= 0 )
		{
			StringBuffer query = new StringBuffer("SELECT COUNT(DISTINCT j.articleId) ");
			query.append(bodyQuery);
			
			String finalQuery = StringUtil.replace(query.toString(), IterKeys.QUERY_TOKEN_CHK_VALIDITYDATE, queryDate);
	
			_log.trace("getSubqueryDate:\n" + finalQuery);
			List<Object> resultsQuery = PageContentLocalServiceUtil.sqlQuery(finalQuery);
			if ( Integer.valueOf(resultsQuery.get(0).toString()).intValue() < numResults ) 
			{
				// Esta franja de tiempo no se encontró el número de registros deseados, se amplia
				interval = getNexInterval(interval);
				queryDate = getSubqueryDate(bodyQuery, validityDate, numResults, interval);
			}
		}
		
		return queryDate;
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private static List<Object> limitByValidityDate(String query, Date validityDate, int numResults, int interval)
	{
		List<Object> resultsQuery = null;
		
		// Se obtiene las subquery que acota por fecha
		String queryDate = getSubqueryDateFromDate(validityDate, interval);
		
		// Se sustituye en la query final
		String finalQuery = StringUtil.replace(query, IterKeys.QUERY_TOKEN_CHK_VALIDITYDATE, queryDate);

		if (_log.isTraceEnabled())
			_log.trace("executeFilterArticlesSQL: limitByValidityDate (expected rows:"+numResults+"):\n" + finalQuery);
		
		resultsQuery = PageContentLocalServiceUtil.sqlQuery(finalQuery);
		if ( interval >= 0 && resultsQuery.size() < numResults )
		{
			// Esta franja de tiempo no se encontró el número de registros deseados, se amplia
			interval = getNexInterval(interval);
			resultsQuery = limitByValidityDate(query, validityDate, numResults, interval);
		}
				
		return resultsQuery;
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Recibe:
	//	- la subquery correspondiente al Select.
	//	- la subquery correspondiente al Cuerpo de la sentencia (From... JOIN...WHERE...).
	//	- la subquery correspondiente a la ordenación de los resultados.
	//
	// Evalua dicha sentencia pero si no hay fecha concreta se intenta acotar de modo que no se tenga que buscar 
	// en TODOS los registros ya que sería muy lento.
	//
	// ATENCIÓN: ESTE MÉTODO ES SENSIBLE A MODIFICACIONES EN LA ESTRUCTURA DE LA CONSULTA SQL.
	// SI SE MODIFICA LA CONSULTA, COMPROBAR QUE LOS LÍMITES POR SECCIÓN NO SE VEAN AFECTADOS.
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private static List<Object> executeFilterArticlesSQL(StringBuffer selectQuery, StringBuffer bodyQuery, StringBuffer orderQuery,
														 boolean chkValiditydate, Date validityDate, boolean showNonActiveContents, 
														 int numResults, String extraLimit, String[] layoutIds, int[] layoutLimits)
	{
		List<Object> resultsQuery = null;
		int interval = 0;
		
		if (validityDate == null)
			validityDate = new Date();
		
		// Para hacerlo bien habra que analizar si hay más índices dentro del USE INDEX() porque en tal cao estarán separador por comas
		// y habrá que eliminar del resultado final esa coma
//		int indexPos;
//		// Solo los contenidos VIGENTES con validación por fecha usarán el índice VIGHASTA
//		if ( (!chkValiditydate || showNonActiveContents) &&
//			 (indexPos = bodyQuery.indexOf(IterKeys.QUERY_INDEX_PAGECONTENT_VIGHASTA)) >= 0 )
//		{
//			bodyQuery.delete(indexPos, indexPos+IterKeys.QUERY_INDEX_PAGECONTENT_VIGHASTA.length());
//		}
		
		selectQuery.append(bodyQuery);
		selectQuery.append(orderQuery);
		
		// Límites por sección
		// ATENCIÓN: ESTE MÉTODO ES SENSIBLE A MODIFICACIONES EN LA ESTRUCTURA DE LA CONSULTA SQL.
		// SI SE MODIFICA LA CONSULTA, COMPROBAR QUE LOS LÍMITES POR SECCIÓN NO SE VEAN AFECTADOS.
		if (null != layoutLimits && !showNonActiveContents)
			selectQuery = splitQueryBySectionLimits(selectQuery, layoutIds, layoutLimits, numResults);
		
		// La query anterior era un resultado total del que queremos un paginado, o resultado parcial
		if (!extraLimit.isEmpty())
		{
			StringBuffer partialSQL = new StringBuffer();
			partialSQL.append("SELECT *\n FROM \n\t(\n").append(selectQuery).append("\n\t) PAG \n\tLIMIT ").append(extraLimit);
			
			selectQuery = partialSQL;
		}

		String finalQuery = selectQuery.toString();
		
		if (!chkValiditydate)
		{
			// NO se quiere realizar una validación de las fechas de vigencia. Ej consultas de links internos
			resultsQuery = executeFilterArticlesSQL(finalQuery);
		}
		else if (!showNonActiveContents)
		{
			// Contenidos NO caducados. VIGENTES.
			// La fecha de referencia esté entre la vigencia del PageContent y no se incluyan vigencias futuras aunque la fecha de referencia lo sea
			// http://jira.protecmedia.com:8080/browse/ITER-205?focusedCommentId=14771&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-14771
			SimpleDateFormat format  = new SimpleDateFormat(PropsValues.IS_PREVIEW_ENVIRONMENT 		? 
															IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_00 :
															IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss);
			
			String referenDateString = format.format(validityDate);
			
			finalQuery 	 = StringUtil.replace(finalQuery, IterKeys.QUERY_TOKEN_CHK_VALIDITYDATE, "\n\tAND ('" + referenDateString + "' BETWEEN n.vigenciadesde AND n.vigenciahasta) ");
			resultsQuery = executeFilterArticlesSQL(finalQuery);
		}
		else if (extraLimit.isEmpty())
		{
			// Contenidos caducados que NO sean paginados
			resultsQuery = limitByValidityDate(finalQuery, validityDate, numResults, interval);
		}
		else
		{
			// Contenidos caducados PAGINADOS. Necesitan primero un SELECT(COUNT()) 
			String queryDate = getSubqueryDate(bodyQuery, validityDate, numResults, interval);
			
			finalQuery 	 = StringUtil.replace(finalQuery, IterKeys.QUERY_TOKEN_CHK_VALIDITYDATE, queryDate);
			resultsQuery = executeFilterArticlesSQL(finalQuery);
		}
		
		return resultsQuery;
	}
	
	private static List<Object> executeFilterArticlesSQL(String finalQuery)
	{
		if (_log.isTraceEnabled())
			_log.trace("executeFilterArticlesSQL:\n" + finalQuery);

		return PageContentLocalServiceUtil.sqlQuery(finalQuery);
	}
	
	public static List<String[]> getMetadatasLinks(long companyId, long groupId, String contentId, 
												   List<String> structures, int startIndex, int numElements, 
												   Date validityDate, boolean showNonActiveContents, 
												   String[] orderFields, int typeOrder)
	{
		return getMetadatasLinks(companyId, groupId, contentId, structures, startIndex, numElements, validityDate, 
								 showNonActiveContents, orderFields, typeOrder, "", null, null, null);
	}
	
	public static List<String[]> getMetadatasLinks(long companyId, long groupId, String contentId, 
												   List<String> structures, int startIndex, int numElements, 
												   Date validityDate, boolean showNonActiveContents, 
												   String[] orderFields, int typeOrder, 
												   List<String> contentVocabularyIds, List<String> contentCategoryIds, String[] layoutIds)
	{
		return getMetadatasLinks(companyId, groupId, contentId, structures, startIndex, numElements, validityDate, 
								 showNonActiveContents, orderFields, typeOrder, "", contentVocabularyIds, contentCategoryIds, layoutIds);
	}
	
	public static List<String[]> getMetadatasLinks(long companyId, long groupId, String contentId, 
												   List<String> structures, int startIndex, int numElements, 
												   Date validityDate, boolean showNonActiveContents, 
												   String[] orderFields, int typeOrder, String extraLimit,
												   List<String> contentVocabularyIds, List<String> contentCategoryIds, String[] layoutIds)
	{
		long[] categoryIds = CategoriesUtil.getArticleMetadatasFilteredLong(contentId, contentVocabularyIds, contentCategoryIds);
		
		List<String[]> articlesResult = new ArrayList<String[]>();
		if(categoryIds != null && categoryIds.length > 0)
		{
			if (_catUseOnlyParents.isFatalEnabled())
			{
				// Primero se intenta obtener el número de artículos esperado con las categorías implicadas, y de lo contrario se busca también en las categorías hijas
				// ITER-915 La versión AMP de algunos artículos tarda mucho en generarse
				// http://jira.protecmedia.com:8080/browse/ITER-915?focusedCommentId=35838&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-35838
				IterRequest.setAttribute(WebKeys.FILTERARTICLES_CATEGORIES_USE_ONLY_PARENTS, Boolean.TRUE);
						
				try
				{
					articlesResult = getFilterArticles(groupId, contentId, structures, startIndex, numElements, 
													   validityDate, showNonActiveContents, orderFields, typeOrder, 
													   categoryIds, null, null, layoutIds, false, extraLimit, null);
				}
				finally
				{
					IterRequest.removeAttribute(WebKeys.FILTERARTICLES_CATEGORIES_USE_ONLY_PARENTS);
				}
			}
			
			// En un pagina que tiene límites externo (número de elementos por páginas), el número de elementos esperado será el mínimo entre
			// el total de elementos que se quiere y el número de elementos que se quiere por página
			int expectedNumElements = extraLimit.isEmpty() ? numElements : Math.min(numElements, Integer.parseInt(extraLimit.split(",")[1]));
			
			// Se repite la consulta buscando en las categorías hijas
			if (articlesResult.size() < expectedNumElements)
			{
				articlesResult = getFilterArticles(groupId, contentId, structures, startIndex, numElements, 
						   validityDate, showNonActiveContents, orderFields, typeOrder, 
						   categoryIds, null, null, layoutIds, false, extraLimit, null);
			}
		}
		
		return articlesResult;
	}
	
	public static List<String[]> getInternalLinks(long groupId, String contentId, List<String> structures, int startIndex, 
												  int numElements, Date validityDate, boolean showNonActiveContents, String[] orderFields, int typeOrder, String[] layoutIds)
	{
		return getInternalLinks(groupId, contentId, structures, startIndex, numElements, 
				  validityDate, showNonActiveContents, orderFields, typeOrder, "", layoutIds);
	}
	
	public static List<String[]> getInternalLinks(long groupId, String contentId, List<String> structures, int startIndex, int numElements, 
			  Date validityDate, boolean showNonActiveContents, String[] orderFields, int typeOrder, String extraLimit, String[] layoutIds)
	{
		List<String[]> results = new ArrayList<String[]>();
		try
		{
			if( Validator.isNotNull(contentId) && !contentId.equals("0") )
			{
				JournalArticle article = JournalArticleLocalServiceUtil.getArticle(GroupMgr.getGlobalGroupId(), contentId);
				List<KeyValuePair> internalLinks = getRelatedInternalLinks(article);
				
				if(internalLinks != null && internalLinks.size() > 0)
				{
					results = getFilterArticles(groupId, contentId, structures, startIndex, 
												numElements, validityDate, showNonActiveContents, 
												orderFields, typeOrder, null, internalLinks, 
												null, layoutIds, true, extraLimit, null);
				}
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		return results;
	}
	
	private static KeyValuePair getkeyInList(String s, List<KeyValuePair> list)
	{
		KeyValuePair result = null;
		for(KeyValuePair item:list)
		{
			if(s.equals(item.getKey()))
			{
				result = item;
				break;
			}
		}
		return result;
	}
	
	@Deprecated
	public static String getTemplateId(JournalArticle article, String templateIdArticle, String templateIdGallery, String templateIdPoll, String templateIdMultimedia, String structureId, String templateId)
	{
		String resultTemplateId = "";
		if (article.getStructureId().equals(IterKeys.STRUCTURE_ARTICLE)){
			if(templateIdArticle != null && !templateIdArticle.isEmpty())
			{
				resultTemplateId = templateIdArticle;
			}else if(structureId != null && structureId.equals(IterKeys.STRUCTURE_ARTICLE)){
				resultTemplateId = templateId;
			}
		} 
		if (article.getStructureId().equals(IterKeys.STRUCTURE_GALLERY)){
			if(templateIdGallery != null && !templateIdGallery.isEmpty())
			{
				resultTemplateId = templateIdGallery;
			}else if(structureId != null && structureId.equals(IterKeys.STRUCTURE_GALLERY)){
				resultTemplateId = templateId;
			}
		} 
		if (article.getStructureId().equals(IterKeys.STRUCTURE_POLL)){
			if(templateIdPoll != null && !templateIdPoll.isEmpty())
			{
				resultTemplateId = templateIdPoll;
			}else if(structureId != null && structureId.equals(IterKeys.STRUCTURE_POLL)){
				resultTemplateId = templateId;
			}
		} 
		if (article.getStructureId().equals(IterKeys.STRUCTURE_MULTIMEDIA)){
			if(templateIdMultimedia != null && !templateIdMultimedia.isEmpty())
			{
				resultTemplateId = templateIdMultimedia;
			}else if(structureId != null && structureId.equals(IterKeys.STRUCTURE_MULTIMEDIA)){
				resultTemplateId = templateId;
			}
		} 
		
		if(resultTemplateId == null || resultTemplateId.isEmpty())
		{
			resultTemplateId = article.getTemplateId();
		}
		
		return resultTemplateId;
	}
	
	public static String getTemplateId(JournalArticle article, String templateIdArticle, String templateIdGallery, String templateIdPoll, String templateIdMultimedia)
	{
		String resultTemplateId = "";
		if(Validator.isNotNull(article))
		{
			resultTemplateId = article.getTemplateId();
			boolean iterSurveysEnabled = IterSurveyUtil.isEnabledInDelegation(article);
			
			// Si hay plantilla de artículo y o bien tiene estructura de artículo o bien estructura de encuesta con el nuevo sistema activado
			if((article.getStructureId().equals(IterKeys.STRUCTURE_ARTICLE) || (iterSurveysEnabled && article.getStructureId().equals(IterKeys.STRUCTURE_POLL))) && Validator.isNotNull(templateIdArticle))
			{
				resultTemplateId = templateIdArticle;
			}
			else if(article.getStructureId().equals(IterKeys.STRUCTURE_GALLERY) && 
					Validator.isNotNull(templateIdGallery))
			{
				resultTemplateId = templateIdGallery;
			} 
			else if(!iterSurveysEnabled && article.getStructureId().equals(IterKeys.STRUCTURE_POLL) && Validator.isNotNull(templateIdPoll))
			{
				resultTemplateId = templateIdPoll;
			} 
			else if(article.getStructureId().equals(IterKeys.STRUCTURE_MULTIMEDIA) &&
					Validator.isNotNull(templateIdMultimedia))
			{
				resultTemplateId = templateIdMultimedia;
			} 
		}
		
		return resultTemplateId;
	}
	
	public static int getTemplateMode(JournalArticle article, int articleTemplateMode, int galleryTemplateMode, int pollTemplateMode, int multimediaTemplateMode)
	{
		int resultTemplateMode = -1;
		if(Validator.isNotNull(article))
		{
			if(IterSurveyUtil.isEnabledInDelegation(article) || article.getStructureId().equals(IterKeys.STRUCTURE_ARTICLE))
			{
				resultTemplateMode = articleTemplateMode;
			}
			else if(article.getStructureId().equals(IterKeys.STRUCTURE_GALLERY))
			{
				resultTemplateMode = galleryTemplateMode;
			} 
			else if(article.getStructureId().equals(IterKeys.STRUCTURE_POLL))
			{
				resultTemplateMode = pollTemplateMode;
			} 
			else if(article.getStructureId().equals(IterKeys.STRUCTURE_MULTIMEDIA))
			{
				resultTemplateMode = multimediaTemplateMode;
			} 
		}
		
		return resultTemplateMode;
	}
	
	public static JournalArticle getExampleJournal(long globalGroupId, boolean articleFilter, boolean galleryFilter, 
												   boolean multimediaFilter, boolean pollFilter)
	{
		JournalArticle journalArticle = null;
		try
		{
			if(articleFilter)
			{
				journalArticle = JournalArticleLocalServiceUtil.getArticle(globalGroupId, IterKeys.EXAMPLEARTICLEID);
			}
			else
			{
				if(galleryFilter)
				{
					journalArticle = JournalArticleLocalServiceUtil.getArticle(globalGroupId, IterKeys.EXAMPLEGALLERYID);
				}
				else
				{
					if(pollFilter)
					{
						journalArticle = JournalArticleLocalServiceUtil.getArticle(globalGroupId, IterKeys.EXAMPLEPOLLID);
					}
					else
					{
						if(multimediaFilter)
						{
							journalArticle = JournalArticleLocalServiceUtil.getArticle(globalGroupId, IterKeys.EXAMPLEMULTIMEDIAID);
						}
						else
						{
							journalArticle = JournalArticleLocalServiceUtil.getArticle(globalGroupId, IterKeys.EXAMPLEARTICLEID);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		return journalArticle;
	}
	
	public static String getOrderName(String value, PageContext pageContext){

		int valueChanged = Integer.valueOf(value);
		String name;
		switch(valueChanged){
			case IterKeys.ORDER_STARTDATE:
				name = LanguageUtil.format(pageContext, "teaser-viewer-edit-order-by-display-date", new Object());
				break;
			case IterKeys.ORDER_EXPIRATIONDATE:
				name = LanguageUtil.format(pageContext, "teaser-viewer-edit-order-by-expiration-date", new Object());
				break;
			case IterKeys.ORDER_ORDEN:
				name = LanguageUtil.format(pageContext, "teaser-viewer-edit-order-by-order", new Object());
				break;
			case IterKeys.ORDER_DISPLAYDATE:
				name = LanguageUtil.format(pageContext, "teaser-viewer-edit-order-by-creation-date", new Object());
				break;
			case IterKeys.ORDER_MODIFICATIONDATE:
				name = LanguageUtil.format(pageContext, "teaser-viewer-edit-order-by-modification-article-date", new Object());
				break;
			case IterKeys.ORDER_COMMENT:
				name = LanguageUtil.format(pageContext, "teaser-viewer-edit-order-operation-comment", new Object());
				break;
			case IterKeys.ORDER_SHARED:
				name = LanguageUtil.format(pageContext, "teaser-viewer-edit-order-operation-shared", new Object());
				break;
			case IterKeys.ORDER_RATINGS:
				name = LanguageUtil.format(pageContext, "teaser-viewer-edit-order-operation-ratings", new Object());
				break;
			case IterKeys.ORDER_SENT:
				name = LanguageUtil.format(pageContext, "teaser-viewer-edit-order-operation-sent", new Object());
				break;
			case IterKeys.ORDER_VIEW:
				name = LanguageUtil.format(pageContext, "teaser-viewer-edit-order-operation-view", new Object());
				break;
			default:
				name = "";
				break;
		}
		return name;
	}
	
	private static String getSQLOrderField(int orderKey, int sqlOrderType){
		String typeOrder = "ASC";
		if(sqlOrderType == IterKeys.ORDER_DESC)
		{
			typeOrder = "DESC";
		}
		String result = null;
		switch(orderKey){
			case IterKeys.ORDER_ORDEN:
				result = "n.orden " + typeOrder;
				break;
			case IterKeys.ORDER_EXPIRATIONDATE:	
				result = "n.vigenciahasta " + typeOrder;
				break;
			case IterKeys.ORDER_STARTDATE:
				result = "n.vigenciadesde " + typeOrder;
				break;
			case IterKeys.ORDER_MODIFICATIONDATE:
				result = "j.modifiedDate " + typeOrder;
				break;
			case IterKeys.ORDER_SENT:
				result = "envios " + typeOrder;
				break;
			case IterKeys.ORDER_COMMENT:
				result = "iter_statistics_journalarticle.statisticCounter DESC ";
				break;
			case IterKeys.ORDER_RATINGS:
				result = "ratings " + typeOrder;
				break;
			case IterKeys.ORDER_VIEW:
				result = "visitas " + typeOrder;
				break;
			case IterKeys.ORDER_SHARED:
				result = "statisticCounter DESC " ;
				break;
			default:
				result = "j.displayDate " + typeOrder;
				break;
		}
		return result;
	}
	
	private static String getSQLOrderSelect(int orderKey){
		String result = null;
		switch(orderKey){
			case IterKeys.ORDER_SENT:
				result = "(SELECT IFNULL(SUM(c1.counter),0) FROM News_Counters c1 WHERE j.articleId=c1.contentId AND operation=" + IterKeys.OPERATION_SENT + ") envios";
				break;
			case IterKeys.ORDER_RATINGS:
				result = "(SELECT IFNULL((c2.value/c2.counter),0) FROM News_Counters c2 WHERE j.articleId=c2.contentId AND operation=" + IterKeys.OPERATION_RATINGS + ") ratings";
				break;
			case IterKeys.ORDER_VIEW:
				result = "(SELECT IFNULL(SUM(av.visits),0) FROM article_visits av WHERE j.articleId=av.articleId ) visitas";
				break;
			case IterKeys.ORDER_SHARED:
				result = "SUM(IFNULL(iter_statistics_journalarticle.statisticCounter, 0)) statisticCounter, SUM(IFNULL(iter_statistics_journalarticle.share_count,0)) share_count ,  SUM(IFNULL(iter_statistics_journalarticle.like_count,0)) like_count , SUM(IFNULL(iter_statistics_journalarticle.comment_count,0)) comment_count , "
                      + " SUM(IFNULL(iter_statistics_journalarticle.click_count,0)) click_count , SUM(IFNULL(iter_statistics_journalarticle.commentsbox_count,0)) commentsbox_count, SUM(IFNULL(iter_statistics_journalarticle.facebookCounter,0)) facebookCounter ,  SUM(IFNULL(iter_statistics_journalarticle.googlePlusCounter,0)) googlePlusCounter , SUM(IFNULL(iter_statistics_journalarticle.twitterCounter,0)) twitterCounter ";
				break;
		}
		return result;
	}

	private static String getSQLHavingField(int orderKey)
	{
		String result = null;
		
		switch(orderKey)
		{
			case IterKeys.ORDER_RATINGS:
				result = "\n\tHAVING ratings>0";
			break;
			
			case IterKeys.ORDER_VIEW:
				result = "\n\tHAVING visitas>0";
			break;
		}

		return result;
	}
	
	public static String replaceFieldInArticleContentXML(String valueToChange, String newValue, String xmlContent)
	{
		String newXMLContent = xmlContent;
		try{
			if(xmlContent != null)
			{
				Document doc = SAXReaderUtil.read(xmlContent);
				Element oldRoot = doc.getRootElement();
				List<Node> oldNodes = oldRoot.content();
				if(oldNodes != null && oldNodes.size() > 0)
				{
					for(int i = 0; i < oldNodes.size(); i++)
					{
						Node currentNode = oldNodes.get(i);
						if(currentNode.getStringValue().equals(valueToChange))
						{
							currentNode.setText(newValue);
							break;
						}
					}
					newXMLContent = doc.asXML();
				}
			}
		}catch(Exception e){
			_log.error(e);
		}
		return newXMLContent;
	}
	
	public static String getLocaleValueByKey(Locale locale, String key)
	{
		rb = ResourceBundle.getBundle("content.Language", locale);
		return rb.getString(key);
	}
	
	public static String getCSSClass(int index, int gblLastItem)
	{
		String result = "";
		
		if (!HtmlOptimizer.isEnabled())
		{
			StringBuffer cssClass = new StringBuffer("n" + String.valueOf(index) + (index%2==0?" even":" odd"));
			
			if(index == 1)
				cssClass.append(" first");
			
			if(index == gblLastItem)
				cssClass.append(" last");
			
			result = cssClass.toString();
		}

		return result;
	}
	
	public static String getCSSClass(int index, int listSize, JournalArticle journalArticle, HttpServletRequest request)
	{
		return getCSSClass(index, listSize) + " " + getCSSAccessClass(journalArticle, request);
	}
	
	public static String getCSSAccessClass(JournalArticle journalArticle, HttpServletRequest request)
	{
		StringBuilder cssAccessClass = new StringBuilder();
		String contentId = null;
		try
		{
			contentId = journalArticle.getArticleId();
			if(PHPUtil.isApacheRequest(request))
			{
				String productsList = PortalLocalServiceUtil.getProductsByArticleId(contentId);
				if(Validator.isNotNull(productsList))
				{
			    	String modifiedDate = simpleDateFormat.format(journalArticle.getModifiedDate());
			    	
					PublicIterParams.set(WebKeys.ITER_RESPONSE_NEEDS_PHP, true);
					
					cssAccessClass.append("<?php if (user_has_access_to_any_of_these_products( '" + contentId 		+ 	"', array("			+ 
				 	 																 				productsList 	+ 	"),'" 				+ 
				 	 																 				modifiedDate 	+ 	"' )===true){ print('full-access'); } else {print('no-access');} ?>");
				}
			}
			else
			{
				ThemeDisplay td = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
				boolean signedIn = true;
				if(Validator.isNull(td))
					_log.error("TeaserContentUtil.getCSSAccessClass: ThemeDisplay is null");
				else
					signedIn = td.isSignedIn();
				
				if( signedIn && !PortalLocalServiceUtil.hasSimulatedAccessToArticle(contentId, request))
					cssAccessClass.append("no-access");
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		finally
		{
			if(cssAccessClass.length() == 0)
				cssAccessClass.append("full-access");

			// Comprueba si tiene productos asignados
			if (Validator.isNotNull(contentId))
			{
				try
				{
					cssAccessClass.append(PortalLocalServiceUtil.getProductsByArticleId(contentId).length() == 0 ? " norestricted" : " restricted");
				}
				catch(Exception e)
				{
					_log.error(e.toString());
					_log.trace(e);
				}
			}
		}
		
		return cssAccessClass.toString();
	}
	
	public static String getStructureCssClass(String structureId)
	{
	 	String classCss = "";
	 	if (!HtmlOptimizer.isEnabled() && Validator.isNotNull(structureId))
	 	{
		 	if(IterKeys.STRUCTURE_GALLERY.equals(structureId))
		 		classCss = "td-gallery";
		 	else if(IterKeys.STRUCTURE_POLL.equals(structureId))
		 		classCss = "td-poll";
		 	else  if (IterKeys.STRUCTURE_MULTIMEDIA.equals(structureId))
		 		classCss = "td-multimedia";
		 	else
		 		classCss = "td-article";
	 	}
	 	return classCss;
	}
	
	public static int getLastPosition(int i, int teaserPerPage)
	{
		if (i == 1)
			return teaserPerPage + 1;
		else
			return teaserPerPage + getLastPosition(i-1,teaserPerPage);
	}
	
	public static String formatWidth(int size)
	{
		double dend = 100;
		double dsor = size;
		double resultWidth = dend/dsor;
		
		DecimalFormat df = new DecimalFormat("#.###");
		df.setRoundingMode(RoundingMode.DOWN);
		String formatWidth = df.format(resultWidth);
		
		return formatWidth.replace(',', '.');
	}
	
	private static List<String> getStatisticsOperation(String[] orderFields)
	{
		List<String> result = new ArrayList<String>();
		for (String orderField : orderFields)
		{
			if(ordersValuesIsUsedInIterStatistics.contains(orderField)){
				result.addAll(IterKeys.ORDER_TO_OPERATION.get(orderField));				
			}
				
		}
		return result;
	}
	
	public static String encodeUsrFilterOpts(String usrFilterBy, String[] usrFilterLayouts, String[] usrFilterCategories, String[] usrFilterVocabularies, String displayOption ) throws UnsupportedEncodingException
	{
		String retVal = "";
		if( !usrFilterBy.equalsIgnoreCase(IterKeys.WITHOUT_FILTER) && !usrFilterBy.equalsIgnoreCase(IterKeys.DATE) )
		{
			JSONObject jsObj = JSONFactoryUtil.createJSONObject();
			jsObj.put(IterKeys.FILTER_TYPE, usrFilterBy);
			jsObj.put(IterKeys.FILTER_DISPLAY_OPT, displayOption);
			
			if(usrFilterBy.equalsIgnoreCase(IterKeys.SECTIONS))
				jsObj.put(IterKeys.FILTER_DATA, StringUtil.merge(usrFilterLayouts, "','") );
			else
			{
				//jsObj.put(IterKeys.FILTER_DATA, StringUtil.merge(usrFilterCategories) );
				jsObj.put(IterKeys.FILTER_CAT_DATA, StringUtil.merge(usrFilterCategories) );
				jsObj.put(IterKeys.FILTER_VOC_DATA, StringUtil.merge(usrFilterVocabularies) );
			}
				

			retVal = Base64.encode(jsObj.toString().getBytes(Digester.ENCODING));
		}
		
		return retVal;
	}
	
	public static List<String> getUserOptions(String query) throws SecurityException, NoSuchMethodException
	{
		Document d = PortalLocalServiceUtil.executeQueryAsDom(query, true, XMLHelper.rsTagName, "option", new String[]{"name"});
		
		List<Node> optionNodes = d.selectNodes("/rs/option");
		List<String> optionsList = new ArrayList<String>();
		optionsList.add("<option value='-1'></option>");
		
		for(Node option : optionNodes)
		{
			((Element)option).setText( 
										option.selectSingleNode("name").detach().getText() 
									);
			optionsList.add( option.asXML() );
		}
		
		return optionsList;
	}

	public static String encodeTeaserConfigParams(String portletItem, String refPreferenceId, String portletId, String contentId, String categoryIds,
			String date, long teasertotalcount, long scopeGroupId, long companyId, String languageId, long plid, long sectionPlid,
			boolean secure, long userId, boolean lifecycleRender, String pathFriendlyURLPublic, String pathFriendlyURLPrivateUser,
			String pathFriendlyURLPrivateGroup, String serverName, String cdnHost, String pathImage, String pathMain, String pathContext,
			String urlPortal, String pathThemeImages, int serverPort, String scheme, boolean includeCurrentContent ) 
	throws UnsupportedEncodingException
	{
		String retVal = "";
		
		JSONObject jsObj = JSONFactoryUtil.createJSONObject();
		
		jsObj.put(IterKeys.PREFS_PORTLETITEM, portletItem);
		jsObj.put(IterKeys.REFPREFERENCEID, refPreferenceId);
		jsObj.put(IterKeys.PORTLETID, portletId);
		jsObj.put(IterKeys.CONTENTID, contentId);
		jsObj.put(IterKeys.CATEGORYIDS, categoryIds);
		jsObj.put(IterKeys.DATE, date);
		jsObj.put(IterKeys.TEASERTOTALCOUNT, teasertotalcount);
		jsObj.put(IterKeys.SCOPEGROUPID, scopeGroupId);
		jsObj.put(IterKeys.COMPANYID, companyId);
		jsObj.put(IterKeys.LANGUAGEID, languageId);
		jsObj.put(IterKeys.PLID, plid);
		jsObj.put(IterKeys.SECTIONPLID, sectionPlid);
		jsObj.put(IterKeys.SECURE, secure);
		jsObj.put(IterKeys.USERID, userId);
		jsObj.put(IterKeys.LIFECYCLERENDER, lifecycleRender);
		jsObj.put(IterKeys.PATHFRIENDLYURLPUBLIC, pathFriendlyURLPublic);
		jsObj.put(IterKeys.PATHFRIENDLYURLPRIVATEUSER, pathFriendlyURLPrivateUser);
		jsObj.put(IterKeys.PATHFRIENDLYURLPRIVATEGROUP, pathFriendlyURLPrivateGroup);
		jsObj.put(IterKeys.SERVERNAME, serverName);
		jsObj.put(IterKeys.CDNHOST, cdnHost);
		jsObj.put(IterKeys.PATHIMAGE, pathImage);
		jsObj.put(IterKeys.PATHMAIN, pathMain);
		jsObj.put(IterKeys.PATHCONTEXT, pathContext);
		jsObj.put(IterKeys.URLPORTAL, urlPortal);
		jsObj.put(IterKeys.PATHTHEMEIMAGES, pathThemeImages);
		jsObj.put(IterKeys.SERVERPORT, serverPort);
		jsObj.put(IterKeys.SCHEME, scheme);
		jsObj.put(IterKeys.INCLUDECURRENTCONTENT, includeCurrentContent);
		jsObj.put(WebKeys.IS_MOBILE_REQUEST, IterRequest.isMobileRequest());
		
		retVal = Base64.encode(jsObj.toString().getBytes(Digester.ENCODING)); 
		
		return retVal;
	}
	
	private static StringBuffer splitQueryBySectionLimits(StringBuffer originalQuery, String[] layoutIds, int[] layoutLimits, int globalLimit)
	{
		long t0 = Calendar.getInstance().getTimeInMillis();
		// Si no hay límites, retorna la query original
		if (ArrayUtil.sum(layoutLimits) == 0 || layoutIds == null || layoutIds.length != layoutLimits.length)
		{
			return originalQuery;
		}
		// Si hay límites Entonces
		else
		{
			if (_log.isTraceEnabled())
				_log.trace( String.format("splitQueryBySectionLimits before:\n%s", originalQuery.toString()));
			
			// Crea la query patrón para cada sección con límite.
			// Es MUY IMPORTANTE (?!\\)) porque de lo contrario toma el último paréntesis, de esta forma se indica que es cualquier carácter excepto el paréntesis
			String pattern = originalQuery.toString().replaceAll("AND n.layoutId IN(\\()(((?!\\)).)*)(\\))", "AND n.layoutId IN (%s)").replaceAll("LIMIT .*", "LIMIT 0,%s");
			// Añadir al SELECT las columnas del ORDER BY que no estén ya, sin la referencia a las tablas.
			String selectFieldsWithTables = StringUtil.extractFromQuery(originalQuery, StringUtil.EXTRACT_SELECT_FIELDS, false, false);
			String finalSelectFields = selectFieldsWithTables;
			
			String orderFields = StringUtil.extractFromQuery(originalQuery, StringUtil.EXTRACT_ORDER_FIELDS, false, true);
			// ITER-514	El TeaserViewer no carga noticias si se ordena por calificación y se limita el número de artículos de una sección
			// Se sustituye:
			//		j.createDate, FIELD(n.qualificationId,  '10890163', '10890166') , n.orden , j.articleId
			// por:
			//		j.createDate, n.qualificationId , n.orden , j.articleId
			String[] orderFieldsWithTables = orderFields.replaceAll("(?i)(.*)(field\\s*\\(\\s*([^\\)]*qualificationId)([^\\)]+)\\))(.*)", "$1$3$5").split(StringPool.COMMA);
			// String[] orderFieldsWithTables = StringUtil.extractFromQuery(originalQuery, StringUtil.EXTRACT_ORDER_FIELDS, false, true).split(StringPool.COMMA);
			for (String field : orderFieldsWithTables)
			{
				if (!finalSelectFields.contains(field.trim()))
				{
					finalSelectFields += StringPool.COMMA_AND_SPACE + field;
				}
			}
			pattern = pattern.replace(selectFieldsWithTables, finalSelectFields);
			
			StringBuffer finalQuery = new StringBuffer();

			StringBuilder layoutIdsNoLimit = new StringBuilder();
			// Para cada sección
			for (int i = 0; i < layoutIds.length; i++)
			{
				// Si tiene límite Entonces
				if (layoutLimits[i] > 0)
				{
					// Copiar la query -- > Sustituir el AND n.layoutId IN (...) por los layoutNoLimit[] -->
					// Sustituir el LIMIT por el Limite global --> Wrappear la query con paréntesis (<query>).
					if (finalQuery.length() > 0)
						finalQuery.append("\nUNION ALL\n");
					finalQuery.append(StringPool.OPEN_PARENTHESIS)
							  .append(String.format(pattern, StringUtil.apostrophe(layoutIds[i]), layoutLimits[i]))
							  .append(StringPool.CLOSE_PARENTHESIS);
					// Restar su Límite al Límite global
					globalLimit -= layoutLimits[i];
				}
				// Si No, se añade al listado de secciones sin límite
				else
				{
					layoutIdsNoLimit.append( layoutIdsNoLimit.length() > 0 ? StringPool.COMMA_AND_SPACE + StringUtil.apostrophe(layoutIds[i]) : StringUtil.apostrophe(layoutIds[i]));
				}
			}

			// Secciones sin límite definido
			if (layoutIdsNoLimit.length() > 0 && globalLimit > 0)
			{
				// Copiar la query -- > Sustituir el AND n.layoutId IN (...) por los layoutNoLimit[] -->
				//Sustituir el LIMIT por el Limite global --> Wrappear la query con paréntesis (<query>).
				if (finalQuery.length() > 0)
					finalQuery.append("\nUNION ALL\n");
				finalQuery.append(StringPool.OPEN_PARENTHESIS)
						  .append(String.format(pattern, layoutIdsNoLimit, globalLimit))
						  .append(StringPool.CLOSE_PARENTHESIS);
			}
			
			//Wreapear la query final con <SELECT ORIGINAL sin referencia a tablas> ( <query> ) AS T <ORDER BY ORIGINAL sin referencia a tablas> <LIMIT ORIGINAL>
			String selectFields = StringUtil.extractFromQuery(originalQuery, StringUtil.EXTRACT_SELECT_FIELDS, true, true);
			orderFields = StringUtil.extractFromQuery(originalQuery, StringUtil.EXTRACT_ORDER_FIELDS, true, false);
			String limit = StringUtil.extractFromQuery(originalQuery, StringUtil.EXTRACT_LIMIT, true, true);
	        
			finalQuery.insert(0, "SELECT DISTINCT " + selectFields + " FROM (\n")
			          .append("\n) AS T\nORDER BY " + orderFields + "\n")
			          .append("LIMIT "+ limit);

			if (_log.isTraceEnabled())
				_log.trace( String.format("splitQueryBySectionLimits after:\n%s", finalQuery.toString()));
			
			if (_log.isDebugEnabled())
				_log.debug("Time Splitting Query: " + ( Calendar.getInstance().getTimeInMillis() - t0 ) + "ms");
			
			return finalQuery;
		}
	}
	
	/**
	 * 1- Método que obtiene la lista de resultados a pintar<br/>
	 * 
	 * 2- Posteriormente analiza dicha lista de resultados para:<br/>
	 *  - Determinar los artículos que habría que incluir en el X-ITER_RESPONSE_VARY_IDARTS<br/>
	 *  - Determinar los artículos que realmente habría que pintar (exluyendo el actual si <i>includeCurrentContent</i>=false)<br/>
	 * 
	 * @param listResultWithCurrentContent
	 * @param includeCurrentContent
	 * @see http://confluence.protecmedia.com:8090/x/w4RmAg#GeneraciónautomáticadeWidgetsenlosportlets-ListCurrentContent
	 * @return Lista de artíulos a pintar
	 */
	public static List<String[]> getTeaserArticles( long groupId, String contentId, List<String> structures, int startIndex, int numElements, 
													Date validityDate, boolean showNonActiveContents, String[] orderFields, int typeOrder, 
													long[] categoriesId, String[] qualificationId, String[] layoutIds, 
													boolean includeCurrentContent, Date creationDate, int[] layoutLimits)
	{
		// http://confluence.protecmedia.com:8090/x/w4RmAg#GeneraciónautomáticadeWidgetsenlosportlets-ListCurrentContent
		// Si no existe contentId NO se puede "excluir" de la lista de resultados
		includeCurrentContent = !(!includeCurrentContent && Validator.isNotNull(contentId));
		
		
		// Si hay que excluir el contenido actual, se incrementa en 1 para luego descartarlo. 
		// El modo autowidget necesita el elemento excluido aunque no se pinte
		int numArticles = (!includeCurrentContent) ? numElements+1 : numElements;
			
		List<String[]> listResultWithCurrentContent = getFilterArticles(groupId, contentId, structures, startIndex, numArticles, 
																		validityDate, showNonActiveContents, orderFields, typeOrder, 
																		categoriesId, null, qualificationId, layoutIds, 
																		true, "", creationDate, layoutLimits);
		
		// Se obtiene la lista que se pasarà como ITER_RESPONSE_VARY_IDARTS
		if (Validator.isNotNull(listResultWithCurrentContent))
		{
			List <String> articleList = new ArrayList<String>();
			int limit  = Math.min(listResultWithCurrentContent.size(), numElements);
			for (int i = 0; i < limit; i++)
				articleList.add(listResultWithCurrentContent.get(i)[0]);
			
			IterRequest.getOriginalRequest().setAttribute(WebKeys.REQUEST_ATTRIBUTE_ARTICLE_LIST, articleList);
		}
		
		List<String[]> listResult = listResultWithCurrentContent.subList(0, listResultWithCurrentContent.size());

		// Como se ha incluído un elemento de más, se elimina el contenido actual, o el último si el actual no está en la lista
		if (!includeCurrentContent)
		{
			for (int i = 0; i < listResult.size(); i++)
			{
				if ( contentId.equals(listResult.get(i)[0]) || i == numElements )
				{
					listResult.remove(i);
					break;
				}
			}
		}

		return listResult;
	}
	
	public static boolean allowChangePosition(String[] orderBy, boolean defaultLayout, String[] layoutIds)
	{
		return (ArrayUtil.contains(orderBy,String.valueOf(IterKeys.ORDER_ORDEN)) && (defaultLayout || Validator.isNotNull(layoutIds)));
	}
}
