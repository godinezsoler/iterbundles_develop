<%@page import="com.protecmedia.iter.tracking.util.TrackingUtil"%>
<%@page import="com.liferay.portal.kernel.dao.orm.SQLQuery"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.Collection"%>
<%@page import="com.protecmedia.iter.news.model.Counters"%>
<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@ include file="init.jsp"%>

<%@page import="java.util.Calendar"%>
<%@page import="com.liferay.portal.kernel.util.CalendarFactoryUtil"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.text.DateFormat"%>
<%@page import="com.liferay.portal.kernel.util.Validator"%>
<%@page import="java.util.Date"%>
<%@page import="com.liferay.portal.kernel.dao.orm.Junction"%>
<%@page import="com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil"%>
<%@page import="com.liferay.portlet.polls.service.PollsQuestionLocalServiceUtil"%>
<%@page import="com.liferay.portlet.polls.model.PollsQuestion"%>
<%@page import="com.protecmedia.iter.news.service.ArticlePollLocalServiceUtil"%>
<%@page import="com.protecmedia.iter.news.model.ArticlePoll"%>
<%@page import="com.liferay.util.portlet.PortletRequestUtil"%>
<%@page import="com.liferay.portal.kernel.util.HttpUtil"%>

<%@page import="com.liferay.portal.kernel.language.UnicodeLanguageUtil"%>

<%	
	
	//Vemos si estamos en la vista de milenium
	String milenium = ParamUtil.getString(request, "milenium", "");
	String moderate = ParamUtil.get(renderRequest, "pendingModeration", "");
	String tabs1 = ParamUtil.getString(request, "tabs1", "pages");
	PortletURL portletURL = renderResponse.createRenderURL();	
	portletURL.setParameter("tabs1", tabs1);
	portletURL.setParameter("milenium", milenium);
	
%>

<liferay-ui:tabs 
	names="pages,articles"
	param="tabs1"
	url="<%= portletURL.toString() %>"
/>

<%

	PortletURL filterURL = renderResponse.createActionURL();
	filterURL.setParameter("javax.portlet.action", "filterContentTracking");
	filterURL.setParameter("milenium", milenium);
	
	String keyword = ParamUtil.getString(renderRequest, "keyword", "");	
	String keywordComment = ParamUtil.getString(renderRequest, "keywordComment", "");
	String pendingModeration = ParamUtil.getString(renderRequest, "pendingModeration", "");
	
	boolean standardArticleCheck = ParamUtil.getBoolean(renderRequest, "standardArticleCheck", true);
	boolean standardGalleryCheck = ParamUtil.getBoolean(renderRequest, "standardGalleryCheck", true);
	boolean standardPollCheck = ParamUtil.getBoolean(renderRequest, "standardPollCheck", true);
	boolean standardMultimediaCheck = ParamUtil.getBoolean(renderRequest, "standardMultimediaCheck", true);
	
	DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	Date start = ParamUtil.getDate(renderRequest, "startDate", df, null);
	Date end = ParamUtil.getDate(renderRequest, "endDate", df, null);

	String orderByCol = ParamUtil.getString(renderRequest, "orderByCol", "title");
	String orderByType = ParamUtil.getString(renderRequest, "orderByType", "asc");
	
	String queryFilter = "";
	List<TrackingSearchObject> tempResults = new ArrayList<TrackingSearchObject>();

%>

<c:if test='<%= milenium.equals("true") %>'>								
	<%@include file="milenium-styles.jsp"%>
</c:if>

<c:choose>	

	<c:when test='<%= tabs1.equals("articles") %>'>
	
		<%--***************--%>
		<%--    Articles   --%>
		<%--***************--%>	

		<aui:fieldset label="tracking-view-filter-contents">
			<aui:form name="fm1" method="post" action="<%= filterURL %>" cssClass="filter-view-form">	
				
				<aui:fieldset>
					<aui:column columnWidth="50">				
						<aui:input type="text" name="keyword" label="tracking-view-search" value="<%= keyword %>" />
					</aui:column>
					
					<aui:column columnWidth="50">
						<aui:input type="text" name="keywordComment" label="tracking-view-comment-text-search" value="<%= keywordComment %>" />
					</aui:column>
				</aui:fieldset>
				
				<aui:fieldset cssClass="content-types">
					<label class="aui-field-label"><liferay-ui:message key="tracking-view-content-types" /></label>
					<aui:input inlineLabel="left" inlineField="true" name="standardArticleCheck" label="tracking-view-standard-article" type="checkbox" value="<%= standardArticleCheck %>" />		
					<aui:input inlineLabel="left" inlineField="true" name="standardGalleryCheck" label="tracking-view-standard-gallery" type="checkbox" value="<%= standardGalleryCheck %>" />
					<aui:input inlineLabel="left" inlineField="true" name="standardPollCheck" label="tracking-view-standard-poll" type="checkbox" value="<%= standardPollCheck %>" />
					<aui:input inlineLabel="left" inlineField="true" name="standardMultimediaCheck" label="tracking-view-standard-multimedia" type="checkbox" value="<%= standardMultimediaCheck %>" />
				</aui:fieldset>
						
				<%
					
					queryFilter += TrackingUtil.getSqlStructureFilter(standardArticleCheck, standardGalleryCheck, standardPollCheck, standardMultimediaCheck);
				
					queryFilter += TrackingUtil.getSqlDateFilter(start, end);
					
					queryFilter += TrackingUtil.getSqlKeywordsFilter(keyword, keywordComment);

					Calendar endDate = Calendar.getInstance();
					if (end != null) {
						endDate.setTime(end);
					}
					
					Calendar startDate = Calendar.getInstance();
					startDate.add(Calendar.MONTH, -1);
					if (start != null) {
						startDate.setTime(start);
					}
		
					// Start Date
					int monthStartDate = startDate.get(Calendar.MONTH);						
					
					int dayStartDate = startDate.get(Calendar.DATE);						
		
					int yearStartDate = startDate.get(Calendar.YEAR);									
					int yearRangeStartStartDate = yearStartDate - 5;
					int yearRangeEndStartDate = yearStartDate + 5;
		
					int firstDayOfWeekStartDate = startDate.getFirstDayOfWeek() - 1;			
		
					// End Date
					int monthEndDate = endDate.get(Calendar.MONTH);						
					
					int dayEndDate = endDate.get(Calendar.DATE);						
		
					int yearEndDate = endDate.get(Calendar.YEAR);									
					int yearRangeStartEndDate = yearStartDate - 5;
					int yearRangeEndEndDate = yearStartDate + 5;
		
					int firstDayOfWeekEndDate = endDate.getFirstDayOfWeek() - 1;			
								
					// Show Time 
					String timeFormatPattern = ((SimpleDateFormat)(DateFormat.getTimeInstance(DateFormat.SHORT, locale))).toPattern();
		
					boolean timeFormatAmPm = true;
		
					if (timeFormatPattern.indexOf("a") == -1) {
						timeFormatAmPm = false;
					}
					
					int startHour = startDate.get(Calendar.HOUR_OF_DAY);
					int endHour = endDate.get(Calendar.HOUR_OF_DAY);
		
					if (timeFormatAmPm) {
						startHour = startDate.get(Calendar.HOUR);
						endHour = endDate.get(Calendar.HOUR);
					}
		
					int startMinute = startDate.get(Calendar.MINUTE);
					int endMinute = endDate.get(Calendar.MINUTE);
					
					int amPm = Calendar.AM;
					if (timeFormatAmPm) {
						amPm = endDate.get(Calendar.AM_PM);
					}
				%>
		
				<span class="aui-field aui-field-date">
					<span class="aui-field-content">
						<label class="aui-field-label"><liferay-ui:message key="tracking-view-filter-start-date" /></label>
						<liferay-ui:input-date
							dayParam="dateStartDay"
							dayValue="<%= dayStartDate %>"
							firstDayOfWeek="<%= firstDayOfWeekStartDate %>"
							formName="fm1"			
							monthParam="dateStartMonth"
							monthValue="<%= monthStartDate %>"
							yearParam="dateStartYear"
							yearValue="<%= yearStartDate %>"
							yearRangeStart="<%= yearRangeStartStartDate %>"
							yearRangeEnd="<%= yearRangeEndStartDate %>"
						/>		
			
						<liferay-ui:input-time
							amPmParam="dateStartAmPm"
							amPmValue="<%= amPm %>"						
							hourParam="dateStartHour"
							hourValue="<%= startHour %>"
							minuteParam="dateStartMinute"
							minuteValue="<%= startMinute %>"				
						/>
					</span>
				</span>
				
				<span class="aui-field aui-field-date">
					<span class="aui-field-content">
						<label class="aui-field-label"><liferay-ui:message key="tracking-view-filter-end-date" /></label>
						<liferay-ui:input-date
							dayParam="dateEndDay"
							dayValue="<%= dayEndDate %>"
							firstDayOfWeek="<%= firstDayOfWeekEndDate %>"
							formName="fm1"			
							monthParam="dateEndMonth"
							monthValue="<%= monthEndDate %>"
							yearParam="dateEndYear"
							yearValue="<%= yearEndDate %>"
							yearRangeStart="<%= yearRangeStartEndDate %>"
							yearRangeEnd="<%= yearRangeEndEndDate %>"
						/>
				
						<liferay-ui:input-time
							amPmParam="dateEndAmPm"
							amPmValue="<%= amPm %>"						
							hourParam="dateEndHour"
							hourValue="<%= endHour %>"
							minuteParam="dateEndMinute"
							minuteValue="<%= endMinute %>"				
						/>
					</span>
				</span>
				
				<aui:select name="pendingModeration" label="tracking-edit-filter-pending-moderation">
					<aui:option selected='<%= moderate.equals("") %>' label="tracking-edit-filter-all" value="" />
					<aui:option selected='<%= moderate.equals("true") %>' label="tracking-edit-filter-true" value="true" />
					<aui:option selected='<%= moderate.equals("false") %>' label="tracking-edit-filter-false" value="false" />
				</aui:select>
			
				<aui:button-row>
					<aui:button type="submit" value="tracking-view-filter" />
				</aui:button-row>
			</aui:form>
		</aui:fieldset>
		
		<div class="separator article-separator"></div>

		<liferay-portlet:renderURL varImpl="iteratorURL">
			<liferay-portlet:param name="standardArticleCheck" value="<%= String.valueOf(standardArticleCheck) %>" />
			<liferay-portlet:param name="standardGalleryCheck" value="<%= String.valueOf(standardGalleryCheck) %>" />
			<liferay-portlet:param name="standardPollCheck" value="<%= String.valueOf(standardPollCheck) %>" />
			<liferay-portlet:param name="standardMultimediaCheck" value="<%= String.valueOf(standardMultimediaCheck) %>" />
			<liferay-portlet:param name="pendingModeration" value="<%= pendingModeration %>" />
			<liferay-portlet:param name="startDate" value="<%= (start!= null) ? df.format(start) : null %>" />
			<liferay-portlet:param name="endDate" value="<%= (end != null) ? df.format(end) : null %>" />
			<liferay-portlet:param name="keyword" value="<%= keyword %>" />
			<liferay-portlet:param name="keywordComment" value="<%= keywordComment %>" />
			<liferay-portlet:param name="tabs1" value="articles" />
			<liferay-portlet:param name="milenium" value="<%= milenium %>" />
		</liferay-portlet:renderURL>

		<liferay-ui:search-container emptyResultsMessage="tracking-view-empty-result" iteratorURL="<%= iteratorURL %>" orderByCol="<%= orderByCol %>" orderByType="<%= orderByType %>" >
			<liferay-ui:search-container-results>
				<%
				
					List<Object> trackingData = TrackingUtil.getArticleTrackingData(scopeGroupId, orderByCol, orderByType.toUpperCase(), 
												searchContainer.getStart(), searchContainer.getEnd(), queryFilter, pendingModeration);
					
					List<TrackingSearchObject> resultArticles = new ArrayList<TrackingSearchObject>();
				
					int sizeData = 0;
	
					if(trackingData != null && trackingData.size() > 0)
					{
						sizeData = TrackingUtil.getSizeArticleTrackingData(scopeGroupId, queryFilter, pendingModeration);
						resultArticles = TrackingUtil.getTrackingsFromObjects(trackingData);
					}
	
					results = resultArticles;
					total = sizeData;							
					pageContext.setAttribute("results", results);
					pageContext.setAttribute("total", total);
					
				%>
			</liferay-ui:search-container-results>
			
			<liferay-ui:search-container-row  className="com.protecmedia.iter.tracking.util.TrackingSearchObject" modelVar="tracking">
				<%
					PortletURL updateURL = renderResponse.createActionURL();
					updateURL.setParameter("javax.portlet.action", "editTracking");
					updateURL.setParameter("contentId", tracking.getContentId());
					updateURL.setParameter("tabs1", "statistics");
					updateURL.setParameter("milenium", milenium);
					String decodeURL = TrackingUtil.getRelativeURL(updateURL);
					
				%>	
						
				<liferay-ui:search-container-column-text href="<%= decodeURL%>" orderable="true" orderableProperty="name" title="" name="tracking-view-name" value="<%= tracking.getName() %>"/>
				<liferay-ui:search-container-column-text href="<%= decodeURL%>" orderable="true" orderableProperty="type" title="" name="tracking-view-type" value="<%= tracking.getType() %>" />
				<liferay-ui:search-container-column-text href="<%= decodeURL%>" orderable="true" orderableProperty="views" align="center" title="" name="tracking-view-views" value="<%= String.valueOf(tracking.getViews()) %>"/>	
				<liferay-ui:search-container-column-text orderable="true" orderableProperty="rating" title="" name="tracking-view-rating">
					<liferay-ui:ratings-score score="<%= tracking.getRating() %>"/>
				</liferay-ui:search-container-column-text>	
				<liferay-ui:search-container-column-text href="<%= decodeURL%>" orderable="true" orderableProperty="sent" align="center" title="" name="tracking-view-sent" value="<%= String.valueOf(tracking.getSent()) %>"/>
				<liferay-ui:search-container-column-text href="<%= decodeURL%>" orderable="true" orderableProperty="comments" align="center" title="" name="tracking-view-comments" value="<%= String.valueOf(tracking.getComments()) %>"/>
				<liferay-ui:search-container-column-text href="<%= decodeURL%>" orderable="true" orderableProperty="moderation" title="" name="tracking-view-moderated" value="<%= String.valueOf(tracking.isModeration()) %>"/>
				<liferay-ui:search-container-column-text href="<%= decodeURL%>" orderable="true" orderableProperty="votings" align="center" title="" name="tracking-view-votings" value="<%= String.valueOf(tracking.getVotings()) %>"/>	
				
			</liferay-ui:search-container-row>
				
			<liferay-ui:search-iterator  />
		</liferay-ui:search-container>
	</c:when>

	<c:when test='<%= tabs1.equals("pages")%>'>
	
		<c:choose>
		
			<c:when test="<%=scopeGroupId == globalGroupId%>">
				<liferay-ui:message key="tracking-portlet-page-option-not-available-in-global-environment" />
			</c:when>
			<c:otherwise>
				<%--***************--%>
				<%--     Pages     --%>
				<%--***************--%>	
				<jsp:useBean id="addPageURL" class="java.lang.String" scope="request" />
				
				<liferay-portlet:renderURL varImpl="iteratorURL">
					<liferay-portlet:param name="layout-id" value="<%= String.valueOf(layoutId) %>" />
					<liferay-portlet:param name="milenium" value="<%= milenium %>" />
				</liferay-portlet:renderURL>
		
				<%
					List<Long> openNodes = new ArrayList<Long>();
					
					// Añadimos el root
					openNodes.add(0L);
					Layout paginaActual = null;
					
					try{
						paginaActual = LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(layoutId, scopeGroupId);
					}catch(Exception e){
						seccionList = LayoutLocalServiceUtil.getLayouts(scopeGroupId, false, 0);
						paginaActual = LayoutLocalServiceUtil.getLayoutByUuidAndGroupId(seccionList.get(0).getUuid(), scopeGroupId);
					}
					
					getOpenNodes(paginaActual.getLayoutId(), 0, scopeGroupId, openNodes);	
					
					String nameRoot = portletDisplay.getThemeDisplay().getScopeGroupName();
					String idRoot =  "rootTreeViewId";
					String idActual = paginaActual.getLayoutId() + "Id";
				
					String children = getChildrens(nameRoot, idRoot, paginaActual.getLayoutId(), 0, scopeGroupId, openNodes, renderResponse, milenium);								
				%>
				
				<div class="page-content-view-portlet">
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
								
									
				<liferay-portlet:renderURL varImpl="iteratorURL">
					<liferay-portlet:param name="standardArticleCheck" value="<%= String.valueOf(standardArticleCheck) %>" />
					<liferay-portlet:param name="standardGalleryCheck" value="<%= String.valueOf(standardGalleryCheck) %>" />
					<liferay-portlet:param name="standardPollCheck" value="<%= String.valueOf(standardPollCheck) %>" />
					<liferay-portlet:param name="standardMultimediaCheck" value="<%= String.valueOf(standardMultimediaCheck) %>" />
					<liferay-portlet:param name="pendingModeration" value="<%= pendingModeration %>" />
					<liferay-portlet:param name="startDate" value="<%= (start!= null) ? df.format(start) : null %>" />
					<liferay-portlet:param name="endDate" value="<%= (end != null) ? df.format(end) : null %>" />
					<liferay-portlet:param name="keyword" value="<%= keyword %>" />
					<liferay-portlet:param name="keywordComment" value="<%= keywordComment %>" />
					<liferay-portlet:param name="layout-id" value="<%= layoutId %>" />
					<liferay-portlet:param name="milenium" value="<%= milenium %>" />
				</liferay-portlet:renderURL>
				
				<liferay-ui:search-container emptyResultsMessage="tracking-view-empty-result" iteratorURL="<%= iteratorURL %>" 
											 orderByCol="<%= orderByCol %>" orderByType="<%= orderByType %>" >
					<liferay-ui:search-container-results>
						<%					
												   			 
							List<Object> trackingData = TrackingUtil.getPageTrackingData(layoutId, scopeGroupId, orderByCol, orderByType.toUpperCase(), searchContainer.getStart(), searchContainer.getEnd());
							
							List<TrackingSearchObject> resultArticles = new ArrayList<TrackingSearchObject>();
						
							int sizeData = 0;

							if(trackingData != null && trackingData.size() > 0)
							{
								sizeData = TrackingUtil.getSizePageTrackingData(layoutId, scopeGroupId);
								
								resultArticles = TrackingUtil.getTrackingsFromObjects(trackingData);
							}

							results = resultArticles;
							total = sizeData;							
							pageContext.setAttribute("results", results);
							pageContext.setAttribute("total", total);
							
						%>
					</liferay-ui:search-container-results>
					
					
					
					<liferay-ui:search-container-row  className="com.protecmedia.iter.tracking.util.TrackingSearchObject" modelVar="tracking">
						<%
							PortletURL updateURL = renderResponse.createActionURL();
							updateURL.setParameter("javax.portlet.action", "editTracking");
							updateURL.setParameter("contentId", tracking.getContentId());
							updateURL.setParameter("tabs1", "statistics");
							updateURL.setParameter("milenium", milenium);
							String decodeURL = TrackingUtil.getRelativeURL(updateURL);
						%>	
						
						<liferay-ui:search-container-column-text href="<%= decodeURL%>" orderable="true" orderableProperty="name" title="" name="tracking-view-name" value="<%= tracking.getName() %>"/>
						<liferay-ui:search-container-column-text href="<%= decodeURL%>" orderable="true" orderableProperty="type" title="" name="tracking-view-type" value="<%= tracking.getType() %>" />
						<liferay-ui:search-container-column-text href="<%= decodeURL%>" orderable="true" orderableProperty="views" align="center" title="" name="tracking-view-views" value="<%= String.valueOf(tracking.getViews()) %>"/>	
						<liferay-ui:search-container-column-text href="<%= decodeURL%>" orderable="true" orderableProperty="comments" align="center" title="" name="tracking-view-comments" value="<%= String.valueOf(tracking.getComments()) %>"/>
						<liferay-ui:search-container-column-text href="<%= decodeURL%>" orderable="true" orderableProperty="moderation" title="" name="tracking-view-moderated" value="<%= String.valueOf(tracking.isModeration()) %>"/>
						<liferay-ui:search-container-column-text href="<%= decodeURL%>" orderable="true" orderableProperty="votings" align="center" title="" name="tracking-view-votings" value="<%= String.valueOf(tracking.getVotings()) %>"/>	

					</liferay-ui:search-container-row>
						
					<liferay-ui:search-iterator  />
				</liferay-ui:search-container>
					
							
							</td>
						</tr>
					</table>					
					
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
									var actualChildrenNodes = actualNode.get('childNodes');
									
									var firstNode = actualChildrenNodes.item(0);
									
									firstNode.setStyle('backgroundColor', '#EEEEEE');
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
					private String getChildrens(String nameNode, String idNode, long layoutId, long idPadre, long idGrupo, List<Long> openNodes, RenderResponse renderResponse, String milenium) throws Exception  {
						List<Layout> layoutList = getLayouts(idGrupo, false, idPadre);
						
						String uuid = "";
						long id = 0;
						if (idPadre != 0) {
							Layout l = LayoutLocalServiceUtil.getLayout(idGrupo, false, idPadre);
							uuid = l.getUuid();
							id = l.getLayoutId();
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
							portletUrl.setParameter("milenium", milenium);
							children.append("label:'");
							children.append("<a href=\"");
							children.append(portletUrl.toString());
							children.append("\"");
							children.append(">");
							children.append(HtmlUtil.escape(nameNode));
							children.append("</a>");
							children.append("',");
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
									children.append(getChildrens(nameNode, idNode, layoutId, _layout.getLayoutId(), idGrupo, openNodes, renderResponse, milenium));												
									
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
											
								if (layoutId == _layout.getLayoutId()) {					
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

			</c:otherwise>
		</c:choose>
	</c:when>
</c:choose>
