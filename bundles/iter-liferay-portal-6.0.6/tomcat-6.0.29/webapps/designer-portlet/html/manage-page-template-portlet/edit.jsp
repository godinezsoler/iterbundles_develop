<%@page import="com.protecmedia.iter.designer.model.PageTemplate"%>
<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@include file="init.jsp"%>

<%@page import="com.liferay.portal.kernel.util.StringBundler"%>
<%@page import="com.liferay.portal.kernel.language.LanguageUtil"%>

<%@page import="com.liferay.portal.kernel.dao.orm.DynamicQuery"%>
<%@page import="com.liferay.portal.kernel.util.PortalClassLoaderUtil"%>
<%@page import="com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.liferay.portal.kernel.util.LocaleUtil"%>
<%@page import="com.liferay.portal.kernel.util.HtmlUtil"%>
<%@page import="javax.portlet.PortletURL"%>
<%@page import="javax.portlet.RenderResponse"%>
<%@page import="com.liferay.portal.model.LayoutConstants"%>
<%@page import="com.liferay.portal.kernel.servlet.ImageServletTokenUtil"%>

<jsp:useBean id="editURL" class="java.lang.String" scope="request" />
<jsp:useBean id="pageTemplate" type="com.protecmedia.iter.designer.model.PageTemplate" scope="request" />

<portlet:renderURL var="cancelURL" />

<%	
	String redirect = "javascript:location.href='" + cancelURL + "'";
%>

<liferay-ui:error key="manage-page-template-name-required" message="manage-page-template-name-required" />
<liferay-ui:error key="manage-page-template-already-exist" message="manage-page-template-already-exist" />

<aui:form name="fm" method="post" action="<%= editURL %>" cssClass="fm-edit-page-template" enctype="multipart/form-data" >
	<aui:input type="hidden" name="resourcePrimKey" value="<%= pageTemplate.getId() %>" />
	
	<aui:select name="type" label="manage-page-template-edit-type">
		<aui:option selected='<%= pageTemplate.getType().equals("page-template") %>' value="page-template"><liferay-ui:message key="manage-page-template-page-tempalte" /></aui:option>
		<aui:option selected='<%= pageTemplate.getType().equals("article-template") %>' value="article-template"><liferay-ui:message key="manage-page-template-article-template" /></aui:option>
		<aui:option selected='<%= pageTemplate.getType().equals("newsletter") %>' value="newsletter"><liferay-ui:message key="manage-page-template-newsletter" /></aui:option>
	</aui:select>
			
	<aui:input type="text" name="pageTemplateId" disabled='<%= !pageTemplate.getPageTemplateId().equals("") %>' value="<%= pageTemplate.getPageTemplateId() %>" label="manage-page-template-edit-id" />			
	<aui:input type="text" name="name" value="<%= pageTemplate.getName() %>" label="manage-page-template-edit-name" />
	<aui:input type="textarea" rows="10" name="description" value="<%= pageTemplate.getDescription() %>" label="manage-page-template-edit-description" maxlength="75" />
	
	<div id="<portlet:namespace />parentIdDiv" class="aui-helper-hidden">
		<aui:input type="hidden" name="parentId" />
		<br />	
		<div class="portlet-msg-info">	
			<span class="displaying-help-message-parent-holder aui-helper-hidden">
				<liferay-ui:message key="manage-page-template-please-select-a-parent" />
			</span>
		
			<span class="displaying-parent-id-holder ">
					<%
						String parentName = portletDisplay.getThemeDisplay().getScopeGroupName();
						try {
							Layout templateLayout = LayoutLocalServiceUtil.getLayout(pageTemplate.getLayoutId());
							if (templateLayout.getParentLayoutId() != 0){
								parentName = LayoutLocalServiceUtil.getLayout(templateLayout.getParentPlid()).getName();
							}
						} catch (Exception e) {			
						}
					%>			
				<liferay-ui:message key="manage-page-template-displaying-parent" />: <span class="displaying-parent-id"><%= parentName %></span>
			</span>
		</div>
		
		<div class="lfr-tree-controls aui-helper-clearfix">
			<div class="lfr-tree-controls-item" id="<portlet:namespace />treeExpandAll">
				<div class="aui-icon lfr-tree-controls-expand"></div>				
				<a href="javascript:;" class="lfr-tree-controls-label"><liferay-ui:message key="expand-all" /></a>
			</div>
		
			<div class="lfr-tree-controls-item" id="<portlet:namespace />treeCollapseAll">
				<div class="aui-icon lfr-tree-controls-collapse"></div>
		
				<a href="javascript:;" class="lfr-tree-controls-label"><liferay-ui:message key="collapse-all" /></a>
			</div>
		</div>					
	
		<div id="secciones">
			<div class="lfr-tree-loading" id="<portlet:namespace />treeLoading">
				<span class="aui-icon aui-icon-loading lfr-tree-loading-icon"></span>
			</div>						
		</div>		
	</div>
		
	<aui:input name="imageDetail" label="manage-page-template-edit-detail-view-template-image" type="file" />
	
	<c:if test="<%= pageTemplate.getImageId() != 0 && pageTemplate.getImageId() != -1 %>">
		<liferay-ui:toggle-area align="none" id="imageId" defaultShowContent="false" showMessage="show" hideMessage="hide">
			<img border="0" src="<%= themeDisplay.getPathImage() + "/journal/template?img_id=" + pageTemplate.getImageId() + "&t=" + ImageServletTokenUtil.getToken(pageTemplate.getImageId()) %>" />
		</liferay-ui:toggle-area>
	</c:if>
	
	<br />	
		
	<aui:select name="selectPageTemplate" label="manage-page-template-edit-select-page-template">
		<aui:option selected="true" value="false"><liferay-ui:message key="manage-page-template-edit-no" /></aui:option>
		<aui:option value="true"><liferay-ui:message key="manage-page-template-edit-yes" /></aui:option>
	</aui:select>
		
	<div class="aui-helper-hidden" id="<portlet:namespace />pageTemplateDiv">
		<aui:input name="idPageTemplate" type="hidden" value="" />			
		<div class="portlet-msg-info">				
			<span class="displaying-help-message-page-template-holder">
				<liferay-ui:message key="manage-page-template-please-select-a-page-template" />
			</span>
			<span class="displaying-page-template-id-holder aui-helper-hidden">
				<liferay-ui:message key="manage-page-template-displaying-page-content" />: <span class="displaying-page-template-id"></span>
			</span>		
		</div>
	
		<liferay-ui:search-container emptyResultsMessage="manage-page-template-edit-empty-result"  >
			<liferay-ui:search-container-results >
				<%
					List<PageTemplate> tempResults = PageTemplateLocalServiceUtil.getPageTemplates(scopeGroupId);
					results = ListUtil.subList(tempResults, searchContainer.getStart(), searchContainer.getEnd());	
					total = tempResults.size();							
					pageContext.setAttribute("results", results);
					pageContext.setAttribute("total", total);
				%>
			</liferay-ui:search-container-results>
		
			<liferay-ui:search-container-row className="com.protecmedia.iter.designer.model.PageTemplate" keyProperty="id" modelVar="pageTemplateSearch">	
			
				<%
				pageTemplateSearch = pageTemplateSearch.toEscapedModel();
							
					StringBundler sb = new StringBundler(7);
								
					sb.append("javascript:");
					sb.append(renderResponse.getNamespace());
					sb.append("selectPageTemplate('");
					sb.append(pageTemplateSearch.getId());
					sb.append("','");
					sb.append(pageTemplateSearch.getName());			
					sb.append("');");
					
					String description = pageTemplateSearch.getDescription();
					
					description.substring(0, (description.length() < DesignerConstantsUtil.MAX_STRING_SIZE) ? description.length() : DesignerConstantsUtil.MAX_STRING_SIZE);
				%>	
				<c:if test="<%= pageTemplateSearch.getLayoutId() != pageTemplate.getLayoutId()  %>">
					<liferay-ui:search-container-column-text href="<%= sb.toString() %>"  name="manage-page-template-edit-name" value="<%= pageTemplateSearch.getName() %>" />					
					<liferay-ui:search-container-column-text href="<%= sb.toString() %>" name="manage-page-template-edit-description" value="<%= description %>" />
				</c:if>					
				
				<c:if test="<%= pageTemplateSearch.getLayoutId() == pageTemplate.getLayoutId() %>">
					<liferay-ui:search-container-column-text name="manage-page-template-edit-name" value="<%= pageTemplateSearch.getName() %>" />					
					<liferay-ui:search-container-column-text name="manage-page-template-edit-description" value="<%= description %>" />
				</c:if>
						
				<liferay-ui:search-container-column-text name="" align="right" >
					<%
						String editTemplateURL = "";
						try {
							Layout layoutTpl = LayoutLocalServiceUtil.getLayout(pageTemplateSearch.getLayoutId());
							editTemplateURL = PortalUtil.getLayoutFullURL(layoutTpl, themeDisplay);
						} catch (Exception e) {			
						}
					%>
					<c:if test='<%= !editTemplateURL.equals("") %>'>
					    <liferay-ui:icon image="preview" label="" target="_blank" message="" url="<%= editTemplateURL %>" />						
					</c:if>					
				</liferay-ui:search-container-column-text>
			</liferay-ui:search-container-row>
			
			<liferay-ui:search-iterator />
		</liferay-ui:search-container>
	</div>

	<aui:button-row>	
		<aui:button type="submit" />	
		<aui:button onClick="<%= redirect %>" type="cancel" />
	</aui:button-row>
</aui:form>


<%												
	String nameRoot = portletDisplay.getThemeDisplay().getScopeGroupName();
	String idRoot =  "rootTreeViewId";
	String idActual = "0Id";

	String children = getChildrens(nameRoot, idRoot, 0, 0, scopeGroupId, renderResponse);								
%>

<aui:script use="aui-tree-view">	
			
	var children = [<%= children %>]
	
	var secciones = new A.TreeView({
		type: 'file',		
		boundingBox: '#secciones',
		children: children,		
		after: {
			render: function(event) {			
				var loadingEl = A.get('#<portlet:namespace />treeLoading');			
				loadingEl.hide();
				
				var rootNode = A.get('#<portlet:namespace /><%= idRoot %>');
				rootNode.addClass('lfr-root-node');							
			}
		}
	}).render();

	A.on('click', function() { secciones.expandAll(); }, '#<portlet:namespace />treeExpandAll');
	A.on('click', function() { secciones.collapseAll(); }, '#<portlet:namespace />treeCollapseAll');


	Liferay.provide(
		window,
		'<portlet:namespace />deletePages',
		function() {
			if (confirm('<%= UnicodeLanguageUtil.get(pageContext, "are-you-sure-you-want-to-delete-the-selected-page-contents") %>')) {
				document.<portlet:namespace />fm.submit();	
			}
		},
		['aui-base']
	);
		
	Liferay.Util.toggleSelectBox('<portlet:namespace />selectPageTemplate','true','<portlet:namespace />pageTemplateDiv');
	
	Liferay.Util.toggleSelectBox('<portlet:namespace />type','article-template','<portlet:namespace />parentIdDiv');

	Liferay.provide(
		window,
		'<portlet:namespace />selectParent',
		function(id, name) {
			var A = AUI();

			document.<portlet:namespace />fm.<portlet:namespace />parentId.value = id;
			
			A.one('.displaying-parent-id-holder').show();
			A.one('.displaying-help-message-parent-holder').hide();					

			var displayParentId = A.one('.displaying-parent-id');

			displayParentId.set('innerHTML', String(name) + ' (<%= LanguageUtil.get(pageContext, "modified") %>)');
			displayParentId.addClass('modified');
		},
		['aui-base']
	);


	Liferay.provide(
		window,
		'<portlet:namespace />selectPageTemplate',
		function(id, name) {
			var A = AUI();

			document.<portlet:namespace />fm.<portlet:namespace />idPageTemplate.value = id;
			
			A.one('.displaying-page-template-id-holder').show();
			A.one('.displaying-help-message-page-template-holder').hide();					

			var displayPageTemplateId = A.one('.displaying-page-template-id');

			displayPageTemplateId.set('innerHTML', name + ' (<%= LanguageUtil.get(pageContext, "modified") %>)');
			displayPageTemplateId.addClass('modified');
		},
		['aui-base']
	);
</aui:script>

<%!

	private String getChildrens(String nameNode, String idNode, long layoutId, long idPadre, long idGrupo, RenderResponse renderResponse) throws Exception  {
		List<Layout> layoutList = getLayouts(idGrupo, false, idPadre);
		
		long id = 0;
		String name = "";
		long lId = LayoutConstants.DEFAULT_PARENT_LAYOUT_ID;
		if (idPadre != 0) {
			Layout l = LayoutLocalServiceUtil.getLayout(idGrupo, false, idPadre);
			id = l.getPlid();
			lId = l.getLayoutId();
		}

		StringBuffer children = new StringBuffer();
		
		children.append("{");		
		
		children.append("id:'");
		children.append(renderResponse.getNamespace());
		children.append(idNode);
		children.append("',");
		
		//if (layoutId != idPadre && idPadre != 0) {		
			children.append("label:'<a href=\"");

			children.append("javascript:");
			children.append(renderResponse.getNamespace());
			children.append("selectParent(");
			children.append(String.valueOf(lId));			
			children.append(",\\'");
			children.append(HtmlUtil.escape(nameNode));			
			children.append("\\'); \">");
			children.append(HtmlUtil.escape(nameNode));
			children.append("</a>',");
		/*} else {
			children.append("label:'");
			children.append(HtmlUtil.escape(nameNode));
			children.append("',");			
		}*/
		
		if (layoutList.size() > 0) {	
			children.append("leaf: false,");
			children.append("expanded: ");
			children.append("false");
			children.append(",");
			children.append("children: [");
			for (int i = 0; i < layoutList.size(); i++) {
				Layout _layout = layoutList.get(i);			
											
				nameNode = _layout.getName(LocaleUtil.getDefault());
				idNode = _layout.getLayoutId() + "Id";
				children.append(getChildrens(nameNode, idNode, layoutId, _layout.getLayoutId(), idGrupo, renderResponse));												
					
			}
			children.append("]");
		} else {
			children.append("leaf: true,");
			children.append("expanded: false");
		}
		children.append("},");
		
		return children.toString();
	}

	private List<Layout> getLayouts(long groupId, boolean privateLayout, long parentLayoutId) throws Exception {
		List<Layout> layouts = new ArrayList<Layout>();
		
		//Listado de articulos
		ClassLoader cl = PortalClassLoaderUtil.getClassLoader();
		
		// Buscar por group, structureId, status 
		DynamicQuery query = DynamicQueryFactoryUtil.forClass(Layout.class, cl);
		
		query.add(PropertyFactoryUtil.forName("groupId").eq(groupId));
		query.add(PropertyFactoryUtil.forName("privateLayout").eq(privateLayout));
		query.add(PropertyFactoryUtil.forName("parentLayoutId").eq(parentLayoutId));				
		query.add(PropertyFactoryUtil.forName("type").ne(IterKeys.CUSTOM_TYPE_TEMPLATE));
				
		layouts = LayoutLocalServiceUtil.dynamicQuery(query);
	
		return layouts;
	}	

%>
