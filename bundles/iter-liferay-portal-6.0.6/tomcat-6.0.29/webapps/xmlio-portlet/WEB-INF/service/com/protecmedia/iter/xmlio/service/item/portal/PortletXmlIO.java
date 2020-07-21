/*******************************************************************************
 * Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
 ******************************************************************************/
package com.protecmedia.iter.xmlio.service.item.portal;


import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import javax.portlet.PortletPreferences;

import com.liferay.portal.NoSuchLayoutException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.BaseModel;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutTypePortlet;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.PortletConstants;
import com.liferay.portal.model.ResourceConstants;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.PortletItemLocalServiceUtil;
import com.liferay.portal.service.ResourceLocalServiceUtil;
import com.liferay.portal.service.permission.PortletPermissionUtil;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portlet.PortletPreferencesFactoryUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.PortletPreferencesTools;
import com.protecmedia.iter.designer.model.PageTemplate;
import com.protecmedia.iter.designer.service.PageTemplateLocalServiceUtil;
import com.protecmedia.iter.xmlio.NoSuchLiveException;
import com.protecmedia.iter.xmlio.model.Live;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;
import com.protecmedia.iter.xmlio.service.item.ItemXmlIO;
import com.protecmedia.iter.xmlio.service.util.CDATAUtil;
import com.protecmedia.iter.xmlio.service.util.XMLIOContext;
import com.protecmedia.iter.xmlio.service.util.XMLIOExport;

/* XML Structure:
 *
<item operation="update" globalid="DiariTerrassaPre_118077" classname="com.liferay.portal.model.Portlet" groupid="The Star">
	<param name="pluginid">&lt;![CDATA[facebookportlet_WAR_serviceportlet]]&gt;</param>
	<param name="columnid">&lt;![CDATA[column-1]]&gt;</param>
	<param name="selectHeader" type="preference">
		<param name="value_0">&lt;![CDATA[false]]&gt;</param>
	</param>
	<param name="heightBody" type="preference">
		<param name="value_0">&lt;![CDATA[300]]&gt;</param>
	</param>
	<param name="width" type="preference">
		<param name="value_0">&lt;![CDATA[298]]&gt;</param>
	</param>
	<param name="selectNotes" type="preference">
		<param name="value_0">&lt;![CDATA[false]]&gt;</param>
	</param>	
</item>
*/


public class PortletXmlIO extends ItemXmlIO {
	
	private static Log _log = LogFactoryUtil.getLog(PortletXmlIO.class);
	private String _className = IterKeys.CLASSNAME_PORTLET;
	
	public PortletXmlIO() {
		super();
	}
	
	public PortletXmlIO(XMLIOContext xmlIOContext) {
		super(xmlIOContext);
	}	
	
	@Override
	public String getClassName(){
		return _className;
	}
	
	/*
	 * Live Functions
	 * 
	 * INFO: Los portlets no se comportan igual que el resto de items. Son agregados a live
	 * en los CRUDs de las páginas
	 */
	@Override
	public void populateLive(long groupId, long companyId)
			throws SystemException, PortalException {
		//Done in LayoutXmlIO
	}
	
	@Override
	public void createLiveEntry(BaseModel<?> model){}
	
	@Override
	public void deleteLiveEntry(BaseModel<?> model){}
	
	/*
	 * Export Contents
	 * 
	 * 
	 * INFO: Los portlets no se comportan igual que el resto de items. Cuando se crea, se borra o se modifica
	 * algún portlet, se crea una única entrada en la tabla de "live". 
	 * Se llamará al método exportContents del xmlio una única vez con el id (plid) del layout donde se 
	 * ha modificado algún portlet. Este método creará un único elemnto xml delete y un create por cada 
	 * portlet que haya en el backend. 
	 * Al hacer la importación, primero se eliminarán todos los portlets del layout y, a continuación, se crearan
	 * los mismos portlets que había en el backend.
	 */		
	@Override
	protected String createItemXML(XMLIOExport xmlioExport, Element root, String operation, Group group, Live live){
		
		StringBuffer error = new StringBuffer();
		
		long plid = GetterUtil.getLong(live.getLocalId());
		
		if (plid != -1) 
		{
			try
			{
				if (operation.equals(IterKeys.DELETE))
				{
					//Create a delete element to remove all portlets from the layout
					error.append(createPortletXML(xmlioExport, root, IterKeys.DELETE, group, null, live));
				}
				else
				{
					//Only import non private layouts.
					Layout layout = LayoutLocalServiceUtil.getLayout(plid);
					
					LayoutTypePortlet layoutTypePortlet = (LayoutTypePortlet) layout.getLayoutType();
					
					//Create a delete element to remove all portlets from the layout
					error.append(createPortletXML(xmlioExport, root, IterKeys.DELETE, group, null, live));
					
					List<Portlet> portlets =  layoutTypePortlet.getPortlets();
						
					for (Portlet portlet : portlets) 
					{
						error.append(createPortletXML(xmlioExport, root, operation, group, portlet, live));
					}
				}
			}
			catch (Exception e)
			{
				//LiveLocalServiceUtil.setError(live.getId(), IterKeys.CORRUPT, "Can't export item");			
				error.append("Cannot export item");
			}	
		}	
		
		return error.toString();
	}
	
	
	private String createPortletXML(XMLIOExport xmlioExport, Element root, String operation, 
									Group group, Portlet portlet, Live live)
	{
		StringBuffer error = new StringBuffer();
		
		Map<String, String> attributes = new HashMap<String, String>();
		Map<String, String> params = new HashMap<String, String>();	
		
		setCommonAttributes(attributes, group.getName(), live, operation);
			
		if (operation.equals(IterKeys.DELETE))
		{
			addNode(root, "item", attributes, params);
		}
		else
		{
			String columnId = "column_1";
			
			try
			{			
				Layout layout = LayoutLocalServiceUtil.getLayout(GetterUtil.getLong(live.getLocalId()));
								
				String portletId = portlet.getPortletId();
				
				Iterator<Entry<String, String>>  portletIterator = layout.getTypeSettingsProperties().entrySet().iterator();
				
				while (portletIterator.hasNext()) 
				{
					Entry<String, String> entry = (Entry<String, String>) portletIterator.next();
					
					if (entry.getValue().contains(portletId))
					{						
						columnId = entry.getKey().equals("nested-column-ids") ? columnId : entry.getKey();						
					}
				}				
				
				params.put("columnid", columnId);	
				//Si es un anidador de portlets lo pasamos como una instancia
				if (portlet.getPluginId().equals("118"))
				{
					params.put("pluginid", portlet.getPortletId());
				}
				else
				{
					params.put("pluginid", portlet.getPluginId());	
				}
			
				Element itemNode = addNode(root, "item", attributes, params);	
				
				error.append(exportPreferences(itemNode, group, layout, portletId, live.getId()));
				
				if (PortletConstants.getRootPortletId(portletId).equals(PortletKeys.NESTED_PORTLETS))
				{
					// Se obtiene el TPL configurado para añadirlo como dependencia a publicar al LIVE
					String tplId = CDATAUtil.strip(XMLHelper.getStringValueOf(itemNode, "param[@name='layout-template-id']/param[@name='value_0']"));
					
					if (Validator.isNotNull(tplId))
						xmlioExport.addTpl(tplId);				
				}
			} 
			catch (Exception e) 
			{	
				String errormsg = (portlet == null ? "Error al borrar los portlets"  : "Error in portlet: " + portlet.getPortletId());
				error.append((error.length()==0?"":";") + errormsg);
				//LiveLocalServiceUtil.setError(live.getId(), IterKeys.CORRUPT, errormsg);			
				//throw e;
			}
		}
				
		_log.debug("INFO: Portlet exported");
		
		return error.toString();
		
	}	
	
	/**
	 * 
	 * @param parentNode
	 * @param group
	 * @param layout
	 * @param portletId
	 * @return
	 * @throws Exception 
	 * @throws PortalException
	 */
	private String exportPreferences(Element parentNode, Group group, Layout layout, String portletId, long liveId) throws Exception
	{
		StringBuffer error = new StringBuffer();
		
		PortletPreferences preferences = PortletPreferencesFactoryUtil.getLayoutPortletSetup(layout, portletId);	
		Map<String, String[]> preferencesMap = preferences.getMap();
		Iterator<Entry<String, String[]>> prefsIterator = preferencesMap.entrySet().iterator();
			
		while (prefsIterator.hasNext()) 
		{
			Map<String, String> preferenceAttributes 	= new HashMap<String, String>();
			Map<String, String> preferenceParams 	 	= new HashMap<String, String>();	
			
			/*mapa utilizado para guardar las preferencias cuyo orden sea importante mantener(como las calificaciones) 
			  En LinkedHashMap se insertan los elementos en el mismo orden en el que se van insertando*/
			Map<String, String> preferenceParamsInOrder = new LinkedHashMap<String, String>();
			
			Map.Entry<String, String[]> entry = (Map.Entry<String, String[]>) prefsIterator.next();	
			
			String name = (String) entry.getKey();
			preferenceAttributes.put("name", name);
			preferenceAttributes.put("type", "preference");	
			
			try
			{
				//Evitamos las preferencias nulas
				if(entry.getValue() != null)
				{
					//Si los values son de tipo id pueden NO coincidir en diferentes máquinas y debemos pasar en el xml su globalId.
					
					if (name.equals(IterKeys.PREFS_PORTLETITEM))
					{
						// Si está vinculada a un PortletItem se añaden todos los campos de dicho PortletItem
						String userName 	= entry.getValue()[0];
						
						Document dom 		= PortletPreferencesTools.getPortletItem(userName);
						
						String piUserId		= XMLHelper.getTextValueOf(dom, "/rs/row/@userId");
						ErrorRaiser.throwIfNull(piUserId);
						
						String piGroupId	= XMLHelper.getTextValueOf(dom, "/rs/row/@groupId");
						ErrorRaiser.throwIfNull(piGroupId);
						
						String piName		= XMLHelper.getTextValueOf(dom, "/rs/row/@name");
						ErrorRaiser.throwIfNull(piName);
						
						String piPortletId	= XMLHelper.getTextValueOf(dom, "/rs/row/@portletId");
						ErrorRaiser.throwIfNull(piPortletId);
						
						//getPortletItem(String userName)
						preferenceParams.put("portletItem_userId", 		piUserId);
						preferenceParams.put("portletItem_groupId", 	piGroupId);
						preferenceParams.put("portletItem_name", 		piName);
						preferenceParams.put("portletItem_portletId", 	piPortletId);
						preferenceParams.put("portletItem_userName", 	userName);
					}
					else if(name.equals("layoutIds") 		|| name.equals("layoutPlid") 	||
							name.equals("layoutIdsFilter") 	|| name.equals("depthStartAt")	||
							name.equals(PortletKeys.PREFS_SHOW_SECTIONS_LIST))
					{
						int i = 0;	
						for (String prefValue : entry.getValue())
						{					
							Layout layoutPref = null;
							try
							{
								layoutPref = LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(prefValue, group.getGroupId());
							}
							catch(NoSuchLayoutException e)
							{
								//El layout no existe, luego no se importa		
								_log.error("Layout " + prefValue + " referred in portlet " + portletId + " does not exist");
							}
							if (layoutPref != null)
							{
								Live liveLayoutPref = LiveLocalServiceUtil.getLiveByLocalId(group.getGroupId(), IterKeys.CLASSNAME_LAYOUT, String.valueOf(layoutPref.getPlid()));
								if (liveLayoutPref != null)
								{
									preferenceParams.put("value_" + i++, liveLayoutPref.getGlobalId());
								}
							}
						}
						
					}
					else if(name.equals("assetCategoryIds") 	|| 
							 name.equals("contentCategoryIds") 	||
							 name.equals("stickyCategoryIds") 	||
							 name.equals("excludeCategoryIds")	||
							 name.equals("categoryIdsFilter")	||
							 name.equals(PortletKeys.PREFS_SHOW_CATEGORIES_LIST)
							 )
					{
						
						int i = 0;	
						for (String prefValue : entry.getValue()){
							StringTokenizer st = new StringTokenizer(prefValue,",");
							while (st.hasMoreTokens()){
								String assetCategoryId= st.nextToken();
								Live liveCategoryPref = LiveLocalServiceUtil.getLiveByLocalId(group.getGroupId(), IterKeys.CLASSNAME_CATEGORY, assetCategoryId);
								if (liveCategoryPref != null)
								{
									preferenceParams.put("value_" + i++, liveCategoryPref.getGlobalId());
								}
								else
								{ //intentamos con el grupo global
									liveCategoryPref = LiveLocalServiceUtil.getLiveByLocalId(CompanyLocalServiceUtil.getCompany(group.getCompanyId()).getGroup().getGroupId(), IterKeys.CLASSNAME_CATEGORY, assetCategoryId);
									if (liveCategoryPref != null)
									{
										preferenceParams.put("value_" + i++, liveCategoryPref.getGlobalId());
									}
								}
							}
						}
						
					}
					else if( name.equals("contentVocabularyIds") || name.equals("excludeVocabularyIds")  || name.equals("vocabularyIdsFilter")  )
					{
						int i = 0;	
						for (String prefValue : entry.getValue())
						{
							StringTokenizer st = new StringTokenizer(prefValue,",");
							while (st.hasMoreTokens())
							{
								String vocabularyId= st.nextToken();
								Live livePref = LiveLocalServiceUtil.getLiveByLocalId(group.getGroupId(), IterKeys.CLASSNAME_VOCABULARY, vocabularyId);
								if (livePref != null)
								{
									preferenceParams.put("value_" + i++, livePref.getGlobalId());
								}
								else
								{ //intentamos con el grupo global
									livePref = LiveLocalServiceUtil.getLiveByLocalId(CompanyLocalServiceUtil.getCompany(group.getCompanyId()).getGroup().getGroupId(), 
																							 IterKeys.CLASSNAME_VOCABULARY, vocabularyId);
									if (livePref != null)
									{
										preferenceParams.put("value_" + i++, livePref.getGlobalId());
									}
								}
							}
						}
						
					}
					else if(name.equals("qualificationId"))
					{
						int i = 0;	
						for (String prefValue : entry.getValue())
						{						
							Live liveQualificationPref = LiveLocalServiceUtil.getLiveByLocalId(group.getGroupId(), IterKeys.CLASSNAME_QUALIFICATION, prefValue);
							if (liveQualificationPref != null)
							{
								preferenceParamsInOrder.put("value_" + i++, liveQualificationPref.getGlobalId());
							}
						}
					
					}
					else if(name.equals("group-id"))
					{
						int i = 0;	
						for (String prefValue : entry.getValue()){						
							Live liveQualificationPref = LiveLocalServiceUtil.getLiveByLocalId(group.getGroupId(), IterKeys.CLASSNAME_GROUP, prefValue);
							
							if (liveQualificationPref != null){
								preferenceParams.put("value_" + i++, liveQualificationPref.getGlobalId());
							}else{ //intentamos con el grupo global
								liveQualificationPref = LiveLocalServiceUtil.getLiveByLocalId(CompanyLocalServiceUtil.getCompany(group.getCompanyId()).getGroup().getGroupId(), IterKeys.CLASSNAME_GROUP, prefValue);
								if (liveQualificationPref != null){
									preferenceParams.put("value_" + i++, liveQualificationPref.getGlobalId());
								}
							}
						}
					}
					else if(name.equals("bannerTextHTML") || name.equals("textHTML") || name.equals("defaultTextHTML") || name.equals("webName") 
							|| name.equals("registeredUserCode") || name.equals("unregisteredUserCode") || name.equals("textVelocity"))
					{
						int i = 0;	
						for (String prefValue : entry.getValue())
						{
							if(prefValue != null)
							{
								prefValue = toCompactSafe(prefValue);
							}
							preferenceParams.put("value_" + i++, prefValue);
						}
					}
					else if(name.equals("modelId"))
					{
						int i = 0;	
						for (String prefValue : entry.getValue())
						{						
							if ((prefValue != null) && 
								(!prefValue.equals("")) && 
								(!prefValue.equals("0")) && 
								(!prefValue.equals("-1"))){
									long id = Long.parseLong(prefValue);
									PageTemplate pg = PageTemplateLocalServiceUtil.getPageTemplateById(id);
									Live liveQualificationPref = LiveLocalServiceUtil.getLiveByLocalId(group.getGroupId(), IterKeys.CLASSNAME_PAGETEMPLATE, pg.getPageTemplateId());
									if (liveQualificationPref != null)
									{
										preferenceParams.put("value_" + i++, liveQualificationPref.getGlobalId());
									}
							}
							else{
								preferenceParams.put("value_" + i++, prefValue);
							}
						}
					}
					else if( name.equals("subscriptions") || name.equals("subscriptions4show") || name.equals("subscriptions4notShow") )
					{
						long scopeGroupId = group.getGroupId();
						int i = 0;
						for (String prefValue : entry.getValue())
						{
							Live liveProductPref = LiveLocalServiceUtil.getLiveByLocalId(scopeGroupId, IterKeys.CLASSNAME_PRODUCT, prefValue);
							preferenceParams.put("value_" + i++, liveProductPref.getGlobalId());
						}
						i++;
					}
					else
					{
						int i = 0;	
						for (String prefValue : entry.getValue())
						{
							preferenceParams.put("value_" + i++, prefValue);
						}
					}				
				}
				
				if(preferenceParamsInOrder.size() > 0 )
					addNode(parentNode, "param", preferenceAttributes, preferenceParamsInOrder);
				else 
					addNode(parentNode, "param", preferenceAttributes, preferenceParams);
				
			}
			catch(Exception e)
			{
				_log.error(e);
				error.append((error.length()==0?"":";") +  String.format("Can't get preference: %s (%s)", portletId, name));				
				//LiveLocalServiceUtil.setError(liveId, IterKeys.CORRUPT, "Can't get preference: " + name);			
				//throw e;
			}
		}	
		
		return error.toString();
	}	
	
	
	public Layout delete(Layout layout, Element item, String globalId, String sGroupId, long plid)
	{
		try
		{
			List<Portlet> portlets = ((LayoutTypePortlet)layout.getLayoutType()).getPortlets();
			for (Portlet portlet : portlets) 
			{	
				((LayoutTypePortlet)layout.getLayoutType()).removePortletId(xmlIOContext.getUserId(), portlet.getPortletId(), true, false);

                ResourceLocalServiceUtil.deleteResource(layout.getCompanyId(), portlet.getRootPortletId(),
                        ResourceConstants.SCOPE_INDIVIDUAL,
                        PortletPermissionUtil.getPrimaryKey(plid, portlet.getPortletId()));
			}
            layout = LayoutLocalServiceUtil.updateLayout(layout, false);
            
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Done", IterKeys.DONE, sGroupId);
		}
		catch(Exception e)
		{
			_log.error(e);
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.DELETE, "Error: " + e.getMessage(), IterKeys.ERROR, sGroupId);				
		}
		return layout;
	}
	
	@Override
	protected void delete(Element item) 
	{
		// ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_NOT_IMPLEMENTED_ZYX);
	}

	public Layout modify(Layout layout, Element item, String globalId, String sGroupId, long plid)
	{
		String pluginId = getParamTextByName(item, "pluginid");	
		String columnId = getParamTextByName(item, "columnid");	
		
		try
		{
			long groupId = getGroupId(sGroupId);
			
			try
			{
				String portletId = ((LayoutTypePortlet) layout.getLayoutType()).addPortletId(0, pluginId, columnId, -1, false);	
			
				if (portletId != null)
				{
					copyPreferences(item, layout, portletId, sGroupId, globalId);
                    layout = LayoutLocalServiceUtil.updateLayout(layout, false);
					
					LiveLocalServiceUtil.add(_className, groupId, globalId, Long.toString(plid), IterKeys.CREATE, IterKeys.DONE, new Date(), IterKeys.ENVIRONMENT_LIVE);
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Done", IterKeys.DONE, sGroupId);		
				}
				else
				{
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Cannot add portlet " + pluginId + " in column " + columnId, IterKeys.ERROR, sGroupId);
				}
			}
			catch(Exception e)
			{
				_log.error(e);
				xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Element not found", IterKeys.ERROR, sGroupId);				
			}
		} 
		catch (Exception e) 
		{
			_log.error(e);
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "GroupId not found", IterKeys.ERROR, sGroupId);
		}
		
		return layout;
	}

	@Override
	protected void modify(Element item, Document doc)
	{
		// ErrorRaiser.throwIfError(IterErrorKeys.XYZ_E_NOT_IMPLEMENTED_ZYX);
	}

	private void copyPreferences(Element item, Layout layout, String portletId, String sGroupId, String globalId)
	{		
		try
		{
			_log.trace("[Enter] - PortletXmlIO - copyPreferences");
			
			PortletPreferences preferences = PortletPreferencesFactoryUtil.getLayoutPortletSetup(layout, portletId);
					
			_log.trace("[1] Current Preferences...\n" + preferences.toString());
			
			List<Element> prefList = getParamElementListByType(item, "preference");		
			
			long groupId = getGroupId(sGroupId);
			
			for (Element pref : prefList) 
			{					
				String key = getAttribute(pref, "name");			
				List<String[]> prefValueList = getParamListByType(pref, "");	
			
				String[] values = new String[prefValueList.size()];
				int i=0;
				
				try 
				{
					// Si es una referencia al portletItem se crea o actualiza dicho portletItem 
					if (key.equals(IterKeys.PREFS_PORTLETITEM))
					{
						long piUserId = 0; 		long piGroupId = 0;
						String piName = null; 	String piPortletId = null; String piUserName = null;
						for (String[] prefValue : prefValueList) 
						{
							if (prefValue[0].equals("portletItem_userId"))
								piUserId = Long.parseLong(prefValue[1]);
							else if (prefValue[0].equals("portletItem_groupId"))
								piGroupId = Long.parseLong(prefValue[1]);
							else if (prefValue[0].equals("portletItem_name"))
								piName = prefValue[1];
							else if (prefValue[0].equals("portletItem_portletId"))
								piPortletId = prefValue[1];
							else if (prefValue[0].equals("portletItem_userName"))
								piUserName = prefValue[1];
						}
						ErrorRaiser.throwIfFalse(piUserId > 0);
						ErrorRaiser.throwIfFalse(piGroupId > 0);
						ErrorRaiser.throwIfNull(piName);
						ErrorRaiser.throwIfNull(piPortletId);
						ErrorRaiser.throwIfNull(piUserName);
						
						// Se crea o actualiza el PortletItem
						PortletItemLocalServiceUtil.updatePortletItem(piUserId, piGroupId, piName, piPortletId, PortletPreferences.class.getName(), piUserName);
						
						// Se guarda en las preferencias el UUID de dicho PortletItem. Si no se actualiza la dimensión de "values" no se guardaría
						// la preferencia correctamente (<preference><name>portletItem</name><value>16855cb9-1171-11e3-8d90-f3682eadd837</value><value>NULL_VALUE</value><value>NULL_VALUE</value>...</preference>)
						values = new String[1];
						values[0] = piUserName;
					}
					else if(key.equals("layoutIds") 		|| key.equals("layoutPlid") 	||
							key.equals("layoutIdsFilter") 	|| key.equals("depthStartAt") 	||
							key.equals(PortletKeys.PREFS_SHOW_SECTIONS_LIST))
					{
						for (String[] prefValue : prefValueList) 
						{
							Live liveLayoutPref = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_LAYOUT, prefValue[1]);
							Layout layoutPref = LayoutLocalServiceUtil.getLayout(GetterUtil.getLong(liveLayoutPref.getLocalId()));
							
							values[i] = layoutPref.getUuid();
							i++;
						}
										
					}
					else if(key.equals("qualificationId"))
					{
						for (String[] prefValue : prefValueList)
						{
							Live liveQualificationPref = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_QUALIFICATION, prefValue[1]);
							
							values[i] = liveQualificationPref.getLocalId();
							i++;
						}			
					}
					else if (key.equals("assetCategoryIds") && prefValueList.size() > 0)
					{
						StringBuilder categoriesValues = new StringBuilder();
						for (String[] prefValue : prefValueList) 
						{
							Live liveCategoryPref = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_CATEGORY, prefValue[1]);
							if(liveCategoryPref != null)
							{
								categoriesValues.append( categoriesValues.length()!=0 ? ","+liveCategoryPref.getLocalId() : liveCategoryPref.getLocalId() );
							}
							else
							{
								long globalGroupId = CompanyLocalServiceUtil.getCompany(GroupLocalServiceUtil.getGroup(groupId).getCompanyId()).getGroup().getGroupId();
								liveCategoryPref = LiveLocalServiceUtil.getLiveByGlobalId(globalGroupId, IterKeys.CLASSNAME_CATEGORY, prefValue[1]);
								
								categoriesValues.append( categoriesValues.length()!=0 ? ","+liveCategoryPref.getLocalId() : liveCategoryPref.getLocalId() );
							}
						}
						
						values[0] = categoriesValues.toString();
					}
					else if ((key.equals("contentCategoryIds")  ||
							  key.equals("stickyCategoryIds")   ||
							  key.equals("excludeCategoryIds")	||
							  key.equals("categoryIdsFilter")	||
							  key.equals(PortletKeys.PREFS_SHOW_CATEGORIES_LIST)) &&
							  prefValueList.size() > 0)
					{
						for (String[] prefValue : prefValueList) 
						{
							Live liveCategoryPref = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_CATEGORY, prefValue[1]);
							if(liveCategoryPref != null)
							{
								values[i] = liveCategoryPref.getLocalId();
							}
							else
							{
								long globalGroupId = CompanyLocalServiceUtil.getCompany(GroupLocalServiceUtil.getGroup(groupId).getCompanyId()).getGroup().getGroupId();
								liveCategoryPref = LiveLocalServiceUtil.getLiveByGlobalId(globalGroupId, IterKeys.CLASSNAME_CATEGORY, prefValue[1]);
								
								values[i] = liveCategoryPref.getLocalId();
							}
							i++;
						}
					}
					else if ( (key.equals("contentVocabularyIds") || key.equals("excludeVocabularyIds") || key.equals("vocabularyIdsFilter")) && prefValueList.size() > 0)
					{
						for (String[] prefValue : prefValueList) 
						{
							Live liveCategoryPref = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_VOCABULARY, prefValue[1]);
							if(liveCategoryPref != null)
							{
								values[i] = liveCategoryPref.getLocalId();
							}
							else
							{
								long globalGroupId = CompanyLocalServiceUtil.getCompany(GroupLocalServiceUtil.getGroup(groupId).getCompanyId()).getGroup().getGroupId();
								liveCategoryPref = LiveLocalServiceUtil.getLiveByGlobalId(globalGroupId, IterKeys.CLASSNAME_VOCABULARY, prefValue[1]);
								
								values[i] = liveCategoryPref.getLocalId();
							}
							i++;
						}
					}
					else if(key.equals("group-id"))
					{
						for (String[] prefValue : prefValueList) 
						{
							Live liveQualificationPref = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_GROUP, prefValue[1]);
							
							if(liveQualificationPref != null){
								values[i] = liveQualificationPref.getLocalId();
								i++;
							}else{
								long globalGroupId = CompanyLocalServiceUtil.getCompany(GroupLocalServiceUtil.getGroup(groupId).getCompanyId()).getGroup().getGroupId();
								liveQualificationPref = LiveLocalServiceUtil.getLiveByGlobalId(globalGroupId, IterKeys.CLASSNAME_GROUP, prefValue[1]);
								
								values[i] = liveQualificationPref.getLocalId();
								i++;
							}						
						}			
					}
					else if(key.equals("bannerTextHTML") || key.equals("defaultTextHTML") || key.equals("webName") 
							|| key.equals("textHTML") || key.equals("registeredUserCode") || key.equals("unregisteredUserCode")
							|| key.equals("textVelocity") )
					{
						for (String[] prefValue : prefValueList)
						{
							if(prefValue != null && prefValue.length > 0)
							{
								values[i] = fromCompactSafe(prefValue[1]);
							}
							i++;
						}
					}
					else if(key.equals("modelId"))
					{
						for (String[] prefValue : prefValueList) 
						{
							if(prefValue != null && 
							   prefValue.length > 0 && 
							   !prefValue[1].equals("") && 
							   !prefValue[1].equals("0") && 
							   !prefValue[1].equals("-1")){
								Live liveLayoutPref = LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_PAGETEMPLATE, prefValue[1]);
								PageTemplate pg = PageTemplateLocalServiceUtil.getPageTemplateByPageTemplateId(groupId, liveLayoutPref.getLocalId());
								values[i] = String.valueOf(pg.getId());
							}
							else{
								values[i] = prefValue[1];
							}
							i++;
						}
										
					}
					else if( key.equals("subscriptions") || key.equals("subscriptions4show") || key.equals("subscriptions4notShow") )
					{
						for (String[] prefValue : prefValueList)
						{
							Live liveCategoryPref =  LiveLocalServiceUtil.getLiveByGlobalId(groupId, IterKeys.CLASSNAME_PRODUCT, prefValue[1]);
							if(liveCategoryPref != null)
								values[i] = liveCategoryPref.getLocalId();

							i++;
						}
					}
					else if( key.equals("service") )
					{
						for (String[] prefValue : prefValueList)
						{
							checkServiceInLive(prefValue[1]);
							values[i] = prefValue[1];
							i++;
						}
					}
					else
					{	
						for (String[] prefValue : prefValueList) 
						{
							values[i] = prefValue[1];
							i++;
						}
					}
					
					preferences.setValues(key, values);	
					
				} 
				catch (Exception e) 
				{
					_log.error(e);
					xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error copying Portlet preferences for " + key + "- in " + portletId, IterKeys.ERROR, sGroupId);				
				}	
			}		
			
			preferences.store();
			_log.trace("[2] Updated Preferences...\n" + preferences.toString());
		} 
		catch (Exception e) 
		{
			_log.error(e);
			xmlIOContext.itemLog.addMessage(item, globalId, _className, IterKeys.CREATE, "Error storing Portlet preferences for portlet Id " + portletId + " at layout " + layout.getFriendlyURL(), IterKeys.ERROR, sGroupId);				
		}
		
		_log.trace("[Exit] - PortletXmlIO - copyPreferences");
	}
	

	//Funciones iguales a las que hay en com.liferay.util.xml.XMLFormatter (util-java.jar)
	//que tratan los \n de los CDATA
	public static String toCompactSafe(String xml) 
	{
		return StringUtil.replace(
			xml,
			new String[] {
				StringPool.RETURN_NEW_LINE,
				StringPool.NEW_LINE,
				StringPool.RETURN
			},
			new String[] {
				"[$NEW_LINE$]",
				"[$NEW_LINE$]",
				"[$NEW_LINE$]"
			});
	}
	
	public static String fromCompactSafe(String xml) 
	{
		return StringUtil.replace(xml, "[$NEW_LINE$]", StringPool.NEW_LINE);
	}
	
	private void checkServiceInLive(String serviceid) throws SecurityException, NoSuchMethodException, NoSuchLiveException
	{
		String sql = String.format("SELECT COUNT(*) count FROM externalservice WHERE serviceId='%s'", serviceid);
		Document service = PortalLocalServiceUtil.executeQueryAsDom(sql);
		if (XMLHelper.getLongValueOf(service.getRootElement(), "/rs/row/@count") <= 0)
		{
			throw new NoSuchLiveException();
		}
	}
}
