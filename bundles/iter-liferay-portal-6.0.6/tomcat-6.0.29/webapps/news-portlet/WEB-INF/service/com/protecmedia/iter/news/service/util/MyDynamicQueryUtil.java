/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.news.service.util;

/*

query2.setProjection(ProjectionFactoryUtil.distinct(
	ProjectionFactoryUtil.projectionList()
		.add(ProjectionFactoryUtil.property("articleId"))
		.add(ProjectionFactoryUtil.property("groupId"))));

DynamicQuery query = DynamicQueryFactoryUtil.forClass(JournalArticle.class, cl);
query.add(PropertyFactoryUtil.forName("groupId").eq(scopeGroupId));
query.add(PropertyFactoryUtil.forName("status").eq(WorkflowConstants.STATUS_APPROVED));
query.add(PropertyFactoryUtil.forName("structureId").eq(IterKeys.ARTICULOMILENIUMID));			

List<JournalArticle> articulos = JournalArticleLocalServiceUtil.dynamicQuery(query);	

*/

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.Junction;
import com.liferay.portal.kernel.dao.orm.OrderFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ProjectionFactoryUtil;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.persistence.impl.BasePersistenceImpl;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.model.JournalTemplate;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portlet.journal.service.JournalTemplateLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;

public class MyDynamicQueryUtil extends BasePersistenceImpl {
		
	/**
	 * 
	 * @param groupId
	 * @param structures
	 * @param keyword
	 * @param order
	 * @param orderByType
	 * @return
	 */
	private static DynamicQuery createQueryArticle(long groupId, List<Object> structures, String keyword, String order, String orderByType) {
		//Listado de articulos
		ClassLoader cl = PortalClassLoaderUtil.getClassLoader();				
		
		// Buscar por group, structureId, status 
		DynamicQuery query = DynamicQueryFactoryUtil.forClass(JournalArticle.class, cl);		
		
		query.add(PropertyFactoryUtil.forName("groupId").eq(groupId));
		query.add(PropertyFactoryUtil.forName("status").eq(WorkflowConstants.STATUS_APPROVED));
		query.add(PropertyFactoryUtil.forName("structureId").in(structures));				
		query.add(PropertyFactoryUtil.forName("articleId").ne(IterKeys.EXAMPLEARTICLEID));
		query.add(PropertyFactoryUtil.forName("articleId").ne(IterKeys.EXAMPLEGALLERYID));
		query.add(PropertyFactoryUtil.forName("articleId").ne(IterKeys.EXAMPLEMULTIMEDIAID));
		query.add(PropertyFactoryUtil.forName("articleId").ne(IterKeys.EXAMPLEPOLLID));
		
		if (keyword != null && !keyword.equals("")) {
			String[] keywords = keyword.split(" ");
			Junction junction = RestrictionsFactoryUtil.disjunction();
			//Junction junctionContent = RestrictionsFactoryUtil.disjunction();
			for (int i = 0; i < keywords.length; i++) {
				junction.add(PropertyFactoryUtil.forName("title").like("%" + keywords[i] + "%"));
				junction.add(PropertyFactoryUtil.forName("content").like("%" + keywords[i] + "%"));
			}
			query.add(junction);
			//query.add(junctionContent);
		}
				
		if (!order.equals("")) {			
			if (orderByType.equals("asc")) {
				query.addOrder(OrderFactoryUtil.asc(order));
			} else {
				query.addOrder(OrderFactoryUtil.desc(order));
			}
		}
		
		query.setProjection(ProjectionFactoryUtil.projectionList()
				.add(ProjectionFactoryUtil.distinct(ProjectionFactoryUtil.property("articleId")))
				.add(ProjectionFactoryUtil.property("structureId"))
				.add(ProjectionFactoryUtil.property("title")));					
		
		return query;
	}
	
	/**
	 * 
	 * @param groupId
	 * @param structures
	 * @param keyword
	 * @param order
	 * @param orderByType
	 * @return
	 */
	public static long getContentsCount(long groupId, List<Object> structures, String keyword, String order, String orderByType) {		

		try {
			DynamicQuery query = createQueryArticle(groupId, structures, keyword, order, orderByType);
			
			return JournalArticleLocalServiceUtil.dynamicQueryCount(query);
		} catch (SystemException e) {			
			e.printStackTrace();
		}			
		
		return 0;
	}
	
	/**
	 * 
	 * @param groupId
	 * @param structures
	 * @param keyword
	 * @param order
	 * @param orderByType
	 * @return
	 */
	public static List<JournalArticle> getContents(long groupId, List<Object> structures, String keyword, String order, String orderByType) {		
		
		try {
			DynamicQuery query = createQueryArticle(groupId, structures, keyword, order, orderByType);
			
			List<Object> articulosQuery = JournalArticleLocalServiceUtil.dynamicQuery(query);
						
			List<JournalArticle> articulos = new ArrayList<JournalArticle>();
			for (int i = 0; i < articulosQuery.size(); i++) {
				Object []articulo = (Object []) articulosQuery.get(i);
				try {
					articulos.add(JournalArticleLocalServiceUtil.getArticle(groupId, String.valueOf(articulo[0])));
				} catch (PortalException e) {
					e.printStackTrace();
				}				
			}
			
			return articulos;
		} catch (SystemException e) {
			return null;
		}
		
	}
	
	/**
	 * 
	 * @param groupId
	 * @param structures
	 * @param keyword
	 * @param order
	 * @param orderByType
	 * @param start
	 * @param end
	 * @return
	 */
	public static List<JournalArticle> getContents(long groupId, List<Object> structures, String keyword, String order, String orderByType, int start, int end) {		
		
		try {
			DynamicQuery query = createQueryArticle(groupId, structures, keyword, order, orderByType);
			
			List<Object> articulosQuery = JournalArticleLocalServiceUtil.dynamicQuery(query, start, end);
						
			List<JournalArticle> articulos = new ArrayList<JournalArticle>();
			for (int i = 0; i < articulosQuery.size(); i++) {
				Object []articulo = (Object []) articulosQuery.get(i);
				try {
					articulos.add(JournalArticleLocalServiceUtil.getArticle(groupId, String.valueOf(articulo[0])));
				} catch (PortalException e) {
					e.printStackTrace();
				}				
			}
			
			return articulos;
		} catch (SystemException e) {
			return null;
		}
		
	}
	
	/**
	 * 
	 * @param groupId
	 * @param privateLayout
	 * @param parentLayoutId
	 * @param hiddenLayout
	 * @param typeLayout
	 * @return
	 */
	public static List<Layout> getLayouts(long groupId, boolean privateLayout, long parentLayoutId, boolean hiddenLayout, String typeLayout) {
		List<Layout> layouts = new ArrayList<Layout>();
		
		//Listado de articulos
		ClassLoader cl = PortalClassLoaderUtil.getClassLoader();
		
		// Buscar por group, structureId, status 
		DynamicQuery query = DynamicQueryFactoryUtil.forClass(Layout.class, cl);
		
		query.add(PropertyFactoryUtil.forName("groupId").eq(groupId));
		query.add(PropertyFactoryUtil.forName("privateLayout").eq(privateLayout));
		query.add(PropertyFactoryUtil.forName("parentLayoutId").eq(parentLayoutId));
		query.add(PropertyFactoryUtil.forName("hidden").eq(hiddenLayout));
		//query.add(PropertyFactoryUtil.forName("type").eq(typeLayout));
		
		try {
			layouts = LayoutLocalServiceUtil.dynamicQuery(query);
		} catch (SystemException e) {
			e.printStackTrace();
		}
		
		return layouts;
	}
	
	
	private static DynamicQuery getQueryTemplates(long companyId, long groupId, String structureId, String type, String keyword) {
		//Listado de articulos
		ClassLoader cl = PortalClassLoaderUtil.getClassLoader();
		
		// Buscar por group, structureId, status 
		DynamicQuery query = DynamicQueryFactoryUtil.forClass(JournalTemplate.class, cl);
		
		query.add(PropertyFactoryUtil.forName("companyId").eq(companyId));
		query.add(PropertyFactoryUtil.forName("groupId").eq(groupId));
		query.add(PropertyFactoryUtil.forName("structureId").eq(structureId));
		query.add(PropertyFactoryUtil.forName("templateId").like(type + "%"));
		
		if (keyword != null && !keyword.equals("")) {
			String[] keywords = keyword.split(" ");
			Junction junction = RestrictionsFactoryUtil.disjunction();

			for (int i = 0; i < keywords.length; i++) {
				junction.add(PropertyFactoryUtil.forName("name").like("%" + keywords[i] + "%"));
				junction.add(PropertyFactoryUtil.forName("description").like("%" + keywords[i] + "%"));
			}
			query.add(junction);
		}
		
		return query;
	}
	
	public static List<JournalTemplate> getTemplates(long companyId, long groupId, String structureId, String type, String keywords, int start, int end) {
		List<JournalTemplate> templates = new ArrayList<JournalTemplate>();
		
		try {
			DynamicQuery query = getQueryTemplates(companyId, groupId, structureId, type, keywords);

			templates = JournalTemplateLocalServiceUtil.dynamicQuery(query, start, end);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return templates;
	}
	
	
	public static List<JournalTemplate> getTemplates(long companyId, long groupId, String structureId, String type, String keywords) {
		List<JournalTemplate> templates = new ArrayList<JournalTemplate>();
		
		try {
			DynamicQuery query = getQueryTemplates(companyId, groupId, structureId, type, keywords);
			int start = 0;
			int end = getTemplatesCount(companyId, groupId, structureId, type, keywords);

			templates = JournalTemplateLocalServiceUtil.dynamicQuery(query, start, end);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return templates;
	}
	
	public static int getTemplatesCount(long companyId, long groupId, String structureId, String type, String keywords) {
		
		try {
			DynamicQuery query = getQueryTemplates(companyId, groupId, structureId, type, keywords);

			return (int) JournalTemplateLocalServiceUtil.dynamicQueryCount(query);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return 0;
	}	
	
	private static Log _log = LogFactoryUtil.getLog(MyDynamicQueryUtil.class);
}
