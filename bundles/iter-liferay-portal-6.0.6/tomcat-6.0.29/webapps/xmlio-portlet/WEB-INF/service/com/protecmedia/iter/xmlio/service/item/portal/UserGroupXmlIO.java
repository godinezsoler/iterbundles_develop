/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.service.item.portal;

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
import com.liferay.portal.model.UserGroup;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.UserGroupLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;

/*
 * Group of Users
 */

public class UserGroupXmlIO extends ItemXmlIO {
	
	private static Log _log = LogFactoryUtil.getLog(UserGroupXmlIO.class);
	private String _className = IterKeys.CLASSNAME_USERGROUP;
	
	public UserGroupXmlIO () {		
		super();
	}
	
	public UserGroupXmlIO (XMLIOContext xmlIOContext) {		
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

		//Only exists in global group
		List<UserGroup> userGroups = UserGroupLocalServiceUtil.getUserGroups(companyId);
		for (UserGroup userGroup : userGroups){				
			try {
				createLiveEntry(userGroup);
			} catch (PortalException e) {
				_log.error("Can't add Live, UserGroup: " + userGroup.getUserGroupId());
			}
		}
		
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		UserGroup userGroup = (UserGroup)model;
		
		String id = String.valueOf(userGroup.getUserGroupId());		
		long globalGroupId = CompanyLocalServiceUtil.getCompany(userGroup.getCompanyId()).getGroup().getGroupId();
				
		Live liveGroup = LiveLocalServiceUtil.getLiveByLocalId(globalGroupId, IterKeys.CLASSNAME_GROUP, String.valueOf(globalGroupId));
		
		LiveLocalServiceUtil.add(_className, globalGroupId, liveGroup.getId(), liveGroup.getId(),
				IterLocalServiceUtil.getSystemName() + "_" + id, id,
				IterKeys.CREATE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);	
	}
	
	@Override
	public void deleteLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		UserGroup userGroup = (UserGroup)model;
		
		String id = String.valueOf(userGroup.getUserGroupId());
		long globalGroupId = CompanyLocalServiceUtil.getCompany(userGroup.getCompanyId()).getGroup().getGroupId();
		
		LiveLocalServiceUtil.add(_className, globalGroupId, IterLocalServiceUtil.getSystemName() + "_" + id, 
				id, IterKeys.DELETE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
		
	}
	
	/*
	 * Export Functions
	 */
	@Override
	protected String createItemXML(XMLIOExport xmlioExport, Element root, String operation, Group group, Live live){
		
		String error = "";
		
		Map<String, String> attributes = new HashMap<String, String>();
		Map<String, String> params = new HashMap<String, String>();
		
		setCommonAttributes(attributes, group.getName(), live, operation);
				
		//si la estructura no es nula, agrego parametros avanzados
		if (operation.equals(IterKeys.CREATE)){			
			try {
				UserGroup usergroup = UserGroupLocalServiceUtil.getUserGroup(GetterUtil.getLong(live.getLocalId()));
								
				params.put("name", usergroup.getName());	
				params.put("description", usergroup.getDescription());
		
				Live parentUserGroupLive = LiveLocalServiceUtil.getLiveByLocalId(usergroup.getCompanyId(), _className, String.valueOf(usergroup.getParentUserGroupId()));
				if (parentUserGroupLive != null){
					attributes.put("parentid", parentUserGroupLive.getGlobalId());
				}
			
			}catch(Exception e){
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
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
				
				try{					
					UserGroup userGroup = UserGroupLocalServiceUtil.getUserGroup(GetterUtil.getLong(live.getLocalId()));
					
					try{
						UserGroupLocalServiceUtil.deleteUserGroup(userGroup.getUserGroupId());
						
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
		//String parentUserGroup = getAttribute(item, "parentid");
		
		String name = getParamTextByName(item, "name");
		String description = getParamTextByName(item, "description");
				
		try{
			long groupId = getGroupId(sGroupId);
			Group group = GroupLocalServiceUtil.getGroup(groupId);
			
			try{
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);	
			
				try {	
					
					try{
						UserGroup userGroup = UserGroupLocalServiceUtil.getUserGroup(GetterUtil.getLong(live.getLocalId()));
						
						//UPDATE						
						try{
							UserGroupLocalServiceUtil.updateUserGroup(xmlIOContext.getCompanyId(), userGroup.getUserGroupId(), name, description);
												
							//update entry in live table
							LiveLocalServiceUtil.add(_className, group.getGroupId(), globalId,
									String.valueOf(userGroup.getUserGroupId()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
												
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
						} catch (Exception ex) {
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + ex.toString(), IterKeys.ERROR, group.getName());
						}						
					}catch(Exception e){
						//CREATE						
						try{
							UserGroup userGroup = UserGroupLocalServiceUtil.addUserGroup(xmlIOContext.getUserId(), xmlIOContext.getCompanyId(), name, description);
							
							//update entry in live table
							LiveLocalServiceUtil.add(_className, group.getGroupId(), globalId,
									String.valueOf(userGroup.getUserGroupId()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
								
							try{
								//update globalId to assure that match in every server.
								LiveLocalServiceUtil.updateGlobalId(group.getGroupId(), _className, String.valueOf(userGroup.getUserGroupId()), globalId);
							}catch(Exception e2){
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicate globalId", IterKeys.ERROR, group.getName());
							}
							
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
						} catch (Exception e1) {
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e1.toString(), IterKeys.ERROR, group.getName());
						}
					}
				} catch (Exception e1) {
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e1.toString(), IterKeys.ERROR, sGroupId);
				}
			}catch (Exception e1) {			
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e1.toString(), IterKeys.ERROR, sGroupId);
			}
		} catch (Exception e) {
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}		
	}	
	
}
