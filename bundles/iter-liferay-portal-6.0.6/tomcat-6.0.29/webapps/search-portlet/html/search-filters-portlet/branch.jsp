<%@ page contentType="text/html; charset=UTF-8" %>

<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>

<%@page import="com.liferay.portal.kernel.util.KeyValuePair"%>
<%@page import="com.liferay.portal.kernel.util.ParamUtil"%>

<%@page import="com.protecmedia.iter.search.util.SearchUtil"%>

<%

	String[] selectedCategoryIdsArray = ParamUtil.get(request, "selectedCategoryIds", "").split("-");
	String excludedCategoryIds = ParamUtil.get(request, "excludedCategoryIds", "");
	String id = ParamUtil.get(request, "id", "");
	String type = ParamUtil.get(request, "type", "");
	String lista = ParamUtil.get(request, "lista", "");
	String scopeGroupId = ParamUtil.get(request, "scopeGroupId", "");
	String companyId = ParamUtil.get(request, "companyId", "");
	String groupId = ParamUtil.get(request, "groupId", "");
	List<KeyValuePair> list = new ArrayList<KeyValuePair>();
	list = SearchUtil.getSubLevel(type, id, excludedCategoryIds, companyId, scopeGroupId, groupId);
%>

<ul class="childList nonEmptyList">
<%
	for(int a=0; a<list.size(); a++)
	{
		String currentId = list.get(a).getKey();
		String value = list.get(a).getValue().substring(1);
		String hasChildren = list.get(a).getValue().substring(0,1);
%>
		<li id="<%=currentId%>" class="nonTopLevel listLevel_deep_2" haschildren="<%= hasChildren %>">
		 	<input type="checkbox" class="checkItem" 
			 	<%
			 		if(SearchUtil.isInArray(currentId, selectedCategoryIdsArray))
			 			out.print(" checked=\"checked\"");
			 	%>
		 	/>
		 	<span class="itemLabel">
		 		<%= value %>
		 	</span>
		</li>
<%
	}
%>
</ul>

