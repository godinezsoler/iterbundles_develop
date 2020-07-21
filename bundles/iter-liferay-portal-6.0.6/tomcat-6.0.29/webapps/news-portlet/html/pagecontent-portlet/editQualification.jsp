<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>
<%@ include file="init.jsp" %>

<jsp:useBean id="qualification" type="com.protecmedia.iter.news.model.Qualification" scope="request" />
<jsp:useBean class="java.lang.String" id="editURL" scope="request" />

<portlet:renderURL var="cancelQualificationURL" />

<%
	String redirect = "javascript:location.href='" + cancelQualificationURL + "'";

	String tabs1 = ParamUtil.getString(request, "tabs1", "page-content");

	PortletURL portletURL = renderResponse.createRenderURL();	
	portletURL.setParameter("tabs1", tabs1);
%>

<liferay-ui:error key="qualification-already-exist" message="qualification-already-exist" />

<aui:form name="fm" method="post" action="<%= editURL %>">
	<aui:input name="resourcePrimKey" type="hidden" value="<%= qualification.getPrimaryKey() %>" />
	
	<aui:fieldset>
	
		<%-- Qualification --%>
		<aui:input name="qualification" label="qualification-edit-qualification"  value="<%= qualification.getName() %>"/>
								
		<aui:button-row>	
			<aui:button type="submit" />	
			<aui:button onClick="<%= redirect %>" type="cancel" />
		</aui:button-row>
	</aui:fieldset>	
	
</aui:form>
