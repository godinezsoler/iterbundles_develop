/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.service.item.portal;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Contact;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Organization;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.User;
import com.liferay.portal.model.UserGroup;
import com.liferay.portal.model.UserGroupRole;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.ContactLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.OrganizationLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserGroupLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.service.persistence.UserUtil;
import com.liferay.portlet.asset.model.AssetEntry;
import com.liferay.portlet.asset.service.AssetEntryLocalServiceUtil;
import com.liferay.portlet.asset.service.persistence.AssetEntryUtil;
import com.liferay.portlet.expando.model.ExpandoRow;
import com.liferay.portlet.expando.model.ExpandoTable;
import com.liferay.portlet.expando.model.ExpandoValue;
import com.liferay.portlet.expando.service.ExpandoRowLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoTableLocalServiceUtil;
import com.liferay.portlet.expando.service.ExpandoValueLocalServiceUtil;
import com.liferay.portlet.expando.service.persistence.ExpandoRowUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LivePoolLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.item.asset.AssetEntryXmlIO;
import com.protecmedia.iter.xmlio.service.item.expando.ExpandoRowXmlIO;
import com.protecmedia.iter.xmlio.service.item.expando.ExpandoValueXmlIO;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;
import com.protecmedia.iter.xmlio.service.util.XMLIOImport;
import com.protecmedia.iter.xmlio.service.util.XMLIOUtil;

/*
 * User
 */

public class UserXmlIO extends ItemXmlIO {
	
	private static Log _log = LogFactoryUtil.getLog(UserXmlIO.class);
	private String _className = IterKeys.CLASSNAME_USER;
	
	public UserXmlIO () {		
		super();
	}
	
	public UserXmlIO (XMLIOContext xmlIOContext) {		
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
		List<User> users = UserUtil.findByCompanyId(companyId);
		for (User user : users){	
			try {	
				//NO se publican los usuarios con rol administrador ni el usuario por defecto
				if(!ArrayUtils.contains(user.getRoleIds(), RoleLocalServiceUtil.getRole(companyId, IterKeys.ROLE_ADMINISTRATOR).getRoleId()) &&
						!user.getEmailAddress().equals(IterKeys.DEFAULT_USER_EMAIL)){
			
					//1. Populate User's AssetEntry
					try {
						AssetEntry assetEntry = AssetEntryUtil.fetchByC_C(ClassNameLocalServiceUtil.getClassNameId(IterKeys.CLASSNAME_USER), user.getPrimaryKey());
						if (assetEntry != null){
							ItemXmlIO entryItem = new AssetEntryXmlIO();
							entryItem.createLiveEntry(assetEntry);
						}
					} catch (PortalException e) {
						_log.error("Can't add Live, AssetEntry, userId: " + user.getUserId());
					}	
					
					//2. Populate Layout's ExpandoRow	
					List<ExpandoTable> expandoTableList = ExpandoTableLocalServiceUtil.getTables(companyId, IterKeys.CLASSNAME_USER);
					ItemXmlIO expandoRowItem = new ExpandoRowXmlIO();
					for (ExpandoTable expandoTable : expandoTableList){				
						ExpandoRow expandoRow = ExpandoRowUtil.fetchByT_C(expandoTable.getTableId(), user.getPrimaryKey());
						if (expandoRow != null){
							try {
								expandoRowItem.createLiveEntry(expandoRow);
							} catch (PortalException e) {
								_log.error("Can't add Live, ExpandoRow: " + expandoRow.getRowId());
							}					
							
							//3. Populate Layout's ExpandoValue
							List<ExpandoValue> expandoValueList = ExpandoValueLocalServiceUtil.getRowValues(expandoRow.getRowId());
							ItemXmlIO expandoValueItem = new ExpandoValueXmlIO();
							for (ExpandoValue expandoValue : expandoValueList){				
								try {
									expandoValueItem.createLiveEntry(expandoValue);
								} catch (PortalException e) {
									_log.error("Can't add Live, ExpandoValue: " + expandoValue.getValueId());
								}
							}
						}
					}
					
					//4. Populate User
					createLiveEntry(user);	
				}					
			} catch (PortalException e) {
				_log.error("Can't add Live, User: " + user.getUserId());
			}			
		}
		
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		User user = (User)model;
		
		Company company = CompanyLocalServiceUtil.getCompany(user.getCompanyId());
		
		String id = String.valueOf(user.getUserId());
		
		/*
		 * Hay elementos dependientes de User que se crean antes que él, 
		 * por lo que hay que establecer las dependencias aqui. 
		 */
		
		//TODO: comprobar si el usuario tiene rol administrador. En tal caso no se importará

		//1. Añado User
		Live liveUser = LiveLocalServiceUtil.add(_className, company.getGroup().getGroupId(), -1, 0, 
				IterLocalServiceUtil.getSystemName() + "_" + user.getEmailAddress(), id,
				IterKeys.CREATE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
	
		
		if (IterLocalServiceUtil.getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW)){
			
			try{
				//2. Actualizo el pool en el AssetEntry del User
				AssetEntry assetEntry = AssetEntryLocalServiceUtil.getEntry(IterKeys.CLASSNAME_USER, user.getPrimaryKey());
				Live liveEntry = LiveLocalServiceUtil.getLiveByLocalId(assetEntry.getGroupId(), IterKeys.CLASSNAME_ENTRY, String.valueOf(assetEntry.getEntryId()));
				LivePoolLocalServiceUtil.createLivePool(liveUser.getId(), liveUser.getId(), liveEntry.getId(), false);
			}
			catch(Exception err){
				_log.debug("AssetEntry data cannot be added as dependency of User " + user.getUserId());
			}
			
			try{
				//3. Actualizo el pool en el ExpandoRow del User
				ExpandoTable table = ExpandoTableLocalServiceUtil.getDefaultTable(company.getCompanyId(), IterKeys.CLASSNAME_USER);
				ExpandoRow row = ExpandoRowLocalServiceUtil.getRow(table.getTableId(), user.getUserId());
				Live liveRow = LiveLocalServiceUtil.getLiveByLocalId(company.getGroup().getGroupId(), IterKeys.CLASSNAME_EXPANDOROW, String.valueOf(row.getRowId()));
				LivePoolLocalServiceUtil.createLivePool(liveUser.getId(), liveUser.getId(), liveRow.getId(), false);
				
				//4. Actualizo el pool en los ExpandoValue del User
				List<ExpandoValue> expandoValues = ExpandoValueLocalServiceUtil.getRowValues(row.getRowId());
				for(ExpandoValue expandoValue : expandoValues){
					Live liveValue = LiveLocalServiceUtil.getLiveByLocalId(company.getGroup().getGroupId(), IterKeys.CLASSNAME_EXPANDOVALUE, String.valueOf(expandoValue.getValueId()));
					LivePoolLocalServiceUtil.createLivePool(liveUser.getId(), liveRow.getId(), liveValue.getId(), false);
				}
			}
			catch(Exception err){
				_log.debug("Expando data cannot be added as dependency of User " + user.getUserId());
			}
		}
	
	}
	
	@Override
	public void deleteLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		User user = (User)model;
		
		String id = String.valueOf(user.getUserId());
		long groupId = CompanyLocalServiceUtil.getCompany(user.getCompanyId()).getGroup().getGroupId();
		
		LiveLocalServiceUtil.add(_className, groupId, IterLocalServiceUtil.getSystemName() + "_" + user.getEmailAddress(), 
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
				
		//si la estructura no es nula, agrego parametros avanzados
		if (operation.equals(IterKeys.CREATE)){			
			try {
				User user = UserLocalServiceUtil.getUser(GetterUtil.getLong(live.getLocalId()));
				Contact contact = ContactLocalServiceUtil.getContact(user.getContactId());				
				
				//Elementos de la tabla USER
				params.put("email", user.getEmailAddress());
				params.put("firstname", user.getFirstName());	
				params.put("middlename", user.getMiddleName());
				params.put("lastname", user.getLastName());
				params.put("title", user.getJobTitle());				
				params.put("autopassword", String.valueOf(user.getPasswordReset()));
				params.put("password", user.getPassword());
				params.put("screenname", user.getScreenName());
				params.put("autoscreenname", (user.getScreenName().trim().equals("")) ? "true" : "false" );	
				params.put("languageid", user.getLanguageId());				
				params.put("facebookid", String.valueOf(user.getFacebookId())); //TODO: ver si este Id es único para todos los sistemas
				params.put("openid", String.valueOf(user.getOpenId())); //TODO: ver si este Id es único para todos los sistemas
				params.put("reminderqueryquestion", user.getReminderQueryQuestion());
				params.put("reminderqueryanswer", user.getReminderQueryAnswer());
				params.put("timezoneid", user.getTimeZoneId());				
				params.put("greeting", user.getGreeting());			
				params.put("comments", user.getComments());			
				params.put("icon-image-file", XMLIOUtil.exportImageFile(xmlioExport, user.getPortraitId()));			
				params.put("active", String.valueOf(user.getActive()));
												
				//Elementos de la tabla CONTACT
				params.put("prefixid", String.valueOf(contact.getPrefixId()));
				params.put("suffixid", String.valueOf(contact.getSuffixId()));
				params.put("male", String.valueOf(contact.getMale()));
				params.put("birthday", GetterUtil.getString(user.getBirthday(), ""));				
				params.put("smssn", contact.getSmsSn());
				params.put("aimsn", contact.getAimSn());
				params.put("facebooksn", contact.getFacebookSn());
				params.put("icqsn", contact.getIcqSn());
				params.put("jabbersn", contact.getJabberSn());
				params.put("msnsn", contact.getMsnSn());
				params.put("myspacesn", contact.getMySpaceSn());
				params.put("skypesn", contact.getSkypeSn());
				params.put("twittersn", contact.getTwitterSn());
				params.put("ymsn", contact.getYmSn());
				
				Element itemElement = addNode(root, "item", attributes, params);
				
				//TODO: Comprobar que los parametros a continuación tienen el Name como clave única				
				long [] groupIds = user.getGroupIds();									
				for(long gId : groupIds){
					Group g = GroupLocalServiceUtil.getGroup(gId);
					
					Map<String, String> groupAttributes = new HashMap<String, String>();
									
					groupAttributes.put("type", "group");
					groupAttributes.put("name", g.getName());
					
					addNode(itemElement, "param", groupAttributes, "");
				}				
				
				long[] organizationIds = user.getOrganizationIds();
				for(long oId : organizationIds){
					Organization userOrganization = OrganizationLocalServiceUtil.getOrganization(oId);	
					
					Map<String, String> organizationAttributes = new HashMap<String, String>();
					
					organizationAttributes.put("type", "organization");
					organizationAttributes.put("name", userOrganization.getName());
					
					addNode(itemElement, "param", organizationAttributes, "");
				}
				
				long[] roleIds = user.getRoleIds();
				for(long rId : roleIds){
					Role userRole = RoleLocalServiceUtil.getRole(rId);	
					
					Map<String, String> roleAttributes = new HashMap<String, String>();
					
					roleAttributes.put("type", "role");
					roleAttributes.put("name", userRole.getName());
					
					addNode(itemElement, "param", roleAttributes, "");	
				}
				
				long[] userGroupIds = user.getUserGroupIds();
				for(long ugId : userGroupIds){
					UserGroup userGroup = UserGroupLocalServiceUtil.getUserGroup(ugId);
					
					Map<String, String> userGroupAttributes = new HashMap<String, String>();
					
					userGroupAttributes.put("type", "usergroup");
					userGroupAttributes.put("name", userGroup.getName());
					
					addNode(itemElement, "param", userGroupAttributes, "");	
				}
				addDependencies(itemElement, live.getId());
			}catch(Exception e){
				//LiveLocalServiceUtil.setError(live.getId(), IterKeys.CORRUPT, "Can't export item");			
				error = "Cannot export item";
			}
		}else if (operation.equals(IterKeys.CREATE)){		
			addDependencies(addNode(root, "item", attributes, params), live.getId());
		}else{
			addNode(root, "item", attributes, params);
		}
		
		_log.debug("XmlItem OK");
		
		return error;
		
		/*Parametros pendientes USER:
		 * 	
			createDate,	modifiedDate, defaultUser, contactId, passwordEncrypted,
			passwordModifiedDate, digest, graceLoginCount, loginDate, loginIP, lastLoginDate,
			lastLoginIP, lastFailedLoginDate, failedLoginAttempts, lockout,	lockoutDate
			agreedToTermsOfUse
		 */
		
		/*Parametros pendientes CONTACT:
		 * 	
		  `createDate` datetime DEFAULT NULL,
		  `modifiedDate` datetime DEFAULT NULL,
		  `accountId` bigint(20) DEFAULT NULL,
		  `parentContactId` bigint(20) DEFAULT NULL,		  
		  `employeeStatusId` varchar(75) COLLATE utf8_unicode_ci DEFAULT NULL,
		  `employeeNumber` varchar(75) COLLATE utf8_unicode_ci DEFAULT NULL,
		  `jobClass` varchar(75) COLLATE utf8_unicode_ci DEFAULT NULL,
		  `hoursOfOperation` varchar(75) COLLATE utf8_unicode_ci DEFAULT NULL,
		 * 
		 */
	}
	
	
	/*
	 * Import Functions
	 */
	@Override	
	public void delete(Element item) {
		
		String sGroupId = getAttribute(item, "groupid");
		String globalId = getAttribute(item, "globalid");
		
		try{
			long groupId = getGroupId(sGroupId);
			
			try {
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
				
				try{					
					User user = UserLocalServiceUtil.getUser(GetterUtil.getLong(live.getLocalId()));
					
					try{
						UserLocalServiceUtil.deleteUser(user.getUserId());
						
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
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Calendar now = Calendar.getInstance();			
		
		String sGroupId = getAttribute(item, "groupid");
		String globalId = getAttribute(item, "globalid");	
			
		String email = getParamTextByName(item, "email");
		String screenName = getParamTextByName(item, "screenname");
		String firstName = getParamTextByName(item, "firstname");
		String middleName = getParamTextByName(item, "middlename");
		String lastName = getParamTextByName(item, "lastname");
		String title = getParamTextByName(item, "title");
		boolean autoPassword = GetterUtil.getBoolean(getParamTextByName(item, "autopassword"), true);
		String password = getParamTextByName(item, "password");
		boolean autoScreenName = GetterUtil.getBoolean(getParamTextByName(item, "autoscreenname"), false);	
		String languageId = getParamTextByName(item, "languageid");	
		long facebookId = GetterUtil.getLong(getParamTextByName(item, "facebookid"), 0);
		String openId = GetterUtil.get(getParamTextByName(item, "openid"),StringPool.BLANK);
		String reminderQQ = getParamTextByName(item, "reminderqueryquestion");
		String reminderQA = getParamTextByName(item, "reminderqueryanswer");
		String timeZoneId = getParamTextByName(item, "timezoneid");
		String greeting = getParamTextByName(item, "greeting");
		String comments = getParamTextByName(item, "comments");
		String iconImageFile = getParamTextByName(item, "icon-image-file");
		boolean active = GetterUtil.getBoolean(getParamTextByName(item, "active"), true);
		
		int prefixId = GetterUtil.getInteger(getParamTextByName(item, "prefixid"), 0);
		int suffixId = GetterUtil.getInteger(getParamTextByName(item, "suffixid"), 0);
		boolean male = GetterUtil.getBoolean(getParamTextByName(item, "male"), true);
		Date birthday = GetterUtil.getDate(getParamTextByName(item, "birthday"), df, now.getTime());
		String smsSn = GetterUtil.get(getParamTextByName(item, "smssn"),StringPool.BLANK);
		String aimSn = GetterUtil.get(getParamTextByName(item, "aimsn"),StringPool.BLANK);
		String facebookSn = GetterUtil.get(getParamTextByName(item, "facebooksn"),StringPool.BLANK);
		String icqSn = GetterUtil.get(getParamTextByName(item, "icqsn"),StringPool.BLANK);
		String jabberSn = GetterUtil.get(getParamTextByName(item, "jabbersn"),StringPool.BLANK);
		String msnSn = GetterUtil.get(getParamTextByName(item, "msnsn"),StringPool.BLANK);
		String mySpaceSn = GetterUtil.get(getParamTextByName(item, "myspacesn"),StringPool.BLANK);
		String skypeSn = GetterUtil.get(getParamTextByName(item, "skypesn"),StringPool.BLANK);
		String twitterSn = GetterUtil.get(getParamTextByName(item, "twittersn"),StringPool.BLANK);
		String ymSn = GetterUtil.get(getParamTextByName(item, "ymsn"),StringPool.BLANK);
		
		try{
			long groupId = getGroupId(sGroupId);
			Group group = GroupLocalServiceUtil.getGroup(groupId);
			try{
				Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);	
			
				try {	
					ServiceContext serviceContext = new ServiceContext();					
					serviceContext.setAddCommunityPermissions(true);
					serviceContext.setAddGuestPermissions(true);
					
					//Birthday Date
					Calendar cal=Calendar.getInstance();
					cal.setTime(birthday);
					int birthdayMonth = cal.get(Calendar.MONTH);
					int birthdayDay = cal.get(Calendar.DAY_OF_MONTH);
					int birthdayYear = cal.get(Calendar.YEAR);	
					
					try{
						User user = UserLocalServiceUtil.getUser(GetterUtil.getLong(live.getLocalId()));
						
						//UPDATE
						try{			
							
							
							//TODO: Importar UserGroupRole y pasar sus globalIds
							List<UserGroupRole> userGroupRoles = null;			
							
							UserLocalServiceUtil.updateUser(user.getUserId(), user.getPassword(), null, null, 
									autoPassword, reminderQQ, reminderQA, screenName, 
									email, facebookId, openId, languageId, timeZoneId, greeting, comments, 
									firstName, middleName, lastName, prefixId, suffixId, male, birthdayMonth, 
									birthdayDay, birthdayYear, smsSn, aimSn, facebookSn, icqSn, jabberSn, msnSn, 
									mySpaceSn, skypeSn, twitterSn, ymSn, title, getGroupIdents(item, globalId, group.getName()),
									getOrganizationIds(item, globalId, group.getName()), getRoleIds(item, globalId, group.getName()),
									userGroupRoles, getUserGroupIds(item, globalId, group.getName()), serviceContext);
							
							UserLocalServiceUtil.updateActive(user.getUserId(), active);
							
							try{
								UserLocalServiceUtil.updatePassword(user.getUserId(), password, password, false);
							}catch(Exception e){			
							}
							
							//Update icon image
							try {	
								if(!iconImageFile.equals("")){
									byte[] bytes = XMLIOImport.getFileAsBytes(iconImageFile);									
									UserLocalServiceUtil.updatePortrait(user.getUserId(), bytes);	
								}
							}catch(Exception e){
								_log.error("XMLIO Utils Error adding image");
							}
										
							//update entry in live table
							LiveLocalServiceUtil.add(_className, group.getGroupId(), globalId,
									String.valueOf(user.getUserId()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
												
							try {
								//Creamos/modificamos sus dependencias	
								if (! evaluateDependencies(item, doc)){
									LiveLocalServiceUtil.updateStatus(group.getGroupId(), _className, globalId, IterKeys.INTERRUPT);
									xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, group.getName());				
								}else{									
									xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
								}
							} catch (DocumentException err) {
								LiveLocalServiceUtil.updateStatus(group.getGroupId(), _className, globalId, IterKeys.INTERRUPT);
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, group.getName());				
							}	
						} catch (Exception ex) {
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + ex.toString(), IterKeys.ERROR, group.getName());
						}				
					}catch(Exception e){
						//CREATE
						try{			
							long creatorUserId = 0;
				
							User user = UserLocalServiceUtil.addUser(creatorUserId, group.getCompanyId(), autoPassword, password, password, 
									autoScreenName, screenName,	email, facebookId, openId, LocaleUtil.fromLanguageId(languageId), firstName,
									middleName, lastName, prefixId, suffixId, male, birthdayMonth, birthdayDay, 
									birthdayYear, title, getGroupIdents(item, globalId, group.getName()), getOrganizationIds(item, globalId, group.getName()),
									getRoleIds(item, globalId, group.getName()), getUserGroupIds(item, globalId, group.getName()), false,
									serviceContext);
							
							UserLocalServiceUtil.updateActive(user.getUserId(), active);
							
							//Update icon image
							long imageId = XMLIOUtil.importImageFile (iconImageFile);
							if (imageId != -1){
								user.setPortraitId(imageId);
							}

							//update entry in live table
							LiveLocalServiceUtil.add(_className, group.getGroupId(), globalId,
									String.valueOf(user.getUserId()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
								
							try{
								//update globalId to assure that match in every server.
								LiveLocalServiceUtil.updateGlobalId(group.getGroupId(), _className, String.valueOf(user.getUserId()), globalId);
							}catch(Exception e2){
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicate globalId", IterKeys.ERROR, group.getName());
							}
							
							try {
								//Creamos/modificamos sus dependencias	
								if (! evaluateDependencies(item, doc)){
									LiveLocalServiceUtil.updateStatus(group.getGroupId(), _className, globalId, IterKeys.INTERRUPT);
									xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, group.getName());				
								}else{									
									xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, group.getName());
								}
							} catch (DocumentException err) {
								LiveLocalServiceUtil.updateStatus(group.getGroupId(), _className, globalId, IterKeys.INTERRUPT);
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, group.getName());				
							}	
						} catch (Exception e1) {
							xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e1.toString(), IterKeys.ERROR, group.getName());
						}
					}
				} catch (Exception e1) {
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e1.toString(), IterKeys.ERROR, sGroupId);
				}
			}catch (Exception e1) {			
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e1.toString(), IterKeys.ERROR, sGroupId);
			}
		} catch (Exception e) {
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}		
	}
	
	
	/**
	 * 
	 * @param item
	 * @param globalId
	 * @param groupName
	 * @return
	 */
	private long[] getGroupIdents(Element item, String globalId, String groupName){
		
		List<String[]> paramList = getParamListByType(item, "group");
		long[] listIds = new long[paramList.size()];
		
		try{			
			int numElem = 0;		
			for (String[] param : paramList) {	
				Group group = GroupLocalServiceUtil.getGroup(xmlIOContext.getCompanyId(), param[0]);
				listIds[numElem] = group.getGroupId();
					
				numElem++;			
			}
		}catch(Exception e){
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "GroupId not found", IterKeys.ERROR, groupName);
		}
		
		return listIds;
	}
	
	/**
	 * 
	 * @param item
	 * @param globalId
	 * @param groupName
	 * @return
	 */
	private long[] getOrganizationIds(Element item, String globalId, String groupName){
		
		List<String[]> paramList = getParamListByType(item, "organization");
		long[] listIds = new long[paramList.size()];
		
		try{			
			int numElem = 0;		
			for (String[] param : paramList) {	
				Organization org = OrganizationLocalServiceUtil.getOrganization(xmlIOContext.getCompanyId(), param[0]);
				listIds[numElem] = org.getOrganizationId();
					
				numElem++;			
			}
		}catch(Exception e){
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "OrganizationId not found", IterKeys.ERROR, groupName);
		}
		
		return listIds;
	}
	
	/**
	 * 
	 * @param item
	 * @param globalId
	 * @param groupName
	 * @return
	 */
	private long[] getRoleIds(Element item, String globalId, String groupName){
		
		List<String[]> paramList = getParamListByType(item, "role");
		long[] listIds = new long[paramList.size()];
		
		try{			
			int numElem = 0;		
			for (String[] param : paramList) {	
				Role role = RoleLocalServiceUtil.getRole(xmlIOContext.getCompanyId(), param[0]);
				listIds[numElem] = role.getRoleId();
					
				numElem++;			
			}
		}catch(Exception e){
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "RoleId not found", IterKeys.ERROR, groupName);
		}
		
		return listIds;
	}
	
	/**
	 * 
	 * @param item
	 * @param globalId
	 * @param groupName
	 * @return
	 */
	private long[] getUserGroupIds(Element item, String globalId, String groupName){
		
		List<String[]> paramList = getParamListByType(item, "usergroup");
		long[] listIds = new long[paramList.size()];
		
		try{			
			int numElem = 0;		
			for (String[] param : paramList) {	
				UserGroup userGroup = UserGroupLocalServiceUtil.getUserGroup(xmlIOContext.getCompanyId(), param[0]);
				listIds[numElem] = userGroup.getUserGroupId();
					
				numElem++;			
			}
		}catch(Exception e){
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "UserGroupId not found", IterKeys.ERROR, groupName);
		}
		
		return listIds;
	}

}
