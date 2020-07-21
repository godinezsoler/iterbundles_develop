/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.base.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.portlet.ActionRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.gson.JsonObject;
import com.liferay.cluster.ClusterMgr;
import com.liferay.cluster.IClusterMgr.ClusterMgrOperation;
import com.liferay.cluster.IClusterMgr.ClusterMgrType;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.cache.CacheRegistryUtil;
import com.liferay.portal.kernel.cache.MultiVMPoolUtil;
import com.liferay.portal.kernel.dao.shard.ShardUtil;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.SearchEngineUtil;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.GroupConfigTools;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.IterSecureConfigTools;
import com.liferay.portal.kernel.util.LayoutSetTools;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.MobileConfig;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.legacyurl.LegacyurlRulesTools;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.webcache.WebCachePoolUtil;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.LayoutSet;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.LayoutSetLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.PortletLocalServiceUtil;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.IterRulesUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.comparator.PortletLuceneComparator;
import com.liferay.portlet.asset.model.AssetCategory;
import com.liferay.portlet.asset.model.AssetCategoryProperty;
import com.liferay.portlet.asset.service.AssetCategoryLocalServiceUtil;
import com.liferay.portlet.asset.service.AssetCategoryPropertyLocalServiceUtil;
import com.liferay.portlet.asset.service.AssetVocabularyLocalServiceUtil;
import com.liferay.portlet.journal.util.GlobalJournalTemplateMgr;
import com.liferay.util.Encryptor;
import com.liferay.util.SystemProperties;
import com.liferay.util.survey.IterSurveyUtil;
import com.protecmedia.iter.base.model.Iter;
import com.protecmedia.iter.base.service.CommunitiesLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.base.IterLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterErrorKeys;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.SQLQueries;
import com.protecmedia.iter.base.util.IterAdminTools;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.model.LiveConfiguration;
import com.protecmedia.iter.xmlio.service.LiveConfigurationLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.util.CacheRefreshMgr;
import com.protecmedia.iter.xmlio.service.util.TomcatUtil;

/**
 * The implementation of the iter local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.IterLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.IterLocalServiceUtil} to access the iter local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.IterLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.IterLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class IterLocalServiceImpl extends IterLocalServiceBaseImpl {
	
	private static Log _log = LogFactoryUtil.getLog(IterLocalServiceImpl.class);
	
	
	private static final String INSERT_MOBILE_VERSION_CONF = new StringBuilder()
	.append("INSERT INTO group_config(groupId, mobileToken, mobileScript, lastPublicationDate) \n")
	.append("VALUES (%s,'%s', '%s', '%s' )  \n")
	.append("ON DUPLICATE KEY UPDATE mobileToken=VALUES(mobileToken), mobileScript=VALUES(mobileScript)").toString();
	
	private static final String GET_LEGACYURL_RULES 	= "SELECT * FROM legacyurlrules WHERE scopegroupid=%s ORDER BY ruleorder ASC";
	private static final String DELETE_LEGACYURL_RULES 	= "DELETE FROM legacyurlrules WHERE scopegroupid=%s";

	/*
	 * ADD Functions
	 */
	public Iter addIter(String name, String version, String environment) throws SystemException, PortalException {
		
		long id = counterLocalService.increment();
		
		Iter iter = iterPersistence.create(id);
		
		iter.setName(name);
		iter.setVersion(version);
		iter.setEnvironment(environment);
		
		iterPersistence.update(iter, false);
		
		return iter;
	}

	/*
	 * GET Functions
	 */	
	public Iter getLastIter() throws SystemException {		
		int count = iterPersistence.countAll();
		
		List<Iter> list = iterPersistence.findAll(count - 1, count);
		
		if (list != null && list.size() > 0) {
			return list.get(0);
		}
		
		return null;
	}
		
	public Iter getIterByVersion(String version) throws SystemException {
		return getLastIter();
	}
	
	public String getSystemInfo() throws SystemException, PortalException{
		return getSystemInfo(-1);
	}
	
	/**
	 * Obtains System General Info
	 */
	public String getSystemInfo(long userId) throws SystemException, PortalException {
		
		String strValue = "<iterwebcms><settings/></iterwebcms>";
		
		try
		{
			// ITER-860 Soporte para Protecmedia Cloud
			// http://jira.protecmedia.com:8080/browse/ITER-860?focusedCommentId=33267&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-33267
			userId = GroupMgr.getDefaultUserId();
			Document dom;
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			
			//get an instance of builder
			DocumentBuilder db = dbf.newDocumentBuilder();
			
			//create an instance of DOM
			dom = db.newDocument();
			
			//iterwebcms
			Element eleIter = dom.createElement("iterwebcms");
			Iter iter = getLastIter();
			if (iter != null)
				eleIter.setAttribute("version", iter.getVersion());
			
			// ITER-1064 Integración con Milenium: Proporcionar versión de ITER mediante servicio web
			eleIter.setAttribute("iterversion", IterGlobal.getIterWebCmsVersion());
			
			//settings
			Element eleSettings = dom.createElement("settings");
			eleSettings.setAttribute("urlmode", PropsValues.ITER_SEMANTICURL_ENABLED ? "semanticURL" : "friendlyURL");

			List<Company> companies = CompanyLocalServiceUtil.getCompanies();
			for (Company company : companies) 
			{
				//company-settings
				//-----------------
				Element eleCompany = dom.createElement("company-settings");
				
				//globalGroupId
				Element eleParam = dom.createElement("param");
				eleParam.setAttribute("id", "globalGroupId");
				eleParam.setAttribute("value", String.valueOf(company.getGroup().getGroupId()));
				eleCompany.appendChild(eleParam);
				
				//companyId
				eleParam = dom.createElement("param");
				eleParam.setAttribute("id", "companyId");
				eleParam.setAttribute("value", String.valueOf(company.getCompanyId()));
				eleCompany.appendChild(eleParam);
				
				//userId
				eleParam = dom.createElement("param");
				eleParam.setAttribute("id", "userId");
				eleParam.setAttribute("value", String.valueOf(userId));
				eleCompany.appendChild(eleParam);
				
				
				String userName = "";
				String userMail = "";
				try
				{
					User user = UserLocalServiceUtil.getUser(userId);
				
					if (user != null)
					{
						userName = user.getScreenName();
						userMail = user.getEmailAddress();
					}
				
				}
				catch(Exception err){}
				
				//userName
				eleParam = dom.createElement("param");
				eleParam.setAttribute("id", "userName");
				eleParam.setAttribute("value", userName);
				eleCompany.appendChild(eleParam);
				
				//userMail
				eleParam = dom.createElement("param");
				eleParam.setAttribute("id", "userMail");
				eleParam.setAttribute("value", userMail);
				eleCompany.appendChild(eleParam);
				
				//userAccessVocabularyId
				eleParam = dom.createElement("param");
				String userVocabularyId = "";
				try{
					userVocabularyId = String.valueOf(AssetVocabularyLocalServiceUtil.getGroupVocabulary(company.getGroup().getGroupId(), IterKeys.USER_ACCESS_LEVEL_VOCABULARY).getVocabularyId());
				}
				catch(Exception err){}
				eleParam.setAttribute("id", "UserAccessVocabularyId");
				eleParam.setAttribute("value", String.valueOf(userVocabularyId));
				eleCompany.appendChild(eleParam);
				
				//userAccessVocabularyPropertyName
				eleParam = dom.createElement("param");
				eleParam.setAttribute("id", "UserAccessVocabularyPropertyName");
				eleParam.setAttribute("value", IterKeys.USER_ACCESS_LEVEL_VALUE_PROPERTY);
				eleCompany.appendChild(eleParam);
				
				//Company Home URL
				eleParam = dom.createElement("param");
				eleParam.setAttribute("id", "homeUrl");
				eleParam.setAttribute("value", company.getHomeURL());
				eleCompany.appendChild(eleParam);

				//GatewayHost
				eleParam = dom.createElement("param");
				String gatewayHost = "";
				try
				{
					LiveConfiguration lc = LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(company.getCompanyId());
					gatewayHost = lc.getGatewayHost().equals("") ? lc.getRemoteIterServer2() : lc.getGatewayHost();
					
				}
				catch(Exception e){}
				
				eleParam = dom.createElement("param");
				eleParam.setAttribute("id", "gatewayHost");
				eleParam.setAttribute("value", gatewayHost);
				eleCompany.appendChild(eleParam);				
								
				eleSettings.appendChild(eleCompany);
				
				//group-settings
				//-----------------
				List<Live> liveGroups = LiveLocalServiceUtil.getLiveByClassNameValue(IterKeys.CLASSNAME_GROUP);
				long classNameId = ClassNameLocalServiceUtil.getClassName(IterKeys.CLASSNAME_GROUP).getClassNameId();
				
				for (Live liveGroup : liveGroups){
					
					try
					{
						Group group = GroupLocalServiceUtil.getGroup(liveGroup.getGroupId());
						
						if (group.getClassNameId() == classNameId && 
								!group.getName().equals(IterKeys.GUEST_GROUP_NAME) &&
								!group.getName().equals(IterKeys.CONTROL_PANEL_GROUP_NAME)){
							Element eleGroup = dom.createElement("group-settings");
							eleGroup.setAttribute("localId", String.valueOf(group.getGroupId()));
							eleGroup.setAttribute("name", group.getName());
											
							//virtualHost		
							String publicVirtualHost = "";
							try{
								publicVirtualHost = LayoutSetLocalServiceUtil.getLayoutSet(group.getGroupId(), false).getVirtualHost();
							}catch(Exception e){}
							eleParam = dom.createElement("param");
							eleParam.setAttribute("id", "virtualHost");
							eleParam.setAttribute("value", publicVirtualHost);
							eleGroup.appendChild(eleParam);
							
							//homeUrl
							eleParam = dom.createElement("param");
							eleParam.setAttribute("id", "homeUrl");
							eleParam.setAttribute("value", "/web" + group.getFriendlyURL());
							eleGroup.appendChild(eleParam);
							
							// Proporcionar a MLN la información del protocolo usado en el sitio web público 
							// http://jira.protecmedia.com:8080/browse/ITER-632 
							if (!PropsValues.IS_PREVIEW_ENVIRONMENT)
							{
								eleParam = dom.createElement("param");
								eleParam.setAttribute("id", "protocol");
								eleParam.setAttribute("value", IterSecureConfigTools.getConfiguredHTTPS(group.getGroupId()) ? Http.HTTPS : Http.HTTP);
								eleGroup.appendChild(eleParam);
							}
							
							//searchUrl
							String searchUrl = "";
							try
							{
								searchUrl = CommunitiesLocalServiceUtil.getCommunitiesByGroup(liveGroup.getGroupId()).getPrivateSearchUrl();
							}
							catch(Exception e){}
							eleParam = dom.createElement("param");
							eleParam.setAttribute("id", "searchUrl");
							eleParam.setAttribute("value", searchUrl);
							eleGroup.appendChild(eleParam);
							
							//urlSplitter
							String urlSplitter = "";
							try
							{
								if( PortalUtil.getCheckMappingWithPrefix() )
									urlSplitter +=  "/-";
								
								String newsMappingPrefix = PortalUtil.getNewsMappingPrefix();
								if( !newsMappingPrefix.equals("") )
									urlSplitter += newsMappingPrefix;
								else
									urlSplitter += "/";
									
							}
							catch(Exception e){}
							eleParam = dom.createElement("param");
							eleParam.setAttribute("id", "urlSplitter");
							eleParam.setAttribute("value", urlSplitter);
							eleGroup.appendChild(eleParam);
							
							eleSettings.appendChild(eleGroup);
							
							// Sistema de encuestas de Iter
							eleParam = dom.createElement("param");
							eleParam.setAttribute("id", "iterSurveys");
							eleParam.setAttribute("value", Boolean.toString(IterSurveyUtil.isEnabled(group.getGroupId())));
							eleGroup.appendChild(eleParam);
						}
						
					}
					catch(Exception e){}
					
				}
			}

			eleIter.appendChild(eleSettings);
			dom.appendChild(eleIter);
			
			DOMSource domSource = new DOMSource(dom);                
			StringWriter writer = new StringWriter();                
			StreamResult result = new StreamResult(writer);          
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();           
			transformer.setOutputProperty("encoding", "ISO-8859-1");
			transformer.transform(domSource, result);                
			
			strValue = writer.toString(); 			
					
		}
		catch(ParserConfigurationException pce)
		{
			_log.error("Error while trying to instantiate DocumentBuilder", pce);
		}
		catch(DOMException de)
		{
			_log.error("Error while trying to parse the log", de);
		}
		catch(TransformerException te)
		{
			_log.error("Error while trying to parse the log", te);
		}
		catch(Exception e)
		{
			_log.error("Error while trying to parse the log", e);
		}
		
		return strValue;
	}	
	
	
	public String getSystemInfoEncoded(long userId){
	
		String strValue = "<iterwebcms><settings/></iterwebcms>";
		
		try
		{
			strValue = getSystemInfo(userId);
			
			strValue = URLEncoder.encode(strValue, "UTF-8"); 
		}			
		catch(Exception e)
		{
			_log.error("Error while trying to parse the log", e);
		}
		
		return strValue;
	}
	
	public String getIterAdminInfo(HttpServletRequest req, HttpServletResponse res) throws SystemException
	{
		JsonObject response = new JsonObject();

		try
		{
			response.addProperty("userId", GroupMgr.getDefaultUserId());
			response.addProperty("environment", IterLocalServiceUtil.getEnvironment());
			response.addProperty("productVersion", IterKeys.PRODUCT_VERSION);
			response.addProperty("iterWebVersion", IterGlobal.getIterWebCmsVersion());
			response.addProperty("wsdlMlnWS", PropsValues.ITER_MLNWS_URL);
	
			String urlPortal = req.getRequestURL().toString();
			urlPortal = urlPortal.substring(0, urlPortal.indexOf(req.getContextPath()));
			response.addProperty("urlPortal", urlPortal);
	
			String headervalue = "";
			if( req.getHeader("ITER_GATEWAY") != null)
			 	headervalue = req.getHeader("ITER_GATEWAY"); 
			response.addProperty("iterGateWay", headervalue);
			
			List<Company> companies = CompanyLocalServiceUtil.getCompanies();
			if (companies.size() > 0)
			{
				Company company = companies.get(0);
				response.addProperty("companyId", company.getCompanyId());
				response.addProperty("globalGroupId", company.getGroup().getGroupId());
				
				LiveConfiguration liveConf = LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(company.getCompanyId());
				String gatewayHost = "";
				if(liveConf!=null)
					gatewayHost = liveConf.getGatewayHost();
				
				response.addProperty("gatewayHost", gatewayHost);
			}
		}
		catch (Throwable th)
		{
			throw new SystemException(th);
		}
		
		return response.toString();
	}
	
	/**
	 * GET Environment
	 */
	public String getEnvironment()
	{
		return PropsValues.ITER_ENVIRONMENT;
	}
	
	/**
	 * SET Environment
	 * @throws SystemException 
	 */
	public void setEnvironment(String environment) throws SystemException
	{
		try
		{
			Iter iter = getLastIter();
			iter.setEnvironment(environment);
			iterPersistence.update(iter, false);
		}
		catch(Exception e)
		{
			_log.error(e);
		}
	}
	
	/**
	 * GET PublicKey
	 */
	public String getPublicKey(){
		try{			
			return getLastIter().getPublicKey();
		}
		catch(Exception e){	
			return "";
		}
	}
	
	/**
	 * GET SystemName
	 */
	public String getSystemName(){
		String sysName = "";
		try{			
			sysName = getLastIter().getName();
		}
		catch(Exception e){			
		}
		return sysName;
	}
	
	/**
	 * GET CookieKey
	 */
	public String getCookieKey() {
		String cookieKey = "";
		try{		
			Iter iter = getLastIter();
			cookieKey = iter.getCookieKey();
			if (cookieKey.equals("")){
				cookieKey = Base64.objectToString(Encryptor.generateKey("DES"));
				iter.setCookieKey(cookieKey);
				iterPersistence.update(iter, false);
			}		
		}
		catch(Exception e){			
		}
		return cookieKey;
	}
	
	/**
	 * SET Version
	 * @throws SystemException 
	 */
	public void setProductVersion(String version) throws SystemException{
		Iter iter = getLastIter();
		iter.setVersion(version);
		iterPersistence.update(iter, false);
	}
	
	/**
	 * Funciones Auxiliares Globales
	 */
	public int getUserAccess(long userId){
		try{
			User user = UserLocalServiceUtil.getUser(userId);
			return getUserAccess(user);
		}
		catch(Exception err){
			_log.error("No user found with Id " + userId);
		}
		return -1;
	}
	
	
	/**
	 * @param user
	 * @return
	 */
	public int getUserAccess(User user){
		try{ 
			List<String> accessLevels = new ArrayList<String>();
			
			long globalGroupId = CompanyLocalServiceUtil.getCompany(user.getCompanyId()).getGroup().getGroupId();
			List<AssetCategory> categories = AssetCategoryLocalServiceUtil.getCategories(IterKeys.CLASSNAME_USER, user.getPrimaryKey());
			long vocabularyId = AssetVocabularyLocalServiceUtil.getGroupVocabulary(globalGroupId, IterKeys.USER_ACCESS_LEVEL_VOCABULARY).getVocabularyId();
			
			for (AssetCategory category : categories){
					if (category.getVocabularyId() == vocabularyId) {
						AssetCategoryProperty acp = AssetCategoryPropertyLocalServiceUtil.getCategoryProperty(category.getCategoryId(), IterKeys.USER_ACCESS_LEVEL_VALUE_PROPERTY);
						accessLevels.add(acp.getValue());
					}
			}

			//3. Recupera la que tiene menor nivel (menos control)
			Collections.sort(accessLevels);
			
			return (Integer.valueOf(accessLevels.get(0)));
		}
		catch(Exception err){
			return -1;
		}
	}

	/**
	 * Obtiene la propiedad del sistema indicada.
	 * Este método es útil para su uso desde ámbitos sin acceso a la clase SystemProperties
	 * @param propertyName
	 * @return
	 */
	public String getSystemProperty(String propertyName){
		return SystemProperties.get(propertyName);
	}
	
	public String getPortalProperty(String propertyName)
	{
		return PropsUtil.get(propertyName);
	}
	
	/**
	 * 
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public ThemeDisplay rebuildThemeDisplayForAjax(HttpServletRequest request) throws Exception
	{
		long scopeGroupId 					= ParamUtil.getLong(request, 	"scopeGroupId", 				0);
		long companyId 						= ParamUtil.getLong(request, 	"companyId", 					0);
		String languageId 					= ParamUtil.get(request, 		"languageId", 					"");
		long plid 							= ParamUtil.getLong(request, 	"plid", 						0);
		boolean secure 						= ParamUtil.getBoolean(request, "secure"						);
		long userId 						= ParamUtil.getLong(request, 	"userId", 						0);
		boolean lifecycleRender 			= ParamUtil.getBoolean(request, "lifecycleRender"				);
		String pathFriendlyURLPublic		= ParamUtil.get(request, 		"pathFriendlyURLPublic", 		"");
		String pathFriendlyURLPrivateUser 	= ParamUtil.get(request, 		"pathFriendlyURLPrivateUser",	"");
		String pathFriendlyURLPrivateGroup 	= ParamUtil.get(request, 		"pathFriendlyURLPrivateGroup", 	"");
		String serverName 					= ParamUtil.get(request, 		"serverName", 					"");
		String cdnHost 						= ParamUtil.get(request, 		"cdnHost", 						"");
		String pathImage 					= ParamUtil.get(request, 		"pathImage", 					"");
		String pathMain 					= ParamUtil.get(request, 		"pathMain", 					"");
		String pathContext 					= ParamUtil.get(request, 		"pathContext", 					"");
		String urlPortal 					= ParamUtil.get(request, 		"urlPortal", 					"");
		String pathThemeImages 				= ParamUtil.get(request, 		"pathThemeImages", 				"");
		
		ThemeDisplay themeDisplay = rebuildThemeDisplayForAjax(scopeGroupId, companyId, languageId, 
				plid, secure, userId, lifecycleRender, 
				pathFriendlyURLPublic, pathFriendlyURLPrivateUser,
				pathFriendlyURLPrivateGroup, serverName, cdnHost, 
				pathImage, pathMain, pathContext, urlPortal,
				pathThemeImages, request);

		return themeDisplay;
	}
	
	/**
	 * 
	 * @param scopeGroupId
	 * @param companyId
	 * @param languageId
	 * @param plid
	 * @param secure
	 * @param userId
	 * @param lifecycleRender
	 * @param pathFriendlyURLPublic
	 * @param pathFriendlyURLPrivateUser
	 * @param pathFriendlyURLPrivateGroup
	 * @param serverName
	 * @param cdnHost
	 * @param pathImage
	 * @param pathMain
	 * @param pathContext
	 * @param urlPortal
	 * @param pathThemeImages
	 * @return
	 * @throws Exception
	 */
	public ThemeDisplay rebuildThemeDisplayForAjax(long scopeGroupId, long companyId, String languageId,
			long plid, boolean secure, long userId, boolean lifecycleRender, String pathFriendlyURLPublic,
			String pathFriendlyURLPrivateUser, String pathFriendlyURLPrivateGroup, String serverName,
			String cdnHost, String pathImage, String pathMain, String pathContext, String urlPortal,
			String pathThemeImages, HttpServletRequest request) throws Exception{
		ThemeDisplay themeDisplay = new ThemeDisplay();
		
		themeDisplay.setScopeGroupId(scopeGroupId);
		
		Company company = CompanyLocalServiceUtil.getCompany(companyId);
		themeDisplay.setCompany(company);
		
		themeDisplay.setLanguageId(languageId);
		
		themeDisplay.setPlid(plid);
		
		themeDisplay.setLayout( (plid > 0) ? LayoutLocalServiceUtil.getLayout(plid) : null );
		
		themeDisplay.setSecure(secure);
		
		User user = UserLocalServiceUtil.getUser(userId);
		themeDisplay.setUser(user);
		themeDisplay.setRealUser(user);
		
		TimeZone timeZone = user.getTimeZone();
		if (timeZone == null) 
			timeZone = company.getTimeZone();
		themeDisplay.setTimeZone(timeZone);
		
		themeDisplay.setSignedIn(!user.isDefaultUser());
		
		PermissionChecker permissionChecker = PermissionCheckerFactoryUtil.create(user, false);
		themeDisplay.setPermissionChecker(permissionChecker);
		
		themeDisplay.setLifecycleRender(lifecycleRender);
		
		themeDisplay.setPathFriendlyURLPublic(pathFriendlyURLPublic);
		
		themeDisplay.setPathFriendlyURLPrivateUser(pathFriendlyURLPrivateUser);
		
		themeDisplay.setPathFriendlyURLPrivateGroup(pathFriendlyURLPrivateGroup);
		
		themeDisplay.setServerName(serverName);
		
		themeDisplay.setCDNHost(cdnHost);
		
		themeDisplay.setPathImage(pathImage);
		
		themeDisplay.setPathMain(pathMain);
		
		themeDisplay.setPathContext(pathContext);
		
		themeDisplay.setURLPortal(urlPortal);
		
		themeDisplay.setPathThemeImages(pathThemeImages);
		
		themeDisplay.setRequest(request);
		
		themeDisplay.setServerPort(request.getServerPort());
		
		request.setAttribute(WebKeys.THEME_DISPLAY, themeDisplay);
		
		PortalUtil.setVirtualHostLayoutSet(request);
		
		IterRequest.setOriginalRequest(request);
		
		IterRequest.setAttribute(WebKeys.IS_MOBILE_REQUEST, String.valueOf(ParamUtil.getLong(request, "isMobileRequest", 0)) );
		
		return themeDisplay;
	}

	public String getSWF(String app)
	{
		StringBuilder sb = new StringBuilder(app);
		String ext = ".swf";
		//Eleccion entre swf de release o debug (Por defecto el de release)
		String swf_properties = PortalUtil.getPortalProperties().getProperty(IterKeys.PORTAL_PROPERTIES_KEY_ITER_FLEX_PORTLETS_DEBUG);
		
		File webappsDir = new File(PortalUtil.getPortalWebDir()).getParentFile();
		
		if ( (swf_properties != null) && (swf_properties.equals("true")) )
			ext = "-debug.swf";
				
		StringBuilder swfAppPath = new StringBuilder( webappsDir.getAbsolutePath() );
		swfAppPath.append(app).append(ext);
		File swfAppFile = new File( swfAppPath.toString() );
		
		return swfAppFile.exists() ? sb.append(ext).append("?t=").append(swfAppFile.lastModified()).toString() : "";
	}
	
	/** Get VirtualHost 
	 * @throws Exception */
	public String getVirtualHostIterAdmin(String groupId/*, boolean refresh*/) throws Exception
	{
		long groupid = Long.parseLong(groupId);

		String result = "</config>";
		com.liferay.portal.kernel.xml.Element data = SAXReaderUtil.createElement("config");
		//virtualHost
		LayoutSet ls = LayoutSetLocalServiceUtil.getLayoutSet(groupid, false);
		
		data.addAttribute("publicVirtualHost", ls.getVirtualHost());
//		data.addAttribute("vitualHostAlias", ls.getSettings().replaceAll(StringPool.SECTION, StringPool.NEW_LINE));
		UnicodeProperties settings = ls.getSettingsProperties();
		data.addAttribute("vitualHostAlias", settings.getProperty(LayoutSetTools.SERVER_ALIAS, StringPool.BLANK).replaceAll(StringPool.SECTION, StringPool.NEW_LINE));
		
		//friendlyURL
		data.addAttribute("friendlyURL", GroupLocalServiceUtil.getGroup(groupid).getFriendlyURL());

		data.addAttribute("staticServer", settings.getProperty(LayoutSetTools.STATIC_SERVERS, StringPool.BLANK).replaceAll(StringPool.SECTION, StringPool.NEW_LINE));
		
		data.addAttribute("lang", GroupConfigTools.getGroupConfigField(groupid, "lang"));
		
		// Confioguración https (Sólo en el LIVE)
		if (IterKeys.ENVIRONMENT_LIVE.equals(PropsValues.ITER_ENVIRONMENT))
		{
			String secureconfig = GroupConfigTools.getGroupConfigField(groupid, "secureconfig");
			if (secureconfig != null)
			{
				try
				{
					com.liferay.portal.kernel.xml.Document securedoc = SAXReaderUtil.read(secureconfig);
					data.add(securedoc.getRootElement().detach());
				}
				catch (Throwable th)
				{
					_log.error(String.format("Invalid secure config for group %s", groupId));
				}
			}
		}
		
		result = data.asXML();
		
//		if(refresh)
//		{
//			LayoutSetTools.initServerSettings(groupId);
//		}
		
		return result;
	}
	
	public String saveConfigIterAdmin(String _groupid, String friendlyURL, String publicVirtualHost, String serverAlias, String staticsServers, String lang, String useSecureConnection) throws Exception
	{
		String _friendlyURL = friendlyURL;
		long liveGroupId = Long.parseLong(_groupid);

		IterAdminTools iat = new IterAdminTools(liveGroupId, friendlyURL, publicVirtualHost, serverAlias, staticsServers);
		iat.saveConfigIterAdmin();
		
		MultiVMPoolUtil.clear();
		
		LayoutSetTools.initServerSettings(_groupid);

		_friendlyURL = GroupLocalServiceUtil.getGroup(liveGroupId).getFriendlyURL();	
		
		if (Validator.isNotNull(lang))
			GroupConfigTools.setGroupConfigField(liveGroupId, "lang", lang);
		
		// Confioguración https (Sólo en el LIVE)
		if (IterKeys.ENVIRONMENT_LIVE.equals(PropsValues.ITER_ENVIRONMENT) && Validator.isNotNull(useSecureConnection))
		{
			if (IterSecureConfigTools.updateSecureConfig(liveGroupId, useSecureConnection))
			{
				// Invalida la caché de los Tomcat
				CacheRefreshMgr.refreshTomcat();
				// Invalida la caché de los Apache
				TomcatUtil.invalidate(liveGroupId);
				// Añade o elimina la regla de redirección HTTPS
				IterRulesUtil.regenerateApacheIterRules(liveGroupId);
			}
		}
		
		return _friendlyURL;
	}
	
	/*Métodos para consulta y guardado de datos de configuración Miscelánea/Versión movil de IterAdmin  */
	public String setMobileVersionConf(String xmlData) throws Exception
	{
		com.liferay.portal.kernel.xml.Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		
		XPath xpath = SAXReaderUtil.createXPath("//row");
		Node node = xpath.selectSingleNode(dataRoot);
		
		long groupid = XMLHelper.getLongValueOf(node, "@groupId");
		ErrorRaiser.throwIfFalse(groupid > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String mobiletoken = XMLHelper.getStringValueOf(node, "@mobileToken");
		
		String mobilescript = "";
		String mobilescriptEncoded = "";
		if( Validator.isNotNull(mobiletoken) )
		{
			/*Hay versión móvil para el grupo, se recupera el valor del script para guardarlo en base de datos(escapado)
			y para actualizar el mapa _mobileTokenMap de IterGlobal(sin escapar)*/
			mobilescript = XMLHelper.getStringValueOf(node, "mobileScript");

			if (Validator.isNotNull(mobilescript))
			{
				mobilescriptEncoded = Base64.encode(mobilescript.getBytes());
			}
		 }
	
		PortalLocalServiceUtil.executeUpdateQuery(String.format(INSERT_MOBILE_VERSION_CONF, groupid,mobiletoken, mobilescriptEncoded, SQLQueries.getCurrentDate()));
		
		IterGlobal.setMobileConfig(groupid, mobiletoken, mobilescript);

		// ITER-936 Error 500 cuando se pone un script PHP personalizado para móviles
		return getMobileVersionConf( String.valueOf(groupid) ).asXML();
	}
	
	public com.liferay.portal.kernel.xml.Document getMobileVersionConf(String groupId) throws Exception
	{
		
		com.liferay.portal.kernel.xml.Document dom 	= SAXReaderUtil.createDocument();
		
		//se obtienen datos necesarios, para formar el xml que se retorna, de la estructura de datos creada en memoria al arrancar el sistema( IterGlobal.java )
		long groupid = Long.parseLong(groupId);
		
		MobileConfig mobileConf = IterGlobal.getMobileConfig(groupid);
		
		String token  = mobileConf.getMobileToken();
		String script = mobileConf.getMobileScript();
		
		//se crea el xml que se retorna con la configuración de la versión móvil del grupo
		com.liferay.portal.kernel.xml.Element eleRS	= dom.addElement(XMLHelper.rsTagName);
		com.liferay.portal.kernel.xml.Element elem = eleRS.addElement(XMLHelper.rowTagName);
		elem.addAttribute("mobileToken", token);
		
		com.liferay.portal.kernel.xml.Element elemScript = SAXReaderUtil.createElement("mobileScript");
		elemScript.addCDATA(script);
		
		elem.add(elemScript);
		
		
		return dom;
		
	}
	
	public void publishToLiveMobileVersionConf(String groupId) throws Exception
	{
		
		if (getEnvironment().equals(IterKeys.ENVIRONMENT_PREVIEW)) 
		{
//			long companyId = GroupLocalServiceUtil.getGroup(GroupMgr.getGlobalGroupId()).getCompanyId();
			
//			LiveConfiguration liveConf = LiveConfigurationLocalServiceUtil.getLiveConfigurationByCompanyId(companyId);
//			String remoteIP = liveConf.getRemoteIterServer().split(":")[0];
//			int remotePort = Integer.valueOf(liveConf.getRemoteIterServer().split(":")[1]);
//			String remoteMethodPath = "/base-portlet/secure/json";
			
			//se obtiene la configuración para el groupId
			com.liferay.portal.kernel.xml.Document mobileVersionConfDom = getMobileVersionConf(groupId);
			
			//Recupera el nombre del grupo e inserta el groupName para la publicación.
			Group group = GroupLocalServiceUtil.getGroup(Long.valueOf(groupId));
			String groupName = group.getName();
			mobileVersionConfDom.getRootElement().addAttribute("groupName", groupName);
			
			String mobileVersionConf = mobileVersionConfDom.asXML();
			
			//se llama al método importData del LIVE
			List<NameValuePair> remoteMethodParams = new ArrayList<NameValuePair>();
			remoteMethodParams.add(new BasicNameValuePair("serviceClassName",	"com.protecmedia.iter.base.service.IterServiceUtil"));
			remoteMethodParams.add(new BasicNameValuePair("serviceMethodName",	"importData"));
			remoteMethodParams.add(new BasicNameValuePair("serviceParameters",	"[mobileVersionConf]"));
			remoteMethodParams.add(new BasicNameValuePair("mobileVersionConf",	mobileVersionConf));
			
			
			JSONUtil.executeMethod("/c/portal/json_service", remoteMethodParams);
			
		}
	}
	
	public com.liferay.portal.kernel.xml.Document exportData(Long groupId) throws DocumentException, Exception
	{
		ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		// Recupera los datos a exportar.
		com.liferay.portal.kernel.xml.Document dom = getMobileVersionConf(String.valueOf(groupId));

		return dom;
	}
	
	public void importData(String data) throws Exception
	{
		com.liferay.portal.kernel.xml.Document dom = SAXReaderUtil.read(data);
        // Busca primero el groupIid en el xml, y si no existe, se busca por groupName
        long groupId = XMLHelper.getLongValueOf(dom, "/rs/@groupId");
        if (groupId <= 0)
        	groupId = GroupLocalServiceUtil.getGroup(GroupMgr.getCompanyId(), XMLHelper.getStringValueOf(dom, "/rs/@groupName")).getGroupId();
        ErrorRaiser.throwIfFalse(groupId > 0, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
        
        // Inserta el groupId
        dom.getRootElement().element("row").addAttribute("groupId", String.valueOf(groupId));
	
		// Inserta / Actualiza el registro
		setMobileVersionConf(dom.asXML());
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////
	public String getSystemProperties(HttpServletRequest request) throws Exception
	{
		Map<Object,Object> propertiesMap = new TreeMap<Object,Object>();
		
		propertiesMap.putAll( System.getProperties());
		propertiesMap.putAll( PropsUtil.getProperties());
		
		com.liferay.portal.kernel.xml.Element data = SAXReaderUtil.createElement("SystemProperties");
		for (Entry<Object, Object> entry: propertiesMap.entrySet())
		{
			String property = String.valueOf(entry.getKey());
			String value    = String.valueOf(entry.getValue());
			
			com.liferay.portal.kernel.xml.Element row = data.addElement("row");
			row.addAttribute("property", property);
			row.addAttribute("value", value);
		}
	
		return data.asXML();
	}
	
	public String getTraceLevels(HttpServletRequest request) throws Exception
	{
		String result = "</TraceLevels>";
		com.liferay.portal.kernel.xml.Element data = SAXReaderUtil.createElement("TraceLevels");
		
		List<?> results = LogFactoryUtil.getCurrentLoggers();
		
		for (int i = 0; i < results.size(); i++) {
			Map.Entry entry = (Map.Entry)results.get(i);
			String name = (String)entry.getKey();
			String level = (String)entry.getValue();
			
			com.liferay.portal.kernel.xml.Element row = data.addElement("row");
			row.addAttribute("name", name);
			row.addAttribute("level", level);
		}
		result = data.asXML();
		return result;
	}
	
	public String updateLogLevels(String xmlData, ActionRequest actionRequest)
			throws DocumentException, IOException, SQLException, SecurityException, NoSuchMethodException, com.liferay.portal.kernel.error.ServiceError 
	{

		com.liferay.portal.kernel.xml.Element dataRoot = SAXReaderUtil.read(xmlData).getRootElement();
		XPath xpath = SAXReaderUtil.createXPath("//row");
		List<Node> LogLevelsNodes = xpath.selectNodes(dataRoot);
		//Node node = xpath.selectSingleNode(dataRoot);
		
//		List<?> results = LogFactoryUtil.getCurrentLoggers();

		for (Node node : LogLevelsNodes) {			
			String _name = XMLHelper.getTextValueOf(node, "@name");
			ErrorRaiser.throwIfNull(_name, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			_name = StringEscapeUtils.escapeSql(_name);
				
			String _level = XMLHelper.getTextValueOf(node, "@level");
			ErrorRaiser.throwIfNull(_level, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
			_level = StringEscapeUtils.escapeSql(_level);
	
			LogFactoryUtil.setLevel(_name, _level);
		}

		return xmlData;
	}
	
	public String cacheSingle(HttpServletRequest request) throws Exception {
		WebCachePoolUtil.clear();
		return "true";
	}
	
	public String cacheMulti(HttpServletRequest request) throws Exception {
		MultiVMPoolUtil.clear();
		return "true";
	}
	
	public String cacheDb(HttpServletRequest request) throws Exception {
		CacheRegistryUtil.clear();
		return "true";
	}
	
	public String reindex(HttpServletRequest request) throws Exception 
	{
		List<Company> companies = CompanyLocalServiceUtil.getCompanies();

		for (Company company : companies)
		{
			try {
				long companyId = company.getCompanyId();
				
				ShardUtil.pushCompanyService(companyId);
				try {
						doReIndex(companyId);
				}
				finally{
						ShardUtil.popCompanyService();
				}
			} catch (Exception e) {
					_log.error(e, e);
			}
		}
		return "true";
	}

	public void doReIndex (long companyId) 
	{	
		if (SearchEngineUtil.isIndexReadOnly())
			return;

		if (_log.isInfoEnabled())
			_log.info("Reindexing started");
		
		StopWatch stopWatch1 = null;
		if (_log.isInfoEnabled()) 
		{
			stopWatch1 = new StopWatch();
			stopWatch1.start();
		}
		try 
		{
			List<Portlet> portlets = PortletLocalServiceUtil.getPortlets(companyId);
			portlets = ListUtil.sort(portlets, new PortletLuceneComparator());

			for (Portlet portlet : portlets) 
			{
				if (!portlet.isActive())
					continue;

				Indexer indexer = portlet.getIndexerInstance();

				if (indexer == null)
					continue;

				indexer.reindex(new String[] {String.valueOf(companyId)});
				
			}
			if (_log.isInfoEnabled()) 
				_log.info("Reindexing completed in " +(stopWatch1.getTime() / Time.SECOND) + " seconds");
			
		}catch (Exception e){
			_log.error("Error encountered while reindexing", e);

			if (_log.isInfoEnabled())
				_log.info("Reindexing failed");
			
		}
	}

	public com.liferay.portal.kernel.xml.Element getLegacyUrlRules(String scopeGroupId, boolean refresh) throws Exception
	{
		String query = String.format(GET_LEGACYURL_RULES, scopeGroupId);
		
		if(refresh)
		{
			LegacyurlRulesTools.initLegacyurlRules(scopeGroupId);
		}
		
		List<Map<String,Object>> result = PortalLocalServiceUtil.executeQueryAsMap(query);
		
		com.liferay.portal.kernel.xml.Element rootElem = SAXReaderUtil.createElement("legacyurlrules");
		rootElem.addAttribute("scopegroupid", scopeGroupId);
		for(Map<String,Object> row : result)
		{
			com.liferay.portal.kernel.xml.Element ruleElem = rootElem.addElement("rule");
			ruleElem.addAttribute( "order", String.valueOf(row.get("ruleorder")) );
			
			com.liferay.portal.kernel.xml.Element regexprElem = ruleElem.addElement("regexpr");
			regexprElem.addCDATA( String.valueOf(row.get("regexpr")) );
			
			com.liferay.portal.kernel.xml.Element subexprElem = ruleElem.addElement("subexpr");
			subexprElem.addCDATA( String.valueOf(row.get("subexpr")) );
		}
		
		
		return rootElem;
	}
	
	public void setLegacyUrlRules(String xml) throws Exception
	{
		com.liferay.portal.kernel.xml.Document dom = SAXReaderUtil.read(xml);
		
		ClusterMgr.processConfig(ClusterMgrType.LEGAYURL_RULE, ClusterMgrOperation.SET, dom);
		
		String scopeGroupId = XMLHelper.getTextValueOf(dom.getRootElement(), "@scopegroupid");
		
		String value = "(%s, %s, '%s', '%s')"; //(scopegroupid, ruleorder, regexpr, subexpr)
		List<String> valuesList = new ArrayList<String>();
		List<Node> rules = dom.selectNodes("/legacyurlrules/rule");
		for(Node rule : rules)
		{
			String regularExpr = XMLHelper.getTextValueOf(rule, "regexpr");
			ErrorRaiser.throwIfFalse(Validator.isNotNull(regularExpr), com.liferay.portal.kernel.error.IterErrorKeys.XYZ_ITR_E_EMPTY_REG_EXPR_4_LEGACYURL_ZYX);
			try
			{
				Pattern.compile(regularExpr, Pattern.CASE_INSENSITIVE);
			}
			catch (PatternSyntaxException pse)
			{
				_log.error(pse);
				ErrorRaiser.throwIfError(com.liferay.portal.kernel.error.IterErrorKeys.XYZ_ITR_E_INVALID_REG_EXPR_4_LEGACYURL_ZYX, regularExpr);
			}
			
			String rowValue = String.format(value, 	scopeGroupId, 
													XMLHelper.getStringValueOf(rule, "@order"), 
													StringEscapeUtils.escapeSql( regularExpr ),
													StringEscapeUtils.escapeSql( XMLHelper.getTextValueOf(rule, "subexpr") )
											);
			valuesList.add( rowValue );
		}
		
		String query = String.format(DELETE_LEGACYURL_RULES, scopeGroupId);
		
		PortalLocalServiceUtil.executeUpdateQuery(query);
		
		if( Validator.isNotNull(valuesList) )
		{
			query = String.format(LegacyurlRulesTools.SET_LEGACYURL_RULES, StringUtil.merge(valuesList, StringPool.COMMA_AND_SPACE));
			
			// Escapa las barras invertidas
			query = query.replace(StringPool.BACK_SLASH, StringPool.DOUBLE_BACK_SLASH);
		
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
		
		LegacyurlRulesTools.initLegacyurlRules(scopeGroupId);
	}
	
	public String getGlobalJournalTemplates() throws SecurityException, NoSuchMethodException
	{
		return GlobalJournalTemplateMgr.getXsl();
	}
	
}
