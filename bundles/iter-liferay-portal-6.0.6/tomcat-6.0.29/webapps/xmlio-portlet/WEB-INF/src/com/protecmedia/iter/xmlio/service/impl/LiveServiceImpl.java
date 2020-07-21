/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/

package com.protecmedia.iter.xmlio.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.ORMException;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GroupMgr;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.NoSuchLiveException;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.base.LiveServiceBaseImpl;

/**
 * The implementation of the Live remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.xmlio.service.LiveService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.xmlio.service.LiveServiceUtil} to access the Live remote service.
 * </p>
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author Protecmedia
 * @see com.protecmedia.iter.xmlio.service.base.LiveServiceBaseImpl
 * @see com.protecmedia.iter.xmlio.service.LiveServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor = { Exception.class })
public class LiveServiceImpl extends LiveServiceBaseImpl 
{
	
	private static Log _log = LogFactoryUtil.getLog(LiveServiceImpl.class);
	
	private static final String SEL_ARTICLE_SCOPEGROUP = new StringBuilder(
	"SELECT distinct ExpandoValue.data_																							\n").append(
	"FROM JournalArticle																										\n").append(
	"INNER JOIN ExpandoColumn 				 ON ExpandoColumn.name			  ='").append(IterKeys.EXPANDO_COLUMN_NAME_SCOPEGROUPID).append("'\n").append(
	"LEFT  JOIN ExpandoValue 				 ON (    ExpandoValue.columnId	  = ExpandoColumn.columnId 							\n").append(
	"											 AND ExpandoValue.classpk	  = JournalArticle.id_								\n").append(
	"											 AND LENGTH(ExpandoValue.data_) > 0												\n").append(
	"											)																				\n").append(
	"WHERE 																														\n").append(	
	"		JournalArticle.groupId = %d																							\n").append(
	"	AND JournalArticle.articleId IN ('%s') 																					").toString();

	private String specialCase(String errorCode, ServiceError errorSource) throws Exception
	{
		String strValue = "";
		
		if ( errorSource.getErrorCode().equals(IterErrorKeys.XYZ_E_XPORTCONTENT_ALL_FAILED_ZYX) )
			strValue = errorSource.getMessage();
		else
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(errorCode, errorSource), errorSource);
			//throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(errorCode, errorSource));	
		
		return strValue;
	}
	
	// Publicaci�n del TPU
	public String publishGroupToLive(long companyId, long groupId, long userId, String processId) throws Exception
	{
		String result = "";
	
		try
		{
			long scopeGroupId = groupId;
			result = liveLocalService.publishToLive(companyId, groupId, userId, processId, null, null, true, scopeGroupId);
		}
		catch(ServiceError e)
		{
			result = specialCase(IterErrorKeys.XYZ_E_PUBLISH_GROUP_TO_LIVE_ZYX, e);
		}
		catch(Exception ex)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_PUBLISH_GROUP_TO_LIVE_ZYX, ex));
		}	
		catch (Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_PUBLISH_GROUP_TO_LIVE_ZYX, new Exception(th)));
		}
		return result;
	}
	
	// Punto de entrada antiguo para MLN
	public String publishContentToLive(long companyId, long groupId, long userId, String processId, String [] contentIds) throws Exception
	{
		String result = "";

		try
		{
			ErrorRaiser.throwIfNull(contentIds, IterErrorKeys.XYZ_E_NO_CONTENT_IDS_PROVIDED_TO_PUBLISH_ZYX);
			ErrorRaiser.throwIfFalse(contentIds.length > 0, IterErrorKeys.XYZ_E_NO_CONTENT_IDS_PROVIDED_TO_PUBLISH_ZYX);

			String [] liveItemIds = getLiveItemIdsFromLocalIds(companyId, GroupMgr.getGlobalGroupId(), contentIds, IterKeys.CLASSNAME_JOURNALARTICLE);
						
			ErrorRaiser.throwIfNull(liveItemIds, IterErrorKeys.XYZ_E_NO_LIVE_ENTRY_FOR_CONTENT_ZYX);
			ErrorRaiser.throwIfFalse(liveItemIds.length > 0, IterErrorKeys.XYZ_E_NO_LIVE_ENTRY_FOR_CONTENT_ZYX);

			result = liveLocalService.publishToLive(companyId, GroupMgr.getGlobalGroupId(), userId, processId, 
													IterKeys.CLASSNAME_MILENIUMARTICLE, 
													liveItemIds, true, groupId);
		}
		catch(ServiceError se)
		{
			result = specialCase(IterErrorKeys.XYZ_E_PUBLISH_CONTENT_TO_LIVE_ZYX, se);
		}
		catch(Exception ex)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_PUBLISH_CONTENT_TO_LIVE_ZYX, ex));
		}
		catch (Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_PUBLISH_CONTENT_TO_LIVE_ZYX, new Exception(th)));
		}
		
		return result;
	}
		
	/**
	 * Publishes a layout in Milenium-Style (recursively with all PageContents and children)
	 */
	public String publishLayoutToLive(long companyId, long groupId, long userId, String processId, String [] layoutIds) throws Exception
	{
		String result = "";

		try
		{
			long scopeGroupId = groupId;
			String [] liveItemIds = getLiveItemIdsFromLocalIds(companyId, groupId, layoutIds, IterKeys.CLASSNAME_LAYOUT);
			result = liveLocalService.publishToLive(companyId, groupId, userId, processId, IterKeys.CLASSNAME_MILENIUMSECTION, liveItemIds, true, scopeGroupId);
		}
		catch(ServiceError e)
		{
			result = specialCase(IterErrorKeys.XYZ_E_PUBLISH_LAYOUT_TO_LIVE_ZYX, e);
		}
		catch(Exception ex)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_PUBLISH_LAYOUT_TO_LIVE_ZYX, ex));
		}
		catch (Throwable th)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_PUBLISH_LAYOUT_TO_LIVE_ZYX, new Exception(th)));
		}
		
		return result;
	}
	
	public void changeLiveStatus(long groupId, String classNameValue, String localId, String status) throws Exception{
		try
		{
			LiveLocalServiceUtil.changeLiveStatus(groupId, classNameValue, localId, status);
		}
		catch(Exception ex)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_CHANGE_LIVE_STATUS_ZYX, ex));
		}
	}
	
	public void populateLive(String groupName, long companyId) throws Exception
	{
		try
		{
			LiveLocalServiceUtil.populateLive(groupName, companyId);
		}
		catch(Exception ex)
		{
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_POPULATE_LIVE_ZYX, ex));
		}
	}
	
	public String [] getLiveItemIdsFromLocalIds(long companyId, long groupId, String [] localIds, String className) throws NoSuchLiveException, SystemException
	{
		ArrayList<String> liveIds = new ArrayList<String>();
		for (String localId : localIds)
		{
			Live liveItem = liveLocalService.getLiveByLocalId(groupId, className, localId);
			if(liveItem != null)
			{
				liveIds.add(String.valueOf(liveItem.getId()));
			}
		}
		return liveIds.toArray(new String[0]);
	}
	
	public String getPublicationListFlex(String globalGroupId, String groupId, String xmlFilters, String startIn, String limit, String sort) 
			throws ServiceError, SecurityException, NoSuchMethodException, DocumentException{
		String result = null;
		try
		{			
			result = liveLocalService.getPublicationListFlex(globalGroupId, groupId, xmlFilters, startIn, limit, sort);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		return result;
	}
	
	public String getPublicationDetailsFlex(String globalGroupId, String groupId, String liveId, String xmlFilters, String startIn, String limit, String sort) 
			throws ServiceError, SecurityException, NoSuchMethodException, DocumentException{
		String result = null;
		try
		{			
			result = liveLocalService.getPublicationDetailsFlex(globalGroupId, groupId, liveId, xmlFilters, startIn, limit, sort);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		return result;
	}
	
	public String getKeyFieldsFlex(String groupId, String globalGroupId, String publicationIds) throws PortalException, SystemException, DocumentException, ServiceError{
		String result = null;
		try
		{			
			result = liveLocalService.getKeyFieldsFlex(groupId, globalGroupId, publicationIds);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		return result;
	}
	
	public String publishToLiveSelectiveFlex(String xmlData) throws ServiceError, DocumentException{
		String result = null;
		try
		{			
			result = liveLocalService.publishToLiveSelectiveFlex(xmlData);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	public String publishToLiveMassiveFlex(long companyId, long groupId, long userId) throws Exception{
		String result = null;
		try
		{			
			result = publishGroupToLive(companyId, groupId, userId, null);
		}
		catch(ORMException orme)
		{
			ServiceErrorUtil.throwSQLIterException(orme);
		}
		
		return result;
	}
	
	// Nueva funci�n gen�rica de importaci�n
	public String publishToLive(String companyId, String groupId, String userId, String processId, String xml) throws Exception
	{
		String result = null;
		try
		{
			result = liveLocalService.publishToLive(companyId, groupId, userId, processId, xml);
		}
		catch(Throwable th)
        {
			throw new Exception(com.liferay.portal.kernel.error.ServiceErrorUtil.getServiceErrorAsXml(th));
        }
		return result;
	}
	
	// Funci�n para publicar cat�logos/p�ginas de cat�logos s�lo para Iter
	public String publishCatalogsIter(String xmlData) throws Exception
	{
		String result = null;
		try
		{
			result = liveLocalService.publishCatalogsIter(xmlData);		
		}
		catch(Throwable th)
		{
			throw new Exception(com.liferay.portal.kernel.error.ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String idGlobalToIdLocal(String xmlGlobalId) throws Exception
	{
		String result = null;
		try
		{
			result = liveLocalService.idGlobalToIdLocal(xmlGlobalId).asXML();		
		}
		catch(Throwable th)
		{
			_log.error(th);
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	public String localBackToLocalLive(String xmlLocalBackId) throws Exception
	{
		String result = null;
		try
		{
			result = liveLocalService.localBackToLocalLive(xmlLocalBackId);		
		}
		catch(Throwable th)
		{
			_log.error(th);
			throw new Exception(com.liferay.portal.kernel.error.ServiceErrorUtil.getServiceErrorAsXml(th));
		}
		return result;
	}
	
	
}