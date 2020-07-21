package com.protecmedia.iter.xmlio.service.item.asset;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.User;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portlet.asset.model.AssetCategory;
import com.liferay.portlet.asset.model.AssetCategoryProperty;
import com.liferay.portlet.asset.model.AssetVocabulary;
import com.liferay.portlet.asset.service.AssetCategoryLocalServiceUtil;
import com.liferay.portlet.asset.service.AssetCategoryPropertyLocalServiceUtil;
import com.liferay.portlet.asset.service.AssetVocabularyLocalServiceUtil;
import com.liferay.portlet.asset.service.persistence.AssetCategoryUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;

/* XML Structure:
 * 
<item operation="create" globalid="DiariTerrassaPre_152714" classname="com.liferay.portlet.asset.model.AssetCategoryProperty" groupid="10130">
	<param name="categoryglobalid">&lt;![CDATA[DiariTerrassaPre_151926]]&gt;</param>
	<param name="value">&lt;![CDATA[sss]]&gt;</param>
	<param name="categorygroupid">&lt;![CDATA[The Star]]&gt;</param>
	<param name="key">&lt;![CDATA[jdjd]]&gt;</param>
	<param name="userscreenname">&lt;![CDATA[test]]&gt;</param>
</item>
*/

public class AssetCategoryPropertyXmlIO extends ItemXmlIO {

	private static Log _log = LogFactoryUtil.getLog(AssetCategoryPropertyXmlIO.class);
	private String _className = IterKeys.CLASSNAME_CATEGORYPROPERTY;
	
	public AssetCategoryPropertyXmlIO() {
		super();
	}
	
	public AssetCategoryPropertyXmlIO(XMLIOContext xmlIOContext) {
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
	 * @see AssetCategoryXmlIO
	 */
	@Override
	public void populateLive(long groupId, long companyId) throws SystemException, PortalException{
		//Live populated via AssetCategoryXmlIO
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		AssetCategoryProperty categoryProperty = (AssetCategoryProperty)model;
		
		//Ojo: lo inserta como perteneciente al grupo Global
		Company company = CompanyLocalServiceUtil.getCompany(categoryProperty.getCompanyId());
		
		String id = String.valueOf(categoryProperty.getCategoryPropertyId());
		
		AssetCategory ac = AssetCategoryUtil.fetchByPrimaryKey(categoryProperty.getCategoryId());
		Live liveCategory = LiveLocalServiceUtil.getLiveByLocalId(ac.getGroupId(), IterKeys.CLASSNAME_CATEGORY,	String.valueOf(ac.getCategoryId()));
		
		AssetVocabulary av = AssetVocabularyLocalServiceUtil.getAssetVocabulary(ac.getVocabularyId());
		Live liveVocabulary = LiveLocalServiceUtil.getLiveByLocalId(av.getGroupId(), IterKeys.CLASSNAME_VOCABULARY, String.valueOf(av.getVocabularyId()));
		
		
		//insert element in LIVE
		LiveLocalServiceUtil.add(_className, company.getGroup().getGroupId(), liveVocabulary.getId(), liveCategory.getId(),
				IterLocalServiceUtil.getSystemName() + "_" + id, id, 
				IterKeys.CREATE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);	
			
		
	}
	
	@Override
	public void deleteLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		AssetCategoryProperty categoryProperty = (AssetCategoryProperty)model;
		
		//Ojo: solo lo borra si es de Global
		Company company = CompanyLocalServiceUtil.getCompany(categoryProperty.getCompanyId());
	
		String id = String.valueOf(categoryProperty.getCategoryPropertyId());
		
		//insert element in LIVE
		LiveLocalServiceUtil.add(_className, company.getGroup().getGroupId(),
				IterLocalServiceUtil.getSystemName() + "_" + id, id, 
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
		
		try{
		
			setCommonAttributes(attributes, group.getName(), live, operation);
			
			if (operation.equals(IterKeys.CREATE)){
				
					AssetCategoryProperty categoryProperty = AssetCategoryPropertyLocalServiceUtil.getAssetCategoryProperty(GetterUtil.getLong(live.getLocalId()));
					
					AssetCategory category = AssetCategoryLocalServiceUtil.getAssetCategory(categoryProperty.getCategoryId());
					Live liveCat = LiveLocalServiceUtil.getLiveByLocalId(category.getGroupId(), IterKeys.CLASSNAME_CATEGORY, String.valueOf(category.getCategoryId()));
					Group categoryGroup = GroupLocalServiceUtil.getGroup(category.getGroupId());
					
					User user = UserLocalServiceUtil.getUser(categoryProperty.getUserId());
					
					params.put("key", categoryProperty.getKey());	
					params.put("value", categoryProperty.getValue());	
					params.put("categoryglobalid", liveCat.getGlobalId());		
					params.put("categorygroupid", categoryGroup.getName());		
					params.put("userscreenname", user.getScreenName());
					
				
			}
			
			addNode(root, "item", attributes, params);	
				
			_log.debug("INFO: Category Content exported");					
			
		}catch(Exception e){
			error = "Cannot export item";
		}
		
		return error;
	}



	/*
	 * Import Functions 
	 */
	//Never updates. Deletes everything and then creates again.
	
	@Override
	protected void delete(Element item) {
		
		String sGroupId = getAttribute(item, "groupid");
		String globalId = getAttribute(item, "globalid");
		
		try{
			long groupId = getGroupId(sGroupId);
	
			try {
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
				
				try{					
					AssetCategoryProperty cat = AssetCategoryPropertyLocalServiceUtil.getAssetCategoryProperty(GetterUtil.getLong(live.getLocalId()));
					
					try{
						AssetCategoryPropertyLocalServiceUtil.deleteAssetCategoryProperty(cat);
						
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
		
		String key = getParamTextByName(item, "key");	
		String value = getParamTextByName(item, "value");	
		String categoryGlobalId = getParamTextByName(item, "categoryglobalid");	
		String categoryGroupId = getParamTextByName(item, "categorygroupid");		
		String userScreenName = getParamTextByName(item, "userscreenname");
		
		try{
			long groupId = getGroupId(sGroupId);
	
			try{
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
				
				try {
					AssetCategoryPropertyLocalServiceUtil.getAssetCategoryProperty(GetterUtil.getLong(live.getLocalId()));
				
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicate element", IterKeys.DONE, sGroupId);
				} catch (Exception e1) {
					
					try{
						long cGroupId = getGroupId(categoryGroupId);
						Live liveCat = LiveLocalServiceUtil.getLiveByGlobalId(cGroupId, IterKeys.CLASSNAME_CATEGORY, categoryGlobalId);
						
						AssetCategory category = AssetCategoryLocalServiceUtil.getAssetCategory(GetterUtil.getLong(liveCat.getLocalId()));
						
						try{							
							User user = UserLocalServiceUtil.getUserByScreenName(xmlIOContext.getCompanyId(), userScreenName);
													
							AssetCategoryProperty acp = AssetCategoryPropertyLocalServiceUtil.addCategoryProperty(user.getUserId(), category.getCategoryId(), key, value);
							
							//update entry in live table
							LiveLocalServiceUtil.add(_className, groupId, 0, 0, globalId,
									String.valueOf(acp.getCategoryPropertyId()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
								
							try{
								//update globalId to assure that match in every server.
								LiveLocalServiceUtil.updateGlobalId(groupId, _className, String.valueOf(acp.getCategoryPropertyId()), globalId);
							}catch(Exception e2){
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicate globalId", IterKeys.ERROR, sGroupId);
							}
							
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, sGroupId);	
						}catch (Exception e) {			
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e1.toString(), IterKeys.ERROR, sGroupId);
						}
					}catch (Exception e) {			
						xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "AssetCategory not found", IterKeys.ERROR, sGroupId);
					}	
				}			
			}catch (Exception e) {			
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e.toString(), IterKeys.ERROR, sGroupId);
			}							
		} catch (Exception e) {
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}	
	}
}
