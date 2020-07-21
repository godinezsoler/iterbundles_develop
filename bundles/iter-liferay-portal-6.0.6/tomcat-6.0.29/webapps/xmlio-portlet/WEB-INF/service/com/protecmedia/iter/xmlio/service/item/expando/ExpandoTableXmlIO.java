/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.service.item.expando;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.ClassName;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portlet.expando.model.ExpandoTable;
import com.liferay.portlet.expando.service.ExpandoTableLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;

/* XML Structure:
*
*/

public class ExpandoTableXmlIO extends ItemXmlIO {
	
	private static Log _log = LogFactoryUtil.getLog(ExpandoTableXmlIO.class);
	private String _className = IterKeys.CLASSNAME_EXPANDOTABLE;
	
	public ExpandoTableXmlIO() {
		super();
	}
	
	public ExpandoTableXmlIO(XMLIOContext xmlIOContext) {
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
		for (String className : IterKeys.CUSTOMFIELD_CLASSNAME_TYPES){
			//Only exists in global group					
			try {
				List<ExpandoTable> expandoTableList = ExpandoTableLocalServiceUtil.getTables(companyId, className);
				for (ExpandoTable expandoTable : expandoTableList){	
					createLiveEntry(expandoTable);
				}
			} catch (PortalException e) {
				_log.error("Can't add Live, ExpandoTable, className: " + className);
			}			
		}	
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		ExpandoTable expandoTable = (ExpandoTable) model;
		
		if (ArrayUtils.contains(IterKeys.CUSTOMFIELD_CLASSNAME_TYPES, expandoTable.getClassName())){
			long groupId = CompanyLocalServiceUtil.getCompany(expandoTable.getCompanyId()).getGroup().getGroupId();
			Live liveGroup = LiveLocalServiceUtil.getLiveByLocalId(groupId, IterKeys.CLASSNAME_GROUP, String.valueOf(groupId));
			
			String id = String.valueOf(expandoTable.getTableId());	
			
			LiveLocalServiceUtil.add(_className, groupId, liveGroup.getId(), liveGroup.getId(),
					IterLocalServiceUtil.getSystemName() + "_" + id, id, 
					IterKeys.CREATE, IterKeys.PENDING, new Date(), 
					IterKeys.ENVIRONMENT_PREVIEW);
		}		
	}
	
	@Override
	public void deleteLiveEntry(BaseModel<?> model) throws PortalException, SystemException{}
	
	/*
	 * Export Functions
	 */
	@Override
	protected String createItemXML(XMLIOExport xmlioExport, Element root, String operation, Group group,
			Live live){
		
		String error = "";
			
		Map<String, String> attributes = new HashMap<String, String>();
		Map<String, String> params = new HashMap<String, String>();
		
		try {
			//Only exists in global group
			String globalGroup = CompanyLocalServiceUtil.getCompany(group.getCompanyId()).getGroup().getName();
			
			setCommonAttributes(attributes, globalGroup, live, operation);
		
		
			ExpandoTable expandoTable = ExpandoTableLocalServiceUtil.getExpandoTable(GetterUtil.getLong(live.getLocalId()));
			ClassName className = ClassNameLocalServiceUtil.getClassName(expandoTable.getClassName());
				
			params.put("fieldclassname", className.getValue());
			
			Element itemElement = addNode(root, "item", attributes, params);
			
			addDependencies(itemElement , live.getId());
		
			_log.debug("INFO: Expando Table exported");
			
		}catch(Exception e){
			//LiveLocalServiceUtil.setError(live.getId(), IterKeys.CORRUPT, "Can't export item");	
			error = "Cannot export item";
		}	
		
		return error;
	}

	/*
	 * Import Functions 
	 */
	
	@Override 
	protected void delete(Element item){
		//Los ExpandoTable NUNCA se borran
	}
	
	//Always create the element if it doesn't exists. Not depends of the live table.
	@Override
	protected void modify(Element item, Document doc) {
		
		String sGroupId = getAttribute(item, "groupid");
		String globalId = getAttribute(item, "globalid");
	
		String fieldClassName = getParamTextByName(item, "fieldclassname");			
		
		try{
			long groupId = getGroupId(sGroupId);
			
			try{								
				ExpandoTableLocalServiceUtil.getDefaultTable(xmlIOContext.getCompanyId(), fieldClassName);
					
				try {
					//Creamos/modificamos sus dependencias	
					if (! evaluateDependencies(item, doc)){
						LiveLocalServiceUtil.updateStatus(groupId, _className, globalId, IterKeys.INTERRUPT);
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, sGroupId);				
					}else{									
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, sGroupId);
					}
				} catch (DocumentException err) {
					LiveLocalServiceUtil.updateStatus(groupId, _className, globalId, IterKeys.INTERRUPT);
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, sGroupId);				
				}
			} catch (Exception e) {
				try{
					ExpandoTable et = ExpandoTableLocalServiceUtil.addDefaultTable(xmlIOContext.getCompanyId(), fieldClassName);
					
					//update entry in live table
					LiveLocalServiceUtil.add(_className, groupId, 0, 0, globalId,
							String.valueOf(et.getTableId()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
					
					try{
						//update globalId to assure that match in every server.
						LiveLocalServiceUtil.updateGlobalId(groupId, _className, String.valueOf(et.getTableId()), globalId);
					}catch(Exception e2){
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicate globalId", IterKeys.ERROR, sGroupId);
					}
					
					try {
						//Creamos/modificamos sus dependencias	
						if (! evaluateDependencies(item, doc)){
							LiveLocalServiceUtil.updateStatus(groupId, _className, globalId, IterKeys.INTERRUPT);
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, sGroupId);				
						}else{									
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, sGroupId);
						}
					} catch (DocumentException err) {
						LiveLocalServiceUtil.updateStatus(groupId, _className, globalId, IterKeys.INTERRUPT);
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, sGroupId);				
					}
					
				}catch(Exception e1){
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e.toString(), IterKeys.ERROR, sGroupId);
				}
			}
			
		} catch (Exception e) {
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}
	}	
	
}
