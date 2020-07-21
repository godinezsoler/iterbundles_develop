/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.service.item.expando;
import java.util.Date;
import java.util.HashMap;
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
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.User;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portlet.expando.model.ExpandoRow;
import com.liferay.portlet.expando.model.ExpandoTable;
import com.liferay.portlet.expando.model.ExpandoTableConstants;
import com.liferay.portlet.expando.model.ExpandoValue;
import com.liferay.portlet.expando.service.ExpandoRowLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoTableLocalServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;

/* XML example:
 *
<item operation="create" globalid="DiariTerrassaPre_316705" classname="com.liferay.portlet.expando.model.ExpandoRow" groupid="Global">
	<param name="folder">&lt;![CDATA[0]]&gt;</param>
	<param name="title">&lt;![CDATA[Lighthouse.jpg]]&gt;</param>
	<param name="elemclassname">&lt;![CDATA[com.liferay.portlet.documentlibrary.model.DLFileEntry]]&gt;</param>
	<param name="elemgroupid">&lt;![CDATA[The Star]]&gt;</param>
	<param name="version">&lt;![CDATA[1.0]]&gt;</param>
	<param name="DiariTerrassaPre_316706" type="dependency">
		<param name="classname">&lt;![CDATA[com.liferay.portlet.expando.model.ExpandoValue]]&gt;</param>
		<param name="groupname">&lt;![CDATA[Global]]&gt;</param>
	</param>
</item>
*/

public class ExpandoRowXmlIO extends ItemXmlIO {
	
	private static Log _log = LogFactoryUtil.getLog(ExpandoRowXmlIO.class);
	private String _className = IterKeys.CLASSNAME_EXPANDOROW;
	
	public ExpandoRowXmlIO() {
		super();
	}
	
	public ExpandoRowXmlIO(XMLIOContext xmlIOContext) {
		super(xmlIOContext);
	}
	
	@Override
	public String getClassName(){
		return _className;
	}
	
	/*
	 * Live Functions
	 */
	/**
	 * @see ExpandoTableXmlIO
	 */
	@Override
	public void populateLive(long groupId, long companyId)
			throws SystemException, PortalException {
		//Live is populated in the container element
		
	}
	
	@Override 
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		//Created in ExpandoValueXmlIO.jave
	}
	
	@Override
	public void updateStatusLiveEntry(BaseModel<?> model, String status) throws PortalException, SystemException
	{
		ExpandoRow expandoRow 	= (ExpandoRow)model;
		ExpandoTable table 		= ExpandoTableLocalServiceUtil.getTable(expandoRow.getTableId());
		
		if (ArrayUtils.contains(IterKeys.CUSTOMFIELD_CLASSNAME_TYPES, table.getClassName()))
		{
			Company company = CompanyLocalServiceUtil.getCompany(expandoRow.getCompanyId());
			long groupId 	= company.getGroup().getGroupId();
			String globalId = IterLocalServiceUtil.getSystemName() + "_" + String.valueOf(expandoRow.getRowId());
			
			LiveLocalServiceUtil.updateStatus(groupId, _className, globalId, status);
		}
	}

	@Override
	public void deleteLiveEntry(BaseModel<?> model, boolean deleteJustNow) throws PortalException, SystemException
	{
		ExpandoRow expandoRow 	= (ExpandoRow)model;
		ExpandoTable table 		= ExpandoTableLocalServiceUtil.getTable(expandoRow.getTableId());
		
		if (ArrayUtils.contains(IterKeys.CUSTOMFIELD_CLASSNAME_TYPES, table.getClassName()))
		{
			Company company = CompanyLocalServiceUtil.getCompany(expandoRow.getCompanyId());
			long groupId	= company.getGroup().getGroupId();
			String globalId = IterLocalServiceUtil.getSystemName() + "_" + String.valueOf(expandoRow.getRowId());
			
			_log.trace( getTraceDeleteLiveEntry(globalId, deleteJustNow) );
			
			if (deleteJustNow)
			{
				LiveLocalServiceUtil.deleteLive(groupId, _className, globalId);
			}
			else
			{
				LiveLocalServiceUtil.add(_className, groupId, 
					globalId, String.valueOf(expandoRow.getRowId()), 
					IterKeys.DELETE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
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
				ExpandoRow row = ExpandoRowLocalServiceUtil.getExpandoRow(GetterUtil.getLong(live.getLocalId()));
				ExpandoTable table = ExpandoTableLocalServiceUtil.getTable(row.getTableId());				
							
				params.put("elemclassname", table.getClassName());	
				
				Element itemElement = exportContentsByClassName(root, row, table.getClassName(), attributes, params, live.getId());
			
				if(itemElement == null){
					//_log.error("Can't export Expando Row");
					//LiveLocalServiceUtil.setError(live.getId(), IterKeys.CORRUPT, "Can't export item");
					error = "Cannot export item";
				}else{
					addDependencies(itemElement , live.getId());
				}
			}else if(operation.equals(IterKeys.UPDATE)){	
				addDependencies(addNode(root, "item", attributes, params) , live.getId());
			}else{
				addNode(root, "item", attributes, params);
			}
				
			_log.debug("INFO: Expando Row exported");
				
		} catch (Exception e1) {			
			//LiveLocalServiceUtil.setError(live.getId(), IterKeys.CORRUPT, "Can't export item");
			error = "Cannot export item";
		}	
		
		return error;
	}
	
	/**
	 * 
	 * @param root
	 * @param rowId
	 * @param className
	 * @param attributes
	 * @param params
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	private Element exportContentsByClassName(Element root, ExpandoRow row, String className, Map<String, String> attributes, Map<String, String> params, long liveId) throws PortalException, SystemException{
		
		Element element = null;
		
		Map<String, String> paramsVal = new HashMap<String, String>();
		paramsVal.putAll(params);
							
		if (className.equals(IterKeys.CLASSNAME_LAYOUT)){
				
			Layout layout = LayoutLocalServiceUtil.getLayout(row.getClassPK());					
			Live live = LiveLocalServiceUtil.getLiveByLocalId(layout.getGroupId(), IterKeys.CLASSNAME_LAYOUT, String.valueOf(layout.getPlid()));
			Group group = GroupLocalServiceUtil.getGroup(layout.getGroupId());
			
			paramsVal.put("elemglobalid", live.getGlobalId());	
			paramsVal.put("elemgroupid", group.getName());	
			
			element = addNode(root, "item", attributes, paramsVal);
					
		/*}else if (className.equals(IterKeys.CLASSNAME_DLFILEENTRY)){
				
			DLFileVersion dlFileVersion = DLFileVersionLocalServiceUtil.getFileVersion(row.getClassPK());
	
			DLFileEntry dLFileEntry = DLFileEntryLocalServiceUtil.getFileEntry(dlFileVersion.getGroupId(), dlFileVersion.getFolderId(), dlFileVersion.getName());
			DLFileVersion lastDlFileVersion = dLFileEntry.getLatestFileVersion();
			
			//Only import if it's the last version. 
			if (lastDlFileVersion.getFileVersionId() == row.getClassPK()){
				Group group = GroupLocalServiceUtil.getGroup(dlFileVersion.getGroupId());
				
				String folder = "0";
				if (dlFileVersion.getFolderId()!= 0){
					folder = LiveLocalServiceUtil.getLiveByLocalId(dlFileVersion.getGroupId(), IterKeys.CLASSNAME_DLFOLDER, String.valueOf(dlFileVersion.getFolderId())).getGlobalId();
				}
				
				paramsVal.put("elemgroupid", group.getName());	
				paramsVal.put("version", dlFileVersion.getVersion());	
				paramsVal.put("title", dlFileVersion.getTitle());	
				paramsVal.put("folder", folder);						
				
				element =  addNode(root, "item", attributes, paramsVal);
				
			}else{
				//Si no es la última versión borramos la entrada de la tabla live
				long globalGroupId = getGroupId(IterKeys.GLOBAL_GROUP_NAME); 
				
				Live live = LiveLocalServiceUtil.getLiveByLocalId(globalGroupId, _className, String.valueOf(row.getRowId()));
				LiveLocalServiceUtil.deleteLive(live);
			}
		*/
		}else if (className.equals(IterKeys.CLASSNAME_USER)){
			
			User user = UserLocalServiceUtil.getUserById(row.getClassPK());
				
			paramsVal.put("screenname", user.getScreenName());	
				
			element =  addNode(root, "item", attributes, paramsVal);
		
		}else if (className.equals(IterKeys.CLASSNAME_JOURNALARTICLE)){
			
			JournalArticle journalArticle = JournalArticleLocalServiceUtil.getArticle(row.getClassPK());
			Live live = LiveLocalServiceUtil.getLiveByLocalId(journalArticle.getGroupId(), IterKeys.CLASSNAME_JOURNALARTICLE, String.valueOf(journalArticle.getArticleId()));
			Group group = GroupLocalServiceUtil.getGroup(journalArticle.getGroupId());
			
			paramsVal.put("elemglobalid", live.getGlobalId());	
			paramsVal.put("elemgroupid", group.getName());	
			
			element =  addNode(root, "item", attributes, paramsVal);
			
		}	
		
		return element;
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
					ExpandoRow row = ExpandoRowLocalServiceUtil.getExpandoRow(GetterUtil.getLong(live.getLocalId()));
					
					try{
						ExpandoRowLocalServiceUtil.deleteRow(row.getTableId(), row.getClassPK());
						
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
			
		String elemClassName = getParamTextByName(item, "elemclassname");	
		
		
		try{
			long groupId = getGroupId(sGroupId);
			
			try {
				long classNameId = ClassNameLocalServiceUtil.getClassNameId(elemClassName);
								
				ExpandoTable table = ExpandoTableLocalServiceUtil.getTable(xmlIOContext.getCompanyId(), classNameId, ExpandoTableConstants.DEFAULT_TABLE_NAME);
				long tableId = table.getTableId();	
				
				long classPK = getClassPK(item, elemClassName);
				
				if (classPK != -1){						
					try {		
						ExpandoRowLocalServiceUtil.getRow(tableId, classPK);
						
						try {
							//Creamos/modificamos sus dependencias	
							if (! evaluateDependencies(item, doc)){
								LiveLocalServiceUtil.updateStatus(groupId, _className, globalId, IterKeys.INTERRUPT);
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, sGroupId);				
							}else{									
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicated element", IterKeys.DONE, sGroupId);
							}
						} catch (DocumentException err) {
							LiveLocalServiceUtil.updateStatus(groupId, _className, globalId, IterKeys.INTERRUPT);
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, sGroupId);				
						}	
						
					} catch (Exception e) {
						ExpandoRow row = ExpandoRowLocalServiceUtil.addRow(tableId, classPK);
						
						//update entry in live table
						LiveLocalServiceUtil.add(_className, groupId, 0, 0, globalId,
								String.valueOf(row.getRowId()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
						
						try{
							//update globalId to assure that match in every server.
							LiveLocalServiceUtil.updateGlobalId(groupId, _className, String.valueOf(row.getRowId()), globalId);
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
					}
				}else{
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "ClassPK not found", IterKeys.ERROR, sGroupId);
				}
			} catch (Exception e) {
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e.toString(), IterKeys.ERROR, sGroupId);
			}
		} catch (Exception e) {
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}
	}

	/**
	 * 
	 * @param item
	 * @param className
	 * @return
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	private long getClassPK(Element item, String className) throws PortalException, SystemException {
		
		long classPK = -1;
				
		if (className.equals(IterKeys.CLASSNAME_LAYOUT)){	//Page			
					
			String globalId = getParamTextByName(item, "elemglobalid");
			String groupName = getParamTextByName(item, "elemgroupid");
			
			long groupId = getGroupId(groupName);
				
			if(groupId != -1){
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_LAYOUT, globalId);	
						
				Layout layout = LayoutLocalServiceUtil.getLayout(GetterUtil.getLong(live.getLocalId()));	
						
				classPK = layout.getPlid();				
			}
			
		/*}else if(className.equals(IterKeys.CLASSNAME_DLFILEENTRY)){	//Document
			
			String groupName = getParamTextByName(item, "elemgroupid");		
			String folder = getParamTextByName(item, "folder");
			String version = getParamTextByName(item, "version");
			String title = getParamTextByName(item, "title");
			
			long groupId = getGroupId(groupName);
			
			long folderId = 0;
			if (!folder.equals("0")){
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_DLFOLDER, folder);	
				folderId = GetterUtil.getLong(live.getLocalId());
			}
				
			DLFileEntry dlFileEntry = DLFileEntryLocalServiceUtil.getFileEntryByTitle(groupId, folderId, title);
			DLFileVersion dlFileVersion = DLFileVersionLocalServiceUtil.getFileVersion(groupId, folderId, dlFileEntry.getName(), version);
			
			classPK = dlFileVersion.getFileVersionId();
				
			*/
		}else if(className.equals(IterKeys.CLASSNAME_USER)){	//User
			
			String screenName = getParamTextByName(item, "screenname");
			
			User user = UserLocalServiceUtil.getUserByScreenName(xmlIOContext.getCompanyId(), screenName);
			
			classPK = user.getUserId();			
			
		}else if(className.equals(IterKeys.CLASSNAME_JOURNALARTICLE)){	//Web Content
			
			String globalId = getParamTextByName(item, "elemglobalid");
			String groupName = getParamTextByName(item, "elemgroupid");			
	
			long groupId = getGroupId(groupName);
			
			Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_JOURNALARTICLE, globalId);
				
			JournalArticle journalArticle = JournalArticleLocalServiceUtil.getArticle(groupId, live.getLocalId());
					
			classPK = journalArticle.getId();							
			
		}
		
		return classPK;
	}	
}
