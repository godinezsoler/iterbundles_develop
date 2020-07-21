<%--
*Copyright (c) 2011 Promocion Tecnologica y Comercial, S.A - protecmedia.com. All rights reserved.
--%>

<%@ include file="init.jsp" %> 

<span class="iter-field iter-field-text">
	<span class="iter-field-content">
		<span class="iter-field-element">
			<input id="<portlet:namespace />keywords" type="text" value="" name="keywords"
	   			   inlineField="true" class="iter-field-input iter-field-input-text"
	   			   onkeydown="javascript:<portlet:namespace />onKeyEnterSearch(event)"/>
	   	</span>
	</span>
</span>
	   
<span class="iter-button iter-button-submit">
	<span class="iter-button-content"> 
		<input id="<portlet:namespace />search" type="submit" name="search" 
			   value="<%=searchLabel %>" class="iter-button-input iter-button-input-submit"
			   onclick="javascript:<portlet:namespace />onClickSearch()" />
	</span>
</span>

<script type="text/javascript">

<%@ include file="../commons/javascripts.jsp" %>

function <portlet:namespace />onClickSearch()
{
<%
	if(Validator.isNotNull(resultsLayoutURL))
	{
%>
		var keywords = <portlet:namespace />cleanKeywords(jQryIter("#<portlet:namespace />keywords").val());
		if(keywords.length > 0)
		{
			var resultsLayoutURL = '<%= resultsLayoutURL %>';
			var resultsParamURL = '<%= resultsParamURL %>';
			window.location.href = resultsLayoutURL + keywords + resultsParamURL;
		}
<%
	}
%>
}

function <portlet:namespace />onKeyEnterSearch(event)
{
	if(event.keyCode==13)
		<portlet:namespace />onClickSearch();
}

</script>