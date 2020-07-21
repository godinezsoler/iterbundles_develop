/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.designer.service.item;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liferay.portal.NoSuchLayoutException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.designer.model.PageTemplate;
import com.protecmedia.iter.designer.service.PageTemplateLocalServiceUtil;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

/*
	<item operation="delete" globalid="MODELO_PAGINA" classname="com.protecmedia.iter.designer.model.PageTemplate" groupid="Guest">
			<param name="image-file">&lt;![CDATA[]]&gt;</param>
			<param name="description">&lt;![CDATA[]]&gt;</param>
			<param name="name">&lt;![CDATA[modelo_pagina]]&gt;</param>
			<param name="type">&lt;![CDATA[page-template]]&gt;</param>
			<param name="url">&lt;![CDATA[/custompagestemplates/modelo_pagina]]&gt;</param>
			<param name="defaultTemplate">&lt;![CDATA[false]]&gt;</param>
		</item>
 */

public class PageTemplateXmlIO extends ItemXmlIO {
	
	private static Log _log = LogFactoryUtil.getLog(PageTemplateXmlIO.class);
	private String _className = IterKeys.CLASSNAME_PAGETEMPLATE;
	
	public PageTemplateXmlIO () {
		super();
	}
	
	public PageTemplateXmlIO (XMLIOContext xmlIOContext) {
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
	public void populateLive(long groupId, long companyId)
			throws SystemException, PortalException {

		List<PageTemplate> pageTemplateList = PageTemplateLocalServiceUtil.getPageTemplates(groupId);
		for (PageTemplate pageTemplate : pageTemplateList){	
			try{
				createLiveEntry(pageTemplate);
			} catch(Exception err){
				_log.error("Can't add Live, PageTemplate: " + pageTemplate.getPageTemplateId());
			}
		} 
		
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		PageTemplate pageTemplate = (PageTemplate)model;
		
		LiveLocalServiceUtil.add(_className, pageTemplate.getGroupId(), -1, 0, 
				IterLocalServiceUtil.getSystemName() + "_" + pageTemplate.getPageTemplateId(), pageTemplate.getPageTemplateId(),
				IterKeys.CREATE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
	}

	@Override
	public void deleteLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		PageTemplate pageTemplate = (PageTemplate)model;
		
		LiveLocalServiceUtil.add(_className, pageTemplate.getGroupId(), 
				IterLocalServiceUtil.getSystemName() + "_" + pageTemplate.getPageTemplateId(), pageTemplate.getPageTemplateId(),
				IterKeys.DELETE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
	}
	
	/*
	 * Export Functions
	 */	
	@Override
	protected String createItemXML(XMLIOExport xmlioExport, Element root, String operation, Group group, Live live) {
		_log.trace("In PageTemplateXmlIO.createItemXML");
		
		String error = "";
		
		Map<String, String> attributes = new HashMap<String, String>();
		Map<String, String> params = new HashMap<String, String>();

		setCommonAttributes(attributes, group.getName(), live, operation);
				
		//si la operacion NO es de borrado
		if (operation.equals(IterKeys.CREATE)){		
			PageTemplate pageTemplate = null;
			try {
				pageTemplate = PageTemplateLocalServiceUtil.getPageTemplateByPageTemplateId(group.getGroupId(), live.getLocalId());
				Live liveLayout = LiveLocalServiceUtil.getLiveByLocalId(group.getGroupId(), IterKeys.CLASSNAME_LAYOUT, String.valueOf(pageTemplate.getLayoutId()));
			
				params.put("pagetemplateid", pageTemplate.getPageTemplateId());
				params.put("name", pageTemplate.getName());
				params.put("description", pageTemplate.getDescription());
				params.put("type", pageTemplate.getType());
				params.put("defaultTemplate", String.valueOf(pageTemplate.getDefaultTemplate()));
				params.put("defaultMobileTemplate", String.valueOf(pageTemplate.getDefaultMobileTemplate()));
				params.put("layout", liveLayout.getGlobalId());
				params.put("image-file", XMLIOUtil.exportImageFile(xmlioExport, pageTemplate.getImageId()));			
			
			} catch (Exception e) {
				//LiveLocalServiceUtil.setError(live.getId(), IterKeys.CORRUPT, "Can't export item");			
				error = "Cannot export item";
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
	protected void delete(Element item) {
		String sGroupId = getAttribute(item, "groupid");		
		String globalId = getAttribute(item, "globalid");	
		
		try{
			long groupId = getGroupId(sGroupId);
			
			try {
				//Get live to get the element localId
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
				
				try{	
					PageTemplate pageTemplate = PageTemplateLocalServiceUtil.getPageTemplateByPageTemplateId(groupId, live.getLocalId());				
					if (pageTemplate != null) {	
							
						try {						
							PageTemplateLocalServiceUtil.deletePageTemplateId(pageTemplate.getId());
							
							//update entry in live table
							LiveLocalServiceUtil.add(_className, groupId, globalId,
									live.getLocalId(), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
													
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Done", IterKeys.DONE, sGroupId);
						} catch (Exception e) {
						
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Error: " + e.toString(), IterKeys.ERROR, sGroupId);
						}
					} else {
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Element not found", IterKeys.ERROR, sGroupId);
					}
				} catch (Exception e) {
					if (live != null){
						//clean entry in live table
						LiveLocalServiceUtil.add(_className, groupId, globalId,
							live.getLocalId(), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
					}
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Element not found", IterKeys.DONE, sGroupId);
				}
			} catch (Exception e) {			
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Element not found", IterKeys.ERROR, sGroupId);
			} 
		} catch (Exception e) {
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}
	}
	
	@Override
	protected void modify(Element item, Document doc) {
		_log.trace("In PageTemplateXmlIO.modify");
		
		String sGroupId = getAttribute(item, "groupid");		
		String globalId = getAttribute(item, "globalid");	
				
		String pageTemplateId = getParamTextByName(item, "pagetemplateid");
		String name = getParamTextByName(item, "name");		
		String type = getParamTextByName(item, "type");			
		String description = getParamTextByName(item, "description");
		String layout = getParamTextByName(item, "layout");		
		boolean defaultTemplate = GetterUtil.getBoolean(getParamTextByName(item, "defaultTemplate"), false);
		boolean defaultMobileTemplate = GetterUtil.getBoolean(getParamTextByName(item, "defaultMobileTemplate"), false);
		String imageFile = getParamTextByName(item, "image-file");
		
		PageTemplate oldDefaultPT = null;
		PageTemplate oldDefaultMobilePT = null;
		
		try{
			long groupId = getGroupId(sGroupId);
			Group group = GroupLocalServiceUtil.getGroup(groupId);
			
			if( type.equalsIgnoreCase(IterKeys.DESIGNER_PAGE_TEMPLATE_ARTICLE_TYPE) )
			{
				xmlIOContext.setPublishArticleTemplate(true);
				
				if( defaultTemplate )
					oldDefaultPT = PageTemplateLocalServiceUtil.getDefaultPageTemplate(groupId, IterKeys.DESIGNER_PAGE_TEMPLATE_ARTICLE_TYPE);
				
				if( defaultMobileTemplate )
					oldDefaultMobilePT = PageTemplateLocalServiceUtil.getDefaultMobilePageTemplate(groupId, IterKeys.DESIGNER_PAGE_TEMPLATE_ARTICLE_TYPE);;
			}
			
			//Update image
			long imageId = XMLIOUtil.importImageFile (imageFile);
			
			try {
				//Get live to get the element localId
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
				
				try{
					Live liveLayout = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_LAYOUT, layout);
			
					try{				
						PageTemplate pageTemplate = PageTemplateLocalServiceUtil.getPageTemplateByPageTemplateId(groupId, live.getLocalId());				
											
						//UPDATE
						try {										
							pageTemplate = PageTemplateLocalServiceUtil.updatePageTemplate(pageTemplate.getId(), GetterUtil.getLong(liveLayout.getLocalId()), name, description, type, defaultTemplate, imageId, defaultMobileTemplate);
								
							//update entry in live table
							LiveLocalServiceUtil.add(_className, group.getGroupId(), globalId,
									pageTemplate.getPageTemplateId(), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
								
								
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
						} catch (Exception e) {
							
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e.toString(), IterKeys.ERROR, group.getName());
						}
					}catch(Exception e2){
						if (live == null || !live.getOperation().equals(IterKeys.DELETE)){
							//Existe la entrada en Live pero no existe el elemento. Borramos en Live para volver realizar una inserción completa.
							if (live != null){
								LiveLocalServiceUtil.deleteLiveById(live.getId());
							}
							
							//CREATE	
							PageTemplate pageTemplate = null;
							try {						
								pageTemplate = PageTemplateLocalServiceUtil.addPageTemplate_PTI_DT_IMG_DMT(group.getGroupId(), GetterUtil.getLong(liveLayout.getLocalId()), pageTemplateId, name, description, type, defaultTemplate, imageId, defaultMobileTemplate);
									
								//update entry in live table
								LiveLocalServiceUtil.add(_className, group.getGroupId(), globalId,
										pageTemplate.getPageTemplateId(), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
															
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
							} catch (NoSuchLayoutException e1) {
//								long parentId = getLayoutIdFromFriendlyURL(group.getGroupId(), "");
								long parentId = LayoutConstants.DEFAULT_PARENT_LAYOUT_ID;
																
								pageTemplate = PageTemplateLocalServiceUtil.createPageTemplate_PTI_PI_DT_IMG_URL_DMT(group.getGroupId(), pageTemplateId, name, description, type, xmlIOContext.getUserId(), parentId, defaultTemplate, imageId, StringPool.BLANK, defaultMobileTemplate);
									
								//update entry in live table
								LiveLocalServiceUtil.add(_className, group.getGroupId(), globalId,
										pageTemplate.getPageTemplateId(), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
									
								try{
									//Update globalId.
									LiveLocalServiceUtil.updateGlobalId(group.getGroupId(), _className, pageTemplate.getPageTemplateId(), globalId);
								}catch(Exception e3){
									xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicated globalId", IterKeys.ERROR, group.getName());
								}
								
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
							}
						}
					}
					
					if( oldDefaultPT!=null && !oldDefaultPT.getPageTemplateId().equals(pageTemplateId) )
					{
						oldDefaultPT.setDefaultTemplate(false);
						PageTemplateLocalServiceUtil.updatePageTemplate(oldDefaultPT, false);
					}
					
					if( oldDefaultMobilePT!=null && !oldDefaultMobilePT.getPageTemplateId().equals(pageTemplateId) )
					{
						oldDefaultMobilePT.setDefaultMobileTemplate(false);
						PageTemplateLocalServiceUtil.updatePageTemplate(oldDefaultMobilePT, false);
					}
					
				}catch(Exception e2){
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Parent not found", IterKeys.ERROR, sGroupId);
				}
			}catch(Exception e2){
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e2.toString(), IterKeys.ERROR, sGroupId);
			}	
		} catch (Exception e) {
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}

	}
	
}
