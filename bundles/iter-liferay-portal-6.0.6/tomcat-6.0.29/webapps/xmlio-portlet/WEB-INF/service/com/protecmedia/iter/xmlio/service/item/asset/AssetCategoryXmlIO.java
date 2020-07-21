package com.protecmedia.iter.xmlio.service.item.asset;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.asset.model.AssetCategory;
import com.liferay.portlet.asset.model.AssetCategoryConstants;
import com.liferay.portlet.asset.model.AssetCategoryProperty;
import com.liferay.portlet.asset.model.AssetVocabulary;
import com.liferay.portlet.asset.service.AssetCategoryLocalServiceUtil;
import com.liferay.portlet.asset.service.AssetVocabularyLocalServiceUtil;
import com.liferay.portlet.asset.service.persistence.AssetCategoryPropertyUtil;
import com.liferay.portlet.asset.service.persistence.AssetCategoryUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;

/* XML Structure:
 * 
<item operation="create" globalid="DiariTerrassaPre_151926" classname="com.liferay.portlet.asset.model.AssetCategory" parentid="0" groupid="The Star">
	<param name="name">&lt;![CDATA[cat2]]&gt;</param>
	<param name="vocabularyglobalid">&lt;![CDATA[DiariTerrassaPre_151920]]&gt;</param>
	<param name="ca_ES" type="title">&lt;![CDATA[cat2]]&gt;</param>
	<param name="es_ES" type="title">&lt;![CDATA[cat2]]&gt;</param>
	<param name="en_US" type="title">&lt;![CDATA[cat2]]&gt;</param>
	<param name="fr_FR" type="title">&lt;![CDATA[cat2]]&gt;</param>
	<param name="de_DE" type="title">&lt;![CDATA[cat2]]&gt;</param>
	<param name="pt_PT" type="title">&lt;![CDATA[cat2]]&gt;</param>
	<param name="pl_PL" type="title">&lt;![CDATA[cat2]]&gt;</param>
	<param name="DiariTerrassaPre_152714" type="dependency">
		<param name="classname">&lt;![CDATA[com.liferay.portlet.asset.model.AssetCategoryProperty]]&gt;</param>
		<param name="groupname">&lt;![CDATA[10130]]&gt;</param>
	</param>
	<param name="DiariTerrassaPre_152716" type="dependency">
		<param name="classname">&lt;![CDATA[com.liferay.portlet.asset.model.AssetCategoryProperty]]&gt;</param>
		<param name="groupname">&lt;![CDATA[10130]]&gt;</param>
	</param>
</item>
*/

public class AssetCategoryXmlIO extends ItemXmlIO {

	private static Log _log = LogFactoryUtil.getLog(AssetCategoryXmlIO.class);
	private String _className = IterKeys.CLASSNAME_CATEGORY;
	
	public AssetCategoryXmlIO() {
		super();
	}
	
	public AssetCategoryXmlIO(XMLIOContext xmlIOContext) {
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
		
		AssetCategoryPropertyXmlIO acpXmlIO = new AssetCategoryPropertyXmlIO();
		
		try{
			long globalGroupId = CompanyLocalServiceUtil.getCompany(companyId).getGroup().getGroupId();
			
			List<AssetCategory> globalAssetCategories = AssetCategoryUtil.findByGroupId(globalGroupId);
			for (AssetCategory globalAssetCategory : globalAssetCategories){				
				try {
					createLiveEntry(globalAssetCategory);
				} catch (PortalException e) {
					_log.error("Can't add Live, Category: " + globalAssetCategory.getCategoryId());
				}
				
				//-- AssetCategoryProperty --		
				List<AssetCategoryProperty> assetCategoryProperties = AssetCategoryPropertyUtil.findByCategoryId(globalAssetCategory.getCategoryId());
				for (AssetCategoryProperty assetCategoryProperty : assetCategoryProperties){				
					try {
						acpXmlIO.createLiveEntry(assetCategoryProperty);
					} catch (PortalException e) {
						_log.error("Can't add Live, CategoryProperty: " + assetCategoryProperty.getCategoryPropertyId());
					}
				}
			}
		} catch (PortalException e) {
			_log.error("Can't add Live, Categories and CategoryProperties from Global group");
		}
		
		List<AssetCategory> assetCategories = AssetCategoryUtil.findByGroupId(groupId);
		for (AssetCategory assetCategory : assetCategories){				
			try {
				createLiveEntry(assetCategory);
			} catch (PortalException e) {
				_log.error("Can't add Live, Category: " + assetCategory.getCategoryId());
			}
			
			//-- AssetCategoryProperty --		
			List<AssetCategoryProperty> assetCategoryProperties = AssetCategoryPropertyUtil.findByCategoryId(assetCategory.getCategoryId());
			for (AssetCategoryProperty assetCategoryProperty : assetCategoryProperties){				
				try {
					acpXmlIO.createLiveEntry(assetCategoryProperty);
				} catch (PortalException e) {
					_log.error("Can't add Live, CategoryProperty: " + assetCategoryProperty.getCategoryPropertyId());
				}
			}
		}	
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		AssetCategory category = (AssetCategory) model;
		AssetVocabulary av = AssetVocabularyLocalServiceUtil.getAssetVocabulary(category.getVocabularyId());
		Live liveVocabulary = LiveLocalServiceUtil.getLiveByLocalId(av.getGroupId(), IterKeys.CLASSNAME_VOCABULARY, String.valueOf(av.getVocabularyId()));
		String id = String.valueOf(category.getCategoryId());
		
		//insert element in LIVE
		LiveLocalServiceUtil.add(_className, category.getGroupId(), liveVocabulary.getId(), liveVocabulary.getId(), 
				IterLocalServiceUtil.getSystemName() + "_" + id, id, 
				IterKeys.CREATE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW, true);
		
	}
	@Override
	public void deleteLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		AssetCategory category = (AssetCategory) model;
		String id = String.valueOf(category.getCategoryId());
		
		//insert element in LIVE
		LiveLocalServiceUtil.add(_className, category.getGroupId(),
				IterLocalServiceUtil.getSystemName() + "_" + id, id, 
				IterKeys.DELETE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);			
		
	}
	
	/*
	 * Export function
	 */	
	@Override
	protected String createItemXML(XMLIOExport xmlioExport, Element root, String operation, Group group,
			Live live) {
		
		String error = "";
		
		Map<String, String> attributes = new HashMap<String, String>();
		Map<String, String> params = new HashMap<String, String>();
		
		setCommonAttributes(attributes, group.getName(), live, operation);
		
		try{		
			if (operation.equals(IterKeys.CREATE)){
				AssetCategory assetCategory = AssetCategoryLocalServiceUtil.getAssetCategory(GetterUtil.getLong(live.getLocalId()));
				Live vocabularyLive = LiveLocalServiceUtil.getLiveByLocalId(assetCategory.getGroupId(), IterKeys.CLASSNAME_VOCABULARY, String.valueOf(assetCategory.getVocabularyId()));
				
				if (assetCategory.getParentCategoryId() != 0){
					Live parentCatLive = LiveLocalServiceUtil.getLiveByLocalId(assetCategory.getGroupId(), IterKeys.CLASSNAME_CATEGORY, String.valueOf(assetCategory.getParentCategoryId()));				
					attributes.put("parentid", parentCatLive.getGlobalId());
				}else{
					attributes.put("parentid", "");
				}
							
				
				params.put("name", assetCategory.getName());
				params.put("vocabularyglobalid", vocabularyLive.getGlobalId());	
				
				Element itemElement = addNode(root, "item", attributes, params);	
				
				Locale[] languages = LanguageUtil.getAvailableLocales();
				for (Locale language : languages){					
					//Title
					Map<String, String> titleAttributes = new HashMap<String, String>();
					
					titleAttributes.put("type", "title");
					titleAttributes.put("name", LocaleUtil.toLanguageId(language));
					
					addNode(itemElement, "param", titleAttributes, assetCategory.getTitle(language));					
				}
			
				addDependencies(itemElement , live.getId());
				
			}else if (operation.equals(IterKeys.UPDATE)){
				Element itemElement = addNode(root, "item", attributes, params);
				
				addDependencies(itemElement , live.getId());
			}else{
				addNode(root, "item", attributes, params);				
			}
		}catch(Exception e){
			//LiveLocalServiceUtil.setError(live.getId(), IterKeys.CORRUPT, "Can't export item");
			error = "Cannot export item";
		}
		
		_log.debug("INFO: Category exported");
		
		return error;
	}

	/*
	 * Import Functions
	 */
	
	@Override
	protected void delete(Element item) 
	{
		String sGroupId = getAttribute(item, "groupid");
		String globalId = getAttribute(item, "globalid");
			
		try
		{
			long groupId = getGroupId(sGroupId);
			
			try 
			{
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
				
				try
				{					
					AssetCategory cat = AssetCategoryLocalServiceUtil.getAssetCategory(GetterUtil.getLong(live.getLocalId()));
					
					try
					{
						AssetCategoryLocalServiceUtil.deleteCategory(cat);
						
						//update entry in live table
						LiveLocalServiceUtil.add(_className, groupId, globalId,
								live.getLocalId(), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
										
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Done", IterKeys.DONE, sGroupId);
					} 
					catch (Exception e) 
					{
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Error: " + e.toString(), IterKeys.ERROR, sGroupId);				
					}
				} 
				catch (Exception e) 
				{
					if (live != null)
					{
						//clean entry in live table
						LiveLocalServiceUtil.add(_className, groupId, globalId,
								live.getLocalId(), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
					}
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Element not found", IterKeys.DONE, sGroupId);				
				}
			} 
			catch (Exception e) 
			{
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Element not found", IterKeys.ERROR, sGroupId);				
			}
		} 
		catch (Exception e) 
		{
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}
	}

	@Override
	protected void modify(Element item, Document doc) 
	{
		boolean parentCategoryExists = false;
		boolean vocabularyExists = false;
		
		String sGroupId = getAttribute(item, "groupid");
		String globalId = getAttribute(item, "globalid");	
		String parentCatGlobalId = getAttribute(item, "parentid");
		
		String name = getParamTextByName(item, "name");	
		String vocabularyGlobalId = getParamTextByName(item, "vocabularyglobalid");
		
		try
		{
			long groupId = getGroupId(sGroupId);
			Group group = GroupLocalServiceUtil.getGroup(groupId);
			
			try
			{
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);	
			
				try 
				{
					long parentCategoryId = AssetCategoryConstants.DEFAULT_PARENT_CATEGORY_ID;
					
					if (!parentCatGlobalId.equals("")) 
					{
						Live liveCatParent = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_CATEGORY, parentCatGlobalId);
						ErrorRaiser.throwIfNull(liveCatParent);
						
						parentCategoryId = GetterUtil.getLong(liveCatParent.getLocalId());
					}
					
					parentCategoryExists = true;
				
					//Sirven para los diferentes idiomas	
					Map<Locale,String> localeTitlesMap = new HashMap<Locale, String>();
					
					List <String[]> titlelocaleList = getParamListByType(item, "title");		
					for (String[] titlelocale : titlelocaleList)
					{
						localeTitlesMap.put(LocaleUtil.fromLanguageId(titlelocale[0]), titlelocale[1]);
					}	
							
					ServiceContext serviceContext = new ServiceContext();
					serviceContext.setScopeGroupId(GroupMgr.getGlobalGroupId());
					
					Live vocLive = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_VOCABULARY, vocabularyGlobalId);
					ErrorRaiser.throwIfNull(vocLive);
					vocabularyExists=true;
					long vocabularyId = GetterUtil.getLong(vocLive.getLocalId());
					
					try
					{
						AssetCategory assetCategory = AssetCategoryLocalServiceUtil.getAssetCategory(GetterUtil.getLong(live.getLocalId()));
							
						//UPDATE--------					
						String[] categoryProperties = new String[]{};
						
						AssetCategory ac = AssetCategoryLocalServiceUtil.updateCategory(xmlIOContext.getUserId(), assetCategory.getCategoryId(), parentCategoryId, localeTitlesMap, vocabularyId, categoryProperties, serviceContext);
						
						//update entry in live table
						LiveLocalServiceUtil.add(_className, groupId, 0, 0, globalId,
								String.valueOf(ac.getCategoryId()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
					
						
						//Creamos/modificamos sus dependencias		
						try 
						{
							if (! evaluateDependencies(item, doc))
							{
								LiveLocalServiceUtil.updateStatus(groupId, _className, globalId, IterKeys.INTERRUPT);
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, sGroupId);				
							}
							else
							{					
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
							}
						} 
						catch (DocumentException err) 
						{
							LiveLocalServiceUtil.updateStatus(groupId, _className, globalId, IterKeys.INTERRUPT);
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, sGroupId);				
						}
						//--------------
						
					}
					catch(Exception e)
					{	
						
						String [] categoryProperties = {};
						
						//CREATE---------
						try
						{

							AssetCategory ac = AssetCategoryLocalServiceUtil.addCategory(xmlIOContext.getUserId(), parentCategoryId, localeTitlesMap, vocabularyId, categoryProperties, serviceContext);
							ac.setName(name);

							//update entry in live table
							LiveLocalServiceUtil.add(_className, groupId, 0 , 0, globalId,
									String.valueOf(ac.getCategoryId()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
								
							try
							{
								//update globalId to assure that match in every server.
								LiveLocalServiceUtil.updateGlobalId(group.getGroupId(), _className, String.valueOf(ac.getCategoryId()), globalId);
							}
							catch(Exception e2)
							{
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicate globalId", IterKeys.ERROR, group.getName());
							}
							
							//Creamos/modificamos sus dependencias		
							try 
							{
								if (! evaluateDependencies(item, doc))
								{
									LiveLocalServiceUtil.updateStatus(groupId, _className, globalId, IterKeys.INTERRUPT);
									xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, sGroupId);				
								}
								else
								{	
									xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
								}
							} 
							catch (DocumentException err) 
							{
								LiveLocalServiceUtil.updateStatus(groupId, _className, globalId, IterKeys.INTERRUPT);
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, sGroupId);				
							}
						} 
						catch (Exception e1) 
						{
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e1.toString(), IterKeys.ERROR, group.getName());
						}
						//-------------
					}
				} 
				catch (Exception e1) 
				{
					String errorMsg = "";
					
					if( e1 instanceof ServiceError )
					{
						if( !parentCategoryExists )
							errorMsg = "Parent category '" + parentCatGlobalId + "' does not exists in LIVE.";
						else if( !vocabularyExists )
							errorMsg = "Vocabulary '" + vocabularyGlobalId + "' does not exists in LIVE.";
						else
							errorMsg = e1.toString();
					}
					else
						errorMsg = e1.toString();
					
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + errorMsg, IterKeys.ERROR, sGroupId);
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

}
