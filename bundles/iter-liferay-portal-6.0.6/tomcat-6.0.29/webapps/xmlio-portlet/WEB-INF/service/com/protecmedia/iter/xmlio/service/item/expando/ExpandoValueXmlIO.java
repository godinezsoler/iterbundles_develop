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
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.User;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portlet.asset.model.AssetCategory;
import com.liferay.portlet.expando.model.ExpandoColumn;
import com.liferay.portlet.expando.model.ExpandoRow;
import com.liferay.portlet.expando.model.ExpandoTable;
import com.liferay.portlet.expando.model.ExpandoTableConstants;
import com.liferay.portlet.expando.model.ExpandoValue;
import com.liferay.portlet.expando.service.ExpandoColumnLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoTableLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoValueLocalServiceUtil;
import com.liferay.portlet.expando.service.persistence.ExpandoRowUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.NoSuchLiveException;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;


/* XML example:
*
<item operation="create" globalid="DiariTerrassaPre_316706" classname="com.liferay.portlet.expando.model.ExpandoValue" groupid="Global">
	<param name="folder">&lt;![CDATA[0]]&gt;</param>
	<param name="title">&lt;![CDATA[Lighthouse.jpg]]&gt;</param>
	<param name="elementclassname">&lt;![CDATA[com.liferay.portlet.documentlibrary.model.DLFileEntry]]&gt;</param>
	<param name="data">&lt;![CDATA[]]&gt;</param>
	<param name="columnglobalid">&lt;![CDATA[DiariTerrassaPre_118095]]&gt;</param>
	<param name="elemgroupid">&lt;![CDATA[The Star]]&gt;</param>
	<param name="version">&lt;![CDATA[1.0]]&gt;</param>
</item>
*/

public class ExpandoValueXmlIO extends ItemXmlIO {
	
	private static Log _log = LogFactoryUtil.getLog(ExpandoValueXmlIO.class);
	private String _className = IterKeys.CLASSNAME_EXPANDOVALUE;
	
	public ExpandoValueXmlIO() {
		super();
	}
	
	public ExpandoValueXmlIO(XMLIOContext xmlIOContext) {
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
	public void createLiveEntry(BaseModel<?> model) throws SystemException, PortalException
	{
		try
		{
			ExpandoValue value = (ExpandoValue)model;
			
			long globalGroupId = CompanyLocalServiceUtil.getCompany(value.getCompanyId()).getGroup().getGroupId();
				
			//Get Row if exists
			ExpandoRow row = ExpandoRowUtil.fetchByT_C(value.getTableId(), value.getClassPK());
			Live liveRow = LiveLocalServiceUtil.getLiveByLocalId(globalGroupId, IterKeys.CLASSNAME_EXPANDOROW, String.valueOf(row.getRowId()));
					
			//Crear el Expando Value asociado al elemento correspondiente (similar a AssetEntry)
			if (ArrayUtils.contains(IterKeys.CUSTOMFIELD_CLASSNAME_TYPES, value.getClassName()))
			{
				//Crear la ExpandoRow correspondiente
				if(liveRow == null)
				{	
					LiveLocalServiceUtil.add(IterKeys.CLASSNAME_EXPANDOROW, globalGroupId, 0, 0,
							IterLocalServiceUtil.getSystemName() + "_" + String.valueOf(value.getRowId()), 
							String.valueOf(value.getRowId()), 
							IterKeys.CREATE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
						
					liveRow = LiveLocalServiceUtil.getLiveByLocalId(globalGroupId, IterKeys.CLASSNAME_EXPANDOROW, String.valueOf(value.getRowId()));
				}
					
				//Add Element			
				String id = String.valueOf(value.getValueId());	
				LiveLocalServiceUtil.add(_className, globalGroupId, 0, 0,
						IterLocalServiceUtil.getSystemName() + "_" + id, id, 
						IterKeys.CREATE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
				
				// Si el ExpandoRow existe y esta publicado correctamente no se publicara el ExpandoValue
				// Se cambia a update pending para que s epublique en el pool del JournalArticle
				// El ExpandoRow pertenece al pool del JournalArticle, y el ExpandoValue pertenece al pool del ExpandoRow
				if( !liveRow.getStatus().equals(IterKeys.PENDING) )
				{
					liveRow.setOperation(IterKeys.UPDATE);
					LiveLocalServiceUtil.updateStatus(globalGroupId, IterKeys.CLASSNAME_EXPANDOROW, liveRow.getGlobalId(), IterKeys.PENDING);
				}
			}	
		}
		catch(Exception e)
		{
			_log.error("Live error", e);
		}
	}
	
	@Override
	public void updateStatusLiveEntry(BaseModel<?> model, String status) throws PortalException, SystemException
	{
		ExpandoValue value 	= (ExpandoValue)model;
		long groupId 		= CompanyLocalServiceUtil.getCompany(value.getCompanyId()).getGroup().getGroupId();
		String globalId 	= IterLocalServiceUtil.getSystemName() + "_" + String.valueOf(value.getValueId());
		
		LiveLocalServiceUtil.updateStatus(groupId, _className, globalId, status);
	}

	@Override
	public void deleteLiveEntry(BaseModel<?> model, boolean deleteJustNow) throws PortalException, SystemException
	{
		ExpandoValue value = (ExpandoValue)model;
				
		if (ArrayUtils.contains(IterKeys.CUSTOMFIELD_CLASSNAME_TYPES, value.getClassName()))
		{		
			long groupId	= CompanyLocalServiceUtil.getCompany(value.getCompanyId()).getGroup().getGroupId();
			String globalId = IterLocalServiceUtil.getSystemName() + "_" + String.valueOf(value.getValueId());
			
			_log.trace( getTraceDeleteLiveEntry(globalId, deleteJustNow) );
			
			if (deleteJustNow)
			{
				LiveLocalServiceUtil.deleteLive(groupId, _className, globalId);
			}
			else
			{
				LiveLocalServiceUtil.add(_className, groupId, 
						globalId, String.valueOf(value.getValueId()), 
						IterKeys.DELETE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
			}
		}
	}
	

	/*
	 * Export Functions
	 */
	@Override
	protected String createItemXML(XMLIOExport xmlioExport, Element root, String operation, Group group, Live live) 
	{
		String error = "";
		
		Map<String, String> attributes = new HashMap<String, String>();
		Map<String, String> params = new HashMap<String, String>();
			
		try 
		{
			//Only exists in global group
			Group globalGroup = CompanyLocalServiceUtil.getCompany(group.getCompanyId()).getGroup();
			
			setCommonAttributes(attributes, globalGroup.getName(), live, operation);
			
			if (operation.equals(IterKeys.CREATE))
			{				
				ExpandoValue value = ExpandoValueLocalServiceUtil.getExpandoValue(GetterUtil.getLong(live.getLocalId()));
				
				Live liveColumn = LiveLocalServiceUtil.getLiveByLocalId(globalGroup.getGroupId(), IterKeys.CLASSNAME_EXPANDOCOLUMN, String.valueOf(value.getColumnId()));
													
				params.put("elementclassname", value.getClassName());
				ExpandoColumn currentColumn = ExpandoColumnLocalServiceUtil.getColumn(value.getColumnId());
				if(currentColumn != null)
				{
					String valueData = value.getData();
					if (currentColumn.getName().equals(IterKeys.EXPANDO_COLUMN_NAME_SCOPEGROUPID))
					{
						valueData = getGlobalFromLocalData(valueData, Group.class, -1);
					}
					else if ( currentColumn.getName().equals(WebKeys.EXPANDO_COLUMN_NAME_MAIN_METADATAS_IDS) )
					{
						valueData = getGlobalFromLocalData(valueData, AssetCategory.class, globalGroup.getGroupId());
					}
					
					params.put("data", valueData);	
					params.put("columnglobalid", liveColumn.getGlobalId());	
					
					exportContentsByClassName(root, value.getClassPK(), value.getClassName(), attributes, params);
				}
			}
			else
			{
				addNode(root, "item", attributes, params);	
			}
			
			_log.debug("INFO: Expando Value exported");
		}
		catch (Throwable th1) 
		{
			_log.error(th1);
			error = "Cannot export item: ".concat(th1.toString());
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
	private void exportContentsByClassName(Element root, long classPK, String className, Map<String, String> attributes, Map<String, String> params) throws SystemException, PortalException{
			
			Map<String, String> paramsVal = new HashMap<String, String>();
			paramsVal.putAll(params);
				
			if (className.equals(IterKeys.CLASSNAME_LAYOUT)){
				
				Layout layout = LayoutLocalServiceUtil.getLayout(classPK);					
				Live live = LiveLocalServiceUtil.getLiveByLocalId(layout.getGroupId(), IterKeys.CLASSNAME_LAYOUT, String.valueOf(layout.getPlid()));
				Group group = GroupLocalServiceUtil.getGroup(layout.getGroupId());
				
				paramsVal.put("elemglobalid", live.getGlobalId());	
				paramsVal.put("elemgroupid", group.getName());	
				
				addNode(root, "item", attributes, paramsVal);			
			/*}else if (className.equals(IterKeys.CLASSNAME_DLFILEENTRY)){
				
				DLFileVersion dlFileVersion = DLFileVersionLocalServiceUtil.getFileVersion(classPK);
		
				DLFileEntry dLFileEntry = DLFileEntryLocalServiceUtil.getFileEntry(dlFileVersion.getGroupId(), dlFileVersion.getFolderId(), dlFileVersion.getName());
				DLFileVersion lastDlFileVersion = dLFileEntry.getLatestFileVersion();
				
				//Only import if it's the last version. 
				if (lastDlFileVersion.getFileVersionId() == classPK){
					Group group = GroupLocalServiceUtil.getGroup(dlFileVersion.getGroupId());
					
					String folder = "0";
					if (dlFileVersion.getFolderId()!= 0){
						folder = LiveLocalServiceUtil.getLiveByLocalId(dlFileVersion.getGroupId(), IterKeys.CLASSNAME_DLFOLDER, String.valueOf(dlFileVersion.getFolderId())).getGlobalId();
					}
					
					paramsVal.put("elemgroupid", group.getName());	
					paramsVal.put("version", dlFileVersion.getVersion());	
					paramsVal.put("title", dlFileVersion.getTitle());	
					paramsVal.put("folder", folder);						
					
					addNode(root, "item", attributes, paramsVal);		
				}else{
					//Si no es la última versión borramos la entrada de la tabla live
					long globalGroupId = getGroupId(IterKeys.GLOBAL_GROUP_NAME); 
					
					Live live = LiveLocalServiceUtil.getLiveByLocalId(globalGroupId, _className, String.valueOf(classPK));
					LiveLocalServiceUtil.deleteLive(live);
				}
			*/
			}else if (className.equals(IterKeys.CLASSNAME_USER)){
				
				User user = UserLocalServiceUtil.getUserById(classPK);
					
				paramsVal.put("screenname", user.getScreenName());	
					
				addNode(root, "item", attributes, paramsVal);		
			
			}else if (className.equals(IterKeys.CLASSNAME_JOURNALARTICLE)){
				
				JournalArticle journalArticle = JournalArticleLocalServiceUtil.getArticle(classPK);
				Live live = LiveLocalServiceUtil.getLiveByLocalId(journalArticle.getGroupId(), IterKeys.CLASSNAME_JOURNALARTICLE, String.valueOf(journalArticle.getArticleId()));
				Group group = GroupLocalServiceUtil.getGroup(journalArticle.getGroupId());
				
				paramsVal.put("elemglobalid", live.getGlobalId());	
				paramsVal.put("elemgroupid", group.getName());	
				
				addNode(root, "item", attributes, paramsVal);		
					
			}else{
					//Doesn´t creates the element.
			}		
	}	
	
	/*
	 * Import Functions 
	 */
	
	@Override
	protected void delete(Element item) {
		String sGroupId = getAttribute(item, "groupid");		
		String globalId = getAttribute(item, "globalid");	
		String classNameValue = getAttribute(item, "classname");			
		
		try{
			long groupId = getGroupId(sGroupId);
			
			try{
				//Get live to get the element localId
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, classNameValue, globalId);
			
				//long classNameId = ClassNameLocalServiceUtil.getClassNameId(className);				
				try {
					ExpandoValue expandoValue = ExpandoValueLocalServiceUtil.getExpandoValue(GetterUtil.getLong(live.getLocalId()));
					
					try{
						ExpandoValueLocalServiceUtil.deleteValue(expandoValue.getValueId());
						
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
	
	
	private String getGlobalFromLocalData(String data, Class<?> classValue, long groupId) throws NoSuchLiveException, SystemException
	{
		String[]values = data.split(",");
		data = "";
		for (String value : values)
		{
			Live live = LiveLocalServiceUtil.getLiveByLocalId(groupId > 0 ? groupId : Long.valueOf(value), 
																classValue.getName(), value);
			if (live != null)
				data += "," + live.getGlobalId();
			else
				_log.debug( String.format("%s %s does not exists in live.", classValue.getSimpleName(), value) );
		}
		
		if (data.length() > 0)
			data = data.substring(1);

		return data;
	}
	
	/**
	 * 
	 * @param data
	 * @param classValue
	 * @return A partir de la lista de IDs globales obtiene la lista de IDs locales
	 * @throws NoSuchLiveException
	 * @throws SystemException
	 */
	private String getLocalFromGlobalData(String data, Class<?> classValue) throws NoSuchLiveException, SystemException
	{
		String[]values = data.split(",");
		data = "";
		for (String value : values)
		{
			List<Live> live = LiveLocalServiceUtil.getLiveByGlobalId(classValue.getName(), value);
			if (Validator.isNotNull(live))
				data += "," + live.get(0).getLocalId();
			else
				_log.debug( String.format("%s %s does not exists in live.", classValue.getSimpleName(), value) );
		}
		
		if (data.length() >0 )
			data = data.substring(1);

		return data;
	}
	
	@Override
	protected void modify(Element item, Document doc) 
	{		
		_log.trace("ExpandoValueXMLIO: Begin modify");
		
		String sGroupId = getAttribute(item, "groupid");		
		String globalId = getAttribute(item, "globalid");	
		String elementClassName = getParamTextByName(item, "elementclassname");	
		String data 		= getParamTxtByName(item, "data", "");
		String columnGlobalId = getParamTextByName(item, "columnglobalid");
		
		try
		{
			List<Live> expandoColumnLive = LiveLocalServiceUtil.getLiveByGlobalId(IterKeys.CLASSNAME_EXPANDOCOLUMN, columnGlobalId);
			ExpandoColumn currentColumn = ExpandoColumnLocalServiceUtil.getColumn(Long.parseLong(expandoColumnLive.get(0).getLocalId()));
			
			if (currentColumn != null && currentColumn.getName().equals(IterKeys.EXPANDO_COLUMN_NAME_SCOPEGROUPID))
			{
				data = getLocalFromGlobalData(data, Group.class);
			}
			else if (currentColumn != null && currentColumn.getName().equals(WebKeys.EXPANDO_COLUMN_NAME_MAIN_METADATAS_IDS))
			{
				data = getLocalFromGlobalData(data, AssetCategory.class);
			}
			
			long groupId = getGroupId(sGroupId);
			Group group = GroupLocalServiceUtil.getGroup(groupId);
			
			try
			{			
				//Get live to get the element localId
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
							
				long classPK = getClassPK(item, elementClassName);
				
				if (classPK != -1)
				{	
					try
					{
						_log.trace("ExpandoValueXMLIO: Before getExpandoValue");
						ExpandoValue value = ExpandoValueLocalServiceUtil.getExpandoValue(GetterUtil.getLong(live.getLocalId()));
						_log.trace("ExpandoValueXMLIO: After getExpandoValue");
						
						//UPDATE
						try
						{
							_log.trace("ExpandoValueXMLIO: Before updateExpandoValue");
							value.setData(data);
							ExpandoValueLocalServiceUtil.updateExpandoValue(value);
							_log.trace("ExpandoValueXMLIO: After updateExpandoValue");
							
							//update entry in live table
							LiveLocalServiceUtil.add(_className, group.getGroupId(), globalId,
									String.valueOf(value.getValueId()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
							
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
						}
						catch(Exception e2)
						{
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e2.toString(), IterKeys.ERROR, group.getName());
						}
					} 
					catch (Exception e) 
					{
						if (live == null || !live.getOperation().equals(IterKeys.DELETE))
						{							
							//Existe la entrada en Live pero no existe el elemento. Borramos en Live para volver realizar una inserción completa.
							if (live != null)
							{
								LiveLocalServiceUtil.deleteLiveById(live.getId());
							}
							
							//CREATE						
							try 
							{
								long classNameId = ClassNameLocalServiceUtil.getClassNameId(elementClassName);
								
								ExpandoTable table = ExpandoTableLocalServiceUtil.getTable(xmlIOContext.getCompanyId(), classNameId, ExpandoTableConstants.DEFAULT_TABLE_NAME);
								
								Live columnLive = LiveLocalServiceUtil.getLiveByGlobalId(group.getGroupId(), IterKeys.CLASSNAME_EXPANDOCOLUMN, columnGlobalId);
							
								_log.trace("ExpandoValueXMLIO: Before addValue");	
								ExpandoValue value = ExpandoValueLocalServiceUtil.addValue(classNameId, table.getTableId(), GetterUtil.getLong(columnLive.getLocalId()), classPK, data);
								_log.trace("ExpandoValueXMLIO: After addValue");	
								
								//update entry in live table
								LiveLocalServiceUtil.add(_className, group.getGroupId(), 0, 0, globalId, 
										String.valueOf(value.getValueId()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
										
								try
								{
									//update globalId to assure that match in every server.
									LiveLocalServiceUtil.updateGlobalId(group.getGroupId(), _className, String.valueOf(value.getValueId()), globalId);
								}
								catch(Exception e2)
								{
									xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicate globalId", IterKeys.ERROR, group.getName());
								}
								_log.trace("ExpandoValueXMLIO: After updateGlobalId");
								
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
							} 
							catch (Exception e1) 
							{
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e.toString(), IterKeys.ERROR, group.getName());
							}
						}
					} 
				}
				else
				{
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE , "classPK not found", IterKeys.ERROR, sGroupId);
				}
			}
			catch(Exception e)
			{
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE , "Error: " + e.toString(), IterKeys.ERROR, sGroupId);
			}
		} 
		catch (Exception e) 
		{
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE , "GroupId not found", IterKeys.ERROR, sGroupId);
		}
		_log.trace("ExpandoValueXMLIO: End modify");
	}
	
	
	/**
	 * 
	 * @param item
	 * @param groupId
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
