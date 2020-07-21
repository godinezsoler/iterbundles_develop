/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.service.item.expando;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.ClassName;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portlet.expando.model.ExpandoColumn;
import com.liferay.portlet.expando.model.ExpandoColumnConstants;
import com.liferay.portlet.expando.model.ExpandoTable;
import com.liferay.portlet.expando.model.ExpandoTableConstants;
import com.liferay.portlet.expando.model.ExpandoValue;
import com.liferay.portlet.expando.service.ExpandoColumnLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoTableLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoValueLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;

/* XML Structure:
*
* INFO: El campo name es único y no puede modificarse en los updates
* 
<item operation="create" globalid="_11605" classname="com.liferay.portlet.expando.model.ExpandoColumn" groupid="10132">
	<param name="typesettings">&lt;![CDATA[indexable=true]]&gt;</param>
	<param name="fieldclassname">&lt;![CDATA[com.liferay.portal.model.Layout]]&gt;</param>
	<param name="name">&lt;![CDATA[rtyutyui]]&gt;</param>
	<param name="type">&lt;![CDATA[custom.field.java.lang.String]]&gt;</param>
	<param name="defaultdata">&lt;![CDATA[]]&gt;</param>
</item>
*/

public class ExpandoColumnXmlIO extends ItemXmlIO {
	
	private static Log _log = LogFactoryUtil.getLog(ExpandoColumnXmlIO.class);
	private String _className = IterKeys.CLASSNAME_EXPANDOCOLUMN;
	
	public ExpandoColumnXmlIO() {
		super();
	}
	
	public ExpandoColumnXmlIO(XMLIOContext xmlIOContext) {
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
		
		//Only exists in global group
		for (String className : IterKeys.CUSTOMFIELD_CLASSNAME_TYPES){
			List<ExpandoColumn> expandoColumnList = ExpandoColumnLocalServiceUtil.getDefaultTableColumns(companyId, className);
			for (ExpandoColumn expandoColumn : expandoColumnList){				
				try {
					createLiveEntry(expandoColumn);
				} catch (PortalException e) {
					_log.error("Can't add Live, ExpandoColumn: " + expandoColumn.getColumnId());
				}
			}
		}
		
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		ExpandoColumn expandoColumn = (ExpandoColumn)model;
		
		ExpandoTable expandoTable = ExpandoTableLocalServiceUtil.getTable(expandoColumn.getTableId());

		if (ArrayUtils.contains(IterKeys.CUSTOMFIELD_CLASSNAME_TYPES, expandoTable.getClassName())){			
			long groupId = CompanyLocalServiceUtil.getCompany(expandoTable.getCompanyId()).getGroup().getGroupId();
			Live liveGroup = LiveLocalServiceUtil.getLiveByLocalId(groupId, IterKeys.CLASSNAME_GROUP, String.valueOf(groupId));
			Live liveTable = LiveLocalServiceUtil.getLiveByLocalId(groupId, IterKeys.CLASSNAME_EXPANDOTABLE, String.valueOf(expandoTable.getTableId()));

			String id = String.valueOf(expandoColumn.getColumnId());	
			
			//Add element
			LiveLocalServiceUtil.add(_className, groupId, liveGroup.getId(), liveTable.getId(),
					IterLocalServiceUtil.getSystemName() + "_" + id, id, 
					IterKeys.CREATE, IterKeys.PENDING, new Date(), 
					IterKeys.ENVIRONMENT_PREVIEW);
		}		
	}
	
	@Override
	public void deleteLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		ExpandoColumn expandoColumn = (ExpandoColumn)model;
		
		Company company = CompanyLocalServiceUtil.getCompany(expandoColumn.getCompanyId());
		
		long groupId = company.getGroup().getGroupId();
			
		Live live = LiveLocalServiceUtil.getLiveByLocalId(groupId, _className, String.valueOf(expandoColumn.getColumnId()));
		
		if (live != null){
			//Column
			LiveLocalServiceUtil.add(_className, groupId, IterLocalServiceUtil.getSystemName() + "_" + String.valueOf(expandoColumn.getColumnId()), 
					String.valueOf(expandoColumn.getColumnId()), IterKeys.DELETE, 
					IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
					
			//Column Values
			int count = ExpandoValueLocalServiceUtil.getColumnValuesCount(expandoColumn.getColumnId());
			List<ExpandoValue> values = ExpandoValueLocalServiceUtil.getColumnValues(expandoColumn.getColumnId(), 0, count);
			for (ExpandoValue value : values){
				LiveLocalServiceUtil.add(IterKeys.CLASSNAME_EXPANDOVALUE, groupId, IterLocalServiceUtil.getSystemName() + "_" + String.valueOf(value.getValueId()), 
						String.valueOf(value.getValueId()), IterKeys.DELETE, 
						IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
			}
		}
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
				
		try {
			//Only exists in global group
			String globalGroup = CompanyLocalServiceUtil.getCompany(group.getCompanyId()).getGroup().getName();
		
			setCommonAttributes(attributes, globalGroup, live, operation);
			
			if (operation.equals(IterKeys.CREATE)){
				try{
					ExpandoColumn expandoColumn = ExpandoColumnLocalServiceUtil.getColumn(GetterUtil.getLong(live.getLocalId()));
					ExpandoTable expandoTable = ExpandoTableLocalServiceUtil.getExpandoTable(expandoColumn.getTableId());
					ClassName className = ClassNameLocalServiceUtil.getClassName(expandoTable.getClassName());
				
					params.put("name", expandoColumn.getName());
					params.put("type", ExpandoColumnConstants.getTypeLabel(expandoColumn.getType()));
					params.put("defaultdata", expandoColumn.getDefaultData());
					params.put("typesettings", expandoColumn.getTypeSettings());		
					params.put("fieldclassname", className.getValue());
				}catch(Exception e){
					//_log.error("Can't export type " + live.getClassNameValue() + " Id " + live.getLocalId());
					//LiveLocalServiceUtil.setError(live.getId(), IterKeys.CORRUPT, "Can't export item");
					error = "Cannot export item";
				}
			}
						
			addNode(root, "item", attributes, params);	
			
			_log.debug("INFO: Expando Column exported");
		
		} catch (Exception e1) {
			//LiveLocalServiceUtil.setError(live.getId(), IterKeys.CORRUPT, "Can't export item");
			error = "Cannot export item";
		}
		
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
					ExpandoColumn column = ExpandoColumnLocalServiceUtil.getColumn(GetterUtil.getLong(live.getLocalId()));
					
					try{
						ExpandoColumnLocalServiceUtil.deleteColumn(column);
						
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
		
		DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		
		String sGroupId = getAttribute(item, "groupid");
		String globalId = getAttribute(item, "globalid");
	
		String fieldClassName = getParamTextByName(item, "fieldclassname");			
		String name = getParamTextByName(item, "name");	
		String type = getParamTextByName(item, "type");	
		String defaultData = getParamTextByName(item, "defaultdata");
		Object dData = defaultData;	
		String typeSettings = getParamTextByName(item, "typesettings");
		
		int typeId = 0;
		while (typeId < ExpandoColumnConstants.TYPES.length && 
				!type.equals(ExpandoColumnConstants.getTypeLabel(typeId))){
			typeId++;
		}				
				
		try{
			long groupId = getGroupId(sGroupId);
			Group group = GroupLocalServiceUtil.getGroup(groupId);
			
			try {
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
				
				try{
					ExpandoColumn ec = ExpandoColumnLocalServiceUtil.getColumn(GetterUtil.getLong(live.getLocalId()));
							
					//UPDATE
					try{
						if(defaultData.equals("")){
							ExpandoColumnLocalServiceUtil.updateColumn(ec.getColumnId(), name, typeId);
						}else{
							ExpandoColumnLocalServiceUtil.updateColumn(ec.getColumnId(), name, typeId, dData);
						}
						ExpandoColumnLocalServiceUtil.updateTypeSettings(ec.getColumnId(), typeSettings);
						
						//update entry in live table
						LiveLocalServiceUtil.add(_className, group.getGroupId(), globalId,
								String.valueOf(ec.getColumnId()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
											
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
					
					} catch (Exception e) {
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e.toString(), IterKeys.ERROR, group.getName());				
					}
				}catch (Exception e) {
					if (live == null || !live.getOperation().equals(IterKeys.DELETE)){							
						//Existe la entrada en Live pero no existe el elemento. Borramos en Live para volver realizar una inserción completa.
						if (live != null){
							LiveLocalServiceUtil.deleteLiveById(live.getId());
						}
						
						//CREATE
						try{
							ExpandoTable table = ExpandoTableLocalServiceUtil.getTable(xmlIOContext.getCompanyId(), fieldClassName, ExpandoTableConstants.DEFAULT_TABLE_NAME);
							
							ExpandoColumn column = null;
							if (defaultData.equals("")){
								column = ExpandoColumnLocalServiceUtil.addColumn(table.getTableId(), name, typeId);
							}else{
								column = ExpandoColumnLocalServiceUtil.addColumn(table.getTableId(), name, typeId, dData);
							}					
							column.setTypeSettings(typeSettings);
							
							//update entry in live table
							LiveLocalServiceUtil.add(_className, group.getGroupId(), 0, 0, globalId,
									String.valueOf(column.getColumnId()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
								
							try{
								//update globalId to assure that match in every server.
								LiveLocalServiceUtil.updateGlobalId(group.getGroupId(), _className, String.valueOf(column.getColumnId()), globalId);
							}catch(Exception e2){
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicate globalId", IterKeys.ERROR, group.getName());
							}
							
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
							
						}catch (Exception e1) {			
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e1.toString(), IterKeys.ERROR, group.getName());
						}
					}
				}
			}catch (Exception e1) {			
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e1.toString(), IterKeys.ERROR, sGroupId);
			}
		} catch (Exception e) {
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}
	}
	
}
