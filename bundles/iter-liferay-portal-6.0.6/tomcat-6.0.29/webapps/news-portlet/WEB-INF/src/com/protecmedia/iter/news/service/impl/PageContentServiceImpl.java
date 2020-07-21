/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
/**
 * Copyright (c) 2000-2010 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.protecmedia.iter.news.service.impl;

import java.util.List;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.IterURLQueries;
import com.liferay.portal.kernel.util.IterURLUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.news.model.PageContent;
import com.protecmedia.iter.news.service.base.PageContentServiceBaseImpl;

@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class PageContentServiceImpl extends PageContentServiceBaseImpl 
{
	/*
	 * ADD
	 */
	public PageContent insertPageContentArticleModel(String pageContentId, String articleId, long sectionId,
			long groupId, long globalId, String qualification, String type, long articleModelId, long pageContentTargetId, 
			int expirationDateYear, int expirationDateMonth, int expirationDateDay,
			int expirationDateHour, int expirationDateMinute,
			int displayDateYear, int displayDateMonth, int displayDateDay,
			int displayDateHour, int displayDateMinute, 
			boolean online, boolean defaultSection)
			throws SystemException
	{
		try
		{
			return pageContentLocalService.insertPageContentArticleModel(pageContentId, articleId, sectionId,
					groupId, globalId, qualification, type, articleModelId, pageContentTargetId, 
					expirationDateYear, expirationDateMonth, expirationDateDay,
					expirationDateHour, expirationDateMinute,
					displayDateYear, displayDateMonth, displayDateDay,
					displayDateHour, displayDateMinute, 
					online, defaultSection);
		} 
		catch(Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}

	public PageContent addPageContentArticleModel(String pageContentId, String articleId, long sectionId,
			long groupId, long globalId, String qualification, String type, long articleModelId, int position, 
			int expirationDateYear, int expirationDateMonth, int expirationDateDay,
			int expirationDateHour, int expirationDateMinute,
			int displayDateYear, int displayDateMonth, int displayDateDay,
			int displayDateHour, int displayDateMinute, 
			boolean online)
			throws Exception 
	{
		// Este método NO se utiliza como existen métodos sobrecargados SOAP genera nombres únicos para ellos, y MLN llama a uno de ellos así que si se elimina 
		// MLN llamaría a métodos de Iter que ya no existirían. 
		return null;
	}
	
	public PageContent addPageContentArticleModel(String pageContentId, String articleId, long sectionId,
			long groupId, long globalId, String qualification, String type, long articleModelId, int position, 
			int expirationDateYear, int expirationDateMonth, int expirationDateDay,
			int expirationDateHour, int expirationDateMinute,
			int displayDateYear, int displayDateMonth, int displayDateDay,
			int displayDateHour, int displayDateMinute, 
			boolean online, boolean defaultSection)
			throws Exception 
	{
		try
		{
			boolean compatibility = GetterUtil.getBoolean( PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_NEWS_PAGECONTENT_COMPATIBILITY_ADDPAGECONTENT) );
			if (!compatibility)
				throw new Exception(IterErrorKeys.ITER_UPDATE_MILENIUM_ITER);

			return pageContentLocalService.addPageContent(pageContentId, articleId, sectionId,
				groupId, globalId, qualification, type, articleModelId, position, 
				expirationDateYear, expirationDateMonth, expirationDateDay, 
				expirationDateHour, expirationDateMinute,
				displayDateYear, displayDateMonth, displayDateDay,
				displayDateHour, displayDateMinute, 
				online, defaultSection);
		} 
		catch(Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}

	/*
	 * UPDATE 
	 */
	public PageContent updatePageContentQualification(long pageContentId, String qualification) throws Exception 
	{
		try
		{
			return pageContentLocalService.updatePageContent(qualification, pageContentId);
		} 
		catch(Exception ex)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(ex));
		}
	}
	
	public PageContent updatePageContentArticleModel(long pageContentId, long articleModelId) throws Exception 
	{
		try
		{
			return pageContentLocalService.updatePageContentArticleModel(pageContentId,
				articleModelId);
		} 
		catch(Exception ex)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(ex));
		}
	}

	public PageContent updatePageContentDate(
			int expirationDateYear, int expirationDateMonth, int expirationDateDay,
			int expirationDateHour, int expirationDateMinute,
			int displayDateYear, int displayDateMonth, int displayDateDay,
			int displayDateHour, int displayDateMinute, 
			long pageContentId) throws Exception 
	{
		try
		{
			return pageContentLocalService.updatePageContent(
				expirationDateYear, expirationDateMonth, expirationDateDay, 
				expirationDateHour, expirationDateMinute,
				displayDateYear, displayDateMonth, displayDateDay,
				displayDateHour, displayDateMinute, pageContentId);
		} 
		catch(Exception ex)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(ex));
		}
	}

	public PageContent updatePageContentQualificationDate(
			String qualification, 
			int expirationDateYear, int expirationDateMonth, int expirationDateDay,
			int expirationDateHour, int expirationDateMinute,
			int displayDateYear, int displayDateMonth, int displayDateDay,
			int displayDateHour, int displayDateMinute,
			long pageContentId) throws Exception 
	{
		try
		{
			return pageContentLocalService.updatePageContent(qualification, 
				expirationDateYear, expirationDateMonth, expirationDateDay, 
				expirationDateHour, expirationDateMinute,
				displayDateYear, displayDateMonth, displayDateDay,
				displayDateHour, displayDateMinute, 
				pageContentId);
		} 
		catch(Exception ex)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(ex));
		}
	}

	public void deletePageContent(long groupId, String articleId, long layoutId, String type) throws Exception 
	{
		// Este método NO se utiliza como existen métodos sobrecargados SOAP genera nombres únicos para ellos, y MLN llama a uno de ellos así que si se elimina 
		// MLN llamaría a métodos de Iter que ya no existirían. 
	}
	
	public void deletePageContent(long pageContentId) throws Exception 
	{
		try
		{
			pageContentLocalService.deletePageContent(pageContentId);
		} 
		catch(Exception ex)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(ex));
		}
	}
	
	public boolean activatePageContent(long pageContentId) throws Exception 
	{	
		try
		{
			return pageContentLocalService.activatePageContent(pageContentId);
		} 
		catch(Exception ex)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(ex));
		}
	}	

	public boolean deactivatePageContent(long pageContentId) throws Exception 
	{
		try
		{
			return pageContentLocalService.deactivatePageContent(pageContentId);
		} 
		catch(Exception ex)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(ex));
		}
	}
	
	public void changePageContentPosition(long pageContentId, int position) throws Exception 
	{
		// Este método ya no se utiliza pero se mantiene en MLN por copatibilidad hacia atrás
		boolean compatibility = GetterUtil.getBoolean( PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_NEWS_PAGECONTENT_COMPATIBILITY_ADDPAGECONTENT) );
		if (!compatibility)
			throw new Exception(IterErrorKeys.ITER_UPDATE_MILENIUM_ITER);
	}
	
	public void setDefaultPageContent (long groupId, String pageContentId, boolean defaultSection) throws Exception
	{
		try
		{
			// ITER-683 Problemas al editar una nota de la Web y cambiarle la sección predeterminada
			// MLN envía siempre el id_ de la tabla News_Pagecontent, pero el método setDefaultPageContent(...) trabajaba
			// con el PageContentId (Que en el BACK es igual que id_ pero en el LIVE no). Se ha modificado para que trabaje con el id_.
			// Como id_ es un long, hay que realizar una conversión del valor.
			long id_ = Long.parseLong(pageContentId);
			pageContentLocalService.setDefaultPageContent(groupId, id_, defaultSection);
		} 
		catch(Exception ex)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(ex));
		}
	}

	public void reorderPageContents(long groupId, String newOrderXml) throws Exception 
	{
		try
		{
			pageContentLocalService.reorderPageContent(groupId, newOrderXml);
		} 
		catch(Exception ex)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(ex));
		}
	}


	/**
 	 * @param sourceLayoutPlid Id del Layout (sección) que se moverá.
	 * @param targetPositionPlid Id del Layout (sección) que se toma como referencia.
	 * @deprecated Esta función es la utilizada hasta la versión v2 de Iter
	 * @since 14/11/2012.
	 */
	public void changeLayoutOrder(long sourceLayoutPlid, long targetPositionPlid) throws Exception
	{
		try
		{
			pageContentLocalService.changeLayoutOrder(sourceLayoutPlid, targetPositionPlid);
		} 
		catch(Exception ex)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_CHANGE_LAYOUT_ORDER_ZYX, ex));
		}
	}
	/**
	 * @param sourceLayoutPlid Id del Layout (sección) que se moverá.
	 * @param targetPositionPlid Id del Layout (sección) que se toma como referencia.
	 * @param afterTarget Parámetro booleano, que con valor “true” mueve la sección detrás de la de referencia, y con valor “false” la deja delante.
	 */
	public void changeLayoutPosition(long sourceLayoutPlid, long targetPositionPlid, boolean afterTarget) throws SystemException
	{
		try
		{
			pageContentLocalService.changeLayoutOrder(sourceLayoutPlid, targetPositionPlid, afterTarget);
		} 
		catch(Exception ex)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(ex), ex);
		}
	}
	
	/**
	 * 
	 */
	public String getViewerUrl(long groupId, long globalGroupId, String contentId, long pageTemplateId, long layoutId) throws Exception
	{
		String result = StringPool.BLANK;
		try
		{
			if (PropsValues.IS_PREVIEW_ENVIRONMENT)
			{
				String sql = String.format(IterURLQueries.GET_VIEWER_URL, contentId, groupId, pageTemplateId, layoutId);
				List<Object> viewerURL = PortalLocalServiceUtil.executeQueryAsList(sql);
				ErrorRaiser.throwIfFalse(viewerURL != null && viewerURL.size() > 0);
				
				result = String.valueOf( viewerURL.get(0) );
			}
			else				
				result = IterURLUtil.getArticleURLByPlid( groupId, contentId, new long[] {layoutId}, false);
		} 
		catch (Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	@Deprecated
	public String getViewerUrl(long groupId, long globalGroupId, String contentId, long pageTemplateId) throws Exception
	{
		// Este método NO se utiliza como existen métodos sobrecargados SOAP genera nombres únicos para ellos, y MLN llama a uno de ellos así que si se elimina 
		// MLN llamaría a métodos de Iter que ya no existirían. 
		return null;
	}

	
	/**
	 * 
	 * @param model XML que modela el movimiento. Será de la forma:
	 * 	<?xml version="1.0"?>
	 *	<rs>
	 *	    <target id="1234" afterTarget="0|1"/>
	 *	    <source id="7654"/>
	 *	    <source id="9876"/>
	 *	    <source id="1415"/>
	 *	</rs>
	 *
	 * @see http://10.15.20.59:8090/pages/viewpage.action?pageId=11600340
	 * @return El método de momento retornará una cadena vacía, pero se deja la opción 
	 * por si en el futuro se amplian los requerimientos y fuese necesario devolver un 
	 * XML, no surjan problemas de compatibilidad con versiones previas.
	 * @throws SystemException 
	 */
	public String movePageContents(String model) throws SystemException
	{
		String result = "";
		try
		{
			result = pageContentLocalService.movePageContents(model);
		} 
		catch(Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th), th);
		}

		return result;
	}
	
	/**
	 * 
	 * @param groupId
	 * @param contentId
	 * @param layoutIds
	 * @return
	 * @throws SystemException
	 */
	public String getArticleContextInfo(long groupId, String contentId, String[] layoutIds) throws SystemException
	{
		String result = "";
		try
		{
			result = pageContentLocalService.getArticleContextInfo(groupId, contentId, layoutIds);
		} 
		catch(Throwable th)
		{
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th), th);
		}

		return result;
	}
}
