/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.protecmedia.iter.base.service.impl;

import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.model.PortletConstants;
import com.liferay.portal.model.PortletItem;
import com.liferay.portal.model.PortletPreferences;
import com.liferay.portal.security.permission.PermissionThreadLocal;
import com.liferay.portal.service.PortletItemLocalServiceUtil;
import com.liferay.portal.service.PortletPreferencesLocalServiceUtil;
import com.liferay.portal.service.PortletPreferencesServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortletKeys;
import com.protecmedia.iter.base.service.base.IterPortletPreferencesLocalServiceBaseImpl;
import com.protecmedia.iter.base.service.util.ErrorRaiser;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.PortletMgr;
import com.protecmedia.iter.base.service.util.PortletPreferencesTools;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.base.service.util.ThemeDisplayUtil;

/**
 * The implementation of the iter portlet preferences local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.base.service.IterPortletPreferencesLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.base.service.IterPortletPreferencesLocalServiceUtil} to access the iter portlet preferences local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author
 * @see com.protecmedia.iter.base.service.base.IterPortletPreferencesLocalServiceBaseImpl
 * @see com.protecmedia.iter.base.service.IterPortletPreferencesLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class IterPortletPreferencesLocalServiceImpl	extends IterPortletPreferencesLocalServiceBaseImpl 
{
	/**
	 * 
	 * @param request
	 * @param xmlInitParams
	 * @return
	 * @throws Exception
	 */
	public String getPortletPreferences(HttpServletRequest request, String xmlInitParams) throws Exception
	{
		return PortletMgr.getPortletPreferences(request, xmlInitParams);
	}
	
	public String setPortletPreferences(long plid, String portletResource, String xmlPreferences) throws Exception
	{
		return PortletMgr.setPortletPreferences(plid, portletResource, xmlPreferences);
	}
	
	/**
	 * 
	 * @param request
	 * @param xmlInitParams
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public Document linkSetup(HttpServletRequest request, String xmlInitParams, String data) throws Exception
	{
		// Acción de LINK o UNLINK
		Element elemData = SAXReaderUtil.read(data).getRootElement();
		boolean doUnlink = XMLHelper.getLongValueOf(elemData, "@isLinked") == 0 ? false : true;

		// PortletItemId a vincular
		long portletItemId			= XMLHelper.getLongValueOf(elemData, "@portletitemid");
		ErrorRaiser.throwIfFalse(portletItemId > 0);
		
		// UUID de dicho PortletItemId, se podría calcular en el servidor pero si ya lo tiene disponible el Flex no es necesario añadir lógica innecesariamente
		String portletItemUUID 		= XMLHelper.getTextValueOf(elemData, "@username");
		ErrorRaiser.throwIfNull(portletItemUUID);
				
		// Se construye el ThemeDisplay
		Document domInit = SAXReaderUtil.read(xmlInitParams);
		ThemeDisplay themeDisplay = ThemeDisplayUtil.buildThemeDisplay(domInit, request);
		request.setAttribute(WebKeys.THEME_DISPLAY, themeDisplay);
		request.setAttribute(WebKeys.LAYOUT, themeDisplay.getLayout());
		
		String portletId 			= GetterUtil.get(domInit.getRootElement().selectSingleNode("portletResource").getText(), "");;
		String portletItemPortletId = PortletConstants.getRootPortletId(portletId);
		
		Element rootResult = SAXReaderUtil.read("<rs/>").getRootElement();
		if (doUnlink)
		{
			// Se obtienen las preferencias del portlet
			Document domPreferences = SAXReaderUtil.read( PortletPreferencesTools.getPreferencesAsXML(request, xmlInitParams) );
			
			// Si está vinculado al UUID que se suministra se elimina dicho vínculo
			String linkedPortletItemUUID = XMLHelper.getTextValueOf(domPreferences, PortletPreferencesTools.PREFS_PORTLETITEM_VALUE_XPATH);
			if (portletItemUUID.equals(linkedPortletItemUUID))
			{
				domPreferences.selectSingleNode( PortletPreferencesTools.PREFS_PORTLETITEM_NODE_XPATH ).detach();
			}	

			// Se añaden las preferencias al XML de resultado
			rootResult.add( domPreferences.getRootElement().detach() );
		}
		else
		{
			// Se obtienen las preferencias del PortletItem
			javax.portlet.PortletPreferences portletItemPrefs = PortletPreferencesLocalServiceUtil.getPreferences(themeDisplay.getCompanyId(), 
																		portletItemId, PortletKeys.PREFS_OWNER_TYPE_ARCHIVED, 
																		LayoutConstants.DEFAULT_PLID, portletItemPortletId);
			
			// A estas preferencias se le añade el nodo PortletItem
			portletItemPrefs.setValue(IterKeys.PREFS_PORTLETITEM, portletItemUUID);
			
			Document doc = SAXReaderUtil.read( PortletPreferencesLocalServiceUtil.toXML(portletItemPrefs)  );
			
			// Si algún portlet está vinculado a este PortletItem. el PortletItem NO podrá tener una 'Sección Actual' configurada
			PortletPreferencesTools.checkDefaultLayoutPreferences(doc);
			
			// Se añaden las preferencias al XML de resultado
			rootResult.add( doc.getRootElement().detach() );
		}
		
		// Se añade la información de los PortletItems al XML de resultado
		Document domPortletItems = getSetups(String.valueOf(themeDisplay.getScopeGroupId()), portletItemPortletId);
		rootResult.add( domPortletItems.getRootElement().detach() );
		
		return rootResult.getDocument();
	}
	
	/**
	 * 
	 * @param scopeGroupId
	 * @param portletId
	 * @return
	 * @throws NumberFormatException
	 * @throws SystemException
	 * @throws PortalException 
	 * @throws DocumentException 
	 */
	public Document getSetups(String scopeGroupId, String portletId) throws NumberFormatException, SystemException, PortalException, DocumentException
	{
		List<PortletItem> portletItems = PortletItemLocalServiceUtil.getPortletItems(Long.parseLong(scopeGroupId), portletId, com.liferay.portal.model.PortletPreferences.class.getName());
		
		Document d = SAXReaderUtil.createDocument();
		Element root = d.addElement(IterKeys.XMLIO_XML_ELEMENT_ROOT);
		
		Iterator<PortletItem> itr = portletItems.iterator();
		while (itr.hasNext())
		{
			PortletItem pi = itr.next();
			Element row = root.addElement("portletitem");
			row.addAttribute( "portletitemid" 	,String.valueOf(pi.getPortletItemId()) );
			row.addAttribute( "modifieddate"	,pi.getModifiedDate().toString() );
			row.addAttribute( "name" 			,pi.getName() );
			row.addAttribute( "username" 		,pi.getUserName() );
			row.addAttribute( "portletid" 		,pi.getPortletId() );
			
			// Michel Godínez. 2013/09/12
			// De momento se desactiva, NO se quiere forzar a que por el hecho de tener Sección actual NO se puedan compartir 
			// preferencias. Si se quisiese forzar (idea inicial), basta con descomentar el código que aparece a continuación
			row.addAttribute( "defaultLayout", "false");
//			// Se determina si la preferencia tiene o no configurado el defaultLayout
//			javax.portlet.PortletPreferences preferences = PortletPreferencesLocalServiceUtil.getPreferences(GroupMgr.getCompanyId(), pi.getPortletItemId(), PortletKeys.PREFS_OWNER_TYPE_ARCHIVED, 0, pi.getPortletId());
//			Document doc = SAXReaderUtil.read( PortletPreferencesLocalServiceUtil.toXML(preferences) );
//			row.addAttribute( "defaultLayout" 	,String.valueOf(PortletPreferencesTools.isDefaultLayoutEnable(doc)) );
		}

		return d;
	}
	
	/**
	 * 
	 * @param request
	 * @param xmlInitParams
	 * @param portletId
	 * @param portletItemIds
	 * @return
	 * @throws Exception
	 */
	public Document deleteSetup(HttpServletRequest request, String xmlInitParams, String portletId, String portletItemIds, boolean checkNumReferences) throws Exception
	{
		PortletPreferencesTools.getPreferences(request, xmlInitParams);
		ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
		PermissionThreadLocal.setPermissionChecker(themeDisplay.getPermissionChecker());
		
		if (checkNumReferences)
		{
			String referencesLinksInfo = PortletPreferencesTools.checkNumReferencesLinks(portletItemIds);
			if( !referencesLinksInfo.equals(StringPool.BLANK)  )
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_ITR_E_PREFS_PORTLETITEM_WITH_LINKS_ZYX, referencesLinksInfo.toString());
		}
			
		String[] portletsId = portletItemIds.split(",");
		
		for(int i=0; i<portletsId.length; i++)
		{
			PortletPreferencesServiceUtil.deleteArchivedPreferences( Long.parseLong(portletsId[i]) );
		}
		
		return getSetups(String.valueOf(themeDisplay.getScopeGroupId()), portletId);
	}

	/**
	 * 
	 * @param request
	 * @param xmlInitParams
	 * @param portletId
	 * @param name
	 * @param preferences
	 * @return
	 * @throws Exception
	 */
	public Document updateSetup(HttpServletRequest request, String xmlInitParams, String portletId, String portletItemIdOverride, String name, String preferences, boolean checkNumReferences) throws Exception
	{
		if (checkNumReferences && !portletItemIdOverride.equals(StringPool.BLANK))
		{
			String referencesLinksInfo = PortletPreferencesTools.checkNumReferencesLinks(portletItemIdOverride);
			if( !referencesLinksInfo.equals(StringPool.BLANK)  )
				ErrorRaiser.throwIfError(IterErrorKeys.XYZ_ITR_E_PREFS_PORTLETITEM_WITH_LINKS_IMPOSSIBLE_OVERRIDE_ZYX, referencesLinksInfo.toString());
		}
			
		
		Element root 		= SAXReaderUtil.read(xmlInitParams).getRootElement();
		long userId  		= XMLHelper.getLongValueOf(root, "userId");
		long scopeGroupId 	= XMLHelper.getLongValueOf(root, "scopeGroupId");

		// Se actualiza el nombre del PortletItem
		PortletItem portletItem = PortletItemLocalServiceUtil.updatePortletItem(userId, scopeGroupId, name, portletId, PortletPreferences.class.getName());
		
		// Se actualizan las preferencias de dicho PortletItem
		PortletPreferencesLocalServiceUtil.updatePreferences(portletItem.getPortletItemId(), PortletKeys.PREFS_OWNER_TYPE_ARCHIVED, 0, portletId, preferences);
		
		return getSetups(String.valueOf(scopeGroupId), portletId);
	}

	/**
	 * 
	 * @param companyId
	 * @param portletItemId
	 * @param portletId
	 * @param name
	 * @param oldPreferences
	 * @return
	 * @throws Exception
	 */
	public String restoreSetup(String companyId, String portletItemId, String portletId, String name, String oldPreferences) throws Exception
	{
		javax.portlet.PortletPreferences preferences = PortletPreferencesLocalServiceUtil.getPreferences(Long.parseLong(companyId), Long.parseLong(portletItemId), PortletKeys.PREFS_OWNER_TYPE_ARCHIVED, 0, portletId);
		
		// El portlet podría tener preferencias que se desean conservar 
		Document doc = SAXReaderUtil.read(oldPreferences);
		
		// A las preferencias del PortletItemse le añade el PortletItem para que la interfaz sepa de donde viene
		String portletItemUUID	= XMLHelper.getTextValueOf(doc, PortletPreferencesTools.PREFS_PORTLETITEM_VALUE_XPATH);
		if (portletItemUUID != null)
			preferences.setValue(IterKeys.PREFS_PORTLETITEM, portletItemUUID);
		
		return PortletPreferencesLocalServiceUtil.toXML(preferences);
	}
	
	/**
	 * Método que a partir de un XML descriptor del PortletItem devuelve un DOM con las preferencias de aquellos portlets vinculados a él
	 * @param portletItem Descriptor de un PortletItem
	 * @return DOM con las preferencias de aquellos portlets vinculados a él
	 * @throws DocumentException 
	 * @throws ServiceError 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 */
	public Document getLinkedPreferences(String portletItem) throws DocumentException, ServiceError, SecurityException, NoSuchMethodException
	{
		Element elemRoot = SAXReaderUtil.read(portletItem).getRootElement();
		
		String userName = XMLHelper.getTextValueOf(elemRoot, "@username");
		ErrorRaiser.throwIfNull(userName);
		
		String portletId= XMLHelper.getTextValueOf(elemRoot, "@portletid");
		ErrorRaiser.throwIfNull(portletId);
		
		return PortletPreferencesTools.getLinkedPreferences(userName, portletId);
	}

}