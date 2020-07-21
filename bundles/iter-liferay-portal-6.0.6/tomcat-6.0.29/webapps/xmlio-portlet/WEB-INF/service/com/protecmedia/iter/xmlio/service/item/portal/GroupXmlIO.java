/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.service.item.portal;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.IterGlobalKeys;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.persistence.GroupUtil;
import com.liferay.portlet.asset.model.AssetEntry;
import com.liferay.portlet.asset.service.AssetEntryLocalServiceUtil;
import com.liferay.portlet.asset.service.persistence.AssetEntryUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LivePoolLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.asset.AssetEntryXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;

/*
*/
public class GroupXmlIO extends ItemXmlIO {
	
	private static Log _log = LogFactoryUtil.getLog(GroupXmlIO.class);
	private String _className = IterKeys.CLASSNAME_GROUP;
	
	public GroupXmlIO () {		
		super();
	}
	
	public GroupXmlIO (XMLIOContext xmlIOContext) {		
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

		List<Group> groups = GroupUtil.findByCompanyId(companyId);
		for (Group group : groups){		
			if (group.getClassName().equals(IterKeys.CLASSNAME_GROUP) || group.getClassName().equals(IterKeys.CLASSNAME_COMPANY)){				
				try {
					//1. Populate Group's AssetEntry
					AssetEntry assetEntry = AssetEntryUtil.fetchByC_C(ClassNameLocalServiceUtil.getClassNameId(IterKeys.CLASSNAME_GROUP), group.getPrimaryKey());
					if (assetEntry != null){	
						try {
							ItemXmlIO entryItem = new AssetEntryXmlIO();
							entryItem.createLiveEntry(assetEntry);
						} catch (PortalException e) {
							_log.error("Can't add Live, AssetEntry: " + assetEntry.getEntryId());
						}	
					}	
					
					//2. Populate Group
					createLiveEntry(group);			
					
				} catch (PortalException e) {
					_log.error("Can't add Live, Group: " + group.getName());
				}
			}			
		}
		
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		Group group = (Group)model;
		
		String id = String.valueOf(group.getGroupId());
			
		/*
		 * Hay elementos dependientes de Group que se crean antes que él, 
		 * por lo que hay que establecer las dependencias aqui. 		
		 */	
		//1. Añado Group
		Live liveGroup = LiveLocalServiceUtil.add(_className, group.getGroupId(), -1, 0, 
				group.getName(), id, IterKeys.CREATE, IterKeys.PENDING, new Date(), 
				IterKeys.ENVIRONMENT_PREVIEW);		
		
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW))
		{
			liveGroup = updateGlobalId(liveGroup, group.getName());
			
			try
			{
				// 2. Actualizo el pool en el AssetEntry del Group
				AssetEntry assetEntry 	= AssetEntryLocalServiceUtil.getEntry(IterKeys.CLASSNAME_GROUP, group.getPrimaryKey());
				Live liveEntry 			= LiveLocalServiceUtil.getLiveByLocalId(assetEntry.getGroupId(), IterKeys.CLASSNAME_ENTRY, String.valueOf(assetEntry.getEntryId()));
				LivePoolLocalServiceUtil.createLivePool(liveGroup.getId(), liveGroup.getId(), liveEntry.getId(), false);
			}
			catch(Exception err)
			{
				_log.debug("AssetEntry data cannot be added as dependency of Group " + liveGroup.getGroupId());
			}
		}
	}
	
	/**
	 *  Es necesario actualizar el globalId con el nuevo combre del grupo, si dicho grupo aún no se ha publicado
     *
	 * @param liveGroup
	 * @param newGlobalId
	 * @see ITER-1041 Error "No Group exists with the key" al intentar publicar un sitio importado desde un mln
	 * @see http://jira.protecmedia.com:8080/browse/ITER-1041?focusedCommentId=42821&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-42821 <br/>
	 * @return
	 */
	private Live updateGlobalId(Live liveGroup, String newGlobalId)
	{
		Live result = liveGroup;
		try
		{
			// Si no se ha publicado, se actualiza el globalId con el nuevo con el nuevo nombre
			if (PropsValues.IS_PREVIEW_ENVIRONMENT && 
				!GetterUtil.getString(liveGroup.getExistInLive(), "N").equalsIgnoreCase("S"))
			{
				liveGroup.setGlobalId(newGlobalId);
				result = LiveLocalServiceUtil.updateLive(liveGroup, false);
			}
		}
		catch (Exception e)
		{
			_log.error(e);
		}
		return result;
	}
	
	@Override
	public void deleteLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		Group group = (Group)model;
		
		String id = String.valueOf(group.getGroupId());
		
		LiveLocalServiceUtil.add(_className, group.getGroupId(), group.getName(), 
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
		
		if (operation.equals(IterKeys.CREATE))
		{				
			try 
			{				
				Group impGroup = GroupLocalServiceUtil.getGroup(GetterUtil.getLong(live.getLocalId()));
			
				attributes.put("parentid", String.valueOf(impGroup.getParentGroupId()));
				
				String grpName = (impGroup.getDelegationId() > 0) 																			? 
									String.format("%d%s%s", impGroup.getDelegationId(), IterGlobalKeys.DLG_SEPARATOR, impGroup.getName())	:
									impGroup.getName();
									
				params.put("name", grpName);
				params.put("description", impGroup.getDescription());
				params.put("url", impGroup.getFriendlyURL());
				params.put("type", String.valueOf(impGroup.getType()));
				params.put("active", String.valueOf(impGroup.getActive()));				
				if (! impGroup.getName().equals(IterKeys.GLOBAL_GROUP_NAME))
				{
					params.put("theme", impGroup.getPublicLayoutSet().getThemeId());
				}
				
				addDependencies(addNode(root, "item", attributes, params), live.getId());	
				
			}
			catch(Exception e)
			{
				//LiveLocalServiceUtil.setError(live.getId(), IterKeys.CORRUPT, "Can't export item");			
				error = "Cannot export item";
			}
		}
		else if (operation.equals(IterKeys.UPDATE))
		{	
			addDependencies(addNode(root, "item", attributes, params), live.getId());	
		}
		else
		{
			addNode(root, "item", attributes, params);	
		}
		
		_log.debug("XmlItem OK");
											
		return error;
	}		
	
	/*
	 * Import Functions
	 */	
	
	@Override
	protected void delete(Element item) {
		
		String globalId = getAttribute(item, "globalid");
		
		try{					
			long groupId = getGroupId(globalId);
			
			try{				
				GroupLocalServiceUtil.deleteGroup(groupId);
						
				//update entry in live table
				LiveLocalServiceUtil.add(_className, groupId, globalId,
						String.valueOf(groupId), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
										
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Done", IterKeys.DONE, globalId);
			} catch (Exception e) {
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Error: " + e.toString(), IterKeys.ERROR, globalId);				
			}
		} catch (Exception e) {
			//TODO: can't clean entry in live table		
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Element not found", IterKeys.DONE, globalId);				
		}	
	}
	
	@Override
	public void modify(Element item, Document doc) {		
		
		String globalId 	= getAttribute(item, "globalid");	
		String name 		= getParamTextByName(item, "name");
		String description 	= getParamTextByName(item, "description");			
		String url 			= getParamTextByName(item, "url");			
		String theme 		= getParamTextByName(item, "theme");			
		String colorScheme 	= getParamTextByName(item, "color-scheme");	
		int type 			= GetterUtil.getInteger(getParamTextByName(item, "type"), 0);	
		boolean active 		= GetterUtil.getBoolean(getParamTextByName(item, "active"), true);	
		
		ServiceContext serviceContext = new ServiceContext();					
		serviceContext.setAddCommunityPermissions(true);
		serviceContext.setAddGuestPermissions(true);
		
		Group group = null;
		
		try
		{
			long groupId = getGroupId(globalId);
			group = GroupLocalServiceUtil.getGroup(groupId);
			
			// Si se trata del grupo global no se crea ni se modifica.		
			if (!group.getFriendlyURL().equals("/null"))
			{
				//--------UPDATE
				GroupLocalServiceUtil.updateGroup(group.getGroupId(), name, description, type, url, active, serviceContext);
				
				// Layout set				
				try
				{
					LayoutSetLocalServiceUtil.updateLookAndFeel(group.getGroupId(), false, theme, colorScheme, "", false);
				}
				catch(Exception e)
				{
					_log.debug("Error updating look and feel");
				}
			}
			
			//update entry in live table
			LiveLocalServiceUtil.add(_className, group.getGroupId(), globalId,
					String.valueOf(group.getGroupId()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
								
			try 
			{
				//Creamos/modificamos sus dependencias	
				if (! evaluateDependencies(item, doc))
				{
					LiveLocalServiceUtil.updateStatus(group.getGroupId(), _className, globalId, IterKeys.INTERRUPT);
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, group.getName());				
				}
				else
				{									
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
				}
			} 
			catch (DocumentException err) 
			{
				LiveLocalServiceUtil.updateStatus(group.getGroupId(), _className, globalId, IterKeys.INTERRUPT);
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, group.getName());				
			}
		}
		catch(Exception e)
		{
			//---------CREATE
			try 
			{
				group = GroupLocalServiceUtil.addGroup(xmlIOContext.getUserId(), _className, CounterLocalServiceUtil.increment(), name, description, type, url, active, serviceContext);
								
				// Layout set				
				try
				{
					LayoutSetLocalServiceUtil.updateLookAndFeel(group.getGroupId(), false, theme, colorScheme, "", false);
				}
				catch(Exception e1)
				{
					_log.debug("Error updating look and feel");
				}
				
				//update entry in live table
				LiveLocalServiceUtil.add(_className, group.getGroupId(), globalId,
						String.valueOf(group.getGroupId()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
					
				try
				{
					//update globalId to assure that match in every server.
					LiveLocalServiceUtil.updateGlobalId(group.getGroupId(), _className, String.valueOf(group.getGroupId()), globalId);
				}
				catch(Exception e2)
				{
					_log.error(e2);
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicate globalId", IterKeys.ERROR, group.getName());
				}
				
				try 
				{
					//Creamos/modificamos sus dependencias	
					if (! evaluateDependencies(item, doc))
					{
						LiveLocalServiceUtil.updateStatus(group.getGroupId(), _className, globalId, IterKeys.INTERRUPT);
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, group.getName());				
					}
					else
					{									
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
					}
				} 
				catch (DocumentException err) 
				{
					_log.error(err);
					LiveLocalServiceUtil.updateStatus(group.getGroupId(), _className, globalId, IterKeys.INTERRUPT);
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, group.getName());				
				}
				
			} 
			catch (Exception e1) 
			{
				_log.error(e1);
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e1.toString(), IterKeys.ERROR, group.getName());
			}
		}
		finally
		{
			if(group != null)
				xmlIOContext.setGroupId(group.getGroupId());
		}
	}
	
}
