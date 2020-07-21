/*******************************************************************************
  * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.news.service.item;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.VisitsStatisticsLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.designer.model.PageTemplate;
import com.protecmedia.iter.designer.service.PageTemplateLocalServiceUtil;
import com.protecmedia.iter.news.DuplicatePageContentIdException;
import com.protecmedia.iter.news.PageContentExistsException;
import com.protecmedia.iter.news.model.PageContent;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.journal.JournalArticleXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;

/*
<item operation="delete" globalid="10457" classname="com.protecmedia.iter.news.model.PageContent" groupid="Guest">
			<param name="orden">&lt;![CDATA[1]]&gt;</param>
			<param name="pagetemplate">&lt;![CDATA[MODELO_DETALLE]]&gt;</param>
			<param name="qualification">&lt;![CDATA[qual_10387]]&gt;</param>
			<param name="datefrom">&lt;![CDATA[2011/09/01 14:39:00]]&gt;</param>
			<param name="article">&lt;![CDATA[10447]]&gt;</param>
			<param name="dateto">&lt;![CDATA[2011/10/01 14:39:00]]&gt;</param>
			<param name="type">&lt;![CDATA[STANDARD-ARTICLE]]&gt;</param>
			<param name="contentgroup">&lt;![CDATA[10132]]&gt;</param>
			<param name="pagecontentid">&lt;![CDATA[10457]]&gt;</param>
			<param name="url">&lt;![CDATA[/pagina1]]&gt;</param>
			<param name="online">&lt;![CDATA[true]]&gt;</param>
		</item>
 */

public class PageContentXmlIO extends ItemXmlIO {
	
	private static Log _log = LogFactoryUtil.getLog(PageContentXmlIO.class);
	private String _className = IterKeys.CLASSNAME_PAGECONTENT;

	public PageContentXmlIO(){
		super();
	}
	
	public PageContentXmlIO (XMLIOContext xmlIOContext) {
		super(xmlIOContext);
	}
	
	@Override
	public String getClassName(){
		return _className;
	}
	
	/*
	 * Live Functions
	 */
	@Override
	public void populateLive(long groupId, long companyId) throws SystemException 
	{
		List<PageContent> pageContentList = PageContentLocalServiceUtil.getPageContents(groupId);
		for (PageContent pageContent : pageContentList)
		{	
			try 
			{
				createLiveEntry(pageContent);
			} 
			catch (Exception e) 
			{
				_log.error("Can't add Live, PageContent: " + pageContent.getPageContentId());
			}
		}
		
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException
	{
		// ITER-893 Error al editar nota del histórico (migrada) y mover modelo web
		// http://jira.protecmedia.com:8080/browse/ITER-893?focusedCommentId=36234&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-36234
		// Si solo se quiere crear el registro en el entorno BACK se comprueba dicho entorno y se evita su ejecución en el LIVE,
		// que presenta problemas (Editar contenido de la web) cuando el contenido es migrado y no tiene XMLIO_Live
		if (PropsValues.IS_PREVIEW_ENVIRONMENT)
		{
			PageContent pageContent = (PageContent)model;
			
			Live liveArticle = LiveLocalServiceUtil.getLiveByLocalId(pageContent.getContentGroupId(), IterKeys.CLASSNAME_JOURNALARTICLE, pageContent.getContentId());
			
			//Publicabilidad
			String status = IterKeys.PENDING;
			if (!pageContent.getOnline())
			{
				status = IterKeys.DRAFT;
			}
			
			LiveLocalServiceUtil.add(_className, pageContent.getGroupId(), liveArticle.getId(), liveArticle.getId(), 
					IterLocalServiceUtil.getSystemName() + "_" + pageContent.getPageContentId(), pageContent.getPageContentId(),
					IterKeys.CREATE, status, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
		}
	}

	@Override
	public void deleteLiveEntry(BaseModel<?> model, boolean deleteJustNow) throws PortalException, SystemException
	{
		PageContent pageContent = (PageContent)model;
		String id 				= pageContent.getPageContentId();
		
		Live live = LiveLocalServiceUtil.getLiveByLocalId(pageContent.getGroupId(), _className, id);
		
		// Como es un borrado si no existe NO se creará
		if (live != null)
		{
			_log.trace( getTraceDeleteLiveEntry(live.getGlobalId(), deleteJustNow) );
			
			if (deleteJustNow)
			{
				LiveLocalServiceUtil.deleteLive(live.getGroupId(), live.getClassNameValue(), live.getGlobalId());
			}
			else
			{
				String environment 	= IterLocalServiceUtil.getEnvironment();
				String status 		= environment.equals(IterKeys.ENVIRONMENT_PREVIEW) ? IterKeys.PENDING : IterKeys.DONE;

				LiveLocalServiceUtil.add(live.getClassNameValue(), live.getGroupId(),
						live.getGlobalId(), id, IterKeys.DELETE, status, new Date(), environment);			
			}
		}
	}
	
	@Override
	public void updateStatusLiveEntry(BaseModel<?> model, String status) throws PortalException, SystemException
	{
		PageContent pageContent = (PageContent)model;
		String globalId 		= IterLocalServiceUtil.getSystemName() + "_" + pageContent.getPageContentId();
		
		LiveLocalServiceUtil.updateStatus(pageContent.getGroupId(), _className, globalId, status);
	}

	/*
	 * Publish Functions
	 */
	// See JournalArticleXmlio

	
	/*
	 * Export Functions
	 */	
	@Override
	protected String createItemXML(XMLIOExport xmlioExport, Element root, String operation, Group group, Live live) {
		
		String error = "";
		
		DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		
		Map<String, String> params = new HashMap<String, String>();				
		Map<String, String> attributes = new HashMap<String, String>();
		
		setCommonAttributes(attributes, group.getName(), live, operation);
		
		//Put necessary parameters for each kind of operation.
		if (operation.equals(IterKeys.CREATE)){
			
			PageContent pageContent = null;
			
			try {
				
				pageContent = PageContentLocalServiceUtil.getPageContent(group.getGroupId(), live.getLocalId());
				PageTemplate pageTemplate = PageTemplateLocalServiceUtil.getPageTemplateById(pageContent.getArticleModelId());
				String pageTemplateId = "";
				if (pageTemplate != null) {
					pageTemplateId = pageTemplate.getPageTemplateId();
				}
				
				Layout layout;
				layout = LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(pageContent.getLayoutId(), pageContent.getGroupId());
				Live liveLayout = LiveLocalServiceUtil.getLiveByLocalId(layout.getGroupId(), IterKeys.CLASSNAME_LAYOUT, String.valueOf(layout.getPlid()));
				
				// Solo se pueden asignar contenidos del grupo Global
				Group auxgrp 		= GroupLocalServiceUtil.getGroup(pageContent.getGroupId());
				Company c 			= CompanyLocalServiceUtil.getCompany(auxgrp.getCompanyId());
				Group contentGroup 	= GroupLocalServiceUtil.getGroup(pageContent.getContentGroupId());
				Live liveContent 	= LiveLocalServiceUtil.getLiveByLocalId(c.getGroup().getGroupId(), IterKeys.CLASSNAME_JOURNALARTICLE, pageContent.getContentId());
				
				params.put("pagecontentid", 	pageContent.getPageContentId());		
				params.put("article", 			pageContent.getContentId());
				params.put("articleGlobalId", 	liveContent.getGlobalId());
				params.put("qualification", 	pageContent.getQualificationId());
				params.put("datefrom", 			df.format(pageContent.getVigenciadesde()));
				params.put("dateto", 			df.format(pageContent.getVigenciahasta()));
				params.put("defaultSection", 	String.valueOf(pageContent.getDefaultSection()));
				params.put("online", 			String.valueOf(pageContent.getOnline()));
				params.put("type", 				pageContent.getTypeContent());
				params.put("pagetemplate", 		pageTemplateId);
				params.put("layout", 			liveLayout.getGlobalId());
				params.put("orden", 			String.valueOf(pageContent.getOrden()));
				params.put("contentgroup", 		contentGroup.getName());
			
			
			} 
			catch (Exception e) 
			{
				//LiveLocalServiceUtil.setError(live.getId(), IterKeys.CORRUPT, "Can't export item");	
				_log.error(e);
				error = "Cannot export item: "+e.toString();
			}
		}
			
		addNode(root, "item", attributes, params);		
			
		_log.debug("XmlItem OK");
	
		return error;
	}
	
	/*
	 * Import Functions
	 */	

	@Override
	protected void delete(Element item) 
	{
		//TODO: hemeroteca: en entorno live, si está offline OR (vigente AND online) se borra. else se deja pero forzando evaluateVisibility del content.
		String sGroupId = getAttribute(item, "groupid");			
		String globalId = getAttribute(item, "globalid");	
		
		try
		{
			long groupId = getGroupId(sGroupId);
			
			try
			{
				//Get live to get the element localId
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
				
				try
				{					
					PageContent pageContent = PageContentLocalServiceUtil.getPageContent(groupId, live.getLocalId());
					
					try
					{
						// El ItemXMLIO correspondiente se borra al borrar el PageContent
						// La lógica del borrado la tiene el BACK, si al LIVE llega un borrado se borra
						PageContentLocalServiceUtil.deletePageContent(pageContent.getId());
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Done", IterKeys.DONE, sGroupId);
					} 
					catch (Exception e1) 
					{
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Error: "+ e1.toString(), IterKeys.ERROR, sGroupId);				
					}	
				} 
				catch (Exception e1) 
				{
					if (live != null)
					{
						// clean entry in live table
						LiveLocalServiceUtil.add(_className, groupId, globalId, live.getLocalId(), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
					}
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Element not found", IterKeys.DONE, sGroupId);				
				}
			} 
			catch (Exception e1) 
			{
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Element not found", IterKeys.ERROR, sGroupId);				
			}
		} 
		catch (Exception e) 
		{
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}
	}
	
	//TODO: Tener en cuenta el parametro "defaultSection"
	@Override
	protected void modify(Element item, Document doc) 
	{
		_log.trace(" ");
		_log.trace("PageContent: Begin");
		DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); 
		
		String sGroupId 		= getAttribute(item, "groupid");			
		String globalId 		= getAttribute(item, "globalid");	
	
		String id 				= getParamTextByName(item, "pagecontentid");
		String content 			= getParamTextByName(item, "article");
		String qualification 	= getParamTextByName(item, "qualification");
		String type 			= getParamTextByName(item, "type");
		String sPageTemplate 	= getParamTextByName(item, "pagetemplate");
		String layout 			= getParamTextByName(item, "layout");	
		String sContentGroupId 	= getParamTextByName(item, "contentgroup");		
		int orden 				= GetterUtil.getInteger(getParamTextByName(item, "orden"), 1);	
		boolean online 			= GetterUtil.getBoolean(getParamTextByName(item, "online"), false);		
		boolean defaultSection 	= GetterUtil.getBoolean(getParamTextByName(item, "defaultSection"), false);		
		Date dfrom 				= GetterUtil.getDate(getParamTextByName(item, "datefrom"), df, new Date());
		Date dto 				= GetterUtil.getDate(getParamTextByName(item, "dateto"), df, new Date());
		_log.trace("PageContent: After params");
		try
		{
			long groupId = getGroupId(sGroupId);	
		
			long groupContentId = getGroupId(sContentGroupId);					
		
			if (groupContentId != -1) 
			{
				try 
				{
					//Get live to get the element localId
					Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
					_log.trace("PageContent: After LiveByGlobalId");
					try
					{
						Live liveLayout = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_LAYOUT, layout);
						Layout laux = LayoutLocalServiceUtil.getLayout(GetterUtil.getLong(liveLayout.getLocalId()));
						_log.trace("PageContent: getLayout");
						
						Company c = CompanyLocalServiceUtil.getCompany(xmlIOContext.getCompanyId());
						
						// ITER-403	Podría existir el caso en que exista el JournalArticle pero NO el XMLIO_Live del JournalArticle
						JournalArticleLocalServiceUtil.getArticle(c.getGroup().getGroupId(), content);
						Live liveContent = LiveLocalServiceUtil.getLiveByLocalId(c.getGroup().getGroupId(), IterKeys.CLASSNAME_JOURNALARTICLE, content);
						
						// El XMLIO_Live del JournalArticle se regenerara si no existe
						if (liveContent == null)
						{
							String articleGlobalId = getParamTextByName(item, "articleGlobalId");
							
							LiveLocalServiceUtil.add(IterKeys.CLASSNAME_JOURNALARTICLE, c.getGroup().getGroupId(), articleGlobalId,
									content, IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
						}
						
						_log.trace("PageContent: getArticle");
						
						try
						{
							long pageTemplateId = getPageTemplateId(groupId, sPageTemplate);
							_log.trace("PageContent: getPageTemplateId");
							
							PageContent pageContent = null;
							try
							{
								pageContent = PageContentLocalServiceUtil.getPageContent(groupId, live.getLocalId());
								_log.trace("PageContent: getPageContent");
								
								if (pageContent != null)
								{					
									boolean needStatisticsAnnotation = pageContent.getQualificationId() != qualification &&
											GetterUtil.getBoolean(PortalUtil.getPortalProperties().getProperty("iter.statistics.enabled"), true);
									
									int intOnline = (online) ? 1 : 0;
									
									//UPDATE
									pageContent = PageContentLocalServiceUtil.updatePageContent( intOnline, pageTemplateId,
											qualification, dto,dfrom, pageContent.getId(), defaultSection, orden);
									_log.trace("PageContent: updatePageContent");
	
									//update entry in live table
									LiveLocalServiceUtil.add(_className, groupId, globalId,
											pageContent.getPageContentId(), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
									_log.trace("PageContent: LiveLocalServiceUtil.add");
									
									xmlIOContext.itemLog.addMessage(item,globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, sGroupId);
									_log.trace("PageContent: itemLog.addMessage");
									
									// Inserta unaanotación en las estadísticas del artículo
									if(needStatisticsAnnotation)
									{
										try {
											String annotationLiteral = GetterUtil.getString(GroupConfigTools.getGroupConfigXMLField(groupId, "visitstatistics", "/visits/annotations/qualification/text()"), "Se ha modificado la calificación en la sección");
											String section = LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(pageContent.getLayoutId(), groupId).getFriendlyURL();
											VisitsStatisticsLocalServiceUtil.addArticleStatisticsAnnotation(groupId, content, annotationLiteral + StringPool.SPACE + section);
										}
										catch (Throwable th) {
											_log.error("Unable to add statistics system annotation for article " + content);
										}
									}
								}
							} 
							catch (Exception e1)
							{
								_log.trace("PageContent: catch!!!");
								if (live == null || !live.getOperation().equals(IterKeys.DELETE))
								{
									//CREATE
									try
									{
										pageContent = PageContentLocalServiceUtil.addPageContent(id, content, laux.getLayoutId(), groupId, groupContentId, 
														qualification, type, pageTemplateId, orden, dfrom, dto, online, defaultSection);
									}
									catch (DuplicatePageContentIdException dpc)
									{
										// ITER-422	Error "Duplicate pageContentId"
										// http://jira.protecmedia.com:8080/browse/ITER-422?focusedCommentId=17024&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-17024
										pageContent = PageContentLocalServiceUtil.getPageContent(groupId, id);
									}
									catch (PageContentExistsException pce)
									{
										// ITER-422	Error "Duplicate pageContentId"
										// http://jira.protecmedia.com:8080/browse/ITER-422?focusedCommentId=17024&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-17024
										pageContent = PageContentLocalServiceUtil.getPageContent(content, laux.getLayoutId(), groupId, type);
										id = pageContent.getPageContentId();
									}
								    
									_log.trace("PageContent: addPageContent");
									
									//update entry in live table
									LiveLocalServiceUtil.add(_className, groupId, globalId,
											id, IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
									_log.trace("PageContent: LiveLocalServiceUtil.add");
									try
									{
										//Update globalId.
										LiveLocalServiceUtil.updateGlobalId(groupId, _className, id, globalId);
										_log.trace("PageContent: LiveLocalServiceUtil.updateGlobalId");
									}
									catch(Exception e3)
									{
										xmlIOContext.itemLog.addMessage(item,globalId, _className, IterKeys.CREATE, "Duplicated globalId", IterKeys.ERROR, sGroupId);
									}
									
									xmlIOContext.itemLog.addMessage(item,globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, sGroupId);
									_log.trace("PageContent: itemLog.addMessage");
								}
							}
							// Inserta el pageContentId en la tabla pending_action
							if (pageContent != null) insertIntoPendingAction(pageContent.getId(), doc);
						}
						catch(DuplicatePageContentIdException dpc)
						{
							xmlIOContext.itemLog.addMessage(item,globalId, _className, IterKeys.CREATE, "Duplicate pageContentId", IterKeys.ERROR, sGroupId);
						}
						catch(PageContentExistsException pce)
						{
							xmlIOContext.itemLog.addMessage(item,globalId, _className, IterKeys.CREATE, "Article is already assigned to page ", IterKeys.ERROR, sGroupId);
						}
						catch (Exception e1)
						{
							xmlIOContext.itemLog.addMessage(item,globalId, _className, IterKeys.CREATE, "PageTemplate not found", IterKeys.ERROR, sGroupId);
							_log.error(e1);
						}
					} 
					catch (Exception e1) 
					{
						xmlIOContext.itemLog.addMessage(item,globalId, _className, IterKeys.CREATE, "Layout not found", IterKeys.ERROR, sGroupId);				
					}
				} 
				catch (Exception e1) 
				{
					xmlIOContext.itemLog.addMessage(item,globalId, _className, IterKeys.CREATE, "Element not found", IterKeys.ERROR, sGroupId);				
				}
			} 
			else
			{
				xmlIOContext.itemLog.addMessage(item,globalId, _className, IterKeys.CREATE, "GroupContentId not found", IterKeys.ERROR, sGroupId);
			}
		}
		catch (Exception e)
		{
			xmlIOContext.itemLog.addMessage(item,globalId, _className, IterKeys.CREATE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}

		_log.trace("PageContent: End");
		_log.trace(" ");
	}
	
	
	/**
	 * @param localId: News_PageContent.pageContentId
	 */
	@Override
	public String getMileniumId(long groupId, String localId){
		try{
			PageContent pc = PageContentLocalServiceUtil.getPageContent(groupId, localId);
			return String.valueOf(pc.getId());
		}
		catch(Exception err){
			return localId;
		}
	}
	
	/**
	 * @param localId: JournalArticle.articleId
	 */
	@Override 
	public boolean isDraft(long groupId, String localId)
	{
		// No se tiene en cuenta el Grupo por las siguientes razones:
		// - La función que llama tiene acceso al ContentGroupID, es decir, el grupo global, y la búsqueda se espera por grupo local que no siempre está accesible
		// - Si se decide que es Draft teniendo en cuenta el grupo local puede que el artículo esté en la página de otro grupo. Si se eliminase se borraría de todo, 
		//   así que es más correcto comprobarlo en TODAS las páginas sea el grupo que sea.
		
		// Si existe un PageContent Publicable y NO vigente NO se puede borrar el artículo
		return JournalArticleXmlIO.isArchivableArticle(localId);
	}
	
	/**
	 * 
	 * @param groupId
	 * @param pageTemplateId
	 * @return
	 * @throws SystemException 
	 */
	protected long getPageTemplateId(long groupId, String pageTemplateId) throws SystemException {
		long id = -1;	
		
		if (!pageTemplateId.equals("") && !pageTemplateId.equals("-1")){
			PageTemplate pageTemplate = PageTemplateLocalServiceUtil.getPageTemplateByPageTemplateId(groupId, pageTemplateId);			
			id = pageTemplate.getId();
		}
		
		return id;
	}

	private static final String SQL_INSERT_PENDING_ACTION = "INSERT INTO pending_actions (publicationId, kind, pageContentId) VALUES ('%s', 'publication schedule', %d)";
	private void insertIntoPendingAction(long pageContentId, Document doc)
	{
		try
		{
			// Recupera el processId
			String processId = XMLHelper.getStringValueOf(doc.getRootElement(), "@processid");
			
			// Anota el pagecontent para que se comprueben las fechas de publicación
			PortalLocalServiceUtil.executeUpdateQuery(String.format(SQL_INSERT_PENDING_ACTION, processId, pageContentId));
		}
		catch (Throwable th)
		{
			_log.error(th);
		}
	}
}
