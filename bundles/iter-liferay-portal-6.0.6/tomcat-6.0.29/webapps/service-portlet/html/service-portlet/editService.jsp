<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@include file="init.jsp"%>

<%@page import="com.protecmedia.iter.services.model.Service"%>


<%@page import="com.liferay.portal.theme.ThemeDisplay"%>
<%@page import="java.util.Locale"%>

<jsp:useBean class="java.lang.String" id="editURL" scope="request" />
<jsp:useBean id="service" type="com.protecmedia.iter.services.model.Service" scope="request" />

<portlet:renderURL var="cancelServiceURL" />

<%
	String redirect = "javascript:location.href='" + cancelServiceURL + "'";

	//List<Layout> layoutList = LayoutLocalServiceUtil.getLayouts(scopeGroupId, false, 0);
%>

<liferay-ui:tabs
	names="Services" 
	backURL="<%= redirect %>" 
/>

<form name="<portlet:namespace />editService" action="<%=editURL%>"	enctype="multipart/form-data" method="post">		    
 	<aui:input type="hidden" name="serviceId" value="<%= service.getPrimaryKey() %>" /> 
	<aui:input type="hidden" name="imageId" value="<%= service.getImageId() %>" />   	
	
	<aui:fieldset label="">
        	
    	<aui:model-context bean="<%= service %>" model="<%= Service.class %>" />
    	
    	<liferay-ui:error key="servicetitle-required" message="servicetitle-required" />
    	<aui:input name="title" label="service-back-end-edit-title"  />        		
		
		<liferay-ui:error key="serviceurl-required"	message="serviceurl-required" />
        <aui:select name="serviceUrl" label="service-back-end-edit-url">        	        	        	
        	<%        			
        		List<Layout> layoutList = new ArrayList<Layout>();
        		List<Integer> depths = new ArrayList<Integer>();
        		_getPaginas(0, 0, scopeGroupId, layoutList, depths);
        		
        		for (int i = 0; i < layoutList.size(); i++) {
        			Layout _layout = layoutList.get(i);
        			String name = "- " + _layout.getName(themeDisplay.getLocale());
        			for (int j = 0; j < depths.get(i); j++) {
        				name = "&nbsp;" + name; 
        			}
        	%>   
        			<aui:option value="<%= _layout.getPlid() %>" selected="<%= (service.getLinkId() == _layout.getPlid()) %>"><%= name %></aui:option>
        	<%
        		}
        	%>									
        </aui:select>		
       	
       	<liferay-ui:error key="serviceimage-required"	message="serviceimage-required" />
       	<aui:input name="imageFile" label="service-back-end-edit-image" type="file"></aui:input>
		    
		<c:if test="<%= Validator.isNotNull(service.getImageId()) && service.getImageId() != -1 %>">
			<div>
				<img src="/image/journal/article?img_id=<%=service.getImageId()%>&r=<%= Math.random() %>" width="50px" height="50px" />
			</div>
		</c:if>				

   		<aui:button-row>	
			<aui:button type="submit" />	
			<aui:button onClick="<%= redirect %>" type="cancel" />
		</aui:button-row>
		
	</aui:fieldset>
</form>

<%!

private void _getPaginas(int depth, long idPadre, long idGrupo, List<Layout> paginas, List<Integer> depths) throws Exception  {
	List<Layout> layoutList = LayoutLocalServiceUtil.getLayouts(idGrupo, false, idPadre);

	for (int i = 0; i < layoutList.size(); i++) {
		Layout _layout = layoutList.get(i);	
		if (!_layout.isHidden()) {			
			paginas.add(_layout);
			depths.add(depth);
		}
		
		depth++;
		_getPaginas(depth, _layout.getLayoutId(), idGrupo, paginas, depths);
		depth--;
	}	
}

%>
