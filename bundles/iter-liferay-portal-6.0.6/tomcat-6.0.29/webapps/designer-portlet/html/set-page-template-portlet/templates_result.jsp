<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>
<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>
<%@page import="com.protecmedia.iter.designer.service.PageTemplateLocalServiceUtil"%>
<%@page import="java.util.List"%>
<%@page import="com.protecmedia.iter.designer.model.PageTemplate"%>
<%@page import="com.liferay.portal.kernel.util.StringBundler"%>

<%@page import="com.liferay.portal.kernel.language.LanguageUtil"%>
<%@page import="com.liferay.portal.kernel.util.StringPool"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />

<%
	int itemsPerPage = ParamUtil.getInteger(request, "itemsPerPage", 10);		

	int numPage = ParamUtil.getInteger(request, "numPage", 1);		
	
	int total = PageTemplateLocalServiceUtil.getPageTemplatesCount(scopeGroupId);
	
	int start = itemsPerPage * (numPage  - 1); 
	int end = itemsPerPage * numPage;
	end = (end > total) ? total : end;
	
	int numPages = (int) Math.ceil((double)total / (double)itemsPerPage);
	
	List<PageTemplate> pageTemplates = PageTemplateLocalServiceUtil.getPageTemplates(scopeGroupId, start, end);

%>

<c:choose>
	<c:when test="<%= numPages == 0 || total == 0 %>">
		<div class="portlet-msg-info"><liferay-ui:message key="result-list-empty" /></div>
	</c:when>
	<c:otherwise>	
		<div class="lfr-search-container "> 
			<div class="results-grid"> 
				<table class="taglib-search-iterator"> 
					<tbody>
						<tr class="portlet-section-header results-header"> 
							<th id="vpdr_col-1" class="col-1 "><liferay-ui:message key="set-page-template-view-name" /></th> 
							<th id="vpdr_col-2" class="col-2 "><liferay-ui:message key="set-page-template-view-type" /></th> 
						</tr> 
						<tr class="lfr-template portlet-section-body results-row"> 
							<td></td>
							<td></td>
						</tr>	
			
						<%
							int i = 0;
							for (PageTemplate pageTemplate : pageTemplates) {
								pageTemplate = pageTemplate.toEscapedModel();
														
								StringBundler sb = new StringBundler(7);
											
								sb.append("javascript:");
								sb.append(renderResponse.getNamespace());
								sb.append("selectPageTemplate(");
								sb.append(pageTemplate.getId());
								sb.append(",'");
								sb.append(pageTemplate.getName());			
								sb.append("');");						
						%>	
								<tr class="portlet-section-body results-row last <%= (i % 2 == 0) ? "" : "alt" %>"> 
									<td headers="vpdr_col-1" colspan="1" class="align-left col-1 first valign-middle">
										<a href="<%= sb.toString() %>"><%= pageTemplate.getName() %></a>
									</td> 
									<td headers="vpdr_col-2" colspan="1" class="align-left col-2 last valign-middle"> 
										<a href="<%= sb.toString() %>"><%= pageTemplate.getType() %></a> 
									</td>
								</tr>		
						<%
								i++;
							}
						%>			
					</tbody>
				</table>
			</div>	 
			
			<%
				String suffix = LanguageUtil.get(pageContext, "of") + StringPool.SPACE + numPages;
			%>
			
			<div class="taglib-search-iterator-page-iterator-bottom"> 
				<div class="taglib-page-iterator"> 
				
					<div class="search-results">
						<c:choose>
							<c:when test="<%= total > itemsPerPage %>">
								<%= LanguageUtil.format(pageContext, "showing-x-x-of-x-results", new Object[] {start + 1, end, total}) %>
							</c:when>
							<c:otherwise>
								<%= LanguageUtil.format(pageContext, "showing-x-results", total) %>							
							</c:otherwise>
						</c:choose>
					</div>
					<c:if test="<%= total > itemsPerPage %>">				 
						<div class="search-pages"> 
							<%
								StringBundler onClickFirst = new StringBundler(3);
							
								onClickFirst.append("ajaxFunction(");
								onClickFirst.append("1");
								onClickFirst.append(")");
								
								StringBundler onClickPrevious = new StringBundler(3);
								
								onClickPrevious.append("ajaxFunction(");
								onClickPrevious.append(numPage - 1);
								onClickPrevious.append(")");
								
								StringBundler onClickNext = new StringBundler(3);
								
								onClickNext.append("ajaxFunction(");
								onClickNext.append(numPage + 1);
								onClickNext.append(")");
								
								StringBundler onClickLast = new StringBundler(3);
								
								onClickLast.append("ajaxFunction(");
								onClickLast.append(numPages);
								onClickLast.append(")");
							%>
							
							<div class="page-links">
								<c:choose>
									<c:when test="<%= start <= 1 %>">
										<span class="first"> First</span>
									</c:when>
									<c:when test="<%= start > 1 %>">
										<a onClick="<%= onClickFirst.toString() %>" class="first"> First</a>
									</c:when>
								</c:choose>
								
								<c:choose>
									<c:when test="<%= start <= 1 %>">
										<span class="previous"> Previous</span>
									</c:when>
									<c:when test="<%= start > 1 %>">
										<a onClick="<%= onClickPrevious.toString() %>" class="previous"> Previous</a>
									</c:when>
								</c:choose>
								
								<c:choose>
									<c:when test="<%= end >= total %>">
										<span class="next"> Next</span>
									</c:when>
									<c:when test="<%= end < total %>">
										<a onClick="<%= onClickNext.toString() %>" class="next"> Next</a>
									</c:when>
								</c:choose>
								
								<c:choose>
									<c:when test="<%= end >= total %>">
										<span class="last"> Last</span>
									</c:when>
									<c:when test="<%= end < total %>">
										<a onClick="<%= onClickLast.toString() %>"  class="last"> Last</a>
									</c:when>
								</c:choose>
							</div> 
						</div>
					</c:if>
				</div>
			</div> 
		</div>
	</c:otherwise>
</c:choose>
