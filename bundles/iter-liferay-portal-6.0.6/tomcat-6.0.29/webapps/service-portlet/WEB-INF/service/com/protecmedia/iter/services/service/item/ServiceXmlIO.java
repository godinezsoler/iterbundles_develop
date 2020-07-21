/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.services.service.item;

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
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.services.model.Service;
import com.protecmedia.iter.services.service.ServiceLocalServiceUtil;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

/* XML Service:
 * 
 */
public class ServiceXmlIO extends ItemXmlIO {
	
	private static Log _log = LogFactoryUtil.getLog(ServiceXmlIO.class);
	private String _className = IterKeys.CLASSNAME_SERVICE;
	
	public ServiceXmlIO () {		
		super();
	}
	
	public ServiceXmlIO (XMLIOContext xmlIOContext) {
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
		
		List<Service> serviceList = ServiceLocalServiceUtil.getServices(groupId);
		for (Service service : serviceList){
			try{
				createLiveEntry(service);
			}catch(Exception err){
				_log.error("Can't add Live, Service: " + service.getServiceId());
			}
		} 
		
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		Service service = (Service)model;
		
		LiveLocalServiceUtil.add(_className, service.getGroupId(), -1, 0,
				IterLocalServiceUtil.getSystemName() + "_" + service.getServiceId(), service.getServiceId(),
				IterKeys.CREATE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
	}

	@Override
	public void deleteLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		Service service = (Service)model;
		
		LiveLocalServiceUtil.add(_className, service.getGroupId(), 
				IterLocalServiceUtil.getSystemName() + "_" + service.getServiceId(), service.getServiceId(),
				IterKeys.DELETE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
		
	}

	/*
	 * Export functions
	 */	
	@Override
	protected String createItemXML(XMLIOExport xmlioExport, Element root, String operation, Group group, Live live) {
		
		String error = "";
		
		Map<String, String> attributes = new HashMap<String, String>();
		Map<String, String> params = new HashMap<String, String>();
		
		setCommonAttributes(attributes, group.getName(), live, operation);
													
		if (operation.equals(IterKeys.CREATE)){
			
			Service service = null;
			
			try{
				service = ServiceLocalServiceUtil.getServiceByServiceId(group.getGroupId(), live.getLocalId());
				Live layout = LiveLocalServiceUtil.getLiveByLocalId(group.getGroupId(), IterKeys.CLASSNAME_LAYOUT, String.valueOf(service.getLinkId()));
				
				params.put("serviceid", service.getServiceId());
				params.put("title", service.getTitle());				
				params.put("imageid", String.valueOf(service.getImageId()));
				params.put("image-file", XMLIOUtil.exportImageFile(xmlioExport, service.getImageId()));
				//TODO: Si el Layout es nulo no se exporta. Permitir que se creen servicios con link nulo.
				params.put("linkid", layout.getGlobalId());
				
			}catch(Exception e){
				//LiveLocalServiceUtil.setError(live.getId(), IterKeys.CORRUPT, "Can't export item");			
				error = "Cannot export item";
			}
		}
		
		addNode(root, "item", attributes, params);
		
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
			
			try{
				//Get live to get the element localId
				Live sLive = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
				
				try{
					Service service = ServiceLocalServiceUtil.getServiceByServiceId(groupId, sLive.getLocalId());
					
					try{
						ServiceLocalServiceUtil.deleteService(service.getId());
						
						//update entry in live table
						LiveLocalServiceUtil.add(_className, groupId, globalId,
								sLive.getLocalId(), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
						
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Done", IterKeys.DONE, sGroupId);
					}catch (Exception e1) {
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Error: "+ e1.toString(), IterKeys.ERROR, sGroupId);				
					}
				}catch (Exception e1) {
					if(sLive != null){
						//clean entry in live table
						LiveLocalServiceUtil.add(_className, groupId, globalId,
							sLive.getLocalId(), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
					}
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Element not found", IterKeys.DONE, sGroupId);
				}
			}catch (Exception e1) {
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Element not found", IterKeys.ERROR, sGroupId);				
			}
		}catch (Exception e) {
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}
	}	
	
	@Override
	protected void modify(Element item, Document doc) {
		
		String sGroupId = getAttribute(item, "groupid");		
		String globalId = getAttribute(item, "globalid");	
				
		String serviceId = getParamTextByName(item, "serviceid");
		String title = getParamTextByName(item, "title");		
		String linkId = getParamTextByName(item, "linkid");			
		//long imageId = GetterUtil.getLong(getParamTextByName(item, "imageid"));
		String imageFile = getParamTextByName(item, "image-file");

		try{
			
			long groupId = getGroupId(sGroupId);
			
			try {
				
				long imageFileId = XMLIOUtil.importImageFile (imageFile);
				Live layoutLive = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_LAYOUT, linkId);
				long layoutId = GetterUtil.getLong(layoutLive.getLocalId());
				
				try{
					Service s = ServiceLocalServiceUtil.getServiceByServiceId(groupId, serviceId);
					ServiceLocalServiceUtil.updateService(s, s.getGroupId(), title, layoutId, imageFileId);
					//update entry in live table
					LiveLocalServiceUtil.add(_className, groupId, globalId, serviceId,
							IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
					
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, sGroupId);
				}catch(Exception e){
					//Create
					ServiceLocalServiceUtil.addService(groupId, serviceId, title, layoutId, imageFileId);
					//update entry in live table
					LiveLocalServiceUtil.add(_className, groupId, globalId,
							String.valueOf(serviceId), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
					
					try{
						//Update globalId.
						LiveLocalServiceUtil.updateGlobalId(groupId, _className, serviceId, globalId);
					}catch(Exception e3){
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicated globalId", IterKeys.ERROR, sGroupId);
					}
					
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, sGroupId);
				}
			} catch (Exception e) {
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error during create process", IterKeys.ERROR, sGroupId);
			}
		} catch (Exception e) {
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "GroupId doesn't exist", IterKeys.ERROR, sGroupId);
		}
		
	}
	
}
