package com.protecmedia.iter.xmlio.service.item.asset;

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
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portlet.asset.model.AssetCategory;
import com.liferay.portlet.asset.model.AssetEntry;
import com.liferay.portlet.asset.service.AssetCategoryLocalServiceUtil;
import com.liferay.portlet.asset.service.AssetEntryLocalServiceUtil;
import com.liferay.portlet.asset.service.AssetTagLocalServiceUtil;
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
<item operation="create" globalid="DiariTerrassaPre_148236" classname="com.liferay.portlet.asset.model.AssetEntry" groupid="The Star">
	<param name="elemglobalid">&lt;![CDATA[DiariTerrassaPre_148215]]&gt;</param>
	<param name="elemclassname">&lt;![CDATA[com.liferay.portlet.journal.model.JournalArticle]]&gt;</param>
	<param name="elemgroupname">&lt;![CDATA[The Star]]&gt;</param>
	<param name="DiariTerrassaPre_122013" type="category">&lt;![CDATA[]]&gt;</param>
	<param name="DiariTerrassaPre_122015" type="category">&lt;![CDATA[]]&gt;</param>
	<param name="DiariTerrassaPre_122308" type="category">&lt;![CDATA[]]&gt;</param>
	<param name="DiariTerrassaPre_122310" type="category">&lt;![CDATA[]]&gt;</param>
</item>
*/

public class AssetEntryXmlIO extends ItemXmlIO {

	private static Log _log = LogFactoryUtil.getLog(AssetEntryXmlIO.class);
	private String _className = IterKeys.CLASSNAME_ENTRY;
	
	public AssetEntryXmlIO() {
		super();
	}
	
	public AssetEntryXmlIO(XMLIOContext xmlIOContext) {
		super(xmlIOContext);
	}
	
	@Override
	public String getClassName(){
		return _className;
	}
	
	/*
	 * Live Functions
	 */
	//TODO: Usar Reflection para simplificar
	@Override
	public void populateLive(long groupId, long companyId) throws SystemException
	{
		//Created in the elements that contains it
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException
	{
		AssetEntry entry = (AssetEntry)model;
		if (ArrayUtils.contains(IterKeys.ENTRY_CLASSNAME_TYPES, entry.getClassName()))
		{
			
			long groupId = entry.getGroupId();
			
			//Add Element			
			String id = String.valueOf(entry.getEntryId());	
			LiveLocalServiceUtil.add(_className, groupId, 0, 0,
					IterLocalServiceUtil.getSystemName() + "_" + id, id, 
					IterKeys.CREATE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
		}
	}

	@Override
	public void deleteLiveEntry(BaseModel<?> model, boolean deleteJustNow) throws PortalException, SystemException
	{
		try 
		{
			AssetEntry entry = (AssetEntry)model;
			if (ArrayUtils.contains(IterKeys.ENTRY_CLASSNAME_TYPES, entry.getClassName()))
			{
				String id 		= String.valueOf(entry.getEntryId());
				long groupId	= entry.getGroupId();
				String globalId = IterLocalServiceUtil.getSystemName() + "_" + id;

				_log.trace( getTraceDeleteLiveEntry(globalId, deleteJustNow) );
				
				if (deleteJustNow)
				{
					LiveLocalServiceUtil.deleteLive(groupId, _className, globalId);
				}
				else
				{		
					LiveLocalServiceUtil.add(_className, groupId, globalId, id, IterKeys.DELETE, 
											 IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
				}
			}
		} 
		catch (Exception e) 
		{		
			_log.error(e.toString());
			_log.trace(e);
		}		
	}
	
	@Override
	public void updateStatusLiveEntry(BaseModel<?> model, String status) throws PortalException, SystemException
	{
		AssetEntry entry = (AssetEntry)model;
		String globalId  = IterLocalServiceUtil.getSystemName() + "_" + String.valueOf(entry.getEntryId());
		
		LiveLocalServiceUtil.updateStatus(entry.getGroupId(), _className, globalId, status);
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
		
		setCommonAttributes(attributes, group.getName(), live, operation);
		
		try
		{						
			if (operation.equals(IterKeys.CREATE))
			{
				AssetEntry assetEntry = AssetEntryLocalServiceUtil.getAssetEntry(GetterUtil.getLong(live.getLocalId()));
				String className = assetEntry.getClassName();
				
				//JournalArticle localId uses articleId instead of id_. Let's control this:
				String ident = String.valueOf(assetEntry.getClassPK());
				long groupId = assetEntry.getGroupId();
				
				if(className.equals(IterKeys.CLASSNAME_JOURNALARTICLE))
				{
					JournalArticle ja = JournalArticleLocalServiceUtil.getLatestArticle(assetEntry.getClassPK());
					ident = ja.getArticleId();
				}
				else if (className.equals(IterKeys.CLASSNAME_GROUP))
				{
					Group g = GroupLocalServiceUtil.getGroup(assetEntry.getClassPK());
					groupId = g.getGroupId();
				}
				Live liveElement = LiveLocalServiceUtil.getLiveByLocalId(groupId, className, ident);
				
				params.put("elemglobalid", 	liveElement.getGlobalId());
				params.put("elemclassname", className);
				params.put("elemgroupname", GroupLocalServiceUtil.getGroup(groupId).getName());
				
				
				Element itemNode = addNode(root, "item", attributes, params);	
				
				error = exportCategoryIds(itemNode, assetEntry);				
			}
			else
			{
				addNode(root, "item", attributes, params);
			}			
		}
		catch(Exception e)
		{
			//LiveLocalServiceUtil.setError(live.getId(), IterKeys.CORRUPT, "Can't export item");
			error = "Cannot export item";
		}				
		
		_log.debug("INFO: Entry exported");
		
		return error;
	}
	
	/**
	 * 
	 * @param parentNode
	 * @param assetEntry
	 * @throws SystemException
	 * @throws PortalException
	 */
	private String exportCategoryIds(Element parentNode, AssetEntry assetEntry) throws SystemException
	{
		StringBuilder error = new StringBuilder();
		
		Map<String, String> catAttributes = new HashMap<String, String>();
	
		long [] categoryIds = assetEntry.getCategoryIds();												
		for(long categoryId : categoryIds)
		{
			try
			{
				AssetCategory cat = AssetCategoryLocalServiceUtil.getAssetCategory(categoryId);
				Live liveCat = LiveLocalServiceUtil.getLiveByLocalId(cat.getGroupId(), IterKeys.CLASSNAME_CATEGORY, String.valueOf(categoryId));
				String groupName = GroupLocalServiceUtil.getGroup(cat.getGroupId()).getName();
					
				catAttributes.put("type", "category");
				catAttributes.put("name", groupName);
				addNode(parentNode, "param", catAttributes, liveCat.getGlobalId());
			}
			catch (Exception e)
			{
				error.append((error.length()==0?"":";") + "Error in category " + categoryId);				
			}			
		}		
		
		return error.toString();
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
					AssetEntry entry = AssetEntryLocalServiceUtil.getAssetEntry(GetterUtil.getLong(live.getLocalId()));
					
					try{
						AssetEntryLocalServiceUtil.deleteAssetEntry(entry);
						
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
	protected void modify(Element item, Document doc) 
	{
		_log.trace(" ");
		_log.trace("AssetEntry: Begin");
		
		String sGroupId = getAttribute(item, "groupid");
		String globalId = getAttribute(item, "globalid");	
		
		String elemGlobalId = getParamTextByName(item, "elemglobalid");	
		String elemClassName = getParamTextByName(item, "elemclassname");	
		String elemGroupName = getParamTextByName(item, "elemgroupname");	
		List<String []> catList = getParamListByType(item, "category");	
		
		_log.trace("AssetEntry: After params");
		try
		{
			long groupId = getGroupId(sGroupId);
			
			try
			{				
				//gets associated element Live globalId
				long elemGroupId = getGroupId(elemGroupName);
				Group groupG = GroupLocalServiceUtil.getGroup(elemGroupId);
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupG.getGroupId(), elemClassName, elemGlobalId);
				
				_log.trace("AssetEntry: After LiveByGlobalId");
				try
				{
					//gets entryId associated to the element
					long entryId = getEntryId(live.getLocalId(), elemClassName, groupG.getGroupId());
					
					_log.trace("AssetEntry: After getEntryId");
					if(entryId != -1)
					{
						AssetEntry assetEntry = AssetEntryLocalServiceUtil.getAssetEntry(entryId);
						_log.trace("AssetEntry: After getAssetEntry");
						
						String [] tagNames = AssetTagLocalServiceUtil.getTagNames(assetEntry.getClassName(), assetEntry.getClassPK());  
						_log.trace("AssetEntry: After getTagNames");
															
						AssetEntryLocalServiceUtil.updateEntry(assetEntry.getUserId(), groupId, assetEntry.getClassName(), 
								assetEntry.getClassPK(), getCategoryIds(catList, globalId, groupId), tagNames);
						_log.trace("AssetEntry: After updateEntry");
						
						//update entry in live table
						LiveLocalServiceUtil.add(_className, groupId, 0, 0, globalId,
								String.valueOf(assetEntry.getEntryId()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
						_log.trace("AssetEntry: LiveLocalServiceUtil.add");
						try
						{
							//Update globalId.
							LiveLocalServiceUtil.updateGlobalId(groupId, _className, String.valueOf(assetEntry.getEntryId()), globalId);
							_log.trace("AssetEntry: updateGlobalId");
							
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, sGroupId);
							_log.trace("AssetEntry: update XMLIO_Live Done");
						}
						catch(Exception e3)
						{
							_log.trace(e3);
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, e3.toString(), IterKeys.ERROR, sGroupId);
						}
						
						_log.trace("AssetEntry: itemLog.addMessage");
						
					}
					else
					{
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Element not found", IterKeys.ERROR, sGroupId);	
					}	
				}
				catch(Exception e)
				{	
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, elemClassName + " not found", IterKeys.ERROR, sGroupId);	
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
		
		_log.trace("AssetEntry: End");
		_log.trace(" ");
	}
	
	
	/**
	 * 
	 * @param id
	 * @param className
	 * @param groupId
	 * @return
	 * @throws PortalException
	 * @throws SystemException
	 */	
	private long getEntryId(String id, String className, long groupId){
		long entryId = -1;
		
		try{
			if (ArrayUtils.contains(IterKeys.ENTRY_CLASSNAME_TYPES, className)){
				
				long pk = GetterUtil.getLong(id);
				
				if(className.equals(IterKeys.CLASSNAME_JOURNALARTICLE)){
					JournalArticle article = JournalArticleLocalServiceUtil.getArticle(groupId, id);
					pk = article.getResourcePrimKey();				
				}
				
				AssetEntry ae = AssetEntryLocalServiceUtil.getEntry(className, pk);
				entryId = ae.getEntryId();								
			}	
		}
		catch(Exception err){
			_log.error("Asset not found " + groupId + ", " + id + ", " + className);
		}
		
		return entryId;
	}
	
	/**
	 * 
	 * @param catList
	 * @param globalId
	 * @param groupName
	 * @return
	 * @throws SystemException 
	 * @throws NoSuchLiveException 
	 */
	private long[] getCategoryIds(List<String []> catList, String globalId, long groupId) throws NoSuchLiveException, SystemException
	{
		long[] listIds = new long[catList.size()];
		int numElem = 0;
		
		for (String [] cat : catList) 
		{	
			Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_CATEGORY, cat[1]);
			
			if(live != null)
			{
				listIds[numElem] = GetterUtil.getLong(live.getLocalId());
			}
			
			numElem++;	
		}						
		
		return listIds;
		
	}

}
