/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.portal.service;

import java.io.File;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.model.JournalStructureConstants;
import com.liferay.portlet.journal.service.JournalArticleLocalService;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceWrapper;
import com.liferay.portlet.journal.service.persistence.JournalArticleUtil;
import com.liferay.portlet.journal.util.comparator.ArticleVersionComparator;
import com.liferay.portlet.polls.service.PollsQuestionLocalServiceUtil;
import com.liferay.util.survey.IterSurveyModel;
import com.liferay.util.survey.IterSurveyUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceErrorUtil;
import com.protecmedia.iter.news.service.ArticlePollLocalServiceUtil;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.journal.JournalArticleXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

public class MyJournalArticleLocalService extends
		JournalArticleLocalServiceWrapper {
	
	private static Log _log = LogFactoryUtil.getLog(MyJournalArticleLocalService.class);
	private static ItemXmlIO itemXmlIO = new JournalArticleXmlIO();
	
	private String DELETE_NEWS_COUNTERS_BY_CONTENTID = "DELETE FROM News_Counters WHERE contentId='%s'";
	
	public MyJournalArticleLocalService(
			JournalArticleLocalService journalArticleLocalService) {
		super(journalArticleLocalService);
	}

	
	/*
	 * Add Functions 
	 * --------------
	 * 
	 * Live table:
	 *  GlobalId -> articleId
	 *  LocalId -> articleId
	 *  NOTA: Lanzamos las excepciones a las capas superiores, para que sean gestionadas y se puedan
	 *  mostrar correctamente en la GUI
	 */	
	@Override
	public JournalArticle addArticle(long userId, long groupId,
			String articleId, boolean autoArticleId, double version,
			String title, String description, String content, String type,
			String structureId, String templateId, int displayDateMonth,
			int displayDateDay, int displayDateYear, int displayDateHour,
			int displayDateMinute, int expirationDateMonth,
			int expirationDateDay, int expirationDateYear,
			int expirationDateHour, int expirationDateMinute,
			boolean neverExpire, int reviewDateMonth, int reviewDateDay,
			int reviewDateYear, int reviewDateHour, int reviewDateMinute,
			boolean neverReview, boolean indexable, boolean smallImage,
			String smallImageURL, File smallFile, Map<String, byte[]> images,
			String articleURL, ServiceContext serviceContext)
					throws PortalException, SystemException
	{
		
			JournalArticle article = null;
			
			// Recuperamos el globalGroupId
			long globalGroupId = GroupMgr.getGlobalGroupId();
			
			try
			{
			
				if ((articleId == null || "".equals(articleId)) && autoArticleId)
				{
					articleId = "" + CounterLocalServiceUtil.increment();
				}
				
				// Si está habilitado el nuevo sistema de encuestas y llega un artículo con estructura STANDARD-POLL, lanza un error por incompatibilidad
				IterSurveyUtil.checkCompatibility(structureId, groupId);
	
				/*
				 * CREATE LIFERAY POLL
				 */
				boolean iterSurveysEnabled = IterSurveyUtil.isEnabled(groupId);
				if (!iterSurveysEnabled && structureId.equals("STANDARD-POLL")) 
				{
					long questionId = ArticlePollLocalServiceUtil.createPoll(title, userId, globalGroupId, 
																			 content, expirationDateMonth, expirationDateDay,
																			 expirationDateYear, expirationDateHour,
																			 expirationDateMinute, neverExpire);
				
					ErrorRaiser.throwIfFalse(questionId != -1, IterErrorKeys.XYZ_E_QUESTIONPOLL_NOT_WELLFORMED_ZYX, "At least two choices needed");
					
					article = super.addArticle(userId, globalGroupId, articleId, false, version, title, description, content, type, 
											   structureId, templateId, displayDateMonth, displayDateDay, displayDateYear, 
											   displayDateHour, displayDateMinute, expirationDateMonth, expirationDateDay, 
											   expirationDateYear, expirationDateHour, expirationDateMinute, neverExpire,
											   reviewDateMonth, reviewDateDay, reviewDateYear, reviewDateHour, reviewDateMinute, 
											   neverReview, indexable, smallImage, smallImageURL, smallFile, images, articleURL, 
											   serviceContext);
					
					ArticlePollLocalServiceUtil.addArticlePoll(globalGroupId, article.getArticleId(), questionId);
					
					//Add to live
					itemXmlIO.createLiveEntry(article);
						
				
				}
				//Cualquier otro tipo de contenido
				else
				{
					// Si está habilitado el nuevo sistema de encuestas y contiene encuestas válidas,
					// se establece el structureId a STANDARD-POLL y el templateId a FULL-CONTENT-POLL
					List<IterSurveyModel> surveys = null;
					if (iterSurveysEnabled && (surveys = IterSurveyModel.processArticleSurveys(articleId, content)).size() > 0)
					{
						structureId = JournalStructureConstants.STRUCTURE_POLL;
						templateId = "FULL-CONTENT-POLL";
					}
					
					article = super.addArticle(userId, globalGroupId, articleId, false,
											   version, title, description, content, type,
											   structureId, templateId, displayDateMonth,
											   displayDateDay, displayDateYear, displayDateHour,
											   displayDateMinute, expirationDateMonth,
											   expirationDateDay, expirationDateYear,
											   expirationDateHour, expirationDateMinute, neverExpire,
											   reviewDateMonth, reviewDateDay, reviewDateYear,
											   reviewDateHour, reviewDateMinute, neverReview,
											   indexable, smallImage, smallImageURL, smallFile,
											   images, articleURL, serviceContext);
					
					// Crea / Actualiza / Elimina las encuestas del artículo
					if (iterSurveysEnabled)
						IterSurveyModel.saveArticleSurveys(articleId, surveys);
					
					//Add required codifications
					//extendMultimediaCodifications(article);
					
					//Add to live
					itemXmlIO.createLiveEntry(article);
				}
	
				// En el back se fuerza a que el displayDate sea el creationDate 
				// (0005969: Cambios en los criterios de creación, actualización y publicación de createDate, modifiedDate y displayDate en JournalArticle)
				if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
				{
					article.setDisplayDate( article.getCreateDate() );
					JournalArticleUtil.update(article, false);
				}
			
			}
			catch(Exception e){
				String result = ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, e);
				throw new SystemException(result);
			}
					
		return article;
	}

	/*
	 * UPDATE FUNCTIONS
	 */
	@Override
	public JournalArticle updateArticle(long userId, long groupId, 
										String articleId, double version, 
										String content) throws PortalException, SystemException 
	{

		JournalArticle article = null;

		ServiceContext serviceContext = new ServiceContext();
		serviceContext.setAddCommunityPermissions(true);
		serviceContext.setAddGuestPermissions(true);
		serviceContext.setScopeGroupId(groupId);
			
		/*
		 * Para evitar el incremento de versión, primero se cambia el status a draft, se actualiza y después se pone a approved
		 */
		
		//DRAFT
		article = super.updateStatus(userId, groupId, articleId,	version,  WorkflowConstants.STATUS_DRAFT, "", serviceContext);
	
		/*
		 * UPDATE ARTICLE POLL
		 */
		if (article.getStructureId().equals("STANDARD-POLL")) {
			
			article = super.updateArticle(userId, groupId, articleId, version, content);
			//TODO: crear un metodo updatePoll que ACTUALICE Y NO BORRE-CREE LOS DATOS DE LA ENCUESTA
			//Controlar excepciones de forma diferenciada al updateArticle del resto de estructuras si no se cumplen los requisitos de encuesta
			

		}
		//Otro tipo de contenido
		else{
			article = super.updateArticle(userId, groupId, articleId,
					version, content);
		}
	
		//APPROVED
		article = super.updateStatus(userId, groupId, articleId, version,  WorkflowConstants.STATUS_APPROVED, "", serviceContext);

		//Add to live
		itemXmlIO.createLiveEntry(article);
		
		// En el back se fuerza a que el displayDate sea el creationDate 
		// (0005969: Cambios en los criterios de creación, actualización y publicación de createDate, modifiedDate y displayDate en JournalArticle)
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
		{
			article.setDisplayDate( article.getCreateDate() );
			JournalArticleUtil.update(article, false);
		}

		return article;
	}

	//Utilizada por MILENIUM
	@Override
	public JournalArticle updateArticle(long userId, long groupId,
			String articleId, double version, String title, String description,
			String content, String type, String structureId, String templateId,
			int displayDateMonth, int displayDateDay, int displayDateYear,
			int displayDateHour, int displayDateMinute,
			int expirationDateMonth, int expirationDateDay,
			int expirationDateYear, int expirationDateHour,
			int expirationDateMinute, boolean neverExpire, int reviewDateMonth,
			int reviewDateDay, int reviewDateYear, int reviewDateHour,
			int reviewDateMinute, boolean neverReview, boolean indexable,
			boolean smallImage, String smallImageURL, File smallFile,
			Map<String, byte[]> images, String articleURL,
			ServiceContext serviceContext) throws PortalException,
			SystemException {

			JournalArticle article = null;

			try
			{
				// Si está habilitado el nuevo sistema de encuestas y llega un artículo con estructura STANDARD-POLL, lanza un error por incompatibilidad
				IterSurveyUtil.checkCompatibility(structureId, groupId);
				
				/*
				 * Para evitar el incremento de versión, primero se cambia el status a draft, se actualiza y después se pone a approved
				 */
				
				//DRAFT
				article = super.updateStatus(userId, groupId, articleId, version,  WorkflowConstants.STATUS_DRAFT, title.toLowerCase().replace(" ", "-"), serviceContext);
				
				
				/*
				 * UPDATE ARTICLE POLL
				 */
				boolean iterSurveysEnabled = IterSurveyUtil.isEnabledInDelegation(article);
				if (!iterSurveysEnabled && structureId.equals("STANDARD-POLL")) {
	
					long questionId = ArticlePollLocalServiceUtil.updatePoll(articleId, title, userId, 
																			 groupId, content, expirationDateMonth, 
																			 expirationDateDay, expirationDateYear, 
																			 expirationDateHour, expirationDateMinute, 
																			 neverExpire);
					
					ErrorRaiser.throwIfFalse(questionId != -1, IterErrorKeys.XYZ_E_QUESTIONPOLL_NOT_WELLFORMED_ZYX,"At least two choices needed");
					
					article = super.updateArticle(userId, groupId, articleId,
							version, title, description, content, type,
							structureId, templateId, displayDateMonth,
							displayDateDay, displayDateYear, displayDateHour,
							displayDateMinute, expirationDateMonth,
							expirationDateDay, expirationDateYear,
							expirationDateHour, expirationDateMinute, neverExpire,
							reviewDateMonth, reviewDateDay, reviewDateYear,
							reviewDateHour, reviewDateMinute, neverReview,
							indexable, smallImage, smallImageURL, smallFile,
							images, articleURL, serviceContext);
				}
				//Otro tipo de contenido
				else
				{
					// Si está habilitado el nuevo sistema de encuestas y contiene encuestas válidas,
					// se establece el structureId a STANDARD-POLL y el templateId a FULL-CONTENT-POLL
					List<IterSurveyModel> surveys = null;
					if (iterSurveysEnabled && (surveys = IterSurveyModel.processArticleSurveys(articleId, content)).size() > 0)
					{
						structureId = JournalStructureConstants.STRUCTURE_POLL;
						templateId = "FULL-CONTENT-POLL";
					}
					
					article = super.updateArticle(userId, groupId, articleId,
							version, title, description, content, type,
							structureId, templateId, displayDateMonth,
							displayDateDay, displayDateYear, displayDateHour,
							displayDateMinute, expirationDateMonth,
							expirationDateDay, expirationDateYear,
							expirationDateHour, expirationDateMinute, neverExpire,
							reviewDateMonth, reviewDateDay, reviewDateYear,
							reviewDateHour, reviewDateMinute, neverReview,
							indexable, smallImage, smallImageURL, smallFile,
							images, articleURL, serviceContext);
					
					// Crea / Actualiza / Elimina las encuestas del artículo
					if (iterSurveysEnabled)
						IterSurveyModel.saveArticleSurveys(articleId, surveys);
	
				}
	
				//APPROVED
				article = super.updateStatus(userId, groupId, articleId,	
											 version,  WorkflowConstants.STATUS_APPROVED, 
											 title.toLowerCase().replace(" ", "-"), 
											 serviceContext);
	
				//Add to live
				itemXmlIO.createLiveEntry(article);
				
				// En el back se fuerza a que el displayDate sea el creationDate 
				// (0005969: Cambios en los criterios de creación, actualización y publicación de createDate, modifiedDate y displayDate en JournalArticle)
				if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
				{
					article.setDisplayDate( article.getCreateDate() );
					JournalArticleUtil.update(article, false);
				}
			}
			catch(Exception e)
			{
				String result = ServiceErrorUtil.getServiceErrorAsXml(IterErrorKeys.XYZ_E_UNEXPECTED_ZYX, e);
				throw new SystemException(result);
			}
		return article;
	}

	/*
	 * DELETE FUNCTIONS
	 */
	@Override
	public boolean deleteArticle(long groupId, String articleId, ServiceContext serviceContext,
			  boolean deleteFileEntry) throws PortalException, SystemException 
	{
		List<JournalArticle> articles = JournalArticleUtil.findByG_A(
		groupId, articleId, QueryUtil.ALL_POS, QueryUtil.ALL_POS,
		new ArticleVersionComparator(true));
		
		for (JournalArticle article : articles) 
		{
			deleteArticle(article, null, serviceContext, deleteFileEntry);
		}
		
		return articles.size() > 0;
	}

	@Override
	public void deleteArticle(JournalArticle article, String articleURL, ServiceContext serviceContext, boolean deleteFileEntry) throws PortalException, SystemException 
	{
		//boolean deleteJustNow = JournalArticleXmlIO.isArchivableArticle(article.getArticleId());
		long groupId = article.getGroupId();
		deleteArticleReferences(groupId, article, serviceContext, deleteFileEntry);
		
		super.deleteArticle(article, articleURL, serviceContext, deleteFileEntry);
		
		//Add to live
		itemXmlIO.deleteLiveEntry(article, true);
	}

	@Override
	public void deleteArticle(
			long groupId, String articleId, double version, String articleURL,
			ServiceContext serviceContext)
		throws PortalException, SystemException {

		JournalArticle article = JournalArticleUtil.findByG_A_V(
			groupId, articleId, version);

		deleteArticle(article, articleURL, serviceContext);
	}	
	
	/**
	 * Es el utilizado desde MLN -> news-portlet.JournalArticleLocalServiceImpl.java
	 */
	/*
	@Override
	public void deleteArticle(long groupId, String articleId, ServiceContext serviceContext,
							  boolean deleteFileEntry) throws PortalException, SystemException 
	{
		List<JournalArticle> articles = JournalArticleUtil.findByG_A(
																groupId, articleId, QueryUtil.ALL_POS, QueryUtil.ALL_POS,
																new ArticleVersionComparator(true));

		for (JournalArticle article : articles) 
		{
			deleteArticle(article, null, serviceContext, deleteFileEntry);
		}
	}
	*/
	@Override
	public void deleteJournalArticle(JournalArticle journalArticle) throws SystemException 
	{
		try 
		{
			long groupId = journalArticle.getGroupId();
			deleteArticleReferences(groupId, journalArticle, null, false);
			
			super.deleteJournalArticle(journalArticle);
			
			//Add to live
			itemXmlIO.deleteLiveEntry(journalArticle, true);
		} 
		catch (Exception e) 
		{
			_log.error("DeleteArticle Error", e);
		}
	}

	@Override
	public void deleteJournalArticle(long id) throws PortalException, SystemException 
	{
		try 
		{
			JournalArticle article = JournalArticleLocalServiceUtil.getArticle(id);
			deleteJournalArticle(article);
		} 
		catch (Exception e) 
		{
			_log.error("DeleteArticle Error", e);
		}
	}

	@Override
	public void deleteArticles(long groupId) throws PortalException, SystemException 
	{
		for (JournalArticle article : JournalArticleUtil.findByGroupId(groupId)) 
		{
			deleteArticle(article, null, null);
		}
	}
	
	/*
	 * AUXILIAR FUNCTIONS
	 */

	/**
	 * Delete article references
	 * 
	 * @param groupId
	 * @param article
	 * @throws SystemException
	 * @throws PortalException
	 */
	private void deleteArticleReferences(long groupId, 
										 JournalArticle article, 
										 ServiceContext serviceContext,
										 boolean deleteFileEntry) throws SystemException, PortalException 
	{
		_log.info("Deleting JournalArticle with articleId: " +  article.getArticleId() + ". This article has been completely deleted, no other versions remaining. So its associated content it is going to be deleted.");
		
		//PageContent
		try
		{
			PageContentLocalServiceUtil.deletePageContentByContentId(article.getArticleId(), true);
		}
		catch(Exception e)
		{
			_log.error("Error deleting PageContent with contentId: " + article.getArticleId());
			_log.error(e.toString());
			_log.trace(e);
		}
		
		//Counters and DateCounters
		try
		{
			if(serviceContext != null)
			{
				PortalLocalServiceUtil.executeUpdateQuery(String.format(DELETE_NEWS_COUNTERS_BY_CONTENTID, article.getArticleId()));
			}
		}
		catch(Exception e)
		{
			_log.error("Error deleting Counter and DateCounter with contentId: " + article.getArticleId());
			_log.error(e.toString());
			_log.trace(e);
		}
		
		//Poll
		try
		{
			if ( article.getStructureId().equals("STANDARD-POLL")) 
			{
				long questionId = ArticlePollLocalServiceUtil.getPoll(groupId, article.getArticleId());
				ArticlePollLocalServiceUtil.deleteArticlePoll(groupId, article.getArticleId());
				PollsQuestionLocalServiceUtil.deleteQuestion(questionId);
			}
		}
		catch(Exception e)
		{
			_log.error("Error deleting Poll with articleId: " + article.getArticleId());
			_log.error(e.toString());
			_log.trace(e);
		}
		
		if (deleteFileEntry)
		{
			// Borrar DLFileEntries. Si alguna falla es importante que se sepa el fallo por lo que NO se captura el error
			List<DLFileEntry> dlfeList = XMLIOUtil.getWebContentFileEntries(article);
			for (DLFileEntry dlfe : dlfeList)
			{
				try
				{
					serviceContext.setDelegationId(GroupLocalServiceUtil.getGroup(groupId).getDelegationId());
					DLFileEntryLocalServiceUtil.deleteFileEntry(dlfe, serviceContext);
				}
				catch(Exception e1)
				{
					_log.error("Article " +  article.getArticleId() + ". Error removing DLFileEntry " + dlfe.getTitle(), e1);
				}
			}
		}
	}
	
	/***********************
	 * Video Code/Decode Functions
	 ****************************/
	
	public boolean extendMultimediaCodifications(JournalArticle article){	
		//TODO: Hay que añadir un campo que sirva para activar o desactivar la conversión
		
		//Solo se ejecuta para "nuestras" estructuras		
		if (ArrayUtils.contains(IterKeys.MILENIUM_STRUCTURES, article.getStructureId())){
		
			//1. Obtener el contenido del artículo
			Document document = null;
			try {
				document = SAXReaderUtil.read(article.getContent());
			} catch (DocumentException e) {
				_log.error("Unable to read the WebContent content", e);	
				return false;
			}
			
			//2. Obtener todos los multimedia del contenido y recorrerlos	
			List<Element> documentNodes = document.getRootElement().elements("dynamic-element");
			for (Element documentNode : documentNodes) {
				
				if(documentNode.attribute("name").getValue().equals(IterKeys.STANDARD_ARTICLE_MULTIMEDIA)){
											
					List<Element> multimediaNodes = documentNode.elements("dynamic-element");
					int i = 0;				 
					while (i < multimediaNodes.size()) {
										
						//3. Obtener fileEntry del primero de todos los source pertenecientes al multimedia actual
						//	(Este será el que utilizaremos para codificar los demás)	
						if(multimediaNodes.get(i).attribute("name").getValue().equals(IterKeys.STANDARD_ARTICLE_SOURCE)){
							Element sourceMasterNode = multimediaNodes.get(i);
							
							String [] dlURL = sourceMasterNode.element("dynamic-content").getText().split(StringPool.SLASH);
							long folderId = 0;
							//Si el formato es 0/1/2/.../N-2/N-1, 0 es el grupo, N-2 es el folderId y N-1 es el title
							//En cualquier otro caso, el folderId es 0
							if (dlURL.length > 2){
								folderId = Long.valueOf(dlURL[dlURL.length-2]);
							}
							String title = dlURL[dlURL.length-1];
							try{
								DLFileEntryLocalServiceUtil.getFileEntryByTitle(article.getGroupId(), folderId, title);
							}
							catch(Exception err1){
								//Comprobar que el title no necesita un URLdecode (por los parentesis de Milenium)
								try{
									DLFileEntryLocalServiceUtil.getFileEntryByTitle(article.getGroupId(), folderId, URLDecoder.decode(title, "UTF-8"));
								}
								catch(Exception err2){
									//Si el último miembro de la URL no es el title, es el UUID
									try{
										DLFileEntryLocalServiceUtil.getFileEntryByUuidAndGroupId(title, article.getGroupId());
									}
									catch(Exception err3){
										_log.error("File " + title + " not found");	
										return false;
									}							
								}					
							}	
							
							/*
							//Comprobar que el archivo muestra se encuentra en una extensión válida.
							if(fileEntry.getExtension().equals("mp4")){
								//5. obtener los tipos de codificación que que queremos crear
								for (String[] codecType : IterKeys.CODEC_TYPES){
									
									//6. Crear la nueva codificación				
									try {
										String newFileUrl = encodeFileEntry(fileEntry, codecType);
										
										if(!newFileUrl.equals("")){									
											
											//7. Crear nuevo source
											//7.1. Crear source
											Element newSourceElement = SAXReaderUtil.createElement("dynamic-element");									
											newSourceElement.addAttribute("instance-id", PwdGenerator.getPassword());
											newSourceElement.addAttribute("name", IterKeys.STANDARD_ARTICLE_SOURCE);
											newSourceElement.addAttribute("type", "document_library");
											newSourceElement.addAttribute("index-type", "");
											newSourceElement.addAttribute("repeatable", "true");
											
											//7.2. Crear contenido del source
											Element newSourceElementContent = SAXReaderUtil.createElement("dynamic-content");									
											newSourceElementContent.addCDATA(newFileUrl);
											
											//7.3. Añadir contenido del source al source
											newSourceElement.add(newSourceElementContent);
																				
											//7.4 Crear extensión
											Element newSourceElementExtension = SAXReaderUtil.createElement("dynamic-element");
											newSourceElementExtension.addAttribute("instance-id", PwdGenerator.getPassword());
											newSourceElementExtension.addAttribute("name", IterKeys.STANDARD_ARTICLE_EXTENSION);
											newSourceElementExtension.addAttribute("type", "text");
											newSourceElementExtension.addAttribute("index-type", "");
											newSourceElementExtension.addAttribute("repeatable", "false");
											
											//7.5. Crear contenido de la extension
											Element newSourceElementExtensionContent = SAXReaderUtil.createElement("dynamic-content");									
											newSourceElementExtensionContent.addCDATA(codecType[0]);
											
											//7.6. Añadir contenido de la extensión a la extension
											newSourceElementExtension.add(newSourceElementExtensionContent);
											
											//7.7. Añadir la extensión al source
											newSourceElement.add(newSourceElementExtension);
											
											//7.8. Añadir nuevo source al multimedia actual
											documentNode.add(newSourceElement);			
										}		
									} catch (Exception e) {
										_log.error("Can't Encode/Decode video");
									}							
								}
							}else{
								_log.error("Incorrect source video extension");
							}
							
							*/
							
							i = multimediaNodes.size();
						}else{
							i++;
						}
					}				
				}
			}
		
			try {
				article.setContent(document.formattedString());
				JournalArticleLocalServiceUtil.updateJournalArticle(article, false);
			} catch (Exception e) {
				_log.error("Unable to set content", e);	
				return false;
			}
		}
		
		return true;
	}

}
