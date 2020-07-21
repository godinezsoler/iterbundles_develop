<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@ include file="init.jsp" %>

<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %>

<%@page import="com.liferay.portal.kernel.util.Constants"%>

<liferay-portlet:actionURL portletConfiguration="true" var="editURL" />

<%	
	String redirect = ParamUtil.getString(request, "redirect");
%> 

<liferay-ui:tabs 
	names="comments"
/>

<aui:form name="fm" method="post" action="<%= editURL %>">	
	<aui:input name="<%= Constants.CMD %>" type="hidden" value="<%= Constants.UPDATE %>" />	
	<aui:panel label="ratings-configuration-permission">
		<aui:fieldset label="">		
			<aui:select name="ratingReadAccessLevelCategoryId" label="ratings-configuration-permission-read">
				<aui:option label="ratings-configuration-permission-default" selected="<%=ratingReadAccessLevelCategoryId==-1%>" value="-1"/>
				<%
					List<AssetCategory> categories = CategorizeLocalServiceUtil.getVocabularyCategories(globalGroupId, IterKeys.USER_ACCESS_LEVEL_VOCABULARY);
					for (AssetCategory category : categories){
						long categoryId = category.getCategoryId();
						String categoryName = category.getName();
				%>
					<aui:option label="<%=categoryName%>" selected="<%= ratingReadAccessLevelCategoryId == categoryId %>" value="<%=String.valueOf(categoryId)%>"/>
				<%	} %>
			</aui:select>		
			<aui:select name="ratingAddAccessLevelCategoryId" label="ratings-configuration-permission-add">
				<aui:option label="ratings-configuration-permission-default" selected="<%=ratingAddAccessLevelCategoryId==-1%>" value="-1"/>
				<%
					List<AssetCategory> categories = CategorizeLocalServiceUtil.getVocabularyCategories(globalGroupId, IterKeys.USER_ACCESS_LEVEL_VOCABULARY);
					for (AssetCategory category : categories){
						long categoryId = category.getCategoryId();
						String categoryName = category.getName();
				%>
					<aui:option label="<%=categoryName%>" selected="<%= ratingAddAccessLevelCategoryId == categoryId %>" value="<%=String.valueOf(categoryId)%>"/>
				<%	} %>
			</aui:select>	
		</aui:fieldset>	
	</aui:panel>
	
	<aui:button-row>	
			<aui:button type="submit" />	
			<aui:button onClick="<%= redirect %>" type="cancel" />
	</aui:button-row>
	
</aui:form>
