package com.protecmedia.portal.service;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.portlet.ReadOnlyException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import com.liferay.portal.NoSuchPortletPreferencesException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.IterGlobal;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.kernel.xml.XSLUtil;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutConstants;
import com.liferay.portal.model.PortletConstants;
import com.liferay.portal.model.PortletPreferences;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.PortletPreferencesLocalService;
import com.liferay.portal.service.PortletPreferencesLocalServiceUtil;
import com.liferay.portal.service.PortletPreferencesLocalServiceWrapper;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portal.util.PortletPreferencesTools;
import com.protecmedia.iter.base.service.util.IterKeys;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.xmlio.service.item.portal.LayoutXmlIO;
import com.protecmedia.portal.util.PortletPreferencesSort;


public class MyPortletPreferencesLocalService extends PortletPreferencesLocalServiceWrapper
{

	private static Log _log		= LogFactoryUtil.getLog(MyPortletPreferencesLocalService.class);
	private static Log _logSort = LogFactoryUtil.getLog(MyPortletPreferencesLocalService.class.getName().concat(".sort"));
	
	private static String SEL_PORTLETS_PLID = String.format("SELECT plid FROM Layout WHERE type_ = '%s'", LayoutConstants.TYPE_PORTLET);
	
	public static final String PREFS_EMPTY						= "<portlet-preferences/>";
	
	public MyPortletPreferencesLocalService(PortletPreferencesLocalService portletPreferencesLocalService) 
	{
		super(portletPreferencesLocalService);
	}
	
	/*
	 * Add Functions 
	 * --------------
	 *  Cualquier modificación de la configuración de cualquier portlet de una página,
	 *  crea una entrada en live de tipo portlet para esa página con estado UPDATE/PENDING.
	 *  Si dicha entrada ya existe se actualiza. (No puede haber más de una entrada en live para portlets de la misma página)
	 */	
	@Override
	public PortletPreferences updatePreferences( long ownerId, int ownerType, long plid, String portletId, String xml ) throws SystemException 
	{
		return updatePreferences( ownerId, ownerType, plid, portletId, xml, true );
	}
	private PortletPreferences updatePreferences( long ownerId, int ownerType, long plid, String portletId, String xml, boolean update2Pending ) throws SystemException 
	{
		PortletPreferences result = null;
		
		try
		{
			if ((!portletId.equals("LIFERAY_PORTAL") && ownerType != PortletKeys.PREFS_OWNER_TYPE_ORGANIZATION && plid > 0))
			{
				// Por el hecho de guardarse el portlet se pone a pending
				if (update2Pending)
					updateXMLIO2Pending(plid);
			
				// Se comprueba si es un portlet vinculado a un PortletItem
				Document doc			= SAXReaderUtil.read(xml);
				String portletItemUUID	= XMLHelper.getTextValueOf(doc, PortletPreferencesTools.PREFS_PORTLETITEM_VALUE_XPATH);
				if (portletItemUUID != null)
				{
					Node nodePortletItem = doc.selectSingleNode( PortletPreferencesTools.PREFS_PORTLETITEM_NODE_XPATH ).detach();
					
					long portletItemOwnerId = getPortletItemId(portletItemUUID);
					if (portletItemOwnerId > 0)
					{
						Document docNewPref 	= SAXReaderUtil.read(PREFS_EMPTY);
						
						// Se elimina el nodo 'portletItem' de las preferencias del PortletItem, y se añade a las preferencias del portlet	
						docNewPref.getRootElement().add( nodePortletItem );
						
						// Además, MUY IMPORTANTE, si existe un nodo refPreference es porque se acaba de vincular las preferencias de este Portlet con el PortletItem.
						// En dicho caso NO hará falta las preferencias de referencia y se borran
						Node nodeRefPreference = doc.selectSingleNode( "/portlet-preferences/preference[name='refPreference']" );
						if (nodeRefPreference != null)
						{
							String refPreferenceId = XMLHelper.getTextValueOf(nodeRefPreference, "value");
							if (refPreferenceId != null)
							{
								try
								{
									PortletPreferencesLocalServiceUtil.deletePortletPreferences(PortletKeys.PREFS_OWNER_ID_DEFAULT, PortletKeys.PREFS_OWNER_TYPE_ORGANIZATION, plid, refPreferenceId);
								}
								catch (NoSuchPortletPreferencesException nsppe)
								{
									_log.debug(nsppe.toString());
								}
							}
							
							nodeRefPreference.detach();
						}
						
						// Si algún portlet está vinculado a este PortletItem. el PortletItem NO podrá tener una 'Sección Actual' configurada
						PortletPreferencesTools.checkDefaultLayoutPreferences(doc);
						
						// Se guardan las preferencias en el PortletItem
						updatePreferences(portletItemOwnerId, PortletKeys.PREFS_OWNER_TYPE_ARCHIVED, 0, PortletConstants.getRootPortletId(portletId), doc.asXML());
						
						// Se actualiza el XML que se salvará como Preferencias del portlet
						xml = docNewPref.asXML(); 
					}
					else
					{
						// Si no existe el PortletItem se guardan las preferencias directamente en el portlet. No existirá vinculación.
						xml = doc.asXML();
					}
				}
			
				if (portletItemUUID == null)
					xml = processRefPreferences(ownerId, ownerType, plid, portletId, xml);
			}
			
			Document doc = SAXReaderUtil.read(xml);
			checkOwnerTypeArchived(ownerType, doc);
			
			checkIncludeCurrentContent(doc, ownerType, plid, portletId);
			
			// Se añade la versión de ITERWebCMS con que fue guardada
			PortletPreferencesTools.updatePrefsValue(doc, IterKeys.PREFS_ITERWEBCMS_VERSION, IterGlobal.getIterWebCmsVersion());
			
			// Se ordenan los campos, muy importante para que las mismas preferencias, aunque sean de distintos portlets, den el mismo MD5 
			String xslpath = new StringBuilder(File.separator).append("iter-hook")
								.append(File.separatorChar).append("xsl")
								.append(File.separatorChar).append("sortPreferences.xsl").toString();

			Transformer transformer = XSLUtil.getTransformer(XSLUtil.getSourceByLocalPath(xslpath), "xml", null);
			transformer.setOutputProperty(OutputKeys.INDENT, "no");
			xml = XSLUtil.transform(XSLUtil.getSource(doc.asXML()), transformer);

			result = super.updatePreferences(ownerId, ownerType, plid, portletId, xml);
		}
		catch (UnsupportedEncodingException uee)
		{
			throw new SystemException(uee);
		}
		catch (TransformerException te)
		{
			throw new SystemException(te);
		}
		catch (PortalException pe)
		{
			throw new SystemException(pe);
		}
		catch (ServiceError se)
		{
			throw new SystemException(se);
		}
		catch (com.liferay.portal.kernel.error.ServiceError se)
		{
			throw new SystemException(se);
		}
		catch (NoSuchMethodException nsme)
		{
			throw new SystemException(nsme);
		}
		catch (DocumentException e)
		{
			throw new SystemException(e);
		}

		return result;
	}

	/**
	 * 
	 */
	public javax.portlet.PortletPreferences getPreferences(long companyId, long ownerId, int ownerType, long plid,
														   String portletId, String defaultPreferences) throws SystemException 
	{
		javax.portlet.PortletPreferences preferences = super.getPreferences(companyId, ownerId, ownerType, plid, portletId, defaultPreferences);
		boolean enableSharedPreferences = GetterUtil.getBoolean( PropsUtil.get(PropsKeys.ITER_SHAREDPREFERENCES_ENABLED), true);
		
		if (enableSharedPreferences && !portletId.equals("LIFERAY_PORTAL") && ownerType != 6 && plid > 0)
		{
			try
			{
				// Se comprueba si la preferencia es un vínculo a un PortletItem, y en tal caso se devuelven las preferencias de dicho PortletItem
				String portletItemUUID = preferences.getValue(IterKeys.PREFS_PORTLETITEM, null);
				
				if (Validator.isNotNull(portletItemUUID))
				{
					// Se obtienen las preferencias del PortletItem
					long portletItemOwnerId = getPortletItemId(portletItemUUID);
					if (portletItemOwnerId > 0)
					{
						preferences = super.getPreferences(companyId, portletItemOwnerId, PortletKeys.PREFS_OWNER_TYPE_ARCHIVED, 0, PortletConstants.getRootPortletId(portletId), defaultPreferences);
		
						// A estas preferencias se le añade el PortletItem para que la interfaz sepa de donde viene
						preferences.setValue(IterKeys.PREFS_PORTLETITEM, portletItemUUID);
					}
					else
					{
						// Si el PortletItem NO existe se devuelven unas preferencias VACIAS
						preferences = PortletPreferencesLocalServiceUtil.fromDefaultXML(PREFS_EMPTY);
					}
				}
			}
			catch (ServiceError se)
			{
				throw new SystemException(se);
			}
			catch (com.liferay.portal.kernel.error.ServiceError se)
			{
				throw new SystemException(se);
			}
			catch (ReadOnlyException roe)
			{
				throw new SystemException(roe);
			}
			catch (NoSuchMethodException nsme)
			{
				throw new SystemException(nsme);
			}
		}
		
		checkIncludeCurrentContent(preferences, ownerType, plid, portletId);
		
		return preferences;
	}

	/**
	 * Método que añade a las preferencias el valor por defecto de la propiedad <code>listCurrentContent</code>
	 * @param preferences
	 * @param ownerId
	 * @param ownerType
	 * @param plid
	 * @param portletId
	 * @throws SystemException 
	 * @throws ReadOnlyException 
	 */
	private void checkIncludeCurrentContent(javax.portlet.PortletPreferences preferences, int ownerType, long plid, String portletId) throws SystemException
	{
		ThemeDisplay themeDisplay = ((ThemeDisplay)IterRequest.getAttribute(WebKeys.THEME_DISPLAY));
		
		// Si está autentificado, es un TeaserViewer y no tiene la preferencia
		if (PropsValues.IS_PREVIEW_ENVIRONMENT																						&&
			themeDisplay != null && themeDisplay.isSignedIn() && ownerType != PortletKeys.PREFS_OWNER_TYPE_ORGANIZATION && plid > 0 &&
			portletId.startsWith(PortletKeys.PORTLET_TEASERVIEWER) 																	&& 
			preferences.getValue(PortletKeys.PREFS_LIST_CURRENT_CONTENT, null) == null)
		{
			try
			{
				preferences.setValue( PortletKeys.PREFS_LIST_CURRENT_CONTENT, String.valueOf(LayoutConstants.isCatalog(plid)) );
			}
			catch (ReadOnlyException roe)
			{
				throw new SystemException(roe);
			}
		}
	}
	
	private void checkIncludeCurrentContent(Document preferences, int ownerType, long plid, String portletId)
	{
		// Si es un TeaserViewer y no tiene la preferencia
		if (PropsValues.IS_PREVIEW_ENVIRONMENT 									&&
			ownerType != PortletKeys.PREFS_OWNER_TYPE_ORGANIZATION && plid > 0 	&&
			portletId.startsWith(PortletKeys.PORTLET_TEASERVIEWER) 				&& 
			XMLHelper.getTextValueOf(preferences, PortletPreferencesTools.PREF_VALUE_XPATH_LIST_CURRENT_CONTENT) == null)
		{
			PortletPreferencesTools.updatePrefsValue(preferences, PortletKeys.PREFS_LIST_CURRENT_CONTENT, String.valueOf(LayoutConstants.isCatalog(plid)));
		}
	}
	
	/**
	 * 
	 * @param ownerType
	 * @param xml
	 * @return
	 * @throws DocumentException 
	 * @throws ServiceError 
	 */
	private void checkOwnerTypeArchived(long ownerType, Document doc) throws DocumentException, ServiceError
	{
		// Las preferencias de tipo PREFS_OWNER_TYPE_ARCHIVED son las asociadas a los PortletItems que se utilizan como platillas.
		if (ownerType == PortletKeys.PREFS_OWNER_TYPE_ARCHIVED)
		{
			// Estas NO podrán tener referencia alguna a nodos portletItem o refPreference
			Node nodeRefPreference = doc.selectSingleNode( "/portlet-preferences/preference[name='refPreference']" );
			if (nodeRefPreference != null)
				nodeRefPreference.detach();

			Node nodePortletItem = doc.selectSingleNode( PortletPreferencesTools.PREFS_PORTLETITEM_NODE_XPATH );
			if (nodePortletItem != null)
				nodePortletItem.detach();
		}
	}
	
	/**
	 * @throws DocumentException 
	 * @throws SystemException 
	 * 
	 */
	private String processRefPreferences(long ownerId, int ownerType, long plid, String portletId, String xml) throws DocumentException, SystemException
	{
		Document doc = SAXReaderUtil.read(xml);
		
		boolean isRankingPortlet 		= StringUtil.startsWith(portletId, IterKeys.PORTLETID_RANKING);
		boolean isRelatedPortlet 		= StringUtil.startsWith(portletId, IterKeys.PORTLETID_RELATED);
		boolean isTeaserViewerPortlet 	= StringUtil.startsWith(portletId, IterKeys.PORTLETID_TEASER) || StringUtil.startsWith(portletId, PortletKeys.PORTLET_TEASERVIEWERNR);
		
		if (isRankingPortlet || isRelatedPortlet || isTeaserViewerPortlet)
		{
			boolean addRefPreferences = isRankingPortlet;
			
			// Si no es de Ranking será un Related o un TeaserViewer, y en tal caso tendrán que ser paginados
			if (!addRefPreferences)
				addRefPreferences = GetterUtil.getBoolean( XMLHelper.getTextValueOf(doc, "/portlet-preferences/preference[name='paged']/value/text()"), false);
			
			// Los filtros en los teasers son por AJAX, también necesitan el portlet de referencia
			if (!addRefPreferences && isTeaserViewerPortlet)
				addRefPreferences = !IterKeys.WITHOUT_FILTER.equals(XMLHelper.getTextValueOf(doc, "/portlet-preferences/preference[name='filterBy']/value/text()", IterKeys.WITHOUT_FILTER));
				
			if (addRefPreferences)
			{
				String refPreferenceId = portletId;
				
				// Se guarda, si no lo está ya, la preferencia de referencia utilizada
				Node nodeRefPreference = doc.selectSingleNode( "/portlet-preferences/preference[name='refPreference']" );
				
				if (nodeRefPreference != null)
				{
					refPreferenceId = XMLHelper.getTextValueOf(nodeRefPreference, "value");
					
					if (refPreferenceId == null)
					{
						nodeRefPreference.detach();
						nodeRefPreference = null;
					}
				}
				
				// Si nunca ha existido o si existía pero estaba mal formado
				if (nodeRefPreference == null)
				{
					refPreferenceId = portletId;
					Element preferenceElement = doc.getRootElement().addElement("preference");
					preferenceElement.addElement("name").addText("refPreference");
					preferenceElement.addElement("value").addText(refPreferenceId);
					xml = doc.asXML();
				}

				// Se actualiza la preferencia de referencia
				super.updatePreferences(PortletKeys.PREFS_OWNER_ID_DEFAULT, 6, plid, refPreferenceId, xml);
			}
		}
		
		return xml;
	}
	
	/**
	 * @throws com.liferay.portal.kernel.error.ServiceError 
	 */
	private Long getPortletItemId(String portletItemUUID) throws SecurityException, NoSuchMethodException, com.liferay.portal.kernel.error.ServiceError
	{
		Document portletItemDom = PortletPreferencesTools.getPortletItem(portletItemUUID);
		return XMLHelper.getLongValueOf(portletItemDom, "/rs/row/@portletItemId']");
	}
	
	/**
	 * @throws SystemException 
	 * @throws PortalException 
	 * 
	 */
	private void updateXMLIO2Pending(long plid)
	{
		try
		{
			Layout layout = LayoutLocalServiceUtil.getLayout(plid);
			LayoutXmlIO layoutXmlIO = new LayoutXmlIO();
			layoutXmlIO.createLiveEntryPortlet(layout);	
		}
		catch(Exception e)
		{
			if (PropsValues.IS_PREVIEW_ENVIRONMENT)
				_log.error("Live error", e);
			else
				_log.debug("Live error", e);
		}

	}
	
	/**
	 * Ordena las preferencias del sistema.
	 * @throws SystemException
	 */
	public void sortPreferences() throws SystemException
	{
		_logSort.info("sortPreferences: Start");
		
		List<Object> plidList = PortalLocalServiceUtil.executeQueryAsList(SEL_PORTLETS_PLID);
		for (Object plid : plidList)
		{
			// La actualización se lanza en un hilo independiente para que se cree una transacción por página (plid)
			try
			{
				PortletPreferencesSort.sort( Long.parseLong(String.valueOf(plid)) );
			}
			catch (InterruptedException ie)
			{
				throw new SystemException(ie);
			}
		}
		
		_logSort.info("sortPreferences: Finish");
	}
	
	/**
	 * Obtiene las preferencias asociadas al plid y las guarda para forzar que estas reordenen sus campos
	 * @param plid
	 * @throws SystemException
	 */
	public void sortPreferencesByPlid(long plid) throws SystemException
	{
		if (_logSort.isDebugEnabled())
			_logSort.debug( String.format("sortPreferencesByPlid(%d): Start", plid) );
		
		List<PortletPreferences> list = getPortletPreferencesByPlid(plid);
		
		for (PortletPreferences preferences : list)
		{
			if (_logSort.isTraceEnabled())
				_logSort.trace( String.format("sortPreferences: Before updating preference (ownerType:%s,\tplid:%d,\tportletId:%s)",
										 preferences.getOwnerType(), preferences.getPlid(), preferences.getPortletId()) );
			
			updatePreferences(preferences.getOwnerId(),		preferences.getOwnerType(),		preferences.getPlid(), 
							  preferences.getPortletId(), 	preferences.getPreferences(), 	false);
		}
		
		if (_logSort.isDebugEnabled())
			_logSort.debug( String.format("sortPreferencesByPlid(%d): Finish", plid) );
	}
}






	


	

	