<%@page import="com.liferay.portal.util.HtmlOptimizer"%>
<%@ include file="menu-portlet-init.jsp" %>

<%
// Id de la instancia del portlet
final String portletId = HtmlOptimizer.getPortletId( themeDisplay.getPortletDisplay().getId() );

/* Es necesario que el atributo TilesSelectable de la clase themeDisplay este a true para que se detecte (isSelected()) qué navItem está seleccionado.
   Nos quedamos con el estado anterior, lo ponemos a true y lo dejamos al final como estuviese para no romper nada */
boolean beforeStateTitlesSelectable = themeDisplay.isTilesSelectable();

themeDisplay.setTilesSelectable(true);

RequestVars requestVars = new RequestVars(request, themeDisplay, themeDisplay.getLayout().getAncestorPlid(), 
	                                      themeDisplay.getLayout().getAncestorLayoutId());
%>
<c:if test='<%= (mainSiteNavigation) %>'>
	<nav>
</c:if>

<%
MenuUtil.getMenu(portletId, menuType, layoutId, useActualLayout, requestVars, 
				 response, menuOrientation, levels, onlyChildren, desplegado, 
				 withMegaMenu, useCurrentLayoutParent, mainSiteNavigation);

themeDisplay.setTilesSelectable(beforeStateTitlesSelectable);
%>

<c:if test='<%= (mainSiteNavigation) %>'>
	</nav>
</c:if>