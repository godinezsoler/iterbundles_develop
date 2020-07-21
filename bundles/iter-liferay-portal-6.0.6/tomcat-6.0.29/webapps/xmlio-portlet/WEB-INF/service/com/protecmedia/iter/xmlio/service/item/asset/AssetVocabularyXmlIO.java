package com.protecmedia.iter.xmlio.service.item.asset;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.liferay.portal.kernel.error.IterErrorKeys;
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
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XPath;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portlet.asset.model.AssetVocabulary;
import com.liferay.portlet.asset.model.AssetVocabularyModel;
import com.liferay.portlet.asset.service.AssetVocabularyLocalServiceUtil;
import com.protecmedia.iter.base.service.IterLocalServiceUtil;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.util.CDATAUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;

/* XML Structure:
 * 
<item operation="create" globalid="DiariTerrassaPre_151920" classname="com.liferay.portlet.asset.model.AssetVocabulary" groupid="The Star">
	<param name="settings">&lt;![CDATA[]]&gt;</param>
	<param name="ca_ES" type="title">&lt;![CDATA[voc2]]&gt;</param>
	<param name="ca_ES" type="description">&lt;![CDATA[]]&gt;</param>
	<param name="es_ES" type="title">&lt;![CDATA[voc2]]&gt;</param>
	<param name="es_ES" type="description">&lt;![CDATA[]]&gt;</param>
	<param name="en_US" type="title">&lt;![CDATA[voc2]]&gt;</param>
	<param name="en_US" type="description">&lt;![CDATA[]]&gt;</param>
	<param name="fr_FR" type="title">&lt;![CDATA[voc2]]&gt;</param>
	<param name="fr_FR" type="description">&lt;![CDATA[]]&gt;</param>
	<param name="de_DE" type="title">&lt;![CDATA[voc2]]&gt;</param>
	<param name="de_DE" type="description">&lt;![CDATA[]]&gt;</param>
	<param name="pt_PT" type="title">&lt;![CDATA[voc2]]&gt;</param>
	<param name="pt_PT" type="description">&lt;![CDATA[]]&gt;</param>
	<param name="pl_PL" type="title">&lt;![CDATA[voc2]]&gt;</param>
	<param name="pl_PL" type="description">&lt;![CDATA[]]&gt;</param>
	<param name="DiariTerrassaPre_151924" type="dependency">
		<param name="classname">&lt;![CDATA[com.liferay.portlet.asset.model.AssetCategory]]&gt;</param>
		<param name="groupname">&lt;![CDATA[The Star]]&gt;</param>
	</param>
	<param name="DiariTerrassaPre_151926" type="dependency">
		<param name="classname">&lt;![CDATA[com.liferay.portlet.asset.model.AssetCategory]]&gt;</param>
		<param name="groupname">&lt;![CDATA[The Star]]&gt;</param>
	</param>
</item>
*/


public class AssetVocabularyXmlIO extends ItemXmlIO {

	private static Log _log = LogFactoryUtil.getLog(AssetVocabularyXmlIO.class);
	private String _className = IterKeys.CLASSNAME_VOCABULARY;
	private static final String isCategoryPublication = "iscategorypublication";

	public AssetVocabularyXmlIO() {
		super();
	}
	
	public AssetVocabularyXmlIO(XMLIOContext xmlIOContext) {
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
		
		try{
			long globalGroupId = CompanyLocalServiceUtil.getCompany(companyId).getGroup().getGroupId();
			
			List<AssetVocabulary> globalAssetVocabularies = AssetVocabularyLocalServiceUtil.getGroupVocabularies(globalGroupId);
			for (AssetVocabulary globalAssetVocabulary : globalAssetVocabularies){				
				try {
					createLiveEntry(globalAssetVocabulary);
				} catch (PortalException e) {
					_log.error("Can't add Live, Vocabulary: " + globalAssetVocabulary.getVocabularyId());
				}
			}
		} catch (PortalException e) {
			_log.error("Can't add Live, Vocabularies from Global group");
		}
		
		try{
			List<AssetVocabulary> assetVocabularies = AssetVocabularyLocalServiceUtil.getGroupVocabularies(groupId);
			for (AssetVocabulary assetVocabulary : assetVocabularies){				
				try {
					createLiveEntry(assetVocabulary);
				} catch (PortalException e) {
					_log.error("Can't add Live, Vocabulary: " + assetVocabulary.getVocabularyId());
				}
			}
		} catch (PortalException e) {
			_log.error("Can't add Live, Vocabularies from current group");
		}
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		AssetVocabulary vocabulary = (AssetVocabulary)model;
		String id = String.valueOf(vocabulary.getVocabularyId());
		//insert element in LIVE
		LiveLocalServiceUtil.add(_className, vocabulary.getGroupId(), -1, 0, 
				IterLocalServiceUtil.getSystemName() + "_" + id, id, 
				IterKeys.CREATE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
	}
	@Override
	public void deleteLiveEntry(BaseModel<?> model) throws PortalException, SystemException{
		
		AssetVocabulary vocabulary = (AssetVocabulary)model;
		String id = String.valueOf(vocabulary.getVocabularyId());
		//insert element in LIVE
		LiveLocalServiceUtil.add(_className, vocabulary.getGroupId(), -1, 0, 
				IterLocalServiceUtil.getSystemName() + "_" + id, id, 
				IterKeys.DELETE, IterKeys.PENDING, new Date(), IterKeys.ENVIRONMENT_PREVIEW);
		
	}
	
	/*
	 * Export function
	 */	
	@Override
	protected String createItemXML(XMLIOExport xmlioExport, Element root, String operation, Group group, Live live)
	{
			
		String error = "";
		
		boolean categoryPublication = xmlIOContext.getAssetCategoryId()!=null;
		XPath xpath = SAXReaderUtil.createXPath("count(item)");
		
		/**
		 * 	Luis Miguel
		 * 
		 *    La publicación de categorias se hace mediante su vocabulario.
		 *    Sabemos si vamos a publicar una categoria consultando el valor de xmlIOContext.getAssetCategoryId(), que se relenó en la función exportLiveContents del LiveLocalServiceImpl.
		 *    Puede ser que la categoria ya esté publicada, en cuyo caso solo nos llevaríamos al LIVE el vocabulario, del que no se van importar sus cambios porque lo que queremos 
		 *  publicar es la categoria.
		 *    Al no enviar ninguna categoria al LIVE y no importar los cambios del vocabulario, en el PREVIEW cuando se recibe y comprueba la respuesta del LIVE en el método checkLiveResponse
		 *  del LiveLocalServiceImpl se lanza el error IterErrorKeys.XYZ_E_PUBRESPONSE_STR_ERROR_ZYX, por la condicion 3.b. Es un XML "correcto" pero NO tiene items.
		 *    Lo que se consigue aqui es publicar el vocabulario solo cuando lo que se pretende publicar es el propio vocabulario o alguna categoria PENDIENTE de publicar. Si se intenta
		 *  publicar una categoria que no tiene cambios pendiente de enviar al LIVE se generará el error IterErrorKeys.XYZ_E_XPORTCONTENT_EMPTY_ZYX.
		 */
		if( !categoryPublication || xpath.numberValueOf(root).intValue()>0 )
		{
			Map<String, String> attributes = new HashMap<String, String>();
			Map<String, String> params = new HashMap<String, String>();
			setCommonAttributes(attributes, group.getName(), live, operation);
			attributes.put(isCategoryPublication, String.valueOf(categoryPublication) );
			
			try{
						
				if (operation.equals(IterKeys.CREATE)){	
					AssetVocabulary voc = AssetVocabularyLocalServiceUtil.getAssetVocabulary(GetterUtil.getLong(live.getLocalId()));
					
					params.put("settings", voc.getSettings());		
					
					Element itemElement = addNode(root, "item", attributes, params);	
					
					Locale[] languages = LanguageUtil.getAvailableLocales();
					for (Locale language : languages){					
						//Title
						Map<String, String> titleAttributes = new HashMap<String, String>();
						
						titleAttributes.put("type", "title");
						titleAttributes.put("name", LocaleUtil.toLanguageId(language));
						
						addNode(itemElement, "param", titleAttributes, voc.getTitle(language));					
					
						//Description
						Map<String, String> descAttributes = new HashMap<String, String>();
						
						descAttributes.put("type", "description");
						descAttributes.put("name", LocaleUtil.toLanguageId(language));
						
						addNode(itemElement, "param", descAttributes, voc.getDescription(language));
						
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
			
			_log.debug("INFO: Vocabulary exported");	
		}
		
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
					AssetVocabulary voc = AssetVocabularyLocalServiceUtil.getAssetVocabulary(GetterUtil.getLong(live.getLocalId()));
					
					try
					{
						AssetVocabularyLocalServiceUtil.deleteVocabulary(voc);
						
						//update entry in live table
						LiveLocalServiceUtil.add(_className, groupId, globalId, live.getLocalId(), IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
										
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
						LiveLocalServiceUtil.add(_className, groupId, globalId, live.getLocalId(),IterKeys.DELETE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
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
		String sGroupId = getAttribute(item, "groupid");
		String globalId = getAttribute(item, "globalid");
		
		if( !GetterUtil.getBoolean(getAttribute(item, isCategoryPublication), false) )
		{
			String settings = getParamTextByName(item, "settings");
			
			try
			{
				long groupId = getGroupId(sGroupId);
				
				try 
				{
					Live live = LiveLocalServiceUtil.getLiveByGlobalId(groupId, _className, globalId);
								
					try 
					{
						AssetVocabulary vocabulary = AssetVocabularyLocalServiceUtil.getAssetVocabulary(GetterUtil.getLong(live.getLocalId()));
						
						String name = XMLHelper.getTextValueOf( 
																	item, 
																	String.format("param[@name='%s']", LocaleUtil.getDefault()) 
																);
						
						name = CDATAUtil.strip(name);
						
						if( !name.equalsIgnoreCase(vocabulary.getName()) || !settings.equalsIgnoreCase(vocabulary.getSettings()) )
						{
							ServiceContext serviceContext = new ServiceContext();
							//Sirven para los diferentes idiomas	
							HashMap<Locale,String> localeTitlesMap = new HashMap<Locale, String>();
							HashMap<Locale,String> localeDescriptionsMap = new HashMap<Locale, String>();
							
							initializeParams(serviceContext, groupId, localeTitlesMap, localeDescriptionsMap, item);
	
							AssetVocabularyLocalServiceUtil.updateVocabulary(vocabulary.getVocabularyId(), localeTitlesMap, localeDescriptionsMap, settings, serviceContext);
						}
						
						updateLiveTable(groupId, globalId, sGroupId, vocabulary, item, doc);
						
	//					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicated element", IterKeys.DONE, sGroupId);
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
							
							try 
							{
								ServiceContext serviceContext = new ServiceContext();
								//Sirven para los diferentes idiomas	
								HashMap<Locale,String> localeTitlesMap = new HashMap<Locale, String>();
								HashMap<Locale,String> localeDescriptionsMap = new HashMap<Locale, String>();
								
								initializeParams(serviceContext, groupId, localeTitlesMap, localeDescriptionsMap, item);
								
								AssetVocabulary vocabulary = AssetVocabularyLocalServiceUtil.addVocabulary(xmlIOContext.getUserId(), localeTitlesMap, localeDescriptionsMap, settings, serviceContext);
								
								updateLiveTable(groupId, globalId, sGroupId, vocabulary, item, doc);
								
							} 
							catch (Exception e1) 
							{
								xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error: " + e1.toString(), IterKeys.ERROR, sGroupId);
							}	
						}
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
		else
		{
			try
			{
				evaluateDependencies(item, doc);
			}
			catch (DocumentException e)
			{
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create categories", IterKeys.INTERRUPT, sGroupId);
			}
		}
	}

	private void initializeParams(ServiceContext serviceContext, long groupId, Map<Locale,String> localeTitlesMap, HashMap<Locale, String> localeDescriptionsMap, Element item)
	{
		serviceContext.setScopeGroupId(groupId);					
		
		List <String[]> titlelocaleList = getParamListByType(item, "title");		
		for (String[] titlelocale : titlelocaleList)
		{
			localeTitlesMap.put(LocaleUtil.fromLanguageId(titlelocale[0]), titlelocale[1]);
		}		
		
		List <String[]> descriptionlocaleList = getParamListByType(item, "description");		
		for (String[] descriptionlocale : descriptionlocaleList)
		{
			localeDescriptionsMap.put(LocaleUtil.fromLanguageId(descriptionlocale[0]), descriptionlocale[1]);
		}
		
	}
	
	private void updateLiveTable(long groupId, String globalId, String sGroupId, AssetVocabularyModel vocabulary, Element item, Document doc) throws PortalException, SystemException
	{
		//update entry in live table
		LiveLocalServiceUtil.add(_className, groupId, 0, 0, globalId,
				String.valueOf(vocabulary.getVocabularyId()), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
		
		try
		{
			//update globalId to assure that match in every server.
			LiveLocalServiceUtil.updateGlobalId(groupId, _className, String.valueOf(vocabulary.getVocabularyId()), globalId);
		}
		catch(Exception e2)
		{
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Duplicate globalId", IterKeys.ERROR, sGroupId);
		}
		
		try 
		{
			//Creamos/modificamos sus dependencias	
			if (! evaluateDependencies(item, doc))
			{
				LiveLocalServiceUtil.updateStatus(groupId, _className, globalId, IterKeys.INTERRUPT);
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, sGroupId);				
			}
			else
			{									
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, sGroupId);
			}
		} 
		catch (DocumentException err) 
		{
			LiveLocalServiceUtil.updateStatus(groupId, _className, globalId, IterKeys.INTERRUPT);
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Can't create dependency", IterKeys.INTERRUPT, sGroupId);				
		}
		
	}
}

