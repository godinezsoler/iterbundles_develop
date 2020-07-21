/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.news.dataHandler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.portlet.PortletPreferences;

import sun.nio.cs.ext.PCK;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.lar.BasePortletDataHandler;
import com.liferay.portal.kernel.lar.PortletDataContext;
import com.liferay.portal.kernel.lar.PortletDataException;
import com.liferay.portal.kernel.lar.PortletDataHandlerBoolean;
import com.liferay.portal.kernel.lar.PortletDataHandlerControl;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portlet.journal.service.JournalStructureLocalServiceUtil;
import com.liferay.portlet.journal.service.JournalTemplateLocalServiceUtil;
import com.protecmedia.iter.news.model.PageContent;
import com.protecmedia.iter.news.model.Qualification;
import com.protecmedia.iter.news.service.PageContentLocalServiceUtil;
import com.protecmedia.iter.news.service.PageContentServiceUtil;
import com.protecmedia.iter.news.service.QualificationLocalServiceUtil;
import com.protecmedia.iter.news.service.QualificationServiceUtil;
import com.protecmedia.iter.news.service.persistence.PageContentUtil;
import com.protecmedia.iter.news.service.persistence.QualificationUtil;

/**
 * @author protecmedia
 *
 */

public class PageContentDataHandlerImpl extends BasePortletDataHandler {

	/**
	 * @return what things must be exported and how
	 */
	public PortletDataHandlerControl[] getExportControls()
			throws PortletDataException {
		return new PortletDataHandlerControl[] { _qualifications, _page_contents };
	}

	/**
	 * @return what things must be imported and how
	 */
	public PortletDataHandlerControl[] getImportControls()
			throws PortletDataException {
		return new PortletDataHandlerControl[] { _qualifications, _page_contents };
	}
	
	/**
	 * @param context 
	 * 		portelt data context
	 * @param portletId 
	 * 		id of the portlet 
	 * @param preferences 
	 * 		portlet preferences
	 * @return a String showing the exportations
	 * @throws Exception
	 */
	protected String doExportData(PortletDataContext context, String portletId,
			PortletPreferences preferences) throws Exception {
		System.out.println("Doing Page Content Export");

		context.addPermissions("com.protecmedia.iter.news",
				context.getScopeGroupId());

		Document document = SAXReaderUtil.createDocument();

		Element rootElement = document.addElement("page-content-data");

		rootElement.addAttribute("group-id",
				String.valueOf(context.getScopeGroupId()));
		
		Element qualificationsElement = rootElement.addElement("qualifications");
		Element pageContentsElement = rootElement.addElement("page-contents");
		
		//para almacenar el listado de los de staging
		Element qualifStagingElement = rootElement.addElement("qualificationsList");
		Element pageContStagingElement = rootElement.addElement("pageContList");

		List<Qualification> qualifications = QualificationUtil.findByGroupFinder(context
				.getScopeGroupId());

		for (Qualification qualification : qualifications) {
			Element qualifElement = qualifStagingElement.addElement("qualif");
			qualifElement.addAttribute("qualifUuid", qualification.getUuid());			
			exportQualification(context, qualificationsElement, qualification);
		}
		
		List<PageContent> pageContents = PageContentUtil.findByGroupFinder(context.getScopeGroupId());

		for (PageContent pageContent : pageContents) {
			Element pageContElement = pageContStagingElement.addElement("pageCont");
			pageContElement.addAttribute("pageContUuid", pageContent.getUuid());	
			exportPageContent(context, pageContentsElement, pageContent);
		}
		
		return document.formattedString();
	}
	
	/**
	 * @param context
	 * 		portlet data context
	 * @param qualificationsElement
	 * 		element to add the qualifications
	 * @param qualification
	 * 		qualification to be added
	 * @throws Exception
	 */
	protected static void exportQualification(PortletDataContext context, Element qualificationsElement, 
			Qualification qualification) throws Exception {
		
		//Se comprueba si han sido modificados desde el último staging
		if (!context.isWithinDateRange(qualification.getModifiedDate())) {
			return;
		}
		
		Element qualificationElement = qualificationsElement.addElement("qualification");
		qualificationElement.addAttribute("qualificationId",
				String.valueOf(qualification.getId()));
		qualificationElement.addAttribute("name", qualification.getName());
		qualificationElement.addAttribute("uuid", qualification.getUuid());
		qualificationElement.addAttribute("qualifId", qualification.getQualifId());
	}
	
	/**
	 * @param context
	 * 		portlet data context
	 * @param pageContentsElement
	 * 		element to add the pagecontents
	 * @param pageContent
	 * 		pagecontent to be added
	 * @throws Exception
	 */
	protected static void exportPageContent(PortletDataContext context, Element pageContentsElement, 
			PageContent pageContent) throws Exception {
		
		//Se comprueba si han sido modificados desde el último staging
		if (!context.isWithinDateRange(pageContent.getModifiedDate())) {
			return;
		}
		
		Element pageContentElement = pageContentsElement.addElement("pageContent");
		pageContentElement.addAttribute("pageContentId",
				String.valueOf(pageContent.getId()));
		pageContentElement.addAttribute("uuid", pageContent.getUuid());
		pageContentElement.addAttribute("contentId", pageContent.getContentId());
		pageContentElement.addAttribute("layoutId", pageContent.getLayoutId());
		pageContentElement.addAttribute("qualifId", pageContent.getQualificationId());
		pageContentElement.addAttribute("typeContent", pageContent.getTypeContent());
		pageContentElement.addAttribute("orden",
				String.valueOf(pageContent.getOrden()));
		pageContentElement.addAttribute("articleModelId",
				String.valueOf(pageContent.getArticleModelId()));
		pageContentElement.addAttribute("desde",
				String.valueOf(pageContent.getVigenciadesde().getTime()));
		pageContentElement.addAttribute("hasta",
				String.valueOf(pageContent.getVigenciahasta().getTime()));
		pageContentElement.addAttribute("online", String.valueOf(pageContent.getOnline()));
		
	}

	/**
	 * @param context
	 * 		portlet data context
	 * @param portletId
	 * 		id of the portlet we are importing
	 * @param preferences
	 * 		preferences of the source portlet
	 * @param data
	 * 		exportation data
	 * @return the preferences of the new live portlet
	 * @throws Exception
	 */
	protected PortletPreferences doImportData(PortletDataContext context,
			String portletId, PortletPreferences preferences, String data)
			throws Exception {
		
		System.out.println("Doing Page Content Import");
		
		context.importPermissions(
				"com.protecmedia.iter.news", context.getSourceGroupId(),
				context.getScopeGroupId());
		
		Document document = SAXReaderUtil.read(data);

		Element rootElement = document.getRootElement();
		
		Element qualificationsElement = rootElement.element("qualifications");

		List<Element> qualifications = qualificationsElement.elements("qualification");
		
		for (Element element : qualifications) {
			String name = element.attributeValue("name");
			String uuid = element.attributeValue("uuid");
			String qualifId = element.attributeValue("qualifId");	
			importQualification(context, name, uuid, qualifId);
		}
		
		Element pageContentsElement = rootElement.element("page-contents");
		
		List<Element> pageContents = pageContentsElement.elements("pageContent");
		
		for (Element element : pageContents) {
			long pageContentId = Long.parseLong(element.attributeValue("pageContentId"));
			String uuid = element.attributeValue("uuid");
			String contentId = element.attributeValue("contentId");
			String layoutId = element.attributeValue("layoutId");
			String qualifId = element.attributeValue("qualifId");
			String typeContent = element.attributeValue("typeContent");
			int orden = Integer.parseInt(element.attributeValue("orden"));
			long articleModelId = Long.parseLong(element.attributeValue("articleModelId"));
			long desde = Long.parseLong(element.attributeValue("desde"));
			long hasta = Long.parseLong(element.attributeValue("hasta"));
			boolean online = Boolean.parseBoolean(element.attributeValue("online"));
			importPageContent(context, uuid, contentId, layoutId, qualifId, typeContent, orden, articleModelId, desde, hasta, online);
		}
		
		//se borran los que se hayan quedado descolgados
		Element qualifList = rootElement.element("qualificationsList");
		List<Element> qualifStaging = qualifList.elements("qualif");
		List<String> qualifUuids = new ArrayList<String>();
		for (Element element : qualifStaging) {
			String uuid = element.attributeValue("qualifUuid");
			qualifUuids.add(uuid);
		}
		removeDeletedQualifications(context, qualifUuids);

		Element pageContList = rootElement.element("pageContList");
		List<Element> pageContStaging = pageContList.elements("pageCont");
		List<String> pageContUuids = new ArrayList<String>();
		for (Element element : pageContStaging) {
			String uuid = element.attributeValue("pageContUuid");
			pageContUuids.add(uuid);
		}
		removeDeletedPageContents(context, pageContUuids);
		
		return preferences;
	}
	
	/**
	 * @param context
	 * 		portlet data context
	 * @param qualificationId
	 * 		id of the source qualification
	 * @throws Exception
	 */
	public void importQualification(PortletDataContext context, String name, String uuid, String qualifId) throws Exception {
		
		try {
			Qualification qualif = QualificationLocalServiceUtil.getQualificationByUuidAndGroupId(uuid, context.getScopeGroupId());
			qualif.getName();
			qualif.setGroupId(context.getScopeGroupId());
			qualif.setName(name);
			qualif.setUuid(uuid);
			qualif.setQualifId(qualifId);
			qualif.setModifiedDate(new Date());
			QualificationLocalServiceUtil.updateQualification(qualif);
			System.out.println("Updated Qualificacion: " + name);
		} catch (Exception e) {
			long quald = CounterLocalServiceUtil.increment();
			Qualification qualification = QualificationLocalServiceUtil.createQualification(quald);
			qualification.setGroupId(context.getScopeGroupId());
			qualification.setName(name);
			qualification.setUuid(uuid);
			qualification.setModifiedDate(new Date());
			qualification.setQualifId(qualifId);
			QualificationLocalServiceUtil.addQualification(qualification);
			System.out.println("Added Qualificacion: " + name);
		}		
	}
	
	/**
	 * @param context
	 * 		portlet data context
	 * @param pageContentId
	 * 		id of the source pageContent
	 * @throws Exception
	 */
	public void importPageContent(PortletDataContext context, String uuid, String contentId, String layoutId,
			String qualifId, String typeContent, int orden, long articleModelId, long desde, long hasta, boolean online) throws Exception {
		
		Date vigenciadesde = new Date();
		vigenciadesde.setTime(desde);
		Date vigenciahasta = new Date();
		vigenciahasta.setTime(hasta);
		
		try {
			PageContent pageCont = PageContentLocalServiceUtil.getPageContentByUuidAndGroupId(uuid, context.getScopeGroupId());
			pageCont.getContentId();
			pageCont.setUuid(uuid);
			pageCont.setContentId(contentId);
			pageCont.setLayoutId(layoutId);
			pageCont.setGroupId(context.getScopeGroupId());
			pageCont.setQualificationId(qualifId);
			pageCont.setVigenciadesde(vigenciadesde);
			pageCont.setVigenciahasta(vigenciahasta);
			pageCont.setModifiedDate(new Date());
			pageCont.setTypeContent(typeContent);
			pageCont.setOrden(orden);
			pageCont.setArticleModelId(articleModelId);
			pageCont.setOnline(online);
			PageContentLocalServiceUtil.updatePageContent(pageCont);
			System.out.println("Updated PageContent.");
		} catch (Exception e) {
			try {
				long pageContId = CounterLocalServiceUtil.increment();
				PageContent pageContent = PageContentLocalServiceUtil.createPageContent(pageContId);
				pageContent.setUuid(uuid);
				pageContent.setContentId(contentId);
				pageContent.setLayoutId(layoutId);
				pageContent.setGroupId(context.getScopeGroupId());
				pageContent.setQualificationId(qualifId);
				pageContent.setVigenciadesde(vigenciadesde);
				pageContent.setVigenciahasta(vigenciahasta);
				pageContent.setModifiedDate(new Date());
				pageContent.setTypeContent(typeContent);
				pageContent.setOrden(orden);
				pageContent.setArticleModelId(articleModelId);
				pageContent.setOnline(online);
				PageContent pc = PageContentLocalServiceUtil.addPageContent(pageContent);
				System.out.println("Added PageContent: " + pc.getId());
			} catch (Exception e2) {
				System.out.println(e);
			}
		}
	}
	
	
	/**
	 * @param context 
	 * 		portlet data context
	 * @param qualifStaging
	 * 		listado de uuids de las qualifications de staging
	 */
	public void removeDeletedQualifications(PortletDataContext context, List<String> qualifStaging) {
		try {
			List<Qualification> qualifLive = QualificationLocalServiceUtil.getQualifications(context.getScopeGroupId());
			if (qualifStaging == null || qualifStaging.size() < 1) {
				for (Qualification qualif : qualifLive) {
					QualificationLocalServiceUtil.deleteQualification(qualif);
					System.out.println("Deleted Qualificacion: " + qualif.getName());
				}
			} else {
				for (Qualification qualif : qualifLive) {
					if (!qualifStaging.contains(qualif.getUuid())) {
						QualificationLocalServiceUtil.deleteQualification(qualif);
						System.out.println("Deleted Qualificacion: " + qualif.getName());
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Problems removing deleted Qualifications.");
		}
	}
	
	/**
	 * @param context
	 * 		portlet data context
	 * @param pageContStaging
	 * 		listado de uuids de los page contents de staging
	 */
	public void removeDeletedPageContents(PortletDataContext context, List<String> pageContStaging) {
		try {
			List<PageContent> pageContLive = PageContentLocalServiceUtil.findPageGroupFinder(context.getScopeGroupId());
			if (pageContStaging == null || pageContStaging.size() < 1) {
				for (PageContent pageCont : pageContLive) {
					PageContentLocalServiceUtil.deletePageContent(pageCont);
					System.out.println("Deleted PageContent: " + pageCont.getId());
				}
			} else {
				for (PageContent pageCont : pageContLive) {
					if (!pageContStaging.contains(pageCont.getUuid())) {
						PageContentLocalServiceUtil.deletePageContent(pageCont);
						System.out.println("Deleted PageContent: " + pageCont.getId());
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Problems removing deleted PageContents.");
		}
	}	
	
	/**
	 * @return if we must export it always or not
	 */
	public boolean isAlwaysExportable() {
		return _ALWAYS_EXPORTABLE;
	}

	/**
	 * @return if it will be published by default
	 */
	public boolean isPublishToLiveByDefault() {
		return _DEFAULT_EXPORT;
	}

	//******VARIABLES Y CONSTANTES******//
	private static final boolean _ALWAYS_EXPORTABLE = true;

	private static final boolean _DEFAULT_EXPORT = true;

	private static final String _NAMESPACE = "page-content";

	private static Log _log = LogFactoryUtil
			.getLog(PageContentDataHandlerImpl.class);
	
	private static PortletDataHandlerBoolean _qualifications = new PortletDataHandlerBoolean(
			_NAMESPACE, "qualification", true, true);  //defaultState true disabled true
	
	private static PortletDataHandlerBoolean _page_contents = new PortletDataHandlerBoolean(
			_NAMESPACE, "page-content", true, true);  //defaultState true disabled true
	
}
