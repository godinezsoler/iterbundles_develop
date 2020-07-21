/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.service.item.documentlibrary;

/*
 * 
<item operation="create" globalid="preview1_12208" classname="com.liferay.portlet.documentlibrary.model.DLFolder" groupid="Guest">
			<param name="parentfoldergid">&lt;![CDATA[preview1_12202]]&gt;</param>
			<param name="description">&lt;![CDATA[]]&gt;</param>
			<param name="name">&lt;![CDATA[subfolder1]]&gt;</param>
		</item>
 */


import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFolderLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;


public class DLFolderXmlIO extends ItemXmlIO {
	
	private static Log _log = LogFactoryUtil.getLog(DLFolderXmlIO.class);
	private String _className = IterKeys.CLASSNAME_DLFOLDER;
	
	public DLFolderXmlIO() {
		super();
	}
	
	public DLFolderXmlIO(XMLIOContext xmlIOContext) {
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
	public void populateLive(long groupId, long companyId) throws SystemException{
		
		DLFileEntryXmlIO dlfeXmlio = new DLFileEntryXmlIO();
		
		//Global and current group		
		List<DLFolder> folderList = DLFolderLocalServiceUtil.getFolders(companyId);
		for (DLFolder folder : folderList){			
			try {
				createLiveEntry(folder);
			} catch (PortalException e) {
				_log.error("Can't add Live, DLFolder: " + folder.getFolderId());
			}
			
			//-- DLFileEntry --
			List<DLFileEntry> fileEntryList = DLFileEntryLocalServiceUtil.getFileEntries(folder.getGroupId(), folder.getFolderId());
			for (DLFileEntry fileEntry : fileEntryList){	
				try {
					dlfeXmlio.createLiveEntry(fileEntry);
				} catch (PortalException e) {
					_log.error("Can't add Live, DLFileEntry: " + fileEntry.getFileEntryId());
				}
			}
		}	
		
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		DLFolder dlFolder = (DLFolder)model;
		
		String id = String.valueOf(dlFolder.getFolderId());
		
		Live liveGroup = LiveLocalServiceUtil.getLiveByLocalId(dlFolder.getGroupId(), IterKeys.CLASSNAME_GROUP, String.valueOf(dlFolder.getGroupId()));
		
		//insert element in LIVE
		LiveLocalServiceUtil.add(_className, dlFolder.getGroupId(), liveGroup.getId(), liveGroup.getId(), 
				IterLocalServiceUtil.getSystemName() + "_" + id, id, 
				IterKeys.CREATE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
		
	}
	
	@Override
	public void deleteLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		DLFolder dlFolder = (DLFolder)model;
		
		String id = String.valueOf(dlFolder.getFolderId());
		
		//insert element in LIVE
		LiveLocalServiceUtil.add(_className, dlFolder.getGroupId(),
				IterLocalServiceUtil.getSystemName() + "_" + id, id, 
				IterKeys.DELETE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);			
		
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
		
			try{
				DLFolder dlFolder = DLFolderLocalServiceUtil.getDLFolder(GetterUtil.getLong(live.getLocalId()));
				
				params.put("description", dlFolder.getDescription());									
				params.put("name", dlFolder.getName());	
				
				long pfid = dlFolder.getParentFolderId();
				if(pfid != 0){
					Live liveFolder = LiveLocalServiceUtil.getLiveByLocalId(group.getGroupId(), _className, String.valueOf(pfid));
					params.put("parentfoldergid", liveFolder.getGlobalId());
				}else{
					params.put("parentfoldergid", "0");
				}
				
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
	 * Import functions
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
					String folderId = live.getLocalId(); 
					DLFolder dlFolder = DLFolderLocalServiceUtil.getDLFolder(GetterUtil.getLong(folderId));
					
					try{
						DLFolderLocalServiceUtil.deleteFolder(dlFolder.getFolderId());
							
						//update entry in live table
						LiveLocalServiceUtil.add(_className, groupId, globalId,
								folderId, IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
						
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Done", IterKeys.DONE, sGroupId);
					}catch(Exception e2){									
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Error: " + e2.toString(), IterKeys.ERROR, sGroupId);
					}
				}catch(Exception e1){
					if (live != null){
						//clean entry in live table
						LiveLocalServiceUtil.add(_className, groupId, globalId,
								live.getLocalId(), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
					}	
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Done", IterKeys.DONE, sGroupId);
					
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
		String sGroupId = getAttribute(item, "groupid");
		String globalId = getAttribute(item, "globalid");
	
		String description = getParamTextByName(item, "description");		
		String parentFolderGId = getParamTextByName(item, "parentfoldergid");
		String name = getParamTextByName(item, "name");
		
		try{
			long groupId = getGroupId(sGroupId);
			Group group = GroupLocalServiceUtil.getGroup(groupId);
			
			//If parent folder isn't ROOT, get parentFolderId
			if(!parentFolderGId.equals("0")){
				try{
					Live live= LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, parentFolderGId);
					parentFolderGId = live.getLocalId();
				}catch(Exception e){
					parentFolderGId = "-1";
				}
			}
			
			if (!parentFolderGId.equals("-1")){
				
				try{				
					//Get live to get the element localId
					Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
			
					ServiceContext sc = new ServiceContext();
					sc.setAddCommunityPermissions(true);
					sc.setAddGuestPermissions(true);
					
					try{				
						DLFolder folder = DLFolderLocalServiceUtil.getFolder(GetterUtil.getLong(live.getLocalId()));
						
						//UPDATE				
						try{
							DLFolderLocalServiceUtil.updateFolder(folder.getFolderId(), GetterUtil.getLong(parentFolderGId), name, description, sc);
						
							//update entry in live table
							LiveLocalServiceUtil.add(_className, group.getGroupId(), globalId,
									String.valueOf(folder.getFolderId()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
							
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
							
						}catch(Exception dfe){
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + dfe.toString(), IterKeys.ERROR, group.getName());
						}	
					}catch(Exception e){ //Element doesn't exist. Let's create it.
						
						if (live == null || !live.getOperation().equals(IterKeys.DELETE)){
							//Existe la entrada en Live pero no existe el elemento. Borramos en Live para volver realizar una inserción completa.
							if (live != null){
								LiveLocalServiceUtil.deleteLiveById(live.getId());
							}
							
							//CREATE						
							try{
								DLFolder dlFolder = DLFolderLocalServiceUtil.addFolder(xmlIOContext.getUserId(), group.getGroupId(), GetterUtil.getLong(parentFolderGId), name, description, sc);
								
								//update entry in live table
								LiveLocalServiceUtil.add(_className, group.getGroupId(), globalId,
										String.valueOf(dlFolder.getFolderId()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
								
								try{
									//update globalId in live. Necessary for child folders.
									LiveLocalServiceUtil.updateGlobalId(group.getGroupId(), _className, String.valueOf(dlFolder.getFolderId()), globalId);
								}catch(Exception e1){
									xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicated globalId", IterKeys.ERROR, group.getName());
								}
								
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
								
							}catch(Exception dfe){
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + dfe.toString(), IterKeys.ERROR, group.getName());
							}	
						}
					}
				} catch (Exception e) {
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e.toString(), IterKeys.ERROR, sGroupId);
				}
			}else{
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "parentFolderId not found", IterKeys.ERROR, sGroupId);
			}	
			
		} catch (Exception e) {
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}		
	}		
	
}
