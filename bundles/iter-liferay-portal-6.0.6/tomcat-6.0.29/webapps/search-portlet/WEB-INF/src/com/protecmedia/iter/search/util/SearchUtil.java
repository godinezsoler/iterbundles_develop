/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.search.util;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.portlet.PortletPreferences;
import javax.portlet.RenderRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.util.Base64;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.BooleanClauseOccur;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.BooleanQueryFactoryUtil;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Hits;
import com.liferay.portal.kernel.search.IndexSearcher;
import com.liferay.portal.kernel.search.IterSearchUtil;
import com.liferay.portal.kernel.search.ParseException;
import com.liferay.portal.kernel.search.SearchEngine;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.StringQueryImpl;
import com.liferay.portal.kernel.search.TermQuery;
import com.liferay.portal.kernel.search.TermQueryFactoryUtil;
import com.liferay.portal.kernel.search.facet.Facet;
import com.liferay.portal.kernel.search.facet.FacetGroup;
import com.liferay.portal.kernel.search.facet.FacetGroupImpl;
import com.liferay.portal.kernel.search.facet.FacetedField;
import com.liferay.portal.kernel.search.facet.FacetedQueryImpl;
import com.liferay.portal.kernel.util.IterGlobalKeys;
import com.liferay.portal.kernel.util.IterURLUtil;
import com.liferay.portal.kernel.util.KeyValuePair;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.sectionservers.SectionServersMgr;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.model.Communities;
import com.protecmedia.iter.base.service.CommunitiesLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceError;

public class SearchUtil {
	
	private static Log _log = LogFactoryUtil.getLog(SearchUtil.class);
	
	private static final String GET_LAYOUT_FRIENDLYURL = "SELECT friendlyURL FROM Layout WHERE groupId=%d AND uuid_= '%s'";
	private static final String GET_LAYOUT_FRIENDLYURL_CHECK = "SELECT plid FROM Layout WHERE friendlyURL = '%s'";
	private static final String GET_LAYOUT_UUID = "SELECT plid FROM Layout WHERE uuid_ IN (\"%s\")";
	
	public static final String SEARCH_PORTLET_RESULTS_ID = "searchresultsportlet_WAR_searchportlet";
	
	public static final String SEARCH_KEYWORDS_FULL = "^";
	public static final int MAX_ITEMS_PER_PAGE = 75;
	
	public static final String SEARCH_ORDER_DEFAULT = SearchOptions.ORDER_BY_RELEVANCE;
	
	public static final String GET_LAYOUTS_NAMES	= " SELECT '%s' field, uuid_ id, EXTRACTVALUE(Layout.name, '/root/name[1]/text()') label FROM Layout \n\t WHERE uuid_ IN ('%s') ";
	public static final String GET_METADATA_NAMES 	= " SELECT '%s' field, categoryId id, name label FROM AssetCategory \n\t WHERE categoryId IN ('%s') ";
	
	/**
	 * @param searchOptions opciones de busqueda
	 * @return listado con los identificadores de los webContent resultado
	 * @throws ParseException
	 * @throws SearchException
	 * @throws DocumentException
	 * @throws UnsupportedEncodingException 
	 */
	public static SearchResults search(SearchOptions searchOptions, RenderRequest request, int cur, int delta, boolean paged) 
			throws ParseException, SearchException, DocumentException, UnsupportedEncodingException
	{

		SearchResults sr = new SearchResults();
		
		try
		{
			long tIni = 0L;
			long tIni2 = 0L;
			
			if(_log.isTraceEnabled())
				tIni = System.currentTimeMillis();
			
			String solrEndpoint = PortalUtil.getPortalProperties().getProperty(IterGlobalKeys.PORTAL_PROPERTIES_KEY_ITER_SEARCH_PLUGIN_ENDPOINT);
			boolean solr = Validator.isNull(solrEndpoint) ? false : true;
			
			if(Validator.isNotNull(searchOptions.getText()))
			{
	
				BooleanQuery fullQuery = BooleanQueryFactoryUtil.create();
				
				//Sólo contenidos vigentes
				Date currentDate = Calendar.getInstance().getTime();
				DateFormat df = new SimpleDateFormat(WebKeys.URL_PARAM_DATE_FORMAT_FULL);
				String dateFilterStr = IterKeys.STANDARD_ARTICLE_INDEX_STARTVALIDITY + ":[0 TO " + df.format(currentDate) + "]";
				StringQueryImpl dateFilter = new StringQueryImpl(dateFilterStr);
				fullQuery.add(dateFilter, BooleanClauseOccur.MUST);
				
				// Contenidos buscables
				// http://jira.protecmedia.com:8080/browse/ITER-1305?focusedCommentId=56794&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-56794
				StringQueryImpl searchFilter = new StringQueryImpl( String.format("%s:(\"true\")", Field.SEARCHABLE) );
				fullQuery.add(searchFilter, BooleanClauseOccur.MUST);
				
				//Fecha de creación (YYYYMMDDHHMMSS)
				if ((searchOptions.getStartDate() != null) && (searchOptions.getEndDate() != null))
				{
					Date newStart = getDateFromURLParam(searchOptions.getStartDate(), false);
					Date newEnd = getDateFromURLParam(searchOptions.getEndDate(), true);
					
					if(newStart != null && newEnd != null)
					{
						dateFilterStr = Field.MODIFIED_DATE + ":[" + df.format(newStart) + " TO " + df.format(newEnd) + "]";
						dateFilter = new StringQueryImpl(dateFilterStr);
						fullQuery.add(dateFilter, BooleanClauseOccur.MUST);
					}
				}
					
				//Estructuras
				appendStructureQuery(solr, searchOptions, fullQuery);
				
				//Texto			
				String searchText = searchOptions.getText();
				if(!searchText.equals(SEARCH_KEYWORDS_FULL))
				{
					searchText = searchText.replaceAll(StringPool.COLON, StringPool.SPACE);
					if(searchOptions.isFuzzy())
						searchText = searchText + "~";
						
					if(solr)
					{
						TermQuery termQuery = TermQueryFactoryUtil.create(Field.CONTENT, "(" + searchText + ")");
						fullQuery.add(termQuery, BooleanClauseOccur.MUST);
					}
					else
					{
						StringQueryImpl textFilter = new StringQueryImpl(Field.CONTENT 	+ ":(" + searchText + ")");
						fullQuery.add(textFilter, BooleanClauseOccur.MUST);
					}
				}
				
				//Categorías
				if (searchOptions.getCategoriesIds() != null && searchOptions.getCategoriesIds().size() > 0)
				{
					BooleanQuery categoriesIdsQuery = BooleanQueryFactoryUtil.create();
					for (Long category : searchOptions.getCategoriesIds())
					{
						if (category != 0)
							categoriesIdsQuery.add(TermQueryFactoryUtil.create(Field.ASSET_CATEGORY_IDS, category), BooleanClauseOccur.SHOULD);
					}
					
					fullQuery.add(categoriesIdsQuery, BooleanClauseOccur.MUST);
				}
				
				//Páginas
				List<Long> layoutsPlid = searchOptions.getLayoutsPlid();
				if (layoutsPlid != null && layoutsPlid.size() > 0)
				{
					StringBuilder layoutIds = new StringBuilder( StringPool.OPEN_PARENTHESIS );
					boolean first = true;
					for (Long currentPlid : layoutsPlid)
					{
						if (currentPlid != 0)
						{
							if(!first) 
								layoutIds.append(" OR ");
							first=false;
							layoutIds.append( currentPlid );
						}	
					}
					layoutIds.append( StringPool.CLOSE_PARENTHESIS );
					
					fullQuery.add(TermQueryFactoryUtil.create(IterKeys.STANDARD_ARTICLE_INDEX_LAYOUTSPLID, layoutIds.toString()), BooleanClauseOccur.MUST);
				}
				
				//ScopeGroup
				Layout layout = (Layout) request.getAttribute(WebKeys.LAYOUT);
				long scopeGroupId = layout.getGroupId();
				fullQuery.addRequiredTerm(Field.SCOPE_GROUP_ID, scopeGroupId);
				
				String fq = searchOptions.getFilterquery();
				Map<String, Object> filterParams = getFilterQuery( fq );
				BooleanQuery filterQuery = (BooleanQuery)filterParams.get(SearchKeys.FILTER_QUERY);
				Map<String, String> filters = (Map<String, String>)filterParams.get(SearchKeys.FILTERS);
				
				if(_log.isTraceEnabled())
					tIni2 = Calendar.getInstance().getTimeInMillis();
				
				Map<String, Object> facets = FacetedTools.getFacets( layout.getTypeSettings(), layout.getPlid(), filters );
				
				if(_log.isTraceEnabled())
					_log.trace( String.format("SOLR-TIME Obtener datos de la segmentacion para la consulta. Tiempo %s ms.", System.currentTimeMillis()-tIni2));
				
				List<Facet> facetList = (List<Facet>) facets.get(SearchKeys.FACETS_LIST);
				Map<String, List<String>> portletFacets = (Map<String, List<String>>) facets.get(SearchKeys.PORTLET_FACETS);
				
				PortalUtil.getOriginalServletRequest( PortalUtil.getHttpServletRequest(request) ).setAttribute(IterKeys.REQUEST_ATTRIBUTE_FACETS, portletFacets);
				
				//Ordenación (Por defecto, relevancia)
				Sort sort = new Sort();
				if(searchOptions.getOrder().equals(SearchOptions.ORDER_BY_TITLE))
				{
					sort.setFieldName(Field.TITLE);
					sort.setReverse(false);
					sort.setType(Sort.STRING_TYPE);
				}
				else if(searchOptions.getOrder().equals(SearchOptions.ORDER_BY_DATE))
				{
					sort.setFieldName(Field.MODIFIED_DATE);
					sort.setType(Sort.LONG_TYPE);
					sort.setReverse(true);
				}
				else if(searchOptions.getOrder().equals(SearchOptions.ORDER_BY_VIEWS))
				{
					sort.setFieldName( String.format(Field.VISITS, scopeGroupId) );
					sort.setType(Sort.LONG_TYPE);
					sort.setReverse(true);
				}
				
				Hits hits = null;
				
				if(_log.isTraceEnabled())
					_log.trace( String.format("SOLR-TIME Composición de la consulta de búsqueda. Tiempo %s ms.", System.currentTimeMillis()-tIni));
				
				if(_log.isDebugEnabled())
				{
					_log.debug("fullQuery: " + fullQuery.toString());
					_log.debug("filterQuery: " + ((filterQuery!=null) ? filterQuery.toString() : "") );
				}
				
				int start = (cur - 1) * delta;
				int end = start + delta;
					
				SearchEngine se = IterSearchUtil.getSearchEngine();
				if(se!=null)
				{
					IndexSearcher searcher = se.getSearcher();
					if(searcher!=null)
					{
						if(_log.isTraceEnabled())
							tIni2 = System.currentTimeMillis();
						
						hits = searcher.search(searchOptions.getDelegationId(), fullQuery, new Sort[] {sort}, start, end, new String[]{Field.ENTRY_CLASS_PK}, facetList, filterQuery);
						
						if(_log.isTraceEnabled())
							_log.trace( String.format("SOLR-TIME Resultado de la búsqueda. Tiempo %s ms.", System.currentTimeMillis()-tIni2));
					}
					else
						_log.error("Searcher is null!!!");
				}
				else
				{
					_log.error("SearchEngine is null!!!");
					_log.debug("#0009819");
				}
		
				if(hits != null && hits.getDocs() != null && hits.getLength() > 0)
				{
					if(_log.isTraceEnabled())
						tIni2 = System.currentTimeMillis();
					
					getFacetedQueriesLabels(hits.getFacetedFiels());
					
					if(_log.isTraceEnabled())
					{
						_log.trace( String.format("SOLR-TIME Obtener los nombres de las segmentaciones. Tiempo %s ms.", System.currentTimeMillis()-tIni2));
						tIni2 = System.currentTimeMillis();
					}
					
					sr.setResults(hits);
					
					if(_log.isTraceEnabled())
					{
						_log.trace( String.format("SOLR-TIME Setear los resultados. Tiempo %s ms.", System.currentTimeMillis()-tIni2));
						tIni2 = System.currentTimeMillis();
					}
					
					sr.setTotal(hits.getLength());
					
					if(_log.isTraceEnabled())
					{
						_log.trace( String.format("SOLR-TIME Setear el numero de resultados. Tiempo %s ms.", System.currentTimeMillis()-tIni2));
						tIni2 = System.currentTimeMillis();
					}
				}
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return sr;
	}
	
	private static Map<String, Object> getFilterQuery(String filterQueryParam) throws ParseException, java.text.ParseException, UnsupportedEncodingException
	{
		Map<String, Object> retVal = new HashMap<String, Object>();
		BooleanQuery filterQuery = null;
		Map<String, String> filters = new HashMap<String, String>();
		
		filterQueryParam = new String(Base64.decodeBase64(filterQueryParam), StringPool.UTF8);
		_log.debug("filterQuery: " + filterQueryParam);
		
		try
		{
			if( Validator.isNotNull(filterQueryParam) && !filterQueryParam.equals("0") )
			{
				filterQuery = BooleanQueryFactoryUtil.create();
				String[] filterFields = filterQueryParam.split("\\"+StringPool.DOLLAR);
				
				for(String field : filterFields)
				{
					String[] fieldInfo = field.split(StringPool.COLON);
					String fieldName = fieldInfo[0];
					String fieldValues = fieldInfo[1];
					int numFields = fieldInfo.length;
					if(numFields>2)
						for(int idx=2;idx<numFields;idx++)
							fieldValues = fieldValues.concat(StringPool.COLON).concat(fieldInfo[idx]);
					
					if(fieldName.equalsIgnoreCase(Field.ITR_MODIFIED_DATE))
					{
						_log.debug("field name: " + fieldName);
						filters.put(Field.ITR_MODIFIED_DATE, fieldValues);
						
						SimpleDateFormat df_HH = new SimpleDateFormat(IterKeys.URL_PARAM_DATE_FORMAT_EXT_HH);
						SimpleDateFormat df_UTC = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_T_HH_MM_ss_Z);
						
						String[] ranges = fieldValues.split(StringPool.BACK_SLASH + StringPool.CARET);
						
						_log.debug("\t field values: " + fieldValues);
						
						for(String range : ranges)
						{
							_log.debug("range " + range);
							
							String[] rangeDate = range.split("TO");
							Date startDate = df_HH.parse(rangeDate[0]);
							Date endDate = df_HH.parse(rangeDate[1]);
							
							StringBuilder dateFilterStr =  new StringBuilder()
												.append( StringPool.OPEN_CURLY_BRACE) 
												.append( df_UTC.format( getDateFromURLParam(startDate, false) ) )
												.append( " TO " )
												.append( df_UTC.format( getDateFromURLParam(endDate, true) ) )
												.append( StringPool.CLOSE_CURLY_BRACE );
			
							_log.debug("dateFilter " + dateFilterStr);
							
							filterQuery.add(
									TermQueryFactoryUtil.create(Field.ITR_MODIFIED_DATE, dateFilterStr.toString()),
									BooleanClauseOccur.MUST);
							
						}
					}
					else
					{
						_log.debug("field name: " + fieldName);
						
						filters.put(fieldName, fieldValues);
						
						String regExpr = StringPool.BACK_SLASH + StringPool.CARET;
						String[] fVals = fieldValues.split(regExpr);
						StringBuilder valuesbuilder = new StringBuilder( StringPool.OPEN_PARENTHESIS );
						int idx = 0;
						for(String fVal : fVals)
						{
							if(idx>0) 
								valuesbuilder.append(" AND ");
							else
								idx++;
							
							if(fVal.endsWith(StringPool.STAR))
								valuesbuilder.append(fVal);
							else
								valuesbuilder.append(StringPool.QUOTE).append(fVal).append(StringPool.QUOTE);
						}
						valuesbuilder.append(StringPool.CLOSE_PARENTHESIS);
						
						_log.debug("\t field values: " + valuesbuilder.toString());
						
						filterQuery.add(
								TermQueryFactoryUtil.create( fieldName, valuesbuilder.toString()), 
								BooleanClauseOccur.MUST  );
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.error( "Error trying to get the filter query. Param: " + filterQueryParam + " " + e.toString());
			_log.trace(e);
		}
		
		retVal.put(SearchKeys.FILTER_QUERY, filterQuery);
		retVal.put(SearchKeys.FILTERS, filters);
		
		return retVal;
	}

	private static void getFacetedQueriesLabels(Map<String, FacetedField> facetedFiels)
	{
		/*
		 *  SELECT 'sectionName' FIELD, uuid_ id, EXTRACTVALUE(Layout.name, '/root/name[1]/text()') label FROM layout 
			WHERE uuid_ IN ('4ae1e346-cace-4392-ae4d-ea7af7c55f60', 'c53132fb-8064-4305-accd-fcf5809b01d8')
			UNION ALL
			SELECT 'metadata' FIELD, categoryId id, NAME label FROM assetcategory 
			WHERE categoryId IN ('61384', '61396')
		 */
		long tIni = 0L;
		if(_log.isTraceEnabled())
			tIni = Calendar.getInstance().getTimeInMillis();
		
		StringBuilder query = new StringBuilder();
		
		for(Entry<String, FacetedField> entry : facetedFiels.entrySet())
		{
			FacetedField ff = entry.getValue();
			if(ff instanceof FacetedQueryImpl)
			{
				FacetedQueryImpl facetQuery = (FacetedQueryImpl)ff;
				String getQuery = "";
				if(entry.getKey().equalsIgnoreCase(Field.SECTION_NAME))
					getQuery = String.format(GET_LAYOUTS_NAMES, Field.SECTION_NAME, StringUtil.merge(facetQuery.getKeys(), "','") );
				else if(entry.getKey().equalsIgnoreCase(Field.CATEGORY_NAME))
					getQuery = String.format(GET_METADATA_NAMES, Field.CATEGORY_NAME, StringUtil.merge(facetQuery.getKeys(), "','"));
				
				if(query.length()>0)
					query = query.append("\nUNION ALL\n");
				
				query = query.append(getQuery);
			}
		}
		
		if(query.length()>0)
		{
			List<Map<String, Object>> result = PortalLocalServiceUtil.executeQueryAsMap(query.toString());
			if(result!= null && result.size()>0)
			{
				for(Map<String, Object> resultMap : result)
				{
					FacetedField ff = facetedFiels.get( resultMap.get("field") );
					List<FacetGroup> fg = ff.getValues( String.valueOf(resultMap.get("id")) );
					((FacetGroupImpl)fg.get(0)).setLabel( String.valueOf(resultMap.get("label")) );
				}
			}
		}
		
		if(_log.isTraceEnabled())
			_log.trace( String.format("SOLR-TIME Obtener los nombres de las páginas y categorias de la segmentacion. Tiempo %s ms.", System.currentTimeMillis()-tIni));
	}

	private static void appendStructureQuery(boolean solr, SearchOptions searchOptions, BooleanQuery fullQuery) throws ParseException
	{
		if(solr)
		{
			if (searchOptions.isCheckArticle() || searchOptions.isCheckPoll())
			{
				StringBuffer structureQuery = new StringBuffer();
				
				if (searchOptions.isCheckArticle())
					structureQuery.append(IterKeys.STRUCTURE_ARTICLE);
	
				if (searchOptions.isCheckPoll())
				{
					if(structureQuery.length() > 0)
						structureQuery.append(" OR ");
					
					structureQuery.append(IterKeys.STRUCTURE_POLL);
				}
	
				structureQuery.insert(0, "(");
				structureQuery.append(")");
				
				fullQuery.add(TermQueryFactoryUtil.create(	IterKeys.STANDARD_ARTICLE_INDEX_STRUCTUREID, 
															structureQuery.toString()), 
															BooleanClauseOccur.MUST  );
			}
			else
			{
				fullQuery.add(TermQueryFactoryUtil.create(	IterKeys.STANDARD_ARTICLE_INDEX_STRUCTUREID, "(" + 
															IterKeys.STRUCTURE_ARTICLE 		+ " OR " +
															IterKeys.STRUCTURE_POLL 					+ ")"),
															BooleanClauseOccur.MUST	 );
			}
		}
		else
		{
			BooleanQuery structureIdsQuery = BooleanQueryFactoryUtil.create();
			if ( searchOptions.isCheckArticle() || searchOptions.isCheckPoll() )
			{
				if (searchOptions.isCheckArticle())
					structureIdsQuery.add(TermQueryFactoryUtil.create(IterKeys.STANDARD_ARTICLE_INDEX_STRUCTUREID, 
																	  IterKeys.STRUCTURE_ARTICLE), BooleanClauseOccur.SHOULD);
	
				if (searchOptions.isCheckPoll())
					structureIdsQuery.add(TermQueryFactoryUtil.create(IterKeys.STANDARD_ARTICLE_INDEX_STRUCTUREID, 
																	  IterKeys.STRUCTURE_POLL), BooleanClauseOccur.SHOULD);
			}
			else
			{
				structureIdsQuery.add(TermQueryFactoryUtil.create(IterKeys.STANDARD_ARTICLE_INDEX_STRUCTUREID, 
																  IterKeys.STRUCTURE_ARTICLE), BooleanClauseOccur.SHOULD);
				structureIdsQuery.add(TermQueryFactoryUtil.create(IterKeys.STANDARD_ARTICLE_INDEX_STRUCTUREID, 
																  IterKeys.STRUCTURE_POLL), BooleanClauseOccur.SHOULD);
			}
	
			fullQuery.add(structureIdsQuery, BooleanClauseOccur.MUST);
		}
	}
	
	public static String getSearchResultURL(HttpServletRequest request, ThemeDisplay themeDisplay, String layoutUUID)
	{
		String searchResultURL = "";
		
		if(themeDisplay != null)
		{
			String searchLayoutURL = PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SEARCH_FRIENDLY_URL);
			try
			{
				if(Validator.isNull(layoutUUID))
				{
					//Si el portlet no tiene preferencias se miran las preferencias de la comunidad
					Communities community = CommunitiesLocalServiceUtil.getCommunitiesByGroup(themeDisplay.getScopeGroupId());
					if (community != null && !community.getPublicSearchUrl().isEmpty())
						searchLayoutURL = community.getPublicSearchUrl();
				}
				else
				{
					//Si el portlet tiene preferencias
					String query = String.format(GET_LAYOUT_FRIENDLYURL, themeDisplay.getScopeGroupId(), layoutUUID);
					List<Object> results = PortalLocalServiceUtil.executeQueryAsList(query);
					if(results != null && results.size() == 1)
						searchLayoutURL = results.get(0).toString();
				}
			}
			catch(Exception e)
			{
				_log.warn("No search url configured for groupId " + themeDisplay.getScopeGroupId());
				_log.trace(e);
			}
	
			try
			{
				if(Validator.isNull(searchLayoutURL))
					searchLayoutURL = themeDisplay.getLayout().getFriendlyURL();
				
				String portalURL = IterURLUtil.getIterURLPrefix();
				
				searchResultURL = portalURL.concat( searchLayoutURL ).concat( PortalUtil.getSearchSeparator() );
				
				/*si en miscelánea se configura la URL para búsquedas terminando en /, habrá doble / al concatenar searchLayoutURL y PortalUtil.getSearchSeparator() */
				searchResultURL = searchResultURL.replaceAll(StringPool.SLASH + StringPool.SLASH, StringPool.SLASH);
				
				searchResultURL = SectionServersMgr.processMobileURL("", searchResultURL, themeDisplay.getScopeGroupId());
			}
			catch(Exception e)
			{
				_log.error(e.toString());
				_log.trace(e);
			}
		}
		
		return searchResultURL;
	}
	
	public static String getSearchResultBasicParams(String showFuzzyButton, String order)
	{
		return getSearchResultParams(null, showFuzzyButton, null, null, order, null, null, null, null, null, null, null, null);
	}
	
	public static String getSearchResultParams(String fuzzy, String fuzzyOption, String startDate, String endDate,
											   String order, String checkArticle, String checkPoll, String filterquery, String wildcard,
											   String categoryIds, String layoutsPlid, String delta, String cur)
	{
		StringBuffer searchResultParams = new StringBuffer();

		if(Validator.isNull(fuzzy))
			fuzzy = String.valueOf(false);
		
		if(Validator.isNull(fuzzyOption))
			fuzzyOption = String.valueOf(false);

		if(Validator.isNull(startDate))
		{
			DateFormat dateFormat = new SimpleDateFormat(IterKeys.URL_PARAM_DATE_FORMAT);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(new Date());
			calendar.add(Calendar.YEAR, -100);
			startDate = dateFormat.format(calendar.getTime());
		}
		
		if(Validator.isNull(endDate))
		{
			DateFormat dateFormat = new SimpleDateFormat(IterKeys.URL_PARAM_DATE_FORMAT);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(new Date());
			endDate = dateFormat.format(calendar.getTime());
		}
		
		if(Validator.isNull(order))
			order = SEARCH_ORDER_DEFAULT;
		
		if(Validator.isNull(checkArticle))
			checkArticle = String.valueOf(true);
		
		if(Validator.isNull(checkPoll))
			checkPoll = String.valueOf(true);
		
		if(Validator.isNull(layoutsPlid))
			layoutsPlid = "0";
		
		if(Validator.isNull(categoryIds))
			categoryIds = "0";
		
		if(Validator.isNull(delta))
			delta = "0";
		
		if(Validator.isNull(cur))
			cur = "1";
		
		if(Validator.isNull(filterquery))
			filterquery = "0";
		
		if(Validator.isNull(wildcard))
			wildcard = "0";
		
		searchResultParams.append(StringPool.SLASH + fuzzy + StringPool.SLASH + fuzzyOption + 
								  StringPool.SLASH + startDate +  StringPool.SLASH + endDate + 
								  StringPool.SLASH + order + 
								  StringPool.SLASH + checkArticle + StringPool.SLASH + checkPoll +
								  StringPool.SLASH + filterquery + StringPool.SLASH + wildcard +
								  StringPool.SLASH + "meta" +
								  StringPool.SLASH + categoryIds + StringPool.SLASH + layoutsPlid +
								  StringPool.SLASH + delta + StringPool.SLASH + cur);  

		return searchResultParams.toString();
	}
	
	public static boolean showFuzzyButton(String showFuzzyButtonPreference, long scopeGroupId)
	{
		boolean showFuzzyButton = false;
		
		try
		{
			if(Validator.isNull(showFuzzyButtonPreference))
			{
				Communities community = CommunitiesLocalServiceUtil.getCommunitiesByGroup(scopeGroupId);
				if (community != null)
				{
					//1- Preferencia de la comunidad
					showFuzzyButton = community.getFuzzySearch();
				}
				else
				{
					//2- Propiedad del portal-ext.properties	
					showFuzzyButton = Boolean.getBoolean(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_SEARCH_FUZZY));
				}
			}
			else
			{
				showFuzzyButton = Boolean.parseBoolean(showFuzzyButtonPreference);
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return showFuzzyButton;
	}
	
	public static Date getDateFromURLParam(Date value, boolean fin){

		Date resultDate = null;
		
		if(value != null)
		{
			try{
				Calendar paramCalendar = Calendar.getInstance();
				paramCalendar.setTime(value);
				if(fin)
				{
					paramCalendar.set(Calendar.HOUR_OF_DAY, 23);
					paramCalendar.set(Calendar.MINUTE, 59);
					paramCalendar.set(Calendar.SECOND, 59);
				}
				resultDate = paramCalendar.getTime();
			}catch (Exception err){
				_log.debug(err);
			}
		}
		return resultDate;
	}
	
	public static List<KeyValuePair> getVocabularies(String excludedVocabularies, long companyId, long groupId) throws SecurityException, NoSuchMethodException, DocumentException{
		
		List<KeyValuePair> list = new ArrayList<KeyValuePair>();
	
		String result = "<rs/>";
		String query = null;
		
		if (excludedVocabularies == ""){
			query =  String.format("SELECT vocabularyId id, name, (0 < (select count(*) FROM AssetCategory ac2 WHERE ac2.companyId = %s AND ac2.groupId = %s AND ac2.vocabularyId = id)) hasChildren \n" +
								   "FROM AssetVocabulary c\n" +
								   "WHERE companyId = %s AND groupId = %s ORDER BY name ASC, id ASC", companyId, groupId, companyId, groupId);
		}
		else{
			query =  String.format("SELECT vocabularyId id, name, (0 < (select count(*) FROM AssetCategory ac2 WHERE ac2.companyId = %s AND ac2.groupId = %s AND ac2.vocabularyId = id)) hasChildren \n" +
		   			   			   "FROM AssetVocabulary c\n" +
		                           "WHERE companyId = %s AND groupId = %s AND " +
		                           "vocabularyId NOT IN (%s) ORDER BY name ASC, id ASC", companyId, groupId, companyId, groupId, excludedVocabularies);
		}
			
		_log.trace(query);
		result = PortalLocalServiceUtil.executeQueryAsDom(query).asXML();
		
		com.liferay.portal.kernel.xml.Document xmlDoc = SAXReaderUtil.read(result);		
		
		String xPathQuery = "//rs/row";
		
		XPath xpathSelector = SAXReaderUtil.createXPath(xPathQuery);
		List<Node> nodes = xpathSelector.selectNodes(xmlDoc);
		Element elem = null;
		
		for(int a=0;a<nodes.size();a++){
			Node node = nodes.get(a);
			elem = (Element)node;
			KeyValuePair pair = new KeyValuePair();
			if (elem.attribute("id")!= null){
				String idTemp = elem.attribute("id").getValue();
				String name = elem.attribute("name").getValue();
				String hasChildren = elem.attribute("hasChildren").getValue();
				pair.setKey(idTemp);
				pair.setValue(hasChildren+name);
				list.add(pair);
			}
		}
		
		return list;
		
	}
	
	public static List<KeyValuePair> getSubLevel(String type, String id, String excludedCategoryIds, String companyId, String scopeGroupId, String groupId) throws SecurityException, NoSuchMethodException, DocumentException, ServiceError, PortalException, SystemException{
		
		List<KeyValuePair> list = new ArrayList<KeyValuePair>();
		
		String result = "<rs/>";
		String query = null;
		
		if(type.equals("topLevel")){
			String topClause = "";
			if (excludedCategoryIds == ""){
				topClause =  String.format("SELECT categoryId id, name, (0 < (select count(*) FROM AssetCategory ac2 WHERE ac2.companyId = %s AND ac2.groupId = %s AND ac2.parentCategoryId = id)) hasChildren \n" +							
										   "FROM AssetCategory \n" +
										   "WHERE companyId = %s AND groupId = %s AND " +
										   "parentCategoryId = 0 AND vocabularyId = %s ORDER BY name ASC, id ASC" , companyId, groupId, companyId, groupId, id);
			}
			else{
				topClause =  String.format("SELECT categoryId id, name, (0 < (select count(*) FROM AssetCategory ac2 WHERE ac2.companyId = %s AND ac2.groupId = %s AND ac2.parentCategoryId = id)) hasChildren \n" +							
						   				   "FROM AssetCategory\n" +
						   				   "WHERE companyId = %s AND groupId = %s AND " +
						   				   "parentCategoryId = 0 AND vocabularyId = %s AND " +
				                           "categoryId NOT IN (%s) ORDER BY name ASC, id ASC", companyId, groupId, companyId, groupId, id, excludedCategoryIds);
			}
			
			query =	topClause;
		}
		else{
			String noTopClause = "";
			if (excludedCategoryIds == ""){
				noTopClause =  String.format("SELECT categoryId id, name, (0 < (select count(*) FROM AssetCategory ac2 WHERE ac2.companyId = %s AND ac2.groupId = %s AND ac2.parentCategoryId = id)) hasChildren \n" +												
						   					 "FROM AssetCategory \n" +
						   					 "WHERE companyId = %s AND groupId = %s AND " +
						   					 "parentCategoryId = %s ORDER BY name ASC, id ASC" , companyId, groupId, companyId, groupId, id);
			}
			else{
				noTopClause =  String.format("SELECT categoryId id, name, (0 < (select count(*) FROM AssetCategory ac2 WHERE ac2.companyId = %s AND ac2.groupId = %s AND ac2.parentCategoryId = id)) hasChildren \n" +												
	   										 "FROM AssetCategory \n" +
	   										 "WHERE companyId = %s AND groupId = %s AND parentCategoryId = %s AND " +
	   										 "categoryId NOT IN (%s) ORDER BY name ASC, id ASC", companyId, groupId, companyId, groupId, id, excludedCategoryIds);
			}
			
			query =	noTopClause;
		}
			
		_log.trace(query);
		result = PortalLocalServiceUtil.executeQueryAsDom(query).asXML();
		com.liferay.portal.kernel.xml.Document xmlDoc = SAXReaderUtil.read(result);		
		String xPathQuery = "//rs/row";
		XPath xpathSelector = SAXReaderUtil.createXPath(xPathQuery);
		List<Node> nodes = xpathSelector.selectNodes(xmlDoc);
		Element elem = null;
		
		for(int a=0;a<nodes.size();a++){
			Node node = nodes.get(a);
			elem = (Element)node;
			KeyValuePair pair = new KeyValuePair();
			if (elem.attribute("id")!= null){
				String idTemp = elem.attribute("id").getValue();
				String name = elem.attribute("name").getValue();
				String hasChildren = elem.attribute("hasChildren").getValue();
				pair.setKey(idTemp);
				pair.setValue(hasChildren+name);
				list.add(pair);
			}
		}
	
		return list;
	}

	public static String getPreference(PortletPreferences preferences, String preferenceName)
	{
		
		String[] allValues = preferences.getValues(preferenceName, null);
		StringBuilder sb = new StringBuilder();
		if (allValues != null){
			for(String value : allValues){
				sb.append(sb.length() == 0 ? value : ","+value);
			}
		}
		if (sb.length() == 0)
			return "";
		
		return sb.toString();
	}
	
	public static boolean isInArray(String id, String[] ids)
	{
		boolean isIn = false;
		
		if(Validator.isNotNull(id) && Validator.isNotNull(ids))
		{
			for(String currentId:ids)
			{
				if(currentId != null && currentId.equals(id))
				{
					isIn = true;
					break;
				}
			}
		}
		
		return isIn;
	}
	
	public static List<Long> getLayoutsPidLong(String layouts)
	{
		List<Long> layoutsPlid = new ArrayList<Long>();

		if (Validator.isNotNull(layouts))
		{
			String[] tagTokens = layouts.split(StringPool.DASH);
			for(String token : tagTokens)
			{
				try
				{
					if(Long.valueOf(token) != 0)
						layoutsPlid.add(Long.valueOf(token));
				}
				catch(Exception e)
				{
					layoutsPlid = new ArrayList<Long>();
					
					if(layouts.contains(StringPool.PLUS))
						layouts = layouts.replaceAll("\\+", StringPool.SLASH);
					
					String query = String.format(GET_LAYOUT_FRIENDLYURL_CHECK, StringPool.SLASH + layouts);
					List<Object> layoutsList = PortalLocalServiceUtil.executeQueryAsList(query);
					if(layoutsList != null && layoutsList.size() > 0)
						layoutsPlid.add(Long.parseLong(layoutsList.get(0).toString()));
					else
						layoutsPlid.add(-1L);
				}
			}
		}

		return layoutsPlid;
	}
	
	public static List<String> getLayoutsPidLong(String[] layouts)
	{
		List<String> layoutsPlid = new ArrayList<String>();
		if(layouts != null && layouts.length > 0 && !layouts[0].equals("0"))
		{
			String query = String.format(GET_LAYOUT_UUID, StringUtils.join(layouts, "\",\""));
			List<Object> layoutsList = PortalLocalServiceUtil.executeQueryAsList(query);
			for(Object currentLayout:layoutsList)
				layoutsPlid.add(currentLayout.toString());
		}
		
		return layoutsPlid;
	}
	
	public static String getLayouts(RenderRequest request)
	{
		String layoutsPlid = ParamUtil.getString(request, "layoutsPlid", "0");
		return layoutsPlid.replaceAll(StringPool.SPACE, "\\+");
	}
	
	public static Date getDate(String dateString)
	{
		Date date = null;
		
		try
		{
			DateFormat df = new SimpleDateFormat(IterKeys.URL_PARAM_DATE_FORMAT);
			date = df.parse(dateString);
		}
		catch(Exception e){}
		
		return date;
	}
	
	public static String getJournalArticleId(Document journalArticle)
	{
		String id = null;
		
		try
		{
			id = journalArticle.get(Field.ENTRY_CLASS_PK);
			if(id.contains("[") && id.contains("]"))
			{
				id = id.substring(1, id.length() - 1);
			}
		}
		catch(Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return id;
	}
}
