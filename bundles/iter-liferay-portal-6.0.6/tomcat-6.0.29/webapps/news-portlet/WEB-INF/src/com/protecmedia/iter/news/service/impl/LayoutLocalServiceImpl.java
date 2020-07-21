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

package com.protecmedia.iter.news.service.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.portlet.PortletPreferences;
import javax.portlet.ReadOnlyException;
import javax.portlet.ValidatorException;

import org.apache.commons.lang.StringUtils;

import com.liferay.portal.LayoutFriendlyURLException;
import com.liferay.portal.kernel.annotation.Isolation;
import com.liferay.portal.kernel.annotation.Transactional;
import com.liferay.portal.kernel.error.ErrorRaiser;
import com.liferay.portal.kernel.error.IterErrorKeys;
import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.model.LayoutTemplate;
import com.liferay.portal.model.LayoutTypePortlet;
import com.liferay.portal.model.LayoutTypePortletConstants;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.PortletConstants;
import com.liferay.portal.model.ResourceConstants;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.ResourceLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.permission.PortletPermissionUtil;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portlet.PortletPreferencesFactoryUtil;
import com.protecmedia.iter.base.service.ThemeWebResourcesLocalServiceUtil;
import com.protecmedia.iter.base.service.util.GroupMgr;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.designer.service.PageTemplateLocalServiceUtil;
import com.protecmedia.iter.news.service.base.LayoutLocalServiceBaseImpl;
import com.protecmedia.iter.xmlio.service.LiveLocalServiceUtil;

/**
 * The implementation of the layout local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.protecmedia.iter.news.service.LayoutLocalService} interface.
 * </p>
 *
 * <p>
 * Never reference this interface directly. Always use {@link com.protecmedia.iter.news.service.LayoutLocalServiceUtil} to access the layout local service.
 * </p>
 *
 * <p>
 * This is a local service. Methods of this service will not have security checks based on the propagated JAAS credentials because this service can only be accessed from within the same VM.
 * </p>
 *
 * @author protec
 * @see com.protecmedia.iter.news.service.base.LayoutLocalServiceBaseImpl
 * @see com.protecmedia.iter.news.service.LayoutLocalServiceUtil
 */
@Transactional(isolation = Isolation.PORTAL, rollbackFor =  {Exception.class} )
public class LayoutLocalServiceImpl extends LayoutLocalServiceBaseImpl
{
	private static Log _log = LogFactoryUtil.getLog(LayoutLocalServiceImpl.class);
	
	private final String _xpath ="/root/params/%s";
	
	private final String TYPE_PAGE_TEMPLATE			= "page-template";
	
	private final String LAYOUTID = "layoutid";
	private final String PLID = "plid";
	private final String FRIENDLY_URL = "friendlyurl";
	
	private final String COPY_SECTION_PROPERTIES = new StringBuilder(
		"INSERT INTO sectionproperties( groupid, plid, 	menuelementid, nomenu, headerelementid, noheader, footerelementid, nofooter ) 	\n").append(
		"SELECT 						groupid, %d, 	menuelementid, nomenu, headerelementid, noheader, footerelementid, nofooter 	\n").append(
		"FROM sectionproperties 																										\n").append(
		"	WHERE plid=%d 																												\n").append(
		"	LIMIT 1 																													\n").append(
		"ON DUPLICATE KEY UPDATE 	menuelementid	= VALUES(menuelementid), 	nomenu	 = VALUES(nomenu), 								\n").append( 										
		"							headerelementid	= VALUES(headerelementid), 	noheader = VALUES(noheader), 							\n").append( 						
		"							footerelementid	= VALUES(footerelementid), 	nofooter = VALUES(nofooter)								\n").toString();
	
	public final String GET_LAYOUTS_FRIENDLYURLS = String.format(new StringBuilder(
		"SELECT IF ( Designer_PageTemplate.id_ IS NULL, Layout.plid, Designer_PageTemplate.id_) id, \n").append(
		"		IF ( Designer_PageTemplate.id_ IS NULL, '%s', '%s') type,							\n").append(
		"		friendlyURL																			\n").append(
		"FROM Layout																				\n").append(
		"LEFT JOIN Designer_PageTemplate ON Layout.plid = Designer_PageTemplate.layoutId			\n").toString(), LayoutConstants.CLASS_LAYOUT, LayoutConstants.CLASS_MODEL);
		
	
	public void setHidden(long plid, boolean hidden) throws Exception 
	{	
		Layout layout = LayoutLocalServiceUtil.getLayout(plid);
		layout.setHidden(hidden);
		LayoutLocalServiceUtil.updateLayout(layout);
	}
	
	public Document addLayout(long groupId, long parentLayoutId, String xml) throws Exception
	{
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse( Validator.isNotNull(xml), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		if(_log.isDebugEnabled())
			_log.debug( String.format("addLayout parameters: groupid: %s, parentLayoutId: %s, data: %s", groupId, parentLayoutId, xml) );
		
		Document d = SAXReaderUtil.read(xml);
		
		String type = XMLHelper.getTextValueOf(d, String.format(_xpath, "@type") );
		ErrorRaiser.throwIfNull(type, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String name = XMLHelper.getTextValueOf(d, String.format(_xpath, "@name") );
		ErrorRaiser.throwIfNull(name, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		//Puede ser "pending" (publicable) o "draft" (no publicable).
		//Este parámetro sustituye funcionalmente al método antiguo "changeLiveStatus()"
		String livestatus = XMLHelper.getTextValueOf(d, String.format(_xpath, "@livestatus") );
		ErrorRaiser.throwIfNull(livestatus, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		String pagetemplateid = XMLHelper.getTextValueOf(d, String.format(_xpath, "@pagetemplateid") );
		if( pagetemplateid!=null && !pagetemplateid.equals("0") )
			checkPageTemplateType(pagetemplateid);
		
		long srcPlid = XMLHelper.getLongValueOf(d, String.format(_xpath, "@srcplid") );
		
		Map<Locale, String> localeNamesMap = getLocaleMap(name);
		
		String htmlTitle = XMLHelper.getTextValueOf(d, String.format(_xpath, "@htmltitle") );
		Map<Locale, String> localeTitleMap = getLocaleMap(htmlTitle);
		
		boolean hidden = GetterUtil.getBoolean( XMLHelper.getTextValueOf(d, String.format(_xpath, "@hidden") ), false);
		
		String friendlyUrl = XMLHelper.getTextValueOf(d, String.format(_xpath, "@friendlyurl") );
		
		ServiceContext serviceContext = new ServiceContext();

		long userId = GroupMgr.getDefaultUserId();
		
		Layout l = null; 
		
		try
		{
			l = LayoutLocalServiceUtil.addLayout(userId, groupId, false, parentLayoutId, localeNamesMap, localeTitleMap, StringPool.BLANK, type, hidden, friendlyUrl, serviceContext);
		}
		catch (Exception e)
		{
			if( e instanceof LayoutFriendlyURLException )
			{
				LayoutFriendlyURLException lfue = (LayoutFriendlyURLException)e;
				if( lfue.getType()==LayoutFriendlyURLException.DUPLICATE )
				{
					String duplicateFriendlyUrl = Validator.isNotNull(friendlyUrl) ? friendlyUrl : StringPool.BLANK;
					ErrorRaiser.throwIfError( IterErrorKeys.XYZ_E_LAYOUT_FRIENDLYURL_DUPLICATE_ZYX, duplicateFriendlyUrl);
				}
			}
			
			throw e;
		}
		
		l = _updateLayout(l, d, groupId, userId, srcPlid, pagetemplateid);

		//Se actualiza el estado de la página en la tabla Xmlio_Live
		LiveLocalServiceUtil.changeLiveStatus(groupId, IterKeys.CLASSNAME_LAYOUT, String.valueOf(l.getPlid()), livestatus);
		
		Map<String, String> info = new HashMap<String, String>();
		info.put(LAYOUTID,  String.valueOf(l.getLayoutId()) );
		info.put(PLID,       String.valueOf(l.getPlid()) );
		info.put(FRIENDLY_URL,  l.getFriendlyURL() );

		return layoutInfo( info );
	}
	
	public Document updateLayout(long plid, String xml) throws Exception 
	{
		ErrorRaiser.throwIfFalse( Validator.isNotNull(xml), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		Document d = SAXReaderUtil.read(xml);
		
		ErrorRaiser.throwIfFalse( d.selectNodes("/root/params").size()==1, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		if(_log.isDebugEnabled())
			_log.debug( String.format("updateLayout parameters: plid: %s, data: %s", plid, xml) );
		
		String pagetemplateid = XMLHelper.getTextValueOf(d, String.format(_xpath, "@pagetemplateid") );
		if( pagetemplateid!=null && !pagetemplateid.equals("0") )
			checkPageTemplateType(pagetemplateid);
		
		long srcPlid = XMLHelper.getLongValueOf(d, String.format(_xpath, "@srcplid") );
		
		Layout l = LayoutLocalServiceUtil.getLayout(plid);
		
		String name = XMLHelper.getTextValueOf(d, String.format(_xpath, "@name") );
		if(Validator.isNotNull(name))
			l.setName(name, LocaleUtil.getDefault());
		
		String type = XMLHelper.getTextValueOf(d, String.format(_xpath, "@type") );
		if(Validator.isNotNull(type))
			l.setType(type);
		
		String friendlyUrl = XMLHelper.getTextValueOf(d, String.format(_xpath, "@friendlyurl") );
		if(Validator.isNotNull(friendlyUrl))
		{
			try
			{
				friendlyUrl = LayoutLocalServiceUtil.validateLayoutFriendlyUrl(l, friendlyUrl);
			}
			catch (Exception e)
			{
				if( e instanceof LayoutFriendlyURLException )
				{
					LayoutFriendlyURLException lfue = (LayoutFriendlyURLException)e;
					if( lfue.getType()==LayoutFriendlyURLException.DUPLICATE )
					{
						ErrorRaiser.throwIfError( IterErrorKeys.XYZ_E_LAYOUT_FRIENDLYURL_DUPLICATE_ZYX, friendlyUrl);
					}
				}
				
				throw e;
			}
			
			l.setFriendlyURL(friendlyUrl);
		}
		
		String htmlTitle = XMLHelper.getTextValueOf(d, String.format(_xpath, "@htmltitle") );
		if(htmlTitle!=null)
			l.setTitle(htmlTitle, LocaleUtil.getDefault());
		
		String hidden = XMLHelper.getTextValueOf(d, String.format(_xpath, "@hidden"));
		if(Validator.isNotNull(hidden))
			l.setHidden( GetterUtil.getBoolean(hidden, false) );
		
		l = _updateLayout(l, d, l.getGroupId(), GroupMgr.getDefaultUserId(), srcPlid, pagetemplateid);
		
		String livestatus = XMLHelper.getTextValueOf(d, String.format(_xpath, "@livestatus") );
		if(Validator.isNotNull(livestatus))
			LiveLocalServiceUtil.changeLiveStatus(l.getGroupId(), IterKeys.CLASSNAME_LAYOUT, String.valueOf(l.getPlid()), livestatus);
		
		Map<String, String> info = new HashMap<String, String>();
		info.put(FRIENDLY_URL,  l.getFriendlyURL() );
		
		return layoutInfo( info );
	}
	
	public void setGroupDefaultProperties(long groupId, String xml) throws Exception
	{
		ErrorRaiser.throwIfNull(groupId, IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		ErrorRaiser.throwIfFalse( Validator.isNotNull(xml), IterErrorKeys.XYZ_E_INVALIDARG_ZYX);
		
		Document d = SAXReaderUtil.read(xml);
		
		String dropdownmenu = XMLHelper.getTextValueOf(d, String.format(_xpath, "@dropdownmenu") );
		
		String header = XMLHelper.getTextValueOf(d, String.format(_xpath, "@header") );
		
		String footer = XMLHelper.getTextValueOf(d, String.format(_xpath, "@footer") );
		
		LayoutLocalServiceUtil.updateSectionProperties( groupId, 0L, dropdownmenu, header, footer, null, null );
	}
	
	private Layout _updateLayout(Layout layout, Document d, long groupId, long userId, long srcPlid, String pagetemplateid) throws Exception
	{
		long layoutPlid = layout.getPlid();
		// Si vale "0" es que queremos quitar el modelo anteriormente aplicado a la sección web. 
		//Este parámetro sustituye funcionalmente a los antiguos "loadPageTemplate" y "clearLayout"
		//(pagetemplateid<>0, significa loadPageTemplate y pagetemplateid=0, signifca clearLayout )
		if (srcPlid > 0 || Validator.isNotNull(pagetemplateid))
		{
			LayoutLocalServiceUtil.updateLayout(layout);
			
			if (Validator.isNotNull(pagetemplateid) && pagetemplateid.equals("0"))
				PageTemplateLocalServiceUtil.clearLayout(userId, layoutPlid);
			
			else if (Validator.isNotNull(pagetemplateid))
				PageTemplateLocalServiceUtil.loadPageTemplate(userId, groupId, Long.valueOf(pagetemplateid), layoutPlid);
			
			else
				copyLayout(srcPlid, layoutPlid, true, true);
			
			layout = LayoutLocalServiceUtil.getLayout(layoutPlid);
		}

		UnicodeProperties layoutTypeSettingsProperties = layout.getTypeSettingsProperties();
		
		UnicodeProperties newTypeSettingsProperties = getTypeSettingsProperties(groupId, d);
		
		if(newTypeSettingsProperties.size()>0)
		{
			for (Entry<String, String> property : newTypeSettingsProperties.entrySet())
			{
				if( Validator.isNotNull(property.getValue()) )
					layoutTypeSettingsProperties.setProperty(property.getKey(), property.getValue());
				else
					layoutTypeSettingsProperties.remove( property.getKey() );
			}
		
			layout.setTypeSettingsProperties(layoutTypeSettingsProperties);
		}
		
		String specificcss = XMLHelper.getTextValueOf(d, String.format(_xpath, "specificcss") );
		if( specificcss!=null )
			layout.setCss(specificcss);
		
		String autorss = XMLHelper.getTextValueOf(d, String.format(_xpath, "@autorss") );
		
		//Indica el elemento de catálogo para el menú desplegable. 
		
		
		String dropdownmenu = XMLHelper.getTextValueOf(d, String.format(_xpath, "@dropdownmenu") );
		
		//Es el Id del artículo instrumental "Acerca de" que describe la sección. 
		
		String aboutid = XMLHelper.getTextValueOf(d, String.format(_xpath, "@aboutid") );
		
		String header = XMLHelper.getTextValueOf(d, String.format(_xpath, "@header") );
		
		String footer = XMLHelper.getTextValueOf(d, String.format(_xpath, "@footer") );
		
		LayoutLocalServiceUtil.updateSectionProperties( groupId, layoutPlid, dropdownmenu, header, footer, aboutid, autorss );
		
		return LayoutLocalServiceUtil.updateLayout(layout);
	}
	
	private UnicodeProperties getTypeSettingsProperties(long groupId, Document d)
	{
		String languageId = LocaleUtil.toLanguageId(LocaleUtil.getDefault());
		UnicodeProperties properties = new UnicodeProperties(true);
		
		setPropertyValue(d, properties, String.format(IterKeys.META_ROBOTS, languageId),		"@robots" );
		setPropertyValue(d, properties, IterKeys.SITEMAP_INCLUDE,								"@sitemapincluded" );
		setPropertyValue(d, properties, IterKeys.SITEMAP_PRIORITY,								"@priority" );
		setPropertyValue(d, properties, IterKeys.SITEMAP_CHANGE_FREQUENCY,						"@changefrequency" );
		setPropertyValue(d, properties, String.format(IterKeys.META_KEYWORDS, languageId),		"keywords" );
		setPropertyValue(d, properties, IterKeys.JAVASCRIPT_1, 									"javascript1" );
		setPropertyValue(d, properties, IterKeys.JAVASCRIPT_2, 									"javascript2" );
		setPropertyValue(d, properties, IterKeys.JAVASCRIPT_3, 									"javascript3" );
		setPropertyValue(d, properties, String.format(IterKeys.META_DESCRIPTION, languageId),	"description" );
		setPropertyValue(d, properties, IterKeys.TARGET, 										"@target" );
		//Es la URL de la web a incrustar (cuando la página es de tipo "web incrustada")
		setPropertyValue(d, properties, IterKeys.URL, 											"@embedurl" );
		//Es la URL donde saltar (cuando la página es de tipo "enlace a URL")
		setPropertyValue(d, properties, IterKeys.URL, 											"@jumpurl" );

		//Es el layout Id de la página de destino (cuando la página es de tipo "enlace a página")
		Node property = d.selectSingleNode(String.format(_xpath, "@redirectlayout"));
		if(property!=null)
		{
			String redirectlayout = GetterUtil.getString(property.getStringValue(), StringPool.BLANK);
			if( Validator.isNotNull(redirectlayout) )
			{
				properties.setProperty(IterKeys.LINK_TO_LAYOUT_ID, redirectlayout);
				properties.setProperty(IterKeys.GROUPID, String.valueOf(groupId));
				properties.setProperty(IterKeys.PRIVATE_LAYOUT, "false");
			}
			else
			{
				properties.setProperty(IterKeys.LINK_TO_LAYOUT_ID, StringPool.BLANK);
				properties.setProperty(IterKeys.GROUPID, StringPool.BLANK);
				properties.setProperty(IterKeys.PRIVATE_LAYOUT, StringPool.BLANK);
			}
		}
		
		return properties;
	}
	
	private void setPropertyValue(Document doc, UnicodeProperties properties, String propertyName, String xpath)
	{
		Node property = doc.selectSingleNode(String.format(_xpath, xpath));
		
		if(property!=null)
		{
			String value = GetterUtil.getString(property.getStringValue(), StringPool.BLANK);
			properties.setProperty( propertyName, value );
		}
	}

	private Document layoutInfo(Map<String, String> info)
	{
		Document result = SAXReaderUtil.createDocument();
		Element rs = SAXReaderUtil.createElement("rs");
		Element row = rs.addElement("row");
		
		for( Entry<String, String> item : info.entrySet() )
			row.addAttribute( item.getKey(), item.getValue() );
		
		result.add(rs);
		
		return result;
	}
	
	private Map<Locale, String> getLocaleMap(String str)
	{
		Map<Locale, String> localeMap = new HashMap<Locale, String>();
		if(Validator.isNotNull(str))
		{
			Locale defaultLocale = LocaleUtil.getDefault();
			localeMap.put(defaultLocale, str);
		}
		
		return localeMap;
	}
	
	private void checkPageTemplateType(String pagetemplateid) throws ServiceError
	{
		String sql = String.format("SELECT type_ FROM designer_pagetemplate WHERE id_='%s'", pagetemplateid);
		List<Object> result = PortalLocalServiceUtil.executeQueryAsList(sql);
		if(result!=null)
		{
			ErrorRaiser.throwIfFalse(result.size()==1, IterErrorKeys.XYZ_E_PAGETEMPLATE_NOT_FOUND_ZYX);
			
			if( result.get(0)!=null )
			{
				String type = result.get(0).toString();
				ErrorRaiser.throwIfFalse(type.equalsIgnoreCase(TYPE_PAGE_TEMPLATE), IterErrorKeys.XYZ_E_INVALID_PAGETEMPLATE_TYPE_ZYX);
			}
		}
	}
	
	public Layout copyLayout(long srcPlid, long dstPlid, boolean copySectionProperties, boolean copyWebResources) throws PortalException, SystemException, IOException, SQLException, ReadOnlyException, ValidatorException 
	{
		long userId = GroupMgr.getDefaultUserId();
		
		// Copiamos el layout y borramos los portlets 
		Layout srcLayout = LayoutLocalServiceUtil.getLayout(srcPlid);
		Layout dstLayout = LayoutLocalServiceUtil.getLayout(dstPlid);
		dstLayout = removeLayoutPortlets(dstLayout);

		// Cambiamos el Layout Template
		LayoutTypePortlet ltp = (LayoutTypePortlet) srcLayout.getLayoutType();
		((LayoutTypePortlet) dstLayout.getLayoutType()).setLayoutTemplateId(0, ltp.getLayoutTemplateId(), false);

		// Cambiamos el tema y el esquema de color
		copyLookAndFeel(srcLayout, dstLayout);
		
		LayoutTemplate layoutTemplate = ltp.getLayoutTemplate();

		// Recorremos las columnas normales
		for (String column : layoutTemplate.getColumns()) 
		{
			int i = 0;
			for (Portlet portlet : ltp.getAllPortlets(column)) 
			{
				//Si es un anidador de portlets lo pasamos como una instancia
				String pluginId =  portlet.getPluginId();	
				if (pluginId.equals(PortletKeys.NESTED_PORTLETS))
				{
					pluginId = portlet.getPortletId();
				}
				String portletId = ((LayoutTypePortlet) dstLayout.getLayoutType()).addPortletId(userId, pluginId, column, i, false);
				dstLayout.setTypeSettings( dstLayout.getTypeSettings() );
				
				copyPreferences(srcLayout, dstLayout, portlet.getPortletId(), portletId);
				i++;
			}
		}
		
		// Recorremos las columnas anidadas
		List<String> nestedColumnIdList = ListUtil.fromArray(StringUtil.split(srcLayout.getTypeSettingsProperties().getProperty(LayoutTypePortletConstants.NESTED_COLUMN_IDS)));

		for (String nestedColumn : nestedColumnIdList)
		{
			int i = 0;
			for (Portlet portlet : ltp.getAllPortlets(nestedColumn)) 
			{
				// Si es un anidador de portlets lo pasamos como una instancia
				String pluginId =  portlet.getPluginId();
				if (pluginId.equals(PortletKeys.NESTED_PORTLETS))
				{
					pluginId = portlet.getPortletId();
				}
				
				String portletId = ((LayoutTypePortlet) dstLayout.getLayoutType()).addPortletId(userId, pluginId, nestedColumn, i, false);
				dstLayout.setTypeSettings( dstLayout.getTypeSettings() );
				
				copyPreferences(srcLayout, dstLayout, portlet.getPortletId(), portletId);
				i++;
			}
		}
		
		if (copySectionProperties)
			copySectionProperties(srcPlid, dstPlid);
		
		// Se copian los recursos del tema
		if (copyWebResources)
			ThemeWebResourcesLocalServiceUtil.copyWebResources(srcPlid, dstPlid);
		
		dstLayout = LayoutLocalServiceUtil.updateLayout(dstLayout, false);
		return dstLayout;
	}
	
	private void copySectionProperties(long srcPlid, long dstPlid) throws IOException, SQLException
	{
		String sql = String.format(COPY_SECTION_PROPERTIES, dstPlid, srcPlid);
		PortalLocalServiceUtil.executeUpdateQuery(sql);
	}
	private void copyLookAndFeel(Layout srcLayout, Layout dstLayout)
	{
		dstLayout.setThemeId(srcLayout.getThemeId());
		dstLayout.setColorSchemeId(srcLayout.getColorSchemeId());
		dstLayout.setCss(srcLayout.getCss());
	}

	/**
	 * @param srcLayout
	 * @param dstLayout
	 * @param srcPortletId
	 * @param dstPortletId
	 * @throws SystemException
	 * @throws ReadOnlyException
	 * @throws IOException 
	 * @throws ValidatorException 
	 */
	private void copyPreferences(Layout srcLayout, Layout dstLayout, String srcPortletId, String dstPortletId) throws SystemException, ReadOnlyException, ValidatorException, IOException
	{
		PortletPreferences srcPref = PortletPreferencesFactoryUtil.getLayoutPortletSetup(srcLayout, srcPortletId);
		PortletPreferences dstPref = PortletPreferencesFactoryUtil.getLayoutPortletSetup(dstLayout, dstPortletId);

		Iterator<Entry<String, String[]>> it = srcPref.getMap().entrySet().iterator();
		
		while (it.hasNext()) 
		{
			Entry<String, String[]> entry = it.next();
			dstPref.setValues(entry.getKey(), entry.getValue());
		}
		dstPref.store();
	}

	/**
	 * 
	 * @param layout
	 * @return
	 * @throws PortalException
	 * @throws SystemException
	 */
	public Layout removeLayoutPortlets(Layout layout) throws PortalException, SystemException 
	{
		List<Portlet> portlets = ((LayoutTypePortlet)layout.getLayoutType()).getPortlets();
		
		for (Portlet portlet : portlets) 
		{	
			((LayoutTypePortlet)layout.getLayoutType()).removePortletId(GroupMgr.getDefaultUserId(), portlet.getPortletId());

            String rootPortletId = PortletConstants.getRootPortletId(portlet.getPortletId());
            ResourceLocalServiceUtil.deleteResource(layout.getCompanyId(), rootPortletId,
                    ResourceConstants.SCOPE_INDIVIDUAL,
                    PortletPermissionUtil.getPrimaryKey(layout.getPlid(), portlet.getPortletId()));
		}
		
		// Se fuerza una actualización del TypeSettings(superpufo: 0006727)
        layout.setTypeSettings( layout.getTypeSettings() );
        layout = LayoutLocalServiceUtil.updateLayout(layout, false);
        
		return layout;
	}
	
	/**
	 * 0010688: Proporcionar acceso a MLN para obtener las friendlyURL de modelos y secciones
	 * 
	 * @param xmlListIds
	 * @return
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws DocumentException
	 */
	public Document getLayoutsFriendlyURLs(String xmlListIds) throws SecurityException, NoSuchMethodException, DocumentException
	{
		Document dom = SAXReaderUtil.read(xmlListIds);
		final String XPATH = "/rs/row[@type='%s']/@id";
		
		StringBuilder sql = new StringBuilder(GET_LAYOUTS_FRIENDLYURLS);
		
		// Se obtiene el listado de modelos
		List<Node> modelList = dom.selectNodes( String.format(XPATH, LayoutConstants.CLASS_MODEL) );
		if (modelList.size() > 0)
		{
			sql.append( String.format(" WHERE Designer_PageTemplate.id_ IN (%s)\n", StringUtils.join(XMLHelper.getStringValues(modelList),',')) );
		}
		
		// Se obtiene la lista de secciones
		List<Node> layoutList= dom.selectNodes( String.format(XPATH, LayoutConstants.CLASS_LAYOUT) );
		if (layoutList.size() > 0)
		{
			sql.append( (modelList.size() == 0) ? "	WHERE " : "	OR " );
			sql.append( String.format(" Layout.plid IN (%s)\n", StringUtils.join(XMLHelper.getStringValues(layoutList),',')) );
		}
		
		String query = sql.toString();
		
		_log.debug(query);
		return PortalLocalServiceUtil.executeQueryAsDom(query);
	}

}