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

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portlet.asset.model.AssetCategory;
import com.liferay.portlet.asset.model.AssetVocabulary;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.expando.model.ExpandoTable;
import com.liferay.portlet.expando.service.ExpandoValueLocalServiceUtil;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.news.service.base.CategorizeServiceBaseImpl;

/**
 * @author Protecmedia
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class CategorizeServiceImpl extends CategorizeServiceBaseImpl
{

	private static Log _log = LogFactoryUtil.getLog(CategorizeServiceImpl.class);
	
	/* ***********************************
	 * 
	 *  VOCABULARY
	 * 
	 * ***********************************/
	/*
	 * ADD
	 */
	public long addVocabulary(long groupId, long userId, String title, String description) throws SystemException 
	{
		try
		{
			AssetVocabulary vocabulary = categorizeLocalService.addVocabulary(groupId, userId, title, description);
			return vocabulary.getVocabularyId();
		} 
 		catch(Throwable th)
		{
 			_log.error(th);
 			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}

	/*
	 * DELETE
	 */
	public void deleteVocabulary(long vocabularyId) throws SystemException
	{
		try
		{
			categorizeLocalService.deleteVocabulary(vocabularyId);
		}
		catch(Throwable th)
		{
 			_log.error(th);
 			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}

	/*
	 * UPDATE
	 */
	public long updateVocabulary(long groupId, long vocabularyId, String title, String description) throws SystemException
	{
		try
		{
			AssetVocabulary vocabulary = categorizeLocalService.updateVocabulary(groupId, vocabularyId, title, description);
			return vocabulary.getVocabularyId();
		}
		catch(Throwable th)
		{
 			_log.error(th);
 			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	/* ***********************************
	 * 
	 *  CATEGORY
	 * 
	 * ***********************************/
	
	/*
	 * ADD 
	 */
	public long addCategory(long groupId, long userId, long parentCategoryId, long vocabularyId, String title) throws SystemException
	{		
		try
		{
			AssetCategory category = categorizeLocalService.addCategory(groupId, userId, parentCategoryId, vocabularyId, title);
			return category.getCategoryId();
		}
		catch(Throwable th)
		{
 			_log.error(th);
 			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	/*
	 * UPDATE
	 */
	public long updateCategory(long groupId, long userId, long categoryId, long parentCategoryId, long vocabularyId, String title) throws SystemException
	{
		try
		{
			AssetCategory category = categorizeLocalService.updateCategory(groupId, userId, categoryId, parentCategoryId, vocabularyId, title);
			return category.getCategoryId();
		}
		catch(Throwable th)
		{
 			_log.error(th);
 			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}

	/*
	 * DELETE 
	 */
	public void deleteCategory(long categoryId)	throws SystemException 
	{
		try
		{
			categorizeLocalService.deleteCategory(categoryId);
		} 
		catch(Throwable th)
		{
 			_log.error(th);
 			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}

	/* ***********************************
	 * 
	 *  EXPANDO TABLE
	 * 
	 * ***********************************/
	
	/*
	 * ADD
	 */
	public long addExpandoTable(long companyId, String className) throws SystemException
	{
		try
		{
			ExpandoTable exTable = categorizeLocalService.addExpandoTable(companyId, className);
			return exTable.getTableId();
		}
		catch(Throwable th)
		{
 			_log.error(th);
 			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	/*
	 * GET
	 */
	public long getExpandoTable(long companyId, String className) throws SystemException 
	{
		try
		{
			ExpandoTable exTable = categorizeLocalService.getExpandoTable(companyId, className);
			return exTable.getTableId();
		}
		catch(Throwable th)
		{
 			_log.error(th);
 			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	/*
	 * DELETE
	 */
	public void deleteExpandoTable(long companyId, String className) throws SystemException
	{
		try
		{
			categorizeLocalService.deleteExpandoTable(companyId, className);
		} 
		catch(Throwable th)
		{
 			_log.error(th);
 			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void deleteExpandoTable(long tableId) throws SystemException
	{
		try
		{
			categorizeLocalService.deleteExpandoTable(tableId);
		}
		catch(Throwable th)
		{
 			_log.error(th);
 			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	/*
	 * PARA AÑADIR VALORES A CAMPOS EXPANDO
	 */
	
	/*
	 * Nueva version para Milenium, para enviar menos parametros
	 */
	String tableName = "CUSTOM_FIELDS";
	
	public long addBooleanValue(long companyId, long groupId, String className, String columnName, String id, boolean data) throws SystemException
	{
		try
		{
			long classPK = getClassPK(groupId, className, id);
			return ExpandoValueLocalServiceUtil.addValue(companyId, className, tableName, columnName, classPK, data).getClassNameId();
		}
		catch(Throwable th)
		{
 			_log.error(th);
 			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public long addStringValue(long companyId, long groupId, String className, String columnName, String id, String data) throws SystemException
	{
		try
		{
			long classPK = getClassPK(groupId, className, id);
			return ExpandoValueLocalServiceUtil.addValue(companyId, className, tableName, columnName, classPK, data).getValueId();
		}
		catch(Throwable th)
		{
 			_log.error(th);
 			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public long addDoubleValue(long companyId, long groupId, String className, String columnName, String id, double data) throws SystemException
	{
		try
		{
			long classPK = getClassPK(groupId, className, id);
			return ExpandoValueLocalServiceUtil.addValue(companyId, className, tableName, columnName, classPK, data).getValueId();
		}
		catch(Throwable th)
		{
 			_log.error(th);
 			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public long addDateValue(long companyId, long groupId, String className, String columnName, String id, java.util.Date data) throws SystemException
	{
		try
		{
			long classPK = getClassPK(groupId, className, id);
			return ExpandoValueLocalServiceUtil.addValue(companyId, className, tableName, columnName, classPK, data).getValueId();
		}
		catch(Throwable th)
		{
 			_log.error(th);
 			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	private long getClassPK(long groupId, String className, String id) throws SystemException
	{
		try
		{
			if (className.equals(IterKeys.CLASSNAME_JOURNALARTICLE)){
				return JournalArticleLocalServiceUtil.getArticle(groupId, id).getId();
			} else if (className.equals(IterKeys.CLASSNAME_LAYOUT)){
				return Long.valueOf (id);
			} else if (className.equals(IterKeys.CLASSNAME_DLFILEENTRY)){
				return DLFileEntryLocalServiceUtil.getFileEntry(groupId, 0, id).getFileEntryId();
			}
			return -1;
		}
		catch(Throwable th)
		{
 			_log.error(th);
 			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	/*
	 * PARA ACTUALIZAR CATEGORIAS DE UN WEB CONTENT
	 */
	public void updateWebContentCategories(long groupId, long userId, String articleId, long[] assetCategoryIds) throws SystemException 
	{
		try
		{
			categorizeLocalService.updateWebContentCategories(groupId, userId, articleId, assetCategoryIds, null);
		}
		catch(Throwable th)
		{
 			_log.error(th);
 			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void updateWebContentCategories2(long groupId, long userId, String articleId, long[] assetCategoryIds, long[] mainAssetCategoryIds) throws SystemException 
	{
		try
		{
			categorizeLocalService.updateWebContentCategories(groupId, userId, articleId, assetCategoryIds, mainAssetCategoryIds);
		}
		catch(Throwable th)
		{
 			_log.error(th);
 			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
	}
	
	public void asignArticleAsAboutCategoryArticle(long groupid, long categoryId, String articleId) throws SystemException
	{
		try
		{
			categorizeLocalService.asignArticleAsAboutCategoryArticle(groupid, categoryId, articleId);
		}
		catch(Throwable th)
		{
			_log.error(th);
			throw new SystemException( ServiceErrorUtil.getServiceErrorAsXml(th) );
		}
	}
	
}
