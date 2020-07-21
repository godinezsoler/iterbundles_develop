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
import com.liferay.portal.model.User;
import com.liferay.portal.model.Website;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.service.WebsiteLocalServiceUtil;
import com.liferay.portal.service.persistence.WebsiteUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;

/*  
 * Website (part of user contact info)
 */

public class WebsiteXmlIO extends ItemXmlIO {
	
	private static Log _log = LogFactoryUtil.getLog(WebsiteXmlIO.class);
	private String _className = IterKeys.CLASSNAME_WEBSITE;

	public WebsiteXmlIO () {		
		super();
	}
	
	public WebsiteXmlIO (XMLIOContext xmlIOContext) {		
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
		
		//Only exists in global group		
		List<Website> websites = WebsiteUtil.findByC_C(companyId, ClassNameLocalServiceUtil.getClassNameId(IterKeys.CLASSNAME_CONTACT));
		for (Website website : websites){				
			createLiveEntry(website);
		}
		
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		Website website = (Website) model;
		
		String id = String.valueOf(website.getWebsiteId());
		long userId = website.getUserId();
		long groupId = CompanyLocalServiceUtil.getCompany(website.getCompanyId()).getGroup().getGroupId();
		Live liveUser = LiveLocalServiceUtil.getLiveByLocalId(groupId, IterKeys.CLASSNAME_USER, String.valueOf(userId));

		LiveLocalServiceUtil.add(_className, groupId, liveUser.getId(), liveUser.getId(),
				IterLocalServiceUtil.getSystemName() + "_" + id, id, 
				IterKeys.CREATE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
		
	}
	@Override
	public void deleteLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		Website website = (Website) model;
		
		String id = String.valueOf(website.getWebsiteId());
		long groupId = CompanyLocalServiceUtil.getCompany(website.getCompanyId()).getGroup().getGroupId();
		
		LiveLocalServiceUtil.add(_className, groupId,
				IterLocalServiceUtil.getSystemName() + "_" + id, id, 
				IterKeys.DELETE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
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
		if (!operation.equals(IterKeys.DELETE)){			
			try {
				Website website = WebsiteLocalServiceUtil.getWebsite(GetterUtil.getLong(live.getLocalId()));
				Live liveUser = LiveLocalServiceUtil.getLiveByLocalId(group.getGroupId(), IterKeys.CLASSNAME_USER, String.valueOf(website.getUserId()));
				
				params.put("url", website.getUrl());
				params.put("primary", String.valueOf(website.getPrimary()));
				params.put("typeid", String.valueOf(website.getTypeId()));
				params.put("user", liveUser.getGlobalId());
				
			}catch(Exception e){
				//LiveLocalServiceUtil.setError(live.getId(), IterKeys.CORRUPT, "Can't export item");			
				error = "Cannot export item";
			}
		}
		
		addNode(root, "item", attributes, params);	
		
		_log.debug("XmlItem OK");
				
		return error;
		/*Parametros pendientes:
		 * 	
			  `userId` bigint(20) DEFAULT NULL,
			  `userName` varchar(75) DEFAULT NULL,
			  `createDate` datetime DEFAULT NULL,
			  `modifiedDate` datetime DEFAULT NULL,
			  `classNameId` bigint(20) DEFAULT NULL,
			  `classPK` bigint(20) DEFAULT NULL,
			  `typeId` int(11) DEFAULT NULL,			 
		 */
		
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
					Website website = WebsiteLocalServiceUtil.getWebsite(GetterUtil.getLong(live.getLocalId()));
					
					try{
						WebsiteLocalServiceUtil.deleteWebsite(website.getWebsiteId());
						
						//update entry in live table
						LiveLocalServiceUtil.add(_className, groupId, globalId,
								live.getLocalId(), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
										
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Done", IterKeys.DONE, sGroupId);
					} catch (Exception e) {
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Error: " + e.toString(), IterKeys.ERROR, sGroupId);				
					}
				} catch (Exception e) {
					//Check if is not a DELETE/ERROR
					if (! live.getStatus().equals(IterKeys.ERROR)){
						//clean entry in live table
						LiveLocalServiceUtil.add(_className, groupId, globalId,
								live.getLocalId(), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
					}
					
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Element not found", IterKeys.ERROR, sGroupId);				
				}
			} catch (Exception e) {
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Element not found", IterKeys.ERROR, sGroupId);				
			}
			
		} catch (Exception e) {
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}
	}
	
	@Override
	protected void modify(Element item, Document doc) 
	{		
		String sGroupId = getAttribute(item, "groupid");
		String globalId = getAttribute(item, "globalid");	
			
		String url = getParamTextByName(item, "url");
		boolean primary = GetterUtil.getBoolean(getParamTextByName(item, "primary"), false);
		int typeId = GetterUtil.getInteger(getParamTextByName(item, "typeid"));
		String userS = getParamTextByName(item, "user");
						
		try
		{
			Group group = GroupLocalServiceUtil.getGroup(xmlIOContext.getCompanyId(), sGroupId);
			long groupId = group.getGroupId();
			
			try
			{
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);	
			
				try
				{
					Website website = WebsiteLocalServiceUtil.getWebsite(GetterUtil.getLong(live.getLocalId()));
					
					//typeId  ---> listtype buscar
					//UPDATE
					update(item, group, globalId, website.getWebsiteId(), url, typeId, primary);
												
				}
				catch(Exception e)
				{
					//CREATE
					create(item, group, globalId, userS, url, typeId, primary);
				}
			} 
			catch (Exception e1) 
			{
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e1.toString(), IterKeys.ERROR, sGroupId);
			}			
		} 
		catch (Exception e) 
		{
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}	
	}
	
	/**
	 * 
	 * @param group
	 * @param globalId
	 * @param userS
	 * @param url
	 * @param typeId
	 * @param primary
	 */
	private void create(Element item, Group group, String globalId, String userS, String url, int typeId, boolean primary) 
	{
		try
		{	
			Live liveUser = LiveLocalServiceUtil.getLiveByGlobalId(group.getGroupId(), IterKeys.CLASSNAME_USER, userS);
			long userId = GetterUtil.getLong(liveUser.getLocalId());
			
			User user = UserLocalServiceUtil.getUser(userId);					
			
			Website website = WebsiteLocalServiceUtil.addWebsite(userId, IterKeys.CLASSNAME_CONTACT, user.getContactId(), url, typeId, primary);
			
			//update entry in live table
			LiveLocalServiceUtil.add(_className, group.getGroupId(), globalId,
					String.valueOf(website.getWebsiteId()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
				
			try
			{
				//update globalId to assure that match in every server.
				LiveLocalServiceUtil.updateGlobalId(group.getGroupId(), _className, String.valueOf(website.getWebsiteId()), globalId);
			}
			catch(Exception e2)
			{
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicate globalId", IterKeys.ERROR, group.getName());
			}
			
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
		} 
		catch (Exception e1) 
		{
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e1.toString(), IterKeys.ERROR, group.getName());
		}
	}
	
	
	/**
	 * 
	 * @param group
	 * @param globalId
	 * @param websiteId
	 * @param url
	 * @param typeId
	 * @param primary
	 */
	private void update(Element item, Group group, String globalId, long websiteId, String url, int typeId, boolean primary) 
	{
		try
		{
			WebsiteLocalServiceUtil.updateWebsite(websiteId, url, typeId, primary);
									
			//update entry in live table
			LiveLocalServiceUtil.add(_className, group.getGroupId(), globalId,
					String.valueOf(websiteId), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
								
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
		} 
		catch (Exception ex) 
		{
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + ex.toString(), IterKeys.ERROR, group.getName());
		}
	}	

}
