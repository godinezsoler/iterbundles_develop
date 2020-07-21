package com.protecmedia.iter.news.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.portlet.PortletPreferences;
import javax.portlet.RenderRequest;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.CategoriesUtil;
import com.liferay.portal.kernel.util.IterURLUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.designer.model.PageTemplate;
import com.protecmedia.iter.designer.service.PageTemplateLocalServiceUtil;

public class TopicsUtil 
{
	
	private static Log _log = LogFactoryUtil.getLog(TopicsUtil.class);
	
	public static List<String> getAllCategoriesByContentIds(long groupId, List<String> contentIds)
	{
		List<String> categoryContentIdsResult = new ArrayList<String>();
		if (contentIds != null && contentIds.size() > 0)
		{
			StringBuffer query = new StringBuffer("						\n");
			query.append("		SELECT DISTINCT c.categoryId			\n");
			query.append(		getFromSQL(groupId, false, "INNER")   );
			query.append("			WHERE								\n");

			//Contenidos a incluir
			query.append(getInClauseSQL(contentIds, "j.articleId", false, false));
			
			_log.trace(query.toString());
			
			List<Object> categoryContentIds = PortalLocalServiceUtil.executeQueryAsList(query.toString());
			
			for(Object currentCategoryContentId:categoryContentIds)
			{
				categoryContentIdsResult.add(currentCategoryContentId.toString());
			}
		}
		return categoryContentIdsResult;
	}

	private static List<Object> getArticleTopicsMeta(long groupId, int orderType, String orderDirection, 
													List<String> stickyCategoryIds,
													List<String> excludeVocabularyIds,
													List<String> excludeCategoryIds)
	{
		
		List<Object> resultCategoryIds = new ArrayList<Object>();
		if(stickyCategoryIds != null && stickyCategoryIds.size() > 0)
		{
			StringBuffer query = new StringBuffer();
			query.append("SELECT c.categoryId, c.name");
			
			if(orderType == IterKeys.ORDER_NUM_ARTICLES)
			{
				query.append( ", COUNT(1) criteria " );
				query.append( "\nFROM AssetCategory c ");
				query.append( "\nINNER JOIN AssetEntries_AssetCategories a ON a.categoryId=c.categoryId");
				query.append( "\nINNER JOIN AssetEntry e ON e.entryId=a.entryId");
				query.append( "\nINNER JOIN JournalArticle j ON j.resourcePrimKey=e.classPK");
			}
			else
			{
				query.append( ", c.name criteria ");
				query.append("\nFROM AssetCategory c ");
			}
			
			query.append("\nWHERE\n");
			query.append(getInClauseSQL(stickyCategoryIds, "c.categoryId", false, false));
			
			if(excludeVocabularyIds != null && excludeVocabularyIds.size() > 0)
				query.append(getInClauseSQL(excludeVocabularyIds, "c.vocabularyId", true, true));
			
			if(excludeCategoryIds != null && excludeCategoryIds.size() > 0)
			{
				StringBuffer excludeWhere = new StringBuffer();
				excludeWhere.append(" AND\n\t\t\tIF(((SELECT COUNT(0) FROM AssetCategory c2 WHERE ");
				excludeWhere.append(getInClauseSQL(excludeCategoryIds, "c2.categoryId", false, false));
				excludeWhere.append("AND\n\t\tc.leftCategoryId >= c2.leftCategoryId AND \n\t\tc.rightCategoryId <= c2.rightCategoryId) > 0), FALSE, TRUE)");
				query.append(excludeWhere);
			}
			
			query.append("\nGROUP BY c.name");
			query.append(getOrderBySQL(orderDirection, "criteria", "c.categoryId"));
			
			if(_log.isTraceEnabled())
				_log.trace(query.toString());
			
			resultCategoryIds = PortalLocalServiceUtil.executeQueryAsList(query.toString());
		}
		
		return resultCategoryIds;
	}
	
	public static List<Object> getFilterCategories(long groupId, List<String> contentIds,
												   List<String> selectedCategoryIds,
												   List<String> excludeVocabularyIds,
												   List<String> excludeCategoryIds,
												   int orderType, String orderDirection)
	{
		
		List<String> selectedCategoryIdsByArticle = getAllCategoriesByContentIds(groupId, contentIds);
		
		Set<String> uniqueCategoryIds = new HashSet<String>();
		if(selectedCategoryIdsByArticle != null && selectedCategoryIdsByArticle.size() > 0)
		{
			uniqueCategoryIds.addAll(selectedCategoryIdsByArticle);
		}
		
		if(selectedCategoryIds != null && selectedCategoryIds.size() > 0)
		{
			uniqueCategoryIds.addAll(selectedCategoryIds);
		}
		
		List<String> categoryIdsParam = new ArrayList<String>(uniqueCategoryIds);

		return getArticleTopicsMeta(groupId, orderType, orderDirection, 
									categoryIdsParam, excludeVocabularyIds,
									excludeCategoryIds);
	}
	
	public static List<Object> getFilterCategories(long groupId, List<String> contentTypes, 
												   List<String> selectedVocabularyIds, List<String> selectedCategoryIds, 
												   List<String> excludeCategoryIds, List<String> stickyCategoryIds, 
												   List<String> layoutIds, List<String> qualificationId, 
												   int orderType, String orderDirection, 
												   Date validityDate, int startIndex, 
												   int numElements)
	{
		
		StringBuffer query = new StringBuffer();
		
		query.append("SELECT categoryId, name, criteriaAll");
		query.append("\nFROM");
		query.append("\n(");
		
		//CATEGORÍAS DINÁMICAS
		query.append(getSelectWithCriteriaSQL(orderType));
		query.append("\n\t\tFROM");
		query.append("\n\t\t(SELECT c.categoryId, c.name");
		query.append(getOrderFieldSQL(orderType));
		query.append(getFromSQL(groupId, true, "INNER"));
		query.append("		").append(CategoriesUtil.getDiscreteCategoriesJoin(selectedCategoryIds));
		query.append("			WHERE TRUE\n");
		
		//Filtros básicos
		query.append(getBasicFilterSQL(contentTypes, layoutIds, qualificationId, validityDate));
		
		//Vocabularios/Categorías a incluir/excluir
		query.append(getSelectedExcludedTopicsSQL(selectedVocabularyIds, Validator.isNotNull(selectedCategoryIds), excludeCategoryIds, stickyCategoryIds));

		if(orderType == IterKeys.ORDER_NUM_ARTICLES)
		{
			query.append("\n\t\t\tGROUP BY name");
		}
		query.append("\n\t\t)articlesFilter");
		query.append("\n\t\tGROUP BY name");
		
		//Ordenación
		query.append(getOrderBySQL(orderDirection));
		
		//Límite
		if(startIndex != QueryUtil.ALL_POS && numElements != QueryUtil.ALL_POS)
		{
			int limitBySticky = numElements;
			if(stickyCategoryIds != null && stickyCategoryIds.size() > 0)
			{
				limitBySticky = numElements - stickyCategoryIds.size();
			}
			
			if(startIndex > -1 && limitBySticky > 0)
			{
				query.append("\n\t\tLIMIT " + startIndex + ", " + limitBySticky);
			}
		}
		query.append("\n\t)");
		
		//CATEGORÍAS FIJAS
		if(stickyCategoryIds != null && stickyCategoryIds.size() > 0)
		{
			query.append("	UNION ALL								\n");
			query.append(getSelectWithCriteriaSQL(orderType));
			query.append("	FROM									\n");
			query.append("		(SELECT c.categoryId, c.name		\n");
			query.append(getOrderFieldSQL(orderType)                   );
			query.append(getFromSQL(groupId, false, "LEFT")			   );
			query.append("			WHERE							\n");
			
			// Categorías fijas
			query.append(getInClauseSQL(stickyCategoryIds, "c.categoryId", false, false));
			if(orderType == IterKeys.ORDER_NUM_ARTICLES)
			{
				query.append("\n			GROUP BY name			\n");
			}
			query.append("\n		) stickyFilter					\n");
			query.append("		GROUP BY name						\n");
			query.append("\n\t)");
		}
		
		query.append("\n)finalCategories");
		
		//Ordenación
		query.append(getOrderBySQL(orderDirection));
		
		if(startIndex > -1 && numElements > 0)
		{
			query.append("\n\t\tLIMIT " + startIndex + ", " + numElements);
		}
		
		//System.out.println(query.toString());
		_log.trace(query.toString());
		
		return PortalLocalServiceUtil.executeQueryAsList(query.toString());
	}
	
	public static String getSelectedExcludedTopicsSQL(List<String> selectedVocabularyIds, boolean selectedCategoryIds, 
			  List<String> excludeCategoryIds, List<String> stickyCategoryIds )
	{
		return getSelectedExcludedTopicsSQL( selectedVocabularyIds, selectedCategoryIds, excludeCategoryIds, stickyCategoryIds, false  );
	}
	
	public static String getSelectedExcludedTopicsSQL(List<String> selectedVocabularyIds, boolean selectedCategoryIds, 
													  List<String> excludeCategoryIds, List<String> stickyCategoryIds, Boolean onlyCategoryDescendents )
	{
		return CategoriesUtil.getSelectedExcludedTopicsSQL(
									selectedVocabularyIds, selectedCategoryIds, 
									excludeCategoryIds, stickyCategoryIds, 
									onlyCategoryDescendents);
	}
	
	private static String getBasicFilterSQL(List<String> contentTypes, List<String> layoutIds, 
											List<String> qualificationId, Date validityDate)
	{
		StringBuffer query = new StringBuffer();
		//Tipo de contenido
		if(contentTypes != null && contentTypes.size() > 0)
		{
			query.append(getInClauseSQL(contentTypes, "j.structureId", false, true));
		}
		
		//Secciones
		if(layoutIds != null && layoutIds.size() > 0)
		{
			query.append(getInClauseSQL(layoutIds, "n.layoutId", false, true));
		}
		
		//Calificación
		if(qualificationId != null && qualificationId.size() > 0 && !qualificationId.get(0).isEmpty() )
		{
			query.append(getInClauseSQL(qualificationId, "n.qualificationId", false, true));
		}

		//Fecha de vigencia
		query.append(getValidityDateSQL(validityDate));
		return query.toString();
	}
	
	private static String getFromSQL(long groupId, boolean useNewsPageCont, String joinType)
	{
		StringBuffer query = new StringBuffer("												\n").append(
			"		FROM AssetCategory c													\n").append(
			"		%1$s JOIN AssetEntries_AssetCategories a ON a.categoryId=c.categoryId	\n").append(	
			"		%1$s JOIN AssetEntry e ON e.entryId=a.entryId							\n").append(
			"		%1$s JOIN JournalArticle j ON j.resourcePrimKey=e.classPK				\n");	
							
		if (useNewsPageCont)
		{
			query.append( String.format("		INNER JOIN News_PageContent n ON (n.contentId=j.articleId AND n.online_=TRUE AND n.groupId=%d)\n", groupId) );
		}
		
		return String.format(query.toString(), joinType);
	}
	
	private static String getValidityDateSQL(Date validityDate)
	{
		String query = "";
		if(validityDate != null)
		{
			SimpleDateFormat format = new SimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_00);
			String formatValidityDate  = format.format(validityDate);
			query = "\n\t\t\t AND '" + formatValidityDate + "' BETWEEN n.vigenciadesde AND n.vigenciahasta";
		}
		return query;
	}
	
	public static String getInClauseSQL(List<String> ids, String columnId, boolean notIn, boolean and)
	{
		return CategoriesUtil.getInClauseSQL(ids, columnId, notIn, and);
	}
	
	public static String getInClauseSQLLong(long[] ids, String columnId, boolean notIn, boolean and)
	{
		return CategoriesUtil.getInClauseSQLLong(ids, columnId, notIn, and);
	}
	
	private static String getSelectWithCriteriaSQL(int orderType)
	{
		String str = String.format("	(SELECT categoryId, name, %s \n", 
											(orderType == IterKeys.ORDER_NUM_ARTICLES)			? 
											"(SELECT IFNULL( SUM(criteria), 0 )) criteriaAll" 	: 
											"(criteria) criteriaAll"							);
		return str;
	}
	
	private static String getOrderFieldSQL(int orderType)
	{
		String str = String.format(", IFNULL((%s),0)criteria \n", 
											(orderType == IterKeys.ORDER_NUM_ARTICLES)			? 
											"SELECT COUNT(j.articleId)"							:
											"c.name"											);
		return str;
	}
	
	private static String getOrderBySQL(String orderDirection)
	{
		return getOrderBySQL(orderDirection, "criteriaAll", "categoryId");
	}
	
	private static String getOrderBySQL(String orderDirection, String criteriaName, String noDrawCriteria)
	{
		StringBuffer query = new StringBuffer();
		String orderDirectionParam = "ASC";
		
		if(orderDirection != null && !orderDirection.isEmpty())
		{
			orderDirectionParam = orderDirection;
		}
		
		query.append("\nORDER BY " + criteriaName);
		query.append(" " + orderDirectionParam);
		query.append(", " + noDrawCriteria + " " + orderDirectionParam);
		
		return query.toString();
	}
	
	public static String getTopicURLById(long modelId, String topicName)
	{
		StringBuffer topicURLBuffer = new StringBuffer();
		
		try 
		{
			//Añadimos modelURL
			PageTemplate pageTemplate = PageTemplateLocalServiceUtil.getPageTemplateById(modelId);
			
			if(pageTemplate != null)
			{
				String modelURL = PageTemplateLocalServiceUtil.getPageTemplateURL(pageTemplate.getId());
				
				if(modelURL != null)
				{
					topicURLBuffer.append(IterURLUtil.getIterURLPrefix());
					topicURLBuffer.append(modelURL);
					
					//Añadimos SEPARATOR/META/NORMALIZED_TOPIC_NAME
					
					if(PortalUtil.getCheckMappingWithPrefix())
					{
						topicURLBuffer.append("/-");
					}
					
					if(!PortalUtil.getNewsMappingPrefix().isEmpty())
					{
						topicURLBuffer.append( PortalUtil.getNewsMappingPrefix() );
					}
					else
					{
						topicURLBuffer.append("/");
					}
					
					topicURLBuffer.append(CategoriesUtil.TOPIC_URL_ID);
					//topicURLBuffer.append(topicId + "/");
					
					String normalizedTopicName  = CategoriesUtil.normalizeText(topicName);
					topicURLBuffer.append(normalizedTopicName);
				}
			}
			else
			{
				//Añadir model de pageTemplate por defecto
				topicURLBuffer = new StringBuffer();
			}
		} 
		catch (Exception e) 
		{
			_log.error(e.toString());
			_log.trace(e);
		}
		
		return topicURLBuffer.toString();
	}
	
	public static List<String> getCategoriesIds(RenderRequest renderRequest)
	{
		String tagsParam = CategoriesUtil.getCategoriesFromURL(renderRequest);
		List<String> categoryIds = CategoriesUtil.getCategoriesIds(tagsParam);
		
		return categoryIds;
	}
	
	public static List<String> getListPreference(PortletPreferences preferences, String preferenceName)
	{
		List<String> preferenceList = null;
		if(Validator.isNotNull(preferences) && Validator.isNotNull(preferenceName))
		{
			String[] preferenceArray = preferences.getValues(preferenceName, null);
			if(preferenceArray != null && preferenceArray.length > 0)
				preferenceList = Arrays.asList(preferenceArray);
		}
		
		return preferenceList;
	}
}
