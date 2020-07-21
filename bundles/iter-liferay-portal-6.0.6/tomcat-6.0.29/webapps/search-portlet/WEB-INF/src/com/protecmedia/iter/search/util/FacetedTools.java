package com.protecmedia.iter.search.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.portlet.PortletPreferences;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.facet.Facet;
import com.liferay.portal.kernel.search.facet.FacetImpl;
import com.liferay.portal.kernel.search.facet.QueryFacetImpl;
import com.liferay.portal.kernel.search.facet.RangeFacetImpl;
import com.liferay.portal.kernel.util.CategoriesUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.PortletPreferencesLocalServiceUtil;
import com.liferay.portal.util.PortletKeys;
import com.liferay.util.xml.CDATAUtil;
import com.protecmedia.iter.base.service.util.IterKeys;

public class FacetedTools
{
	private static Log _log = LogFactoryUtil.getLog(FacetedTools.class);

	public static final String GET_SECTION_INFO = new StringBuilder(
								"SELECT IFNULL(ParentLayout.uuid_, 0) AS parent, Layout.uuid_ AS value, \n"				).append(
								"ExtractValue(Layout.NAME, '/root/name[1]/text()') AS name \n"							).append( 
								"FROM Layout\n"																			).append(
								"LEFT JOIN Layout ParentLayout ON (Layout.parentLayoutId = ParentLayout.layoutId )\n"	).append(
								"WHERE Layout.uuid_ IN ('%s') \n"														).append(
								"ORDER BY name ASC	\n"																	).toString();

	public static final String GET_SECTION_DESCENDENTS = new StringBuilder(
								"SELECT ParentLayout.uuid_ AS parent, Layout.uuid_ AS value, \n"																).append(
								"ExtractValue(Layout.NAME, '/root/name[1]/text()') AS name \n"																	).append( 
								"FROM Layout\n"																													).append(
								"INNER JOIN Layout ParentLayout ON (Layout.parentLayoutId = ParentLayout.layoutId AND Layout.groupId = ParentLayout.groupId)\n"	).append(
								"WHERE ParentLayout.uuid_ IN ('%s') \n"																							).append(
								"ORDER BY name ASC	\n"																											).toString();

	public static final String GET_SECTION_DESCENDENTS_WITH_EXCEPTIONS = new StringBuilder(
								"SELECT ParentLayout.uuid_ AS parent, Layout.uuid_ AS value, \n"																).append(
								"ExtractValue(Layout.NAME, '/root/name[1]/text()') AS name \n"																	).append( 
								"FROM Layout\n"																													).append(
								"INNER JOIN Layout ParentLayout ON (Layout.parentLayoutId = ParentLayout.layoutId AND Layout.groupId = ParentLayout.groupId)\n"	).append(
								"WHERE ParentLayout.uuid_ IN ('%s') \n"																							).append(
								"AND Layout.uuid_ NOT IN ('%s') \n"																								).append(
								"ORDER BY name ASC	\n"																											).toString();

	public static final String GET_METADATA_DESCENDENTS = new StringBuilder(
								"SELECT c.categoryId as value, c.parentCategoryId parent 		\n").append(
								"FROM AssetCategory c \n%s										\n").append(
								"WHERE true	 													\n").append(
								"%s  															\n").append(
								"GROUP BY c.name												\n").toString();
	
	public static final String GET_VOCABULARY_DESCENDENTS = new StringBuilder(
								"SELECT c.categoryId as value \n"								).append(
								"FROM AssetCategory c \n"										).append(
								"WHERE c.parentcategoryid=0 AND vocabularyid IN ('%s')	 	\n"	).toString();
	
	public static final String GET_METADATA_WITH_PARENT = new StringBuilder(
								"SELECT c.categoryId as value, c.parentCategoryId parent \n"	).append(
								"FROM AssetCategory c \n"										).append(
								"WHERE %s"														).toString();
	
	public static final String OR = " %s OR %s "; 
	public static final String WHERE_VOCABULAY_CLAUSE = " (c.parentcategoryid=0 AND vocabularyid IN ('%s')) ";
	public static final String WHERE_CATEGORY_CLAUSE = " categoryId IN ('%s') ";

	public static Map<String, Object> getFacets(String layoutTypeSettings, long LayoutPlid, Map<String, String> filtersParam) throws SystemException, java.text.ParseException, DocumentException
	{
		Map<String, Object> retVal = new HashMap<String, Object>(); 
		Map<String, Facet> facetsMap = new HashMap<String, Facet>();
		Map<String, FacetPreferences> portletFacets = new HashMap<String, FacetPreferences>();
		
		try
		{
			Pattern p = Pattern.compile( "facetedresultsportlet_WAR_[^,\\n]+", Pattern.CASE_INSENSITIVE );
			Matcher m = p.matcher(layoutTypeSettings);
			while(m.find())
			{
				String portletId = m.group(0);
				PortletPreferences preferences = PortletPreferencesLocalServiceUtil.getPreferences(IterGlobal.getCompanyId(), 
			            PortletKeys.PREFS_OWNER_ID_DEFAULT, PortletKeys.PREFS_OWNER_TYPE_LAYOUT, LayoutPlid, portletId);
				
				String segmentationType = preferences.getValue(SearchKeys.PREF_SEGMENTATIONTYPE, "");
				boolean autoSegmentation = GetterUtil.getBoolean( preferences.getValue(SearchKeys.PREF_AUTOMATICSEGMENTATION, "false"), false );
				String autoSegmentationLimit = preferences.getValue(SearchKeys.PREF_AUTOMATICSEGMENTATIONLIMIT, "0");

				FacetPreferences facetPortletConfig = null;
				if (segmentationType.equalsIgnoreCase(SearchKeys.SEGMENTATION_BY_DATE))
					facetPortletConfig = getRangeSegmentation(autoSegmentation, autoSegmentationLimit, preferences, facetsMap, filtersParam);
				else
				{
					if(segmentationType.equalsIgnoreCase(Field.SECTION_NAME))
							facetPortletConfig = getSectionSegmentation(autoSegmentation, autoSegmentationLimit, preferences, facetsMap, filtersParam);
					else
						if(segmentationType.equalsIgnoreCase(Field.CATEGORY_NAME))
							facetPortletConfig = getCategorySegmentation(autoSegmentation, autoSegmentationLimit, preferences, facetsMap, filtersParam);
				}
				
				if(facetPortletConfig!=null)
				{
					facetPortletConfig.set_autoSegmentation(autoSegmentation);
					facetPortletConfig.setAutoSegmentationLimit( autoSegmentationLimit );
					facetPortletConfig.setColumns( 
							GetterUtil.getInteger( preferences.getValue(SearchKeys.PREF_NUMCOLUMNS, ""), 1) 
						);
					facetPortletConfig.showEmpty( 
									GetterUtil.getBoolean(preferences.getValue(SearchKeys.PREF_SHOWEMPTY, "false"), false) 
								);
					facetPortletConfig.setPrefix( preferences.getValue(SearchKeys.PREF_PREFIXWRAP, "") );
					facetPortletConfig.setSufix( preferences.getValue(SearchKeys.PREF_POSTWRAP, "") );
					facetPortletConfig.setTitle( preferences.getValue(SearchKeys.PREF_TITLEFACETED, "") );
					
					portletFacets.put(portletId, facetPortletConfig);
				}
			}
		}
		catch (Exception e)
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		retVal.put(SearchKeys.PORTLET_FACETS, portletFacets);
		retVal.put(SearchKeys.FACETS_LIST, new ArrayList<Facet>(facetsMap.values()));
		
		return retVal;
	}
	
	private static FacetPreferences getSectionSegmentation(boolean autoSegmentation, String autoSegmentationLimit, PortletPreferences preferences, Map<String, Facet> facetsMap, Map<String, String> filtersParam)
	{
		FacetImpl f = null;
		Set<String> facetKeys = null;
		Set<String> facetValues = null;
		FacetPreferences portletFacets = null;

		portletFacets = new FacetPreferences(Field.SECTION_NAME);
		
		if(autoSegmentation)
		{
			String filterValue = filtersParam.get(Field.SECTION_NAME);
			if( Validator.isNotNull(filterValue) )
			{
				String[] sections = filterValue.split(StringPool.BACK_SLASH + StringPool.CARET);
				String lastSection = "";
				
				int segmentLimit = 0;
				
				try
				{
					segmentLimit = Integer.valueOf(autoSegmentationLimit).intValue();
				}
				catch (NumberFormatException e) {}
				
				if( segmentLimit==0 || sections.length<segmentLimit )
					lastSection = sections[sections.length-1];
				else
					lastSection = sections[segmentLimit-1];
				
				String prefix = getPrefix(Field.SECTION_NAME, lastSection, null);
				
				f = new FacetImpl(Field.SECTION_NAME, prefix);
				f.autoFaceting(true);
				facetsMap.put(WebKeys.PREFIX_AUTO+Field.SECTION_NAME, f);
			}
			else
			{
				if(facetsMap.size() > 0)
					f = (FacetImpl) facetsMap.get(WebKeys.PREFIX_AUTO+Field.SECTION_NAME);
				
				if(f==null)
				{
					f = new FacetImpl(Field.SECTION_NAME);
					f.autoFaceting(true);
					facetsMap.put(WebKeys.PREFIX_AUTO+Field.SECTION_NAME, f);
					facetKeys = new HashSet<String>();
					facetValues = new HashSet<String>();
				}
				else
				{
					facetKeys = ((FacetImpl)f).getFacetKeys();
					facetValues = ((FacetImpl)f).getFacetValues();
				}
				
				getSectionSegmentationData(f, facetKeys, facetValues, portletFacets, preferences);
			}
		}
		else
		{
			if(facetsMap.size() > 0)
				f = (FacetImpl) facetsMap.get(Field.SECTION_NAME);
			
			if(f==null)
			{
				f = new FacetImpl(Field.SECTION_NAME);
				facetsMap.put(Field.SECTION_NAME, f);
				facetValues = new HashSet<String>();
			}
			else
				facetValues = ((FacetImpl)f).getFacetValues();
			
			getSectionSegmentationData(f, facetKeys, facetValues, portletFacets, preferences);
		}
		
		int minArticle = GetterUtil.getInteger( preferences.getValue(SearchKeys.PREF_NUMMINARTICLE, ""), 1);
		f.setMinArticle(minArticle);
		
		portletFacets.setMinArticle(minArticle);
		portletFacets.setColumns( GetterUtil.getInteger( preferences.getValue(SearchKeys.PREF_NUMCOLUMNS, ""), 1) );
		
		return portletFacets;
	}
	
	private static void getSectionSegmentationData(Facet f, Set<String> facetKeys, Set<String> facetValues, FacetPreferences portletFacets, PortletPreferences preferences)
	{
		String displayoption = preferences.getValue(SearchKeys.PREF_DISPLAYOPTIONSSECTION, SearchKeys.DISPLAY_SELECTED);
		String[] layouts = preferences.getValues(SearchKeys.PREF_LAYOUTIDS, null);
		
		if( displayoption.equalsIgnoreCase(SearchKeys.DISPLAY_DESCENDENTS) )
		{
			String sql = "";
			String[] excludedLayouts = preferences.getValues(SearchKeys.PREF_EXCLUDELAYOUTSIDS, null);
			
			if( Validator.isNotNull(excludedLayouts) )
				sql = String.format( GET_SECTION_DESCENDENTS_WITH_EXCEPTIONS, StringUtil.merge(layouts, "','"), StringUtil.merge(excludedLayouts, "','"));
			else
				sql = String.format( GET_SECTION_DESCENDENTS, StringUtil.merge(layouts, "','"));
			
			List<Map<String, Object>> result = PortalLocalServiceUtil.executeQueryAsMap(sql);
			
			if(result!=null && result.size()>0)
				for(Map<String, Object> layout : result)
				{
					String parentUUID = String.valueOf( layout.get("parent") );
					String layoutUUID = String.valueOf( layout.get("value") );
					portletFacets.addItem(layoutUUID);
					if(f instanceof FacetImpl)
					{
						facetValues.add( parentUUID + StringPool.SECTION + layoutUUID );
						if(((FacetImpl) f).autoFaceting())
							facetKeys.add( parentUUID + StringPool.SECTION );
					}
					else if (f instanceof QueryFacetImpl)
						facetValues.add( StringPool.STAR + layoutUUID + StringPool.STAR );
				}
		}
		else if( displayoption.equalsIgnoreCase(SearchKeys.DISPLAY_SELECTED) )
		{
			if( Validator.isNotNull(layouts) )
			{
				if(f instanceof FacetImpl)
				{
					String sql = String.format( GET_SECTION_INFO, StringUtil.merge(layouts, "','"));
					List<Map<String, Object>> result = PortalLocalServiceUtil.executeQueryAsMap(sql);
					
					if(result!=null && result.size()>0)
						for(Map<String, Object> layout : result)
						{
							String parentUUID = String.valueOf( layout.get("parent") );
							String layoutUUID = String.valueOf( layout.get("value") );
							portletFacets.addItem(layoutUUID);
							facetValues.add( parentUUID + StringPool.SECTION + layoutUUID );
							if(((FacetImpl) f).autoFaceting())
								facetKeys.add( parentUUID + StringPool.SECTION );
						}
				}
				else if (f instanceof QueryFacetImpl)
					for(String layout : layouts)
					{
						portletFacets.addItem(layout);
						facetValues.add( StringPool.STAR + layout + StringPool.STAR );
					}
			}
		}
		
		if(f instanceof FacetImpl)
			((FacetImpl) f).setFacetKeys(facetKeys);
		
		f.setFacetValues(facetValues);
	}

	private static FacetPreferences getCategorySegmentation(boolean autoSegmentation, String autoSegmentationLimit, PortletPreferences preferences, Map<String, Facet> facetsMap, Map<String, String> filtersParam)
	{
		FacetImpl f = null;
		Set<String> facetValues = null;
		Set<String> facetKeys = null;
		FacetPreferences portletFacets = null;
		
		portletFacets = new FacetPreferences(Field.CATEGORY_NAME);

		if(autoSegmentation)
		{
			String filterValue = filtersParam.get(Field.CATEGORY_NAME);
			if( Validator.isNotNull(filterValue) )
			{
				String[] categories = filterValue.split(StringPool.BACK_SLASH + StringPool.CARET);
				String lastCategory = "";
				
				int segmentLimit = 0;
				
				try
				{
					segmentLimit = Integer.valueOf(autoSegmentationLimit).intValue();
				}
				catch (NumberFormatException e) {}
				
				if( segmentLimit==0 || categories.length<segmentLimit )
					lastCategory = categories[categories.length-1];
				else
					lastCategory = categories[segmentLimit-1];
				
				String prefix = getPrefix(Field.CATEGORY_NAME, lastCategory, preferences.getValues(SearchKeys.PREF_EXCLUDECATEGORYIDS, null));
				
				f = new FacetImpl(Field.CATEGORY_NAME, prefix);
				f.autoFaceting(true);
				facetsMap.put(WebKeys.PREFIX_AUTO+Field.CATEGORY_NAME, f);
			}
			else
			{
				if(facetsMap.size() > 0)
					f = (FacetImpl) facetsMap.get(WebKeys.PREFIX_AUTO+Field.CATEGORY_NAME);
				
				if(f==null)
				{
					f = new FacetImpl(Field.CATEGORY_NAME);
					f.autoFaceting(true);
					facetsMap.put(WebKeys.PREFIX_AUTO+Field.CATEGORY_NAME, f);
					facetKeys = new HashSet<String>();
					facetValues = new HashSet<String>();
				}
				else
				{
					facetKeys = f.getFacetKeys();
					facetValues = f.getFacetValues();
				}
				
				getCategorySegmentationData(f, facetKeys, facetValues, portletFacets, preferences);
			}
		}
		else
		{
			if(facetsMap.size() > 0)
				f = (FacetImpl) facetsMap.get(Field.CATEGORY_NAME);

			if(f==null)
			{
				f = new FacetImpl(Field.CATEGORY_NAME);
				facetsMap.put(Field.CATEGORY_NAME, f);
				facetValues = new HashSet<String>();
			}
			else
				facetValues = ((FacetImpl)f).getFacetValues();
			
			getCategorySegmentationData(f, facetKeys, facetValues, portletFacets, preferences);
		}
		
		int minArticle = GetterUtil.getInteger( preferences.getValue(SearchKeys.PREF_NUMMINARTICLE, ""), 1);
		f.setMinArticle(minArticle);
		
		portletFacets.setMinArticle(minArticle);
		portletFacets.setColumns( GetterUtil.getInteger( preferences.getValue(SearchKeys.PREF_NUMCOLUMNS, ""), 1) );
		
		return portletFacets;
	}
	
	private static void getCategorySegmentationData(Facet f, Set<String> facetKeys, Set<String> facetValues, FacetPreferences portletFacets, PortletPreferences preferences)
	{
		String displayoption = preferences.getValue(SearchKeys.PREF_DISPLAYOPTIONSMETADATA, SearchKeys.DISPLAY_SELECTED);
		String[] categories = preferences.getValues(SearchKeys.PREF_CONTENTCATEGORYIDS, null);
		String[] vocabularies = preferences.getValues(SearchKeys.PREF_CONTENTVOCABULARYIDS, null);
		
		if( displayoption.equalsIgnoreCase(SearchKeys.DISPLAY_DESCENDENTS) )
		{
			String[] excludedCategories = preferences.getValues(SearchKeys.PREF_EXCLUDECATEGORYIDS, null);
			
			List<String> categoriesList = ListUtil.toList(categories);
			String whereClause = CategoriesUtil.getSelectedExcludedTopicsSQL(
										ListUtil.toList(vocabularies), Validator.isNotNull(categoriesList), 
										ListUtil.toList(excludedCategories), null, true, true);
			
			String sql = String.format(GET_METADATA_DESCENDENTS, CategoriesUtil.getDiscreteCategoriesJoin(categoriesList), whereClause);
			
			List<Map<String, Object>> result = PortalLocalServiceUtil.executeQueryAsMap(sql);
			
			if(result!=null && result.size()>0)
				for(Map<String, Object> categoriesMap : result)
				{
					String categoryId = String.valueOf( categoriesMap.get("value") );
					String parentCategoryId = String.valueOf( categoriesMap.get("parent") );
					portletFacets.addItem(categoryId);
					if(f instanceof FacetImpl)
					{
						facetValues.add( parentCategoryId + StringPool.SECTION + categoryId);
						if(((FacetImpl) f).autoFaceting())
							facetKeys.add( parentCategoryId + StringPool.SECTION );
					}
					else if (f instanceof QueryFacetImpl)
						facetValues.add( StringPool.STAR + categoryId + StringPool.STAR );
				}
		}
		else if( displayoption.equalsIgnoreCase(SearchKeys.DISPLAY_SELECTED) )
		{
			if(f instanceof QueryFacetImpl)
			{
				if( Validator.isNotNull(categories) )
				{
					for(String categoryId : categories)
					{
						portletFacets.addItem(categoryId);
						facetValues.add( StringPool.STAR + categoryId + StringPool.STAR );
					}
				}
				
				if( Validator.isNotNull(vocabularies) )
				{
					String sql = String.format( GET_VOCABULARY_DESCENDENTS, StringUtil.merge(vocabularies, "','") );
					List<Map<String, Object>> result = PortalLocalServiceUtil.executeQueryAsMap(sql);
					if(result!=null && result.size()>0)
						for(Map<String, Object> categoriesMap : result)
						{
							String categoryId = String.valueOf( categoriesMap.get("value") );
							portletFacets.addItem(categoryId);
							facetValues.add( StringPool.STAR + categoryId + StringPool.STAR );
						}
				}
			}
			else if(f instanceof FacetImpl)
			{
				String whereClause = "";
				String categoriesClause = "";
				String vocabulariesClause = "";
				
				if(Validator.isNotNull(categories))
					categoriesClause = String.format( WHERE_CATEGORY_CLAUSE, StringUtil.merge(categories, "','") );
				
				if(Validator.isNotNull(vocabularies))
					vocabulariesClause = String.format( WHERE_VOCABULAY_CLAUSE, StringUtil.merge(vocabularies, "','") );
				
				if( categoriesClause!="" && vocabulariesClause!="" )
					whereClause = String.format( OR, categoriesClause, vocabulariesClause );
				else
					whereClause = categoriesClause + vocabulariesClause;
				
				if(Validator.isNotNull(whereClause))
				{
					String sql = String.format(GET_METADATA_WITH_PARENT, whereClause);
					List<Map<String, Object>> result = PortalLocalServiceUtil.executeQueryAsMap(sql);
					if(result!=null && result.size()>0)
						for(Map<String, Object> categoriesMap : result)
						{
							String categoryId = String.valueOf( categoriesMap.get("value") );
							String parentCategoryId = String.valueOf( categoriesMap.get("parent") );
							portletFacets.addItem(categoryId);
							facetValues.add( parentCategoryId + StringPool.SECTION + categoryId);
							if(((FacetImpl) f).autoFaceting())
								facetKeys.add( parentCategoryId + StringPool.SECTION );
						}
				}
			}
		}

		if(f instanceof FacetImpl)
			((FacetImpl) f).setFacetKeys(facetKeys);
		
		f.setFacetValues(facetValues);
	}
	
	private static FacetPreferences getRangeSegmentation(boolean autoSegmentation, String autoSegmentationLimit, PortletPreferences preferences, Map<String, Facet> facetsMap, Map<String, String> filtersParam) throws DocumentException, ParseException
	{
		FacetPreferences portletFacets = new FacetPreferences(Field.ITR_MODIFIED_DATE);
		
		int minArticle = GetterUtil.getInteger( preferences.getValue("numminarticle", ""), 1);
		portletFacets.setMinArticle(minArticle);
		
		portletFacets.setDateformat( preferences.getValue("dateformat", IterKeys.DATEFORMAT_YYYY_MM_DD) );
		portletFacets.setDatelanguage( preferences.getValue("datelanguage", "en") );
		
		if(autoSegmentation)
		{
			String filterValue = filtersParam.get(Field.ITR_MODIFIED_DATE);
			if( Validator.isNotNull(filterValue) )
			{
				String[] ranges = filterValue.split(StringPool.BACK_SLASH + StringPool.CARET);
				String lastRange = ranges[ranges.length-1];
				
				Map<String, String> limits = getLimits(lastRange, autoSegmentationLimit); 
				
				if( limits==null  )
				{
					if((ranges.length-2>=0))
					{
						lastRange = ranges[ranges.length-2];
						limits = getLimits(lastRange, autoSegmentationLimit);
					}
					else
						getRangeSegmentationData(portletFacets, preferences, facetsMap, minArticle);
				}
				
				if(limits!=null)
				{
					String gap = StringPool.PLUS + "1" + limits.get("gapUnit");
					String id = PortalUUIDUtil.generate();
					Facet f = new RangeFacetImpl(id, Field.ITR_MODIFIED_DATE, limits.get("dateIni"), limits.get("dateFin"), gap, minArticle);
					String key = autoSegmentation ? WebKeys.PREFIX_AUTO.concat(id) : id;
					facetsMap.put(key, f);
					
					portletFacets.setRangePreferences(id, StringPool.BLANK, limits.get("gapUnit"), 1);
				}
			}
			else
				getRangeSegmentationData(portletFacets, preferences, facetsMap, minArticle);
		}
		else
			getRangeSegmentationData(portletFacets, preferences, facetsMap, minArticle);
		
		return portletFacets;
	}
	
	private static void getRangeSegmentationData(FacetPreferences portletFacets, PortletPreferences preferences, Map<String, Facet> facetsMap, int minArticle) throws DocumentException
	{
		String dateList = preferences.getValue("datelist", "");
		dateList = CDATAUtil.strip(dateList);
		com.liferay.portal.kernel.xml.Document d = SAXReaderUtil.read(dateList);
		
		List<Node> rows = d.selectNodes("/dates/row");
		for(Node row : rows)
		{
			String id = XMLHelper.getStringValueOf(row, "@id");
			
			String startUnit = XMLHelper.getStringValueOf(row, "startunit");
			int startValue = GetterUtil.getInteger( XMLHelper.getStringValueOf(row, "startvalue") );
			
			String gapUnit = XMLHelper.getStringValueOf(row, "gapunit");
			int gapValue = GetterUtil.getInteger( XMLHelper.getStringValueOf(row, "gapvalue") );
			
			String label = (startValue==gapValue && startUnit.equalsIgnoreCase(gapUnit)) ? XMLHelper.getStringValueOf(row, "label") : StringPool.BLANK;
			
			if(gapUnit.equalsIgnoreCase(IterKeys.CALENDAR_UNIT_WEEK ))
			{
				gapValue = gapValue * 7;
				gapUnit = IterKeys.CALENDAR_UNIT_DAY;
			}
			
			String gap = StringPool.PLUS+gapValue+gapUnit.toUpperCase();
			
			Map<String, String> limits = getRangeLimits(startUnit, startValue, gapUnit);
			
			Facet f = new RangeFacetImpl(id, Field.ITR_MODIFIED_DATE, limits.get("dateIni"), limits.get("dateFin"), gap, minArticle);
			facetsMap.put(id, f);
			
			portletFacets.setRangePreferences(id, label, gapUnit, gapValue);
		}
	}
	
	private static Map<String, String> getLimits(String lastRange, String autoSegmentationLimit) throws ParseException
	{
		Map<String, String> limits = null;
		
		DateFormat urlDateFormatExt = new SimpleDateFormat(IterKeys.URL_PARAM_DATE_FORMAT_EXT_HH);
		String[] range = lastRange.split("TO");
		Date ini = urlDateFormatExt.parse( range[0] );
		Date fin = urlDateFormatExt.parse( range[1] );
		Calendar cIni = Calendar.getInstance();
		Calendar cFin = Calendar.getInstance();
		cIni.setTime(ini);
		cFin.setTime(fin);
		String gapUnit = "";
		
		if( cIni.get(Calendar.YEAR) != cFin.get(Calendar.YEAR) )
		{
			gapUnit = IterKeys.CALENDAR_UNIT_YEAR.toUpperCase();
			limits = getAutoRangeLimits(gapUnit, Calendar.YEAR, cIni, cFin);
		}
		else if( cIni.get(Calendar.MONTH) != cFin.get(Calendar.MONTH) )
		{
			if( !autoSegmentationLimit.equalsIgnoreCase(IterKeys.CALENDAR_UNIT_YEAR) )
			{
				gapUnit = IterKeys.CALENDAR_UNIT_MONTH.toUpperCase();
				limits = getAutoRangeLimits(gapUnit, Calendar.MONTH, cIni, cFin);
			}
		}
		else if( cIni.get(Calendar.DAY_OF_MONTH) != cFin.get(Calendar.DAY_OF_MONTH) )
		{
			if( !autoSegmentationLimit.equalsIgnoreCase(IterKeys.CALENDAR_UNIT_YEAR) &&
				!autoSegmentationLimit.equalsIgnoreCase(IterKeys.CALENDAR_UNIT_MONTH))
			{
				gapUnit = IterKeys.CALENDAR_UNIT_DAY.toUpperCase();
				limits = getAutoRangeLimits(gapUnit, Calendar.DAY_OF_MONTH, cIni, cFin);
			}
		}
		
		return limits;
	}
	
	private static Map<String, String> getAutoRangeLimits(String gapUnit, int seg, Calendar rangeStart, Calendar rangeEnd)
	{
		Map<String, String> limits = new HashMap<String, String>();
		
		Calendar cal = Calendar.getInstance();
		
		StringBuilder startTime = new StringBuilder("NOW");
		StringBuilder endTime = new StringBuilder("NOW");
		
		int startYearsDiff = cal.get(Calendar.YEAR) - rangeStart.get(Calendar.YEAR);
		int endYearsDiff = cal.get(Calendar.YEAR) - rangeEnd.get(Calendar.YEAR);
		
		if( startYearsDiff > 0 )
			startTime.append(StringPool.MINUS).append(startYearsDiff).append(IterKeys.CALENDAR_UNIT_YEAR.toUpperCase());
		
		if( endYearsDiff > 0 )
			endTime.append(StringPool.MINUS).append(endYearsDiff).append(IterKeys.CALENDAR_UNIT_YEAR.toUpperCase());
		
		int startMonthDiff = cal.get(Calendar.MONTH) - rangeStart.get(Calendar.MONTH);
		int endMonthDiff = cal.get(Calendar.MONTH) - rangeEnd.get(Calendar.MONTH);
		
		if( startMonthDiff != 0 )
		{
			if(startMonthDiff > 0)
				startTime.append(StringPool.MINUS);
			else
				startTime.append(StringPool.PLUS);
			
			startTime.append( Math.abs(startMonthDiff) ).append(IterKeys.CALENDAR_UNIT_MONTH.toUpperCase());
		}
		
		if( endMonthDiff != 0 )
		{
			if(endMonthDiff > 0)
				endTime.append(StringPool.MINUS);
			else
				endTime.append(StringPool.PLUS);
			
			endTime.append( Math.abs(endMonthDiff) ).append(IterKeys.CALENDAR_UNIT_MONTH.toUpperCase());
		}
		
		int startDayDiff = cal.get(Calendar.DAY_OF_MONTH) - rangeStart.get(Calendar.DAY_OF_MONTH);
		int endDayDiff = cal.get(Calendar.DAY_OF_MONTH) - rangeEnd.get(Calendar.DAY_OF_MONTH);
		
		if( startDayDiff != 0 )
		{
			if(startDayDiff > 0)
				startTime.append(StringPool.MINUS);
			else
				startTime.append(StringPool.PLUS);
			
			startTime.append( Math.abs(startDayDiff) ).append(IterKeys.CALENDAR_UNIT_DAY.toUpperCase());
		}
		
		if( endDayDiff != 0 )
		{
			if(endDayDiff > 0)
				endTime.append(StringPool.MINUS);
			else
				endTime.append(StringPool.PLUS);
			
			endTime.append( Math.abs(endDayDiff) ).append(IterKeys.CALENDAR_UNIT_DAY.toUpperCase());
		}
		
		String dateIni = startTime.append(StringPool.SLASH).append(gapUnit).toString();
		String dateFin = endTime.append(StringPool.SLASH).append(gapUnit).toString();
		
		limits.put("dateIni", dateIni);
		limits.put("dateFin", dateFin);
		limits.put("gapUnit", gapUnit);
		
		return limits;
	}

	private static Map<String, String> getRangeLimits(String startUnit, int startValue, String gapUnit)
	{
		Map<String, String> limits = new HashMap<String, String>();
		
		String startTime = "NOW";
		String endTime = "NOW";
		
		if(startUnit.equalsIgnoreCase(IterKeys.CALENDAR_UNIT_WEEK))
		{
			//Obtenemos el primer dia de la semana anterior.
			Calendar ini = Calendar.getInstance();
			int i = ini.get(Calendar.DAY_OF_WEEK) - ini.getFirstDayOfWeek();
			startTime += -i-7 + "DAY";
			startValue = startValue * 7;
			startUnit = IterKeys.CALENDAR_UNIT_DAY;
		}
		
		String roundTo = startUnit.equalsIgnoreCase(gapUnit) ? gapUnit.toUpperCase() : startUnit.toUpperCase();
		
		if( startValue>1 )
		{
			// Queremos que el (año|mes|dia|hora) actual se incluya en los resultados
			startTime += "+1" + startUnit.toUpperCase();
			endTime += "+1" + startUnit.toUpperCase();
		}
		else if( !startUnit.equalsIgnoreCase(gapUnit) )
		{
			startTime += "+1" + gapUnit.toUpperCase();
			endTime += "+1" + gapUnit.toUpperCase();
		}
		
		String dateIni = startTime + "/" + roundTo + "-" + startValue + startUnit.toUpperCase();
		String dateFin = endTime + "/" + roundTo;
		
		limits.put("dateIni", dateIni);
		limits.put("dateFin", dateFin);
		
		return limits;
	}
	
	private static String getPrefix( String field, String lastSelected, String[] excludedCategories )
	{
		String prefix = "";
		
		int sectionIdx = lastSelected.indexOf(StringPool.SECTION);
		String parentSection = "";
		if(sectionIdx!=-1)
		{
			parentSection = lastSelected.substring( 0, sectionIdx );
			lastSelected = lastSelected.substring( sectionIdx+1 );
		}

		if(lastSelected.startsWith(StringPool.STAR))
			lastSelected = lastSelected.substring(1);
		if(lastSelected.endsWith(StringPool.STAR))
			lastSelected = lastSelected.substring(0, lastSelected.length() - 1);
		
		if(field.equalsIgnoreCase(Field.CATEGORY_NAME))
			prefix = getCategoryPrefix(lastSelected, parentSection, excludedCategories);
		else if(field.equalsIgnoreCase(Field.SECTION_NAME))
			prefix = getSectionPrefix(lastSelected, parentSection);
		
		return prefix;
	}
	
	private static String getCategoryPrefix(String lastSelected, String parentSection, String[] excludedCategories )
	{
		String prefix = lastSelected + StringPool.SECTION;
		
		if( !lastSelected.equalsIgnoreCase("0") )
		{
			String catId = "";
			int sectionIdx = lastSelected.indexOf(StringPool.SECTION);
			if(sectionIdx!=-1)
				catId = lastSelected.substring(0, sectionIdx);
			else
				catId = lastSelected;
			
			List<String> categories = ListUtil.fromString(catId);
			String whereClause = CategoriesUtil.getSelectedExcludedTopicsSQL(null, Validator.isNotNull(categories), 
																			ListUtil.toList(excludedCategories), null, true);
			
			String sql = String.format(GET_METADATA_DESCENDENTS, CategoriesUtil.getDiscreteCategoriesJoin(categories), whereClause);
			List<Map<String, Object>> result = PortalLocalServiceUtil.executeQueryAsMap(sql);
			
			if (result != null && result.size() == 0)
				prefix = parentSection;
		}
		
		return prefix;
	}
	
	private static String getSectionPrefix( String lastSelected, String parentSection )
	{
		String prefix = lastSelected;
		
		if( !lastSelected.equalsIgnoreCase("0") )
		{
			String sql = String.format( GET_SECTION_DESCENDENTS, lastSelected );
			List<Map<String, Object>> result = PortalLocalServiceUtil.executeQueryAsMap(sql);
			if(result!=null && result.size()==0)
				prefix = parentSection;
		}
		
		return prefix + StringPool.SECTION; 
	}
}
