/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
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

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.error.ServiceErrorUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.plugin.Version;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.servlet.HttpMethods;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.DateFormatFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.InstrumentalContentUtil;
import com.liferay.portal.kernel.util.IterGlobalKeys;
import com.liferay.portal.kernel.util.IterURLUtil;
import com.liferay.portal.kernel.util.MegaSiteMapMgr;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.TimeZoneUtil;
import com.liferay.portal.kernel.util.URLSigner;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.velocity.IterVelocityTools;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.documentlibrary.NoSuchFileEntryException;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.service.BinaryRepositoryLocalServiceUtil;
import com.liferay.portlet.expando.model.ExpandoBridge;
import com.liferay.portlet.journal.NoSuchArticleException;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.model.JournalArticleConstants;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portlet.journal.service.persistence.JournalArticleUtil;
import com.liferay.portlet.journal.util.comparator.ArticleVersionComparator;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.news.service.DLFileEntryLocalServiceUtil;
import com.protecmedia.iter.news.service.base.JournalArticleLocalServiceBaseImpl;
import com.protecmedia.iter.news.util.EditArticleTools;
import com.protecmedia.iter.news.util.ExpandoUtil;
import com.protecmedia.iter.news.util.JournalArticleEraser;
import com.protecmedia.iter.news.util.JournalArticleIndexer;
import com.protecmedia.iter.news.util.TeaserContentUtil;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.journal.JournalArticleXmlIO;
import com.protecmedia.iter.xmlio.service.util.CacheRefresh;
import com.protecmedia.iter.xmlio.service.util.CacheRefreshMgr;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

/**
 * Es importante que sea SystemException para que NO se haga rollback de los ServiceError, por ejemplo
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {SystemException.class} )
public class JournalArticleLocalServiceImpl extends
		JournalArticleLocalServiceBaseImpl {

	private static Log _log = LogFactoryUtil
			.getLog(JournalArticleLocalServiceImpl.class);
	
	private static final String GET_SCOPEGROUPID_FROM_JOURNALARTICLE = new StringBuilder()
		.append("SELECT v.data_ \n")
		.append("FROM ExpandoValue v, ExpandoColumn c \n")
		.append("WHERE c.name = '%s' \n")
		.append("  AND v.columnId = c.columnId \n")
		// journalarticle.id_
		.append("  AND v.classpk = %s").toString();	

	// Map = ("0Fbxd23w_Image", bytes)
	public String addContent(long userId, long groupId, String structureId, String templateId, String title, String description,
							 String articleId, String content, Map<String, byte[]> images, ServiceContext serviceContext, 
							 boolean indexable, long[] mainAssetCategoryIds, boolean acceptComments, String xml) throws Exception 
	{
		String articleIdResult = "";
		Calendar now = Calendar.getInstance();
		Boolean multisite = Boolean.parseBoolean(PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_SEARCH_MULTISITE));

		if (!multisite || (multisite && groupId != GroupMgr.getGlobalGroupId())) 
		{
			userId = com.liferay.portal.kernel.util.GroupMgr.getDefaultUserId();
			serviceContext.setAddCommunityPermissions(acceptComments);
			
			// Comprobamos que no hay que realizar mapping del scopeGroupId
			String scopeGroupId = GroupMgr.getScopeGroupId(groupId);

			// Recuperamos el globalGroupId
			groupId = GroupMgr.getGlobalGroupId();

			// Añadimos el scopeGroupId como dato extendido del artículo para
			// que sea indexado
			serviceContext.setScopeGroupId(Long.parseLong(scopeGroupId));
			Map<String, Serializable> expandoAttributes = new HashMap<String, Serializable>();
			expandoAttributes.put(IterKeys.EXPANDO_COLUMN_NAME_SCOPEGROUPID,scopeGroupId);
			
			ExpandoUtil.checkExpandoRequirements(groupId, IterKeys.CLASSNAME_JOURNALARTICLE, IterKeys.EXPANDO_COLUMN_NAME_SCOPEGROUPID);
			if (mainAssetCategoryIds != null) 
			{
				expandoAttributes.put(WebKeys.EXPANDO_COLUMN_NAME_MAIN_METADATAS_IDS, StringUtils.join(ArrayUtil.toArray(mainAssetCategoryIds), ","));
				
				ExpandoUtil.checkExpandoRequirements(groupId, IterKeys.CLASSNAME_JOURNALARTICLE, WebKeys.EXPANDO_COLUMN_NAME_MAIN_METADATAS_IDS);
			}
			serviceContext.setExpandoBridgeAttributes(expandoAttributes);

			boolean hideAdv = false;
			
			if(Validator.isNotNull(xml))
			{
				Document d = SAXReaderUtil.read(xml);
				
				String modDate = XMLHelper.getStringValueOf(d, "/root/param/@modifieddate");
				setModifiedDate(modDate, serviceContext);
				
				hideAdv = GetterUtil.getBoolean( String.valueOf(XMLHelper.getLongValueOf(d, "/root/param/@hideadv")) );
			}
			else
				serviceContext.setModifiedDate( new Date() );
			
			JournalArticle journalArticle = JournalArticleLocalServiceUtil
					.addArticle(userId, Long.valueOf(scopeGroupId), articleId, true,
							JournalArticleConstants.DEFAULT_VERSION, title,
							description, content, "general", structureId,
							templateId, now.get(Calendar.MONTH),
							now.get(Calendar.DAY_OF_MONTH),
							now.get(Calendar.YEAR),
							now.get(Calendar.HOUR_OF_DAY),
							now.get(Calendar.MINUTE), 0, 0, 0, 0, 0, true, 0,
							0, 0, 0, 0, true, false, hideAdv, StringPool.BLANK,
							null, images, StringPool.BLANK, serviceContext);

			journalArticle.setIndexable(indexable);
			JournalArticleLocalServiceUtil.updateJournalArticle(journalArticle);
			JournalArticleLocalServiceUtil.updateStatus(userId, groupId, journalArticle.getArticleId(), journalArticle.getVersion(),
														WorkflowConstants.STATUS_APPROVED, title.toLowerCase().replace(" ", "-"), serviceContext);

			articleIdResult = journalArticle.getArticleId();
		}
		else
		{
			Exception ex = new Exception(IterErrorKeys.ITER_UPDATE_MILENIUM_ITER);
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(ex), ex);
		}

		return articleIdResult;
	}

	// Map = ("0Fbxd23w_Image", bytes)
	public String updateContent(long userId, long groupId, String structureId,
			String templateId, String title, String articleId, String content,
			Map<String, byte[]> images, boolean indexable, boolean acceptComments, String xml)	throws Exception 
	{
		Calendar now = Calendar.getInstance();
		
		userId = com.liferay.portal.kernel.util.GroupMgr.getDefaultUserId();

		// Recuperamos el globalGroupId
		groupId = GroupMgr.getGlobalGroupId();
		
		ServiceContext serviceContext = new ServiceContext();
		
		boolean hideAdv = false;
		
		if(Validator.isNotNull(xml))
		{
			Document d = SAXReaderUtil.read(xml);
			
			// Valida que Milenium sea compatible con el Repositorio de Documentos
			String versionIter = XMLHelper.getStringValueOf(d, "/root/param/@versioniter");
			if (Validator.isNull(versionIter) || Version.compare(versionIter, "18.0") < 0)
			{
				JournalArticle journalArticle = JournalArticleLocalServiceUtil.getArticle(groupId, articleId);
				Document xmlContent = SAXReaderUtil.read(journalArticle.getContent());
				int docsInRepository = xmlContent.selectNodes("//dynamic-element[@type='document_library' and starts-with(dynamic-content, '/binrepository/')]").size();
				ErrorRaiser.throwIfFalse(docsInRepository == 0, IterErrorKeys.XYZ_E_UPDATE_MILENIUM_ZYX, IterErrorKeys.ITER_UPDATE_MILENIUM_ITER);
			}
			
			String modDate = XMLHelper.getStringValueOf(d, "/root/param/@modifieddate");
			setModifiedDate(modDate, serviceContext);
			
			hideAdv = GetterUtil.getBoolean( String.valueOf(XMLHelper.getLongValueOf(d, "/root/param/@hideadv")) );
		}
		else
			serviceContext.setModifiedDate( new Date() );
		
		serviceContext.setAddCommunityPermissions(acceptComments);
		serviceContext.setAddGuestPermissions(true);
		serviceContext.setScopeGroupId(GroupMgr.getGlobalGroupId());

		// MILENIUM utiliza esta función y actualiza Live
		JournalArticle journalArticle = JournalArticleLocalServiceUtil
				.updateArticle(
						userId,
						groupId,
						articleId,
						JournalArticleLocalServiceUtil.getArticle(groupId,
								articleId).getVersion(), title,
						StringPool.BLANK, content, "general", structureId,
						templateId, now.get(Calendar.MONTH),
						now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.YEAR),
						now.get(Calendar.HOUR_OF_DAY),
						now.get(Calendar.MINUTE), 0, 0, 0, 0, 0, true, 0, 0, 0,
						0, 0, true, indexable, hideAdv, StringPool.BLANK, null,
						images, StringPool.BLANK, serviceContext);

		if(IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
			IterVelocityTools.checkHasToRefreshAboutsModel(articleId);
		
		try
		{
			// Si el artículo es instrumental ponemos su categoría a "PENDING" en la tabla xmlio_live
			if (IterKeys.STRUCTURE_INSTRUMENTAL.equals(structureId))
			{
				// Obtenemos el scopeGroupId del artículo
				long scopeGroupId = getScopeGroupIdFromJournalArticle(journalArticle.getId());
				
				String assetCategorycategoryId = InstrumentalContentUtil.getCategoryIdFromInstrumentalArticle(scopeGroupId, articleId);
				if (Validator.isNotNull(assetCategorycategoryId))
				{
					Live categoryLive = LiveLocalServiceUtil.getLiveByLocalId(groupId, IterKeys.CLASSNAME_CATEGORY, assetCategorycategoryId);
					categoryLive.setStatus(IterKeys.PENDING);
					LiveLocalServiceUtil.updateLive(categoryLive);				
				}
			}
		}
		catch(Exception e)
		{
			_log.error("Error updating the register of the assetcategory in xmlio_live", e);
		}
		
		return journalArticle.getArticleId();
	}

	private void setModifiedDate(String modDate, ServiceContext serviceContext) throws ParseException, ServiceError
	{
		ErrorRaiser.throwIfFalse( Validator.isNotNull(modDate), IterErrorKeys.XYZ_E_EMPTY_MODIFIED_DATE_ZYX);
		
		DateFormat df = DateFormatFactoryUtil.getSimpleDateFormat(IterKeys.DATEFORMAT_YYYY_MM_DD_HH_MM_ss, TimeZoneUtil.getTimeZone("UTC"));
		Date modifiedDate = df.parse( modDate );
		
		serviceContext.setModifiedDate( modifiedDate );
	}
	
	public void deleteContent(long groupId, String contentId) throws Exception 
	{
		deleteContent2(groupId, contentId, false, false);
	}

	public void deleteContent2(long groupId, String contentId, boolean deleteFileEntry, boolean deleteFromLive) throws Exception 
	{
		Exception eLive = null;
		boolean foundInLive = true;
		boolean foundInBack = true;

		if (deleteFromLive)
		{
			try 
			{
				// Se intenta borrar del LIVE
				foundInLive = deleteRemoteContent(groupId, contentId, deleteFileEntry);
			} 
			catch (Exception e) 
			{
				eLive = e;
			}
		}

		try
		{
			foundInBack = deleteJournalArticle(contentId, deleteFileEntry);
		}
		catch(Throwable th)
		{
			// Esta excepción SÍ se quiere que provoque un rollback
			throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_DELETE_CONTENT_ZYX, th), th);
		}

		// Esta excepción NO se quiere que provoque un rollback
		if (eLive != null)
			throw new Exception(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_DELETE_CONTENT_ZYX, eLive), eLive);

		// No se encuentra en ninguno de los dos sistemas. Esta excepción NO se quiere que provoque un rollback
		ErrorRaiser.throwIfFalse(!(!foundInLive && !foundInBack), IterErrorKeys.XYZ_E_CONTENT_NOT_FOUND_IN_BACK_LIVE_ZYX);

		// No se encuentra en el LIVE. Esta excepción NO se quiere que provoque un rollback
		ErrorRaiser.throwIfFalse(foundInLive, IterErrorKeys.XYZ_E_CONTENT_NOT_FOUND_IN_LIVE_ZYX);

		// No se encuentra en el BACK. Esta excepción NO se quiere que provoque un rollback
		ErrorRaiser.throwIfFalse(foundInBack, IterErrorKeys.XYZ_E_CONTENT_NOT_FOUND_IN_BACK_ZYX);
		
		if(IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
			IterVelocityTools.checkHasToRefreshAboutsModel(contentId);
	}

	public boolean deleteJournalArticle(String contentId, boolean deleteFileEntry) throws PortalException, SystemException, ServiceError, IOException, SQLException, SecurityException, NoSuchMethodException 
	{
		long groupId = GroupMgr.getGlobalGroupId();
		ServiceContext serviceContext = new ServiceContext();
		
		List<JournalArticle> articles = JournalArticleUtil.findByG_A(	groupId, contentId, QueryUtil.ALL_POS, 
																		QueryUtil.ALL_POS, new ArticleVersionComparator(true)
																	);
		boolean found = articles.size() > 0;
		
		for (JournalArticle article : articles) 
		{
			List<Long> scopeGroupIdsList = article.getScopeGroupIds();
			String articleStructureId = article.getStructureId();
			String assetCategorycategoryId = null;
			
			if( IterKeys.STRUCTURE_INSTRUMENTAL.equals(articleStructureId) && 
					IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
			{
				assetCategorycategoryId = InstrumentalContentUtil.getCategoryIdFromInstrumentalArticle(scopeGroupIdsList.get(0), contentId);
			}
			
			// Es IMPORTANTE que antes de hacer JournalArticleLocalServiceUtil.deleteArticle se borren los binarios, para que al borrarlos 
			// existan los expandos del artículo y se pueda determinar a la delegación a la que pertenece.
			// ITER-661	NAI172320 No se visualizan las fotos en la Web
			// http://jira.protecmedia.com:8080/browse/ITER-661?focusedCommentId=23726&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-23726
			if (deleteFileEntry)
			{
				// Borra todos los binarios del artículo
				deleteAllArticleBinaries(article);
				
				// Si estamos en el PREVIEW, se borran los binarios pendientes de eliminar en el LIVE.
				if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
					clearDeletePendingArticleBinaries(article.getArticleId());
			}
			
			JournalArticleLocalServiceUtil.deleteArticle(article, null, serviceContext, deleteFileEntry);
			
			if( IterKeys.STRUCTURE_INSTRUMENTAL.equals(articleStructureId) && 
					IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
			{
				String query = String.format("UPDATE sectionproperties SET aboutid=NULL, modifieddate='%s' WHERE aboutid='%s'", SQLQueries.getCurrentDate(), contentId);
				PortalLocalServiceUtil.executeUpdateQuery(query);
				
				if (Validator.isNotNull(assetCategorycategoryId))
				{
					Live categoryLive = LiveLocalServiceUtil.getLiveByLocalId(groupId, IterKeys.CLASSNAME_CATEGORY, assetCategorycategoryId);
					categoryLive.setStatus(IterKeys.PENDING);
					LiveLocalServiceUtil.updateLive(categoryLive);				
				}
			}

			// ITER-351	Proporcionar a MLN la posibilidad para eliminar artículos completamente del PREVIEW y del LIVE
			// No se notifica a los apaches los binarios, porque no van a refrescarse todos los encuadres, etc
//			boolean notifyDelete= found && deleteFileEntry && IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_LIVE);
//			if (notifyDelete)
//			{
//				for(long scopeGroupId : scopeGroupIdsList)
//				{
//					DLFileEntryLocalServiceUtil.notifyDeleteFileEntryToApaches(article, scopeGroupId);
//					DLFileEntryLocalServiceUtil.notifyDeleteBinaryToApaches(article, scopeGroupId);
//				}
//			}
		}
		
		return found;
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public boolean deleteRemoteContent(long groupId, String contentId, boolean deleteFileEntry) throws 	PortalException, SystemException, 
																										UnsupportedEncodingException, ClientProtocolException, IOException, ServiceError 
	{
		boolean found = false;
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW)) 
		{
			long companyId = GroupLocalServiceUtil.getGroup(GroupMgr.getGlobalGroupId()).getCompanyId();

			LiveConfiguration liveConf = LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(companyId);
			String remoteIP = liveConf.getRemoteIterServer2().split(":")[0];
			int remotePort = Integer.valueOf(liveConf.getRemoteIterServer2().split(":")[1]);
			String remoteMethodPath = "/news-portlet/secure/json";

			List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
			remoteMethodParams.add(new BasicNameValuePair("serviceClassName",	"com.protecmedia.iter.news.service.JournalArticleServiceUtil"));
			remoteMethodParams.add(new BasicNameValuePair("serviceMethodName",	"deleteJournalArticle"));
			remoteMethodParams.add(new BasicNameValuePair("serviceParameters",	"[contentId, deleteFileEntry]"));
			remoteMethodParams.add(new BasicNameValuePair("contentId",			contentId));
			remoteMethodParams.add(new BasicNameValuePair("deleteFileEntry",	Boolean.toString(deleteFileEntry)));

			String result = XMLIOUtil.executeJSONRemoteMethod2(companyId, remoteIP, remotePort, liveConf.getRemoteUserName(),
															   liveConf.getRemoteUserPassword(), remoteMethodPath,remoteMethodParams);
			JSONObject json = JSONFactoryUtil.createJSONObject(result);

			String errorMsg = json.getString("exception");
			if (!errorMsg.isEmpty()) 
			{
				// Puede ser una excepción de tipo Iter, si no lo es devuelve
				// todo el texto y también se lanza porque era una excepción del
				// sistema
				String iterErrorMsg = ServiceErrorUtil.containIterException(errorMsg);
				throw new SystemException(iterErrorMsg.isEmpty() ? errorMsg : iterErrorMsg);
			}

			found = Boolean.parseBoolean(json.getString("returnValue"));
		}
		return found;
	}

	/**
	 * ITER-351 Proporcionar a MLN la posibilidad para eliminar artículos completamente del PREVIEW y del LIVE
	 * @param articleId ID del artículo a eliminar de ambos entornos
	 * @throws Exception 
	 */
	public void deleteContentFromArticleId(String articleId) throws Exception
	{
		boolean foundInLive = deleteRemoteContentFromArticleId(articleId);
		boolean foundInBack = deleteJournalArticle(articleId, true);

		// No se encuentra en ninguno de los dos sistemas. Esta excepción NO se quiere que provoque un rollback
		ErrorRaiser.throwIfFalse(!(!foundInLive && !foundInBack), IterErrorKeys.XYZ_E_CONTENT_NOT_FOUND_IN_BACK_LIVE_ZYX);

		// No se encuentra en el LIVE. Esta excepción NO se quiere que provoque un rollback
		ErrorRaiser.throwIfFalse(foundInLive, IterErrorKeys.XYZ_E_CONTENT_NOT_FOUND_IN_LIVE_ZYX);

		// No se encuentra en el BACK. Esta excepción NO se quiere que provoque un rollback
		ErrorRaiser.throwIfFalse(foundInBack, IterErrorKeys.XYZ_E_CONTENT_NOT_FOUND_IN_BACK_ZYX);
		
	}
	
	private boolean deleteRemoteContentFromArticleId(String articleId) throws Exception
	{
		boolean found = false;
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW)) 
		{
			List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
			remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.news.service.JournalArticleServiceUtil"));
			remoteMethodParams.add(new BasicNameValuePair("serviceMethodName",	"deleteJournalArticleAndRefresh"));
			remoteMethodParams.add(new BasicNameValuePair("serviceParameters",	"[articleId, urls]"));
			remoteMethodParams.add(new BasicNameValuePair("articleId", 			articleId));
			remoteMethodParams.add(new BasicNameValuePair("urls", 				""));

			found = Boolean.parseBoolean( JSONUtil.executeMethod("/news-portlet/secure/json", remoteMethodParams).getString("returnValue") );
			
			// Si se ha borrado correctamente el contenido del entorno LIVE, se actualiza la caché de los Apaches
			//if (found)
		}
		return found;
	}
	
	/**
	 * 
	 * @param url
	 * @param deleteFileEntry
	 * @throws Exception
	 */
	public void deleteContentFromPublicURL(String url, boolean deleteFileEntry) throws Exception
	{
		boolean foundInLive = false;
		boolean foundInBack = false;

		// Lo primero es llamar al LIVE, que es quien analiza la URL y nos devuelve la info del elemento a borrar
		Document dom = deleteRemoteContentFromPublicURL(url, deleteFileEntry);
		
		String type = XMLHelper.getTextValueOf(dom, "/rs/@type", IterKeys.CLASSNAME_UNKNOWN);
		ErrorRaiser.throwIfFalse(type.equals(IterKeys.CLASSNAME_JOURNALARTICLE) || type.equals(IterKeys.CLASSNAME_DLFILEENTRY), IterErrorKeys.XYZ_E_INVALID_URL_ZYX);
		
		foundInLive = Boolean.parseBoolean( XMLHelper.getTextValueOf(dom, "/rs/@found", "false") );
		
		if (type.equals(IterKeys.CLASSNAME_JOURNALARTICLE))
		{
			String contentId = XMLHelper.getTextValueOf(dom, "/rs/article/@contentId");
			ErrorRaiser.throwIfNull(contentId, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
			
			foundInBack = deleteJournalArticle(contentId, deleteFileEntry);
		}
		else
		{
			String groupName = XMLHelper.getTextValueOf(dom, "/rs/fileentry/@groupName");
			ErrorRaiser.throwIfNull(groupName, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
			
			long folderId = XMLHelper.getLongValueOf(dom, "/rs/fileentry/@folderId");
			ErrorRaiser.throwIfNull(folderId, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX);
			
			String title = XMLHelper.getTextValueOf(dom, "/rs/fileentry/@title");
			ErrorRaiser.throwIfNull(title);
			
			long companyId 	= GroupLocalServiceUtil.getGroup( GroupMgr.getGlobalGroupId() ).getCompanyId();
			long groupId	= GroupLocalServiceUtil.getGroup(companyId, groupName).getGroupId();
			try
			{
				DLFileEntry dlFileEntry = com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil.getFileEntryByTitle(groupId, folderId, title);
				ServiceContext serviceContext = new ServiceContext();
				serviceContext.setDelegationId(GroupLocalServiceUtil.getGroup(groupId).getDelegationId());
				com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil.deleteFileEntry(dlFileEntry, serviceContext);
				foundInBack = true;
			}
			catch (NoSuchFileEntryException e)
			{
				_log.debug(e.toString());
			}
			catch(Throwable th)
			{
				// Esta excepción SÍ se quiere que provoque un rollback
				throw new SystemException(ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_DELETE_CONTENT_ZYX, th), th);
			}
		}
		
		// No se encuentra en ninguno de los dos sistemas. Esta excepción NO se quiere que provoque un rollback
		ErrorRaiser.throwIfFalse(!(!foundInLive && !foundInBack), IterErrorKeys.XYZ_E_CONTENT_NOT_FOUND_IN_BACK_LIVE_ZYX);

		// No se encuentra en el LIVE. Esta excepción NO se quiere que provoque un rollback
		ErrorRaiser.throwIfFalse(foundInLive, IterErrorKeys.XYZ_E_CONTENT_NOT_FOUND_IN_LIVE_ZYX);

		// No se encuentra en el BACK. Esta excepción NO se quiere que provoque un rollback
		ErrorRaiser.throwIfFalse(foundInBack, IterErrorKeys.XYZ_E_CONTENT_NOT_FOUND_IN_BACK_ZYX);
	}

	/**
	 * 
	 */
	public Document deleteContentFromURL(String url, boolean deleteFileEntry) throws Exception 
	{
		// Se determina si es una URL de JA o de DLFileEntry
		String currentURL = HttpUtil.removeDomain( HttpUtil.fixPath(  PortalUtil.getCurrentURL(url)   ) );

		// Se intenta obtener el contentId a partir de la URL
		String contentId = "";
		String searchSeparator 	= PortalUtil.getSearchSeparator();
		
		if (Validator.isNotNull(currentURL) && !currentURL.startsWith("/widget/") && !currentURL.contains(searchSeparator)) 
		{
			String urlSeparator		= PortalUtil.getUrlSeparator();

			if (currentURL.contains("?")) 
			{
				currentURL = currentURL.substring(0, currentURL.indexOf('?'));
			}

			if ( !currentURL.contains(urlSeparator) && PropsValues.ITER_SEMANTICURL_ENABLED  )
			{
				//url semánticas activadas y currentURL es url semántica
				int articleSeparatorPos = currentURL.lastIndexOf(StringPool.MINUS);
				
				// El articleId está separado por un "-" al final del todo
				if (articleSeparatorPos >= 0)
				{
					String articleId = currentURL.substring(articleSeparatorPos + StringPool.MINUS.length());
					
					// El articleId, para que sea válido, estará compuesto por un CRC de dos dígitos + articleId,
					// Si articleId es del tipo 'CG27023#.VFyHeRaNF3A',se descarta todo lo que hay a partir de #
					if ( articleId.length() > IterURLUtil.ARTICLEID_CRC_LENGTH)
					{
						String crc 	= articleId.substring(0, IterURLUtil.ARTICLEID_CRC_LENGTH);
						
						if(articleId.contains("#"))
							articleId =	articleId.substring(IterURLUtil.ARTICLEID_CRC_LENGTH, articleId.indexOf('#'));
						else
							articleId 	= articleId.substring(IterURLUtil.ARTICLEID_CRC_LENGTH);
						
						// El CRC que viene en la URL tiene que coincidir con el calculado
						if ( crc.equals(URLSigner.generateSign(articleId, IterURLUtil.ARTICLEID_CRC_LENGTH)) )
						{
							contentId  = articleId;
						}
					}				
				}
				
			}
			else if ( currentURL.contains(urlSeparator) && !PropsValues.ITER_SEMANTICURL_ENABLED ) 
			{
				//url semánticas no activadas y currentURL es url friendly
				int pos = currentURL.indexOf(urlSeparator);
				String auxUrl = currentURL.substring(pos + urlSeparator.length());

				if (!auxUrl.equals("")) 
				{
					int fisrtSlashIdx = auxUrl.indexOf("/") != -1 ? auxUrl.indexOf("/") : auxUrl.length();

					if (!auxUrl.substring(0, fisrtSlashIdx).equals("date") && !auxUrl.substring(0, fisrtSlashIdx).equals("meta")) 
					{
						String[] urlString = auxUrl.split("/");

						if (urlString.length <= 3 && urlString.length > 0)
							contentId = urlString[0];
					}
				}
			}
		}

		Document document = null;
		if (contentId.length() > 0) 
		{
			boolean found = deleteJournalArticleAndRefresh(contentId, new String[]{url});

			document = SAXReaderUtil.createDocument();

			Element rootElement = document.addElement("rs");
			rootElement.addAttribute("type",  IterKeys.CLASSNAME_JOURNALARTICLE);
			rootElement.addAttribute("found", Boolean.toString(found));

			Element elemArticle = rootElement.addElement("article");
			elemArticle.addAttribute("contentId", contentId);
		} 
		else 
		{
			// La URL podría corresponder a un DLFileEntry, se intenta borrar
			document = DLFileEntryLocalServiceUtil.deleteFileEntryFromURL(url);
		}

		return document;
	}
	
	public boolean deleteJournalArticleAndRefresh(String articleId, String[] urls) throws Exception
	{
		boolean found 			= false;
		List<Long> groupList 	= null;
		
		_log.info( String.format("deleteJournalArticleAndRefresh(%s): Start", articleId) );
		
		try
		{
			try
			{
				if (!PropsValues.IS_PREVIEW_ENVIRONMENT)
				{
					// Se invalidan todos los sitios que contengan el artículo
					// http://jira.protecmedia.com:8080/browse/ITER-1068?focusedCommentId=46304&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-46304
					JournalArticle article = JournalArticleLocalServiceUtil.getArticle(com.liferay.portal.kernel.util.GroupMgr.getGlobalGroupId(), articleId);
					groupList = article.getScopeGroupIds();
				}
					
				// Se llama a un hilo que relice el borrado del artículo para garantizar que se hará en otra transacción
				// Así al refrescar las URLs se detectarán como 404
				// http://jira.protecmedia.com:8080/browse/ITER-351?focusedCommentId=14444&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-14444
				found = JournalArticleEraser.erase(articleId, true);
			}
			catch (NoSuchArticleException nse)
			{
				_log.debug(nse);
			}
			
			// http://jira.protecmedia.com:8080/browse/ITER-351?focusedCommentId=14569&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-14569
			if (found && Validator.isNotNull(groupList))
			{
				// NO se recalcula la invalidación de cachés programadas porque este mecanismo se basa en el LastPublicationDate de los grupos y dicha fecha NO se ha modificado
				CacheRefresh cr = new CacheRefresh(groupList);
				cr.setRescheduleCacheInvalidation(false);
				CacheRefreshMgr.refresh(cr);
			}
		}
		finally
		{
			_log.info( String.format("deleteJournalArticleAndRefresh(%s): Finish", articleId) );
		}
		
		return found;
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private Document deleteRemoteContentFromPublicURL(String url, boolean deleteFileEntry) throws 	PortalException, SystemException, UnsupportedEncodingException, 
																									ClientProtocolException, IOException, DocumentException, ServiceError 
	{
		Document dom = null;
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW)) 
		{
			long companyId = GroupLocalServiceUtil.getGroup(GroupMgr.getGlobalGroupId()).getCompanyId();

			LiveConfiguration liveConf = LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(companyId);
			String remoteIP = liveConf.getRemoteIterServer2().split(":")[0];
			int remotePort = Integer.valueOf(liveConf.getRemoteIterServer2().split(":")[1]);
			String remoteMethodPath = "/news-portlet/secure/json";

			List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
			remoteMethodParams.add(new BasicNameValuePair("serviceClassName", 	"com.protecmedia.iter.news.service.JournalArticleServiceUtil"));
			remoteMethodParams.add(new BasicNameValuePair("serviceMethodName",	"deleteContentFromURL"));
			remoteMethodParams.add(new BasicNameValuePair("serviceParameters",	"[url, deleteFileEntry]"));
			remoteMethodParams.add(new BasicNameValuePair("url", url));
			remoteMethodParams.add(new BasicNameValuePair("deleteFileEntry", Boolean.toString(deleteFileEntry)));

			String result = XMLIOUtil.executeJSONRemoteMethod2(companyId, remoteIP, remotePort, liveConf.getRemoteUserName(),
					liveConf.getRemoteUserPassword(), remoteMethodPath, remoteMethodParams);
			JSONObject json = JSONFactoryUtil.createJSONObject(result);

			String errorMsg = json.getString("exception");
			if (!errorMsg.isEmpty())
			{
				// Puede ser una excepción de tipo Iter, si no lo es devuelve
				// todo el texto y también se lanza porque era una excepción del
				// sistema
				String iterErrorMsg = ServiceErrorUtil.containIterException(errorMsg);
				throw new SystemException(iterErrorMsg.isEmpty() ? errorMsg : iterErrorMsg);
			}

			dom = SAXReaderUtil.read(json.getString("returnValue"));
		}
		return dom;
	}

	public void updateMainCategoriesIds(long groupId, JournalArticle ja, long[] mainAssetCategoryIds) throws PortalException, SystemException 
	{
		Map<String, Serializable> expandoAttributes = new HashMap<String, Serializable>();
		expandoAttributes.put(WebKeys.EXPANDO_COLUMN_NAME_MAIN_METADATAS_IDS, StringUtils.join(ArrayUtil.toArray(mainAssetCategoryIds), ","));
		ExpandoUtil.checkExpandoRequirements(groupId, IterKeys.CLASSNAME_JOURNALARTICLE, WebKeys.EXPANDO_COLUMN_NAME_MAIN_METADATAS_IDS);
		ExpandoBridge expandoBridge = ja.getExpandoBridge();
		expandoBridge.setAttributes(expandoAttributes);
		JournalArticleXmlIO jaXmlio = new JournalArticleXmlIO();
		
		try 
		{
			jaXmlio.updateExpandoPool(ja);
		} 
		catch (Exception err) 
		{
			_log.debug("Expando data could not be added as dependency of article " + ja.getArticleId());
		}
	}
	
	// Obtiene el scropeGroupId con un journalarticleId
	public long getScopeGroupIdFromJournalArticle(long journalarticleId_) throws ServiceError
	{		
		String sql = String.format(GET_SCOPEGROUPID_FROM_JOURNALARTICLE, IterKeys.EXPANDO_COLUMN_NAME_SCOPEGROUPID, journalarticleId_);
		
		if (_log.isDebugEnabled())
			_log.debug(new StringBuilder("Query to get scopeGroupId from journalarticle:\n").append(sql));		
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList(sql);
		
		ErrorRaiser.throwIfFalse(null != result && result.size() == 1, IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, 
						new StringBuilder("ScopegroupId not found to journalarticle.articleid ").append(journalarticleId_).toString());
		
		return Long.parseLong(((String)result.get(0)));
	}
/**********************************************************************************/
/************************	Indexacion de articulos	*******************************/
/**********************************************************************************/
	private ReentrantLock _lock = new ReentrantLock();
	private ExecutorService _executorService = null;
	private JournalArticleIndexer jaIndexer = null;
	private Future<JournalArticleIndexer> futureJorArtIndexer = null;
	
	@SuppressWarnings("unchecked")
	public Document reIndexJournalArticles(String xmlData) throws ServiceError, DocumentException, InterruptedException, ExecutionException, PortalException, SystemException
	{
		_lock.lock();
		
		Document d = SAXReaderUtil.read("<rs/>");
		
		ErrorRaiser.throwIfNull(xmlData, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Element root = SAXReaderUtil.read(xmlData).getRootElement();
		
		long scopeGroupId = XMLHelper.getLongValueOf(root, "/rs/row/@scopegroupid");
		int packageSize = (int) XMLHelper.getLongValueOf(root, "/rs/row/@packagesize");
		int commitWithIn = (int) XMLHelper.getLongValueOf(root, "/rs/row/@committime");
		
		ErrorRaiser.throwIfFalse(scopeGroupId>0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX);
		ErrorRaiser.throwIfFalse(packageSize>=0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse(commitWithIn>=0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		commitWithIn = commitWithIn*60000;
		
		try
		{
			if( futureJorArtIndexer==null || futureJorArtIndexer.isDone() )
			{
				_log.debug("Ready to index articles.");
				
				if(_executorService==null)
					_executorService = Executors.newSingleThreadExecutor();
				
				if(_log.isDebugEnabled())
					_log.debug("packageSize: " + packageSize + " commitWithIn: " + commitWithIn +" ms.");
				
				jaIndexer = new JournalArticleIndexer(scopeGroupId, packageSize, commitWithIn);
				futureJorArtIndexer = (Future<JournalArticleIndexer>) _executorService.submit( jaIndexer );
				d = getIndexingProgress();
			}
			else
				_log.debug("Indexation in progress.");
		}
		finally
		{
			_lock.unlock();
		}
		
		return d;
	}
	
	public void deleteIndexedArticles(String scopeGroupId) throws MalformedURLException, IOException, com.liferay.portal.kernel.error.ServiceError, NumberFormatException, PortalException, SystemException
	{
		_lock.lock();
		
		try
		{
			ErrorRaiser.throwIfFalse( (futureJorArtIndexer==null || futureJorArtIndexer.isDone()) , com.liferay.portal.kernel.error.IterErrorKeys.XYZ_E_INDEXATION_IN_PROGRESS_ZYX);
			
			String solrEndpoint = PropsUtil.get(IterGlobalKeys.PORTAL_PROPERTIES_KEY_ITER_SEARCH_PLUGIN_ENDPOINT);
			
			if( Validator.isNotNull(solrEndpoint) )
			{
				if (solrEndpoint.endsWith("/"))
					solrEndpoint = solrEndpoint.substring(0, solrEndpoint.length() - 1);

				long delegationId = GroupLocalServiceUtil.getGroup( Long.valueOf(scopeGroupId) ).getDelegationId();
				String solrQuery = String.format("%s/update?stream.body=<delete><query>%s:%s</query></delete>&commit=true", delegationId!=0?delegationId:StringPool.BLANK, Field.SCOPE_GROUP_ID, scopeGroupId);
				String url = solrEndpoint.concat( solrQuery );
				_log.debug("solr url: " + url);
				
				HttpURLConnection httpConnection = null;
				
				httpConnection = (HttpURLConnection)(new URL(url).openConnection());
		        httpConnection.setConnectTimeout( PropsValues.ITER_DELETESOLR_CONEXIONTIMEOUT );
		        httpConnection.setReadTimeout(	 PropsValues.ITER_DELETESOLR_RESPONSETIMEOUT);
		       	httpConnection.setRequestMethod( HttpMethods.GET );
		       
		        httpConnection.connect();
		        
		        HttpUtil.throwIfConnectionFailed(httpConnection, com.liferay.portal.kernel.error.IterErrorKeys.XYZ_E_DELETE_SOLR_URLCONNECTION_FAILED_ZYX);
		        
		        _log.debug("Delete SOLR database response code: " + httpConnection.getResponseCode());
			}
			else
				_log.debug("SOLR end point is empty.");
		}
		finally
		{
			_lock.unlock();
		}
	}
	
	public Document getIndexingProgress() throws DocumentException, InterruptedException, ExecutionException
	{
		Document d = SAXReaderUtil.read("<rs/>");
		
		_lock.lock();
		
		try
		{
			if( jaIndexer!=null && futureJorArtIndexer!=null && !futureJorArtIndexer.isDone() )
			{
				d = jaIndexer.getIndexingStatus();
			}
			else
				_log.debug("No progress info.");
		}
		finally
		{
			_lock.unlock();
		}
		
		return d;
	}
	
	public boolean stopIndexation() throws InterruptedException
	{
		boolean stop = false;
		
		_lock.lock();
		
		try
		{
			if(_executorService!=null && !_executorService.isTerminated() && futureJorArtIndexer!=null && !futureJorArtIndexer.isDone())
			{
				_executorService.shutdownNow();
				
				stop = _executorService.awaitTermination(PropsValues.ITER_STOPINDEXATION_TIMEOUT, TimeUnit.SECONDS);
			}
			else
				if(_log.isDebugEnabled())
				{
					_log.debug("Stop indexation. Thread is terminated: " + (_executorService!=null && _executorService.isTerminated()) +
									". Indexations is done: " + (futureJorArtIndexer!=null && futureJorArtIndexer.isDone()) );
				}
		}
		finally
		{
			if ( !stop )
			{
				if( _executorService!=null && !_executorService.isTerminated() && futureJorArtIndexer!=null && !futureJorArtIndexer.isDone() )
					_log.error("Impossible to stop. Indexation is still in progress");
				else
					if(_log.isDebugEnabled())
					{
						_log.debug("Stop indexation. Thread is terminated: " + (_executorService!=null && _executorService.isTerminated()) +
										". Indexations is done: " + (futureJorArtIndexer!=null && futureJorArtIndexer.isDone()) );
					}
			}
			else
				_log.info("Indexing of articles stopped by user.");
			
			jaIndexer = null;
			futureJorArtIndexer = null;
			_executorService = null;
			
			_lock.unlock();
		}
		
		return stop;
	}
	
	public Document getEditArticle(String articleId) throws Exception
	{
		Document retVal = SAXReaderUtil.read("<root/>");
		
		EditArticleTools eat = new EditArticleTools(articleId);
		retVal = eat.getEditArticle();
		
		return retVal;
	}

	private void deleteAllArticleBinaries(JournalArticle article) throws PortalException, SystemException, ServiceError
	{
		try
		{
			List<String> titleList = XMLIOUtil.getWebContentBinaryTitles(article);
			String delegationId	= String.valueOf(article.getDelegationId());
			for (String title : titleList)
			{
				BinaryRepositoryLocalServiceUtil.deleteBinaryByTitle(title, delegationId);
			}
		}
		catch (DocumentException e)
		{
			_log.error("Can't retrieve article binaries to delete");
		}
	}
	
	private void clearDeletePendingArticleBinaries(String articleId) throws PortalException, SystemException, ServiceError, IOException, SQLException
	{
		String sql = String.format("DELETE FROM binariesdeleted WHERE articleId = '%s'", articleId);
		PortalLocalServiceUtil.executeUpdateQuery(sql);
	}
	
	/**
	 * Es necesario llamar desde el núcleo (<code>IterTemplateContent._getRelatedMetaArticles</code>) a la funcionalidad 
	 * <code>getMetadatasLinks</code>.<br/>
	 * Pasar la funcionalidad al núcleo implicaría modificar muchísimos ficheros, con el riesgo que implica así que se invoca
	 * en tiempo de ejecución; pero desde un jar externo al proyecto <i>News_Portlet</i> NO se puede invocar a 
	 * <code>com.protecmedia.iter.news.util.TeaserContentUtil.getMetadatasLinks</code> porque está en el WAR, y pasarlo al JAR
	 * sería igual de arriesgado.<br/><br/>
	 * 
	 * Por ello se crea el método <code>com.protecmedia.iter.news.service.util.getMetadatasLinks</code>, para que sea invocado 
	 * desde el núcleo, y sirva de puente hacia <code>com.protecmedia.iter.news.util.TeaserContentUtil.getMetadatasLinks</code>.
	 *
	 * @param companyId
	 * @param groupId
	 * @param contentId
	 * @param structures
	 * @param startIndex
	 * @param numElements
	 * @param validityDate
	 * @param showNonActiveContents
	 * @param orderFields
	 * @param typeOrder
	 * @return
	 */
	public List<String[]> getMetadatasLinks(long companyId, long groupId, String contentId, 
								String[] structures, int startIndex, int numElements, 
								Date validityDate, boolean showNonActiveContents, 
								String[] orderFields, int typeOrder)
	{
		List<String> stringList = new ArrayList<String>(Arrays.asList(structures));
		
		return TeaserContentUtil.getMetadatasLinks(companyId, groupId, contentId, stringList, startIndex, numElements, validityDate, 
				showNonActiveContents, orderFields, typeOrder);
	}

	/**********************************************************************************/
	/***********************	Generación del megasitemap	***************************/
	/**
	 * @throws Exception ********************************************************************************/
	@SuppressWarnings("unchecked")
	public Document buildMegasitemap(String xmlData) throws Exception
	{
		ErrorRaiser.throwIfNull(xmlData, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Element root = SAXReaderUtil.read(xmlData).getRootElement();
		
		long groupId = XMLHelper.getLongValueOf(root, "@scopegroupid");
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALID_SCOPE_GROUP_ID_ZYX);
		
		String sitemapXslId = XMLHelper.getTextValueOf(root, "@sitemapxsl", "");
		
		MegaSiteMapMgr.setMegasitemapConfig(groupId, sitemapXslId);
		
		MegaSiteMapMgr.startSitemapBuild(groupId);
		
		return MegaSiteMapMgr.getSitemapBuildStatus(groupId);
	}
	
	public void deleteMegasitemap(long groupId) throws NumberFormatException, PortalException, SystemException, ServiceError, IOException, SQLException
	{
		MegaSiteMapMgr.deleteSitemap( groupId );
	}

	public Document getMegasitemapBuildStatus(long groupId) throws DocumentException, NumberFormatException, ServiceError
	{
		return MegaSiteMapMgr.getSitemapBuildStatus( groupId );
	}
	
	public boolean stopMegasitemapBuild(long groupId) throws NumberFormatException, PortalException, SystemException, ServiceError, InterruptedException
	{
		return MegaSiteMapMgr.stopSitemapBuild( groupId );
	}
	
	public Document getMegasitemapConfig(long groupId) throws Exception
	{
		return MegaSiteMapMgr.getMegasitemapConfig( groupId );
	}
	
	public String createInstrumentalArticle(Long groupId, String title, String content) throws Exception
	{
		return addContent(GroupMgr.getDefaultUserId(), groupId, "INSTRUMENTAL_ARTICLE", "INSTRUMENTAL_TEMPLATE", title, StringPool.BLANK, 
			StringPool.BLANK, content, null, new ServiceContext(), false, null, false, StringPool.BLANK);
		
	}
	
	public String updateInstrumentalArticle(String articleId, String title, String content) throws Exception
	{
		return updateContent(GroupMgr.getDefaultUserId(), GroupMgr.getGlobalGroupId(), "INSTRUMENTAL_ARTICLE", "INSTRUMENTAL_TEMPLATE",
			title, articleId, content, null, false, false, StringPool.BLANK);
	}
	
	public void deleteInstrumentalArticle(String articleId) throws Exception
	{
		deleteJournalArticle(articleId, true);
	}
}