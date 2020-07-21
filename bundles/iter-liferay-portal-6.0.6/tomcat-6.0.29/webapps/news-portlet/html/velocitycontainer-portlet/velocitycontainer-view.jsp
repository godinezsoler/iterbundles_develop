<%@page import="com.liferay.portal.kernel.velocity.VelocityContext"%>
<%@page import="com.liferay.portal.kernel.util.GetterUtil"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="com.protecmedia.iter.base.service.util.PortletPreferencesTools"%>
<%@ page import="javax.portlet.PortletPreferences" %>

<%@ page import="com.protecmedia.iter.base.service.IterLocalServiceUtil"%>

<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="com.protecmedia.iter.news.util.VelocityContainerTools"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.kernel.log.Log"%>
<%@page import="com.liferay.portal.kernel.log.LogFactoryUtil"%>

<liferay-theme:defineObjects />
<portlet:defineObjects />

<% 	// Se crea el log con velocitycontainer-view_jsp y no con velocitycontainer-view.jsp para que en la traza se muestre el nombre del fichero que genera el error
	// Traza esperada:	ERROR [velocitycontainer-view_jsp:261] Reconfigure VelocityContainer
	// Traza errónea:	ERROR [jsp:261] Reconfigure VelocityContainer
%>
<%! private static Log _logVelocityContainer = LogFactoryUtil.getLog("news-portlet.docroot.html.velocitycontainer-portlet.velocitycontainer-view_jsp"); %>

<%
	PortletPreferences preferences = PortletPreferencesTools.getPortletPreferences(renderRequest, request);

	String textVelocity = preferences.getValue("textVelocity", "");
	String errorMsg = "";

	if(Validator.isNotNull(textVelocity))
	{
		String velocityContent = textVelocity;
		
		String displayoptionsSection	= preferences.getValue("displayoptionsSection", "selected");
		String[] layoutIds 				= preferences.getValues("layoutIds", null);
		boolean defaultLayout 			= GetterUtil.getBoolean( preferences.getValue("defaultLayout", null), false);
		String[] selectedLayoutsPlids	= null;
		
		if( !defaultLayout && Validator.isNotNull(layoutIds) )
			selectedLayoutsPlids = VelocityContainerTools.getSectionPlids(displayoptionsSection, layoutIds);
		
		
		String displayoptionsMetadata	= preferences.getValue("displayoptionsMetadata", "selected");
		String[] contentCategoryIds		= preferences.getValues("contentCategoryIds", null);
		String[] contentVocabularyIds 	= preferences.getValues("contentVocabularyIds", null);
		int categoryOperation 			= GetterUtil.getInteger( preferences.getValue("categoryOperation", null ), WebKeys.LOGIC_IGNORE);
		long[] selectedCategoriesIds	= null;
		
		if( Validator.isNotNull(contentCategoryIds) || Validator.isNotNull(contentVocabularyIds) )
			selectedCategoriesIds = VelocityContainerTools.getCategories(displayoptionsMetadata, contentCategoryIds, contentVocabularyIds);
		
		VelocityContext velocityContext = VelocityContainerTools.mergeVelocityTemplate( velocityContent , request, selectedLayoutsPlids, selectedCategoriesIds, categoryOperation );
		textVelocity = String.valueOf(velocityContext.get(VelocityContext.TRANSFORM_RESULT));
		
		if( velocityContent.equals(textVelocity) )
		{
			textVelocity = "";
			
			errorMsg = "Editor content is not velocity code";
		}
		else if( !VelocityContainerTools.isValidVelocityCode(defaultLayout) )
		{
			textVelocity = "";
			
			if(defaultLayout)
				errorMsg = "VelocityContainer portlet is configured to use the current layout but is not being used.";
			else
				errorMsg = "VelocityContainer portlet is not configured to use the current layout but is being used.";
			
			velocityContext.rollback();
		}
	}
	
	if( Validator.isNotNull(errorMsg) )
	{
		String environment = IterLocalServiceUtil.getEnvironment();
		
		//mostrar mensaje en el log. Y en la web sólo en entorno preview
		Long layoutPlid = layout.getPlid();
		String layoutFriendlyUrl  =  layout.getFriendlyURL();
		String portletId = (String) renderRequest.getAttribute(WebKeys.PORTLET_ID);
				
		_logVelocityContainer.error("Reconfigure VelocityContainer with portletId " + portletId +
					", located in layout with plid " +layoutPlid.toString() + " and friendlyURL "+layoutFriendlyUrl + ". " + errorMsg);
		
		errorMsg = "Reconfigure this portlet.\n ".concat(errorMsg);
%>
		<c:if test="<%=environment.equals(IterKeys.ENVIRONMENT_PREVIEW)%>">
			<span class="portlet-msg-info">
	   			<liferay-ui:message key="<%= errorMsg %>" />
			</span>
		</c:if>	
<%
	}
%>

<%= textVelocity %>
