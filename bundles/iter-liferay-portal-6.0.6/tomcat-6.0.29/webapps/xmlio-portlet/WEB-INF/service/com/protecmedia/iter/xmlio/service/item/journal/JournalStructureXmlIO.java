/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.service.item.journal;

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
import com.liferay.portlet.journal.model.JournalStructure;
import com.liferay.portlet.journal.service.JournalStructureLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;
import com.protecmedia.iter.xmlio.service.util.XMLIOImport;

/* XML Structure:
 * 
 */
public class JournalStructureXmlIO extends ItemXmlIO {
	
	private static Log _log = LogFactoryUtil.getLog(JournalStructureXmlIO.class);
	private String _className = IterKeys.CLASSNAME_JOURNALSTRUCTURE;
	
	public JournalStructureXmlIO () {
		super();
	}
	
	public JournalStructureXmlIO (XMLIOContext xmlIOContext) {
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
			throws SystemException {
		
		try{
			long globalGroupId = CompanyLocalServiceUtil.getCompany(companyId).getGroup().getGroupId();
			
			List<JournalStructure> globalStructureList = JournalStructureLocalServiceUtil.getStructures(globalGroupId);
			for (JournalStructure globalStructure : globalStructureList){
				try {
					createLiveEntry(globalStructure);
				} catch (PortalException e) {
					_log.error("Can't add Live, JournalStructure: " + globalStructure.getStructureId());
				}
			}
		} catch (PortalException e) {
			_log.error("Can't add Live, JournalStructures from Global group ");
		}
		
		List<JournalStructure> structureList = JournalStructureLocalServiceUtil.getStructures(groupId);
		for (JournalStructure structure : structureList){			
			try {
				createLiveEntry(structure);
			} catch (PortalException e) {
				_log.error("Can't add Live, JournalStructure: " + structure.getStructureId());
			}
		}
		
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		JournalStructure structure = (JournalStructure)model;
		
		String id = structure.getStructureId();
		
		Live liveGroup = LiveLocalServiceUtil.getLiveByLocalId(structure.getGroupId(), IterKeys.CLASSNAME_GROUP, String.valueOf(structure.getGroupId()));
		
		LiveLocalServiceUtil.add(_className,
				structure.getGroupId(), liveGroup.getId(), liveGroup.getId(), IterLocalServiceUtil.getSystemName() + "_" + id, id,
				IterKeys.CREATE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);	
	}
	@Override
	public  void deleteLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		JournalStructure structure = (JournalStructure)model;
		
		String id = structure.getStructureId();
		
		LiveLocalServiceUtil.add(_className, structure.getGroupId(), IterLocalServiceUtil.getSystemName() + "_" + id, 
				id, IterKeys.DELETE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
		
	}

	/*
	 * Export Functions
	 */

	@Override
	protected String createItemXML(XMLIOExport xmlioExport, Element root, String operation, Group group, Live live) {
		
		String error = "";
		
		Map<String, String> attributes = new HashMap<String, String>();
		Map<String, String> params = new HashMap<String, String>();
		
		setCommonAttributes(attributes, group.getName(), live, operation);
		
		//Put necessary parameters for each kind of operation.
		if (operation.equals(IterKeys.CREATE)){	
			
			JournalStructure structure = null;
			
			try {				
				structure = JournalStructureLocalServiceUtil.getStructure(group.getGroupId(), live.getLocalId());
				
//				String fileName = "structures" + StringPool.SLASH + structure.getStructureId().toLowerCase() + ".xml";
				
				String fileName = "structures" + StringPool.SLASH + structure.getId() + ".xml";
				
				attributes.put("parentid", structure.getParentStructureId());
				
				params.put("ident", structure.getStructureId());
				params.put("name", structure.getName());
				params.put("description", structure.getDescription());							
				params.put("file", fileName);
					
				xmlioExport.addResource(fileName,  structure.getXsd().getBytes());
				
			}catch(Exception e){
				//LiveLocalServiceUtil.setError(live.getId(), IterKeys.CORRUPT, "Can't export item");			
				error = "Cannot export item";
			}
		}
		
		addNode(root, "item", attributes, params);	
		
		_log.debug("INFO: Structure exported");
	
		return error;
	}
	
	/*
	 * Import Functions 
	 */
	
	@Override
	protected void delete(Element item) {	
		
		//INFO: Only it's possible to delete a structure if it's a leaf and has no templates.
		
		String sGroupId = getAttribute(item, "groupid");		
		String globalId = getAttribute(item, "globalid");	
		String classNameValue = getAttribute(item, "classname");
		
		try{
			long groupId = getGroupId(sGroupId);
			
			try{
				//Get live to get the element localId
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, classNameValue, globalId);
				
				try {
					
					JournalStructure structure = JournalStructureLocalServiceUtil.getStructure(groupId, live.getLocalId());
					
					try{
						JournalStructureLocalServiceUtil.deleteStructure(structure);
						
						//update entry in live table
						LiveLocalServiceUtil.add(_className, groupId, globalId,
								live.getLocalId(), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
										
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
			} catch (Exception e) {					
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
		String parentStructureId = GetterUtil.getString(getAttribute(item, "parentid"), StringPool.BLANK);
			
		String ident = getParamTextByName(item, "ident");	
		String name = getParamTextByName(item, "name");		
		String description = getParamTextByName(item, "description");			
		String file = getParamTextByName(item, "file");

		try{
			long groupId = getGroupId(sGroupId);
			Group group = GroupLocalServiceUtil.getGroup(groupId);
			
			try{			
				//Get live to get the element localId
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
				
				try{
					JournalStructure structure = JournalStructureLocalServiceUtil.getStructure(groupId, live.getLocalId());
					
					//UPDATE---
					try{
						String xsd = XMLIOImport.getFileAsString(file, "UTF-8");

						ServiceContext serviceContext = new ServiceContext();

						serviceContext.setAddCommunityPermissions(true);
						serviceContext.setAddGuestPermissions(true);
						
						JournalStructureLocalServiceUtil.updateStructure(group.getGroupId(), structure.getStructureId(), parentStructureId, 
								name, description, xsd, serviceContext);
						
						//update entry in live table
						LiveLocalServiceUtil.add(_className, group.getGroupId(), globalId,
								structure.getStructureId(), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
						
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
					} catch (Exception e) {					
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e.toString(), IterKeys.ERROR, group.getName());
					}
					//----
				} catch (Exception e) {
					if (live == null || !live.getOperation().equals(IterKeys.DELETE)){							
						//Existe la entrada en Live pero no existe el elemento. Borramos en Live para volver realizar una inserción completa.
						if (live != null){
							LiveLocalServiceUtil.deleteLiveById(live.getId());
						}
						
						//CREATE----
						try{
							String xsd = XMLIOImport.getFileAsString(file, "UTF-8");

							ServiceContext serviceContext = new ServiceContext();

							serviceContext.setAddCommunityPermissions(true);
							serviceContext.setAddGuestPermissions(true);
							
											
							JournalStructureLocalServiceUtil.addStructure(xmlIOContext.getUserId(), group.getGroupId(), ident, false, parentStructureId, name, description, xsd, serviceContext);		
						
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
						//---
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
