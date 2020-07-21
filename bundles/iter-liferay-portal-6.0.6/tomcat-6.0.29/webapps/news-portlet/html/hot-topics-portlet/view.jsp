<%--
*Copyright (c) 2012 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@page import="com.liferay.portal.util.HtmlOptimizer"%>
<%@page import="com.liferay.portal.kernel.metrics.NewsletterMASTools"%>
<%@page import="com.liferay.portal.kernel.util.IterURLUtil"%>
<%@page import="com.liferay.portal.kernel.util.request.IterRequest"%>
<%@ include file="init.jsp" %>

	<%
	String elemParentTagName = "ul";
	String elemChildTagName  = "li";
	if (HtmlOptimizer.isEnabled())
	{
		elemParentTagName = "section";
		elemChildTagName  = "article";
	}

	//Portlet sin configuracion
	if (title == null){
		if (environment.equals(IterKeys.ENVIRONMENT_PREVIEW)){%>	
			<div class="portlet-msg-info">
				<span class="displaying-help-message-tpl-holder">
					<liferay-ui:message key="please-contact-with-your-administrator-to-configure-this-portlet" />
				</span>
			</div>
		<%}
	}
	//Portlet configurado
	else{
		Date validityDate = GroupMgr.getPublicationDate(scopeGroupId);
		List<Object> listCategories = TopicsUtil.getFilterCategories(scopeGroupId, contentTypes, contentVocabularyIds, 
																 	 contentCategoryIds, excludeCategoryIds, 
																 	 stickyCategoryIds, layoutIds, qualificationId, 
																 	 orderType, orderDirection, validityDate, 0, 
																 	 numTopics);

		if (listCategories != null && listCategories.size() > 0){ %>
		<%-- Caso no vacío --%>
		
			<div class="categoryGroup">
				<div class="categorySpacerTop"></div>  
	                <div class="categoryTitle">
	                	<c:if test="<%= (title != null) %>">
							<span><%= title %></span>
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
								
							if (!HtmlOptimizer.isEnabled())
							{								
								if(i == 0)
								{
									liClass += " first";
								}
								else if(i == listCategories.size() - 1)
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
								
								String categoryURL = TopicsUtil.getTopicURLById(modelId, categoryName);
						%>
								<<%=elemChildTagName%> class="<%= liClass%>" data-categoryid="<%=categoryId%>" data-categoryname="<%=categoryName%>">
									<span class="categoryListItemPosition"><%= currentPos%></span>
									<c:choose>
										<c:when test="<%=categoryURL.isEmpty()%>">
											<span class="categoryListItemLink linkDisabled">
												<%= categoryName%>
											</span>
										</c:when>
										<c:otherwise>
											<%
												if (IterRequest.isNewsletterPage())
												{
													categoryURL = IterURLUtil.getIterHost() + categoryURL;
													categoryURL = NewsletterMASTools.addMark2Meta(categoryURL, categoryName);
												}
											%>
											<a class ="categoryListItemLink" href="<%=categoryURL%>"> 
												<%=categoryName%> 
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
		<% }
		else
		{%>
			<%-- Caso  vacío --%>
			<c:if test='<%= showDefaultTextHTML && !defaultTextHTML.equals("")%>'>
	                	<ul class="categoryList">
		                	<li>
								<div>
									<%=defaultTextHTML%>
								</div>
							</li>
						</ul>
			</c:if>
		<%}
	} %>
