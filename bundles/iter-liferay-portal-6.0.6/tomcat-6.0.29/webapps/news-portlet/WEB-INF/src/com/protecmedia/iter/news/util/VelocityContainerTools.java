package com.protecmedia.iter.news.util;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;

import com.liferay.portal.kernel.util.CategoriesUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.MethodKey;
import com.liferay.portal.kernel.util.PortalClassInvoker;
import com.liferay.portal.kernel.util.SectionUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.velocity.IterVelocityTools;
import com.liferay.portal.kernel.velocity.VelocityContext;
import com.liferay.portal.kernel.velocity.VelocityEngineUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;

public class VelocityContainerTools
{

	public static final String GET_PLID = "SELECT Layout.plid AS value \n";
	
	public static final String GET_PLID_AND_NAME = "SELECT Layout.plid AS value, Layout.name \n";
	
	public static final String GET_VALUE = "SELECT value \nFROM (\n%sUNION ALL \n%s)layouts \nORDER BY layouts.name ASC";
	
	public static final String GET_SECTION_PLIDS = new StringBuilder(
										"%s"								).append(
										"FROM Layout\n"						).append(
										"WHERE Layout.uuid_ IN ('%s') \n"	).toString();
	
	public static final String GET_SECTION_DESCENDENTS = new StringBuilder(
										"%s"																															).append(
										"FROM Layout\n"																													).append(
										"INNER JOIN Layout ParentLayout ON (Layout.parentLayoutId = ParentLayout.layoutId AND Layout.groupId = ParentLayout.groupId)\n"	).append(
										"WHERE ParentLayout.uuid_ IN ('%s') \n"																							).toString();
	
	public static final String ORDER_SECTIONS = "ORDER BY Layout.name ASC	\n";
	
	public static final String GET_METADATA_DESCENDENTS = new StringBuilder(
		"SELECT c.categoryId as value  		\n").append(
		"FROM AssetCategory c \n%s			\n").append(
		"WHERE true	 						\n").append(
		"%s  								\n").append(
		"GROUP BY c.name					\n").toString();

	public static final String GET_VOCABULARY_DESCENDENTS = new StringBuilder(
										"SELECT c.categoryId as value \n"							).append(
										"FROM AssetCategory c \n"									).append(
										"WHERE c.parentcategoryid=0 AND vocabularyid IN ('%s')	\n"	).toString();
	
	public static String[] getSectionPlids(String displayoptionsSection, String[] layoutIds)
	{
		String[] retVal = new String[]{};
		
		String sql = "";
		String idsLayout = StringUtil.merge(layoutIds, "','");
		
		if(displayoptionsSection.equalsIgnoreCase("descendent"))
			sql = String.format( GET_SECTION_DESCENDENTS, GET_PLID, idsLayout).concat(ORDER_SECTIONS);
		else if( displayoptionsSection.equalsIgnoreCase("selected") )
			sql = String.format( GET_SECTION_PLIDS, GET_PLID, idsLayout).concat(ORDER_SECTIONS);
		else
			sql = String.format(GET_VALUE, String.format(GET_SECTION_PLIDS, GET_PLID_AND_NAME, idsLayout), String.format(GET_SECTION_DESCENDENTS, GET_PLID_AND_NAME, idsLayout));
		
		List<Map<String, Object>> result = PortalLocalServiceUtil.executeQueryAsMap(sql);
		if(result!=null && result.size()>0)
			for(Map<String, Object> layout : result)
			{
				String layoutPlid = String.valueOf( layout.get("value") );
				retVal = (String[]) ArrayUtils.add(retVal, layoutPlid);
			}
		
		return retVal;
	}
	
	public static long[] getCategories(String displayoptionsMetadata, String[] contentCategoryIds, String[] contentVocabularyIds)
	{
		long[] retVal = new long[]{};
		
		if( displayoptionsMetadata.equalsIgnoreCase("descendent") || displayoptionsMetadata.equalsIgnoreCase("selected_descendent") )
		{
			List<String> categories = ListUtil.toList(contentCategoryIds);
			String whereClause = CategoriesUtil.getSelectedExcludedTopicsSQL(ListUtil.toList(contentVocabularyIds), Validator.isNotNull(categories),	null, null, true);

			String sql = String.format( GET_METADATA_DESCENDENTS, CategoriesUtil.getDiscreteCategoriesJoin(categories), whereClause );
			
			List<Map<String, Object>> result = PortalLocalServiceUtil.executeQueryAsMap(sql);
			
			if(result!=null && result.size()>0)
				for(Map<String, Object> categoriesMap : result)
				{
					long categoryId = Long.parseLong( String.valueOf( categoriesMap.get("value") ) );
					retVal = ArrayUtils.add(retVal, categoryId);
				}
		}
		
		if( displayoptionsMetadata.equalsIgnoreCase("selected") || displayoptionsMetadata.equalsIgnoreCase("selected_descendent") )
		{
			if( Validator.isNotNull(contentCategoryIds) )
				for(String categoryId : contentCategoryIds)
					retVal = ArrayUtils.add(retVal, Long.parseLong(categoryId) );
		}
		
		if(displayoptionsMetadata.equalsIgnoreCase("selected"))
		{
			if( Validator.isNotNull(contentVocabularyIds) )
			{
				String sql = String.format( GET_VOCABULARY_DESCENDENTS, StringUtil.merge(contentVocabularyIds, "','") );
				List<Map<String, Object>> result = PortalLocalServiceUtil.executeQueryAsMap(sql);
				if(result!=null && result.size()>0)
					for(Map<String, Object> categoriesMap : result)
					{
						long categoryId = Long.parseLong( String.valueOf( categoriesMap.get("value") ) );
						retVal = ArrayUtils.add(retVal, categoryId);
					}
			}
		}
		
		return retVal;
	}
	
	public static VelocityContext mergeVelocityTemplate(String velocityContent, HttpServletRequest request, String[] selectedLayoutsPlids, long[] selectedCategoriesIds, int categoryOperation ) throws IOException, Exception
	{
        VelocityContext velocityContext = VelocityEngineUtil.getEmptyContext();
        
        Object instrumentalObj = PortalClassInvoker.invoke(
									false, 
									new MethodKey("com.liferay.portal.util.InstrumentalContentUtil", "getNewInstance", String.class),
									IterVelocityTools.SECTION_MODE);
        
        velocityContext.put("aboutSectionArticles", instrumentalObj);
              
        //Inyección $aboutCategoryArticles
        Object instrumentalCatObj = PortalClassInvoker.invoke(
				false, 
				new MethodKey("com.liferay.portal.util.InstrumentalContentUtil", "getNewInstance", String.class),
				IterVelocityTools.CATEGORY_MODE);
        
        velocityContext.put("aboutCategoryArticles", instrumentalCatObj);
        
        //Inyección iterVelocityTools
        velocityContext.put(VelocityContext.ITER_VELOCITY_TOOLS, 	new IterVelocityTools(velocityContext));
        
        
        /*Inyección de variable: $categoryid. 
        	$categoryid = null, excepto si estamos en una página de "meta" o "resultados de búsqueda" y la búsqueda incluía metadatos. */
        long[] categoriesIds = selectedCategoriesIds;
        
        String urlType = SectionUtil.getURLType(request);
        if( categoryOperation!=WebKeys.LOGIC_IGNORE && (urlType.equals(SectionUtil.URLTYPE_META) || urlType.equals(SectionUtil.URLTYPE_SEARCH)))
        {
        	String urlCategories = CategoriesUtil.getURLCategories(request);
        	long[] urlMetas = null;
        	
        	if(urlType.equalsIgnoreCase(SectionUtil.URLTYPE_SEARCH))
        		urlMetas = StringUtil.split(urlCategories, StringPool.DASH, 0L);
        	else
			{
				List<Long> categoriesList = CategoriesUtil.getCategoriesIdsLong( urlCategories );
				if(categoriesList!=null && categoriesList.size()>0)
				{
					urlMetas = new long[categoriesList.size()];
					int i=0;
					for(Long cat : categoriesList)
					{
						urlMetas[i++] = cat;
					}
				}
			}
        	
        	switch (categoryOperation)
			{
				//Inclusiva: Se devuelven las categorías recuperas y las pasadas por parámetro
				case WebKeys.LOGIC_OR:
					categoriesIds = ArrayUtils.addAll(categoriesIds, urlMetas);
					break;
				//Exclusiva: Se devuelven las categorías recuperadas y se ignoran las pasadas por parámetro
				case WebKeys.LOGIC_AND:
					categoriesIds = urlMetas;
					break;
			}
        }
       
        velocityContext.put("categoryid", categoriesIds);
        
        /*Inyección de variable: $sectionsid.*/
        velocityContext.put("sectionsid", selectedLayoutsPlids);

        IterRequest.setAttribute(WebKeys.VELOCITY_CONTAINER_TRANSFORM, "1");
      
        StringWriter sw = new StringWriter();
        VelocityEngineUtil.mergeTemplate((new Random(10000)).toString(), velocityContent, velocityContext, sw);	
        
        String result = sw.toString();
        velocityContext.put(VelocityContext.TRANSFORM_RESULT, result);
        
		return velocityContext;
	}
	
	public static boolean isValidVelocityCode(boolean defaultLayout)
	{
		boolean codeCurrentSection = Validator.isNotNull( IterRequest.getAttribute(WebKeys.VELOCITY_CONTAINER_CODE_IS_USING_CURRENT_SECTION) );
        
		boolean isvalid = ( codeCurrentSection && defaultLayout ) || ( !codeCurrentSection && !defaultLayout );
        
        IterRequest.removeAttribute(WebKeys.VELOCITY_CONTAINER_TRANSFORM);
        IterRequest.removeAttribute(WebKeys.VELOCITY_CONTAINER_CODE_IS_USING_CURRENT_SECTION);
        
        return isvalid;
	}

}
