<%@page import="com.liferay.portal.util.PortletKeys"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>

<%@ page import="javax.portlet.PortletPreferences"%>
<%@ page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>
<%@ page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@ page import="com.protecmedia.iter.news.util.MenuUtil"%>
<%@ page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@ page import="com.liferay.portal.util.PortalUtil"%>
<%@ page import="com.liferay.portal.theme.RequestVars"%>

<%@ page import="java.util.Calendar"%>

<portlet:defineObjects /> 
<liferay-theme:defineObjects />

<%	
	// Obtenemos las preferencias del portlet
	PortletPreferences preferences = renderRequest.getPreferences();
	
	// Tipo de menú
	String menuType = preferences.getValue("layoutConfiguration", "default"); //menuType="accordion";
	
	// Orientación del menu (horizontal | vertical) "" significa horizontal
	final String menuOrientation = preferences.getValue("layoutType", "horizontal");
	
	// layout/sección (uuid_ del layout) por la que empezar
	final String layoutId = preferences.getValue("depthStartAt", null);
	
	// Indica que mostremos el árbol a partir del layout actual
	final boolean useActualLayout = GetterUtil.getBoolean(preferences.getValue("defaultLayout", null), false);
	
	// Indica que mostremos el árbol a partir del padre del layout actual
	final boolean useCurrentLayoutParent = GetterUtil.getBoolean(preferences.getValue("currentLayoutParent", null), false);
	
	// Indica si se empieza a mostrar el menú con los hijos de la sección indicada
	boolean onlyChildren = GetterUtil.getBoolean(preferences.getValue("onlyChildren", null), false); 
	
	// Niveles a mostrar
	final int levels = GetterUtil.getInteger(preferences.getValue("maxDepthShown", ""), -1);
	
	// Menu desplegado o plegado (true, false)
	final boolean desplegado = GetterUtil.getBoolean(preferences.getValue("unFolded", "false"), false);
		
	// Mostrar mega menú (true | false)
	final boolean withMegaMenu = GetterUtil.getBoolean((preferences.getValue(PortletKeys.PREFS_MENU_SHOW_EXTENDED_MENU, "false")), false); 
	
	// Menú para la navegación principal del sitio, ponemos a false si no existe
	final boolean mainSiteNavigation = GetterUtil.getBoolean(preferences.getValue("mainSiteNavigation", "false"), false);

%>