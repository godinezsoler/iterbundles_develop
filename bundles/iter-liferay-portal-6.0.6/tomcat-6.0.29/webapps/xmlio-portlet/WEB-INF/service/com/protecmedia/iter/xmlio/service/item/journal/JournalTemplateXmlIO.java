/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.service.item.journal;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.journal.model.JournalTemplate;
import com.liferay.portlet.journal.service.JournalTemplateLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;
import com.protecmedia.iter.xmlio.service.util.XMLIOImport;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;


/* 
 * XML Template:
 */
public class JournalTemplateXmlIO extends ItemXmlIO {
	
	private static Log _log = LogFactoryUtil.getLog(JournalTemplateXmlIO.class);
	private String _className = IterKeys.CLASSNAME_JOURNALTEMPLATE;
	
	public JournalTemplateXmlIO () {
		super();
	}
	
	public JournalTemplateXmlIO(XMLIOContext xmlIOContext) {
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
			throws SystemException{
		
		try{
			long globalGroupId = CompanyLocalServiceUtil.getCompany(companyId).getGroup().getGroupId();
			
			//Global and current group
			List<JournalTemplate> globalTemplateList = JournalTemplateLocalServiceUtil.getTemplates(globalGroupId);
			for (JournalTemplate globalTemplate : globalTemplateList){	
				try {
					createLiveEntry(globalTemplate);
				} catch (PortalException e) {
					_log.error("Can't add Live, JournalTemplate: " + globalTemplate.getTemplateId());
				}
			}
		} catch (PortalException e) {
			_log.error("Can't add Live, JournalTemplates from Global group");
		}
			
		List<JournalTemplate> templateList = JournalTemplateLocalServiceUtil.getTemplates(groupId);
		for (JournalTemplate template : templateList){	
			try {
				createLiveEntry(template);
			} catch (PortalException e) {
				_log.error("Can't add Live, JournalTemplate: " + template.getTemplateId());
			}
		}
		
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		JournalTemplate template = (JournalTemplate)model;
		
		String id = template.getTemplateId();
		
		Live liveGroup = LiveLocalServiceUtil.getLiveByLocalId(template.getGroupId(), IterKeys.CLASSNAME_GROUP, String.valueOf(template.getGroupId()));
		
		LiveLocalServiceUtil.add(_className, template.getGroupId(), liveGroup.getId(), liveGroup.getId(), IterLocalServiceUtil.getSystemName() + "_" + id, 
				id, IterKeys.CREATE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
	}
	
	@Override
	public void deleteLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		JournalTemplate template = (JournalTemplate)model;
		
		String id = template.getTemplateId();
		
		LiveLocalServiceUtil.add(_className, template.getGroupId(), IterLocalServiceUtil.getSystemName() + "_" + id, 
				id, IterKeys.DELETE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);

	}

	
	/*
	 * Export Functions
	 */

	@Override
	protected String createItemXML(XMLIOExport xmlioExport, Element root, String operation, Group group,
			Live live) {
	
		String error = "";
		
		Map<String, String> attributes = new HashMap<String, String>();
		Map<String, String> params = new HashMap<String, String>();

		setCommonAttributes(attributes, group.getName(), live, operation);
		
		try {
		
			//Put necessary parameters for each kind of operation.
			if (operation.equals(IterKeys.CREATE)){			
			
				JournalTemplate template = JournalTemplateLocalServiceUtil.getTemplate(group.getGroupId(), live.getLocalId());
				
				//String fileName = "templates" + StringPool.SLASH + template.getTemplateId().toLowerCase() + "." + template.getLangType();
				
				String fileName = "templates" + StringPool.SLASH + template.getId() + "." + template.getLangType();
				
				params.put("ident", template.getTemplateId());
				params.put("name", template.getName());
				params.put("description", template.getDescription());
				params.put("structure", template.getStructureId());
				params.put("file", fileName);
				params.put("langtype", template.getLangType());
				params.put("cacheable", String.valueOf(template.getCacheable()));
				params.put("small-image-url", template.getSmallImageURL());
				params.put("small-image", String.valueOf(template.getSmallImage()));		
				params.put("small-image-file", XMLIOUtil.exportImageFile(xmlioExport, template.getSmallImageId()));	
						
				xmlioExport.addResource(fileName,  template.getXsl().getBytes());
				
			}
			
			addNode(root, "item", attributes, params);	
		
		} catch (Exception e) {			
			error = "Cannot export item";
		}
		
		_log.debug("INFO: Template exported");		

		return error;
	}

	/*
	 * Import Functions
	 */
		
	@Override
	protected void delete (Element item) {
		
		String sGroupId = getAttribute(item, "groupid");		
		String globalId = getAttribute(item, "globalid");
		String className = getAttribute(item, "classname");
							
		try{
			long groupId = getGroupId(sGroupId);
			
			try{				
				//Get live to get the element localId
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, className, globalId);				
				
				try {					
					JournalTemplate template = JournalTemplateLocalServiceUtil.getTemplate(groupId, live.getLocalId());
					
					try{
						JournalTemplateLocalServiceUtil.deleteTemplate(groupId, template.getTemplateId());
						
						//update entry in live table
						LiveLocalServiceUtil.add(_className, groupId, globalId,
								template.getTemplateId(), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
					
						
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Done", IterKeys.DONE, sGroupId);
						
					} catch (Exception e) {					
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Error: " + e.toString(), IterKeys.ERROR, sGroupId);
					}
					
				} catch (Exception e) {	
					if (live != null){
						//clean entry in live table
						LiveLocalServiceUtil.add(_className, groupId, globalId,
								live.getLocalId(), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
					}
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Element not found", IterKeys.DONE, sGroupId);
				}
			}catch(Exception e){
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Error: " + e.toString(), IterKeys.ERROR, sGroupId);
			}			
		} catch (Exception e) {
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}
	}

	@Override
	protected void modify(Element item, Document doc) {
		
		String sGroupId = getAttribute(item, "groupid");		
		String globalId = getAttribute(item, "globalid");
		String className = getAttribute(item, "classname");
		
		String ident = getParamTextByName(item, "ident");	
		String name = getParamTextByName(item, "name");	
		String description = getParamTextByName(item, "description");			
		String file = getParamTextByName(item, "file");
		String structure = getParamTextByName(item, "structure");
		String langType = GetterUtil.getString(getParamTextByName(item, "langtype"),"vm");
		boolean cacheable = GetterUtil.getBoolean(getParamTextByName(item, "cacheable"), true);
		String smallImageURL = getParamTextByName(item, "small-image-url");
		boolean smallImage = GetterUtil.getBoolean(getParamTextByName(item, "small-image"), false);
		String smallImageFile = getParamTextByName(item, "small-image-file");
				
		try{
			long groupId = getGroupId(sGroupId);
			Group group = GroupLocalServiceUtil.getGroup(groupId);
			
			try{
				//Get live to get the element localId
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, className, globalId);
			
				//--- Pedro 12/07/2011 : No sé para que sirven estas variables (pero se usan en el update)
				boolean formatXsl = false;
				File smallFile = null;
				//------
				
				String xsl = XMLIOImport.getFileAsString(file, "UTF-8");

				ServiceContext serviceContext = new ServiceContext();

				serviceContext.setAddCommunityPermissions(true);
				serviceContext.setAddGuestPermissions(true);
				
				try {					
					JournalTemplate template = JournalTemplateLocalServiceUtil.getTemplate(groupId, live.getLocalId());
					
					//UPDATE
					try {
						//Main update
						JournalTemplate jt = JournalTemplateLocalServiceUtil.updateTemplate(group.getGroupId(), template.getTemplateId(), structure, name, description, xsl, formatXsl, langType, cacheable, smallImage, smallImageURL, smallFile, serviceContext);
						
						//Update small image
						long imageId = XMLIOUtil.importImageFile (smallImageFile);
						if (imageId != -1){
							jt.setSmallImageId(imageId);
						}
						
						//update entry in live table
						LiveLocalServiceUtil.add(_className, group.getGroupId(), globalId,
								template.getTemplateId(), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
												
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
						
					} catch (Exception e) {					
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " +e.toString(), IterKeys.ERROR, group.getName());
					}
				} catch (Exception e) {
					if (live == null || !live.getOperation().equals(IterKeys.DELETE)){							
						//Existe la entrada en Live pero no existe el elemento. Borramos en Live para volver realizar una inserción completa.
						if (live != null){
							LiveLocalServiceUtil.deleteLiveById(live.getId());
						}
						
						//CREATE
						try {	
							//Add main template
							JournalTemplate jt = JournalTemplateLocalServiceUtil.addTemplate(xmlIOContext.getUserId(), group.getGroupId(), ident, false, structure, name, description, xsl, formatXsl, langType, cacheable, smallImage, smallImageURL, smallFile, serviceContext); 
							
							//Update small image
							long imageId = XMLIOUtil.importImageFile (smallImageFile);
							if (imageId != -1){
								jt.setSmallImageId(imageId);
							}
							
							//update entry in live table
							LiveLocalServiceUtil.add(_className, group.getGroupId(), 0, 0, globalId,
									ident, IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
							
							try{
								//Update globalId.
								LiveLocalServiceUtil.updateGlobalId(group.getGroupId(), _className, ident, globalId);
							}catch(Exception e3){
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicated globalId", IterKeys.ERROR, group.getName());
							}
							
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
						} catch (Exception e1) {
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e1.toString(), IterKeys.ERROR, group.getName());
						}
					}					
				}
			}catch(Exception e){
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e.toString(), IterKeys.ERROR, sGroupId);
			}
		} catch (Exception e) {
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}
	}
	
}
