<%@page import="com.protecmedia.iter.news.service.PageContentLocalServiceUtil"%>
<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@ include file="init.jsp" %>

<%@page import="com.liferay.portlet.imagegallery.service.IGFolderLocalServiceUtil"%>
<%@page import="com.liferay.portlet.imagegallery.model.IGFolder"%>
<%@page import="com.liferay.portal.kernel.language.UnicodeLanguageUtil"%>
<%@page import="com.protecmedia.iter.designer.service.PageTemplateLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.designer.model.PageTemplate"%>
<%@page import="com.liferay.portal.kernel.util.PortalClassLoaderUtil"%>
<%@page import="com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil"%>
<%@page import="com.liferay.portal.kernel.dao.orm.DynamicQuery"%>
<%@page import="com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil"%>

<%
	String tabs1 = ParamUtil.getString(request, "tabs1", "page-content");

	PortletURL portletURL = renderResponse.createRenderURL();	
	portletURL.setParameter("tabs1", tabs1);
%>

<liferay-ui:tabs 
	names="page-content,qualification"
	param="tabs1"
	url="<%= portletURL.toString() %>"
/>

<c:choose>	

	<c:when test='<%= scopeGroupId == globalGroupId  %>'>
	
	<liferay-ui:message key="page-content-portlet-not-available-in-global-environment" />
	
	</c:when>
	<c:when test='<%= tabs1.equals("page-content") %>'>
<%--****************--%>
<%-- Page Content --%>
<%--****************--%>
	
<jsp:useBean id="addPageContentURL" class="java.lang.String" scope="request" />
<jsp:useBean id="deletePageContentURL" class="java.lang.String" scope="request" />		

<liferay-portlet:renderURL varImpl="iteratorURL">
	<liferay-portlet:param name="layout-id" value="<%= String.valueOf(layoutId) %>" />
</liferay-portlet:renderURL>

<liferay-ui:success key="page-content-added"	message="page-content-added-successfully" />
<liferay-ui:success key="page-content-updated" message="page-content-updated-successfully" />
<liferay-ui:success key="page-content-deleted" message="page-content-deleted-successfully" />
<liferay-ui:success key="page-content-deleted-all" message="page-content-deleted-all-successfully" />
<liferay-ui:success key="page-content-order-changed" message="page-content-order-changed-successfully" />
<liferay-ui:success key="page-content-activate" message="page-content-activate" />
<liferay-ui:success key="page-content-deactivate" message="page-content-deactivate" />
<liferay-ui:error key="page-content-id-not-exist" message="page-content-id-not-exist" />
<liferay-ui:error key="page-content-deleted-all-error" message="page-content-deleted-all-error" />

<%
	String editURL = "self.location = '" + addPageContentURL + "'";	

	String vacio = "---";
	List<PageContent> tempResults = PageContentLocalServiceUtil.findPageLayoutFinder(layoutId, scopeGroupId);	
	
	Layout seccionActual = null;
	if (layoutId != "") {
		seccionActual = LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(layoutId, scopeGroupId);
	}
				
	//
	List<Long> openNodes = new ArrayList<Long>();
	// Añadimos el root
	openNodes.add(0L);
	Layout pagina = LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(layoutId, scopeGroupId);
	getOpenNodes(pagina.getLayoutId(), 0, scopeGroupId, openNodes);
%>

<div class="page-content-view-portlet">
	<aui:button-row>				
		<c:if test='<%= (Boolean)request.getAttribute("hasAddPermission")%>'>						
			<aui:button onClick="<%= editURL %>" name="page-content-view-new-page-content" value="page-content-view-new-page-content" />			
		</c:if>
		<c:if test='<%= (Boolean)request.getAttribute("hasDeletePermission") %>'>						
			<aui:button onClick='<%= renderResponse.getNamespace() + "deletePages();" %>' name="page-content-view-delete-page-content" value="page-content-view-delete-page-content" />			
		</c:if>
	</aui:button-row>
	
	
	<div class="separator article-separator"></div>	
	
	<table width="100%">
		<tr>
			<td class="aui-w25 lfr-top">
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
			</td>
			<td class="aui-w75 lfr-top">
			
				<aui:form name="fm" method="post" action="<%= deletePageContentURL %>">
					<liferay-ui:search-container emptyResultsMessage="page-content-view-empty-result" iteratorURL="<%= iteratorURL %>" rowChecker="<%= new RowChecker(renderResponse) %>">
						<liferay-ui:search-container-results>
							<%						
								results = ListUtil.subList(tempResults, searchContainer.getStart(), searchContainer.getEnd());	
								total = tempResults.size();							
								pageContext.setAttribute("results", results);
								pageContext.setAttribute("total", total);
							%>
						</liferay-ui:search-container-results>
						
						<liferay-ui:search-container-row  className="com.protecmedia.iter.news.model.PageContent" keyProperty="id" modelVar="pagecontent">	
							<%
								String contentTitle = vacio;			
								try {
									JournalArticle article = JournalArticleLocalServiceUtil.getArticle(pagecontent.getContentGroupId(), pagecontent.getContentId());
									contentTitle = article.getTitle();
								} catch (Exception e) {}
							
								PortletURL updateURL = renderResponse.createActionURL();
								updateURL.setParameter("resourcePrimKey", String.valueOf(pagecontent.getPrimaryKey()));
								updateURL.setParameter("layout-id", String.valueOf(layoutId));
								updateURL.setParameter("javax.portlet.action", "editPageContent");													
							%>	
							
							<liferay-ui:search-container-column-text href="<%= updateURL.toString() %>" title="<%= contentTitle %>" name="page-content-view-journal-article" value="<%= contentTitle %>" />
																		
							<% 
								Qualification qualification  = null;
								if (pagecontent.getQualificationId() != "") {
									qualification  = QualificationLocalServiceUtil.getQualificationByQualifId(scopeGroupId, pagecontent.getQualificationId());
								}
								String qualificationName = vacio;
								if (qualification != null) {
									qualificationName = qualification.getName();
								}
								
								String articleModelName = "default";
								if (pagecontent.getArticleModelId() != 0) {
									try {
										PageTemplate pageTemplate = PageTemplateLocalServiceUtil.getPageTemplate(pagecontent.getArticleModelId());
										articleModelName = pageTemplate.getName();
									} catch (Exception e) {}									
								}
							%>
																					
							<liferay-ui:search-container-column-text href="<%= updateURL.toString() %>" name="page-content-view-qualification"  value="<%= qualificationName %>"/>
							<liferay-ui:search-container-column-text href="<%= updateURL.toString() %>" name="page-content-view-type" value="<%= pagecontent.getTypeContent() %>" />
							<liferay-ui:search-container-column-text href="<%= updateURL.toString() %>" name="page-content-view-article-model" value="<%= articleModelName %>" />
							<liferay-ui:search-container-column-text href="<%= updateURL.toString() %>" name="page-content-view-display-date" value="<%= df.format(pagecontent.getVigenciadesde()) %>" />
							<liferay-ui:search-container-column-text href="<%= updateURL.toString() %>" name="page-content-view-expiration-date" value="<%= df.format(pagecontent.getVigenciahasta()) %>" />
							<liferay-ui:search-container-column-text href="<%= updateURL.toString() %>" name="page-content-view-online" value="<%= String.valueOf(pagecontent.getOnline()) %>" />
							<liferay-ui:search-container-column-jsp align="right" path="/html/pagecontent-portlet/edit_actions.jsp" />
						</liferay-ui:search-container-row>
							
						<liferay-ui:search-iterator />
					</liferay-ui:search-container>
				</aui:form>
			</td>
		</tr>
	</table>

	<%												
		String nameRoot = portletDisplay.getThemeDisplay().getScopeGroupName();
		String idRoot =  "rootTreeViewId";
		String idActual = seccionActual.getLayoutId() + "Id";
	
		String children = getChildrens(nameRoot, idRoot, pagina.getLayoutId(), 0, scopeGroupId, openNodes, renderResponse);								
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
					
					var actualNode = A.get('#<portlet:namespace /><%= idActual %>');
					actualNode.addClass('node-selected');								
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
	
	</aui:script>		
</div>

<%!

	private String getChildrens(String nameNode, String idNode, long layoutId, long idPadre, long idGrupo, List<Long> openNodes, RenderResponse renderResponse) throws Exception  {
		List<Layout> layoutList = getLayouts(idGrupo, false, idPadre);
		
		String uuid = "";
		long id = 0;
		if (idPadre != 0) {
			Layout l = LayoutLocalServiceUtil.getLayout(idGrupo, false, idPadre);
			uuid = l.getUuid();
			id = l.getPlid();
		}
		StringBuffer children = new StringBuffer();
		
		children.append("{");		
		
		children.append("id:'");
		children.append(renderResponse.getNamespace());
		children.append(idNode);
		children.append("',");
		
		if (layoutId != idPadre && idPadre != 0) {
			PortletURL portletUrl = renderResponse.createRenderURL();
			portletUrl.setParameter("layout-id", uuid);								
			children.append("label:'<a href=\"");
			children.append(portletUrl.toString());
			children.append("\">");
			children.append(HtmlUtil.escape(nameNode));
			children.append("</a>',");
		} else {
			children.append("label:'");
			children.append(HtmlUtil.escape(nameNode));
			children.append("',");			
		}
		
		if (layoutList.size() > 0) {	
			children.append("leaf: false,");
			children.append("expanded: ");
			children.append(openNodes.contains(id));
			children.append(",");
			children.append("children: [");
			for (int i = 0; i < layoutList.size(); i++) {
				Layout _layout = layoutList.get(i);			
											
					nameNode = _layout.getName(LocaleUtil.getDefault());
					idNode = _layout.getLayoutId() + "Id";
					children.append(getChildrens(nameNode, idNode, layoutId, _layout.getLayoutId(), idGrupo, openNodes, renderResponse));												
					
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
	
	
	private boolean getOpenNodes(long layoutId, long idPadre, long idGrupo, List<Long> openNodes) throws Exception  {
		List<Layout> layoutList = LayoutLocalServiceUtil.getLayouts(idGrupo, false, idPadre);
		boolean encontrado = false;
		for (int i = 0; i < layoutList.size(); i++) {
			Layout _layout = layoutList.get(i);			
							
				if (layoutId == _layout.getPlid()) {					
					encontrado = true;
					openNodes.add(layoutId);
					break;
				} else {					
					if (getOpenNodes(layoutId, _layout.getLayoutId(), idGrupo, openNodes)) {
						encontrado = true;
						openNodes.add(_layout.getLayoutId());						
						break;
					}
				}
			
		}	
		return encontrado;
	}

%>			
	</c:when>
	
	<c:when test='<%= tabs1.equals("qualification") %>'>
		<%--***************--%>
		<%-- Qualification --%>
		<%--***************--%>	
		<jsp:useBean id="addQualificationURL" class="java.lang.String" scope="request" />
		
		<liferay-ui:success key="qualification-added" message="qualification-added-successfully" />
		<liferay-ui:success key="qualification-updated" message="qualification-updated-successfully" />
		<liferay-ui:success key="qualification-deleted" message="qualification-deleted-successfully" />
		
		<%
			String editURL = "self.location = '" + addQualificationURL + "'";
		%>
		
		<c:if test='<%= (Boolean)request.getAttribute("hasAddPermission") %>'>						
			<aui:button onClick="<%= editURL %>" name="qualification-view-new-qualification" value="qualification-view-new-qualification" />			
		</c:if>
				
		<div class="separator article-separator"></div>
		<br />	
		<%
		    PortletURL qualificationIteratorURL = renderResponse.createRenderURL();
			qualificationIteratorURL.setParameter("tabs1", "qualification");
		    pageContext.setAttribute("qualificationIteratorURL", qualificationIteratorURL);
		%>				
		<liferay-ui:search-container iteratorURL="${qualificationIteratorURL}" emptyResultsMessage="qualification-view-empty-result"  >
			<liferay-ui:search-container-results >
				<%
					List<Qualification> tempResults = (List<Qualification>) request.getAttribute("tempResults");
					results = ListUtil.subList(tempResults, searchContainer.getStart(), searchContainer.getEnd());	
					total = tempResults.size();							
					pageContext.setAttribute("results", results);
					pageContext.setAttribute("total", total);
				%>
			</liferay-ui:search-container-results>
		
			<liferay-ui:search-container-row className="com.protecmedia.iter.news.model.Qualification" keyProperty="id" modelVar="qualification">			
				<liferay-ui:search-container-column-text name="qualification-view-qualification" value="<%= qualification.getName() %>" />			
				<liferay-ui:search-container-column-jsp align="right" path="/html/pagecontent-portlet/edit_actions_qualification.jsp" />
			</liferay-ui:search-container-row>
			
			<liferay-ui:search-iterator />
		</liferay-ui:search-container>
	</c:when>
	
</c:choose>
