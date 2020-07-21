
<%@page import="com.liferay.portal.kernel.util.PropsValues"%>
<%@page import="com.liferay.portal.util.HtmlOptimizer"%>
<%@ include file="init.jsp" %>

<%
	String elemParentTagName = "ul";
	String elemChildTagName  = "li";
	if (HtmlOptimizer.isEnabled())
	{
		elemParentTagName = "section";
		elemChildTagName  = "article";
	}

	String contentId = (String) renderRequest.getParameter(WebKeys.URL_PARAM_CONTENT_ID);
	String currentURL = themeDisplay.getURLCurrent();

	Date validityDate = GroupMgr.getPublicationDate(scopeGroupId);
	boolean contentIdOperation = Validator.isNotNull(contentId) && !contentId.equals("0");
	
	List<String> contentIds = new ArrayList<String>();
	List<String> selectedCategoriesIds = new ArrayList<String>();

	if(currentURL.contains("/meta/"))
		selectedCategoriesIds = TopicsUtil.getCategoriesIds(renderRequest);
	
	
	if(selectedCategoriesIds.size() == 1 && selectedCategoriesIds.get(0).equals("-1"))
		selectedCategoriesIds = new ArrayList<String>();
	
	
	if(contentIdOperation && selectedCategoriesIds.size() == 0 &&
	   !currentURL.contains(PortalUtil.getSearchSeparator()) && 
	   !currentURL.contains("/meta/"))
	{
		contentIds.add(contentId);
	}
	
	List<Object> listCategories = null;
	
	if(selectedCategoriesIds.size() > 0 || contentIds.size() > 0)
	{
		listCategories = TopicsUtil.getFilterCategories(scopeGroupId, contentIds, selectedCategoriesIds, excludeVocabularyIds, 
														excludeCategoryIds, orderType, orderDirection);
	}

%>	
	
<c:choose>
	<c:when test="<%= (listCategories != null && listCategories.size() > 0) %>">
	<%-- Caso no vacío --%>
	
		<div class="categoryGroup">
			<div class="categorySpacerTop"></div>  
                <div class="categoryTitle">
                	<c:if test="<%= (title != null) %>">
						<h2><%= title %></h2>
					</c:if>
				</div>
			<div class="categorySpacerTitle"></div>
		 	<div class="categoryListBlock">
                <<%=elemParentTagName%> class="categoryList">
					<%
						for(int i = 0; i < listCategories.size(); i++)
						{
							Object[] currentCategoryData = (Object[])listCategories.get(i);
							String categoryId = currentCategoryData[0].toString();
							String categoryName = currentCategoryData[1].toString();
							int currentPos = i+1;
							
							String liClass = "categoryListItem";
							
							if(contentIdOperation && selectedCategoriesIds.size() == 0)
							{
								liClass += " categoryArticleItem";
							}
							else
							{
								liClass += " categoryPageItem";
							}

if (!HtmlOptimizer.isEnabled())
{
							if(i == 0)
							{
								liClass += " first";
							}
							if(i == listCategories.size() - 1)
							{
								liClass += " last";
							}
							
							if(currentPos % 2 == 0)
							{
								liClass += " even";
							}
							else
							{
								liClass += " odd";
							}
							
							liClass += " n" + currentPos;
}

							String categoryURL = "";
							if (modelId != 0)
								categoryURL = TopicsUtil.getTopicURLById(modelId, categoryName);
							
							// ITER-1280 Evitar el marcaje automático con Microdatos para Google Structured Data Tool
							String itemprop = PropsValues.ITER_MICRODATA_FOR_GOOGLE_DISABLED ? "" : "itemprop=\"keywords\"";
					%>
							<<%=elemChildTagName%> class="<%= liClass%>" data-categoryid="<%=categoryId%>" data-categoryname="<%=categoryName%>">
								<span class="categoryListItemPosition"><%= currentPos%></span>
								<c:choose>
									<c:when test="<%=categoryURL.isEmpty()%>">
										<span class="categoryListItemLink linkDisabled" <%=itemprop%>><%= categoryName%></span>
									</c:when>
									<c:otherwise>
										<a class ="categoryListItemLink" href="<%=categoryURL%>">
											<span <%=itemprop%>><%=categoryName%></span>
										</a>
									</c:otherwise>
								</c:choose>
							</<%=elemChildTagName%>>
					<%
						}
					%>
				</<%=elemParentTagName%>>
			</div>
			<div class="categorySpacerBottom"></div>
		</div>
	</c:when>
	
	<c:otherwise>
	<%-- Caso vacío --%>
		<%
		if(  showDefaultTextHTML && !defaultTextHTML.equals("")   )
		{
		%>
			<div>
				<%=defaultTextHTML%>
			</div>
		<%
		}
		else if( environment.equals(IterKeys.ENVIRONMENT_PREVIEW)    )
		{
		%>
			<div class="portlet-msg-alert">
				<liferay-ui:message key="article-topics-alert-no-topics" />
			</div>
		<%	
		}
		%>
	</c:otherwise>  
</c:choose>