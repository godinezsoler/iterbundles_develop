/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.news.service.item;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.news.model.Qualification;
import com.protecmedia.iter.news.service.QualificationLocalServiceUtil;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;


/*
	<item operation="delete" globalid="qual_10387" classname="com.protecmedia.iter.news.model.Qualification" groupid="Guest">
			<param name="qualificationid">&lt;![CDATA[qual_10387]]&gt;</param>
			<param name="name">&lt;![CDATA[calificación test 1]]&gt;</param>
		</item>
*/

public class QualificationXmlIO extends ItemXmlIO {
	
	private static Log _log = LogFactoryUtil.getLog(QualificationXmlIO.class);
	private String _className = IterKeys.CLASSNAME_QUALIFICATION;
	
	public QualificationXmlIO() {
		super();
	}
	
	public QualificationXmlIO(XMLIOContext xmlIOContext) {
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
		
		List<Qualification> qualificationList = QualificationLocalServiceUtil.getQualifications(groupId);
		for (Qualification qualification : qualificationList){			
			try {
				createLiveEntry(qualification);
			} catch (PortalException e) {
				_log.error("Can't add Live, Qualification: " + qualification.getQualifId());
			}
		}
		
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		Qualification qualification = (Qualification)model;
		
		Live liveGroup = LiveLocalServiceUtil.getLiveByLocalId(qualification.getGroupId(), IterKeys.CLASSNAME_GROUP, String.valueOf(qualification.getGroupId()));
		
		LiveLocalServiceUtil.add(_className, qualification.getGroupId(), liveGroup.getId(), liveGroup.getId(), 
				IterLocalServiceUtil.getSystemName() + "_" + qualification.getQualifId(), qualification.getQualifId(),
				IterKeys.CREATE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
		
	}

	@Override
	public void deleteLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		Qualification qualification = (Qualification)model;
		
		LiveLocalServiceUtil.add(_className, qualification.getGroupId(), 
				IterLocalServiceUtil.getSystemName() + "_" + qualification.getQualifId(), qualification.getQualifId(),
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
													
		//si la operacion NO es de borrado
		if (operation.equals(IterKeys.CREATE)){
			Qualification qualification = null;
			try {
				qualification = QualificationLocalServiceUtil.getQualificationByQualifId(group.getGroupId(), live.getLocalId());
				params.put("name", qualification.getName());
				params.put("qualificationid", qualification.getQualifId());
			} catch (SystemException e) {
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
					Qualification c = QualificationLocalServiceUtil.getQualificationByQualifId(groupId, live.getLocalId());
					
					if (c != null) {	
						try{
							QualificationLocalServiceUtil.deleteQualification(c.getId(), false);
							
							//update entry in live table
							LiveLocalServiceUtil.add(_className, groupId, globalId,
									live.getLocalId(), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
													
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Done", IterKeys.DONE, sGroupId);
						} catch (Exception e) {		
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Error: " + e.toString(), IterKeys.ERROR, sGroupId);
						}
					} else {
						if(live != null){
							//clean entry in live table
							LiveLocalServiceUtil.add(_className, groupId, globalId,
								live.getLocalId(), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
						}
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Element not found", IterKeys.DONE, sGroupId);
					}				
				} catch (Exception e) {	
					//Check if is not a DELETE/ERROR
					if (! live.getStatus().equals(IterKeys.ERROR)){
						//clean entry in live table
						LiveLocalServiceUtil.add(_className, groupId, globalId,
								live.getLocalId(), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
					}	
					
					//xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Element not found", IterKeys.WARN, groupId);
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
		
		String name = getParamTextByName(item, "name");		
		String qualificationId = getParamTextByName(item, "qualificationid");	
		
		try{
			long groupId = getGroupId(sGroupId);
			Group group = GroupLocalServiceUtil.getGroup(groupId);
			
			try{
				//Get live to get the element localId
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
				
				try {		
					Qualification c = QualificationLocalServiceUtil.getQualificationByQualifId(groupId, live.getLocalId());
					
					//UPDATE
					if (c != null) {
						try{
							if (!name.equals(c.getName())){
								QualificationLocalServiceUtil.updateQualification(c.getId(), name, false);					
							}
							
							//update entry in live table
							LiveLocalServiceUtil.add(_className, group.getGroupId(), globalId,
									qualificationId, IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
							
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
						} catch (Exception e) {		
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e.toString(), IterKeys.ERROR, group.getName());
						}
					} else {
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Element not found", IterKeys.ERROR, group.getName());
					}	
				} catch (Exception e) {		
					if (live == null || !live.getOperation().equals(IterKeys.DELETE)){	
						//Existe la entrada en Live pero no existe el elemento. Borramos en Live para volver realizar una inserción completa.
						if (live != null){
							LiveLocalServiceUtil.deleteLiveById(live.getId());
						}
						
						//CREATE
						try{
							
							Qualification qua = QualificationLocalServiceUtil.addQualification(group.getGroupId(), name, qualificationId, false );
							
							//update entry in live table
							LiveLocalServiceUtil.add(_className, group.getGroupId(), globalId,
									qua.getQualifId(), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
							
							try{
								//Update globalId.
								LiveLocalServiceUtil.updateGlobalId(group.getGroupId(), _className, qualificationId, globalId);
							}catch(Exception e3){
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicated globalId", IterKeys.ERROR, group.getName());
							}
							
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
						
						} catch (Exception e1) {		
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e1.toString(), IterKeys.ERROR, group.getName());
						}
					}
				}
			} catch (Exception e1) {		
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e1.toString(), IterKeys.ERROR, sGroupId);
			}
		} catch (Exception e) {
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}
	}
	

}
