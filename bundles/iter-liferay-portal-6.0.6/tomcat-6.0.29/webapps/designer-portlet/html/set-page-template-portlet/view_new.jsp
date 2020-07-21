<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@page import="com.liferay.portal.kernel.portlet.LiferayWindowState"%>
<%@include file="init.jsp"%>

<%@page import="com.liferay.portal.kernel.util.StringBundler"%>
<%@page import="com.liferay.portal.kernel.language.LanguageUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>


<%@page import="com.liferay.portal.model.Layout"%>
<%@page import="com.liferay.portal.model.LayoutConstants"%>
<%@page import="com.liferay.portal.service.LayoutLocalServiceUtil"%>
<%@page import="com.liferay.portal.kernel.util.HtmlUtil"%>
<%@page import="com.liferay.portal.kernel.util.LocaleUtil"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.liferay.portal.kernel.util.PortalClassLoaderUtil"%>
<%@page import="com.liferay.portal.kernel.dao.orm.DynamicQuery"%>
<%@page import="com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil"%>
<%@page import="com.protecmedia.iter.base.service.util.IterKeys"%>
<%@page import="com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil"%>
<%@page import="javax.portlet.RenderResponse"%>


<div class="set-page-template">

	<portlet:actionURL var="createURL" >
		<portlet:param name="action" value="add" />
	</portlet:actionURL>
	
	<aui:fieldset label="set-page-template-view-add-new-page-template">
		<aui:form action="<%= createURL %>" method="post" name="form1" onSubmit="">							
		
			<aui:select name="type" label="set-page-template-view-type">
				<aui:option value="page-template"><liferay-ui:message key="set-page-template-view-page-tempalte" /></aui:option>
				<aui:option value="article-template"><liferay-ui:message key="set-page-template-view-article-template" /></aui:option>
			</aui:select>
		
			<aui:input type="pageTemplateId" name="name" label="set-page-template-id" />
		
			<aui:input type="text" name="name" label="set-page-template-view-name" />
			<aui:input type="textarea" name="description" label="set-page-template-view-description" maxlength="75" />
				
			<div id="parentIdDiv" class="aui-helper-hidden">
				<aui:input type="hidden" name="parentId" />
				<br />	
				<div class="portlet-msg-info">
					<span class="displaying-help-message-parent-holder">
						<liferay-ui:message key="manage-page-template-please-select-a-parent" />
					</span>
				
					<span class="displaying-parent-id-holder aui-helper-hidden">
						<liferay-ui:message key="manage-page-template-displaying-parent" />: <span class="displaying-parent-id"></span>
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
		
			<aui:button-row>				
				<aui:button type="submit" value="set-page-template-view-add-new-template" />
			</aui:button-row>
		</aui:form>
	</aui:fieldset>
</div>

<%												
	String nameRoot = portletDisplay.getThemeDisplay().getScopeGroupName();
	String idRoot =  "rootTreeViewId";
	String idActual = "0Id";

	String children = getChildrens(nameRoot, idRoot, 0, 0, scopeGroupId, renderResponse);								
%>

<aui:script use="aui-tree-view">	
						
	Liferay.provide(
		window,
		'<portlet:namespace />submitForm',
		function <portlet:namespace />submitForm() {			
			submitForm(document.<portlet:namespace />form1);
			//window.close();
		},
		['aui-base']
	);			
			
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
		'<portlet:namespace />selectParent',
		function(id, name) {
			var A = AUI();

			document.<portlet:namespace />form1.<portlet:namespace />parentId.value = id;
			
			A.one('.displaying-parent-id-holder').show();
			A.one('.displaying-help-message-parent-holder').hide();					

			var displayParentId = A.one('.displaying-parent-id');

			displayParentId.set('innerHTML', String(name) + ' (<%= LanguageUtil.get(pageContext, "modified") %>)');
			displayParentId.addClass('modified');
		},
		['aui-base']
	);
	
	Liferay.Util.toggleSelectBox('<portlet:namespace />type','article-template','parentIdDiv');

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

