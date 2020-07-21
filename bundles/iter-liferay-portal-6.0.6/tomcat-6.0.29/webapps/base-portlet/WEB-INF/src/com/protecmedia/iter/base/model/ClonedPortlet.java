package com.protecmedia.iter.base.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.portlet.PortletMode;
import javax.portlet.WindowState;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.PortletBag;
import com.liferay.portal.kernel.portlet.PortletBagPool;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.PortletConstants;
import com.liferay.portal.model.PortletInfo;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.liferay.portal.service.PortletLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.InvokerPortlet;
import com.liferay.portlet.PortletInstanceFactoryUtil;
import com.protecmedia.iter.base.service.util.ErrorRaiser;

public class ClonedPortlet 
{
	private static Log _log = LogFactoryUtil.getLog(ClonedPortlet.class);
	
	private static final String INSTANCE_SERVICE_METHOD	= "getPortletInstance";
	private static final String GET_CLONES 				= "SELECT * FROM Base_ClonedPortlet"; 
	
	private static final String DEFAULT_PREFERENCES		= "<?xml version='1.0' encoding='UTF-8'?><portlet-preferences/>";

	private String	_cloneId;
	private long	_companyId;
	private String	_name;
	private String 	_defaultPreferences;
	private String	_portletId;
	private String 	_portletCategory;
	private String	_instanceServiceClass;
	
	private static ConcurrentHashMap<String, Portlet> _portletsPool = new ConcurrentHashMap<String, Portlet>();
	
	public ClonedPortlet(Node node) throws PortalException, SystemException
	{
		_companyId 				= CompanyLocalServiceUtil.getCompanies().get(0).getCompanyId();
		_cloneId 				= XMLHelper.getTextValueOf(node, "cloneId/text()");
		_name 					= XMLHelper.getTextValueOf(node, "NAME/text()");
		setDefaultPreferences( 	  XMLHelper.getTextValueOf(node, "defaultPreferences/text()", DEFAULT_PREFERENCES) );
		_portletId				= XMLHelper.getTextValueOf(node, "portletId/text()");
		_portletCategory		= XMLHelper.getTextValueOf(node, "portletCategory/text()");
		_instanceServiceClass	= XMLHelper.getTextValueOf(node, "instanceServiceClass/text()");
	}
	
	public String getCloneId()
	{
		return _cloneId;
	}
	public long getCompanyId()
	{
		return _companyId;
	}
	public String getName()
	{
		return _name;
	}
	private String getInstanceServiceClass()
	{
		return _instanceServiceClass;
	}	
	
	private void setDefaultPreferences(String preferences)
	{
		_defaultPreferences = checkPreferences(preferences);
	}
	private String checkPreferences(String preferences)
	{
		if (preferences == null || preferences.length() == 0)
			preferences = DEFAULT_PREFERENCES;

		return preferences;
	}
	public String getDefaultPreferences()
	{
		return _defaultPreferences;
	}
	public String getPortletId()
	{
		return _portletId;
	}
	public String getPortletCategory()
	{
		return _portletCategory;
	}

	
	/**
	 * Lee de BBDD los portlets clonados y devuelve una lista de ClonedPortlet
	 * @return lista de ClonedPortlet del sistema
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws SystemException 
	 * @throws PortalException 
	 */
	public static List<ClonedPortlet> readClonesFromDB() throws SecurityException, NoSuchMethodException, PortalException, SystemException
	{
		List<ClonedPortlet> result = new ArrayList<ClonedPortlet>();
		
		Document doc = PortalLocalServiceUtil.executeQueryAsDom(GET_CLONES, false, XMLHelper.rsTagName, XMLHelper.rowTagName);
		List<Node> nodes = doc.selectNodes(String.format("//%s", XMLHelper.rowTagName));
		
		for (Node node : nodes)
		{
			result.add( new ClonedPortlet(node) );
		}

		return result;
	}

	public Portlet getPortlet() throws Throwable
	{
		Portlet portlet = _portletsPool.get( getCloneId() );
		
		if (portlet == null)
		{
			String portletId = PortalUtil.getJsSafePortletId( String.format("%s%d%s%s%s%s", 
															  PortletConstants.CLONEDPORTLET_PREFIX, getCompanyId(), 
															  StringPool.UNDERLINE, getCloneId(), 
															  StringPool.UNDERLINE, getPortletId()) );
	
			portlet = PortletLocalServiceUtil.clonePortlet( getCompanyId(), getPortletId());
	
			String clonedPortletId		= PortalUtil.getJsSafePortletId( portlet.getPortletId() );
			String clonedPortletName 	= PortalUtil.getJsSafePortletId( portlet.getPortletName() );
			
			portlet.setPortletId(portletId);
			portlet.setTimestamp(System.currentTimeMillis());
			portlet.setPortletName(	portletId );
			portlet.setDisplayName(	portletId );
			// El setter es necesario para que internamente el portletApp añada dicho portlet (_portletApp.addPortlet(this))
			portlet.setPortletApp( portlet.getPortletApp() );

			// Se añaden las preferencias por defecto si se indican unas EXPLÍCITAMENTE.
			String preferences = getDefaultPreferences();
			if (preferences != null && !preferences.isEmpty())
				portlet.setDefaultPreferences(preferences);
	

			// InitParams
			Map<String, String> initParams = portlet.getInitParams();
			initParams.put( InvokerPortlet.INIT_INVOKER_PORTLET_NAME, clonedPortletName);
	
			// Extra info
			addPortletExtraInfo(portlet, getName());
	
				
			// PortletBag
			PortletBag portletBag = (PortletBag)PortletBagPool.get( getPortletId() ).clone();
	
//			Portlet tmpPortlet 				= PortletLocalServiceUtil.getPortletById( clonedPortletId );
//			PortletBag tmpPortletBag 		= PortletBagPool.get( clonedPortletId );
//			InvokerPortlet invoker 			= PortletInstanceFactoryUtil.create(tmpPortlet, tmpPortletBag.getServletContext());
//			javax.portlet.Portlet instance 	= invoker.getPortlet();
//			portletBag.setPortletInstance(instance);
			

			portletBag.setPortletName(portletId);
			portletBag.setPortletInstance(  getPortletInstance(portlet.getPortletClass())  );
			
			PortletBagPool.put(portletId, portletBag);
			
			
			// Set in map
			_portletsPool.put(getCloneId(), portlet);
			
		}
		return portlet;
	}
	
	public void initClone() throws Throwable
	{
		PortletLocalServiceUtil.deployRemotePortlet(getPortlet(), getPortletCategory());
	}
	
	/**
	 * Elimina el portlet de la lista de Portlets disponibles
	 * @throws Throwable
	 */
	public void destroyClone() throws Throwable
	{
		Portlet portlet = getPortlet();
		PortletLocalServiceUtil.destroyRemotePortlet(portlet);
		PortletInstanceFactoryUtil.destroy(portlet);
	}
	
	/**
	 * 
	 * @param portlet
	 * @param portletApp
	 * @param title
	 */
	private void addPortletExtraInfo(Portlet portlet, String title) 
	{
		// Modes
		Set<String> mimeTypePortletModes = new HashSet<String>();
		
		mimeTypePortletModes.add(PortletMode.VIEW.toString());
		
		portlet.getPortletModes().put(ContentTypes.TEXT_HTML, mimeTypePortletModes);

		
		// Window states
		Set<String> mimeTypeWindowStates = new HashSet<String>();

		mimeTypeWindowStates.add(WindowState.MAXIMIZED.toString());
		mimeTypeWindowStates.add(WindowState.MINIMIZED.toString());
		mimeTypeWindowStates.add(WindowState.NORMAL.toString());

		portlet.getWindowStates().put(ContentTypes.TEXT_HTML, mimeTypeWindowStates);

		
		// PortletInfo
		PortletInfo portletInfo = new PortletInfo(title, title, title, title);
		portlet.setPortletInfo(portletInfo);
	}
	
	/**
	 * 
	 * @return
	 * @throws Throwable 
	 */
	private javax.portlet.Portlet getPortletInstance(String instanceClass) throws Throwable
	{
		javax.portlet.Portlet result = null;
		
		Class<?> comObject	= Class.forName(getInstanceServiceClass());
		Method[] methods	= comObject.getDeclaredMethods();
		Method m = null;

		for (int i = 0; i < methods.length; i++)
		{
			if ( methods[i].getName().equals(INSTANCE_SERVICE_METHOD) )
			{
				m = methods[i];
				break;
			}
		}
		
		ErrorRaiser.throwIfNull(m, "METHOD NOT FOUND");
		List<Object> methodParams = new ArrayList<Object>();
		methodParams.add( instanceClass/*getInstanceClass()*/ );
		
		try
		{
			result = (javax.portlet.Portlet)m.invoke(null, methodParams.toArray());
		}
		catch(InvocationTargetException ite)
		{
			 Throwable th = ite.getTargetException();
			 throw th;
		}
		
		return result;
	}
}