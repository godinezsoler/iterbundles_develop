package com.protecmedia.iter.news.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.NoSuchLayoutException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.CatalogUtil;
import com.liferay.portal.kernel.util.PropsValues;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.util.request.IterRequest;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;
import com.liferay.portal.model.Layout;
import com.liferay.portal.service.LayoutLocalServiceUtil;
import com.liferay.portal.theme.MenuItem;
import com.liferay.portal.theme.RequestVars;
import com.liferay.portal.util.HtmlOptimizer;
import com.liferay.portal.util.PortalUtil;
import com.protecmedia.iter.base.service.util.VelocityUtil;

public class MenuUtil {
	
	private static Log _log = LogFactoryUtil.getLog(MenuUtil.class);		
		
	public static void getMenu(String portletId, String menuType, String layoutId, boolean useActualLayout, RequestVars requestVars, 
		                       HttpServletResponse response, String orientation, int levels, boolean onlyChildren, boolean desplegado, 
		                       boolean withMegaMenu, boolean useCurrentLayoutParent, boolean mainSiteNavigation) throws Exception
    {		
		_log.trace("In MenuUtil.getMenu"); 
		
		boolean error = false;	
		// menuItems a pintar
		List<MenuItem> menuItemsToBeDrawn = null;		
		MenuItem menuItemSelected         = null;
		
		// Todos los menuItem
		HttpServletRequest originalRequest = PortalUtil.getOriginalServletRequest(requestVars.getRequest());
		@SuppressWarnings("unchecked")
		// Vemos si la lista de todos los items está ya como atributo en el request
		List<MenuItem> allMenuItems = (List<MenuItem>) originalRequest.getAttribute(WebKeys.REQUEST_ATTRIBUTE_ALL_MENU_ITEMS);
		
		// No llegan los menuItems porque se acaba de configurar el portlet de menú y se inicia una nueva llamada que no pasa por VelocityVariables metiendo en IterRequest los menuItems
		if (null == allMenuItems || allMenuItems.size() == 0)
		{ 
			_log.debug("The menu items (all) are not in the request attribute");
			
			allMenuItems = MenuItem.getAllMenuItem(requestVars);			
			
			_log.debug("Before setting all menu items into a request attribute");			
			originalRequest.setAttribute(WebKeys.REQUEST_ATTRIBUTE_ALL_MENU_ITEMS, allMenuItems);
			_log.debug("After setting all menu items into a request attribute");
		}
		
		
		// Seccion actual o seccion indicada
		if (useActualLayout || Validator.isNotNull(layoutId) || useCurrentLayoutParent)
		{
			Layout auxLayout = null;
			long layoutPlid = -1L;
			
			// Sección actual
			if (useActualLayout)
			{
				_log.debug("Looking for actual layout");
				
				// Cualquier menuItem nos dice cual es el layout actual, utilizamos el primero
				auxLayout = MenuItem.getCurrentLayout(requestVars);
				if(auxLayout!=null)
					layoutPlid = auxLayout.getPlid();
			}
			else if (useCurrentLayoutParent)
			{
				//Seccion padre de la actual
				_log.debug("Looking for current layout parent");
				auxLayout = MenuItem.getCurrentLayout(requestVars);
				
				long parentLayoutId = auxLayout.getParentLayoutId();
				if(parentLayoutId!=0)
				{
					try
					{
						auxLayout = LayoutLocalServiceUtil.getLayout(requestVars.getThemeDisplay().getScopeGroupId(), false, parentLayoutId);
						layoutPlid = auxLayout.getPlid();
					}
					catch (NoSuchLayoutException nsle) 
					{
						_log.error(nsle);
					}
				}
				else
					layoutPlid = 0L;
				
			}
			else
			{
				// Sección indicada
				if(_log.isDebugEnabled())
					_log.debug(new StringBuilder("The layout: '").append(layoutId).append("' is requested") );
				
				try
				{
					auxLayout = LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(layoutId, requestVars.getThemeDisplay().getScopeGroupId());
					layoutPlid = auxLayout.getPlid();
				}
				catch (NoSuchLayoutException nsle) 
				{
					_log.error(nsle);
				}
			}
			
			if (layoutPlid!=0 && layoutPlid!=-1)
			{
				// Buscamos el menuItem correspondiente al layout solicitado				
				_log.debug("Looking for the menuItem that corresponds to the layout found");				
				final MenuItem menuItemFound = MenuItem.findMenuItem(allMenuItems, layoutPlid);
				
				if (null != menuItemFound)
				{				
					if(_log.isDebugEnabled())
						_log.debug(new StringBuffer("menuItem found: '").append(menuItemFound.getPlid()).append("' (plid)") );
					
					// Solo se quieren los hijos del menuItem solicitado
					if (onlyChildren)
					{
						_log.debug("Only children are wanted");
						
						if (menuItemFound.hasChildren(allMenuItems))
						{
							if(_log.isDebugEnabled())
								_log.debug(new StringBuilder("Getting children of the layout: '").append(menuItemFound.getPlid()).append("' (plid)") );
							
							menuItemsToBeDrawn = menuItemFound.getChildren(allMenuItems);
						}
						else
						{
							error = true;
							if(_log.isDebugEnabled())
								_log.info(new StringBuilder("No children found for the layout: '").append(menuItemFound.getPlid()).append("' requested to draw the menu"));
						}
					}
					else
					{					
						menuItemsToBeDrawn = new ArrayList<MenuItem>();
						menuItemsToBeDrawn.add(menuItemFound);
						menuItemSelected = menuItemFound;
					}
				}
				else
				{
					error = true;
					_log.error(new StringBuilder("No menuItem found for the layout: ").append(layoutPlid).append("' (plid)"));				
				}
			}
			else if(layoutPlid==-1)
			{
				error = true;
				_log.error("No layout found for the menu-portlet with portletid: " + portletId ) ;
			}
		}

		if (!error)
		{
			// Si no ha habido error y no se ha seleccionado ninguna sección en particular, se pintan las secciones padres y los hijos de éstos
			if (null == menuItemsToBeDrawn || menuItemsToBeDrawn.size() == 0)
			{
				menuItemsToBeDrawn = MenuItem.getParentsFromAllMenuItem(allMenuItems);
			}
			
			_log.debug("Setting variables to be injected in velocity");
			
			// Metemos las variables que serán inyectadas en velocity
			HashMap<String, Object> variablesToBeInjected = new HashMap<String, Object>();			
			variablesToBeInjected.put("orientation"     		, orientation         );
			variablesToBeInjected.put("levels"          		, levels              );
			variablesToBeInjected.put("desplegado"      		, desplegado          );
			variablesToBeInjected.put("withMegaMenu"    		, withMegaMenu        );
			variablesToBeInjected.put("allMenuItems"    		, allMenuItems        );
			variablesToBeInjected.put("menuItems"       		, menuItemsToBeDrawn  );
			variablesToBeInjected.put("menuItemSelected"		, menuItemSelected    );			
			variablesToBeInjected.put("menuType"        		, menuType            );		
			variablesToBeInjected.put("portalUUIDUtil"  		, new PortalUUIDUtil());
			variablesToBeInjected.put("mainSiteNavigation"		, mainSiteNavigation  );
			variablesToBeInjected.put("HtmlOptimizer_isEnabled"	, HtmlOptimizer.isEnabled());
			
			// ITER-1280 Evitar el marcaje automático con Microdatos para Google Structured Data Tool
			variablesToBeInjected.put("microdata4GoogleDisabled", PropsValues.ITER_MICRODATA_FOR_GOOGLE_DISABLED);
			
			 // Identificador único para el div que envuelve el menu (id del portlet)
			variablesToBeInjected.put("uniqueDivId" , portletId);				
			
			// Identificadores para el menu con pestañas. Estos ids relacionan la pestaña con el contenido a mostrar
			if(menuType.equals("tabs"))
			{
				String[] ids = new String[menuItemsToBeDrawn.size()];
				for (int i = 0; i < menuItemsToBeDrawn.size(); i++)
				{
					ids[i] = PortalUUIDUtil.newUUID();
				}
				variablesToBeInjected.put("ids", ids);
			}

			try 
			{
				// Aplicamos la plantilla velócity
				_log.debug("Before call merge template to menu-portlet");
				
				IterRequest.setAttribute(WebKeys.CATALOG_TYPE, CatalogUtil.CATALOG_TYPE_MENU);
				
				VelocityUtil.mergeTemplate( new File(getVelocityTemplate(menuType)), null, variablesToBeInjected, response.getWriter());
				_log.debug("After call merge template to menu-portlet");
			}
			catch (Exception e) 
			{
				_log.error("Error merging template", e);
				throw(e);
			}
			finally
			{
				IterRequest.removeAttribute(WebKeys.CATALOG_TYPE);
			}
		}
	}		
	
	// Obtiene la plantilla velócity en función del tipo de menu
	private static String getVelocityTemplate(String menuType){
		_log.trace("In getVelocityTemplate");
		
		StringBuffer vm = new StringBuffer(new File(PortalUtil.getPortalWebDir()).getParentFile().getAbsolutePath())
			                               .append(File.separator + "news-portlet" + File.separator + "xsl" + File.separator);
			
		// Menú en acordeón o con tabs
		if(menuType.equals("accordion") || menuType.equals("tabs")){
			vm.append("navigation_acordeonOrTabs.vm");
			
		// Meú por defecto
		}else{
			vm.append("navigation_default.vm");
		}
		
		if(_log.isDebugEnabled())
			_log.debug(new StringBuilder("Velocity template to be used: '").append(vm.toString()).append("'"));
		
		return vm.toString();
	}
}